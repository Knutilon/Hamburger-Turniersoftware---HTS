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

    private enum Page { PARTICIPANTS, CLASSES, TABLES, MATCHES }

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
    private Button matchesMenu;
    private Page currentPage = Page.PARTICIPANTS;

    private final Label pageTitle = new Label();
    private final HBox pageActions = new HBox(8);
    private final StackPane pageContent = new StackPane();
    private final StackPane participantsCard = new StackPane();
    private final Label statusLabel = new Label("Bereit.");
    private final Label saveState = new Label();
    private final HBox startBox = new HBox(6);
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
        header.addMenuItem("classes", "Klassen", null, () -> showPage(Page.CLASSES));
        header.addMenuItem("tables", "Tische", null, () -> showPage(Page.TABLES));
        matchesMenu = header.addMenuItem("matches", "Spiele", null, () -> showPage(Page.MATCHES));
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
        return header.getRoot();
    }

    private void goBack() {
        if (prepareLeave()) onBack.run();
    }

    private void updateStartBox() {
        startBox.getChildren().clear();
        if (tournament.isStarted()) {
            Circle dot = new Circle(6);
            dot.getStyleClass().add("live-dot");
            Label l = new Label("Gestartet " + tournament.getStartedAt().format(START_FORMAT), dot);
            l.getStyleClass().add("started-label");
            l.setGraphicTextGap(8);
            startBox.getChildren().add(l);
            if (tournament.isCompletelyFinished()) {
                javafx.scene.control.MenuButton export = new javafx.scene.control.MenuButton("Ergebnisse exportieren", Icons.of(Icons.START, 14));
                export.getStyleClass().add("navy-pill");
                javafx.scene.control.MenuItem pdf = new javafx.scene.control.MenuItem("Gesamtergebnis als PDF");
                pdf.setOnAction(e -> Reports.showTournamentResults(stage, tournament, this::setStatus));
                javafx.scene.control.MenuItem ctt = new javafx.scene.control.MenuItem("click-TT-Ergebnisdatei (XML)");
                ctt.setOnAction(e -> exportClickTt());
                export.getItems().addAll(pdf, ctt);
                startBox.getChildren().add(export);
            }
            if (!tournament.previewStart().started().isEmpty()) {
                Button more = new Button("Bereite Klassen starten", Icons.of(Icons.START, 14));
                more.getStyleClass().add("navy-pill");
                more.setOnAction(e -> startTournament());
                startBox.getChildren().add(more);
            }
        } else {
            Button start = new Button("Turnier starten", Icons.of(Icons.START, 16));
            start.getStyleClass().add("navy-pill");
            start.setOnAction(e -> startTournament());
            startBox.getChildren().add(start);
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
        VBox titles = new VBox(2, pageTitle, tournamentLine);

        startBox.setAlignment(Pos.CENTER_RIGHT);
        tournament.startedAtProperty().addListener((o, a, b) -> updateStartBox());
        updateStartBox();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(12, titles, spacer, startBox);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("page-header");

        pageActions.setAlignment(Pos.CENTER_LEFT);
        pageActions.getStyleClass().add("page-actions");

        Region sbSpacer = new Region();
        HBox.setHgrow(sbSpacer, Priority.ALWAYS);
        Label count = new Label();
        count.textProperty().bind(participants.countTextProperty());
        HBox statusBar = new HBox(8, statusLabel, sbSpacer, count);
        statusBar.setPadding(new Insets(6, 24, 6, 24));
        statusBar.getStyleClass().add("status-bar");

        VBox.setVgrow(pageContent, Priority.ALWAYS);
        VBox content = new VBox(titleRow, pageActions, pageContent, statusBar);
        content.getStyleClass().add("content");
        return content;
    }

    private void showPage(Page page) {
        currentPage = page;
        pageActions.getChildren().clear();
        if (page == Page.PARTICIPANTS) {
            header.select("participants");
            pageTitle.setText("Teilnehmende");
            Button importBtn = new Button("XML der Teilnehmenden importieren", Icons.of(Icons.UPLOAD, 16));
            importBtn.getStyleClass().add("navy-pill");
            importBtn.setOnAction(e -> importClickTt());
            Button add = new Button("Hinzufügen", Icons.of(Icons.PLUS, 16));
            add.setOnAction(e -> addParticipant());
            Button edit = new Button("Bearbeiten", Icons.of(Icons.EDIT, 16));
            edit.setOnAction(e -> editParticipant(participants.getSelected()));
            edit.disableProperty().bind(participants.getTable().getSelectionModel().selectedItemProperty().isNull());
            Button delete = new Button("Löschen", Icons.of(Icons.TRASH, 16));
            delete.setOnAction(e -> deleteParticipants());
            delete.disableProperty().bind(participants.getTable().getSelectionModel().selectedItemProperty().isNull());
            for (Button b : List.of(add, edit, delete)) b.getStyleClass().add("pill");
            pageActions.getChildren().addAll(importBtn, add, edit, delete);
            pageContent.getChildren().setAll(participantsCard);
            participants.refresh();
        } else if (page == Page.TABLES) {
            header.select("tables");
            pageTitle.setText("Tische");
            pageContent.getChildren().setAll(tablesView.getRoot());
            tablesView.refresh();
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
            pageActions.getChildren().addAll(add);
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
        if (r.started().isEmpty()) {
            sb.append(tournament.isStarted() ? "Es ist keine weitere Klasse bereit." : "Es ist noch keine Klasse bereit – es wird nur die Startzeit festgehalten.");
        } else {
            sb.append("Freie Tische (").append(tournament.getTableCount()).append(") werden automatisch belegt.");
        }
        boolean allReady = r.notReady().isEmpty() && r.alreadyRunning().isEmpty();
        Alert a = new Alert(allReady ? Alert.AlertType.CONFIRMATION : Alert.AlertType.WARNING, sb.toString(), ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setTitle("Turnier starten");
        a.setHeaderText(allReady ? "Alle Klassen sind bereit – Turnier jetzt starten?"
                : "Nicht alle Klassen sind bereit. Trotzdem starten?");
        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        tournament.startTournament();
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
        return new ContextMenu(edit, new SeparatorMenuItem(), delete);
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
        int newClasses = 0;
        java.util.Set<String> freshSingles = new java.util.HashSet<>();
        for (ClickTtXmlImporter.CompetitionInfo ci : result.competitions()) {
            Discipline d = ci.doubles() ? Discipline.DOUBLES : Discipline.SINGLES;
            var existing = tournament.findClass(ci.name());
            if (existing.isEmpty()) {
                tournament.addClass(new PlayingClass(ci.name(), d, ClassType.guess(ci.name(), ci.sex()), null, null));
                if (!ci.doubles()) freshSingles.add(ci.name());
                newClasses++;
            } else if (ci.doubles() && !existing.get().isDoubles()) {
                tournament.changeDiscipline(existing.get(), Discipline.DOUBLES);
            }
        }
        // neu angelegte Einzelklassen: Gemeldete stehen zunächst unter „Verfügbar“ (Meldung „gemeldet“), nicht in der Klasse
        Tournament.registerOnly(result.players(), freshSingles);
        Tournament.MergeResult merge = tournament.merge(result.players());
        int newPairs = importPairs(result.pairs());
        tournament.ensurePairsForEntries();
        participants.refresh();
        classes.refresh();
        setStatus("Import aus " + file.getName() + ": " + merge.added() + " neu, " + merge.updated() + " aktualisiert"
                + (newClasses > 0 ? ", " + newClasses + " Klassen angelegt" : "")
                + (newPairs > 0 ? ", " + newPairs + " Doppelpaarungen gebildet." : "."));
    }

    /**
     * Übernimmt die Doppelpaarungen aus der Datei. Wer in der Klasse schon fest gepaart ist
     * (z. B. von Hand), wird nicht umgepaart.
     */
    private int importPairs(List<ClickTtXmlImporter.ImportedPair> imported) {
        java.util.Map<String, Player> byKey = new java.util.HashMap<>();
        tournament.getPlayers().forEach(p -> byKey.put(p.identityKey(), p));
        int n = 0;
        for (ClickTtXmlImporter.ImportedPair ip : imported) {
            PlayingClass c = tournament.findClass(ip.competition()).orElse(null);
            Player a = byKey.get(ip.firstKey());
            Player b = byKey.get(ip.secondKey());
            if (ip.waitingList() || c == null || !c.isDoubles() || a == null || b == null || a == b) continue;
            var pa = tournament.pairOf(a, c);
            var pb = tournament.pairOf(b, c);
            if (pa.isPresent() && pa.equals(pb)) continue; // schon so gepaart
            boolean aFixed = pa.isPresent() && pa.get().isComplete();
            boolean bFixed = pb.isPresent() && pb.get().isComplete();
            if (aFixed || bFixed) continue;
            tournament.createPair(c, a, b);
            n++;
        }
        return n;
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
