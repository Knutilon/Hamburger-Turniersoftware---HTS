package de.hts.tournament;

import de.hts.importer.csv.HandicapCsvImporter;
import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.31.0: Vorgabeturnier (TTR-Klassen, fester TTR-Abstand, Doppel/Mixed), CSV-Import. */
class Version031Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static void set(List<Integer> target, Integer... values) {
        target.clear();
        target.addAll(List.of(values));
    }

    private static Player p(String first, String last, Integer ttr, Sex sex) {
        Player x = new Player(first, last);
        x.setInternalNr(first + last);
        x.setTtr(ttr);
        x.setSex(sex);
        return x;
    }

    /** Beispiel des Nutzers: Klasse 1 > 0 … <= 1350, 2 … <= 1500, 3 … <= 3000 (Damen eigene Grenzen); 1→3, 2→5 Bälle. */
    private static HandicapRules classRules() {
        HandicapRules r = new HandicapRules();
        r.setMode(HandicapRules.Mode.CLASSES);
        r.setRowCount(HandicapRules.Mode.CLASSES, 3);
        HandicapRules.Bounds a = r.bounds(HandicapRules.Mode.CLASSES, false), w = r.bounds(HandicapRules.Mode.CLASSES, true);
        a.setFirstLower(0);
        set(a.getUppers(), 1350, 1500, 3000);
        w.setFirstLower(0);
        set(w.getUppers(), 1150, 1300, 3000);
        set(r.balls(HandicapRules.Mode.CLASSES), 3, 5);
        return r;
    }

    private static HandicapRules distanceRules() {
        HandicapRules r = new HandicapRules();
        r.setMode(HandicapRules.Mode.DISTANCE);
        r.setRowCount(HandicapRules.Mode.DISTANCE, 3);
        for (boolean women : new boolean[]{false, true}) {
            HandicapRules.Bounds b = r.bounds(HandicapRules.Mode.DISTANCE, women);
            b.setFirstLower(0);
            set(b.getUppers(), 50, 100, 150);
        }
        set(r.balls(HandicapRules.Mode.DISTANCE), 0, 1, 2);
        return r;
    }

    @Test
    void ttrKlassenUndFesterAbstand() {
        HandicapRules c = classRules();
        assertTrue(c.validate().isEmpty());
        assertEquals(0, c.between(1300, 1340, false).balls(), "gleiche Klasse");
        assertEquals(3, c.between(1300, 1400, false).balls(), "1 Klasse Unterschied: der TTR-Niedrigere bekommt 3");
        assertEquals(-5, c.between(1600, 1300, false).balls(), "2 Klassen: B bekommt 5");
        assertEquals(3, c.between(1200, 1400, true).balls(), "Damen-Grenzen: 1200 = Klasse 2, 1400 = Klasse 3 → 1 Klasse");
        assertNotNull(c.between(3100, 1300, false).problem(), "über der höchsten Obergrenze");

        HandicapRules d = distanceRules();
        assertTrue(d.validate().isEmpty());
        assertEquals(0, d.between(1500, 1500, false).balls(), "Abstand 0 → keine Vorgabe");
        assertEquals(0, d.between(1500, 1540, false).balls(), "Kategorie 1 → 0 Bälle");
        assertEquals(1, d.between(1500, 1580, false).balls(), "Kategorie 2: der TTR-Niedrigere (A) bekommt 1");
        assertEquals(-2, d.between(1640, 1500, false).balls());
        assertNotNull(d.between(1700, 1500, false).problem(), "Abstand über der höchsten Obergrenze");
    }

    @Test
    void pflichtfelderUndLueckenlos() {
        HandicapRules r = new HandicapRules();
        assertFalse(r.validate().isEmpty(), "Art fehlt");
        r.setMode(HandicapRules.Mode.CLASSES);
        assertFalse(r.validate().isEmpty(), "Grenzen und Bälle fehlen");
        HandicapRules c = classRules();
        assertEquals(1350, c.bounds(HandicapRules.Mode.CLASSES, false).lower(1), "Obergrenze = Untergrenze der nächsten");
        c.balls(HandicapRules.Mode.CLASSES).set(1, null);
        assertFalse(c.validate().isEmpty(), "jeder Klassenunterschied braucht eine Bällezahl");
        c.setRowCount(HandicapRules.Mode.CLASSES, 9);
        assertEquals(7, c.rowCount(HandicapRules.Mode.CLASSES), "höchstens 7 Klassen");
        c.setRowCount(HandicapRules.Mode.DISTANCE, 12);
        assertEquals(10, c.rowCount(HandicapRules.Mode.DISTANCE), "höchstens 10 Kategorien");
        assertEquals(10, c.balls(HandicapRules.Mode.DISTANCE).size());
    }

    @Test
    void doppelVerrechnenUndHalbieren() {
        Tournament t = new Tournament();
        t.setHandicap(true);
        t.setHandicapRules(classRules());
        List<Player> a = List.of(p("a", "1", 1600, Sex.MALE), p("a", "2", 1200, Sex.MALE));
        List<Player> b = List.of(p("b", "1", 1400, Sex.MALE), p("b", "2", 1450, Sex.MALE));
        // höher↔höher: 1600 (3) vs 1450 (2) → B +3; tiefer↔tiefer: 1200 (1) vs 1400 (2) → A +3 → verrechnet 0
        assertEquals(0, t.handicapBetween(a, b, false));
        // Beispiel des Nutzers: „höher“ +2 für A, „tiefer“ +4 für B → ohne Halbierung +2 B, mit Halbierung +1 B
        HandicapRules r = t.getHandicapRules();
        assertEquals(-1, r.combine(2, -4), "mit Halbierung (Standard): +1 für B");
        assertEquals(3, r.combine(2, 4), "gleichgerichtet: (2+4)/2 = 3");
        HandicapRules noHalve = classRules();
        noHalve.setHalve(false);
        assertEquals(-2, noHalve.combine(2, -4));
        assertEquals(6, noHalve.combine(2, 4));
        assertEquals(1, r.combine(3, 0), "abgerundet: 3/2 = 1");
    }

    @Test
    void mixedHerrGegenHerrDameGegenDame() {
        Tournament t = new Tournament();
        t.setHandicap(true);
        t.setHandicapRules(classRules());
        // Herren nach Erwachsenen-Grenzen: 1600 (Kl.3) vs 1300 (Kl.1) → B +5; Damen nach Damen-Grenzen: 1100 (Kl.1) vs 1250 (Kl.2) → A +3
        List<Player> a = List.of(p("am", "x", 1600, Sex.MALE), p("aw", "x", 1100, Sex.FEMALE));
        List<Player> b = List.of(p("bm", "x", 1300, Sex.MALE), p("bw", "x", 1250, Sex.FEMALE));
        assertEquals(-1, t.handicapBetween(a, b, false), "(-5 + 3) / 2 = -1 → B +1");
    }

    @Test
    void startGesperrtOhneTtrUndUeberGrenze() throws Exception {
        Tournament t = new Tournament();
        t.setHandicap(true);
        t.setTableCount(4);
        PlayingClass c = t.addClass(new PlayingClass("Offen"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 4; i++) ps.add(p("S" + i, "N" + i, 1300 + i * 40, Sex.MALE));
        ps.get(3).setTtr(null);
        t.merge(ps);
        t.getPlayers().forEach(x -> t.assign(x, c));
        t.drawKnockout(c, false, new Random(1));
        assertFalse(t.handicapProblems(c).isEmpty(), "Regeln fehlen");
        assertThrows(IllegalStateException.class, () -> t.startClass(c));
        t.setHandicapRules(classRules());
        assertTrue(t.handicapProblems(c).stream().anyMatch(s -> s.startsWith("Ohne TTR")));
        assertThrows(IllegalStateException.class, () -> t.startClass(c));
        t.getPlayers().stream().filter(x -> x.getTtr() == null).forEach(x -> x.setTtr(3500));
        assertTrue(t.handicapProblems(c).stream().anyMatch(s -> s.contains("über der höchsten Klassengrenze")));
        t.getPlayers().stream().filter(x -> x.getTtr() == 3500).forEach(x -> x.setTtr(1450));
        assertTrue(t.handicapProblems(c).isEmpty());
        t.startClass(c);
        assertTrue(t.isHandicapLocked());
        assertThrows(IllegalStateException.class, () -> t.setHandicapRules(classRules()), "nach dem Start gesperrt");
        Match m = t.matchesOf(c).stream().filter(x -> x.getA() != null && x.getB() != null).findFirst().orElseThrow();
        int h = t.handicapOf(m);
        String la = t.handicapLabel(m, m.getA()), lb = t.handicapLabel(m, m.getB());
        assertTrue(h == 0 ? la.isEmpty() && lb.isEmpty() : (la.isEmpty() != lb.isEmpty()));

        Tournament back = roundTrip(t);
        assertTrue(back.isHandicap());
        assertEquals(t.getHandicapRules().encode(), back.getHandicapRules().encode());
    }

    @Test
    void festerAbstandStartPrueftGroesstmoeglichenAbstand() {
        Tournament t = new Tournament();
        t.setHandicap(true);
        t.setHandicapRules(distanceRules());
        PlayingClass c = t.addClass(new PlayingClass("Offen"));
        List<Player> ps = List.of(p("A", "a", 1500, Sex.MALE), p("B", "b", 1560, Sex.MALE), p("C", "c", 1700, Sex.MALE));
        t.merge(ps);
        t.getPlayers().forEach(x -> t.assign(x, c));
        assertTrue(t.handicapProblems(c).stream().anyMatch(s -> s.contains("Größtmöglicher TTR-Abstand 200")));
    }

    @Test
    void csvImportUndErsetzen() throws Exception {
        String csv = "Vorname,Nachname,TTR-Wert,Klasse,Egal\n"
                + "Jürgen,Müller,1520,Erwachsene,x\n"
                + "Anna,\"Schmidt, geb. Groß\",1380,Damen\n"
                + ",Ohne,1400,Erwachsene\n"
                + "Max,Fehler,abc,Erwachsene\n"
                + "Eva,Klasse,1300,Jugend\n"
                + "Leer,TTR,,Damen\n"
                + "\n";
        HandicapCsvImporter.Result r = new HandicapCsvImporter().parse(csv.getBytes(StandardCharsets.UTF_8));
        assertEquals(3, r.players().size());
        assertEquals(3, r.ignored().size());
        assertTrue(r.ignored().get(0).startsWith("Zeile 4:"));
        assertEquals("Müller", r.players().get(0).getLastname(), "UTF-8 Umlaute");
        assertEquals(Sex.MALE, r.players().get(0).getSex());
        assertEquals(Sex.FEMALE, r.players().get(1).getSex());
        assertEquals("Schmidt, geb. Groß", r.players().get(1).getLastname());
        assertNull(r.players().get(2).getTtr(), "leerer TTR wird übernommen (Start dann gesperrt)");
        assertThrows(HandicapCsvImporter.CsvException.class,
                () -> new HandicapCsvImporter().parse("Vorname\nJürgen".getBytes(StandardCharsets.ISO_8859_1)), "kein UTF-8");

        Tournament t = new Tournament();
        t.setHandicap(true);
        t.merge(List.of(p("Alt", "A", 1500, Sex.MALE), p("Alt", "B", 1400, Sex.MALE)));
        t.replaceParticipants(r.players());
        assertEquals(3, t.getPlayers().size(), "alle bisherigen ersetzt");
        PlayingClass c = t.addClass(new PlayingClass("Offen"));
        t.getPlayers().forEach(x -> t.assign(x, c));
        t.drawKnockout(c, false, new Random(1));
        assertThrows(IllegalStateException.class, () -> t.replaceParticipants(r.players()), "nach der Auslosung nicht mehr");
    }
}
