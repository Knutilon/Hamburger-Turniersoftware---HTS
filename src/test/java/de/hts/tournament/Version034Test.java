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

/** Neuerungen 0.34.0: KO-Qualifikation mit der Anzahl der Topplatzierten insgesamt (HEM-Modi). */
class Version034Test {

    private static PlayingClass groupsClass(Tournament t, String name, int n, int groupCount) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        Random rnd = new Random(7);
        for (int i = 1; i <= n; i++) {
            Player p = new Player(name + i, "N" + name + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1100 + rnd.nextInt(900));
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(groupCount);
        t.drawGroups(c, true, new Random(3));
        return c;
    }

    /** Höherer TTR gewinnt; Sätze variieren mit der Spielnummer, damit die Verhältnisse sich unterscheiden. */
    private static void playAll(Tournament t, PlayingClass c) {
        // 0.38: Qualifikationsphase startet erst per „Phase starten“; nach Runde 1 wird entschieden (hier: weitere Runde)
        if (c.isAwaitingStart()) t.resumeClass(c);
        if (t.qualificationDecisionPending(c)) t.decideQualification(c, false);
        if (c.isAwaitingStart()) t.resumeClass(c);
        for (int guard = 0; guard < 5000; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .findFirst().orElse(null);
            if (open == null) {
                if (t.qualificationDecisionPending(c)) {
                    t.decideQualification(c, false);
                    if (c.isAwaitingStart()) t.resumeClass(c);       // 0.41: Runde 2 per „Qualifikation Runde 2 starten“
                    continue;
                }
                return;
            }
            Entrant a = t.entrant(open, open.getA()), b = t.entrant(open, open.getB());
            boolean aWins = a.ttr() > b.ttr();
            int lose = 3 + open.getNumber() % 7;
            List<int[]> sets = open.getNumber() % 2 == 0
                    ? List.of(new int[]{11, lose}, new int[]{11, lose}, new int[]{11, lose})
                    : List.of(new int[]{11, lose}, new int[]{lose, 11}, new int[]{11, lose}, new int[]{11, lose});
            List<int[]> flipped = new ArrayList<>();
            for (int[] st : sets) flipped.add(new int[]{st[1], st[0]});
            t.recordResult(open, aWins ? sets : flipped, 0);
        }
    }

    private static long count(List<Tournament.Placing> rows, String info) {
        return rows.stream().filter(r -> r.info().equals(info)).count();
    }

    @Test
    void hemMaennlichVierundzwanzigInsgesamt() throws Exception {
        Tournament t = new Tournament();
        t.setTableCount(60);
        PlayingClass c = groupsClass(t, "Herren", 80, 16);         // 16 Gruppen à 5
        t.startClass(c);
        playAll(t, c);
        assertEquals(64, Tournament.qualificationAvailable(c, 1));
        assertThrows(IllegalArgumentException.class, () -> t.createQualificationPhase(c, 1, 22, true, 0, new Random(1)),
                "22 geht nicht auf");
        assertThrows(IllegalArgumentException.class, () -> t.createQualificationPhase(c, 1, 65, true, 0, new Random(1)));
        PlayingClass q = t.createQualificationPhase(c, 1, 24, true, 0, new Random(1));
        assertEquals(24, q.getQualKoTotal());
        assertEquals(16, q.getQualifiedDirect().size());
        assertEquals(24, q.getQualField().size());
        assertEquals(16, q.getQualPlace().values().stream().filter(p -> p == 2).count(), "alle Gruppenzweiten");
        assertEquals(8, q.getQualPlace().values().stream().filter(p -> p == 3).count(), "die 8 besten Dritten");
        // die ausgewählten Dritten haben das bessere Spiel-/Satz-/Punktverhältnis als die übrigen Dritten
        java.util.Set<String> inKo = q.getQualField().keySet();
        List<GroupStanding.Row> thirds = new ArrayList<>();
        for (int g = 0; g < 16; g++) thirds.add(t.standings(c, g).get(2));
        double worstIn = thirds.stream().filter(r -> inKo.contains(r.entrant().id())).mapToDouble(r -> ratio(r.score(), r.lost())).min().orElseThrow();
        double bestOut = thirds.stream().filter(r -> !inKo.contains(r.entrant().id())).mapToDouble(r -> ratio(r.score(), r.lost())).max().orElseThrow();
        assertTrue(worstIn >= bestOut);
        assertEquals(12, t.matchesOf(q).size(), "Runde 1: 12 Spiele");
        assertEquals(8, t.matchesOf(q).stream().filter(m -> q.getQualPlace().get(m.getB()) == 3 || q.getQualPlace().get(m.getA()) == 3).count(),
                "jeder Dritte gegen einen Zweiten");
        playAll(t, q);
        assertEquals(18, t.matchesOf(q).size(), "Runde 2: 6 Spiele");
        assertTrue(t.isPhaseComplete(q));
        var rows = t.finalResult(c).rows();
        assertEquals(16, count(rows, Tournament.QUALIFIED_BY_GROUP));
        assertEquals(6, count(rows, Tournament.QUALIFIED_BY_KO));
        assertEquals(6, count(rows, "Nachrücker 1. Ranges"));
        assertEquals(12, count(rows, "Nachrücker 2. Ranges"));
        // die 8 übrigen Dritten: nur Platzierte (Platz 3 ihrer Gruppe), danach die Vierten und Fünften
        assertEquals(8, rows.stream().filter(r -> r.info().endsWith("Platz 3")).count());
        assertEquals(80, rows.size(), "alle platziert, niemand doppelt");
        assertEquals(80, rows.stream().map(r -> r.entrant().id()).distinct().count());
        assertEquals(41, rows.stream().filter(r -> r.info().endsWith("Platz 3")).findFirst().orElseThrow().from());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TournamentFile.write(out, TournamentFile.Content.of(t));
        Tournament back = TournamentFile.read(new ByteArrayInputStream(out.toByteArray())).toTournament();
        PlayingClass bq = back.findClass(q.getName()).orElseThrow();
        assertEquals(24, bq.getQualKoTotal());
        assertEquals(q.getQualKoCounts(), bq.getQualKoCounts());
        assertEquals(rows.stream().map(Tournament.Placing::info).toList(),
                back.finalResult(back.findClass("Herren").orElseThrow()).rows().stream().map(Tournament.Placing::info).toList());
    }

    private static double ratio(int won, int lost) {
        return won + lost == 0 ? 0 : (double) won / (won + lost);
    }

    @Test
    void nachrueckerAbDemErstenNichtKoTeilnehmenden() {
        Tournament t = new Tournament();
        t.setTableCount(60);
        PlayingClass c = groupsClass(t, "Jugend", 80, 16);
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 1, 24, true, 1, new Random(1));
        playAll(t, q);
        var rows = t.finalResult(c).rows();
        // je Gruppe 1 Nachrücker ohne Spiel: 8 übrige Dritte + 8 Vierte (aus den Gruppen, deren Dritter in der KO ist)
        assertEquals(16, count(rows, "Nachrücker 3. Ranges"));
        assertEquals(0, rows.stream().filter(r -> r.info().endsWith("Platz 3")).count(), "kein Dritter nur platziert");
        assertEquals(8, rows.stream().filter(r -> r.info().endsWith("Platz 4")).count());
        assertEquals(80, rows.stream().map(r -> r.entrant().id()).distinct().count());
    }

    @Test
    void hemWeiblichOhneKoRunde() {
        Tournament t = new Tournament();
        t.setTableCount(20);
        PlayingClass c = groupsClass(t, "Damen", 22, 4);           // 4 Gruppen (6/6/5/5)
        t.startClass(c);
        playAll(t, c);
        PlayingClass q = t.createQualificationPhase(c, 3, 0, 2, 2, new Random(1));
        assertTrue(t.matchesOf(q).isEmpty());
        t.finishClass(q);
        var rows = t.finalResult(c).rows();
        assertEquals(12, count(rows, Tournament.QUALIFIED_BY_GROUP));
        assertEquals(8, count(rows, "Nachrücker 1. Ranges"), "Gruppenvierte und -fünfte");
        assertEquals(2, rows.stream().filter(r -> r.info().endsWith("Platz 6")).count());
    }
}
