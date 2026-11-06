package de.hts.ui;

import de.hts.tournament.HandicapRules;
import de.hts.tournament.HandicapRules.Mode;
import de.hts.tournament.Tournament;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reiter „Vorgaberegeln“ eines Vorgabeturniers. Änderungen werden NICHT automatisch gespeichert, sondern nur über
 * „Speichern“ (mit Rückfrage); bis zum Start der ersten Klasse änderbar, danach nur lesbar.
 */
public final class HandicapView {

    private final Tournament tournament;
    private final Consumer<String> status;
    private final Runnable onSaved;
    private final Supplier<Window> owner;
    private final VBox root = new VBox(14);
    private final VBox form = new VBox(16);
    private final Label lockHint = new Label();
    private final Label error = new Label();
    private final Button saveBtn = new Button("Speichern");
    private HandicapRules working;

    public HandicapView(Tournament tournament, Consumer<String> status, Runnable onSaved, Supplier<Window> owner) {
        this.tournament = tournament;
        this.status = status;
        this.onSaved = onSaved;
        this.owner = owner;
        lockHint.getStyleClass().add("hint");
        lockHint.setWrapText(true);
        lockHint.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        lockHint.managedProperty().bind(lockHint.visibleProperty());
        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.managedProperty().bind(error.visibleProperty());
        saveBtn.getStyleClass().add("navy-pill");
        saveBtn.setOnAction(e -> save());

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("bracket-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        HBox bottom = new HBox(12, saveBtn, error);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(12, lockHint, sp, bottom);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        root.getChildren().add(card);
        root.setPadding(new Insets(0, 40, 16, 40));
    }

    public Node getRoot() {
        return root;
    }

    /** Neu aufbauen aus den gespeicherten Regeln (verwirft ungespeicherte Änderungen). */
    public void refresh() {
        working = tournament.getHandicapRules().copy();
        rebuild();
    }

    /** Gibt es ungespeicherte Änderungen? */
    public boolean isDirty() {
        return working != null && !working.encode().equals(tournament.getHandicapRules().encode());
    }

    /** Ungespeicherte Änderungen verwerfen. */
    public void discard() {
        refresh();
    }

    private boolean locked() {
        return tournament.isHandicapLocked();
    }

    private void rebuild() {
        boolean locked = locked();
        lockHint.setVisible(true);
        lockHint.setText(locked
                ? "Die erste Klasse ist gestartet – die Vorgaberegeln sind jetzt gesperrt und nur noch lesbar."
                : "Die Vorgaberegeln werden erst mit „Speichern“ übernommen (kein automatisches Speichern). Klassen, Tische und "
                + "Spiele erscheinen, sobald die Art der Vorgabe gespeichert ist. Änderbar bis zum Start der ersten Klasse.");
        error.setVisible(false);
        saveBtn.setDisable(locked);
        form.getChildren().clear();

        // Art der Vorgabe
        Label modeHead = heading("Art der Vorgabe");
        ToggleGroup tg = new ToggleGroup();
        HBox modes = new HBox(18);
        for (Mode m : Mode.values()) {
            RadioButton rb = new RadioButton(m.getLabel());
            rb.setToggleGroup(tg);
            rb.setSelected(working.getMode() == m);
            rb.setDisable(locked);
            rb.setOnAction(e -> {
                working.setMode(m);
                rebuild();
            });
            modes.getChildren().add(rb);
        }
        form.getChildren().addAll(modeHead, modes);

        // Aufschlag und Halbierung
        Label serveHead = heading("Aufschlag");
        ComboBox<HandicapRules.Serve> serve = new ComboBox<>();
        serve.getItems().setAll(HandicapRules.Serve.values());
        serve.setValue(working.getServe());
        serve.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(HandicapRules.Serve s) {
                return s == null ? "" : s.getLabel();
            }

            @Override
            public HandicapRules.Serve fromString(String v) {
                return null;
            }
        });
        serve.setDisable(locked);
        serve.valueProperty().addListener((o, a, b) -> working.setServe(b));
        CheckBox halve = new CheckBox("Halbierung im Doppel (Doppel und Mixed: beide Vorgaben verrechnen, dann halbieren und abrunden)");
        halve.setSelected(working.isHalve());
        halve.setDisable(locked);
        halve.setOnAction(e -> working.setHalve(halve.isSelected()));
        form.getChildren().addAll(serveHead, serve, heading("Doppel"), halve);

        if (working.getMode() != null) form.getChildren().add(table(working.getMode(), locked));
        else {
            Label l = new Label("Bitte zuerst die Art der Vorgabe wählen.");
            l.getStyleClass().add("hint");
            form.getChildren().add(l);
        }
    }

    private static Label heading(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("round-title");
        return l;
    }

    private static TextField numberField(Integer value, boolean locked, Consumer<Integer> onChange) {
        TextField f = new TextField(value == null ? "" : value.toString());
        f.setPrefColumnCount(5);
        f.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,5}") ? c : null));
        f.setDisable(locked);
        f.textProperty().addListener((o, a, b) -> onChange.accept(b == null || b.isBlank() ? null : Integer.parseInt(b)));
        return f;
    }

    /** Grenzen-Tabelle (Erwachsene und Damen) samt Bällen der gewählten Art. */
    private Node table(Mode mode, boolean locked) {
        boolean classes = mode == Mode.CLASSES;
        String row = classes ? "Klasse" : "Abstand";
        int n = working.rowCount(mode);
        GridPane g = new GridPane();
        g.setHgap(0);
        g.setVgap(0);
        // Kopfzeile: so groß und deutlich wie „Abstand 1“, Einheit einmal klein dahinter, ohne Trennlinien
        String unitText = classes ? "(TTR-Punkte)" : "(TTR-Punkte Abstand)";
        g.add(headCell("Erwachsene", unitText), 1, 0);
        g.add(headCell("Damen", unitText), 2, 0);
        if (!classes) g.add(headCell("Bälle Vorgabe", null), 3, 0);
        HandicapRules.Bounds[] bounds = {working.bounds(mode, false), working.bounds(mode, true)};
        Label[][] lowers = new Label[2][n];
        for (int i = 0; i < n; i++) {
            int ri = i;
            Label name = new Label(row + " " + (i + 1));
            name.getStyleClass().add("class-name");
            name.setPadding(new Insets(4, 14, 4, 0));
            g.add(name, 0, i + 1);
            for (int w = 0; w < 2; w++) {
                int wi = w;
                Label gt = new Label(">");
                Node lowerNode;
                if (i == 0) {
                    TextField lf = numberField(bounds[w].getFirstLower(), locked, v -> bounds[wi].setFirstLower(v));
                    lf.setPromptText("z. B. 0");
                    lowerNode = lf;
                } else {
                    Label ll = new Label(text(bounds[w].lower(i)));
                    ll.getStyleClass().add("pos");
                    ll.setMinWidth(62);
                    lowers[w][i] = ll;
                    lowerNode = ll;
                }
                Label le = new Label("<=");
                TextField uf = numberField(bounds[w].getUppers().get(i), locked, v -> {
                    bounds[wi].getUppers().set(ri, v);
                    if (ri + 1 < n && lowers[wi][ri + 1] != null) lowers[wi][ri + 1].setText(text(v));   // Obergrenze = Untergrenze der nächsten Zeile
                });
                HBox cell = new HBox(8, gt, lowerNode, le, uf);
                cell.setAlignment(Pos.CENTER_LEFT);
                g.add(dataCell(cell), 1 + w, i + 1);
            }
            if (!classes) {
                List<Integer> balls = working.balls(mode);
                TextField bf = numberField(balls.get(i), locked, v -> balls.set(ri, v));
                HBox ballBox = new HBox(6, bf, new Label("Bälle"));
                ballBox.setAlignment(Pos.CENTER_LEFT);
                g.add(dataCell(ballBox), 3, i + 1);
            }
        }
        // Hinweis direkt unter der (kurzen) Überschrift
        Label rule = new Label("Untergrenze „>“, Obergrenze „<=“. Die Obergrenze einer Zeile ist automatisch die Untergrenze der "
                + "nächsten – es gibt keine Lücken. " + (classes ? "Auch die höchste Klasse braucht eine Obergrenze (z. B. 3000)."
                : "Abstand 0 bedeutet keine Vorgabe. Auch die höchste Kategorie braucht eine Obergrenze."));
        rule.getStyleClass().add("hint");
        rule.setWrapText(true);
        VBox box = new VBox(10, heading(classes ? "TTR-Klassen" : "Kategorien des TTR-Abstands"), rule, g);

        int min = classes ? HandicapRules.MIN_CLASSES : HandicapRules.MIN_CATEGORIES;
        int max = classes ? HandicapRules.MAX_CLASSES : HandicapRules.MAX_CATEGORIES;
        Button add = new Button((classes ? "Klasse" : "Abstand") + " hinzufügen", Icons.of(Icons.PLUS, 14));
        add.getStyleClass().add("pill");
        add.setDisable(locked || n >= max);
        add.setOnAction(e -> {
            working.setRowCount(mode, n + 1);
            rebuild();
        });
        Button remove = new Button("Letzte entfernen");
        remove.getStyleClass().add("pill");
        remove.setDisable(locked || n <= min);
        remove.setOnAction(e -> {
            working.setRowCount(mode, n - 1);
            rebuild();
        });
        Label count = new Label(n + " von höchstens " + max + " (mindestens " + min + ")");
        count.getStyleClass().add("hint");
        HBox addRow = new HBox(10, add, remove, count);
        addRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(addRow);

        if (classes) {
            GridPane bg = new GridPane();
            bg.setHgap(8);
            bg.setVgap(8);
            List<Integer> balls = working.balls(mode);
            for (int d = 1; d <= n - 1; d++) {
                int di = d - 1;
                Label l = new Label(d + (d == 1 ? " Klasse" : " Klassen") + " Unterschied:");
                GridPane.setHalignment(l, HPos.RIGHT);
                TextField bf = numberField(balls.get(di), locked, v -> balls.set(di, v));
                bg.addRow(d - 1, l, bf, new Label("Bälle"));
            }
            box.getChildren().addAll(heading("Bälle Vorgabe je Klassenunterschied (Pflicht für jeden Unterschied)"), bg);
        }
        return box;
    }

    /** Spaltenkopf: fett wie „Abstand 1“, Einheit klein dahinter. */
    private static Node headCell(String title, String unit) {
        Label t = new Label(title);
        t.getStyleClass().add("class-name");
        HBox h = new HBox(6, t);
        if (unit != null) {
            Label u = new Label(unit);
            u.getStyleClass().add("hint");
            h.getChildren().add(u);
        }
        h.setAlignment(Pos.BASELINE_LEFT);
        h.setPadding(new Insets(0, 14, 6, 14));
        return h;
    }

    /** Datenzelle mit leicht gestrichelter senkrechter Linie links (nur in den Zeilen „Abstand n“/„Klasse n“). */
    private static Node dataCell(Node content) {
        HBox c = new HBox(content);
        c.setAlignment(Pos.CENTER_LEFT);
        c.getStyleClass().add("hc-sep");
        return c;
    }

    private static String text(Integer v) {
        return v == null ? "–" : v.toString();
    }

    /** „Speichern“: prüfen, Rückfrage „Sind Sie sicher …?“ (Speichern / Zurück), dann übernehmen. */
    public boolean save() {
        if (locked()) return false;
        List<String> errors = working.validate();
        if (!errors.isEmpty()) {
            error.setText(String.join("\n", errors));
            error.setVisible(true);
            status.accept("Vorgaberegeln unvollständig – nicht gespeichert.");
            return false;
        }
        error.setVisible(false);
        ButtonType yes = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType back = new ButtonType("Zurück", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Sind Sie sicher, dass Sie die Vorgaberegeln speichern möchten?", yes, back);
        a.initOwner(owner.get());
        a.setTitle("Vorgaberegeln speichern");
        a.setHeaderText(null);
        if (a.showAndWait().orElse(back) != yes) return false;
        try {
            tournament.setHandicapRules(working);
        } catch (RuntimeException ex) {
            error.setText(ex.getMessage());
            error.setVisible(true);
            return false;
        }
        status.accept("Vorgaberegeln gespeichert.");
        refresh();
        onSaved.run();
        return true;
    }

    /**
     * Vor dem Verlassen: bei ungespeicherten Änderungen Rückfrage „Speichern“, „Verwerfen“, „Zurück“.
     *
     * @return {@code true}, wenn verlassen werden darf
     */
    public boolean confirmLeave() {
        if (!isDirty()) return true;
        ButtonType saveB = new ButtonType("Speichern", ButtonBar.ButtonData.YES);
        ButtonType discardB = new ButtonType("Verwerfen", ButtonBar.ButtonData.NO);
        ButtonType back = new ButtonType("Zurück", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.WARNING, "Es gibt ungespeicherte Änderungen an den Vorgaberegeln.", saveB, discardB, back);
        a.initOwner(owner.get());
        a.setTitle("Vorgaberegeln");
        a.setHeaderText(null);
        ButtonType r = a.showAndWait().orElse(back);
        if (r == saveB) return save();
        if (r == discardB) {
            discard();
            return true;
        }
        return false;
    }
}
