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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Schweizer System (nach hermann-baum.de):
 * <ul>
 *   <li>Feste Rundenzahl; je Runde spielt jede/r einmal. Niemand spielt zweimal gegen denselben Gegner.</li>
 *   <li>1. Runde: nach Setzung die obere gegen die untere Hälfte (1 gegen n/2+1 …), sonst zufällig.</li>
 *   <li>Folgerunden: nach aktueller Tabelle – Gleichplatzierte (gleiche Punkte) gegeneinander; geht das nicht
 *       (ungerade Punktgruppe oder schon gespielt), rückt jemand aus der nächstniedrigeren Punktgruppe auf.</li>
 *   <li>Ungerade Teilnehmerzahl: je Runde ein Freilos, bevorzugt an Tabellenletzte, höchstens eins je Person.
 *       Ein Freilos zählt als Sieg (1 Punkt, ohne Sätze und Bälle) und fließt nicht in die Buchholzzahl ein.</li>
 *   <li>Tabelle: Punkte (Siege + Freilos), Buchholz (Summe der Punkte der Gegner), Feinbuchholz (Summe der
 *       Buchholzzahlen der Gegner), Satzdifferenz, Balldifferenz, gewonnene Sätze.</li>
 * </ul>
 */
public final class SwissSystem {

    private SwissSystem() {
    }

    /** Eine Runde: Paarungen (A, B) und ggf. das Freilos. */
    public record Round(List<String[]> pairs, String bye) {
    }

    /** Vorschlag: Runden eines KO-Systems + 1, höchstens so viele, dass niemand doppelt spielen muss. */
    public static int defaultRounds(int n) {
        if (n < 2) return 1;
        int ko = 32 - Integer.numberOfLeadingZeros(n - 1);
        return Math.max(1, Math.min(ko + 1, maxRounds(n)));
    }

    /** Mehr Runden gehen nicht ohne Wiederholungen (= jeder gegen jeden). */
    public static int maxRounds(int n) {
        return Math.max(1, n % 2 == 0 ? n - 1 : n);
    }

    /** 1. Runde. */
    public static Round firstRound(List<Entrant> entrants, boolean seeded, Random random) {
        List<Entrant> list = new ArrayList<>(entrants);
        if (seeded) {
            list.sort(Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        } else {
            Collections.shuffle(list, random);
        }
        String bye = null;
        if (list.size() % 2 == 1) {
            // Freilos per Los aus der unteren Hälfte (nach Setzung) bzw. zufällig
            int from = seeded ? list.size() / 2 : 0;
            Entrant b = list.remove(from + random.nextInt(list.size() - from));
            bye = b.id();
        }
        List<String[]> pairs = new ArrayList<>();
        int half = list.size() / 2;
        for (int i = 0; i < half; i++) {
            if (seeded) pairs.add(new String[]{list.get(i).id(), list.get(i + half).id()});
            else pairs.add(new String[]{list.get(2 * i).id(), list.get(2 * i + 1).id()});
        }
        return new Round(pairs, bye);
    }

    /**
     * Folgerunde.
     *
     * @param ranked  Entrant-IDs nach aktueller Tabelle (Erster zuerst)
     * @param played  bereits gespielte Paarungen (Schlüssel aus {@link #key})
     * @param hadBye  wer schon ein Freilos hatte
     */
    public static Round nextRound(List<String> ranked, Set<String> played, Set<String> hadBye) {
        List<String> ids = new ArrayList<>(ranked);
        String bye = null;
        if (ids.size() % 2 == 1) {
            for (int i = ids.size() - 1; i >= 0; i--) {
                if (!hadBye.contains(ids.get(i))) {
                    bye = ids.remove(i);
                    break;
                }
            }
            if (bye == null) bye = ids.remove(ids.size() - 1);
        }
        int[] budget = {200_000};
        List<String[]> pairs = pair(ids, played, budget);
        if (pairs == null) {
            // keine Paarung ohne Wiederholung möglich – dann der Reihe nach
            pairs = new ArrayList<>();
            for (int i = 0; i + 1 < ids.size(); i += 2) pairs.add(new String[]{ids.get(i), ids.get(i + 1)});
        }
        return new Round(pairs, bye);
    }

    /** Oberster Ungepaarter gegen den nächsten möglichen darunter; bei Sackgasse zurück (Backtracking). */
    private static List<String[]> pair(List<String> remaining, Set<String> played, int[] budget) {
        if (remaining.isEmpty()) return new ArrayList<>();
        if (--budget[0] < 0) return null;
        String a = remaining.get(0);
        for (int j = 1; j < remaining.size(); j++) {
            String b = remaining.get(j);
            if (played.contains(key(a, b))) continue;
            List<String> rest = new ArrayList<>(remaining);
            rest.remove(j);
            rest.remove(0);
            List<String[]> sub = pair(rest, played, budget);
            if (sub != null) {
                sub.add(0, new String[]{a, b});
                return sub;
            }
            if (budget[0] < 0) return null;
        }
        return null;
    }

    public static String key(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    // --- Tabelle -------------------------------------------------------------

    private static final class Stat {
        final Entrant e;
        final int seed;
        int played, won, lost, byes, sw, sl, pw, pl;
        final List<String> opponents = new ArrayList<>();
        int buchholz, fine;

        Stat(Entrant e, int seed) {
            this.e = e;
            this.seed = seed;
        }

        int points() {
            return won + byes;
        }
    }

    /**
     * Tabelle nach Punkten, Buchholz, Feinbuchholz, Satz- und Balldifferenz.
     *
     * @param byes je Runde die ID mit Freilos (oder {@code null})
     */
    public static List<GroupStanding.Row> standings(List<Entrant> members, List<Match> matches, List<String> byes) {
        return standings(members, matches, byes, null);
    }

    /** @param lot fest gespeichertes Los je Teilnehmer-ID als letztes Kriterium ({@code null} = Reihenfolge) */
    public static List<GroupStanding.Row> standings(List<Entrant> members, List<Match> matches, List<String> byes,
                                                    java.util.function.ToIntFunction<String> lot) {
        Map<String, Stat> stats = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) {
            stats.put(members.get(i).id(), new Stat(members.get(i), lot == null ? i : lot.applyAsInt(members.get(i).id())));
        }
        for (String b : byes) if (b != null && stats.containsKey(b)) stats.get(b).byes++;
        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.DONE || m.winner() == null) continue;
            Stat a = stats.get(m.getA()), b = stats.get(m.getB());
            if (a == null || b == null) continue;
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
            a.opponents.add(b.e.id());
            b.opponents.add(a.e.id());
        }
        Map<String, Integer> points = new HashMap<>();
        stats.values().forEach(s -> points.put(s.e.id(), s.points()));
        for (Stat s : stats.values()) s.buchholz = s.opponents.stream().mapToInt(o -> points.getOrDefault(o, 0)).sum();
        Map<String, Integer> bh = new HashMap<>();
        stats.values().forEach(s -> bh.put(s.e.id(), s.buchholz));
        for (Stat s : stats.values()) s.fine = s.opponents.stream().mapToInt(o -> bh.getOrDefault(o, 0)).sum();

        List<Stat> list = new ArrayList<>(stats.values());
        list.sort(Comparator.comparingInt(Stat::points).reversed()
                .thenComparing(Comparator.comparingInt((Stat s) -> s.buchholz).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> s.fine).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> s.sw - s.sl).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> s.pw - s.pl).reversed())
                .thenComparing(Comparator.comparingInt((Stat s) -> s.sw).reversed())
                .thenComparingInt(s -> s.seed));
        List<GroupStanding.Row> rows = new ArrayList<>();
        for (int k = 0; k < list.size(); k++) {
            Stat s = list.get(k);
            rows.add(new GroupStanding.Row(k + 1, s.e, s.played, s.won, s.lost, s.sw, s.sl, s.pw, s.pl,
                    s.byes, s.buchholz, s.fine));
        }
        return rows;
    }

    /** Bereits gespielte (bzw. angesetzte) Paarungen. */
    public static Set<String> playedPairs(List<Match> matches) {
        Set<String> set = new HashSet<>();
        for (Match m : matches) if (m.getA() != null && m.getB() != null) set.add(key(m.getA(), m.getB()));
        return set;
    }
}
