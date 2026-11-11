/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoublesTest {

    private static Player p(String first, String last) {
        Player p = new Player(first, last);
        p.setSex(Sex.MALE);
        p.setBirthyear(1990);
        p.setInternalNr("NU-" + first);
        return p;
    }

    private final Player a = p("A", "Anders");
    private final Player b = p("B", "Berg");
    private final Player c = p("C", "Clausen");
    private final Player d = p("D", "Dahl");

    private Tournament tournament() {
        Tournament t = new Tournament();
        t.merge(List.of(a, b, c, d));
        return t;
    }

    @Test
    void paarungBildenUndMeldungenSetzen() {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("Herren Doppel", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        DoublesPair pair = t.createPair(hd, a, b);
        assertTrue(pair.isComplete());
        assertEquals(List.of(pair), t.pairsIn(hd));
        assertTrue(a.isInClass("Herren Doppel"));
        assertTrue(b.isInClass("Herren Doppel"));
        assertEquals(b, pair.partnerOf(a));
        assertEquals("Anders, A / Berg, B", pair.getDisplayName());
    }

    @Test
    void paarungSchrittweisePerDragAndDrop() {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        DoublesPair pair = t.createPair(hd, a, null);
        assertFalse(pair.isComplete());
        t.addToPair(pair, b);
        assertTrue(pair.isComplete());
        assertThrows(IllegalStateException.class, () -> t.addToPair(pair, c), "höchstens zwei Personen");
    }

    @Test
    void personWechseltPaarungInnerhalbDerKlasse() {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        DoublesPair ab = t.createPair(hd, a, b);
        DoublesPair c0 = t.createPair(hd, c, null);
        t.addToPair(c0, b); // B wechselt von A zu C
        assertEquals(b, c0.partnerOf(c));
        assertFalse(ab.isComplete());
        assertEquals(a, ab.getFirst());
        assertEquals(2, t.pairsIn(hd).size());
        // eine Person ist pro Doppelklasse nur in einer Paarung
        assertEquals(1, t.getPairs().stream().filter(pp -> pp.contains(b)).count());
    }

    @Test
    void ausPaarungEntfernenUndAufloesen() {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        DoublesPair pair = t.createPair(hd, a, b);
        t.removeFromPair(pair, a);
        assertFalse(a.isInClass("HD"));
        assertEquals(b, pair.getFirst());
        assertNull(pair.getSecond());
        t.removeFromPair(pair, b);
        assertTrue(t.pairsIn(hd).isEmpty(), "leere Paarung verschwindet");

        DoublesPair cd = t.createPair(hd, c, d);
        t.removePair(cd);
        assertTrue(t.pairsIn(hd).isEmpty());
        assertFalse(c.isInClass("HD"));
    }

    @Test
    void umbenennenLoeschenUndTeilnehmendeEntfernen() {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        t.createPair(hd, a, b);
        t.createPair(hd, c, d);
        t.renameClass(hd, "Herren Doppel");
        assertEquals(2, t.pairsIn(hd).size());
        t.removePlayers(List.of(a));
        assertEquals(2, t.pairsIn(hd).size());
        assertFalse(t.pairsIn(hd).get(0).isComplete());
        t.removeClass(hd);
        assertTrue(t.getPairs().isEmpty());
    }

    @Test
    void einzelNachDoppelUndZurueck() {
        Tournament t = tournament();
        PlayingClass c1 = t.addClass(new PlayingClass("Mixed"));
        t.assign(a, c1);
        t.assign(b, c1);
        t.changeDiscipline(c1, Discipline.DOUBLES);
        assertEquals(2, t.pairsIn(c1).size(), "je eine Paarung ohne Partner/in");
        t.changeDiscipline(c1, Discipline.SINGLES);
        assertTrue(t.getPairs().isEmpty());
        assertEquals(2, t.countIn(c1), "Zuordnungen bleiben");
    }

    @Test
    void doppelWerdenGespeichertUndGeladen() throws Exception {
        Tournament t = tournament();
        PlayingClass hd = t.addClass(new PlayingClass("HD", Discipline.DOUBLES, ClassType.ADULTS, null, null));
        t.addClass(new PlayingClass("HE"));
        t.createPair(hd, a, b);
        t.createPair(hd, c, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament loaded = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();

        PlayingClass lhd = loaded.findClass("HD").orElseThrow();
        assertTrue(lhd.isDoubles());
        assertFalse(loaded.findClass("HE").orElseThrow().isDoubles());
        List<DoublesPair> pairs = loaded.pairsIn(lhd);
        assertEquals(2, pairs.size());
        assertEquals("Anders", pairs.get(0).getFirst().getLastname());
        assertEquals("Berg", pairs.get(0).getSecond().getLastname());
        assertEquals("Clausen", pairs.get(1).getFirst().getLastname());
        assertNull(pairs.get(1).getSecond());
        assertSame(loaded.getPlayers().stream().filter(p -> p.getLastname().equals("Anders")).findFirst().orElseThrow(),
                pairs.get(0).getFirst(), "Paarung verweist auf dieselbe Person wie die Teilnehmendenliste");
        assertFalse(loaded.isModified());
    }

    @Test
    void importAusClickTtErkenntDoppelUndPaarungen() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/clicktt-beispiel.xml")) {
            var r = new ClickTtXmlImporter().importStream(in);
            var hd = r.competitions().stream().filter(ci -> ci.name().equals("Herren Doppel")).findFirst().orElseThrow();
            assertTrue(hd.doubles());
            assertFalse(r.competitions().get(0).doubles());
            assertEquals(2, r.pairs().size());
        }
    }
}
