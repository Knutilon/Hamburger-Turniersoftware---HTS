/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.importer.clicktt.ClickTtResultExporter;
import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.persistence.TournamentFile;
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.report.PdfReports;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 1.0.0 – Zeitplan (#77), Abgleich nachgetragener Personen (#78/#79), Präsentation (#81), Startgebühren (#82). */
class Version100Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static String sample() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("samples/clicktt-beispiel.xml"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Turnier aus der Beispieldatei, Herren Einzel mit einer von Hand nachgetragenen Person (Paul Neu), gespielt. */
    private static Tournament playedWithManual() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(sample().getBytes(StandardCharsets.UTF_8)));
        for (var ci : r.competitions()) {
            PlayingClass nc = t.addClass(new PlayingClass(ci.name(), ci.doubles() ? Discipline.DOUBLES : Discipline.SINGLES,
                    ClassType.guess(ci.name(), ci.sex()), null, null));
            nc.setImportedFromXml(true);
            nc.setFeeCents(ci.feeCents());
        }
        t.mergeImportedEntries(r.players(), new ArrayList<>());
        PlayingClass c = t.findClass("Herren Einzel").orElseThrow();
        Player neu = new Player("Paul", "Neu");
        neu.setBirthyear(1990);
        neu.setClubName("TTC Elbufer");
        neu.setSex(Sex.MALE);
        neu.setTtr(1500);
        t.merge(List.of(neu));
        neu = t.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        t.setAttendance(neu, Attendance.PRESENT);
        for (Player p : List.copyOf(t.getPlayers())) {
            if (p.getRegistrations().contains(c.getName())) t.setAttendance(p, Attendance.PRESENT);
        }
        for (Player p : List.copyOf(t.getPlayers())) {
            if (p.isPresent() && (p.getRegistrations().contains(c.getName()) || p == neu)) t.assign(p, c);
        }
        c.setGroupsBySize(false);
        c.setGroupCount(1);
        t.drawGroups(c, true, new Random(1));
        t.startClass(c);
        PhaseFixtures.playAll(t, c);
        t.finishClass(c);
        return t;
    }

    private static String withNewPerson(String xml, String person) {
        int i = xml.indexOf("id=\"PLAYER1\"");
        i = xml.lastIndexOf("<player", i);
        return xml.substring(0, i) + "<player type=\"single\" id=\"PLAYER99\">" + person + "</player>\n" + xml.substring(i);
    }

    @Test
    void erneuterImportUebernimmtNachgetragenePersonOhneDoppel() throws Exception {
        Tournament t = playedWithManual();
        String file = withNewPerson(sample(), "<person firstname=\"Paul\" lastname=\"Neu\" birthyear=\"1990\" sex=\"1\" "
                + "club-name=\"TTC Elbufer\" internal-nr=\"NU1000099\" licence-nr=\"101099\"/>");
        var before = ClickTtResultExporter.export(t, new ByteArrayInputStream(sample().getBytes(StandardCharsets.UTF_8)), OutputStreamNull.get());
        assertEquals(1, before.withoutId().size(), "Paul Neu ohne click-TT-Nummer");
        assertTrue(before.notExported().stream().anyMatch(n -> n.reason().equals("Paul Neu ohne click-TT-Nummer")));

        int count = t.getPlayers().size();
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
        t.mergeImportedEntries(r.players(), new ArrayList<>());
        assertEquals(count, t.getPlayers().size(), "#78: kein doppelter Eintrag");
        Player neu = t.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        assertEquals("NU1000099", neu.getInternalNr());
        var after = ClickTtResultExporter.export(t, new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)), OutputStreamNull.get());
        assertTrue(after.withoutId().isEmpty());
        assertEquals(before.matches() + 3, after.matches(), "die 3 Spiele von Paul Neu werden jetzt übertragen");
        assertEquals(3, before.notExported().stream().filter(n -> n.reason().endsWith("ohne click-TT-Nummer")).count());
    }

    @Test
    void abgleichOrdnetEindeutigeAutomatischZuUndLaesstMehrdeutigeOffen() throws Exception {
        Tournament t = playedWithManual();
        Player neu = t.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        // Verein anders → nicht automatisch; zwei weitere neue Personen in der Datei
        String file = withNewPerson(sample(), "<person firstname=\"Paul\" lastname=\"Neu\" birthyear=\"1990\" sex=\"1\" "
                + "club-name=\"TSV Speicherstadt\" internal-nr=\"NU1000099\"/>");
        file = withNewPerson(file, "<person firstname=\"Lena\" lastname=\"Hoffmann\" birthyear=\"2011\" sex=\"0\" club-name=\"Hammer SC\" internal-nr=\"NU1000098\"/>");
        var r = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
        Tournament.Matching m = t.matchManual(List.of(neu), r.players());
        assertTrue(m.auto().isEmpty(), "Verein passt nicht → keine automatische Zuordnung");
        assertEquals(List.of(neu), m.open());
        assertEquals(2, m.newPersons().size(), "nur die neuen Personen der Datei");
        // von Hand zuordnen → Nummern übernommen, Spiele werden exportiert
        Player paul = m.newPersons().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        t.adoptNumbers(neu, paul);
        var rep = ClickTtResultExporter.export(t, new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)), OutputStreamNull.get());
        assertTrue(rep.withoutId().isEmpty());

        // gleicher Verein → automatisch
        Tournament t2 = playedWithManual();
        Player neu2 = t2.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        String file2 = withNewPerson(sample(), "<person firstname=\"Paul\" lastname=\"Neu\" sex=\"1\" club-name=\"TTC Elbufer\" internal-nr=\"NU1000099\"/>");
        var r2 = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(file2.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, t2.matchManual(List.of(neu2), r2.players()).auto().size());
    }

    @Test
    void nichtExportierteSpieleMitGrund() throws Exception {
        Tournament t = playedWithManual();
        PlayingClass c = t.findClass("Herren Einzel").orElseThrow();
        Match excluded = t.matchesOf(c).stream().filter(x -> !t.entrant(x, x.getA()).name().contains("Neu")
                && !t.entrant(x, x.getB()).name().contains("Neu")).findFirst().orElseThrow();
        excluded.setExportExcluded(true);
        var rep = ClickTtResultExporter.export(t, new ByteArrayInputStream(sample().getBytes(StandardCharsets.UTF_8)), OutputStreamNull.get());
        assertTrue(rep.notExported().stream().anyMatch(n -> n.number() == excluded.getNumber() && n.reason().equals("bewusst ausgelassen")));
        try (var doc = PdfReports.notExported(t, rep.notExported())) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void startgebuehren() throws Exception {
        Tournament t = playedWithManual();
        PlayingClass c = t.findClass("Herren Einzel").orElseThrow();
        assertEquals(800, c.getFeeCents(), "Startgebühr aus click-TT (entry-fee=\"8.0\")");
        assertTrue(t.hasFees());
        Player neu = t.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        assertEquals(800, t.feeOf(neu));
        t.setLateFeeCents(200);
        assertTrue(t.isLateEntry(neu));
        assertEquals(1000, t.feeOf(neu), "Nachmeldegebühr einmal je Person");
        assertEquals(t.playersIn(c).size(), t.unpaidIn(c).size());
        t.markPaid(List.of(neu), Player.Payment.CASH, null);
        assertTrue(neu.isPaid());
        assertEquals(1000, neu.getPaidCents());
        assertFalse(t.unpaidIn(c).contains(neu));
        List<Player> club = t.getPlayers().stream().filter(p -> "TTC Elbufer".equals(p.getClubName()) && t.isFeeOpen(p)).toList();
        t.markPaid(club, Player.Payment.TRANSFER, "TTC Elbufer");
        assertTrue(club.stream().allMatch(p -> "TTC Elbufer".equals(p.getPaidByClub())));
        // Abgemeldete zahlen nichts
        Player any = t.getPlayers().stream().filter(p -> !p.isPaid() && t.feeOf(p) > 0).findFirst().orElseThrow();
        t.setAttendance(any, Attendance.WITHDRAWN);
        assertEquals(0, t.feeOf(any));

        Tournament back = roundTrip(t);
        assertEquals(200, back.getLateFeeCents());
        assertEquals(800, back.findClass("Herren Einzel").orElseThrow().getFeeCents());
        Player neuBack = back.getPlayers().stream().filter(p -> p.getLastname().equals("Neu")).findFirst().orElseThrow();
        assertEquals(Player.Payment.CASH, neuBack.getPayment());
        assertEquals(1000, neuBack.getPaidCents());
        assertNotNull(neuBack.getPaidAt());
        try (var d1 = PdfReports.receipt(t, List.of(neu), null); var d2 = PdfReports.cashReport(t)) {
            assertEquals(1, d1.getNumberOfPages());
            assertTrue(d2.getNumberOfPages() >= 1);
        }
        assertEquals("8,00 €", Tournament.euro(800));
        assertEquals(850, ClickTtXmlImporter.feeCents("8,50"));
    }

    @Test
    void praesentationWarteschlangeJeKlasse() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(2);
        PlayingClass c = PhaseFixtures.groupsClass(t, "W", 8, 2);
        t.startClass(c);
        PresentationPlan.Station q = PresentationPlan.queueStation(t, c);
        assertNotNull(q);
        assertEquals("queue:W", q.key());
        assertFalse(PresentationPlan.isOn(t, q), "standardmäßig aus");
        assertTrue(PresentationPlan.all(t).stream().anyMatch(s -> s.key().equals("queue:W")));
        t.setPresentationOn("queue:W", true);
        assertTrue(PresentationPlan.isOn(t, q));
        t.setPresentationNextCount(100);
        assertEquals(48, t.getPresentationNextCount());
        t.setPresentationNextCount(24);
        Tournament back = roundTrip(t);
        assertEquals(24, back.getPresentationNextCount());
        assertTrue(back.getPresentationOn().contains("queue:W"));
    }

    @Test
    void zeitplanHaeltRundenZurueckUndFragt() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "Z", 8, 2);
        c.setScheduleMinutes(30);
        t.startClass(c);
        assertTrue(t.hasSchedule(c));
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getStatus() != MatchStatus.PLANNED).allMatch(m -> m.getRound() == 1),
                "nur Runde 1 ist eingereiht bzw. am Tisch");
        assertTrue(t.matchesOf(c).stream().anyMatch(m -> m.getRound() == 2 && m.getStatus() == MatchStatus.PLANNED));
        // Runde 1 von Gruppe A beenden → vor dem Zeitplan: Rückfrage, Runde 2 bleibt zurück
        for (Match m : List.copyOf(t.matchesOf(c))) {
            if (m.getGroupIndex() == 0 && m.getRound() == 1) PhaseFixtures.play(t, m);
        }
        List<Tournament.ScheduleQuestion> qs = t.takeScheduleQuestions();
        assertEquals(1, qs.size());
        assertTrue(qs.get(0).label().contains("Gruppe A") && qs.get(0).label().contains("Runde 2"), qs.get(0).label());
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getGroupIndex() == 0 && m.getRound() == 2)
                .allMatch(m -> m.getStatus() == MatchStatus.PLANNED));
        assertEquals(1, t.waitingRounds(c).size());
        assertTrue(t.takeScheduleQuestions().isEmpty(), "nur einmal fragen");
        // „Jetzt starten“
        t.releaseRound(qs.get(0).phase(), qs.get(0).key());
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getGroupIndex() == 0 && m.getRound() == 2)
                .allMatch(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.ACTIVE));
        Tournament back = roundTrip(t);
        PlayingClass cb = back.findClass("Z").orElseThrow();
        assertEquals(30, cb.getScheduleMinutes());
        assertTrue(cb.getReleasedRounds().contains("g0:2"));
        assertNotNull(cb.getScheduleAnchor());
        // Zeit abgelaufen → automatisch eingereiht
        c.setScheduleAnchor(java.time.LocalDateTime.now().minusHours(5));
        for (Match m : List.copyOf(t.matchesOf(c))) {
            if (m.getGroupIndex() == 1 && m.getRound() == 1 && m.getStatus() != MatchStatus.DONE) PhaseFixtures.play(t, m);
        }
        assertTrue(t.matchesOf(c).stream().filter(m -> m.getGroupIndex() == 1 && m.getRound() == 2)
                .allMatch(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.ACTIVE), "Zeit erreicht: ohne Rückfrage");
        // Simulieren ignoriert den Zeitplan und spielt alles
        t.setTest(true);
        c.setScheduleAnchor(java.time.LocalDateTime.now());
        t.simulateAll(new Random(3));
        assertTrue(t.isPhaseComplete(c), "Simulation spielt alle Runden");
    }

    @Test
    void ohneZeitplanUnveraendert() {
        Tournament t = new Tournament();
        t.setTableCount(10);
        PlayingClass c = PhaseFixtures.groupsClass(t, "O", 8, 2);
        t.startClass(c);
        assertFalse(t.hasSchedule(c));
        assertTrue(t.matchesOf(c).stream().noneMatch(m -> m.getStatus() == MatchStatus.PLANNED), "alle Spiele eingereiht");
    }

    @Test
    void mobilAusgeblendet() {
        assertFalse(de.hts.HtsApp.MOBILE_ENABLED, "#80: Reiter „Mobile Einstellungen“ ausgeblendet");
    }

    /** Ausgabe verwerfen. */
    private static final class OutputStreamNull {
        static java.io.OutputStream get() {
            return java.io.OutputStream.nullOutputStream();
        }
    }
}
