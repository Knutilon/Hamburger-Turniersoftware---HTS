package de.hts.ui;

import de.hts.player.Player;
import de.hts.player.PlayerFilter;
import de.hts.tournament.DoublesPair;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Seite „Klassen“: links die Klassenliste, daneben alle Teilnehmenden des Turniers (filterbar nach Einzelklasse),
 * rechts die Zuordnung.
 *
 * <ul>
 *   <li><b>Einzel</b>: Teilnehmende per Drag &amp; Drop, Doppelklick oder Schaltfläche zuordnen bzw. entfernen.</li>
 *   <li><b>Doppel</b>: Paarungen aus je zwei Teilnehmenden. Teilnehmende auf eine Paarungskarte ziehen
 *       (freier Platz) oder auf „Neues Doppel“; aus einer Karte zurück in die Liste ziehen entfernt sie.</li>
 * </ul>
 */
public final class ClassesView {

    /** Kennung für Drag &amp; Drop innerhalb von HTS. */
    private static final String DND_PREFIX = "hts-participants:";

    private final Tournament tournament;
    private final Consumer<String> status;
    private final Runnable onChanged;
    private final DrawView drawView;
    private final TabPane classTabs = new TabPane();
    private final Tab participantsTab = new Tab("Teilnehmende der Klasse");
    private final List<Tab> phaseTabs = new ArrayList<>();
    private final Tab resultTab = new Tab("Endergebnis");
    private final Button modeBtn = new Button("Teilnehmende fertig – Modus festlegen", Icons.of(Icons.START, 14));
    private final java.util.Random random = new java.security.SecureRandom();
    private final FinalResultView resultView;
    private boolean syncingTabs;
    private final Label classState = new Label();
    private final ComboBox<String> clubFilter = new ComboBox<>();
    private final ComboBox<de.hts.player.Sex> sexFilter = new ComboBox<>();
    /** Filter für „In der Klasse“ – wie bei „Verfügbar“. */
    private final TextField inSearch = new TextField();
    private final ComboBox<String> inClubFilter = new ComboBox<>();
    private final ComboBox<de.hts.player.Sex> inSexFilter = new ComboBox<>();
    /** Hinweis, solange die Klasse ausgelost ist: Zuordnung gesperrt. */
    private final Label lockText = new Label();
    private final HBox lockBanner = new HBox(10);

    private final BorderPane root = new BorderPane();
    private final ListView<PlayingClass> classList = new ListView<>();

    private final FilteredList<Player> available;
    private final FilteredList<Player> assigned;
    private final TableView<Player> availableTable = new TableView<>();
    private final TableView<Player> assignedTable = new TableView<>();
    private TableColumn<Player, String> partnerColumn;

    private final TextField search = new TextField();
    private final ComboBox<PlayingClass> singlesFilter = new ComboBox<>();
    private final CheckBox onlyMatching = new CheckBox("nur passende");
    private final Label classTitle = new Label();
    private final Label classCriteria = new Label();
    private final Label availableTitle = new Label();
    private final Label assignedTitle = new Label();
    private final Label dndHint = new Label();
    /** Doppel: Einzelne ohne Partner/in einander zulosen */
    private final Button partnerBtn = new Button("Partner zulosen");
    private final Button newParticipantBtn = new Button("Teilnehmende neu erfassen", Icons.of(Icons.PLUS, 14));
    /** Filter nach Einzelklasse – nur bei Doppelklassen sichtbar. */
    private final HBox filterRow = new HBox(8);

    private final VBox middle = new VBox(10);
    private final StackPane rightContent = new StackPane();
    private final VBox singlesBox;
    private final VBox pairsList = new VBox(8);
    /** Suche über den Doppelpaarungen (Vorname, Nachname, Verein) */
    private final TextField pairSearch = new TextField();
    private final VBox doublesBox;

    /** Teilnehmende, die gerade gezogen werden (Drag &amp; Drop innerhalb dieses Fensters). */
    private List<Player> dragged = List.of();
    /** Paarung, aus der gerade gezogen wird ({@code null} = aus einer Liste). */
    private DoublesPair draggedFromPair;

    /**
     * @param status    zeigt eine Meldung in der Statuszeile an
     * @param onChanged wird nach jeder Änderung der Zuordnungen aufgerufen
     */
    public ClassesView(Tournament tournament, Consumer<String> status, Runnable onChanged,
                       Runnable onRunningChanged) {
        this.tournament = tournament;
        this.drawView = new DrawView(tournament, status, () -> {
            refresh();
            onRunningChanged.run();
        }, c -> {
            refresh();
            classTabs.getSelectionModel().select(participantsTab);
        }, this::openClassDraw);
        this.resultView = new FinalResultView(tournament, status);
        this.status = status;
        this.onChanged = onChanged;
        this.available = new FilteredList<>(tournament.getPlayers(), p -> false);
        this.assigned = new FilteredList<>(tournament.getPlayers(), p -> false);

        // --- Klassenliste ---
        classList.setItems(new FilteredList<>(tournament.getClasses(), pc -> !pc.isPhase()));
        classList.setCellFactory(lv -> new ClassCell());
        classList.setPlaceholder(new Label("Noch keine Klassen.\n„Klasse anlegen“ legt eine an."));
        classList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            showClass(b);
            if (b != null && b != a) openDefaultTab(b);
        });
        classList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) editClass();
        });
        Button editBtn = new Button("Bearbeiten", Icons.of(Icons.EDIT, 14));
        editBtn.setOnAction(e -> editClass());
        Button delBtn = new Button("Löschen", Icons.of(Icons.TRASH, 14));
        delBtn.setOnAction(e -> deleteClass());
        editBtn.disableProperty().bind(classList.getSelectionModel().selectedItemProperty().isNull());
        delBtn.disableProperty().bind(classList.getSelectionModel().selectedItemProperty().isNull());
        editBtn.getStyleClass().add("pill");
        delBtn.getStyleClass().add("pill");
        Label classesHeader = new Label("Klassen");
        classesHeader.getStyleClass().add("card-heading");
        VBox left = new VBox(10, classesHeader, classList, new HBox(6, editBtn, delBtn));
        VBox.setVgrow(classList, Priority.ALWAYS);
        left.getStyleClass().add("card");
        left.setMinWidth(240);

        // --- Verfügbare Teilnehmende ---
        setupAvailableTable();
        search.setPromptText("Suchen: Vorname, Nachname, Verein");
        search.textProperty().addListener((o, a, b) -> updateFilters());
        onlyMatching.setSelected(true);
        onlyMatching.selectedProperty().addListener((o, a, b) -> updateFilters());
        singlesFilter.setPromptText("Alle Teilnehmenden");
        singlesFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(PlayingClass c) {
                return c == null ? "Alle Teilnehmenden" : c.getName();
            }

            @Override
            public PlayingClass fromString(String s) {
                return null;
            }
        });
        singlesFilter.valueProperty().addListener((o, a, b) -> updateFilters());
        singlesFilter.setTooltip(new Tooltip("Nur Teilnehmende einer Einzelklasse anzeigen"));
        availableTitle.getStyleClass().add("card-heading");
        filterRow.getChildren().addAll(new Label("Einzelklasse:"), singlesFilter);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.managedProperty().bind(filterRow.visibleProperty());
        clubFilter.setPromptText("Alle Vereine");
        clubFilter.valueProperty().addListener((o, a, b) -> updateFilters());
        clubFilter.setTooltip(new Tooltip("Nach Verein filtern"));
        clubFilter.setPrefWidth(170);
        // Einzel: kein Vereinsfilter (die Suche findet auch Vereine); Doppel: bleibt – hilft beim Paaren
        clubFilter.managedProperty().bind(clubFilter.visibleProperty());
        sexFilter.getItems().setAll(de.hts.player.Sex.UNKNOWN, de.hts.player.Sex.FEMALE, de.hts.player.Sex.MALE);
        sexFilter.setValue(de.hts.player.Sex.UNKNOWN);
        sexFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(de.hts.player.Sex x) {
                if (x == null || x == de.hts.player.Sex.UNKNOWN) return "Alle";
                return x == de.hts.player.Sex.MALE ? "männlich" : "weiblich";
            }

            @Override
            public de.hts.player.Sex fromString(String v) {
                return null;
            }
        });
        sexFilter.valueProperty().addListener((o, a, b) -> updateFilters());
        sexFilter.setTooltip(new Tooltip("Nach Geschlecht filtern"));
        // gleiche Filter für „In der Klasse“
        inSearch.setPromptText("Suchen: Vorname, Nachname, Verein");
        inSearch.textProperty().addListener((o, a, b) -> updateFilters());
        inClubFilter.setPromptText("Alle Vereine");
        inClubFilter.valueProperty().addListener((o, a, b) -> updateFilters());
        inClubFilter.setTooltip(new Tooltip("Nach Verein filtern"));
        inClubFilter.setPrefWidth(170);
        inClubFilter.setVisible(false);
        inClubFilter.managedProperty().bind(inClubFilter.visibleProperty());
        inSexFilter.getItems().setAll(sexFilter.getItems());
        inSexFilter.setValue(de.hts.player.Sex.UNKNOWN);
        inSexFilter.setConverter(sexFilter.getConverter());
        inSexFilter.valueProperty().addListener((o, a, b) -> updateFilters());
        inSexFilter.setTooltip(new Tooltip("Nach Geschlecht filtern"));
        inSearch.setPrefColumnCount(10);
        search.setPrefColumnCount(10);
        HBox searchRow = new HBox(8, search, clubFilter, sexFilter, onlyMatching);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        newParticipantBtn.getStyleClass().add("pill");
        newParticipantBtn.setOnAction(e -> newParticipantInClass());
        newParticipantBtn.setTooltip(new Tooltip("Neue Person erfassen – sie steht danach hier unter „Verfügbar“ (Warteliste)"));
        Region availSpacer = new Region();
        HBox.setHgrow(availSpacer, Priority.ALWAYS);
        HBox availableHead = new HBox(10, availableTitle, availSpacer, newParticipantBtn);
        availableHead.setAlignment(Pos.CENTER_LEFT);
        VBox availableBox = new VBox(8, availableHead, filterRow, searchRow, availableTable);
        VBox.setVgrow(availableTable, Priority.ALWAYS);
        availableBox.getStyleClass().add("card");
        installDropTarget(availableBox, this::dropOnAvailable);

        // --- Einzel: zugeordnete Teilnehmende ---
        setupAssignedTable();
        assignedTitle.getStyleClass().add("card-heading");
        HBox inSearchRow = new HBox(8, inSearch, inClubFilter, inSexFilter);
        inSearchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inSearch, Priority.ALWAYS);
        singlesBox = new VBox(8, assignedTitle, inSearchRow, assignedTable);
        VBox.setVgrow(assignedTable, Priority.ALWAYS);
        installDropTarget(assignedTable, this::dropOnAssigned);

        // --- Doppel: Paarungen ---
        Label pairsTitle = new Label();
        pairsTitle.textProperty().bind(assignedTitle.textProperty());
        pairsTitle.getStyleClass().add("card-heading");
        ScrollPane scroll = new ScrollPane(pairsList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pairs-scroll");
        DrawView.installAutoScroll(scroll);              // beim Ziehen am Rand weiterblättern
        pairSearch.setPromptText("Suchen: Vorname, Nachname, Verein");
        pairSearch.textProperty().addListener((o, a, b) -> updateFilters());
        pairsList.setPadding(new Insets(2));
        partnerBtn.getStyleClass().add("pill");
        partnerBtn.setTooltip(new Tooltip("Alle Teilnehmenden ohne Partner/in einander rein zufällig zulosen – "
                + "danach sind es normale Doppel (per Drag & Drop änderbar). Bei ungerader Zahl bleibt eine/r übrig."));
        partnerBtn.setOnAction(e -> drawPartners());
        // Titel über die ganze Breite (Trefferzahl „3 von 8 …“), darunter Suche und „Partner zulosen“
        HBox pairsHead = new HBox(10, pairsTitle);
        pairsHead.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pairSearch, Priority.ALWAYS);
        partnerBtn.setMinWidth(Region.USE_PREF_SIZE);
        HBox pairsTools = new HBox(10, pairSearch, partnerBtn);
        pairsTools.setAlignment(Pos.CENTER_LEFT);
        doublesBox = new VBox(8, pairsHead, pairsTools, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        rightContent.getChildren().addAll(singlesBox, doublesBox);
        VBox.setVgrow(rightContent, Priority.ALWAYS);
        classTitle.getStyleClass().add("class-title");
        classCriteria.getStyleClass().add("criteria");
        classCriteria.setWrapText(true);
        classState.getStyleClass().add("chip");
        dndHint.getStyleClass().add("hint");
        dndHint.setWrapText(true);
        Button discard = new Button("Auslosung verwerfen", Icons.of(Icons.BACK, 14));
        discard.getStyleClass().add("pill");
        discard.setOnAction(e -> discardDraw());
        lockText.setWrapText(true);
        Region lockSpacer = new Region();
        HBox.setHgrow(lockSpacer, Priority.ALWAYS);
        lockBanner.getChildren().addAll(lockText, lockSpacer, discard);
        lockBanner.setAlignment(Pos.CENTER_LEFT);
        lockBanner.getStyleClass().add("lock-banner");
        lockBanner.managedProperty().bind(lockBanner.visibleProperty());
        lockBanner.setVisible(false);

        // „Teilnehmende der Klasse“: Verfügbar | Pfeile | In der Klasse
        VBox assignedBox = new VBox(8, rightContent);
        assignedBox.getStyleClass().add("inner-card");
        availableBox.getStyleClass().remove("card");
        availableBox.getStyleClass().add("inner-card");
        middle.setAlignment(Pos.CENTER);
        middle.setPadding(new Insets(0, 4, 0, 4));
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        HBox.setHgrow(assignedBox, Priority.ALWAYS);
        availableBox.setPrefWidth(480);
        assignedBox.setPrefWidth(480);
        HBox lists = new HBox(10, availableBox, middle, assignedBox);
        VBox.setVgrow(lists, Priority.ALWAYS);
        modeBtn.getStyleClass().add("navy-pill");
        modeBtn.setTooltip(new Tooltip("Die Teilnehmenden stehen fest: Modus und Optionen für Phase 1 wählen und auslosen"));
        modeBtn.setOnAction(e -> openModeDialog());
        modeBtn.managedProperty().bind(modeBtn.visibleProperty());
        Region modeGap = new Region();
        HBox.setHgrow(modeGap, Priority.ALWAYS);
        dndHint.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dndHint, Priority.SOMETIMES);
        HBox bottomBar = new HBox(10, dndHint, modeGap, modeBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        VBox participantsPane = new VBox(8, lockBanner, lists, bottomBar);
        participantsPane.setPadding(new Insets(10, 0, 0, 0));
        participantsTab.setContent(participantsPane);
        resultTab.setContent(resultView.getRoot());
        classTabs.getTabs().addAll(participantsTab);
        classTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        classTabs.getStyleClass().add("result-tabs");
        classTabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (!syncingTabs) showSelectedTab();
        });
        VBox.setVgrow(classTabs, Priority.ALWAYS);

        // fester Klassenkopf: Titel + Infos + Status
        VBox headText = new VBox(2, classTitle, classCriteria);
        headText.setMinWidth(0);
        HBox.setHgrow(headText, Priority.ALWAYS);
        classCriteria.setMinHeight(Region.USE_PREF_SIZE);
        classState.setMinWidth(Region.USE_PREF_SIZE);
        HBox classHead = new HBox(10, headText, classState);
        classHead.setAlignment(Pos.TOP_LEFT);
        VBox classCard = new VBox(6, classHead, classTabs);
        classCard.getStyleClass().add("card");

        SplitPane split = new SplitPane(left, classCard);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.2);
        split.getStyleClass().add("plain-split");
        SplitPane.setResizableWithParent(left, false);
        root.setCenter(split);
        root.setPadding(new Insets(0, 35, 16, 35));

        tournament.getPlayers().addListener((ListChangeListener<Player>) c -> refresh());
        tournament.getClasses().addListener((ListChangeListener<PlayingClass>) c -> {
            if (!tournament.getClasses().contains(selectedClass())) showClass(null);
            updateSinglesFilterItems();
        });
        updateSinglesFilterItems();
        showClass(null);
    }

    public BorderPane getRoot() {
        return root;
    }

    // --- Anzeige -----------------------------------------------------------

    private PlayingClass selectedClass() {
        return classList.getSelectionModel().getSelectedItem();
    }

    /** Wechselt zur Gruppenübersicht/zum Turnierbaum einer (z. B. gerade erstellten) Folgeklasse. */
    private void openClassDraw(String name) {
        if (DrawView.RESULT_TAB.equals(name)) {
            refresh();
            if (classTabs.getTabs().contains(resultTab)) classTabs.getSelectionModel().select(resultTab);
            return;
        }
        tournament.findClass(name).ifPresent(pc -> {
            PlayingClass first = tournament.rootOf(pc);
            if (selectedClass() != first) select(first);
            else refresh();
            phaseTabs.stream().filter(t -> name.equals(t.getUserData())).findFirst()
                    .ifPresent(t -> classTabs.getSelectionModel().select(t));
        });
    }

    /**
     * Reiter der Klasse: „Teilnehmende der Klasse“, je Phase ein Reiter („Phase 1 – Gruppenübersicht“,
     * „Phase 2 – KO-System“ …) und „Endergebnis“. Die Auswahl bleibt beim Aktualisieren erhalten.
     */
    private void syncTabs(PlayingClass c) {
        // „Phase 1“ erst, wenn die Teilnehmenden feststehen und der Modus festgelegt (= ausgelost) ist
        List<PlayingClass> phases = c == null || !c.isDrawn() ? List.of() : tournament.phasesOf(c);
        List<String> keys = phases.stream().map(PlayingClass::getName).toList();
        List<Object> old = phaseTabs.stream().map(Tab::getUserData).toList();
        if (phases.isEmpty() || !keys.equals(old)) {
            Tab selected = classTabs.getSelectionModel().getSelectedItem();
            boolean wasPhase = phaseTabs.contains(selected);
            Object selKey = selected == null ? null : selected.getUserData();
            syncingTabs = true;
            try {
                phaseTabs.forEach(t -> t.setContent(null));
                phaseTabs.clear();
                for (PlayingClass ph : phases) {
                    Tab t = new Tab();
                    t.setUserData(ph.getName());
                    phaseTabs.add(t);
                }
                List<Tab> all = new ArrayList<>();
                all.add(participantsTab);
                all.addAll(phaseTabs);
                if (classTabs.getTabs().contains(resultTab)) all.add(resultTab);
                classTabs.getTabs().setAll(all);
                Tab target = selected == resultTab ? resultTab : participantsTab;
                if (wasPhase && !phaseTabs.isEmpty()) {
                    target = phaseTabs.stream().filter(t -> t.getUserData().equals(selKey)).findFirst().orElse(phaseTabs.get(0));
                }
                classTabs.getSelectionModel().select(target);
            } finally {
                syncingTabs = false;
            }
        }
        for (int i = 0; i < phases.size(); i++) phaseTabs.get(i).setText(DrawView.phaseTabTitle(i + 1, phases.get(i)));
        // „Endergebnis“ erst, wenn die Klasse beendet ist (nach dem letzten Spiel bzw. „Klasse beenden“)
        boolean finished = c != null && c.isFinished();
        if (finished && !classTabs.getTabs().contains(resultTab)) {
            classTabs.getTabs().add(resultTab);
        } else if (!finished && classTabs.getTabs().contains(resultTab)) {
            syncingTabs = true;
            try {
                if (classTabs.getSelectionModel().getSelectedItem() == resultTab) {
                    classTabs.getSelectionModel().select(phaseTabs.isEmpty() ? participantsTab : phaseTabs.get(phaseTabs.size() - 1));
                }
                classTabs.getTabs().remove(resultTab);
            } finally {
                syncingTabs = false;
            }
        }
        showSelectedTab();
    }

    private void showSelectedTab() {
        Tab t = classTabs.getSelectionModel().getSelectedItem();
        for (Tab p : phaseTabs) if (p != t) p.setContent(null);
        if (t != null && phaseTabs.contains(t)) {
            if (t.getContent() == null) t.setContent(drawView.getRoot());
            String key = (String) t.getUserData();
            drawView.show(key.isEmpty() ? selectedClass() : tournament.findClass(key).orElse(selectedClass()));
        } else if (t == resultTab) {
            resultView.show(selectedClass());
        }
    }

    /** „Modus festlegen“: Optionen für Phase 1 wählen, auslosen und zu „Phase 1“ wechseln. */
    private void openModeDialog() {
        PlayingClass c = selectedClass();
        if (c == null) return;
        if (Boolean.TRUE.equals(new ModeDialog(window(), tournament, c, random).showAndWait().orElse(false))) {
            status.accept("„" + c.getName() + "“ ausgelost – Phase 1 steht.");
            changed();
            if (!phaseTabs.isEmpty()) classTabs.getSelectionModel().select(phaseTabs.get(0));
        }
    }

    public void select(PlayingClass c) {
        classList.getSelectionModel().select(c);
        classList.scrollTo(c);
    }

    private static final String ALL_CLUBS = "Alle Vereine";

    private void updateClubFilterItems() {
        String cur = clubFilter.getValue();
        List<String> clubs = new ArrayList<>();
        clubs.add(ALL_CLUBS);
        tournament.getPlayers().stream().map(Player::getClubName).filter(java.util.Objects::nonNull).distinct()
                .sorted(Collator.getInstance(Locale.GERMAN)).forEach(clubs::add);
        if (!clubs.equals(clubFilter.getItems())) clubFilter.getItems().setAll(clubs);
        clubFilter.setValue(cur != null && clubs.contains(cur) ? cur : ALL_CLUBS);
        String curIn = inClubFilter.getValue();
        if (!clubs.equals(inClubFilter.getItems())) inClubFilter.getItems().setAll(clubs);
        inClubFilter.setValue(curIn != null && clubs.contains(curIn) ? curIn : ALL_CLUBS);
    }

    private void updateSinglesFilterItems() {
        PlayingClass current = singlesFilter.getValue();
        List<PlayingClass> items = new ArrayList<>();
        items.add(null);
        tournament.getClasses().stream().filter(c -> !c.isDoubles() && !c.isPhase()).forEach(items::add);
        singlesFilter.getItems().setAll(items);
        singlesFilter.setValue(items.contains(current) ? current : null);
    }

    /**
     * Beim Auswählen einer Klasse: beendete Klasse → „Endergebnis“, ausgeloste Klasse → aktuellste Phase,
     * sonst „Teilnehmende der Klasse“.
     */
    private void openDefaultTab(PlayingClass c) {
        Tab target = c.isFinished() && classTabs.getTabs().contains(resultTab) ? resultTab
                : !phaseTabs.isEmpty() ? phaseTabs.get(phaseTabs.size() - 1) : participantsTab;
        if (classTabs.getSelectionModel().getSelectedItem() != target) classTabs.getSelectionModel().select(target);
        else showSelectedTab();
    }

    private void showClass(PlayingClass c) {
        boolean has = c != null;
        boolean dbl = has && c.isDoubles();
        availableTable.setDisable(!has);
        newParticipantBtn.setDisable(!has);
        boolean locked = has && c.isDrawn();
        lockBanner.setVisible(locked);
        if (locked) {
            lockText.setText("„" + c.getName() + "“ ist ausgelost (" + c.getMode().getLabel() + "). "
                    + "Die Teilnehmenden sind festgelegt – zum Ändern die Auslosung verwerfen.");
        }
        middle.setDisable(locked);
        classTabs.setDisable(!has);
        modeBtn.setVisible(has && !c.isDrawn());
        modeBtn.setDisable(!has || !tournament.canChooseMode(c));
        updateClassState(c);
        syncTabs(c);
        search.setDisable(!has);
        singlesFilter.setDisable(!has);
        onlyMatching.setDisable(!has || c.acceptsEveryone());
        singlesBox.setVisible(!dbl);
        doublesBox.setVisible(dbl);
        if (partnerColumn != null) partnerColumn.setVisible(dbl);
        filterRow.setVisible(dbl);
        clubFilter.setVisible(dbl);
        if (!dbl && clubFilter.getValue() != null && !ALL_CLUBS.equals(clubFilter.getValue())) clubFilter.setValue(ALL_CLUBS);
        if (!dbl) singlesFilter.setValue(null);
        if (has) {
            classTitle.setText(c.getName());
            long n = c.isDoubles() ? tournament.pairsIn(c).stream().filter(DoublesPair::isComplete).count() : tournament.countIn(c);
            classCriteria.setText(c.getDiscipline().getLabel() + " · " + n + (c.isDoubles() ? " Doppel" : " Teilnehmende")
                    + " in der Klasse · " + c.getType().getLabel()
                    + (c.getMode() != de.hts.tournament.TournamentMode.NONE ? " · " + c.getMode().getLabel() : ""));
            classCriteria.setTooltip(new Tooltip(tournament.criteriaText(c)));
            // Vorgabeturnier: vor dem Start markieren, was den Start sperrt (z. B. Teilnehmende ohne TTR)
            classCriteria.getStyleClass().remove("handicap-problem");
            if (tournament.isHandicap() && !c.isStarted() && n > 0) {
                java.util.List<String> hc = tournament.handicapProblems(c);
                if (!hc.isEmpty()) {
                    classCriteria.setText(classCriteria.getText() + "\n⚠ Start gesperrt: " + String.join(" · ", hc));
                    classCriteria.setTooltip(new Tooltip(String.join("\n", hc)));
                    classCriteria.getStyleClass().add("handicap-problem");
                }
            }
            dndHint.setText(dbl
                    ? "Teilnehmende aus der linken Liste auf einen freien Platz ziehen oder auf „Neues Doppel“. "
                    + "Aus einer Paarung zurück in die Liste ziehen entfernt sie. In der Klasse = angemeldet."
                    : "Teilnehmende per Drag & Drop, Doppelklick oder mit den Pfeil-Knöpfen zuordnen. "
                    + "In der Klasse = angemeldet, Verfügbar = nicht angemeldet.");
        } else {
            classTitle.setText(tournament.getClasses().isEmpty() ? "Noch keine Klassen" : "Keine Klasse ausgewählt");
            classCriteria.setText("Links eine Klasse auswählen oder mit „Klasse anlegen“ eine neue erfassen.");
            dndHint.setText("");
        }
        buildMiddleButtons(c);
        updateFilters();
    }

    private void updateClassState(PlayingClass c) {
        classState.getStyleClass().removeAll("chip-planned", "chip-started", "chip-ready", "chip-paused", "chip-finished");
        if (c == null) {
            classState.setText("");
            classState.setVisible(false);
            return;
        }
        classState.setVisible(true);
        if (c.isFinished()) {
            classState.setText("beendet");
            classState.getStyleClass().add("chip-finished");
        } else if (tournament.phasesOf(c).stream().anyMatch(PlayingClass::isPaused)) {
            classState.setText("angehalten");
            classState.getStyleClass().add("chip-paused");
        } else if (c.isStarted()) {
            int n = tournament.phasesOf(c).size();
            classState.setText(n > 1 ? "läuft · Phase " + n : "läuft");
            classState.getStyleClass().add("chip-started");
        } else if (c.isReady()) {
            classState.setText("bereit");
            classState.getStyleClass().add("chip-ready");
        } else if (c.isDrawn()) {
            classState.setText("ausgelost");
            classState.getStyleClass().add("chip-planned");
        } else {
            classState.setText("in Vorbereitung");
            classState.getStyleClass().add("chip-planned");
        }
    }

    private void buildMiddleButtons(PlayingClass c) {
        middle.getChildren().clear();
        if (c == null) return;
        if (c.isDoubles()) {
            Button pair = new Button("Als Doppel →");
            pair.setTooltip(new Tooltip("Ein oder zwei markierte Teilnehmende als neue Paarung anlegen"));
            pair.setOnAction(e -> pairSelected());
            pair.disableProperty().bind(availableTable.getSelectionModel().selectedItemProperty().isNull());
            pair.setMaxWidth(Double.MAX_VALUE);
            middle.getChildren().add(pair);
        } else {
            Button add = new Button("→");
            add.setTooltip(new Tooltip("Markierte hinzufügen"));
            add.setOnAction(e -> addSelected());
            add.disableProperty().bind(availableTable.getSelectionModel().selectedItemProperty().isNull());
            Button remove = new Button("←");
            remove.setTooltip(new Tooltip("Markierte entfernen"));
            remove.setOnAction(e -> removeSelected());
            remove.disableProperty().bind(assignedTable.getSelectionModel().selectedItemProperty().isNull());
            add.getStyleClass().add("round-button");
            remove.getStyleClass().add("round-button");
            middle.getChildren().addAll(add, remove);
        }
    }

    private void updateFilters() {
        PlayingClass c = selectedClass();
        if (c == null) {
            available.setPredicate(p -> false);
            assigned.setPredicate(p -> false);
            availableTitle.setText("Teilnehmende");
            assignedTitle.setText("");
            pairsList.getChildren().clear();
            return;
        }
        String q = search.getText();
        PlayingClass filter = singlesFilter.getValue();
        boolean matchOnly = onlyMatching.isSelected() && !c.acceptsEveryone();
        PlayingClass f = c.isDoubles() ? filter : null;
        String club = clubFilter.getValue();
        de.hts.player.Sex sx = sexFilter.getValue();
        available.setPredicate(p -> (c.isDoubles() || !p.isInClass(c.getName()))
                && (club == null || club.equals(ALL_CLUBS) || club.equals(p.getClubName()))
                && (sx == null || sx == de.hts.player.Sex.UNKNOWN || p.getSex() == sx)
                && (f == null || p.isInClass(f.getName()))
                && (!matchOnly || tournament.fit(c, p) != PlayingClass.Fit.NO)
                && matchesName(p, q));
        String inQ = inSearch.getText();
        String inClub = inClubFilter.getValue();
        de.hts.player.Sex inSx = inSexFilter.getValue();
        assigned.setPredicate(p -> p.isInClass(c.getName())
                && (inClub == null || inClub.equals(ALL_CLUBS) || inClub.equals(p.getClubName()))
                && (inSx == null || inSx == de.hts.player.Sex.UNKNOWN || p.getSex() == inSx)
                && matchesName(p, inQ));
        long inTotal = tournament.getPlayers().stream().filter(p -> p.isInClass(c.getName())).count();

        availableTitle.setText("Verfügbar (" + available.size() + ")");
        if (c.isDoubles()) {
            List<DoublesPair> pairs = tournament.pairsIn(c);
            long complete = pairs.stream().filter(DoublesPair::isComplete).count();
            long incomplete = pairs.size() - complete;
            int shown = rebuildPairs(c);
            String pq = pairSearch.getText() == null ? "" : pairSearch.getText().trim();
            assignedTitle.setText("Doppelpaarungen (" + (pq.isEmpty() ? "" : shown + " von ") + complete + maxSuffix(c)
                    + (incomplete > 0 ? ", " + incomplete + " ohne Partner/in" : "") + ")");
        } else {
            assignedTitle.setText("In der Klasse (" + (assigned.size() == inTotal ? String.valueOf(inTotal)
                    : assigned.size() + " von " + inTotal) + maxSuffix(c) + ")");
        }
    }

    private static String maxSuffix(PlayingClass c) {
        return c.getMaxParticipants() == null ? "" : " von max. " + c.getMaxParticipants();
    }

    /** Jedes Suchwort muss im Vor- oder Nachnamen vorkommen. */
    private static boolean matchesName(Player p, String q) {
        return PlayerFilter.matchesPerson(p, q);       // Vorname, Nachname, Verein
    }

    /** Nach Änderungen von außen (z. B. Import, Seitenwechsel) neu darstellen. */
    public void refresh() {
        classList.refresh();
        availableTable.refresh();
        assignedTable.refresh();
        updateSinglesFilterItems();
        updateClubFilterItems();
        PlayingClass c = selectedClass();
        if (c != null) showClass(c);
        else updateFilters();
    }

    private void changed() {
        refresh();
        onChanged.run();
    }

    // --- Doppel: Paarungskarten ---------------------------------------------

    private void drawPartners() {
        PlayingClass c = selectedClass();
        if (c == null || !c.isDoubles()) return;
        try {
            Tournament.PartnerDraw r = tournament.drawPartners(c, new java.util.Random());
            String msg = r.pairs().size() + " Doppel zugelost"
                    + (r.leftOver() != null ? " – übrig (ohne Partner/in, spielt so nicht mit): " + r.leftOver().getDisplayName() : "") + ".";
            status.accept(msg);
            if (r.leftOver() != null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, r.pairs().size() + " Doppel wurden zugelost.\n\n"
                        + r.leftOver().getDisplayName() + " bleibt übrig (ungerade Zahl) und spielt ohne Partner/in nicht mit.",
                        ButtonType.OK);
                a.initOwner(window());
                a.setHeaderText(null);
                a.setTitle("Partner zulosen");
                a.showAndWait();
            }
            changed();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            status.accept(ex.getMessage());
        }
    }

    /** @return Anzahl der angezeigten Paarungen (Suche) */
    private int rebuildPairs(PlayingClass c) {
        long open = tournament.pairsIn(c).stream().filter(p -> !p.isComplete()).count()
                + tournament.getPlayers().stream().filter(p -> p.getRegistrations().contains(c.getName())
                && tournament.pairOf(p, c).isEmpty()).count();
        partnerBtn.setDisable(open < 2 || c.isDrawn() || c.isStarted());
        partnerBtn.setText(open >= 2 ? "Partner zulosen (" + open + ")" : "Partner zulosen");
        pairsList.getChildren().clear();
        int i = 1, shown = 0;
        String q = pairSearch.getText() == null ? "" : pairSearch.getText().trim();
        java.util.Set<String> over = tournament.overLimit(c);
        for (DoublesPair pair : tournament.pairsIn(c)) {
            int number = i++;              // Nummer bleibt auch bei aktiver Suche die ursprüngliche
            if (!q.isEmpty() && !PlayerFilter.matchesPerson(pair.getFirst(), q) && !PlayerFilter.matchesPerson(pair.getSecond(), q)) continue;
            shown++;
            Node card = pairCard(c, pair, number);
            if (over.contains(pair.getId())) {
                card.getStyleClass().add("over-limit");
                Tooltip.install(card, new Tooltip("Über der Höchstzahl von " + c.getMaxParticipants() + " Doppeln"));
            }
            pairsList.getChildren().add(card);
        }
        pairsList.getChildren().add(newPairDropZone(c));
        return shown;
    }

    private Node pairCard(PlayingClass c, DoublesPair pair, int number) {
        Label nr = new Label(number + ".");
        nr.getStyleClass().add("pair-number");
        nr.setMinWidth(24);


        Node slot1 = slot(c, pair, pair.getFirst());
        Node slot2 = slot(c, pair, pair.getSecond());
        Label amp = new Label("&");
        amp.getStyleClass().add("hint");
        HBox.setHgrow(slot1, Priority.ALWAYS);
        HBox.setHgrow(slot2, Priority.ALWAYS);

        Button remove = new Button("", Icons.of(Icons.TRASH, 14));
        remove.getStyleClass().add("icon-button");
        remove.setTooltip(new Tooltip("Paarung auflösen"));
        remove.setDisable(c.isDrawn());
        remove.setOnAction(e -> {
            if (isLocked(c)) return;
            tournament.removePair(pair);
            status.accept("Paarung " + pair.getDisplayName() + " aufgelöst.");
            changed();
        });

        Integer avg = pair.getAverageTtr();
        Label avgLabel = new Label(avg == null ? "Ø TTR –" : "Ø TTR " + avg);
        avgLabel.getStyleClass().add("avg-ttr");
        avgLabel.setMinWidth(78);
        avgLabel.setTooltip(new Tooltip("TTR des Doppels für die Setzung: (TTR 1 + TTR 2) / 2; fehlt einer, zählt der Partnerwert; fehlen beide, 0"));
        HBox card = new HBox(10, nr, slot1, amp, slot2, avgLabel, remove);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("pair-card");
        if (!pair.isComplete()) card.getStyleClass().add("incomplete");

        // Ablegen auf der Karte: freien Platz füllen
        installDropTarget(card, players -> dropOnPair(pair, players));
        return card;
    }

    private Node slot(PlayingClass c, DoublesPair pair, Player p) {
        if (p == null) {
            Label empty = new Label("Partner/in hierher ziehen");
            empty.getStyleClass().add("slot-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            return empty;
        }
        Label l = new Label(p.getDisplayName() + (p.getTtr() != null ? "  (" + p.getTtr() + ")" : ""));
        l.getStyleClass().add("slot");
        l.setMaxWidth(Double.MAX_VALUE);
        PlayingClass.Fit fit = tournament.fit(c, p);
        if (fit == PlayingClass.Fit.NO) {
            l.getStyleClass().add("slot-misfit");
            l.setTooltip(new Tooltip("Erfüllt die Kriterien der Klasse nicht: " + tournament.criteriaText(c)));
        } else {
            l.setTooltip(new Tooltip(p.getClubName() == null ? p.getDisplayName() : p.getDisplayName() + ", " + p.getClubName()));
        }
        // aus der Paarung herausziehen
        l.setOnDragDetected(e -> {
            startDrag(l, List.of(p), pair);
            e.consume();
        });
        return l;
    }

    private Node newPairDropZone(PlayingClass c) {
        Label zone = new Label("＋  Neues Doppel – eine oder zwei Personen hierher ziehen", null);
        zone.getStyleClass().add("drop-zone");
        zone.setMaxWidth(Double.MAX_VALUE);
        installDropTarget(zone, players -> dropOnNewPair(c, players));
        return zone;
    }

    // --- Drag & Drop ---------------------------------------------------------

    /** Solange die Klasse ausgelost ist, sind die Teilnehmenden festgelegt. */
    private boolean isLocked(PlayingClass c) {
        if (c != null && c.isDrawn()) {
            status.accept("„" + c.getName() + "“ ist ausgelost – zum Ändern der Teilnehmenden zuerst die Auslosung verwerfen.");
            return true;
        }
        return false;
    }

    private void discardDraw() {
        PlayingClass c = selectedClass();
        if (c == null || !c.isDrawn()) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Auslosung von „" + c.getName() + "“ verwerfen?",
                ButtonType.YES, ButtonType.NO);
        a.initOwner(window());
        a.setHeaderText(null);
        a.setTitle("Auslosung verwerfen");
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            tournament.clearDraw(c);
            status.accept("Auslosung von „" + c.getName() + "“ verworfen – Teilnehmende können wieder geändert werden.");
            changed();
        }
    }

    private void startDrag(Node source, List<Player> players, DoublesPair fromPair) {
        if (players.isEmpty()) return;
        dragged = List.copyOf(players);
        draggedFromPair = fromPair;
        Dragboard db = source.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        content.putString(DND_PREFIX + players.stream().map(Player::getId).reduce((a, b) -> a + "," + b).orElse(""));
        db.setContent(content);
        db.setDragView(source.snapshot(null, null));
    }

    private boolean isOurDrag(DragEvent e) {
        return e.getDragboard().hasString() && e.getDragboard().getString().startsWith(DND_PREFIX) && !dragged.isEmpty();
    }

    /** Macht einen Bereich zum Ablageziel; {@code onDrop} liefert {@code true}, wenn das Ablegen geklappt hat. */
    private void installDropTarget(Node target, Function<List<Player>, Boolean> onDrop) {
        target.setOnDragOver(e -> {
            if (isOurDrag(e) && e.getGestureSource() != target) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        target.setOnDragEntered(e -> {
            if (isOurDrag(e)) target.getStyleClass().add("drop-hover");
            e.consume();
        });
        target.setOnDragExited(e -> {
            target.getStyleClass().remove("drop-hover");
            e.consume();
        });
        target.setOnDragDropped(e -> {
            boolean ok = false;
            if (isOurDrag(e)) {
                try {
                    ok = onDrop.apply(dragged);
                } catch (RuntimeException ex) {
                    status.accept(ex.getMessage());
                }
            }
            e.setDropCompleted(ok);
            e.consume();
            dragged = List.of();
            draggedFromPair = null;
            if (ok) changed();
        });
    }

    private boolean dropOnAvailable(List<Player> players) {
        PlayingClass c = selectedClass();
        if (c == null) return false;
        if (draggedFromPair != null || players.stream().anyMatch(p -> p.isInClass(c.getName()))) {
            if (isLocked(c)) return false;
        }
        if (c.isDoubles()) {
            if (draggedFromPair == null) return false; // aus der Liste in die Liste: nichts zu tun
            players.forEach(p -> tournament.removeFromPair(draggedFromPair, p));
            status.accept(names(players) + " aus „" + c.getName() + "“ entfernt.");
            return true;
        }
        if (players.stream().noneMatch(p -> p.isInClass(c.getName()))) return false;
        players.forEach(p -> tournament.unassign(p, c));
        status.accept(players.size() + " Teilnehmende aus „" + c.getName() + "“ entfernt.");
        return true;
    }

    private boolean dropOnAssigned(List<Player> players) {
        PlayingClass c = selectedClass();
        if (c == null || c.isDoubles() || isLocked(c)) return false;
        int n = 0;
        for (Player p : players) {
            if (tournament.assign(p, c)) n++;
        }
        status.accept(n + " Teilnehmende zu „" + c.getName() + "“ hinzugefügt.");
        return n > 0;
    }

    private boolean dropOnPair(DoublesPair pair, List<Player> players) {
        if (isLocked(selectedClass())) return false;
        List<Player> incoming = players.stream().filter(p -> !pair.contains(p)).toList();
        int free = (pair.getFirst() == null ? 1 : 0) + (pair.getSecond() == null ? 1 : 0);
        if (incoming.isEmpty()) return false;
        if (incoming.size() > free) {
            status.accept(free == 0
                    ? "Die Paarung " + pair.getDisplayName() + " ist schon vollständig – zuerst jemanden herausziehen."
                    : "Auf diese Paarung passt nur noch eine Person.");
            return false;
        }
        incoming.forEach(p -> tournament.addToPair(pair, p));
        status.accept("Paarung: " + pair.getDisplayName());
        return true;
    }

    private boolean dropOnNewPair(PlayingClass c, List<Player> players) {
        if (isLocked(c)) return false;
        if (players.size() > 2) {
            status.accept("Ein Doppel besteht aus genau zwei Personen – bitte höchstens zwei ziehen.");
            return false;
        }
        DoublesPair pair = tournament.createPair(c, players.get(0), players.size() > 1 ? players.get(1) : null);
        status.accept("Neue Paarung: " + pair.getDisplayName()
                + (pair.isComplete() ? "" : " – Partner/in noch auf die Karte ziehen."));
        return true;
    }

    private void pairSelected() {
        PlayingClass c = selectedClass();
        if (c == null || !c.isDoubles()) return;
        List<Player> sel = new ArrayList<>(availableTable.getSelectionModel().getSelectedItems());
        if (dropOnNewPair(c, sel)) changed();
    }

    /**
     * Neue Person erfassen (Pflichtangaben ohne Geburtsdatum). Sie steht danach unter „Verfügbar“
     * mit Wartelisten-Kreuz für diese Klasse und wird per Drag &amp; Drop in die Klasse gezogen.
     */
    private void newParticipantInClass() {
        PlayingClass c = selectedClass();
        if (c == null) return;
        Player p = new Player();
        if (!Boolean.TRUE.equals(new ParticipantDialog(window(), p, true, null, true, tournament.isHandicap()).showAndWait().orElse(false))) return;
        p.addWaitingList(c.getName());
        tournament.getPlayers().add(p);
        status.accept(p.getDisplayName() + " erfasst – steht unter „Verfügbar“ (Warteliste „" + c.getName() + "“).");
        changed();
        availableTable.getSelectionModel().clearSelection();
        availableTable.getSelectionModel().select(p);
        availableTable.scrollTo(p);
    }

    private static String names(List<Player> players) {
        return players.stream().map(Player::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("");
    }

    // --- Einzel: Aktionen ------------------------------------------------------

    private void addSelected() {
        PlayingClass c = selectedClass();
        if (c == null || isLocked(c)) return;
        List<Player> sel = new ArrayList<>(availableTable.getSelectionModel().getSelectedItems());
        long misfit = sel.stream().filter(p -> tournament.fit(c, p) == PlayingClass.Fit.NO).count();
        int n = 0;
        for (Player p : sel) {
            if (tournament.assign(p, c)) n++;
        }
        status.accept(n + " Teilnehmende zu „" + c.getName() + "“ hinzugefügt"
                + (misfit > 0 ? " (" + misfit + " davon außerhalb der Kriterien)." : "."));
        changed();
    }

    private void removeSelected() {
        PlayingClass c = selectedClass();
        if (c == null || isLocked(c)) return;
        List<Player> sel = new ArrayList<>(assignedTable.getSelectionModel().getSelectedItems());
        sel.forEach(p -> tournament.unassign(p, c));
        status.accept(sel.size() + " Teilnehmende aus „" + c.getName() + "“ entfernt.");
        changed();
    }

    // --- Klassen ---------------------------------------------------------------

    public void newClass() {
        ClassDialog d = new ClassDialog(window(), tournament, null);
        if (Boolean.TRUE.equals(d.showAndWait().orElse(false))) {
            PlayingClass c = d.getPlayingClass();
            status.accept("Klasse „" + c.getName() + "“ angelegt.");
            select(c);
            changed();
        }
    }

    private void editClass() {
        PlayingClass c = selectedClass();
        if (c == null) return;
        ClassDialog d = new ClassDialog(window(), tournament, c);
        if (Boolean.TRUE.equals(d.showAndWait().orElse(false))) {
            status.accept("Klasse „" + c.getName() + "“ gespeichert.");
            showClass(c);
            changed();
        }
    }

    private void deleteClass() {
        PlayingClass c = selectedClass();
        if (c == null) return;
        long n = tournament.countIn(c);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Klasse „" + c.getName() + "“ löschen?" + (n > 0 ? "\n" + n + " Zuordnungen von Teilnehmenden gehen dabei verloren." : ""),
                ButtonType.YES, ButtonType.NO);
        a.initOwner(window());
        a.setHeaderText(null);
        a.setTitle("Klasse löschen");
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            tournament.removeClass(c);
            status.accept("Klasse „" + c.getName() + "“ gelöscht.");
            changed();
        }
    }

    // --- Tabellen --------------------------------------------------------------

    private void setupAvailableTable() {
        TableColumn<Player, String> fit = fitColumn();
        TableColumn<Player, String> last = textCol("Nachname", Player::getLastname, 110);
        TableColumn<Player, String> first = textCol("Vorname", Player::getFirstname, 90);
        partnerColumn = col("Doppelpartner/in", p -> {
            PlayingClass c = selectedClass();
            if (c == null || !c.isDoubles()) return "";
            return tournament.pairOf(p, c).map(pair -> {
                Player partner = pair.partnerOf(p);
                return partner == null ? "(ohne Partner/in)" : partner.getDisplayName();
            }).orElse("");
        }, 130);
        partnerColumn.setSortable(false);
        setupTable(availableTable, available, List.of(fit, last, first, birthCol(), sexCol(), ttrCol(), partnerColumn, clubCol()),
                last, first, false);
        availableTable.setPlaceholder(new Label("Keine passenden Teilnehmenden."));
    }

    private void setupAssignedTable() {
        TableColumn<Player, String> last = textCol("Nachname", Player::getLastname, 110);
        TableColumn<Player, String> first = textCol("Vorname", Player::getFirstname, 90);
        TableColumn<Player, Integer> nr = col("Nr.", p -> {
            PlayingClass c = selectedClass();
            if (c == null) return null;
            int i = tournament.playersInOrder(c).indexOf(p);
            return i < 0 ? null : i + 1;
        }, 44);
        nr.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        nr.setStyle("-fx-alignment: CENTER-RIGHT;");
        setupTable(assignedTable, assigned, List.of(nr, last, first, birthCol(), sexCol(), ttrCol(), fitColumn(), clubCol()),
                last, first, true);
        assignedTable.setPlaceholder(new Label("Noch niemand zugeordnet.\nTeilnehmende von links hierher ziehen."));
    }

    private void setupTable(TableView<Player> table, FilteredList<Player> items, List<TableColumn<Player, ?>> columns,
                            TableColumn<Player, String> last, TableColumn<Player, String> first, boolean assignedSide) {
        SortedList<Player> sorted = new SortedList<>(items);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(columns);
        table.getSortOrder().addAll(List.of(last, first));

        table.setRowFactory(tv -> {
            TableRow<Player> row = new TableRow<>() {
                @Override
                protected void updateItem(Player p, boolean empty) {
                    super.updateItem(p, empty);
                    getStyleClass().removeAll("misfit", "waiting-list", "paired", "over-limit");
                    setTooltip(null);
                    PlayingClass c = selectedClass();
                    if (empty || p == null || c == null) return;
                    PlayingClass.Fit fit = tournament.fit(c, p);
                    if (fit == PlayingClass.Fit.NO) {
                        getStyleClass().add("misfit");
                        setTooltip(new Tooltip("Erfüllt die Kriterien der Klasse nicht: " + tournament.criteriaText(c)));
                    } else if (fit == PlayingClass.Fit.UNKNOWN) {
                        setTooltip(new Tooltip("Jahrgang oder Geschlecht fehlt – Kriterien nicht prüfbar"));
                    }
                    if (!assignedSide && c.isDoubles() && tournament.pairOf(p, c).isPresent()) {
                        getStyleClass().add("paired");   // schon in einem Doppel (auch wenn Partner/in noch fehlt)
                    }
                    if (assignedSide && tournament.overLimit(c).contains(p.getId())) {
                        getStyleClass().add("over-limit");
                        setTooltip(new Tooltip("Über der Höchstzahl von " + c.getMaxParticipants() + " Teilnehmenden"));
                    }
                    if (!assignedSide && p.getWaitingLists().contains(c.getName())) {
                        getStyleClass().add("waiting-list");
                        setTooltip(new Tooltip("Laut Meldeliste auf der Warteliste für „" + c.getName() + "“"));
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    PlayingClass c = selectedClass();
                    if (c == null || c.isDoubles()) return;
                    table.getSelectionModel().clearAndSelect(row.getIndex());
                    if (assignedSide) removeSelected();
                    else addSelected();
                }
            });
            row.setOnDragDetected(e -> {
                if (row.isEmpty()) return;
                if (!table.getSelectionModel().getSelectedItems().contains(row.getItem())) {
                    table.getSelectionModel().clearAndSelect(row.getIndex());
                }
                startDrag(row, new ArrayList<>(table.getSelectionModel().getSelectedItems()), null);
                e.consume();
            });
            return row;
        });
    }

    private TableColumn<Player, String> fitColumn() {
        TableColumn<Player, String> fit = col("", p -> {
            PlayingClass c = selectedClass();
            if (c == null || c.acceptsEveryone()) return "";
            return switch (tournament.fit(c, p)) {
                case YES -> "✓";
                case NO -> "✗";
                case UNKNOWN -> "?";
            };
        }, 28);
        fit.setSortable(false);
        fit.setStyle("-fx-alignment: CENTER;");
        return fit;
    }

    private static TableColumn<Player, String> textCol(String title, Function<Player, String> getter, double width) {
        TableColumn<Player, String> c = col(title, getter, width);
        c.setComparator(Comparator.nullsLast(Collator.getInstance(Locale.GERMAN)::compare));
        return c;
    }

    private static TableColumn<Player, String> birthCol() {
        TableColumn<Player, String> c = col("Jg.", Player::getBirthText, 70);
        c.setSortable(false);
        return c;
    }

    private static TableColumn<Player, String> sexCol() {
        TableColumn<Player, String> c = col("G", p -> p.getSex().getShortLabel(), 30);
        c.setSortable(false);
        c.setStyle("-fx-alignment: CENTER;");
        return c;
    }

    private static TableColumn<Player, Integer> ttrCol() {
        TableColumn<Player, Integer> c = col("TTR", Player::getTtr, 50);
        c.setComparator(Comparator.nullsLast(Comparator.naturalOrder()));
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    private static TableColumn<Player, String> clubCol() {
        TableColumn<Player, String> c = col("Verein", Player::getClubName, 130);
        c.setSortable(false);
        return c;
    }

    private static <T> TableColumn<Player, T> col(String title, Function<Player, T> getter, double width) {
        TableColumn<Player, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    /** Listeneintrag einer Klasse: Name, Einzel/Doppel, Anzahl, Kriterien. Teilnehmende lassen sich auf eine Klasse ziehen. */
    private final class ClassCell extends ListCell<PlayingClass> {
        ClassCell() {
            setOnDragOver(e -> {
                PlayingClass c = getItem();
                if (c != null && isOurDrag(e)) e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            });
            setOnDragEntered(e -> {
                if (getItem() != null && isOurDrag(e)) getStyleClass().add("drop-hover");
            });
            setOnDragExited(e -> getStyleClass().remove("drop-hover"));
            setOnDragDropped(e -> {
                PlayingClass c = getItem();
                boolean ok = false;
                if (c != null && isOurDrag(e) && !isLocked(c)) {
                    List<Player> players = dragged;
                    if (c.isDoubles()) {
                        if (players.size() <= 2) {
                            tournament.createPair(c, players.get(0), players.size() > 1 ? players.get(1) : null);
                            status.accept("Neue Paarung in „" + c.getName() + "“.");
                            ok = true;
                        } else {
                            status.accept("Ein Doppel besteht aus genau zwei Personen.");
                        }
                    } else {
                        int n = 0;
                        for (Player p : players) if (tournament.assign(p, c)) n++;
                        status.accept(n + " Teilnehmende zu „" + c.getName() + "“ hinzugefügt.");
                        ok = n > 0;
                    }
                }
                e.setDropCompleted(ok);
                e.consume();
                dragged = List.of();
                draggedFromPair = null;
                if (ok) changed();
            });
        }

        @Override
        protected void updateItem(PlayingClass c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label name = new Label(c.getName());
            name.getStyleClass().add("class-name");
            Label badge = new Label(c.getDiscipline().getLabel());
            badge.getStyleClass().addAll("badge", c.isDoubles() ? "badge-doubles" : "badge-singles");
            String countText = c.isDoubles()
                    ? tournament.pairsIn(c).stream().filter(DoublesPair::isComplete).count() + " Doppel"
                    : tournament.countIn(c) + " Teilnehmende";
            Label count = new Label(countText);
            count.getStyleClass().add("hint");
            Label crit = new Label(c.getType().getLabel() + criteriaSuffix(c)
                    + (c.isFinished() ? " · beendet" : c.isStarted() ? " · läuft" : ""));
            crit.getStyleClass().add("hint");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(8, name, spacer, badge);
            top.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(new VBox(2, top, new HBox(8, count, crit)));
        }

        private String criteriaSuffix(PlayingClass c) {
            if (c.getBirthyearFrom() != null && c.getBirthyearTo() != null) return " · Jg. " + c.getBirthyearFrom() + "–" + c.getBirthyearTo();
            if (c.getBirthyearFrom() != null) return " · ab Jg. " + c.getBirthyearFrom();
            if (c.getBirthyearTo() != null) return " · bis Jg. " + c.getBirthyearTo();
            return "";
        }
    }

    // --- Hilfsmethoden -----------------------------------------------------

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private void info(String title, String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.initOwner(window());
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
