package de.hts.tournament;

/** Status eines Spiels. */
public enum MatchStatus {
    /** ausgelost, die Klasse ist aber noch nicht gestartet (bzw. Gegner stehen noch nicht fest) */
    PLANNED("geplant"),
    /** in der Warteschlange für einen freien Tisch */
    QUEUED("eingereiht"),
    /** läuft an einem Tisch */
    ACTIVE("aktiv"),
    /** Ergebnis erfasst */
    DONE("abgeschlossen"),
    /** an einem Tisch, aber angehalten (Phase pausiert) – kein Ergebnis möglich, muss bewusst fortgesetzt werden */
    PAUSED("pausiert");

    /** Belegt das Spiel einen Tisch (aktiv oder pausiert)? */
    public boolean atTable() {
        return this == ACTIVE || this == PAUSED;
    }

    private final String label;

    MatchStatus(String label) {
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
