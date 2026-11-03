package de.hts.importer.clicktt;

import de.hts.player.Player;
import de.hts.player.Sex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liest Spieler aus einer click-TT/nuLiga-Turnier-XML-Datei.
 *
 * <p>Erwarteter Aufbau (alle Daten stehen in Attributen):
 * <pre>{@code
 * <tournament name="..." start-date="..." ...>
 *   <competition age-group="Herren" type="Einzel" ...>
 *     <players>
 *       <player type="single" id="PLAYER1">
 *         <person firstname="Max" lastname="Muster" birthyear="1990" sex="1"
 *                 club-name="..." club-nr="..." internal-nr="NU..." licence-nr="..." ttr="1500" .../>
 *       </player>
 *     </players>
 *   </competition>
 * </tournament>
 * }</pre>
 *
 * <p>Der Import ist tolerant: Es werden alle {@code <person>}-Elemente im Dokument gelesen,
 * egal wie tief sie verschachtelt sind. Bei Doppeln (zwei Personen pro {@code <player>})
 * werden beide Personen als Spieler übernommen. Personen, die in mehreren Konkurrenzen
 * gemeldet sind, erscheinen nur einmal; die Meldungen werden gesammelt.
 *
 * <p>Personen auf der Warteliste ({@code <player waiting-list-player="true">}) werden ebenfalls
 * übernommen, aber nicht in die Klasse eingetragen (nur als Wartelisten-Hinweis).
 */
public class ClickTtXmlImporter {

    /**
     * Zusammenfassung einer Konkurrenz aus der Datei.
     * {@code entries} zählt alle Meldungen inkl. Warteliste, {@code waitingList} nur die Warteliste,
     * {@code sex} ist das in der Datei angegebene Geschlecht der Konkurrenz ({@link Sex#UNKNOWN} = offen/keine Angabe).
     */
    public record CompetitionInfo(String name, int entries, int waitingList, Sex sex, boolean doubles) {
    }

    /** Eine Doppelmeldung aus der Datei: zwei Personen (über {@link Player#identityKey()}). */
    public record ImportedPair(String competition, String firstKey, String secondKey, boolean waitingList) {
    }

    /** Ergebnis des Einlesens. */
    public record ImportResult(String tournamentName,
                               List<Player> players,
                               List<CompetitionInfo> competitions,
                               List<ImportedPair> pairs,
                               List<String> warnings) {
    }

    public ImportResult importFile(Path file) throws ClickTtImportException {
        try (InputStream in = Files.newInputStream(file)) {
            return importStream(in);
        } catch (IOException e) {
            throw new ClickTtImportException("Die Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        }
    }

    public ImportResult importStream(InputStream in) throws ClickTtImportException {
        Document doc = parse(in);
        Element root = doc.getDocumentElement();

        String tournamentName = "tournament".equals(root.getTagName()) ? attr(root, "name") : null;

        Map<String, Player> playersByKey = new LinkedHashMap<>();
        List<CompetitionInfo> competitions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        NodeList persons = doc.getElementsByTagName("person");
        Map<Element, Integer> entriesPerCompetition = new LinkedHashMap<>();
        Map<Element, Integer> waitingPerCompetition = new LinkedHashMap<>();
        Map<Element, String> firstKeyOfPlayerEl = new java.util.HashMap<>();
        List<ImportedPair> pairs = new ArrayList<>();

        for (int i = 0; i < persons.getLength(); i++) {
            Element personEl = (Element) persons.item(i);
            Player player = toPlayer(personEl);

            if (isBlank(player.getLastname()) && isBlank(player.getFirstname())) {
                warnings.add("Person ohne Namen übersprungen (Eintrag " + (i + 1) + ").");
                continue;
            }

            Element competitionEl = findAncestor(personEl, "competition");
            Element playerEl = findAncestor(personEl, "player");
            boolean waitingList = playerEl != null && isTrue(attr(playerEl, "waiting-list-player"));
            if (competitionEl != null) {
                if (waitingList) {
                    player.addWaitingList(competitionName(competitionEl));
                } else {
                    player.addToClass(competitionName(competitionEl));
                }
            }

            // Meldungen je Konkurrenz zählen (ein Doppel = eine Meldung)
            if (competitionEl != null && playerEl != null && firstPersonOf(playerEl) == personEl) {
                entriesPerCompetition.merge(competitionEl, 1, Integer::sum);
                if (waitingList) {
                    waitingPerCompetition.merge(competitionEl, 1, Integer::sum);
                }
            }

            // Doppel: die zweite Person eines <player> bildet mit der ersten eine Paarung
            if (competitionEl != null && playerEl != null) {
                if (firstPersonOf(playerEl) == personEl) {
                    firstKeyOfPlayerEl.put(playerEl, player.identityKey());
                } else if (firstKeyOfPlayerEl.containsKey(playerEl)) {
                    pairs.add(new ImportedPair(competitionName(competitionEl), firstKeyOfPlayerEl.get(playerEl),
                            player.identityKey(), waitingList));
                }
            }

            Player existing = playersByKey.get(player.identityKey());
            if (existing == null) {
                playersByKey.put(player.identityKey(), player);
            } else {
                existing.updateFrom(player);
            }
        }

        NodeList competitionEls = doc.getElementsByTagName("competition");
        for (int i = 0; i < competitionEls.getLength(); i++) {
            Element c = (Element) competitionEls.item(i);
            competitions.add(new CompetitionInfo(competitionName(c), entriesPerCompetition.getOrDefault(c, 0),
                    waitingPerCompetition.getOrDefault(c, 0), competitionSex(attr(c, "sex")), isDoubles(c)));
        }

        if (playersByKey.isEmpty()) {
            throw new ClickTtImportException(
                    "In der Datei wurden keine Teilnehmenden gefunden. Erwartet wird ein click-TT-Turnierexport "
                            + "mit <person>-Einträgen.");
        }

        return new ImportResult(tournamentName, new ArrayList<>(playersByKey.values()), competitions, pairs, warnings);
    }

    // ---------------------------------------------------------------------

    static Document parse(InputStream in) throws ClickTtImportException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            // click-TT-Dateien verweisen per DOCTYPE auf eine DTD im Internet.
            // Die wird nicht geladen (offline-fähig und Schutz vor XXE).
            f.setValidating(false);
            f.setNamespaceAware(false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null); // keine Ausgaben auf stderr
            return b.parse(in);
        } catch (SAXParseException e) {
            throw new ClickTtImportException("Die Datei ist kein gültiges XML (Zeile " + e.getLineNumber()
                    + "): " + e.getMessage(), e);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new ClickTtImportException("Die XML-Datei konnte nicht verarbeitet werden: " + e.getMessage(), e);
        }
    }

    static Player toPlayer(Element el) {
        Player p = new Player();
        p.setFirstname(trim(attr(el, "firstname")));
        p.setLastname(trim(attr(el, "lastname")));
        p.setBirthyear(intAttr(el, "birthyear"));
        p.setBirthdate(dateAttr(el, "birthdate", "birth-date", "date-of-birth"));
        p.setSex(Sex.fromClickTt(intAttr(el, "sex")));
        p.setClubName(trim(attr(el, "club-name")));
        p.setClubNr(trim(attr(el, "club-nr")));
        p.setClubFederation(trim(attr(el, "club-federation-nickname")));
        p.setInternalNr(trim(attr(el, "internal-nr")));
        p.setLicenceNr(trim(attr(el, "licence-nr")));
        p.setTtr(intAttr(el, "ttr"));
        p.setTtrMatchCount(intAttr(el, "ttr-match-count"));
        p.setNationality(trim(attr(el, "nationality")));
        p.setRegion(trim(attr(el, "region")));
        return p;
    }

    /** Lesbarer Name einer Konkurrenz, z. B. "Herren Einzel" oder "Damen Doppel (TTR bis 1400)". */
    static String competitionName(Element c) {
        StringBuilder sb = new StringBuilder();
        append(sb, attr(c, "age-group"));
        append(sb, attr(c, "type"));
        Integer ttrTo = intAttrOf(c, "ttr-to");
        if (ttrTo != null && ttrTo > 0 && ttrTo < 3000) {
            sb.append(" (TTR bis ").append(ttrTo).append(')');
        }
        if (sb.isEmpty()) {
            String id = attr(c, "competition-id");
            if (id == null) id = attr(c, "id");
            sb.append(id != null ? "Konkurrenz " + id : "Konkurrenz");
        }
        return sb.toString();
    }

    static Element firstPersonOf(Element playerEl) {
        for (Node n = playerEl.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && "person".equals(e.getTagName())) {
                return e;
            }
        }
        return null;
    }

    private static Element findAncestor(Element el, String tag) {
        for (Node n = el.getParentNode(); n != null; n = n.getParentNode()) {
            if (n instanceof Element e && tag.equals(e.getTagName())) {
                return e;
            }
        }
        return null;
    }

    private static void append(StringBuilder sb, String s) {
        if (s != null && !s.isBlank()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(s.trim());
        }
    }

    static String attr(Element el, String name) {
        return el.hasAttribute(name) ? el.getAttribute(name) : null;
    }

    private static Integer intAttr(Element el, String name) {
        return intAttrOf(el, name);
    }

    private static Integer intAttrOf(Element el, String name) {
        String v = attr(el, name);
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Liest ein Datum im Format JJJJ-MM-TT oder TT.MM.JJJJ aus dem ersten vorhandenen Attribut. */
    private static LocalDate dateAttr(Element el, String... names) {
        for (String name : names) {
            String v = attr(el, name);
            if (v == null || v.isBlank()) continue;
            v = v.trim();
            try {
                return v.contains(".") ? LocalDate.parse(v, Player.GERMAN_DATE) : LocalDate.parse(v.substring(0, Math.min(10, v.length())));
            } catch (DateTimeParseException e) {
                // ungültiges Datum ignorieren, Jahrgang bleibt erhalten
            }
        }
        return null;
    }

    /** Doppelkonkurrenz? Erkennbar an type="Doppel"/"double" oder an Meldungen mit zwei Personen. */
    static boolean isDoubles(Element competition) {
        String type = attr(competition, "type");
        if (type != null) {
            String t = type.toLowerCase(java.util.Locale.ROOT);
            if (t.contains("doppel") || t.contains("double") || t.contains("mixed")) return true;
            if (t.contains("einzel") || t.contains("single")) return false;
        }
        NodeList players = competition.getElementsByTagName("player");
        for (int i = 0; i < players.getLength(); i++) {
            Element p = (Element) players.item(i);
            if ("double".equalsIgnoreCase(attr(p, "type")) || p.getElementsByTagName("person").getLength() >= 2) {
                return true;
            }
        }
        return false;
    }

    /** Geschlecht einer Konkurrenz; nur eindeutige Angaben werden übernommen ("mixed" o. Ä. = offen). */
    static Sex competitionSex(String s) {
        if (s == null) return Sex.UNKNOWN;
        return switch (s.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "male", "m", "1", "männlich", "maennlich" -> Sex.MALE;
            case "female", "w", "f", "2", "weiblich" -> Sex.FEMALE;
            default -> Sex.UNKNOWN;
        };
    }

    private static boolean isTrue(String s) {
        return s != null && (s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("yes"));
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
