package de.hts.player;

/** Anwesenheit am Turniertag (0.35): „Angemeldet“ in der Teilnehmenden-Übersicht. */
public enum Attendance {
    /** Noch nicht an- oder abgemeldet. */
    UNKNOWN("offen"),
    /** Am Turniertag anwesend („angemeldet“). */
    PRESENT("angemeldet"),
    /** Per Handy als „nicht anwesend“ gemeldet – die Turnierleitung muss das noch bestätigen. */
    WITHDRAW_REQUESTED("Abmeldung bestätigen"),
    /** Endgültig abgemeldet (von der Turnierleitung bestätigt bzw. selbst abgemeldet). */
    WITHDRAWN("abgemeldet");

    private final String label;

    Attendance(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Für den Start einer Klasse geklärt: angemeldet oder endgültig abgemeldet. */
    public boolean isSettled() {
        return this == PRESENT || this == WITHDRAWN;
    }

    public String code() {
        return switch (this) {
            case UNKNOWN -> "";
            case PRESENT -> "present";
            case WITHDRAW_REQUESTED -> "withdraw-requested";
            case WITHDRAWN -> "withdrawn";
        };
    }

    public static Attendance ofCode(String s) {
        if (s == null) return UNKNOWN;
        return switch (s) {
            case "present" -> PRESENT;
            case "withdraw-requested" -> WITHDRAW_REQUESTED;
            case "withdrawn" -> WITHDRAWN;
            default -> UNKNOWN;
        };
    }
}
