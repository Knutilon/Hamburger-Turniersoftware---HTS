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
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Auslosung für das KO-System.
 *
 * <p>Das Feld wird auf die nächste Zweierpotenz aufgefüllt (2, 4, 8, 16, …); fehlende Plätze sind Freilose.
 * Die Plätze werden nach der Setzliste belegt – bei 16 Plätzen (von oben nach unten):
 * 1–16, 9–8, 5–12, 13–4, 3–14, 11–6, 7–10, 15–2; bei 8 Plätzen 1–8, 5–4, 3–6, 7–2; bei 4 Plätzen 1–4, 3–2;
 * für 32/64 Plätze entsprechend hochgerechnet (die Ersten der Paarungen sind 1, 2·k+1 in Bit-Umkehr-Reihenfolge,
 * Gegner ist jeweils Platz n+1−s):
 * <ul>
 *   <li>Nr. 1 und Nr. 2 stehen in verschiedenen Hälften und können sich erst im Finale treffen,</li>
 *   <li>Nr. 1 und Nr. 4 stehen in derselben Hälfte (Halbfinale), ebenso Nr. 2 und Nr. 3,</li>
 *   <li>Freilose gehen an die am besten Gesetzten, und nie treffen zwei Freilose aufeinander.</li>
 * </ul>
 * <b>Nach Setzung</b> ist die Reihenfolge der TTR-Wert (absteigend; gleiche Werte und fehlende TTR werden ausgelost).
 * <b>Ohne Setzung</b> wird die Reihenfolge komplett zufällig bestimmt.
 */
public final class KnockoutDraw {

    private KnockoutDraw() {
    }

    /** Größe des KO-Felds: nächste Zweierpotenz ≥ Anzahl (mindestens 2). */
    public static int bracketSize(int entrants) {
        int size = 2;
        while (size < entrants) size *= 2;
        return size;
    }

    /**
     * Setzliste: für jeden Platz (von oben nach unten) die Setznummer (Rang nach TTR bzw. Ø-TTR).
     * 16 → [1, 16, 9, 8, 5, 12, 13, 4, 3, 14, 11, 6, 7, 10, 15, 2]; 8 → [1, 8, 5, 4, 3, 6, 7, 2]; 4 → [1, 4, 3, 2].
     */
    public static int[] seedOrder(int size) {
        int pairs = Math.max(1, size / 2);
        int bits = Integer.numberOfTrailingZeros(pairs);
        int[] order = new int[Math.max(2, size)];
        for (int i = 0; i < pairs; i++) {
            int rev = bits == 0 ? 0 : Integer.reverse(i) >>> (32 - bits);
            int s = 2 * rev + 1;
            order[2 * i] = s;
            order[2 * i + 1] = order.length + 1 - s;
        }
        return order;
    }

    /**
     * Vermeidet Erstrundenspiele von Vereinskameraden: Bei Vereinsgleichheit wird der schwächer Gesetzte mit dem
     * Nächstplatzierten (kleinster Rangabstand) einer anderen Paarung getauscht – nur wenn danach beide Paarungen
     * ohne Vereinsgleichheit sind. Geht das nicht, bleibt es (dann ist nur noch Vereinsgleichheit möglich).
     *
     * @param rankAt Rang je Platz (kleiner = stärker); bestimmt, wer getauscht wird und mit wem
     */
    public static void avoidClubClashes(List<Entrant> slots, int[] rankAt) {
        for (int pass = 0; pass < 3; pass++) {
            boolean changed = false;
            for (int i = 0; i + 1 < slots.size(); i += 2) {
                Entrant a = slots.get(i), b = slots.get(i + 1);
                if (a == null || b == null || !a.sameClub(b)) continue;
                int weak = rankAt[i] > rankAt[i + 1] ? i : i + 1;
                Integer best = null;
                for (int q = 0; q < slots.size(); q++) {
                    if (q / 2 == i / 2 || slots.get(q) == null) continue;
                    int partner = q % 2 == 0 ? q + 1 : q - 1;
                    Entrant moving = slots.get(weak), other = slots.get(q), stay = slots.get(weak == i ? i + 1 : i);
                    Entrant qPartner = slots.get(partner);
                    if (stay.sameClub(other) || moving.sameClub(qPartner)) continue;
                    if (best == null || Math.abs(rankAt[q] - rankAt[weak]) < Math.abs(rankAt[best] - rankAt[weak])) best = q;
                }
                if (best != null) {
                    Entrant tmp = slots.get(weak);
                    slots.set(weak, slots.get(best));
                    slots.set(best, tmp);
                    changed = true;
                }
            }
            if (!changed) break;
        }
    }

    /** Plätze, deren Erstrundengegner aus demselben Verein ist. */
    public static java.util.Set<Integer> clubClashPositions(List<Entrant> slots) {
        java.util.Set<Integer> set = new java.util.HashSet<>();
        for (int i = 0; i + 1 < slots.size(); i += 2) {
            if (slots.get(i) != null && slots.get(i).sameClub(slots.get(i + 1))) {
                set.add(i);
                set.add(i + 1);
            }
        }
        return set;
    }

    /**
     * Lost aus.
     *
     * @return Belegung der Plätze von oben nach unten (Länge = {@link #bracketSize}); {@code null} = Freilos
     */
    public static List<Entrant> draw(List<Entrant> entrants, boolean seeded, Random random) {
        return draw(entrants, seeded, false, random);
    }

    /** Wie {@link #draw(List, boolean, Random)}; mit {@code avoidClubs} werden Vereinskameraden in Runde 1 getrennt. */
    public static List<Entrant> draw(List<Entrant> entrants, boolean seeded, boolean avoidClubs, Random random) {
        if (entrants.size() < 2) {
            throw new IllegalArgumentException("Für eine Auslosung braucht es mindestens zwei Teilnehmende bzw. Doppel.");
        }
        List<Entrant> ranking = new ArrayList<>(entrants);
        Collections.shuffle(ranking, random);           // zufällige Reihenfolge …
        if (seeded) {                                   // … bei Setzung stabil nach TTR sortiert (Gleichstand bleibt zufällig)
            ranking.sort(Comparator.comparing(Entrant::ttr, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        int size = bracketSize(ranking.size());
        int[] order = seedOrder(size);
        List<Entrant> slots = new ArrayList<>(size);
        for (int seed : order) {
            slots.add(seed <= ranking.size() ? ranking.get(seed - 1) : null);
        }
        if (avoidClubs) avoidClubClashes(slots, order);
        return slots;
    }

    /**
     * KO-Feld für Phase 2/3 aus einer Platzierungsliste (alle Gruppenersten – die besten zuerst –, dann alle Zweiten …):
     * <ol>
     *   <li>Freilose gehen an die Besten der Liste (also die besten Gruppenersten).</li>
     *   <li>Der Höchstplatzierte ohne Freilos spielt gegen den Niedrigstplatzierten, der noch nicht zugeordnet ist und
     *       nicht aus seiner Gruppe kommt; dann der Nächste usw.</li>
     *   <li>Die Paarungen werden nach dem Rang ihres Besseren platziert (1 oben, 2 unten …); Teilnehmende derselben
     *       Gruppe kommen dabei in verschiedene Hälften (Viertel …), damit sie sich erst im Finale treffen können.</li>
     * </ol>
     *
     * @param groupOf Gruppe je Entrant-ID (leer = keine Gruppen, z. B. nach dem Schweizer System)
     */
    public static List<Entrant> drawFromRanking(List<Entrant> ranked, Map<String, Integer> groupOf, int firstSeeds, Random random) {
        if (ranked.size() < 2) {
            throw new IllegalArgumentException("Für eine Auslosung braucht es mindestens zwei Teilnehmende bzw. Doppel.");
        }
        int n = ranked.size();
        int size = bracketSize(n);
        int byes = size - n;
        // Einheiten = Erstrunden-Paarungen (bzw. Freilos), in Rangfolge ihres Besseren
        List<Entrant[]> units = new ArrayList<>();
        for (int k = 0; k < byes; k++) units.add(new Entrant[]{ranked.get(k), null});
        List<Entrant> rest = new ArrayList<>(ranked.subList(byes, n));
        while (!rest.isEmpty()) {
            Entrant top = rest.remove(0);
            Integer g = groupOf.get(top.id());
            int pick = -1;
            for (int k = rest.size() - 1; k >= 0; k--) {
                if (g == null || !g.equals(groupOf.get(rest.get(k).id()))) {
                    pick = k;
                    break;
                }
            }
            if (pick < 0) pick = rest.size() - 1;
            units.add(new Entrant[]{top, rest.remove(pick)});
        }
        List<Entrant[]> arranged = arrange(units, groupOf);
        List<Entrant> slots = new ArrayList<>(size);
        for (Entrant[] u : arranged) {
            slots.add(u[0]);
            slots.add(u[1]);
        }
        return slots;
    }

    /** Verteilt die Einheiten (in Rangfolge) rekursiv auf Hälften, Viertel …: Setzmuster, Gruppengleiche getrennt. */
    private static List<Entrant[]> arrange(List<Entrant[]> units, Map<String, Integer> groupOf) {
        int m = units.size();
        if (m == 1) return units;
        int[] order = seedOrder(m);
        boolean[] topHalf = new boolean[m + 1];
        for (int pos = 0; pos < m; pos++) topHalf[order[pos]] = pos < m / 2;
        List<Entrant[]> top = new ArrayList<>(), bottom = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            Entrant[] u = units.get(k);
            boolean preferTop = topHalf[k + 1];
            List<Entrant[]> pref = preferTop ? top : bottom, other = preferTop ? bottom : top;
            List<Entrant[]> target = pref;
            if (pref.size() >= m / 2) {
                target = other;
            } else if (other.size() < m / 2 && clash(pref, u, groupOf) > clash(other, u, groupOf)) {
                target = other;
            }
            target.add(u);
        }
        List<Entrant[]> result = new ArrayList<>(arrange(top, groupOf));
        result.addAll(arrange(bottom, groupOf));
        return result;
    }

    /** Wie viele Gruppengleiche die Einheit in dieser Hälfte hätte. */
    private static int clash(List<Entrant[]> half, Entrant[] unit, Map<String, Integer> groupOf) {
        int c = 0;
        for (Entrant e : unit) {
            if (e == null) continue;
            Integer g = groupOf.get(e.id());
            if (g == null) continue;
            for (Entrant[] u : half) for (Entrant x : u) if (x != null && g.equals(groupOf.get(x.id()))) c++;
        }
        return c;
    }

    /** Rundenname nach Anzahl der Plätze in der Runde, z. B. 8 → „Viertelfinale“. */
    public static String roundName(int slotsInRound) {
        return switch (slotsInRound) {
            case 2 -> "Finale";
            case 4 -> "Halbfinale";
            case 8 -> "Viertelfinale";
            case 16 -> "Achtelfinale";
            case 32 -> "Sechzehntelfinale";
            default -> "Runde der letzten " + slotsInRound;
        };
    }
}
