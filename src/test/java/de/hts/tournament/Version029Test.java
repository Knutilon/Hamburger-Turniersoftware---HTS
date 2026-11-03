package de.hts.tournament;

import de.hts.persistence.TournamentFile;
import de.hts.player.Player;
import de.hts.player.Sex;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Neuerungen 0.29.0: Setzung „Gruppenkopf gesetzt“, Präsentationsansicht (Stationen, Einstellungen). */
class Version029Test {

    private static Tournament roundTrip(Tournament t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        return TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
    }

    private static List<Player> players(Tournament t, String prefix, int n, int ttrBase) {
        List<Player> ps = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Player p = new Player(prefix + i, "N" + prefix + i);
            p.setInternalNr(prefix + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(ttrBase - i * 10);
            ps.add(p);
        }
        t.merge(ps);
        return ps;
    }

    @Test
    void gruppenkopfGesetzt() {
        List<Entrant> es = new ArrayList<>();
        for (int i = 1; i <= 16; i++) es.add(new Entrant("p" + i, "P" + i, 2000 - i * 10, i <= 4 ? "Verein A" : "Verein " + (i % 3)));
        for (int seed = 0; seed < 30; seed++) {
            List<List<Entrant>> g = GroupDraw.draw(es, 4, GroupDraw.Seeding.HEADS, true, new Random(seed));
            for (int k = 0; k < 4; k++) {
                assertEquals("p" + (k + 1), g.get(k).get(0).id(), "Nr. " + (k + 1) + " ist Kopf von Gruppe " + GroupDraw.groupName(k));
                assertEquals(4, g.get(k).size());
                // je Gruppe genau eine/r aus jeder Stärkegruppe 5–8, 9–12, 13–16
                for (int pot = 1; pot < 4; pot++) {
                    int p0 = pot;
                    assertEquals(1, g.get(k).stream().filter(e -> (Integer.parseInt(e.id().substring(1)) - 1) / 4 == p0).count());
                }
            }
        }
        assertEquals("TTR Setzung (Schlange)", GroupDraw.Seeding.SNAKE.getLabel());
        assertEquals("TTR Setzung (Stärkegruppen)", GroupDraw.Seeding.POTS.getLabel());
        assertEquals("Gruppenkopf gesetzt", GroupDraw.Seeding.HEADS.getLabel());
    }

    @Test
    void gruppenkopfGesetztWirdGespeichert() throws Exception {
        Tournament t = new Tournament();
        PlayingClass c = t.addClass(new PlayingClass("H"));
        players(t, "H", 12, 2000).forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(3);
        c.setGroupSeeding(GroupDraw.Seeding.HEADS);
        t.drawGroups(c, true, new Random(1));
        for (int k = 0; k < 3; k++) assertEquals(1990 - 10 * k, t.groupsOf(c).get(k).get(0).ttr(), "Gruppenkopf " + k);
        Tournament back = roundTrip(t);
        assertEquals(GroupDraw.Seeding.HEADS, back.findClass("H").orElseThrow().getGroupSeeding());
    }

    @Test
    void praesentationStationenUndReihenfolge() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(8);
        t.setTest(true);
        PlayingClass a = t.addClass(new PlayingClass("A"));
        players(t, "A", 8, 2000).forEach(p -> t.assign(p, a));
        a.setGroupsBySize(false);
        a.setGroupCount(2);
        t.drawGroups(a, true, new Random(1));
        PlayingClass b = t.addClass(new PlayingClass("B"));
        players(t, "B", 8, 1900).forEach(p -> t.assign(p, b));
        t.drawKnockout(b, true, new Random(2));
        PlayingClass idle = t.addClass(new PlayingClass("C nicht gestartet"));
        players(t, "C", 4, 1500).forEach(p -> t.assign(p, idle));
        t.drawKnockout(idle, true, new Random(3));
        t.startClass(a);
        t.startClass(b);
        t.simulateAll(new Random(4));
        t.finishClass(b);                                              // B beendet
        PlayingClass a2 = t.createNextPhase(a, TournamentMode.KNOCKOUT, 2, 4, new Random(5));   // A läuft in Phase 2

        List<String> keys = PresentationPlan.all(t).stream().map(PresentationPlan.Station::key).toList();
        assertEquals(List.of("phase:" + a2.getName(), "tables", "next16", "phase:A", "phase:B", "result:B"), keys);
        assertTrue(PresentationPlan.all(t).get(3).finished(), "Phase 1 von A ist abgeschlossen");

        t.setPresentationOn("tables", false);
        t.setPresentationOn("result:B", false);
        t.setPresentationSeconds(30);
        Tournament back = roundTrip(t);
        assertEquals(30, back.getPresentationSeconds());
        assertEquals(java.util.Set.of("tables", "result:B"), back.getPresentationOff());
        assertEquals(4, PresentationPlan.active(back).size());
    }
}
