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

class PhasesTest {

    private static List<Player> players(Tournament t, String prefix, int n, int ttrBase) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(prefix + i, "N" + prefix + i);
            p.setInternalNr(prefix + "-" + i);
            p.setSex(Sex.FEMALE);
            p.setTtr(ttrBase - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        return ps;
    }

    private static PlayingClass groupsClass(Tournament t, String name, List<Player> ps, int groupCount) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(groupCount);
        t.drawGroups(c, true, new Random(3));
        return c;
    }

    /** Höherer TTR gewinnt immer 3:0. */
    private static void playAll(Tournament t, PlayingClass c) {
        // 0.38: Qualifikationsphase startet erst per „Phase starten“; nach Runde 1 wird entschieden (hier: weitere Runde)
        if (c.isAwaitingStart()) t.resumeClass(c);
        if (t.qualificationDecisionPending(c)) t.decideQualification(c, false);
        if (c.isAwaitingStart()) t.resumeClass(c);
        for (int guard = 0; guard < 500; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .findFirst().orElse(null);
            if (open == null) {
                if (t.qualificationDecisionPending(c)) {
                    t.decideQualification(c, false);
                    if (c.isAwaitingStart()) t.resumeClass(c);       // 0.41: Runde 2 per „Qualifikation Runde 2 starten“
                    continue;
                }
                return;
            }
            Entrant a = t.entrant(open, open.getA()), b = t.entrant(open, open.getB());
            boolean aWins = a.ttr() > b.ttr();
            t.recordResult(open, aWins ? List.of(new int[]{11, 5}, new int[]{11, 6}, new int[]{11, 7})
                    : List.of(new int[]{5, 11}, new int[]{6, 11}, new int[]{7, 11}), 0);
        }
    }

    @Test
    void tischeWerdenKlassenuebergreifendVollBelegt() {
        Tournament t = new Tournament();
        t.setTableCount(12);
        PlayingClass maedchen = groupsClass(t, "Mädchen U15", players(t, "M", 6, 1500), 1);   // max. 3 Spiele gleichzeitig
        PlayingClass jungen = groupsClass(t, "Jungen U19", players(t, "J", 30, 1800), 5);
        t.startClass(maedchen);
        t.startClass(jungen);
        assertTrue(t.freeTables().isEmpty(), "alle 12 Tische belegt");
        long m = t.getMatches().stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE && x.getClassName().equals("Mädchen U15")).count();
        assertEquals(3, m, "Mädchen: so viele, wie Personen frei sind – den Rest bekommen die Jungen");
    }

    @Test
    void phaseAnhaltenUndFortsetzen() {
        Tournament t = new Tournament();
        t.setTableCount(2);
        PlayingClass a = groupsClass(t, "A", players(t, "A", 8, 1500), 2);
        PlayingClass b = groupsClass(t, "B", players(t, "B", 8, 1500), 2);
        t.startClass(a);
        t.startClass(b);
        List<String> queuedA = t.queue().stream().filter(m -> m.getClassName().equals("A")).map(Match::getId).toList();
        assertFalse(queuedA.isEmpty());
        t.pauseClass(a);
        assertTrue(a.isPaused());
        assertTrue(t.queue().stream().noneMatch(m -> m.getClassName().equals("A")), "angehaltene Phase belegt keine Tische");
        // laufende Spiele von A dürfen noch beendet werden – danach rückt B nach
        Match activeA = t.getMatches().stream().filter(m -> m.getClassName().equals("A") && m.getStatus() == MatchStatus.ACTIVE)
                .findFirst().orElse(null);
        if (activeA != null) {
            t.recordResult(activeA, 3, 0);
            assertEquals("B", t.activeMatchAt(1).or(() -> t.activeMatchAt(2)).orElseThrow().getClassName());
        }
        t.resumeClass(a);
        assertFalse(a.isPaused());
        assertTrue(t.queue().stream().anyMatch(m -> m.getClassName().equals("A")));
        assertEquals(queuedA.get(queuedA.size() - 1), t.queue().stream().filter(m -> m.getClassName().equals("A"))
                .reduce((x, y) -> y).orElseThrow().getId(), "Reihenfolge bleibt erhalten");
    }

    @Test
    void gruppenphaseMitPlatzierungsspielenUngleicheGruppen() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        List<Player> ps = players(t, "S", 9, 2000);
        PlayingClass c = groupsClass(t, "Mädchen", ps, 2);      // 5 + 4
        assertTrue(t.canChoosePlacement(c));
        c.setNextPhase(TournamentMode.PLACEMENT);
        t.startClass(c);
        playAll(t, c);
        assertTrue(t.isGroupPhaseComplete(c));
        assertFalse(c.isFinished(), "nach der Gruppenphase entscheidet die Turnierleitung");

        PlayingClass p = t.createNextPhase(c, new Random(1));
        assertEquals("Mädchen – Phase 2", p.getName());
        assertEquals(TournamentMode.PLACEMENT, p.getMode());
        assertEquals(2, t.phaseNumber(p));
        assertTrue(p.isStarted(), "Folgephase startet sofort");
        assertEquals(List.of(p), t.rootClasses().stream().filter(x -> x == p).toList().isEmpty() ? List.of(p) : List.of(),
                "Phase 2 erscheint nicht als eigene Klasse");
        List<Match> ms = t.matchesOf(p);
        assertEquals(2, ms.stream().filter(Match::isCarried).count(), "Erster gegen Zweiter je Gruppe wird übernommen");
        assertEquals(4, ms.stream().filter(m -> m.getGroupIndex() == 0 && !m.isCarried()).count(), "nur noch 4 Spiele");
        assertEquals(List.of(5, 7), ms.stream().filter(m -> m.getPlacement() > 0).map(Match::getPlacement).toList());
        assertTrue(ms.stream().filter(m -> !m.isCarried()).allMatch(m -> m.getStatus() != MatchStatus.PLANNED));

        Tournament.FinalResult interim = t.finalResult(c);
        assertFalse(interim.complete());
        assertEquals(9, interim.rows().size());

        playAll(t, p);
        assertFalse(c.isFinished(), "die Turnierleitung schließt die Phase ab");
        t.finishClass(p);
        assertTrue(c.isFinished());
        Tournament.FinalResult fin = t.finalResult(c);
        assertTrue(fin.complete());
        List<Integer> places = fin.rows().stream().map(Tournament.Placing::from).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), places);
        // Höherer TTR gewinnt immer -> Endergebnis nach TTR
        for (int i = 0; i < 9; i++) assertEquals(ps.get(i).getId(), fin.rows().get(i).entrant().id(), "Platz " + (i + 1));
        assertEquals("ohne Gegner (kampflos)", fin.rows().get(8).info());

        // speichern und laden
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass c2 = back.findClass("Mädchen").orElseThrow();
        assertTrue(c2.isFinished());
        assertEquals(2, back.phasesOf(c2).size());
        assertEquals(places, back.finalResult(c2).rows().stream().map(Tournament.Placing::from).toList());
    }

    @Test
    void koEndergebnisNachGruppenUndHoechstensZweiPhasen() {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Jungen", players(t, "J", 16, 2000), 4);
        t.startClass(c);
        playAll(t, c);
        PlayingClass p2 = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        assertEquals(2, t.phaseNumber(p2));
        assertFalse(t.canHaveNextPhase(p2), "es gibt keine Phase 3");
        Tournament.FinalResult mid = t.finalResult(c);
        assertEquals(16, mid.rows().size());
        assertTrue(mid.rows().stream().limit(8).allMatch(r -> r.info().equals("noch im Wettbewerb")));
        playAll(t, p2);
        assertThrows(IllegalStateException.class, () -> t.createNextPhase(p2, TournamentMode.GROUPS, 2, 4, new Random(1)));
        t.finishClass(p2);
        assertTrue(c.isFinished());
        List<String> places = t.finalResult(c).rows().stream().map(Tournament.Placing::placeText).toList();
        assertEquals(List.of("1.", "2.", "3.", "3.", "5.–8.", "5.–8.", "5.–8.", "5.–8.",
                "9.–12.", "9.–12.", "9.–12.", "9.–12.", "13.–16.", "13.–16.", "13.–16.", "13.–16."), places);
    }

    @Test
    void gruppenphaseBeendenOhneFolgephase() {
        Tournament t = new Tournament();
        t.setTableCount(5);
        PlayingClass c = groupsClass(t, "Damen", players(t, "D", 5, 1500), 1);
        t.startClass(c);
        assertThrows(IllegalStateException.class, () -> t.finishClass(c));
        playAll(t, c);
        t.finishClass(c);
        assertTrue(c.isFinished());
        assertEquals(List.of(1, 2, 3, 4, 5), t.finalResult(c).rows().stream().map(Tournament.Placing::from).toList());
        assertFalse(t.canCreateNextPhase(c));
    }

    @Test
    void doppelEineGruppeUndKoPhase() {
        Tournament t = new Tournament();
        t.setTableCount(8);
        List<Player> ps = players(t, "P", 12, 2000);
        PlayingClass big = t.addClass(new PlayingClass("Doppel groß", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        for (int i = 0; i < 12; i += 2) t.createPair(big, ps.get(i), ps.get(i + 1));
        assertThrows(IllegalStateException.class, () -> t.drawGroups(big, true, new Random(2)), "mehr als 5 Doppel: nur KO-System");
        assertThrows(IllegalStateException.class, () -> t.drawSwiss(big, true, new Random(2)), "kein Schweizer System für Doppel");

        List<Player> qs = players(t, "Q", 10, 1900);
        PlayingClass c = t.addClass(new PlayingClass("Doppel", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        for (int i = 0; i < 10; i += 2) t.createPair(c, qs.get(i), qs.get(i + 1));
        c.setGroupsBySize(false);
        c.setGroupCount(2);
        t.drawGroups(c, true, new Random(2));
        assertEquals(1, c.getGroups().size(), "bis 5 Doppel: genau eine Gruppe, jeder gegen jeden");
        assertEquals(10, t.matchesOf(c).size());
        t.startClass(c);
        playAll(t, c);
        PlayingClass p = t.createNextPhase(c, TournamentMode.KNOCKOUT, 4, 4, new Random(1));
        assertTrue(qs.stream().noneMatch(x -> x.isInClass(p.getName())), "keine Meldung in der Phase");
        playAll(t, p);
        t.finishClass(p);
        assertTrue(c.isFinished());
        assertEquals(List.of(1, 2, 3, 3, 5), t.finalResult(c).rows().stream().map(Tournament.Placing::from).toList());
    }

    @Test
    void tischeNurFuerBestimmteKlassen() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass m = groupsClass(t, "Mädchen", players(t, "M", 4, 1500), 1);
        PlayingClass j = groupsClass(t, "Jungen", players(t, "J", 8, 1800), 2);
        t.setTableExcluded(1, List.of("Mädchen"));          // 0.38: Haken bei Mädchen entfernt
        t.setTableExcluded(2, List.of("Mädchen"));
        t.setTableExcluded(3, List.of("Mädchen"));
        // Mädchen zuerst gestartet -> stehen vorn in der Warteschlange, dürfen aber nur an Tisch 4
        t.startClass(m);
        t.startClass(j);
        assertEquals("Mädchen", t.activeMatchAt(4).orElseThrow().getClassName());
        for (int tb = 1; tb <= 3; tb++) assertEquals("Jungen", t.activeMatchAt(tb).orElseThrow().getClassName(), "Tisch " + tb);
        assertTrue(t.tableAccepts(4, t.activeMatchAt(1).orElseThrow()), "ohne Einschränkung: alle Klassen");
    }

    @Test
    void spielAnAnderenTischVerschieben() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(3);
        PlayingClass c = groupsClass(t, "A", players(t, "A", 4, 1500), 1);
        t.startClass(c);
        Match one = t.activeMatchAt(1).orElseThrow(), two = t.activeMatchAt(2).orElseThrow();
        t.moveMatch(one, 3);
        assertEquals(3, one.getTable());
        assertTrue(t.activeMatchAt(1).isEmpty());
        t.moveMatch(two, 3);
        assertEquals(3, two.getTable(), "Tausch");
        assertEquals(2, one.getTable());
        t.setTableExcluded(2, List.of("A"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertEquals(java.util.Set.of("A"), back.getTableExcluded(2));
        assertTrue(back.getTableExcluded(1).isEmpty());
        assertEquals(1, back.getClasses().size(), "keine leeren Kopien (Bug #59)");
    }

    @Test
    void phaseAbschliessenMitNeuerPhase() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Damen", players(t, "D", 12, 1500), 3);
        t.startClass(c);
        playAll(t, c);
        assertEquals(TournamentMode.NONE, c.getNextPhase(), "in Phase 1 wird „danach“ nicht mehr vorab gewählt");
        PlayingClass p2 = t.createNextPhase(c, TournamentMode.GROUPS, 2, 3, new Random(1));
        assertEquals(TournamentMode.GROUPS, p2.getMode());
        assertEquals(2, p2.getGroups().size(), "6 Aufsteiger in Gruppen à 3");
        assertThrows(IllegalStateException.class, () -> t.createNextPhase(p2, TournamentMode.PLACEMENT, 2, 4, new Random(1)),
                "erst wenn alle Spiele gespielt sind");
        playAll(t, p2);
        assertThrows(IllegalStateException.class, () -> t.createNextPhase(p2, TournamentMode.PLACEMENT, 0, 4, new Random(1)),
                "höchstens 2 Phasen");
        t.finishClass(p2);
        assertEquals(12, t.finalResult(c).rows().size());
        assertEquals(List.of(1, 1, 3, 3, 5, 5, 7, 7, 7, 10, 10, 10), t.finalResult(c).rows().stream().map(Tournament.Placing::from).toList());
    }

    @Test
    void koKorrekturMitFolgespielenUndTauschWaehrendDerPhase() {
        Tournament t = new Tournament();
        t.setTableCount(1);
        PlayingClass c = t.addClass(new PlayingClass("KO"));
        List<Player> ps = players(t, "K", 8, 2000);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        // nur ein Tisch: Spiel 1 läuft, die anderen sind eingereiht -> Paarungen 2 und 3 lassen sich noch tauschen
        Match m1 = t.activeMatchAt(1).orElseThrow();
        assertThrows(IllegalStateException.class, () -> t.swapKnockoutPairings(c, 0, 1), "laufendes Spiel");
        String before = c.getDraw().get(2);
        t.swapKnockoutSlots(c, 2, 4);
        assertEquals(before, c.getDraw().get(4));
        Match second = t.matchesOf(c).stream().filter(m -> m.getKoRound() == 0 && m.getKoIndex() == 2).findFirst().orElseThrow();
        assertEquals(before, second.getA(), "Spiel bekommt den neuen Teilnehmenden");
        // alles durchspielen (Höherer TTR gewinnt)
        playAll(t, c);
        Match fin = t.matchesOf(c).stream().filter(m -> m.getKoRound() == 2).findFirst().orElseThrow();
        assertEquals(MatchStatus.DONE, fin.getStatus());
        t.finishClass(c);
        // Korrektur Viertelfinale 1: Sieger wechselt -> Halbfinale und Finale gespielt -> Konflikt
        String oldWinner = m1.winner();
        List<int[]> flip = List.of(new int[]{0, 11}, new int[]{0, 11}, new int[]{0, 11});
        if (oldWinner.equals(m1.getB())) flip = List.of(new int[]{11, 0}, new int[]{11, 0}, new int[]{11, 0});
        List<int[]> sets = flip;
        Tournament.FollowUpConflict conflict = assertThrows(Tournament.FollowUpConflict.class, () -> t.correctResult(m1, sets, 0));
        assertEquals(2, conflict.getAffected().size());
        t.correctResult(m1, sets, 0, true);
        assertNotEquals(oldWinner, m1.winner());
        assertFalse(c.isFinished(), "muss neu abgeschlossen werden");
        Match semi = t.matchesOf(c).stream().filter(m -> m.getKoRound() == 1 && m.getKoIndex() == 0).findFirst().orElseThrow();
        assertTrue(semi.getStatus() == MatchStatus.ACTIVE || semi.getStatus() == MatchStatus.QUEUED, "neu eingereiht");
        assertTrue(m1.winner().equals(semi.getA()) || m1.winner().equals(semi.getB()));
    }

    @Test
    void phaseZweiAngehaltenNeuAuslosen() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Jugend", players(t, "Y", 12, 1800), 3);
        t.startClass(c);
        playAll(t, c);
        PlayingClass p2 = t.createNextPhase(c, TournamentMode.KNOCKOUT, 2, 4, new Random(1));
        assertThrows(IllegalStateException.class, () -> t.redrawPhase(p2, new Random(2)), "erst anhalten");
        // laufende Spiele eintragen, dann anhalten
        t.pauseClass(p2);
        for (Match m : t.matchesOf(p2)) if (m.getStatus() == MatchStatus.ACTIVE) t.recordResult(m, 3, 0);
        PlayingClass again = t.redrawPhase(p2, new Random(2));
        assertEquals("Jugend – Phase 2", again.getName());
        assertTrue(again.isPaused());
        assertTrue(t.matchesOf(again).stream().noneMatch(m -> m.getStatus() == MatchStatus.DONE || m.getStatus() == MatchStatus.ACTIVE));
        assertEquals(again.getName(), c.getFollowUpClass());
        t.resumeClass(again);
        assertTrue(t.matchesOf(again).stream().anyMatch(m -> m.getStatus() == MatchStatus.ACTIVE));
        assertTrue(t.clubClashes(again).isEmpty(), "ab Phase 2 keine Vereinsprüfung");
    }

    @Test
    void pausierenZurueckziehenUndUmsortieren() {
        Tournament t = new Tournament();
        t.setTableCount(2);
        PlayingClass c = t.addClass(new PlayingClass("KO8"));
        List<Player> ps = players(t, "Q", 8, 2000);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        Match atOne = t.activeMatchAt(1).orElseThrow();
        // zurückziehen -> hinten in der Warteschlange, Tisch 1 bekommt das nächste Spiel
        t.withdrawMatch(atOne);
        assertEquals(MatchStatus.QUEUED, atOne.getStatus());
        assertEquals(atOne, t.queue().get(t.queue().size() - 1));
        assertNotEquals(atOne, t.activeMatchAt(1).orElseThrow());
        // anhalten: Spiele an den Tischen werden pausiert und bleiben dort, Warteschlange leer
        t.pauseClass(c);
        Match paused = t.activeMatchAt(1).orElseThrow();
        assertEquals(MatchStatus.PAUSED, paused.getStatus());
        assertTrue(t.queue().isEmpty());
        assertTrue(t.freeTables().isEmpty(), "pausierte Spiele belegen ihre Tische weiter");
        assertThrows(IllegalStateException.class, () -> t.recordResult(paused, 3, 0), "kein Ergebnis für pausierte Spiele");
        // einzeln fortsetzen
        t.resumeMatch(paused);
        assertEquals(MatchStatus.ACTIVE, paused.getStatus());
        t.pauseClass(c);   // schon angehalten -> nichts
        // umsortieren in der angehaltenen Phase: Paarung des pausierten Spiels an Tisch 2 mit Paarung 3 tauschen
        Match atTwo = t.activeMatchAt(2).orElseThrow();
        assertEquals(MatchStatus.PAUSED, atTwo.getStatus());
        int pair = atTwo.getKoIndex();
        int other = pair == 3 ? 2 : 3;
        String oldA = atTwo.getA();
        t.swapKnockoutSlots(c, 2 * pair, 2 * other);
        assertEquals(MatchStatus.PLANNED, atTwo.getStatus(), "nicht mehr gültig -> zurückgesetzt");
        assertNotEquals(oldA, atTwo.getA());
        assertTrue(t.activeMatchAt(2).isEmpty(), "Tisch frei");
        // fortsetzen: alles wieder eingereiht bzw. am Tisch
        t.resumeClass(c);
        assertEquals(MatchStatus.ACTIVE, paused.getStatus());
        assertTrue(t.activeMatchAt(2).isPresent());
    }

    /** HEM-Hauptqualifikation: 12 Gruppen, Gruppenerste direkt, Zweite + Dritte im KO, qualifiziert nach 2 Siegen (0.33). */
    @Test
    void qualifikationsmodusHem() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(30);
        PlayingClass c = groupsClass(t, "HEM", players(t, "H", 72, 2400), 12);      // 12 Gruppen à 6
        t.startClass(c);
        playAll(t, c);
        assertEquals(5, Tournament.maxQualificationRounds(24));
        PlayingClass q = t.createQualificationPhase(c, 1, 2, 2, new Random(1));
        assertEquals(12, q.getQualifiedDirect().size());
        assertEquals(12, t.matchesOf(q).size(), "Runde 1: 24 Spieler");
        // Runde 1: Zweiter gegen Dritten einer anderen Gruppe
        for (Match m : t.matchesOf(q)) {
            assertEquals(2, q.getQualPlace().get(m.getA()));
            assertEquals(3, q.getQualPlace().get(m.getB()));
            assertNotEquals(q.getQualField().get(m.getA()), q.getQualField().get(m.getB()));
        }
        playAll(t, q);
        assertEquals(12 + 6, t.matchesOf(q).size(), "Runde 2: die 12 Sieger");
        assertTrue(t.isPhaseComplete(q), "ohne Zusatzrunde fertig");
        var rows = t.finalResult(c).rows();
        assertEquals(12, rows.stream().filter(r -> r.info().equals(Tournament.QUALIFIED_BY_GROUP)).count(), "12 direkt");
        assertEquals(6, rows.stream().filter(r -> r.info().equals(Tournament.QUALIFIED_BY_KO)).count(), "6 Sieger Runde 2");
        assertEquals(6, rows.stream().filter(r -> r.info().equals("Nachrücker 1. Ranges")).count(), "Verlierer Runde 2");
        assertEquals(12, rows.stream().filter(r -> r.info().equals("Nachrücker 2. Ranges")).count(), "Verlierer Runde 1"); 
        // Gruppenerste nach TTR
        List<Integer> directTtr = rows.subList(0, 12).stream().map(r -> r.entrant().ttr()).toList();
        assertEquals(directTtr.stream().sorted(java.util.Comparator.reverseOrder()).toList(), directTtr);
        // Rang 1: Gruppenplatz zuerst (Zweite vor Dritten); Rang 2: nur nach TTR
        List<Integer> places = rows.stream().filter(r -> r.info().equals("Nachrücker 1. Ranges"))
                .map(r -> q.getQualPlace().get(r.entrant().id())).toList();
        assertEquals(places.stream().sorted().toList(), places);
        List<Integer> ttr2 = rows.stream().filter(r -> r.info().equals("Nachrücker 2. Ranges")).map(r -> r.entrant().ttr()).toList();
        assertEquals(ttr2.stream().sorted(java.util.Comparator.reverseOrder()).toList(), ttr2);

        t.finishClass(q);
        // die übrigen nach Gruppenplatz: gleicher Gruppenplatz = gleicher Platz
        assertEquals(12, rows.stream().filter(r -> r.from() == 37).count(), "alle Gruppenvierten auf Platz 37");

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        de.hts.persistence.TournamentFile.write(out, de.hts.persistence.TournamentFile.Content.of(t));
        Tournament back = de.hts.persistence.TournamentFile.read(new java.io.ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass c2 = back.findClass("HEM").orElseThrow();
        assertEquals(t.finalResult(c).rows().stream().map(Tournament.Placing::info).toList(),
                back.finalResult(c2).rows().stream().map(Tournament.Placing::info).toList());
    }

    /** Vorqualifikation: die ersten 2 je Gruppe qualifiziert, Dritte und Vierte Nachrücker (Platz, dann TTR), ab 5. geteilt. */
    @Test
    void vorqualifikationMitNachrueckernAusDerGruppenphase() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Vorquali", players(t, "V", 28, 2000), 4);    // 4 Gruppen à 7
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 2, 0, 1, 2, new Random(1));
        assertTrue(t.matchesOf(q).isEmpty(), "keine Spiele in Phase 2");
        assertTrue(t.isPhaseComplete(q));
        t.finishClass(q);
        var rows = t.finalResult(c).rows();
        assertEquals(28, rows.size());
        assertEquals(8, rows.stream().filter(r -> r.info().equals(Tournament.QUALIFIED_BY_GROUP)).count());
        var reserve = rows.stream().filter(r -> r.info().equals("Nachrücker 1. Ranges")).toList();   // ohne KO: Rang 1
        assertEquals(8, reserve.size(), "Gruppendritte und -vierte");
        assertEquals(List.of(9, 10, 11, 12, 13, 14, 15, 16), reserve.stream().map(Tournament.Placing::from).toList());
        // erst alle Dritten, dann alle Vierten; innerhalb nach TTR
        java.util.Map<String, Integer> placeOf = new java.util.HashMap<>();
        for (int g = 0; g < 4; g++) for (var r : t.standings(c, g)) placeOf.put(r.entrant().id(), r.place());
        List<Integer> places = reserve.stream().map(r -> placeOf.get(r.entrant().id())).toList();
        assertEquals(List.of(3, 3, 3, 3, 4, 4, 4, 4), places);
        for (int i = 0; i < 3; i++) assertTrue(reserve.get(i).entrant().ttr() >= reserve.get(i + 1).entrant().ttr());
        assertEquals(4, rows.stream().filter(r -> r.from() == 17).count(), "alle Fünften teilen sich Platz 17");
        assertEquals(4, rows.stream().filter(r -> r.from() == 21).count());

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        de.hts.persistence.TournamentFile.write(out, de.hts.persistence.TournamentFile.Content.of(t));
        Tournament back = de.hts.persistence.TournamentFile.read(new java.io.ByteArrayInputStream(out.toByteArray())).toTournament();
        assertEquals(rows.stream().map(Tournament.Placing::from).toList(),
                back.finalResult(back.findClass("Vorquali").orElseThrow()).rows().stream().map(Tournament.Placing::from).toList());
    }

    @Test
    void tischAbschalten() {
        Tournament t = new Tournament();
        t.setTableCount(3);
        t.setTableDisabled(2, true);
        assertEquals(List.of(1, 3), t.freeTables());
        PlayingClass c = groupsClass(t, "Tische", players(t, "T", 8, 1500), 2);
        t.startClass(c);
        assertTrue(t.matchesOf(c).stream().noneMatch(m -> Integer.valueOf(2).equals(m.getTable())), "Tisch 2 bekommt nichts");
        Match onOne = t.activeMatchAt(1).orElseThrow();
        assertThrows(IllegalStateException.class, () -> t.setTableDisabled(1, true), "Spiel am Tisch");
        assertThrows(IllegalStateException.class, () -> t.moveMatch(onOne, 2), "kein Wechsel an einen deaktivierten Tisch");
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            de.hts.persistence.TournamentFile.write(out, de.hts.persistence.TournamentFile.Content.of(t));
            Tournament back = de.hts.persistence.TournamentFile.read(new java.io.ByteArrayInputStream(out.toByteArray())).toTournament();
            assertTrue(back.isTableDisabled(2), "gespeichert");
            assertFalse(back.isTableDisabled(1));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        t.setTableDisabled(2, false);
        assertTrue(t.activeMatchAt(2).isPresent(), "nach dem Zuschalten sofort belegt");
        assertNotNull(onOne);
    }

    @Test
    void qualifikationsmodusNurAufgehendeFelder() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Damen", players(t, "D", 14, 1800), 2);
        t.startClass(c);
        playAll(t, c);
        // 6 in der KO: 3 Gewinner der Runde 1 – Runde 2 ginge nur mit Freilos -> nicht erlaubt
        assertThrows(IllegalArgumentException.class, () -> t.createQualificationPhase(c, 1, 3, 2, new Random(1)));
        assertEquals(List.of(3, 4, 7, 8, 11, 12), java.util.stream.IntStream.rangeClosed(1, 12)
                .filter(Tournament::qualificationFieldFits).boxed().toList());
        assertTrue(Tournament.qualificationFieldFits(0), "ohne KO-Runde");
    }

    @Test
    void qualifikationSpielerTauschenUndPdf() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = groupsClass(t, "Senioren", players(t, "S", 10, 1700), 2);
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 2, 2, new Random(1));   // 4 Spieler
        Match m1 = t.matchesOf(q).get(0), m2 = t.matchesOf(q).get(1);
        String a1 = m1.getA(), b2 = m2.getB();
        if (m1.getStatus().atTable()) {
            assertThrows(IllegalStateException.class, () -> t.swapQualificationPlayers(q, a1, b2), "laufende Spiele nicht");
            t.pauseClass(q);
        }
        t.swapQualificationPlayers(q, a1, b2);
        assertEquals(b2, m1.getA());
        assertEquals(a1, m2.getB());
        assertThrows(IllegalStateException.class, () -> t.swapQualificationPlayers(q, m1.getA(), m1.getB()));
        if (q.isPaused()) t.resumeClass(q);
        playAll(t, q);
        assertTrue(t.isPhaseComplete(q));
        try (var doc = de.hts.report.PdfReports.phase(t, q)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }
}
