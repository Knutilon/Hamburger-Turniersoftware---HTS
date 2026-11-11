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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SwissSystemTest {

    private static PlayingClass swiss(Tournament t, int n, int rounds) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("S" + i, "N" + i);
            p.setInternalNr("S-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        PlayingClass c = t.addClass(new PlayingClass("Schweizer"));
        ps.forEach(p -> t.assign(p, c));
        c.setSwissRounds(rounds);
        t.drawSwiss(c, true, new Random(5));
        return c;
    }

    /** Höherer TTR gewinnt. */
    private static void playRunning(Tournament t, PlayingClass c) {
        for (int guard = 0; guard < 1000; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED).findFirst().orElse(null);
            if (open == null) return;
            boolean aWins = t.entrant(open, open.getA()).ttr() > t.entrant(open, open.getB()).ttr();
            t.recordResult(open, aWins ? 3 : 0, aWins ? 0 : 3);
        }
    }

    @Test
    void ersteRundeObereGegenUntereHaelfte() {
        Tournament t = new Tournament();
        PlayingClass c = swiss(t, 8, 3);
        List<Match> r1 = t.matchesOf(c);
        assertEquals(4, r1.size());
        assertEquals(1, t.swissRound(c));
        // nach Setzung: 1–5, 2–6, 3–7, 4–8
        assertEquals("N1, S1", t.entrantName(r1.get(0), r1.get(0).getA()));
        assertEquals("N5, S5", t.entrantName(r1.get(0), r1.get(0).getB()));
    }

    @Test
    void rundenWerdenNachTabelleGepaartOhneWiederholungUndMitFreilos() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = swiss(t, 9, 5);
        assertEquals(5, t.swissRounds(c));
        t.startClass(c);
        playRunning(t, c);
        assertEquals(5, t.swissRound(c));
        assertTrue(t.isGroupPhaseComplete(c));
        assertFalse(c.isFinished(), "nach der letzten Runde entscheidet die Turnierleitung (Folgephase oder beenden)");
    }

    @Test
    void keineWiederholungUndHoechstensEinFreilos() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = swiss(t, 9, 5);
        c.setNextPhase(TournamentMode.KNOCKOUT);
        c.setQualifiers(4);
        t.startClass(c);
        playRunning(t, c);
        Set<String> seen = new HashSet<>();
        for (Match m : t.matchesOf(c)) assertTrue(seen.add(SwissSystem.key(m.getA(), m.getB())), "Wiederholung " + m.getNumber());
        assertEquals(5, c.getSwissByes().size());
        assertEquals(5, new HashSet<>(c.getSwissByes()).size(), "jede/r höchstens ein Freilos");
        assertEquals(4 * 5, t.matchesOf(c).size());
        // Freilos zählt als Sieg, Spiele ohne Freilos
        var rows = t.standings(c, 0);
        assertEquals(1, rows.get(0).place());
        assertEquals("N1, S1", rows.get(0).entrant().name(), "der Stärkste gewinnt alles");
        assertEquals(5, rows.get(0).score());
        for (var r : rows) assertEquals(5, r.played() + r.byes(), r.entrant().name());
        // Buchholz = Summe der Punkte der Gegner
        var byId = new java.util.HashMap<String, Integer>();
        rows.forEach(r -> byId.put(r.entrant().id(), r.score()));
        for (var r : rows) {
            int bh = 0;
            for (Match m : t.matchesOf(c)) {
                if (m.getA().equals(r.entrant().id())) bh += byId.get(m.getB());
                if (m.getB().equals(r.entrant().id())) bh += byId.get(m.getA());
            }
            assertEquals(bh, r.buchholz(), r.entrant().name());
        }
        assertFalse(c.isFinished());
        // Schweizer System: keine weitere Phase, nur das Endergebnis
        assertFalse(t.canHaveNextPhase(c));
        assertThrows(IllegalStateException.class, () -> t.createNextPhase(c, new Random(1)));
        t.finishClass(c);
        assertTrue(c.isFinished());
        assertEquals(rows.get(0).entrant().id(), t.finalResult(c).rows().get(0).entrant().id());
    }

    @Test
    void speichernUndLaden() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = swiss(t, 7, 3);
        t.startClass(c);
        // nur die 1. Runde spielen -> Runde 2 entsteht
        for (Match m : new ArrayList<>(t.matchesOf(c))) {
            if (m.getStatus() != MatchStatus.DONE) t.recordResult(m, 3, 1);
        }
        assertEquals(2, t.swissRound(c));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass c2 = back.findClass("Schweizer").orElseThrow();
        assertEquals(TournamentMode.SWISS, c2.getMode());
        assertEquals(3, c2.getSwissRounds());
        assertEquals(c.getSwissByes(), c2.getSwissByes());
        assertEquals(2, back.swissRound(c2));
        assertEquals(t.standings(c, 0).stream().map(r -> r.entrant().id()).toList(),
                back.standings(c2, 0).stream().map(r -> r.entrant().id()).toList());
    }
}
