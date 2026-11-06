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

/** Neuerungen 0.32.0: 2er/3er Turnier (Teams, Begegnung als ein Spiel), Kennzeichen „fertig importiert“. */
class Version032Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static Player p(String last, int ttr, Sex sex) {
        Player x = new Player("V" + last, last);
        x.setInternalNr(last);
        x.setTtr(ttr);
        x.setSex(sex);
        return x;
    }

    private static Tournament handicapTournament() {
        Tournament t = new Tournament();
        t.setHandicap(true);
        t.setTableCount(4);
        t.setTest(true);
        HandicapRules r = new HandicapRules();
        r.setMode(HandicapRules.Mode.DISTANCE);
        r.setRowCount(HandicapRules.Mode.DISTANCE, 2);
        for (boolean w : new boolean[]{false, true}) {
            r.bounds(HandicapRules.Mode.DISTANCE, w).setFirstLower(0);
            r.bounds(HandicapRules.Mode.DISTANCE, w).getUppers().clear();
            r.bounds(HandicapRules.Mode.DISTANCE, w).getUppers().addAll(List.of(100, 3000));
        }
        r.balls(HandicapRules.Mode.DISTANCE).clear();
        r.balls(HandicapRules.Mode.DISTANCE).addAll(List.of(1, 3));
        t.setHandicapRules(r);
        return t;
    }

    @Test
    void dreierTeamsMitDameZulosenUndPartien() throws Exception {
        Tournament t = handicapTournament();
        PlayingClass c = t.addClass(new PlayingClass("3er", Discipline.TEAM3, ClassType.ADULTS, null, null));
        c.setWomanRequired(true);
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 8; i++) ps.add(p("H" + i, 1300 + i * 20, Sex.MALE));
        for (int i = 1; i <= 4; i++) ps.add(p("D" + i, 1200 + i * 20, Sex.FEMALE));
        t.merge(ps);
        t.getPlayers().forEach(x -> t.assign(x, c));
        assertTrue(t.pairsIn(c).stream().allMatch(pr -> pr.getCapacity() == 3));
        Tournament.PartnerDraw draw = t.drawPartners(c, new Random(1));
        assertEquals(4, draw.pairs().size());
        assertTrue(draw.leftOvers().isEmpty());
        for (DoublesPair team : t.pairsIn(c)) {
            assertTrue(team.isComplete());
            assertEquals(Sex.FEMALE, team.getThird().getSex(), "Standard-Aufstellung H1, H2, D: Dame auf Platz 3");
        }
        assertTrue(Entrant.ofTeam(t.pairsIn(c).get(0)).name().matches("[^/]+/[^/]+/[^/]+"), "Teamname aus den Nachnamen");

        c.setGroupsBySize(false);
        c.setGroupCount(1);
        t.drawGroups(c, true, new Random(2));
        assertTrue(t.handicapProblems(c).isEmpty(), t.handicapProblems(c).toString());
        t.startClass(c);
        Match m = t.getMatches().stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        assertTrue(t.isTeamMatch(m));
        List<Tournament.Rubber> rubbers = t.rubbersOf(m);
        assertEquals(List.of("Mixed", "Herren-Einzel", "Damen-Einzel", "Herren-Doppel", "Mixed"),
                rubbers.stream().map(Tournament.Rubber::label).toList());
        assertEquals(List.of("H1", "H2", "D"), t.teamPositions(c));

        List<String> la = t.defaultLineup(m, m.getA()), lb = t.defaultLineup(m, m.getB());
        List<String> wrong = List.of(la.get(2), la.get(1), la.get(0));          // Dame auf H1 → D ist ein Herr
        assertThrows(IllegalArgumentException.class, () -> t.recordTeamResult(m, wrong, lb, List.of(1, 1, 2, 2, 1), false));
        assertThrows(IllegalArgumentException.class, () -> t.recordTeamResult(m, la, lb, List.of(1, 1), false), "alle Partien");
        Integer table = m.getTable();
        t.recordTeamResult(m, la, lb, List.of(1, 1, 2, 2, 1), false);
        assertEquals(MatchStatus.DONE, m.getStatus());
        assertEquals("3:2", m.getResultText());
        assertEquals("", m.getSetsText(), "keine Sätze bei Team-Begegnungen");
        assertNotNull(table);
        assertEquals(la, m.getLineupA());

        // Gruppenwertung: Begegnungen, dann Partien
        List<GroupStanding.Row> rows = t.standings(c, 0);
        GroupStanding.Row winner = rows.stream().filter(r -> r.entrant().id().equals(m.getA())).findFirst().orElseThrow();
        assertEquals(1, winner.won());
        assertEquals(3, winner.setsWon());

        int sims = t.simulateAll(new Random(3));
        assertTrue(sims > 0);
        assertTrue(t.matchesOf(c).stream().allMatch(x -> x.getStatus() == MatchStatus.DONE));

        Tournament back = roundTrip(t);
        PlayingClass bc = back.findClass("3er").orElseThrow();
        assertEquals(Discipline.TEAM3, bc.getDiscipline());
        assertTrue(bc.isWomanRequired());
        assertEquals(4, back.pairsIn(bc).size());
        assertTrue(back.pairsIn(bc).stream().allMatch(pr -> pr.getThird() != null && pr.getCapacity() == 3));
        Match bm = back.getMatches().stream().filter(x -> x.getId().equals(m.getId())).findFirst().orElseThrow();
        assertEquals(la, bm.getLineupA());
    }

    @Test
    void zweierTurnierGruppeDreiKoFuenfPartien() {
        Tournament t = handicapTournament();
        PlayingClass c = t.addClass(new PlayingClass("2er", Discipline.TEAM2, ClassType.ADULTS, null, null));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 8; i++) ps.add(p("S" + i, 1300 + i * 30, Sex.MALE));
        t.merge(ps);
        t.getPlayers().forEach(x -> t.assign(x, c));
        t.drawPartners(c, new Random(1));
        assertEquals(4, t.entrantsOf(c).size());
        assertTrue(t.entrantsOf(c).get(0).name().matches("[^/ ]+/[^/ ]+"), "Teamname aus den Nachnamen, z. B. „S1/S2“");
        t.drawKnockout(c, true, new Random(2));
        t.startClass(c);
        Match ko = t.getMatches().stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        assertEquals(5, t.rubbersOf(ko).size(), "KO: A1–B1, A2–B2, Doppel, A1–B2, A2–B1");
        Tournament.Rubber cross = t.rubbersOf(ko).get(3);
        assertArrayEquals(new int[]{0}, cross.posA());
        assertArrayEquals(new int[]{1}, cross.posB());

        Tournament g = handicapTournament();
        PlayingClass gc = g.addClass(new PlayingClass("2er G", Discipline.TEAM2, ClassType.ADULTS, null, null));
        List<Player> gs = new ArrayList<>();
        for (int i = 1; i <= 6; i++) gs.add(p("G" + i, 1300 + i * 30, Sex.MALE));
        g.merge(gs);
        g.getPlayers().forEach(x -> g.assign(x, gc));
        g.drawPartners(gc, new Random(1));
        gc.setGroupsBySize(false);
        gc.setGroupCount(1);
        g.drawGroups(gc, true, new Random(2));
        g.startClass(gc);
        Match gm = g.getMatches().stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        assertEquals(3, g.rubbersOf(gm).size(), "Gruppenphase: A1–B1, A2–B2, Doppel");
        // Vorgabe je Partie nach den Vorgaberegeln
        List<String> la = g.defaultLineup(gm, gm.getA()), lb = g.defaultLineup(gm, gm.getB());
        int h = g.rubberHandicap(gm, g.rubbersOf(gm).get(0), la, lb);
        assertTrue(Math.abs(h) <= 3);
    }

    @Test
    void zweierMitDameDameAufPlatzZwei() {
        Tournament t = handicapTournament();
        PlayingClass c = t.addClass(new PlayingClass("2er Mixed", Discipline.TEAM2, ClassType.ADULTS, null, null));
        c.setWomanRequired(true);
        Player d = p("Dame", 1250, Sex.FEMALE), h = p("Herr", 1400, Sex.MALE);
        t.merge(List.of(d, h));
        DoublesPair team = t.createTeam(c, List.of(d, h));
        assertEquals(h, team.getFirst());
        assertEquals(d, team.getSecond(), "Dame automatisch Teilnehmende 2");
    }

    @Test
    void fertigImportiertWirdGespeichert() throws Exception {
        Tournament t = new Tournament();
        t.setImportedFinished(true);
        assertTrue(roundTrip(t).isImportedFinished());
        assertFalse(roundTrip(new Tournament()).isImportedFinished());
    }
}
