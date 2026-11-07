package de.hts.player;

import de.hts.player.PlayerFilter.RegistrationFilter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlayerFilterTest {

    private static Player player(String first, String last, Integer year, LocalDate date) {
        Player p = new Player(first, last);
        p.setBirthyear(year);
        p.setBirthdate(date);
        return p;
    }

    private static PlayerFilter byName(String first, String last) {
        return new PlayerFilter(first, last, null, null, RegistrationFilter.ALL);
    }

    private static PlayerFilter byBirth(String birth) {
        return new PlayerFilter(null, null, birth, null, RegistrationFilter.ALL);
    }

    @Test
    void sucheNachVornameUndNachname() {
        Player p = player("Jonas", "Brückner", 1994, null);
        assertTrue(byName("jon", null).test(p));
        assertTrue(byName(null, "brück").test(p));
        assertTrue(byName("Jonas", "Brückner").test(p));
        assertFalse(byName("Malte", null).test(p));
        assertFalse(byName("Jonas", "Meier").test(p), "beide Felder müssen passen");
        // Vorname-Suche findet nicht im Nachnamen
        assertFalse(byName("brück", null).test(p));
    }

    @Test
    void umlauteUndAkzenteEgal() {
        Player p = player("Finn", "Möller", 2008, null);
        assertTrue(byName(null, "möller").test(p));
        assertTrue(byName(null, "moller").test(p));
        assertTrue(byName(null, "moeller").test(p));
        assertTrue(byName(null, "MÖLLER").test(p));
        assertTrue(byName(null, "ostergaard").test(player("Malte", "Østergaard", 1988, null)));
        assertTrue(byName("ines", "garcia").test(player("Ines", "García", 1996, null)));
        assertTrue(byName(null, "strass").test(player("Eva", "Straße", 1990, null)));
    }

    @Test
    void sucheNachGeburtsdatum() {
        Player nurJahrgang = player("A", "B", 1994, null);
        Player mitDatum = player("C", "D", null, LocalDate.of(1994, 3, 14));

        assertTrue(byBirth("1994").test(nurJahrgang));
        assertTrue(byBirth("1994").test(mitDatum));
        assertTrue(byBirth("14.03.1994").test(mitDatum));
        assertTrue(byBirth("03.1994").test(mitDatum));
        assertTrue(byBirth("14.3.1994").test(mitDatum));
        assertTrue(byBirth("1994-03-14").test(mitDatum));
        assertFalse(byBirth("1995").test(mitDatum));
        assertFalse(byBirth("14.03.1994").test(nurJahrgang), "nur Jahrgang bekannt, Tag unbekannt");
        assertFalse(byBirth("1994").test(player("E", "F", null, null)));
    }

    @Test
    void filterNachStatus() {
        // 0.35: Status = Anwesenheit am Turniertag; Nachrücker = Warteliste laut Meldeliste
        Player angemeldet = new Player("A", "A");
        angemeldet.setAttendance(Attendance.PRESENT);
        Player warteliste = new Player("W", "W");
        warteliste.addWaitingList("Herren Einzel");
        Player abgemeldet = new Player("O", "O");
        abgemeldet.setAttendance(Attendance.WITHDRAWN);
        Player handy = new Player("H", "H");
        handy.setAttendance(Attendance.WITHDRAW_REQUESTED);

        PlayerFilter reg = new PlayerFilter(null, null, null, null, RegistrationFilter.REGISTERED);
        PlayerFilter not = new PlayerFilter(null, null, null, null, RegistrationFilter.NOT_REGISTERED);
        PlayerFilter wait = new PlayerFilter(null, null, null, null, RegistrationFilter.WAITING_LIST);
        PlayerFilter off = new PlayerFilter(null, null, null, null, RegistrationFilter.WITHDRAWN);
        PlayerFilter req = new PlayerFilter(null, null, null, null, RegistrationFilter.REQUESTED);

        assertTrue(reg.test(angemeldet));
        assertFalse(reg.test(warteliste));
        assertTrue(not.test(warteliste));
        assertFalse(not.test(angemeldet));
        assertTrue(wait.test(warteliste));
        assertFalse(wait.test(angemeldet));
        assertTrue(off.test(abgemeldet));
        assertFalse(off.test(handy));
        assertTrue(req.test(handy));
    }

    @Test
    void inKlasseEintragenLoeschtWartelistenHinweis() {
        Player p = new Player("G", "G");
        p.addWaitingList("Herren Einzel");
        assertFalse(p.isRegistered());
        p.addToClass("Herren Einzel");
        assertTrue(p.isRegistered());
        assertTrue(p.getWaitingLists().isEmpty());
        p.addWaitingList("Herren Einzel");
        assertTrue(p.getWaitingLists().isEmpty(), "wer in der Klasse ist, steht nicht auf deren Warteliste");
    }

    @Test
    void filterNachGeschlecht() {
        Player m = player("A", "B", null, null);
        m.setSex(Sex.MALE);
        assertTrue(new PlayerFilter(null, null, null, Sex.MALE, RegistrationFilter.ALL).test(m));
        assertFalse(new PlayerFilter(null, null, null, Sex.FEMALE, RegistrationFilter.ALL).test(m));
        assertTrue(new PlayerFilter(null, null, null, Sex.UNKNOWN, RegistrationFilter.ALL).test(m));
        assertTrue(PlayerFilter.NONE.test(m));
    }
}
