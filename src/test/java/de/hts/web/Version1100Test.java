/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.10.0 – komprimierter Upload alle 30 s (#170), Seiten-Zwischenspeicher (#171), Download HTS (#168), KO-Karten und
 * sauberer KO-Baum (#174), Untertitel (#175), „Laufende Turniere“ (#176).
 */
class Version1100Test {

    private static final String KEY = "geheimer-test-schluessel-1100";
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private static Tournament tournament(String name, int n) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDate(LocalDate.now().plusDays(3));
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
        t.drawKnockout(c, true, new Random(5));
        t.startClass(c);
        return t;
    }

    private record Srv(LiveHost host, String base) implements AutoCloseable {
        @Override
        public void close() {
            host.stop();
        }
    }

    private static Srv server(int maxMb) throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        LiveHost h = new LiveHost(new LiveHost.Config("127.0.0.1", 0, data, content, LiveHost.sha256(KEY.getBytes(StandardCharsets.UTF_8)),
                90, maxMb, "HTS Live-Scoring"));
        h.start();
        return new Srv(h, "http://127.0.0.1:" + h.port());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String url, byte[] body, boolean gzip) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + KEY)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        if (gzip) b.header("Content-Encoding", "gzip");
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void komprimierterUploadHoechstensAlle30Sekunden() throws Exception {
        assertEquals(30_000, Publisher.MIN_INTERVAL_MS, "höchstens alle 30 Sekunden");
        try (Srv s = server(1)) {
            Tournament t = tournament("Gzip-Turnier", 8);
            t.setId("gz1100");
            try (Publisher p = new Publisher(s.base(), KEY, t.getId(), Runnable::run, () -> TournamentFile.publishBytes(t), () -> null, 50, 100)) {
                long until = System.currentTimeMillis() + 5000;
                while (p.status().state() != Publisher.State.OK && System.currentTimeMillis() < until) Thread.sleep(20);
                assertEquals(Publisher.State.OK, p.status().state());
                assertTrue(p.usesGzip(), "Server meldet: komprimiert erlaubt");
                t.setName("Gzip-Turnier umbenannt");
                p.changed();
                until = System.currentTimeMillis() + 5000;
                String start = "";
                while (!start.contains("Gzip-Turnier umbenannt") && System.currentTimeMillis() < until) {
                    Thread.sleep(50);
                    start = get(s.base() + "/").body();
                }
                assertTrue(start.contains("Gzip-Turnier umbenannt"), "komprimierter Upload kommt an");
            }
            byte[] raw = TournamentFile.publishBytes(tournament("Roh", 4));
            byte[] gz = Publisher.gzip(raw);
            assertTrue(gz.length * 2 < raw.length, "deutlich kleiner: " + gz.length + " statt " + raw.length);
            assertEquals(200, put(s.base() + "/api/turniere/roh1", gz, true).statusCode());
            assertEquals(200, put(s.base() + "/api/turniere/roh2", raw, false).statusCode(), "unkomprimiert geht weiter");
            assertEquals(413, put(s.base() + "/api/turniere/kaputt", "kein gzip".getBytes(StandardCharsets.UTF_8), true).statusCode());
            byte[] bomb = Publisher.gzip(new byte[3 * 1024 * 1024]);       // 3 MB Nullen, höchstens 1 MB erlaubt
            assertTrue(bomb.length < 100_000);
            assertEquals(413, put(s.base() + "/api/turniere/bombe", bomb, true).statusCode(), "Zip-Bombe abgelehnt");
            assertNull(LiveHost.gunzip(bomb, 1024 * 1024));
        }
    }

    @Test
    void seitenWerdenBisZumNaechstenUploadZwischengespeichert() throws Exception {
        try (Srv s = server(25)) {
            Tournament t = tournament("Cache-Turnier", 8);
            t.setId("cache1100");
            assertEquals(200, put(s.base() + "/api/turniere/" + t.getId(), TournamentFile.publishBytes(t), false).statusCode());
            String url = get(s.base() + "/").body().replaceAll("(?s).*href=\"(/live/[^\"]+)\".*", "$1");
            String a = get(s.base() + url + "/klasse?k=Herren+C").body(), b = get(s.base() + url + "/klasse?k=Herren+C").body();
            assertEquals(a, b);
            get(s.base() + url + "/tische");
            assertTrue(view(s, t.getId()).cachedPages() >= 2, "Seiten liegen im Zwischenspeicher");
            Match m = t.getMatches().stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE && x.isReady()).findFirst().orElseThrow();
            t.recordResult(m, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
            assertEquals(200, put(s.base() + "/api/turniere/" + t.getId(), TournamentFile.publishBytes(t), false).statusCode());
            String c = get(s.base() + url + "/klasse?k=Herren+C").body();
            assertNotEquals(a, c, "neuer Stand → neue Seite");
            assertTrue(c.contains("<i class=\"g\">11</i>"), "Ergebnis sichtbar");
        }
    }

    private static LiveServer view(Srv s, String id) throws Exception {
        var f = LiveHost.class.getDeclaredField("entries");
        f.setAccessible(true);
        Object e = ((Map<?, ?>) f.get(s.host())).get(id);
        var v = e.getClass().getDeclaredField("view");
        v.setAccessible(true);
        return (LiveServer) v.get(e);
    }

    @Test
    void downloadSeiteReiterKopfUndFusszeile() throws Exception {
        try (Srv s = server(25)) {
            String start = get(s.base() + "/").body();
            assertTrue(start.contains(">Laufende Turniere</a>") && !start.contains("Aktuelle Turniere"), "Reiter umbenannt (#176)");
            assertTrue(start.contains("href=\"/download\">Download HTS</a>"), "Reiter Download HTS");
            assertTrue(start.contains("<small>Hamburger Turniersoftware</small>") && !start.contains("<small>Live-Ergebnisse</small>"), "Untertitel (#175)");
            assertTrue(start.contains("Gerade keine laufenden Turniere."));
            HttpResponse<String> dl = get(s.base() + "/download");
            assertEquals(200, dl.statusCode());
            String d = dl.body();
            assertTrue(d.contains("href=\"https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/releases/latest\"") && d.contains("Neueste Version herunterladen"));
            assertTrue(d.contains("Projekt auf GitHub") && d.contains("Welche Datei brauche ich?") && d.contains("macOS (Apple-Chip)") && d.contains("Gut zu wissen"));
            assertTrue(d.contains("href=\"/download\" class=\"on\""), "Reiter aktiv");
            assertTrue(d.contains("target=\"_blank\" rel=\"noopener\""));
            assertFalse(d.contains("← Alle Turniere"), "Reiterseite ohne Zurück-Knopf");
            assertTrue(d.contains("<a href=\"/download\">Download HTS</a></footer>") && d.contains("Spenden</a> <a"), "Fußzeile, umbrechbar");
            HttpResponse<byte[]> logo = http.send(HttpRequest.newBuilder(URI.create(s.base() + "/hts-logo.png?v=1")).build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, logo.statusCode());
            assertEquals("image/png", logo.headers().firstValue("Content-Type").orElse(""));
            assertTrue(get(s.base() + "/spenden").body().contains("kostenfreie Nutzung der „Hamburger Turniersoftware“"));
            assertTrue(get(s.base() + "/spenden").body().contains("<small>Hamburger Turniersoftware</small>"));
        }
    }

    @Test
    void koKartenMitSatzspaltenUndKlammerlinien() throws Exception {
        Tournament t = tournament("Baum", 8);
        t.setThirdPlace(t.findClass("Herren C").orElseThrow(), true);
        List<Match> ready = t.getMatches().stream().filter(x -> x.getKoRound() >= 0 && x.isReady() && x.getStatus() == MatchStatus.ACTIVE).toList();
        t.recordResult(ready.get(0), List.of(new int[]{11, 7}, new int[]{9, 11}, new int[]{11, 5}, new int[]{8, 11}, new int[]{11, 9}), 0);
        String h = classHtml(LiveServer.forTest(t), t.findClass("Herren C").orElseThrow());
        assertTrue(h.contains("class=\"kt kl\""), "regelmäßiger Baum mit Linien");
        assertTrue(h.contains("class=\"kp j\"") && h.contains("class=\"kx o\"") && h.contains("class=\"kx i\""), "Klammer: Paare, Striche aus/zu den Karten");
        assertTrue(h.contains("class=\"s w\""), "Siegerzeile hinterlegt");
        assertTrue(h.contains("<i class=\"g\">11</i><i>9</i><i class=\"g\">11</i><i>8</i><i class=\"g\">11</i><b>3</b>"), "Sätze als Spalten, Satzstand");
        assertTrue(h.contains("<i>7</i><i class=\"g\">11</i><i>5</i><i class=\"g\">11</i><i>9</i><b>2</b>"));
        assertFalse(h.substring(h.indexOf("class=\"ktw\""), h.indexOf("class=\"k3\"")).contains("chip res"), "keine Ergebnis-Plakette mehr im Baum");
        assertTrue(h.contains("class=\"k3\"") && h.contains("Spiel um Platz 3"), "Platz 3 unter dem Baum");
        assertTrue(h.contains("chip run\">laufend · "), "laufend mit Tisch, ohne Zwischenstand");
        assertTrue(h.contains("class=\"chip\">offen</span>"));
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
}
