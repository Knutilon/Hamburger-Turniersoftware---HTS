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
 * <p>Je Satz ein Feld: „11:8“, „7:11“, „12:14“ – oder kurz „+8“ (→ 11:8), „-7“ (→ 7:11), „+0“ (→ 11:0),
 * „+10“ (→ 12:10). Mit Enter geht es zum nächsten Satz. Die erste Zahl gehört immer zur erstgenannten Partei.
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
            f.setPromptText(i == 0 ? "z. B. 11:8 oder +8" : "");
            f.setPrefColumnCount(8);
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
        Label retireHint = new Label("Bei Aufgabe werden auch alle noch offenen Spiele dieser Partei\nin dieser Phase für die Gegner gewertet (3:0).");
        retireHint.getStyleClass().add("hint");
        VBox retireBox = new VBox(4, retireLabel, noRetire, retireA, retireB, retireHint);

        summary.getStyleClass().add("result-summary");
        summary.setWrapText(true);
        error.getStyleClass().add("error");
        error.setWrapText(true);
        Label hint = new Label("Eingabe: 11:8 · 7:11 · 18:16 – oder kurz +8 (= 11:8), -7 (= 7:11), +0 (= 11:0), "
                + "+10 (= 12:10). Enter = nächster Satz; ist das Spiel entschieden, speichert ein weiteres Enter. "
                + "Gewinnsätze: " + winningSets + " – danach sind die übrigen Sätze gesperrt.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        HBox top = new HBox(28, grid, retireBox);
        VBox content = new VBox(12, top, summary, error, hint);
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

    private int retiredSide() {
        if (retireA.isSelected()) return 1;
        if (retireB.isSelected()) return 2;
        return 0;
    }

    /** Kurzeingabe in die Langform umwandeln, z. B. „+8“ → „11:8“. */
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
