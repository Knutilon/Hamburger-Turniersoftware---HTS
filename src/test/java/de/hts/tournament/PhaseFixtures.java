/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.player.Player;
import de.hts.player.Sex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Gemeinsame Testdaten (0.38). */
final class PhaseFixtures {

    private PhaseFixtures() {
    }

    static PlayingClass groupsClass(Tournament t, String name, int n, int groupCount) {
        PlayingClass c = t.addClass(new PlayingClass(name));
        List<Player> ps = new ArrayList<>();
        Random rnd = new Random(7);
        for (int i = 1; i <= n; i++) {
            Player p = new Player(name + i, "N" + name + i);
            p.setInternalNr(name + "-" + i);
            p.setSex(Sex.MALE);
            p.setTtr(1100 + rnd.nextInt(900));
            p.setClubName("Verein " + name + i);
            ps.add(p);
        }
        t.merge(ps);
        ps.forEach(p -> t.assign(p, c));
        c.setGroupsBySize(false);
        c.setGroupCount(groupCount);
        t.drawGroups(c, true, new Random(3));
        return c;
    }

    /** Höherer TTR gewinnt 3:1 bzw. 3:0. */
    static void playAll(Tournament t, PlayingClass c) {
        for (int guard = 0; guard < 5000; guard++) {
            Match open = t.matchesOf(c).stream()
                    .filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED))
                    .findFirst().orElse(null);
            if (open == null) return;
            play(t, open);
        }
    }

    static void play(Tournament t, Match open) {
        Entrant a = t.entrant(open, open.getA()), b = t.entrant(open, open.getB());
        boolean aWins = (a.ttr() == null ? 0 : a.ttr()) >= (b.ttr() == null ? 0 : b.ttr());
        int lose = 3 + open.getNumber() % 7;
        List<int[]> sets = List.of(new int[]{11, lose}, new int[]{11, lose}, new int[]{11, lose});
        List<int[]> flipped = new ArrayList<>();
        for (int[] st : sets) flipped.add(new int[]{st[1], st[0]});
        t.recordResult(open, aWins ? sets : flipped, 0);
    }
}
