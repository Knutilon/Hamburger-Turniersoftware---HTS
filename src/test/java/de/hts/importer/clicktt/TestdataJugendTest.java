package de.hts.importer.clicktt;

import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.KnockoutBracket;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Prüft die mitgelieferte Testdatei „testturnier-jugend.xml“ von Import bis Auslosung. */
class TestdataJugendTest {

    @Test
    void testdateiImportierenUndAuslosen() throws Exception {
        ClickTtXmlImporter.ImportResult r;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            r = new ClickTtXmlImporter().importStream(in);
        }
        assertEquals(48, r.players().size());
        assertEquals(2, r.competitions().size());
        var girls = r.competitions().get(0);
        var boys = r.competitions().get(1);
        assertEquals("Mädchen U15 Einzel", girls.name());
        assertEquals(18, girls.entries());
        assertEquals(2, girls.waitingList());
        assertEquals(Sex.FEMALE, girls.sex());
        assertEquals(ClassType.GIRLS, ClassType.guess(girls.name(), girls.sex()));
        assertEquals("Jungen U19 Einzel", boys.name());
        assertEquals(30, boys.entries());
        assertEquals(2, boys.waitingList());
        assertEquals(ClassType.YOUTH, ClassType.guess(boys.name(), boys.sex()));
        assertTrue(r.players().stream().allMatch(p -> p.getTtr() != null && p.getBirthyear() != null));

        // so, wie die Oberfläche importiert
        Tournament t = new Tournament();
        for (var ci : r.competitions()) {
            t.addClass(new PlayingClass(ci.name(), Discipline.SINGLES, ClassType.guess(ci.name(), ci.sex()), null, null));
        }
        t.merge(r.players());
        PlayingClass m15 = t.findClass("Mädchen U15 Einzel").orElseThrow();
        PlayingClass j19 = t.findClass("Jungen U19 Einzel").orElseThrow();
        assertEquals(16, t.countIn(m15), "18 Meldungen, davon 2 Warteliste");
        assertEquals(28, t.countIn(j19), "30 Meldungen, davon 2 Warteliste");
        assertEquals(4, t.getPlayers().stream().filter(p -> !p.isRegistered()).count());

        // Mädchen: Gruppen à 4 nach Setzung -> 4 Gruppen, 24 Spiele
        m15.setGroupsBySize(true);
        m15.setGroupSize(4);
        t.drawGroups(m15, true, new Random(1));
        assertEquals(4, m15.getGroups().size());
        assertEquals(24, t.matchesOf(m15).size());
        int bestTtr = t.playersIn(m15).stream().mapToInt(Player::getTtr).max().orElseThrow();
        assertEquals(bestTtr, t.groupsOf(m15).get(0).get(0).ttr(), "höchster TTR ist Kopf von Gruppe A");

        // Jungen: KO nach Setzung -> 32er-Feld, 4 Freilose, 27 Spiele
        t.drawKnockout(j19, true, new Random(2));
        assertEquals(32, j19.getDraw().size());
        assertEquals(27, new KnockoutBracket(t.drawSlots(j19)).playedMatches());
        assertEquals(27, t.matchesOf(j19).size());
        assertEquals(12, t.matchesOf(j19).stream().filter(de.hts.tournament.Match::isReady).count(),
                "Runde 1: 16 Spiele, davon 4 mit Freilos");
    }
}
