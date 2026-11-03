package de.hts.ui;

import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog zum Anlegen bzw. Bearbeiten einer Spielklasse.
 * Ergebnis: {@code true}, wenn gespeichert wurde. Die Änderungen werden direkt am Turnier vorgenommen.
 */
public final class ClassDialog extends Dialog<Boolean> {

    private final TextField name = new TextField();
    private final ToggleGroup disciplineGroup = new ToggleGroup();
    private final RadioButton singles = new RadioButton("Einzel");
    private final RadioButton doubles = new RadioButton("Doppel (Paarungen aus je 2 Teilnehmenden)");
    private final ToggleGroup ageGroup = new ToggleGroup();
    private final RadioButton adults = new RadioButton("Erwachsene (jedes Alter – auch unter 21)");
    private final RadioButton youth = new RadioButton("Jugend (unter 21)");
    private final ToggleGroup sexGroup = new ToggleGroup();
    private final RadioButton everyone = new RadioButton("weiblich und männlich");
    private final RadioButton femaleOnly = new RadioButton("nur weiblich");
    private final Label typeLabel = new Label();
    private final TextField yearFrom = yearField();
    private final TextField yearTo = yearField();
    private final TextField maxField = yearField();
    private final Label maxLabel = new Label("Max. Teilnehmende:");
    private final Label preview = new Label();
    private final Label error = new Label();

    private PlayingClass created;

    /**
     * @param existing {@code null} = neue Klasse anlegen
     */
    public ClassDialog(Window owner, Tournament tournament, PlayingClass existing) {
        initOwner(owner);
        setTitle(existing == null ? "Neue Klasse" : "Klasse bearbeiten");
        setHeaderText(existing == null ? "Neue Spielklasse anlegen" : "Spielklasse „" + existing.getName() + "“ bearbeiten");

        name.setPromptText("z. B. Herren A, Damen, Jugend, Mädchen U15");
        name.setPrefColumnCount(24);
        singles.setToggleGroup(disciplineGroup);
        doubles.setToggleGroup(disciplineGroup);
        (existing != null && existing.isDoubles() ? doubles : singles).setSelected(true);
        adults.setToggleGroup(ageGroup);
        youth.setToggleGroup(ageGroup);
        everyone.setToggleGroup(sexGroup);
        femaleOnly.setToggleGroup(sexGroup);
        yearFrom.setPromptText("z. B. 2012");
        yearTo.setPromptText("leer = keine Grenze");

        ClassType type = existing != null ? existing.getType() : ClassType.ADULTS;
        (type.isYouthOnly() ? youth : adults).setSelected(true);
        (type.isFemaleOnly() ? femaleOnly : everyone).setSelected(true);
        if (existing != null) {
            name.setText(existing.getName());
            yearFrom.setText(existing.getBirthyearFrom() == null ? "" : existing.getBirthyearFrom().toString());
            yearTo.setText(existing.getBirthyearTo() == null ? "" : existing.getBirthyearTo().toString());
            maxField.setText(existing.getMaxParticipants() == null ? "" : existing.getMaxParticipants().toString());
        }
        typeLabel.getStyleClass().add("class-name");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        int r = 0;
        grid.addRow(r++, new Label("Name:"), name);
        grid.addRow(r++, new Label("Spielart:"), new HBox(16, singles, doubles));
        grid.addRow(r++, new Label("Alter:"), new VBox(4, adults, youth));
        grid.addRow(r++, new Label("Wer darf mitspielen:"), new VBox(4, everyone, femaleOnly));
        grid.addRow(r++, new Label("Klassentyp:"), typeLabel);
        grid.addRow(r++, new Label("Jahrgang von:"), hint(yearFrom, "optional, ältester zulässiger Jahrgang"));
        grid.addRow(r++, new Label("Jahrgang bis:"), hint(yearTo, "optional, jüngster zulässiger Jahrgang"));
        maxField.setPromptText("leer = offen");
        grid.addRow(r++, maxLabel, hint(maxField, "optional, nur zur Kontrolle: wer darüber liegt, wird markiert"));
        Label examples = new Label("Beispiele: Mädchen U15 → Jugend + nur weiblich, Jahrgang von 2012 · "
                + "Senioren 40 → Erwachsene, Jahrgang bis 1986");
        examples.getStyleClass().add("hint");
        examples.setWrapText(true);
        grid.add(examples, 0, r++, 2, 1);
        grid.add(preview, 0, r++, 2, 1);
        grid.add(error, 0, r, 2, 1);
        preview.getStyleClass().add("criteria");
        preview.setWrapText(true);
        error.getStyleClass().add("error");

        Runnable update = () -> {
            typeLabel.setText(selectedType().getLabel());
            preview.setText("Kriterien (Hinweis beim Zuordnen): " + buildPreview().getCriteriaText(tournament.referenceDate()));
        };
        name.textProperty().addListener((o, a, b) -> update.run());
        ageGroup.selectedToggleProperty().addListener((o, a, b) -> update.run());
        disciplineGroup.selectedToggleProperty().addListener((o, a, b) -> update.run());
        sexGroup.selectedToggleProperty().addListener((o, a, b) -> update.run());
        yearFrom.textProperty().addListener((o, a, b) -> update.run());
        yearTo.textProperty().addListener((o, a, b) -> update.run());
        disciplineGroup.selectedToggleProperty().addListener((o, a, b) ->
                maxLabel.setText(doubles.isSelected() ? "Max. Doppel:" : "Max. Teilnehmende:"));
        maxLabel.setText(doubles.isSelected() ? "Max. Doppel:" : "Max. Teilnehmende:");
        update.run();

        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(620);
        boolean locked = existing != null && tournament.hasResults(existing);
        if (locked) {
            // Nach dem ersten Ergebnis in Phase 1: Einstellungen nur noch lesbar
            setHeaderText("Spielklasse „" + existing.getName() + "“ – nur Ansicht");
            for (javafx.scene.Node n : grid.getChildren()) {
                if (n != preview && n != error && !(n instanceof Label)) n.setDisable(true);
            }
            error.setText("In Phase 1 sind bereits Ergebnisse eingetragen – die Einstellungen der Klasse lassen sich "
                    + "nicht mehr ändern.");
            error.getStyleClass().remove("error");
            error.getStyleClass().add("hint");
            error.setWrapText(true);
            getDialogPane().getButtonTypes().add(new ButtonType("Schließen", ButtonBar.ButtonData.CANCEL_CLOSE));
            DialogSizing.fit(this);
            setResultConverter(bt -> false);
            return;
        }
        ButtonType ok = new ButtonType(existing == null ? "Anlegen" : "Speichern", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        // Prüfen und übernehmen, bevor der Dialog schließt
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String msg = validateAndApply(tournament, existing);
            if (msg != null) {
                error.setText(msg);
                e.consume();
            }
        });
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok);
        setOnShown(e -> name.requestFocus());
    }

    /** Die neu angelegte bzw. bearbeitete Klasse (nach erfolgreichem Abschluss). */
    public PlayingClass getPlayingClass() {
        return created;
    }

    private Discipline selectedDiscipline() {
        return doubles.isSelected() ? Discipline.DOUBLES : Discipline.SINGLES;
    }

    private ClassType selectedType() {
        return ClassType.of(youth.isSelected(), femaleOnly.isSelected());
    }

    private PlayingClass buildPreview() {
        return new PlayingClass(name.getText().isBlank() ? "?" : name.getText().trim(), selectedDiscipline(),
                selectedType(), parseYear(yearFrom.getText()), parseYear(yearTo.getText()));
    }

    /** @return Fehlermeldung oder {@code null}, wenn alles übernommen wurde */
    private String validateAndApply(Tournament t, PlayingClass existing) {
        String n = name.getText() == null ? "" : name.getText().trim();
        if (n.isEmpty()) return "Bitte einen Namen eingeben.";
        Integer from = parseYear(yearFrom.getText());
        Integer to = parseYear(yearTo.getText());
        if (!yearFrom.getText().isBlank() && from == null) return "„Jahrgang von“ muss eine vierstellige Jahreszahl sein.";
        if (!yearTo.getText().isBlank() && to == null) return "„Jahrgang bis“ muss eine vierstellige Jahreszahl sein.";
        if (from != null && to != null && from > to) {
            return "„Jahrgang von“ (ältester) muss vor „Jahrgang bis“ (jüngster) liegen.";
        }
        var clash = t.findClass(n).filter(c -> c != existing);
        if (clash.isPresent()) return "Es gibt bereits eine Klasse „" + clash.get().getName() + "“.";

        if (existing == null) {
            created = t.addClass(new PlayingClass(n, selectedDiscipline(), selectedType(), from, to));
            created.setMaxParticipants(parseMax());
            return null;
        }
        boolean dropsPairs = existing.isDoubles() && selectedDiscipline() == Discipline.SINGLES
                && !t.pairsIn(existing).isEmpty();
        if (dropsPairs && !confirmDropPairs(t, existing)) {
            return "Spielart nicht geändert.";
        }
        t.renameClass(existing, n);
        t.changeDiscipline(existing, selectedDiscipline());
        existing.setType(selectedType());
        existing.setBirthyearFrom(from);
        existing.setBirthyearTo(to);
        existing.setMaxParticipants(parseMax());
        t.markModified();
        created = existing;
        return null;
    }

    /** Fragt nach, ob beim Wechsel Doppel → Einzel die Paarungen aufgelöst werden dürfen. */
    private boolean confirmDropPairs(Tournament t, PlayingClass c) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "„" + c.getName() + "“ hat " + t.pairsIn(c).size() + " Doppelpaarungen. Beim Wechsel auf Einzel werden die "
                        + "Paarungen aufgelöst (die Teilnehmenden bleiben der Klasse zugeordnet). Fortfahren?",
                ButtonType.YES, ButtonType.NO);
        a.initOwner(getOwner());
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static Integer parseYear(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            int y = Integer.parseInt(s.trim());
            return y >= 1900 && y <= 2100 ? y : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseMax() {
        String v = maxField.getText();
        return v == null || v.isBlank() ? null : Integer.valueOf(v.trim());
    }

    private static TextField yearField() {
        TextField tf = new TextField();
        tf.setPrefColumnCount(6);
        tf.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,4}") ? c : null));
        return tf;
    }

    private static Node hint(TextField field, String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hint");
        HBox box = new HBox(8, field, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
