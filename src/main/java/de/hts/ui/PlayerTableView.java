package de.hts.ui;

import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.player.PlayerFilter;
import de.hts.player.PlayerFilter.RegistrationFilter;
import de.hts.player.Sex;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tabelle der Spieler mit Suchleiste.
 * Suche nach Vorname, Nachname und Geburtsdatum; sortierbar nach Vorname, Nachname und TTR.
 */
public final class PlayerTableView {

    private final ObservableList<Player> source;
    private final Consumer<Player> onPlayerChanged;
    private final TableView<Player> table = new TableView<>();
    private final FilteredList<Player> filtered;

    private final TextField firstnameSearch = searchField("Vorname");
    private final TextField lastnameSearch = searchField("Nachname");
    private final TextField birthSearch = searchField("z. B. 1994");
    private final ComboBox<Sex> sexFilter = new ComboBox<>();
    private final ComboBox<RegistrationFilter> registrationFilter = new ComboBox<>();

    private final BorderPane root = new BorderPane();
    private final StringBinding countText;

    /**
     * @param players            die Spielerliste
     * @param onPlayerChanged    wird aufgerufen, wenn ein Spieler in der Tabelle geändert wurde
     */
    public PlayerTableView(ObservableList<Player> players, Consumer<Player> onPlayerChanged) {
        this.source = players;
        this.onPlayerChanged = onPlayerChanged;
        this.filtered = new FilteredList<>(players, p -> true);
        SortedList<Player> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Noch keine Teilnehmenden. Über „XML der Teilnehmenden importieren“ eine click-TT-Datei laden\noder mit „Hinzufügen“ von Hand erfassen."));
        table.setRowFactory(tv -> {
            TableRow<Player> row = new TableRow<>() {
                @Override
                protected void updateItem(Player p, boolean empty) {
                    super.updateItem(p, empty);
                    getStyleClass().removeAll("attendance-withdrawn", "attendance-requested");
                    if (!empty && p != null && p.getAttendance() == Attendance.WITHDRAWN) getStyleClass().add("attendance-withdrawn");
                    if (!empty && p != null && p.getAttendance() == Attendance.WITHDRAW_REQUESTED) getStyleClass().add("attendance-requested");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2
                        && !row.isEmpty() && onEdit != null) {
                    onEdit.accept(row.getItem());
                }
            });
            return row;
        });

        Collator collator = Collator.getInstance(Locale.GERMAN);
        Comparator<String> textCmp = Comparator.nullsLast(collator::compare);

        // 0.35: Angemeldet = am Turniertag anwesend (Haken per Klick; per Handy über die Anmeldeseite)
        TableColumn<Player, Attendance> registered = new TableColumn<>("Angemeldet");
        registered.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getAttendance()));
        registered.getStyleClass().add("col-center");
        registered.setCellFactory(col -> new TableCell<>() {
            private final CheckBox box = new CheckBox();
            {
                box.setOnAction(e -> {
                    Player p = getTableRow() == null ? null : getTableRow().getItem();
                    if (p == null) return;
                    p.setAttendance(box.isSelected() ? Attendance.PRESENT : Attendance.UNKNOWN);
                    onPlayerChanged.accept(p);
                    refresh();
                });
                box.setFocusTraversable(false);
            }

            @Override
            protected void updateItem(Attendance a, boolean empty) {
                super.updateItem(a, empty);
                setAlignment(Pos.CENTER);
                if (empty || a == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    return;
                }
                if (a == Attendance.WITHDRAWN || a == Attendance.WITHDRAW_REQUESTED) {
                    setGraphic(null);
                    setText(a == Attendance.WITHDRAWN ? "abgemeldet" : "Abmeldung ?");
                    setTooltip(new Tooltip(a == Attendance.WITHDRAWN ? "Endgültig abgemeldet – über „Anwesenheit“ zurücknehmen"
                            : "Per Handy als „nicht anwesend“ gemeldet – über „Anwesenheit → Abmeldung bestätigen“ bestätigen"));
                } else {
                    box.setSelected(a == Attendance.PRESENT);
                    setGraphic(box);
                    setText(null);
                    setTooltip(new Tooltip("Haken = am Turniertag anwesend (angemeldet)"));
                }
            }
        });
        registered.setSortable(false);
        registered.setStyle("-fx-alignment: CENTER;");
        registered.setPrefWidth(118);
        registered.setMinWidth(112);
        registered.setMaxWidth(140);
        TableColumn<Player, String> reserve = new TableColumn<>("Nachrücker");
        reserve.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isReserve() ? "✓" : ""));
        reserve.setStyle("-fx-alignment: CENTER;");
        reserve.getStyleClass().add("col-center");
        reserve.setSortable(false);
        reserve.setPrefWidth(108);
        reserve.setMinWidth(102);
        reserve.setMaxWidth(130);

        TableColumn<Player, String> lastname = textColumn("Nachname", Player::getLastname, 140, textCmp);
        TableColumn<Player, String> firstname = textColumn("Vorname", Player::getFirstname, 120, textCmp);
        TableColumn<Player, String> birth = textColumn("Geburtsdatum", Player::getBirthText, 100, textCmp);
        birth.setSortable(false);
        TableColumn<Player, String> sex = textColumn("G", p -> p.getSex().getShortLabel(), 40, textCmp);
        sex.setSortable(false);
        sex.setStyle("-fx-alignment: CENTER;");
        sex.getStyleClass().add("col-center");
        TableColumn<Player, String> club = textColumn("Verein", Player::getClubName, 190, textCmp);
        club.setSortable(false);
        TableColumn<Player, Integer> ttr = new TableColumn<>("TTR");
        ttr.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTtr()));
        ttr.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        ttr.setPrefWidth(60);
        ttr.setStyle("-fx-alignment: CENTER-RIGHT;");
        ttr.getStyleClass().add("col-right");
        TableColumn<Player, String> entries = textColumn("Klassen", Player::getClassesText, 260, textCmp);
        entries.setSortable(false);
        TableColumn<Player, String> licence = textColumn("Lizenz-Nr.", Player::getLicenceNr, 90, textCmp);
        licence.setSortable(false);

        table.getColumns().addAll(List.of(registered, reserve, lastname, firstname, birth, sex, club, ttr, entries, licence));
        table.getSortOrder().addAll(List.of(lastname, firstname));

        // --- Suchleiste ---
        sexFilter.getItems().addAll(Sex.UNKNOWN, Sex.MALE, Sex.FEMALE);
        sexFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Sex s) {
                if (s == null || s == Sex.UNKNOWN) return "Alle";
                return s == Sex.MALE ? "männlich" : "weiblich";
            }

            @Override
            public Sex fromString(String s) {
                return null;
            }
        });
        sexFilter.setValue(Sex.UNKNOWN);
        registrationFilter.getItems().addAll(RegistrationFilter.values());
        registrationFilter.setValue(RegistrationFilter.ALL);

        firstnameSearch.textProperty().addListener((o, a, b) -> updateFilter());
        lastnameSearch.textProperty().addListener((o, a, b) -> updateFilter());
        birthSearch.textProperty().addListener((o, a, b) -> updateFilter());
        sexFilter.valueProperty().addListener((o, a, b) -> updateFilter());
        registrationFilter.valueProperty().addListener((o, a, b) -> updateFilter());

        Button reset = new Button("Zurücksetzen");
        reset.setOnAction(e -> resetFilter());

        birthSearch.setTooltip(new Tooltip("Jahrgang (1994) oder Datum (14.03.1994)"));

        FlowPane searchBar = new FlowPane(14, 6,
                labeled("Vorname:", firstnameSearch),
                labeled("Nachname:", lastnameSearch),
                labeled("Geburtsdatum:", birthSearch),
                labeled("Geschlecht:", sexFilter),
                labeled("Status:", registrationFilter),
                reset);
        searchBar.setPadding(new Insets(6, 10, 6, 10));
        searchBar.getStyleClass().add("search-bar");

        countText = Bindings.createStringBinding(() -> {
            long present = source.stream().filter(Player::isPresent).count();
            long withdrawn = source.stream().filter(p -> p.getAttendance() == Attendance.WITHDRAWN).count();
            long requested = source.stream().filter(p -> p.getAttendance() == Attendance.WITHDRAW_REQUESTED).count();
            String base = filtered.size() == source.size()
                    ? source.size() + " Teilnehmende"
                    : filtered.size() + " von " + source.size() + " Teilnehmenden";
            return base + " · " + present + " angemeldet" + (withdrawn > 0 ? " · " + withdrawn + " abgemeldet" : "")
                    + (requested > 0 ? " · " + requested + " Abmeldung(en) zu bestätigen" : "");
        }, filtered, source, refreshTick);

        root.setTop(searchBar);
        root.setCenter(table);
    }

    // --- Filter ------------------------------------------------------------

    private void updateFilter() {
        filtered.setPredicate(currentFilter());
    }

    public PlayerFilter currentFilter() {
        return new PlayerFilter(firstnameSearch.getText(), lastnameSearch.getText(), birthSearch.getText(),
                sexFilter.getValue(), registrationFilter.getValue());
    }

    public void resetFilter() {
        firstnameSearch.clear();
        lastnameSearch.clear();
        birthSearch.clear();
        sexFilter.setValue(Sex.UNKNOWN);
        registrationFilter.setValue(RegistrationFilter.ALL);
    }

    public void focusSearch() {
        lastnameSearch.requestFocus();
    }

    /** Nach Änderungen an Spielern Anzeige und Filter neu auswerten. */
    public void refresh() {
        table.refresh();
        updateFilter();
        refreshTick.set(refreshTick.get() + 1);
    }

    /** Zählt bei jeder Aktualisierung hoch (für die Zähler in der Statuszeile). */
    private final javafx.beans.property.IntegerProperty refreshTick = new javafx.beans.property.SimpleIntegerProperty();

    // --- Hilfsmethoden -----------------------------------------------------

    private static TextField searchField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefColumnCount(10);
        return tf;
    }

    private static HBox labeled(String label, javafx.scene.Node control) {
        HBox box = new HBox(5, new Label(label), control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static TableColumn<Player, String> textColumn(String title, Function<Player, String> getter,
                                                          double width, Comparator<String> cmp) {
        TableColumn<Player, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getter.apply(c.getValue())));
        col.setComparator(cmp);
        col.setPrefWidth(width);
        return col;
    }

    // --- Zugriff -----------------------------------------------------------

    private Consumer<Player> onEdit;

    /** Wird bei Doppelklick auf eine Zeile aufgerufen (Teilnehmende bearbeiten). */
    public void setOnEdit(Consumer<Player> onEdit) {
        this.onEdit = onEdit;
    }

    public Player getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }

    public TableView<Player> getTable() {
        return table;
    }

    public BorderPane getRoot() {
        return root;
    }

    public ObservableValue<String> countTextProperty() {
        return countText;
    }
}
