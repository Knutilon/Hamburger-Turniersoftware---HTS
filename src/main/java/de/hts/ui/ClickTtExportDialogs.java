/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.ui;

import de.hts.importer.clicktt.ClickTtResultExporter;
import de.hts.player.Player;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 1.0 (#79): Dialoge rund um den click-TT-Export – Rückfrage bei nachgetragenen Teilnehmenden, Zuordnungsfenster
 * (Spielerabgleich mit einer aktualisierten Teilnehmerliste) und Bericht mit den nicht exportierten Spielen.
 */
final class ClickTtExportDialogs {

    private ClickTtExportDialogs() {
    }

    /** Antwort der Rückfrage beim Klick auf „click-TT-Ergebnisdatei (XML)“. */
    enum Choice { ABORT, WITHOUT_MATCHING, MATCH }

    /** Anzahl abgeschlossener Spiele, an denen die Person beteiligt war (auch im Doppel). */
    static int matchesOf(Tournament t, Player p) {
        int n = 0;
        for (Match m : t.getMatches()) {
            if (m.getStatus() != MatchStatus.DONE || m.isCarried() || t.isInactiveMatch(m)) continue;
            if (ClickTtResultExporter.persons(t, m.getA()).contains(p.getId()) || ClickTtResultExporter.persons(t, m.getB()).contains(p.getId())) n++;
        }
        return n;
    }

    private static String classesOf(Tournament t, Player p) {
        return String.join(", ", t.rootClasses().stream().filter(c -> p.isInClass(c.getName()) || p.isEnteredFor(c.getName()))
                .map(PlayingClass::getName).toList());
    }

    /** Rückfrage (Entwurf 2): nachgetragene Teilnehmende ohne click-TT-Nummer. */
    static Choice askMissing(Window owner, Tournament t, List<Player> withoutId) {
        Dialog<Choice> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("click-TT-Ergebnisdatei");
        d.setHeaderText(withoutId.size() == 1 ? "1 Teilnehmende/r wurde von Hand nachgetragen"
                : withoutId.size() + " Teilnehmende wurden von Hand nachgetragen");
        int total = 0;
        List<String> lines = new ArrayList<>();
        for (Player p : withoutId) {
            int n = matchesOf(t, p);
            total += n;
            lines.add(p.getDisplayName() + " (" + classesOf(t, p) + ", " + Ui.spiele(n) + ")");
        }
        Label text = new Label("Für " + String.join(", ", lines) + " kennt HTS keine click-TT-Nummer – ohne Abgleich fehlen "
                + (total == 1 ? "dieses Spiel" : "diese " + total + " Spiele") + " in der Ergebnisdatei.");
        text.setWrapText(true);
        Label tip = new Label("Tipp: Die Personen in click-TT nachmelden, die Teilnehmerliste neu herunterladen und hier einspielen.");
        tip.getStyleClass().add("hint");
        tip.setWrapText(true);
        VBox box = new VBox(10, text, tip);
        box.setPadding(new Insets(6, 6, 6, 6));
        box.setPrefWidth(620);
        d.getDialogPane().setContent(box);
        ButtonType abort = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType plain = new ButtonType("Ohne Abgleich speichern …", ButtonBar.ButtonData.OTHER);
        ButtonType match = new ButtonType("Aktualisierte Teilnehmerliste einspielen …", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(abort, plain, match);
        DialogSizing.fit(d);
        d.setResultConverter(bt -> bt == match ? Choice.MATCH : bt == plain ? Choice.WITHOUT_MATCHING : Choice.ABORT);
        return d.showAndWait().orElse(Choice.ABORT);
    }

    // --- Zuordnungsfenster (Entwürfe 3 und 3b) -----------------------------------------------------------

    /**
     * Zuordnung nachgetragener Teilnehmender zu den neuen Personen der Datei. Ergebnis: von Hand erfasste Person →
     * Person der Datei ({@code null} = keine Zuordnung); leer = abgebrochen (nichts ändern).
     */
    static Optional<Map<Player, Player>> matchPlayers(Window owner, Tournament t, String fileName, Tournament.Matching matching) {
        return new MatchDialog(owner, t, fileName, matching).showAndWait();
    }

    private static final class MatchDialog extends Dialog<Map<Player, Player>> {
        private final Tournament t;
        private final String fileName;
        private final List<Player> manual = new ArrayList<>();
        private final Map<Player, Player> auto = new LinkedHashMap<>();
        /** von Hand gewählt: Person → Person der Datei bzw. NONE (keine Zuordnung) */
        private final Map<Player, Player> chosen = new LinkedHashMap<>();
        private final java.util.Set<Player> editing = new java.util.HashSet<>();
        private final List<Player> newPersons;
        private final VBox body = new VBox(14);
        private final Label head = new Label();
        private final FlowPane chips = new FlowPane(8, 6);
        private final Label openHint = new Label();
        private final ButtonType ok = new ButtonType("Übernehmen und Speichern", ButtonBar.ButtonData.OK_DONE);
        private static final Player NONE = new Player("", "");

        MatchDialog(Window owner, Tournament t, String fileName, Tournament.Matching m) {
            this.t = t;
            this.fileName = fileName;
            this.newPersons = m.newPersons();
            auto.putAll(m.auto());
            manual.addAll(m.auto().keySet());
            manual.addAll(m.open());
            initOwner(owner);
            setTitle("click-TT-Teilnehmerliste abgleichen");
            setHeaderText(null);
            Label title = new Label("click-TT-Teilnehmerliste abgleichen");
            title.getStyleClass().add("card-heading");
            head.setWrapText(true);
            head.getStyleClass().add("hint");
            VBox top = new VBox(6, title, head, chips);
            ScrollPane sp = new ScrollPane(body);
            sp.setFitToWidth(true);
            sp.getStyleClass().add("page-scroll");
            sp.setPrefViewportHeight(520);
            body.setPadding(new Insets(4, 4, 4, 0));
            openHint.getStyleClass().add("fee-open-label");
            HBox hintRow = new HBox(openHint);
            hintRow.setAlignment(Pos.CENTER_RIGHT);
            VBox content = new VBox(12, top, sp, hintRow);
            content.setPrefWidth(900);
            VBox.setVgrow(sp, Priority.ALWAYS);
            getDialogPane().setContent(content);
            ButtonType cancel = new ButtonType("Abbrechen – nichts ändern", ButtonBar.ButtonData.LEFT);
            getDialogPane().getButtonTypes().addAll(cancel, ok);
            render();
            DialogSizing.fit(this);
            setResultConverter(bt -> {
                if (bt != ok) return null;
                Map<Player, Player> out = new LinkedHashMap<>(auto);
                chosen.forEach((p, f) -> out.put(p, f == NONE ? null : f));
                return out;
            });
        }

        /** Person der Datei ist schon vergeben (automatisch oder von Hand, außer an {@code self}). */
        private boolean taken(Player f, Player self) {
            if (auto.containsValue(f)) return true;
            for (var e : chosen.entrySet()) if (e.getKey() != self && e.getValue() == f) return true;
            return false;
        }

        private List<Player> openPersons() {
            return manual.stream().filter(p -> !auto.containsKey(p)).toList();
        }

        private void render() {
            body.getChildren().clear();
            List<Player> open = openPersons();
            long done = open.stream().filter(p -> chosen.containsKey(p) && !editing.contains(p)).count();
            int fresh = newPersons.size();
            head.setText("Datei: " + fileName + " · " + fresh + (fresh == 1 ? " neue Person" : " neue Personen")
                    + " in der Datei (alle anderen sind bekannte Teilnehmende) · " + manual.size()
                    + (manual.size() == 1 ? " von Hand nachgetragene Person" : " von Hand nachgetragene Teilnehmende") + " in HTS");
            chips.getChildren().clear();
            if (!auto.isEmpty()) chips.getChildren().add(chip("✓ " + auto.size() + " eindeutig zugeordnet", "chip-started"));
            if (done > 0) chips.getChildren().add(chip("✓ " + done + " von Hand zugeordnet", "chip-started"));
            if (open.size() - done > 0) chips.getChildren().add(chip((open.size() - done) + " noch offen – bitte wählen", "chip-paused"));

            if (!open.isEmpty()) {
                body.getChildren().add(section("Bitte zuordnen (" + done + " von " + open.size() + " erledigt)"));
                for (Player p : open) body.getChildren().add(chosen.containsKey(p) && !editing.contains(p) ? doneRow(p) : card(p));
            }
            if (!auto.isEmpty()) {
                body.getChildren().add(section("Eindeutig zugeordnet (" + auto.size() + ")"));
                for (var e : auto.entrySet()) body.getChildren().add(autoRow(e.getKey(), e.getValue()));
            }
            List<Player> unassigned = newPersons.stream().filter(f -> !taken(f, null)).toList();
            body.getChildren().add(section("Neu in der Datei, nicht zugeordnet (" + unassigned.size() + ")"));
            Label rest = new Label(unassigned.isEmpty() ? "Alle neuen Personen der Datei sind zugeordnet."
                    : String.join(" · ", unassigned.stream().map(Player::getDisplayName).toList())
                    + " – noch keiner nachgetragenen Person zugeordnet; ohne Zuordnung bleiben sie unberücksichtigt.");
            rest.setWrapText(true);
            rest.getStyleClass().add("hint");
            body.getChildren().add(rest);

            long missing = open.stream().filter(p -> !chosen.containsKey(p) || editing.contains(p)).count();
            openHint.setText(missing == 0 ? "" : missing == 1 ? "Noch 1 Zuordnung offen" : "Noch " + missing + " Zuordnungen offen");
            Node okBtn = getDialogPane().lookupButton(ok);
            if (okBtn != null) okBtn.setDisable(missing > 0);
        }

        private static Label chip(String text, String style) {
            Label l = new Label(text);
            l.getStyleClass().addAll("chip", style);
            l.setMinWidth(Region.USE_PREF_SIZE);
            return l;
        }

        private static Label section(String text) {
            Label l = new Label(text);
            l.getStyleClass().add("card-title");
            return l;
        }

        private String personLine(Player p) {
            List<String> parts = new ArrayList<>();
            if (p.getBirthyear() != null) parts.add("Jg. " + p.getBirthyear());
            if (p.getClubName() != null && !p.getClubName().isBlank()) parts.add(p.getClubName());
            if (p.getTtr() != null) parts.add("TTR " + p.getTtr());
            return String.join(" · ", parts);
        }

        private static String fileLine(Player f) {
            List<String> parts = new ArrayList<>(List.of(f.getDisplayName()));
            if (f.getBirthyear() != null) parts.add("Jg. " + f.getBirthyear());
            if (f.getClubName() != null && !f.getClubName().isBlank()) parts.add(f.getClubName());
            return String.join(" · ", parts);
        }

        private static String entered(Player f) {
            List<String> cs = new ArrayList<>(f.getClassNames());
            cs.addAll(f.getRegistrations());
            cs.addAll(f.getWaitingLists());
            return cs.isEmpty() ? "" : "gemeldet: " + String.join(", ", cs.stream().distinct().toList());
        }

        /** Offene Karte: links die nachgetragene Person, rechts alle noch nicht zugeordneten neuen Personen. */
        private Node card(Player p) {
            Label who = new Label("IN HTS (VON HAND ERFASST)");
            who.getStyleClass().add("caption-label");
            Label name = new Label(p.getDisplayName());
            name.getStyleClass().add("card-title");
            Label line = new Label(personLine(p));
            int games = matchesOf(t, p);
            Label cls = new Label(classesOf(t, p) + ": " + Ui.spiele(games));
            cls.getStyleClass().add("hint");
            cls.setWrapText(true);
            VBox left = new VBox(4, who, name, line, cls);

            Label legend = new Label("NEU IN DER DATEI – ALLE NOCH NICHT ZUGEORDNETEN PERSONEN");
            legend.getStyleClass().add("caption-label");
            GridPane options = new GridPane();
            options.setHgap(6);
            options.setVgap(6);
            for (int k = 0; k < 2; k++) {
                ColumnConstraints half = new ColumnConstraints();
                half.setPercentWidth(50);
                half.setMinWidth(0);
                half.setFillWidth(true);
                options.getColumnConstraints().add(half);
            }
            options.setMinWidth(0);
            ToggleGroup g = new ToggleGroup();
            int i = 0;
            for (Player f : newPersons) {
                if (taken(f, p)) continue;
                options.add(option(g, p, f, fileLine(f), entered(f)), i % 2, i / 2);
                i++;
            }
            options.add(option(g, p, NONE, "keine Zuordnung", games == 1 ? "1 Spiel wird nicht exportiert"
                    : games + " Spiele werden nicht exportiert"), i % 2, i / 2);
            VBox right = new VBox(6, legend, options);
            right.setMinWidth(0);
            legend.setWrapText(true);
            HBox.setHgrow(right, Priority.ALWAYS);
            left.setMinWidth(220);
            left.setPrefWidth(260);
            HBox card = new HBox(20, left, right);
            card.getStyleClass().add("match-open-card");
            return card;
        }

        private Node option(ToggleGroup g, Player p, Player f, String title, String sub) {
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(g);
            rb.setSelected(chosen.get(p) == f);
            rb.setAccessibleText(title);
            Label t1 = new Label(title);
            t1.getStyleClass().add("class-name");
            t1.setWrapText(true);
            t1.setMinWidth(0);
            Label t2 = new Label(sub);
            t2.getStyleClass().add("hint");
            t2.setWrapText(true);
            t2.setMinWidth(0);
            VBox txt = new VBox(2, t1, t2);
            txt.setMinWidth(0);
            HBox.setHgrow(txt, Priority.ALWAYS);
            HBox box = new HBox(10, rb, txt);
            box.setAlignment(Pos.TOP_LEFT);
            box.setMinWidth(0);
            box.setMaxWidth(Double.MAX_VALUE);
            box.getStyleClass().add("match-option");
            if (rb.isSelected()) box.getStyleClass().add("match-option-selected");
            Runnable pick = () -> {
                chosen.put(p, f);
                editing.remove(p);
                render();
            };
            rb.setOnAction(e -> pick.run());
            box.setOnMouseClicked(e -> pick.run());
            return box;
        }

        /** Erledigte Karte, zu einer Zeile zusammengeklappt, mit „Ändern“. */
        private Node doneRow(Player p) {
            Player f = chosen.get(p);
            Label ok = new Label("✓");
            ok.getStyleClass().add("fee-paid-label");
            Label name = new Label(p.getDisplayName());
            name.getStyleClass().add("class-name");
            Label line = new Label(personLine(p) + " · " + classesOf(t, p) + ", " + Ui.spiele(matchesOf(t, p)));
            line.getStyleClass().add("hint");
            Label arrow = new Label("⟷");
            Label target = new Label(f == NONE ? "keine Zuordnung (Spiele werden nicht exportiert)" : fileLine(f));
            target.getStyleClass().add("class-name");
            Button change = new Button("Ändern");
            change.getStyleClass().add("pill");
            change.setOnAction(e -> {
                editing.add(p);
                render();
            });
            return row(ok, name, line, arrow, target, change);
        }

        private Node autoRow(Player p, Player f) {
            Label ok = new Label("✓");
            ok.getStyleClass().add("fee-paid-label");
            Label a = new Label(fileLine(p));
            a.getStyleClass().add("class-name");
            Label arrow = new Label("⟷");
            Label b = new Label(fileLine(f));
            Button undo = new Button("Zuordnung lösen");
            undo.getStyleClass().add("pill");
            undo.setOnAction(e -> {
                auto.remove(p);
                render();
            });
            return row(ok, a, arrow, b, undo);
        }

        private static Node row(Node... nodes) {
            FlowPane fp = new FlowPane(12, 6, nodes);
            fp.setAlignment(Pos.CENTER_LEFT);
            fp.getStyleClass().add("match-done-row");
            for (Node n : nodes) if (n instanceof Region r) r.setMinWidth(Region.USE_PREF_SIZE);
            return fp;
        }
    }

    // --- Bericht nach dem Export (Entwurf 4) -------------------------------------------------------------

    /** Bericht: was exportiert wurde und welche Spiele fehlen (mit Grund); „Teilnehmerliste abgleichen …“ ruft {@code onMatch}. */
    static void showReport(Window owner, Tournament t, ClickTtResultExporter.Report rep, java.nio.file.Path target,
                           Runnable onMatch, Consumer<String> status) {
        Dialog<ButtonType> d = new Dialog<>();
        d.initOwner(owner);
        d.setTitle("click-TT-Export");
        d.setHeaderText("Ergebnisdatei erstellt");
        Label summary = new Label(rep.competitions() + " Konkurrenz(en), " + rep.matches() + " Spiele, " + rep.placements()
                + " Platzierungen exportiert nach\n" + target + "\n\nDie Datei kann in click-TT beim Turnier hochgeladen werden.");
        summary.setWrapText(true);
        VBox box = new VBox(12, summary);
        if (!rep.skipped().isEmpty()) {
            Label sk = new Label("Nicht exportierte Konkurrenzen:\n• " + String.join("\n• ", rep.skipped()));
            sk.setWrapText(true);
            box.getChildren().add(sk);
        }
        List<ClickTtResultExporter.NotExported> ne = rep.notExported();
        if (!ne.isEmpty()) {
            Label h = new Label("Nicht exportierte Spiele (" + ne.size() + ")");
            h.getStyleClass().add("card-title");
            long noId = ne.stream().filter(n -> n.reason().endsWith("ohne click-TT-Nummer")).count();
            Label why = new Label("Bewusst ausgelassene Spiele lassen sich auf der Seite „Spiele“ (Spalte „Export“) wieder einschließen."
                    + (noId > 0 ? " " + noId + (noId == 1 ? " Spiel fehlt" : " Spiele fehlen") + " mangels click-TT-Nummer." : ""));
            why.setWrapText(true);
            why.getStyleClass().add("hint");
            TableView<ClickTtResultExporter.NotExported> table = new TableView<>();
            table.getItems().setAll(ne);
            table.getColumns().add(col("Klasse", ClickTtResultExporter.NotExported::className));
            table.getColumns().add(col("Gruppe", ClickTtResultExporter.NotExported::stage));
            table.getColumns().add(col("Spiel", n -> String.valueOf(n.number())));
            table.getColumns().add(col("Teilnehmende A", ClickTtResultExporter.NotExported::a));
            table.getColumns().add(col("Teilnehmende B", ClickTtResultExporter.NotExported::b));
            table.getColumns().add(col("Ergebnis", ClickTtResultExporter.NotExported::result));
            table.getColumns().add(col("Grund", ClickTtResultExporter.NotExported::reason));
            table.setPrefHeight(Math.min(360, 34 + 28 * ne.size()));
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            Ui.fitColumns(table);
            box.getChildren().addAll(h, why, table);
        }
        box.setPadding(new Insets(6));
        box.setPrefWidth(860);
        d.getDialogPane().setContent(box);
        ButtonType pdf = new ButtonType("Als PDF", ButtonBar.ButtonData.LEFT);
        ButtonType match = new ButtonType("Teilnehmerliste abgleichen …", ButtonBar.ButtonData.OTHER);
        ButtonType close = new ButtonType("Schließen", ButtonBar.ButtonData.OK_DONE);
        if (!ne.isEmpty()) d.getDialogPane().getButtonTypes().add(pdf);
        if (!rep.withoutId().isEmpty()) d.getDialogPane().getButtonTypes().add(match);
        d.getDialogPane().getButtonTypes().add(close);
        DialogSizing.fit(d);
        ButtonType r = d.showAndWait().orElse(close);
        if (r == pdf) Reports.showNotExported(owner, t, ne, status);
        else if (r == match) onMatch.run();
    }

    private static TableColumn<ClickTtResultExporter.NotExported, String> col(String title,
            java.util.function.Function<ClickTtResultExporter.NotExported, String> f) {
        TableColumn<ClickTtResultExporter.NotExported, String> c = new TableColumn<>(title);
        c.setCellValueFactory(x -> new javafx.beans.property.ReadOnlyStringWrapper(f.apply(x.getValue())));
        c.setSortable(false);
        return c;
    }
}
