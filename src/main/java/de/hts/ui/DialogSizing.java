/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Dialoge passen ihre Größe dem Inhalt an: Mehrzeilige Hinweise werden nicht abgeschnitten, und ändert sich der Inhalt
 * (z. B. weil Optionen ein- oder ausgeblendet werden), wächst bzw. schrumpft das Fenster mit – die Knöpfe bleiben sichtbar.
 * 1.4.0 (#110): Hat der Nutzer die Größe selbst mit der Maus geändert, wird sie nicht mehr zurückgesetzt; das Fenster
 * wächst dann nur noch mit, wenn der Inhalt höher wird.
 */
final class DialogSizing {

    private DialogSizing() {
    }

    static void fit(Dialog<?> d) {
        d.setResizable(true);
        DialogPane pane = d.getDialogPane();
        // 0.36: Mindestgröße klein halten – der Inhalt scrollt (siehe unten), wenn der Bildschirm zu klein ist
        pane.setMinHeight(220);
        pane.setMinWidth(320);
        final boolean[] state = {false, false};   // [0] = Größe gerade vom Programm gesetzt, [1] = vom Nutzer mit der Maus geändert
        Node content = pane.getContent();
        if (content != null && !(content instanceof javafx.scene.control.ScrollPane)) {
            // 0.36: Passt der Inhalt nicht auf den Bildschirm (kleine Auflösung), wird gescrollt statt abgeschnitten
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(content);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.getStyleClass().add("dialog-scroll");
            pane.setContent(sp);
        }
        if (content != null) {
            growLabels(content);
            content.layoutBoundsProperty().addListener((o, a, b) -> {
                if (a == null || Math.abs(a.getHeight() - b.getHeight()) > 0.5 || Math.abs(a.getWidth() - b.getWidth()) > 0.5) {
                    resize(pane, state);
                }
            });
        }
        d.addEventHandler(DialogEvent.DIALOG_SHOWN, e -> resize(pane, state));
    }

    /** Umbrechende Texte bekommen die Höhe, die sie brauchen (statt „…“). */
    private static void growLabels(Node n) {
        if (n instanceof Labeled l && l.isWrapText()) l.setMinHeight(Region.USE_PREF_SIZE);
        if (n instanceof Label l && l.isWrapText()) l.setMinHeight(Region.USE_PREF_SIZE);
        // 0.36: einzeilige Texte nie mit „…“ kürzen
        if (n instanceof Label l && !l.isWrapText() && l.getMinWidth() == Region.USE_COMPUTED_SIZE) l.setMinWidth(Region.USE_PREF_SIZE);
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) growLabels(c);
    }

    private static void resize(DialogPane pane, boolean[] state) {
        Platform.runLater(() -> {
            if (pane.getScene() == null) return;
            Window w = pane.getScene().getWindow();
            if (w == null || !w.isShowing()) return;
            if (state[0]) return;                    // Größenänderung vom Programm läuft noch
            if (!(w.getProperties().containsKey("hts.sizeWatch"))) {
                w.getProperties().put("hts.sizeWatch", Boolean.TRUE);
                javafx.beans.value.ChangeListener<Number> watch = (o, a, b) -> { if (!state[0]) state[1] = true; };
                w.widthProperty().addListener(watch);
                w.heightProperty().addListener(watch);
            }
            state[0] = true;
            try {
                Rectangle2D screen = Screen.getScreensForRectangle(w.getX(), w.getY(), Math.max(1, w.getWidth()), Math.max(1, w.getHeight()))
                        .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
                if (state[1]) {
                    // Nutzer hat die Größe selbst gewählt: nicht zurücksetzen, nur bei zu wenig Höhe mitwachsen
                    double need = pane.prefHeight(pane.getWidth()) + (w.getHeight() - pane.getHeight());
                    if (need > w.getHeight()) w.setHeight(Math.min(need, screen.getHeight()));
                    return;
                }
                w.sizeToScene();
                // 0.38: nie seitlich verschoben öffnen (Inhalt war links abgeschnitten)
                if (pane.getContent() instanceof javafx.scene.control.ScrollPane sp) sp.setHvalue(sp.getHmin());
                if (w.getHeight() > screen.getHeight()) w.setHeight(screen.getHeight());
                if (w.getWidth() > screen.getWidth()) w.setWidth(screen.getWidth());
                if (w.getX() + w.getWidth() > screen.getMaxX()) w.setX(Math.max(screen.getMinX(), screen.getMaxX() - w.getWidth()));
                if (w.getY() + w.getHeight() > screen.getMaxY()) w.setY(Math.max(screen.getMinY(), screen.getMaxY() - w.getHeight()));
            } finally {
                Platform.runLater(() -> state[0] = false);   // verspätete Größenmeldungen des Fensters zählen noch als „vom Programm“
            }
        });
    }
}
