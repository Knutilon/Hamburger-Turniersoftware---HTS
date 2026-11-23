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
import de.hts.persistence.TournamentFile;
import de.hts.persistence.TournamentStore;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Startbildschirm: Übersicht aller Turniere, Anlegen, Starten, Öffnen. */
public final class OverviewView {

    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'");

    private final Stage stage;
    private final TournamentStore store;
    private final Consumer<Tournament> onOpen;

    private final BorderPane root = new BorderPane();
    private final ListView<TournamentStore.Summary> list = new ListView<>();
    private final Label message = new Label();
    /** 1.4.0 (#112): true = Reiter „Archivierte Turniere“ (Turniere, die mehr als 14 Tage zurückliegen). */
    private boolean archive;
    private List<TournamentStore.Summary> allTournaments = List.of();
    private final AppHeader appHeader = new AppHeader();
    private final Label title = new Label("Meine Turniere");
    private final Label subtitle = new Label(HtsApp.NAME + " · Version " + HtsApp.VERSION);
    private final Label empty = new Label();
    private Button create;
    private Button importFile;
    private Button importResults;

    /** Ab so vielen Tagen Abstand zum Turnierdatum zählt ein Turnier als archiviert (mehr als 14 Tage). */
    public static final int ARCHIVE_AFTER_DAYS = 14;

    /** 1.4.0 (#112): Turnier ist archiviert, wenn sein Datum mehr als 14 Tage vor {@code today} liegt (ohne Datum: nein). */
    public static boolean isArchived(LocalDate date, LocalDate today) {
        return date != null && java.time.temporal.ChronoUnit.DAYS.between(date, today) > ARCHIVE_AFTER_DAYS;
    }

    public OverviewView(Stage stage, TournamentStore store, Consumer<Tournament> onOpen) {
        this.stage = stage;
        this.store = store;
        this.onOpen = onOpen;

        // Kopfleiste
        appHeader.addMenuItem("tournaments", "Turniere", null, () -> showArchive(false));
        appHeader.addMenuItem("archive", "Archivierte Turniere", null, () -> showArchive(true));
        appHeader.addMenuItem("organizer", "Ausrichter", null, this::showOrganizer);      // 1.5.0 (#139)
        appHeader.addMenuItem("webserver", "Webserver", null, this::showWebserver);       // 1.5.0 (#135)
        appHeader.select("tournaments");
        InfoDialog.install(appHeader, () -> stage);

        // Seitenkopf
        title.getStyleClass().add("page-title");
        subtitle.getStyleClass().add("page-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        create = new Button("Neues Turnier", Icons.of(Icons.PLUS, 16));
        create.getStyleClass().add("accent");
        create.setOnAction(e -> createTournament());
        importFile = new Button("Turnierdatei übernehmen", Icons.of(Icons.FOLDER, 16));
        importFile.setOnAction(e -> importTournamentFile());
        importFile.getStyleClass().add("pill");
        create.getStyleClass().remove("accent");
        create.getStyleClass().add("navy-pill");
        importResults = new Button("Fertiges Turnier importieren (click-TT)", Icons.of(Icons.UPLOAD, 16));
        importResults.setOnAction(e -> importFinishedTournament());
        importResults.getStyleClass().add("pill");
        importResults.setTooltip(new javafx.scene.control.Tooltip("click-TT-Ergebnisdatei (XML, z. B. „Ergebnisse.xml“) eines "
                + "abgeschlossenen Turniers einlesen – daraus wird ein neues, abgeschlossenes HTS-Turnier"));
        HBox header = new HBox(14, new VBox(2, title, subtitle), spacer, importResults, importFile, create);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("page-header");

        // Liste
        list.setCellFactory(lv -> new TournamentCell());
        list.getStyleClass().add("tournament-list");
        empty.getStyleClass().add("hint");
        list.setPlaceholder(empty);
        list.setOnMouseClicked(e -> {
            if (TouchSupport.isActivate(e, list)
                    && list.getSelectionModel().getSelectedItem() != null) {
                open(list.getSelectionModel().getSelectedItem());
            }
        });
        list.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && list.getSelectionModel().getSelectedItem() != null) {
                open(list.getSelectionModel().getSelectedItem());
            }
        });

        message.getStyleClass().add("warning");
        message.setWrapText(true);
        Label folder = new Label("Ablageordner: " + store.getDirectory().toAbsolutePath()
                + "  (je Turnier ein Ordner mit „Turnierdatei“, „XML Export“ und „PDF Export“)", Icons.of(Icons.FOLDER, 14));
        folder.getStyleClass().add("hint");
        folder.setWrapText(true);                         // 1.4.0: nie mit „…“ abkürzen, sondern umbrechen
        folder.setMinHeight(Region.USE_PREF_SIZE);
        VBox footer = new VBox(4, message, folder);
        footer.setPadding(new Insets(8, 40, 14, 40));

        VBox center = new VBox(list);
        VBox.setVgrow(list, Priority.ALWAYS);
        center.setPadding(new Insets(0, 24, 0, 24));

        VBox top = new VBox(appHeader.getRoot(), header);
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(footer);
        root.getStyleClass().addAll("content", "overview");
        showArchive(false);
    }

    public Node getRoot() {
        return root;
    }

    public void refresh() {
        try {
            allTournaments = store.list();
            LocalDate today = LocalDate.now();
            // 1.4.0 (#112): „Turniere“ = bis 14 Tage alt, „Archivierte Turniere“ = älter; jeweils neuestes Datum zuerst
            list.getItems().setAll(allTournaments.stream()
                    .filter(t -> isArchived(t.date(), today) == archive)
                    .sorted(java.util.Comparator.comparing(TournamentStore.Summary::date,
                                    java.util.Comparator.nullsLast(java.util.Comparator.<LocalDate>reverseOrder()))
                            .thenComparing(t -> t.name() == null ? "" : t.name(), String.CASE_INSENSITIVE_ORDER))
                    .toList());
            var problems = store.getProblems();
            message.setText(problems.isEmpty() ? "" : "Nicht lesbar: " + problems.stream()
                    .map(p -> p.file().getFileName() + " (" + p.message() + ")").collect(Collectors.joining("; ")));
        } catch (IOException e) {
            message.setText("Turnierordner konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    /** 1.4.0 (#112): zwischen „Turniere“ und „Archivierte Turniere“ wechseln. */
    private Node listCenter;

    /** 1.5.0 (#139): Seite „Ausrichter“ – Name und Logo des ausrichtenden Verbands bzw. Vereins (für alle Turniere). */
    private void showOrganizer() {
        if (listCenter == null) listCenter = root.getCenter();
        appHeader.select("organizer");
        title.setText("Ausrichter");
        subtitle.setText("Gilt für alle Turniere – Logo auf dem Schiedsrichterzettel und auf den Live-Seiten");
        for (Button b : new Button[]{create, importFile, importResults}) {
            b.setVisible(false);
            b.setManaged(false);
        }
        de.hts.persistence.Organizer org = de.hts.persistence.Organizer.get();
        Label nameLbl = new Label("Name des Ausrichters (Verband oder Verein)");
        nameLbl.getStyleClass().add("class-name");
        javafx.scene.control.TextField name = new javafx.scene.control.TextField(org.getName());
        name.setPrefColumnCount(30);
        name.setMaxWidth(520);
        name.textProperty().addListener((o, a, b) -> org.setName(b));
        Label logoLbl = new Label("Logo");
        logoLbl.getStyleClass().add("class-name");
        javafx.scene.image.ImageView preview = new javafx.scene.image.ImageView();
        preview.setPreserveRatio(true);
        preview.setFitWidth(150);
        preview.setFitHeight(150);
        Label noLogo = new Label("Kein Logo");
        noLogo.getStyleClass().add("hint");
        javafx.scene.layout.StackPane frame = new javafx.scene.layout.StackPane(preview, noLogo);
        frame.setMinSize(170, 170);
        frame.setMaxSize(170, 170);
        frame.getStyleClass().add("card");
        Label source = new Label();
        source.getStyleClass().add("hint");
        source.setWrapText(true);
        Runnable show = () -> {
            byte[] png = org.logoPng();
            preview.setImage(png == null ? null : new javafx.scene.image.Image(new java.io.ByteArrayInputStream(png)));
            noLogo.setVisible(png == null);
            source.setText(switch (org.getLogoSource()) {
                case DEFAULT -> "Standard: Logo des Hamburger Tisch-Tennis-Verbands";
                case CUSTOM -> "Eigenes Logo (gespeichert im Ablageordner der Turniere)";
                case NONE -> "Ohne Logo";
            });
        };
        Button upload = new Button("Logo hochladen …", Icons.of(Icons.UPLOAD, 16));
        upload.getStyleClass().add("navy-pill");
        upload.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Logo des Ausrichters wählen");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Bild (PNG, JPG)", "*.png", "*.jpg", "*.jpeg"));
            java.io.File f = fc.showOpenDialog(stage);
            if (f == null) return;
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                if (img == null) throw new java.io.IOException("Das Bild lässt sich nicht lesen (bitte PNG oder JPG).");
                // auf höchstens 600 Bildpunkte verkleinern und als PNG speichern (reicht für A6 und Bildschirm)
                double k = Math.min(1, 600.0 / Math.max(img.getWidth(), img.getHeight()));
                int w = (int) Math.round(img.getWidth() * k), h = (int) Math.round(img.getHeight() * k);
                java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = out.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img, 0, 0, w, h, null);
                g.dispose();
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(out, "png", bo);
                org.setCustomLogo(bo.toByteArray());
                show.run();
            } catch (java.io.IOException | RuntimeException ex) {
                showError("Logo konnte nicht übernommen werden: " + ex.getMessage());
            }
        });
        Button std = new Button("Standard-Logo (HaTTV)");
        std.getStyleClass().add("pill");
        std.setOnAction(e -> {
            org.useDefaultLogo();
            show.run();
        });
        Button none = new Button("Kein Logo");
        none.getStyleClass().add("pill");
        none.setOnAction(e -> {
            org.useNoLogo();
            show.run();
        });
        for (Button b : new Button[]{upload, std, none}) b.setMinWidth(Region.USE_PREF_SIZE);
        javafx.scene.layout.FlowPane buttons = new javafx.scene.layout.FlowPane(8, 8, upload, std, none);
        Label hint = new Label("Das Logo erscheint auf dem Schiedsrichterzettel oben rechts und auf den Live-Seiten. Name und "
                + "Logo gelten für alle Turniere auf diesem Rechner und liegen im Ablageordner der Turniere.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setMaxWidth(620);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        VBox logoInfo = new VBox(10, source, buttons);
        HBox logoRow = new HBox(18, frame, logoInfo);
        VBox form = new VBox(12, nameLbl, name, logoLbl, logoRow, hint);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(18, 20, 18, 20));
        form.setMaxWidth(760);
        VBox page = new VBox(form);
        page.setPadding(new Insets(0, 40, 16, 40));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        show.run();
        root.setCenter(sp);
    }

    /** 1.5.0 (#135): Seite „Webserver“ – Adresse und Schlüssel (Programmeinstellung), Verbindung testen. */
    private void showWebserver() {
        if (listCenter == null) listCenter = root.getCenter();
        appHeader.select("webserver");
        title.setText("Webserver");
        subtitle.setText("Ergebnisse im Internet einsehen – Adresse und Schlüssel gelten für alle Turniere auf diesem Rechner");
        for (Button b : new Button[]{create, importFile, importResults}) {
            b.setVisible(false);
            b.setManaged(false);
        }
        Label aLbl = new Label("Adresse des Webservers");
        aLbl.getStyleClass().add("class-name");
        javafx.scene.control.TextField addr = new javafx.scene.control.TextField(de.hts.web.WebSettings.server());
        addr.setPromptText("z. B. live.mein-verband.de");
        addr.setMaxWidth(520);
        Label kLbl = new Label("Geheimer Schlüssel");
        kLbl.getStyleClass().add("class-name");
        javafx.scene.control.PasswordField key = new javafx.scene.control.PasswordField();
        key.setText(de.hts.web.WebSettings.key());
        key.setMaxWidth(520);
        Runnable save = () -> de.hts.web.WebSettings.set(addr.getText(), key.getText());
        addr.textProperty().addListener((o, x, y) -> save.run());
        key.textProperty().addListener((o, x, y) -> save.run());
        Label result = new Label();
        result.setWrapText(true);
        Button test = new Button("Verbindung testen");
        test.getStyleClass().add("navy-pill");
        test.setMinWidth(Region.USE_PREF_SIZE);
        test.setOnAction(e -> {
            result.setText("Prüfe …");
            String a = addr.getText(), k = key.getText();
            Thread th = new Thread(() -> {
                String err = de.hts.web.Publisher.test(a, k);
                javafx.application.Platform.runLater(() -> result.setText(err == null ? "Verbindung in Ordnung." : err));
            }, "HTS-Verbindungstest");
            th.setDaemon(true);
            th.start();
        });
        Label hint = new Label("Veröffentlicht wird je Turnier auf der Seite „Live im Internet“ des Turniers. HTS lädt nur hoch – "
                + "vom Server kommt nichts zurück. Der Schlüssel bleibt auf diesem Rechner und steht nicht in der Turnierdatei.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setMaxWidth(620);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        VBox form = new VBox(10, aLbl, addr, kLbl, key, new HBox(12, test, result), hint);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(18, 20, 18, 20));
        form.setMaxWidth(760);
        VBox page = new VBox(form);
        page.setPadding(new Insets(0, 40, 16, 40));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        root.setCenter(sp);
    }

    private void showError(String text) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, text);
        a.initOwner(stage);
        a.setHeaderText(null);
        DialogSizing.fit(a);
        a.showAndWait();
    }

    private void showArchive(boolean on) {
        if (listCenter != null) root.setCenter(listCenter);
        archive = on;
        appHeader.select(on ? "archive" : "tournaments");
        title.setText(on ? "Archivierte Turniere" : "Meine Turniere");
        subtitle.setText(on ? "Turniere, die mehr als " + ARCHIVE_AFTER_DAYS + " Tage zurückliegen"
                : HtsApp.NAME + " · Version " + HtsApp.VERSION);
        empty.setText(on ? "Keine archivierten Turniere.\nTurniere, die mehr als " + ARCHIVE_AFTER_DAYS
                + " Tage zurückliegen, erscheinen hier."
                : "Noch keine Turniere.\nMit „Neues Turnier“ das erste anlegen.");
        for (Button b : new Button[]{create, importFile, importResults}) {
            b.setVisible(!on);
            b.setManaged(!on);
        }
        refresh();
    }

    // --- Aktionen ----------------------------------------------------------

    private void createTournament() {
        TournamentDialog dlg = new TournamentDialog(stage, null, null, null, true, null, false, new java.util.ArrayList<>(allTournaments));
        dlg.showAndWait().ifPresent(r -> {
            try {
                Tournament t = r.copyFrom() != null
                        ? store.copy(r.copyFrom(), r.copyMode(), r.name(), r.date(), r.tables(), r.test(), r.fileDir())
                        : store.create(r.name(), r.date(), r.tables(), r.fileDir(), r.test(), r.handicap());
                if (dlg.lateFeeCents() > 0) {
                    t.setLateFeeCents(dlg.lateFeeCents());               // 1.0 (#82)
                    store.save(t);
                }
                refresh();
                onOpen.accept(t);
            } catch (IOException e) {
                error("Turnier konnte nicht angelegt werden", e.getMessage());
            }
        });
    }

    private void open(TournamentStore.Summary s) {
        try {
            onOpen.accept(store.load(s));
        } catch (IOException e) {
            error("Turnier konnte nicht geöffnet werden", e.getMessage());
        }
    }

    private void start(TournamentStore.Summary s) {
        if (!confirm("Turnier starten", "Turnier „" + s.name() + "“ jetzt starten?\n"
                + "Die aktuelle Uhrzeit wird als Startzeit festgehalten.")) return;
        try {
            Tournament t = store.load(s);
            t.start();
            store.save(t);
            refresh();
        } catch (IOException | IllegalStateException e) {
            error("Turnier konnte nicht gestartet werden", e.getMessage());
        }
    }

    private void edit(TournamentStore.Summary s) {
        Tournament loaded;
        try {
            loaded = store.load(s);
        } catch (IOException e) {
            error("Turnier konnte nicht geöffnet werden", e.getMessage());
            return;
        }
        TournamentDialog dlg = new TournamentDialog(stage, s.name(), s.date(), s.tables(), false, s.file(), s.test(), List.of(), s.handicap())
                .withLateFee(loaded.getLateFeeCents());
        dlg.showAndWait().ifPresent(r -> {
            try {
                Tournament t = loaded;
                t.setLateFeeCents(dlg.lateFeeCents());                   // 1.0 (#82)
                t.setTest(r.test());
                t.setName(r.name());
                t.setDate(r.date());
                t.setTableCount(r.tables());
                store.save(t);
                if (r.fileDir() != null) store.moveFile(t, r.fileDir());
                refresh();
            } catch (IOException e) {
                error("Turnier konnte nicht gespeichert werden", e.getMessage());
            }
        });
    }

    private void delete(TournamentStore.Summary s) {
        if (!confirm("Turnier löschen", "Turnier „" + s.name() + "“ mit " + s.participants() + " Teilnehmenden und "
                + s.classes() + " Klassen endgültig löschen?")) return;
        try {
            store.delete(s);
            refresh();
        } catch (IOException e) {
            error("Turnier konnte nicht gelöscht werden", e.getMessage());
        }
    }

    /** Eine Turnierdatei aus HTS 0.2/0.3 (oder eine Kopie) als neues Turnier übernehmen. */
    /** Fertiges Turnier aus einer click-TT-Ergebnisdatei: neues, abgeschlossenes HTS-Turnier. */
    private void importFinishedTournament() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("click-TT-Ergebnisdatei eines fertigen Turniers");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml", "*.XML"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;
        try {
            var r = new de.hts.importer.clicktt.ClickTtResultImporter().importFile(f.toPath());
            Tournament t = store.importFinished(r.tournament());
            refresh();
            StringBuilder sb = new StringBuilder("„" + t.getName() + "“ wurde als abgeschlossenes Turnier angelegt: "
                    + r.classes() + (r.classes() == 1 ? " Klasse, " : " Klassen, ") + r.matches() + " Spiele, "
                    + t.getPlayers().size() + " Teilnehmende.");
            if (!r.notes().isEmpty()) {
                sb.append("\n\nHinweise:\n• ").append(String.join("\n• ", r.notes().subList(0, Math.min(20, r.notes().size()))));
                if (r.notes().size() > 20) sb.append("\n• … (").append(r.notes().size() - 20).append(" weitere)");
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
            a.initOwner(stage);
            a.setTitle("Fertiges Turnier importiert");
            a.setHeaderText(null);
            a.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            a.showAndWait();
            message.setText("„" + t.getName() + "“ importiert.");
        } catch (de.hts.importer.clicktt.ClickTtImportException | IOException | RuntimeException e) {
            error("Import fehlgeschlagen", e.getMessage());
        }
    }

    private void importTournamentFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Turnierdatei übernehmen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTS-Turnierdateien", "*." + TournamentFile.EXTENSION));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;
        try {
            Tournament t = store.importFile(f.toPath());
            refresh();
            message.setText("„" + t.getName() + "“ übernommen.");
        } catch (IOException e) {
            error("Turnierdatei konnte nicht übernommen werden", e.getMessage());
        }
    }

    // --- Darstellung -------------------------------------------------------

    private final class TournamentCell extends ListCell<TournamentStore.Summary> {
        @Override
        protected void updateItem(TournamentStore.Summary s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label name = new Label(s.name() == null ? "(ohne Namen)" : s.name());
            name.getStyleClass().add("card-title");
            Label date = new Label(s.date() == null ? "ohne Datum" : s.date().format(TournamentDialog.DATE),
                    Icons.of(Icons.CALENDAR, 14));
            date.getStyleClass().add("hint");
            Label counts = new Label(s.participants() + " Teilnehmende · " + s.classes() + " Klassen · " + s.tables() + " Tische");
            counts.getStyleClass().add("hint");
            Label status = new Label(s.isStarted() ? "gestartet " + s.startedAt().format(START_FORMAT) : "nicht gestartet");
            if (s.isStarted()) {
                javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
                dot.getStyleClass().add("live-dot");
                status.setGraphic(dot);
            }
            status.getStyleClass().addAll("chip", s.isStarted() ? "chip-started" : "chip-planned");
            HBox meta = new HBox(14, date, counts, status);
            if (s.test()) {
                Label test = new Label("Testturnier");
                test.getStyleClass().addAll("chip", "chip-test");
                meta.getChildren().add(test);
            }
            if (s.imported()) {
                Label imp = new Label("Fertiges Turnier per XML importiert");
                imp.getStyleClass().addAll("chip", "chip-planned");
                meta.getChildren().add(imp);
            }
            if (s.handicap()) {
                Label hc = new Label("Vorgabeturnier");
                hc.getStyleClass().addAll("chip", "chip-ready");
                meta.getChildren().add(hc);
            }
            meta.setAlignment(Pos.CENTER_LEFT);
            VBox info = new VBox(4, name, meta);
            HBox.setHgrow(info, Priority.ALWAYS);

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (!s.isStarted()) {
                Button start = new Button("Starten", Icons.of(Icons.START, 14));
                start.getStyleClass().add("pill");
                start.setOnAction(e -> start(s));
                actions.getChildren().add(start);
            }
            Button open = new Button("Öffnen");
            open.getStyleClass().add("navy-pill");
            open.setOnAction(e -> open(s));
            Button edit = new Button("", Icons.of(Icons.EDIT, 14));
            edit.getStyleClass().add("round-small");
            edit.setOnAction(e -> edit(s));
            edit.setTooltip(new javafx.scene.control.Tooltip("Name und Datum bearbeiten"));
            Button del = new Button("", Icons.of(Icons.TRASH, 14));
            del.getStyleClass().add("round-small");
            del.setOnAction(e -> delete(s));
            del.setTooltip(new javafx.scene.control.Tooltip("Turnier löschen"));
            actions.getChildren().addAll(open, edit, del);

            StackPane icon = new StackPane(Icons.of(Icons.ARROW_RIGHT, 16));
            icon.getStyleClass().add("arrow-circle");
            HBox card = new HBox(14, icon, info, actions);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("card");
            setText(null);
            setGraphic(card);
        }
    }

    private boolean confirm(String title, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setTitle(title);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void error(String title, String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.initOwner(stage);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
