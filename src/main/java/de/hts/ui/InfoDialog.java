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
import javafx.scene.control.Button;
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

    /** 1.4.0 (#109): „Danke an:“ – Wortlaut wie vom Nutzer vorgegeben. */
    static final java.util.List<String> THANKS = java.util.List.of(
            "Alexander Zappe (NSR und Verbandsorga) – Hamburg",
            "Felix Merkel (Softwareinputs und Verbandsorga) – Hamburg",
            "Roman Diewald (NSR) – Hamburg",
            "David Schnell (Geschäftsführer HaTTV) – Hamburg");

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
        // 1.10.0 (#173): Kopf und Hinweise umbrechen statt rechts abgeschnitten zu werden
        for (Label l : new Label[]{name, version, signature}) {
            l.setWrapText(true);
            l.setMinHeight(Region.USE_PREF_SIZE);
        }
        VBox head = new VBox(2, name, version, signature);
        head.setMinWidth(0);
        HBox.setHgrow(head, javafx.scene.layout.Priority.ALWAYS);
        HBox top = new HBox(14, logo, head);
        top.setAlignment(Pos.CENTER_LEFT);

        Label license = new Label("Copyright © 2026 " + HtsApp.AUTHOR + ". HTS ist freie Software (Open Source) unter "
                + "der GNU Affero General Public License v3.0 oder später (AGPL-3.0-or-later): Sie dürfen HTS nutzen, "
                + "verändern und weitergeben. Wer eine veränderte Fassung weitergibt oder anderen über ein Netzwerk zur "
                + "Nutzung anbietet (z. B. die Live-Seiten im Internet), muss ihnen den Quellcode ebenfalls unter der AGPL zugänglich "
                + "machen. HTS wird ohne jede Gewährleistung bereitgestellt.");
        license.setWrapText(true);
        license.setMinHeight(Region.USE_PREF_SIZE);
        Label notice = new Label(NOTICE.strip());
        notice.getStyleClass().add("hint");
        notice.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");
        notice.setMinHeight(Region.USE_PREF_SIZE);
        notice.setWrapText(true);
        Hyperlink licenseLink = link("Lizenztext (AGPL-3.0)", LICENSE);
        Hyperlink source = link("Quellcode auf GitHub", HtsApp.REPO_URL);

        Label feedbackHead = new Label("Mitmachen");
        feedbackHead.getStyleClass().add("class-name");
        VBox feedback = new VBox(2,
                flow(link("Feature-Sammlung (Wünsche ansehen)", FEATURES), link("neuen Wunsch melden", NEW_FEATURE)),
                flow(link("Bug-Sammlung (Fehler ansehen)", BUGS), link("neuen Fehler melden", NEW_BUG)));

        // 1.4.0 (#109): Danksagung
        Label thanksHead = new Label("Danke an:");
        thanksHead.getStyleClass().add("class-name");
        VBox thanks = new VBox(2);
        for (String line : THANKS) {
            Label l = new Label("• " + line);
            l.setWrapText(true);
            thanks.getChildren().add(l);
        }

        Label libsHead = new Label("Mitgelieferte Bibliotheken");
        libsHead.getStyleClass().add("class-name");
        Label libs = new Label("• Apache PDFBox 3.0.8 – Apache License 2.0\n"
                + "• JSVG 2.2.0 (SVG-Logos) – MIT-Lizenz\n"
                + "• JavaFX 21.0.8 (OpenJFX) – GNU GPL v2 mit Classpath Exception\n"
                + "• Java-Laufzeit Eclipse Temurin 21.0.13 – GNU GPL v2 mit Classpath Exception\n"
                + "• Schrift Comic Neue (für Urkunden) – SIL Open Font License 1.1\n"
                + "Die Lizenztexte liegen im Programmordner unter „Lizenzen“ (Java-Laufzeit zusätzlich unter „jre\\legal“).");
        libs.getStyleClass().add("hint");
        libs.setWrapText(true);

        // 1.10.0 (#177): „Bedienung“ und „Protokoll für die Fehlersuche“ stehen jetzt im Reiter „Programm“
        VBox content = new VBox(12, top, license, notice, flow(licenseLink, source), feedbackHead, feedback, thanksHead, thanks, libsHead, libs);
        content.setPadding(new Insets(8, 6, 4, 6));
        content.setPrefWidth(560);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType("Schließen", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
        DialogSizing.fit(this);
    }

    private static javafx.scene.layout.FlowPane flow(javafx.scene.Node... nodes) {
        return new javafx.scene.layout.FlowPane(10, 2, nodes);
    }

    private static Hyperlink link(String text, String url) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> HtsApp.openLink(url));
        h.setTooltip(new javafx.scene.control.Tooltip(url));
        h.setPadding(Insets.EMPTY);
        return h;
    }
}
