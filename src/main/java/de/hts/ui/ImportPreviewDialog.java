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

    public ImportPreviewDialog(Window owner, String fileName, ClickTtXmlImporter.ImportResult result,
                               Tournament repository) {
        initOwner(owner);
        setTitle("click-TT-Import");
        setHeaderText("Teilnehmende aus „" + fileName + "“ übernehmen?");

        Set<String> existingKeys = repository.getPlayers().stream()
                .map(Player::identityKey).collect(Collectors.toCollection(HashSet::new));
        long existing = result.players().stream().filter(p -> existingKeys.contains(p.identityKey())).count();
        long added = result.players().size() - existing;

        GridPane facts = new GridPane();
        facts.setHgap(12);
        facts.setVgap(4);
        int row = 0;
        if (result.tournamentName() != null && !result.tournamentName().isBlank()) {
            facts.addRow(row++, bold("Turnier:"), new Label(result.tournamentName()));
        }
        facts.addRow(row++, bold("Teilnehmende in der Datei:"), new Label(String.valueOf(result.players().size())));
        facts.addRow(row++, bold("Davon neu:"), new Label(String.valueOf(added)));
        long waitingOnly = result.players().stream().filter(p -> !p.isRegistered() && !p.getWaitingLists().isEmpty()).count();
        if (waitingOnly > 0) {
            facts.addRow(row++, bold("Nur auf Warteliste:"),
                    new Label(waitingOnly + " (werden übernommen, aber keiner Klasse zugeordnet)"));
        }
        long newSingles = result.competitions().stream()
                .filter(ci -> !ci.doubles() && repository.findClass(ci.name()).isEmpty()).count();
        if (newSingles > 0) {
            facts.addRow(row++, bold("Neue Einzelklassen:"), new Label(newSingles
                    + " (die Gemeldeten stehen dort zunächst unter „Verfügbar“ – Meldung „gemeldet“)"));
        }
        facts.addRow(row++, bold("Bereits vorhanden:"),
                new Label(existing + (existing > 0 ? " (werden mit den Daten aus der Datei aktualisiert)" : "")));

        TableView<ClickTtXmlImporter.CompetitionInfo> comps = new TableView<>();
        TableColumn<ClickTtXmlImporter.CompetitionInfo, String> name = new TableColumn<>("Konkurrenz");
        name.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name()));
        name.setPrefWidth(280);
        TableColumn<ClickTtXmlImporter.CompetitionInfo, Integer> entries = new TableColumn<>("Meldungen");
        entries.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().entries()));
        entries.setStyle("-fx-alignment: CENTER-RIGHT;");
        entries.getStyleClass().add("col-right");
        TableColumn<ClickTtXmlImporter.CompetitionInfo, Integer> waiting = new TableColumn<>("davon Warteliste");
        waiting.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().waitingList()));
        waiting.setStyle("-fx-alignment: CENTER-RIGHT;");
        waiting.getStyleClass().add("col-right");
        comps.getColumns().add(name);
        comps.getColumns().add(entries);
        comps.getColumns().add(waiting);
        comps.getItems().addAll(result.competitions());
        comps.setPrefHeight(Math.min(260, 30 + 26 * Math.max(1, result.competitions().size())));
        comps.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        comps.setPlaceholder(new Label("Keine Konkurrenzen in der Datei."));

        VBox content = new VBox(10, facts);
        if (!result.competitions().isEmpty()) {
            content.getChildren().addAll(bold("Konkurrenzen:"), comps);
        }
        if (!result.warnings().isEmpty()) {
            Label w = new Label("Hinweise:\n• " + String.join("\n• ", result.warnings()));
            w.getStyleClass().add("warning");
            w.setWrapText(true);
            content.getChildren().add(w);
        }
        content.setPadding(new Insets(10));
        content.setPrefWidth(600);

        getDialogPane().setContent(content);
        ButtonType importBtn = new ButtonType("Importieren", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(importBtn, ButtonType.CANCEL);
        DialogSizing.fit(this);
        setResultConverter(bt -> bt == importBtn ? ButtonType.OK : ButtonType.CANCEL);
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }
}
