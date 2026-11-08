package de.hts.tournament;

import de.hts.persistence.TournamentFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.38.0 */
class Version038Test {

    // --- #48 Vereinsgleichheit: Schwächster zum Stärksten ---------------------------------------------

    /** n Teilnehmende mit TTR 2000, 1990, …; die Plätze in {@code xPlaces} (1-basiert) spielen für Verein X. */
    private static List<Entrant> field(int n, int... xPlaces) {
        java.util.Set<Integer> xs = new java.util.HashSet<>();
        for (int p : xPlaces) xs.add(p);
        List<Entrant> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) list.add(new Entrant("p" + i, "S" + i, 2000 - 10 * i, xs.contains(i) ? "X" : "Verein " + i));
        return list;
    }

    private static int groupOf(List<List<Entrant>> gs, String id) {
        for (int g = 0; g < gs.size(); g++) for (Entrant e : gs.get(g)) if (e.id().equals(id)) return g;
        return -1;
    }

    @Test
    void schwaechsterKommtZumStaerksten() {
        for (long seed = 1; seed <= 20; seed++) {
            var gs = GroupDraw.draw(field(16, 3, 5, 10, 13, 16), 4, GroupDraw.Seeding.SNAKE, true, new Random(seed));
            assertEquals(groupOf(gs, "p3"), groupOf(gs, "p16"), "Nr. 5 des Vereins (Platz 16) zu Nr. 1 (Platz 3)");
            for (var g : gs) assertEquals(4, g.size());
        }
    }

    @Test
    void sechsUndSiebenGleiche() {
        // Verein X: Plätze 1,2,3,4 (Lage 1) und 21,22,23 (Lage 2) bei 4 Gruppen à 6
        var gs = GroupDraw.draw(field(24, 1, 2, 3, 4, 21, 22, 23), 4, GroupDraw.Seeding.SNAKE, true, new Random(5));
        assertEquals(groupOf(gs, "p1"), groupOf(gs, "p23"), "Nr. 7 zu Nr. 1");
        assertEquals(groupOf(gs, "p2"), groupOf(gs, "p22"), "Nr. 6 zu Nr. 2");
        assertEquals(groupOf(gs, "p3"), groupOf(gs, "p21"), "Nr. 5 zu Nr. 3");
        var six = GroupDraw.draw(field(24, 1, 2, 3, 4, 21, 22), 4, GroupDraw.Seeding.SNAKE, true, new Random(5));
        assertEquals(groupOf(six, "p1"), groupOf(six, "p22"), "Nr. 6 zu Nr. 1");
        assertEquals(groupOf(six, "p2"), groupOf(six, "p21"), "Nr. 5 zu Nr. 2");
    }

    @Test
    void dritteLageWiederVorwaerts() {
        // 12 Gleiche bei 4 Gruppen à 10: A = 1, 8, 9 · B = 2, 7, 10 · C = 3, 6, 11 · D = 4, 5, 12 (Vereinsnummern)
        int[] x = {1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 23, 24};
        var gs = GroupDraw.draw(field(40, x), 4, GroupDraw.Seeding.SNAKE, true, new Random(2));
        int[][] together = {{1, 8, 9}, {2, 7, 10}, {3, 6, 11}, {4, 5, 12}};
        for (int[] t : together) {
            int g = groupOf(gs, "p" + x[t[0] - 1]);
            for (int k : t) assertEquals(g, groupOf(gs, "p" + x[k - 1]), "Vereins-Nr. " + k + " bei Nr. " + t[0]);
        }
    }

    @Test
    void funktioniertFuerAlleGruppengroessen() {
        for (int size = 3; size <= 10; size++) {
            for (int perGroup = 2; perGroup <= Math.min(5, size); perGroup++) {
                int groups = 4, n = groups * size;
                int clubCount = groups * (perGroup - 1) + 1;                     // erzwingt perGroup in einer Gruppe
                int[] x = new int[clubCount];
                for (int i = 0; i < clubCount; i++) x[i] = 1 + i * n / clubCount;
                var gs = GroupDraw.draw(field(n, x), groups, GroupDraw.Seeding.SNAKE, true, new Random(size * 10L + perGroup));
                assertEquals(n, gs.stream().mapToInt(List::size).sum());
                assertEquals(groupOf(gs, "p" + x[0]), groupOf(gs, "p" + x[clubCount - 1]), "Schwächster bei Stärkstem, Größe " + size);
                int maxX = gs.stream().mapToInt(g -> (int) g.stream().filter(e -> "X".equals(e.detail())).count()).max().orElse(0);
                assertEquals(perGroup, maxX, "höchstens " + perGroup + " aus X je Gruppe, Größe " + size);
            }
        }
    }

    @Test
    void doppelBleibenUnveraendert() {
        List<Entrant> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) list.add(new Entrant("pair:" + i, "D" + i, 2000 - i, i == 1 || i == 3 || i == 5 ? "X" : "V" + i));
        var a = GroupDraw.draw(list, 2, GroupDraw.Seeding.SNAKE, true, new Random(1));
        assertEquals(8, a.stream().mapToInt(List::size).sum());
    }

    // --- #47 Satz ohne Vorzeichen --------------------------------------------------------------------

    @Test
    void satzOhneVorzeichenGiltAlsPlus() {
        assertArrayEquals(new int[]{11, 8}, Scoring.parseSet("8"));
        assertArrayEquals(new int[]{11, 8}, Scoring.parseSet("+8"));
        assertArrayEquals(new int[]{8, 11}, Scoring.parseSet("-8"));
        assertArrayEquals(new int[]{12, 10}, Scoring.parseSet("10"));
        assertArrayEquals(new int[]{11, 0}, Scoring.parseSet("0"));
        assertArrayEquals(new int[]{11, 8}, Scoring.parseSet("11:8"));
        assertArrayEquals(new int[]{7, 11}, Scoring.parseSet("−7"));
    }

    // --- #46 Gruppen mit mehr Runden beginnen früher -------------------------------------------------

    @Test
    void grosseGruppenBeginnenFrueher() {
        Tournament t = new Tournament();
        PlayingClass c = PhaseFixtures.groupsClass(t, "K", 18, 4);          // 2 × 5 und 2 × 4
        List<Match> ms = t.matchesOf(c);
        // die ersten Spiele gehören zu 5er-Gruppen (Runde 1 und 2 vorweg)
        java.util.Set<Integer> fives = new java.util.HashSet<>();
        for (int g = 0; g < c.getGroups().size(); g++) if (c.getGroups().get(g).size() == 5) fives.add(g);
        assertEquals(2, fives.size());
        for (int i = 0; i < 8; i++) assertTrue(fives.contains(ms.get(i).getGroupIndex()), "Spiel " + (i + 1));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ms.stream().limit(10).map(Match::getNumber).toList());
        // letzte Runde: alle Gruppen gemeinsam
        java.util.Set<Integer> lastSlot = new java.util.HashSet<>();
        for (Match m : ms.subList(ms.size() - 8, ms.size())) lastSlot.add(m.getGroupIndex());
        assertEquals(4, lastSlot.size());
    }

    // --- #59 / #57 Tischzuordnung ---------------------------------------------------------------------

    @Test
    void tischzuordnungErzeugtKeineKlassenkopien() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(16);
        t.addClass(new PlayingClass("Jugend 13 Einzel"));
        t.addClass(new PlayingClass("Damen"));
        for (int i = 1; i <= 16; i++) t.setTableExcluded(i, List.of("Damen"));
        t.setTableExcluded(3, List.of());                                   // Haken wieder gesetzt
        for (int round = 0; round < 3; round++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TournamentFile.write(out, TournamentFile.Content.of(t));
            t = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
            assertEquals(2, t.getClasses().size());
        }
        assertEquals(java.util.Set.of("Damen"), t.getTableExcluded(1));
        assertTrue(t.getTableExcluded(3).isEmpty());
        for (int i = 1; i <= 16; i++) t.setTableExcluded(i, List.of("Damen", "Jugend 13 Einzel"));
        assertTrue(t.tableHasNoClass(5));
        assertFalse(t.tableAllows(5, "Damen"));
    }

    @Test
    void beschaedigteDateiWirdRepariertUndAlteZuordnungUmgerechnet() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hts-tournament name=\"T\" tables=\"3\">"
                + "<table-classes><table nr=\"1\"><class name=\"A\"/></table></table-classes>"
                + "<classes><class name=\"A\"/><class name=\"A\" type=\"adults\" mode=\"groups\" ready=\"true\"/>"
                + "<class name=\"B\" type=\"adults\"/></classes><players/></hts-tournament>";
        Tournament t = TournamentFile.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))).toTournament();
        assertEquals(2, t.getClasses().size(), "leere Kopie entfernt");
        assertEquals(TournamentMode.GROUPS, t.findClass("A").orElseThrow().getMode(), "echte Klasse bleibt");
        assertEquals(java.util.Set.of("B"), t.getTableExcluded(1), "Tisch 1 nur für A → B ausgeschlossen");
    }

    // --- #60 / #50 Qualifikation: Phase starten, nach Runde 1 entscheiden ---------------------------

    private static PlayingClass qualPhase(Tournament t) {
        PlayingClass c = PhaseFixtures.groupsClass(t, "Q", 16, 4);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        return t.createQualificationPhase(c, 1, 2, 2, 0, new Random(1));      // 8 in der KO-Qualifikation
    }

    @Test
    void qualifikationStartetErstPerKnopf() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass q = qualPhase(t);
        assertTrue(q.isAwaitingStart());
        assertTrue(t.matchesOf(q).stream().allMatch(m -> m.getStatus() == MatchStatus.PLANNED), "noch keine Tische belegt");
        List<String> field = new ArrayList<>(q.getQualField().keySet());
        Match m1 = t.matchesOf(q).get(0), m2 = t.matchesOf(q).get(1);
        String a = m1.getA(), b = m2.getA();
        t.swapQualificationPlayers(q, a, b);                                  // Drag & Drop vor dem Start
        assertEquals(b, m1.getA());
        assertEquals(a, m2.getA());
        t.resumeClass(q);
        assertFalse(q.isAwaitingStart());
        assertTrue(t.matchesOf(q).stream().anyMatch(m -> m.getStatus() == MatchStatus.ACTIVE));
        assertEquals(8, field.size());
    }

    @Test
    void nachRundeEinsGewinnerQualifiziert() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass q = qualPhase(t);
        t.resumeClass(q);
        for (Match m : List.copyOf(t.matchesOf(q))) PhaseFixtures.play(t, m);
        assertTrue(t.qualificationDecisionPending(q));
        assertFalse(t.isPhaseComplete(q), "bis zur Entscheidung nicht abgeschlossen");
        assertEquals(4, t.matchesOf(q).size(), "keine Runde 2 von selbst");
        t.decideQualification(q, true);
        assertTrue(t.isPhaseComplete(q));
        PlayingClass root = t.rootOf(q);
        var rows = t.finalResult(root).rows();
        assertEquals(4, rows.stream().filter(r -> r.info().equals(Tournament.QUALIFIED_BY_KO)).count());
        List<Integer> ttr = rows.stream().filter(r -> r.info().equals("Nachrücker 1. Ranges")).map(r -> r.entrant().ttr()).toList();
        assertEquals(4, ttr.size());
        assertEquals(ttr.stream().sorted(java.util.Comparator.reverseOrder()).toList(), ttr, "Nachrücker 1. Ranges nach TTR");
        // zurücknehmen und doch eine weitere Runde
        t.undoQualificationDecision(q);
        assertTrue(t.qualificationDecisionPending(q));
        t.decideQualification(q, false);
        assertEquals(6, t.matchesOf(q).size(), "Runde 2 für die 4 Gewinner");
        t.undoQualificationDecision(q);
        assertEquals(4, t.matchesOf(q).size(), "Runde 2 wieder entfernt");
        // speichern/laden behält die Entscheidung
        t.decideQualification(q, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertEquals(1, back.findClass(q.getName()).orElseThrow().getQualDecision());
    }

    // --- #61 KO mit bestimmten Gruppenplätzen ---------------------------------------------------------

    @Test
    void koMitBestimmtenGruppenplaetzen() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "B", 20, 4);           // 4 Gruppen à 5
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        assertNotNull(t.placesProblem(c, List.of(2, 6)));
        assertNotNull(t.placesProblem(c, List.of(2, 2)));
        assertNull(Tournament.parsePlaces("2,x"));
        assertEquals(List.of(2, 3, 4), Tournament.parsePlaces(" 2, 3 ,4"));
        PlayingClass ko = t.createNextPhase(c, List.of(2, 3, 4), new Random(1));
        assertEquals(TournamentMode.KNOCKOUT, ko.getMode());
        assertEquals(12, ko.getDraw().stream().filter(java.util.Objects::nonNull).count());
        java.util.Set<String> firsts = new java.util.HashSet<>();
        for (int g = 0; g < 4; g++) firsts.add(t.standings(c, g).get(0).entrant().id());
        assertTrue(ko.getDraw().stream().noneMatch(firsts::contains), "Gruppenerste nicht im KO");
        assertTrue(ko.getSeedLabels().values().stream().allMatch(l -> l.endsWith("2") || l.endsWith("3") || l.endsWith("4")));
        PhaseFixtures.playAll(t, ko);
        var rows = t.finalResult(c).rows();
        assertEquals(20, rows.size());
        assertTrue(rows.subList(0, 4).stream().allMatch(r -> r.info().endsWith("Platz 1")), "Gruppenerste vorn");
        assertEquals(5, rows.get(4).from(), "dann das KO");
        assertTrue(rows.subList(16, 20).stream().allMatch(r -> r.info().endsWith("Platz 5")));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertEquals(List.of(2, 3, 4), back.findClass("B").orElseThrow().getQualifierPlaces());
    }

    // --- #51 vorzeitig beenden, #58 neu starten -----------------------------------------------------

    @Test
    void phaseVorzeitigBeendenUndNeuStarten() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(2);
        PlayingClass c = PhaseFixtures.groupsClass(t, "E", 8, 2);
        t.startClass(c);
        for (int i = 0; i < 3; i++) {
            Match m = t.matchesOf(c).stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
            PhaseFixtures.play(t, m);
        }
        long open = t.openMatchCount(c);
        assertTrue(open > 0);
        t.endPhaseEarly(c);
        assertTrue(c.isFinished());
        assertEquals(open, t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.CANCELLED).count());
        assertTrue(t.freeTables().size() == 2, "Tische frei");
        assertEquals(3, t.matchesOf(c).stream().filter(Match::isExported).count(), "nur gespielte Spiele im Export");
        assertEquals(8, t.finalResult(c).rows().size());
        assertTrue(t.finalResult(c).rows().stream().noneMatch(r -> r.info().contains("Zwischenstand")));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertTrue(back.findClass("E").orElseThrow().isEndedEarly());
        t.undoEndPhaseEarly(c);
        assertFalse(c.isFinished());
        assertEquals(0, t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.CANCELLED).count());
        // neu starten: Auslosung bleibt, Ergebnisse weg
        List<List<String>> groups = List.copyOf(c.getGroups());
        assertEquals(3, t.restartResultCount(c));
        t.restartPhase(c);
        assertEquals(groups, c.getGroups());
        assertEquals(0, t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.DONE).count());
        assertEquals(2, t.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).count());
    }

    @Test
    void neuStartNurOhneFolgephase() {
        Tournament t = new Tournament();
        t.setTableCount(6);
        PlayingClass c = PhaseFixtures.groupsClass(t, "F", 8, 2);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        assertNotNull(t.restartProblem(c));
        PhaseFixtures.playAll(t, ko);
        int koMatches = t.matchesOf(ko).size();
        t.restartPhase(ko);
        assertEquals(koMatches, t.matchesOf(ko).size());
        assertTrue(t.matchesOf(ko).stream().filter(m -> m.getKoRound() > 0).allMatch(m -> m.getA() == null && m.getB() == null),
                "spätere Runden wieder ohne Gegner");
    }

    // --- #55 Nachrücker, #54 Abmeldung per Handy ------------------------------------------------------

    @Test
    void nachrueckerOhneAnmeldestatusUndHandyAbmeldungGemerkt() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = t.addClass(new PlayingClass("N"));
        de.hts.player.Player r = new de.hts.player.Player("Rita", "Reserve");
        r.addWaitingList("N");
        de.hts.player.Player p = new de.hts.player.Player("Paul", "Phone");
        p.addRegistration("N");
        t.merge(List.of(r, p));
        assertTrue(r.isReserveOnly());
        assertFalse(p.isReserveOnly());
        t.assign(r, c);
        assertEquals(de.hts.player.Attendance.PRESENT, r.getAttendance(), "übernommener Nachrücker gilt als angemeldet");
        t.setAttendance(p, de.hts.player.Attendance.WITHDRAW_REQUESTED);
        t.setAttendance(p, de.hts.player.Attendance.WITHDRAWN);
        assertTrue(p.isWithdrawnByPhone());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertTrue(back.getPlayers().stream().filter(x -> x.getFirstname().equals("Paul")).findFirst().orElseThrow().isWithdrawnByPhone());
        try (var doc = de.hts.report.PdfReports.withdrawnList(t)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
