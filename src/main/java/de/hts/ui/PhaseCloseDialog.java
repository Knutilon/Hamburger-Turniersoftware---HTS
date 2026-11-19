/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.tournament.Plan;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
                  boolean koTotal, List<Integer> places, Plan.Kind plan, Tournament.PlanOptions planOptions,
                  int tierWidth, boolean tierMirrored) {
        Choice(boolean finish, TournamentMode kind, int qualifiers, int groupSize, int koPerGroup, int koRounds, int reserve,
               boolean koTotal) {
            this(finish, kind, qualifiers, groupSize, koPerGroup, koRounds, reserve, koTotal, null, null, null, 0, false);
        }

        /** 1.2: „Gruppen nach Leistungsstufen“. */
        boolean tiers() {
            return tierWidth > 0;
        }
    }

    /** 1.2: alle wählbaren Phasentypen (bisherige Modi, Leistungsstufen und die Platzierungspläne). */
    enum K {
        GROUPS("Gruppenphase"), KNOCKOUT("KO-System"), PLACEMENT("Gruppenphase mit Platzierungsspielen"),
        QUALIFICATION("Qualifikationsmodus"), TIERS("Gruppen nach Leistungsstufen"),
        MAIN_CONSOLATION("Haupt- und Trostrunde"), CONTINUED("Fortgesetztes K.-o.-System (WO 7.3)"),
        DOUBLE("Doppeltes K.-o.-System (WO 7.4)"), TOP24("TOP24-Platzierungsspiele"), TOP32("TOP32-Platzierungsspiele"),
        FINAL_BY_PLACE("Endrunde je Gruppenplatz");

        final String label;

        K(String label) {
            this.label = label;
        }

        TournamentMode mode() {
            return switch (this) {
                case GROUPS, TIERS -> TournamentMode.GROUPS;
                case KNOCKOUT -> TournamentMode.KNOCKOUT;
                case PLACEMENT -> TournamentMode.PLACEMENT;
                case QUALIFICATION -> TournamentMode.QUALIFICATION;
                default -> TournamentMode.PLAN;
            };
        }

        Plan.Kind plan() {
            return switch (this) {
                case MAIN_CONSOLATION -> Plan.Kind.MAIN_CONSOLATION;
                case CONTINUED -> Plan.Kind.CONTINUED;
                case DOUBLE -> Plan.Kind.DOUBLE;
                case TOP24 -> Plan.Kind.TOP24;
                case TOP32 -> Plan.Kind.TOP32;
                case FINAL_BY_PLACE -> Plan.Kind.FINAL_BY_PLACE;
                default -> null;
            };
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
            case PLAN -> "Platzierungen aus den Spielen des Plans.";
            default -> c.getGroups().size() > 1 ? "Gruppenergebnisse: alle Gruppenersten teilen sich Platz 1–" + c.getGroups().size()
                    + ", alle Zweiten die nächsten Plätze usw." : "Platzierungen nach der Gruppentabelle.";
        } + (n > 1 ? " Wer in früheren Phasen ausgeschieden ist, folgt dahinter." : ""));
        finishHint.getStyleClass().add("hint");
        finishHint.setWrapText(true);

        boolean canNext = t.canHaveNextPhase(c);
        ComboBox<K> kind = new ComboBox<>();
        List<K> kinds = new ArrayList<>();
        if (n == 1) {
            kinds.addAll(List.of(K.GROUPS, K.KNOCKOUT));
            if (t.canChoosePlacement(c)) kinds.add(K.PLACEMENT);
            if (c.getMode() == TournamentMode.GROUPS) kinds.add(K.QUALIFICATION);
            if (t.canChooseTiers(c)) kinds.add(K.TIERS);
        }
        for (Plan.Kind pk : t.planKindsAfter(c)) {
            for (K k : K.values()) if (k.plan() == pk) kinds.add(k);
        }
        kind.getItems().setAll(kinds);
        kind.setConverter(new StringConverter<>() {
            @Override
            public String toString(K m) {
                return m == null ? "" : m.label;
            }

            @Override
            public K fromString(String s) {
                return null;
            }
        });
        kind.setValue(kinds.contains(K.KNOCKOUT) ? K.KNOCKOUT : kinds.isEmpty() ? null : kinds.get(0));
        // 1.2: Einstellungen der Platzierungspläne und Leistungsstufen
        javafx.scene.control.TextField mainField = new javafx.scene.control.TextField("1,2");
        mainField.setPrefColumnCount(10);
        javafx.scene.control.TextField consField = new javafx.scene.control.TextField(maxGroupOf(c) >= 4 ? "3,4" : "3");
        consField.setPrefColumnCount(10);
        CheckBox thirdMain = new CheckBox("Spiel um Platz 3");
        CheckBox thirdCons = new CheckBox("Spiel um Platz 3");
        thirdMain.setSelected(true);
        Label mainLabel = new Label("Gruppenplätze Hauptrunde:");
        Label consLabel = new Label("Gruppenplätze Trostrunde:");
        javafx.scene.layout.HBox mainBox = new javafx.scene.layout.HBox(10, mainField, thirdMain);
        javafx.scene.layout.HBox consBox = new javafx.scene.layout.HBox(10, consField, thirdCons);
        mainBox.setAlignment(Pos.CENTER_LEFT);
        consBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Integer> cutoff = new ComboBox<>();
        cutoff.getItems().setAll(de.hts.tournament.Plan.CUTOFF_ALL, 32, 16, 8, 4);
        cutoff.setValue(de.hts.tournament.Plan.CUTOFF_ALL);
        cutoff.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer v) {
                if (v == null) return "";
                if (v == de.hts.tournament.Plan.CUTOFF_ALL) return "alle Plätze ausspielen";
                return "ab " + de.hts.tournament.KnockoutDraw.roundName(v) + " (Verlierer früherer Runden teilen sich den Platz)";
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });
        Label cutoffLabel = new Label("Weiterspielen:");
        Spinner<Integer> tierWidth = new Spinner<>(1, Math.max(1, maxGroupOf(c)), Math.min(2, Math.max(1, maxGroupOf(c))));
        tierWidth.setEditable(true);
        tierWidth.setPrefWidth(80);
        Label tierLabel = new Label("Gruppenplätze je Stufe:");
        ComboBox<String> tierMode = new ComboBox<>();
        tierMode.getItems().setAll("gespiegelt", "ausgelost");
        tierMode.setValue(tierMode.getItems().get(0));
        Label tierModeLabel = new Label("Gruppen bilden:");
        Label planError = new Label();
        planError.getStyleClass().add("field-error-text");
        planError.setWrapText(true);
        planError.setMaxWidth(460);
        planError.managedProperty().bind(planError.visibleProperty());
        planError.setVisible(false);
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
        Label advSpacer = new Label();
        advSpacer.managedProperty().bind(advSpacer.visibleProperty());
        opts.addRow(2, advTop, advSpacer);
        opts.addRow(3, advPlaces, placesField);
        opts.add(placesError, 1, 4);
        opts.addRow(5, gLabel, groupSize);
        for (Label l : new Label[]{qLabel, gLabel, kLabel, nLabel}) l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        opts.addRow(6, kLabel, koBox);
        opts.add(koError, 1, 7);
        opts.addRow(8, nLabel, reserve);
        // dieselben Zeilen wie ausgeblendete Felder anderer Phasentypen – so entstehen keine Lücken
        opts.add(mainLabel, 0, 6);
        opts.add(mainBox, 1, 6);
        opts.add(consLabel, 0, 7);
        opts.add(consBox, 1, 7);
        opts.add(cutoffLabel, 0, 8);
        opts.add(cutoff, 1, 8);
        opts.add(tierLabel, 0, 2);
        opts.add(tierWidth, 1, 2);
        opts.add(tierModeLabel, 0, 3);
        opts.add(tierMode, 1, 3);
        opts.add(planError, 1, 9);
        for (Label l : new Label[]{mainLabel, consLabel, cutoffLabel, tierLabel, tierModeLabel}) l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        opts.add(preview, 0, 10, 2, 1);
        ButtonType ok = new ButtonType("Phase abschließen", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        javafx.beans.property.BooleanProperty blocked = new javafx.beans.property.SimpleBooleanProperty(false);

        Runnable update = () -> {
            boolean nx = next.isSelected();
            opts.setDisable(!nx);
            K kk = kind.getValue();
            TournamentMode k = kk == null ? TournamentMode.NONE : kk.mode();
            boolean planKind = kk != null && kk.plan() != null, tiersKind = kk == K.TIERS;
            boolean mc = kk == K.MAIN_CONSOLATION, koLike = kk == K.CONTINUED || kk == K.DOUBLE;
            for (javafx.scene.Node x : new javafx.scene.Node[]{mainLabel, mainBox}) {
                x.setVisible(mc || koLike);
                x.setManaged(mc || koLike);
            }
            mainLabel.setText(mc ? "Gruppenplätze Hauptrunde:" : "Gruppenplätze im KO:");
            thirdMain.setVisible(mc);
            thirdMain.setManaged(mc);
            for (javafx.scene.Node x : new javafx.scene.Node[]{consLabel, consBox}) {
                x.setVisible(mc);
                x.setManaged(mc);
            }
            for (javafx.scene.Node x : new javafx.scene.Node[]{cutoffLabel, cutoff}) {
                x.setVisible(kk == K.CONTINUED);
                x.setManaged(kk == K.CONTINUED);
            }
            for (javafx.scene.Node x : new javafx.scene.Node[]{tierLabel, tierWidth, tierModeLabel, tierMode}) {
                x.setVisible(tiersKind);
                x.setManaged(tiersKind);
            }
            Tournament.PlanOptions po = planOptions(mainField, consField, thirdMain, thirdCons, cutoff);
            String planProblem = !planKind ? null
                    : (mc || koLike) && po == null ? "Bitte Gruppenplätze durch Komma getrennt angeben, z. B. 1,2."
                    : t.planProblem(c, kk.plan(), po == null ? Tournament.PlanOptions.of(List.of(1, 2)) : po);
            planError.setText(planProblem == null ? "" : "⚠ " + planProblem);
            planError.setVisible(nx && planProblem != null);
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
            advSpacer.setVisible(koPlacesPossible);
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
            qLabel.setVisible(!placement && !planKind && !tiersKind);
            qualifiers.setVisible(!placement && !planKind && !tiersKind);
            qLabel.setManaged(qLabel.isVisible());
            qualifiers.setManaged(qualifiers.isVisible());
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
            blocked.set(nx && (kk == null || qualiBlocked || placesProblem != null || planProblem != null));
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
            gLabel.setText(tiersKind ? "Gruppengröße je Stufe:" : "Gruppengröße:");
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
            String planText = planPreview(t, c, kk, po, tierWidth.getValue() == null ? 1 : tierWidth.getValue(),
                    groupSize.getValue() == null ? 4 : groupSize.getValue(), tierMode.getSelectionModel().getSelectedIndex() == 0);
            if (planText != null) {
                preview.setText(planText);
                return;
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
        for (javafx.beans.Observable ob : new javafx.beans.Observable[]{mainField.textProperty(), consField.textProperty(),
                thirdMain.selectedProperty(), thirdCons.selectedProperty(), cutoff.valueProperty(), tierWidth.valueProperty(),
                tierMode.valueProperty()}) {
            ob.addListener(o -> update.run());
        }

        VBox content = new VBox(10, finish, indent(finishHint), next);
        if (canNext) {
            content.getChildren().add(opts);
        } else {
            next.setDisable(true);
            Label why = new Label(n >= Tournament.MAX_PHASES ? "Es sind höchstens " + Tournament.MAX_PHASES + " Phasen möglich."
                    : n == 2 && c.getMode() == TournamentMode.GROUPS ? "Eine Phase 3 gibt es nur nach „Gruppen nach Leistungsstufen“."
                    : c.isBadeni() ? "Nach dem Schweizer System nach Badeni gibt es keine weitere Phase – nur das Endergebnis."
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
            K kk = kind.getValue();
            if (kk == K.TIERS) {
                return new Choice(false, TournamentMode.GROUPS, 0, groupSize.getValue(), 0, 0, 0, false, null, null, null,
                        tierWidth.getValue(), tierMode.getSelectionModel().getSelectedIndex() == 0);
            }
            if (kk.plan() != null) {
                return new Choice(false, TournamentMode.PLAN, 0, 0, 0, 0, 0, false, null, kk.plan(),
                        planOptions(mainField, consField, thirdMain, thirdCons, cutoff), 0, false);
            }
            boolean placesChosen = kk == K.KNOCKOUT && !swiss && advPlaces.isSelected();
            return new Choice(false, kk.mode(), qualifiers.getValue(), groupSize.getValue(), koPer.getValue(), Tournament.QUAL_WINS,
                    reserve.getValue(), "insgesamt".equals(koMode.getValue()),
                    placesChosen ? Tournament.parsePlaces(placesField.getText()) : null, null, null, 0, false);
        });
    }

    private static int maxGroupOf(PlayingClass c) {
        return c.getGroups().stream().mapToInt(List::size).max().orElse(2);
    }

    private static Tournament.PlanOptions planOptions(javafx.scene.control.TextField main, javafx.scene.control.TextField cons,
                                                      CheckBox thirdMain, CheckBox thirdCons, ComboBox<Integer> cutoff) {
        List<Integer> m = Tournament.parsePlaces(main.getText()), k = Tournament.parsePlaces(cons.getText());
        if (m == null) return null;
        return new Tournament.PlanOptions(m, k == null ? List.of() : k, thirdMain.isSelected(), thirdCons.isSelected(),
                cutoff.getValue() == null ? de.hts.tournament.Plan.CUTOFF_ALL : cutoff.getValue());
    }

    /** Vorschau für Leistungsstufen und Platzierungspläne ({@code null} = andere Phasentypen). */
    private static String planPreview(Tournament t, PlayingClass c, K k, Tournament.PlanOptions po, int tierWidth, int groupSize,
                                      boolean mirrored) {
        if (k == null) return null;
        int groups = c.getGroups().size(), all = c.getGroups().stream().mapToInt(List::size).sum();
        return switch (k) {
            case TIERS -> "→ Stufen aus je " + tierWidth + (tierWidth == 1 ? " Gruppenplatz" : " Gruppenplätzen") + " (Stufe 1 = Plätze 1–"
                    + tierWidth + ", Stufe 2 = Plätze " + (tierWidth + 1) + "–" + (2 * tierWidth) + " …); je Stufe Gruppen mit etwa "
                    + groupSize + " Teilnehmenden, " + (mirrored ? "gespiegelt aus den Vorrundengruppen (A mit "
                    + de.hts.tournament.GroupDraw.groupName(Math.max(0, groups - 1)) + ", B mit "
                    + de.hts.tournament.GroupDraw.groupName(Math.max(0, groups - 2)) + " …)" : "ausgelost")
                    + ". Ergebnisse werden nicht übernommen; die Gruppennamen laufen weiter (ab "
                    + t.groupLabel(c, groups) + "). Eine Stufe steht im Endergebnis vor der nächsten. Danach ist eine "
                    + "Phase 3 „Endrunde je Gruppenplatz“ möglich.";
            case MAIN_CONSOLATION -> "→ Die Hauptrunde (KO) spielt um Platz 1, die Trostrunde (KO) eigens um Platz 1 der Trostrunde. "
                    + "Zwei Reiter und zwei getrennte Endergebnisse; im click-TT-Export zählt die Trostrunde hinter der Hauptrunde "
                    + "weiter (z. B. Sieger der Trostrunde = 17., wenn 16 in der Hauptrunde spielen). Wer in keiner der beiden "
                    + "spielt, behält seinen Gruppenplatz dahinter.";
            case CONTINUED -> "→ KO-System, in dem auch die Verlierer weiterspielen (WO 7.3): Verlierer des Halbfinales spielen um "
                    + "Platz 3, Verlierer des Viertelfinales in einem eigenen Baum um die Plätze 5–8 usw. – alle Bäume "
                    + "untereinander in einem Reiter.";
            case DOUBLE -> "→ Doppeltes K.-o.-System (WO 7.4): wer in der Siegerrunde verliert, spielt in der Trostrunde weiter "
                    + "(gekreuzt, damit sich Begegnungen nicht wiederholen); erst die zweite Niederlage scheidet aus. Gewinnt im "
                    + "Finale die Siegerin/der Sieger der Trostrunde, folgt ein Stichkampf.";
            case TOP24 -> "→ 24 Plätze nach dem DTTB-Muster (4 Gruppen à 6): Gruppenerste und -zweite spielen über Kreuz um die "
                    + "Plätze 1–4 bzw. in Runde 2, die übrigen ebenso – alle Plätze werden ausgespielt. Fehlende Gruppenplätze "
                    + "sind Freilose; weitere Gruppen bzw. Plätze ab 7 folgen dahinter (" + all + " Teilnehmende).";
            case TOP32 -> "→ 32 Plätze nach dem DTTB-Muster (4 Gruppen à 8) – alle Plätze werden ausgespielt. Fehlende Gruppenplätze "
                    + "sind Freilose; weitere Gruppen bzw. Plätze ab 9 folgen dahinter (" + all + " Teilnehmende).";
            case FINAL_BY_PLACE -> "→ Je Leistungsstufe spielen die Gleichplatzierten der Gruppen ihre Plätze aus: Halbfinale "
                    + "(Gruppe 1 gegen 4, 2 gegen 3), danach Finale und Spiel um Platz 3 – so bekommt jede Person einen eigenen Platz.";
            default -> null;
        };
    }

    private static VBox indent(Label l) {
        VBox v = new VBox(l);
        v.setPadding(new Insets(0, 0, 0, 26));
        return v;
    }
}
