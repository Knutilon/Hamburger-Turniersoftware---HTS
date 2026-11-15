/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.player.Player;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

/**
 * 1.1 (#89): Startgebühr erlassen – zeigt die Gebühren je Einzelklasse und die Summe; optionaler Grund (erscheint in
 * der Kassenübersicht). Ergebnis: der Grund ({@code ""} = ohne Grund) oder leer bei „Abbrechen“.
 */
final class FeeWaiveDialog extends Dialog<String> {

    FeeWaiveDialog(Window owner, Tournament t, List<Player> players) {
        initOwner(owner);
        setTitle("Gebühr erlassen");
        setHeaderText("Startgebühr erlassen für " + (players.size() == 1 ? "1 Person" : players.size() + " Personen"));

        GridPane rows = new GridPane();
        rows.setHgap(16);
        rows.setVgap(4);
        rows.getStyleClass().add("fee-waive-list");
        int r = 0, total = 0;
        for (Player p : players) {
            Label name = new Label(p.getDisplayName() + (p.getClubName() != null ? " · " + p.getClubName() : ""));
            name.getStyleClass().add("class-name");
            name.setWrapText(true);
            name.setMinHeight(Region.USE_PREF_SIZE);
            rows.add(name, 0, r++, 2, 1);
            for (PlayingClass c : t.feeClassesOf(p)) {
                rows.add(new Label(c.getName()), 0, r);
                Label fee = new Label(Tournament.euro(c.getFeeCents()));
                fee.getStyleClass().add("col-right");
                rows.add(fee, 1, r++);
            }
            if (t.isLateEntry(p) && t.getLateFeeCents() > 0) {
                rows.add(new Label("Nachmeldung"), 0, r);
                rows.add(new Label(Tournament.euro(t.getLateFeeCents())), 1, r++);
            }
            total += t.feeOf(p);
        }
        Label sumLabel = new Label("Erlassen werden");
        sumLabel.getStyleClass().add("class-name");
        Label sum = new Label(Tournament.euro(total));
        sum.getStyleClass().add("class-name");
        rows.add(sumLabel, 0, r);
        rows.add(sum, 1, r);

        Label reasonLabel = new Label("Grund (optional, erscheint in der Kassenübersicht)");
        reasonLabel.getStyleClass().add("class-name");
        TextField reason = new TextField();
        reason.setPromptText("z. B. Helfer beim Aufbau");
        Label hint = new Label("Gilt wie bezahlt: Die Person wird automatisch angemeldet und steht in „Verfügbar“. "
                + "Rücknahme über „Zahlung rückgängig“.");
        hint.setWrapText(true);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        hint.getStyleClass().add("fee-pending-banner");

        VBox box = new VBox(12, rows, reasonLabel, reason, hint);
        box.setPadding(new Insets(10));
        box.setPrefWidth(520);
        getDialogPane().setContent(box);
        ButtonType ok = new ButtonType("Gebühr erlassen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok ? (reason.getText() == null ? "" : reason.getText().trim()) : null);
        setOnShown(e -> reason.requestFocus());
    }
}
