/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.report;

import de.hts.tournament.Tournament;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.4.0 (#113): Urkunden (DIN A4 hoch) nach der Vorlage des HaTTV. Die Stellen sind der Vorlage nachgemessen
 * (794 × 1123 Bildpunkte bei 96 dpi, links und rechts 2,5 cm Rand, oben 5 cm für vorgedrucktes Briefpapier).
 * Leere Texte bleiben leer; nichts wird mit „…“ abgekürzt, zu lange Namen werden kleiner gesetzt.
 */
public final class CertificateReport {

    private static final String FONT_RESOURCE = "/de/hts/fonts/ComicNeue-Bold.ttf";
    /** Bildpunkte der Vorlage (96 dpi) in PDF-Punkte. */
    private static final float PX = 0.75f;
    private static final float PAGE_W = 794 * PX;
    private static final float TEXT_W = (794 - 2 * 94.5f) * PX;
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);

    private CertificateReport() {
    }

    /** Datum wie auf der Urkunde, z. B. „12. September 2026“; leer, wenn kein Datum. */
    public static String dateText(LocalDate d) {
        return d == null ? "" : d.format(LONG_DATE);
    }

    /** Eine Seite je Urkunde, in der Reihenfolge der Liste. Ohne Einträge entsteht eine leere A4-Seite. */
    public static PDDocument certificates(Tournament t, List<Tournament.CertEntry> entries) {
        PDDocument doc = new PDDocument();
        PDDocumentInformation info = doc.getDocumentInformation();
        info.setTitle("Urkunden " + (t.getName() == null ? "" : t.getName()));
        info.setCreator("HTS – Hamburger Turniersoftware");
        PDFont comic = loadComic(doc);
        PdfCanvas c = new PdfCanvas(doc, PDRectangle.A4, 0);
        if (entries.isEmpty()) c.newPage();
        for (Tournament.CertEntry e : entries) {
            c.newPage();
            page(c, t, e, comic);
        }
        c.finish();
        return settled(doc);
    }

    /**
     * PDFBox bettet die Teilmenge der Schrift erst beim Speichern ein; Anzeige und Druck rendern aber direkt aus dem
     * Dokument. Deshalb wird es einmal gespeichert und neu geladen – dann enthält es die eingebettete Schrift.
     */
    private static PDDocument settled(PDDocument doc) {
        try (doc) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.save(out);
            return org.apache.pdfbox.Loader.loadPDF(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PDFont loadComic(PDDocument doc) {
        try (InputStream in = CertificateReport.class.getResourceAsStream(FONT_RESOURCE)) {
            if (in == null) return PdfCanvas.BOLD;
            return PDType0Font.load(doc, in, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Mitte einer Zeile bei {@code cy} (Bildpunkte), Schrift {@code pt}, auf Textbreite verkleinert. */
    private static void centered(PdfCanvas c, String s, PDFont f, float pt, float cyPx, float maxW) {
        if (s == null || s.isBlank()) return;
        float size = pt;
        while (size > 8 && PdfCanvas.textWidth(PdfCanvas.clean(s, f), f, size) > maxW) size -= 0.5f;
        c.text(0, cyPx * PX - 0.45f * size, s, f, size, PAGE_W, PdfCanvas.Align.CENTER, 0f);
    }

    private static void page(PdfCanvas c, Tournament t, Tournament.CertEntry e, PDFont comic) {
        float up = t.isCertLeaveTop() ? 0 : 150;          // ohne vorgedrucktes Briefpapier rückt der Text nach oben
        PDFont arial = PdfCanvas.REGULAR, arialBold = PdfCanvas.BOLD;
        centered(c, t.getCertTitle(), arial, 50, 284 - up, TEXT_W);
        centered(c, e.name(), comic, 22, 411 - up, TEXT_W);
        centered(c, e.club(), comic, 18, 444 - up, TEXT_W);
        List<String> lines = wrap(t.getName() == null ? "" : t.getName(), arial, 18, TEXT_W, 2);
        if (lines.size() == 1) centered(c, lines.get(0), arial, 18, 570 - up, TEXT_W);
        else if (lines.size() >= 2) {
            centered(c, lines.get(0), arial, 18, 558 - up, TEXT_W);
            centered(c, lines.get(1), arial, 18, 583 - up, TEXT_W);
        }
        centered(c, "in der Altersklasse " + e.className(), arial, 18, 631 - up, TEXT_W);
        centered(c, "den " + e.place() + ". Platz", arialBold, 24, 717 - up, TEXT_W);
        centered(c, "Als Anerkennung", arial, 18, 787 - up, TEXT_W);
        centered(c, "wird diese Urkunde überreicht", arial, 18, 832 - up, TEXT_W);
        String date = dateText(t.getDate());
        String place = t.getCertPlace();
        String when = (place.isBlank() ? "" : place + (date.isEmpty() ? "" : ", ")) + (date.isEmpty() ? "" : "den " + date);
        float lineCy = 929 * PX;
        float pt = 14;
        if (!when.isBlank()) {
            c.text(106 * PX, lineCy - 0.45f * pt, when, arial, pt, (538 - 106 - 12) * PX, PdfCanvas.Align.LEFT, 0f);
        }
        if (!t.getCertSigner().isBlank()) {
            c.text(538 * PX, lineCy - 0.45f * pt, t.getCertSigner(), arial, pt, (794 - 94.5f - 538) * PX, PdfCanvas.Align.LEFT, 0f);
        }
        centered(c, t.getCertHost(), arialBold, 20, 1013, TEXT_W);
    }

    /** Bricht {@code s} in höchstens {@code maxLines} Zeilen um; passt es nicht, wird die Schrift nie gekürzt, sondern die Zeile länger gesetzt (und dann verkleinert). */
    private static List<String> wrap(String s, PDFont f, float pt, float maxW, int maxLines) {
        List<String> out = new ArrayList<>();
        String text = PdfCanvas.clean(s, f).strip();
        if (text.isEmpty()) return out;
        String[] words = text.split(" +");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String cand = cur.length() == 0 ? w : cur + " " + w;
            if (cur.length() > 0 && PdfCanvas.textWidth(cand, f, pt) > maxW && out.size() < maxLines - 1) {
                out.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(cand);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
