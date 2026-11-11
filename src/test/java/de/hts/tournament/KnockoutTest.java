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

class KnockoutTest {

    private static Entrant e(String name, Integer ttr) {
        return new Entrant(name, name, ttr, null);
    }

    /** Spieler „S1“ hat den höchsten TTR, „S2“ den zweithöchsten usw. */
    private static List<Entrant> field(int n) {
        List<Entrant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(e("S" + i, 2000 - i * 10));
        return list;
    }

    private static int pos(List<Entrant> slots, String name) {
        for (int i = 0; i < slots.size(); i++) if (slots.get(i) != null && slots.get(i).id().equals(name)) return i;
        return -1;
    }

    @Test
    void feldgroesseUndSetzliste() {
        assertEquals(2, KnockoutDraw.bracketSize(2));
        assertEquals(4, KnockoutDraw.bracketSize(3));
        assertEquals(8, KnockoutDraw.bracketSize(5));
        assertEquals(16, KnockoutDraw.bracketSize(16));
        assertEquals(32, KnockoutDraw.bracketSize(17));
        assertArrayEquals(new int[]{1, 8, 5, 4, 3, 6, 7, 2}, KnockoutDraw.seedOrder(8));
        assertArrayEquals(new int[]{1, 16, 9, 8, 5, 12, 13, 4, 3, 14, 11, 6, 7, 10, 15, 2}, KnockoutDraw.seedOrder(16));
        int[] o32 = KnockoutDraw.seedOrder(32);
        assertEquals(1, o32[0]);
        assertEquals(32, o32[1]);
        assertEquals(2, o32[31], "Nr. 2 ganz unten");
        int[] o64 = KnockoutDraw.seedOrder(64);
        assertEquals(64, java.util.Arrays.stream(o64).distinct().count());
        for (int i = 0; i < 64; i += 2) assertEquals(65, o64[i] + o64[i + 1], "Paarung " + i);
        assertArrayEquals(new int[]{1, 4, 3, 2}, KnockoutDraw.seedOrder(4));
    }

    @Test
    void nachSetzungTreffenSichDieBestenErstSpaet() {
        for (int seedRun = 0; seedRun < 20; seedRun++) {
            List<Entrant> slots = KnockoutDraw.draw(field(8), true, new Random(seedRun));
            int p1 = pos(slots, "S1"), p2 = pos(slots, "S2"), p3 = pos(slots, "S3"), p4 = pos(slots, "S4");
            // 1 und 2 in verschiedenen Hälften -> frühestens Finale
            assertNotEquals(p1 / 4, p2 / 4);
            // 1 und 4 in derselben Hälfte, aber verschiedenen Vierteln -> frühestens Halbfinale
            assertEquals(p1 / 4, p4 / 4);
            assertNotEquals(p1 / 2, p4 / 2);
            // 2 und 3 im anderen Halbfinale
            assertEquals(p2 / 4, p3 / 4);
            assertNotEquals(p2 / 2, p3 / 2);
        }
    }

    @Test
    void freiloseGehenAnDieBestenUndNieZweiFreiloseGegeneinander() {
        List<Entrant> slots = KnockoutDraw.draw(field(5), true, new Random(1));
        assertEquals(8, slots.size());
        assertEquals(3, slots.stream().filter(x -> x == null).count());
        for (int i = 0; i < slots.size(); i += 2) {
            assertFalse(slots.get(i) == null && slots.get(i + 1) == null, "zwei Freilose in Spiel " + i);
        }
        // S1, S2, S3 haben Freilose
        for (String best : List.of("S1", "S2", "S3")) {
            int p = pos(slots, best);
            assertNull(slots.get(p % 2 == 0 ? p + 1 : p - 1), best + " sollte ein Freilos haben");
        }
        // auch zufällig nie zwei Freilose zusammen
        for (int r = 0; r < 50; r++) {
            List<Entrant> rnd = KnockoutDraw.draw(field(9), false, new Random(r));
            for (int i = 0; i < rnd.size(); i += 2) {
                assertFalse(rnd.get(i) == null && rnd.get(i + 1) == null);
            }
        }
    }

    @Test
    void zufaelligeAuslosungVariiertUndEnthaeltAlle() {
        Set<String> firstSlots = new HashSet<>();
        for (int r = 0; r < 30; r++) {
            List<Entrant> slots = KnockoutDraw.draw(field(8), false, new Random(r));
            assertEquals(8, slots.stream().filter(x -> x != null).map(Entrant::id).distinct().count());
            firstSlots.add(slots.get(0).id());
        }
        assertTrue(firstSlots.size() > 3, "ohne Setzung ist die Platzierung zufällig");
    }

    @Test
    void weniger_als_zwei_geht_nicht() {
        assertThrows(IllegalArgumentException.class, () -> KnockoutDraw.draw(field(1), false, new Random()));
    }

    @Test
    void begegnungenMitRundennamen() {
        List<Entrant> slots = KnockoutDraw.draw(field(6), true, new Random(3));
        KnockoutBracket b = new KnockoutBracket(slots);
        assertEquals(List.of("Viertelfinale", "Halbfinale", "Finale"),
                b.getRounds().stream().map(KnockoutBracket.Round::name).toList());
        assertEquals(5, b.playedMatches(), "6 Teilnehmende -> 5 Spiele");
        // Wer ein Freilos hat, steht schon im Halbfinale
        var semi = b.getRounds().get(1).matches();
        long known = semi.stream().flatMap(m -> java.util.stream.Stream.of(m.a(), m.b())).filter(s -> s.entrant() != null).count();
        assertEquals(2, known);
        assertEquals("Finale", KnockoutDraw.roundName(2));
        assertEquals("Achtelfinale", KnockoutDraw.roundName(16));
        assertEquals(15, new KnockoutBracket(KnockoutDraw.draw(field(16), false, new Random())).playedMatches());
    }

    // --- Turnier: Einzel/Doppel, Ø-TTR, Höchstzahl, Speichern ------------------

    private static Player p(String name, int ttr) {
        Player p = new Player(name, name);
        p.setInternalNr("NU-" + name);
        p.setSex(Sex.MALE);
        p.setTtr(ttr);
        return p;
    }

    @Test
    void doppelWerdenNachDurchschnittsTtrGesetzt() {
        Tournament t = new Tournament();
        Player a = p("A", 1800), b = p("B", 1600), c = p("C", 1500), d = p("D", 1300),
                e = p("E", 2000), f = p("F", 1000), g = p("G", 1200), h = p("H", 1100);
        t.merge(List.of(a, b, c, d, e, f, g, h));
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        DoublesPair ab = t.createPair(hd, a, b);   // Ø 1700
        DoublesPair cd = t.createPair(hd, c, d);   // Ø 1400
        DoublesPair ef = t.createPair(hd, e, f);   // Ø 1500
        DoublesPair gh = t.createPair(hd, g, h);   // Ø 1150
        t.createPair(hd, p("X", 0), null);         // unvollständig -> nicht ausgelost
        assertEquals(1700, ab.getAverageTtr());
        assertEquals(1500, ef.getAverageTtr());
        assertEquals(4, t.entrantsOf(hd).size());
        assertTrue(t.canChooseMode(hd));

        List<Entrant> slots = t.drawKnockout(hd, true, new Random(5));
        assertEquals(TournamentMode.KNOCKOUT, hd.getMode());
        // Setzliste bei 4: [1, 4, 3, 2] -> 1 = AB, 4 = GH, 3 = CD, 2 = EF
        assertEquals("pair:" + ab.getId(), slots.get(0).id());
        assertEquals("pair:" + gh.getId(), slots.get(1).id());
        assertEquals("pair:" + cd.getId(), slots.get(2).id());
        assertEquals("pair:" + ef.getId(), slots.get(3).id());
        assertFalse(t.isDrawOutdated(hd));
    }

    @Test
    void modusErstAbZweiTeilnehmenden() {
        Tournament t = new Tournament();
        Player a = p("A", 1500), b = p("B", 1400);
        t.merge(List.of(a, b));
        PlayingClass he = t.addClass(new PlayingClass("HE"));
        t.assign(a, he);
        assertFalse(t.canChooseMode(he));
        t.assign(b, he);
        assertTrue(t.canChooseMode(he));
    }

    @Test
    void auslosungVeraltetWennSichDieKlasseAendert() {
        Tournament t = new Tournament();
        Player a = p("A", 1500), b = p("B", 1400), c = p("C", 1300);
        t.merge(List.of(a, b, c));
        PlayingClass he = t.addClass(new PlayingClass("HE"));
        t.assign(a, he);
        t.assign(b, he);
        t.drawKnockout(he, false, new Random());
        assertFalse(t.isDrawOutdated(he));
        t.assign(c, he);
        assertTrue(t.isDrawOutdated(he));
        t.clearDraw(he);
        assertFalse(he.isDrawn());
    }

    @Test
    void hoechstzahlMarkiertDieZuletztHinzugefuegten() {
        Tournament t = new Tournament();
        Player a = p("A", 1000), b = p("B", 2000), c = p("C", 1500), d = p("D", 1700);
        t.merge(List.of(a, b, c, d));
        PlayingClass he = t.addClass(new PlayingClass("HE"));
        he.setMaxParticipants(2);
        t.assign(c, he);
        t.assign(a, he);
        t.assign(d, he);
        t.assign(b, he);
        assertEquals(List.of(c, a, d, b), t.playersInOrder(he), "Reihenfolge der Anmeldung");
        assertEquals(Set.of(d.getId(), b.getId()), t.overLimit(he));
        he.setMaxParticipants(null);
        assertTrue(t.overLimit(he).isEmpty());
    }

    @Test
    void reihenfolgeHoechstzahlUndAuslosungWerdenGespeichert() throws Exception {
        Tournament t = new Tournament();
        Player a = p("A", 1500), b = p("B", 1400), c = p("C", 1300);
        t.merge(List.of(a, b, c));
        PlayingClass he = t.addClass(new PlayingClass("HE"));
        he.setMaxParticipants(2);
        t.assign(c, he);
        t.assign(a, he);
        t.assign(b, he);
        t.drawKnockout(he, true, new Random(7));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament l = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass lhe = l.findClass("HE").orElseThrow();
        assertEquals(2, lhe.getMaxParticipants());
        assertEquals(TournamentMode.KNOCKOUT, lhe.getMode());
        assertTrue(lhe.isDrawSeeded());
        assertEquals(he.getDraw(), lhe.getDraw());
        assertEquals(List.of("C", "A", "B"), l.playersInOrder(lhe).stream().map(Player::getLastname).toList());
        assertFalse(l.isDrawOutdated(lhe));
        List<Entrant> slots = l.drawSlots(lhe);
        assertEquals(4, slots.size());
        assertEquals("A, A", slots.get(0).name(), "höchster TTR auf Platz 1");
    }
}
