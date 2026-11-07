package de.hts.ui;

import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Seite „Spiele“: alle Spiele des Turniers klassenübergreifend – standardmäßig die laufenden und eingereihten. */
public final class MatchesView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private enum Filter {
        OPEN("aktiv, pausiert & eingereiht"), ACTIVE("aktiv"), PAUSED("pausiert"), QUEUED("eingereiht"), DONE("abgeschlossen"),
        PLANNED("geplant"), ALL("alle");
        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private final Tournament tournament;
    private final Runnable onChanged;
    private final java.util.function.Consumer<String> status;

    private final VBox root = new VBox(12);
    private final TableView<Match> table = new TableView<>();
    private final ComboBox<Filter> filter = new ComboBox<>();
    private final ComboBox<String> classFilter = new ComboBox<>();
    private final Label counts = new Label();
    private final javafx.scene.control.TextField search = new javafx.scene.control.TextField();
    private boolean updating;

    public MatchesView(Tournament tournament, java.util.function.Consumer<String> status, Runnable onChanged) {
        this.tournament = tournament;
        this.status = status;
        this.onChanged = onChanged;

        filter.getItems().setAll(Filter.values());
        filter.setValue(Filter.OPEN);
        filter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Filter f) {
                return f == null ? "" : f.label;
            }

            @Override
            public Filter fromString(String s) {
                return null;
            }
        });
        filter.valueProperty().addListener((o, a, b) -> refresh());
        classFilter.valueProperty().addListener((o, a, b) -> {
            if (!updating) refresh();
        });
        Button result = new Button("Ergebnis eintragen / korrigieren", Icons.of(Icons.EDIT, 14));
        result.getStyleClass().add("navy-pill");
        result.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> !ResultEntry.isEditable(table.getSelectionModel().getSelectedItem()),
                table.getSelectionModel().selectedItemProperty()));
        result.setOnAction(e -> enterResult(table.getSelectionModel().getSelectedItem()));
        search.setPromptText("Suchen: Vorname, Nachname, Verein");
        search.setPrefColumnCount(16);
        search.textProperty().addListener((o, a, b) -> refresh());
        counts.getStyleClass().add("hint");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label sl = new Label("Status:");
        sl.getStyleClass().add("class-name");
        Label cl = new Label("Klasse:");
        cl.getStyleClass().add("class-name");
        HBox bar = new HBox(10, search, sl, filter, cl, classFilter, counts, sp, result);
        bar.setAlignment(Pos.CENTER_LEFT);

        setupTable();
        VBox card = new VBox(12, bar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        root.getChildren().add(card);
        root.setPadding(new Insets(0, 40, 16, 40));
    }

    public Node getRoot() {
        return root;
    }

    public void refresh() {
        updating = true;
        String cur = classFilter.getValue();
        List<String> names = new java.util.ArrayList<>();
        names.add("Alle Klassen");
        tournament.getClasses().stream().map(PlayingClass::getName).forEach(names::add);
        classFilter.getItems().setAll(names);
        classFilter.setValue(cur != null && names.contains(cur) ? cur : "Alle Klassen");
        updating = false;

        Filter f = filter.getValue();
        String cls = classFilter.getValue();
        String q = search.getText() == null ? "" : search.getText().trim();
        List<Match> list = tournament.getMatches().stream()
                .filter(m -> cls == null || cls.equals("Alle Klassen") || m.getClassName().equals(cls))
                .filter(m -> q.isEmpty() || matchesName(m, q))
                .filter(m -> switch (f) {
                    case OPEN -> m.getStatus().atTable() || m.getStatus() == MatchStatus.QUEUED;
                    case ACTIVE -> m.getStatus() == MatchStatus.ACTIVE;
                    case PAUSED -> m.getStatus() == MatchStatus.PAUSED;
                    case QUEUED -> m.getStatus() == MatchStatus.QUEUED;
                    case DONE -> m.getStatus() == MatchStatus.DONE;
                    case PLANNED -> m.getStatus() == MatchStatus.PLANNED;
                    case ALL -> true;
                })
                .sorted(Comparator.comparingInt((Match m) -> m.getStatus().ordinal() == 2 ? 0 : m.getStatus().ordinal() == 1 ? 1 : 2)
                        .thenComparing(m -> m.getTable() == null ? Integer.MAX_VALUE : m.getTable())
                        .thenComparingLong(Match::getQueueOrder)
                        .thenComparing(Match::getClassName)
                        .thenComparingInt(Match::getNumber))
                .toList();
        table.getItems().setAll(list);
        Ui.fitColumns(table);                      // 0.36: Spalten so breit wie ihr Inhalt, notfalls Scrollleiste
        long active = tournament.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.ACTIVE).count();
        long queued = tournament.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.QUEUED).count();
        long done = tournament.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.DONE).count();
        counts.setText(active + " aktiv · " + queued + " eingereiht · " + done + " abgeschlossen");
    }

    private void setupTable() {
        TableColumn<Match, MatchStatus> st = new TableColumn<>("Status");
        st.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatus()));
        st.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(MatchStatus s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty || s == null ? null : DrawView.statusChip(s));
                setText(null);
            }
        });
        st.setPrefWidth(120);
        st.getProperties().put("fit-min", 120.0);
        table.getColumns().add(center(st));
        // Titel und Werte gleich ausgerichtet: Text links, kurze Werte zentriert
        table.getColumns().add(center(col("Tisch", m -> m.getTable() == null ? "" : tournament.getTableNames().containsKey(m.getTable())
                ? tournament.tableName(m.getTable()) : String.valueOf(m.getTable()), 80)));
        table.getColumns().add(left(col("Klasse", Match::getClassName, 150)));
        table.getColumns().add(left(col("Phase", Match::getStage, 110)));
        table.getColumns().add(center(col("Runde", Match::getRound, 60)));
        table.getColumns().add(center(col("Spiel", Match::getNumber, 55)));
        table.getColumns().add(left(col("Teilnehmende A", m -> name(m, m.getA()), 210)));
        table.getColumns().add(left(col("Teilnehmende B", m -> name(m, m.getB()), 210)));
        table.getColumns().add(center(col("Ergebnis", Match::getResultText, 75)));
        table.getColumns().add(left(col("Sätze", Match::getSetsText, 220)));
        table.getColumns().add(center(col("Beginn", m -> m.getStartedAt() == null ? "" : m.getStartedAt().format(TIME), 70)));
        table.getColumns().add(center(exportColumn()));
        table.getColumns().forEach(c -> c.setSortable(false));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Keine Spiele für diese Auswahl."));
        table.setRowFactory(tv -> {
            TableRow<Match> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()
                        && ResultEntry.isEditable(row.getItem())) {
                    enterResult(row.getItem());
                }
            });
            return row;
        });
    }

    /**
     * „Export“: nur für abgeschlossene Spiele – Haken weg = „nicht relevant für Export“ (nicht in der click-TT-Datei).
     * Nach einer Aufgabe kampflos gewertete und übernommene Spiele werden nie exportiert.
     */
    private TableColumn<Match, Match> exportColumn() {
        TableColumn<Match, Match> c = new TableColumn<>("Export");
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            private final javafx.scene.control.CheckBox box = new javafx.scene.control.CheckBox();

            {
                box.setOnAction(e -> {
                    Match m = getItem();
                    if (m == null) return;
                    m.setExportExcluded(!box.isSelected());
                    tournament.markModified();
                    status.accept("Spiel " + m.getNumber() + (box.isSelected() ? " wird exportiert." : " ist nicht relevant für den Export."));
                });
                box.setTooltip(new javafx.scene.control.Tooltip("Haken entfernen = nicht relevant für Export (nicht in der click-TT-Ergebnisdatei)"));
            }

            @Override
            protected void updateItem(Match m, boolean empty) {
                super.updateItem(m, empty);
                setText(null);
                setGraphic(null);
                if (empty || m == null || m.getStatus() != MatchStatus.DONE) return;
                if (m.getRetirementOf() != null) {
                    setText("nein (kampflos)");
                } else if (m.isCarried()) {
                    setText("übernommen");
                } else {
                    box.setSelected(!m.isExportExcluded());
                    setGraphic(box);
                }
            }
        });
        c.setPrefWidth(80);
        c.getProperties().put("fit-min", 110.0);
        return c;
    }

    private String name(Match m, String id) {
        String n = tournament.entrantName(m, id);
        return n == null ? "offen" : n + tournament.handicapSuffix(m, id);
    }

    private static <T> TableColumn<Match, T> left(TableColumn<Match, T> c) {
        c.getStyleClass().add("col-left");
        c.setStyle("-fx-alignment: CENTER-LEFT;");
        return c;
    }

    private static <T> TableColumn<Match, T> center(TableColumn<Match, T> c) {
        c.getStyleClass().add("col-center");
        c.setStyle("-fx-alignment: CENTER;");
        return c;
    }

    private static <T> TableColumn<Match, T> col(String title, Function<Match, T> getter, double width) {
        TableColumn<Match, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private void enterResult(Match m) {
        Window w = root.getScene() == null ? null : root.getScene().getWindow();
        if (ResultEntry.open(w, tournament, m, status)) {
            refresh();
            onChanged.run();
        }
    }

    /** Jedes Suchwort muss im Namen einer der beiden Parteien vorkommen (Umlaute/Groß-Klein egal). */
    private boolean matchesName(Match m, String q) {
        return tournament.matchMatches(m, q);          // Vorname, Nachname, Verein beider Parteien
    }
}
