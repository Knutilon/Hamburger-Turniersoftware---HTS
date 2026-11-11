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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.40.0 – feste Spielreihenfolge in Gruppen (#67). */
class Version040Test {

    private static List<Entrant> group(int n) {
        List<Entrant> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(new Entrant("P" + i, "P" + i, 2000 - i, "V" + i));
        return l;
    }

    private static String round(List<GroupDraw.Pairing> plan, int r) {
        List<String> out = new ArrayList<>();
        for (GroupDraw.Pairing p : plan) if (p.round() == r) out.add(p.a().id().substring(1) + "-" + p.b().id().substring(1));
        return String.join(" ", out);
    }

    @Test
    void schemataWieVorgegeben() {
        assertEquals("1-3", round(GroupDraw.roundRobin(group(3)), 1));
        assertEquals("2-3", round(GroupDraw.roundRobin(group(3)), 2));
        assertEquals("1-2", round(GroupDraw.roundRobin(group(3)), 3));
        assertEquals("1-3 2-4", round(GroupDraw.roundRobin(group(4)), 1));
        assertEquals("1-5 2-4 3-6", round(GroupDraw.roundRobin(group(6)), 1));
        assertEquals("1-6 2-5 3-4", round(GroupDraw.roundRobin(group(6)), 2));
        assertEquals("1-5 2-4", round(GroupDraw.roundRobin(group(5)), 1), "bei 5 entfällt das Spiel gegen P6");
        assertEquals("1-8 2-7 3-6 4-5", round(GroupDraw.roundRobin(group(8)), 1));
        assertEquals("1-5 2-8 3-7 4-6", round(GroupDraw.roundRobin(group(8)), 4));
        assertEquals("1-2 3-4 5-6 7-8", round(GroupDraw.roundRobin(group(8)), 7));
        assertEquals("2-7 3-6 4-5", round(GroupDraw.roundRobin(group(7)), 1), "bei 7 entfällt das Spiel gegen P8");
        assertEquals("1-10 2-9 3-8 4-7 5-6", round(GroupDraw.roundRobin(group(10)), 1));
        assertEquals("1-2 3-5 4-8 6-7 9-10", round(GroupDraw.roundRobin(group(10)), 9));
        assertEquals("1-12 2-11 3-10 4-9 5-8 6-7", round(GroupDraw.roundRobin(group(12)), 1));
        assertEquals("1-2 3-5 4-6 7-8 9-11 10-12", round(GroupDraw.roundRobin(group(12)), 11));
    }

    @Test
    void jedeGegenJedenGenauEinmal() {
        for (int n = 2; n <= 14; n++) {
            List<GroupDraw.Pairing> plan = GroupDraw.roundRobin(group(n));
            assertEquals(n * (n - 1) / 2, plan.size(), "n=" + n);
            Set<String> pairs = new HashSet<>();
            int rounds = plan.stream().mapToInt(GroupDraw.Pairing::round).max().orElse(0);
            assertEquals(n % 2 == 0 ? n - 1 : n, rounds, "Runden n=" + n);
            for (int r = 1; r <= rounds; r++) {
                Set<String> inRound = new HashSet<>();
                for (GroupDraw.Pairing p : plan) {
                    if (p.round() != r) continue;
                    assertTrue(inRound.add(p.a().id()) && inRound.add(p.b().id()), "n=" + n + " Runde " + r);
                }
            }
            for (GroupDraw.Pairing p : plan) {
                String k = p.a().id().compareTo(p.b().id()) < 0 ? p.a().id() + p.b().id() : p.b().id() + p.a().id();
                assertTrue(pairs.add(k), "n=" + n + " doppelt " + k);
            }
        }
    }
}
