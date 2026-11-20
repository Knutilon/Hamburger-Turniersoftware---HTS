/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
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

    /** 1.2: Modi der Phase 1 – dazu Badeni (#95) und doppeltes KO (#99). */
    private enum Opt {
        KO("KO-System"), GROUPS("Gruppenphase (jeder gegen jeden)"), SWISS("Schweizer System"),
        BADENI("Schweizer System nach Badeni"), DOUBLE_KO("Doppeltes K.-o.-System"),
        EXEMPT("Gruppenphase mit Freistellung");
        final String label;

        Opt(String l) {
            label = l;
        }

        TournamentMode mode() {
            return switch (this) {
                case KO -> TournamentMode.KNOCKOUT;
                case GROUPS, EXEMPT -> TournamentMode.GROUPS;
                case SWISS, BADENI -> TournamentMode.SWISS;
                case DOUBLE_KO -> TournamentMode.PLAN;
            };
        }
    }

    ModeDialog(Window owner, Tournament t, PlayingClass c, Random random) {
        initOwner(owner);
        int n = t.entrantsOf(c).size();
        setTitle("Modus festlegen – " + c.getName());
        setHeaderText(c.getName() + ": " + n + (c.isTeam() ? " Teams" : c.isDoubles() ? " Doppel" : " Teilnehmende")
                + ". Modus und Optionen für Phase 1 wählen – „Auslosen“ erstellt Phase 1.");

        ComboBox<Opt> mode = new ComboBox<>();
        boolean dbl = c.isDoubles();
        boolean koOnly = c.isPureDoubles() && n > Tournament.MAX_DOUBLES_GROUP;
        if (koOnly) mode.getItems().setAll(Opt.KO, Opt.DOUBLE_KO);                      // Doppel > 5: nur KO-Systeme
        else if (dbl) mode.getItems().setAll(Opt.GROUPS, Opt.KO, Opt.DOUBLE_KO);        // kein Schweizer System
        else mode.getItems().setAll(Opt.KO, Opt.GROUPS, Opt.SWISS, Opt.BADENI, Opt.DOUBLE_KO);
        // 1.3 (#106): Freistellung nur im Einzel und erst, wenn neben S1–S4 mindestens 2 Gruppen à 3 möglich sind
        if (!dbl && n >= Tournament.EXEMPT_COUNT + 6) mode.getItems().add(Opt.EXEMPT);
        mode.setConverter(conv(m -> m == Opt.GROUPS && c.isPureDoubles() ? "Gruppenphase (eine Gruppe, jeder gegen jeden)" : m.label));
        Opt initial = switch (c.getMode()) {
            case KNOCKOUT -> Opt.KO;
            case SWISS -> c.isBadeni() ? Opt.BADENI : Opt.SWISS;
            case GROUPS -> c.getExempt().isEmpty() ? Opt.GROUPS : Opt.EXEMPT;
            case PLAN -> Opt.DOUBLE_KO;
            default -> Opt.GROUPS;
        };
        mode.setValue(mode.getItems().contains(initial) ? initial : mode.getItems().get(0));

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

        // 1.3 (#106): Freistellung – Setzplätze S1–S4 (Vorschlag: höchster TTR) und Plätze je Gruppe
        List<de.hts.tournament.Entrant> everyone = t.entrantsOf(c);
        java.util.Map<String, de.hts.tournament.Entrant> byId = new java.util.HashMap<>();
        for (de.hts.tournament.Entrant e : everyone) byId.put(e.id(), e);
        List<ComboBox<String>> slots = new java.util.ArrayList<>();
        javafx.scene.layout.FlowPane slotBox = new javafx.scene.layout.FlowPane(12, 8);
        slotBox.setPrefWrapLength(520);
        List<String> suggested = t.exemptSuggestion(c, new Random(c.getLotSeed() == 0 ? 1 : c.getLotSeed()));
        List<String> initialExempt = c.getExempt().size() == Tournament.EXEMPT_COUNT && byId.keySet().containsAll(c.getExempt())
                ? c.getExempt() : suggested;
        for (int k = 0; k < Tournament.EXEMPT_COUNT; k++) {
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().setAll(everyone.stream()
                    .sorted(java.util.Comparator.comparing((de.hts.tournament.Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed())
                    .map(de.hts.tournament.Entrant::id).toList());
            cb.setConverter(conv(id -> {
                de.hts.tournament.Entrant e = byId.get(id);
                return e == null ? "" : e.name() + (e.ttr() == null ? "" : " (" + e.ttr() + ")");
            }));
            if (k < initialExempt.size()) cb.setValue(initialExempt.get(k));
            Label sl = new Label("S" + (k + 1));
            sl.getStyleClass().add("class-name");
            HBox one = new HBox(6, sl, cb);
            one.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            slots.add(cb);
            slotBox.getChildren().add(one);
        }
        javafx.scene.control.Button resetExempt = new javafx.scene.control.Button("Auf TTR-Vorschlag zurücksetzen");
        resetExempt.setOnAction(e -> {
            for (int k = 0; k < slots.size(); k++) slots.get(k).setValue(k < suggested.size() ? suggested.get(k) : null);
        });
        VBox exemptBox = new VBox(8, slotBox, resetExempt);
        ToggleGroup advTg = new ToggleGroup();
        RadioButton adv2 = new RadioButton("Plätze 1–2");
        RadioButton adv3 = new RadioButton("Plätze 1–3");
        adv2.setToggleGroup(advTg);
        adv3.setToggleGroup(advTg);
        (c.getExemptPlaces() == 3 ? adv3 : adv2).setSelected(true);
        HBox advBox = new HBox(12, adv2, adv3);
        Label lExempt = new Label("Freigestellt (Setzplätze):"), lAdv = new Label("Weiter je Gruppe:");
        javafx.beans.property.BooleanProperty blocked = new javafx.beans.property.SimpleBooleanProperty(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Label lMode = new Label("Modus:"), lSeed = new Label("Setzung:"), lGroups = new Label("Gruppen:"),
                lTb = new Label("Bei gleichem Spielverhältnis:"), lRounds = new Label("Anzahl Runden:"),
                lSets = new Label("Gewinnsätze:"), lClubs = new Label("Vereinsgleichheit beachten:");
        for (Label l : new Label[]{lMode, lSeed, lGroups, lTb, lRounds, lSets, lClubs, lExempt, lAdv}) l.getStyleClass().add("class-name");
        lExempt.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
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
        grid.addRow(7, lExempt, exemptBox);
        grid.addRow(8, lAdv, advBox);
        javafx.scene.layout.GridPane.setValignment(lExempt, javafx.geometry.VPos.TOP);
        VBox content = new VBox(12, grid, clubsHint, preview);
        content.setPadding(new Insets(10));
        content.setPrefWidth(640);
        getDialogPane().setContent(content);

        boolean[] busy = {false};
        Runnable update = () -> {
            if (busy[0]) return;
            busy[0] = true;
            Opt o = mode.getValue();
            TournamentMode m = o.mode();
            boolean groups = m == TournamentMode.GROUPS, swiss = m == TournamentMode.SWISS, badeni = o == Opt.BADENI;
            boolean exemptMode = o == Opt.EXEMPT;
            for (javafx.scene.Node x : new javafx.scene.Node[]{lExempt, exemptBox, lAdv, advBox}) {
                x.setVisible(exemptMode);
                x.setManaged(exemptMode);
            }
            blocked.set(false);
            List<Seed> options = badeni ? List.of(Seed.TTR) : groups ? List.of(Seed.SNAKE, Seed.POTS, Seed.HEADS, Seed.RANDOM) : List.of(Seed.TTR, Seed.RANDOM);
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
            if (c.isTeam()) {        // 2er/3er Turnier: Wertung fest (Begegnungen, Partien, Los), keine Gewinnsätze
                for (javafx.scene.Node x : new javafx.scene.Node[]{lTb, tbBox, lSets, sets}) {
                    x.setVisible(false);
                    x.setManaged(false);
                }
            }
            if (c.isPureDoubles()) { // Doppel: eine Gruppe, keine Gruppeneinstellungen
                lGroups.setVisible(false);
                lGroups.setManaged(false);
                groupsBox.setVisible(false);
                groupsBox.setManaged(false);
                byCount.setSelected(true);
                count.getValueFactory().setValue(1);
            }
            if (dbl) {               // Doppel und Teams: keine Vereinsgleichheit
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
            clubsHint.setText(badeni ? "Badeni: Runde 1 nach TTR (Erster gegen Letzten), danach je Siegegruppe Erster gegen Letzten der "
                    + "Gruppe; ein Freilos bekommt der Beste der unteren Hälfte (niemand zweimal). Reihung: Siege, TTR, Sätze – ohne "
                    + "Buchholz. Vereine spielen keine Rolle."
                    : o == Opt.DOUBLE_KO ? "Doppeltes K.-o.-System: erst die zweite Niederlage scheidet aus. Vereinskameraden treffen "
                    + "nicht schon in der 1. Runde aufeinander (soweit möglich)."
                    : swiss ? "Im Schweizer System wird nach der Tabelle gepaart – Vereine spielen dabei keine Rolle."
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
            if (exemptMode) {
                int places = adv3.isSelected() ? 3 : 2, gp = n - Tournament.EXEMPT_COUNT;
                int g = bySize.isSelected() ? GroupDraw.groupCountForSize(Math.max(2, gp), size.getValue()) : count.getValue();
                List<String> chosen = slots.stream().map(ComboBox::getValue).toList();
                String problem = chosen.stream().anyMatch(java.util.Objects::isNull) || new java.util.HashSet<>(chosen).size() != Tournament.EXEMPT_COUNT
                        ? "Bitte für S1 bis S4 je eine andere Person wählen." : Tournament.exemptProblem(gp, g, places);
                if (problem != null) {
                    text = "⚠ " + problem + "\nPhase 1 lässt sich so nicht anlegen.";
                    blocked.set(true);
                } else {
                    int big = gp % g == 0 ? g : gp % g, bigSize = GroupDraw.maxGroupSize(gp, g), small = g - big;
                    String sizes = gp % g == 0 ? g + " Gruppen à " + bigSize : big + " × " + bigSize + " und " + small + " × " + (bigSize - 1);
                    int games = big * bigSize * (bigSize - 1) / 2 + (gp % g == 0 ? 0 : small * (bigSize - 1) * (bigSize - 2) / 2);
                    int field = Tournament.EXEMPT_COUNT + g * places;
                    text = "→ S1–S4 freigestellt, " + gp + " Teilnehmende in " + sizes + " · " + games + " Gruppenspiele · "
                            + Tournament.EXEMPT_COUNT + " + " + g + " × " + places + " = " + field + "er-Feld (KO ohne Freilose, ab "
                            + de.hts.tournament.KnockoutDraw.roundName(field) + ")"
                            + (g == 4 && places == 3 ? " · feste Paarungen: S1–D3, B1–D2, A1–C2, S4–C3, S3–A3, C1–B2, D1–A2, S2–B3"
                            : " · die Freigestellten spielen gegen die schlechtesten Gruppen" + (places == 3 ? "dritten" : "zweiten")
                            + ", die Gruppenersten untereinander nach Spiel-, Satz- und Punkteverhältnis");
                }
            } else if (groups && c.isPureDoubles()) {
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
            } else if (badeni) {
                text = "→ " + rounds.getValue() + " Runden à " + (n / 2) + " Spiele" + (n % 2 == 1 ? " + 1 Freilos" : "")
                        + " · mindestens " + de.hts.tournament.BadeniSystem.MIN_ROUNDS + " Runden empfohlen; weitere Runden lassen "
                        + "sich später anhängen. Keine Phase 2 – das Endergebnis ist die Tabelle.";
            } else if (o == Opt.DOUBLE_KO) {
                int fieldSize = de.hts.tournament.KnockoutDraw.bracketSize(n);
                text = "→ " + fieldSize + "er-Feld · Siegerrunde, Trostrunde, Finale (ggf. Stichkampf) · etwa " + (2 * n - 2) + " Spiele"
                        + (fieldSize > n ? " · " + (fieldSize - n) + " Freilose (an die Besten)" : "");
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
        mode.valueProperty().addListener((o, a, b) -> {
            if (b == Opt.BADENI && rounds.getValue() < de.hts.tournament.BadeniSystem.defaultRounds(n)) {
                rounds.getValueFactory().setValue(de.hts.tournament.BadeniSystem.defaultRounds(n));
            }
            update.run();
        });
        seed.valueProperty().addListener((o, a, b) -> update.run());
        by.selectedToggleProperty().addListener((o, a, b) -> update.run());
        size.valueProperty().addListener((o, a, b) -> update.run());
        count.valueProperty().addListener((o, a, b) -> update.run());
        rounds.valueProperty().addListener((o, a, b) -> update.run());
        advTg.selectedToggleProperty().addListener((o, a, b) -> update.run());
        for (ComboBox<String> cb : slots) cb.valueProperty().addListener((o, a, b) -> update.run());
        update.run();

        ButtonType ok = new ButtonType("Auslosen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            Opt o = mode.getValue();
            TournamentMode m = o.mode();
            Seed s = seed.getValue();
            if (m != TournamentMode.PLAN) c.setMode(m);
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
            c.setTiebreak(tbH2h.isSelected() && !c.isTeam() ? GroupStanding.Tiebreak.HEAD_TO_HEAD : GroupStanding.Tiebreak.SETS_POINTS);
            c.setSwissRounds(rounds.getValue());
            boolean seeded = s != Seed.RANDOM;
            try {
                if (o == Opt.GROUPS) t.drawGroups(c, seeded, random);
                else if (o == Opt.EXEMPT) t.drawGroupsWithExemption(c, slots.stream().map(ComboBox::getValue).toList(),
                        adv3.isSelected() ? 3 : 2, seeded, random);
                else if (o == Opt.BADENI) t.drawBadeni(c, random);
                else if (o == Opt.SWISS) t.drawSwiss(c, seeded, random);
                else if (o == Opt.DOUBLE_KO) t.drawDoubleKo(c, seeded, random);
                else t.drawKnockout(c, seeded, random);
                t.markModified();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                preview.setText("⚠ " + ex.getMessage());
                e.consume();
            }
        });
        getDialogPane().lookupButton(ok).disableProperty().bind(blocked);
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
