package de.hts.tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Stationen der Präsentationsansicht (zweiter Bildschirm) in der Reihenfolge der Rotation:
 * <ol>
 *   <li>laufende Klassen mit ihrer laufenden (bzw. angehaltenen) Phase,</li>
 *   <li>die Tischansicht und „Die nächsten 16 Spiele“,</li>
 *   <li>beendete Phasen (laufender wie beendeter Klassen),</li>
 *   <li>die Endergebnisse beendeter Klassen.</li>
 * </ol>
 * Innerhalb jedes Blocks gilt die Reihenfolge der Klassenliste. Nicht gestartete Klassen fehlen.
 */
public final class PresentationPlan {

    public enum Kind { PHASE, TABLES, NEXT, RESULT }

    /**
     * @param key      Schlüssel zum Ein-/Ausschalten (bleibt über Neustarts gleich)
     * @param kind     Art der Station
     * @param phase    Phase (PHASE) bzw. Hauptklasse (RESULT), sonst {@code null}
     * @param finished abgeschlossene Phase bzw. Endergebnis
     */
    public record Station(String key, Kind kind, PlayingClass phase, boolean finished) {
    }

    public static final String TABLES = "tables";
    public static final String NEXT = "next16";

    private PresentationPlan() {
    }

    public static String phaseKey(PlayingClass phase) {
        return "phase:" + phase.getName();
    }

    public static String resultKey(PlayingClass root) {
        return "result:" + root.getName();
    }

    /** Alle Stationen (auch ausgeschaltete). */
    public static List<Station> all(Tournament t) {
        List<PlayingClass> roots = t.getClasses().stream().filter(c -> !c.isPhase() && c.isStarted()).toList();
        List<Station> running = new ArrayList<>(), done = new ArrayList<>(), results = new ArrayList<>();
        for (PlayingClass root : roots) {
            List<PlayingClass> phases = t.phasesOf(root);
            if (phases.isEmpty()) continue;
            boolean finished = root.isFinished();
            PlayingClass currentPhase = phases.get(phases.size() - 1);
            for (PlayingClass ph : phases) {
                if (!finished && ph == currentPhase) running.add(new Station(phaseKey(ph), Kind.PHASE, ph, false));
                else done.add(new Station(phaseKey(ph), Kind.PHASE, ph, true));
            }
            if (finished) results.add(new Station(resultKey(root), Kind.RESULT, root, true));
        }
        List<Station> out = new ArrayList<>(running);
        out.add(new Station(TABLES, Kind.TABLES, null, false));
        out.add(new Station(NEXT, Kind.NEXT, null, false));
        out.addAll(done);
        out.addAll(results);
        return out;
    }

    /** Nur die eingeschalteten Stationen. */
    public static List<Station> active(Tournament t) {
        return all(t).stream().filter(s -> !t.getPresentationOff().contains(s.key())).toList();
    }
}
