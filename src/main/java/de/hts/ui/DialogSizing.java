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
                    resize(pane);
                }
            });
        }
        d.addEventHandler(DialogEvent.DIALOG_SHOWN, e -> resize(pane));
    }

    /** Umbrechende Texte bekommen die Höhe, die sie brauchen (statt „…“). */
    private static void growLabels(Node n) {
        if (n instanceof Labeled l && l.isWrapText()) l.setMinHeight(Region.USE_PREF_SIZE);
        if (n instanceof Label l && l.isWrapText()) l.setMinHeight(Region.USE_PREF_SIZE);
        // 0.36: einzeilige Texte nie mit „…“ kürzen
        if (n instanceof Label l && !l.isWrapText() && l.getMinWidth() == Region.USE_COMPUTED_SIZE) l.setMinWidth(Region.USE_PREF_SIZE);
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) growLabels(c);
    }

    private static void resize(DialogPane pane) {
        Platform.runLater(() -> {
            if (pane.getScene() == null) return;
            Window w = pane.getScene().getWindow();
            if (w == null || !w.isShowing()) return;
            w.sizeToScene();
            // 0.38: nie seitlich verschoben öffnen (Inhalt war links abgeschnitten)
            if (pane.getContent() instanceof javafx.scene.control.ScrollPane sp) sp.setHvalue(sp.getHmin());
            Rectangle2D screen = Screen.getScreensForRectangle(w.getX(), w.getY(), Math.max(1, w.getWidth()), Math.max(1, w.getHeight()))
                    .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
            if (w.getHeight() > screen.getHeight()) w.setHeight(screen.getHeight());
            if (w.getWidth() > screen.getWidth()) w.setWidth(screen.getWidth());
            if (w.getX() + w.getWidth() > screen.getMaxX()) w.setX(Math.max(screen.getMinX(), screen.getMaxX() - w.getWidth()));
            if (w.getY() + w.getHeight() > screen.getMaxY()) w.setY(Math.max(screen.getMinY(), screen.getMaxY() - w.getHeight()));
        });
    }
}
