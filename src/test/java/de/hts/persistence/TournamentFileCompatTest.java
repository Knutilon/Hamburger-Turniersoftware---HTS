/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 0.42.0 (#76): Turnierdateien bleiben lesbar. Für jede veröffentlichte Version liegt eine Beispiel-Turnierdatei
 * {@code turnierdatei-<Version>.hts} samt erwarteten Werten ({@code .properties}) unter src/test/resources; die aktuelle
 * Version muss jede davon vollständig öffnen – und nach erneutem Speichern unverändert wieder lesen.
 */
class TournamentFileCompatTest {

    @Test
    void alleBeispielTurnierdateienLassenSichOeffnen() throws Exception {
        List<Path> files = fixtures();
        assertFalse(files.isEmpty(), "keine Beispiel-Turnierdateien gefunden");
        for (Path f : files) {
            Properties exp = new Properties();
            try (java.io.Reader in = Files.newBufferedReader(Path.of(f.toString().replace(".hts", ".properties")),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                exp.load(in);
            }
            Tournament t = TournamentFile.load(f).toTournament();
            check(f.getFileName() + "", t, exp);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            TournamentFile.write(out, TournamentFile.Content.of(t));
            Tournament again = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
            check(f.getFileName() + " (neu gespeichert)", again, exp);
        }
    }

    @Test
    void programmversionWirdMitgespeichert() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(new Tournament()));
        assertTrue(out.toString(java.nio.charset.StandardCharsets.UTF_8).contains("hts-version=\"" + de.hts.HtsApp.VERSION + "\""));
    }

    private static void check(String name, Tournament t, Properties exp) {
        assertEquals(exp.getProperty("classes"), String.valueOf(t.getClasses().size()), name + ": Klassen");
        assertEquals(exp.getProperty("classNames"),
                String.join("|", t.getClasses().stream().map(PlayingClass::getName).toList()), name + ": Klassennamen");
        assertEquals(exp.getProperty("players"), String.valueOf(t.getPlayers().size()), name + ": Personen");
        assertEquals(exp.getProperty("matches"), String.valueOf(t.getMatches().size()), name + ": Spiele");
        assertEquals(exp.getProperty("completed"),
                String.valueOf(t.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.DONE).count()), name + ": abgeschlossen");
        assertEquals(exp.getProperty("present"), String.valueOf(t.getPlayers().stream().filter(Player::isPresent).count()), name + ": angemeldet");
        assertEquals(exp.getProperty("withdrawn"),
                String.valueOf(t.getPlayers().stream().filter(p -> p.getAttendance() == Attendance.WITHDRAWN).count()), name + ": abgemeldet");
        assertEquals(exp.getProperty("registrations"),
                String.valueOf(t.getPlayers().stream().mapToInt(p -> p.getRegistrations().size()).sum()), name + ": Meldungen");
        assertEquals(exp.getProperty("waitingLists"),
                String.valueOf(t.getPlayers().stream().mapToInt(p -> p.getWaitingLists().size()).sum()), name + ": Nachrücker");
        assertEquals(exp.getProperty("pairs"), String.valueOf(t.getPairs().size()), name + ": Doppel");
        assertEquals(exp.getProperty("tableName1"), t.tableName(1), name + ": Tischname");
        StringBuilder sets = new StringBuilder();
        for (Match m : t.getMatches()) {
            if (m.getStatus() == MatchStatus.DONE) sets.append(m.getNumber()).append(':').append(m.getSetsText()).append(';');
        }
        assertEquals(exp.getProperty("sets"), sets.toString(), name + ": Ergebnisse");
    }

    private static List<Path> fixtures() throws URISyntaxException, java.io.IOException {
        Path dir = Path.of(TournamentFileCompatTest.class.getResource("/turnierdatei-0.41.0.hts").toURI()).getParent();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().matches("turnierdatei-.*\\.hts")).sorted().toList();
        }
    }
}
