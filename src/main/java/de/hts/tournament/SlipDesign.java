/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

/**
 * 1.4.0 (#108): Aussehen der Spielzettel (A6) eines Turniers. Gilt für Einzel und Doppel; der Zettel für
 * Mannschaftsbegegnungen (2er/3er-Team, A4) bleibt in beiden Fällen unverändert.
 */
public enum SlipDesign {
    /** Bisheriges schlichtes Design (Standard), mit Ergebnis-Boxen und „Gewinner/in“. */
    PLAYER("Spielerzettel", "Standard: schlichter Zettel mit Satztabelle, Ergebnis und Gewinner/in"),
    /** Design nach dem DTTB-Schiedsrichterzettel (Karten, Auszeit, Aufschlag je Satz). */
    REFEREE("Schiedsrichterzettel", "Wie der DTTB-Schiedsrichterzettel: mit Karten, Auszeit und Aufschlag je Satz");

    private final String label;
    private final String description;

    SlipDesign(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label;
    }

    /** Aus dem gespeicherten Namen; unbekannt oder leer = Spielerzettel. */
    public static SlipDesign fromName(String name) {
        if (name == null) return PLAYER;
        try {
            return valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return PLAYER;
        }
    }
}
