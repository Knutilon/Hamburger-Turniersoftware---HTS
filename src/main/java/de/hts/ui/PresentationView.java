/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.tournament.PlayingClass;
import de.hts.tournament.PresentationPlan;
import de.hts.tournament.PresentationPlan.Station;
import de.hts.tournament.Tournament;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Seite „Präsentationsansicht“: Anzeige auf dem zweiten Bildschirm starten/beenden, Wechselzeit und welche Ansichten
 * (Stationen) in der Rotation laufen.
 */
public final class PresentationView {

    private final Tournament tournament;
    private final Consumer<String> status;
    private final VBox root = new VBox(14);
    private final Label screenInfo = new Label();
    private final Button startBtn = new Button("Auf zweitem Bildschirm starten", Icons.of(Icons.START, 14));
    private final Button previewBtn = new Button("Vorschau im Fenster");
    private final Button stopBtn = new Button("Anzeige beenden");
    private final Spinner<Integer> seconds = new Spinner<>(5, 600, 20, 5);
    private final VBox stations = new VBox(6);
    private PresentationShow show;

    public PresentationView(Tournament tournament, Consumer<String> status) {
        this.tournament = tournament;
        this.status = status;

        screenInfo.getStyleClass().add("criteria");
        startBtn.getStyleClass().add("navy-pill");
        previewBtn.getStyleClass().add("pill");
        stopBtn.getStyleClass().add("pill");
        startBtn.setTooltip(new Tooltip("Randlose Anzeige auf dem zweiten Bildschirm – Esc auf jenem Bildschirm oder „Anzeige beenden“ schließt sie"));
        previewBtn.setTooltip(new Tooltip("Dieselbe Anzeige in einem normalen Fenster, z. B. zum Ausprobieren ohne zweiten Bildschirm"));
        startBtn.setOnAction(e -> start(false));
        previewBtn.setOnAction(e -> start(true));
        stopBtn.setOnAction(e -> {
            if (show != null) show.close();
        });
        HBox actions = new HBox(10, startBtn, previewBtn, stopBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label secLabel = new Label("Wechselzeit:");
        secLabel.getStyleClass().add("class-name");
        seconds.setEditable(true);
        seconds.setPrefWidth(90);
        seconds.valueProperty().addListener((o, a, b) -> {
            if (b != null && b != tournament.getPresentationSeconds()) tournament.setPresentationSeconds(b);
        });
        Label secUnit = new Label("Sekunden je Seite");
        secUnit.getStyleClass().add("hint");
        HBox secRow = new HBox(10, secLabel, seconds, secUnit);
        secRow.setAlignment(Pos.CENTER_LEFT);

        Label listHead = new Label("Ansichten in der Rotation");
        listHead.getStyleClass().add("card-heading");
        Label listHint = new Label("Reihenfolge der Rotation: laufende Phasen, Tische und die nächsten 16 Spiele, beendete Phasen, "
                + "Endergebnisse, zuletzt ausgeloste, noch nicht gestartete Klassen – je Block in der Reihenfolge der Klassenliste. "
                + "Häkchen entfernen nimmt eine Ansicht aus der Rotation; noch nicht gestartete Klassen sind zunächst ausgeschaltet.");
        listHint.getStyleClass().add("hint");
        listHint.setWrapText(true);
        ScrollPane sp = new ScrollPane(stations);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("bracket-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox card = new VBox(12, screenInfo, actions, secRow, listHead, listHint, sp);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        root.getChildren().add(card);
        root.setPadding(new Insets(0, 40, 16, 40));

        Screen.getScreens().addListener((ListChangeListener<Screen>) c -> updateButtons());
    }

    public Node getRoot() {
        return root;
    }

    public void refresh() {
        seconds.getValueFactory().setValue(tournament.getPresentationSeconds());
        stations.getChildren().clear();
        List<Station> all = PresentationPlan.all(tournament);

        // eigener Block: Tische und Warteschlange
        stations.getChildren().add(blockTitle("Tische und Warteschlange", true));
        for (Station s : all) if (s.kind() == PresentationPlan.Kind.TABLES || s.kind() == PresentationPlan.Kind.NEXT) stations.getChildren().add(check(s));

        // je Klasse: Zustand, alle Phasen und (beendet) das Endergebnis
        List<PlayingClass> roots = tournament.getClasses().stream().filter(c -> !c.isPhase()).toList();
        if (!roots.isEmpty()) stations.getChildren().add(blockTitle("Klassen", false));
        for (PlayingClass root : roots) {
            PresentationPlan.ClassState st = PresentationPlan.stateOf(tournament, root);
            Label name = new Label(root.getName());
            name.getStyleClass().add("class-name");
            Label chip = new Label(st.getLabel());
            chip.getStyleClass().addAll("chip", switch (st) {
                case NOT_DRAWN -> "chip-planned";
                case DRAWN -> "chip-ready";
                case RUNNING -> "chip-started";
                case PAUSED -> "chip-paused";
                case FINISHED -> "chip-finished";
            });
            chip.setMinWidth(Region.USE_PREF_SIZE);
            HBox head = new HBox(10, name, chip);
            head.setAlignment(Pos.CENTER_LEFT);
            head.setPadding(new Insets(6, 0, 0, 0));
            stations.getChildren().add(head);
            List<Station> mine = new ArrayList<>();
            for (Station s : all) {
                if (s.phase() == null) continue;
                PlayingClass r = s.kind() == PresentationPlan.Kind.RESULT ? s.phase() : tournament.rootOf(s.phase());
                if (r == root) mine.add(s);
            }
            // Phasen in ihrer Reihenfolge, das Endergebnis zuletzt
            mine.sort(java.util.Comparator.comparingInt((Station s) -> s.kind() == PresentationPlan.Kind.RESULT ? 999
                    : tournament.phaseNumber(s.phase())));
            if (mine.isEmpty()) {
                Label l = new Label("Erscheint hier, sobald die Klasse ausgelost ist.");
                l.getStyleClass().add("hint");
                l.setPadding(new Insets(0, 0, 0, 22));
                stations.getChildren().add(l);
            }
            for (Station s : mine) {
                CheckBox cb = check(s);
                cb.setPadding(new Insets(0, 0, 0, 22));
                stations.getChildren().add(cb);
            }
        }
        updateButtons();
    }

    private Label blockTitle(String text, boolean first) {
        Label h = new Label(text);
        h.getStyleClass().add("round-title");
        h.setPadding(new Insets(first ? 0 : 10, 0, 0, 0));
        return h;
    }

    private CheckBox check(Station s) {
        CheckBox cb = new CheckBox(label(s));
        cb.setSelected(PresentationPlan.isOn(tournament, s));
        cb.setOnAction(e -> tournament.setPresentationOn(s.key(), cb.isSelected()));
        if (s.upcoming()) cb.setTooltip(new Tooltip("Noch nicht gestartet – standardmäßig nicht in der Rotation; eingeschaltet erscheint "
                + "die Auslosung am Ende der Rotation."));
        return cb;
    }

    private String label(Station s) {
        return switch (s.kind()) {
            case PHASE -> DrawView.phaseTabTitle(tournament.phaseNumber(s.phase()), s.phase())
                    + (s.upcoming() ? " (Auslosung)" : s.finished() ? " (abgeschlossen)" : s.phase().isPaused() ? " (angehalten)" : " (läuft)");
            case TABLES -> "Tische (laufende Partien, bei Bedarf mehrere Seiten)";
            case NEXT -> "Die nächsten 16 Spiele";
            case RESULT -> "Endergebnis";
        };
    }

    private Screen secondScreen() {
        List<Screen> all = Screen.getScreens();
        Screen primary = Screen.getPrimary();
        return all.stream().filter(s -> !s.equals(primary)).findFirst().orElse(null);
    }

    private void updateButtons() {
        Screen second = secondScreen();
        boolean running = show != null && show.isShowing();
        screenInfo.setText(second != null
                ? "Zweiter Bildschirm erkannt (" + (int) second.getBounds().getWidth() + " × " + (int) second.getBounds().getHeight() + ")."
                + (running ? " Die Anzeige läuft." : "")
                : "Kein zweiter Bildschirm erkannt – zum Ausprobieren gibt es die Vorschau im Fenster." + (running ? " Die Anzeige läuft." : ""));
        startBtn.setDisable(second == null || running);
        previewBtn.setDisable(running);
        stopBtn.setDisable(!running);
    }

    private void start(boolean preview) {
        Screen target = preview ? Screen.getPrimary() : secondScreen();
        if (target == null) {
            status.accept("Kein zweiter Bildschirm erkannt.");
            updateButtons();
            return;
        }
        show = PresentationShow.start(tournament, target, preview, () -> {
            status.accept("Präsentationsansicht beendet.");
            updateButtons();
        });
        status.accept(preview ? "Präsentationsansicht als Vorschau gestartet (Esc beendet)." : "Präsentationsansicht auf dem zweiten Bildschirm gestartet.");
        updateButtons();
    }

    /** Beim Schließen des Turniers die Anzeige mit beenden. */
    public void stop() {
        if (show != null) show.close();
    }
}
