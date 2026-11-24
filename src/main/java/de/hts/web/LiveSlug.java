/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.text.Normalizer;
import java.time.LocalDate;

/**
 * 1.7.0 (#147): lesbare Adresse eines Turniers auf dem Live-Server, z. B. „Hamburger_Ranglistenturnier_28_11_2026“.
 * HTS (Anzeige, QR-Aushang) und der Live-Server rechnen gleich.
 */
public final class LiveSlug {

    private LiveSlug() {
    }

    public static String of(String name, LocalDate date) {
        String n = name == null || name.isBlank() ? "Turnier" : name.strip();
        n = n.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue")
                .replace("ß", "ss");
        n = Normalizer.normalize(n, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (n.isEmpty()) n = "Turnier";
        if (n.length() > 48) n = n.substring(0, 48).replaceAll("_+$", "");
        if (date != null) n += String.format("_%02d_%02d_%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        return n;
    }
}
