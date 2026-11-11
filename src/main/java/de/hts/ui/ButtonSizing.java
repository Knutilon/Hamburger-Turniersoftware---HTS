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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Programmweit: Knöpfe werden nie gekürzt („Einstellungen änder…“), sondern immer ausgeschrieben – ihre
 * Mindestbreite ist ihre bevorzugte Breite. Gilt für alle Fenster (Hauptfenster, Dialoge, PDF-Ansicht).
 */
final class ButtonSizing {

    private ButtonSizing() {
    }

    /** Für alle jetzigen und künftigen Fenster einrichten. */
    static void installEverywhere() {
        for (Window w : Window.getWindows()) install(w);
        Window.getWindows().addListener((ListChangeListener<Window>) ch -> {
            while (ch.next()) for (Window w : ch.getAddedSubList()) install(w);
        });
    }

    private static void install(Window w) {
        if (w.getScene() != null) install(w.getScene());
        w.sceneProperty().addListener((o, a, b) -> {
            if (b != null) install(b);
        });
    }

    static void install(Scene scene) {
        if (scene.getProperties().containsKey(ButtonSizing.class)) return;
        scene.getProperties().put(ButtonSizing.class, Boolean.TRUE);
        scene.addPostLayoutPulseListener(() -> {
            if (scene.getRoot() != null) fix(scene.getRoot());
        });
    }

    private static void fix(Node n) {
        // nur Knöpfe ohne eigene Mindestbreite (z. B. aus dem Stylesheet) – sonst „zucken“ sie beim Überfahren
        if (n instanceof ButtonBase b) {
            if (b.getMinWidth() == Region.USE_COMPUTED_SIZE) b.setMinWidth(Region.USE_PREF_SIZE);
        } else if (n instanceof ComboBoxBase<?> c) {
            if (c.getMinWidth() == Region.USE_COMPUTED_SIZE) c.setMinWidth(Region.USE_PREF_SIZE);
        }
        if (n instanceof javafx.scene.Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) fix(c);
        }
    }
}
