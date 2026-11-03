package de.hts.importer.clicktt;

import de.hts.player.Player;
import de.hts.tournament.DoublesPair;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ergebnisdatei für den click-TT-Upload – wie in poinTT („Export/Import → Click-TT → Ergebnisse exportieren“):
 * Die ursprüngliche click-TT-Turnierdatei (die importierte XML) wird übernommen und je Konkurrenz um die Spiele
 * ({@code <matches><match nr group scheduled player-a player-b set-a-1 … set-b-7 sets-a sets-b matches-a matches-b
 * games-a games-b/></matches>}) und die Platzierung je Teilnehmer ({@code placement}) ergänzt.
 * Exportiert werden nur beendete Klassen und nur Spiele, die nicht als „nicht relevant für Export“ markiert sind.
 * Gibt jemand auf, wird das Spiel mit der Aufgabe mit 0:3 (Sätze 0:11) übertragen; die danach kampflos gewerteten
 * Spiele nicht.
 */
public final class ClickTtResultExporter {

    public static final String DOCTYPE_SYSTEM = "https://liga.nu/dtd/TournamentPortal.dtd";
    private static final DateTimeFormatter SCHEDULED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Ergebnis des Exports. */
    public record Report(int competitions, int matches, int placements, List<String> skipped, List<String> warnings) {
    }

    private ClickTtResultExporter() {
    }

    public static Report export(Tournament t, Path original, Path target) throws IOException {
        try (InputStream in = Files.newInputStream(original); OutputStream out = Files.newOutputStream(target)) {
            return export(t, in, out);
        }
    }

    public static Report export(Tournament t, InputStream original, OutputStream out) throws IOException {
        Document doc = parse(original);
        List<String> skipped = new ArrayList<>(), warnings = new ArrayList<>();
        int comps = 0, matchCount = 0, placements = 0;
        NodeList compEls = doc.getElementsByTagName("competition");
        for (int i = 0; i < compEls.getLength(); i++) {
            Element comp = (Element) compEls.item(i);
            String name = ClickTtXmlImporter.competitionName(comp);
            // click-TT-Teilnehmer: ID → Personen, Personenschlüssel → ID
            Map<Set<String>, String> idByPersons = new HashMap<>();
            Map<String, Element> playerEls = new HashMap<>();
            for (Element pe : children(childOf(comp, "players"), "player")) {
                Set<String> persons = new TreeSet<>();
                for (Element person : children(pe, "person")) {
                    Player local = findLocal(t, person);
                    if (local != null) persons.add(local.getId());
                }
                String id = pe.getAttribute("id");
                if (!persons.isEmpty() && !id.isEmpty()) {
                    idByPersons.put(persons, id);
                    playerEls.put(id, pe);
                }
            }
            PlayingClass c = findClass(t, name, idByPersons.keySet());
            if (c == null) {
                skipped.add(name + " – keine passende Klasse in HTS");
                continue;
            }
            if (!c.isFinished()) {
                skipped.add(name + " – Klasse „" + c.getName() + "“ ist noch nicht beendet");
                continue;
            }
            comps++;
            // Spiele aller Phasen (übernommene Kopien nur einmal, kampflose nicht)
            Element matchesEl = childOf(comp, "matches");
            if (matchesEl != null) comp.removeChild(matchesEl);
            matchesEl = doc.createElement("matches");
            int nr = 0;
            for (PlayingClass phase : t.phasesOf(c)) {
                String prefix = t.phasesOf(c).size() > 1 ? "Phase " + t.phaseNumber(phase) + " " : "";
                for (Match m : t.matchesOf(phase)) {
                    // nicht exportiert: offene, übernommene, „nicht relevant für Export“ markierte und die nach einer
                    // Aufgabe kampflos gewerteten Spiele; das Spiel mit der Aufgabe selbst wird mit 0:3 / 0:11 übertragen
                    if (!m.isExported()) continue;
                    String a = idByPersons.get(persons(t, m.getA())), b = idByPersons.get(persons(t, m.getB()));
                    if (a == null || b == null) {
                        warnings.add(name + ": Spiel " + m.getNumber() + " – Teilnehmende ohne click-TT-ID, nicht übertragen.");
                        continue;
                    }
                    Element me = doc.createElement("match");
                    me.setAttribute("nr", String.valueOf(++nr));
                    me.setAttribute("group", prefix + m.getStage());
                    if (m.getStartedAt() != null) me.setAttribute("scheduled", SCHEDULED.format(m.getStartedAt()));
                    else if (m.getFinishedAt() != null) me.setAttribute("scheduled", SCHEDULED.format(m.getFinishedAt()));
                    me.setAttribute("player-a", a);
                    me.setAttribute("player-b", b);
                    List<int[]> pts = m.getPoints();
                    boolean aWon = m.getA().equals(m.winner());
                    for (int k = 0; k < 7; k++) me.setAttribute("set-a-" + (k + 1), String.valueOf(k < pts.size() ? pts.get(k)[0] : 0));
                    me.setAttribute("sets-a", String.valueOf(m.getSetsA()));
                    me.setAttribute("matches-a", aWon ? "1" : "0");
                    me.setAttribute("games-a", String.valueOf(m.pointsA()));
                    for (int k = 0; k < 7; k++) me.setAttribute("set-b-" + (k + 1), String.valueOf(k < pts.size() ? pts.get(k)[1] : 0));
                    me.setAttribute("sets-b", String.valueOf(m.getSetsB()));
                    me.setAttribute("matches-b", aWon ? "0" : "1");
                    me.setAttribute("games-b", String.valueOf(m.pointsB()));
                    matchesEl.appendChild(me);
                    matchCount++;
                }
            }
            Element playersEl = childOf(comp, "players");
            if (playersEl != null && playersEl.getNextSibling() != null) comp.insertBefore(matchesEl, playersEl.getNextSibling());
            else comp.appendChild(matchesEl);
            // Platzierungen
            for (Tournament.Placing p : t.finalResult(c).rows()) {
                String id = idByPersons.get(persons(t, p.entrant().id()));
                if (id == null) continue;
                playerEls.get(id).setAttribute("placement", String.valueOf(p.from()));
                placements++;
            }
        }
        write(doc, out);
        return new Report(comps, matchCount, placements, skipped, warnings);
    }

    /** Personen-IDs einer Partei (Einzel: eine, Doppel: beide). */
    private static Set<String> persons(Tournament t, String entrantId) {
        Set<String> set = new TreeSet<>();
        if (entrantId == null) return set;
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair p : t.getPairs()) {
                if (p.getId().equals(pid)) {
                    if (p.getFirst() != null) set.add(p.getFirst().getId());
                    if (p.getSecond() != null) set.add(p.getSecond().getId());
                }
            }
        } else {
            set.add(entrantId);
        }
        return set;
    }

    /** Klasse über den Namen, sonst die mit den meisten gemeinsamen Teilnehmenden (mind. die Hälfte). */
    private static PlayingClass findClass(Tournament t, String name, Set<Set<String>> entries) {
        for (PlayingClass c : t.rootClasses()) if (c.getName().equals(name)) return c;
        PlayingClass best = null;
        int bestHits = 0;
        for (PlayingClass c : t.rootClasses()) {
            Set<Set<String>> mine = new HashSet<>();
            for (var e : t.entrantsOf(c)) mine.add(persons(t, e.id()));
            int hits = 0;
            for (Set<String> s : entries) if (mine.contains(s)) hits++;
            if (hits > bestHits) {
                bestHits = hits;
                best = c;
            }
        }
        return bestHits * 2 >= Math.max(1, entries.size()) ? best : null;
    }

    private static Player findLocal(Tournament t, Element person) {
        String internal = person.getAttribute("internal-nr"), licence = person.getAttribute("licence-nr");
        for (Player p : t.getPlayers()) if (!internal.isBlank() && internal.trim().equals(p.getInternalNr())) return p;
        for (Player p : t.getPlayers()) if (!licence.isBlank() && licence.trim().equals(p.getLicenceNr())) return p;
        String fn = person.getAttribute("firstname").trim(), ln = person.getAttribute("lastname").trim(), by = person.getAttribute("birthyear").trim();
        for (Player p : t.getPlayers()) {
            if (fn.equalsIgnoreCase(nz(p.getFirstname())) && ln.equalsIgnoreCase(nz(p.getLastname()))
                    && (by.isEmpty() || p.getBirthyear() == null || by.equals(String.valueOf(p.getBirthyear())))) return p;
        }
        return null;
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static Element childOf(Element parent, String tag) {
        if (parent == null) return null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(tag)) return e;
        }
        return null;
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> list = new ArrayList<>();
        if (parent == null) return list;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(tag)) list.add(e);
        }
        return list;
    }

    private static Document parse(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setValidating(false);
            f.setNamespaceAware(false);
            f.setExpandEntityReferences(false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(in);
        } catch (Exception e) {
            throw new IOException("Die click-TT-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        }
    }

    private static void stripWhitespace(Node n) {
        for (Node c = n.getFirstChild(); c != null; ) {
            Node next = c.getNextSibling();
            if (c.getNodeType() == Node.TEXT_NODE && c.getTextContent().isBlank()) n.removeChild(c);
            else stripWhitespace(c);
            c = next;
        }
    }

    private static void write(Document doc, OutputStream out) throws IOException {
        stripWhitespace(doc.getDocumentElement());
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer tr = tf.newTransformer();
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DOCTYPE_SYSTEM);
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tr.transform(new DOMSource(doc), new StreamResult(out));
        } catch (Exception e) {
            throw new IOException("Die Ergebnisdatei konnte nicht geschrieben werden: " + e.getMessage(), e);
        }
    }
}
