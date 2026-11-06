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

/** Neuerungen 0.20.0: Spiel um Platz 3, Gewinnsätze je Klasse, „A1“-Kennungen, Schweizer System, Vereine in der Quali. */
class Version020Test {

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

    private static PlayingClass koClass(Tournament t, String name, int n) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        players(t, name.substring(0, 1), n, 2000).forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        return c;
    }

    private static PlayingClass groupsClass(Tournament t, String name, int n, int groups) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        players(t, name.substring(0, 1), n, 2000).forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(groups);
        t.drawGroups(c, true, new Random(3));
        return c;
    }

    /** Höherer TTR gewinnt; {@code until} = solange spielen, wie die Bedingung für das nächste Spiel gilt. */
    private static void play(Tournament t, PlayingClass c, java.util.function.Predicate<Match> which) {
        for (int guard = 0; guard < 500; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .filter(which).findFirst().orElse(null);
            if (open == null) return;
            Entrant a = t.entrant(open, open.getA()), b = t.entrant(open, open.getB());
            int ws = t.winningSets(open);
            List<int[]> sets = new ArrayList<>();
            for (int i = 0; i < ws; i++) sets.add(a.ttr() > b.ttr() ? new int[]{11, 5} : new int[]{5, 11});
            t.recordResult(open, sets, 0);
        }
    }

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    @Test
    void spielUmPlatz3VorDemStart() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = koClass(t, "KO", 8);
        assertTrue(t.canHaveThirdPlace(c));
        t.setThirdPlace(c, true);
        assertEquals(8, t.matchesOf(c).size(), "4 + 2 + Spiel um Platz 3 + Finale");
        Match third = t.thirdPlaceMatch(c);
        Match fin = t.matchesOf(c).stream().filter(m -> m.getStage().equals("Finale")).findFirst().orElseThrow();
        assertTrue(third.getNumber() < fin.getNumber(), "Spiel um Platz 3 vor dem Finale");
        t.startClass(c);
        play(t, c, m -> true);
        assertTrue(t.isPhaseComplete(c));
        assertThrows(IllegalStateException.class, () -> t.setThirdPlace(c, false), "gespielt – nicht mehr abschaltbar");
        t.finishClass(c);
        List<Integer> from = t.finalResult(c).rows().stream().map(Tournament.Placing::from).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 5, 5, 5), from);
        assertEquals("Spiel um Platz 3 (Sieg)", t.finalResult(c).rows().get(2).info());

        Tournament back = roundTrip(t);
        PlayingClass c2 = back.findClass("KO").orElseThrow();
        assertTrue(c2.isThirdPlace());
        assertEquals(from, back.finalResult(c2).rows().stream().map(Tournament.Placing::from).toList());
    }

    @Test
    void spielUmPlatz3NachDenHalbfinalsZuschalten() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = koClass(t, "Spaet", 6);       // 8er-Feld mit 2 Freilosen
        t.startClass(c);
        play(t, c, m -> !m.getStage().equals("Finale"));
        assertNull(t.thirdPlaceMatch(c));
        // ohne Spiel um Platz 3: zweimal Platz 3
        assertEquals(2, t.finalResult(c).rows().stream().filter(r -> r.from() == 3).count());
        t.setThirdPlace(c, true);
        Match third = t.thirdPlaceMatch(c);
        assertTrue(third.isReady(), "Halbfinalverlierer stehen fest");
        assertTrue(third.getStatus() == MatchStatus.QUEUED || third.getStatus() == MatchStatus.ACTIVE, "sofort eingereiht");
        if (third.getStatus().atTable()) {
            assertThrows(IllegalStateException.class, () -> t.setThirdPlace(c, false), "läuft schon am Tisch");
        } else {
            t.setThirdPlace(c, false);
            assertNull(t.thirdPlaceMatch(c), "abschaltbar, solange nicht am Tisch");
            t.setThirdPlace(c, true);
        }
        play(t, c, m -> true);
        t.finishClass(c);
        assertEquals(List.of(1, 2, 3, 4), t.finalResult(c).rows().stream().limit(4).map(Tournament.Placing::from).toList());
    }

    @Test
    void spielUmPlatz3KorrekturImHalbfinale() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = koClass(t, "Korr", 4);
        t.setThirdPlace(c, true);
        t.startClass(c);
        play(t, c, m -> true);
        Match semi = t.matchesOf(c).stream().filter(m -> m.getStage().equals("Halbfinale")).findFirst().orElseThrow();
        Match third = t.thirdPlaceMatch(c);
        String oldLoser = semi.loser();
        List<int[]> flipped = new ArrayList<>();
        for (int i = 0; i < 3; i++) flipped.add(semi.winner().equals(semi.getA()) ? new int[]{5, 11} : new int[]{11, 5});
        assertThrows(Tournament.FollowUpConflict.class, () -> t.correctResult(semi, flipped, 0));
        t.correctResult(semi, flipped, 0, true);
        assertNotEquals(oldLoser, semi.loser());
        assertTrue(semi.loser().equals(third.getA()) || semi.loser().equals(third.getB()), "neuer Halbfinalverlierer im Spiel um Platz 3");
        assertNotEquals(MatchStatus.DONE, third.getStatus());
    }

    @Test
    void keinSpielUmPlatz3InDerQualifikationOderBeiFreilosImHalbfinale() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass small = koClass(t, "Drei", 3);    // 4er-Feld, ein Halbfinale ist ein Freilos
        assertFalse(t.canHaveThirdPlace(small));
        assertThrows(IllegalStateException.class, () -> t.setThirdPlace(small, true));

        PlayingClass g = groupsClass(t, "Quali", 12, 2);
        t.startClass(g);
        play(t, g, m -> true);
        PlayingClass q = t.createQualificationPhase(g, 2, 4, 2, new Random(1));
        assertFalse(t.canHaveThirdPlace(q));
        assertThrows(IllegalStateException.class, () -> t.setThirdPlace(q, true));
    }

    @Test
    void phase2KoMitGruppenkennungUndSpielUmPlatz3() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Gruppen", 16, 4);
        t.startClass(c);
        play(t, c, m -> true);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        assertEquals(8, ko.getSeedLabels().size());
        assertEquals(java.util.Set.of("A1", "B1", "C1", "D1", "A2", "B2", "C2", "D2"), new java.util.HashSet<>(ko.getSeedLabels().values()));
        for (int g = 0; g < 4; g++) {
            String first = t.standings(c, g).get(0).entrant().id();
            assertEquals(GroupDraw.groupName(g) + "1", ko.getSeedLabels().get(first));
        }
        t.setThirdPlace(ko, true);       // auch in Phase 2, auch laufend
        play(t, ko, m -> true);
        t.finishClass(ko);
        assertEquals(List.of(1, 2, 3, 4), t.finalResult(c).rows().stream().limit(4).map(Tournament.Placing::from).toList());

        Tournament back = roundTrip(t);
        PlayingClass ko2 = back.findClass(ko.getName()).orElseThrow();
        assertEquals(ko.getSeedLabels(), ko2.getSeedLabels());
    }

    @Test
    void gewinnsaetzeJeKlasse() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass vier = koClass(t, "Vier", 2);
        PlayingClass drei = koClass(t, "Drei", 2);
        t.setWinningSets(vier, 4);
        assertEquals(4, vier.getWinningSets());
        assertEquals(3, drei.getWinningSets());
        t.startClass(vier);
        t.startClass(drei);
        Match mv = t.matchesOf(vier).get(0), md = t.matchesOf(drei).get(0);
        List<int[]> three = List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5});
        assertThrows(IllegalArgumentException.class, () -> t.recordResult(mv, three, 0), "bei 4 Gewinnsätzen nicht entschieden");
        t.recordResult(md, three, 0);
        assertEquals(3, md.getSetsA());
        t.recordResult(mv, List.of(new int[]{11, 5}, new int[]{5, 11}, new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5}), 0);
        assertEquals(4, mv.getSetsA());
        assertEquals(1, mv.getSetsB());
        assertThrows(IllegalStateException.class, () -> t.setWinningSets(vier, 3), "nach dem ersten Ergebnis fest");

        Tournament back = roundTrip(t);
        assertEquals(4, back.findClass("Vier").orElseThrow().getWinningSets());
        assertEquals(3, back.findClass("Drei").orElseThrow().getWinningSets());
    }

    @Test
    void gewinnsaetzeGeltenAuchInPhase2UndBeiAufgabe() {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Lang", 8, 2);
        t.setWinningSets(c, 4);
        t.startClass(c);
        play(t, c, m -> true);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        Match m = t.matchesOf(ko).get(0);
        assertEquals(4, t.winningSets(m));
        t.recordResult(m, List.of(new int[]{11, 5}), 1);    // A gibt auf
        assertEquals(0, m.getSetsA());
        assertEquals(4, m.getSetsB());
    }

    @Test
    void schweizerSystemOhneFolgephase() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = t.addClass(new PlayingClass("Schweiz"));
        players(t, "S", 8, 1800).forEach(p -> t.assign(p, c));
        c.setSwissRounds(3);
        t.drawSwiss(c, true, new Random(1));
        assertFalse(t.canHaveNextPhase(c));
    }

    @Test
    void qualifikationVermeidetVereinsgleichheit() {
        // 3 Gruppen: Zweiter Gruppe g hat den Verein des Dritten aus Gruppe g-1 -> nur eine gültige Zuordnung
        for (int seed = 0; seed < 15; seed++) {
            Tournament t = new Tournament();
            t.setTableCount(20);
            t.setQualificationRandom(new Random(seed));
            PlayingClass g = groupsClass(t, "Herren", 16, 4);       // 8 in der KO-Qualifikation
            t.startClass(g);
            play(t, g, m -> true);
            for (int gi = 0; gi < 4; gi++) {
                var rows = t.standings(g, gi);
                String second = rows.get(1).entrant().id(), third = rows.get(2).entrant().id();
                for (Player p : t.getPlayers()) {
                    if (p.getId().equals(second)) p.setClubName("Verein " + gi);
                    if (p.getId().equals(third)) p.setClubName("Verein " + ((gi + 1) % 4));
                }
            }
            PlayingClass q = t.createQualificationPhase(g, 1, 2, 1, new Random(seed));
            for (Match m : t.matchesOf(q)) {
                Entrant a = t.entrant(m, m.getA()), b = t.entrant(m, m.getB());
                assertFalse(a.sameClub(b), "seed " + seed + ": " + a.name() + " – " + b.name());
                assertNotEquals(q.getQualField().get(m.getA()), q.getQualField().get(m.getB()), "andere Gruppe");
                assertEquals(2, q.getQualPlace().get(m.getA()));
                assertEquals(3, q.getQualPlace().get(m.getB()));
            }
        }
    }
}
