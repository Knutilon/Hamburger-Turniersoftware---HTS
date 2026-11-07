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
            if (!"GET".equals(method)) {
                send(ex, 405, "text/plain", "Nur lesend");
                return;
            }
            String html = switch (path) {
                case "/live" -> call(this::overview);
                case "/live/tische" -> call(this::tablesPage);
                case "/live/klasse" -> call(() -> classPage(q.get("k")));
                case "/live/ergebnis" -> call(() -> resultPage(q.get("k")));
                case "/live/suche" -> call(this::searchPage);
                case "/live/ich" -> call(() -> mePage(q.get("id")));
                case "/live/spieler.json" -> null;
                default -> "";
            };
            if (path.equals("/live/spieler.json")) {
                send(ex, 200, "application/json", call(this::playersJson));
            } else if (html == null || html.isEmpty()) {
                send(ex, 404, "text/html", page("Nicht gefunden", "<p>Diese Seite gibt es nicht. <a href=\"/live\">Zur Übersicht</a></p>", false));
            } else {
                send(ex, 200, "text/html", html);
            }
        } catch (Exception e) {
            try {
                send(ex, 500, "text/plain", "Fehler: " + e.getMessage());
            } catch (IOException ignored) {
                // Verbindung schon zu
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

    private static final String CSS = """
            body{font-family:-apple-system,"Segoe UI",Roboto,Arial,sans-serif;margin:0;background:#f4f5f7;color:#222;font-size:16px}
            header{background:#173a6c;color:#fff;padding:12px 14px}header a{color:#fff;text-decoration:none}
            header h1{font-size:18px;margin:0}header .sub{font-size:13px;opacity:.85;margin-top:2px}
            main{padding:10px 12px 40px;max-width:900px;margin:0 auto}
            h2{font-size:17px;color:#173a6c;margin:18px 0 8px}h3{font-size:15px;color:#173a6c;margin:14px 0 6px}
            .card{background:#fff;border-radius:10px;padding:10px 12px;margin:8px 0;box-shadow:0 1px 2px rgba(0,0,0,.06)}
            a.card{display:block;color:inherit;text-decoration:none}a.card:active{background:#e8f6f6}
            .row{display:flex;justify-content:space-between;gap:8px;align-items:baseline}
            .muted{color:#6b7280;font-size:13px}.chip{display:inline-block;font-size:12px;padding:2px 8px;border-radius:10px;background:#e5e7eb}
            .run{background:#dff5f6;color:#0b5b5e}.done{background:#173a6c;color:#fff}.queue{background:#fff4d6;color:#7a5200}
            .me{border-left:4px solid #6fd1d4}.win{font-weight:600}
            table{border-collapse:collapse;width:100%;font-size:14px}th,td{padding:5px 4px;text-align:left;border-bottom:1px solid #eee}
            th{color:#173a6c;font-size:12px}td.n,th.n{text-align:right}
            input[type=search],select{width:100%;box-sizing:border-box;font-size:16px;padding:10px;border:1px solid #cfd4da;border-radius:8px}
            button{font-size:16px;padding:12px 14px;border-radius:22px;border:0;background:#173a6c;color:#fff;margin:6px 6px 0 0}
            button.light{background:#e5e7eb;color:#173a6c}.ok{color:#0f6e56}.warn{color:#9a3412}
            label.pl{display:flex;gap:10px;align-items:center;padding:8px 0;border-bottom:1px solid #eee}
            label.pl input{width:22px;height:22px}
            """;

    private static final String RELOAD = """
            <script>(function(){var k='y'+location.pathname+location.search;
            addEventListener('load',function(){var y=sessionStorage.getItem(k);if(y)scrollTo(0,+y);});
            setTimeout(function(){sessionStorage.setItem(k,scrollY);location.reload();},10000);})();</script>
            """;

    private String page(String title, String body, boolean reload) {
        String name = t.getName() == null || t.getName().isBlank() ? "Turnier" : t.getName();
        return "<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + esc(title) + " – " + esc(name)
                + "</title><style>" + CSS + "</style></head><body><header><a href=\"/live\"><h1>" + esc(name) + "</h1></a><div class=\"sub\">"
                + esc(title) + "</div></header><main>" + body + "</main>" + (reload ? RELOAD : "") + "</body></html>";
    }

    // --- Live-Anzeige ----------------------------------------------------------------------------

    private String overview() {
        StringBuilder b = new StringBuilder();
        b.append("<div id=\"me\"></div>");
        b.append("<a class=\"card\" href=\"/live/tische\"><b>Tische und laufende Spiele</b><div class=\"muted\">")
                .append(t.getMatches().stream().filter(m -> m.getStatus().atTable()).count()).append(" Spiele an den Tischen · ")
                .append(t.queue().size()).append(" eingereiht</div></a>");
        b.append("<a class=\"card\" href=\"/live/suche\"><b>Meine Spiele / Meine Gruppe</b><div class=\"muted\">Eigenen Namen suchen</div></a>");
        b.append("<h2>Klassen</h2>");
        for (PlayingClass c : t.rootClasses()) {
            String state = c.isFinished() ? "<span class=\"chip done\">beendet</span>" : c.isStarted() ? "<span class=\"chip run\">läuft</span>"
                    : c.isDrawn() ? "<span class=\"chip\">ausgelost</span>" : "<span class=\"chip\">noch nicht ausgelost</span>";
            b.append("<a class=\"card\" href=\"/live/klasse?k=").append(enc(c.getName())).append("\"><div class=\"row\"><b>")
                    .append(esc(c.getName())).append("</b>").append(state).append("</div></a>");
        }
        b.append("""
                <script>try{var me=JSON.parse(localStorage.getItem('hts-me')||'null');if(me&&me.id){
                document.getElementById('me').innerHTML='<a class="card me" href="/live/ich?id='+encodeURIComponent(me.id)+'"><b>Meine Spiele: '
                +me.name.replace(/[<>&]/g,'')+'</b><div class="muted">Spiele, Tisch und Gruppe</div></a>';}}catch(e){}</script>
                """);
        return page("Live-Anzeige", b.toString(), true);
    }

    private String matchLine(Match m, String highlightPerson) {
        String a = t.entrantName(m, m.getA()), bb = t.entrantName(m, m.getB());
        String w = m.winner();
        String na = a == null ? "–" : esc(a), nb = bb == null ? "–" : esc(bb);
        if (w != null && w.equals(m.getA())) na = "<span class=\"win\">" + na + "</span>";
        if (w != null && w.equals(m.getB())) nb = "<span class=\"win\">" + nb + "</span>";
        String chip = switch (m.getStatus()) {
            case ACTIVE, PAUSED -> "<span class=\"chip run\">" + esc(m.getTable() != null ? t.tableName(m.getTable()) : m.getStatus().getLabel()) + "</span>";
            case QUEUED -> "<span class=\"chip queue\">eingereiht</span>";
            case DONE -> "<span class=\"chip done\">" + esc(m.getResultText()) + "</span>";
            default -> "<span class=\"chip\">geplant</span>";
        };
        boolean mine = highlightPerson != null && t.personsOf(m).contains(highlightPerson);
        return "<div class=\"card" + (mine ? " me" : "") + "\"><div class=\"row\"><span>" + na + " – " + nb + "</span>" + chip + "</div>"
                + "<div class=\"muted\">Spiel " + m.getNumber() + " · " + esc(className(m)) + " · " + esc(m.getStage())
                + (m.getStatus() == MatchStatus.DONE && !m.getSetsText().isEmpty() ? " · " + esc(m.getSetsText()) : "") + "</div></div>";
    }

    private String className(Match m) {
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        if (c == null) return m.getClassName();
        PlayingClass root = t.rootOf(c);
        return root.getName() + (c.isPhase() ? " · Phase " + t.phaseNumber(c) : "");
    }

    private String tablesPage() {
        StringBuilder b = new StringBuilder("<h2>An den Tischen</h2>");
        boolean any = false;
        for (int nr = 1; nr <= t.getTableCount(); nr++) {
            if (t.isTableDisabled(nr)) continue;
            Match m = t.activeMatchAt(nr).orElse(null);
            if (m == null) {
                b.append("<div class=\"card\"><div class=\"row\"><b>").append(esc(t.tableName(nr))).append("</b><span class=\"muted\">frei</span></div></div>");
            } else {
                any = true;
                b.append(matchLine(m, null));
            }
        }
        if (!any && t.getTableCount() == 0) b.append("<p class=\"muted\">Keine Tische.</p>");
        List<Match> queue = t.queue();
        b.append("<h2>Als Nächstes</h2>");
        if (queue.isEmpty()) b.append("<p class=\"muted\">Keine eingereihten Spiele.</p>");
        for (Match m : queue.subList(0, Math.min(20, queue.size()))) b.append(matchLine(m, null));
        return page("Tische und laufende Spiele", b.toString(), true);
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

    private String classPage(String name) {
        PlayingClass root = name == null ? null : t.findClass(name).map(t::rootOf).orElse(null);
        if (root == null) return "";
        StringBuilder b = new StringBuilder();
        if (!root.isDrawn()) b.append("<p class=\"muted\">Noch nicht ausgelost.</p>");
        for (PlayingClass phase : t.phasesOf(root)) {
            if (!phase.isDrawn()) continue;
            b.append("<h2>Phase ").append(t.phaseNumber(phase)).append(" – ").append(esc(modeName(phase))).append("</h2>");
            phaseBody(b, phase, null);
        }
        b.append("<a class=\"card\" href=\"/live/ergebnis?k=").append(enc(root.getName())).append("\"><b>")
                .append(root.isFinished() ? "Endergebnis" : "Zwischenstand").append("</b></a>");
        return page(root.getName(), b.toString(), true);
    }

    private static String modeName(PlayingClass c) {
        return switch (c.getMode()) {
            case GROUPS -> "Gruppenphase";
            case KNOCKOUT -> "KO-System";
            case SWISS -> "Schweizer System";
            case PLACEMENT -> "Platzierungsspiele";
            case QUALIFICATION -> "Qualifikation";
            default -> "";
        };
    }

    private void phaseBody(StringBuilder b, PlayingClass phase, String person) {
        List<Match> ms = new ArrayList<>(t.matchesOf(phase));
        ms.sort(Comparator.comparingInt(Match::getNumber));
        if (phase.getMode() == TournamentMode.GROUPS || phase.getMode() == TournamentMode.PLACEMENT || phase.getMode() == TournamentMode.SWISS) {
            for (int g = 0; g < phase.getGroups().size(); g++) {
                final int gi = g;
                b.append("<h3>").append(phase.getMode() == TournamentMode.SWISS ? "Tabelle" : "Gruppe " + GroupDraw.groupName(g)).append("</h3>");
                b.append("<div class=\"card\">").append(standingsTable(phase, g, person)).append("</div>");
                for (Match m : ms) if (m.getGroupIndex() == gi || (phase.getMode() == TournamentMode.SWISS)) b.append(matchLine(m, person));
                if (phase.getMode() == TournamentMode.SWISS) break;
            }
            if (phase.getMode() == TournamentMode.PLACEMENT) {
                for (Match m : ms) if (m.getGroupIndex() < 0) b.append(matchLine(m, person));
            }
        } else {
            Map<String, List<Match>> byStage = new LinkedHashMap<>();
            ms.sort(Comparator.comparingInt((Match m) -> m.getKoRound() >= 0 ? m.getKoRound() : m.getRound()).thenComparingInt(Match::getNumber));
            for (Match m : ms) byStage.computeIfAbsent(m.getStage(), k -> new ArrayList<>()).add(m);
            byStage.forEach((stage, list) -> {
                b.append("<h3>").append(esc(stage)).append("</h3>");
                for (Match m : list) if (m.getA() != null || m.getB() != null) b.append(matchLine(m, person));
            });
        }
    }

    private String resultPage(String name) {
        PlayingClass root = name == null ? null : t.findClass(name).map(t::rootOf).orElse(null);
        if (root == null) return "";
        Tournament.FinalResult r = t.finalResult(root);
        StringBuilder b = new StringBuilder("<div class=\"card\"><table><tr><th>Platz</th><th>Name</th><th>Ermittelt durch</th></tr>");
        for (Tournament.Placing p : r.rows()) {
            b.append("<tr><td>").append(esc(p.placeText())).append("</td><td>").append(esc(p.entrant().name()))
                    .append(p.entrant().detail() != null ? "<div class=\"muted\">" + esc(p.entrant().detail()) + "</div>" : "")
                    .append("</td><td class=\"muted\">").append(esc(p.info())).append("</td></tr>");
        }
        b.append("</table></div>");
        if (r.rows().isEmpty()) b = new StringBuilder("<p class=\"muted\">Noch keine Platzierungen.</p>");
        return page((r.complete() ? "Endergebnis " : "Zwischenstand ") + root.getName(), b.toString(), true);
    }

    private String playersJson() {
        StringBuilder b = new StringBuilder("[");
        List<Player> list = sortedPlayers();
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            if (i > 0) b.append(',');
            b.append("{\"id\":").append(json(p.getId())).append(",\"n\":").append(json(p.getDisplayName()))
                    .append(",\"c\":").append(json(p.getClubName())).append(",\"s\":").append(json(p.getAttendance().code())).append('}');
        }
        return b.append(']').toString();
    }

    private List<Player> sortedPlayers() {
        Collator col = Collator.getInstance(Locale.GERMAN);
        List<Player> list = new ArrayList<>(t.getPlayers());
        list.sort(Comparator.comparing((Player p) -> p.getDisplayName() == null ? "" : p.getDisplayName(), col));
        return list;
    }

    private String searchPage() {
        String body = """
                <p class="muted">Eigenen Namen suchen (Vorname, Nachname oder Verein) und antippen – das Handy merkt sich die Auswahl.</p>
                <input type="search" id="q" placeholder="z. B. Müller Eimsbüttel" autofocus>
                <div id="list"></div>
                <script>
                var all=[];fetch('/live/spieler.json').then(r=>r.json()).then(d=>{all=d;show();});
                function norm(s){return (s||'').toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'');}
                function esc(s){return (s||'').replace(/[<>&"]/g,c=>({'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c]));}
                function show(){var w=norm(document.getElementById('q').value).split(/\\s+/).filter(x=>x);
                 var hits=all.filter(p=>w.every(x=>norm(p.n+' '+p.c).includes(x))).slice(0,40);
                 document.getElementById('list').innerHTML=w.length?hits.map(p=>'<a class="card" href="#" data-id="'+esc(p.id)+'" data-n="'+esc(p.n)+'"><b>'+esc(p.n)+'</b><div class="muted">'+esc(p.c)+'</div></a>').join(''):'';}
                document.getElementById('q').addEventListener('input',show);
                document.getElementById('list').addEventListener('click',function(e){var a=e.target.closest('a');if(!a)return;e.preventDefault();
                 localStorage.setItem('hts-me',JSON.stringify({id:a.dataset.id,name:a.dataset.n}));location.href='/live/ich?id='+encodeURIComponent(a.dataset.id);});
                </script>
                """;
        return page("Meine Spiele – Namen suchen", body, false);
    }

    private String mePage(String id) {
        Player p = id == null ? null : t.getPlayers().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (p == null) return page("Meine Spiele", "<p>Person nicht gefunden. <a href=\"/live/suche\">Neu suchen</a></p>", false);
        List<Match> mine = new ArrayList<>();
        for (Match m : t.getMatches()) if (t.personsOf(m).contains(id)) mine.add(m);
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"card me\"><b>").append(esc(p.getDisplayName())).append("</b><div class=\"muted\">").append(esc(p.getClubName()))
                .append(" · <a href=\"/live/suche\">andere Person</a></div></div>");
        List<Match> now = mine.stream().filter(m -> m.getStatus().atTable()).toList();
        List<Match> next = mine.stream().filter(m -> m.getStatus() == MatchStatus.QUEUED || m.getStatus() == MatchStatus.PLANNED)
                .sorted(Comparator.comparingInt((Match m) -> m.getStatus() == MatchStatus.QUEUED ? 0 : 1).thenComparingInt(Match::getNumber)).toList();
        List<Match> done = mine.stream().filter(m -> m.getStatus() == MatchStatus.DONE).sorted(Comparator.comparingInt(Match::getNumber).reversed()).toList();
        if (!now.isEmpty()) {
            b.append("<h2>Jetzt am Tisch</h2>");
            for (Match m : now) b.append(matchLine(m, id));
        }
        b.append("<h2>Als Nächstes</h2>");
        if (next.isEmpty()) b.append("<p class=\"muted\">Keine offenen Spiele.</p>");
        for (Match m : next) b.append(matchLine(m, id));
        // Meine Gruppe(n): Gruppen mit einem Spiel dieser Person
        Set<String> shown = new TreeSet<>();
        for (Match m : mine) {
            PlayingClass c = t.findClass(m.getClassName()).orElse(null);
            if (c == null || m.getGroupIndex() < 0 || !(c.getMode() == TournamentMode.GROUPS || c.getMode() == TournamentMode.PLACEMENT)) continue;
            if (!shown.add(c.getName() + "#" + m.getGroupIndex())) continue;
            b.append("<h2>Meine Gruppe – ").append(esc(className(m))).append(", Gruppe ").append(GroupDraw.groupName(m.getGroupIndex())).append("</h2>");
            b.append("<div class=\"card\">").append(standingsTable(c, m.getGroupIndex(), id)).append("</div>");
        }
        if (!done.isEmpty()) {
            b.append("<h2>Gespielt</h2>");
            for (Match m : done) b.append(matchLine(m, id));
        }
        b.append("<script>try{localStorage.setItem('hts-me',JSON.stringify({id:").append(json(p.getId())).append(",name:")
                .append(json(p.getDisplayName())).append("}));}catch(e){}</script>");
        return page("Meine Spiele", b.toString(), true);
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
