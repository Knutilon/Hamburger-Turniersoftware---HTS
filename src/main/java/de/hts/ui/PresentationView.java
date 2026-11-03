package de.hts.ui;

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
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

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
        Label listHint = new Label("Reihenfolge: laufende Phasen, Tische und die nächsten 16 Spiele, beendete Phasen, Endergebnisse – "
                + "je Block in der Reihenfolge der Klassenliste. Häkchen entfernen nimmt eine Ansicht aus der Rotation.");
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
        String block = null;
        for (Station s : PresentationPlan.all(tournament)) {
            String b = switch (s.kind()) {
                case PHASE -> s.finished() ? "Beendete Phasen" : "Laufende Phasen";
                case TABLES, NEXT -> "Tische und Warteschlange";
                case RESULT -> "Endergebnisse";
            };
            if (!b.equals(block)) {
                Label h = new Label(b);
                h.getStyleClass().add("round-title");
                h.setPadding(new Insets(block == null ? 0 : 8, 0, 0, 0));
                stations.getChildren().add(h);
                block = b;
            }
            CheckBox cb = new CheckBox(label(s));
            cb.setSelected(!tournament.getPresentationOff().contains(s.key()));
            cb.setOnAction(e -> tournament.setPresentationOn(s.key(), cb.isSelected()));
            stations.getChildren().add(cb);
        }
        if (PresentationPlan.all(tournament).stream().noneMatch(s -> s.kind() == PresentationPlan.Kind.PHASE)) {
            Label l = new Label("Klassen erscheinen hier, sobald sie gestartet sind.");
            l.getStyleClass().add("hint");
            stations.getChildren().add(0, l);
        }
        updateButtons();
    }

    private String label(Station s) {
        return switch (s.kind()) {
            case PHASE -> tournament.rootOf(s.phase()).getName() + " · "
                    + DrawView.phaseTabTitle(tournament.phaseNumber(s.phase()), s.phase())
                    + (s.phase().isPaused() ? " (angehalten)" : "");
            case TABLES -> "Tische (laufende Partien, bei Bedarf mehrere Seiten)";
            case NEXT -> "Die nächsten 16 Spiele";
            case RESULT -> s.phase().getName() + " · Endergebnis";
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
