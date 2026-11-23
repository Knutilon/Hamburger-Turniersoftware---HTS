/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.report.CertificateReport;
import de.hts.report.PdfReports;
import de.hts.ui.OverviewView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.4.0/1.4.1 – Zettel-Design je Turnier (#108), Archiv (#112) und Urkunden (#113).
 */
class Version140Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static PlayingClass finishedKo(Tournament t, String name, int n) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("Vorname" + i, "Nachname" + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - 10 * i);
            p.setClubName("Verein " + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        for (int k = 0; k < 100; k++) {
            Match o = t.matchesOf(c).stream().filter(m -> m.isReady() && m.getStatus() != MatchStatus.DONE).findFirst().orElse(null);
            if (o == null) break;
            Entrant a = t.entrant(o, o.getA()), b = t.entrant(o, o.getB());
            boolean aw = a.ttr() >= b.ttr();
            t.recordResult(o, aw ? List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5})
                    : List.of(new int[]{5, 11}, new int[]{5, 11}, new int[]{5, 11}), 0);
        }
        t.finishClass(c);                       // „Klasse beenden“
        return c;
    }

    // --- #112 ---------------------------------------------------------------------------------------------

    @Test
    void archivNachMehrAls14Tagen() {
        LocalDate today = LocalDate.of(2026, 11, 22);
        assertFalse(OverviewView.isArchived(today, today));
        assertFalse(OverviewView.isArchived(today.minusDays(14), today), "genau 14 Tage: noch in „Turniere“");
        assertTrue(OverviewView.isArchived(today.minusDays(15), today));
        assertFalse(OverviewView.isArchived(today.plusDays(3), today), "künftige Turniere nie im Archiv");
        assertFalse(OverviewView.isArchived(null, today), "ohne Datum bleibt das Turnier in der Liste");
    }

    // --- #108 ---------------------------------------------------------------------------------------------

    @Test
    void zettelDesignWirdGespeichertUndAlteDateienSindSpielerzettel() throws Exception {
        Tournament t = new Tournament();
        assertEquals(SlipDesign.PLAYER, t.getSlipDesign());
        assertEquals(SlipDesign.PLAYER, roundTrip(t).getSlipDesign());
        t.setSlipDesign(SlipDesign.REFEREE);
        assertEquals(SlipDesign.REFEREE, roundTrip(t).getSlipDesign());
        assertEquals(SlipDesign.PLAYER, SlipDesign.fromName("UNBEKANNT"));
        assertEquals(SlipDesign.PLAYER, SlipDesign.fromName(null));
    }

    @Test
    void beideZettelEinzelUndDoppel() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = t.addClass(new PlayingClass("Jungen 19"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Player p = new Player("Max" + i, "Muster" + i);
            p.setInternalNr("J" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1500 - i);
            p.setClubName("TTC " + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        Match m = t.matchesOf(c).stream().filter(Match::isReady).findFirst().orElseThrow();
        for (SlipDesign d : SlipDesign.values()) {
            t.setSlipDesign(d);
            try (PDDocument doc = PdfReports.slip(t, m)) {
                assertEquals(1, doc.getNumberOfPages());
                String text = new PDFTextStripper().getText(doc);
                assertTrue(text.contains("Muster"), text);
                assertTrue(text.contains("Gewinner/in"), text);
                assertFalse(text.contains("Wahl:"), "„Wahl“ entfällt in beiden Designs");
                if (d == SlipDesign.PLAYER) {
                    assertTrue(text.contains("gewonnene Sätze"), text);
                    assertFalse(text.contains("Unterschrift Schiedsrichter"), text);
                } else {
                    assertTrue(text.contains("Schiedsrichter/in"), text);
                    assertTrue(text.contains("1. Aufschlag"), text);
                    assertTrue(text.contains("Auszeit"), text);
                }
            }
        }
    }

    // --- #113 ---------------------------------------------------------------------------------------------

    @Test
    void urkundenAusDemEndergebnis() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = finishedKo(t, "Jungen 19", 4);
        assertTrue(t.isCompletelyFinished());
        List<Tournament.CertEntry> es = t.certificateEntries();
        assertEquals(4, es.size());
        assertEquals(1, es.get(0).place());
        assertEquals("Vorname1 Nachname1", es.get(0).name(), "Urkunde: Vorname Nachname");
        assertEquals("Verein 1", es.get(0).club());
        assertEquals("Jungen 19", es.get(0).className());
        assertEquals(2, es.get(1).place());
        assertTrue(es.stream().allMatch(e -> !e.changed()));
        assertNotNull(c);
    }

    @Test
    void druckanpassungNurFuerDieUrkunde() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        finishedKo(t, "Jungen 19", 4);
        Tournament.CertEntry first = t.certificateEntries().get(0);
        t.setCertOverride(first, "Max Mustermann-Langname", "TTC Hamburg-Nord e.V.");
        Tournament.CertEntry now = t.certificateEntries().get(0);
        assertTrue(now.changed());
        assertEquals("Max Mustermann-Langname", now.name());
        assertEquals("Vorname1 Nachname1", now.originalName());
        // Teilnehmerdaten und Endergebnis bleiben unverändert
        assertTrue(t.getPlayers().stream().noneMatch(p -> "Mustermann-Langname".equals(p.getLastname())));
        assertTrue(t.getPlayers().stream().noneMatch(p -> "TTC Hamburg-Nord e.V.".equals(p.getClubName())));
        PlayingClass c = t.rootClasses().get(0);
        assertTrue(t.finalResult(c).rows().stream().noneMatch(r -> r.entrant().name().contains("Mustermann")));
        // gespeichert in der Turnierdatei
        Tournament back = roundTrip(t);
        Tournament.CertEntry loaded = back.certificateEntries().get(0);
        assertEquals("Max Mustermann-Langname", loaded.name());
        assertEquals("TTC Hamburg-Nord e.V.", loaded.club());
        // Originalwerte wiederherstellen entfernt die Anpassung
        back.setCertOverride(loaded, loaded.originalName(), loaded.originalClub());
        assertFalse(back.certificateEntries().get(0).changed());
        assertTrue(back.getCertOverrides().isEmpty());
    }

    @Test
    void urkundenJeBeendeterKlasseUndFrageGemerkt() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(8);
        finishedKo(t, "Jungen 19", 4);
        PlayingClass open = t.addClass(new PlayingClass("Mädchen 15"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Player p = new Player("M" + i, "Offen" + i);
            p.setInternalNr("M15-" + i);
            p.setSex(Sex.FEMALE);
            p.setTtr(1200 + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, open));
        t.drawKnockout(open, true, new Random(2));
        t.startClass(open);
        assertFalse(t.isCompletelyFinished(), "Turnier läuft noch");
        assertEquals(List.of("Jungen 19"), t.finishedRootClasses().stream().map(PlayingClass::getName).toList());
        assertTrue(t.certificateEntries().stream().allMatch(e -> e.className().equals("Jungen 19")),
                "nur die beendete Klasse hat Urkunden");
        assertFalse(t.isCertAsked("Jungen 19"));
        t.markCertAsked("Jungen 19");
        Tournament back = roundTrip(t);
        assertTrue(back.isCertAsked("Jungen 19"));
        assertFalse(back.isCertAsked("Mädchen 15"));
    }

    @Test
    void urkundenFuerFestePlaetzeNachPhase1() {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = PhaseFixtures.groupsClass(t, "Herren A", 16, 4);
        t.startClass(c);
        assertNull(t.certAskKey(c), "vor Phase 2 keine Frage");
        PhaseFixtures.playAll(t, c);
        assertTrue(t.certificateEntries().isEmpty(), "Phase 1 gespielt, aber noch nicht abgeschlossen");
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        assertEquals("Herren A · Phase 1", t.certAskKey(c));
        List<Tournament.CertEntry> es = t.certificateEntries();
        assertEquals(8, es.size(), "die 8 in der Gruppenphase Ausgeschiedenen");
        java.util.Set<String> inKo = new java.util.HashSet<>();
        for (Match m : t.matchesOf(ko)) { inKo.add(m.getA()); inKo.add(m.getB()); }
        assertTrue(es.stream().noneMatch(e -> inKo.contains(e.entrantId())), "wer in Phase 2 spielt, hat noch keinen festen Platz");
        assertTrue(es.stream().allMatch(e -> e.place() >= 9), "Plätze ab 9");
        assertEquals(List.of("Herren A"), t.certificateClasses().stream().map(PlayingClass::getName).toList());
    }

    @Test
    void urkundentexteLeerBleibenLeer() throws Exception {
        Tournament t = new Tournament();
        assertEquals("Ehrenurkunde", t.getCertTitle());
        assertEquals("Hamburg", t.getCertPlace());
        assertEquals("Jugendausschuss", t.getCertSigner());
        assertEquals("Hamburger Tisch-Tennis-Verband e.V.", t.getCertHost());
        assertTrue(t.isCertLeaveTop());
        t.setCertTexts("Urkunde", "", "Turnierleitung", "");
        t.setCertLeaveTop(false);
        t.setCertTabShown(true);
        Tournament back = roundTrip(t);
        assertEquals("Urkunde", back.getCertTitle());
        assertEquals("", back.getCertPlace(), "leeres Feld bleibt leer (nicht die Vorgabe)");
        assertEquals("Turnierleitung", back.getCertSigner());
        assertEquals("", back.getCertHost());
        assertFalse(back.isCertLeaveTop());
        assertTrue(back.isCertTabShown());
        Tournament fresh = roundTrip(new Tournament());
        assertEquals("Hamburg", fresh.getCertPlace(), "ältere Dateien: Vorgaben");
        assertFalse(fresh.isCertTabShown());
    }

    @Test
    void urkundenPdfMitEingebetteterSchrift() throws Exception {
        Tournament t = new Tournament();
        t.setName("Hamburger Ranglistenturnier");
        t.setDate(LocalDate.of(2026, 9, 12));
        t.setTableCount(4);
        finishedKo(t, "Jungen 19", 4);
        List<Tournament.CertEntry> es = t.certificateEntries();
        t.setCertTexts(null, "", null, null);
        try (PDDocument doc = CertificateReport.certificates(t, es)) {
            assertEquals(4, doc.getNumberOfPages());
            PDFTextStripper st = new PDFTextStripper();
            st.setStartPage(1);
            st.setEndPage(1);
            String text = st.getText(doc);
            assertTrue(text.contains("Ehrenurkunde"), text);
            assertTrue(text.contains("Vorname1 Nachname1"), text);
            assertTrue(text.contains("Hamburger Ranglistenturnier"), text);
            assertTrue(text.contains("in der Altersklasse Jungen 19"), text);
            assertTrue(text.contains("den 1. Platz"), text);
            assertTrue(text.contains("den 12. September 2026"), text);
            assertFalse(text.contains(", den"), "ohne Ort entfällt das Komma");
            boolean comic = false;
            for (org.apache.pdfbox.cos.COSName n : doc.getPage(0).getResources().getFontNames()) {
                PDFont f = doc.getPage(0).getResources().getFont(n);
                if (f.getName().contains("ComicNeue")) comic = f.isEmbedded();
            }
            assertTrue(comic, "Comic Neue ist eingebettet");
        }
        assertEquals("12. September 2026", CertificateReport.dateText(LocalDate.of(2026, 9, 12)));
        assertEquals("", CertificateReport.dateText(null));
    }
}
