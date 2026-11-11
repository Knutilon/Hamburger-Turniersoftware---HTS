/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.importer.clicktt;

import de.hts.player.Player;
import de.hts.tournament.Tournament;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ClickTtXmlImporterTest {

    private final ClickTtXmlImporter importer = new ClickTtXmlImporter();

    private ClickTtXmlImporter.ImportResult importSample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/clicktt-beispiel.xml")) {
            assertNotNull(in, "Beispieldatei fehlt");
            return importer.importStream(in);
        }
    }

    private static Player byLastname(ClickTtXmlImporter.ImportResult r, String lastname) {
        return r.players().stream().filter(p -> lastname.equals(p.getLastname())).findFirst()
                .orElseThrow(() -> new AssertionError("Spieler fehlt: " + lastname));
    }

    @Test
    void liestTurnierUndAlleSpielerOhneDuplikate() throws Exception {
        var r = importSample();
        assertEquals("HTS Testturnier Hamburg", r.tournamentName());
        // 8 verschiedene Personen, obwohl 11 <person>-Einträge in der Datei stehen
        assertEquals(8, r.players().size());
        assertTrue(r.warnings().isEmpty());
    }

    @Test
    void liestAlleFelderUndUmlauteKorrekt() throws Exception {
        Player p = byLastname(importSample(), "Brückner");
        assertEquals("Jonas", p.getFirstname());
        assertEquals(1994, p.getBirthyear());
        assertEquals(Sex.MALE, p.getSex());
        assertEquals("TTC Elbufer", p.getClubName());
        assertEquals("1001", p.getClubNr());
        assertEquals("HaTTV", p.getClubFederation());
        assertEquals("NU1000001", p.getInternalNr());
        assertEquals("101001", p.getLicenceNr());
        assertEquals(1712, p.getTtr());
        assertEquals(230, p.getTtrMatchCount());
        assertEquals("DE", p.getNationality());

        assertEquals(Sex.FEMALE, byLastname(importSample(), "Schäfer").getSex());
        assertNotNull(byLastname(importSample(), "Østergaard"));
    }

    @Test
    void sammeltKlassenJePerson() throws Exception {
        var r = importSample();
        assertEquals(java.util.Set.of("Herren Einzel", "Herren Doppel"), byLastname(r, "Brückner").getClassNames());
        assertEquals(java.util.Set.of("Herren Doppel"), byLastname(r, "Behrens").getClassNames());
        assertEquals(java.util.Set.of("Damen Einzel (TTR bis 1500)"), byLastname(r, "García").getClassNames());
    }

    @Test
    void wartelisteWirdNichtInDieKlasseEingetragen() throws Exception {
        var r = importSample();
        Player kroeger = byLastname(r, "Kröger");
        assertTrue(kroeger.getClassNames().isEmpty());
        assertFalse(kroeger.isRegistered());
        assertEquals(java.util.Set.of("Damen Einzel (TTR bis 1500)"), kroeger.getWaitingLists());

        // Möller: im Einzel auf der Warteliste, im Doppel angemeldet
        Player moeller = byLastname(r, "Möller");
        assertEquals(java.util.Set.of("Herren Doppel"), moeller.getClassNames());
        assertEquals(java.util.Set.of("Herren Einzel"), moeller.getWaitingLists());
        assertTrue(moeller.isRegistered());
        assertEquals("Herren Doppel · Warteliste: Herren Einzel", moeller.getClassesText());
    }

    @Test
    void erneuterImportMeldetNachgerueckteAnUndEntferntNiemanden() throws Exception {
        Tournament repo = new Tournament();
        repo.merge(importSample().players());
        Player kroeger = repo.getPlayers().stream().filter(p -> "Kröger".equals(p.getLastname())).findFirst().orElseThrow();
        var damen = repo.findClass("Damen Einzel (TTR bis 1500)").orElseThrow();

        // von Hand in die Klasse gezogen -> erneuter Import (dort noch Warteliste) nimmt sie nicht heraus
        repo.assign(kroeger, damen);
        repo.merge(importSample().players());
        assertTrue(kroeger.isInClass(damen.getName()));
        assertTrue(kroeger.getWaitingLists().isEmpty());

        // steht jemand in der neuen Datei regulär, wird er eingetragen
        repo.unassign(kroeger, damen);
        String xml = "<tournament><competition age-group=\"Damen\" type=\"Einzel\" ttr-to=\"1500\"><players>"
                + "<player waiting-list-player=\"false\"><person firstname=\"Svenja\" lastname=\"Kröger\" internal-nr=\"NU1000009\"/></player>"
                + "</players></competition></tournament>";
        repo.merge(importer.importStream(stream(xml)).players());
        assertTrue(kroeger.isInClass(damen.getName()));
    }

    @Test
    void zaehltMeldungenJeKonkurrenzDoppelAlsEineMeldung() throws Exception {
        var r = importSample();
        assertEquals(3, r.competitions().size());
        assertEquals(new ClickTtXmlImporter.CompetitionInfo("Herren Einzel", 4, 1, Sex.MALE, false), r.competitions().get(0));
        assertEquals(new ClickTtXmlImporter.CompetitionInfo("Damen Einzel (TTR bis 1500)", 3, 1, Sex.FEMALE, false), r.competitions().get(1));
        assertEquals(new ClickTtXmlImporter.CompetitionInfo("Herren Doppel", 2, 0, Sex.UNKNOWN, true), r.competitions().get(2));
    }

    @Test
    void wiederholterImportLegtKeineDuplikateAn() throws Exception {
        Tournament repo = new Tournament();
        var first = repo.merge(importSample().players());
        assertEquals(8, first.added());
        var second = repo.merge(importSample().players());
        assertEquals(0, second.added());
        assertEquals(8, second.updated());
        assertEquals(8, repo.size());
    }

    @Test
    void erkenntSpielerOhneIdAnNameJahrgangVerein() throws Exception {
        String xml = """
                <tournament>
                  <competition age-group="Senioren" type="Einzel"><players>
                    <player><person firstname="Anna" lastname="Ohneid" birthyear="1970" club-name="Club A" ttr="1200"/></player>
                  </players></competition>
                  <competition age-group="Mixed" type="Doppel"><players>
                    <player><person firstname="Anna" lastname="Ohneid" birthyear="1970" club-name="Club A" ttr="1210"/>
                            <person firstname="Bert" lastname="Partner" birthyear="1969" club-name="Club A"/></player>
                  </players></competition>
                </tournament>""";
        var r = importer.importStream(stream(xml));
        assertEquals(2, r.players().size());
        Player anna = byLastname(r, "Ohneid");
        assertEquals(1210, anna.getTtr(), "neuere Angabe gewinnt");
        assertEquals(2, anna.getClassNames().size());
    }

    @Test
    void doppelmeldungenWerdenAlsPaarungenErkannt() throws Exception {
        var r = importSample();
        assertEquals(2, r.pairs().size());
        var p1 = r.pairs().get(0);
        assertEquals("Herren Doppel", p1.competition());
        assertEquals("id:NU1000001", p1.firstKey());
        assertEquals("id:NU1000004", p1.secondKey());
        var p2 = r.pairs().get(1);
        assertEquals("id:NU1000008", p2.firstKey());
        assertEquals("id:NU1000002", p2.secondKey());
    }

    @Test
    void liestGeburtsdatumFallsVorhanden() throws Exception {
        String xml = "<tournament><person firstname=\"A\" lastname=\"B\" birthdate=\"1994-03-14\"/>"
                + "<person firstname=\"C\" lastname=\"D\" birth-date=\"02.07.2001\"/></tournament>";
        var r = importer.importStream(stream(xml));
        assertEquals(java.time.LocalDate.of(1994, 3, 14), byLastname(r, "B").getBirthdate());
        assertEquals(1994, byLastname(r, "B").getBirthyear());
        assertEquals("02.07.2001", byLastname(r, "D").getBirthText());
    }

    @Test
    void ueberspringtPersonenOhneNamenMitWarnung() throws Exception {
        String xml = "<tournament><person firstname=\"Ok\" lastname=\"Spieler\"/><person ttr=\"1000\"/></tournament>";
        var r = importer.importStream(stream(xml));
        assertEquals(1, r.players().size());
        assertEquals(1, r.warnings().size());
    }

    @Test
    void ungueltigesXmlLiefertVerstaendlicheMeldung() {
        var ex = assertThrows(ClickTtImportException.class, () -> importer.importStream(stream("<tournament><person>")));
        assertTrue(ex.getMessage().startsWith("Die Datei ist kein gültiges XML"));
    }

    @Test
    void dateiOhneSpielerWirdAbgelehnt() {
        var ex = assertThrows(ClickTtImportException.class, () -> importer.importStream(stream("<tournament/>")));
        assertTrue(ex.getMessage().contains("keine Teilnehmenden"));
    }

    @Test
    void externeEntitaetenWerdenNichtAufgeloest() throws Exception {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE tournament [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <tournament><person firstname="X" lastname="&xxe;"/></tournament>""";
        try {
            var r = importer.importStream(stream(xml));
            r.players().forEach(p -> assertFalse(String.valueOf(p.getLastname()).contains("root:")));
        } catch (ClickTtImportException expected) {
            // ebenfalls in Ordnung: Datei wird abgelehnt
        }
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
