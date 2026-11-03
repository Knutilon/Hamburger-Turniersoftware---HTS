package de.hts.importer.clicktt;

import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.Match;
import de.hts.tournament.MatchStatus;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ClickTtResultExporterTest {

    @Test
    void ergebnisseWieInPoinTT() throws Exception {
        ClickTtXmlImporter.ImportResult r;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            r = new ClickTtXmlImporter().importStream(in);
        }
        Tournament t = new Tournament();
        t.setTableCount(10);
        for (var ci : r.competitions()) {
            t.addClass(new PlayingClass(ci.name(), Discipline.SINGLES, ClassType.guess(ci.name(), ci.sex()), null, null));
        }
        t.merge(r.players());
        PlayingClass m15 = t.findClass("Mädchen U15 Einzel").orElseThrow();
        m15.setGroupsBySize(true);
        m15.setGroupSize(4);
        t.drawGroups(m15, true, new Random(1));
        t.startClass(m15);
        boolean first = true;
        for (int g = 0; g < 300; g++) {
            Match m = t.matchesOf(m15).stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE || x.getStatus() == MatchStatus.QUEUED)
                    .findFirst().orElse(null);
            if (m == null) break;
            if (first) {
                t.recordResult(m, List.of(new int[]{11, 8}, new int[]{9, 11}, new int[]{11, 5}, new int[]{14, 12}), 0);
                first = false;
            } else {
                t.recordResult(m, 3, 1);
            }
        }
        t.finishClass(m15);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ClickTtResultExporter.Report rep;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            rep = ClickTtResultExporter.export(t, in, out);
        }
        assertEquals(1, rep.competitions(), "nur die beendete Klasse");
        assertEquals(24, rep.matches());
        assertEquals(16, rep.placements());
        assertTrue(rep.skipped().stream().anyMatch(s -> s.startsWith("Jungen U19 Einzel")));

        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("TournamentPortal.dtd"), xml.substring(0, 300));
        var f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(out.toByteArray()));
        Element comp = (Element) doc.getElementsByTagName("competition").item(0);
        NodeList matches = comp.getElementsByTagName("match");
        assertEquals(24, matches.getLength());
        Element m1 = (Element) matches.item(0);
        assertEquals("1", m1.getAttribute("nr"));
        assertTrue(m1.getAttribute("group").startsWith("Gruppe"));
        assertTrue(m1.getAttribute("player-a").startsWith("PLAYER") || !m1.getAttribute("player-a").isEmpty());
        assertEquals("11", m1.getAttribute("set-a-1"));
        assertEquals("8", m1.getAttribute("set-b-1"));
        assertEquals("14", m1.getAttribute("set-a-4"));
        assertEquals("0", m1.getAttribute("set-a-5"));
        assertEquals("3", m1.getAttribute("sets-a"));
        assertEquals("1", m1.getAttribute("matches-a"));
        assertEquals(String.valueOf(11 + 9 + 11 + 14), m1.getAttribute("games-a"));
        assertTrue(m1.getAttribute("scheduled").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
        // Platzierungen der gemeldeten Spielerinnen; Warteliste ohne
        NodeList players = comp.getElementsByTagName("player");
        int placed = 0;
        for (int i = 0; i < players.getLength(); i++) {
            Element p = (Element) players.item(i);
            if (!p.getAttribute("placement").isEmpty()) placed++;
            if ("true".equals(p.getAttribute("waiting-list-player"))) assertTrue(p.getAttribute("placement").isEmpty());
        }
        assertEquals(16, placed);
        // <matches> steht hinter <players>
        org.w3c.dom.Node prev = comp.getElementsByTagName("matches").item(0).getPreviousSibling();
        while (prev != null && !(prev instanceof Element)) prev = prev.getPreviousSibling();
        assertEquals("players", ((Element) prev).getTagName());
    }

    @Test
    void aufgabeUndNichtRelevanteSpiele() throws Exception {
        ClickTtXmlImporter.ImportResult r;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            r = new ClickTtXmlImporter().importStream(in);
        }
        Tournament t = new Tournament();
        t.setTableCount(1);
        for (var ci : r.competitions()) {
            t.addClass(new PlayingClass(ci.name(), Discipline.SINGLES, ClassType.guess(ci.name(), ci.sex()), null, null));
        }
        t.merge(r.players());
        PlayingClass m15 = t.findClass("Mädchen U15 Einzel").orElseThrow();
        m15.setGroupsBySize(true);
        m15.setGroupSize(4);
        t.drawGroups(m15, true, new Random(1));
        t.startClass(m15);
        // Spiel 1: A gibt auf
        Match first = t.activeMatchAt(1).orElseThrow();
        String quitter = first.getA();
        t.recordResult(first, List.of(), 1);
        assertEquals("Aufgabe A", first.getSetsText());
        List<Match> walkovers = t.matchesOf(m15).stream().filter(x -> quitter.equals(x.getRetirementOf())).toList();
        assertEquals(2, walkovers.size(), "die übrigen beiden Gruppenspiele werden kampflos gewertet");
        for (Match w : walkovers) {
            assertEquals(MatchStatus.DONE, w.getStatus());
            assertNotEquals(quitter, w.winner());
            assertFalse(w.isExported());
        }
        assertTrue(first.isExported(), "das Spiel mit der Aufgabe wird exportiert");
        // Korrektur: doch keine Aufgabe -> kampflose Wertungen werden zurückgenommen
        t.correctResult(first, List.of(new int[]{11, 3}, new int[]{11, 4}, new int[]{11, 5}), 0);
        assertTrue(t.matchesOf(m15).stream().noneMatch(x -> quitter.equals(x.getRetirementOf())));
        // erneut aufgeben lassen
        t.correctResult(first, List.of(), 1);
        assertEquals(2, t.matchesOf(m15).stream().filter(x -> quitter.equals(x.getRetirementOf())).count());
        for (int g = 0; g < 300; g++) {
            Match m = t.matchesOf(m15).stream().filter(x -> x.getStatus() == MatchStatus.ACTIVE || x.getStatus() == MatchStatus.QUEUED)
                    .findFirst().orElse(null);
            if (m == null) break;
            t.recordResult(m, 3, 1);
        }
        Match excluded = t.matchesOf(m15).stream().filter(Match::isExported).filter(x -> x != first).findFirst().orElseThrow();
        excluded.setExportExcluded(true);
        t.finishClass(m15);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ClickTtResultExporter.Report rep;
        try (InputStream in = getClass().getResourceAsStream("/testturnier-jugend.xml")) {
            rep = ClickTtResultExporter.export(t, in, out);
        }
        assertEquals(24 - 2 - 1, rep.matches(), "24 Spiele – 2 kampflos nach Aufgabe – 1 nicht relevant");
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("set-a-1=\"0\"") && xml.contains("set-b-1=\"11\""), "Aufgabe als 0:11 exportiert");
    }
}
