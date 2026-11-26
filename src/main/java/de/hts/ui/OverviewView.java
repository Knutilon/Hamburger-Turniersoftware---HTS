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
        appHeader.addMenuItem("program", "Programm", null, this::showProgram);           // 1.8.0 (#152)
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
        // 1.8.0: Titel nie mit „…“ abkürzen – die Knöpfe brechen bei wenig Platz in eine zweite Zeile um
        VBox titleBox = new VBox(2, title, subtitle);
        title.setMinWidth(Region.USE_PREF_SIZE);
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(380);
        subtitle.setMinHeight(Region.USE_PREF_SIZE);
        titleBox.setMinWidth(Region.USE_PREF_SIZE);
        for (Button b : new Button[]{importResults, importFile, create}) b.setMinWidth(Region.USE_PREF_SIZE);
        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(8, 8, importResults, importFile, create);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPrefWrapLength(300);
        HBox.setHgrow(actions, Priority.ALWAYS);
        HBox header = new HBox(14, titleBox, spacer, actions);
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
        Label folder = new Label(store.usesFlatLayout()
                ? "Neue Turniere: " + de.hts.persistence.ProgramFolder.htsDir().toAbsolutePath() + "  ·  ältere Turniere bleiben in "
                + store.getDirectory().toAbsolutePath() + "  (Speicherorte im Reiter „Programm“)"
                : "Ablageordner: " + store.getDirectory().toAbsolutePath()
                + "  (je Turnier ein Ordner mit „Turnierdatei“, „XML Export“ und „PDF Export“)", Icons.of(Icons.FOLDER, 14));
        folderLabel = folder;
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
                case CUSTOM -> "Eigenes Logo (gespeichert im Programmordner)";
                case NONE -> "Ohne Logo";
            });
        };
        Button upload = new Button("Logo hochladen", Icons.of(Icons.UPLOAD, 16));
        upload.getStyleClass().add("navy-pill");
        upload.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Logo des Ausrichters wählen");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Bild (PNG, JPG, SVG)", "*.png", "*.jpg", "*.jpeg", "*.svg",
                    "*.PNG", "*.JPG", "*.JPEG", "*.SVG"));
            java.io.File f = fc.showOpenDialog(stage);
            if (f == null) return;
            try {
                // 1.7.0 (#143): PNG/JPG auf höchstens 600 Bildpunkte, SVG mit 1200 Bildpunkten als PNG (de.hts.persistence.LogoImport)
                org.setCustomLogo(de.hts.persistence.LogoImport.toPng(f.toPath()));
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
                + "Logo gelten für alle Turniere auf diesem Rechner und liegen im Programmordner (dort, wo „HTS starten“ liegt).");
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

    private Label folderLabel;

    /**
     * 1.8.0 (#152): Seite „Programm“ – Dunkelmodus, Speicherorte (Turnierdateien, PDF-Exporte, XML-Exporte) und
     * „Einstellungen aus älterer Version übernehmen“.
     */
    private void showProgram() {
        if (listCenter == null) listCenter = root.getCenter();
        appHeader.select("program");
        title.setText("Programm");
        subtitle.setText("Einstellungen für HTS auf diesem Rechner");
        for (Button b : new Button[]{create, importFile, importResults}) {
            b.setVisible(false);
            b.setManaged(false);
        }
        // Dunkelmodus
        Label darkHead = new Label("Darstellung");
        darkHead.getStyleClass().add("class-name");
        javafx.scene.control.CheckBox dark = new javafx.scene.control.CheckBox("Dunkelmodus aktivieren");
        dark.setSelected(DarkMode.isOn());
        dark.selectedProperty().addListener((o, a, b) -> DarkMode.set(b));
        Label darkHint = wrapHint("Dunkler Hintergrund mit heller Schrift für die ganze Oberfläche. Ausdrucke und PDF-Dateien bleiben hell.");
        VBox darkCard = cardOf(darkHead, dark, darkHint);

        // 1.10.0 (#177): Bedienung (vorher im Info-Dialog; 1.5.0 #124)
        Label useHead = new Label("Bedienung");
        useHead.getStyleClass().add("class-name");
        javafx.scene.control.CheckBox tablet = new javafx.scene.control.CheckBox();
        tablet.setSelected(TouchSupport.isTabletMode());
        tablet.selectedProperty().addListener((o, a, b) -> TouchSupport.setTabletMode(b));
        Label tabletText = new Label("Tablet-Modus: große Bedienelemente und Zahlenfeld bei der Ergebniseingabe – immer, nicht erst "
                + "nach dem ersten Antippen (gilt für diesen Rechner)");
        tabletText.setWrapText(true);
        tabletText.setMinHeight(Region.USE_PREF_SIZE);
        tabletText.setOnMouseClicked(e -> tablet.setSelected(!tablet.isSelected()));
        HBox.setHgrow(tabletText, javafx.scene.layout.Priority.ALWAYS);
        HBox tabletRow = new HBox(6, tablet, tabletText);
        VBox useCard = cardOf(useHead, tabletRow);

        // Speicherorte
        Label whereHead = new Label("Speicherorte");
        whereHead.getStyleClass().add("class-name");
        VBox whereCard = cardOf(whereHead,
                folderRow("Speicherort der HTS-Turnierdateien (speichern und laden)", de.hts.persistence.ProgramFolder.KEY_HTS,
                        de.hts.persistence.ProgramFolder.defaultHtsDir()),
                folderRow("Speicherort der PDF-Exporte", de.hts.persistence.ProgramFolder.KEY_PDF,
                        de.hts.persistence.ProgramFolder.defaultExportDir()),
                folderRow("Speicherort der XML-Exporte (click-TT)", de.hts.persistence.ProgramFolder.KEY_XML,
                        de.hts.persistence.ProgramFolder.defaultExportDir()),
                wrapHint("Gilt für neue Turniere: Die Turnierdateien liegen direkt im gewählten Ordner, PDF- und XML-Dateien "
                        + "beginnen mit Turniername und Datum. Turniere aus älteren Versionen bleiben in ihrem bisherigen Ordner ("
                        + store.getDirectory().toAbsolutePath() + ") und werden dort weiter gespeichert."));

        // 1.10.0 (#177): Protokoll für die Fehlersuche (vorher im Info-Dialog; 1.7.0 #140) – direkt unter „Speicherorte“
        Label logHead = new Label("Protokoll für die Fehlersuche");
        logHead.getStyleClass().add("class-name");
        Button openLog = new Button("Protokollordner öffnen", Icons.of(Icons.FOLDER, 16));
        openLog.getStyleClass().add("pill");
        openLog.setMinWidth(Region.USE_PREF_SIZE);
        openLog.setOnAction(e -> ErrorReport.openFolder(stage));
        Button report = new Button("Fehlerbericht erstellen");
        report.getStyleClass().add("pill");
        report.setMinWidth(Region.USE_PREF_SIZE);
        report.setOnAction(e -> ErrorReport.saveReport(stage));
        VBox logCard = cardOf(logHead, new javafx.scene.layout.FlowPane(8, 8, openLog, report),
                wrapHint("HTS schreibt je Tag ein Protokoll (30 Tage, nur technische Angaben, keine Namen oder Ergebnisse). "
                        + "Den Fehlerbericht bei Problemen an ein GitHub-Issue anhängen."));

        // Programmordner, ältere Version
        Label progHead = new Label("Programmordner");
        progHead.getStyleClass().add("class-name");
        Label progDir = wrapHint("Ausrichter (Name und Logo), Programmeinstellungen und der Ordner „Protokoll“ liegen hier: "
                + de.hts.persistence.ProgramFolder.dir().toAbsolutePath()
                + (de.hts.persistence.ProgramFolder.isFallback() ? "  (Der Ordner von HTS ist nicht beschreibbar – deshalb im Benutzerordner.)" : ""));
        Button openDir = new Button("Programmordner öffnen", Icons.of(Icons.FOLDER, 16));
        openDir.getStyleClass().add("pill");
        openDir.setMinWidth(Region.USE_PREF_SIZE);
        openDir.setOnAction(e -> HtsApp.openLink(de.hts.persistence.ProgramFolder.dir().toUri().toString()));
        Button carry = new Button("Einstellungen aus älterer Version übernehmen");
        carry.getStyleClass().add("pill");
        carry.setMinWidth(Region.USE_PREF_SIZE);
        carry.setOnAction(e -> carryOverSettings());
        javafx.scene.layout.FlowPane progButtons = new javafx.scene.layout.FlowPane(8, 8, openDir, carry);
        Label carryHint = wrapHint("Nach dem Entpacken einer neuen HTS-Version: den Ordner der bisherigen Version wählen (dort, wo deren "
                + "„HTS starten“ liegt). Übernommen werden Ausrichter und Programmeinstellungen. Bei Versionen vor 1.8.0 den Ordner "
                + "„HTS Turniere“ unter „Dokumente“ wählen.");
        VBox progCard = cardOf(progHead, progDir, progButtons, carryHint);

        VBox page = new VBox(14, darkCard, useCard, whereCard, logCard, progCard);
        page.setPadding(new Insets(0, 40, 16, 40));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        root.setCenter(sp);
    }

    private static Label wrapHint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hint");
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static VBox cardOf(Node... nodes) {
        VBox v = new VBox(10, nodes);
        v.getStyleClass().add("card");
        v.setPadding(new Insets(18, 20, 18, 20));
        v.setMaxWidth(820);
        return v;
    }

    /** Eine Zeile „Speicherort“: Bezeichnung, gewählter Ordner (bzw. Vorgabe), „Ordner wählen“, „Vorgabe“. */
    private Node folderRow(String label, String key, java.nio.file.Path standard) {
        Label head = new Label(label);
        head.setWrapText(true);
        head.setMinHeight(Region.USE_PREF_SIZE);
        Label value = new Label();
        value.getStyleClass().add("hint");
        value.setWrapText(true);
        value.setMinHeight(Region.USE_PREF_SIZE);
        Runnable show = () -> {
            java.nio.file.Path c = de.hts.persistence.ProgramFolder.chosen(key);
            value.setText(c != null ? c.toString() : standard + "  (Vorgabe)");
        };
        Button choose = new Button("Ordner wählen", Icons.of(Icons.FOLDER, 16));
        choose.getStyleClass().add("pill");
        choose.setMinWidth(Region.USE_PREF_SIZE);
        choose.setOnAction(e -> {
            javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
            dc.setTitle(label);
            java.nio.file.Path c = de.hts.persistence.ProgramFolder.chosen(key);
            dc.setInitialDirectory(de.hts.persistence.StorageLocations.ensure(c != null ? c : standard));
            File d = dc.showDialog(stage);
            if (d == null) return;
            de.hts.persistence.ProgramFolder.choose(key, d.toPath().equals(standard) ? null : d.toPath());
            show.run();
            updateFolderLabel();
        });
        Button reset = new Button("Vorgabe");
        reset.getStyleClass().add("pill");
        reset.setMinWidth(Region.USE_PREF_SIZE);
        reset.setOnAction(e -> {
            de.hts.persistence.ProgramFolder.choose(key, null);
            show.run();
            updateFolderLabel();
        });
        show.run();
        javafx.scene.layout.FlowPane row = new javafx.scene.layout.FlowPane(8, 6, value, choose, reset);
        row.setAlignment(Pos.CENTER_LEFT);
        value.setMaxWidth(520);
        return new VBox(4, head, row);
    }

    private void updateFolderLabel() {
        if (folderLabel != null && store.usesFlatLayout()) {
            folderLabel.setText("Neue Turniere: " + de.hts.persistence.ProgramFolder.htsDir().toAbsolutePath() + "  ·  ältere Turniere bleiben in "
                    + store.getDirectory().toAbsolutePath() + "  (Speicherorte im Reiter „Programm“)");
        }
    }

    /** „Einstellungen aus älterer Version übernehmen“: Ordner wählen, Dateien kopieren, Ausrichter neu laden. */
    private void carryOverSettings() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Ordner der älteren HTS-Version wählen");
        File d = dc.showDialog(stage);
        if (d == null) return;
        try {
            List<String> done = de.hts.persistence.ProgramFolder.carryOverFrom(d.toPath());
            if (done.isEmpty()) {
                showError("Im gewählten Ordner liegen keine Einstellungen von HTS (ausrichter.properties bzw. programm.properties).");
                return;
            }
            de.hts.persistence.Organizer.load(de.hts.persistence.ProgramFolder.dir());
            DarkMode.set(de.hts.persistence.ProgramFolder.isDarkMode());
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.initOwner(stage);
            a.setTitle("Einstellungen übernommen");
            a.setHeaderText("Übernommen: " + String.join(", ", done));
            a.setContentText("Ausrichter und Programmeinstellungen gelten ab sofort.");
            DialogSizing.fit(a);
            a.showAndWait();
            showProgram();
        } catch (IOException ex) {
            showError("Einstellungen konnten nicht übernommen werden: " + ex.getMessage());
        }
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
        showWebserverForm();
    }

    /**
     * Seite „Webserver“: Adresse und Schlüssel verdeckt (Auge-Knopf zeigt sie). 1.12.0 (#190): ohne Admin-Passwort – jeder
     * Verein bzw. Verband hat seinen eigenen Ausrichter-Schlüssel (#189); mehrere Rechner dürfen sich einen teilen.
     */
    private void showWebserverForm() {
        de.hts.web.WebSettings.State st = de.hts.web.WebSettings.state();
        Label aLbl = new Label("Adresse des Webservers");
        aLbl.getStyleClass().add("class-name");
        Label kLbl = new Label("Geheimer Schlüssel");
        kLbl.getStyleClass().add("class-name");
        Label result = new Label();
        result.setWrapText(true);
        result.setMinHeight(Region.USE_PREF_SIZE);
        VBox form = new VBox(10);
        if (st == de.hts.web.WebSettings.State.UNREADABLE) {
            Label warn = new Label("Die gespeicherten Webserver-Einstellungen lassen sich auf diesem Rechner bzw. unter diesem Benutzer "
                    + "nicht entschlüsseln. Bitte Adresse und Schlüssel neu eintragen. Bis dahin pausiert das Veröffentlichen.");
            warn.getStyleClass().add("publish-state");
            warn.getStyleClass().add("publish-error");
            warn.setWrapText(true);
            warn.setMinHeight(Region.USE_PREF_SIZE);
            form.getChildren().add(warn);
        }
        javafx.scene.control.PasswordField addrHidden = new javafx.scene.control.PasswordField();
        javafx.scene.control.TextField addrShown = new javafx.scene.control.TextField();
        javafx.scene.control.PasswordField keyHidden = new javafx.scene.control.PasswordField();
        javafx.scene.control.TextField keyShown = new javafx.scene.control.TextField();
        addrShown.textProperty().bindBidirectional(addrHidden.textProperty());
        keyShown.textProperty().bindBidirectional(keyHidden.textProperty());
        addrHidden.setText(de.hts.web.WebSettings.server());
        keyHidden.setText(de.hts.web.WebSettings.key());
        addrShown.setPromptText("z. B. https://hts-livescoring.de");
        addrHidden.setPromptText("z. B. https://hts-livescoring.de");
        for (javafx.scene.control.TextField f : new javafx.scene.control.TextField[]{addrHidden, addrShown, keyHidden, keyShown}) {
            f.setMaxWidth(520);
            HBox.setHgrow(f, javafx.scene.layout.Priority.ALWAYS);
        }
        javafx.scene.layout.StackPane addrBox = new javafx.scene.layout.StackPane(addrHidden, addrShown);
        javafx.scene.layout.StackPane keyBox = new javafx.scene.layout.StackPane(keyHidden, keyShown);
        addrBox.setMaxWidth(520);
        keyBox.setMaxWidth(520);
        addrShown.setVisible(false);
        keyShown.setVisible(false);
        javafx.scene.control.ToggleButton eye = new javafx.scene.control.ToggleButton("Anzeigen", Icons.of(Icons.EYE, 16));
        eye.setMinWidth(Region.USE_PREF_SIZE);
        eye.selectedProperty().addListener((o, was, now) -> {
            addrShown.setVisible(now);
            addrHidden.setVisible(!now);
            keyShown.setVisible(now);
            keyHidden.setVisible(!now);
            eye.setText(now ? "Verbergen" : "Anzeigen");
        });
        Button saveBtn = new Button("Speichern");
        saveBtn.getStyleClass().add("navy-pill");
        saveBtn.setMinWidth(Region.USE_PREF_SIZE);
        saveBtn.setOnAction(e -> {
            try {
                de.hts.web.WebSettings.set(addrHidden.getText(), keyHidden.getText());
                result.setText("Gespeichert (verschlüsselt).");
            } catch (IllegalStateException ex) {
                result.setText(ex.getMessage());
            }
        });
        Button test = new Button("Verbindung testen");
        test.getStyleClass().add("pill");
        test.setMinWidth(Region.USE_PREF_SIZE);
        test.setOnAction(e -> {
            result.setText("Prüfe …");
            String a = addrHidden.getText(), k = keyHidden.getText();
            Thread th = new Thread(() -> {
                String err = de.hts.web.Publisher.test(a, k);
                javafx.application.Platform.runLater(() -> result.setText(err == null ? "Verbindung in Ordnung." : err));
            }, "HTS-Verbindungstest");
            th.setDaemon(true);
            th.start();
        });
        javafx.scene.layout.FlowPane actions = new javafx.scene.layout.FlowPane(10, 8, saveBtn, test, eye);
        // 1.12.0 (#189/#190): eigener Schlüssel je Verein bzw. Verband – auf der Website beantragen
        javafx.scene.control.Hyperlink apply = new javafx.scene.control.Hyperlink("Ausrichter-Schlüssel beantragen (im Browser)");
        apply.setWrapText(true);
        apply.setOnAction(e -> {
            String base = de.hts.web.Publisher.normalize(addrHidden.getText().isBlank() ? "https://hts-livescoring.de" : addrHidden.getText());
            if (base.startsWith("https://") || base.startsWith("http://")) de.hts.HtsApp.openLink(base + "/schluessel");
        });
        Label keyHint = new Label("Jeder Verein bzw. Verband bekommt einen eigenen Schlüssel und veröffentlicht damit nur seine eigenen "
                + "Turniere. Mehrere Rechner des Vereins dürfen denselben Schlüssel benutzen. Den Betreiber-Schlüssel des Servers "
                + "(der alle Turniere ändern darf) nur auf eigenen Geräten eintragen.");
        keyHint.getStyleClass().add("hint");
        keyHint.setWrapText(true);
        keyHint.setMaxWidth(620);
        keyHint.setMinHeight(Region.USE_PREF_SIZE);
        form.getChildren().addAll(aLbl, addrBox, kLbl, keyBox, actions, result, keyHint, apply);
        Label hint = new Label("Veröffentlicht wird je Turnier auf der Seite „Live im Internet“ des Turniers. HTS lädt nur hoch – "
                + "vom Server kommt nichts zurück. Adresse und Schlüssel bleiben verschlüsselt auf diesem Rechner (an das "
                + "Benutzerkonto gebunden) und stehen nicht in der Turnierdatei.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        hint.setMaxWidth(620);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        form.getChildren().add(hint);
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
                if (dlg.lateFeeCents() > 0 || !dlg.worldUrl().isEmpty() || dlg.checkinUntil() != null) {
                    t.setLateFeeCents(dlg.lateFeeCents());               // 1.0 (#82)
                    t.setWorldUrl(dlg.worldUrl());                       // 1.8.0 (#151)
                    t.setCheckinUntil(dlg.checkinUntil());               // 1.9.0 (#164)
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
                .withLateFee(loaded.getLateFeeCents()).withWorldUrl(loaded.getWorldUrl())
                .withCheckinUntil(loaded.getCheckinUntil());
        dlg.showAndWait().ifPresent(r -> {
            try {
                Tournament t = loaded;
                t.setLateFeeCents(dlg.lateFeeCents());                   // 1.0 (#82)
                t.setWorldUrl(dlg.worldUrl());                           // 1.8.0 (#151)
                t.setCheckinUntil(dlg.checkinUntil());                   // 1.9.0 (#164)
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
        boolean askServer = wasPublished(s);                      // 1.8.0 (#154)
        boolean fromServer = askServer && confirm("Live-Daten löschen", "Live-Daten auch vom Webserver löschen?\n"
                + "Ja: Das Turnier verschwindet sofort von der Live-Seite.\n"
                + "Nein: Es bleibt dort, bis es der Server automatisch löscht (90 Tage nach der letzten Änderung).");
        try {
            store.delete(s);
            refresh();
        } catch (IOException e) {
            error("Turnier konnte nicht gelöscht werden", e.getMessage());
            return;
        }
        if (fromServer) {
            String[] problem = {null};
            Ui.busy(stage, "Live-Daten werden vom Webserver gelöscht", () -> problem[0] = de.hts.web.PendingDeletes.deleteNow(s.id()));
            if (problem[0] != null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.initOwner(stage);
                a.setTitle("Live-Daten löschen");
                a.setHeaderText("Webserver gerade nicht erreichbar");
                a.setContentText("Das Turnier ist hier gelöscht. Vom Webserver löscht HTS es beim nächsten Start bzw. beim nächsten "
                        + "Veröffentlichen (" + problem[0] + ").");
                DialogSizing.fit(a);
                a.showAndWait();
            }
        }
    }

    /** 1.8.0 (#154): Wurde das Turnier veröffentlicht (eingeschaltet oder schon einmal hochgeladen)? */
    private static boolean wasPublished(TournamentStore.Summary s) {
        if (s.id() == null) return false;
        try {
            var f = de.hts.persistence.TournamentFile.load(s.file()).folders();
            return f != null && ("true".equals(f.get("publish")) || "true".equals(f.get("published-once")));
        } catch (IOException e) {
            return false;
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
