/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.persistence;

import de.hts.tournament.*; import de.hts.player.*; import java.nio.file.*; import java.util.*;
/**
 * HTS 0.42.0 (#76): Erzeugt die Beispiel-Turnierdatei einer Version für {@link TournamentFileCompatTest}
 * (Gruppen→KO, Gruppen→Qualifikation, KO direkt, Doppel, Anwesenheit, Meldungen) samt erwarteten Werten.
 * Bei jeder veröffentlichten Version ausführen und beide Dateien nach src/test/resources legen:
 * {@code java … de.hts.persistence.CompatFixtureGenerator src/test/resources/turnierdatei-<Version>.hts}
 */
public class CompatFixtureGenerator {
  static List<Player> players(Tournament t, String name, int n, int seed, boolean women) {
    List<Player> ps = new ArrayList<>(); Random r = new Random(seed);
    for (int i = 1; i <= n; i++) { Player p = new Player("Vor" + name + i, "Nach" + name + i);
      p.setInternalNr(name + "-" + i); p.setSex(women && i % 2 == 0 ? Sex.FEMALE : Sex.MALE); p.setBirthyear(1990 + i % 20);
      p.setTtr(1100 + r.nextInt(900)); p.setClubName("Verein " + (char) ('A' + i % 6)); p.setLicenceNr("9000" + seed + i); ps.add(p); }
    t.merge(ps); return t.getPlayers().stream().filter(p -> p.getInternalNr() != null && p.getInternalNr().startsWith(name + "-")).toList();
  }
  static PlayingClass groups(Tournament t, String name, int n, int g, int seed) {
    PlayingClass c = t.addClass(new PlayingClass(name));
    for (Player p : players(t, name, n, seed, false)) { t.setAttendance(p, Attendance.PRESENT); t.assign(p, c); }
    c.setGroupsBySize(false); c.setGroupCount(g); t.drawGroups(c, true, new Random(seed)); return c;
  }
  static void play(Tournament t, Match m) {
    Entrant a = t.entrant(m, m.getA()), b = t.entrant(m, m.getB());
    boolean aw = (a.ttr() == null ? 0 : a.ttr()) >= (b.ttr() == null ? 0 : b.ttr());
    int l = 3 + m.getNumber() % 7; List<int[]> s = new ArrayList<>();
    for (int i = 0; i < 3; i++) s.add(aw ? new int[]{11, l} : new int[]{l, 11});
    s.add(1, aw ? new int[]{9, 11} : new int[]{11, 9});
    t.recordResult(m, s, 0);
  }
  static void playAll(Tournament t, PlayingClass c, int max) {
    for (int k = 0; k < max; k++) { Match o = t.matchesOf(c).stream().filter(m -> m.isReady() && (m.getStatus() == MatchStatus.ACTIVE || m.getStatus() == MatchStatus.QUEUED)).findFirst().orElse(null);
      if (o == null) return; play(t, o); }
  }
  public static void main(String[] a) throws Exception {
    Tournament t = new Tournament(); t.setTableCount(8); t.setTableName(1, "Center Court");
    PlayingClass ko = groups(t, "Herren A", 12, 3, 11); t.startClass(ko); playAll(t, ko, 9999);
    PlayingClass ko2 = t.createNextPhase(ko, TournamentMode.KNOCKOUT, 2, 0, new Random(5)); playAll(t, ko2, 3);
    PlayingClass q = groups(t, "Jugend Q", 16, 4, 12); t.startClass(q); playAll(t, q, 9999);
    PlayingClass q2 = t.createQualificationPhase(q, 1, 2, 2, 0, new Random(6)); t.resumeClass(q2); playAll(t, q2, 9999);
    // Einzel KO direkt (geplant, nicht gestartet) mit Gemeldeten und einem Nachrücker
    PlayingClass d = t.addClass(new PlayingClass("Damen Einzel"));
    List<Player> ds = players(t, "Damen", 6, 13, true);
    for (int i = 0; i < ds.size(); i++) { Player p = ds.get(i); if (i == 5) p.addWaitingList(d.getName()); else p.addRegistration(d.getName()); }
    t.setAttendance(ds.get(0), Attendance.PRESENT); t.setAttendance(ds.get(1), Attendance.PRESENT); t.assign(ds.get(0), d); t.assign(ds.get(1), d);
    t.setAttendance(ds.get(2), Attendance.WITHDRAWN); t.setAttendance(ds.get(3), Attendance.WITHDRAW_REQUESTED);
    // Doppel
    PlayingClass dd = t.addClass(new PlayingClass("Herren Doppel")); t.changeDiscipline(dd, Discipline.DOUBLES);
    List<Player> hs = t.playersIn(ko).stream().limit(8).toList();
    for (int i = 0; i < 8; i += 2) t.createPair(dd, hs.get(i), hs.get(i + 1));
    t.drawKnockout(dd, true, new Random(7)); t.startClass(dd); playAll(t, dd, 2);
    t.setTableExcluded(8, List.of("Herren Doppel"));
    TournamentFile.save(Path.of(a[0]), t);
    StringBuilder sets = new StringBuilder();
    for (Match m : t.getMatches()) if (m.getStatus() == MatchStatus.DONE) sets.append(m.getNumber()).append(':').append(m.getSetsText()).append(';');
    Properties pr = new Properties();
    pr.setProperty("classes", String.valueOf(t.getClasses().size()));
    pr.setProperty("classNames", String.join("|", t.getClasses().stream().map(PlayingClass::getName).toList()));
    pr.setProperty("players", String.valueOf(t.getPlayers().size()));
    pr.setProperty("matches", String.valueOf(t.getMatches().size()));
    pr.setProperty("completed", String.valueOf(t.getMatches().stream().filter(m -> m.getStatus() == MatchStatus.DONE).count()));
    pr.setProperty("present", String.valueOf(t.getPlayers().stream().filter(Player::isPresent).count()));
    pr.setProperty("withdrawn", String.valueOf(t.getPlayers().stream().filter(p -> p.getAttendance() == Attendance.WITHDRAWN).count()));
    pr.setProperty("registrations", String.valueOf(t.getPlayers().stream().mapToInt(p -> p.getRegistrations().size()).sum()));
    pr.setProperty("waitingLists", String.valueOf(t.getPlayers().stream().mapToInt(p -> p.getWaitingLists().size()).sum()));
    pr.setProperty("pairs", String.valueOf(t.getPairs().size()));
    pr.setProperty("tableName1", t.tableName(1));
    pr.setProperty("sets", sets.toString());
    try (var w = Files.newBufferedWriter(Path.of(a[0].replace(".hts", ".properties")))) { pr.store(w, "Erwartete Werte, erzeugt beim Speichern"); }
    System.out.println("ok " + t.getClasses().size() + " Klassen, " + t.getPlayers().size() + " Personen, " + t.getMatches().size() + " Spiele");
  }
}
