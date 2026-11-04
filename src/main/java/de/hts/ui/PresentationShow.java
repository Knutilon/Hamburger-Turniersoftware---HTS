package de.hts.ui;

import de.hts.tournament.Entrant;
import de.hts.tournament.KnockoutBracket;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.PresentationPlan;
import de.hts.tournament.PresentationPlan.Station;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Anzeige auf dem zweiten Bildschirm: reine Anzeige ohne Bedienung, die eingeschalteten Stationen der
 * {@link PresentationPlan Präsentationsansicht} rotieren; was nicht auf eine Seite passt, verteilt sich auf mehrere
 * Seiten. Esc beendet die Anzeige.
 */
public final class PresentationShow {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int RESULT_ROWS_PER_PAGE = 20;
    private static final double GAP = 18;
    /** Mindestgröße für Gruppen und Tische: nicht kleiner als die normale HTS-Schrift – sonst mehr Seiten */
    private static final double MIN_SCALE = 1.0;
    private static final double TABLE_CARD_WIDTH = 340;

    private final Tournament tournament;
    private final DrawView painter;
    private final Stage stage = new Stage();
    private final Label titleLabel = new Label();
    private final Label stateChip = new Label();
    private final Label pageLabel = new Label();
    private final Label clock = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final StackPane body = new StackPane();
    private final Timeline timer;
    private final Runnable onClosed;

    /** Teile der laufenden Rotation: je Station eine oder mehrere Seiten */
    private List<Part> parts = List.of();
    private int partIndex = -1;
    private int subPage;
    private FitPane shown;
    private double elapsed;
    private long renderedChange = -1;

    /** Darstellung eines Teils: ganz einpassen, seitenweise (nur zwischen ganzen Karten/Zeilen) oder eigenes Raster */
    private enum Fit { WHOLE, PAGED, GRID }

    private record Part(Station station, String title, boolean finished, Supplier<Node> content, Fit fit, int page, int pages) {
    }

    private PresentationShow(Tournament tournament, Screen screen, boolean preview, Runnable onClosed) {
        this.tournament = tournament;
        this.painter = DrawView.forPresentation(tournament);
        this.onClosed = onClosed;

        Label name = new Label(tournament.getName() == null ? "" : tournament.getName());
        name.getStyleClass().add("pres-tournament");
        titleLabel.getStyleClass().add("pres-title");
        stateChip.getStyleClass().add("pres-chip");
        stateChip.managedProperty().bind(stateChip.visibleProperty());
        pageLabel.getStyleClass().add("pres-page");
        clock.getStyleClass().add("pres-clock");
        Region sp1 = new Region(), sp2 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        HBox.setHgrow(sp2, Priority.ALWAYS);
        HBox bar = new HBox(16, name, sp1, titleLabel, stateChip, pageLabel, sp2, clock);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("pres-bar");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("pres-progress");
        body.setPadding(new Insets(18, 24, 18, 24));
        body.getStyleClass().add("pres-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(bar, progress, body);
        root.getStyleClass().add("pres-root");

        Rectangle2D b = screen.getBounds();
        Scene scene = preview ? new Scene(root, 1280, 720) : new Scene(root, b.getWidth(), b.getHeight());
        scene.getStylesheets().add(PresentationShow.class.getResource("/de/hts/ui/hts.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close();
        });
        stage.setScene(scene);
        stage.setTitle("HTS – Präsentationsansicht" + (preview ? " (Vorschau, Esc beendet)" : ""));
        stage.getIcons().add(AppHeader.logo(32));
        if (!preview) {
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setX(b.getMinX());
            stage.setY(b.getMinY());
            stage.setWidth(b.getWidth());
            stage.setHeight(b.getHeight());
        }
        stage.setOnHidden(e -> {
            timerStop();
            if (this.onClosed != null) this.onClosed.run();
        });
        timer = new Timeline(new KeyFrame(Duration.millis(200), e -> tick(0.2)));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    /** Anzeige starten: auf {@code screen} randlos bzw. als Vorschau in einem normalen Fenster. */
    public static PresentationShow start(Tournament t, Screen screen, boolean preview, Runnable onClosed) {
        PresentationShow s = new PresentationShow(t, screen, preview, onClosed);
        s.stage.show();
        s.next();
        s.timer.play();
        return s;
    }

    public void close() {
        timerStop();
        stage.hide();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    private void timerStop() {
        if (timer != null) timer.stop();
    }

    private void tick(double dt) {
        clock.setText(LocalTime.now().format(CLOCK));
        elapsed += dt;
        double secs = tournament.getPresentationSeconds();
        progress.setProgress(Math.min(1, elapsed / secs));
        if (elapsed >= secs) {
            next();
        } else if (tournament.getChangeCount() != renderedChange && partIndex >= 0 && partIndex < parts.size()) {
            render();      // neues Ergebnis: aktuelle Seite neu zeichnen
        }
    }

    /** Zur nächsten Seite: weitere Seite desselben Teils, sonst nächster Teil, nach dem letzten neu planen. */
    private void next() {
        elapsed = 0;
        progress.setProgress(0);
        if (shown != null && shown.pageCount() > subPage + 1) {
            subPage++;
            render();
            return;
        }
        subPage = 0;
        partIndex++;
        if (partIndex >= parts.size()) {
            parts = plan();
            partIndex = 0;
        }
        render();
    }

    private void render() {
        renderedChange = tournament.getChangeCount();
        clock.setText(LocalTime.now().format(CLOCK));
        if (parts.isEmpty()) {
            titleLabel.setText("Keine Ansicht ausgewählt");
            stateChip.setVisible(false);
            pageLabel.setText("");
            Label l = new Label("In der Präsentationsansicht ist keine Ansicht eingeschaltet.");
            l.getStyleClass().add("pres-empty");
            body.getChildren().setAll(l);
            shown = null;
            return;
        }
        Part p = parts.get(partIndex);
        Node content;
        try {
            content = p.content().get();
        } catch (RuntimeException ex) {
            content = new Label("Diese Ansicht kann gerade nicht angezeigt werden.");
        }
        if (p.fit() == Fit.GRID) {
            shown = null;
            body.getChildren().setAll(content);
        } else {
            FitPane fit = new FitPane(content, p.fit() == Fit.PAGED, p.fit() == Fit.PAGED ? 1.6 : 2.0);
            fit.setPage(subPage);
            shown = fit;
            body.getChildren().setAll(fit);
            fit.pageCountProperty = () -> updatePageLabel(p);
        }
        titleLabel.setText(p.title());
        boolean upcoming = p.station() != null && p.station().upcoming();
        stateChip.setText(upcoming ? "noch nicht gestartet" : "abgeschlossen");
        stateChip.setVisible(p.finished() || upcoming);
        updatePageLabel(p);
    }

    private void updatePageLabel(Part p) {
        int pages = p.pages() > 1 ? p.pages() : shown == null ? 1 : shown.pageCount();
        int page = p.pages() > 1 ? p.page() : subPage;
        pageLabel.setText(pages > 1 ? "Seite " + (page + 1) + " / " + pages : "");
    }

    // --- Planung ---------------------------------------------------------------------------------

    private List<Part> plan() {
        List<Part> out = new ArrayList<>();
        for (Station s : PresentationPlan.active(tournament)) out.addAll(partsOf(s));
        return out;
    }

    /** Verfügbare Fläche für den Inhalt (ohne Kopfleiste und Rand). */
    private double[] area() {
        Scene sc = stage.getScene();
        sc.getRoot().applyCss();
        sc.getRoot().layout();
        double w = body.getWidth() > 0 ? body.getWidth() - 48 : sc.getWidth() - 48;
        double h = body.getHeight() > 0 ? body.getHeight() - 36 : sc.getHeight() - 120;
        return new double[]{Math.max(200, w), Math.max(150, h)};
    }

    /** Natürliche Größe eines Knotens mit dem HTS-Stil (zum Planen der Seiten). */
    private double[] measure(Node n) {
        javafx.scene.Group g = new javafx.scene.Group(n);
        Scene sc = new Scene(g);
        sc.getStylesheets().addAll(stage.getScene().getStylesheets());
        g.applyCss();
        g.layout();
        javafx.geometry.Bounds b = n.getLayoutBounds();
        g.getChildren().clear();
        return new double[]{b.getWidth(), b.getHeight()};
    }

    /** Teile (Seiten) einer Station – Gruppen, Tische und Endergebnisse blockweise, KO-Bäume in Blöcken. */
    List<Part> partsOf(Station s) {
        List<Part> out = new ArrayList<>();
        switch (s.kind()) {
            case PHASE -> {
                PlayingClass ph = s.phase();
                PlayingClass root = tournament.rootOf(ph);
                String title = root.getName() + " · " + DrawView.phaseTabTitle(tournament.phaseNumber(ph), ph);
                TournamentMode m = ph.getMode();
                if (m == TournamentMode.GROUPS && !ph.getGroups().isEmpty()) {
                    groupParts(s, ph, title, out);
                } else if (m == TournamentMode.KNOCKOUT && !ph.getDraw().isEmpty()) {
                    // höchstens 4 Runden nebeneinander (gut lesbar), ab der ersten Runde mit offenem Spiel; reicht die
                    // Höhe nicht, je Seite ein Block der ersten gezeigten Runde (z. B. 8 Spiele) samt Folgespielen –
                    // Karten werden nie zerschnitten, die Rundentitel stehen auf jeder Seite
                    int rounds = new KnockoutBracket(tournament.drawSlots(ph)).getRounds().size();
                    int from = Math.max(0, Math.min(firstRound(ph, s.finished()), rounds - 4));
                    int cols = Math.max(1, Math.min(4, rounds - from));
                    int first = 1 << Math.max(0, rounds - 1 - from);          // Spiele der ersten gezeigten Runde
                    int block = 1 << (cols - 1);
                    int pages = Math.max(1, first / block);
                    for (int k = 0; k < pages; k++) {
                        int bf = k * block, bs = pages > 1 ? block : 0;
                        out.add(new Part(s, title, s.finished(), () -> painter.presentBracket(ph, from, 4, bf, bs), Fit.WHOLE, k, pages));
                    }
                } else if (m == TournamentMode.QUALIFICATION) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentQualification(ph), Fit.PAGED, 0, 1));
                } else if (m == TournamentMode.SWISS && ph.isDrawn()) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentSwiss(ph), Fit.PAGED, 0, 1));
                } else if (m == TournamentMode.PLACEMENT) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentPlacement(ph), Fit.PAGED, 0, 1));
                }
            }
            case TABLES -> tableParts(s, out);
            case NEXT -> out.add(new Part(s, "Die nächsten 16 Spiele", false, this::nextPage, Fit.WHOLE, 0, 1));
            case RESULT -> {
                PlayingClass root = s.phase();
                int rows = tournament.finalResult(root).rows().size();
                int pages = Math.max(1, (rows + RESULT_ROWS_PER_PAGE - 1) / RESULT_ROWS_PER_PAGE);
                for (int k = 0; k < pages; k++) {
                    int from = k * RESULT_ROWS_PER_PAGE;
                    out.add(new Part(s, root.getName() + " · Endergebnis", true, () -> resultPage(root, from), Fit.WHOLE, k, pages));
                }
            }
        }
        return out;
    }

    /**
     * Gruppen je nach Auflösung: höchstens 4 je Bildschirm in einem festen 2×2-Raster (gleich große Zellen, feste
     * Plätze links oben, rechts oben, links unten, rechts unten); passen nur 2, nebeneinander (bei ungerader Zahl die
     * letzte links); passt nur eine, ohne Raster. Nie kleiner als die normale Schrift – dann mehr Seiten.
     */
    private void groupParts(Station s, PlayingClass ph, String title, List<Part> out) {
        int n = ph.getGroups().size();
        if (n == 1) {
            out.add(new Part(s, title, s.finished(), () -> painter.presentGroupCard(ph, 0), Fit.WHOLE, 0, 1));
            return;
        }
        double maxW = 1, maxH = 1;
        for (int g = 0; g < n; g++) {
            double[] d = measure(painter.presentGroupCard(ph, g));
            maxW = Math.max(maxW, d[0]);
            maxH = Math.max(maxH, d[1]);
        }
        double[] a = area();
        double cellW = (a[0] - GAP) / 2, halfH = (a[1] - GAP) / 2;
        int cols, rows;
        if (cellW >= maxW * MIN_SCALE && halfH >= maxH * MIN_SCALE) {
            cols = 2;
            rows = 2;
        } else if (cellW >= maxW * MIN_SCALE && a[1] >= maxH * MIN_SCALE) {
            cols = 2;
            rows = 1;
        } else {
            cols = 1;
            rows = 1;
        }
        int per = cols * rows, pages = (n + per - 1) / per;
        double rw = maxW, rh = maxH;
        for (int k = 0; k < pages; k++) {
            int from = k * per, to = Math.min(n, from + per);
            int c0 = cols, r0 = rows;
            if (per == 1) {
                int g = from;
                out.add(new Part(s, title, s.finished(), () -> painter.presentGroupCard(ph, g), Fit.WHOLE, k, pages));
            } else {
                out.add(new Part(s, title, s.finished(), () -> {
                    List<Node> cards = new ArrayList<>();
                    for (int g = from; g < to; g++) cards.add(painter.presentGroupCard(ph, g));
                    return new CellGrid(cards, c0, r0, rw, rh, 1.6);
                }, Fit.GRID, k, pages));
            }
        }
    }

    private List<Integer> busyTables() {
        List<Integer> busy = new ArrayList<>();
        for (int t = 1; t <= tournament.getTableCount(); t++) if (tournament.activeMatchAt(t).isPresent()) busy.add(t);
        return busy;
    }

    /**
     * Tische: dynamisch so groß wie möglich in einem Raster gleich großer Zellen; reicht die Fläche bei normaler
     * Schriftgröße nicht, auf mehrere Seiten verteilt – nur ganze Tische je Seite.
     */
    private void tableParts(Station s, List<Part> out) {
        List<Integer> busy = busyTables();
        if (busy.isEmpty()) {
            out.add(new Part(s, "Tische", false, () -> {
                Label l = new Label("Zurzeit läuft an keinem Tisch ein Spiel.");
                l.getStyleClass().add("pres-empty");
                return l;
            }, Fit.WHOLE, 0, 1));
            return;
        }
        int n = busy.size();
        double maxH = 1;
        for (int t : busy) maxH = Math.max(maxH, measure(tableCard(t))[1]);
        double[] a = area();
        int bestCols = 1;
        double best = 0;
        for (int c = 1; c <= n; c++) {
            int r = (n + c - 1) / c;
            double sc = Math.min((a[0] - (c - 1) * GAP) / c / TABLE_CARD_WIDTH, (a[1] - (r - 1) * GAP) / r / maxH);
            if (sc > best + 1e-9) {
                best = sc;
                bestCols = c;
            }
        }
        int cols, rows;
        if (best >= MIN_SCALE) {
            cols = bestCols;
            rows = (n + cols - 1) / cols;
        } else {
            cols = Math.max(1, (int) Math.floor((a[0] + GAP) / (TABLE_CARD_WIDTH * MIN_SCALE + GAP)));
            rows = Math.max(1, (int) Math.floor((a[1] + GAP) / (maxH * MIN_SCALE + GAP)));
        }
        int per = cols * rows, pages = (n + per - 1) / per;
        double rh = maxH;
        for (int k = 0; k < pages; k++) {
            int from = k * per, c0 = cols, r0 = rows;
            out.add(new Part(s, "Tische", false, () -> {
                List<Integer> now = busyTables();
                List<Node> cards = new ArrayList<>();
                for (int i = from; i < Math.min(now.size(), from + per); i++) cards.add(tableCard(now.get(i)));
                if (cards.isEmpty()) {
                    Label l = new Label("Zurzeit läuft an keinem Tisch ein Spiel.");
                    l.getStyleClass().add("pres-empty");
                    return l;
                }
                return new CellGrid(cards, c0, r0, TABLE_CARD_WIDTH, rh, 2.0);
            }, Fit.GRID, k, pages));
        }
    }

    /** Laufende KO-Phase: ab der ersten Runde mit offenem Spiel; beendete: ab dem Viertelfinale. */
    private int firstRound(PlayingClass ph, boolean finished) {
        KnockoutBracket b = new KnockoutBracket(tournament.drawSlots(ph));
        int rounds = b.getRounds().size();
        if (finished) return Math.max(0, rounds - 3);
        java.util.Map<String, Match> byPos = new java.util.HashMap<>();
        for (Match m : tournament.matchesOf(ph)) byPos.put(m.getKoRound() + ":" + m.getKoIndex(), m);
        for (int r = 0; r < rounds; r++) {
            List<KnockoutBracket.Match> ms = b.getRounds().get(r).matches();
            for (int i = 0; i < ms.size(); i++) {
                if (ms.get(i).isBye()) continue;
                Match m = byPos.get(r + ":" + i);
                if (m == null || m.getStatus() != MatchStatus.DONE) return r;
            }
        }
        return Math.max(0, rounds - 3);
    }

    // --- Seiten ohne Gegenstück in der Turnieransicht ------------------------------------------------

    private Node tableCard(int t) {
        Match m = tournament.activeMatchAt(t).orElseThrow();
        Label tn = new Label(tournament.tableName(t));
        tn.getStyleClass().add("round-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(8, tn, sp, DrawView.statusChip(m.getStatus()));
        head.setAlignment(Pos.CENTER_LEFT);
        Label what = new Label(m.getClassName() + " · " + m.getStage() + " · Spiel " + m.getNumber());
        what.getStyleClass().add("hint");
        Label a = new Label(tournament.partyFullName(m, m.getA()));
        Label vs = new Label("gegen");
        vs.getStyleClass().add("hint");
        Label bb = new Label(tournament.partyFullName(m, m.getB()));
        a.getStyleClass().add("table-player");
        bb.getStyleClass().add("table-player");
        VBox card = new VBox(6, head, what, a, vs, bb);
        card.getStyleClass().add("table-card");
        if (m.getStatus() == MatchStatus.PAUSED) card.getStyleClass().add("table-paused");
        card.setPrefWidth(TABLE_CARD_WIDTH);
        card.setMinWidth(TABLE_CARD_WIDTH);
        card.setMaxWidth(TABLE_CARD_WIDTH);
        return card;
    }

    private Node nextPage() {
        List<Match> q = tournament.queue();
        if (q.isEmpty()) {
            Label l = new Label("Zurzeit sind keine Spiele eingereiht.");
            l.getStyleClass().add("pres-empty");
            return l;
        }
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(6);
        String[] heads = {"Nr.", "Klasse", "Phase / Runde", "Spiel", "Begegnung"};
        for (int k = 0; k < heads.length; k++) {
            Label h = new Label(heads[k]);
            h.getStyleClass().add("table-head");
            g.add(h, k, 0);
        }
        for (int i = 0; i < Math.min(16, q.size()); i++) {
            Match m = q.get(i);
            Label nr = new Label(String.valueOf(i + 1));
            nr.getStyleClass().add("pos");
            Label cls = new Label(m.getClassName());
            Label st = new Label(m.getStage());
            Label no = new Label(String.valueOf(m.getNumber()));
            Label who = new Label(tournament.partyFullName(m, m.getA()) + "  –  " + tournament.partyFullName(m, m.getB()));
            who.getStyleClass().add("match-name");
            for (Label l : new Label[]{cls, st, no}) l.getStyleClass().add("hint");
            g.addRow(i + 1, nr, cls, st, no, who);
        }
        return g;
    }

    private Node resultPage(PlayingClass root, int from) {
        List<Tournament.Placing> rows = tournament.finalResult(root).rows();
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(6);
        String[] heads = {"Platz", "Name", "Verein", ""};
        for (int k = 0; k < heads.length; k++) {
            Label h = new Label(heads[k]);
            h.getStyleClass().add("table-head");
            g.add(h, k, 0);
        }
        int r = 1;
        for (int i = from; i < Math.min(rows.size(), from + RESULT_ROWS_PER_PAGE); i++) {
            Tournament.Placing p = rows.get(i);
            Entrant e = p.entrant();
            Label pl = new Label(p.from() == p.to() ? p.from() + "." : p.from() + ".–" + p.to() + ".");
            pl.getStyleClass().add(p.from() <= 3 ? "seed" : "pos");
            Label nm = new Label(e.name());
            nm.getStyleClass().add("match-name");
            Label club = new Label(e.detail() == null ? "" : e.detail());
            club.getStyleClass().add("hint");
            Label info = new Label(p.info() == null ? "" : p.info());
            info.getStyleClass().add("hint");
            g.addRow(r++, pl, nm, club, info);
        }
        return g;
    }

    // --- Einpassen ---------------------------------------------------------------------------------

    /**
     * Festes Raster gleich großer Zellen (links oben, rechts oben, links unten, …): jede Karte oben links in ihrer
     * Zelle, alle mit demselben Maßstab (aus der größten Karte der Station), freie Zellen bleiben leer.
     */
    static final class CellGrid extends Pane {
        private final List<Node> cards;
        private final int cols, rows;
        private final double refW, refH, maxScale;

        CellGrid(List<Node> cards, int cols, int rows, double refW, double refH, double maxScale) {
            this.cards = cards;
            this.cols = cols;
            this.rows = rows;
            this.refW = refW;
            this.refH = refH;
            this.maxScale = maxScale;
            for (Node c : cards) c.setManaged(false);
            getChildren().addAll(cards);
            setMinSize(0, 0);
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            double cellW = (w - (cols - 1) * GAP) / cols, cellH = (h - (rows - 1) * GAP) / rows;
            double s = Math.min(maxScale, Math.min(cellW / refW, cellH / refH));
            for (int k = 0; k < cards.size(); k++) {
                Node c = cards.get(k);
                c.autosize();
                if (c instanceof Region r) r.resize(Math.max(r.getWidth(), refW), r.getHeight());     // gleich breite Karten
                c.getTransforms().setAll(new javafx.scene.transform.Scale(s, s, 0, 0));
                c.relocate((k % cols) * (cellW + GAP), (k / cols) * (cellH + GAP));
            }
        }
    }

    /**
     * Passt den Inhalt in die verfügbare Fläche: ohne Blättern ganz (vergrößert bis {@code maxScale}); mit Blättern
     * nach der Breite, die Höhe verteilt sich auf Seiten – getrennt wird nur zwischen ganzen Karten und Zeilen.
     */
    static final class FitPane extends Pane {
        private final Node child;
        private final boolean paged;
        private final double maxScale;
        private final Rectangle clip = new Rectangle();
        private int page;
        private int pages = 1;
        Runnable pageCountProperty;

        FitPane(Node child, boolean paged, double maxScale) {
            this.child = child;
            this.paged = paged;
            this.maxScale = maxScale;
            child.setManaged(false);
            getChildren().add(child);
            clip.widthProperty().bind(widthProperty());
            clip.setHeight(0);
            setClip(clip);
            setMinSize(0, 0);
        }

        void setPage(int p) {
            page = p;
            requestLayout();
        }

        int pageCount() {
            return pages;
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            child.autosize();
            if (child instanceof javafx.scene.Parent p) p.layout();
            double cw = Math.max(1, child.getLayoutBounds().getWidth()), ch = Math.max(1, child.getLayoutBounds().getHeight());
            double s = paged ? Math.min(maxScale, w / cw) : Math.min(maxScale, Math.min(w / cw, h / ch));
            if (paged && ch * s <= h) s = Math.min(maxScale, Math.min(w / cw, h / ch));
            List<Double> starts = new ArrayList<>();
            starts.add(0.0);
            if (paged && ch * s > h + 1) starts = pageStarts(child, ch, h / s);
            int newPages = starts.size();
            if (newPages != pages) {
                pages = newPages;
                if (pageCountProperty != null) pageCountProperty.run();
            }
            int pg = Math.min(page, pages - 1);
            child.getTransforms().setAll(new javafx.scene.transform.Scale(s, s, 0, 0));
            double x = Math.max(0, (w - cw * s) / 2);
            if (pages > 1) {
                double y0 = starts.get(pg), y1 = pg + 1 < pages ? starts.get(pg + 1) : ch;
                child.relocate(x, -y0 * s);
                clip.setHeight(Math.min(h, (y1 - y0) * s + 1));     // Teil der nächsten Karte nicht anschneiden
            } else {
                child.relocate(x, Math.max(0, (h - ch * s) / 2));
                clip.setHeight(h);
            }
        }

        /** Seitenanfänge (in Koordinaten des Inhalts): je Seite so viel wie passt, Schnitt nur zwischen ganzen Karten/Zeilen. */
        static List<Double> pageStarts(Node content, double totalH, double pageH) {
            List<double[]> atoms = new ArrayList<>();
            collect(content, 0, pageH, atoms, true);
            java.util.TreeSet<Double> cuts = new java.util.TreeSet<>();
            for (double[] at : atoms) {
                double y = at[1];
                boolean inside = false;
                for (double[] o : atoms) {
                    if (o[0] < y - 0.5 && o[1] > y + 0.5) {
                        inside = true;
                        break;
                    }
                }
                if (!inside) cuts.add(y);
            }
            List<Double> starts = new ArrayList<>();
            double y0 = 0;
            while (y0 < totalH - 1 && starts.size() < 200) {
                starts.add(y0);
                Double cut = cuts.floor(y0 + pageH);
                double next = cut != null && cut > y0 + 1 ? cut : y0 + pageH;
                // nach dem Schnitt mit der nächsten Karte beginnen (Zwischenraum überspringen)
                Double after = null;
                for (double[] at : atoms) if (at[0] >= next - 0.5 && (after == null || at[0] < after)) after = at[0];
                if (after == null) break;
                y0 = Math.max(next, after);
            }
            if (starts.isEmpty()) starts.add(0.0);
            return starts;
        }

        /** Unteilbare Teile: Texte, Bedienelemente und Karten (sofern sie auf eine Seite passen). */
        private static void collect(Node n, double offY, double pageH, List<double[]> out, boolean root) {
            if (!root) {
                javafx.geometry.Bounds b = n.getBoundsInParent();
                double top = offY + b.getMinY(), bottom = offY + b.getMaxY();
                if (!n.isVisible() || b.getHeight() <= 0) return;
                if (n instanceof javafx.scene.shape.Shape && !(n instanceof javafx.scene.text.Text)) return;
                boolean card = n instanceof javafx.scene.control.Control || n instanceof javafx.scene.text.Text
                        || n.getStyleClass().stream().anyMatch(c -> c.contains("card") || c.equals("chip"));
                if (card && b.getHeight() <= pageH) {
                    out.add(new double[]{top, bottom});
                    return;
                }
            }
            if (n instanceof javafx.scene.Parent p) {
                double base = root ? 0 : offY + n.getLayoutY() + n.getTranslateY();
                for (Node c : p.getChildrenUnmodifiable()) collect(c, base, pageH, out, false);
            }
        }
    }
}
