package de.hts.ui;

import de.hts.importer.clicktt.ClickTtXmlImporter;
import de.hts.player.Player;
import de.hts.tournament.Tournament;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zeigt vor der Übernahme, was in der Datei steckt: Turnier, Konkurrenzen mit Meldezahlen
 * und wie viele Spieler neu sind bzw. bereits in der Liste stehen.
 */
public final class ImportPreviewDialog extends Dialog<ButtonType> {

    /** 0.36: je Konkurrenz „importieren ja/nein“ (Standard: alle). */
    private final java.util.Map<String, javafx.beans.property.BooleanProperty> chosen = new java.util.LinkedHashMap<>();

    public ImportPreviewDialog(Window owner, String fileName, ClickTtXmlImporter.ImportResult result,
                               Tournament repository) {
        initOwner(owner);
        setTitle("click-TT-Import");
        setHeaderText("Teilnehmende aus „" + fileName + "“ übernehmen?");
        for (ClickTtXmlImporter.CompetitionInfo ci : result.competitions()) chosen.put(ci.name(), new javafx.beans.property.SimpleBooleanProperty(true));

        Set<String> existingKeys = repository.getPlayers().stream()
                .map(Player::identityKey).collect(Collectors.toCollection(HashSet::new));
        GridPane facts = new GridPane();
        facts.setHgap(12);
        facts.setVgap(4);
        Runnable update = () -> {
            facts.getChildren().clear();
            Set<String> sel = getSelectedCompetitions();
            java.util.List<Player> kept = result.players().stream().filter(p -> keeps(p, sel)).toList();
            long existing = kept.stream().filter(p -> existingKeys.contains(p.identityKey())).count();
            long added = kept.size() - existing;
            int row = 0;
            if (result.tournamentName() != null && !result.tournamentName().isBlank()) {
                facts.addRow(row++, bold("Turnier:"), value(result.tournamentName()));
            }
            facts.addRow(row++, bold("Teilnehmende in der Datei:"), value(kept.size() + (kept.size() != result.players().size()
                    ? " von " + result.players().size() + " (nur ausgewählte Klassen)" : "")));
            facts.addRow(row++, bold("Davon neu:"), value(String.valueOf(added)));
            long waitingOnly = kept.stream().filter(p -> p.getClassNames().stream().noneMatch(sel::contains)
                    && p.getWaitingLists().stream().anyMatch(sel::contains)).count();
            if (waitingOnly > 0) {
                facts.addRow(row++, bold("Nur auf Warteliste:"), value(waitingOnly + " (Nachrücker – werden übernommen, aber nicht gemeldet)"));
            }
            long newSingles = result.competitions().stream()
                    .filter(ci -> sel.contains(ci.name()) && !ci.doubles() && repository.findClass(ci.name()).isEmpty()).count();
            if (newSingles > 0) {
                facts.addRow(row++, bold("Neue Einzelklassen:"), value(newSingles
                        + " (die Gemeldeten stehen dort zunächst unter „Verfügbar“ – Meldung „gemeldet“)"));
            }
            facts.addRow(row++, bold("Bereits vorhanden:"),
                    value(existing + (existing > 0 ? " (werden mit den Daten aus der Datei aktualisiert)" : "")));
        };

        TableView<ClickTtXmlImporter.CompetitionInfo> comps = new TableView<>();
        comps.setEditable(true);
        TableColumn<ClickTtXmlImporter.CompetitionInfo, Boolean> use = new TableColumn<>("Importieren");
        use.setCellValueFactory(c -> chosen.get(c.getValue().name()));
        use.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(use));
        use.setEditable(true);
        use.setStyle("-fx-alignment: CENTER;");
        use.getStyleClass().add("col-center");
        use.getProperties().put("fit-min", 110.0);
        TableColumn<ClickTtXmlImporter.CompetitionInfo, String> name = new TableColumn<>("Konkurrenz");
        name.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name()));
        TableColumn<ClickTtXmlImporter.CompetitionInfo, Integer> entries = new TableColumn<>("Meldungen");
        entries.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().entries()));
        entries.setStyle("-fx-alignment: CENTER-RIGHT;");
        entries.getStyleClass().add("col-right");
        TableColumn<ClickTtXmlImporter.CompetitionInfo, Integer> waiting = new TableColumn<>("davon Warteliste");
        waiting.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().waitingList()));
        waiting.setStyle("-fx-alignment: CENTER-RIGHT;");
        waiting.getStyleClass().add("col-right");
        comps.getColumns().add(use);
        comps.getColumns().add(name);
        comps.getColumns().add(entries);
        comps.getColumns().add(waiting);
        comps.getItems().addAll(result.competitions());
        comps.setPrefHeight(Math.min(300, 34 + 28 * Math.max(1, result.competitions().size())));
        comps.setPlaceholder(new Label("Keine Konkurrenzen in der Datei."));
        Ui.fitColumns(comps);
        comps.setMinWidth(Region.USE_PREF_SIZE);
        comps.setPrefWidth(comps.getColumns().stream().mapToDouble(c -> c.getMinWidth()).sum() + 24);

        VBox content = new VBox(10, facts);
        if (!result.competitions().isEmpty()) {
            Label hint = new Label("Haken entfernen = diese Klasse wird nicht aus der Datei übernommen.");
            hint.getStyleClass().add("hint");
            hint.setWrapText(true);
            content.getChildren().addAll(bold("Konkurrenzen:"), hint, comps);
        }
        if (!result.warnings().isEmpty()) {
            Label w = new Label("Hinweise:\n• " + String.join("\n• ", result.warnings()));
            w.getStyleClass().add("warning");
            w.setWrapText(true);
            content.getChildren().add(w);
        }
        content.setPadding(new Insets(10));

        getDialogPane().setContent(content);
        ButtonType importBtn = new ButtonType("Importieren", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(importBtn, ButtonType.CANCEL);
        javafx.scene.Node okNode = getDialogPane().lookupButton(importBtn);
        Runnable all = () -> {
            update.run();
            okNode.setDisable(!result.competitions().isEmpty() && getSelectedCompetitions().isEmpty());
        };
        chosen.values().forEach(b -> b.addListener((o, a, v) -> all.run()));
        all.run();
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == importBtn ? ButtonType.OK : ButtonType.CANCEL);
    }

    /** Ausgewählte Konkurrenzen (Namen). */
    public Set<String> getSelectedCompetitions() {
        Set<String> out = new java.util.LinkedHashSet<>();
        chosen.forEach((n, b) -> {
            if (b.get()) out.add(n);
        });
        return out;
    }

    /** Wird die Person übernommen? Nur wer ausschließlich in abgewählten Klassen gemeldet ist, fällt weg. */
    static boolean keeps(Player p, Set<String> selected) {
        Set<String> entries = new HashSet<>(p.getClassNames());
        entries.addAll(p.getWaitingLists());
        entries.addAll(p.getRegistrations());
        return entries.isEmpty() || entries.stream().anyMatch(selected::contains);
    }

    private static Label value(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(460);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }
}
