package de.hts.ui;

import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Tische bestimmten Klassen zuordnen: Matrix Tisch × Klasse. Ein Tisch ohne Haken ist für alle Klassen da (Standard).
 * Ein reservierter Tisch bekommt aus der Warteschlange nur Spiele seiner Klassen – notfalls ein späteres Spiel.
 */
final class TableAssignmentDialog extends Dialog<ButtonType> {

    TableAssignmentDialog(Window owner, Tournament t) {
        initOwner(owner);
        setTitle("Tische den Klassen zuordnen");
        setHeaderText("Haken setzen = Tisch nur für diese Klasse(n). Ohne Haken spielt an dem Tisch jede Klasse.");
        List<PlayingClass> classes = t.rootClasses();
        int n = t.getTableCount();
        CheckBox[][] boxes = new CheckBox[n + 1][classes.size()];
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(6);
        grid.setPadding(new Insets(6));
        // einheitliche Spalten: Klassenname (umbrechend, gleiche Höhe) und „alle“ zentriert über den Haken
        double colW = 120;
        javafx.scene.layout.ColumnConstraints first = new javafx.scene.layout.ColumnConstraints(70);
        grid.getColumnConstraints().add(first);
        for (int k = 0; k < classes.size(); k++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints(colW);
            cc.setHalignment(javafx.geometry.HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }
        Label tableHead = new Label("Tisch");
        tableHead.getStyleClass().add("class-name");
        grid.add(tableHead, 0, 0);
        for (int k = 0; k < classes.size(); k++) {
            Label l = new Label(classes.get(k).getName());
            l.getStyleClass().add("class-name");
            l.setWrapText(true);
            l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            l.setAlignment(Pos.CENTER);
            l.setMaxWidth(colW);
            l.setPrefWidth(colW);
            l.setMinHeight(38);
            l.setPrefHeight(38);
            int col = k;
            Button all = new Button("alle");
            all.getStyleClass().addAll("pill", "small-pill");
            all.setTooltip(new javafx.scene.control.Tooltip("Alle Tische nur für „" + classes.get(k).getName() + "“ an/aus"));
            all.setOnAction(e -> {
                boolean target = false;
                for (int tb = 1; tb <= n; tb++) target |= !boxes[tb][col].isSelected();
                for (int tb = 1; tb <= n; tb++) boxes[tb][col].setSelected(target);
            });
            VBox head = new VBox(4, l, all);
            head.setAlignment(Pos.BOTTOM_CENTER);
            head.setPrefWidth(colW);
            grid.add(head, k + 1, 0);
        }
        Label state = new Label();
        for (int tb = 1; tb <= n; tb++) {
            Label l = new Label(t.tableName(tb));
            l.getStyleClass().add("class-name");
            grid.add(l, 0, tb);
            for (int k = 0; k < classes.size(); k++) {
                CheckBox cb = new CheckBox();
                cb.setSelected(t.getTableClasses(tb).contains(classes.get(k).getName()));
                boxes[tb][k] = cb;
                HBox cell = new HBox(cb);
                cell.setAlignment(Pos.CENTER);
                grid.add(cell, k + 1, tb);
            }
        }
        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(Math.min(520, 40 + n * 30));
        Label hint = new Label("Ist das nächste Spiel der Warteschlange für einen freien Tisch nicht freigegeben, bekommt der Tisch "
                + "das erste passende spätere Spiel (z. B. Spiel 3, 7 oder 12 der Warteschlange).");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        VBox content = new VBox(10, sp, hint, state);
        content.setPrefWidth(Math.min(1000, 120 + classes.size() * 134));
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogSizing.fit(this);
        setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                for (int tb = 1; tb <= n; tb++) {
                    List<String> names = new ArrayList<>();
                    for (int k = 0; k < classes.size(); k++) if (boxes[tb][k].isSelected()) names.add(classes.get(k).getName());
                    t.setTableClasses(tb, names);
                }
                t.dispatch();
            }
            return bt;
        });
    }
}
