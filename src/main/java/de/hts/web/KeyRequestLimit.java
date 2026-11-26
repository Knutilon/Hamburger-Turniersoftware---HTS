/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * 1.12.0 (#189): Ratenbremse für Anträge auf Ausrichter-Schlüssel – höchstens 3 je Adresse und 20 insgesamt pro Stunde.
 * Nur im Arbeitsspeicher und nur als gekürzter Prüfwert der Adresse; nichts wird auf die Platte geschrieben.
 */
final class KeyRequestLimit {

    static final int PER_ADDRESS = 3;
    static final int TOTAL = 20;
    static final long WINDOW_MS = 60 * 60 * 1000L;

    private final Map<String, Deque<Long>> byAddress = new HashMap<>();
    private final Deque<Long> all = new ArrayDeque<>();

    synchronized boolean allow(String address, long now) {
        prune(all, now);
        String k = HexFormat.of().formatHex(LiveHost.sha256(String.valueOf(address).getBytes(StandardCharsets.UTF_8)), 0, 8);
        Deque<Long> d = byAddress.computeIfAbsent(k, x -> new ArrayDeque<>());
        prune(d, now);
        if (d.size() >= PER_ADDRESS || all.size() >= TOTAL) return false;
        d.addLast(now);
        all.addLast(now);
        return true;
    }

    synchronized void cleanUp(long now) {
        prune(all, now);
        byAddress.values().removeIf(d -> {
            prune(d, now);
            return d.isEmpty();
        });
    }

    private static void prune(Deque<Long> d, long now) {
        while (!d.isEmpty() && now - d.peekFirst() > WINDOW_MS) d.removeFirst();
    }
}
