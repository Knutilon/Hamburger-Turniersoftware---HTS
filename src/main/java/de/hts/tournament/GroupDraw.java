package de.hts.tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Auslosung einer Gruppenphase (jeder gegen jeden).
 *
 * <p>Die Anzahl der Gruppen ergibt sich entweder aus der gewünschten Gruppengröße
 * (Teilnehmende ÷ Gruppengröße, aufgerundet – keine Gruppe wird größer als gewünscht) oder wird direkt vorgegeben.
 * Ungleich große Gruppen sind erlaubt; kleinere Gruppen haben dann weniger Spiele.
 *
 * <p>Verteilung im <b>Schlangensystem</b>: Nr. 1 → Gruppe 1, Nr. 2 → Gruppe 2, … bis zur letzten Gruppe
 * (Gruppenköpfe), dann rückwärts von der letzten zur ersten Gruppe, dann wieder vorwärts usw.
 * <b>Nach Setzung</b> ist die Reihenfolge der TTR (absteigend, bei Doppeln Ø-TTR; Gleichstand und fehlender TTR
 * werden ausgelost), <b>ohne Setzung</b> ist sie komplett zufällig.
 */
public final class GroupDraw {

    private GroupDraw() {
    }

    /** Anzahl der Gruppen bei vorgegebener Gruppengröße: aufgerundet, damit keine Gruppe größer wird. */
    public static int groupCountForSize(int entrants, int groupSize) {
        if (groupSize < 2) throw new IllegalArgumentException("Eine Gruppe braucht mindestens 2 Mitglieder.");
        return Math.max(1, (entrants + groupSize - 1) / groupSize);
    }

    /** Größte Gruppengröße bei vorgegebener Anzahl Gruppen (aufgerundet). */
    public static int maxGroupSize(int entrants, int groups) {
        return (entrants + groups - 1) / groups;
    }

    /** Prüft, ob sich so auslosen lässt; liefert eine verständliche Meldung oder {@code null}. */
    public static String validate(int entrants, int groups) {
        if (entrants < 2) return "Für eine Gruppenphase braucht es mindestens 2 Teilnehmende bzw. Doppel.";
        if (groups < 1) return "Es muss mindestens eine Gruppe geben.";
        if (entrants / groups < 2) {
            return "Bei " + groups + " Gruppen hätte eine Gruppe weniger als 2 Mitglieder – bitte weniger Gruppen wählen.";
        }
        return null;
    }

    /** Art der Setzung. */
    public enum Seeding {
        /** komplett zufällig */
        RANDOM("zufällig"),
        /** Schlangensystem nach TTR */
        SNAKE("TTR Setzung (Schlange)"),
        /** Stärkegruppen (Töpfe) nach TTR; aus jeder Stärkegruppe mindestens eine/r je Gruppe */
        POTS("TTR Setzung (Stärkegruppen)"),
        /** Gruppenköpfe fest (TTR-Nr. 1 → Gruppe A, Nr. 2 → B, …), die übrigen Plätze wie bei den Stärkegruppen */
        HEADS("Gruppenkopf gesetzt");

        private final String label;

        Seeding(String label) {
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

    /** Lost aus: Liste der Gruppen (Gruppe 1 zuerst), je Gruppe die Mitglieder in Setzreihenfolge. */
    public static List<List<Entrant>> draw(List<Entrant> entrants, int groups, boolean seeded, Random random) {
        return draw(entrants, groups, seeded ? Seeding.SNAKE : Seeding.RANDOM, false, random);
    }

    /**
     * Lost aus.
     * <ul>
     *   <li>{@link Seeding#SNAKE}: Schlangensystem nach TTR. Mit {@code avoidClubs} wird die Schlange unterbrochen:
     *       auf den nächsten Platz kommt der Stärkste ohne Vereinsgleichheit in dieser Gruppe; gibt es niemanden
     *       mehr ohne Vereinsgleichheit, geht es normal nach der Schlange weiter.</li>
     *   <li>{@link Seeding#POTS}: Stärkegruppen à „Anzahl Gruppen“ nach TTR (1–4, 5–8 … bei 4 Gruppen); die letzte
     *       Stärkegruppe nimmt den Rest auf (z. B. 9–15). Jede Gruppe bekommt je Stärkegruppe eine/n per Los, aus der
     *       letzten so viele, bis die Gruppe voll ist. Mit {@code avoidClubs} wird bevorzugt jemand ohne
     *       Vereinsgleichheit aus derselben Stärkegruppe gezogen.</li>
     *   <li>{@link Seeding#HEADS}: wie Stärkegruppen, aber die erste Stärkegruppe wird nicht gelost: TTR-Nr. 1 kommt
     *       in Gruppe A, Nr. 2 in Gruppe B usw. Gruppenköpfe werden auch beim Nachbessern nie getauscht.</li>
     *   <li>{@link Seeding#RANDOM}: zufällige Reihenfolge, verteilt wie die Schlange (mit {@code avoidClubs} ebenso
     *       unter Beachtung der Vereine).</li>
     * </ul>
     */
    public static List<List<Entrant>> draw(List<Entrant> entrants, int groups, Seeding seeding, boolean avoidClubs, Random random) {
        String problem = validate(entrants.size(), groups);
        if (problem != null) throw new IllegalArgumentException(problem);
        List<Entrant> ranking = new ArrayList<>(entrants);
        Collections.shuffle(ranking, random);
        if (seeding != Seeding.RANDOM) {
            ranking.sort(Comparator.comparing(Entrant::ttr, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        int n = ranking.size();
        // Zielgruppe je Platz nach der Schlange – bestimmt auch die Gruppengrößen
        int[] targets = new int[n];
        int[] sizes = new int[groups];
        for (int i = 0; i < n; i++) {
            int pass = i / groups, pos = i % groups;
            targets[i] = pass % 2 == 0 ? pos : groups - 1 - pos;
            sizes[targets[i]]++;
        }
        List<List<Entrant>> result = new ArrayList<>();
        for (int g = 0; g < groups; g++) result.add(new ArrayList<>());

        if (seeding == Seeding.POTS || seeding == Seeding.HEADS) {
            int pots = n / groups;                         // ≥ 2 (validate)
            List<Integer> order = new ArrayList<>();
            for (int g = 0; g < groups; g++) order.add(g);
            for (int k = 0; k < pots; k++) {
                int from = k * groups, to = k == pots - 1 ? n : from + groups;
                List<Entrant> pot = new ArrayList<>(ranking.subList(from, to));
                Collections.shuffle(pot, random);
                if (k == 0 && seeding == Seeding.HEADS) {
                    // Gruppenköpfe fest: Nr. 1 in Gruppe A, Nr. 2 in Gruppe B, …
                    for (int g = 0; g < groups; g++) result.get(g).add(ranking.get(g));
                } else if (k < pots - 1) {
                    Collections.shuffle(order, random);
                    assignPot(pot, order, result, avoidClubs);
                } else {
                    // letzte Stärkegruppe: Gruppen der Reihe nach auffüllen, bis alle voll sind
                    while (!pot.isEmpty()) {
                        Collections.shuffle(order, random);
                        boolean placed = false;
                        for (int g : order) {
                            if (pot.isEmpty()) break;
                            if (result.get(g).size() < sizes[g]) {
                                result.get(g).add(pick(pot, result.get(g), avoidClubs));
                                placed = true;
                            }
                        }
                        if (!placed) break;
                    }
                }
            }
        } else {
            List<Entrant> remaining = new ArrayList<>(ranking);
            for (int i = 0; i < n; i++) {
                List<Entrant> group = result.get(targets[i]);
                Entrant chosen = remaining.get(0);
                if (avoidClubs) {
                    for (Entrant e : remaining) {
                        if (!clashes(group, e)) {
                            chosen = e;
                            break;
                        }
                    }
                }
                remaining.remove(chosen);
                group.add(chosen);
            }
        }
        if (avoidClubs) repairClubClashes(result, ranking, groups, seeding == Seeding.POTS || seeding == Seeding.HEADS, n, random);
        if (avoidClubs && entrants.stream().noneMatch(e -> e.id().startsWith("pair:"))) {
            clubMatching(result, ranking, groups, seeding == Seeding.POTS || seeding == Seeding.HEADS);   // 0.38 (#48), nicht für Doppel
            // falls dadurch doch noch ein Tausch Vereinsgleichheiten spart: Vermeiden hat Vorrang
            repairClubClashes(result, ranking, groups, seeding == Seeding.POTS || seeding == Seeding.HEADS, n, random);
        }
        // Mitglieder je Gruppe in Setzreihenfolge (Gruppenkopf zuerst)
        for (List<Entrant> g : result) g.sort(Comparator.comparingInt(ranking::indexOf));
        return result;
    }

    /**
     * Nachbesserung: Die schrittweise Verteilung kann am Ende in eine Lage geraten, in der für eine Gruppe nur noch
     * Vereinskameraden übrig sind, obwohl ein Tausch mit einer anderen Gruppe das verhindern würde. Solange es geht,
     * wird deshalb ein Mitglied mit Vereinsgleichheit gegen ein möglichst gleich starkes Mitglied einer anderen Gruppe
     * getauscht, wenn das die Zahl der Vereinsgleichheiten insgesamt senkt. Gruppenköpfe bleiben, bei Stärkegruppen
     * wird nur innerhalb derselben Stärkegruppe getauscht.
     */
    private static void repairClubClashes(List<List<Entrant>> result, List<Entrant> ranking, int groups, boolean pots, int n,
                                          Random random) {
        java.util.Map<Entrant, Integer> rank = new java.util.HashMap<>();
        for (int i = 0; i < ranking.size(); i++) rank.put(ranking.get(i), i);
        int lastPot = Math.max(0, n / groups - 1);
        java.util.function.ToIntFunction<Entrant> potOf = e -> Math.min(rank.get(e) / groups, lastPot);
        int current = totalClashes(result);
        int best = current;
        int floor = minimalClashes(ranking, groups);           // 0.38: weniger geht nicht – dann sofort fertig
        List<List<Entrant>> bestState = copy(result);
        Entrant lastA = null, lastB = null;
        for (int guard = 0; guard < 40 * n && best > floor; guard++) {
            int bestDist = Integer.MAX_VALUE, bestGain = 0;
            int bg = -1, bi = -1, bh = -1, bj = -1;
            List<int[]> plateau = new ArrayList<>();
            for (int g = 0; g < result.size(); g++) {
                List<Entrant> gl = result.get(g);
                for (int i = 0; i < gl.size(); i++) {
                    Entrant a = gl.get(i);
                    int ca = clashCount(gl, a, null);
                    if (ca == 0 || rank.get(a) < groups) continue;                // Gruppenköpfe bleiben
                    for (int h = 0; h < result.size(); h++) {
                        if (h == g) continue;
                        List<Entrant> hl = result.get(h);
                        for (int j = 0; j < hl.size(); j++) {
                            Entrant b = hl.get(j);
                            if (rank.get(b) < groups) continue;
                            if (pots && potOf.applyAsInt(a) != potOf.applyAsInt(b)) continue;
                            int before = ca + clashCount(hl, b, null);
                            int after = clashCount(gl, b, a) + clashCount(hl, a, b);
                            int gain = before - after;
                            int dist = Math.abs(rank.get(a) - rank.get(b));
                            if (gain > 0 && (dist < bestDist || (dist == bestDist && gain > bestGain))) {   // möglichst gleich stark
                                bestGain = gain;
                                bestDist = dist;
                                bg = g; bi = i; bh = h; bj = j;
                            } else if (gain == 0 && dist <= 2 * groups && !(a == lastB && b == lastA)) {
                                plateau.add(new int[]{g, i, h, j});
                            }
                        }
                    }
                }
            }
            int[] mv;
            if (bg >= 0) {
                mv = new int[]{bg, bi, bh, bj};
                current -= bestGain;
            } else if (!plateau.isEmpty()) {
                mv = plateau.get(random.nextInt(plateau.size()));      // gleichwertiger Tausch, um weiterzukommen
            } else {
                break;
            }
            Entrant a = result.get(mv[0]).get(mv[1]), b = result.get(mv[2]).get(mv[3]);
            result.get(mv[0]).set(mv[1], b);
            result.get(mv[2]).set(mv[3], a);
            lastA = a;
            lastB = b;
            if (current < best) {
                best = current;
                bestState = copy(result);
            }
        }
        for (int g = 0; g < result.size(); g++) {
            result.get(g).clear();
            result.get(g).addAll(bestState.get(g));
        }
    }

    /**
     * 0.38 (#48): Unvermeidbare Vereinsgleichheit – hat ein Verein mehr Mitglieder als es Gruppen gibt, kommen die
     * Überzähligen gezielt zu den Stärksten ihres Vereins: Die Vereinsmitglieder werden nach Setzreihenfolge in Lagen
     * zu je „Gruppenzahl“ geteilt; Lage 1 verteilt sich auf verschiedene Gruppen, in Lage 2 kommt der/die Schwächste
     * zur Nr. 1, der/die Zweitschwächste zur Nr. 2 usw. (Richtung rückwärts), Lage 3 wieder vorwärts (Nr. 9 zu Nr. 1 und
     * 8 bei 4 Gruppen), Lage 4 rückwärts … Dafür wird jeweils mit einem möglichst gleich starken Mitglied der Zielgruppe
     * getauscht (bevorzugt höchstens Gruppenzahl − 1 Plätze entfernt, bei Stärkegruppen aus derselben Stärkegruppe);
     * Gruppenköpfe bleiben. Vereine mit dem stärkeren Stärksten haben Vorrang; geht ein Tausch nicht, wird es mit der
     * Gruppe des/der nächsten Vereinskameraden versucht („eine Gruppe nach hinten“).
     */
    static void clubMatching(List<List<Entrant>> result, List<Entrant> ranking, int groups, boolean pots) {
        if (groups < 2) return;
        java.util.Map<Entrant, Integer> rank = new java.util.HashMap<>();
        for (int i = 0; i < ranking.size(); i++) rank.put(ranking.get(i), i);
        int lastPot = Math.max(0, ranking.size() / groups - 1);
        java.util.function.ToIntFunction<Entrant> potOf = e -> Math.min(rank.get(e) / groups, lastPot);
        java.util.Map<String, List<Entrant>> byClub = new java.util.LinkedHashMap<>();
        List<Entrant> byTtr = new ArrayList<>(ranking);                  // Vereins-Nr. immer nach TTR (auch bei „zufällig“)
        byTtr.sort(Comparator.comparing(Entrant::ttr, Comparator.nullsLast(Comparator.reverseOrder())));
        for (Entrant e : byTtr) {
            if (e.clubs().size() != 1) continue;
            byClub.computeIfAbsent(e.clubs().iterator().next(), k -> new ArrayList<>()).add(e);
        }
        List<List<Entrant>> big = new ArrayList<>();
        for (List<Entrant> members : byClub.values()) if (members.size() > groups) big.add(members);
        if (big.isEmpty()) return;
        big.sort(Comparator.comparingInt(m -> byTtr.indexOf(m.get(0))));          // stärkerer Stärkster zuerst
        java.util.Set<Entrant> fixed = new java.util.HashSet<>();
        for (List<Entrant> members : big) fixed.addAll(members.subList(0, groups));
        for (List<Entrant> members : big) {
            // Lage 1 auf verschiedene Gruppen: steht ein/e Stärkere/r schon in derselben Gruppe, in eine freie tauschen
            java.util.Set<Integer> used = new java.util.HashSet<>();
            for (int j = 0; j < groups; j++) {
                Entrant e = members.get(j);
                int g = groupIndex(result, e);
                if (used.contains(g)) {
                    for (int f = 0; f < result.size(); f++) {
                        if (!used.contains(f) && moveTo(result, e, f, fixed, rank, potOf, groups, pots, true)) break;
                    }
                    g = groupIndex(result, e);
                }
                used.add(g);
            }
            List<Integer> base = new ArrayList<>();
            for (int j = 0; j < groups; j++) base.add(groupIndex(result, members.get(j)));
            List<List<Entrant>> snapshot = copy(result);
            int clashesBefore = totalClashes(result);
            java.util.Set<Entrant> fixedBefore = new java.util.HashSet<>(fixed);
            for (int idx = groups; idx < members.size(); idx++) {
                int layer = idx / groups, pos = idx % groups;
                int k = Math.min(groups, members.size() - layer * groups);
                int j = layer % 2 == 1 ? k - 1 - pos : pos;                       // Lage 2 rückwärts, 3 vorwärts …
                Entrant sMember = members.get(idx);
                for (int t = 0; t < groups; t++) {
                    if (moveTo(result, sMember, base.get((j + t) % groups), fixed, rank, potOf, groups, pots, false)) break;
                }
                fixed.add(sMember);
            }
            if (totalClashes(result) > clashesBefore) {            // nie mehr Vereinsgleichheiten als vorher
                for (int g = 0; g < result.size(); g++) {
                    result.get(g).clear();
                    result.get(g).addAll(snapshot.get(g));
                }
                fixed.clear();
                fixed.addAll(fixedBefore);
                fixed.addAll(members);
            }
        }
    }

    private static int groupIndex(List<List<Entrant>> result, Entrant e) {
        for (int g = 0; g < result.size(); g++) if (result.get(g).contains(e)) return g;
        return -1;
    }

    /** Setzt {@code s} per Tausch in Gruppe {@code target}; {@code false}, wenn kein passender Tausch möglich ist. */
    private static boolean moveTo(List<List<Entrant>> result, Entrant s, int target, java.util.Set<Entrant> fixed,
                                  java.util.Map<Entrant, Integer> rank, java.util.function.ToIntFunction<Entrant> potOf,
                                  int groups, boolean pots, boolean guard) {
        int from = groupIndex(result, s);
        if (from == target) return true;
        if (from < 0 || target < 0) return false;
        List<Entrant> fromG = result.get(from), toG = result.get(target);
        List<Entrant> cands = new ArrayList<>();
        java.util.Map<Entrant, Integer> score = new java.util.HashMap<>();
        for (Entrant p : toG) {
            if (fixed.contains(p) || rank.get(p) < groups) continue;     // Gruppenköpfe und schon Platzierte bleiben
            int dist = Math.abs(rank.get(p) - rank.get(s));
            int sc = dist;
            if (dist > groups - 1) sc += 10_000;                                          // höchstens Gruppenzahl − 1 Plätze
            if (pots && potOf.applyAsInt(p) != potOf.applyAsInt(s)) sc += 100_000;        // gleiche Stärkegruppe bevorzugt
            if (clashCount(fromG, p, s) > 0 && !p.sameClub(s)) sc += 1_000_000;          // möglichst keine neue Gleichheit
            cands.add(p);
            score.put(p, sc);
        }
        cands.sort(Comparator.comparingInt(score::get));
        int before = totalClashes(result);
        for (Entrant best : cands) {
            fromG.set(fromG.indexOf(s), best);
            toG.set(toG.indexOf(best), s);
            if (!guard || totalClashes(result) <= before) return true;     // nie mehr Vereinsgleichheiten als vorher
            toG.set(toG.indexOf(s), best);
            fromG.set(fromG.indexOf(best), s);
        }
        return false;
    }

    /** Untergrenze der Vereinsgleichheiten (Paare): je Verein so gleichmäßig wie möglich auf die Gruppen verteilt. */
    static int minimalClashes(List<Entrant> entrants, int groups) {
        java.util.Map<String, Integer> perClub = new java.util.HashMap<>();
        for (Entrant e : entrants) if (e.clubs().size() == 1) perClub.merge(e.clubs().iterator().next(), 1, Integer::sum);
        int sum = 0;
        for (int m : perClub.values()) {
            int q = m / groups, r = m % groups;
            sum += r * (q + 1) * q / 2 + (groups - r) * q * (q - 1) / 2;
        }
        return sum;
    }

    private static List<List<Entrant>> copy(List<List<Entrant>> groups) {
        List<List<Entrant>> c = new ArrayList<>();
        for (List<Entrant> g : groups) c.add(new ArrayList<>(g));
        return c;
    }

    private static int totalClashes(List<List<Entrant>> groups) {
        int c = 0;
        for (List<Entrant> g : groups)
            for (int i = 0; i < g.size(); i++)
                for (int j = i + 1; j < g.size(); j++) if (g.get(i).sameClub(g.get(j))) c++;
        return c;
    }

    /** Wie viele in {@code group} (ohne {@code without}) mit {@code e} denselben Verein haben. */
    private static int clashCount(List<Entrant> group, Entrant e, Entrant without) {
        int c = 0;
        for (Entrant x : group) if (x != e && x != without && x.sameClub(e)) c++;
        return c;
    }

    /**
     * Eine Stärkegruppe auf die Gruppen verteilen (je Gruppe eine/r). Mit {@code avoidClubs}: möglichst viele
     * Zuordnungen ohne Vereinsgleichheit (maximales Matching); der Rest wird zugelost.
     */
    private static void assignPot(List<Entrant> pot, List<Integer> order, List<List<Entrant>> result, boolean avoidClubs) {
        int m = order.size();
        int[] playerOfGroup = new int[m];
        java.util.Arrays.fill(playerOfGroup, -1);
        if (avoidClubs) {
            int[] groupOfPlayer = new int[pot.size()];
            java.util.Arrays.fill(groupOfPlayer, -1);
            for (int gi = 0; gi < m; gi++) {
                augment(gi, new boolean[pot.size()], order, pot, result, playerOfGroup, groupOfPlayer);
            }
        }
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int p : playerOfGroup) if (p >= 0) used.add(p);
        List<Integer> free = new ArrayList<>();
        for (int p = 0; p < pot.size(); p++) if (!used.contains(p)) free.add(p);
        for (int gi = 0; gi < m && gi < pot.size(); gi++) {
            int p = playerOfGroup[gi] >= 0 ? playerOfGroup[gi] : free.remove(0);
            result.get(order.get(gi)).add(pot.get(p));
        }
        List<Entrant> rest = new ArrayList<>();
        for (int p : free) rest.add(pot.get(p));
        pot.clear();
        pot.addAll(rest);
    }

    private static boolean augment(int gi, boolean[] seen, List<Integer> order, List<Entrant> pot, List<List<Entrant>> result,
                                   int[] playerOfGroup, int[] groupOfPlayer) {
        for (int p = 0; p < pot.size(); p++) {
            if (seen[p] || clashes(result.get(order.get(gi)), pot.get(p))) continue;
            seen[p] = true;
            if (groupOfPlayer[p] < 0 || augment(groupOfPlayer[p], seen, order, pot, result, playerOfGroup, groupOfPlayer)) {
                groupOfPlayer[p] = gi;
                playerOfGroup[gi] = p;
                return true;
            }
        }
        return false;
    }

    /** Erster aus dem (gemischten) Topf ohne Vereinsgleichheit in der Gruppe, sonst der erste überhaupt. */
    private static Entrant pick(List<Entrant> pot, List<Entrant> group, boolean avoidClubs) {
        Entrant chosen = pot.get(0);
        if (avoidClubs) {
            for (Entrant e : pot) {
                if (!clashes(group, e)) {
                    chosen = e;
                    break;
                }
            }
        }
        pot.remove(chosen);
        return chosen;
    }

    private static boolean clashes(List<Entrant> group, Entrant e) {
        for (Entrant x : group) if (x.sameClub(e)) return true;
        return false;
    }

    /** IDs in der Gruppe, die einen Vereinskameraden in derselben Gruppe haben. */
    public static java.util.Set<String> clubClashes(List<Entrant> group) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                if (group.get(i).sameClub(group.get(j))) {
                    set.add(group.get(i).id());
                    set.add(group.get(j).id());
                }
            }
        }
        return set;
    }

    /** Gruppenname: A, B, C, … Z, AA, AB … */
    public static String groupName(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index;
        do {
            sb.insert(0, (char) ('A' + i % 26));
            i = i / 26 - 1;
        } while (i >= 0);
        return sb.toString();
    }

    /** Eine Begegnung „jeder gegen jeden“ mit Runde (Spieltag) innerhalb der Gruppe. */
    public record Pairing(int round, Entrant a, Entrant b) {
    }

    /** 0.40 (#67): feste Spielreihenfolgen je Gruppengröße (gerade Größe; die ungerade eins kleiner lässt P n weg). */
    private static final java.util.Map<Integer, int[][][]> FIXED = java.util.Map.of(
            4, new int[][][]{{{1, 3}, {2, 4}}, {{1, 4}, {2, 3}}, {{1, 2}, {3, 4}}},
            6, new int[][][]{{{1, 5}, {2, 4}, {3, 6}}, {{1, 6}, {2, 5}, {3, 4}}, {{1, 4}, {2, 3}, {5, 6}},
                    {{1, 3}, {2, 6}, {4, 5}}, {{1, 2}, {3, 5}, {4, 6}}},
            8, new int[][][]{{{1, 8}, {2, 7}, {3, 6}, {4, 5}}, {{1, 7}, {2, 6}, {3, 5}, {4, 8}}, {{1, 6}, {2, 5}, {3, 8}, {4, 7}},
                    {{1, 5}, {2, 8}, {3, 7}, {4, 6}}, {{1, 4}, {2, 3}, {5, 8}, {6, 7}}, {{1, 3}, {2, 4}, {5, 7}, {6, 8}},
                    {{1, 2}, {3, 4}, {5, 6}, {7, 8}}},
            10, new int[][][]{{{1, 10}, {2, 9}, {3, 8}, {4, 7}, {5, 6}}, {{1, 9}, {2, 8}, {3, 10}, {4, 6}, {5, 7}},
                    {{1, 8}, {2, 6}, {3, 7}, {4, 10}, {5, 9}}, {{1, 7}, {2, 10}, {3, 6}, {4, 9}, {5, 8}},
                    {{1, 6}, {2, 5}, {3, 4}, {7, 10}, {8, 9}}, {{1, 5}, {2, 4}, {3, 9}, {6, 10}, {7, 8}},
                    {{1, 4}, {2, 3}, {5, 10}, {6, 8}, {7, 9}}, {{1, 3}, {2, 7}, {4, 5}, {6, 9}, {8, 10}},
                    {{1, 2}, {3, 5}, {4, 8}, {6, 7}, {9, 10}}},
            12, new int[][][]{{{1, 12}, {2, 11}, {3, 10}, {4, 9}, {5, 8}, {6, 7}}, {{1, 11}, {2, 10}, {3, 9}, {4, 8}, {5, 7}, {6, 12}},
                    {{1, 10}, {2, 9}, {3, 8}, {4, 7}, {5, 12}, {6, 11}}, {{1, 9}, {2, 8}, {3, 7}, {4, 12}, {5, 11}, {6, 10}},
                    {{1, 8}, {2, 7}, {3, 12}, {4, 11}, {5, 10}, {6, 9}}, {{1, 7}, {2, 12}, {3, 11}, {4, 10}, {5, 9}, {6, 8}},
                    {{1, 6}, {2, 5}, {3, 4}, {7, 12}, {8, 11}, {9, 10}}, {{1, 5}, {2, 4}, {3, 6}, {7, 11}, {8, 10}, {9, 12}},
                    {{1, 4}, {2, 3}, {5, 6}, {7, 10}, {8, 9}, {11, 12}}, {{1, 3}, {2, 6}, {4, 5}, {7, 9}, {8, 12}, {10, 11}},
                    {{1, 2}, {3, 5}, {4, 6}, {7, 8}, {9, 11}, {10, 12}}});

    /**
     * Spielplan „jeder gegen jeden“ nach dem Kreisverfahren: jede Runde spielt jede/r höchstens einmal,
     * bei ungerader Anzahl hat pro Runde eine/r spielfrei. Ergibt n·(n−1)/2 Begegnungen.
     */
    public static List<Pairing> roundRobin(List<Entrant> members) {
        int[][][] fixed = FIXED.get(members.size() % 2 == 1 ? members.size() + 1 : members.size());
        if (fixed != null && members.size() >= 3) {
            // 0.39/0.40 (#67): feste Spielreihenfolge nach Position in der Gruppe (P1 = erste Position nach der Auslosung);
            // bei ungerader Größe entfallen die Spiele gegen die fehlende letzte Position
            List<Pairing> result = new ArrayList<>();
            for (int r = 0; r < fixed.length; r++) {
                for (int[] m : fixed[r]) {
                    if (m[0] > members.size() || m[1] > members.size()) continue;
                    result.add(new Pairing(r + 1, members.get(m[0] - 1), members.get(m[1] - 1)));
                }
            }
            return result;
        }
        List<Entrant> list = new ArrayList<>(members);
        if (list.size() % 2 == 1) list.add(null); // spielfrei
        int n = list.size();
        List<Pairing> result = new ArrayList<>();
        for (int round = 0; round < n - 1; round++) {
            for (int i = 0; i < n / 2; i++) {
                Entrant a = list.get(i);
                Entrant b = list.get(n - 1 - i);
                if (a != null && b != null) {
                    // Heim/Gast abwechseln, damit der Gruppenkopf nicht immer links steht
                    result.add(round % 2 == 0 || i > 0 ? new Pairing(round + 1, a, b) : new Pairing(round + 1, b, a));
                }
            }
            // rotieren, erstes Element bleibt fest
            Entrant last = list.remove(n - 1);
            list.add(1, last);
        }
        return result;
    }
}
