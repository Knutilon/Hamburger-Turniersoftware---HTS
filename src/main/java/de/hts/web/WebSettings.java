/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.util.prefs.Preferences;

/**
 * 1.5.0 (#135): Adresse und geheimer Schlüssel des Webservers – Programmeinstellung (nur auf diesem Rechner, nicht in
 * der Turnierdatei, damit der Schlüssel nicht mit Turnierdateien weitergegeben wird).
 */
public final class WebSettings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(WebSettings.class);

    private WebSettings() {
    }

    public static String server() {
        return PREFS.get("server", "");
    }

    public static String key() {
        return PREFS.get("key", "");
    }

    public static void set(String server, String key) {
        PREFS.put("server", server == null ? "" : server.strip());
        PREFS.put("key", key == null ? "" : key.strip());
    }

    /** Sind Adresse und Schlüssel eingetragen? */
    public static boolean configured() {
        return !server().isBlank() && !key().isBlank();
    }
}
