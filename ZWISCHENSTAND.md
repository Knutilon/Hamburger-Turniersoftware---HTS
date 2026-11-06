# Zwischenstand HTS 0.31.0 (06.11.2026)

## Ablauf GitHub (BESTÄTIGT 03.11.2026)

Bei jedem „kompilieren“: Quellcode (src, pom.xml, README.md, ZWISCHENSTAND.md, CLAUDE.md, docs, samples, .gitignore,
„HTS starten.bat“) als EIN Commit „HTS x.y.z“ auf main in https://github.com/Knutilon/Hamburger-Turniersoftware---HTS
(Klon /home/claude/hamburger-turniersoftware---hts, push mit -c push.negotiate=false). Keine Releases/Tags, keine
alte Versionshistorie. Windows-ZIP weiter per Datei. Repo ist öffentlich. 0.28.0 hochgeladen (Commit b4cb270).
WUNSCH (03.11.2026, ab der nächsten Version): zusätzlich Release je Version mit dem Windows-ZIP und dem Nutzer statt
des ZIPs den Link zum aktuellen Release nennen. PROBLEM: Release anlegen ist aus der Sitzung gesperrt (HTTP 403 „not
permitted for this session type“), Tags-Push ebenfalls.
ENTSCHIEDEN (Weg 2): Commit „HTS x.y.z“ hochladen; dem Nutzer das Windows-ZIP schicken plus vorausgefüllten Link
https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/releases/new?tag=vX.Y.Z&target=main&title=HTS%20X.Y.Z
(+ Release-Text „Neu in X.Y.Z“ aus der README zum Einfügen). Nutzer zieht das ZIP hinein und veröffentlicht; danach
Link zum Release: https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/releases/tag/vX.Y.Z

## Feature-Sammlung für 0.32.0 (NICHT kompilieren – bis „… kompilieren“)

(neue Punkte zusätzlich als GitHub-Issue mit Label „Feature“ anlegen)

1. OFFEN aus 0.31.0 (Issue #12): JavaFX 21 GA → 21.0.8 (neueste sicher frei verfügbare 21.0.x; Windows-Jars
   javafx-base/-controls/-graphics-21.0.8-win.jar) und PDFBox/pdfbox-io/fontbox 3.0.5 → 3.0.8. Maven Central und
   Gluon-/Apache-Downloads sind aus der Sitzung gesperrt → Nutzer um die 6 Jar-Dateien gebeten (06.11.2026). Danach
   pom.xml angleichen (javafx.version 21.0.8, pdfbox 3.0.8), Fat-Jar neu bauen, Lizenzhinweise, ABHAENGIGKEITEN.md.

## Umgesetzt

0.31.0 (Issues #13, #14 umgesetzt; #12 teilweise – Temurin 21.0.13 eingebaut). Doku NICHT aktualisiert (nur auf Zuruf).
Vorgabeturnier: tournament/HandicapRules (Mode CLASSES/DISTANCE, Serve, halve, Bounds je Erwachsene/Damen mit
firstLower + Obergrenzen, Bälle; validate, between, combine, encode/decode), Tournament.handicap/handicapRules
(isHandicapReady, isHandicapLocked, setHandicapRules, handicapOf/handicapLabel/handicapSuffix, handicapProblems,
Startsperre in startClass/startTournament, replaceParticipants); persistiert als Attribute „handicap“ und „hc“.
TournamentDialog Checkbox (nur beim Anlegen, Kopie übernimmt vom Original), Summary.handicap + Chip in der Übersicht.
ui/HandicapView (Reiter „Vorgaberegeln“, Speichern mit Rückfrage, confirmLeave Speichern/Verwerfen/Zurück, gesperrt
nach Start), WorkspaceView (Menü nur Teilnehmende + Vorgaberegeln bis gespeichert, CSV-Import/Vorlage statt XML,
kein click-TT-Export), ClassDialog nur Erwachsene/Damen, ParticipantDialog ohne Pflicht Verein/Jahrgang,
ClassesView-Hinweis „Start gesperrt“, DrawView.handicapBadge („+X“) in Gruppen/KO/Qualifikation, Tische,
Präsentation, Ergebnisdialog; Texte „ (+X)“ in Warteschlange/Spiele; PDFs (Spielplan, KO-Baum) und Spielzettel
(„+ X Punkte“, Aufschlag-Zeile bei „Partei ohne Vorsprung“). importer/csv/HandicapCsvImporter (Komma, UTF-8 strikt,
ab Zeile 2, A–D, ignorierte Zeilen). Tests: Version031Test. UI unter Xvfb geprüft.

0.30.0 (Issues #1–#11 umgesetzt, Commit „HTS 0.30.0“ mit Closes #1 … #11). DOKU AKTUALISIERT auf 0.30.0
(04.11.2026, auf Zuruf): docs/HTS-Dokumentation.md und Claude-Docs-Dokument inkl. der 0.29.0-Änderungen.
OverviewView ohne „+“-Knopf. ClassesView: Vereinsfilter nur im Doppel, „Verfügbar“ auch im Doppel, Suchfeld
pairSearch über den Doppelpaarungen (rebuildPairs filtert, Nummern bleiben, Titel „x von y“), „Partner zulosen“
neben dem Suchfeld, DrawView.installAutoScroll auf der Paar-ScrollPane. Import (WorkspaceView.onImportRead): Einzel
direkt in die Klasse (registerOnly entfällt), Doppel ohne Partner → Tournament.registerWithoutPartner (Meldung ohne
Paar, unter „Verfügbar“); drawPartners nimmt Gemeldete ohne Paar mit. DoublesPair.getAverageTtr: Partnerwert bzw. 0.
Festes Los: PlayingClass.lotSeed/lotOf (persistiert „lot-seed“, bei drawGroups/drawSwiss/createNextPhase gesetzt,
sonst aus dem Klassennamen), GroupStanding.compute/SwissSystem.standings mit ToIntFunction lot; vor dem ersten
Ergebnis bleibt die Auslosungsreihenfolge. Präsentation: PresentationPlan mit ClassState/stateOf, Station.upcoming,
isOn (Tournament.presentationOn + Off, persistiert „pres-on“/„pres-off“), ausgeloste nicht gestartete Klassen am
Ende und standardmäßig aus; PresentationView je Klasse mit Status-Chip; PresentationShow: KO in Blöcken
(DrawView.presentBracket mit blockFrom/blockSize), Gruppen in CellGrid 2×2/2×1/einzeln (presentGroupCard, Messung
über measure()), Tische in CellGrid nach Auflösung, FitPane-Seitenumbruch nur zwischen ganzen Karten/Zeilen
(pageStarts). Tests: Version030Test. UI unter Xvfb geprüft (Präsentation, Doppel-Suche).

0.29.0 (6 Punkte umgesetzt; Doku NICHT aktualisiert – Stand 0.28.0, nur auf Zuruf):
WorkspaceView: Kopfbereich (topActions rechts oben in FlowPane mit startBox, startedInfo in der Unterzeile, pageActions
nur noch bei „Teilnehmende“), Menüpunkt „Präsentationsansicht“. ClassesView.openDefaultTab (Endergebnis / letzte
Phase / Teilnehmende). InfoDialog (Icons.INFO, HtsApp.REPO_URL/openLink, Labels „Feature“ (angelegt) und „bug“
(GitHub-Standard)), LICENSE (GPL-3.0), hts-dist: LICENSE.txt, Lizenzen/LIESMICH.txt, JavaFX-und-Java-Laufzeit-LICENSE.txt.
GroupDraw.Seeding.HEADS „Gruppenkopf gesetzt“ (Köpfe fest A, B, …, Rest wie POTS, Nachbessern tauscht keine Köpfe),
Umbenennung „TTR Setzung (…)“. ModeDialog: Zeile „Vereinsgleichheit beachten:“ + Checkbox, Combos 320 px.
Präsentation: tournament/PresentationPlan (Stationen + Reihenfolge), Tournament.presentationSeconds/-Off/changeCount
(persistiert „pres-secs“, „pres-off“), ui/PresentationView (Seite), ui/PresentationShow (Fenster, Rotation, FitPane
mit Seiten, Tische/nächste 16/Endergebnis-Seiten), DrawView.forPresentation + present*-Methoden (KO max. 4 Spalten
ab erster offener Runde). Tests: Version029Test. UI unter Xvfb gerendert (Klassen, Info, Modus, Präsentation).

0.28.0 (8 Punkte umgesetzt). DOKU AKTUALISIERT auf 0.28.0 (03.11.2026, auf Zuruf): docs/HTS-Dokumentation.md und
Claude-Docs-Dokument (Abschnitt Leistung/Hardware mit Messwerten, Phasenbild auf 2 Phasen). Weitere Doku-Updates nur auf Zuruf.
PlayerFilter.matchesPerson (Vorname/Nachname/Verein, Wörter UND-verknüpft, Satzzeichen am Rand ignoriert),
Tournament.entrantMatches/matchMatches (Doppel: eine Person passt); alle Suchfelder umgestellt. DrawView: Suchfeld
auch im KO, Treffer hervorgehoben + Pfad („bracket-path-hit“), Rest „search-dim“, Enter/Umschalt+Enter (stepHit,
„search-current“), Rundentitel in titleBar mit sticky-Verschiebung, Turnierpfad durchgezogen, Platz-3-Schalter
(thirdBtn, switchGraphic), openText in der Infozeile, Hinweis „angehalten“ in followRow (Breite/Höhe in
updateBannerWidth), followBtn/nextPhaseBtn rechts; KO-Kartenkopf mit echter Spielnummer (Finale nach Platz 3).
Tournament.thirdPlaceLabel („Verlierer/in Spiel n“), KnockoutBracket „Sieger/in Spiel n“, PdfReports/KoTree.
Tische: tableName/setTableName (max. 30, persistiert als Attribute „tname.N“), TablesView Raster (GridPane, 340 px,
Spalten nach Breite, Zeilen gleich hoch), Kopfzeile Name · Klassen · Status, Umbenennen per Klick; Namen in
Spielkarten (gekürzt + Tooltip), Spiele-Ansicht, Ergebnisdialog, Spielzettel. Test: Version028Test.
UI unter Xvfb (Linux-JavaFX 11) gerendert und geprüft: KO-Baum, Suche, Tische.

0.27.0 (9 Punkte umgesetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
DrawView: installAutoScroll (DRAG_OVER-Filter + AnimationTimer am Rand der ScrollPane), buildBracket mit fester
Positionierung (Pane), Verbindungslinien und Trennlinien, statusStyle (card-upcoming/running/paused/done), followRow
oben in der linken Spalte, koCounts in summary(). Tournament: disabledTables/setTableDisabled (freeTables, moveMatch),
createQualificationPhase mit reserve, reserveFromGroups, drawFirstQualRound, drawFreeRound. PlayingClass.qualReserve;
TournamentFile „qual“ = q,k,r,n und „tables-off“. TablesView: „Tisch deaktivieren/aktivieren“. PhaseCloseDialog:
Nachrücker-Spinner. Grenze: Tisch nur ohne Spiel deaktivierbar.

0.26.0 (Qualifikationsmodus komplett ersetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
Tournament: maxQualificationRounds (aufrunden), addQualificationRound (Zusatzrunde „Qualifikation Zusatzrunde“),
advanceQualification (Gewinner weiter, nach Runde n ggf. Zusatzrunde der Verlierer), setQualExtra, qualified (direkt
nach TTR + Sieger Runde n nach Gruppenplatz/TTR), qualificationTiers/tiersWithLevel (Freilos Runde n → Zusatzrunden-
Sieger bzw. Verlierer Runde n → Zusatzrunden-Verlierer → Verlierer n-1 … 1), rankQualification (offene Spieler an
passender Stelle). PlayingClass.qualPlace/qualExtra (persistiert qual-place/qual-extra). Tests: qualifikationsmodusHem,
qualifikationsmodusFreilosInDerLetztenRunde. UI: PhaseCloseDialog „Qualifiziert nach Siegen“, DrawView Häkchen
Zusatzrunde, PDF-Hinweise.

0.25.0 (4 Punkte umgesetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
importer/clicktt/ClickTtResultImporter (fertiges Turnier aus Ergebnis-XML: Gruppen/KO inkl. Platz 3, Phase 1;
spätere Phasen als Hinweis), Tournament.importGroups/importKnockout/importResult/importFinished,
TournamentStore.importFinished, OverviewView-Knopf „Fertiges Turnier importieren (click-TT)“; Beispiel
samples/clicktt-ergebnisse-beispiel.xml (Test: Platzierungen stimmen mit der Datei überein); KO-Platz-3-Karte unter
„Finale“; MatchesView Ausrichtung (col-left/col-center); Spalte „Meldung“ entfernt.
Offen/Grenze: Import übernimmt nur Phase 1 je Konkurrenz (Phase 2 aus der Datei wird gemeldet).

0.24.0 (4 Punkte umgesetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
GroupDraw.repairClubClashes (Tausch-Nachbesserung mit gleichwertigen Tauschen, Köpfe fest, Stärkegruppen intakt;
ClubClashRepairTest), Tournament.MAX_DOUBLES_GROUP=5 (drawGroups: Doppel 1 Gruppe bzw. Fehler >5; drawSwiss: kein
Doppel; Vereinsgleichheit bei Doppeln aus), ModeDialog Doppel-Regeln, CSS .badge-doubles in ausgewählter Zeile,
Spalte „Klassen“ in „Verfügbar“ entfernt.

0.23.0 (8 gesammelte Punkte umgesetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
DrawView Zeilenklick-Toggle, ClassDialog-Sperre (Tournament.hasResults), Tournament.drawPartners/PartnerDraw +
ClassesView „Partner zulosen“, Tournament.partyLines/personLines/partyFullName, ResultDialog (Doppel mehrzeilig,
„1. Satz“, „Punkte“), PdfReports roundPlan/planRow (Gruppen), doublesMatchRow/personCell/nameWithClub,
Doppel zweizeilig in standings/writeFinal/slip, report/KoTree (Baum mit Skalierung bis 75 % bzw. Teilbäumen),
knockoutPlan (Karten), DrawView KO-Karten (cardH dynamisch, sideRow neu, TTR nur Einzel).

0.22.0 (7 gesammelte Punkte umgesetzt; Doku weiterhin NICHT aktualisiert – Stand 0.20.0, nur auf Zuruf):
DrawView COLLAPSE_LIMIT=7 + statusRank-Sortierung, openSets (Satzdetails per Klick), allGroupsLabel, Gruppen-Druckknopf
(Reports.showGroup / PdfReports.group), Endergebnis-PDF-Knopf im followRow entfernt, StorageLocations.resultsFileName.

0.21.0 (alle 9 gesammelten Features umgesetzt; Doku NICHT aktualisiert – erst auf ausdrücklichen Wunsch):
Player.registrations („gemeldet“, <registration competition>), Tournament.registerOnly beim Import neuer Einzelklassen,
ClassesView inSearch/inClubFilter/inSexFilter, DrawView groupSearch/expandedGroups/COLLAPSE_LIMIT=10/topRow-Layout,
Tournament.test (Root-Attribut test) + simulateAll/randomSets (ohne Spielzettel-Druck), Icons.DICE im Kopf,
TournamentStore.copy(CopyMode WITHOUT_DRAW|EVERYTHING|PLAYERS_ONLY), Tournament.resetToBeforeDraw/removeAllClasses,
TournamentDialog: Testturnier-Häkchen, „Kopie von“ + „Übernehmen“.
Offen: Dokumentation auf 0.21.0 bringen (nur auf Zuruf).

0.20.0 (alle 7 gesammelten Features umgesetzt): Spiel um Platz 3 (PlayingClass.thirdPlace, Match placement=3,
koRound -1, Tournament.setThirdPlace/canHaveThirdPlace/thirdPlaceMatch/resolveThirdPlace, rankKnockout), Gewinnsätze je
Klasse (PlayingClass.winningSets, Tournament.winningSets(Match)/setWinningSets(c,n), ModeDialog), seedLabels „A1“ für
Phase-2-KO, SWISS ohne Folgephase (canHaveNextPhase), Vereinsgleichheit in addQualificationRound, Ui.spiele(n),
StorageLocations + TournamentStore (Dokumente\HTS Turniere\<Name>\Turnierdatei|XML Export|PDF Export, Migration der
alten Ablage, extraTournamentFiles in Preferences, moveFile), Tournament.folder/pdfDir/xmlDir (Root-Attribute).

0.19.0: MAX_PHASES = 2. Qualifikation ohne KO-Baum: qualField (id→Gruppe, persistiert als qual-field), Runden
„Qualifikation Runde k“ (addQualificationRound/advanceQualification, Freilos in swissByes), qualificationTiers,
qualificationTierLabel, rankQualification (TTR je Stufe), rankGroups(samePlace), swapQualificationPlayers (DnD).


0.18.0: TournamentMode.QUALIFICATION (createQualificationPhase: direkt je Gruppe, KO-Quali je Gruppe, Runden;
createKnockoutMatches begrenzt Runden über koRoundsPlayed; qualified(); rankQualification), Vereinsnamen in KO.

Stand 0.17.0:

Dokumentation: docs/HTS-Dokumentation.md (Markdown, fortschreibbar; bei neuen Versionen ergänzen und den
Versionsverlauf oben in der Datei fortführen).

0.17.0: keine „…“ in Knöpfen/Menüs; Aufgabe → offene Spiele der Partei in der Phase kampflos (Match.retirementOf,
nicht exportiert; Aufgabenspiel selbst 0:3/0:11 exportiert; Korrektur nimmt das zurück; Swiss: withdrawn);
Match.exportExcluded („Export“-Haken nur auf Seite „Spiele“, nur abgeschlossene Spiele).

Stand 0.16.2:

0.16.2: Logo (resources/de/hts/ui/hts-logo.png) als Stage-Icon + Kopfleiste, Turniername oben; Speichern-Knopf und
Speicherstatus entfernt; ButtonSizing nur für Knöpfe ohne CSS-Mindestbreite (behebt Zucken); Vereinsgleichheit
nur vor Start; Tischzuordnung einheitliche Spalten; Phasen-PDF A4 quer ohne Status, breite Sätze-Spalte.

Stand 0.16.1:

0.16.1: ButtonSizing (alle Knöpfe min. = bevorzugte Breite, alle Fenster); Phasen-Reiter-Knöpfe umbenannt/verkleinert,
Knopfzeile als FlowPane. Regel des Nutzers: Knöpfe programmweit immer ausgeschrieben.

Stand 0.16.0:

0.16.0: Verein-Spalte; leerer Zwischensatz ungültig; MatchStatus.PAUSED (Phase anhalten pausiert Tischspiele,
resumeMatch einzeln, resumeClass alle); withdrawMatch; KO in angehaltener Phase frei umsortieren (rebuildFirstRound
löscht/ersetzt Spiele, setzt Folgespiele zurück); Export: Gesamtergebnis-PDF und click-TT „Ergebnisse.xml“
(ClickTtResultExporter nach poinTT-Vorbild: CTTMatch-Attribute, placement, kampflose ausgelassen, DOCTYPE
liga.nu TournamentPortal.dtd). Importdatei wird als <id>.clicktt.xml im Turnierordner abgelegt.
Offen: Export-Datei mit echtem click-TT-Upload testen.

Stand 0.15.0:

0.15.0: Vereinsgleichheit nur Phase 1; Folgephase angehalten → neu auslosen (bleibt angehalten); Pause-Knopf-Stil;
KO-DnD auch laufend (nur ungespielte Erstrundenspiele); Ergebniskorrektur mit Rücksetzen der Folgespiele
(FollowUpConflict → Bestätigung), Klassenende wird dabei aufgehoben; übernommene Endgruppenspiele synchron.

Stand 0.14.2:

0.14.2: Gruppenplatz-Rangfolge nach Spiel-/Satz-/Punktverhältnis (Anteile), Gleichstand per Los, kein TTR;
Tischkarte: Chip neben Titel, Aktionen „Ergebnis“/„Drucken“/„wechseln“ rechts; Ergebnisdialog sperrt Sätze nach
Entscheidung, Enter → Fokus auf Speichern, Enter speichert.

Stand 0.14.1:

0.14.1: DialogSizing (alle Dialoge wachsen mit dem Inhalt), TTR-Spalte in Gruppentabellen, breitere Spielliste,
KO Phase 2/3 nach Gruppenplatzierung (Freilose an beste Gruppenerste, Bester gegen Schlechtesten anderer Gruppe,
Gruppengleiche in verschiedenen Hälften).

Stand 0.14.0:

0.14.0: „Modus festlegen“-Dialog (Phase-1-Reiter erst danach), Gruppensetzung Schlange/Stärkegruppen/zufällig,
Vereinsgleichheit (Schlange unterbrechen, Stärkegruppen per Matching, KO-Tausch mit Nächstplatziertem),
Markierung, Drag & Drop (Gruppen tauschen/verschieben, KO Plätze/Paarungen tauschen), neue KO-Setzliste
(Bit-Umkehr: 1–16, 9–8, 5–12, 13–4, 3–14, 11–6, 7–10, 15–2).

Stand 0.13.0:

0.13.0: PDF (PDFBox 3.0.5) für Spielzettel/Phase/Endergebnis mit eingebautem Viewer (Speichern, Drucken),
Tische ↔ Klassen (Matrix, Dispatcher nimmt passendes späteres Spiel), Spiele verschieben/tauschen,
„Phase abschließen“-Dialog (Endergebnis oder neue Phase: Gruppen, KO, Platzierungsspiele) statt „Danach“.
Kein automatisches Klassenende mehr. Endergebnis-Reiter erst nach Klassenende.
Build: PDFBox-, fontbox-, pdfbox-io- und commons-logging-Jars aus poinTT/lib in Klassenpfad und Fat-Jar.

Stand 0.12.0:

0.12.0: Schweizer System (Phase 1, Buchholz/Feinbuchholz, Freilos = Sieg – Annahmen dem Nutzer mitgeteilt),
Endergebnis-Reiter erst nach Klassenende, Phasendruck A4 je Gruppe, automatischer A6-Spielzettel beim Aufruf
an einen Tisch (Standarddrucker, abschaltbar auf „Tische“). Druck-Layout noch nicht auf echtem Drucker geprüft.

Stand 0.11.0:

0.11.0: Phasen als Reiter (max. 3), Phase anhalten/fortsetzen, nach Gruppenphase „Danach“ wählen oder Klasse
beenden, Endergebnis-Reiter (dynamisch), Gruppenphase mit Platzierungsspielen (nur nach 2 Gruppen; bei ungleich
großen Gruppen bekommt der Übrige den letzten Platz kampflos – vom Nutzer bestätigt).

Stand 0.10.0:

Fertig: Teilnehmende (click-TT-Import, Warteliste, manuelle Erfassung), Klassen (Einzel/Doppel, Erwachsene/Damen/
Jugend/Mädchen, Jahrgänge, Max.-Teilnehmende, Drag & Drop, Doppelpaarungen), Auslosung KO (gesetzt/zufällig,
Freilose) und Gruppen (Schlange, Größe oder Anzahl), Klasse bereit/starten, Turnier starten, Warteschlange & Tische,
Satzweise Ergebniserfassung (+8/-7-Kurzform, Gültigkeitsprüfung, Aufgabe), KO-Fortschritt, Gruppentabellen
(Tiebreak Satz/Ball oder direkter Vergleich), Folgephase (KO oder weitere Gruppen, Aufsteiger je Gruppe),
Spielsuche und Ergebniskorrektur.

Offen / Ideen:
- Gewinnsätze (derzeit fest 3) beim Anlegen des Turniers wählbar machen.
- Oberfläche von 0.10 am Bildschirm prüfen (Ergebnisfenster, Gruppentabellen, „Endrunde erstellen“).

Technik: Java 21 + JavaFX 21, Paket de.hts; Turnierdateien .hts (XML, FORMAT_VERSION 6) in
<Benutzerordner>\HTS\Turniere. Abwärtskompatibilität zu älteren Versionen ist nicht nötig.
