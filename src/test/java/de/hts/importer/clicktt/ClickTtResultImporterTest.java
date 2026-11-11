/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.importer.clicktt;

import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ClickTtResultImporterTest {

    private static byte[] resource(String name) throws Exception {
        try (InputStream in = ClickTtResultImporterTest.class.getResourceAsStream(name)) {
            return in.readAllBytes();
        }
    }

    @Test
    void fertigesTurnierMitGruppenphase() throws Exception {
        byte[] xml = resource("/clicktt-ergebnisse-hem.xml");
        ClickTtResultImporter.Result r = new ClickTtResultImporter().importBytes(xml);
        Tournament t = r.tournament();
        assertEquals("HEM-Vorquali / Hauptquali 2026 - Erwachsene", t.getName());
        assertEquals(java.time.LocalDate.of(2026, 11, 1), t.getDate());
        assertEquals(16, t.getTableCount());
        assertEquals(1, r.classes());
        assertEquals(294, r.matches());
        PlayingClass c = t.getClasses().get(0);
        assertEquals(TournamentMode.GROUPS, c.getMode());
        assertEquals(14, c.getGroups().size());
        assertTrue(c.getGroups().stream().allMatch(g -> g.size() == 7));
        assertTrue(t.matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE), "alle Spiele abgeschlossen");
        assertTrue(c.isFinished(), "Turnier gilt als abgeschlossen");
        assertTrue(t.isStarted());
        assertEquals(109, t.getPlayers().size());
        assertEquals(11, t.getPlayers().stream().filter(p -> p.getClassNames().isEmpty()).count(), "nicht angetreten: ohne Klasse");
        assertFalse(r.notes().isEmpty());

        // Platzierungen wie in der Datei (Beginn des geteilten Platzbereichs)
        String s = new String(xml, java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Integer> placeByNr = new HashMap<>();
        Matcher pm = Pattern.compile("<player [^>]*?placement=\"(\\d+)\"[^>]*>\\s*<person [^>]*?internal-nr=\"([^\"]+)\"").matcher(s);
        while (pm.find()) placeByNr.put(pm.group(2), Integer.parseInt(pm.group(1)));
        assertEquals(98, placeByNr.size());
        int checked = 0;
        for (Tournament.Placing p : t.finalResult(c).rows()) {
            String nr = t.getPlayers().stream().filter(x -> x.getId().equals(p.entrant().id())).findFirst().orElseThrow().getInternalNr();
            assertEquals((Integer) placeByNr.get(nr), (Integer) p.from(), p.entrant().name());
            checked++;
        }
        assertEquals(98, checked);
    }

    @Test
    void koTurnierMitSpielUmPlatz3RundUm() throws Exception {
        ClickTtXmlImporter.ImportResult base;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            base = new ClickTtXmlImporter().importStream(in);
        }
        Tournament t = new Tournament();
        t.setName("KO-Test");
        t.setTableCount(10);
        for (var ci : base.competitions()) {
            t.addClass(new PlayingClass(ci.name(), Discipline.SINGLES, ClassType.guess(ci.name(), ci.sex()), null, null));
        }
        t.merge(base.players());
        PlayingClass m15 = t.findClass("Mädchen U15 Einzel").orElseThrow();
        t.drawKnockout(m15, true, new Random(1));
        t.setThirdPlace(m15, true);
        t.startClass(m15);
        t.simulateAll(new Random(4));
        t.finishClass(m15);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            ClickTtResultExporter.export(t, in, out);
        }
        ClickTtResultImporter.Result r = new ClickTtResultImporter().importBytes(out.toByteArray());
        PlayingClass c = r.tournament().findClass("Mädchen U15 Einzel").orElseThrow();
        assertEquals(TournamentMode.KNOCKOUT, c.getMode());
        assertTrue(c.isThirdPlace());
        assertEquals(t.matchesOf(m15).size(), r.tournament().matchesOf(c).size());
        assertTrue(r.tournament().matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE), r.notes().toString());
        assertEquals(t.finalResult(m15).rows().stream().map(p -> p.entrant().name() + p.from()).toList(),
                r.tournament().finalResult(c).rows().stream().map(p -> p.entrant().name() + p.from()).toList());
    }
}
