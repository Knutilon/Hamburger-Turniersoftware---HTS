/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.importer.clicktt;

/** Fehler beim Einlesen einer click-TT-XML-Datei, mit einer für Anwender verständlichen Meldung. */
public class ClickTtImportException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClickTtImportException(String message) {
        super(message);
    }

    public ClickTtImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
