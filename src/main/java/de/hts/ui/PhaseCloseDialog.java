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
    record Choice(boolean finish, TournamentMode kind, int qualifiers, int groupSize, int koPerGroup, int koRounds, int reserve) {
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
        Spinner<Integer> koPer = new Spinner<>(0, Math.max(1, maxGroup), Math.min(2, maxGroup));
        koPer.setEditable(true);
        koPer.setPrefWidth(80);
        Spinner<Integer> reserve = new Spinner<>(0, Math.max(1, maxGroup), 0);
        reserve.setEditable(true);
        reserve.setPrefWidth(80);
        Label nLabel = new Label("Nachrücker je Gruppe (ohne Spiel):");
        Spinner<Integer> rounds = new Spinner<>(1, 6, 1);
        rounds.setEditable(true);
        rounds.setPrefWidth(80);
        Label kLabel = new Label("In die KO-Qualifikation je Gruppe:");
        Label rLabel = new Label("Qualifiziert nach Siegen:");
        Label qLabel = new Label(swiss ? "Aufsteiger insgesamt:" : "Aufsteiger je Gruppe:");
        Label gLabel = new Label("Gruppengröße:");
        Label preview = new Label();
        preview.getStyleClass().add("criteria");
        preview.setWrapText(true);
        GridPane opts = new GridPane();
        opts.setHgap(10);
        opts.setVgap(8);
        opts.setPadding(new Insets(0, 0, 0, 26));
        opts.addRow(0, new Label("Phasentyp:"), kind);
        opts.addRow(1, qLabel, qualifiers);
        opts.addRow(2, gLabel, groupSize);
        opts.addRow(3, kLabel, koPer);
        opts.addRow(4, rLabel, rounds);
        opts.addRow(5, nLabel, reserve);
        opts.add(preview, 0, 6, 2, 1);

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
            qLabel.setVisible(!placement);
            qualifiers.setVisible(!placement);
            for (javafx.scene.Node x : new javafx.scene.Node[]{kLabel, koPer, rLabel, rounds, nLabel, reserve}) {
                x.setVisible(quali);
                x.setManaged(quali);
            }
            int koCount = 0;
            for (List<String> g : c.getGroups()) {
                int q0 = Math.min(g.size(), qualifiers.getValue() == null ? 0 : qualifiers.getValue());
                koCount += Math.min(g.size(), q0 + (koPer.getValue() == null ? 0 : koPer.getValue())) - q0;
            }
            int maxR = Math.max(1, Tournament.maxQualificationRounds(koCount));
            ((javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory) rounds.getValueFactory()).setMax(maxR);
            if (rounds.getValue() != null && rounds.getValue() > maxR) rounds.getValueFactory().setValue(maxR);
            rounds.setDisable(koCount < 2);
            gLabel.setVisible(k == TournamentMode.GROUPS);
            groupSize.setVisible(k == TournamentMode.GROUPS);
            int count = 0;
            if (placement) {
                count = c.getGroups().stream().mapToInt(List::size).sum();
            } else {
                for (List<String> g : c.getGroups()) count += Math.min(g.size(), qualifiers.getValue() == null ? 0 : qualifiers.getValue());
            }
            String text = switch (k == null ? TournamentMode.NONE : k) {
                case PLACEMENT -> "→ Die ersten beiden jeder Gruppe spielen eine Endgruppe um Platz 1–4 (das Spiel aus Phase " + n
                        + " wird übernommen), die Dritten um Platz 5/6, die Vierten um 7/8 …";
                case KNOCKOUT -> "→ " + count + " im KO-System nach Platzierung: Freilose an die besten "
                        + (swiss ? "Platzierten" : "Gruppenersten") + "; der Höchstplatzierte ohne Freilos spielt gegen den "
                        + "Niedrigstplatzierten noch nicht Zugeordneten" + (swiss ? "." : " (nicht aus seiner Gruppe); Spieler/innen "
                        + "derselben Gruppe treffen frühestens im Finale aufeinander.");
                case GROUPS -> "→ " + count + " in " + de.hts.tournament.GroupDraw.groupCountForSize(Math.max(1, count), groupSize.getValue())
                        + " Gruppe(n), Schlangensystem nach Platzierung.";
                case QUALIFICATION -> {
                    int q0 = qualifiers.getValue() == null ? 0 : qualifiers.getValue();
                    int kp = koPer.getValue() == null ? 0 : koPer.getValue();
                    int r = rounds.getValue() == null ? 1 : rounds.getValue();
                    StringBuilder sb = new StringBuilder("→ ");
                    sb.append(q0 > 0 ? "Plätze 1–" + q0 + " jeder Gruppe direkt qualifiziert (untereinander nach TTR)"
                            : "Niemand direkt qualifiziert");
                    if (kp > 0) {
                        sb.append("; Plätze ").append(q0 + 1).append("–").append(q0 + kp).append(" spielen ein KO-System (")
                                .append(koCount).append(" Spieler, frei zugelost – in Runde 1 spielt ein besser Platzierter gegen einen schlechter "
                                + "Platzierten einer anderen Gruppe; die Gewinner spielen weiter).");
                        if (koCount >= 2) {
                            sb.append(" Wer ").append(r).append(r == 1 ? " Spiel" : " Spiele")
                                    .append(" gewinnt, ist qualifiziert. Die Verlierer werden Nachrücker – wer später ausscheidet, "
                                            + "steht weiter oben; innerhalb einer Stufe zuerst der bessere Gruppenplatz, dann TTR. "
                                            + "Bei Bedarf lässt sich im Phasen-Reiter eine Zusatzrunde für die Verlierer der letzten "
                                            + "Runde zuschalten.");
                        }
                    }
                    int rv = reserve.getValue() == null ? 0 : reserve.getValue();
                    if (rv > 0) {
                        sb.append(" Plätze ").append(q0 + kp + 1).append("–").append(q0 + kp + rv)
                                .append(" jeder Gruppe sind Nachrücker ohne Spiel (zuerst nach Gruppenplatz, dann TTR).");
                    }
                    sb.append(" Alle anderen erhalten den Platz ihrer Gruppenplatzierung (gleiche Gruppenplätze teilen sich den Platz). ")
                            .append("Höchstens ").append(maxR).append(r == 1 && maxR == 1 ? " Sieg" : " Siege").append(" möglich.");
                    if (q0 == 0 && koCount < 2) sb.append("  ⚠ Ohne direkt Qualifizierte braucht die KO-Qualifikation mindestens 2 Spieler.");
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
        reserve.valueProperty().addListener((o, a, b) -> update.run());
        rounds.valueProperty().addListener((o, a, b) -> update.run());

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

        ButtonType ok = new ButtonType("Phase abschließen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        DialogSizing.fit(this);
        setResultConverter(bt -> {
            if (bt != ok) return null;
            if (finish.isSelected()) return new Choice(true, TournamentMode.NONE, 0, 0, 0, 0, 0);
            return new Choice(false, kind.getValue(), qualifiers.getValue(), groupSize.getValue(), koPer.getValue(), rounds.getValue(),
                    reserve.getValue());
        });
    }

    private static VBox indent(Label l) {
        VBox v = new VBox(l);
        v.setPadding(new Insets(0, 0, 0, 26));
        return v;
    }
}
