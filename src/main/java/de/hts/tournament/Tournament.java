/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.tournament;

import de.hts.player.Attendance;
import de.hts.player.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Ein Turnier: Stammdaten, Spielerliste und Spielklassen.
 *
 * <p>Die Zuordnung eines Spielers zu einer Klasse ist eine Meldung
 * ({@link Player#getClassNames()}). „In der Klasse“ heißt angemeldet. Teilnehmende dürfen mehreren
 * Klassen angehören. Alle Listen sind beobachtbar, damit sich die Oberfläche automatisch
 * aktualisiert; {@link #modifiedProperty()} zeigt ungespeicherte Änderungen an.
 */
public final class Tournament {

    private String id;
    private final ObjectProperty<LocalDateTime> startedAt = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final StringProperty location = new SimpleStringProperty();
    private final ObservableList<Player> players = FXCollections.observableArrayList();
    private final ObservableList<PlayingClass> classes = FXCollections.observableArrayList();
    private final ObservableList<DoublesPair> pairs = FXCollections.observableArrayList();
    private final ObservableList<Match> matches = FXCollections.observableArrayList();
    private final BooleanProperty modified = new SimpleBooleanProperty(false);

    public Tournament() {
        players.addListener((ListChangeListener<Player>) c -> {
            playerIdCache = null;
            markModified();
        });
        classes.addListener((ListChangeListener<PlayingClass>) c -> markModified());
        pairs.addListener((ListChangeListener<DoublesPair>) c -> markModified());
        matches.addListener((ListChangeListener<Match>) c -> markModified());
        name.addListener((o, a, b) -> markModified());
        date.addListener((o, a, b) -> markModified());
        location.addListener((o, a, b) -> markModified());
        startedAt.addListener((o, a, b) -> markModified());
    }

    // --- Spieler -----------------------------------------------------------

    public ObservableList<Player> getPlayers() {
        return players;
    }

    /** Ergebnis eines Zusammenführens: wie viele Spieler neu bzw. aktualisiert wurden. */
    public record MergeResult(int added, int updated) {
    }

    /**
     * Fügt Spieler hinzu. Bereits vorhandene Personen (gleicher {@link Player#identityKey()})
     * werden nicht doppelt angelegt, sondern mit den neuen Daten aktualisiert.
     * Für Meldungen zu noch unbekannten Klassen werden die Klassen angelegt.
     */
    /**
     * Import in neu angelegte Klassen: Meldungen für {@code classNames} werden nur als „gemeldet“ vermerkt – die
     * Personen stehen dann unter „Verfügbar“ und werden von Hand (Drag &amp; Drop) in die Klasse genommen.
     */
    public static void registerOnly(Collection<Player> imported, java.util.Set<String> classNames) {
        for (Player p : imported) {
            for (String cn : new java.util.ArrayList<>(p.getClassNames())) {
                if (classNames.contains(cn)) {
                    p.removeFromClass(cn);
                    p.addRegistration(cn);
                }
            }
        }
    }

    public MergeResult merge(Collection<Player> incoming) {
        Map<String, Player> byKey = new HashMap<>();
        for (Player p : players) {
            byKey.put(p.identityKey(), p);
        }
        int added = 0;
        int updated = 0;
        for (Player p : incoming) {
            Player existing = byKey.get(p.identityKey());
            if (existing == null) {
                players.add(p);
                byKey.put(p.identityKey(), p);
                added++;
            } else {
                existing.updateFrom(p);
                updated++;
            }
        }
        ensureClassesForEntries();
        if (updated > 0) {
            markModified();
        }
        return new MergeResult(added, updated);
    }

    public void removePlayers(Collection<Player> toRemove) {
        for (DoublesPair pair : List.copyOf(pairs)) {
            if (pair.getFirst() != null && toRemove.contains(pair.getFirst())) pair.setFirst(null);
            if (pair.getSecond() != null && toRemove.contains(pair.getSecond())) pair.setSecond(null);
            if (pair.getThird() != null && toRemove.contains(pair.getThird())) pair.setThird(null);
            // Lücken schließen (Reihenfolge der übrigen Mitglieder bleibt)
            List<Player> rest = pair.members();
            pair.setFirst(rest.size() > 0 ? rest.get(0) : null);
            pair.setSecond(rest.size() > 1 ? rest.get(1) : null);
            pair.setThird(rest.size() > 2 ? rest.get(2) : null);
            if (pair.isEmpty()) pairs.remove(pair);
        }
        players.removeAll(toRemove);
    }

    public void clearPlayers() {
        pairs.clear();
        players.clear();
    }

    public int size() {
        return players.size();
    }

    // --- Klassen -----------------------------------------------------------

    public ObservableList<PlayingClass> getClasses() {
        return classes;
    }

    public Optional<PlayingClass> findClass(String className) {
        if (className == null) return Optional.empty();
        String key = className.trim().toLowerCase(Locale.GERMAN);
        return classes.stream().filter(c -> c.getName().trim().toLowerCase(Locale.GERMAN).equals(key)).findFirst();
    }

    /** Legt eine Klasse an. */
    public PlayingClass addClass(PlayingClass c) {
        if (findClass(c.getName()).isPresent()) {
            throw new IllegalArgumentException("Es gibt bereits eine Klasse „" + c.getName() + "“.");
        }
        classes.add(c);
        return c;
    }

    /** Benennt eine Klasse um; die Meldungen aller Spieler werden mitgezogen. */
    public void renameClass(PlayingClass c, String newName) {
        String trimmed = newName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Der Klassenname darf nicht leer sein.");
        }
        if (trimmed.equals(c.getName())) return;
        findClass(trimmed).filter(other -> other != c).ifPresent(other -> {
            throw new IllegalArgumentException("Es gibt bereits eine Klasse „" + trimmed + "“.");
        });
        String old = c.getName();
        for (Player p : players) {
            p.renameClass(old, trimmed);
        }
        for (DoublesPair pair : pairs) {
            if (pair.getClassName().equals(old)) pair.setClassName(trimmed);
        }
        for (Match m : matches) {
            if (m.getClassName().equals(old)) m.setClassName(trimmed);
        }
        for (PlayingClass other : classes) {
            if (old.equals(other.getFollowUpClass())) other.setFollowUpClass(trimmed);
            if (old.equals(other.getSourceClass())) other.setSourceClass(trimmed);
        }
        for (java.util.Set<String> set : tableClasses.values()) {
            if (set.remove(old)) set.add(trimmed);
        }
        c.setName(trimmed);
        markModified();
    }

    /** Löscht eine Klasse samt aller Meldungen zu dieser Klasse. */
    public void removeClass(PlayingClass c) {
        if (c.getFollowUpClass() != null) findClass(c.getFollowUpClass()).filter(f -> f != c).ifPresent(this::removeClass);
        // 1.3 (#107): Korrektur- und deaktivierte Phasen hängen an der Vorphase – mit ihr bzw. mit der regulären Folgephase weg
        if (!c.isSideClass()) {
            for (PlayingClass side : new java.util.ArrayList<>(classes)) {
                if (side == c || !side.isSideClass() || side.getSourceClass() == null) continue;
                if (side.getSourceClass().equals(c.getName()) || side.getSourceClass().equals(c.getSourceClass())) removeClass(side);
            }
        }
        for (Player p : players) {
            p.forgetClass(c.getName());
        }
        pairs.removeIf(pair -> pair.getClassName().equals(c.getName()));
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        tableClasses.values().forEach(set -> set.remove(c.getName()));
        tableClasses.values().removeIf(java.util.Set::isEmpty);
        classes.remove(c);
    }

    /**
     * Ordnet einen Spieler einer Klasse zu (falls noch nicht geschehen).
     * In einer Doppelklasse entsteht dabei eine Paarung, bei der der Partner noch fehlt.
     */
    public boolean assign(Player p, PlayingClass c) {
        if (p.isInClass(c.getName())) return false;
        boolean wasReserveOnly = p.isReserveOnly();
        p.addToClass(c.getName());
        // 0.38 (#55): ein in die Klasse übernommener Nachrücker gilt als angemeldet (wie manuell Hinzugefügte)
        if (wasReserveOnly && p.getAttendance() == Attendance.UNKNOWN) p.setAttendance(Attendance.PRESENT);
        if (c.isDoubles() && pairOf(p, c).isEmpty()) {
            pairs.add(newPair(c, p, null));
        }
        markModified();
        return true;
    }

    public boolean unassign(Player p, PlayingClass c) {
        if (c.isDoubles()) {
            pairOf(p, c).ifPresent(pair -> clearSlot(pair, p));
        }
        boolean removed = p.removeFromClass(c.getName());
        if (removed) markModified();
        return removed;
    }

    // --- Doppel ------------------------------------------------------------

    public ObservableList<DoublesPair> getPairs() {
        return pairs;
    }

    /** Ergebnis von {@link #drawPartners}: neu gebildete Doppel und wer übrig bleibt. */
    public record PartnerDraw(List<DoublesPair> pairs, Player leftOver, List<Player> leftOvers) {
        public PartnerDraw(List<DoublesPair> pairs, Player leftOver) {
            this(pairs, leftOver, leftOver == null ? List.of() : List.of(leftOver));
        }
    }

    private DoublesPair newPair(PlayingClass c, Player first, Player second) {
        DoublesPair pair = new DoublesPair(c.getName(), first, second);
        pair.setCapacity(c.teamSize());
        return pair;
    }

    /** Team (bzw. Doppel) aus den angegebenen Personen bilden (2er: Dame automatisch auf Platz 2). */
    public DoublesPair createTeam(PlayingClass c, List<Player> members) {
        List<Player> ms = new java.util.ArrayList<>(members);
        if (c.isTeam() && c.isWomanRequired()) {
            // Dame(n) nach hinten: 2er Team → Dame ist Teilnehmende/r 2, 3er Team → Standard-Aufstellung H1, H2, D
            ms.sort(java.util.Comparator.comparingInt(p -> p.getSex() == de.hts.player.Sex.FEMALE ? 1 : 0));
        }
        DoublesPair pair = createPair(c, ms.get(0), ms.size() > 1 ? ms.get(1) : null);
        for (int i = 2; i < ms.size(); i++) addToPair(pair, ms.get(i));
        return pair;
    }

    /**
     * Doppelklasse: alle Einzelnen ohne Partner/in werden einander rein zufällig zugelost (ohne Rücksicht auf Verein,
     * TTR oder Geschlecht). Bei ungerader Zahl bleibt eine/r (per Los) übrig. Danach sind es normale Doppel.
     */
    public PartnerDraw drawPartners(PlayingClass c, java.util.Random random) {
        requireDoubles(c);
        if (c.isDrawn() || c.isStarted()) throw new IllegalStateException("„" + c.getName() + "“ ist schon ausgelost.");
        List<Player> singles = new java.util.ArrayList<>();
        for (DoublesPair p : pairsIn(c)) {
            if (p.isComplete()) continue;
            singles.addAll(p.members());
        }
        // Gemeldete ohne Partner/in (z. B. aus dem Import) stehen unter „Verfügbar“ und werden mit zugelost
        for (Player x : players) {
            if (x.getRegistrations().contains(c.getName()) && pairOf(x, c).isEmpty() && !singles.contains(x)) singles.add(x);
        }
        int size = c.teamSize();
        if (singles.size() < size) throw new IllegalStateException("Zum Zulosen braucht es mindestens " + size
                + " Teilnehmende ohne " + (c.isTeam() ? "Team." : "Partner/in."));
        java.util.Collections.shuffle(singles, random);
        List<DoublesPair> made = new java.util.ArrayList<>();
        if (c.isTeam() && c.isWomanRequired()) {
            // je Team mindestens eine Dame: zuerst je Team eine Dame, dann auffüllen
            List<Player> women = new java.util.ArrayList<>(singles.stream().filter(x -> x.getSex() == de.hts.player.Sex.FEMALE).toList());
            List<Player> rest = new java.util.ArrayList<>(singles);
            rest.removeAll(women);
            int teams = Math.min(women.size(), singles.size() / size);
            List<List<Player>> groups = new java.util.ArrayList<>();
            for (int k = 0; k < teams; k++) groups.add(new java.util.ArrayList<>(List.of(women.remove(0))));
            rest.addAll(women);
            java.util.Collections.shuffle(rest, random);
            for (List<Player> g : groups) while (g.size() < size && !rest.isEmpty()) g.add(rest.remove(0));
            for (List<Player> g : groups) made.add(createTeam(c, g));
            singles = rest;
        } else {
            int full = singles.size() / size * size;
            for (int i = 0; i < full; i += size) made.add(createTeam(c, singles.subList(i, i + size)));
            singles = new java.util.ArrayList<>(singles.subList(full, singles.size()));
        }
        markModified();
        return new PartnerDraw(made, singles.isEmpty() ? null : singles.get(0), singles);
    }

    public List<DoublesPair> pairsIn(PlayingClass c) {
        return pairs.stream().filter(p -> p.getClassName().equals(c.getName())).toList();
    }

    public Optional<DoublesPair> pairOf(Player p, PlayingClass c) {
        return pairs.stream().filter(pair -> pair.getClassName().equals(c.getName()) && pair.contains(p)).findFirst();
    }

    /**
     * Bildet eine neue Paarung. {@code second} darf {@code null} sein (Partner/in folgt).
     * Stehen die Teilnehmenden in dieser Klasse schon in einer anderen Paarung, werden sie dort herausgenommen.
     */
    public DoublesPair createPair(PlayingClass c, Player first, Player second) {
        requireDoubles(c);
        if (first == null) throw new IllegalArgumentException("Eine Paarung braucht mindestens eine Person.");
        if (first == second) throw new IllegalArgumentException("Eine Person kann nicht mit sich selbst Doppel spielen.");
        detachFromPairs(first, c);
        if (second != null) detachFromPairs(second, c);
        DoublesPair pair = newPair(c, first, second);
        pairs.add(pair);
        enter(first, c);
        if (second != null) enter(second, c);
        markModified();
        return pair;
    }

    /**
     * Setzt eine Person auf den freien Platz einer Paarung (z. B. per Drag & Drop).
     * @throws IllegalStateException wenn die Paarung schon vollständig ist
     */
    public void addToPair(DoublesPair pair, Player p) {
        PlayingClass c = findClass(pair.getClassName()).orElseThrow();
        if (pair.contains(p)) return;
        if (pair.isComplete()) {
            throw new IllegalStateException("Die Paarung " + pair.getDisplayName() + " ist bereits vollständig.");
        }
        detachFromPairs(p, c);
        if (!pairs.contains(pair)) pairs.add(pair); // falls sie durch das Herauslösen leer wurde
        if (pair.getFirst() == null) pair.setFirst(p);
        else if (pair.getSecond() == null) pair.setSecond(p);
        else pair.setThird(p);
        enter(p, c);
        markModified();
    }

    /** Nimmt eine Person aus ihrer Paarung (und damit aus der Doppelklasse). */
    public void removeFromPair(DoublesPair pair, Player p) {
        PlayingClass c = findClass(pair.getClassName()).orElseThrow();
        unassign(p, c);
    }

    /** Löst eine Paarung ganz auf. */
    public void removePair(DoublesPair pair) {
        for (Player p : pair.members()) p.removeFromClass(pair.getClassName());
        pairs.remove(pair);
        markModified();
    }

    /**
     * Ändert Einzel/Doppel einer Klasse. Wird eine Einzelklasse zum Doppel, bekommt jede schon
     * zugeordnete Person eine Paarung ohne Partner/in; wird ein Doppel zum Einzel, entfallen die Paarungen
     * (die Zuordnungen bleiben).
     */
    public void changeDiscipline(PlayingClass c, Discipline d) {
        if (c.getDiscipline() == d) return;
        boolean sameSize = c.isDoubles() && d != Discipline.SINGLES && c.teamSize() == Math.max(2, d.size());
        c.setDiscipline(d);
        if (d == Discipline.SINGLES || !sameSize) pairs.removeIf(pair -> pair.getClassName().equals(c.getName()));
        if (d != Discipline.SINGLES) {
            for (Player p : playersIn(c)) {
                if (pairOf(p, c).isEmpty()) pairs.add(newPair(c, p, null));
            }
        }
        markModified();
    }

    /** Jede Person mit Meldung in einer Doppelklasse, aber ohne Paarung, bekommt eine Paarung ohne Partner/in. */
    public void ensurePairsForEntries() {
        for (PlayingClass c : classes) {
            if (!c.isDoubles()) continue;
            for (Player p : playersIn(c)) {
                if (pairOf(p, c).isEmpty()) pairs.add(newPair(c, p, null));
            }
        }
    }

    /**
     * Import in eine Doppelklasse: Wer keinen Partner aus der Datei hat, steht nicht als halbe Paarung in der Klasse,
     * sondern als Gemeldete/r unter „Verfügbar“ (bis von Hand oder per „Partner zulosen“ gepaart).
     */
    public void registerWithoutPartner(Player p, PlayingClass c) {
        requireDoubles(c);
        java.util.Optional<DoublesPair> pair = pairOf(p, c);
        if (pair.isPresent() && pair.get().isComplete()) return;
        pair.ifPresent(pairs::remove);
        p.removeFromClass(c.getName());
        p.addRegistration(c.getName());
        markModified();
    }

    /** Nur für das Laden aus einer Datei. */
    public void restorePair(String id, String className, Player first, Player second) {
        restorePair(id, className, first, second, null);
    }

    /** Nur für das Laden aus einer Datei (3er Team mit drittem Mitglied). */
    public void restorePair(String id, String className, Player first, Player second, Player third) {
        DoublesPair pair = new DoublesPair(id, className, first, second);
        pair.setCapacity(findClass(className).map(PlayingClass::teamSize).orElse(2));
        pair.setThird(third);
        pairs.add(pair);
    }

    private void enter(Player p, PlayingClass c) {
        if (!p.isInClass(c.getName())) {
            p.addToClass(c.getName());
        }
    }

    private void detachFromPairs(Player p, PlayingClass c) {
        pairOf(p, c).ifPresent(pair -> clearSlot(pair, p));
    }

    private void clearSlot(DoublesPair pair, Player p) {
        if (pair.getFirst() == p) {
            pair.setFirst(pair.getSecond());
            pair.setSecond(pair.getThird());
            pair.setThird(null);
        } else if (pair.getSecond() == p) {
            pair.setSecond(pair.getThird());
            pair.setThird(null);
        } else if (pair.getThird() == p) {
            pair.setThird(null);
        }
        if (pair.isEmpty()) pairs.remove(pair);
        markModified();
    }

    private static void requireDoubles(PlayingClass c) {
        if (!c.isDoubles()) throw new IllegalArgumentException("„" + c.getName() + "“ ist keine Doppelklasse.");
    }

    public List<Player> playersIn(PlayingClass c) {
        return players.stream().filter(p -> p.isInClass(c.getName())).toList();
    }

    public long countIn(PlayingClass c) {
        return players.stream().filter(p -> p.isInClass(c.getName())).count();
    }

    /** Legt für jede Meldung, zu der es noch keine Klasse gibt, eine (kriterienlose) Klasse an. */
    public void ensureClassesForEntries() {
        for (Player p : players) {
            for (String entry : p.getClassNames()) {
                if (findClass(entry).isEmpty()) {
                    classes.add(new PlayingClass(entry));
                }
            }
            for (String entry : p.getWaitingLists()) {
                if (findClass(entry).isEmpty()) {
                    classes.add(new PlayingClass(entry));
                }
            }
        }
    }

    // --- Reihenfolge, Höchstzahl, Auslosung ----------------------------------

    /** Teilnehmende der Klasse in der Reihenfolge ihrer Anmeldung. */
    public List<Player> playersInOrder(PlayingClass c) {
        return players.stream().filter(p -> p.isInClass(c.getName()))
                .sorted(java.util.Comparator.comparing(p -> p.getClassOrder(c.getName())))
                .toList();
    }

    /**
     * Wer über der Höchstzahl der Klasse liegt (die zuletzt Hinzugefügten).
     * Einzel: Personen-IDs; Doppel: Paarungs-IDs. Leer, wenn keine Höchstzahl gesetzt ist.
     */
    public java.util.Set<String> overLimit(PlayingClass c) {
        Integer max = c.getMaxParticipants();
        if (max == null) return java.util.Set.of();
        java.util.Set<String> ids = new java.util.HashSet<>();
        if (c.isDoubles()) {
            List<DoublesPair> list = pairsIn(c);
            for (int i = max; i < list.size(); i++) ids.add(list.get(i).getId());
        } else {
            List<Player> list = playersInOrder(c);
            for (int i = max; i < list.size(); i++) ids.add(list.get(i).getId());
        }
        return ids;
    }

    /** Parteien der Klasse für die Auslosung: Einzel = Personen, Doppel = vollständige Paarungen. */
    public List<Entrant> entrantsOf(PlayingClass c) {
        if (c.isDoubles()) {
            return pairsIn(c).stream().filter(DoublesPair::isComplete).map(pr -> c.isTeam() ? Entrant.ofTeam(pr) : Entrant.of(pr)).toList();
        }
        return playersInOrder(c).stream().map(Entrant::of).toList();
    }

    /** Ein Modus kann festgelegt werden, sobald mindestens 2 Teilnehmende bzw. 2 vollständige Doppel in der Klasse sind. */
    public boolean canChooseMode(PlayingClass c) {
        return entrantsOf(c).size() >= 2;
    }

    /** Lost die Klasse im KO-System aus (zufällig oder nach Setzung), speichert das Ergebnis und legt die Spiele an. */
    public List<Entrant> drawKnockout(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        c.setExempt(List.of());
        // Doppel: Vereinsgleichheit spielt keine Rolle
        List<Entrant> slots = KnockoutDraw.draw(entrantsOf(c), seeded, c.isAvoidSameClub() && !c.isDoubles(), random);
        c.setMode(TournamentMode.KNOCKOUT);
        c.setSwissByes(List.of());
        c.setDraw(slots.stream().map(e -> e == null ? null : e.id()).toList(), seeded);
        c.setReady(false);
        createKnockoutMatches(c);
        markModified();
        return slots;
    }

    /** Platzbelegung der gespeicherten Auslosung; nicht mehr vorhandene Parteien erscheinen als „(entfernt)“. */
    public List<Entrant> drawSlots(PlayingClass c) {
        java.util.Map<String, Entrant> byId = entrantIndex(c);
        List<Entrant> slots = new java.util.ArrayList<>();
        for (String id : c.getDraw()) {
            slots.add(id == null ? null : byId.getOrDefault(id, new Entrant(id, "(entfernt)", null, null)));
        }
        return slots;
    }

    /** Anzahl der Gruppen nach den Einstellungen der Klasse (Gruppengröße bzw. Anzahl Gruppen). */
    /** Doppel: bis zu so vielen Doppeln darf Phase 1 eine Gruppe (jeder gegen jeden) sein, darüber nur KO-System. */
    public static final int MAX_DOUBLES_GROUP = 5;

    public int plannedGroupCount(PlayingClass c) {
        int n = groupEntrantsOf(c).size();
        return c.isGroupsBySize() ? GroupDraw.groupCountForSize(n, c.getGroupSize()) : c.getGroupCount();
    }

    /** Lost die Gruppenphase aus (Schlangensystem, zufällig oder nach Setzung), speichert sie und legt die Spiele an. */
    public List<List<Entrant>> drawGroups(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        c.setExempt(List.of());
        c.setExemptPlaces(0);
        return drawGroupsOf(c, seeded, random);
    }

    private List<List<Entrant>> drawGroupsOf(PlayingClass c, boolean seeded, java.util.Random random) {
        if (c.isPureDoubles() && !c.isPhase() && entrantsOf(c).size() > MAX_DOUBLES_GROUP) {
            throw new IllegalStateException("Bei mehr als " + MAX_DOUBLES_GROUP + " Doppeln ist Phase 1 ein KO-System.");
        }
        // Doppel (Phase 1): genau eine Gruppe, jeder gegen jeden; Vereinsgleichheit spielt keine Rolle
        int groupCount = c.isPureDoubles() && !c.isPhase() ? 1 : plannedGroupCount(c);
        List<List<Entrant>> groups = GroupDraw.draw(groupEntrantsOf(c), groupCount,
                seeded ? c.getGroupSeeding() : GroupDraw.Seeding.RANDOM, c.isAvoidSameClub() && !c.isDoubles(), random);
        c.setMode(TournamentMode.GROUPS);
        c.setSwissByes(List.of());
        c.setGroups(groups.stream().map(g -> g.stream().map(Entrant::id).toList()).toList(), seeded);
        c.setLotSeed(random.nextLong() | 1L);
        c.setReady(false);
        createGroupMatches(c);
        markModified();
        return groups;
    }

    // --- 1.3 (#106): Gruppenphase mit Freistellung -----------------------------------------------

    /** So viele Setzplätze (S1–S4) sind von der Gruppenphase freigestellt. */
    public static final int EXEMPT_COUNT = 4;

    /** Parteien, die in Phase 1 in Gruppen spielen – ohne die Freigestellten. */
    public List<Entrant> groupEntrantsOf(PlayingClass c) {
        if (c.getExempt().isEmpty()) return entrantsOf(c);
        java.util.Set<String> ex = new java.util.HashSet<>(c.getExempt());
        return entrantsOf(c).stream().filter(e -> !ex.contains(e.id())).toList();
    }

    /** TTR-Vorschlag für die Freistellung: die {@value #EXEMPT_COUNT} mit dem höchsten TTR (bei gleichem TTR entscheidet das Los). */
    public List<String> exemptSuggestion(PlayingClass c, java.util.Random random) {
        List<Entrant> all = new java.util.ArrayList<>(entrantsOf(c));
        java.util.Collections.shuffle(all, random);
        all.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        return all.stream().limit(EXEMPT_COUNT).map(Entrant::id).toList();
    }

    /** KO-Feld ohne Freilose: 2, 4, 8, 16, 32, 64 oder 128. */
    public static boolean isFullKoField(int n) {
        return n >= 2 && n <= 128 && Integer.bitCount(n) == 1;
    }

    /**
     * Warum sich die Gruppenphase mit Freistellung so nicht anlegen lässt – sonst {@code null}. 4 Freigestellte plus
     * Gruppen × Plätze je Gruppe müssen ein KO-Feld ohne Freilose ergeben.
     *
     * @param groupPlayers Teilnehmende in den Gruppen (ohne die Freigestellten)
     */
    public static String exemptProblem(int groupPlayers, int groups, int places) {
        if (groups < 1) return "Es muss mindestens eine Gruppe geben.";
        if (groupPlayers / groups < 2) {
            return "Bei " + groups + " Gruppen hätte eine Gruppe weniger als 2 Mitglieder – bitte weniger Gruppen wählen.";
        }
        if (groupPlayers / groups <= places) {
            return "Bei " + groups + " Gruppen kämen aus einer Gruppe alle weiter (Plätze 1–" + places + ") – bitte weniger Gruppen wählen. "
                    + exemptSuggestionText(groupPlayers);
        }
        int field = EXEMPT_COUNT + groups * places;
        if (!isFullKoField(field)) {
            return EXEMPT_COUNT + " Freigestellte + " + groups + " Gruppen × " + places + " Weiterkommende = " + field
                    + " – das ist kein KO-Feld ohne Freilose (8, 16, 32, 64 oder 128). Freilose gibt es in diesem Modus nicht. "
                    + exemptSuggestionText(groupPlayers);
        }
        return null;
    }

    /** Vorschläge, die bei so vielen Gruppen-Teilnehmenden ein KO-Feld ohne Freilose ergeben. */
    public static String exemptSuggestionText(int groupPlayers) {
        List<String> out = new java.util.ArrayList<>();
        for (int q = 2; q <= 3; q++) {
            for (int field = 8; field <= 128; field *= 2) {
                if ((field - EXEMPT_COUNT) % q != 0) continue;
                int g = (field - EXEMPT_COUNT) / q;
                if (g < 1 || groupPlayers / g <= q || GroupDraw.maxGroupSize(groupPlayers, g) > 10) continue;
                int min = groupPlayers / g, max = GroupDraw.maxGroupSize(groupPlayers, g);
                out.add(g + " Gruppen (" + (min == max ? "à " + min : "mit " + min + " oder " + max) + ") × Plätze 1–" + q
                        + " = " + (g * q) + " + " + EXEMPT_COUNT + " = " + field + "er-Feld");
            }
        }
        return out.isEmpty() ? "Bei " + groupPlayers + " Teilnehmenden in den Gruppen ergibt keine Einteilung ein KO-Feld ohne Freilose."
                : "Möglich: " + String.join("; ", out) + ".";
    }

    /**
     * Lost die Gruppenphase mit Freistellung aus: die Setzplätze {@code exempt} (S1–S4) spielen keine Gruppenspiele und
     * steigen erst in Phase 2 (KO-System ohne Freilose) ein; aus jeder Gruppe kommen die Plätze 1–{@code places} weiter.
     */
    public List<List<Entrant>> drawGroupsWithExemption(PlayingClass c, List<String> exempt, int places, boolean seeded,
                                                       java.util.Random random) {
        requireNotStarted(c);
        if (c.isDoubles()) throw new IllegalStateException("Die Gruppenphase mit Freistellung gibt es nur im Einzel.");
        if (places < 2 || places > 3) throw new IllegalArgumentException("Weiter je Gruppe: Plätze 1–2 oder 1–3.");
        java.util.Set<String> all = new java.util.HashSet<>();
        for (Entrant e : entrantsOf(c)) all.add(e.id());
        if (exempt == null || exempt.size() != EXEMPT_COUNT || exempt.stream().anyMatch(java.util.Objects::isNull)
                || new java.util.HashSet<>(exempt).size() != EXEMPT_COUNT) {
            throw new IllegalArgumentException("Bitte für S1 bis S4 je eine andere Person wählen.");
        }
        if (!all.containsAll(exempt)) throw new IllegalArgumentException("Eine freigestellte Person spielt nicht in dieser Klasse.");
        int groupPlayers = all.size() - EXEMPT_COUNT;
        int groups = c.isGroupsBySize() ? GroupDraw.groupCountForSize(Math.max(1, groupPlayers), c.getGroupSize()) : c.getGroupCount();
        String problem = exemptProblem(groupPlayers, groups, places);
        if (problem != null) throw new IllegalArgumentException(problem);
        c.setExempt(exempt);
        c.setExemptPlaces(places);
        c.setQualifiers(places);
        c.setQualifierPlaces(List.of());
        return drawGroupsOf(c, seeded, random);
    }

    /** Größe des KO-Feldes nach der Gruppenphase mit Freistellung (0 = keine Freistellung). */
    public int exemptFieldSize(PlayingClass c) {
        return c.getExempt().isEmpty() ? 0 : c.getExempt().size() + c.getGroups().size() * c.getExemptPlaces();
    }

    /**
     * Feste Paarungen bei 4 Gruppen à mindestens 4 und Plätzen 1–3 (16er-Feld), von oben nach unten.
     */
    private static final String[] EXEMPT_4X3 = {"S1", "D3", "B1", "D2", "A1", "C2", "S4", "C3", "S3", "A3", "C1", "B2", "D1", "A2", "S2", "B3"};

    private static final java.util.Set<String> EXEMPT_6X2_LABELS = java.util.Set.of("S1", "S2", "S3", "S4",
            "A1", "B1", "C1", "D1", "E1", "F1", "A2", "B2", "C2", "D2", "E2", "F2");

    /**
     * 1.7.0 (#145): 6 Gruppen × Plätze 1–2 → 16er-KO ohne Freilose, Auslosung je Hälfte (Reihenfolge im Baum von oben):
     * <pre>
     * obere Hälfte:  S–2 · 1–1 · 1–2 · 2–S   (S1 und S4, Platz innerhalb der Hälfte ausgelost)
     * untere Hälfte: S–2 · 2–1 · 1–1 · 2–S   (S2 und S3, ebenso)
     * </pre>
     * Drei ausgeloste Gruppen haben ihren Ersten oben und den Zweiten unten, die anderen drei umgekehrt – Erster und
     * Zweiter derselben Gruppe treffen erst im Finale aufeinander; je Hälfte kommt jeder Gruppenbuchstabe einmal vor.
     * Innerhalb der Hälfte werden Erste bzw. Zweite zufällig auf die Plätze ihrer Art verteilt. Ein mögliches Ergebnis:
     * S1–D2 · F1–E1 · A1–B2 · C2–S4 | S3–E2 · A2–B1 · C1–D1 · F2–S2.
     */
    static String[] drawExempt6x2(java.util.Random random) {
        List<String> groups = new java.util.ArrayList<>(List.of("A", "B", "C", "D", "E", "F"));
        java.util.Collections.shuffle(groups, random);
        List<String> firstsTop = new java.util.ArrayList<>(), secondsTop = new java.util.ArrayList<>();
        List<String> firstsBottom = new java.util.ArrayList<>(), secondsBottom = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String g = groups.get(i);
            if (i < 3) {
                firstsTop.add(g + "1");
                secondsBottom.add(g + "2");
            } else {
                firstsBottom.add(g + "1");
                secondsTop.add(g + "2");
            }
        }
        java.util.Collections.shuffle(firstsTop, random);
        java.util.Collections.shuffle(secondsTop, random);
        java.util.Collections.shuffle(firstsBottom, random);
        java.util.Collections.shuffle(secondsBottom, random);
        boolean s1First = random.nextBoolean(), s3First = random.nextBoolean();
        return new String[]{
                s1First ? "S1" : "S4", secondsTop.get(0), firstsTop.get(0), firstsTop.get(1), firstsTop.get(2), secondsTop.get(1),
                secondsTop.get(2), s1First ? "S4" : "S1",
                s3First ? "S3" : "S2", secondsBottom.get(0), secondsBottom.get(1), firstsBottom.get(0), firstsBottom.get(1),
                firstsBottom.get(2), secondsBottom.get(2), s3First ? "S2" : "S3"};
    }

    /**
     * Phase 2 nach der Gruppenphase mit Freistellung: KO ohne Freilose. Setzliste: S1–S4, dann die Gruppenersten, dann
     * die Zweiten (ggf. Dritten), jeweils nach Spiel-, Satz- und Punkteverhältnis (Gleichstand: Los); Setzplatz k spielt
     * gegen F+1−k (die Freigestellten gegen die schlechtesten Gruppenzweiten bzw. -dritten). Gleiche Gruppe in Runde 1
     * wird durch Tausch mit dem Nächstplatzierten vermieden. 4 Gruppen × 3: festes Schema {@link #EXEMPT_4X3};
     * 6 Gruppen × 2: Auslosung je Hälfte ({@link #drawExempt6x2}).
     */
    private void buildExemptKnockout(PlayingClass c, PlayingClass next, List<List<GroupStanding.Row>> tables,
                                     java.util.Map<String, String> idMap, java.util.Random random) {
        int q = c.getExemptPlaces();
        java.util.Map<String, Entrant> index = entrantIndex(c);
        List<Entrant> seeds = new java.util.ArrayList<>();
        java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
        java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
        java.util.Map<String, Entrant> byLabel = new java.util.HashMap<>();
        for (int k = 0; k < c.getExempt().size(); k++) {
            Entrant e = index.get(c.getExempt().get(k));
            Entrant now = e == null ? null : carryOver(next, e, idMap);
            if (now == null) continue;
            seeds.add(now);
            groupOf.put(now.id(), -1 - k);
            labels.put(now.id(), "S" + (k + 1));
            byLabel.put("S" + (k + 1), now);
        }
        for (int place = 0; place < q; place++) {
            for (int g : tierOrder(tables, place, random, false)) {
                Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
                if (now == null) continue;
                seeds.add(now);
                groupOf.put(now.id(), g);
                labels.put(now.id(), groupLabel(c, g) + (place + 1));
                byLabel.put(GroupDraw.groupName(g) + (place + 1), now);
            }
        }
        int f = seeds.size();
        if (!isFullKoField(f)) {
            removeClass(next);
            throw new IllegalStateException("Es kommen " + f + " ins KO-System – das ist kein KO-Feld ohne Freilose.");
        }
        List<Entrant> slots = new java.util.ArrayList<>();
        boolean fixed = tables.size() == 4 && q == 3 && f == 16 && java.util.Arrays.stream(EXEMPT_4X3).allMatch(byLabel::containsKey);
        if (fixed) {
            for (String l : EXEMPT_4X3) slots.add(byLabel.get(l));
        } else if (tables.size() == 6 && q == 2 && f == 16 && byLabel.keySet().containsAll(EXEMPT_6X2_LABELS)) {
            for (String l : drawExempt6x2(random)) slots.add(byLabel.get(l));
        } else {
            int[] order = KnockoutDraw.seedOrder(f);
            for (int i = 0; i < f; i++) slots.add(seeds.get(order[i] - 1));
            java.util.Map<String, Integer> rank = new java.util.HashMap<>();
            for (int i = 0; i < f; i++) rank.put(seeds.get(i).id(), i);
            avoidSameGroupInRoundOne(slots, rank, groupOf);
        }
        next.setMode(TournamentMode.KNOCKOUT);
        next.setSeedLabels(labels);
        next.setDraw(slots.stream().map(Entrant::id).toList(), true);
        createKnockoutMatches(next);
    }

    /** Gleiche Gruppe in Runde 1: der schwächer Gesetzte tauscht mit dem Nächstplatzierten, mit dem kein Konflikt entsteht. */
    static void avoidSameGroupInRoundOne(List<Entrant> slots, java.util.Map<String, Integer> rank, java.util.Map<String, Integer> groupOf) {
        for (int p = 0; p < slots.size() / 2; p++) {
            if (!groupClash(slots, p, groupOf)) continue;
            int a = 2 * p, b = a + 1;
            int weak = rank.get(slots.get(a).id()) > rank.get(slots.get(b).id()) ? a : b;
            int wr = rank.get(slots.get(weak).id());
            int best = -1, bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < slots.size(); i++) {
                if (i / 2 == p || groupOf.get(slots.get(i).id()) < 0) continue;   // Freigestellte bleiben auf ihrem Platz
                int d = Math.abs(rank.get(slots.get(i).id()) - wr);
                java.util.Collections.swap(slots, weak, i);
                boolean ok = !groupClash(slots, p, groupOf) && !groupClash(slots, i / 2, groupOf);
                java.util.Collections.swap(slots, weak, i);
                if (ok && d < bestDist) {
                    best = i;
                    bestDist = d;
                }
            }
            if (best >= 0) java.util.Collections.swap(slots, weak, best);
        }
    }

    private static boolean groupClash(List<Entrant> slots, int pair, java.util.Map<String, Integer> groupOf) {
        Integer x = groupOf.get(slots.get(2 * pair).id()), y = groupOf.get(slots.get(2 * pair + 1).id());
        return x != null && x >= 0 && x.equals(y);
    }

    /** Rundenzahl im Schweizer System (Einstellung oder Vorschlag), höchstens so viele wie ohne Wiederholung möglich. */
    public int swissRounds(PlayingClass c) {
        int n = c.getMode() == TournamentMode.SWISS && !c.getGroups().isEmpty() ? c.getGroups().get(0).size() : entrantsOf(c).size();
        int r = c.getSwissRounds() > 0 ? c.getSwissRounds() : c.isBadeni() ? BadeniSystem.defaultRounds(n) : SwissSystem.defaultRounds(n);
        return Math.min(r, SwissSystem.maxRounds(n));
    }

    /** Aktuelle (zuletzt angesetzte) Runde im Schweizer System, 0 = noch keine. */
    public int swissRound(PlayingClass c) {
        return matchesOf(c).stream().mapToInt(Match::getRound).max().orElse(c.getSwissByes().isEmpty() ? 0 : c.getSwissByes().size());
    }

    /** Lost die 1. Runde im Schweizer System aus; die weiteren Runden werden nach der Tabelle gepaart. */
    public void drawSwiss(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        c.setExempt(List.of());
        if (c.isDoubles()) throw new IllegalStateException("Das Schweizer System gibt es für Doppel und 2er/3er Turniere nicht.");
        List<Entrant> entrants = entrantsOf(c);
        if (entrants.size() < 2) throw new IllegalArgumentException("Für das Schweizer System braucht es mindestens 2 Teilnehmende.");
        List<Entrant> order = new java.util.ArrayList<>(entrants);
        if (seeded) order.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        c.setMode(TournamentMode.SWISS);
        c.setGroups(List.of(order.stream().map(Entrant::id).toList()), seeded);
        c.setLotSeed(random.nextLong() | 1L);
        c.setSwissByes(List.of());
        c.setReady(false);
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        addSwissRound(c, 1, SwissSystem.firstRound(entrants, seeded, random));
        c.setBadeni(false);
        markModified();
    }

    /**
     * 1.2 (#95): Schweizer System nach Badeni-Art. Runde 1 nach TTR gereiht (bei gleichem TTR ausgelost) und wie alle
     * Runden „Erster gegen Letzten“ gepaart; ein Freilos bekommt der Beste der unteren Hälfte.
     */
    public void drawBadeni(PlayingClass c, java.util.Random random) {
        requireNotStarted(c);
        c.setExempt(List.of());
        if (c.isDoubles()) throw new IllegalStateException("Das Schweizer System nach Badeni gibt es für Doppel und 2er/3er Turniere nicht.");
        List<Entrant> entrants = new java.util.ArrayList<>(entrantsOf(c));
        if (entrants.size() < 2) throw new IllegalArgumentException("Dafür braucht es mindestens 2 Teilnehmende.");
        java.util.Collections.shuffle(entrants, random);
        entrants.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        c.setMode(TournamentMode.SWISS);
        c.setBadeni(true);
        c.setSwissPaused(List.of());
        c.setGroups(List.of(entrants.stream().map(Entrant::id).toList()), true);
        c.setLotSeed(random.nextLong() | 1L);
        c.setSwissByes(List.of());
        c.setReady(false);
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        List<String> ids = entrants.stream().map(Entrant::id).toList();
        addSwissRound(c, 1, BadeniSystem.nextRound(ids, java.util.Map.of(), java.util.Set.of(), java.util.Set.of()));
        markModified();
    }

    private void addSwissRound(PlayingClass c, int round, SwissSystem.Round r) {
        int no = matchesOf(c).size();
        List<Match> created = new java.util.ArrayList<>();
        for (String[] p : r.pairs()) {
            created.add(new Match(null, c.getName(), "Runde " + round, round, ++no, -1, -1, 0, p[0], p[1]));
        }
        c.addSwissBye(r.bye());
        matches.addAll(created);
    }

    /** Ist eine Runde komplett gespielt, wird die nächste nach der aktuellen Tabelle gepaart und eingereiht. */
    private void advanceSwiss(PlayingClass c) {
        if (c.getMode() != TournamentMode.SWISS) return;
        int round = swissRound(c);
        if (round >= swissRounds(c)) return;
        if (!matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE)) return;
        List<GroupStanding.Row> table = standings(c, 0);
        List<String> ranked = table.stream().map(r -> r.entrant().id())
                .filter(id -> !c.getWithdrawn().contains(id) && !c.getSwissPaused().contains(id)).toList();
        java.util.Set<String> hadBye = new java.util.HashSet<>();
        for (String b : c.getSwissByes()) if (b != null) hadBye.add(b);
        if (ranked.size() < 2) return;
        if (c.isBadeni()) {
            java.util.Map<String, Integer> wins = new java.util.HashMap<>();
            for (GroupStanding.Row r : table) wins.put(r.entrant().id(), r.won() + r.byes());
            addSwissRound(c, round + 1, BadeniSystem.nextRound(ranked, wins, SwissSystem.playedPairs(matchesOf(c)), hadBye));
        } else {
            addSwissRound(c, round + 1, SwissSystem.nextRound(ranked, SwissSystem.playedPairs(matchesOf(c)), hadBye));
        }
        if (c.isStarted()) queueReady(c);
    }

    // --- Auslosung von Hand ändern (vor dem Start der Phase) ---------------------

    /** Verschiebt eine Partei in eine andere Gruppe (Spiele werden neu angelegt). */
    public void moveGroupMember(PlayingClass c, String entrantId, int targetGroup) {
        requireNotStarted(c);
        List<List<String>> groups = new java.util.ArrayList<>();
        for (List<String> g : c.getGroups()) groups.add(new java.util.ArrayList<>(g));
        if (targetGroup < 0 || targetGroup >= groups.size()) throw new IllegalArgumentException("Diese Gruppe gibt es nicht.");
        int from = -1;
        for (int g = 0; g < groups.size(); g++) if (groups.get(g).contains(entrantId)) from = g;
        if (from < 0) throw new IllegalArgumentException("Nicht in dieser Auslosung.");
        if (from == targetGroup) return;
        if (groups.get(from).size() <= 2) {
            throw new IllegalStateException("Gruppe " + groupLabel(c, from) + " braucht mindestens 2 Mitglieder – "
                    + "stattdessen auf ein Mitglied der anderen Gruppe ziehen (Tausch).");
        }
        groups.get(from).remove(entrantId);
        groups.get(targetGroup).add(entrantId);
        c.setGroups(groups, c.isDrawSeeded());
        createGroupMatches(c);
        markModified();
    }

    /** Tauscht zwei Parteien zwischen (oder innerhalb) der Gruppen. */
    public void swapGroupMembers(PlayingClass c, String idA, String idB) {
        requireNotStarted(c);
        List<List<String>> groups = new java.util.ArrayList<>();
        for (List<String> g : c.getGroups()) groups.add(new java.util.ArrayList<>(g));
        int[] pa = null, pb = null;
        for (int g = 0; g < groups.size(); g++) {
            int ia = groups.get(g).indexOf(idA), ib = groups.get(g).indexOf(idB);
            if (ia >= 0) pa = new int[]{g, ia};
            if (ib >= 0) pb = new int[]{g, ib};
        }
        // 1.3 (#106): Freigestellte mit einem Gruppenmitglied tauschen (vor dem Start)
        int ea = c.getExempt().indexOf(idA), eb = c.getExempt().indexOf(idB);
        if ((ea >= 0 && pb != null) || (eb >= 0 && pa != null) || (ea >= 0 && eb >= 0)) {
            List<String> ex = new java.util.ArrayList<>(c.getExempt());
            if (ea >= 0 && eb >= 0) {
                java.util.Collections.swap(ex, ea, eb);
            } else if (ea >= 0) {
                ex.set(ea, idB);
                groups.get(pb[0]).set(pb[1], idA);
            } else {
                ex.set(eb, idA);
                groups.get(pa[0]).set(pa[1], idB);
            }
            c.setExempt(ex);
            c.setGroups(groups, c.isDrawSeeded());
            createGroupMatches(c);
            markModified();
            return;
        }
        if (pa == null || pb == null) throw new IllegalArgumentException("Nicht in dieser Auslosung.");
        groups.get(pa[0]).set(pa[1], idB);
        groups.get(pb[0]).set(pb[1], idA);
        c.setGroups(groups, c.isDrawSeeded());
        createGroupMatches(c);
        markModified();
    }

    /** Tauscht zwei Plätze im KO-Feld (auch mit einem Freilos); zwei Freilose dürfen nicht aufeinandertreffen. */
    public void swapKnockoutSlots(PlayingClass c, int posA, int posB) {
        List<String> slots = new java.util.ArrayList<>(c.getDraw());
        if (posA < 0 || posB < 0 || posA >= slots.size() || posB >= slots.size() || posA == posB) return;
        if (c.isStarted() && c.isPaused()) {
            java.util.Collections.swap(slots, posA, posB);
            requireNoDoubleBye(slots);
            rebuildFirstRound(c, slots, java.util.Set.of(posA / 2, posB / 2));
            return;
        }
        if (c.isStarted()) {
            requireEditableFirstRound(c, posA / 2);
            requireEditableFirstRound(c, posB / 2);
            java.util.Collections.swap(slots, posA, posB);
            c.setDraw(slots, c.isDrawSeeded());
            for (int p : new int[]{posA / 2, posB / 2}) {
                Match m = koMatch(c, 0, p);
                m.setA(slots.get(2 * p));
                m.setB(slots.get(2 * p + 1));
            }
            markModified();
            return;
        }
        java.util.Collections.swap(slots, posA, posB);
        for (int i = 0; i + 1 < slots.size(); i += 2) {
            if (slots.get(i) == null && slots.get(i + 1) == null) {
                throw new IllegalStateException("So träfen zwei Freilose aufeinander.");
            }
        }
        c.setDraw(slots, c.isDrawSeeded());
        createKnockoutMatches(c);
        markModified();
    }

    /** Tauscht zwei ganze Erstrunden-Paarungen (Index 0 = oberste). */
    public void swapKnockoutPairings(PlayingClass c, int pairA, int pairB) {
        List<String> slots = new java.util.ArrayList<>(c.getDraw());
        if (pairA == pairB || pairA < 0 || pairB < 0 || 2 * Math.max(pairA, pairB) + 1 >= slots.size()) return;
        if (c.isStarted() && c.isPaused()) {
            java.util.Collections.swap(slots, 2 * pairA, 2 * pairB);
            java.util.Collections.swap(slots, 2 * pairA + 1, 2 * pairB + 1);
            rebuildFirstRound(c, slots, java.util.Set.of(pairA, pairB));
            return;
        }
        if (c.isStarted()) {
            requireEditableFirstRound(c, pairA);
            requireEditableFirstRound(c, pairB);
            java.util.Collections.swap(slots, 2 * pairA, 2 * pairB);
            java.util.Collections.swap(slots, 2 * pairA + 1, 2 * pairB + 1);
            c.setDraw(slots, c.isDrawSeeded());
            for (int p : new int[]{pairA, pairB}) {
                Match m = koMatch(c, 0, p);
                m.setA(slots.get(2 * p));
                m.setB(slots.get(2 * p + 1));
            }
            markModified();
            return;
        }
        java.util.Collections.swap(slots, 2 * pairA, 2 * pairB);
        java.util.Collections.swap(slots, 2 * pairA + 1, 2 * pairB + 1);
        c.setDraw(slots, c.isDrawSeeded());
        createKnockoutMatches(c);
        markModified();
    }

    private static void requireNoDoubleBye(List<String> slots) {
        for (int i = 0; i + 1 < slots.size(); i += 2) {
            if (slots.get(i) == null && slots.get(i + 1) == null) throw new IllegalStateException("So träfen zwei Freilose aufeinander.");
        }
    }

    /**
     * Angehaltene KO-Phase umsortieren: Die betroffenen Erstrunden-Paarungen werden neu gebildet – nicht mehr gültige
     * Spiele (gleich welcher Status, auch pausiert oder gespielt) werden gelöscht bzw. zurückgesetzt, ebenso die
     * davon abhängigen Spiele späterer Runden. Neue Spiele werden beim Fortsetzen eingereiht.
     */
    private void rebuildFirstRound(PlayingClass c, List<String> slots, java.util.Set<Integer> pairs) {
        c.setDraw(slots, c.isDrawSeeded());
        int size = slots.size();
        int no = matchesOf(c).stream().mapToInt(Match::getNumber).max().orElse(0);
        for (int p : pairs) {
            Match m0 = koMatch(c, 0, p);
            String a = slots.get(2 * p), b = slots.get(2 * p + 1);
            boolean need = a != null && b != null;
            if (m0 != null) {
                m0.clearScore();
                if (need) {
                    m0.setA(a);
                    m0.setB(b);
                } else {
                    matches.remove(m0);
                }
            } else if (need) {
                matches.add(new Match(null, c.getName(), koStageName(c, 0, size), 1, ++no, 0, p, -1, a, b));
            }
            for (int r = 1, idx = p / 2; ; r++, idx /= 2) {
                Match x = koMatch(c, r, idx);
                if (x == null) break;
                if (x.getStatus() != MatchStatus.PLANNED) x.clearScore();
            }
        }
        PlayingClass root = rootOf(c);
        if (root.isFinished()) root.setFinishedAt(null);
        resolveKnockout(c);
        dispatch();
        markModified();
    }

    /**
     * Läuft die Phase schon, lässt sich eine Erstrunden-Paarung nur ändern, solange ihr Spiel noch nicht läuft und
     * nicht gespielt ist (und es kein Freilos ist – dessen Gewinner steht schon in Runde 2).
     */
    private void requireEditableFirstRound(PlayingClass c, int pairIndex) {
        Match m = koMatch(c, 0, pairIndex);
        if (m == null) throw new IllegalStateException("Freilose lassen sich nach dem Start nicht mehr verschieben.");
        if (m.getStatus() == MatchStatus.DONE || m.getStatus().atTable()) {
            throw new IllegalStateException("Spiel " + m.getNumber() + " läuft schon bzw. ist gespielt – zum Umsortieren die Phase "
                    + "anhalten (oder das Ergebnis korrigieren).");
        }
    }

    /** IDs mit Vereinsgleichheit: in derselben Gruppe bzw. als Erstrundengegner im KO-System. */
    public java.util.Set<String> clubClashes(PlayingClass c) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (c.isPhase() || c.isStarted() || c.isDoubles()) return set;    // nur Phase 1 vor dem Start, nie bei Doppeln
        if (!c.getGroups().isEmpty() && c.getMode() != TournamentMode.SWISS) {
            for (List<Entrant> g : groupsOf(c)) set.addAll(GroupDraw.clubClashes(g));
        } else if (!c.getDraw().isEmpty()) {
            List<Entrant> slots = drawSlots(c);
            for (int pos : KnockoutDraw.clubClashPositions(slots)) set.add(slots.get(pos).id());
        }
        return set;
    }

    /** Gruppen der gespeicherten Auslosung; nicht mehr vorhandene Parteien erscheinen als „(entfernt)“. */
    public List<List<Entrant>> groupsOf(PlayingClass c) {
        java.util.Map<String, Entrant> byId = entrantIndex(c);
        List<List<Entrant>> result = new java.util.ArrayList<>();
        for (List<String> g : c.getGroups()) {
            result.add(g.stream().map(id -> byId.getOrDefault(id, new Entrant(id, "(entfernt)", null, null))).toList());
        }
        return result;
    }

    /** Alle Parteien, die in Spielen dieser Klasse/Phase vorkommen können (Personen und Doppel/Teams der Klasse). */
    public java.util.Map<String, Entrant> entrantIndex(PlayingClass c) {
        java.util.Map<String, Entrant> byId = new java.util.HashMap<>();
        for (Player p : players) byId.put(p.getId(), Entrant.of(p));
        for (DoublesPair pair : pairsIn(c)) byId.put("pair:" + pair.getId(), c.isTeam() ? Entrant.ofTeam(pair) : Entrant.of(pair));
        return byId;
    }

    // --- Spiele -----------------------------------------------------------------

    public ObservableList<Match> getMatches() {
        return matches;
    }

    /** Spiele der Klasse, nach Spielnummer. */
    public List<Match> matchesOf(PlayingClass c) {
        return matches.stream().filter(m -> m.getClassName().equals(c.getName()))
                .sorted(java.util.Comparator.comparingInt(Match::getNumber)).toList();
    }

    /** Gruppenspiele „jeder gegen jeden“, Runde für Runde über alle Gruppen nummeriert (große Gruppen beginnen früher). */
    private void createGroupMatches(PlayingClass c) {
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        List<List<GroupDraw.Pairing>> perGroup = new java.util.ArrayList<>();
        for (List<Entrant> g : groupsOf(c)) perGroup.add(GroupDraw.roundRobin(g));
        int maxRound = perGroup.stream().flatMap(List::stream).mapToInt(GroupDraw.Pairing::round).max().orElse(0);
        // 0.38 (#46): auf ein gemeinsames Ende ausrichten – eine Gruppe beginnt um so viele Runden früher, wie sie mehr
        // Runden hat (z. B. 5er-Gruppen spielen Runde 1–2 vorweg, danach alle gemeinsam). Spielnummern folgen dieser Reihenfolge.
        int[] offset = new int[perGroup.size()];
        for (int gi = 0; gi < perGroup.size(); gi++) {
            int rounds = perGroup.get(gi).stream().mapToInt(GroupDraw.Pairing::round).max().orElse(0);
            offset[gi] = maxRound - rounds;
        }
        int no = 0;
        List<Match> created = new java.util.ArrayList<>();
        for (int slot = 1; slot <= maxRound; slot++) {
            for (int gi = 0; gi < perGroup.size(); gi++) {
                int r = slot - offset[gi];
                if (r < 1) continue;
                for (GroupDraw.Pairing p : perGroup.get(gi)) {
                    if (p.round() == r) {
                        created.add(new Match(null, c.getName(), "Gruppe " + groupLabel(c, gi), r, ++no,
                                -1, -1, gi, p.a().id(), p.b().id()));
                    }
                }
            }
        }
        matches.addAll(created);
    }

    /** KO-Spiele aller Runden (ohne Freilos-Begegnungen); spätere Runden zunächst ohne Gegner. */
    /** Rundenname; in der KO-Qualifikation „Qualifikation Runde n“. */
    private static String koStageName(PlayingClass c, int roundNo, int inRound) {
        return c.getMode() == TournamentMode.QUALIFICATION ? "Qualifikation Runde " + (roundNo + 1) : KnockoutDraw.roundName(inRound);
    }

    /** Anzahl KO-Runden, die gespielt werden (Qualifikation: begrenzt, sonst bis zum Finale). */
    public int koRoundsPlayed(PlayingClass c) {
        int full = Integer.numberOfTrailingZeros(Math.max(2, c.getDraw().size()));
        return c.getMode() == TournamentMode.QUALIFICATION && c.getQualRounds() > 0 ? Math.min(full, c.getQualRounds()) : full;
    }

    private void createKnockoutMatches(PlayingClass c) {
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        List<String> slots = c.getDraw();
        int size = slots.size();
        int no = 0;
        List<Match> created = new java.util.ArrayList<>();
        int roundNo = 0;
        int limit = koRoundsPlayed(c);
        for (int inRound = size; inRound >= 2 && roundNo < limit; inRound /= 2, roundNo++) {
            String name = koStageName(c, roundNo, inRound);
            for (int i = 0; i < inRound / 2; i++) {
                if (roundNo == 0) {
                    String a = slots.get(2 * i), b = slots.get(2 * i + 1);
                    if (a == null || b == null) continue;           // Freilos: kein Spiel
                    created.add(new Match(null, c.getName(), name, 1, ++no, 0, i, -1, a, b));
                } else {
                    created.add(new Match(null, c.getName(), name, roundNo + 1, ++no, roundNo, i, -1, null, null));
                }
            }
        }
        matches.addAll(created);
        if (c.isThirdPlace()) {
            if (canHaveThirdPlace(c)) addThirdPlaceMatch(c);
            else c.setThirdPlace(false);
        }
        resolveKnockout(c);
    }

    // --- Spiel um Platz 3 ----------------------------------------------------------

    public static final String THIRD_PLACE_STAGE = "Spiel um Platz 3";

    /** Runde (koRound) der Halbfinals oder -1 (Feld kleiner als 4). */
    private int semifinalRound(PlayingClass c) {
        int size = c.getDraw().size();
        if (size < 4) return -1;
        return Integer.numberOfTrailingZeros(size) - 2;
    }

    /** Spiel um Platz 3 möglich: KO-System (nicht KO-Qualifikation) mit zwei echten Halbfinalspielen. */
    public boolean canHaveThirdPlace(PlayingClass c) {
        if (c.getMode() != TournamentMode.KNOCKOUT) return false;
        int sf = semifinalRound(c);
        if (sf < 0) return false;
        if (sf > 0) return true;     // ab Runde 2 sind die Halbfinals immer echte Spiele
        List<String> slots = c.getDraw();
        for (int i = 0; i < 4; i++) if (slots.get(i) == null) return false;   // Halbfinale mit Freilos
        return true;
    }

    public Match thirdPlaceMatch(PlayingClass c) {
        for (Match m : matches) {
            if (m.getClassName().equals(c.getName()) && m.getKoRound() < 0 && m.getPlacement() == 3) return m;
        }
        return null;
    }

    private void addThirdPlaceMatch(PlayingClass c) {
        if (thirdPlaceMatch(c) != null) return;
        int sf = semifinalRound(c);
        int no = matchesOf(c).stream().mapToInt(Match::getNumber).max().orElse(0) + 1;
        Match third = new Match(null, c.getName(), THIRD_PLACE_STAGE, sf + 2, no, -1, -1, -1, null, null);
        third.restorePhaseInfo(3, false);
        // 1.8.0 (#158): Spielnummern bleiben fest – Platz 3 bekommt die Nummer nach dem Finale (kein Tausch mehr);
        // aufgerufen wird es trotzdem vor dem Finale (siehe queueReady)
        matches.add(third);
    }

    /**
     * Spiel um Platz 3 zu- oder abschalten – auch während die Phase läuft. Stehen die Halbfinals schon fest, wird das
     * Spiel sofort angelegt und eingereiht. Abschalten nur, solange es noch nicht läuft bzw. gespielt ist.
     */
    public void setThirdPlace(PlayingClass c, boolean on) {
        if (on == c.isThirdPlace() && (on == (thirdPlaceMatch(c) != null))) return;
        if (rootOf(c).isFinished()) throw new IllegalStateException("Die Klasse ist abgeschlossen.");
        if (on) {
            if (!canHaveThirdPlace(c)) {
                throw new IllegalStateException(c.getMode() == TournamentMode.KNOCKOUT
                        ? "Ein Spiel um Platz 3 braucht zwei Halbfinalspiele (mindestens 4 Teilnehmende, kein Freilos im Halbfinale)."
                        : "Ein Spiel um Platz 3 gibt es nur im KO-System.");
            }
            c.setThirdPlace(true);
            if (c.isDrawn()) {
                addThirdPlaceMatch(c);
                resolveKnockout(c);
            }
        } else {
            Match m = thirdPlaceMatch(c);
            if (m != null && (m.getStatus() == MatchStatus.DONE || m.getStatus().atTable())) {
                throw new IllegalStateException("Das Spiel um Platz 3 läuft schon bzw. ist gespielt – zum Abschalten erst das Ergebnis "
                        + "bzw. das Spiel zurückziehen.");
            }
            if (m != null) matches.remove(m);
            c.setThirdPlace(false);
        }
        dispatch();
        markModified();
    }

    /** Trägt die Halbfinalverlierer ins Spiel um Platz 3 ein; ändern sie sich, wird es zurückgesetzt. */
    private void resolveThirdPlace(PlayingClass c) {
        Match third = thirdPlaceMatch(c);
        if (third == null) return;
        int sf = semifinalRound(c);
        Match s0 = koMatch(c, sf, 0), s1 = koMatch(c, sf, 1);
        String la = s0 == null ? null : s0.loser(), lb = s1 == null ? null : s1.loser();
        if (!java.util.Objects.equals(la, third.getA()) || !java.util.Objects.equals(lb, third.getB())) {
            if (third.getStatus() != MatchStatus.PLANNED) third.clearScore();
            third.setA(la);
            third.setB(lb);
        }
    }

    private Match koMatch(PlayingClass c, int round, int index) {
        for (Match m : matches) {
            if (m.getClassName().equals(c.getName()) && m.getKoRound() == round && m.getKoIndex() == index) return m;
        }
        return null;
    }

    /**
     * Trägt die Sieger (bzw. Freilos-Gewinner) in die Spiele der nächsten Runde ein. Läuft die Klasse,
     * werden Spiele, deren Gegner nun feststehen, eingereiht.
     */
    private void resolveKnockout(PlayingClass c) {
        List<String> slots = c.getDraw();
        if (slots.isEmpty()) return;
        int size = slots.size();
        int round = 1;
        for (int inRound = size / 2; inRound >= 2; inRound /= 2, round++) {
            for (int i = 0; i < inRound / 2; i++) {
                Match m = koMatch(c, round, i);
                if (m == null) continue;
                m.setA(advancing(c, slots, round - 1, 2 * i));
                m.setB(advancing(c, slots, round - 1, 2 * i + 1));
            }
        }
        resolveThirdPlace(c);
        if (c.isStarted()) queueReady(c);
    }

    /** Wer aus Spiel (round, index) weiterkommt: bei Freilos der Gegner des Freiloses, sonst der Sieger. */
    private String advancing(PlayingClass c, List<String> slots, int round, int index) {
        if (round == 0) {
            String a = slots.get(2 * index), b = slots.get(2 * index + 1);
            if (a == null) return b;
            if (b == null) return a;
        }
        Match m = koMatch(c, round, index);
        return m == null ? null : m.winner();
    }

    // --- Klassen starten, Turnier starten, Tische ---------------------------------

    private final javafx.beans.property.IntegerProperty tableCount = new javafx.beans.property.SimpleIntegerProperty(1);
    private long queueCounter;

    public javafx.beans.property.IntegerProperty tableCountProperty() { return tableCount; }
    public int getTableCount() { return tableCount.get(); }
    public void setTableCount(int n) { tableCount.set(Math.max(1, n)); markModified(); }

    /** Klasse als bereit markieren (nur nach der Auslosung, vor dem Start). */
    public void setClassReady(PlayingClass c, boolean ready) {
        if (ready && !c.isDrawn()) throw new IllegalStateException("„" + c.getName() + "“ ist noch nicht ausgelost.");
        requireNotStarted(c);
        c.setReady(ready);
        markModified();
    }

    /**
     * Startet eine Klasse: hält die Startzeit fest (und – falls noch nicht geschehen – die Startzeit des Turniers),
     * reiht die Spiele ein und belegt freie Tische.
     */
    public void startClass(PlayingClass c) {
        de.hts.Log.info("Klasse gestartet: " + c.getName());   // 1.7.0 (#140)
        if (!c.isDrawn()) throw new IllegalStateException("„" + c.getName() + "“ ist noch nicht ausgelost.");
        requireNotStarted(c);
        requireHandicapStartable(c);
        String attendance = attendanceProblem(c);
        if (attendance != null) throw new IllegalStateException(attendance);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        c.setStartedAt(now);
        c.setReady(true);
        if (!isStarted()) startedAt.set(now);
        if (c.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(c);
        else if (c.getMode() == TournamentMode.PLAN) resolvePlan(c);
        else queueReady(c);
        dispatch();
        markModified();
    }

    /**
     * Ergebnis von {@link #startTournament()}; {@code attendanceOpen}: bereite Klassen „aus XML importiert“, die wegen
     * noch nicht an- oder abgemeldeter Gemeldeter nicht starten (0.35).
     */
    public record StartReport(List<PlayingClass> started, List<PlayingClass> notReady, List<PlayingClass> alreadyRunning,
                              List<PlayingClass> attendanceOpen) {
        public StartReport(List<PlayingClass> started, List<PlayingClass> notReady, List<PlayingClass> alreadyRunning) {
            this(started, notReady, alreadyRunning, List.of());
        }
    }

    /** Was beim Turnierstart passieren würde (für den Hinweis vorab). */
    public StartReport previewStart() {
        List<PlayingClass> ready = new java.util.ArrayList<>(), notReady = new java.util.ArrayList<>(),
                running = new java.util.ArrayList<>();
        List<PlayingClass> open = new java.util.ArrayList<>();
        for (PlayingClass c : classes) {
            if (c.isPhase()) continue;
            if (c.isStarted()) running.add(c);
            else if (c.isReady() && c.isDrawn() && !openAttendance(c).isEmpty()) open.add(c);
            else if (c.isReady() && c.isDrawn()) ready.add(c);
            else notReady.add(c);
        }
        return new StartReport(ready, notReady, running, open);
    }

    /** Startet das Turnier: alle bereiten Klassen werden gestartet und ihre Spiele eingereiht. */
    public StartReport startTournament() {
        StartReport r = previewStart();
        for (PlayingClass c : r.started()) requireHandicapStartable(c);      // erst alles prüfen, dann starten
        if (!isStarted()) startedAt.set(LocalDateTime.now().withNano(0));
        for (PlayingClass c : r.started()) startClass(c);
        markModified();
        return r;
    }

    /** Läuft mindestens eine Klasse (bzw. ist das Turnier gestartet)? */
    public boolean isRunning() {
        return isStarted() || classes.stream().anyMatch(PlayingClass::isStarted);
    }

    /** Reiht alle geplanten Spiele der (laufenden) Klasse ein, deren Gegner feststehen. */
    private void queueReady(PlayingClass c) {
        if (c.isPaused()) return;
        boolean sched = hasSchedule(c);
        if (sched && c.getScheduleAnchor() == null) c.setScheduleAnchor(LocalDateTime.now().withNano(0));
        List<Match> ms = matchesOf(c);
        roundCache = sched ? ms : null;                 // 1.2: Runden nicht je Spiel neu aus allen Spielen suchen
        try {
            for (Match m : queueOrderOf(c, ms)) {
                if (m.getStatus() == MatchStatus.PLANNED && m.isReady() && (!sched || roundOpen(c, m))) {
                    m.setStatus(MatchStatus.QUEUED);
                    if (m.getQueueOrder() == 0) m.setQueueOrder(++queueCounter);   // nach Anhalten: alter Platz bleibt
                }
            }
        } finally {
            roundCache = null;
        }
    }

    /**
     * 1.8.0 (#158): Reihenfolge beim Einreihen – nach Spielnummer, nur das Spiel um Platz 3 kommt direkt vor das Finale
     * (es hat die höhere Nummer, wird aber wie bisher zuerst aufgerufen).
     */
    private List<Match> queueOrderOf(PlayingClass c, List<Match> ms) {
        Match third = null;
        for (Match m : ms) if (THIRD_PLACE_STAGE.equals(m.getStage())) third = m;
        if (third == null) return ms;
        Match fin = c.getMode() == TournamentMode.KNOCKOUT ? koMatch(c, semifinalRound(c) + 1, 0) : null;
        if (fin == null || fin == third) return ms;
        List<Match> out = new java.util.ArrayList<>(ms);
        out.remove(third);
        int i = out.indexOf(fin);
        out.add(i < 0 ? out.size() : i, third);
        return out;
    }

    /** Spiele der Klasse während {@link #queueReady} (sonst {@code null}). */
    private List<Match> roundCache;

    // --- 1.0 (#77): Zeitplan ---------------------------------------------------------------------------

    /**
     * Rückfrage: Runde fertig, die nächste Runde liegt laut Zeitplan noch in der Zukunft.
     * @param label z. B. „Jugend 13 Einzel · Gruppe A · Runde 3“
     */
    public record ScheduleQuestion(PlayingClass phase, String key, String label, LocalDateTime planned) {
    }

    private final List<ScheduleQuestion> scheduleQuestions = new java.util.ArrayList<>();

    /** Zeitplan aktiv: Hauptklasse mit Minuten je Runde; die Qualifikation startet ihre Runden ohnehin einzeln. */
    public boolean hasSchedule(PlayingClass phase) {
        Integer min = rootOf(phase).getScheduleMinutes();
        return min != null && min > 0 && phase.getMode() != TournamentMode.QUALIFICATION;
    }

    /** Schlüssel der Runde eines Spiels: Gruppen je Gruppe („g0:2“), sonst die Runde („3“). */
    public static String roundKey(PlayingClass phase, Match m) {
        return phase.getMode() == TournamentMode.GROUPS && m.getGroupIndex() >= 0 ? "g" + m.getGroupIndex() + ":" + m.getRound()
                : String.valueOf(m.getRound());
    }

    private List<Match> roundMatches(PlayingClass phase, Match like, int round) {
        boolean groups = phase.getMode() == TournamentMode.GROUPS && like.getGroupIndex() >= 0;
        List<Match> base = roundCache != null && !roundCache.isEmpty() && roundCache.get(0).getClassName().equals(phase.getName())
                ? roundCache : matchesOf(phase);
        return base.stream().filter(x -> x.getRound() == round && (!groups || x.getGroupIndex() == like.getGroupIndex())).toList();
    }

    /** Zeitfenster der Runde (1 = erstes): bei Gruppen mit Versatz für das gemeinsame Ende. */
    private int slotOf(PlayingClass phase, Match m) {
        if (phase.getMode() != TournamentMode.GROUPS || m.getGroupIndex() < 0) return Math.max(1, m.getRound());
        int max = 0, mine = 0;
        for (Match x : roundCache != null && !roundCache.isEmpty() && roundCache.get(0).getClassName().equals(phase.getName())
                ? roundCache : matchesOf(phase)) {
            max = Math.max(max, x.getRound());
            if (x.getGroupIndex() == m.getGroupIndex()) mine = Math.max(mine, x.getRound());
        }
        return m.getRound() + (max - mine);
    }

    /** Geplanter Beginn der Runde des Spiels laut Zeitplan. */
    public LocalDateTime plannedStart(PlayingClass phase, Match m) {
        LocalDateTime anchor = scheduleStartOf(phase);
        if (anchor == null) anchor = phase.getScheduleAnchor() == null ? LocalDateTime.now() : phase.getScheduleAnchor();
        return anchor.plusMinutes((long) (slotOf(phase, m) - 1) * rootOf(phase).getScheduleMinutes());
    }

    /** 1.1 (#88): eingestellte Startzeit, gilt für die erste Phase der Klasse; sonst {@code null}. */
    public LocalDateTime scheduleStartOf(PlayingClass phase) {
        PlayingClass root = rootOf(phase);
        if (root.getScheduleStart() == null) return null;
        List<PlayingClass> ph = phasesOf(root);
        return !ph.isEmpty() && ph.get(0) == phase ? root.getScheduleStart() : null;
    }

    /**
     * 1.1 (#86): Startzeit der Runde zum Anzeigen – nur mit Zeitplan und wenn die Zeit feststeht (Phase gestartet oder
     * Startzeit eingestellt); sonst {@code null}.
     */
    public LocalDateTime displayedStart(PlayingClass phase, Match m) {
        if (phase == null || m == null || !hasSchedule(phase)) return null;
        if (scheduleStartOf(phase) == null && (phase.getScheduleAnchor() == null || !phase.isStarted())) return null;
        return plannedStart(phase, m);
    }

    /** Anzeige „ab 14:25“ für ein Spiel (leer ohne Zeitplan bzw. vor dem Start). */
    public String startText(Match m) {
        PlayingClass phase = m == null ? null : findClass(m.getClassName()).orElse(null);
        LocalDateTime t = displayedStart(phase, m);
        return t == null ? "" : "ab " + t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** 1.1 (#88): Uhrzeit → nächstgelegener Zeitpunkt (höchstens 12 Stunden vor oder nach {@code now}). */
    public static LocalDateTime nearestTime(java.time.LocalTime time, LocalDateTime now) {
        LocalDateTime best = null;
        for (int d = -1; d <= 1; d++) {
            LocalDateTime c = now.toLocalDate().plusDays(d).atTime(time.withSecond(0).withNano(0));
            if (best == null || Math.abs(java.time.Duration.between(now, c).toMinutes()) < Math.abs(java.time.Duration.between(now, best).toMinutes())) best = c;
        }
        return best;
    }

    /**
     * 1.1 (#88): Startzeit des Zeitplans setzen ({@code null} = ab Start der Klasse); auch nach dem Start – noch nicht
     * freigegebene Runden bekommen damit neue Zeiten, laufende bleiben.
     */
    public void setScheduleStart(PlayingClass root, LocalDateTime start) {
        root.setScheduleStart(start);
        List<PlayingClass> ph = phasesOf(root);
        if (!ph.isEmpty() && ph.get(0).isStarted() && hasSchedule(ph.get(0))) {
            queueReady(ph.get(0));
            dispatch();
        }
        markModified();
    }

    private boolean previousRoundDone(PlayingClass phase, Match m) {
        if (m.getRound() <= 1) return true;
        List<Match> prev = roundMatches(phase, m, m.getRound() - 1);
        return prev.stream().allMatch(x -> x.getStatus() == MatchStatus.DONE || x.getStatus() == MatchStatus.CANCELLED);
    }

    /** Darf die Runde des Spiels eingereiht werden? Gibt sie frei, wenn ihre Zeit gekommen ist, sonst ggf. Rückfrage. */
    private boolean roundOpen(PlayingClass phase, Match m) {
        String key = roundKey(phase, m);
        if (phase.getReleasedRounds().contains(key)) return true;
        if (!previousRoundDone(phase, m)) return false;
        LocalDateTime planned = plannedStart(phase, m);
        if (!LocalDateTime.now().isBefore(planned)) {
            phase.getReleasedRounds().add(key);
            return true;
        }
        // vor dem Zeitplan: nach einer beendeten Runde fragen (Runde 1 mit Versatz wartet still auf ihre Zeit)
        if (m.getRound() > 1 && phase.getAskedRounds().add(key)) {
            String what = phase.getMode() == TournamentMode.GROUPS && m.getGroupIndex() >= 0
                    ? m.getStage() + " · Runde " + m.getRound() : m.getStage();
            scheduleQuestions.add(new ScheduleQuestion(phase, key, rootOf(phase).getName() + " · " + what, planned));
        }
        return false;
    }

    /** Offene Rückfragen „vor dem Zeitplan“ abholen (die Oberfläche fragt nach). */
    public List<ScheduleQuestion> takeScheduleQuestions() {
        List<ScheduleQuestion> out = List.copyOf(scheduleQuestions);
        scheduleQuestions.clear();
        return out;
    }

    /** Runde jetzt starten (Antwort „Ja“ bzw. Knopf „Runde jetzt starten“). */
    public void releaseRound(PlayingClass phase, String key) {
        phase.getReleasedRounds().add(key);
        queueReady(phase);
        dispatch();
        markModified();
    }

    /** Wartende Runden einer Phase (nach Rückfrage „Warten“): Schlüssel → Bezeichnung und geplanter Beginn. */
    public List<ScheduleQuestion> waitingRounds(PlayingClass phase) {
        if (!hasSchedule(phase) || !phase.isStarted() || phase.isFinished()) return List.of();
        List<ScheduleQuestion> out = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Match m : matchesOf(phase)) {
            String key = roundKey(phase, m);
            if (m.getStatus() != MatchStatus.PLANNED || phase.getReleasedRounds().contains(key) || !seen.add(key)) continue;
            if (!previousRoundDone(phase, m)) continue;
            String what = phase.getMode() == TournamentMode.GROUPS && m.getGroupIndex() >= 0 ? m.getStage() + " · Runde " + m.getRound() : m.getStage();
            out.add(new ScheduleQuestion(phase, key, rootOf(phase).getName() + " · " + what, plannedStart(phase, m)));
        }
        return out;
    }

    /** Regelmäßig aufrufen: Runden, deren geplanter Beginn erreicht ist, einreihen. @return ob sich etwas geändert hat */
    public boolean releaseDueRounds() {
        boolean changed = false;
        for (PlayingClass c : classes) {
            if (!c.isStarted() || c.isPaused() || c.isFinished() || !hasSchedule(c)) continue;
            int before = c.getReleasedRounds().size();
            queueReady(c);
            if (c.getReleasedRounds().size() != before) changed = true;
        }
        if (changed) {
            dispatch();
            markModified();
        }
        return changed;
    }

    /**
     * Hält eine laufende Phase an: ihre eingereihten Spiele gehen zurück auf „geplant“ und belegen keine Tische mehr;
     * Spiele, die gerade an einem Tisch laufen, werden noch zu Ende gespielt. Freie Tische gehen an andere Klassen.
     */
    public void pauseClass(PlayingClass c) {
        if (!c.isStarted()) throw new IllegalStateException("„" + c.getName() + "“ ist noch nicht gestartet.");
        if (c.isPaused()) return;
        c.setPaused(true);
        for (Match m : matchesOf(c)) {
            if (m.getStatus() == MatchStatus.QUEUED) m.setStatus(MatchStatus.PLANNED);
            else if (m.getStatus() == MatchStatus.ACTIVE) m.setStatus(MatchStatus.PAUSED);   // bleibt am Tisch
        }
        dispatch();
        markModified();
    }

    /**
     * Setzt eine angehaltene Phase fort: pausierte Spiele an den Tischen laufen weiter, die übrigen werden wieder
     * (an ihrem alten Platz) eingereiht, freie Tische belegt.
     */
    public void resumeClass(PlayingClass c) {
        if (!c.isPaused()) return;
        c.setPaused(false);
        LocalDateTime fixed = scheduleStartOf(c);
        if (c.isAwaitingStart() && hasSchedule(c) && (fixed == null || !LocalDateTime.now().isBefore(fixed))) {   // 1.0 (#77): von Hand gestartet = die bereiten Runden jetzt
            for (Match m : matchesOf(c)) if (m.getStatus() == MatchStatus.PLANNED && m.isReady()) c.getReleasedRounds().add(roundKey(c, m));
        }
        c.setAwaitingStart(false);
        for (Match m : matchesOf(c)) if (m.getStatus() == MatchStatus.PAUSED) m.setStatus(MatchStatus.ACTIVE);
        queueReady(c);
        dispatch();
        markModified();
    }

    /** Ein pausiertes Spiel an seinem Tisch bewusst fortsetzen (auch wenn die Phase noch angehalten ist). */
    public void resumeMatch(Match m) {
        if (m.getStatus() != MatchStatus.PAUSED) throw new IllegalStateException("Das Spiel ist nicht pausiert.");
        m.setStatus(MatchStatus.ACTIVE);
        markModified();
    }

    /**
     * Zieht ein Spiel vom Tisch zurück: es reiht sich hinten in der Warteschlange wieder ein (in einer angehaltenen
     * Phase: geplant). Der Tisch wird frei und neu belegt.
     */
    public void withdrawMatch(Match m) {
        if (!m.getStatus().atTable()) throw new IllegalStateException("Nur Spiele an einem Tisch können zurückgezogen werden.");
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        m.setTable(null);
        m.setStartedAt(null);
        if (c != null && c.isPaused()) {
            m.setStatus(MatchStatus.PLANNED);
            m.setQueueOrder(++queueCounter);
        } else {
            m.setStatus(MatchStatus.QUEUED);
            m.setQueueOrder(++queueCounter);
        }
        dispatch();
        markModified();
    }

    /** Freie Tische (1 … Anzahl), aufsteigend. */
    public List<Integer> freeTables() {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (Match m : matches) if (m.getStatus().atTable() && m.getTable() != null) used.add(m.getTable());
        List<Integer> free = new java.util.ArrayList<>();
        for (int t = 1; t <= getTableCount(); t++) if (!used.contains(t) && !disabledTables.contains(t)) free.add(t);
        return free;
    }

    // --- Tische abschalten ------------------------------------------------------------
    private final java.util.Set<Integer> disabledTables = new java.util.TreeSet<>();
    private final java.util.Map<Integer, String> tableNames = new java.util.TreeMap<>();
    public static final int TABLE_NAME_MAX = 30;

    /** Anzeigename eines Tisches: eigener Name (bis 30 Zeichen) oder „Tisch n“. */
    public String tableName(int table) {
        String n = tableNames.get(table);
        return n != null ? n : "Tisch " + table;
    }

    /** Anzeigename zur Tischnummer, {@code null} = kein Tisch. */
    public String tableName(Integer table) {
        return table == null ? null : tableName(table.intValue());
    }

    /** Eigener Name (gekürzt auf 30 Zeichen); leer bzw. „Tisch n“ = wieder Standardname. */
    public void setTableName(int table, String name) {
        String n = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (n.length() > TABLE_NAME_MAX) n = n.substring(0, TABLE_NAME_MAX).strip();
        if (n.isEmpty() || n.equals("Tisch " + table)) tableNames.remove(table);
        else tableNames.put(table, n);
        markModified();
    }

    public java.util.Map<Integer, String> getTableNames() {
        return java.util.Collections.unmodifiableMap(tableNames);
    }

    /** Von der Turnierleitung abgeschalteter Tisch: bekommt keine Spiele. */
    public boolean isTableDisabled(int table) {
        return disabledTables.contains(table);
    }

    public java.util.Set<Integer> getDisabledTables() {
        return java.util.Collections.unmodifiableSet(disabledTables);
    }

    /** Tisch ab- bzw. wieder zuschalten; abschalten nur, wenn gerade kein Spiel am Tisch liegt. */
    public void setTableDisabled(int table, boolean disabled) {
        if (disabled) {
            if (activeMatchAt(table).isPresent()) {
                throw new IllegalStateException("An Tisch " + table + " liegt noch ein Spiel – erst das Ergebnis eintragen, "
                        + "das Spiel zurückziehen oder an einen anderen Tisch wechseln.");
            }
            disabledTables.add(table);
        } else {
            disabledTables.remove(table);
            dispatch();
        }
        markModified();
    }

    public Optional<Match> activeMatchAt(int table) {
        return matches.stream().filter(m -> m.getStatus().atTable() && Integer.valueOf(table).equals(m.getTable()))
                .findFirst();
    }

    /** Warteschlange: eingereihte Spiele in Reihenfolge. */
    public List<Match> queue() {
        return matches.stream().filter(m -> m.getStatus() == MatchStatus.QUEUED)
                .sorted(java.util.Comparator.comparingLong(Match::getQueueOrder)).toList();
    }

    /**
     * Belegt freie Tische mit eingereihten Spielen (in Reihenfolge der Warteschlange). Ein Spiel wird
     * übersprungen, solange eine beteiligte Person gerade an einem anderen Tisch spielt.
     */
    public void dispatch() {
        List<Integer> free = new java.util.ArrayList<>(freeTables());
        if (free.isEmpty()) return;
        java.util.Set<String> busy = new java.util.HashSet<>();
        for (Match m : matches) if (m.getStatus().atTable()) busy.addAll(personsOf(m));
        List<Match> activated = new java.util.ArrayList<>();
        for (Match m : queue()) {
            if (free.isEmpty()) break;
            java.util.Set<String> persons = personsOf(m);
            if (persons.stream().anyMatch(busy::contains)) continue;
            // erster freier Tisch, der für die Klasse dieses Spiels freigegeben ist – sonst ein späteres Spiel nehmen
            Integer table = free.stream().filter(t -> tableAccepts(t, m)).findFirst().orElse(null);
            if (table == null) continue;
            free.remove(table);
            m.setStatus(MatchStatus.ACTIVE);
            m.setTable(table);
            m.setStartedAt(LocalDateTime.now().withNano(0));
            busy.addAll(persons);
            activated.add(m);
        }
        markModified();
        if (!simulating) for (Match m : activated) for (java.util.function.Consumer<Match> l : activationListeners) l.accept(m);
    }

    // --- Tische: Zuordnung zu Klassen, Spiele verschieben -------------------------

    /**
     * 0.38: Tisch → ausgeschlossene Klassen (Namen der Klassen, nicht der Phasen). Standard: jede Klasse darf an jedem
     * Tisch spielen (Haken gesetzt); ein Eintrag hier heißt „Haken bewusst entfernt“. Neu angelegte Klassen dürfen
     * daher automatisch überall spielen. Sind alle Klassen ausgeschlossen, bekommt der Tisch keine Spiele, bleibt aber aktiv.
     */
    private final java.util.Map<Integer, java.util.Set<String>> tableClasses = new java.util.TreeMap<>();

    /** Klassen, die an diesem Tisch NICHT spielen; leer = alle Klassen dürfen (Standard). */
    public java.util.Set<String> getTableExcluded(int table) {
        return java.util.Collections.unmodifiableSet(tableClasses.getOrDefault(table, java.util.Set.of()));
    }

    /** Alle Ausschlüsse (nur Tische mit mindestens einer ausgeschlossenen Klasse). */
    public java.util.Map<Integer, java.util.Set<String>> getTableExcludedMap() {
        return java.util.Collections.unmodifiableMap(tableClasses);
    }

    /** Legt fest, welche Klassen an einem Tisch nicht spielen; leere Auswahl = alle Klassen dürfen. */
    public void setTableExcluded(int table, java.util.Collection<String> classNames) {
        if (classNames == null || classNames.isEmpty()) tableClasses.remove(table);
        else tableClasses.put(table, new java.util.TreeSet<>(classNames));
        markModified();
    }

    /** Darf die Klasse (bzw. ihre Phase 1) an diesem Tisch spielen? */
    public boolean tableAllows(int table, String className) {
        java.util.Set<String> ex = tableClasses.get(table);
        if (ex == null || ex.isEmpty()) return true;
        PlayingClass c = findClass(className).orElse(null);
        String name = c == null ? className : rootOf(c).getName();
        return !ex.contains(name);
    }

    /** Ist der Tisch für keine (Haupt-)Klasse freigegeben? Dann bekommt er keine Spiele, bleibt aber aktiv. */
    public boolean tableHasNoClass(int table) {
        java.util.Set<String> ex = tableClasses.get(table);
        if (ex == null || ex.isEmpty()) return false;
        List<PlayingClass> roots = classes.stream().filter(c -> !c.isPhase()).toList();
        return !roots.isEmpty() && roots.stream().allMatch(c -> ex.contains(c.getName()));
    }

    /** Darf das Spiel an diesem Tisch gespielt werden (Klasse des Spiels bzw. ihrer Phase 1 freigegeben)? */
    public boolean tableAccepts(int table, Match m) {
        return tableAllows(table, m.getClassName());
    }

    /**
     * Verschiebt ein laufendes Spiel an einen anderen Tisch. Ist der Zieltisch belegt, tauschen die beiden Spiele.
     * Ein eingereihtes Spiel kann so auch direkt an einen freien Tisch geholt werden (wenn niemand daraus gerade spielt).
     * Die Tischzuordnung zu Klassen gilt hier nicht – die Turnierleitung entscheidet.
     */
    public void moveMatch(Match m, int table) {
        if (table < 1 || table > getTableCount()) throw new IllegalArgumentException("Tisch " + table + " gibt es nicht.");
        Match other = activeMatchAt(table).orElse(null);
        if (other == m) return;
        if (disabledTables.contains(table)) throw new IllegalStateException("Tisch " + table + " ist deaktiviert.");
        if (m.getStatus().atTable()) {
            Integer from = m.getTable();
            if (other != null) other.setTable(from);
            m.setTable(table);
        } else if (m.getStatus() == MatchStatus.QUEUED) {
            if (other != null) throw new IllegalStateException("Tisch " + table + " ist belegt.");
            java.util.Set<String> busy = new java.util.HashSet<>();
            for (Match x : matches) if (x.getStatus().atTable()) busy.addAll(personsOf(x));
            if (personsOf(m).stream().anyMatch(busy::contains)) {
                throw new IllegalStateException("Mindestens eine Person aus diesem Spiel spielt gerade an einem anderen Tisch.");
            }
            m.setStatus(MatchStatus.ACTIVE);
            m.setTable(table);
            m.setStartedAt(LocalDateTime.now().withNano(0));
            for (java.util.function.Consumer<Match> l : activationListeners) l.accept(m);
        } else {
            throw new IllegalStateException("Nur laufende oder eingereihte Spiele können an einen Tisch verschoben werden.");
        }
        markModified();
    }

    private final List<java.util.function.Consumer<Match>> activationListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** während „Alle Spiele simulieren“: keine Spielzettel drucken */
    private boolean simulating;

    /** Wird aufgerufen, sobald ein Spiel an einen Tisch geht (z. B. zum Drucken des Spielzettels). */
    public void addActivationListener(java.util.function.Consumer<Match> l) {
        activationListeners.add(l);
    }

    public void removeActivationListener(java.util.function.Consumer<Match> l) {
        activationListeners.remove(l);
    }

    /** Personen-IDs, die an einem Spiel beteiligt sind (bei Doppeln beide). */
    public java.util.Set<String> personsOf(Match m) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (String e : new String[]{m.getA(), m.getB()}) {
            if (e == null) continue;
            if (e.startsWith("pair:")) {
                String pid = e.substring(5);
                pairs.stream().filter(p -> p.getId().equals(pid)).findFirst().ifPresent(p -> {
                    for (Player x : p.members()) ids.add(x.getId());
                });
            } else {
                ids.add(e);
            }
        }
        return ids;
    }

    // --- Gruppentabellen und Folgephase -----------------------------------------

    /** Tabelle einer Gruppe (0 = A) nach den abgeschlossenen Spielen. */
    public List<GroupStanding.Row> standings(PlayingClass c, int groupIndex) {
        List<List<Entrant>> groups = groupsOf(c);
        if (groupIndex < 0 || groupIndex >= groups.size()) return List.of();
        List<Match> gm = matchesOf(c).stream().filter(m -> m.getGroupIndex() == groupIndex).toList();
        // letztes Kriterium bei völliger Gleichheit: festes, gespeichertes Los (nicht TTR/Setzung); solange noch kein
        // Spiel beendet ist, bleibt die Reihenfolge der Auslosung stehen
        boolean anyDone = gm.stream().anyMatch(m -> m.getStatus() == MatchStatus.DONE);
        java.util.function.ToIntFunction<String> lot = anyDone ? c::lotOf : null;
        if (c.getMode() == TournamentMode.SWISS) {
            List<GroupStanding.Row> rows = SwissSystem.standings(groups.get(groupIndex), gm, c.getSwissByes(), lot);
            return c.isBadeni() ? BadeniSystem.standings(rows) : rows;
        }
        return GroupStanding.compute(groups.get(groupIndex), gm, c.getTiebreak(), lot);
    }

    /** Alle Gruppenspiele abgeschlossen? */
    public boolean isGroupPhaseComplete(PlayingClass c) {
        if (c.getGroups().isEmpty()) return false;
        if (c.getMode() == TournamentMode.SWISS && swissRound(c) < swissRounds(c)) return false;
        List<Match> ms = matchesOf(c);
        return !ms.isEmpty() && ms.stream().allMatch(m -> m.getStatus() == MatchStatus.DONE);
    }

    /** Höchstzahl der Phasen je Klasse. */
    public static final int MAX_PHASES = 3;

    /** Sind alle (ausgelosten) Klassen beendet? Dann können die Gesamtergebnisse exportiert werden. */
    public boolean isCompletelyFinished() {
        List<PlayingClass> drawn = rootClasses().stream().filter(PlayingClass::isDrawn).toList();
        return !drawn.isEmpty() && drawn.stream().allMatch(PlayingClass::isFinished);
    }

    /** Klassen ohne die Folgephasen (Phase 2/3 erscheinen als Reiter ihrer Klasse). */
    public List<PlayingClass> rootClasses() {
        return classes.stream().filter(c -> !c.isPhase()).toList();
    }

    /** Die erste Phase (= die Klasse selbst) zu einer Phase. */
    public PlayingClass rootOf(PlayingClass c) {
        PlayingClass r = c;
        for (int k = 0; k < 10 && r.getSourceClass() != null; k++) {
            PlayingClass src = findClass(r.getSourceClass()).orElse(null);
            if (src == null) break;
            r = src;
        }
        return r;
    }

    /** Alle Phasen der Klasse in Reihenfolge (Phase 1 = die Klasse selbst). */
    public List<PlayingClass> phasesOf(PlayingClass c) {
        List<PlayingClass> list = new java.util.ArrayList<>();
        PlayingClass p = rootOf(c);
        while (p != null && !list.contains(p) && list.size() < 10) {
            list.add(p);
            p = p.getFollowUpClass() == null ? null : findClass(p.getFollowUpClass()).orElse(null);
        }
        return list;
    }

    /** 1, 2 oder 3. */
    public int phaseNumber(PlayingClass c) {
        return phasesOf(c).indexOf(c) + 1;
    }

    /** Alle Spiele der Phase abgeschlossen? */
    public boolean isPhaseComplete(PlayingClass c) {
        if (c.isEndedEarly()) return true;                      // 0.38 (#51)
        if (c.getMode() == TournamentMode.QUALIFICATION) return isQualificationComplete(c);
        if (c.getMode() == TournamentMode.PLAN) {
            return c.getPlan() != null && isPlanComplete(c) && matchesOf(c).stream().allMatch(m -> m.getStatus() == MatchStatus.DONE);
        }
        if (c.getMode() == TournamentMode.SWISS && swissRound(c) < swissRounds(c)) return false;
        List<Match> ms = matchesOf(c);
        return c.isDrawn() && ms.stream().allMatch(m -> m.getStatus() == MatchStatus.DONE)
                && (!ms.isEmpty() || !c.getDraw().isEmpty());
    }

    /** „Gruppenphase mit Platzierungsspielen“ geht nur nach genau 2 Gruppen mit je mindestens 2 Teilnehmenden. */
    public boolean canChoosePlacement(PlayingClass c) {
        return c.getMode() == TournamentMode.GROUPS && c.getGroups().size() == 2
                && c.getGroups().stream().allMatch(g -> g.size() >= 2) && phaseNumber(c) == 1;
    }

    /** Darf nach dieser Phase noch eine weitere folgen (höchstens 3 Phasen, nur nach einer Gruppenphase)? */
    public boolean canHaveNextPhase(PlayingClass c) {
        // Schweizer System: nur Phase 1 und danach keine weitere Phase – nur das Endergebnis
        // 1.2: Phase 3 nur nach „Gruppen nach Leistungsstufen“ (#97)
        if (c.getMode() != TournamentMode.GROUPS) return false;
        int n = phaseNumber(c);
        return n == 1 || (n == 2 && c.isTiered());
    }

    /** Kann aus dieser Gruppenphase die Folgephase erstellt werden? */
    public boolean canCreateNextPhase(PlayingClass c) {
        if (c.getNextPhase() == TournamentMode.NONE || c.getFollowUpClass() != null || !isGroupPhaseComplete(c)) return false;
        if (!canHaveNextPhase(c) || rootOf(c).isFinished()) return false;
        if (phaseNumber(c) > 1 && c.getNextPhase() != TournamentMode.PLAN) return false;
        if (c.getNextPhase() == TournamentMode.QUALIFICATION) return c.getMode() == TournamentMode.GROUPS && phaseNumber(c) == 1;
        return c.getNextPhase() != TournamentMode.PLACEMENT || canChoosePlacement(c);
    }

    /**
     * Nach dem letzten Gruppenspiel: Klasse ohne weitere Phase beenden. Die Platzierungen stehen dann im Endergebnis.
     */
    // --- 0.38: Phase vorzeitig beenden (#51) bzw. komplett neu starten (#58) -----------------------------

    /** Letzte Phase der Klasse. */
    public PlayingClass lastPhase(PlayingClass c) {
        List<PlayingClass> ps = phasesOf(c);
        return ps.get(ps.size() - 1);
    }

    /** Anzahl offener (nicht gespielter) Spiele der Phase. */
    public long openMatchCount(PlayingClass phase) {
        return matchesOf(phase).stream().filter(m -> m.getStatus() != MatchStatus.DONE && m.getStatus() != MatchStatus.CANCELLED).count();
    }

    /** Warum die Phase nicht vorzeitig beendet werden kann – sonst {@code null}. */
    public String endEarlyProblem(PlayingClass phase) {
        if (phase.getFollowUpClass() != null) return "Es gibt schon eine weitere Phase.";
        if (rootOf(phase).isFinished()) return "Die Klasse ist schon beendet.";
        if (!phase.isStarted()) return "Die Phase ist noch nicht gestartet.";
        return null;
    }

    /**
     * Beendet die (letzte) Phase vorzeitig: alle offenen Spiele – auch die an Tischen – werden „nicht gespielt“, zählen
     * in keiner Wertung und werden nicht exportiert; die bisher gespielten bleiben. Die Klasse gilt danach als beendet.
     */
    public void endPhaseEarly(PlayingClass phase) {
        String problem = endEarlyProblem(phase);
        if (problem != null) throw new IllegalStateException(problem);
        for (Match m : matchesOf(phase)) {
            if (m.getStatus() == MatchStatus.DONE) continue;
            m.setTable(null);
            m.setStartedAt(null);
            m.setStatus(MatchStatus.CANCELLED);
        }
        phase.setEndedEarly(true);
        phase.setNextPhase(TournamentMode.NONE);
        PlayingClass root = rootOf(phase);
        root.setFinishedAt(LocalDateTime.now().withNano(0));
        dispatch();
        markModified();
    }

    /** Nimmt „vorzeitig beenden“ zurück (solange es keine weitere Phase gibt): die Spiele sind wieder offen. */
    public void undoEndPhaseEarly(PlayingClass phase) {
        if (!phase.isEndedEarly()) return;
        if (phase.getFollowUpClass() != null) throw new IllegalStateException("Es gibt schon eine weitere Phase.");
        for (Match m : matchesOf(phase)) if (m.getStatus() == MatchStatus.CANCELLED) m.setStatus(MatchStatus.PLANNED);
        phase.setEndedEarly(false);
        rootOf(phase).setFinishedAt(null);
        if (phase.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(phase);
        else if (phase.getMode() == TournamentMode.PLAN) resolvePlan(phase);
        else if (phase.isStarted()) queueReady(phase);
        dispatch();
        markModified();
    }

    /** Anzahl der Ergebnisse, die ein Neustart der Phase löscht (übernommene zählen nicht). */
    public long restartResultCount(PlayingClass phase) {
        return matchesOf(phase).stream().filter(m -> !m.isCarried() && m.getStatus() == MatchStatus.DONE).count();
    }

    /** Warum die Phase nicht neu gestartet werden kann – sonst {@code null}. */
    public String restartProblem(PlayingClass phase) {
        if (phase.getFollowUpClass() != null) return "Es gibt schon eine weitere Phase – erst die folgende Phase verwerfen.";
        if (!phase.isStarted()) return "Die Phase ist noch nicht gestartet.";
        return null;
    }

    /**
     * Startet die (letzte) Phase komplett neu: Auslosung bzw. Übernahme aus der Vorphase bleiben, alle Ergebnisse werden
     * zurückgesetzt (auch Aufgaben, kampflose Wertungen, „nicht relevant für Export“). Spiele an Tischen werden
     * abgebrochen. KO: spätere Runden wieder ohne Gegner; Qualifikation/Schweizer System: nur Runde 1 bleibt.
     * Auch nach „vorzeitig beenden“ bzw. abgeschlossener Klasse – die Klasse läuft danach wieder.
     */
    public void restartPhase(PlayingClass phase) {
        String problem = restartProblem(phase);
        if (problem != null) throw new IllegalStateException(problem);
        if (phase.getMode() == TournamentMode.QUALIFICATION || phase.getMode() == TournamentMode.SWISS) {
            matches.removeIf(m -> m.getClassName().equals(phase.getName()) && m.getRound() > 1);
            List<String> byes = new java.util.ArrayList<>(phase.getSwissByes());
            if (byes.size() > 1) phase.setSwissByes(byes.subList(0, 1));
        }
        if (phase.getMode() == TournamentMode.QUALIFICATION) {
            if (phase.getQualDecision() == 1) phase.setQualification(phase.getQualDirect(), phase.getQualKo(), QUAL_WINS);
            phase.setQualDecision(0);
            phase.setQualExtra(false);
            phase.setQualExtraDecision(0);
        }
        phase.setScheduleAnchor(null);                         // 1.0 (#77): Zeitplan beginnt neu
        phase.getReleasedRounds().clear();
        phase.getAskedRounds().clear();
        for (Match m : matchesOf(phase)) {
            if (m.isCarried()) continue;
            m.clearScore();
            m.setQueueOrder(0);
        }
        for (String id : new java.util.ArrayList<>(phase.getWithdrawn())) phase.removeWithdrawn(id);
        phase.setEndedEarly(false);
        PlayingClass root = rootOf(phase);
        root.setFinishedAt(null);
        if (phase.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(phase);
        else if (phase.getMode() == TournamentMode.PLAN) resolvePlan(phase);
        else queueReady(phase);
        dispatch();
        markModified();
    }

    public void finishClass(PlayingClass c) {
        de.hts.Log.info("Klasse beenden: " + c.getName());
        PlayingClass last = phasesOf(c).get(phasesOf(c).size() - 1);
        if (!isPhaseComplete(last)) {
            throw new IllegalStateException("Die Klasse kann erst beendet werden, wenn alle Spiele abgeschlossen sind.");
        }
        last.setNextPhase(TournamentMode.NONE);
        PlayingClass root = rootOf(c);
        if (!root.isFinished()) root.setFinishedAt(LocalDateTime.now().withNano(0));
        markModified();
    }

    /**
     * Erstellt nach abgeschlossener Gruppenphase die nächste Phase der Klasse und startet sie sofort, damit freie
     * Tische weiter belegt werden.
     * <ul>
     *   <li>KO-Phase: die besten {@code qualifiers} jeder Gruppe; Gruppenerste auf den Setzplätzen,
     *       Gruppengleiche möglichst spät gegeneinander.</li>
     *   <li>Weitere Gruppenphase: Schlangensystem nach Platzierung.</li>
     *   <li>Gruppenphase mit Platzierungsspielen (nur nach 2 Gruppen): die ersten beiden jeder Gruppe spielen eine
     *       Endgruppe um Platz 1–4 (das Spiel aus der Vorrunde wird übernommen, es bleiben 4 Spiele; gewertet nach
     *       Spielverhältnis, direktem Vergleich, Satz- und Ballverhältnis). Die Dritten spielen um Platz 5, die Vierten
     *       um Platz 7 usw. Ohne Gegner (ungleich große Gruppen) gibt es den Platz kampflos.</li>
     * </ul>
     */
    /**
     * Phase abschließen und eine neue Phase anlegen.
     *
     * @param kind       KNOCKOUT, GROUPS oder PLACEMENT
     * @param qualifiers Aufsteiger je Gruppe (Schweizer System: insgesamt); bei PLACEMENT ohne Bedeutung
     * @param groupSize  Gruppengröße der neuen Gruppenphase
     */
    /** 0.38 (#61): KO-Folgephase mit bestimmten Gruppenplätzen (z. B. 2, 3, 4) statt der Besten je Gruppe. */
    public PlayingClass createNextPhase(PlayingClass c, List<Integer> places, java.util.Random random) {
        de.hts.Log.info("Folgephase anlegen: " + c.getName());
        return withRecipe("places|" + (places == null ? "" : String.join(",", places.stream().map(String::valueOf).toList())), random,
                r -> createNextPhase0(c, places, r));
    }

    private PlayingClass createNextPhase0(PlayingClass c, List<Integer> places, java.util.Random random) {
        String problem = placesProblem(c, places);
        if (problem != null) throw new IllegalArgumentException(problem);
        c.setNextPhase(TournamentMode.KNOCKOUT);
        c.setQualifierPlaces(places);
        c.setQualifiers(places.stream().mapToInt(Integer::intValue).max().orElse(1));
        try {
            return createNextPhase(c, random);
        } catch (RuntimeException e) {
            c.setNextPhase(TournamentMode.NONE);
            c.setQualifierPlaces(List.of());
            throw e;
        }
    }

    /**
     * Liest „2,3,4“ (durch Komma getrennte Gruppenplätze); {@code null}, wenn die Eingabe nicht lesbar ist.
     */
    public static List<Integer> parsePlaces(String text) {
        if (text == null || text.isBlank()) return null;
        List<Integer> out = new java.util.ArrayList<>();
        for (String part : text.split(",")) {
            String p = part.trim();
            if (p.isEmpty() || !p.matches("\\d{1,3}")) return null;
            out.add(Integer.parseInt(p));
        }
        return out;
    }

    /** Warum diese Gruppenplätze nicht gehen – sonst {@code null}. */
    public String placesProblem(PlayingClass c, List<Integer> places) {
        if (places == null || places.isEmpty()) return "Bitte Gruppenplätze durch Komma getrennt angeben, z. B. 2,3,4.";
        int maxGroup = c.getGroups().stream().mapToInt(List::size).max().orElse(0);
        if (places.stream().distinct().count() < places.size()) return "Ein Gruppenplatz ist doppelt angegeben.";
        for (int p : places) {
            if (p < 1) return "Gruppenplätze beginnen bei 1.";
            if (p > maxGroup) return "Platz " + p + " gibt es nicht – die größte Gruppe hat " + maxGroup + " Plätze.";
        }
        int count = 0;
        for (List<String> g : c.getGroups()) for (int p : places) if (p <= g.size()) count++;
        if (count < 2) return "Es müssen mindestens 2 weiterkommen.";
        return null;
    }

    /** Gruppenplätze (1-basiert), die in die Folgephase einziehen. */
    public static List<Integer> advancingPlaces(PlayingClass c) {
        if (!c.getQualifierPlaces().isEmpty()) return c.getQualifierPlaces();
        List<Integer> out = new java.util.ArrayList<>();
        for (int p = 1; p <= c.getQualifiers(); p++) out.add(p);
        return out;
    }

    public PlayingClass createNextPhase(PlayingClass c, TournamentMode kind, int qualifiers, int groupSize,
                                        java.util.Random random) {
        de.hts.Log.info("Folgephase anlegen: " + c.getName() + " (" + kind + ")");
        return withRecipe("kind|" + (kind == null ? "NONE" : kind.name()) + "|" + qualifiers + "|" + groupSize, random,
                r -> createNextPhase0(c, kind, qualifiers, groupSize, r));
    }

    private PlayingClass createNextPhase0(PlayingClass c, TournamentMode kind, int qualifiers, int groupSize,
                                          java.util.Random random) {
        if (kind == TournamentMode.QUALIFICATION) {
            throw new IllegalArgumentException("Für den Qualifikationsmodus createQualificationPhase verwenden.");
        }
        if (kind == null || kind == TournamentMode.NONE || kind == TournamentMode.SWISS) {
            throw new IllegalArgumentException("Bitte KO-System, Gruppenphase oder Gruppenphase mit Platzierungsspielen wählen.");
        }
        c.setNextPhase(kind);
        c.setQualifiers(qualifiers);
        c.setQualifierPlaces(List.of());
        c.setGroupSize(Math.max(2, groupSize));
        try {
            return createNextPhase(c, random);
        } catch (RuntimeException e) {
            c.setNextPhase(TournamentMode.NONE);
            throw e;
        }
    }

    /**
     * Folgephase neu auslosen – nur wenn sie angehalten ist und an keinem Tisch mehr ein Spiel von ihr läuft.
     * Bisherige Ergebnisse dieser Phase gehen verloren; die neue Auslosung bleibt angehalten (zum Prüfen/Verschieben).
     */
    public PlayingClass redrawPhase(PlayingClass phase, java.util.Random random) {
        if (!phase.isPhase()) throw new IllegalStateException("Nur Folgephasen (Phase 2/3) können hier neu ausgelost werden.");
        if (phase.getMode() == TournamentMode.PLAN || phase.isTiered()) {
            throw new IllegalStateException("Diese Phase lässt sich nicht neu auslosen – stattdessen „Zurück zu Phase "
                    + (phaseNumber(phase) - 1) + "“ und die Phase neu anlegen.");
        }
        if (!phase.isPaused()) throw new IllegalStateException("Bitte die Phase zuerst anhalten.");
        if (phase.getFollowUpClass() != null) throw new IllegalStateException("Es gibt schon eine weitere Phase.");
        if (matchesOf(phase).stream().anyMatch(m -> m.getStatus() == MatchStatus.ACTIVE)) {   // pausierte werden mit gelöscht
            throw new IllegalStateException("An einem Tisch läuft noch ein Spiel dieser Phase – erst eintragen, dann neu auslosen.");
        }
        PlayingClass source = findClass(phase.getSourceClass()).orElseThrow();
        if (!isPhaseComplete(source)) throw new IllegalStateException("Die vorherige Phase ist nicht abgeschlossen.");
        TournamentMode kind = phase.getMode();
        int groupSize = phase.getGroupSize();
        removeClass(phase);
        source.setFollowUpClass(null);
        source.setNextPhase(kind);
        source.setGroupSize(groupSize);
        long seed = random.nextLong();
        PlayingClass next = createNextPhase(source, new java.util.Random(seed), true);
        next.setRecipe("next");
        next.setRecipeSeed(seed);
        markModified();
        return next;
    }

    public PlayingClass createNextPhase(PlayingClass c, java.util.Random random) {
        de.hts.Log.info("Folgephase anlegen: " + c.getName());
        return withRecipe("next", random, r -> createNextPhase(c, r, false));
    }

    private PlayingClass createNextPhase(PlayingClass c, java.util.Random random, boolean startPaused) {
        if (!canCreateNextPhase(c)) {
            if (c.getMode() == TournamentMode.SWISS) throw new IllegalStateException("Nach dem Schweizer System gibt es keine weitere Phase.");
            if (!canHaveNextPhase(c)) throw new IllegalStateException("Eine Klasse hat höchstens " + MAX_PHASES + " Phasen.");
            throw new IllegalStateException("Die Folgephase kann erst erstellt werden, wenn alle Gruppenspiele abgeschlossen sind.");
        }
        TournamentMode kind = c.getNextPhase();
        PlayingClass root = rootOf(c);
        int number = phaseNumber(c) + 1;
        String base = root.getName() + " – Phase " + number;
        String name = base;
        for (int k = 2; findClass(name).isPresent(); k++) name = base + " (" + k + ")";
        PlayingClass next = new PlayingClass(name, c.getDiscipline(), c.getType(), null, null);
        next.setWomanRequired(c.isWomanRequired());
        next.setSourceClass(c.getName());      // vor addClass: erscheint so nicht in der Klassenliste
        addClass(next);
        next.setLotSeed(random.nextLong() | 1L);
        next.setTiebreak(c.getTiebreak());
        next.setGroupsBySize(true);
        next.setGroupSize(c.getGroupSize());

        int groupCount = c.getGroups().size();
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        for (int g = 0; g < groupCount; g++) tables.add(standings(c, g));
        java.util.Map<String, String> idMap = new java.util.HashMap<>();

        if (!c.getExempt().isEmpty()) {
            if (kind != TournamentMode.KNOCKOUT) {
                removeClass(next);
                throw new IllegalStateException("Nach der Gruppenphase mit Freistellung folgt immer das KO-System.");
            }
            c.setQualifiers(c.getExemptPlaces());
            c.setQualifierPlaces(List.of());
            buildExemptKnockout(c, next, tables, idMap, random);
        } else if (kind == TournamentMode.QUALIFICATION) {
            int q = c.getQualDirect(), k = c.getQualKo();
            List<Entrant> direct = new java.util.ArrayList<>();
            java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
            java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
            for (int place = 0; place < q; place++) {
                for (Entrant e : tier(tables, place, next, idMap, groupOf, random)) {
                    direct.add(e);
                    labels.put(e.id(), GroupDraw.groupName(groupOf.get(e.id())) + (place + 1));
                }
            }
            List<Entrant> ko = new java.util.ArrayList<>();
            java.util.Map<String, Integer> placeOf = new java.util.LinkedHashMap<>();
            int[] koPerGroup = new int[groupCount];
            int total = c.getQualKoTotal();
            int maxSize = tables.stream().mapToInt(List::size).max().orElse(0);
            int remaining = total;
            for (int place = q; total > 0 ? place < maxSize && remaining > 0 : place < q + k; place++) {
                List<Integer> order = tierOrder(tables, place, random, true);
                if (total > 0 && order.size() > remaining) {
                    // nur die besten dieser Gruppenplätze: Spiel-, Satz-, Punktverhältnis, Gleichstand Los
                    java.util.Set<Integer> chosen = new java.util.HashSet<>(tierOrder(tables, place, random, false).subList(0, remaining));
                    order.removeIf(g -> !chosen.contains(g));
                }
                remaining -= order.size();
                for (int g : order) koPerGroup[g]++;
                for (Entrant e : carry(tables, place, order, next, idMap, groupOf)) {
                    ko.add(e);
                    placeOf.put(e.id(), place + 1);
                    labels.put(e.id(), GroupDraw.groupName(groupOf.get(e.id())) + (place + 1));
                }
            }
            next.setMode(TournamentMode.QUALIFICATION);
            next.setQualification(q, k, c.getQualRounds());
            next.setQualKoTotal(total);
            next.setQualKoCounts(java.util.Arrays.stream(koPerGroup).boxed().toList());
            next.setQualReserve(c.getQualReserve());
            next.setQualifiedDirect(direct.stream().map(Entrant::id).toList());
            java.util.LinkedHashMap<String, Integer> field = new java.util.LinkedHashMap<>();
            for (Entrant e : ko) field.put(e.id(), groupOf.get(e.id()));
            next.setQualField(field);
            next.setQualPlace(placeOf);
            next.setQualExtra(false);
            next.setSeedLabels(labels);
            if (ko.size() >= 2 && c.getQualRounds() > 0) addQualificationRound(next, 1, new java.util.ArrayList<>(field.keySet()));
        } else if (kind == TournamentMode.PLACEMENT) {
            createPlacementPhase(c, next, tables, idMap);
        } else {
            // Platzierungsliste: alle Ersten (A, B, C …), dann alle Zweiten …
            List<Entrant> ranked = new java.util.ArrayList<>();
            java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
            java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
            List<Integer> advancing = kind == TournamentMode.KNOCKOUT ? advancingPlaces(c)
                    : java.util.stream.IntStream.rangeClosed(1, c.getQualifiers()).boxed().toList();
            for (int placeNo : advancing) {
                int place = placeNo - 1;
                // alle Gruppen-x-ten, die besten zuerst: Spielverhältnis, Satzverhältnis, Punktverhältnis; bei
                // Gleichheit entscheidet das Los (gemischt, dann stabil sortiert). TTR zählt nur in Phase 1.
                List<int[]> tier = new java.util.ArrayList<>();
                for (int g = 0; g < groupCount; g++) if (place < tables.get(g).size()) tier.add(new int[]{g});
                java.util.Collections.shuffle(tier, random);
                final int pl = place;
                tier.sort(java.util.Comparator.comparingDouble((int[] x) -> -gameRatio(tables.get(x[0]).get(pl)))
                        .thenComparingDouble(x -> -ratio(tables.get(x[0]).get(pl).setsWon(), tables.get(x[0]).get(pl).setsLost()))
                        .thenComparingDouble(x -> -ratio(tables.get(x[0]).get(pl).pointsWon(), tables.get(x[0]).get(pl).pointsLost())));
                for (int[] x : tier) {
                    int g = x[0];
                    Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
                    if (now != null) {
                        ranked.add(now);
                        groupOf.put(now.id(), g);
                        labels.put(now.id(), GroupDraw.groupName(g) + (place + 1));
                    }
                }
            }
            if (ranked.size() < 2) {
                removeClass(next);
                throw new IllegalStateException("Für die Folgephase qualifizieren sich weniger als 2.");
            }
            if (kind == TournamentMode.KNOCKOUT) {
                List<Entrant> slots = KnockoutDraw.drawFromRanking(ranked,
                        c.getMode() == TournamentMode.SWISS ? java.util.Map.of() : groupOf, groupCount, random);
                next.setMode(TournamentMode.KNOCKOUT);
                next.setSeedLabels(labels);
                next.setDraw(slots.stream().map(e -> e == null ? null : e.id()).toList(), true);
                createKnockoutMatches(next);
            } else {
                int groups = GroupDraw.groupCountForSize(ranked.size(), next.getGroupSize());
                List<List<String>> g = new java.util.ArrayList<>();
                for (int k = 0; k < groups; k++) g.add(new java.util.ArrayList<>());
                for (int k = 0; k < ranked.size(); k++) {
                    int pass = k / groups, pos = k % groups;
                    g.get(pass % 2 == 0 ? pos : groups - 1 - pos).add(ranked.get(k).id());
                }
                next.setMode(TournamentMode.GROUPS);
                next.setGroups(g, true);
                createGroupMatches(next);
            }
        }
        c.setFollowUpClass(next.getName());
        if (c.isStarted()) {
            LocalDateTime now = LocalDateTime.now().withNano(0);
            next.setStartedAt(now);
            next.setReady(true);
            if (next.getMode() == TournamentMode.QUALIFICATION && !startPaused) {
                // 0.38 (#50): ausgelost, aber noch nicht gestartet – bis „Phase starten“ per Drag & Drop tauschbar
                startPaused = true;
                next.setAwaitingStart(true);
            }
            if (startPaused) next.setPaused(true);
            if (next.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(next);
            else queueReady(next);
            if (!startPaused) dispatch();
        }
        markModified();
        return next;
    }

    /** Spielverhältnis als Anteil gewonnener Spiele (vergleichbar auch bei unterschiedlich großen Gruppen). */
    private static double gameRatio(GroupStanding.Row r) {
        return ratio(r.score(), r.lost());
    }

    /** Anteil gewonnen ÷ gesamt; ohne Spiele 0. */
    private static double ratio(int won, int lost) {
        return won + lost == 0 ? 0 : (double) won / (won + lost);
    }

    /** Alle Gruppen-x-ten (place, 0-basiert), die besten zuerst (Spiel-, Satz-, Punktverhältnis, dann TTR; Gleichstand: Los). */
    private List<Entrant> tier(List<List<GroupStanding.Row>> tables, int place, PlayingClass next, java.util.Map<String, String> idMap,
                               java.util.Map<String, Integer> groupOf, java.util.Random random) {
        return carry(tables, place, tierOrder(tables, place, random, true), next, idMap, groupOf);
    }

    /**
     * Gruppen-Indizes der Gruppen-x-ten (place, 0-basiert), die besten zuerst: Spiel-, Satz-, Punktverhältnis, dann –
     * mit {@code ttr} – TTR; bei Gleichstand entscheidet das Los.
     */
    private static List<Integer> tierOrder(List<List<GroupStanding.Row>> tables, int place, java.util.Random random, boolean ttr) {
        List<Integer> t = new java.util.ArrayList<>();
        for (int g = 0; g < tables.size(); g++) if (place < tables.get(g).size()) t.add(g);
        java.util.Collections.shuffle(t, random);
        java.util.Comparator<Integer> cmp = java.util.Comparator.comparingDouble((Integer x) -> -gameRatio(tables.get(x).get(place)))
                .thenComparingDouble(x -> -ratio(tables.get(x).get(place).setsWon(), tables.get(x).get(place).setsLost()))
                .thenComparingDouble(x -> -ratio(tables.get(x).get(place).pointsWon(), tables.get(x).get(place).pointsLost()));
        if (ttr) {
            cmp = cmp.thenComparingInt(x -> {
                Integer v = tables.get(x).get(place).entrant().ttr();
                return v == null ? Integer.MAX_VALUE : -v;
            });
        }
        t.sort(cmp);
        return t;
    }

    private List<Entrant> carry(List<List<GroupStanding.Row>> tables, int place, List<Integer> order, PlayingClass next,
                                java.util.Map<String, String> idMap, java.util.Map<String, Integer> groupOf) {
        List<Entrant> out = new java.util.ArrayList<>();
        for (int g : order) {
            Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
            if (now != null) {
                out.add(now);
                groupOf.put(now.id(), g);
            }
        }
        return out;
    }

    // =====================================================================================================
    // 1.3 (#107): Korrektur-Reiter – Folgephase nach einer Ergebniskorrektur neu berechnen
    // =====================================================================================================

    private int recipeDepth;

    /** Legt eine Folgephase mit eigenem, gespeichertem Los an, damit sie sich später identisch neu berechnen lässt. */
    private PlayingClass withRecipe(String recipe, java.util.Random random, java.util.function.Function<java.util.Random, PlayingClass> create) {
        if (recipeDepth > 0) return create.apply(random);
        long seed = random.nextLong();
        PlayingClass next;
        recipeDepth++;
        try {
            next = create.apply(new java.util.Random(seed));
        } finally {
            recipeDepth--;
        }
        next.setRecipe(recipe);
        next.setRecipeSeed(seed);
        return next;
    }

    private static List<Integer> intsOf(String s) {
        List<Integer> l = parsePlaces(s);
        return l == null ? List.of() : l;
    }

    /** Folgephase nach Rezept neu anlegen (die Vorphase darf dabei keine Folgephase haben und nicht gestartet sein). */
    private PlayingClass recreate(PlayingClass source, String recipe, long seed) {
        java.util.Random r = new java.util.Random(seed);
        String[] p = recipe.split("\\|", -1);
        recipeDepth++;
        try {
            return switch (p[0]) {
                case "next" -> createNextPhase(source, r);
                case "places" -> createNextPhase(source, intsOf(p[1]), r);
                case "kind" -> createNextPhase(source, TournamentMode.valueOf(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), r);
                case "qual" -> createQualificationPhase(source, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Boolean.parseBoolean(p[3]),
                        Integer.parseInt(p[4]), r);
                case "plan" -> createPlanPhase(source, Plan.Kind.valueOf(p[1]), "-".equals(p[2]) ? null : new PlanOptions(intsOf(p[2]),
                        intsOf(p[3]), Boolean.parseBoolean(p[4]), Boolean.parseBoolean(p[5]), Integer.parseInt(p[6])), r);
                case "tier" -> createTieredPhase(source, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Boolean.parseBoolean(p[3]), r);
                default -> null;
            };
        } finally {
            recipeDepth--;
        }
    }

    /** Die laufende (reguläre) Folgephase der Vorphase, sonst {@code null}. */
    private PlayingClass followUpOf(PlayingClass source) {
        return source == null || source.getFollowUpClass() == null ? null : findClass(source.getFollowUpClass()).orElse(null);
    }

    /** Korrektur-Reiter der Phase {@code phase} (= „Phase n – Korrektur“ zu dieser laufenden Phase), sonst {@code null}. */
    public PlayingClass correctionOf(PlayingClass phase) {
        if (phase == null) return null;
        for (PlayingClass x : classes) if (phase.getName().equals(x.getCorrectionOf())) return x;
        return null;
    }

    /** Die laufende Phase, die eine Korrektur- bzw. deaktivierte Phase ersetzt bzw. ersetzen würde. */
    public PlayingClass correctedPhase(PlayingClass side) {
        if (side == null || side.getCorrectionOf() == null) return null;
        return findClass(side.getCorrectionOf()).orElse(null);
    }

    /** Deaktivierte Phasen, die an der Vorphase {@code source} hängen (in der Reihenfolge ihres Entstehens). */
    public List<PlayingClass> deactivatedAfter(PlayingClass source) {
        List<PlayingClass> out = new java.util.ArrayList<>();
        if (source == null) return out;
        for (PlayingClass x : classes) if (x.isDeactivated() && source.getName().equals(x.getSourceClass())) out.add(x);
        return out;
    }

    /** Spiel einer deaktivierten Phase bzw. eines Korrektur-Reiters (zählt nirgends, wird nicht exportiert)? */
    public boolean isInactiveMatch(Match m) {
        PlayingClass c = m == null ? null : findClass(m.getClassName()).orElse(null);
        return c != null && c.isSideClass();
    }

    /**
     * Nach einer Ergebniskorrektur in Phase n: Phase n+1 nach ihrem Rezept neu berechnen – nur, wenn es Phase n+1 gibt und
     * danach keine weitere Phase. Weicht das Ergebnis ab (andere Paarungen bzw. Teilnehmende), entsteht bzw. erneuert sich
     * der Reiter „Phase n+1 – Korrektur“; sonst verschwindet ein bisheriger Korrektur-Reiter.
     *
     * @return der Korrektur-Reiter oder {@code null}
     */
    public PlayingClass updateCorrection(PlayingClass source) {
        PlayingClass follow = followUpOf(source);
        if (follow == null || follow.getFollowUpClass() != null || follow.getRecipe() == null || follow.isSideClass()) return null;
        PlayingClass old = correctionOf(follow);
        if (old != null) removeClass(old);
        PlayingClass root = rootOf(source);
        LocalDateTime started = source.getStartedAt(), finished = root.getFinishedAt();
        PlayingClass corr = null;
        source.setFollowUpClass(null);
        source.setStartedAt(null);
        root.setFinishedAt(null);
        try {
            corr = recreate(source, follow.getRecipe(), follow.getRecipeSeed());
        } catch (RuntimeException e) {
            corr = null;            // z. B. kämen jetzt weniger als 2 weiter – dann bleibt es bei der laufenden Phase
        } finally {
            if (source.getFollowUpClass() != null && corr == null) {
                findClass(source.getFollowUpClass()).filter(x -> x != follow).ifPresent(this::removeClass);
            }
            source.setFollowUpClass(follow.getName());
            source.setStartedAt(started);
            root.setFinishedAt(finished);
        }
        if (corr == null) {
            markModified();
            return null;
        }
        corr.setRecipe(follow.getRecipe());
        corr.setRecipeSeed(follow.getRecipeSeed());
        corr.setCorrectionOf(follow.getName());
        corr.setStartedAt(null);
        corr.setReady(false);
        corr.setPaused(false);
        corr.setAwaitingStart(false);
        if (structureOf(corr).equals(structureOf(follow)) && newPairings(corr).isEmpty()) {
            removeClass(corr);
            markModified();
            return null;
        }
        markModified();
        return corr;
    }

    /** Aufbau einer Phase (Gruppen, KO-Feld, Qualifikation, Setzkürzel) unabhängig von Doppel-Kopien. */
    private String structureOf(PlayingClass p) {
        StringBuilder sb = new StringBuilder(p.getMode().name()).append('|');
        for (List<String> g : p.getGroups()) sb.append(g.stream().map(this::personKey).toList()).append(';');
        sb.append('|').append(p.getDraw().stream().map(this::personKey).toList());
        sb.append('|').append(p.getQualifiedDirect().stream().map(this::personKey).toList());
        sb.append('|').append(p.getQualField().keySet().stream().map(this::personKey).toList());
        java.util.TreeMap<String, String> labels = new java.util.TreeMap<>();
        p.getSeedLabels().forEach((k, v) -> labels.put(String.valueOf(personKey(k)), v));
        sb.append('|').append(labels);
        if (p.getPlan() != null) sb.append('|').append(p.getPlan().getSeedOrigins());
        return sb.toString();
    }

    private String pairKey(Match m) {
        String a = personKey(m.getA()), b = personKey(m.getB());
        return a.compareTo(b) <= 0 ? a + "~" + b : b + "~" + a;
    }

    /** Personen (bzw. Doppel als Personenpaar), die in der Phase vorkommen. */
    private java.util.Set<String> phasePersons(PlayingClass p) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (List<String> g : p.getGroups()) for (String id : g) out.add(personKey(id));
        for (String id : p.getDraw()) if (id != null) out.add(personKey(id));
        for (String id : p.getQualifiedDirect()) out.add(personKey(id));
        for (String id : p.getQualField().keySet()) out.add(personKey(id));
        for (Match m : matchesOf(p)) {
            if (m.getA() != null) out.add(personKey(m.getA()));
            if (m.getB() != null) out.add(personKey(m.getB()));
        }
        out.remove(null);
        return out;
    }

    /** Korrektur-Reiter: Teilnehmende (Entrant-IDs), die in der laufenden Phase nicht vorkommen. */
    public java.util.Set<String> newEntrants(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        java.util.Set<String> out = new java.util.HashSet<>();
        if (old == null) return out;
        java.util.Set<String> before = phasePersons(old);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (List<String> g : corr.getGroups()) ids.addAll(g);
        for (String id : corr.getDraw()) if (id != null) ids.add(id);
        ids.addAll(corr.getQualifiedDirect());
        ids.addAll(corr.getQualField().keySet());
        for (Match m : matchesOf(corr)) {
            if (m.getA() != null) ids.add(m.getA());
            if (m.getB() != null) ids.add(m.getB());
        }
        for (String id : ids) if (!before.contains(personKey(id))) out.add(id);
        return out;
    }

    /** Korrektur-Reiter: Spiele, deren Paarung es in der laufenden Phase nicht gibt. */
    public java.util.Set<Match> newPairings(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        java.util.Set<Match> out = new java.util.HashSet<>();
        if (old == null) return out;
        java.util.Set<String> before = new java.util.HashSet<>();
        for (Match m : matchesOf(old)) if (m.getA() != null && m.getB() != null) before.add(pairKey(m));
        for (Match m : matchesOf(corr)) if (m.getA() != null && m.getB() != null && !before.contains(pairKey(m))) out.add(m);
        return out;
    }

    /** Spiel der laufenden Phase mit demselben Ergebnis-Schlüssel (Paarung und Spielart), das schon gespielt ist. */
    private Match unchangedPlayed(List<Match> oldMatches, Match m) {
        String key = pairKey(m);
        for (Match o : oldMatches) {
            if (o.getStatus() != MatchStatus.DONE || o.getA() == null || o.getB() == null || o.getRetirementOf() != null) continue;
            if ((o.getGroupIndex() >= 0) != (m.getGroupIndex() >= 0) || o.getKoRound() != m.getKoRound()) continue;
            if (key.equals(pairKey(o))) return o;
        }
        return null;
    }

    /** Wie viele Ergebnisse „Unveränderte Ergebnisse übernehmen“ jetzt übernehmen würde. */
    public int takeOverCount(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        if (old == null) return 0;
        List<Match> om = matchesOf(old);
        int n = 0;
        for (Match m : matchesOf(corr)) {
            if (m.getStatus() != MatchStatus.DONE && !m.isCarried() && m.getA() != null && m.getB() != null && unchangedPlayed(om, m) != null) n++;
        }
        return n;
    }

    /**
     * „Unveränderte Ergebnisse übernehmen“: Spiele mit derselben Paarung wie ein schon gespieltes Spiel der laufenden Phase
     * bekommen dessen Ergebnis; im KO rücken die Sieger nach, und auch daraus entstehende gleiche Paarungen werden übernommen.
     *
     * @return Anzahl übernommener Ergebnisse
     */
    public int takeOverUnchanged(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        if (old == null) throw new IllegalStateException("Kein Korrektur-Reiter.");
        List<Match> om = matchesOf(old);
        int taken = 0;
        for (boolean changed = true; changed; ) {
            changed = false;
            for (Match m : matchesOf(corr)) {
                if (m.getStatus() == MatchStatus.DONE || m.isCarried() || m.getA() == null || m.getB() == null) continue;
                Match o = unchangedPlayed(om, m);
                if (o == null) continue;
                boolean flip = !String.valueOf(personKey(o.getA())).equals(personKey(m.getA()));
                List<int[]> sets = new java.util.ArrayList<>();
                for (int[] x : o.getPoints()) sets.add(flip ? new int[]{x[1], x[0]} : new int[]{x[0], x[1]});
                int retired = o.getRetired() == 0 ? 0 : flip ? 3 - o.getRetired() : o.getRetired();
                int sa = o.getSetsA() == null ? 0 : o.getSetsA(), sb = o.getSetsB() == null ? 0 : o.getSetsB();
                m.setScore(sets, retired, flip ? sb : sa, flip ? sa : sb);
                m.setLineups(flip ? o.getLineupB() : o.getLineupA(), flip ? o.getLineupA() : o.getLineupB());
                m.setStatus(MatchStatus.DONE);
                m.setTable(o.getTable());
                m.setStartedAt(o.getStartedAt());
                m.setFinishedAt(o.getFinishedAt());
                taken++;
                changed = true;
            }
            if (changed && corr.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(corr);
            if (changed && corr.getMode() == TournamentMode.PLAN) resolvePlan(corr);
        }
        markModified();
        return taken;
    }

    /** Folgen von „Phase n weiterführen“ (für den Warnhinweis). */
    public record AdoptInfo(int recalled, int playedDropped, int taken) {
    }

    public AdoptInfo adoptInfo(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        if (old == null) return new AdoptInfo(0, 0, 0);
        int recalled = 0, dropped = 0;
        java.util.Set<String> keptPairs = new java.util.HashSet<>();
        for (Match m : matchesOf(corr)) if (m.getStatus() == MatchStatus.DONE && m.getA() != null && m.getB() != null) keptPairs.add(pairKey(m));
        for (Match m : matchesOf(old)) {
            if (m.getStatus().atTable()) recalled++;
            else if (m.getStatus() == MatchStatus.DONE && m.isExported() && m.getA() != null && m.getB() != null
                    && !keptPairs.contains(pairKey(m))) dropped++;
        }
        int taken = (int) matchesOf(corr).stream().filter(m -> m.getStatus() == MatchStatus.DONE && !m.isCarried()
                && m.getA() != null && m.getB() != null).count();
        return new AdoptInfo(recalled, dropped, taken);
    }

    /**
     * „Phase n weiterführen“: die bisherige Phase n wird deaktiviert (laufende Spiele zurückgerufen, offene nicht mehr
     * gespielt; sie zählt nirgends und kommt nicht in die click-TT-Ergebnisdatei), die korrigierte Phase wird zur laufenden.
     */
    public PlayingClass adoptCorrection(PlayingClass corr) {
        PlayingClass old = correctedPhase(corr);
        PlayingClass source = corr.getSourceClass() == null ? null : findClass(corr.getSourceClass()).orElse(null);
        if (old == null || source == null || !old.getName().equals(source.getFollowUpClass())) {
            throw new IllegalStateException("Der Korrektur-Reiter passt nicht mehr zur laufenden Phase.");
        }
        if (old.getFollowUpClass() != null) throw new IllegalStateException("Nach Phase " + phaseNumber(old) + " gibt es schon eine weitere Phase.");
        for (Match m : matchesOf(old)) {
            if (m.getStatus() == MatchStatus.DONE || m.getStatus() == MatchStatus.CANCELLED) continue;
            m.setStatus(MatchStatus.CANCELLED);
            m.setTable(null);
        }
        old.setDeactivated(true);
        old.setPaused(false);
        old.setAwaitingStart(false);
        corr.setCorrectionOf(null);
        source.setFollowUpClass(corr.getName());
        PlayingClass root = rootOf(source);
        if (root.isFinished()) root.setFinishedAt(null);
        if (source.isStarted()) {
            corr.setStartedAt(LocalDateTime.now().withNano(0));
            corr.setReady(true);
            if (corr.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(corr);
            else if (corr.getMode() == TournamentMode.PLAN) resolvePlan(corr);
            queueReady(corr);
            dispatch();
        }
        markModified();
        return corr;
    }

    /** „Korrektur verwerfen“: nur der Korrektur-Reiter verschwindet; die Korrektur in der Vorphase bleibt. */
    public void discardCorrection(PlayingClass corr) {
        if (corr == null || corr.getCorrectionOf() == null) throw new IllegalStateException("Kein Korrektur-Reiter.");
        removeClass(corr);
        markModified();
    }

    /** Qualifiziert ist in der KO-Qualifikation, wer 2 Spiele gewinnt (ein Freilos in Runde 1 zählt als Sieg). */
    public static final int QUAL_WINS = 2;

    /**
     * Qualifikationsmodus (nur Phase 2 nach Gruppen in Phase 1, z. B. HEM-Hauptqualifikation): die besten {@code direct}
     * jeder Gruppe sind direkt qualifiziert; die nächsten {@code ko} jeder Gruppe spielen eine KO-Qualifikation. Wer
     * 2 Spiele gewinnt, ist qualifiziert (Runde 1: höher gegen niedriger Platzierte, Freilos für den Stärksten; Runde 2
     * frei gelost, ohne Freilos). Optional spielen die Verlierer der Runde 2 eine Zusatzrunde – deren Gewinner sind
     * ebenfalls qualifiziert ({@link #setQualExtra}). Nachrücker: Verlierer der Runde 2 bzw. der Zusatzrunde, dann
     * Verlierer der Runde 1, dann (optional) Nachrücker aus der Gruppenphase.
     *
     * @param rounds ohne Bedeutung (immer {@link #QUAL_WINS}); nur noch aus Kompatibilität vorhanden
     */
    public PlayingClass createQualificationPhase(PlayingClass c, int direct, int ko, int rounds, java.util.Random random) {
        return createQualificationPhase(c, direct, ko, rounds, 0, random);
    }

    /** Anzahl der Teilnehmenden der KO-Qualifikation bei {@code direct} direkt Qualifizierten und {@code ko} je Gruppe. */
    public static int qualificationKoCount(PlayingClass c, int direct, int ko) {
        int koCount = 0;
        for (List<String> g : c.getGroups()) koCount += Math.max(0, Math.min(g.size(), direct + ko) - Math.min(g.size(), direct));
        return koCount;
    }

    /**
     * Geht die KO-Qualifikation mit {@code n} Teilnehmenden auf? Runde 2 hat kein Freilos: die Gewinner der Runde 1
     * (inkl. Freilos, also n/2 aufgerundet) müssen eine gerade Zahl sein – n = 0 (keine KO-Runde), 3, 4, 7, 8, 11, 12 …
     */
    public static boolean qualificationFieldFits(int n) {
        return n == 0 || (n >= 3 && (n % 4 == 0 || n % 4 == 3));
    }

    /** Hinweis für eine KO-Qualifikation, die nicht aufgeht (sonst {@code null}). */
    public static String qualificationFieldProblem(int n) {
        if (qualificationFieldFits(n)) return null;
        int lower = n, upper = n;
        while (lower > 0 && !qualificationFieldFits(lower)) lower--;
        while (!qualificationFieldFits(upper)) upper++;
        return "Mit " + n + " Teilnehmenden geht die KO-Qualifikation nicht auf: In Runde 2 gibt es kein Freilos, die Gewinner "
                + "der Runde 1 müssen also eine gerade Zahl sein. Möglich sind z. B. " + (lower > 0 ? lower + " oder " : "") + upper
                + " Teilnehmende (3, 4, 7, 8, 11, 12 …) – bitte die Plätze für die KO-Qualifikation anpassen.";
    }

    /** @param reserve Nachrücker je Gruppe direkt aus der Gruppenphase (ohne Spiel), nach Gruppenplatz, dann TTR */
    public PlayingClass createQualificationPhase(PlayingClass c, int direct, int ko, int rounds, int reserve, java.util.Random random) {
        return createQualificationPhase(c, direct, ko, false, reserve, random);
    }

    /** Teilnehmende, die nach den direkt Qualifizierten noch für die KO-Qualifikation zur Verfügung stehen. */
    public static int qualificationAvailable(PlayingClass c, int direct) {
        int n = 0;
        for (List<String> g : c.getGroups()) n += Math.max(0, g.size() - direct);
        return n;
    }

    /**
     * @param ko      Plätze je Gruppe ({@code koTotal = false}) bzw. Anzahl der Topplatzierten insgesamt ({@code koTotal =
     *                true}, z. B. 24 bei 16 Gruppen = alle Zweiten + die 8 besten Dritten; Auswahl nach Spiel-, Satz-,
     *                Punktverhältnis, Gleichstand: Los)
     * @param reserve Nachrücker je Gruppe ohne Spiel, ab dem ersten Nicht-KO-Teilnehmenden der Gruppe
     */
    public PlayingClass createQualificationPhase(PlayingClass c, int direct, int ko, boolean koTotal, int reserve, java.util.Random random) {
        return withRecipe("qual|" + direct + "|" + ko + "|" + koTotal + "|" + reserve, random,
                r -> createQualificationPhase0(c, direct, ko, koTotal, reserve, r));
    }

    private PlayingClass createQualificationPhase0(PlayingClass c, int direct, int ko, boolean koTotal, int reserve, java.util.Random random) {
        if (c.getMode() != TournamentMode.GROUPS || phaseNumber(c) != 1) {
            throw new IllegalStateException("Der Qualifikationsmodus folgt nur auf eine Gruppenphase in Phase 1.");
        }
        int available = qualificationAvailable(c, direct);
        if (koTotal && ko > available) {
            throw new IllegalArgumentException("Für die KO-Qualifikation stehen nur " + available + " Teilnehmende zur Verfügung.");
        }
        int koCount = koTotal ? ko : qualificationKoCount(c, direct, ko);
        if (direct < 1 && koCount < 3) throw new IllegalArgumentException("Ohne direkt Qualifizierte braucht die KO-Qualifikation mindestens 3 Teilnehmende.");
        String problem = qualificationFieldProblem(koCount);
        if (problem != null) throw new IllegalArgumentException(problem);
        c.setQualification(direct, koTotal ? 0 : ko, koCount > 0 ? QUAL_WINS : 0);
        c.setQualKoTotal(koTotal ? ko : 0);
        c.setQualReserve(reserve);
        int groups = Math.max(1, c.getGroups().size());
        c.setQualifiers(direct + (koTotal ? (ko + groups - 1) / groups : ko));
        c.setNextPhase(TournamentMode.QUALIFICATION);
        try {
            return createNextPhase(c, random);
        } catch (RuntimeException e) {
            c.setNextPhase(TournamentMode.NONE);
            throw e;
        }
    }

    // --- Qualifikationsmodus (HEM): KO aufwärts, qualifiziert nach n Siegen, Zusatzrunde ------------

    public static final String QUAL_EXTRA_STAGE = "Qualifikation Zusatzspiel";

    /** Zufall für die Auslosung der KO-Qualifikation (Tests können ihn festlegen). */
    private java.util.Random qualRandom = new java.util.Random();

    public void setQualificationRandom(java.util.Random r) {
        qualRandom = r == null ? new java.util.Random() : r;
    }

    /**
     * Höchstzahl „qualifiziert nach n Siegen“: In Runde 1 spielen alle, in jeder weiteren Runde die Sieger (bei
     * ungerader Zahl hat der Bestplatzierte ein Freilos). Runde n gibt es, solange vor ihr noch mindestens 2 übrig sind.
     */
    public static int maxQualificationRounds(int players) {
        int r = 0, n = players;
        while (n >= 2) {
            r++;
            n = (n + 1) / 2;
        }
        return r;
    }

    /** Aktuelle (höchste angelegte) Runde der KO-Qualifikation, Zusatzrunde = n+1 (0 = keine). */
    public int qualificationRound(PlayingClass c) {
        return matchesOf(c).stream().mapToInt(Match::getRound).max().orElse(0);
    }

    /**
     * Legt eine Runde an: bei ungerader Zahl Freilos für den Bestplatzierten, sonst spielt jeweils der
     * Höchstplatzierte gegen den Niedrigstplatzierten, der nicht aus seiner Gruppe (und möglichst nicht aus seinem
     * Verein) kommt.
     */
    private void addQualificationRound(PlayingClass c, int round, List<String> ranked) {
        String stage = round > c.getQualRounds() ? QUAL_EXTRA_STAGE : "Qualifikation Runde " + round;   // 1.3: „Qualifikation Zusatzspiel“
        List<String> rest = new java.util.ArrayList<>(ranked);
        java.util.List<String> byes = new java.util.ArrayList<>(c.getSwissByes());
        while (byes.size() < round - 1) byes.add(null);
        String bye = rest.size() % 2 == 1 ? rest.remove(0) : null;           // Freilos: Bestplatzierter
        if (byes.size() >= round) byes.set(round - 1, bye);
        else byes.add(bye);
        c.setSwissByes(byes);
        int no = matchesOf(c).stream().mapToInt(Match::getNumber).max().orElse(0);
        List<String[]> pairs = round == 1 ? drawFirstQualRound(c, rest) : drawFreeRound(rest);
        List<Match> created = new java.util.ArrayList<>();
        for (String[] p : pairs) created.add(new Match(null, c.getName(), stage, round, ++no, -1, -1, -1, p[0], p[1]));
        matches.addAll(created);
        if (c.isStarted()) queueReady(c);
    }

    /** Ab Runde 2 (und Zusatzrunde): frei zugelost. */
    private List<String[]> drawFreeRound(List<String> players) {
        List<String> l = new java.util.ArrayList<>(players);
        java.util.Collections.shuffle(l, qualRandom);
        List<String[]> out = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < l.size(); i += 2) out.add(new String[]{l.get(i), l.get(i + 1)});
        return out;
    }

    /**
     * Runde 1: per Los. Es spielt jeweils ein besser Platzierter (obere Hälfte des Feldes, z. B. Gruppenzweiter) gegen
     * einen schlechter Platzierten (untere Hälfte, z. B. Gruppenvierter). Bleiben nur noch gleiche Gruppenplätze übrig
     * (z. B. nur Gruppendritte), spielen diese gegeneinander. Möglichst nicht aus derselben Gruppe und – falls
     * eingestellt – nicht aus demselben Verein.
     */
    private List<String[]> drawFirstQualRound(PlayingClass c, List<String> players) {
        java.util.Map<String, Integer> groupOf = c.getQualField();
        java.util.Map<String, Integer> place = c.getQualPlace();
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        List<String> sorted = new java.util.ArrayList<>(players);   // Feldreihenfolge: Gruppenplatz zuerst
        sorted.sort(java.util.Comparator.comparingInt((String id) -> place.getOrDefault(id, 0)));
        int half = sorted.size() / 2;
        List<String> upper = new java.util.ArrayList<>(sorted.subList(0, half));
        List<String> lower = new java.util.ArrayList<>(sorted.subList(half, sorted.size()));
        java.util.function.BiPredicate<String, String> higher = (a, b) -> place.getOrDefault(a, 0) < place.getOrDefault(b, 0);
        // so viele Paarungen „höher gegen niedriger“ wie überhaupt möglich
        int maxStrict = countMatched(maxMatching(upper, lower, higher));
        boolean clubs = rootOf(c).isAvoidSameClub();
        for (int level = clubs ? 2 : 1; level >= 0; level--) {
            final int lv = level;
            java.util.function.BiPredicate<String, String> ok = (a, b) -> {
                if (lv >= 1 && java.util.Objects.equals(groupOf.get(a), groupOf.get(b))) return false;
                return lv < 2 || idx.get(a) == null || idx.get(b) == null || !idx.get(a).sameClub(idx.get(b));
            };
            for (int attempt = 0; attempt < (level == 0 ? 1 : 80); attempt++) {
                List<String> u = new java.util.ArrayList<>(upper), lo = new java.util.ArrayList<>(lower);
                java.util.Collections.shuffle(u, qualRandom);
                java.util.Collections.shuffle(lo, qualRandom);
                int[] strict = maxMatching(u, lo, (a, b) -> higher.test(a, b) && ok.test(a, b));
                if (countMatched(strict) < maxStrict) continue;
                // Rest (gleiche Gruppenplätze) untereinander
                List<String> restU = new java.util.ArrayList<>(), restL = new java.util.ArrayList<>();
                boolean[] usedU = new boolean[u.size()];
                for (int j = 0; j < lo.size(); j++) if (strict[j] >= 0) usedU[strict[j]] = true; else restL.add(lo.get(j));
                for (int i = 0; i < u.size(); i++) if (!usedU[i]) restU.add(u.get(i));
                int[] rest = maxMatching(restU, restL, ok);
                if (countMatched(rest) < restU.size()) continue;
                List<String[]> out = new java.util.ArrayList<>();
                for (int j = 0; j < lo.size(); j++) if (strict[j] >= 0) out.add(new String[]{u.get(strict[j]), lo.get(j)});
                for (int j = 0; j < restL.size(); j++) if (rest[j] >= 0) out.add(new String[]{restU.get(rest[j]), restL.get(j)});
                java.util.Collections.shuffle(out, qualRandom);
                return out;
            }
        }
        List<String[]> out = new java.util.ArrayList<>();
        for (int i = 0; i < upper.size(); i++) out.add(new String[]{upper.get(i), lower.get(i)});
        return out;
    }

    /** Größtmögliche Zuordnung oben–unten (Kuhn); Ergebnis: je Index unten der Index oben bzw. -1. */
    private static int[] maxMatching(List<String> upper, List<String> lower, java.util.function.BiPredicate<String, String> ok) {
        int[] partner = new int[lower.size()];
        java.util.Arrays.fill(partner, -1);
        for (int i = 0; i < upper.size(); i++) augment(i, new boolean[lower.size()], upper, lower, ok, partner);
        return partner;
    }

    private static boolean augment(int i, boolean[] seen, List<String> upper, List<String> lower,
                                   java.util.function.BiPredicate<String, String> ok, int[] partner) {
        for (int j = 0; j < lower.size(); j++) {
            if (seen[j] || !ok.test(upper.get(i), lower.get(j))) continue;
            seen[j] = true;
            if (partner[j] < 0 || augment(partner[j], seen, upper, lower, ok, partner)) {
                partner[j] = i;
                return true;
            }
        }
        return false;
    }

    private static int countMatched(int[] partner) {
        int n = 0;
        for (int p : partner) if (p >= 0) n++;
        return n;
    }

    /**
     * Qualifikation: tauscht zwei Spieler (oder einen Spieler mit dem Freilos) der aktuellen Runde. Läuft die Phase,
     * nur bei Spielen, die noch nicht am Tisch bzw. gespielt sind; ist sie angehalten, werden betroffene Spiele
     * zurückgesetzt.
     */
    public void swapQualificationPlayers(PlayingClass c, String idA, String idB) {
        if (c.getMode() != TournamentMode.QUALIFICATION || idA == null || idB == null || idA.equals(idB)) return;
        if (rootOf(c).isFinished()) throw new IllegalStateException("Die Klasse ist abgeschlossen.");
        int round = qualificationRound(c);
        if (round == 0) return;
        List<Match> cur = matchesOf(c).stream().filter(m -> m.getRound() == round).toList();
        Match ma = null, mb = null;
        for (Match m : cur) {
            if (idA.equals(m.getA()) || idA.equals(m.getB())) ma = m;
            if (idB.equals(m.getA()) || idB.equals(m.getB())) mb = m;
        }
        List<String> byes = new java.util.ArrayList<>(c.getSwissByes());
        String bye = round - 1 < byes.size() ? byes.get(round - 1) : null;
        if ((ma == null && !idA.equals(bye)) || (mb == null && !idB.equals(bye))) {
            throw new IllegalStateException("Getauscht werden kann nur innerhalb der aktuellen Qualifikationsrunde.");
        }
        if (ma != null && ma == mb) {
            throw new IllegalStateException("Die beiden spielen schon gegeneinander.");
        }
        for (Match m : new Match[]{ma, mb}) {
            if (m == null) continue;
            if (!c.isPaused() && (m.getStatus() == MatchStatus.DONE || m.getStatus().atTable())) {
                throw new IllegalStateException("Spiel " + m.getNumber() + " läuft schon bzw. ist gespielt – zum Umsortieren die Phase "
                        + "anhalten (oder das Ergebnis korrigieren).");
            }
        }
        for (Match m : new Match[]{ma, mb}) {
            if (m == null) continue;
            if (m.getStatus() != MatchStatus.PLANNED) m.clearScore();
            if (idA.equals(m.getA())) m.setA(idB);
            else if (idA.equals(m.getB())) m.setB(idB);
            else if (idB.equals(m.getA())) m.setA(idA);
            else if (idB.equals(m.getB())) m.setB(idA);
        }
        if (ma == null) byes.set(round - 1, idB);
        if (mb == null) byes.set(round - 1, idA);
        c.setSwissByes(byes);
        PlayingClass root = rootOf(c);
        if (root.isFinished()) root.setFinishedAt(null);
        if (c.isStarted() && !c.isPaused()) queueReady(c);
        dispatch();
        markModified();
    }

    /** Rangfolge der KO-Qualifikation (Reihenfolge des Feldes: Gruppenplatz, dann Spiel-/Satz-/Ballverhältnis). */
    private List<String> fieldOrder(PlayingClass c, java.util.Collection<String> ids) {
        List<String> order = new java.util.ArrayList<>(c.getQualField().keySet());
        order.removeIf(id -> !ids.contains(id));
        return order;
    }

    private String qualBye(PlayingClass c, int round) {
        List<String> byes = c.getSwissByes();
        return round - 1 < byes.size() ? byes.get(round - 1) : null;
    }

    private List<Match> roundMatches(PlayingClass c, int round) {
        return matchesOf(c).stream().filter(m -> m.getRound() == round).toList();
    }

    private boolean qualificationRoundDone(PlayingClass c, int round) {
        List<Match> ms = roundMatches(c, round);
        return !ms.isEmpty() || qualBye(c, round) != null
                ? ms.stream().allMatch(m -> m.getStatus() == MatchStatus.DONE) : false;
    }

    /** Sieger einer Runde (inkl. Freilos) bzw. Verlierer. */
    private java.util.Set<String> roundWinners(PlayingClass c, int round) {
        java.util.Set<String> w = new java.util.LinkedHashSet<>();
        String bye = qualBye(c, round);
        if (bye != null) w.add(bye);
        for (Match m : roundMatches(c, round)) if (m.winner() != null) w.add(m.winner());
        return w;
    }

    private java.util.Set<String> roundLosers(PlayingClass c, int round) {
        java.util.Set<String> l = new java.util.LinkedHashSet<>();
        for (Match m : roundMatches(c, round)) if (m.loser() != null) l.add(m.loser());
        return l;
    }

    /** Ist die Runde fertig: Sieger spielen die nächste Runde; nach Runde n ggf. die Zusatzrunde der Verlierer. */
    private void advanceQualification(PlayingClass c) {
        if (c.getMode() != TournamentMode.QUALIFICATION) return;
        int n = c.getQualRounds();
        int round = qualificationRound(c);
        if (round == 0 || round > n || !qualificationRoundDone(c, round)) return;
        if (round < n) {
            if (round == 1 && c.getQualDecision() != 2) return;      // 0.38 (#60): erst fragen, ob die Gewinner qualifiziert sind
            List<String> next = fieldOrder(c, roundWinners(c, round));
            if (next.size() >= 2) addQualificationRound(c, round + 1, next);
        }
    }

    /**
     * 0.38 (#60): Runde 1 der KO-Qualifikation ist gespielt und es ist noch offen, ob die Gewinner qualifiziert sind oder
     * eine weitere KO-Runde spielen.
     */
    public boolean qualificationDecisionPending(PlayingClass c) {
        return c != null && c.getMode() == TournamentMode.QUALIFICATION && c.getQualRounds() >= 2 && c.getQualDecision() == 0
                && qualificationRound(c) == 1 && qualificationRoundDone(c, 1)
                && fieldOrder(c, roundWinners(c, 1)).size() >= 2 && !rootOf(c).isFinished();
    }

    /**
     * Entscheidung nach Runde 1: {@code true} = die Gewinner sind qualifiziert (nur eine KO-Runde; die Verlierer sind
     * Nachrücker 1. Ranges nach TTR), {@code false} = die Gewinner spielen eine weitere KO-Runde.
     */
    public void decideQualification(PlayingClass c, boolean winnersQualified) {
        if (!qualificationDecisionPending(c)) throw new IllegalStateException("Es ist gerade keine Entscheidung offen.");
        if (winnersQualified) {
            c.setQualDecision(1);
            c.setQualification(c.getQualDirect(), c.getQualKo(), 1);
        } else {
            c.setQualDecision(2);
            // 0.41 (#70): Runde 2 wird ausgelost, aber erst mit „Qualifikation Runde 2 starten“ eingereiht – bis dahin
            // lassen sich die Spieler per Drag & Drop tauschen
            if (c.isStarted() && !c.isPaused()) {
                c.setPaused(true);
                c.setAwaitingStart(true);
            }
            advanceQualification(c);
        }
        markModified();
    }

    /** Lässt sich die Entscheidung nach Runde 1 zurücknehmen? */
    public boolean canUndoQualificationDecision(PlayingClass c) {
        if (c == null || c.getMode() != TournamentMode.QUALIFICATION || c.getQualDecision() == 0 || rootOf(c).isFinished()) return false;
        if (c.getQualDecision() == 1) return true;
        return roundMatches(c, 2).stream().noneMatch(m -> m.getStatus() == MatchStatus.DONE);
    }

    /** Nimmt die Entscheidung zurück: „Ja“ bis zum Klassenende, „Nein“ solange in Runde 2 noch kein Ergebnis steht. */
    public void undoQualificationDecision(PlayingClass c) {
        if (!canUndoQualificationDecision(c)) {
            throw new IllegalStateException(c != null && c.getQualDecision() == 2
                    ? "In Runde 2 steht schon ein Ergebnis – die Entscheidung lässt sich nicht mehr zurücknehmen." : "Nichts zurückzunehmen.");
        }
        if (c.getQualDecision() == 1) {
            c.setQualification(c.getQualDirect(), c.getQualKo(), QUAL_WINS);
        } else {
            if (c.isAwaitingStart()) {                       // Runde 2 war noch nicht gestartet
                c.setAwaitingStart(false);
                c.setPaused(false);
            }
            List<Match> r2 = roundMatches(c, 2);
            for (Match m : r2) {
                m.setTable(null);
                m.setStartedAt(null);
            }
            matches.removeAll(r2);
            List<String> byes = new java.util.ArrayList<>(c.getSwissByes());
            if (byes.size() > 1) c.setSwissByes(byes.subList(0, 1));
            dispatch();
        }
        c.setQualDecision(0);
        markModified();
    }


    private boolean isQualificationComplete(PlayingClass c) {
        int n = c.getQualRounds();
        if (c.getQualField().size() < 2 || n == 0) return true;
        int round = qualificationRound(c);
        if (round == 0 || !qualificationRoundDone(c, round)) return false;
        if (round < n) return fieldOrder(c, roundWinners(c, round)).size() < 2;   // offene Entscheidung: nicht abgeschlossen
        if (round > n) return c.getQualExtraDecision() != 0;                    // 1.3 (#105): Zusatzspiel entschieden
        return true;
    }

    /** Sortierung innerhalb einer Stufe: besserer Gruppenplatz zuerst (Zweite vor Dritten), dann TTR. */
    private java.util.Comparator<Entrant> placeThenTtr(PlayingClass c) {
        java.util.Map<String, Integer> place = c.getQualPlace();
        return java.util.Comparator.comparingInt((Entrant e) -> place.getOrDefault(e.id(), 99))
                .thenComparing(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
    }

    /** Direkt (über die Gruppenplatzierung) Qualifizierte, nach TTR. */
    public List<Entrant> qualifiedDirect(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        List<Entrant> direct = new java.util.ArrayList<>();
        for (String id : c.getQualifiedDirect()) if (idx.containsKey(id)) direct.add(idx.get(id));
        direct.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        return direct;
    }

    /**
     * Über die KO-Qualifikation Qualifizierte: Sieger der Runde 2 und – falls gespielt – Sieger der Zusatzrunde (inkl.
     * Freilos); Reihenfolge wie im Feld (Gruppenplatz, Spiel-, Satz-, Ballverhältnis, TTR).
     */
    public List<Entrant> qualifiedViaKo(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        int n = c.getQualRounds();
        java.util.Set<String> ids = new java.util.LinkedHashSet<>();
        if (n > 0 && qualificationRound(c) >= n && qualificationRoundDone(c, n)) {
            for (Match m : roundMatches(c, n)) if (m.winner() != null) ids.add(m.winner());
            String bye = qualBye(c, n);
            if (bye != null) ids.add(bye);
            if (qualificationRound(c) > n && qualificationRoundDone(c, n + 1) && c.getQualExtraDecision() == 1) ids.addAll(roundWinners(c, n + 1));
        }
        List<Entrant> out = new java.util.ArrayList<>();
        for (String id : fieldOrder(c, ids)) if (idx.containsKey(id)) out.add(idx.get(id));
        return out;
    }

    /** Qualifizierte: direkt Qualifizierte (nach TTR), dann die über die KO-Qualifikation. */
    public List<Entrant> qualified(PlayingClass c) {
        List<Entrant> out = new java.util.ArrayList<>(qualifiedDirect(c));
        out.addAll(qualifiedViaKo(c));
        return out;
    }

    /**
     * Nachrücker-Stufen (bereits feststehend), von oben nach unten; die Rangnummern werden fortlaufend vergeben, leere
     * Stufen zählen nicht:
     * <ol>
     *   <li>Verlierer der Runde 2 – bzw., wenn die Zusatzrunde gespielt wurde, deren Verlierer (Gruppenplatz, Spiel-,
     *       Satz-, Ballverhältnis, zuletzt TTR)</li>
     *   <li>Verlierer der Runde 1 (nur nach TTR)</li>
     *   <li>Nachrücker aus der Gruppenphase (Gruppenplatz, dann TTR)</li>
     * </ol>
     */
    public List<List<Entrant>> qualificationTiers(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        int n = c.getQualRounds();
        int round = qualificationRound(c);
        List<List<Entrant>> tiers = new java.util.ArrayList<>();
        java.util.function.Function<java.util.Collection<String>, List<Entrant>> byField = ids -> {
            List<Entrant> l = new java.util.ArrayList<>();
            for (String id : fieldOrder(c, ids)) if (idx.containsKey(id)) l.add(idx.get(id));
            return l;
        };
        if (n > 0 && round >= n && qualificationRoundDone(c, n)) {
            if (round > n) {
                // 1.3 (#105): Zusatzspiel – Gewinner qualifiziert (1) oder Nachrücker 1. Ranges (2); innerhalb nach TTR
                if (qualificationRoundDone(c, n + 1) && c.getQualExtraDecision() != 0) {
                    if (c.getQualExtraDecision() == 2) tiers.add(byTtr(idx, roundWinners(c, n + 1)));
                    tiers.add(byTtr(idx, roundLosers(c, n + 1)));
                }
            } else if (n == 1) {
                // 0.38 (#60): nur eine KO-Runde – die Verlierer sind Nachrücker 1. Ranges, Rangfolge nach TTR
                List<Entrant> l = new java.util.ArrayList<>();
                for (String id : roundLosers(c, 1)) if (idx.containsKey(id)) l.add(idx.get(id));
                l.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
                tiers.add(l);
            } else {
                tiers.add(byField.apply(roundLosers(c, n)));
            }
        }
        if (n > 1 && round >= 1 && qualificationRoundDone(c, 1)) {
            List<Entrant> l = new java.util.ArrayList<>();
            for (String id : roundLosers(c, 1)) if (idx.containsKey(id)) l.add(idx.get(id));
            l.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
            tiers.add(l);
        }
        tiers.add(reserveFromGroups(c));
        tiers.removeIf(List::isEmpty);
        return tiers;
    }

    private static List<Entrant> byTtr(java.util.Map<String, Entrant> idx, java.util.Collection<String> ids) {
        List<Entrant> l = new java.util.ArrayList<>();
        for (String id : ids) if (idx.containsKey(id)) l.add(idx.get(id));
        l.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        return l;
    }

    // --- 1.3 (#105): optionales Zusatzspiel für die Verlierer der letzten Runde --------------------------

    /** Lässt sich das Zusatzspiel anlegen (nach der letzten Runde, mindestens 2 Verlierer)? */
    public boolean canAddQualificationExtra(PlayingClass c) {
        if (c == null || c.getMode() != TournamentMode.QUALIFICATION || c.isQualExtra() || rootOf(c).isFinished()) return false;
        int n = c.getQualRounds();
        return n >= 2 && qualificationRound(c) == n && qualificationRoundDone(c, n) && roundLosers(c, n).size() >= 2;
    }

    /** Zusatzspiel anlegen: die Verlierer der letzten Runde spielen gegeneinander (frei gelost). */
    public void addQualificationExtra(PlayingClass c) {
        if (!canAddQualificationExtra(c)) throw new IllegalStateException("Ein Zusatzspiel ist jetzt nicht möglich.");
        int n = c.getQualRounds();
        c.setQualExtra(true);
        c.setQualExtraDecision(0);
        addQualificationRound(c, n + 1, fieldOrder(c, roundLosers(c, n)));
        PlayingClass root = rootOf(c);
        if (root.isFinished()) root.setFinishedAt(null);
        dispatch();
        markModified();
    }

    /** Zusatzspiel zurücknehmen, solange darin kein Ergebnis steht. */
    public boolean canRemoveQualificationExtra(PlayingClass c) {
        return c != null && c.isQualExtra() && !rootOf(c).isFinished()
                && roundMatches(c, c.getQualRounds() + 1).stream().noneMatch(m -> m.getStatus() == MatchStatus.DONE || m.getStatus().atTable());
    }

    public void removeQualificationExtra(PlayingClass c) {
        if (!canRemoveQualificationExtra(c)) throw new IllegalStateException("Im Zusatzspiel steht schon ein Ergebnis bzw. es läuft.");
        int n = c.getQualRounds();
        matches.removeAll(roundMatches(c, n + 1));
        List<String> byes = new java.util.ArrayList<>(c.getSwissByes());
        if (byes.size() > n) c.setSwissByes(byes.subList(0, n));
        c.setQualExtra(false);
        c.setQualExtraDecision(0);
        dispatch();
        markModified();
    }

    /** Zusatzspiel gespielt, Entscheidung offen? */
    public boolean qualificationExtraDecisionPending(PlayingClass c) {
        return c != null && c.getMode() == TournamentMode.QUALIFICATION && c.isQualExtra() && c.getQualExtraDecision() == 0
                && qualificationRound(c) > c.getQualRounds() && qualificationRoundDone(c, c.getQualRounds() + 1);
    }

    /** Gewinner des Zusatzspiels qualifiziert ({@code true}) oder Nachrücker – änderbar, bis die Klasse beendet ist. */
    public void decideQualificationExtra(PlayingClass c, boolean winnersQualified) {
        if (c == null || !c.isQualExtra() || rootOf(c).isFinished()) throw new IllegalStateException("Nichts zu entscheiden.");
        c.setQualExtraDecision(winnersQualified ? 1 : 2);
        markModified();
    }

    /**
     * Nachrücker direkt aus der Gruppenphase (ohne Spiel): die Gruppenplätze nach direkt Qualifizierten und
     * KO-Qualifikation, z. B. Dritte und Vierte – zuerst nach Gruppenplatz, innerhalb nach TTR.
     */
    private List<Entrant> reserveFromGroups(PlayingClass c) {
        int n = c.getQualReserve();
        PlayingClass src = c.getSourceClass() == null ? null : findClass(c.getSourceClass()).orElse(null);
        if (n <= 0 || src == null) return List.of();
        List<Entrant> out = new java.util.ArrayList<>();
        java.util.Map<String, Integer> placeOf = new java.util.HashMap<>();
        for (int g = 0; g < src.getGroups().size(); g++) {
            List<GroupStanding.Row> rows = standings(src, g);
            int from = qualSkip(c, g, rows.size(), false);        // ab dem ersten Nicht-KO-Teilnehmenden der Gruppe
            for (int p = from; p < from + n && p < rows.size(); p++) {
                out.add(rows.get(p).entrant());
                placeOf.put(rows.get(p).entrant().id(), p + 1);
            }
        }
        out.sort(java.util.Comparator.comparingInt((Entrant e) -> placeOf.get(e.id()))
                .thenComparing(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed()));
        return out;
    }

    /**
     * Gruppe und Gruppenplatz aus der Vorphase je Partei der Qualifikationsphase, z. B. „D3“ (direkt Qualifizierte,
     * KO-Feld und Nachrücker aus der Gruppenphase).
     */
    /**
     * 0.38 (#53): Herkunft einer Partei in einer Folgephase („A2“ = Gruppe A, Platz 2) – Qualifikation und KO ab Phase 2;
     * sonst {@code null}.
     */
    public String originLabel(Match m, String id) {
        if (m == null || id == null) return null;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null || !c.isPhase()) return null;
        if (c.getMode() == TournamentMode.QUALIFICATION) return qualificationPlaceLabels(c).get(id);
        return c.getSeedLabels().get(id);
    }

    public java.util.Map<String, String> qualificationPlaceLabels(PlayingClass c) {
        java.util.Map<String, String> out = new java.util.HashMap<>(c.getSeedLabels());
        PlayingClass src = c.getSourceClass() == null ? null : findClass(c.getSourceClass()).orElse(null);
        if (src != null) {
            for (int g = 0; g < src.getGroups().size(); g++) {
                List<GroupStanding.Row> rows = standings(src, g);
                for (int p = 0; p < rows.size(); p++) out.putIfAbsent(rows.get(p).entrant().id(), groupLabel(src, g) + (p + 1));
            }
        }
        return out;
    }

    /**
     * Wie viele Plätze der Gruppe {@code g} die Qualifikationsphase belegt: direkt Qualifizierte + KO-Teilnehmende
     * (+ mit {@code withReserve} die Nachrücker ohne Spiel).
     */
    private static int qualSkip(PlayingClass phase, int g, int groupSize, boolean withReserve) {
        int direct = Math.min(groupSize, phase.getQualDirect());
        List<Integer> counts = phase.getQualKoCounts();
        int ko = g < counts.size() ? counts.get(g) : Math.min(groupSize, phase.getQualDirect() + phase.getQualKo()) - direct;
        int skip = direct + ko;
        return withReserve ? Math.min(groupSize, skip + phase.getQualReserve()) : skip;
    }

    /** Bezeichnung einer Nachrücker-Stufe: „Nachrücker 1. Ranges“, „Nachrücker 2. Ranges“ … */
    public String qualificationTierLabel(PlayingClass c, int tierIndex) {
        return "Nachrücker " + (tierIndex + 1) + ". Ranges";
    }

    public static final String QUALIFIED_BY_GROUP = "Qualifiziert durch Gruppenplatzierung";
    public static final String QUALIFIED_BY_KO = "Qualifiziert durch KO-Runde";
    public static final String STILL_IN_QUALIFICATION = "noch in der Qualifikation";

    /**
     * Endplatzierung im Qualifikationsmodus: „Qualifiziert durch Gruppenplatzierung“ (nach TTR), „Qualifiziert durch
     * KO-Runde“, „Nachrücker 1., 2., … Ranges“. Wer noch spielt, steht als „noch in der Qualifikation“ vor den
     * Nachrückern (dort, wo er höchstens landen kann).
     */
    private List<Placing> rankQualification(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        List<Placing> list = new java.util.ArrayList<>();
        java.util.Set<String> placed = new java.util.HashSet<>();
        int next = 1;
        // 0.36: die Qualifizierten teilen sich die Plätze (z. B. 1.–16. direkt, 17.–22. über die KO-Runde)
        List<Entrant> direct = qualifiedDirect(c).stream().filter(e -> placed.add(e.id())).toList();
        for (Entrant e : direct) list.add(new Placing(next, next + direct.size() - 1, e, QUALIFIED_BY_GROUP));
        next += direct.size();
        List<Entrant> viaKo = qualifiedViaKo(c).stream().filter(e -> placed.add(e.id())).toList();
        for (Entrant e : viaKo) list.add(new Placing(next, next + viaKo.size() - 1, e, QUALIFIED_BY_KO));
        next += viaKo.size();
        List<List<Entrant>> tiers = qualificationTiers(c);
        for (List<Entrant> t : tiers) for (Entrant e : t) placed.add(e.id());
        List<Entrant> open = new java.util.ArrayList<>();
        for (String id : c.getQualField().keySet()) if (!placed.contains(id) && idx.containsKey(id)) open.add(idx.get(id));
        if (!open.isEmpty()) {
            int from = next;
            for (Entrant e : open) list.add(new Placing(from, from + open.size() - 1, e, STILL_IN_QUALIFICATION));
            next += open.size();
        }
        for (int k = 0; k < tiers.size(); k++) {
            String label = qualificationTierLabel(c, k);
            for (Entrant e : tiers.get(k)) {
                if (placed.contains(e.id()) && list.stream().anyMatch(p -> p.entrant().id().equals(e.id()))) continue;
                list.add(new Placing(next, next, e, label));
                next++;
            }
        }
        return list;
    }

    /** Endgruppe (Platz 1–4) mit übernommenen Vorrundenspielen + Platzierungsspiele um Platz 5, 7, … */
    private void createPlacementPhase(PlayingClass c, PlayingClass next, List<List<GroupStanding.Row>> tables,
                                      java.util.Map<String, String> idMap) {
        List<GroupStanding.Row> ga = tables.get(0), gb = tables.get(1);
        List<String> finalGroup = new java.util.ArrayList<>();
        // Reihenfolge A1, B1, A2, B2 (bei völligem Gleichstand entscheidet diese Reihenfolge)
        for (GroupStanding.Row r : new GroupStanding.Row[]{ga.get(0), gb.get(0), ga.get(1), gb.get(1)}) {
            Entrant e = carryOver(next, r.entrant(), idMap);
            if (e != null) finalGroup.add(e.id());
        }
        List<String> rest = new java.util.ArrayList<>();
        int maxSize = Math.max(ga.size(), gb.size());
        for (int k = 2; k < maxSize; k++) {
            for (List<GroupStanding.Row> t : List.of(ga, gb)) {
                if (k < t.size()) {
                    Entrant e = carryOver(next, t.get(k).entrant(), idMap);
                    if (e != null) rest.add(e.id());
                }
            }
        }
        next.setMode(TournamentMode.PLACEMENT);
        next.setTiebreak(GroupStanding.Tiebreak.HEAD_TO_HEAD);
        next.setGroups(List.of(finalGroup, rest), true);

        List<Match> created = new java.util.ArrayList<>();
        int no = 0;
        String stage = "Endgruppe";
        // übernommene Spiele: Erster gegen Zweiter derselben Gruppe
        for (Match old : matchesOf(c)) {
            String a = idMap.get(old.getA()), b = idMap.get(old.getB());
            if (old.getGroupIndex() < 0 || a == null || b == null || !finalGroup.contains(a) || !finalGroup.contains(b)) continue;
            Match m = new Match(null, next.getName(), stage, 0, ++no, -1, -1, 0, a, b);
            m.restore(MatchStatus.DONE, null, 0, old.getSetsA(), old.getSetsB(), old.getStartedAt(), old.getFinishedAt(),
                    old.getPoints(), old.getRetired());
            m.restorePhaseInfo(0, true);
            created.add(m);
        }
        // neue Spiele: über Kreuz – R1: A1–B2, B1–A2; R2: A1–B1, A2–B2
        if (finalGroup.size() == 4) {
            String a1 = finalGroup.get(0), b1 = finalGroup.get(1), a2 = finalGroup.get(2), b2 = finalGroup.get(3);
            created.add(new Match(null, next.getName(), stage, 1, ++no, -1, -1, 0, a1, b2));
            created.add(new Match(null, next.getName(), stage, 1, ++no, -1, -1, 0, b1, a2));
            created.add(new Match(null, next.getName(), stage, 2, ++no, -1, -1, 0, a1, b1));
            created.add(new Match(null, next.getName(), stage, 2, ++no, -1, -1, 0, a2, b2));
        }
        // Platzierungsspiele: Dritte um Platz 5, Vierte um Platz 7 …
        int place = 5;
        for (int k = 2; k < Math.min(ga.size(), gb.size()); k++, place += 2) {
            String a = idMap.get(ga.get(k).entrant().id()), b = idMap.get(gb.get(k).entrant().id());
            if (a == null || b == null) continue;
            Match m = new Match(null, next.getName(), "Spiel um Platz " + place, 1, ++no, -1, -1, -1, a, b);
            m.restorePhaseInfo(place, false);
            created.add(m);
        }
        matches.addAll(created);
    }

    /**
     * Übernimmt eine Partei in die Folgephase. Einzel: dieselbe Person (ohne Meldung in der Phase – sie bleibt in
     * ihrer Klasse gemeldet); Doppel: eine Paarung der Phase mit denselben Personen.
     */
    private Entrant carryOver(PlayingClass to, Entrant e, java.util.Map<String, String> idMap) {
        if (!e.id().startsWith("pair:")) {
            Player p = players.stream().filter(x -> x.getId().equals(e.id())).findFirst().orElse(null);
            if (p == null) return null;
            idMap.put(e.id(), e.id());
            return Entrant.of(p);
        }
        String pid = e.id().substring(5);
        DoublesPair old = pairs.stream().filter(x -> x.getId().equals(pid)).findFirst().orElse(null);
        if (old == null || !old.isComplete()) return null;
        DoublesPair copy = new DoublesPair(to.getName(), old.getFirst(), old.getSecond());
        copy.setCapacity(old.getCapacity());
        copy.setThird(old.getThird());
        pairs.add(copy);
        Entrant now = to.isTeam() ? Entrant.ofTeam(copy) : Entrant.of(copy);
        idMap.put(e.id(), now.id());
        return now;
    }

    // =====================================================================================================
    // 1.2: Platzierungspläne (#94, #96–#100), Leistungsstufen (#97), Phase 3
    // =====================================================================================================

    /** Gruppenname einer Phase, fortlaufend nach der Vorphase (Leistungsstufen: I, J, K …). */
    public String groupLabel(PlayingClass c, int groupIndex) {
        return GroupDraw.groupName(groupIndex + (c == null ? 0 : c.getGroupNameOffset()));
    }

    /**
     * Einstellungen für eine Plan-Phase.
     *
     * @param mainPlaces Gruppenplätze der Hauptrunde bzw. des KO-Feldes (fortgesetztes/doppeltes KO)
     * @param consPlaces Gruppenplätze der Trostrunde (nur Haupt- und Trostrunde)
     * @param cutoffSize fortgesetztes KO: ab welcher Rundengröße die Verlierer weiterspielen ({@link Plan#CUTOFF_ALL} = alle)
     */
    public record PlanOptions(List<Integer> mainPlaces, List<Integer> consPlaces, boolean thirdMain, boolean thirdCons, int cutoffSize) {
        public static PlanOptions of(List<Integer> places) {
            return new PlanOptions(places, List.of(), false, false, Plan.CUTOFF_ALL);
        }
    }

    /** Welche Plan-Arten nach dieser Phase wählbar sind. */
    public List<Plan.Kind> planKindsAfter(PlayingClass c) {
        if (c.getMode() != TournamentMode.GROUPS || !canHaveNextPhase(c)) return List.of();
        if (phaseNumber(c) == 2) return c.isTiered() ? List.of(Plan.Kind.FINAL_BY_PLACE) : List.of();
        return List.of(Plan.Kind.MAIN_CONSOLATION, Plan.Kind.CONTINUED, Plan.Kind.DOUBLE, Plan.Kind.TOP24, Plan.Kind.TOP32);
    }

    /** „Gruppen nach Leistungsstufen“ ist nur als Phase 2 nach einer Gruppenphase wählbar. */
    public boolean canChooseTiers(PlayingClass c) {
        return c.getMode() == TournamentMode.GROUPS && phaseNumber(c) == 1 && canHaveNextPhase(c);
    }

    private void requireNextPhase(PlayingClass c) {
        if (c.getFollowUpClass() != null) throw new IllegalStateException("Es gibt schon eine weitere Phase.");
        if (rootOf(c).isFinished()) throw new IllegalStateException("Die Klasse ist schon beendet.");
        if (!canHaveNextPhase(c)) {
            throw new IllegalStateException(c.getMode() == TournamentMode.SWISS ? "Nach dem Schweizer System gibt es keine weitere Phase."
                    : "Eine weitere Phase ist hier nicht möglich.");
        }
        if (!isGroupPhaseComplete(c)) throw new IllegalStateException("Die Folgephase kann erst erstellt werden, wenn alle Gruppenspiele abgeschlossen sind.");
    }

    /** Legt die Klasse der Folgephase an (Name „… – Phase n“, Einstellungen der Vorphase). */
    private PlayingClass newPhaseClass(PlayingClass c, java.util.Random random) {
        PlayingClass root = rootOf(c);
        int number = phaseNumber(c) + 1;
        String base = root.getName() + " – Phase " + number;
        String name = base;
        for (int k = 2; findClass(name).isPresent(); k++) name = base + " (" + k + ")";
        PlayingClass next = new PlayingClass(name, c.getDiscipline(), c.getType(), null, null);
        next.setWomanRequired(c.isWomanRequired());
        next.setSourceClass(c.getName());
        addClass(next);
        next.setLotSeed(random.nextLong() | 1L);
        next.setTiebreak(c.getTiebreak());
        next.setGroupsBySize(true);
        next.setGroupSize(c.getGroupSize());
        return next;
    }

    /** Verknüpft die neue Phase und startet sie (wie {@code createNextPhase}). */
    private void finishNewPhase(PlayingClass c, PlayingClass next) {
        c.setFollowUpClass(next.getName());
        if (c.isStarted()) {
            next.setStartedAt(LocalDateTime.now().withNano(0));
            next.setReady(true);
        }
        if (next.getMode() == TournamentMode.PLAN) resolvePlan(next);
        else if (next.isStarted()) queueReady(next);
        if (next.isStarted()) dispatch();
        markModified();
    }

    /**
     * Rangliste der angegebenen Gruppenplätze (alle Gruppen-x-ten, die besten zuerst – Spiel-, Satz-, Ballverhältnis,
     * Los), übernommen in die neue Phase.
     */
    private List<Entrant> rankedFromPlaces(List<List<GroupStanding.Row>> tables, List<Integer> places, PlayingClass next,
                                           java.util.Map<String, String> idMap, java.util.Map<String, Integer> groupOf,
                                           java.util.Map<String, String> labels, PlayingClass source, java.util.Random random) {
        List<Entrant> ranked = new java.util.ArrayList<>();
        for (int placeNo : places) {
            int place = placeNo - 1;
            for (int g : tierOrder(tables, place, random, false)) {
                Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
                if (now != null) {
                    ranked.add(now);
                    groupOf.put(now.id(), g);
                    labels.put(now.id(), groupLabel(source, g) + (place + 1));
                }
            }
        }
        return ranked;
    }

    /** Herkunfts-ID (Vorphase) zu einer ID der neuen Phase. */
    private static String originOf(java.util.Map<String, String> idMap, String newId) {
        if (newId == null) return null;
        for (var e : idMap.entrySet()) if (e.getValue().equals(newId)) return e.getKey();
        return newId;
    }

    private List<String> slotIds(List<Entrant> slots) {
        return slots.stream().map(e -> e == null ? null : e.id()).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    /** Warum diese Plan-Einstellungen nicht gehen – sonst {@code null}. */
    public String planProblem(PlayingClass c, Plan.Kind kind, PlanOptions o) {
        int maxGroup = c.getGroups().stream().mapToInt(List::size).max().orElse(0);
        switch (kind) {
            case MAIN_CONSOLATION -> {
                if (o.mainPlaces() == null || o.mainPlaces().isEmpty()) return "Bitte die Gruppenplätze der Hauptrunde angeben, z. B. 1,2.";
                if (o.consPlaces() == null || o.consPlaces().isEmpty()) return "Bitte die Gruppenplätze der Trostrunde angeben, z. B. 3,4.";
                java.util.Set<Integer> all = new java.util.HashSet<>(o.mainPlaces());
                for (int p : o.consPlaces()) if (!all.add(p)) return "Platz " + p + " ist in Haupt- und Trostrunde angegeben.";
                for (int p : all) if (p < 1 || p > maxGroup) return "Platz " + p + " gibt es nicht – die größte Gruppe hat " + maxGroup + " Plätze.";
                if (countPlaces(c, o.mainPlaces()) < 2) return "In der Hauptrunde müssen mindestens 2 spielen.";
            }
            case CONTINUED, DOUBLE -> {
                String p = placesProblem(c, o.mainPlaces());
                if (p != null) return p;
                if (kind == Plan.Kind.DOUBLE && countPlaces(c, o.mainPlaces()) < 3) return "Das doppelte K.-o.-System braucht mindestens 3 Teilnehmende.";
            }
            case FINAL_BY_PLACE -> {
                if (!c.isTiered()) return "Die Endrunde je Gruppenplatz geht nur nach „Gruppen nach Leistungsstufen“.";
            }
            case TOP24, TOP32 -> {
                if (c.getGroups().size() < 2) return "Dafür braucht es mindestens 2 Gruppen.";
            }
        }
        return null;
    }

    private static int countPlaces(PlayingClass c, List<Integer> places) {
        int n = 0;
        for (List<String> g : c.getGroups()) for (int p : places) if (p >= 1 && p <= g.size()) n++;
        return n;
    }

    /** Folgephase als Platzierungsplan anlegen und (wenn die Klasse läuft) starten. */
    public PlayingClass createPlanPhase(PlayingClass c, Plan.Kind kind, PlanOptions o, java.util.Random random) {
        String opts = o == null ? "-|||||" : joinInts(o.mainPlaces()) + "|" + joinInts(o.consPlaces()) + "|" + o.thirdMain() + "|"
                + o.thirdCons() + "|" + o.cutoffSize();
        return withRecipe("plan|" + (kind == null ? "" : kind.name()) + "|" + opts, random, r -> createPlanPhase0(c, kind, o, r));
    }

    private static String joinInts(List<Integer> l) {
        return l == null ? "" : String.join(",", l.stream().map(String::valueOf).toList());
    }

    private PlayingClass createPlanPhase0(PlayingClass c, Plan.Kind kind, PlanOptions o, java.util.Random random) {
        requireNextPhase(c);
        if (!planKindsAfter(c).contains(kind)) throw new IllegalStateException(kind.getLabel() + " ist nach dieser Phase nicht wählbar.");
        String problem = planProblem(c, kind, o == null ? PlanOptions.of(List.of(1, 2)) : o);
        if (problem != null) throw new IllegalArgumentException(problem);
        PlayingClass next = newPhaseClass(c, random);
        try {
            int groupCount = c.getGroups().size();
            List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
            for (int g = 0; g < groupCount; g++) tables.add(standings(c, g));
            java.util.Map<String, String> idMap = new java.util.HashMap<>();
            java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
            Plan plan;
            switch (kind) {
                case MAIN_CONSOLATION -> {
                    java.util.Map<String, Integer> gm = new java.util.HashMap<>(), gc = new java.util.HashMap<>();
                    List<Entrant> main = rankedFromPlaces(tables, o.mainPlaces(), next, idMap, gm, labels, c, random);
                    List<Entrant> cons = rankedFromPlaces(tables, o.consPlaces(), next, idMap, gc, labels, c, random);
                    List<String> ms = main.size() >= 2 ? slotIds(KnockoutDraw.drawFromRanking(main, gm, groupCount, random)) : slotIds(main);
                    List<String> cs = cons.size() >= 2 ? slotIds(KnockoutDraw.drawFromRanking(cons, gc, groupCount, random)) : slotIds(cons);
                    plan = Plan.mainConsolation(ms, ms.stream().map(x -> originOf(idMap, x)).toList(),
                            cs, cs.stream().map(x -> originOf(idMap, x)).toList(), o.thirdMain(), o.thirdCons());
                }
                case CONTINUED, DOUBLE -> {
                    java.util.Map<String, Integer> gm = new java.util.HashMap<>();
                    List<Entrant> field = rankedFromPlaces(tables, o.mainPlaces(), next, idMap, gm, labels, c, random);
                    List<String> slots = slotIds(KnockoutDraw.drawFromRanking(field, gm, groupCount, random));
                    List<String> origins = slots.stream().map(x -> originOf(idMap, x)).toList();
                    plan = kind == Plan.Kind.CONTINUED ? Plan.continued(slots, origins, o.cutoffSize()) : Plan.doubleKo(slots, origins);
                }
                case TOP24, TOP32 -> plan = Plan.fromTemplate(kind, kind == Plan.Kind.TOP24 ? Plan.TOP24 : Plan.TOP32, token -> {
                    int g = token.charAt(0) - 'A', place = Integer.parseInt(token.substring(1)) - 1;
                    if (g >= tables.size() || place >= tables.get(g).size()) return null;
                    Entrant src = tables.get(g).get(place).entrant();
                    Entrant now = carryOver(next, src, idMap);
                    if (now == null) return null;
                    labels.put(now.id(), groupLabel(c, g) + (place + 1));
                    return new String[]{now.id(), src.id()};
                });
                case FINAL_BY_PLACE -> {
                    List<Plan.Block> blocks = new java.util.ArrayList<>();
                    int g0 = 0, offset = 0;
                    for (int tierGroups : c.getTierSizes()) {
                        int maxSize = 0, count = 0;
                        for (int g = g0; g < g0 + tierGroups && g < groupCount; g++) {
                            maxSize = Math.max(maxSize, tables.get(g).size());
                            count += tables.get(g).size();
                        }
                        for (int place = 0; place < maxSize; place++) {
                            List<String> ids = new java.util.ArrayList<>(), origins = new java.util.ArrayList<>();
                            for (int g = g0; g < g0 + tierGroups && g < groupCount; g++) {
                                if (place >= tables.get(g).size()) {
                                    ids.add(null);
                                    origins.add(null);
                                    continue;
                                }
                                Entrant src = tables.get(g).get(place).entrant();
                                Entrant now = carryOver(next, src, idMap);
                                ids.add(now == null ? null : now.id());
                                origins.add(src.id());
                                if (now != null) labels.put(now.id(), groupLabel(c, g) + (place + 1));
                            }
                            blocks.add(new Plan.Block(ids, origins, offset + place * tierGroups + 1));
                        }
                        offset += Math.max(count, maxSize * tierGroups);
                        g0 += tierGroups;
                    }
                    plan = Plan.finalByPlace(blocks);
                }
                default -> throw new IllegalArgumentException(kind.toString());
            }
            next.setMode(TournamentMode.PLAN);
            next.setPlan(plan);
            next.setSeedLabels(labels);
            c.setNextPhase(TournamentMode.PLAN);
        } catch (RuntimeException e) {
            removeClass(next);
            throw e;
        }
        finishNewPhase(c, next);
        return next;
    }

    /**
     * Phase 2 „Gruppen nach Leistungsstufen“ (#97): Stufen aus je {@code tierWidth} Vorrundenplätzen (z. B. 2 → 1–2, 3–4,
     * 5–6); je Stufe Gruppen der Größe {@code groupSize} – gespiegelt aus den Vorrundengruppen (A+H, B+G …) oder ausgelost.
     * Ergebnisse werden nicht übernommen.
     */
    public PlayingClass createTieredPhase(PlayingClass c, int tierWidth, int groupSize, boolean mirrored, java.util.Random random) {
        return withRecipe("tier|" + tierWidth + "|" + groupSize + "|" + mirrored, random,
                r -> createTieredPhase0(c, tierWidth, groupSize, mirrored, r));
    }

    private PlayingClass createTieredPhase0(PlayingClass c, int tierWidth, int groupSize, boolean mirrored, java.util.Random random) {
        requireNextPhase(c);
        if (!canChooseTiers(c)) throw new IllegalStateException("„Gruppen nach Leistungsstufen“ gibt es nur als Phase 2 nach einer Gruppenphase.");
        if (tierWidth < 1) throw new IllegalArgumentException("Eine Stufe umfasst mindestens einen Gruppenplatz.");
        if (groupSize < 2) throw new IllegalArgumentException("Eine Gruppe braucht mindestens 2 Teilnehmende.");
        int groupCount = c.getGroups().size();
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        for (int g = 0; g < groupCount; g++) tables.add(standings(c, g));
        int maxSize = tables.stream().mapToInt(List::size).max().orElse(0);
        PlayingClass next = newPhaseClass(c, random);
        java.util.Map<String, String> idMap = new java.util.HashMap<>();
        java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
        List<List<String>> groups = new java.util.ArrayList<>();
        List<Integer> tierSizes = new java.util.ArrayList<>();
        List<Integer> mirror = new java.util.ArrayList<>();
        for (int i = 0, j = groupCount - 1; i <= j; i++, j--) {
            mirror.add(i);
            if (j != i) mirror.add(j);
        }
        int perGroup = Math.max(1, groupSize / tierWidth);
        for (int from = 0; from < maxSize; from += tierWidth) {
            int before = groups.size();
            if (mirrored) {
                for (int k = 0; k < mirror.size(); k += perGroup) {
                    List<String> members = new java.util.ArrayList<>();
                    for (int place = from; place < Math.min(maxSize, from + tierWidth); place++) {
                        for (int m = k; m < Math.min(mirror.size(), k + perGroup); m++) {
                            int g = mirror.get(m);
                            if (place >= tables.get(g).size()) continue;
                            Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
                            if (now == null) continue;
                            members.add(now.id());
                            labels.put(now.id(), groupLabel(c, g) + (place + 1));
                        }
                    }
                    if (!members.isEmpty()) groups.add(members);
                }
            } else {
                List<String> pool = new java.util.ArrayList<>();
                for (int place = from; place < Math.min(maxSize, from + tierWidth); place++) {
                    for (int g = 0; g < groupCount; g++) {
                        if (place >= tables.get(g).size()) continue;
                        Entrant now = carryOver(next, tables.get(g).get(place).entrant(), idMap);
                        if (now == null) continue;
                        pool.add(now.id());
                        labels.put(now.id(), groupLabel(c, g) + (place + 1));
                    }
                }
                java.util.Collections.shuffle(pool, random);
                int n = Math.max(1, (int) Math.round(pool.size() / (double) groupSize));
                List<List<String>> tier = new java.util.ArrayList<>();
                for (int k = 0; k < n; k++) tier.add(new java.util.ArrayList<>());
                for (int k = 0; k < pool.size(); k++) tier.get(k % n).add(pool.get(k));
                for (List<String> t : tier) if (!t.isEmpty()) groups.add(t);
            }
            if (groups.size() > before) tierSizes.add(groups.size() - before);
        }
        if (groups.stream().mapToInt(List::size).sum() < 2) {
            removeClass(next);
            throw new IllegalStateException("Für die Folgephase qualifizieren sich weniger als 2.");
        }
        next.setMode(TournamentMode.GROUPS);
        next.setGroups(groups, true);
        next.setTierSizes(tierSizes);
        next.setGroupNameOffset(c.getGroupNameOffset() + groupCount);
        next.setSeedLabels(labels);
        createGroupMatches(next);
        c.setNextPhase(TournamentMode.GROUPS);
        c.setQualifiers(maxSize);                       // alle kommen weiter – keine Restwertung aus Phase 1
        c.setQualifierPlaces(List.of());
        finishNewPhase(c, next);
        return next;
    }

    /** Phase 1 im doppelten K.-o.-System (#99): Auslosung wie beim einfachen KO. */
    public void drawDoubleKo(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        c.setExempt(List.of());
        List<Entrant> entrants = entrantsOf(c);
        if (entrants.size() < 3) throw new IllegalArgumentException("Das doppelte K.-o.-System braucht mindestens 3 Teilnehmende.");
        List<Entrant> slots = KnockoutDraw.draw(entrants, seeded, c.isAvoidSameClub() && !c.isDoubles(), random);
        List<String> ids = slotIds(slots);
        c.setMode(TournamentMode.PLAN);
        c.setSwissByes(List.of());
        c.setGroups(List.of(), seeded);
        c.setDraw(List.of(), seeded);
        c.setPlan(Plan.doubleKo(ids, ids));
        c.setReady(false);
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        resolvePlan(c);
        markModified();
    }

    // --- Auflösen ----------------------------------------------------------------------------------------

    /**
     * Stand eines Plans: je Knoten die beiden Seiten und Sieger/Verlierer ({@code null} = offen, „-“ = Freilos);
     * {@code real} = echtes Spiel, {@code skipped} = entfällt (Stichkampf nicht nötig).
     */
    public record PlanState(java.util.Map<String, String[]> sides, java.util.Map<String, String[]> out,
                            java.util.Set<String> real, java.util.Set<String> skipped) {
    }

    private static String planSrc(Plan p, String src, java.util.Map<String, String[]> out) {
        if (src == null) return null;
        if ("-".equals(src)) return "-";
        int si = Plan.seedIndex(src);
        if (si >= 0) {
            String id = si < p.getSeeds().size() ? p.getSeeds().get(si) : null;
            return id == null ? "-" : id;
        }
        String k = Plan.refKey(src);
        String[] o = k == null ? null : out.get(k);
        if (o == null) return null;
        return src.startsWith("W:") ? o[0] : o[1];
    }

    public PlanState planState(PlayingClass c) {
        Plan p = c.getPlan();
        java.util.Map<String, String[]> sides = new java.util.HashMap<>(), out = new java.util.HashMap<>();
        java.util.Set<String> real = new java.util.HashSet<>(), skipped = new java.util.HashSet<>();
        if (p == null) return new PlanState(sides, out, real, skipped);
        java.util.Map<Integer, Match> byNo = new java.util.HashMap<>();
        for (Match m : matchesOf(c)) byNo.put(m.getNumber(), m);
        List<Plan.Node> nodes = new java.util.ArrayList<>(p.getNodes());
        nodes.sort(java.util.Comparator.comparingInt(Plan.Node::no));
        for (Plan.Node n : nodes) {
            String a = planSrc(p, n.a(), out), b = planSrc(p, n.b(), out);
            sides.put(n.key(), new String[]{a, b});
            if (n.cond() != null && n.cond().startsWith("IFB:")) {
                String ck = n.cond().substring(4);
                String[] o = out.get(ck), sd = sides.get(ck);
                if (o == null || o[0] == null || sd == null) {
                    out.put(n.key(), new String[]{null, null});
                    continue;
                }
                if (!real.contains(ck) || !o[0].equals(sd[1])) {
                    skipped.add(n.key());
                    out.put(n.key(), new String[]{"-", "-"});
                    continue;
                }
            }
            if (a == null || b == null) {
                out.put(n.key(), new String[]{null, null});
            } else if ("-".equals(a) && "-".equals(b)) {
                out.put(n.key(), new String[]{"-", "-"});
            } else if ("-".equals(a) || "-".equals(b)) {
                out.put(n.key(), new String[]{"-".equals(a) ? b : a, "-"});
            } else {
                real.add(n.key());
                Match m = byNo.get(n.no());
                if (m != null && a.equals(m.getA()) && b.equals(m.getB())) out.put(n.key(), new String[]{m.winner(), m.loser()});
                else out.put(n.key(), new String[]{null, null});
            }
        }
        return new PlanState(sides, out, real, skipped);
    }

    /** Spiele des Plans anlegen bzw. anpassen, sobald ihre Gegner feststehen; läuft die Phase, einreihen. */
    void resolvePlan(PlayingClass c) {
        Plan p = c.getPlan();
        if (p == null) return;
        for (int pass = 0; pass < 6; pass++) {
            boolean changed = false;
            PlanState st = planState(c);
            java.util.Map<Integer, Match> byNo = new java.util.HashMap<>();
            for (Match m : matchesOf(c)) byNo.put(m.getNumber(), m);
            for (Plan.Node n : p.getNodes()) {
                Match m = byNo.get(n.no());
                String[] sd = st.sides().get(n.key());
                if (st.real().contains(n.key())) {
                    if (m == null) {
                        matches.add(new Match(null, c.getName(), n.stage(), n.depth(), n.no(), -1, -1, -1, sd[0], sd[1]));
                        changed = true;
                    } else if (!sd[0].equals(m.getA()) || !sd[1].equals(m.getB())) {
                        if (m.getStatus() != MatchStatus.PLANNED) m.clearScore();
                        m.setA(sd[0]);
                        m.setB(sd[1]);
                        changed = true;
                    }
                } else if (m != null) {
                    if (m.getStatus() == MatchStatus.DONE || m.getStatus().atTable()) m.clearScore();
                    matches.remove(m);
                    changed = true;
                }
            }
            if (!changed) break;
        }
        if (c.isStarted() && !c.isPaused()) queueReady(c);
    }

    /** Sind alle Spiele des Plans entschieden? */
    private boolean isPlanComplete(PlayingClass c) {
        PlanState st = planState(c);
        for (Plan.Node n : c.getPlan().getNodes()) {
            String[] o = st.out().get(n.key());
            if (o == null || o[0] == null) return false;
        }
        return true;
    }

    /** Setzplätze tauschen (Drag &amp; Drop vor dem Start der Phase). */
    public void swapPlanSeeds(PlayingClass c, int i, int j) {
        if (c.getPlan() == null) return;
        if (c.isStarted() && matchesOf(c).stream().anyMatch(m -> m.getStatus() != MatchStatus.PLANNED && m.getStatus() != MatchStatus.QUEUED)) {
            throw new IllegalStateException("Die Phase läuft schon – die Auslosung lässt sich nicht mehr ändern.");
        }
        c.getPlan().swapSeeds(i, j);
        for (Match m : matchesOf(c)) if (m.getStatus() == MatchStatus.QUEUED) m.setStatus(MatchStatus.PLANNED);
        resolvePlan(c);
        dispatch();
        markModified();
    }

    /** Korrektur in einem Plan: betroffene Folgespiele melden bzw. (mit {@code reset}) zurücksetzen. */
    private void planConflicts(PlayingClass c, Match m, String newWinner, boolean reset) {
        if (c == null || c.getMode() != TournamentMode.PLAN || c.getPlan() == null) return;
        List<Match> affected = planAffected(c, m, newWinner);
        if (affected.isEmpty()) return;
        if (!reset) throw new FollowUpConflict(affected);
        for (Match x : affected) x.clearScore();
        PlayingClass root = rootOf(c);
        if (root.isFinished()) root.setFinishedAt(null);
    }

    /** Folgespiele, die eine Korrektur in einem Plan zurücksetzen würde (gespielt oder am Tisch). */
    private List<Match> planAffected(PlayingClass c, Match m, String newWinner) {
        List<Match> out = new java.util.ArrayList<>();
        Plan p = c.getPlan();
        Plan.Node n = p.nodeByNumber(m.getNumber());
        if (n == null || newWinner.equals(m.winner())) return out;
        java.util.Set<String> dirty = new java.util.HashSet<>(List.of(n.key()));
        java.util.Map<Integer, Match> byNo = new java.util.HashMap<>();
        for (Match x : matchesOf(c)) byNo.put(x.getNumber(), x);
        List<Plan.Node> nodes = new java.util.ArrayList<>(p.getNodes());
        nodes.sort(java.util.Comparator.comparingInt(Plan.Node::no));
        for (Plan.Node x : nodes) {
            String ka = Plan.refKey(x.a()), kb = Plan.refKey(x.b());
            String kc = x.cond() == null ? null : x.cond().substring(4);
            if ((ka != null && dirty.contains(ka)) || (kb != null && dirty.contains(kb)) || (kc != null && dirty.contains(kc))) {
                dirty.add(x.key());
                Match mm = byNo.get(x.no());
                if (mm != null && (mm.getStatus() == MatchStatus.DONE || mm.getStatus().atTable())) out.add(mm);
            }
        }
        return out;
    }

    /** Text einer Seite eines Plan-Spiels: Name, „Freilos“ oder z. B. „Sieger/in Spiel 12“. */
    public String planSideText(PlayingClass c, PlanState st, Plan.Node n, boolean sideA) {
        String[] sd = st.sides().get(n.key());
        String id = sd == null ? null : sd[sideA ? 0 : 1];
        if ("-".equals(id)) return "Freilos";
        if (id != null) {
            Entrant e = entrantIndex(c).get(id);
            String label = c.getSeedLabels().get(id);
            return (label == null || Plan.seedIndex(sideA ? n.a() : n.b()) < 0 ? "" : label + " ") + (e == null ? "(entfernt)" : e.name());
        }
        return c.getPlan().placeholder(sideA ? n.a() : n.b());
    }

    /** Spiel eines Plan-Knotens (falls schon angelegt). */
    public Match planMatch(PlayingClass c, Plan.Node n) {
        for (Match m : matchesOf(c)) if (m.getNumber() == n.no()) return m;
        return null;
    }

    /** Was Sieger und Verlierer eines Plan-Spiels erwartet, z. B. „Sieger/in → Platz 3“, „Verlierer/in → Spiel 14“. */
    public String planOutcomeText(PlayingClass c, Plan.Node n) {
        Plan p = c.getPlan();
        List<String> parts = new java.util.ArrayList<>();
        String winNext = null, loseNext = null;
        for (Plan.Node x : p.getNodes()) {
            String w = "W:" + n.key(), l = "L:" + n.key();
            if (w.equals(x.a()) || w.equals(x.b())) winNext = "Spiel " + x.no();
            if (l.equals(x.a()) || l.equals(x.b())) loseNext = "Spiel " + x.no();
        }
        if (n.hasWinPlace()) parts.add("Sieger/in: " + placeText(n.winFrom(), n.winTo()));
        else if (winNext != null) parts.add("Sieger/in → " + winNext);
        if (n.hasLosePlace()) parts.add("Verlierer/in: " + placeText(n.loseFrom(), n.loseTo()));
        else if (loseNext != null) parts.add("Verlierer/in → " + loseNext);
        if (n.cond() != null) parts.add("nur nötig, wenn die Siegerin/der Sieger der Verliererrunde das Finale gewinnt");
        return String.join(" · ", parts);
    }

    private static String placeText(int from, int to) {
        return from == to ? "Platz " + from : "Platz " + from + "–" + to;
    }

    // --- 1.3 (#104): Kennzeichnung, wer aus der Gruppenphase weiterkommt -------------------------------

    /** Schlüssel einer Partei über ihre Personen (Doppel werden beim Übernehmen kopiert und bekommen neue IDs). */
    private String personKey(String entrantId) {
        if (entrantId == null) return null;
        if (!entrantId.startsWith("pair:")) return entrantId;
        List<String> ids = new java.util.ArrayList<>();
        for (Player p : personsOf(entrantId)) if (p != null) ids.add(p.getId());
        java.util.Collections.sort(ids);
        return String.join("+", ids);
    }

    /**
     * Wer aus dieser Gruppenphase weiterkommt – nur wenn die Folgephase angelegt ist: Entrant-ID → 1 (dunkelblau: in Phase 2,
     * Hauptrunde, direkt qualifiziert), 2 (hellblau: Trostrunde, KO-Qualifikation), 3 (grau: Nachrücker). Bei
     * Leistungsstufen (alle kommen weiter) leer.
     */
    public java.util.Map<String, Integer> advanceMarks(PlayingClass c) {
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        if (c == null || c.getMode() != TournamentMode.GROUPS || c.getFollowUpClass() == null) return out;
        PlayingClass f = findClass(c.getFollowUpClass()).orElse(null);
        if (f == null || f.isTiered()) return out;
        java.util.Map<String, Integer> byKey = new java.util.HashMap<>();
        if (f.getMode() == TournamentMode.PLAN && f.getPlan() != null) {
            Plan p = f.getPlan();
            for (int i = 0; i < p.getSeeds().size(); i++) {
                String id = p.getSeeds().get(i);
                if (id != null) byKey.put(personKey(id), p.getSeedGroups().get(i) == 0 ? 1 : 2);
            }
        } else if (f.getMode() == TournamentMode.QUALIFICATION) {
            for (Entrant e : qualifiedDirect(f)) byKey.put(personKey(e.id()), 1);
            for (String id : f.getQualField().keySet()) byKey.putIfAbsent(personKey(id), 2);
            for (List<Entrant> tier : qualificationTiers(f)) for (Entrant e : tier) byKey.putIfAbsent(personKey(e.id()), 3);
        } else {
            for (String id : f.getDraw()) if (id != null) byKey.put(personKey(id), 1);
            for (List<String> g : f.getGroups()) for (String id : g) byKey.put(personKey(id), 1);
        }
        for (List<String> g : c.getGroups()) {
            for (String id : g) {
                Integer m = byKey.get(personKey(id));
                if (m != null) out.put(id, m);
            }
        }
        return out;
    }

    /** Tooltip zur Kennzeichnung. */
    public String advanceMarkText(PlayingClass c, int mark) {
        PlayingClass f = c.getFollowUpClass() == null ? null : findClass(c.getFollowUpClass()).orElse(null);
        if (f != null && f.getMode() == TournamentMode.QUALIFICATION) {
            return mark == 1 ? "direkt qualifiziert" : mark == 2 ? "spielt die KO-Qualifikation" : "Nachrücker (ohne Spiel)";
        }
        if (f != null && f.getMode() == TournamentMode.PLAN && f.getPlan() != null && f.getPlan().getResultTitles().size() > 1) {
            return mark == 1 ? "spielt in der " + f.getPlan().getResultTitles().get(0) : "spielt in der " + f.getPlan().getResultTitles().get(1);
        }
        return "zieht in Phase " + (phaseNumber(c) + 1) + " ein";
    }

    // --- Wertung -----------------------------------------------------------------------------------------

    /** Plätze einer Wertung des Plans (0 = Haupt-, 1 = Trostrunde), lückenlos durchnummeriert, sobald alles feststeht. */
    private List<Placing> rankPlan(PlayingClass c, int group) {
        Plan p = c.getPlan();
        PlanState st = planState(c);
        java.util.Map<String, Placing> placed = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Match> byNo = new java.util.HashMap<>();
        for (Match m : matchesOf(c)) byNo.put(m.getNumber(), m);
        java.util.Set<String> members = new java.util.LinkedHashSet<>();
        for (int i = 0; i < p.getSeeds().size(); i++) {
            if (p.getSeeds().get(i) != null && p.getSeedGroups().get(i) == group) members.add(p.getSeeds().get(i));
        }
        List<Plan.Node> nodes = new java.util.ArrayList<>(p.getNodes());
        nodes.sort(java.util.Comparator.comparingInt(Plan.Node::no));
        for (Plan.Node n : nodes) {
            if (p.getSections().get(n.section()).resultGroup() != group) continue;
            String[] o = st.out().get(n.key());
            if (o == null || o[0] == null || st.skipped().contains(n.key())) continue;
            if (pendingDecider(p, st, n)) continue;           // Stichkampf steht noch aus
            if (n.hasWinPlace() && !"-".equals(o[0])) placed.put(o[0], placing(c, o[0], n.winFrom(), n.winTo(), n.stage(), true, n));
            if (n.hasLosePlace() && o[1] != null && !"-".equals(o[1])) placed.put(o[1], placing(c, o[1], n.loseFrom(), n.loseTo(), n.stage(), false, n));
        }
        List<Placing> list = new java.util.ArrayList<>();
        placed.values().forEach(x -> {
            if (x.entrant() != null) list.add(x);
        });
        boolean complete = placed.keySet().containsAll(members);
        list.sort(java.util.Comparator.comparingInt(Placing::from).thenComparingInt(Placing::to));
        List<Placing> out = new java.util.ArrayList<>();
        if (complete || c.isEndedEarly()) {
            // lückenlos: gleiche Bereiche bleiben zusammen, Lücken (Freilose) entfallen
            int next = 1;
            for (int i = 0; i < list.size(); ) {
                int j = i;
                while (j < list.size() && list.get(j).from() == list.get(i).from() && list.get(j).to() == list.get(i).to()) j++;
                int count = j - i;
                for (int k = i; k < j; k++) {
                    Placing x = list.get(k);
                    out.add(new Placing(next, next + count - 1, x.entrant(), x.info()));
                }
                next += count;
                i = j;
            }
        } else {
            out.addAll(list);
        }
        int total = members.size();
        for (String id : members) {
            if (placed.containsKey(id)) continue;
            Entrant e = entrantIndex(c).get(id);
            if (e != null) out.add(new Placing(1, Math.max(1, total), e, c.isEndedEarly() ? "Phase vorzeitig beendet" : "noch im Wettbewerb"));
        }
        out.sort(java.util.Comparator.comparingInt(Placing::from).thenComparingInt(Placing::to));
        return out;
    }

    private static boolean pendingDecider(Plan p, PlanState st, Plan.Node n) {
        for (Plan.Node x : p.getNodes()) {
            if (x.cond() == null || !x.cond().equals("IFB:" + n.key())) continue;
            String[] o = st.out().get(x.key());
            if (!st.skipped().contains(x.key()) && (o == null || o[0] == null)) return true;
        }
        return false;
    }

    private Placing placing(PlayingClass c, String id, int from, int to, String stage, boolean won, Plan.Node n) {
        Entrant e = entrantIndex(c).get(id);
        String info = from == to ? stage + (won ? " (Sieg)" : " verloren") : "ausgeschieden: " + stage;
        if (won && from == 1 && to == 1) info = "Sieger/in";
        return new Placing(from, to, e, info);
    }

    /** Alle Wertungen eines Plans hintereinander (Trostrunde zählt hinter der Hauptrunde weiter – für click-TT). */
    private List<Placing> rankPlanAll(PlayingClass c) {
        List<Placing> out = new java.util.ArrayList<>();
        int groups = c.getPlan().getResultTitles().size();
        for (int g = 0; g < groups; g++) {
            int offset = out.size();
            for (Placing x : rankPlan(c, g)) out.add(new Placing(x.from() + offset, x.to() + offset, x.entrant(), x.info()));
        }
        return out;
    }

    /** Ein Abschnitt des Endergebnisses (z. B. „Hauptrunde“, „Trostrunde“). */
    public record ResultSection(String title, List<Placing> rows) {
    }

    /**
     * Endergebnis zur Anzeige: bei Haupt- und Trostrunde zwei getrennte Wertungen (die Trostrunde zählt ab 1), sonst
     * eine. Wer nicht im Plan spielt, steht in der Hauptwertung dahinter.
     */
    public List<ResultSection> finalResultSections(PlayingClass c) {
        PlayingClass root = rootOf(c);
        FinalResult all = finalResult(root);
        PlayingClass last = lastPhase(root);
        if (last.getMode() != TournamentMode.PLAN || last.getPlan() == null || last.getPlan().getResultTitles().size() < 2) {
            return List.of(new ResultSection("Endergebnis", all.rows()));
        }
        List<ResultSection> out = new java.util.ArrayList<>();
        List<Placing> main = new java.util.ArrayList<>(rankPlan(last, 0));
        java.util.Set<String> inPlan = new java.util.HashSet<>();
        for (int i = 0; i < last.getPlan().getSeeds().size(); i++) {
            if (last.getPlan().getSeeds().get(i) != null) inPlan.add(last.getPlan().getSeeds().get(i));
            if (last.getPlan().getSeedOrigins().get(i) != null) inPlan.add(last.getPlan().getSeedOrigins().get(i));
        }
        int combined = rankPlanAll(last).size(), mainCount = main.size();
        for (Placing x : all.rows()) {
            if (x.entrant() == null || inPlan.contains(x.entrant().id())) continue;
            main.add(new Placing(x.from() - combined + mainCount, x.to() - combined + mainCount, x.entrant(), x.info()));
        }
        out.add(new ResultSection(last.getPlan().getResultTitles().get(0), main));
        for (int g = 1; g < last.getPlan().getResultTitles().size(); g++) {
            out.add(new ResultSection(last.getPlan().getResultTitles().get(g), rankPlan(last, g)));
        }
        return out;
    }

    /** Gruppenwertung ohne die angegebenen Parteien (z. B. die im Plan Spielenden), ab {@code offset}. */
    private List<Placing> rankGroupsExcept(PlayingClass c, java.util.Set<String> except, int offset) {
        int groupCount = c.getGroups().size();
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        int maxSize = 0;
        for (int g = 0; g < groupCount; g++) {
            tables.add(standings(c, g));
            maxSize = Math.max(maxSize, tables.get(g).size());
        }
        List<Placing> list = new java.util.ArrayList<>();
        int next = offset + 1;
        for (int place = 1; place <= maxSize; place++) {
            List<GroupStanding.Row> same = new java.util.ArrayList<>();
            List<Integer> gi = new java.util.ArrayList<>();
            for (int g = 0; g < groupCount; g++) {
                if (place > tables.get(g).size()) continue;
                GroupStanding.Row r = tables.get(g).get(place - 1);
                if (except.contains(r.entrant().id())) continue;
                same.add(r);
                gi.add(g);
            }
            if (same.isEmpty()) continue;
            for (int k = 0; k < same.size(); k++) {
                list.add(new Placing(next, next + same.size() - 1, same.get(k).entrant(),
                        "Gruppe " + groupLabel(c, gi.get(k)) + ", Platz " + place));
            }
            next += same.size();
        }
        return list;
    }

    /** Leistungsstufen ohne Folgephase: Stufe für Stufe, in der Stufe nach Gruppenplatz. */
    private List<Placing> rankTiers(PlayingClass c) {
        List<Placing> out = new java.util.ArrayList<>();
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        for (int g = 0; g < c.getGroups().size(); g++) tables.add(standings(c, g));
        boolean done = isGroupPhaseComplete(c) || c.isEndedEarly();
        int g0 = 0, next = 1;
        for (int tier : c.getTierSizes()) {
            int maxSize = 0;
            for (int g = g0; g < g0 + tier && g < tables.size(); g++) maxSize = Math.max(maxSize, tables.get(g).size());
            for (int place = 1; place <= maxSize; place++) {
                List<Integer> gs = new java.util.ArrayList<>();
                for (int g = g0; g < g0 + tier && g < tables.size(); g++) if (place <= tables.get(g).size()) gs.add(g);
                for (int g : gs) {
                    out.add(new Placing(next, next + gs.size() - 1, tables.get(g).get(place - 1).entrant(),
                            "Gruppe " + groupLabel(c, g) + ", Platz " + place + (done ? "" : " (Zwischenstand)")));
                }
                next += gs.size();
            }
            g0 += tier;
        }
        return out;
    }

    // --- Badeni (#95) ------------------------------------------------------------------------------------

    /** Weitere Runde im Schweizer System/Badeni (z. B. als Platzierungsrunde), solange die Klasse nicht beendet ist. */
    public void addSwissRound(PlayingClass c) {
        if (c.getMode() != TournamentMode.SWISS) throw new IllegalStateException("Nur im Schweizer System bzw. Badeni.");
        if (rootOf(c).isFinished()) throw new IllegalStateException("Die Klasse ist schon beendet.");
        int n = c.getGroups().isEmpty() ? 0 : c.getGroups().get(0).size();
        int rounds = swissRounds(c);
        if (rounds >= SwissSystem.maxRounds(n)) throw new IllegalStateException("Mehr Runden gehen nicht ohne Wiederholungen.");
        c.setSwissRounds(rounds + 1);
        advanceSwiss(c);
        dispatch();
        markModified();
    }

    /** Pausierende festlegen (werden ab der nächsten Runde nicht ausgelost; ihr Stand bleibt). */
    public void setSwissPaused(PlayingClass c, java.util.Collection<String> ids) {
        c.setSwissPaused(ids);
        markModified();
    }

    // --- Endergebnis -------------------------------------------------------------

    /**
     * Eine Platzierung im Endergebnis; {@code from < to} = geteilter bzw. noch offener Bereich (z. B. 5.–8.).
     * {@code info} beschreibt, wie der Platz zustande kam (z. B. „Viertelfinale“, „Gruppe A, Platz 3“).
     */
    public record Placing(int from, int to, Entrant entrant, String info) {
        public String placeText() {
            return from == to ? from + "." : from + ".–" + to + ".";
        }
    }

    /** Endergebnis bzw. – solange die Klasse läuft – der aktuelle Zwischenstand. */
    public record FinalResult(List<Placing> rows, boolean complete) {
    }

    /** Platzierungen der Klasse über alle Phasen. */
    public FinalResult finalResult(PlayingClass c) {
        PlayingClass root = rootOf(c);
        if (!root.isDrawn()) return new FinalResult(List.of(), false);
        return new FinalResult(rank(root, 0), root.isFinished());
    }

    private List<Placing> rank(PlayingClass c, int depth) {
        PlayingClass follow = c.getFollowUpClass() == null || depth > 5 ? null : findClass(c.getFollowUpClass()).orElse(null);
        return switch (c.getMode()) {
            case KNOCKOUT -> rankKnockout(c);
            case PLACEMENT -> rankPlacement(c);
            case QUALIFICATION -> rankQualification(c);
            case PLAN -> c.getPlan() == null ? List.of() : rankPlanAll(c);
            case GROUPS, SWISS -> {
                if (follow == null && !c.getExempt().isEmpty()) {
                    // 1.3 (#106): Freigestellte stehen vor allen Gruppenplätzen (Bereich bis zur Feldgröße noch offen)
                    List<Placing> out = new java.util.ArrayList<>();
                    java.util.Map<String, Entrant> index = entrantIndex(c);
                    int f = Math.max(c.getExempt().size(), exemptFieldSize(c));
                    for (int k = 0; k < c.getExempt().size(); k++) {
                        Entrant e = index.get(c.getExempt().get(k));
                        if (e != null) out.add(new Placing(1, f, e, "freigestellt (S" + (k + 1) + ")"));
                    }
                    out.addAll(rankGroups(c, 0, 0).stream().map(pl -> new Placing(pl.from(), pl.to(), pl.entrant(), pl.info())).toList());
                    yield out;
                }
                if (follow == null) yield c.isTiered() ? rankTiers(c) : rankGroups(c, 0, 0);
                List<Placing> top = new java.util.ArrayList<>(rank(follow, depth + 1));
                if (follow.getMode() == TournamentMode.PLAN && follow.getPlan() != null) {
                    // 1.2: wer nicht im Plan spielt, steht dahinter (nach Gruppenplatz)
                    java.util.Set<String> inPlan = new java.util.HashSet<>();
                    for (String id : follow.getPlan().getSeedOrigins()) if (id != null) inPlan.add(id);
                    top.addAll(rankGroupsExcept(c, inPlan, top.size()));
                } else if (follow.getMode() == TournamentMode.QUALIFICATION) {
                    int[] skips = new int[c.getGroups().size()];
                    for (int g = 0; g < skips.length; g++) skips[g] = qualSkip(follow, g, c.getGroups().get(g).size(), true);
                    top.addAll(rankGroups(c, skips, top.size(), true));
                } else if (follow.getMode() == TournamentMode.KNOCKOUT && !c.getQualifierPlaces().isEmpty()) {
                    // 0.38 (#61): bestimmte Gruppenplätze im KO – Plätze davor zuerst, dann das KO, dann die übrigen
                    java.util.Set<Integer> ko = new java.util.HashSet<>(c.getQualifierPlaces());
                    int first = java.util.Collections.min(ko);
                    java.util.Set<Integer> before = new java.util.HashSet<>(), after = new java.util.HashSet<>();
                    for (int p = 1; p <= 64; p++) if (!ko.contains(p)) (p < first ? before : after).add(p);
                    List<Placing> head = rankGroupPlaces(c, before, 0);
                    List<Placing> out = new java.util.ArrayList<>(head);
                    for (Placing pl : top) out.add(new Placing(pl.from() + head.size(), pl.to() + head.size(), pl.entrant(), pl.info()));
                    out.addAll(rankGroupPlaces(c, after, out.size()));
                    top = out;
                } else if (follow.getMode() != TournamentMode.PLACEMENT) {
                    top.addAll(rankGroups(c, c.getQualifiers(), top.size(), false));
                }
                yield top;
            }
            case NONE -> List.of();
        };
    }

    /** KO: Sieger 1., Finalverlierer 2., Halbfinalverlierer 3., Viertelfinalverlierer 5.–8. … */
    private List<Placing> rankKnockout(PlayingClass c) {
        List<Entrant> slots = drawSlots(c);
        int size = slots.size();
        List<Match> ms = matchesOf(c);
        java.util.Map<String, Match> lostIn = new java.util.HashMap<>();
        String champion = null;
        Match third = thirdPlaceMatch(c);
        for (Match m : ms) {
            if (m == third) continue;
            if (m.loser() != null) lostIn.put(m.loser(), m);
            if (m.getKoRound() >= 0 && (size >> (m.getKoRound() + 1)) == 1 && m.winner() != null) champion = m.winner();
        }
        long alive = slots.stream().filter(e -> e != null && !lostIn.containsKey(e.id())).count();
        // 1.0: mit Freilosen scheiden in einer Runde weniger aus, als sie Plätze hat (120 im 128er-Feld: 65.–120., nicht –128.)
        java.util.Map<Integer, Integer> losers = new java.util.HashMap<>();      // Spiele je Runde = Ausscheidende je Runde
        for (Match m : ms) if (m != third && m.getKoRound() >= 0) losers.merge(m.getKoRound(), 1, Integer::sum);
        List<Placing> list = new java.util.ArrayList<>();
        for (Entrant e : slots) {
            if (e == null) continue;
            Match lost = lostIn.get(e.id());
            if (e.id().equals(champion)) {
                list.add(new Placing(1, 1, e, "Sieger/in"));
            } else if (third != null && lost != null && (size >> lost.getKoRound()) == 4) {
                // Spiel um Platz 3: Gewinner 3., Verlierer 4.; noch offen: 3.–4.
                if (e.id().equals(third.winner())) list.add(new Placing(3, 3, e, "Spiel um Platz 3 (Sieg)"));
                else if (e.id().equals(third.loser())) list.add(new Placing(4, 4, e, "Spiel um Platz 3 verloren"));
                else list.add(new Placing(3, 4, e, "ausgeschieden: " + lost.getStage() + " – Spiel um Platz 3 offen"));
            } else if (lost != null) {
                int inRound = size >> lost.getKoRound();
                int from = inRound / 2 + 1, to = inRound == 4 ? 3 : Math.min(inRound, from + losers.getOrDefault(lost.getKoRound(), 1) - 1);
                if (inRound == 4) from = 3;
                list.add(new Placing(from, to, e, inRound == 2 ? "Finale verloren" : "ausgeschieden: " + lost.getStage()));
            } else {
                list.add(new Placing(1, (int) alive, e, c.isEndedEarly() ? "Phase vorzeitig beendet" : "noch im Wettbewerb"));
            }
        }
        list.sort(java.util.Comparator.comparingInt(Placing::from).thenComparingInt(Placing::to));
        return list;
    }

    /** Gruppen: Platz in der Gruppe; bei mehreren Gruppen teilen sich alle Gruppen-x-ten die Plätze. */
    private List<Placing> rankGroups(PlayingClass c, int skipPlaces, int offset) {
        return rankGroups(c, skipPlaces, offset, false);
    }

    /** @param samePlace alle Gruppen-x-ten bekommen denselben Platz (z. B. viermal „13.“), sonst einen Bereich „13.–16.“ */
    private List<Placing> rankGroups(PlayingClass c, int skipPlaces, int offset, boolean samePlace) {
        int[] skips = new int[c.getGroups().size()];
        java.util.Arrays.fill(skips, skipPlaces);
        return rankGroups(c, skips, offset, samePlace);
    }

    /** Wie oben, je Gruppe mit eigener Zahl übersprungener Plätze (z. B. nur die besten Dritten in der KO-Qualifikation). */
    private List<Placing> rankGroups(PlayingClass c, int[] skips, int offset, boolean samePlace) {
        int groupCount = c.getGroups().size();
        int skipPlaces = java.util.Arrays.stream(skips).min().orElse(0);
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        int maxSize = 0;
        for (int g = 0; g < groupCount; g++) {
            tables.add(standings(c, g));
            maxSize = Math.max(maxSize, tables.get(g).size());
        }
        boolean done = isGroupPhaseComplete(c) || c.isEndedEarly();
        List<Placing> list = new java.util.ArrayList<>();
        int next = offset + 1;
        for (int place = skipPlaces + 1; place <= maxSize; place++) {
            List<GroupStanding.Row> same = new java.util.ArrayList<>();
            List<Integer> groupIdx = new java.util.ArrayList<>();
            for (int g = 0; g < groupCount; g++) {
                if (place <= tables.get(g).size() && place > skips[g]) {
                    same.add(tables.get(g).get(place - 1));
                    groupIdx.add(g);
                }
            }
            if (same.isEmpty()) continue;
            int from = next, to = samePlace ? next : next + same.size() - 1;
            for (int k = 0; k < same.size(); k++) {
                String info = (c.getMode() == TournamentMode.SWISS ? "Schweizer System, "
                        : groupCount > 1 ? "Gruppe " + groupLabel(c, groupIdx.get(k)) + ", " : "Gruppe, ")
                        + "Platz " + place + (done ? "" : " (Zwischenstand)");
                list.add(new Placing(from, to, same.get(k).entrant(), info));
            }
            next = next + same.size();
        }
        return list;
    }

    /** Nur die angegebenen Gruppenplätze (je Platz alle Gruppen, Bereich „x.–y.“). */
    private List<Placing> rankGroupPlaces(PlayingClass c, java.util.Set<Integer> places, int offset) {
        int groupCount = c.getGroups().size();
        List<List<GroupStanding.Row>> tables = new java.util.ArrayList<>();
        int maxSize = 0;
        for (int g = 0; g < groupCount; g++) {
            tables.add(standings(c, g));
            maxSize = Math.max(maxSize, tables.get(g).size());
        }
        boolean done = isGroupPhaseComplete(c);
        List<Placing> list = new java.util.ArrayList<>();
        int next = offset + 1;
        for (int place = 1; place <= maxSize; place++) {
            if (!places.contains(place)) continue;
            List<GroupStanding.Row> same = new java.util.ArrayList<>();
            List<Integer> groupIdx = new java.util.ArrayList<>();
            for (int g = 0; g < groupCount; g++) {
                if (place <= tables.get(g).size()) {
                    same.add(tables.get(g).get(place - 1));
                    groupIdx.add(g);
                }
            }
            if (same.isEmpty()) continue;
            int from = next, to = next + same.size() - 1;
            for (int k = 0; k < same.size(); k++) {
                String info = (groupCount > 1 ? "Gruppe " + groupLabel(c, groupIdx.get(k)) + ", " : "Gruppe, ")
                        + "Platz " + place + (done ? "" : " (Zwischenstand)");
                list.add(new Placing(from, to, same.get(k).entrant(), info));
            }
            next += same.size();
        }
        return list;
    }

    /** Platzierungsspiele: Endgruppe Platz 1–4, dann je Spiel Sieger x., Verlierer x+1.; ohne Gegner kampflos. */
    private List<Placing> rankPlacement(PlayingClass c) {
        List<Placing> list = new java.util.ArrayList<>();
        boolean groupDone = matchesOf(c).stream().filter(m -> m.getGroupIndex() == 0).allMatch(m -> m.getStatus() == MatchStatus.DONE);
        for (GroupStanding.Row r : standings(c, 0)) {
            list.add(new Placing(r.place(), r.place(), r.entrant(), "Endgruppe" + (groupDone ? "" : " (Zwischenstand)")));
        }
        java.util.Set<String> placed = new java.util.HashSet<>();
        int nextPlace = list.size() + 1;
        List<Match> placementMatches = matchesOf(c).stream().filter(m -> m.getPlacement() > 0)
                .sorted(java.util.Comparator.comparingInt(Match::getPlacement)).toList();
        for (Match m : placementMatches) {
            int p = m.getPlacement();
            Entrant a = entrant(m, m.getA()), b = entrant(m, m.getB());
            if (m.winner() != null) {
                boolean aWon = m.winner().equals(m.getA());
                list.add(new Placing(p, p, aWon ? a : b, m.getStage()));
                list.add(new Placing(p + 1, p + 1, aWon ? b : a, m.getStage()));
            } else {
                list.add(new Placing(p, p + 1, a, m.getStage() + " (offen)"));
                list.add(new Placing(p, p + 1, b, m.getStage() + " (offen)"));
            }
            placed.add(m.getA());
            placed.add(m.getB());
            nextPlace = Math.max(nextPlace, p + 2);
        }
        List<List<Entrant>> groups = groupsOf(c);
        if (groups.size() > 1) {
            for (Entrant e : groups.get(1)) {
                if (placed.contains(e.id())) continue;
                list.add(new Placing(nextPlace, nextPlace, e, "ohne Gegner (kampflos)"));
                nextPlace++;
            }
        }
        list.removeIf(p -> p.entrant() == null);
        list.sort(java.util.Comparator.comparingInt(Placing::from).thenComparingInt(Placing::to));
        return list;
    }

    // --- Ergebnisse -----------------------------------------------------------

    private int winningSets = 3;

    /** nur für {@link #recordResult(Match, int, int)}: erzwungene Gewinnsätze (0 = Einstellung der Klasse). */
    private int forcedWinningSets;

    /** Standard-Gewinnsätze (3 = „Best of 5“); maßgeblich ist die Einstellung der Klasse. */
    public int getWinningSets() { return winningSets; }
    public void setWinningSets(int n) { winningSets = Math.max(1, n); markModified(); }

    /** Gewinnsätze eines Spiels: Einstellung seiner Klasse (Phasen: die der Stammklasse), 3 oder 4. */
    public int winningSets(Match m) {
        if (forcedWinningSets > 0) return forcedWinningSets;
        PlayingClass c = m == null ? null : findClass(m.getClassName()).orElse(null);
        return c == null ? winningSets : rootOf(c).getWinningSets();
    }

    /** Gewinnsätze einer Klasse ändern – nur solange in keiner ihrer Phasen ein Ergebnis eingetragen ist. */
    public void setWinningSets(PlayingClass c, int n) {
        PlayingClass root = rootOf(c);
        if (root.getWinningSets() == (n == 4 ? 4 : 3)) return;
        for (PlayingClass p : phasesOf(root)) {
            if (matchesOf(p).stream().anyMatch(m -> m.getStatus() == MatchStatus.DONE)) {
                throw new IllegalStateException("Die Gewinnsätze lassen sich nicht mehr ändern – es sind schon Ergebnisse eingetragen.");
            }
        }
        root.setWinningSets(n);
        markModified();
    }

    /**
     * Erfasst das Ergebnis eines laufenden Spiels mit den Punkten jedes Satzes. Das Spiel ist danach
     * abgeschlossen, der Tisch frei; im KO-System rückt der Sieger weiter, und freie Tische werden neu belegt.
     *
     * @param sets    Punkte je Satz, erste Zahl = erstgenannte Partei (A)
     * @param retired 0 = keine Aufgabe, 1 = A gibt auf, 2 = B gibt auf (dann zählt 11:0 je Gewinnsatz für den Gegner)
     * @throws IllegalArgumentException bei ungültigen Sätzen oder nicht entschiedenem Spiel
     */
    public void recordResult(Match m, List<int[]> sets, int retired) {
        if (m.getStatus() != MatchStatus.ACTIVE && m.getStatus() != MatchStatus.QUEUED) {
            throw new IllegalStateException("Ein Ergebnis kann nur für ein aktives oder eingereihtes Spiel erfasst werden.");
        }
        if (!m.isReady()) throw new IllegalStateException("Die Gegner dieses Spiels stehen noch nicht fest.");
        undoMatch = m;                                   // 1.5.0 (#131): für „Letztes Ergebnis zurücknehmen“
        undoTable = m.getTable();
        undoStarted = m.getStartedAt();
        undoQueueOrder = m.getQueueOrder();
        applyScore(m, sets, retired);
        m.setStatus(MatchStatus.DONE);
        m.setTable(null);
        m.setFinishedAt(LocalDateTime.now().withNano(0));
        if (retired != 0) applyRetirement(m);
        if (m.isKnockout()) findClass(m.getClassName()).ifPresent(this::resolveKnockout);
        findClass(m.getClassName()).ifPresent(c -> {
            if (c.getMode() == TournamentMode.PLAN) resolvePlan(c);
        });
        findClass(m.getClassName()).ifPresent(this::advanceSwiss);
        findClass(m.getClassName()).ifPresent(this::advanceQualification);
        findClass(m.getClassName()).ifPresent(c -> {           // 1.0 (#77): nächste Runde nach Zeitplan freigeben/erfragen
            if (c.isStarted() && hasSchedule(c)) queueReady(c);
        });
        dispatch();
        markModified();
    }

    // --- 1.5.0 (#131): Letztes Ergebnis zurücknehmen -------------------------------------------------

    private Match undoMatch;
    private Integer undoTable;
    private LocalDateTime undoStarted;
    private long undoQueueOrder;

    /**
     * Das zuletzt eingetragene Ergebnis (seit dem Öffnen), wenn es sich noch gefahrlos zurücknehmen lässt: ohne
     * Aufgabe, Klasse nicht beendet, keine spätere Phase, danach in der Klasse nichts weiter gespielt, im KO-System das
     * Folgespiel noch nicht am Tisch; Schweizer System, Qualifikation und Platzierungspläne nur, solange keine neue
     * Runde daraus entstanden ist. Sonst {@code null} (dann über „Ergebnis korrigieren“).
     */
    public Match undoableResult() {
        Match m = undoMatch;
        if (m == null || !matches.contains(m) || m.getStatus() != MatchStatus.DONE || m.getRetired() != 0) return null;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null || rootOf(c).isFinished()) return null;
        List<PlayingClass> phases = phasesOf(c);
        if (phases.get(phases.size() - 1) != c) return null;
        if (correctionOf(c) != null) return null;
        for (Match x : matchesOf(c)) {
            if (x == m) continue;
            if (x.getFinishedAt() != null && m.getFinishedAt() != null && x.getFinishedAt().isAfter(m.getFinishedAt())) return null;
        }
        if (c.getMode() == TournamentMode.SWISS || c.getMode() == TournamentMode.QUALIFICATION || c.getMode() == TournamentMode.PLAN) {
            int maxRound = matchesOf(c).stream().mapToInt(Match::getRound).max().orElse(0);
            if (m.getRound() < maxRound) return null;
            if (c.getMode() != TournamentMode.SWISS) return null;   // Qualifikation/Plan: Folgespiele hängen vom Ergebnis ab
        }
        if (m.isKnockout()) {
            Match next = koMatch(c, m.getKoRound() + 1, m.getKoIndex() / 2);
            if (next != null && (next.getStatus() == MatchStatus.DONE || next.getStatus().atTable())) return null;
            Match third = thirdPlaceMatch(c);
            if (third != null && third != m && (third.getStatus() == MatchStatus.DONE || third.getStatus().atTable())) return null;
        }
        return m;
    }

    /** Nimmt das letzte Ergebnis zurück: das Spiel läuft wieder (am alten Tisch, wenn er frei ist, sonst eingereiht). */
    public Match undoLastResult() {
        Match m = undoableResult();
        if (m == null) throw new IllegalStateException("Das letzte Ergebnis lässt sich nicht mehr zurücknehmen – bitte über „Ergebnis korrigieren“.");
        PlayingClass c = findClass(m.getClassName()).orElseThrow();
        m.clearScore();
        if (m.isKnockout()) resolveKnockout(c);
        if (undoTable != null && activeMatchAt(undoTable).isEmpty() && !isTableDisabled(undoTable)) {
            m.setStatus(MatchStatus.ACTIVE);
            m.setTable(undoTable);
            m.setStartedAt(undoStarted);
        } else {
            m.setStatus(MatchStatus.QUEUED);
            m.setQueueOrder(undoQueueOrder == 0 ? ++queueCounter : undoQueueOrder);
        }
        undoMatch = null;
        dispatch();
        markModified();
        return m;
    }

    // --- 2er/3er Turnier: Begegnung Team gegen Team = ein Spiel ------------------------------------

    /** Eine Partie einer Begegnung: Bezeichnung und Positionen (0-basiert) beider Teams. */
    public record Rubber(String label, int[] posA, int[] posB) {
    }

    /** Ist das Spiel eine Team-Begegnung (2er/3er Turnier)? */
    public boolean isTeamMatch(Match m) {
        PlayingClass c = m == null ? null : findClass(m.getClassName()).orElse(null);
        return c != null && rootOf(c).isTeam();
    }

    /** Bezeichnungen der Positionen eines Teams, z. B. „H1“, „H2“, „D“ bzw. „1“, „2“, „3“. */
    public List<String> teamPositions(PlayingClass c) {
        PlayingClass root = rootOf(c);
        if (root.getDiscipline() == Discipline.TEAM3) return root.isWomanRequired() ? List.of("H1", "H2", "D") : List.of("1", "2", "3");
        return List.of("1", "2");
    }

    /**
     * Partien einer Begegnung (immer alle werden gespielt): 3er Turnier 5 Partien; 2er Turnier in der Gruppenphase 3,
     * im KO-System 5.
     */
    public List<Rubber> rubbersOf(Match m) {
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null) return List.of();
        PlayingClass root = rootOf(c);
        if (root.getDiscipline() == Discipline.TEAM3) {
            if (root.isWomanRequired()) {
                return List.of(new Rubber("Mixed", new int[]{1, 2}, new int[]{1, 2}),
                        new Rubber("Herren-Einzel", new int[]{0}, new int[]{0}),
                        new Rubber("Damen-Einzel", new int[]{2}, new int[]{2}),
                        new Rubber("Herren-Doppel", new int[]{0, 1}, new int[]{0, 1}),
                        new Rubber("Mixed", new int[]{0, 2}, new int[]{0, 2}));
            }
            return List.of(new Rubber("Doppel 1", new int[]{1, 2}, new int[]{1, 2}),
                    new Rubber("Einzel 1", new int[]{0}, new int[]{0}),
                    new Rubber("Einzel 2", new int[]{2}, new int[]{2}),
                    new Rubber("Doppel 2", new int[]{0, 1}, new int[]{0, 1}),
                    new Rubber("Doppel 3", new int[]{0, 2}, new int[]{0, 2}));
        }
        if (root.getDiscipline() == Discipline.TEAM2) {
            List<Rubber> list = new java.util.ArrayList<>(List.of(
                    new Rubber("Einzel", new int[]{0}, new int[]{0}),
                    new Rubber("Einzel", new int[]{1}, new int[]{1}),
                    new Rubber("Doppel", new int[]{0, 1}, new int[]{0, 1})));
            if (c.getMode() == TournamentMode.KNOCKOUT || c.getMode() == TournamentMode.PLAN || m.isKnockout()) {
                list.add(new Rubber("Einzel", new int[]{0}, new int[]{1}));
                list.add(new Rubber("Einzel", new int[]{1}, new int[]{0}));
            }
            return list;
        }
        return List.of();
    }

    /** Mitglieder eines Teams (Partei) in Team-Reihenfolge. */
    public List<Player> teamMembers(String entrantId) {
        return personsOf(entrantId);
    }

    /** Standard-Aufstellung eines Teams: Reihenfolge im Team (2er/3er mit Dame: Dame hinten). */
    public List<String> defaultLineup(Match m, String entrantId) {
        List<Player> ms = new java.util.ArrayList<>(personsOf(entrantId));
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c != null && rootOf(c).isWomanRequired()) {
            ms.sort(java.util.Comparator.comparingInt(p -> p.getSex() == de.hts.player.Sex.FEMALE ? 1 : 0));
        }
        return ms.stream().map(Player::getId).toList();
    }

    private Player playerById(String id) {
        if (id == null) return null;
        java.util.Map<String, Player> cache = playerIdCache;
        Player hit = cache == null ? null : cache.get(id);
        if (hit != null && id.equals(hit.getId())) return hit;
        // 1.0.1: Index neu aufbauen (Liste geändert, ID geändert oder unbekannt) – große Turniere: keine lineare Suche je Zelle
        cache = new java.util.HashMap<>();
        for (Player p : players) cache.put(p.getId(), p);
        playerIdCache = cache;
        return cache.get(id);
    }

    private java.util.Map<String, Player> playerIdCache;

    /** Vorgabe einer Partie (positiv = Team A) nach den Vorgaberegeln, 0 ohne Vorgabeturnier/Aufstellung. */
    public int rubberHandicap(Match m, Rubber r, List<String> lineupA, List<String> lineupB) {
        if (!isHandicapReady() || lineupA == null || lineupB == null) return 0;
        List<Player> a = new java.util.ArrayList<>(), b = new java.util.ArrayList<>();
        for (int i : r.posA()) if (i < lineupA.size() && playerById(lineupA.get(i)) != null) a.add(playerById(lineupA.get(i)));
        for (int i : r.posB()) if (i < lineupB.size() && playerById(lineupB.get(i)) != null) b.add(playerById(lineupB.get(i)));
        if (a.size() != r.posA().length || b.size() != r.posB().length) return 0;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        return handicapBetween(a, b, womenTable(c == null ? null : rootOf(c)));
    }

    private void checkLineup(Match m, String entrantId, List<String> lineup, String side) {
        PlayingClass c = rootOf(findClass(m.getClassName()).orElseThrow());
        List<Player> members = personsOf(entrantId);
        int size = c.teamSize();
        if (lineup == null || lineup.size() != size || new java.util.HashSet<>(lineup).size() != size) {
            throw new IllegalArgumentException("Bitte die Aufstellung von Team " + side + " vollständig wählen (jede Person einmal).");
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        members.forEach(p -> ids.add(p.getId()));
        if (!ids.containsAll(lineup)) throw new IllegalArgumentException("Die Aufstellung von Team " + side + " enthält Fremde.");
        if (c.isWomanRequired()) {
            int pos = c.getDiscipline() == Discipline.TEAM3 ? 2 : 1;
            boolean hasWoman = members.stream().anyMatch(p -> p.getSex() == de.hts.player.Sex.FEMALE);
            Player atPos = playerById(lineup.get(pos));
            if (hasWoman && (atPos == null || atPos.getSex() != de.hts.player.Sex.FEMALE)) {
                throw new IllegalArgumentException("Team " + side + ": Auf Position " + teamPositions(c).get(pos) + " muss eine Dame spielen.");
            }
        }
    }

    /**
     * Ergebnis einer Team-Begegnung: Aufstellung beider Teams und je Partie der Sieger (1 = Team A, 2 = Team B).
     * Es werden immer alle Partien gespielt; das Spiel zählt die gewonnenen Partien (z. B. 3:2). Funktioniert auch
     * als Korrektur (bei Siegerwechsel im KO-System wie {@link #correctResult(Match, List, int, boolean)}).
     */
    public void recordTeamResult(Match m, List<String> lineupA, List<String> lineupB, List<Integer> winners, boolean resetFollowing) {
        if (!isTeamMatch(m)) throw new IllegalArgumentException("Kein Spiel eines 2er/3er Turniers.");
        if (!m.isReady()) throw new IllegalStateException("Die Gegner dieses Spiels stehen noch nicht fest.");
        boolean correction = m.getStatus() == MatchStatus.DONE;
        if (!correction && m.getStatus() != MatchStatus.ACTIVE && m.getStatus() != MatchStatus.QUEUED) {
            throw new IllegalStateException("Ein Ergebnis kann nur für ein aktives oder eingereihtes Spiel erfasst werden.");
        }
        List<Rubber> rubbers = rubbersOf(m);
        if (winners == null || winners.size() != rubbers.size() || winners.stream().anyMatch(w -> w == null || (w != 1 && w != 2))) {
            throw new IllegalArgumentException("Bitte für jede Partie den Sieger angeben (es werden immer alle Partien gespielt).");
        }
        checkLineup(m, m.getA(), lineupA, "A");
        checkLineup(m, m.getB(), lineupB, "B");
        List<int[]> sets = new java.util.ArrayList<>();
        int wa = 0, wb = 0;
        for (int w : winners) {
            sets.add(w == 1 ? new int[]{1, 0} : new int[]{0, 1});
            if (w == 1) wa++;
            else wb++;
        }
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (correction) {
            String newWinner = wa > wb ? m.getA() : m.getB();
            planConflicts(c, m, newWinner, resetFollowing);
            if (m.isKnockout() && !newWinner.equals(m.winner()) && c != null) {
                List<Match> affected = new java.util.ArrayList<>();
                for (int r = m.getKoRound() + 1, idx = m.getKoIndex() / 2; ; r++, idx /= 2) {
                    Match next = koMatch(c, r, idx);
                    if (next == null) break;
                    if (next.getStatus() == MatchStatus.DONE || next.getStatus().atTable()) affected.add(next);
                }
                if (!affected.isEmpty()) {
                    if (!resetFollowing) throw new FollowUpConflict(affected);
                    for (Match x : affected) x.clearScore();
                    PlayingClass root = rootOf(c);
                    if (root.isFinished()) root.setFinishedAt(null);
                }
            }
        }
        m.setScore(sets, 0, wa, wb);
        m.setLineups(lineupA, lineupB);
        if (correction) {
            syncCarriedCopies(m);
        } else {
            m.setStatus(MatchStatus.DONE);
            m.setTable(null);
            m.setFinishedAt(LocalDateTime.now().withNano(0));
        }
        if (m.isKnockout() && c != null) resolveKnockout(c);
        if (c != null && c.getMode() == TournamentMode.PLAN) resolvePlan(c);
        if (correction && c != null && !c.isSideClass()) updateCorrection(c);       // 1.3 (#107)
        dispatch();
        markModified();
    }

    /** Kurzform für Tests/Importe: nur Satzverhältnis (z. B. 3:1); die Sätze werden als 11:0 bzw. 0:11 erzeugt. */
    public void recordResult(Match m, int setsA, int setsB) {
        if (setsA < 0 || setsB < 0 || setsA == setsB) {
            throw new IllegalArgumentException("Bitte ein eindeutiges Ergebnis eingeben (z. B. 3:1).");
        }
        List<int[]> sets = new java.util.ArrayList<>();
        // Verlierer-Sätze zuerst, damit das Spiel erst mit dem letzten Satz entschieden ist
        for (int i = 0; i < Math.min(setsA, setsB); i++) sets.add(setsA > setsB ? new int[]{0, 11} : new int[]{11, 0});
        for (int i = 0; i < Math.max(setsA, setsB); i++) sets.add(setsA > setsB ? new int[]{11, 0} : new int[]{0, 11});
        forcedWinningSets = Math.max(setsA, setsB);
        try {
            recordResult(m, sets, 0);
        } finally {
            forcedWinningSets = 0;
        }
    }

    /**
     * Korrigiert das Ergebnis eines abgeschlossenen Spiels. Ändert sich im KO-System dadurch der Sieger,
     * wird das Folgespiel angepasst – das geht nur, solange das Folgespiel noch nicht abgeschlossen ist.
     */
    public void correctResult(Match m, List<int[]> sets, int retired) {
        correctResult(m, sets, retired, false);
    }

    /** Folgespiele, die bei einem Siegerwechsel zurückgesetzt werden müssten (Abbruch ohne Bestätigung). */
    public static final class FollowUpConflict extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        private final transient List<Match> affected;

        FollowUpConflict(List<Match> affected) {
            super("Der Sieger ändert sich – die Folgespiele " + affected.stream().map(x -> String.valueOf(x.getNumber()))
                    .collect(java.util.stream.Collectors.joining(", ")) + " wurden schon gespielt bzw. laufen.");
            this.affected = List.copyOf(affected);
        }

        public List<Match> getAffected() {
            return affected;
        }
    }

    /**
     * Korrigiert das Ergebnis eines abgeschlossenen Spiels – auch in abgeschlossenen Phasen; Tabellen und Platzierungen
     * rechnen sich neu. Ändert sich im KO-System der Sieger und wurden Folgespiele schon gespielt (oder laufen),
     * werden diese mit {@code resetFollowing} zurückgesetzt (Ergebnis gelöscht, neu eingereiht); sonst
     * {@link FollowUpConflict}.
     */
    public void correctResult(Match m, List<int[]> sets, int retired, boolean resetFollowing) {
        if (m.getStatus() != MatchStatus.DONE) throw new IllegalStateException("Nur abgeschlossene Spiele können korrigiert werden.");
        Scoring.Evaluation ev = evaluateScore(m, sets, retired);
        String newWinner = ev.winner() == 1 ? m.getA() : m.getB();
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        planConflicts(c, m, newWinner, resetFollowing);
        if (m.isKnockout() && !newWinner.equals(m.winner()) && c != null) {
            List<Match> affected = new java.util.ArrayList<>();
            for (int r = m.getKoRound() + 1, idx = m.getKoIndex() / 2; ; r++, idx /= 2) {
                Match next = koMatch(c, r, idx);
                if (next == null) break;
                if (next.getStatus() == MatchStatus.DONE || next.getStatus().atTable()) affected.add(next);
            }
            Match third = thirdPlaceMatch(c);
            if (third != null && m.getKoRound() <= semifinalRound(c)
                    && (third.getStatus() == MatchStatus.DONE || third.getStatus().atTable())) affected.add(third);
            if (!affected.isEmpty()) {
                if (!resetFollowing) throw new FollowUpConflict(affected);
                for (Match x : affected) x.clearScore();
                PlayingClass root = rootOf(c);
                if (root.isFinished()) root.setFinishedAt(null);    // Klasse muss neu abgeschlossen werden
            }
        }
        String oldQuitter = quitterOf(m);
        applyScore(m, sets, retired);
        String newQuitter = quitterOf(m);
        if (oldQuitter != null && !oldQuitter.equals(newQuitter)) revertRetirement(m, oldQuitter);
        if (newQuitter != null && !newQuitter.equals(oldQuitter)) applyRetirement(m);
        syncCarriedCopies(m);
        if (m.isKnockout() && c != null) resolveKnockout(c);
        if (c != null && c.getMode() == TournamentMode.PLAN) resolvePlan(c);
        // 1.3 (#107): Folgephase neu berechnen – weicht sie ab, erscheint der Reiter „Phase n+1 – Korrektur“
        if (c != null && !c.isSideClass()) updateCorrection(c);
        dispatch();
        markModified();
    }

    // --- Aufgabe -------------------------------------------------------------

    /** Wer in diesem Spiel aufgegeben hat (Entrant-ID) oder {@code null}. */
    private static String quitterOf(Match m) {
        if (m.getRetired() == 0 || m.getRetirementOf() != null) return null;
        return m.getRetired() == 1 ? m.getA() : m.getB();
    }

    /**
     * Gibt eine Partei auf, werden ihre noch nicht gespielten Spiele dieser Phase für die Gegner gewertet
     * (3:0, alle Sätze 11:0) – für die Turnierwertung. Diese Spiele werden nicht exportiert; das Spiel mit der
     * Aufgabe selbst schon (0:3, Sätze 0:11). Im Schweizer System wird die Partei nicht mehr gepaart.
     */
    private void applyRetirement(Match m) {
        String quitter = quitterOf(m);
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (quitter == null || c == null) return;
        c.addWithdrawn(quitter);
        for (Match x : matchesOf(c)) {
            if (x == m || !x.isReady() || !(quitter.equals(x.getA()) || quitter.equals(x.getB()))) continue;
            if (x.getStatus() == MatchStatus.DONE || x.getStatus().atTable()) continue;
            int side = quitter.equals(x.getA()) ? 1 : 2;
            int ws = winningSets(x);
            List<int[]> wo = Scoring.walkover(side, ws);
            x.setScore(wo, side, side == 1 ? 0 : ws, side == 1 ? ws : 0);
            x.setStatus(MatchStatus.DONE);
            x.setTable(null);
            x.setFinishedAt(LocalDateTime.now().withNano(0));
            x.setRetirementOf(quitter);
        }
    }

    /** Aufgabe wurde wegkorrigiert: die deshalb kampflos gewerteten Spiele wieder öffnen. */
    private void revertRetirement(Match m, String quitter) {
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null) return;
        c.removeWithdrawn(quitter);
        for (Match x : matchesOf(c)) {
            if (quitter.equals(x.getRetirementOf())) {
                x.clearScore();
                x.setRetirementOf(null);
            }
        }
        if (c.isStarted()) queueReady(c);
    }

    /** In eine Endgruppe übernommene Kopie eines Vorrundenspiels mitkorrigieren. */
    private void syncCarriedCopies(Match m) {
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null || c.getFollowUpClass() == null) return;
        PlayingClass next = findClass(c.getFollowUpClass()).orElse(null);
        if (next == null) return;
        String na = entrantName(m, m.getA()), nb = entrantName(m, m.getB());
        for (Match x : matchesOf(next)) {
            if (!x.isCarried()) continue;
            String xa = entrantName(x, x.getA()), xb = entrantName(x, x.getB());
            if (na.equals(xa) && nb.equals(xb)) x.setScore(m.getPoints(), m.getRetired(), m.getSetsA(), m.getSetsB());
            else if (na.equals(xb) && nb.equals(xa)) {
                List<int[]> flipped = new java.util.ArrayList<>();
                for (int[] p : m.getPoints()) flipped.add(new int[]{p[1], p[0]});
                x.setScore(flipped, m.getRetired() == 0 ? 0 : 3 - m.getRetired(), m.getSetsB(), m.getSetsA());
            }
        }
    }

    private Scoring.Evaluation evaluateScore(Match m, List<int[]> sets, int retired) {
        int ws = winningSets(m);
        List<int[]> effective = retired != 0 ? Scoring.walkover(retired, ws) : sets;
        Scoring.Evaluation ev = Scoring.evaluate(effective, ws);
        if (!ev.isComplete()) throw new IllegalArgumentException(ev.message());
        return ev;
    }

    private void applyScore(Match m, List<int[]> sets, int retired) {
        Scoring.Evaluation ev = evaluateScore(m, sets, retired);
        List<int[]> effective = retired != 0 ? Scoring.walkover(retired, winningSets(m)) : sets;
        m.setScore(effective, retired, ev.setsA(), ev.setsB());
    }

    /** Name einer Partei (für Anzeigen), über alle Klassen. */
    public String entrantName(Match m, String entrantId) {
        if (entrantId == null) return null;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        if (c == null) return "(entfernt)";
        // 1.0.1: gezielt nachschlagen statt je Aufruf den Index aller Personen zu bauen (Seite „Spiele“ bei 10 000 Spielen)
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair pair : pairs) {
                if (pair.getId().equals(pid) && pair.getClassName().equals(c.getName()))
                    return (c.isTeam() ? Entrant.ofTeam(pair) : Entrant.of(pair)).name();
            }
        }
        Player p = playerById(entrantId);
        return p == null ? "(entfernt)" : Entrant.of(p).name();
    }

    /** Personen einer Partei als {Name „Nachname, Vorname“, Verein}: Einzel eine Zeile, Doppel je Person eine. */
    public List<String[]> partyLines(Match m, String entrantId) {
        List<String[]> out = new java.util.ArrayList<>();
        if (entrantId == null) return out;
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair pair : pairs) {
                if (!pair.getId().equals(pid)) continue;
                for (Player p : pair.members()) {
                    if (p != null) out.add(new String[]{p.getDisplayName(), p.getClubName()});
                }
            }
            if (!out.isEmpty()) return out;
        }
        Entrant e = entrant(m, entrantId);
        if (e != null) out.add(new String[]{e.name(), e.detail()});
        return out;
    }

    /**
     * Passt die Partei zur Suche (Vorname, Nachname, Verein – siehe {@link de.hts.player.PlayerFilter#matchesPerson})?
     * Doppel: wenn eine der beiden Personen passt.
     */
    public boolean entrantMatches(String entrantId, String query) {
        if (query == null || query.isBlank()) return true;
        if (entrantId == null) return false;
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair pair : pairs) {
                if (!pair.getId().equals(pid)) continue;
                return de.hts.player.PlayerFilter.matchesPerson(pair.getFirst(), query)
                        || de.hts.player.PlayerFilter.matchesPerson(pair.getSecond(), query)
                        || de.hts.player.PlayerFilter.matchesPerson(pair.getThird(), query);
            }
            return false;
        }
        for (Player p : players) if (entrantId.equals(p.getId())) return de.hts.player.PlayerFilter.matchesPerson(p, query);
        return false;
    }

    /**
     * Platzhalter im Spiel um Platz 3: „Verlierer/in Spiel n“ mit der Nummer des Halbfinals (side 0 = erstes,
     * 1 = zweites Halbfinale).
     */
    public String thirdPlaceLabel(PlayingClass c, int side) {
        List<Entrant> slots = drawSlots(c);
        if (slots.size() >= 4) {
            KnockoutBracket b = new KnockoutBracket(slots);
            int semi = b.getRounds().size() - 2;
            for (Match x : matchesOf(c)) {
                if (x.getPlacement() == 0 && x.getKoRound() == semi && x.getKoIndex() == side) return "Verlierer/in Spiel " + x.getNumber();
            }
            if (semi >= 0) return "Verlierer/in Spiel " + b.getRounds().get(semi).matches().get(side).number();
        }
        return "Verlierer/in Halbfinale " + (side + 1);
    }

    /** Spiel passt zur Suche, wenn eine der beiden Parteien passt. */
    public boolean matchMatches(Match m, String query) {
        return query == null || query.isBlank() || entrantMatches(m.getA(), query) || entrantMatches(m.getB(), query);
    }

    /** Personen einer Partei per ID (ohne Spiel): Doppel je Person {Name, Verein}, sonst {@code null}. */
    public List<String[]> personLines(String entrantId) {
        if (entrantId == null || !entrantId.startsWith("pair:")) return null;
        String pid = entrantId.substring(5);
        for (DoublesPair pair : pairs) {
            if (!pair.getId().equals(pid)) continue;
            List<String[]> out = new java.util.ArrayList<>();
            for (Player p : pair.members()) {
                if (p != null) out.add(new String[]{p.getDisplayName(), p.getClubName()});
            }
            return out.isEmpty() ? null : out;
        }
        return null;
    }

    /** Partei ausgeschrieben, Doppel „Nachname, Vorname / Nachname, Vorname“. */
    public String partyFullName(Match m, String entrantId) {
        List<String[]> l = partyLines(m, entrantId);
        if (l.isEmpty()) return entrantName(m, entrantId);
        return l.stream().map(x -> x[0]).collect(java.util.stream.Collectors.joining(" / "));
    }

    public Entrant entrant(Match m, String entrantId) {
        if (entrantId == null) return null;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        return c == null ? null : entrantIndex(c).get(entrantId);
    }

    private static void requireNotStarted(PlayingClass c) {
        if (c.isStarted()) {
            throw new IllegalStateException("„" + c.getName() + "“ läuft bereits – die Auslosung kann nicht mehr geändert werden.");
        }
    }

    /** Nur zum Laden. */
    public void restoreMatches(java.util.Collection<Match> loaded) {
        matches.setAll(loaded);
        queueCounter = loaded.stream().mapToLong(Match::getQueueOrder).max().orElse(0);
    }

    /** {@code true}, wenn sich die Teilnehmenden der Klasse seit der Auslosung geändert haben. */
    public boolean isDrawOutdated(PlayingClass c) {
        if (!c.isDrawn() || c.isPhase()) return false;
        java.util.Set<String> drawn = new java.util.HashSet<>();
        for (String id : c.getDraw()) if (id != null) drawn.add(id);
        for (List<String> g : c.getGroups()) drawn.addAll(g);
        drawn.addAll(c.getExempt());
        java.util.Set<String> now = new java.util.HashSet<>();
        for (Entrant e : entrantsOf(c)) now.add(e.id());
        return !drawn.equals(now);
    }

    // --- 0.33: Zurück zur vorherigen Phase, solange kein Ergebnis vorliegt -------------------------

    /** Liegt in der Phase schon ein eingetragenes Ergebnis vor? Freilose zählen nicht. */
    public boolean hasPhaseResult(PlayingClass phase) {
        return matchesOf(phase).stream().anyMatch(m -> m.getStatus() == MatchStatus.DONE && m.getA() != null && m.getB() != null);
    }

    /**
     * Grund, warum „Auslosung verwerfen“ (Phase 1) bzw. „Zurück zu Phase 1“ (Phase 2) nicht geht – sonst {@code null}.
     * Möglich nur in der letzten Phase der Klasse und nur, solange darin noch kein Ergebnis eingetragen ist.
     */
    public String stepBackProblem(PlayingClass phase) {
        if (phase == null) return "Keine Phase gewählt.";
        if (phase.getFollowUpClass() != null) return "Es gibt schon eine weitere Phase.";
        if (!phase.isPhase() && !phase.isDrawn()) return "Die Klasse ist noch nicht ausgelost.";
        if (hasPhaseResult(phase)) return "Es liegt bereits ein Ergebnis vor.";
        return null;
    }

    /** Spiele der Phase, die gerade an einem Tisch laufen bzw. aufgerufen sind. */
    public long matchesAtTables(PlayingClass phase) {
        return matchesOf(phase).stream().filter(m -> m.getStatus().atTable()).count();
    }

    /**
     * Phase 1: Auslosung verwerfen – zurück zu „Teilnehmende der Klasse“ (auch nach dem Start); Phase 2: löschen und
     * zurück zu Phase 1 (wieder offen). Spiele an Tischen werden zurückgezogen, die Tische sind sofort frei.
     */
    public void stepBack(PlayingClass phase) {
        String problem = stepBackProblem(phase);
        if (problem != null) throw new IllegalStateException(problem);
        PlayingClass root = rootOf(phase);
        if (phase.isPhase()) {
            PlayingClass source = findClass(phase.getSourceClass()).orElseThrow();
            removeClass(phase);
            source.setFollowUpClass(null);
            source.setNextPhase(TournamentMode.NONE);
        } else {
            matches.removeIf(m -> m.getClassName().equals(phase.getName()));
            phase.setDraw(List.of(), false);
            phase.setSwissByes(List.of());
            phase.setSeedLabels(java.util.Map.of());
            phase.setNextPhase(TournamentMode.NONE);
            phase.setStartedAt(null);
            phase.setReady(false);
            phase.setPaused(false);
        }
        root.setFinishedAt(null);
        dispatch();
        markModified();
    }

    // --- 0.35: Anwesenheit am Turniertag, Anmeldung per Adresse -----------------------------------

    private boolean checkinLocked;
    private String checkinToken;

    /** Anmeldung per Adresse (Handy) von der Turnierleitung gesperrt? */
    public boolean isCheckinLocked() {
        return checkinLocked;
    }

    public void setCheckinLocked(boolean b) {
        if (checkinLocked != b) {
            checkinLocked = b;
            markModified();
        }
    }

    /** Anmeldung per Adresse möglich: nicht gesperrt und noch nicht alle Klassen beendet. */
    public boolean isCheckinOpen() {
        return !checkinLocked && !isCompletelyFinished();
    }

    /** Zufälliger Schlüssel in der Adresse der Anmeldeseite (steht im ausgedruckten QR-Code). */
    public String getCheckinToken() {
        if (checkinToken == null || checkinToken.isBlank()) {
            String abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            java.security.SecureRandom r = new java.security.SecureRandom();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(abc.charAt(r.nextInt(abc.length())));
            checkinToken = sb.toString();
        }
        return checkinToken;
    }

    // --- 1.1 (#93): Ergebnisse per Handy (mit dem Handy-Zugang vorerst ausgeblendet) ---------------------------

    private boolean phoneResults;
    private boolean phoneResultsDirect;

    /** Ein per Handy gemeldetes Ergebnis, das die Turnierleitung noch bestätigen muss. */
    public record PhoneResult(Match match, int table, List<int[]> sets, LocalDateTime at) {
        public String setsText() {
            return String.join(", ", sets.stream().map(x -> x[0] + ":" + x[1]).toList());
        }
    }

    private final List<PhoneResult> phoneInbox = new java.util.ArrayList<>();

    /** Option „Ergebnisse per Handy eintragen“ (Mobile Einstellungen, Standard aus). */
    public boolean isPhoneResults() { return phoneResults; }

    public void setPhoneResults(boolean b) {
        if (phoneResults != b) {
            phoneResults = b;
            markModified();
        }
    }

    /** Per Handy gemeldete Ergebnisse ohne Bestätigung übernehmen. */
    public boolean isPhoneResultsDirect() { return phoneResultsDirect; }

    public void setPhoneResultsDirect(boolean b) {
        if (phoneResultsDirect != b) {
            phoneResultsDirect = b;
            markModified();
        }
    }

    /** Schlüssel in der Adresse der Ergebnisseite eines Tisches (aus dem Turnier-Schlüssel abgeleitet, je Tisch anders). */
    public String tableResultToken(int table) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((getCheckinToken() + ":ergebnis:" + getId() + ":" + table).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String abc = "abcdefghjkmnpqrstuvwxyz23456789";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) sb.append(abc.charAt((h[i] & 0xff) % abc.length()));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Ergebnis per Handy für das Spiel an einem Tisch melden. Geprüft wird wie bei der Eingabe in HTS; mit „direkt
     * übernehmen“ wird es sofort eingetragen, sonst wartet es auf die Bestätigung.
     * @return {@code true}, wenn direkt übernommen
     * @throws IllegalArgumentException ungültiges Ergebnis; IllegalStateException kein passendes Spiel am Tisch
     */
    public boolean submitPhoneResult(int table, int matchNumber, List<int[]> sets) {
        if (!phoneResults) throw new IllegalStateException("Ergebnisse per Handy sind ausgeschaltet.");
        Match m = activeMatchAt(table).orElse(null);
        if (m == null || m.getNumber() != matchNumber) throw new IllegalStateException("An diesem Tisch läuft dieses Spiel nicht mehr.");
        if (m.getStatus() != MatchStatus.ACTIVE) throw new IllegalStateException("Das Spiel ist angehalten.");
        evaluateScore(m, sets, 0);                       // wirft bei ungültigem Ergebnis
        if (phoneResultsDirect) {
            recordResult(m, sets, 0);
            return true;
        }
        phoneInbox.removeIf(r -> r.match() == m);
        phoneInbox.add(new PhoneResult(m, table, List.copyOf(sets), LocalDateTime.now().withNano(0)));
        return false;
    }

    /** Wartende Handy-Ergebnisse (ältestes zuerst); Spiele, die inzwischen anders erledigt sind, fallen heraus. */
    public List<PhoneResult> pendingPhoneResults() {
        phoneInbox.removeIf(r -> r.match().getStatus() != MatchStatus.ACTIVE || !matches.contains(r.match()));
        return List.copyOf(phoneInbox);
    }

    /** Handy-Ergebnis übernehmen. */
    public void acceptPhoneResult(PhoneResult r) {
        phoneInbox.remove(r);
        recordResult(r.match(), r.sets(), 0);
    }

    /** Handy-Ergebnis verwerfen. */
    public void rejectPhoneResult(PhoneResult r) {
        phoneInbox.remove(r);
    }

    /** Nur für das Laden. */
    public void restoreCheckin(boolean locked, String token) {
        checkinLocked = locked;
        checkinToken = token;
    }

    /** Anwesenheit setzen (Turnierleitung oder Anmeldeseite). */
    public void setAttendance(Player p, Attendance a) {
        if (p.getAttendance() != a) {
            p.setAttendance(a);
            markModified();
        }
    }

    /**
     * Anmeldung per Handy: Teilnehmende/Betreuende melden „anwesend“ oder „nicht anwesend“. „Nicht anwesend“ muss die
     * Turnierleitung bestätigen; endgültig Abgemeldete ändert nur die Turnierleitung.
     *
     * @return {@code true}, wenn sich etwas geändert hat
     */
    public boolean checkinFromPhone(Player p, boolean present) {
        if (!isCheckinOpen()) throw new IllegalStateException("Die Anmeldung ist geschlossen.");
        if (p.getAttendance() == Attendance.WITHDRAWN || p.isReserveOnly()) return false;     // Nachrücker: keine An-/Abmeldung
        Attendance next = present ? Attendance.PRESENT : Attendance.WITHDRAW_REQUESTED;
        if (p.getAttendance() == next) return false;
        setAttendance(p, next);
        return true;
    }

    /** Gemeldete einer Klasse (laut Meldeliste bzw. in der Klasse/in Paarungen) – Nachrücker zählen nicht. */
    public List<Player> enteredPlayers(PlayingClass c) {
        PlayingClass root = rootOf(c);
        java.util.Set<Player> out = new java.util.LinkedHashSet<>();
        for (Player p : players) if (p.isEnteredFor(root.getName())) out.add(p);
        if (root.isDoubles()) for (DoublesPair pair : pairsIn(root)) for (Player m : pair.members()) if (m != null) out.add(m);
        return new java.util.ArrayList<>(out);
    }

    /**
     * 0.39 (#62): Setzplätze einer per XML importierten Klasse – Rangfolge nach TTR der Gemeldeten (ohne endgültig
     * Abgemeldete und reine Nachrücker); gleicher TTR = gleicher Platz (3, 3, 5 …), ohne TTR gemeinsam ganz unten; bei
     * Doppeln zählt der Ø-TTR der Paarung. Leer, wenn die Klasse nicht per XML importiert wurde.
     */
    public java.util.Map<Player, Integer> seedPositions(PlayingClass c) {
        PlayingClass root = rootOf(c);
        if (!root.isImportedFromXml()) return java.util.Map.of();
        java.util.Map<Player, Integer> ttrOf = new java.util.HashMap<>();
        for (Player p : enteredPlayers(root)) {
            if (p.getAttendance() == Attendance.WITHDRAWN) continue;
            Integer t = p.getTtr();
            if (root.isDoubles()) {
                DoublesPair pair = pairOf(p, root).orElse(null);
                if (pair != null && pair.getAverageTtr() != null) t = pair.getAverageTtr();
            }
            ttrOf.put(p, t);
        }
        List<Player> order = new java.util.ArrayList<>(ttrOf.keySet());
        order.sort(java.util.Comparator.comparing((Player p) -> ttrOf.get(p), java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        java.util.Map<Player, Integer> out = new java.util.LinkedHashMap<>();
        Integer prev = null;
        int rank = 0;
        for (int i = 0; i < order.size(); i++) {
            Integer t = ttrOf.get(order.get(i));
            if (i == 0 || !java.util.Objects.equals(t, prev)) rank = i + 1;
            out.put(order.get(i), rank);
            prev = t;
        }
        return out;
    }

    /** Klassen „aus XML importiert“: Gemeldete, die noch nicht angemeldet oder endgültig abgemeldet sind. */
    public List<Player> openAttendance(PlayingClass c) {
        PlayingClass root = rootOf(c);
        if (!root.isImportedFromXml()) return List.of();
        return enteredPlayers(root).stream().filter(p -> !p.getAttendance().isSettled()).toList();
    }

    /** Grund, warum die Klasse wegen offener An-/Abmeldungen nicht starten kann – sonst {@code null}. */
    public String attendanceProblem(PlayingClass c) {
        List<Player> open = openAttendance(c);
        if (open.isEmpty()) return null;
        List<String> names = open.stream().limit(8).map(Player::getDisplayName).toList();
        return "„" + rootOf(c).getName() + "“ kann noch nicht starten: " + open.size()
                + (open.size() == 1 ? " gemeldete Person ist" : " gemeldete Personen sind") + " noch nicht angemeldet oder "
                + "endgültig abgemeldet (" + String.join(", ", names) + (open.size() > names.size() ? ", …" : "") + ").";
    }

    /**
     * „Alle anwesenden Gemeldeten übernehmen“: anwesende Personen mit Meldung für die Klasse kommen in die Klasse; im
     * Doppel mit dem gemeldeten Partner, wenn auch er anwesend und noch nicht gepaart ist.
     *
     * @return Anzahl der übernommenen Personen
     */
    public int takeOverPresentEntries(PlayingClass c) {
        requireNotStarted(c);
        Map<String, Player> byKey = new java.util.HashMap<>();
        for (Player p : players) byKey.put(p.identityKey(), p);
        int n = 0;
        for (Player p : new java.util.ArrayList<>(players)) {
            if (!p.isPresent() || !p.getRegistrations().contains(c.getName()) || paymentPending(p)) continue;   // 1.1 (#89): nur Bezahlte
            if (c.isDoubles()) {
                if (pairOf(p, c).map(DoublesPair::isComplete).orElse(false)) continue;
                Player partner = p.getRegisteredPartner(c.getName()) == null ? null : byKey.get(p.getRegisteredPartner(c.getName()));
                boolean partnerFree = partner != null && partner != p && partner.isPresent() && !paymentPending(partner)
                        && (partner.isEnteredFor(c.getName())) && !pairOf(partner, c).map(DoublesPair::isComplete).orElse(false);
                if (partnerFree) {
                    createPair(c, p, partner);
                    n += 2;
                    continue;
                }
            }
            if (assign(p, c)) n++;
        }
        markModified();
        return n;
    }

    /**
     * 0.35 click-TT-Import: Meldungen kommen nicht mehr direkt in die Klasse, sondern als Meldung unter „Verfügbar“;
     * die Doppelpartner laut Meldeliste werden gemerkt (für die Übernahme).
     */
    public MergeResult mergeImportedEntries(List<Player> imported, List<String[]> pairs) {
        for (Player p : imported) p.entriesToRegistrations();
        adoptManualPersons(imported);
        Map<String, Player> importedByKey = new java.util.HashMap<>();
        for (Player p : imported) importedByKey.put(p.identityKey(), p);
        for (String[] pr : pairs) {          // {Klasse, Schlüssel 1, Schlüssel 2}
            Player a = importedByKey.get(pr[1]), b = importedByKey.get(pr[2]);
            if (a != null && b != null && a != b) {
                a.setRegisteredPartner(pr[0], b.identityKey());
                b.setRegisteredPartner(pr[0], a.identityKey());
            }
        }
        return merge(imported);
    }

    /**
     * 1.0 (#78): Von Hand erfasste Personen (ohne click-TT-Nummer) werden beim Import nicht doppelt angelegt: Gibt es zu
     * einer Person der Datei, die HTS noch nicht kennt, GENAU EINE von Hand erfasste Person mit gleichem Vor- und
     * Nachnamen (Verein und Jahrgang gleich, soweit bei beiden angegeben), übernimmt diese die click-TT-Nummern – der
     * anschließende Abgleich führt beide dann zusammen. Mehrdeutige Fälle bleiben getrennt (Zuordnung von Hand, #79).
     * @return die so zugeordneten Personen (von Hand erfasst → Person der Datei)
     */
    public Map<Player, Player> adoptManualPersons(Collection<Player> imported) {
        java.util.Set<String> known = new java.util.HashSet<>();
        for (Player p : players) known.add(p.identityKey());
        Map<Player, Player> adopted = new java.util.LinkedHashMap<>();
        for (Player in : imported) {
            if (known.contains(in.identityKey()) || !in.hasClickTtNumber()) continue;
            List<Player> cand = players.stream().filter(m -> !m.hasClickTtNumber() && !adopted.containsKey(m)
                    && samePerson(m, in)).toList();
            List<Player> rivals = imported.stream().filter(o -> o != in && !known.contains(o.identityKey())
                    && cand.stream().anyMatch(m -> samePerson(m, o))).toList();
            if (cand.size() != 1 || !rivals.isEmpty()) continue;
            Player m = cand.get(0);
            adoptNumbers(m, in);
            adopted.put(m, in);
            known.add(m.identityKey());
        }
        if (!adopted.isEmpty()) markModified();
        return adopted;
    }

    /**
     * 1.0 (#79): Ergebnis des Spielerabgleichs vor dem click-TT-Export.
     * @param auto       eindeutig zugeordnet (von Hand erfasste Person → Person der Datei)
     * @param open       von Hand erfasste Personen ohne eindeutige Zuordnung
     * @param newPersons alle neuen Personen der Datei (die keinem bekannten Teilnehmenden entsprechen)
     */
    public record Matching(Map<Player, Player> auto, List<Player> open, List<Player> newPersons) {
    }

    /**
     * 1.0 (#79): Abgleich nachgetragener Personen mit einer aktualisierten click-TT-Teilnehmerliste. Neu = Personen der
     * Datei, deren click-TT-Nummer (interne Nummer bzw. Lizenz-Nr.) HTS noch nicht kennt. Automatisch zugeordnet wird nur
     * eindeutig über Vor-/Nachname und Verein (Jahrgang, soweit bei beiden angegeben, muss passen).
     */
    public Matching matchManual(List<Player> manual, List<Player> fromFile) {
        java.util.Set<String> numbers = new java.util.HashSet<>();
        for (Player p : players) {
            if (p.getInternalNr() != null && !p.getInternalNr().isBlank()) numbers.add("i:" + p.getInternalNr().trim());
            if (p.getLicenceNr() != null && !p.getLicenceNr().isBlank()) numbers.add("l:" + p.getLicenceNr().trim());
        }
        List<Player> fresh = fromFile.stream().filter(f -> !(f.getInternalNr() != null && numbers.contains("i:" + f.getInternalNr().trim()))
                && !(f.getLicenceNr() != null && numbers.contains("l:" + f.getLicenceNr().trim()))).toList();
        Map<Player, Player> auto = new java.util.LinkedHashMap<>();
        List<Player> open = new java.util.ArrayList<>();
        for (Player m : manual) {
            List<Player> cand = fresh.stream().filter(f -> samePerson(m, f)).toList();
            boolean unique = cand.size() == 1 && manual.stream().filter(o -> samePerson(o, cand.get(0))).count() == 1;
            if (unique) auto.put(m, cand.get(0));
            else open.add(m);
        }
        return new Matching(auto, open, fresh);
    }

    /** click-TT-Nummern (und fehlende Stammdaten) einer Person der Datei auf eine von Hand erfasste übertragen. */
    public void adoptNumbers(Player manual, Player fromFile) {
        if (fromFile.getInternalNr() != null && !fromFile.getInternalNr().isBlank()) manual.setInternalNr(fromFile.getInternalNr());
        if (fromFile.getLicenceNr() != null && !fromFile.getLicenceNr().isBlank()) manual.setLicenceNr(fromFile.getLicenceNr());
        markModified();
    }

    /** Gleicher Vor- und Nachname; Verein bzw. Jahrgang gleich, soweit bei beiden angegeben (Groß-/Kleinschreibung egal). */
    public static boolean samePerson(Player a, Player b) {
        if (!eqText(a.getFirstname(), b.getFirstname()) || !eqText(a.getLastname(), b.getLastname())) return false;
        if (notBlankText(a.getClubName()) && notBlankText(b.getClubName()) && !eqText(a.getClubName(), b.getClubName())) return false;
        return a.getBirthyear() == null || b.getBirthyear() == null || a.getBirthyear().equals(b.getBirthyear());
    }

    private static boolean eqText(String a, String b) {
        return (a == null ? "" : a.trim()).equalsIgnoreCase(b == null ? "" : b.trim());
    }

    private static boolean notBlankText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Ältere Turnierdateien (vor 0.35): In noch nicht ausgelosten Klassen kommen die Personen aus „In der Klasse“
     * zurück zu den Meldungen („In der Klasse“ = 0); Doppelpaarungen werden als gemeldete Partner gemerkt.
     */
    public void migrateToAttendanceModel() {
        for (PlayingClass c : classes) {
            if (c.isPhase() || c.isDrawn()) continue;
            String cn = c.getName();
            if (c.isDoubles()) {
                for (DoublesPair pair : new java.util.ArrayList<>(pairsIn(c))) {
                    Player a = pair.getFirst(), b = pair.getSecond();
                    if (a != null && b != null) {
                        a.setRegisteredPartner(cn, b.identityKey());
                        b.setRegisteredPartner(cn, a.identityKey());
                    }
                    pairs.remove(pair);
                }
            }
            for (Player p : players) {
                if (p.removeFromClass(cn)) p.addRegistration(cn);
            }
        }
    }

    /** Setzt Modus und Auslosung der Klasse zurück. */
    public void clearDraw(PlayingClass c) {
        requireNotStarted(c);
        c.setDraw(List.of(), false);
        c.setSwissByes(List.of());
        c.setReady(false);
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        markModified();
    }

    /** Stichtag für Altersgrenzen: das Turnierdatum, ohne Datum der heutige Tag. */
    public LocalDate referenceDate() {
        return date.get() != null ? date.get() : LocalDate.now();
    }

    /** Erfüllt der Spieler die Kriterien der Klasse (Stichtag = Turniertag)? */
    public PlayingClass.Fit fit(PlayingClass c, Player p) {
        return c.fit(p, referenceDate());
    }

    /** Kriterien der Klasse als Text, bezogen auf den Turniertag. */
    public String criteriaText(PlayingClass c) {
        return c.getCriteriaText(referenceDate());
    }

    // --- Laden / Speichern -------------------------------------------------

    /** Ersetzt den gesamten Inhalt (z. B. beim Öffnen einer Datei). Danach gilt das Turnier als ungeändert. */
    public void replaceAll(String name, LocalDate date, String location,
                           Collection<PlayingClass> newClasses, Collection<Player> newPlayers) {
        this.name.set(name);
        this.date.set(date);
        this.location.set(location);
        pairs.clear();
        matches.clear();
        classes.setAll(newClasses);
        players.setAll(newPlayers);
        ensureClassesForEntries();
        modified.set(false);
    }

    /** Aufrufen, wenn ein Spieler oder eine Klasse selbst geändert wurde. */
    public void markModified() {
        modified.set(true);
        changeCount++;
    }

    private long changeCount;

    /** Zähler, der bei jeder Änderung steigt (z. B. für die Präsentationsansicht). */
    public long getChangeCount() { return changeCount; }

    // --- Präsentationsansicht (zweiter Bildschirm) ---
    private int presentationSeconds = 20;
    private final java.util.Set<String> presentationOff = new java.util.TreeSet<>();
    private final java.util.Set<String> presentationOn = new java.util.TreeSet<>();

    public int getPresentationSeconds() { return presentationSeconds; }

    // --- Startgebühren (1.0, #82) ---
    private int lateFeeCents;

    /** Nachmeldegebühr in Cent: einmal je Person für alle, die nicht in der click-TT-Datei standen. */
    public int getLateFeeCents() { return lateFeeCents; }

    public void setLateFeeCents(int cents) {
        lateFeeCents = Math.max(0, cents);
        markModified();
    }

    /** Wird in diesem Turnier überhaupt eine Gebühr erhoben (Klassen- oder Nachmeldegebühr)? */
    public boolean hasFees() {
        return lateFeeCents > 0 || classes.stream().anyMatch(c -> !c.isPhase() && !c.isDoubles() && c.getFeeCents() != null);
    }

    /**
     * Hauptklassen mit Gebühr, für die die Person gemeldet bzw. in denen sie ist (ohne reine Wartelisten). 1.1 (#89):
     * nur Einzelklassen – Doppel (und Teams) sind ohne Gebühr.
     */
    public List<PlayingClass> feeClassesOf(Player p) {
        return classes.stream().filter(c -> !c.isPhase() && !c.isDoubles() && c.getFeeCents() != null && p.isEnteredFor(c.getName())).toList();
    }

    /** Nachmeldung = Person ohne click-TT-Nummer (von Hand erfasst). */
    public boolean isLateEntry(Player p) {
        return !p.hasClickTtNumber() && !isHandicap();
    }

    /** Gebühr der Person in Cent: Summe ihrer Klassen plus ggf. Nachmeldegebühr; endgültig Abgemeldete zahlen nichts. */
    public int feeOf(Player p) {
        if (p.getAttendance() == Attendance.WITHDRAWN || p.isReserveOnly()) return 0;
        List<PlayingClass> cs = feeClassesOf(p);
        int sum = cs.stream().mapToInt(PlayingClass::getFeeCents).sum();
        if (lateFeeCents > 0 && isLateEntry(p) && (!cs.isEmpty() || !p.getClassNames().isEmpty())) sum += lateFeeCents;
        return sum;
    }

    /** Noch offen: Gebühr fällig, aber nicht bezahlt. */
    public boolean isFeeOpen(Player p) {
        return !p.isPaid() && feeOf(p) > 0;
    }

    /**
     * Bezahlung der Personen vermerken (Betrag = ihre aktuelle Gebühr); {@code byClub}: Sammelabrechnung. 1.1 (#89):
     * Wer bezahlt hat, ist damit auch angemeldet.
     */
    public void markPaid(Collection<Player> ps, Player.Payment how, String byClub) {
        LocalDateTime now = LocalDateTime.now();
        for (Player p : ps) {
            p.setPayment(how, feeOf(p), now, byClub);
            if (how != null && how != Player.Payment.NONE && !p.isPresent()) setAttendance(p, Attendance.PRESENT);
        }
        markModified();
    }

    /** 1.1 (#89): Gebühr erlassen (gilt wie bezahlt, Person ist damit angemeldet); {@code reason} optional. */
    public void waiveFee(Collection<Player> ps, String reason) {
        markPaid(ps, Player.Payment.WAIVED, null);
        for (Player p : ps) p.setWaivedReason(reason);
        markModified();
    }

    /** 1.1 (#89): Angemeldet bzw. gemeldet, Gebühr fällig, aber weder bezahlt noch erlassen. */
    public boolean paymentPending(Player p) {
        return hasFees() && !p.isPaid() && feeOf(p) > 0;
    }

    /**
     * 1.1 (#89): Steht unter „Verfügbar“ – angemeldet und (bei Gebühren) bezahlt bzw. erlassen; nach dem Turnierstart
     * auch Angemeldete ohne Zahlung (sie werden markiert).
     */
    public boolean availableForClasses(Player p) {
        return p.isPresent() && (!paymentPending(p) || isRunning());
    }

    /** 1.1 (#89): Angemeldete mit offener Gebühr (für den Hinweis nach dem Turnierstart). */
    public List<Player> presentUnpaid() {
        return players.stream().filter(p -> p.isPresent() && paymentPending(p)).toList();
    }

    public void undoPayment(Collection<Player> ps) {
        for (Player p : ps) p.setPayment(Player.Payment.NONE, 0, null, null);
        markModified();
    }

    /** Teilnehmende einer Hauptklasse (in der Klasse), deren Gebühr noch offen ist – für die Warnung beim Start. */
    public List<Player> unpaidIn(PlayingClass root) {
        return playersIn(root).stream().filter(this::isFeeOpen).toList();
    }

    /** „8,00 €“ */
    public static String euro(int cents) {
        return String.format(Locale.GERMAN, "%d,%02d €", cents / 100, cents % 100);
    }

    // --- 1.5.0 (#135/#138): Veröffentlichen auf dem Webserver ------------------------------------------

    private boolean publish;
    private boolean publishAbbrev = true;                  // 1.8.0 (#156): immer abgekürzt

    /** Wird dieses Turnier auf dem Webserver veröffentlicht? (Adresse und Schlüssel: Programmeinstellung) */
    public boolean isPublish() { return publish; }

    public void setPublish(boolean b) {
        if (b == publish) return;
        publish = b;
        markModified();
    }

    /** Datenschutz: Vornamen beim Veröffentlichen abkürzen („M. Mustermann“). */
    public boolean isPublishAbbrev() { return publishAbbrev; }

    public void setPublishAbbrev(boolean b) {
        if (b == publishAbbrev) return;
        publishAbbrev = b;
        markModified();
    }

    // --- 1.8.0 ------------------------------------------------------------------------------------------

    private boolean publishedOnce;
    private String worldUrl = "";
    private boolean flatLayout;

    /** 1.8.0 (#154): Wurde der Stand schon einmal erfolgreich hochgeladen? (Rückfrage beim Löschen/Archivieren) */
    public boolean isPublishedOnce() { return publishedOnce; }

    public void setPublishedOnce(boolean b) {
        if (b == publishedOnce) return;
        publishedOnce = b;
        markModified();
    }

    /** 1.8.0 (#151): Link zur myTischtennis-Turnierwelt ({@code ""} = keiner). */
    public String getWorldUrl() { return worldUrl; }

    public void setWorldUrl(String url) {
        String n = url == null ? "" : url.strip();
        if (n.equals(worldUrl)) return;
        worldUrl = n;
        markModified();
    }

    /** Nur gültige https-Adressen (für die Anzeige als Link); sonst {@code null}. */
    public static String validWorldUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI u = new java.net.URI(url.strip());
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null || u.getHost().isBlank()) return null;
            return u.toASCIIString();
        } catch (java.net.URISyntaxException e) {
            return null;
        }
    }

    /**
     * 1.8.0 (#152): neue Ablage – Turnierdatei direkt im HTS-Ordner (Standard „Dokumente\HTS“), Exporte gemeinsam in
     * „Export files“ (bzw. den im Reiter „Programm“ gewählten Ordnern); ältere Turniere behalten ihren Turnierordner.
     */
    public boolean isFlatLayout() { return flatLayout; }

    public void setFlatLayout(boolean b) {
        flatLayout = b;
    }

    /** 1.4.0 (#108): Aussehen der Spielzettel (A6), Standard: Spielerzettel. */
    private SlipDesign slipDesign = SlipDesign.PLAYER;

    public SlipDesign getSlipDesign() { return slipDesign; }

    public void setSlipDesign(SlipDesign d) {
        SlipDesign n = d == null ? SlipDesign.PLAYER : d;
        if (n == slipDesign) return;
        slipDesign = n;
        markModified();
    }

    // --- Urkunden (1.4.0, #113) --------------------------------------------------------------

    public static final String CERT_TITLE_DEFAULT = "Ehrenurkunde";
    public static final String CERT_PLACE_DEFAULT = "Hamburg";
    public static final String CERT_SIGNER_DEFAULT = "Jugendausschuss";
    public static final String CERT_HOST_DEFAULT = "Hamburger Tisch-Tennis-Verband e.V.";

    private String certTitle = CERT_TITLE_DEFAULT, certPlace = CERT_PLACE_DEFAULT, certSigner = CERT_SIGNER_DEFAULT,
            certHost = CERT_HOST_DEFAULT;
    private boolean certLeaveTop = true;
    private boolean certTabShown;

    /** Druckanpassungen (nur für die Urkunde): Schlüssel {@code Klasse + '\u0001' + Partei-ID} → {Name, Verein}. */
    private final Map<String, String[]> certOverrides = new java.util.LinkedHashMap<>();

    public String getCertTitle() { return certTitle; }
    public String getCertPlace() { return certPlace; }
    public String getCertSigner() { return certSigner; }
    public String getCertHost() { return certHost; }
    public boolean isCertLeaveTop() { return certLeaveTop; }

    /** Ein leerer Text bleibt leer (die Stelle auf der Urkunde bleibt frei); {@code null} = Vorgabe. */
    public void setCertTexts(String title, String place, String signer, String host) {
        String t = title == null ? CERT_TITLE_DEFAULT : title.strip(), p = place == null ? CERT_PLACE_DEFAULT : place.strip(),
                sg = signer == null ? CERT_SIGNER_DEFAULT : signer.strip(), h = host == null ? CERT_HOST_DEFAULT : host.strip();
        if (t.equals(certTitle) && p.equals(certPlace) && sg.equals(certSigner) && h.equals(certHost)) return;
        certTitle = t;
        certPlace = p;
        certSigner = sg;
        certHost = h;
        markModified();
    }

    public void setCertLeaveTop(boolean b) {
        if (b == certLeaveTop) return;
        certLeaveTop = b;
        markModified();
    }

    /** 1.5.0 (#132): bereits gedruckte bzw. als PDF gespeicherte Urkunden (Schlüssel wie {@link CertEntry#key()}). */
    private final java.util.Set<String> certPrinted = new java.util.LinkedHashSet<>();

    public java.util.Set<String> getCertPrinted() { return java.util.Collections.unmodifiableSet(certPrinted); }

    public boolean isCertPrinted(String key) { return certPrinted.contains(key); }

    public void markCertPrinted(java.util.Collection<String> keys) {
        if (certPrinted.addAll(keys)) markModified();
    }

    public void restoreCertPrinted(String key) {
        certPrinted.add(key);
    }

    /** 1.4.1: Klassen, für die HTS nach „Klasse beenden“ schon nach dem Urkundendruck gefragt hat. */
    private final java.util.Set<String> certAsked = new java.util.LinkedHashSet<>();

    public java.util.Set<String> getCertAsked() { return java.util.Collections.unmodifiableSet(certAsked); }

    public boolean isCertAsked(String className) { return certAsked.contains(className); }

    /** Merkt die Frage für eine Klasse (gespeichert in der Turnierdatei). */
    public void markCertAsked(String className) {
        if (certAsked.add(className)) markModified();
    }

    /** Laden bzw. ältere Dateien: ohne „geändert“-Markierung. */
    public void restoreCertAsked(String className) {
        certAsked.add(className);
    }

    /**
     * 1.4.1: Parteien einer laufenden Klasse, deren Platz schon feststeht: Sie spielen in keiner späteren Phase mehr und
     * ihre Phase ist komplett gespielt (eine Folgephase ist angelegt). Solange ein Korrektur-Reiter offen ist, keine.
     */
    public java.util.Set<String> fixedEntrants(PlayingClass root) {
        java.util.Set<String> out = new java.util.HashSet<>();
        List<PlayingClass> phases = phasesOf(root);
        if (phases.size() < 2) return out;
        for (PlayingClass ph : phases) if (correctionOf(ph) != null) return out;
        java.util.Map<String, Integer> lastPhase = new java.util.HashMap<>();
        for (int i = 0; i < phases.size(); i++) {
            for (Match m : matchesOf(phases.get(i))) {
                if (m.getA() != null) lastPhase.put(m.getA(), i);
                if (m.getB() != null) lastPhase.put(m.getB(), i);
            }
        }
        for (java.util.Map.Entry<String, Integer> en : lastPhase.entrySet()) {
            int i = en.getValue();
            if (i < phases.size() - 1 && isPhaseComplete(phases.get(i))) out.add(en.getKey());
        }
        return out;
    }

    /** 1.4.1: Klassen, für die es schon Urkunden gibt (beendet oder mit festen Plätzen nach einer Phase). */
    public List<PlayingClass> certificateClasses() {
        return rootClasses().stream().filter(c -> c.isDrawn() && (c.isFinished() || !fixedEntrants(c).isEmpty())).toList();
    }

    /**
     * 1.4.1: Schlüssel für die Frage „Urkundendruck starten?“: beendete Klasse = ihr Name, sonst Name + abgeschlossene
     * Phase (z. B. „Herren A · Phase 1“); {@code null}, solange noch keine Phase abgeschlossen ist.
     */
    public String certAskKey(PlayingClass root) {
        if (!root.isDrawn()) return null;
        if (root.isFinished()) return root.getName();
        List<PlayingClass> phases = phasesOf(root);
        if (phases.size() < 2) return null;
        int closed = phases.size() - 1;
        return isPhaseComplete(phases.get(closed - 1)) ? root.getName() + " · Phase " + closed : null;
    }

    /** Beendete Klassen (Endergebnis steht). */
    public List<PlayingClass> finishedRootClasses() {
        return rootClasses().stream().filter(c -> c.isDrawn() && c.isFinished()).toList();
    }

    /** Wurde der Reiter „Urkunden drucken“ schon freigeschaltet? Bleibt dauerhaft. */
    public boolean isCertTabShown() { return certTabShown; }

    public void setCertTabShown(boolean b) {
        if (b == certTabShown) return;
        certTabShown = b;
        markModified();
    }

    /** Alle Druckanpassungen (schreibgeschützt) – für die Turnierdatei. */
    public Map<String, String[]> getCertOverrides() { return java.util.Collections.unmodifiableMap(certOverrides); }

    /** Lädt eine Druckanpassung aus der Turnierdatei (ohne „geändert“-Markierung). */
    public void restoreCertOverride(String className, String entrantId, String name, String club) {
        certOverrides.put(className + '\u0001' + entrantId, new String[]{name, club});
    }

    /** Eine Urkunde: Klasse, Platz, Name und Verein (ggf. für den Druck geändert), dazu die Originalwerte. */
    public record CertEntry(String className, String entrantId, int place, String name, String club, String originalName,
                            String originalClub) {
        public boolean changed() {
            return !java.util.Objects.equals(name, originalName) || !java.util.Objects.equals(club, originalClub);
        }

        public String key() {
            return className + '\u0001' + entrantId;
        }
    }

    /** 1.4.1: eine Urkunde mit dem aktuellen Stand ihrer Druckanpassung – ohne das Endergebnis neu zu berechnen. */
    public CertEntry currentCertEntry(CertEntry e) {
        String[] ov = certOverrides.get(e.key());
        return new CertEntry(e.className(), e.entrantId(), e.place(), ov == null ? e.originalName() : ov[0],
                ov == null ? e.originalClub() : ov[1], e.originalName(), e.originalClub());
    }

    /** Name und Verein nur für den Druck ändern; stimmen beide mit dem Original überein, entfällt die Anpassung. */
    public void setCertOverride(CertEntry e, String name, String club) {
        String n = name == null ? "" : name.strip(), c = club == null ? "" : club.strip();
        String key = e.key();
        String[] old = certOverrides.get(key);
        if (n.equals(e.originalName()) && c.equals(e.originalClub() == null ? "" : e.originalClub())) {
            if (certOverrides.remove(key) != null) markModified();
            return;
        }
        if (old != null && old[0].equals(n) && old[1].equals(c)) return;
        certOverrides.put(key, new String[]{n, c});
        markModified();
    }

    /**
     * Urkunden für alle beendeten Klassen: je Platzierung eine (Spanne 5.–8. = Platz 5), sortiert nach
     * Klasse (Reihenfolge der Klassen) und Platz. Einzel „Vorname Nachname“, Doppel beide Personen, Mannschaft der Teamname.
     */
    public List<CertEntry> certificateEntries() {
        List<CertEntry> out = new java.util.ArrayList<>();
        for (PlayingClass root : rootClasses()) {
            if (!root.isDrawn()) continue;
            boolean finished = root.isFinished();
            java.util.Set<String> fixed = finished ? null : fixedEntrants(root);   // 1.4.1: laufende Klasse – nur feste Plätze
            if (!finished && fixed.isEmpty()) continue;
            for (Placing pl : finalResult(root).rows()) {
                Entrant e = pl.entrant();
                if (e == null) continue;
                if (fixed != null && !fixed.contains(e.id())) continue;
                String name = certName(e), club = e.detail() == null ? "" : e.detail();
                String[] ov = certOverrides.get(root.getName() + '\u0001' + e.id());
                out.add(new CertEntry(root.getName(), e.id(), pl.from(), ov == null ? name : ov[0], ov == null ? club : ov[1], name, club));
            }
        }
        return out;
    }

    private String certName(Entrant e) {
        if (e.id() != null && e.id().startsWith("pair:")) {
            String pid = e.id().substring(5);
            for (DoublesPair pair : pairs) {
                if (!pair.getId().equals(pid)) continue;
                if (pair.getCapacity() >= 3 || pair.getThird() != null) return e.name();
                List<String> names = new java.util.ArrayList<>();
                for (Player p : pair.members()) if (p != null) names.add(fullName(p));
                return names.isEmpty() ? e.name() : String.join(" / ", names);
            }
            return e.name();
        }
        Player p = playerById(e.id());                    // 1.4.1: Index statt linearer Suche (6000 Personen)
        return p != null ? fullName(p) : e.name();
    }

    private static String fullName(Player p) {
        String f = p.getFirstname() == null ? "" : p.getFirstname().strip(), l = p.getLastname() == null ? "" : p.getLastname().strip();
        return (f + " " + l).strip();
    }

    /** 1.0 (#81): Anzahl der Spiele in „Die nächsten Spiele (alle Klassen)“, 4–48, Standard 16. */
    private int presentationNextCount = 16;

    public int getPresentationNextCount() { return presentationNextCount; }

    public void setPresentationNextCount(int n) {
        presentationNextCount = Math.max(4, Math.min(48, n));
        markModified();
    }

    public void setPresentationSeconds(int s) {
        presentationSeconds = Math.max(5, Math.min(600, s));
        markModified();
    }

    /** Aus der Rotation genommene Ansichten (Schlüssel wie „phase:Herren A“, „tables“, „next16“, „result:Herren A“). */
    public java.util.Set<String> getPresentationOff() { return java.util.Collections.unmodifiableSet(presentationOff); }

    /** Ausdrücklich eingeschaltete Ansichten (z. B. eine noch nicht gestartete Klasse, die standardmäßig fehlt). */
    public java.util.Set<String> getPresentationOn() { return java.util.Collections.unmodifiableSet(presentationOn); }

    public void setPresentationOn(String key, boolean on) {
        if (on) {
            presentationOff.remove(key);
            presentationOn.add(key);
        } else {
            presentationOff.add(key);
            presentationOn.remove(key);
        }
        markModified();
    }

    public void markSaved() {
        modified.set(false);
    }

    public BooleanProperty modifiedProperty() { return modified; }
    public boolean isModified() { return modified.get(); }

    /** Eindeutige Kennung des Turniers (auch Dateiname im Turnierordner). */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // --- Speicherorte (Ordner des Turniers, abweichende Export-Ordner) -------------
    private String folder, pdfDir, xmlDir;

    /** Turnierordner, z. B. „Dokumente\HTS Turniere\Herbstturnier“ ({@code null} = noch nicht festgelegt). */
    public String getFolder() { return folder; }
    public void setFolder(String f) { folder = blankToNull(f); }
    /** abweichender Ordner für PDF-Exporte ({@code null} = „PDF Export“ im Turnierordner) */
    public String getPdfDir() { return pdfDir; }
    public void setPdfDir(String d) { if (!java.util.Objects.equals(pdfDir, blankToNull(d))) { pdfDir = blankToNull(d); markModified(); } }
    /** abweichender Ordner für den click-TT-Export ({@code null} = „XML Export“ im Turnierordner) */
    public String getXmlDir() { return xmlDir; }
    public void setXmlDir(String d) { if (!java.util.Objects.equals(xmlDir, blankToNull(d))) { xmlDir = blankToNull(d); markModified(); } }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    /** Ist in Phase 1 der (Stamm-)Klasse schon ein Ergebnis eingetragen? Dann sind die Klasseneinstellungen gesperrt. */
    public boolean hasResults(PlayingClass c) {
        PlayingClass root = rootOf(c);
        return matchesOf(root).stream().anyMatch(m -> m.getStatus() == MatchStatus.DONE);
    }

    // --- Import eines fertigen Turniers (click-TT-Ergebnisdatei) -----------------------

    /** Import: Gruppenphase mit genau diesen Gruppen (Entrant-IDs, Kopf zuerst) und allen Gruppenspielen anlegen. */
    public void importGroups(PlayingClass c, List<List<String>> groups) {
        c.setMode(TournamentMode.GROUPS);
        c.setGroupsBySize(false);
        c.setGroupCount(Math.max(1, groups.size()));
        c.setSwissByes(List.of());
        c.setGroups(groups, true);
        createGroupMatches(c);
        markModified();
    }

    /** Import: KO-System mit diesem Feld (Plätze, {@code null} = Freilos) anlegen. */
    public void importKnockout(PlayingClass c, List<String> slots, boolean thirdPlace) {
        c.setMode(TournamentMode.KNOCKOUT);
        c.setSwissByes(List.of());
        c.setThirdPlace(thirdPlace);
        c.setDraw(slots, true);
        createKnockoutMatches(c);
        markModified();
    }

    /** Import: Ergebnis eines Spiels übernehmen (Sätze aus Sicht von A), Zeiten setzen, KO-Sieger weiterreichen. */
    public void importResult(Match m, List<int[]> sets, int retired, LocalDateTime when) {
        applyScore(m, sets, retired);
        m.setStatus(MatchStatus.DONE);
        m.setTable(null);
        m.setStartedAt(when);
        m.setFinishedAt(when);
        findClass(m.getClassName()).ifPresent(c -> {
            if (c.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(c);
            if (c.getMode() == TournamentMode.PLAN) resolvePlan(c);
        });
        markModified();
    }

    /** Import: Klasse als gestartet und beendet markieren (ohne Spiele einzureihen). */
    public void importFinished(PlayingClass c, LocalDateTime started, LocalDateTime finished) {
        c.setStartedAt(started);
        c.setReady(false);
        c.setPaused(false);
        rootOf(c).setFinishedAt(finished);
        if (getStartedAt() == null || (started != null && started.isBefore(getStartedAt()))) setStartedAt(started);
        markModified();
    }

    // --- Kopieren --------------------------------------------------------------------

    /** Kopie ohne Auslosung: Phasen, Auslosungen, Spiele, Ergebnisse und Startzeiten entfernen (Einstellungen bleiben). */
    public void resetToBeforeDraw() {
        for (PlayingClass c : new java.util.ArrayList<>(classes)) {
            if (c.isPhase() && classes.contains(c)) removeClass(c);
        }
        for (Player p : players) p.setAttendance(Attendance.UNKNOWN);
        checkinLocked = false;
        checkinToken = null;
        for (PlayingClass c : classes) {
            c.setFollowUpClass(null);
            c.setStartedAt(null);
            c.setPaused(false);
            c.setFinishedAt(null);
            c.setReady(false);
            for (String id : new java.util.ArrayList<>(c.getWithdrawn())) c.removeWithdrawn(id);
            c.setDraw(List.of(), false);
            c.setGroups(List.of(), false);
            c.setSwissByes(List.of());
            c.setQualifiedDirect(List.of());
            c.setQualField(java.util.Map.of());
            c.setSeedLabels(java.util.Map.of());
        }
        matches.clear();
        setStartedAt(null);
        markModified();
    }

    /** Kopie nur mit Teilnehmenden: alle Klassen (und damit Zuordnungen, Paarungen, Spiele) entfernen. */
    public void removeAllClasses() {
        for (PlayingClass c : new java.util.ArrayList<>(classes)) {
            if (classes.contains(c)) removeClass(c);
        }
        for (Player p : players) {
            for (String cn : new java.util.ArrayList<>(p.getClassNames())) p.forgetClass(cn);
            for (String cn : new java.util.ArrayList<>(p.getWaitingLists())) p.forgetClass(cn);
            for (String cn : new java.util.ArrayList<>(p.getRegistrations())) p.forgetClass(cn);
        }
        pairs.clear();
        matches.clear();
        tableClasses.clear();
        for (Player p : players) p.setAttendance(Attendance.UNKNOWN);
        checkinLocked = false;
        checkinToken = null;
        setStartedAt(null);
        markModified();
    }

    // --- Testturnier ---------------------------------------------------------------
    private boolean test;

    /** Testturnier: im Kopfbereich gibt es „Alle Spiele simulieren“. */
    public boolean isTest() { return test; }
    public void setTest(boolean b) { if (test != b) { test = b; markModified(); } }

    /** Über „Fertiges Turnier importieren (click-TT)“ angelegt (Chip in der Turnierübersicht). */
    private boolean importedFinished;

    public boolean isImportedFinished() { return importedFinished; }
    public void setImportedFinished(boolean b) { if (importedFinished != b) { importedFinished = b; markModified(); } }

    // --- Vorgabeturnier ---------------------------------------------------------------------------
    /**
     * Vorgabeturnier, CSV-Import: ersetzt ALLE bisherigen Teilnehmenden (samt Klassenzuordnungen und Doppelpaarungen)
     * durch die importierten – nur, solange keine Klasse ausgelost ist.
     */
    public void replaceParticipants(List<Player> incoming) {
        if (classes.stream().anyMatch(PlayingClass::isDrawn)) {
            throw new IllegalStateException("Es ist bereits eine Klasse ausgelost – die Teilnehmenden können nicht mehr ersetzt werden.");
        }
        pairs.clear();
        players.clear();
        for (Player p : incoming) p.setAttendance(Attendance.PRESENT);     // CSV (Vorgabeturnier): gelten als angemeldet
        merge(incoming);
        markModified();
    }

    private boolean handicap;
    private HandicapRules handicapRules = new HandicapRules();

    /** Vorgabeturnier (nur beim Anlegen festgelegt). */
    public boolean isHandicap() { return handicap; }
    public void setHandicap(boolean b) { if (handicap != b) { handicap = b; markModified(); } }

    /** Gespeicherte Vorgaberegeln (Kopie zum Bearbeiten über {@link HandicapRules#copy()}). */
    public HandicapRules getHandicapRules() { return handicapRules; }

    /** Vorgabeturnier mit festgelegter (gespeicherter) Art der Vorgabe – erst dann gibt es Klassen, Tische usw. */
    public boolean isHandicapReady() {
        return handicap && handicapRules.getMode() != null;
    }

    /** Vorgaberegeln gesperrt, sobald die erste Klasse gestartet ist. */
    public boolean isHandicapLocked() {
        return classes.stream().anyMatch(PlayingClass::isStarted);
    }

    /** Vorgaberegeln speichern (nur gültige, nur vor dem Start der ersten Klasse). */
    public void setHandicapRules(HandicapRules r) {
        if (isHandicapLocked()) throw new IllegalStateException("Die Vorgaberegeln sind gesperrt – die erste Klasse ist bereits gestartet.");
        List<String> errors = r.validate();
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
        handicapRules = r.copy();
        markModified();
    }

    /** Nur zum Laden. */
    public void restoreHandicapRules(HandicapRules r) {
        handicapRules = r == null ? new HandicapRules() : r;
    }

    private static boolean womenTable(PlayingClass c) {
        return c != null && (c.getType() == ClassType.WOMEN || c.getType() == ClassType.GIRLS);
    }

    /** Personen einer Partei (Einzel: eine, Doppel: bis zu zwei). */
    private List<Player> personsOf(String entrantId) {
        List<Player> out = new java.util.ArrayList<>();
        if (entrantId == null) return out;
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair pair : pairs) {
                if (!pair.getId().equals(pid)) continue;
                out.addAll(pair.members());
            }
            return out;
        }
        for (Player p : players) if (entrantId.equals(p.getId())) out.add(p);
        return out;
    }

    private static boolean isMixedPair(List<Player> ps) {
        return ps.size() == 2 && ps.stream().filter(p -> p.getSex() == de.hts.player.Sex.FEMALE).count() == 1
                && ps.stream().filter(p -> p.getSex() == de.hts.player.Sex.MALE).count() == 1;
    }

    /**
     * Vorgabe eines Spiels in Bällen je Satz: positiv = Partei A beginnt jeden Satz mit so vielen Punkten Vorsprung,
     * negativ = Partei B, 0 = keine Vorgabe (oder kein Vorgabeturnier). Verglichen wird im Einzel Person gegen Person,
     * im Doppel die jeweils TTR-höheren und die TTR-tieferen miteinander (Mixed: Herr–Herr nach den Erwachsenen-,
     * Dame–Dame nach den Damen-Grenzen); beide Vorgaben werden verrechnet (ggf. halbiert, abgerundet).
     */
    public int handicapOf(Match m) {
        if (!isHandicapReady() || m == null || m.getA() == null || m.getB() == null) return 0;
        if (isTeamMatch(m)) return 0;              // Team-Begegnung: Vorgabe je Partie (siehe rubberHandicap)
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        return handicapBetween(personsOf(m.getA()), personsOf(m.getB()), womenTable(c == null ? null : rootOf(c)));
    }

    int handicapBetween(List<Player> a, List<Player> b, boolean women) {
        HandicapRules r = handicapRules;
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.stream().anyMatch(p -> p.getTtr() == null) || b.stream().anyMatch(p -> p.getTtr() == null)) return 0;
        if (a.size() == 1 && b.size() == 1) return r.between(a.get(0).getTtr(), b.get(0).getTtr(), women).balls();
        if (a.size() != 2 || b.size() != 2) return 0;
        if (isMixedPair(a) && isMixedPair(b)) {
            Player am = a.stream().filter(p -> p.getSex() == de.hts.player.Sex.MALE).findFirst().orElseThrow();
            Player aw = a.stream().filter(p -> p.getSex() == de.hts.player.Sex.FEMALE).findFirst().orElseThrow();
            Player bm = b.stream().filter(p -> p.getSex() == de.hts.player.Sex.MALE).findFirst().orElseThrow();
            Player bw = b.stream().filter(p -> p.getSex() == de.hts.player.Sex.FEMALE).findFirst().orElseThrow();
            return r.combine(r.between(am.getTtr(), bm.getTtr(), false).balls(), r.between(aw.getTtr(), bw.getTtr(), true).balls());
        }
        List<Player> as = new java.util.ArrayList<>(a), bs = new java.util.ArrayList<>(b);
        java.util.Comparator<Player> byTtr = java.util.Comparator.comparingInt((Player p) -> p.getTtr()).reversed();
        as.sort(byTtr);
        bs.sort(byTtr);
        return r.combine(r.between(as.get(0).getTtr(), bs.get(0).getTtr(), women).balls(),
                r.between(as.get(1).getTtr(), bs.get(1).getTtr(), women).balls());
    }

    /** „ (+3)“ hinter einem Namen in reinen Textzeilen; leer ohne Vorsprung. */
    public String handicapSuffix(Match m, String entrantId) {
        String h = handicapLabel(m, entrantId);
        return h.isEmpty() ? "" : " (" + h + ")";
    }

    /** Text der Vorgabe für eine Partei, z. B. „+3“; leer, wenn sie keinen Vorsprung hat. */
    public String handicapLabel(Match m, String entrantId) {
        int h = handicapOf(m);
        if (h == 0 || entrantId == null) return "";
        if (h > 0 && entrantId.equals(m.getA())) return "+" + h;
        if (h < 0 && entrantId.equals(m.getB())) return "+" + (-h);
        return "";
    }

    /**
     * Gründe, aus denen eine Klasse im Vorgabeturnier nicht starten darf: Vorgaberegeln fehlen, Teilnehmende ohne
     * TTR, TTR außerhalb der Klassengrenzen bzw. größtmöglicher TTR-Abstand über der höchsten Obergrenze.
     */
    public List<String> handicapProblems(PlayingClass c) {
        List<String> out = new java.util.ArrayList<>();
        if (!handicap) return out;
        if (!isHandicapReady()) {
            out.add("Die Vorgaberegeln sind noch nicht festgelegt und gespeichert.");
            return out;
        }
        PlayingClass root = rootOf(c);
        boolean women = womenTable(root);
        // Personen der Klasse samt der Tabelle, nach der sie eingestuft werden
        // Personen der Klasse samt der Tabellen, nach denen sie eingestuft werden
        java.util.Map<Player, java.util.Set<Boolean>> persons = new java.util.LinkedHashMap<>();
        if (root.isDoubles()) {
            for (DoublesPair pair : pairsIn(root)) {
                List<Player> ps = pair.members();
                boolean mixed = isMixedPair(ps);
                for (Player p : ps) {
                    java.util.Set<Boolean> tables = persons.computeIfAbsent(p, k -> new java.util.HashSet<>());
                    boolean female = p.getSex() == de.hts.player.Sex.FEMALE;
                    if (root.isTeam()) {
                        tables.add(women);                 // Einzel/Doppel der Begegnung nach dem Klassentyp
                        if (female) tables.add(true);      // Mixed-Partien: Dame–Dame nach den Damen-Grenzen
                    } else {
                        tables.add(mixed ? female : women);
                    }
                }
                if (root.isTeam() && pair.isComplete() && root.isWomanRequired()
                        && ps.stream().noneMatch(p -> p.getSex() == de.hts.player.Sex.FEMALE)) {
                    out.add("Team " + Entrant.ofTeam(pair).name() + " hat keine Dame (mindestens 1 weiblich verlangt).");
                }
            }
        } else {
            for (Player p : players) if (p.isInClass(root.getName())) persons.put(p, new java.util.HashSet<>(java.util.Set.of(women)));
        }
        List<String> noTtr = new java.util.ArrayList<>();
        persons.keySet().forEach(p -> { if (p.getTtr() == null) noTtr.add(p.getDisplayName()); });
        if (!noTtr.isEmpty()) out.add("Ohne TTR-Wert: " + String.join(", ", noTtr) + " – bitte unter „Teilnehmende“ einen Wert eintragen.");
        HandicapRules r = handicapRules;
        for (boolean w : new boolean[]{false, true}) {
            List<Player> group = persons.entrySet().stream().filter(e -> e.getValue().contains(w) && e.getKey().getTtr() != null)
                    .map(java.util.Map.Entry::getKey).toList();
            if (group.isEmpty()) continue;
            String table = w ? "Damen" : "Erwachsene";
            HandicapRules.Bounds b = r.bounds(r.getMode(), w);
            if (r.getMode() == HandicapRules.Mode.CLASSES) {
                for (Player p : group) {
                    int k = b.indexOf(p.getTtr());
                    if (k == -2) out.add(p.getDisplayName() + " (TTR " + p.getTtr() + ") liegt über der höchsten Klassengrenze (" + table + ").");
                    else if (k == -1) out.add(p.getDisplayName() + " (TTR " + p.getTtr() + ") liegt unter der niedrigsten Klassengrenze (" + table + ").");
                }
            } else {
                Player hi = group.stream().max(java.util.Comparator.comparingInt(Player::getTtr)).orElseThrow();
                Player lo = group.stream().min(java.util.Comparator.comparingInt(Player::getTtr)).orElseThrow();
                int d = hi.getTtr() - lo.getTtr();
                if (b.indexOf(d) == -2) out.add("Größtmöglicher TTR-Abstand " + d + " (" + hi.getDisplayName() + " – " + lo.getDisplayName()
                        + ") liegt über der höchsten Obergrenze (" + table + ").");
            }
        }
        return out;
    }

    private void requireHandicapStartable(PlayingClass c) {
        List<String> problems = handicapProblems(c);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("„" + rootOf(c).getName() + "“ kann nicht gestartet werden (Vorgabeturnier):\n– "
                    + String.join("\n– ", problems));
        }
    }

    /**
     * Testmodus: trägt alle offenen Spiele laufender (gestarteter, nicht angehaltener) Phasen nicht beendeter Klassen
     * zufällig ein – auch Folgerunden (KO, Schweizer System, Qualifikation), bis die Phasen fertig sind. Neue Phasen
     * werden nicht angelegt, nichts wird abgeschlossen.
     *
     * @return Anzahl eingetragener Spiele
     */
    public int simulateAll(java.util.Random random) {
        simulating = true;
        try {
            return simulateAllInternal(random);
        } finally {
            simulating = false;
        }
    }

    private int simulateAllInternal(java.util.Random random) {
        int count = 0;
        for (int guard = 0; guard < 100_000; guard++) {
            Match next = null;
            for (Match m : matches) {
                if (!m.isReady() || (m.getStatus() != MatchStatus.QUEUED && m.getStatus() != MatchStatus.ACTIVE)) continue;
                PlayingClass c = findClass(m.getClassName()).orElse(null);
                if (c == null || !c.isStarted() || c.isPaused() || rootOf(c).isFinished()) continue;
                next = m;
                break;
            }
            if (next == null) {
                // 1.0 (#77): beim Simulieren gilt kein Zeitplan – wartende Runden sofort freigeben
                boolean released = false;
                for (PlayingClass c : classes) {
                    if (!c.isStarted() || c.isPaused() || rootOf(c).isFinished() || !hasSchedule(c)) continue;
                    for (Match m : matchesOf(c)) {
                        if (m.getStatus() == MatchStatus.PLANNED && m.isReady() && previousRoundDone(c, m)
                                && c.getReleasedRounds().add(roundKey(c, m))) released = true;
                    }
                    if (released) queueReady(c);
                }
                scheduleQuestions.clear();
                if (released) {
                    dispatch();
                    continue;
                }
                // 0.38 (#60): offene Entscheidung nach Runde 1 der KO-Qualifikation – beim Simulieren: weitere Runde
                PlayingClass pending = classes.stream().filter(c -> c.isStarted() && !c.isPaused() && qualificationDecisionPending(c))
                        .findFirst().orElse(null);
                if (pending == null) break;
                decideQualification(pending, false);
                if (pending.isAwaitingStart()) resumeClass(pending);     // beim Simulieren gleich starten
                continue;
            }
            if (isTeamMatch(next)) {
                // Team-Begegnung: Standard-Aufstellung, Sieger jeder Partie zufällig
                List<Integer> winners = new java.util.ArrayList<>();
                for (int i = 0; i < rubbersOf(next).size(); i++) winners.add(random.nextBoolean() ? 1 : 2);
                recordTeamResult(next, defaultLineup(next, next.getA()), defaultLineup(next, next.getB()), winners, false);
            } else {
                recordResult(next, randomSets(winningSets(next), random), 0);
            }
            count++;
        }
        return count;
    }

    /**
     * Zufälliges, regelgerechtes Ergebnis: Sieger zufällig, Satzverhältnis zufällig (z. B. 3:1); Sätze bis 11 mit
     * mindestens 2 Punkten Abstand, Verlängerung höchstens bis 20:18.
     */
    static List<int[]> randomSets(int winningSets, java.util.Random random) {
        boolean aWins = random.nextBoolean();
        int loserSets = random.nextInt(winningSets);
        List<Boolean> winnerOfSet = new java.util.ArrayList<>();   // true = Sieger des Spiels gewinnt den Satz
        for (int i = 0; i < winningSets - 1; i++) winnerOfSet.add(true);
        for (int i = 0; i < loserSets; i++) winnerOfSet.add(false);
        java.util.Collections.shuffle(winnerOfSet, random);
        winnerOfSet.add(true);                                     // der letzte Satz entscheidet
        List<int[]> sets = new java.util.ArrayList<>();
        for (boolean w : winnerOfSet) {
            int hi, lo;
            if (random.nextInt(100) < 15) {          // Verlängerung: 12:10 … 20:18
                lo = 10 + random.nextInt(9);
                hi = lo + 2;
            } else {
                hi = 11;
                lo = random.nextInt(10);             // 11:0 … 11:9
            }
            boolean aTakes = w == aWins;
            sets.add(aTakes ? new int[]{hi, lo} : new int[]{lo, hi});
        }
        return sets;
    }

    /** Zeitpunkt, zu dem das Turnier gestartet wurde; {@code null} = noch nicht gestartet. */
    public ObjectProperty<LocalDateTime> startedAtProperty() { return startedAt; }
    public LocalDateTime getStartedAt() { return startedAt.get(); }
    public void setStartedAt(LocalDateTime t) { startedAt.set(t); }
    public boolean isStarted() { return startedAt.get() != null; }

    /** Startet das Turnier: hält die aktuelle Uhrzeit als Startzeit fest. */
    public void start() {
        if (isStarted()) {
            throw new IllegalStateException("Das Turnier wurde bereits gestartet.");
        }
        startedAt.set(LocalDateTime.now().withNano(0));
    }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String n) { name.set(n); }

    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate d) { date.set(d); }

    public StringProperty locationProperty() { return location; }
    public String getLocation() { return location.get(); }
    public void setLocation(String l) { location.set(l); }
}
