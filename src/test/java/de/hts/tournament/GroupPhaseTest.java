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

class GroupPhaseTest {

    /** „S1“ hat den höchsten TTR, „S2“ den zweithöchsten usw. */
    private static List<Entrant> field(int n) {
        List<Entrant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(new Entrant("S" + i, "S" + i, 2000 - i * 10, null));
        return list;
    }

    private static List<String> ids(List<Entrant> g) {
        return g.stream().map(Entrant::id).toList();
    }

    @Test
    void anzahlGruppenAusGruppengroesse() {
        assertEquals(4, GroupDraw.groupCountForSize(16, 4));
        assertEquals(6, GroupDraw.groupCountForSize(16, 3), "aufgerundet – keine Gruppe größer als 3");
        assertEquals(1, GroupDraw.groupCountForSize(3, 4));
        assertEquals(4, GroupDraw.maxGroupSize(16, 4));
        assertEquals(6, GroupDraw.maxGroupSize(16, 3), "16 auf 3 Gruppen: 6, 5, 5");
        assertNull(GroupDraw.validate(5, 2));
        assertNotNull(GroupDraw.validate(5, 3), "eine Gruppe hätte nur 1 Mitglied");
        assertNotNull(GroupDraw.validate(1, 1));
    }

    @Test
    void setzungImSchlangensystem() {
        // 12 Teilnehmende, 4 Gruppen: Köpfe 1-4, dann 5-8 rückwärts, dann 9-12 vorwärts
        List<List<Entrant>> g = GroupDraw.draw(field(12), 4, true, new Random(1));
        assertEquals(List.of("S1", "S8", "S9"), ids(g.get(0)));
        assertEquals(List.of("S2", "S7", "S10"), ids(g.get(1)));
        assertEquals(List.of("S3", "S6", "S11"), ids(g.get(2)));
        assertEquals(List.of("S4", "S5", "S12"), ids(g.get(3)));
    }

    @Test
    void ungleicheGruppenSindErlaubt() {
        List<List<Entrant>> g = GroupDraw.draw(field(10), 4, true, new Random(1));
        assertEquals(List.of(3, 3, 2, 2), g.stream().map(List::size).toList());
        assertEquals(List.of("S1", "S8", "S9"), ids(g.get(0)));
        assertEquals(List.of("S2", "S7", "S10"), ids(g.get(1)));
        assertEquals(List.of("S4", "S5"), ids(g.get(3)));
    }

    @Test
    void zufaelligeGruppenEnthaltenAlleGenauEinmal() {
        Set<String> heads = new HashSet<>();
        for (int r = 0; r < 30; r++) {
            List<List<Entrant>> g = GroupDraw.draw(field(13), 4, false, new Random(r));
            assertEquals(13, g.stream().mapToInt(List::size).sum());
            assertEquals(13, g.stream().flatMap(List::stream).map(Entrant::id).distinct().count());
            heads.add(g.get(0).get(0).id());
        }
        assertTrue(heads.size() > 3, "ohne Setzung sind die Gruppenköpfe zufällig");
    }

    @Test
    void jederGegenJeden() {
        for (int n = 2; n <= 7; n++) {
            List<GroupDraw.Pairing> plan = GroupDraw.roundRobin(field(n));
            assertEquals(n * (n - 1) / 2, plan.size(), "Spiele bei " + n);
            Set<String> seen = new HashSet<>();
            for (GroupDraw.Pairing p : plan) {
                String key = p.a().id().compareTo(p.b().id()) < 0 ? p.a().id() + p.b().id() : p.b().id() + p.a().id();
                assertTrue(seen.add(key), "doppelte Begegnung " + key);
            }
            // pro Runde spielt jede/r höchstens einmal
            int rounds = plan.stream().mapToInt(GroupDraw.Pairing::round).max().orElse(0);
            for (int r = 1; r <= rounds; r++) {
                Set<String> inRound = new HashSet<>();
                for (GroupDraw.Pairing p : plan) {
                    if (p.round() == r) {
                        assertTrue(inRound.add(p.a().id()));
                        assertTrue(inRound.add(p.b().id()));
                    }
                }
            }
        }
    }

    @Test
    void gruppennamen() {
        assertEquals("A", GroupDraw.groupName(0));
        assertEquals("Z", GroupDraw.groupName(25));
        assertEquals("AA", GroupDraw.groupName(26));
    }

    // --- im Turnier ------------------------------------------------------------

    private static Tournament tournamentWith(int n, PlayingClass c) {
        Tournament t = new Tournament();
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("NU-" + i);
            p.setSex(Sex.FEMALE);
            p.setTtr(1500 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        t.addClass(c);
        ps.forEach(p -> t.assign(p, c));
        return t;
    }

    @Test
    void gruppenphaseAuslosenSpiellisteUndSpeichern() throws Exception {
        PlayingClass c = new PlayingClass("Mädchen U15", Discipline.SINGLES, ClassType.GIRLS, null, null);
        Tournament t = tournamentWith(16, c);
        c.setMode(TournamentMode.GROUPS);
        c.setGroupsBySize(true);
        c.setGroupSize(4);
        assertEquals(4, t.plannedGroupCount(c));
        t.drawGroups(c, true, new Random(3));
        assertTrue(c.isDrawn());
        assertTrue(c.getDraw().isEmpty());
        assertEquals(4, c.getGroups().size());
        assertEquals("N1, V1", t.groupsOf(c).get(0).get(0).name(), "höchster TTR = Kopf Gruppe A");
        assertEquals("N2, V2", t.groupsOf(c).get(1).get(0).name());

        List<Match> matches = t.matchesOf(c);
        assertEquals(4 * 6, matches.size(), "4 Gruppen à 4 -> je 6 Spiele");
        assertEquals("Gruppe A", matches.get(0).getStage());
        assertEquals(1, matches.get(0).getNumber());
        assertEquals(24, matches.get(23).getNumber());

        // über Anzahl Gruppen
        c.setGroupsBySize(false);
        c.setGroupCount(5);
        t.drawGroups(c, false, new Random(4));
        assertEquals(5, c.getGroups().size());
        assertEquals(List.of(4, 3, 3, 3, 3), t.groupsOf(c).stream().map(List::size).toList().stream().sorted(java.util.Comparator.reverseOrder()).toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament l = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass lc = l.findClass("Mädchen U15").orElseThrow();
        assertEquals(TournamentMode.GROUPS, lc.getMode());
        assertFalse(lc.isGroupsBySize());
        assertEquals(5, lc.getGroupCount());
        assertEquals(c.getGroups(), lc.getGroups());
        assertEquals(t.matchesOf(c).size(), l.matchesOf(lc).size());
        assertFalse(l.isDrawOutdated(lc));
    }

    @Test
    void verwerfenUndWechselZwischenKoUndGruppen() {
        PlayingClass c = new PlayingClass("HE");
        Tournament t = tournamentWith(6, c);
        t.drawKnockout(c, true, new Random(1));
        assertFalse(c.getDraw().isEmpty());
        // KO-Spielliste: nur Spiele mit feststehenden Gegnern (6 im 8er-Feld -> 2 Spiele in Runde 1)
        assertEquals(5, t.matchesOf(c).size(), "alle KO-Spiele: 6 Teilnehmende -> 5 Spiele");
        assertEquals(2, t.matchesOf(c).stream().filter(Match::isReady).count(), "davon 2 mit feststehenden Gegnern");
        assertEquals("Viertelfinale", t.matchesOf(c).get(0).getStage());

        t.drawGroups(c, false, new Random(2));           // Gruppenauslosung ersetzt KO
        assertTrue(c.getDraw().isEmpty());
        assertFalse(c.getGroups().isEmpty());

        t.clearDraw(c);                                   // verwerfen
        assertFalse(c.isDrawn());
        assertTrue(t.matchesOf(c).isEmpty());
    }

    @Test
    void neuAuslosenIstImmerMoeglich() {
        PlayingClass c = new PlayingClass("HE");
        Tournament t = tournamentWith(8, c);
        t.drawKnockout(c, true, new Random(1));
        List<String> first = c.getDraw();
        t.drawKnockout(c, true, new Random(2));
        assertEquals(first.get(0), c.getDraw().get(0), "nach Setzung bleibt Nr. 1 oben");
        t.drawKnockout(c, false, new Random(3));
        assertFalse(c.isDrawSeeded());
        assertEquals(8, c.getDraw().size());
    }
}
