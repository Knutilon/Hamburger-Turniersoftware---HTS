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
import java.util.prefs.Preferences;

/**
 * 1.5.0 (#135): Adresse und geheimer Schlüssel des Webservers – Programmeinstellung (nur auf diesem Rechner, nicht in
 * der Turnierdatei, damit der Schlüssel nicht mit Turnierdateien weitergegeben wird).
 * <p>
 * 1.7.0 (#142): verschlüsselt abgelegt ({@link SecretStore}, AES-256-GCM); Klartext-Einträge aus 1.5/1.6 werden beim
 * ersten Zugriff übernommen und gelöscht. Lässt sich nichts entschlüsseln (anderer Rechner/Benutzer), gilt der Webserver
 * als nicht eingerichtet ({@link #state()} = {@code UNREADABLE}) – das Veröffentlichen pausiert.
 */
public final class WebSettings {

    public enum State { EMPTY, OK, UNREADABLE }

    private static Preferences prefs = Preferences.userNodeForPackage(WebSettings.class);
    private static SecretStore store = new SecretStore(SecretStore.platformProvider(prefs));

    private static volatile boolean loaded;
    private static volatile String server = "";
    private static volatile String key = "";
    private static volatile State state = State.EMPTY;

    private WebSettings() {
    }

    /** Für Tests: andere Ablage und Schlüsselquelle. */
    static synchronized void useForTest(Preferences p, SecretStore.KeyProvider provider) {
        prefs = p;
        store = new SecretStore(provider);
        loaded = false;
    }

    /** Beim Programmstart im Hintergrund aufrufen (Windows: DPAPI braucht etwa eine Sekunde). */
    public static void preload() {
        load();
    }

    private static synchronized void load() {
        if (loaded) return;
        String oldServer = prefs.get("server", null), oldKey = prefs.get("key", null);
        String encServer = prefs.get("server-enc", null), encKey = prefs.get("key-enc", null);
        try {
            if (encServer == null && encKey == null && (oldServer != null || oldKey != null)) {
                // Übernahme aus 1.5/1.6 (Klartext) – verschlüsselt speichern, Klartext löschen
                server = oldServer == null ? "" : oldServer;
                key = oldKey == null ? "" : oldKey;
                write();
            } else {
                server = encServer == null ? "" : store.decrypt(encServer);
                key = encKey == null ? "" : store.decrypt(encKey);
            }
            state = server.isBlank() && key.isBlank() ? State.EMPTY : State.OK;
        } catch (IOException | RuntimeException e) {
            server = "";
            key = "";
            state = State.UNREADABLE;
            de.hts.Log.warn("Webserver-Einstellungen lassen sich nicht entschlüsseln: " + e.getMessage());
        }
        loaded = true;
    }

    private static void write() throws IOException {
        prefs.put("server-enc", store.encrypt(server));
        prefs.put("key-enc", store.encrypt(key));
        prefs.remove("server");
        prefs.remove("key");
        try {
            prefs.flush();
        } catch (java.util.prefs.BackingStoreException e) {
            throw new IOException(e);
        }
    }

    public static String server() {
        load();
        return server;
    }

    public static String key() {
        load();
        return key;
    }

    public static State state() {
        load();
        return state;
    }

    /** Speichert verschlüsselt; ersetzt auch unlesbare alte Einträge. */
    public static synchronized void set(String newServer, String newKey) {
        load();
        server = newServer == null ? "" : newServer.strip();
        key = newKey == null ? "" : newKey.strip();
        try {
            write();
            state = server.isBlank() && key.isBlank() ? State.EMPTY : State.OK;
        } catch (IOException e) {
            de.hts.Log.error("Webserver-Einstellungen konnten nicht gespeichert werden", e);
            throw new IllegalStateException("Webserver-Einstellungen konnten nicht verschlüsselt gespeichert werden: " + e.getMessage(), e);
        }
    }

    /** Sind Adresse und Schlüssel eingetragen (und lesbar)? */
    public static boolean configured() {
        return !server().isBlank() && !key().isBlank();
    }

    /** Für die Anzeige ohne Admin-Passwort: „http•••••.de“. */
    public static String mask(String s) {
        if (s == null || s.isBlank()) return "–";
        String t = s.strip();
        if (t.length() <= 8) return "••••••";
        return t.substring(0, 4) + "•••••" + t.substring(t.length() - 3);
    }
}
