package de.hts.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;

import java.util.List;
import java.util.Locale;

/**
 * 0.40 (#63): durchsuchbare Auswahl (z. B. „Verein:“). Sieht aus wie eine Filterauswahl; Tippen filtert nur die
 * Einträge der aufklappenden Liste (Teil des Namens genügt, Groß-/Kleinschreibung egal). Der Wert – und damit ein
 * Tabellenfilter – ändert sich erst, wenn ein Eintrag gewählt wird (Klick oder Enter). Pfeiltasten bewegen sich in der
 * Liste, Esc bzw. Verlassen des Feldes stellt den gewählten Wert wieder her.
 * <p>Eigenes Steuerelement statt einer editierbaren ComboBox: Dort setzt das Filtern der Einträge die Auswahl und damit
 * den Wert zurück (Text verschwindet, Tabelle springt).
 */
final class SearchChoice extends HBox {

    private final TextField field = new TextField();
    private final Label arrow = new Label("▾");
    private final ObjectProperty<String> value = new SimpleObjectProperty<>();
    private final ObservableList<String> items = FXCollections.observableArrayList();
    private final FilteredList<String> shown = new FilteredList<>(items, x -> true);
    private final ListView<String> list = new ListView<>(shown);
    private final Popup popup = new Popup();
    private boolean programmatic;

    SearchChoice(String searchPrompt) {
        getStyleClass().add("search-choice");
        field.getStyleClass().add("search-choice-field");
        arrow.getStyleClass().add("search-choice-arrow");
        field.setPromptText(searchPrompt);
        HBox.setHgrow(field, Priority.ALWAYS);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        getChildren().addAll(field, arrow);

        list.getStyleClass().add("search-choice-list");
        list.setFocusTraversable(false);
        list.setPlaceholder(new Label("Kein Treffer"));
        popup.getContent().add(list);
        popup.setAutoHide(true);
        popup.setHideOnEscape(false);                 // Esc behandelt das Feld selbst (stellt den Wert wieder her)
        popup.setConsumeAutoHidingEvents(false);
        popup.setOnHidden(e -> {
            if (!field.isFocused()) restore();
        });

        value.addListener((o, a, b) -> restore());
        field.textProperty().addListener((o, a, b) -> {
            if (programmatic || !field.isFocused()) return;
            String q = b == null ? "" : b.trim().toLowerCase(Locale.GERMAN);
            shown.setPredicate(x -> q.isEmpty() || x.toLowerCase(Locale.GERMAN).contains(q));
            if (!shown.isEmpty()) list.getSelectionModel().select(0);
            showPopup();
        });
        field.focusedProperty().addListener((o, a, f) -> {
            if (f) {
                javafx.application.Platform.runLater(field::selectAll);
            } else {
                popup.hide();
                restore();
            }
        });
        field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case DOWN, UP -> {
                    if (!popup.isShowing()) {
                        showAll();
                    } else {
                        int i = list.getSelectionModel().getSelectedIndex() + (e.getCode() == KeyCode.DOWN ? 1 : -1);
                        if (i >= 0 && i < shown.size()) {
                            list.getSelectionModel().select(i);
                            list.scrollTo(Math.max(0, i - 3));
                        }
                    }
                    e.consume();
                }
                case ENTER -> {
                    String pick = popup.isShowing() ? list.getSelectionModel().getSelectedItem() : null;
                    if (pick == null && shown.size() == 1) pick = shown.get(0);
                    if (pick == null && (field.getText() == null || field.getText().isBlank()) && !items.isEmpty()) pick = items.get(0);
                    commit(pick);
                    e.consume();
                }
                case ESCAPE -> {
                    popup.hide();
                    restore();
                    field.selectAll();
                    e.consume();
                }
                default -> { }
            }
        });
        field.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !popup.isShowing()) showAll();
        });
        arrow.setOnMouseClicked(e -> {
            field.requestFocus();
            if (popup.isShowing()) popup.hide();
            else showAll();
        });
        list.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && list.getSelectionModel().getSelectedItem() != null) {
                commit(list.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void showAll() {
        shown.setPredicate(x -> true);
        String v = value.get();
        if (v != null) list.getSelectionModel().select(v);
        showPopup();
        int i = list.getSelectionModel().getSelectedIndex();
        if (i > 0) list.scrollTo(Math.max(0, i - 3));
    }

    private void showPopup() {
        if (getScene() == null || getScene().getWindow() == null) return;
        list.setPrefWidth(Math.max(getWidth(), 220));
        list.setPrefHeight(Math.min(shown.size(), 10) * 26 + 6);
        if (!popup.isShowing()) {
            Bounds b = localToScreen(getBoundsInLocal());
            if (b != null) popup.show(this, b.getMinX(), b.getMaxY() + 2);
        }
    }

    private void commit(String pick) {
        popup.hide();
        if (pick != null && items.contains(pick)) value.set(pick);
        restore();
        field.selectAll();
    }

    /** Text zeigt wieder den gewählten Wert, Liste ungefiltert. */
    private void restore() {
        programmatic = true;
        try {
            field.setText(value.get() == null ? "" : value.get());
            shown.setPredicate(x -> true);
        } finally {
            programmatic = false;
        }
    }

    // --- Zugriff wie bei einer ComboBox ---------------------------------------------------------------

    ObjectProperty<String> valueProperty() {
        return value;
    }

    String getValue() {
        return value.get();
    }

    void setValue(String v) {
        value.set(v);
        restore();
    }

    /** Einträge setzen; der gewählte Wert bleibt (sonst der erste Eintrag). */
    void setItems(List<String> newItems) {
        if (!newItems.equals(items)) items.setAll(newItems);
        if (value.get() == null || !items.contains(value.get())) value.set(items.isEmpty() ? null : items.get(0));
        restore();
    }

    List<String> getItemsCopy() {
        return List.copyOf(items);
    }

    void setTooltip(Tooltip t) {
        field.setTooltip(t);
    }
}
