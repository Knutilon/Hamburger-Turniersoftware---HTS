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
        volatile String slug = "";                           // 1.7.0 (#147): lesbare Adresse, z. B. Herbstturnier_28_11_2026
        volatile String organizer = "";                      // 1.8.0 (#150): Ausrichter (Kopfzeile X-HTS-Ausrichter)
        volatile String world = "";                          // 1.8.0 (#151): Link zur Turnierwelt
        // 1.9.0 (#164): Online-Anmeldung
        volatile String checkinToken = "";
        volatile boolean onlineCheckin;
        volatile java.time.LocalTime checkinUntil;
        final List<Report> reports = new ArrayList<>();      // noch nicht von HTS abgeholt (unter lock)
        long reportSeq;
        final AtomicInteger checkinPosts = new AtomicInteger();
        volatile long checkinWindow;
        volatile LiveServer view;                            // geladen bei Bedarf
        volatile long lastAccess;

        Entry(String id) {
            this.id = id;
        }
    }

    /** 1.9.0 (#164): eine Online-Meldung, bis HTS sie abholt. */
    record Report(long seq, String id, boolean present, long at) {
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
                readReports(e, d);
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
            if (!"GET".equals(method) && !"HEAD".equals(method) && !("POST".equals(method) && path.startsWith("/anmeldung/"))) {
                text(ex, 405, "Nicht erlaubt");
                return;
            }
            switch (path) {
                case "/", "/live", "/live/" -> {
                    if (!path.equals("/")) {
                        redirect(ex, "/");
                    } else {
                        html(ex, 200, startPage(false));
                    }
                    return;
                }
                case "/vergangen" -> {                               // 1.8.0 (#157)
                    html(ex, 200, startPage(true));
                    return;
                }
                case "/spenden" -> {                                 // 1.8.0 (#153)
                    html(ex, 200, donationPage());
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
                    text(ex, 200, "User-agent: *\nDisallow: /live/\nDisallow: /anmeldung/\n");
                    return;
                }
                default -> { }
            }
            if (path.startsWith("/anmeldung/")) {                    // 1.9.0 (#164): Online-Anmeldung
                checkin(ex, path.substring("/anmeldung/".length()), method);
                return;
            }
            if (path.startsWith("/live/")) {
                String rest = path.substring(6);
                int slash = rest.indexOf('/');
                String id = slash < 0 ? rest : rest.substring(0, slash);
                String sub = slash < 0 ? "" : rest.substring(slash);
                Entry e = ID.matcher(id).matches() ? entries.get(id) : null;
                if (e != null && !e.slug.isEmpty() && !e.slug.equals(id) && bySlug(e.slug) == e) {   // Kennung → lesbare Adresse
                    String query = ex.getRequestURI().getRawQuery();
                    redirect(ex, "/live/" + e.slug + sub + (query == null ? "" : "?" + query));
                    return;
                }
                if (e == null && ID.matcher(id).matches()) e = bySlug(id);
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
            case "GET anmeldungen" -> json(ex, 200, reportsJson(id));          // 1.9.0 (#164): HTS holt Meldungen ab
            case "POST anmeldungen" -> {                                        // Quittung: „bis=<Nr>“ – Server löscht sie
                byte[] b = readBody(ex, 1024);
                Map<String, String> f = LiveServer.parseQuery(b == null ? "" : new String(b, StandardCharsets.UTF_8));
                long bis;
                try {
                    bis = Long.parseLong(f.getOrDefault("bis", "-1"));
                } catch (NumberFormatException nfe) {
                    bis = -1;
                }
                acknowledge(id, bis);
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
            e.slug = LiveSlug.of(t.getName(), t.getDate());
            e.checkinToken = t.getCheckinToken();
            e.onlineCheckin = t.isOnlineCheckin();
            e.checkinUntil = t.getCheckinUntil();
            e.organizer = organizerOf(ex);
            String w = Tournament.validWorldUrl(t.getWorldUrl());
            e.world = w == null ? "" : w;
            e.state = stateOf(t);
            e.updated = System.currentTimeMillis();
            e.finalOnly = false;                              // neuer Stand: wieder alles zeigen
            e.view = makeView(e, t);
            e.lastAccess = e.updated;
            writeMeta(e);
        }
        for (Entry o : entries.values()) if (o != e && o.slug.equals(e.slug)) o.view = null;   // gleiche Adresse: Verweise neu berechnen
        json(ex, 200, "{\"ok\":true}");
    }

    /** 1.8.0 (#150): Ausrichter aus der Kopfzeile X-HTS-Ausrichter (URL-kodiert); höchstens 120 Zeichen, ohne Steuerzeichen. */
    static String organizerOf(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("X-HTS-Ausrichter");
        if (h == null || h.isBlank()) return "";
        try {
            String n = java.net.URLDecoder.decode(h, StandardCharsets.UTF_8).replaceAll("\\p{Cntrl}", " ").strip();
            return n.length() > 120 ? n.substring(0, 120) : n;
        } catch (IllegalArgumentException e) {
            return "";
        }
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
        Entry e = entries.get(id);
        if (e != null) e.view = null;                         // neues Logo sofort zeigen
        json(ex, 200, "{\"ok\":true}");
    }

    // --- 1.9.0 (#164): Online-Anmeldung --------------------------------------------------------------

    /** Online-Anmeldung jetzt offen? Schalter an, Uhrzeit, Turniertag (Berlin) vor der Uhrzeit, nicht beendet. */
    static boolean checkinOpen(boolean on, java.time.LocalTime until, LocalDate date, String state, boolean finalOnly, java.time.ZonedDateTime now) {
        return on && until != null && date != null && date.equals(now.toLocalDate()) && now.toLocalTime().isBefore(until)
                && !"beendet".equals(state) && !finalOnly;
    }

    private boolean checkinOpen(Entry e) {
        return checkinOpen(e.onlineCheckin, e.checkinUntil, e.date, e.state, e.finalOnly, java.time.ZonedDateTime.now(ZoneId.of("Europe/Berlin")));
    }

    private Entry byToken(String token) {
        if (token == null || token.length() < 20 || !ID.matcher(token).matches()) return null;
        for (Entry e : entries.values()) {
            if (!e.checkinToken.isEmpty() && MessageDigest.isEqual(e.checkinToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) return e;
        }
        return null;
    }

    private void checkin(HttpExchange ex, String rest, String method) throws Exception {
        String[] parts = rest.split("/", 2);
        Entry e = byToken(parts[0]);
        LiveServer v = e == null ? null : view(e);
        if (v == null || !e.onlineCheckin) {
            drain(ex, 64 * 1024);
            html(ex, 404, frame("Anmeldung", "<p>Diesen Anmelde-Link gibt es nicht (mehr). Bitte an die Turnierleitung wenden.</p>"));
            return;
        }
        if ("POST".equals(method)) {
            long now = System.currentTimeMillis();
            if (now - e.checkinWindow > 60_000) {
                e.checkinWindow = now;
                e.checkinPosts.set(0);
            }
            if (e.checkinPosts.incrementAndGet() > 120) {            // Ratenbremse je Turnier
                drain(ex, 64 * 1024);
                json(ex, 429, "{\"ok\":false,\"msg\":" + LiveServer.json("Zu viele Meldungen – bitte kurz warten.") + "}");
                return;
            }
        }
        String sub = parts.length > 1 ? parts[1] : "";
        v.serveCheckin(ex, sub, method, checkinOpen(e), (id, present) -> addReport(e, id, present));
    }

    /** Wird unter dem Turnier-Schloss aufgerufen (aus LiveServer.call). */
    private void addReport(Entry e, String id, boolean present) {
        e.reports.removeIf(r -> r.id().equals(id));
        e.reports.add(new Report(++e.reportSeq, id, present, System.currentTimeMillis()));
        writeReports(e);
    }

    private String reportsJson(String id) {
        Entry e = entries.get(id);
        if (e == null) return "{\"bis\":0,\"meldungen\":[]}";
        synchronized (e.lock) {
            StringBuilder b = new StringBuilder("{\"bis\":").append(e.reportSeq).append(",\"meldungen\":[");
            int i = 0;
            for (Report r : e.reports) {
                b.append(i++ > 0 ? "," : "").append("{\"n\":").append(r.seq()).append(",\"id\":").append(LiveServer.json(r.id()))
                        .append(",\"a\":").append(r.present() ? 1 : 0).append(",\"t\":").append(r.at()).append('}');
            }
            return b.append("]}").toString();
        }
    }

    private void acknowledge(String id, long bis) {
        Entry e = entries.get(id);
        if (e == null || bis < 0) return;
        synchronized (e.lock) {
            if (e.reports.removeIf(r -> r.seq() <= bis)) writeReports(e);
        }
    }

    private void writeReports(Entry e) {
        StringBuilder b = new StringBuilder();
        b.append(e.reportSeq).append('\n');
        for (Report r : e.reports) b.append(r.seq()).append(';').append(r.id()).append(';').append(r.present() ? 1 : 0).append(';').append(r.at()).append('\n');
        try {
            writeAtomic(turniere.resolve(e.id).resolve("anmeldungen.txt"), b.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.println("Anmeldungen von " + e.id + " nicht gespeichert: " + ex.getMessage());
        }
    }

    private void readReports(Entry e, Path dir) {
        Path f = dir.resolve("anmeldungen.txt");
        if (!Files.isRegularFile(f)) return;
        try {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;
            e.reportSeq = Long.parseLong(lines.get(0).strip());
            for (String l : lines.subList(1, lines.size())) {
                String[] x = l.split(";");
                if (x.length == 4 && ID.matcher(x[1]).matches()) e.reports.add(new Report(Long.parseLong(x[0]), x[1], "1".equals(x[2]), Long.parseLong(x[3])));
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("Anmeldungen von " + e.id + " unlesbar: " + ex.getMessage());
        }
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

    /** Turnier zur lesbaren Adresse (bei gleicher Adresse das zuletzt aktualisierte). */
    private Entry bySlug(String slug) {
        Entry best = null;
        for (Entry e : entries.values()) if (slug.equals(e.slug) && (best == null || e.updated > best.updated)) best = e;
        return best;
    }

    /** Adresse eines Turniers: lesbar, außer ein anderes (neueres) Turnier hat dieselbe lesbare Adresse. */
    private String path(Entry e) {
        return "/live/" + (e.slug.isEmpty() || bySlug(e.slug) != e ? e.id : e.slug);
    }

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
        }, path(e), () -> {
            try {
                return Files.exists(logo) ? Files.readAllBytes(logo) : null;
            } catch (IOException ex) {
                return null;
            }
        }, () -> e.organizer, HEAD_EXTRA, footer(), e.finalOnly, () -> e.updated)
                .home(() -> isPast(e.state, e.finalOnly, e.date, LocalDate.now(ZoneId.of("Europe/Berlin"))) ? "/vergangen" : "/");   // 1.9.0 (#165)
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
            e.slug = p.getProperty("slug", LiveSlug.of(e.name, e.date));
            e.organizer = p.getProperty("organizer", "");
            e.world = p.getProperty("world", "");
            e.checkinToken = p.getProperty("checkin-token", "");
            e.onlineCheckin = Boolean.parseBoolean(p.getProperty("online-checkin", "false"));
            String cu = p.getProperty("checkin-until", "");
            e.checkinUntil = cu.isEmpty() ? null : java.time.LocalTime.parse(cu);
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
        p.setProperty("slug", e.slug);
        if (!e.organizer.isEmpty()) p.setProperty("organizer", e.organizer);
        if (!e.world.isEmpty()) p.setProperty("world", e.world);
        if (!e.checkinToken.isEmpty()) p.setProperty("checkin-token", e.checkinToken);
        p.setProperty("online-checkin", String.valueOf(e.onlineCheckin));
        if (e.checkinUntil != null) p.setProperty("checkin-until", e.checkinUntil.toString());
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

    private static final String LOGO_URL = "/favicon.png?v=" + de.hts.HtsApp.VERSION;   // 1.8.0 (#149): Version gegen alten Zwischenspeicher
    private static final String HEAD_EXTRA = "<link rel=\"icon\" href=\"" + LOGO_URL + "\"><meta name=\"robots\" content=\"noindex\">";

    private static final String HOST_CSS = "footer.legal{text-align:center;font-size:13px;padding:18px 12px 0;line-height:2}footer.legal a{color:#187250;margin:0 8px;white-space:nowrap}"
            + ".info h2:first-child{margin-top:4px}.info{line-height:1.5}main>h2:first-child{margin-top:6px}"
            + ".tcard{background:#fff;border:1px solid #e4e6ea;border-radius:12px;margin:8px 0}.tcard>a.cl{display:block;padding:10px 12px;color:inherit;text-decoration:none}"
            + ".tcard .org{display:flex;align-items:center;gap:6px;font-size:12px;color:#4a5560}.tcard .org img{height:28px;width:auto;max-width:64px;object-fit:contain}"
            + ".tcard .tw{display:block;padding:0 12px 10px;font-size:13px}.chip.final{background:#e8ebf0;color:#2d3440}";

    private static String footer() {
        return "<style>" + HOST_CSS + "</style><footer class=\"legal\"><a href=\"/\">Alle Turniere</a><a href=\"/impressum\">Impressum</a>"
                + "<a href=\"/datenschutz\">Datenschutz</a><a href=\"/spenden\">Spenden</a></footer>";
    }

    String frame(String title, String body) {
        return frame(title, "", body);
    }

    /** Seitengerüst; {@code tabs} = Reiterleiste unter dem Kopf (Startseite) oder leer. */
    String frame(String title, String tabs, String body) {
        if (tabs.isEmpty()) body = "<a class=\"homebtn\" href=\"/\">← Alle Turniere</a>" + body;             // 1.9.0 (#165)
        return "<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + LiveServer.esc(title) + " – " + LiveServer.esc(cfg.siteTitle()) + "</title>" + HEAD_EXTRA
                + "<style>" + LiveServer.CSS + HOST_CSS + "</style></head><body><header style=\"padding-bottom:10px\"><div class=\"hb\"><a class=\"brand\" href=\"/\">"
                + "<img src=\"" + LOGO_URL + "\" alt=\"HTS\"><span><b>" + LiveServer.esc(cfg.siteTitle()) + "</b><small>" + LiveServer.esc(title)
                + "</small></span></a></div>" + tabs + "</header><main>" + body + "</main>"
                + footer().replaceFirst("<style>[^<]*</style>", "") + LiveServer.SOURCE_FOOTER + "</body></html>";
    }

    /** Aktuell = noch nicht beendet und Turnierdatum heute oder später (bzw. ohne Datum); sonst vergangen. */
    static boolean isPast(String state, boolean finalOnly, LocalDate date, LocalDate today) {
        return "beendet".equals(state) || finalOnly || (date != null && date.isBefore(today));
    }

    /**
     * 1.8.0 (#157): Startseite mit den Reitern „Aktuelle Turniere“ ({@code /}) und „Vergangene Turniere“ ({@code /vergangen}).
     * Aktuelle: laufende zuerst, dann nach Datum aufsteigend; vergangene: neuestes Datum zuerst.
     */
    String startPage(boolean past) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        List<Entry> list = new ArrayList<>(entries.values());
        list.removeIf(e -> isPast(e.state, e.finalOnly, e.date, today) != past);
        if (past) {
            list.sort(Comparator.comparing((Entry e) -> e.date, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingLong((Entry e) -> e.updated).reversed()));
        } else {
            list.sort(Comparator.comparingInt((Entry e) -> "läuft".equals(e.state) ? 0 : 1)
                    .thenComparing(e -> e.date, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Comparator.comparingLong((Entry e) -> e.updated).reversed()));
        }
        String tabs = "<nav class=\"tabs\" style=\"margin:8px -12px 0;border-bottom:0\"><a href=\"/\"" + (past ? "" : " class=\"on\"")
                + ">Aktuelle Turniere</a><a href=\"/vergangen\"" + (past ? " class=\"on\"" : "") + ">Vergangene Turniere</a></nav>";
        StringBuilder b = new StringBuilder();
        if (list.isEmpty()) {
            b.append("<p class=\"muted\">").append(past ? "Keine vergangenen Turniere." : "Gerade keine aktuellen Turniere.").append("</p>");
        }
        for (Entry e : list) {
            String chip = past ? (e.finalOnly ? "<span class=\"chip final\">Endergebnis</span>" : "<span class=\"chip done\">beendet</span>")
                    : "läuft".equals(e.state) ? "<span class=\"chip run\">läuft</span>" : "<span class=\"chip wait\">demnächst</span>";
            b.append("<div class=\"tcard\"><a class=\"cl\" href=\"").append(path(e)).append("\"><div class=\"row\"><b>").append(LiveServer.esc(e.name))
                    .append("</b>").append(chip).append("</div><div class=\"row\" style=\"margin-top:4px\"><span class=\"muted\">")
                    .append(e.date == null ? "" : DAY.format(e.date) + " · ")
                    .append("Stand ").append(STAMP.format(Instant.ofEpochMilli(e.updated))).append("</span>").append(organizerBadge(e)).append("</div></a>");
            if (!e.world.isEmpty()) {
                b.append("<a class=\"tw\" href=\"").append(LiveServer.esc(e.world)).append("\" target=\"_blank\" rel=\"noopener\">zur Turnierwelt ↗</a>");
            }
            b.append("</div>");
        }
        if (!past) b.append("<script>setTimeout(function(){location.reload();},30000);</script>");
        return frame(past ? "Vergangene Turniere" : "Live-Ergebnisse", tabs, b.toString());
    }

    /** Ausrichter in der Turnierkarte: kleines Logo und Name (soweit vorhanden). */
    private String organizerBadge(Entry e) {
        boolean logo = Files.isRegularFile(turniere.resolve(e.id).resolve("logo.png"));
        if (!logo && e.organizer.isEmpty()) return "";
        return "<span class=\"org\">" + (e.organizer.isEmpty() ? "" : "<span>" + LiveServer.esc(e.organizer) + "</span>")
                + (logo ? "<img src=\"" + path(e) + "/logo.png\" alt=\"" + (e.organizer.isEmpty() ? "Logo des Ausrichters" : "Logo " + LiveServer.esc(e.organizer)) + "\">" : "")
                + "</span>";
    }

    /** 1.8.0 (#153): Seite „Spenden“ – Text aus spenden.html im Konfigurationsordner, sonst die Vorgabe. */
    static final String DONATION_DEFAULT = "<p>Unterstützt als Dankeschön für das Projekt gerne ein Herzensprojekt von mir.</p>"
            + "<p><a href=\"https://www.gofundme.com/f/eine-sportliche-heimat-fur-hamburgs-tischtennistalente?lang=de_DE&amp;ts=1791899912\" "
            + "target=\"_blank\" rel=\"noopener\">Eine sportliche Heimat für Hamburgs Tischtennistalente (GoFundMe) ↗</a></p>";

    String donationPage() {
        Path f = cfg.content().resolve("spenden.html");
        String body;
        try {
            body = Files.isRegularFile(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            body = null;
        }
        if (body == null || body.isBlank()) body = DONATION_DEFAULT;
        return frame("Spenden", "<div class=\"card info\">" + body + "</div>");
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
            String q = ex.getRequestURI().getRawQuery();          // 1.8.0 (#149): mit Version lange, ohne nur kurz zwischenspeichern
            ex.getResponseHeaders().set("Cache-Control", q != null && q.startsWith("v=") ? "max-age=604800" : "max-age=3600");
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
