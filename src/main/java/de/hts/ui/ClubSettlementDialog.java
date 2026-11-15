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
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.0 (#82): Sammelabrechnung der Startgebühren eines Vereins (Betreuer zahlt für alle): Verein wählen, offene
 * Gebühren seiner Mitglieder abhaken, Zahlungsart wählen; auf Wunsch gleich eine Quittung als PDF.
 */
final class ClubSettlementDialog extends Dialog<ClubSettlementDialog.Result> {

    /** @param toPay die zu bezahlenden Personen (offene Gebühren), {@code receipt}: danach Quittung zeigen */
    record Result(String club, List<Player> toPay, Player.Payment how, boolean receipt) {
    }

    private final Tournament tournament;
    private final ComboBox<String> club = new ComboBox<>();
    private final GridPane rows = new GridPane();
    private final Map<Player, CheckBox> checks = new LinkedHashMap<>();
    private final Label total = new Label();

    ClubSettlementDialog(Window owner, Tournament tournament, List<String> clubs) {
        this.tournament = tournament;
        initOwner(owner);
        setTitle("Verein abrechnen");
        setHeaderText("Startgebühren eines Vereins gesammelt abrechnen");

        club.getItems().setAll(clubs);
        club.getSelectionModel().select(0);
        club.getStyleClass().add("filter-combo");
        club.valueProperty().addListener((o, a, b) -> fill());
        Label clubLabel = new Label("Verein:");
        clubLabel.getStyleClass().add("class-name");
        HBox clubRow = new HBox(10, clubLabel, club);
        clubRow.setAlignment(Pos.CENTER_LEFT);

        rows.setHgap(16);
        rows.setVgap(6);

        ToggleGroup how = new ToggleGroup();
        RadioButton cash = new RadioButton("bar");
        RadioButton transfer = new RadioButton("Überweisung");
        cash.setToggleGroup(how);
        transfer.setToggleGroup(how);
        cash.setSelected(true);
        Label howLabel = new Label("Zahlungsart:");
        howLabel.getStyleClass().add("class-name");
        HBox howRow = new HBox(14, howLabel, cash, transfer);
        howRow.setAlignment(Pos.CENTER_LEFT);
        CheckBox receipt = new CheckBox("Quittung als PDF anzeigen");
        receipt.setSelected(true);
        total.getStyleClass().add("class-name");

        VBox box = new VBox(12, clubRow, rows, total, howRow, receipt);
        box.setPadding(new Insets(10));
        getDialogPane().setContent(box);
        ButtonType ok = new ButtonType("Als bezahlt markieren", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);
        fill();
        DialogSizing.fit(this);
        setResultConverter(bt -> {
            if (bt != ok) return null;
            List<Player> pay = new ArrayList<>();
            checks.forEach((p, cb) -> {
                if (cb.isSelected() && !cb.isDisabled()) pay.add(p);
            });
            return new Result(club.getValue(), pay, transfer.isSelected() ? Player.Payment.TRANSFER : Player.Payment.CASH, receipt.isSelected());
        });
    }

    private void fill() {
        rows.getChildren().clear();
        checks.clear();
        String c = club.getValue();
        String[] heads = {"", "Name", "Klassen", "Gebühr", "Stand"};
        for (int k = 0; k < heads.length; k++) {
            Label h = new Label(heads[k]);
            h.getStyleClass().add("table-head");
            rows.add(h, k, 0);
        }
        java.text.Collator col = java.text.Collator.getInstance(java.util.Locale.GERMAN);
        List<Player> members = tournament.getPlayers().stream()
                .filter(p -> c != null && c.equals(p.getClubName()) && (tournament.feeOf(p) > 0 || p.isPaid()))
                .sorted(java.util.Comparator.comparing((Player p) -> p.getLastname() == null ? "" : p.getLastname(), col)
                        .thenComparing(p -> p.getFirstname() == null ? "" : p.getFirstname(), col))
                .toList();
        int r = 1;
        for (Player p : members) {
            CheckBox cb = new CheckBox();
            cb.setSelected(!p.isPaid());
            cb.setDisable(p.isPaid());
            cb.selectedProperty().addListener((o, a, b) -> updateTotal());
            checks.put(p, cb);
            Label name = new Label(p.getDisplayName());
            Label cls = new Label(String.join(", ", tournament.feeClassesOf(p).stream().map(PlayingClass::getName).toList())
                    + (tournament.isLateEntry(p) && tournament.getLateFeeCents() > 0 ? " + Nachmeldung" : ""));
            cls.getStyleClass().add("hint");
            Label fee = new Label(Tournament.euro(p.isPaid() ? p.getPaidCents() : tournament.feeOf(p)));
            Label state = new Label(p.getPayment() == Player.Payment.WAIVED ? "erlassen" : p.isPaid() ? "bezahlt (" + p.getPayment().getLabel() + ")" : "offen");
            state.getStyleClass().add(p.isPaid() ? "fee-paid-label" : "fee-open-label");
            rows.addRow(r++, cb, name, cls, fee, state);
        }
        updateTotal();
    }

    private void updateTotal() {
        int sum = 0, n = 0;
        for (var e : checks.entrySet()) {
            if (e.getValue().isSelected() && !e.getValue().isDisabled()) {
                sum += tournament.feeOf(e.getKey());
                n++;
            }
        }
        total.setText(n == 0 ? "Keine offenen Startgebühren ausgewählt."
                : "Zu zahlen: " + Tournament.euro(sum) + " für " + n + (n == 1 ? " Person" : " Personen"));
    }
}
