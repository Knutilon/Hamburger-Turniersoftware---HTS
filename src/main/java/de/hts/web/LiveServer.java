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
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.tournament.Entrant;
import de.hts.tournament.GroupDraw;
import de.hts.tournament.GroupStanding;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Handy-Zugang über das lokale WLAN (0.35): ein kleiner Webserver im Programm (Java-eigener HttpServer).
 * <ul>
 *   <li>{@code /live} – Live-Anzeige, nur lesend: Tische und laufende Spiele, Klassen (Gruppen, KO-Runden,
 *       Ergebnisse), „Meine Spiele“ / „Meine Gruppe“ (das Handy merkt sich den Namen).</li>
 *   <li>{@code /anmeldung/<Schlüssel>} – Anmeldung am Turniertag als Teilnehmende/r (nur sich selbst) oder
 *       Betreuende/r (Verein); der Schlüssel steht im ausgedruckten QR-Code. „Nicht anwesend“ muss die Turnierleitung
 *       bestätigen.</li>
 * </ul>
 * Alle Zugriffe auf das Turnier laufen über {@code ui} (in HTS der JavaFX-Thread), damit nichts gleichzeitig mit der
 * Oberfläche geändert wird.
 */
public final class LiveServer {

    public static final int PORT = 8080;

    private final Tournament t;
    private final Executor ui;
    private final Runnable onChange;
    private final int port;
    private HttpServer server;
    private ExecutorService pool;

    // 1.6.0 (#101): Einsatz im Live-Server auf dem Internetserver (LiveHost) – mehrere Turniere unter /live/<ID>
    private String base = "/live";
    private java.util.function.Supplier<byte[]> logoSource = () -> de.hts.persistence.Organizer.get().logoPng();
    private java.util.function.Supplier<String> orgName = () -> de.hts.persistence.Organizer.get().getName();
    private String extraHead = "";
    private String extraFooter = "";
    private boolean finalOnly;
    private java.util.function.LongSupplier stampSource = System::currentTimeMillis;

    /**
     * @param ui       führt Zugriffe auf das Turnier aus (HTS: {@code Platform::runLater})
     * @param onChange nach einer Anmeldung per Handy (auf {@code ui}) – Ansichten aktualisieren
     */
    public LiveServer(Tournament t, Executor ui, Runnable onChange) {
        this(t, ui, onChange, PORT);
    }

    public LiveServer(Tournament t, Executor ui, Runnable onChange, int port) {
        this.t = t;
        this.ui = ui;
        this.onChange = onChange == null ? () -> { } : onChange;
        this.port = port;
    }

    /**
     * 1.6.0 (#101): Ansicht für den Live-Server im Internet. Nur lesend (keine Anmeldung, keine Ergebnisse per Handy),
     * Adressen unter {@code base} (z. B. „/live/5c0c83c510be“), Logo und Fußzeile vom Server.
     */
    static LiveServer forHost(Tournament t, Executor lock, String base, java.util.function.Supplier<byte[]> logo,
                              String extraHead, String extraFooter, boolean finalOnly, java.util.function.LongSupplier stamp) {
        return forHost(t, lock, base, logo, () -> "", extraHead, extraFooter, finalOnly, stamp);
    }

    /** 1.8.0 (#150): mit Namen des Ausrichters (von HTS beim Hochladen mitgeschickt). */
    static LiveServer forHost(Tournament t, Executor lock, String base, java.util.function.Supplier<byte[]> logo,
                              java.util.function.Supplier<String> org, String extraHead, String extraFooter, boolean finalOnly,
                              java.util.function.LongSupplier stamp) {
        LiveServer v = new LiveServer(t, lock, null, 0);
        v.stampSource = stamp;
        v.base = base;
        v.logoSource = logo;
        v.orgName = org == null ? () -> "" : org;
        v.extraHead = extraHead == null ? "" : extraHead;
        v.extraFooter = extraFooter == null ? "" : extraFooter;
        v.finalOnly = finalOnly;
        return v;
    }

    /** Pfade „/live…“ in der fertigen Seite auf {@code base} umschreiben (Live-Server: „/live/<ID>…“). */
    String rebase(String html) {
        if (html == null || base.equals("/live")) return html;
        return html.replace("\"/live", "\"" + base).replace("'/live", "'" + base);
    }

    public synchronized void start() throws IOException {
        if (server != null) return;
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/", this::handle);
        pool = Executors.newFixedThreadPool(4, r -> {
            Thread th = new Thread(r, "HTS-Handy-Zugang");
            th.setDaemon(true);
            return th;
        });
        s.setExecutor(pool);
        s.start();
        server = s;
    }

    public synchronized void stop() {
        if (server == null) return;
        server.stop(0);
        pool.shutdownNow();
        server = null;
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    /** Tatsächlicher Port (bei Port 0 der vom System vergebene). */
    public synchronized int port() {
        return server == null ? port : server.getAddress().getPort();
    }

    /** IPv4-Adressen dieses Rechners im lokalen Netz (private Bereiche zuerst). */
    public static List<String> localAddresses() {
        List<String> priv = new ArrayList<>(), other = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress() || a.isLinkLocalAddress()) continue;
                    (a.isSiteLocalAddress() ? priv : other).add(a.getHostAddress());
                }
            }
        } catch (IOException ignored) {
            // keine Netzwerkkarte: Liste bleibt leer
        }
        priv.sort(Comparator.comparing((String x) -> x.startsWith("192.168.") ? 0 : x.startsWith("10.") ? 1 : 2));
        priv.addAll(other);
        return priv;
    }

    public static String liveUrl(String host, int port) {
        return "http://" + host + ":" + port + "/live";
    }

    public static String checkinUrl(String host, int port, String token) {
        return "http://" + host + ":" + port + "/anmeldung/" + token;
    }

    /** 1.1 (#93): Adresse der Ergebnisseite eines Tisches. */
    public static String resultUrl(String host, int port, Tournament t, int table) {
        return "http://" + host + ":" + port + "/ergebnis/" + t.tableResultToken(table) + "/" + table;
    }

    // --- Verteilung ------------------------------------------------------------------------------

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            if (!"GET".equals(method) && !"POST".equals(method)) {
                send(ex, 405, "text/plain", "Nicht erlaubt");
                return;
            }
            if (path.equals("/") || path.equals("/live/")) {
                ex.getResponseHeaders().add("Location", "/live");
                send(ex, 302, "text/plain", "");
                return;
            }
            if (path.startsWith("/anmeldung/")) {
                handleCheckin(ex, path.substring("/anmeldung/".length()), method);
                return;
            }
            if (path.startsWith("/ergebnis/")) {                 // 1.1 (#93): Ergebnis per Handy am Tisch
                handleResult(ex, path.substring("/ergebnis/".length()), method);
                return;
            }
            if (!"GET".equals(method)) {
                send(ex, 405, "text/plain", "Nur lesend");
                return;
            }
            serveLive(ex, path, q);
        } catch (Exception e) {
            try {
                send(ex, 500, "text/plain", "Fehler: " + e.getMessage());
            } catch (IOException ignored) {
                // Verbindung schon zu
            }
        }
    }

    /**
     * Die Live-Anzeige (nur lesend). {@code path} in der Form „/live…“; im Live-Server bereits ohne Turnier-Kennung.
     */
    void serveLive(HttpExchange ex, String path, Map<String, String> q) throws Exception {
        {
            if (finalOnly && !(path.equals("/live") || path.equals("/live/ergebnis") || path.equals("/live/logo.png") || path.equals("/live/hts-logo.png"))) {
                ex.getResponseHeaders().add("Location", rebase("\"/live").substring(1));
                send(ex, 302, "text/plain", "");
                return;
            }
            java.util.regex.Matcher mm = MATCH_PATH.matcher(path);             // 1.8.0 (#158): …/<Klasse>/spiel45
            if (mm.matches()) {
                String seg = mm.group(1), ph = mm.group(2);
                int nr = Integer.parseInt(mm.group(3));
                send(ex, 200, "text/html", call(() -> matchPage(seg, ph == null ? 1 : Integer.parseInt(ph), nr)));
                return;
            }
            String html = switch (path) {
                case "/live" -> call(this::overview);
                case "/live/tische" -> call(this::tablesPage);
                case "/live/klasse" -> call(() -> classPage(q.get("k"), q.get("p")));
                case "/live/ergebnis" -> call(() -> classPage(q.get("k"), "e"));
                case "/live/suche" -> call(this::searchPage);
                case "/live/ich" -> call(() -> mePage(q.get("id")));
                case "/live/spieler.json" -> null;
                default -> "";
            };
            if (path.equals("/live/hts-logo.png")) {             // 1.7.0 (#147): HTS-Logo im Seitenkopf
                try (InputStream in = LiveServer.class.getResourceAsStream("/de/hts/ui/hts-logo-klein.png")) {
                    byte[] png = in == null ? new byte[0] : in.readAllBytes();
                    ex.getResponseHeaders().set("Content-Type", "image/png");
                    // 1.8.0 (#149): Adresse mit ?v=<Version> – lange zwischenspeichern; ohne Version nur kurz
                    ex.getResponseHeaders().set("Cache-Control", q.containsKey("v") ? "max-age=604800" : "max-age=3600");
                    ex.sendResponseHeaders(png.length == 0 ? 404 : 200, png.length == 0 ? -1 : png.length);
                    if (png.length > 0) {
                        try (OutputStream os = ex.getResponseBody()) {
                            os.write(png);
                        }
                    }
                }
            } else if (path.equals("/live/logo.png")) {          // 1.5.0 (#139): Logo des Ausrichters
                byte[] png = logoSource.get();
                if (png == null) {
                    send(ex, 404, "text/plain", "");
                } else {
                    ex.getResponseHeaders().set("Content-Type", "image/png");
                    ex.getResponseHeaders().set("Cache-Control", "max-age=300");
                    ex.sendResponseHeaders(200, png.length);
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(png);
                    }
                }
            } else if (path.equals("/live/spieler.json")) {
                send(ex, 200, "application/json", call(this::playersJson));
            } else if (html == null || html.isEmpty()) {
                send(ex, 404, "text/html", page("Nicht gefunden", "<p>Diese Seite gibt es nicht. <a href=\"/live\">Zur Übersicht</a></p>", false));
            } else {
                send(ex, 200, "text/html", html);
            }
        }
    }

    private <T> T call(Callable<T> c) throws Exception {
        FutureTask<T> task = new FutureTask<>(c);
        ui.execute(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            String k = URLDecoder.decode(i < 0 ? part : part.substring(0, i), StandardCharsets.UTF_8);
            String v = i < 0 ? "" : URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }

    // --- HTML-Bausteine -------------------------------------------------------------------------

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '&' -> b.append("&amp;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    static String json(String s) {
        if (s == null) return "\"\"";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '<' -> b.append("\\u003c");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    static final String CSS = """
            *{box-sizing:border-box}body{font-family:-apple-system,"Segoe UI",Roboto,Arial,sans-serif;margin:0;background:#f6f7f8;color:#1f2933;font-size:15px}
            a{color:#187250}
            header{background:#fff;border-bottom:2px solid #187250;padding:10px 12px 0}
            header .hb{display:flex;align-items:center;gap:10px;max-width:1000px;margin:0 auto}
            header .brand{display:flex;align-items:center;gap:10px;text-decoration:none;color:#1f2933;flex:1;min-width:0}
            header .brand img{width:34px;height:34px;border-radius:8px;flex:none}
            header .brand b{display:block;font-size:16px;line-height:1.2}header .brand small{display:block;color:#6b7280;font-size:12px}
            header h1{font-size:16px;margin:0}header .sub{font-size:12px;color:#6b7280}
            header .org{display:flex;align-items:center;gap:6px;flex:none}header .org img{height:40px;width:auto;max-width:90px;object-fit:contain}
            header .org span{font-size:11px;color:#4a5560;max-width:120px;text-align:right}
            .sbox{max-width:1000px;margin:10px auto 0}.sbox input{width:100%;font-size:15px;padding:9px 12px;border:1px solid #d0d5db;border-radius:10px;background:#f1f3f4}
            nav.chips{display:flex;gap:6px;overflow-x:auto;padding:10px 0;max-width:1000px;margin:0 auto;scrollbar-width:none}
            nav.chips a{flex:none;border:1px solid #d0d5db;border-radius:999px;padding:5px 12px;font-size:13px;color:#1f2933;text-decoration:none;background:#fff}
            nav.chips a.on{background:#187250;border-color:#187250;color:#fff}
            main,#sr{padding:10px 12px 30px;max-width:1000px;margin:0 auto}#sr:empty{display:none}
            h2{font-size:16px;margin:16px 0 8px}h3{font-size:14px;color:#4a5560;margin:14px 0 6px;font-weight:600}
            .tabs{display:flex;gap:2px;overflow-x:auto;border-bottom:1px solid #e4e6ea;margin:0 -12px 10px;padding:0 12px;background:#fff}
            .tabs a{flex:none;padding:9px 12px 7px;text-decoration:none;color:#4a5560;border-bottom:3px solid transparent;font-size:13px;text-align:center}
            .tabs a small{display:block;font-size:11px;color:#6b7280}.tabs a.on{color:#1f2933;font-weight:600;border-bottom-color:#5fd3a8}
            .card,.tile{background:#fff;border:1px solid #e4e6ea;border-radius:12px;padding:10px 12px;margin:8px 0}
            a.card{display:block;color:inherit;text-decoration:none}
            .grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}
            @media(min-width:700px){.grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(min-width:1000px){.grid{grid-template-columns:repeat(4,minmax(0,1fr))}}
            .grid .tile{margin:0;cursor:pointer}.tile.open{grid-column:1/-1;border-color:#5fd3a8;cursor:default}
            .tile .th{display:flex;justify-content:space-between;align-items:center;gap:6px}.tile .tb,.tile .ml{display:none}
            .tile.open .tb,.tile.games .ml{display:block}.tile.open .more{display:none}.tile .th b{font-size:14px}
            .bar{height:4px;background:#e8ebf0;border-radius:2px;margin:8px 0 5px}.bar i{display:block;height:4px;background:#5fd3a8;border-radius:2px}
            .row{display:flex;justify-content:space-between;gap:8px;align-items:baseline}
            .muted{color:#6b7280;font-size:12px}.lead{font-size:13px;margin-top:4px;overflow-wrap:anywhere}
            .chip{display:inline-block;font-size:11px;padding:2px 8px;border-radius:999px;background:#e8ebf0;color:#2d3440;white-space:nowrap}
            .run{background:#e4f7ef;color:#0b3d2c}.done{background:#e8ebf0;color:#2d3440}.wait,.queue{background:#fff4d6;color:#7a5200}
            .res{background:#f1f3f4;color:#1f2933;font-weight:600}
            .m{display:flex;justify-content:space-between;gap:8px;padding:7px 0;border-bottom:1px solid #eef0f2;font-size:13px}.m:last-child{border-bottom:0}
            .m .who{min-width:0;overflow-wrap:anywhere}.m .st{flex:none;text-align:right}
            .me{border-left:4px solid #5fd3a8;border-radius:0}.mine{background:#e4f7ef}.win{font-weight:600}
            table{border-collapse:collapse;width:100%;font-size:13px}th,td{padding:5px 4px;text-align:left;border-bottom:1px solid #eef0f2}
            th{color:#6b7280;font-size:11px;font-weight:500}td.n,th.n{text-align:right}
            .gm,.btn,button{display:inline-block;font-size:14px;padding:8px 14px;border-radius:999px;border:0;background:#187250;color:#fff;margin:8px 6px 0 0;text-decoration:none;cursor:pointer}
            .gm,.btn.light,button.light{background:#fff;color:#1f2933;border:1px solid #d0d5db}
            .kl{margin-top:6px;display:flex;gap:4px;flex-wrap:wrap}
            input[type=search],select{width:100%;font-size:16px;padding:10px;border:1px solid #d0d5db;border-radius:8px}
            .ok{color:#187250}.warn{color:#9a3412}
            label.pl{display:flex;gap:10px;align-items:center;padding:8px 0;border-bottom:1px solid #eee}label.pl input{width:22px;height:22px}
            footer.src{padding:16px 12px 24px;text-align:center;font-size:12px;color:#6b7280}footer.src a{color:#6b7280}
            a.m{color:inherit;text-decoration:none}a.m:hover .who{text-decoration:underline}a.tile{display:block;color:inherit;text-decoration:none}
            header .tw{display:block;max-width:1000px;margin:0 auto;padding:2px 0 8px;font-size:13px}
            .back{display:inline-block;font-size:13px;margin:4px 0 6px;text-decoration:none}
            .vs td{padding:8px 4px}.vs .nm{font-size:15px}.big{font-size:20px;font-weight:600}
            .note{background:#e4f7ef;border:1px solid #b9e8d6;border-radius:12px;padding:10px 12px;margin:8px 0;font-size:13px}
            """;

    /**
     * 1.7.0 (#147): Tabellen und Spiele aufklappen, Aktualisierung alle 10 s ohne Neuladen. 1.8.0 (#155): Zustand nur in
     * einer Variablen, solange die Seite offen ist – nichts im Browser gespeichert.
     */
    private static final String TILE_JS = """
            <script>(function(){var s={};
            function ap(){document.querySelectorAll('.tile[data-key]').forEach(function(t){var v=s[t.dataset.key]||0;
            t.classList.toggle('open',v>=1);t.classList.toggle('games',v>=2);var b=t.querySelector('.gm');if(b)b.textContent=v>=2?'Spiele ausblenden':b.dataset.l;});}
            document.addEventListener('click',function(e){var t=e.target.closest('.tile[data-key]');if(!t)return;var v=s[t.dataset.key]||0;
            if(e.target.closest('.gm'))v=v>=2?1:2;else if(e.target.closest('.tb')||e.target.closest('.ml'))return;else v=v>=1?0:1;
            s[t.dataset.key]=v;ap();});window.htsApply=ap;ap();})();</script>
            """;

    /** 1.8.0 (#155/#158): „Link kopieren“ – Teilen-Menü des Handys bzw. Zwischenablage, ohne Speichern im Browser. */
    private static final String SHARE_JS = """
            <script>document.addEventListener('click',function(e){var b=e.target.closest('[data-share]');if(!b)return;e.preventDefault();
            var u=location.href;if(navigator.share){navigator.share({title:document.title,url:u}).catch(function(){});return;}
            if(navigator.clipboard&&window.isSecureContext){navigator.clipboard.writeText(u).then(function(){b.textContent='Link kopiert ✓';});return;}
            window.prompt('Link zum Kopieren:',u);});</script>
            """;

    private static final String REFRESH_JS = """
            <script>setInterval(function(){fetch(location.href,{cache:'no-store'}).then(function(r){return r.ok?r.text():null}).then(function(t){if(!t)return;
            var d=new DOMParser().parseFromString(t,'text/html'),n=d.getElementById('live'),m=document.getElementById('live');
            if(n&&m&&n.innerHTML!==m.innerHTML){m.innerHTML=n.innerHTML;if(window.htsApply)htsApply();}
            var a=d.getElementById('stamp'),b=document.getElementById('stamp');if(a&&b)b.textContent=a.textContent;}).catch(function(){});},10000);</script>
            """;

    private static final String SEARCH_JS = """
            <script>(function(){var q=document.getElementById('q');if(!q)return;var all=null,sr=document.getElementById('sr'),mn=document.getElementById('live');
            function nm(s){return(s||'').toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'');}
            function es(s){return(s||'').replace(/[<>&"]/g,function(c){return{'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c];});}
            function cd(p){return '<div class="card"><b>'+es(p.n)+'</b><div class="muted">'+es(p.c)+'</div><div class="kl">'+(p.k||[]).map(function(x){return '<span class="chip">'+es(x)+'</span>';}).join('')
            +'</div><a class="btn" href="/live/ich?id='+encodeURIComponent(p.id)+'">Meine Spiele</a><div class="muted">Tipp: die Seite „Meine Spiele“ als Lesezeichen speichern</div></div>';}
            function sh(){var w=nm(q.value).split(/\\s+/).filter(function(x){return x;});if(!w.length){sr.innerHTML='';mn.style.display='';return;}
            if(!all){fetch('/live/spieler.json').then(function(r){return r.json();}).then(function(d){all=d;sh();});return;}
            var h=all.filter(function(p){return w.every(function(x){return nm(p.n+' '+p.c).indexOf(x)>=0;});}).slice(0,30);mn.style.display='none';
            sr.innerHTML='<div class="muted">'+h.length+' Treffer</div>'+(h.length?h.map(cd).join(''):'<div class="card">Niemand gefunden – Vor- oder Nachname bzw. Verein versuchen.</div>');}
            q.addEventListener('input',sh);if(location.pathname.slice(-6)==='/suche')q.focus();})();</script>
            """;

    /** 0.42 (#73): AGPL § 13 – wer HTS über das Netzwerk nutzt, kommt an den Quellcode. */
    static final String SOURCE_FOOTER = "<footer class=\"src\">HTS " + de.hts.HtsApp.VERSION + " · freie Software · <a href=\""
            + de.hts.HtsApp.REPO_URL + "\" target=\"_blank\" rel=\"noopener\">Quellcode (AGPL-3.0)</a></footer>";

    /** 1.5.0 (#139): Logo und Name des Ausrichters rechts im Seitenkopf. */
    private String organizerBadge() {
        String name = orgName.get() == null ? "" : orgName.get();
        boolean logo = logoSource.get() != null;
        if (!logo && name.isBlank()) return "";
        return "<div class=\"org\">" + (logo ? "<img src=\"/live/logo.png\" alt=\"Logo " + (name.isBlank() ? "des Ausrichters" : esc(name)) + "\">" : "")
                + (name.isBlank() ? "" : "<span>" + esc(name) + "</span>") + "</div>";
    }

    private static final java.time.format.DateTimeFormatter DAY = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final java.time.format.DateTimeFormatter HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.of("Europe/Berlin"));

    /** Seiten der Anmeldung und Ergebnis-Eingabe (lokaler Handy-Zugang): ohne Suche und Klassenleiste. */
    private String page(String title, String body, boolean reload) {
        return page(title, null, body, reload, false);
    }

    /**
     * 1.7.0 (#147): Seitengerüst – Kopf mit Logo, Turniername, Datum und Stand, Suche, Leiste „Tische“ + Klassen;
     * {@code active} = „tische“ oder der Name der gewählten Klasse.
     */
    private String page(String title, String active, String body, boolean refresh, boolean nav) {
        String name = t.getName() == null || t.getName().isBlank() ? "Turnier" : t.getName();
        StringBuilder h = new StringBuilder("<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + esc(title) + " – " + esc(name)
                + "</title>" + extraHead + "<style>" + CSS + "</style></head><body><header><div class=\"hb\"><a class=\"brand\" href=\"/live\">"
                + "<img src=\"/live/hts-logo.png?v=" + de.hts.HtsApp.VERSION + "\" alt=\"HTS\"><span><b>" + esc(name) + "</b><small>"
                + (t.getDate() == null ? "" : DAY.format(t.getDate()) + " · ") + "<span id=\"stamp\">Stand "
                + HM.format(java.time.Instant.ofEpochMilli(stampSource.getAsLong())) + " Uhr</span></small></span></a>" + organizerBadge() + "</div>");
        String world = Tournament.validWorldUrl(t.getWorldUrl());          // 1.8.0 (#151)
        if (world != null) {
            h.append("<a class=\"tw\" href=\"").append(esc(world)).append("\" target=\"_blank\" rel=\"noopener\">zur Turnierwelt ↗</a>");
        }
        if (nav) {
            if (!finalOnly) h.append("<div class=\"sbox\"><input type=\"search\" id=\"q\" placeholder=\"Spieler oder Verein suchen\" autocomplete=\"off\" aria-label=\"Spieler oder Verein suchen\"></div>");
            h.append("<nav class=\"chips\">");
            if (!finalOnly) h.append("<a href=\"/live/tische\"").append("tische".equals(active) ? " class=\"on\"" : "").append(">Tische</a>");
            for (PlayingClass c : t.rootClasses()) {
                h.append("<a href=\"/live/").append(finalOnly ? "ergebnis" : "klasse").append("?k=").append(enc(c.getName())).append("\"")
                        .append(c.getName().equals(active) ? " class=\"on\"" : "").append(">").append(esc(c.getName())).append("</a>");
            }
            h.append("</nav>");
        }
        h.append("</header><div id=\"sr\"></div><main id=\"live\">").append(body).append("</main>").append(extraFooter).append(SOURCE_FOOTER)
                .append(TILE_JS).append(SHARE_JS);
        if (nav && !finalOnly) h.append(SEARCH_JS);
        if (refresh) h.append(REFRESH_JS);
        return rebase(h.append("</body></html>").toString());
    }

    // --- Live-Anzeige ----------------------------------------------------------------------------

    /** Startseite des Turniers: die laufende Klasse (oder die erste), darüber „Meine Spiele“. */
    private String overview() {
        if (finalOnly) {                                       // 1.6.0 (#101): „Nur Endergebnis behalten“
            StringBuilder b = new StringBuilder("<p class=\"muted\">Das Turnier ist abgeschlossen. Hier stehen nur noch die Endergebnisse.</p><h2>Endergebnisse</h2>");
            for (PlayingClass c : t.rootClasses()) {
                b.append("<a class=\"card\" href=\"/live/ergebnis?k=").append(enc(c.getName())).append("\"><b>").append(esc(c.getName())).append("</b></a>");
            }
            return page("Endergebnisse", null, b.toString(), false, true);
        }
        List<PlayingClass> roots = t.rootClasses();
        if (roots.isEmpty()) return page("Live-Anzeige", null, "<p class=\"muted\">Noch keine Klassen.</p>", true, true);
        PlayingClass sel = roots.stream().filter(c -> c.isStarted() && !c.isFinished()).findFirst()
                .orElse(roots.stream().filter(PlayingClass::isStarted).findFirst().orElse(roots.get(0)));
        return page("Live-Anzeige", sel.getName(), classBody(sel, null), true, true);
    }

    private String matchLine(Match m, String highlightPerson) {
        String a = t.entrantName(m, m.getA()), bb = t.entrantName(m, m.getB());
        String w = m.winner();
        String na = a == null ? "–" : esc(a), nb = bb == null ? "–" : esc(bb);
        String oa = t.originLabel(m, m.getA()), ob = t.originLabel(m, m.getB());      // 0.38: Herkunft, z. B. „A2“
        if (a != null && oa != null) na = "<span class=\"muted\">" + esc(oa) + "</span> " + na;
        if (bb != null && ob != null) nb = "<span class=\"muted\">" + esc(ob) + "</span> " + nb;
        if (w != null && w.equals(m.getA())) na = "<span class=\"win\">" + na + "</span>";
        if (w != null && w.equals(m.getB())) nb = "<span class=\"win\">" + nb + "</span>";
        // 1.7.0 (#147): laufende Spiele nur als „laufend“ (kein Punktestand)
        String chip = switch (m.getStatus()) {
            case ACTIVE, PAUSED -> "<span class=\"chip run\">laufend</span>";
            case QUEUED -> "<span class=\"chip queue\">eingereiht</span>";
            case DONE -> "<span class=\"chip res\">" + esc(m.getResultText()) + "</span>"
                    + (m.getSetsText().isEmpty() ? "" : "<div class=\"muted\">" + esc(m.getSetsText()) + "</div>");
            case CANCELLED -> "<span class=\"chip\">nicht gespielt</span>";
            default -> "<span class=\"chip\">geplant</span>";
        };
        boolean mine = highlightPerson != null && t.personsOf(m).contains(highlightPerson);
        // 1.8.0 (#158): jedes Spiel führt auf seine eigene Seite
        return "<a class=\"m" + (mine ? " mine" : "") + "\" href=\"" + matchHref(m) + "\"><div class=\"who\">" + na + " – " + nb
                + "<div class=\"muted\">Spiel " + m.getNumber() + " · " + esc(className(m)) + " · " + esc(m.getStage())
                + "</div></div><div class=\"st\">" + chip + "</div></a>";
    }

    private String className(Match m) {
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        if (c == null) return m.getClassName();
        PlayingClass root = t.rootOf(c);
        return root.getName() + (c.isPhase() ? " · Phase " + t.phaseNumber(c) : "");
    }

    private String tablesPage() {
        StringBuilder b = new StringBuilder("<h2>An den Tischen</h2><div class=\"grid\">");
        int shown = 0;
        for (int nr = 1; nr <= t.getTableCount(); nr++) {
            if (t.isTableDisabled(nr)) continue;
            shown++;
            Match m = t.activeMatchAt(nr).orElse(null);
            b.append(m == null ? "<div class=\"tile\" style=\"cursor:default\">" : "<a class=\"tile\" href=\"" + matchHref(m) + "\">")
                    .append("<div class=\"th\"><b>").append(esc(t.tableName(nr))).append("</b>")
                    .append(m == null ? "<span class=\"chip\">frei</span>" : "<span class=\"chip run\">laufend</span>").append("</div>");
            if (m != null) {
                String a = t.entrantName(m, m.getA()), bb = t.entrantName(m, m.getB());
                b.append("<div class=\"lead\">").append(esc(a == null ? "–" : a)).append("<br>").append(esc(bb == null ? "–" : bb))
                        .append("</div><div class=\"muted\">").append(esc(className(m))).append(" · ").append(esc(m.getStage()))
                        .append(" · Spiel ").append(m.getNumber()).append("</div>");
            }
            b.append(m == null ? "</div>" : "</a>");
        }
        b.append("</div>");
        if (shown == 0) b.append("<p class=\"muted\">Keine Tische.</p>");
        List<Match> queue = t.queue();
        b.append("<h2>Als Nächstes</h2>");
        if (queue.isEmpty()) b.append("<p class=\"muted\">Keine eingereihten Spiele.</p>");
        else {
            b.append("<div class=\"card\">");
            for (Match m : queue.subList(0, Math.min(20, queue.size()))) b.append(matchLine(m, null));
            b.append("</div>");
        }
        return page("Tische", "tische", b.toString(), true, true);
    }

    private String standingsTable(PlayingClass c, int g, String highlightPerson) {
        StringBuilder b = new StringBuilder("<table><tr><th>Pl.</th><th>Name</th><th class=\"n\">Sp</th><th class=\"n\">S:N</th><th class=\"n\">Sätze</th></tr>");
        for (GroupStanding.Row r : t.standings(c, g)) {
            boolean mine = highlightPerson != null && involves(r.entrant(), highlightPerson);
            b.append("<tr").append(mine ? " class=\"win\"" : "").append("><td>").append(r.place()).append("</td><td>").append(esc(r.entrant().name()))
                    .append(r.entrant().detail() != null ? "<div class=\"muted\">" + esc(r.entrant().detail()) + "</div>" : "")
                    .append("</td><td class=\"n\">").append(r.played()).append("</td><td class=\"n\">").append(r.won()).append(':').append(r.lost())
                    .append("</td><td class=\"n\">").append(r.setsWon()).append(':').append(r.setsLost()).append("</td></tr>");
        }
        return b.append("</table>").toString();
    }

    private boolean involves(Entrant e, String personId) {
        if (e == null) return false;
        if (e.id().equals(personId)) return true;
        return e.id().startsWith("pair:") && t.getPairs().stream().anyMatch(p -> ("pair:" + p.getId()).equals(e.id())
                && p.members().stream().anyMatch(x -> x != null && x.getId().equals(personId)));
    }

    private String classPage(String name, String tab) {
        PlayingClass root = name == null ? null : t.findClass(name).map(t::rootOf).orElse(null);
        if (root == null) return "";
        if (finalOnly) tab = "e";
        return page(root.getName(), root.getName(), classBody(root, tab), true, true);
    }

    /** 1.7.0 (#147): Reiter je Phase + „Ergebnis“; {@code tab} = Phasennummer, „e“ oder {@code null} (laufende Phase). */
    private String classBody(PlayingClass root, String tab) {
        List<PlayingClass> phases = t.phasesOf(root).stream().filter(PlayingClass::isDrawn).toList();
        int sel = -1;
        boolean result = "e".equals(tab) || finalOnly;
        if (!result) {
            try {
                sel = tab == null ? -1 : Integer.parseInt(tab) - 1;
            } catch (NumberFormatException e) {
                sel = -1;
            }
            if (sel < 0 || sel >= phases.size()) {
                sel = -1;
                for (int i = phases.size() - 1; i >= 0 && sel < 0; i--) if (phases.get(i).isStarted()) sel = i;
                if (sel < 0) sel = phases.size() - 1;
            }
            if (sel < 0) result = root.isFinished();
        }
        StringBuilder b = new StringBuilder("<div class=\"tabs\">");
        String base = "/live/klasse?k=" + enc(root.getName());
        if (!finalOnly) {
            for (int i = 0; i < phases.size(); i++) {
                b.append("<a href=\"").append(base).append("&amp;p=").append(i + 1).append("\"").append(!result && i == sel ? " class=\"on\"" : "")
                        .append(">Phase ").append(i + 1).append("<small>").append(esc(modeName(phases.get(i)))).append("</small></a>");
            }
        }
        b.append("<a href=\"").append(base).append("&amp;p=e\"").append(result ? " class=\"on\"" : "").append(">")
                .append(root.isFinished() ? "Ergebnis<small>Endergebnis</small>" : "Ergebnis<small>Zwischenstand</small>").append("</a></div>");
        if (result) {
            b.append(resultBody(root));
        } else if (sel < 0) {
            b.append("<p class=\"muted\">Noch nicht ausgelost.</p>");
        } else {
            phaseBody(b, phases.get(sel), null);
        }
        return b.toString();
    }

    private static String modeName(PlayingClass c) {
        return switch (c.getMode()) {
            case GROUPS -> "Gruppen";
            case KNOCKOUT -> "KO-Runde";
            case SWISS -> c.isBadeni() ? "Schweizer System (Badeni)" : "Schweizer System";
            case PLACEMENT -> "Platzierungsspiele";
            case QUALIFICATION -> "Qualifikation";
            case PLAN -> c.getPlan() == null ? "Platzierungsplan" : c.getPlan().getKind().getLabel();
            default -> "";
        };
    }

    private void phaseBody(StringBuilder b, PlayingClass phase, String person) {
        List<Match> ms = new ArrayList<>(t.matchesOf(phase));
        ms.removeIf(t::isInactiveMatch);
        ms.sort(Comparator.comparingInt(Match::getNumber));
        TournamentMode mode = phase.getMode();
        if (mode == TournamentMode.GROUPS || mode == TournamentMode.PLACEMENT || mode == TournamentMode.SWISS) {
            b.append("<div class=\"grid\">");
            int groups = mode == TournamentMode.SWISS ? Math.min(1, phase.getGroups().size()) : phase.getGroups().size();
            for (int g = 0; g < groups; g++) {
                final int gi = g;
                List<Match> gm = mode == TournamentMode.SWISS ? ms : ms.stream().filter(m -> m.getGroupIndex() == gi).toList();
                b.append(groupTile(phase, g, mode == TournamentMode.SWISS ? "Tabelle" : "Gruppe " + t.groupLabel(phase, g), gm, person));
            }
            b.append("</div>");
            if (mode == TournamentMode.PLACEMENT) {
                List<Match> rest = ms.stream().filter(m -> m.getGroupIndex() < 0).toList();
                if (!rest.isEmpty()) {
                    b.append("<h3>Platzierungsspiele</h3><div class=\"card\">");
                    for (Match m : rest) b.append(matchLine(m, person));
                    b.append("</div>");
                }
            }
        } else {
            Map<String, List<Match>> byStage = new LinkedHashMap<>();
            ms.sort(Comparator.comparingInt((Match m) -> m.getKoRound() >= 0 ? m.getKoRound() : m.getRound()).thenComparingInt(Match::getNumber));
            for (Match m : ms) if (m.getA() != null || m.getB() != null) byStage.computeIfAbsent(m.getStage(), k -> new ArrayList<>()).add(m);
            if (byStage.isEmpty()) b.append("<p class=\"muted\">Noch keine Spiele.</p>");
            byStage.forEach((stage, list) -> {
                b.append("<h3>").append(esc(stage)).append("</h3><div class=\"card\">");
                for (Match m : list) b.append(matchLine(m, person));
                b.append("</div>");
            });
        }
    }

    /** Kachel einer Gruppe: zu → Status, Fortschritt, Erster; 1× antippen → Tabelle; „Spiele anzeigen“ → Spiele. */
    private String groupTile(PlayingClass phase, int g, String title, List<Match> gm, String person) {
        long counted = gm.stream().filter(m -> m.getStatus() != MatchStatus.CANCELLED).count();
        long done = gm.stream().filter(m -> m.getStatus() == MatchStatus.DONE).count();
        boolean running = gm.stream().anyMatch(m -> m.getStatus().atTable());
        String status = !phase.isStarted() ? "<span class=\"chip wait\">wartet</span>"
                : counted > 0 && done == counted ? "<span class=\"chip done\">beendet</span>"
                : running || done > 0 ? "<span class=\"chip run\">läuft</span>" : "<span class=\"chip wait\">wartet</span>";
        List<GroupStanding.Row> rows = t.standings(phase, g);
        String lead = rows.isEmpty() || done == 0 ? "noch keine Ergebnisse" : "1. " + esc(rows.get(0).entrant().name());
        int pct = counted == 0 ? 0 : (int) Math.round(100.0 * done / counted);
        StringBuilder b = new StringBuilder("<div class=\"tile\" data-key=\"").append(esc(phase.getName())).append('|').append(g).append("\">")
                .append("<div class=\"th\"><b>").append(esc(title)).append("</b>").append(status).append("</div>")
                .append("<div class=\"bar\"><i style=\"width:").append(pct).append("%\"></i></div><div class=\"muted\">").append(done)
                .append(" von ").append(counted).append(" Spielen</div><div class=\"lead\">").append(lead).append("</div>")
                .append("<div class=\"muted more\">Antippen für die Tabelle</div>")
                .append("<div class=\"tb\">").append(standingsTable(phase, g, person));
        String label = "Spiele anzeigen (" + counted + ")";
        b.append("<button type=\"button\" class=\"gm\" data-l=\"").append(esc(label)).append("\">").append(esc(label)).append("</button></div>");
        b.append("<div class=\"ml\">");
        for (Match m : gm) b.append(matchLine(m, person));
        return b.append("</div></div>").toString();
    }

    private String resultBody(PlayingClass root) {
        Tournament.FinalResult r = t.finalResult(root);
        if (r.rows().isEmpty()) return "<p class=\"muted\">Noch keine Platzierungen.</p>";
        StringBuilder b = new StringBuilder();
        List<Tournament.ResultSection> secs = t.finalResultSections(root);
        for (Tournament.ResultSection sec : secs.size() > 1 ? secs : List.of(new Tournament.ResultSection("", r.rows()))) {
            if (!sec.title().isEmpty()) b.append("<h3>").append(esc(sec.title())).append("</h3>");
            b.append("<div class=\"card\"><table><tr><th>Platz</th><th>Name</th><th>Ermittelt durch</th></tr>");
            for (Tournament.Placing p : sec.rows()) {
                b.append("<tr><td>").append(esc(p.placeText())).append("</td><td>").append(esc(p.entrant().name()))
                        .append(p.entrant().detail() != null ? "<div class=\"muted\">" + esc(p.entrant().detail()) + "</div>" : "")
                        .append("</td><td class=\"muted\">").append(esc(p.info())).append("</td></tr>");
            }
            b.append("</table></div>");
        }
        if (!r.complete()) b.insert(0, "<p class=\"muted\">Zwischenstand – noch nicht alle Plätze stehen fest.</p>");
        return b.toString();
    }

    private String playersJson() {
        // 1.7.0 (#147): je Person auch die Klassen (für die Suche)
        Map<String, List<String>> classes = new java.util.HashMap<>();
        for (PlayingClass c : t.rootClasses()) {
            for (Entrant e : t.entrantsOf(c)) {
                if (e.id().startsWith("pair:")) {
                    t.getPairs().stream().filter(p -> ("pair:" + p.getId()).equals(e.id())).flatMap(p -> p.members().stream())
                            .filter(java.util.Objects::nonNull).forEach(x -> addClass(classes, x.getId(), c.getName()));
                } else {
                    addClass(classes, e.id(), c.getName());
                }
            }
        }
        StringBuilder b = new StringBuilder("[");
        List<Player> list = new ArrayList<>(sortedPlayers());
        list.removeIf(Player::isReserveOnly);                    // 0.38 (#55): Nachrücker melden sich nicht an oder ab
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            if (i > 0) b.append(',');
            b.append("{\"id\":").append(json(p.getId())).append(",\"n\":").append(json(p.getDisplayName()))
                    .append(",\"c\":").append(json(p.getClubName())).append(",\"s\":").append(json(p.getAttendance().code())).append(",\"k\":[");
            List<String> ks = classes.getOrDefault(p.getId(), List.of());
            for (int j = 0; j < ks.size(); j++) b.append(j > 0 ? "," : "").append(json(ks.get(j)));
            b.append("]}");
        }
        return b.append(']').toString();
    }

    private static void addClass(Map<String, List<String>> m, String id, String cls) {
        List<String> l = m.computeIfAbsent(id, k -> new ArrayList<>());
        if (!l.contains(cls)) l.add(cls);
    }

    private List<Player> sortedPlayers() {
        Collator col = Collator.getInstance(Locale.GERMAN);
        List<Player> list = new ArrayList<>(t.getPlayers());
        list.sort(Comparator.comparing((Player p) -> p.getDisplayName() == null ? "" : p.getDisplayName(), col));
        return list;
    }

    /** Alte Adresse „Meine Spiele suchen“: die Startseite mit dem Suchfeld oben. */
    private String searchPage() {
        return overview();
    }

    private String mePage(String id) {
        Player p = id == null ? null : t.getPlayers().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (p == null) return page("Meine Spiele", null, "<p>Person nicht gefunden. Oben im Suchfeld neu suchen.</p>", false, true);
        List<Match> mine = new ArrayList<>();
        for (Match m : t.getMatches()) if (!t.isInactiveMatch(m) && t.personsOf(m).contains(id)) mine.add(m);
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"card me\"><b>").append(esc(p.getDisplayName())).append("</b><div class=\"muted\">").append(esc(p.getClubName()))
                .append("</div></div>");
        // 1.8.0 (#155): Lesezeichen statt Speichern im Browser
        b.append("<div class=\"note\">Diese Seite als Lesezeichen speichern bzw. zum Startbildschirm hinzufügen – dann sind die eigenen "
                + "Spiele mit einem Tippen wieder da.<br><a class=\"btn light\" href=\"#\" data-share=\"1\">Link kopieren</a></div>");
        List<Match> now = mine.stream().filter(m -> m.getStatus().atTable()).toList();
        List<Match> next = mine.stream().filter(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.PLANNED)
                .sorted(Comparator.comparingInt((Match m) -> m.getStatus() == MatchStatus.QUEUED ? 0 : 1).thenComparingInt(Match::getNumber)).toList();
        List<Match> done = mine.stream().filter(m -> m.getStatus() == MatchStatus.DONE).sorted(Comparator.comparingInt(Match::getNumber).reversed()).toList();
        if (!now.isEmpty()) {
            b.append("<h2>Jetzt am Tisch</h2><div class=\"card\">");
            for (Match m : now) {
                b.append(matchLine(m, id));
                if (m.getTable() != null) b.append("<div class=\"muted\">").append(esc(t.tableName(m.getTable()))).append("</div>");
            }
            b.append("</div>");
        }
        b.append("<h2>Als Nächstes</h2>");
        if (next.isEmpty()) b.append("<p class=\"muted\">Keine offenen Spiele.</p>");
        else {
            b.append("<div class=\"card\">");
            for (Match m : next) b.append(matchLine(m, id));
            b.append("</div>");
        }
        java.util.Set<String> shown = new TreeSet<>();
        for (Match m : mine) {
            PlayingClass c = t.findClass(m.getClassName()).orElse(null);
            if (c == null || m.getGroupIndex() < 0 || !(c.getMode() == TournamentMode.GROUPS || c.getMode() == TournamentMode.PLACEMENT)) continue;
            if (!shown.add(c.getName() + "#" + m.getGroupIndex())) continue;
            b.append("<h2>Meine Gruppe – ").append(esc(className(m))).append(", Gruppe ").append(t.groupLabel(c, m.getGroupIndex())).append("</h2>");
            b.append("<div class=\"card\">").append(standingsTable(c, m.getGroupIndex(), id)).append("</div>");
        }
        if (!done.isEmpty()) {
            b.append("<h2>Gespielt</h2><div class=\"card\">");
            for (Match m : done) b.append(matchLine(m, id));
            b.append("</div>");
        }
        return page("Meine Spiele", null, b.toString(), true, true);
    }

    // --- 1.8.0 (#158): eigene Seite je Spiel ---------------------------------------------------------

    /** „/live/<Klasse>/spiel45“ bzw. „/live/<Klasse>/phase2/spiel3“ (Spielnummern zählen je Klasse und Phase). */
    static final java.util.regex.Pattern MATCH_PATH = java.util.regex.Pattern.compile("/live/(.+?)(?:/phase(\\d{1,3}))?/spiel(\\d{1,6})");

    /** Klassenname in der Adresse: lesbar, Leerzeichen → „_“ (z. B. „Herren_C“). */
    static String classSegment(String name) {
        return name == null ? "" : name.strip().replaceAll("\\s+", "_");
    }

    /** Adresse der Spielseite (vor {@link #rebase}). */
    String matchHref(Match m) {
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        PlayingClass root = c == null ? null : t.rootOf(c);
        String seg = classSegment(root == null ? m.getClassName() : root.getName());
        int phase = c == null || root == null || c == root ? 1 : Math.max(1, t.phaseNumber(c));
        return "/live/" + URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20") + (phase > 1 ? "/phase" + phase : "")
                + "/spiel" + m.getNumber();
    }

    private String matchPage(String seg, int phaseNo, int nr) {
        PlayingClass root = t.rootClasses().stream().filter(c -> classSegment(c.getName()).equals(seg)).findFirst().orElse(null);
        if (root == null) {
            return page("Spiel nicht gefunden", null, "<h2>Spiel nicht gefunden</h2><p>Diese Klasse gibt es in diesem Turnier nicht. "
                    + "<a href=\"/live\">Zur Übersicht</a></p>", false, true);
        }
        List<PlayingClass> phases = t.phasesOf(root);
        PlayingClass phase = phaseNo >= 1 && phaseNo <= phases.size() ? phases.get(phaseNo - 1) : null;
        Match m = phase == null ? null : t.matchesOf(phase).stream().filter(x -> x.getNumber() == nr && !t.isInactiveMatch(x)).findFirst().orElse(null);
        String back = "/live/klasse?k=" + enc(root.getName()) + "&amp;p=" + Math.max(1, phaseNo);
        String backText = "← " + esc(root.getName()) + (phases.size() > 1 ? " · Phase " + Math.max(1, phaseNo) : "");
        if (m == null) {
            return page("Spiel nicht gefunden", root.getName(), "<a class=\"back\" href=\"" + back + "\">" + backText + "</a><h2>Spiel nicht gefunden</h2>"
                    + "<p>Spiel " + nr + " gibt es in dieser Klasse (nicht mehr) – vielleicht wurde neu ausgelost.</p>", true, true);
        }
        String a = t.entrantName(m, m.getA()), bb = t.entrantName(m, m.getB());
        String w = m.winner();
        String chip = switch (m.getStatus()) {
            case ACTIVE, PAUSED -> "<span class=\"chip run\">laufend</span>";
            case QUEUED -> "<span class=\"chip queue\">eingereiht</span>";
            case DONE -> "<span class=\"chip done\">beendet</span>";
            case CANCELLED -> "<span class=\"chip\">nicht gespielt</span>";
            default -> "<span class=\"chip\">geplant</span>";
        };
        StringBuilder b = new StringBuilder("<a class=\"back\" href=\"").append(back).append("\">").append(backText).append("</a>");
        b.append("<div class=\"card\"><div class=\"row\"><b class=\"big\">Spiel ").append(m.getNumber()).append("</b>").append(chip).append("</div>")
                .append("<div class=\"muted\">").append(esc(className(m))).append(" · ").append(esc(m.getStage()));
        if (m.getTable() != null && m.getStatus() != MatchStatus.DONE) b.append(" · ").append(esc(t.tableName(m.getTable())));
        b.append("</div><table class=\"vs\">");
        b.append(sideRow(m, m.getA(), a, w));
        b.append(sideRow(m, m.getB(), bb, w));
        b.append("</table>");
        if (m.getStatus() == MatchStatus.DONE) {
            b.append("<div class=\"row\" style=\"margin-top:6px\"><span class=\"muted\">Ergebnis</span><span class=\"chip res\">")
                    .append(esc(m.getResultText())).append("</span></div>");
            if (!m.getSetsText().isEmpty()) b.append("<div class=\"muted\" style=\"text-align:right\">Sätze ").append(esc(m.getSetsText())).append("</div>");
        }
        b.append("</div><a class=\"btn light\" href=\"#\" data-share=\"1\">Link kopieren</a>");
        return page("Spiel " + m.getNumber() + " – " + root.getName(), root.getName(), b.toString(), true, true);
    }

    private String sideRow(Match m, String id, String name, String winner) {
        if (id == null || name == null) return "<tr><td class=\"nm muted\">noch offen</td></tr>";
        String origin = t.originLabel(m, id);
        String detail = null;
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        if (c != null) {
            detail = t.entrantsOf(c).stream().filter(e -> e.id().equals(id)).map(Entrant::detail).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        }
        if (detail == null) detail = t.getPlayers().stream().filter(p -> p.getId().equals(id)).map(Player::getClubName).findFirst().orElse(null);
        boolean win = id.equals(winner);
        return "<tr><td class=\"nm" + (win ? " win" : "") + "\">" + (origin == null ? "" : "<span class=\"muted\">" + esc(origin) + "</span> ")
                + esc(name) + (detail == null || detail.isBlank() ? "" : "<div class=\"muted\">" + esc(detail) + "</div>") + "</td></tr>";
    }

    // --- 1.1 (#93): Ergebnis per Handy ----------------------------------------------------------

    private void handleResult(HttpExchange ex, String rest, String method) throws Exception {
        String[] parts = rest.split("/");
        int table;
        try {
            table = parts.length >= 2 ? Integer.parseInt(parts[1]) : -1;
        } catch (NumberFormatException e) {
            table = -1;
        }
        final int tb = table;
        boolean ok = tb >= 1 && call(() -> t.isPhoneResults() && tb <= t.getTableCount() && parts[0].equals(t.tableResultToken(tb)));
        if (!ok) {
            send(ex, 404, "text/html", page("Nicht gefunden", "<p>Diese Seite gibt es nicht (oder Ergebnisse per Handy sind ausgeschaltet).</p>", false));
            return;
        }
        if (parts.length == 2 && "GET".equals(method)) {
            send(ex, 200, "text/html", call(() -> resultEntryPage(tb)));
        } else if (parts.length == 3 && parts[2].equals("melden") && "POST".equals(method)) {
            byte[] body;
            try (InputStream in = ex.getRequestBody()) {
                body = in.readNBytes(16 * 1024);
            }
            Map<String, String> form = parseQuery(new String(body, StandardCharsets.UTF_8));
            send(ex, 200, "application/json", call(() -> submitResult(tb, form.getOrDefault("nr", ""), form.getOrDefault("saetze", ""))));
        } else {
            send(ex, 404, "text/plain", "Nicht gefunden");
        }
    }

    /** „11:5,9:11,11:7“ → Sätze; leere Sätze werden übersprungen. */
    static List<int[]> parseSets(String s) {
        List<int[]> out = new ArrayList<>();
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty() || p.equals(":")) continue;
            String[] ab = p.split(":");
            if (ab.length != 2 || ab[0].isBlank() || ab[1].isBlank()) throw new IllegalArgumentException("Bitte jeden Satz vollständig angeben, z. B. 11:7.");
            out.add(new int[]{Integer.parseInt(ab[0].trim()), Integer.parseInt(ab[1].trim())});
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Bitte die Sätze eintragen.");
        return out;
    }

    private String submitResult(int table, String nr, String sets) {
        try {
            boolean direct = t.submitPhoneResult(table, Integer.parseInt(nr.trim()), parseSets(sets));
            onChange.run();
            return "{\"ok\":true,\"msg\":" + json(direct ? "Ergebnis eingetragen – danke!" : "Ergebnis gemeldet – die Turnierleitung bestätigt es.") + "}";
        } catch (NumberFormatException e) {
            return "{\"ok\":false,\"msg\":" + json("Bitte nur Zahlen eintragen.") + "}";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "{\"ok\":false,\"msg\":" + json(e.getMessage()) + "}";
        }
    }

    private String resultEntryPage(int table) {
        Match m = t.activeMatchAt(table).orElse(null);
        StringBuilder b = new StringBuilder();
        b.append("<h2>").append(esc(t.tableName(table))).append("</h2>");
        if (m == null || m.getStatus() != de.hts.tournament.MatchStatus.ACTIVE) {
            b.append("<p class=\"muted\">Gerade läuft hier kein Spiel. Seite neu laden, sobald das nächste Spiel am Tisch ist.</p>")
                    .append("<button onclick=\"location.reload()\">Neu laden</button>");
            return page("Ergebnis eintragen", b.toString(), false);
        }
        int maxSets = t.winningSets(m) * 2 - 1;
        String a = t.entrantName(m, m.getA()), bn = t.entrantName(m, m.getB());
        b.append("<div class=\"card\"><div class=\"muted\">").append(esc(className(m))).append(" · ").append(esc(m.getStage()))
                .append(" · Spiel ").append(m.getNumber()).append("</div><p><b>").append(esc(a)).append("</b><br>gegen<br><b>")
                .append(esc(bn)).append("</b></p></div>");
        b.append("<div class=\"card\"><table class=\"sets\"><tr><th>Satz</th><th>").append(esc(a)).append("</th><th>")
                .append(esc(bn)).append("</th></tr>");
        for (int k = 1; k <= maxSets; k++) {
            b.append("<tr><td>").append(k).append("</td><td><input inputmode=\"numeric\" class=\"pa\" size=\"3\"></td>")
                    .append("<td><input inputmode=\"numeric\" class=\"pb\" size=\"3\"></td></tr>");
        }
        b.append("</table><button onclick=\"send()\">Ergebnis melden</button></div><div id=\"msg\" class=\"card ok\" style=\"display:none\"></div>");
        b.append("<script>function send(){var a=[].map.call(document.querySelectorAll('.pa'),x=>x.value.trim()),")
                .append("b=[].map.call(document.querySelectorAll('.pb'),x=>x.value.trim()),s=[];")
                .append("for(var i=0;i<a.length;i++){if(a[i]===''&&b[i]==='')continue;s.push(a[i]+':'+b[i]);}")
                .append("fetch(location.pathname+'/melden',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},")
                .append("body:'nr=").append(m.getNumber()).append("&saetze='+encodeURIComponent(s.join(','))}).then(r=>r.json()).then(d=>{")
                .append("var e=document.getElementById('msg');e.style.display='block';e.className='card '+(d.ok?'ok':'warn');e.textContent=d.msg;})")
                .append(".catch(()=>{var e=document.getElementById('msg');e.style.display='block';e.className='card warn';e.textContent='Keine Verbindung zum Turnier-Laptop.';});}</script>");
        return page("Ergebnis eintragen", b.toString(), false);
    }

    // --- Anmeldung -------------------------------------------------------------------------------

    private void handleCheckin(HttpExchange ex, String rest, String method) throws Exception {
        String token = call(t::getCheckinToken);
        String[] parts = rest.split("/", 2);
        if (!parts[0].equalsIgnoreCase(token)) {
            send(ex, 404, "text/html", page("Nicht gefunden", "<p>Diese Seite gibt es nicht.</p>", false));
            return;
        }
        String sub = parts.length > 1 ? parts[1] : "";
        if (sub.isEmpty() && "GET".equals(method)) {
            send(ex, 200, "text/html", call(this::checkinPage));
        } else if (sub.equals("daten.json") && "GET".equals(method)) {
            send(ex, 200, "application/json", call(this::checkinData));
        } else if (sub.equals("melden") && "POST".equals(method)) {
            byte[] body;
            try (InputStream in = ex.getRequestBody()) {
                body = in.readNBytes(64 * 1024);
            }
            Map<String, String> form = parseQuery(new String(body, StandardCharsets.UTF_8));
            List<String> ids = form.getOrDefault("ids", "").isBlank() ? List.of() : List.of(form.get("ids").split(","));
            boolean present = "1".equals(form.get("anwesend"));
            send(ex, 200, "application/json", call(() -> report(ids, present)));
        } else {
            send(ex, 404, "text/plain", "Nicht gefunden");
        }
    }

    /** Anmeldung bzw. Abmeldung per Handy (auf {@code ui}). */
    private String report(List<String> ids, boolean present) {
        if (!t.isCheckinOpen()) return "{\"ok\":false,\"msg\":" + json("Die Anmeldung ist geschlossen – bitte an die Turnierleitung wenden.") + "}";
        int changed = 0, locked = 0, same = 0;
        for (String id : ids) {
            Player p = t.getPlayers().stream().filter(x -> x.getId().equals(id.trim())).findFirst().orElse(null);
            if (p == null) continue;
            if (p.getAttendance() == Attendance.WITHDRAWN) {
                locked++;
                continue;
            }
            if (t.checkinFromPhone(p, present)) changed++;
            else same++;
        }
        if (changed > 0) onChange.run();
        String msg = present ? changed + (changed == 1 ? " Person" : " Personen") + " als anwesend gemeldet."
                : changed + (changed == 1 ? " Person" : " Personen") + " als nicht anwesend gemeldet – die Turnierleitung bestätigt das.";
        if (same > 0) msg += " " + same + (same == 1 ? " war" : " waren") + " bereits so gemeldet.";
        if (locked > 0) msg += " " + locked + " bereits endgültig abgemeldet – bitte an die Turnierleitung wenden.";
        return "{\"ok\":true,\"changed\":" + changed + ",\"msg\":" + json(msg) + "}";
    }

    private String checkinData() {
        StringBuilder b = new StringBuilder("{\"open\":").append(t.isCheckinOpen()).append(",\"players\":").append(playersJson()).append(",\"clubs\":[");
        Collator col = Collator.getInstance(Locale.GERMAN);
        Set<String> clubs = new TreeSet<>(col);
        for (Player p : t.getPlayers()) if (p.getClubName() != null && !p.getClubName().isBlank()) clubs.add(p.getClubName());
        int i = 0;
        for (String c : clubs) b.append(i++ > 0 ? "," : "").append(json(c));
        return b.append("]}").toString();
    }

    private String checkinPage() {
        String body = """
                <div id="closed" class="card warn" style="display:none">Die Anmeldung ist geschlossen. Bitte an die Turnierleitung wenden.</div>
                <div id="role"><p>Wer meldet an?</p>
                 <button onclick="pickRole('self')">Ich bin Teilnehmende/r</button><button class="light" onclick="pickRole('coach')">Ich bin Betreuende/r</button></div>
                <div id="self" style="display:none"><h2>Teilnehmende/r</h2>
                 <p class="muted">Eigenen Namen suchen (Vorname, Nachname oder Verein) und auswählen.</p>
                 <input type="search" id="q" placeholder="z. B. Müller Eimsbüttel"><div id="hits"></div>
                 <div id="chosen" class="card" style="display:none"></div></div>
                <div id="coach" style="display:none"><h2>Betreuende/r</h2>
                 <select id="club"><option value="">Verein wählen …</option></select>
                 <div id="clubList"></div>
                 <div id="coachBtns" style="display:none"><button onclick="sendSel(true)">Ausgewählte: anwesend</button>
                 <button class="light" onclick="sendSel(false)">Ausgewählte: nicht anwesend</button></div></div>
                <div id="msg" class="card ok" style="display:none"></div>
                <script>
                var base=location.pathname.replace(/\\/$/,''),data={players:[],clubs:[],open:true},me=null;
                var lbl={'':'offen','present':'angemeldet ✓','withdraw-requested':'Abmeldung gemeldet – wartet auf Bestätigung','withdrawn':'endgültig abgemeldet'};
                function esc(s){return (s||'').replace(/[<>&"]/g,c=>({'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c]));}
                function norm(s){return (s||'').toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'');}
                function load(){return fetch(base+'/daten.json').then(r=>r.json()).then(d=>{data=d;
                 document.getElementById('closed').style.display=d.open?'none':'block';
                 var sel=document.getElementById('club');if(sel.options.length<=1)d.clubs.forEach(c=>{var o=document.createElement('option');o.value=c;o.textContent=c;sel.appendChild(o);});
                 hits();club();if(me)choose(me);});}
                function pickRole(r){document.getElementById('role').style.display='none';document.getElementById(r).style.display='block';}
                function hits(){var w=norm(document.getElementById('q').value).split(/\\s+/).filter(x=>x);
                 var h=w.length?data.players.filter(p=>w.every(x=>norm(p.n+' '+p.c).includes(x))).slice(0,30):[];
                 document.getElementById('hits').innerHTML=h.map(p=>'<a class="card" href="#" data-id="'+esc(p.id)+'"><div class="row"><b>'+esc(p.n)+'</b><span class="muted">'+lbl[p.s]+'</span></div><div class="muted">'+esc(p.c)+'</div></a>').join('');}
                document.getElementById('q').addEventListener('input',hits);
                document.getElementById('hits').addEventListener('click',function(e){var a=e.target.closest('a');if(!a)return;e.preventDefault();choose(a.dataset.id);});
                function choose(id){me=id;var p=data.players.find(x=>x.id===id);if(!p)return;var c=document.getElementById('chosen');c.style.display='block';
                 document.getElementById('hits').innerHTML='';document.getElementById('q').value='';
                 c.innerHTML='<b>'+esc(p.n)+'</b><div class="muted">'+esc(p.c)+' · '+lbl[p.s]+'</div>'+(p.s==='withdrawn'?'<p class="warn">Bereits endgültig abgemeldet – bitte an die Turnierleitung wenden.</p>':
                 '<label class="pl"><input type="checkbox" id="here" '+(p.s==='present'?'checked':'')+'> angemeldet (ich bin heute da)</label><button onclick="sendMe()">Abschicken</button>');}
                function sendMe(){send([me],document.getElementById('here').checked);}
                function club(){var c=document.getElementById('club').value,l=document.getElementById('clubList');
                 if(!c){l.innerHTML='';document.getElementById('coachBtns').style.display='none';return;}
                 var ps=data.players.filter(p=>p.c===c);
                 l.innerHTML='<div class="card">'+ps.map(p=>'<label class="pl"><input type="checkbox" value="'+esc(p.id)+'" '+(p.s==='withdrawn'?'disabled':'')+'><span><b>'+esc(p.n)+'</b><div class="muted">'+lbl[p.s]+'</div></span></label>').join('')+'</div>';
                 document.getElementById('coachBtns').style.display=ps.length?'block':'none';}
                document.getElementById('club').addEventListener('change',club);
                function sendSel(present){var ids=[].slice.call(document.querySelectorAll('#clubList input:checked')).map(x=>x.value);
                 if(!ids.length){alertMsg('Bitte zuerst Personen auswählen.',false);return;}send(ids,present);}
                function alertMsg(t,ok){var m=document.getElementById('msg');m.style.display='block';m.className='card '+(ok?'ok':'warn');m.textContent=t;scrollTo(0,0);}
                function send(ids,present){fetch(base+'/melden',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},
                 body:'ids='+encodeURIComponent(ids.join(','))+'&anwesend='+(present?'1':'0')}).then(r=>r.json()).then(d=>{alertMsg(d.msg,d.ok);load();})
                 .catch(()=>alertMsg('Keine Verbindung zum Turnier-Laptop.',false));}
                load();
                </script>
                """;
        return page("Anmeldung am Turniertag", body, false);
    }
}
