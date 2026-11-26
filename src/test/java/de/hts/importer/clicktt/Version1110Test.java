/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.importer.clicktt;

import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.report.PdfReports;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.11.0 – Gewinnsätze 1–4 (#182), Platzierungstexte (#180), click-TT: nur exportierte Konkurrenzen (#183),
 * verständliche Meldungen und UTF-16 (#184), Mannschaftskonkurrenzen (#185), gleichnamige Konkurrenzen (#186).
 */
class Version1110Test {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tournament name="Testturnier">
              <competition age-group="Herren" type="Einzel" ttr-from="1201" ttr-to="1400" start-date="2026-12-06 10:00" competition-id="H1">
                <players><player type="single" id="P1"><person firstname="Anna" lastname="A" internal-nr="NU1" sex="1"/></player>
                <player type="single" id="P2"><person firstname="Bert" lastname="B" internal-nr="NU2" sex="1"/></player></players>
              </competition>
              <competition age-group="Herren" type="Einzel" ttr-from="1001" ttr-to="1400" start-date="2026-12-06 14:00" competition-id="H2">
                <players><player type="single" id="P3"><person firstname="Carl" lastname="C" internal-nr="NU3" sex="1"/></player></players>
              </competition>
              <competition age-group="Herren" type="Mannschaft" competition-id="M1">
                <players><player type="team" id="T1"><person firstname="Dora" lastname="D" internal-nr="NU4" sex="2"/></player></players>
              </competition>
            </tournament>
            """;

    private static Tournament tournament(int n, int winningSets) {
        Tournament t = new Tournament();
        t.setName("Gewinnsätze");
        t.setDate(java.time.LocalDate.of(2026, 12, 6));
        PlayingClass c = t.addClass(new PlayingClass("Herren C"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("w-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1800 - i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.setWinningSets(c, winningSets);
        t.drawKnockout(c, true, new Random(1));
        t.setThirdPlace(c, true);
        t.startClass(c);
        return t;
    }

    @Test
    void gewinnsaetzeEinsBisVier() throws Exception {
        for (int ws = 1; ws <= 4; ws++) {
            Tournament t = tournament(4, ws);
            PlayingClass c = t.findClass("Herren C").orElseThrow();
            assertEquals(ws, c.getWinningSets());
            Match m = t.matchesOf(c).stream().filter(x -> x.isReady() && x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
            List<int[]> sets = new ArrayList<>();
            for (int k = 0; k < ws; k++) sets.add(new int[]{11, 5});
            t.recordResult(m, sets, 0);
            assertEquals(ws, m.getSetsA(), ws + " Gewinnsätze");
            try (var d = PdfReports.slip(t, t.matchesOf(c).stream().filter(Match::isReady).filter(x -> x.getStatus() != MatchStatus.DONE).findFirst().orElseThrow())) {
                assertEquals(1, d.getNumberOfPages(), "Spielzettel mit " + (2 * ws - 1) + " Satzfeldern");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TournamentFile.write(out, TournamentFile.Content.of(t));
            Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
            assertEquals(ws, back.findClass("Herren C").orElseThrow().getWinningSets(), "gespeichert");
        }
        assertEquals("1 Gewinnsatz", Tournament.winningSetsLabel(1));
        assertEquals("2 Gewinnsätze", Tournament.winningSetsLabel(2));
        Tournament t = tournament(4, 2);
        PlayingClass c = t.findClass("Herren C").orElseThrow();
        Match m = t.matchesOf(c).stream().filter(x -> x.isReady() && x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> t.recordResult(m, List.of(new int[]{11, 5}), 0), "1 Satz reicht bei 2 Gewinnsätzen nicht");
    }

    @Test
    void platzierungNurMitRunde() {
        Tournament t = tournament(8, 3);
        PlayingClass c = t.findClass("Herren C").orElseThrow();
        for (int k = 0; k < 20; k++) {
            Match m = t.matchesOf(c).stream().filter(x -> x.isReady() && (x.getStatus() == MatchStatus.ACTIVE || x.getStatus() == MatchStatus.QUEUED)).findFirst().orElse(null);
            if (m == null) break;
            t.recordResult(m, List.of(new int[]{11, 3}, new int[]{11, 3}, new int[]{11, 3}), 0);
        }
        List<String> infos = t.finalResult(c).rows().stream().map(Tournament.Placing::info).toList();
        assertEquals(List.of("Sieger/in", "Finale", "Sieger/in Spiel um Platz 3", "Spiel um Platz 3", "Viertelfinale", "Viertelfinale", "Viertelfinale", "Viertelfinale"), infos);
        assertTrue(infos.stream().noneMatch(s -> s.contains("ausgeschieden") || s.contains("verloren") || s.contains("(Sieg)")));
    }

    @Test
    void importGleichnamigeUndMannschaftskonkurrenzen() throws Exception {
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));
        List<String> names = r.competitions().stream().map(ClickTtXmlImporter.CompetitionInfo::name).toList();
        assertEquals(List.of("Herren Einzel (TTR bis 1400) (TTR 1201–1400)", "Herren Einzel (TTR bis 1400) (TTR 1001–1400)"), names, "eindeutig, Mannschaft nicht dabei");
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Mannschaftskonkurrenz")));
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("mehrere Konkurrenzen")));
        assertTrue(r.players().stream().noneMatch(p -> "D".equals(p.getLastname())), "Personen der Mannschaftskonkurrenz nicht übernommen");
        // UTF-16 mit Kennung, Deklaration noch UTF-8 (per E-Mail verändert)
        byte[] u16 = ("﻿" + XML.strip()).getBytes(StandardCharsets.UTF_16LE);
        byte[] withBom = new byte[u16.length];
        System.arraycopy(u16, 0, withBom, 0, u16.length);
        assertEquals((byte) 0xFF, withBom[0]);
        var r16 = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(withBom));
        assertEquals(3, r16.players().size(), "UTF-16 wird gelesen");
        var bad = assertThrows(ClickTtImportException.class, () -> new ClickTtXmlImporter().importStream(new ByteArrayInputStream("<html>Login".getBytes(StandardCharsets.UTF_8))));
        assertTrue(bad.getMessage().contains("erneut herunterladen"), bad.getMessage());
        var prolog = assertThrows(ClickTtImportException.class, () -> new ClickTtXmlImporter().importStream(new ByteArrayInputStream("Fehler 404".getBytes(StandardCharsets.UTF_8))));
        assertTrue(prolog.getMessage().startsWith("Die Datei ist keine click-TT-Teilnehmerdatei"), prolog.getMessage());
    }

    @Test
    void exportNurMitExportiertenKonkurrenzen() throws Exception {
        Tournament t = new Tournament();
        t.setName("Export");
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));
        for (var ci : r.competitions()) t.addClass(new PlayingClass(ci.name()));
        t.merge(r.players());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var rep = ClickTtResultExporter.export(t, new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)), out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertEquals(0, rep.competitions());
        assertFalse(xml.contains("<competition"), "nicht beendete Konkurrenzen stehen nicht in der Datei");
        assertTrue(rep.warnings().stream().anyMatch(w -> w.contains("nicht in der Datei")));
        assertEquals(3, rep.skipped().size());
    }
}
