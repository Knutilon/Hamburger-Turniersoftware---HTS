/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dunkelblaue Kopfleiste: HTS-Zeichen links, runde Icon-Knöpfe rechts, darunter das waagerechte Menü.
 */
public final class AppHeader {

    private final VBox root = new VBox();
    private final javafx.scene.layout.FlowPane menu = new javafx.scene.layout.FlowPane(6, 4);   // 0.36: bricht bei schmalen Fenstern um
    private final HBox tools = new HBox(10);
    private final Map<String, Button> items = new LinkedHashMap<>();
    private final Label context = new Label();

    /** Das HTS-Logo (für Fenster-Symbole und Kopfleiste). */
    public static javafx.scene.image.Image logo(double size) {
        return new javafx.scene.image.Image(AppHeader.class.getResourceAsStream("/de/hts/ui/hts-logo.png"), size, size, true, true);
    }

    public AppHeader() {
        // HTS-Logo + Schriftzug, daneben das geöffnete Turnier
        javafx.scene.image.ImageView img = new javafx.scene.image.ImageView(logo(96));
        img.setFitWidth(44);
        img.setFitHeight(44);
        img.setSmooth(true);
        Label sub = new Label("Hamburger Turniersoftware");
        sub.getStyleClass().add("logo-sub");
        context.getStyleClass().add("header-context");
        context.managedProperty().bind(context.visibleProperty());
        context.visibleProperty().bind(context.textProperty().isNotEmpty());
        HBox logo = new HBox(14, img, sub, context);
        logo.setAlignment(Pos.CENTER_LEFT);
        // 0.36: schmale Fenster – Schriftzug ausblenden, Turniername bricht um (statt „…“)
        sub.managedProperty().bind(sub.visibleProperty());
        sub.visibleProperty().bind(root.widthProperty().greaterThan(1200));
        context.setWrapText(true);
        context.setMinWidth(0);
        logo.setMinWidth(0);
        HBox.setHgrow(logo, Priority.ALWAYS);
        HBox.setHgrow(context, Priority.SOMETIMES);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        tools.setAlignment(Pos.CENTER_RIGHT);
        HBox top = new HBox(logo, spacer, tools);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("header-top");

        menu.setAlignment(Pos.CENTER_LEFT);
        menu.getStyleClass().add("header-menu");

        root.getChildren().addAll(top, menu);
        root.getStyleClass().add("app-header");
    }

    public Node getRoot() {
        return root;
    }

    /** Fügt einen Menüpunkt hinzu. */
    public Button addMenuItem(String key, String text, Node icon, Runnable action) {
        Button b = new Button(text, icon);
        b.getStyleClass().add("menu-item");
        b.setOnAction(e -> action.run());
        items.put(key, b);
        menu.getChildren().add(b);
        return b;
    }

    /** Text neben dem Logo, z. B. das geöffnete Turnier. */
    public Label getContextLabel() {
        return context;
    }

    /** Markiert den aktiven Menüpunkt. */
    public void select(String key) {
        items.forEach((k, b) -> {
            b.getStyleClass().remove("active");
            if (k.equals(key)) b.getStyleClass().add("active");
        });
    }

    /** Runder türkiser Icon-Knopf oben rechts. */
    public Button addTool(String iconPath, String tooltip, Runnable action) {
        Button b = new Button("", Icons.of(iconPath, 20));
        b.getStyleClass().add("round-tool");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        tools.getChildren().add(b);
        return b;
    }

    /** Beliebiges Element rechts oben (z. B. Speicherstatus). */
    public void addToolNode(Node n) {
        tools.getChildren().add(0, n);
    }
}
