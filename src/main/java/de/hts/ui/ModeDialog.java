package de.hts.ui;

import de.hts.tournament.GroupDraw;
import de.hts.tournament.GroupStanding;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.SwissSystem;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Random;

/**
 * „Modus festlegen“ – sobald die Teilnehmenden der Klasse feststehen: Modus und Optionen für Phase 1
 * (Setzung, Gruppen, Vereinsgleichheit, Gleichstand, Runden) und gleich auslosen.
 */
final class ModeDialog extends Dialog<Boolean> {

    private enum Seed {
        TTR("nach Setzung (TTR)"), SNAKE("TTR Setzung (Schlange)"), POTS("TTR Setzung (Stärkegruppen)"),
        HEADS("Gruppenkopf gesetzt"), RANDOM("zufällig");
        final String label;

        Seed(String l) {
            label = l;
        }
    }

    ModeDialog(Window owner, Tournament t, PlayingClass c, Random random) {
        initOwner(owner);
        int n = t.entrantsOf(c).size();
        setTitle("Modus festlegen – " + c.getName());
        setHeaderText(c.getName() + ": " + n + (c.isDoubles() ? " Doppel" : " Teilnehmende")
                + ". Modus und Optionen für Phase 1 wählen – „Auslosen“ erstellt Phase 1.");

        ComboBox<TournamentMode> mode = new ComboBox<>();
        boolean dbl = c.isDoubles();
        boolean koOnly = dbl && n > Tournament.MAX_DOUBLES_GROUP;
        if (koOnly) mode.getItems().setAll(TournamentMode.KNOCKOUT);                     // Doppel > 5: nur KO-System
        else if (dbl) mode.getItems().setAll(TournamentMode.GROUPS, TournamentMode.KNOCKOUT); // kein Schweizer System
        else mode.getItems().setAll(TournamentMode.KNOCKOUT, TournamentMode.GROUPS, TournamentMode.SWISS);
        mode.setConverter(conv(m -> m == TournamentMode.GROUPS ? (dbl ? "Gruppenphase (eine Gruppe, jeder gegen jeden)"
                : "Gruppenphase (jeder gegen jeden)") : m.getLabel()));
        TournamentMode initial = c.getMode() == TournamentMode.NONE || c.getMode() == TournamentMode.PLACEMENT ? TournamentMode.GROUPS : c.getMode();
        mode.setValue(mode.getItems().contains(initial) ? initial : mode.getItems().get(0));
        mode.setDisable(koOnly);

        ComboBox<Seed> seed = new ComboBox<>();
        seed.setConverter(conv(s -> s.label));

        ToggleGroup by = new ToggleGroup();
        RadioButton bySize = new RadioButton("Gruppengröße:");
        RadioButton byCount = new RadioButton("Anzahl Gruppen:");
        bySize.setToggleGroup(by);
        byCount.setToggleGroup(by);
        (c.isGroupsBySize() ? bySize : byCount).setSelected(true);
        Spinner<Integer> size = new Spinner<>(2, Math.max(2, n), Math.min(Math.max(2, c.getGroupSize()), Math.max(2, n)));
        Spinner<Integer> count = new Spinner<>(1, Math.max(1, n / 2), Math.min(Math.max(1, c.getGroupCount()), Math.max(1, n / 2)));
        size.setEditable(true);
        count.setEditable(true);
        size.setPrefWidth(80);
        count.setPrefWidth(80);

        ToggleGroup tb = new ToggleGroup();
        RadioButton tbSets = new RadioButton("Satz- und Ballverhältnis");
        RadioButton tbH2h = new RadioButton("direkter Vergleich");
        tbSets.setToggleGroup(tb);
        tbH2h.setToggleGroup(tb);
        (c.getTiebreak() == GroupStanding.Tiebreak.HEAD_TO_HEAD ? tbH2h : tbSets).setSelected(true);

        Spinner<Integer> rounds = new Spinner<>(1, SwissSystem.maxRounds(Math.max(2, n)),
                c.getSwissRounds() > 0 ? Math.min(c.getSwissRounds(), SwissSystem.maxRounds(Math.max(2, n))) : SwissSystem.defaultRounds(n));
        rounds.setEditable(true);
        rounds.setPrefWidth(80);

        javafx.scene.control.ComboBox<Integer> sets = new javafx.scene.control.ComboBox<>();
        sets.getItems().setAll(3, 4);
        sets.setValue(t.rootOf(c).getWinningSets());
        sets.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer n) {
                return n == null ? "" : n + " Gewinnsätze (Best of " + (2 * n - 1) + ")";
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });

        CheckBox clubs = new CheckBox();          // Beschriftung steht links in der Beschriftungsspalte
        clubs.setTooltip(new javafx.scene.control.Tooltip("Vereinsgleichheit bei der Auslosung beachten"));
        clubs.setSelected(c.isAvoidSameClub());
        Label clubsHint = new Label();
        clubsHint.getStyleClass().add("hint");
        clubsHint.setWrapText(true);

        Label preview = new Label();
        preview.getStyleClass().add("criteria");
        preview.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Label lMode = new Label("Modus:"), lSeed = new Label("Setzung:"), lGroups = new Label("Gruppen:"),
                lTb = new Label("Bei gleichem Spielverhältnis:"), lRounds = new Label("Anzahl Runden:"),
                lSets = new Label("Gewinnsätze:"), lClubs = new Label("Vereinsgleichheit beachten:");
        for (Label l : new Label[]{lMode, lSeed, lGroups, lTb, lRounds, lSets, lClubs}) l.getStyleClass().add("class-name");
        // Auswahllisten einheitlich breit (an der längsten Option ausgerichtet)
        for (javafx.scene.control.ComboBox<?> cb : List.of(mode, seed, sets)) {
            cb.setPrefWidth(320);
            cb.setMinWidth(320);
            cb.setMaxWidth(320);
        }
        HBox groupsBox = new HBox(8, bySize, size, new Label("oder"), byCount, count);
        groupsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox tbBox = new HBox(12, tbSets, tbH2h);
        grid.addRow(0, lMode, mode);
        grid.addRow(1, lSeed, seed);
        grid.addRow(2, lGroups, groupsBox);
        grid.addRow(3, lClubs, clubs);
        grid.addRow(4, lTb, tbBox);
        grid.addRow(5, lRounds, rounds);
        grid.addRow(6, lSets, sets);
        VBox content = new VBox(12, grid, clubsHint, preview);
        content.setPadding(new Insets(10));
        content.setPrefWidth(640);
        getDialogPane().setContent(content);

        boolean[] busy = {false};
        Runnable update = () -> {
            if (busy[0]) return;
            busy[0] = true;
            TournamentMode m = mode.getValue();
            boolean groups = m == TournamentMode.GROUPS, swiss = m == TournamentMode.SWISS;
            List<Seed> options = groups ? List.of(Seed.SNAKE, Seed.POTS, Seed.HEADS, Seed.RANDOM) : List.of(Seed.TTR, Seed.RANDOM);
            Seed cur = seed.getValue();
            if (!seed.getItems().equals(options)) {
                seed.getItems().setAll(options);
                if (cur == null) {
                    boolean seededBefore = !c.isDrawn() || c.isDrawSeeded();
                    cur = !seededBefore ? Seed.RANDOM : groups ? (c.getGroupSeeding() == GroupDraw.Seeding.POTS ? Seed.POTS
                            : c.getGroupSeeding() == GroupDraw.Seeding.HEADS ? Seed.HEADS : Seed.SNAKE) : Seed.TTR;
                } else if (!options.contains(cur)) {
                    cur = cur == Seed.RANDOM ? Seed.RANDOM : options.get(0);
                }
                seed.setValue(cur);
            }
            for (javafx.scene.Node x : new javafx.scene.Node[]{lGroups, groupsBox, lTb, tbBox}) {
                x.setVisible(groups);
                x.setManaged(groups);
            }
            if (dbl) {               // Doppel: eine Gruppe, keine Gruppeneinstellungen, keine Vereinsgleichheit
                lGroups.setVisible(false);
                lGroups.setManaged(false);
                groupsBox.setVisible(false);
                groupsBox.setManaged(false);
                byCount.setSelected(true);
                count.getValueFactory().setValue(1);
                clubs.setVisible(false);
                clubs.setManaged(false);
                lClubs.setVisible(false);
                lClubs.setManaged(false);
                clubsHint.setVisible(false);
                clubsHint.setManaged(false);
            }
            lRounds.setVisible(swiss);
            lRounds.setManaged(swiss);
            rounds.setVisible(swiss);
            rounds.setManaged(swiss);
            size.setDisable(!bySize.isSelected());
            count.setDisable(bySize.isSelected());
            if (!dbl) {              // Schweizer System: Vereine spielen keine Rolle → Zeile ausblenden
                for (javafx.scene.Node x : new javafx.scene.Node[]{lClubs, clubs}) {
                    x.setVisible(!swiss);
                    x.setManaged(!swiss);
                }
            }
            Seed s = seed.getValue();
            clubsHint.setText(swiss ? "Im Schweizer System wird nach der Tabelle gepaart – Vereine spielen dabei keine Rolle."
                    : m == TournamentMode.KNOCKOUT ? "KO-System: Vereinskameraden treffen nicht schon in der 1. Runde aufeinander – "
                    + "der schwächer Gesetzte tauscht mit dem Nächstplatzierten, solange das möglich ist."
                    : s == Seed.HEADS ? "Gruppenkopf gesetzt: TTR-Nr. 1 in Gruppe A, Nr. 2 in B usw.; danach wie Stärkegruppen – möglichst "
                    + "jemand ohne Vereinsgleichheit aus derselben Stärkegruppe. Gruppenköpfe werden nie getauscht."
                    : s == Seed.POTS ? "Stärkegruppen: aus derselben Stärkegruppe wird möglichst jemand ohne Vereinsgleichheit gezogen; "
                    + "geht das nicht, wird nach Stärkegruppe verteilt."
                    : "Schlange: der nächste Platz geht an den Nächststärkeren ohne Vereinsgleichheit in dieser Gruppe; "
                    + "geht das nicht mehr, wird normal nach der Schlange verteilt.");
            // Vorschau
            String text;
            if (groups && dbl) {
                text = "→ eine Gruppe mit " + n + " Doppeln, jeder gegen jeden · " + (n * (n - 1) / 2) + " Spiele";
            } else if (groups) {
                int g = bySize.isSelected() ? GroupDraw.groupCountForSize(Math.max(2, n), size.getValue()) : count.getValue();
                String problem = GroupDraw.validate(n, g);
                if (problem != null) {
                    text = "⚠ " + problem;
                } else {
                    int big = n % g == 0 ? g : n % g, bigSize = GroupDraw.maxGroupSize(n, g), small = g - big;
                    String sizes = n % g == 0 ? g + " Gruppen à " + bigSize : big + " × " + bigSize + " und " + small + " × " + (bigSize - 1);
                    int games = big * bigSize * (bigSize - 1) / 2 + (n % g == 0 ? 0 : small * (bigSize - 1) * (bigSize - 2) / 2);
                    text = "→ " + sizes + " · " + games + " Gruppenspiele";
                    if (s == Seed.POTS || s == Seed.HEADS) {
                        int pots = n / g;
                        StringBuilder sb = new StringBuilder(" · Stärkegruppen: ");
                        for (int k = 0; k < pots; k++) {
                            int from = k * g + 1, to = k == pots - 1 ? n : (k + 1) * g;
                            sb.append(k > 0 ? ", " : "").append(from).append("–").append(to);
                        }
                        text += sb;
                    }
                }
            } else if (swiss) {
                text = "→ " + rounds.getValue() + " Runden à " + (n / 2) + " Spiele" + (n % 2 == 1 ? " + 1 Freilos" : "")
                        + " · Vorschlag " + SwissSystem.defaultRounds(n) + " Runden";
            } else {
                int fieldSize = de.hts.tournament.KnockoutDraw.bracketSize(n);
                text = "→ " + fieldSize + "er-Feld · " + (n - 1) + " Spiele" + (fieldSize > n ? " · " + (fieldSize - n) + " Freilose (an die Besten)" : "")
                        + (s == Seed.TTR ? " · Setzliste 1–16, 9–8, 5–12, 13–4, 3–14, 11–6, 7–10, 15–2 (entsprechend hochgerechnet)" : "");
            }
            preview.setText(koOnly ? text + "\nBei mehr als " + Tournament.MAX_DOUBLES_GROUP + " Doppeln ist Phase 1 immer ein KO-System." : text);
            busy[0] = false;
        };
        mode.valueProperty().addListener((o, a, b) -> update.run());
        seed.valueProperty().addListener((o, a, b) -> update.run());
        by.selectedToggleProperty().addListener((o, a, b) -> update.run());
        size.valueProperty().addListener((o, a, b) -> update.run());
        count.valueProperty().addListener((o, a, b) -> update.run());
        rounds.valueProperty().addListener((o, a, b) -> update.run());
        update.run();

        ButtonType ok = new ButtonType("Auslosen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            TournamentMode m = mode.getValue();
            Seed s = seed.getValue();
            c.setMode(m);
            c.setGroupsBySize(bySize.isSelected());
            c.setGroupSize(size.getValue());
            c.setGroupCount(count.getValue());
            c.setGroupSeeding(s == Seed.POTS ? GroupDraw.Seeding.POTS : s == Seed.HEADS ? GroupDraw.Seeding.HEADS : GroupDraw.Seeding.SNAKE);
            c.setAvoidSameClub(!dbl && clubs.isSelected());
            try {
                t.setWinningSets(c, sets.getValue() == null ? 3 : sets.getValue());
            } catch (IllegalStateException ex) {
                preview.setText("⚠ " + ex.getMessage());
                e.consume();
                return;
            }
            c.setTiebreak(tbH2h.isSelected() ? GroupStanding.Tiebreak.HEAD_TO_HEAD : GroupStanding.Tiebreak.SETS_POINTS);
            c.setSwissRounds(rounds.getValue());
            boolean seeded = s != Seed.RANDOM;
            try {
                if (m == TournamentMode.GROUPS) t.drawGroups(c, seeded, random);
                else if (m == TournamentMode.SWISS) t.drawSwiss(c, seeded, random);
                else t.drawKnockout(c, seeded, random);
                t.markModified();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                preview.setText("⚠ " + ex.getMessage());
                e.consume();
            }
        });
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok);
    }

    private static <T> StringConverter<T> conv(java.util.function.Function<T, String> f) {
        return new StringConverter<>() {
            @Override
            public String toString(T x) {
                return x == null ? "" : f.apply(x);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
