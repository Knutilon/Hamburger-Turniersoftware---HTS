/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.tournament.Match;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Seite „Tische“: alle Spieltische mit ihrem aktiven Spiel; rechts die Warteschlange.
 * Ein Tisch wird frei, sobald für sein Spiel ein Ergebnis erfasst ist – dann rückt automatisch das nächste
 * eingereihte Spiel nach.
 */
public final class TablesView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Tournament tournament;
    private final Consumer<String> status;
    private final Runnable onChanged;

    private final HBox root = new HBox(16);
    private final javafx.scene.layout.GridPane tables = new javafx.scene.layout.GridPane();
    private final List<Node> cards = new java.util.ArrayList<>();
    private ScrollPane tablesScroll;
    private int lastCols = -1;
    private final VBox queueList = new VBox(6);
    private final Label queueTitle = new Label();
    private final Spinner<Integer> tableCount = new Spinner<>(1, 200, 1);
    private boolean updating;

    public TablesView(Tournament tournament, Consumer<String> status, Runnable onChanged) {
        this.tournament = tournament;
        this.status = status;
        this.onChanged = onChanged;

        Label countLabel = new Label("Anzahl Tische:");
        countLabel.getStyleClass().add("class-name");
        tableCount.setEditable(true);
        tableCount.setPrefWidth(90);
        tableCount.valueProperty().addListener((o, a, b) -> {
            if (updating || b == null) return;
            tournament.setTableCount(b);
            tournament.dispatch();
            status.accept("Anzahl Tische: " + b);
            changed();
        });
        javafx.scene.control.CheckBox autoPrint = new javafx.scene.control.CheckBox(
                "Spielzettel automatisch drucken, sobald ein Spiel an einen Tisch geht (Standarddrucker, A6)");
        autoPrint.setSelected(Reports.isAutoPrintSlips());
        autoPrint.selectedProperty().addListener((o, a, b) -> {
            Reports.setAutoPrintSlips(b);
            status.accept(b ? "Spielzettel werden automatisch gedruckt." : "Spielzettel werden nicht mehr automatisch gedruckt.");
        });
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button assign = new Button("Tische den Klassen zuordnen");
        assign.getStyleClass().add("pill");
        assign.setTooltip(new Tooltip("Bestimmte Tische nur für bestimmte Klassen – Standard: alle Tische für alle Klassen"));
        assign.setOnAction(e -> openAssignment());
        HBox countRow = new HBox(10, countLabel, tableCount, assign, gap, autoPrint);
        countRow.setAlignment(Pos.CENTER_LEFT);

        tables.setHgap(CARD_GAP);
        tables.setVgap(CARD_GAP);
        ScrollPane scroll = new ScrollPane(tables);
        tablesScroll = scroll;
        scroll.viewportBoundsProperty().addListener((o, a, b) -> layoutCards());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("bracket-scroll");
        tables.setPadding(new Insets(4));
        VBox left = new VBox(12, countRow, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        left.getStyleClass().add("card");
        HBox.setHgrow(left, Priority.ALWAYS);

        queueTitle.getStyleClass().add("card-heading");
        ScrollPane qs = new ScrollPane(queueList);
        qs.setFitToWidth(true);
        qs.getStyleClass().add("bracket-scroll");
        VBox right = new VBox(10, queueTitle, qs);
        VBox.setVgrow(qs, Priority.ALWAYS);
        right.getStyleClass().add("card");
        right.setPrefWidth(380);
        right.setMinWidth(320);

        root.getChildren().addAll(left, right);
        root.setPadding(new Insets(0, 40, 16, 40));
    }

    public Node getRoot() {
        return root;
    }

    public void refresh() {
        updating = true;
        tableCount.getValueFactory().setValue(tournament.getTableCount());
        updating = false;

        cards.clear();
        for (int t = 1; t <= tournament.getTableCount(); t++) cards.add(tableCard(t));
        lastCols = -1;
        layoutCards();
        List<Match> queue = tournament.queue();
        queueTitle.setText("Warteschlange (" + queue.size() + ")");
        queueList.getChildren().clear();
        if (queue.isEmpty()) {
            Label l = new Label(tournament.isRunning() ? "Keine eingereihten Spiele." : "Noch keine Klasse gestartet.");
            l.getStyleClass().add("hint");
            queueList.getChildren().add(l);
        }
        int pos = 1;
        for (Match m : queue) {
            Label nr = new Label(String.valueOf(pos++));
            nr.getStyleClass().add("pos");
            nr.setMinWidth(24);
            Label what = new Label(m.getClassName() + " · " + m.getStage() + " · Spiel " + m.getNumber());
            what.getStyleClass().add("hint");
            Label who = new Label(tournament.entrantName(m, m.getA()) + tournament.handicapSuffix(m, m.getA()) + "  –  "
                    + tournament.entrantName(m, m.getB()) + tournament.handicapSuffix(m, m.getB()));
            who.getStyleClass().add("match-name");
            who.setWrapText(true);
            Region qsp = new Region();
            HBox.setHgrow(qsp, Priority.ALWAYS);
            Button call = new Button("an Tisch");
            call.getStyleClass().addAll("pill", "small-pill");
            call.setTooltip(new Tooltip("Dieses Spiel sofort an einen freien Tisch holen"));
            call.setDisable(tournament.freeTables().isEmpty());
            call.setOnAction(e -> moveMatch(m));
            HBox row = new HBox(8, nr, new VBox(1, who, what), qsp, call);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("queue-row");
            queueList.getChildren().add(row);
        }
    }

    static final double CARD_WIDTH = 340;
    static final double CARD_GAP = 14;

    /** Karten im Raster: alle gleich breit, je Zeile gleich hoch; Spaltenzahl nach Fensterbreite. */
    private void layoutCards() {
        double w = tablesScroll.getViewportBounds().getWidth();
        if (w <= 0) w = tablesScroll.getWidth();
        int cols = Math.max(1, (int) ((w - 8 + CARD_GAP) / (CARD_WIDTH + CARD_GAP)));
        if (cols == lastCols && tables.getChildren().size() == cards.size()) return;
        lastCols = cols;
        tables.getChildren().clear();
        tables.getColumnConstraints().clear();
        for (int c = 0; c < cols; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints(CARD_WIDTH, CARD_WIDTH, CARD_WIDTH);
            tables.getColumnConstraints().add(cc);
        }
        for (int k = 0; k < cards.size(); k++) tables.add(cards.get(k), k % cols, k / cols);
    }

    private Node tableCard(int t) {
        Label title = new Label(tournament.tableName(t));
        title.getStyleClass().addAll("round-title", "table-name");
        title.setMinWidth(Region.USE_PREF_SIZE);
        title.setTooltip(new Tooltip("Klicken: Tisch umbenennen (bis " + Tournament.TABLE_NAME_MAX + " Zeichen)"));
        title.setOnMouseClicked(e -> rename(t));
        VBox card = new VBox(8);
        card.getStyleClass().add("table-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setMinHeight(150);
        card.setMaxHeight(Double.MAX_VALUE);
        javafx.scene.layout.GridPane.setFillHeight(card, true);
        javafx.scene.layout.GridPane.setValignment(card, javafx.geometry.VPos.TOP);
        Match m = tournament.activeMatchAt(t).orElse(null);
        java.util.Set<String> excluded = tournament.getTableExcluded(t);
        java.util.List<String> allowed = tournament.rootClasses().stream().map(de.hts.tournament.PlayingClass::getName)
                .filter(n -> !excluded.contains(n)).toList();
        boolean none = tournament.tableHasNoClass(t);
        String onlyText = excluded.isEmpty() ? "alle Klassen" : none ? "keine Klasse zugeordnet" : "nur: " + String.join(", ", allowed);
        Label onlyLabel = new Label(onlyText);
        onlyLabel.getStyleClass().addAll("hint", excluded.isEmpty() ? "table-all" : "table-only");
        onlyLabel.setWrapText(true);                         // 0.38: Klassenliste nicht abkürzen, sondern umbrechen
        onlyLabel.setMinWidth(0);
        onlyLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        onlyLabel.setOnMouseClicked(e -> openAssignment());
        onlyLabel.setTooltip(new Tooltip(onlyText + "\nKlicken: Tische den Klassen zuordnen"));
        HBox.setHgrow(onlyLabel, Priority.SOMETIMES);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        // Kopfzeile einheitlich: Name, direkt dahinter die Klassen, rechts der Status
        Label chip;
        if (m == null && tournament.isTableDisabled(t)) {
            chip = new Label("deaktiviert");
            chip.getStyleClass().add("chip-disabled");
        } else if (m == null) {
            chip = new Label("frei");
            chip.getStyleClass().addAll("chip", "chip-free");
        } else {
            chip = DrawView.statusChip(m.getStatus());
        }
        chip.setMinWidth(Region.USE_PREF_SIZE);
        HBox head = new HBox(8, title, onlyLabel, sp, chip);
        head.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(head);
        if (m == null && tournament.isTableDisabled(t)) {
            Label hint = new Label("Bekommt keine Spiele, bis er wieder aktiviert wird.");
            hint.getStyleClass().add("hint");
            hint.setWrapText(true);
            card.getChildren().addAll(hint, toggleButton(t, false));
            card.getStyleClass().add("table-disabled");
            return card;
        }
        if (m == null) {
            Label hint = new Label(tournament.queue().isEmpty() ? "Wartet auf eingereihte Spiele"
                    : "Nächste Spiele warten auf Personen, die noch spielen");
            hint.getStyleClass().add("hint");
            hint.setWrapText(true);
            card.getChildren().addAll(hint, toggleButton(t, true));
            card.getStyleClass().add("table-free");
            return card;
        }
        Label what = new Label(m.getClassName() + " · " + m.getStage() + " · Spiel " + m.getNumber());
        what.getStyleClass().add("hint");
        what.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        what.setMinWidth(0);
        Label a = new Label(tournament.entrantName(m, m.getA()));
        Label vs = new Label("gegen");
        vs.getStyleClass().add("hint");
        Label b = new Label(tournament.entrantName(m, m.getB()));
        a.getStyleClass().add("table-player");
        b.getStyleClass().add("table-player");
        DrawView.handicapBadge(a, tournament, m, m.getA());
        DrawView.handicapBadge(b, tournament, m, m.getB());
        a.setWrapText(true);
        b.setWrapText(true);
        String since = "";
        if (m.getStartedAt() != null) {
            long min = Duration.between(m.getStartedAt(), LocalDateTime.now()).toMinutes();
            since = "seit " + m.getStartedAt().format(TIME) + " Uhr (" + min + " min)";
        }
        Label time = new Label(since);
        time.getStyleClass().add("hint");
        Button result = new Button("Ergebnis");
        result.getStyleClass().addAll("navy-pill", "small-pill");
        result.setTooltip(new Tooltip("Ergebnis eintragen"));
        result.setOnAction(e -> enterResult(m));
        Button slip = new Button("Drucken");
        slip.getStyleClass().addAll("pill", "small-pill");
        slip.setTooltip(new Tooltip("Spielzettel (A6) als PDF anzeigen – speichern oder erneut drucken"));
        slip.setOnAction(e -> Reports.showSlip(window(), tournament, m, status));
        Button move = new Button("wechseln");
        move.getStyleClass().addAll("pill", "small-pill");
        move.setTooltip(new Tooltip("Spiel an einen anderen Tisch verschieben (belegter Tisch: die Spiele tauschen)"));
        move.setOnAction(e -> moveMatch(m));
        Button back = new Button("zurückziehen");
        back.getStyleClass().addAll("pill", "small-pill");
        back.setTooltip(new Tooltip("Spiel vom Tisch nehmen – es reiht sich hinten in der Warteschlange wieder ein"));
        back.setOnAction(e -> {
            tournament.withdrawMatch(m);
            status.accept("Spiel " + m.getNumber() + " zurückgezogen – " + tournament.tableName(t) + " ist neu belegt bzw. frei.");
            changed();
        });
        VBox actions = new VBox(6);
        if (m.getStatus() == de.hts.tournament.MatchStatus.PAUSED) {
            // pausiert: kein Ergebnis – bewusst fortsetzen
            Button resume = new Button("fortsetzen");
            resume.getStyleClass().addAll("navy-pill", "small-pill");
            resume.setTooltip(new Tooltip("Dieses Spiel weiterspielen (auch wenn die Phase noch angehalten ist)"));
            resume.setOnAction(e -> {
                tournament.resumeMatch(m);
                status.accept("Spiel " + m.getNumber() + " an " + tournament.tableName(t) + " fortgesetzt.");
                changed();
            });
            actions.getChildren().addAll(resume, slip, move, back);
            card.getStyleClass().add("table-paused");
        } else {
            actions.getChildren().addAll(result, slip, move, back);
        }
        for (javafx.scene.Node btn : actions.getChildren()) ((Button) btn).setMaxWidth(Double.MAX_VALUE);
        // links die Partie, rechts daneben die Aktionen
        VBox info = new VBox(6, what, a, vs, b, time);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);
        actions.setMinWidth(Region.USE_PREF_SIZE);
        actions.setAlignment(Pos.TOP_RIGHT);
        HBox body = new HBox(10, info, actions);
        card.getChildren().add(body);
        return card;
    }

    /** Tisch umbenennen (Klick auf den Namen); leer = Standardname „Tisch n“. */
    private void rename(int t) {
        javafx.scene.control.TextInputDialog d = new javafx.scene.control.TextInputDialog(tournament.tableName(t));
        d.initOwner(window());
        d.setTitle("Tisch umbenennen");
        d.setHeaderText("Name für Tisch " + t + " (bis " + Tournament.TABLE_NAME_MAX + " Zeichen, leer = „Tisch " + t + "“)");
        d.setContentText("Name:");
        javafx.scene.control.TextField f = d.getEditor();
        f.setTextFormatter(new javafx.scene.control.TextFormatter<String>(ch ->
                ch.getControlNewText().length() <= Tournament.TABLE_NAME_MAX ? ch : null));
        f.setPrefColumnCount(Tournament.TABLE_NAME_MAX);
        d.showAndWait().ifPresent(name -> {
            tournament.setTableName(t, name);
            status.accept("Tisch " + t + " heißt jetzt „" + tournament.tableName(t) + "“.");
            changed();
        });
    }

    /** „Tisch deaktivieren“ (nur ohne Spiel am Tisch) bzw. „Tisch aktivieren“. */
    private Node toggleButton(int t, boolean disable) {
        Button b = new Button(disable ? "Tisch deaktivieren" : "Tisch aktivieren");
        b.getStyleClass().addAll(disable ? "pill" : "navy-pill", "small-pill");
        b.setTooltip(new Tooltip(disable ? "Tisch vorübergehend sperren – er bekommt keine Spiele mehr"
                : "Tisch wieder freigeben – das nächste passende Spiel kommt sofort hierher"));
        b.setOnAction(e -> {
            try {
                tournament.setTableDisabled(t, disable);
                status.accept(tournament.tableName(t) + (disable ? " deaktiviert." : " wieder aktiviert."));
            } catch (IllegalStateException ex) {
                status.accept(ex.getMessage());
            }
            changed();
        });
        return b;
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private void openAssignment() {
        if (new TableAssignmentDialog(window(), tournament).showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            status.accept("Tischzuordnung gespeichert.");
            changed();
        }
    }

    /** Laufendes Spiel an einen anderen Tisch (belegt = tauschen) bzw. eingereihtes Spiel an einen freien Tisch. */
    private void moveMatch(Match m) {
        boolean active = m.getStatus().atTable();
        List<String> choices = new java.util.ArrayList<>();
        java.util.Map<String, Integer> byLabel = new java.util.HashMap<>();
        for (int t = 1; t <= tournament.getTableCount(); t++) {
            if (active && Integer.valueOf(t).equals(m.getTable())) continue;
            if (tournament.isTableDisabled(t)) continue;
            Match other = tournament.activeMatchAt(t).orElse(null);
            if (!active && other != null) continue;
            String label = tournament.tableName(t) + (tournament.getTableNames().containsKey(t) ? " (Tisch " + t + ")" : "") + (other == null ? " (frei)" : " (belegt – Spiele tauschen)")
                    + (tournament.tableAccepts(t, m) ? "" : " – eigentlich für andere Klassen");
            choices.add(label);
            byLabel.put(label, t);
        }
        if (choices.isEmpty()) {
            status.accept("Kein freier Tisch.");
            return;
        }
        javafx.scene.control.ChoiceDialog<String> d = new javafx.scene.control.ChoiceDialog<>(choices.get(0), choices);
        d.initOwner(window());
        d.setTitle(active ? "Tisch wechseln" : "An Tisch holen");
        d.setHeaderText("Spiel " + m.getNumber() + ": " + tournament.entrantName(m, m.getA()) + " – " + tournament.entrantName(m, m.getB())
                + (active ? "\nbisher an " + tournament.tableName(m.getTable()) : ""));
        d.setContentText("Neuer Tisch:");
        d.showAndWait().ifPresent(choice -> {
            int target = byLabel.get(choice);
            try {
                tournament.moveMatch(m, target);
                status.accept("Spiel " + m.getNumber() + " jetzt an " + tournament.tableName(target) + ".");
                changed();
            } catch (IllegalStateException | IllegalArgumentException ex) {
                status.accept(ex.getMessage());
            }
        });
    }

    private void enterResult(Match m) {
        Window w = window();
        if (ResultEntry.open(w, tournament, m, status)) changed();
    }

    private void changed() {
        refresh();
        onChanged.run();
    }
}
