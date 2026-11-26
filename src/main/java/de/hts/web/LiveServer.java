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
import de.hts.player.Attendance;
import de.hts.player.Player;
import de.hts.tournament.Entrant;
import de.hts.tournament.GroupStanding;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import de.hts.tournament.TournamentMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Aufbau der Live-Seiten (seit 0.35; seit 1.9.0 nur noch für den Live-Server im Internet, {@link LiveHost}): Tische,
 * Klassen mit Reitern je Phase, Gruppen, KO-Baum, Ergebnisse, Spielseiten, Suche, „Meine Spiele“ und die
 * Online-Anmeldung am Turniertag ({@code /anmeldung/<Schlüssel>}, 1.9.0 #164). Der frühere Handy-Zugang über das
 * Hallen-WLAN (eigener Server auf Port 8080, Anmeldung und Ergebnisse per Handy) ist mit 1.9.0 (#163) entfallen.
 * <p>
 * Alle Zugriffe auf das Turnier laufen über {@code lock}, damit nichts gleichzeitig geändert wird.
 */
public final class LiveServer {

    private final Tournament t;
    private final Executor ui;

    private String base = "/live";
    private java.util.function.Supplier<byte[]> logoSource = () -> null;
    private java.util.function.Supplier<String> orgName = () -> "";
    private String extraHead = "";
    private String extraFooter = "";
    private boolean finalOnly;
    private java.util.function.LongSupplier stampSource = System::currentTimeMillis;
    /** 1.9.0 (#165): Ziel des Knopfs „← Alle Turniere“ ({@code null} = kein Knopf) */
    private java.util.function.Supplier<String> homeHref = () -> null;

    /**
     * 1.10.0 (#171): fertige Seiten bis zum nächsten Upload zwischenspeichern (nur Live-Server – dort ändert sich das
     * Turnier einer Ansicht nie; ein neuer Stand bringt eine neue Ansicht). Höchstens 600 Seiten, je höchstens 60 s alt.
     */
    private boolean cacheable;
    private final Map<String, Object[]> cache = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object[]> eldest) {
            return size() > 600;
        }
    };
    static final long CACHE_MS = 60_000;

    private LiveServer(Tournament t, Executor ui) {
        this.t = t;
        this.ui = ui;
    }

    private String cached(String key, Callable<String> c) throws Exception {
        if (!cacheable) return call(c);
        long now = System.currentTimeMillis();
        synchronized (cache) {
            Object[] hit = cache.get(key);
            if (hit != null && now - (Long) hit[1] < CACHE_MS) return (String) hit[0];
        }
        String html = call(c);
        if (html != null && !html.isEmpty()) {
            synchronized (cache) {
                cache.put(key, new Object[]{html, now});
            }
        }
        return html;
    }

    int cachedPages() {
        synchronized (cache) {
            return cache.size();
        }
    }

    /** Für Tests: Seiten eines Turniers unter „/live“ ohne Live-Server. */
    static LiveServer forTest(Tournament t) {
        return new LiveServer(t, Runnable::run);
    }

    static LiveServer forHost(Tournament t, Executor lock, String base, java.util.function.Supplier<byte[]> logo,
                              String extraHead, String extraFooter, boolean finalOnly, java.util.function.LongSupplier stamp) {
        return forHost(t, lock, base, logo, () -> "", extraHead, extraFooter, finalOnly, stamp);
    }

    /** 1.6.0 (#101) / 1.8.0 (#150): Ansicht für den Live-Server mit Namen des Ausrichters. */
    static LiveServer forHost(Tournament t, Executor lock, String base, java.util.function.Supplier<byte[]> logo,
                              java.util.function.Supplier<String> org, String extraHead, String extraFooter, boolean finalOnly,
                              java.util.function.LongSupplier stamp) {
        LiveServer v = new LiveServer(t, lock);
        v.stampSource = stamp;
        v.base = base;
        v.logoSource = logo;
        v.orgName = org == null ? () -> "" : org;
        v.extraHead = extraHead == null ? "" : extraHead;
        v.extraFooter = extraFooter == null ? "" : extraFooter;
        v.finalOnly = finalOnly;
        v.cacheable = true;
        return v;
    }

    /** 1.9.0 (#165): Knopf „← Alle Turniere“ oben auf jeder Seite. */
    LiveServer home(java.util.function.Supplier<String> href) {
        homeHref = href == null ? () -> null : href;
        return this;
    }

    Tournament tournament() {
        return t;
    }

    /** Pfade „/live…“ in der fertigen Seite auf {@code base} umschreiben (Live-Server: „/live/<Turnier>…“). */
    String rebase(String html) {
        if (html == null || base.equals("/live")) return html;
        return html.replace("\"/live", "\"" + base).replace("'/live", "'" + base);
    }

    // --- Verteilung ------------------------------------------------------------------------------

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
                send(ex, 200, "text/html", cached(path, () -> matchPage(seg, ph == null ? 1 : Integer.parseInt(ph), nr)));
                return;
            }
            String html = switch (path) {
                case "/live" -> cached(path, this::overview);
                case "/live/tische" -> cached(path, this::tablesPage);
                case "/live/klasse" -> cached(path + "|" + q.get("k") + "|" + q.get("p"), () -> classPage(q.get("k"), q.get("p")));
                case "/live/ergebnis" -> cached(path + "|" + q.get("k"), () -> classPage(q.get("k"), "e"));
                case "/live/suche" -> cached(path, this::searchPage);
                case "/live/ich" -> cached(path + "|" + q.get("id"), () -> mePage(q.get("id")));
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
                send(ex, 200, "application/json", cached(path, this::playersJson));
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
            .homebtn{display:inline-flex;align-items:center;min-height:44px;padding:0 14px;margin:0 0 8px;border:1px solid #d0d5db;border-radius:999px;background:#fff;color:#1f2933;text-decoration:none;font-size:14px;font-weight:600}
            header .homebtn{margin:0 auto 8px;max-width:1000px}header .hrow{max-width:1000px;margin:0 auto}
            .exempt{border-left:4px solid #5fd3a8}.exempt .row{padding:4px 0;border-bottom:1px solid #eef0f2}.exempt .row:last-child{border-bottom:0}
            .vt{display:inline-flex;border:1px solid #d0d5db;border-radius:999px;overflow:hidden;margin:0 0 10px}.vt a{padding:7px 16px;font-size:13px;color:#1f2933;text-decoration:none;cursor:pointer}.vt a.on{background:#187250;color:#fff}
            .ktw{overflow-x:auto;padding:0 0 12px;-webkit-overflow-scrolling:touch}.kt{display:flex;align-items:stretch;min-width:max-content}
            .kc{display:flex;flex-direction:column;min-width:max-content}.kc+.kc{margin-left:40px}.kc h3{margin:0 0 6px;white-space:nowrap}
            .kcm{display:flex;flex-direction:column;flex:1}.kp{flex:1;display:flex;flex-direction:column;position:relative}
            .kx{flex:1;display:flex;align-items:center;position:relative;padding:6px 0}
            .kl .kp.j::after{content:"";position:absolute;right:-20px;top:25%;bottom:25%;border-right:1.5px solid #b9c1ca}
            .kl .kx.o::after{content:"";position:absolute;right:-20px;top:50%;width:20px;border-top:1.5px solid #b9c1ca}
            .kl .kx.i::before{content:"";position:absolute;left:-20px;top:50%;width:20px;border-top:1.5px solid #b9c1ca}
            .k3{display:flex;align-items:center;gap:14px;flex-wrap:wrap;margin-top:6px}.k3 h3{margin:0;white-space:nowrap}
            .km{display:block;flex:1;box-sizing:border-box;background:#fff;border:1px solid #e4e6ea;border-radius:10px;padding:4px 4px 5px;color:inherit;text-decoration:none;white-space:nowrap}
            .km .s{display:flex;justify-content:space-between;align-items:center;gap:16px;font-size:13px;padding:4px 6px;border-radius:6px}
            .km .s.w{background:#e4f7ef}.km .s small{color:#6b7280;font-size:11px;margin-left:4px}
            .km .sc{display:inline-flex;align-items:center;gap:2px;font-variant-numeric:tabular-nums}
            .km .sc i{font-style:normal;width:20px;text-align:center;font-size:12px;color:#6b7280}.km .sc i.g{font-weight:700;color:#1f2933}
            .km .sc b{width:22px;text-align:center;font-size:15px;color:#4a5560;font-weight:600;margin-left:4px;padding-left:4px;border-left:1px solid #d9dee3}
            .km .s.w .sc b{color:#187250;font-weight:800}
            .km .meta{font-size:11px;color:#6b7280;display:flex;justify-content:space-between;align-items:center;gap:12px;padding:3px 6px 0}
            .km.run{border-color:#5fd3a8}.km.mine{background:#fff;border-color:#187250;box-shadow:0 0 0 1px #187250}
            """;

    /**
     * 1.7.0 (#147): Tabellen und Spiele aufklappen, Aktualisierung alle 10 s ohne Neuladen. 1.8.0 (#155): Zustand nur in
     * einer Variablen, solange die Seite offen ist – nichts im Browser gespeichert.
     */
    private static final String TILE_JS = """
            <script>(function(){var s={},view='tree';
            function ap(){document.querySelectorAll('.tile[data-key]').forEach(function(t){var v=s[t.dataset.key]||0;
            t.classList.toggle('open',v>=1);t.classList.toggle('games',v>=2);var b=t.querySelector('.gm');if(b)b.textContent=v>=2?'Spiele ausblenden':b.dataset.l;});
            document.querySelectorAll('.vt a').forEach(function(a){a.classList.toggle('on',a.dataset.v===view);});
            document.querySelectorAll('.ktw').forEach(function(x){x.style.display=view==='tree'?'':'none';});
            document.querySelectorAll('.klst').forEach(function(x){x.style.display=view==='tree'?'none':'';});}
            document.addEventListener('click',function(e){var a=e.target.closest('.vt a');if(a){e.preventDefault();view=a.dataset.v;ap();return;}
            var t=e.target.closest('.tile[data-key]');if(!t)return;var v=s[t.dataset.key]||0;
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
            if(n&&m&&n.innerHTML!==m.innerHTML){var sl=[].map.call(document.querySelectorAll('.ktw'),function(x){return x.scrollLeft;});
            m.innerHTML=n.innerHTML;if(window.htsApply)htsApply();[].forEach.call(document.querySelectorAll('.ktw'),function(x,i){if(sl[i])x.scrollLeft=sl[i];});}
            var a=d.getElementById('stamp'),b=document.getElementById('stamp');if(a&&b)b.textContent=a.textContent;}).catch(function(){});},10000);</script>
            """;

    private static final String SEARCH_JS = """
            <script>(function(){var q=document.getElementById('q');if(!q)return;var all=null,sr=document.getElementById('sr'),mn=document.getElementById('live');
            function nm(s){return(s||'').toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'');}
            function es(s){return(s||'').replace(/[<>&"]/g,function(c){return{'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c];});}
            function cd(p){return '<div class="card"><b>'+es(p.n)+'</b><div class="muted">'+es(p.c)+'</div><div class="kl">'+(p.k||[]).map(function(x){return '<span class="chip">'+es(x)+'</span>';}).join('')
            +(p.x||[]).map(function(x){return '<span class="chip run">freigestellt: '+es(x)+'</span>';}).join('')
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

    /** Seiten ohne Suche und Klassenleiste (Online-Anmeldung). */
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
                + "</title>" + extraHead + "<style>" + CSS + "</style></head><body><header>"
                + (homeHref.get() == null ? "" : "<div class=\"hrow\"><a class=\"homebtn\" href=\"" + esc(homeHref.get()) + "\">← Alle Turniere</a></div>")
                + "<div class=\"hb\"><a class=\"brand\" href=\"/live\">"
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
            if (mode == TournamentMode.GROUPS) b.append(exemptCard(phase, person));            // 1.9.0 (#161)
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
            if (byStage.isEmpty()) {
                b.append("<p class=\"muted\">Noch keine Spiele.</p>");
                return;
            }
            String tree = koTree(ms, person);                       // 1.9.0 (#162): KO-Baum, Liste umschaltbar
            if (tree != null) {
                b.append("<div class=\"vt\"><a data-v=\"tree\" class=\"on\">Baum</a><a data-v=\"list\">Liste</a></div>").append(tree)
                        .append("<div class=\"klst\" style=\"display:none\">");
            }
            byStage.forEach((stage, list) -> {
                b.append("<h3>").append(esc(stage)).append("</h3><div class=\"card\">");
                for (Match m : list) b.append(matchLine(m, person));
                b.append("</div>");
            });
            if (tree != null) b.append("</div>");
        }
    }

    /**
     * 1.9.0 (#162): KO-Baum – je Runde eine Spalte (nach Position im Feld), das Spiel um Platz 3 unter dem Finale.
     * {@code null}, wenn die Phase kein reines KO-Feld ist (dann nur die Liste).
     */
    /**
     * 1.9.0 (#162), 1.10.0 (#174): KO-Baum. Jede Runde gleich hoch, jedes Spiel in einer gleich hohen Zelle – so steht ein
     * Folgespiel genau mittig zwischen seinen beiden Vorgängern. Linien als Klammer (Strich aus jeder Karte, senkrechte
     * Verbindung des Paars, Strich zum Folgespiel), wenn sich die Spiele je Runde halbieren; Spiel um Platz 3 darunter.
     */
    private String koTree(List<Match> ms, String person) {
        java.util.TreeMap<Integer, List<Match>> rounds = new java.util.TreeMap<>();
        Match third = null;
        for (Match m : ms) {
            if (m.getKoRound() >= 0) rounds.computeIfAbsent(m.getKoRound(), k -> new ArrayList<>()).add(m);
            else if (m.getPlacement() == 3 && Tournament.THIRD_PLACE_STAGE.equals(m.getStage())) third = m;
            else return null;
        }
        if (rounds.size() < 2 && third == null) return null;
        List<List<Match>> rs = new ArrayList<>();
        for (List<Match> l : rounds.values()) {
            List<Match> list = new ArrayList<>(l);
            list.sort(Comparator.comparingInt(Match::getKoIndex));
            rs.add(list);
        }
        boolean lines = true;                                  // Linien nur bei einem regelmäßigen Baum
        for (int r = 0; r + 1 < rs.size(); r++) if (rs.get(r).size() != 2 * rs.get(r + 1).size()) lines = false;
        StringBuilder b = new StringBuilder("<div class=\"ktw\"><div class=\"kt").append(lines ? " kl" : "").append("\">");
        for (int r = 0; r < rs.size(); r++) {
            List<Match> list = rs.get(r);
            boolean last = r == rs.size() - 1;
            String cls = "kx" + (last ? "" : " o") + (r == 0 ? "" : " i");
            b.append("<div class=\"kc\"><h3>").append(esc(list.get(0).getStage())).append("</h3><div class=\"kcm\">");
            if (last || !lines) {
                for (Match m : list) b.append("<div class=\"kp\"><div class=\"").append(cls).append("\">").append(koCard(m, person)).append("</div></div>");
            } else {
                for (int k = 0; k < list.size(); k += 2) {
                    b.append("<div class=\"kp j\">");
                    for (int j = k; j < Math.min(k + 2, list.size()); j++) {
                        b.append("<div class=\"").append(cls).append("\">").append(koCard(list.get(j), person)).append("</div>");
                    }
                    b.append("</div>");
                }
            }
            b.append("</div></div>");
        }
        b.append("</div>");
        if (third != null) {
            b.append("<div class=\"k3\"><h3>").append(esc(third.getStage())).append("</h3><div style=\"display:flex\">")
                    .append(koCard(third, person)).append("</div></div>");
        }
        return b.append("</div>").toString();
    }

    /**
     * 1.10.0 (#174): KO-Karte – Zeile des Siegers hellgrün, jeder Satz als eigene Spalte (gewonnene Sätze fett), dahinter
     * der Satzstand (Sieger grün). Laufende Spiele ohne Zwischenstand.
     */
    private String koCard(Match m, String person) {
        String w = m.winner();
        boolean done = m.getStatus() == MatchStatus.DONE && w != null;
        boolean walkover = done && m.getSetsText() != null && m.getSetsText().startsWith("kampflos");
        String status = switch (m.getStatus()) {
            case ACTIVE, PAUSED -> "<span class=\"chip run\">laufend" + (m.getTable() == null ? "" : " · " + esc(t.tableName(m.getTable()))) + "</span>";
            case QUEUED -> "<span class=\"chip queue\">eingereiht</span>";
            case CANCELLED -> "<span class=\"chip\">nicht gespielt</span>";
            case DONE -> "";
            default -> "<span class=\"chip\">offen</span>";
        };
        boolean mine = person != null && t.personsOf(m).contains(person);
        StringBuilder b = new StringBuilder("<a class=\"km").append(m.getStatus().atTable() ? " run" : "").append(mine ? " mine" : "")
                .append("\" href=\"").append(matchHref(m)).append("\">");
        String[] ids = {m.getA(), m.getB()};
        for (int i = 0; i < 2; i++) {
            String id = ids[i];
            String name = id == null ? null : t.entrantName(m, id);
            String origin = id == null ? null : t.originLabel(m, id);
            String detail = id == null ? null : entrantDetail(m, id);
            boolean win = done && id != null && id.equals(w);
            b.append("<div class=\"s").append(win ? " w" : "").append("\"><span>")
                    .append(origin == null ? "" : "<span class=\"muted\">" + esc(origin) + "</span> ")
                    .append(name == null ? "<span class=\"muted\">" + (m.getKoRound() == 0 ? "Freilos" : "offen") + "</span>"
                            : win ? "<b>" + esc(name) + "</b>" : esc(name))
                    .append(detail == null || detail.isBlank() ? "" : "<small>" + esc(detail) + "</small>").append("</span>");
            if (done) {
                b.append("<span class=\"sc\">");
                if (!walkover && m.getPoints() != null) {
                    for (int[] p : m.getPoints()) b.append("<i").append(p[i] > p[1 - i] ? " class=\"g\"" : "").append(">").append(p[i]).append("</i>");
                }
                Integer sets = i == 0 ? m.getSetsA() : m.getSetsB();
                b.append("<b>").append(sets == null ? "" : sets).append("</b></span>");
            }
            b.append("</div>");
        }
        String note = walkover ? " · kampflos" : done && m.getRetired() != 0 ? " · Aufgabe" : "";
        b.append("<div class=\"meta\"><span>Spiel ").append(m.getNumber()).append(note).append("</span>").append(status).append("</div></a>");
        return b.toString();
    }

    /** Verein (Einzel) bzw. Angabe der Paarung. */
    private String entrantDetail(Match m, String id) {
        PlayingClass c = t.findClass(m.getClassName()).orElse(null);
        if (c != null) {
            String d = t.entrantsOf(c).stream().filter(e -> e.id().equals(id)).map(Entrant::detail).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            if (d != null) return d;
        }
        return t.getPlayers().stream().filter(p -> p.getId().equals(id)).map(Player::getClubName).findFirst().orElse(null);
    }

    /** 1.9.0 (#161): Karte „Freigestellt“ über den Gruppen (S1 … S4, direkt in der KO-Runde). */
    private String exemptCard(PlayingClass phase, String person) {
        List<String> ex = phase.getExempt();
        if (ex.isEmpty()) return "";
        StringBuilder b = new StringBuilder("<div class=\"card exempt\"><div class=\"row\"><b>Freigestellt</b><span class=\"muted\">direkt in der KO-Runde</span></div>");
        int i = 0;
        for (String id : ex) {
            i++;
            Entrant e = t.entrantsOf(phase).stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
            String name = e != null ? e.name() : t.getPlayers().stream().filter(p -> p.getId().equals(id)).map(Player::getDisplayName).findFirst().orElse(id);
            String detail = e != null ? e.detail() : null;
            boolean mine = person != null && (id.equals(person) || (e != null && involves(e, person)));
            b.append("<div class=\"row").append(mine ? " win" : "").append("\"><span><span class=\"muted\">S").append(i).append("</span> ")
                    .append(esc(name)).append(detail == null || detail.isBlank() ? "" : " <span class=\"muted\">" + esc(detail) + "</span>")
                    .append("</span></div>");
        }
        return b.append("</div>").toString();
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
            b.append("],\"x\":[");
            List<String> xs = exemptClassesOf(p.getId());
            for (int j = 0; j < xs.size(); j++) b.append(j > 0 ? "," : "").append(json(xs.get(j)));
            b.append("]}");
        }
        return b.append(']').toString();
    }

    /** 1.9.0 (#161): Klassen, in denen die Person (bzw. ihre Paarung) in einer noch laufenden Gruppenphase freigestellt ist. */
    private List<String> exemptClassesOf(String personId) {
        List<String> out = new ArrayList<>();
        for (PlayingClass c : t.getClasses()) {
            if (c.getExempt().isEmpty() || c.isFinished()) continue;
            for (String id : c.getExempt()) {
                Entrant e = t.entrantsOf(c).stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
                if (id.equals(personId) || (e != null && involves(e, personId))) {
                    String n = t.rootOf(c).getName();
                    if (!out.contains(n)) out.add(n);
                }
            }
        }
        return out;
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
        for (String cls : exemptClassesOf(id)) {                          // 1.9.0 (#161)
            b.append("<div class=\"note\"><b>Freigestellt</b> in ").append(esc(cls)).append(" – erstes Spiel in der KO-Runde.</div>");
        }
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

    // --- 1.9.0 (#164): Online-Anmeldung am Turniertag ----------------------------------------------

    /** Nimmt eine Online-Meldung entgegen (Kennung, anwesend ja/nein) – der Live-Server merkt sie sich für HTS. */
    interface CheckinSink {
        void report(String id, boolean present);
    }

    /**
     * Anmeldeseite unter {@code …/anmeldung/<Schlüssel>}: {@code sub} = "" (Seite), "daten.json" oder "melden" (POST).
     * {@code open} = Online-Anmeldung jetzt möglich.
     */
    void serveCheckin(HttpExchange ex, String sub, String method, boolean open, CheckinSink sink) throws Exception {
        if (sub.isEmpty() && "GET".equals(method)) {
            send(ex, 200, "text/html", call(() -> checkinPage(open)));
        } else if (sub.equals("daten.json") && "GET".equals(method)) {
            send(ex, 200, "application/json", call(() -> checkinData(open)));
        } else if (sub.equals("melden") && "POST".equals(method)) {
            byte[] body;
            try (InputStream in = ex.getRequestBody()) {
                body = in.readNBytes(16 * 1024);
            }
            Map<String, String> form = parseQuery(new String(body, StandardCharsets.UTF_8));
            List<String> ids = form.getOrDefault("ids", "").isBlank() ? List.of() : List.of(form.get("ids").split(","));
            if (ids.size() > 200) ids = ids.subList(0, 200);
            boolean present = "1".equals(form.get("anwesend"));
            List<String> fids = ids;
            send(ex, 200, "application/json", call(() -> report(fids, present, open, sink)));
        } else {
            send(ex, 404, "text/plain", "Nicht gefunden");
        }
    }

    private String report(List<String> ids, boolean present, boolean open, CheckinSink sink) {
        if (!open) return "{\"ok\":false,\"msg\":" + json("Die Online-Anmeldung ist geschlossen – bitte an die Turnierleitung wenden.") + "}";
        int changed = 0, locked = 0, same = 0;
        for (String raw : ids) {
            String id = raw.trim();
            Player p = t.getPlayers().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
            if (p == null) continue;
            if (!t.canCheckinOnline(p)) {
                locked++;
                continue;
            }
            if (t.checkinOnline(p, present)) {
                changed++;
                sink.report(id, present);
            } else {
                same++;
            }
        }
        String msg = changed + (changed == 1 ? " Person" : " Personen") + (present ? " angemeldet." : " abgemeldet.");
        if (same > 0) msg += " " + same + (same == 1 ? " war" : " waren") + " bereits so gemeldet.";
        if (locked > 0) msg += " " + locked + " nicht mehr online änderbar (abgemeldet, Nachrücker oder Klasse läuft schon) – bitte an die Turnierleitung wenden.";
        return "{\"ok\":true,\"changed\":" + changed + ",\"msg\":" + json(msg) + "}";
    }

    private String checkinData(boolean open) {
        StringBuilder b = new StringBuilder("{\"open\":").append(open).append(",\"until\":")
                .append(json(t.getCheckinUntil() == null ? "" : t.getCheckinUntil().toString())).append(",\"players\":[");
        int i = 0;
        for (Player p : sortedPlayers()) {
            if (p.isReserveOnly()) continue;
            String st = !t.canCheckinOnline(p) ? (p.getAttendance() == Attendance.WITHDRAWN ? "withdrawn" : "locked")
                    : p.getAttendance() == Attendance.PRESENT ? "present" : "";
            b.append(i++ > 0 ? "," : "").append("{\"id\":").append(json(p.getId())).append(",\"n\":").append(json(p.getDisplayName()))
                    .append(",\"c\":").append(json(p.getClubName())).append(",\"s\":").append(json(st)).append('}');
        }
        b.append("],\"clubs\":[");
        Collator col = Collator.getInstance(Locale.GERMAN);
        Set<String> clubs = new TreeSet<>(col);
        for (Player p : t.getPlayers()) if (!p.isReserveOnly() && p.getClubName() != null && !p.getClubName().isBlank()) clubs.add(p.getClubName());
        i = 0;
        for (String c : clubs) b.append(i++ > 0 ? "," : "").append(json(c));
        return b.append("]}").toString();
    }

    private String checkinPage(boolean open) {
        String until = t.getCheckinUntil() == null ? "" : " bis " + t.getCheckinUntil() + " Uhr";
        String body = "<h2>Anmeldung am Turniertag</h2><p class=\"muted\">Online-Anmeldung" + esc(until)
                + ". Danach meldet nur noch die Turnierleitung an oder ab.</p>" + """
                <div id="closed" class="card warn" style="display:none">Die Online-Anmeldung ist geschlossen. Bitte an die Turnierleitung wenden.</div>
                <div id="role"><p>Wer meldet?</p>
                 <button type="button" data-role="self">Ich spiele selbst</button><button type="button" class="light" data-role="coach">Ich bin Betreuer/in</button></div>
                <div id="self" style="display:none"><h3>Spieler/in</h3>
                 <p class="muted">Eigenen Namen suchen (Vorname, Nachname oder Verein) und auswählen.</p>
                 <input type="search" id="cq" placeholder="z. B. Brandt Elbinsel" autocomplete="off"><div id="hits"></div>
                 <div id="chosen" class="card" style="display:none"></div></div>
                <div id="coach" style="display:none"><h3>Betreuer/in</h3>
                 <select id="club"><option value="">Verein wählen</option></select>
                 <div id="clubList"></div>
                 <div id="coachBtns" style="display:none"><button type="button" data-send="1">Ausgewählte anmelden</button>
                 <button type="button" class="light" data-send="0">Ausgewählte abmelden</button></div></div>
                <div id="msg" class="card ok" style="display:none"></div>
                <script>(function(){
                var base=location.pathname.replace(/\\/$/,''),data={players:[],clubs:[],open:true},me=null;
                var lbl={'':'nicht angemeldet','present':'angemeldet ✓','withdrawn':'endgültig abgemeldet','locked':'Klasse läuft schon'};
                function $(i){return document.getElementById(i);}
                function esc(s){return (s||'').replace(/[<>&"]/g,function(c){return{'<':'&lt;','>':'&gt;','&':'&amp;','"':'&quot;'}[c];});}
                function norm(s){return (s||'').toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g,'');}
                function load(){return fetch(base+'/daten.json',{cache:'no-store'}).then(function(r){return r.json();}).then(function(d){data=d;
                 $('closed').style.display=d.open?'none':'block';$('role').style.display=d.open&&!$('self').offsetParent&&!$('coach').offsetParent?'block':$('role').style.display;
                 if(!d.open){$('role').style.display='none';$('self').style.display='none';$('coach').style.display='none';}
                 var sel=$('club');if(sel.options.length<=1)d.clubs.forEach(function(c){var o=document.createElement('option');o.value=c;o.textContent=c;sel.appendChild(o);});
                 hits();club();if(me)choose(me);});}
                document.addEventListener('click',function(e){var r=e.target.closest('[data-role]');if(r){$('role').style.display='none';$(r.dataset.role).style.display='block';return;}
                 var s=e.target.closest('[data-send]');if(s){var ids=[].slice.call(document.querySelectorAll('#clubList input:checked')).map(function(x){return x.value;});
                  if(!ids.length){msg('Bitte zuerst Personen auswählen.',false);return;}send(ids,s.dataset.send==='1');return;}
                 var m=e.target.closest('[data-me]');if(m){send([me],m.dataset.me==='1');return;}
                 var a=e.target.closest('#hits a');if(a){e.preventDefault();choose(a.dataset.id);}});
                function hits(){var w=norm($('cq').value).split(/\\s+/).filter(function(x){return x;});
                 var h=w.length?data.players.filter(function(p){return w.every(function(x){return norm(p.n+' '+p.c).indexOf(x)>=0;});}).slice(0,30):[];
                 $('hits').innerHTML=h.map(function(p){return '<a class="card" href="#" data-id="'+esc(p.id)+'"><div class="row"><b>'+esc(p.n)+'</b><span class="muted">'+lbl[p.s]+'</span></div><div class="muted">'+esc(p.c)+'</div></a>';}).join('');}
                $('cq').addEventListener('input',hits);
                function choose(id){me=id;var p=data.players.filter(function(x){return x.id===id;})[0];if(!p)return;var c=$('chosen');c.style.display='block';
                 $('hits').innerHTML='';$('cq').value='';
                 c.innerHTML='<b>'+esc(p.n)+'</b><div class="muted">'+esc(p.c)+' · '+lbl[p.s]+'</div>'+(p.s==='withdrawn'||p.s==='locked'?'<p class="warn">Online nicht mehr änderbar – bitte an die Turnierleitung wenden.</p>':
                 '<button type="button" data-me="1">Ich bin da – anmelden</button><button type="button" class="light" data-me="0">Ich komme nicht – abmelden</button>');}
                function club(){var c=$('club').value,l=$('clubList');
                 if(!c){l.innerHTML='';$('coachBtns').style.display='none';return;}
                 var ps=data.players.filter(function(p){return p.c===c;});
                 l.innerHTML='<div class="card">'+ps.map(function(p){var off=p.s==='withdrawn'||p.s==='locked';return '<label class="pl"><input type="checkbox" value="'+esc(p.id)+'"'+(off?' disabled':'')+'><span><b>'+esc(p.n)+'</b><div class="muted">'+lbl[p.s]+'</div></span></label>';}).join('')+'</div>';
                 $('coachBtns').style.display=ps.length?'block':'none';}
                $('club').addEventListener('change',club);
                function msg(t,ok){var m=$('msg');m.style.display='block';m.className='card '+(ok?'ok':'warn');m.textContent=t;scrollTo(0,0);}
                function send(ids,present){fetch(base+'/melden',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},
                 body:'ids='+encodeURIComponent(ids.join(','))+'&anwesend='+(present?'1':'0')}).then(function(r){return r.json();}).then(function(d){msg(d.msg,d.ok);load();})
                 .catch(function(){msg('Keine Verbindung – bitte später noch einmal versuchen.',false);});}
                load();})();
                </script>
                """;
        return page("Anmeldung am Turniertag", body, false);
    }
}
