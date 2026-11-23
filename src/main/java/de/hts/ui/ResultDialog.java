/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.tournament.Match;
import de.hts.tournament.Scoring;
import de.hts.tournament.Tournament;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Ergebnis eines Spiels Satz für Satz erfassen (oder korrigieren).
 *
 * <p>Je Satz ein Feld: „11:8“, „7:11“, „12:14“ – oder kurz „8“ bzw. „+8“ (→ 11:8), „-7“ (→ 7:11), „0“ (→ 11:0),
 * „10“ (→ 12:10); ohne Vorzeichen gilt „+“ (0.38). Mit Enter geht es zum nächsten Satz. Die erste Zahl gehört immer zur erstgenannten Partei.
 * Ungültige Sätze (z. B. 13:14) werden rot markiert; gespeichert werden kann erst ein vollständiges, gültiges Ergebnis.
 * Alternativ kann eine Aufgabe markiert werden – dann gilt 11:0 je Gewinnsatz für den Gegner.
 */
public final class ResultDialog extends Dialog<ResultDialog.Result> {

    /** @param retired 0 = keine Aufgabe, 1 = A gibt auf, 2 = B gibt auf */
    public record Result(List<int[]> sets, int retired) {
    }

    private final List<TextField> fields = new ArrayList<>();
    private final Label summary = new Label();
    private final Label error = new Label();
    private final ToggleGroup retire = new ToggleGroup();
    private final RadioButton noRetire = new RadioButton("keine Aufgabe");
    private final RadioButton retireA = new RadioButton();
    private final RadioButton retireB = new RadioButton();
    private final int winningSets;
    private final String nameA;
    private final String nameB;
    private Scoring.Evaluation evaluation;
    private javafx.scene.control.Button okButton;
    private boolean adjusting;
    /** 1.5.0 (#121/#122): zuletzt bearbeitetes Satzfeld – Ziel von Zahlenfeld und Schnelleingabe. */
    private TextField lastField;

    public ResultDialog(Window owner, Tournament t, Match m) {
        initOwner(owner);
        boolean correction = m.getStatus() == de.hts.tournament.MatchStatus.DONE;
        winningSets = t.winningSets(m);
        nameA = t.partyFullName(m, m.getA());
        nameB = t.partyFullName(m, m.getB());
        boolean doublesMatch = m.getA() != null && m.getA().startsWith("pair:");
        setTitle(correction ? "Ergebnis korrigieren" : "Ergebnis eintragen");
        // Doppel: Paarungen ausgeschrieben, je Partei eine eigene Zeile statt alles in eine zu quetschen
        setHeaderText(m.getClassName() + " · " + m.getStage() + " · Spiel " + m.getNumber()
                + (m.getTable() != null ? " · " + t.tableName(m.getTable()) : "")
                + (doublesMatch ? "\n" + nameA + "\ngegen\n" + nameB : "\n" + nameA + "  –  " + nameB));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        Label la = new Label(nameA);
        Label lb = new Label(nameB);
        la.getStyleClass().add("match-name");
        lb.getStyleClass().add("match-name");
        DrawView.handicapBadge(la, t, m, m.getA());
        DrawView.handicapBadge(lb, t, m, m.getB());
        Label satzHead = new Label("Satz");
        satzHead.setMinWidth(Region.USE_PREF_SIZE);
        grid.add(satzHead, 0, 0);
        grid.add(new Label("Punkte"), 1, 0);
        int maxSets = 2 * winningSets - 1;
        for (int i = 0; i < maxSets; i++) {
            TextField f = new TextField();
            f.setPromptText(i == 0 ? "z. B. 11:8 oder 8" : "");
            f.setPrefColumnCount(10);
            f.getStyleClass().add("set-field");
            int idx = i;
            f.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    normalize(f);
                    update();
                    if (evaluation != null && evaluation.isComplete()) {
                        // Spiel entschieden: Fokus auf „Ergebnis speichern“ – ein weiteres Enter speichert
                        if (okButton != null && !okButton.isDisabled()) okButton.requestFocus();
                    } else if (idx + 1 < fields.size() && !fields.get(idx + 1).isDisabled()) {
                        fields.get(idx + 1).requestFocus();
                    }
                    e.consume();
                }
            });
            f.focusedProperty().addListener((o, was, now) -> {
                if (now) lastField = f;
                if (!now) normalize(f);
            });
            f.textProperty().addListener((o, a, b) -> update());
            fields.add(f);
            Label sl = new Label((i + 1) + ". Satz");
            sl.setMinWidth(Region.USE_PREF_SIZE);
            grid.add(sl, 0, i + 1);
            grid.add(f, 1, i + 1);
        }

        noRetire.setToggleGroup(retire);
        retireA.setToggleGroup(retire);
        retireB.setToggleGroup(retire);
        retireA.setText(nameA + " gibt auf");
        retireB.setText(nameB + " gibt auf");
        for (RadioButton rb : new RadioButton[]{retireA, retireB}) {
            rb.setWrapText(true);
            rb.setMaxWidth(330);
            rb.setMinHeight(Region.USE_PREF_SIZE);
        }
        noRetire.setSelected(true);
        retire.selectedToggleProperty().addListener((o, a, b) -> update());
        Label retireLabel = new Label("Aufgabe:");
        retireLabel.getStyleClass().add("class-name");
        Label retireHint = new Label("Bei Aufgabe werden auch alle noch offenen Spiele dieser Partei in dieser Phase für die Gegner "
                + "gewertet (3:0).");
        retireHint.getStyleClass().add("hint");
        retireHint.setWrapText(true);
        retireHint.setMaxWidth(330);
        retireHint.setMinHeight(Region.USE_PREF_SIZE);
        VBox retireBox = new VBox(4, retireLabel, noRetire, retireA, retireB, retireHint);

        summary.getStyleClass().add("result-summary");
        summary.setWrapText(true);
        error.getStyleClass().add("error");
        error.setWrapText(true);
        Label hint = new Label("Eingabe: 11:8 · 7:11 · 18:16 – oder kurz 8 bzw. +8 (= 11:8), -7 (= 7:11), 0 (= 11:0), "
                + "10 (= 12:10); ohne Vorzeichen gilt „+“. Enter = nächster Satz; ist das Spiel entschieden, speichert ein weiteres Enter. "
                + "Gewinnsätze: " + winningSets + " – danach sind die übrigen Sätze gesperrt.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        VBox quick = quickEntry();
        VBox pad = keypad();
        HBox top = new HBox(28, grid, retireBox);
        javafx.scene.layout.FlowPane inputs = new javafx.scene.layout.FlowPane(28, 12, top, pad);
        VBox content = new VBox(12, inputs, quick, summary, error, hint);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);
        content.setPrefWidth(doublesMatch ? 760 : 660);
        getDialogPane().setContent(content);

        // vorhandenes Ergebnis übernehmen (Korrektur)
        if (m.getRetired() != 0) {
            (m.getRetired() == 1 ? retireA : retireB).setSelected(true);
        } else {
            List<int[]> pts = m.getPoints();
            for (int i = 0; i < pts.size() && i < fields.size(); i++) fields.get(i).setText(Scoring.format(pts.get(i)));
        }

        ButtonType ok = new ButtonType(correction ? "Korrektur speichern" : "Ergebnis speichern", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        okButton = (javafx.scene.control.Button) getDialogPane().lookupButton(ok);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            fields.forEach(this::normalize);
            update();
            if (evaluation == null || !evaluation.isComplete()) {
                error.setText(evaluation == null ? "Bitte die Sätze eingeben." : evaluation.message());
                e.consume();
            }
        });
        getDialogPane().lookupButton(ok).disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> evaluation == null || !evaluation.isComplete(), summary.textProperty()));
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok ? new Result(currentSets(), retiredSide()) : null);
        setOnShown(e -> Platform.runLater(() -> fields.get(0).requestFocus()));
        update();
    }

    /** Feld, in das Zahlenfeld/Schnelleingabe schreiben: zuletzt benutztes, sonst das erste freie. */
    private TextField target() {
        if (lastField != null && !lastField.isDisabled()) return lastField;
        for (TextField f : fields) if (!f.isDisabled() && f.getText().isBlank()) return f;
        return fields.get(0);
    }

    /** Wie Enter im Feld: übernehmen und zum nächsten Satz bzw. zum Speichern-Knopf. */
    private void advance(TextField f) {
        normalize(f);
        update();
        int idx = fields.indexOf(f);
        if (evaluation != null && evaluation.isComplete()) {
            lastField = null;
            if (okButton != null && !okButton.isDisabled()) okButton.requestFocus();
        } else if (idx + 1 < fields.size() && !fields.get(idx + 1).isDisabled()) {
            lastField = fields.get(idx + 1);
            lastField.requestFocus();
        }
    }

    /**
     * 1.5.0 (#122): Schnelleingabe – häufige Satzergebnisse mit einem Klick bzw. Tipp (11:3 bis 11:9 je Seite); trägt in
     * den aktuellen Satz ein und springt weiter. Seltenes wie 13:11 von Hand.
     */
    private VBox quickEntry() {
        Label head = new Label("Schnelleingabe (aktueller Satz):");
        head.getStyleClass().add("class-name");
        javafx.scene.layout.FlowPane a = new javafx.scene.layout.FlowPane(6, 6);
        javafx.scene.layout.FlowPane b = new javafx.scene.layout.FlowPane(6, 6);
        Label la = new Label("Satz für " + nameA + ":");
        Label lb = new Label("Satz für " + nameB + ":");
        la.setMinWidth(Region.USE_PREF_SIZE);
        lb.setMinWidth(Region.USE_PREF_SIZE);
        a.getChildren().add(la);
        b.getChildren().add(lb);
        for (int l = 3; l <= 9; l++) {
            a.getChildren().add(quickButton(11 + ":" + l));
            b.getChildren().add(quickButton(l + ":" + 11));
        }
        a.setAlignment(Pos.CENTER_LEFT);
        b.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, head, a, b);
    }

    private javafx.scene.control.Button quickButton(String text) {
        javafx.scene.control.Button q = new javafx.scene.control.Button(text);
        q.getStyleClass().addAll("pill", "quick-set");
        q.setFocusTraversable(false);
        q.setMinWidth(Region.USE_PREF_SIZE);
        q.setOnAction(e -> {
            TextField f = target();
            f.setText(text);
            advance(f);
        });
        return q;
    }

    /**
     * 1.5.0 (#121): Zahlenfeld für die Bedienung mit dem Finger (Tablet-Modus bzw. nach dem ersten Antippen) – damit
     * die Bildschirmtastatur nicht gebraucht wird. Schreibt in das aktuelle Satzfeld.
     */
    private VBox keypad() {
        javafx.scene.layout.GridPane g = new javafx.scene.layout.GridPane();
        g.setHgap(6);
        g.setVgap(6);
        String[][] keys = {{"7", "8", "9"}, {"4", "5", "6"}, {"1", "2", "3"}, {"−", "0", ":"}};
        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < 3; c++) {
                String k = keys[r][c];
                javafx.scene.control.Button bt = padButton(k);
                bt.setOnAction(e -> {
                    TextField f = target();
                    f.appendText(k.equals("−") ? "-" : k);
                    f.positionCaret(f.getText().length());
                });
                g.add(bt, c, r);
            }
        }
        javafx.scene.control.Button del = padButton("⌫");
        del.setOnAction(e -> {
            TextField f = target();
            String t = f.getText();
            if (!t.isEmpty()) f.setText(t.substring(0, t.length() - 1));
        });
        javafx.scene.control.Button next = padButton("Weiter");
        next.getStyleClass().remove("pill");
        next.getStyleClass().add("navy-pill");
        next.setOnAction(e -> advance(target()));
        g.add(del, 0, 4);
        g.add(next, 1, 4, 2, 1);
        next.setMaxWidth(Double.MAX_VALUE);
        Label head = new Label("Zahlenfeld");
        head.getStyleClass().add("class-name");
        VBox box = new VBox(6, head, g);
        box.getStyleClass().add("keypad");
        // nur im Tablet-Modus bzw. nach dem ersten Antippen sichtbar
        Runnable show = () -> {
            boolean on = TouchSupport.isTabletMode() || (getDialogPane().getScene() != null && TouchSupport.isTouch(getDialogPane()));
            box.setVisible(on);
            box.setManaged(on);
        };
        show.run();
        getDialogPane().sceneProperty().addListener((o, x, sc) -> {
            if (sc == null) return;
            show.run();
            sc.rootProperty().addListener((oo, xx, r) -> show.run());
            sc.addEventFilter(javafx.scene.input.TouchEvent.TOUCH_PRESSED, ev -> javafx.application.Platform.runLater(show));
        });
        return box;
    }

    private static javafx.scene.control.Button padButton(String k) {
        javafx.scene.control.Button bt = new javafx.scene.control.Button(k);
        bt.getStyleClass().addAll("pill", "pad-key");
        bt.setFocusTraversable(false);
        bt.setMinSize(56, 48);
        bt.setPrefSize(56, 48);
        return bt;
    }

    private int retiredSide() {
        if (retireA.isSelected()) return 1;
        if (retireB.isSelected()) return 2;
        return 0;
    }

    /** Kurzeingabe in die Langform umwandeln, z. B. „+8“ bzw. „8“ → „11:8“. */
    private void normalize(TextField f) {
        int[] s = Scoring.parseSet(f.getText());
        if (s != null) {
            String formatted = Scoring.format(s);
            if (!formatted.equals(f.getText())) f.setText(formatted);
        }
    }

    private List<int[]> currentSets() {
        List<int[]> sets = new ArrayList<>();
        int lastFilled = -1;
        for (int i = 0; i < fields.size(); i++) if (!fields.get(i).isDisabled() && !fields.get(i).getText().isBlank()) lastFilled = i;
        for (int i = 0; i < fields.size(); i++) {
            TextField f = fields.get(i);
            if (f.isDisabled() && retiredSide() == 0) continue;
            if (f.getText().isBlank() && i < lastFilled) {
                sets.add(new int[]{-1, -1});   // fehlender Satz -> ungültig, blockiert das Speichern
                continue;
            }
            int[] s = Scoring.parseSet(f.getText());
            if (s != null) sets.add(s);
            else if (!f.getText().isBlank()) sets.add(new int[]{-1, -1});   // unlesbar -> ungültig
        }
        return sets;
    }

    private boolean isDecided() {
        return evaluation != null && evaluation.decided();
    }

    private void update() {
        if (adjusting) return;
        int retired = retiredSide();
        // Sobald eine Seite die Gewinnsätze erreicht hat, werden die folgenden Sätze gesperrt (und geleert)
        int wa = 0, wb = 0, decidedAt = -1;
        for (int i = 0; i < fields.size(); i++) {
            int[] st = Scoring.parseSet(fields.get(i).getText());
            if (st == null || !Scoring.isValidSet(st[0], st[1])) {
                if (fields.get(i).getText().isBlank()) continue;
                break;
            }
            if (st[0] > st[1]) wa++;
            else wb++;
            if (wa == winningSets || wb == winningSets) {
                decidedAt = i;
                break;
            }
        }
        adjusting = true;
        try {
            for (int i = 0; i < fields.size(); i++) {
                TextField f = fields.get(i);
                boolean off = retired != 0 || (decidedAt >= 0 && i > decidedAt);
                if (off && decidedAt >= 0 && retired == 0 && !f.getText().isBlank()) f.setText("");
                f.setDisable(off);
            }
        } finally {
            adjusting = false;
        }
        // Felder einzeln markieren; ein leerer Satz vor einem eingetragenen gilt als fehlerhaft
        int setsA = 0, setsB = 0;
        int lastFilled = -1;
        for (int i = 0; i < fields.size(); i++) if (!fields.get(i).isDisabled() && !fields.get(i).getText().isBlank()) lastFilled = i;
        for (int i = 0; i < fields.size(); i++) {
            TextField f = fields.get(i);
            f.getStyleClass().removeAll("set-invalid", "set-a", "set-b");
            if (f.getText().isBlank()) {
                if (retired == 0 && i < lastFilled) f.getStyleClass().add("set-invalid");
                continue;
            }
            int[] s = Scoring.parseSet(f.getText());
            if (s == null || !Scoring.isValidSet(s[0], s[1])) {
                f.getStyleClass().add("set-invalid");
            } else {
                f.getStyleClass().add(s[0] > s[1] ? "set-a" : "set-b");
            }
        }
        List<int[]> sets = retired != 0 ? Scoring.walkover(retired, winningSets) : currentSets();
        evaluation = Scoring.evaluate(sets, winningSets);
        setsA = evaluation.setsA();
        setsB = evaluation.setsB();
        String text;
        if (retired != 0) {
            text = (retired == 1 ? nameA : nameB) + " gibt auf – Wertung " + (retired == 1 ? "0:" + winningSets : winningSets + ":0")
                    + " für " + (retired == 1 ? nameB : nameA) + ".";
        } else if (evaluation.isComplete()) {
            text = "Satzverhältnis " + setsA + ":" + setsB + " – Sieger: " + (evaluation.winner() == 1 ? nameA : nameB)
                    + " (Bälle " + evaluation.pointsA() + ":" + evaluation.pointsB() + ")";
        } else {
            text = "Satzverhältnis " + setsA + ":" + setsB;
        }
        summary.setText(text);
        error.setText(evaluation.isComplete() || sets.isEmpty() ? "" : evaluation.message());
    }
}
