/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import java.util.ArrayList;
import java.util.List;

/**
 * Vorgaberegeln eines Vorgabeturniers: TTR-Unterschiede werden durch einen Punktevorsprung je Satz ausgeglichen.
 *
 * <p>Zwei Arten: <b>TTR-Klassen</b> (2–7 Klassen; Bälle je Klassenunterschied) oder <b>Fester TTR-Abstand</b>
 * (2–10 Kategorien des TTR-Abstands; Bälle je Kategorie). Grenzen getrennt für „Erwachsene“ und „Damen“, lückenlos:
 * Untergrenze „&gt;“, Obergrenze „&lt;=“, die Obergrenze einer Zeile ist die Untergrenze der nächsten. Es gibt keine
 * fest eingebauten Werte.</p>
 */
public final class HandicapRules {

    public enum Mode {
        CLASSES("TTR-Klassen"), DISTANCE("Fester TTR-Abstand");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Serve {
        DRAW("Aufschlag wird gelost"), NO_ADVANTAGE("Partei ohne Punktevorsprung hat den Aufschlag");

        private final String label;

        Serve(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final int MIN_CLASSES = 2, MAX_CLASSES = 7, MIN_CATEGORIES = 2, MAX_CATEGORIES = 10;

    /** Eine Grenzen-Tabelle (Klassen oder Kategorien): Untergrenze der ersten Zeile, Obergrenzen je Zeile. */
    public static final class Bounds {
        private Integer firstLower;
        private final List<Integer> uppers = new ArrayList<>();

        public Integer getFirstLower() { return firstLower; }
        public void setFirstLower(Integer v) { firstLower = v; }
        public List<Integer> getUppers() { return uppers; }

        /** Untergrenze (exklusiv) der Zeile {@code i}. */
        public Integer lower(int i) {
            return i == 0 ? firstLower : uppers.get(i - 1);
        }

        /** Zeile (0-basiert), in der der Wert liegt: Untergrenze &lt; v &lt;= Obergrenze; -1 = unterhalb, -2 = oberhalb. */
        public int indexOf(int v) {
            if (firstLower == null || uppers.isEmpty() || uppers.contains(null)) return -1;
            if (v <= firstLower) return -1;
            for (int i = 0; i < uppers.size(); i++) if (v <= uppers.get(i)) return i;
            return -2;
        }

        Bounds copy() {
            Bounds b = new Bounds();
            b.firstLower = firstLower;
            b.uppers.addAll(uppers);
            return b;
        }
    }

    private Mode mode;
    private Serve serve = Serve.DRAW;
    private boolean halve = true;
    private final Bounds classAdults = new Bounds(), classWomen = new Bounds();
    private final List<Integer> classBalls = new ArrayList<>();          // Index d-1 = Bälle bei d Klassen Unterschied
    private final Bounds distAdults = new Bounds(), distWomen = new Bounds();
    private final List<Integer> distBalls = new ArrayList<>();           // je Kategorie

    public HandicapRules() {
        setRowCount(Mode.CLASSES, MIN_CLASSES);
        setRowCount(Mode.DISTANCE, MIN_CATEGORIES);
    }

    /** Art der Vorgabe; {@code null} = noch nicht festgelegt (gespeichert). */
    public Mode getMode() { return mode; }
    public void setMode(Mode m) { mode = m; }
    public Serve getServe() { return serve; }
    public void setServe(Serve s) { serve = s == null ? Serve.DRAW : s; }
    public boolean isHalve() { return halve; }
    public void setHalve(boolean h) { halve = h; }

    public Bounds bounds(Mode m, boolean women) {
        return m == Mode.CLASSES ? (women ? classWomen : classAdults) : (women ? distWomen : distAdults);
    }

    /** Bälle: bei Klassen je Klassenunterschied (Index d-1), bei Abständen je Kategorie. */
    public List<Integer> balls(Mode m) {
        return m == Mode.CLASSES ? classBalls : distBalls;
    }

    public int rowCount(Mode m) {
        return bounds(m, false).uppers.size();
    }

    /** Anzahl Klassen bzw. Kategorien setzen (Zeilen anhängen bzw. am Ende entfernen). */
    public void setRowCount(Mode m, int n) {
        int min = m == Mode.CLASSES ? MIN_CLASSES : MIN_CATEGORIES, max = m == Mode.CLASSES ? MAX_CLASSES : MAX_CATEGORIES;
        n = Math.max(min, Math.min(max, n));
        for (Bounds b : new Bounds[]{bounds(m, false), bounds(m, true)}) {
            while (b.uppers.size() < n) b.uppers.add(null);
            while (b.uppers.size() > n) b.uppers.remove(b.uppers.size() - 1);
        }
        List<Integer> balls = balls(m);
        int nb = m == Mode.CLASSES ? n - 1 : n;
        while (balls.size() < nb) balls.add(null);
        while (balls.size() > nb) balls.remove(balls.size() - 1);
    }

    public HandicapRules copy() {
        HandicapRules r = new HandicapRules();
        r.mode = mode;
        r.serve = serve;
        r.halve = halve;
        for (Mode m : Mode.values()) {
            for (boolean w : new boolean[]{false, true}) {
                Bounds src = bounds(m, w), dst = r.bounds(m, w);
                dst.firstLower = src.firstLower;
                dst.uppers.clear();
                dst.uppers.addAll(src.uppers);
            }
            r.balls(m).clear();
            r.balls(m).addAll(balls(m));
        }
        return r;
    }

    /** Fehler, die das Speichern verhindern; leer = gültig. Geprüft wird nur der Teil der gewählten Art. */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (mode == null) {
            errors.add("Bitte die Art der Vorgabe wählen.");
            return errors;
        }
        String row = mode == Mode.CLASSES ? "Klasse" : "Abstand";
        for (boolean w : new boolean[]{false, true}) {
            Bounds b = bounds(mode, w);
            String who = w ? "Damen" : "Erwachsene";
            if (b.firstLower == null) errors.add(who + ": Untergrenze von " + row + " 1 fehlt.");
            for (int i = 0; i < b.uppers.size(); i++) {
                Integer up = b.uppers.get(i), lo = b.lower(i);
                if (up == null) errors.add(who + ": Obergrenze von " + row + " " + (i + 1) + " fehlt.");
                else if (lo != null && up <= lo) errors.add(who + ": Obergrenze von " + row + " " + (i + 1) + " muss größer als die Untergrenze (" + lo + ") sein.");
            }
        }
        List<Integer> balls = balls(mode);
        for (int i = 0; i < balls.size(); i++) {
            if (balls.get(i) == null || balls.get(i) < 0) {
                errors.add(mode == Mode.CLASSES ? "Bälle bei " + (i + 1) + (i == 0 ? " Klasse" : " Klassen") + " Unterschied fehlen."
                        : "Bälle bei Abstand " + (i + 1) + " fehlen.");
            }
        }
        return errors;
    }

    // --- Berechnung ----------------------------------------------------------------------------

    /** Ergebnis einer Einzelberechnung. */
    public record Result(int balls, String problem) {
        static Result ok(int b) { return new Result(b, null); }
        static Result fail(String p) { return new Result(0, p); }
    }

    /**
     * Vorgabe zwischen zwei Personen (TTR {@code a} und {@code b}) nach der gewählten Tabelle: positiv = {@code a}
     * bekommt Bälle, negativ = {@code b}. Problem gesetzt, wenn ein Wert außerhalb der Grenzen liegt.
     */
    public Result between(int a, int b, boolean women) {
        if (mode == null) return Result.ok(0);
        if (mode == Mode.CLASSES) {
            Bounds bs = bounds(Mode.CLASSES, women);
            int ka = bs.indexOf(a), kb = bs.indexOf(b);
            if (ka < 0) return Result.fail("TTR " + a + " liegt außerhalb der Klassengrenzen (" + (women ? "Damen" : "Erwachsene") + ")");
            if (kb < 0) return Result.fail("TTR " + b + " liegt außerhalb der Klassengrenzen (" + (women ? "Damen" : "Erwachsene") + ")");
            int d = Math.abs(ka - kb);
            if (d == 0) return Result.ok(0);
            Integer balls = d - 1 < classBalls.size() ? classBalls.get(d - 1) : null;
            int v = balls == null ? 0 : balls;
            return Result.ok(ka < kb ? v : -v);                         // niedrigere Klasse (niedrigerer TTR) bekommt Vorsprung
        }
        int d = Math.abs(a - b);
        if (d == 0) return Result.ok(0);
        Bounds bs = bounds(Mode.DISTANCE, women);
        int k = bs.indexOf(d);
        if (k == -2) return Result.fail("TTR-Abstand " + d + " liegt über der höchsten Obergrenze (" + (women ? "Damen" : "Erwachsene") + ")");
        if (k < 0) return Result.ok(0);
        Integer balls = k < distBalls.size() ? distBalls.get(k) : null;
        int v = balls == null ? 0 : balls;
        return Result.ok(a < b ? v : -v);
    }

    /** Doppel: zwei Vergleiche verrechnen; mit Halbierung danach durch 2 (abgerundet). */
    public int combine(int first, int second) {
        int sum = first + second;
        if (!halve) return sum;
        return Integer.signum(sum) * (Math.abs(sum) / 2);
    }

    // --- Speichern als Text ----------------------------------------------------------------------

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append("mode=").append(mode == null ? "" : mode.name());
        sb.append(";serve=").append(serve.name());
        sb.append(";halve=").append(halve);
        for (Mode m : Mode.values()) {
            String p = m == Mode.CLASSES ? "c" : "d";
            sb.append(';').append(p).append("A0=").append(num(bounds(m, false).firstLower));
            sb.append(';').append(p).append("W0=").append(num(bounds(m, true).firstLower));
            sb.append(';').append(p).append("A=").append(list(bounds(m, false).uppers));
            sb.append(';').append(p).append("W=").append(list(bounds(m, true).uppers));
            sb.append(';').append(p).append("B=").append(list(balls(m)));
        }
        return sb.toString();
    }

    public static HandicapRules decode(String s) {
        HandicapRules r = new HandicapRules();
        if (s == null || s.isBlank()) return r;
        java.util.Map<String, String> kv = new java.util.HashMap<>();
        for (String part : s.split(";")) {
            int i = part.indexOf('=');
            if (i > 0) kv.put(part.substring(0, i), part.substring(i + 1));
        }
        try {
            String m = kv.get("mode");
            r.mode = m == null || m.isBlank() ? null : Mode.valueOf(m);
        } catch (IllegalArgumentException ignored) {
            r.mode = null;
        }
        try {
            if (kv.get("serve") != null) r.serve = Serve.valueOf(kv.get("serve"));
        } catch (IllegalArgumentException ignored) {
            // Standard
        }
        if (kv.get("halve") != null) r.halve = !"false".equals(kv.get("halve"));
        for (Mode m : Mode.values()) {
            String p = m == Mode.CLASSES ? "c" : "d";
            List<Integer> a = parseList(kv.get(p + "A")), w = parseList(kv.get(p + "W")), b = parseList(kv.get(p + "B"));
            int n = Math.max(a.size(), w.size());
            if (n > 0) r.setRowCount(m, n);
            fill(r.bounds(m, false).uppers, a);
            fill(r.bounds(m, true).uppers, w);
            fill(r.balls(m), b);
            r.bounds(m, false).firstLower = parseNum(kv.get(p + "A0"));
            r.bounds(m, true).firstLower = parseNum(kv.get(p + "W0"));
        }
        return r;
    }

    private static void fill(List<Integer> target, List<Integer> values) {
        for (int i = 0; i < target.size() && i < values.size(); i++) target.set(i, values.get(i));
    }

    private static String num(Integer v) {
        return v == null ? "" : v.toString();
    }

    private static String list(List<Integer> l) {
        return String.join(",", l.stream().map(HandicapRules::num).toList());
    }

    private static Integer parseNum(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Integer> parseList(String v) {
        List<Integer> out = new ArrayList<>();
        if (v == null || v.isEmpty()) return out;
        for (String x : v.split(",", -1)) out.add(parseNum(x));
        return out;
    }
}
