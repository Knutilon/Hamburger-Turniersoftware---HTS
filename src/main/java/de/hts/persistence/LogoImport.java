/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Logo des Ausrichters einlesen (#139, 1.7.0 #143): PNG/JPG werden auf höchstens 600 Bildpunkte verkleinert, SVG wird
 * mit JSVG in ein PNG mit 1200 Bildpunkten auf der langen Seite umgerechnet (scharf auf dem A6-Zettel, Transparenz bleibt).
 * Externe Verweise in SVG-Dateien werden nicht geladen.
 */
public final class LogoImport {

    static final int RASTER_MAX = 600;
    static final int SVG_LONG_SIDE = 1200;
    private static final long MAX_FILE = 10L * 1024 * 1024;

    private LogoImport() {
    }

    /** Liest die Datei und liefert das Logo als PNG. */
    public static byte[] toPng(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE) throw new IOException("Die Datei ist größer als 10 MB.");
        byte[] data = Files.readAllBytes(file);
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".svg") || name.endsWith(".svgz") || looksLikeSvg(data) ? svgToPng(data) : rasterToPng(data);
    }

    static boolean looksLikeSvg(byte[] data) {
        String head = new String(data, 0, Math.min(data.length, 512), java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }

    static byte[] rasterToPng(byte[] data) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) throw new IOException("Das Bild lässt sich nicht lesen (bitte PNG, JPG oder SVG).");
        double k = Math.min(1, (double) RASTER_MAX / Math.max(img.getWidth(), img.getHeight()));
        return draw(img.getWidth() * k, img.getHeight() * k, g -> g.drawImage(img, 0, 0,
                (int) Math.round(img.getWidth() * k), (int) Math.round(img.getHeight() * k), null));
    }

    static byte[] svgToPng(byte[] data) throws IOException {
        LoaderContext ctx = LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL).build();
        SVGDocument doc = new SVGLoader().load(new ByteArrayInputStream(data), null, ctx);
        if (doc == null) throw new IOException("Die SVG-Datei lässt sich nicht lesen.");
        var size = doc.size();
        if (!(size.width > 0) || !(size.height > 0)) throw new IOException("Die SVG-Datei hat keine Größe (width/height oder viewBox fehlt).");
        double k = SVG_LONG_SIDE / Math.max(size.width, size.height);
        return draw(size.width * k, size.height * k, g -> {
            g.scale(k, k);
            doc.render(null, g);
        });
    }

    private interface Painter {
        void paint(Graphics2D g);
    }

    private static byte[] draw(double w, double h, Painter p) throws IOException {
        int iw = Math.max(1, (int) Math.round(w)), ih = Math.max(1, (int) Math.round(h));
        BufferedImage out = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            p.paint(g);
        } catch (RuntimeException e) {
            throw new IOException("Das Bild lässt sich nicht umrechnen: " + e.getMessage(), e);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bo);
        return bo.toByteArray();
    }
}
