package de.hts.tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Übersicht der Begegnungen eines KO-Felds, berechnet aus der Platzbelegung der Auslosung.
 * Begegnungen mit Freilos werden nicht gespielt; wer ein Freilos hat, steht direkt in der nächsten Runde.
 */
public final class KnockoutBracket {

    /** Eine Seite einer Begegnung: bekannte Partei, Freilos oder Platzhalter („Sieger/in Spiel 3“). */
    public record Side(Entrant entrant, boolean bye, String placeholder) {
        static Side of(Entrant e) {
            return e == null ? new Side(null, true, null) : new Side(e, false, null);
        }

        static Side winnerOf(int matchNumber) {
            return new Side(null, false, "Sieger/in Spiel " + matchNumber);
        }

        public String label() {
            if (entrant != null) return entrant.name();
            if (bye) return "Freilos";
            return placeholder;
        }
    }

    /**
     * Eine Begegnung. {@code number} ist die fortlaufende Spielnummer; Begegnungen mit Freilos haben keine (0).
     */
    public record Match(int number, Side a, Side b) {
        public boolean isBye() {
            return a.bye() || b.bye();
        }
    }

    public record Round(String name, List<Match> matches) {
    }

    private final List<Round> rounds = new ArrayList<>();

    public KnockoutBracket(List<Entrant> slots) {
        int matchNo = 0;
        List<Side> current = new ArrayList<>();
        for (Entrant e : slots) current.add(Side.of(e));
        while (current.size() >= 2) {
            List<Match> matches = new ArrayList<>();
            List<Side> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                Side a = current.get(i);
                Side b = current.get(i + 1);
                if (a.bye() || b.bye()) {
                    matches.add(new Match(0, a, b));
                    next.add(a.bye() ? b : a);          // Freilos: die andere Seite kommt weiter
                } else {
                    Match m = new Match(++matchNo, a, b);
                    matches.add(m);
                    next.add(Side.winnerOf(m.number()));
                }
            }
            rounds.add(new Round(KnockoutDraw.roundName(current.size()), matches));
            current = next;
        }
    }

    public List<Round> getRounds() {
        return rounds;
    }

    /** Anzahl tatsächlich zu spielender Begegnungen (ohne Freilose). */
    public int playedMatches() {
        return (int) rounds.stream().flatMap(r -> r.matches().stream()).filter(m -> !m.isBye()).count();
    }
}
