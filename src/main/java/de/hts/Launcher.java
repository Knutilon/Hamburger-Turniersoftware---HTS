package de.hts;

/**
 * Startpunkt für die ausführbare JAR-Datei.
 * (Eine Klasse, die nicht von javafx.application.Application erbt, ist nötig,
 * damit JavaFX aus einer "fat jar" ohne Modulpfad startet.)
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        HtsApp.main(args);
    }
}
