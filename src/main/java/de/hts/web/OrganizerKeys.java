/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1.12.0 (#189): Ausrichter-Schlüssel des Live-Servers.
 * <p>
 * Vereine und Verbände beantragen auf {@code /schluessel} einen eigenen Schlüssel; er gilt erst, wenn der Betreiber ihn
 * in der Verwaltung (nur über SSH-Tunnel) freigibt. Gespeichert wird nur der SHA-256-Prüfwert des Schlüssels, dazu
 * Verein/Verband, Name, E-Mail, Zeitpunkte und Status – Datei {@code <data>/ausrichter-schluessel.tsv} (eine Zeile je
 * Schlüssel, Felder durch Tabulator getrennt). Abgelehnte Anträge verschwinden nach 30 Tagen.
 */
final class OrganizerKeys {

    enum Status { OFFEN, AKTIV, GESPERRT, ABGELEHNT }

    /** Ein Antrag bzw. Schlüssel. */
    static final class Key {
        final String id;
        final String hash;                                   // SHA-256 des Schlüssels, hex
        final String org;
        final String name;
        final String email;
        final long requested;
        volatile Status status;
        volatile long decided;
        volatile long lastUsed;

        Key(String id, String hash, String org, String name, String email, long requested, Status status, long decided, long lastUsed) {
            this.id = id;
            this.hash = hash;
            this.org = org;
            this.name = name;
            this.email = email;
            this.requested = requested;
            this.status = status;
            this.decided = decided;
            this.lastUsed = lastUsed;
        }
    }

    static final String FILE = "ausrichter-schluessel.tsv";
    static final int MAX_OPEN = 100;                         // mehr offene Anträge nimmt der Server nicht an
    static final long REJECTED_KEEP_MS = TimeUnit.DAYS.toMillis(30);
    private static final String ALPHABET = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path file;
    private final Map<String, Key> byId = new LinkedHashMap<>();
    private final Map<String, Key> byHash = new LinkedHashMap<>();
    private long lastSave;

    OrganizerKeys(Path dataDir) throws IOException {
        this.file = dataDir.resolve(FILE);
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length < 9) continue;
                try {
                    Key k = new Key(f[0], f[1], f[2], f[3], f[4], Long.parseLong(f[5]), Status.valueOf(f[6]), Long.parseLong(f[7]), Long.parseLong(f[8]));
                    put(k);
                } catch (RuntimeException e) {
                    System.err.println("Ausrichter-Schlüssel: ungültige Zeile übersprungen");
                }
            }
        }
    }

    private void put(Key k) {
        byId.put(k.id, k);
        byHash.put(k.hash, k);
    }

    /** Neuer Schlüssel „hts-xxxx-xxxx-xxxx-xxxx-xxxx-xxxx“ (24 Zeichen aus 55, etwa 138 Bit). */
    static String newKey() {
        StringBuilder b = new StringBuilder("hts");
        for (int g = 0; g < 6; g++) {
            b.append('-');
            for (int i = 0; i < 4; i++) b.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return b.toString();
    }

    static String hashOf(String key) {
        return HexFormat.of().formatHex(LiveHost.sha256(key.strip().getBytes(StandardCharsets.UTF_8)));
    }

    /** Text aus dem Formular säubern: Steuerzeichen weg, Leerraum zusammenfassen, höchstens {@code max} Zeichen. */
    static String clean(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ").replaceAll("\\s+", " ").strip();
        return t.length() > max ? t.substring(0, max).strip() : t;
    }

    static boolean validEmail(String e) {
        return e.length() <= 200 && e.matches("[^@\\s<>\"]+@[^@\\s<>\"]+\\.[^@\\s<>\".]{2,}");
    }

    /** Ergebnis eines Antrags: Schlüssel im Klartext (nur jetzt!) und der Eintrag. */
    record Request(String plainKey, Key key) {
    }

    synchronized int openCount() {
        int n = 0;
        for (Key k : byId.values()) if (k.status == Status.OFFEN) n++;
        return n;
    }

    /** Neuen Antrag anlegen; {@code null}, wenn schon zu viele offen sind. */
    synchronized Request request(String org, String name, String email, long now) throws IOException {
        if (openCount() >= MAX_OPEN) return null;
        String plain = newKey();
        String id;
        do {
            id = HexFormat.of().formatHex(new byte[]{(byte) RANDOM.nextInt(), (byte) RANDOM.nextInt(), (byte) RANDOM.nextInt(), (byte) RANDOM.nextInt()});
        } while (byId.containsKey(id));
        Key k = new Key(id, hashOf(plain), org, name, email, now, Status.OFFEN, 0, 0);
        put(k);
        save();
        return new Request(plain, k);
    }

    /** Schlüssel zum Klartext (jeder Status) oder {@code null}. */
    synchronized Key find(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return null;
        return byHash.get(hashOf(plainKey));
    }

    synchronized Key get(String id) {
        return byId.get(id);
    }

    synchronized List<Key> all() {
        List<Key> l = new ArrayList<>(byId.values());
        l.sort(Comparator.comparingLong((Key k) -> k.requested).reversed());
        return l;
    }

    /** Erfolgreiche Nutzung merken; gespeichert höchstens alle 10 Minuten (sonst bei jedem Upload). */
    synchronized void used(Key k, long now) {
        k.lastUsed = now;
        if (now - lastSave > TimeUnit.MINUTES.toMillis(10)) {
            try {
                save();
            } catch (IOException e) {
                System.err.println("Ausrichter-Schlüssel nicht gespeichert: " + e.getMessage());
            }
        }
    }

    /** Status ändern (Freigeben, Ablehnen, Sperren, Entsperren); {@code false}, wenn der Übergang nicht passt. */
    synchronized boolean setStatus(String id, Status to, long now) throws IOException {
        Key k = byId.get(id);
        if (k == null) return false;
        boolean ok = switch (to) {
            case AKTIV -> k.status == Status.OFFEN || k.status == Status.GESPERRT;
            case ABGELEHNT -> k.status == Status.OFFEN;
            case GESPERRT -> k.status == Status.AKTIV;
            case OFFEN -> false;
        };
        if (!ok) return false;
        k.status = to;
        k.decided = now;
        save();
        return true;
    }

    synchronized boolean delete(String id) throws IOException {
        Key k = byId.remove(id);
        if (k == null) return false;
        byHash.remove(k.hash);
        save();
        return true;
    }

    /** Abgelehnte Anträge nach 30 Tagen löschen; ausstehende Nutzungszeiten speichern. */
    synchronized void cleanUp(long now) throws IOException {
        boolean changed = byId.values().removeIf(k -> k.status == Status.ABGELEHNT && now - k.decided > REJECTED_KEEP_MS);
        if (changed) byHash.values().removeIf(k -> !byId.containsKey(k.id));
        save();
    }

    synchronized void save() throws IOException {
        StringBuilder b = new StringBuilder("# HTS-Live-Server: Ausrichter-Schlüssel (nur Prüfwerte). Verwaltung: Admin-Anwendung über SSH-Tunnel.\n");
        for (Key k : byId.values()) {
            b.append(k.id).append('\t').append(k.hash).append('\t').append(k.org).append('\t').append(k.name).append('\t').append(k.email)
                    .append('\t').append(k.requested).append('\t').append(k.status.name()).append('\t').append(k.decided).append('\t').append(k.lastUsed).append('\n');
        }
        Path tmp = file.resolveSibling(FILE + ".tmp");
        Files.writeString(tmp, b, StandardCharsets.UTF_8);
        try {
            tmp.toFile().setReadable(false, false);
            tmp.toFile().setReadable(true, true);
        } catch (SecurityException ignored) {
            // nur Rechte, kein Muss
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        lastSave = System.currentTimeMillis();
    }
}
