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
import de.hts.persistence.TournamentStore;
import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.21.0: Meldungen „gemeldet“, Testturnier/Simulation, Turnier kopieren. */
class Version021Test {

    private static List<Player> players(Tournament t, String prefix, int n, int ttrBase) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(prefix + i, "N" + prefix + i);
            p.setInternalNr(prefix + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(ttrBase - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        return ps;
    }

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    @Test
    void importInNeueKlassenNurAlsMeldung() throws Exception {
        Player a = new Player("Anna", "Alt");
        a.addToClass("Damen");
        a.addToClass("Mixed alt");
        Player b = new Player("Bea", "Berg");
        b.addWaitingList("Damen");
        Tournament.registerOnly(List.of(a, b), Set.of("Damen"));
        assertFalse(a.isInClass("Damen"));
        assertTrue(a.getRegistrations().contains("Damen"));
        assertTrue(a.isInClass("Mixed alt"), "bestehende Klasse unverändert");
        assertTrue(b.getWaitingLists().contains("Damen"));
        assertFalse(b.getRegistrations().contains("Damen"), "Warteliste bleibt Warteliste");

        Tournament t = new Tournament();
        t.addClass(new PlayingClass("Damen"));
        t.merge(List.of(a, b));
        PlayingClass damen = t.findClass("Damen").orElseThrow();
        assertEquals(0, t.playersIn(damen).size(), "alle unter „Verfügbar“");
        assertTrue(a.getClassesText().contains("gemeldet: Damen"));

        Tournament back = roundTrip(t);
        Player a2 = back.getPlayers().stream().filter(p -> p.getLastname().equals("Alt")).findFirst().orElseThrow();
        assertTrue(a2.getRegistrations().contains("Damen"));
        // in die Klasse ziehen: Meldung erledigt
        back.assign(a2, back.findClass("Damen").orElseThrow());
        assertTrue(a2.getRegistrations().isEmpty());
    }

    @Test
    void zufallsergebnisseSindRegelgerecht() {
        Random r = new Random(42);
        for (int ws : new int[]{3, 4}) {
            boolean deuce = false;
            for (int i = 0; i < 2000; i++) {
                List<int[]> sets = Tournament.randomSets(ws, r);
                Scoring.Evaluation ev = Scoring.evaluate(sets, ws);
                assertTrue(ev.isComplete(), ev.message());
                assertTrue(sets.size() >= ws && sets.size() <= 2 * ws - 1);
                for (int[] s : sets) {
                    int hi = Math.max(s[0], s[1]), lo = Math.min(s[0], s[1]);
                    assertTrue(hi <= 20 && lo <= 18, hi + ":" + lo);
                    assertTrue(hi == 11 ? lo <= 9 : hi - lo == 2 && lo >= 10, hi + ":" + lo);
                    if (hi > 11) deuce = true;
                }
            }
            assertTrue(deuce, "auch Verlängerungen");
        }
    }

    @Test
    void alleSpieleSimulierenBisPhasenende() {
        Tournament t = new Tournament();
        t.setTest(true);
        t.setTableCount(4);
        PlayingClass ko = t.addClass(new PlayingClass("KO"));
        players(t, "K", 11, 2000).forEach(p -> t.assign(p, ko));
        t.drawKnockout(ko, true, new Random(1));
        t.setThirdPlace(ko, true);
        PlayingClass sw = t.addClass(new PlayingClass("Swiss"));
        players(t, "S", 9, 1900).forEach(p -> t.assign(p, sw));
        sw.setSwissRounds(4);
        t.drawSwiss(sw, true, new Random(2));
        PlayingClass gr = t.addClass(new PlayingClass("Gruppen"));
        players(t, "G", 12, 1800).forEach(p -> t.assign(p, gr));
        gr.setGroupsBySize(false);
        gr.setGroupCount(2);
        t.setWinningSets(gr, 4);
        t.drawGroups(gr, true, new Random(3));
        PlayingClass idle = t.addClass(new PlayingClass("Nicht gestartet"));
        players(t, "N", 4, 1500).forEach(p -> t.assign(p, idle));
        t.drawKnockout(idle, true, new Random(4));

        t.startClass(ko);
        t.startClass(sw);
        t.startClass(gr);
        int n = t.simulateAll(new Random(7));
        assertEquals(t.matchesOf(ko).size() + t.matchesOf(sw).size() + t.matchesOf(gr).size(), n);
        assertTrue(t.isPhaseComplete(ko), "inkl. Folgerunden und Spiel um Platz 3");
        assertTrue(t.isPhaseComplete(sw), "alle Runden des Schweizer Systems");
        assertTrue(t.isPhaseComplete(gr));
        assertFalse(ko.isFinished(), "nichts abschließen");
        assertTrue(t.matchesOf(idle).stream().noneMatch(m -> m.getStatus() == MatchStatus.DONE), "nicht gestartete Klasse bleibt");
        assertTrue(t.matchesOf(gr).stream().allMatch(m -> Math.max(m.getSetsA(), m.getSetsB()) == 4), "4 Gewinnsätze");

        // Phase 2 Qualifikation angehalten: wird nicht simuliert, fortgesetzt dann schon
        PlayingClass q = t.createQualificationPhase(gr, 1, 2, 2, new Random(1));   // 4 in der KO
        t.pauseClass(q);
        assertEquals(0, t.simulateAll(new Random(8)));
        t.resumeClass(q);
        assertTrue(t.simulateAll(new Random(9)) > 0);
        assertTrue(t.isPhaseComplete(q), "alle Qualifikationsrunden");
    }

    @Test
    void turnierKopierenInDreiVarianten() throws Exception {
        Path dir = Files.createTempDirectory("hts-copy");
        try {
            TournamentStore store = new TournamentStore(dir.resolve("HTS Turniere"));
            Tournament src = store.create("Quelle", LocalDate.of(2026, 5, 1), 6, null, true);
            PlayingClass c = src.addClass(new PlayingClass("Herren"));
            players(src, "H", 8, 1700).forEach(p -> src.assign(p, c));
            c.setGroupsBySize(false);
            c.setGroupCount(2);
            src.setWinningSets(c, 4);
            src.drawGroups(c, true, new Random(1));
            src.startClass(c);
            src.simulateAll(new Random(1));
            src.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
            store.save(src);
            var summary = store.list().get(0);
            assertTrue(summary.test());

            Tournament all = store.copy(summary, TournamentStore.CopyMode.EVERYTHING, "Alles", LocalDate.of(2026, 6, 1), 6, false, null);
            assertEquals(2, all.getClasses().size(), "inkl. Phase 2");
            assertEquals(src.getMatches().size(), all.getMatches().size());
            assertFalse(all.isTest());

            Tournament fresh = store.copy(summary, TournamentStore.CopyMode.WITHOUT_DRAW, "Ohne", LocalDate.of(2026, 6, 2), 6, true, null);
            assertEquals(1, fresh.getClasses().size());
            PlayingClass fc = fresh.getClasses().get(0);
            assertFalse(fc.isDrawn());
            assertFalse(fc.isStarted());
            assertNull(fc.getFollowUpClass());
            assertEquals(8, fresh.playersIn(fc).size(), "Zuordnungen bleiben");
            assertEquals(4, fc.getWinningSets(), "Einstellungen bleiben");
            assertTrue(fresh.getMatches().isEmpty());
            assertFalse(fresh.isStarted());
            assertTrue(fresh.isTest());

            Tournament onlyPlayers = store.copy(summary, TournamentStore.CopyMode.PLAYERS_ONLY, "Nur", LocalDate.of(2026, 6, 3), 6, false, null);
            assertTrue(onlyPlayers.getClasses().isEmpty());
            assertEquals(8, onlyPlayers.getPlayers().size());
            assertTrue(onlyPlayers.getPlayers().stream().allMatch(p -> p.getClassNames().isEmpty()));

            assertEquals(4, store.list().size());
            assertNotEquals(fresh.getFolder(), src.getFolder());
            // Quelle unverändert
            Tournament again = store.load(summary.id());
            assertEquals(2, again.getClasses().size());
        } finally {
            try (var walk = Files.walk(dir)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }
}
