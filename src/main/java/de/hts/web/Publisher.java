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
 */
public final class Publisher implements AutoCloseable {

    public static final long MIN_INTERVAL_MS = 5_000;
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

    /** Das Turnier hat sich geändert (Ergebnis, neue Phase, Tischvergabe …). */
    public void changed() {
        dirty = true;
    }

    public Status status() {
        return status;
    }

    private void tick() {
        long now = System.currentTimeMillis();
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
            request("PUT", "", data, "application/octet-stream");
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
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base + "/api/turniere/" + id + suffix))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key)
                .header("X-HTS-Version", de.hts.HtsApp.VERSION);
        if (body != null) rb.header("Content-Type", type).method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        else rb.method(method, HttpRequest.BodyPublishers.noBody());
        HttpResponse<String> r = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
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
