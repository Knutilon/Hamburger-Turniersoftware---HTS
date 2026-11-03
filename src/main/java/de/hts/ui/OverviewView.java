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

    public OverviewView(Stage stage, TournamentStore store, Consumer<Tournament> onOpen) {
        this.stage = stage;
        this.store = store;
        this.onOpen = onOpen;

        // Kopfleiste
        AppHeader appHeader = new AppHeader();
        appHeader.addMenuItem("tournaments", "Turniere", null, this::refresh);
        appHeader.select("tournaments");
        appHeader.addTool(Icons.PLUS, "Neues Turnier", this::createTournament);
        InfoDialog.install(appHeader, () -> stage);

        // Seitenkopf
        Label title = new Label("Meine Turniere");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(HtsApp.NAME + " · Version " + HtsApp.VERSION);
        subtitle.getStyleClass().add("page-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button create = new Button("Neues Turnier", Icons.of(Icons.PLUS, 16));
        create.getStyleClass().add("accent");
        create.setOnAction(e -> createTournament());
        Button importFile = new Button("Turnierdatei übernehmen", Icons.of(Icons.FOLDER, 16));
        importFile.setOnAction(e -> importTournamentFile());
        importFile.getStyleClass().add("pill");
        create.getStyleClass().remove("accent");
        create.getStyleClass().add("navy-pill");
        Button importResults = new Button("Fertiges Turnier importieren (click-TT)", Icons.of(Icons.UPLOAD, 16));
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
        Label empty = new Label("Noch keine Turniere.\nMit „Neues Turnier“ das erste anlegen.");
        empty.getStyleClass().add("hint");
        list.setPlaceholder(empty);
        list.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
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
        refresh();
    }

    public Node getRoot() {
        return root;
    }

    public void refresh() {
        try {
            list.getItems().setAll(store.list());
            var problems = store.getProblems();
            message.setText(problems.isEmpty() ? "" : "Nicht lesbar: " + problems.stream()
                    .map(p -> p.file().getFileName() + " (" + p.message() + ")").collect(Collectors.joining("; ")));
        } catch (IOException e) {
            message.setText("Turnierordner konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    // --- Aktionen ----------------------------------------------------------

    private void createTournament() {
        new TournamentDialog(stage, null, null, null, true, null, false, new java.util.ArrayList<>(list.getItems()))
                .showAndWait().ifPresent(r -> {
            try {
                Tournament t = r.copyFrom() != null
                        ? store.copy(r.copyFrom(), r.copyMode(), r.name(), r.date(), r.tables(), r.test(), r.fileDir())
                        : store.create(r.name(), r.date(), r.tables(), r.fileDir(), r.test());
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
        new TournamentDialog(stage, s.name(), s.date(), s.tables(), false, s.file(), s.test(), List.of()).showAndWait().ifPresent(r -> {
            try {
                Tournament t = store.load(s);
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
