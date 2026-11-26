/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
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
    public static final String VERSION = "1.12.0";

    /** Repository auf GitHub (Quellcode, Feature- und Bug-Sammlung) */
    public static final String REPO_URL = "https://github.com/Knutilon/Hamburger-Turniersoftware---HTS";
    /** 0.42 (#72/#73): Urheber (Copyright) – einheitlich in Info-Fenster, NOTICE, README und Datei-Köpfen. */
    public static final String AUTHOR = "Richard Johan Frederich Michaelis, Hamburg, Germany";
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
        // 1.7.0 (#140): Protokoll und Behandlung unerwarteter Fehler (auch im Programm-Thread)
        // 1.8.0 (#152): Protokoll, Ausrichter und Programmeinstellungen im Programmordner (dort, wo hts.jar liegt)
        de.hts.persistence.ProgramFolder.migrateFrom(store.getDirectory());
        Log.init(de.hts.persistence.ProgramFolder.dir());
        javafx.stage.Screen scr = javafx.stage.Screen.getPrimary();
        Log.startup((int) scr.getBounds().getWidth() + " × " + (int) scr.getBounds().getHeight() + " bei "
                + Math.round(scr.getOutputScaleX() * 100) + " %");
        Thread.setDefaultUncaughtExceptionHandler((th, ex) -> unexpected(ex));
        Thread.currentThread().setUncaughtExceptionHandler((th, ex) -> unexpected(ex));
        Thread pre = new Thread(de.hts.web.WebSettings::preload, "HTS-Einstellungen");   // 1.7.0 (#142): DPAPI im Hintergrund
        pre.setDaemon(true);
        pre.start();
        if (de.hts.persistence.ProgramFolder.isFallback()) {
            Log.warn("Programmordner nicht beschreibbar – Einstellungen und Protokoll in " + de.hts.persistence.ProgramFolder.dir());
        }
        de.hts.persistence.Organizer.load(de.hts.persistence.ProgramFolder.dir());     // 1.5.0 (#139): Ausrichter (Name, Logo)
        de.hts.ui.DarkMode.install();                                                  // 1.8.0 (#152)
        de.hts.web.PendingDeletes.retryLater();                                        // 1.8.0 (#154): offene Löschaufträge
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

    private static volatile long lastErrorDialog;

    /** 1.7.0 (#140): unerwarteter Fehler – protokollieren, Turnier sichern, Hinweis mit „Fehlerbericht speichern“. */
    private void unexpected(Throwable ex) {
        Log.error("Unerwarteter Fehler", ex);
        javafx.application.Platform.runLater(() -> {
            if (workspace != null) workspace.saveAfterError();
            long now = System.currentTimeMillis();
            if (now - lastErrorDialog < 10_000) return;             // kein Fensterregen bei Folgefehlern
            lastErrorDialog = now;
            de.hts.ui.ErrorReport.showUnexpected(stage, ex);
        });
    }

    private void showOverview() {
        Log.info("Turnierübersicht geöffnet");
        workspace = null;
        overview.refresh();
        root.getChildren().setAll(overview.getRoot());
        stage.titleProperty().unbind();
        stage.setTitle(SHORT_NAME + " – " + NAME);
    }

    private void openTournament(Tournament t) {
        Log.info("Turnier geöffnet: Kennung " + t.getId() + ", " + t.rootClasses().size() + " Klassen, " + t.getMatches().size() + " Spiele");
        workspace = new WorkspaceView(stage, store, t, this::showOverview);
        root.getChildren().setAll(workspace.getRoot());
        stage.titleProperty().bind(t.nameProperty().concat(" – " + SHORT_NAME));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
