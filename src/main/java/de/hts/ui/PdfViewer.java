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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/** Eingebauter PDF-Viewer: Seitenansicht mit Zoom, Speichern als PDF und Drucken (Druckdialog). */
final class PdfViewer {

    private static final Preferences PREFS = Preferences.userNodeForPackage(PdfViewer.class);
    private static final float RENDER_DPI = 150f;

    private PdfViewer() {
    }

    /** Zeigt das Dokument in einem eigenen Fenster; es wird beim Schließen freigegeben. */
    static void show(Window owner, de.hts.tournament.Tournament t, String title, PDDocument doc, String fileName, Consumer<String> status) {
        byte[] bytes;
        List<WritableImage> pages = new ArrayList<>();
        List<Float> widthsPt = new ArrayList<>();
        try (doc) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            bytes = out.toByteArray();
            PDFRenderer r = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                pages.add(toFx(r.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB)));
                widthsPt.add(doc.getPage(i).getMediaBox().getWidth());
            }
        } catch (IOException e) {
            status.accept("PDF konnte nicht erstellt werden: " + e.getMessage());
            return;
        }

        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(title + " – PDF");

        VBox pageBox = new VBox(18);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(20));
        pageBox.setStyle("-fx-background-color: #5f6b7a;");
        List<ImageView> views = new ArrayList<>();
        for (WritableImage img : pages) {
            ImageView v = new ImageView(img);
            v.setPreserveRatio(true);
            v.setSmooth(true);
            v.setEffect(new DropShadow(12, javafx.scene.paint.Color.rgb(0, 0, 0, 0.4)));
            views.add(v);
            pageBox.getChildren().add(v);
        }
        double[] zoom = {widthsPt.isEmpty() || widthsPt.get(0) > 400 ? 1.25 : 2.0};
        Label zoomLabel = new Label();
        Runnable applyZoom = () -> {
            for (int i = 0; i < views.size(); i++) views.get(i).setFitWidth(widthsPt.get(i) * zoom[0]);
            zoomLabel.setText(Math.round(zoom[0] / 1.333 * 100) + " %");
        };
        applyZoom.run();

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #5f6b7a; -fx-background-color: #5f6b7a;");

        Button minus = new Button("−");
        Button plus = new Button("+");
        minus.getStyleClass().add("pill");
        plus.getStyleClass().add("pill");
        minus.setOnAction(e -> {
            zoom[0] = Math.max(0.5, zoom[0] / 1.2);
            applyZoom.run();
        });
        plus.setOnAction(e -> {
            zoom[0] = Math.min(4, zoom[0] * 1.2);
            applyZoom.run();
        });
        Button save = new Button("Speichern", Icons.of(Icons.EDIT, 14));
        save.getStyleClass().add("pill");
        save.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("PDF speichern");
            fc.setInitialFileName(fileName);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dokument", "*.pdf"));
            // Standard: „PDF Export“ im Turnierordner (bzw. der zuletzt für dieses Turnier gewählte Ordner)
            if (t != null) {
                fc.setInitialDirectory(de.hts.persistence.StorageLocations.ensure(de.hts.persistence.StorageLocations.pdfDir(t)));
            } else {
                String dir = PREFS.get("lastPdfDir", null);
                if (dir != null && new File(dir).isDirectory()) fc.setInitialDirectory(new File(dir));
            }
            File f = fc.showSaveDialog(stage);
            if (f == null) return;
            if (!f.getName().toLowerCase().endsWith(".pdf")) f = new File(f.getParentFile(), f.getName() + ".pdf");
            try {
                java.nio.file.Files.write(f.toPath(), bytes);
                PREFS.put("lastPdfDir", f.getParent());
                if (t != null) de.hts.persistence.StorageLocations.rememberPdfDir(t, f.getParentFile().toPath());
                status.accept("PDF gespeichert: " + f.getAbsolutePath());
            } catch (IOException ex) {
                status.accept("PDF konnte nicht gespeichert werden: " + ex.getMessage());
            }
        });
        Button print = new Button("Drucken", Icons.of(Icons.START, 14));
        print.getStyleClass().add("navy-pill");
        print.setOnAction(e -> {
            try {
                Reports.printWithDialog(Loader.loadPDF(bytes), title, status);
            } catch (IOException ex) {
                status.accept("Drucken fehlgeschlagen: " + ex.getMessage());
            }
        });
        Label name = new Label(title + "  ·  " + pages.size() + (pages.size() == 1 ? " Seite" : " Seiten"));
        name.getStyleClass().add("class-name");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, name, sp, minus, zoomLabel, plus, new Label("  "), save, print);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #d0d7e1 transparent;");

        BorderPane root = new BorderPane(scroll);
        root.setTop(bar);
        Scene scene = new Scene(root, widthsPt.isEmpty() || widthsPt.get(0) > 400 ? 900 : 560, 860);
        if (owner != null && owner.getScene() != null) scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+P"), print::fire);
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+S"), save::fire);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        stage.setScene(scene);
        stage.show();
    }

    private static WritableImage toFx(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        WritableImage out = new WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        return out;
    }
}
