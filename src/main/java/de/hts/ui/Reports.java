package de.hts.ui;

import de.hts.report.PdfReports;
import de.hts.tournament.Match;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.application.Platform;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import java.awt.print.PrinterJob;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * PDF-Ansichten (im eingebauten Viewer mit Speichern und Drucken) und der automatische Druck der Spielzettel.
 */
final class Reports {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Reports.class);
    /** Druckaufträge nacheinander im Hintergrund, damit die Oberfläche nicht hängt. */
    private static final ExecutorService PRINTER = Executors.newSingleThreadExecutor(r -> {
        Thread th = new Thread(r, "HTS-Druck");
        th.setDaemon(true);
        return th;
    });

    private Reports() {
    }

    /** Spielzettel automatisch drucken, sobald ein Spiel an einen Tisch geht (Einstellung je Rechner, Standard: an). */
    static boolean isAutoPrintSlips() {
        return PREFS.getBoolean("autoPrintSlips", true);
    }

    static void setAutoPrintSlips(boolean on) {
        PREFS.putBoolean("autoPrintSlips", on);
    }

    // --- Ansichten -------------------------------------------------------------

    static void showSlip(Window owner, Tournament t, Match m, Consumer<String> status) {
        PdfViewer.show(owner, t, "Spielzettel – " + (m.getTable() == null ? "Tisch -" : t.tableName(m.getTable())) + ", Spiel " + m.getNumber(),
                PdfReports.slip(t, m), fileName("Spielzettel Spiel " + m.getNumber()), status);
    }

    static void showGroup(Window owner, Tournament t, PlayingClass phase, int g, Consumer<String> status) {
        String name = t.rootOf(phase).getName() + " – Phase " + t.phaseNumber(phase) + " – Gruppe " + de.hts.tournament.GroupDraw.groupName(g);
        PdfViewer.show(owner, t, name, PdfReports.group(t, phase, g), fileName(name), status);
    }

    static void showPhase(Window owner, Tournament t, PlayingClass phase, Consumer<String> status) {
        String name = t.rootOf(phase).getName() + " – Phase " + t.phaseNumber(phase);
        PdfViewer.show(owner, t, name, PdfReports.phase(t, phase), fileName(name), status);
    }

    /** 0.38 (#54): Liste der endgültig Abgemeldeten. */
    static void showWithdrawn(Window owner, Tournament t, Consumer<String> status) {
        String name = "Abgemeldete " + (t.getName() == null ? "Turnier" : t.getName());
        PdfViewer.show(owner, t, name, PdfReports.withdrawnList(t), fileName(name), status);
    }

    static void showTournamentResults(Window owner, Tournament t, Consumer<String> status) {
        String name = "Ergebnisse " + (t.getName() == null ? "Turnier" : t.getName());
        PdfViewer.show(owner, t, name, PdfReports.tournamentResults(t), fileName(name), status);
    }

    static void showFinalResult(Window owner, Tournament t, PlayingClass c, Consumer<String> status) {
        String name = "Endergebnis " + t.rootOf(c).getName();
        PdfViewer.show(owner, t, name, PdfReports.finalResult(t, c), fileName(name), status);
    }

    private static String fileName(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf";
    }

    // --- automatischer Druck -----------------------------------------------------

    /** Druckt den Spielzettel ohne Rückfrage auf dem Standarddrucker (A6, falls der Drucker es kann). */
    static void printSlipSilently(Tournament t, Match m, Consumer<String> status) {
        PDDocument doc = PdfReports.slip(t, m);   // Daten auf dem FX-Thread lesen
        String job = "Spielzettel " + t.tableName(m.getTable()) + " – Spiel " + m.getNumber();
        PRINTER.submit(() -> {
            String msg;
            try (doc) {
                javax.print.PrintService ps = PrintServiceLookup.lookupDefaultPrintService();
                if (ps == null) {
                    msg = "Spielzettel nicht gedruckt: kein Standarddrucker eingerichtet.";
                } else {
                    PrinterJob pj = PrinterJob.getPrinterJob();
                    pj.setPrintService(ps);
                    pj.setPageable(new PDFPageable(doc));
                    PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
                    attrs.add(new JobName(job, null));
                    boolean a6 = ps.isAttributeValueSupported(MediaSizeName.ISO_A6, null, null);
                    if (a6) attrs.add(MediaSizeName.ISO_A6);
                    pj.print(attrs);
                    msg = job + " gedruckt (" + ps.getName() + (a6 ? ", A6)." : ", Drucker kennt kein A6 – Standardpapier).");
                }
            } catch (Exception e) {
                msg = "Spielzettel konnte nicht gedruckt werden: " + e.getMessage();
            }
            String fin = msg;
            Platform.runLater(() -> status.accept(fin));
        });
    }

    /** Druckdialog für ein Dokument (im Hintergrund); das Dokument wird danach geschlossen. */
    static void printWithDialog(PDDocument doc, String jobName, Consumer<String> status) {
        PRINTER.submit(() -> {
            String msg;
            try (doc) {
                PrinterJob pj = PrinterJob.getPrinterJob();
                pj.setJobName(jobName);
                pj.setPageable(new PDFPageable(doc));
                if (pj.printDialog()) {
                    pj.print();
                    msg = "„" + jobName + "“ gedruckt.";
                } else {
                    msg = "Drucken abgebrochen.";
                }
            } catch (Exception e) {
                msg = "Drucken fehlgeschlagen: " + e.getMessage();
            }
            String fin = msg;
            Platform.runLater(() -> status.accept(fin));
        });
    }
}
