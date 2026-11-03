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
    private static final int GROUPS_PER_PAGE = 6;
    private static final int TABLES_PER_PAGE = 12;
    private static final int RESULT_ROWS_PER_PAGE = 20;

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

    private record Part(Station station, String title, boolean finished, Supplier<Node> content, boolean paged, int page, int pages) {
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
        FitPane fit = new FitPane(content, p.paged(), p.paged() ? 1.6 : 2.0);
        fit.setPage(subPage);
        shown = fit;
        body.getChildren().setAll(fit);
        titleLabel.setText(p.title());
        stateChip.setText("abgeschlossen");
        stateChip.setVisible(p.finished());
        updatePageLabel(p);
        fit.pageCountProperty = () -> updatePageLabel(p);
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

    /** Teile (Seiten) einer Station – Gruppen, Tische und Endergebnisse blockweise, alles andere als ein Teil. */
    List<Part> partsOf(Station s) {
        List<Part> out = new ArrayList<>();
        switch (s.kind()) {
            case PHASE -> {
                PlayingClass ph = s.phase();
                PlayingClass root = tournament.rootOf(ph);
                String title = root.getName() + " · " + DrawView.phaseTabTitle(tournament.phaseNumber(ph), ph);
                TournamentMode m = ph.getMode();
                if (m == TournamentMode.GROUPS && !ph.getGroups().isEmpty()) {
                    int n = ph.getGroups().size(), pages = (n + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE;
                    for (int k = 0; k < pages; k++) {
                        int from = k * GROUPS_PER_PAGE, to = from + GROUPS_PER_PAGE;
                        out.add(new Part(s, title, s.finished(), () -> painter.presentGroups(ph, from, to), false, k, pages));
                    }
                } else if (m == TournamentMode.KNOCKOUT && !ph.getDraw().isEmpty()) {
                    // höchstens 4 Runden nebeneinander (gut lesbar), ab der ersten Runde mit offenem Spiel
                    int rounds = new KnockoutBracket(tournament.drawSlots(ph)).getRounds().size();
                    int from = Math.max(0, Math.min(firstRound(ph, s.finished()), rounds - 4));
                    out.add(new Part(s, title, s.finished(), () -> painter.presentBracket(ph, from, 4), true, 0, 1));
                } else if (m == TournamentMode.QUALIFICATION) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentQualification(ph), true, 0, 1));
                } else if (m == TournamentMode.SWISS && ph.isDrawn()) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentSwiss(ph), true, 0, 1));
                } else if (m == TournamentMode.PLACEMENT) {
                    out.add(new Part(s, title, s.finished(), () -> painter.presentPlacement(ph), true, 0, 1));
                }
            }
            case TABLES -> {
                List<Integer> busy = new ArrayList<>();
                for (int t = 1; t <= tournament.getTableCount(); t++) if (tournament.activeMatchAt(t).isPresent()) busy.add(t);
                int pages = Math.max(1, (busy.size() + TABLES_PER_PAGE - 1) / TABLES_PER_PAGE);
                for (int k = 0; k < pages; k++) {
                    int from = k * TABLES_PER_PAGE;
                    out.add(new Part(s, "Tische", false, () -> tablesPage(from), false, k, pages));
                }
            }
            case NEXT -> out.add(new Part(s, "Die nächsten 16 Spiele", false, this::nextPage, false, 0, 1));
            case RESULT -> {
                PlayingClass root = s.phase();
                int rows = tournament.finalResult(root).rows().size();
                int pages = Math.max(1, (rows + RESULT_ROWS_PER_PAGE - 1) / RESULT_ROWS_PER_PAGE);
                for (int k = 0; k < pages; k++) {
                    int from = k * RESULT_ROWS_PER_PAGE;
                    out.add(new Part(s, root.getName() + " · Endergebnis", true, () -> resultPage(root, from), false, k, pages));
                }
            }
        }
        return out;
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

    private Node tablesPage(int from) {
        List<Integer> busy = new ArrayList<>();
        for (int t = 1; t <= tournament.getTableCount(); t++) if (tournament.activeMatchAt(t).isPresent()) busy.add(t);
        if (busy.isEmpty()) {
            Label l = new Label("Zurzeit läuft an keinem Tisch ein Spiel.");
            l.getStyleClass().add("pres-empty");
            return l;
        }
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        int k = 0;
        for (int i = from; i < Math.min(busy.size(), from + TABLES_PER_PAGE); i++, k++) {
            int t = busy.get(i);
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
            card.setPrefWidth(340);
            card.setMinWidth(340);
            card.setMaxWidth(340);
            card.setMaxHeight(Double.MAX_VALUE);
            grid.add(card, k % 4, k / 4);
        }
        return grid;
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
     * Passt den Inhalt in die verfügbare Fläche: ohne Blättern ganz (vergrößert bis {@code maxScale}); mit Blättern
     * nach der Breite, die Höhe verteilt sich auf Seiten.
     */
    static final class FitPane extends Pane {
        private final Node child;
        private final boolean paged;
        private final double maxScale;
        private int page;
        private int pages = 1;
        Runnable pageCountProperty;

        FitPane(Node child, boolean paged, double maxScale) {
            this.child = child;
            this.paged = paged;
            this.maxScale = maxScale;
            child.setManaged(false);
            getChildren().add(child);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(widthProperty());
            clip.heightProperty().bind(heightProperty());
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
            double cw = Math.max(1, child.getLayoutBounds().getWidth()), ch = Math.max(1, child.getLayoutBounds().getHeight());
            double s = paged ? Math.min(maxScale, w / cw) : Math.min(maxScale, Math.min(w / cw, h / ch));
            if (paged && ch * s <= h) s = Math.min(maxScale, Math.min(w / cw, h / ch));
            int newPages = paged ? Math.max(1, (int) Math.ceil(ch * s / h - 0.02)) : 1;
            if (newPages != pages) {
                pages = newPages;
                if (pageCountProperty != null) pageCountProperty.run();
            }
            int pg = Math.min(page, pages - 1);
            child.getTransforms().setAll(new javafx.scene.transform.Scale(s, s, 0, 0));
            double x = Math.max(0, (w - cw * s) / 2);
            double y = pages > 1 ? -pg * h : Math.max(0, (h - ch * s) / 2);
            child.relocate(x, y);
        }
    }
}
