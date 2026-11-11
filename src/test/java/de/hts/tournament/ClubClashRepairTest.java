/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Schlange/Stärkegruppen mit „Vereinsgleichheit beachten“: keine vermeidbaren Vereinsgleichheiten. */
class ClubClashRepairTest {

    private static int clashPairs(List<List<Entrant>> groups) {
        int c = 0;
        for (List<Entrant> g : groups)
            for (int i = 0; i < g.size(); i++)
                for (int j = i + 1; j < g.size(); j++) if (g.get(i).sameClub(g.get(j))) c++;
        return c;
    }

    /** Untergrenze: jeder Verein gleichmäßig auf die Gruppen verteilt. */
    private static int lowerBound(List<Entrant> es, int groups) {
        Map<String, Integer> per = new HashMap<>();
        for (Entrant e : es) per.merge(e.detail(), 1, Integer::sum);
        int lb = 0;
        for (int k : per.values()) {
            int q = k / groups, r = k % groups;
            lb += r * (q + 1) * q / 2 + (groups - r) * q * (q - 1) / 2;
        }
        return lb;
    }

    @Test
    void keineVereinsgleichheitWennVermeidbar() {
        // jeder Verein höchstens so oft wie es Gruppen gibt -> ohne Vereinsgleichheit möglich
        for (int seed = 0; seed < 60; seed++) {
            Random r = new Random(seed);
            for (int groups : new int[]{2, 3, 4, 6}) {
                int n = (3 + r.nextInt(3)) * groups;       // gleich große Gruppen, jeder Verein genau „Gruppen“-mal
                List<String> pool = new ArrayList<>();
                for (int c = 0; pool.size() < n; c++) for (int k = 0; k < groups && pool.size() < n; k++) pool.add("Verein " + c);
                java.util.Collections.shuffle(pool, r);
                List<Entrant> es = new ArrayList<>();
                for (int i = 0; i < n; i++) es.add(new Entrant("p" + i, "P" + i, 2000 - i * 7, pool.get(i)));
                for (GroupDraw.Seeding sd : GroupDraw.Seeding.values()) {
                    List<List<Entrant>> res = GroupDraw.draw(es, groups, sd, true, new Random(seed));
                    if (sd != GroupDraw.Seeding.POTS) assertEquals(0, clashPairs(res), "seed " + seed + ", " + groups + " Gruppen, " + sd);
                }
            }
        }
    }

    @Test
    void keinEinzelnerTauschVerbessertNoch() {
        String[] clubs = {"Horner TV", "Horner TV", "Hamburger SV", "Walddörfer SV", "Horner TV", "Oberalster VfW",
                "Meiendorfer SV", "Niendorfer TSV", "Eimsbütteler TV", "Horner TV", "TSV Sasel", "SC Poppenbüttel"};
        for (int seed = 0; seed < 40; seed++) {
            Random r = new Random(seed);
            int groups = 2 + r.nextInt(5);
            int n = 5 * groups + r.nextInt(groups);
            List<Entrant> es = new ArrayList<>();
            for (int i = 0; i < n; i++) es.add(new Entrant("p" + i, "P" + i, 2000 - i * 7, clubs[r.nextInt(clubs.length)]));
            List<List<Entrant>> res = GroupDraw.draw(es, groups, GroupDraw.Seeding.SNAKE, true, new Random(seed));
            int base = clashPairs(res);
            assertTrue(base >= lowerBound(es, groups));
            for (int g = 0; g < groups; g++)
                for (int h = g + 1; h < groups; h++)
                    for (int i = 1; i < res.get(g).size(); i++)
                        for (int j = 1; j < res.get(h).size(); j++) {
                            Entrant a = res.get(g).get(i), b = res.get(h).get(j);
                            res.get(g).set(i, b);
                            res.get(h).set(j, a);
                            assertTrue(clashPairs(res) >= base, "seed " + seed + ": Tausch hätte geholfen");
                            res.get(g).set(i, a);
                            res.get(h).set(j, b);
                        }
        }
    }

    @Test
    void gruppenkoepfeBleibenBeiDerSchlange() {
        List<Entrant> es = new ArrayList<>();
        for (int i = 0; i < 16; i++) es.add(new Entrant("p" + i, "P" + i, 2000 - i * 10, i % 3 == 0 ? "Horner TV" : "Verein " + i));
        List<List<Entrant>> res = GroupDraw.draw(es, 4, GroupDraw.Seeding.SNAKE, true, new Random(1));
        for (int g = 0; g < 4; g++) assertEquals("P" + g, res.get(g).get(0).name(), "Kopf von Gruppe " + g);
    }
}
