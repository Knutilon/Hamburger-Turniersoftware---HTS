package de.hts.tournament;

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
        players.addListener((ListChangeListener<Player>) c -> markModified());
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
            if (toRemove.contains(pair.getFirst())) pair.setFirst(null);
            if (toRemove.contains(pair.getSecond())) pair.setSecond(null);
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
        p.addToClass(c.getName());
        if (c.isDoubles() && pairOf(p, c).isEmpty()) {
            pairs.add(new DoublesPair(c.getName(), p, null));
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
    public record PartnerDraw(List<DoublesPair> pairs, Player leftOver) {
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
            Player x = p.getFirst() != null ? p.getFirst() : p.getSecond();
            if (x != null) singles.add(x);
        }
        // Gemeldete ohne Partner/in (z. B. aus dem Import) stehen unter „Verfügbar“ und werden mit zugelost
        for (Player x : players) {
            if (x.getRegistrations().contains(c.getName()) && pairOf(x, c).isEmpty() && !singles.contains(x)) singles.add(x);
        }
        if (singles.size() < 2) throw new IllegalStateException("Zum Zulosen braucht es mindestens 2 Teilnehmende ohne Partner/in.");
        java.util.Collections.shuffle(singles, random);
        Player left = singles.size() % 2 == 1 ? singles.remove(singles.size() - 1) : null;
        List<DoublesPair> made = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < singles.size(); i += 2) made.add(createPair(c, singles.get(i), singles.get(i + 1)));
        markModified();
        return new PartnerDraw(made, left);
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
        DoublesPair pair = new DoublesPair(c.getName(), first, second);
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
        else pair.setSecond(p);
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
        for (Player p : new Player[]{pair.getFirst(), pair.getSecond()}) {
            if (p != null) p.removeFromClass(pair.getClassName());
        }
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
        c.setDiscipline(d);
        if (d == Discipline.DOUBLES) {
            for (Player p : playersIn(c)) {
                if (pairOf(p, c).isEmpty()) pairs.add(new DoublesPair(c.getName(), p, null));
            }
        } else {
            pairs.removeIf(pair -> pair.getClassName().equals(c.getName()));
        }
        markModified();
    }

    /** Jede Person mit Meldung in einer Doppelklasse, aber ohne Paarung, bekommt eine Paarung ohne Partner/in. */
    public void ensurePairsForEntries() {
        for (PlayingClass c : classes) {
            if (!c.isDoubles()) continue;
            for (Player p : playersIn(c)) {
                if (pairOf(p, c).isEmpty()) pairs.add(new DoublesPair(c.getName(), p, null));
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
        pairs.add(new DoublesPair(id, className, first, second));
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
            pair.setSecond(null);
        } else if (pair.getSecond() == p) {
            pair.setSecond(null);
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
            return pairsIn(c).stream().filter(DoublesPair::isComplete).map(Entrant::of).toList();
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
        int n = entrantsOf(c).size();
        return c.isGroupsBySize() ? GroupDraw.groupCountForSize(n, c.getGroupSize()) : c.getGroupCount();
    }

    /** Lost die Gruppenphase aus (Schlangensystem, zufällig oder nach Setzung), speichert sie und legt die Spiele an. */
    public List<List<Entrant>> drawGroups(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        if (c.isDoubles() && !c.isPhase() && entrantsOf(c).size() > MAX_DOUBLES_GROUP) {
            throw new IllegalStateException("Bei mehr als " + MAX_DOUBLES_GROUP + " Doppeln ist Phase 1 ein KO-System.");
        }
        // Doppel (Phase 1): genau eine Gruppe, jeder gegen jeden; Vereinsgleichheit spielt keine Rolle
        int groupCount = c.isDoubles() && !c.isPhase() ? 1 : plannedGroupCount(c);
        List<List<Entrant>> groups = GroupDraw.draw(entrantsOf(c), groupCount,
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

    /** Rundenzahl im Schweizer System (Einstellung oder Vorschlag), höchstens so viele wie ohne Wiederholung möglich. */
    public int swissRounds(PlayingClass c) {
        int n = c.getMode() == TournamentMode.SWISS && !c.getGroups().isEmpty() ? c.getGroups().get(0).size() : entrantsOf(c).size();
        int r = c.getSwissRounds() > 0 ? c.getSwissRounds() : SwissSystem.defaultRounds(n);
        return Math.min(r, SwissSystem.maxRounds(n));
    }

    /** Aktuelle (zuletzt angesetzte) Runde im Schweizer System, 0 = noch keine. */
    public int swissRound(PlayingClass c) {
        return matchesOf(c).stream().mapToInt(Match::getRound).max().orElse(c.getSwissByes().isEmpty() ? 0 : c.getSwissByes().size());
    }

    /** Lost die 1. Runde im Schweizer System aus; die weiteren Runden werden nach der Tabelle gepaart. */
    public void drawSwiss(PlayingClass c, boolean seeded, java.util.Random random) {
        requireNotStarted(c);
        if (c.isDoubles()) throw new IllegalStateException("Das Schweizer System gibt es für Doppel nicht.");
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
        List<String> ranked = standings(c, 0).stream().map(r -> r.entrant().id())
                .filter(id -> !c.getWithdrawn().contains(id)).toList();
        java.util.Set<String> hadBye = new java.util.HashSet<>();
        for (String b : c.getSwissByes()) if (b != null) hadBye.add(b);
        addSwissRound(c, round + 1, SwissSystem.nextRound(ranked, SwissSystem.playedPairs(matchesOf(c)), hadBye));
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
            throw new IllegalStateException("Gruppe " + GroupDraw.groupName(from) + " braucht mindestens 2 Mitglieder – "
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

    private java.util.Map<String, Entrant> entrantIndex(PlayingClass c) {
        java.util.Map<String, Entrant> byId = new java.util.HashMap<>();
        for (Player p : players) byId.put(p.getId(), Entrant.of(p));
        for (DoublesPair pair : pairsIn(c)) byId.put("pair:" + pair.getId(), Entrant.of(pair));
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

    /** Gruppenspiele „jeder gegen jeden“, Runde für Runde über alle Gruppen nummeriert. */
    private void createGroupMatches(PlayingClass c) {
        matches.removeIf(m -> m.getClassName().equals(c.getName()));
        List<List<GroupDraw.Pairing>> perGroup = new java.util.ArrayList<>();
        for (List<Entrant> g : groupsOf(c)) perGroup.add(GroupDraw.roundRobin(g));
        int maxRound = perGroup.stream().flatMap(List::stream).mapToInt(GroupDraw.Pairing::round).max().orElse(0);
        int no = 0;
        List<Match> created = new java.util.ArrayList<>();
        for (int r = 1; r <= maxRound; r++) {
            for (int gi = 0; gi < perGroup.size(); gi++) {
                for (GroupDraw.Pairing p : perGroup.get(gi)) {
                    if (p.round() == r) {
                        created.add(new Match(null, c.getName(), "Gruppe " + GroupDraw.groupName(gi), r, ++no,
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
        Match fin = koMatch(c, sf + 1, 0);
        Match third = new Match(null, c.getName(), THIRD_PLACE_STAGE, sf + 2, no, -1, -1, -1, null, null);
        third.restorePhaseInfo(3, false);
        if (fin != null && fin.getStatus() == MatchStatus.PLANNED && fin.getQueueOrder() == 0) {
            // vor das Finale ziehen
            third.setNumber(fin.getNumber());
            fin.setNumber(no);
        }
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
        if (!c.isDrawn()) throw new IllegalStateException("„" + c.getName() + "“ ist noch nicht ausgelost.");
        requireNotStarted(c);
        requireHandicapStartable(c);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        c.setStartedAt(now);
        c.setReady(true);
        if (!isStarted()) startedAt.set(now);
        if (c.getMode() == TournamentMode.KNOCKOUT) resolveKnockout(c);
        else queueReady(c);
        dispatch();
        markModified();
    }

    /** Ergebnis von {@link #startTournament()}. */
    public record StartReport(List<PlayingClass> started, List<PlayingClass> notReady, List<PlayingClass> alreadyRunning) {
    }

    /** Was beim Turnierstart passieren würde (für den Hinweis vorab). */
    public StartReport previewStart() {
        List<PlayingClass> ready = new java.util.ArrayList<>(), notReady = new java.util.ArrayList<>(),
                running = new java.util.ArrayList<>();
        for (PlayingClass c : classes) {
            if (c.isPhase()) continue;
            if (c.isStarted()) running.add(c);
            else if (c.isReady() && c.isDrawn()) ready.add(c);
            else notReady.add(c);
        }
        return new StartReport(ready, notReady, running);
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
        for (Match m : matchesOf(c)) {
            if (m.getStatus() == MatchStatus.PLANNED && m.isReady()) {
                m.setStatus(MatchStatus.QUEUED);
                if (m.getQueueOrder() == 0) m.setQueueOrder(++queueCounter);   // nach Anhalten: alter Platz bleibt
            }
        }
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

    /** Tisch → Klassen (Namen der Klassen, nicht der Phasen); fehlt ein Tisch, ist er für alle Klassen da. */
    private final java.util.Map<Integer, java.util.Set<String>> tableClasses = new java.util.TreeMap<>();

    /** Klassen, für die der Tisch reserviert ist; leer = alle Klassen (Standard). */
    public java.util.Set<String> getTableClasses(int table) {
        return java.util.Collections.unmodifiableSet(tableClasses.getOrDefault(table, java.util.Set.of()));
    }

    /** Alle Einschränkungen (nur Tische mit Einschränkung). */
    public java.util.Map<Integer, java.util.Set<String>> getTableClassMap() {
        return java.util.Collections.unmodifiableMap(tableClasses);
    }

    /** Reserviert einen Tisch für bestimmte Klassen; leere Auswahl = alle Klassen. Belegt danach freie Tische neu. */
    public void setTableClasses(int table, java.util.Collection<String> classNames) {
        if (classNames == null || classNames.isEmpty()) tableClasses.remove(table);
        else tableClasses.put(table, new java.util.TreeSet<>(classNames));
        markModified();
    }

    /** Darf das Spiel an diesem Tisch gespielt werden (Klasse des Spiels bzw. ihrer Phase 1 freigegeben)? */
    public boolean tableAccepts(int table, Match m) {
        java.util.Set<String> allowed = tableClasses.get(table);
        if (allowed == null || allowed.isEmpty()) return true;
        PlayingClass c = findClass(m.getClassName()).orElse(null);
        String name = c == null ? m.getClassName() : rootOf(c).getName();
        return allowed.contains(name);
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
                    if (p.getFirst() != null) ids.add(p.getFirst().getId());
                    if (p.getSecond() != null) ids.add(p.getSecond().getId());
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
        if (c.getMode() == TournamentMode.SWISS) return SwissSystem.standings(groups.get(groupIndex), gm, c.getSwissByes(), lot);
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
    public static final int MAX_PHASES = 2;

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
        if (c.getMode() == TournamentMode.QUALIFICATION) return isQualificationComplete(c);
        if (c.getMode() == TournamentMode.SWISS && swissRound(c) < swissRounds(c)) return false;
        List<Match> ms = matchesOf(c);
        return c.isDrawn() && ms.stream().allMatch(m -> m.getStatus() == MatchStatus.DONE)
                && (!ms.isEmpty() || !c.getDraw().isEmpty());
    }

    /** „Gruppenphase mit Platzierungsspielen“ geht nur nach genau 2 Gruppen mit je mindestens 2 Teilnehmenden. */
    public boolean canChoosePlacement(PlayingClass c) {
        return c.getMode() == TournamentMode.GROUPS && c.getGroups().size() == 2
                && c.getGroups().stream().allMatch(g -> g.size() >= 2) && phaseNumber(c) < MAX_PHASES;
    }

    /** Darf nach dieser Phase noch eine weitere folgen (höchstens 3 Phasen, nur nach einer Gruppenphase)? */
    public boolean canHaveNextPhase(PlayingClass c) {
        // Schweizer System: nur Phase 1 und danach keine weitere Phase – nur das Endergebnis
        return c.getMode() == TournamentMode.GROUPS && phaseNumber(c) < MAX_PHASES;
    }

    /** Kann aus dieser Gruppenphase die Folgephase erstellt werden? */
    public boolean canCreateNextPhase(PlayingClass c) {
        if (c.getNextPhase() == TournamentMode.NONE || c.getFollowUpClass() != null || !isGroupPhaseComplete(c)) return false;
        if (!canHaveNextPhase(c) || rootOf(c).isFinished()) return false;
        if (c.getNextPhase() == TournamentMode.QUALIFICATION) return c.getMode() == TournamentMode.GROUPS && phaseNumber(c) == 1;
        return c.getNextPhase() != TournamentMode.PLACEMENT || canChoosePlacement(c);
    }

    /**
     * Nach dem letzten Gruppenspiel: Klasse ohne weitere Phase beenden. Die Platzierungen stehen dann im Endergebnis.
     */
    public void finishClass(PlayingClass c) {
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
    public PlayingClass createNextPhase(PlayingClass c, TournamentMode kind, int qualifiers, int groupSize,
                                        java.util.Random random) {
        if (kind == TournamentMode.QUALIFICATION) {
            throw new IllegalArgumentException("Für den Qualifikationsmodus createQualificationPhase verwenden.");
        }
        if (kind == null || kind == TournamentMode.NONE || kind == TournamentMode.SWISS) {
            throw new IllegalArgumentException("Bitte KO-System, Gruppenphase oder Gruppenphase mit Platzierungsspielen wählen.");
        }
        c.setNextPhase(kind);
        c.setQualifiers(qualifiers);
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
        PlayingClass next = createNextPhase(source, random, true);
        markModified();
        return next;
    }

    public PlayingClass createNextPhase(PlayingClass c, java.util.Random random) {
        return createNextPhase(c, random, false);
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

        if (kind == TournamentMode.QUALIFICATION) {
            int q = c.getQualDirect(), k = c.getQualKo();
            List<Entrant> direct = new java.util.ArrayList<>();
            java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
            for (int place = 0; place < q; place++) direct.addAll(tier(tables, place, next, idMap, groupOf, random));
            List<Entrant> ko = new java.util.ArrayList<>();
            java.util.Map<String, Integer> placeOf = new java.util.LinkedHashMap<>();
            for (int place = q; place < q + k; place++) {
                for (Entrant e : tier(tables, place, next, idMap, groupOf, random)) {
                    ko.add(e);
                    placeOf.put(e.id(), place + 1);
                }
            }
            next.setMode(TournamentMode.QUALIFICATION);
            next.setQualification(q, k, c.getQualRounds());
            next.setQualReserve(c.getQualReserve());
            next.setQualifiedDirect(direct.stream().map(Entrant::id).toList());
            java.util.LinkedHashMap<String, Integer> field = new java.util.LinkedHashMap<>();
            for (Entrant e : ko) field.put(e.id(), groupOf.get(e.id()));
            next.setQualField(field);
            next.setQualPlace(placeOf);
            next.setQualExtra(false);
            if (ko.size() >= 2 && c.getQualRounds() > 0) addQualificationRound(next, 1, new java.util.ArrayList<>(field.keySet()));
        } else if (kind == TournamentMode.PLACEMENT) {
            createPlacementPhase(c, next, tables, idMap);
        } else {
            // Platzierungsliste: alle Ersten (A, B, C …), dann alle Zweiten …
            List<Entrant> ranked = new java.util.ArrayList<>();
            java.util.Map<String, Integer> groupOf = new java.util.HashMap<>();
            java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
            for (int place = 0; place < c.getQualifiers(); place++) {
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

    /** Alle Gruppen-x-ten (place, 0-basiert), die besten zuerst (Spiel-, Satz-, Punktverhältnis; Gleichstand: Los). */
    private List<Entrant> tier(List<List<GroupStanding.Row>> tables, int place, PlayingClass next, java.util.Map<String, String> idMap,
                               java.util.Map<String, Integer> groupOf, java.util.Random random) {
        List<int[]> t = new java.util.ArrayList<>();
        for (int g = 0; g < tables.size(); g++) if (place < tables.get(g).size()) t.add(new int[]{g});
        java.util.Collections.shuffle(t, random);
        t.sort(java.util.Comparator.comparingDouble((int[] x) -> -gameRatio(tables.get(x[0]).get(place)))
                .thenComparingDouble(x -> -ratio(tables.get(x[0]).get(place).setsWon(), tables.get(x[0]).get(place).setsLost()))
                .thenComparingDouble(x -> -ratio(tables.get(x[0]).get(place).pointsWon(), tables.get(x[0]).get(place).pointsLost())));
        List<Entrant> out = new java.util.ArrayList<>();
        for (int[] x : t) {
            Entrant now = carryOver(next, tables.get(x[0]).get(place).entrant(), idMap);
            if (now != null) {
                out.add(now);
                groupOf.put(now.id(), x[0]);
            }
        }
        return out;
    }

    /**
     * Qualifikationsmodus (nur Phase 2 nach Gruppen in Phase 1, z. B. HEM-Hauptqualifikation): die besten {@code direct}
     * jeder Gruppe sind direkt qualifiziert; die nächsten {@code ko} jeder Gruppe spielen ein KO-System (Gewinner spielen
     * weiter; Runde 1: Höchstplatzierter gegen Niedrigstplatzierten einer anderen Gruppe). Wer {@code rounds} Spiele
     * gewinnt, ist qualifiziert; die Verlierer werden Nachrücker (später ausgeschieden = besser, je Stufe Gruppenplatz
     * vor TTR). Bei Bedarf spielen die Verlierer der letzten Runde eine Zusatzrunde ({@link #setQualExtra}).
     */
    public PlayingClass createQualificationPhase(PlayingClass c, int direct, int ko, int rounds, java.util.Random random) {
        return createQualificationPhase(c, direct, ko, rounds, 0, random);
    }

    /** @param reserve Nachrücker je Gruppe direkt aus der Gruppenphase (ohne Spiel), nach Gruppenplatz, dann TTR */
    public PlayingClass createQualificationPhase(PlayingClass c, int direct, int ko, int rounds, int reserve, java.util.Random random) {
        if (c.getMode() != TournamentMode.GROUPS || phaseNumber(c) != 1) {
            throw new IllegalStateException("Der Qualifikationsmodus folgt nur auf eine Gruppenphase in Phase 1.");
        }
        int koCount = 0;
        for (List<String> g : c.getGroups()) koCount += Math.max(0, Math.min(g.size(), direct + ko) - Math.min(g.size(), direct));
        if (direct < 1 && koCount < 2) throw new IllegalArgumentException("Ohne direkt Qualifizierte braucht die KO-Qualifikation mindestens 2 Teilnehmende.");
        int maxRounds = maxQualificationRounds(koCount);
        if (koCount >= 2 && (rounds < 1 || rounds > maxRounds)) {
            throw new IllegalArgumentException("Bei " + koCount + " Teilnehmenden in der KO-Qualifikation ist „qualifiziert nach "
                    + "n Siegen“ mit n = 1 bis " + maxRounds + " möglich.");
        }
        c.setQualification(direct, ko, rounds);
        c.setQualReserve(reserve);
        c.setQualifiers(direct + ko);
        c.setNextPhase(TournamentMode.QUALIFICATION);
        try {
            return createNextPhase(c, random);
        } catch (RuntimeException e) {
            c.setNextPhase(TournamentMode.NONE);
            throw e;
        }
    }

    // --- Qualifikationsmodus (HEM): KO aufwärts, qualifiziert nach n Siegen, Zusatzrunde ------------

    public static final String QUAL_EXTRA_STAGE = "Qualifikation Zusatzrunde";

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
        String stage = round > c.getQualRounds() ? QUAL_EXTRA_STAGE : "Qualifikation Runde " + round;
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
     * Runde 1: per Los – einzige Bedingung: ein besser Platzierter (z. B. Gruppenzweiter) spielt gegen einen schlechter
     * Platzierten (Gruppendritten) einer ANDEREN Gruppe; nach Möglichkeit auch nicht aus demselben Verein.
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
        boolean clubs = rootOf(c).isAvoidSameClub();
        for (int level = clubs ? 2 : 1; level >= 0; level--) {
            for (int attempt = 0; attempt < 300; attempt++) {
                List<String> u = new java.util.ArrayList<>(upper), lo = new java.util.ArrayList<>(lower);
                java.util.Collections.shuffle(u, qualRandom);
                java.util.Collections.shuffle(lo, qualRandom);
                List<String[]> out = matchUp(u, lo, groupOf, idx, level);
                if (out != null) return out;
            }
        }
        List<String[]> out = new java.util.ArrayList<>();
        for (int i = 0; i < upper.size(); i++) out.add(new String[]{upper.get(i), lower.get(i)});
        return out;
    }

    /** Zuordnung (Matching) oben–unten; level 2: andere Gruppe + anderer Verein, 1: andere Gruppe, 0: beliebig. */
    private static List<String[]> matchUp(List<String> upper, List<String> lower, java.util.Map<String, Integer> groupOf,
                                          java.util.Map<String, Entrant> idx, int level) {
        int n = upper.size();
        int[] partner = new int[lower.size()];
        java.util.Arrays.fill(partner, -1);
        for (int i = 0; i < n; i++) {
            if (!augmentPair(i, new boolean[lower.size()], upper, lower, groupOf, idx, level, partner)) return null;
        }
        List<String[]> out = new java.util.ArrayList<>();
        String[] of = new String[n];
        for (int j = 0; j < lower.size(); j++) if (partner[j] >= 0) of[partner[j]] = lower.get(j);
        for (int i = 0; i < n; i++) out.add(new String[]{upper.get(i), of[i]});
        return out;
    }

    private static boolean augmentPair(int i, boolean[] seen, List<String> upper, List<String> lower,
                                       java.util.Map<String, Integer> groupOf, java.util.Map<String, Entrant> idx, int level, int[] partner) {
        for (int j = 0; j < lower.size(); j++) {
            if (seen[j]) continue;
            String a = upper.get(i), b = lower.get(j);
            if (level >= 1 && java.util.Objects.equals(groupOf.get(a), groupOf.get(b))) continue;
            if (level >= 2 && idx.get(a) != null && idx.get(b) != null && idx.get(a).sameClub(idx.get(b))) continue;
            seen[j] = true;
            if (partner[j] < 0 || augmentPair(partner[j], seen, upper, lower, groupOf, idx, level, partner)) {
                partner[j] = i;
                return true;
            }
        }
        return false;
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
            List<String> next = fieldOrder(c, roundWinners(c, round));
            if (next.size() >= 2) addQualificationRound(c, round + 1, next);
        } else if (c.isQualExtra()) {
            List<String> losers = fieldOrder(c, roundLosers(c, n));
            if (losers.size() >= 2) addQualificationRound(c, n + 1, losers);
        }
    }

    /**
     * Zusatzrunde für die Verlierer der letzten Runde (bei Bedarf) zu- oder abschalten – auch während der Phase. Ist
     * Runde n schon gespielt, wird die Zusatzrunde sofort angelegt. Abschalten nur, solange sie nicht läuft bzw.
     * gespielt ist.
     */
    public void setQualExtra(PlayingClass c, boolean on) {
        if (c.getMode() != TournamentMode.QUALIFICATION) throw new IllegalStateException("Nur im Qualifikationsmodus.");
        if (rootOf(c).isFinished()) throw new IllegalStateException("Die Klasse ist abgeschlossen.");
        int n = c.getQualRounds();
        List<Match> extra = roundMatches(c, n + 1);
        if (on) {
            c.setQualExtra(true);
            if (extra.isEmpty()) advanceQualification(c);
        } else {
            for (Match m : extra) {
                if (m.getStatus() == MatchStatus.DONE || m.getStatus().atTable()) {
                    throw new IllegalStateException("Die Zusatzrunde läuft schon bzw. ist gespielt – zum Abschalten erst die "
                            + "Ergebnisse bzw. Spiele zurückziehen.");
                }
            }
            matches.removeAll(extra);
            List<String> byes = new java.util.ArrayList<>(c.getSwissByes());
            if (byes.size() > n) c.setSwissByes(byes.subList(0, n));
            c.setQualExtra(false);
        }
        dispatch();
        markModified();
    }

    private boolean isQualificationComplete(PlayingClass c) {
        int n = c.getQualRounds();
        if (c.getQualField().size() < 2 || n == 0) return true;
        int round = qualificationRound(c);
        if (round == 0 || !qualificationRoundDone(c, round)) return false;
        if (round < n) return fieldOrder(c, roundWinners(c, round)).size() < 2;
        if (round == n) return !c.isQualExtra() || roundLosers(c, n).size() < 2;
        return true;      // Zusatzrunde gespielt
    }

    /** Sortierung innerhalb einer Stufe: besserer Gruppenplatz zuerst (Zweite vor Dritten), dann TTR. */
    private java.util.Comparator<Entrant> placeThenTtr(PlayingClass c) {
        java.util.Map<String, Integer> place = c.getQualPlace();
        return java.util.Comparator.comparingInt((Entrant e) -> place.getOrDefault(e.id(), 99))
                .thenComparing(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
    }

    /** Qualifizierte: direkt Qualifizierte (nach TTR) und die Sieger der Runde n (Gruppenplatz, dann TTR). */
    public List<Entrant> qualified(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        List<Entrant> direct = new java.util.ArrayList<>();
        for (String id : c.getQualifiedDirect()) if (idx.containsKey(id)) direct.add(idx.get(id));
        direct.sort(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed());
        List<Entrant> out = new java.util.ArrayList<>(direct);
        int n = c.getQualRounds();
        List<Entrant> viaKo = new java.util.ArrayList<>();
        if (c.getQualField().size() == 1) {
            Entrant only = idx.get(c.getQualField().keySet().iterator().next());
            if (only != null) viaKo.add(only);
        } else if (n > 0 && qualificationRound(c) >= n && qualificationRoundDone(c, n)) {
            for (Match m : roundMatches(c, n)) if (m.winner() != null && idx.containsKey(m.winner())) viaKo.add(idx.get(m.winner()));
        }
        viaKo.sort(placeThenTtr(c));
        out.addAll(viaKo);
        return out;
    }

    /**
     * Nachrücker-Stufen (bereits feststehend), von oben nach unten: Freilos der Runde n; Sieger der Zusatzrunde
     * (inkl. Freilos) bzw. – ohne Zusatzrunde – Verlierer der Runde n; Verlierer der Zusatzrunde; Verlierer der Runden
     * n-1, …, 1. Innerhalb einer Stufe: Gruppenplatz, dann TTR.
     */
    public List<List<Entrant>> qualificationTiers(PlayingClass c) {
        return tiersWithLevel(c).stream().map(Tier::entrants).toList();
    }

    private record Tier(int level, List<Entrant> entrants) {
    }

    private List<Tier> tiersWithLevel(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        int n = c.getQualRounds();
        List<Tier> tiers = new java.util.ArrayList<>();
        java.util.function.Function<java.util.Collection<String>, List<Entrant>> ents = ids -> {
            List<Entrant> l = new java.util.ArrayList<>();
            for (String id : ids) if (idx.containsKey(id)) l.add(idx.get(id));
            l.sort(placeThenTtr(c));
            return l;
        };
        int round = qualificationRound(c);
        if (n > 0 && round >= n && qualificationRoundDone(c, n)) {
            String byeN = qualBye(c, n);
            if (byeN != null) tiers.add(new Tier(2, ents.apply(List.of(byeN))));
            boolean extraDone = round > n && qualificationRoundDone(c, n + 1);
            if (extraDone) {
                tiers.add(new Tier(4, ents.apply(roundWinners(c, n + 1))));
                tiers.add(new Tier(6, ents.apply(roundLosers(c, n + 1))));
            } else if (round == n) {
                tiers.add(new Tier(4, ents.apply(roundLosers(c, n))));
            }
        }
        for (int r = Math.min(n - 1, round); r >= 1; r--) {
            if (qualificationRoundDone(c, r)) tiers.add(new Tier(10 + (n - r), ents.apply(roundLosers(c, r))));
        }
        List<Entrant> reserve = reserveFromGroups(c);
        if (!reserve.isEmpty()) tiers.add(new Tier(100, reserve));
        tiers.removeIf(t -> t.entrants().isEmpty());
        return tiers;
    }

    /**
     * Nachrücker direkt aus der Gruppenphase (ohne Spiel): die Gruppenplätze nach direkt Qualifizierten und
     * KO-Qualifikation, z. B. Dritte und Vierte – zuerst nach Gruppenplatz, innerhalb nach TTR.
     */
    private List<Entrant> reserveFromGroups(PlayingClass c) {
        int n = c.getQualReserve();
        PlayingClass src = c.getSourceClass() == null ? null : findClass(c.getSourceClass()).orElse(null);
        if (n <= 0 || src == null) return List.of();
        int from = c.getQualDirect() + c.getQualKo();
        List<Entrant> out = new java.util.ArrayList<>();
        java.util.Map<String, Integer> placeOf = new java.util.HashMap<>();
        for (int g = 0; g < src.getGroups().size(); g++) {
            List<GroupStanding.Row> rows = standings(src, g);
            for (int p = from; p < from + n && p < rows.size(); p++) {
                out.add(rows.get(p).entrant());
                placeOf.put(rows.get(p).entrant().id(), p + 1);
            }
        }
        out.sort(java.util.Comparator.comparingInt((Entrant e) -> placeOf.get(e.id()))
                .thenComparing(java.util.Comparator.comparing((Entrant e) -> e.ttr() == null ? Integer.MIN_VALUE : e.ttr()).reversed()));
        return out;
    }

    /** Bezeichnung einer Nachrücker-Stufe: „Nachrücker Rang 1, 2, …“. */
    public String qualificationTierLabel(PlayingClass c, int tierIndex) {
        return "Nachrücker Rang " + (tierIndex + 1);
    }

    /**
     * Endplatzierung im Qualifikationsmodus: „qualifiziert“ (direkt nach TTR, dann über die KO), „Nachrücker Rang 1, 2,
     * …“ (je Stufe Gruppenplatz, dann TTR). Wer noch spielt, steht als „noch in der Qualifikation“ dort, wo er
     * höchstens landen kann.
     */
    private List<Placing> rankQualification(PlayingClass c) {
        java.util.Map<String, Entrant> idx = entrantIndex(c);
        List<Placing> list = new java.util.ArrayList<>();
        java.util.Set<String> placed = new java.util.HashSet<>();
        int next = 1;
        for (Entrant e : qualified(c)) {
            if (!placed.add(e.id())) continue;
            list.add(new Placing(next, next, e, "qualifiziert"));
            next++;
        }
        List<Tier> tiers = tiersWithLevel(c);
        for (Tier t : tiers) for (Entrant e : t.entrants()) placed.add(e.id());
        // noch im Rennen: in Runde ≤ n vor allen Nachrückern, in der Zusatzrunde nach dem Freilos der Runde n
        List<Entrant> open = new java.util.ArrayList<>();
        for (String id : c.getQualField().keySet()) if (!placed.contains(id) && idx.containsKey(id)) open.add(idx.get(id));
        open.sort(placeThenTtr(c));
        int openLevel = qualificationRound(c) > c.getQualRounds() ? 3 : 1;
        boolean openDone = open.isEmpty();
        int rank = 0;
        for (Tier t : tiers) {
            if (!openDone && t.level() > openLevel) {
                int from = next;
                for (Entrant e : open) list.add(new Placing(from, from + open.size() - 1, e, "noch in der Qualifikation"));
                next += open.size();
                openDone = true;
            }
            String label = qualificationTierLabel(c, rank++);
            for (Entrant e : t.entrants()) {
                list.add(new Placing(next, next, e, label));
                next++;
            }
        }
        if (!openDone) {
            int from = next;
            for (Entrant e : open) list.add(new Placing(from, from + open.size() - 1, e, "noch in der Qualifikation"));
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
        pairs.add(copy);
        Entrant now = Entrant.of(copy);
        idMap.put(e.id(), now.id());
        return now;
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
            case GROUPS, SWISS -> {
                if (follow == null) yield rankGroups(c, 0, 0);
                List<Placing> top = new java.util.ArrayList<>(rank(follow, depth + 1));
                if (follow.getMode() != TournamentMode.PLACEMENT) {
                    int skip = follow.getMode() == TournamentMode.QUALIFICATION
                            ? follow.getQualDirect() + follow.getQualKo() + follow.getQualReserve() : c.getQualifiers();
                    top.addAll(rankGroups(c, skip, top.size(), follow.getMode() == TournamentMode.QUALIFICATION));
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
                int from = inRound / 2 + 1, to = inRound == 4 ? 3 : inRound;
                if (inRound == 4) from = 3;
                list.add(new Placing(from, to, e, inRound == 2 ? "Finale verloren" : "ausgeschieden: " + lost.getStage()));
            } else {
                list.add(new Placing(1, (int) alive, e, "noch im Wettbewerb"));
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
        for (int place = skipPlaces + 1; place <= maxSize; place++) {
            List<GroupStanding.Row> same = new java.util.ArrayList<>();
            List<Integer> groupIdx = new java.util.ArrayList<>();
            for (int g = 0; g < groupCount; g++) {
                if (place <= tables.get(g).size()) {
                    same.add(tables.get(g).get(place - 1));
                    groupIdx.add(g);
                }
            }
            int from = next, to = samePlace ? next : next + same.size() - 1;
            for (int k = 0; k < same.size(); k++) {
                String info = (c.getMode() == TournamentMode.SWISS ? "Schweizer System, "
                        : groupCount > 1 ? "Gruppe " + GroupDraw.groupName(groupIdx.get(k)) + ", " : "Gruppe, ")
                        + "Platz " + place + (done ? "" : " (Zwischenstand)");
                list.add(new Placing(from, to, same.get(k).entrant(), info));
            }
            next = next + same.size();
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
        applyScore(m, sets, retired);
        m.setStatus(MatchStatus.DONE);
        m.setTable(null);
        m.setFinishedAt(LocalDateTime.now().withNano(0));
        if (retired != 0) applyRetirement(m);
        if (m.isKnockout()) findClass(m.getClassName()).ifPresent(this::resolveKnockout);
        findClass(m.getClassName()).ifPresent(this::advanceSwiss);
        findClass(m.getClassName()).ifPresent(this::advanceQualification);
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
        java.util.Map<String, Entrant> idx = c == null ? java.util.Map.of() : entrantIndex(c);
        Entrant e = idx.get(entrantId);
        return e == null ? "(entfernt)" : e.name();
    }

    /** Personen einer Partei als {Name „Nachname, Vorname“, Verein}: Einzel eine Zeile, Doppel je Person eine. */
    public List<String[]> partyLines(Match m, String entrantId) {
        List<String[]> out = new java.util.ArrayList<>();
        if (entrantId == null) return out;
        if (entrantId.startsWith("pair:")) {
            String pid = entrantId.substring(5);
            for (DoublesPair pair : pairs) {
                if (!pair.getId().equals(pid)) continue;
                for (Player p : new Player[]{pair.getFirst(), pair.getSecond()}) {
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
                        || de.hts.player.PlayerFilter.matchesPerson(pair.getSecond(), query);
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
            for (Player p : new Player[]{pair.getFirst(), pair.getSecond()}) {
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
        java.util.Set<String> now = new java.util.HashSet<>();
        for (Entrant e : entrantsOf(c)) now.add(e.id());
        return !drawn.equals(now);
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
        setStartedAt(null);
        markModified();
    }

    // --- Testturnier ---------------------------------------------------------------
    private boolean test;

    /** Testturnier: im Kopfbereich gibt es „Alle Spiele simulieren“. */
    public boolean isTest() { return test; }
    public void setTest(boolean b) { if (test != b) { test = b; markModified(); } }

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
                if (pair.getFirst() != null) out.add(pair.getFirst());
                if (pair.getSecond() != null) out.add(pair.getSecond());
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
        java.util.Map<Player, Boolean> persons = new java.util.LinkedHashMap<>();
        if (root.isDoubles()) {
            for (DoublesPair pair : pairsIn(root)) {
                List<Player> ps = new java.util.ArrayList<>();
                if (pair.getFirst() != null) ps.add(pair.getFirst());
                if (pair.getSecond() != null) ps.add(pair.getSecond());
                boolean mixed = isMixedPair(ps);
                for (Player p : ps) persons.put(p, mixed ? p.getSex() == de.hts.player.Sex.FEMALE : women);
            }
        } else {
            for (Player p : players) if (p.isInClass(root.getName())) persons.put(p, women);
        }
        List<String> noTtr = new java.util.ArrayList<>();
        persons.keySet().forEach(p -> { if (p.getTtr() == null) noTtr.add(p.getDisplayName()); });
        if (!noTtr.isEmpty()) out.add("Ohne TTR-Wert: " + String.join(", ", noTtr) + " – bitte unter „Teilnehmende“ einen Wert eintragen.");
        HandicapRules r = handicapRules;
        for (boolean w : new boolean[]{false, true}) {
            List<Player> group = persons.entrySet().stream().filter(e -> e.getValue() == w && e.getKey().getTtr() != null)
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
            if (next == null) break;
            recordResult(next, randomSets(winningSets(next), random), 0);
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
