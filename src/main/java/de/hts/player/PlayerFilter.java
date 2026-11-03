package de.hts.player;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Suchkriterien für die Spielerliste.
 *
 * <p>Vorname und Nachname werden als Teilstring gesucht, Groß-/Kleinschreibung und Akzente
 * spielen keine Rolle: „muller“, „mueller“ und „Müller“ finden jeweils „Müller“.
 * Das Geburtsdatum wird gegen „TT.MM.JJJJ“ bzw. den Jahrgang geprüft, „1994“ findet also
 * alle mit Jahrgang 1994, „03.1994“ alle mit Geburtsmonat März 1994.
 */
public record PlayerFilter(String firstname, String lastname, String birth,
                           Sex sex, RegistrationFilter registration) implements Predicate<Player> {

    public enum RegistrationFilter {
        ALL("Alle"),
        /** in mindestens einer Klasse */
        REGISTERED("angemeldet"),
        /** in keiner Klasse */
        NOT_REGISTERED("nicht angemeldet"),
        /** mit Wartelisten-Hinweis aus click-TT */
        WAITING_LIST("Warteliste");

        private final String label;

        RegistrationFilter(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final PlayerFilter NONE = new PlayerFilter(null, null, null, null, RegistrationFilter.ALL);

    @Override
    public boolean test(Player p) {
        return matchesName(p.getFirstname(), firstname)
                && matchesName(p.getLastname(), lastname)
                && matchesBirth(p, birth)
                && (sex == null || sex == Sex.UNKNOWN || p.getSex() == sex)
                && matchesRegistration(p);
    }

    private boolean matchesRegistration(Player p) {
        if (registration == null) return true;
        return switch (registration) {
            case ALL -> true;
            case REGISTERED -> p.isRegistered();
            case NOT_REGISTERED -> !p.isRegistered();
            case WAITING_LIST -> !p.getWaitingLists().isEmpty();
        };
    }

    /** Teilstring-Suche ohne Rücksicht auf Groß-/Kleinschreibung, Umlaute und Akzente. */
    public static boolean matchesText(String value, String query) {
        return matchesName(value, query);
    }

    /**
     * Suche über Vorname, Nachname und Verein: jedes Suchwort (durch Leerzeichen getrennt, Satzzeichen am Wortrand
     * ignoriert) muss in mindestens einem der drei Felder als Teilwort vorkommen – ohne Groß-/Kleinschreibung,
     * Reihenfolge egal. Leere Suche passt immer.
     */
    public static boolean matchesPerson(String first, String last, String club, String query) {
        if (query == null || query.isBlank()) return true;
        for (String raw : query.trim().split("\\s+")) {
            String token = raw.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
            if (token.isEmpty()) continue;
            if (!matchesName(first, token) && !matchesName(last, token) && !matchesName(club, token)) return false;
        }
        return true;
    }

    public static boolean matchesPerson(Player p, String query) {
        return p != null && matchesPerson(p.getFirstname(), p.getLastname(), p.getClubName(), query);
    }

    static boolean matchesName(String value, String query) {
        if (query == null || query.isBlank()) return true;
        if (value == null) return false;
        String q = fold(query.trim(), false);
        return fold(value, false).contains(q) || fold(value, true).contains(q);
    }

    static boolean matchesBirth(Player p, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim();
        String text = p.getBirthText();
        if (text.contains(q)) return true;
        // "14.3.1994" soll auch "14.03.1994" finden
        if (p.getBirthdate() != null) {
            var d = p.getBirthdate();
            String loose = d.getDayOfMonth() + "." + d.getMonthValue() + "." + d.getYear();
            return loose.contains(q) || d.toString().contains(q);
        }
        return false;
    }

    /**
     * Vereinheitlicht Text für den Vergleich.
     * @param umlautAsE {@code true}: ä→ae, ö→oe, ü→ue; sonst ä→a usw.
     */
    static String fold(String s, boolean umlautAsE) {
        String t = s.toLowerCase(Locale.GERMAN).replace("ß", "ss");
        if (umlautAsE) {
            t = t.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue");
        }
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.replace("ø", "o").replace("æ", "ae").replace("ı", "i").replace("ł", "l");
    }
}
