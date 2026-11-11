/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.player;

/**
 * Geschlecht eines Spielers. click-TT kodiert es als Zahl: 1 = männlich, 0 = weiblich (so in echten Turnierexporten,
 * 0.42 / Bug #71); 2 = weiblich wird weiterhin gelesen (ältere Beispieldateien).
 */
public enum Sex {
    MALE("m"),
    FEMALE("w"),
    UNKNOWN("");

    private final String shortLabel;

    Sex(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public static Sex fromClickTt(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return switch (code) {
            case 1 -> MALE;
            case 0, 2 -> FEMALE;
            default -> UNKNOWN;
        };
    }

    @Override
    public String toString() {
        return shortLabel;
    }
}
