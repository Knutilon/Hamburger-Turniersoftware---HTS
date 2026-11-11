/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
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
                double pw = c.prefWidth(-1);
                if (c instanceof javafx.scene.control.Labeled l && l.isWrapText()) pw = Math.min(pw, 360);   // bricht selbst um
                w += pw;
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

    /** 0.42 (#74): größte Breite eines mitwachsenden Suchfelds. */
    public static final double SEARCH_MAX_WIDTH = 450;

    /**
     * 0.42 (#74): Suchfeld mindestens so breit wie sein Platzhalter (nie abgeschnitten), auf breiten Bildschirmen
     * wächst es mit dem freien Platz seiner Zeile bis {@link #SEARCH_MAX_WIDTH}. In einer HBox über Hgrow, in einer
     * FlowPane über den Platz, den die übrigen Elemente der Zeile übrig lassen (wird es eng, bricht die Zeile um).
     */
    public static void growingSearch(javafx.scene.control.TextField f) {
        double min = Math.ceil(textWidth(f.getPromptText(), false, 13) + 36);
        f.setPrefColumnCount(1);
        f.setMinWidth(min);
        f.setPrefWidth(min);
        f.setMaxWidth(SEARCH_MAX_WIDTH);
        javafx.scene.layout.HBox.setHgrow(f, javafx.scene.layout.Priority.ALWAYS);
        javafx.beans.value.ChangeListener<Object> relayout = (o, a, b) -> fitInFlow(f, min);
        f.parentProperty().addListener((o, oldP, newP) -> {
            if (oldP instanceof javafx.scene.layout.FlowPane old) old.widthProperty().removeListener(relayout);
            if (newP instanceof javafx.scene.layout.FlowPane fp) {
                fp.widthProperty().addListener(relayout);
                fp.needsLayoutProperty().addListener((oo, aa, bb) -> {
                    if (bb) javafx.application.Platform.runLater(() -> fitInFlow(f, min));
                });
                javafx.application.Platform.runLater(() -> fitInFlow(f, min));
            }
        });
    }

    private static void fitInFlow(javafx.scene.control.TextField f, double min) {
        if (!(f.getParent() instanceof javafx.scene.layout.FlowPane fp) || fp.getWidth() <= 0) return;
        double others = 0;
        int n = 0;
        for (javafx.scene.Node c : fp.getChildren()) {
            if (c == f || !c.isManaged()) continue;
            others += c.prefWidth(-1);
            n++;
        }
        double free = fp.getWidth() - fp.getInsets().getLeft() - fp.getInsets().getRight() - others - n * fp.getHgap() - 2;
        double w = Math.max(min, Math.min(SEARCH_MAX_WIDTH, Math.floor(free)));
        if (Math.abs(f.getPrefWidth() - w) > 0.5) f.setPrefWidth(w);
    }
}
