/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.importer.clicktt;

import de.hts.player.Player;
import de.hts.tournament.ClassType;
import de.hts.tournament.Discipline;
import de.hts.tournament.DoublesPair;
import de.hts.tournament.Match;
import de.hts.tournament.PlayingClass;
import de.hts.tournament.Tournament;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importiert ein FERTIGES Turnier aus einer click-TT-Ergebnisdatei (Format {@code TournamentPortal.dtd}, wie von
 * click-TT, poinTT oder HTS exportiert): Teilnehmende, je Konkurrenz eine Klasse, Phase 1 als Gruppenphase
 * („Gruppe 1“, „Gruppe A“ …) oder KO-System („Viertelfinale“, „Halbfinale“, „Finale“, „Spiel um Platz 3“ …), alle
 * Spiele mit Sätzen. Das Turnier gilt danach als abgeschlossen. Was nicht zugeordnet werden kann, wird als Hinweis
 * gemeldet statt abzubrechen.
 */
public final class ClickTtResultImporter {

    /** Ergebnis: das neue Turnier und Hinweise zum Import. */
    public record Result(Tournament tournament, List<String> notes, int classes, int matches) {
    }

    private static final Pattern GROUP = Pattern.compile("(?i)^gruppe\\s*(\\S+)$");
    private static final Pattern PHASE = Pattern.compile("(?i)^phase\\s*(\\d+)\\s*[·:\\-–]?\\s*(.*)$");
    private static final DateTimeFormatter SCHEDULED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Eine Begegnung aus der Datei. */
    private record XmlMatch(int nr, int phase, String stage, String a, String b, List<int[]> sets, int setsA, int setsB,
                            int winner, LocalDateTime when) {
    }

    public Result importFile(Path file) throws ClickTtImportException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ClickTtImportException("Die Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        }
        return importBytes(bytes);
    }

    public Result importBytes(byte[] bytes) throws ClickTtImportException {
        ClickTtXmlImporter.ImportResult base = new ClickTtXmlImporter().importStream(new ByteArrayInputStream(bytes));
        Element root = ClickTtXmlImporter.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
        List<String> notes = new ArrayList<>(base.warnings());

        Tournament t = new Tournament();
        String name = ClickTtXmlImporter.attr(root, "name");
        t.setName(name == null || name.isBlank() ? "Importiertes Turnier" : name.trim());
        t.setDate(date(ClickTtXmlImporter.attr(root, "start-date")));
        t.setLocation(locationOf(root));
        Integer tables = intOf(ClickTtXmlImporter.attr(root, "table-count"));
        t.setTableCount(tables == null ? 1 : tables);
        int winningSets = intOf(ClickTtXmlImporter.attr(root, "winning-sets")) == null ? 3 : intOf(ClickTtXmlImporter.attr(root, "winning-sets"));

        // Teilnehmende: erst ohne Klassen übernehmen – in eine Klasse kommt nur, wer dort gespielt hat
        for (Player p : base.players()) {
            for (String cn : new ArrayList<>(p.getClassNames())) p.removeFromClass(cn);
        }
        t.merge(base.players());
        Map<String, Player> byKey = new HashMap<>();
        for (Player p : t.getPlayers()) byKey.put(p.identityKey(), p);

        int classCount = 0, matchCount = 0;
        NodeList comps = root.getElementsByTagName("competition");
        for (int ci = 0; ci < comps.getLength(); ci++) {
            Element comp = (Element) comps.item(ci);
            String cname = ClickTtXmlImporter.competitionName(comp);
            boolean doubles = ClickTtXmlImporter.isDoubles(comp);
            List<XmlMatch> xms = readMatches(comp, notes, cname);
            if (xms.isEmpty()) {
                notes.add("„" + cname + "“: keine Spiele in der Datei – Klasse nicht angelegt.");
                continue;
            }
            // die Klasse kann beim Übernehmen der Teilnehmenden (Warteliste) schon entstanden sein
            PlayingClass c = t.findClass(cname).orElse(null);
            if (c == null) {
                c = t.addClass(new PlayingClass(cname, doubles ? Discipline.DOUBLES : Discipline.SINGLES,
                        ClassType.guess(cname, ClickTtXmlImporter.competitionSex(ClickTtXmlImporter.attr(comp, "sex"))), null, null));
            } else if (doubles && !c.isDoubles()) {
                t.changeDiscipline(c, Discipline.DOUBLES);
            }
            c.setWinningSets(winningSets);
            c.setAvoidSameClub(false);
            classCount++;

            // XML-Player-ID -> Entrant-ID (Einzel: Spieler-ID, Doppel: „pair:…“) – nur wer gespielt hat
            Set<String> played = new LinkedHashSet<>();
            for (XmlMatch m : xms) {
                played.add(m.a());
                played.add(m.b());
            }
            Map<String, String> entrantOf = new HashMap<>();
            Map<String, Integer> ttrOf = new HashMap<>();
            Element players = child(comp, "players");
            if (players != null) {
                for (Node n = players.getFirstChild(); n != null; n = n.getNextSibling()) {
                    if (!(n instanceof Element pe) || !"player".equals(pe.getTagName())) continue;
                    String xid = pe.getAttribute("id");
                    if (!played.contains(xid)) continue;
                    List<Player> persons = new ArrayList<>();
                    for (Node q = pe.getFirstChild(); q != null; q = q.getNextSibling()) {
                        if (q instanceof Element pp && "person".equals(pp.getTagName())) {
                            Player found = byKey.get(ClickTtXmlImporter.toPlayer(pp).identityKey());
                            if (found != null) persons.add(found);
                        }
                    }
                    if (persons.isEmpty()) continue;
                    if (doubles && persons.size() >= 2) {
                        DoublesPair pair = t.createPair(c, persons.get(0), persons.get(1));
                        entrantOf.put(xid, "pair:" + pair.getId());
                        ttrOf.put(xid, pair.getAverageTtr() == null ? 0 : pair.getAverageTtr());
                    } else {
                        t.assign(persons.get(0), c);
                        entrantOf.put(xid, persons.get(0).getId());
                        ttrOf.put(xid, persons.get(0).getTtr() == null ? 0 : persons.get(0).getTtr());
                    }
                }
            }
            for (String xid : played) if (!entrantOf.containsKey(xid)) notes.add("„" + cname + "“: Teilnehmer " + xid + " nicht gefunden – Spiele ohne ihn.");
            xms.removeIf(m -> !entrantOf.containsKey(m.a()) || !entrantOf.containsKey(m.b()));

            // Phase 1 übernehmen; spätere Phasen werden gemeldet
            Map<Integer, List<XmlMatch>> byPhase = new TreeMap<>();
            for (XmlMatch m : xms) byPhase.computeIfAbsent(m.phase(), k -> new ArrayList<>()).add(m);
            int firstPhase = byPhase.keySet().iterator().next();
            List<XmlMatch> phase1 = byPhase.get(firstPhase);
            for (var e : byPhase.entrySet()) {
                if (e.getKey() != firstPhase) {
                    notes.add("„" + cname + "“: Phase " + e.getKey() + " (" + e.getValue().size() + " Spiele) wurde nicht übernommen – "
                            + "importiert wird Phase 1.");
                }
            }
            int imported = buildPhase(t, c, phase1, entrantOf, ttrOf, notes, cname);
            matchCount += imported;

            LocalDateTime first = phase1.stream().map(XmlMatch::when).filter(java.util.Objects::nonNull).min(Comparator.naturalOrder())
                    .orElse(t.getDate() == null ? LocalDateTime.now().withNano(0) : t.getDate().atTime(9, 0));
            LocalDateTime last = phase1.stream().map(XmlMatch::when).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder())
                    .orElse(first);
            t.importFinished(c, first, last);
        }
        if (classCount == 0) {
            throw new ClickTtImportException("In der Datei wurden keine Spiele gefunden. Erwartet wird eine click-TT-Ergebnisdatei "
                    + "(Turnier mit <matches>).");
        }
        // Klassen ohne Spiele (nur Warteliste) wieder entfernen
        for (PlayingClass c : new ArrayList<>(t.getClasses())) {
            if (t.matchesOf(c).isEmpty()) t.removeClass(c);
        }
        long withoutClass = t.getPlayers().stream().filter(p -> p.getClassNames().isEmpty()).count();
        if (withoutClass > 0) notes.add(withoutClass + " Gemeldete ohne Spiel (nicht angetreten) stehen als Teilnehmende ohne Klasse im Turnier.");
        t.markSaved();
        return new Result(t, notes, classCount, matchCount);
    }

    /** Legt Phase 1 an (Gruppen bzw. KO) und überträgt die Ergebnisse. */
    private int buildPhase(Tournament t, PlayingClass c, List<XmlMatch> xms, Map<String, String> entrantOf, Map<String, Integer> ttrOf,
                           List<String> notes, String cname) {
        Map<String, List<XmlMatch>> byStage = new LinkedHashMap<>();
        for (XmlMatch m : xms) byStage.computeIfAbsent(m.stage(), k -> new ArrayList<>()).add(m);
        boolean allGroups = byStage.keySet().stream().allMatch(s -> GROUP.matcher(s).matches());
        if (allGroups || byStage.size() == 1 && !isKoStage(byStage.keySet().iterator().next())) {
            // Gruppen in natürlicher Reihenfolge („Gruppe 2“ vor „Gruppe 10“, „Gruppe A“ vor „Gruppe B“)
            List<String> names = new ArrayList<>(byStage.keySet());
            names.sort(Comparator.comparing(ClickTtResultImporter::groupSortKey));
            List<List<String>> groups = new ArrayList<>();
            for (String gname : names) {
                Set<String> members = new LinkedHashSet<>();
                for (XmlMatch m : byStage.get(gname)) {
                    members.add(m.a());
                    members.add(m.b());
                }
                List<String> list = new ArrayList<>(members);
                list.sort(Comparator.comparing((String x) -> ttrOf.getOrDefault(x, 0)).reversed());
                groups.add(list.stream().map(entrantOf::get).toList());
            }
            t.importGroups(c, groups);
            int n = 0;
            for (XmlMatch xm : xms) {
                Match m = findMatch(t, c, entrantOf.get(xm.a()), entrantOf.get(xm.b()));
                if (m == null) {
                    notes.add("„" + cname + "“: Spiel " + xm.nr() + " (" + xm.stage() + ") passt zu keiner Gruppenpaarung – übersprungen.");
                    continue;
                }
                if (apply(t, m, xm, entrantOf, notes, cname)) n++;
            }
            long open = t.matchesOf(c).stream().filter(m -> m.getStatus() != de.hts.tournament.MatchStatus.DONE).count();
            if (open > 0) notes.add("„" + cname + "“: " + open + " Gruppenspiele ohne Ergebnis in der Datei.");
            return n;
        }
        return buildKnockout(t, c, byStage, entrantOf, notes, cname);
    }

    private static String groupSortKey(String gname) {
        Matcher mm = GROUP.matcher(gname);
        String id = mm.matches() ? mm.group(1) : gname;
        return id.matches("\\d+") ? String.format("%06d", Integer.parseInt(id)) : String.format("%6s", id);
    }

    private static boolean isKoStage(String s) {
        String l = s.toLowerCase(java.util.Locale.GERMAN);
        return l.contains("finale") || l.contains("platz") || l.contains("runde") || l.contains("ko");
    }

    /** KO-System: Feld aus den Spielen zurückbauen (vom Finale aus), dann Ergebnisse Runde für Runde übertragen. */
    private int buildKnockout(Tournament t, PlayingClass c, Map<String, List<XmlMatch>> byStage, Map<String, String> entrantOf,
                              List<String> notes, String cname) {
        List<XmlMatch> third = new ArrayList<>();
        List<List<XmlMatch>> rounds = new ArrayList<>();
        for (var e : byStage.entrySet()) {
            String l = e.getKey().toLowerCase(java.util.Locale.GERMAN);
            if (l.contains("platz 3") || l.contains("platz3") || l.contains("3. platz")) third.addAll(e.getValue());
            else rounds.add(e.getValue());
        }
        rounds.sort(Comparator.comparingInt((List<XmlMatch> r) -> -r.size()).thenComparingInt(r -> r.get(0).nr()));
        if (rounds.isEmpty() || rounds.get(rounds.size() - 1).size() != 1) {
            notes.add("„" + cname + "“: KO-Runden ließen sich nicht eindeutig zuordnen – Spiele nicht übernommen.");
            return 0;
        }
        int depth = rounds.size();
        int size = 1 << depth;
        String[] slots = new String[size];
        // vom Finale aus: Gewinner oben, jeweils aus dem Spiel der Vorrunde, in dem er gespielt hat (sonst Freilos)
        place(rounds, depth - 1, rounds.get(depth - 1).get(0), 0, size, slots);
        List<String> slotIds = new ArrayList<>();
        for (String x : slots) slotIds.add(x == null ? null : entrantOf.get(x));
        try {
            t.importKnockout(c, slotIds, !third.isEmpty());
        } catch (RuntimeException ex) {
            notes.add("„" + cname + "“: KO-Feld konnte nicht aufgebaut werden (" + ex.getMessage() + ").");
            return 0;
        }
        int n = 0;
        List<XmlMatch> all = new ArrayList<>();
        rounds.forEach(all::addAll);
        all.addAll(third);
        for (XmlMatch xm : all) {
            Match m = findMatch(t, c, entrantOf.get(xm.a()), entrantOf.get(xm.b()));
            if (m == null) {
                notes.add("„" + cname + "“: Spiel " + xm.nr() + " (" + xm.stage() + ") passt nicht ins KO-Feld – übersprungen.");
                continue;
            }
            if (apply(t, m, xm, entrantOf, notes, cname)) n++;
        }
        return n;
    }

    /** Setzt die Teilnehmer des Spiels {@code m} (Runde r) in das Feld-Segment [from, from+span). */
    private static void place(List<List<XmlMatch>> rounds, int r, XmlMatch m, int from, int span, String[] slots) {
        String[] sides = {m.a(), m.b()};
        for (int s = 0; s < 2; s++) {
            int segFrom = from + s * span / 2, segSpan = span / 2;
            String who = sides[s];
            if (r == 0) {
                slots[segFrom] = who;          // segSpan == 1
                continue;
            }
            XmlMatch prev = null;
            for (XmlMatch x : rounds.get(r - 1)) if (who.equals(x.a()) || who.equals(x.b())) prev = x;
            if (prev != null) {
                place(rounds, r - 1, prev, segFrom, segSpan, slots);
            } else {
                // Freilos bis hierher: oben einsetzen, Rest bleibt leer
                slots[segFrom] = who;
            }
        }
    }

    private static Match findMatch(Tournament t, PlayingClass c, String a, String b) {
        for (Match m : t.matchesOf(c)) {
            if (m.getStatus() == de.hts.tournament.MatchStatus.DONE) continue;
            if ((a.equals(m.getA()) && b.equals(m.getB())) || (a.equals(m.getB()) && b.equals(m.getA()))) return m;
        }
        return null;
    }

    private static boolean apply(Tournament t, Match m, XmlMatch xm, Map<String, String> entrantOf, List<String> notes, String cname) {
        boolean swapped = !entrantOf.get(xm.a()).equals(m.getA());
        List<int[]> sets = new ArrayList<>();
        for (int[] s : xm.sets()) sets.add(swapped ? new int[]{s[1], s[0]} : s);
        int winnerSide = xm.winner() == 0 ? 0 : (swapped ? 3 - xm.winner() : xm.winner());
        try {
            t.importResult(m, sets, 0, xm.when());
            return true;
        } catch (IllegalArgumentException ex) {
            // unvollständige Sätze (z. B. kampflos/Aufgabe): Verlierer gibt auf
            if (winnerSide != 0) {
                try {
                    t.importResult(m, sets, winnerSide == 1 ? 2 : 1, xm.when());
                    notes.add("„" + cname + "“: Spiel " + xm.nr() + " als Aufgabe/kampflos übernommen.");
                    return true;
                } catch (RuntimeException ignored) {
                    // unten melden
                }
            }
            notes.add("„" + cname + "“: Spiel " + xm.nr() + " – Ergebnis nicht lesbar (" + ex.getMessage() + ").");
            return false;
        }
    }

    private static List<XmlMatch> readMatches(Element comp, List<String> notes, String cname) {
        List<XmlMatch> out = new ArrayList<>();
        Element matches = child(comp, "matches");
        if (matches == null) return out;
        for (Node n = matches.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element me) || !"match".equals(me.getTagName())) continue;
            String a = me.getAttribute("player-a"), b = me.getAttribute("player-b");
            if (a.isBlank() || b.isBlank()) {
                notes.add("„" + cname + "“: Spiel " + me.getAttribute("nr") + " ohne zwei Teilnehmende – übersprungen.");
                continue;
            }
            String group = me.getAttribute("group").trim();
            int phase = 1;
            Matcher pm = PHASE.matcher(group);
            if (pm.matches()) {
                phase = Integer.parseInt(pm.group(1));
                group = pm.group(2).trim();
            }
            List<int[]> sets = new ArrayList<>();
            for (int k = 1; k <= 7; k++) {
                Integer sa = intOf(me.getAttribute("set-a-" + k)), sb = intOf(me.getAttribute("set-b-" + k));
                if (sa == null || sb == null || (sa == 0 && sb == 0)) break;
                sets.add(new int[]{sa, sb});
            }
            Integer ma = intOf(me.getAttribute("matches-a")), mb = intOf(me.getAttribute("matches-b"));
            int winner = ma != null && ma > 0 ? 1 : mb != null && mb > 0 ? 2 : 0;
            Integer nr = intOf(me.getAttribute("nr"));
            out.add(new XmlMatch(nr == null ? out.size() + 1 : nr, phase, group.isEmpty() ? "Gruppe 1" : group, a, b, sets,
                    intOrZero(me.getAttribute("sets-a")), intOrZero(me.getAttribute("sets-b")), winner, dateTime(me.getAttribute("scheduled"))));
        }
        out.sort(Comparator.comparingInt(XmlMatch::nr));
        return out;
    }

    private static Element child(Element parent, String tag) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && tag.equals(e.getTagName())) return e;
        }
        return null;
    }

    private static String locationOf(Element root) {
        Element loc = child(root, "tournament-location");
        if (loc == null) return null;
        String n = loc.getAttribute("name"), city = loc.getAttribute("city");
        String s = (n == null ? "" : n) + (city == null || city.isBlank() ? "" : (n == null || n.isBlank() ? "" : ", ") + city);
        return s.isBlank() ? null : s;
    }

    private static Integer intOf(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int intOrZero(String s) {
        Integer i = intOf(s);
        return i == null ? 0 : i;
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim().substring(0, Math.min(10, s.trim().length())));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalDateTime dateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), SCHEDULED);
        } catch (RuntimeException e) {
            LocalDate d = date(s);
            return d == null ? null : d.atTime(12, 0);
        }
    }
}
