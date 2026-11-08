package de.hts.ui;

import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * „Phase abschließen“: entweder <b>Endergebnis</b> (Klasse beenden – KO: Platzierungen nach Runden, Gruppen:
 * Gruppenplätze) oder <b>neue Phase</b> (Gruppenphase, KO-System oder Gruppenphase mit Platzierungsspielen).
 */
final class PhaseCloseDialog extends Dialog<PhaseCloseDialog.Choice> {

    /** @param finish {@code true} = Endergebnis; sonst neue Phase {@code kind} */
    /** @param koTotal {@code true}: {@code koPerGroup} ist die Anzahl der Topplatzierten insgesamt (0.34) */
    record Choice(boolean finish, TournamentMode kind, int qualifiers, int groupSize, int koPerGroup, int koRounds, int reserve,
                  boolean koTotal, List<Integer> places) {
        Choice(boolean finish, TournamentMode kind, int qualifiers, int groupSize, int koPerGroup, int koRounds, int reserve,
               boolean koTotal) {
            this(finish, kind, qualifiers, groupSize, koPerGroup, koRounds, reserve, koTotal, null);
        }
    }

    PhaseCloseDialog(Window owner, Tournament t, PlayingClass c) {
        initOwner(owner);
        int n = t.phaseNumber(c);
        setTitle("Phase " + n + " abschließen");
        setHeaderText(t.rootOf(c).getName() + ": Alle Spiele von Phase " + n + " sind gespielt. Wie geht es weiter?");

        ToggleGroup tg = new ToggleGroup();
        RadioButton finish = new RadioButton("Endergebnis – die Klasse ist damit beendet");
        RadioButton next = new RadioButton("Neue Phase (Phase " + (n + 1) + ")");
        finish.setToggleGroup(tg);
        next.setToggleGroup(tg);
        finish.getStyleClass().add("class-name");
        next.getStyleClass().add("class-name");
        Label finishHint = new Label(switch (c.getMode()) {
            case KNOCKOUT -> "Platzierungen aus dem KO-System: Sieger/in, Finale, Halbfinale (zweimal 3. – bzw. 3./4. mit Spiel um Platz 3), Viertelfinale (5.–8.) …";
            case PLACEMENT -> "Platzierungen aus Endgruppe und Platzierungsspielen.";
            case SWISS -> "Platzierungen nach der Tabelle des Schweizer Systems.";
            default -> c.getGroups().size() > 1 ? "Gruppenergebnisse: alle Gruppenersten teilen sich Platz 1–" + c.getGroups().size()
                    + ", alle Zweiten die nächsten Plätze usw." : "Platzierungen nach der Gruppentabelle.";
        } + (n > 1 ? " Wer in früheren Phasen ausgeschieden ist, folgt dahinter." : ""));
        finishHint.getStyleClass().add("hint");
        finishHint.setWrapText(true);

        boolean canNext = t.canHaveNextPhase(c);
        ComboBox<TournamentMode> kind = new ComboBox<>();
        List<TournamentMode> kinds = new ArrayList<>(List.of(TournamentMode.GROUPS, TournamentMode.KNOCKOUT));
        if (t.canChoosePlacement(c)) kinds.add(TournamentMode.PLACEMENT);
        boolean qualiPossible = c.getMode() == TournamentMode.GROUPS && t.phaseNumber(c) == 1;
        if (qualiPossible) kinds.add(TournamentMode.QUALIFICATION);
        kind.getItems().setAll(kinds);
        kind.setConverter(new StringConverter<>() {
            @Override
            public String toString(TournamentMode m) {
                if (m == null) return "";
                return switch (m) {
                    case GROUPS -> "Gruppenphase";
                    case KNOCKOUT -> "KO-System";
                    case PLACEMENT -> "Gruppenphase mit Platzierungsspielen";
                    case QUALIFICATION -> "Qualifikationsmodus";
                    default -> m.getLabel();
                };
            }

            @Override
            public TournamentMode fromString(String s) {
                return null;
            }
        });
        kind.setValue(TournamentMode.KNOCKOUT);
        boolean swiss = c.getMode() == TournamentMode.SWISS;
        int maxGroup = c.getGroups().stream().mapToInt(List::size).max().orElse(2);
        Spinner<Integer> qualifiers = new Spinner<>(1, Math.max(1, maxGroup), Math.min(2, maxGroup));
        if (swiss) qualifiers.getValueFactory().setValue(Math.min(8, maxGroup));
        qualifiers.setEditable(true);
        qualifiers.setPrefWidth(80);
        Spinner<Integer> groupSize = new Spinner<>(2, 64, Math.max(2, c.getGroupSize()));
        groupSize.setEditable(true);
        groupSize.setPrefWidth(80);
        int allEntrants = c.getGroups().stream().mapToInt(List::size).sum();
        Spinner<Integer> koPer = new Spinner<>(0, Math.max(1, allEntrants), Math.min(2, maxGroup));
        koPer.setEditable(true);
        koPer.setPrefWidth(80);
        ComboBox<String> koMode = new ComboBox<>();
        koMode.getItems().setAll("je Gruppe", "insgesamt");
        koMode.setValue("je Gruppe");
        koMode.setTooltip(new Tooltip("je Gruppe: Plätze je Gruppe, z. B. 2 = Zweite und Dritte\n"
                + "insgesamt: Anzahl der Topplatzierten über alle Gruppen, z. B. 24 bei 16 Gruppen = alle Zweiten und die 8 besten "
                + "Dritten (Spiel-, Satz-, Punktverhältnis, Gleichstand: Los)"));
        javafx.scene.layout.HBox koBox = new javafx.scene.layout.HBox(8, koPer, koMode);
        koBox.setAlignment(Pos.CENTER_LEFT);
        Label koError = new Label();
        koError.getStyleClass().add("field-error-text");
        koError.setWrapText(true);
        koError.setMaxWidth(460);
        koError.managedProperty().bind(koError.visibleProperty());
        koError.setVisible(false);
        Spinner<Integer> reserve = new Spinner<>(0, Math.max(1, maxGroup), 0);
        reserve.setEditable(true);
        reserve.setPrefWidth(80);
        Label nLabel = new Label("Nachrücker je Gruppe (ohne Spiel):");
        Label kLabel = new Label("In die KO-Qualifikation je Gruppe:");
        Label qLabel = new Label(swiss ? "Aufsteiger insgesamt:" : "Aufsteiger je Gruppe:");
        Label gLabel = new Label("Gruppengröße:");
        // 0.38 (#61): KO-Folgephase – ENTWEDER die Besten je Gruppe ODER bestimmte Gruppenplätze
        javafx.scene.control.ToggleGroup advTg = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.RadioButton advTop = new javafx.scene.control.RadioButton("Die Besten je Gruppe:");
        javafx.scene.control.RadioButton advPlaces = new javafx.scene.control.RadioButton("Bestimmte Gruppenplätze:");
        advTop.setToggleGroup(advTg);
        advPlaces.setToggleGroup(advTg);
        advTop.setSelected(true);
        advTop.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        advPlaces.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        javafx.scene.control.TextField placesField = new javafx.scene.control.TextField();
        placesField.setPromptText("2,3,4");
        placesField.setPrefColumnCount(10);
        Label placesError = new Label();
        placesError.getStyleClass().add("field-error-text");
        placesError.setWrapText(true);
        placesError.setMaxWidth(460);
        placesError.managedProperty().bind(placesError.visibleProperty());
        placesError.setVisible(false);
        Label preview = new Label();
        preview.getStyleClass().add("criteria");
        preview.setWrapText(true);
        GridPane opts = new GridPane();
        opts.setHgap(10);
        opts.setVgap(8);
        opts.setPadding(new Insets(0, 0, 0, 26));
        opts.addRow(0, new Label("Phasentyp:"), kind);
        opts.addRow(1, qLabel, qualifiers);
        opts.addRow(2, advTop, new Label());
        opts.addRow(3, advPlaces, placesField);
        opts.add(placesError, 1, 4);
        opts.addRow(5, gLabel, groupSize);
        for (Label l : new Label[]{qLabel, gLabel, kLabel, nLabel}) l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        opts.addRow(6, kLabel, koBox);
        opts.add(koError, 1, 7);
        opts.addRow(8, nLabel, reserve);
        opts.add(preview, 0, 9, 2, 1);
        ButtonType ok = new ButtonType("Phase abschließen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        javafx.beans.property.BooleanProperty blocked = new javafx.beans.property.SimpleBooleanProperty(false);

        Runnable update = () -> {
            boolean nx = next.isSelected();
            opts.setDisable(!nx);
            TournamentMode k = kind.getValue();
            boolean placement = k == TournamentMode.PLACEMENT;
            boolean quali = k == TournamentMode.QUALIFICATION;
            var qf = (javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory) qualifiers.getValueFactory();
            qf.setMin(quali ? 0 : 1);
            if (qualifiers.getValue() != null && qualifiers.getValue() < qf.getMin()) qf.setValue(qf.getMin());
            qLabel.setText(quali ? "Direkt qualifiziert je Gruppe:" : swiss ? "Aufsteiger insgesamt:" : "Aufsteiger je Gruppe:");
            boolean koPlacesPossible = k == TournamentMode.KNOCKOUT && !swiss;
            boolean usePlaces = koPlacesPossible && advPlaces.isSelected();
            // KO: statt der Beschriftung „Aufsteiger je Gruppe“ die beiden Optionsfelder (entweder – oder)
            opts.getChildren().remove(advTop);
            opts.getChildren().remove(qLabel);
            if (koPlacesPossible) opts.add(advTop, 0, 1);
            else opts.add(qLabel, 0, 1);
            advPlaces.setVisible(koPlacesPossible);
            advPlaces.setManaged(koPlacesPossible);
            placesField.setVisible(koPlacesPossible);
            placesField.setManaged(koPlacesPossible);
            placesField.setDisable(!usePlaces);
            qualifiers.setDisable(usePlaces);
            List<Integer> placesList = Tournament.parsePlaces(placesField.getText());
            String placesProblem = !usePlaces ? null
                    : placesList == null ? "Bitte Gruppenplätze durch Komma getrennt angeben, z. B. 2,3,4."
                    : t.placesProblem(c, placesList);
            if (placesProblem != null) {
                if (!placesField.getStyleClass().contains("field-error")) placesField.getStyleClass().add("field-error");
            } else {
                placesField.getStyleClass().remove("field-error");
            }
            placesError.setText(placesProblem == null ? "" : "⚠ " + placesProblem);
            placesError.setVisible(placesProblem != null);
            qLabel.setVisible(!placement);
            qualifiers.setVisible(!placement);
            for (javafx.scene.Node x : new javafx.scene.Node[]{kLabel, koBox, nLabel, reserve}) {
                x.setVisible(quali);
                x.setManaged(quali);
            }
            boolean total = "insgesamt".equals(koMode.getValue());
            int q0Check = qualifiers.getValue() == null ? 0 : qualifiers.getValue();
            int kpValue = koPer.getValue() == null ? 0 : koPer.getValue();
            int available = Tournament.qualificationAvailable(c, q0Check);
            int koCount = total ? kpValue : Tournament.qualificationKoCount(c, q0Check, kpValue);
            String fieldProblem = total && kpValue > available
                    ? "Nach den direkt Qualifizierten stehen nur " + available + " Teilnehmende für die KO-Qualifikation zur Verfügung."
                    : Tournament.qualificationFieldProblem(koCount);
            if (fieldProblem == null && q0Check == 0 && koCount < 3) {
                fieldProblem = "Ohne direkt Qualifizierte braucht die KO-Qualifikation mindestens 3 Teilnehmende.";
            }
            boolean qualiBlocked = quali && fieldProblem != null;
            blocked.set(nx && (qualiBlocked || placesProblem != null));
            // 0.34: das Feld rot hervorheben und darunter erklären, warum es nicht weitergeht
            if (qualiBlocked) {
                if (!koPer.getStyleClass().contains("field-error")) koPer.getStyleClass().add("field-error");
            } else {
                koPer.getStyleClass().remove("field-error");
            }
            koError.setText(qualiBlocked ? "⚠ " + fieldProblem : "");
            koError.setVisible(quali && qualiBlocked);
            gLabel.setVisible(k == TournamentMode.GROUPS);
            groupSize.setVisible(k == TournamentMode.GROUPS);
            gLabel.setManaged(gLabel.isVisible());
            groupSize.setManaged(groupSize.isVisible());
            int count = 0;
            if (placement) {
                count = c.getGroups().stream().mapToInt(List::size).sum();
            } else if (usePlaces && placesList != null) {
                for (List<String> g : c.getGroups()) for (int pl : placesList) if (pl >= 1 && pl <= g.size()) count++;
            } else {
                for (List<String> g : c.getGroups()) count += Math.min(g.size(), qualifiers.getValue() == null ? 0 : qualifiers.getValue());
            }
            String text = switch (k == null ? TournamentMode.NONE : k) {
                case PLACEMENT -> "→ Die ersten beiden jeder Gruppe spielen eine Endgruppe um Platz 1–4 (das Spiel aus Phase " + n
                        + " wird übernommen), die Dritten um Platz 5/6, die Vierten um 7/8 …";
                case KNOCKOUT -> "→ " + count + " im KO-System nach Platzierung"
                        + (usePlaces && placesList != null ? " (Gruppenplätze " + String.join(", ", placesList.stream().map(String::valueOf).toList())
                        + "; Rangliste: Gruppenplatz, Spiel-, Satz-, Ballverhältnis, Los; die übrigen behalten ihren Gruppenplatz)" : "")
                        + ": Freilose an die besten "
                        + (swiss ? "Platzierten" : "Gruppenersten") + "; der Höchstplatzierte ohne Freilos spielt gegen den "
                        + "Niedrigstplatzierten noch nicht Zugeordneten" + (swiss ? "." : " (nicht aus seiner Gruppe); Spieler/innen "
                        + "derselben Gruppe treffen frühestens im Finale aufeinander.");
                case GROUPS -> "→ " + count + " in " + de.hts.tournament.GroupDraw.groupCountForSize(Math.max(1, count), groupSize.getValue())
                        + " Gruppe(n), Schlangensystem nach Platzierung.";
                case QUALIFICATION -> {
                    int q0 = qualifiers.getValue() == null ? 0 : qualifiers.getValue();
                    int kp = koPer.getValue() == null ? 0 : koPer.getValue();
                    boolean totalMode = "insgesamt".equals(koMode.getValue());
                    StringBuilder sb = new StringBuilder("→ ");
                    sb.append(q0 > 0 ? (q0 == 1 ? "Platz 1" : "Plätze 1–" + q0) + " jeder Gruppe direkt qualifiziert (untereinander nach TTR)"
                            : "Niemand direkt qualifiziert");
                    if (kp > 0 && koCount > 0) {
                        sb.append(totalMode ? "; die " + kp + " Topplatzierten nach den direkt Qualifizierten (über alle Gruppen nach "
                                + "Gruppenplatz, Spiel-, Satz-, Punktverhältnis, Gleichstand Los) spielen"
                                : kp == 1 ? "; Platz " + (q0 + 1) + " spielt" : "; Plätze " + (q0 + 1) + "–" + (q0 + kp) + " spielen")
                                .append(" eine KO-Qualifikation (")
                                .append(koCount).append(" Teilnehmende). Runde 1: besser gegen schlechter Platzierte (möglichst andere "
                                + "Gruppe), Freilos für den Stärksten nach der Gruppenphase; Runde 2 frei gelost. Wer 2 Spiele gewinnt "
                                + "(Freilos zählt als Sieg), ist qualifiziert. Nachrücker: Verlierer der Runde 2, dann Verlierer der "
                                + "Runde 1 (nach TTR). Im Phasen-Reiter lässt sich eine Zusatzrunde für die Verlierer der Runde 2 "
                                + "zuschalten – deren Gewinner sind ebenfalls qualifiziert.");
                    }
                    int rv = reserve.getValue() == null ? 0 : reserve.getValue();
                    if (rv > 0 && totalMode) {
                        sb.append(" Je Gruppe sind die ").append(rv == 1 ? "nächste Person" : "nächsten " + rv)
                                .append(" nach dem letzten KO-Teilnehmenden Nachrücker ohne Spiel (zuerst nach Gruppenplatz, dann TTR).");
                    } else if (rv > 0) {
                        sb.append(" Plätze ").append(q0 + kp + 1).append("–").append(q0 + kp + rv)
                                .append(" jeder Gruppe sind Nachrücker ohne Spiel (zuerst nach Gruppenplatz, dann TTR).");
                    }
                    sb.append(" Alle anderen erhalten den Platz ihrer Gruppenplatzierung (gleiche Gruppenplätze teilen sich den Platz).");
                    if (fieldProblem != null) sb.append("\n⚠ Phase 2 kann so nicht angelegt werden – siehe Hinweis am Feld „In die KO-Qualifikation“.");
                    yield sb.toString();
                }
                default -> "";
            };
            preview.setText(text + (count < 2 && !placement && !quali ? "  ⚠ Es müssen mindestens 2 weiterkommen." : ""));
        };
        tg.selectedToggleProperty().addListener((o, a, b) -> update.run());
        kind.valueProperty().addListener((o, a, b) -> update.run());
        qualifiers.valueProperty().addListener((o, a, b) -> update.run());
        groupSize.valueProperty().addListener((o, a, b) -> update.run());
        koPer.valueProperty().addListener((o, a, b) -> update.run());
        koMode.valueProperty().addListener((o, a, b) -> {
            boolean tot = "insgesamt".equals(b);
            kLabel.setText(tot ? "In die KO-Qualifikation insgesamt:" : "In die KO-Qualifikation je Gruppe:");
            update.run();
        });
        reserve.valueProperty().addListener((o, a, b) -> update.run());
        advTg.selectedToggleProperty().addListener((o, a, b) -> update.run());
        placesField.textProperty().addListener((o, a, b) -> update.run());

        VBox content = new VBox(10, finish, indent(finishHint), next);
        if (canNext) {
            content.getChildren().add(opts);
        } else {
            next.setDisable(true);
            Label why = new Label(n >= Tournament.MAX_PHASES ? "Es sind höchstens " + Tournament.MAX_PHASES + " Phasen möglich."
                    : c.getMode() == TournamentMode.SWISS ? "Nach dem Schweizer System gibt es keine weitere Phase – nur das Endergebnis."
                    : "Nach einem KO-System bzw. den Platzierungsspielen steht die Platzierung fest – keine weitere Phase.");
            why.getStyleClass().add("hint");
            why.setWrapText(true);
            content.getChildren().add(indent(why));
        }
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);
        content.setPrefWidth(560);
        getDialogPane().setContent(content);
        finish.setSelected(true);
        update.run();

        getDialogPane().lookupButton(ok).disableProperty().bind(blocked);
        DialogSizing.fit(this);
        setResultConverter(bt -> {
            if (bt != ok) return null;
            if (finish.isSelected()) return new Choice(true, TournamentMode.NONE, 0, 0, 0, 0, 0, false);
            boolean placesChosen = kind.getValue() == TournamentMode.KNOCKOUT && !swiss && advPlaces.isSelected();
            return new Choice(false, kind.getValue(), qualifiers.getValue(), groupSize.getValue(), koPer.getValue(), Tournament.QUAL_WINS,
                    reserve.getValue(), "insgesamt".equals(koMode.getValue()),
                    placesChosen ? Tournament.parsePlaces(placesField.getText()) : null);
        });
    }

    private static VBox indent(Label l) {
        VBox v = new VBox(l);
        v.setPadding(new Insets(0, 0, 0, 26));
        return v;
    }
}
