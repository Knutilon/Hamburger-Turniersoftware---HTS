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

/**
 * Eine Doppelpaarung in einer Doppelklasse bzw. ein Team im 2er/3er Turnier. Vollständig mit zwei (Doppel, 2er Team)
 * bzw. drei Teilnehmenden (3er Team); während der Zuordnung kann ein Platz kurzzeitig frei sein.
 * Änderungen bitte über {@link Tournament} vornehmen, damit die Meldungen der Teilnehmenden stimmen.
 */
public final class DoublesPair {

    private final String id;
    private String className;
    private Player first;
    private Player second;
    private Player third;
    /** Anzahl Plätze: 2 (Doppel, 2er Team) oder 3 (3er Team) */
    private int capacity = 2;

    DoublesPair(String className, Player first, Player second) {
        this(null, className, first, second);
    }

    DoublesPair(String id, String className, Player first, Player second) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString().substring(0, 13).replace("-", "");
        this.className = className;
        this.first = first;
        this.second = second;
    }

    public int getCapacity() {
        return capacity;
    }

    void setCapacity(int n) {
        capacity = Math.max(2, Math.min(3, n));
    }

    public Player getThird() {
        return third;
    }

    void setThird(Player p) {
        third = p;
    }

    /** Alle (gesetzten) Mitglieder in Reihenfolge. */
    public java.util.List<Player> members() {
        java.util.List<Player> out = new java.util.ArrayList<>(3);
        if (first != null) out.add(first);
        if (second != null) out.add(second);
        if (third != null) out.add(third);
        return out;
    }

    /** Stabile Kennung (für die Auslosung in der Turnierdatei). */
    public String getId() {
        return id;
    }

    /**
     * Durchschnitts-TTR des Doppels: (TTR 1 + TTR 2) / 2, gerundet.
     * {@code null}, solange die Paarung unvollständig ist oder ein TTR-Wert fehlt.
     */
    /**
     * TTR des Doppels für die Setzung: Durchschnitt beider Werte; fehlt einer, zählt der Wert des Partners; fehlen
     * beide, 0 (wird als letztes gesetzt, mehrere mit 0 in zufälliger Reihenfolge).
     */
    public Integer getAverageTtr() {
        java.util.List<Integer> known = members().stream().map(Player::getTtr).filter(java.util.Objects::nonNull).toList();
        if (known.isEmpty()) return 0;
        return (int) Math.round(known.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    public String getClassName() {
        return className;
    }

    void setClassName(String className) {
        this.className = className;
    }

    public Player getFirst() {
        return first;
    }

    public Player getSecond() {
        return second;
    }

    void setFirst(Player p) {
        first = p;
    }

    void setSecond(Player p) {
        second = p;
    }

    public boolean isComplete() {
        return members().size() >= capacity;
    }

    public boolean isEmpty() {
        return first == null && second == null && third == null;
    }

    public boolean contains(Player p) {
        return p != null && (p == first || p == second || p == third);
    }

    /** Partner/in von {@code p} in dieser Paarung, oder {@code null}. */
    public Player partnerOf(Player p) {
        if (p == first) return second;
        if (p == second) return first;
        return null;
    }

    /** z. B. „Brückner / Möller“ */
    public String getDisplayName() {
        String s = name(first) + " / " + name(second);
        return capacity >= 3 ? s + " / " + name(third) : s;
    }

    private static String name(Player p) {
        return p == null ? "–" : p.getDisplayName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
