/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.8.0 (#154): Löschaufträge für den Webserver, die nicht sofort ausgeführt werden konnten (Server nicht erreichbar,
 * Webserver nicht entschlüsselbar). Sie stehen in {@code webserver-loeschen.txt} im Programmordner (nur Turnier-Kennungen,
 * eine je Zeile) und werden beim nächsten Start bzw. beim nächsten Veröffentlichen nachgeholt.
 */
public final class PendingDeletes {

    static final String FILE = "webserver-loeschen.txt";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Object LOCK = new Object();

    private PendingDeletes() {
    }

    private static Path file() {
        return de.hts.persistence.ProgramFolder.dir().resolve(FILE);
    }

    /** Offene Aufträge. */
    public static Set<String> list() {
        synchronized (LOCK) {
            Set<String> s = new LinkedHashSet<>();
            try {
                if (Files.isRegularFile(file())) {
                    for (String l : Files.readAllLines(file(), StandardCharsets.UTF_8)) if (ID.matcher(l.strip()).matches()) s.add(l.strip());
                }
            } catch (IOException ignored) {
                // unlesbar: keine Aufträge
            }
            return s;
        }
    }

    /** Auftrag merken (nur gültige Kennungen). */
    public static void add(String id) {
        if (id == null || !ID.matcher(id).matches()) return;
        synchronized (LOCK) {
            Set<String> s = list();
            if (s.add(id)) write(s);
        }
        de.hts.Log.info("Webserver: Löschauftrag gemerkt (Turnier " + id + ")");
    }

    static void remove(String id) {
        synchronized (LOCK) {
            Set<String> s = list();
            if (s.remove(id)) write(s);
        }
    }

    private static void write(Set<String> s) {
        try {
            if (s.isEmpty()) Files.deleteIfExists(file());
            else Files.write(file(), String.join(System.lineSeparator(), s).concat(System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            de.hts.Log.warn("Löschaufträge nicht gespeichert: " + e.getMessage());
        }
    }

    /**
     * Turnier sofort vom Webserver löschen.
     *
     * @return {@code null} bei Erfolg, sonst der Grund (der Auftrag ist dann gemerkt)
     */
    public static String deleteNow(String id) {
        if (!WebSettings.configured()) {
            add(id);
            return "Webserver nicht eingerichtet bzw. Einstellungen nicht lesbar";
        }
        Publisher p = new Publisher(WebSettings.server(), WebSettings.key(), id, r -> { }, () -> new byte[0], () -> null,
                Long.MAX_VALUE, Long.MAX_VALUE);
        try {
            p.deleteOnServer();
            remove(id);
            de.hts.Log.info("Webserver: Turnier " + id + " gelöscht");
            return null;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            add(id);
            return e.getMessage() == null ? "nicht erreichbar" : e.getMessage();
        } finally {
            p.close();
        }
    }

    /** Offene Aufträge im Hintergrund nachholen (beim Start und beim Einschalten des Veröffentlichens). */
    public static void retryLater() {
        if (list().isEmpty()) return;
        Thread th = new Thread(() -> {
            try {
                Thread.sleep(3000);                                 // Einstellungen erst laden lassen
            } catch (InterruptedException e) {
                return;
            }
            if (!WebSettings.configured()) return;
            for (String id : list()) deleteNow(id);
        }, "HTS-Loeschauftraege");
        th.setDaemon(true);
        th.start();
    }
}
