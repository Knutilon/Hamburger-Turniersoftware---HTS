/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import de.hts.player.Player;
import de.hts.tournament.ClassType;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentStoreTest {

    private static Path tempDir() throws IOException {
        return Files.createTempDirectory("hts-store");
    }

    @Test
    void mehrereTurniereAnlegenUndAuflisten() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir.resolve("Turniere"));
            assertTrue(store.list().isEmpty(), "Ordner existiert noch nicht");

            Tournament a = store.create("Herbstturnier", LocalDate.of(2026, 10, 3));
            Tournament b = store.create("Wintercup", LocalDate.of(2026, 12, 12));
            assertNotEquals(a.getId(), b.getId());
            assertFalse(a.isModified());

            List<TournamentStore.Summary> list = store.list();
            assertEquals(2, list.size());
            assertEquals("Wintercup", list.get(0).name(), "neuestes Datum zuerst");
            assertEquals("Herbstturnier", list.get(1).name());
            assertFalse(list.get(0).isStarted());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void teilnehmendeUndKlassenGehoerenZumTurnier() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir);
            Tournament a = store.create("A", LocalDate.of(2026, 1, 1));
            store.create("B", LocalDate.of(2026, 2, 1));

            a.getPlayers().add(new Player("Lea", "Schäfer"));
            a.addClass(new PlayingClass("Damen", ClassType.WOMEN, null, null));
            assertTrue(a.isModified());
            store.save(a);
            assertFalse(a.isModified());

            var summaries = store.list();
            var sa = summaries.stream().filter(s -> s.name().equals("A")).findFirst().orElseThrow();
            var sb = summaries.stream().filter(s -> s.name().equals("B")).findFirst().orElseThrow();
            assertEquals(1, sa.participants());
            assertEquals(1, sa.classes());
            assertEquals(0, sb.participants());
            assertEquals(0, sb.classes());

            Tournament loaded = store.load(sa);
            assertEquals(a.getId(), loaded.getId());
            assertEquals("Schäfer", loaded.getPlayers().get(0).getLastname());
            assertFalse(loaded.isModified());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void turnierStartenHaeltStartzeitFest() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir);
            Tournament t = store.create("Cup", LocalDate.of(2026, 11, 14));
            assertFalse(t.isStarted());
            LocalDateTime before = LocalDateTime.now().withNano(0);
            t.start();
            assertTrue(t.isStarted());
            assertFalse(t.getStartedAt().isBefore(before));
            assertThrows(IllegalStateException.class, t::start, "nur einmal startbar");
            store.save(t);

            var s = store.list().get(0);
            assertTrue(s.isStarted());
            assertEquals(t.getStartedAt(), s.startedAt());
            assertEquals(t.getStartedAt(), store.load(s).getStartedAt());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void turnierLoeschen() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir);
            store.create("Weg", LocalDate.of(2026, 1, 1));
            store.create("Bleibt", LocalDate.of(2026, 1, 2));
            store.delete(store.list().stream().filter(s -> s.name().equals("Weg")).findFirst().orElseThrow());
            assertEquals(List.of("Bleibt"), store.list().stream().map(TournamentStore.Summary::name).toList());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void alteTurnierdateiUebernehmen() throws Exception {
        Path dir = tempDir();
        try {
            Path old = dir.resolve("Mein altes Turnier.hts");
            TournamentFile.save(old, null, List.of(new Player("A", "B")));
            TournamentStore store = new TournamentStore(dir.resolve("store"));
            Tournament t = store.importFile(old);
            assertEquals("Mein altes Turnier", t.getName(), "Name aus Dateiname");
            assertEquals(1, store.list().size());
            assertTrue(Files.exists(old), "Original bleibt erhalten");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void dateiMitBeliebigemNamenImOrdnerWirdUebernommen() throws Exception {
        Path dir = tempDir();
        try {
            TournamentFile.save(dir.resolve("Kopie von Turnier.hts"), "Kopie", List.of());
            TournamentStore store = new TournamentStore(dir);
            var s = store.list().get(0);
            Tournament t = store.load(s);
            assertEquals("Kopie", t.getName());
            store.save(t);
            assertEquals(1, store.list().size(), "keine doppelte Datei");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void kaputteDateiWirdGemeldetNichtAbgebrochen() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir);
            store.create("Gut", LocalDate.of(2026, 1, 1));
            Files.writeString(dir.resolve("kaputt.hts"), "<<<kein xml");
            assertEquals(1, store.list().size());
            assertEquals(1, store.getProblems().size());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void ordnerstrukturJeTurnier() throws Exception {
        Path dir = tempDir();
        try {
            TournamentStore store = new TournamentStore(dir.resolve("HTS Turniere"));
            Tournament t = store.create("Herbst: Cup/2026", LocalDate.of(2026, 10, 3));
            Path folder = dir.resolve("HTS Turniere").resolve("Herbst Cup 2026");
            assertEquals(folder.toString(), t.getFolder());
            assertTrue(Files.isRegularFile(folder.resolve("Turnierdatei").resolve("Herbst Cup 2026.hts")));
            assertEquals(folder.resolve("PDF Export"), StorageLocations.pdfDir(t));
            assertEquals(folder.resolve("XML Export"), StorageLocations.xmlDir(t));
            // gleicher Name: eigener Ordner
            Tournament u = store.create("Herbst: Cup/2026", LocalDate.of(2026, 10, 4));
            assertEquals(dir.resolve("HTS Turniere").resolve("Herbst Cup 2026 (2)").toString(), u.getFolder());
            assertEquals(2, store.list().size());

            // abweichender PDF-Ordner wird im Turnier gespeichert
            Path other = dir.resolve("woanders");
            StorageLocations.rememberPdfDir(t, other);
            store.save(t);
            var s = store.list().stream().filter(x -> x.date().getDayOfMonth() == 3).findFirst().orElseThrow();
            Tournament back = store.load(s);
            assertEquals(other, StorageLocations.pdfDir(back));
            StorageLocations.rememberPdfDir(back, folder.resolve("PDF Export"));
            assertNull(back.getPdfDir(), "Standardordner = keine Abweichung");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void turnierdateiVerschieben() throws Exception {
        Path dir = tempDir();
        try {
            Path root = dir.resolve("HTS Turniere");
            TournamentStore store = new TournamentStore(root);
            Tournament t = store.create("Wandert", LocalDate.of(2026, 5, 1));
            Path before = store.fileOf(t);
            Path target = root.resolve("Wandert").resolve("anders");
            store.moveFile(t, target);
            assertFalse(Files.exists(before));
            assertTrue(Files.isRegularFile(target.resolve("Wandert.hts")));
            t.setName("Wandert weiter");
            store.save(t);
            assertTrue(Files.isRegularFile(target.resolve("Wandert.hts")), "bleibt in seiner Datei");
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void alteAblageWirdUebernommen() throws Exception {
        Path dir = tempDir();
        try {
            Path legacy = dir.resolve("HTS").resolve("Turniere");
            Files.createDirectories(legacy);
            TournamentStore old = new TournamentStore(legacy);
            Tournament t = old.create("Altes Turnier", LocalDate.of(2025, 9, 1));
            // alte Ablage: <id>.hts lose im Ordner
            Path oldFile = legacy.resolve(t.getId() + ".hts");
            Files.move(old.fileOf(t), oldFile);
            Files.writeString(legacy.resolve(t.getId() + ".clicktt.xml"), "<x/>");
            TournamentStore store = new TournamentStore(dir.resolve("HTS Turniere"), legacy, null);
            var list = store.list();
            assertEquals(1, list.size());
            assertEquals(t.getId(), list.get(0).id(), "ID bleibt");
            Path folder = dir.resolve("HTS Turniere").resolve("Altes Turnier");
            assertTrue(Files.isRegularFile(folder.resolve("Turnierdatei").resolve("Altes Turnier.hts")));
            assertTrue(Files.isRegularFile(folder.resolve("Turnierdatei").resolve("Altes Turnier.clicktt.xml")));
            assertFalse(Files.exists(oldFile));
            assertEquals(1, store.list().size(), "nur einmal übernommen");
        } finally {
            deleteRecursively(dir);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
