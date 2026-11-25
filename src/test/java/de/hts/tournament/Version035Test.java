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
import de.hts.player.Sex;
import de.hts.web.QrCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.35.0: Anwesenheit am Turniertag, Meldungen statt „In der Klasse“, Handy-Zugang (Live + Anmeldung). */
class Version035Test {

    private static Player p(String last, String club, int ttr) {
        Player x = new Player("V" + last, last);
        x.setInternalNr("id-" + last);
        x.setClubName(club);
        x.setTtr(ttr);
        x.setSex(Sex.MALE);
        x.setBirthyear(1990);
        return x;
    }

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    /** Wie nach dem click-TT-Import: Klassen „aus XML importiert“, Meldungen, Doppelpartner. */
    private static Tournament imported() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass he = t.addClass(new PlayingClass("Herren Einzel"));
        he.setImportedFromXml(true);
        PlayingClass hd = t.addClass(new PlayingClass("Herren Doppel", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        hd.setImportedFromXml(true);
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Player x = p("S" + i, i % 2 == 0 ? "TSV Nord" : "SC Süd", 1500 - i * 10);
            x.addToClass("Herren Einzel");                 // so liefert der Import die Meldung
            if (i <= 4) x.addToClass("Herren Doppel");
            ps.add(x);
        }
        Player reserve = p("Nach", "SC Süd", 1200);
        reserve.addWaitingList("Herren Einzel");
        ps.add(reserve);
        t.mergeImportedEntries(ps, List.of(new String[]{"Herren Doppel", "id:id-S1", "id:id-S2"},
                new String[]{"Herren Doppel", "id:id-S3", "id:id-S4"}));
        return t;
    }

    private static Player byName(Tournament t, String last) {
        return t.getPlayers().stream().filter(x -> x.getLastname().equals(last)).findFirst().orElseThrow();
    }

    @Test
    void importLegtMeldungenAnNichtInDieKlasse() {
        Tournament t = imported();
        PlayingClass he = t.findClass("Herren Einzel").orElseThrow();
        assertEquals(0, t.countIn(he), "„In der Klasse“ beginnt bei 0");
        Player s1 = byName(t, "S1");
        assertTrue(s1.getRegistrations().contains("Herren Einzel"));
        assertEquals("id:id-S2", s1.getRegisteredPartner("Herren Doppel"));
        assertTrue(byName(t, "Nach").isReserve(), "Nachrücker aus der Warteliste");
        assertFalse(s1.isReserve());
        assertEquals(Attendance.UNKNOWN, s1.getAttendance());
    }

    @Test
    void uebernahmeNurAnwesenderGemeldeterMitPartner() {
        Tournament t = imported();
        PlayingClass he = t.findClass("Herren Einzel").orElseThrow();
        PlayingClass hd = t.findClass("Herren Doppel").orElseThrow();
        for (String n : List.of("S1", "S2", "S3", "S5")) t.setAttendance(byName(t, n), Attendance.PRESENT);
        t.setAttendance(byName(t, "Nach"), Attendance.PRESENT);
        assertEquals(4, t.takeOverPresentEntries(he), "nur anwesende Gemeldete – kein Nachrücker");
        assertTrue(byName(t, "S5").isInClass("Herren Einzel"));
        assertFalse(byName(t, "S4").isInClass("Herren Einzel"), "nicht anwesend");
        assertFalse(byName(t, "Nach").isInClass("Herren Einzel"), "Nachrücker zieht die Turnierleitung von Hand");
        t.takeOverPresentEntries(hd);
        DoublesPair pair = t.pairOf(byName(t, "S1"), hd).orElseThrow();
        assertTrue(pair.isComplete());
        assertEquals(byName(t, "S2"), pair.partnerOf(byName(t, "S1")), "mit dem gemeldeten Partner");
        // 1.9.0 (#167): gemeldete Paarungen stehen schon nach dem Import in der Klasse – S4 fehlt noch → markiert
        DoublesPair half = t.pairOf(byName(t, "S3"), hd).orElseThrow();
        assertTrue(half.isComplete(), "gemeldete Paarung sofort angelegt");
        assertTrue(t.isPairUnconfirmed(half), "S4 noch nicht angemeldet → markiert");
        assertFalse(t.isPairUnconfirmed(pair));
        assertNotNull(t.pairProblem(hd), "Auslosen/Start gesperrt");
        t.setAttendance(byName(t, "S4"), Attendance.PRESENT);
        assertNull(t.pairProblem(hd));
        t.setAttendance(byName(t, "S4"), Attendance.WITHDRAWN);
        assertTrue(t.pairProblem(hd).contains("Partner/in abgemeldet"));
    }

    @Test
    void startErstWennAlleGemeldetenAnOderAbgemeldetSind() {
        Tournament t = imported();
        PlayingClass he = t.findClass("Herren Einzel").orElseThrow();
        for (int i = 1; i <= 5; i++) t.setAttendance(byName(t, "S" + i), Attendance.PRESENT);
        t.takeOverPresentEntries(he);
        t.drawKnockout(he, true, new Random(1));
        t.setClassReady(he, true);
        assertEquals(List.of(byName(t, "S6")), t.openAttendance(he), "Nachrücker zählen nicht");
        assertThrows(IllegalStateException.class, () -> t.startClass(he));
        assertEquals(List.of(he), t.previewStart().attendanceOpen());
        t.setAttendance(byName(t, "S6"), Attendance.WITHDRAW_REQUESTED);
        assertNotNull(t.attendanceProblem(he), "unbestätigte Abmeldung per Handy ist noch offen");
        t.setAttendance(byName(t, "S6"), Attendance.WITHDRAWN);
        assertNull(t.attendanceProblem(he));
        t.startClass(he);
        assertTrue(he.isStarted());

        // von Hand angelegte Klassen werden nie gesperrt
        PlayingClass manual = t.addClass(new PlayingClass("Freies Einzel"));
        Player a = p("Frei1", "X", 1000), b = p("Frei2", "X", 990);
        t.merge(List.of(a, b));
        t.assign(byName(t, "Frei1"), manual);
        t.assign(byName(t, "Frei2"), manual);
        assertTrue(t.openAttendance(manual).isEmpty());
    }

    @Test
    void anmeldungOnline() {
        // 1.9.0 (#164): Online-Anmeldung statt Anmeldung per Handy im Hallen-WLAN
        Tournament t = imported();
        Player s1 = byName(t, "S1");
        assertTrue(t.checkinOnline(s1, true));
        assertEquals(Attendance.PRESENT, s1.getAttendance());
        assertTrue(t.isOnlineMarked(s1));
        assertTrue(t.checkinOnline(s1, false));
        assertEquals(Attendance.UNKNOWN, s1.getAttendance(), "Online-Abmeldung = nicht angemeldet");
        assertTrue(t.undoOnline(s1));
        assertEquals(Attendance.UNKNOWN, s1.getAttendance(), "zurück auf den Stand vor der ersten Online-Meldung");
        t.setAttendance(s1, Attendance.WITHDRAWN);
        assertFalse(t.checkinOnline(s1, true), "endgültig abgemeldet: nur die Turnierleitung");
        assertTrue(t.getCheckinToken().matches("[a-z2-9]{26}"));
    }

    @Test
    void wirdGespeichert() throws Exception {
        Tournament t = imported();
        t.setAttendance(byName(t, "S1"), Attendance.PRESENT);
        t.setAttendance(byName(t, "S2"), Attendance.WITHDRAW_REQUESTED);
        t.setAttendance(byName(t, "S3"), Attendance.WITHDRAWN);
        t.setOnlineCheckin(true);
        t.setCheckinUntil(java.time.LocalTime.of(10, 30));
        t.checkinOnline(byName(t, "S4"), true);
        String token = t.getCheckinToken();
        Tournament back = roundTrip(t);
        assertEquals(Attendance.PRESENT, byName(back, "S1").getAttendance());
        assertEquals(Attendance.WITHDRAW_REQUESTED, byName(back, "S2").getAttendance());
        assertEquals(Attendance.WITHDRAWN, byName(back, "S3").getAttendance());
        assertEquals("id:id-S2", byName(back, "S1").getRegisteredPartner("Herren Doppel"));
        assertTrue(back.findClass("Herren Einzel").orElseThrow().isImportedFromXml());
        assertTrue(back.isOnlineCheckin());
        assertEquals(java.time.LocalTime.of(10, 30), back.getCheckinUntil());
        assertTrue(back.isOnlineMarked(byName(back, "S4")));
        assertEquals(token, back.getCheckinToken());
        assertEquals(0, back.countIn(back.findClass("Herren Einzel").orElseThrow()), "keine Umstellung bei neuen Dateien");
    }

    @Test
    void aeltereDateienNichtAusgeloste_KlassenStartenBeiNull() throws Exception {
        Tournament t = new Tournament();
        PlayingClass open = t.addClass(new PlayingClass("Offen"));
        PlayingClass drawn = t.addClass(new PlayingClass("Ausgelost"));
        PlayingClass dbl = t.addClass(new PlayingClass("Doppel", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) ps.add(p("L" + i, "V", 1000 + i));
        t.merge(ps);
        for (Player x : t.getPlayers()) {
            t.assign(x, open);
            t.assign(x, drawn);
        }
        t.createPair(dbl, byName(t, "L1"), byName(t, "L2"));
        t.drawKnockout(drawn, true, new Random(1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        String xml = out.toString(StandardCharsets.UTF_8).replace(" model=\"35\"", "");      // wie eine Datei vor 0.35
        Tournament back = TournamentFile.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).toTournament();
        assertEquals(0, back.countIn(back.findClass("Offen").orElseThrow()));
        assertTrue(byName(back, "L1").getRegistrations().contains("Offen"));
        assertEquals(4, back.countIn(back.findClass("Ausgelost").orElseThrow()), "ausgeloste Klassen bleiben");
        assertTrue(back.pairsIn(back.findClass("Doppel").orElseThrow()).isEmpty());
        assertEquals(byName(back, "L2").identityKey(), byName(back, "L1").getRegisteredPartner("Doppel"));
    }

    @Test
    void qrCodeGroessen() {
        assertEquals(21, QrCode.encode("HTS").size(), "Version 1");
        QrCode url = QrCode.encode("http://192.168.2.20:8080/anmeldung/K7P2X9AB");
        assertTrue(url.size() >= 29 && url.size() <= 37);
        assertTrue(url.get(0, 0) && url.get(6, 6) && !url.get(7, 7), "Suchmuster oben links");
        assertThrows(IllegalArgumentException.class, () -> QrCode.encode("x".repeat(400)));
    }
}
