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
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 1.12.0 – Ausrichter-Schlüssel selbst beantragen + Verwaltung (#189), Webserver-Seite ohne Admin-Passwort (#190). */
class Version1120Test {

    private static final String KEY = "betreiber-test-schluessel-1120";
    private final HttpClient http = HttpClient.newBuilder().build();

    private record Srv(LiveHost host, String base, String admin, Path data) implements AutoCloseable {
        @Override
        public void close() {
            host.stop();
        }
    }

    private static int freePort() throws Exception {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    private static Srv server(Path data) throws Exception {
        Path content = Files.createTempDirectory("hts-inhalt");
        int ap = freePort();
        LiveHost h = new LiveHost(new LiveHost.Config("127.0.0.1", 0, data, content, LiveHost.sha256(KEY.getBytes(StandardCharsets.UTF_8)),
                90, 25, "HTS Live-Scoring", ap));
        h.start();
        return new Srv(h, "http://127.0.0.1:" + h.port(), "http://localhost:" + ap, data);
    }

    private static byte[] tournament(String name) throws Exception {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDate(LocalDate.now().plusDays(2));
        t.addClass(new PlayingClass("Herren C"));
        return TournamentFile.publishBytes(t);
    }

    private HttpResponse<String> api(String method, String url, String key, byte[] body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + key);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, String form, String origin) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        if (origin != null) b.header("Origin", origin);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String apply(Srv s, String org, String name) throws Exception {
        HttpResponse<String> r = post(s.base() + "/schluessel", "verein=" + enc(org) + "&name=" + enc(name) + "&email=" + enc("info@example.org")
                + "&darf=1&datenschutz=1&website=", null);
        assertEquals(200, r.statusCode(), r.body());
        Matcher m = Pattern.compile("<code id=\"key\">(hts(?:-[0-9A-Za-z]{4}){6})</code>").matcher(r.body());
        assertTrue(m.find(), "Schlüssel wird einmal angezeigt");
        assertTrue(r.body().contains("wartet auf Freigabe"));
        return m.group(1);
    }

    private String adminAction(Srv s, String tat, String org) throws Exception {
        String page = http.send(HttpRequest.newBuilder(URI.create(s.admin() + "/admin")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        String token = s.host().keys.all().isEmpty() ? "" : page.replaceAll("(?s).*name=\"t\" value=\"([0-9a-f]+)\".*", "$1");
        String id = s.host().keys.all().stream().filter(k -> k.org.equals(org)).findFirst().orElseThrow().id;
        HttpResponse<String> r = post(s.admin() + "/admin/aktion", "t=" + token + "&id=" + id + "&tat=" + tat, s.admin());
        assertEquals(303, r.statusCode(), r.body());
        return id;
    }

    @Test
    void antragFreigabeUndNurEigeneTurniere() throws Exception {
        try (Srv s = server(Files.createTempDirectory("hts-live"))) {
            String form = http.send(HttpRequest.newBuilder(URI.create(s.base() + "/schluessel")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(form.contains("Ausrichter-Schlüssel beantragen") && form.contains("Verein oder Verband") && form.contains("So geht es"));
            assertFalse(form.contains("…"), "keine abgekürzten Texte");
            assertTrue(http.send(HttpRequest.newBuilder(URI.create(s.base() + "/")).GET().build(), HttpResponse.BodyHandlers.ofString()).body()
                    .contains("href=\"/schluessel\""), "Link in der Fußzeile");

            String a = apply(s, "TSV Elbinsel", "Jana Petersen");
            String b = apply(s, "SC Fleetenkamp", "Tom Krüger");
            assertFalse(Files.readString(s.data().resolve(OrganizerKeys.FILE)).contains(a), "nur der Prüfwert wird gespeichert");

            HttpResponse<String> wait = api("GET", s.base() + "/api/status", a, null);
            assertEquals(403, wait.statusCode());
            assertEquals("Schlüssel wartet auf Freigabe", wait.body());
            assertEquals("Schlüssel wartet auf Freigabe", Publisher.keyMessage(403, wait.body()));
            assertEquals(401, api("GET", s.base() + "/api/status", "hts-falsch", null).statusCode());

            adminAction(s, "freigeben", "TSV Elbinsel");
            adminAction(s, "freigeben", "SC Fleetenkamp");
            assertEquals(200, api("GET", s.base() + "/api/status", a, null).statusCode());
            assertEquals(200, api("PUT", s.base() + "/api/turniere/elb1", a, tournament("Elbinsel-Cup")).statusCode());
            HttpResponse<String> foreign = api("PUT", s.base() + "/api/turniere/elb1", b, tournament("Übernahme"));
            assertEquals(403, foreign.statusCode(), "fremdes Turnier nicht änderbar");
            assertTrue(Publisher.keyMessage(403, foreign.body()).contains("anderen Ausrichter"));
            assertEquals(403, api("DELETE", s.base() + "/api/turniere/elb1", b, null).statusCode());
            assertEquals(404, api("DELETE", s.base() + "/api/turniere/gibtsnicht", b, null).statusCode());
            assertEquals(200, api("PUT", s.base() + "/api/turniere/elb1", KEY, tournament("Elbinsel-Cup")).statusCode(), "Betreiber darf alles");

            String start = http.send(HttpRequest.newBuilder(URI.create(s.base() + "/")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(start.contains("Elbinsel-Cup"));
            api("PUT", s.base() + "/api/turniere/elb2", a, tournament("Zweites Turnier"));
            start = http.send(HttpRequest.newBuilder(URI.create(s.base() + "/")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(start.contains("TSV Elbinsel"), "Ausrichtername kommt aus dem Schlüssel");

            String page = s.host().keys.all().isEmpty() ? "" : new KeyAdmin(s.host(), 1).page(java.util.Map.of());
            assertTrue(page.contains("TSV Elbinsel") && page.contains("Betreiber") && page.contains("Offene Anträge"));

            adminAction(s, "sperren", "SC Fleetenkamp");
            HttpResponse<String> locked = api("GET", s.base() + "/api/status", b, null);
            assertEquals(403, locked.statusCode());
            assertTrue(locked.body().startsWith("Schlüssel gesperrt"));
            adminAction(s, "loeschen", "SC Fleetenkamp");
            assertEquals(401, api("GET", s.base() + "/api/status", b, null).statusCode());
        }
    }

    @Test
    void schluesselUeberlebenNeustartUndAblehnungVerschwindet() throws Exception {
        Path data = Files.createTempDirectory("hts-live");
        String a;
        try (Srv s = server(data)) {
            a = apply(s, "TTG Hammer Deich", "Jonas Wulf");
            apply(s, "Spam-Verein", "Max Muster");
            adminAction(s, "freigeben", "TTG Hammer Deich");
            adminAction(s, "ablehnen", "Spam-Verein");
            api("PUT", s.base() + "/api/turniere/hd1", a, tournament("Deich-Turnier"));
        }
        try (Srv s = server(data)) {
            assertEquals(200, api("GET", s.base() + "/api/status", a, null).statusCode(), "Schlüssel bleibt nach Neustart gültig");
            assertEquals(1, s.host().tournamentsByOwner().getOrDefault(s.host().keys.find(a).id, 0));
            s.host().keys.cleanUp(System.currentTimeMillis() + OrganizerKeys.REJECTED_KEEP_MS + 60_000);
            assertTrue(s.host().keys.all().stream().noneMatch(k -> k.org.equals("Spam-Verein")), "abgelehnt nach 30 Tagen gelöscht");
        }
    }

    @Test
    void formularPruefungRatenbremseUndAdminSchutz() throws Exception {
        try (Srv s = server(Files.createTempDirectory("hts-live"))) {
            HttpResponse<String> r = post(s.base() + "/schluessel", "verein=X&name=Jo&email=kaputt&darf=1", null);
            assertEquals(400, r.statusCode());
            assertTrue(r.body().contains("kerr"));
            HttpResponse<String> bot = post(s.base() + "/schluessel", "verein=Bot&name=Bot+Bot&email=a%40b.de&darf=1&datenschutz=1&website=spam", null);
            assertEquals(200, bot.statusCode());
            assertTrue(s.host().keys.all().isEmpty(), "Honigtopf-Feld ausgefüllt: kein Antrag");

            KeyRequestLimit lim = new KeyRequestLimit();
            for (int i = 0; i < KeyRequestLimit.PER_ADDRESS; i++) assertTrue(lim.allow("1.2.3.4", 1000));
            assertFalse(lim.allow("1.2.3.4", 2000), "höchstens 3 je Adresse und Stunde");
            assertTrue(lim.allow("1.2.3.4", 1000 + KeyRequestLimit.WINDOW_MS + 1));

            // Verwaltung: nur über localhost, Aktionen nur mit Prüfzeichen und passendem Origin
            String admin127 = s.admin().replace("localhost", "127.0.0.1");
            assertEquals(200, http.send(HttpRequest.newBuilder(URI.create(admin127 + "/admin")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertTrue(KeyAdmin.localHost("localhost:8091") && KeyAdmin.localHost("127.0.0.1") && !KeyAdmin.localHost("boese.example:8091"));
            assertFalse(KeyAdmin.localOrigin("https://boese.example"));
            assertFalse(KeyAdmin.localOrigin("null"), "Origin null (Referrer-Policy) wird abgelehnt – daher same-origin");
            apply(s, "TSV Elbinsel", "Jana Petersen");
            String id = s.host().keys.all().get(0).id;
            assertEquals(403, post(s.admin() + "/admin/aktion", "t=falsch&id=" + id + "&tat=freigeben", s.admin()).statusCode());
            assertEquals(403, post(s.admin() + "/admin/aktion", "t=" + s.host().keys.all().size() + "&id=" + id + "&tat=freigeben", "https://boese.example").statusCode());
            assertEquals(OrganizerKeys.Status.OFFEN, s.host().keys.get(id).status);
            assertTrue(OrganizerKeys.newKey().matches("hts(-[2-9a-km-np-zA-HJ-NP-Z]{4}){6}"));
        }
    }

    @Test
    void webserverSeiteOhneAdminPasswort() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("de.hts.web.AdminPassword"), "Admin-Passwort entfällt (#190)");
    }
}
