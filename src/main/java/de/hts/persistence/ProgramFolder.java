/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 1.8.0 (#152): Programmordner und Programmeinstellungen.
 * <p>
 * Der Programmordner ist der Ordner, in dem {@code hts.jar} (und „HTS starten.bat“) liegt. Dort liegen die Angaben zum
 * Ausrichter ({@code ausrichter.properties}, {@code ausrichter-logo.png}), der Ordner „Protokoll“ und die
 * Programmeinstellungen ({@code programm.properties}: Dunkelmodus, Speicherorte). Ist der Programmordner nicht
 * beschreibbar (z. B. unter „C:\Programme“), dient {@code <Benutzerordner>/.hts} als Ersatz.
 * <p>
 * Neue Ablage (Standard, wenn nichts gewählt ist): Turnierdateien direkt in „Dokumente\HTS“, alle PDF- und XML-Dateien
 * gemeinsam in „Dokumente\HTS\Export files“.
 */
public final class ProgramFolder {

    public static final String SETTINGS_FILE = "programm.properties";
    public static final String HTS_DIR_NAME = "HTS";
    public static final String EXPORT_DIR_NAME = "Export files";
    /** Dateien, die „Einstellungen aus älterer Version übernehmen“ kopiert. */
    static final List<String> CARRY_OVER = List.of("ausrichter.properties", "ausrichter-logo.png", SETTINGS_FILE);

    private static volatile Path dir;
    private static volatile boolean fallback;
    private static final Properties settings = new Properties();
    private static volatile boolean loaded;

    private ProgramFolder() {
    }

    /** Programmordner (beschreibbar); beim ersten Aufruf ermittelt. */
    public static Path dir() {
        Path d = dir;
        if (d == null) {
            synchronized (ProgramFolder.class) {
                if (dir == null) dir = detect();
                d = dir;
            }
        }
        return d;
    }

    /** {@code true}, wenn der Programmordner nicht beschreibbar war und der Ersatzordner im Benutzerprofil gilt. */
    public static boolean isFallback() {
        dir();
        return fallback;
    }

    /** Nur für Tests: Programmordner festlegen (setzt die geladenen Einstellungen zurück). */
    public static synchronized void useForTest(Path d) {
        dir = d;
        fallback = false;
        synchronized (settings) {
            settings.clear();
            loaded = false;
        }
    }

    private static Path detect() {
        String custom = System.getProperty("hts.programDir");
        if (custom != null && !custom.isBlank()) return Path.of(custom);
        Path home = Path.of(System.getProperty("user.home"), ".hts");
        Path jarDir = null;
        try {
            var src = ProgramFolder.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                Path p = Path.of(src.getLocation().toURI());
                if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar")) jarDir = p.getParent();
            }
        } catch (Exception | Error ignored) {
            // z. B. in der Entwicklungsumgebung
        }
        if (jarDir != null && writable(jarDir)) return jarDir;
        fallback = jarDir != null;
        try {
            Files.createDirectories(home);
        } catch (IOException ignored) {
            // dann eben ohne
        }
        return home;
    }

    static boolean writable(Path d) {
        try {
            Path probe = Files.createTempFile(d, ".hts-probe", ".tmp");
            Files.deleteIfExists(probe);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Beim Start: Ausrichter-Dateien aus dem bisherigen Ablageordner („Dokumente\HTS Turniere“) übernehmen, wenn im
     * Programmordner noch keine liegen. Die alten Dateien bleiben liegen.
     */
    public static void migrateFrom(Path oldStoreDir) {
        if (oldStoreDir == null) return;
        Path d = dir();
        if (Files.exists(d.resolve("ausrichter.properties"))) return;
        for (String n : List.of("ausrichter.properties", "ausrichter-logo.png")) {
            Path src = oldStoreDir.resolve(n);
            try {
                if (Files.isRegularFile(src)) Files.copy(src, d.resolve(n), StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException ignored) {
                // nicht kritisch
            }
        }
    }

    /**
     * „Einstellungen aus älterer Version übernehmen“: Ausrichter und Programmeinstellungen aus dem Programmordner einer
     * älteren HTS-Version kopieren (vorhandene werden ersetzt). Aus Versionen vor 1.8.0 liegen die Ausrichter-Dateien
     * noch im Turnierordner – der kann ebenso gewählt werden.
     *
     * @return Namen der übernommenen Dateien (leer = nichts gefunden)
     */
    public static List<String> carryOverFrom(Path older) throws IOException {
        List<String> done = new ArrayList<>();
        Path d = dir();
        if (older == null || older.toAbsolutePath().normalize().equals(d.toAbsolutePath().normalize())) return done;
        for (String n : CARRY_OVER) {
            Path src = older.resolve(n);
            if (Files.isRegularFile(src)) {
                Files.copy(src, d.resolve(n), StandardCopyOption.REPLACE_EXISTING);
                done.add(n);
            }
        }
        if (done.contains(SETTINGS_FILE)) {
            synchronized (settings) {
                loaded = false;
                settings.clear();
            }
        }
        return done;
    }

    // --- Programmeinstellungen ----------------------------------------------------------------------------

    private static Properties props() {
        synchronized (settings) {
            if (!loaded) {
                loaded = true;
                Path f = dir().resolve(SETTINGS_FILE);
                if (Files.isRegularFile(f)) {
                    try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                        settings.load(r);
                    } catch (IOException | IllegalArgumentException e) {
                        settings.clear();
                    }
                }
            }
            return settings;
        }
    }

    private static void put(String key, String value) {
        synchronized (settings) {
            Properties p = props();
            if (value == null || value.isBlank()) p.remove(key);
            else p.setProperty(key, value);
            try (Writer w = Files.newBufferedWriter(dir().resolve(SETTINGS_FILE), StandardCharsets.UTF_8)) {
                p.store(w, "HTS - Programmeinstellungen");
            } catch (IOException e) {
                de.hts.Log.warn("Programmeinstellungen nicht gespeichert: " + e.getMessage());
            }
        }
    }

    public static boolean isDarkMode() {
        return "true".equals(props().getProperty("dark"));
    }

    public static void setDarkMode(boolean on) {
        put("dark", on ? "true" : null);
    }

    /** Gewählter Ordner oder {@code null} (= Vorgabe). */
    public static Path chosen(String key) {
        String v = props().getProperty(key);
        return v == null || v.isBlank() ? null : Path.of(v);
    }

    public static void choose(String key, Path folder) {
        put(key, folder == null ? null : folder.toAbsolutePath().toString());
    }

    public static final String KEY_HTS = "dir-hts", KEY_PDF = "dir-pdf", KEY_XML = "dir-xml";

    /** Vorgabe „Dokumente\HTS“. */
    public static Path defaultHtsDir() {
        return StorageLocations.documents().resolve(HTS_DIR_NAME);
    }

    /** Vorgabe „Dokumente\HTS\Export files“. */
    public static Path defaultExportDir() {
        return defaultHtsDir().resolve(EXPORT_DIR_NAME);
    }

    /** Ordner der Turnierdateien (neue Turniere). */
    public static Path htsDir() {
        Path p = chosen(KEY_HTS);
        return p != null ? p : defaultHtsDir();
    }

    /** Ordner der PDF-Exporte (Turniere der neuen Ablage). */
    public static Path pdfDir() {
        Path p = chosen(KEY_PDF);
        return p != null ? p : defaultExportDir();
    }

    /** Ordner der XML-Exporte (click-TT). */
    public static Path xmlDir() {
        Path p = chosen(KEY_XML);
        return p != null ? p : defaultExportDir();
    }
}
