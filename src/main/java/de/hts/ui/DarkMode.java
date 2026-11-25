/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * 1.8.0 (#152): Dunkelmodus für die ganze Oberfläche. Die Stildatei {@code hts-dunkel.css} wird jedem Fenster (auch
 * Dialogen) zusätzlich zu {@code hts.css} gegeben, solange der Dunkelmodus eingeschaltet ist. Druck und PDFs bleiben hell.
 */
public final class DarkMode {

    static final String CSS = DarkMode.class.getResource("/de/hts/ui/hts-dunkel.css").toExternalForm();
    private static boolean installed;

    private DarkMode() {
    }

    /** Einmal beim Start: alle (auch später geöffneten) Fenster beobachten. */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        Window.getWindows().addListener((ListChangeListener<Window>) c -> {
            while (c.next()) for (Window w : c.getAddedSubList()) watch(w);
        });
        for (Window w : Window.getWindows()) watch(w);
    }

    private static void watch(Window w) {
        apply(w.getScene());
        w.sceneProperty().addListener((o, a, b) -> apply(b));
    }

    static void apply(Scene s) {
        if (s == null) return;
        boolean on = de.hts.persistence.ProgramFolder.isDarkMode();
        var list = s.getStylesheets();
        if (on && !list.contains(CSS)) list.add(CSS);
        else if (!on) list.remove(CSS);
    }

    public static boolean isOn() {
        return de.hts.persistence.ProgramFolder.isDarkMode();
    }

    /** Ein- bzw. ausschalten: gilt sofort für alle offenen Fenster und wird gemerkt. */
    public static void set(boolean on) {
        de.hts.persistence.ProgramFolder.setDarkMode(on);
        for (Window w : Window.getWindows()) apply(w.getScene());
    }
}
