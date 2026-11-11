/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.player.Player;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Ergebnis einer Begegnung im 2er/3er Turnier: Aufstellung beider Teams wählen und je Partie den Sieger eintragen
 * (keine Sätze, keine Bälle). Es werden immer alle Partien gespielt.
 */
public final class TeamResultDialog extends Dialog<TeamResultDialog.Result> {

    /** Aufstellung (Personen-IDs nach Position) und Sieger je Partie (1 = Team A, 2 = Team B). */
    public record Result(List<String> lineupA, List<String> lineupB, List<Integer> winners) {
    }

    private final Tournament t;
    private final Match m;
    private final List<Tournament.Rubber> rubbers;
    private final List<ComboBox<Player>> posA = new ArrayList<>(), posB = new ArrayList<>();
    private final List<ToggleGroup> winners = new ArrayList<>();
    private final List<Label> namesA = new ArrayList<>(), namesB = new ArrayList<>(), hcA = new ArrayList<>(), hcB = new ArrayList<>();
    private final Label score = new Label();
    private final Label error = new Label();

    public TeamResultDialog(Window owner, Tournament t, Match m) {
        this.t = t;
        this.m = m;
        this.rubbers = t.rubbersOf(m);
        initOwner(owner);
        boolean correction = m.getStatus() == MatchStatus.DONE;
        setTitle(correction ? "Ergebnis korrigieren" : "Ergebnis eintragen");
        String teamA = t.entrantName(m, m.getA()), teamB = t.entrantName(m, m.getB());
        setHeaderText(m.getClassName() + " · " + m.getStage() + " · Spiel " + m.getNumber()
                + (m.getTable() != null ? " · " + t.tableName(m.getTable()) : "") + "\n" + teamA + "  –  " + teamB);

        // Aufstellung
        List<String> positions = t.teamPositions(t.findClass(m.getClassName()).orElseThrow());
        List<String> la = m.getLineupA().isEmpty() ? t.defaultLineup(m, m.getA()) : m.getLineupA();
        List<String> lb = m.getLineupB().isEmpty() ? t.defaultLineup(m, m.getB()) : m.getLineupB();
        GridPane lineup = new GridPane();
        lineup.setHgap(10);
        lineup.setVgap(6);
        Label head = new Label("Aufstellung");
        head.getStyleClass().add("round-title");
        lineup.add(head, 0, 0, 3, 1);
        Label ta = new Label("Team A: " + teamA), tb = new Label("Team B: " + teamB);
        ta.getStyleClass().add("class-name");
        tb.getStyleClass().add("class-name");
        lineup.add(ta, 1, 1);
        lineup.add(tb, 2, 1);
        for (int i = 0; i < positions.size(); i++) {
            Label pl = new Label("Position " + positions.get(i) + ":");
            ComboBox<Player> ca = playerBox(t.teamMembers(m.getA()), i < la.size() ? la.get(i) : null);
            ComboBox<Player> cb = playerBox(t.teamMembers(m.getB()), i < lb.size() ? lb.get(i) : null);
            posA.add(ca);
            posB.add(cb);
            lineup.addRow(i + 2, pl, ca, cb);
        }

        // Partien
        GridPane games = new GridPane();
        games.setHgap(10);
        games.setVgap(6);
        Label gh = new Label("Partien – Sieger je Partie (es werden immer alle Partien gespielt)");
        gh.getStyleClass().add("round-title");
        games.add(gh, 0, 0, 6, 1);
        List<int[]> prior = correction ? m.getPoints() : List.of();
        for (int k = 0; k < rubbers.size(); k++) {
            Tournament.Rubber r = rubbers.get(k);
            Label nr = new Label("Spiel " + (k + 1) + " · " + r.label());
            nr.getStyleClass().add("class-name");
            Label a = new Label(), b = new Label(), ha = new Label(), hb = new Label();
            for (Label x : new Label[]{ha, hb}) x.getStyleClass().add("handicap-badge");
            a.getStyleClass().add("match-name");
            b.getStyleClass().add("match-name");
            namesA.add(a);
            namesB.add(b);
            hcA.add(ha);
            hcB.add(hb);
            ToggleGroup g = new ToggleGroup();
            ToggleButton wa = new ToggleButton("A gewinnt"), wb = new ToggleButton("B gewinnt");
            wa.setUserData(1);
            wb.setUserData(2);
            wa.setToggleGroup(g);
            wb.setToggleGroup(g);
            wa.getStyleClass().add("pill");
            wb.getStyleClass().add("pill");
            if (k < prior.size()) (prior.get(k)[0] > prior.get(k)[1] ? wa : wb).setSelected(true);
            g.selectedToggleProperty().addListener((o, x, y) -> updateScore());
            winners.add(g);
            Label vs = new Label("–");
            vs.getStyleClass().add("hint");
            games.addRow(k + 1, nr, new HBox(5, a, ha), vs, new HBox(5, b, hb), new HBox(6, wa, wb));
        }
        for (ComboBox<Player> c : posA) c.valueProperty().addListener((o, x, y) -> updateNames());
        for (ComboBox<Player> c : posB) c.valueProperty().addListener((o, x, y) -> updateNames());
        updateNames();
        updateScore();

        score.getStyleClass().add("class-name");
        error.getStyleClass().add("error");
        error.setWrapText(true);
        VBox content = new VBox(14, lineup, games, score, error);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);

        ButtonType ok = new ButtonType(correction ? "Korrektur speichern" : "Ergebnis speichern", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String msg = check();
            if (msg != null) {
                error.setText(msg);
                e.consume();
            }
        });
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == ok ? new Result(ids(posA), ids(posB), selectedWinners()) : null);
    }

    private static ComboBox<Player> playerBox(List<Player> members, String selectedId) {
        ComboBox<Player> cb = new ComboBox<>();
        cb.getItems().setAll(members);
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(Player p) {
                return p == null ? "" : p.getDisplayName() + (p.getTtr() != null ? "  (" + p.getTtr() + ")" : "");
            }

            @Override
            public Player fromString(String s) {
                return null;
            }
        });
        for (Player p : members) if (p.getId().equals(selectedId)) cb.setValue(p);
        cb.setPrefWidth(240);
        return cb;
    }

    private static List<String> ids(List<ComboBox<Player>> boxes) {
        List<String> out = new ArrayList<>();
        for (ComboBox<Player> b : boxes) out.add(b.getValue() == null ? null : b.getValue().getId());
        return out;
    }

    private List<Integer> selectedWinners() {
        List<Integer> out = new ArrayList<>();
        for (ToggleGroup g : winners) out.add(g.getSelectedToggle() == null ? null : (Integer) g.getSelectedToggle().getUserData());
        return out;
    }

    private String names(List<ComboBox<Player>> boxes, int[] pos) {
        List<String> n = new ArrayList<>();
        for (int i : pos) {
            Player p = i < boxes.size() ? boxes.get(i).getValue() : null;
            n.add(p == null ? "?" : p.getLastname());
        }
        return String.join(" / ", n);
    }

    private void updateNames() {
        List<String> la = ids(posA), lb = ids(posB);
        boolean complete = !la.contains(null) && !lb.contains(null);
        for (int k = 0; k < rubbers.size(); k++) {
            Tournament.Rubber r = rubbers.get(k);
            namesA.get(k).setText(names(posA, r.posA()));
            namesB.get(k).setText(names(posB, r.posB()));
            int h = complete ? t.rubberHandicap(m, r, la, lb) : 0;
            hcA.get(k).setText(h > 0 ? "+" + h : "");
            hcB.get(k).setText(h < 0 ? "+" + (-h) : "");
            hcA.get(k).setVisible(h > 0);
            hcB.get(k).setVisible(h < 0);
        }
    }

    private void updateScore() {
        int a = 0, b = 0;
        for (Integer w : selectedWinners()) {
            if (w == null) continue;
            if (w == 1) a++;
            else b++;
        }
        score.setText("Stand der Begegnung: " + a + ":" + b + (a + b < rubbers.size() ? "  (noch " + (rubbers.size() - a - b) + " Partien offen)" : ""));
    }

    private String check() {
        if (ids(posA).contains(null) || ids(posB).contains(null)) return "Bitte die Aufstellung beider Teams vollständig wählen.";
        if (new java.util.HashSet<>(ids(posA)).size() != posA.size() || new java.util.HashSet<>(ids(posB)).size() != posB.size()) {
            return "Jede Person darf in der Aufstellung nur einmal stehen.";
        }
        if (selectedWinners().contains(null)) return "Bitte für jede Partie den Sieger angeben.";
        return null;
    }
}
