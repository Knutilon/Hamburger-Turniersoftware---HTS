/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.report;

import de.hts.tournament.Entrant;
import de.hts.tournament.KnockoutBracket;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.hts.report.PdfCanvas.BOLD;
import static de.hts.report.PdfCanvas.REGULAR;

/**
 * Turnierbaum eines KO-Systems im PDF (A4 quer): jede Runde eine Spalte, Kästen mit „Spiel n“, ausgeschriebenen
 * Namen („Nachname, Vorname“, Verein klein grau), Doppel je Person eine Zeile, gewonnene Sätze rechts (Gewinner fett,
 * Verlierer grau), darunter klein die Satzergebnisse. Freilose mit Kasten und Hinweis „Freilos“; das Spiel um Platz 3
 * steht unter dem Finale. Passt das Feld nicht auf eine Seite, wird es in Teilbäume aufgeteilt.
 */
final class KoTree {

    private static final float GAP_X = 18;
    private float setsH = 9;      // Zeile für die Satzergebnisse unter dem Kasten
    private float space = 6;      // Abstand zwischen zwei Kästen
    private final float basePt;

    private final PdfCanvas c;
    private final Tournament t;
    private final PlayingClass phase;
    private final String title;
    private final List<KnockoutBracket.Round> rounds;
    private final Map<String, Match> byPos = new HashMap<>();
    private final boolean doubles;
    private float pt, line, boxH;
    private float scale = 1f;

    private KoTree(PdfCanvas c, Tournament t, PlayingClass phase, String title, List<Entrant> slots) {
        this.c = c;
        this.t = t;
        this.phase = phase;
        this.title = title;
        this.rounds = new KnockoutBracket(slots).getRounds();
        for (Match m : t.matchesOf(phase)) byPos.put(m.getKoRound() + ":" + m.getKoIndex(), m);
        this.doubles = slots.stream().anyMatch(e -> e != null && e.id().startsWith("pair:"));
        this.basePt = doubles ? 7.4f : 8.2f;
        applyScale(1f);
    }

    /** Schrift und Abstände verkleinern (höchstens bis 72 %), damit ein Baum auf eine Seite passt. */
    private void applyScale(float f) {
        scale = f;
        pt = basePt * f;
        line = (basePt + 2.4f) * f;
        boxH = (11 + 4) * Math.max(f, 0.85f) + (doubles ? 4 : 2) * line;
        setsH = 9 * f;
        space = 6 * f;
    }

    static void draw(PdfCanvas c, Tournament t, PlayingClass phase, String title) {
        List<Entrant> slots = t.drawSlots(phase);
        if (slots.size() < 2) return;
        KoTree tree = new KoTree(c, t, phase, title, slots);
        tree.layout(0, 0, tree.rounds.get(0).matches().size(), 1, 1);
    }

    private float slot() {
        return boxH + setsH + space;
    }

    /** freie Höhe einer Seite unterhalb von Kopf und Rundentitel (Kopf: Turnier, Titel, Untertitel, Linie) */
    private float available() {
        return c.bottom() - 4 - (topMargin + HEADER_H + 16);
    }

    private static final float HEADER_H = 58;
    private final float topMargin = 40;

    /**
     * Zeichnet ab Runde {@code r0} die {@code count} Spiele ab Index {@code i0} samt allen Folgerunden bis zum Finale –
     * passt das nicht auf eine Seite, zuerst Teilbäume, dann die restlichen Runden.
     */
    private void layout(int r0, int i0, int count, int part, int parts) {
        int last = rounds.size() - 1;
        applyScale(1f);
        if (count * slot() > available()) {
            // etwas kleiner setzen, wenn das reicht – sonst in normaler Größe auf mehrere Seiten aufteilen
            float f = available() / (count * slot());
            if (f >= 0.75f) {
                applyScale(Math.max(0.75f, f));
                while (scale > 0.75f && count * slot() > available()) applyScale(scale - 0.01f);
                if (count * slot() > available()) applyScale(1f);
            }
        }
        int maxFit = 1;
        while (maxFit * 2 <= count && maxFit * 2 * slot() <= available()) maxFit *= 2;
        if (count * slot() <= available() || maxFit >= count) {
            page(r0, last, i0, count, true, parts > 1 ? "Turnierbaum – Finalrunden" : "Turnierbaum");
            return;
        }
        int depth = Integer.numberOfTrailingZeros(maxFit);
        int chunks = count / maxFit;
        for (int k = 0; k < chunks; k++) {
            page(r0, r0 + depth, i0 + k * maxFit, maxFit, false, "Turnierbaum – Teil " + (k + 1) + " von " + chunks);
        }
        layout(r0 + depth, i0 / maxFit, chunks, 2, 2);
    }

    /** Eine Seite: Runden {@code rFrom..rTo}, in Runde rFrom {@code count} Spiele ab Index {@code i0}. */
    private void page(int rFrom, int rTo, int i0, int count, boolean withFinal, String sub) {
        c.newPage();
        PdfReports.header(c, t, title, sub);
        int cols = rTo - rFrom + 1;
        float colW = Math.min(240, (c.width() - GAP_X * (cols - 1)) / cols);
        float top = c.y + 16, bottom = c.bottom() - 4;
        float unit = (bottom - top) / count;
        for (int r = rFrom; r <= rTo; r++) {
            int n = count >> (r - rFrom);
            if (n < 1) break;
            float x = c.left() + (r - rFrom) * (colW + GAP_X);
            c.text(x, c.y, rounds.get(r).name(), BOLD, 9.5f, colW);
            float span = unit * (1 << (r - rFrom));
            int base = i0 >> (r - rFrom);
            for (int k = 0; k < n; k++) {
                int idx = base + k;
                float cy = top + span * k + span / 2;
                box(rounds.get(r).matches().get(idx), byPos.get(r + ":" + idx), x, cy - (boxH + setsH) / 2, colW);
                if (r < rTo) {        // Verbindung zur nächsten Runde
                    float x1 = x + colW, xm = x1 + GAP_X / 2;
                    float other = top + span * (k ^ 1) + span / 2;
                    float mid = cy - setsH / 2;
                    c.line(x1, mid, xm, mid, 0.5f);
                    if (k % 2 == 0) {
                        float omid = other - setsH / 2;
                        c.line(xm, mid, xm, omid, 0.5f);
                        c.line(xm, (mid + omid) / 2, x1 + GAP_X, (mid + omid) / 2, 0.5f);
                    }
                }
            }
        }
        Match third = t.thirdPlaceMatch(phase);
        if (withFinal && third != null) {
            float x = c.left() + (rTo - rFrom) * (colW + GAP_X);
            float y0 = bottom - boxH - setsH - 2;
            c.text(x, y0 - 13, "Spiel um Platz 3", BOLD, 9.5f, colW);
            box(null, third, x, y0, colW);
        }
    }

    private void box(KnockoutBracket.Match bm, Match m, float x, float y0, float w) {
        c.rect(x, y0, w, boxH, 0.6f);
        boolean bye = bm != null && bm.isBye();
        String head = bye ? "Freilos" : "Spiel " + (m != null ? m.getNumber() : bm.number());
        c.text(x + 4, y0 + 2.5f, head, bye ? BOLD : REGULAR, 6.5f, w - 8, PdfCanvas.Align.LEFT, 0.45f);
        float yy = y0 + 11.5f;
        boolean done = m != null && m.getStatus() == MatchStatus.DONE;
        String winner = m == null ? null : m.winner();
        for (int side = 0; side < 2; side++) {
            String id = m != null ? (side == 0 ? m.getA() : m.getB()) : null;
            List<String[]> ls = new ArrayList<>();
            if (id != null) ls = PdfReports.withHandicap(t, m, id, t.partyLines(m, id));
            else if (bm != null) {
                Entrant e = side == 0 ? bm.a().entrant() : bm.b().entrant();
                if (e != null) {
                    List<String[]> pl = t.personLines(e.id());
                    ls = pl != null ? pl : List.<String[]>of(new String[]{e.name(), e.detail()});
                }
            }
            boolean win = done && id != null && id.equals(winner);
            float sy = yy;
            if (ls.isEmpty()) {
                String ph = bye ? "Freilos" : m != null && m.getPlacement() == 3 ? t.thirdPlaceLabel(phase, side)
                        : bm != null ? (side == 0 ? bm.a() : bm.b()).label() : "";
                c.text(x + 4, yy, ph, REGULAR, pt, w - 30, PdfCanvas.Align.LEFT, 0.55f);
                yy += line * (doubles ? 2 : 1);
            } else {
                float gray = done && !win ? 0.4f : 0f;
                for (String[] l : ls) {
                    PdfReports.nameWithClub(c, x + 4, yy, l[0], l[1], win, pt, w - 26, gray);
                    yy += line;
                }
                if (doubles && ls.size() == 1) yy += line;
            }
            if (done) {
                c.text(x + w - 18, sy + (doubles ? line / 2 : 0), String.valueOf(side == 0 ? m.getSetsA() : m.getSetsB()),
                        win ? BOLD : REGULAR, pt + 1.2f, 14, PdfCanvas.Align.RIGHT, win ? 0f : 0.4f);
            }
            if (side == 0) {
                c.line(x + 4, yy + 0.5f, x + w - 4, yy + 0.5f, 0.3f);
                yy += 2.5f;
            }
        }
        if (done && !m.getSetsText().isEmpty()) {
            c.text(x + 2, y0 + boxH + 1.5f, m.getSetsText(), REGULAR, 6.5f, w - 4, PdfCanvas.Align.LEFT, 0.35f);
        }
    }
}
