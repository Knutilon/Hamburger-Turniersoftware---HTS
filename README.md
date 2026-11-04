# HTS – Hamburger Turniersoftware

Turniersoftware für Tischtennis, Java 21 + JavaFX.

Copyright © 2026 Richard Michaelis · freie Software unter der [GNU General Public License v3.0](LICENSE) ·
Wünsche und Fehler bitte als [Issue](https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/issues)
(Label „Feature“ bzw. „bug“).

## Neu in 0.30.0

- **Präsentationsansicht – Auswahl je Klasse:** Unter „Ansichten in der Rotation“ stehen alle Phasen aller Klassen,
  gruppiert je Klasse mit Status („nicht ausgelost“, „ausgelost“, „läuft“, „angehalten“, „beendet“), dazu das
  Endergebnis beendeter Klassen; „Tische“ und „Die nächsten 16 Spiele“ bilden einen eigenen Block. Ausgeloste, noch
  nicht gestartete Klassen sind zunächst ausgeschaltet und laufen – eingeschaltet – am Ende der Rotation.
- **Präsentationsansicht – Seiten nach Auflösung:** Keine Karte wird mehr zwischen zwei Seiten zerschnitten. KO-Bäume
  teilen sich in Blöcke der ersten gezeigten Runde (z. B. 8 Spiele samt Folgespielen), die Rundentitel stehen auf
  jeder Seite. Gruppen: höchstens 4 je Bildschirm im festen 2×2-Raster (passen nur 2: nebeneinander, die letzte
  links). Tische: dynamisch skaliert, sonst mehrere Seiten mit ganzen Tischen. Nie kleiner als die normale Schrift.
- **Import:** Einzel – Gemeldete stehen direkt „In der Klasse“ (auch in neuen Klassen), Nachrücker auf der
  Warteliste. Doppel – Paare aus click-TT bleiben Paare; wer ohne Partner/in gemeldet ist, steht unter „Verfügbar“.
- **Einzelklassen ohne Vereinsfilter** (Doppel behalten ihn); die linke Doppel-Liste heißt jetzt „Verfügbar“.
- **Doppelpaarungen:** Suchfeld (Vorname, Nachname, Verein) mit „x von y“ im Titel, ursprüngliche Nummern, Drag &
  Drop auch gefiltert; automatisches Blättern beim Ziehen an den Rand.
- **Doppel-TTR:** fehlt einer Person der TTR, zählt der Wert der Partnerin / des Partners; ohne beide 0 (zuletzt
  gesetzt, Gleichstand per Los).
- **Festes Los als letztes Kriterium** in Gruppentabellen und im Schweizer System (einmal je Gruppe bestimmt und
  gespeichert) statt Setzung/TTR. Der TTR zählt im Ergebnis nur noch im Qualifikationsmodus innerhalb einer Stufe.
- **Turnierübersicht:** der runde „+“-Knopf oben entfällt („Neues Turnier“ auf der Seite bleibt).
- Dokumentation auf 0.30.0 aktualisiert.

## Neu in 0.29.0

- **Präsentationsansicht (zweiter Bildschirm):** neuer Menüpunkt. Erkennt HTS einen zweiten Bildschirm, läuft dort
  randlos eine reine Anzeige mit großer Schrift (Esc beendet; „Vorschau im Fenster“ zum Ausprobieren). Die Ansichten
  wechseln nach einstellbarer Zeit (Standard 20 Sekunden): laufende Phasen, Tische, die nächsten 16 Spiele, beendete
  Phasen, Endergebnisse. Jede Ansicht lässt sich aus der Rotation nehmen; große Felder verteilen sich auf Seiten.
- **Kopfbereich kompakter:** „● Gestartet …“ steht unter dem Seitentitel neben Turniername und Datum; „Klasse
  anlegen“ und die Startknöpfe stehen rechts oben – mehr Platz für Klassenliste und Klassenansicht.
- **Klasse auswählen** öffnet die aktuellste Phase (beendete Klasse: „Endergebnis“, noch nicht ausgelost:
  „Teilnehmende der Klasse“).
- **Info-Knopf (i)** rechts oben: Version, Signatur, Lizenz GPL 3.0, Links zur Feature- und Bug-Sammlung auf GitHub,
  Lizenzen der mitgelieferten Bibliotheken. HTS steht jetzt unter der GPL 3.0 (Datei LICENSE).
- **Gruppen-Setzung „Gruppenkopf gesetzt“:** TTR-Nr. 1 in Gruppe A, Nr. 2 in B …, die übrigen Plätze wie bei den
  Stärkegruppen. Die anderen heißen jetzt „TTR Setzung (Schlange)“ und „TTR Setzung (Stärkegruppen)“.
- **Modus festlegen:** eigene Zeile „Vereinsgleichheit beachten“ mit Checkbox; Auswahllisten gleich breit.

## Neu in 0.28.0

- **Suche über Vorname, Nachname und Verein** in allen Suchfeldern (Gruppen, KO, Klassen, Spiele): mehrere Wörter
  müssen alle passen, als Teilwort, Groß-/Kleinschreibung und Reihenfolge egal (z. B. „Michaelis Harburg“, „Harburg“).
- **Suche im KO-Baum:** Die Partien der Treffer (auch Doppel) stehen samt Pfad hervorgehoben, alles andere ist
  abgeblendet. Enter springt zum nächsten Treffer, Umschalt+Enter zum vorherigen („Treffer 2 von 5“) – auch in der
  Gruppenphase.
- **KO-Baum:** Rundentitel bleiben beim Scrollen oben stehen; der Turnierpfad ist durchgezogen, die Trennlinien
  zwischen den Runden bleiben gestrichelt. „Spiel um Platz 3“ ist ein Schalter oben rechts. Offene Plätze heißen
  „Sieger/in Spiel n“ bzw. „Verlierer/in Spiel n“ (App und PDFs).
- **Hinweise:** „Phase angehalten“ im gelben Kasten oben links (mit Zeilenumbruch); die offenen Spiele stehen in der
  Infozeile („3 Spiele offen“ / „keine Spiele offen“); „Phase abschließen“ und „Zu Phase 2“ stehen bei den Knöpfen rechts.
- **Tische:** eigene Namen bis 30 Zeichen (Klick auf den Namen der Tischkarte), überall statt „Tisch n“ angezeigt
  (Karten, Warteschlange, Spielzettel, PDFs). Einheitliche Kopfzeile (Name · Klassen · Status rechts) und gleich
  große Karten im Raster.

## Neu in 0.27.0

- **Automatisch scrollen beim Ziehen:** In Gruppen-, KO- und Qualifikationsansicht blättert die Ansicht weiter, sobald
  der Mauszeiger beim Drag & Drop den oberen/unteren (bzw. linken/rechten) Rand erreicht – je näher am Rand, desto schneller.
- **KO-Baum:** Verbindungslinien von jedem Spiel zum Folgespiel (nicht beim Spiel um Platz 3) und senkrechte
  Trennlinien zwischen den Runden – alles hellgrau, dünn, gestrichelt.
- **Kartenrand nach Status** (KO, Qualifikation, Platz 3): anstehend grau, läuft türkis, pausiert gelb, beendet schwarz.
- **Layout:** Gelber Hinweis („Noch … Spiele offen“) schmal und oben links neben den Knöpfen. Begegnungen/Freilose
  stehen in der Infozeile, z. B. „KO-System · 128er-Feld · 95 Begegnungen · 32 Freilose · zufällig · Vereinsgleichheit beachtet“.
- **Tische deaktivieren:** Knopf auf der Karte eines freien Tisches; deaktivierte Tische bekommen keine Spiele,
  haben einen schwarzen Rand und lassen sich wieder aktivieren (in der Turnierdatei gespeichert).
- **Qualifikation – Nachrücker je Gruppe (ohne Spiel):** Die nächsten N Gruppenplätze werden Nachrücker mit eigenem
  Rang (zuerst Gruppenplatz, dann TTR); schlechtere Plätze teilen sich je Gruppenplatz einen Platz. Vorquali z. B.
  direkt 2, KO 0, Nachrücker 2.
- **Qualifikation – freie Auslosung:** Runde 1 per Los, wobei ein besser Platzierter (z. B. Gruppenzweiter) gegen
  einen schlechter Platzierten (Gruppendritten) einer anderen Gruppe spielt (Vereinsgleichheit möglichst vermieden);
  ab Runde 2 und in der Zusatzrunde ganz frei. Das Freilos bekommt weiterhin der Bestplatzierte.

## Neu in 0.26.0

- **Qualifikationsmodus neu (HEM-Hauptqualifikation):** Die besten Q je Gruppe sind direkt qualifiziert (untereinander
  nach TTR). Die nächsten K je Gruppe spielen ein KO-System – die Gewinner spielen weiter (Runde 1: Höchstplatzierter
  gegen Niedrigstplatzierten einer anderen Gruppe). Wer n Spiele gewinnt, ist qualifiziert. Die Verlierer werden
  Nachrücker: später ausgeschieden = weiter oben; ein Freilos in der letzten Runde steht über deren Verlierern;
  innerhalb einer Stufe zuerst der bessere Gruppenplatz (Zweite vor Dritten), dann TTR. Bei Bedarf lässt sich eine
  Zusatzrunde für die Verlierer der letzten Runde zuschalten (auch laufend). Die alte Logik („Verlierer spielen
  weiter“) entfällt.

## Neu in 0.25.0

- **Fertiges Turnier importieren (click-TT):** Turnierübersicht → „Fertiges Turnier importieren (click-TT)“ liest eine
  click-TT-Ergebnisdatei (Format TournamentPortal.dtd, egal ob aus click-TT, poinTT oder HTS) und legt ein neues,
  abgeschlossenes Turnier an: je Konkurrenz eine Klasse, Phase 1 als Gruppenphase („Gruppe 1“/„Gruppe A“) oder
  KO-System (inkl. Spiel um Platz 3), alle Spiele mit Sätzen; Nicht-Angetretene ohne Klasse. Hinweise statt Abbruch.
- **KO-Baum:** „Spiel um Platz 3“ steht unter der Spalte „Finale“ (Karte mit „Spiel n“).
- **Seite „Spiele“:** Spaltentitel und Werte gleich ausgerichtet (Text links, kurze Werte zentriert).
- Tabelle „Verfügbar“: Spalte „Meldung“ entfernt.

## Neu in 0.24.0

- **Vereinsgleichheit bei der Gruppenauslosung** (Schlange, zufällig, Stärkegruppen): nach der Verteilung wird
  nachgebessert – Mitglieder mit Vereinsgleichheit werden gegen möglichst gleich starke Mitglieder anderer Gruppen
  getauscht, solange das Vereinsgleichheiten verringert. Gruppenköpfe bleiben, Stärkegruppen bleiben erhalten.
- **Doppelklassen:** mehr als 5 Doppel → Phase 1 immer KO-System; bis 5 Doppel → „Gruppenphase“ (eine Gruppe, jeder
  gegen jeden) oder KO-System; kein Schweizer System; Vereinsgleichheit spielt bei Doppeln keine Rolle.
- Klassenliste: Abzeichen „Doppel“ auch in der ausgewählten Zeile lesbar.
- Tabelle „Verfügbar“: Spalte „Klassen“ entfernt.

## Neu in 0.23.0

- **Satzergebnisse** in der Gruppenansicht per Klick auf die ganze Spielzeile auf-/zuklappen (Doppelklick = korrigieren).
- **Klasse bearbeiten** ist nach dem ersten Ergebnis in Phase 1 nur noch lesbar.
- **Partner zulosen** (Doppelklassen): Einzelne ohne Partner/in werden rein zufällig zu Doppeln; bei ungerader Zahl
  bleibt eine/r übrig (Hinweis).
- **Doppel ausgeschrieben** (je Person eine Zeile „Nachname, Vorname“): Ergebnisdialog, KO-Baum, Spielzettel und alle PDFs.
- **Ergebnisdialog:** „1. Satz“ … ausgeschrieben, Spalte „Punkte“, breiteres Fenster.
- **PDF Gruppen** (einzeln und alle): Spielplan zweispaltig je Runde („Runde k“), ohne Nr./Runde-Spalten.
- **PDF KO-System:** Turnierbaum (A4 quer, Kästen mit „Spiel n“, Namen + Verein, Sätze unter dem Kasten, Freilose mit
  Kasten, Spiel um Platz 3 unter dem Finale; große Felder auf mehrere Seiten), danach Spielplan je Runde als Karten.
- **KO-Baum in HTS:** neue Karten – Kopf „Spiel n · Tisch · Status“, Namen ausgeschrieben mit Verein, Doppel zweizeilig
  (ohne TTR), Kennung als Chip, Sätze unter dem Spiel.

## Neu in 0.22.0

- **Gruppen-Spielelisten:** sortiert (aktiv/pausiert → eingereiht → geplant → abgeschlossen, je nach Spielnummer),
  die ersten 7 Spiele sichtbar, Spiele 8 bis Ende ausklappbar; neu sortiert nach jeder Ergebniseingabe.
- **Satzergebnisse:** Klick auf das Ergebnis einer abgeschlossenen Partie klappt die Sätze darunter aus, ein weiterer
  Klick auf die Zeile klappt sie ein (Doppelklick = Ergebnis korrigieren).
- **Einzelne Gruppe drucken:** Button „Drucken“ auf jeder Gruppenkarte (aktueller Stand, jederzeit);
  „alle Gruppen“ vor „PDF / Drucken“.
- Button „Endergebnis als PDF“ im Abschluss-Hinweis entfernt (PDF weiter im Reiter „Endergebnis“).
- **click-TT-Ergebnisdatei** heißt „Ergebnisse - <erste 20 Zeichen des Turniernamens> - DD_MM_YYYY.xml“.
- Suchfeld der Gruppenphase: Platzhalter „Name suchen (Vor-/Nachname)“.

## Neu in 0.21.0

- **click-TT-Import:** In neu angelegten Einzelklassen stehen die Gemeldeten zunächst unter „Verfügbar“
  (Spalte „Meldung“: „gemeldet“) und werden von Hand in die Klasse gezogen.
- **Filter für „In der Klasse“:** Name suchen, Verein, Geschlecht – wie bei „Verfügbar“.
- **Gruppenphase:** Suche nach Teilnehmenden (Vor- und/oder Nachname) zeigt deren Gruppe und Spiele; Gruppen mit mehr
  als 10 Spielen sind eingeklappt und lassen sich je Gruppe ausklappen.
- **Phasen-Reiter:** Startinfo und Knöpfe rechts neben „Bei gleichem Spielverhältnis“, darunter eine Infozeile
  „x Gruppen · y Spiele offen“.
- **Testturnier** (beim Anlegen/Bearbeiten): im Kopfbereich ein Würfel-Knopf „Alle Spiele simulieren“ – trägt alle
  offenen Spiele laufender Phasen zufällig und regelgerecht ein (Best of 5/7, Verlängerung bis 20:18), inkl.
  Folgerunden; ohne Spielzettel-Druck, ohne Phasen abzuschließen.
- **Turnier kopieren** beim Anlegen: ohne Auslosung, alles inkl. Ergebnisse oder nur Teilnehmende.

## Neu in 0.20.0

- **Spiel um Platz 3** für jedes KO-System (Phase 1 und 2) per Häkchen im Phasen-Reiter zuschaltbar – auch während
  die Phase läuft (sind die Halbfinals gespielt, wird es sofort angelegt und eingereiht). Gewinner 3., Verlierer 4.
  Nicht in der KO-Qualifikation des Qualifikationsmodus.
- **Gewinnsätze je Klasse:** 3 oder 4 im Dialog „Modus festlegen“; Ergebnisdialog, Prüfung, Spielzettel (bis 7 Sätze),
  Aufgabe-Wertung und Export richten sich danach.
- **KO-Baum in Phase 2:** statt der Setznummer Gruppe + Gruppenplatz, z. B. „A1“, „D3“.
- **Schweizer System** ohne Folgephase – danach nur das Endergebnis.
- **Qualifikationsmodus:** bei der Paarung wird Vereinsgleichheit nach Möglichkeit vermieden (keine festen Gruppenpaare).
- **Speicherorte:** `Dokumente\HTS Turniere\<Turniername>\` mit den Unterordnern „Turnierdatei“, „XML Export“ und
  „PDF Export“. Per „Durchsuchen“ bzw. im Speichern-Dialog andere Ordner wählbar (gilt je Turnier). Alte Turniere aus
  `<Benutzerordner>\HTS\Turniere` werden automatisch übernommen.
- Text „Noch 1 Spiel offen“ (Einzahl).

## Neu in 0.19.0

- **Höchstens 2 Phasen** je Klasse – Phase 3 entfällt.
- **Qualifikationsmodus neu:** Q direkt Qualifizierte je Gruppe (auch 0), K je Gruppe in der KO-Qualifikation, R Runden.
  Runde 1: alle K-Teilnehmenden (Höchstplatzierter gegen Niedrigstplatzierten einer anderen Gruppe); die Verlierer
  spielen jeweils die nächste Runde. Q = 0: Gewinner Runde 1 qualifiziert, Gewinner Runde 2 Nachrücker Rang 1 usw.;
  Q > 0: Gewinner Runde 1 Nachrücker Rang 1 usw. Die Verlierer der letzten Runde bilden den nächsten Nachrückerrang.
- **Endergebnis Qualifikation:** nur „qualifiziert“ und „Nachrücker Rang x“, innerhalb nach TTR; alle anderen nach
  Gruppenplatz, gleiche Gruppenplätze teilen sich den Platz (z. B. viermal 13.).
- Spieler der laufenden Qualifikationsrunde per Drag & Drop tauschbar.

## Neu in 0.18.0

- **Qualifikationsmodus** (Phase 2, nur nach einer Gruppenphase in Phase 1): die besten n jeder Gruppe sind direkt
  qualifiziert; die nächsten m jeder Gruppe spielen eine KO-Qualifikation (Höchstplatzierter gegen
  Niedrigstplatzierten einer anderen Gruppe) über wählbar viele Runden – höchstens so viele, dass mindestens eine/r
  übrig bleibt. Wer alle Runden übersteht, ist ebenfalls qualifiziert. Drag & Drop wie im KO-System.
- **Vereinsnamen** in allen KO-Darstellungen (KO-Baum, KO-Qualifikation, PDF-Spielplan): „Name (Verein)“.
- Dokumentation fortgeschrieben: `docs/HTS-Dokumentation.md`.

## Neu in 0.17.0

- Knöpfe und Menüpunkte ohne „…“ (z. B. „Phase abschließen“, „Tische den Klassen zuordnen“, „Speichern“, „Drucken“).
- **Aufgabe**: Gibt eine Partei auf, zählt das Spiel 0:3 (Sätze 0:11) – so wird es auch exportiert. Alle noch
  offenen Spiele dieser Partei in der Phase werden für die Gegner mit 3:0 (Sätze 11:0) gewertet (Turnierwertung),
  aber **nicht** exportiert; im Schweizer System wird sie nicht mehr gepaart. Wird die Aufgabe wegkorrigiert, werden
  diese Spiele wieder geöffnet.
- **„nicht relevant für Export“**: Auf der Seite „Spiele“ (nur dort, nur für abgeschlossene Spiele) lässt sich je
  Spiel der Haken „Export“ entfernen – das Spiel fehlt dann in der click-TT-Ergebnisdatei. Standard: alle exportiert.

## Neu in 0.16.2

- **HTS-Logo** als Programmsymbol (Fenster/Taskleiste) und in der Kopfleiste; daneben steht oben das geöffnete
  Turnier („Turnier: Name · Datum“).
- Der Speichern-Knopf oben rechts entfällt (es wird automatisch gespeichert); die Knöpfe oben rechts „zucken“ beim
  Überfahren nicht mehr.
- Vereinsgleichheit wird nur bis zum Start der Phase markiert.
- Tische den Klassen zuordnen: gleich breite Spalten, Klassennamen einheitlich, „alle“ zentriert über den Haken.
- Phasen-PDF im **Querformat**, über die ganze Breite; ohne Spalte „Status“; die Spalte „Sätze“ zeigt 5 Sätze auch
  mit Verlängerung vollständig (z. B. 13:11, 12:14, 15:13, 11:13, 16:14).

## Neu in 0.16.1

- Knöpfe sind programmweit immer ausgeschrieben (nie mit „…“ gekürzt); wird es eng, bricht die Knopfzeile im
  Phasen-Reiter um.
- Phasen-Reiter: „Einstellungen ändern“ entfällt; „Neu auslosen“ in normaler Größe (dunkelblau), „Auslosung
  verwerfen“ ausgeschrieben, „bereit“ (statt „Als bereit markieren“), „starten“ (statt „Klasse starten“).

## Neu in 0.16.0

- Gruppentabelle: Spalte **Verein** zwischen Name und TTR.
- Ergebnis eintragen: Ein leerer Satz vor einem eingetragenen Satz wird wie eine falsche Eingabe rot markiert und
  blockiert das Speichern.
- **Spiele zurückziehen** (Tische: „zurückziehen“): Das Spiel verlässt den Tisch und reiht sich hinten in der
  Warteschlange wieder ein (in einer angehaltenen Phase: geplant).
- **Pausieren**: Wird eine Phase angehalten, werden ihre Spiele an den Tischen **pausiert** (Status „pausiert“) – sie
  bleiben ihren Tischen zugeordnet, Ergebnisse sind nicht möglich; eingereihte Spiele verlassen die Warteschlange.
  Ein pausiertes Spiel wird bewusst mit „fortsetzen“ (am Tisch) wieder gestartet; „Phase fortsetzen“ setzt alle
  pausierten Spiele fort und reiht die übrigen wieder ein.
- **KO umsortieren in angehaltenen Phasen** (auch Phase 2/3, z. B. direkt nach der Auslosung): Plätze und Paarungen
  der 1. Runde per Drag & Drop frei tauschen. Nicht mehr gültige Spiele (gleich welcher Status) werden gelöscht bzw.
  zurückgesetzt – samt abhängiger Spiele späterer Runden – und neue Partien entstehen; sie werden beim Fortsetzen
  eingereiht. In laufenden (nicht angehaltenen) Phasen geht das weiterhin nur für Spiele, die noch nicht begonnen haben.
- **Ergebnisse exportieren**, sobald alle Klassen beendet sind (Knopf in der Kopfzeile der Turnieransicht):
  - **Gesamtergebnis als PDF** – je Klasse das Endergebnis, im eingebauten Viewer (Speichern/Drucken).
  - **click-TT-Ergebnisdatei (XML)** wie in poinTT („Ergebnisse.xml“): Die importierte click-TT-Turnierdatei
    (HTS legt beim Import eine Kopie ab, sonst wird nach ihr gefragt) wird je Konkurrenz um `<matches>` mit allen
    Spielen ergänzt (`nr`, `group`, `scheduled`, `player-a`, `player-b`, `set-a-1…7`, `set-b-1…7`, `sets-a/b`,
    `matches-a/b`, `games-a/b`) und jeder Teilnehmer erhält `placement`. Kampflose Spiele (Aufgabe) werden – wie in
    poinTT – nicht übertragen. Die Datei kann in click-TT hochgeladen werden.

## Neu in 0.15.0

- **Vereinsgleichheit** nur noch in Phase 1 (Gruppen und KO-System): Hinweis und Markierung entfallen ab Phase 2.
- **Phase 2/3 neu auslosen**: Ist die Folgephase angehalten (und läuft an keinem Tisch mehr ein Spiel von ihr),
  erscheint „Neu auslosen“ – nach Platzierung der Vorphase; bisherige Ergebnisse dieser Phase entfallen, die neue
  Auslosung bleibt angehalten, bis „Phase fortsetzen“ geklickt wird.
- Knopf „Phase fortsetzen“ ist wieder beschriftet.
- **KO-System per Drag & Drop korrigieren – auch während der Phase** (Phase 1, 2 und 3): Plätze bzw. Paarungen der
  1. Runde lassen sich tauschen, solange deren Spiele noch nicht laufen bzw. gespielt sind.
- **Ergebnisse korrigieren – auch in abgeschlossenen Phasen**: Tabellen, Platzierungen und Endergebnis rechnen
  sich neu. Ändert sich im KO-System der Sieger und sind Folgespiele schon gespielt (oder laufen), fragt HTS nach:
  diese Folgespiele werden zurückgesetzt und mit dem neuen Sieger neu eingereiht; eine beendete Klasse muss danach
  erneut abgeschlossen werden. In eine Endgruppe übernommene Vorrundenspiele werden mitkorrigiert.

## Neu in 0.14.2

- Rangfolge unter allen Gruppenersten (Zweiten …) für die Setzung in Phase 2/3: Spielverhältnis, Satzverhältnis,
  Punktverhältnis (jeweils als Anteil gewonnen ÷ gesamt); bei Gleichheit entscheidet das Los. TTR zählt nur in Phase 1.
- Tische: Status („aktiv“) direkt neben „Tisch x“; Aktionen rechts neben der Partie: **Ergebnis**, **Drucken**
  (Spielzettel als PDF), **wechseln** (Tisch wechseln).
- Ergebnis eintragen: Hat eine Seite die Gewinnsätze erreicht, sind die übrigen Sätze gesperrt; Enter nach dem
  letzten Satz springt auf „Ergebnis speichern“, ein weiteres Enter speichert.

## Neu in 0.14.1

- Dialoge passen ihre Größe dem Inhalt an (Knöpfe bleiben sichtbar, Hinweise werden nicht abgeschnitten).
- Gruppenübersicht: Spalte **TTR** hinter dem Namen; Spielliste breiter, „Tisch x“ vollständig.
- **KO-System in Phase 2/3** wird nach der Gruppenplatzierung gesetzt (die TTR-Setzliste gilt nur in Phase 1):
  Freilose an die besten Gruppenersten; der Höchstplatzierte ohne Freilos spielt gegen den Niedrigstplatzierten,
  der noch nicht zugeordnet ist (nicht aus seiner Gruppe); Spieler/innen derselben Gruppe stehen in verschiedenen
  Hälften und können sich erst im Finale treffen. (Rangfolge unter den Gruppenersten: siehe 0.14.2.)

## Neu in 0.14.0 – Modus festlegen, Setzung, Vereinsgleichheit, Drag & Drop

- **Modus festlegen**: Der Reiter „Phase 1“ erscheint erst, wenn die Teilnehmenden feststehen und unter
  „Teilnehmende der Klasse“ **„Teilnehmende fertig – Modus festlegen …“** ausgeführt wurde. Dort: Modus
  (KO-System, Gruppenphase, Schweizer System), Setzung, Gruppengröße/-anzahl, Gleichstand-Regel, Runden,
  Vereinsgleichheit – „Auslosen“ erstellt Phase 1. Im Phasen-Reiter: „Neu auslosen“ (gleiche Einstellungen),
  „Einstellungen ändern …“, „Auslosung verwerfen“ (Reiter verschwindet wieder).
- **Setzung in Gruppen**: *Setzung (Schlange)*, *Setzung (Stärkegruppen)* oder zufällig. Stärkegruppen à „Anzahl
  Gruppen“ nach TTR (bei 4 Gruppen: 1–4, 5–8, 9–12, 13–16); die letzte Stärkegruppe nimmt den Rest auf
  (15 Teilnehmende: 9–15). Jede Gruppe bekommt aus jeder Stärkegruppe mindestens eine/n.
- **Vereinsgleichheit beachten** (einstellbar, Standard an):
  - Schlange: wird unterbrochen – der Nächststärkere ohne Vereinsgleichheit in der Gruppe kommt auf den Platz;
    geht das nicht mehr, normal nach der Schlange.
  - Stärkegruppen: jemand anderes derselben Stärkegruppe ohne Vereinsgleichheit; sonst nach Stärkegruppe.
  - KO-System: Vereinskameraden nicht in Runde 1 – der schwächer Gesetzte tauscht mit dem Nächstplatzierten.
  - Vereinsgleichheit wird in Gruppen (rote Namen, „⚠ Vereinsgleichheit“) und bei Erstrundenpaarungen markiert.
- **KO-Setzliste** nach TTR (Doppel: Ø-TTR), z. B. 16er-Feld: 1–16, 9–8, 5–12, 13–4, 3–14, 11–6, 7–10, 15–2;
  8er: 1–8, 5–4, 3–6, 7–2; 4er: 1–4, 3–2; 32er/64er entsprechend. Freilose gehen an die Besten.
- **Drag & Drop vor dem Start der Phase**: Gruppenübersicht – Namen auf ein anderes Mitglied ziehen = tauschen,
  auf eine andere Gruppe = verschieben (eine Gruppe behält mind. 2). KO-System – Namen auf einen anderen Platz der
  1. Runde ziehen = Plätze tauschen (auch mit Freilosen), „Spiel x“-Kopf auf eine andere Paarung = Paarungen tauschen.
  Die Spiele werden dabei neu angelegt.

## Neu in 0.13.0 – PDF, Tischzuordnung, Phase abschließen

- **Tische**: Knopf „Ergebnis“ (kleiner). „Spielzettel“ öffnet den Zettel als **PDF im eingebauten Viewer**
  (Zoom, Speichern, Drucken). **Tisch wechseln**: laufendes Spiel an einen anderen Tisch verschieben – ist der belegt,
  tauschen die Spiele. In der Warteschlange holt „an Tisch …“ ein Spiel direkt an einen freien Tisch.
- **Tische den Klassen zuordnen** (Seite „Tische“): Matrix Tisch × Klasse. Ohne Haken ist ein Tisch für alle
  Klassen da (Standard). Ein reservierter Tisch bekommt nur Spiele seiner Klassen – passt das nächste Spiel der
  Warteschlange nicht, nimmt er das erste passende spätere (z. B. Spiel 3, 7 oder 12 der Warteschlange).
- **Phase abschließen** statt „Danach“/„Aufsteiger“ bei der Auslosung: Sind alle Spiele einer Phase gespielt,
  „Phase abschließen“ wählen:
  - **Endergebnis** – die Klasse ist beendet (KO: Platzierungen nach Runden, Gruppen: Gruppenplätze);
  - **Neue Phase** – Gruppenphase, KO-System oder Gruppenphase mit Platzierungsspielen (nur nach 2 Gruppen), mit
    Aufsteigern je Gruppe (Schweizer System: insgesamt) und Gruppengröße. Nach KO-System und Platzierungsspielen
    sowie in Phase 2 gibt es nur das Endergebnis.
  Auch KO-System und Platzierungsspiele werden jetzt ausdrücklich abgeschlossen.
- **PDF statt Direktdruck**: Phase („PDF / Drucken“ im Phasen-Reiter, je Gruppe eine A4-Seite), Endergebnis
  („Als PDF anzeigen / drucken“) und Spielzettel öffnen im eingebauten PDF-Viewer. Der automatische
  Spielzettel-Druck beim Aufruf an einen Tisch läuft weiter ohne Rückfrage auf dem Standarddrucker (A6).
- PDF-Bibliothek: Apache PDFBox 3.0.5 (Apache License 2.0, siehe `Lizenzen/`).

## Neu in 0.12.0 – Schweizer System, Drucken, Spielzettel

- **Schweizer System** (nur für Phase 1 wählbar): feste Rundenzahl (Vorschlag: Runden eines KO-Systems + 1).
  1. Runde nach Setzung obere gegen untere Hälfte (sonst zufällig); jede weitere Runde wird automatisch nach der
  Tabelle gepaart, sobald die vorige komplett gespielt ist – Punktgleiche gegeneinander, sonst rückt jemand aus der
  nächsten Punktgruppe auf; niemand spielt zweimal gegen denselben Gegner. Ungerade Zahl: je Runde ein Freilos
  (bevorzugt Tabellenletzte, höchstens eins je Person; zählt als Sieg, nicht in der Buchholzzahl).
  Tabelle: Sp, Pkt, S:N, Sätze, Bälle, **BHZ** (Buchholz = Summe der Punkte der Gegner) und **FBHZ**
  (Feinbuchholz = Summe der Buchholzzahlen der Gegner). Reihenfolge: Punkte, Buchholz, Feinbuchholz,
  Satzdifferenz, Balldifferenz. Danach optional KO-System oder Gruppenphase mit den besten n („Aufsteiger gesamt“).
- **Endergebnis**-Reiter erscheint erst, wenn die Klasse beendet ist.
- **Drucken** (Knopf in jedem Phasen-Reiter): je Gruppe eine A4-Seite mit Zwischenstand der Tabelle und Spielplan
  (gespielte Ergebnisse mit Sätzen, kommende Spiele mit Status/Tisch); KO-System: Spielplan aller Runden.
  (Seit 0.13.0 als PDF im eingebauten Viewer.)
- **Spielzettel (A6)**: Sobald ein Spiel an einen Tisch geht, wird automatisch ein Spielzettel auf dem
  Standarddrucker gedruckt – mit Tisch, Zeit, Spiel-Nr., Konkurrenz (Klasse, Phase, Gruppe/Runde) und den Namen
  vorausgefüllt, Satztabelle zum Ausfüllen, Wahl, Gewinner/in, Unterschrift. Ein-/ausschaltbar auf der Seite
  „Tische“; dort lässt sich der Zettel je Tisch auch erneut drucken. Kennt der Drucker kein A6, wird in A6-Größe
  auf das Standardpapier gedruckt.

## Neu in 0.11.0 – Phasen, Endergebnis, Platzierungsspiele

- **Tische klassenübergreifend voll belegt**: Alle gestarteten Klassen teilen sich die Warteschlange. Kann eine Klasse
  gerade kein Spiel stellen (alle ihre Spieler/innen spielen schon), bekommen andere Klassen die freien Tische.
  Eine neue Phase startet sofort, damit keine Tische leer stehen.
- **Phasen als Reiter** einer Klasse (höchstens 3): „Phase 1 – Gruppenübersicht“, „Phase 2 – KO-System“,
  „Phase 2 – Gruppenübersicht“, „Phase 2 – Platzierungsspiele“. Folgephasen erscheinen nicht mehr als eigene Klassen.
- **Phase anhalten / fortsetzen**: Eingereihte Spiele dieser Phase gehen zurück auf „geplant“, laufende Spiele
  werden noch beendet, die Tische gehen an andere Klassen. Beim Fortsetzen behalten die Spiele ihren Platz.
- **Nach dem letzten Gruppenspiel**: unter „Danach“ die nächste Phase wählen (KO-System, weitere Gruppenphase,
  Gruppenphase mit Platzierungsspielen) und erstellen – oder **Klasse beenden**. KO-Phase, Platzierungsspiele und
  eine Gruppenphase als 3. Phase beenden die Klasse automatisch mit dem letzten Ergebnis.
- **Reiter „Endergebnis“**: alle Platzierungen über alle Phasen, während des Turniers als Zwischenstand
  (KO: ausgeschieden in welcher Runde / noch im Wettbewerb; Gruppen: aktueller Tabellenplatz). KO-Plätze:
  1., 2., zweimal 3., 5.–8., 9.–16. … In Gruppen ausgeschiedene folgen nach Gruppenplatz.
- **Gruppenphase mit Platzierungsspielen** (nur nach genau 2 Gruppen): Die ersten beiden jeder Gruppe spielen eine
  Endgruppe um Platz 1–4. Das Spiel Erster gegen Zweiter aus Phase 1 wird übernommen, es bleiben 4 Spiele
  (über Kreuz). Wertung: Spielverhältnis, direkter Vergleich, Satz- und Ballverhältnis. Die Dritten beider Gruppen
  spielen um Platz 5/6, die Vierten um 7/8 usw. Sind die Gruppen ungleich groß, bekommt der Übrige den letzten
  Platz ohne Spiel.

## Neu in 0.10.0 – Ergebnisse Satz für Satz

- **Ergebniserfassung je Satz** (Tische, Spiele, Gruppenübersicht und Turnierbaum per Doppelklick):
  ein Feld je Satz, z. B. `11:8`, `7:11`, `14:12` – oder kurz `+8` (= 11:8), `-7` (= 7:11), `+0` (= 11:0),
  `+10` (= 12:10). Enter springt zum nächsten Satz. Die erste Zahl gehört immer zur erstgenannten Partei.
- Gültig ist ein Satz mit 11 Punkten und höchstens 9 für den Gegner oder – ab 10:10 – mit genau 2 Punkten Abstand.
  Ungültige Sätze (z. B. 13:14, 11:10) werden **rot markiert**, gespeichert werden kann erst ein vollständiges Ergebnis.
  Das **Satzverhältnis** wird automatisch berechnet (3 Gewinnsätze).
- **Aufgabe** markierbar: der Gegner gewinnt mit 11:0 je Gewinnsatz (3:0).
- **KO**: Der Sieger rückt automatisch in die nächste Runde; sobald beide Gegner feststehen, wird das Spiel eingereiht.
- **Gruppentabellen** in der Gruppenübersicht: Platz, Spiele, Siege:Niederlagen, Sätze, Bälle. Gewertet wird zuerst
  nach Spielverhältnis; bei Gleichstand – bei der Auslosung wählbar – nach **Satz- und Ballverhältnis** oder nach dem
  **direkten Vergleich**.
- **Folgephase**: bei der Auslosung wählbar, ob nach der Gruppenphase eine **KO-Phase** oder eine **weitere Gruppenphase**
  folgt und wie viele je Gruppe aufsteigen (Aufsteiger grün markiert). Sind alle Gruppenspiele beendet, legt
  „Endrunde erstellen“ eine neue Klasse an und lost sie nach Platzierung aus (alle Gruppenersten zuerst gesetzt,
  Spieler/innen derselben Gruppe möglichst weit auseinander).
- **Spiele**: Suchfeld nach Teilnehmenden, Spalte „Sätze“. Ergebnisse abgeschlossener Spiele lassen sich
  **nachträglich korrigieren** (nicht mehr, wenn sich der KO-Sieger ändern würde und das Folgespiel schon beendet ist).

## Stand 0.9.0

**Design** im Stil von mytischtennis.de: dunkelblaue Kopfleiste mit waagerechtem Menü, Türkis als Akzent,
weiße Karten auf hellgrauem Grund, Pillen-Knöpfe. HTS hat ein eigenes Zeichen (kein myTischtennis-Logo).

**Turnierübersicht** (Startbildschirm, Menü „Turniere“)
- Mehrere Turniere: *Neues Turnier* fragt Name, Datum und **Anzahl Spieltische** (Pflicht) ab und legt das Turnier
  sofort gespeichert an.
- Jede Karte zeigt Datum, Anzahl Teilnehmende und Klassen sowie den Status.
- **Starten** hält die Startzeit fest (vorher: „nicht gestartet“). Starten geht auch in der Turnieransicht.
- Bearbeiten (Name, Datum), Löschen, Öffnen (Doppelklick oder „Öffnen“).
- *Turnierdatei übernehmen …* holt eine `.hts`-Datei aus HTS 0.2/0.3 als neues Turnier herein.
- Alle Turniere liegen als `.hts`-Dateien im Ordner `<Benutzerordner>\HTS\Turniere` (unten angezeigt).

**Turnieransicht**
- Menü in der Kopfleiste: **← Turniere** (zurück zur Übersicht), **Teilnehmende**, **Klassen**, **Tische**, **Spiele**
  („Spiele“ ist ausgegraut, bis das Turnier oder mindestens eine Klasse gestartet ist);
  rechts runde Knöpfe für Speichern und Zurück.
- Seitenkopf mit Turniername, Datum und „Turnier starten“ bzw. „Gestartet …“.
- Teilnehmende, Klassen und der XML-Import beziehen sich immer nur auf das geöffnete Turnier.
- **Speichern**: Jede Änderung wird automatisch gespeichert; zusätzlich Knopf „Speichern“ (`Strg+S`).

**Teilnehmende**
- „XML der Teilnehmenden importieren“ (nur auf dieser Seite), „Hinzufügen“, „Bearbeiten“ (auch Doppelklick), „Löschen“.
- Bearbeitbar: Vorname, Nachname, Geschlecht, Geburtsdatum oder Jahrgang, Verein, TTR, Lizenz-Nr.
- Beim „Hinzufügen“ kann gleich eine bestehende **Einzelklasse** gewählt werden (oder leer bleiben).
- Rechtsklick: Bearbeiten, Löschen.
- **Angemeldet** heißt: in mindestens einer Klasse. Das wird auf der Seite „Klassen“ per Drag & Drop festgelegt.

**Klassen**
- Klassen von Hand anlegen („Klasse anlegen“), bearbeiten (auch umbenennen), löschen.
- **Spielart**: Einzel oder Doppel.
- **Klassentyp** aus zwei Merkmalen:

  | | weiblich und männlich | nur weiblich |
  |---|---|---|
  | **Erwachsene** (jedes Alter) | Erwachsene | Damen |
  | **Jugend** (unter 21) | Jugend | Mädchen |

  Teilnehmende unter 21 dürfen immer auch bei Erwachsenen bzw. Damen starten.
  „Unter 21“ gilt am Turniertag (Turnierdatum); ist nur der Jahrgang bekannt, zählt der Jahrgang
  (Turnier 2026 → Jugend = Jahrgang 2006 und jünger).
- Optional zusätzlich Jahrgang von (ältester) / bis (jüngster), z. B. Mädchen U15 → Mädchen, von 2012;
  Senioren 40 → Erwachsene, bis 1986.
- **Zuordnung nur von Hand** (einzige Automatik: der XML-Import). „In der Klasse“ = angemeldet,
  „Verfügbar“ = nicht angemeldet. Ein eigener Haken „angemeldet“ entfällt.
- **Teilnehmende neu erfassen** (Knopf im Bereich „Verfügbar“): Pflicht sind Vorname, Nachname, Geschlecht, Jahrgang,
  Verein und TTR (kein Geburtsdatum). Die Person steht danach unter „Verfügbar“ mit Wartelisten-Kreuz für diese
  Klasse und wird per Drag & Drop in die Klasse gezogen. Sie erscheint auch unter „Teilnehmende“.
- **Spalte „Warteliste“** unter „Verfügbar“: ✗ = steht für diese Klasse auf der Warteliste.
- **Höchstzahl** (optional beim Anlegen/Bearbeiten der Klasse, bei Doppel: max. Doppel): nur zur Kontrolle.
  Wer über der Höchstzahl liegt (die zuletzt Hinzugefügten), wird rot markiert. Die Spalte „Nr.“ zeigt die
  Reihenfolge der Anmeldung.
- Die Kriterien (Klassentyp, Jahrgang) sind nur ein Hinweis: „nur passende“ blendet Unpassende aus,
  von Hand zugeordnete Unpassende erscheinen orange mit ✗.
- **Linke Liste** („Verfügbar“): Teilnehmende des Turniers, durchsuchbar nach Namen. Bei **Doppelklassen** zusätzlich
  Filter nach einer **Einzelklasse**. Wer laut click-TT auf der Warteliste steht, ist grau markiert.
- **Drag & Drop**: Teilnehmende aus der Liste in die Klasse ziehen (oder auf eine Klasse in der Klassenliste),
  zurück in die Liste ziehen entfernt sie. Doppelklick und die runden Pfeil-Knöpfe gehen weiterhin.
- **Doppel**: rechts stehen die Paarungen als Karten. Eine Paarung besteht immer aus zwei Teilnehmenden.
  - Eine oder zwei Personen auf „Neues Doppel“ ziehen → neue Paarung
  - Eine Person auf eine Karte mit freiem Platz ziehen → Partner/in eintragen
  - Eine Person aus einer Karte in die linke Liste ziehen → aus dem Doppel entfernen;
    auf eine andere Karte ziehen → Partner/in wechseln
  - Mülleimer löst die Paarung auf
  - Jede Paarung zeigt ihren **Ø-TTR** = (TTR 1 + TTR 2) / 2.
  - Unvollständige Paarungen sind hell-orange markiert. Wer schon einer Paarung zugeordnet ist, ist in der
    Liste sofort grau – auch wenn der Partner/die Partnerin noch fehlt.
  - Beim click-TT-Import werden Doppelkonkurrenzen als Doppelklassen angelegt und die gemeldeten Paarungen übernommen.
- **Mehrfachmeldungen erlaubt**: Teilnehmende können in mehreren Klassen sein (z. B. Jugend und Herren).
- Beim click-TT-Import werden die Konkurrenzen der Datei automatisch als Klassen angelegt; der Klassentyp
  wird aus dem Namen geschätzt (Damen → Damen, Mädchen/Schülerinnen → Mädchen, Jugend/Jungen/U.. → Jugend,
  sonst Erwachsene) und lässt sich unter „Bearbeiten“ ändern.

**Klasse: Teilnehmende und Auslosung**
- Rechts steht fest der Klassenkopf (z. B. „Jungen U19“, darunter Einzel/Doppel, Anzahl in der Klasse, Klassentyp, Modus)
  und der Status (in Vorbereitung / ausgelost / bereit / läuft).
- Darunter zwei Reiter: **Teilnehmende der Klasse** (Zuordnung) und **Gruppenübersicht** bzw. **Turnierbaum**
  (vor der Moduswahl: „Auslosung“).
- Unter „Verfügbar“ filtern nach Name, **Verein** und **Geschlecht** (bei Doppel zusätzlich nach Einzelklasse).
- Ein Modus kann festgelegt werden, sobald mindestens **2 Teilnehmende** (Einzel) bzw. **2 vollständige Doppel**
  in der Klasse sind: **KO-System** oder **Gruppenphase (jeder gegen jeden)**.
- **Auslosen** / **Neu auslosen**: bis zum Klassenstart jederzeit möglich, mit oder ohne Setzung.
- **Auslosung verwerfen**: zurück zu „Teilnehmende der Klasse“. Solange ausgelost ist, ist die Zuordnung gesperrt.
- Nach der Auslosung: **Als bereit markieren** oder **Klasse starten** (einzeln). Ein Klassenstart hält die Startzeit
  der Klasse fest – und, falls noch nicht geschehen, die des Turniers. Danach lässt sich die Auslosung nicht mehr ändern.
- Gruppenübersicht: je Gruppe links die Mitglieder, rechts daneben ihre Spiele mit Status und Ergebnis.
  Turnierbaum: Spiele mit Status, Tisch und Ergebnis; Sieger rücken automatisch in die nächste Runde.

**Spielbetrieb**
- Spielstatus: **geplant** (ausgelost, Klasse noch nicht gestartet bzw. Gegner noch offen) → **eingereiht**
  (Warteschlange für freie Tische) → **aktiv** (an einem Tisch) → **abgeschlossen** (Ergebnis erfasst).
- **Turnier starten** (Seitenkopf): startet alle bereiten Klassen und reiht deren Spiele ein. Sind nicht alle Klassen
  bereit oder laufen schon welche, zeigt HTS vorher, welche. Nach dem Turnierstart: „Bereite Klassen starten“.
- **Tische**: alle Spieltische mit dem aktiven Spiel; rechts die Warteschlange. Freie Tische werden automatisch in
  Reihenfolge der Warteschlange belegt – wer gerade an einem anderen Tisch spielt, wird übersprungen.
  **Ergebnis eintragen** (Satz für Satz, siehe 0.10.0) macht den Tisch frei, das nächste Spiel rückt nach.
  Die Anzahl der Tische lässt sich hier ändern.
- **Spiele**: alle Spiele klassenübergreifend, standardmäßig aktiv & eingereiht; Filter nach Status und Klasse;
  Ergebnis eintragen per Knopf oder Doppelklick.

*KO-System** oder **Gruppenphase (jeder gegen jeden)**.
- **Auslosen** / **Neu auslosen**: jederzeit möglich, mit oder ohne Setzung, ohne Rückfrage.
- **Auslosung verwerfen**: löscht die Auslosung und springt zurück zur Auswahl der Teilnehmenden (Seite „Klassen“).
  Solange eine Klasse ausgelost ist, sind ihre Teilnehmenden festgelegt (Zuordnung dort gesperrt).
- Reiter **Übersicht** (KO-Baum bzw. Gruppen) und **Spiele** (Spielliste der Klasse mit Nr., Phase, Runde, Gegnern).

*KO-System*
- Feld wird auf 2, 4, 8, 16, … aufgefüllt; fehlende Plätze sind **Freilose** (nie zwei gegeneinander).
- Runden bis zum Finale (… Achtelfinale, Viertelfinale, Halbfinale, Finale), „Sieger Spiel x“.
- **Nach Setzung** (TTR, Doppel: Ø-TTR): Nr. 1 und 2 frühestens im Finale, Nr. 1/4 und 2/3 frühestens im Halbfinale;
  Freilose an die am besten Gesetzten. **Ohne Setzung**: komplett zufällig.
- Alle KO-Spiele werden bei der Auslosung angelegt; spätere Runden bekommen ihre Gegner, sobald die Vorrunde entschieden ist.

*Gruppenphase*
- Entweder **Gruppengröße** vorgeben (Anzahl Gruppen = Teilnehmende ÷ Größe, aufgerundet) oder **Anzahl Gruppen**
  (Größe = Teilnehmende ÷ Gruppen). Ungleich große Gruppen sind in Ordnung – kleinere haben weniger Spiele.
  Eine Vorschau zeigt Gruppen, Größen und Anzahl Spiele.
- **Nach Setzung**: Schlangensystem – höchster TTR wird Kopf von Gruppe A, zweithöchster Kopf von B … bis alle
  Gruppen einen Kopf haben; dann von der letzten zur ersten Gruppe weiter nach absteigendem TTR, dann wieder
  vorwärts usw. **Ohne Setzung**: komplett zufällig (gleiche Verteilung, zufällige Reihenfolge).
- Gruppenspiele „jeder gegen jeden“, Runde für Runde über alle Gruppen nummeriert (pro Runde spielt jede/r höchstens
  einmal) – so werden sie auch eingereiht.

## Teilnehmendenliste

**Liste**
- Spalten: Angemeldet (✓), Nachname, Vorname, Geburtsdatum, Geschlecht, Verein, TTR, Klassen, Lizenz-Nr.
- **Sortieren** nach Nachname, Vorname oder TTR (Klick auf den Spaltenkopf, Umschalt+Klick für eine zweite Sortierung)
- **Suche** mit eigenen Feldern für Vorname, Nachname und Geburtsdatum, dazu Filter nach Geschlecht und Status
  (Alle / Angemeldet / Warteliste)
  - Groß-/Kleinschreibung, Umlaute und Akzente spielen keine Rolle: „moller“, „moeller“ und „Möller“ finden „Möller“
  - Geburtsdatum: „1994“ findet den Jahrgang, „14.03.1994“ oder „03.1994“ ein genaues Datum
    (click-TT liefert normalerweise nur den Jahrgang)
- **Angemeldet**: ✓ = in mindestens einer Klasse. Nicht angemeldete Teilnehmende sind grau und kursiv.
  Statusfilter: Alle / angemeldet / nicht angemeldet / Warteliste.

**Import aus click-TT/nuLiga-Turnier-XML** (Seite „Teilnehmende“ → „XML der Teilnehmenden importieren“)
- Vorschau vor der Übernahme: Turnier, Konkurrenzen mit Meldezahlen und Warteliste, Anzahl neuer bzw. vorhandener Spieler
- Die Meldungen werden automatisch den Klassen zugeordnet (die einzige automatische Zuordnung).
- Wartelisten-Meldungen (`waiting-list-player="true"`) werden übernommen, aber **nicht** in die Klasse eingetragen;
  sie stehen unter „Verfügbar“ mit dem Hinweis „Warteliste“.
- Doppel-Meldungen: beide Partner werden als Spieler übernommen
- Wer in mehreren Konkurrenzen gemeldet ist, steht nur einmal in der Liste
- Erneuter Import legt keine Duplikate an, sondern aktualisiert (z. B. TTR). Ist jemand von der Warteliste nachgerückt,
  wird er eingetragen; eine Zuordnung wird durch einen Import nie entfernt.
  Erkennung über click-TT-ID, sonst Lizenz-Nr., sonst Name + Jahrgang + Verein

## Starten (Windows)

1. ZIP-Archiv **vollständig entpacken** (Rechtsklick → „Alle extrahieren …“), nicht direkt aus dem ZIP heraus starten.
2. Im entpackten Ordner doppelt auf `HTS starten.bat` klicken.

Eine Java-Installation ist nicht nötig: Im Ordner `jre` liegt eine Java-21-Laufzeit
(Eclipse Temurin, frei weitergebbar; Lizenztexte unter `jre/legal`).

Für den Quellcode-Stand gilt: ohne `jre`-Ordner wird ein installiertes Java 21 (JAVA_HOME oder Suchpfad) verwendet.

## Entwickeln

```
mvn test          # Tests
mvn javafx:run    # Starten
mvn package -Djavafx.platform=win   # ausführbare JAR für Windows: target/hts-0.26.0-all.jar
```

## Aufbau

| Paket | Inhalt |
|---|---|
| `de.hts.player` | `Player` (Spieler mit Meldungen je Klasse), `Sex`, `PlayerFilter` (Suche) |
| `de.hts.tournament` | `Tournament` (Turnier: Daten, Spieler, Klassen), `PlayingClass` (Spielklasse), `ClassType` (Erwachsene/Damen/Jugend/Mädchen), `Discipline` (Einzel/Doppel), `DoublesPair` (Doppelpaarung mit Ø-TTR), `Entrant` (Partei einer Begegnung), `TournamentMode`, `KnockoutDraw` (Auslosung, Setzliste), `KnockoutBracket` (Begegnungen), `GroupDraw` (Gruppen, Schlangensystem, jeder gegen jeden), `Match`/`MatchStatus` (Spiele, Status, Ergebnis) |
| `de.hts.persistence` | `TournamentFile` – Turnierdatei `.hts`, `TournamentStore` – Turnierordner (alle Turniere) |
| `de.hts.importer.clicktt` | `ClickTtXmlImporter` – liest `<person>`-Einträge aus der click-TT-XML |
| `de.hts.ui` | Kopfleiste, Turnierübersicht, Turnieransicht, Teilnehmendentabelle, Klassenansicht mit Gruppenübersicht/Turnierbaum, Tische, Spiele, Dialoge, Icons |

## click-TT-XML-Format (Auszug)

```xml
<tournament name="…" start-date="…">
  <competition age-group="Herren" type="Einzel" ttr-to="…">
    <players>
      <player type="single" id="PLAYER1" waiting-list-player="false">
        <person firstname="…" lastname="…" birthyear="1990" sex="1"
                club-name="…" club-nr="…" internal-nr="NU…" licence-nr="…"
                ttr="1500" ttr-match-count="…" nationality="DE"/>
      </player>
    </players>
  </competition>
</tournament>
```

`sex`: 1 = männlich, 2 = weiblich. Eine Beispieldatei mit erfundenen Daten liegt in `samples/`.

## Testdateien (`samples/`, erfundene Daten)

| Datei | Inhalt |
|---|---|
| `clicktt-beispiel.xml` | Herren/Damen Einzel und Herren Doppel, mit Warteliste |
| `testturnier-jugend.xml` | **Mädchen U15 Einzel**: 18 Meldungen (2 Warteliste), Jg. 2012–2015 · **Jungen U19 Einzel**: 30 Meldungen (2 Warteliste), Jg. 2008–2013 · TTR offen |
