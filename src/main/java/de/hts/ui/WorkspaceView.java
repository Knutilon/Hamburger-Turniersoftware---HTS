/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
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
    private final javafx.scene.layout.FlowPane startBox = new javafx.scene.layout.FlowPane(6, 6);   // 1.0: bricht bei wenig Platz um
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
        participants.setSeedSource(name -> tournament.findClass(name).map(tournament::seedPositions).orElse(java.util.Map.of()));   // 0.39 (#62)
        participants.setFeeSource(tournament::feeOf, tournament::hasFees);                                                         // 1.0 (#82)
        participants.setPendingMark(p -> tournament.isRunning() && tournament.paymentPending(p));                                  // 1.1 (#89)
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

        // 1.0 (#77): Zeitplan – Rückfragen „vor dem Zeitplan“ stellen, fällige Runden einreihen
        scheduleTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> scheduleTick()));
        scheduleTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        scheduleTimer.play();

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
        scheduleTimer.stop();
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
        if (de.hts.HtsApp.MOBILE_ENABLED) {                // 1.0 (#80): ausgeblendet, Code bleibt
            afterHandicapMenus.add(header.addMenuItem("mobile", "Mobile Einstellungen", null, () -> showPage(Page.MOBILE)));
        }
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
        addExportButtons();
    }

    /**
     * 1.0 (#79): „Gesamtergebnis als PDF“ und „click-TT-Ergebnisdatei (XML)“ als zwei Knöpfe – immer sichtbar, aber erst
     * aktiv, wenn das Turnier gespielt ist (alle Klassen beendet). Vorgabeturnier: kein click-TT-Export.
     */
    private void addExportButtons() {
        boolean done = tournament.isStarted() && tournament.isCompletelyFinished();
        List<String> open = tournament.rootClasses().stream().filter(c -> !c.isFinished()).map(PlayingClass::getName).toList();
        String why = done ? null : !tournament.isStarted() ? "Erst möglich, wenn das Turnier gespielt ist (noch nicht gestartet)"
                : "Erst möglich, wenn alle Klassen beendet sind (noch offen: " + String.join(", ", open) + ")";
        Button pdf = new Button("Gesamtergebnis als PDF");
        pdf.getStyleClass().add("pill");
        pdf.setMinWidth(Region.USE_PREF_SIZE);
        pdf.setOnAction(e -> Reports.showTournamentResults(stage, tournament, this::setStatus));
        startBox.getChildren().add(withDisabledHint(pdf, why));
        if (!tournament.isHandicap()) {
            Button ctt = new Button("click-TT-Ergebnisdatei (XML)");
            ctt.getStyleClass().add("pill");
            ctt.setMinWidth(Region.USE_PREF_SIZE);
            ctt.setOnAction(e -> exportClickTt());
            startBox.getChildren().add(withDisabledHint(ctt, why));
        }
    }

    /** Deaktivierter Knopf mit Hinweis beim Darüberfahren (Tooltip über einen Rahmen, da deaktivierte Knöpfe keinen zeigen). */
    private static Node withDisabledHint(Button b, String why) {
        b.setDisable(why != null);
        if (why == null) return b;
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(b);
        Tooltip.install(wrap, new Tooltip(why));
        return wrap;
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
        Ui.oneLineWhenPossible(startBox);
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
        // 1.1: reicht die Breite nicht für Titel und Knöpfe nebeneinander, rutschen die Knöpfe in eine eigene Zeile
        // darunter (vorher überlagerten sie bei 125 %/150 % Skalierung die Zeile „Gestartet …“)
        HeaderRow titleRow = new HeaderRow(titles, right);
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

    private final javafx.animation.Timeline scheduleTimer;
    private boolean scheduleAsking;
    private int scheduleTicks;

    /** 1.0 (#77): alle 2 s Rückfragen abholen, alle 20 s fällige Runden einreihen. */
    private void scheduleTick() {
        if (scheduleAsking) return;
        if (++scheduleTicks % 10 == 0 && tournament.releaseDueRounds()) onRunningChanged();
        List<Tournament.ScheduleQuestion> qs = tournament.takeScheduleQuestions();
        if (qs.isEmpty()) return;
        scheduleAsking = true;
        Platform.runLater(() -> {
            try {
                boolean changed = false;
                for (Tournament.ScheduleQuestion q : qs) {
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
                    ButtonType now = new ButtonType("Jetzt starten", javafx.scene.control.ButtonBar.ButtonData.YES);
                    ButtonType wait = new ButtonType("Warten bis " + q.planned().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " Uhr",
                            javafx.scene.control.ButtonBar.ButtonData.NO);
                    a.getButtonTypes().setAll(wait, now);
                    a.initOwner(stage);
                    a.setTitle("Zeitplan");
                    a.setHeaderText("Vor dem Zeitplan: " + q.label());
                    a.setContentText("Die vorige Runde ist beendet. Laut Zeitplan beginnt diese Runde erst um "
                            + q.planned().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " Uhr. "
                            + "Trotzdem jetzt starten? Beim Warten werden die Spiele zur geplanten Zeit automatisch eingereiht.");
                    a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    if (a.showAndWait().orElse(wait) == now) {
                        tournament.releaseRound(q.phase(), q.key());
                        changed = true;
                    }
                }
                if (changed) onRunningChanged();
                else classes.refresh();
            } finally {
                scheduleAsking = false;
            }
        });
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
        // 1.0 (#82): offene Startgebühren der zu startenden Klassen (nur Warnung)
        StringBuilder fees = new StringBuilder();
        for (PlayingClass c : r.started()) {
            List<Player> unpaid = tournament.unpaidIn(c);
            if (!unpaid.isEmpty()) fees.append("  • ").append(c.getName()).append(": ")
                    .append(String.join(", ", unpaid.stream().map(Player::getDisplayName).toList())).append("\n");
        }
        if (!fees.isEmpty()) sb.append("Startgebühr noch offen (Start trotzdem möglich):\n").append(fees).append("\n");
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
        // 1.0 (#82): Startgebühr per Rechtsklick
        // 1.1 (#89): Bezahlt – bar / Überweisung, Gebühr erlassen, Zahlung rückgängig
        MenuItem payCash = new MenuItem("Bezahlt – bar");
        payCash.setOnAction(e -> paySelected(de.hts.player.Player.Payment.CASH));
        MenuItem payTransfer = new MenuItem("Bezahlt – Überweisung");
        payTransfer.setOnAction(e -> paySelected(de.hts.player.Player.Payment.TRANSFER));
        MenuItem payWaive = new MenuItem("Gebühr erlassen");
        payWaive.setOnAction(e -> waiveSelected());
        MenuItem payUndo = new MenuItem("Zahlung rückgängig");
        payUndo.setOnAction(e -> undoPaymentSelected());
        MenuItem receipt = new MenuItem("Quittung (PDF)");
        receipt.setOnAction(e -> receiptSelected());
        SeparatorMenuItem paySep = new SeparatorMenuItem();
        ContextMenu menu = new ContextMenu(present, withdraw, undo, confirmReq, paySep, payCash, payTransfer, payWaive, payUndo, receipt,
                new SeparatorMenuItem(), edit, new SeparatorMenuItem(), delete);
        menu.setOnShowing(e -> {
            Player p = participants.getSelected();
            boolean fees = tournament.hasFees() && p != null;
            paySep.setVisible(fees);
            payCash.setVisible(fees && !p.isPaid() && tournament.feeOf(p) > 0);
            payTransfer.setVisible(fees && !p.isPaid() && tournament.feeOf(p) > 0);
            payWaive.setVisible(fees && !p.isPaid() && tournament.feeOf(p) > 0);
            payUndo.setVisible(fees && p.isPaid());
            receipt.setVisible(fees && p.isPaid() && p.getPayment() != de.hts.player.Player.Payment.WAIVED);
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
        // 1.0 (#79): erst prüfen, wessen Spiele mangels click-TT-Nummer fehlen würden – dann Rückfrage / Abgleich
        de.hts.importer.clicktt.ClickTtResultExporter.Report dry;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(original)) {
            dry = de.hts.importer.clicktt.ClickTtResultExporter.export(tournament, in, java.io.OutputStream.nullOutputStream());
        } catch (java.io.IOException ex) {
            error("Export fehlgeschlagen", ex.getMessage());
            return;
        }
        if (!dry.withoutId().isEmpty()) {
            ClickTtExportDialogs.Choice c = ClickTtExportDialogs.askMissing(stage, tournament, dry.withoutId());
            if (c == ClickTtExportDialogs.Choice.ABORT) return;
            if (c == ClickTtExportDialogs.Choice.MATCH) {
                Path updated = matchWithUpdatedList(dry.withoutId());
                if (updated == null) return;                      // abgebrochen: nichts geändert
                original = updated;
            }
        }
        saveClickTtResults(original);
    }

    /**
     * 1.0 (#79): aktualisierte click-TT-Teilnehmerliste einspielen und nachgetragene Teilnehmende zuordnen. Übernimmt die
     * click-TT-Nummern der zugeordneten Personen und hinterlegt die neue Datei als Grundlage des Exports.
     * @return die neue Grundlage oder {@code null} (abgebrochen – nichts geändert)
     */
    private Path matchWithUpdatedList(List<Player> withoutId) {
        FileChooser pick = new FileChooser();
        pick.setTitle("Aktualisierte click-TT-Teilnehmerliste (XML) auswählen");
        pick.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML-Dateien", "*.xml", "*.XML"));
        File lastDir = new File(prefs.get(PREF_LAST_IMPORT_DIR, System.getProperty("user.home")));
        if (lastDir.isDirectory()) pick.setInitialDirectory(lastDir);
        File f = pick.showOpenDialog(stage);
        if (f == null) return null;
        if (f.getParentFile() != null) prefs.put(PREF_LAST_IMPORT_DIR, f.getParentFile().getAbsolutePath());
        de.hts.importer.clicktt.ClickTtXmlImporter.ImportResult r;
        try {
            r = new de.hts.importer.clicktt.ClickTtXmlImporter().importFile(f.toPath());
        } catch (de.hts.importer.clicktt.ClickTtImportException ex) {
            error("Teilnehmerliste nicht lesbar", ex.getMessage());
            return null;
        }
        Tournament.Matching m = tournament.matchManual(withoutId, r.players());
        java.util.Optional<java.util.Map<Player, Player>> res = ClickTtExportDialogs.matchPlayers(stage, tournament, f.getName(), m);
        if (res.isEmpty()) {
            setStatus("Abgleich abgebrochen – nichts geändert.");
            return null;
        }
        int n = 0;
        for (var e : res.get().entrySet()) {
            if (e.getValue() == null) continue;
            tournament.adoptNumbers(e.getKey(), e.getValue());
            n++;
        }
        rememberClickTtFile(f.toPath());
        save();
        setStatus("Teilnehmerliste abgeglichen: " + n + (n == 1 ? " Person zugeordnet." : " Personen zugeordnet."));
        Path copy = clickTtCopy();
        return java.nio.file.Files.isRegularFile(copy) ? copy : f.toPath();
    }

    private void saveClickTtResults(Path original) {
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
            setStatus("click-TT-Ergebnisdatei gespeichert: " + target.getName());
            ClickTtExportDialogs.showReport(stage, tournament, rep, target.toPath(), () -> {
                Path updated = matchWithUpdatedList(rep.withoutId());
                if (updated != null) saveClickTtResults(updated);
            }, this::setStatus);
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
                nc.setFeeCents(ci.feeCents());                              // 1.0 (#82): Startgebühr aus click-TT
                newClasses++;
            } else {
                if (ci.doubles() && !existing.get().isDoubles()) tournament.changeDiscipline(existing.get(), Discipline.DOUBLES);
                if (existing.get().getFeeCents() == null) existing.get().setFeeCents(ci.feeCents());
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
        // 0.39 (#64): Aktionsmenüs im Design der Auswahl „Gleichstand“, Text sagt, wozu sie dienen
        javafx.scene.control.MenuButton marked = new javafx.scene.control.MenuButton("Aktion für Markierte wählen");
        marked.getStyleClass().add("action-menu");
        marked.setMinWidth(Region.USE_PREF_SIZE);
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

        javafx.scene.control.MenuButton phone = new javafx.scene.control.MenuButton("Handyabmeldungen bearbeiten");
        phone.getStyleClass().add("action-menu");
        phone.setMinWidth(Region.USE_PREF_SIZE);
        phone.managedProperty().bind(phone.visibleProperty());
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
        // 0.38 (#54): PDF der endgültig Abgemeldeten – nur aktiv, wenn mindestens eine Person abgemeldet ist
        Button pdf = new Button("Abgemeldete als PDF");
        pdf.getStyleClass().add("pill");
        pdf.setMinWidth(Region.USE_PREF_SIZE);
        pdf.setTooltip(new Tooltip("Liste der endgültig Abgemeldeten je Klasse (Name, Vorname, Verein, TTR, abgemeldet durch)"));
        pdf.setOnAction(e -> Reports.showWithdrawn(getRoot().getScene() == null ? null : getRoot().getScene().getWindow(), tournament, this::setStatus));
        Runnable updatePdf = () -> {
            pdf.setDisable(tournament.getPlayers().stream()
                    .noneMatch(p -> p.getAttendance() == de.hts.player.Attendance.WITHDRAWN && !p.isReserveOnly()));
            // 0.39 (#66): „Handyabmeldungen“ nur bei laufendem Handy-Zugang oder noch offenen Handyabmeldungen
            phone.setVisible((liveServer != null && liveServer.isRunning()) || tournament.getPlayers().stream()
                    .anyMatch(p -> p.getAttendance() == de.hts.player.Attendance.WITHDRAW_REQUESTED));
        };
        updatePdf.run();
        participants.countTextProperty().addListener((o, a, b) -> updatePdf.run());
        // 1.0 (#82): Startgebühren – Markierte als bezahlt, Verein abrechnen, Kassenübersicht
        MenuItem mPayCash = new MenuItem("Bezahlt – bar");
        mPayCash.setOnAction(e -> paySelected(de.hts.player.Player.Payment.CASH));
        MenuItem mPayTransfer = new MenuItem("Bezahlt – Überweisung");
        mPayTransfer.setOnAction(e -> paySelected(de.hts.player.Player.Payment.TRANSFER));
        MenuItem mPayWaive = new MenuItem("Gebühr erlassen");
        mPayWaive.setOnAction(e -> waiveSelected());
        MenuItem mPayUndo = new MenuItem("Zahlung rückgängig");
        mPayUndo.setOnAction(e -> undoPaymentSelected());
        MenuItem mReceipt = new MenuItem("Quittung für Markierte (PDF)");
        mReceipt.setOnAction(e -> receiptSelected());
        SeparatorMenuItem feeSep = new SeparatorMenuItem();
        marked.getItems().addAll(feeSep, mPayCash, mPayTransfer, mPayWaive, mPayUndo, mReceipt);
        marked.setOnShowing(e -> {
            boolean fees = tournament.hasFees();
            for (MenuItem mi : new MenuItem[]{feeSep, mPayCash, mPayTransfer, mPayWaive, mPayUndo, mReceipt}) mi.setVisible(fees);
        });
        javafx.scene.control.MenuButton fees = new javafx.scene.control.MenuButton("Startgebühren");
        fees.getStyleClass().add("action-menu");
        fees.setMinWidth(Region.USE_PREF_SIZE);
        fees.managedProperty().bind(fees.visibleProperty());
        MenuItem club = new MenuItem("Verein abrechnen …");
        club.setOnAction(e -> settleClub());
        MenuItem cash = new MenuItem("Kassenübersicht (PDF)");
        cash.setOnAction(e -> Reports.showCashReport(getRoot().getScene() == null ? null : getRoot().getScene().getWindow(), tournament, this::setStatus));
        fees.getItems().addAll(club, cash);
        fees.setTooltip(new Tooltip("Startgebühren je Verein abrechnen (mit Quittung) oder die Kassenübersicht drucken"));
        fees.setVisible(tournament.hasFees());
        participants.countTextProperty().addListener((o, a, b) -> fees.setVisible(tournament.hasFees()));
        // 0.39 (#64): in einer Zeile, PDF rechts neben den Menüs (Umbruch nur, wenn es wirklich eng wird)
        javafx.scene.layout.FlowPane box = new javafx.scene.layout.FlowPane(8, 8, marked, phone, fees, pdf);
        Ui.oneLineWhenPossible(box);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // --- Startgebühren (1.0, #82) ----------------------------------------------------

    private List<Player> selectedPlayers() {
        return new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems());
    }

    private void paySelected(de.hts.player.Player.Payment how) {
        List<Player> sel = selectedPlayers().stream().filter(p -> !p.isPaid() && tournament.feeOf(p) > 0).toList();
        if (sel.isEmpty()) {
            setStatus(participants.getTable().getSelectionModel().isEmpty() ? "Bitte zuerst Teilnehmende markieren."
                    : "Unter den Markierten ist keine offene Startgebühr.");
            return;
        }
        tournament.markPaid(sel, how, null);
        int sum = sel.stream().mapToInt(de.hts.player.Player::getPaidCents).sum();
        setStatus(sel.size() + (sel.size() == 1 ? " Startgebühr" : " Startgebühren") + " bezahlt (" + how.getLabel() + "): "
                + Tournament.euro(sum) + ".");
        refreshAfterAttendance();
    }

    /** 1.1 (#89): Gebühr der Markierten erlassen (mit optionalem Grund). */
    private void waiveSelected() {
        List<Player> sel = selectedPlayers().stream().filter(p -> !p.isPaid() && tournament.feeOf(p) > 0).toList();
        if (sel.isEmpty()) {
            setStatus(participants.getTable().getSelectionModel().isEmpty() ? "Bitte zuerst Teilnehmende markieren."
                    : "Unter den Markierten ist keine offene Startgebühr.");
            return;
        }
        FeeWaiveDialog d = new FeeWaiveDialog(stage, tournament, sel);
        String reason = d.showAndWait().orElse(null);
        if (reason == null) return;
        int sum = sel.stream().mapToInt(tournament::feeOf).sum();
        tournament.waiveFee(sel, reason);
        setStatus(sel.size() + (sel.size() == 1 ? " Startgebühr" : " Startgebühren") + " erlassen: " + Tournament.euro(sum) + ".");
        refreshAfterAttendance();
    }

    private void undoPaymentSelected() {
        List<Player> sel = selectedPlayers().stream().filter(de.hts.player.Player::isPaid).toList();
        if (sel.isEmpty()) {
            setStatus("Unter den Markierten hat niemand bezahlt.");
            return;
        }
        if (!confirm("Zahlung rückgängig", (sel.size() == 1 ? "Die Zahlung von " + sel.get(0).getDisplayName() : sel.size() + " Zahlungen")
                + " rückgängig machen? Die Startgebühr ist danach wieder offen; „angemeldet“ und die Klassen bleiben.")) return;
        tournament.undoPayment(sel);
        setStatus(sel.size() + " Zahlung(en) zurückgenommen.");
        refreshAfterAttendance();
    }

    private void receiptSelected() {
        List<Player> sel = selectedPlayers().stream().filter(de.hts.player.Player::isPaid).toList();
        if (sel.isEmpty()) {
            setStatus("Quittung: Unter den Markierten hat niemand bezahlt.");
            return;
        }
        String who = sel.size() == 1 ? sel.get(0).getDisplayName() : sel.size() + " Personen";
        Reports.showReceipt(getRoot().getScene() == null ? null : getRoot().getScene().getWindow(), tournament, sel, who, null, this::setStatus);
    }

    /** Sammelabrechnung eines Vereins: offene Gebühren seiner Mitglieder auf einmal bezahlen, mit Quittung. */
    private void settleClub() {
        java.text.Collator col = java.text.Collator.getInstance(java.util.Locale.GERMAN);
        List<String> clubs = tournament.getPlayers().stream().filter(p -> tournament.feeOf(p) > 0)
                .map(Player::getClubName).filter(c -> c != null && !c.isBlank()).distinct().sorted(col).toList();
        if (clubs.isEmpty()) {
            setStatus("Keine Startgebühren mit Verein vorhanden.");
            return;
        }
        ClubSettlementDialog d = new ClubSettlementDialog(stage, tournament, clubs);
        d.showAndWait().ifPresent(r -> {
            if (!r.toPay().isEmpty()) {
                tournament.markPaid(r.toPay(), r.how(), r.club());
                setStatus("„" + r.club() + "“: " + r.toPay().size() + (r.toPay().size() == 1 ? " Startgebühr" : " Startgebühren")
                        + " bezahlt (" + r.how().getLabel() + "), " + Tournament.euro(r.toPay().stream().mapToInt(Player::getPaidCents).sum()) + ".");
                refreshAfterAttendance();
            }
            if (r.receipt()) {
                List<Player> paid = tournament.getPlayers().stream().filter(p -> r.club().equals(p.getClubName()) && p.isPaid()).toList();
                if (!paid.isEmpty()) Reports.showReceipt(stage, tournament, paid, r.club(), r.club(), this::setStatus);
            }
        });
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

    private boolean askingPhone;

    /** 1.1 (#93): per Handy gemeldete Ergebnisse bestätigen (übernehmen oder verwerfen). */
    private void askPhoneResults() {
        if (askingPhone) return;
        if (tournament.isPhoneResultsDirect()) {
            onRunningChanged();
            return;
        }
        askingPhone = true;
        try {
            for (Tournament.PhoneResult r : tournament.pendingPhoneResults()) {
                de.hts.tournament.Match m = r.match();
                ButtonType take = new ButtonType("Übernehmen", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                ButtonType drop = new ButtonType("Verwerfen", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        tournament.entrantName(m, m.getA()) + "  –  " + tournament.entrantName(m, m.getB()) + "\n" + r.setsText(), take, drop);
                a.initOwner(stage);
                a.setTitle("Ergebnis vom Handy");
                a.setHeaderText("Ergebnis vom Handy: " + tournament.tableName(r.table()) + " · " + m.getClassName() + " · Spiel " + m.getNumber());
                if (a.showAndWait().orElse(drop) == take) {
                    try {
                        tournament.acceptPhoneResult(r);
                        setStatus("Spiel " + m.getNumber() + ": Ergebnis vom Handy übernommen (" + r.setsText() + ").");
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        error("Ergebnis vom Handy", ex.getMessage());
                    }
                } else {
                    tournament.rejectPhoneResult(r);
                    setStatus("Spiel " + m.getNumber() + ": Ergebnis vom Handy verworfen.");
                }
            }
        } finally {
            askingPhone = false;
        }
        onRunningChanged();
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
                    liveServer = new de.hts.web.LiveServer(tournament, javafx.application.Platform::runLater, () -> {
                        refreshAfterAttendance();
                        askPhoneResults();                       // 1.1 (#93)
                    });
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
        // 1.1 (#93): Ergebnisse per Handy eintragen (Standard aus)
        javafx.scene.control.CheckBox phone = new javafx.scene.control.CheckBox("Ergebnisse per Handy eintragen (je Tisch ein QR-Code)");
        phone.setSelected(tournament.isPhoneResults());
        phone.setWrapText(true);
        javafx.scene.control.CheckBox direct = new javafx.scene.control.CheckBox("Handy-Ergebnisse direkt übernehmen (ohne Bestätigung)");
        direct.setSelected(tournament.isPhoneResultsDirect());
        direct.setWrapText(true);
        direct.disableProperty().bind(phone.selectedProperty().not());
        Button tableQr = new Button("QR-Codes für die Tische drucken");
        tableQr.getStyleClass().add("pill");
        tableQr.disableProperty().bind(phone.selectedProperty().not());
        phone.selectedProperty().addListener((o, a, b) -> {
            tournament.setPhoneResults(b);
            setStatus(b ? "Ergebnisse per Handy eingeschaltet." : "Ergebnisse per Handy ausgeschaltet.");
        });
        direct.selectedProperty().addListener((o, a, b) -> tournament.setPhoneResultsDirect(b));
        tableQr.setOnAction(e -> {
            String host = de.hts.web.LiveServer.localAddresses().stream().findFirst().orElse(null);
            if (host == null) {
                error("QR-Codes", "Dieser Rechner hat keine Netzwerkverbindung – bitte mit dem WLAN-Router verbinden.");
                return;
            }
            int port = liveServer != null ? liveServer.port() : de.hts.web.LiveServer.PORT;
            java.util.Map<Integer, String> urls = new java.util.LinkedHashMap<>();
            for (int tb = 1; tb <= tournament.getTableCount(); tb++) urls.put(tb, de.hts.web.LiveServer.resultUrl(host, port, tournament, tb));
            String name = "Tisch-QR-Codes " + (tournament.getName() == null ? "Turnier" : tournament.getName());
            PdfViewer.show(stage, tournament, name, de.hts.report.PdfReports.tableQrSheet(tournament, urls),
                    name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf", this::setStatus);
        });
        Label phoneHint = new Label("Am Tisch den QR-Code scannen: Das Handy zeigt das laufende Spiel, die Sätze werden wie in HTS geprüft. "
                + "Gemeldete Ergebnisse erscheinen hier im Programm zur Bestätigung (übernehmen oder verwerfen).");
        phoneHint.getStyleClass().add("hint");
        phoneHint.setWrapText(true);
        phoneHint.setMinHeight(Region.USE_PREF_SIZE);
        VBox phoneBox = new VBox(8, phone, direct, tableQr, phoneHint);
        update.run();
        card.getChildren().addAll(state, buttons, codes, phoneBox, hints);
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

    /** 1.1: Titel links, Knöpfe rechts – bei zu wenig Breite die Knöpfe in einer eigenen Zeile (rechtsbündig) darunter. */
    static final class HeaderRow extends javafx.scene.layout.Pane {
        private static final double GAP = 12;
        private final Region left;
        private final Region right;

        HeaderRow(Region left, Region right) {
            this.left = left;
            this.right = right;
            getChildren().addAll(left, right);
        }

        private double innerWidth(double w) {
            return w - snappedLeftInset() - snappedRightInset();
        }

        @Override
        public javafx.geometry.Orientation getContentBias() {
            return javafx.geometry.Orientation.HORIZONTAL;       // Höhe hängt von der Breite ab (eine oder zwei Zeilen)
        }

        private boolean oneRow(double w) {
            return w < 0 || left.prefWidth(-1) + GAP + right.prefWidth(-1) <= innerWidth(w) + 0.5;
        }

        @Override
        protected double computePrefWidth(double h) {
            return snappedLeftInset() + snappedRightInset() + left.prefWidth(-1) + GAP + right.prefWidth(-1);
        }

        @Override
        protected double computeMinWidth(double h) {
            return snappedLeftInset() + snappedRightInset() + left.minWidth(-1);
        }

        @Override
        protected double computePrefHeight(double w) {
            double ins = snappedTopInset() + snappedBottomInset();
            if (oneRow(w)) return ins + Math.max(left.prefHeight(-1), right.prefHeight(right.prefWidth(-1)));
            return ins + left.prefHeight(-1) + GAP / 2 + right.prefHeight(innerWidth(w));
        }

        @Override
        protected double computeMinHeight(double w) {
            return computePrefHeight(w);
        }

        @Override
        protected void layoutChildren() {
            double x0 = snappedLeftInset(), y0 = snappedTopInset(), iw = innerWidth(getWidth());
            double lw = Math.min(left.prefWidth(-1), iw), lh = left.prefHeight(lw);
            if (oneRow(getWidth())) {
                double rw = right.prefWidth(-1), rh = right.prefHeight(rw);
                double h = Math.max(lh, rh);
                left.resizeRelocate(x0, y0 + (h - lh) / 2, lw, lh);
                right.resizeRelocate(x0 + iw - rw, y0 + (h - rh) / 2, rw, rh);
            } else {
                left.resizeRelocate(x0, y0, lw, lh);
                double rh = right.prefHeight(iw);
                right.resizeRelocate(x0, y0 + lh + GAP / 2, iw, rh);
            }
        }
    }
}
