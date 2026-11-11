/*
 * HTS – Hamburger Turniersoftware
 * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Dieses Programm ist freie Software unter der GNU Affero General
 * Public License v3.0; Einzelheiten in der Datei LICENSE.
 */
package de.hts.player;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ein Spieler (eine Person) im Turnier.
 * Die Felder orientieren sich an den Personendaten des click-TT-Turnierexports.
 */
public class Player {

    public static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Interne, stabile Kennung (z. B. für Doppelpaarungen in der Turnierdatei). */
    private String id;
    private String firstname;
    private String lastname;
    private Integer birthyear;
    /** Vollständiges Geburtsdatum, falls bekannt (click-TT liefert meist nur den Jahrgang). */
    private LocalDate birthdate;
    private Sex sex = Sex.UNKNOWN;

    private String clubName;
    private String clubNr;
    private String clubFederation;

    /** Personen-ID aus click-TT (z. B. "NU1234567"). Eindeutig, wenn vorhanden. */
    private String internalNr;
    /** Spielberechtigungs-/Lizenznummer. */
    private String licenceNr;

    private Integer ttr;
    private Integer ttrMatchCount;
    private String nationality;
    private String region;

    /** Klassen, in denen die Person angemeldet ist („In der Klasse“). */
    private final java.util.Map<String, Long> classes = new java.util.TreeMap<>();

    /** Fortlaufende Nummer für die Reihenfolge der Anmeldungen (für die Höchstzahl je Klasse). */
    private static long lastOrder;

    private static synchronized long nextOrder() {
        lastOrder = Math.max(lastOrder + 1, System.currentTimeMillis() * 1000);
        return lastOrder;
    }

    /**
     * Klassen, für die die Person laut click-TT nur auf der Warteliste steht. Sie ist dort
     * (noch) nicht angemeldet und erscheint unter „Verfügbar“ mit dem Hinweis „Warteliste“.
     */
    private final Set<String> waitingLists = new TreeSet<>();
    /** Laut Meldeliste für diese Klassen gemeldet, aber (noch) nicht in der Klasse – steht unter „Verfügbar“. */
    private final Set<String> registrations = new TreeSet<>();
    /** 0.35: Doppelpartner laut Meldeliste je Klasse (Klasse → identityKey des Partners). */
    private final java.util.Map<String, String> registeredPartners = new java.util.TreeMap<>();
    /** 0.35: Anwesenheit am Turniertag. */
    private Attendance attendance = Attendance.UNKNOWN;

    public Player() {
    }

    public Player(String firstname, String lastname) {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    /**
     * Schlüssel zum Erkennen doppelter Spieler: zuerst die click-TT-Personen-ID,
     * dann die Lizenznummer, zuletzt Name + Jahrgang + Verein.
     */
    public String identityKey() {
        if (notBlank(internalNr)) {
            return "id:" + internalNr.trim();
        }
        if (notBlank(licenceNr)) {
            return "lic:" + licenceNr.trim();
        }
        return ("name:" + norm(lastname) + "|" + norm(firstname) + "|" + getBirthyear() + "|" + norm(clubName));
    }

    /**
     * Übernimmt die aktuelleren Daten eines anderen Datensatzes derselben Person.
     * Klassen werden zusammengeführt: Eine Anmeldung wird dabei nie entfernt, nur hinzugefügt
     * (z. B. wenn jemand von der Warteliste nachgerückt ist).
     */
    public void updateFrom(Player other) {
        if (notBlank(other.firstname)) firstname = other.firstname;
        if (notBlank(other.lastname)) lastname = other.lastname;
        if (other.birthyear != null) birthyear = other.birthyear;
        if (other.birthdate != null) birthdate = other.birthdate;
        if (other.sex != Sex.UNKNOWN) sex = other.sex;
        if (notBlank(other.clubName)) clubName = other.clubName;
        if (notBlank(other.clubNr)) clubNr = other.clubNr;
        if (notBlank(other.clubFederation)) clubFederation = other.clubFederation;
        if (notBlank(other.internalNr)) internalNr = other.internalNr;
        if (notBlank(other.licenceNr)) licenceNr = other.licenceNr;
        if (other.ttr != null) ttr = other.ttr;
        if (other.ttrMatchCount != null) ttrMatchCount = other.ttrMatchCount;
        if (notBlank(other.nationality)) nationality = other.nationality;
        if (notBlank(other.region)) region = other.region;
        other.classes.keySet().forEach(this::addToClass);
        other.waitingLists.forEach(this::addWaitingList);
        other.registrations.forEach(this::addRegistration);
        registeredPartners.putAll(other.registeredPartners);
    }

    // --- Anwesenheit (0.35) --------------------------------------------------

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance a) {
        Attendance next = a == null ? Attendance.UNKNOWN : a;
        // 0.38 (#54): merken, ob die endgültige Abmeldung per Handy kam (bestätigte Handy-Abmeldung) oder von der Turnierleitung
        if (next == Attendance.WITHDRAWN && attendance != Attendance.WITHDRAWN) withdrawnByPhone = attendance == Attendance.WITHDRAW_REQUESTED;
        if (next != Attendance.WITHDRAWN) withdrawnByPhone = false;
        attendance = next;
    }

    private boolean withdrawnByPhone;

    /** Endgültig abgemeldet über eine bestätigte Handy-Abmeldung (sonst durch die Turnierleitung). */
    public boolean isWithdrawnByPhone() { return withdrawnByPhone; }

    /** Nur zum Laden. */
    public void restoreWithdrawnByPhone(boolean b) { withdrawnByPhone = b && attendance == Attendance.WITHDRAWN; }

    /** Am Turniertag anwesend („angemeldet“). */
    public boolean isPresent() {
        return attendance == Attendance.PRESENT;
    }

    /** Nachrücker = laut Meldeliste (click-TT) auf der Warteliste mindestens einer Klasse. */
    public boolean isReserve() {
        return !waitingLists.isEmpty();
    }

    /** 0.38 (#55): nur Nachrücker (Warteliste), nirgends gemeldet – zählt nicht zu den An-/Abgemeldeten. */
    public boolean isReserveOnly() {
        return !waitingLists.isEmpty() && registrations.isEmpty() && classes.isEmpty();
    }

    /** Laut Meldeliste für die Klasse gemeldet (noch nicht oder schon in der Klasse). */
    public boolean isEnteredFor(String className) {
        return registrations.contains(className) || classes.containsKey(className);
    }

    /** Doppelpartner laut Meldeliste ({@code identityKey}) oder {@code null}. */
    public String getRegisteredPartner(String className) {
        return registeredPartners.get(className);
    }

    public void setRegisteredPartner(String className, String partnerKey) {
        if (partnerKey == null) registeredPartners.remove(className);
        else registeredPartners.put(className, partnerKey);
    }

    public java.util.Map<String, String> getRegisteredPartners() {
        return Collections.unmodifiableMap(registeredPartners);
    }

    /**
     * 0.35: Meldungen aus der Meldeliste landen nicht mehr direkt „In der Klasse“, sondern als Meldung unter
     * „Verfügbar“ (Übernahme erst für anwesende Personen).
     */
    public void entriesToRegistrations() {
        for (String c : new java.util.ArrayList<>(classes.keySet())) {
            classes.remove(c);
            registrations.add(c);
        }
    }

    // --- Klassen -----------------------------------------------------------

    /** Herkunft einer Klassenzuordnung (0.42, Bug #75): vorher gemeldet bzw. Nachrücker laut Meldeliste. */
    public static final String FROM_REGISTRATION = "registration";
    public static final String FROM_WAITING_LIST = "waiting-list";

    /** Klasse → Herkunft der Zuordnung; wird beim Zurückziehen aus der Klasse wiederhergestellt. */
    private final java.util.Map<String, String> classOrigins = new java.util.TreeMap<>();

    /** Meldet die Person in einer Klasse an (ein Wartelisten-Hinweis dazu entfällt, die Herkunft wird gemerkt). */
    public void addToClass(String className) {
        if (classes.containsKey(className)) return;
        classes.put(className, nextOrder());
        rememberOrigin(className);
    }

    private void rememberOrigin(String className) {
        if (registrations.remove(className)) classOrigins.put(className, FROM_REGISTRATION);
        else if (waitingLists.remove(className)) classOrigins.put(className, FROM_WAITING_LIST);
    }

    /** Herkunft der Zuordnung ({@link #FROM_REGISTRATION}, {@link #FROM_WAITING_LIST}) oder {@code null}. */
    public String getClassOrigin(String className) {
        return classes.containsKey(className) ? classOrigins.get(className) : null;
    }

    /** Nur zum Laden. */
    public void restoreClassOrigin(String className, String origin) {
        if (classes.containsKey(className) && (FROM_REGISTRATION.equals(origin) || FROM_WAITING_LIST.equals(origin))) {
            classOrigins.put(className, origin);
        }
    }

    /** Beim Laden: Anmeldung mit gespeicherter Reihenfolge. */
    public void addToClass(String className, long order) {
        classes.put(className, order);
        rememberOrigin(className);
        synchronized (Player.class) {
            lastOrder = Math.max(lastOrder, order);
        }
    }

    /** Zeitpunkt/Reihenfolge der Anmeldung in der Klasse ({@code null} = nicht angemeldet). */
    public Long getClassOrder(String className) {
        return classes.get(className);
    }

    /**
     * Nimmt die Person aus der Klasse. 0.42 (Bug #75): Wer vorher gemeldet bzw. Nachrücker war, ist es danach wieder –
     * nur die Zuordnung entfällt (die Anwesenheit bleibt ohnehin unverändert).
     * @return {@code true}, wenn die Person in der Klasse war
     */
    public boolean removeFromClass(String className) {
        if (classes.remove(className) == null) return false;
        String origin = classOrigins.remove(className);
        if (FROM_REGISTRATION.equals(origin)) registrations.add(className);
        else if (FROM_WAITING_LIST.equals(origin)) waitingLists.add(className);
        return true;
    }

    public boolean isInClass(String className) {
        return classes.containsKey(className);
    }

    /** Benennt eine Klasse um (Anmeldung und Wartelisten-Hinweis). */
    public void renameClass(String oldName, String newName) {
        Long order = classes.remove(oldName);
        if (order != null) classes.put(newName, order);
        if (waitingLists.remove(oldName)) waitingLists.add(newName);
        if (registrations.remove(oldName)) registrations.add(newName);
        String origin = classOrigins.remove(oldName);
        if (origin != null) classOrigins.put(newName, origin);
        String partner = registeredPartners.remove(oldName);
        if (partner != null) registeredPartners.put(newName, partner);
    }

    /** Entfernt alles zu einer gelöschten Klasse. */
    public void forgetClass(String className) {
        classes.remove(className);
        waitingLists.remove(className);
        registrations.remove(className);
        classOrigins.remove(className);
        registeredPartners.remove(className);
    }

    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(classes.keySet());
    }

    /** Merkt sich, dass die Person laut Meldeliste in dieser Klasse auf der Warteliste steht. */
    public void addWaitingList(String className) {
        if (classes.containsKey(className)) classOrigins.putIfAbsent(className, FROM_WAITING_LIST);
        else waitingLists.add(className);
    }

    public Set<String> getWaitingLists() {
        return Collections.unmodifiableSet(waitingLists);
    }

    /** Merkt sich eine Meldung laut Meldeliste, ohne die Person schon in die Klasse zu nehmen. */
    public void addRegistration(String className) {
        if (classes.containsKey(className)) {
            classOrigins.putIfAbsent(className, FROM_REGISTRATION);   // erneuter Import: schon übernommen
        } else if (!waitingLists.contains(className)) {
            registrations.add(className);
        }
    }

    public Set<String> getRegistrations() {
        return Collections.unmodifiableSet(registrations);
    }

    /** Angemeldet = in mindestens einer Klasse. */
    public boolean isRegistered() {
        return !classes.isEmpty();
    }

    /** z. B. "Herren Doppel, Herren Einzel" bzw. "Warteliste: Damen Einzel" */
    public String getClassesText() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (!classes.isEmpty()) parts.add(String.join(", ", classes.keySet()));
        if (!registrations.isEmpty()) parts.add("gemeldet: " + String.join(", ", registrations));
        if (!waitingLists.isEmpty()) parts.add("Warteliste: " + String.join(", ", waitingLists));
        return String.join(" · ", parts);
    }

    // --- Anzeige -----------------------------------------------------------

    public String getDisplayName() {
        String ln = lastname == null ? "" : lastname;
        String fn = firstname == null ? "" : firstname;
        return fn.isEmpty() ? ln : ln + ", " + fn;
    }

    /** Geburtsdatum als "TT.MM.JJJJ", sonst nur der Jahrgang, sonst leer. */
    public String getBirthText() {
        if (birthdate != null) return birthdate.format(GERMAN_DATE);
        if (birthyear != null) return String.valueOf(birthyear);
        return "";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.GERMAN);
    }

    // --- Getter / Setter -------------------------------------------------

    public String getId() {
        if (id == null) id = java.util.UUID.randomUUID().toString().substring(0, 13).replace("-", "");
        return id;
    }
    public void setId(String id) { this.id = id; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    /** Jahrgang; wird aus dem Geburtsdatum abgeleitet, falls nur dieses bekannt ist. */
    public Integer getBirthyear() {
        if (birthyear == null && birthdate != null) return birthdate.getYear();
        return birthyear;
    }
    public void setBirthyear(Integer birthyear) { this.birthyear = birthyear; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex == null ? Sex.UNKNOWN : sex; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getClubNr() { return clubNr; }
    public void setClubNr(String clubNr) { this.clubNr = clubNr; }

    public String getClubFederation() { return clubFederation; }
    public void setClubFederation(String clubFederation) { this.clubFederation = clubFederation; }

    public String getInternalNr() { return internalNr; }
    public void setInternalNr(String internalNr) { this.internalNr = internalNr; }

    public String getLicenceNr() { return licenceNr; }
    public void setLicenceNr(String licenceNr) { this.licenceNr = licenceNr; }

    public Integer getTtr() { return ttr; }
    public void setTtr(Integer ttr) { this.ttr = ttr; }

    public Integer getTtrMatchCount() { return ttrMatchCount; }
    public void setTtrMatchCount(Integer ttrMatchCount) { this.ttrMatchCount = ttrMatchCount; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player p)) return false;
        return Objects.equals(identityKey(), p.identityKey());
    }

    @Override
    public int hashCode() {
        return identityKey().hashCode();
    }

    @Override
    public String toString() {
        return getDisplayName() + (clubName != null ? " (" + clubName + ")" : "");
    }
}
