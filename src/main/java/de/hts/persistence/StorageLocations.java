/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import de.hts.tournament.Tournament;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Standard-Speicherorte:
 * <pre>
 * Dokumente\HTS Turniere\                 Stammordner
 *     &lt;Turniername&gt;\                       je Turnier
 *         Turnierdatei\                    die .hts-Datei
 *         XML Export\                      click-TT-Ergebnisdatei
 *         PDF Export\                      alle PDFs (Gesamtergebnis, Phasen, Endergebnis, Spielzettel)
 * </pre>
 * Abweichende Ordner werden im Turnier gespeichert und gelten, bis sie erneut geändert werden.
 */
public final class StorageLocations {

    public static final String ROOT_NAME = "HTS Turniere";
    public static final String FILE_DIR = "Turnierdatei";
    public static final String XML_DIR = "XML Export";
    public static final String PDF_DIR = "PDF Export";

    private static volatile Path root;

    private StorageLocations() {
    }

    /** Windows-Ordner „Dokumente“ (auch bei OneDrive-Umleitung), sonst Benutzerordner\Documents. */
    public static Path documents() {
        try {
            java.io.File d = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();
            if (d != null && d.isDirectory()) return d.toPath();
        } catch (RuntimeException | Error ignored) {
            // ohne Desktop-Modul / headless
        }
        Path home = Path.of(System.getProperty("user.home"));
        for (String n : new String[]{"Documents", "Dokumente"}) {
            if (Files.isDirectory(home.resolve(n))) return home.resolve(n);
        }
        return home;
    }

    /** Stammordner „Dokumente\HTS Turniere“ (per Systemeigenschaft {@code hts.dataDir} änderbar). */
    public static Path defaultRoot() {
        String custom = System.getProperty("hts.dataDir");
        return custom != null && !custom.isBlank() ? Path.of(custom) : documents().resolve(ROOT_NAME);
    }

    public static Path root() {
        Path r = root;
        return r != null ? r : defaultRoot();
    }

    static void setRoot(Path r) {
        root = r;
    }

    /** Turniername als Ordner-/Dateiname (ohne \ / : * ? " &lt; &gt; |). */
    public static String safeName(String name) {
        String s = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        s = s.replaceAll("[. ]+$", "");
        if (s.length() > 80) s = s.substring(0, 80).trim();
        return s.isEmpty() ? "Turnier" : s;
    }

    /** 1.8.0 (#152): Ordner neuer Turniere (neue Ablage) – {@code null}, wenn die Ablage die alte Ordnerstruktur nutzt. */
    private static volatile java.util.function.Supplier<Path> flat;

    static void setFlat(java.util.function.Supplier<Path> s) {
        flat = s;
    }

    /** Ordner, in den neue Turnierdateien kommen ({@code null} = alte Ordnerstruktur „HTS Turniere\Name\Turnierdatei“). */
    public static Path newTournamentDir() {
        java.util.function.Supplier<Path> s = flat;
        return s == null ? null : s.get();
    }

    /** Turnierordner: festgelegter Ordner oder Stammordner\Turniername. */
    public static Path tournamentFolder(Tournament t) {
        if (t.getFolder() != null) return Path.of(t.getFolder());
        return root().resolve(safeName(t.getName()));
    }

    public static Path pdfDir(Tournament t) {
        if (t.getPdfDir() != null) return Path.of(t.getPdfDir());
        return t.isFlatLayout() ? ProgramFolder.pdfDir() : tournamentFolder(t).resolve(PDF_DIR);
    }

    public static Path xmlDir(Tournament t) {
        if (t.getXmlDir() != null) return Path.of(t.getXmlDir());
        return t.isFlatLayout() ? ProgramFolder.xmlDir() : tournamentFolder(t).resolve(XML_DIR);
    }

    private static Path defaultPdfDir(Tournament t) {
        return t.isFlatLayout() ? ProgramFolder.pdfDir() : tournamentFolder(t).resolve(PDF_DIR);
    }

    private static Path defaultXmlDir(Tournament t) {
        return t.isFlatLayout() ? ProgramFolder.xmlDir() : tournamentFolder(t).resolve(XML_DIR);
    }

    /**
     * 1.8.0 (#152): Dateiname für einen Export. In der neuen Ablage liegen die Exporte aller Turniere in einem Ordner –
     * dann beginnt der Name mit Turniername und Datum (z. B. „Herbstturnier 2026-11-28 - Endergebnis Herren C.pdf“), und
     * bei gleichem Namen wird „(2)“, „(3)“ … angehängt.
     */
    public static String exportFileName(Tournament t, Path dir, String fileName) {
        String fn = fileName == null || fileName.isBlank() ? "Export" : fileName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (t == null || !t.isFlatLayout()) return fn;
        String prefix = safeName(t.getName()) + (t.getDate() == null ? "" : " " + t.getDate());
        if (!fn.startsWith(prefix)) fn = prefix + " - " + fn;
        if (dir == null) return fn;
        int dot = fn.lastIndexOf('.');
        String stem = dot > 0 ? fn.substring(0, dot) : fn, ext = dot > 0 ? fn.substring(dot) : "";
        String candidate = fn;
        for (int k = 2; Files.exists(dir.resolve(candidate)) && k < 1000; k++) candidate = stem + " (" + k + ")" + ext;
        return candidate;
    }

    /** Name der click-TT-Ergebnisdatei: „Ergebnisse - &lt;erste 20 Zeichen des Turniernamens&gt; - DD_MM_YYYY.xml“. */
    public static String resultsFileName(Tournament t) {
        String n = t.getName() == null ? "" : t.getName().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
        if (n.length() > 20) n = n.substring(0, 20).trim();
        StringBuilder sb = new StringBuilder("Ergebnisse");
        if (!n.isEmpty()) sb.append(" - ").append(n);
        if (t.getDate() != null) sb.append(" - ").append(t.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd_MM_yyyy")));
        return sb.append(".xml").toString();
    }

    /** Ordner anlegen (falls nötig) und zurückgeben; schlägt das fehl, der Benutzerordner. */
    public static java.io.File ensure(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir.toFile();
        } catch (IOException | RuntimeException e) {
            return new java.io.File(System.getProperty("user.home"));
        }
    }

    /** Merkt sich einen im Speichern-Dialog gewählten Ordner als PDF-Ordner des Turniers (Standard: zurücksetzen). */
    public static void rememberPdfDir(Tournament t, Path chosen) {
        if (t == null || chosen == null) return;
        t.setPdfDir(chosen.equals(defaultPdfDir(t)) ? null : chosen.toString());
    }

    public static void rememberXmlDir(Tournament t, Path chosen) {
        if (t == null || chosen == null) return;
        t.setXmlDir(chosen.equals(defaultXmlDir(t)) ? null : chosen.toString());
    }
}
