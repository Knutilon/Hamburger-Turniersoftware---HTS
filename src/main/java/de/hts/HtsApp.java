package de.hts;

import de.hts.persistence.TournamentStore;
import de.hts.tournament.Tournament;
import de.hts.ui.OverviewView;
import de.hts.ui.WorkspaceView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Hamburger Turniersoftware (HTS) – Hauptanwendung.
 * Startet mit der Turnierübersicht; nach Auswahl eines Turniers folgt die Turnieransicht mit Menü in der Kopfleiste.
 */
public class HtsApp extends Application {

    public static final String NAME = "Hamburger Turniersoftware";
    public static final String SHORT_NAME = "HTS";
    public static final String VERSION = "0.32.0";

    /** Repository auf GitHub (Quellcode, Feature- und Bug-Sammlung) */
    public static final String REPO_URL = "https://github.com/Knutilon/Hamburger-Turniersoftware---HTS";

    private static javafx.application.HostServices hostServices;

    /** Link im Standardbrowser öffnen. */
    public static void openLink(String url) {
        try {
            if (hostServices != null) {
                hostServices.showDocument(url);
                return;
            }
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception | Error ignored) {
            // kein Browser verfügbar
        }
    }

    private final TournamentStore store = TournamentStore.defaultStore();
    private final StackPane root = new StackPane();
    private Stage stage;
    private OverviewView overview;
    private WorkspaceView workspace;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        hostServices = getHostServices();
        overview = new OverviewView(stage, store, this::openTournament);

        Scene scene = new Scene(root, 1200, 760);
        de.hts.ui.Ui.installButtonSizing();
        scene.getStylesheets().add(getClass().getResource("/de/hts/ui/hts.css").toExternalForm());
        stage.setScene(scene);
        stage.getIcons().addAll(de.hts.ui.AppHeader.logo(16), de.hts.ui.AppHeader.logo(32),
                de.hts.ui.AppHeader.logo(64), de.hts.ui.AppHeader.logo(256));
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> {
            if (workspace != null && !workspace.prepareLeave()) {
                e.consume();
            }
        });
        showOverview();
        stage.show();
    }

    private void showOverview() {
        workspace = null;
        overview.refresh();
        root.getChildren().setAll(overview.getRoot());
        stage.titleProperty().unbind();
        stage.setTitle(SHORT_NAME + " – " + NAME);
    }

    private void openTournament(Tournament t) {
        workspace = new WorkspaceView(stage, store, t, this::showOverview);
        root.getChildren().setAll(workspace.getRoot());
        stage.titleProperty().bind(t.nameProperty().concat(" – " + SHORT_NAME));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
