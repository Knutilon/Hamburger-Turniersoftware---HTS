/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

/** Spielart einer Klasse: Einzel, Doppel oder Mannschaftskampf (2er/3er Turnier, nur im Vorgabeturnier). */
public enum Discipline {
    SINGLES("Einzel"),
    DOUBLES("Doppel"),
    TEAM2("2er Turnier"),
    TEAM3("3er Turnier");

    private final String label;

    Discipline(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Mannschaftskampf (2er/3er Turnier). */
    public boolean isTeam() {
        return this == TEAM2 || this == TEAM3;
    }

    /** Personen je Partei: 1 (Einzel), 2 (Doppel, 2er Team), 3 (3er Team). */
    public int size() {
        return switch (this) {
            case SINGLES -> 1;
            case DOUBLES, TEAM2 -> 2;
            case TEAM3 -> 3;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
