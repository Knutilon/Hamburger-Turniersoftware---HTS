package de.hts.ui;

import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Reiter „Endergebnis“ einer Klasse: alle Platzierungen über alle Phasen. Solange die Klasse läuft, wird der
 * Zwischenstand gezeigt (KO: wer ausgeschieden ist und wer noch im Wettbewerb ist; Gruppen: aktuelle Tabellenplätze).
 */
final class FinalResultView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'");

    private final Tournament tournament;
    private final VBox root = new VBox(10);
    private final Label head = new Label();
    private final Label sub = new Label();
    private final TableView<Tournament.Placing> table = new TableView<>();
    private final javafx.scene.control.Button pdf = new javafx.scene.control.Button("Als PDF anzeigen / drucken");
    private final java.util.function.Consumer<String> status;
    private PlayingClass shown;

    FinalResultView(Tournament tournament, java.util.function.Consumer<String> status) {
        this.tournament = tournament;
        this.status = status;
        pdf.getStyleClass().add("navy-pill");
        pdf.setOnAction(e -> {
            if (shown != null) {
                Reports.showFinalResult(root.getScene() == null ? null : root.getScene().getWindow(), tournament, shown, status);
            }
        });
        head.getStyleClass().add("card-heading");
        sub.getStyleClass().add("hint");
        sub.setWrapText(true);

        TableColumn<Tournament.Placing, String> place = col("Platz", Tournament.Placing::placeText, 80);
        place.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().removeAll("place-1", "place-2", "place-3");
                Tournament.Placing p = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (p != null && p.from() == p.to() && p.from() <= 3) getStyleClass().add("place-" + p.from());
            }
        });
        table.getColumns().add(place);
        table.getColumns().add(col("Name", p -> p.entrant().name(), 260));
        table.getColumns().add(col("Verein", p -> p.entrant().detail() == null ? "" : p.entrant().detail(), 240));
        table.getColumns().add(col("TTR", p -> p.entrant().ttr() == null ? "" : String.valueOf(p.entrant().ttr()), 70));
        table.getColumns().add(col("Ermittelt durch", Tournament.Placing::info, 260));
        table.getColumns().forEach(c -> c.setSortable(false));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Noch keine Platzierungen – die Klasse ist noch nicht ausgelost."));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Tournament.Placing p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().remove("row-open");
                if (!empty && p != null && p.from() != p.to()) getStyleClass().add("row-open");
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        javafx.scene.layout.Region gap = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(gap, Priority.ALWAYS);
        javafx.scene.layout.HBox top = new javafx.scene.layout.HBox(10, head, gap, pdf);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        root.getChildren().addAll(top, sub, table);
        root.setPadding(new Insets(10, 0, 0, 0));
    }

    Node getRoot() {
        return root;
    }

    void show(PlayingClass c) {
        shown = c;
        pdf.setDisable(c == null);
        if (c == null) {
            head.setText("Endergebnis");
            sub.setText("");
            table.getItems().clear();
            return;
        }
        PlayingClass first = tournament.rootOf(c);
        Tournament.FinalResult r = tournament.finalResult(first);
        if (r.complete()) {
            head.setText("Endergebnis " + first.getName());
            sub.setText("Klasse beendet am " + first.getFinishedAt().format(TIME) + " · " + r.rows().size() + " Platzierungen über "
                    + tournament.phasesOf(first).size() + (tournament.phasesOf(first).size() == 1 ? " Phase." : " Phasen."));
        } else {
            head.setText("Zwischenstand " + first.getName());
            sub.setText(first.isStarted()
                    ? "Wird mit jedem Ergebnis aktualisiert. Bereiche wie „5.–8.“ stehen noch nicht endgültig fest "
                    + "(KO: noch im Wettbewerb bzw. geteilter Platz; Gruppen: aktueller Tabellenplatz)."
                    : "Die Klasse ist noch nicht gestartet – die Tabelle füllt sich mit den Ergebnissen.");
        }
        table.getItems().setAll(r.rows());
    }

    private static <T> TableColumn<Tournament.Placing, T> col(String title, Function<Tournament.Placing, T> getter, double width) {
        TableColumn<Tournament.Placing, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }
}
