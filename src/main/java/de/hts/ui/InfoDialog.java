package de.hts.ui;

import de.hts.HtsApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Info-Fenster (Knopf „(i)“ rechts oben): Programm, Version, Signatur, Lizenz (GPL 3.0), Links zur Feature- und
 * Bug-Sammlung auf GitHub und Hinweise zu den mitgelieferten Bibliotheken.
 */
public final class InfoDialog extends Dialog<ButtonType> {

    static final String FEATURES = HtsApp.REPO_URL + "/issues?q=is%3Aissue+label%3AFeature";
    static final String NEW_FEATURE = HtsApp.REPO_URL + "/issues/new?labels=Feature";
    static final String BUGS = HtsApp.REPO_URL + "/issues?q=is%3Aissue+label%3Abug";
    static final String NEW_BUG = HtsApp.REPO_URL + "/issues/new?labels=bug";
    static final String LICENSE = HtsApp.REPO_URL + "/blob/main/LICENSE";

    /** Den Info-Knopf als letzten (rechten) Knopf in die Kopfleiste setzen. */
    public static void install(AppHeader header, java.util.function.Supplier<Window> owner) {
        header.addTool(Icons.INFO, "Info: Version, Lizenz, Feature- und Bug-Sammlung", () -> new InfoDialog(owner.get()).showAndWait());
    }

    public InfoDialog(Window owner) {
        initOwner(owner);
        setTitle("Info – " + HtsApp.SHORT_NAME);
        setHeaderText(null);

        ImageView logo = new ImageView(AppHeader.logo(64));
        Label name = new Label(HtsApp.SHORT_NAME + " – " + HtsApp.NAME);
        name.getStyleClass().add("round-title");
        Label version = new Label("Version " + HtsApp.VERSION);
        version.getStyleClass().add("hint");
        Label signature = new Label("Richard Michaelis");
        signature.getStyleClass().add("info-signature");
        VBox head = new VBox(2, name, version, signature);
        HBox top = new HBox(14, logo, head);
        top.setAlignment(Pos.CENTER_LEFT);

        Label license = new Label("Copyright © 2026 Richard Michaelis. HTS ist freie Software (Open Source) unter der "
                + "GNU General Public License v3.0 (GPL-3.0): Sie dürfen HTS nutzen, verändern und weitergeben. Wer eine "
                + "veränderte Fassung weitergibt, muss sie ebenfalls unter der GPL-3.0 samt Quellcode weitergeben. "
                + "HTS wird ohne jede Gewährleistung bereitgestellt.");
        license.setWrapText(true);
        Hyperlink licenseLink = link("Lizenztext (GPL-3.0)", LICENSE);
        Hyperlink source = link("Quellcode auf GitHub", HtsApp.REPO_URL);

        Label feedbackHead = new Label("Mitmachen");
        feedbackHead.getStyleClass().add("class-name");
        VBox feedback = new VBox(2,
                new HBox(10, link("Feature-Sammlung (Wünsche ansehen)", FEATURES), link("neuen Wunsch melden", NEW_FEATURE)),
                new HBox(10, link("Bug-Sammlung (Fehler ansehen)", BUGS), link("neuen Fehler melden", NEW_BUG)));

        Label libsHead = new Label("Mitgelieferte Bibliotheken");
        libsHead.getStyleClass().add("class-name");
        Label libs = new Label("• Apache PDFBox 3.0.5 – Apache License 2.0\n"
                + "• JavaFX 21 (OpenJFX) – GNU GPL v2 mit Classpath Exception\n"
                + "• Java-Laufzeit Eclipse Temurin 21 – GNU GPL v2 mit Classpath Exception\n"
                + "Die Lizenztexte liegen im Programmordner unter „Lizenzen“ (Java-Laufzeit zusätzlich unter „jre\\legal“).");
        libs.getStyleClass().add("hint");
        libs.setWrapText(true);

        VBox content = new VBox(12, top, license, new HBox(10, licenseLink, source), feedbackHead, feedback, libsHead, libs);
        content.setPadding(new Insets(8, 6, 4, 6));
        content.setPrefWidth(560);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType("Schließen", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
        DialogSizing.fit(this);
    }

    private static Hyperlink link(String text, String url) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> HtsApp.openLink(url));
        h.setTooltip(new javafx.scene.control.Tooltip(url));
        h.setPadding(Insets.EMPTY);
        return h;
    }
}
