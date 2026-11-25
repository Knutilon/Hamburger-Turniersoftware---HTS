/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import java.nio.file.Path;

/** Testzugang zur Ablage mit neuer Ordnerstruktur (1.8.0, #152). */
public final class TournamentStoreAccess {

    private TournamentStoreAccess() {
    }

    public static TournamentStore withFlatDir(Path oldRoot, Path flat) {
        return TournamentStore.withFlatDir(oldRoot, flat, null);
    }
}
