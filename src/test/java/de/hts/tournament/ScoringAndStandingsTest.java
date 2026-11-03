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

class ScoringAndStandingsTest {

    // --- Satzregeln -----------------------------------------------------------

    @Test
    void gueltigeUndUngueltigeSaetze() {
        for (int[] ok : new int[][]{{11, 0}, {11, 9}, {9, 11}, {12, 10}, {10, 12}, {13, 11}, {18, 16}, {25, 23}}) {
            assertTrue(Scoring.isValidSet(ok[0], ok[1]), ok[0] + ":" + ok[1]);
        }
        for (int[] bad : new int[][]{{13, 14}, {11, 10}, {12, 9}, {10, 8}, {11, 11}, {14, 11}, {15, 12}, {0, 0}, {20, 16}}) {
            assertFalse(Scoring.isValidSet(bad[0], bad[1]), bad[0] + ":" + bad[1]);
        }
    }

    @Test
    void kurzeingabeWirdUmgewandelt() {
        assertArrayEquals(new int[]{11, 8}, Scoring.parseSet("+8"));
        assertArrayEquals(new int[]{7, 11}, Scoring.parseSet("-7"));
        assertArrayEquals(new int[]{11, 0}, Scoring.parseSet("+0"));
        assertArrayEquals(new int[]{12, 10}, Scoring.parseSet("+10"));
        assertArrayEquals(new int[]{12, 14}, Scoring.parseSet("-12"));
        assertArrayEquals(new int[]{18, 16}, Scoring.parseSet("18:16"));
        assertArrayEquals(new int[]{12, 14}, Scoring.parseSet(" 12 - 14 "));
        assertArrayEquals(new int[]{11, 0}, Scoring.parseSet("11 0"));
        assertNull(Scoring.parseSet("abc"));
        assertNull(Scoring.parseSet(""));
    }

    @Test
    void spielwertung() {
        var ev = Scoring.evaluate(List.of(new int[]{11, 8}, new int[]{7, 11}, new int[]{11, 0}, new int[]{12, 14},
                new int[]{18, 16}), 3);
        assertTrue(ev.isComplete());
        assertEquals(3, ev.setsA());
        assertEquals(2, ev.setsB());
        assertEquals(1, ev.winner(), "die erste Zahl gehört der erstgenannten Partei");
        assertEquals(11 + 7 + 11 + 12 + 18, ev.pointsA());

        var open = Scoring.evaluate(List.of(new int[]{11, 8}, new int[]{11, 9}), 3);
        assertFalse(open.decided());
        assertFalse(open.isComplete());

        var invalid = Scoring.evaluate(List.of(new int[]{11, 8}, new int[]{13, 14}, new int[]{11, 3}, new int[]{11, 4}), 3);
        assertEquals(1, invalid.invalidSet());
        assertFalse(invalid.isComplete(), "ein ungültiger Satz verhindert das Speichern");

        var tooMany = Scoring.evaluate(List.of(new int[]{11, 1}, new int[]{11, 2}, new int[]{11, 3}, new int[]{3, 11}), 3);
        assertFalse(tooMany.isComplete(), "nach 3:0 kein weiterer Satz");

        assertEquals(2, Scoring.evaluate(List.of(new int[]{4, 11}, new int[]{5, 11}, new int[]{9, 11}), 3).winner());
    }

    // --- im Turnier ----------------------------------------------------------------

    private static Tournament withGroup(int n, PlayingClass c, int groupSize) {
        Tournament t = new Tournament();
        t.setTableCount(20);
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("NU-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        t.addClass(c);
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(true);
        c.setGroupSize(groupSize);
        t.drawGroups(c, true, new Random(1));
        t.startClass(c);
        return t;
    }

    private static Match between(Tournament t, PlayingClass c, String nameA, String nameB) {
        return t.matchesOf(c).stream().filter(m -> {
            String a = t.entrantName(m, m.getA()), b = t.entrantName(m, m.getB());
            return (a.startsWith(nameA) && b.startsWith(nameB)) || (a.startsWith(nameB) && b.startsWith(nameA));
        }).findFirst().orElseThrow();
    }

    /** Ergebnis so eintragen, dass {@code winner} gewinnt. */
    private static void win(Tournament t, Match m, String winner, List<int[]> setsFromWinnerView) {
        boolean winnerIsA = t.entrantName(m, m.getA()).startsWith(winner);
        List<int[]> sets = new ArrayList<>();
        for (int[] s : setsFromWinnerView) sets.add(winnerIsA ? s : new int[]{s[1], s[0]});
        t.recordResult(m, sets, 0);
    }

    @Test
    void ergebnisMitSaetzenUndAufgabe() {
        PlayingClass c = new PlayingClass("G");
        Tournament t = withGroup(2, c, 2);
        Match m = t.matchesOf(c).get(0);
        assertThrows(IllegalArgumentException.class, () -> t.recordResult(m, List.of(new int[]{11, 8}, new int[]{13, 14}), 0));
        assertThrows(IllegalArgumentException.class, () -> t.recordResult(m, List.of(new int[]{11, 8}, new int[]{11, 9}), 0),
                "noch nicht entschieden");
        assertEquals(MatchStatus.ACTIVE, m.getStatus());
        t.recordResult(m, List.of(), 1);     // A gibt auf
        assertEquals(MatchStatus.DONE, m.getStatus());
        assertEquals("0:3", m.getResultText());
        assertEquals(m.getB(), m.winner());
        assertEquals("Aufgabe A", m.getSetsText());
        assertEquals(3, m.getPoints().size());
        assertArrayEquals(new int[]{0, 11}, m.getPoints().get(0));
    }

    @Test
    void tabelleNachSpielSatzUndBallverhaeltnis() {
        PlayingClass c = new PlayingClass("G");
        c.setTiebreak(GroupStanding.Tiebreak.SETS_POINTS);
        Tournament t = withGroup(3, c, 3);
        // Jede/r gewinnt einmal: N1 > N2, N2 > N3, N3 > N1 -> Satzverhältnis entscheidet
        win(t, between(t, c, "N1", "N2"), "N1", List.of(new int[]{11, 1}, new int[]{11, 1}, new int[]{11, 1}));      // N1 3:0
        win(t, between(t, c, "N2", "N3"), "N2", List.of(new int[]{11, 9}, new int[]{9, 11}, new int[]{11, 9}, new int[]{11, 9})); // N2 3:1
        win(t, between(t, c, "N3", "N1"), "N3", List.of(new int[]{11, 9}, new int[]{9, 11}, new int[]{9, 11}, new int[]{11, 9}, new int[]{11, 9})); // N3 3:2
        var rows = t.standings(c, 0);
        // Sätze: N1 3:0 + 2:3 = 5:3 (+2); N2 0:3 + 3:1 = 3:4 (-1); N3 1:3 + 3:2 = 4:5 (-1)
        assertEquals("N1, V1", rows.get(0).entrant().name());
        assertEquals(1, rows.get(0).won());
        assertEquals(5, rows.get(0).setsWon());
        assertEquals(3, rows.get(0).setsLost());
        // N2 und N3 gleiche Satzdifferenz -1 -> Balldifferenz
        int diff2 = rows.stream().filter(r -> r.entrant().name().startsWith("N2")).findFirst().orElseThrow().pointDiff();
        int diff3 = rows.stream().filter(r -> r.entrant().name().startsWith("N3")).findFirst().orElseThrow().pointDiff();
        assertTrue(rows.get(1).pointDiff() >= rows.get(2).pointDiff());
        assertEquals(Math.max(diff2, diff3), rows.get(1).pointDiff());
        assertTrue(t.isGroupPhaseComplete(c));
    }

    @Test
    void direkterVergleichGegenSatzverhaeltnis() {
        PlayingClass c = new PlayingClass("G");
        Tournament t = withGroup(4, c, 4);
        List<int[]> clear = List.of(new int[]{11, 0}, new int[]{11, 0}, new int[]{11, 0});
        List<int[]> tight = List.of(new int[]{11, 9}, new int[]{9, 11}, new int[]{9, 11}, new int[]{11, 9}, new int[]{11, 9});
        // N1 und N2 je 2 Siege. N2 gewinnt den direkten Vergleich, N1 hat das bessere Satzverhältnis.
        win(t, between(t, c, "N2", "N1"), "N2", tight);   // N2 3:2
        win(t, between(t, c, "N1", "N3"), "N1", clear);   // N1 3:0
        win(t, between(t, c, "N1", "N4"), "N1", clear);   // N1 3:0
        win(t, between(t, c, "N3", "N2"), "N3", clear);   // N3 3:0
        win(t, between(t, c, "N2", "N4"), "N2", tight);   // N2 3:2
        win(t, between(t, c, "N4", "N3"), "N4", clear);   // N4 3:0
        assertTrue(t.isGroupPhaseComplete(c));

        c.setTiebreak(GroupStanding.Tiebreak.SETS_POINTS);
        var bySets = t.standings(c, 0);
        assertEquals("N1, V1", bySets.get(0).entrant().name());
        assertEquals("N2, V2", bySets.get(1).entrant().name());
        assertEquals(2, bySets.get(0).won());
        assertEquals(8, bySets.get(0).setsWon());
        assertEquals(3, bySets.get(0).setsLost());

        c.setTiebreak(GroupStanding.Tiebreak.HEAD_TO_HEAD);
        var byH2h = t.standings(c, 0);
        assertEquals("N2, V2", byH2h.get(0).entrant().name(), "direkter Vergleich: N2 hat gegen N1 gewonnen");
        assertEquals("N1, V1", byH2h.get(1).entrant().name());
    }

    @Test
    void koPhaseNachGruppenMitZweiAufsteigern() {
        PlayingClass c = new PlayingClass("Jungen");
        c.setNextPhase(TournamentMode.KNOCKOUT);
        c.setQualifiers(2);
        Tournament t = withGroup(12, c, 3);     // 4 Gruppen à 3
        assertEquals(4, c.getGroups().size());
        assertFalse(t.canCreateNextPhase(c));
        // alle Gruppenspiele: der Gruppenkopf (höherer TTR, steht zuerst) gewinnt immer gegen später Gesetzte
        for (Match m : t.matchesOf(c)) {
            if (m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.ACTIVE) {
                Entrant a = t.entrant(m, m.getA()), b = t.entrant(m, m.getB());
                boolean aBetter = a.ttr() > b.ttr();
                t.recordResult(m, aBetter ? List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5})
                        : List.of(new int[]{5, 11}, new int[]{5, 11}, new int[]{5, 11}), 0);
            }
        }
        assertTrue(t.canCreateNextPhase(c));
        PlayingClass ko = t.createNextPhase(c, new Random(7));
        assertEquals("Jungen – Phase 2", ko.getName());
        assertEquals("Jungen", ko.getSourceClass());
        assertEquals(ko.getName(), c.getFollowUpClass());
        assertEquals(8L, ko.getDraw().stream().filter(java.util.Objects::nonNull).count(), "4 Gruppen × 2 Aufsteiger");
        assertEquals(8, ko.getDraw().size());
        assertEquals(7, t.matchesOf(ko).size());
        assertFalse(t.canCreateNextPhase(c), "nur einmal");

        // Gruppenerste auf Setzplätzen 1–4, und kein Erstrundenspiel zweier aus derselben Gruppe
        List<Entrant> slots = t.drawSlots(ko);
        java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
        for (int g = 0; g < c.getGroups().size(); g++) for (String id : c.getGroups().get(g)) groupOf.put(id, g);
        for (int i = 0; i < 8; i += 2) {
            assertNotEquals(groupOf.get(slots.get(i).id()), groupOf.get(slots.get(i + 1).id()), "Spiel " + i);
        }
        // Gruppenerster und -zweiter derselben Gruppe in verschiedenen Hälften
        for (int g = 0; g < 4; g++) {
            int first = -1, second = -1;
            for (int i = 0; i < 8; i++) {
                if (groupOf.get(slots.get(i).id()) == g) {
                    if (first < 0) first = i;
                    else second = i;
                }
            }
            assertNotEquals(first / 4, second / 4, "Gruppe " + g + " in verschiedenen Hälften");
        }
    }

    @Test
    void weitereGruppenphase() {
        PlayingClass c = new PlayingClass("Damen");
        c.setNextPhase(TournamentMode.GROUPS);
        c.setQualifiers(2);
        Tournament t = withGroup(8, c, 4);      // 2 Gruppen à 4 -> 4 Aufsteiger
        for (Match m : t.matchesOf(c)) {
            if (m.getStatus() != MatchStatus.DONE) t.recordResult(m, 3, 0);
        }
        PlayingClass z = t.createNextPhase(c, new Random(1));
        assertEquals("Damen – Phase 2", z.getName());
        assertEquals(TournamentMode.GROUPS, z.getMode());
        assertEquals(4, z.getGroups().get(0).size());
        assertEquals(1, z.getGroups().size(), "4 Aufsteiger, Gruppengröße 4 -> 1 Gruppe");
        assertEquals(6, t.matchesOf(z).size());
    }

    @Test
    void ergebnisKorrigieren() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = new PlayingClass("KO");
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Player p = new Player("K" + i, "K" + i);
            p.setInternalNr("NU-K" + i);
            p.setTtr(2000 - i);
            ps.add(p);
        }
        t.merge(ps);
        t.addClass(c);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        Match sf1 = t.matchesOf(c).get(0), sf2 = t.matchesOf(c).get(1), fin = t.matchesOf(c).get(2);
        t.recordResult(sf1, List.of(new int[]{11, 3}, new int[]{11, 3}, new int[]{11, 3}), 0);
        assertEquals(sf1.getA(), fin.getA());
        // Korrektur: eigentlich hat B gewonnen -> Finalist wird ausgetauscht
        t.correctResult(sf1, List.of(new int[]{3, 11}, new int[]{3, 11}, new int[]{3, 11}), 0);
        assertEquals("0:3", sf1.getResultText());
        assertEquals(sf1.getB(), fin.getA());
        t.recordResult(sf2, 3, 1);
        t.recordResult(fin, 3, 0);
        // Folgespiel abgeschlossen -> Siegerwechsel nicht mehr möglich, Satzkorrektur ohne Siegerwechsel schon
        assertThrows(IllegalStateException.class,
                () -> t.correctResult(sf1, List.of(new int[]{11, 3}, new int[]{11, 3}, new int[]{11, 3}), 0));
        t.correctResult(sf1, List.of(new int[]{9, 11}, new int[]{3, 11}, new int[]{14, 16}), 0);
        assertEquals("9:11, 3:11, 14:16", sf1.getSetsText());
    }

    @Test
    void saetzeAufgabeUndPhaseWerdenGespeichert() throws Exception {
        PlayingClass c = new PlayingClass("G");
        c.setTiebreak(GroupStanding.Tiebreak.HEAD_TO_HEAD);
        c.setNextPhase(TournamentMode.KNOCKOUT);
        c.setQualifiers(1);
        Tournament t = withGroup(3, c, 3);
        t.setWinningSets(3);
        Match m1 = t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        t.recordResult(m1, List.of(new int[]{11, 8}, new int[]{7, 11}, new int[]{11, 0}, new int[]{12, 14}, new int[]{18, 16}), 0);
        Match m2 = t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        t.recordResult(m2, List.of(), 2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament l = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass lc = l.findClass("G").orElseThrow();
        assertEquals(GroupStanding.Tiebreak.HEAD_TO_HEAD, lc.getTiebreak());
        assertEquals(TournamentMode.KNOCKOUT, lc.getNextPhase());
        assertEquals(1, lc.getQualifiers());
        assertEquals(3, l.getWinningSets());
        Match l1 = l.getMatches().stream().filter(m -> m.getId().equals(m1.getId())).findFirst().orElseThrow();
        assertEquals("11:8, 7:11, 11:0, 12:14, 18:16", l1.getSetsText());
        assertEquals("3:2", l1.getResultText());
        Match l2 = l.getMatches().stream().filter(m -> m.getId().equals(m2.getId())).findFirst().orElseThrow();
        assertEquals(2, l2.getRetired());
        assertEquals("Aufgabe B", l2.getSetsText());
        assertEquals(t.standings(c, 0).get(0).entrant().id(), l.standings(lc, 0).get(0).entrant().id());
    }
}
