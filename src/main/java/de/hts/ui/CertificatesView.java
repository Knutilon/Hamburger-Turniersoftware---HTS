/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.report.CertificateReport;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 1.4.0 (#113): Reiter „Urkunden drucken“ – Klasse und Plätze wählen, Name und Verein nur für den Druck anpassen,
 * Urkundentexte je Turnier einstellen, Vorschau mit Blättern, als PDF speichern oder drucken.
 */
final class CertificatesView {

    private static final String ALL = "Alle Klassen";

    private final Tournament tournament;
    private final Consumer<String> status;
    private final VBox content = new VBox(14);
    private final javafx.scene.control.ScrollPane root = new javafx.scene.control.ScrollPane(content);

    private final ComboBox<String> scope = new ComboBox<>();
    private final ToggleGroup mode = new ToggleGroup();
    private final RadioButton top3 = new RadioButton("Plätze 1 bis 3");
    private final RadioButton allPlaces = new RadioButton("Alle Plätze");
    private final RadioButton custom = new RadioButton("Eigene Auswahl");
    private final Spinner<Integer> from = new Spinner<>(1, 99, 1);
    private final Spinner<Integer> to = new Spinner<>(1, 99, 3);
    private final HBox range = new HBox(6);
    private final Label counter = new Label();
    private final GridPane list = new GridPane();
    private final ImageView preview = new ImageView();
    private final StackPane previewBox = new StackPane();
    private final Label previewHint = new Label("Keine Urkunde zum Druck ausgewählt.\nSetze Häkchen in der Liste oder wähle Plätze aus.");
    private final Label previewNr = new Label();
    private final Button prev = new Button("‹");
    private final Button next = new Button("›");
    private final Button savePdf = new Button("Als PDF speichern");
    private final Button print = new Button("Drucken", Icons.of(Icons.START, 14));

    private List<Tournament.CertEntry> all = List.of();
    private final Set<String> selected = new LinkedHashSet<>();
    private int previewIndex;
    private boolean adjusting;

    CertificatesView(Tournament tournament, Consumer<String> status) {
        this.tournament = tournament;
        this.status = status;

        content.setPadding(new Insets(0, 40, 24, 40));
        root.setFitToWidth(true);                    // eigene Scrollleiste wie die übrigen Seiten
        root.getStyleClass().add("page-scroll");

        Label intro = new Label("Name, Verein, Klasse, Platz und Datum setzt HTS aus den Ergebnissen ein.");
        intro.getStyleClass().add("hint");
        intro.setWrapText(true);

        // Auswahl
        Label scopeLbl = new Label("Klasse:");
        scopeLbl.getStyleClass().add("class-name");
        scope.setMinWidth(Region.USE_PREF_SIZE);
        scope.valueProperty().addListener((o, a, b) -> {
            if (!adjusting && b != null) {
                applyMode();
                rebuildList();
            }
        });
        for (RadioButton rb : List.of(top3, allPlaces, custom)) {
            rb.setStyle("-fx-text-fill: #1f2937;");
            rb.setMinWidth(Region.USE_PREF_SIZE);
        }
        top3.setToggleGroup(mode);
        allPlaces.setToggleGroup(mode);
        custom.setToggleGroup(mode);
        mode.selectedToggleProperty().addListener((o, a, b) -> {
            range.setVisible(b == custom);
            range.setManaged(b == custom);
            if (!adjusting) {
                applyMode();
                rebuildList();
            }
        });
        from.setEditable(true);
        to.setEditable(true);
        from.setPrefWidth(80);
        to.setPrefWidth(80);
        from.valueProperty().addListener((o, a, b) -> rangeChanged());
        to.valueProperty().addListener((o, a, b) -> rangeChanged());
        range.setAlignment(Pos.CENTER_LEFT);
        range.getChildren().addAll(new Label("Platz"), from, new Label("bis"), to);
        range.setVisible(false);
        range.setManaged(false);
        FlowPane choose = new FlowPane(14, 8, scopeLbl, scope, top3, allPlaces, custom, range);
        choose.setAlignment(Pos.CENTER_LEFT);

        counter.getStyleClass().add("class-name");

        // Texte
        VBox texts = textsBox();

        // Liste
        list.setHgap(10);
        list.setVgap(6);
        list.getStyleClass().add("card");
        list.setPadding(new Insets(12));
        ColumnConstraints c0 = new ColumnConstraints(), c1 = new ColumnConstraints(), c2 = new ColumnConstraints(),
                c3 = new ColumnConstraints(), c4 = new ColumnConstraints(), c5 = new ColumnConstraints();
        for (ColumnConstraints c : List.of(c0, c1, c2, c5)) c.setHgrow(Priority.NEVER);
        c3.setHgrow(Priority.ALWAYS);
        c4.setHgrow(Priority.ALWAYS);
        list.getColumnConstraints().addAll(c0, c1, c2, c3, c4, c5);

        // Vorschau
        preview.setPreserveRatio(true);
        preview.setFitWidth(300);
        preview.setSmooth(true);
        preview.setEffect(new DropShadow(10, javafx.scene.paint.Color.rgb(0, 0, 0, 0.3)));
        previewHint.setWrapText(true);
        previewHint.getStyleClass().add("hint");
        previewHint.setMaxWidth(260);
        previewHint.setStyle("-fx-text-alignment: center;");
        previewBox.setMinSize(300, 424);
        previewBox.setPrefSize(300, 424);
        previewBox.setMaxSize(300, 424);
        previewBox.setStyle("-fx-background-color: white; -fx-border-color: #cfd6e0;");
        previewBox.getChildren().addAll(preview, previewHint);
        prev.getStyleClass().add("pill");
        next.getStyleClass().add("pill");
        prev.setOnAction(e -> showPreview(previewIndex - 1));
        next.setOnAction(e -> showPreview(previewIndex + 1));
        previewNr.getStyleClass().add("class-name");
        HBox pager = new HBox(10, prev, previewNr, next);
        pager.setAlignment(Pos.CENTER);
        savePdf.getStyleClass().add("pill");
        print.getStyleClass().add("navy-pill");
        savePdf.setMinWidth(Region.USE_PREF_SIZE);
        print.setMinWidth(Region.USE_PREF_SIZE);
        savePdf.setOnAction(e -> savePdf());
        print.setOnAction(e -> printSelected());
        HBox buttons = new HBox(8, savePdf, print);
        buttons.setAlignment(Pos.CENTER);
        VBox right = new VBox(10, new Label("Vorschau"), previewBox, pager, buttons);
        right.getChildren().get(0).getStyleClass().add("class-name");
        right.setAlignment(Pos.TOP_CENTER);
        right.setMinWidth(Region.USE_PREF_SIZE);

        // oben Urkundentexte und Vorschau nebeneinander (schmal: Vorschau darunter), darunter die Liste
        FlowPane top = new FlowPane(18, 18, texts, right);
        top.setAlignment(Pos.TOP_LEFT);
        top.setRowValignment(javafx.geometry.VPos.TOP);
        list.setMaxWidth(Region.USE_PREF_SIZE);
        content.getChildren().addAll(intro, choose, counter, top, list);
    }

    javafx.scene.control.ScrollPane getRoot() {
        return root;
    }

    /** Inhalt ohne Scrollbereich (für Bildschirmfotos der ganzen Seite). */
    VBox getContent() {
        return content;
    }

    // --- Texte -------------------------------------------------------------------------------------

    private VBox textsBox() {
        Label head = new Label("Urkundentexte");
        head.getStyleClass().add("class-name");
        TextField title = field(tournament.getCertTitle());
        TextField place = field(tournament.getCertPlace());
        TextField signer = field(tournament.getCertSigner());
        TextField host = field(tournament.getCertHost());
        Runnable commit = () -> {
            tournament.setCertTexts(title.getText(), place.getText(), signer.getText(), host.getText());
            showPreview(previewIndex);
        };
        for (TextField f : List.of(title, place, signer, host)) f.textProperty().addListener((o, a, b) -> commit.run());
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(6);
        String[] names = {"Überschrift", "Ort", "Unterschrift rechts", "Ausrichter unten"};
        TextField[] fs = {title, place, signer, host};
        for (int i = 0; i < 4; i++) {
            Label l = new Label(names[i]);
            l.setMinWidth(Region.USE_PREF_SIZE);
            g.add(l, 0, i);
            g.add(fs[i], 1, i);
        }
        ColumnConstraints a = new ColumnConstraints(), b = new ColumnConstraints();
        a.setMinWidth(Region.USE_PREF_SIZE);
        b.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(a, b);
        Label hint = new Label("Bleibt ein Feld leer, bleibt die Stelle auf der Urkunde frei (bei „Ort“ entfällt auch das Komma).");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setPrefWidth(1);
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        CheckBox top = new CheckBox("Oben 5 cm frei lassen (für vorgedrucktes Briefpapier)");
        top.setSelected(tournament.isCertLeaveTop());
        top.selectedProperty().addListener((o, x, y) -> {
            tournament.setCertLeaveTop(y);
            showPreview(previewIndex);
        });
        top.setWrapText(true);
        VBox box = new VBox(8, head, g, hint, top);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        return box;
    }

    private static TextField field(String v) {
        TextField f = new TextField(v);
        f.setPrefColumnCount(26);
        f.setMaxWidth(380);
        return f;
    }

    // --- Daten und Auswahl ---------------------------------------------------------------------------

    /** Liest die Platzierungen neu (Reiter geöffnet, Ergebnisse geändert). */
    void refresh() {
        all = tournament.certificateEntries();
        adjusting = true;
        String keep = scope.getValue();
        List<String> classes = new ArrayList<>();
        classes.add(ALL);
        for (de.hts.tournament.PlayingClass c : tournament.rootClasses()) {
            if (c.isDrawn()) classes.add(c.getName());
        }
        scope.getItems().setAll(classes);
        scope.setValue(keep != null && classes.contains(keep) ? keep : ALL);
        if (mode.getSelectedToggle() == null) mode.selectToggle(custom);
        range.setVisible(mode.getSelectedToggle() == custom);
        range.setManaged(mode.getSelectedToggle() == custom);
        adjusting = false;
        selected.retainAll(all.stream().map(Tournament.CertEntry::key).toList());
        rebuildList();
    }

    /** „Zur Druckansicht“: Plätze 1 bis 3 sind vorausgewählt. */
    void selectTop3() {
        adjusting = true;
        scope.setValue(ALL);
        mode.selectToggle(top3);
        adjusting = false;
        selected.clear();
        applyMode();
        rebuildList();
    }

    /** „Abbrechen“: kein Urkundendruck ausgewählt. */
    void selectNone() {
        adjusting = true;
        scope.setValue(ALL);
        mode.selectToggle(custom);
        adjusting = false;
        selected.clear();
        rebuildList();
    }

    private List<Tournament.CertEntry> inScope() {
        String s = scope.getValue();
        if (s == null || ALL.equals(s)) return all;
        return all.stream().filter(e -> e.className().equals(s)).toList();
    }

    private void rangeChanged() {
        if (adjusting || mode.getSelectedToggle() != custom) return;
        applyMode();
        rebuildList();
    }

    /** Setzt die Häkchen der Klasse(n) im Gültigkeitsbereich nach der gewählten Plätze-Einstellung. */
    private void applyMode() {
        Toggle t = mode.getSelectedToggle();
        if (t == null || adjusting) return;
        int lo = 1, hi = Integer.MAX_VALUE;
        if (t == top3) hi = 3;
        else if (t == custom) {
            lo = value(from, 1);
            hi = Math.max(lo, value(to, lo));
        }
        for (Tournament.CertEntry e : inScope()) {
            if (e.place() >= lo && e.place() <= hi) selected.add(e.key());
            else selected.remove(e.key());
        }
    }

    private static int value(Spinner<Integer> s, int def) {
        try {
            Integer v = s.getValue();
            return v == null ? def : v;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private List<Tournament.CertEntry> selectedEntries() {
        return inScope().stream().filter(e -> selected.contains(e.key())).toList();
    }

    // --- Liste ----------------------------------------------------------------------------------------

    private void rebuildList() {
        list.getChildren().clear();
        String[] heads = {"", "Klasse", "Platz", "Name (änderbar)", "Verein (änderbar)", ""};
        for (int i = 0; i < heads.length; i++) {
            Label l = new Label(heads[i]);
            l.getStyleClass().add("class-name");
            l.setMinWidth(Region.USE_PREF_SIZE);
            list.add(l, i, 0);
        }
        int row = 1;
        for (Tournament.CertEntry e : inScope()) {
            addRow(e, row++);
        }
        Label note = new Label("Namen und Vereine lassen sich hier für den Druck anpassen (z. B. ausgeschriebener Vereinsname). "
                + "Das gilt nur für die Urkunde: Teilnehmerdaten, Ergebnisse und die click-TT-Datei bleiben unverändert. "
                + "Mit einem Klick auf „nur für Druck geändert“ geht es zurück auf den Originalwert.");
        note.getStyleClass().add("hint");
        note.setWrapText(true);
        note.setMinHeight(Region.USE_PREF_SIZE);
        note.setPrefWidth(10);
        note.setMaxWidth(Double.MAX_VALUE);
        if (row > 1) list.add(note, 0, row + 1, 6, 1);
        if (row == 1) {
            Label none = new Label("Noch keine Platzierungen – die Urkunden erscheinen, sobald Klassen ausgelost sind.");
            none.getStyleClass().add("hint");
            list.add(none, 0, 1, 6, 1);
        }
        updateCounter();
        showPreview(previewIndex);
    }

    private void addRow(Tournament.CertEntry e, int row) {
        CheckBox cb = new CheckBox();
        cb.setSelected(selected.contains(e.key()));
        cb.selectedProperty().addListener((o, a, b) -> {
            if (b) selected.add(e.key());
            else selected.remove(e.key());
            if (!adjusting && mode.getSelectedToggle() != custom) {
                adjusting = true;
                mode.selectToggle(custom);
                adjusting = false;
            }
            updateCounter();
            showPreview(previewIndex);
        });
        Label cls = new Label(e.className());
        cls.setMinWidth(Region.USE_PREF_SIZE);
        Label place = new Label(e.place() + ".");
        place.setMinWidth(Region.USE_PREF_SIZE);
        TextField name = new TextField(e.name());
        TextField club = new TextField(e.club());
        name.setMaxWidth(Double.MAX_VALUE);
        club.setMaxWidth(Double.MAX_VALUE);
        name.setPrefColumnCount(16);
        club.setPrefColumnCount(18);
        Button changed = new Button("nur für Druck geändert");
        changed.getStyleClass().add("pill");
        changed.setMinWidth(Region.USE_PREF_SIZE);
        changed.setTooltip(new Tooltip("Original: " + e.originalName() + (e.originalClub().isBlank() ? "" : " · " + e.originalClub())
                + "\nKlick = Originalwerte wiederherstellen. Gilt nur für die Urkunde."));
        changed.setVisible(e.changed());
        changed.setManaged(e.changed());
        Runnable commit = () -> {
            tournament.setCertOverride(e, name.getText(), club.getText());
            all = tournament.certificateEntries();
            Tournament.CertEntry now = all.stream().filter(x -> x.key().equals(e.key())).findFirst().orElse(e);
            changed.setVisible(now.changed());
            changed.setManaged(now.changed());
            showPreview(previewIndex);
        };
        name.setOnAction(a -> commit.run());
        club.setOnAction(a -> commit.run());
        name.focusedProperty().addListener((o, a, b) -> {
            if (!b) commit.run();
        });
        club.focusedProperty().addListener((o, a, b) -> {
            if (!b) commit.run();
        });
        changed.setOnAction(a -> {
            name.setText(e.originalName());
            club.setText(e.originalClub());
            commit.run();
        });
        HBox.setHgrow(name, Priority.ALWAYS);
        list.add(cb, 0, row);
        list.add(cls, 1, row);
        list.add(place, 2, row);
        list.add(name, 3, row);
        list.add(club, 4, row);
        list.add(changed, 5, row);
    }

    private void updateCounter() {
        List<Tournament.CertEntry> scoped = inScope();
        long n = scoped.stream().filter(e -> selected.contains(e.key())).count();
        counter.setText(n + " von " + scoped.size() + (scoped.size() == 1 ? " Urkunde" : " Urkunden") + " ausgewählt");
        boolean none = n == 0;
        savePdf.setDisable(none);
        print.setDisable(none);
        Tooltip tip = none ? new Tooltip("Zuerst Urkunden auswählen") : null;
        savePdf.setTooltip(tip);
        print.setTooltip(tip);
    }

    // --- Vorschau -------------------------------------------------------------------------------------

    private void showPreview(int index) {
        List<Tournament.CertEntry> sel = selectedEntries();
        if (sel.isEmpty()) {
            preview.setImage(null);
            previewHint.setVisible(true);
            previewNr.setText("0 von 0");
            prev.setDisable(true);
            next.setDisable(true);
            previewIndex = 0;
            return;
        }
        previewIndex = Math.max(0, Math.min(index, sel.size() - 1));
        previewHint.setVisible(false);
        try (PDDocument doc = CertificateReport.certificates(tournament, List.of(sel.get(previewIndex)))) {
            BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(0, 100f, ImageType.RGB);
            preview.setImage(toFx(img));
        } catch (IOException ex) {
            status.accept("Vorschau nicht möglich: " + ex.getMessage());
        }
        previewNr.setText("Urkunde " + (previewIndex + 1) + " von " + sel.size());
        prev.setDisable(previewIndex == 0);
        next.setDisable(previewIndex >= sel.size() - 1);
    }

    private static WritableImage toFx(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        WritableImage out = new WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        return out;
    }

    // --- PDF und Druck --------------------------------------------------------------------------------

    private String jobName() {
        return "Urkunden " + (tournament.getName() == null ? "Turnier" : tournament.getName());
    }

    private void savePdf() {
        List<Tournament.CertEntry> sel = selectedEntries();
        if (sel.isEmpty()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Urkunden als PDF speichern");
        fc.setInitialFileName(jobName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dokument", "*.pdf"));
        fc.setInitialDirectory(de.hts.persistence.StorageLocations.ensure(de.hts.persistence.StorageLocations.pdfDir(tournament)));
        File f = fc.showSaveDialog(content.getScene() == null ? null : content.getScene().getWindow());
        if (f == null) return;
        if (!f.getName().toLowerCase().endsWith(".pdf")) f = new File(f.getParentFile(), f.getName() + ".pdf");
        try (PDDocument doc = CertificateReport.certificates(tournament, sel)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            java.nio.file.Files.write(f.toPath(), out.toByteArray());
            de.hts.persistence.StorageLocations.rememberPdfDir(tournament, f.getParentFile().toPath());
            status.accept("Urkunden gespeichert: " + f.getAbsolutePath());
        } catch (IOException ex) {
            status.accept("PDF konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private void printSelected() {
        List<Tournament.CertEntry> sel = selectedEntries();
        if (sel.isEmpty()) return;
        Reports.printWithDialog(CertificateReport.certificates(tournament, sel), jobName(), status);
    }
}
