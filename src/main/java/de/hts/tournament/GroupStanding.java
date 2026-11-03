package de.hts.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gruppentabelle aus den abgeschlossenen Spielen einer Gruppe.
 *
 * <p>Reihenfolge: zuerst das <b>Spielverhältnis</b> (gewonnene Spiele). Bei Gleichstand je nach Einstellung
 * <ul>
 *   <li>{@link Tiebreak#SETS_POINTS}: Satzdifferenz, dann Balldifferenz (Punktverhältnis), dann gewonnene Sätze;</li>
 *   <li>{@link Tiebreak#HEAD_TO_HEAD}: direkter Vergleich – bei zwei Gleichplatzierten das Spiel gegeneinander,
 *       bei mehreren eine Tabelle nur aus den Spielen untereinander (Siege, Sätze, Bälle);
 *       danach Satz- und Balldifferenz aller Spiele.</li>
 * </ul>
 * Bleibt es gleich, entscheidet die Setzreihenfolge in der Gruppe.
 */
public final class GroupStanding {

    /** Regel bei gleichem Spielverhältnis. */
    public enum Tiebreak {
        SETS_POINTS("Satz- und Ballverhältnis"),
        HEAD_TO_HEAD("direkter Vergleich");

        private final String label;

        Tiebreak(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Eine Tabellenzeile. */
    public record Row(int place, Entrant entrant, int played, int won, int lost,
                      int setsWon, int setsLost, int pointsWon, int pointsLost,
                      int byes, Integer buchholz, Integer fineBuchholz) {
        public Row(int place, Entrant entrant, int played, int won, int lost,
                   int setsWon, int setsLost, int pointsWon, int pointsLost) {
            this(place, entrant, played, won, lost, setsWon, setsLost, pointsWon, pointsLost, 0, null, null);
        }

        /** Punkte im Schweizer System: Siege + Freilos. */
        public int score() {
            return won + byes;
        }

        public int setDiff() {
            return setsWon - setsLost;
        }

        public int pointDiff() {
            return pointsWon - pointsLost;
        }
    }

    private GroupStanding() {
    }

    private static final class Stat {
        final Entrant e;
        final int seedIndex;
        int played, won, lost, sw, sl, pw, pl;

        Stat(Entrant e, int seedIndex) {
            this.e = e;
            this.seedIndex = seedIndex;
        }
    }

    public static List<Row> compute(List<Entrant> members, List<Match> groupMatches, Tiebreak tiebreak) {
        Map<String, Stat> stats = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) stats.put(members.get(i).id(), new Stat(members.get(i), i));
        List<Match> done = groupMatches.stream().filter(m -> m.getStatus() == MatchStatus.DONE && m.winner() != null).toList();
        for (Match m : done) add(stats, m);

        List<Stat> list = new ArrayList<>(stats.values());
        Comparator<Stat> byWins = Comparator.comparingInt((Stat s) -> s.won).reversed();
        list.sort(byWins);
        List<Stat> ordered = new ArrayList<>();
        int i = 0;
        while (i < list.size()) {
            int j = i;
            while (j < list.size() && list.get(j).won == list.get(i).won) j++;
            List<Stat> block = new ArrayList<>(list.subList(i, j));
            if (block.size() > 1) block.sort(tieComparator(block, done, tiebreak));
            ordered.addAll(block);
            i = j;
        }
        List<Row> rows = new ArrayList<>();
        for (int k = 0; k < ordered.size(); k++) {
            Stat s = ordered.get(k);
            rows.add(new Row(k + 1, s.e, s.played, s.won, s.lost, s.sw, s.sl, s.pw, s.pl));
        }
        return rows;
    }

    private static void add(Map<String, Stat> stats, Match m) {
        Stat a = stats.get(m.getA()), b = stats.get(m.getB());
        if (a == null || b == null) return;
        boolean aWon = m.getA().equals(m.winner());
        a.played++;
        b.played++;
        if (aWon) {
            a.won++;
            b.lost++;
        } else {
            b.won++;
            a.lost++;
        }
        a.sw += m.getSetsA();
        a.sl += m.getSetsB();
        b.sw += m.getSetsB();
        b.sl += m.getSetsA();
        a.pw += m.pointsA();
        a.pl += m.pointsB();
        b.pw += m.pointsB();
        b.pl += m.pointsA();
    }

    private static Comparator<Stat> overall() {
        return Comparator.comparingInt((Stat s) -> s.sw - s.sl).reversed()
                .thenComparing(Comparator.comparingInt((Stat s) -> s.pw - s.pl).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> s.sw).reversed())
                .thenComparingInt(s -> s.seedIndex);
    }

    private static Comparator<Stat> tieComparator(List<Stat> block, List<Match> done, Tiebreak tiebreak) {
        if (tiebreak == Tiebreak.SETS_POINTS) return overall();
        // direkter Vergleich: Tabelle nur aus den Spielen der Gleichplatzierten untereinander
        Map<String, Stat> mini = new HashMap<>();
        for (Stat s : block) mini.put(s.e.id(), new Stat(s.e, s.seedIndex));
        for (Match m : done) {
            if (mini.containsKey(m.getA()) && mini.containsKey(m.getB())) add(mini, m);
        }
        Comparator<Stat> h2h = Comparator.comparingInt((Stat s) -> mini.get(s.e.id()).won).reversed()
                .thenComparing(Comparator.comparingInt((Stat s) -> mini.get(s.e.id()).sw - mini.get(s.e.id()).sl).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> mini.get(s.e.id()).pw - mini.get(s.e.id()).pl).reversed());
        return h2h.thenComparing(overall());
    }
}
