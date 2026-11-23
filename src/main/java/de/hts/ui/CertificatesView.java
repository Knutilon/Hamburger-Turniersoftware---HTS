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
import javafx.scene.layout.FlowPane;
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
    private final RadioButton notPrinted = new RadioButton("Noch nicht gedruckte");   // 1.5.0 (#132)
    private final Spinner<Integer> from = new Spinner<>(1, 99, 1);
    private final Spinner<Integer> to = new Spinner<>(1, 99, 3);
    private final HBox range = new HBox(6);
    private final Label counter = new Label();
    /** 1.4.1: virtualisierte Tabelle statt einer Zeile mit Eingabefeldern je Urkunde (6000 Urkunden: Speicher). */
    private final javafx.scene.control.TableView<Tournament.CertEntry> table = new javafx.scene.control.TableView<>();
    private final VBox list = new VBox(8);
    private final Label none = new Label("Noch stehen keine Plätze fest – Urkunden erscheinen, sobald eine Phase abgeschlossen oder eine Klasse beendet ist.");
    private final ImageView preview = new ImageView();
    private final StackPane previewBox = new StackPane();
    private final Label previewHint = new Label("Keine Urkunde zum Druck ausgewählt.\nSetze Häkchen in der Liste oder wähle Plätze aus.");
    private final Label previewNr = new Label();
    private final Button prev = new Button("‹");
    private final Button next = new Button("›");
    private final Button savePdf = new Button("Als PDF speichern");
    private final Button print = new Button("Drucken", Icons.of(Icons.START, 14));

    /** 1.4.1: Suche in der Tabelle (filtert nur die Anzeige, nicht die Auswahl). */
    private final TextField search = new TextField();
    private final VBox leftCol = new VBox(14);
    private final VBox previewCard = new VBox(10);
    private final HBox body = new HBox(18);
    private Boolean wideLayout;

    private List<Tournament.CertEntry> all = List.of();
    private final Set<String> selected = new LinkedHashSet<>();
    private int previewIndex;
    private boolean adjusting;

    CertificatesView(Tournament tournament, Consumer<String> status) {
        this.tournament = tournament;
        this.status = status;

        content.setPadding(new Insets(0, 40, 24, 40));
        root.setFitToWidth(true);                    // eigene Scrollleiste wie die übrigen Seiten
        root.setFitToHeight(true);                   // breit: Tabelle über die volle Höhe
        root.getStyleClass().add("page-scroll");

        Label intro = new Label("Name, Verein, Klasse, Platz und Datum setzt HTS aus den Ergebnissen ein. Laufende Klassen: "
                + "nur die Plätze, die nach einer abgeschlossenen Phase schon feststehen.");
        intro.getStyleClass().add("hint");
        intro.setWrapText(true);
        intro.setMinHeight(Region.USE_PREF_SIZE);

        // --- links: Welche Urkunden? ---
        Label chooseHead = new Label("Welche Urkunden?");
        chooseHead.getStyleClass().add("class-name");
        Label scopeLbl = new Label("Klasse");
        scopeLbl.getStyleClass().add("hint");
        scope.setMaxWidth(Double.MAX_VALUE);
        scope.valueProperty().addListener((o, a, b) -> {
            if (!adjusting && b != null) {
                applyMode();
                rebuildList();
            }
        });
        for (RadioButton rb : List.of(top3, allPlaces, notPrinted, custom)) {
            rb.setStyle("-fx-text-fill: #1f2937;");
            rb.setMinWidth(Region.USE_PREF_SIZE);
        }
        top3.setToggleGroup(mode);
        allPlaces.setToggleGroup(mode);
        custom.setToggleGroup(mode);
        notPrinted.setToggleGroup(mode);
        mode.selectedToggleProperty().addListener((o, a, b) -> {
            range.setDisable(b != custom);
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
        range.setDisable(true);
        counter.getStyleClass().add("class-name");
        counter.setWrapText(true);
        VBox radios = new VBox(8, top3, allPlaces, notPrinted, custom);
        VBox chooseCard = new VBox(10, chooseHead, scopeLbl, scope, radios, range, new javafx.scene.control.Separator(), counter);
        chooseCard.getStyleClass().add("card");
        chooseCard.setPadding(new Insets(14, 16, 14, 16));
        VBox texts = textsBox();
        leftCol.getChildren().addAll(chooseCard, texts);
        leftCol.setPrefWidth(360);
        leftCol.setMinWidth(300);
        leftCol.setMaxWidth(360);

        // --- Mitte: Tabelle ---
        buildTable();
        list.getStyleClass().add("card");
        list.setPadding(new Insets(12));
        HBox.setHgrow(list, Priority.ALWAYS);
        list.setMinWidth(420);

        // --- rechts: Vorschau ---
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setEffect(new DropShadow(10, javafx.scene.paint.Color.rgb(0, 0, 0, 0.3)));
        previewHint.setWrapText(true);
        previewHint.getStyleClass().add("hint");
        previewHint.setStyle("-fx-text-alignment: center;");
        previewBox.setStyle("-fx-background-color: white; -fx-border-color: #cfd6e0;");
        previewBox.getChildren().addAll(preview, previewHint);
        prev.getStyleClass().add("pill");
        next.getStyleClass().add("pill");
        prev.setOnAction(e -> showPreview(previewIndex - 1));
        next.setOnAction(e -> showPreview(previewIndex + 1));
        previewNr.getStyleClass().add("class-name");
        previewNr.setMinWidth(Region.USE_PREF_SIZE);
        HBox pager = new HBox(10, prev, previewNr, next);
        pager.setAlignment(Pos.CENTER);
        savePdf.getStyleClass().add("pill");
        print.getStyleClass().add("navy-pill");
        savePdf.setMinWidth(Region.USE_PREF_SIZE);
        print.setMinWidth(Region.USE_PREF_SIZE);
        savePdf.setOnAction(e -> savePdf());
        print.setOnAction(e -> Ui.busy(content.getScene() == null ? null : content.getScene().getWindow(), "Urkunden werden erstellt",
                this::printSelected));
        FlowPane buttons = new FlowPane(8, 8, savePdf, print);
        buttons.setAlignment(Pos.CENTER);
        Label previewHead = new Label("Vorschau");
        previewHead.getStyleClass().add("class-name");
        previewCard.getChildren().addAll(previewHead, previewBox, pager, buttons);
        previewCard.getStyleClass().add("card");
        previewCard.setPadding(new Insets(14, 16, 14, 16));
        previewCard.setAlignment(Pos.TOP_CENTER);
        previewCard.setMaxHeight(Region.USE_PREF_SIZE);

        body.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);
        content.getChildren().addAll(intro, body);
        // breit: Einstellungen | Tabelle | Vorschau; schmal: Einstellungen + Vorschau links, Tabelle rechts
        content.widthProperty().addListener((o, a, b) -> layoutFor(b.doubleValue()));
        layoutFor(1600);
    }

    private void layoutFor(double width) {
        boolean wide = width >= 1420;
        if (wideLayout != null && wideLayout == wide) return;
        wideLayout = wide;
        double pw = wide ? 420 : 260;
        preview.setFitWidth(pw);
        previewBox.setMinSize(pw, pw * 1.414);
        previewBox.setPrefSize(pw, pw * 1.414);
        previewBox.setMaxSize(pw, pw * 1.414);
        previewHint.setMaxWidth(pw - 30);
        previewCard.setPrefWidth(pw + 32);
        previewCard.setMinWidth(pw + 32);
        previewCard.setMaxWidth(wide ? pw + 32 : Double.MAX_VALUE);
        leftCol.getChildren().remove(previewCard);
        if (wide) {
            body.getChildren().setAll(leftCol, list, previewCard);
        } else {
            leftCol.getChildren().add(previewCard);
            body.getChildren().setAll(leftCol, list);
        }
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
        VBox g = new VBox(8);
        String[] names = {"Überschrift", "Ort", "Unterschrift rechts", "Ausrichter unten"};
        TextField[] fs = {title, place, signer, host};
        for (int i = 0; i < 4; i++) {
            Label l = new Label(names[i]);
            l.getStyleClass().add("hint");
            fs[i].setMaxWidth(Double.MAX_VALUE);
            g.getChildren().add(new VBox(3, l, fs[i]));
        }
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
        // Häkchen ohne eigenen Text + umbrechendes Label daneben (CheckBox-Text würde sonst mit „…“ gekürzt)
        top.setText(null);
        Label topText = new Label("Oben 5 cm frei lassen (für vorgedrucktes Briefpapier)");
        topText.setWrapText(true);
        topText.setPrefWidth(10);
        topText.setMaxWidth(Double.MAX_VALUE);
        topText.setMinHeight(Region.USE_PREF_SIZE);
        topText.setOnMouseClicked(e -> top.setSelected(!top.isSelected()));
        HBox.setHgrow(topText, Priority.ALWAYS);
        HBox topRow = new HBox(6, top, topText);
        topRow.setAlignment(Pos.TOP_LEFT);
        hint.setPrefWidth(10);
        VBox box = new VBox(10, head, g, hint, topRow);
        box.setMinWidth(0);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.getStyleClass().add("card");
        return box;
    }

    private static TextField field(String v) {
        TextField f = new TextField(v);
        f.setPrefColumnCount(10);
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
        for (de.hts.tournament.PlayingClass c : tournament.certificateClasses()) classes.add(c.getName());
        scope.getItems().setAll(classes);
        scope.setValue(keep != null && classes.contains(keep) ? keep : ALL);
        if (mode.getSelectedToggle() == null) mode.selectToggle(custom);
        range.setVisible(mode.getSelectedToggle() == custom);
        range.setManaged(mode.getSelectedToggle() == custom);
        adjusting = false;
        selected.retainAll(all.stream().map(Tournament.CertEntry::key).toList());
        rebuildList();
    }

    /** „Zur Druckansicht“: Plätze 1 bis 3 der Klasse sind vorausgewählt ({@code null} = alle Klassen). */
    void selectTop3(String className) {
        refresh();
        adjusting = true;
        scope.setValue(className != null && scope.getItems().contains(className) ? className : ALL);
        mode.selectToggle(top3);
        adjusting = false;
        selected.clear();
        applyMode();
        rebuildList();
    }

    /** „Abbrechen“: kein Urkundendruck ausgewählt. */
    void selectNone(String className) {
        refresh();
        adjusting = true;
        scope.setValue(className != null && scope.getItems().contains(className) ? className : ALL);
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
        if (t == notPrinted) {
            for (Tournament.CertEntry e : inScope()) {
                if (!tournament.isCertPrinted(e.key())) selected.add(e.key());
                else selected.remove(e.key());
            }
            return;
        }
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
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase(java.util.Locale.GERMAN);
        table.getItems().setAll(q.isEmpty() ? inScope() : inScope().stream().filter(e -> (e.name() + " " + e.club() + " " + e.className())
                .toLowerCase(java.util.Locale.GERMAN).contains(q)).toList());
        Ui.fitColumns(table);
        boolean empty = inScope().isEmpty();
        none.setVisible(empty);
        none.setManaged(empty);
        table.setVisible(!empty);
        table.setManaged(!empty);
        updateCounter();
        showPreview(previewIndex);
    }

    private void buildTable() {
        table.setEditable(true);
        table.setFixedCellSize(32);
        table.setPrefHeight(32 * 12 + 34);
        table.setMinHeight(32 * 6 + 34);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label(""));
        javafx.scene.control.TableColumn<Tournament.CertEntry, Boolean> pick = new javafx.scene.control.TableColumn<>("Druck");
        pick.setSortable(false);
        pick.setCellValueFactory(c -> new javafx.beans.property.SimpleBooleanProperty(selected.contains(c.getValue().key())));
        pick.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    Tournament.CertEntry en = getTableRow() == null ? null : getTableRow().getItem();
                    if (en == null) return;
                    if (cb.isSelected()) selected.add(en.key());
                    else selected.remove(en.key());
                    if (!adjusting && mode.getSelectedToggle() != custom) {
                        adjusting = true;
                        mode.selectToggle(custom);
                        adjusting = false;
                    }
                    updateCounter();
                    showPreview(previewIndex);
                });
            }

            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                Tournament.CertEntry en = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (en == null) {
                    setGraphic(null);
                    return;
                }
                cb.setSelected(selected.contains(en.key()));
                setGraphic(cb);
            }
        });
        pick.getProperties().put("fit-header-only", true);
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> cls = new javafx.scene.control.TableColumn<>("Klasse");
        cls.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().className()));
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> place = new javafx.scene.control.TableColumn<>("Platz");
        place.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().place() + "."));
        place.setComparator(java.util.Comparator.comparingInt(x -> Integer.parseInt(x.replace(".", ""))));
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> name = new javafx.scene.control.TableColumn<>("Name (Doppelklick ändert)");
        name.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name()));
        name.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        name.setOnEditCommit(ev -> commit(ev.getRowValue(), ev.getNewValue(), ev.getRowValue().club()));
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> club = new javafx.scene.control.TableColumn<>("Verein (Doppelklick ändert)");
        club.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().club()));
        club.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        club.setOnEditCommit(ev -> commit(ev.getRowValue(), ev.getRowValue().name(), ev.getNewValue()));
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> printedCol = new javafx.scene.control.TableColumn<>("Gedruckt");
        printedCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(tournament.isCertPrinted(c.getValue().key()) ? "✓ gedruckt" : ""));
        javafx.scene.control.TableColumn<Tournament.CertEntry, String> mark = new javafx.scene.control.TableColumn<>("Druckanpassung");
        mark.setSortable(false);
        mark.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().changed() ? "nur für Druck geändert" : ""));
        mark.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final Button b = new Button("nur für Druck geändert");
            {
                b.getStyleClass().add("pill");
                b.setMinWidth(Region.USE_PREF_SIZE);
                b.setOnAction(e -> {
                    Tournament.CertEntry en = getTableRow() == null ? null : getTableRow().getItem();
                    if (en != null) commit(en, en.originalName(), en.originalClub());
                });
            }

            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                Tournament.CertEntry en = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (en == null || !en.changed()) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                b.setTooltip(new Tooltip("Original: " + en.originalName() + (en.originalClub().isBlank() ? "" : " · " + en.originalClub())
                        + "\nKlick = Originalwerte wiederherstellen. Gilt nur für die Urkunde."));
                setGraphic(b);
            }
        });
        mark.getProperties().put("fit-min", Ui.textWidth("nur für Druck geändert", true, 13) + 50);
        table.getColumns().setAll(List.of(pick, cls, place, name, club, printedCol, mark));
        none.getStyleClass().add("hint");
        none.setWrapText(true);
        Label note = new Label("Namen und Vereine lassen sich hier für den Druck anpassen (z. B. ausgeschriebener Vereinsname). "
                + "Das gilt nur für die Urkunde: Teilnehmerdaten, Ergebnisse und die click-TT-Datei bleiben unverändert. "
                + "Mit einem Klick auf „nur für Druck geändert“ geht es zurück auf den Originalwert.");
        note.getStyleClass().add("hint");
        note.setWrapText(true);
        note.setMinHeight(Region.USE_PREF_SIZE);
        Label head = new Label("Urkunden");
        head.getStyleClass().add("class-name");
        search.setPromptText("Name, Verein oder Klasse suchen");
        Ui.growingSearch(search);
        search.textProperty().addListener((o, a, b) -> rebuildList());
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, head, sp, search);
        bar.setAlignment(Pos.CENTER_LEFT);
        list.getChildren().setAll(bar, table, none, note);
    }

    /** Druckanpassung speichern und nur diese Zeile aktualisieren (große Turniere: Endergebnis nicht neu berechnen). */
    private void commit(Tournament.CertEntry e, String name, String club) {
        tournament.setCertOverride(e, name, club);
        Tournament.CertEntry now = tournament.currentCertEntry(e);
        List<Tournament.CertEntry> copy = new ArrayList<>(all);
        for (int i = 0; i < copy.size(); i++) if (copy.get(i).key().equals(e.key())) copy.set(i, now);
        all = copy;
        int i = table.getItems().indexOf(e);
        if (i < 0) for (int k = 0; k < table.getItems().size(); k++) if (table.getItems().get(k).key().equals(e.key())) i = k;
        if (i >= 0) table.getItems().set(i, now);
        showPreview(previewIndex);
    }

    private void updateCounter() {
        List<Tournament.CertEntry> scoped = inScope();
        long n = scoped.stream().filter(e -> selected.contains(e.key())).count();
        counter.setText(n + " von " + scoped.size() + (scoped.size() == 1 ? " Urkunde" : " Urkunden") + " ausgewählt");
        boolean none = n == 0;
        print.setText(none ? "Drucken" : n == 1 ? "1 Urkunde drucken" : n + " Urkunden drucken");
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
            tournament.markCertPrinted(sel.stream().map(Tournament.CertEntry::key).toList());
            table.refresh();
            status.accept("Urkunden gespeichert: " + f.getAbsolutePath());
        } catch (IOException ex) {
            status.accept("PDF konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private void printSelected() {
        List<Tournament.CertEntry> sel = selectedEntries();
        if (sel.isEmpty()) return;
        Reports.printWithDialog(CertificateReport.certificates(tournament, sel), jobName(), status, () -> {
            tournament.markCertPrinted(sel.stream().map(Tournament.CertEntry::key).toList());
            table.refresh();
        });
    }
}
