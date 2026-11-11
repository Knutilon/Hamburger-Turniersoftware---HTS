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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Spielbetrieb: Klassenstart, Warteschlange, Tische, Ergebnisse, KO-Fortschritt, Turnierstart. */
class MatchOperationTest {

    private static Tournament tournament(int tables) {
        Tournament t = new Tournament();
        t.setTableCount(tables);
        return t;
    }

    private static List<Player> addPlayers(Tournament t, String prefix, int n, PlayingClass c) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(prefix + i, prefix);
            p.setInternalNr("NU-" + prefix + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        if (t.findClass(c.getName()).isEmpty()) t.addClass(c);
        ps.forEach(p -> t.assign(p, c));
        return ps;
    }

    private static long count(Tournament t, MatchStatus s) {
        return t.getMatches().stream().filter(m -> m.getStatus() == s).count();
    }

    @Test
    void nachAuslosungGeplantNachStartEingereihtUndAnTischen() {
        Tournament t = tournament(2);
        PlayingClass c = new PlayingClass("Gruppe");
        addPlayers(t, "P", 4, c);
        c.setGroupsBySize(true);
        c.setGroupSize(4);
        t.drawGroups(c, true, new Random(1));
        assertEquals(6, t.matchesOf(c).size());
        assertEquals(6, count(t, MatchStatus.PLANNED));
        assertFalse(t.isRunning());

        t.startClass(c);
        assertTrue(c.isStarted());
        assertTrue(t.isStarted(), "Klassenstart hält auch die Turnier-Startzeit fest");
        assertEquals(2, count(t, MatchStatus.ACTIVE), "2 Tische belegt");
        assertEquals(4, count(t, MatchStatus.QUEUED));
        assertTrue(t.freeTables().isEmpty());
        // niemand spielt gleichzeitig an zwei Tischen
        List<Match> active = t.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).toList();
        assertTrue(java.util.Collections.disjoint(t.personsOf(active.get(0)), t.personsOf(active.get(1))));
        assertThrows(IllegalStateException.class, () -> t.drawGroups(c, false, new Random()), "nach dem Start keine neue Auslosung");
        assertThrows(IllegalStateException.class, () -> t.clearDraw(c));
    }

    @Test
    void ergebnisMachtTischFreiUndNaechstesSpielRueckt() {
        Tournament t = tournament(1);
        PlayingClass c = new PlayingClass("Gruppe");
        addPlayers(t, "P", 3, c);
        c.setGroupsBySize(true);
        c.setGroupSize(3);
        t.drawGroups(c, false, new Random(2));
        t.startClass(c);
        Match first = t.activeMatchAt(1).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> t.recordResult(first, 2, 2), "kein Unentschieden");
        t.recordResult(first, 3, 1);
        assertEquals(MatchStatus.DONE, first.getStatus());
        assertNull(first.getTable());
        assertEquals(first.getA(), first.winner());
        Match second = t.activeMatchAt(1).orElseThrow();
        assertNotSame(first, second);
        assertEquals(1, count(t, MatchStatus.QUEUED));
    }

    @Test
    void koSiegerRueckenWeiterBisInsFinale() {
        Tournament t = tournament(4);
        PlayingClass c = new PlayingClass("KO");
        addPlayers(t, "K", 4, c);
        t.drawKnockout(c, true, new Random(3));     // 1-4 und 2-3 im Halbfinale
        assertEquals(3, t.matchesOf(c).size());
        Match fin = t.matchesOf(c).get(2);
        assertEquals("Finale", fin.getStage());
        assertFalse(fin.isReady());
        t.startClass(c);
        assertEquals(2, count(t, MatchStatus.ACTIVE));
        assertEquals(MatchStatus.PLANNED, fin.getStatus(), "Finale wartet auf die Halbfinal-Sieger");

        Match sf1 = t.matchesOf(c).get(0), sf2 = t.matchesOf(c).get(1);
        t.recordResult(sf1, 3, 0);
        assertEquals(sf1.getA(), fin.getA());
        assertNull(fin.getB());
        assertEquals(MatchStatus.PLANNED, fin.getStatus());
        t.recordResult(sf2, 1, 3);
        assertEquals(sf2.getB(), fin.getB());
        assertEquals(MatchStatus.ACTIVE, fin.getStatus(), "Finale wird eingereiht und sofort an einen freien Tisch gesetzt");
        t.recordResult(fin, 3, 2);
        assertEquals(3, count(t, MatchStatus.DONE));
    }

    @Test
    void freiloseUndWeiterrueckenBeiFuenfTeilnehmenden() {
        Tournament t = tournament(8);
        PlayingClass c = new PlayingClass("KO5");
        addPlayers(t, "F", 5, c);
        t.drawKnockout(c, true, new Random(4));
        // 8er-Feld: 1 Spiel in Runde 1, 2 Halbfinale, 1 Finale
        assertEquals(4, t.matchesOf(c).size());
        t.startClass(c);
        // Halbfinale 1: Nr. 1 (Freilos) gegen Sieger 4/5 -> wartet; Halbfinale 2: Nr. 2 gegen Nr. 3 (beide Freilos) -> sofort
        assertEquals(2, count(t, MatchStatus.ACTIVE));
        Match r1 = t.matchesOf(c).stream().filter(m -> m.getKoRound() == 0).findFirst().orElseThrow();
        t.recordResult(r1, 3, 1);
        Match sf1 = t.matchesOf(c).stream().filter(m -> m.getKoRound() == 1 && m.getKoIndex() == 0).findFirst().orElseThrow();
        assertTrue(sf1.isReady());
        assertEquals(MatchStatus.ACTIVE, sf1.getStatus());
    }

    @Test
    void turnierstartStartetNurBereiteKlassen() {
        Tournament t = tournament(3);
        PlayingClass a = new PlayingClass("A"), b = new PlayingClass("B"), c = new PlayingClass("C");
        addPlayers(t, "A", 4, a);
        addPlayers(t, "B", 4, b);
        addPlayers(t, "C", 4, c);
        t.drawKnockout(a, false, new Random(1));
        t.drawKnockout(b, false, new Random(2));
        assertThrows(IllegalStateException.class, () -> t.setClassReady(c, true), "C ist nicht ausgelost");
        t.setClassReady(a, true);

        var preview = t.previewStart();
        assertEquals(List.of(a), preview.started());
        assertEquals(List.of(b, c), preview.notReady());
        assertTrue(preview.alreadyRunning().isEmpty());

        t.startTournament();
        assertTrue(t.isStarted());
        assertTrue(a.isStarted());
        assertFalse(b.isStarted());
        assertEquals(2, count(t, MatchStatus.ACTIVE));

        t.startClass(b);                            // B einzeln nachstarten
        assertEquals(List.of(a, b), t.previewStart().alreadyRunning());
        assertEquals(3, count(t, MatchStatus.ACTIVE), "nur 3 Tische");
        assertEquals(1, count(t, MatchStatus.QUEUED));
    }

    @Test
    void spielbetriebWirdGespeichert() throws Exception {
        Tournament t = tournament(2);
        PlayingClass c = new PlayingClass("KO");
        addPlayers(t, "S", 4, c);
        t.drawKnockout(c, true, new Random(5));
        t.startClass(c);
        Match sf1 = t.matchesOf(c).get(0);
        t.recordResult(sf1, 3, 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament l = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass lc = l.findClass("KO").orElseThrow();
        assertEquals(2, l.getTableCount());
        assertTrue(lc.isStarted());
        assertEquals(3, l.matchesOf(lc).size());
        Match lsf1 = l.matchesOf(lc).get(0);
        assertEquals(MatchStatus.DONE, lsf1.getStatus());
        assertEquals("3:1", lsf1.getResultText());
        assertEquals(1, l.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).count());
        assertEquals(sf1.getA(), l.matchesOf(lc).get(2).getA(), "Finalist bleibt eingetragen");
        // nach dem Laden geht es normal weiter
        Match lsf2 = l.matchesOf(lc).get(1);
        l.recordResult(lsf2, 3, 0);
        assertEquals(MatchStatus.ACTIVE, l.matchesOf(lc).get(2).getStatus());
    }
}
