/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.PlayerFilter;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.28.0: Suche über Vorname/Nachname/Verein, Tischnamen, Platzhalter „Sieger/in“/„Verlierer/in Spiel n“. */
class Version028Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    @Test
    void sucheUeberVornameNachnameUndVerein() {
        Player r = new Player("Richard", "Michaelis");
        r.setClubName("Grün-Weiß Harburg e.V.");
        for (String q : List.of("Richard Michaelis", "Richard", "Michaelis", "Richard Grün", "Richard- Grün-weiß",
                "Michaelis Harburg", "Harburg", "michaelis RICHARD", "harb", "gruen", "  ")) {
            assertTrue(PlayerFilter.matchesPerson(r, q), q);
        }
        for (String q : List.of("Richard Meier", "Hamburg", "Michaelis Altona")) {
            assertFalse(PlayerFilter.matchesPerson(r, q), q);
        }
    }

    @Test
    void doppelWirdUeberEinePersonGefunden() {
        Tournament t = new Tournament();
        Player a = new Player("Richard", "Michaelis");
        a.setClubName("Grün-Weiß Harburg");
        Player b = new Player("Tom", "Schulz");
        b.setClubName("TSV Altona");
        t.merge(List.of(a, b));
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        t.assign(a, hd);
        t.assign(b, hd);
        DoublesPair pair = t.createPair(hd, a, b);
        String id = "pair:" + pair.getId();
        assertTrue(t.entrantMatches(id, "Richard Harburg"));
        assertTrue(t.entrantMatches(id, "Altona"));
        assertFalse(t.entrantMatches(id, "Richard Altona"), "alle Wörter bei derselben Person");
        assertTrue(t.entrantMatches(a.getId(), "Michaelis"));
    }

    @Test
    void tischnamenBis30ZeichenUndGespeichert() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(4);
        assertEquals("Tisch 2", t.tableName(2));
        t.setTableName(2, "  Halle 2 – Center Court  ");
        assertEquals("Halle 2 – Center Court", t.tableName(2));
        t.setTableName(3, "x".repeat(45));
        assertEquals(30, t.tableName(3).length());
        Tournament back = roundTrip(t);
        assertEquals("Halle 2 – Center Court", back.tableName(2));
        assertEquals("x".repeat(30), back.tableName(3));
        assertEquals("Tisch 1", back.tableName(1));
        back.setTableName(2, "");
        assertEquals("Tisch 2", back.tableName(2), "leer = Standardname");
        assertFalse(back.getTableNames().containsKey(2));
    }

    @Test
    void platzhalterSiegerInUndVerliererInSpiel() {
        Tournament t = new Tournament();
        t.setTableCount(2);
        PlayingClass c = t.addClass(new PlayingClass("KO"));
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Player p = new Player("V" + i, "N" + i);
            p.setSex(Sex.MALE);
            p.setTtr(2000 - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        t.drawKnockout(c, true, new Random(1));
        t.setThirdPlace(c, true);
        KnockoutBracket b = new KnockoutBracket(t.drawSlots(c));
        KnockoutBracket.Round semis = b.getRounds().get(b.getRounds().size() - 2);
        assertTrue(semis.matches().get(0).a().label().startsWith("Sieger/in Spiel "), semis.matches().get(0).a().label());
        assertEquals("Verlierer/in Spiel " + semis.matches().get(0).number(), t.thirdPlaceLabel(c, 0));
        assertEquals("Verlierer/in Spiel " + semis.matches().get(1).number(), t.thirdPlaceLabel(c, 1));
    }
}
