/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 1.7.0 (#140): Protokoll für die Analyse von Abstürzen. Eine Datei je Tag im Ablageordner der Turniere unter
 * „Protokoll“ ({@code hts-JJJJ-MM-TT.log}), Aufbewahrung 30 Tage, höchstens 5 MB je Datei.
 * <p>
 * Datenschutz: keine Namen, Vereine oder Ergebnisse – nur Kennungen (Spiel-Nr., Klassenname) und technische Angaben.
 * Ohne Ordner (Tests, Live-Server) wird nur auf die Fehlerausgabe geschrieben.
 */
public final class Log {

    static final int KEEP_DAYS = 30;
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static volatile Path dir;
    private static final Object LOCK = new Object();
    private static boolean capNoted;

    private Log() {
    }

    /** Protokollordner festlegen (Ablageordner/Protokoll) und alte Dateien löschen. */
    public static void init(Path storeDirectory) {
        try {
            Path d = storeDirectory.resolve("Protokoll");
            Files.createDirectories(d);
            dir = d;
            cleanUp(d, LocalDate.now());
        } catch (IOException | RuntimeException e) {
            System.err.println("Protokoll nicht verfügbar: " + e.getMessage());
        }
    }

    public static Path directory() {
        return dir;
    }

    public static void info(String msg) {
        write("INFO ", msg, null);
    }

    public static void warn(String msg) {
        write("WARN ", msg, null);
    }

    public static void error(String msg, Throwable t) {
        write("FEHLER", msg, t);
    }

    static Path fileFor(Path d, LocalDate day) {
        return d.resolve("hts-" + day + ".log");
    }

    private static void write(String level, String msg, Throwable t) {
        StringBuilder b = new StringBuilder();
        b.append(LocalDateTime.now().format(TIME)).append(' ').append(level).append(" [").append(Thread.currentThread().getName())
                .append("] ").append(msg).append(System.lineSeparator());
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            b.append(sw);
        }
        Path d = dir;
        if (d == null) {
            if (t != null || !"INFO ".equals(level)) System.err.print(b);
            return;
        }
        synchronized (LOCK) {
            try {
                Path f = fileFor(d, LocalDate.now());
                long size = Files.exists(f) ? Files.size(f) : 0;
                if (size > MAX_BYTES) {
                    if (!capNoted) {
                        capNoted = true;
                        Files.writeString(f, "… Protokoll für heute voll (" + MAX_BYTES / 1024 / 1024 + " MB) – weitere Einträge entfallen."
                                + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                    }
                    return;
                }
                Files.writeString(f, b, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.print(b);
            }
        }
    }

    /** Dateien älter als 30 Tage löschen (Protokolle und Absturzberichte der Java-Laufzeit). */
    static void cleanUp(Path d, LocalDate today) throws IOException {
        LocalDate limit = today.minusDays(KEEP_DAYS);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
            for (Path f : ds) {
                String n = f.getFileName().toString();
                if (n.matches("hts-\\d{4}-\\d{2}-\\d{2}\\.log")) {
                    if (LocalDate.parse(n.substring(4, 14)).isBefore(limit)) Files.deleteIfExists(f);
                } else if (n.startsWith("hs_err") && n.endsWith(".log")) {
                    if (Files.getLastModifiedTime(f).toInstant().isBefore(limit.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())) {
                        Files.deleteIfExists(f);
                    }
                }
            }
        }
    }

    /** Fehlerbericht: ZIP der Protokolle (und Absturzberichte) der letzten {@code days} Tage. */
    public static void writeReport(OutputStream target, int days) throws IOException {
        Path d = dir;
        LocalDate limit = LocalDate.now().minusDays(days);
        List<Path> files = new ArrayList<>();
        if (d != null) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                for (Path f : ds) {
                    String n = f.getFileName().toString();
                    boolean log = n.matches("hts-\\d{4}-\\d{2}-\\d{2}\\.log") && !LocalDate.parse(n.substring(4, 14)).isBefore(limit);
                    boolean crash = n.startsWith("hs_err") && n.endsWith(".log");
                    if (log || crash) files.add(f);
                }
            }
        }
        // Absturzberichte der Java-Laufzeit (Startdateien: -XX:ErrorFile=~/.hts/hs_err_pid….log)
        Path jvm = Path.of(System.getProperty("user.home"), ".hts");
        if (Files.isDirectory(jvm)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(jvm, "hs_err*.log")) {
                for (Path f : ds) {
                    if (!Files.getLastModifiedTime(f).toInstant().isBefore(limit.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())) files.add(f);
                }
            }
        }
        files.sort(null);
        try (ZipOutputStream zip = new ZipOutputStream(target, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("LIESMICH.txt"));
            zip.write(("HTS-Fehlerbericht " + HtsApp.VERSION + " vom " + LocalDateTime.now().withNano(0) + System.lineSeparator()
                    + "Enthält nur technische Angaben (keine Namen, Vereine oder Ergebnisse)." + System.lineSeparator()
                    + "Bitte an ein GitHub-Issue anhängen: " + HtsApp.REPO_URL + "/issues" + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (Path f : files) {
                zip.putNextEntry(new ZipEntry(f.getFileName().toString()));
                Files.copy(f, zip);
                zip.closeEntry();
            }
        }
    }

    /** Programmstart: Version, System, Java, Bildschirm, Speicher. */
    public static void startup(String screen) {
        Runtime r = Runtime.getRuntime();
        info("HTS " + HtsApp.VERSION + " gestartet · " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ") · Java " + System.getProperty("java.version") + " "
                + System.getProperty("java.vendor", "") + " · Bildschirm " + screen + " · Speicher max. " + r.maxMemory() / 1024 / 1024
                + " MB, " + r.availableProcessors() + " Kerne");
    }
}
