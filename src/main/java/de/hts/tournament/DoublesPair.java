package de.hts.tournament;

import de.hts.player.Player;

/**
 * Eine Doppelpaarung in einer Doppelklasse. Eine vollständige Paarung besteht immer aus zwei Teilnehmenden;
 * während der Zuordnung kann ein Platz kurzzeitig frei sein („Partner/in fehlt“).
 * Änderungen bitte über {@link Tournament} vornehmen, damit die Meldungen der Teilnehmenden stimmen.
 */
public final class DoublesPair {

    private final String id;
    private String className;
    private Player first;
    private Player second;

    DoublesPair(String className, Player first, Player second) {
        this(null, className, first, second);
    }

    DoublesPair(String id, String className, Player first, Player second) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString().substring(0, 13).replace("-", "");
        this.className = className;
        this.first = first;
        this.second = second;
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
        Integer a = first == null ? null : first.getTtr(), b = second == null ? null : second.getTtr();
        if (a != null && b != null) return (int) Math.round((a + b) / 2.0);
        if (a != null) return a;
        if (b != null) return b;
        return 0;
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
        return first != null && second != null;
    }

    public boolean isEmpty() {
        return first == null && second == null;
    }

    public boolean contains(Player p) {
        return p != null && (p == first || p == second);
    }

    /** Partner/in von {@code p} in dieser Paarung, oder {@code null}. */
    public Player partnerOf(Player p) {
        if (p == first) return second;
        if (p == second) return first;
        return null;
    }

    /** z. B. „Brückner / Möller“ */
    public String getDisplayName() {
        return name(first) + " / " + name(second);
    }

    private static String name(Player p) {
        return p == null ? "–" : p.getDisplayName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
