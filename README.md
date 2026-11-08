# HTS – Hamburger Turniersoftware

Turniersoftware für Tischtennis, Java 21 + JavaFX.

Copyright © 2026 Richard Michaelis · freie Software unter der [GNU General Public License v3.0](LICENSE) ·
Wünsche und Fehler bitte als [Issue](https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/issues)
(Label „Feature“ bzw. „bug“).

## Neu in 0.39.0

- **Teilnehmende – Setzplatz:** Ist auf eine per XML importierte Klasse gefiltert, zeigt die Spalte „Setzplatz“ die
  Rangfolge nach TTR in der Klasse (gleicher TTR = gleicher Platz, ohne TTR ganz unten, Doppel nach Ø-TTR).
- **Vereinsfilter durchsuchbar:** Tippen filtert die Liste der Vereine (Teil des Namens genügt); die Tabelle ändert
  sich erst, wenn ein Verein gewählt ist. Auf „Teilnehmende“ und in den Klassen.
- **Einheitliche Menüs:** Aktions- und Einstellungsmenüs im Design der Auswahl „Gleichstand“ mit erklärendem Text
  („Aktion für Markierte wählen“, „Handyabmeldungen bearbeiten“, „PDF / Drucken“); Filter im hellgrauen Design.
- **Teilnehmende:** „Abgemeldete als PDF“ steht rechts neben den Menüs; „Handyabmeldungen bearbeiten“ erscheint nur bei
  laufendem Handy-Zugang oder offenen Handyabmeldungen.
- **Klassenansicht:** Suche, „Gleichstand“, alle Knöpfe und die Infozeile in einer Zeile (Umbruch nur, wenn es nicht
  passt); Reihenfolge: „Auslosung verwerfen“ bzw. „Zurück zu Phase …“, „Neu auslosen“, „bereit“, „starten“ bzw. die
  Phasen-Knöpfe, zuletzt „PDF / Drucken“.

## Neu in 0.38.0

- **Gruppenauslosung – unvermeidbare Vereinsgleichheit:** Hat ein Verein mehr Teilnehmende als es Gruppen gibt, kommt
  der/die Schwächste zum/zur Stärksten des Vereins, der/die Zweitschwächste zum/zur Zweitstärksten usw.; ab drei je
  Gruppe wechselt die Richtung wie bei der Schlange (z. B. 4 Gruppen, 12 Gleiche: 1/8/9 · 2/7/10 · 3/6/11 · 4/5/12).
  Gilt für alle Setzungsarten, nicht für Doppel.
- **Gruppenspiele:** Gruppen mit mehr Runden (z. B. 5er-Gruppen) beginnen früher, damit alle Gruppen etwa gleichzeitig
  fertig werden; die Spielnummern folgen dieser Reihenfolge.
- **Ergebnis eintragen:** Ein Satz ohne Vorzeichen gilt als „+“ („8“ = 11:8, „10“ = 12:10).
- **Qualifikationsphase:** wird ausgelost, aber erst mit „Phase starten“ gestartet – bis dahin lassen sich die Spieler
  per Drag & Drop tauschen. Nach Runde 1 fragt HTS, ob die Gewinner qualifiziert sind (die Verlierer sind dann
  Nachrücker 1. Ranges nach TTR) oder eine weitere KO-Runde spielen; die Zusatzrunde entfällt. Überall steht die
  Herkunft „A2“, „B3“ statt nur des Gruppenbuchstabens (Karten, Listen, Präsentation, Live-Anzeige, PDF).
- **KO als Folgephase:** entweder die Besten je Gruppe oder bestimmte Gruppenplätze (z. B. „2,3,4“).
- **Phase vorzeitig beenden** (offene Spiele „nicht gespielt“, nicht im XML-Export; zurücknehmbar) und **Phase neu
  starten** (Auslosung bleibt, alle Ergebnisse werden zurückgesetzt).
- **Klassenkopf kompakter:** Name und Kurzinfo in einer Zeile, eine Werkzeugzeile mit Suche, „Gleichstand: …“,
  „PDF / Drucken ▾“ (Gruppenauswahl im Menü) und den Phasen-Knöpfen.
- **Tische den Klassen zuordnen:** Haken = Klasse darf an diesem Tisch spielen; standardmäßig sind alle Haken gesetzt.
  Ein Tisch ohne Haken bekommt keine Spiele, bleibt aber aktiv („keine Klasse zugeordnet“).
- **Spiele:** neuer Filter „Gruppe“, die Spalte „Phase“ heißt „Gruppe“.
- **Teilnehmende:** „Abgemeldete als PDF“ (je Klasse, mit „abgemeldet durch Handy/Turnierleitung“); reine Nachrücker
  haben keinen An-/Abmeldestatus und werden bei Übernahme in die Klasse automatisch angemeldet.
- **Klasse löschen:** Rückfrage nennt, was verloren geht („Abbrechen“ vorausgewählt), bei Ergebnissen eine zweite
  Rückfrage; auch mit der Taste „Entf“ in der Klassenliste.
- **Fehler behoben:** Nach dem Zuordnen von Tischen zu Klassen entstanden beim erneuten Öffnen leere Kopien der Klassen
  (viele Klassen gleichen Namens). Betroffene Turnierdateien werden beim Öffnen automatisch repariert.

## Neu in 0.37.0

- **Neuer Menüpunkt „Mobile Einstellungen“:** „Handy-Zugang starten“, „QR-Codes drucken“ und „Anmeldung per Adresse
  sperren“ haben eine eigene Seite mit Zustand (läuft/aus, Anmeldung offen/gesperrt), den Adressen der Live-Anzeige und
  der Anmeldung samt QR-Code-Vorschau und Hinweisen zu WLAN und Firewall.
- **Teilnehmende:** Auswahl „Verein:“ statt „Status:“ (kombinierbar mit Suche und Klassenfilter); der Knopf
  „Zurücksetzen“ entfällt. Auf der Seite bleiben „Aktion bezüglich Markierte“ und „Handyabmeldungen“.
- **Klassen:** Statt des grauen Bedienhinweises steht links neben „Teilnehmende fertig – Modus festlegen“ der Hinweis
  „XX Gemeldete noch nicht an- oder abgemeldet.“ mit dem Link „Offene Personen anzeigen“ (öffnet „Teilnehmende“ mit
  dieser Klasse im Filter). Die Marke „Start gesperrt“ steht im Klassenkopf; die rote Zeile darunter entfällt.

## Neu in 0.36.0

- **Texte nie abgekürzt, auch bei kleinen Auflösungen** (geprüft bei 1366 × 768 mit 125 % und 150 % Skalierung):
  Tabellenspalten so breit wie ihr Inhalt (notfalls waagerechte Scrollleiste), KO- und Qualifikationskarten so breit
  wie der längste Name samt Verein, Dialoge scrollen statt abzuschneiden, Menü- und Knopfzeilen brechen um, die Seite
  scrollt bei wenig Höhe, der Turniername im Kopf bricht um.
- **Teilnehmende:** Menüs „Aktion bezüglich Markierte“ (angemeldet, endgültig abmelden, Abmeldung zurücknehmen) und
  „Handyabmeldungen“ (alle bestätigen, Markierte bzw. alle zurück auf offen); Rechtsklick auf eine Zeile mit
  angemeldet / endgültig abmelden / Abmeldung zurücknehmen bzw. bestätigen. Neuer **Klassenfilter** (gemeldet, in
  der Klasse, Nachrücker) – kombinierbar mit dem Status.
- **Sortierung nach TTR:** Personen ohne TTR stehen in beiden Richtungen unten.
- **click-TT-Import:** Klassen lassen sich vor dem Import abwählen (Haken je Konkurrenz); wer nur in abgewählten
  Klassen gemeldet ist, wird nicht übernommen; die Zahlen im Dialog passen sich an.
- **Endergebnis Qualifikation:** Die über die Gruppenplatzierung Qualifizierten teilen sich die ersten Plätze
  (z. B. 1.–16.), die Gewinner der KO-Runde die Folgeplätze (z. B. 17.–22.); Nachrücker behalten Einzelplätze.

## Neu in 0.35.0

- **Anwesenheit am Turniertag:** Die Spalte „Angemeldet“ der Teilnehmenden heißt jetzt „am Turniertag anwesend“ und
  lässt sich abhaken. Neues Menü „Anwesenheit“: Markierte anmelden, endgültig abmelden (gelb hervorgehoben),
  Abmeldungen per Handy bestätigen (orange), zurücknehmen. Eigene Spalte „Nachrücker“ (Warteliste aus click-TT).
- **Meldungen statt „In der Klasse“:** Der click-TT-Import legt die Gemeldeten als Meldung unter „Verfügbar“ an –
  „In der Klasse“ beginnt bei 0. Unter „Verfügbar“ stehen nur Angemeldete (anwesend); „Alle anwesenden Gemeldeten
  übernehmen“ holt sie in die Klasse (Doppel mit dem gemeldeten Partner). Von Hand erfasste Personen gelten sofort
  als angemeldet. Ältere Turnierdateien: noch nicht ausgeloste Klassen werden so umgestellt.
- **Startsperre für importierte Klassen:** Klassen „aus XML importiert“ starten erst, wenn alle Gemeldeten
  angemeldet oder endgültig abgemeldet sind („Turnier starten“ startet die übrigen und nennt die offenen).
- **Handy-Zugang im lokalen WLAN** (Knopf unter „Teilnehmende“, Port 8080, kein Internet nötig):
  - Live-Anzeige (nur lesend): Tische und laufende Spiele, Klassen mit Gruppen und KO-Runden, Ergebnisse,
    „Meine Spiele“ / „Meine Gruppe“ – das Handy merkt sich den Namen; aktualisiert sich alle 10 Sekunden.
  - Anmeldung am Turniertag über einen QR-Code mit Schlüssel: Teilnehmende melden sich selbst an, Betreuende wählen
    ihren Verein und haken die Anwesenden ab; „nicht anwesend“ bestätigt die Turnierleitung. Die Anmeldung lässt sich
    sperren und wieder öffnen; sie endet, wenn alle Klassen beendet sind.
  - „QR-Codes drucken“: A4-Aushang mit beiden QR-Codes und Feldern für WLAN-Name und Passwort.
  - Beim ersten Start fragt die Windows-Firewall nach – „Private Netzwerke“ erlauben. Gäste-WLANs, die Geräte
    voneinander trennen, funktionieren nicht.

## Neu in 0.34.0

- **KO-Qualifikation „insgesamt“:** Im Dialog „Phase abschließen“ lässt sich für die KO-Qualifikation wählen, ob die
  Zahl „je Gruppe“ (wie bisher) oder „insgesamt“ gilt. Beispiel HEM: 16 Gruppen, Gruppenerste direkt, 24 insgesamt
  → alle Gruppenzweiten und die 8 besten Gruppendritten (Spiel-, Satz-, Punktverhältnis, Gleichstand Los). Die
  übrigen Dritten sind nur Platzierte; „Nachrücker je Gruppe (ohne Spiel)“ zählt ab dem ersten Nicht-KO-Teilnehmenden
  jeder Gruppe.
- **Geht die KO-Qualifikation nicht auf**, wird das Feld rot hervorgehoben, darunter steht der Grund und „Phase
  abschließen“ ist gesperrt.
- **Qualifikationsansicht nutzt die ganze Breite:** Die Spielkarten wachsen mit dem Fenster (mehr Karten je Reihe,
  längere Namen sichtbar); TTR und Tischname werden nicht mehr abgeschnitten.

## Neu in 0.33.0

- **Qualifikationsmodus überarbeitet:** Wer in der KO-Qualifikation 2 Spiele gewinnt, ist qualifiziert (die
  Einstellung „qualifiziert nach n Siegen“ entfällt).
  - Runde 1: besser gegen schlechter Platzierte (möglichst andere Gruppe/anderer Verein); bleiben nur gleiche
    Gruppenplätze übrig, spielen diese gegeneinander. Bei ungerader Zahl Freilos für den Stärksten nach der
    Gruppenphase (Gruppenplatz, Spiel-, Satz-, Ballverhältnis, TTR) – das Freilos zählt als Sieg.
  - Runde 2 frei gelost, ohne Freilos: die KO-Qualifikation muss dafür aufgehen (3, 4, 7, 8, 11, 12 … Teilnehmende),
    sonst lässt sich die Phase nicht anlegen und HTS nennt passende Möglichkeiten.
  - Zusatzrunde (wählbar im Phasen-Reiter) für die Verlierer der Runde 2: deren Gewinner sind ebenfalls qualifiziert
    (bei ungerader Zahl Freilos für den Bestplatzierten).
  - Nachrücker, fortlaufend nummeriert: Verlierer der Runde 2 bzw. der Zusatzrunde (Gruppenplatz, Verhältnisse,
    TTR), dann Verlierer der Runde 1 (nur nach TTR), dann die Nachrücker aus der Gruppenphase. Ohne KO-Runde sind
    die Nachrücker aus der Gruppenphase „Nachrücker 1. Ranges“.
- **Qualifikationsansicht:** Namen in den Spielkarten (fehlten bisher); Qualifizierte und Nachrücker als nummerierte
  Liste in zwei Spalten mit Verein, Gruppenplatz (z. B. „D3“) und TTR – alle Einträge, Namen vollständig.
- **Endergebnis:** „Qualifiziert durch Gruppenplatzierung“, „Qualifiziert durch KO-Runde“, „Nachrücker 1./2./…
  Ranges“ statt nur „qualifiziert“ (auch im PDF); Spaltentitel linksbündig wie die Werte (auch in den anderen
  Tabellen).
- **Zurück zur vorherigen Phase:** Solange in der letzten Phase noch kein Ergebnis eingetragen ist, gibt es
  „Auslosung verwerfen“ (Phase 1 → „Teilnehmende der Klasse“, auch nach dem Start) bzw. „Zurück zu Phase 1“
  (Phase 2 wird gelöscht, Phase 1 ist wieder offen) – mit Sicherheitsabfrage; Spiele an Tischen werden
  zurückgezogen, die Tische sind sofort frei.
- **Suche im KO-Baum und in den Gruppen deutlich schneller:** Ausgewertet wird 0,25 s nach dem letzten Tastendruck,
  und es werden nur noch Hervorhebungen umgeschaltet statt die Ansicht neu aufzubauen.

## Neu in 0.32.0

- **2er und 3er Turnier (nur Vorgabeturnier):** neue Spielarten für Mannschaftskämpfe. Teams wie Doppelpaarungen
  bilden (Drag & Drop, „Teams zulosen“), Teamname aus den Nachnamen; Option „mindestens 1 Dame je Team“.
  - Eine Begegnung Team gegen Team ist ein Spiel an einem Tisch; es werden immer alle Partien gespielt.
  - 3er Turnier: 5 Partien – mit Dame Mixed (H2/D), Herren-Einzel (H1), Damen-Einzel (D), Herren-Doppel (H1/H2),
    Mixed (H1/D); sonst Doppel 1 (T2/T3), Einzel 1 (T1), Einzel 2 (T3), Doppel 2 (T1/T2), Doppel 3 (T1/T3).
  - 2er Turnier: Gruppenphase 3 Partien (A1–B1, A2–B2, Doppel), KO-System 5 (zusätzlich A1–B2, A2–B1); mit Dame ist
    sie automatisch Teilnehmende/r 2.
  - Ergebnis: Aufstellung beider Teams wählen und je Partie den Sieger eintragen (mit Vorgabe je Partie);
    Gruppenwertung nach Begegnungen, Partien, festem Los.
  - Spielzettel DIN A4 je Begegnung: Schema der Partien, freie Felder für die Aufstellung, Teams mit TTR und die
    Vorgabe-Tabelle aus den Vorgaberegeln.
- **„Turnier starten“** erst mit mindestens 2 Teilnehmenden, 1 Klasse und 1 Tisch – sonst deaktiviert mit Hinweis.
- **Turnierübersicht:** Chip „Fertiges Turnier per XML importiert“; der Chip „gestartet …“ bleibt auch auf der
  markierten Karte weiß.
- **Klasse anlegen:** Auswahllisten statt Radio-Buttons (so breit wie „Name“); im Vorgabeturnier ohne „Alter“.
- **Vorgaberegeln:** übersichtlichere Tabellen – Hinweis unter der Überschrift, deutliche Spaltenköpfe, gestrichelte
  Trennlinien zwischen den Spaltengruppen.
- **Bibliotheken:** JavaFX 21.0.8 und Apache PDFBox 3.0.8 (Java-Laufzeit bereits 21.0.13).

## Neu in 0.31.0

- **Vorgabeturnier:** neue Option beim Anlegen eines Turniers (später nicht umschaltbar). TTR-Unterschiede werden
  durch einen Punktevorsprung ausgeglichen – die Partei mit Vorsprung beginnt jeden Satz mit X:0.
  - Eigener Menüpunkt **„Vorgaberegeln“**; bis die Art der Vorgabe gespeichert ist, gibt es nur „Teilnehmende“ und
    „Vorgaberegeln“. Speichern nur per Knopf mit Rückfrage, Warnung bei ungespeicherten Änderungen; nach dem Start
    der ersten Klasse nur noch lesbar.
  - Art der Vorgabe: **TTR-Klassen** (2–7 Klassen, Bälle je Klassenunterschied) oder **Fester TTR-Abstand**
    (2–10 Kategorien, Bälle je Kategorie); Grenzen getrennt für Erwachsene und Damen, lückenlos („>“ / „<=“), alle
    Werte frei pflegbar. Aufschlag gelost oder für die Partei ohne Vorsprung; Halbierung im Doppel einstellbar.
  - Doppel: TTR-Höhere und TTR-Tiefere werden verglichen (Mixed: Herr–Herr, Dame–Dame), Vorgaben verrechnet.
  - Anzeige „+X“ in Gruppen-, KO-, Tisch-, Spiele- und Präsentationsansicht, im Ergebnisdialog und in den PDFs;
    Spielzettel mit „+ X Punkte“ unter dem Namen.
  - Start einer Klasse gesperrt bei fehlendem TTR oder TTR/Abstand über der höchsten Grenze (mit Hinweis).
  - Klassentypen nur „Erwachsene“ und „Damen“; Verein und Jahrgang keine Pflicht; kein click-TT-Export.
- **CSV-Import für Vorgabeturniere:** Vorname, Nachname, TTR-Wert, Klasse (Erwachsene/Damen), Komma, UTF-8, ab
  Zeile 2; ersetzt alle Teilnehmenden (nur vor der ersten Auslosung, mit Rückfrage); ignorierte Zeilen werden
  gemeldet. Vorlage: „CSV-Vorlage speichern“ bzw. `Vorlage-Teilnehmende-Vorgabeturnier.csv`.
- **Java-Laufzeit** auf Temurin 21.0.13 aktualisiert. (JavaFX und PDFBox folgen, sobald die Dateien vorliegen.)

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
