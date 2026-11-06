package de.hts.tournament;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ein Spiel einer Klasse (Gruppenspiel oder KO-Spiel) mit Status, Tisch und Ergebnis.
 * Die Parteien werden über ihre Entrant-ID referenziert (Personen-ID bzw. {@code "pair:" + Paarungs-ID});
 * bei KO-Spielen späterer Runden sind sie erst bekannt, wenn die Vorrunden-Spiele entschieden sind.
 * Änderungen bitte über {@link Tournament} vornehmen.
 */
public final class Match {

    private final String id;
    private String className;
    /** z. B. „Gruppe A“ oder „Viertelfinale“ */
    private final String stage;
    /** Runde: in Gruppen der Spieltag, im KO-System 1 = erste Runde */
    private final int round;
    /** fortlaufende Spielnummer innerhalb der Klasse */
    private int number;
    /** KO: Runde (0 = erste) und Position in der Runde; Gruppen: -1 */
    private final int koRound;
    private final int koIndex;
    /** Gruppen: Index der Gruppe (0 = A); KO: -1 */
    private final int groupIndex;

    private String a;
    private String b;
    private MatchStatus status = MatchStatus.PLANNED;
    private Integer table;
    /** Reihenfolge in der Warteschlange (kleiner = früher eingereiht) */
    private long queueOrder;
    private Integer setsA;
    private Integer setsB;
    /** Punkte je Satz, z. B. [[11, 8], [7, 11], …] – erste Zahl gehört zu A. */
    private java.util.List<int[]> points = java.util.List.of();
    /** Aufgabe: 0 = keine, 1 = A gibt auf, 2 = B gibt auf */
    private int retired;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    /** Platzierungsspiel: Platz des Siegers (Verlierer = +1); 0 = kein Platzierungsspiel. */
    private int placement;
    /** Aus der vorherigen Phase übernommenes Ergebnis. */
    private boolean carried;
    /** Von Hand als „nicht relevant für Export“ markiert (nur abgeschlossene Spiele). */
    private boolean exportExcluded;
    /** Kampflos gewertet, weil diese Partei (Entrant-ID) in einem anderen Spiel aufgegeben hat; nie exportiert. */
    private String retirementOf;

    public Match(String id, String className, String stage, int round, int number,
                 int koRound, int koIndex, int groupIndex, String a, String b) {
        this.id = id != null ? id : UUID.randomUUID().toString().substring(0, 13).replace("-", "");
        this.className = className;
        this.stage = stage;
        this.round = round;
        this.number = number;
        this.koRound = koRound;
        this.koIndex = koIndex;
        this.groupIndex = groupIndex;
        this.a = a;
        this.b = b;
    }

    public boolean isKnockout() {
        return koRound >= 0;
    }

    /** Beide Gegner stehen fest. */
    public boolean isReady() {
        return a != null && b != null;
    }

    /** Sieger-ID (nur bei abgeschlossenen Spielen), sonst {@code null}. */
    public String winner() {
        if (status != MatchStatus.DONE || setsA == null || setsB == null) return null;
        return setsA > setsB ? a : b;
    }

    public String loser() {
        if (status != MatchStatus.DONE || setsA == null || setsB == null) return null;
        return setsA > setsB ? b : a;
    }

    /** z. B. „3:1“ oder leer */
    public String getResultText() {
        return setsA == null || setsB == null ? "" : setsA + ":" + setsB;
    }

    /** z. B. „11:8, 7:11, 11:5, 11:9“ bzw. „Aufgabe B“ */
    public String getSetsText() {
        if (retirementOf != null) return "kampflos (Aufgabe " + (retired == 1 ? "A" : "B") + " in anderem Spiel)";
        if (retired != 0) return "Aufgabe " + (retired == 1 ? "A" : "B");
        if (!lineupA.isEmpty()) return "";        // Team-Begegnung: nur Partien (z. B. 3:2), keine Sätze
        StringBuilder sb = new StringBuilder();
        for (int[] p : points) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(p[0]).append(':').append(p[1]);
        }
        return sb.toString();
    }

    public java.util.List<int[]> getPoints() { return points; }
    public int getRetired() { return retired; }

    /** Punkte (Bälle) von A bzw. B über alle Sätze. */
    public int pointsA() { return points.stream().mapToInt(p -> p[0]).sum(); }
    public int pointsB() { return points.stream().mapToInt(p -> p[1]).sum(); }

    void setScore(java.util.List<int[]> sets, int retired, int setsA, int setsB) {
        java.util.List<int[]> copy = new java.util.ArrayList<>();
        for (int[] s : sets) copy.add(new int[]{s[0], s[1]});
        this.points = java.util.Collections.unmodifiableList(copy);
        this.retired = retired;
        this.setsA = setsA;
        this.setsB = setsB;
    }

    public String getId() { return id; }
    public String getClassName() { return className; }
    void setClassName(String n) { className = n; }
    public String getStage() { return stage; }
    public int getRound() { return round; }
    public int getNumber() { return number; }
    public int getKoRound() { return koRound; }
    public int getKoIndex() { return koIndex; }
    public int getGroupIndex() { return groupIndex; }
    public String getA() { return a; }
    public String getB() { return b; }
    void setA(String a) { this.a = a; }
    void setB(String b) { this.b = b; }
    public MatchStatus getStatus() { return status; }
    void setStatus(MatchStatus s) { status = s; }
    public Integer getTable() { return table; }
    void setTable(Integer t) { table = t; }
    public long getQueueOrder() { return queueOrder; }
    void setQueueOrder(long q) { queueOrder = q; }
    public Integer getSetsA() { return setsA; }
    public Integer getSetsB() { return setsB; }
    void setSets(Integer a, Integer b) { setsA = a; setsB = b; }
    public LocalDateTime getStartedAt() { return startedAt; }
    void setStartedAt(LocalDateTime t) { startedAt = t; }
    /** 2er/3er Turnier: Aufstellung beider Teams (Personen-IDs nach Position), leer = noch nicht festgelegt. */
    private java.util.List<String> lineupA = java.util.List.of(), lineupB = java.util.List.of();

    public java.util.List<String> getLineupA() { return lineupA; }
    public java.util.List<String> getLineupB() { return lineupB; }

    /** Aufstellung setzen (beim Ergebnis und beim Laden). */
    public void setLineups(java.util.List<String> a, java.util.List<String> b) {
        lineupA = a == null ? java.util.List.of() : java.util.List.copyOf(a);
        lineupB = b == null ? java.util.List.of() : java.util.List.copyOf(b);
    }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    void setFinishedAt(LocalDateTime t) { finishedAt = t; }

    /** Ergebnis und Tisch löschen – das Spiel ist wieder „geplant“ (z. B. weil sich ein Vorrundensieger geändert hat). */
    /** Nummer neu vergeben (Spiel um Platz 3 vor das Finale ziehen, solange beide noch nicht laufen). */
    void setNumber(int n) { number = n; }

    void clearScore() {
        lineupA = java.util.List.of();
        lineupB = java.util.List.of();
        retirementOf = null;
        exportExcluded = false;
        points = java.util.List.of();
        retired = 0;
        setsA = null;
        setsB = null;
        status = MatchStatus.PLANNED;
        table = null;
        startedAt = null;
        finishedAt = null;
    }

    public boolean isExportExcluded() { return exportExcluded; }
    public void setExportExcluded(boolean b) { exportExcluded = b; }
    public String getRetirementOf() { return retirementOf; }
    void setRetirementOf(String id) { retirementOf = id; }

    /** Gehört das Spiel in die click-TT-Ergebnisdatei? */
    public boolean isExported() {
        return status == MatchStatus.DONE && !carried && !exportExcluded && retirementOf == null;
    }

    /** Nur zum Laden. */
    public void restoreExport(boolean excluded, String retirementOf) {
        this.exportExcluded = excluded;
        this.retirementOf = retirementOf;
    }

    public int getPlacement() { return placement; }
    public boolean isCarried() { return carried; }

    /** Platzierungsspiel/übernommenes Spiel (beim Erstellen und Laden). */
    public void restorePhaseInfo(int placement, boolean carried) {
        this.placement = placement;
        this.carried = carried;
    }

    /** Nur zum Laden aus einer Datei. */
    public void restore(MatchStatus status, Integer table, long queueOrder, Integer setsA, Integer setsB,
                        LocalDateTime startedAt, LocalDateTime finishedAt, java.util.List<int[]> points, int retired) {
        this.points = points == null ? java.util.List.of() : java.util.List.copyOf(points);
        this.retired = retired;
        this.status = status;
        this.table = table;
        this.queueOrder = queueOrder;
        this.setsA = setsA;
        this.setsB = setsB;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }
}
