package de.hts.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Schlichte Linien-Icons (24×24-Raster, Strichstärke 2), als Vektorgrafik gezeichnet.
 * Die Farbe folgt der CSS-Klasse {@code .icon} (Eigenschaft {@code -fx-stroke}).
 */
public final class Icons {

    public static final String PARTICIPANTS =
            "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8z "
                    + "M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";
    public static final String CLASSES = "M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5";
    public static final String BACK = "M19 12H5 M12 19l-7-7 7-7";
    public static final String START = "M6 3l14 9-14 9V3z";
    public static final String SAVE = "M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z M17 21v-8H7v8 M7 3v5h8";
    public static final String UPLOAD = "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M17 8l-5-5-5 5 M12 3v12";
    public static final String PLUS = "M12 5v14 M5 12h14";
    public static final String EDIT = "M12 20h9 M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z";
    public static final String TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
    public static final String CALENDAR = "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M16 2v4 M8 2v4 M3 10h18";
    public static final String TROPHY = "M8 21h8 M12 17v4 M7 4h10v5a5 5 0 0 1-10 0V4z M7 6H4a3 3 0 0 0 3 4 M17 6h3a3 3 0 0 1-3 4";
    public static final String AUTO = "M13 2L3 14h9l-1 8 10-12h-9l1-8z";
    public static final String FOLDER = "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z";
    /** Würfel (Testmodus: Spiele simulieren) */
    public static final String DICE = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z "
            + "M7.5 8h1 M15.5 8h1 M11.5 12h1 M7.5 16h1 M15.5 16h1";
    public static final String HOME = "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10";
    public static final String ARROW_RIGHT = "M5 12h14 M12 5l7 7-7 7";
    public static final String CLOCK = "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z M12 6v6l4 2";

    private Icons() {
    }

    /** Icon in der angegebenen Größe (Pixel). */
    public static Node of(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#1f1f1f"));
        svg.setStrokeWidth(2);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.getStyleClass().add("icon");
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        StackPane box = new StackPane(new Group(svg));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.getStyleClass().add("icon-box");
        return box;
    }

    public static Node of(String path) {
        return of(path, 18);
    }
}
