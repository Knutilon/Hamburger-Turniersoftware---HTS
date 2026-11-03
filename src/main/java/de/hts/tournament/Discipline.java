package de.hts.tournament;

/** Spielart einer Klasse: Einzel oder Doppel. */
public enum Discipline {
    SINGLES("Einzel"),
    DOUBLES("Doppel");

    private final String label;

    Discipline(String label) {
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
