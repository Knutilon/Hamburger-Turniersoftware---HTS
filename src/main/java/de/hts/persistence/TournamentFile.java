package de.hts.persistence;

import de.hts.player.Player;
import de.hts.player.Sex;
import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.TournamentMode;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Speichert und lädt eine HTS-Turnierdatei (Endung {@code .hts}, Inhalt: XML, UTF-8).
 *
 * <pre>{@code
 * <hts-tournament format-version="2" name="..." date="2026-11-14" location="...">
 *   <classes>
 *     <class name="Mädchen U15" type="girls" birthyear-from="2012"/>
 *     <class name="Herren A" type="adults"/>
 *   </classes>
 *   <players>
 *     <player firstname="..." lastname="..." birthyear="1990" birthdate="1990-03-14" sex="m"
 *             club-name="..." internal-nr="NU..." ttr="1500" ...>
 *       <entry competition="Herren Einzel"/>
 *       <waiting-list competition="Damen Einzel"/>
 *     </player>
 *   </players>
 * </hts-tournament>
 * }</pre>
 *
 * Gespeichert wird zuerst in eine temporäre Datei, die dann die alte ersetzt –
 * so bleibt bei einem Absturz während des Speicherns die vorherige Fassung erhalten.
 */
public final class TournamentFile {

    public static final String EXTENSION = "hts";
    /**
     * 1 = nur Teilnehmende (HTS 0.2), 2 = zusätzlich Turnierdaten und Klassen (HTS 0.3),
     * 3 = zusätzlich Turnier-ID und Startzeit (HTS 0.4),
     * 4 = zusätzlich Einzel/Doppel, Teilnehmenden-IDs und Doppelpaarungen (HTS 0.5/0.6),
     * 5 = zusätzlich Anmelde-Reihenfolge, Höchstzahl, Modus und Auslosung je Klasse (HTS 0.7/0.8),
     * 6 = zusätzlich Tische, Spiele mit Status/Ergebnis, Klassenstart (ab HTS 0.9).
     */
    static final int FORMAT_VERSION = 7;
    private static final String ROOT = "hts-tournament";

    /** Eine gespeicherte Doppelpaarung (über die IDs der Teilnehmenden). */
    public record PairData(String id, String className, String firstId, String secondId) {
    }

    static final String TABLE_NAME_KEY = "tname.";

    private static Map<String, String> readFolders(Element root) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        for (String k : List.of("folder", "dir-pdf", "dir-xml", "test", "tables-off", "pres-secs", "pres-off", "pres-on", "handicap", "hc")) {
            String v = root.getAttribute(k);
            if (v != null && !v.isBlank()) m.put(k, v);
        }
        org.w3c.dom.NamedNodeMap attrs = root.getAttributes();       // Tischnamen „tname.N“
        for (int i = 0; i < attrs.getLength(); i++) {
            org.w3c.dom.Node a = attrs.item(i);
            if (a.getNodeName().startsWith(TABLE_NAME_KEY)) m.put(a.getNodeName(), a.getNodeValue());
        }
        return m;
    }

    /** Inhalt einer Turnierdatei. */
    public record Content(String id, String tournamentName, LocalDate date, String location,
                          LocalDateTime startedAt, List<PlayingClass> classes, List<Player> players,
                          List<PairData> pairs, int tables, List<Match> matches, int winningSets,
                          Map<Integer, List<String>> tableClasses, Map<String, String> folders) {

        public Content(String id, String tournamentName, LocalDate date, String location,
                       LocalDateTime startedAt, List<PlayingClass> classes, List<Player> players,
                       List<PairData> pairs, int tables, List<Match> matches, int winningSets,
                       Map<Integer, List<String>> tableClasses) {
            this(id, tournamentName, date, location, startedAt, classes, players, pairs, tables, matches, winningSets, tableClasses, Map.of());
        }

        public Content(String id, String tournamentName, LocalDate date, String location,
                       LocalDateTime startedAt, List<PlayingClass> classes, List<Player> players,
                       List<PairData> pairs, int tables, List<Match> matches, int winningSets) {
            this(id, tournamentName, date, location, startedAt, classes, players, pairs, tables, matches, winningSets, Map.of());
        }

        public Content(String id, String tournamentName, LocalDate date, String location,
                       LocalDateTime startedAt, List<PlayingClass> classes, List<Player> players) {
            this(id, tournamentName, date, location, startedAt, classes, players, List.of(), 1, List.of(), 3);
        }

        public Content(String tournamentName, LocalDate date, String location,
                       List<PlayingClass> classes, List<Player> players) {
            this(null, tournamentName, date, location, null, classes, players);
        }

        public Content(String tournamentName, List<Player> players) {
            this(tournamentName, null, null, List.of(), players);
        }

        /** Momentaufnahme eines Turniers zum Speichern. */
        public static Content of(Tournament t) {
            List<PairData> pairs = t.getPairs().stream()
                    .map(p -> new PairData(p.getId(), p.getClassName(),
                            p.getFirst() == null ? null : p.getFirst().getId(),
                            p.getSecond() == null ? null : p.getSecond().getId()))
                    .toList();
            return new Content(t.getId(), t.getName(), t.getDate(), t.getLocation(), t.getStartedAt(),
                    new ArrayList<>(t.getClasses()), new ArrayList<>(t.getPlayers()), pairs,
                    t.getTableCount(), new ArrayList<>(t.getMatches()), t.getWinningSets(),
                    t.getTableClassMap().entrySet().stream().collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, e -> List.copyOf(e.getValue()))), folders(t));
        }

        private static Map<String, String> folders(Tournament t) {
            Map<String, String> m = new java.util.LinkedHashMap<>();
            if (t.getFolder() != null) m.put("folder", t.getFolder());
            if (t.getPdfDir() != null) m.put("dir-pdf", t.getPdfDir());
            if (t.getXmlDir() != null) m.put("dir-xml", t.getXmlDir());
            if (t.isTest()) m.put("test", "true");
            if (t.isHandicap()) {
                m.put("handicap", "true");
                m.put("hc", t.getHandicapRules().encode());
            }
            if (!t.getDisabledTables().isEmpty()) {
                m.put("tables-off", String.join(",", t.getDisabledTables().stream().map(String::valueOf).toList()));
            }
            t.getTableNames().forEach((nr, name) -> m.put(TABLE_NAME_KEY + nr, name));
            if (t.getPresentationSeconds() != 20) m.put("pres-secs", String.valueOf(t.getPresentationSeconds()));
            if (!t.getPresentationOff().isEmpty()) m.put("pres-off", String.join("|", t.getPresentationOff()));
            if (!t.getPresentationOn().isEmpty()) m.put("pres-on", String.join("|", t.getPresentationOn()));
            return m;
        }

        /** Baut daraus ein Turnier (ohne ungespeicherte Änderungen). */
        public Tournament toTournament() {
            Tournament t = new Tournament();
            t.replaceAll(tournamentName, date, location, classes, players);
            Map<String, Player> byId = new java.util.HashMap<>();
            players.forEach(p -> byId.put(p.getId(), p));
            for (PairData pd : pairs) {
                Player a = pd.firstId() == null ? null : byId.get(pd.firstId());
                Player b = pd.secondId() == null ? null : byId.get(pd.secondId());
                if (a == null && b == null) continue;
                if (a == null) { a = b; b = null; }
                t.restorePair(pd.id(), pd.className(), a, b);
            }
            t.ensurePairsForEntries();
            t.setTableCount(tables);
            t.setWinningSets(winningSets);
            if (tableClasses != null) tableClasses.forEach(t::setTableClasses);
            if (folders != null) {
                t.setFolder(folders.get("folder"));
                t.setPdfDir(folders.get("dir-pdf"));
                t.setXmlDir(folders.get("dir-xml"));
                t.setTest("true".equals(folders.get("test")));
                t.setHandicap("true".equals(folders.get("handicap")));
                if (folders.get("hc") != null) t.restoreHandicapRules(de.hts.tournament.HandicapRules.decode(folders.get("hc")));
                folders.forEach((k, v) -> {
                    if (!k.startsWith(TABLE_NAME_KEY)) return;
                    try {
                        t.setTableName(Integer.parseInt(k.substring(TABLE_NAME_KEY.length())), v);
                    } catch (RuntimeException ignored) {
                        // unlesbar: Standardname
                    }
                });
                try {
                    if (folders.get("pres-secs") != null) t.setPresentationSeconds(Integer.parseInt(folders.get("pres-secs").trim()));
                } catch (NumberFormatException ignored) {
                    // Standard 20 s
                }
                if (folders.get("pres-off") != null) {
                    for (String k : folders.get("pres-off").split("\\|")) if (!k.isBlank()) t.setPresentationOn(k, false);
                }
                if (folders.get("pres-on") != null) {
                    for (String k : folders.get("pres-on").split("\\|")) if (!k.isBlank()) t.setPresentationOn(k, true);
                }
                String off = folders.get("tables-off");
                if (off != null) {
                    for (String x : off.split(",")) {
                        try {
                            t.setTableDisabled(Integer.parseInt(x.trim()), true);
                        } catch (NumberFormatException ignored) {
                            // unlesbar: Tisch bleibt aktiv
                        }
                    }
                }
            }
            t.restoreMatches(matches);
            t.setId(id);
            t.setStartedAt(startedAt);
            t.markSaved();
            return t;
        }
    }

    private TournamentFile() {
    }

    // --- Speichern ---------------------------------------------------------

    public static void save(Path file, Tournament t) throws IOException {
        save(file, Content.of(t));
    }

    public static void save(Path file, String tournamentName, List<Player> players) throws IOException {
        save(file, new Content(tournamentName, players));
    }

    public static void save(Path file, Content content) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Path tmp = Files.createTempFile(dir, ".hts-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                write(out, content);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicNotSupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public static void write(OutputStream out, Content content) throws IOException {
        try {
            Document doc = newBuilder().newDocument();
            Element root = doc.createElement(ROOT);
            root.setAttribute("format-version", String.valueOf(FORMAT_VERSION));
            set(root, "id", content.id());
            set(root, "name", content.tournamentName());
            set(root, "date", content.date());
            set(root, "location", content.location());
            set(root, "started-at", content.startedAt());
            root.setAttribute("tables", String.valueOf(content.tables()));
            root.setAttribute("winning-sets", String.valueOf(content.winningSets()));
            if (content.folders() != null) content.folders().forEach(root::setAttribute);
            if (content.tableClasses() != null && !content.tableClasses().isEmpty()) {
                Element tc = doc.createElement("table-classes");
                for (Map.Entry<Integer, List<String>> e : new java.util.TreeMap<>(content.tableClasses()).entrySet()) {
                    Element te = doc.createElement("table");
                    te.setAttribute("nr", String.valueOf(e.getKey()));
                    for (String cn : e.getValue()) {
                        Element ce = doc.createElement("class");
                        ce.setAttribute("name", cn);
                        te.appendChild(ce);
                    }
                    tc.appendChild(te);
                }
                root.appendChild(tc);
            }
            doc.appendChild(root);

            Element classesEl = doc.createElement("classes");
            root.appendChild(classesEl);
            for (PlayingClass c : content.classes()) {
                Element el = doc.createElement("class");
                el.setAttribute("name", c.getName());
                el.setAttribute("type", typeCode(c.getType()));
                el.setAttribute("discipline", c.isDoubles() ? "doubles" : "singles");
                set(el, "birthyear-from", c.getBirthyearFrom());
                set(el, "birthyear-to", c.getBirthyearTo());
                set(el, "max", c.getMaxParticipants());
                if (c.isReady()) el.setAttribute("ready", "true");
                el.setAttribute("tiebreak", c.getTiebreak().name());
                el.setAttribute("next-phase", c.getNextPhase().name());
                el.setAttribute("qualifiers", String.valueOf(c.getQualifiers()));
                set(el, "follow-up", c.getFollowUpClass());
                set(el, "source", c.getSourceClass());
                set(el, "started-at", c.getStartedAt());
                if (c.isPaused()) el.setAttribute("paused", "true");
                if (!c.getWithdrawn().isEmpty()) el.setAttribute("withdrawn", String.join(",", c.getWithdrawn()));
                set(el, "finished-at", c.getFinishedAt());
                if (c.getMode() == TournamentMode.KNOCKOUT) el.setAttribute("mode", "knockout");
                if (c.getMode() == TournamentMode.GROUPS) el.setAttribute("mode", "groups");
                if (c.getMode() == TournamentMode.PLACEMENT) el.setAttribute("mode", "placement");
                if (c.getMode() == TournamentMode.SWISS) el.setAttribute("mode", "swiss");
                if (c.getMode() == TournamentMode.QUALIFICATION) el.setAttribute("mode", "qualification");
                if (c.getQualDirect() > 0 || c.getQualKo() > 0 || c.getQualRounds() > 0 || c.getQualReserve() > 0) {
                    el.setAttribute("qual", c.getQualDirect() + "," + c.getQualKo() + "," + c.getQualRounds() + "," + c.getQualReserve());
                }
                if (!c.getQualifiedDirect().isEmpty()) el.setAttribute("qualified-direct", String.join(",", c.getQualifiedDirect()));
                if (!c.getSeedLabels().isEmpty()) {
                    List<String> sl = new java.util.ArrayList<>();
                    c.getSeedLabels().forEach((id, l) -> sl.add(id + "=" + l));
                    el.setAttribute("seed-labels", String.join(",", sl));
                }
                if (c.isThirdPlace()) el.setAttribute("third-place", "true");
                if (c.getWinningSets() != 3) el.setAttribute("winning-sets", String.valueOf(c.getWinningSets()));
                if (!c.getQualPlace().isEmpty()) {
                    List<String> qp = new java.util.ArrayList<>();
                    c.getQualPlace().forEach((id, pl) -> qp.add(id + "=" + pl));
                    el.setAttribute("qual-place", String.join(",", qp));
                }
                if (c.isQualExtra()) el.setAttribute("qual-extra", "true");
                if (!c.getQualField().isEmpty()) {
                    List<String> qf = new java.util.ArrayList<>();
                    c.getQualField().forEach((id, g) -> qf.add(id + "=" + g));
                    el.setAttribute("qual-field", String.join(",", qf));
                }
                el.setAttribute("group-seeding", c.getGroupSeeding().name());
                el.setAttribute("avoid-same-club", String.valueOf(c.isAvoidSameClub()));
                if (c.getLotSeed() != 0) el.setAttribute("lot-seed", Long.toString(c.getLotSeed()));
                if (c.getSwissRounds() > 0) el.setAttribute("swiss-rounds", String.valueOf(c.getSwissRounds()));
                if (!c.getSwissByes().isEmpty()) {
                    el.setAttribute("swiss-byes", String.join(",", c.getSwissByes().stream().map(b -> b == null ? "-" : b).toList()));
                }
                el.setAttribute("groups-by", c.isGroupsBySize() ? "size" : "count");
                el.setAttribute("group-size", String.valueOf(c.getGroupSize()));
                el.setAttribute("group-count", String.valueOf(c.getGroupCount()));
                if (!c.getGroups().isEmpty()) {
                    Element gd = doc.createElement("group-draw");
                    gd.setAttribute("seeded", String.valueOf(c.isDrawSeeded()));
                    for (List<String> g : c.getGroups()) {
                        Element ge = doc.createElement("group");
                        for (String id : g) {
                            Element m = doc.createElement("member");
                            m.setAttribute("ref", id);
                            ge.appendChild(m);
                        }
                        gd.appendChild(ge);
                    }
                    el.appendChild(gd);
                }
                if (!c.getDraw().isEmpty()) {
                    Element drawEl = doc.createElement("draw");
                    drawEl.setAttribute("seeded", String.valueOf(c.isDrawSeeded()));
                    for (String id : c.getDraw()) {
                        Element slot = doc.createElement("slot");
                        if (id == null) slot.setAttribute("bye", "true");
                        else slot.setAttribute("ref", id);
                        drawEl.appendChild(slot);
                    }
                    el.appendChild(drawEl);
                }
                classesEl.appendChild(el);
            }

            Element playersEl = doc.createElement("players");
            root.appendChild(playersEl);
            for (Player p : content.players()) {
                playersEl.appendChild(toElement(doc, p));
            }

            Element matchesEl = doc.createElement("matches");
            root.appendChild(matchesEl);
            for (Match m : content.matches()) {
                Element el = doc.createElement("match");
                el.setAttribute("id", m.getId());
                el.setAttribute("class", m.getClassName());
                el.setAttribute("stage", m.getStage());
                el.setAttribute("round", String.valueOf(m.getRound()));
                el.setAttribute("number", String.valueOf(m.getNumber()));
                el.setAttribute("ko-round", String.valueOf(m.getKoRound()));
                el.setAttribute("ko-index", String.valueOf(m.getKoIndex()));
                el.setAttribute("group", String.valueOf(m.getGroupIndex()));
                set(el, "a", m.getA());
                set(el, "b", m.getB());
                el.setAttribute("status", m.getStatus().name());
                set(el, "table", m.getTable());
                el.setAttribute("queue", String.valueOf(m.getQueueOrder()));
                set(el, "sets-a", m.getSetsA());
                set(el, "sets-b", m.getSetsB());
                if (!m.getPoints().isEmpty()) el.setAttribute("points", de.hts.tournament.Scoring.encode(m.getPoints()));
                if (m.getRetired() != 0) el.setAttribute("retired", String.valueOf(m.getRetired()));
                set(el, "started-at", m.getStartedAt());
                set(el, "finished-at", m.getFinishedAt());
                if (m.getPlacement() > 0) el.setAttribute("placement", String.valueOf(m.getPlacement()));
                if (m.isCarried()) el.setAttribute("carried", "true");
                if (m.isExportExcluded()) el.setAttribute("export", "false");
                set(el, "retirement-of", m.getRetirementOf());
                matchesEl.appendChild(el);
            }

            Element pairsEl = doc.createElement("pairs");
            root.appendChild(pairsEl);
            for (PairData pd : content.pairs()) {
                Element el = doc.createElement("pair");
                el.setAttribute("id", pd.id());
                el.setAttribute("class", pd.className());
                set(el, "first", pd.firstId());
                set(el, "second", pd.secondId());
                pairsEl.appendChild(el);
            }

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            t.transform(new DOMSource(doc), new StreamResult(out));
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("Turnierdatei konnte nicht geschrieben werden: " + e.getMessage(), e);
        }
    }

    private static Element toElement(Document doc, Player p) {
        Element el = doc.createElement("player");
        el.setAttribute("id", p.getId());
        set(el, "firstname", p.getFirstname());
        set(el, "lastname", p.getLastname());
        set(el, "birthyear", p.getBirthyear());
        set(el, "birthdate", p.getBirthdate());
        if (p.getSex() != Sex.UNKNOWN) {
            set(el, "sex", p.getSex() == Sex.MALE ? "m" : "w");
        }
        set(el, "club-name", p.getClubName());
        set(el, "club-nr", p.getClubNr());
        set(el, "club-federation", p.getClubFederation());
        set(el, "internal-nr", p.getInternalNr());
        set(el, "licence-nr", p.getLicenceNr());
        set(el, "ttr", p.getTtr());
        set(el, "ttr-match-count", p.getTtrMatchCount());
        set(el, "nationality", p.getNationality());
        set(el, "region", p.getRegion());
        for (String c : p.getClassNames()) {
            Element entry = doc.createElement("entry");
            entry.setAttribute("competition", c);
            entry.setAttribute("order", String.valueOf(p.getClassOrder(c)));
            el.appendChild(entry);
        }
        for (String c : p.getWaitingLists()) {
            Element wl = doc.createElement("waiting-list");
            wl.setAttribute("competition", c);
            el.appendChild(wl);
        }
        for (String c : p.getRegistrations()) {
            Element rg = doc.createElement("registration");
            rg.setAttribute("competition", c);
            el.appendChild(rg);
        }
        return el;
    }

    // --- Laden -------------------------------------------------------------

    public static Content load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in);
        }
    }

    public static Content read(InputStream in) throws IOException {
        Document doc;
        try {
            doc = newBuilder().parse(in);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Die Datei ist keine gültige HTS-Turnierdatei: " + e.getMessage(), e);
        }
        Element root = doc.getDocumentElement();
        if (!ROOT.equals(root.getTagName())) {
            throw new IOException("Die Datei ist keine HTS-Turnierdatei (Wurzelelement <" + root.getTagName() + ">).");
        }
        Integer version = intAttr(root, "format-version");
        if (version != null && version > FORMAT_VERSION) {
            throw new IOException("Die Datei stammt aus einer neueren HTS-Version (Format " + version
                    + "). Bitte HTS aktualisieren.");
        }

        List<PlayingClass> classes = new ArrayList<>();
        NodeList classList = root.getElementsByTagName("class");
        for (int i = 0; i < classList.getLength(); i++) {
            Element el = (Element) classList.item(i);
            String name = attr(el, "name");
            if (name == null) continue;
            ClassType type = parseType(attr(el, "type"));
            if (type == null) {
                // Dateien aus HTS 0.3.0 hatten statt "type" ein "sex": weiblich -> Damen, sonst Erwachsene
                type = "w".equals(attr(el, "sex")) ? ClassType.WOMEN : ClassType.ADULTS;
            }
            Discipline discipline = "doubles".equals(attr(el, "discipline")) ? Discipline.DOUBLES : Discipline.SINGLES;
            PlayingClass pc = new PlayingClass(name, discipline, type, intAttr(el, "birthyear-from"), intAttr(el, "birthyear-to"));
            pc.setMaxParticipants(intAttr(el, "max"));
            pc.setReady("true".equals(attr(el, "ready")));
            try {
                if (attr(el, "tiebreak") != null) pc.setTiebreak(de.hts.tournament.GroupStanding.Tiebreak.valueOf(attr(el, "tiebreak")));
                if (attr(el, "next-phase") != null) pc.setNextPhase(TournamentMode.valueOf(attr(el, "next-phase")));
            } catch (IllegalArgumentException ignored) {
                // unbekannter Wert: Standard behalten
            }
            pc.setQualifiers(intOr(el, "qualifiers", 2));
            pc.setFollowUpClass(attr(el, "follow-up"));
            pc.setSourceClass(attr(el, "source"));
            pc.setStartedAt(dateTime(attr(el, "started-at")));
            pc.setPaused("true".equals(attr(el, "paused")));
            if (attr(el, "withdrawn") != null) for (String w : attr(el, "withdrawn").split(",")) if (!w.isBlank()) pc.addWithdrawn(w);
            pc.setFinishedAt(dateTime(attr(el, "finished-at")));
            if ("knockout".equals(attr(el, "mode"))) pc.setMode(TournamentMode.KNOCKOUT);
            if ("groups".equals(attr(el, "mode"))) pc.setMode(TournamentMode.GROUPS);
            if ("placement".equals(attr(el, "mode"))) pc.setMode(TournamentMode.PLACEMENT);
            if ("swiss".equals(attr(el, "mode"))) pc.setMode(TournamentMode.SWISS);
            if ("qualification".equals(attr(el, "mode"))) pc.setMode(TournamentMode.QUALIFICATION);
            if (attr(el, "qual") != null) {
                String[] q = attr(el, "qual").split(",");
                if (q.length >= 3) pc.setQualification(Integer.parseInt(q[0].trim()), Integer.parseInt(q[1].trim()), Integer.parseInt(q[2].trim()));
                if (q.length >= 4) pc.setQualReserve(Integer.parseInt(q[3].trim()));
            }
            if (attr(el, "qualified-direct") != null && !attr(el, "qualified-direct").isBlank()) {
                pc.setQualifiedDirect(List.of(attr(el, "qualified-direct").split(",")));
            }
            if (attr(el, "seed-labels") != null && !attr(el, "seed-labels").isBlank()) {
                java.util.LinkedHashMap<String, String> sl = new java.util.LinkedHashMap<>();
                for (String e : attr(el, "seed-labels").split(",")) {
                    int k = e.lastIndexOf('=');
                    if (k > 0) sl.put(e.substring(0, k), e.substring(k + 1));
                }
                pc.setSeedLabels(sl);
            }
            pc.setThirdPlace("true".equals(attr(el, "third-place")));
            if (attr(el, "winning-sets") != null) pc.setWinningSets(Integer.parseInt(attr(el, "winning-sets").trim()));
            if (attr(el, "qual-place") != null && !attr(el, "qual-place").isBlank()) {
                java.util.LinkedHashMap<String, Integer> qp = new java.util.LinkedHashMap<>();
                for (String e : attr(el, "qual-place").split(",")) {
                    int k = e.lastIndexOf('=');
                    if (k > 0) qp.put(e.substring(0, k), Integer.parseInt(e.substring(k + 1).trim()));
                }
                pc.setQualPlace(qp);
            }
            pc.setQualExtra("true".equals(attr(el, "qual-extra")));
            if (attr(el, "qual-field") != null && !attr(el, "qual-field").isBlank()) {
                java.util.LinkedHashMap<String, Integer> qf = new java.util.LinkedHashMap<>();
                for (String e : attr(el, "qual-field").split(",")) {
                    int k = e.lastIndexOf('=');
                    if (k > 0) qf.put(e.substring(0, k), Integer.parseInt(e.substring(k + 1).trim()));
                }
                pc.setQualField(qf);
            }
            try {
                if (attr(el, "group-seeding") != null) pc.setGroupSeeding(de.hts.tournament.GroupDraw.Seeding.valueOf(attr(el, "group-seeding")));
            } catch (IllegalArgumentException ignored) {
                // unbekannt -> Standard
            }
            pc.setAvoidSameClub(!"false".equals(attr(el, "avoid-same-club")));
            if (attr(el, "lot-seed") != null) {
                try {
                    pc.setLotSeed(Long.parseLong(attr(el, "lot-seed")));
                } catch (NumberFormatException ignored) {
                    // unbekannt -> Ersatzlos aus dem Klassennamen
                }
            }
            pc.setSwissRounds(intOr(el, "swiss-rounds", 0));
            String byes = attr(el, "swiss-byes");
            if (byes != null && !byes.isEmpty()) {
                List<String> list = new ArrayList<>();
                for (String b : byes.split(",", -1)) list.add("-".equals(b) ? null : b);
                pc.setSwissByes(list);
            }
            pc.setGroupsBySize(!"count".equals(attr(el, "groups-by")));
            if (intAttr(el, "group-size") != null) pc.setGroupSize(intAttr(el, "group-size"));
            if (intAttr(el, "group-count") != null) pc.setGroupCount(intAttr(el, "group-count"));
            NodeList gds = el.getElementsByTagName("group-draw");
            if (gds.getLength() > 0) {
                Element gd = (Element) gds.item(0);
                List<List<String>> groups = new ArrayList<>();
                NodeList ges = gd.getElementsByTagName("group");
                for (int k = 0; k < ges.getLength(); k++) {
                    List<String> members = new ArrayList<>();
                    NodeList ms = ((Element) ges.item(k)).getElementsByTagName("member");
                    for (int m = 0; m < ms.getLength(); m++) members.add(((Element) ms.item(m)).getAttribute("ref"));
                    groups.add(members);
                }
                pc.setGroups(groups, "true".equals(gd.getAttribute("seeded")));
            }
            NodeList draws = el.getElementsByTagName("draw");
            if (draws.getLength() > 0) {
                Element drawEl = (Element) draws.item(0);
                List<String> slots = new ArrayList<>();
                NodeList slotEls = drawEl.getElementsByTagName("slot");
                for (int k = 0; k < slotEls.getLength(); k++) {
                    Element slot = (Element) slotEls.item(k);
                    slots.add("true".equals(slot.getAttribute("bye")) ? null : attr(slot, "ref"));
                }
                pc.setDraw(slots, "true".equals(drawEl.getAttribute("seeded")));
            }
            classes.add(pc);
        }

        List<Player> players = new ArrayList<>();
        NodeList list = root.getElementsByTagName("player");
        for (int i = 0; i < list.getLength(); i++) {
            players.add(fromElement((Element) list.item(i)));
        }
        LocalDate date = null;
        String d = attr(root, "date");
        if (d != null) {
            try {
                date = LocalDate.parse(d);
            } catch (DateTimeParseException ignored) {
                // ungültiges Datum überspringen
            }
        }
        LocalDateTime startedAt = null;
        String s = attr(root, "started-at");
        if (s != null) {
            try {
                startedAt = LocalDateTime.parse(s);
            } catch (DateTimeParseException ignored) {
                // ungültige Startzeit überspringen
            }
        }
        List<PairData> pairs = new ArrayList<>();
        NodeList pairList = root.getElementsByTagName("pair");
        for (int i = 0; i < pairList.getLength(); i++) {
            Element el = (Element) pairList.item(i);
            if (attr(el, "class") != null) {
                pairs.add(new PairData(attr(el, "id"), attr(el, "class"), attr(el, "first"), attr(el, "second")));
            }
        }
        List<Match> matches = new ArrayList<>();
        NodeList matchList = root.getElementsByTagName("match");
        for (int i = 0; i < matchList.getLength(); i++) {
            Element el = (Element) matchList.item(i);
            Match m = new Match(attr(el, "id"), attr(el, "class"), attr(el, "stage"), intOr(el, "round", 1),
                    intOr(el, "number", i + 1), intOr(el, "ko-round", -1), intOr(el, "ko-index", -1), intOr(el, "group", -1),
                    attr(el, "a"), attr(el, "b"));
            MatchStatus st;
            try {
                st = MatchStatus.valueOf(attr(el, "status"));
            } catch (RuntimeException e) {
                st = MatchStatus.PLANNED;
            }
            String q = attr(el, "queue");
            m.restore(st, intAttr(el, "table"), q == null ? 0 : Long.parseLong(q), intAttr(el, "sets-a"), intAttr(el, "sets-b"),
                    dateTime(attr(el, "started-at")), dateTime(attr(el, "finished-at")),
                    de.hts.tournament.Scoring.decode(attr(el, "points")), intOr(el, "retired", 0));
            m.restorePhaseInfo(intOr(el, "placement", 0), "true".equals(attr(el, "carried")));
            m.restoreExport("false".equals(attr(el, "export")), attr(el, "retirement-of"));
            matches.add(m);
        }
        Integer tables = intAttr(root, "tables");
        Map<Integer, List<String>> tableClasses = new java.util.TreeMap<>();
        NodeList tcs = root.getElementsByTagName("table-classes");
        if (tcs.getLength() > 0) {
            NodeList tl = ((Element) tcs.item(0)).getElementsByTagName("table");
            for (int i = 0; i < tl.getLength(); i++) {
                Element te = (Element) tl.item(i);
                Integer nr = intAttr(te, "nr");
                if (nr == null) continue;
                List<String> names = new ArrayList<>();
                NodeList cl = te.getElementsByTagName("class");
                for (int k = 0; k < cl.getLength(); k++) names.add(((Element) cl.item(k)).getAttribute("name"));
                if (!names.isEmpty()) tableClasses.put(nr, names);
            }
        }
        return new Content(attr(root, "id"), attr(root, "name"), date, attr(root, "location"), startedAt,
                classes, players, pairs, tables == null ? 1 : tables, matches, intOr(root, "winning-sets", 3), tableClasses, readFolders(root));
    }

    private static Player fromElement(Element el) {
        Player p = new Player();
        if (attr(el, "id") != null) p.setId(attr(el, "id"));
        p.setFirstname(attr(el, "firstname"));
        p.setLastname(attr(el, "lastname"));
        p.setBirthyear(intAttr(el, "birthyear"));
        String bd = attr(el, "birthdate");
        if (bd != null) {
            try {
                p.setBirthdate(LocalDate.parse(bd));
            } catch (DateTimeParseException ignored) {
                // ungültiges Datum überspringen
            }
        }
        String sex = attr(el, "sex");
        p.setSex("m".equals(sex) ? Sex.MALE : "w".equals(sex) ? Sex.FEMALE : Sex.UNKNOWN);
        p.setClubName(attr(el, "club-name"));
        p.setClubNr(attr(el, "club-nr"));
        p.setClubFederation(attr(el, "club-federation"));
        p.setInternalNr(attr(el, "internal-nr"));
        p.setLicenceNr(attr(el, "licence-nr"));
        p.setTtr(intAttr(el, "ttr"));
        p.setTtrMatchCount(intAttr(el, "ttr-match-count"));
        p.setNationality(attr(el, "nationality"));
        p.setRegion(attr(el, "region"));

        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.hasAttribute("competition")) {
                String c = e.getAttribute("competition");
                if ("registration".equals(e.getTagName())) {
                    p.addRegistration(c);
                } else if ("waiting-list".equals(e.getTagName())
                        || ("entry".equals(e.getTagName()) && "false".equalsIgnoreCase(e.getAttribute("registered")))) {
                    // ältere Dateien: registered="false" = Warteliste = nicht in der Klasse
                    p.addWaitingList(c);
                } else if ("entry".equals(e.getTagName())) {
                    String order = e.getAttribute("order");
                    if (order != null && order.matches("\\d+")) p.addToClass(c, Long.parseLong(order));
                    else p.addToClass(c);
                }
            }
        }
        return p;
    }

    // --- Hilfsmethoden -----------------------------------------------------

    private static int intOr(Element el, String name, int fallback) {
        Integer v = intAttr(el, name);
        return v == null ? fallback : v;
    }

    private static LocalDateTime dateTime(String s) {
        if (s == null) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String typeCode(ClassType t) {
        return switch (t) {
            case ADULTS -> "adults";
            case WOMEN -> "women";
            case YOUTH -> "youth";
            case GIRLS -> "girls";
        };
    }

    private static ClassType parseType(String s) {
        if (s == null) return null;
        return switch (s) {
            case "adults" -> ClassType.ADULTS;
            case "women" -> ClassType.WOMEN;
            case "youth" -> ClassType.YOUTH;
            case "girls" -> ClassType.GIRLS;
            default -> null;
        };
    }

    private static DocumentBuilder newBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        DocumentBuilder b = f.newDocumentBuilder();
        b.setErrorHandler(null);
        return b;
    }

    private static void set(Element el, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            el.setAttribute(name, value.toString());
        }
    }

    private static String attr(Element el, String name) {
        if (!el.hasAttribute(name)) return null;
        String v = el.getAttribute(name).trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer intAttr(Element el, String name) {
        String v = attr(el, name);
        if (v == null) return null;
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
