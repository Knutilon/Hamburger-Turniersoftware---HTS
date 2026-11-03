package de.hts.tournament;

/** Spielmodus einer Klasse. */
public enum TournamentMode {
    NONE("– noch kein Modus –"),
    KNOCKOUT("KO-System"),
    GROUPS("Gruppenphase (jeder gegen jeden)"),
    /** Nur als Folgephase nach 2 Gruppen: Endgruppe der besten 2 je Gruppe + Spiele um Platz 5, 7, … */
    PLACEMENT("Gruppenphase mit Platzierungsspielen"),
    /** Nur für Phase 1: feste Rundenzahl, Paarung nach Tabelle, Buchholz-Wertung. */
    SWISS("Schweizer System"),
    /** Nur als Phase 2 nach Gruppen in Phase 1: Beste je Gruppe direkt qualifiziert, Folgende spielen eine KO-Qualifikation. */
    QUALIFICATION("Qualifikationsmodus");

    private final String label;

    TournamentMode(String label) {
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
