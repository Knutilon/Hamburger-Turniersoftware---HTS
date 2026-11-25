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

    private final javafx.scene.control.TextField lateFeeField = ClassDialog.euroField();

    /** 1.0 (#82): Nachmeldegebühr vorbelegen (Cent). */
    public TournamentDialog withLateFee(int cents) {
        lateFeeField.setText(cents <= 0 ? "" : ClassDialog.euroText(cents));
        return this;
    }

    /** 1.0 (#82): eingegebene Nachmeldegebühr in Cent (leer = 0). */
    // 1.8.0 (#151): Link zur myTischtennis-Turnierwelt
    private final javafx.scene.control.TextField worldField = new javafx.scene.control.TextField();

    public TournamentDialog withWorldUrl(String url) {
        worldField.setText(url == null ? "" : url);
        return this;
    }

    // 1.9.0 (#164): Online-Anmeldung bis (Uhrzeit am Turniertag)
    private final javafx.scene.control.TextField untilField = new javafx.scene.control.TextField();

    public TournamentDialog withCheckinUntil(java.time.LocalTime t) {
        untilField.setText(t == null ? "" : t.toString());
        return this;
    }

    /** Eingetragene Uhrzeit oder {@code null} (leer bzw. ungültig). */
    public java.time.LocalTime checkinUntil() {
        return parseTime(untilField.getText());
    }

    static java.time.LocalTime parseTime(String s) {
        String txt = s == null ? "" : s.strip().replace('.', ':');
        if (txt.isEmpty()) return null;
        try {
            return java.time.LocalTime.parse(txt.length() == 4 ? "0" + txt : txt);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    /** Eingetragener Link zur Turnierwelt ({@code ""} = keiner). */
    public String worldUrl() {
        return worldField.getText() == null ? "" : worldField.getText().strip();
    }

    public int lateFeeCents() {
        Integer c = de.hts.importer.clicktt.ClickTtXmlImporter.feeCents(lateFeeField.getText());
        return c == null ? 0 : c;
    }

    private static Label hintLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hint");
        l.setWrapText(true);
        l.setMaxWidth(360);
        return l;
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
            else if (de.hts.persistence.StorageLocations.newTournamentDir() != null)          // 1.8.0 (#152)
                where.setText(de.hts.persistence.StorageLocations.newTournamentDir() + "  (Standard)");
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
                    : de.hts.persistence.StorageLocations.newTournamentDir() != null
                    ? de.hts.persistence.StorageLocations.newTournamentDir() : de.hts.persistence.StorageLocations.root();
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
        // 1.0 (#82): Nachmeldegebühr (einmal je Person, die nicht in der click-TT-Datei stand)
        Label lateLabel = new Label("Nachmeldegebühr (€):");
        javafx.scene.layout.HBox lateBox = new javafx.scene.layout.HBox(8, lateFeeField, hintLabel(
                "optional, einmal je Person, die nicht in der click-TT-Datei stand (nicht im Vorgabeturnier)"));
        lateBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        lateFeeField.setPromptText("z. B. 2,00");
        grid.addRow(8, lateLabel, lateBox);
        lateLabel.visibleProperty().bind(handicapBox.selectedProperty().not());
        lateBox.visibleProperty().bind(handicapBox.selectedProperty().not());
        // 1.8.0 (#151): Link zur Turnierwelt (myTischtennis)
        worldField.setPromptText("z. B. https://www.mytischtennis.de/turnierwelt");
        worldField.setPrefColumnCount(28);
        Label worldHint = hintLabel("optional – erscheint auf den Live-Seiten als Link „zur Turnierwelt“");
        Label worldWarn = new Label();
        worldWarn.getStyleClass().add("warning");
        worldWarn.setWrapText(true);
        worldWarn.setMaxWidth(380);
        worldWarn.managedProperty().bind(worldWarn.textProperty().isNotEmpty());
        Runnable checkWorld = () -> {
            String u = worldUrl();
            String ok = de.hts.tournament.Tournament.validWorldUrl(u);
            worldWarn.setText(u.isEmpty() || (ok != null && ok.toLowerCase(java.util.Locale.ROOT).contains("mytischtennis.de")) ? ""
                    : ok == null ? "Bitte eine vollständige Adresse mit https:// eingeben." : "Hinweis: Das ist keine Adresse von mytischtennis.de.");
        };
        javafx.scene.control.Hyperlink worldOpen = new javafx.scene.control.Hyperlink("Turnierwelt im Browser öffnen");
        worldOpen.setOnAction(e -> {
            String ok = de.hts.tournament.Tournament.validWorldUrl(worldUrl());
            if (ok != null) de.hts.HtsApp.openLink(ok);
        });
        worldField.textProperty().addListener((o, a, b) -> {
            checkWorld.run();
            worldOpen.setVisible(de.hts.tournament.Tournament.validWorldUrl(worldUrl()) != null);
        });
        checkWorld.run();
        worldOpen.setVisible(de.hts.tournament.Tournament.validWorldUrl(worldUrl()) != null);
        worldOpen.managedProperty().bind(worldOpen.visibleProperty());
        javafx.scene.layout.VBox worldBox = new javafx.scene.layout.VBox(4, worldField, worldHint, worldWarn, worldOpen);
        Label worldLabel = new Label("Turnierwelt-Link:");
        grid.addRow(9, worldLabel, worldBox);
        untilField.setPromptText("z. B. 10:30");
        untilField.setPrefColumnCount(6);
        untilField.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        javafx.scene.layout.HBox untilBox = new javafx.scene.layout.HBox(8, untilField,
                hintLabel("optional – Uhrzeit am Turniertag, bis zu der die Online-Anmeldung über den QR-Code möglich ist (einschalten unter „Live im Internet“)"));
        untilBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.addRow(10, new Label("Online-Anmeldung bis:"), untilBox);
        grid.add(error, 0, 11, 2, 1);
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
            } else if (!untilField.getText().isBlank() && checkinUntil() == null) {
                error.setText("Bitte die Uhrzeit der Online-Anmeldung als SS:MM eingeben, z. B. 10:30 (oder leer lassen).");
                e.consume();
            } else if (!worldUrl().isEmpty() && de.hts.tournament.Tournament.validWorldUrl(worldUrl()) == null) {
                error.setText("Der Turnierwelt-Link muss eine vollständige Adresse mit https:// sein (oder leer bleiben).");
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
