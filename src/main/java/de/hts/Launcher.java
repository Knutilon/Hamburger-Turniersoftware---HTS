/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts;

/**
 * Startpunkt für die ausführbare JAR-Datei.
 * (Eine Klasse, die nicht von javafx.application.Application erbt, ist nötig,
 * damit JavaFX aus einer "fat jar" ohne Modulpfad startet.)
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        HtsApp.main(args);
    }
}
