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

    /** Turnierordner: festgelegter Ordner oder Stammordner\Turniername. */
    public static Path tournamentFolder(Tournament t) {
        if (t.getFolder() != null) return Path.of(t.getFolder());
        return root().resolve(safeName(t.getName()));
    }

    public static Path pdfDir(Tournament t) {
        return t.getPdfDir() != null ? Path.of(t.getPdfDir()) : tournamentFolder(t).resolve(PDF_DIR);
    }

    public static Path xmlDir(Tournament t) {
        return t.getXmlDir() != null ? Path.of(t.getXmlDir()) : tournamentFolder(t).resolve(XML_DIR);
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
        t.setPdfDir(chosen.equals(tournamentFolder(t).resolve(PDF_DIR)) ? null : chosen.toString());
    }

    public static void rememberXmlDir(Tournament t, Path chosen) {
        if (t == null || chosen == null) return;
        t.setXmlDir(chosen.equals(tournamentFolder(t).resolve(XML_DIR)) ? null : chosen.toString());
    }
}
