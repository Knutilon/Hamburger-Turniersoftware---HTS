package de.hts.tournament;

import de.hts.player.Attendance;
import de.hts.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.39.0 */
class Version039Test {

    private static Player p(String name, Integer ttr) {
        Player x = new Player(name, "N" + name);
        x.setTtr(ttr);
        x.addRegistration("K");
        return x;
    }

    @Test
    void setzplatzNachTtrMitGleichstandUndOhneTtr() {
        Tournament t = new Tournament();
        PlayingClass c = t.addClass(new PlayingClass("K"));
        assertTrue(t.seedPositions(c).isEmpty(), "nur bei per XML importierten Klassen");
        c.setImportedFromXml(true);
        Player a = p("A", 1800), b = p("B", 1700), b2 = p("B2", 1700), d = p("D", 1500), none = p("O", null), gone = p("W", 1900);
        Player reserve = new Player("R", "NR");
        reserve.setTtr(2000);
        reserve.addWaitingList("K");
        t.merge(List.of(a, b, b2, d, none, gone, reserve));
        t.setAttendance(gone, Attendance.WITHDRAWN);
        Map<Player, Integer> s = t.seedPositions(c);
        assertEquals(1, s.get(a));
        assertEquals(2, s.get(b));
        assertEquals(2, s.get(b2), "gleicher TTR = gleicher Setzplatz");
        assertEquals(4, s.get(d), "danach geht es mit 4 weiter");
        assertEquals(5, s.get(none), "ohne TTR ganz unten");
        assertNull(s.get(gone), "endgültig Abgemeldete ohne Setzplatz");
        assertNull(s.get(reserve), "Nachrücker ohne Setzplatz");
    }
}
