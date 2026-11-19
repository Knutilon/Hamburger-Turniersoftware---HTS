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
import de.hts.report.PdfReports;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.2.0 – Haupt- und Trostrunde (#94), Badeni (#95), fortgesetztes KO (#96), Leistungsstufen und Phase 3 (#97),
 * TOP24/TOP32 (#98/#100), doppeltes KO mit Stichkampf (#99).
 */
class Version120Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static PlayingClass played(Tournament t, String name, int n, int groups) {
        t.setTableCount(12);
        PlayingClass c = PhaseFixtures.groupsClass(t, name, n, groups);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        return c;
    }

    /** Plätze 1, 2, 3 … ohne Lücken und ohne Doppelungen. */
    private static void assertDistinctPlaces(List<Tournament.Placing> rows, int n) {
        assertEquals(n, rows.size(), rows.toString());
        for (int i = 0; i < n; i++) {
            assertEquals(i + 1, rows.get(i).from(), rows.toString());
            assertEquals(i + 1, rows.get(i).to(), rows.toString());
        }
    }

    private static void assertCovers(List<Tournament.Placing> rows, int n) {
        assertEquals(n, rows.size(), rows.toString());
        Set<String> ids = new HashSet<>();
        for (Tournament.Placing p : rows) {
            assertTrue(ids.add(p.entrant().id()), "doppelt: " + p);
            assertTrue(p.from() >= 1 && p.to() <= n && p.from() <= p.to(), p.toString());
        }
        assertEquals(1, rows.get(0).from());
    }

    // --- #94 Haupt- und Trostrunde ------------------------------------------------------------------

    @Test
    void hauptUndTrostrundeMitGetrenntenErgebnissenUndFortlaufendemExport() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "HT", 16, 4);
        assertTrue(t.planKindsAfter(c).contains(Plan.Kind.MAIN_CONSOLATION));
        assertNotNull(t.planProblem(c, Plan.Kind.MAIN_CONSOLATION, new Tournament.PlanOptions(List.of(1, 2), List.of(2, 3), false, false, 0)));
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.MAIN_CONSOLATION,
                new Tournament.PlanOptions(List.of(1, 2), List.of(3, 4), true, false, 0), new Random(1));
        assertEquals(TournamentMode.PLAN, p.getMode());
        assertEquals(List.of("Hauptrunde", "Trostrunde"), p.getPlan().getResultTitles());
        assertFalse(t.matchesOf(p).isEmpty());
        assertTrue(t.matchesOf(p).stream().allMatch(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.ACTIVE));
        PhaseFixtures.playAll(t, p);
        assertTrue(t.isPhaseComplete(p));
        List<Tournament.ResultSection> sections = t.finalResultSections(c);
        assertEquals(2, sections.size());
        assertEquals("Hauptrunde", sections.get(0).title());
        List<Tournament.Placing> main = sections.get(0).rows(), cons = sections.get(1).rows();
        assertEquals(8, main.size());
        assertEquals(1, main.get(0).from());
        assertEquals(3, main.get(2).from(), "Spiel um Platz 3");
        assertEquals(4, main.get(3).from());
        assertEquals(5, main.get(4).from());
        assertEquals(8, main.get(4).to());
        assertEquals(8, cons.size());
        assertEquals(1, cons.get(0).from(), "Trostrunde zählt eigens ab 1");
        assertEquals(3, cons.get(2).from());
        assertEquals(4, cons.get(2).to(), "ohne Spiel um Platz 3");
        // click-TT: Trostrunde hinter der Hauptrunde (Sieger der Trostrunde = 9.)
        List<Tournament.Placing> all = t.finalResult(c).rows();
        assertCovers(all, 16);
        assertEquals(cons.get(0).entrant().id(), all.stream().filter(x -> x.from() == 9).findFirst().orElseThrow().entrant().id());
        // Speichern und Laden
        Tournament t2 = roundTrip(t);
        PlayingClass p2 = t2.findClass(p.getName()).orElseThrow();
        assertEquals(Plan.Kind.MAIN_CONSOLATION, p2.getPlan().getKind());
        assertEquals(p.getPlan().getNodes(), p2.getPlan().getNodes());
        assertEquals(t.finalResult(c).rows().toString(), t2.finalResult(t2.findClass(c.getName()).orElseThrow()).rows().toString());
        try (var pdf = PdfReports.phase(t, p)) {
            assertTrue(pdf.getNumberOfPages() >= 2);
        }
    }

    @Test
    void hauptrundeMitNichtVollemFeldUndRestwertung() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "HR", 15, 4);         // Gruppen 4,4,4,3 → 4. Plätze nur dreimal
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.MAIN_CONSOLATION,
                new Tournament.PlanOptions(List.of(1), List.of(2), false, false, 0), new Random(2));
        PhaseFixtures.playAll(t, p);
        List<Tournament.ResultSection> s = t.finalResultSections(c);
        assertEquals(4 + 3 + 4, s.get(0).rows().size(), "Haupt 4 + nicht im Plan 7");
        assertEquals(4, s.get(1).rows().size());
        assertCovers(t.finalResult(c).rows(), 15);
    }

    // --- #96 Fortgesetztes KO -----------------------------------------------------------------------

    @Test
    void fortgesetztesKoSpieltAllePlaetzeAus() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "FK", 16, 4);
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.CONTINUED, Tournament.PlanOptions.of(List.of(1, 2)), new Random(3));
        assertTrue(p.getPlan().getSections().size() >= 3, "Platz 3–4, 5–8 als eigene Bäume");
        PhaseFixtures.playAll(t, p);
        assertTrue(t.isPhaseComplete(p));
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertDistinctPlaces(rows.subList(0, 8), 8);
        assertCovers(rows, 16);
    }

    @Test
    void fortgesetztesKoMitGrenze() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "FG", 16, 4);
        // 4 Plätze je Gruppe → 16er-Feld; erst ab dem Viertelfinale (8) weiterspielen
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.CONTINUED, new Tournament.PlanOptions(List.of(1, 2, 3, 4), List.of(), false, false, 8), new Random(4));
        PhaseFixtures.playAll(t, p);
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertDistinctPlaces(rows.subList(0, 8), 8);
        assertEquals(9, rows.get(8).from());
        assertEquals(16, rows.get(8).to(), "Verlierer der ersten Runde teilen sich 9.–16.");
    }

    // --- #99 Doppeltes KO ---------------------------------------------------------------------------

    private static PlayingClass koClass(Tournament t, String name, int n) {
        t.setTableCount(12);
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(name + i, "N" + name + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - i * 10);
            p.setClubName("Verein " + name + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        return c;
    }

    @Test
    void doppeltesKoOhneStichkampf() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = koClass(t, "DK", 8);
        t.drawDoubleKo(c, true, new Random(5));
        assertEquals(TournamentMode.PLAN, c.getMode());
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        assertTrue(t.isPhaseComplete(c));
        Tournament.PlanState st = t.planState(c);
        Plan.Node reset = c.getPlan().getNodes().stream().filter(n -> n.cond() != null).findFirst().orElseThrow();
        assertTrue(st.skipped().contains(reset.key()), "Siegerrunden-Sieger gewinnt das Finale → kein Stichkampf");
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertCovers(rows, 8);
        assertEquals(1, rows.get(0).from());
        assertEquals(2, rows.get(1).from());
        assertEquals(3, rows.get(2).from());
        assertEquals(4, rows.get(3).from());
        // jede Person verliert höchstens zweimal, der Sieger nie
        java.util.Map<String, Integer> losses = new java.util.HashMap<>();
        for (Match m : t.matchesOf(c)) losses.merge(m.loser(), 1, Integer::sum);
        assertTrue(losses.values().stream().allMatch(v -> v <= 2), losses.toString());
        assertNull(losses.get(rows.get(0).entrant().id()));
        // Begegnungen aus der Siegerrunde wiederholen sich in der Trostrunde möglichst nicht
        try (var pdf = PdfReports.phase(t, c)) {
            assertTrue(pdf.getNumberOfPages() >= 3);
        }
    }

    @Test
    void doppeltesKoMitStichkampf() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = koClass(t, "DS", 6);                // mit Freilosen
        t.drawDoubleKo(c, true, new Random(6));
        t.startClass(c);
        Plan.Node fin = c.getPlan().getNodes().stream().filter(n -> n.stage().equals("Finale")).findFirst().orElseThrow();
        for (int guard = 0; guard < 200; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .findFirst().orElse(null);
            if (open == null) break;
            if (open.getNumber() == fin.no()) {
                t.recordResult(open, List.of(new int[]{5, 11}, new int[]{5, 11}, new int[]{5, 11}), 0);    // B (Trostrunde) gewinnt
            } else {
                PhaseFixtures.play(t, open);
            }
        }
        Tournament.PlanState st = t.planState(c);
        Plan.Node reset = c.getPlan().getNodes().stream().filter(n -> n.cond() != null).findFirst().orElseThrow();
        assertTrue(st.real().contains(reset.key()), "Stichkampf wird gespielt");
        assertTrue(t.isPhaseComplete(c));
        Match decider = t.planMatch(c, reset);
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertCovers(rows, 6);
        assertEquals(decider.winner(), rows.get(0).entrant().id(), "der Stichkampf entscheidet");
        // Korrektur des Finales: Stichkampf entfällt (nach Bestätigung)
        Match finM = t.planMatch(c, fin);
        assertThrows(Tournament.FollowUpConflict.class, () -> t.correctResult(finM, List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5}), 0));
        t.correctResult(finM, List.of(new int[]{11, 5}, new int[]{11, 5}, new int[]{11, 5}), 0, true);
        assertNull(t.planMatch(c, reset));
        assertTrue(t.isPhaseComplete(c));
        Tournament t2 = roundTrip(t);
        assertTrue(t2.isPhaseComplete(t2.findClass("DS").orElseThrow()));
    }

    @Test
    void doppeltesKoAlsPhase2() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "D2", 16, 4);
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.DOUBLE, Tournament.PlanOptions.of(List.of(1, 2)), new Random(7));
        PhaseFixtures.playAll(t, p);
        assertTrue(t.isPhaseComplete(p));
        assertCovers(t.finalResult(c).rows(), 16);
    }

    // --- #98 / #100 TOP24 und TOP32 -----------------------------------------------------------------

    @Test
    void top24SpieltAllePlaetzeAus() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "T24", 24, 4);
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.TOP24, null, new Random(8));
        assertEquals(44, p.getPlan().getNodes().size());
        PhaseFixtures.playAll(t, p);
        assertTrue(t.isPhaseComplete(p));
        assertDistinctPlaces(t.finalResult(c).rows(), 24);
    }

    @Test
    void top24MitFehlendenPlaetzenUndZusaetzlichenGruppen() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "T22", 26, 5);       // 5 Gruppen: E läuft hinten mit, D/E-Plätze fehlen teils
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.TOP24, null, new Random(9));
        PhaseFixtures.playAll(t, p);
        assertTrue(t.isPhaseComplete(p));
        assertCovers(t.finalResult(c).rows(), 26);
    }

    @Test
    void top32SpieltAllePlaetzeAus() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "T32", 32, 4);
        PlayingClass p = t.createPlanPhase(c, Plan.Kind.TOP32, null, new Random(10));
        assertEquals(60, p.getPlan().getNodes().size());
        PhaseFixtures.playAll(t, p);
        assertDistinctPlaces(t.finalResult(c).rows(), 32);
        assertEquals(List.of(), t.planKindsAfter(p), "nach einem Plan keine weitere Phase");
    }

    // --- #97 Leistungsstufen und Phase 3 ------------------------------------------------------------

    @Test
    void leistungsstufenUndEndrundeJeGruppenplatz() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "LS", 16, 4);
        assertTrue(t.canChooseTiers(c));
        PlayingClass p2 = t.createTieredPhase(c, 2, 4, true, new Random(11));
        assertEquals(4, p2.getGroups().size());
        assertEquals(List.of(2, 2), p2.getTierSizes());
        assertEquals("E", t.groupLabel(p2, 0), "Gruppennamen laufen weiter");
        assertTrue(t.matchesOf(p2).stream().noneMatch(Match::isCarried), "keine Ergebnisse übernommen");
        // gespiegelt: A und D bilden die erste Gruppe der ersten Stufe
        Set<String> first = new HashSet<>(p2.getGroups().get(0));
        List<GroupStanding.Row> a = t.standings(c, 0), d = t.standings(c, 3);
        assertTrue(first.contains(a.get(0).entrant().id()) && first.contains(d.get(1).entrant().id()), p2.getGroups().toString());
        assertFalse(t.canChooseTiers(p2));
        assertEquals(List.of(Plan.Kind.FINAL_BY_PLACE), t.planKindsAfter(p2));
        PhaseFixtures.playAll(t, p2);
        assertCovers(t.finalResult(c).rows(), 16);
        PlayingClass p3 = t.createPlanPhase(p2, Plan.Kind.FINAL_BY_PLACE, null, new Random(12));
        assertEquals(3, t.phaseNumber(p3));
        PhaseFixtures.playAll(t, p3);
        assertTrue(t.isPhaseComplete(p3));
        assertDistinctPlaces(t.finalResult(c).rows(), 16);
        Tournament t2 = roundTrip(t);
        PlayingClass p2b = t2.findClass(p2.getName()).orElseThrow();
        assertEquals(List.of(2, 2), p2b.getTierSizes());
        assertEquals(4, p2b.getGroupNameOffset());
        assertDistinctPlaces(t2.finalResult(t2.findClass("LS").orElseThrow()).rows(), 16);
    }

    @Test
    void leistungsstufenAusgelostOhnePhase3() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "LA", 18, 3);         // 3 Gruppen à 6
        PlayingClass p2 = t.createTieredPhase(c, 3, 3, false, new Random(13));
        assertEquals(2, p2.getTierSizes().size());
        PhaseFixtures.playAll(t, p2);
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertCovers(rows, 18);
        // Stufe 1 (Plätze 1–3 der Vorrunde) steht komplett vor Stufe 2
        Set<String> tier1 = new HashSet<>();
        for (int g = 0; g < p2.getTierSizes().get(0); g++) tier1.addAll(p2.getGroups().get(g));
        for (int i = 0; i < 9; i++) assertTrue(tier1.contains(rows.get(i).entrant().id()), rows.toString());
    }

    @Test
    void phase3NurNachLeistungsstufen() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = played(t, "P3", 16, 4);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.GROUPS, 2, 0, new Random(14));
        assertFalse(t.canHaveNextPhase(ko), "normale Gruppenphase 2: keine Phase 3");
        assertEquals(List.of(), t.planKindsAfter(ko));
    }

    // --- #95 Badeni ---------------------------------------------------------------------------------

    @Test
    void badeniFreilosUndKeineWiederholungen() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = koClass(t, "BA", 9);
        t.drawBadeni(c, new Random(15));
        assertTrue(c.isBadeni());
        assertEquals(TournamentMode.SWISS, c.getMode());
        assertEquals(5, t.swissRounds(c));
        // Runde 1: TTR-Reihung, Freilos für den Besten der unteren Hälfte (Platz 5), 1 gegen 9 usw.
        String byeId = c.getSwissByes().get(0);
        Entrant bye = t.entrantIndex(c).get(byeId);
        assertEquals(2000 - 5 * 10, bye.ttr());
        Match r1 = t.matchesOf(c).stream().min(java.util.Comparator.comparingInt(Match::getNumber)).orElseThrow();
        assertEquals(1990, t.entrant(r1, r1.getA()).ttr());
        assertEquals(1910, t.entrant(r1, r1.getB()).ttr());
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        assertEquals(5, t.swissRound(c));
        Set<String> pairs = new HashSet<>();
        for (Match m : t.matchesOf(c)) assertTrue(pairs.add(SwissSystem.key(m.getA(), m.getB())), "Wiederholung " + m);
        assertEquals(5, new HashSet<>(c.getSwissByes()).size(), "niemand zweimal Freilos");
        List<GroupStanding.Row> table = t.standings(c, 0);
        for (int i = 1; i < table.size(); i++) assertTrue(table.get(i - 1).score() >= table.get(i).score());
        assertCovers(t.finalResult(c).rows(), 9);
        assertFalse(t.canHaveNextPhase(c), "keine Phase 2 nach Badeni");
        // weitere Runde und Pausieren
        String paused = table.get(8).entrant().id();
        t.setSwissPaused(c, List.of(paused));
        t.addSwissRound(c);
        assertEquals(6, t.swissRound(c));
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getRound() == 6).noneMatch(m -> paused.equals(m.getA()) || paused.equals(m.getB())));
        assertEquals(4, t.matchesOf(c).stream().filter(m -> m.getRound() == 6).count(), "8 Spielende, kein Freilos");
        Tournament t2 = roundTrip(t);
        PlayingClass c2 = t2.findClass("BA").orElseThrow();
        assertTrue(c2.isBadeni());
        assertEquals(Set.of(paused), c2.getSwissPaused());
    }

    @Test
    void badeniPaarungErsterGegenLetztenInDerSiegegruppe() {
        List<String> ids = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        java.util.Map<String, Integer> wins = new java.util.HashMap<>();
        for (String s : List.of("a", "b", "c", "d")) wins.put(s, 1);
        SwissSystem.Round r = BadeniSystem.nextRound(ids, wins, Set.of(), Set.of());
        assertNull(r.bye());
        assertEquals("a-d b-c e-h f-g", r.pairs().stream().map(p -> p[0] + "-" + p[1]).reduce((x, y) -> x + " " + y).orElse(""));
        // ungerade Siegegruppe: der Schwächste rutscht eine Gruppe tiefer
        wins.put("e", 1);
        r = BadeniSystem.nextRound(ids, wins, Set.of(), Set.of());
        assertEquals("a-d b-c e-h f-g", r.pairs().stream().map(p -> p[0] + "-" + p[1]).reduce((x, y) -> x + " " + y).orElse(""));
        // schon gespielt a-d → a gegen den nächstmöglichen
        r = BadeniSystem.nextRound(ids, java.util.Map.of(), Set.of(SwissSystem.key("a", "h")), Set.of());
        assertEquals("a-g", r.pairs().get(0)[0] + "-" + r.pairs().get(0)[1]);
    }
}
