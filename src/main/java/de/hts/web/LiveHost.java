/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.hts.persistence.TournamentFile;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 1.6.0 (#101): Live-Server für den gemieteten Internetserver (ohne Bildschirm, z. B. hts-livescoring.de).
 * <p>
 * Nimmt das Protokoll aus {@code docs/WEBSERVER-PROTOKOLL.md} an (HTS lädt hoch, vom Server fließt nichts zurück) und
 * zeigt die Live-Seiten aller Turniere: Startseite {@code /} mit den Turnieren, je Turnier {@code /live/<ID>} mit Tischen,
 * Klassen, Ergebnissen und „Meine Spiele“ (Darstellung aus {@link LiveServer}). Dazu {@code /impressum} und
 * {@code /datenschutz} aus Textdateien im Konfigurationsordner.
 * <p>
 * Läuft hinter Caddy (HTTPS) nur auf 127.0.0.1. Keine Cookies, keine Zugriffsprotokolle, keine IP-Adressen in den
 * Meldungen. Der geheime Schlüssel steht nur als SHA-256-Prüfwert in der Konfiguration.
 * <p>
 * Start: {@code java -jar hts-server.jar /etc/hts-server/server.properties}
 */
public final class LiveHost {

    static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMAN)
            .withZone(ZoneId.of("Europe/Berlin"));

    /** Einstellungen aus server.properties. */
    record Config(String bind, int port, Path data, Path content, byte[] keyHash, int retentionDays, int maxUploadMb, String siteTitle) {

        static Config load(Path file) throws IOException {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            String hash = p.getProperty("key-sha256", "").strip().toLowerCase(Locale.ROOT);
            if (!hash.matches("[0-9a-f]{64}")) throw new IOException("key-sha256 fehlt oder ist kein SHA-256-Wert (64 Hex-Zeichen) in " + file);
            Path dir = file.toAbsolutePath().getParent();
            return new Config(p.getProperty("bind", "127.0.0.1").strip(),
                    Integer.parseInt(p.getProperty("port", "8090").strip()),
                    Path.of(p.getProperty("data", "/var/lib/hts-server").strip()),
                    Path.of(p.getProperty("content", dir.toString()).strip()),
                    HexFormat.of().parseHex(hash),
                    Integer.parseInt(p.getProperty("retention-days", "90").strip()),
                    Integer.parseInt(p.getProperty("max-upload-mb", "25").strip()),
                    p.getProperty("site-title", "HTS Live-Scoring").strip());
        }
    }

    /** Ein Turnier auf dem Server (Ordner {@code <data>/turniere/<ID>}). */
    static final class Entry {
        final String id;
        final Object lock = new Object();
        volatile String name = "Turnier";
        volatile LocalDate date;
        volatile String state = "";
        volatile long updated;
        volatile boolean finalOnly;
        volatile LiveServer view;                            // geladen bei Bedarf
        volatile long lastAccess;

        Entry(String id) {
            this.id = id;
        }
    }

    private final Config cfg;
    private final Path turniere;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger failures = new AtomicInteger();
    private volatile long failureWindow = System.currentTimeMillis();
    private HttpServer http;
    private ScheduledExecutorService housekeeping;

    LiveHost(Config cfg) throws IOException {
        this.cfg = cfg;
        this.turniere = cfg.data().resolve("turniere");
        Files.createDirectories(turniere);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(turniere)) {
            for (Path d : ds) {
                String id = d.getFileName().toString();
                if (!Files.isDirectory(d) || !ID.matcher(id).matches() || !Files.exists(d.resolve("turnier.hts"))) continue;
                Entry e = new Entry(id);
                readMeta(e, d);
                entries.put(id, e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Aufruf: java -jar hts-server.jar /etc/hts-server/server.properties");
            System.exit(2);
        }
        LiveHost h = new LiveHost(Config.load(Path.of(args[0])));
        h.start();
        System.err.println("HTS-Live-Server " + de.hts.HtsApp.VERSION + " läuft auf " + h.cfg.bind() + ":" + h.port()
                + " (" + h.entries.size() + " Turniere)");
        Runtime.getRuntime().addShutdownHook(new Thread(h::stop));
        Thread.currentThread().join();
    }

    void start() throws IOException {
        http = HttpServer.create(new InetSocketAddress(InetAddress.getByName(cfg.bind()), cfg.port()), 64);
        http.createContext("/", this::handle);
        http.setExecutor(Executors.newFixedThreadPool(16, r -> {
            Thread t = new Thread(r, "HTS-Live-Server");
            t.setDaemon(true);
            return t;
        }));
        http.start();
        housekeeping = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HTS-Aufraeumen");
            t.setDaemon(true);
            return t;
        });
        housekeeping.scheduleWithFixedDelay(this::cleanUp, 1, 60, TimeUnit.MINUTES);
    }

    void stop() {
        if (http != null) http.stop(1);
        if (housekeeping != null) housekeeping.shutdownNow();
    }

    int port() {
        return http.getAddress().getPort();
    }

    // --- Verteilung ------------------------------------------------------------------------------

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            securityHeaders(ex);
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.startsWith("/api/")) {
                try {
                    api(ex, method, path.substring(5));
                } finally {
                    drain(ex, (long) cfg.maxUploadMb() * 1024 * 1024 + 1024 * 1024);
                }
                return;
            }
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                text(ex, 405, "Nicht erlaubt");
                return;
            }
            switch (path) {
                case "/", "/live", "/live/" -> {
                    if (!path.equals("/")) {
                        redirect(ex, "/");
                    } else {
                        html(ex, 200, startPage());
                    }
                    return;
                }
                case "/impressum" -> {
                    html(ex, 200, infoPage("Impressum", "impressum.html"));
                    return;
                }
                case "/datenschutz" -> {
                    html(ex, 200, infoPage("Datenschutzerklärung", "datenschutz.html"));
                    return;
                }
                case "/favicon.ico", "/favicon.png" -> {
                    favicon(ex);
                    return;
                }
                case "/robots.txt" -> {
                    text(ex, 200, "User-agent: *\nDisallow: /live/\n");
                    return;
                }
                default -> { }
            }
            if (path.startsWith("/live/")) {
                String rest = path.substring(6);
                int slash = rest.indexOf('/');
                String id = slash < 0 ? rest : rest.substring(0, slash);
                String sub = slash < 0 ? "" : rest.substring(slash);
                Entry e = ID.matcher(id).matches() ? entries.get(id) : null;
                LiveServer v = e == null ? null : view(e);
                if (v == null) {
                    html(ex, 404, frame("Nicht gefunden", "<p>Dieses Turnier gibt es hier (nicht mehr). <a href=\"/\">Zur Übersicht</a></p>"));
                    return;
                }
                if (sub.equals("/")) {
                    redirect(ex, "/live/" + id);
                    return;
                }
                if ("HEAD".equals(method)) {
                    ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    ex.sendResponseHeaders(200, -1);
                    return;
                }
                v.serveLive(ex, "/live" + sub, LiveServer.parseQuery(ex.getRequestURI().getRawQuery()));
                return;
            }
            html(ex, 404, frame("Nicht gefunden", "<p>Diese Seite gibt es nicht. <a href=\"/\">Zur Übersicht</a></p>"));
        } catch (Exception e) {
            System.err.println("Fehler bei der Anfrage: " + e);
            try {
                text(ex, 500, "Interner Fehler");
            } catch (IOException ignored) {
                // Verbindung schon zu
            }
        }
    }

    // --- Schnittstelle für HTS ---------------------------------------------------------------------

    private void api(HttpExchange ex, String method, String rest) throws IOException {
        if (!authorized(ex)) {
            text(ex, tooManyFailures() ? 429 : 401, "Schlüssel abgelehnt");
            return;
        }
        if (rest.equals("status") && "GET".equals(method)) {
            json(ex, 200, "{\"ok\":true,\"server\":\"HTS-Live-Server\",\"version\":" + LiveServer.json(de.hts.HtsApp.VERSION) + "}");
            return;
        }
        if (!rest.startsWith("turniere/")) {
            text(ex, 404, "Unbekannte Anfrage");
            return;
        }
        String[] parts = rest.substring(9).split("/", -1);
        String id = parts[0];
        if (!ID.matcher(id).matches() || parts.length > 2) {
            text(ex, 400, "Ungültige Turnier-Kennung");
            return;
        }
        String sub = parts.length == 2 ? parts[1] : "";
        switch (method + " " + sub) {
            case "PUT " -> putTournament(ex, id);
            case "PUT logo" -> putLogo(ex, id);
            case "DELETE " -> {
                delete(id);
                json(ex, 200, "{\"ok\":true}");
            }
            case "POST nur-endergebnis" -> {
                Entry e = entries.get(id);
                if (e == null) {
                    text(ex, 404, "Turnier nicht vorhanden");
                    return;
                }
                synchronized (e.lock) {
                    e.finalOnly = true;
                    e.view = null;
                    writeMeta(e);
                }
                json(ex, 200, "{\"ok\":true}");
            }
            default -> text(ex, 405, "Nicht erlaubt");
        }
    }

    private boolean authorized(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        boolean ok = false;
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            byte[] given = sha256(h.substring(7).strip().getBytes(StandardCharsets.UTF_8));
            ok = MessageDigest.isEqual(given, cfg.keyHash());   // Vergleich in konstanter Zeit
        }
        if (!ok) {
            long now = System.currentTimeMillis();
            if (now - failureWindow > 60_000) {
                failureWindow = now;
                failures.set(0);
            }
            failures.incrementAndGet();
        }
        return ok && !tooManyFailures();
    }

    /** Mehr als 30 Fehlversuche je Minute: vorübergehend auch richtige Schlüssel ablehnen (Ratenbremse). */
    private boolean tooManyFailures() {
        return System.currentTimeMillis() - failureWindow <= 60_000 && failures.get() > 30;
    }

    private void putTournament(HttpExchange ex, String id) throws IOException {
        byte[] body = readBody(ex, (long) cfg.maxUploadMb() * 1024 * 1024);
        if (body == null) {
            text(ex, 413, "Turnierdatei zu groß (höchstens " + cfg.maxUploadMb() + " MB)");
            return;
        }
        Tournament t;
        try {
            t = TournamentFile.read(new ByteArrayInputStream(body)).toTournament();
        } catch (IOException | RuntimeException e) {
            text(ex, 400, "Keine gültige HTS-Turnierdatei");
            return;
        }
        Entry e = entries.computeIfAbsent(id, Entry::new);
        synchronized (e.lock) {
            Path dir = turniere.resolve(id);
            Files.createDirectories(dir);
            writeAtomic(dir.resolve("turnier.hts"), body);
            e.name = t.getName() == null || t.getName().isBlank() ? "Turnier" : t.getName();
            e.date = t.getDate();
            e.state = stateOf(t);
            e.updated = System.currentTimeMillis();
            e.finalOnly = false;                              // neuer Stand: wieder alles zeigen
            e.view = makeView(e, t);
            e.lastAccess = e.updated;
            writeMeta(e);
        }
        json(ex, 200, "{\"ok\":true}");
    }

    private void putLogo(HttpExchange ex, String id) throws IOException {
        byte[] body = readBody(ex, 2L * 1024 * 1024);
        if (body == null) {
            text(ex, 413, "Logo zu groß (höchstens 2 MB)");
            return;
        }
        if (body.length < PNG_MAGIC.length || !java.util.Arrays.equals(body, 0, PNG_MAGIC.length, PNG_MAGIC, 0, PNG_MAGIC.length)) {
            text(ex, 400, "Logo muss eine PNG-Datei sein");
            return;
        }
        Path dir = turniere.resolve(id);
        Files.createDirectories(dir);
        writeAtomic(dir.resolve("logo.png"), body);
        json(ex, 200, "{\"ok\":true}");
    }

    private void delete(String id) throws IOException {
        Entry e = entries.remove(id);
        Object lock = e == null ? new Object() : e.lock;
        synchronized (lock) {
            Path dir = turniere.resolve(id);
            if (!Files.isDirectory(dir)) return;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path f : ds) Files.deleteIfExists(f);
            }
            Files.deleteIfExists(dir);
        }
    }

    /** Ungelesenen Rest der Anfrage verwerfen (begrenzt), sonst bricht die Verbindung vor der Antwort ab. */
    private static void drain(HttpExchange ex, long limit) {
        try (InputStream in = ex.getRequestBody()) {
            byte[] buf = new byte[64 * 1024];
            long total = 0;
            int n;
            while (total < limit && (n = in.read(buf)) >= 0) total += n;
        } catch (IOException ignored) {
            // Verbindung schon zu
        }
    }

    /** Liest den Inhalt, höchstens {@code max} Bytes; {@code null}, wenn er größer ist. */
    private static byte[] readBody(HttpExchange ex, long max) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] b = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, max + 1));
            if (b.length <= max) return b;
            in.transferTo(OutputStream.nullOutputStream());  // Rest verwerfen, damit die Antwort vollständig ankommt
            return null;
        }
    }

    // --- Turniere laden, Metadaten, Aufräumen ------------------------------------------------------

    private LiveServer view(Entry e) {
        LiveServer v = e.view;
        e.lastAccess = System.currentTimeMillis();
        if (v != null) return v;
        synchronized (e.lock) {
            if (e.view != null) return e.view;
            Path f = turniere.resolve(e.id).resolve("turnier.hts");
            try (InputStream in = Files.newInputStream(f)) {
                e.view = makeView(e, TournamentFile.read(in).toTournament());
            } catch (IOException | RuntimeException ex) {
                System.err.println("Turnier " + e.id + " nicht lesbar: " + ex.getMessage());
                return null;
            }
            return e.view;
        }
    }

    private LiveServer makeView(Entry e, Tournament t) {
        Path logo = turniere.resolve(e.id).resolve("logo.png");
        return LiveServer.forHost(t, r -> {
            synchronized (e.lock) {                           // Turnier-Objekte sind nicht für parallele Zugriffe gebaut
                r.run();
            }
        }, "/live/" + e.id, () -> {
            try {
                return Files.exists(logo) ? Files.readAllBytes(logo) : null;
            } catch (IOException ex) {
                return null;
            }
        }, HEAD_EXTRA, footer(), e.finalOnly);
    }

    static String stateOf(Tournament t) {
        List<PlayingClass> roots = t.rootClasses();
        if (!roots.isEmpty() && roots.stream().allMatch(PlayingClass::isFinished)) return "beendet";
        if (t.getStartedAt() != null || roots.stream().anyMatch(PlayingClass::isStarted)) return "läuft";
        return "angekündigt";
    }

    private void readMeta(Entry e, Path dir) {
        Properties p = new Properties();
        Path f = dir.resolve("meta.properties");
        try {
            if (Files.exists(f)) {
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
            }
            e.name = p.getProperty("name", "Turnier");
            String d = p.getProperty("date", "");
            e.date = d.isEmpty() ? null : LocalDate.parse(d);
            e.state = p.getProperty("state", "");
            e.updated = Long.parseLong(p.getProperty("updated", String.valueOf(Files.getLastModifiedTime(dir.resolve("turnier.hts")).toMillis())));
            e.finalOnly = Boolean.parseBoolean(p.getProperty("final-only", "false"));
        } catch (IOException | RuntimeException ex) {
            System.err.println("Angaben zu Turnier " + e.id + " unvollständig: " + ex.getMessage());
        }
    }

    private void writeMeta(Entry e) throws IOException {
        Properties p = new Properties();
        p.setProperty("name", e.name);
        if (e.date != null) p.setProperty("date", e.date.toString());
        p.setProperty("state", e.state);
        p.setProperty("updated", String.valueOf(e.updated));
        p.setProperty("final-only", String.valueOf(e.finalOnly));
        java.io.StringWriter w = new java.io.StringWriter();
        p.store(w, "HTS-Live-Server");
        writeAtomic(turniere.resolve(e.id).resolve("meta.properties"), w.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeAtomic(Path target, byte[] data) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".neu");
        Files.write(tmp, data);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Stündlich: alte Turniere löschen (Aufbewahrungsfrist), lange nicht aufgerufene aus dem Speicher nehmen. */
    void cleanUp() {
        long now = System.currentTimeMillis();
        long limit = now - TimeUnit.DAYS.toMillis(Math.max(1, cfg.retentionDays()));
        for (Entry e : List.copyOf(entries.values())) {
            try {
                if (e.updated < limit) {
                    delete(e.id);
                    System.err.println("Turnier " + e.id + " nach " + cfg.retentionDays() + " Tagen gelöscht.");
                } else if (e.view != null && now - e.lastAccess > TimeUnit.HOURS.toMillis(1)) {
                    e.view = null;
                }
            } catch (IOException ex) {
                System.err.println("Aufräumen von " + e.id + " fehlgeschlagen: " + ex.getMessage());
            }
        }
    }

    // --- Seiten des Servers ------------------------------------------------------------------------

    private static final String HEAD_EXTRA = "<link rel=\"icon\" href=\"/favicon.png\"><meta name=\"robots\" content=\"noindex\">";

    private static final String HOST_CSS = "footer.legal{text-align:center;font-size:13px;padding:18px 12px 0}footer.legal a{color:#173a6c;margin:0 8px}"
            + ".info h2:first-child{margin-top:4px}.info{line-height:1.5}";

    private static String footer() {
        return "<style>" + HOST_CSS + "</style><footer class=\"legal\"><a href=\"/\">Alle Turniere</a><a href=\"/impressum\">Impressum</a>"
                + "<a href=\"/datenschutz\">Datenschutz</a></footer>";
    }

    String frame(String title, String body) {
        return "<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + LiveServer.esc(title) + " – " + LiveServer.esc(cfg.siteTitle()) + "</title>" + HEAD_EXTRA
                + "<style>" + LiveServer.CSS + HOST_CSS + "</style></head><body><header><a href=\"/\"><h1>" + LiveServer.esc(cfg.siteTitle())
                + "</h1></a><div class=\"sub\">" + LiveServer.esc(title) + "</div></header><main>" + body + "</main>"
                + footer().replaceFirst("<style>[^<]*</style>", "") + LiveServer.SOURCE_FOOTER + "</body></html>";
    }

    String startPage() {
        List<Entry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparingInt((Entry e) -> switch (e.state) {
            case "läuft" -> 0;
            case "angekündigt" -> 1;
            default -> 2;
        }).thenComparing(Comparator.comparingLong((Entry e) -> e.updated).reversed()));
        StringBuilder b = new StringBuilder();
        if (list.isEmpty()) {
            b.append("<p class=\"muted\">Gerade sind keine Turniere veröffentlicht.</p>");
        }
        String last = null;
        for (Entry e : list) {
            String group = switch (e.state) {
                case "läuft" -> "Laufende Turniere";
                case "angekündigt" -> "Demnächst";
                default -> "Abgeschlossene Turniere";
            };
            if (!group.equals(last)) {
                b.append("<h2>").append(group).append("</h2>");
                last = group;
            }
            String chip = switch (e.state) {
                case "läuft" -> "<span class=\"chip run\">läuft</span>";
                case "beendet" -> "<span class=\"chip done\">beendet</span>";
                default -> "<span class=\"chip\">angekündigt</span>";
            };
            b.append("<a class=\"card\" href=\"/live/").append(e.id).append("\"><div class=\"row\"><b>").append(LiveServer.esc(e.name))
                    .append("</b>").append(chip).append("</div><div class=\"muted\">")
                    .append(e.date == null ? "" : DAY.format(e.date) + " · ")
                    .append("Stand ").append(STAMP.format(Instant.ofEpochMilli(e.updated))).append("</div></a>");
        }
        b.append("<script>setTimeout(function(){location.reload();},30000);</script>");
        return frame("Live-Ergebnisse", b.toString());
    }

    String infoPage(String title, String file) {
        Path f = cfg.content().resolve(file);
        String body;
        try {
            body = Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            body = null;
        }
        if (body == null || body.isBlank()) {
            body = "<p class=\"muted\">Dieser Text wurde noch nicht hinterlegt (Datei " + LiveServer.esc(f.toString()) + ").</p>";
        }
        return frame(title, "<div class=\"card info\">" + body + "</div>");
    }

    private void favicon(HttpExchange ex) throws IOException {
        try (InputStream in = LiveHost.class.getResourceAsStream("/de/hts/ui/hts-logo-klein.png")) {
            if (in == null) {
                text(ex, 404, "");
                return;
            }
            byte[] b = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", "image/png");
            ex.getResponseHeaders().set("Cache-Control", "max-age=86400");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    // --- Antworten ---------------------------------------------------------------------------------

    private static void securityHeaders(HttpExchange ex) {
        var h = ex.getResponseHeaders();
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Referrer-Policy", "no-referrer");
        h.set("X-Frame-Options", "DENY");
        h.set("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                + "img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        h.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    }

    private static void redirect(HttpExchange ex, String to) throws IOException {
        ex.getResponseHeaders().set("Location", to);
        ex.sendResponseHeaders(302, -1);
    }

    private static void html(HttpExchange ex, int code, String body) throws IOException {
        send(ex, code, "text/html", body);
    }

    private static void json(HttpExchange ex, int code, String body) throws IOException {
        send(ex, code, "application/json", body);
    }

    private static void text(HttpExchange ex, int code, String body) throws IOException {
        send(ex, code, "text/plain", body);
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        boolean head = "HEAD".equals(ex.getRequestMethod());
        ex.sendResponseHeaders(code, head || b.length == 0 ? -1 : b.length);
        if (!head && b.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
