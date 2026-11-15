/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import javafx.animation.PauseTransition;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * 1.1 (#91): Bedienung auf Windows-Tablets. Sobald ein Fenster zum ersten Mal mit dem Finger berührt wird, bekommt es
 * die Stilklasse „touch“ (größere Bedienelemente und Tabellenzeilen, siehe hts.css). Langes Antippen (0,6 s, ohne
 * Bewegung) öffnet das Kontextmenü wie ein Rechtsklick. Wischen blättert (das erzeugt JavaFX selbst als Bildlauf),
 * Ziehen per Finger nutzt die von Windows nachgebildeten Mausereignisse.
 */
final class TouchSupport {

    static final String STYLE = "touch";
    private static final Duration HOLD = Duration.millis(600);
    private static final double SLOP = 12;           // Bildpunkte Bewegung, ab denen es kein langes Antippen mehr ist

    private TouchSupport() {
    }

    static void installEverywhere() {
        for (Window w : Window.getWindows()) install(w);
        Window.getWindows().addListener((ListChangeListener<Window>) ch -> {
            while (ch.next()) for (Window w : ch.getAddedSubList()) install(w);
        });
    }

    private static void install(Window w) {
        if (w.getProperties().containsKey(STYLE)) return;
        w.getProperties().put(STYLE, Boolean.TRUE);
        if (w.getScene() != null) install(w.getScene());
        w.sceneProperty().addListener((o, a, b) -> {
            if (b != null) install(b);
        });
    }

    static void install(Scene scene) {
        if (scene.getProperties().containsKey(STYLE)) return;
        scene.getProperties().put(STYLE, Boolean.TRUE);
        final Point2D[] start = new Point2D[1];
        final EventTarget[] target = new EventTarget[1];
        final PickResult[] pick = new PickResult[1];
        final boolean[] fired = new boolean[1];
        PauseTransition hold = new PauseTransition(HOLD);
        hold.setOnFinished(e -> {
            if (target[0] == null || start[0] == null) return;
            Point2D scr = start[0];
            Point2D local = scene.getRoot().screenToLocal(scr);
            ContextMenuEvent cme = new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                    local == null ? 0 : local.getX(), local == null ? 0 : local.getY(), scr.getX(), scr.getY(), false, pick[0]);
            fired[0] = true;
            Event.fireEvent(target[0], cme);
            fired[0] = false;
            target[0] = null;
        });
        // Windows bildet langes Antippen teils selbst als Rechtsklick nach – dann nicht doppelt öffnen
        scene.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, e -> {
            if (!fired[0]) {
                hold.stop();
                target[0] = null;
            }
        });
        scene.addEventFilter(TouchEvent.TOUCH_PRESSED, e -> {
            markTouch(scene);
            if (e.getTouchCount() != 1) {
                hold.stop();
                target[0] = null;
                return;
            }
            TouchPoint tp = e.getTouchPoint();
            start[0] = new Point2D(tp.getScreenX(), tp.getScreenY());
            target[0] = tp.getTarget();
            pick[0] = tp.getPickResult();
            hold.playFromStart();
        });
        scene.addEventFilter(TouchEvent.TOUCH_MOVED, e -> {
            TouchPoint tp = e.getTouchPoint();
            if (start[0] != null && start[0].distance(tp.getScreenX(), tp.getScreenY()) > SLOP) {
                hold.stop();
                target[0] = null;
            }
        });
        scene.addEventFilter(TouchEvent.TOUCH_RELEASED, e -> {
            hold.stop();
            target[0] = null;
        });
        // Maus oder Stift: wieder normale Größen
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (!e.isSynthesized()) unmarkTouch(scene);
        });
    }

    private static void markTouch(Scene scene) {
        Parent root = scene.getRoot();
        if (root != null && !root.getStyleClass().contains(STYLE)) root.getStyleClass().add(STYLE);
    }

    private static void unmarkTouch(Scene scene) {
        Parent root = scene.getRoot();
        if (root != null) root.getStyleClass().remove(STYLE);
    }

    /** Für Tests: ist die Touch-Darstellung aktiv? */
    static boolean isTouch(Node n) {
        return n.getScene() != null && n.getScene().getRoot().getStyleClass().contains(STYLE);
    }
}
