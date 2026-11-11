/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.33.0: Qualifikation (2 Siege, Zusatzrunde qualifiziert, Nachrücker-Ränge), Zurück zur vorherigen Phase. */
class Version033Test {

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

    private static PlayingClass groupsClass(Tournament t, String name, int n, int groupCount) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        players(t, name.substring(0, 1), n, 2000).forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(groupCount);
        t.drawGroups(c, true, new Random(3));
        return c;
    }

    /** Höherer TTR gewinnt immer 3:0. */
    private static void playAll(Tournament t, PlayingClass c) {
        // 0.38: Qualifikationsphase startet erst per „Phase starten“; nach Runde 1 wird entschieden (hier: weitere Runde)
        if (c.isAwaitingStart()) t.resumeClass(c);
        if (t.qualificationDecisionPending(c)) t.decideQualification(c, false);
        if (c.isAwaitingStart()) t.resumeClass(c);
        for (int guard = 0; guard < 500; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .findFirst().orElse(null);
            if (open == null) {
                if (t.qualificationDecisionPending(c)) {
                    t.decideQualification(c, false);
                    if (c.isAwaitingStart()) t.resumeClass(c);       // 0.41: Runde 2 per „Qualifikation Runde 2 starten“
                    continue;
                }
                return;
            }
            Entrant a = t.entrant(open, open.getA()), b = t.entrant(open, open.getB());
            boolean aWins = a.ttr() > b.ttr();
            t.recordResult(open, aWins ? List.of(new int[]{11, 5}, new int[]{11, 6}, new int[]{11, 7})
                    : List.of(new int[]{5, 11}, new int[]{6, 11}, new int[]{7, 11}), 0);
        }
    }

    private static long count(List<Tournament.Placing> rows, String info) {
        return rows.stream().filter(r -> r.info().equals(info)).count();
    }

    @Test
    void qualifikationsansichtKenntAlleTeilnehmenden() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Herren", 16, 4);
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 2, 2, new Random(1));
        assertTrue(t.entrantsOf(q).isEmpty(), "der Phase ist niemand zugeordnet …");
        for (Match m : t.matchesOf(q)) {
            assertNotNull(t.entrantIndex(q).get(m.getA()), "… die Ansicht findet die Spieler trotzdem");
            assertNotNull(t.entrantIndex(q).get(m.getB()));
        }
        assertEquals(4, t.qualifiedDirect(q).size());
        var labels = t.qualificationPlaceLabels(q);
        for (Entrant e : t.qualifiedDirect(q)) assertTrue(labels.get(e.id()).endsWith("1"), "Gruppenerste");
        for (String id : q.getQualField().keySet()) assertTrue(labels.get(id).matches("[A-D][23]"));
    }

    @Test
    void freilosZaehltAlsSiegUndGleicheRaengeSpielenGegeneinander() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Damen", 15, 3);            // 3 Gruppen à 5
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 2, 1, 2, new Random(1));   // nur die 3 Gruppendritten in der KO
        assertEquals(Tournament.QUAL_WINS, q.getQualRounds());
        List<Match> r1 = t.matchesOf(q);
        assertEquals(1, r1.size(), "Freilos für den Stärksten + 1 Spiel");
        String bye = q.getSwissByes().get(0);
        assertEquals(q.getQualField().keySet().iterator().next(), bye, "Freilos: Bestplatzierter nach der Gruppenphase");
        Match m = r1.get(0);
        assertEquals(3, q.getQualPlace().get(m.getA()));
        assertEquals(3, q.getQualPlace().get(m.getB()), "nur noch Dritte übrig – gleiche Ränge gegeneinander");
        assertNotEquals(q.getQualField().get(m.getA()), q.getQualField().get(m.getB()), "andere Gruppe");
        playAll(t, q);
        assertEquals(2, t.matchesOf(q).size(), "Runde 2: Freilos-Gewinner gegen den Sieger, kein Freilos");
        assertTrue(q.getSwissByes().size() < 2 || q.getSwissByes().get(1) == null);
        assertTrue(t.isPhaseComplete(q));
        var rows = t.finalResult(c).rows();
        assertEquals(6, count(rows, Tournament.QUALIFIED_BY_GROUP));
        assertEquals(1, count(rows, Tournament.QUALIFIED_BY_KO));
        assertEquals(1, count(rows, "Nachrücker 1. Ranges"), "Verlierer Runde 2");
        assertEquals(1, count(rows, "Nachrücker 2. Ranges"), "Verlierer Runde 1");
    }

    @Test
    void verliererRundeZweiInFeldreihenfolge() {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Jugend", 15, 3);
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 4, 2, 0, new Random(1));   // 12 in der KO
        playAll(t, q);
        assertEquals(6 + 3, t.matchesOf(q).size());
        assertTrue(t.isPhaseComplete(q));
        var before = t.finalResult(c).rows();
        assertEquals(3, count(before, Tournament.QUALIFIED_BY_KO));
        List<Entrant> rank1 = t.qualificationTiers(q).get(0);
        assertEquals(3, rank1.size(), "Verlierer Runde 2");
        List<String> fieldOrder = new ArrayList<>(q.getQualField().keySet());
        fieldOrder.retainAll(rank1.stream().map(Entrant::id).toList());
        assertEquals(fieldOrder, rank1.stream().map(Entrant::id).toList(), "Gruppenplatz, Verhältnisse, TTR");

    }

    @Test
    void nachrueckerAusDerGruppenphaseBekommenDenNaechstenRang() {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Senioren", 24, 4);       // 4 Gruppen à 6
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 2, 2, 1, new Random(1));   // 8 in der KO, 1 Nachrücker je Gruppe
        playAll(t, q);
        var rows = t.finalResult(c).rows();
        assertEquals(2, count(rows, "Nachrücker 1. Ranges"));
        assertEquals(4, count(rows, "Nachrücker 2. Ranges"));
        assertEquals(4, count(rows, "Nachrücker 3. Ranges"), "Gruppenvierte ohne Spiel");
    }

    @Test
    void zurueckZuPhaseEinsSolangeKeinErgebnis() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Vorquali", 16, 4);
        t.startClass(c);
        playAll(t, c);
        assertEquals("Es liegt bereits ein Ergebnis vor.", t.stepBackProblem(c));
        PlayingClass q = t.createQualificationPhase(c, 2, 0, 2, new Random(1));     // ohne KO: Phase 2 ganz ohne Spiele
        assertEquals("Es gibt schon eine weitere Phase.", t.stepBackProblem(c));
        assertNull(t.stepBackProblem(q));
        t.stepBack(q);
        assertFalse(t.findClass(q.getName()).isPresent(), "Phase 2 gelöscht");
        assertNull(c.getFollowUpClass());
        assertTrue(t.isPhaseComplete(c), "Phase 1 wieder offen – kann neu abgeschlossen werden");
        PlayingClass q2 = t.createQualificationPhase(c, 1, 2, 2, new Random(2));
        assertNotNull(q2);
        playAll(t, q2);
        assertEquals("Es liegt bereits ein Ergebnis vor.", t.stepBackProblem(q2));
        assertThrows(IllegalStateException.class, () -> t.stepBack(q2));
    }

    @Test
    void auslosungVerwerfenNachDemStartOhneErgebnis() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = groupsClass(t, "Herren", 12, 3);
        t.startClass(c);
        assertTrue(t.matchesAtTables(c) > 0, "Spiele laufen an Tischen");
        assertNull(t.stepBackProblem(c));
        t.stepBack(c);
        assertFalse(c.isDrawn());
        assertFalse(c.isStarted());
        assertTrue(t.matchesOf(c).isEmpty());
        assertEquals(List.of(1, 2, 3, 4), t.freeTables(), "Tische sofort frei");
        assertEquals("Die Klasse ist noch nicht ausgelost.", t.stepBackProblem(c));
        // neu auslosen und starten geht wieder
        t.drawGroups(c, true, new Random(5));
        t.startClass(c);
        assertTrue(c.isStarted());
    }
}
