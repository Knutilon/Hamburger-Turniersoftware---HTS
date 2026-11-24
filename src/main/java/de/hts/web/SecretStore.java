/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * 1.7.0 (#142): verschlüsselte Ablage kleiner Geheimnisse (Webserver-Adresse und -Schlüssel) mit AES-256-GCM; je Wert
 * ein eigener Zufalls-IV. Der AES-Schlüssel selbst liegt
 * <ul>
 *   <li>unter Windows mit DPAPI geschützt (an das Windows-Benutzerkonto gebunden) in den Java-Einstellungen – der
 *       Aufruf läuft über PowerShell ({@code [Security.Cryptography.ProtectedData]}), ohne zusätzliche Bibliothek;</li>
 *   <li>unter Linux/macOS in einer Datei im Benutzerprofil, nur für den Benutzer lesbar (rw-------).</li>
 * </ul>
 * Schützt gegen Mitlesen in der Registrierung und Kopieren auf andere Rechner, nicht gegen jemanden, der unter
 * demselben Benutzerkonto angemeldet ist.
 */
public final class SecretStore {

    /** Liefert (und erzeugt beim ersten Mal) den 32-Byte-Schlüssel. */
    public interface KeyProvider {
        byte[] key() throws IOException;
    }

    private static final String PREFIX = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final KeyProvider provider;
    private volatile byte[] cached;

    public SecretStore(KeyProvider provider) {
        this.provider = provider;
    }

    /** Schlüsselquelle passend zum Betriebssystem. */
    public static KeyProvider platformProvider(Preferences prefs) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return new DpapiProvider(prefs);
        return new FileProvider(Path.of(System.getProperty("user.home"), ".hts", "geheimnis.key"));
    }

    private byte[] key() throws IOException {
        byte[] k = cached;
        if (k == null) {
            synchronized (this) {
                if (cached == null) {
                    byte[] kk = provider.key();
                    if (kk == null || kk.length != 32) throw new IOException("Schlüssel zum Entschlüsseln ungültig");
                    cached = kk;
                }
                k = cached;
            }
        }
        return k;
    }

    public String encrypt(String plain) throws IOException {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, all, 0, iv.length);
            System.arraycopy(ct, 0, all, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(all);
        } catch (GeneralSecurityException e) {
            throw new IOException("Verschlüsseln fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    public String decrypt(String stored) throws IOException {
        if (stored == null || !stored.startsWith(PREFIX)) throw new IOException("Unbekanntes Format");
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (all.length < 12 + 16) throw new IOException("Daten zu kurz");
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IOException("Entschlüsseln fehlgeschlagen (anderer Rechner oder Benutzer?)", e);
        }
    }

    static byte[] newKey() {
        byte[] k = new byte[32];
        RANDOM.nextBytes(k);
        return k;
    }

    // --- Linux/macOS: Datei nur für den Benutzer -------------------------------------------------------

    static final class FileProvider implements KeyProvider {
        private final Path file;

        FileProvider(Path file) {
            this.file = file;
        }

        @Override
        public byte[] key() throws IOException {
            if (Files.exists(file)) return Base64.getDecoder().decode(Files.readString(file).strip());
            Files.createDirectories(file.getParent());
            byte[] k = newKey();
            Path tmp = file.resolveSibling(file.getFileName() + ".neu");
            Files.deleteIfExists(tmp);
            try {
                Files.createFile(tmp, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } catch (UnsupportedOperationException e) {
                Files.createFile(tmp);
            }
            Files.writeString(tmp, Base64.getEncoder().encodeToString(k), StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return k;
        }
    }

    // --- Windows: DPAPI über PowerShell ----------------------------------------------------------------

    static final class DpapiProvider implements KeyProvider {
        private static final String PREF = "secret-key-dpapi";
        private final Preferences prefs;

        DpapiProvider(Preferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public byte[] key() throws IOException {
            String blob = prefs.get(PREF, "");
            if (!blob.isEmpty()) return Base64.getDecoder().decode(powershell("Unprotect", blob));
            byte[] k = newKey();
            prefs.put(PREF, powershell("Protect", Base64.getEncoder().encodeToString(k)));
            return k;
        }

        /** Daten laufen über die Standardeingabe (nicht über die Befehlszeile). */
        private static String powershell(String op, String base64In) throws IOException {
            String script = "Add-Type -AssemblyName System.Security; $i=[Console]::In.ReadLine(); "
                    + "[Console]::Out.Write([Convert]::ToBase64String([Security.Cryptography.ProtectedData]::" + op
                    + "([Convert]::FromBase64String($i), $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)))";
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-Command", script).redirectErrorStream(false).start();
            try (OutputStream os = p.getOutputStream()) {
                os.write((base64In + "\r\n").getBytes(StandardCharsets.US_ASCII));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                in.transferTo(out);
            }
            try {
                if (!p.waitFor(30, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    throw new IOException("Windows-Datenschutz (DPAPI) antwortet nicht");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("unterbrochen", e);
            }
            String r = out.toString(StandardCharsets.US_ASCII).strip();
            if (p.exitValue() != 0 || r.isEmpty()) throw new IOException("Windows-Datenschutz (DPAPI) fehlgeschlagen");
            return r;
        }
    }
}
