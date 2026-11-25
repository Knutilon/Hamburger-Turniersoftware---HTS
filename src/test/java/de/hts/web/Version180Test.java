/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import de.hts.persistence.ProgramFolder;
import de.hts.persistence.StorageLocations;
import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

/**
 * HTS 1.8.0 – Live-Seiten: Logo mit Version (#149), Ausrichter (#150), Turnierwelt (#151), Spenden (#153), nichts im
 * Browser gespeichert (#155), Startseite geteilt (#157), Seite je Spiel (#158); Programm: Dunkelmodus, Speicherorte und
 * Programmordner (#152), Löschaufträge (#154), Veröffentlichen als Standard und Vornamen immer abgekürzt (#156).
 */
class Version180Test {

    private static final String KEY = "geheimer-test-schluessel-180";
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final HttpClient raw = HttpClient.newHttpClient();

    private static Tournament tournament(String name, LocalDate date, String... classes) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDate(date);
        int n = 0;
        for (String cn : classes) {
            PlayingClass c = t.addClass(new PlayingClass(cn));
            List<Player> ps = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                n++;
                Player p = new Player("Vorname" + n, "Nachname" + n);
                p.setInternalNr("p-" + n);
                p.setSex(Sex.MALE);
                p.setTtr(1900 - 10 * n);
                p.setClubName("Verein " + (n % 3));
                ps.add(p);
            }
            t.merge(ps);
            ps.forEach(p -> t.assign(p, c));
            t.drawKnockout(c, true, new Random(5));
            t.startClass(c);
        }
        return t;
    }

    private record Srv(LiveHost host, Path content, String base) implements AutoCloseable {
        @Override
        public void close() {
            host.stop();
        }
    }

    private static Srv server() throws Exception {
        Path data = Files.createTempDirectory("hts-live"), content = Files.createTempDirectory("hts-inhalt");
        LiveHost.Config cfg = new LiveHost.Config("127.0.0.1", 0, data, content, LiveHost.sha256(KEY.getBytes(StandardCharsets.UTF_8)),
                90, 25, "HTS Live-Scoring");
        LiveHost h = new LiveHost(cfg);
        h.start();
        return new Srv(h, content, "http://127.0.0.1:" + h.port());
    }

    private HttpResponse<String> get(String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void publish(Srv s, Tournament t, String organizer) throws Exception {
        try (Publisher p = new Publisher(s.base(), KEY, t.getId(), Runnable::run, () -> TournamentFile.publishBytes(t), () -> null, 50, 100)) {
            p.setTransform(TournamentFile::abbreviateFirstNames);
            p.setOrganizer(() -> organizer);
            long until = System.currentTimeMillis() + 5000;
            while (p.status().state() != Publisher.State.OK && System.currentTimeMillis() < until) Thread.sleep(50);
            assertEquals(Publisher.State.OK, p.status().state(), "Hochladen erfolgreich");
        }
    }

    @Test
    void livetSeitenOhneSpeicherungImBrowserMitSpielseitenUndAusrichter() throws Exception {
        try (Srv s = server()) {
            Tournament t = tournament("Sommerturnier", LocalDate.now().plusDays(3), "Herren C", "Damen B");
            t.setId("sommer1");
            t.setWorldUrl("https://www.mytischtennis.de/turnierwelt/12345");
            publish(s, t, "TTC Altona e.V. & Freunde");
            String live = s.base() + "/live/" + LiveSlug.of(t.getName(), t.getDate());

            // #155: nirgends localStorage, sessionStorage oder Cookies
            for (String page : List.of(live, live + "/tische", live + "/klasse?k=Herren+C", live + "/ich?id=" + t.getPlayers().get(0).getId(),
                    live + "/Herren_C/spiel1", s.base() + "/", s.base() + "/vergangen", s.base() + "/spenden")) {
                HttpResponse<String> r = get(page);
                assertEquals(200, r.statusCode(), page);
                for (String bad : List.of("localStorage", "sessionStorage", "document.cookie")) assertFalse(r.body().contains(bad), bad + " in " + page);
                assertTrue(r.headers().firstValue("Set-Cookie").isEmpty());
            }
            String me = get(live + "/ich?id=" + t.getPlayers().get(0).getId()).body();
            assertTrue(me.contains("als Lesezeichen speichern") && me.contains("data-share"), "Lesezeichen-Hinweis und „Link kopieren“");

            // #158: Seite je Spiel, Nummer je Klasse
            String cls = get(live + "/klasse?k=Herren+C").body();
            assertTrue(cls.contains("href=\"/live/" + LiveSlug.of(t.getName(), t.getDate()) + "/Herren_C/spiel1\""), "Spiele verlinkt");
            String m1 = get(live + "/Herren_C/spiel1").body();
            String d1 = get(live + "/Damen_B/spiel1").body();
            assertTrue(m1.contains("Spiel 1") && m1.contains("Herren C") && m1.contains("Link kopieren"));
            assertTrue(d1.contains("Spiel 1") && d1.contains("Damen B") && !d1.contains("Herren C ·"), "gleiche Nummer, andere Klasse");
            assertTrue(m1.contains("Verein "), "Verein der Spieler");
            assertTrue(get(live + "/Herren_C/spiel99").body().contains("Spiel nicht gefunden"));
            assertTrue(get(live + "/Gibtsnicht/spiel1").body().contains("Spiel nicht gefunden"));
            assertFalse(m1.contains("Vorname1 Nachname1"), "Vornamen abgekürzt");

            // #150/#151: Ausrichter und Turnierwelt auf Start- und Turnierseite
            String start = get(s.base() + "/").body();
            assertTrue(start.contains("TTC Altona e.V. &amp; Freunde"), "Ausrichter auf der Startseite");
            assertTrue(start.contains("https://www.mytischtennis.de/turnierwelt/12345") && start.contains("zur Turnierwelt"));
            assertTrue(start.contains("rel=\"noopener\""));
            String ov = get(live).body();
            assertTrue(ov.contains("TTC Altona e.V. &amp; Freunde") && ov.contains("zur Turnierwelt"), "Kopf der Turnierseite");

            // #149: Logo-Adressen mit Version, Zwischenspeicher passend
            assertTrue(start.contains("/favicon.png?v=" + de.hts.HtsApp.VERSION) && ov.contains("hts-logo.png?v=" + de.hts.HtsApp.VERSION));
            assertEquals("max-age=604800", get(s.base() + "/favicon.png?v=" + de.hts.HtsApp.VERSION).headers().firstValue("Cache-Control").orElse(""));
            assertEquals("max-age=3600", get(s.base() + "/favicon.png").headers().firstValue("Cache-Control").orElse(""));

            // #153: Spenden mit Vorgabetext, Datei im Konfigurationsordner hat Vorrang; Link in der Fußzeile
            assertTrue(start.contains("href=\"/spenden\""));
            assertTrue(get(s.base() + "/spenden").body().contains("Herzensprojekt") && get(s.base() + "/spenden").body().contains("gofundme.com"));
            Files.writeString(s.content().resolve("spenden.html"), "<p>Eigener Spendentext</p>");
            assertTrue(get(s.base() + "/spenden").body().contains("Eigener Spendentext"));

            // „Nur Endergebnis“: Spielseiten gesperrt, Turnier unter „Vergangene Turniere“
            HttpRequest post = HttpRequest.newBuilder(URI.create(s.base() + "/api/turniere/sommer1/nur-endergebnis"))
                    .header("Authorization", "Bearer " + KEY).POST(HttpRequest.BodyPublishers.noBody()).build();
            assertEquals(200, raw.send(post, HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(302, raw.send(HttpRequest.newBuilder(URI.create(live + "/Herren_C/spiel1")).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode(), "Spielseite nach „Nur Endergebnis“ gesperrt");
            assertTrue(get(s.base() + "/vergangen").body().contains("Sommerturnier") && get(s.base() + "/vergangen").body().contains("Endergebnis"));
            assertFalse(get(s.base() + "/").body().contains("Sommerturnier"));
        }
    }

    @Test
    void startseiteGeteiltInAktuelleUndVergangeneTurniere() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        assertFalse(LiveHost.isPast("läuft", false, today, today));
        assertFalse(LiveHost.isPast("angekündigt", false, today.plusDays(4), today));
        assertFalse(LiveHost.isPast("angekündigt", false, null, today), "ohne Datum: aktuell");
        assertTrue(LiveHost.isPast("beendet", false, today.plusDays(1), today), "beendet = vergangen");
        assertTrue(LiveHost.isPast("läuft", false, today.minusDays(1), today), "Datum vor heute = vergangen");
        assertTrue(LiveHost.isPast("läuft", true, today, today), "nur Endergebnis = vergangen");
    }

    @Test
    void spielAdresseMitPhaseUndSonderzeichen() {
        assertEquals("Herren_C", LiveServer.classSegment("Herren  C"));
        var m = LiveServer.MATCH_PATH.matcher("/live/Jungen_U15/phase2/spiel12");
        assertTrue(m.matches());
        assertEquals("Jungen_U15", m.group(1));
        assertEquals("2", m.group(2));
        assertEquals("12", m.group(3));
        var m2 = LiveServer.MATCH_PATH.matcher("/live/Mädchen_A/spiel3");
        assertTrue(m2.matches() && m2.group(2) == null && m2.group(1).equals("Mädchen_A"));
        assertFalse(LiveServer.MATCH_PATH.matcher("/live/tische").matches());
        assertEquals("TTC%20%26%20Co", Publisher.organizerHeader("TTC & Co").replace("+", "%20"));
        assertNull(Publisher.organizerHeader("  "));
    }

    @Test
    void turnierweltNurMitHttpsUndNeueFelderInDerTurnierdatei() throws Exception {
        assertEquals("https://www.mytischtennis.de/x", Tournament.validWorldUrl(" https://www.mytischtennis.de/x "));
        assertNull(Tournament.validWorldUrl("http://www.mytischtennis.de/x"));
        assertNull(Tournament.validWorldUrl("javascript:alert(1)"));
        assertNull(Tournament.validWorldUrl("https://"));
        Tournament t = tournament("Dateitest", LocalDate.of(2026, 12, 6), "Herren C");
        t.setWorldUrl("https://www.mytischtennis.de/turnierwelt/1");
        t.setPublishedOnce(true);
        t.setFlatLayout(true);
        t.setPublishAbbrev(false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        assertEquals("https://www.mytischtennis.de/turnierwelt/1", back.getWorldUrl());
        assertTrue(back.isPublishedOnce() && back.isFlatLayout());
        assertTrue(back.isPublishAbbrev(), "#156: Vornamen auf dem Webserver immer abgekürzt");
    }

    @Test
    void neueAblageVeroeffentlichenUndExportnamen() throws Exception {
        Path old = Files.createTempDirectory("hts-alt"), flat = Files.createTempDirectory("hts-neu");
        Path prog = Files.createTempDirectory("hts-programm");
        ProgramFolder.useForTest(prog);
        ProgramFolder.choose(ProgramFolder.KEY_PDF, flat.resolve("Export files"));
        var store = de.hts.persistence.TournamentStoreAccess.withFlatDir(old, flat);
        Tournament a = store.create("Herbstturnier", LocalDate.of(2026, 11, 28), 8, null, false, false);
        Tournament b = store.create("Herbstturnier", LocalDate.of(2026, 11, 28), 8, null, true, false);
        assertTrue(Files.exists(flat.resolve("Herbstturnier.hts")), "Turnierdatei direkt im HTS-Ordner");
        assertTrue(Files.exists(flat.resolve("Herbstturnier (2).hts")), "gleicher Name → (2)");
        assertTrue(a.isFlatLayout() && a.isPublish(), "#156: neues Turnier wird veröffentlicht");
        assertFalse(b.isPublish(), "Testturnier nicht automatisch veröffentlicht");
        assertEquals(2, store.list().size(), "beide werden gefunden");
        Path pdf = StorageLocations.pdfDir(a);
        assertEquals(flat.resolve("Export files"), pdf);
        String n1 = StorageLocations.exportFileName(a, pdf, "Endergebnis Herren C.pdf");
        assertEquals("Herbstturnier 2026-11-28 - Endergebnis Herren C.pdf", n1);
        Files.createDirectories(pdf);
        Files.writeString(pdf.resolve(n1), "x");
        assertEquals("Herbstturnier 2026-11-28 - Endergebnis Herren C (2).pdf", StorageLocations.exportFileName(a, pdf, "Endergebnis Herren C.pdf"));
        Tournament legacy = new Tournament();
        legacy.setName("Alt");
        assertEquals("Endergebnis.pdf", StorageLocations.exportFileName(legacy, pdf, "Endergebnis.pdf"), "alte Ablage: Name unverändert");
    }

    @Test
    void programmEinstellungenDunkelmodusUndAeltereVersion() throws Exception {
        Path prog = Files.createTempDirectory("hts-programm"), older = Files.createTempDirectory("hts-alt");
        ProgramFolder.useForTest(prog);
        assertFalse(ProgramFolder.isDarkMode());
        ProgramFolder.setDarkMode(true);
        ProgramFolder.useForTest(prog);                          // neu laden
        assertTrue(ProgramFolder.isDarkMode(), "Dunkelmodus wird gemerkt");
        assertNull(ProgramFolder.chosen(ProgramFolder.KEY_HTS));
        Files.writeString(older.resolve("ausrichter.properties"), "name=TSV Test\n");
        Files.writeString(older.resolve("programm.properties"), "dark=false\ndir-hts=/tmp/hts-x\n");
        List<String> done = ProgramFolder.carryOverFrom(older);
        assertEquals(List.of("ausrichter.properties", "programm.properties"), done);
        assertFalse(ProgramFolder.isDarkMode());
        assertEquals(Path.of("/tmp/hts-x"), ProgramFolder.htsDir());
        Path fresh = Files.createTempDirectory("hts-programm2"), oldStore = Files.createTempDirectory("hts-ablage");
        Files.writeString(oldStore.resolve("ausrichter.properties"), "name=Alt\n");
        ProgramFolder.useForTest(fresh);
        ProgramFolder.migrateFrom(oldStore);
        assertTrue(Files.exists(fresh.resolve("ausrichter.properties")), "Ausrichter aus der alten Ablage übernommen");
        assertTrue(Files.readString(Path.of("src/main/resources/de/hts/ui/hts-dunkel.css")).contains("-fx-background"), "Stildatei vorhanden");
    }

    @Test
    void loeschauftraegeWerdenGemerktWennDerServerFehlt() throws Exception {
        Path prog = Files.createTempDirectory("hts-programm");
        ProgramFolder.useForTest(prog);
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("hts-test-180-" + System.nanoTime());
        byte[] key = SecretStore.newKey();
        WebSettings.useForTest(p, () -> key);
        try {
            assertNotNull(PendingDeletes.deleteNow("abc123"), "ohne Webserver: gemerkt");
            assertTrue(PendingDeletes.list().contains("abc123"));
            PendingDeletes.add("../boese");
            assertFalse(PendingDeletes.list().contains("../boese"), "nur gültige Kennungen");
            try (Srv s = server()) {
                WebSettings.set(s.base(), KEY);
                assertNull(PendingDeletes.deleteNow("abc123"), "mit Webserver: gelöscht");
                assertFalse(PendingDeletes.list().contains("abc123"));
            }
        } finally {
            p.removeNode();
        }
    }
}
