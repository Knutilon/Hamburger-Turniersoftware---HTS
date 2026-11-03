package de.hts.tournament;

import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentClassesTest {

    /** Turniertag für alle Tests: Jugend (unter 21) = Jahrgang 2006 und jünger. */
    private static final LocalDate DAY = LocalDate.of(2026, 11, 14);

    private static Player player(String first, String last, Sex sex, Integer year) {
        Player p = new Player(first, last);
        p.setSex(sex);
        p.setBirthyear(year);
        p.setInternalNr("NU-" + first + last);
        return p;
    }

    private final Player jonas = player("Jonas", "Herr", Sex.MALE, 1994);
    private final Player finn = player("Finn", "Junge", Sex.MALE, 2012);
    private final Player lea = player("Lea", "Dame", Sex.FEMALE, 1999);
    private final Player mia = player("Mia", "Mädchen", Sex.FEMALE, 2013);
    private final Player otto = player("Otto", "Senior", Sex.MALE, 1960);
    private final Player ohneJg = player("Kim", "Ohnejahr", Sex.MALE, null);

    private Tournament tournament() {
        Tournament t = new Tournament();
        t.setDate(DAY);
        t.merge(List.of(jonas, finn, lea, mia, otto, ohneJg));
        return t;
    }

    @Test
    void vierKlassentypen() {
        PlayingClass erwachsene = new PlayingClass("Erwachsene", ClassType.ADULTS, null, null);
        PlayingClass damen = new PlayingClass("Damen", ClassType.WOMEN, null, null);
        PlayingClass jugend = new PlayingClass("Jugend", ClassType.YOUTH, null, null);
        PlayingClass maedchen = new PlayingClass("Mädchen", ClassType.GIRLS, null, null);

        // Erwachsene: alle, auch Jugendliche und Mädchen
        for (Player p : List.of(jonas, finn, lea, mia, otto, ohneJg)) {
            assertEquals(PlayingClass.Fit.YES, erwachsene.fit(p, DAY));
        }
        // Damen: nur weiblich, jedes Alter (auch Mädchen unter 21)
        assertEquals(PlayingClass.Fit.YES, damen.fit(lea, DAY));
        assertEquals(PlayingClass.Fit.YES, damen.fit(mia, DAY));
        assertEquals(PlayingClass.Fit.NO, damen.fit(jonas, DAY));
        // Jugend: unter 21, weiblich und männlich
        assertEquals(PlayingClass.Fit.YES, jugend.fit(finn, DAY));
        assertEquals(PlayingClass.Fit.YES, jugend.fit(mia, DAY));
        assertEquals(PlayingClass.Fit.NO, jugend.fit(jonas, DAY));
        assertEquals(PlayingClass.Fit.UNKNOWN, jugend.fit(ohneJg, DAY));
        // Mädchen: unter 21 und weiblich
        assertEquals(PlayingClass.Fit.YES, maedchen.fit(mia, DAY));
        assertEquals(PlayingClass.Fit.NO, maedchen.fit(finn, DAY));
        assertEquals(PlayingClass.Fit.NO, maedchen.fit(lea, DAY));

        Player unbekannt = player("X", "Y", Sex.UNKNOWN, 2010);
        assertEquals(PlayingClass.Fit.UNKNOWN, maedchen.fit(unbekannt, DAY));
        assertEquals(PlayingClass.Fit.YES, jugend.fit(unbekannt, DAY));
    }

    @Test
    void jugendGrenzeUnter21() {
        PlayingClass jugend = new PlayingClass("Jugend", ClassType.YOUTH, null, null);
        assertEquals(2006, PlayingClass.firstYouthBirthyear(DAY));
        assertEquals(PlayingClass.Fit.YES, jugend.fit(player("A", "A", Sex.MALE, 2006), DAY));
        assertEquals(PlayingClass.Fit.NO, jugend.fit(player("B", "B", Sex.MALE, 2005), DAY));

        // mit genauem Geburtsdatum zählt das Alter am Turniertag
        Player wird21NachTurnier = new Player("C", "C");
        wird21NachTurnier.setBirthdate(LocalDate.of(2005, 12, 1)); // am 14.11.2026 noch 20
        assertEquals(PlayingClass.Fit.YES, jugend.fit(wird21NachTurnier, DAY));
        Player schon21 = new Player("D", "D");
        schon21.setBirthdate(LocalDate.of(2005, 11, 14)); // am Turniertag 21 geworden
        assertEquals(PlayingClass.Fit.NO, jugend.fit(schon21, DAY));
    }

    @Test
    void zusaetzlicheJahrgangsgrenzen() {
        PlayingClass u15w = new PlayingClass("Mädchen U15", ClassType.GIRLS, 2012, null);
        assertEquals(PlayingClass.Fit.YES, u15w.fit(mia, DAY));
        assertEquals(PlayingClass.Fit.NO, u15w.fit(player("E", "E", Sex.FEMALE, 2008), DAY), "zu alt für U15");

        PlayingClass senioren = new PlayingClass("Senioren 40", ClassType.ADULTS, null, 1986);
        assertEquals(PlayingClass.Fit.YES, senioren.fit(otto, DAY));
        assertEquals(PlayingClass.Fit.NO, senioren.fit(jonas, DAY));
        assertFalse(senioren.acceptsEveryone());
        assertTrue(new PlayingClass("Herren A").acceptsEveryone());
    }

    @Test
    void kriterienText() {
        assertEquals("Einzel · Mädchen (nur weiblich, unter 21 = Jg. 2006 und jünger) · Jg. 2012 und jünger",
                new PlayingClass("a", ClassType.GIRLS, 2012, null).getCriteriaText(DAY));
        assertEquals("Einzel · Erwachsene (weiblich und männlich) · Jg. 1986 und älter",
                new PlayingClass("a", ClassType.ADULTS, null, 1986).getCriteriaText(DAY));
        assertEquals("Einzel · Damen (nur weiblich)", new PlayingClass("a", ClassType.WOMEN, null, null).getCriteriaText(DAY));
    }

    @Test
    void klassentypAusClickTtNamenSchaetzen() {
        assertEquals(ClassType.ADULTS, ClassType.guess("Herren Einzel", Sex.MALE));
        assertEquals(ClassType.WOMEN, ClassType.guess("Damen Einzel (TTR bis 1500)", Sex.UNKNOWN));
        assertEquals(ClassType.WOMEN, ClassType.guess("Einzel", Sex.FEMALE));
        assertEquals(ClassType.YOUTH, ClassType.guess("Jungen 18 Einzel", Sex.MALE));
        assertEquals(ClassType.YOUTH, ClassType.guess("Jugend U19", Sex.UNKNOWN));
        assertEquals(ClassType.GIRLS, ClassType.guess("Mädchen U15 Einzel", Sex.FEMALE));
        assertEquals(ClassType.GIRLS, ClassType.guess("Schülerinnen B", Sex.UNKNOWN));
        assertEquals(ClassType.ADULTS, ClassType.guess("Senioren 40", Sex.UNKNOWN));
    }

    @Test
    void manuellZuordnenUndEntfernen() {
        Tournament t = tournament();
        PlayingClass damen = t.addClass(new PlayingClass("Damen", ClassType.WOMEN, null, null));
        assertTrue(t.assign(lea, damen));
        assertFalse(t.assign(lea, damen), "nicht doppelt in derselben Klasse");
        assertTrue(t.assign(jonas, damen), "von Hand auch außerhalb der Kriterien möglich");
        assertEquals(2, t.countIn(damen));
        assertTrue(t.unassign(jonas, damen));
        assertEquals(List.of(lea), t.playersIn(damen));
    }

    @Test
    void jugendlicheDuerfenAuchBeiErwachsenenStarten() {
        Tournament t = tournament();
        PlayingClass jugend = t.addClass(new PlayingClass("Jugend", ClassType.YOUTH, null, null));
        PlayingClass erwachsene = t.addClass(new PlayingClass("Erwachsene", ClassType.ADULTS, null, null));
        t.assign(finn, jugend);
        t.assign(finn, erwachsene);
        assertEquals(2, finn.getClassNames().size());
    }

    @Test
    void zuordnenMeldetAnUndLoeschtWartelistenHinweis() {
        Tournament t = tournament();
        PlayingClass c = t.addClass(new PlayingClass("Damen", ClassType.WOMEN, null, null));
        lea.addWaitingList("Damen");
        assertFalse(lea.isRegistered());
        t.assign(lea, c);
        assertTrue(lea.isRegistered());
        assertTrue(lea.getWaitingLists().isEmpty());
        t.unassign(lea, c);
        assertFalse(lea.isRegistered(), "verfügbar = nicht angemeldet");
    }

    @Test
    void klassennameEindeutigUndUmbenennenZiehtMeldungenMit() {
        Tournament t = tournament();
        PlayingClass c = t.addClass(new PlayingClass("Herren"));
        assertThrows(IllegalArgumentException.class, () -> t.addClass(new PlayingClass("herren")));
        t.assign(jonas, c);
        t.renameClass(c, "Herren A");
        assertEquals("Herren A", c.getName());
        assertEquals(java.util.Set.of("Herren A"), jonas.getClassNames());
        PlayingClass d = t.addClass(new PlayingClass("Damen"));
        assertThrows(IllegalArgumentException.class, () -> t.renameClass(d, "Herren A"));
    }

    @Test
    void klasseLoeschenEntferntMeldungen() {
        Tournament t = tournament();
        PlayingClass c = t.addClass(new PlayingClass("Herren"));
        t.assign(jonas, c);
        t.removeClass(c);
        assertTrue(jonas.getClassNames().isEmpty());
        assertTrue(t.getClasses().isEmpty());
    }

    @Test
    void ohneTurnierdatumZaehltHeute() {
        Tournament t = new Tournament();
        assertEquals(LocalDate.now(), t.referenceDate());
        t.setDate(DAY);
        assertEquals(DAY, t.referenceDate());
    }

    @Test
    void importMeldungenLegenKlassenAn() {
        Tournament t = new Tournament();
        Player p = player("A", "B", Sex.MALE, 1990);
        p.addToClass("Herren Einzel");
        t.merge(List.of(p));
        assertTrue(t.findClass("Herren Einzel").isPresent());
    }
}
