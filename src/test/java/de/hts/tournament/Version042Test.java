/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.persistence.TournamentFile;
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.web.LiveServer;
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

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.42.0 – Geschlecht beim Import (#71), Meldung bleibt beim Zurückziehen (#75), AGPL-Hinweise (#73). */
class Version042Test {

    @Test
    void sexNullIstWeiblich() throws Exception {
        assertEquals(Sex.FEMALE, Sex.fromClickTt(0), "click-TT: sex=\"0\" = weiblich");
        assertEquals(Sex.FEMALE, Sex.fromClickTt(2), "ältere Beispieldateien: 2 = weiblich");
        assertEquals(Sex.MALE, Sex.fromClickTt(1));
        assertEquals(Sex.UNKNOWN, Sex.fromClickTt(null));
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <tournament name="T">
                  <competition age-group="Mädchen 13" type="Einzel" sex="weiblich">
                    <players>
                      <player id="P1" type="single"><person firstname="Linh" lastname="Dao" sex="0" birthyear="2014" club-name="WTB" internal-nr="NU1"/></player>
                      <player id="P2" type="single"><person firstname="Emil" lastname="Co" sex="1" birthyear="2014" club-name="GFS" internal-nr="NU2"/></player>
                    </players>
                  </competition>
                </tournament>
                """;
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(Sex.FEMALE, r.players().stream().filter(p -> p.getLastname().equals("Dao")).findFirst().orElseThrow().getSex());
        assertEquals(Sex.MALE, r.players().stream().filter(p -> p.getLastname().equals("Co")).findFirst().orElseThrow().getSex());
    }

    @Test
    void zurueckziehenBehaeltMeldungUndAnwesenheit() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = t.addClass(new PlayingClass("Mädchen 13 Einzel"));
        Player gem = new Player("Linh", "Dao");
        gem.setInternalNr("NU1");
        gem.addRegistration(c.getName());
        Player nach = new Player("Mia", "Nach");
        nach.setInternalNr("NU2");
        nach.addWaitingList(c.getName());
        Player hand = new Player("Hand", "Erfasst");
        hand.setInternalNr("NU3");
        t.merge(java.util.List.of(gem, nach, hand));
        gem = t.getPlayers().get(0);
        nach = t.getPlayers().get(1);
        hand = t.getPlayers().get(2);
        t.setAttendance(gem, Attendance.PRESENT);
        t.setAttendance(hand, Attendance.PRESENT);
        t.assign(gem, c);
        t.assign(nach, c);                       // Nachrücker übernommen → gilt als angemeldet
        t.assign(hand, c);
        assertTrue(gem.getRegistrations().isEmpty() && nach.getWaitingLists().isEmpty());

        // Speichern und Laden: die Herkunft bleibt erhalten
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("from=\"registration\""));
        Tournament t2 = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass c2 = t2.findClass(c.getName()).orElseThrow();
        Player g2 = t2.getPlayers().stream().filter(p -> p.getLastname().equals("Dao")).findFirst().orElseThrow();
        Player n2 = t2.getPlayers().stream().filter(p -> p.getLastname().equals("Nach")).findFirst().orElseThrow();
        Player h2 = t2.getPlayers().stream().filter(p -> p.getLastname().equals("Erfasst")).findFirst().orElseThrow();

        t2.unassign(g2, c2);
        assertEquals(Attendance.PRESENT, g2.getAttendance(), "bleibt angemeldet");
        assertTrue(g2.getRegistrations().contains(c2.getName()), "wieder gemeldet");
        assertTrue(g2.getClassesText().contains("gemeldet: " + c2.getName()));
        assertTrue(t2.attendanceProblem(c2) == null || !t2.attendanceProblem(c2).isEmpty());
        t2.unassign(n2, c2);
        assertTrue(n2.getWaitingLists().contains(c2.getName()), "wieder Nachrücker");
        assertTrue(n2.isReserveOnly());
        t2.unassign(h2, c2);
        assertTrue(h2.getRegistrations().isEmpty() && h2.getWaitingLists().isEmpty(), "von Hand Zugeordnete: nur raus");
        assertEquals(Attendance.PRESENT, h2.getAttendance());

        // erneut übernehmen und zurückziehen: wieder gemeldet
        t2.assign(g2, c2);
        t2.unassign(g2, c2);
        assertTrue(g2.getRegistrations().contains(c2.getName()));
    }

    @Test
    void doppelZurueckziehenBehaeltMeldung() {
        Tournament t = new Tournament();
        PlayingClass d = t.addClass(new PlayingClass("Damen Doppel"));
        t.changeDiscipline(d, Discipline.DOUBLES);
        Player a = new Player("A", "Eins"), b = new Player("B", "Zwei");
        a.setInternalNr("NU1");
        b.setInternalNr("NU2");
        a.addRegistration(d.getName());
        b.addRegistration(d.getName());
        t.merge(java.util.List.of(a, b));
        a = t.getPlayers().get(0);
        b = t.getPlayers().get(1);
        DoublesPair pair = t.createPair(d, a, b);
        t.removeFromPair(pair, a);
        assertTrue(a.getRegistrations().contains(d.getName()), "aus dem Doppel genommen: wieder gemeldet");
        t.removePair(t.pairOf(b, d).orElseThrow());
        assertTrue(b.getRegistrations().contains(d.getName()));
    }

    @Test
    void handySeitenZeigenQuellcodeLink() throws Exception {
        Tournament t = new Tournament();
        LiveServer server = new LiveServer(t, Runnable::run, () -> { }, 0);
        server.start();
        try {
            HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
            String base = "http://127.0.0.1:" + server.port();
            for (String path : new String[]{"/live", "/anmeldung/" + t.getCheckinToken()}) {
                String body = http.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString()).body();
                assertTrue(body.contains("Quellcode (AGPL-3.0)") && body.contains(de.hts.HtsApp.REPO_URL), path);
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void lizenzDateien() throws Exception {
        String license = Files.readString(Path.of("LICENSE"));
        assertTrue(license.contains("GNU AFFERO GENERAL PUBLIC LICENSE") && license.contains("Version 3, 19 November 2007"));
        String notice = Files.readString(Path.of("NOTICE"));
        assertTrue(notice.startsWith("HTS - Hamburger Turniersoftware is an application for executing tournaments"));
        assertTrue(notice.contains("Copyright (C) 2026  " + de.hts.HtsApp.AUTHOR));
        assertTrue(Files.readString(Path.of("README.md")).contains(notice.strip()), "README enthält den Lizenzhinweis");
    }
}
