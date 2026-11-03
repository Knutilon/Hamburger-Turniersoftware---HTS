package de.hts.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Kleine Zeichenhilfe über PDFBox: Koordinaten von oben links (wie auf dem Bildschirm), Helvetica,
 * Texte werden auf eine Breite gekürzt („…“), nicht darstellbare Zeichen durch „?“ ersetzt.
 */
final class PdfCanvas {

    static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    enum Align { LEFT, CENTER, RIGHT }

    final PDDocument doc;
    final PDRectangle size;
    final float margin;
    private PDPageContentStream cs;
    /** aktuelle Schreibposition von oben (inkl. Rand) */
    float y;

    PdfCanvas(PDDocument doc, PDRectangle size, float margin) {
        this.doc = doc;
        this.size = size;
        this.margin = margin;
    }

    void newPage() {
        try {
            if (cs != null) cs.close();
            PDPage page = new PDPage(size);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = margin;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void finish() {
        try {
            if (cs != null) cs.close();
            cs = null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    float left() {
        return margin;
    }

    float width() {
        return size.getWidth() - 2 * margin;
    }

    float bottom() {
        return size.getHeight() - margin;
    }

    boolean fits(float height) {
        return y + height <= bottom();
    }

    // --- Text ------------------------------------------------------------------

    static float textWidth(String s, PDFont f, float pt) {
        try {
            return f.getStringWidth(clean(s, f)) / 1000f * pt;
        } catch (IOException e) {
            return s.length() * pt * 0.5f;
        }
    }

    /** Ersetzt Zeichen, die die Schrift nicht kennt (z. B. ł, ő), durch ihre Grundform bzw. „?“. */
    static String clean(String s, PDFont f) {
        if (s == null) return "";
        String norm = s.replace(' ', ' ').replace('\t', ' ').replace('\n', ' ');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            String ch = String.valueOf(c);
            if (canEncode(f, ch)) {
                sb.append(c);
            } else {
                String base = switch (c) {
                    case 'Ł' -> "L";
                    case 'ł' -> "l";
                    case 'Đ' -> "D";
                    case 'đ', 'ð' -> "d";
                    case 'ı' -> "i";
                    case 'Ħ' -> "H";
                    case 'ħ' -> "h";
                    default -> java.text.Normalizer.normalize(ch, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                };
                sb.append(!base.isEmpty() && canEncode(f, base) ? base : "?");
            }
        }
        return sb.toString();
    }

    private static boolean canEncode(PDFont f, String s) {
        try {
            f.encode(s);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Kürzt mit „…“ auf die Breite. */
    static String fit(String s, PDFont f, float pt, float maxWidth) {
        String t = clean(s, f);
        if (maxWidth <= 0 || textWidth(t, f, pt) <= maxWidth) return t;
        while (!t.isEmpty() && textWidth(t + "…", f, pt) > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    /** Text mit Oberkante {@code top}; liefert die Zeilenhöhe. */
    float text(float x, float top, String s, PDFont f, float pt, float maxWidth, Align align, float gray) {
        String t = fit(s, f, pt, maxWidth);
        float w = textWidth(t, f, pt);
        float dx = switch (align) {
            case LEFT -> 0;
            case CENTER -> (maxWidth - w) / 2;
            case RIGHT -> maxWidth - w;
        };
        try {
            cs.beginText();
            cs.setNonStrokingColor(gray, gray, gray);
            cs.setFont(f, pt);
            cs.newLineAtOffset(x + Math.max(0, dx), size.getHeight() - top - pt * 0.8f);
            cs.showText(t);
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return pt * 1.25f;
    }

    float text(float x, float top, String s, PDFont f, float pt, float maxWidth) {
        return text(x, top, s, f, pt, maxWidth, Align.LEFT, 0f);
    }

    /** Fließtext an der aktuellen Position mit Zeilenumbruch; rückt {@link #y} weiter. */
    void paragraph(String s, PDFont f, float pt, float maxWidth) {
        String[] words = clean(s, f).split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            if (textWidth(candidate, f, pt) > maxWidth && !line.isEmpty()) {
                y += text(left(), y, line.toString(), f, pt, maxWidth);
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) y += text(left(), y, line.toString(), f, pt, maxWidth);
    }

    // --- Linien und Kästen ----------------------------------------------------

    void line(float x1, float y1, float x2, float y2, float width) {
        try {
            cs.setLineWidth(width);
            cs.moveTo(x1, size.getHeight() - y1);
            cs.lineTo(x2, size.getHeight() - y2);
            cs.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void rect(float x, float top, float w, float h, float lineWidth) {
        try {
            cs.setLineWidth(lineWidth);
            cs.addRect(x, size.getHeight() - top - h, w, h);
            cs.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void fill(float x, float top, float w, float h, float gray) {
        try {
            cs.setNonStrokingColor(gray, gray, gray);
            cs.addRect(x, size.getHeight() - top - h, w, h);
            cs.fill();
            cs.setNonStrokingColor(0f, 0f, 0f);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
