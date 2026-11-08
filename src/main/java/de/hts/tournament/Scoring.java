package de.hts.tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tischtennis-Zählweise: Satzergebnisse prüfen, Kurzeingaben umwandeln, Spiele werten.
 *
 * <p><b>Gültiger Satz:</b> Der Sieger hat 11 Punkte und der Gegner höchstens 9 (11:0 … 11:9) – oder es gab
 * Verlängerung: dann gewinnt, wer zuerst 2 Punkte Vorsprung hat (12:10, 13:11, 14:12, … ohne Obergrenze).
 * Ungültig sind z. B. 13:14, 12:9, 11:10 oder 10:8.
 *
 * <p><b>Eingabe:</b> „11:8“, „7:11“, „12-14“ – oder kurz mit Vorzeichen, wobei die Zahl die Punkte des
 * Satzverlierers sind: „+8“ → 11:8, „-7“ → 7:11, „+0“ → 11:0, „+10“ → 12:10, „-12“ → 12:14.
 * Die erste Zahl gehört immer zur erstgenannten Partei (A).
 */
public final class Scoring {

    private Scoring() {
    }

    private static final Pattern FULL = Pattern.compile("^\\s*(\\d{1,2})\\s*[:\\-/ ]\\s*(\\d{1,2})\\s*$");
    /** Kurzform: „+8“, „-8“ – 0.38: auch ohne Vorzeichen („8“ = „+8“); auch das typografische Minus „−“. */
    private static final Pattern SHORT = Pattern.compile("^\\s*([+\\-\u2212]?)\\s*(\\d{1,2})\\s*$");

    /** Ist das ein gültiges Satzergebnis? */
    public static boolean isValidSet(int a, int b) {
        if (a < 0 || b < 0) return false;
        int w = Math.max(a, b), l = Math.min(a, b);
        if (w == 11) return l <= 9;
        return w > 11 && w - l == 2;
    }

    /**
     * Wandelt eine Eingabe in ein Satzergebnis {a, b} um (ohne Gültigkeitsprüfung des Ergebnisses).
     * {@code null}, wenn die Eingabe nicht lesbar ist.
     */
    public static int[] parseSet(String input) {
        if (input == null || input.isBlank()) return null;
        Matcher m = SHORT.matcher(input);
        if (m.matches()) {
            int loser = Integer.parseInt(m.group(2));
            int winner = loser <= 9 ? 11 : loser + 2;
            boolean minus = "-".equals(m.group(1)) || "\u2212".equals(m.group(1));
            return minus ? new int[]{loser, winner} : new int[]{winner, loser};
        }
        m = FULL.matcher(input);
        if (m.matches()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        return null;
    }

    /** Formatiert ein Satzergebnis, z. B. „11:8“. */
    public static String format(int[] set) {
        return set[0] + ":" + set[1];
    }

    /** Wertung eines Spiels. */
    public record Evaluation(int setsA, int setsB, int pointsA, int pointsB, boolean decided,
                             /** 0 = noch offen, 1 = A gewinnt, 2 = B gewinnt */ int winner,
                             /** Index des ersten ungültigen Satzes oder -1 */ int invalidSet,
                             String message) {
        public boolean isComplete() {
            return decided && invalidSet < 0 && message == null;
        }
    }

    /**
     * Wertet die Sätze eines Spiels aus.
     *
     * @param winningSets Gewinnsätze (3 = „Best of 5“)
     */
    public static Evaluation evaluate(List<int[]> sets, int winningSets) {
        int sa = 0, sb = 0, pa = 0, pb = 0;
        for (int i = 0; i < sets.size(); i++) {
            int[] s = sets.get(i);
            if (!isValidSet(s[0], s[1])) {
                return new Evaluation(sa, sb, pa, pb, false, 0, i,
                        (i + 1) + ". Satz " + format(s) + " ist kein gültiges Satzergebnis.");
            }
            if (sa == winningSets || sb == winningSets) {
                return new Evaluation(sa, sb, pa, pb, true, sa > sb ? 1 : 2, i,
                        "Das Spiel war nach dem " + i + ". Satz bereits entschieden.");
            }
            pa += s[0];
            pb += s[1];
            if (s[0] > s[1]) sa++;
            else sb++;
        }
        boolean decided = sa == winningSets || sb == winningSets;
        return new Evaluation(sa, sb, pa, pb, decided, decided ? (sa > sb ? 1 : 2) : 0, -1,
                decided ? null : "Noch nicht entschieden (" + sa + ":" + sb + " Sätze, " + winningSets + " Gewinnsätze).");
    }

    /** Sätze bei Aufgabe: 11:0 je Gewinnsatz für die Partei, die nicht aufgibt. */
    public static List<int[]> walkover(int retiredSide, int winningSets) {
        List<int[]> sets = new ArrayList<>();
        for (int i = 0; i < winningSets; i++) {
            sets.add(retiredSide == 1 ? new int[]{0, 11} : new int[]{11, 0});
        }
        return sets;
    }

    /** Liest eine gespeicherte Satzliste „11-8,7-11,…“. */
    public static List<int[]> decode(String s) {
        List<int[]> sets = new ArrayList<>();
        if (s == null || s.isBlank()) return sets;
        for (String part : s.split(",")) {
            String[] ab = part.trim().split("-");
            sets.add(new int[]{Integer.parseInt(ab[0]), Integer.parseInt(ab[1])});
        }
        return sets;
    }

    public static String encode(List<int[]> sets) {
        StringBuilder sb = new StringBuilder();
        for (int[] s : sets) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(s[0]).append('-').append(s[1]);
        }
        return sb.toString();
    }
}
