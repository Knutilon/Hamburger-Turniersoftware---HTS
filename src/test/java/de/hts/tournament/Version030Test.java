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

/** Neuerungen 0.30.0: Doppel-TTR, Gemeldete ohne Partner/in, festes Los in Tabellen, Präsentationsauswahl. */
class Version030Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static Player player(String first, String last, Integer ttr) {
        Player p = new Player(first, last);
        p.setInternalNr(first + "-" + last);
        p.setSex(Sex.MALE);
        p.setTtr(ttr);
        return p;
    }

    private static List<Player> players(Tournament t, String prefix, int n, int ttrBase) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) ps.add(player(prefix + i, "N" + prefix + i, ttrBase - i * 10));
        t.merge(ps);
        return ps;
    }

    @Test
    void doppelTtrPartnerwertOderNull() {
        Tournament t = new Tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        Player a = player("A", "Anders", 1600), b = player("B", "Berg", 1400), c = player("C", "Claus", null),
                d = player("D", "Dorn", null), e = player("E", "Ernst", null);
        t.merge(List.of(a, b, c, d, e));
        assertEquals(1500, t.createPair(hd, a, b).getAverageTtr(), "beide mit TTR: Mittelwert");
        DoublesPair half = t.createPair(hd, c, null);
        assertEquals(0, half.getAverageTtr(), "beide ohne TTR: 0");
        DoublesPair de = t.createPair(hd, d, e);
        assertEquals(0, de.getAverageTtr());
        Player f = player("F", "Fink", 1700);
        t.merge(List.of(f));
        t.addToPair(half, f);
        assertEquals(1700, half.getAverageTtr(), "eine/r ohne TTR: Wert der Partnerin / des Partners");
    }

    @Test
    void gemeldeteOhnePartnerStehenUnterVerfuegbarUndWerdenZugelost() {
        Tournament t = new Tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        List<Player> ps = players(t, "D", 4, 1800);
        t.createPair(hd, ps.get(0), ps.get(1));                // Paar aus der Importdatei
        t.registerWithoutPartner(ps.get(2), hd);               // ohne Partner/in gemeldet
        t.registerWithoutPartner(ps.get(3), hd);
        assertEquals(1, t.pairsIn(hd).size(), "keine halben Paare");
        assertTrue(ps.get(2).getRegistrations().contains("HD"));
        assertTrue(t.pairOf(ps.get(2), hd).isEmpty());
        Tournament.PartnerDraw draw = t.drawPartners(hd, new Random(1));
        assertEquals(1, draw.pairs().size());
        assertNull(draw.leftOver());
        assertEquals(2, t.pairsIn(hd).stream().filter(DoublesPair::isComplete).count());
    }

    /** Drei gleich starke Ergebnisse im Kreis: völlige Gleichheit – es entscheidet das feste Los, nicht der TTR. */
    private static Tournament circle(long seed) {
        Tournament t = new Tournament();
        t.setTableCount(4);
        t.setTest(true);
        PlayingClass c = t.addClass(new PlayingClass("H"));
        players(t, "H", 3, 2000).forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(1);
        t.drawGroups(c, true, new Random(seed));
        t.startClass(c);
        List<String> order = t.groupsOf(c).get(0).stream().map(Entrant::id).toList();
        for (int guard = 0; guard < 20; guard++) {
            boolean any = false;
            for (Match m : t.matchesOf(c)) {
                if (m.getStatus() != MatchStatus.QUEUED && m.getStatus() != MatchStatus.ACTIVE) continue;
                int ia = order.indexOf(m.getA()), ib = order.indexOf(m.getB());
                boolean aWins = (ia + 1) % 3 == ib;            // 0 schlägt 1, 1 schlägt 2, 2 schlägt 0
                t.recordResult(m, aWins ? List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5})
                        : List.of(new int[]{5, 11}, new int[]{5, 11}, new int[]{5, 11}), 0);
                any = true;
            }
            if (!any) break;
        }
        assertTrue(t.matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE));
        return t;
    }

    @Test
    void gleichstandEntscheidetFestesLos() throws Exception {
        boolean notByTtr = false;
        for (long seed = 1; seed <= 12; seed++) {
            Tournament t = circle(seed);
            PlayingClass c = t.findClass("H").orElseThrow();
            List<String> table = t.standings(c, 0).stream().map(r -> r.entrant().id()).toList();
            assertEquals(table, t.standings(c, 0).stream().map(r -> r.entrant().id()).toList(), "springt nicht");
            Tournament back = roundTrip(t);
            assertEquals(table, back.standings(back.findClass("H").orElseThrow(), 0).stream().map(r -> r.entrant().id()).toList(),
                    "das Los wird gespeichert");
            List<String> byTtr = t.groupsOf(c).get(0).stream()
                    .sorted(java.util.Comparator.comparing((Entrant e) -> e.ttr()).reversed()).map(Entrant::id).toList();
            if (!table.equals(byTtr)) notByTtr = true;
        }
        assertTrue(notByTtr, "bei völliger Gleichheit zählt nicht der TTR");
    }

    @Test
    void praesentationNichtGestarteteKlassenStandardmaessigAus() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        t.setTest(true);
        PlayingClass a = t.addClass(new PlayingClass("A"));
        players(t, "A", 4, 2000).forEach(p -> t.assign(p, a));
        t.drawKnockout(a, true, new Random(1));
        PlayingClass b = t.addClass(new PlayingClass("B nicht ausgelost"));
        players(t, "B", 4, 1800).forEach(p -> t.assign(p, b));

        List<PresentationPlan.Station> all = PresentationPlan.all(t);
        assertEquals(List.of("tables", "next16", "phase:A"), all.stream().map(PresentationPlan.Station::key).toList(),
                "nicht ausgeloste Klassen fehlen, ausgeloste stehen am Ende");
        assertTrue(all.get(2).upcoming());
        assertFalse(PresentationPlan.isOn(t, all.get(2)), "zunächst ausgeschaltet");
        assertEquals(PresentationPlan.ClassState.DRAWN, PresentationPlan.stateOf(t, a));
        assertEquals(PresentationPlan.ClassState.NOT_DRAWN, PresentationPlan.stateOf(t, b));

        t.setPresentationOn("phase:A", true);
        Tournament back = roundTrip(t);
        assertTrue(PresentationPlan.isOn(back, PresentationPlan.all(back).get(2)), "Einschalten wird gespeichert");

        Tournament t2 = new Tournament();
        t2.setTableCount(4);
        t2.setTest(true);
        PlayingClass c = t2.addClass(new PlayingClass("C"));
        players(t2, "C", 4, 2000).forEach(p -> t2.assign(p, c));
        t2.drawKnockout(c, true, new Random(1));
        t2.startClass(c);
        PresentationPlan.Station st = PresentationPlan.all(t2).get(0);
        assertEquals("phase:C", st.key());
        assertTrue(PresentationPlan.isOn(t2, st), "gestartet: standardmäßig eingeschaltet");
        assertEquals(PresentationPlan.ClassState.RUNNING, PresentationPlan.stateOf(t2, c));
    }
}
