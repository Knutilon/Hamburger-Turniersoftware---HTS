package de.hts.persistence;

import de.hts.tournament.Tournament;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Ablage aller Turniere. Standard: {@code Dokumente\HTS Turniere\<Turniername>\Turnierdatei\<Turniername>.hts}
 * (siehe {@link StorageLocations}). Turnierdateien an anderen Orten (beim Anlegen per „Durchsuchen“ gewählt) merkt
 * sich die Ablage in den Benutzereinstellungen. Dateien der alten Ablage {@code <Benutzerordner>\HTS\Turniere}
 * werden beim ersten Auflisten in die neue Ordnerstruktur übernommen.
 */
public final class TournamentStore {

    /** Kurzinfo eines Turniers für die Übersicht (ohne alle Teilnehmenden laden zu müssen). */
    public record Summary(String id, String name, LocalDate date, LocalDateTime startedAt,
                          int participants, int classes, Path file, int tables, boolean test, boolean handicap) {

        public Summary(String id, String name, LocalDate date, LocalDateTime startedAt,
                       int participants, int classes, Path file, int tables, boolean test) {
            this(id, name, date, startedAt, participants, classes, file, tables, test, false);
        }

        public Summary(String id, String name, LocalDate date, LocalDateTime startedAt,
                       int participants, int classes, Path file, int tables) {
            this(id, name, date, startedAt, participants, classes, file, tables, false, false);
        }

        public boolean isStarted() {
            return startedAt != null;
        }
    }

    /** Eine Datei, die nicht gelesen werden konnte. */
    public record Problem(Path file, String message) {
    }

    private static final String PREF_EXTRA = "extraTournamentFiles";

    private final Path dir;
    private final Path legacyDir;
    private final java.util.prefs.Preferences prefs;
    private final List<Problem> problems = new ArrayList<>();
    /** Turnier-ID → Datei (aus dem letzten Auflisten bzw. Speichern). */
    private final java.util.Map<String, Path> files = new java.util.HashMap<>();

    public TournamentStore(Path dir) {
        this(dir, null, null);
    }

    TournamentStore(Path dir, Path legacyDir, java.util.prefs.Preferences prefs) {
        this.dir = dir;
        this.legacyDir = legacyDir;
        this.prefs = prefs;
        StorageLocations.setRoot(dir);
    }

    public static TournamentStore defaultStore() {
        String custom = System.getProperty("hts.dataDir");
        if (custom != null && !custom.isBlank()) return new TournamentStore(Path.of(custom));
        return new TournamentStore(StorageLocations.defaultRoot(), Path.of(System.getProperty("user.home"), "HTS", "Turniere"),
                java.util.prefs.Preferences.userNodeForPackage(TournamentStore.class));
    }

    /** Stammordner „HTS Turniere“. */
    public Path getDirectory() {
        return dir;
    }

    /** Datei eines Turniers (bekannt oder Standardort). */
    public Path fileOf(Tournament t) {
        Path f = t.getId() == null ? null : files.get(t.getId());
        return f != null ? f : defaultFile(t);
    }

    private Path defaultFile(Tournament t) {
        return StorageLocations.tournamentFolder(t).resolve(StorageLocations.FILE_DIR)
                .resolve(StorageLocations.safeName(t.getName()) + "." + TournamentFile.EXTENSION);
    }

    /** Alle Turniere, neuestes Datum zuerst. Nicht lesbare Dateien landen in {@link #getProblems()}. */
    public List<Summary> list() throws IOException {
        problems.clear();
        migrateLegacy();
        java.util.LinkedHashSet<Path> candidates = new java.util.LinkedHashSet<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> top = Files.list(dir)) {
                for (Path p : top.toList()) {
                    if (isHts(p)) candidates.add(p);
                    Path sub = p.resolve(StorageLocations.FILE_DIR);
                    if (Files.isDirectory(sub)) {
                        try (Stream<Path> fs = Files.list(sub)) {
                            fs.filter(TournamentStore::isHts).forEach(candidates::add);
                        }
                    }
                }
            }
        }
        for (Path p : extraFiles()) if (isHts(p)) candidates.add(p);
        files.clear();
        List<Summary> result = new ArrayList<>();
        for (Path f : candidates) {
            try {
                TournamentFile.Content c = TournamentFile.load(f);
                String id = isValidId(c.id()) ? c.id() : isValidId(idFromFile(f)) && f.getParent().equals(dir) ? idFromFile(f) : null;
                if (id != null && files.containsKey(id)) id = null;           // Kopie einer Datei: neue ID beim Öffnen
                if (id != null) files.put(id, f);
                result.add(new Summary(id, c.tournamentName(), c.date(), c.startedAt(),
                        c.players().size(), (int) c.classes().stream().filter(x -> !x.isPhase()).count(), f, c.tables(),
                        c.folders() != null && "true".equals(c.folders().get("test")),
                        c.folders() != null && "true".equals(c.folders().get("handicap"))));
            } catch (IOException e) {
                problems.add(new Problem(f, e.getMessage()));
            }
        }
        result.sort(Comparator.comparing(Summary::date, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(s -> s.name() == null ? "" : s.name(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static boolean isHts(Path p) {
        return Files.isRegularFile(p) && p.getFileName().toString().endsWith("." + TournamentFile.EXTENSION);
    }

    public List<Problem> getProblems() {
        return List.copyOf(problems);
    }

    /** Legt ein neues Turnier an und speichert es sofort. */
    public Tournament create(String name, LocalDate date) throws IOException {
        return create(name, date, 1);
    }

    /** Legt ein neues Turnier mit Anzahl Spieltische an und speichert es sofort. */
    public Tournament create(String name, LocalDate date, int tables) throws IOException {
        return create(name, date, tables, null);
    }

    /**
     * Legt ein neues Turnier an. Der Turnierordner ist „HTS Turniere\&lt;Name&gt;“ (bei Namensgleichheit mit Zusatz);
     * {@code fileDir} = abweichender Ordner für die Turnierdatei ({@code null} = „Turnierdatei“ im Turnierordner).
     */
    public Tournament create(String name, LocalDate date, int tables, Path fileDir) throws IOException {
        return create(name, date, tables, fileDir, false);
    }

    public Tournament create(String name, LocalDate date, int tables, Path fileDir, boolean test) throws IOException {
        return create(name, date, tables, fileDir, test, false);
    }

    /** @param handicap Vorgabeturnier (nur beim Anlegen festlegbar) */
    public Tournament create(String name, LocalDate date, int tables, Path fileDir, boolean test, boolean handicap) throws IOException {
        Tournament t = new Tournament();
        t.setTest(test);
        t.setHandicap(handicap);
        t.setTableCount(tables);
        t.setId(newId(date));
        t.setName(name);
        t.setDate(date);
        t.setFolder(uniqueFolder(name).toString());
        if (fileDir != null) {
            Path f = fileDir.resolve(StorageLocations.safeName(name) + "." + TournamentFile.EXTENSION);
            if (Files.exists(f)) f = fileDir.resolve(StorageLocations.safeName(name) + " " + t.getId() + "." + TournamentFile.EXTENSION);
            files.put(t.getId(), f);
            register(f);
        }
        save(t);
        return t;
    }

    /** Freier Turnierordner: „Name“, sonst „Name (2)“, „Name (3)“ … */
    private Path uniqueFolder(String name) {
        String base = StorageLocations.safeName(name);
        Path p = dir.resolve(base);
        for (int k = 2; Files.exists(p) || folderInUse(p); k++) p = dir.resolve(base + " (" + k + ")");
        return p;
    }

    private boolean folderInUse(Path p) {
        for (Path f : files.values()) if (f.startsWith(p)) return true;
        return false;
    }

    public Tournament load(String id) throws IOException {
        Path f = files.get(id);
        if (f == null) {
            list();
            f = files.get(id);
        }
        if (f == null) throw new IOException("Turnier " + id + " nicht gefunden.");
        Tournament t = TournamentFile.load(f).toTournament();
        t.setId(id);
        if (t.getFolder() == null && f.getParent() != null && f.getParent().getFileName() != null
                && f.getParent().getFileName().toString().equals(StorageLocations.FILE_DIR)) {
            t.setFolder(f.getParent().getParent().toString());
        }
        t.markSaved();
        return t;
    }

    /**
     * Lädt das Turnier zu einem Eintrag der Übersicht. Hat die Datei keine (eindeutige) ID – etwa eine Kopie, die
     * jemand in den Ordner gelegt hat –, wird sie als neues Turnier in die Ordnerstruktur übernommen.
     */
    public Tournament load(Summary s) throws IOException {
        if (s.id() != null && s.file().equals(files.get(s.id()))) {
            return load(s.id());
        }
        Tournament t = TournamentFile.load(s.file()).toTournament();
        t.setId(newId(t.getDate()));
        if (t.getFolder() == null || folderInUse(Path.of(t.getFolder()))) t.setFolder(uniqueFolder(t.getName()).toString());
        save(t);
        if (s.file().getParent().equals(dir)) Files.deleteIfExists(s.file());   // lose Datei im Stammordner
        return t;
    }

    /** Speichert das Turnier in seine Datei; danach gilt es als ungeändert. */
    public void save(Tournament t) throws IOException {
        if (t.getId() == null) {
            t.setId(newId(t.getDate()));
        }
        if (t.getFolder() == null) t.setFolder(uniqueFolder(t.getName()).toString());
        Path f = fileOf(t);
        Files.createDirectories(f.getParent());
        TournamentFile.save(f, t);
        files.put(t.getId(), f);
        t.markSaved();
    }

    /** Turnierdatei in einen anderen Ordner verschieben (per „Durchsuchen“ gewählt). */
    public void moveFile(Tournament t, Path newDir) throws IOException {
        Path old = fileOf(t);
        Path target = newDir.resolve(old.getFileName());
        if (target.equals(old)) return;
        if (Files.exists(target)) throw new IOException("Im gewählten Ordner gibt es schon „" + target.getFileName() + "“.");
        Files.createDirectories(newDir);
        files.put(t.getId(), target);
        TournamentFile.save(target, t);
        t.markSaved();
        Files.deleteIfExists(old);
        unregister(old);
        if (!isInStructure(target)) register(target);
    }

    private boolean isInStructure(Path f) {
        return f.getParent() != null && f.getParent().getParent() != null
                && f.getParent().getParent().getParent() != null && f.getParent().getParent().getParent().equals(dir)
                && f.getParent().getFileName().toString().equals(StorageLocations.FILE_DIR);
    }

    public void delete(Summary s) throws IOException {
        Files.deleteIfExists(s.file());
        if (s.id() != null) files.remove(s.id());
        unregister(s.file());
    }

    /**
     * Übernimmt eine vorhandene Turnierdatei (z. B. aus HTS 0.2/0.3) als neues Turnier in die Ablage.
     * Die Originaldatei bleibt unverändert.
     */
    public Tournament importFile(Path source) throws IOException {
        Tournament t = TournamentFile.load(source).toTournament();
        t.setId(newId(t.getDate()));
        if (t.getName() == null || t.getName().isBlank()) {
            String fn = source.getFileName().toString();
            t.setName(fn.endsWith("." + TournamentFile.EXTENSION) ? fn.substring(0, fn.length() - 4) : fn);
        }
        t.setFolder(uniqueFolder(t.getName()).toString());
        save(t);
        return t;
    }

    /** Fertiges (importiertes) Turnier in die Ablage übernehmen: neue ID, eigener Turnierordner. */
    public Tournament importFinished(Tournament t) throws IOException {
        t.setId(newId(t.getDate()));
        t.setFolder(uniqueFolder(t.getName()).toString());
        save(t);
        return t;
    }

    /** Was beim Kopieren eines Turniers übernommen wird. */
    public enum CopyMode {
        WITHOUT_DRAW("Ohne Auslosung – Teilnehmende, Klassen, Zuordnungen, Einstellungen, Tische"),
        EVERYTHING("Alles – inkl. Auslosungen, Spiele und Ergebnisse"),
        PLAYERS_ONLY("Nur Teilnehmende – ohne Klassen");

        private final String label;

        CopyMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Legt ein neues Turnier als Kopie eines bestehenden an (eigener Turnierordner, neue ID). */
    public Tournament copy(Summary source, CopyMode mode, String name, LocalDate date, int tables, boolean test, Path fileDir)
            throws IOException {
        Tournament t = TournamentFile.load(source.file()).toTournament();
        Path sourceFile = source.file();
        t.setId(newId(date));
        t.setName(name);
        t.setDate(date);
        t.setTableCount(tables);
        t.setTest(test);
        t.setPdfDir(null);
        t.setXmlDir(null);
        t.setFolder(uniqueFolder(name).toString());
        switch (mode == null ? CopyMode.WITHOUT_DRAW : mode) {
            case WITHOUT_DRAW -> t.resetToBeforeDraw();
            case PLAYERS_ONLY -> t.removeAllClasses();
            case EVERYTHING -> { }
        }
        if (fileDir != null) {
            Path f = fileDir.resolve(StorageLocations.safeName(name) + "." + TournamentFile.EXTENSION);
            if (Files.exists(f)) f = fileDir.resolve(StorageLocations.safeName(name) + " " + t.getId() + "." + TournamentFile.EXTENSION);
            files.put(t.getId(), f);
            register(f);
        }
        save(t);
        // Kopie der click-TT-Importdatei mitnehmen (Grundlage des Ergebnisexports)
        if (mode != CopyMode.PLAYERS_ONLY && source.name() != null) {
            Path ct = sourceFile.resolveSibling(StorageLocations.safeName(source.name()) + ".clicktt.xml");
            if (Files.isRegularFile(ct)) {
                Files.copy(ct, fileOf(t).resolveSibling(StorageLocations.safeName(name) + ".clicktt.xml"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return t;
    }

    // --- Turnierdateien außerhalb der Ordnerstruktur -----------------------------

    private List<Path> extraFiles() {
        if (prefs == null) return List.of();
        List<Path> out = new ArrayList<>();
        for (String s : prefs.get(PREF_EXTRA, "").split("\n")) if (!s.isBlank()) out.add(Path.of(s));
        return out;
    }

    private void register(Path f) {
        if (prefs == null) return;
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (Path p : extraFiles()) set.add(p.toString());
        set.add(f.toAbsolutePath().toString());
        prefs.put(PREF_EXTRA, String.join("\n", set));
    }

    private void unregister(Path f) {
        if (prefs == null) return;
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (Path p : extraFiles()) set.add(p.toString());
        set.remove(f.toAbsolutePath().toString());
        prefs.put(PREF_EXTRA, String.join("\n", set));
    }

    /** Alte Ablage (Benutzerordner\HTS\Turniere\&lt;id&gt;.hts) einmalig in die neue Ordnerstruktur übernehmen. */
    private void migrateLegacy() {
        if (legacyDir == null || !Files.isDirectory(legacyDir) || legacyDir.equals(dir)) return;
        List<Path> old;
        try (Stream<Path> fs = Files.list(legacyDir)) {
            old = fs.filter(TournamentStore::isHts).toList();
        } catch (IOException e) {
            return;
        }
        for (Path f : old) {
            try {
                Tournament t = TournamentFile.load(f).toTournament();
                String id = idFromFile(f);
                t.setId(isValidId(id) ? id : newId(t.getDate()));
                t.setFolder(uniqueFolder(t.getName()).toString());
                save(t);
                Path clicktt = legacyDir.resolve(id + ".clicktt.xml");
                if (Files.isRegularFile(clicktt)) {
                    Files.copy(clicktt, fileOf(t).resolveSibling(StorageLocations.safeName(t.getName()) + ".clicktt.xml"),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(f, f.resolveSibling(f.getFileName() + ".uebernommen"));
            } catch (IOException | RuntimeException e) {
                problems.add(new Problem(f, "Übernahme in „" + StorageLocations.ROOT_NAME + "“ fehlgeschlagen: " + e.getMessage()));
            }
        }
    }

    private static boolean isValidId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]+");
    }

    private static String idFromFile(Path f) {
        String n = f.getFileName().toString();
        return n.substring(0, n.length() - TournamentFile.EXTENSION.length() - 1);
    }

    /** z. B. "2026-11-14_3f9a2c1b" – nach Datum sortierbar und eindeutig. */
    private static String newId(LocalDate date) {
        String prefix = date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "ohne-datum";
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
