/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 1.5.0 (#139): Ausrichter (Verband bzw. Verein) – Name und Logo, gültig für alle Turniere. Liegt im Ablageordner der
 * Turniere („ausrichter.properties“ und „ausrichter-logo.png“), damit es mit den Turnieren umzieht. Das Logo erscheint
 * auf dem Schiedsrichterzettel oben rechts und auf den Live-Seiten. Ohne eigene Angabe: Logo des Hamburger
 * Tisch-Tennis-Verbands (mitgeliefert).
 */
public final class Organizer {

    /** Woher das Logo kommt. */
    public enum LogoSource { DEFAULT, CUSTOM, NONE }

    public static final String DEFAULT_NAME = "Hamburger Tisch-Tennis-Verband e.V.";
    static final String DEFAULT_LOGO = "/de/hts/report/hattv-logo.png";
    private static final String FILE = "ausrichter.properties", LOGO_FILE = "ausrichter-logo.png";

    private static volatile Organizer current = new Organizer(null);

    private final Path dir;
    private String name = DEFAULT_NAME;
    private LogoSource logo = LogoSource.DEFAULT;
    private byte[] customLogo;

    private Organizer(Path dir) {
        this.dir = dir;
    }

    /** Ausrichter des Programms (beim Start aus dem Ablageordner geladen). */
    public static Organizer get() {
        return current;
    }

    /** Lädt den Ausrichter aus dem Ablageordner der Turniere und macht ihn zum aktuellen. */
    public static Organizer load(Path storeDir) {
        Organizer o = new Organizer(storeDir);
        try {
            Path f = storeDir == null ? null : storeDir.resolve(FILE);
            if (f != null && Files.isRegularFile(f)) {
                Properties p = new Properties();
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                o.name = p.getProperty("name", DEFAULT_NAME);
                try {
                    o.logo = LogoSource.valueOf(p.getProperty("logo", "DEFAULT"));
                } catch (IllegalArgumentException e) {
                    o.logo = LogoSource.DEFAULT;
                }
                Path lf = storeDir.resolve(LOGO_FILE);
                if (o.logo == LogoSource.CUSTOM) {
                    if (Files.isRegularFile(lf)) o.customLogo = Files.readAllBytes(lf);
                    else o.logo = LogoSource.DEFAULT;
                }
            }
        } catch (IOException e) {
            // unlesbar: Vorgaben
        }
        current = o;
        return o;
    }

    public String getName() { return name; }

    public LogoSource getLogoSource() { return logo; }

    public void setName(String n) {
        name = n == null ? "" : n.strip();
        save();
    }

    /** Eigenes Logo (PNG oder JPEG); wird als PNG im Ablageordner gespeichert. */
    public void setCustomLogo(byte[] png) {
        customLogo = png;
        logo = LogoSource.CUSTOM;
        save();
    }

    public void useDefaultLogo() {
        logo = LogoSource.DEFAULT;
        customLogo = null;
        save();
    }

    public void useNoLogo() {
        logo = LogoSource.NONE;
        customLogo = null;
        save();
    }

    /** Logo als PNG-Bytes oder {@code null} (kein Logo). */
    public byte[] logoPng() {
        if (logo == LogoSource.NONE) return null;
        if (logo == LogoSource.CUSTOM && customLogo != null) return customLogo;
        try (InputStream in = Organizer.class.getResourceAsStream(DEFAULT_LOGO)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private void save() {
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            Properties p = new Properties();
            p.setProperty("name", name);
            p.setProperty("logo", logo.name());
            try (Writer w = Files.newBufferedWriter(dir.resolve(FILE), StandardCharsets.UTF_8)) {
                p.store(w, "HTS – Ausrichter (Name und Logo)");
            }
            Path lf = dir.resolve(LOGO_FILE);
            if (logo == LogoSource.CUSTOM && customLogo != null) Files.write(lf, customLogo);
            else Files.deleteIfExists(lf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
