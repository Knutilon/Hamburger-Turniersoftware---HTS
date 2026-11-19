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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.2: Platzierungsplan einer Phase – eine feste Folge von Spielen, deren Teilnehmende aus Setzplätzen (aus der
 * Auslosung bzw. Gruppenplatzierung) oder aus Sieger/Verlierer früherer Spiele kommen. Damit werden Haupt- und
 * Trostrunde (#94), das fortgesetzte K.-o.-System (#96), die Endrunde je Gruppenplatz (#97), die TOP24/TOP32-
 * Platzierungsspiele (#98/#100) und das doppelte K.-o.-System (#99) abgebildet.
 *
 * <p>Quellen: {@code "S3"} = Setzplatz 3, {@code "W:n5"} / {@code "L:n5"} = Sieger/Verlierer von Knoten n5,
 * {@code "-"} = Freilos. Plätze: Sieger erhält {@code winFrom–winTo}, Verlierer {@code loseFrom–loseTo} (0 = keiner).
 * Bedingung {@code "IFB:n7"}: der Knoten wird nur gespielt, wenn in n7 die B-Seite gewonnen hat (Stichkampf).
 */
public final class Plan {

    public enum Kind {
        MAIN_CONSOLATION("Haupt- und Trostrunde"),
        CONTINUED("Fortgesetztes K.-o.-System"),
        DOUBLE("Doppeltes K.-o.-System"),
        FINAL_BY_PLACE("Endrunde je Gruppenplatz"),
        TOP24("TOP24-Platzierungsspiele"),
        TOP32("TOP32-Platzierungsspiele");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Ein Spiel des Plans; {@code no} ist die Spielnummer, {@code depth} die Spielrunde (1 = zuerst spielbar). */
    public record Node(String key, int no, int section, String stage, int depth, String a, String b,
                       int winFrom, int winTo, int loseFrom, int loseTo, String cond) {
        public boolean hasWinPlace() { return winFrom > 0; }
        public boolean hasLosePlace() { return loseFrom > 0; }
    }

    /** Abschnitt der Anzeige (z. B. „Hauptrunde“, „Platz 5–8“); {@code resultGroup} 0 = Hauptwertung, 1 = Trostrunde. */
    public record Section(String title, int resultGroup) {
    }

    private final Kind kind;
    private final List<Section> sections;
    private final List<Node> nodes;
    private final List<String> seeds;          // Entrant-IDs (null = Freilos)
    private final List<String> seedOrigins;    // ID in der Vorphase (Ausschluss bei der Restwertung)
    private final List<Integer> seedGroups;    // Wertung je Setzplatz
    private final List<String> resultTitles;

    public Plan(Kind kind, List<Section> sections, List<Node> nodes, List<String> seeds, List<String> seedOrigins,
                List<Integer> seedGroups, List<String> resultTitles) {
        this.kind = kind;
        this.sections = List.copyOf(sections);
        this.nodes = List.copyOf(nodes);
        this.seeds = new ArrayList<>(seeds);
        this.seedOrigins = new ArrayList<>(seedOrigins);
        this.seedGroups = new ArrayList<>(seedGroups);
        this.resultTitles = List.copyOf(resultTitles);
    }

    public Kind getKind() { return kind; }
    public List<Section> getSections() { return sections; }
    public List<Node> getNodes() { return nodes; }
    public List<String> getSeeds() { return java.util.Collections.unmodifiableList(seeds); }
    public List<String> getSeedOrigins() { return java.util.Collections.unmodifiableList(seedOrigins); }
    public List<Integer> getSeedGroups() { return java.util.Collections.unmodifiableList(seedGroups); }
    public List<String> getResultTitles() { return resultTitles; }

    /** Setzplätze tauschen (Drag &amp; Drop vor dem Start). */
    void swapSeeds(int i, int j) {
        String s = seeds.get(i);
        seeds.set(i, seeds.get(j));
        seeds.set(j, s);
        String o = seedOrigins.get(i);
        seedOrigins.set(i, seedOrigins.get(j));
        seedOrigins.set(j, o);
    }

    public Node node(String key) {
        for (Node n : nodes) if (n.key().equals(key)) return n;
        return null;
    }

    public Node nodeByNumber(int no) {
        for (Node n : nodes) if (n.no() == no) return n;
        return null;
    }

    /** Index des Setzplatzes einer Quelle „S3“ oder -1. */
    public static int seedIndex(String src) {
        return src != null && src.startsWith("S") ? Integer.parseInt(src.substring(1)) : -1;
    }

    /** „W:n5“ → n5, sonst {@code null}. */
    public static String refKey(String src) {
        return src != null && (src.startsWith("W:") || src.startsWith("L:")) ? src.substring(2) : null;
    }

    /** Text für eine noch offene Seite, z. B. „Sieger/in Spiel 12“. */
    public String placeholder(String src) {
        if (src == null || "-".equals(src)) return "Freilos";
        String k = refKey(src);
        if (k != null) {
            Node n = node(k);
            return (src.startsWith("W:") ? "Sieger/in Spiel " : "Verlierer/in Spiel ") + (n == null ? "?" : n.no());
        }
        return "offen";
    }

    // =====================================================================================================
    // Erzeugen
    // =====================================================================================================

    /** Hilfsbau: Knoten sammeln, Tiefe und Nummern am Ende berechnen. */
    static final class Builder {
        final Kind kind;
        final List<Section> sections = new ArrayList<>();
        final List<Integer> sectionOrder = new ArrayList<>();          // Sortierschlüssel (z. B. erster Platz)
        final List<String[]> raw = new ArrayList<>();                   // key, section, stage, a, b, cond, explicitNo
        final List<int[]> places = new ArrayList<>();                   // winFrom, winTo, loseFrom, loseTo
        final List<String> seeds = new ArrayList<>();
        final List<String> origins = new ArrayList<>();
        final List<Integer> seedGroups = new ArrayList<>();
        final List<String> resultTitles = new ArrayList<>();
        int counter;

        Builder(Kind kind) {
            this.kind = kind;
        }

        int section(String title, int group, int order) {
            sections.add(new Section(title, group));
            sectionOrder.add(order);
            return sections.size() - 1;
        }

        String seed(String id, String origin, int group) {
            seeds.add(id);
            origins.add(origin);
            seedGroups.add(group);
            return id == null ? "-" : "S" + (seeds.size() - 1);
        }

        String node(int section, String stage, String a, String b) {
            return node(section, stage, a, b, null, 0);
        }

        String node(int section, String stage, String a, String b, String cond, int explicitNo) {
            String key = "n" + (++counter);
            raw.add(new String[]{key, String.valueOf(section), stage, a, b, cond, String.valueOf(explicitNo)});
            places.add(new int[4]);
            return key;
        }

        private int index(String key) {
            for (int i = 0; i < raw.size(); i++) if (raw.get(i)[0].equals(key)) return i;
            throw new IllegalArgumentException(key);
        }

        void win(String key, int from, int to) {
            int[] p = places.get(index(key));
            p[0] = from;
            p[1] = to;
        }

        void lose(String key, int from, int to) {
            int[] p = places.get(index(key));
            p[2] = from;
            p[3] = to;
        }

        Plan build() {
            // Tiefe: 1 + größte Tiefe der Quellen
            Map<String, Integer> depth = new HashMap<>();
            for (String[] r : raw) {
                int d = 1;
                for (String s : new String[]{r[3], r[4]}) {
                    String k = refKey(s);
                    if (k != null) d = Math.max(d, depth.getOrDefault(k, 1) + 1);
                }
                depth.put(r[0], d);
            }
            // Abschnitte sortieren (z. B. Platz 3–4 vor 5–8 vor 9–16)
            List<Integer> secIdx = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) secIdx.add(i);
            secIdx.sort(Comparator.comparingInt((Integer i) -> sectionOrder.get(i)).thenComparingInt(i -> i));
            int[] newSec = new int[sections.size()];
            List<Section> sortedSections = new ArrayList<>();
            for (int k = 0; k < secIdx.size(); k++) {
                newSec[secIdx.get(k)] = k;
                sortedSections.add(sections.get(secIdx.get(k)));
            }
            // Nummern: vorgegeben oder nach Spielrunde, Abschnitt, Reihenfolge
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) order.add(i);
            boolean explicit = raw.stream().allMatch(r -> Integer.parseInt(r[6]) > 0);
            if (explicit) {
                order.sort(Comparator.comparingInt(i -> Integer.parseInt(raw.get(i)[6])));
            } else {
                order.sort(Comparator.comparingInt((Integer i) -> depth.get(raw.get(i)[0]))
                        .thenComparingInt(i -> newSec[Integer.parseInt(raw.get(i)[1])])
                        .thenComparingInt(i -> i));
            }
            List<Node> nodes = new ArrayList<>();
            int no = 0;
            for (int i : order) {
                String[] r = raw.get(i);
                int[] p = places.get(i);
                int number = explicit ? Integer.parseInt(r[6]) : ++no;
                nodes.add(new Node(r[0], number, newSec[Integer.parseInt(r[1])], r[2], depth.get(r[0]), r[3], r[4],
                        p[0], p[1], p[2], p[3], r[5]));
            }
            if (resultTitles.isEmpty()) resultTitles.add("Endergebnis");
            return new Plan(kind, sortedSections, nodes, seeds, origins, seedGroups, resultTitles);
        }
    }

    /** Bis zu welcher Rundengröße im Hauptbaum weitergespielt wird (Verlierer früherer Runden teilen sich den Platz). */
    public static final int CUTOFF_ALL = Integer.MAX_VALUE;

    enum Extra { NONE, THIRD, FULL }

    /**
     * KO-Baum aus Quellen (Größe Zweierpotenz). FULL: alle Plätze werden ausgespielt (fortgesetzt), THIRD: nur Spiel um
     * Platz 3, NONE: Verlierer teilen sich ihren Platz. {@code cutoffSize}: nur auf oberster Ebene – Verlierer aus Runden
     * mit mehr Teilnehmenden spielen nicht weiter. {@code newSections}: Platzierungsbäume in eigenen Abschnitten.
     */
    static void ko(Builder b, List<String> src, int placeFrom, int section, String title, boolean main, Extra extra,
                   int cutoffSize, boolean newSections, int group) {
        int n = src.size();
        if (n < 2) {
            // eine einzelne Partei: kein Spiel, Platz über die Restwertung
            return;
        }
        List<String> cur = new ArrayList<>(src);
        List<List<String>> roundKeys = new ArrayList<>();
        List<List<String>> roundLosers = new ArrayList<>();
        List<Integer> roundSize = new ArrayList<>();
        int inRound = n;
        int roundNo = 1;
        while (cur.size() >= 2) {
            List<String> next = new ArrayList<>(), losers = new ArrayList<>(), keys = new ArrayList<>();
            for (int i = 0; i < cur.size(); i += 2) {
                String a = cur.get(i), bb = cur.get(i + 1);
                if ("-".equals(a) && "-".equals(bb)) {
                    next.add("-");
                    losers.add("-");
                    continue;
                }
                String key = b.node(section, stageName(title, main, inRound, placeFrom, roundNo), a, bb);
                keys.add(key);
                next.add("W:" + key);
                losers.add("L:" + key);
            }
            roundKeys.add(keys);
            roundLosers.add(losers);
            roundSize.add(inRound);
            cur = next;
            inRound /= 2;
            roundNo++;
        }
        int rounds = roundKeys.size();
        // Finale
        for (String k : roundKeys.get(rounds - 1)) {
            b.win(k, placeFrom, placeFrom);
            b.lose(k, placeFrom + 1, placeFrom + 1);
        }
        for (int r = rounds - 2; r >= 0; r--) {
            List<String> losers = roundLosers.get(r);
            int count = losers.size();
            int from = placeFrom + count, to = placeFrom + 2 * count - 1;
            boolean feed = switch (extra) {
                case FULL -> !main || roundSize.get(r) <= cutoffSize;
                case THIRD -> count == 2;
                case NONE -> false;
            };
            if (feed && losers.stream().filter(x -> !"-".equals(x)).count() >= 2) {
                if (extra == Extra.THIRD) {
                    String k = b.node(section, from == 3 ? Tournament.THIRD_PLACE_STAGE : "Spiel um Platz " + from, losers.get(0), losers.get(1));
                    b.win(k, from, from);
                    b.lose(k, from + 1, from + 1);
                } else {
                    String subTitle = "Platz " + from + "–" + to;
                    int sec = newSections ? b.section(subTitle, group, from) : section;
                    ko(b, losers, from, sec, subTitle, false, Extra.FULL, CUTOFF_ALL, newSections, group);
                }
            } else {
                for (String k : roundKeys.get(r)) b.lose(k, from, to);
            }
        }
    }

    /** Rundenname im Hauptbaum (Finale, Halbfinale …) bzw. in einem Platzierungsbaum („Spiel um Platz 5“). */
    static String stageName(String title, boolean main, int inRound, int placeFrom, int roundNo) {
        if (main) return KnockoutDraw.roundName(inRound);
        if (inRound == 2) return "Spiel um Platz " + placeFrom;
        if (inRound == 4) return title + " · Halbfinale";
        return title + " · Runde " + roundNo;
    }

    /** Zweierpotenz ≥ n (mindestens 2). */
    static int pow2(int n) {
        int s = 2;
        while (s < n) s *= 2;
        return s;
    }

    /** Haupt- und Trostrunde (#94): zwei KO-Bäume mit getrennter Wertung, je optional Spiel um Platz 3. */
    static Plan mainConsolation(List<String> mainSlots, List<String> mainOrigins, List<String> consSlots, List<String> consOrigins,
                                boolean thirdMain, boolean thirdCons) {
        Builder b = new Builder(Kind.MAIN_CONSOLATION);
        b.resultTitles.add("Hauptrunde");
        b.resultTitles.add("Trostrunde");
        int s0 = b.section("Hauptrunde", 0, 0);
        int s1 = b.section("Trostrunde", 1, 1);
        List<String> src0 = new ArrayList<>();
        for (int i = 0; i < mainSlots.size(); i++) src0.add(b.seed(mainSlots.get(i), mainOrigins.get(i), 0));
        List<String> src1 = new ArrayList<>();
        for (int i = 0; i < consSlots.size(); i++) src1.add(b.seed(consSlots.get(i), consOrigins.get(i), 1));
        ko(b, src0, 1, s0, "Hauptrunde", true, thirdMain ? Extra.THIRD : Extra.NONE, CUTOFF_ALL, false, 0);
        ko(b, src1, 1, s1, "Trostrunde", true, thirdCons ? Extra.THIRD : Extra.NONE, CUTOFF_ALL, false, 1);
        return b.build();
    }

    /** Fortgesetztes K.-o.-System (#96, WO 7.3). */
    static Plan continued(List<String> slots, List<String> origins, int cutoffSize) {
        Builder b = new Builder(Kind.CONTINUED);
        int s0 = b.section("Hauptrunde", 0, 0);
        List<String> src = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) src.add(b.seed(slots.get(i), origins.get(i), 0));
        ko(b, src, 1, s0, "Hauptrunde", true, Extra.FULL, cutoffSize, true, 0);
        return b.build();
    }

    /**
     * Doppeltes K.-o.-System (#99, WO 7.4): Siegerrunde, Trostrunde mit „Kreuzen“ der Verlierer, Finale und – falls der
     * Sieger der Trostrunde das Finale gewinnt – Stichkampf.
     */
    static Plan doubleKo(List<String> slots, List<String> origins) {
        Builder b = new Builder(Kind.DOUBLE);
        int sw = b.section("Siegerrunde", 0, 0);
        int sl = b.section("Trostrunde", 0, 1);
        int sf = b.section("Finale", 0, 2);
        List<String> cur = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) cur.add(b.seed(slots.get(i), origins.get(i), 0));
        List<List<String>> wbLosers = new ArrayList<>();
        int inRound = cur.size();
        while (cur.size() >= 2) {
            List<String> next = new ArrayList<>(), losers = new ArrayList<>();
            for (int i = 0; i < cur.size(); i += 2) {
                String a = cur.get(i), bb = cur.get(i + 1);
                if ("-".equals(a) && "-".equals(bb)) {
                    next.add("-");
                    losers.add("-");
                    continue;
                }
                String k = b.node(sw, "Siegerrunde · " + KnockoutDraw.roundName(inRound), a, bb);
                next.add("W:" + k);
                losers.add("L:" + k);
            }
            wbLosers.add(losers);
            cur = next;
            inRound /= 2;
        }
        String wbChampion = cur.get(0);
        int R = wbLosers.size();
        List<List<String>> lbRounds = new ArrayList<>();
        String lbChampion;
        if (R == 1) {
            lbChampion = wbLosers.get(0).get(0);
        } else {
            int lbRound = 1;
            List<String> w = pairUp(b, sl, wbLosers.get(0), "Trostrunde · Runde " + lbRound++, lbRounds);
            for (int r = 1; r < R; r++) {
                List<String> drop = new ArrayList<>(wbLosers.get(r));
                if (r % 2 == 1) java.util.Collections.reverse(drop);       // „Kreuzen“: Begegnungen nicht wiederholen
                List<String> mixed = new ArrayList<>();
                for (int i = 0; i < w.size(); i++) {
                    mixed.add(w.get(i));
                    mixed.add(i < drop.size() ? drop.get(i) : "-");
                }
                w = pairUp(b, sl, mixed, "Trostrunde · Runde " + lbRound++, lbRounds);
                if (w.size() > 1 && r < R - 1) w = pairUp(b, sl, w, "Trostrunde · Runde " + lbRound++, lbRounds);
            }
            lbChampion = w.get(0);
        }
        // Plätze in der Trostrunde: vom Ende her 3., 4., dann je Runde ein Bereich
        int place = 3;
        for (int r = lbRounds.size() - 1; r >= 0; r--) {
            List<String> keys = lbRounds.get(r);
            if (keys.isEmpty()) continue;
            int count = keys.size();
            for (String k : keys) b.lose(k, place, place + count - 1);
            place += count;
        }
        String gf = b.node(sf, "Finale", wbChampion, lbChampion);
        b.win(gf, 1, 1);
        b.lose(gf, 2, 2);
        String reset = b.node(sf, "Stichkampf", wbChampion, lbChampion, "IFB:" + gf, 0);
        b.win(reset, 1, 1);
        b.lose(reset, 2, 2);
        return b.build();
    }

    private static List<String> pairUp(Builder b, int section, List<String> list, String stage, List<List<String>> rounds) {
        List<String> next = new ArrayList<>(), keys = new ArrayList<>();
        for (int i = 0; i + 1 < list.size(); i += 2) {
            String a = list.get(i), bb = list.get(i + 1);
            if ("-".equals(a) && "-".equals(bb)) {
                next.add("-");
                continue;
            }
            if ("-".equals(a) || "-".equals(bb)) {
                next.add("-".equals(a) ? bb : a);           // Freilos: ohne Spiel weiter
                continue;
            }
            String k = b.node(section, stage, a, bb);
            keys.add(k);
            next.add("W:" + k);
        }
        if (list.size() % 2 == 1) next.add(list.get(list.size() - 1));
        rounds.add(keys);
        return next;
    }

    /**
     * Endrunde je Gruppenplatz (#97): je Stufe bilden die Gleichplatzierten der Gruppen einen Block; Halbfinale 1.–4. und
     * 2.–3. Gruppe der Stufe, danach Spiele um alle Plätze des Blocks.
     *
     * @param blocks je Block die Teilnehmenden in Gruppenreihenfolge (null = fehlt) samt Herkunft; {@code firstPlace}
     *               der beste Platz des Blocks
     */
    record Block(List<String> entrants, List<String> origins, int firstPlace) {
    }

    static Plan finalByPlace(List<Block> blocks) {
        Builder b = new Builder(Kind.FINAL_BY_PLACE);
        for (Block bl : blocks) {
            int m = bl.entrants().size();
            int size = pow2(m);
            int last = bl.firstPlace() + m - 1;
            String title = m > 1 ? "Plätze " + bl.firstPlace() + "–" + last : "Platz " + bl.firstPlace();
            int sec = b.section(title, 0, bl.firstPlace());
            int[] order = KnockoutDraw.seedOrder(size);
            List<String> src = new ArrayList<>();
            List<String> seedSrc = new ArrayList<>();
            for (int i = 0; i < m; i++) seedSrc.add(b.seed(bl.entrants().get(i), bl.origins().get(i), 0));
            for (int j = 0; j < size; j++) {
                int s = order[j] - 1;
                src.add(s < m ? seedSrc.get(s) : "-");
            }
            ko(b, src, bl.firstPlace(), sec, title, false, Extra.FULL, CUTOFF_ALL, false, 0);
        }
        return b.build();
    }

    /** TOP24/TOP32 (#98/#100): fester Plan nach DTTB-Ranglistenturnieren; Quellen „B1“ = Gruppe B Platz 1. */
    static final String[] TOP24 = {
            "1|Plätze 1–4|Runde 1|B1|A2", "2|Plätze 1–4|Runde 1|A1|B2", "3|Plätze 1–4|Runde 1|D1|C2", "4|Plätze 1–4|Runde 1|C1|D2",
            "5|Runde 1|Runde 1|D3|C4", "6|Runde 1|Runde 1|C3|D4", "7|Runde 1|Runde 1|B3|A4", "8|Runde 1|Runde 1|A3|B4",
            "9|Runde 1|Runde 1|D5|C6", "10|Runde 1|Runde 1|A5|B6", "11|Runde 1|Runde 1|B5|A6", "12|Runde 1|Runde 1|C5|D6",
            "13|Runde 2|Runde 2|L4|W7", "14|Runde 2|Runde 2|L1|W5", "15|Runde 2|Runde 2|L2|W6", "16|Runde 2|Runde 2|L3|W8",
            "17|Runde 2|Runde 2|W10|L5", "18|Runde 2|Runde 2|W12|L7", "19|Runde 2|Runde 2|W11|L6", "20|Runde 2|Runde 2|W9|L8",
            "21|Plätze 1–4|Halbfinale|W1|W3", "22|Plätze 1–4|Halbfinale|W2|W4",
            "23|Platz 5–8|Platz 5–8 · Halbfinale|W13|W16", "24|Platz 5–8|Platz 5–8 · Halbfinale|W14|W15",
            "25|Platz 9–12|Platz 9–12 · Halbfinale|L13|L15", "26|Platz 9–12|Platz 9–12 · Halbfinale|L14|L16",
            "27|Platz 13–16|Platz 13–16 · Halbfinale|W17|W19", "28|Platz 13–16|Platz 13–16 · Halbfinale|W18|W20",
            "29|Platz 17–20|Platz 17–20 · Halbfinale|L17|L19", "30|Platz 17–20|Platz 17–20 · Halbfinale|L18|L20",
            "31|Platz 21–24|Platz 21–24 · Halbfinale|L9|L10", "32|Platz 21–24|Platz 21–24 · Halbfinale|L11|L12",
            "33|Plätze 1–4|Finale|W21|W22|1", "34|Plätze 1–4|Spiel um Platz 3|L21|L22|3",
            "35|Platz 5–8|Spiel um Platz 5|W23|W24|5", "36|Platz 5–8|Spiel um Platz 7|L23|L24|7",
            "37|Platz 9–12|Spiel um Platz 9|W25|W26|9", "38|Platz 9–12|Spiel um Platz 11|L25|L26|11",
            "39|Platz 13–16|Spiel um Platz 13|W27|W28|13", "40|Platz 13–16|Spiel um Platz 15|L27|L28|15",
            "41|Platz 17–20|Spiel um Platz 17|W29|W30|17", "42|Platz 17–20|Spiel um Platz 19|L29|L30|19",
            "43|Platz 21–24|Spiel um Platz 21|W31|W32|21", "44|Platz 21–24|Spiel um Platz 23|L31|L32|23"};

    static final String[] TOP32 = {
            "1|Plätze 1–4|Runde 1|B1|A2", "2|Plätze 1–4|Runde 1|A1|B2", "3|Plätze 1–4|Runde 1|D1|C2", "4|Plätze 1–4|Runde 1|C1|D2",
            "5|Runde 1|Runde 1|D3|C4", "6|Runde 1|Runde 1|C3|D4", "7|Runde 1|Runde 1|B3|A4", "8|Runde 1|Runde 1|A3|B4",
            "9|Runde 1|Runde 1|D5|C6", "10|Runde 1|Runde 1|A5|B6", "11|Runde 1|Runde 1|B5|A6", "12|Runde 1|Runde 1|C5|D6",
            "13|Runde 1|Runde 1|D7|C8", "14|Runde 1|Runde 1|A7|B8", "15|Runde 1|Runde 1|B7|A8", "16|Runde 1|Runde 1|C7|D8",
            "17|Runde 2|Runde 2|L4|W7", "18|Runde 2|Runde 2|L1|W5", "19|Runde 2|Runde 2|L2|W6", "20|Runde 2|Runde 2|L3|W8",
            "21|Runde 2|Runde 2|W10|L5", "22|Runde 2|Runde 2|W12|L7", "23|Runde 2|Runde 2|W11|L6", "24|Runde 2|Runde 2|W9|L8",
            "25|Runde 2|Runde 2|W13|L10", "26|Runde 2|Runde 2|W14|L11", "27|Runde 2|Runde 2|W15|L9", "28|Runde 2|Runde 2|W16|L12",
            "29|Plätze 1–4|Halbfinale|W1|W3", "30|Plätze 1–4|Halbfinale|W2|W4",
            "31|Platz 5–8|Platz 5–8 · Halbfinale|W17|W20", "32|Platz 5–8|Platz 5–8 · Halbfinale|W18|W19",
            "33|Platz 9–12|Platz 9–12 · Halbfinale|L17|L20", "34|Platz 9–12|Platz 9–12 · Halbfinale|L18|L19",
            "35|Platz 13–16|Platz 13–16 · Halbfinale|W21|W23", "36|Platz 13–16|Platz 13–16 · Halbfinale|W22|W24",
            "37|Platz 17–20|Platz 17–20 · Halbfinale|L21|L23", "38|Platz 17–20|Platz 17–20 · Halbfinale|L22|L24",
            "39|Platz 21–24|Platz 21–24 · Halbfinale|W25|W27", "40|Platz 21–24|Platz 21–24 · Halbfinale|W26|W28",
            "41|Platz 25–28|Platz 25–28 · Halbfinale|L25|L27", "42|Platz 25–28|Platz 25–28 · Halbfinale|L26|L28",
            "43|Platz 29–32|Platz 29–32 · Halbfinale|L13|L15", "44|Platz 29–32|Platz 29–32 · Halbfinale|L16|L14",
            "45|Plätze 1–4|Finale|W29|W30|1", "46|Plätze 1–4|Spiel um Platz 3|L29|L30|3",
            "47|Platz 5–8|Spiel um Platz 5|W31|W32|5", "48|Platz 5–8|Spiel um Platz 7|L31|L32|7",
            "49|Platz 9–12|Spiel um Platz 9|W33|W34|9", "50|Platz 9–12|Spiel um Platz 11|L33|L34|11",
            "51|Platz 13–16|Spiel um Platz 13|W35|W36|13", "52|Platz 13–16|Spiel um Platz 15|L35|L36|15",
            "53|Platz 17–20|Spiel um Platz 17|W37|W38|17", "54|Platz 17–20|Spiel um Platz 19|L37|L38|19",
            "55|Platz 21–24|Spiel um Platz 21|W39|W40|21", "56|Platz 21–24|Spiel um Platz 23|L39|L40|23",
            "57|Platz 25–28|Spiel um Platz 25|W41|W42|25", "58|Platz 25–28|Spiel um Platz 27|L41|L42|27",
            "59|Platz 29–32|Spiel um Platz 29|W43|W44|29", "60|Platz 29–32|Spiel um Platz 31|L43|L44|31"};

    /** Gruppenplätze („B1“ …), die ein Vorlagenplan braucht. */
    static List<String> templateTokens(String[] template) {
        List<String> out = new ArrayList<>();
        for (String row : template) {
            String[] f = row.split("\\|");
            for (String s : new String[]{f[3], f[4]}) {
                if (Character.isLetter(s.charAt(0)) && s.charAt(0) != 'W' && s.charAt(0) != 'L' && !out.contains(s)) out.add(s);
            }
        }
        return out;
    }

    /**
     * Plan aus einer Vorlage.
     *
     * @param entrant Gruppenplatz („B1“) → {Entrant-ID, Herkunfts-ID} oder {@code null} (fehlt → Freilos)
     */
    static Plan fromTemplate(Kind kind, String[] template, java.util.function.Function<String, String[]> entrant) {
        Builder b = new Builder(kind);
        Map<String, Integer> sec = new HashMap<>();
        Map<String, String> seedOf = new HashMap<>();
        Map<String, String> keyOf = new HashMap<>();
        for (String token : templateTokens(template)) {
            String[] e = entrant.apply(token);
            seedOf.put(token, b.seed(e == null ? null : e[0], e == null ? null : e[1], 0));
        }
        for (String row : template) {
            String[] f = row.split("\\|");
            String title = f[1];
            int order = title.startsWith("Plätze 1") ? 0 : title.equals("Runde 1") ? 1 : title.equals("Runde 2") ? 2
                    : 10 + Integer.parseInt(title.replaceAll("\\D*(\\d+).*", "$1"));
            int s = sec.computeIfAbsent(title, t -> b.section(t, 0, order));
            String a = src(f[3], seedOf, keyOf), bb = src(f[4], seedOf, keyOf);
            String k = b.node(s, f[2], a, bb, null, Integer.parseInt(f[0]));
            keyOf.put(f[0], k);
            if (f.length > 5) {
                int p = Integer.parseInt(f[5]);
                b.win(k, p, p);
                b.lose(k, p + 1, p + 1);
            }
        }
        return b.build();
    }

    private static String src(String token, Map<String, String> seedOf, Map<String, String> keyOf) {
        if (token.startsWith("W") || token.startsWith("L")) {
            String k = keyOf.get(token.substring(1));
            if (k == null) throw new IllegalArgumentException("Vorlage: Spiel " + token.substring(1) + " fehlt vor seiner Verwendung");
            return token.charAt(0) + ":" + k;
        }
        return seedOf.get(token);
    }

    /** Text eines Setzplatzes für Anzeige und Speichern. */
    @Override
    public String toString() {
        return kind + " " + nodes.size() + " Spiele";
    }
}
