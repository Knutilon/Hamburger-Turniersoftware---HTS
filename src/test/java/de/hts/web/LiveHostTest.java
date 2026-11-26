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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 1.6.0 – Live-Server für den Internetserver (#101): Protokoll, Seiten, Datenschutz, Aufräumen. */
class LiveHostTest {

    private static final String KEY = "Test-Schluessel-0123456789-abcdefghijklmnop";
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final HttpClient raw = HttpClient.newHttpClient();

    private static Tournament tournament(String name) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDate(LocalDate.of(2026, 11, 28));
        PlayingClass c = t.addClass(new PlayingClass("Herren C"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Player p = new Player("Vorname" + i, "Nachname" + i);
            p.setInternalNr("hc-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1800 - 10 * i);
            p.setClubName(i == 1 ? "SC <Poppenbüttel>" : "TTC Altona");
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(3));
        t.startClass(c);
        return t;
    }

    private record Srv(LiveHost host, Path data, Path content, String base) implements AutoCloseable {
        @Override
        public void close() {
            host.stop();
        }
    }

    private static Srv server(Path data, Path content, int maxMb) throws Exception {
        LiveHost.Config cfg = new LiveHost.Config("127.0.0.1", 0, data, content, LiveHost.sha256(KEY.getBytes(StandardCharsets.UTF_8)),
                90, maxMb, "HTS Live-Scoring");
        LiveHost h = new LiveHost(cfg);
        h.start();
        return new Srv(h, data, content, "http://127.0.0.1:" + h.port());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> call(String method, String url, String key, byte[] body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url));
        if (key != null) b.header("Authorization", "Bearer " + key);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void htsVeroeffentlichtUndZuschauerSehenDieSeiten() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        try (Srv s = server(data, content, 25)) {
            assertNull(Publisher.test(s.base(), KEY), "Verbindung testen mit richtigem Schlüssel");
            assertTrue(Publisher.test(s.base(), "falsch").contains("Schlüssel abgelehnt"));
            Tournament t = tournament("Hamburger Ranglistenturnier");
            t.setId("5c0c83c510be");
            byte[] logo = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 1, 2, 3};
            try (Publisher p = new Publisher(s.base(), KEY, t.getId(), Runnable::run, () -> TournamentFile.publishBytes(t), () -> logo, 50, 100)) {
                long until = System.currentTimeMillis() + 5000;
                while (p.status().state() != Publisher.State.OK && System.currentTimeMillis() < until) Thread.sleep(50);
                assertEquals(Publisher.State.OK, p.status().state(), "Hochladen erfolgreich");
            }
            HttpResponse<String> start = get(s.base() + "/");
            assertEquals(200, start.statusCode());
            String both = start.body() + get(s.base() + "/vergangen").body();      // 1.8.0 (#157): je nach heutigem Datum
            assertTrue(both.contains("Hamburger Ranglistenturnier") && both.contains("/live/Hamburger_Ranglistenturnier_28_11_2026"),
                    "Startseite listet das Turnier mit lesbarer Adresse");
            assertTrue(start.body().contains("Laufende Turniere") && start.body().contains("href=\"/vergangen\"") && both.contains("28.11.2026"));
            assertTrue(start.body().contains("href=\"/impressum\"") && start.body().contains("href=\"/datenschutz\""), "Impressum/Datenschutz verlinkt");
            assertEquals("nosniff", start.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertTrue(start.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'self'"));
            assertTrue(start.headers().firstValue("Set-Cookie").isEmpty(), "keine Cookies");

            String live = s.base() + "/live/Hamburger_Ranglistenturnier_28_11_2026";
            HttpResponse<Void> old = raw.send(HttpRequest.newBuilder(URI.create(s.base() + "/live/5c0c83c510be/tische")).build(), HttpResponse.BodyHandlers.discarding());
            assertEquals(302, old.statusCode(), "alte Adresse mit Kennung leitet um");
            assertEquals("/live/Hamburger_Ranglistenturnier_28_11_2026/tische", old.headers().firstValue("Location").orElse(""));
            HttpResponse<String> ov = get(live);
            assertEquals(200, ov.statusCode());
            assertTrue(ov.body().contains("href=\"/live/Hamburger_Ranglistenturnier_28_11_2026/tische\""), "Verweise mit Turnier-Kennung");
            assertTrue(ov.body().contains("/live/Hamburger_Ranglistenturnier_28_11_2026/klasse?k=Herren+C"));
            assertFalse(ov.body().contains("href=\"/live/tische\""), "keine Verweise ohne Kennung");
            assertTrue(ov.body().contains("/live/Hamburger_Ranglistenturnier_28_11_2026/logo.png"), "Ausrichter-Logo eingebunden");
            assertTrue(ov.body().contains("href=\"/impressum\""));
            assertTrue(get(live + "/tische").body().contains("Nachname1"), "Tische mit Namen");
            assertTrue(get(live + "/klasse?k=Herren+C").body().contains("Viertelfinale"));
            assertTrue(get(live + "/spieler.json").body().contains("SC \\u003cPoppenb"), "Suche liefert Spieler");
            assertFalse(get(live + "/tische").body().contains("<Poppenb"), "Vereinsname in HTML maskiert");
            String search = get(live + "/suche").body();
            assertTrue(search.contains("'/live/Hamburger_Ranglistenturnier_28_11_2026/spieler.json'") && search.contains("/live/Hamburger_Ranglistenturnier_28_11_2026/ich?id="), "Skripte mit Kennung");
            HttpResponse<byte[]> lg = http.send(HttpRequest.newBuilder(URI.create(live + "/logo.png")).build(), HttpResponse.BodyHandlers.ofByteArray());
            assertArrayEquals(logo, lg.body());
            assertEquals(404, get(live + "/anmeldung/x").statusCode(), "keine Anmeldung über den Server");
            assertEquals(404, get(s.base() + "/live/unbekannt").statusCode());
            assertEquals(302, raw.send(HttpRequest.newBuilder(URI.create(s.base() + "/live")).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
            String cls = get(live + "/klasse?k=Herren+C").body();
            assertTrue(cls.contains("class=\"tabs\"") && cls.contains("Phase 1") && cls.contains("Zwischenstand"), "Reiter je Phase + Ergebnis");
            assertTrue(cls.contains("setInterval(") && !cls.contains("location.reload()"), "Aktualisierung ohne Neuladen");
            assertTrue(cls.contains("id=\"q\"") && cls.contains(">Tische</a>"), "Suchfeld und Knopf „Tische“");
            assertTrue(get(live + "/spieler.json").body().contains("\"k\":[\"Herren C\"]"), "Suche kennt die Klassen");
        }
    }

    @Test
    void schluesselGroesseUndKennungWerdenGeprueft() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        try (Srv s = server(data, content, 1)) {
            byte[] file = TournamentFile.publishBytes(tournament("T"));
            assertEquals(401, call("PUT", s.base() + "/api/turniere/abc", null, file).statusCode(), "ohne Schlüssel");
            assertEquals(401, call("PUT", s.base() + "/api/turniere/abc", "falsch", file).statusCode(), "falscher Schlüssel");
            assertEquals(401, call("GET", s.base() + "/api/status", KEY + "x", null).statusCode());
            assertEquals(200, call("GET", s.base() + "/api/status", KEY, null).statusCode());
            assertEquals(400, call("PUT", s.base() + "/api/turniere/..%2Fetc", KEY, file).statusCode(), "keine Pfade außerhalb");
            assertEquals(400, call("PUT", s.base() + "/api/turniere/a.b", KEY, file).statusCode());
            assertEquals(400, call("PUT", s.base() + "/api/turniere/abc", KEY, "<kaputt".getBytes()).statusCode(), "keine Turnierdatei");
            assertEquals(400, call("PUT", s.base() + "/api/turniere/abc", KEY,
                    "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><hts>&e;</hts>".getBytes()).statusCode(), "XXE abgewiesen");
            assertEquals(413, call("PUT", s.base() + "/api/turniere/abc", KEY, new byte[1024 * 1024 + 10]).statusCode(), "Größengrenze");
            assertEquals(400, call("PUT", s.base() + "/api/turniere/abc/logo", KEY, "GIF89a".getBytes()).statusCode(), "Logo nur PNG");
            assertEquals(200, call("PUT", s.base() + "/api/turniere/abc", KEY, file).statusCode());
            assertFalse(Files.exists(data.resolve("turniere/abc/turnier.hts.neu")), "keine halben Dateien");
            assertEquals(405, call("PATCH", s.base() + "/api/turniere/abc", KEY, file).statusCode());
        }
    }

    @Test
    void loeschenNurEndergebnisNeustartUndAufbewahrung() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        Tournament t = tournament("Herbstturnier");
        byte[] file = TournamentFile.publishBytes(t);
        try (Srv s = server(data, content, 25)) {
            assertEquals(200, call("PUT", s.base() + "/api/turniere/t1", KEY, file).statusCode());
            assertEquals(200, call("PUT", s.base() + "/api/turniere/t2", KEY, file).statusCode());
            assertEquals(200, call("POST", s.base() + "/api/turniere/t1/nur-endergebnis", KEY, null).statusCode());
            String ov = get(s.base() + "/live/t1").body();
            assertTrue(ov.contains("Endergebnisse") && ov.contains("/live/t1/ergebnis?k=Herren+C"), "nur noch Endergebnisse");
            assertFalse(ov.contains("/live/t1/tische"));
            assertEquals(302, raw.send(HttpRequest.newBuilder(URI.create(s.base() + "/live/t1/tische")).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode(), "Tische nicht mehr erreichbar");
            assertEquals(200, get(s.base() + "/live/t1/ergebnis?k=Herren+C").statusCode());
            assertEquals(200, call("DELETE", s.base() + "/api/turniere/t2", KEY, null).statusCode());
            assertFalse(Files.exists(data.resolve("turniere/t2")), "Live-Daten gelöscht");
            assertEquals(404, get(s.base() + "/live/t2").statusCode());
        }
        try (Srv s = server(data, content, 25)) {                // Neustart: Stand von der Platte
            String ov = get(s.base() + "/live/t1").body();
            assertTrue(ov.contains("Endergebnisse"), "„Nur Endergebnis“ bleibt nach Neustart");
            assertTrue(get(s.base() + "/vergangen").body().contains("Herbstturnier"), "nur Endergebnis = vergangen (1.8.0)");
            Path meta = data.resolve("turniere/t1/meta.properties");
            String m = Files.readString(meta).replaceAll("updated=\\d+", "updated=1000");
            Files.writeString(meta, m);
        }
        try (Srv s = server(data, content, 25)) {
            s.host().cleanUp();
            assertFalse(Files.exists(data.resolve("turniere/t1")), "nach der Aufbewahrungsfrist gelöscht");
            assertTrue(get(s.base() + "/").body().contains("Gerade keine laufenden Turniere"));
        }
    }

    @Test
    void impressumUndDatenschutzAusDateien() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        try (Srv s = server(data, content, 25)) {
            assertTrue(get(s.base() + "/impressum").body().contains("noch nicht hinterlegt"), "Hinweis, solange die Datei fehlt");
            Files.writeString(content.resolve("impressum.html"), "<h2>Angaben gemäß § 5 DDG</h2><p>Max Muster</p>");
            Files.writeString(content.resolve("datenschutz.html"), "<h2>Cookies</h2><p>Keine Cookies.</p>");
            assertTrue(get(s.base() + "/impressum").body().contains("Max Muster"));
            assertTrue(get(s.base() + "/datenschutz").body().contains("Keine Cookies."));
            assertTrue(get(s.base() + "/robots.txt").body().contains("Disallow: /live/"));
            assertEquals(200, raw.send(HttpRequest.newBuilder(URI.create(s.base() + "/favicon.png")).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }
}
