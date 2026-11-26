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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 1.12.0 (#189): Verwaltung der Ausrichter-Schlüssel (Design vom 26.11.2026).
 * <p>
 * Eigener kleiner Webserver nur auf {@code 127.0.0.1:<admin-port>} (Vorgabe 8091) – von außen nicht erreichbar, Zugang
 * über einen SSH-Tunnel ({@code ssh -L 8091:127.0.0.1:8091 …}, dann {@code http://localhost:8091/admin}). Schutz gegen
 * fremde Webseiten im selben Browser: nur Host {@code localhost}/{@code 127.0.0.1}, Aktionen nur per POST mit
 * Prüfzeichen (bei jedem Start neu) und passendem Origin.
 */
final class KeyAdmin {

    static final long IDLE_MS = TimeUnit.DAYS.toMillis(180);    // „lange nicht genutzt“
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMAN).withZone(BERLIN);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN).withZone(BERLIN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).withZone(BERLIN);

    private final LiveHost host;
    private final int port;
    final String token;
    private HttpServer http;

    KeyAdmin(LiveHost host, int port) {
        this.host = host;
        this.port = port;
        byte[] t = new byte[24];
        new SecureRandom().nextBytes(t);
        this.token = HexFormat.of().formatHex(t);
    }

    void start() throws IOException {
        http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 16);
        http.createContext("/", this::handle);
        http.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread th = new Thread(r, "HTS-Verwaltung");
            th.setDaemon(true);
            return th;
        }));
        http.start();
        System.err.println("Verwaltung der Ausrichter-Schlüssel: http://localhost:" + port() + "/admin (nur über SSH-Tunnel)");
    }

    void stop() {
        if (http != null) http.stop(0);
    }

    int port() {
        return http.getAddress().getPort();
    }

    /** Nur Aufrufe über localhost bzw. 127.0.0.1 (Schutz gegen DNS-Rebinding). */
    static boolean localHost(String hostHeader) {
        if (hostHeader == null) return false;
        String h = hostHeader.strip().toLowerCase(Locale.ROOT);
        int c = h.lastIndexOf(':');
        if (c > 0 && !h.endsWith("]")) h = h.substring(0, c);
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("[::1]");
    }

    static boolean localOrigin(String origin) {
        if (origin == null) return true;                      // ältere Browser schicken keinen Origin
        try {
            java.net.URI u = java.net.URI.create(origin.strip());
            return "http".equals(u.getScheme()) && u.getHost() != null && localHost(u.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            var h = ex.getResponseHeaders();
            h.set("X-Content-Type-Options", "nosniff");
            h.set("Referrer-Policy", "same-origin");       // bei „no-referrer“ schickt Chrome „Origin: null“ – Formulare scheitern
            h.set("X-Frame-Options", "DENY");
            h.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; img-src 'self'; "
                    + "form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
            if (!localHost(ex.getRequestHeaders().getFirst("Host"))) {
                send(ex, 403, "text/plain", "Nur über localhost erreichbar (SSH-Tunnel).");
                return;
            }
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/") || path.equals("/admin/")) {
                redirect(ex, "/admin");
                return;
            }
            if (path.equals("/favicon.png") || path.equals("/favicon.ico")) {
                byte[] png;
                try (var in = KeyAdmin.class.getResourceAsStream("/de/hts/ui/hts-logo-klein.png")) {
                    png = in == null ? new byte[0] : in.readAllBytes();
                }
                ex.getResponseHeaders().set("Content-Type", "image/png");
                ex.sendResponseHeaders(png.length == 0 ? 404 : 200, png.length == 0 ? -1 : png.length);
                if (png.length > 0) try (OutputStream os = ex.getResponseBody()) {
                    os.write(png);
                }
                return;
            }
            if (path.equals("/admin") && ("GET".equals(method) || "HEAD".equals(method))) {
                send(ex, 200, "text/html", page(LiveServer.parseQuery(ex.getRequestURI().getRawQuery())));
                return;
            }
            if (path.equals("/admin/aktion") && "POST".equals(method)) {
                action(ex);
                return;
            }
            send(ex, 404, "text/plain", "Nicht gefunden");
        } catch (RuntimeException e) {
            System.err.println("Verwaltung: Fehler bei der Anfrage: " + e);
            send(ex, 500, "text/plain", "Interner Fehler");
        }
    }

    private void action(HttpExchange ex) throws IOException {
        byte[] b = ex.getRequestBody().readNBytes(8 * 1024);
        Map<String, String> f;
        try {
            f = LiveServer.parseQuery(new String(b, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            f = Map.of();
        }
        if (!localOrigin(ex.getRequestHeaders().getFirst("Origin"))
                || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), f.getOrDefault("t", "").getBytes(StandardCharsets.UTF_8))) {
            send(ex, 403, "text/plain", "Abgelehnt – Seite neu laden und noch einmal versuchen.");
            return;
        }
        String id = f.getOrDefault("id", "");
        long now = System.currentTimeMillis();
        OrganizerKeys.Key k = host.keys.get(id);
        String msg;
        switch (f.getOrDefault("tat", "")) {
            case "freigeben" -> msg = host.keys.setStatus(id, OrganizerKeys.Status.AKTIV, now) ? "freigegeben" : null;
            case "ablehnen" -> msg = host.keys.setStatus(id, OrganizerKeys.Status.ABGELEHNT, now) ? "abgelehnt" : null;
            case "sperren" -> msg = host.keys.setStatus(id, OrganizerKeys.Status.GESPERRT, now) ? "gesperrt" : null;
            case "entsperren" -> msg = host.keys.setStatus(id, OrganizerKeys.Status.AKTIV, now) ? "entsperrt" : null;
            case "loeschen" -> msg = host.keys.delete(id) ? "gelöscht" : null;
            default -> msg = null;
        }
        String back = f.getOrDefault("zurueck", "");
        String q = back.matches("[a-z]*") ? back : "";
        String note = msg == null || k == null ? "Aktion nicht möglich" : k.org + ": " + msg;
        redirect(ex, "/admin?" + (q.isEmpty() ? "" : "f=" + q + "&") + "ok=" + java.net.URLEncoder.encode(note, StandardCharsets.UTF_8));
    }

    static boolean idle(OrganizerKeys.Key k, long now) {
        long since = k.lastUsed > 0 ? k.lastUsed : k.decided;
        return k.status == OrganizerKeys.Status.AKTIV && now - since > IDLE_MS;
    }

    static String when(long t, long now) {
        if (t <= 0) return "noch nie";
        LocalDate d = Instant.ofEpochMilli(t).atZone(BERLIN).toLocalDate();
        if (d.equals(Instant.ofEpochMilli(now).atZone(BERLIN).toLocalDate())) return "heute, " + TIME.format(Instant.ofEpochMilli(t));
        return DAY.format(Instant.ofEpochMilli(t));
    }

    private static final String CSS = "body{margin:0;background:#f6f7f8;color:#1f2933;font-family:-apple-system,'Segoe UI',Roboto,Arial,sans-serif;font-size:15px}"
            + "a{color:#187250}header{background:#fff;border-bottom:2px solid #187250;padding:10px 12px}"
            + ".hb{max-width:1100px;margin:0 auto;display:flex;align-items:center;gap:10px}.hb img{width:34px;height:34px;border-radius:8px}"
            + ".hb b{display:block;font-size:16px;line-height:1.2}.hb small{display:block;color:#6b7280;font-size:12px}"
            + "main{max-width:1100px;margin:0 auto;padding:18px 24px 32px;box-sizing:border-box;display:flex;flex-direction:column;gap:14px}"
            + ".top{display:flex;flex-wrap:wrap;gap:10px;align-items:center;justify-content:space-between}h1{margin:0;font-size:22px}"
            + ".pill{font-size:12px;color:#4a5560;background:#e8ebf0;border-radius:999px;padding:4px 10px}"
            + ".card{background:#fff;border:1px solid #e4e6ea;border-radius:12px;padding:14px 16px 6px}"
            + ".sh{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:4px;flex-wrap:wrap}.sh>span{display:flex;align-items:center;gap:8px;flex-wrap:wrap}"
            + ".btn{display:inline-flex;align-items:center;justify-content:center;min-height:34px;padding:0 12px;border-radius:999px;border:0;background:#187250;color:#fff;"
            + "font-size:13px;font-weight:600;font-family:inherit;cursor:pointer;text-decoration:none;white-space:nowrap}"
            + ".btn.l{background:#fff;color:#1f2933;border:1px solid #d0d5db;font-weight:500}"
            + ".inp{width:320px;max-width:100%;min-height:36px;padding:0 12px;border:1px solid #d0d5db;border-radius:10px;background:#fff;font-size:13px;font-family:inherit;box-sizing:border-box}"
            + ".chip{display:inline-block;font-size:11px;padding:2px 8px;border-radius:999px;white-space:nowrap}"
            + ".c-open{background:#fff4d6;color:#7a5200}.c-on{background:#e4f7ef;color:#0b3d2c}.c-off{background:#fde8e8;color:#8a1c1c}.c-no{background:#e8ebf0;color:#2d3440}"
            + ".tw{overflow-x:auto}table{border-collapse:collapse;width:100%;font-size:13px}th,td{padding:9px 8px;text-align:left;border-bottom:1px solid #eef0f2;vertical-align:middle}"
            + "th{color:#6b7280;font-size:11px;font-weight:600;white-space:nowrap}td{white-space:nowrap}.acts{display:flex;gap:6px;flex-wrap:wrap}.acts form{margin:0}"
            + ".muted{font-size:12px;color:#6b7280}.foot{font-size:12px;color:#6b7280;margin:8px 0 6px;line-height:1.5}"
            + ".ok{background:#e4f7ef;border:1px solid #b9e8d6;border-radius:10px;padding:8px 12px;font-size:14px}";

    String page(Map<String, String> q) {
        long now = System.currentTimeMillis();
        String filter = q.getOrDefault("f", "");
        String search = OrganizerKeys.clean(q.get("q"), 100).toLowerCase(Locale.ROOT);
        Map<String, Integer> counts = host.tournamentsByOwner();
        List<OrganizerKeys.Key> all = host.keys.all();
        StringBuilder open = new StringBuilder();
        int openN = 0;
        for (OrganizerKeys.Key k : all) {
            if (k.status != OrganizerKeys.Status.OFFEN) continue;
            openN++;
            open.append("<tr><td><b>").append(esc(k.org)).append("</b></td><td>").append(esc(k.name)).append("</td><td><a href=\"mailto:")
                    .append(esc(k.email)).append("\">").append(esc(k.email)).append("</a></td><td>").append(STAMP.format(Instant.ofEpochMilli(k.requested)))
                    .append("</td><td><div class=\"acts\">").append(button(k, "freigeben", "Freigeben", false, filter, null))
                    .append(button(k, "ablehnen", "Ablehnen", true, filter, null)).append("</div></td></tr>");
        }
        StringBuilder rows = new StringBuilder();
        int total = 1, shown = 0;
        boolean opMatches = filter.isEmpty() || "aktiv".equals(filter);
        if (opMatches && (search.isEmpty() || "betreiber".contains(search))) {
            shown++;
            rows.append("<tr><td><b>Betreiber</b> <span class=\"muted\">(Schlüssel aus server.properties)</span></td><td>–</td><td>–</td>")
                    .append("<td><span class=\"chip c-on\">aktiv</span></td><td>").append(when(host.lastOperatorUse, now))
                    .append(host.lastOperatorUse == 0 ? " <span class=\"muted\">(seit dem Start)</span>" : "").append("</td><td>")
                    .append(counts.getOrDefault("", 0)).append("</td><td><span class=\"muted\">fest (install.sh)</span></td></tr>");
        }
        for (OrganizerKeys.Key k : all) {
            if (k.status == OrganizerKeys.Status.OFFEN) continue;
            total++;
            boolean ok = switch (filter) {
                case "aktiv" -> k.status == OrganizerKeys.Status.AKTIV;
                case "gesperrt" -> k.status == OrganizerKeys.Status.GESPERRT;
                case "ungenutzt" -> idle(k, now);
                default -> true;
            };
            if (ok && !search.isEmpty()) ok = (k.org + " " + k.name + " " + k.email).toLowerCase(Locale.ROOT).contains(search);
            if (!ok) continue;
            shown++;
            String chip = switch (k.status) {
                case AKTIV -> "<span class=\"chip c-on\">aktiv</span>";
                case GESPERRT -> "<span class=\"chip c-off\">gesperrt</span>";
                case ABGELEHNT -> "<span class=\"chip c-no\">abgelehnt</span>";
                case OFFEN -> "";
            };
            String acts = switch (k.status) {
                case AKTIV -> button(k, "sperren", "Sperren", true, filter, null);
                case GESPERRT -> button(k, "entsperren", "Entsperren", true, filter, null);
                default -> "";
            } + button(k, "loeschen", "Löschen", true, filter, "Schlüssel von " + k.org + " wirklich löschen? Er kann danach nichts mehr hochladen.");
            rows.append("<tr><td><b>").append(esc(k.org)).append("</b></td><td>").append(esc(k.name)).append("</td><td><a href=\"mailto:").append(esc(k.email))
                    .append("\">").append(esc(k.email)).append("</a></td><td>").append(chip).append("</td><td>")
                    .append(k.status == OrganizerKeys.Status.ABGELEHNT ? "abgelehnt am " + DAY.format(Instant.ofEpochMilli(k.decided)) : when(k.lastUsed, now))
                    .append("</td><td>").append(counts.getOrDefault(k.id, 0)).append("</td><td><div class=\"acts\">").append(acts).append("</div></td></tr>");
        }
        StringBuilder b = new StringBuilder("<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>Verwaltung – " + esc(host.siteTitle()) + "</title><link rel=\"icon\" href=\"/favicon.png\"><style>" + CSS + "</style></head><body>"
                + "<header><div class=\"hb\"><img src=\"/favicon.png\" alt=\"HTS\"><span><b>" + esc(host.siteTitle())
                + "</b><small>Verwaltung · nur auf diesem Rechner über SSH-Tunnel</small></span></div></header><main>"
                + "<div class=\"top\"><h1>Verwaltung der Ausrichter-Schlüssel</h1><span class=\"pill\">nur über SSH-Tunnel · http://localhost:" + port + "/admin</span></div>");
        String ok = OrganizerKeys.clean(q.get("ok"), 200);
        if (!ok.isEmpty()) b.append("<div class=\"ok\" role=\"status\">").append(esc(ok)).append("</div>");
        b.append("<section class=\"card\"><div class=\"sh\"><span><b>Offene Anträge</b><span class=\"chip c-open\">").append(openN).append(" offen</span></span></div>");
        if (openN == 0) b.append("<p class=\"muted\" style=\"margin:6px 0 10px\">Keine offenen Anträge.</p>");
        else b.append("<div class=\"tw\"><table><thead><tr><th>Verein / Verband</th><th>Name</th><th>E-Mail</th><th>beantragt</th><th>Aktion</th></tr></thead><tbody>")
                .append(open).append("</tbody></table></div>");
        b.append("</section><section class=\"card\"><div class=\"sh\"><span><b>Ausrichter-Schlüssel</b><span class=\"muted\">").append(shown).append(" von ").append(total).append("</span>");
        String[][] fs = {{"", "Alle"}, {"aktiv", "aktiv"}, {"gesperrt", "gesperrt"}, {"ungenutzt", "lange nicht genutzt"}};
        for (String[] x : fs) {
            b.append("<a class=\"btn").append(x[0].equals(filter) ? "" : " l").append("\" href=\"/admin").append(x[0].isEmpty() ? "" : "?f=" + x[0]).append("\">").append(x[1]).append("</a>");
        }
        b.append("</span><form method=\"get\" action=\"/admin\" style=\"margin:0\">").append(filter.isEmpty() ? "" : "<input type=\"hidden\" name=\"f\" value=\"" + esc(filter) + "\">")
                .append("<input class=\"inp\" name=\"q\" value=\"").append(esc(q.getOrDefault("q", ""))).append("\" placeholder=\"Verein, Name oder E-Mail suchen\" aria-label=\"Schlüssel suchen\"></form></div>")
                .append("<div class=\"tw\"><table><thead><tr><th>Verein / Verband</th><th>Name</th><th>E-Mail</th><th>Status</th><th>zuletzt genutzt</th><th>Turniere</th><th>Aktion</th></tr></thead><tbody>")
                .append(rows).append("</tbody></table></div>")
                .append("<p class=\"foot\">Gesperrt: Der Schlüssel kann nichts mehr hochladen, die Turniere bleiben sichtbar. Löschen: Schlüssel weg; seine Turniere bleiben, "
                        + "bis sie nach der Aufbewahrungsfrist verschwinden. Abgelehnte Anträge werden nach 30 Tagen automatisch gelöscht. Der Server speichert nur Prüfwerte der Schlüssel.</p>")
                .append("</section></main></body></html>");
        return b.toString();
    }

    private String button(OrganizerKeys.Key k, String tat, String label, boolean light, String filter, String confirm) {
        return "<form method=\"post\" action=\"/admin/aktion\"" + (confirm == null ? "" : " onsubmit=\"return confirm('" + esc(confirm.replace("'", "’")) + "')\"")
                + "><input type=\"hidden\" name=\"t\" value=\"" + token + "\"><input type=\"hidden\" name=\"id\" value=\"" + esc(k.id)
                + "\"><input type=\"hidden\" name=\"tat\" value=\"" + tat + "\"><input type=\"hidden\" name=\"zurueck\" value=\"" + esc(filter)
                + "\"><button class=\"btn" + (light ? " l" : "") + "\" type=\"submit\">" + label + "</button></form>";
    }

    private static String esc(String s) {
        return LiveServer.esc(s);
    }

    private static void redirect(HttpExchange ex, String to) throws IOException {
        ex.getResponseHeaders().set("Location", to);
        ex.sendResponseHeaders(303, -1);
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        boolean head = "HEAD".equals(ex.getRequestMethod());
        ex.sendResponseHeaders(code, head ? -1 : b.length);
        if (!head) try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }
}
