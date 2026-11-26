/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 1.5.0 (#135–#138): Veröffentlicht den Turnierstand auf dem Webserver – nur zum Einsehen, vom Server fließt nichts
 * zurück. Nach einer Änderung wird höchstens alle {@link #MIN_INTERVAL_MS} hochgeladen; ist der Server nicht erreichbar,
 * bleibt der letzte Stand liegen und wird alle {@link #RETRY_MS} erneut versucht (Funkloch in der Halle).
 *
 * <p>Protokoll (siehe docs/WEBSERVER-PROTOKOLL.md), alles über HTTPS mit {@code Authorization: Bearer <Schlüssel>}:
 * <ul>
 *   <li>{@code PUT  <Adresse>/api/turniere/<ID>} – Turnierdatei (.hts) als Inhalt</li>
 *   <li>{@code PUT  <Adresse>/api/turniere/<ID>/logo} – Logo des Ausrichters (PNG), einmal je Sitzung</li>
 *   <li>{@code DELETE <Adresse>/api/turniere/<ID>} – „Live-Daten löschen“</li>
 *   <li>{@code POST <Adresse>/api/turniere/<ID>/nur-endergebnis} – „nur Endergebnis behalten“</li>
 *   <li>{@code GET  <Adresse>/api/status} – „Verbindung testen“</li>
 * </ul>
 *
 * <p>1.10.0 (#170): höchstens alle 30 Sekunden; meldet der Server mit {@code X-HTS-Gzip: 1}, dass er komprimierte
 * Turnierdateien versteht, geht jeder weitere Upload gzip-komprimiert ({@code Content-Encoding: gzip}, etwa ein Neuntel
 * der Größe). Ältere Server melden das nicht und bekommen die Datei weiter unkomprimiert.
 */
public final class Publisher implements AutoCloseable {

    public static final long MIN_INTERVAL_MS = 30_000;          // 1.10.0 (#170): vorher 5 s
    public static final long RETRY_MS = 10_000;

    /** Zustand für die Anzeige. */
    public enum State { OFF, WAITING, OK, ERROR }

    /** Momentaufnahme für die Statusanzeige. */
    public record Status(State state, long lastSuccessMillis, String message) {
    }

    private final String base;
    private final String key;
    private final String id;
    private final HttpClient http;
    private final ScheduledExecutorService timer;
    private final Consumer<Runnable> uiThread;
    private final Supplier<byte[]> snapshot;
    private final Supplier<byte[]> logo;
    private final long minInterval, retry;
    /** wird im Hintergrund auf die Momentaufnahme angewandt (z. B. Vornamen abkürzen); {@code null} = unverändert */
    private volatile java.util.function.UnaryOperator<byte[]> transform;

    private volatile boolean dirty;
    private volatile byte[] pending;
    private volatile long lastAttempt;
    private volatile long lastSuccess;
    private volatile boolean logoSent;
    private volatile Status status = new Status(State.WAITING, 0, "noch nicht veröffentlicht");
    private volatile boolean capturing;

    /**
     * @param uiThread  führt die Momentaufnahme im Programm-Thread aus (das Turnier ist nicht threadsicher)
     * @param snapshot  liefert die Turnierdatei (im Programm-Thread aufgerufen)
     * @param logo      liefert das Logo (PNG) oder {@code null}
     */
    public Publisher(String base, String key, String id, Consumer<Runnable> uiThread, Supplier<byte[]> snapshot, Supplier<byte[]> logo) {
        this(base, key, id, uiThread, snapshot, logo, MIN_INTERVAL_MS, RETRY_MS);
    }

    /** Für Tests mit kürzeren Abständen. */
    public Publisher(String base, String key, String id, Consumer<Runnable> uiThread, Supplier<byte[]> snapshot, Supplier<byte[]> logo,
                     long minInterval, long retry) {
        this.base = normalize(base);
        this.key = key == null ? "" : key.strip();
        this.id = id;
        this.uiThread = uiThread;
        this.snapshot = snapshot;
        this.logo = logo;
        this.minInterval = minInterval;
        this.retry = retry;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HTS-Veroeffentlichen");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleWithFixedDelay(this::tick, 300, 300, TimeUnit.MILLISECONDS);
        dirty = true;                                         // beim Einschalten sofort den aktuellen Stand senden
    }

    /** Adresse ohne Schrägstrich am Ende; ohne Schema wird https:// ergänzt. */
    public static String normalize(String base) {
        String b = base == null ? "" : base.strip();
        if (!b.isEmpty() && !b.matches("(?i)^https?://.*")) b = "https://" + b;
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    /** Öffentliche Adresse der Live-Seiten dieses Turniers (1.7.0 #147: lesbar, z. B. „…/live/Herbstturnier_28_11_2026“). */
    public static String publicUrl(String base, String name, java.time.LocalDate date) {
        return normalize(base) + "/live/" + LiveSlug.of(name, date);
    }

    /** Umwandlung vor dem Hochladen setzen (Datenschutz); löst einen neuen Upload aus. */
    public void setTransform(java.util.function.UnaryOperator<byte[]> tf) {
        transform = tf;
        pending = null;
        dirty = true;
    }

    /** 1.8.0 (#150): Name des Ausrichters – wird bei jedem Hochladen als Kopfzeile X-HTS-Ausrichter mitgeschickt. */
    private volatile Supplier<String> organizer = () -> null;

    public void setOrganizer(Supplier<String> name) {
        organizer = name == null ? () -> null : name;
        dirty = true;
    }

    /** Ausrichter-Name für die Kopfzeile: URL-kodiert, höchstens 120 Zeichen, ohne Steuerzeichen. */
    static String organizerHeader(String name) {
        if (name == null) return null;
        String n = name.replaceAll("\\p{Cntrl}", " ").strip();
        if (n.isEmpty()) return null;
        if (n.length() > 120) n = n.substring(0, 120);
        return java.net.URLEncoder.encode(n, java.nio.charset.StandardCharsets.UTF_8);
    }

    // --- 1.9.0 (#164): Online-Anmeldung abholen ----------------------------------------------------

    /** Eine Online-Meldung vom Server. */
    public record CheckinReport(long seq, String id, boolean present) {
    }

    public static final long CHECKIN_POLL_MS = 5_000;
    private volatile java.util.function.BooleanSupplier checkinActive = () -> false;
    private volatile Consumer<java.util.List<CheckinReport>> checkinSink;
    private volatile long lastPoll;
    private volatile boolean polling;
    private volatile long pollInterval = CHECKIN_POLL_MS;

    /**
     * Meldungen der Online-Anmeldung abholen, solange {@code active} gilt (Schalter an, Turniertag). {@code sink} läuft
     * im Programm-Thread; danach quittiert HTS, und der Server löscht die Meldungen.
     */
    public void setCheckinPoll(java.util.function.BooleanSupplier active, Consumer<java.util.List<CheckinReport>> sink) {
        checkinActive = active == null ? () -> false : active;
        checkinSink = sink;
    }

    /** Für Tests. */
    void setCheckinPollInterval(long ms) {
        pollInterval = ms;
    }

    private void pollCheckins() {
        polling = true;
        lastPoll = System.currentTimeMillis();
        try {
            HttpRequest r = HttpRequest.newBuilder(URI.create(base + "/api/turniere/" + id + "/anmeldungen")).timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + key).header("X-HTS-Version", de.hts.HtsApp.VERSION).GET().build();
            HttpResponse<String> resp = http.send(r, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                polling = false;
                return;
            }
            java.util.List<CheckinReport> list = parseReports(resp.body());
            if (list.isEmpty()) {
                polling = false;
                return;
            }
            long max = list.stream().mapToLong(CheckinReport::seq).max().orElse(-1);
            Consumer<java.util.List<CheckinReport>> sink = checkinSink;
            uiThread.accept(() -> {
                try {
                    if (sink != null) sink.accept(list);
                } finally {
                    timer.execute(() -> {
                        try {
                            request("POST", "/anmeldungen", ("bis=" + max).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                    "application/x-www-form-urlencoded");
                        } catch (IOException | InterruptedException e) {
                            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                            // nicht quittiert: kommt beim nächsten Abholen noch einmal (Übernahme ist wiederholbar)
                        } finally {
                            polling = false;
                        }
                    });
                }
            });
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            polling = false;
        }
    }

    private static final java.util.regex.Pattern REPORT = java.util.regex.Pattern.compile(
            "\\{\"n\":(\\d+),\"id\":\"([A-Za-z0-9_-]{1,64})\",\"a\":([01])");

    static java.util.List<CheckinReport> parseReports(String json) {
        java.util.List<CheckinReport> out = new java.util.ArrayList<>();
        if (json == null) return out;
        java.util.regex.Matcher m = REPORT.matcher(json);
        while (m.find()) out.add(new CheckinReport(Long.parseLong(m.group(1)), m.group(2), "1".equals(m.group(3))));
        return out;
    }

    /** Das Turnier hat sich geändert (Ergebnis, neue Phase, Tischvergabe …). */
    public void changed() {
        dirty = true;
    }

    public Status status() {
        return status;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (checkinSink != null && !polling && now - lastPoll >= pollInterval && checkinActive.getAsBoolean()) {
            pollCheckins();
        }
        if (pending != null) {                                // Funkloch: nachholen
            if (now - lastAttempt >= retry) send(pending);
            return;
        }
        if (dirty && !capturing && now - lastAttempt >= minInterval) {
            dirty = false;
            capturing = true;
            uiThread.accept(() -> {
                try {
                    byte[] b = snapshot.get();
                    timer.execute(() -> {
                        java.util.function.UnaryOperator<byte[]> tf = transform;
                        try {
                            send(tf == null ? b : tf.apply(b));
                        } catch (RuntimeException e) {
                            status = new Status(State.ERROR, lastSuccess, "Stand konnte nicht erstellt werden: " + e.getMessage());
                        }
                    });
                } catch (RuntimeException e) {
                    status = new Status(State.ERROR, lastSuccess, "Stand konnte nicht erstellt werden: " + e.getMessage());
                } finally {
                    capturing = false;
                }
            });
        }
    }

    private void send(byte[] data) {
        lastAttempt = System.currentTimeMillis();
        try {
            if (!logoSent) {
                byte[] png = logo == null ? null : logo.get();
                if (png != null) request("PUT", "/logo", png, "image/png");
                logoSent = true;
            }
            String org;
            try {
                org = organizerHeader(organizer.get());
            } catch (RuntimeException e) {
                org = null;
            }
            if (serverGzip) {                                 // 1.10.0 (#170)
                request("PUT", "", gzip(data), "application/octet-stream", org, true);
            } else {
                request("PUT", "", data, "application/octet-stream", org, false);
            }
            pending = null;
            lastSuccess = System.currentTimeMillis();
            if (status.state() != State.OK) de.hts.Log.info("Live im Internet: veröffentlicht");
            status = new Status(State.OK, lastSuccess, "veröffentlicht");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            pending = data;                                   // letzter Stand bleibt liegen
            if (status.state() != State.ERROR) de.hts.Log.warn("Live im Internet: keine Verbindung (" + e.getMessage() + ")");
            status = new Status(State.ERROR, lastSuccess, "keine Verbindung – wird nachgeholt");
        }
    }

    private void request(String method, String suffix, byte[] body, String type) throws IOException, InterruptedException {
        request(method, suffix, body, type, null, false);
    }

    /** 1.10.0 (#170): Server versteht gzip-komprimierte Turnierdateien (Antwortkopf {@code X-HTS-Gzip: 1}). */
    private volatile boolean serverGzip;

    boolean usesGzip() {
        return serverGzip;
    }

    static byte[] gzip(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(Math.max(1024, data.length / 6));
        try (java.util.zip.GZIPOutputStream g = new java.util.zip.GZIPOutputStream(out, 64 * 1024)) {
            g.write(data);
        }
        return out.toByteArray();
    }

    private void request(String method, String suffix, byte[] body, String type, String organizerHeader, boolean gzipped)
            throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base + "/api/turniere/" + id + suffix))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key)
                .header("X-HTS-Version", de.hts.HtsApp.VERSION);
        if (organizerHeader != null) rb.header("X-HTS-Ausrichter", organizerHeader);
        if (gzipped) rb.header("Content-Encoding", "gzip");
        if (body != null) rb.header("Content-Type", type).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        else rb.method(method, HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> r = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        if (r.headers().firstValue("X-HTS-Gzip").map("1"::equals).orElse(false)) serverGzip = true;
        if (r.statusCode() == 401 || r.statusCode() == 403) throw new IOException("Schlüssel abgelehnt (HTTP " + r.statusCode() + ")");
        if (r.statusCode() / 100 != 2) throw new IOException("HTTP " + r.statusCode());
    }

    /** „Live-Daten löschen“: entfernt das Turnier vom Server (sofort, ohne Drosselung). */
    public void deleteOnServer() throws IOException, InterruptedException {
        pending = null;
        dirty = false;
        request("DELETE", "", null, null);
    }

    /** „Nur Endergebnis behalten“: der Server verwirft alles außer den Endergebnissen. */
    public void keepOnlyFinalResults() throws IOException, InterruptedException {
        request("POST", "/nur-endergebnis", null, null);
    }

    /**
     * Verbindung prüfen (Knopf „Verbindung testen“): {@code GET <Adresse>/api/status} mit Schlüssel.
     *
     * @return {@code null} bei Erfolg, sonst die Fehlermeldung
     */
    public static String test(String base, String key) {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest r = HttpRequest.newBuilder(URI.create(normalize(base) + "/api/status")).timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + (key == null ? "" : key.strip())).GET().build();
            int code = c.send(r, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (code == 401 || code == 403) return "Schlüssel abgelehnt (HTTP " + code + ")";
            return code / 100 == 2 ? null : "Server antwortet mit HTTP " + code;
        } catch (IllegalArgumentException e) {
            return "Ungültige Adresse";
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "Server nicht erreichbar" + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")");
        }
    }

    @Override
    public void close() {
        timer.shutdownNow();
    }
}
