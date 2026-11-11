/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.report;

import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportsTest {

    private static Tournament tournament(int n, int groupSize) {
        Tournament t = new Tournament();
        t.setName("Testturnier");
        t.setTableCount(6);
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("Jürgen" + i, i == 1 ? "Łukasiewicz" : "Weiß" + i);   // Ł gibt es in Helvetica nicht
            p.setInternalNr("P" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1800 - i * 10);
            p.setClubName("TSV Groß Flottbek");
            ps.add(p);
        }
        t.merge(ps);
        PlayingClass c = t.addClass(new PlayingClass("Jungen U19"));
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(true);
        c.setGroupSize(groupSize);
        t.drawGroups(c, true, new Random(1));
        t.startClass(c);
        return t;
    }

    private static String text(PDDocument d) throws Exception {
        try (d) {
            return new PDFTextStripper().getText(d);
        }
    }

    @Test
    void spielzettelEnthaeltTischKonkurrenzUndNamen() throws Exception {
        Tournament t = tournament(8, 4);
        Match m = t.activeMatchAt(1).orElseThrow();
        PDDocument d = PdfReports.slip(t, m);
        assertEquals(1, d.getNumberOfPages());
        assertTrue(Math.abs(PdfReports.A6.getWidth() - d.getPage(0).getMediaBox().getWidth()) < 0.5);
        String s = text(d);
        assertTrue(s.contains("Tisch 1"), s);
        assertTrue(s.contains("Jungen U19 · Gruppe"), s);
        assertTrue(s.contains(t.entrantName(m, m.getA()).replace("Ł", "L")), s);
        assertTrue(s.contains("5. Satz"));
    }

    @Test
    void phaseJeGruppeEineSeiteUndEndergebnis() throws Exception {
        Tournament t = tournament(12, 4);
        PlayingClass c = t.findClass("Jungen U19").orElseThrow();
        PDDocument d = PdfReports.phase(t, c);
        assertEquals(3, d.getNumberOfPages(), "3 Gruppen = 3 Seiten");
        String s = text(d);
        assertTrue(s.contains("Gruppe A") && s.contains("Gruppe C"));
        assertTrue(s.contains("Spielplan") && s.contains("Tabelle"));
        for (int g = 0; g < 200; g++) {
            Match m = t.matchesOf(c).stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE || x.getStatus() == MatchStatus.QUEUED)
                    .findFirst().orElse(null);
            if (m == null) break;
            t.recordResult(m, 3, 1);
        }
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(2));
        String k = text(PdfReports.phase(t, ko));
        assertTrue(k.contains("Viertelfinale") || k.contains("Halbfinale"), k);
        assertThrows(IllegalStateException.class, () -> t.finishClass(c), "Phase 2 ist noch nicht fertig");
        for (int g = 0; g < 50; g++) {
            Match m = t.matchesOf(ko).stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE || x.getStatus() == MatchStatus.QUEUED)
                    .findFirst().orElse(null);
            if (m == null) break;
            t.recordResult(m, 3, 0);
        }
        t.finishClass(ko);
        String r = text(PdfReports.finalResult(t, c));
        assertTrue(r.contains("Endergebnis Jungen U19") && r.contains("1.") && r.contains("10.–12."), r);
    }

    @Test
    void vieleSpieleGehenAufFortsetzungsseiten() throws Exception {
        Tournament t = tournament(14, 14);   // eine Gruppe, 91 Spiele
        PlayingClass c = t.findClass("Jungen U19").orElseThrow();
        PDDocument d = PdfReports.phase(t, c);
        assertTrue(d.getNumberOfPages() >= 2);
        assertTrue(text(d).contains("Fortsetzung"));
    }
}
