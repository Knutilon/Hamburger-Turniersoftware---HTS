/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import de.hts.Log;
import de.hts.persistence.LogoImport;
import de.hts.report.PdfReports;
import de.hts.tournament.Tournament;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTS 1.7.0 – SVG-Logos (#143), verschlüsselte Ablage (#142), Admin-Passwort (#141, entfallen in 1.12.0 #190), Protokoll (#140), Freistellung
 * 6 × 2 (#145), lesbare Live-Adressen (#147), QR-Aushang (#148).
 */
class Version170Test {

    @Test
    void svgLogoWirdAlsPngMit1200BildpunktenUebernommen() throws Exception {
        Path dir = Files.createTempDirectory("hts-logo");
        Path svg = dir.resolve("logo.svg");
        Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 100\"><rect width=\"200\" height=\"100\" fill=\"#187250\"/>"
                + "<image href=\"file:///etc/passwd\" width=\"10\" height=\"10\"/></svg>");
        byte[] png = LogoImport.toPng(svg);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(1200, img.getWidth());
        assertEquals(600, img.getHeight());
        assertEquals(0x187250, img.getRGB(600, 300) & 0xffffff, "Fläche gezeichnet");
        Path bad = dir.resolve("kaputt.svg");
        Files.writeString(bad, "<svg xmlns=\"http://www.w3.org/2000/svg\"><unclosed></svg");
        assertThrows(java.io.IOException.class, () -> LogoImport.toPng(bad));
        Path dtd = dir.resolve("dtd.svg");
        Files.writeString(dtd, "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><text>&x;</text></svg>");
        try {
            LogoImport.toPng(dtd);                               // darf lesen oder ablehnen – aber nie die Datei einbinden
        } catch (java.io.IOException expected) {
            // in Ordnung
        }
        BufferedImage big = new BufferedImage(1500, 300, BufferedImage.TYPE_INT_RGB);
        Path pngFile = dir.resolve("gross.png");
        ImageIO.write(big, "png", pngFile.toFile());
        BufferedImage small = ImageIO.read(new ByteArrayInputStream(LogoImport.toPng(pngFile)));
        assertEquals(600, small.getWidth(), "PNG/JPG weiter auf 600 Bildpunkte");
    }

    @Test
    void webserverEinstellungenVerschluesseltUndUebernommen() throws Exception {
        Preferences p = Preferences.userRoot().node("hts-test-" + System.nanoTime());
        byte[] key = SecretStore.newKey();
        try {
            p.put("server", "https://hts-livescoring.de");            // Klartext wie in 1.5/1.6
            p.put("key", "geheim-123");
            WebSettings.useForTest(p, () -> key);
            assertEquals("https://hts-livescoring.de", WebSettings.server());
            assertEquals("geheim-123", WebSettings.key());
            assertEquals(WebSettings.State.OK, WebSettings.state());
            assertNull(p.get("server", null), "Klartext gelöscht");
            assertNull(p.get("key", null));
            String enc = p.get("key-enc", "");
            assertTrue(enc.startsWith("v1:") && !enc.contains("geheim"), "verschlüsselt: " + enc);
            String enc2 = new SecretStore(() -> key).encrypt("geheim-123");
            assertNotEquals(enc, enc2, "je Wert eigener Zufalls-IV");
            WebSettings.set("https://neu.example", "k2");
            WebSettings.useForTest(p, () -> key);
            assertEquals("k2", WebSettings.key(), "nach Neustart lesbar");
            WebSettings.useForTest(p, SecretStore::newKey);              // anderer Rechner/Benutzer
            assertEquals(WebSettings.State.UNREADABLE, WebSettings.state());
            assertFalse(WebSettings.configured(), "Veröffentlichen pausiert");
            assertEquals("http•••••.de", WebSettings.mask("https://hts-livescoring.de"));
            assertEquals("••••••", WebSettings.mask("kurz"));
        } finally {
            p.removeNode();
        }
    }

    @Test
    void schluesseldateiNurFuerDenBenutzer() throws Exception {
        Path f = Files.createTempDirectory("hts-key").resolve("sub").resolve("geheimnis.key");
        SecretStore.FileProvider fp = new SecretStore.FileProvider(f);
        byte[] k1 = fp.key();
        assertEquals(32, k1.length);
        assertArrayEquals(k1, new SecretStore.FileProvider(f).key());
        if (f.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(f)));
        }
    }

    @Test
    void protokollUndFehlerbericht() throws Exception {
        Path store = Files.createTempDirectory("hts-store");
        Path dir = store.resolve("Protokoll");
        Files.createDirectories(dir);
        Path old = dir.resolve("hts-" + LocalDate.now().minusDays(40) + ".log");
        Files.writeString(old, "alt");
        Log.init(store);
        try {
            assertFalse(Files.exists(old), "älter als 30 Tage gelöscht");
            Log.info("Turnier geöffnet: Kennung abc");
            Log.error("Testfehler", new IllegalStateException("kaputt"));
            String today = Files.readString(dir.resolve("hts-" + LocalDate.now() + ".log"));
            assertTrue(today.contains("Turnier geöffnet") && today.contains("IllegalStateException: kaputt"));
            ByteArrayOutputStream zip = new ByteArrayOutputStream();
            Log.writeReport(zip, 7);
            Set<String> names = new HashSet<>();
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip.toByteArray()))) {
                for (var e = in.getNextEntry(); e != null; e = in.getNextEntry()) names.add(e.getName());
            }
            assertTrue(names.contains("LIESMICH.txt") && names.contains("hts-" + LocalDate.now() + ".log"), names.toString());
        } finally {
            Log.init(Files.createTempDirectory("hts-leer"));
        }
    }

    @Test
    void freistellungSechsMalZweiAuslosungJeHaelfte() throws Exception {
        java.lang.reflect.Method m = Tournament.class.getDeclaredMethod("drawExempt6x2", Random.class);
        m.setAccessible(true);
        Set<String> seen = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) {
            String[] s = (String[]) m.invoke(null, new Random(seed));
            assertEquals(16, s.length);
            assertEquals(16, new HashSet<>(Arrays.asList(s)).size(), "jeder genau einmal");
            List<String> top = Arrays.asList(s).subList(0, 8), bottom = Arrays.asList(s).subList(8, 16);
            assertTrue(Set.of(s[0], s[7]).equals(Set.of("S1", "S4")), "S1 und S4 oben: " + String.join(" ", s));
            assertTrue(Set.of(s[8], s[15]).equals(Set.of("S2", "S3")), "S2 und S3 unten");
            // Platzarten: oben S–2 · 1–1 · 1–2 · 2–S, unten S–2 · 2–1 · 1–1 · 2–S
            String types = Arrays.stream(s).map(x -> x.startsWith("S") ? "S" : x.substring(1)).reduce("", String::concat);
            assertEquals("S211122S", types.substring(0, 8), String.join(" ", s));
            assertEquals("S221112S", types.substring(8), String.join(" ", s));
            for (List<String> half : List.of(top, bottom)) {
                Set<Character> letters = new HashSet<>();
                for (String x : half) if (!x.startsWith("S")) assertTrue(letters.add(x.charAt(0)), "Gruppe doppelt in einer Hälfte: " + half);
            }
            seen.add(String.join(" ", s));
        }
        assertTrue(seen.size() > 50, "wirklich ausgelost");
        assertTrue(seen.contains("S1 D2 F1 E1 A1 B2 C2 S4 S3 E2 A2 B1 C1 D1 F2 S2")
                || seen.stream().anyMatch(x -> x.startsWith("S1 ")), "Grundschema ist eine mögliche Auslosung");
    }

    @Test
    void lesbareLiveAdresse() {
        assertEquals("Hamburger_Ranglistenturnier_28_11_2026", LiveSlug.of("Hamburger Ranglistenturnier", LocalDate.of(2026, 11, 28)));
        assertEquals("Grosser_Preis_Oeyten_Jugend_U15_01_02_2027", LiveSlug.of("Großer Preis Öyten – Jugend (U15)", LocalDate.of(2027, 2, 1)));
        assertEquals("Turnier", LiveSlug.of("  ", null));
        assertTrue(LiveHost.ID.matcher(LiveSlug.of("Café & Co. / Spaß!", LocalDate.of(2026, 1, 2))).matches());
    }

    @Test
    void qrAushangAlsPdf() throws Exception {
        Tournament t = new Tournament();
        t.setName("Herbstturnier");
        t.setDate(LocalDate.of(2026, 11, 28));
        try (PDDocument d = PdfReports.liveQrPoster(t, "https://hts-livescoring.de/live/Herbstturnier_28_11_2026", null)) {
            assertEquals(1, d.getNumberOfPages());
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(d);
            assertTrue(text.contains("Herbstturnier") && text.contains("28.11.2026") && text.contains("hts-livescoring.de/live/Herbstturnier_28_11_2026"), text);
        }
    }
}
