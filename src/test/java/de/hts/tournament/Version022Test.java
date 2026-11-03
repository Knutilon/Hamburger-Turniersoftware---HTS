package de.hts.tournament;

import de.hts.persistence.StorageLocations;
import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.report.PdfReports;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.22.0: Dateiname der Ergebnisdatei, Einzeldruck einer Gruppe. */
class Version022Test {

    @Test
    void dateinameDerErgebnisdatei() {
        Tournament t = new Tournament();
        t.setName("HEM Vorquali");
        t.setDate(LocalDate.of(2026, 11, 1));
        assertEquals("Ergebnisse - HEM Vorquali - 01_11_2026.xml", StorageLocations.resultsFileName(t));
        t.setName("12. Hamburger Jugend/Ranglisten: Turnier");
        assertEquals("Ergebnisse - 12. Hamburger Jugend - 01_11_2026.xml", StorageLocations.resultsFileName(t));
        t.setDate(null);
        assertEquals("Ergebnisse - 12. Hamburger Jugend.xml", StorageLocations.resultsFileName(t));
    }

    @Test
    void einzelneGruppeAlsPdf() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = t.addClass(new PlayingClass("Gruppen"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setInternalNr("G-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1800 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(3);
        t.drawGroups(c, true, new Random(1));
        try (var doc = PdfReports.group(t, c, 1)) {
            assertEquals(1, doc.getNumberOfPages(), "vor dem Start");
        }
        t.startClass(c);
        t.simulateAll(new Random(2));
        try (var doc = PdfReports.group(t, c, 2)) {
            assertEquals(1, doc.getNumberOfPages(), "abgeschlossen");
        }
    }
}
