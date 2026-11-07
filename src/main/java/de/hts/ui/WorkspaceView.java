package de.hts.ui;

import de.hts.importer.clicktt.ClickTtImportException;
import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.persistence.TournamentStore;
import de.hts.player.Player;
import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Ansicht eines einzelnen Turniers: Kopfleiste mit Menü (Turniere, Teilnehmende, Klassen)
 * und Inhaltsbereich. Änderungen werden automatisch gespeichert; zusätzlich gibt es „Speichern“.
 */
public final class WorkspaceView {

    private static final String PREF_LAST_IMPORT_DIR = "lastImportDir";
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'");

    private enum Page { PARTICIPANTS, HANDICAP, CLASSES, TABLES, MATCHES, PRESENTATION, MOBILE }

    private final Stage stage;
    private final TournamentStore store;
    private final Tournament tournament;
    private final Runnable onBack;
    private final Preferences prefs = Preferences.userNodeForPackage(WorkspaceView.class);

    private final BorderPane root = new BorderPane();
    private final PlayerTableView participants;
    private final ClassesView classes;
    private final TablesView tablesView;
    private final MatchesView matchesView;
    private final PresentationView presentationView;
    private final HandicapView handicapView;
    private Button matchesMenu;
    /** Menüpunkte, die ein Vorgabeturnier erst nach dem Speichern der Vorgaberegeln zeigt */
    private final List<Button> afterHandicapMenus = new ArrayList<>();
    private Page currentPage = Page.PARTICIPANTS;

    private final Label pageTitle = new Label();
    private final javafx.scene.layout.FlowPane pageActions = new javafx.scene.layout.FlowPane(8, 6);
    private final StackPane pageContent = new StackPane();
    private final StackPane participantsCard = new StackPane();
    private final Label statusLabel = new Label("Bereit.");
    private final Label saveState = new Label();
    private final HBox startBox = new HBox(6);
    /** Knöpfe der Seite rechts oben in der Titelzeile (alle Seiten außer „Teilnehmende“) */
    private final HBox topActions = new HBox(8);
    /** „· ● Gestartet 14:05 Uhr“ in der Unterzeile */
    private final Label startedInfo = new Label();
    private final AppHeader header = new AppHeader();
    private boolean autosaveScheduled;

    public WorkspaceView(Stage stage, TournamentStore store, Tournament tournament, Runnable onBack) {
        this.stage = stage;
        this.store = store;
        this.tournament = tournament;
        this.onBack = onBack;
        this.participants = new PlayerTableView(tournament.getPlayers(), p -> tournament.markModified());
        this.classes = new ClassesView(tournament, this::setStatus, participants::refresh, this::onRunningChanged);
        this.tablesView = new TablesView(tournament, this::setStatus, this::onRunningChanged);
        this.matchesView = new MatchesView(tournament, this::setStatus, this::onRunningChanged);
        this.presentationView = new PresentationView(tournament, this::setStatus);
        this.handicapView = new HandicapView(tournament, this::setStatus, this::onHandicapSaved, () -> stage);
        // Spielzettel drucken, sobald ein Spiel an einen Tisch geht
        tournament.addActivationListener(m -> {
            if (Reports.isAutoPrintSlips()) {
                javafx.application.Platform.runLater(() -> Reports.printSlipSilently(tournament, m, this::setStatus));
            }
        });

        participants.getRoot().getStyleClass().add("card");
        participants.getRoot().setStyle("-fx-padding: 0;");
        participantsCard.getChildren().add(participants.getRoot());
        participantsCard.setPadding(new Insets(0, 40, 16, 40));
        participants.setOnEdit(this::editParticipant);
        participants.setClassSource(() -> tournament.rootClasses().stream().map(PlayingClass::getName).toList());
        classes.setOnShowOpen(c -> {               // 0.37: „Offene Personen anzeigen“
            showPage(Page.PARTICIPANTS);
            participants.showClass(c.getName());
        });
        participants.getTable().setContextMenu(participantContextMenu());
        participants.getTable().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteParticipants();
        });

        root.setTop(createHeader());
        root.setCenter(createContent());
        root.getStyleClass().add("workspace");

        // automatisch speichern
        tournament.modifiedProperty().addListener((o, was, isModified) -> {
            updateSaveState();
            if (isModified) scheduleAutosave();
        });
        updateSaveState();

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN).match(e)) {
                save();
                e.consume();
            }
        });

        showPage(Page.PARTICIPANTS);
    }

    public Node getRoot() {
        return root;
    }

    public Tournament getTournament() {
        return tournament;
    }

    /** Vor dem Verlassen bzw. Schließen: offene Änderungen speichern. @return {@code true}, wenn verlassen werden darf */
    public boolean prepareLeave() {
        if (currentPage == Page.HANDICAP && !handicapView.confirmLeave()) return false;
        presentationView.stop();
        if (liveServer != null) {
            liveServer.stop();
            liveServer = null;
        }
        if (!tournament.isModified()) return true;
        if (save()) return true;
        Alert a = new Alert(Alert.AlertType.WARNING,
                "Die Änderungen konnten nicht gespeichert werden. Trotzdem verlassen? Die Änderungen gehen dann verloren.",
                ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    // --- Kopfleiste -----------------------------------------------------------

    private Node createHeader() {
        header.addMenuItem("back", "Turniere", Icons.of(Icons.BACK, 16), this::goBack);
        header.addMenuItem("participants", "Teilnehmende", null, () -> showPage(Page.PARTICIPANTS));
        if (tournament.isHandicap()) header.addMenuItem("handicap", "Vorgaberegeln", null, () -> showPage(Page.HANDICAP));
        afterHandicapMenus.add(header.addMenuItem("classes", "Klassen", null, () -> showPage(Page.CLASSES)));
        afterHandicapMenus.add(header.addMenuItem("tables", "Tische", null, () -> showPage(Page.TABLES)));
        matchesMenu = header.addMenuItem("matches", "Spiele", null, () -> showPage(Page.MATCHES));
        afterHandicapMenus.add(matchesMenu);
        afterHandicapMenus.add(header.addMenuItem("presentation", "Präsentationsansicht", null, () -> showPage(Page.PRESENTATION)));
        afterHandicapMenus.add(header.addMenuItem("mobile", "Mobile Einstellungen", null, () -> showPage(Page.MOBILE)));
        updateHandicapMenus();
        matchesMenu.setDisable(!tournament.isRunning());
        matchesMenu.setTooltip(new javafx.scene.control.Tooltip("Verfügbar, sobald das Turnier oder eine Klasse gestartet ist"));
        // Speichern geschieht automatisch – kein eigener Knopf; oben links steht das geöffnete Turnier
        header.getContextLabel().textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> "Turnier: " + (tournament.getName() == null ? "" : tournament.getName())
                        + (tournament.getDate() == null ? "" : "  ·  " + tournament.getDate().format(TournamentDialog.DATE)),
                tournament.nameProperty(), tournament.dateProperty()));
        header.addTool(Icons.HOME, "Zurück zur Turnierübersicht", this::goBack);
        if (tournament.isTest()) {
            // Testturnier: alle offenen Spiele zufällig eintragen (nur Symbol)
            header.addTool(Icons.DICE, "Testturnier: alle offenen Spiele der laufenden Phasen zufällig eintragen", this::simulateAll);
            header.getContextLabel().textProperty().unbind();
            header.getContextLabel().textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                    () -> "TESTTURNIER: " + (tournament.getName() == null ? "" : tournament.getName())
                            + (tournament.getDate() == null ? "" : "  ·  " + tournament.getDate().format(TournamentDialog.DATE)),
                    tournament.nameProperty(), tournament.dateProperty()));
        }
        InfoDialog.install(header, () -> stage);
        return header.getRoot();
    }

    /** Vorgabeturnier: Klassen, Tische, Spiele und Präsentation erst, wenn die Art der Vorgabe gespeichert ist. */
    private void updateHandicapMenus() {
        boolean show = !tournament.isHandicap() || tournament.isHandicapReady();
        for (Button b : afterHandicapMenus) {
            b.setVisible(show);
            b.setManaged(show);
        }
        startBox.setVisible(show);
        startBox.setManaged(show);
    }

    private void onHandicapSaved() {
        updateHandicapMenus();
        save();
    }

    private void goBack() {
        if (prepareLeave()) onBack.run();
    }

    private void updateStartBox() {
        startBox.getChildren().clear();
        if (tournament.isStarted()) {
            Circle dot = new Circle(5);
            dot.getStyleClass().add("live-dot");
            // Startinfo in der Unterzeile: Datum nur, wenn es vom Turnierdatum abweicht
            java.time.LocalDateTime at = tournament.getStartedAt();
            boolean sameDay = tournament.getDate() != null && tournament.getDate().equals(at.toLocalDate());
            startedInfo.setText("Gestartet " + (sameDay ? at.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm 'Uhr'"))
                    : at.format(START_FORMAT)));
            startedInfo.setGraphic(dot);
            startedInfo.setGraphicTextGap(6);
            startedInfo.setVisible(true);
            if (tournament.isCompletelyFinished()) {
                javafx.scene.control.MenuButton export = new javafx.scene.control.MenuButton("Ergebnisse exportieren", Icons.of(Icons.START, 14));
                export.getStyleClass().add("navy-pill");
                javafx.scene.control.MenuItem pdf = new javafx.scene.control.MenuItem("Gesamtergebnis als PDF");
                pdf.setOnAction(e -> Reports.showTournamentResults(stage, tournament, this::setStatus));
                javafx.scene.control.MenuItem ctt = new javafx.scene.control.MenuItem("click-TT-Ergebnisdatei (XML)");
                ctt.setOnAction(e -> exportClickTt());
                export.getItems().add(pdf);
                if (!tournament.isHandicap()) export.getItems().add(ctt);      // Vorgabeturnier: kein click-TT-Export
                startBox.getChildren().add(export);
            }
            if (!tournament.previewStart().started().isEmpty()) {
                Button more = new Button("Bereite Klassen starten", Icons.of(Icons.START, 14));
                more.getStyleClass().add("navy-pill");
                more.setOnAction(e -> startTournament());
                startBox.getChildren().add(more);
            }
        } else {
            startedInfo.setVisible(false);
            Button start = new Button("Turnier starten", Icons.of(Icons.START, 16));
            start.getStyleClass().add("navy-pill");
            start.setOnAction(e -> startTournament());
            // Start erst mit mindestens 2 Teilnehmenden, 1 Klasse und 1 Tisch – sonst sichtbar, aber deaktiviert
            List<String> missing = new ArrayList<>();
            if (tournament.getPlayers().size() < 2) missing.add("mindestens 2 Teilnehmende");
            if (tournament.getClasses().stream().noneMatch(c -> !c.isPhase())) missing.add("mindestens 1 Klasse");
            if (tournament.getTableCount() < 1) missing.add("mindestens 1 Tisch");
            start.setDisable(!missing.isEmpty());
            if (!missing.isEmpty()) {
                // Tooltip auch am deaktivierten Knopf: über einen Rahmen um den Knopf
                javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(start);
                javafx.scene.control.Tooltip.install(wrap, new javafx.scene.control.Tooltip("Es fehlen: " + String.join(", ", missing)));
                startBox.getChildren().add(wrap);
            } else {
                startBox.getChildren().add(start);
            }
        }
    }

    // --- Inhalt ------------------------------------------------------------

    private Node createContent() {
        pageTitle.getStyleClass().add("page-title");
        Label tournamentLine = new Label();
        tournamentLine.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> (tournament.getName() == null ? "" : tournament.getName()) + "  ·  "
                        + (tournament.getDate() == null ? "ohne Datum" : tournament.getDate().format(TournamentDialog.DATE)),
                tournament.nameProperty(), tournament.dateProperty()));
        tournamentLine.getStyleClass().add("page-subtitle");
        startedInfo.getStyleClass().addAll("page-subtitle", "started-info");
        startedInfo.managedProperty().bind(startedInfo.visibleProperty());
        Label sep = new Label("·");
        sep.getStyleClass().add("page-subtitle");
        sep.visibleProperty().bind(startedInfo.visibleProperty());
        sep.managedProperty().bind(sep.visibleProperty());
        HBox subLine = new HBox(8, tournamentLine, sep, startedInfo);
        subLine.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(2, pageTitle, subLine);
        titles.setMinWidth(Region.USE_PREF_SIZE);

        startBox.setAlignment(Pos.CENTER_RIGHT);
        tournament.startedAtProperty().addListener((o, a, b) -> updateStartBox());
        tournament.getPlayers().addListener((javafx.collections.ListChangeListener<Player>) ch -> updateStartBox());
        tournament.getClasses().addListener((javafx.collections.ListChangeListener<PlayingClass>) ch -> updateStartBox());
        tournament.tableCountProperty().addListener((o, a, b) -> updateStartBox());
        updateStartBox();

        // rechts oben: Knöpfe der Seite, dahinter die Startknöpfe – bei wenig Platz rechtsbündig umbrechen
        topActions.setAlignment(Pos.CENTER_RIGHT);
        topActions.managedProperty().bind(topActions.visibleProperty());
        javafx.scene.layout.FlowPane right = new javafx.scene.layout.FlowPane(10, 6, topActions, startBox);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setRowValignment(javafx.geometry.VPos.CENTER);
        right.setMinWidth(0);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox titleRow = new HBox(12, titles, right);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("page-header");

        pageActions.setAlignment(Pos.CENTER_LEFT);
        pageActions.getStyleClass().add("page-actions");
        pageActions.managedProperty().bind(pageActions.visibleProperty());

        Region sbSpacer = new Region();
        HBox.setHgrow(sbSpacer, Priority.ALWAYS);
        Label count = new Label();
        count.textProperty().bind(participants.countTextProperty());
        HBox statusBar = new HBox(8, statusLabel, sbSpacer, count);
        statusBar.setPadding(new Insets(6, 24, 6, 24));
        statusBar.getStyleClass().add("status-bar");

        VBox.setVgrow(pageContent, Priority.ALWAYS);
        // 0.36: bei kleinen Auflösungen scrollt die Seite, statt Inhalte zu stauchen oder abzuschneiden
        pageContent.setMinHeight(380);
        VBox page = new VBox(titleRow, pageActions, pageContent);
        javafx.scene.control.ScrollPane pageScroll = new javafx.scene.control.ScrollPane(page);
        pageScroll.setFitToWidth(true);
        pageScroll.setFitToHeight(true);
        pageScroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(pageScroll, Priority.ALWAYS);
        VBox content = new VBox(pageScroll, statusBar);
        content.getStyleClass().add("content");
        return content;
    }

    private void showPage(Page page) {
        if (currentPage == Page.HANDICAP && page != Page.HANDICAP && !handicapView.confirmLeave()) {
            header.select("handicap");
            return;
        }
        if (tournament.isHandicap() && !tournament.isHandicapReady() && page != Page.PARTICIPANTS && page != Page.HANDICAP) {
            page = Page.HANDICAP;
        }
        currentPage = page;
        pageActions.getChildren().clear();
        topActions.getChildren().clear();
        pageActions.setVisible(page == Page.PARTICIPANTS);   // eigene Knopfzeile nur noch bei „Teilnehmende“
        if (page == Page.PARTICIPANTS) {
            header.select("participants");
            pageTitle.setText("Teilnehmende");
            Button importBtn;
            if (tournament.isHandicap()) {
                // Vorgabeturnier: Teilnehmende nur per CSV (Vorname, Nachname, TTR, Klasse)
                importBtn = new Button("CSV der Teilnehmenden importieren", Icons.of(Icons.UPLOAD, 16));
                importBtn.setOnAction(e -> importCsv());
                boolean drawn = tournament.getClasses().stream().anyMatch(PlayingClass::isDrawn);
                importBtn.setDisable(drawn);
                if (drawn) {
                    Label why = new Label("CSV-Import nicht mehr möglich: Es ist bereits eine Klasse ausgelost.");
                    why.getStyleClass().add("hint");
                    pageActions.getChildren().add(why);
                }
            } else {
                importBtn = new Button("XML der Teilnehmenden importieren", Icons.of(Icons.UPLOAD, 16));
                importBtn.setOnAction(e -> importClickTt());
            }
            importBtn.getStyleClass().add("navy-pill");
            Button add = new Button("Hinzufügen", Icons.of(Icons.PLUS, 16));
            add.setOnAction(e -> addParticipant());
            Button edit = new Button("Bearbeiten", Icons.of(Icons.EDIT, 16));
            edit.setOnAction(e -> editParticipant(participants.getSelected()));
            edit.disableProperty().bind(participants.getTable().getSelectionModel().selectedItemProperty().isNull());
            Button delete = new Button("Löschen", Icons.of(Icons.TRASH, 16));
            delete.setOnAction(e -> deleteParticipants());
            delete.disableProperty().bind(participants.getTable().getSelectionModel().selectedItemProperty().isNull());
            for (Button b : List.of(add, edit, delete)) b.getStyleClass().add("pill");
            pageActions.getChildren().addAll(0, List.of(importBtn, add, edit, delete));
            if (tournament.isHandicap()) {
                Button template = new Button("CSV-Vorlage speichern", Icons.of(Icons.SAVE, 16));
                template.getStyleClass().add("pill");
                template.setOnAction(e -> saveCsvTemplate());
                pageActions.getChildren().add(4, template);
            }
            pageActions.getChildren().add(attendanceMenu());
            pageContent.getChildren().setAll(participantsCard);
            participants.refresh();
        } else if (page == Page.HANDICAP) {
            header.select("handicap");
            pageTitle.setText("Vorgaberegeln");
            pageContent.getChildren().setAll(handicapView.getRoot());
            handicapView.refresh();
        } else if (page == Page.TABLES) {
            header.select("tables");
            pageTitle.setText("Tische");
            pageContent.getChildren().setAll(tablesView.getRoot());
            tablesView.refresh();
        } else if (page == Page.PRESENTATION) {
            header.select("presentation");
            pageTitle.setText("Präsentationsansicht");
            pageContent.getChildren().setAll(presentationView.getRoot());
            presentationView.refresh();
        } else if (page == Page.MOBILE) {
            header.select("mobile");
            pageTitle.setText("Mobile Einstellungen");
            pageContent.getChildren().setAll(mobilePage());
        } else if (page == Page.MATCHES) {
            header.select("matches");
            pageTitle.setText("Spiele");
            pageContent.getChildren().setAll(matchesView.getRoot());
            matchesView.refresh();
        } else {
            header.select("classes");
            pageTitle.setText("Klassen");
            Button add = new Button("Klasse anlegen", Icons.of(Icons.PLUS, 16));
            add.getStyleClass().add("navy-pill");
            add.setOnAction(e -> classes.newClass());
            topActions.getChildren().addAll(add);
            pageContent.getChildren().setAll(classes.getRoot());
            classes.refresh();
        }
    }

    /** Nach Starts und Ergebnissen: Menü, Startanzeige und offene Seiten aktualisieren. */
    private void onRunningChanged() {
        participants.refresh();
        if (matchesMenu != null) matchesMenu.setDisable(!tournament.isRunning());
        updateStartBox();
        if (currentPage == Page.TABLES) tablesView.refresh();
        if (currentPage == Page.MATCHES) matchesView.refresh();
        if (currentPage == Page.CLASSES) classes.refresh();
    }

    // --- Speichern / Starten -----------------------------------------------

    private void updateSaveState() {
        boolean dirty = tournament.isModified();
        saveState.setText(dirty ? "Ungespeicherte Änderungen" : "Alles gespeichert");
        saveState.getStyleClass().removeAll("dirty", "clean");
        saveState.getStyleClass().add(dirty ? "dirty" : "clean");
    }

    private void scheduleAutosave() {
        if (autosaveScheduled) return;
        autosaveScheduled = true;
        // mehrere Änderungen kurz hintereinander werden zu einem Speichervorgang zusammengefasst
        Platform.runLater(() -> {
            autosaveScheduled = false;
            if (tournament.isModified()) save();
        });
    }

    /** @return {@code true}, wenn gespeichert wurde */
    private boolean save() {
        try {
            store.save(tournament);
            updateSaveState();
            return true;
        } catch (IOException e) {
            setStatus("Speichern fehlgeschlagen.");
            error("Speichern fehlgeschlagen", e.getMessage());
            return false;
        }
    }

    /**
     * Turnier starten: alle bereiten Klassen werden gestartet und ihre Spiele eingereiht. Sind nicht alle Klassen
     * bereit (oder laufen schon), gibt es vorher einen Hinweis.
     */
    private void startTournament() {
        Tournament.StartReport r = tournament.previewStart();
        StringBuilder sb = new StringBuilder();
        if (!r.started().isEmpty()) {
            sb.append("Diese Klassen werden gestartet, ihre Spiele eingereiht:\n").append(names(r.started())).append("\n\n");
        }
        if (!r.notReady().isEmpty()) {
            sb.append("Noch nicht bereit (werden nicht gestartet):\n").append(names(r.notReady())).append("\n\n");
        }
        if (!r.alreadyRunning().isEmpty()) {
            sb.append("Laufen bereits:\n").append(names(r.alreadyRunning())).append("\n\n");
        }
        if (!r.attendanceOpen().isEmpty()) {
            sb.append("Noch gesperrt – Gemeldete noch nicht an- oder abgemeldet (werden nicht gestartet):\n");
            for (PlayingClass c : r.attendanceOpen()) {
                sb.append("  • ").append(c.getName()).append(": ").append(tournament.openAttendance(c).size()).append(" offen\n");
            }
            sb.append("\n");
        }
        if (r.started().isEmpty()) {
            sb.append(tournament.isStarted() ? "Es ist keine weitere Klasse bereit." : "Es ist noch keine Klasse bereit – es wird nur die Startzeit festgehalten.");
        } else {
            sb.append("Freie Tische (").append(tournament.getTableCount()).append(") werden automatisch belegt.");
        }
        boolean allReady = r.notReady().isEmpty() && r.alreadyRunning().isEmpty() && r.attendanceOpen().isEmpty();
        Alert a = new Alert(allReady ? Alert.AlertType.CONFIRMATION : Alert.AlertType.WARNING, sb.toString(), ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setTitle("Turnier starten");
        a.setHeaderText(allReady ? "Alle Klassen sind bereit – Turnier jetzt starten?"
                : "Nicht alle Klassen sind bereit. Trotzdem starten?");
        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            tournament.startTournament();
        } catch (IllegalStateException ex) {
            setStatus("Start nicht möglich.");
            error("Start nicht möglich", ex.getMessage());
            return;
        }
        save();
        setStatus("Turnier gestartet: " + r.started().size() + " Klassen, " + Ui.spiele(tournament.queue().size())
                + " eingereiht, " + tournament.getMatches().stream().filter(m -> m.getStatus() == de.hts.tournament.MatchStatus.ACTIVE).count()
                + " an den Tischen.");
        onRunningChanged();
        showPage(Page.TABLES);
    }

    private static String names(List<PlayingClass> list) {
        return list.stream().map(c -> "  • " + c.getName()).reduce((x, y) -> x + "\n" + y).orElse("");
    }

    // --- Teilnehmende --------------------------------------------------------

    private ContextMenu participantContextMenu() {
        MenuItem edit = new MenuItem("Bearbeiten");
        edit.setOnAction(e -> editParticipant(participants.getSelected()));
        MenuItem delete = new MenuItem("Löschen");
        delete.setOnAction(e -> deleteParticipants());
        // 0.36: Anwesenheit per Rechtsklick
        MenuItem present = new MenuItem("angemeldet (anwesend)");
        present.setOnAction(e -> setAttendance(de.hts.player.Attendance.PRESENT, "angemeldet"));
        MenuItem withdraw = new MenuItem("endgültig abmelden");
        withdraw.setOnAction(e -> withdrawSelected());
        MenuItem undo = new MenuItem("Abmeldung zurücknehmen");
        undo.setOnAction(e -> setAttendance(de.hts.player.Attendance.UNKNOWN, "wieder offen"));
        MenuItem confirmReq = new MenuItem("Abmeldung bestätigen");
        confirmReq.setOnAction(e -> setAttendance(de.hts.player.Attendance.WITHDRAWN, "endgültig abgemeldet"));
        ContextMenu menu = new ContextMenu(present, withdraw, undo, confirmReq, new SeparatorMenuItem(), edit, new SeparatorMenuItem(), delete);
        menu.setOnShowing(e -> {
            Player p = participants.getSelected();
            de.hts.player.Attendance a = p == null ? null : p.getAttendance();
            undo.setVisible(a == de.hts.player.Attendance.WITHDRAWN);
            confirmReq.setVisible(a == de.hts.player.Attendance.WITHDRAW_REQUESTED);
            present.setVisible(a != de.hts.player.Attendance.PRESENT);
            withdraw.setVisible(a != de.hts.player.Attendance.WITHDRAWN);
        });
        return menu;
    }

    private void addParticipant() {
        Player p = new Player();
        List<PlayingClass> singles = tournament.getClasses().stream().filter(c -> !c.isDoubles() && !c.isPhase()).toList();
        ParticipantDialog dialog = new ParticipantDialog(stage, p, true, singles);
        if (Boolean.TRUE.equals(dialog.showAndWait().orElse(false))) {
            boolean exists = tournament.getPlayers().stream().anyMatch(o -> o.identityKey().equals(p.identityKey()));
            if (exists && !confirm("Doppelt?", p.getDisplayName() + " (gleicher Name, Jahrgang und Verein) ist bereits erfasst. "
                    + "Trotzdem hinzufügen?")) {
                return;
            }
            p.setAttendance(de.hts.player.Attendance.PRESENT);     // von Hand erfasst: gilt sofort als angemeldet
            tournament.getPlayers().add(p);
            PlayingClass chosen = dialog.getSelectedClass();
            if (chosen != null) tournament.assign(p, chosen);
            participants.refresh();
            classes.refresh();
            participants.getTable().getSelectionModel().clearSelection();
            participants.getTable().getSelectionModel().select(p);
            participants.getTable().scrollTo(p);
            setStatus(p.getDisplayName() + " hinzugefügt" + (chosen != null ? " (Klasse „" + chosen.getName() + "“)." : "."));
        }
    }

    private void editParticipant(Player p) {
        if (p == null) return;
        if (Boolean.TRUE.equals(new ParticipantDialog(stage, p, false, List.of()).showAndWait().orElse(false))) {
            tournament.markModified();
            participants.refresh();
            classes.refresh();
            setStatus(p.getDisplayName() + " geändert.");
        }
    }

    private void deleteParticipants() {
        List<Player> selected = new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        String text = selected.size() == 1
                ? "„" + selected.get(0).getDisplayName() + "“ löschen?"
                : selected.size() + " Teilnehmende löschen?";
        if (confirm("Teilnehmende löschen", text)) {
            tournament.removePlayers(selected);
            setStatus(selected.size() + " Teilnehmende gelöscht.");
        }
    }

    // --- Testmodus ----------------------------------------------------------------

    private void simulateAll() {
        if (!confirm("Alle Spiele simulieren", "Alle offenen Spiele der laufenden, nicht abgeschlossenen Phasen zufällig eintragen?\n"
                + "Folgerunden (KO, Schweizer System, Qualifikation) werden mitgespielt, bis die Phasen fertig sind. "
                + "Phasen werden nicht abgeschlossen.")) return;
        int n = tournament.simulateAll(new java.util.Random());
        onRunningChanged();
        setStatus(n == 0 ? "Keine offenen Spiele in laufenden Phasen – zuerst eine Klasse bzw. das Turnier starten."
                : de.hts.ui.Ui.spiele(n) + " zufällig eingetragen.");
    }

    // --- click-TT-Ergebnisexport ------------------------------------------------

    /** Kopie der importierten click-TT-Datei – Grundlage des Ergebnisexports (wie in poinTT). */
    private Path clickTtCopy() {
        Path now = store.fileOf(tournament).resolveSibling(
                de.hts.persistence.StorageLocations.safeName(tournament.getName()) + ".clicktt.xml");
        Path old = store.getDirectory().resolve((tournament.getId() == null ? "turnier" : tournament.getId()) + ".clicktt.xml");
        return !java.nio.file.Files.exists(now) && java.nio.file.Files.isRegularFile(old) ? old : now;
    }

    private void rememberClickTtFile(Path original) {
        try {
            Path copy = clickTtCopy();
            java.nio.file.Files.createDirectories(copy.getParent());
            java.nio.file.Files.copy(original, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException | RuntimeException e) {
            setStatus("Hinweis: Kopie der click-TT-Datei konnte nicht abgelegt werden (" + e.getMessage() + ").");
        }
    }

    /** Ergebnisse aller beendeten Klassen in die click-TT-Turnierdatei schreiben („Ergebnisse.xml“). */
    private void exportClickTt() {
        Path original = clickTtCopy();
        if (!java.nio.file.Files.isRegularFile(original)) {
            FileChooser pick = new FileChooser();
            pick.setTitle("Ursprüngliche click-TT-Turnierdatei (XML) auswählen");
            pick.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml", "*.XML"));
            File lastDir = new File(prefs.get(PREF_LAST_IMPORT_DIR, System.getProperty("user.home")));
            if (lastDir.isDirectory()) pick.setInitialDirectory(lastDir);
            File f = pick.showOpenDialog(stage);
            if (f == null) return;
            original = f.toPath();
        }
        FileChooser save = new FileChooser();
        save.setTitle("click-TT-Ergebnisdatei speichern");
        save.setInitialFileName(de.hts.persistence.StorageLocations.resultsFileName(tournament));
        save.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml"));
        // Standard: „XML Export“ im Turnierordner (bzw. der zuletzt für dieses Turnier gewählte Ordner)
        save.setInitialDirectory(de.hts.persistence.StorageLocations.ensure(de.hts.persistence.StorageLocations.xmlDir(tournament)));
        File target = save.showSaveDialog(stage);
        if (target == null) return;
        de.hts.persistence.StorageLocations.rememberXmlDir(tournament, target.getParentFile().toPath());
        try {
            var rep = de.hts.importer.clicktt.ClickTtResultExporter.export(tournament, original, target.toPath());
            StringBuilder sb = new StringBuilder(rep.competitions() + " Konkurrenz(en), " + rep.matches() + " Spiele, "
                    + rep.placements() + " Platzierungen exportiert nach\n" + target.getAbsolutePath()
                    + "\n\nDie Datei kann in click-TT beim Turnier hochgeladen werden.");
            if (!rep.skipped().isEmpty()) sb.append("\n\nNicht exportiert:\n• ").append(String.join("\n• ", rep.skipped()));
            if (!rep.warnings().isEmpty()) {
                sb.append("\n\nHinweise:\n• ").append(String.join("\n• ", rep.warnings().subList(0, Math.min(15, rep.warnings().size()))));
                if (rep.warnings().size() > 15) sb.append("\n• … (").append(rep.warnings().size() - 15).append(" weitere)");
            }
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
            a.initOwner(stage);
            a.setTitle("click-TT-Export");
            a.setHeaderText("Ergebnisdatei erstellt");
            a.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            a.showAndWait();
            setStatus("click-TT-Ergebnisdatei gespeichert: " + target.getName());
        } catch (java.io.IOException ex) {
            error("Export fehlgeschlagen", ex.getMessage());
        }
    }

    // --- Import ------------------------------------------------------------

    /** Vorgabeturnier: Teilnehmende aus CSV (Vorname, Nachname, TTR-Wert, Klasse) – ersetzt alle bisherigen. */
    private void importCsv() {
        if (tournament.getClasses().stream().anyMatch(PlayingClass::isDrawn)) {
            error("CSV-Import nicht möglich", "Es ist bereits eine Klasse ausgelost – die Teilnehmenden können nicht mehr ersetzt werden.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("CSV der Teilnehmenden auswählen (Vorname, Nachname, TTR-Wert, Klasse)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv", "*.CSV"));
        File lastDir = new File(prefs.get(PREF_LAST_IMPORT_DIR, System.getProperty("user.home")));
        if (lastDir.isDirectory()) chooser.setInitialDirectory(lastDir);
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        if (file.getParentFile() != null) prefs.put(PREF_LAST_IMPORT_DIR, file.getParentFile().getAbsolutePath());
        de.hts.importer.csv.HandicapCsvImporter.Result result;
        try {
            result = new de.hts.importer.csv.HandicapCsvImporter().importFile(file.toPath());
        } catch (de.hts.importer.csv.HandicapCsvImporter.CsvException ex) {
            error("CSV-Import nicht möglich", ex.getMessage());
            return;
        } catch (IOException ex) {
            error("CSV-Import fehlgeschlagen", ex.getMessage());
            return;
        }
        if (result.players().isEmpty()) {
            error("CSV-Import", "Die Datei enthält keine gültigen Teilnehmenden."
                    + (result.ignored().isEmpty() ? "" : "\n\nIgnorierte Zeilen:\n" + String.join("\n", result.ignored())));
            return;
        }
        if (!tournament.getPlayers().isEmpty()) {
            ButtonType go = new ButtonType("Fortfahren", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            ButtonType back = new ButtonType("Zurück", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert a = new Alert(Alert.AlertType.WARNING, "Alle bisherigen Teilnehmenden (" + tournament.getPlayers().size()
                    + ") und ihre Klassenzuordnungen werden entfernt. Fortfahren?", go, back);
            a.initOwner(stage);
            a.setTitle("CSV-Import");
            a.setHeaderText(null);
            if (a.showAndWait().orElse(back) != go) {
                setStatus("CSV-Import abgebrochen.");
                return;
            }
        }
        try {
            tournament.replaceParticipants(result.players());
        } catch (IllegalStateException ex) {
            error("CSV-Import nicht möglich", ex.getMessage());
            return;
        }
        participants.refresh();
        classes.refresh();
        setStatus("CSV-Import aus " + file.getName() + ": " + result.players().size() + " Teilnehmende"
                + (result.ignored().isEmpty() ? "." : ", " + result.ignored().size() + " Zeilen ignoriert."));
        if (!result.ignored().isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION, result.players().size() + " Teilnehmende importiert. "
                    + result.ignored().size() + (result.ignored().size() == 1 ? " Zeile wurde" : " Zeilen wurden") + " ignoriert:\n\n"
                    + String.join("\n", result.ignored()), ButtonType.OK);
            info.initOwner(stage);
            info.setTitle("CSV-Import");
            info.setHeaderText(null);
            DialogSizing.fit(info);
            info.showAndWait();
        }
    }

    /** Vorlage für den CSV-Import speichern. */
    private void saveCsvTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("CSV-Vorlage speichern");
        chooser.setInitialFileName("Vorlage-Teilnehmende-Vorgabeturnier.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv"));
        File lastDir = new File(prefs.get(PREF_LAST_IMPORT_DIR, System.getProperty("user.home")));
        if (lastDir.isDirectory()) chooser.setInitialDirectory(lastDir);
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            java.nio.file.Files.writeString(file.toPath(), de.hts.importer.csv.HandicapCsvImporter.TEMPLATE, java.nio.charset.StandardCharsets.UTF_8);
            setStatus("CSV-Vorlage gespeichert: " + file.getName());
        } catch (IOException ex) {
            error("Vorlage konnte nicht gespeichert werden", ex.getMessage());
        }
    }

    private void importClickTt() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("XML der Teilnehmenden (click-TT-Turnierexport) auswählen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML-Dateien", "*.xml", "*.XML"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        File lastDir = new File(prefs.get(PREF_LAST_IMPORT_DIR, System.getProperty("user.home")));
        if (lastDir.isDirectory()) chooser.setInitialDirectory(lastDir);
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        if (file.getParentFile() != null) prefs.put(PREF_LAST_IMPORT_DIR, file.getParentFile().getAbsolutePath());

        Path path = file.toPath();
        Task<ClickTtXmlImporter.ImportResult> task = new Task<>() {
            @Override
            protected ClickTtXmlImporter.ImportResult call() throws Exception {
                return new ClickTtXmlImporter().importFile(path);
            }
        };
        task.setOnSucceeded(e -> onImportRead(file, task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            setStatus("Import fehlgeschlagen.");
            error("Import fehlgeschlagen", ex instanceof ClickTtImportException ? ex.getMessage() : "Unerwarteter Fehler: " + ex);
        });
        setStatus("Lese " + file.getName() + " …");
        Thread t = new Thread(task, "clicktt-import");
        t.setDaemon(true);
        t.start();
    }

    private void onImportRead(File file, ClickTtXmlImporter.ImportResult result) {
        ImportPreviewDialog dialog = new ImportPreviewDialog(stage, file.getName(), result, tournament);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            setStatus("Import abgebrochen.");
            return;
        }
        rememberClickTtFile(file.toPath());
        // 0.36: abgewählte Klassen nicht übernehmen
        java.util.Set<String> selected = dialog.getSelectedCompetitions();
        java.util.Set<String> dropped = new java.util.HashSet<>();
        result.competitions().forEach(ci -> {
            if (!selected.contains(ci.name())) dropped.add(ci.name());
        });
        if (!dropped.isEmpty()) {
            List<Player> kept = new ArrayList<>();
            for (Player p : result.players()) {
                if (!ImportPreviewDialog.keeps(p, selected)) continue;
                dropped.forEach(p::forgetClass);
                kept.add(p);
            }
            result = new ClickTtXmlImporter.ImportResult(result.tournamentName(), kept,
                    result.competitions().stream().filter(ci -> selected.contains(ci.name())).toList(),
                    result.pairs().stream().filter(pr -> selected.contains(pr.competition())).toList(), result.warnings());
        }
        int newClasses = 0;
        for (ClickTtXmlImporter.CompetitionInfo ci : result.competitions()) {
            Discipline d = ci.doubles() ? Discipline.DOUBLES : Discipline.SINGLES;
            var existing = tournament.findClass(ci.name());
            if (existing.isEmpty()) {
                PlayingClass nc = tournament.addClass(new PlayingClass(ci.name(), d, ClassType.guess(ci.name(), ci.sex()), null, null));
                nc.setImportedFromXml(true);
                newClasses++;
            } else if (ci.doubles() && !existing.get().isDoubles()) {
                tournament.changeDiscipline(existing.get(), Discipline.DOUBLES);
            }
        }
        // 0.35: Gemeldete stehen nicht mehr direkt „In der Klasse“, sondern als Meldung unter „Verfügbar“ – übernommen
        // werden sie erst, wenn sie am Turniertag angemeldet (anwesend) sind; Doppelpartner werden gemerkt
        List<String[]> partnerLinks = new ArrayList<>();
        for (ClickTtXmlImporter.ImportedPair ip : result.pairs()) {
            if (!ip.waitingList()) partnerLinks.add(new String[]{ip.competition(), ip.firstKey(), ip.secondKey()});
        }
        Tournament.MergeResult merge = tournament.mergeImportedEntries(result.players(), partnerLinks);
        int newPairs = partnerLinks.size();
        participants.refresh();
        classes.refresh();
        setStatus("Import aus " + file.getName() + ": " + merge.added() + " neu, " + merge.updated() + " aktualisiert"
                + (newClasses > 0 ? ", " + newClasses + " Klassen angelegt" : "")
                + (newPairs > 0 ? ", " + newPairs + " Doppelpaarungen gemerkt" : "")
                + ". Gemeldete kommen erst nach der Anmeldung am Turniertag in die Klasse.");
    }

    // --- 0.35: Anwesenheit und Handy-Zugang ------------------------------------

    private de.hts.web.LiveServer liveServer;

    /**
     * 0.36: zwei Menüs – „Aktion bezüglich Markierte“ (angemeldet, endgültig abmelden, Abmeldung zurücknehmen) und
     * „Handyabmeldungen“ (alle bestätigen, Markierte bzw. alle zurück auf offen).
     */
    private javafx.scene.Node attendanceMenu() {
        javafx.scene.control.MenuButton marked = new javafx.scene.control.MenuButton("Aktion bezüglich Markierte");
        marked.getStyleClass().add("pill");
        MenuItem present = new MenuItem("angemeldet (anwesend)");
        present.setOnAction(e -> setAttendance(de.hts.player.Attendance.PRESENT, "angemeldet"));
        MenuItem withdraw = new MenuItem("endgültig abmelden");
        withdraw.setOnAction(e -> withdrawSelected());
        MenuItem undo = new MenuItem("Abmeldung zurücknehmen");
        undo.setOnAction(e -> {
            List<Player> sel = selectedWith(de.hts.player.Attendance.WITHDRAWN);
            sel.forEach(p -> tournament.setAttendance(p, de.hts.player.Attendance.UNKNOWN));
            setStatus(sel.isEmpty() ? "Unter den Markierten ist niemand endgültig abgemeldet." : sel.size() + " Abmeldung(en) zurückgenommen.");
            refreshAfterAttendance();
        });
        marked.getItems().addAll(present, withdraw, new SeparatorMenuItem(), undo);
        marked.setTooltip(new Tooltip("Für die markierten Teilnehmenden – gelb = endgültig abgemeldet"));

        javafx.scene.control.MenuButton phone = new javafx.scene.control.MenuButton("Handyabmeldungen");
        phone.getStyleClass().add("pill");
        MenuItem confirmAll = new MenuItem("alle bestätigen");
        confirmAll.setOnAction(e -> {
            List<Player> req = withAttendance(tournament.getPlayers(), de.hts.player.Attendance.WITHDRAW_REQUESTED);
            req.forEach(p -> tournament.setAttendance(p, de.hts.player.Attendance.WITHDRAWN));
            setStatus(req.isEmpty() ? "Keine offenen Handyabmeldungen." : req.size() + (req.size() == 1 ? " Abmeldung" : " Abmeldungen") + " bestätigt.");
            refreshAfterAttendance();
        });
        MenuItem markedOpen = new MenuItem("Markierte zurück auf offen");
        markedOpen.setOnAction(e -> {
            List<Player> req = selectedWith(de.hts.player.Attendance.WITHDRAW_REQUESTED);
            req.forEach(p -> tournament.setAttendance(p, de.hts.player.Attendance.UNKNOWN));
            setStatus(req.isEmpty() ? "Unter den Markierten ist keine Handyabmeldung." : req.size() + " Handyabmeldung(en) verworfen – wieder offen.");
            refreshAfterAttendance();
        });
        MenuItem allOpen = new MenuItem("Alle zurück auf offen");
        allOpen.setOnAction(e -> {
            List<Player> req = withAttendance(tournament.getPlayers(), de.hts.player.Attendance.WITHDRAW_REQUESTED);
            req.forEach(p -> tournament.setAttendance(p, de.hts.player.Attendance.UNKNOWN));
            setStatus(req.isEmpty() ? "Keine offenen Handyabmeldungen." : req.size() + " Handyabmeldung(en) verworfen – wieder offen.");
            refreshAfterAttendance();
        });
        phone.getItems().addAll(confirmAll, new SeparatorMenuItem(), markedOpen, allOpen);
        phone.setTooltip(new Tooltip("Per Handy gemeldete Abmeldungen (orange) bestätigen oder verwerfen"));
        HBox box = new HBox(8, marked, phone);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static List<Player> withAttendance(List<Player> list, de.hts.player.Attendance a) {
        return list.stream().filter(p -> p.getAttendance() == a).toList();
    }

    private List<Player> selectedWith(de.hts.player.Attendance a) {
        return withAttendance(new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems()), a);
    }

    private void withdrawSelected() {
        List<Player> sel = new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems());
        if (sel.isEmpty()) {
            setStatus("Bitte zuerst Teilnehmende markieren.");
            return;
        }
        if (confirm("Endgültig abmelden", (sel.size() == 1 ? sel.get(0).getDisplayName() : sel.size() + " Teilnehmende")
                + " endgültig abmelden (nicht anwesend)?")) {
            setAttendance(de.hts.player.Attendance.WITHDRAWN, "endgültig abgemeldet");
        }
    }

    private void setAttendance(de.hts.player.Attendance a, String text) {
        List<Player> sel = new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems());
        if (sel.isEmpty()) {
            setStatus("Bitte zuerst Teilnehmende markieren.");
            return;
        }
        for (Player p : sel) tournament.setAttendance(p, a);
        setStatus(sel.size() + (sel.size() == 1 ? " Person " : " Personen ") + text + ".");
        refreshAfterAttendance();
    }

    private void refreshAfterAttendance() {
        participants.refresh();
        classes.refresh();
        updateStartBox();
    }

    /**
     * 0.37: Seite „Mobile Einstellungen“ – Handy-Zugang starten/beenden, QR-Codes drucken, Anmeldung sperren/öffnen,
     * Zustand, Adressen mit QR-Code-Vorschau und Hinweise.
     */
    private javafx.scene.Node mobilePage() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 20, 20, 20));
        Button live = new Button();
        live.getStyleClass().add("navy-pill");
        Button qr = new Button("QR-Codes drucken");
        qr.getStyleClass().add("pill");
        Button lock = new Button();
        lock.getStyleClass().add("pill");
        javafx.scene.layout.FlowPane buttons = new javafx.scene.layout.FlowPane(10, 8, live, qr, lock);
        Label state = new Label();
        state.getStyleClass().add("class-name");
        state.setWrapText(true);
        javafx.scene.layout.FlowPane codes = new javafx.scene.layout.FlowPane(30, 16);
        Label hints = new Label("Hinweise: Laptop und Handys müssen im selben WLAN sein – das Haupt-WLAN des Routers nutzen, nicht das "
                + "Gäste-WLAN (es trennt die Geräte voneinander). Ein Internetanschluss ist nicht nötig. Beim ersten Start fragt die "
                + "Windows-Firewall nach – „Private Netzwerke“ erlauben. Die QR-Codes erst drucken, wenn der Laptop im Turnier-WLAN ist "
                + "(die Adresse hängt vom Netz ab). Abmeldungen per Handy bestätigt die Turnierleitung unter „Teilnehmende“ → "
                + "„Handyabmeldungen“.");
        hints.getStyleClass().add("hint");
        hints.setWrapText(true);
        hints.setMinHeight(Region.USE_PREF_SIZE);
        Runnable update = () -> {
            boolean on = liveServer != null && liveServer.isRunning();
            live.setText(on ? "Handy-Zugang beenden" : "Handy-Zugang starten");
            lock.setText(tournament.isCheckinLocked() ? "Anmeldung per Adresse wieder öffnen" : "Anmeldung per Adresse sperren");
            String host = de.hts.web.LiveServer.localAddresses().stream().findFirst().orElse(null);
            state.setText("Handy-Zugang: " + (on ? "läuft (Port " + liveServer.port() + ")" : "aus") + " · Anmeldung per Adresse: "
                    + (tournament.isCheckinOpen() ? "offen" : tournament.isCheckinLocked() ? "gesperrt" : "geschlossen (alle Klassen beendet)")
                    + (host == null ? " · ⚠ keine Netzwerkverbindung" : ""));
            codes.getChildren().clear();
            if (host != null) {
                int port = on ? liveServer.port() : de.hts.web.LiveServer.PORT;
                codes.getChildren().addAll(qrPreview("Live-Anzeige", de.hts.web.LiveServer.liveUrl(host, port)),
                        qrPreview("Anmeldung am Turniertag", de.hts.web.LiveServer.checkinUrl(host, port, tournament.getCheckinToken())));
            }
        };
        live.setOnAction(e -> {
            if (liveServer != null && liveServer.isRunning()) {
                liveServer.stop();
                liveServer = null;
                setStatus("Handy-Zugang beendet.");
            } else {
                try {
                    liveServer = new de.hts.web.LiveServer(tournament, javafx.application.Platform::runLater, this::refreshAfterAttendance);
                    liveServer.start();
                    setStatus("Handy-Zugang gestartet.");
                } catch (java.io.IOException ex) {
                    liveServer = null;
                    error("Handy-Zugang", "Der Handy-Zugang konnte nicht gestartet werden (Port " + de.hts.web.LiveServer.PORT
                            + " belegt?): " + ex.getMessage());
                }
            }
            update.run();
        });
        lock.setOnAction(e -> {
            tournament.setCheckinLocked(!tournament.isCheckinLocked());
            setStatus(tournament.isCheckinLocked() ? "Anmeldung per Adresse gesperrt." : "Anmeldung per Adresse wieder geöffnet.");
            update.run();
        });
        qr.setOnAction(e -> {
            String host = de.hts.web.LiveServer.localAddresses().stream().findFirst().orElse(null);
            if (host == null) {
                error("QR-Codes", "Dieser Rechner hat keine Netzwerkverbindung – bitte mit dem WLAN-Router verbinden.");
                return;
            }
            int port = liveServer != null ? liveServer.port() : de.hts.web.LiveServer.PORT;
            String name = "QR-Codes " + (tournament.getName() == null ? "Turnier" : tournament.getName());
            PdfViewer.show(stage, tournament, name, de.hts.report.PdfReports.qrSheet(tournament,
                    de.hts.web.LiveServer.liveUrl(host, port), de.hts.web.LiveServer.checkinUrl(host, port, tournament.getCheckinToken())),
                    name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf", this::setStatus);
            if (liveServer == null) setStatus("Hinweis: Der Handy-Zugang ist noch nicht gestartet.");
        });
        update.run();
        card.getChildren().addAll(state, buttons, codes, hints);
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(card);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    /** QR-Code mit Titel und Adresse (Vorschau zum direkten Scannen vom Bildschirm). */
    private static javafx.scene.Node qrPreview(String title, String url) {
        de.hts.web.QrCode code = de.hts.web.QrCode.encode(url);
        int n = code.size(), quiet = 4, scale = 5, size = (n + 2 * quiet) * scale;
        javafx.scene.image.WritableImage img = new javafx.scene.image.WritableImage(size, size);
        javafx.scene.image.PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean dark = code.get(x / scale - quiet, y / scale - quiet);
                pw.setColor(x, y, dark ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
            }
        }
        Label t = new Label(title);
        t.getStyleClass().add("card-heading");
        Label u = new Label(url);
        u.getStyleClass().add("hint");
        u.setWrapText(true);
        u.setMaxWidth(size + 40);
        VBox box = new VBox(6, t, new javafx.scene.image.ImageView(img), u);
        return box;
    }

    // --- Hilfsmethoden -----------------------------------------------------

    private void setStatus(String text) {
        statusLabel.setText(text);
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
