# Zwischenstand HTS 0.38.0 (08.11.2026)

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

## Feature-Sammlung für 0.39.0 (NICHT kompilieren – bis „… kompilieren“)

(neue Punkte zusätzlich als GitHub-Issue mit Label „Feature“ anlegen)

1. ZURÜCKGESTELLT (07.11.2026, auf Wunsch des Nutzers – beim Kompilieren NICHT umsetzen): WLAN-QR-CODE AUF DEM
   AUSHANG (Issue #34): In HTS einmal WLAN-Name und Passwort eintragen; der Aushang
   „QR-Codes drucken“ bekommt einen dritten QR-Code „1. WLAN verbinden“ (Format WIFI:T:WPA;S:…;P:…;;), darunter
   „2. Live-Anzeige“ und „3. Anmeldung am Turniertag“. So bleibt das WLAN geschützt, die Teilnehmenden tippen nichts
   ab. Vorschlag (Rückfrage gestellt): Eingabe in einem kleinen Dialog beim Klick auf „QR-Codes drucken“, gespeichert
   als Programmeinstellung auf dem Laptop (gleicher Router für alle Turniere), nicht in der Turnierdatei; Name und
   Passwort zusätzlich als Text unter dem QR-Code; Verschlüsselung WPA/WPA2 (Standard); Feld leer = kein WLAN-QR-Code
   (wie bisher Felder zum Ausfüllen).

2. TEILNEHMENDE: SETZPOSITION NACH TTR (08.11.2026, Issue #62) – TEILWEISE GEKLÄRT. In der Übersicht „Teilnehmende“
   wird eine Setzposition angezeigt: Rangfolge nach TTR innerhalb der Klasse, ohne TTR ganz unten; nur wenn auf eine
   Klasse gefiltert ist und die Klasse per XML importiert wurde. Offen: gleicher TTR (Vorschlag: gleicher Setzplatz,
   z. B. 3 und 3, dann 5); Doppel (Vorschlag: Ø-TTR).

## Umgesetzt

0.38.0 (Issues #46–#61 außer #34). README „Neu in 0.38.0“.
#46 createGroupMatches: Versatz je Gruppe = maxRunden − eigene Runden. #47 Scoring.SHORT ohne Vorzeichen (auch „−“).
#48 GroupDraw.clubMatching (Lagen, Tausch ≤ Gruppenzahl − 1, Lage 1 verteilt, Vereins-Rückgängig bei mehr Gleichheiten,
danach repairClubClashes; minimalClashes als Abbruch; Entrant.clubs zwischengespeichert). #49 MatchesView Gruppenfilter,
Spalte „Gruppe“, Filterzeile als FlowPane. #50 PlayingClass.awaitingStart, Qualifikationsphase angehalten bis „Phase
starten“. #51 MatchStatus.CANCELLED, endPhaseEarly/undoEndPhaseEarly, PlayingClass.endedEarly. #52 ClassesView
deleteClass (Verlustliste, zweite Rückfrage, Entf). #53 Tournament.originLabel (DrawView, LiveServer, PdfReports).
#54 Player.withdrawnByPhone, PdfReports.withdrawnList, Knopf „Abgemeldete als PDF“. #55 Player.isReserveOnly
(PlayerTableView, Zählung, Handy, assign → PRESENT). #56 DrawView-Werkzeugzeile (tiebreakBox, printBtn als MenuButton,
endEarly/undoEnd/restart), ClassesView-Kopf einzeilig. #57 Tischzuordnung als Ausschlüsse (table-exclusions/
excluded-class, alte table-classes umgerechnet), TableAssignmentDialog „alle“ je Zeile/Spalte, DialogSizing hvalue 0.
#58 restartPhase. #59 TournamentFile liest nur direkte <class>-Kinder von <classes>, repairDuplicateClasses.
#60 qualDecision, decideQualification/undo, Zusatzrunde entfernt, ResultEntry fragt nach Runde 1, simulateAll entscheidet
„weitere Runde“. #61 qualifierPlaces, createNextPhase(c, places), placesProblem/parsePlaces, rankGroupPlaces, Dialog
mit Optionsfeldern. Geprüft bei 1093 × 614 und 911 × 512. Tests: 206 bestanden (Version038Test, PhaseFixtures).
Feature-Sammlung 0.38.0 (zur Nachverfolgung):

2. GRUPPENSPIELE: GRUPPEN MIT MEHR RUNDEN VORNE EINREIHEN (08.11.2026, Issue #46) – GEKLÄRT.
   Spiele von Gruppen mit höherer Rundenzahl (5er = 5 Runden, 4er = 3 Runden, 3er = 3 Runden, 6er = 5 Runden)
   werden vor denen kleinerer Gruppen eingereiht. Bisher: Runde 1 aller Gruppen (A, B, C …), dann Runde 2 usw.
   Bestätigt: Variante (a) „Auf gemeinsames Ende ausrichten“: Jede Gruppe beginnt um so viele Runden früher, wie sie
   mehr Runden hat als die Gruppe mit den wenigsten (z. B. 5er spielen Runde 1–2 allein vorweg, ab dann alle
   gemeinsam; alle Gruppen enden etwa gleichzeitig). Spielnummern folgen der neuen Reihenfolge. Gilt für alle
   Gruppenphasen (Gruppen, Gruppen + KO, Qualifikation, Zwischenrunde), nur innerhalb einer Klasse.

3. ERGEBNISEINGABE: SATZ OHNE VORZEICHEN GILT ALS „+“ (08.11.2026, Issue #47) – GEKLÄRT.
   Weiter „+8“ / „-8“ / „11:8“; zusätzlich „8“ = „+8“ = 11:8, „10“ = 12:10, „0“ = 11:0. Hinweistext und Platzhalter
   im Ergebnisdialog anpassen (z. B. „z. B. 11:8 oder 8“). Bestätigt: „l“ in der Nachricht = „-“.

4. GRUPPENAUSLOSUNG: UNVERMEIDBARE VEREINSGLEICHHEIT – STÄRKSTER MIT SCHWÄCHSTEM (08.11.2026, Issue #48) –
   GEKLÄRT. Hat ein Verein mehr Teilnehmende als es Gruppen gibt und ist „Vereinsgleichheit vermeiden“
   an, soll der/die Schwächste des Vereins in die Gruppe des/der Stärksten des Vereins gelost werden (der/die
   Zweitschwächste zum/zur Zweitstärksten usw.). Betrifft v. a. die Schlange: Der Rhythmus wird nur an dieser Stelle
   unterbrochen und davor und danach normal fortgesetzt.
   Bestätigt (08.11.): Beispiel 4 Gruppen, Verein X auf TTR-Platz 3, 5, 10, 13, 16 → X13 kommt in die Gruppe von X3.
   Dafür werden ggf. Betroffene in der Schlange davor oder danach verschoben; eine Verschiebung beträgt höchstens
   (Gruppenzahl − 1) Plätze.
   Bestätigt (08.11.): Es gilt immer ein Matching „Schwächster zu Stärkstem“ (Nummerierung innerhalb des Vereins nach
   TTR), Beispiel 4 Gruppen: 5 Gleiche → Nr. 5 zu Nr. 1; 6 → Nr. 6 zu Nr. 1, Nr. 5 zu Nr. 2; 7 → Nr. 7 zu Nr. 1,
   Nr. 6 zu Nr. 2, Nr. 5 zu Nr. 3 (8 → zusätzlich Nr. 5 zu Nr. 4). Annahme „immer“ = bei allen Setzungsarten.
   Bestätigt: (4) gilt NICHT für Doppel (dort bleibt es wie bisher).
   Bestätigt (5): Sollen Überzählige zweier Vereine in dieselbe Gruppe, wird zuerst geprüft, ob die maximale
   Gruppengröße das zulässt. Ja → beide in diese Gruppe, solange Schlange und höchstens (Gruppenzahl − 1) Plätze
   Verschiebung passen. Sonst wird der von Verein B (Annahme: Verein mit dem schwächeren Stärksten) eine Gruppe nach
   hinten verschoben (Annahme: in die nächste Gruppe der Schlangenrichtung, die noch Platz hat).
   Präzisiert (08.11.): Verein B = Verein mit der schwächeren Nr. 1. Stehen Nr. 1 von A und Nr. 1 von B in derselben
   Gruppe (z. B. B) und lässt sich die Vereinsgleichheit so nicht auflösen, wird Nr. 1 von Verein B in Gruppe C
   verschoben und Nr. 2 von Verein B in Gruppe B (Spielertausch); danach wird erneut geprüft, ob sich die
   Vereinsgleichheit auflösen lässt.
   Sind 3 aus einem Verein je Gruppe nötig: Nr. 1, Nr. 5 und Nr. 9 in eine Gruppe (Beispiel des Nutzers „für
   4er-Gruppen“). GEKLÄRT: Variante (a) – die Lagen wechseln die Richtung wie die Schlange; Beispiel 4 Gruppen,
   12 Gleiche: A = 1, 8, 9 · B = 2, 7, 10 · C = 3, 6, 11 · D = 4, 5, 12 (bei 9 Gleichen: Nr. 9 zu Nr. 1 und 8). Muss für Gruppengrößen 3–10 funktionieren (präzisiert
   08.11.) und mit bis zu 5 Mitgliedern desselben Vereins je Gruppe (bei Gruppen mit 5 oder mehr Mitgliedern; bei 3er-
   bzw. 4er-Gruppen entsprechend bis 3 bzw. 4). Ab Lage 3 wechselt jede weitere Lage wieder die Richtung (Lage 4
   rückwärts, Lage 5 vorwärts).
   Vorschläge: (1) Überzählige = die Schwächsten des Vereins; Ziel = Gruppe mit den wenigsten Vereinskameraden, darunter
   die mit dem/der stärksten Vereinskameraden. (2) Schlange: Tausch innerhalb derselben Schlangenrunde (der/die
   Überzählige nimmt in seiner/ihrer Runde den Platz der Zielgruppe, wer dort gelandet wäre, rückt auf seinen/ihren
   Platz); Gruppengrößen bleiben. (3) Auch bei Stärkegruppen, Gruppenkopf gesetzt und zufällig. (4) Doppel ebenso.
   (5) Konkurrieren zwei Vereine um dieselbe Gruppe, hat der stärkere Vereinskamerad Vorrang, sonst nächste Gruppe.

5. SPIELE: FILTER „GRUPPE“ UND SPALTE „GRUPPE“ (08.11.2026, Issue #49) – GEKLÄRT (Vorschläge angenommen).
   In der Übersicht „Spiele“ eine Auswahl „Gruppe:“ neben „Klasse:“; die Spaltenüberschrift „Phase“ heißt „Gruppe“.
   Vorschläge: Auswahl = „Alle Gruppen“ + alle Werte der Spalte (Gruppe A, B, … und KO-Runden wie „Viertelfinale“,
   „Qualifikation Runde 1“) der gewählten Klasse; bei „Alle Klassen“ filtert „Gruppe A“ die Gruppe A aller Klassen.
   Klassenwechsel setzt die Gruppe zurück, wenn es sie dort nicht gibt. In KO-Zeilen steht unter „Gruppe“ weiter der
   Rundenname.

6. QUALIFIKATIONSPHASE: KO-SPIELE VOR PHASENSTART PER DRAG & DROP TAUSCHEN (08.11.2026, Issue #50) – GEKLÄRT
   (Vorschläge angenommen). Die Teilnehmenden der KO-Spiele der Qualifikationsphase sollen vor dem Phasenstart per Drag & Drop
   getauscht werden können. Stand: Tauschen geht bereits in der aktuellen Runde, aber nur bei Spielen, die noch nicht
   am Tisch sind – die Folgephase wird beim Anlegen sofort gestartet und belegt freie Tische gleich.
   Vorschläge: Qualifikationsphase wird beim Anlegen ausgelost, aber noch nicht gestartet (Hinweis + Knopf „Phase
   starten“ in der Auslosungsansicht); bis dahin alle Spieler der Runde 1 inkl. Freilos tauschbar; erst „Phase
   starten“ reiht die Spiele ein. Nur für die Qualifikationsphase (andere Folgephasen starten weiter sofort).

7. PHASE VORZEITIG BEENDEN (08.11.2026, Issue #51) – GEKLÄRT (Vorschläge angenommen). Eine Phase soll vorzeitig beendet werden
   können: Alle noch offenen Spiele der Phase werden beendet und sind nicht Teil des XML-Exports; alle bisher
   gespielten Spiele werden mit exportiert. (Hinweis: Export nimmt bisher nur beendete Klassen.)
   Vorschläge: (1) Knopf „Phase vorzeitig beenden“ in der Auslosungsansicht mit Rückfrage (Anzahl offener Spiele,
   auch solche am Tisch). (2) Offene Spiele bekommen den Status „nicht gespielt“ (grau), zählen in keiner Wertung,
   Tische werden frei. (3) Die Klasse gilt damit als beendet (keine weitere Phase), Endergebnis aus dem Stand:
   Gruppen nach den gespielten Spielen, KO mit geteilten Plätzen für die offenen Runden. (4) Rücknahme möglich
   („Beenden zurücknehmen“), solange nicht exportiert bzw. keine weitere Phase angelegt.

8. KLASSE LÖSCHEN: BESTÄTIGUNG UND TASTE „ENTF“ (08.11.2026, Issue #52) – GEKLÄRT (Vorschläge angenommen). Löschen einer Klasse
   muss bestätigt werden; zusätzlich soll „Entf“ in der Klassenliste die markierte Klasse löschen (ebenfalls nur mit
   Bestätigung). Stand: Knopf „Löschen“ fragt bereits einmal Ja/Nein („Klasse … löschen? n Zuordnungen gehen
   verloren“). Vorschläge: Rückfrage mit allem, was verloren geht (Zuordnungen, Auslosung, Ergebnisse, weitere
   Phasen), Knöpfe „Löschen“ / „Abbrechen“, „Abbrechen“ als Standard (Enter löscht nicht versehentlich); bei Klassen
   mit eingetragenen Ergebnissen eine zweite Rückfrage. „Entf“ wirkt nur, wenn die Klassenliste den Fokus hat.
   Ergänzung: In der Liste der verloren gehenden Elemente steht nur, was es in der Klasse wirklich gibt (z. B. keine
   „Phase 2“, wenn es keine gibt; keine Ergebnisse, wenn keine eingetragen sind).

9. QUALIFIKATIONSPHASE: GRUPPE MIT PLATZ ANZEIGEN (08.11.2026, Issue #53) – GEKLÄRT (überall, wie vorgeschlagen). In der
   Qualifikationsphase steht statt nur des Gruppenbuchstabens auch der Platz aus der Gruppenphase davor, z. B. „A2“,
   „A3“ statt „A“. Vorschlag: überall in der Qualifikationsphase (KO-Karten, Freilos, Listen „Qualifiziert …“ /
   Nachrücker / Platzierte, Präsentationsansicht, Live-Anzeige und PDF-Ausdrucke), Spalte so breit wie nötig.

10. TEILNEHMENDE: PDF DER ENDGÜLTIG ABGEMELDETEN (08.11.2026, Issue #54) – GEKLÄRT (Vorschläge angenommen). Auf der Seite
   „Teilnehmende“ lässt sich eine PDF-Liste der endgültig Abgemeldeten erzeugen; der Knopf ist nur aktiv, wenn
   mindestens eine Person endgültig abgemeldet ist. Vorschläge: Knopf „Abgemeldete als PDF“ neben den beiden Menüs;
   Inhalt: Turniername, Datum, je Klasse (in der die Person gemeldet war) Name, Vorname, Verein, TTR, abgemeldet per
   Handy oder durch die Turnierleitung; sortiert nach Klasse, dann Name; Öffnen im PDF-Fenster wie die übrigen
   Ausdrucke (Drucken/Speichern dort).

11. NACHRÜCKER ZÄHLEN NICHT ZU DEN ANGEMELDETEN (08.11.2026, Issue #55) – GEKLÄRT (Vorschläge angenommen). Nachrücker
   (Warteliste) zählen nicht zu den Angemeldeten und müssen auch nicht ab- oder angemeldet werden.
   Vorschläge: Wer nur auf Wartelisten steht, hat keinen An-/Abmeldestatus (Spalte „Angemeldet“ leer statt
   Kästchen, keine Zeilenfarbe), zählt nicht in „… angemeldet / abgemeldet“ unten, blockiert keinen Start, erscheint
   nicht bei „Offene Personen anzeigen“ und kann sich nicht per Handy abmelden. Wird ein Nachrücker in die Klasse
   übernommen, gilt er/sie automatisch als angemeldet (wie manuell Hinzugefügte). Wer in einer Klasse gemeldet und in
   einer anderen Nachrücker ist, behält den Status für die Klasse, in der er gemeldet ist.

12. KLASSENANSICHT: KOPFBEREICH DER KLASSE KOMPAKTER (08.11.2026, Issue #56) – GEKLÄRT (Umfang eingeschränkt).
   AUSDRÜCKLICH NUR der Kopfbereich der gewählten Klasse (rechte Karte oben) wird geändert – KEINE Änderung an der
   Programm-Kopfzeile, am Seitenkopf „Klassen“, an der Klassenliste links und an der Darstellung der Gruppen.
   Umsetzen wie im vom Nutzer markierten Ausschnitt der Skizze:
   - Zeile 1: Klassenname, daneben Kurzinfo („Einzel · 66 in der Klasse · Gruppenphase“), rechts Marke „läuft“.
   - Zeile 2: Reiter („Teilnehmende der Klasse“, „Phase 1 – Gruppenübersicht“ …).
   - Werkzeugzeile (FlowPane, bricht um): Suche · Auswahl „Gleichstand: Satz-/Ballverhältnis ▾“ (statt zwei
     Optionsfeldern „Bei gleichem Spielverhältnis“) · rechts „12 Gruppen · 150 Spiele offen“ · „PDF / Drucken ▾“
     (Gruppenauswahl im Menü statt Text „alle Gruppen“) · „Phase anhalten“ · „← Auslosung verwerfen“. Das doppelte
     „Gestartet 08.11.2026, 16:52 Uhr“ in der Werkzeugzeile entfällt. Texte nicht abkürzen.

13. TISCHE DEN KLASSEN ZUORDNEN: HAKEN = DARF SPIELEN (08.11.2026, Issue #57) – GEKLÄRT (Vorschläge 1–5 angenommen).
   - Standardmäßig sind alle Klassen allen Tischen zugeordnet; das zeigen gesetzte Haken. Soll eine Klasse an einem
     Tisch NICHT spielen, wird der Haken bewusst entfernt.
   - Hat ein Tisch keinen Haken mehr, wird an ihm kein Spiel mehr eingereiht; er bleibt aber aktiv (nicht
     abgeschaltet).
   Stand: Haken = Tisch nur für diese Klasse(n), ohne Haken spielt jede Klasse (Gegenlogik).
   Vorschläge: Speicherung als „ausgeschlossene Klassen je Tisch“ → neu angelegte Klassen dürfen automatisch an allen
   Tischen spielen (Haken gesetzt); Folgephasen folgen ihrer Klasse; Knopf „alle“ je Spalte schaltet alle Haken der
   Klasse an bzw. aus, dazu je Zeile ein Knopf für den ganzen Tisch; neuer Erklärtext „Haken = Klasse darf an diesem
   Tisch spielen. Ein Tisch ohne Haken bekommt keine Spiele, bleibt aber aktiv.“; auf der Seite „Tische“ Hinweis „keine
   Klasse zugeordnet“ am Tisch. Bestehende Einschränkungen alter Turnierdateien werden umgerechnet. Nebenbei: Im
   Dialog ist der Inhalt links abgeschnitten (Hinweistext unten beginnt mit „st das …“) – wird mit behoben.

14. PHASE KOMPLETT NEU STARTEN (08.11.2026, Issue #58) – GEKLÄRT (Vorschläge 1–6 angenommen). Eine Phase lässt sich komplett neu
   starten: Auslosung bzw. Übernahme aus vorherigen Phasen bleibt bestehen, nur alle Ergebnisse werden zurückgesetzt.
   Vorschläge: (1) Knopf „Phase neu starten“ in der Werkzeugzeile der Klasse; Rückfrage mit Anzahl der Ergebnisse, die
   verloren gehen (Standard „Abbrechen“). (2) Nur möglich, solange es keine weitere Phase gibt (sonst Hinweis: erst
   die folgende Phase verwerfen). (3) Spiele an Tischen werden abgebrochen, Tische frei; alle Spiele wieder eingereiht
   in der ursprünglichen Reihenfolge (bei angehaltener Phase: geplant). (4) KO: spätere Runden wieder ohne Gegner,
   Freilose wie bei der Auslosung; Qualifikation (KO aufwärts) bzw. Schweizer System: nur Runde 1 bleibt, spätere
   Runden werden neu angelegt; Zusatzrunde entfällt. (5) Übernommene Ergebnisse aus der Vorphase bleiben; Aufgaben,
   kampflose Wertungen und „nicht relevant für Export“ werden mit zurückgesetzt. (6) Auch nach „Phase vorzeitig
   beenden“ (#51) bzw. abgeschlossener Klasse möglich – die Klasse läuft dann wieder.

15. BUG: VIELE KLASSEN MIT GLEICHEM NAMEN NACH ERNEUTEM ÖFFNEN (08.11.2026, Issue #59, Label „bug“) – URSACHE
   GEFUNDEN. Beim Laden liest TournamentFile alle <class>-Elemente der Datei (getElementsByTagName sucht in der
   ganzen Datei) – also auch die <class name=…>-Einträge der Tischzuordnung (<table-classes><table><class/>). Jede
   Tisch-Klasse-Zuordnung wird so beim Öffnen zu einer zusätzlichen leeren Klasse gleichen Namens (ohne Modus/Phase;
   die Teilnehmenden erscheinen, weil sie über den Namen zugeordnet sind). Nachgestellt: 1 Klasse, 16 Tische
   zugeordnet → nach 1. Öffnen 17, nach 2. Öffnen 33 Klassen. Da die Tischzuordnung vor den Klassen gespeichert wird,
   stehen die leeren Kopien vorn und verdecken die echte Klasse. Besteht seit Einführung der Tischzuordnung (≤ 0.28).
   Die echten Klassen samt Phasen und Ergebnissen sind in der Datei weiter vorhanden.
   Behebung: nur direkte Kinder von <classes> als Klassen lesen; Reparatur beim Öffnen beschädigter Dateien: leere
   Kopien (gleicher Name, kein Modus, nicht gestartet, keine Spiele) entfernen, echte Klasse behalten; Test dazu.
   Entschieden (08.11.): KEIN sofortiges 0.37.1 – Korrektur beim nächsten „kompilieren“ (0.38.0) mit einbauen.
   Zusätzlich: Einträge der Tischzuordnung (#57: ausgeschlossene Klassen je Tisch) unter eigenem Elementnamen
   speichern (z. B. <excluded-class>), damit keine Verwechslung mehr möglich ist; Test: Haken entfernen, wieder setzen,
   speichern, öffnen → Klassenzahl unverändert.

16. QUALIFIKATIONSPHASE: NACH RUNDE 1 FRAGEN „GEWINNER QUALIFIZIERT?“, ZUSATZRUNDE ENTFÄLLT (08.11.2026, Issue #60) –
   GEKLÄRT (Vorschläge 1–4 angenommen). Nach Ende der Runde 1 der KO-Qualifikation wird gefragt, ob die Gewinner jetzt qualifiziert
   sind. Ja → keine weitere Runde; die Verlierer der Runde 1 sind Nachrücker 1. Ranges (Rangfolge nach TTR).
   Nein → für die Gewinner eine weitere KO-Runde (bisherige Logik für 2 Runden). Höchstens 2 KO-Runden. Die
   Checkbox „Zusatzrunde für die Verlierer der Runde 2“ samt Zusatzrunde entfällt.
   Stand: Runde 2 wird bisher automatisch für die GEWINNER der Runde 1 angelegt (wer 2 Spiele gewinnt, ist
   qualifiziert). Nebenbei: Der Infotext über den Karten wird rechts abgeschnitten („… aktuellen Runde per“) – wird
   mit behoben (Umbruch).
   Vorschläge: (1) Abfrage als Dialog, sobald das letzte Ergebnis der Runde 1 eingetragen ist: „Gewinner sind
   qualifiziert“ / „Weitere KO-Runde für die Gewinner“; wird der Dialog geschlossen, bleiben beide Knöpfe in der
   Auslosungsansicht, bis entschieden ist (Phase gilt bis dahin als nicht abgeschlossen). (2) Entscheidung „Ja“ lässt
   sich zurücknehmen, solange die Klasse nicht abgeschlossen ist; „Nein“, solange in Runde 2 noch kein Ergebnis
   steht. (3) Feldgröße beim Anlegen weiter so prüfen, dass 2 Runden möglich sind. (4) Infotext: „KO-Qualifikation:
   24 Teilnehmende – nach Runde 1 wird entschieden, ob die Gewinner qualifiziert sind oder eine weitere Runde
   spielen …“.

17. KO ALS FOLGEPHASE: KONKRETE GRUPPENPLÄTZE ALS AUFSTEIGER (08.11.2026, Issue #61) – GEKLÄRT. Bei
   „KO-System“ als zweiter Phase kann man statt der Anzahl „Aufsteiger je Gruppe“ auch konkrete Gruppenplätze durch
   Komma getrennt angeben, z. B. „2,3,4“ → genau diese Plätze ziehen in die KO-Phase ein. Rangliste: Gruppenplatz,
   Spiel-, Satz-, Ballverhältnis, direkter Vergleich, Los. Sonst übliche KO-Setzung der Positionen (wie bei Doppel).
   Vorschläge: (1) Eingabe: Zahl n = Plätze 1–n (wie bisher), Liste „2,3,4“, auch Bereich „2-4“; Fehlerhinweis am
   Feld bei ungültiger Eingabe (Platz größer als die größte Gruppe, doppelt). (2) Setzpositionen nach Rangliste
   (Nr. 1 oben, Nr. 2 unten, 3/4 in die anderen Viertel …), Freilose an die Bestplatzierten; Spieler derselben Gruppe
   möglichst in verschiedene Hälften/Viertel (wie bisher). (3) Direkter Vergleich nur, wenn beide in derselben
   Gruppe gespielt haben (sonst gleich weiter mit Los). (4) Kennzeichnung auf den KO-Karten „B3“ usw. (5) Nur für KO
   als Folgephase (Gruppen-/Platzierungs-/Qualifikationsphase unverändert); Endergebnis: Nicht-Aufsteiger behalten ihre
   Gruppenplätze.
   Bestätigt (08.11.): (2)–(5) passen. (1) geändert: ENTWEDER–ODER – entweder wie bisher die Besten je Gruppe
   („Top n“, Zahl) ODER konkrete Gruppenplätze (durch Komma getrennt, z. B. „2,3,4“). Umsetzung: zwei Optionsfelder
   „Die Besten je Gruppe: [n]“ / „Bestimmte Gruppenplätze: [2,3,4]“, jeweils nur das gewählte Feld aktiv;
   Fehlerhinweis am Feld bei ungültigen Plätzen.

0.37.0 (Issues #42–#45; #34 weiter zurückgestellt). README „Neu in 0.37.0“.
#42: PlayerTableView ohne „Zurücksetzen“.
#43: WorkspaceView Page.MOBILE „Mobile Einstellungen“ (mobilePage: Zustand, Knöpfe als FlowPane, qrPreview mit
Adresse, Hinweise; in ScrollPane); Teilnehmenden-Seite nur noch attendanceMenu.
#44: PlayerTableView clubFilter („Verein:“, updateClubs) statt Statusauswahl; showClass(name) setzt Filter zurück und
wählt die Klasse.
#45: ClassesView lockChip „Start gesperrt“ im Klassenkopf, bottomBar mit lockHint (Text + Hyperlink „Offene Personen
anzeigen“ → setOnShowOpen → Teilnehmende mit Klassenfilter) links neben modeBtn; dndHint und rote Kopfzeile entfernt;
CSS chip-locked, lock-hint, lock-hint-link. Geprüft bei 1093 × 614.
Tests: 191 bestanden.
Feature-Sammlung 0.37.0 (zur Nachverfolgung):

2. TEILNEHMENDE: KNOPF „ZURÜCKSETZEN“ DER FILTER ENTFERNEN (07.11.2026, Issue #42): In der Suchleiste der
   Teilnehmenden-Übersicht entfällt der Knopf „Zurücksetzen“; die Filter werden einzeln zurückgestellt.

3. NEUER MENÜPUNKT „MOBILE EINSTELLUNGEN“ (07.11.2026, Issue #43) – GEKLÄRT (Vorschläge angenommen). „Handy-Zugang starten“, „QR-Codes
   drucken“ und „Anmeldung per Adresse sperren“ wandern von der Seite „Teilnehmende“ auf einen eigenen Menüpunkt
   „Mobile Einstellungen“. Bestätigt: Menüpunkt rechts neben „Präsentationsansicht“; Seite zeigt
   die drei Knöpfe, den Zustand (läuft/aus, Anmeldung offen/gesperrt), die Adressen der Live-Anzeige und der
   Anmeldung samt QR-Codes zur Vorschau und kurze Hinweise (WLAN, Firewall, Gäste-WLAN). Auf „Teilnehmende“ bleiben
   „Aktion bezüglich Markierte“ und „Handyabmeldungen“.

4. TEILNEHMENDE: STATUSFILTER DURCH VEREINSFILTER ERSETZEN (07.11.2026, Issue #44) – GEKLÄRT. In der Suchleiste der
   Teilnehmenden-Übersicht entfällt die Auswahl „Status:“; an ihrer Stelle eine Auswahl „Verein:“ (Alle + alle Vereine
   der Teilnehmenden, alphabetisch), kombinierbar mit den übrigen Filtern und dem Klassenfilter. Keine Sortier-Änderung.

5. KLASSEN: STARTSPERRE-HINWEIS STATT GRAUEM BEDIENHINWEIS (07.11.2026, Issue #45) – GEKLÄRT.
   - Knopf „Teilnehmende fertig – Modus festlegen“ bleibt, wo und wie er ist (rechts unten).
   - Der graue Bedienhinweis („Teilnehmende per Drag & Drop … In der Klasse = angemeldet, Verfügbar = nicht
     angemeldet.“) ENTFÄLLT ganz.
   - An seiner Stelle links neben dem Knopf (volle verfügbare Breite, bricht notfalls um) der rote Hinweis
     „XX Gemeldete noch nicht an- oder abgemeldet. Offene Personen anzeigen“ (Link → „Teilnehmende“ mit dieser Klasse
     im Klassenfilter); verschwindet, wenn alle an- oder abgemeldet sind.
   - Die rote Zeile unter dem Klassenkopf entfällt; keine Schrittleiste. Die Marke „Start gesperrt“ oben rechts im
     Klassenkopf BLEIBT bzw. kommt dorthin (wie im Designvorschlag), solange der Start gesperrt ist.

0.36.0 (Issues #35–#39, #41; #40 entfernt, #34 zurückgestellt). README „Neu in 0.36.0“, CLAUDE.md-Regel „Texte nie
abkürzen“.
#35: Ui.textWidth/fitColumns (Spalten nach Inhalt, UNCONSTRAINED + Restbreite letzte Spalte, „fit-min“/
„fit-header-only“) in PlayerTableView, ClassesView (Verfügbar/In der Klasse), MatchesView, FinalResultView,
ImportPreviewDialog; DrawView matchWidth dynamisch (koCardWidth, Qualifikation nach längstem Namen), Verein/Namen
min = pref, Gruppenspiele Namensbreite nach längstem Namen, Gruppentabelle ohne maxWidth; DialogSizing: Inhalt in
ScrollPane, einzeilige Labels min = pref, Fenster auf Bildschirm begrenzt, Mindestgröße 320×220; WorkspaceView Seite in
ScrollPane (pageContent min 380 hoch), pageActions FlowPane; AppHeader Menü als FlowPane, Schriftzug unter 1200 px
ausgeblendet, Turniername bricht um; ClassesView Klassenliste umbrechend (Zellbreite 0), Zeilen als FlowPane,
Teilnehmende-Reiter waagerecht scrollbar (min 860); DrawView Zusammenfassung umbrechend. Geprüft mit Bildschirmfotos
bei 911 × 512 und 1093 × 614.
#36: WorkspaceView attendanceMenu (zwei MenuButtons), withdrawSelected, Kontextmenü dynamisch je Status.
#37: Ui.nullsLastBothWays (TTR in Teilnehmenden- und Klassentabellen).
#38: PlayerTableView Klassenfilter (setClassSource aus WorkspaceView; isEnteredFor oder Warteliste).
#39: ImportPreviewDialog Spalte „Importieren“ (CheckBoxTableCell), getSelectedCompetitions, keeps(); WorkspaceView
filtert Personen/Konkurrenzen/Paare vor dem Import.
#41: rankQualification – Bereich für direkt bzw. über die KO Qualifizierte; FinalResultView grau nur für offene Bereiche.
Tests: 191 bestanden (bestehende Tests decken die geteilten Plätze mit ab).
Feature-Sammlung 0.36.0 (zur Nachverfolgung):

   1. ZURÜCKGESTELLT (07.11.2026, auf Wunsch des Nutzers – beim Kompilieren NICHT umsetzen): WLAN-QR-CODE AUF DEM
      AUSHANG (Issue #34): In HTS einmal WLAN-Name und Passwort eintragen; der Aushang
      „QR-Codes drucken“ bekommt einen dritten QR-Code „1. WLAN verbinden“ (Format WIFI:T:WPA;S:…;P:…;;), darunter
      „2. Live-Anzeige“ und „3. Anmeldung am Turniertag“. So bleibt das WLAN geschützt, die Teilnehmenden tippen nichts
      ab. Vorschlag (Rückfrage gestellt): Eingabe in einem kleinen Dialog beim Klick auf „QR-Codes drucken“, gespeichert
      als Programmeinstellung auf dem Laptop (gleicher Router für alle Turniere), nicht in der Turnierdatei; Name und
      Passwort zusätzlich als Text unter dem QR-Code; Verschlüsselung WPA/WPA2 (Standard); Feld leer = kein WLAN-QR-Code
      (wie bisher Felder zum Ausfüllen).

   2. HTS AN BILDSCHIRMAUFLÖSUNGEN ANPASSEN – TEXTE NICHT ABKÜRZEN (07.11.2026, Issue #35) – GEKLÄRT. Grundprinzip des
      Nutzers: Texte werden nicht mit „…“ abgekürzt; Knöpfe und Texte notfalls in eine zweite Zeile umbrechen oder in der
      Größe skalieren. Anlass: click-TT-Import-Dialog („Teilnehmende in d…“, „Neue Einzelklassen: 4 (die Gemeldeten …ge…“).
      Gilt generell für alle Dialoge und Ansichten, auch bei kleineren Auflösungen. Als DAUERHAFTE REGEL für künftige
      Änderungen festhalten (CLAUDE.md). GEKLÄRT (07.11.2026):
      - Prüfen bei 1366 × 768 mit Windows-Skalierung 125 % und 150 % (alle Dialoge und Seiten).
      - Tabellenzellen VOLLSTÄNDIG anzeigen (kein „…“): Spalten so breit wie der Inhalt, notfalls waagerechte
        Scrollleiste.
      - KO-Karten (und Spielkarten der Qualifikation) VOLLSTÄNDIG: Namen nicht abschneiden – Karten so breit wie nötig,
        notfalls scrollt der KO-Baum.

   3. TEILNEHMENDE: ANWESENHEITS-MENÜS NEU AUFTEILEN (07.11.2026, Issue #36) – GEKLÄRT.
      - Dropdown 1, Beschriftung „Aktion bezüglich Markierte“ (statt „Anwesenheit“): angemeldet (anwesend), endgültig
        abmelden.
      - Dropdown 2 „Handyabmeldungen“: „alle bestätigen“, „Markierte zurück auf offen“, „Alle zurück auf offen“.
      - Rechtsklick auf eine Zeile: zusätzlich „endgültig abmelden“.
      GEKLÄRT (07.11.2026): „zurück auf offen“ unter „Handyabmeldungen“ betrifft nur unbestätigte Handy-Abmeldungen
      (orange); „Markierte: Abmeldung zurücknehmen“ (für endgültig Abgemeldete, gelb) im Dropdown 1; Rechtsklick
      zusätzlich „angemeldet (anwesend)“, bei gelben Zeilen „Abmeldung zurücknehmen“, bei orangen „Abmeldung bestätigen“.

   4. SORTIERUNG: OHNE TTR IMMER UNTEN (07.11.2026, Issue #37, Fehler): Beim absteigenden Sortieren nach TTR stehen
      Personen ohne TTR oben (Comparator nullsLast wird beim Umkehren zu nullsFirst). Personen ohne TTR stehen künftig
      in beiden Sortierrichtungen unten – in allen Tabellen mit TTR-Spalte (Teilnehmende, Klassen „Verfügbar“/„In der
      Klasse“, ggf. Import-Vorschau).

   5. TEILNEHMENDE: KLASSENFILTER (07.11.2026, Issue #38) – GEKLÄRT (Nachrücker werden mit angezeigt). In der Suchleiste der Teilnehmenden-Übersicht
      eine Auswahl „Klasse:“ (Alle + jede Klasse). Bestätigt: zeigt alle Personen mit Bezug zur
      Klasse – gemeldet, in der Klasse oder Nachrücker; kombinierbar mit Status (z. B. Klasse + „offen“ = wer fehlt
      noch); „Zurücksetzen“ setzt auch die Klasse zurück.

   6. CLICK-TT-IMPORT: KLASSEN ZUM IMPORT AUSWÄHLEN (07.11.2026, Issue #39) – GEKLÄRT (alle 5 Vorschläge bestätigt). Im Import-Dialog je Konkurrenz
      ein Haken (Standard: alle an). Abgewählte Klassen werden nicht aus dem XML importiert. Bestätigt: Personen, die nur in abgewählten Klassen gemeldet sind, werden gar nicht übernommen; Personen mit
      weiteren Meldungen kommen ohne die Meldung/Warteliste der abgewählten Klasse; die Zahlen oben (Teilnehmende, neu,
      neue Klassen) aktualisieren sich beim An-/Abwählen; „Importieren“ gesperrt, wenn nichts ausgewählt ist; bereits
      vorhandene Klassen bleiben unberührt (Abwählen löscht nichts).

   7. ENTFERNT (07.11.2026, auf Wunsch des Nutzers): Nummerierung nach TTR je Klasse (Issue #40, geschlossen) – NICHT umsetzen.

   8. ENDERGEBNIS QUALIFIKATION: GETEILTE PLÄTZE (07.11.2026, Issue #41) – GEKLÄRT. Die über die Gruppenplatzierung
      Qualifizierten teilen sich die Plätze (z. B. 1.–16.), die Gewinner der KO-Runde teilen sich die Folgeplätze
      (z. B. 17.–22.). Bestätigt: Anzeige als Bereich „1.–16.“ in jeder Zeile (wie bei KO
      „5.–8.“), Reihenfolge innerhalb unverändert (Direkte nach TTR, KO nach Feldreihenfolge); Nachrücker behalten
      Einzelplätze (Reihenfolge = Nachrücker-Rangfolge); übrige Gruppenplatzierte wie bisher; gilt auch im PDF und auf der Handy-Ergebnisseite.


0.35.0 (Issues #31–#33). README „Neu in 0.35.0“, docs/ABHAENGIGKEITEN.md (Angriffsfläche Handy-Zugang) ergänzt.
Modell: player/Attendance (UNKNOWN, PRESENT, WITHDRAW_REQUESTED, WITHDRAWN; persistiert „attendance“), Player.isReserve
(= Warteliste), isEnteredFor, registeredPartners (Element „partner“), entriesToRegistrations; PlayingClass.importedFromXml
(„xml-import“); Tournament: checkinLocked/-Token (Ordner-Attribute „checkin-locked“, „checkin-token“), isCheckinOpen
(nicht gesperrt und nicht alle Klassen beendet), setAttendance, checkinFromPhone, enteredPlayers, openAttendance,
attendanceProblem (startClass gesperrt; StartReport.attendanceOpen), takeOverPresentEntries, mergeImportedEntries
(Import: Meldungen statt Klasse, Partnerverknüpfung), migrateToAttendanceModel (Dateien ohne model="35" → „legacy“),
replaceParticipants (CSV) = angemeldet, Kopien setzen Anwesenheit zurück.
UI: PlayerTableView (Spalte „Angemeldet“ als Checkbox bzw. „abgemeldet“/„Abmeldung ?“, Spalte „Nachrücker“, Zeilen gelb/
orange, Zähler), PlayerFilter-Status neu (angemeldet/offen/Abmeldung bestätigen/abgemeldet/Nachrücker); WorkspaceView
(Menü „Anwesenheit“, „Handy-Zugang starten/beenden“, „QR-Codes drucken“, „Anmeldung per Adresse sperren/öffnen“,
pageActions als FlowPane, Server-Stopp in prepareLeave, Import über mergeImportedEntries, neue Klassen „aus XML
importiert“, Startdialog nennt gesperrte Klassen); ClassesView („Verfügbar“ nur Anwesende, Knopf „Alle anwesenden
Gemeldeten übernehmen“, Gemeldete fett, Nachrücker-Hinweis, „⚠ Start gesperrt“ im Klassenkopf, neue Person =
angemeldet); DrawView (Startsperre mit Hinweis).
Web: de.hts.web.LiveServer (com.sun.net.httpserver, Port 8080, Zugriffe über Platform::runLater; /live, /live/tische,
/live/klasse, /live/ergebnis, /live/suche, /live/ich, /live/spieler.json; /anmeldung/<Schlüssel>, daten.json, melden),
de.hts.web.QrCode (eigener QR-Erzeuger, Byte-Modus, Level M, Version 1–10; mit OpenCV gegengeprüft),
PdfReports.qrSheet (A4-Aushang). Handy-Seiten mit Chromium (390 px) geprüft.
Tests: Version035Test (8, u. a. HTTP-Test des Servers) – 191 bestanden.
Feature-Sammlung 0.35.0 (zur Nachverfolgung):

   1. LIVE-ANZEIGE FÜRS HANDY ÜBER LOKALES WLAN (07.11.2026, Issue #31) – IN KLÄRUNG. HTS startet auf Knopfdruck einen
      kleinen Webserver (Java-eigener HttpServer, keine neue Bibliothek), fester Port (z. B. 8080), Adresse + QR-Code in
      HTS. Handygerechte Seiten (kein Bild der Präsentation), Selbst-Aktualisierung ca. alle 10 s: Tische und laufende
      Spiele, Gruppen, KO-Baum, Ergebnisse, „Meine Spiele“ bzw. „Meine Gruppe“ (Namenssuche). Hardware vor Ort: Vodafone
      EasyBox 805 als WLAN ohne Internet (Haupt-WLAN, nicht Gäste-WLAN; Laptop per Kabel, feste Adresse per DHCP).
      Sicherheitsbewertung in docs/ABHAENGIGKEITEN.md anpassen (erstmals offener Port, nur im lokalen Netz).
   2. ANWESENHEIT AM TURNIERTAG („ANGEMELDET“) + SELBST-ANMELDUNG PER HANDY (07.11.2026, Issue #32) – GEKLÄRT.
      - Teilnehmenden-Übersicht: Spalte „Angemeldet“ bedeutet künftig „am Turniertag anwesend“ (abhakbar); bisher hieß
        sie „in mindestens einer Klasse“. Nachrücker (Warteliste) bekommen ein eigenes Merkmal „Nachrücker“.
      - Anmeldung über eine feste lokale Adresse (gleich für alle, Server wie Punkt 1). Erste Auswahl: „Teilnehmende/r“
        oder „Betreuende/r“. Teilnehmende: eine Person suchen (Vorname, Nachname, Verein – ein Suchfeld wie in HTS),
        auswählen, Haken „angemeldet“, abschicken – nur sich selbst. Betreuende: Verein wählen → alle Spieler des Vereins
        im Turnier, Haken für alle Anwesenden setzen. Alternativ mehrere auswählen und „abmelden“.
      - Ergebnis sichtbar in der Teilnehmenden-Übersicht.
      Antworten (07.11.2026):
      (A) Keine PIN – der Zugang zur Anmeldeseite erfolgt über den ausgedruckten QR-Code.
      (B) Abmelden = aus der Liste der Anwesenden „nicht anwesend“ markieren → endgültig abgemeldet. In HTS werden
          endgültig abgemeldete Personen in der Teilnehmenden-Übersicht GELB hervorgehoben; die Turnierleitung kann
          ebenfalls Teilnehmende markieren und endgültig abmelden. Beim Turnierstart (auch: Start der ersten Klasse) Hinweis,
          wenn in Klassen mit Merkmal „aus XML importiert“ noch nicht alle Teilnehmenden an- oder abgemeldet sind. Klassen
          bekommen das Merkmal „aus XML importiert“, wenn sie durch den Import angelegt wurden.
      (C) Anmeldung per Adresse möglich, bis alle Klassen des Turniers beendet sind oder die Turnierleitung sie sperrt
          (Knopf unter „Teilnehmende“ in HTS).
      (D) Unter „Verfügbar“: alle Anwesenden, die die Klassenkriterien (Alter, Geschlecht) erfüllen – wie heute. Im
          Fehlerfall kann die Turnierleitung Teilnehmende von Hand hinzufügen (nicht in der XML); diese gelten sofort als
          „angemeldet“.
      (F) Merkmal „Nachrücker“ = im XML auf der Warteliste einer Klasse; eigene Spalte/Kennzeichen in der
          Teilnehmenden-Übersicht und unter „Verfügbar“; NICHT von Hand setzbar.
      (G) Vorhandene Turnierdateien: nur Klassen, die noch nicht ausgelost sind, bekommen die neue Logik („In der Klasse“
          = 0); ausgeloste/gestartete Klassen bleiben unverändert.
      (H) „Meine Spiele“/„Meine Gruppe“: eigenen Namen suchen, das Handy merkt sich die Auswahl (beim nächsten Öffnen
          direkt eigene Spiele, Tisch, Gruppe).
      (1) Anmeldeseite über Adresse mit zufälligem Schlüssel im QR-Code; Lesesicht mit eigener freier Adresse; Druckblatt
          in HTS mit beiden QR-Codes und WLAN-Zugang.
      (2) Per Handy getätigte Abmeldungen muss die Turnierleitung in HTS BESTÄTIGEN (erst dann endgültig abgemeldet/gelb).
      (3) Knopf unter „Teilnehmende“: „Anmeldung per Adresse sperren“ ↔ „wieder öffnen“.
      (4) START GESPERRT, solange nicht alle laut XML für die Klasse gemeldeten Teilnehmenden (Klassen „aus XML
          importiert“) angemeldet oder endgültig abgemeldet sind.
      (E) Knopf „Alle anwesenden Gemeldeten übernehmen“ in der Klasse; alle anderen per Drag & Drop.
      Annahmen: unbestätigte Handy-Abmeldungen zählen als offen (Kennzeichen „Abmeldung bestätigen“); Nachrücker laut
      XML blockieren den Start nicht. BESTÄTIGT: Sperre je Klasse – NUR für Klassen „aus XML importiert“ (von Hand
      angelegte Klassen nie gesperrt); „Turnier starten“ startet die übrigen Klassen und nennt die offenen Personen.
   3. KLASSEN: „VERFÜGBAR“ NUR ANWESENDE, „IN DER KLASSE“ ANFANGS LEER (07.11.2026, Issue #33) – IN KLÄRUNG. Unter
      „Teilnehmende der Klasse“ stehen unter „Verfügbar“ nur die am Turniertag angemeldeten (anwesenden) Personen;
      „In der Klasse“ beginnt bei 0 – auch nach dem click-TT-XML-Import (Meldungen landen nicht mehr direkt in der Klasse).


0.34.0 (Issues #29, #30). README „Neu in 0.34.0“ ergänzt, sonst Doku nicht aktualisiert.
#30: PlayingClass.qualKoTotal (0 = je Gruppe) und qualKoCounts (KO-Teilnehmende je Gruppe, an der Phase; persistiert
„qual-ko-total“, „qual-ko-counts“); Tournament.createQualificationPhase(c, direct, ko, koTotal, reserve, random),
qualificationAvailable; Auswahl im Modus „insgesamt“ platzweise, letzter Platz nur teilweise: Auswahl nach Spiel-,
Satz-, Punktverhältnis + Los (tierOrder ohne TTR), Feldreihenfolge mit TTR; qualSkip je Gruppe für Nachrücker ohne
Spiel (ab erstem Nicht-KO-Teilnehmenden) und für das Endergebnis (rankGroups mit Skip je Gruppe – übrige Dritte nur
„Gruppe X, Platz 3“). PhaseCloseDialog: Auswahl „je Gruppe/insgesamt“, Feld rot (CSS .field-error) + Text darunter,
„Phase abschließen“ gesperrt. Zusammenfassung „24 insgesamt in der KO-Qualifikation“.
#29: overviewBox mindestens so breit wie der Sichtbereich; Qualifikationskarten über fillRows gleichmäßig verteilt
(Mindestbreite 330), Name kürzbar, TTR/Sätze und Tischname nie abgeschnitten.
Tests: Version034Test (HEM männlich 16 Gruppen/24 insgesamt, Nachrücker je Gruppe, HEM weiblich) – 183 bestanden.
Feature-Sammlung 0.34.0 (zur Nachverfolgung):

   1. QUALIFIKATIONSANSICHT: GANZE BREITE NUTZEN (06.11.2026, Issue #29): Die Spielkarten der Qualifikationsrunden
      brechen schon nach 3 Karten um (rechts bleibt viel Platz frei), Namen/Vereine und TTR werden abgeschnitten
      („SG Farmsen-Bramf…“, „1…“). Ursache: Die Ansicht ist nur so breit wie ihr breitester Inhalt (Liste mit
      Mindestbreite 980), nicht wie das Fenster. Umsetzung: Inhalt an die sichtbare Breite binden, Kartenreihen und
      Listen über die volle Breite; Vorschlag: Karten wachsen gleichmäßig mit (mehr Karten je Reihe auf breiten
      Bildschirmen, längere Namen sichtbar), TTR nie abschneiden. GEKLÄRT: Vorschlag angenommen („passt so“).

   2. KO-QUALIFIKATION: ANZAHL INSGESAMT STATT JE GRUPPE (06.11.2026, Issue #30) – GEKLÄRT. Wunsch: Für die KO-Runde
      wahlweise „Plätze je Gruppe“ (wie bisher) oder „Anzahl der Topplatzierten insgesamt“ angeben. Beispiel: 16
      Gruppen, Gruppenerste direkt qualifiziert, 24 Topplatzierte in die KO-Runde → alle 16 Gruppenzweiten und die
      8 besten Gruppendritten. Rangfolge über die Gruppen hinweg: Spiel-, Satz-, Punktverhältnis, dann Los.
      Antworten (06.11.2026): (1) Einstellung im Dialog „Phase abschließen“ bei „In die KO-Qualifikation“: Auswahl
      „je Gruppe“/„insgesamt“ + Zahl; nur für die KO-Runde. (2) Auswahl der KO-Teilnehmenden bei völligem Gleichstand
      per Los; Nachrücker-Reihenfolge Rang 1 bleibt beim TTR. (3) Geht die Zahl nicht auf: nächste Phase kann nicht
      angelegt werden – das Feld wird optisch hervorgehoben (rot umrandet) und ein Text erklärt, warum. (4) Die nicht
      ausgewählten Gruppendritten (im Beispiel 8) sind weder Nachrücker noch qualifiziert – nur „Platzierte“ (Platz
      nach Gruppenplatzierung wie alle anderen).
      (5) Übrige Regeln unverändert. (6) Nachrücker ohne Spiel: Steht „Nachrücker je Gruppe“ auf 0, sind die nicht
      ausgewählten Dritten nur Platzierte. Steht dort eine Zahl n, gilt sie je Gruppe ab dem ersten Nicht-KO-Teilnehmenden
      dieser Gruppe: Gruppe, deren Dritter nicht in der KO ist → Dritter (und ggf. Folgende) sind Nachrücker; Gruppe, deren
      Dritter in der KO ist → ab dem Vierten. GEKLÄRT.

      PRÜFUNG HEM-MODI (06.11.2026, vom Nutzer bestätigt): Männlich (16 Gruppen: Erste direkt, 24 insgesamt in die KO =
      alle Zweiten + 8 beste Dritte, 2 KO-Runden, Verlierer Runde 2 = Nachrücker 1. Ranges (6), Verlierer Runde 1 =
      Nachrücker 2. Ranges, übrige Dritte nur Platzierte) und Weiblich (4 Gruppen: Plätze 1–3 direkt, 4./5. Nachrücker
      ohne Spiel) sind mit 0.33.0 + #30 abbildbar. Bestätigte Annahmen: Runde 1 mit Feldhälften (8 der 12 besten
      Zweiten gegen die Dritten, übrige 4 gegen die 4 schwächsten Zweiten); Rang 1 nach Gruppenplatz, Verhältnissen,
      TTR; Damen-Ersatzplätze alle Vierten vor allen Fünften, innerhalb nach TTR.


0.33.0 (Issues #21–#26, #28 umgesetzt; #27 war ein Duplikat). README „Neu in 0.33.0“ ergänzt, sonst Doku NICHT
aktualisiert (nur auf Zuruf). Offene Frage B (ungerade Zahl in der Zusatzrunde) ohne Antwort → wie angekündigt
umgesetzt: Freilos für den Bestplatzierten nach der Gruppenphase, zählt als Sieg (qualifiziert).
Qualifikation (Tournament): QUAL_WINS = 2 fest (Spinner im PhaseCloseDialog entfernt), qualificationKoCount,
qualificationFieldFits (n = 0 oder n ≥ 3 mit n mod 4 ∈ {0, 3}), qualificationFieldProblem (Dialog sperrt „Phase
abschließen“ mit Hinweis); tier() mit TTR als letztem Kriterium vor dem Los (= Feldreihenfolge); Runde 1
drawFirstQualRound: größtmögliche Zahl „höher gegen niedriger“ (Kuhn-Matching), Rest gleicher Ränge untereinander,
Gruppe/Verein als nachrangige Bedingungen; qualifiedDirect / qualifiedViaKo (Sieger Runde 2 + Zusatzrunde inkl.
Freilos) / qualified; qualificationTiers dynamisch: Verlierer Runde 2 bzw. Zusatzrunde (Feldreihenfolge), Verlierer
Runde 1 (TTR), Nachrücker aus der Gruppenphase; Labels „Nachrücker n. Ranges“, QUALIFIED_BY_GROUP/QUALIFIED_BY_KO;
Seed-Labels „A1“ für die Qualifikationsphase + qualificationPlaceLabels; entrantIndex öffentlich.
UI: DrawView.buildQualification nutzt entrantIndex (Namen in Spielkarten, #23), qualList (2 Spalten, TextFlow,
CSS .qual-*), Zusatzrunde-Checkbox gesperrt bei nur 1 Verlierer; Zusammenfassung „keine KO-Qualifikation“;
PdfReports Qualifikation mit qualifiedDirect/qualifiedViaKo. Spaltentitel: CSS Kopfzeilen CENTER_LEFT, col-center/
col-right für zentrierte/rechtsbündige Spalten (ClassesView, ImportPreviewDialog, PlayerTableView).
Zurück (#22): Tournament.hasPhaseResult/stepBackProblem/matchesAtTables/stepBack; DrawView „Auslosung verwerfen“
(Phase 1, auch nach Start) und „Zurück zu Phase 1“ (Phase 2) mit Tooltip-Hülle, Abfrage [Verwerfen]/[Zurück].
Suche (#21): PauseTransition 250 ms; KoSearch/GroupSearch merken Knoten beim Aufbau, applyKoSearch/applyGroupSearch
schalten nur Stile/Sichtbarkeit (Gruppen werden immer vollständig aufgebaut, eingeklappte Spiele nur ausgeblendet).
Messung (Software-Rendering, 2 Kerne): KO 256 Doppel – Tastendruck Ø 20 ms, Auswertung Ø 60 ms (max 136; vorher
Ø 520, max 880 ms je Tastendruck); Gruppen 512 Einzel/128 Gruppen – Tastendruck Ø 53 ms (davon fast alles der
erzwungene Layout-Durchlauf der Messung), Auswertung Ø 87 ms (max 143).
Tests: Version033Test (6), PhasesTest/Version020/021 an die neuen Regeln angepasst – 180 bestanden.
Feature-Sammlung 0.33.0 (zur Nachverfolgung):

   1. SUCHE IM KO-BAUM/GRUPPEN SCHNELLER (06.11.2026, Issue #21). Messung (256 Doppel/512 Spieler, 255 Spiele, Messumgebung
      2 Kerne 2,1 GHz, Software-Rendering): KO-Baum erstmals 1,4 s; je Tastendruck 345–880 ms (Ø 520 ms), davon reine
      Treffersuche nur ca. 3 ms – Ursache: DrawView.show() baut bei jedem Tastendruck alles neu auf.
      a) Verzögert suchen: erst ca. 0,25 s nach dem letzten Tastendruck auswerten (Debounce).
      b) Nur hervorheben statt neu aufbauen: Baum/Gruppen bleiben stehen, nur Treffer-/Abblend-Stile (und Pfade) werden
         umgeschaltet; Ziel deutlich unter 50 ms je Tastendruck. Gilt für KO-Baum und Gruppenansicht. Nach der Umsetzung
         erneut messen.

   2. ZURÜCKSPRINGEN, SOLANGE KEIN SPIEL DER PHASE ABGESCHLOSSEN IST (06.11.2026, Issue #22): „Wenn noch kein Spiel einer
      Phase abgeschlossen ist, kann auf ‚Teilnehmende der Klasse‘ oder Phase 1 zurückgesprungen werden.“
      GEKLÄRT (Vorschläge angenommen):
      - Phase 1 (auch schon gestartet) ohne eingetragenes Ergebnis → Knopf „Auslosung verwerfen“ im Reiter der Phase:
        zurück zu „Teilnehmende der Klasse“, Spiele/Tische/Warteschlange der Klasse entfernen. Freilose zählen nicht.
      - Phase 2 ohne eingetragenes Ergebnis → Knopf „Zurück zu Phase 1“ im Reiter der Phase 2: Phase 2 löschen,
        Phase 1 wieder offen (nicht abgeschlossen, Ergebnisse korrigierbar).
      - Liegt ein Ergebnis vor: Knopf sichtbar, aber ausgegraut, Tooltip „Es liegt bereits ein Ergebnis vor.“
      - Sicherheitsabfrage, z. B. „Auslosung von ‚Herren A‘ verwerfen? Alle Spiele dieser Phase werden gelöscht.“
        [Verwerfen] [Zurück]; laufen Spiele an Tischen, steht zusätzlich „n Spiele laufen gerade an Tischen.“
      - Spiele an Tischen/aufgerufen werden ohne weitere Nachfrage zurückgezogen, Tische sofort frei.
      - Es gibt höchstens 2 Phasen. Ergebnisse der Zusatzrunde (Qualifikation) zählen als Ergebnis.
      - Ausdrücklich auch für Phasen OHNE Spiele (z. B. Qualifikation mit 0 je Gruppe in der KO, Screenshot 06.11.2026):
        „Zurück zu Phase 1“ steht dort im Reiter Phase 2 neben „PDF / Drucken“ / „Phase abschließen“.

   3. QUALIFIKATIONSANSICHT ZEIGT KEINE NAMEN (06.11.2026, Issue #23, Fehler): In Phase 2 – Qualifikation stehen in den
      Spielkarten nur „–“ statt der Teilnehmenden; erst der Doppelklick (Ergebnisdialog) zeigt sie. Gewünscht: Namen
      auch in der Phasenansicht. Ursache: `DrawView.buildQualification` sucht die Teilnehmenden nur unter den der
      Phasenklasse zugeordneten Personen (`entrantsOf`), der Ergebnisdialog dagegen über alle Personen/Doppel
      (`entrantIndex`). Behebung: dieselbe Suche wie der Dialog verwenden; betrifft auch die Chips „Direkt
      qualifiziert“, „Über die KO qualifiziert“, Nachrücker und das Freilos. Test ergänzen.

   4. ENDERGEBNIS: SPALTENTITEL WIE DIE WERTE AUSRICHTEN (06.11.2026, Issue #24): Im Reiter „Endergebnis“ (z. B. nach
      der Qualifikationsphase) sind die Werte linksbündig, die Spaltentitel aber zentriert. Titel linksbündig wie die
      Werte (Platz, Name, Verein, TTR, Ermittelt durch). Umsetzung: Tabelle in `FinalResultView` bekommt eine
      Stilklasse, Kopfzeilen-Label per CSS `CENTER_LEFT` mit gleichem Innenabstand wie die Zellen. Gleich mit prüfen:
      andere Tabellen mit linksbündigen Werten (Teilnehmende, Spiele, Import-Vorschau) einheitlich ausrichten.

   5. QUALIFIKATIONSRUNDEN ÜBERARBEITEN (06.11.2026, Issue #25) – IN KLÄRUNG. Wunsch des Nutzers:
      - Nach der Gruppenphase eine KO-Qualifikation; einstellbar wie bisher, welche Gruppenplätze direkt qualifiziert
        sind und welche in die KO-Runde einziehen.
      - Wer 2 KO-Spiele gewinnt, ist für das Folgeturnier qualifiziert.
      - Runde 1: Gruppen-Höherplatzierte gegen Niedrigerplatzierte (wie bisher). Bleiben nur noch gleiche Ränge übrig
        (z. B. nur Gruppendritte, weil alle Zweiten gegen Vierte gelost wurden), spielen diese gegeneinander. Bei
        ungerader Zahl Freilos für den stärksten Qualifikanten der Gruppenphase. Rangfolge nach der Gruppenphase:
        Gruppenplatz, dann Spiel-, Satz-, Ballverhältnis.
      - Runde 2: Gewinner der Runde 1 zufällig gegeneinander gelost; Gewinner der Runde 2 sind qualifiziert.
      - Optional (auf der Phasenübersicht auswählbar): Die Verlierer der Runde 2 spielen eine weitere Runde; deren
        Gewinner sind AUCH qualifiziert (bisher: Nachrücker).
      - Nachrücker Rang 1: Verlierer der Runde 2 bzw. 3; Verlierer der Runde 3 vor Verlierern der Runde 2; innerhalb
        entscheidet die Gruppenplatzierung.
      - Nachrücker Rang 2: „Verlierer der ersten Gruppenphase“ (vermutlich: der KO-Runde 1), Reihenfolge nur nach TTR.
      Antworten (06.11.2026): (1) Einstellung „n Siege“ entfällt, immer 2 Siege. (2) Freilos zählt als Sieg; in Runde 2
      gibt es KEIN Freilos. (3) Gleiche Ränge per Los, möglichst andere Gruppe/anderer Verein. (4) Mit Zusatzrunde
      spielen alle Verlierer der Runde 2 mit → Nachrücker Rang 1 = Verlierer der Runde 2 (ohne Zusatzrunde) bzw. der
      Zusatzrunde (mit Zusatzrunde).
      (5) Rang 1 innerhalb: Gruppenplatz, dann Spiel-, Satz-, Ballverhältnis aus der Gruppenphase, bei völligem
      Gleichstand TTR.
      (6) Nachrücker Rang 2 = Verlierer der KO-Runde 1, nur nach TTR.
      (A) Ungerade Zahl in Runde 2: nicht zulassen – nur Einstellungen erlauben, bei denen Runde 2 gerade aufgeht
      (Gewinner der Runde 1 inkl. Freilos = aufgerundet N/2 muss gerade sein, d. h. N ≡ 0 oder 3 mod 4; N = Anzahl
      Teilnehmende der KO-Runde). Beim Anlegen der Phase prüfen, sonst Hinweis mit passenden Möglichkeiten.
      (7) Nachrücker aus der Gruppenphase (Einstellung bleibt) bilden eine eigene, letzte Stufe. Die Rangnummern werden
      DYNAMISCH fortlaufend vergeben, je nachdem welche Stufen es gibt: mit KO-Runde Rang 1 = Verlierer Runde 2 bzw.
      Zusatzrunde, Rang 2 = Verlierer Runde 1, Rang 3 = Gruppenphase; ohne KO-Runde (0 je Gruppe in der KO) ist die
      Gruppenphasen-Stufe Rang 1. Leere Stufen zählen nicht mit. Gilt gleich für Endergebnis-Spalte (Punkt 6) und Liste
      (Punkt 7). Sortierung der Gruppenphasen-Stufe wie bisher: Gruppenplatz, dann TTR. Kopfzeile der Phase passt sich an
      (ohne KO kein „qualifiziert nach … Sieg“; mit KO „qualifiziert nach 2 Siegen“).
      Noch offen: (B) ungerade Zahl in der Zusatzrunde.

   6. ENDERGEBNIS: GRUND DER QUALIFIKATION ANZEIGEN (06.11.2026, Issue #26): Spalte „Ermittelt durch“ im Endergebnis
      der Qualifikationsphase zeigt statt nur „qualifiziert“: „Qualifiziert durch Gruppenplatzierung“, „Qualifiziert
      durch KO-Runde“, „Nachrücker 1. Ranges“, „Nachrücker 2. Ranges“ (ggf. „Nachrücker 3. Ranges“, siehe Punkt 5).
      Auch im PDF des Endergebnisses.

   7. QUALIFIKATIONSANSICHT: LISTEN STATT CHIPS (06.11.2026, Issue #28, Designvorschlag angenommen): Blöcke „Direkt
      qualifiziert“, „Über die KO qualifiziert“ und alle Nachrücker-Ränge als nummerierte Liste in zwei gleich breiten
      Spalten (Leserichtung erst links von oben nach unten, dann rechts). Zeile: Position · Name + Verein (Verein
      heller) · Gruppenplatz-Kästchen (z. B. „D3“) · TTR rechtsbündig; gestrichelte Trennlinien zwischen den Zeilen.
      Kopfzeile je Block: Überschrift, Anzahl als Marke (grün = qualifiziert, gelb = Nachrücker) und kurze Angabe der
      Sortierung. Anpassung des Nutzers: ALLE Namen anzeigen (keine Kürzung „… weitere“), Namen und Vereine vollständig
      (Zeilenumbruch statt Abschneiden).

   7. QUALIFIKATIONSANSICHT: LISTEN STATT CHIPS (06.11.2026, Issue #28, Designvorschlag angenommen): Blöcke „Direkt
      qualifiziert“, „Über die KO qualifiziert“ und alle Nachrücker-Ränge als nummerierte Liste in 2 gleich breiten
      Spalten (Leserichtung erst links von oben nach unten, dann rechts). Zeile: Position · Name · Verein (heller) ·
      Gruppenplatz-Kästchen (z. B. „D3“) · TTR rechtsbündig; gestrichelte Trennlinien. Kopf je Block: Überschrift,
      Anzahl als Marke (grün = qualifiziert, gelb = Nachrücker), Sortierregel als Hinweis. Anpassung des Nutzers:
      ALLE Einträge zeigen (kein „… weitere“) und Namen/Vereine VOLLSTÄNDIG, nicht abgeschnitten (Zeile darf umbrechen).
      Also: jeder Block zeigt ALLE Qualifizierten bzw. ALLE Nachrücker des jeweiligen Ranges – nichts wird gekürzt.


0.32.0 (Issues #12, #15–#20 umgesetzt). Doku NICHT aktualisiert (nur auf Zuruf). GRUNDSATZ: Abwärtskompatibilität von
Turnierdateien muss nicht garantiert werden.
Abhängigkeiten: JavaFX 21.0.8, PDFBox/pdfbox-io/fontbox 3.0.8 (Jars vom Nutzer, Maven Central; abgelegt im
Scratchpad deps-0.32, build.sh kompiliert dagegen; Fat-Jar-Basis aus diesen Jars neu aufgebaut), pom.xml angeglichen,
Lizenzen/LIESMICH + PDFBox-LICENSE/NOTICE, InfoDialog, docs/ABHAENGIGKEITEN.md.
WorkspaceView: „Turnier starten“ deaktiviert ohne 2 Teilnehmende/1 Klasse/1 Tisch (Tooltip über Rahmen, Listener auf
Teilnehmende/Klassen/Tischzahl). Overview: Chip „Fertiges Turnier per XML importiert“ (Tournament.importedFinished,
„imported“, TournamentStore.importFinished setzt, copy löscht), CSS weiße Schrift für chip-started/-finished in
markierten Zellen. ClassDialog: ComboBoxen (Spielart/Alter/Wer darf) an Name-Breite gebunden, im Vorgabeturnier ohne
Alter, Spielarten 2er/3er nur im Vorgabeturnier, Checkbox „mindestens 1 Dame je Team“. HandicapView: neues
Tabellen-Layout (headCell/dataCell, CSS .hc-sep gestrichelt nur in Datenzeilen).
2er/3er Turnier: Discipline.TEAM2/TEAM3 (isTeam, size), PlayingClass.isDoubles() = Paarungen/Teams, isPureDoubles,
teamSize, womanRequired (persistiert „team2“/„team3“, „woman-required“); DoublesPair mit third/capacity/members();
Tournament.createTeam (Dame nach hinten), drawPartners für Teams (je Team eine Dame), Entrant.ofTeam (Nachnamen),
Rubber/rubbersOf/teamPositions/defaultLineup/rubberHandicap/recordTeamResult (Aufstellung prüfen, Sieger je Partie,
Korrektur mit FollowUpConflict), Match.lineupA/B (persistiert), getSetsText leer bei Teams, simulateAll für Teams,
handicapProblems für Teams (Dame fehlt, Damen-Grenzen für Mixed). UI: ClassesView Teams (zweizeilige Karten, 3. Platz,
„Teams zulosen“, „Neues Team“), ModeDialog (Gruppen/KO, keine Gewinnsätze/Tiebreak), DrawView Gruppentabelle
„Partien“, TeamResultDialog + ResultEntry, PdfReports.teamSlip (A4, Vorgabe-Tabelle). Tests: Version032Test.
UI unter Xvfb geprüft (Start-Knopf, Vorgaberegeln, Neue Klasse, Teams, Gruppe, Ergebnisdialog, A4-Zettel).

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
