package de.hts.ui;

import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.PlayingClass;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Teilnehmende anlegen oder bearbeiten.
 * Ergebnis: {@code true}, wenn übernommen wurde; die Werte stehen dann im übergebenen {@link Player}.
 */
public final class ParticipantDialog extends Dialog<Boolean> {

    private final ComboBox<PlayingClass> classChoice = new ComboBox<>();

    /**
     * @param singlesClasses Einzelklassen zur Auswahl (nur beim Anlegen); leer = keine Auswahl anzeigen
     */
    public ParticipantDialog(Window owner, Player p, boolean isNew, List<PlayingClass> singlesClasses) {
        this(owner, p, isNew, singlesClasses, false);
    }

    /**
     * @param strict Erfassung in einer Klasse: ohne Geburtsdatum; Vorname, Nachname, Geschlecht,
     *               Jahrgang, Verein und TTR sind Pflicht
     */
    public ParticipantDialog(Window owner, Player p, boolean isNew, List<PlayingClass> singlesClasses, boolean strict) {
        this(owner, p, isNew, singlesClasses, strict, false);
    }

    /** @param handicap Vorgabeturnier: Jahrgang und Verein sind keine Pflicht */
    public ParticipantDialog(Window owner, Player p, boolean isNew, List<PlayingClass> singlesClasses, boolean strict, boolean handicap) {
        initOwner(owner);
        setTitle(isNew ? "Teilnehmende hinzufügen" : "Teilnehmende bearbeiten");
        setHeaderText(isNew ? "Neue Teilnehmerin / neuer Teilnehmer" : p.getDisplayName());

        TextField firstname = new TextField(nz(p.getFirstname()));
        TextField lastname = new TextField(nz(p.getLastname()));
        ComboBox<Sex> sex = new ComboBox<>();
        sex.getItems().addAll(Sex.FEMALE, Sex.MALE);
        if (!strict) sex.getItems().add(Sex.UNKNOWN);
        sex.setConverter(new StringConverter<>() {
            @Override
            public String toString(Sex s) {
                if (s == null || s == Sex.UNKNOWN) return "keine Angabe";
                return s == Sex.MALE ? "männlich" : "weiblich";
            }

            @Override
            public Sex fromString(String s) {
                return null;
            }
        });
        sex.setValue(strict && p.getSex() == Sex.UNKNOWN ? null : p.getSex());
        sex.setPromptText("bitte wählen");
        TextField birthyear = numberField(4, p.getBirthdate() == null && p.getBirthyear() != null ? p.getBirthyear().toString() : "");
        birthyear.setPromptText("JJJJ");
        DatePicker birthdate = new DatePicker(p.getBirthdate());
        birthdate.setConverter(new LocalDateStringConverter(TournamentDialog.DATE, TournamentDialog.DATE));
        birthdate.setPromptText("TT.MM.JJJJ (optional)");
        TextField club = new TextField(nz(p.getClubName()));
        TextField ttr = numberField(4, p.getTtr() == null ? "" : p.getTtr().toString());
        TextField licence = new TextField(nz(p.getLicenceNr()));
        Label error = new Label();
        error.getStyleClass().add("error");
        firstname.setPrefColumnCount(20);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        int r = 0;
        String req = strict ? " *" : "";
        grid.addRow(r++, new Label("Vorname:" + req), firstname);
        grid.addRow(r++, new Label("Nachname:" + req), lastname);
        grid.addRow(r++, new Label("Geschlecht:" + req), sex);
        if (strict) {
            grid.addRow(r++, new Label("Jahrgang: *"), birthyear);
        } else {
            grid.addRow(r++, new Label("Geburtsdatum:"), birthdate);
            grid.addRow(r++, new Label("oder Jahrgang:"), birthyear);
        }
        grid.addRow(r++, new Label("Verein:" + req), club);
        grid.addRow(r++, new Label("TTR:" + req), ttr);
        grid.addRow(r++, new Label("Lizenz-Nr.:"), licence);
        if (strict) {
            Label hint = new Label("* Pflichtfeld");
            hint.getStyleClass().add("hint");
            grid.add(hint, 1, r++);
        }
        if (isNew && singlesClasses != null) {
            List<PlayingClass> items = new ArrayList<>();
            items.add(null);
            items.addAll(singlesClasses);
            classChoice.getItems().setAll(items);
            classChoice.setConverter(new StringConverter<>() {
                @Override
                public String toString(PlayingClass c) {
                    return c == null ? (singlesClasses.isEmpty() ? "(noch keine Einzelklasse angelegt)" : "– keine –") : c.getName();
                }

                @Override
                public PlayingClass fromString(String s) {
                    return null;
                }
            });
            classChoice.setValue(null);
            classChoice.setDisable(singlesClasses.isEmpty());
            grid.addRow(r++, new Label("Einzelklasse:"), classChoice);
        }
        grid.add(error, 0, r, 2, 1);
        getDialogPane().setContent(grid);

        ButtonType ok = new ButtonType(isNew ? "Hinzufügen" : "Übernehmen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try {
                birthdate.setValue(birthdate.getConverter().fromString(birthdate.getEditor().getText()));
            } catch (RuntimeException ex) {
                error.setText("Bitte das Geburtsdatum als TT.MM.JJJJ eingeben.");
                e.consume();
                return;
            }
            if (strict) {
                String missing = missing(firstname, "Vorname") + missing(lastname, "Nachname")
                        + (sex.getValue() == null || sex.getValue() == Sex.UNKNOWN ? ", Geschlecht" : "")
                        + (handicap ? "" : missing(birthyear, "Jahrgang") + missing(club, "Verein")) + missing(ttr, "TTR");
                if (!missing.isEmpty()) {
                    error.setText("Bitte ausfüllen: " + missing.substring(2) + ".");
                    e.consume();
                    return;
                }
            }
            if (lastname.getText().isBlank() && firstname.getText().isBlank()) {
                error.setText("Bitte mindestens Vor- oder Nachnamen eingeben.");
                e.consume();
            } else if (!birthyear.getText().isBlank() && birthyear.getText().length() != 4) {
                error.setText("Der Jahrgang muss vierstellig sein.");
                e.consume();
            }
        });
        DialogSizing.fit(this);
        setResultConverter(bt -> {
            if (bt != ok) return false;
            p.setFirstname(blankToNull(firstname.getText()));
            p.setLastname(blankToNull(lastname.getText()));
            p.setSex(sex.getValue());
            p.setBirthdate(strict ? null : birthdate.getValue());
            if (!strict && birthdate.getValue() != null) {
                p.setBirthyear(birthdate.getValue().getYear());
            } else {
                p.setBirthyear(birthyear.getText().isBlank() ? null : Integer.valueOf(birthyear.getText()));
            }
            p.setClubName(blankToNull(club.getText()));
            p.setTtr(ttr.getText().isBlank() ? null : Integer.valueOf(ttr.getText()));
            p.setLicenceNr(blankToNull(licence.getText()));
            return true;
        });
        setOnShown(e -> (isNew ? firstname : lastname).requestFocus());
    }

    /** Beim Anlegen gewählte Einzelklasse, oder {@code null}. */
    public PlayingClass getSelectedClass() {
        return classChoice.getValue();
    }

    private static String missing(TextField f, String name) {
        return f.getText() == null || f.getText().isBlank() ? ", " + name : "";
    }

    private static TextField numberField(int maxDigits, String value) {
        TextField tf = new TextField(value);
        tf.setPrefColumnCount(6);
        tf.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0," + maxDigits + "}") ? c : null));
        return tf;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
