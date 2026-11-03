package de.hts.tournament;

import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.report.PdfReports;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.23.0: Partner zulosen, Sperre der Klasseneinstellungen, Doppel ausgeschrieben, KO-Baum im PDF. */
class Version023Test {

    private static List<Player> players(Tournament t, String prefix, int n) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player("V" + prefix + i, "N" + prefix + i);
            p.setInternalNr(prefix + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1800 - i * 10);
            p.setClubName("Verein " + (i % 4));
            ps.add(p);
        }
        t.merge(ps);
        return ps;
    }

    private static PlayingClass doubles(Tournament t, String name) {
        return t.addClass(new PlayingClass(name, Discipline.DOUBLES, ClassType.ADULTS, null, null));
    }

    @Test
    void partnerZulosen() {
        Tournament t = new Tournament();
        PlayingClass d = doubles(t, "Doppel");
        List<Player> ps = players(t, "D", 9);
        t.createPair(d, ps.get(0), ps.get(1));                      // festes Doppel
        for (int i = 2; i < 9; i++) t.assign(ps.get(i), d);        // 7 Einzelne ohne Partner/in
        long open = t.pairsIn(d).stream().filter(p -> !p.isComplete()).count();
        assertEquals(7, open);
        Tournament.PartnerDraw r = t.drawPartners(d, new Random(5));
        assertEquals(3, r.pairs().size());
        assertNotNull(r.leftOver(), "ungerade Zahl: eine/r bleibt übrig");
        assertEquals(4, t.pairsIn(d).stream().filter(DoublesPair::isComplete).count());
        assertEquals(1, t.pairsIn(d).stream().filter(p -> !p.isComplete()).count());
        // festes Doppel unverändert, niemand doppelt
        assertTrue(t.pairsIn(d).stream().anyMatch(p -> p.isComplete() && p.getFirst() == ps.get(0) && p.getSecond() == ps.get(1)));
        Set<Player> seen = new HashSet<>();
        for (DoublesPair p : t.pairsIn(d)) {
            if (p.getFirst() != null) assertTrue(seen.add(p.getFirst()));
            if (p.getSecond() != null) assertTrue(seen.add(p.getSecond()));
        }
        assertThrows(IllegalStateException.class, () -> t.drawPartners(d, new Random(1)), "nur noch 1 Einzelne/r");
    }

    @Test
    void klasseNachErstemErgebnisGesperrt() {
        Tournament t = new Tournament();
        t.setTableCount(4);
        PlayingClass c = t.addClass(new PlayingClass("Herren"));
        players(t, "H", 4).forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.startClass(c);
        assertFalse(t.hasResults(c));
        Match m = t.matchesOf(c).get(0);
        t.recordResult(m, 3, 0);
        assertTrue(t.hasResults(c));
    }

    @Test
    void doppelAusgeschriebenUndKoBaumImPdf() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(8);
        PlayingClass d = doubles(t, "Doppel");
        List<Player> ps = players(t, "P", 24);
        for (int i = 0; i < 24; i += 2) t.createPair(d, ps.get(i), ps.get(i + 1));
        t.drawKnockout(d, true, new Random(1));
        t.setThirdPlace(d, true);
        t.startClass(d);
        Match m = t.matchesOf(d).get(0);
        List<String[]> lines = t.partyLines(m, m.getA());
        assertEquals(2, lines.size(), "Doppel: je Person eine Zeile");
        assertTrue(t.partyFullName(m, m.getA()).contains(" / "));
        assertTrue(lines.get(0)[0].contains(", "), "Nachname, Vorname");
        t.simulateAll(new Random(2));
        try (var doc = PdfReports.phase(t, d)) {
            assertTrue(doc.getNumberOfPages() >= 2, "Turnierbaum + Spielplan");
        }
        try (var doc = PdfReports.slip(t, m)) {
            assertEquals(1, doc.getNumberOfPages());
        }
        // großes Einzelfeld: Baum auf mehrere Seiten
        PlayingClass e = t.addClass(new PlayingClass("Einzel"));
        players(t, "E", 50).forEach(p -> t.assign(p, e));
        t.drawKnockout(e, true, new Random(3));
        try (var doc = PdfReports.phase(t, e)) {
            assertTrue(doc.getNumberOfPages() >= 4, "64er-Feld: Teilbäume + Finalrunden + Spielplan");
        }
    }
}
