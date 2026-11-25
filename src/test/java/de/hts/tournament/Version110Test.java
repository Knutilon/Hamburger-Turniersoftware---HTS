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
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.report.PdfReports;
import de.hts.web.LiveServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 1.1.0 – Startzeiten im Zeitplan (#86, #88), Startgebühren überarbeitet (#89), Ergebnisse per Handy (#93). */
class Version110Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    // --- #86 / #88 Zeitplan -------------------------------------------------------------------------

    @Test
    void startzeitenErstNachDemStartOhneEingestellteStartzeit() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "Z", 8, 2);
        c.setScheduleMinutes(25);
        Match r2 = t.matchesOf(c).stream().filter(m -> m.getRound() == 2).findFirst().orElseThrow();
        assertEquals("", t.startText(r2), "vor dem Start keine Zeit");
        t.startClass(c);
        LocalDateTime anchor = c.getScheduleAnchor();
        assertNotNull(anchor);
        assertEquals("ab " + anchor.plusMinutes(25).toLocalTime().toString().substring(0, 5), t.startText(r2));
        PlayingClass plain = PhaseFixtures.groupsClass(t, "P", 6, 2);
        t.startClass(plain);
        assertEquals("", t.startText(t.matchesOf(plain).get(0)), "ohne Zeitplan keine Zeit");
        try (var pdf = PdfReports.phase(t, c); var g = PdfReports.group(t, c, 0)) {
            assertTrue(pdf.getNumberOfPages() >= 1);
            assertTrue(g.getNumberOfPages() >= 1);
        }
    }

    @Test
    void naechstgelegeneUhrzeitHoechstens12Stunden() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 23, 0);
        assertEquals(LocalDateTime.of(2026, 11, 16, 1, 30), Tournament.nearestTime(LocalTime.of(1, 30), now), "nach Mitternacht: morgen");
        assertEquals(LocalDateTime.of(2026, 11, 15, 14, 0), Tournament.nearestTime(LocalTime.of(14, 0), now), "heute Nachmittag");
        LocalDateTime morning = LocalDateTime.of(2026, 11, 15, 2, 0);
        assertEquals(LocalDateTime.of(2026, 11, 14, 22, 0), Tournament.nearestTime(LocalTime.of(22, 0), morning), "gestern Abend");
    }

    @Test
    void startzeitInDerZukunftLaesstRunde1Warten() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "F", 8, 2);
        c.setScheduleMinutes(20);
        LocalDateTime start = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        t.setScheduleStart(c, start);
        Match r1 = t.matchesOf(c).stream().filter(m -> m.getRound() == 1).findFirst().orElseThrow();
        assertFalse(t.startText(r1).isEmpty(), "eingestellte Startzeit: Zeiten schon vor dem Start");
        t.startClass(c);
        assertTrue(t.matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.PLANNED), "Runde 1 wartet auf die Startzeit");
        assertFalse(t.waitingRounds(c).isEmpty(), "„Runde jetzt starten“ möglich");
        assertTrue(t.takeScheduleQuestions().isEmpty(), "Runde 1 wartet still");
        // verschoben auf jetzt minus 1 Stunde → Runde 1 (und die schon fällige Runde 2 erst nach Runde 1) laufen
        t.setScheduleStart(c, LocalDateTime.now().minusHours(1));
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getRound() == 1).allMatch(m -> m.getStatus() != MatchStatus.PLANNED));
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getRound() == 2).allMatch(m -> m.getStatus() == MatchStatus.PLANNED),
                "nacheinander: Runde 2 erst nach Runde 1");
        for (Match m : List.copyOf(t.matchesOf(c))) if (m.getGroupIndex() == 0 && m.getRound() == 1) PhaseFixtures.play(t, m);
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getGroupIndex() == 0 && m.getRound() == 2)
                .allMatch(m -> m.getStatus() != MatchStatus.PLANNED), "fällige Runde ohne Wartefrage");
        assertTrue(t.takeScheduleQuestions().isEmpty());
        Tournament back = roundTrip(t);
        assertNotNull(back.findClass("F").orElseThrow().getScheduleStart());
        assertEquals(t.findClass("F").orElseThrow().getScheduleStart(), back.findClass("F").orElseThrow().getScheduleStart());
    }

    // --- #89 Startgebühren ---------------------------------------------------------------------------

    private static Tournament feeTournament() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass single = t.addClass(new PlayingClass("Jugend Einzel"));
        single.setFeeCents(800);
        PlayingClass doubles = t.addClass(new PlayingClass("Jugend Doppel", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        doubles.setFeeCents(1000);
        for (int i = 1; i <= 4; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("x" + i);
            p.addRegistration(single.getName());
            p.addRegistration(doubles.getName());
            t.merge(List.of(p));
        }
        return t;
    }

    @Test
    void gebuehrNurEinzelklassenUndBezahltMeldetAn() throws Exception {
        Tournament t = feeTournament();
        Player p1 = t.getPlayers().get(0);
        assertEquals(800, t.feeOf(p1), "Doppel ohne Gebühr");
        assertEquals(List.of("Jugend Einzel"), t.feeClassesOf(p1).stream().map(PlayingClass::getName).toList());
        assertFalse(p1.isPresent());
        t.markPaid(List.of(p1), Player.Payment.CASH, null);
        assertTrue(p1.isPresent(), "bezahlt → angemeldet");
        Player p2 = t.getPlayers().get(1);
        t.setAttendance(p2, Attendance.PRESENT);
        assertTrue(t.paymentPending(p2), "angemeldet heißt nicht bezahlt");
        assertFalse(t.availableForClasses(p2), "vor dem Turnierstart nur Bezahlte unter „Verfügbar“");
        assertTrue(t.availableForClasses(p1));
        PlayingClass single = t.findClass("Jugend Einzel").orElseThrow();
        assertEquals(1, t.takeOverPresentEntries(single), "nur Bezahlte übernehmen");
        assertTrue(p1.isInClass(single.getName()));
        assertFalse(p2.isInClass(single.getName()));
        // erlassen
        Player p3 = t.getPlayers().get(2);
        t.waiveFee(List.of(p3), "Helfer beim Aufbau");
        assertEquals(Player.Payment.WAIVED, p3.getPayment());
        assertTrue(p3.isPresent());
        assertFalse(t.paymentPending(p3));
        assertEquals("Helfer beim Aufbau", p3.getWaivedReason());
        // Rücknahme: angemeldet und Klassen bleiben
        t.undoPayment(List.of(p1));
        assertTrue(p1.isPresent());
        assertTrue(p1.isInClass(single.getName()));
        Tournament back = roundTrip(t);
        Player p3b = back.getPlayers().stream().filter(p -> "x3".equals(p.getInternalNr())).findFirst().orElseThrow();
        assertEquals(Player.Payment.WAIVED, p3b.getPayment());
        assertEquals("Helfer beim Aufbau", p3b.getWaivedReason());
        try (var cash = PdfReports.cashReport(t)) {
            assertTrue(cash.getNumberOfPages() >= 1);
        }
    }

    @Test
    void nachDemTurnierstartAuchUnbezahlteVerfuegbar() {
        Tournament t = feeTournament();
        PlayingClass other = t.addClass(new PlayingClass("Andere"));
        for (int i = 1; i <= 4; i++) {
            Player p = new Player("A" + i, "B" + i);
            t.merge(List.of(p));
            Player m = t.getPlayers().stream().filter(x -> x.getLastname().equals("B" + p.getFirstname().substring(1))).findFirst().orElseThrow();
            t.setAttendance(m, Attendance.PRESENT);
            t.assign(m, other);
        }
        Player unpaid = t.getPlayers().get(0);
        t.setAttendance(unpaid, Attendance.PRESENT);
        assertFalse(t.availableForClasses(unpaid));
        other.setGroupsBySize(false);
        other.setGroupCount(1);
        t.drawGroups(other, true, new java.util.Random(1));
        t.startClass(other);
        assertTrue(t.isRunning());
        assertTrue(t.availableForClasses(unpaid), "nach dem Start markiert, aber übernehmbar");
        assertTrue(t.presentUnpaid().contains(unpaid));
        Tournament plain = new Tournament();
        Player x = new Player("Ohne", "Gebühr");
        plain.merge(List.of(x));
        Player xm = plain.getPlayers().get(0);
        plain.setAttendance(xm, Attendance.PRESENT);
        assertTrue(plain.availableForClasses(xm), "ohne Gebühren wie bisher");
    }

}
