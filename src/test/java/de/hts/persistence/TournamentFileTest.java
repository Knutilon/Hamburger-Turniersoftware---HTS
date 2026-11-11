/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentFileTest {

    private static List<Player> samplePlayers() throws Exception {
        try (InputStream in = TournamentFileTest.class.getResourceAsStream("/clicktt-beispiel.xml")) {
            return new ClickTtXmlImporter().importStream(in).players();
        }
    }

    @Test
    void speichernUndLadenErhaeltAlleDaten() throws Exception {
        List<Player> original = samplePlayers();
        Player manual = new Player("Ohne", "Meldung");
        manual.setBirthdate(LocalDate.of(2010, 5, 1));
        manual.setSex(Sex.FEMALE);
        manual.addWaitingList("Damen Einzel");
        original.add(manual);

        Path dir = Files.createTempDirectory("hts-test");
        Path file = dir.resolve("Testturnier.hts");
        try {
            TournamentFile.save(file, "Testturnier Ü", original);
            TournamentFile.Content loaded = TournamentFile.load(file);

            assertEquals("Testturnier Ü", loaded.tournamentName());
            assertEquals(original.size(), loaded.players().size());
            for (Player o : original) {
                Player l = loaded.players().stream().filter(p -> p.identityKey().equals(o.identityKey()))
                        .findFirst().orElseThrow(() -> new AssertionError("fehlt: " + o));
                assertEquals(o.getFirstname(), l.getFirstname());
                assertEquals(o.getLastname(), l.getLastname());
                assertEquals(o.getBirthyear(), l.getBirthyear());
                assertEquals(o.getBirthdate(), l.getBirthdate());
                assertEquals(o.getSex(), l.getSex());
                assertEquals(o.getClubName(), l.getClubName());
                assertEquals(o.getClubNr(), l.getClubNr());
                assertEquals(o.getInternalNr(), l.getInternalNr());
                assertEquals(o.getLicenceNr(), l.getLicenceNr());
                assertEquals(o.getTtr(), l.getTtr());
                assertEquals(o.getTtrMatchCount(), l.getTtrMatchCount());
                assertEquals(o.getNationality(), l.getNationality());
                assertEquals(o.getClassNames(), l.getClassNames());
                assertEquals(o.getWaitingLists(), l.getWaitingLists());
            }
            // keine temporären Dateien zurückgelassen
            try (var files = Files.list(dir)) {
                assertEquals(1, files.count());
            }
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void ueberschreibtVorhandeneDatei() throws Exception {
        Path dir = Files.createTempDirectory("hts-test");
        Path file = dir.resolve("t.hts");
        try {
            TournamentFile.save(file, "A", samplePlayers());
            TournamentFile.save(file, "B", List.of(new Player("Nur", "Einer")));
            var loaded = TournamentFile.load(file);
            assertEquals("B", loaded.tournamentName());
            assertEquals(1, loaded.players().size());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void turnierdatenUndKlassenWerdenGespeichert() throws Exception {
        de.hts.tournament.Tournament t = new de.hts.tournament.Tournament();
        t.setName("Jugendturnier");
        t.setDate(LocalDate.of(2026, 11, 14));
        t.setLocation("Sporthalle Elbufer");
        t.merge(samplePlayers());
        var u15 = t.addClass(new de.hts.tournament.PlayingClass("Jugend U15", de.hts.tournament.ClassType.YOUTH, 2012, null));
        t.addClass(new de.hts.tournament.PlayingClass("Senioren", de.hts.tournament.ClassType.ADULTS, null, 1986));
        t.addClass(new de.hts.tournament.PlayingClass("Mädchen", de.hts.tournament.ClassType.GIRLS, null, null));
        t.addClass(new de.hts.tournament.PlayingClass("Damen B", de.hts.tournament.ClassType.WOMEN, null, null));
        Player first = t.getPlayers().get(0);
        t.assign(first, u15);

        Path dir = Files.createTempDirectory("hts-test");
        Path file = dir.resolve("t.hts");
        try {
            TournamentFile.save(file, t);
            var c = TournamentFile.load(file);
            assertEquals("Jugendturnier", c.tournamentName());
            assertEquals(LocalDate.of(2026, 11, 14), c.date());
            assertEquals("Sporthalle Elbufer", c.location());

            de.hts.tournament.Tournament loaded = new de.hts.tournament.Tournament();
            loaded.replaceAll(c.tournamentName(), c.date(), c.location(), c.classes(), c.players());
            assertFalse(loaded.isModified());
            assertEquals(t.getClasses().size(), loaded.getClasses().size());
            var lu15 = loaded.findClass("Jugend U15").orElseThrow();
            assertEquals(de.hts.tournament.ClassType.YOUTH, lu15.getType());
            assertEquals(de.hts.tournament.ClassType.GIRLS, loaded.findClass("Mädchen").orElseThrow().getType());
            assertEquals(de.hts.tournament.ClassType.WOMEN, loaded.findClass("Damen B").orElseThrow().getType());
            assertEquals(de.hts.tournament.ClassType.ADULTS, loaded.findClass("Senioren").orElseThrow().getType());
            assertEquals(2012, lu15.getBirthyearFrom());
            assertEquals(null, lu15.getBirthyearTo());
            assertEquals(1986, loaded.findClass("Senioren").orElseThrow().getBirthyearTo());
            assertEquals(1, loaded.countIn(lu15));
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void alteDateiOhneKlassenWirdGelesenKlassenAusMeldungen() throws Exception {
        String v1 = """
                <hts-tournament format-version="1" name="Alt">
                  <players>
                    <player firstname="A" lastname="B" internal-nr="NU1"><entry competition="Herren Einzel" registered="true"/></player>
                  </players>
                </hts-tournament>""";
        var c = TournamentFile.read(stream(v1));
        assertTrue(c.classes().isEmpty());
        de.hts.tournament.Tournament t = new de.hts.tournament.Tournament();
        t.replaceAll(c.tournamentName(), c.date(), c.location(), c.classes(), c.players());
        assertTrue(t.findClass("Herren Einzel").isPresent());
        assertEquals(1, t.countIn(t.findClass("Herren Einzel").get()));
    }

    @Test
    void klassenAusHts030MitGeschlechtWerdenUmgesetzt() throws Exception {
        String v030 = """
                <hts-tournament format-version="2" name="Alt">
                  <classes><class name="Damen" sex="w"/><class name="Herren" sex="m"/><class name="Offen"/></classes>
                  <players/>
                </hts-tournament>""";
        var c = TournamentFile.read(stream(v030));
        assertEquals(de.hts.tournament.ClassType.WOMEN, c.classes().get(0).getType());
        assertEquals(de.hts.tournament.ClassType.ADULTS, c.classes().get(1).getType());
        assertEquals(de.hts.tournament.ClassType.ADULTS, c.classes().get(2).getType());
    }

    @Test
    void alteWartelistenHakenWerdenZuVerfuegbar() throws Exception {
        String v3 = """
                <hts-tournament format-version="3" name="Alt">
                  <players>
                    <player firstname="A" lastname="B" internal-nr="NU1">
                      <entry competition="Herren Einzel" registered="true"/>
                      <entry competition="Herren Doppel" registered="false"/>
                    </player>
                  </players>
                </hts-tournament>""";
        Player p = TournamentFile.read(stream(v3)).players().get(0);
        assertEquals(java.util.Set.of("Herren Einzel"), p.getClassNames());
        assertEquals(java.util.Set.of("Herren Doppel"), p.getWaitingLists());
    }

    @Test
    void fremdeDateiWirdAbgelehnt() {
        var ex = assertThrows(IOException.class, () -> TournamentFile.read(stream("<tournament/>")));
        assertTrue(ex.getMessage().contains("keine HTS-Turnierdatei"));
    }

    @Test
    void neueresFormatWirdAbgelehnt() {
        var ex = assertThrows(IOException.class,
                () -> TournamentFile.read(stream("<hts-tournament format-version=\"99\"/>")));
        assertTrue(ex.getMessage().contains("neueren HTS-Version"));
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
