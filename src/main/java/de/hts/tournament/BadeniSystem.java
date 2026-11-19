/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.2 (#95): Schweizer System nach Badeni-Art (BaTTV, A. Enichlmayr 2014). Alle spielen in einem Feld; je Runde bilden
 * die Spieler mit gleich vielen Siegen eine Siegegruppe. Innerhalb der Gruppe (nach Reihung) spielt – wie beim
 * Skispringen – der Erste gegen den Letzten, der Zweite gegen den Vorletzten …, die Mitte gegeneinander.
 * <ul>
 *   <li>Reihung: 1. Siege (ein Freilos zählt als Sieg), 2. TTR, 3. Sätze – keine Buchholzzahl.</li>
 *   <li>Ungerades Feld: der beste Spieler der unteren Hälfte der Reihung bekommt ein Freilos, niemand zweimal.</li>
 *   <li>Ungerade Siegegruppe: der Schwächste der Gruppe spielt in der nächstniedrigeren Gruppe mit.</li>
 *   <li>Wiederholte Begegnungen werden vermieden (nächstmöglicher Gegner), nur wenn es nicht anders geht, wiederholt.</li>
 * </ul>
 */
public final class BadeniSystem {

    /** Mindestens 5 Runden (Unterlage BaTTV). */
    public static final int MIN_ROUNDS = 5;

    private BadeniSystem() {
    }

    /** Standard-Rundenzahl: mindestens 5, höchstens so viele wie ohne Wiederholung möglich. */
    public static int defaultRounds(int n) {
        return Math.max(1, Math.min(MIN_ROUNDS, SwissSystem.maxRounds(n)));
    }

    /** Tabelle nach Badeni: Siege (+ Freilos), TTR, Satzdifferenz, Ballverhältnis, festes Los. */
    public static List<GroupStanding.Row> standings(List<GroupStanding.Row> swissRows) {
        List<GroupStanding.Row> rows = new ArrayList<>(swissRows);
        Map<String, Integer> lot = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) lot.put(rows.get(i).entrant().id(), i);
        rows.sort(Comparator.comparingInt(GroupStanding.Row::score).reversed()
                .thenComparing(r -> r.entrant().ttr() == null ? Integer.MIN_VALUE : r.entrant().ttr(), Comparator.reverseOrder())
                .thenComparing(GroupStanding.Row::setDiff, Comparator.reverseOrder())
                .thenComparing(GroupStanding.Row::pointDiff, Comparator.reverseOrder())
                .thenComparingInt(r -> lot.get(r.entrant().id())));
        List<GroupStanding.Row> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            GroupStanding.Row r = rows.get(i);
            out.add(new GroupStanding.Row(i + 1, r.entrant(), r.played(), r.won(), r.lost(), r.setsWon(), r.setsLost(),
                    r.pointsWon(), r.pointsLost(), r.byes(), null, null));
        }
        return out;
    }

    /**
     * Paarungen einer Runde.
     *
     * @param ranked IDs nach aktueller Reihung (ohne Abgemeldete und Pausierende)
     * @param wins   Siege je ID (Freilos zählt als Sieg)
     * @param played bereits gespielte Paarungen ({@link SwissSystem#key})
     * @param hadBye wer schon ein Freilos hatte
     */
    public static SwissSystem.Round nextRound(List<String> ranked, Map<String, Integer> wins, Set<String> played, Set<String> hadBye) {
        List<String> ids = new ArrayList<>(ranked);
        String bye = null;
        if (ids.size() % 2 == 1) {
            int half = ids.size() / 2;
            for (int i = half; i < ids.size() && bye == null; i++) if (!hadBye.contains(ids.get(i))) bye = ids.get(i);
            for (int i = half - 1; i >= 0 && bye == null; i--) if (!hadBye.contains(ids.get(i))) bye = ids.get(i);
            if (bye == null) bye = ids.get(half);
            ids.remove(bye);
        }
        // Siegegruppen in Reihenfolge der Reihung
        List<List<String>> groups = new ArrayList<>();
        Integer last = null;
        for (String id : ids) {
            int w = wins.getOrDefault(id, 0);
            if (last == null || w != last) {
                groups.add(new ArrayList<>());
                last = w;
            }
            groups.get(groups.size() - 1).add(id);
        }
        List<List<String>> pairedLists = new ArrayList<>();      // je gepaarter (ggf. zusammengelegter) Gruppe die Liste
        List<List<String[]>> pairedPairs = new ArrayList<>();
        List<String> carry = new ArrayList<>();
        for (int g = 0; g < groups.size(); g++) {
            List<String> list = new ArrayList<>(carry);
            list.addAll(groups.get(g));
            carry = new ArrayList<>();
            boolean lastGroup = g == groups.size() - 1;
            if (list.size() % 2 == 1 && !lastGroup) carry.add(list.remove(list.size() - 1));   // Schwächster rutscht eine Gruppe tiefer
            List<String[]> sub = fold(list, played, new int[]{100_000});
            if (sub == null && !lastGroup) {
                // ohne Wiederholung nicht paarbar: die ganze Gruppe spielt mit der nächsten zusammen
                carry.addAll(0, list);
                continue;
            }
            // letzte Gruppe nicht paarbar: die darüber liegenden Gruppen schrittweise wieder öffnen
            while (sub == null && !pairedLists.isEmpty()) {
                List<String> above = pairedLists.remove(pairedLists.size() - 1);
                pairedPairs.remove(pairedPairs.size() - 1);
                List<String> merged = new ArrayList<>(above);
                merged.addAll(list);
                list = merged;
                sub = fold(list, played, new int[]{200_000});
            }
            if (sub == null) sub = foldAllowRepeat(list);
            pairedLists.add(list);
            pairedPairs.add(sub);
        }
        List<String[]> pairs = new ArrayList<>();
        for (List<String[]> p : pairedPairs) pairs.addAll(p);
        return new SwissSystem.Round(pairs, bye);
    }

    /** Erster gegen den letztmöglichen (noch nicht gespielt) usw. – Backtracking. */
    private static List<String[]> fold(List<String> remaining, Set<String> played, int[] budget) {
        if (remaining.isEmpty()) return new ArrayList<>();
        if (--budget[0] < 0) return null;
        String a = remaining.get(0);
        for (int j = remaining.size() - 1; j >= 1; j--) {
            String b = remaining.get(j);
            if (played.contains(SwissSystem.key(a, b))) continue;
            List<String> rest = new ArrayList<>(remaining);
            rest.remove(j);
            rest.remove(0);
            List<String[]> sub = fold(rest, played, budget);
            if (sub != null) {
                sub.add(0, new String[]{a, b});
                return sub;
            }
            if (budget[0] < 0) return null;
        }
        return null;
    }

    private static List<String[]> foldAllowRepeat(List<String> list) {
        List<String[]> out = new ArrayList<>();
        for (int i = 0, j = list.size() - 1; i < j; i++, j--) out.add(new String[]{list.get(i), list.get(j)});
        return out;
    }
}
