/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * 1.4.0 (#113), 1.4.1: Frage „Urkundendruck starten?“, sobald eine Klasse beendet ist. Links „Abbrechen“, rechts „Zur Druckansicht“.
 */
final class CertificatesDialog {

    private CertificatesDialog() {
    }

    /** {@code true} = „Zur Druckansicht“ (Plätze 1 bis 3 vorausgewählt), {@code false} = „Abbrechen“. */
    static boolean ask(Window owner, String className, boolean finished) {
        Dialog<ButtonType> d = new Dialog<>();
        if (owner != null) d.initOwner(owner);
        d.setTitle("Urkundendruck starten?");
        d.setHeaderText("Urkundendruck starten?");
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType go = new ButtonType("Zur Druckansicht", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(cancel, go);
        Label text = new Label((finished ? "Die Klasse „" + className + "“ ist beendet."
                : "In der Klasse „" + className + "“ ist eine Phase abgeschlossen – die Plätze der Ausgeschiedenen stehen fest.")
                + " Mit „Zur Druckansicht“ öffnet HTS den Reiter „Urkunden drucken“; die Plätze 1 bis 3 dieser Klasse "
                + "(soweit sie feststehen) sind schon ausgewählt.\n\nMit „Abbrechen“ erscheint "
                + "der Reiter ebenfalls, aber es ist noch keine Urkunde zum Druck ausgewählt. Der Reiter bleibt danach erhalten.");
        text.setWrapText(true);
        text.setPrefWidth(440);
        VBox box = new VBox(text);
        box.setPadding(new Insets(6, 4, 4, 4));
        d.getDialogPane().setContent(box);
        if (owner != null && owner.getScene() != null) d.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        d.getDialogPane().lookupButton(go).getStyleClass().add("navy-pill");
        d.setOnShown(e -> {                     // links Abbrechen, rechts Zur Druckansicht – auf allen Systemen
            ButtonBar bar = (ButtonBar) d.getDialogPane().lookup(".button-bar");
            if (bar != null) bar.setButtonOrder("CO");
        });
        DialogSizing.fit(d);
        return d.showAndWait().filter(b -> b == go).isPresent();
    }
}
