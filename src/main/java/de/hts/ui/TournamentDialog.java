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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.converter.LocalDateStringConverter;

import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;

/** Name und Datum eines Turniers erfassen (Anlegen oder Bearbeiten). Ergebnis: die eingegebenen Werte. */
public final class TournamentDialog extends Dialog<TournamentDialog.Result> {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** @param fileDir gewählter Ordner für die Turnierdatei ({@code null} = Standard bzw. unverändert) */
    public record Result(String name, LocalDate date, int tables, java.nio.file.Path fileDir, boolean test,
                         de.hts.persistence.TournamentStore.Summary copyFrom, de.hts.persistence.TournamentStore.CopyMode copyMode,
                         boolean handicap) {
        public Result(String name, LocalDate date, int tables) {
            this(name, date, tables, null, false, null, null, false);
        }

        public Result(String name, LocalDate date, int tables, java.nio.file.Path fileDir, boolean test,
                      de.hts.persistence.TournamentStore.Summary copyFrom, de.hts.persistence.TournamentStore.CopyMode copyMode) {
            this(name, date, tables, fileDir, test, copyFrom, copyMode, false);
        }
    }

    public TournamentDialog(Window owner, String name, LocalDate date, boolean isNew) {
        this(owner, name, date, isNew ? null : 1, isNew);
    }

    public TournamentDialog(Window owner, String name, LocalDate date, Integer tables, boolean isNew) {
        this(owner, name, date, tables, isNew, null);
    }

    /**
     * @param tables      Anzahl Spieltische ({@code null} = leer, muss angegeben werden)
     * @param currentFile bisherige Turnierdatei (Bearbeiten) – {@code null} beim Anlegen
     */
    public TournamentDialog(Window owner, String name, LocalDate date, Integer tables, boolean isNew, java.nio.file.Path currentFile) {
        this(owner, name, date, tables, isNew, currentFile, false, List.of());
    }

    /**
     * @param test        bisher als Testturnier markiert
     * @param copySources bestehende Turniere, die beim Anlegen kopiert werden können (leer = keine Kopie anbieten)
     */
    public TournamentDialog(Window owner, String name, LocalDate date, Integer tables, boolean isNew, java.nio.file.Path currentFile,
                            boolean test, List<de.hts.persistence.TournamentStore.Summary> copySources) {
        this(owner, name, date, tables, isNew, currentFile, test, copySources, false);
    }

    /** @param handicap bisher als Vorgabeturnier markiert (Bearbeiten: nur Anzeige, nicht änderbar) */
    public TournamentDialog(Window owner, String name, LocalDate date, Integer tables, boolean isNew, java.nio.file.Path currentFile,
                            boolean test, List<de.hts.persistence.TournamentStore.Summary> copySources, boolean handicap) {
        initOwner(owner);
        setTitle(isNew ? "Neues Turnier" : "Turnier bearbeiten");
        setHeaderText(isNew ? "Neues Turnier anlegen" : "Name und Datum des Turniers");

        TextField nameField = new TextField(name == null ? "" : name);
        nameField.setPromptText("z. B. 12. Hamburger Jugendranglistenturnier");
        nameField.setPrefColumnCount(28);
        DatePicker datePicker = new DatePicker(date != null ? date : (isNew ? LocalDate.now() : null));
        datePicker.setConverter(new LocalDateStringConverter(DATE, DATE));
        datePicker.setPromptText("TT.MM.JJJJ");
        javafx.scene.control.TextField tablesField = new javafx.scene.control.TextField(tables == null ? "" : tables.toString());
        tablesField.setPromptText("z. B. 12");
        tablesField.setPrefColumnCount(6);
        tablesField.setTextFormatter(new javafx.scene.control.TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0,3}") ? c : null));
        Label error = new Label();
        error.getStyleClass().add("error");

        // Speicherort der Turnierdatei: Standard „Dokumente\HTS Turniere\<Name>\Turnierdatei“, per „Durchsuchen“ änderbar
        java.nio.file.Path[] chosen = {null};
        Label where = new Label();
        where.getStyleClass().add("hint");
        where.setWrapText(true);
        where.setMaxWidth(380);
        Runnable showWhere = () -> {
            if (chosen[0] != null) where.setText(chosen[0].toString());
            else if (currentFile != null) where.setText(currentFile.getParent().toString());
            else where.setText(de.hts.persistence.StorageLocations.root()
                    .resolve(de.hts.persistence.StorageLocations.safeName(nameField.getText()))
                    .resolve(de.hts.persistence.StorageLocations.FILE_DIR) + "  (Standard)");
        };
        nameField.textProperty().addListener((o, a, b) -> showWhere.run());
        showWhere.run();
        javafx.scene.control.Button browse = new javafx.scene.control.Button("Durchsuchen");
        browse.getStyleClass().add("pill");
        browse.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle("Ordner für die Turnierdatei wählen");
            java.nio.file.Path start = chosen[0] != null ? chosen[0] : currentFile != null ? currentFile.getParent()
                    : de.hts.persistence.StorageLocations.root();
            dc.setInitialDirectory(de.hts.persistence.StorageLocations.ensure(start));
            java.io.File d = dc.showDialog(getDialogPane().getScene().getWindow());
            if (d != null) {
                chosen[0] = currentFile != null && d.toPath().equals(currentFile.getParent()) ? null : d.toPath();
                showWhere.run();
            }
        });
        javafx.scene.layout.HBox whereBox = new javafx.scene.layout.HBox(8, where, browse);
        whereBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Datum:"), datePicker);
        grid.addRow(2, new Label("Spieltische:"), tablesField);
        grid.addRow(3, new Label("Turnierdatei:"), whereBox);
        javafx.scene.control.CheckBox testBox = new javafx.scene.control.CheckBox("Testturnier (im Kopfbereich: „Alle Spiele simulieren“)");
        testBox.setSelected(test);
        grid.add(testBox, 1, 4);
        // Vorgabeturnier: nur beim Anlegen wählbar, danach fest
        javafx.scene.control.CheckBox handicapBox = new javafx.scene.control.CheckBox("Vorgabeturnier (Punktevorsprung nach TTR – nur beim Anlegen wählbar)");
        handicapBox.setSelected(handicap);
        handicapBox.setDisable(!isNew);
        handicapBox.setTooltip(new javafx.scene.control.Tooltip("TTR-Unterschiede werden durch einen Punktevorsprung je Satz ausgeglichen. "
                + "Die Regeln legt der Reiter „Vorgaberegeln“ fest. Später nicht mehr umschaltbar."));
        grid.add(handicapBox, 1, 5);

        // Kopie eines bestehenden Turniers
        javafx.scene.control.ComboBox<de.hts.persistence.TournamentStore.Summary> source = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<de.hts.persistence.TournamentStore.CopyMode> copyMode = new javafx.scene.control.ComboBox<>();
        if (isNew && copySources != null && !copySources.isEmpty()) {
            source.getItems().add(null);
            source.getItems().addAll(copySources);
            source.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(de.hts.persistence.TournamentStore.Summary x) {
                    if (x == null) return "– leeres Turnier –";
                    return (x.name() == null ? "(ohne Namen)" : x.name()) + (x.date() == null ? "" : "  ·  " + x.date().format(DATE))
                            + (x.test() ? "  ·  Test" : "");
                }

                @Override
                public de.hts.persistence.TournamentStore.Summary fromString(String v) {
                    return null;
                }
            });
            source.setPrefWidth(380);
            copyMode.getItems().setAll(de.hts.persistence.TournamentStore.CopyMode.values());
            copyMode.setValue(de.hts.persistence.TournamentStore.CopyMode.WITHOUT_DRAW);
            copyMode.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(de.hts.persistence.TournamentStore.CopyMode m) {
                    return m == null ? "" : m.getLabel();
                }

                @Override
                public de.hts.persistence.TournamentStore.CopyMode fromString(String v) {
                    return null;
                }
            });
            copyMode.setPrefWidth(380);
            copyMode.disableProperty().bind(source.valueProperty().isNull());
            source.valueProperty().addListener((o, a, b) -> {
                if (b == null) return;
                if (nameField.getText().isBlank()) nameField.setText("Kopie von " + (b.name() == null ? "Turnier" : b.name()));
                if (tablesField.getText().isBlank()) tablesField.setText(String.valueOf(Math.max(1, b.tables())));
                if (b.test()) testBox.setSelected(true);
            });
            // eine Kopie übernimmt die Art des Turniers (Vorgabeturnier ja/nein) vom Original
            source.valueProperty().addListener((o, a, b) -> {
                handicapBox.setDisable(b != null);
                if (b != null) handicapBox.setSelected(b.handicap());
            });
            grid.addRow(6, new Label("Kopie von:"), source);
            grid.addRow(7, new Label("Übernehmen:"), copyMode);
        }
        grid.add(error, 0, 8, 2, 1);
        getDialogPane().setContent(grid);

        ButtonType ok = new ButtonType(isNew ? "Anlegen" : "Übernehmen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            // im Datumsfeld getippten Text übernehmen
            try {
                datePicker.setValue(datePicker.getConverter().fromString(datePicker.getEditor().getText()));
            } catch (RuntimeException ex) {
                error.setText("Bitte das Datum als TT.MM.JJJJ eingeben.");
                e.consume();
                return;
            }
            if (nameField.getText() == null || nameField.getText().isBlank()) {
                error.setText("Bitte einen Namen eingeben.");
                e.consume();
            } else if (datePicker.getValue() == null) {
                error.setText("Bitte ein Datum wählen.");
                e.consume();
            } else if (tablesField.getText().isBlank() || Integer.parseInt(tablesField.getText()) < 1) {
                error.setText("Bitte die Anzahl der Spieltische angeben (mindestens 1).");
                e.consume();
            }
        });
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok
                ? new Result(nameField.getText().trim(), datePicker.getValue(), Integer.parseInt(tablesField.getText()), chosen[0],
                        testBox.isSelected(), source.getValue(), source.getValue() == null ? null : copyMode.getValue(),
                        handicapBox.isSelected())
                : null);
        setOnShown(e -> nameField.requestFocus());
    }
}
