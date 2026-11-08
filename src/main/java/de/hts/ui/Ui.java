package de.hts.ui;

/** Programmweite Einstellungen der Oberfläche. */
public final class Ui {

    /** „1 Spiel“ / „n Spiele“. */
    public static String spiele(long n) {
        return n + (n == 1 ? " Spiel" : " Spiele");
    }


    private Ui() {
    }

    // --- 0.36: Texte nie abkürzen – Breiten aus dem Inhalt ------------------------------

    private static final String FAMILY = javafx.scene.text.Font.getFamilies().contains("Segoe UI") ? "Segoe UI"
            : javafx.scene.text.Font.getDefault().getFamily();

    /** Breite eines Textes in der Programmschrift (13 px, Segoe UI). */
    public static double textWidth(String s, boolean bold, double size) {
        if (s == null || s.isEmpty()) return 0;
        javafx.scene.text.Text t = new javafx.scene.text.Text(s);
        t.setFont(javafx.scene.text.Font.font(FAMILY, bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, size));
        return Math.ceil(t.getLayoutBounds().getWidth());
    }

    /**
     * Spalten so breit wie ihr Inhalt (Titel fett, Zellen normal) – keine „…“; ist die Tabelle breiter als das Fenster,
     * erscheint eine waagerechte Scrollleiste. Restplatz bekommt die letzte sichtbare Spalte.
     */
    public static <S> void fitColumns(javafx.scene.control.TableView<S> table) {
        table.setColumnResizePolicy(javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY);
        java.util.List<javafx.scene.control.TableColumn<S, ?>> cols = new java.util.ArrayList<>();
        for (javafx.scene.control.TableColumn<S, ?> c : table.getColumns()) if (c.isVisible()) cols.add(c);
        if (cols.isEmpty()) return;
        java.util.Map<javafx.scene.control.TableColumn<S, ?>, Double> need = new java.util.HashMap<>();
        for (javafx.scene.control.TableColumn<S, ?> c : cols) {
            double w = textWidth(c.getText(), true, 13) + 30;            // Titel + Sortierpfeil
            Object fixed = c.getProperties().get("fit-min");
            if (fixed instanceof Number n) w = Math.max(w, n.doubleValue());
            if (!Boolean.TRUE.equals(c.getProperties().get("fit-header-only"))) {
                for (S item : table.getItems()) {
                    javafx.beans.value.ObservableValue<?> v = c.getCellObservableValue(item);
                    Object val = v == null ? null : v.getValue();
                    if (val instanceof CharSequence || val instanceof Number) w = Math.max(w, textWidth(String.valueOf(val), false, 13) + 18);
                }
            }
            need.put(c, Math.ceil(w));
        }
        javafx.scene.control.TableColumn<S, ?> last = cols.get(cols.size() - 1);
        Runnable apply = () -> {
            double sum = 0;
            for (javafx.scene.control.TableColumn<S, ?> c : cols) {
                double w = need.get(c);
                c.setMinWidth(w);
                c.setMaxWidth(Double.MAX_VALUE);
                if (c != last) {
                    c.setPrefWidth(w);
                    sum += w;
                }
            }
            double rest = table.getWidth() - sum - 20;
            last.setPrefWidth(Math.max(need.get(last), rest));
        };
        apply.run();
        Object old = table.getProperties().get("fit-listener");
        if (old instanceof javafx.beans.value.ChangeListener<?> l) {
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<Number> cl = (javafx.beans.value.ChangeListener<Number>) l;
            table.widthProperty().removeListener(cl);
        }
        javafx.beans.value.ChangeListener<Number> listener = (o, a, b) -> apply.run();
        table.getProperties().put("fit-listener", listener);
        table.widthProperty().addListener(listener);
    }

    /**
     * Vergleich für Zahlenspalten (z. B. TTR): leere Werte stehen in BEIDEN Sortierrichtungen unten (die Tabelle dreht
     * beim absteigenden Sortieren den Vergleich um – deshalb kehrt sich hier die Lage der leeren Werte mit um).
     */
    public static <S, T extends Comparable<? super T>> java.util.Comparator<T> nullsLastBothWays(javafx.scene.control.TableColumn<S, T> col) {
        return (a, b) -> {
            boolean desc = col.getSortType() == javafx.scene.control.TableColumn.SortType.DESCENDING;
            if (a == null && b == null) return 0;
            if (a == null) return desc ? -1 : 1;
            if (b == null) return desc ? 1 : -1;
            return a.compareTo(b);
        };
    }

    /** Alle Knöpfe in allen Fenstern ausgeschrieben (nie mit „…“ gekürzt). */
    public static void installButtonSizing() {
        ButtonSizing.installEverywhere();
    }

    // --- 0.39 (#63): durchsuchbare Auswahl -----------------------------------------------------------

    /**
     * Macht eine Auswahl (z. B. „Verein:“) durchsuchbar: Tippen filtert nur die Einträge der Liste (Teil des Namens genügt,
     * Groß-/Kleinschreibung egal); der Wert – und damit ein Tabellenfilter – ändert sich erst, wenn ein Eintrag gewählt
     * wird (Klick oder Enter). Esc bzw. Verlassen des Feldes stellt den gewählten Wert wieder her; leeres Feld = erster
     * Eintrag („Alle“). Einträge danach nur über {@link #setSearchItems} setzen.
     */
    @SuppressWarnings("unchecked")
    public static void makeSearchable(javafx.scene.control.ComboBox<String> cb, String prompt) {
        javafx.collections.ObservableList<String> master = javafx.collections.FXCollections.observableArrayList(cb.getItems());
        javafx.collections.transformation.FilteredList<String> shown = new javafx.collections.transformation.FilteredList<>(master, x -> true);
        cb.getProperties().put("search-master", master);
        cb.setItems(shown);
        cb.setEditable(true);
        cb.getStyleClass().add("search-combo");
        cb.getEditor().setPromptText(prompt);
        cb.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String s) {
                return s == null ? "" : s;
            }

            @Override
            public String fromString(String text) {
                String t = text == null ? "" : text.trim();
                if (t.isEmpty()) return master.isEmpty() ? null : master.get(0);
                for (String m : master) if (m.equalsIgnoreCase(t)) return m;
                if (shown.size() == 1) return shown.get(0);         // eindeutiger Treffer
                return cb.getValue();                                // sonst bleibt der bisherige Wert
            }
        });
        cb.getEditor().textProperty().addListener((o, a, b) -> {
            if (!cb.getEditor().isFocused()) return;
            String v = cb.getValue();
            if (v != null && v.equals(b)) {
                shown.setPredicate(x -> true);
                return;
            }
            String q = b == null ? "" : b.trim().toLowerCase(java.util.Locale.GERMAN);
            shown.setPredicate(x -> q.isEmpty() || x.toLowerCase(java.util.Locale.GERMAN).contains(q));
            if (!cb.isShowing() && cb.getScene() != null) cb.show();
        });
        Runnable restore = () -> {
            shown.setPredicate(x -> true);
            cb.getEditor().setText(cb.getValue() == null ? "" : cb.getValue());
        };
        cb.getEditor().focusedProperty().addListener((o, a, f) -> {
            if (!f) restore.run();
            else javafx.application.Platform.runLater(() -> cb.getEditor().selectAll());
        });
        cb.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                restore.run();
                cb.hide();
                e.consume();
            }
        });
        cb.setOnHidden(e -> {
            if (!cb.getEditor().isFocused()) shown.setPredicate(x -> true);
        });
    }

    /** Einträge einer durchsuchbaren (oder normalen) Auswahl setzen; der gewählte Wert bleibt, falls vorhanden. */
    @SuppressWarnings("unchecked")
    public static void setSearchItems(javafx.scene.control.ComboBox<String> cb, java.util.List<String> items) {
        Object m = cb.getProperties().get("search-master");
        javafx.collections.ObservableList<String> target = m instanceof javafx.collections.ObservableList<?> ol
                ? (javafx.collections.ObservableList<String>) ol : cb.getItems();
        if (!items.equals(target)) target.setAll(items);
    }

    /**
     * 0.39: Eine FlowPane, die auf breiten Bildschirmen alles in einer Zeile zeigt, ohne ihre Umgebung künstlich zu
     * verbreitern: Die bevorzugte Umbruchbreite ist genau die Summe der sichtbaren Elemente (statt fest 400 px).
     */
    public static void oneLineWhenPossible(javafx.scene.layout.FlowPane fp) {
        Runnable calc = () -> {
            double w = 0;
            int n = 0;
            for (javafx.scene.Node c : fp.getChildren()) {
                if (!c.isManaged()) continue;
                w += c.prefWidth(-1);
                n++;
            }
            w += Math.max(0, n - 1) * fp.getHgap() + fp.getInsets().getLeft() + fp.getInsets().getRight() + 1;
            if (Math.abs(fp.getPrefWrapLength() - w) > 0.5) fp.setPrefWrapLength(w);
        };
        fp.needsLayoutProperty().addListener((o, a, b) -> {
            if (b) javafx.application.Platform.runLater(calc);
        });
        javafx.application.Platform.runLater(calc);
    }
}
