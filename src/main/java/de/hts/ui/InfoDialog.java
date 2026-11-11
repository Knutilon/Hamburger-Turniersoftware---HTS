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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Info-Fenster (Knopf „(i)“ rechts oben): Programm, Version, Signatur, Lizenz (AGPL 3.0, seit 0.42), Links zur Feature- und
 * Bug-Sammlung auf GitHub und Hinweise zu den mitgelieferten Bibliotheken.
 */
public final class InfoDialog extends Dialog<ButtonType> {

    static final String FEATURES = HtsApp.REPO_URL + "/issues?q=is%3Aissue+label%3AFeature";
    static final String NEW_FEATURE = HtsApp.REPO_URL + "/issues/new?labels=Feature";
    static final String BUGS = HtsApp.REPO_URL + "/issues?q=is%3Aissue+label%3Abug";
    static final String NEW_BUG = HtsApp.REPO_URL + "/issues/new?labels=bug";
    static final String LICENSE = HtsApp.REPO_URL + "/blob/main/LICENSE";

    /** 0.42 (#73): Lizenzhinweis – derselbe Text steht in der Datei NOTICE und oben in der README. */
    public static final String NOTICE = """
            HTS - Hamburger Turniersoftware is an application for executing tournaments
            Copyright (C) 2026  Richard Johan Frederich Michaelis, Hamburg, Germany

            This program is free software: you can redistribute it and/or modify
            it under the terms of the GNU Affero General Public License as
            published by the Free Software Foundation, either version 3 of the
            License, or (at your option) any later version.

            This program is distributed in the hope that it will be useful,
            but WITHOUT ANY WARRANTY; without even the implied warranty of
            MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
            GNU Affero General Public License for more details.

            You should have received a copy of the GNU Affero General Public License
            along with this program.  If not, see <https://www.gnu.org/licenses/>.
            """;

    /** Den Info-Knopf als letzten (rechten) Knopf in die Kopfleiste setzen. */
    public static void install(AppHeader header, java.util.function.Supplier<Window> owner) {
        header.addTool(Icons.INFO, "Info: Version, Lizenz, Feature- und Bug-Sammlung", () -> new InfoDialog(owner.get()).showAndWait());
    }

    public InfoDialog(Window owner) {
        initOwner(owner);
        setTitle("Info – " + HtsApp.SHORT_NAME);
        setHeaderText(null);

        ImageView logo = new ImageView(AppHeader.logo(64));
        Label name = new Label(HtsApp.SHORT_NAME + " – " + HtsApp.NAME);
        name.getStyleClass().add("round-title");
        Label version = new Label("Version " + HtsApp.VERSION);
        version.getStyleClass().add("hint");
        Label signature = new Label(HtsApp.AUTHOR);
        signature.getStyleClass().add("info-signature");
        VBox head = new VBox(2, name, version, signature);
        HBox top = new HBox(14, logo, head);
        top.setAlignment(Pos.CENTER_LEFT);

        Label license = new Label("Copyright © 2026 " + HtsApp.AUTHOR + ". HTS ist freie Software (Open Source) unter "
                + "der GNU Affero General Public License v3.0 oder später (AGPL-3.0-or-later): Sie dürfen HTS nutzen, "
                + "verändern und weitergeben. Wer eine veränderte Fassung weitergibt oder anderen über ein Netzwerk zur "
                + "Nutzung anbietet (z. B. die Handy-Seiten), muss ihnen den Quellcode ebenfalls unter der AGPL zugänglich "
                + "machen. HTS wird ohne jede Gewährleistung bereitgestellt.");
        license.setWrapText(true);
        Label notice = new Label(NOTICE.strip());
        notice.getStyleClass().add("hint");
        notice.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");
        notice.setMinHeight(Region.USE_PREF_SIZE);
        Hyperlink licenseLink = link("Lizenztext (AGPL-3.0)", LICENSE);
        Hyperlink source = link("Quellcode auf GitHub", HtsApp.REPO_URL);

        Label feedbackHead = new Label("Mitmachen");
        feedbackHead.getStyleClass().add("class-name");
        VBox feedback = new VBox(2,
                new HBox(10, link("Feature-Sammlung (Wünsche ansehen)", FEATURES), link("neuen Wunsch melden", NEW_FEATURE)),
                new HBox(10, link("Bug-Sammlung (Fehler ansehen)", BUGS), link("neuen Fehler melden", NEW_BUG)));

        Label libsHead = new Label("Mitgelieferte Bibliotheken");
        libsHead.getStyleClass().add("class-name");
        Label libs = new Label("• Apache PDFBox 3.0.8 – Apache License 2.0\n"
                + "• JavaFX 21.0.8 (OpenJFX) – GNU GPL v2 mit Classpath Exception\n"
                + "• Java-Laufzeit Eclipse Temurin 21.0.13 – GNU GPL v2 mit Classpath Exception\n"
                + "Die Lizenztexte liegen im Programmordner unter „Lizenzen“ (Java-Laufzeit zusätzlich unter „jre\\legal“).");
        libs.getStyleClass().add("hint");
        libs.setWrapText(true);

        VBox content = new VBox(12, top, license, notice, new HBox(10, licenseLink, source), feedbackHead, feedback, libsHead, libs);
        content.setPadding(new Insets(8, 6, 4, 6));
        content.setPrefWidth(560);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType("Schließen", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
        DialogSizing.fit(this);
    }

    private static Hyperlink link(String text, String url) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> HtsApp.openLink(url));
        h.setTooltip(new javafx.scene.control.Tooltip(url));
        h.setPadding(Insets.EMPTY);
        return h;
    }
}
