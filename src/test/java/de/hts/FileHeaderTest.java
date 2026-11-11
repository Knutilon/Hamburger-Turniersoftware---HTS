/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** HTS 0.42.0 (#72): Jede .java-Datei beginnt mit dem Copyright- und Lizenz-Kopf. */
class FileHeaderTest {

    static final String HEADER = """
            /*
             * HTS – Hamburger Turniersoftware
             * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
             *
             * SPDX-License-Identifier: AGPL-3.0-or-later
             * Dieses Programm ist freie Software unter der GNU Affero General
             * Public License v3.0; Einzelheiten in der Datei LICENSE.
             */
            """;

    @Test
    void jedeJavaDateiHatDenKopf() throws Exception {
        Path src = Path.of("src");
        assertTrue(Files.isDirectory(src), "Test läuft im Projektordner");
        List<Path> missing;
        try (Stream<Path> s = Files.walk(src)) {
            missing = s.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return !Files.readString(p, StandardCharsets.UTF_8).replace("\r\n", "\n").startsWith(HEADER);
                        } catch (java.io.IOException e) {
                            return true;
                        }
                    }).toList();
        }
        assertTrue(missing.isEmpty(), "Ohne Datei-Kopf: " + missing);
    }
}
