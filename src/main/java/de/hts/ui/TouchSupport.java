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

    // --- 1.5.0 (#124): Tablet-Modus dauerhaft einschaltbar ------------------------------------------

    private static final java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userNodeForPackage(TouchSupport.class);
    private static final String TABLET_KEY = "tabletMode";

    /** Tablet-Modus: große Bedienelemente immer, nicht nur nach dem ersten Fingertipp (Einstellung je Rechner). */
    static boolean isTabletMode() {
        return PREFS.getBoolean(TABLET_KEY, false);
    }

    static void setTabletMode(boolean on) {
        PREFS.putBoolean(TABLET_KEY, on);
        for (Window w : Window.getWindows()) {
            if (w.getScene() == null) continue;
            if (on) markTouch(w.getScene());
            else unmarkTouch(w.getScene());
        }
    }

    // --- 1.5.0 (#119): Bildschirmtastatur ----------------------------------------------------------

    private static long lastKeyboard;

    /** Öffnet unter Windows die Bildschirmtastatur (TabTip), wenn ein Eingabefeld mit dem Finger angetippt wird. */
    static void openKeyboard() {
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) return;
        long now = System.currentTimeMillis();
        if (now - lastKeyboard < 1500) return;
        lastKeyboard = now;
        Thread th = new Thread(() -> {
            String common = System.getenv("CommonProgramFiles");
            java.io.File tabtip = new java.io.File(common == null ? "C:\\Program Files\\Common Files" : common,
                    "microsoft shared\\ink\\TabTip.exe");
            try {
                if (tabtip.isFile()) new ProcessBuilder("cmd", "/c", "start", "", tabtip.getAbsolutePath()).start();
                else new ProcessBuilder("cmd", "/c", "start", "", "osk.exe").start();
            } catch (java.io.IOException ignored) {
                // keine Bildschirmtastatur verfügbar – dann eben ohne
            }
        }, "HTS-Bildschirmtastatur");
        th.setDaemon(true);
        th.start();
    }

    /** Liegt das Ziel in einem Eingabefeld (Text, Zahlenfeld)? */
    private static boolean inTextInput(EventTarget t) {
        Node n = t instanceof Node node ? node : null;
        for (int i = 0; n != null && i < 6; i++, n = n.getParent()) {
            if (n instanceof javafx.scene.control.TextInputControl tic) return tic.isEditable() && !tic.isDisabled();
        }
        return false;
    }

    // --- 1.5.0 (#118): Hinweise gesperrter Knöpfe per Antippen/Klick --------------------------------

    private static final String TIP_KEY = "javafx.scene.control.Tooltip";

    /** Zeigt den Hinweis eines gesperrten Knopfs (über einen Rahmen mit Tooltip) beim Antippen für einige Sekunden. */
    private static void showDisabledHint(MouseEvent e) {
        Node n = e.getTarget() instanceof Node node ? node : null;
        for (int i = 0; n != null && i < 5; i++, n = n.getParent()) {
            Object tip = n.getProperties().get(TIP_KEY);
            if (tip instanceof javafx.scene.control.Tooltip t && n instanceof Parent p && hasDisabledChild(p)) {
                t.show(n, e.getScreenX() + 8, e.getScreenY() + 12);
                PauseTransition hide = new PauseTransition(Duration.seconds(4));
                hide.setOnFinished(x -> t.hide());
                hide.play();
                return;
            }
        }
    }

    private static boolean hasDisabledChild(Parent p) {
        for (Node c : p.getChildrenUnmodifiable()) if (c.isDisabled()) return true;
        return false;
    }

    static void install(Scene scene) {
        if (scene.getProperties().containsKey(STYLE)) return;
        scene.getProperties().put(STYLE, Boolean.TRUE);
        if (isTabletMode()) markTouch(scene);
        scene.rootProperty().addListener((o, a, b) -> {
            if (isTabletMode()) markTouch(scene);
        });
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, TouchSupport::showDisabledHint);
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
            if (inTextInput(tp.getTarget())) openKeyboard();
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
        if (isTabletMode()) return;                      // 1.5.0: im Tablet-Modus bleibt es groß
        Parent root = scene.getRoot();
        if (root != null) root.getStyleClass().remove(STYLE);
    }

    /**
     * 1.5.0 (#117): „Öffnen“-Klick – Doppelklick mit der Maus, am Tablet genügt ein Antippen (Doppeltippen ist
     * unzuverlässig).
     */
    static boolean isActivate(MouseEvent e, Node n) {
        if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return false;
        if (e.getClickCount() == 2) return true;
        return e.getClickCount() == 1 && e.isSynthesized() && isTouch(n);
    }

    /** Für Tests: ist die Touch-Darstellung aktiv? */
    static boolean isTouch(Node n) {
        return n.getScene() != null && n.getScene().getRoot().getStyleClass().contains(STYLE);
    }
}
