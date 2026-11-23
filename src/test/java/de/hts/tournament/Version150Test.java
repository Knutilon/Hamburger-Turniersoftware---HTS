/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import com.sun.net.httpserver.HttpServer;
import de.hts.persistence.Organizer;
import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.web.Publisher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.5.0 – Veröffentlichen auf dem Webserver (#135–#138, gegen einen Testempfänger), letztes Ergebnis zurücknehmen
 * (#131), gedruckte Urkunden (#132), Ausrichter (#139).
 */
class Version150Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static PlayingClass ko(Tournament t, String name, int n) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("Vorname" + i, "Nachname" + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - 10 * i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        return c;
    }

    /** Kleiner Testempfänger: zählt Anfragen, kann „ausfallen“. */
    static final class Receiver implements AutoCloseable {
        final HttpServer server;
        final List<String> log = Collections.synchronizedList(new ArrayList<>());
        volatile boolean down;
        volatile byte[] lastBody;

        Receiver() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", ex -> {
                byte[] body = ex.getRequestBody().readAllBytes();
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                int code = down ? 503 : !"Bearer geheim".equals(auth) ? 401 : 200;
                log.add(ex.getRequestMethod() + " " + ex.getRequestURI().getPath() + " " + code);
                if (code == 200 && body.length > 0 && ex.getRequestURI().getPath().matches("/api/turniere/[^/]+")) lastBody = body;
                ex.sendResponseHeaders(code, -1);
                ex.close();
            });
            server.start();
        }

        String base() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        long count(String prefix) {
            synchronized (log) {
                return log.stream().filter(s -> s.startsWith(prefix)).count();
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static void waitFor(java.util.function.BooleanSupplier c, long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (!c.getAsBoolean() && System.currentTimeMillis() < end) Thread.sleep(20);
    }

    @Test
    void hochladenGedrosseltMitSchluesselUndLogo() throws Exception {
        try (Receiver r = new Receiver()) {
            Tournament t = new Tournament();
            t.setName("Live-Test");
            byte[] logo = {1, 2, 3};
            try (Publisher p = new Publisher(r.base(), "geheim", "abc", Runnable::run, () -> TournamentFile.publishBytes(t), () -> logo, 400, 300)) {
                waitFor(() -> p.status().state() == Publisher.State.OK, 3000);
                assertEquals(Publisher.State.OK, p.status().state());
                assertEquals(1, r.count("PUT /api/turniere/abc/logo 200"), "Logo einmal");
                assertEquals(1, r.count("PUT /api/turniere/abc 200"));
                for (int i = 0; i < 20; i++) {                      // viele Änderungen kurz hintereinander
                    p.changed();
                    Thread.sleep(10);
                }
                Thread.sleep(700);
                long puts = r.count("PUT /api/turniere/abc 200");
                assertTrue(puts >= 2 && puts <= 3, "gedrosselt statt 20 Uploads: " + puts);
                Tournament back = TournamentFile.read(new ByteArrayInputStream(r.lastBody)).toTournament();
                assertEquals("Live-Test", back.getName());
            }
        }
    }

    @Test
    void funklochWirdNachgeholt() throws Exception {
        try (Receiver r = new Receiver()) {
            r.down = true;
            Tournament t = new Tournament();
            try (Publisher p = new Publisher(r.base(), "geheim", "x1", Runnable::run, () -> TournamentFile.publishBytes(t), () -> null, 100, 200)) {
                waitFor(() -> p.status().state() == Publisher.State.ERROR, 3000);
                assertEquals(Publisher.State.ERROR, p.status().state());
                assertTrue(p.status().message().contains("keine Verbindung"));
                r.down = false;
                waitFor(() -> p.status().state() == Publisher.State.OK, 3000);
                assertEquals(Publisher.State.OK, p.status().state(), "nach dem Funkloch nachgeholt");
            }
        }
    }

    @Test
    void falscherSchluesselUndBefehle() throws Exception {
        try (Receiver r = new Receiver()) {
            assertNull(Publisher.test(r.base(), "geheim"));
            assertTrue(Publisher.test(r.base(), "falsch").contains("Schlüssel abgelehnt"));
            assertNotNull(Publisher.test("http://127.0.0.1:1", "geheim"));
            try (Publisher p = new Publisher(r.base(), "geheim", "t9", x -> { }, () -> new byte[0], () -> null, Long.MAX_VALUE, Long.MAX_VALUE)) {
                p.deleteOnServer();
                p.keepOnlyFinalResults();
            }
            assertEquals(1, r.count("DELETE /api/turniere/t9 200"));
            assertEquals(1, r.count("POST /api/turniere/t9/nur-endergebnis 200"));
            assertEquals("https://live.example.de", Publisher.normalize("live.example.de/"));
            assertEquals("https://live.example.de/live/t9", Publisher.publicUrl("live.example.de", "t9"));
        }
    }

    @Test
    void vornamenAbkuerzenNurInDerKopie() throws Exception {
        Tournament t = new Tournament();
        ko(t, "Jungen 19", 4);
        byte[] raw = TournamentFile.publishBytes(t);
        Tournament pub = TournamentFile.read(new ByteArrayInputStream(TournamentFile.abbreviateFirstNames(raw))).toTournament();
        assertTrue(pub.getPlayers().stream().allMatch(p -> p.getFirstname().equals("V.")));
        assertTrue(t.getPlayers().stream().allMatch(p -> p.getFirstname().startsWith("Vorname")), "Turnier selbst unverändert");
    }

    @Test
    void veroeffentlichenSchalterInDerTurnierdatei() throws Exception {
        Tournament t = new Tournament();
        assertFalse(roundTrip(t).isPublish());
        t.setPublish(true);
        t.setPublishAbbrev(true);
        Tournament back = roundTrip(t);
        assertTrue(back.isPublish());
        assertTrue(back.isPublishAbbrev());
    }

    @Test
    void letztesErgebnisZuruecknehmen() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = ko(t, "Jungen 19", 8);
        assertNull(t.undoableResult());
        Match m = t.matchesOf(c).stream().filter(x -> x.isReady() && x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        Integer table = m.getTable();
        t.recordResult(m, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        assertSame(m, t.undoableResult());
        t.undoLastResult();
        assertEquals(MatchStatus.ACTIVE, m.getStatus(), "läuft wieder");
        assertEquals(table, m.getTable(), "am alten Tisch");
        assertTrue(m.getPoints().isEmpty());
        assertNull(t.undoableResult(), "nur einmal");
        // danach weiter gespielt -> nicht mehr zurücknehmbar
        t.recordResult(m, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        // Spiel aus der anderen Hälfte: sein Folgespiel wartet noch auf den zweiten Gegner -> zurücknehmbar
        Match other = t.matchesOf(c).stream().filter(x -> x.getKoRound() == m.getKoRound() && x.getKoIndex() / 2 != m.getKoIndex() / 2
                && x.getStatus() == MatchStatus.ACTIVE).findFirst().orElseThrow();
        t.recordResult(other, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        assertSame(other, t.undoableResult());
        // Partner im selben Halbfinal-Zweig: danach steht das Folgespiel am Tisch -> nicht mehr zurücknehmbar
        Match partner = t.matchesOf(c).stream().filter(x -> x != m && x.getKoRound() == m.getKoRound() && x.getKoIndex() / 2 == m.getKoIndex() / 2)
                .findFirst().orElseThrow();
        t.recordResult(partner, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        assertNull(t.undoableResult(), "Folgespiel läuft schon");
    }

    @Test
    void gedruckteUrkundenWerdenGemerkt() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = ko(t, "Jungen 19", 4);
        for (int k = 0; k < 10; k++) {
            Match o = t.matchesOf(c).stream().filter(m -> m.isReady() && m.getStatus() != MatchStatus.DONE).findFirst().orElse(null);
            if (o == null) break;
            t.recordResult(o, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        }
        t.finishClass(c);
        List<Tournament.CertEntry> es = t.certificateEntries();
        t.markCertPrinted(List.of(es.get(0).key(), es.get(1).key()));
        Tournament back = roundTrip(t);
        assertTrue(back.isCertPrinted(es.get(0).key()));
        assertTrue(back.isCertPrinted(es.get(1).key()));
        assertFalse(back.isCertPrinted(es.get(2).key()));
    }

    @Test
    void ausrichterNameUndLogo() throws Exception {
        Path dir = Files.createTempDirectory("ausrichter");
        Organizer o = Organizer.load(dir);
        assertEquals(Organizer.DEFAULT_NAME, o.getName());
        assertEquals(Organizer.LogoSource.DEFAULT, o.getLogoSource());
        assertNotNull(o.logoPng(), "Standard-Logo mitgeliefert");
        o.setName("TTC Musterstadt");
        o.setCustomLogo(new byte[]{9, 8, 7});
        Organizer again = Organizer.load(dir);
        assertEquals("TTC Musterstadt", again.getName());
        assertArrayEquals(new byte[]{9, 8, 7}, again.logoPng());
        again.useNoLogo();
        assertNull(Organizer.load(dir).logoPng());
        Organizer.load(null);                                   // für andere Tests: Vorgaben
    }
}
