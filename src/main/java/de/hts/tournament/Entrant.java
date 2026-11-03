package de.hts.tournament;

import de.hts.player.Player;

/**
 * Eine Partei in einer Begegnung: eine einzelne Person (Einzel) oder eine Doppelpaarung.
 *
 * @param id     Kennung: Personen-ID bzw. {@code "pair:" + Paarungs-ID}
 * @param name   Anzeigename, z. B. „Brückner, Jonas“ oder „Brückner / Möller“
 * @param ttr    TTR (bei Doppeln der Durchschnitt), {@code null} = unbekannt
 * @param detail Zusatz für die Anzeige, z. B. der Verein
 */
public record Entrant(String id, String name, Integer ttr, String detail) {

    /** Vereine der Partei (Einzel: einer, Doppel: einer oder zwei) – für die Prüfung auf Vereinsgleichheit. */
    public java.util.Set<String> clubs() {
        if (detail == null || detail.isBlank()) return java.util.Set.of();
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String c : detail.split(" / ")) if (!c.isBlank()) set.add(c.trim().toLowerCase(java.util.Locale.GERMAN));
        return set;
    }

    /** Haben beide Parteien einen gemeinsamen Verein? */
    public boolean sameClub(Entrant other) {
        if (other == null) return false;
        java.util.Set<String> mine = clubs();
        for (String c : other.clubs()) if (mine.contains(c)) return true;
        return false;
    }

    public static Entrant of(Player p) {
        return new Entrant(p.getId(), p.getDisplayName(), p.getTtr(), p.getClubName());
    }

    public static Entrant of(DoublesPair pair) {
        String detail = clubs(pair.getFirst(), pair.getSecond());
        return new Entrant("pair:" + pair.getId(), shortName(pair.getFirst()) + " / " + shortName(pair.getSecond()),
                pair.getAverageTtr(), detail);
    }

    private static String shortName(Player p) {
        if (p == null) return "–";
        String ln = p.getLastname() == null ? "" : p.getLastname();
        String fn = p.getFirstname() == null || p.getFirstname().isEmpty() ? "" : " " + p.getFirstname().charAt(0) + ".";
        return ln + fn;
    }

    private static String clubs(Player a, Player b) {
        String ca = a == null ? null : a.getClubName();
        String cb = b == null ? null : b.getClubName();
        if (ca == null) return cb;
        if (cb == null || cb.equals(ca)) return ca;
        return ca + " / " + cb;
    }
}
