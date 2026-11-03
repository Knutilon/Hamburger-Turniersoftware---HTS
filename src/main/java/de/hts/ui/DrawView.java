package de.hts.ui;

import de.hts.tournament.Entrant;
import de.hts.tournament.GroupDraw;
import de.hts.tournament.KnockoutBracket;
import de.hts.tournament.KnockoutDraw;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Unterreiter „Gruppenübersicht“ bzw. „Turnierbaum“ einer Klasse: Modus festlegen (ab 2 Teilnehmenden bzw.
 * 2 vollständigen Doppeln), auslosen / neu auslosen / verwerfen, die Klasse als bereit markieren oder starten.
 * Zeigt die Gruppen mit ihren Spielen bzw. den KO-Baum – jeweils mit Spielstatus und Ergebnissen.
 */
public final class DrawView {

    private static final double MATCH_HEIGHT = 78;
    /** Kartenhöhe im aktuellen KO-Baum (Doppel höher: je Person eine Zeile, darunter die Sätze) */
    private double cardH = 96;
    private static final double MATCH_GAP = 14;
    private static final double MATCH_WIDTH = 330;
    private static final double COLUMN_GAP = 48;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'");

    private final Tournament tournament;
    private final Consumer<String> status;
    private final Runnable onChanged;
    private final Consumer<PlayingClass> onDiscard;
    private final Random random = new SecureRandom();

    private final VBox root = new VBox(12);
    private final Label warning = new Label();
    private final ComboBox<TournamentMode> mode = new ComboBox<>();
    private final CheckBox seeded = new CheckBox("nach Setzung (TTR)");
    private final Button drawBtn = new Button("Neu auslosen");
    private final Button discardBtn = new Button("Auslosung verwerfen", Icons.of(Icons.BACK, 14));
    private final Button readyBtn = new Button("bereit");
    private final Button startBtn = new Button("starten");
    private final Label startedLabel = new Label();

    private final ToggleGroup groupMode = new ToggleGroup();
    private final RadioButton bySize = new RadioButton("Gruppengröße:");
    private final RadioButton byCount = new RadioButton("Anzahl Gruppen:");
    private final Spinner<Integer> sizeSpinner = new Spinner<>(2, 64, 4);
    private final Spinner<Integer> countSpinner = new Spinner<>(1, 64, 2);
    private final Label groupPreview = new Label();
    private final HBox groupRow;
    private final ToggleGroup tiebreakGroup = new ToggleGroup();
    private final RadioButton tbSets = new RadioButton("Satz- und Ballverhältnis");
    private final RadioButton tbH2h = new RadioButton("direkter Vergleich");
    private final HBox phaseRow;
    private final Label followLabel = new Label();
    private final Button followBtn = new Button("Phase abschließen", Icons.of(Icons.START, 14));
    private final HBox followRow;
    private final Consumer<String> onOpenClass;
    private final VBox controls;
    private final HBox modeRow;
    private final Button pauseBtn = new Button("Phase anhalten");
    private final Button settingsBtn = new Button("Einstellungen ändern");
    private final Label settingsSummary = new Label();
    /** IDs mit Vereinsgleichheit in der aktuellen Anzeige */
    private java.util.Set<String> clashIds = java.util.Set.of();
    private final Button printBtn = new Button("PDF / Drucken");
    private final Spinner<Integer> roundsSpinner = new Spinner<>(1, 99, 3);
    private final Label swissPreview = new Label();
    private final HBox swissRow;
    private final Label tbLabel = new Label("Bei gleichem Spielverhältnis:");
    private final Button finishBtn = new Button("Klasse beenden");
    private final VBox overviewBox = new VBox(12);
    /** Gruppenphase: Suche nach Teilnehmenden (Vor- und/oder Nachname). */
    private final javafx.scene.control.TextField groupSearch = new javafx.scene.control.TextField();
    private final HBox searchRow;
    /** ausgeklappte Spielelisten (Klasse + Gruppe), Gruppen mit mehr als {@link #COLLAPSE_LIMIT} Spielen sind sonst eingeklappt */
    private final java.util.Set<String> expandedGroups = new java.util.HashSet<>();
    private static final int COLLAPSE_LIMIT = 7;
    /** Spiele, deren Satzergebnisse gerade ausgeklappt sind */
    private final java.util.Set<String> openSets = new java.util.HashSet<>();
    private final Label allGroupsLabel = new Label("alle Gruppen");
    /** Treffer der Suche in der gerade gebauten Gruppe (Namen hervorheben) */
    private java.util.Set<String> searchHits = java.util.Set.of();

    private PlayingClass current;
    private boolean updating;
    /** Ansicht mit Gruppen bzw. KO-Baum (für automatisches Scrollen, fixierte Rundentitel, Sprung zum Treffer) */
    private ScrollPane bracketScroll;
    /** KO-Baum: hält die Rundentitel oben (nach dem Scrollen aufrufen), sonst {@code null} */
    private Runnable sticky;
    /** Suchtreffer der aktuellen Anzeige in Reihenfolge (Gruppe: Namen, KO: Spielkarten) */
    private final List<Node> hitNodes = new java.util.ArrayList<>();
    private int hitIndex = -1;
    private String lastQuery = "";
    private final Label searchInfo = new Label();
    private final Button thirdBtn = new Button("Spiel um Platz 3");
    private final Button nextPhaseBtn = new Button();
    private HBox topRow;
    private javafx.scene.layout.FlowPane actionRow;

    /**
     * @param onDiscard nach „Auslosung verwerfen“: zurück zu „Teilnehmende der Klasse“
     */
    public DrawView(Tournament tournament, Consumer<String> status, Runnable onChanged, Consumer<PlayingClass> onDiscard,
                    Consumer<String> onOpenClass) {
        this.tournament = tournament;
        this.onOpenClass = onOpenClass;
        this.status = status;
        this.onChanged = onChanged;
        this.onDiscard = onDiscard;

        // „Gruppenphase mit Platzierungsspielen“ gibt es nur als Folgephase; Schweizer System nur in Phase 1
        mode.getItems().setAll(TournamentMode.NONE, TournamentMode.KNOCKOUT, TournamentMode.GROUPS, TournamentMode.SWISS);
        mode.valueProperty().addListener((o, a, b) -> onModeChanged(a, b));
        seeded.setTooltip(new Tooltip("KO: Nr. 1 und Nr. 2 treffen sich frühestens im Finale, Nr. 1/4 und 2/3 frühestens im Halbfinale.\n"
                + "Gruppen: Gruppenköpfe nach TTR, danach Schlangensystem.\nOhne Setzung: komplett zufällig."));
        drawBtn.getStyleClass().addAll("navy-pill", "compact");
        drawBtn.setOnAction(e -> draw());
        discardBtn.getStyleClass().add("pill");
        discardBtn.setOnAction(e -> discard());
        discardBtn.setTooltip(new Tooltip("Auslosung löschen und zurück zu „Teilnehmende der Klasse“"));
        readyBtn.getStyleClass().add("pill");
        readyBtn.setOnAction(e -> toggleReady());
        readyBtn.setTooltip(new Tooltip("Bereite Klassen werden mit „Turnier starten“ gestartet"));
        startBtn.getStyleClass().addAll("navy-pill", "compact");
        startBtn.setOnAction(e -> startClass());
        startBtn.setTooltip(new Tooltip("Nur diese Klasse starten: Spiele werden eingereiht und an freie Tische verteilt"));
        startedLabel.getStyleClass().add("started-label");

        bySize.setToggleGroup(groupMode);
        byCount.setToggleGroup(groupMode);
        sizeSpinner.setEditable(true);
        countSpinner.setEditable(true);
        sizeSpinner.setPrefWidth(80);
        countSpinner.setPrefWidth(80);
        groupMode.selectedToggleProperty().addListener((o, a, b) -> onGroupSettingsChanged());
        sizeSpinner.valueProperty().addListener((o, a, b) -> onGroupSettingsChanged());
        countSpinner.valueProperty().addListener((o, a, b) -> onGroupSettingsChanged());
        groupPreview.getStyleClass().add("criteria");
        groupRow = new HBox(10, bySize, sizeSpinner, new Label("  oder  "), byCount, countSpinner, groupPreview);
        groupRow.setAlignment(Pos.CENTER_LEFT);
        groupRow.managedProperty().bind(groupRow.visibleProperty());

        // Platzierung bei gleichem Spielverhältnis + Folgephase
        tbSets.setToggleGroup(tiebreakGroup);
        tbH2h.setToggleGroup(tiebreakGroup);
        tiebreakGroup.selectedToggleProperty().addListener((o, a, b) -> onPhaseSettingsChanged());
        tbLabel.getStyleClass().add("class-name");
        for (Node n : new Node[]{tbLabel, tbSets, tbH2h}) n.managedProperty().bind(n.visibleProperty());
        printBtn.getStyleClass().add("pill");
        printBtn.setTooltip(new Tooltip("PDF dieser Phase anzeigen (speichern/drucken): je Gruppe eine A4-Seite mit Tabelle und Spielplan"));
        printBtn.setOnAction(e -> {
            if (current == null) return;
            Reports.showPhase(root.getScene() == null ? null : root.getScene().getWindow(), tournament, current, status);
        });
        printBtn.managedProperty().bind(printBtn.visibleProperty());
        roundsSpinner.setEditable(true);
        roundsSpinner.setPrefWidth(80);
        roundsSpinner.valueProperty().addListener((o, a, b) -> {
            PlayingClass c = current;
            if (updating || c == null || b == null) return;
            c.setSwissRounds(b);
            tournament.markModified();
            updateSwissPreview(c);
        });
        swissPreview.getStyleClass().add("criteria");
        Label rl = new Label("Anzahl Runden:");
        rl.getStyleClass().add("class-name");
        swissRow = new HBox(10, rl, roundsSpinner, swissPreview);
        swissRow.setAlignment(Pos.CENTER_LEFT);
        swissRow.managedProperty().bind(swissRow.visibleProperty());
        pauseBtn.getStyleClass().add("pill");
        pauseBtn.setOnAction(e -> togglePause());
        pauseBtn.setTooltip(new Tooltip("Angehalten: keine neuen Spiele dieser Phase an die Tische – laufende Spiele werden noch "
                + "beendet. Freie Tische gehen an andere Klassen."));
        pauseBtn.managedProperty().bind(pauseBtn.visibleProperty());
        finishBtn.getStyleClass().add("pill");
        finishBtn.setVisible(false);
        finishBtn.managedProperty().bind(finishBtn.visibleProperty());
        followBtn.managedProperty().bind(followBtn.visibleProperty());
        phaseRow = new HBox(10, tbLabel, tbSets, tbH2h);
        phaseRow.setAlignment(Pos.CENTER_LEFT);
        phaseRow.managedProperty().bind(phaseRow.visibleProperty());
        followBtn.getStyleClass().add("navy-pill");
        followBtn.setOnAction(e -> closePhase());
        followLabel.getStyleClass().add("criteria");
        followLabel.setWrapText(true);
        followLabel.setMinWidth(0);
        followRow = new HBox(12, followLabel);
        followRow.setAlignment(Pos.CENTER_LEFT);
        followRow.getStyleClass().add("lock-banner");
        followRow.managedProperty().bind(followRow.visibleProperty());

        Label modeLabel = new Label("Modus:");
        modeLabel.getStyleClass().add("class-name");
        modeRow = new HBox(12, modeLabel, mode);
        modeRow.managedProperty().bind(modeRow.visibleProperty());
        for (Node n : new Node[]{seeded, drawBtn, discardBtn}) n.managedProperty().bind(n.visibleProperty());
        modeRow.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        settingsBtn.getStyleClass().add("pill");
        settingsBtn.setOnAction(e -> openModeDialog());
        settingsBtn.setTooltip(new Tooltip("Modus und Optionen für Phase 1 ändern und neu auslosen"));
        settingsBtn.managedProperty().bind(settingsBtn.visibleProperty());
        settingsSummary.getStyleClass().add("criteria");
        settingsSummary.setWrapText(true);
        // Knöpfe immer ausgeschrieben: links Auslosung, rechts Ablauf – bei wenig Platz bricht die Zeile um
        settingsBtn.setVisible(false);
        HBox leftActions = new HBox(10, drawBtn, discardBtn);
        leftActions.setAlignment(Pos.CENTER_LEFT);
        allGroupsLabel.getStyleClass().add("hint");
        allGroupsLabel.managedProperty().bind(allGroupsLabel.visibleProperty());
        thirdBtn.getStyleClass().addAll("pill", "switch-pill");
        thirdBtn.managedProperty().bind(thirdBtn.visibleProperty());
        thirdBtn.setOnAction(e -> toggleThirdPlace());
        nextPhaseBtn.getStyleClass().add("pill");
        nextPhaseBtn.managedProperty().bind(nextPhaseBtn.visibleProperty());
        nextPhaseBtn.setVisible(false);
        nextPhaseBtn.setOnAction(e -> {
            if (current != null && current.getFollowUpClass() != null) onOpenClass.accept(current.getFollowUpClass());
        });
        HBox rightActions = new HBox(10, startedLabel, allGroupsLabel, thirdBtn, printBtn, pauseBtn, followBtn, finishBtn,
                nextPhaseBtn, readyBtn, startBtn);
        rightActions.setAlignment(Pos.CENTER_LEFT);
        rightActions.setAlignment(Pos.CENTER_LEFT);
        actionRow = new javafx.scene.layout.FlowPane(24, 8, leftActions, rightActions);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setRowValignment(javafx.geometry.VPos.CENTER);
        settingsSummary.managedProperty().bind(settingsSummary.visibleProperty());
        settingsSummary.setAlignment(Pos.CENTER_RIGHT);
        settingsSummary.setMaxWidth(Double.MAX_VALUE);
        settingsSummary.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        // Suche in der Gruppenphase
        groupSearch.setPromptText("Suchen: Vorname, Nachname, Verein");
        groupSearch.setPrefColumnCount(24);
        groupSearch.textProperty().addListener((o, a, b) -> {
            if (!updating && current != null) show(current);
        });
        Button clearSearch = new Button("✕");
        clearSearch.getStyleClass().add("pill");
        clearSearch.setTooltip(new Tooltip("Suche zurücksetzen"));
        clearSearch.setOnAction(e -> groupSearch.clear());
        clearSearch.visibleProperty().bind(groupSearch.textProperty().isNotEmpty());
        groupSearch.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                stepHit(e.isShiftDown() ? -1 : 1);       // Enter = nächster, Umschalt+Enter = vorheriger Treffer
                e.consume();
            }
        });
        groupSearch.setTooltip(new Tooltip("Vorname, Nachname und/oder Verein – mehrere Wörter müssen alle passen.\n"
                + "Enter: nächster Treffer · Umschalt+Enter: vorheriger Treffer"));
        searchInfo.getStyleClass().add("hint");
        searchInfo.managedProperty().bind(searchInfo.visibleProperty());
        searchRow = new HBox(8, groupSearch, clearSearch, searchInfo);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.managedProperty().bind(searchRow.visibleProperty());
        // links: Spielverhältnis + Suche; rechts daneben: Startinfo/Knöpfe und darunter die Infozeile
        VBox leftCol = new VBox(10, followRow, phaseRow, searchRow);
        VBox rightCol = new VBox(6, actionRow, settingsSummary);
        rightCol.setAlignment(Pos.TOP_RIGHT);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        topRow = new HBox(24, leftCol, rightCol);
        // gelber Hinweis nur so breit wie nötig – höchstens bis an die Knöpfe heran, sonst Zeilenumbruch
        topRow.widthProperty().addListener((o, a, b) -> updateBannerWidth());
        topRow.setAlignment(Pos.TOP_LEFT);
        controls = new VBox(10, modeRow, groupRow, swissRow, topRow);

        warning.getStyleClass().add("warning");
        warning.setWrapText(true);
        warning.managedProperty().bind(warning.visibleProperty());

        ScrollPane scroll = new ScrollPane(overviewBox);
        bracketScroll = scroll;
        scroll.vvalueProperty().addListener((o, a, b) -> runSticky());
        scroll.viewportBoundsProperty().addListener((o, a, b) -> runSticky());
        overviewBox.heightProperty().addListener((o, a, b) -> runSticky());
        scroll.getStyleClass().add("bracket-scroll");
        scroll.setFitToWidth(false);
        overviewBox.setPadding(new Insets(4));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(controls, warning, scroll);
        installAutoScroll(scroll);
        root.setPadding(new Insets(10, 0, 0, 0));
    }

    /** Beim Ziehen (Drag &amp; Drop) nahe am oberen/unteren bzw. linken/rechten Rand automatisch weiterblättern. */
    private static void installAutoScroll(ScrollPane sp) {
        final double edge = 48, maxStep = 22;
        final double[] speed = new double[2];      // [0] waagerecht, [1] senkrecht (Pixel je Bild)
        final long[] last = new long[1];
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if (System.currentTimeMillis() - last[0] > 2500 || (speed[0] == 0 && speed[1] == 0)) {
                    if (System.currentTimeMillis() - last[0] > 2500) stop();
                    return;
                }
                Node content = sp.getContent();
                if (content == null) return;
                javafx.geometry.Bounds cb = content.getLayoutBounds(), vb = sp.getViewportBounds();
                double dh = cb.getHeight() - vb.getHeight(), dw = cb.getWidth() - vb.getWidth();
                if (speed[1] != 0 && dh > 0) sp.setVvalue(clamp(sp.getVvalue() + speed[1] / dh * (sp.getVmax() - sp.getVmin())));
                if (speed[0] != 0 && dw > 0) sp.setHvalue(clamp(sp.getHvalue() + speed[0] / dw * (sp.getHmax() - sp.getHmin())));
            }
        };
        sp.addEventFilter(javafx.scene.input.DragEvent.DRAG_OVER, e -> {
            javafx.geometry.Point2D p = sp.sceneToLocal(e.getSceneX(), e.getSceneY());
            double h = sp.getHeight(), w = sp.getWidth();
            speed[1] = p.getY() < edge ? -maxStep * (edge - p.getY()) / edge
                    : p.getY() > h - edge ? maxStep * (p.getY() - (h - edge)) / edge : 0;
            speed[0] = p.getX() < edge ? -maxStep * (edge - p.getX()) / edge
                    : p.getX() > w - edge ? maxStep * (p.getX() - (w - edge)) / edge : 0;
            last[0] = System.currentTimeMillis();
            timer.start();
        });
        javafx.event.EventHandler<javafx.scene.input.DragEvent> stop = e -> {
            speed[0] = 0;
            speed[1] = 0;
            timer.stop();
        };
        sp.addEventFilter(javafx.scene.input.DragEvent.DRAG_DROPPED, stop);
        sp.addEventFilter(javafx.scene.input.DragEvent.DRAG_DONE, stop);
        sp.addEventHandler(javafx.scene.input.DragEvent.DRAG_EXITED, e -> {
            if (e.getTarget() == sp) stop.handle(e);
        });
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    public Node getRoot() {
        return root;
    }

    /** Reitertitel, z. B. „Phase 1 – Gruppenübersicht“ oder „Phase 2 – KO-System“. */
    public static String phaseTabTitle(int number, PlayingClass c) {
        return "Phase " + number + " – " + tabTitle(c);
    }

    private static String nextLabel(TournamentMode m) {
        if (m == null || m == TournamentMode.NONE) return "– noch offen –";
        return switch (m) {
            case KNOCKOUT -> "KO-System";
            case GROUPS -> "weitere Gruppenphase";
            case PLACEMENT -> "Gruppenphase mit Platzierungsspielen";
            default -> m.getLabel();
        };
    }

    /** Name des Unterreiters je nach Modus. */
    public static String tabTitle(PlayingClass c) {
        if (c == null) return "Auslosung";
        return switch (c.getMode()) {
            case GROUPS -> "Gruppenübersicht";
            case KNOCKOUT -> "KO-System";
            case PLACEMENT -> "Platzierungsspiele";
            case SWISS -> "Schweizer System";
            case QUALIFICATION -> "Qualifikation";
            case NONE -> "Auslosung";
        };
    }

    // --- Anzeige -----------------------------------------------------------

    public void show(PlayingClass c) {
        if (c != current && groupSearch.getText() != null && !groupSearch.getText().isEmpty()) {
            updating = true;
            groupSearch.clear();      // andere Klasse: Suche zurücksetzen
        }
        current = c;
        updating = true;
        sticky = null;
        hitNodes.clear();
        try {
            overviewBox.getChildren().clear();
            warning.setVisible(false);
            if (c == null) {
                controls.setDisable(true);
                mode.setValue(null);
                groupRow.setVisible(false);
                return;
            }
            List<Entrant> entrants = tournament.entrantsOf(c);
            boolean phase = c.isPhase();
            boolean canChoose = !phase && tournament.canChooseMode(c);
            boolean started = c.isStarted();
            PlayingClass first = tournament.rootOf(c);
            controls.setDisable(false);
            // Modus und Optionen kommen aus „Modus festlegen“ – hier nur Zusammenfassung, neu auslosen, verwerfen
            modeRow.setVisible(false);
            seeded.setVisible(false);
            boolean editable = !phase && c.isDrawn() && !started;
            settingsBtn.setVisible(false);
            // Folgephase: angehalten + vorherige Phase abgeschlossen -> neu auslosen möglich
            boolean redrawPhase = phase && c.isPaused() && c.getFollowUpClass() == null && !first.isFinished()
                    && tournament.findClass(c.getSourceClass()).map(tournament::isPhaseComplete).orElse(false);
            drawBtn.setVisible(editable || redrawPhase);

            discardBtn.setVisible(editable);
            String open = openText(c);
            settingsSummary.setText(c.getMode() == TournamentMode.GROUPS && c.isDrawn()
                    ? c.getGroups().size() + " Gruppen" + open
                    : summary(c) + open);
            searchRow.setVisible(c.isDrawn() && (c.getMode() == TournamentMode.GROUPS
                    || (c.getMode() == TournamentMode.KNOCKOUT && !c.getDraw().isEmpty())));
            clashIds = c.isDrawn() ? tournament.clubClashes(c) : java.util.Set.of();
            mode.setDisable(!canChoose || started);
            mode.setValue(c.getMode());
            TournamentMode m = c.getMode();
            boolean hasMode = m != TournamentMode.NONE;
            groupRow.setVisible(false);
            groupRow.setDisable(started);
            (c.isGroupsBySize() ? bySize : byCount).setSelected(true);
            sizeSpinner.getValueFactory().setValue(c.getGroupSize());
            countSpinner.getValueFactory().setValue(c.getGroupCount());
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) sizeSpinner.getValueFactory()).setMax(Math.max(2, entrants.size()));
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) countSpinner.getValueFactory()).setMax(Math.max(1, entrants.size() / 2));
            sizeSpinner.setDisable(!c.isGroupsBySize());
            countSpinner.setDisable(c.isGroupsBySize());
            boolean swiss = m == TournamentMode.SWISS;
            swissRow.setVisible(false);
            swissRow.setDisable(started);
            int n = swiss && c.isDrawn() ? c.getGroups().get(0).size() : entrants.size();
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) roundsSpinner.getValueFactory())
                    .setMax(de.hts.tournament.SwissSystem.maxRounds(Math.max(2, n)));
            roundsSpinner.getValueFactory().setValue(tournament.swissRounds(c));
            updateSwissPreview(c);
            phaseRow.setVisible(m == TournamentMode.GROUPS);
            printBtn.setVisible(c.isDrawn());
            allGroupsLabel.setVisible(c.isDrawn() && m == TournamentMode.GROUPS);
            (c.getTiebreak() == de.hts.tournament.GroupStanding.Tiebreak.HEAD_TO_HEAD ? tbH2h : tbSets).setSelected(true);
            boolean fixed = c.getFollowUpClass() != null || first.isFinished();
            tbSets.setDisable(fixed);
            tbH2h.setDisable(fixed);
            updateFollowRow(c);

            if (c.isDrawn()) seeded.setSelected(c.isDrawSeeded());
            seeded.setDisable(!hasMode || !canChoose || started);
            drawBtn.setDisable(!hasMode || !canChoose || started);
            drawBtn.setText("Neu auslosen");
            drawBtn.setTooltip(new Tooltip("Mit denselben Einstellungen neu auslosen"));
            if (!clashIds.isEmpty()) {
                showWarning("⚠ Vereinsgleichheit: " + clashIds.size() / 2 + (c.getMode() == TournamentMode.KNOCKOUT
                        ? " Erstrundenpaarung(en)" : " Paar(e) in derselben Gruppe") + " – rot markiert. "
                        + (started ? "" : "Per Drag & Drop lässt sich das vor dem Start ändern."));
            }
            discardBtn.setDisable(!c.isDrawn() || started);
            updateGroupPreview(c);

            readyBtn.setVisible(c.isDrawn() && !started);
            readyBtn.setManaged(readyBtn.isVisible());
            readyBtn.setText(c.isReady() ? "✓ bereit" : "bereit");
            readyBtn.setTooltip(new Tooltip(c.isReady() ? "Als bereit markiert – klicken hebt das auf"
                    : "Als bereit markieren: wird mit „Turnier starten“ gestartet"));
            startBtn.setVisible(c.isDrawn() && !started);
            startBtn.setManaged(startBtn.isVisible());
            startedLabel.setText(started ? "Gestartet " + c.getStartedAt().format(TIME) + (c.isPaused() ? " · angehalten" : "") : "");
            boolean complete = tournament.isPhaseComplete(c);
            pauseBtn.setVisible(started && !complete && !first.isFinished());
            pauseBtn.setText(c.isPaused() ? "Phase fortsetzen" : "Phase anhalten");
            pauseBtn.getStyleClass().removeAll("navy-pill", "pill");
            pauseBtn.getStyleClass().add(c.isPaused() ? "navy-pill" : "pill");
            pauseBtn.setMinWidth(Region.USE_PREF_SIZE);
            boolean thirdPossible = m == TournamentMode.KNOCKOUT && c.isDrawn() && (tournament.canHaveThirdPlace(c) || c.isThirdPlace());
            thirdBtn.setVisible(thirdPossible);
            if (thirdPossible) {
                thirdBtn.setGraphic(switchGraphic(c.isThirdPlace()));
                thirdBtn.setDisable(first.isFinished());
                thirdBtn.setTooltip(new Tooltip((c.isThirdPlace() ? "Spiel um Platz 3 ist an" : "Spiel um Platz 3 ist aus")
                        + " – klicken zum Umschalten (Gewinner 3., Verlierer 4.)"));
            }

            if (!canChoose && !c.isDrawn()) {
                showWarning(c.isDoubles()
                        ? "Ein Modus kann festgelegt werden, sobald mindestens 2 vollständige Doppel in der Klasse sind."
                        : "Ein Modus kann festgelegt werden, sobald mindestens 2 Teilnehmende in der Klasse sind.");
            } else if (tournament.isDrawOutdated(c) && !started) {
                showWarning("Die Teilnehmenden der Klasse haben sich seit der Auslosung geändert – bitte neu auslosen.");
            }

            if (m == TournamentMode.PLACEMENT) {
                overviewBox.getChildren().add(buildPlacement(c));
            } else if (m == TournamentMode.QUALIFICATION) {
                overviewBox.getChildren().add(buildQualification(c));
            } else if (swiss && c.isDrawn()) {
                overviewBox.getChildren().add(buildSwiss(c));
            } else if (!c.getGroups().isEmpty()) {
                overviewBox.getChildren().add(buildGroups(c));
            } else if (!c.getDraw().isEmpty()) {
                overviewBox.getChildren().add(buildBracket(c));
            } else if (hasMode && canChoose) {
                Label l = new Label(m == TournamentMode.SWISS
                        ? "Noch nicht ausgelost. „Auslosen“ erstellt die 1. Runde – jede weitere Runde wird nach der Tabelle gepaart, "
                        + "sobald die vorige komplett gespielt ist."
                        : "Noch nicht ausgelost. „Auslosen“ erstellt die " + tabTitle(c) + " mit allen Spielen.");
                l.getStyleClass().add("hint");
                overviewBox.getChildren().add(l);
            }
        } finally {
            updating = false;
        }
        afterShow();
    }

    // --- Suche: Treffer zählen, Enter springt weiter -------------------------------

    private String searchQuery() {
        return groupSearch.getText() == null ? "" : groupSearch.getText().trim();
    }

    private void afterShow() {
        String q = searchQuery();
        boolean changed = !q.equals(lastQuery);
        lastQuery = q;
        if (changed) hitIndex = -1;
        if (hitIndex >= hitNodes.size()) hitIndex = -1;
        if (hitIndex >= 0) hitNodes.get(hitIndex).getStyleClass().add("search-current");
        updateSearchInfo();
        updateBannerWidth();
        javafx.application.Platform.runLater(() -> {
            updateBannerWidth();
            runSticky();
            if (changed && !q.isEmpty() && !hitNodes.isEmpty()) scrollTo(hitNodes.get(0));   // zum ersten Treffer
        });
    }

    private void updateSearchInfo() {
        String q = searchQuery();
        searchInfo.setVisible(!q.isEmpty() && searchRow.isVisible());
        int n = hitNodes.size();
        searchInfo.setText(n == 0 ? "keine Treffer" : hitIndex < 0 ? n + " Treffer · Enter: nächster"
                : "Treffer " + (hitIndex + 1) + " von " + n);
    }

    /** Zum nächsten (+1) bzw. vorherigen (-1) Treffer springen – nach dem letzten wieder der erste. */
    private void stepHit(int dir) {
        if (hitNodes.isEmpty()) return;
        if (hitIndex >= 0) hitNodes.get(hitIndex).getStyleClass().remove("search-current");
        hitIndex = hitIndex < 0 ? (dir > 0 ? 0 : hitNodes.size() - 1) : Math.floorMod(hitIndex + dir, hitNodes.size());
        Node n = hitNodes.get(hitIndex);
        n.getStyleClass().add("search-current");
        scrollTo(n);
        updateSearchInfo();
    }

    /** Knoten in den sichtbaren Bereich der Ansicht scrollen (unter die fixierten Rundentitel). */
    private void scrollTo(Node n) {
        if (n.getScene() == null || bracketScroll == null) return;
        javafx.geometry.Bounds b = overviewBox.sceneToLocal(n.localToScene(n.getBoundsInLocal()));
        javafx.geometry.Bounds vb = bracketScroll.getViewportBounds();
        double ch = overviewBox.getHeight(), cw = overviewBox.getWidth();
        double margin = sticky != null ? 60 : 30;
        if (ch > vb.getHeight()) bracketScroll.setVvalue(clamp((b.getMinY() - margin) / (ch - vb.getHeight())));
        if (cw > vb.getWidth()) {
            boolean visible = b.getMinX() >= bracketScroll.getHvalue() * (cw - vb.getWidth())
                    && b.getMaxX() <= bracketScroll.getHvalue() * (cw - vb.getWidth()) + vb.getWidth();
            if (!visible) bracketScroll.setHvalue(clamp((b.getMinX() - 30) / (cw - vb.getWidth())));
        }
        runSticky();
    }

    private void runSticky() {
        if (sticky != null) sticky.run();
    }

    /** Gelber Hinweis höchstens bis an die Knöpfe rechts heran (sonst Zeilenumbruch). */
    private void updateBannerWidth() {
        if (topRow == null || actionRow == null) return;
        double avail = topRow.getWidth() - actionRow.prefWidth(-1) - 24 - 8;
        double max = Math.max(260, avail);
        followRow.setMaxWidth(max);
        // Höhe für den Zeilenumbruch selbst bestimmen (die Elternlayouts fragen die Höhe ohne Breite ab)
        javafx.geometry.Insets in = followRow.getInsets();
        double labelW = Math.min(max, followRow.prefWidth(-1)) - in.getLeft() - in.getRight();
        if (labelW > 0) followRow.setMinHeight(followLabel.prefHeight(labelW) + in.getTop() + in.getBottom());
    }

    /** „ · 3 Spiele offen“ bzw. „ · keine Spiele offen“ für die Infozeile. */
    private String openText(PlayingClass c) {
        if (c == null || !c.isDrawn()) return "";
        List<Match> ms = tournament.matchesOf(c);
        long done = ms.stream().filter(x -> x.getStatus() == MatchStatus.DONE).count();
        long open;
        if (c.getMode() == TournamentMode.KNOCKOUT && !c.getDraw().isEmpty()) {
            long total = new KnockoutBracket(tournament.drawSlots(c)).playedMatches() + (tournament.thirdPlaceMatch(c) != null ? 1 : 0);
            open = Math.max(0, total - done);
        } else {
            open = ms.size() - done;
        }
        String prefix = c.getMode() == TournamentMode.SWISS && c.isStarted()
                ? " · Runde " + tournament.swissRound(c) + " von " + tournament.swissRounds(c) : "";
        return prefix + " · " + (open == 0 ? "keine Spiele offen" : Ui.spiele(open) + " offen");
    }

    /** kleiner An/Aus-Schalter als Grafik im Knopf „Spiel um Platz 3“ */
    private static Node switchGraphic(boolean on) {
        javafx.scene.layout.StackPane track = new javafx.scene.layout.StackPane();
        track.getStyleClass().addAll("mini-switch", on ? "on" : "off");
        javafx.scene.shape.Circle knob = new javafx.scene.shape.Circle(5, javafx.scene.paint.Color.WHITE);
        track.getChildren().add(knob);
        javafx.scene.layout.StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        track.setPadding(new Insets(0, 2, 0, 2));
        return track;
    }

    private void toggleThirdPlace() {
        PlayingClass c = current;
        if (c == null) return;
        boolean on = !c.isThirdPlace();
        try {
            tournament.setThirdPlace(c, on);
            status.accept(on ? "Spiel um Platz 3 aktiviert." : "Spiel um Platz 3 entfernt.");
        } catch (IllegalStateException ex) {
            status.accept(ex.getMessage());
            showWarning(ex.getMessage());
        }
        onChanged.run();
    }

    private void updateFollowRow(PlayingClass c) {
        followBtn.setVisible(false);
        finishBtn.setVisible(false);
        nextPhaseBtn.setVisible(false);
        PlayingClass first = tournament.rootOf(c);
        int n = tournament.phaseNumber(c);
        boolean complete = tournament.isPhaseComplete(c);
        if (c.getFollowUpClass() != null) {
            PlayingClass next = tournament.findClass(c.getFollowUpClass()).orElse(null);
            followLabel.setText("Phase " + n + " ist abgeschlossen – weiter in Phase " + (n + 1)
                    + (next != null ? " (" + tabTitle(next) + ")" : "") + ".");
            nextPhaseBtn.setText("Zu Phase " + (n + 1));
            nextPhaseBtn.setVisible(true);
            followRow.setVisible(true);
        } else if (first.isFinished() && complete) {
            followLabel.setText("Phase " + n + " ist abgeschlossen, die Klasse beendet (" + first.getFinishedAt().format(TIME)
                    + ") – Platzierungen im Reiter „Endergebnis“.");
            followRow.setVisible(true);
        } else if (c.isPaused()) {
            // angehalten: Hinweis im gelben Kasten oben links
            followLabel.setText("Phase " + n + " ist angehalten: Es werden keine neuen Spiele an die Tische gegeben. "
                    + "Laufende Spiele können noch eingetragen werden. „Phase fortsetzen“ reiht die Spiele wieder ein.");
            followRow.setVisible(true);
            if (c.isStarted() && complete) followBtn.setVisible(true);
        } else if (c.isStarted() && complete) {
            followBtn.setVisible(true);          // „Phase abschließen“ rechts bei den Knöpfen
            followBtn.setDisable(false);
            followRow.setVisible(false);
        } else {
            followRow.setVisible(false);         // offene Spiele stehen in der Infozeile
        }
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private void onPhaseSettingsChanged() {
        PlayingClass c = current;
        if (updating || c == null) return;
        c.setTiebreak(tbH2h.isSelected() ? de.hts.tournament.GroupStanding.Tiebreak.HEAD_TO_HEAD
                : de.hts.tournament.GroupStanding.Tiebreak.SETS_POINTS);
        tournament.markModified();
        show(c);
    }

    /** Phase abschließen: Endergebnis (Klasse beenden) oder neue Phase. */
    private void closePhase() {
        PlayingClass c = current;
        if (c == null) return;
        PhaseCloseDialog.Choice choice = new PhaseCloseDialog(window(), tournament, c).showAndWait().orElse(null);
        if (choice == null) return;
        try {
            if (choice.finish()) {
                tournament.finishClass(c);
                status.accept("„" + tournament.rootOf(c).getName() + "“ beendet – Endergebnis steht fest.");
                onChanged.run();
                onOpenClass.accept(RESULT_TAB);
            } else {
                PlayingClass next = choice.kind() == TournamentMode.QUALIFICATION
                        ? tournament.createQualificationPhase(c, choice.qualifiers(), choice.koPerGroup(), choice.koRounds(), choice.reserve(), random)
                        : tournament.createNextPhase(c, choice.kind(), choice.qualifiers(), choice.groupSize(), random);
                status.accept("Phase " + tournament.phaseNumber(next) + " (" + tabTitle(next) + ") erstellt, nach Platzierung ausgelost"
                        + (next.isStarted() ? " und gestartet – die Spiele sind eingereiht." : "."));
                onChanged.run();
                onOpenClass.accept(next.getName());
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            showWarning(e.getMessage());
        }
    }

    /** Schlüssel für {@code onOpenClass}: zum Reiter „Endergebnis“ wechseln. */
    static final String RESULT_TAB = "#endergebnis";

    private void togglePause() {
        PlayingClass c = current;
        if (c == null) return;
        if (c.isPaused()) {
            tournament.resumeClass(c);
            status.accept("Phase " + tournament.phaseNumber(c) + " von „" + tournament.rootOf(c).getName() + "“ fortgesetzt.");
        } else {
            tournament.pauseClass(c);
            status.accept("Phase " + tournament.phaseNumber(c) + " von „" + tournament.rootOf(c).getName() + "“ angehalten.");
        }
        onChanged.run();
    }

    private void showWarning(String text) {
        warning.setText(text);
        warning.setVisible(true);
    }

    private void updateGroupPreview(PlayingClass c) {
        int n = tournament.entrantsOf(c).size();
        if (c.getMode() != TournamentMode.GROUPS || n < 2) {
            groupPreview.setText("");
            return;
        }
        int groups;
        try {
            groups = tournament.plannedGroupCount(c);
        } catch (IllegalArgumentException e) {
            groupPreview.setText(e.getMessage());
            return;
        }
        String problem = GroupDraw.validate(n, groups);
        if (problem != null) {
            groupPreview.setText("⚠ " + problem);
            drawBtn.setDisable(true);
            return;
        }
        int big = n % groups == 0 ? groups : n % groups;
        int bigSize = GroupDraw.maxGroupSize(n, groups);
        int small = groups - big;
        String sizes = n % groups == 0
                ? groups + " Gruppen à " + bigSize
                : big + " × " + bigSize + " und " + small + " × " + (bigSize - 1);
        int games = big * bigSize * (bigSize - 1) / 2 + (n % groups == 0 ? 0 : small * (bigSize - 1) * (bigSize - 2) / 2);
        groupPreview.setText("→ " + sizes + " · " + games + " Gruppenspiele");
    }

    /** Je Gruppe: links die Gruppe (Kopf zuerst), rechts daneben ihre Spiele mit Status. */
    private Node buildGroups(PlayingClass c) {
        List<List<Entrant>> groups = tournament.groupsOf(c);
        List<Match> all = tournament.matchesOf(c);
        VBox list = new VBox(14);
        String q = searchQuery();
        int hits = 0;
        for (int g = 0; g < groups.size(); g++) {
            List<Entrant> members = groups.get(g);
            int gi = g;
            List<Match> groupMatches = all.stream().filter(m -> m.getGroupIndex() == gi).toList();
            List<Match> allOfGroup = groupMatches;
            java.util.Set<String> found = new java.util.HashSet<>();
            if (!q.isEmpty()) {
                for (Entrant e : members) if (nameMatches(e, q)) found.add(e.id());
                if (found.isEmpty()) continue;            // Suche: nur Gruppen mit Treffern
                hits += found.size();
                groupMatches = groupMatches.stream().filter(m -> found.contains(m.getA()) || found.contains(m.getB())).toList();
            }
            // aktiv/pausiert → eingereiht → geplant → abgeschlossen, je nach Spielnummer
            groupMatches = groupMatches.stream().sorted(java.util.Comparator.comparingInt((Match m) -> statusRank(m.getStatus()))
                    .thenComparingInt(Match::getNumber)).toList();
            String key = c.getName() + "#" + g;
            boolean collapsible = q.isEmpty() && groupMatches.size() > COLLAPSE_LIMIT;
            boolean expanded = !collapsible || expandedGroups.contains(key);
            Node matchesNode;
            if (!collapsible) {
                matchesNode = groupMatchList(groupMatches, found);
            } else {
                List<Match> shown = expanded ? groupMatches : groupMatches.subList(0, COLLAPSE_LIMIT);
                VBox list0 = (VBox) groupMatchList(shown, found);
                list0.getChildren().add(collapseToggle(key, groupMatches, expanded));
                matchesNode = list0;
            }
            searchHits = found;
            HBox row = new HBox(14, groupCard("Gruppe " + GroupDraw.groupName(g), g, members, allOfGroup), matchesNode);
            searchHits = java.util.Set.of();
            row.setAlignment(Pos.TOP_LEFT);
            list.getChildren().add(row);
        }
        if (!q.isEmpty() && hits == 0) {
            Label res = new Label("Keine Teilnehmenden gefunden für „" + q + "“.");
            res.getStyleClass().add("warning");
            return new VBox(12, res, list);
        }
        return list;
    }

    private void updateSwissPreview(PlayingClass c) {
        if (c == null || c.getMode() != TournamentMode.SWISS) {
            swissPreview.setText("");
            return;
        }
        int n = c.isDrawn() ? c.getGroups().get(0).size() : tournament.entrantsOf(c).size();
        if (n < 2) {
            swissPreview.setText("");
            return;
        }
        int r = tournament.swissRounds(c);
        swissPreview.setText("→ " + n + " Teilnehmende · " + r + " Runden à " + (n / 2) + " Spiele"
                + (n % 2 == 1 ? " + 1 Freilos" : "") + " · Vorschlag: " + de.hts.tournament.SwissSystem.defaultRounds(n)
                + ", höchstens " + de.hts.tournament.SwissSystem.maxRounds(n));
    }

    /** Schweizer System: Tabelle mit Buchholz/Feinbuchholz, daneben die Spiele aller bisherigen Runden. */
    private Node buildSwiss(PlayingClass c) {
        List<Match> all = tournament.matchesOf(c);
        List<Entrant> members = tournament.groupsOf(c).get(0);
        int round = tournament.swissRound(c);
        HBox row = new HBox(14, groupCard("Tabelle", 0, members, all), groupMatchList(all));
        row.setAlignment(Pos.TOP_LEFT);
        Label summary = new Label("Runde " + round + " von " + tournament.swissRounds(c) + " · " + all.size() + " Spiele · ausgelost "
                + (c.isDrawSeeded() ? "nach Setzung (1. Runde: obere gegen untere Hälfte)" : "zufällig")
                + " · Wertung: Punkte (Siege + Freilos), Buchholz, Feinbuchholz, Satz- und Balldifferenz");
        summary.getStyleClass().add("hint");
        summary.setWrapText(true);
        VBox box = new VBox(12, summary, row);
        List<String> byes = c.getSwissByes();
        for (int i = 0; i < byes.size(); i++) {
            if (byes.get(i) == null) continue;
            String id = byes.get(i);
            String name = members.stream().filter(e -> e.id().equals(id)).map(Entrant::name).findFirst().orElse("?");
            Label l = new Label("Freilos Runde " + (i + 1) + ": " + name + " (zählt als Sieg)");
            l.getStyleClass().add("hint");
            box.getChildren().add(l);
        }
        return box;
    }

    /** Platzierungsspiele: Endgruppe (Platz 1–4) mit ihren Spielen, darunter die Spiele um Platz 5, 7, … */
    private Node buildPlacement(PlayingClass c) {
        List<List<Entrant>> groups = tournament.groupsOf(c);
        List<Match> all = tournament.matchesOf(c);
        List<Match> finalMatches = all.stream().filter(m -> m.getGroupIndex() == 0).toList();
        List<Match> placement = all.stream().filter(m -> m.getPlacement() > 0).toList();
        HBox finalRow = new HBox(14, groupCard("Endgruppe – Platz 1 bis 4", 0, groups.isEmpty() ? List.of() : groups.get(0), finalMatches),
                groupMatchList(finalMatches));
        Label rule = new Label("Endgruppe: Wertung nach Spielverhältnis, dann direktem Vergleich, dann Satz- und Ballverhältnis. "
                + "Das Spiel aus Phase 1 (Erster gegen Zweiter derselben Gruppe) ist übernommen.");
        rule.getStyleClass().add("hint");
        rule.setWrapText(true);
        Label ph = new Label("Platzierungsspiele");
        ph.getStyleClass().add("round-title");
        VBox box = new VBox(12, rule, finalRow, ph);
        if (placement.isEmpty()) {
            Label none = new Label("Keine weiteren Platzierungsspiele.");
            none.getStyleClass().add("hint");
            box.getChildren().add(none);
        } else {
            box.getChildren().add(groupMatchList(placement));
        }
        java.util.Set<String> inMatch = new java.util.HashSet<>();
        placement.forEach(m -> {
            inMatch.add(m.getA());
            inMatch.add(m.getB());
        });
        if (groups.size() > 1) {
            for (Tournament.Placing p : tournament.finalResult(c).rows()) {
                if (p.info().startsWith("ohne Gegner")) {
                    Label l = new Label("Platz " + p.from() + ": " + p.entrant().name() + " – ohne Gegner (ungleich große Gruppen), kampflos");
                    l.getStyleClass().add("hint");
                    box.getChildren().add(l);
                }
            }
        }
        return box;
    }

    /** Gruppentabelle: Platz, Name, Spiele, Siege:Niederlagen, Sätze, Bälle; Aufsteiger hervorgehoben. */
    private Node groupCard(String title, int g, List<Entrant> members, List<Match> groupMatches) {
        Label head = new Label(title);
        head.getStyleClass().add("round-title");
        Label games = new Label(Ui.spiele(groupMatches.size()));
        games.getStyleClass().add("hint");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox cardHead = new HBox(8, head, sp, games);
        cardHead.setAlignment(Pos.CENTER_LEFT);
        if (current.getMode() == TournamentMode.GROUPS && current.isDrawn()) {
            Button print = new Button("Drucken");
            print.getStyleClass().add("pill");
            print.setTooltip(new Tooltip("Nur diese Gruppe als PDF anzeigen (speichern/drucken): Tabelle und Gruppenspiele, aktueller Stand"));
            PlayingClass phase = current;
            print.setOnAction(e -> Reports.showGroup(window(), tournament, phase, g, status));
            cardHead.getChildren().add(print);
        }
        VBox card = new VBox(5, cardHead);
        javafx.scene.layout.GridPane table = new javafx.scene.layout.GridPane();
        table.setHgap(8);
        table.setVgap(3);
        boolean swissCard = current.getMode() == TournamentMode.SWISS;
        String[] heads = swissCard ? new String[]{"Pl.", "Name", "Verein", "TTR", "Sp", "Pkt", "S:N", "Sätze", "Bälle", "BHZ", "FBHZ"}
                : new String[]{"Pl.", "Name", "Verein", "TTR", "Sp", "S:N", "Sätze", "Bälle"};
        for (int k = 0; k < heads.length; k++) {
            Label h = new Label(heads[k]);
            h.getStyleClass().add("table-head");
            table.add(h, k, 0);
        }
        List<de.hts.tournament.GroupStanding.Row> rows = tournament.standings(current, g);
        boolean showQual = current.getNextPhase() != TournamentMode.NONE
                && (current.getMode() == TournamentMode.GROUPS || current.getMode() == TournamentMode.SWISS);
        int qualCount = current.getNextPhase() == TournamentMode.PLACEMENT ? 2 : current.getQualifiers();
        int r = 1;
        for (de.hts.tournament.GroupStanding.Row row : rows) {
            boolean qualified = showQual && row.place() <= qualCount && row.played() > 0;
            Label pl = new Label(String.valueOf(row.place()));
            pl.getStyleClass().add(row.place() == 1 && row.played() > 0 ? "seed" : "pos");
            pl.setMinWidth(22);
            Label name = new Label(row.entrant().name());
            name.getStyleClass().add("match-name");
            name.setMaxWidth(150);
            boolean head1 = members.indexOf(row.entrant()) == 0 || (!members.isEmpty() && members.get(0).id().equals(row.entrant().id()));
            if (searchHits.contains(row.entrant().id())) {
                name.getStyleClass().add("search-hit");
                hitNodes.add(name);
            }
            boolean clash = clashIds.contains(row.entrant().id()) && current.getMode() != TournamentMode.SWISS;
            if (clash) name.getStyleClass().add("club-clash");
            name.setTooltip(new Tooltip(row.entrant().name() + (row.entrant().detail() != null ? "\n" + row.entrant().detail() : "")
                    + (row.entrant().ttr() != null ? "\nTTR " + row.entrant().ttr() : "") + (head1 ? "\nGruppenkopf" : "")
                    + (clash ? "\n⚠ Vereinsgleichheit in dieser Gruppe" : "")
                    + (groupEditable() ? "\nZiehen: auf ein Mitglied = tauschen, auf eine Gruppe = verschieben" : "")));
            if (groupEditable()) installMemberDnD(name, row.entrant().id());
            Label club = new Label(row.entrant().detail() == null ? "" : row.entrant().detail());
            club.getStyleClass().add("hint");
            club.setMaxWidth(140);
            if (row.entrant().detail() != null) club.setTooltip(new Tooltip(row.entrant().detail()));
            Label ttr = new Label(row.entrant().ttr() == null ? "–" : String.valueOf(row.entrant().ttr()));
            ttr.getStyleClass().addAll("table-num", "hint");
            ttr.setMinWidth(Region.USE_PREF_SIZE);
            Label played = new Label(String.valueOf(row.played()));
            Label wl = new Label(row.won() + ":" + row.lost());
            Label sets = new Label(row.setsWon() + ":" + row.setsLost());
            Label pts = new Label(row.pointsWon() + ":" + row.pointsLost());
            for (Label l : new Label[]{played, wl, sets, pts}) l.getStyleClass().add("table-num");
            wl.getStyleClass().add("match-name");
            Node[] cells;
            if (swissCard) {
                Label score = new Label(String.valueOf(row.score()));
                score.getStyleClass().addAll("table-num", "match-name");
                if (row.byes() > 0) score.setTooltip(new Tooltip("inkl. " + row.byes() + " Freilos"));
                Label bh = new Label(String.valueOf(row.buchholz()));
                bh.setTooltip(new Tooltip("Buchholz: Summe der Punkte aller Gegner"));
                Label fbh = new Label(String.valueOf(row.fineBuchholz()));
                fbh.setTooltip(new Tooltip("Feinbuchholz: Summe der Buchholzzahlen aller Gegner"));
                bh.getStyleClass().add("table-num");
                fbh.getStyleClass().add("table-num");
                cells = new Node[]{pl, name, club, ttr, played, score, wl, sets, pts, bh, fbh};
            } else {
                cells = new Node[]{pl, name, club, ttr, played, wl, sets, pts};
            }
            table.addRow(r, cells);
            if (qualified) {
                for (Node n : cells) n.getStyleClass().add("qualified");
            }
            r++;
        }
        card.getChildren().add(table);
        if (showQual) {
            Label q = new Label(current.getNextPhase() == TournamentMode.PLACEMENT ? "Grün = spielt in der Endgruppe um Platz 1–4"
                    : "Grün = zieht in Phase " + (tournament.phaseNumber(current) + 1) + " (" + nextLabel(current.getNextPhase()) + ") ein");
            q.getStyleClass().add("hint");
            card.getChildren().add(q);
        }
        card.getStyleClass().add("group-card");
        if (groupEditable()) installGroupDrop(card, g);
        if (current.getMode() == TournamentMode.GROUPS && members.stream().anyMatch(e -> clashIds.contains(e.id()))) {
            Label chip = new Label("⚠ Vereinsgleichheit");
            chip.getStyleClass().addAll("chip", "chip-clash");
            card.getChildren().add(1, chip);
        }
        card.setPrefWidth(swissCard ? 700 : 570);
        card.setMinWidth(swissCard ? 700 : 570);
        return card;
    }

    /** Ein-/Ausklappen: Spiele 8 bis Ende einer Gruppe. */
    private Node collapseToggle(String key, List<Match> groupMatches, boolean expanded) {
        int hidden = groupMatches.size() - COLLAPSE_LIMIT;
        long done = groupMatches.stream().filter(m -> m.getStatus() == MatchStatus.DONE).count();
        Button b = new Button(expanded ? "▴ Spiele 8–" + groupMatches.size() + " einklappen"
                : "▾ " + hidden + " weitere Spiele anzeigen (8–" + groupMatches.size() + ")");
        b.getStyleClass().add("pill");
        b.setOnAction(e -> {
            if (expanded) expandedGroups.remove(key);
            else expandedGroups.add(key);
            show(current);
        });
        Label info = new Label(groupMatches.size() + " Spiele · " + done + " gespielt");
        info.getStyleClass().add("hint");
        HBox box = new HBox(10, b, info);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(2, 0, 0, 4));
        return box;
    }

    private static int statusRank(MatchStatus s) {
        return switch (s) {
            case ACTIVE, PAUSED -> 0;
            case QUEUED -> 1;
            case PLANNED -> 2;
            case DONE -> 3;
        };
    }

    /** Suche: jedes Wort muss im Namen vorkommen (Vor- und/oder Nachname, Groß-/Kleinschreibung egal). */
    private boolean nameMatches(Entrant e, String q) {
        return tournament.entrantMatches(e.id(), q);   // Vorname, Nachname, Verein
    }

    private Node groupMatchList(List<Match> groupMatches) {
        return groupMatchList(groupMatches, java.util.Set.of());
    }

    private Node groupMatchList(List<Match> groupMatches, java.util.Set<String> highlight) {
        VBox box = new VBox(4);
        box.getStyleClass().add("group-matches");
        box.setMinWidth(680);
        box.setPrefWidth(720);
        for (Match m : groupMatches) {
            Label nr = new Label(String.valueOf(m.getNumber()));
            nr.getStyleClass().add("pos");
            nr.setMinWidth(30);
            Label rd = new Label(m.isCarried() ? "übern." : m.getPlacement() > 0 ? "Pl. " + m.getPlacement() + "/" + (m.getPlacement() + 1) : "R" + m.getRound());
            if (m.isCarried()) rd.setTooltip(new Tooltip("Ergebnis aus Phase 1 übernommen"));
            rd.getStyleClass().add("hint");
            rd.setMinWidth(m.getPlacement() > 0 || m.isCarried() ? 52 : 28);
            Label a = new Label(tournament.entrantName(m, m.getA()));
            Label vs = new Label("–");
            vs.getStyleClass().add("hint");
            Label b = new Label(tournament.entrantName(m, m.getB()));
            a.getStyleClass().add("match-name");
            b.getStyleClass().add("match-name");
            markWinner(m, a, b);
            if (highlight.contains(m.getA())) a.getStyleClass().add("search-hit");
            if (highlight.contains(m.getB())) b.getStyleClass().add("search-hit");
            a.setPrefWidth(170);
            b.setPrefWidth(170);
            a.setMinWidth(140);
            b.setMinWidth(140);
            Region s = new Region();
            HBox.setHgrow(s, Priority.ALWAYS);
            Label result = new Label(m.getResultText());
            result.getStyleClass().add("result");
            result.setMinWidth(36);
            HBox row = new HBox(8, nr, rd, a, vs, b, s, tableInfo(m), result, statusChip(m.getStatus()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("group-match-row");
            installResultEntry(row, m);
            if (m.getStatus() == MatchStatus.DONE && !m.getSetsText().isEmpty()) {
                // Klick auf das Ergebnis: Satzergebnisse darunter ausklappen; weiterer Klick auf die Zeile: einklappen
                String st = m.getSetsText();
                Label details = new Label(st.startsWith("Aufgabe") || st.startsWith("kampflos") ? st : "Sätze: " + st.replace(", ", "  ·  "));
                details.getStyleClass().add("set-details");
                details.managedProperty().bind(details.visibleProperty());
                details.setVisible(openSets.contains(m.getId()));
                result.getStyleClass().add("clickable-result");
                Tooltip.install(row, new Tooltip("Klicken: Satzergebnisse auf-/zuklappen · Doppelklick: Ergebnis korrigieren"));
                VBox entry = new VBox(0, row, details);
                entry.getStyleClass().add("group-match-entry");
                // Klick auf die Zeile: Satzergebnisse auf-/zuklappen (Doppelklick = Ergebnis korrigieren)
                row.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                    if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
                    if (e.getClickCount() == 2) {           // der erste Klick des Doppelklicks hat schon umgeschaltet
                        details.setVisible(!details.isVisible());
                        if (details.isVisible()) openSets.add(m.getId()); else openSets.remove(m.getId());
                        return;
                    }
                    if (e.getClickCount() != 1) return;
                    details.setVisible(!details.isVisible());
                    if (details.isVisible()) openSets.add(m.getId()); else openSets.remove(m.getId());
                });
                row.getStyleClass().add("toggle-row");
                box.getChildren().add(entry);
            } else {
                if (!m.getSetsText().isEmpty()) Tooltip.install(row, new Tooltip(m.getSetsText()));
                box.getChildren().add(row);
            }
        }
        return box;
    }

    static Label statusChip(MatchStatus s) {
        Label l = new Label(s.getLabel());
        l.getStyleClass().addAll("chip", "status-" + s.name().toLowerCase());
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private Label tableInfo(Match m) {
        String name = m.getStatus().atTable() && m.getTable() != null ? tournament.tableName(m.getTable()) : "";
        Label l = new Label(name);
        l.getStyleClass().addAll("hint", "table-info");
        // lange Tischnamen an engen Stellen gekürzt („…“), voller Name im Tooltip
        l.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        l.setMaxWidth(130);
        l.setMinWidth(Math.min(58, l.prefWidth(-1)));
        if (name.length() > 14) l.setTooltip(new Tooltip(name));
        if (m.getStatus().atTable()) l.setPrefWidth(Math.min(130, Math.max(58, l.prefWidth(-1))));
        return l;
    }

    private static void markWinner(Match m, Label a, Label b) {
        String w = m.winner();
        if (w == null) return;
        (w.equals(m.getA()) ? a : b).getStyleClass().add("winner");
        (w.equals(m.getA()) ? b : a).getStyleClass().add("loser");
    }

    /** KO-Baum: eine Spalte je Runde; Spiele mit Status, Tisch und Ergebnis; Sieger rücken weiter. */
    private Node buildBracket(PlayingClass c) {
        List<Entrant> slots = tournament.drawSlots(c);
        KnockoutBracket bracket = new KnockoutBracket(slots);
        boolean dbl = slots.stream().anyMatch(x -> x != null && x.id().startsWith("pair:"));
        cardH = dbl ? 136 : 96;
        Map<String, String> seedOf = new HashMap<>();
        if (!c.getSeedLabels().isEmpty()) {
            seedOf.putAll(c.getSeedLabels());     // KO ab Phase 2: Gruppe + Platz, z. B. „A1“
        } else if (c.isDrawSeeded()) {
            int[] order = KnockoutDraw.seedOrder(slots.size());
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i) != null) seedOf.put(slots.get(i).id(), String.valueOf(order[i]));
            }
        }
        Map<String, Match> byPos = new HashMap<>();
        for (Match m : tournament.matchesOf(c)) byPos.put(m.getKoRound() + ":" + m.getKoIndex(), m);

        // Suche: Partien der Treffer hervorheben (samt Pfad), alle anderen abblenden
        String q = searchQuery();
        boolean searching = !q.isEmpty();
        java.util.function.Predicate<String> hit = id -> id != null && tournament.entrantMatches(id, q);
        boolean[][] hitAt = new boolean[bracket.getRounds().size()][];

        // feste Positionen: Spalte r bei x = r·(Breite+Abstand), Karte k mittig in ihrem Block der Höhe unit_r
        final double gap = COLUMN_GAP, top = 34;
        javafx.scene.layout.Pane tree = new javafx.scene.layout.Pane();
        tree.getStyleClass().add("bracket-tree");
        List<KnockoutBracket.Round> rounds = bracket.getRounds();
        double height = 0;
        double finalTop = 0;
        List<Node> lines = new java.util.ArrayList<>();
        List<Node> hitLines = new java.util.ArrayList<>();
        List<Node> cards = new java.util.ArrayList<>();
        // Rundentitel in eigener Leiste (weißer Hintergrund) – bleibt beim senkrechten Scrollen oben stehen
        javafx.scene.layout.Pane titleBar = new javafx.scene.layout.Pane();
        titleBar.getStyleClass().add("bracket-titlebar");
        for (int r = 0; r < rounds.size(); r++) {
            KnockoutBracket.Round round = rounds.get(r);
            hitAt[r] = new boolean[round.matches().size()];
            for (int i = 0; i < round.matches().size(); i++) {
                KnockoutBracket.Match bm = round.matches().get(i);
                Match m = byPos.get(r + ":" + i);
                hitAt[r][i] = searching && (hit.test(bm.a().entrant() == null ? null : bm.a().entrant().id())
                        || hit.test(bm.b().entrant() == null ? null : bm.b().entrant().id())
                        || (m != null && (hit.test(m.getA()) || hit.test(m.getB()))));
            }
        }
        for (int r = 0; r < rounds.size(); r++) {
            KnockoutBracket.Round round = rounds.get(r);
            double x = r * (MATCH_WIDTH + gap);
            double unit = (cardH + MATCH_GAP) * Math.pow(2, r);
            Label roundTitle = new Label(round.name());
            roundTitle.getStyleClass().add("round-title");
            roundTitle.relocate(x, 4);
            titleBar.getChildren().add(roundTitle);
            for (int i = 0; i < round.matches().size(); i++) {
                double cy = top + unit * i + unit / 2;
                Node card = matchCard(round.matches().get(i), byPos.get(r + ":" + i), seedOf, r, i);
                card.relocate(x, cy - cardH / 2);
                if (searching) {
                    card.getStyleClass().add(hitAt[r][i] ? "search-hit-card" : "search-dim");
                    if (hitAt[r][i]) hitNodes.add(card);
                }
                cards.add(card);
                if (r + 1 < rounds.size()) {
                    // Verbindung zum Folgespiel: waagerecht, senkrecht, waagerecht (Pfad eines Treffers kräftig)
                    double nUnit = unit * 2;
                    double ny = top + nUnit * (i / 2) + nUnit / 2;
                    double x1 = x + MATCH_WIDTH, xm = x1 + gap * 0.3, x2 = x + MATCH_WIDTH + gap;
                    boolean path = searching && hitAt[r][i] && hitAt[r + 1][i / 2];
                    String style = path ? "bracket-path-hit" : "bracket-connector";
                    List<Node> target = path ? hitLines : lines;
                    target.add(bracketLine(x1, cy, xm, cy, style));
                    target.add(bracketLine(xm, cy, xm, ny, style));
                    target.add(bracketLine(xm, ny, x2, ny, style));
                }
                height = Math.max(height, cy + cardH / 2);
                if (r == rounds.size() - 1) finalTop = cy - cardH / 2;
            }
        }
        Match thirdMatch = tournament.thirdPlaceMatch(c);
        if (thirdMatch != null && !rounds.isEmpty()) {
            // unter der Spalte „Finale“: Titel „Spiel um Platz 3“, darunter die Karte (Kopf „Spiel n“) – ohne Verbindungslinie
            double x = (rounds.size() - 1) * (MATCH_WIDTH + gap);
            double ty = finalTop + cardH + 28;
            Label t3 = new Label(Tournament.THIRD_PLACE_STAGE);
            t3.getStyleClass().add("round-title");
            t3.relocate(x, ty);
            Node card = thirdPlaceCard(thirdMatch, seedOf, c);
            card.relocate(x, ty + 26);
            if (searching) {
                boolean h = hit.test(thirdMatch.getA()) || hit.test(thirdMatch.getB());
                card.getStyleClass().add(h ? "search-hit-card" : "search-dim");
                if (h) hitNodes.add(card);
            }
            cards.add(t3);
            cards.add(card);
            height = Math.max(height, ty + 26 + cardH + (thirdMatch.getStatus() == MatchStatus.DONE ? 22 : 0));
        }
        // leichte, gestrichelte Trennlinien zwischen den Runden
        for (int r = 0; r + 1 < rounds.size(); r++) {
            double sx = r * (MATCH_WIDTH + gap) + MATCH_WIDTH + gap * 0.72;
            lines.add(bracketLine(sx, 0, sx, height + 6, "bracket-separator"));
        }
        double width = rounds.size() * (MATCH_WIDTH + gap) - gap;
        titleBar.setPrefSize(width + gap, top - 4);
        titleBar.setMinSize(width + gap, top - 4);
        titleBar.setMaxSize(width + gap, top - 4);
        titleBar.relocate(0, 0);
        tree.getChildren().addAll(lines);
        tree.getChildren().addAll(hitLines);
        tree.getChildren().addAll(cards);
        tree.getChildren().add(titleBar);
        tree.setPrefSize(width, height + 8);
        tree.setMinSize(width, height + 8);
        final double treeH = height;
        sticky = () -> {
            // Rundentitel oben fixieren: Leiste um die verdeckte Höhe nach unten schieben
            if (tree.getScene() == null) return;
            javafx.geometry.Bounds vb = bracketScroll.getViewportBounds();
            double contentH = overviewBox.getHeight();
            double visibleTop = contentH > vb.getHeight() ? bracketScroll.getVvalue() * (contentH - vb.getHeight()) : 0;
            javafx.geometry.Point2D p = overviewBox.sceneToLocal(tree.localToScene(0, 0));
            if (p == null) return;
            double off = Math.max(0, Math.min(visibleTop - p.getY(), treeH - top));
            titleBar.setTranslateY(off);
        };
        VBox box = new VBox(12);
        box.getChildren().add(tree);
        return box;
    }

    private static javafx.scene.shape.Line bracketLine(double x1, double y1, double x2, double y2, String style) {
        javafx.scene.shape.Line l = new javafx.scene.shape.Line(x1, y1, x2, y2);
        l.getStyleClass().add(style);
        switch (style) {
            case "bracket-separator" -> {       // Trennlinie zwischen den Runden: hellgrau, gestrichelt
                l.setStroke(javafx.scene.paint.Color.web("#dde1e5"));
                l.setStrokeWidth(1);
                l.getStrokeDashArray().setAll(3.0, 5.0);
            }
            case "bracket-path-hit" -> {        // Pfad eines Suchtreffers
                l.setStroke(javafx.scene.paint.Color.web("#00a5a8"));
                l.setStrokeWidth(2.4);
            }
            default -> {                        // Turnierpfad: durchgezogen, dünn, grau
                l.setStroke(javafx.scene.paint.Color.web("#9aa1a9"));
                l.setStrokeWidth(1.3);
            }
        }
        l.setManaged(false);
        l.setMouseTransparent(true);
        return l;
    }

    /** Linker Kartenrand nach Status: kommend grau, läuft türkis, pausiert gelb, beendet schwarz. */
    static String statusStyle(MatchStatus s) {
        if (s == null) return "card-upcoming";
        return switch (s) {
            case PLANNED, QUEUED -> "card-upcoming";
            case ACTIVE -> "card-running";
            case PAUSED -> "card-paused";
            case DONE -> "card-done";
        };
    }

    /**
     * Qualifikationsmodus: direkt Qualifizierte, die Runden der KO-Qualifikation (Spieler per Drag &amp; Drop tauschbar)
     * und die Stufen „qualifiziert“ / „Nachrücker Rang x“.
     */
    private Node buildQualification(PlayingClass c) {
        VBox box = new VBox(12);
        Map<String, Entrant> idx = new HashMap<>();
        for (Entrant e : tournament.entrantsOf(c)) idx.put(e.id(), e);
        if (!c.getQualifiedDirect().isEmpty()) {
            Label head = new Label("Direkt qualifiziert (" + c.getQualifiedDirect().size() + ")");
            head.getStyleClass().add("round-title");
            javafx.scene.layout.FlowPane direct = new javafx.scene.layout.FlowPane(8, 6);
            for (String id : c.getQualifiedDirect()) {
                Entrant e = idx.get(id);
                if (e == null) continue;
                Label l = new Label(withClub(e));
                l.getStyleClass().addAll("chip", "qualified");
                direct.getChildren().add(l);
            }
            box.getChildren().addAll(head, direct);
        }
        int rounds = tournament.qualificationRound(c);
        if (rounds > 0) {
            int n = c.getQualRounds();
            Label info = new Label("KO-Qualifikation: " + c.getQualField().size() + " Spieler, die Gewinner spielen weiter – wer "
                    + n + (n == 1 ? " Spiel" : " Spiele") + " gewinnt, ist qualifiziert; die Verlierer werden Nachrücker "
                    + "(später ausgeschieden = weiter oben, je Stufe Gruppenplatz vor TTR)"
                    + (qualEditable() ? " · Spieler der aktuellen Runde per Drag & Drop tauschen" : ""));
            info.getStyleClass().add("hint");
            info.setWrapText(true);
            box.getChildren().add(info);
            CheckBox extra = new CheckBox("Zusatzrunde für die Verlierer der Runde " + n + " (bei Bedarf)");
            extra.setSelected(c.isQualExtra());
            extra.setDisable(tournament.rootOf(c).isFinished());
            extra.setTooltip(new Tooltip("Die Verlierer der letzten Runde spielen noch ein Spiel – die Sieger stehen dann als Nachrücker "
                    + "vor den Verlierern (z. B. für frei werdende Startplätze)."));
            extra.setOnAction(e -> {
                try {
                    tournament.setQualExtra(c, extra.isSelected());
                    status.accept(extra.isSelected() ? "Zusatzrunde zugeschaltet." : "Zusatzrunde entfernt.");
                } catch (IllegalStateException ex) {
                    status.accept(ex.getMessage());
                    showWarning(ex.getMessage());
                }
                onChanged.run();
            });
            box.getChildren().add(extra);
        }
        for (int r = 1; r <= rounds; r++) {
            final int round = r;
            Label title = new Label(r > c.getQualRounds() ? "Zusatzrunde (Verlierer der Runde " + c.getQualRounds() + ")"
                    : "Qualifikation Runde " + r);
            title.getStyleClass().add("round-title");
            javafx.scene.layout.FlowPane cards = new javafx.scene.layout.FlowPane(14, 10);
            boolean editable = r == rounds && qualEditable();
            for (Match m : tournament.matchesOf(c)) {
                if (m.getRound() != round) continue;
                cards.getChildren().add(qualCard(c, m, idx, editable));
            }
            List<String> byes = c.getSwissByes();
            String bye = r - 1 < byes.size() ? byes.get(r - 1) : null;
            if (bye != null && idx.containsKey(bye)) {
                Label head = new Label("Freilos");
                head.getStyleClass().add("match-head");
                Node row = qualRow(c, idx.get(bye), null, true, editable);
                VBox card = new VBox(3, head, row);
                card.getStyleClass().addAll("match-card", "match-bye");
                card.setPrefWidth(MATCH_WIDTH);
                cards.getChildren().add(card);
            }
            box.getChildren().addAll(title, cards);
        }
        List<Entrant> viaKo = tournament.qualified(c).stream().filter(e -> !c.getQualifiedDirect().contains(e.id())).toList();
        if (!viaKo.isEmpty()) box.getChildren().addAll(tierBlock("Über die KO qualifiziert", viaKo, true));
        List<List<Entrant>> tiers = tournament.qualificationTiers(c);
        for (int t = 0; t < tiers.size(); t++) {
            box.getChildren().addAll(tierBlock(tournament.qualificationTierLabel(c, t), tiers.get(t), false));
        }
        return box;
    }

    private List<Node> tierBlock(String label, List<Entrant> list, boolean qualified) {
        Label head = new Label(label + " (" + list.size() + ")");
        head.getStyleClass().add("round-title");
        javafx.scene.layout.FlowPane f = new javafx.scene.layout.FlowPane(8, 6);
        list.forEach(e -> {        // Reihenfolge wie in der Wertung (Gruppenplatz, dann TTR)
                    Label l = new Label(withClub(e));
                    l.getStyleClass().add("chip");
                    if (qualified) l.getStyleClass().add("qualified");
                    f.getChildren().add(l);
                });
        return List.of(head, f);
    }

    private Node qualCard(PlayingClass c, Match m, Map<String, Entrant> idx, boolean editable) {
        HBox headRow = new HBox(6);
        Label head = new Label("Spiel " + m.getNumber());
        head.getStyleClass().add("match-head");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        headRow.getChildren().addAll(head, sp, tableInfo(m), statusChip(m.getStatus()));
        headRow.setAlignment(Pos.CENTER_LEFT);
        boolean rowEditable = editable && (c.isPaused() || (m.getStatus() != MatchStatus.DONE && !m.getStatus().atTable()));
        VBox card = new VBox(3, headRow, qualRow(c, idx.get(m.getA()), m, true, rowEditable),
                qualRow(c, idx.get(m.getB()), m, false, rowEditable));
        card.getStyleClass().addAll("match-card", statusStyle(m.getStatus()));
        card.setPrefWidth(MATCH_WIDTH);
        if (!m.getSetsText().isEmpty()) Tooltip.install(card, new Tooltip(m.getSetsText()));
        installResultEntry(card, m);
        return card;
    }

    private Node qualRow(PlayingClass c, Entrant e, Match m, boolean isA, boolean editable) {
        if (e == null) return new Label("–");
        Integer g = c.getQualField().get(e.id());
        Label grp = new Label(g == null ? "" : GroupDraw.groupName(g));
        grp.getStyleClass().add("seed");
        grp.setMinWidth(22);
        Label name = new Label(withClub(e));
        name.getStyleClass().add("match-name");
        name.setMaxWidth(MATCH_WIDTH - 80);
        name.setTooltip(new Tooltip(e.name() + (e.detail() != null ? "\n" + e.detail() : "")
                + (e.ttr() != null ? "\nTTR " + e.ttr() : "") + (g != null ? "\nGruppe " + GroupDraw.groupName(g) : "")));
        Label right = new Label();
        right.getStyleClass().add("hint");
        if (m != null && m.getStatus() == MatchStatus.DONE) {
            right.setText(String.valueOf(isA ? m.getSetsA() : m.getSetsB()));
            right.getStyleClass().add("result");
            name.getStyleClass().add(e.id().equals(m.winner()) ? "winner" : "loser");
        } else if (e.ttr() != null) {
            right.setText(String.valueOf(e.ttr()));
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, grp, name, spacer, right);
        row.setAlignment(Pos.CENTER_LEFT);
        if (editable) {
            startDrag(row, DND_QUAL + e.id());
            dropTarget(row, DND_QUAL, DND_QUAL + e.id(), other -> {
                tournament.swapQualificationPlayers(current, other, e.id());
                status.accept("Spieler in der Qualifikationsrunde getauscht.");
            });
        }
        return row;
    }

    private boolean qualEditable() {
        return current != null && current.getMode() == TournamentMode.QUALIFICATION && !tournament.rootOf(current).isFinished();
    }

    /** Karte für das Spiel um Platz 3 (unter dem KO-Baum). */
    private Node thirdPlaceCard(Match m, Map<String, String> seedOf, PlayingClass c) {
        HBox headRow = new HBox(6);
        Label head = new Label("Spiel " + m.getNumber());
        head.getStyleClass().add("match-head");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        headRow.getChildren().addAll(head, sp, tableInfo(m), statusChip(m.getStatus()));
        headRow.setAlignment(Pos.CENTER_LEFT);
        Node rowA = sideRow(new KnockoutBracket.Side(null, false, tournament.thirdPlaceLabel(c, 0)), m.getA(), m, seedOf, true, -1);
        Node rowB = sideRow(new KnockoutBracket.Side(null, false, tournament.thirdPlaceLabel(c, 1)), m.getB(), m, seedOf, false, -1);
        rowB.getStyleClass().add("side-b");
        VBox card = new VBox(3, headRow, rowA, rowB);
        card.getStyleClass().addAll("match-card", statusStyle(m.getStatus()));
        if (m.getStatus() == MatchStatus.DONE && !m.getSetsText().isEmpty()) {
            Label sets = new Label("Sätze: " + m.getSetsText().replace(", ", " · "));
            sets.getStyleClass().add("card-sets");
            card.getChildren().add(sets);
        }
        card.setMinHeight(cardH);
        card.setPrefWidth(MATCH_WIDTH);
        card.setMaxWidth(MATCH_WIDTH);
        installResultEntry(card, m);
        return card;
    }

    /** „Name (Verein)“ – in allen KO-Darstellungen. */
    static String withClub(Entrant e) {
        return e.detail() == null || e.detail().isBlank() ? e.name() : e.name() + " (" + e.detail() + ")";
    }

    private Node matchCard(KnockoutBracket.Match bm, Match m, Map<String, String> seedOf, int round, int index) {
        boolean editable = round == 0 && koEditable();
        HBox headRow = new HBox(6);
        Label head = new Label(bm.isBye() ? "Freilos" : "Spiel " + (m != null ? m.getNumber() : bm.number()));
        head.getStyleClass().add("match-head");
        headRow.getChildren().add(head);
        if (m != null) {
            Region s = new Region();
            HBox.setHgrow(s, Priority.ALWAYS);
            headRow.getChildren().addAll(s, tableInfo(m), statusChip(m.getStatus()));
        }
        headRow.setAlignment(Pos.CENTER_LEFT);
        Node rowA = sideRow(bm.a(), m == null ? null : m.getA(), m, seedOf, true, round == 0 ? 2 * index : -1);
        Node rowB = sideRow(bm.b(), m == null ? null : m.getB(), m, seedOf, false, round == 0 ? 2 * index + 1 : -1);
        rowB.getStyleClass().add("side-b");
        VBox card = new VBox(3, headRow, rowA, rowB);
        card.getStyleClass().addAll("match-card", statusStyle(m == null ? null : m.getStatus()));
        if (m != null) {
            if (m.getStatus() == MatchStatus.DONE && !m.getSetsText().isEmpty()) {
                Label sets = new Label(m.getSetsText().startsWith("Aufgabe") || m.getSetsText().startsWith("kampflos")
                        ? m.getSetsText() : "Sätze: " + m.getSetsText().replace(", ", " · "));
                sets.getStyleClass().add("card-sets");
                card.getChildren().add(sets);
            }
            installResultEntry(card, m);
        }
        if (bm.isBye()) card.getStyleClass().add("match-bye");
        if (editable) installPairingDnD(card, head, index);
        card.setMinHeight(cardH);
        card.setPrefHeight(cardH);
        card.setMaxHeight(cardH);
        card.setPrefWidth(MATCH_WIDTH);
        card.setMinWidth(MATCH_WIDTH);
        return card;
    }

    /**
     * Eine Seite einer KO-Begegnung: Kennung als Chip, Namen ausgeschrieben („Nachname, Vorname“, Verein klein grau
     * dahinter), Doppel je Person eine Zeile; rechts Ergebnis bzw. TTR (nur Einzel).
     */
    private Node sideRow(KnockoutBracket.Side side, String knownId, Match m, Map<String, String> seedOf, boolean isA, int pos) {
        Entrant e = side.entrant();
        if (e == null && knownId != null && m != null) e = tournament.entrant(m, knownId);
        Label seed = new Label();
        seed.setMinWidth(26);
        VBox names = new VBox(1);
        names.setMinWidth(0);
        HBox.setHgrow(names, Priority.ALWAYS);
        Label right = new Label();
        right.getStyleClass().add("hint");
        right.setMinWidth(Region.USE_PREF_SIZE);
        List<Label> nameLabels = new java.util.ArrayList<>();
        if (e != null) {
            String s = seedOf.get(e.id());
            if (s != null) {
                seed.setText(s);
                seed.getStyleClass().add("seed");
                seed.setTooltip(new Tooltip(Character.isDigit(s.charAt(0)) ? "Setzplatz " + s
                        : "Gruppe " + s.replaceAll("\\d+$", "") + ", Platz " + s.replaceAll("^\\D+", "")));
            }
            List<String[]> persons = tournament.personLines(e.id());
            if (persons == null) persons = List.<String[]>of(new String[]{e.name(), e.detail()});
            for (String[] pl : persons) {
                Label n = new Label(pl[0]);
                n.getStyleClass().add("match-name");
                n.setMinWidth(Region.USE_PREF_SIZE);
                Label club = new Label(pl[1] == null ? "" : pl[1]);
                club.getStyleClass().add("card-club");
                club.setMinWidth(0);
                HBox line = new HBox(6, n, club);
                line.setAlignment(Pos.BASELINE_LEFT);
                line.setMinWidth(0);
                names.getChildren().add(line);
                nameLabels.add(n);
            }
            Tooltip.install(names, new Tooltip(String.join("\n", persons.stream().map(pl -> pl[0] + (pl[1] != null ? " (" + pl[1] + ")" : "")).toList())
                    + (e.ttr() != null && !e.id().startsWith("pair:") ? "\nTTR " + e.ttr() : "")));
            boolean dbl = e.id().startsWith("pair:");
            if (m != null && m.getStatus() == MatchStatus.DONE) {
                right.setText(String.valueOf(isA ? m.getSetsA() : m.getSetsB()));
                right.getStyleClass().add("result");
                String cls = e.id().equals(m.winner()) ? "winner" : "loser";
                nameLabels.forEach(l -> l.getStyleClass().add(cls));
            } else if (e.ttr() != null && !dbl) {
                right.setText(String.valueOf(e.ttr()));       // TTR nur bei Einzel
            }
        } else {
            Label ph = new Label(side.label());
            ph.getStyleClass().add(side.bye() ? "match-bye-label" : "match-placeholder");
            names.getChildren().add(ph);
        }
        HBox row = new HBox(8, seed, names, right);
        row.setAlignment(Pos.CENTER_LEFT);
        if (pos >= 0 && e != null && clashIds.contains(e.id())) {
            nameLabels.forEach(l -> l.getStyleClass().add("club-clash"));
            Tooltip.install(row, new Tooltip("⚠ Vereinsgleichheit mit dem Erstrundengegner"));
        }
        if (pos >= 0 && koEditable()) installSlotDnD(row, pos);
        return row;
    }

    // --- Drag & Drop: Auslosung vor dem Start von Hand ändern ---------------------

    private static final String DND_MEMBER = "hts-member:";
    private static final String DND_SLOT = "hts-slot:";
    private static final String DND_PAIR = "hts-pair:";
    private static final String DND_QUAL = "hts-qual:";

    private boolean groupEditable() {
        return current != null && !current.isStarted() && current.getMode() == TournamentMode.GROUPS;
    }

    /** KO: vor dem Start frei; während der Phase für Erstrundenspiele, die noch nicht laufen bzw. gespielt sind. */
    private boolean koEditable() {
        return current != null && (current.getMode() == TournamentMode.KNOCKOUT)
                && !tournament.rootOf(current).isFinished();
    }

    private static String dragged(javafx.scene.input.DragEvent e) {
        return e.getDragboard().hasString() ? e.getDragboard().getString() : null;
    }

    private static void startDrag(Node source, String payload) {
        source.setOnDragDetected(e -> {
            javafx.scene.input.Dragboard db = source.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(payload);
            db.setContent(cc);
            e.consume();
        });
        source.getStyleClass().add("draggable");
    }

    /** Ziel annehmen, wenn der Inhalt mit {@code prefix} beginnt und nicht {@code self} ist; {@code onDrop} bekommt den Rest. */
    private void dropTarget(Node target, String prefix, String self, java.util.function.Consumer<String> onDrop) {
        target.setOnDragOver(e -> {
            String s = dragged(e);
            if (s != null && s.startsWith(prefix) && !s.equals(self)) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                e.consume();
            }
        });
        target.setOnDragEntered(e -> {
            String s = dragged(e);
            if (s != null && s.startsWith(prefix) && !s.equals(self)) target.getStyleClass().add("drop-hover");
        });
        target.setOnDragExited(e -> target.getStyleClass().remove("drop-hover"));
        target.setOnDragDropped(e -> {
            String s = dragged(e);
            if (s == null || !s.startsWith(prefix) || s.equals(self)) return;
            e.setDropCompleted(true);
            e.consume();
            String arg = s.substring(prefix.length());
            javafx.application.Platform.runLater(() -> {
                try {
                    onDrop.accept(arg);
                    onChanged.run();
                } catch (IllegalStateException | IllegalArgumentException ex) {
                    status.accept(ex.getMessage());
                    showWarning(ex.getMessage());
                }
            });
        });
    }

    private void installMemberDnD(Label name, String id) {
        startDrag(name, DND_MEMBER + id);
        dropTarget(name, DND_MEMBER, DND_MEMBER + id, other -> {
            tournament.swapGroupMembers(current, other, id);
            status.accept("Gruppenmitglieder getauscht – Spiele neu angelegt.");
        });
    }

    private void installGroupDrop(Node card, int g) {
        dropTarget(card, DND_MEMBER, null, id -> {
            tournament.moveGroupMember(current, id, g);
            status.accept("In Gruppe " + GroupDraw.groupName(g) + " verschoben – Spiele neu angelegt.");
        });
    }

    private void installSlotDnD(Node row, int pos) {
        startDrag(row, DND_SLOT + pos);
        dropTarget(row, DND_SLOT, DND_SLOT + pos, other -> {
            tournament.swapKnockoutSlots(current, Integer.parseInt(other), pos);
            status.accept("Plätze im KO-Feld getauscht.");
        });
    }

    private void installPairingDnD(Node card, Node handle, int index) {
        startDrag(handle, DND_PAIR + index);
        if (handle instanceof Label l) l.setTooltip(new Tooltip("Hier ziehen: ganze Paarung verschieben (tauschen). "
                + "Einzelne Namen ziehen: Plätze tauschen."));
        dropTarget(card, DND_PAIR, DND_PAIR + index, other -> {
            tournament.swapKnockoutPairings(current, Integer.parseInt(other), index);
            status.accept("Paarungen getauscht.");
        });
    }

    /** Doppelklick: Ergebnis eintragen (aktiv/eingereiht) bzw. korrigieren (abgeschlossen). */
    private void installResultEntry(Node node, Match m) {
        if (!ResultEntry.isEditable(m)) return;
        node.getStyleClass().add("clickable");
        node.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Window w = root.getScene() == null ? null : root.getScene().getWindow();
                if (ResultEntry.open(w, tournament, m, status)) onChanged.run();
            }
        });
    }

    // --- Aktionen ----------------------------------------------------------

    private void onModeChanged(TournamentMode old, TournamentMode now) {
        PlayingClass c = current;
        if (updating || c == null || now == null || now == c.getMode()) return;
        if (c.isDrawn() && !confirm("Modus ändern", "Die bisherige Auslosung von „" + c.getName() + "“ wird dabei verworfen. Fortfahren?")) {
            updating = true;
            mode.setValue(old);
            updating = false;
            return;
        }
        if (c.isDrawn()) tournament.clearDraw(c);
        c.setMode(now);
        tournament.markModified();
        status.accept("Modus für „" + c.getName() + "“: " + now.getLabel());
        onChanged.run();
    }

    private void onGroupSettingsChanged() {
        PlayingClass c = current;
        if (updating || c == null) return;
        c.setGroupsBySize(bySize.isSelected());
        c.setGroupSize(sizeSpinner.getValue());
        c.setGroupCount(countSpinner.getValue());
        tournament.markModified();
        sizeSpinner.setDisable(!c.isGroupsBySize());
        countSpinner.setDisable(c.isGroupsBySize());
        drawBtn.setDisable(c.isStarted());
        updateGroupPreview(c);
    }

    /** Auslosen bzw. neu auslosen – bis zum Klassenstart jederzeit möglich, mit oder ohne Setzung. */
    private void draw() {
        PlayingClass c = current;
        if (c == null) return;
        boolean again = c.isDrawn();
        if (c.isPhase()) {
            long played = tournament.matchesOf(c).stream().filter(m -> m.getStatus() == MatchStatus.DONE && !m.isCarried()).count();
            if (!confirm("Phase neu auslosen", "Phase " + tournament.phaseNumber(c) + " neu auslosen (nach Platzierung der Vorphase)?"
                    + (played > 0 ? "\n" + played + " bereits eingetragene Ergebnisse dieser Phase gehen verloren." : "")
                    + "\nDie neue Auslosung bleibt angehalten – mit „Phase fortsetzen“ geht es weiter.")) return;
            try {
                PlayingClass next = tournament.redrawPhase(c, random);
                status.accept("Phase " + tournament.phaseNumber(next) + " neu ausgelost (angehalten).");
                onChanged.run();
                onOpenClass.accept(next.getName());
            } catch (IllegalStateException | IllegalArgumentException e) {
                showWarning(e.getMessage());
            }
            return;
        }
        try {
            boolean sd = !c.isDrawn() || c.isDrawSeeded();
            if (c.getMode() == TournamentMode.GROUPS) tournament.drawGroups(c, sd, random);
            else if (c.getMode() == TournamentMode.SWISS) tournament.drawSwiss(c, sd, random);
            else tournament.drawKnockout(c, sd, random);
            status.accept("„" + c.getName() + "“ " + (again ? "neu " : "") + "ausgelost (" + summary(c) + ").");
            onChanged.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            status.accept(e.getMessage());
            showWarning(e.getMessage());
        }
    }

    private void openModeDialog() {
        PlayingClass c = current;
        if (c == null) return;
        if (Boolean.TRUE.equals(new ModeDialog(window(), tournament, c, random).showAndWait().orElse(false))) {
            status.accept("„" + c.getName() + "“ ausgelost: " + summary(c) + ".");
            onChanged.run();
        }
    }

    /** z. B. „Gruppenphase · Gruppengröße 4 · Setzung (Stärkegruppen) · Vereinsgleichheit beachtet“. */
    String summary(PlayingClass c) {
        if (c == null || !c.isDrawn()) return "";
        StringBuilder sb = new StringBuilder(switch (c.getMode()) {
            case GROUPS -> "Gruppenphase · " + c.getGroups().size() + " Gruppen";
            case KNOCKOUT -> "KO-System · " + c.getDraw().size() + "er-Feld" + koCounts(c);
            case SWISS -> "Schweizer System · " + tournament.swissRounds(c) + " Runden";
            case PLACEMENT -> "Platzierungsspiele";
            case QUALIFICATION -> "Qualifikationsmodus · " + c.getQualDirect() + " direkt je Gruppe, " + c.getQualKo()
                    + " je Gruppe in der KO-Qualifikation, qualifiziert nach " + c.getQualRounds() + (c.getQualRounds() == 1 ? " Sieg" : " Siegen")
                    + (c.isQualExtra() ? " · mit Zusatzrunde" : "");
            case NONE -> "";
        });
        if (c.isPhase()) return sb.append(" · nach Platzierung der Vorphase").toString();
        if (!c.isDrawSeeded()) sb.append(" · zufällig");
        else if (c.getMode() == TournamentMode.GROUPS) sb.append(" · ").append(c.getGroupSeeding().getLabel());
        else sb.append(" · nach Setzung (TTR)");
        if (c.getMode() != TournamentMode.SWISS) sb.append(c.isAvoidSameClub() ? " · Vereinsgleichheit beachtet" : " · ohne Vereinsprüfung");
        return sb.toString();
    }

    /** „ · 95 Begegnungen · 32 Freilose“ (· 4 Gewinnsätze) für die Infozeile des KO-Systems. */
    private String koCounts(PlayingClass c) {
        try {
            List<Entrant> slots = tournament.drawSlots(c);
            KnockoutBracket b = new KnockoutBracket(slots);
            long byes = slots.stream().filter(e -> e == null).count();
            return " · " + b.playedMatches() + " Begegnungen" + (byes > 0 ? " · " + byes + " Freilose" : "")
                    + (tournament.rootOf(c).getWinningSets() == 4 ? " · 4 Gewinnsätze" : "");
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private void discard() {
        PlayingClass c = current;
        if (c == null || !c.isDrawn()) return;
        if (!confirm("Auslosung verwerfen", "Auslosung von „" + c.getName() + "“ verwerfen und zurück zu den Teilnehmenden der Klasse?")) {
            return;
        }
        tournament.clearDraw(c);
        status.accept("Auslosung von „" + c.getName() + "“ verworfen.");
        onChanged.run();
        onDiscard.accept(c);
    }

    private void toggleReady() {
        PlayingClass c = current;
        if (c == null) return;
        tournament.setClassReady(c, !c.isReady());
        status.accept("„" + c.getName() + "“ " + (c.isReady() ? "ist bereit." : "ist nicht mehr als bereit markiert."));
        onChanged.run();
    }

    private void startClass() {
        PlayingClass c = current;
        if (c == null) return;
        if (tournament.isDrawOutdated(c)
                && !confirm("Klasse starten", "Die Teilnehmenden haben sich seit der Auslosung geändert. Trotzdem mit dieser Auslosung starten?")) {
            return;
        }
        if (!confirm("Klasse starten", "„" + c.getName() + "“ jetzt starten? Die Spiele werden eingereiht und freie Tische belegt.\n"
                + "Danach lässt sich die Auslosung nicht mehr ändern.")) return;
        tournament.startClass(c);
        status.accept("„" + c.getName() + "“ gestartet – " + tournament.queue().size() + " Spiele in der Warteschlange.");
        onChanged.run();
    }

    private boolean confirm(String t, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
        Window w = root.getScene() == null ? null : root.getScene().getWindow();
        a.initOwner(w);
        a.setTitle(t);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
