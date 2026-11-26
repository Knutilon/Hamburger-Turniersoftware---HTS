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
import de.hts.tournament.Match;
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

    private enum Page { PARTICIPANTS, HANDICAP, CLASSES, TABLES, MATCHES, PRESENTATION, CERTIFICATES, PUBLISH }

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
    private final CertificatesView certificatesView;                 // 1.4.0 (#113)
    private javafx.scene.control.Button certMenu;
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
        participants.setOnlineInfo(tournament::isOnlineMarked, tournament::clearOnlineMark);   // 1.9.0 (#164)
        this.classes = new ClassesView(tournament, this::setStatus, participants::refresh, this::onRunningChanged);
        this.tablesView = new TablesView(tournament, this::setStatus, this::onRunningChanged);
        this.matchesView = new MatchesView(tournament, this::setStatus, this::onRunningChanged);
        this.presentationView = new PresentationView(tournament, this::setStatus);
        this.certificatesView = new CertificatesView(tournament, this::setStatus);
        // 1.4.1: Klassen, die beim Öffnen schon beendet sind (auch aus älteren Dateien), lösen keine Frage mehr aus
        for (PlayingClass c : tournament.rootClasses()) {
            String k = tournament.certAskKey(c);
            if (k != null) tournament.restoreCertAsked(k);
        }
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
            if (isModified && publisher != null) publisher.changed();      // 1.5.0 (#136)
        });
        updateSaveState();
        updatePublisher();

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
        if (publisher != null) {                         // letzter Stand: nach dem Speichern noch einmal (gedrosselt) – hier beenden
            publisher.close();
            publisher = null;
        }
        if (publishTimer != null) publishTimer.stop();
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
        afterHandicapMenus.add(header.addMenuItem("publish", "Live im Internet", null, () -> showPage(Page.PUBLISH)));   // 1.5.0 (#135)
        if (tournament.isCertTabShown() || tournament.isStarted()) showCertMenu();   // 1.4.1: ab Turnierstart (Urkunden manuell)
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
        publishState.getStyleClass().add("publish-state");
        publishState.managedProperty().bind(publishState.visibleProperty());
        publishState.setVisible(false);
        HBox statusBar = new HBox(8, statusLabel, sbSpacer, publishState, count);
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

    private HBox slipDesignBox;

    // --- 1.5.0 (#135–#138): Live im Internet -------------------------------------------------------------

    private de.hts.web.Publisher publisher;
    private javafx.animation.Timeline publishTimer;
    private final Label publishState = new Label();

    /** Startet bzw. beendet das Hochladen je nach Schalter des Turniers und Programmeinstellung. */
    private void updatePublisher() {
        boolean on = tournament.isPublish() && de.hts.web.WebSettings.configured();
        if (on && publisher == null) {
            publisher = new de.hts.web.Publisher(de.hts.web.WebSettings.server(), de.hts.web.WebSettings.key(), tournament.getId(),
                    Platform::runLater, () -> de.hts.persistence.TournamentFile.publishBytes(tournament),
                    () -> de.hts.persistence.Organizer.get().logoPng());
            publisher.setTransform(de.hts.persistence.TournamentFile::abbreviateFirstNames);     // 1.8.0 (#156): immer abgekürzt
            publisher.setOrganizer(() -> de.hts.persistence.Organizer.get().getName());          // 1.8.0 (#150)
            publisher.setCheckinPoll(this::checkinPollActive, this::applyOnlineReports);         // 1.9.0 (#164)
            de.hts.web.PendingDeletes.retryLater();                                              // 1.8.0 (#154)
            if (publishTimer == null) {
                publishTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> showPublishState()));
                publishTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            }
            publishTimer.play();
        } else if (!on && publisher != null) {
            publisher.close();
            publisher = null;
            if (publishTimer != null) publishTimer.stop();
        }
        showPublishState();
    }

    /** 1.9.0 (#164): Meldungen abholen – Schalter an, Turniertag, bis 30 Minuten nach „Online-Anmeldung bis“. */
    private boolean checkinPollActive() {
        if (!tournament.isOnlineCheckin() || tournament.getCheckinUntil() == null) return false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return now.toLocalDate().equals(tournament.getDate()) && now.toLocalTime().isBefore(tournament.getCheckinUntil().plusMinutes(30));
    }

    /** 1.9.0 (#164): online gemeldete An- und Abmeldungen übernehmen (im Programm-Thread). */
    private void applyOnlineReports(List<de.hts.web.Publisher.CheckinReport> list) {
        int in = 0, out = 0;
        for (de.hts.web.Publisher.CheckinReport r : list) {
            Player p = tournament.getPlayers().stream().filter(x -> x.getId().equals(r.id())).findFirst().orElse(null);
            if (p == null || !tournament.checkinOnline(p, r.present())) continue;
            if (r.present()) in++;
            else out++;
        }
        if (in + out == 0) return;
        de.hts.Log.info("Online-Anmeldung: " + in + " angemeldet, " + out + " abgemeldet");
        refreshAfterAttendance();
        setStatus("Online-Anmeldung: " + (in > 0 ? in + " angemeldet" : "") + (in > 0 && out > 0 ? ", " : "") + (out > 0 ? out + " abgemeldet" : "") + ".");
    }

    /** Statusanzeige unten rechts: „Live: veröffentlicht vor 5 s“ bzw. „Live: keine Verbindung – wird nachgeholt“. */
    private void showPublishState() {
        if (publisher == null) {
            publishState.setVisible(false);
            return;
        }
        de.hts.web.Publisher.Status st = publisher.status();
        publishState.setVisible(true);
        publishState.getStyleClass().removeAll("publish-ok", "publish-error");
        String ago = st.lastSuccessMillis() == 0 ? "" : agoText(System.currentTimeMillis() - st.lastSuccessMillis());
        if (st.state() == de.hts.web.Publisher.State.OK && !tournament.isPublishedOnce()) tournament.setPublishedOnce(true);   // 1.8.0 (#154)
        switch (st.state()) {
            case OK -> {
                publishState.setText("Live: veröffentlicht " + ago);
                publishState.getStyleClass().add("publish-ok");
            }
            case ERROR -> {
                publishState.setText("Live: " + st.message() + (ago.isEmpty() ? "" : " (zuletzt " + ago + ")"));
                publishState.getStyleClass().add("publish-error");
            }
            default -> publishState.setText("Live: " + st.message());
        }
    }

    static String agoText(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s < 60) return "vor " + s + " s";
        long m = s / 60;
        if (m < 60) return "vor " + m + " min";
        return "vor " + (m / 60) + " h";
    }

    private Node publishPage() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(18, 20, 18, 20));
        box.setMaxWidth(820);
        Label intro = new Label("Zuschauer und Spieler sehen Tische, Gruppen, KO-Baum und Ergebnisse dieses Turniers im Internet. HTS lädt "
                + "nach jeder Änderung den Stand hoch (komprimiert, höchstens alle 30 Sekunden) – vom Server kommt nichts zurück.");
        intro.setWrapText(true);
        intro.setMinHeight(Region.USE_PREF_SIZE);
        if (!de.hts.web.WebSettings.configured()) {
            Label need = new Label("Noch kein Webserver eingetragen: in der Turnierübersicht unter „Webserver“ Adresse und Schlüssel eintragen.");
            need.getStyleClass().add("warning");
            need.setWrapText(true);
            need.setMinHeight(Region.USE_PREF_SIZE);
            box.getChildren().addAll(intro, need);
        } else {
            box.getChildren().add(intro);
        }
        javafx.scene.control.CheckBox on = new javafx.scene.control.CheckBox();
        on.setSelected(tournament.isPublish());
        on.setDisable(!de.hts.web.WebSettings.configured());
        Label onText = new Label("Dieses Turnier veröffentlichen");
        onText.getStyleClass().add("class-name");
        on.selectedProperty().addListener((o, a, b) -> {
            tournament.setPublish(b);
            updatePublisher();
            setStatus(b ? "Veröffentlichen eingeschaltet." : "Veröffentlichen ausgeschaltet – die Daten bleiben auf dem Server, bis sie gelöscht werden.");
        });
        // 1.8.0 (#156): Vornamen immer abgekürzt – kein Schalter mehr
        Label abbrText = new Label("Auf dem Webserver erscheinen die Vornamen nur abgekürzt (z. B. „M. Mustermann“).");
        abbrText.getStyleClass().add("hint");
        abbrText.setWrapText(true);
        abbrText.setMinHeight(Region.USE_PREF_SIZE);
        box.getChildren().addAll(new HBox(6, on, onText), abbrText);
        String world = Tournament.validWorldUrl(tournament.getWorldUrl());              // 1.8.0 (#151)
        if (world != null) {
            javafx.scene.control.Hyperlink wl = new javafx.scene.control.Hyperlink("Turnierwelt (myTischtennis): " + world);
            wl.setWrapText(true);
            wl.setOnAction(e -> de.hts.HtsApp.openLink(world));
            Label wh = new Label("Der Link erscheint auf den Live-Seiten als „zur Turnierwelt“ (ändern unter „Turnier bearbeiten“).");
            wh.getStyleClass().add("hint");
            wh.setWrapText(true);
            wh.setMinHeight(Region.USE_PREF_SIZE);
            box.getChildren().addAll(wl, wh);
        }
        if (de.hts.web.WebSettings.configured()) {
            String url = de.hts.web.Publisher.publicUrl(de.hts.web.WebSettings.server(), tournament.getName(), tournament.getDate());
            Label urlLbl = new Label("Adresse für Zuschauer: " + url);
            urlLbl.setWrapText(true);
            urlLbl.setMinHeight(Region.USE_PREF_SIZE);
            Button copy = new Button("Adresse kopieren");
            copy.getStyleClass().add("pill");
            copy.setMinWidth(Region.USE_PREF_SIZE);
            copy.setOnAction(e -> {
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(url);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                setStatus("Adresse kopiert.");
            });
            Button poster = new Button("QR-Code-Aushang drucken");                   // 1.7.0 (#148)
            poster.getStyleClass().add("pill");
            poster.setMinWidth(Region.USE_PREF_SIZE);
            poster.setOnAction(e -> showLivePoster());
            javafx.scene.layout.FlowPane urlRow = new javafx.scene.layout.FlowPane(10, 8, urlLbl, copy, poster);
            urlRow.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(urlRow);
        }
        box.getChildren().add(onlineCheckinBox());                         // 1.9.0 (#164)
        Label privHead = new Label("Datenschutz");
        privHead.getStyleClass().add("class-name");
        Button del = new Button("Live-Daten löschen");
        del.getStyleClass().add("pill");
        del.setMinWidth(Region.USE_PREF_SIZE);
        Button fin = new Button("Nur Endergebnis behalten");
        fin.getStyleClass().add("pill");
        fin.setMinWidth(Region.USE_PREF_SIZE);
        del.setDisable(!de.hts.web.WebSettings.configured());
        fin.setDisable(!de.hts.web.WebSettings.configured());
        del.setOnAction(e -> serverCommand(true));
        fin.setOnAction(e -> serverCommand(false));
        Label privHint = new Label("„Live-Daten löschen“ entfernt das Turnier vom Server (das Veröffentlichen wird ausgeschaltet). „Nur Endergebnis "
                + "behalten“ lässt auf dem Server nur die Endergebnisse stehen. Die Turnierdatei auf diesem Rechner bleibt immer unverändert.");
        privHint.getStyleClass().add("hint");
        privHint.setWrapText(true);
        privHint.setMinHeight(Region.USE_PREF_SIZE);
        javafx.scene.layout.FlowPane privButtons = new javafx.scene.layout.FlowPane(8, 8, del, fin);
        box.getChildren().addAll(privHead, privButtons, privHint);
        VBox page = new VBox(box);
        page.setPadding(new Insets(0, 40, 16, 40));
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(page);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    /**
     * 1.9.0 (#164): Bereich „Online-Anmeldung zum Turnier“ auf der Seite „Live im Internet“ – Schalter, Uhrzeit, Link,
     * QR-Code-Aushang und neuer Link.
     */
    private Node onlineCheckinBox() {
        Label head = new Label("Online-Anmeldung zum Turnier");
        head.getStyleClass().add("class-name");
        javafx.scene.control.CheckBox on = new javafx.scene.control.CheckBox("Online-Anmeldung einschalten");
        on.setSelected(tournament.isOnlineCheckin());
        javafx.scene.control.TextField until = new javafx.scene.control.TextField(tournament.getCheckinUntil() == null ? "" : tournament.getCheckinUntil().toString());
        until.setPromptText("z. B. 10:30");
        until.setPrefColumnCount(6);
        until.setMaxWidth(Region.USE_PREF_SIZE);
        Label untilLbl = new Label("Online-Anmeldung bis (Uhrzeit am Turniertag):");
        untilLbl.setWrapText(true);
        Label info = new Label();
        info.getStyleClass().add("hint");
        info.setWrapText(true);
        info.setMinHeight(Region.USE_PREF_SIZE);
        Label link = new Label();
        link.setWrapText(true);
        link.setMinHeight(Region.USE_PREF_SIZE);
        Button copy = new Button("Link kopieren");
        Button poster = new Button("QR-Code-Aushang drucken");
        Button renew = new Button("Neuen Anmelde-Link erzeugen");
        for (Button b : new Button[]{copy, poster, renew}) {
            b.getStyleClass().add("pill");
            b.setMinWidth(Region.USE_PREF_SIZE);
        }
        Runnable show = () -> {
            boolean web = de.hts.web.WebSettings.configured();
            String url = web ? de.hts.web.Publisher.normalize(de.hts.web.WebSettings.server()) + "/anmeldung/" + tournament.getCheckinToken() : "";
            link.setText(web ? "Anmelde-Link: " + url : "Noch kein Webserver eingetragen (Turnierübersicht → „Webserver“).");
            boolean ready = web && tournament.getCheckinUntil() != null;
            on.setDisable(!ready && !on.isSelected());
            copy.setDisable(!web);
            poster.setDisable(!web);
            renew.setDisable(!web);
            String state;
            if (tournament.getCheckinUntil() == null) state = "Bitte zuerst die Uhrzeit eintragen.";
            else if (!tournament.isOnlineCheckin()) state = "Ausgeschaltet – online ist keine An- oder Abmeldung möglich.";
            else if (!tournament.isPublish()) state = "Eingeschaltet, aber das Turnier wird nicht veröffentlicht – bitte oben „Dieses Turnier veröffentlichen“ einschalten.";
            else if (tournament.isOnlineCheckinOpen(java.time.LocalDateTime.now())) state = "Offen bis " + tournament.getCheckinUntil() + " Uhr.";
            else state = "Eingeschaltet – offen am Turniertag bis " + tournament.getCheckinUntil() + " Uhr.";
            info.setText(state + " Spieler/innen melden sich über den QR-Code an oder ab, Betreuer/innen auch für ihren ganzen Verein. "
                    + "Eine Online-Abmeldung heißt „nicht angemeldet“ (endgültig abmelden bleibt bei der Turnierleitung). Danach ist "
                    + "die Anwesenheit hier weiter änderbar, bis Phase 1 der Klasse gestartet ist.");
        };
        on.selectedProperty().addListener((o, a, b) -> {
            tournament.setOnlineCheckin(b);
            if (publisher != null) publisher.changed();
            show.run();
            setStatus(b ? "Online-Anmeldung eingeschaltet." : "Online-Anmeldung ausgeschaltet – der Link nimmt keine Meldungen mehr an.");
        });
        until.focusedProperty().addListener((o, a, f) -> {
            if (f) return;
            String txt = until.getText() == null ? "" : until.getText().strip().replace('.', ':');
            try {
                java.time.LocalTime lt = txt.isEmpty() ? null : java.time.LocalTime.parse(txt.length() == 4 ? "0" + txt : txt);
                tournament.setCheckinUntil(lt);
                if (lt == null && on.isSelected()) on.setSelected(false);
                until.setText(lt == null ? "" : lt.toString());
                if (publisher != null) publisher.changed();
            } catch (java.time.format.DateTimeParseException ex) {
                setStatus("Bitte die Uhrzeit als SS:MM eingeben, z. B. 10:30.");
                until.setText(tournament.getCheckinUntil() == null ? "" : tournament.getCheckinUntil().toString());
            }
            show.run();
        });
        until.setOnAction(e -> on.requestFocus());
        copy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(de.hts.web.Publisher.normalize(de.hts.web.WebSettings.server()) + "/anmeldung/" + tournament.getCheckinToken());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            setStatus("Anmelde-Link kopiert.");
        });
        poster.setOnAction(e -> {
            String url = de.hts.web.Publisher.normalize(de.hts.web.WebSettings.server()) + "/anmeldung/" + tournament.getCheckinToken();
            String name = "Online-Anmeldung " + (tournament.getName() == null ? "Turnier" : tournament.getName());
            PdfViewer.show(stage, tournament, name, de.hts.report.PdfReports.checkinQrPoster(tournament, url,
                    de.hts.persistence.Organizer.get().logoPng()), name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf", this::setStatus);
        });
        renew.setOnAction(e -> {
            if (!confirm("Neuen Anmelde-Link erzeugen", "Der bisherige Link und schon ausgedruckte QR-Codes werden sofort ungültig. Weiter?")) return;
            tournament.newCheckinToken();
            if (publisher != null) publisher.changed();
            show.run();
            setStatus("Neuer Anmelde-Link erzeugt – bitte den QR-Code-Aushang neu drucken.");
        });
        show.run();
        javafx.scene.layout.FlowPane untilRow = new javafx.scene.layout.FlowPane(8, 6, untilLbl, until);
        untilRow.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.FlowPane linkRow = new javafx.scene.layout.FlowPane(8, 8, copy, poster, renew);
        VBox v = new VBox(8, head, on, untilRow, link, linkRow, info);
        v.setPadding(new Insets(6, 0, 6, 0));
        return v;
    }

    /** „Live-Daten löschen“ bzw. „Nur Endergebnis behalten“ – mit Rückfrage, im Hintergrund gesendet. */
    private void serverCommand(boolean delete) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(stage);
        a.setTitle(delete ? "Live-Daten löschen" : "Nur Endergebnis behalten");
        a.setHeaderText(delete ? "Dieses Turnier vom Webserver löschen?" : "Auf dem Webserver nur die Endergebnisse behalten?");
        a.setContentText(delete ? "Zuschauer sehen das Turnier danach nicht mehr. Das Veröffentlichen wird ausgeschaltet."
                : "Tische, Gruppen, KO-Baum und Spiele verschwinden vom Server; die Endergebnisse bleiben. Das Veröffentlichen wird ausgeschaltet, "
                + "damit nichts neu hochgeladen wird.");
        ButtonType yes = new ButtonType(delete ? "Löschen" : "Nur Endergebnis behalten", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        a.getButtonTypes().setAll(ButtonType.CANCEL, yes);
        DialogSizing.fit(a);
        if (a.showAndWait().orElse(null) != yes) return;
        tournament.setPublish(false);
        updatePublisher();
        de.hts.web.Publisher p = new de.hts.web.Publisher(de.hts.web.WebSettings.server(), de.hts.web.WebSettings.key(), tournament.getId(),
                r -> { }, () -> new byte[0], () -> null, Long.MAX_VALUE, Long.MAX_VALUE);
        Thread th = new Thread(() -> {
            String msg;
            try {
                if (delete) p.deleteOnServer();
                else p.keepOnlyFinalResults();
                msg = delete ? "Live-Daten auf dem Webserver gelöscht." : "Auf dem Webserver stehen nur noch die Endergebnisse.";
            } catch (java.io.IOException | InterruptedException ex) {
                msg = "Webserver: " + (ex.getMessage() == null ? "nicht erreichbar" : ex.getMessage());
            } finally {
                p.close();
            }
            String fm = msg;
            Platform.runLater(() -> {
                setStatus(fm);
                if (currentPage == Page.PUBLISH) showPage(Page.PUBLISH);
            });
        }, "HTS-Webserver-Befehl");
        th.setDaemon(true);
        th.start();
    }

    /**
     * 1.5.0 (#131): „Letztes Ergebnis zurücknehmen“ (Tische und Spiele) – für Tippfehler, ohne den Weg über die
     * Korrektur; gesperrt mit Hinweis, wenn es nicht mehr gefahrlos geht.
     */
    private final javafx.scene.layout.StackPane undoHolder = new javafx.scene.layout.StackPane();

    private Node undoResultButton() {
        refreshUndo();
        return undoHolder;
    }

    private void refreshUndo() {
        undoHolder.getChildren().setAll(buildUndoButton());
    }

    private Node buildUndoButton() {
        Button b = new Button("Letztes Ergebnis zurücknehmen");
        b.getStyleClass().add("pill");
        b.setMinWidth(Region.USE_PREF_SIZE);
        Match m = tournament.undoableResult();
        if (m != null) {
            b.setTooltip(new Tooltip("Spiel " + m.getNumber() + " (" + m.getClassName() + "): " + tournament.entrantName(m, m.getA()) + " – "
                    + tournament.entrantName(m, m.getB()) + " " + m.getResultText()));
        }
        b.setOnAction(e -> {
            Match u = tournament.undoableResult();
            if (u == null) return;
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.initOwner(stage);
            a.setTitle("Letztes Ergebnis zurücknehmen");
            a.setHeaderText("Ergebnis von Spiel " + u.getNumber() + " zurücknehmen?");
            a.setContentText(tournament.entrantName(u, u.getA()) + " – " + tournament.entrantName(u, u.getB()) + "  " + u.getResultText()
                    + "\n\nDas Spiel läuft danach wieder (am bisherigen Tisch, wenn er frei ist, sonst in der Warteschlange).");
            ButtonType yes = new ButtonType("Zurücknehmen", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            a.getButtonTypes().setAll(ButtonType.CANCEL, yes);
            DialogSizing.fit(a);
            if (a.showAndWait().orElse(null) != yes) return;
            try {
                Match back = tournament.undoLastResult();
                setStatus("Ergebnis von Spiel " + back.getNumber() + " zurückgenommen.");
            } catch (IllegalStateException ex) {
                setStatus(ex.getMessage());
            }
            onRunningChanged();
            showPage(currentPage);
        });
        return withDisabledHint(b, m == null ? "Kein Ergebnis zum Zurücknehmen – seit dem Öffnen wurde noch keins eingetragen, oder es "
                + "hängt schon etwas davon ab (dann über „Ergebnis korrigieren“)." : null);
    }

    /**
     * 1.4.0 (#108): Auswahl „Zettel-Design“ (Spielerzettel oder Schiedsrichterzettel) oben rechts im Reiter „Tische“;
     * gilt für alle Spielzettel (Einzel und Doppel) dieses Turniers.
     */
    private HBox slipDesignControl() {
        if (slipDesignBox != null) return slipDesignBox;
        Label lbl = new Label("Zettel-Design:");
        lbl.getStyleClass().add("class-name");
        lbl.setMinWidth(Region.USE_PREF_SIZE);
        javafx.scene.control.ComboBox<de.hts.tournament.SlipDesign> box = new javafx.scene.control.ComboBox<>();
        box.getItems().setAll(de.hts.tournament.SlipDesign.values());
        box.setValue(tournament.getSlipDesign());
        box.setPrefWidth(210);                          // 1.5.0: feste Breite (Beschreibungen nur in der Liste) – passt auch hochkant
        box.setMinWidth(210);
        box.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(de.hts.tournament.SlipDesign d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label name = new Label(d.getLabel());
                name.setStyle("-fx-font-weight: bold;");
                Label desc = new Label(d.getDescription());
                desc.getStyleClass().add("hint");
                desc.setWrapText(true);
                desc.setMaxWidth(320);
                desc.setMinHeight(Region.USE_PREF_SIZE);
                setGraphic(new VBox(2, name, desc));
                setText(null);
            }
        });
        box.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(de.hts.tournament.SlipDesign d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.getLabel());
            }
        });
        box.valueProperty().addListener((o, a, b) -> {
            if (b != null && b != tournament.getSlipDesign()) {
                tournament.setSlipDesign(b);
                setStatus("Spielzettel: " + b.getLabel() + " (gilt für alle Spielzettel dieses Turniers).");
            }
        });
        box.setTooltip(new Tooltip("Aussehen der Spielzettel (A6) dieses Turniers. Gilt für alle Spielzettel dieses Turniers; "
                + "der Mannschaftszettel (A4) bleibt unverändert."));
        slipDesignBox = new HBox(8, lbl, box);
        slipDesignBox.setAlignment(Pos.CENTER_LEFT);
        return slipDesignBox;
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
        if (page == Page.PARTICIPANTS && participantsStale) {        // 1.10.0 (#172)
            participantsStale = false;
            participants.refresh();
        }
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
            topActions.getChildren().addAll(undoResultButton(), slipDesignControl());
            pageContent.getChildren().setAll(tablesView.getRoot());
            tablesView.refresh();
        } else if (page == Page.PRESENTATION) {
            header.select("presentation");
            pageTitle.setText("Präsentationsansicht");
            pageContent.getChildren().setAll(presentationView.getRoot());
            presentationView.refresh();
        } else if (page == Page.CERTIFICATES) {
            header.select("certs");
            pageTitle.setText("Urkunden drucken");
            pageContent.getChildren().setAll(certificatesView.getRoot());
            certificatesView.refresh();
        } else if (page == Page.PUBLISH) {
            header.select("publish");
            pageTitle.setText("Live im Internet");
            pageContent.getChildren().setAll(publishPage());
        } else if (page == Page.MATCHES) {
            header.select("matches");
            pageTitle.setText("Spiele");
            topActions.getChildren().add(undoResultButton());
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

    /** 1.4.0 (#113): Menüpunkt „Urkunden drucken“ (bleibt danach dauerhaft). */
    private void showCertMenu() {
        if (certMenu != null) return;
        certMenu = header.addMenuItem("certs", "Urkunden drucken", null, () -> showPage(Page.CERTIFICATES));
    }

    /**
     * 1.4.1: Sobald eine Phase abgeschlossen ist (Folgephase angelegt oder „Klasse beenden“) und dadurch Plätze
     * feststehen, fragt HTS einmal je abgeschlossener Phase, ob der Urkundendruck gestartet werden soll. „Zur
     * Druckansicht“ öffnet den Reiter mit den Plätzen 1 bis 3 dieser Klasse (soweit fest); „Abbrechen“
     * lässt den Reiter erscheinen, ohne Urkunde auszuwählen (man bleibt auf der aktuellen Seite).
     */
    private void offerCertificates() {
        if (tournament.isStarted()) showCertMenu();
        List<PlayingClass> fresh = new java.util.ArrayList<>();
        for (PlayingClass c : tournament.rootClasses()) {
            String k = tournament.certAskKey(c);       // billig: erst bei neu abgeschlossener Phase rechnen
            if (k == null || tournament.isCertAsked(k)) continue;
            tournament.markCertAsked(k);
            if (c.isFinished() || !tournament.fixedEntrants(c).isEmpty()) fresh.add(c);
        }
        if (fresh.isEmpty()) return;
        tournament.setCertTabShown(true);
        javafx.application.Platform.runLater(() -> {
            for (PlayingClass c : fresh) {
                if (CertificatesDialog.ask(stage, c.getName(), c.isFinished())) {
                    showPage(Page.CERTIFICATES);
                    certificatesView.selectTop3(c.getName());
                    return;
                }
                if (currentPage == Page.CERTIFICATES) certificatesView.selectNone(c.getName());
            }
        });
    }

    /** Nach Starts und Ergebnissen: Menü, Startanzeige und offene Seiten aktualisieren. */
    /** 1.10.0 (#172): Teilnehmerliste ist veraltet und wird beim nächsten Anzeigen neu aufgebaut. */
    private boolean participantsStale;

    private void onRunningChanged() {
        offerCertificates();
        refreshUndo();
        if (currentPage == Page.PARTICIPANTS) participants.refresh();   // 1.10.0 (#172): unsichtbare Liste erst beim Anzeigen
        else participantsStale = true;
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

    /** 1.7.0 (#140): nach einem unerwarteten Fehler den Stand sichern (ohne Rückfragen). */
    public void saveAfterError() {
        try {
            if (tournament.isModified()) store.save(tournament);
        } catch (IOException | RuntimeException e) {
            de.hts.Log.error("Speichern nach Fehler fehlgeschlagen", e);
        }
    }

    /** @return {@code true}, wenn gespeichert wurde */
    private boolean save() {
        try {
            store.save(tournament);
            updateSaveState();
            return true;
        } catch (IOException e) {
            de.hts.Log.error("Speichern fehlgeschlagen", e);
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
        offerLivePoster();
    }

    /** 1.7.0 (#148): beim Turnierstart mit „Live im Internet“ den QR-Code-Aushang anbieten. */
    private void offerLivePoster() {
        if (!tournament.isPublish() || !de.hts.web.WebSettings.configured()) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Für die Halle: ein A4-Aushang mit QR-Code zur Live-Seite dieses Turniers.",
                ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setTitle("QR-Code-Aushang");
        a.setHeaderText("QR-Code-Aushang drucken?");
        DialogSizing.fit(a);
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) showLivePoster();
    }

    /** 1.7.0 (#148): Aushang mit QR-Code zur Live-Seite als PDF (Druck über die PDF-Ansicht). */
    private void showLivePoster() {
        if (!de.hts.web.WebSettings.configured()) {
            error("QR-Code-Aushang", "Der Webserver ist noch nicht eingerichtet (Turnierübersicht → Webserver).");
            return;
        }
        String url = de.hts.web.Publisher.publicUrl(de.hts.web.WebSettings.server(), tournament.getName(), tournament.getDate());
        String name = "Live-Aushang " + (tournament.getName() == null ? "Turnier" : tournament.getName());
        PdfViewer.show(stage, tournament, name, de.hts.report.PdfReports.liveQrPoster(tournament, url,
                de.hts.persistence.Organizer.get().logoPng()), name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf", this::setStatus);
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

    // --- 0.35: Anwesenheit am Turniertag -----------------------------------------


    /**
     * 0.36: zwei Menüs – „Aktion bezüglich Markierte“ (angemeldet, endgültig abmelden, Abmeldung zurücknehmen) und
     * (1.9.0: „Handyabmeldungen“ entfallen mit dem Handy-Zugang im Hallen-WLAN.)
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
        MenuItem undoOnline = new MenuItem("Online-Meldung zurücknehmen");                 // 1.9.0 (#164)
        undoOnline.setOnAction(e -> {
            int n = 0;
            for (Player p : new ArrayList<>(participants.getTable().getSelectionModel().getSelectedItems())) if (tournament.undoOnline(p)) n++;
            setStatus(n == 0 ? "Unter den Markierten ist keine Online-Meldung." : n + " Online-Meldung(en) zurückgenommen.");
            refreshAfterAttendance();
        });
        marked.getItems().addAll(present, withdraw, new SeparatorMenuItem(), undo, undoOnline);
        marked.setTooltip(new Tooltip("Für die markierten Teilnehmenden – gelb = endgültig abgemeldet"));

        // 0.38 (#54): PDF der endgültig Abgemeldeten – nur aktiv, wenn mindestens eine Person abgemeldet ist
        Button pdf = new Button("Abgemeldete als PDF");
        pdf.getStyleClass().add("pill");
        pdf.setMinWidth(Region.USE_PREF_SIZE);
        pdf.setTooltip(new Tooltip("Liste der endgültig Abgemeldeten je Klasse (Name, Vorname, Verein, TTR, abgemeldet durch)"));
        pdf.setOnAction(e -> Reports.showWithdrawn(getRoot().getScene() == null ? null : getRoot().getScene().getWindow(), tournament, this::setStatus));
        Runnable updatePdf = () -> {
            pdf.setDisable(tournament.getPlayers().stream()
                    .noneMatch(p -> p.getAttendance() == de.hts.player.Attendance.WITHDRAWN && !p.isReserveOnly()));
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
        MenuItem club = new MenuItem("Verein abrechnen");
        club.setOnAction(e -> settleClub());
        MenuItem cash = new MenuItem("Kassenübersicht (PDF)");
        cash.setOnAction(e -> Reports.showCashReport(getRoot().getScene() == null ? null : getRoot().getScene().getWindow(), tournament, this::setStatus));
        fees.getItems().addAll(club, cash);
        fees.setTooltip(new Tooltip("Startgebühren je Verein abrechnen (mit Quittung) oder die Kassenübersicht drucken"));
        fees.setVisible(tournament.hasFees());
        participants.countTextProperty().addListener((o, a, b) -> fees.setVisible(tournament.hasFees()));
        // 0.39 (#64): in einer Zeile, PDF rechts neben den Menüs (Umbruch nur, wenn es wirklich eng wird)
        javafx.scene.layout.FlowPane box = new javafx.scene.layout.FlowPane(8, 8, marked, fees, pdf);
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

    private void refreshAfterAttendance() {
        participants.refresh();
        classes.refresh();
        updateStartBox();
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
