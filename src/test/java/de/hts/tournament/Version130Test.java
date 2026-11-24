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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.3.0 – Gold/Silber/Bronze und Aufsteiger-Chips (#104), Zusatzspiel in der Qualifikation (#105), Gruppenphase mit
 * Freistellung (#106) und Korrektur-Reiter (#107).
 */
class Version130Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static PlayingClass singles(Tournament t, String name, int n) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(name + i, "N" + name + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - 10 * i);          // eindeutige TTR: Nr. 1 ist die Stärkste
            p.setClubName("Verein " + name + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        return c;
    }

    // --- #104 -----------------------------------------------------------------------------------------

    @Test
    void aufsteigerChipsErstMitPhaseZwei() {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = PhaseFixtures.groupsClass(t, "M", 16, 4);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        assertTrue(t.advanceMarks(c).isEmpty(), "ohne Phase 2 niemand markiert");
        t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        Map<String, Integer> marks = t.advanceMarks(c);
        assertEquals(8, marks.size());
        assertTrue(marks.values().stream().allMatch(v -> v == 1));
    }

    // --- #105 -----------------------------------------------------------------------------------------

    @Test
    void zusatzspielGewinnerQualifiziertOderNachruecker() {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = PhaseFixtures.groupsClass(t, "Q", 24, 4);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 3, 2, 0, new Random(1));
        t.resumeClass(q);
        for (Match m : List.copyOf(t.matchesOf(q))) if (m.getStatus() != MatchStatus.DONE) PhaseFixtures.play(t, m);
        t.decideQualification(q, false);
        t.resumeClass(q);
        PhaseFixtures.playAll(t, q);
        assertTrue(t.canAddQualificationExtra(q));
        int before = t.qualified(q).size();
        t.addQualificationExtra(q);
        PhaseFixtures.playAll(t, q);
        assertTrue(t.qualificationExtraDecisionPending(q));
        assertFalse(t.isPhaseComplete(q), "erst nach der Entscheidung abgeschlossen");
        t.decideQualificationExtra(q, false);
        assertEquals(before, t.qualified(q).size(), "Nachrücker: nicht qualifiziert");
        List<List<Entrant>> tiers = t.qualificationTiers(q);
        assertFalse(tiers.isEmpty());
        t.decideQualificationExtra(q, true);
        assertTrue(t.qualified(q).size() > before, "qualifiziert: mehr Qualifizierte");
        assertTrue(t.isPhaseComplete(q));
    }

    // --- #106 -----------------------------------------------------------------------------------------

    @Test
    void freistellungPruefungUndVorschlag() {
        assertNull(Tournament.exemptProblem(24, 6, 2));            // 4 + 6 × 2 = 16
        assertNotNull(Tournament.exemptProblem(24, 5, 2));         // 14
        assertTrue(Tournament.exemptProblem(24, 5, 2).contains("6 Gruppen"));
        assertNull(Tournament.exemptProblem(16, 4, 3));            // 4 + 12 = 16
        assertNotNull(Tournament.exemptProblem(12, 4, 3), "Gruppen à 3 mit Plätzen 1–3: alle kämen weiter");
        Tournament t = new Tournament();
        PlayingClass c = singles(t, "F", 28);
        List<String> top = t.exemptSuggestion(c, new Random(1));
        assertEquals(List.of("F1", "F2", "F3", "F4").size(), top.size());
        assertEquals(Set.of(id(t, "F1"), id(t, "F2"), id(t, "F3"), id(t, "F4")), new HashSet<>(top));
        c.setGroupsBySize(false);
        c.setGroupCount(5);
        assertThrows(IllegalArgumentException.class, () -> t.drawGroupsWithExemption(c, top, 2, true, new Random(2)));
        assertFalse(c.isDrawn());
    }

    private static String id(Tournament t, String last) {
        return t.getPlayers().stream().filter(p -> p.getFirstname().equals(last)).findFirst().orElseThrow().getId();
    }

    @Test
    void freistellungSechsGruppenAchtelfinaleOhneFreilos() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = singles(t, "J", 28);
        List<String> ex = t.exemptSuggestion(c, new Random(1));
        c.setGroupsBySize(false);
        c.setGroupCount(6);
        t.drawGroupsWithExemption(c, ex, 2, true, new Random(2));
        assertEquals(6, c.getGroups().size());
        assertEquals(24, c.getGroups().stream().mapToInt(List::size).sum());
        assertTrue(c.getGroups().stream().flatMap(List::stream).noneMatch(ex::contains), "Freigestellte spielen keine Gruppenspiele");
        assertFalse(t.isDrawOutdated(c));
        assertEquals(16, t.exemptFieldSize(c));
        c = roundTrip(t).findClass("J").orElseThrow();
        assertEquals(ex, c.getExempt());
        assertEquals(2, c.getExemptPlaces());
        t.startClass(t.findClass("J").orElseThrow());
        c = t.findClass("J").orElseThrow();
        PhaseFixtures.playAll(t, c);
        assertThrows(IllegalStateException.class, () -> t.createNextPhase(t.findClass("J").orElseThrow(), TournamentMode.GROUPS, 2, 4, new Random(3)));
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(3));
        assertEquals(16, ko.getDraw().size());
        assertTrue(ko.getDraw().stream().allMatch(java.util.Objects::nonNull), "keine Freilose");
        Map<String, String> labels = ko.getSeedLabels();
        // 1.7.0 (#145): S1/S4 oben, S2/S3 unten (Platz in der Hälfte ausgelost), jeweils gegen einen Gruppenzweiten
        assertTrue(java.util.Set.of("S1", "S4").contains(labels.get(ko.getDraw().get(0))), labels.get(ko.getDraw().get(0)));
        assertTrue(labels.get(ko.getDraw().get(1)).endsWith("2"), "S gegen einen Gruppenzweiten: " + labels.get(ko.getDraw().get(1)));
        assertTrue(java.util.Set.of("S2", "S3").contains(labels.get(ko.getDraw().get(15))), labels.get(ko.getDraw().get(15)));
        for (int p = 0; p < 8; p++) {
            String a = labels.get(ko.getDraw().get(2 * p)), b = labels.get(ko.getDraw().get(2 * p + 1));
            assertFalse(a.charAt(0) != 'S' && a.charAt(0) == b.charAt(0), "gleiche Gruppe in Runde 1: " + a + "–" + b);
        }
        // Endergebnis deckt alle 28 ab
        PhaseFixtures.playAll(t, ko);
        List<Tournament.Placing> rows = t.finalResult(c).rows();
        assertEquals(28, rows.size());
        assertEquals(1, rows.get(0).from());
    }

    @Test
    void freistellungVierGruppenDreiFestesSchema() {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = singles(t, "V", 20);
        c.setGroupsBySize(false);
        c.setGroupCount(4);
        t.drawGroupsWithExemption(c, t.exemptSuggestion(c, new Random(1)), 3, true, new Random(2));
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 3, 4, new Random(3));
        List<String> order = ko.getDraw().stream().map(id -> ko.getSeedLabels().get(id)).toList();
        assertEquals(List.of("S1", "D3", "B1", "D2", "A1", "C2", "S4", "C3", "S3", "A3", "C1", "B2", "D1", "A2", "S2", "B3"), order);
    }

    @Test
    void freistellungTauschMitGruppenmitglied() {
        Tournament t = new Tournament();
        PlayingClass c = singles(t, "T", 20);
        c.setGroupsBySize(false);
        c.setGroupCount(4);
        List<String> ex = t.exemptSuggestion(c, new Random(1));
        t.drawGroupsWithExemption(c, ex, 3, true, new Random(2));
        String member = c.getGroups().get(0).get(0);
        t.swapGroupMembers(c, ex.get(0), member);
        assertEquals(member, c.getExempt().get(0));
        assertTrue(c.getGroups().get(0).contains(ex.get(0)));
        assertFalse(t.isDrawOutdated(c));
    }

    // --- #107 -----------------------------------------------------------------------------------------

    /** Gruppenspiel zweier Gruppenmitglieder finden. */
    private static String pair(Match m) {
        return m.getA().compareTo(m.getB()) < 0 ? m.getA() + "~" + m.getB() : m.getB() + "~" + m.getA();
    }

    private static Match groupMatch(Tournament t, PlayingClass c, String a, String b) {
        return t.matchesOf(c).stream().filter(m -> (a.equals(m.getA()) && b.equals(m.getB())) || (a.equals(m.getB()) && b.equals(m.getA())))
                .findFirst().orElseThrow();
    }

    @Test
    void korrekturReiterUebernehmenUndWeiterfuehren() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass c = PhaseFixtures.groupsClass(t, "K", 16, 4);
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        PlayingClass ko = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(5));
        assertNotNull(ko.getRecipe());
        // ohne Auswirkung: dasselbe Ergebnis – kein Korrektur-Reiter
        List<GroupStanding.Row> table = t.standings(c, 0);
        String first = table.get(0).entrant().id(), third = table.get(2).entrant().id(), second = table.get(1).entrant().id();
        Match m12 = groupMatch(t, c, first, second);
        t.correctResult(m12, new ArrayList<>(m12.getPoints()), 0);       // gleiches Ergebnis erneut eingetragen
        assertNull(t.correctionOf(ko), "keine Auswirkung, kein Reiter");
        // der Dritte schlägt jetzt den Zweiten und den Ersten deutlich -> Plätze ändern sich
        Match m23 = groupMatch(t, c, second, third);
        Match m13 = groupMatch(t, c, first, third);
        List<int[]> thirdWins = List.of(new int[]{11, 0}, new int[]{11, 0}, new int[]{11, 0});
        List<int[]> flip = List.of(new int[]{0, 11}, new int[]{0, 11}, new int[]{0, 11});
        t.correctResult(m23, third.equals(m23.getA()) ? thirdWins : flip, 0);
        t.correctResult(m13, third.equals(m13.getA()) ? thirdWins : flip, 0);
        PlayingClass corr = t.correctionOf(ko);
        assertNotNull(corr, "Korrektur-Reiter entsteht");
        assertEquals(ko.getName(), corr.getCorrectionOf());
        assertFalse(corr.isStarted());
        assertTrue(t.matchesOf(corr).stream().noneMatch(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus().atTable()), "nichts eingereiht");
        assertFalse(t.newEntrants(corr).isEmpty() && t.newPairings(corr).isEmpty());
        assertEquals(2, t.phasesOf(c).size(), "Korrektur gehört nicht zur Phasenfolge");
        // Speichern/Laden behält den Korrektur-Reiter
        Tournament t2 = roundTrip(t);
        assertNotNull(t2.correctionOf(t2.findClass(ko.getName()).orElseThrow()));
        // in der laufenden Phase die Spiele spielen, deren Paarung es auch in der Korrektur gibt
        Set<String> corrPairs = new HashSet<>();
        for (Match m : t.matchesOf(corr)) if (m.getA() != null && m.getB() != null) corrPairs.add(pair(m));
        int common = 0;
        for (Match m : List.copyOf(t.matchesOf(ko))) {
            if (m.getA() != null && m.getB() != null && corrPairs.contains(pair(m)) && m.getStatus() != MatchStatus.DONE) {
                PhaseFixtures.play(t, m);
                common++;
            }
        }
        assertTrue(common >= 1, "mindestens eine unveränderte Paarung");
        assertEquals(common, t.takeOverCount(corr));
        int taken = t.takeOverUnchanged(corr);
        assertTrue(taken >= common, "K2");
        assertTrue(t.matchesOf(corr).stream().noneMatch(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus().atTable()), "weiter nichts eingereiht");
        // verwerfen und neu entstehen lassen durch erneute Korrektur
        t.discardCorrection(corr);
        assertNull(t.correctionOf(ko));
        t.correctResult(m13, third.equals(m13.getA()) ? List.of(new int[]{11, 1}, new int[]{11, 1}, new int[]{11, 1})
                : List.of(new int[]{1, 11}, new int[]{1, 11}, new int[]{1, 11}), 0);
        corr = t.correctionOf(ko);
        assertNotNull(corr);
        assertEquals(taken, t.takeOverUnchanged(corr), "erneut übernehmbar");
        Tournament.AdoptInfo info = t.adoptInfo(corr);
        assertEquals(taken, info.taken());
        t.adoptCorrection(corr);
        assertTrue(ko.isDeactivated(), "K4");
        assertEquals(corr.getName(), c.getFollowUpClass());
        assertNull(corr.getCorrectionOf());
        assertTrue(corr.isStarted(), "K5");
        assertEquals(List.of(c, corr), t.phasesOf(c));
        assertEquals(List.of(ko), t.deactivatedAfter(c));
        assertTrue(t.matchesOf(ko).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE || m.getStatus() == MatchStatus.CANCELLED), "K6");
        assertTrue(t.isInactiveMatch(t.matchesOf(ko).get(0)), "K7");
        PhaseFixtures.playAll(t, corr);
        assertEquals(16, t.finalResult(c).rows().size());
        // beim Löschen der Klasse verschwinden auch deaktivierte Phasen
        t.removeClass(c);
        assertTrue(t.getClasses().isEmpty(), t.getClasses().toString());
    }
}
