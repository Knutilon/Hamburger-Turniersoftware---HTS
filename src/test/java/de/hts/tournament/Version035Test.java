package de.hts.tournament;

import de.hts.persistence.TournamentFile;
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.web.LiveServer;
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
        DoublesPair half = t.pairOf(byName(t, "S3"), hd).orElseThrow();
        assertFalse(half.isComplete(), "Partner S4 nicht da → ohne Partner");
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
    void anmeldungPerHandy() {
        Tournament t = imported();
        Player s1 = byName(t, "S1");
        assertTrue(t.isCheckinOpen());
        assertTrue(t.checkinFromPhone(s1, true));
        assertEquals(Attendance.PRESENT, s1.getAttendance());
        assertTrue(t.checkinFromPhone(s1, false));
        assertEquals(Attendance.WITHDRAW_REQUESTED, s1.getAttendance(), "Abmeldung muss die Turnierleitung bestätigen");
        t.setAttendance(s1, Attendance.WITHDRAWN);
        assertFalse(t.checkinFromPhone(s1, true), "endgültig abgemeldet: nur die Turnierleitung");
        t.setCheckinLocked(true);
        assertFalse(t.isCheckinOpen());
        assertThrows(IllegalStateException.class, () -> t.checkinFromPhone(byName(t, "S2"), true));
        assertTrue(t.getCheckinToken().matches("[A-Z2-9]{8}"));
    }

    @Test
    void wirdGespeichert() throws Exception {
        Tournament t = imported();
        t.setAttendance(byName(t, "S1"), Attendance.PRESENT);
        t.setAttendance(byName(t, "S2"), Attendance.WITHDRAW_REQUESTED);
        t.setAttendance(byName(t, "S3"), Attendance.WITHDRAWN);
        t.setCheckinLocked(true);
        String token = t.getCheckinToken();
        Tournament back = roundTrip(t);
        assertEquals(Attendance.PRESENT, byName(back, "S1").getAttendance());
        assertEquals(Attendance.WITHDRAW_REQUESTED, byName(back, "S2").getAttendance());
        assertEquals(Attendance.WITHDRAWN, byName(back, "S3").getAttendance());
        assertEquals("id:id-S2", byName(back, "S1").getRegisteredPartner("Herren Doppel"));
        assertTrue(back.findClass("Herren Einzel").orElseThrow().isImportedFromXml());
        assertTrue(back.isCheckinLocked());
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
    void handyZugangLiveUndAnmeldung() throws Exception {
        Tournament t = imported();
        t.setName("Test-Turnier");
        PlayingClass he = t.findClass("Herren Einzel").orElseThrow();
        for (int i = 1; i <= 6; i++) t.setAttendance(byName(t, "S" + i), Attendance.PRESENT);
        t.takeOverPresentEntries(he);
        he.setGroupsBySize(false);
        he.setGroupCount(1);
        t.drawGroups(he, true, new Random(2));
        t.startClass(he);
        int[] changes = {0};
        LiveServer server = new LiveServer(t, Runnable::run, () -> changes[0]++, 0);
        server.start();
        try {
            HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
            String base = "http://127.0.0.1:" + server.port();
            HttpResponse<String> live = http.send(HttpRequest.newBuilder(URI.create(base + "/live")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, live.statusCode());
            assertTrue(live.body().contains("Herren Einzel"));
            String cls = http.send(HttpRequest.newBuilder(URI.create(base + "/live/klasse?k=Herren%20Einzel")).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(cls.contains("Gruppe A") && cls.contains("NS1") || cls.contains("S1"));
            Player s1 = byName(t, "S1");
            String me = http.send(HttpRequest.newBuilder(URI.create(base + "/live/ich?id=" + s1.getId())).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(me.contains("Meine Gruppe"));
            assertEquals(404, http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/FALSCH")).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode(), "ohne Schlüssel aus dem QR-Code keine Anmeldung");
            assertEquals(405, http.send(HttpRequest.newBuilder(URI.create(base + "/live")).POST(HttpRequest.BodyPublishers.ofString("x")).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode(), "Live-Anzeige nur lesend");
            String token = t.getCheckinToken();
            assertEquals(200, http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token)).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
            String data = http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token + "/daten.json")).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(data.contains("\"open\":true") && data.contains("TSV Nord"));
            // 0.38 (#55): reine Nachrücker melden sich nicht per Handy an oder ab
            Player nach = byName(t, "Nach");
            http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token + "/melden"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("ids=" + nach.getId() + "&anwesend=1")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(Attendance.UNKNOWN, nach.getAttendance());
            assertFalse(data.contains(nach.getId()), "Nachrücker nicht in der Anmeldeliste");
            changes[0] = 0;
            Player reserve = byName(t, "S6");
            t.setAttendance(reserve, Attendance.UNKNOWN);
            String res = http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token + "/melden"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("ids=" + reserve.getId() + "&anwesend=1")).build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(res.contains("\"ok\":true"), res);
            assertEquals(Attendance.PRESENT, reserve.getAttendance());
            assertEquals(1, changes[0]);
            http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token + "/melden"))
                    .POST(HttpRequest.BodyPublishers.ofString("ids=" + reserve.getId() + "&anwesend=0")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(Attendance.WITHDRAW_REQUESTED, reserve.getAttendance());
            t.setCheckinLocked(true);
            String closed = http.send(HttpRequest.newBuilder(URI.create(base + "/anmeldung/" + token + "/melden"))
                    .POST(HttpRequest.BodyPublishers.ofString("ids=" + reserve.getId() + "&anwesend=1")).build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(closed.contains("\"ok\":false"));
        } finally {
            server.stop();
        }
        assertFalse(server.isRunning());
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
