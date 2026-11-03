package de.hts.tournament;

import de.hts.player.Player;
import de.hts.player.Sex;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Eine Spielklasse (Konkurrenz) des Turniers, z. B. „Herren A“, „Damen“, „Jugend“ oder „Mädchen U15“.
 *
 * <p>Der {@link ClassType} legt fest, wer grundsätzlich mitspielen darf (Erwachsene, Damen, Jugend, Mädchen).
 * Zusätzlich lassen sich Jahrgangsgrenzen setzen (einschließlich), z. B. für U15
 * {@code birthyearFrom = 2012}, {@code birthyearTo = null} (2012 und jünger) oder für Senioren 40
 * {@code birthyearFrom = null}, {@code birthyearTo = 1986} (1986 und älter).
 *
 * <p>Jugend heißt: jünger als 21 Jahre am Turniertag. Ist nur der Jahrgang bekannt, zählt
 * der Jahrgang: Jugend ist, wer im Turnierjahr höchstens 20 wird.
 *
 * <p>Die Kriterien dienen der automatischen Zuordnung; von Hand kann jeder zugeordnet werden.
 */
public final class PlayingClass {

    private String name;
    private ClassType type = ClassType.ADULTS;
    private Discipline discipline = Discipline.SINGLES;
    /** Frühester zulässiger Jahrgang (ältester Spieler), {@code null} = keine Grenze. */
    private Integer birthyearFrom;
    /** Spätester zulässiger Jahrgang (jüngster Spieler), {@code null} = keine Grenze. */
    private Integer birthyearTo;

    public PlayingClass(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public PlayingClass(String name, ClassType type, Integer birthyearFrom, Integer birthyearTo) {
        this.name = Objects.requireNonNull(name);
        this.type = type == null ? ClassType.ADULTS : type;
        this.birthyearFrom = birthyearFrom;
        this.birthyearTo = birthyearTo;
    }

    public PlayingClass(String name, Discipline discipline, ClassType type, Integer birthyearFrom, Integer birthyearTo) {
        this(name, type, birthyearFrom, birthyearTo);
        setDiscipline(discipline);
    }

    /** Ergebnis der Prüfung, ob ein Spieler die Kriterien der Klasse erfüllt. */
    public enum Fit {
        /** passt */
        YES,
        /** passt nicht (Geschlecht, Alter oder Jahrgang) */
        NO,
        /** kann nicht geprüft werden, weil Geschlecht oder Jahrgang des Spielers fehlen */
        UNKNOWN
    }

    /**
     * {@code true}, wenn die Klasse jeden zulässt (Erwachsene ohne Jahrgangsgrenzen).
     * Solche Klassen werden bei der automatischen Zuordnung nur auf ausdrücklichen Wunsch befüllt.
     */
    public boolean acceptsEveryone() {
        return type == ClassType.ADULTS && birthyearFrom == null && birthyearTo == null;
    }

    /**
     * Prüft Geschlecht, Alter und Jahrgang des Spielers.
     *
     * @param referenceDate Stichtag für die Altersgrenze der Jugend (Turniertag)
     */
    public Fit fit(Player p, LocalDate referenceDate) {
        boolean unknown = false;
        if (type.isFemaleOnly()) {
            if (p.getSex() == Sex.UNKNOWN) unknown = true;
            else if (p.getSex() != Sex.FEMALE) return Fit.NO;
        }
        if (type.isYouthOnly()) {
            Boolean youth = isYouth(p, referenceDate);
            if (youth == null) unknown = true;
            else if (!youth) return Fit.NO;
        }
        if (birthyearFrom != null || birthyearTo != null) {
            Integer year = p.getBirthyear();
            if (year == null) unknown = true;
            else if ((birthyearFrom != null && year < birthyearFrom) || (birthyearTo != null && year > birthyearTo)) {
                return Fit.NO;
            }
        }
        return unknown ? Fit.UNKNOWN : Fit.YES;
    }

    /** Jünger als 21 am Stichtag? {@code null}, wenn weder Geburtsdatum noch Jahrgang bekannt sind. */
    public static Boolean isYouth(Player p, LocalDate referenceDate) {
        if (p.getBirthdate() != null) {
            return Period.between(p.getBirthdate(), referenceDate).getYears() < ClassType.YOUTH_AGE_LIMIT;
        }
        if (p.getBirthyear() != null) {
            return p.getBirthyear() >= firstYouthBirthyear(referenceDate);
        }
        return null;
    }

    /** Ältester Jahrgang, der am Stichtag noch zur Jugend zählt (z. B. 2006 für 2026). */
    public static int firstYouthBirthyear(LocalDate referenceDate) {
        return referenceDate.getYear() - (ClassType.YOUTH_AGE_LIMIT - 1);
    }

    /** Kurzbeschreibung, z. B. „Mädchen (unter 21, Jg. 2006 und jünger) · Jg. 2012 und jünger“. */
    public String getCriteriaText(LocalDate referenceDate) {
        StringBuilder sb = new StringBuilder(discipline.getLabel()).append(" · ").append(type.getLabel());
        sb.append(" (").append(type.isFemaleOnly() ? "nur weiblich" : "weiblich und männlich");
        if (type.isYouthOnly()) {
            sb.append(", unter ").append(ClassType.YOUTH_AGE_LIMIT)
                    .append(" = Jg. ").append(firstYouthBirthyear(referenceDate)).append(" und jünger");
        }
        sb.append(')');
        String years = null;
        if (birthyearFrom != null && birthyearTo != null) {
            years = birthyearFrom.equals(birthyearTo) ? "Jg. " + birthyearFrom : "Jg. " + birthyearFrom + "–" + birthyearTo;
        } else if (birthyearFrom != null) {
            years = "Jg. " + birthyearFrom + " und jünger";
        } else if (birthyearTo != null) {
            years = "Jg. " + birthyearTo + " und älter";
        }
        if (years != null) {
            sb.append(" · ").append(years);
        }
        return sb.toString();
    }

    public String getName() { return name; }

    /** Umbenennen bitte über {@link Tournament#renameClass}, damit die Meldungen mitgezogen werden. */
    void setName(String name) { this.name = Objects.requireNonNull(name); }

    // --- Höchstzahl, Modus, Auslosung ---------------------------------------

    /** Optionale Höchstzahl Teilnehmender (bei Doppel: Paarungen) – nur zur Kontrolle, {@code null} = keine. */
    private Integer maxParticipants;
    private TournamentMode mode = TournamentMode.NONE;
    /** Ergebnis der Auslosung: Belegung der KO-Plätze (Entrant-IDs), {@code null}-Einträge = Freilos. */
    private java.util.List<String> draw = java.util.List.of();
    private boolean drawSeeded;

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer max) { this.maxParticipants = max == null || max <= 0 ? null : max; }

    public TournamentMode getMode() { return mode; }
    public void setMode(TournamentMode mode) { this.mode = mode == null ? TournamentMode.NONE : mode; }

    /** Ergebnis einer Gruppenauslosung: je Gruppe die Entrant-IDs (Gruppenkopf zuerst). */
    private java.util.List<java.util.List<String>> groups = java.util.List.of();
    /** Gruppenphase: {@code true} = über die Gruppengröße, {@code false} = über die Anzahl der Gruppen. */
    private boolean groupsBySize = true;
    private int groupSize = 4;
    private int groupCount = 2;

    /** KO-Platzbelegung (leer, wenn nicht im KO-System ausgelost). */
    public java.util.List<String> getDraw() { return draw; }
    public java.util.List<java.util.List<String>> getGroups() { return groups; }
    public boolean isDrawn() { return !draw.isEmpty() || !groups.isEmpty() || !qualField.isEmpty() || !qualifiedDirect.isEmpty(); }
    public boolean isDrawSeeded() { return drawSeeded; }

    /** Setzt das Ergebnis einer KO-Auslosung (eine evtl. Gruppenauslosung entfällt); leere Liste = löschen. */
    public void setDraw(java.util.List<String> slots, boolean seeded) {
        this.draw = slots == null ? java.util.List.of() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(slots));
        this.groups = java.util.List.of();
        this.drawSeeded = seeded;
    }

    /** Setzt das Ergebnis einer Gruppenauslosung (eine evtl. KO-Auslosung entfällt). */
    public void setGroups(java.util.List<java.util.List<String>> groups, boolean seeded) {
        java.util.List<java.util.List<String>> copy = new java.util.ArrayList<>();
        if (groups != null) for (java.util.List<String> g : groups) copy.add(java.util.List.copyOf(g));
        this.groups = java.util.Collections.unmodifiableList(copy);
        this.draw = java.util.List.of();
        this.drawSeeded = seeded;
    }

    /** Nach der Auslosung als „bereit“ markiert (wird beim Turnierstart gestartet). */
    private boolean ready;
    /** Zeitpunkt des Klassenstarts; {@code null} = noch nicht gestartet. */
    private java.time.LocalDateTime startedAt;

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public java.time.LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(java.time.LocalDateTime t) { this.startedAt = t; }
    public boolean isStarted() { return startedAt != null; }

    // --- Gruppenphase: Platzierung und Folgephase -----------------------------

    private GroupStanding.Tiebreak tiebreak = GroupStanding.Tiebreak.SETS_POINTS;
    /** Was nach der Gruppenphase folgt: NONE, KNOCKOUT oder GROUPS. */
    private TournamentMode nextPhase = TournamentMode.NONE;
    /** Wie viele je Gruppe in die nächste Phase einziehen. */
    private int qualifiers = 2;
    /** Name der daraus erstellten Folgeklasse bzw. der Klasse, aus der diese hervorging. */
    private String followUpClass;
    private String sourceClass;

    public GroupStanding.Tiebreak getTiebreak() { return tiebreak; }
    public void setTiebreak(GroupStanding.Tiebreak t) { tiebreak = t == null ? GroupStanding.Tiebreak.SETS_POINTS : t; }
    public TournamentMode getNextPhase() { return nextPhase; }
    public void setNextPhase(TournamentMode m) { nextPhase = m == null ? TournamentMode.NONE : m; }
    public int getQualifiers() { return qualifiers; }
    public void setQualifiers(int q) { qualifiers = Math.max(1, q); }
    public String getFollowUpClass() { return followUpClass; }
    public void setFollowUpClass(String n) { followUpClass = n; }
    public String getSourceClass() { return sourceClass; }
    public void setSourceClass(String n) { sourceClass = n; }

    /** Phase angehalten: eingereihte Spiele gehen zurück auf „geplant“, laufende werden noch beendet. */
    private boolean paused;
    /** Klasse beendet (nur an der ersten Phase gesetzt). */
    private java.time.LocalDateTime finishedAt;

    /** Schweizer System: Anzahl Runden (0 = Vorschlag) und je Runde die ID mit Freilos ({@code null} = keins). */
    private int swissRounds;
    private java.util.List<String> swissByes = new java.util.ArrayList<>();

    public int getSwissRounds() { return swissRounds; }
    public void setSwissRounds(int r) { swissRounds = Math.max(0, r); }
    public java.util.List<String> getSwissByes() { return java.util.Collections.unmodifiableList(swissByes); }
    public void setSwissByes(java.util.List<String> byes) { swissByes = new java.util.ArrayList<>(byes == null ? java.util.List.of() : byes); }
    void addSwissBye(String id) { swissByes.add(id); }

    /** Art der Gruppensetzung (wenn „nach Setzung“): Schlange oder Stärkegruppen. */
    private GroupDraw.Seeding groupSeeding = GroupDraw.Seeding.SNAKE;
    /** Vereinsgleichheit bei der Auslosung beachten (Standard: ja). */
    private boolean avoidSameClub = true;

    public GroupDraw.Seeding getGroupSeeding() { return groupSeeding; }
    public void setGroupSeeding(GroupDraw.Seeding s) { groupSeeding = s == null || s == GroupDraw.Seeding.RANDOM ? GroupDraw.Seeding.SNAKE : s; }
    public boolean isAvoidSameClub() { return avoidSameClub; }
    public void setAvoidSameClub(boolean b) { avoidSameClub = b; }

    /** Parteien, die aufgegeben haben (werden nicht mehr gepaart). */
    private final java.util.Set<String> withdrawn = new java.util.LinkedHashSet<>();

    public java.util.Set<String> getWithdrawn() { return java.util.Collections.unmodifiableSet(withdrawn); }
    public void addWithdrawn(String id) { withdrawn.add(id); }
    public void removeWithdrawn(String id) { withdrawn.remove(id); }

    /** Qualifikationsmodus: direkt Qualifizierte je Gruppe, Plätze in der KO-Qualifikation je Gruppe, KO-Runden. */
    private int qualDirect;
    private int qualKo;
    private int qualRounds;
    /** Qualifikationsmodus (an der Phase): die direkt Qualifizierten in Rangfolge (Entrant-IDs). */
    private java.util.List<String> qualifiedDirect = new java.util.ArrayList<>();

    public int getQualDirect() { return qualDirect; }
    public int getQualKo() { return qualKo; }
    public int getQualRounds() { return qualRounds; }
    public void setQualification(int direct, int ko, int rounds) { qualDirect = Math.max(0, direct); qualKo = Math.max(0, ko); qualRounds = Math.max(0, rounds); }
    /** Qualifikation: Nachrücker je Gruppe direkt aus der Gruppenphase (ohne Spiel), z. B. Dritte + Vierte = 2. */
    private int qualReserve;
    public int getQualReserve() { return qualReserve; }
    public void setQualReserve(int n) { qualReserve = Math.max(0, n); }
    public java.util.List<String> getQualifiedDirect() { return java.util.Collections.unmodifiableList(qualifiedDirect); }
    public void setQualifiedDirect(java.util.List<String> ids) { qualifiedDirect = new java.util.ArrayList<>(ids == null ? java.util.List.of() : ids); }

    /** Qualifikationsmodus: Teilnehmende der KO-Qualifikation in Rangfolge (Entrant-ID → Gruppe aus Phase 1). */
    private java.util.LinkedHashMap<String, Integer> qualField = new java.util.LinkedHashMap<>();
    /** KO ab Phase 2: Kennung je Teilnehmer aus der Vorphase, z. B. „A1“ (Gruppe A, Platz 1). */
    private java.util.LinkedHashMap<String, String> seedLabels = new java.util.LinkedHashMap<>();
    public java.util.Map<String, String> getSeedLabels() { return java.util.Collections.unmodifiableMap(seedLabels); }
    public void setSeedLabels(java.util.Map<String, String> m) { seedLabels = new java.util.LinkedHashMap<>(m == null ? java.util.Map.of() : m); }
    /** Spiel um Platz 3 (nur KO-System, nicht KO-Qualifikation). */
    private boolean thirdPlace;
    public boolean isThirdPlace() { return thirdPlace; }
    public void setThirdPlace(boolean b) { thirdPlace = b; }
    /** Gewinnsätze je Spiel: 3 (Standard) oder 4. */
    private int bestOf = 3;
    public int getWinningSets() { return bestOf; }
    public void setWinningSets(int n) { bestOf = n == 4 ? 4 : 3; }

    public java.util.Map<String, Integer> getQualField() { return java.util.Collections.unmodifiableMap(qualField); }
    public void setQualField(java.util.Map<String, Integer> field) { qualField = new java.util.LinkedHashMap<>(field == null ? java.util.Map.of() : field); }
    /** Qualifikation: Gruppenplatz je Teilnehmer der KO-Qualifikation (Reihenfolge innerhalb einer Stufe). */
    private java.util.LinkedHashMap<String, Integer> qualPlace = new java.util.LinkedHashMap<>();
    public java.util.Map<String, Integer> getQualPlace() { return java.util.Collections.unmodifiableMap(qualPlace); }
    public void setQualPlace(java.util.Map<String, Integer> m) { qualPlace = new java.util.LinkedHashMap<>(m == null ? java.util.Map.of() : m); }
    /** Qualifikation: Zusatzrunde für die Verlierer der letzten Runde (bei Bedarf). */
    private boolean qualExtra;
    public boolean isQualExtra() { return qualExtra; }
    public void setQualExtra(boolean b) { qualExtra = b; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; }
    public java.time.LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(java.time.LocalDateTime t) { finishedAt = t; }
    public boolean isFinished() { return finishedAt != null; }
    /** {@code true} für Phase 2 und 3 (aus einer vorherigen Phase hervorgegangen). */
    public boolean isPhase() { return sourceClass != null; }

    public boolean isGroupsBySize() { return groupsBySize; }
    public void setGroupsBySize(boolean bySize) { this.groupsBySize = bySize; }
    public int getGroupSize() { return groupSize; }
    public void setGroupSize(int size) { this.groupSize = Math.max(2, size); }
    public int getGroupCount() { return groupCount; }
    public void setGroupCount(int count) { this.groupCount = Math.max(1, count); }

    public Discipline getDiscipline() { return discipline; }
    public void setDiscipline(Discipline d) { this.discipline = d == null ? Discipline.SINGLES : d; }
    public boolean isDoubles() { return discipline == Discipline.DOUBLES; }

    public ClassType getType() { return type; }
    public void setType(ClassType type) { this.type = type == null ? ClassType.ADULTS : type; }

    public Integer getBirthyearFrom() { return birthyearFrom; }
    public void setBirthyearFrom(Integer birthyearFrom) { this.birthyearFrom = birthyearFrom; }

    public Integer getBirthyearTo() { return birthyearTo; }
    public void setBirthyearTo(Integer birthyearTo) { this.birthyearTo = birthyearTo; }

    @Override
    public String toString() {
        return name;
    }
}
