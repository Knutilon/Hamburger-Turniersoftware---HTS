/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.persistence.TournamentFile;
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.DoublesPair;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.9.0 – Online-Anmeldung über den Live-Server (#164), Freigestellte (#161), KO-Baum (#162), Knopf „Alle Turniere“
 * (#165), Bezahlt nur mit Gebühr (#166), importierte Paarungen sofort in der Klasse (#167).
 */
class Version190Test {

    private static final String KEY = "geheimer-test-schluessel-190";
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final HttpClient raw = HttpClient.newHttpClient();

    private static Tournament tournament(String name, LocalDate date, int n) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDate(date);
        PlayingClass c = t.addClass(new PlayingClass("Herren C"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("Vorname" + i, "Nachname" + i);
            p.setInternalNr("p-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1900 - 10 * i);
            p.setClubName(i % 2 == 0 ? "TSV Elbinsel" : "SC Fleetenkamp");
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        return t;
    }

    private record Srv(LiveHost host, String base) implements AutoCloseable {
        @Override
        public void close() {
            host.stop();
        }
    }

    private static Srv server() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        LiveHost h = new LiveHost(new LiveHost.Config("127.0.0.1", 0, data, content, LiveHost.sha256(KEY.getBytes(StandardCharsets.UTF_8)),
                90, 25, "HTS Live-Scoring"));
        h.start();
        return new Srv(h, "http://127.0.0.1:" + h.port());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, String form) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void upload(Srv s, Tournament t) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(s.base() + "/api/turniere/" + t.getId())).header("Authorization", "Bearer " + KEY)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(TournamentFile.abbreviateFirstNames(TournamentFile.publishBytes(t)))).build();
        assertEquals(200, HttpClient.newHttpClient().send(r, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test
    void onlineAnmeldungUeberDenLiveServer() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
        Tournament t = tournament("Anmeldeturnier", now.toLocalDate(), 6);
        t.setId("anm1");
        t.setOnlineCheckin(true);
        t.setCheckinUntil(now.toLocalTime().plusMinutes(90).isBefore(now.toLocalTime()) ? LocalTime.of(23, 59) : now.toLocalTime().plusMinutes(90));
        try (Srv s = server()) {
            upload(s, t);
            String link = s.base() + "/anmeldung/" + t.getCheckinToken();
            HttpResponse<String> page = get(link);
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("Anmeldung am Turniertag") && page.body().contains("Alle Turniere"));
            assertFalse(page.body().contains("localStorage") || page.body().contains("sessionStorage"));
            String data = get(link + "/daten.json").body();
            assertTrue(data.contains("\"open\":true") && data.contains("TSV Elbinsel"), data);
            assertFalse(data.contains("Vorname1 "), "Vornamen abgekürzt");
            Player p1 = t.getPlayers().get(0), p2 = t.getPlayers().get(1);
            String res = post(link + "/melden", "ids=" + p1.getId() + "," + p2.getId() + "&anwesend=1").body();
            assertTrue(res.contains("\"ok\":true") && res.contains("2 Personen angemeldet"), res);
            post(link + "/melden", "ids=" + p2.getId() + "&anwesend=0");
            assertEquals(404, get(s.base() + "/anmeldung/falscherschluesselfalscherschl").statusCode(), "falscher Schlüssel");

            // HTS holt die Meldungen ab (mit dem geheimen Schlüssel) und übernimmt sie
            List<Publisher.CheckinReport> got = new ArrayList<>();
            try (Publisher pub = new Publisher(s.base(), KEY, t.getId(), Runnable::run, () -> TournamentFile.publishBytes(t), () -> null,
                    Long.MAX_VALUE, Long.MAX_VALUE)) {
                pub.setCheckinPollInterval(50);
                pub.setCheckinPoll(() -> true, list -> {
                    got.addAll(list);
                    for (Publisher.CheckinReport r : list) {
                        t.getPlayers().stream().filter(x -> x.getId().equals(r.id())).findFirst().ifPresent(x -> t.checkinOnline(x, r.present()));
                    }
                });
                long until = System.currentTimeMillis() + 5000;
                while (got.size() < 2 && System.currentTimeMillis() < until) Thread.sleep(50);
                Thread.sleep(400);                                    // Quittung
            }
            assertEquals(2, got.size(), "eine Meldung je Person (die letzte zählt)");
            assertEquals(Attendance.PRESENT, p1.getAttendance());
            assertTrue(t.isOnlineMarked(p1));
            assertEquals(Attendance.UNKNOWN, p2.getAttendance(), "online abgemeldet = nicht angemeldet");
            String left = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(s.base() + "/api/turniere/anm1/anmeldungen"))
                    .header("Authorization", "Bearer " + KEY).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(left.contains("\"meldungen\":[]"), "nach der Quittung gelöscht: " + left);
            assertEquals(401, raw.send(HttpRequest.newBuilder(URI.create(s.base() + "/api/turniere/anm1/anmeldungen")).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode(), "Abholen nur mit Schlüssel");

            // Ausschalten → geschlossen; neuer Link → alter ungültig
            t.setOnlineCheckin(false);
            upload(s, t);
            assertEquals(404, get(link).statusCode(), "ausgeschaltet");
            t.setOnlineCheckin(true);
            String old = t.getCheckinToken();
            t.newCheckinToken();
            upload(s, t);
            assertEquals(404, get(s.base() + "/anmeldung/" + old).statusCode(), "alter Link ungültig");
            assertEquals(200, get(s.base() + "/anmeldung/" + t.getCheckinToken()).statusCode());
        }
    }

    @Test
    void anmeldungNurImZeitraum() {
        ZonedDateTime n = ZonedDateTime.of(2026, 12, 6, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        LocalDate day = LocalDate.of(2026, 12, 6);
        assertTrue(LiveHost.checkinOpen(true, LocalTime.of(10, 30), day, "angekündigt", false, n));
        assertFalse(LiveHost.checkinOpen(false, LocalTime.of(10, 30), day, "angekündigt", false, n), "Schalter aus");
        assertFalse(LiveHost.checkinOpen(true, LocalTime.of(9, 30), day, "angekündigt", false, n), "Uhrzeit vorbei");
        assertFalse(LiveHost.checkinOpen(true, LocalTime.of(10, 30), day.plusDays(1), "angekündigt", false, n), "nicht der Turniertag");
        assertFalse(LiveHost.checkinOpen(true, null, day, "angekündigt", false, n), "ohne Uhrzeit");
        assertFalse(LiveHost.checkinOpen(true, LocalTime.of(10, 30), day, "beendet", false, n));
        Tournament t = tournament("X", day, 4);
        t.setOnlineCheckin(true);
        t.setCheckinUntil(LocalTime.of(10, 30));
        assertTrue(t.isOnlineCheckinOpen(n.toLocalDateTime()));
        assertFalse(t.isOnlineCheckinOpen(n.toLocalDateTime().plusHours(1)));
    }

    @Test
    void liveSeitenFreigestellteKoBaumUndAlleTurniere() {
        Tournament t = tournament("Baumturnier", LocalDate.of(2026, 12, 6), 8);
        PlayingClass c = t.findClass("Herren C").orElseThrow();
        t.drawKnockout(c, true, new Random(3));
        t.setThirdPlace(c, true);
        t.startClass(c);
        LiveServer v = LiveServer.forTest(t).home(() -> "/");
        String body = v.rebase(classHtml(v, c));
        assertTrue(body.contains("class=\"ktw\"") && body.contains("data-v=\"tree\"") && body.contains("data-v=\"list\""), "KO-Baum mit Umschalter");
        assertTrue(body.contains("Viertelfinale") && body.contains("Spiel um Platz 3"));
        assertTrue(body.contains("href=\"/\"") && body.contains("← Alle Turniere"), "Knopf zur Startseite");

        Tournament g = tournament("Gruppenturnier", LocalDate.of(2026, 12, 6), 12);
        PlayingClass gc = g.findClass("Herren C").orElseThrow();
        gc.setGroupsBySize(false);
        gc.setGroupCount(2);
        g.drawGroups(gc, true, new Random(4));
        gc.setExempt(List.of(g.getPlayers().get(0).getId(), g.getPlayers().get(1).getId()));
        String gb = classHtml(LiveServer.forTest(g), gc);
        assertTrue(gb.contains("Freigestellt") && gb.contains("direkt in der KO-Runde") && gb.contains("S1") && gb.contains("S2"), "Freigestellte über den Gruppen");
    }

    private static String classHtml(LiveServer v, PlayingClass c) {
        try {
            var m = LiveServer.class.getDeclaredMethod("classPage", String.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(v, c.getName(), null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void bezahltNurMitGebuehrUndPaarungenSofortInDerKlasse() throws Exception {
        Tournament t = new Tournament();
        var r = new ClickTtXmlImporter().importFile(Path.of("src/test/resources/demoturnier-jugend-15.xml"));
        for (ClickTtXmlImporter.CompetitionInfo ci : r.competitions()) {
            PlayingClass nc = t.addClass(new PlayingClass(ci.name(), ci.doubles() ? Discipline.DOUBLES : Discipline.SINGLES,
                    ClassType.guess(ci.name(), ci.sex()), null, null));
            nc.setImportedFromXml(true);
            nc.setFeeCents(0);
        }
        List<String[]> links = new ArrayList<>();
        for (var ip : r.pairs()) if (!ip.waitingList()) links.add(new String[]{ip.competition(), ip.firstKey(), ip.secondKey()});
        t.mergeImportedEntries(r.players(), links);
        assertFalse(t.hasFees(), "#166: Startgebühr 0 € = ohne Bezahlen");
        t.findClass("Jungen 15 Einzel").orElseThrow().setFeeCents(800);
        assertTrue(t.hasFees());

        PlayingClass mixed = t.findClass("Jugend 15 Mixed").orElseThrow();
        List<DoublesPair> mp = t.pairsIn(mixed);
        assertEquals(5, mp.stream().filter(DoublesPair::isComplete).count(), "#167: die 5 gemeldeten Mixed-Paarungen sofort in der Klasse");
        assertEquals(3, t.pairsIn(t.findClass("Mädchen 15 Doppel").orElseThrow()).size());
        assertTrue(mp.stream().allMatch(t::isPairUnconfirmed), "noch niemand angemeldet → markiert");
        assertNotNull(t.pairProblem(mixed));
        for (DoublesPair p : mp) for (Player m : p.members()) t.setAttendance(m, Attendance.PRESENT);
        assertTrue(mp.stream().noneMatch(t::isPairUnconfirmed));
        assertNull(t.pairProblem(mixed));
    }
}
