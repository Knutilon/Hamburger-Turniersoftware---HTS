/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.HtsApp;
import de.hts.Log;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;

/** 1.7.0 (#140): Hinweis bei unerwarteten Fehlern, Fehlerbericht (ZIP der Protokolle) und Protokollordner. */
public final class ErrorReport {

    private ErrorReport() {
    }

    /** „Es ist ein Fehler aufgetreten“ mit „Fehlerbericht speichern“ und „Weiter“. */
    public static void showUnexpected(Window owner, Throwable ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(owner);
        a.setTitle("Es ist ein Fehler aufgetreten");
        a.setHeaderText("Es ist ein Fehler aufgetreten");
        String what = ex.getClass().getSimpleName() + (ex.getMessage() == null ? "" : ": " + ex.getMessage());
        Label text = new Label("Das Turnier wurde vorsorglich gespeichert. Sie können normal weiterarbeiten.\n\n"
                + "Technische Angabe: " + what + "\n\n"
                + "Hilfreich für die Fehlersuche: „Fehlerbericht speichern“ und die Datei an ein GitHub-Issue anhängen "
                + "(enthält nur technische Angaben, keine Namen oder Ergebnisse).");
        text.setWrapText(true);
        text.setMaxWidth(520);
        text.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        a.getDialogPane().setContent(text);
        ButtonType report = new ButtonType("Fehlerbericht speichern", ButtonBar.ButtonData.LEFT);
        ButtonType cont = new ButtonType("Weiter", ButtonBar.ButtonData.OK_DONE);
        a.getButtonTypes().setAll(report, cont);
        DialogSizing.fit(a);
        if (a.showAndWait().orElse(cont) == report) saveReport(owner);
    }

    /** Fehlerbericht als ZIP speichern (Dateiauswahl). */
    public static void saveReport(Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Fehlerbericht speichern");
        fc.setInitialFileName("HTS-Fehlerbericht-" + LocalDate.now() + ".zip");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP-Archiv", "*.zip"));
        File f = fc.showSaveDialog(owner);
        if (f == null) return;
        try (OutputStream os = Files.newOutputStream(f.toPath())) {
            Log.writeReport(os, 7);
            Log.info("Fehlerbericht gespeichert");
            info(owner, "Fehlerbericht gespeichert", "Gespeichert unter:\n" + f.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            Log.error("Fehlerbericht konnte nicht gespeichert werden", e);
            info(owner, "Fehlerbericht", "Der Fehlerbericht konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /** Protokollordner im Dateimanager öffnen. */
    public static void openFolder(Window owner) {
        if (Log.directory() == null) {
            info(owner, "Protokoll", "Der Protokollordner ist nicht verfügbar.");
            return;
        }
        HtsApp.openLink(Log.directory().toUri().toString());
    }

    private static void info(Window owner, String title, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(null);
        DialogSizing.fit(a);
        a.showAndWait();
    }
}
