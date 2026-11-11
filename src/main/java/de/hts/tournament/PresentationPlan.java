/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Stationen der Präsentationsansicht (zweiter Bildschirm) in der Reihenfolge der Rotation:
 * <ol>
 *   <li>laufende Klassen mit ihrer laufenden (bzw. angehaltenen) Phase,</li>
 *   <li>die Tischansicht und „Die nächsten 16 Spiele“,</li>
 *   <li>beendete Phasen (laufender wie beendeter Klassen),</li>
 *   <li>die Endergebnisse beendeter Klassen,</li>
 *   <li>ausgeloste, noch nicht gestartete Klassen (standardmäßig ausgeschaltet).</li>
 * </ol>
 * Innerhalb jedes Blocks gilt die Reihenfolge der Klassenliste. Nicht ausgeloste Klassen fehlen.
 */
public final class PresentationPlan {

    public enum Kind { PHASE, TABLES, NEXT, RESULT }

    /** Zustand einer Klasse für die Auswahl der Ansichten. */
    public enum ClassState {
        NOT_DRAWN("nicht ausgelost"), DRAWN("ausgelost"), RUNNING("läuft"), PAUSED("angehalten"), FINISHED("beendet");

        private final String label;

        ClassState(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * @param key      Schlüssel zum Ein-/Ausschalten (bleibt über Neustarts gleich)
     * @param kind     Art der Station
     * @param phase    Phase (PHASE) bzw. Hauptklasse (RESULT), sonst {@code null}
     * @param finished abgeschlossene Phase bzw. Endergebnis
     * @param upcoming ausgeloste, noch nicht gestartete Klasse (standardmäßig nicht in der Rotation)
     */
    public record Station(String key, Kind kind, PlayingClass phase, boolean finished, boolean upcoming) {
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

    /** Zustand einer Hauptklasse. */
    public static ClassState stateOf(Tournament t, PlayingClass root) {
        if (root.isFinished()) return ClassState.FINISHED;
        List<PlayingClass> phases = t.phasesOf(root);
        PlayingClass current = phases.isEmpty() ? root : phases.get(phases.size() - 1);
        if (root.isStarted()) return current.isPaused() || root.isPaused() ? ClassState.PAUSED : ClassState.RUNNING;
        return root.isDrawn() ? ClassState.DRAWN : ClassState.NOT_DRAWN;
    }

    /** Alle Stationen (auch ausgeschaltete) in der Reihenfolge der Rotation. */
    public static List<Station> all(Tournament t) {
        List<PlayingClass> roots = t.getClasses().stream().filter(c -> !c.isPhase()).toList();
        List<Station> running = new ArrayList<>(), done = new ArrayList<>(), results = new ArrayList<>(), upcoming = new ArrayList<>();
        for (PlayingClass root : roots) {
            if (!root.isStarted()) {
                if (root.isDrawn()) upcoming.add(new Station(phaseKey(root), Kind.PHASE, root, false, true));
                continue;
            }
            List<PlayingClass> phases = t.phasesOf(root);
            if (phases.isEmpty()) continue;
            boolean finished = root.isFinished();
            PlayingClass currentPhase = phases.get(phases.size() - 1);
            for (PlayingClass ph : phases) {
                if (!finished && ph == currentPhase) running.add(new Station(phaseKey(ph), Kind.PHASE, ph, false, false));
                else done.add(new Station(phaseKey(ph), Kind.PHASE, ph, true, false));
            }
            if (finished) results.add(new Station(resultKey(root), Kind.RESULT, root, true, false));
        }
        List<Station> out = new ArrayList<>(running);
        out.add(new Station(TABLES, Kind.TABLES, null, false, false));
        out.add(new Station(NEXT, Kind.NEXT, null, false, false));
        out.addAll(done);
        out.addAll(results);
        out.addAll(upcoming);
        return out;
    }

    /** Ist die Station in der Rotation? Ausdrücklich ein-/ausgeschaltet, sonst: alles außer noch nicht gestarteten Klassen. */
    public static boolean isOn(Tournament t, Station s) {
        if (t.getPresentationOn().contains(s.key())) return true;
        if (t.getPresentationOff().contains(s.key())) return false;
        return !s.upcoming();
    }

    /** Nur die eingeschalteten Stationen. */
    public static List<Station> active(Tournament t) {
        return all(t).stream().filter(s -> isOn(t, s)).toList();
    }
}
