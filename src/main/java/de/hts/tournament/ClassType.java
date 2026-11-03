package de.hts.tournament;

import de.hts.player.Sex;

import java.util.Locale;

/**
 * Art einer Spielklasse, gebildet aus zwei Merkmalen:
 *
 * <table>
 *   <tr><th></th><th>weiblich und männlich</th><th>nur weiblich</th></tr>
 *   <tr><th>Erwachsene (jedes Alter)</th><td>{@link #ADULTS Erwachsene}</td><td>{@link #WOMEN Damen}</td></tr>
 *   <tr><th>Jugend (unter 21)</th><td>{@link #YOUTH Jugend}</td><td>{@link #GIRLS Mädchen}</td></tr>
 * </table>
 *
 * Spieler unter 21 dürfen immer auch in Erwachsenen- bzw. Damenklassen starten.
 */
public enum ClassType {
    ADULTS("Erwachsene", false, false),
    WOMEN("Damen", false, true),
    YOUTH("Jugend", true, false),
    GIRLS("Mädchen", true, true);

    /** Jugend = jünger als 21 Jahre. */
    public static final int YOUTH_AGE_LIMIT = 21;

    private final String label;
    private final boolean youthOnly;
    private final boolean femaleOnly;

    ClassType(String label, boolean youthOnly, boolean femaleOnly) {
        this.label = label;
        this.youthOnly = youthOnly;
        this.femaleOnly = femaleOnly;
    }

    public String getLabel() {
        return label;
    }

    /** nur Spieler unter 21 */
    public boolean isYouthOnly() {
        return youthOnly;
    }

    /** nur Spielerinnen */
    public boolean isFemaleOnly() {
        return femaleOnly;
    }

    /** Beschreibung für Auswahllisten, z. B. „Mädchen – nur weiblich, unter 21“. */
    public String getDescription() {
        return label + " – " + (femaleOnly ? "nur weiblich" : "weiblich und männlich")
                + ", " + (youthOnly ? "unter " + YOUTH_AGE_LIMIT : "jedes Alter");
    }

    public static ClassType of(boolean youthOnly, boolean femaleOnly) {
        for (ClassType t : values()) {
            if (t.youthOnly == youthOnly && t.femaleOnly == femaleOnly) return t;
        }
        throw new IllegalStateException();
    }

    /**
     * Schätzt den Typ aus Name und Geschlechtsangabe einer click-TT-Konkurrenz,
     * z. B. „Damen Einzel“ → Damen, „Mädchen U15“ → Mädchen, „Jungen 18“ → Jugend, „Herren“ → Erwachsene.
     */
    public static ClassType guess(String name, Sex sex) {
        String n = name == null ? "" : name.toLowerCase(Locale.GERMAN);
        boolean youth = n.matches(".*\\b(jugend|jungen|mädchen|maedchen|schüler|schueler|schülerinnen|nachwuchs|u\\s?\\d{1,2})\\b.*");
        boolean female = sex == Sex.FEMALE
                || n.matches(".*\\b(damen|frauen|mädchen|maedchen|schülerinnen|seniorinnen|juniorinnen)\\b.*");
        return of(youth, female);
    }

    @Override
    public String toString() {
        return label;
    }
}
