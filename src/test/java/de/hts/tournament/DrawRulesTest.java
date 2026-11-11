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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DrawRulesTest {

    /** n Teilnehmende mit absteigendem TTR (Rang 1 = Index 0) und Vereinen. */
    private static List<Entrant> field(int n, String... clubs) {
        List<Entrant> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Entrant("e" + (i + 1), "Rang " + (i + 1), 2000 - i * 10, clubs.length == 0 ? null : clubs[i % clubs.length]));
        }
        return list;
    }

    private static int rank(Entrant e) {
        return Integer.parseInt(e.id().substring(1));
    }

    @Test
    void koSetzlisteNachTtrMitFreilosen() {
        List<Entrant> slots = KnockoutDraw.draw(field(16), true, new Random(1));
        int[] expected = {1, 16, 9, 8, 5, 12, 13, 4, 3, 14, 11, 6, 7, 10, 15, 2};
        for (int i = 0; i < 16; i++) assertEquals(expected[i], rank(slots.get(i)), "Platz " + i);
        List<Entrant> twelve = KnockoutDraw.draw(field(12), true, new Random(1));
        assertNull(twelve.get(1), "Nr. 1 hat ein Freilos (Platz für Nr. 16)");
        assertNull(twelve.get(14), "Nr. 2 hat ein Freilos (Platz für Nr. 15)");
        assertEquals(2, rank(twelve.get(15)));
    }

    @Test
    void koTrenntVereinskameradenInRundeEins() {
        // Rang 1 und 16 aus demselben Verein -> Rang 16 wird mit dem Nächstplatzierten (15) getauscht
        String[] clubs = new String[16];
        for (int i = 0; i < 16; i++) clubs[i] = "Verein " + (char) ('A' + i);
        clubs[15] = "Verein A";
        List<Entrant> list = new ArrayList<>();
        for (int i = 0; i < 16; i++) list.add(new Entrant("e" + (i + 1), "R" + (i + 1), 2000 - i * 10, clubs[i]));
        List<Entrant> plain = KnockoutDraw.draw(list, true, false, new Random(1));
        assertEquals(16, rank(plain.get(1)));
        List<Entrant> slots = KnockoutDraw.draw(list, true, true, new Random(1));
        assertEquals(1, rank(slots.get(0)));
        assertEquals(15, rank(slots.get(1)), "Nächstplatzierter ohne Vereinsgleichheit");
        assertTrue(KnockoutDraw.clubClashPositions(slots).isEmpty());
    }

    @Test
    void schlangeMitVereinsgleichheit() {
        // 8 Teilnehmende, 2 Gruppen; Rang 1 und 4 im selben Verein -> Schlange würde 1 und 4 in Gruppe A setzen
        List<Entrant> list = field(8, "X", "Y", "Z", "X", "V", "W", "U", "T");
        List<List<Entrant>> plain = GroupDraw.draw(list, 2, GroupDraw.Seeding.SNAKE, false, new Random(1));
        assertFalse(GroupDraw.clubClashes(plain.get(0)).isEmpty());
        List<List<Entrant>> g = GroupDraw.draw(list, 2, GroupDraw.Seeding.SNAKE, true, new Random(1));
        for (List<Entrant> grp : g) assertTrue(GroupDraw.clubClashes(grp).isEmpty(), grp.toString());
        assertEquals(4, g.get(0).size());
        assertEquals(1, rank(g.get(0).get(0)));
        assertEquals(2, rank(g.get(1).get(0)));
    }

    @Test
    void staerkegruppen16Und15() {
        for (int seed = 0; seed < 20; seed++) {
            List<List<Entrant>> g = GroupDraw.draw(field(16), 4, GroupDraw.Seeding.POTS, false, new Random(seed));
            for (List<Entrant> grp : g) {
                assertEquals(4, grp.size());
                Set<Integer> pots = new HashSet<>();
                for (Entrant e : grp) pots.add((rank(e) - 1) / 4);
                assertEquals(Set.of(0, 1, 2, 3), pots, "aus jeder Stärkegruppe eine/r");
            }
            List<List<Entrant>> h = GroupDraw.draw(field(15), 4, GroupDraw.Seeding.POTS, false, new Random(seed));
            int three = 0;
            for (List<Entrant> grp : h) {
                if (grp.size() == 3) three++;
                long p1 = grp.stream().filter(e -> rank(e) <= 4).count();
                long p2 = grp.stream().filter(e -> rank(e) >= 5 && rank(e) <= 8).count();
                long p3 = grp.stream().filter(e -> rank(e) >= 9).count();
                assertEquals(1, p1);
                assertEquals(1, p2);
                assertTrue(p3 >= 1, "aus der letzten Stärkegruppe (9–15) mindestens eine/r");
            }
            assertEquals(1, three, "eine Gruppe hat nur 3");
        }
    }

    @Test
    void staerkegruppenMitVereinsgleichheit() {
        // 8 Teilnehmende, 2 Gruppen; Stärkegruppe 1 = Rang 1+2 (Verein A und B), Rang 3+4 sind A und B
        List<Entrant> list = field(8, "A", "B", "A", "C", "D", "E", "F", "G");
        for (int seed = 0; seed < 20; seed++) {
            List<List<Entrant>> g = GroupDraw.draw(list, 2, GroupDraw.Seeding.POTS, true, new Random(seed));
            for (List<Entrant> grp : g) assertTrue(GroupDraw.clubClashes(grp).isEmpty(), "Lauf " + seed + ": " + grp);
        }
    }

    @Test
    void handaenderungenVorDemStart() {
        Tournament t = new Tournament();
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("H" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1500 - i);
            p.setClubName(i <= 2 ? "TSV" : "Club " + i);
            ps.add(p);
        }
        t.merge(ps);
        PlayingClass c = t.addClass(new PlayingClass("G"));
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(3);
        t.drawGroups(c, true, new Random(1));
        String a0 = c.getGroups().get(0).get(0), b0 = c.getGroups().get(1).get(0);
        t.swapGroupMembers(c, a0, b0);
        assertEquals(b0, c.getGroups().get(0).get(0));
        t.moveGroupMember(c, a0, 2);
        assertEquals(4, c.getGroups().get(2).size());
        assertEquals(2, c.getGroups().get(1).size(), "a0 war nach dem Tausch in Gruppe B");
        assertEquals(6 + 3 + 1, t.matchesOf(c).size(), "Spiele neu: 4er-Gruppe 6, 3er-Gruppe 3, 2er-Gruppe 1");
        assertThrows(IllegalStateException.class, () -> t.moveGroupMember(c, c.getGroups().get(1).get(0), 0), "mind. 2");
        // Vereinsgleichheit markieren
        String n1 = ps.get(0).getId(), n2 = ps.get(1).getId();
        int g1 = -1;
        for (int g = 0; g < 3; g++) if (c.getGroups().get(g).contains(n1)) g1 = g;
        if (!c.getGroups().get(g1).contains(n2)) t.moveGroupMember(c, n2, g1);
        assertTrue(t.clubClashes(c).containsAll(Set.of(n1, n2)));

        PlayingClass k = t.addClass(new PlayingClass("K"));
        ps.forEach(p -> t.assign(p, k));
        t.drawKnockout(k, true, new Random(1));      // 9 -> 16er-Feld
        List<String> before = new ArrayList<>(k.getDraw());
        t.swapKnockoutPairings(k, 0, 7);
        assertEquals(before.get(0), k.getDraw().get(14));
        assertEquals(before.get(15), k.getDraw().get(1));
        t.swapKnockoutSlots(k, 14, 1);                 // Nr. 1 und Nr. 2 tauschen die Plätze
        assertEquals(before.get(0), k.getDraw().get(1));
        assertEquals(before.get(15), k.getDraw().get(14));
        // Nr. 1 (Platz 1) gegen das Freilos auf Platz 15 tauschen -> Spiel 1 wären zwei Freilose -> verboten
        assertThrows(IllegalStateException.class, () -> t.swapKnockoutSlots(k, 1, 15));
        t.startClass(k);
        assertThrows(IllegalStateException.class, () -> t.swapKnockoutPairings(k, 0, 1));
    }

    /** Rangliste: alle Gruppenersten (A, B, C …), dann alle Zweiten; Gruppe je ID. */
    private static List<Entrant> rankedByGroups(int groups, int perGroup, java.util.Map<String, Integer> groupOf) {
        List<Entrant> list = new ArrayList<>();
        for (int p = 1; p <= perGroup; p++) {
            for (int g = 0; g < groups; g++) {
                String id = (char) ('A' + g) + "" + p;
                list.add(new Entrant(id, id, 1500, null));
                groupOf.put(id, g);
            }
        }
        return list;
    }

    @Test
    void koNachGruppenphaseFreiloseUndTrennung() {
        java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
        List<Entrant> ranked = rankedByGroups(3, 2, groupOf);         // A1 B1 C1 A2 B2 C2 -> 8er-Feld, 2 Freilose
        List<Entrant> slots = KnockoutDraw.drawFromRanking(ranked, groupOf, 3, new Random(1));
        assertEquals("A1", slots.get(0).id());
        assertNull(slots.get(1), "Freilos für den besten Gruppenersten");
        assertNull(slots.get(slots.indexOf(ranked.get(1)) ^ 1), "Freilos für den zweitbesten Gruppenersten");
        // C1 (höchstplatziert ohne Freilos) gegen den Niedrigstplatzierten außer C2 -> B2
        int c1 = slots.indexOf(ranked.get(2));
        assertEquals("B2", slots.get(c1 ^ 1).id());
        // Gruppengleiche in verschiedenen Hälften
        for (int g = 0; g < 3; g++) {
            int a = slots.indexOf(ranked.get(g)), b = slots.indexOf(ranked.get(3 + g));
            assertNotEquals(a / 4, b / 4, "Gruppe " + g);
        }
    }

    @Test
    void koNachVierGruppenErsterGegenLetztenAndererGruppe() {
        java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
        List<Entrant> ranked = rankedByGroups(4, 2, groupOf);         // 8 -> keine Freilose
        List<Entrant> slots = KnockoutDraw.drawFromRanking(ranked, groupOf, 4, new Random(1));
        assertEquals("A1", slots.get(0).id());
        assertEquals("D2", slots.get(1).id(), "Höchstplatzierter gegen Niedrigstplatzierten");
        for (int i = 0; i < 8; i += 2) assertNotEquals(groupOf.get(slots.get(i).id()), groupOf.get(slots.get(i + 1).id()));
        for (int g = 0; g < 4; g++) {
            int a = slots.indexOf(ranked.get(g)), b = slots.indexOf(ranked.get(4 + g));
            assertNotEquals(a / 4, b / 4, "Gruppe " + g + " erst im Finale");
        }
        // 16er: 8 Gruppen à 2
        groupOf.clear();
        List<Entrant> r16 = rankedByGroups(8, 2, groupOf);
        List<Entrant> s16 = KnockoutDraw.drawFromRanking(r16, groupOf, 8, new Random(1));
        for (int g = 0; g < 8; g++) {
            int a = s16.indexOf(r16.get(g)), b = s16.indexOf(r16.get(8 + g));
            assertNotEquals(a / 8, b / 8, "Gruppe " + g + " erst im Finale");
        }
    }
}
