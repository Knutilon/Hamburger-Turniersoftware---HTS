<!--
  HTS – Dokumentation (Markdown-Quelle)
  Diese Datei ist die fortschreibbare Fassung der Dokumentation. Für neue Versionen:
  einfach diese Datei (am besten zusammen mit dem Quellcode-ZIP) wieder an Claude geben
  und um die Änderungen der neuen Version erweitern lassen.
  Stand: 04.11.2026 · Programmversion 0.30.0
-->

# HTS – Hamburger Turniersoftware: Dokumentation

Stand: 04.11.2026 · Programmversion **0.30.0**

## Versionsverlauf der Dokumentation

| Datum | Programmversion | Änderung |
| --- | --- | --- |
| 04.11.2026 | 0.30.0 | Nachtrag 0.29.0 und 0.30.0: Präsentationsansicht für den zweiten Bildschirm (Auswahl je Klasse mit Status, Seiten ohne zerschnittene Karten, Gruppen im 2×2-Raster, Tische nach Auflösung), Info-Knopf und Lizenz GPL 3.0, kompakter Kopfbereich, Klasse öffnet die aktuellste Phase, Setzung „Gruppenkopf gesetzt“, Import (Einzel direkt in die Klasse, Doppel ohne Partner/in unter „Verfügbar“), Vereinsfilter nur noch im Doppel, Suche und automatisches Blättern bei den Doppelpaarungen, Doppel-TTR bei fehlenden Werten, festes Los als letztes Kriterium in Tabellen |
| 03.11.2026 | 0.28.0 | Nachtrag der Versionen 0.21.0–0.28.0: Testturnier und Simulation, Turnier kopieren, fertiges Turnier aus click-TT importieren, Suche über Vorname/Nachname/Verein (auch im KO-Baum, Enter springt zum nächsten Treffer), KO-Baum mit Pfaden, fixierten Rundentiteln und Statusfarben, Schalter „Spiel um Platz 3“, Doppelregeln und „Partner zulosen“, Qualifikationsmodus neu (HEM-Logik, Nachrücker je Gruppe, freie Auslosung, Zusatzrunde), Tische benennen und deaktivieren, neue PDFs (KO-Turnierbaum, Gruppen zweispaltig), Leistung und Hardware |
| 31.10.2026 | 0.20.0 | Spiel um Platz 3, Gewinnsätze je Klasse (3/4), „A1“-Kennungen im KO-Baum der Phase 2, Schweizer System ohne Folgephase, Vereinsgleichheit in der Qualifikation, neue Speicherorte „Dokumente\HTS Turniere“ |
| 31.10.2026 | 0.19.0 | höchstens 2 Phasen je Klasse; Qualifikationsmodus neu: Nachrückerränge, Verlierer spielen weiter, Q = 0 möglich, geteilte Gruppenplätze |
| 30.10.2026 | 0.18.0 | Qualifikationsmodus (Abschnitt 8), Vereinsnamen in KO-Darstellungen |
| 30.10.2026 | 0.17.0 | erste vollständige Fassung |

## 1. Überblick

HTS (Hamburger Turniersoftware) organisiert Tischtennisturniere von der Meldeliste bis zur click-TT-Ergebnisdatei. Diese Dokumentation beschreibt Version 0.30.0. HTS ist freie Software unter der GNU General Public License v3.0 (GPL 3.0); Wünsche und Fehler nimmt das GitHub-Projekt als Issue entgegen (Label „Feature“ bzw. „bug“).

Das Programm deckt den ganzen Turniertag ab:

- Teilnehmende aus click-TT importieren oder von Hand erfassen
- Klassen (Konkurrenzen) anlegen und Teilnehmende zuordnen, auch Doppel
- Modus festlegen und auslosen: KO-System, Gruppenphase oder Schweizer System
- bis zu zwei Phasen je Klasse, z. B. Gruppen, dann KO-System
- Tische automatisch belegen, Spielzettel drucken, Ergebnisse Satz für Satz erfassen
- Endergebnisse als PDF und die Ergebnisdatei für den click-TT-Upload erzeugen
- Qualifikationen nach dem Muster der Hamburger Einzelmeisterschaften (HEM): Vor- und Hauptqualifikation mit Nachrückerrängen
- zum Üben: Testturniere, in denen alle Spiele per Knopfdruck simuliert werden
- eine Präsentationsansicht für einen zweiten Bildschirm (Beamer, Fernseher) mit wechselnden Ansichten

### Systemvoraussetzungen

| Punkt | Mindestens | Empfohlen |
| --- | --- | --- |
| Betriebssystem | Windows 10 oder 11, 64 Bit (x64) | Windows 11 |
| Java | nicht nötig, eine Java-Laufzeit (Temurin 21) liegt bei | – |
| Prozessor | Zweikern-Prozessor der letzten rund 10 Jahre | Vierkern-Prozessor |
| Arbeitsspeicher | 4 GB | 8 GB |
| Speicherplatz | ca. 200 MB für das Programm, wenige MB je Turnier | – |
| Bildschirm | 1366 × 768 Pixel | 1920 × 1080 oder größer; für die Turnierleitung gern ein zweiter Bildschirm |
| Grafik | beliebig; ohne passenden Grafiktreiber (z. B. in einer virtuellen Maschine) zeichnet JavaFX per Software – langsamer, aber lauffähig | aktueller Grafiktreiber (Direct3D) |
| Drucker | optional; für Spielzettel ein Standarddrucker, ideal mit A6-Papier | – |
| Internet | nicht nötig, HTS läuft vollständig offline | – |

Windows auf ARM-Prozessoren sollte über die x64-Emulation von Windows 11 funktionieren, ist aber nicht geprüft.

### Leistung

Gemessen mit einem großen Testturnier (1500 Teilnehmende, 40 Tische, KO-Felder mit 32, 64, 128 und 256 Plätzen, Gruppenphase mit 300 Spielern in 60 Gruppen) auf einem Zweikern-Rechner (Xeon, 2,1 GHz) mit Software-Darstellung. Auf einem Windows-PC mit Grafikbeschleunigung ist die Darstellung eher schneller.

| Vorgang | Dauer |
| --- | --- |
| 1500 Teilnehmende importieren / zusammenführen | ca. 5 ms |
| KO-Feld mit 128 auslosen (Setzung und Vereinsprüfung) / 60 Gruppen auslosen | ca. 2 ms / ca. 7 ms |
| Ergebnis eintragen inkl. neuer Tischvergabe | unter 0,1 ms je Spiel |
| Phase 2 (KO aus 60 Gruppen) anlegen | ca. 20 ms |
| Turnierdatei speichern / laden (ca. 460 KB) | ca. 45 ms / ca. 80 ms |
| Suche über 1500 Personen | ca. 5 ms |
| Ansicht öffnen: KO-Baum 128er-Feld / 256er-Feld | ca. 0,4 s / ca. 0,4–1,2 s |
| Ansicht öffnen: Gruppenphase mit 60 Gruppen | ca. 0,8–1 s (beim allerersten Öffnen bis 2,5 s) |

Speicher: Die Turnierdaten selbst brauchen nur wenige MB. Den meisten Platz belegt die gerade angezeigte Ansicht – ein 256er-KO-Baum rund 190 MB, eine Gruppenphase mit 60 Gruppen rund 260 MB Java-Speicher. Beim Wechsel der Ansicht wird der Speicher wieder frei (gemessen: kein Anwachsen bei wiederholtem Wechsel). Mit Java-Laufzeit, Grafik und Puffern belegt der Prozess bei solchen Ansichten bis rund 1 GB Arbeitsspeicher; daher die Empfehlung von 8 GB. Bei üblichen Turnieren (bis etwa 64er-Felder, 20 Gruppen) ist es deutlich weniger.

### Installation und Start

1. Die Datei `hts-<Version>-windows.zip` in einen beliebigen Ordner entpacken (Rechtsklick → „Alle extrahieren …“, nicht direkt aus dem ZIP starten).
2. Im Ordner `hts-dist` auf **HTS starten.bat** doppelklicken.
3. Es öffnet sich die Turnierübersicht. Eine Installation oder Administratorrechte sind nicht nötig.

Für ein Update wird die neue ZIP-Datei ebenso entpackt. Die Turniere liegen getrennt vom Programm und bleiben erhalten. Meldet die Startdatei „keine Java-Laufzeit gefunden“, fehlt neben `hts.jar` der Ordner `jre` – meist wurde aus dem ZIP heraus gestartet oder die Startdatei verschoben (für den Desktop nur eine Verknüpfung anlegen).

### Wo die Daten liegen

Stammordner ist `Dokumente\HTS Turniere` (auch wenn der Dokumente-Ordner per OneDrive umgeleitet ist). Jedes Turnier bekommt dort einen eigenen Ordner mit seinem Namen:

```
Dokumente\HTS Turniere\
    Herbstturnier 2026\
        Turnierdatei\    Herbstturnier 2026.hts (und die Kopie der click-TT-Importdatei)
        XML Export\      click-TT-Ergebnisdatei „Ergebnisse - … .xml“
        PDF Export\      alle PDFs: Gesamtergebnis, Phasen, Endergebnisse, Spielzettel
```

- Die Ordner entstehen automatisch. Gibt es den Turniernamen schon, heißt der Ordner z. B. „Herbstturnier 2026 (2)“.
- **Turnierdatei woanders:** Beim Anlegen (und unter **Bearbeiten**) wählt **Durchsuchen** einen anderen Ordner; HTS merkt sich die Datei und zeigt sie weiter in der Übersicht.
- **Exporte woanders:** Im Speichern-Dialog (PDF-Viewer bzw. click-TT-Export) einfach einen anderen Ordner wählen. Dieser Ordner gilt für dieses Turnier, bis wieder ein anderer gewählt wird.
- Turniere aus der alten Ablage `<Benutzerordner>\HTS\Turniere` werden beim ersten Start ab Version 0.20.0 automatisch in die neue Struktur übernommen; die alten Dateien heißen danach `….hts.uebernommen`.

Gespeichert wird automatisch nach jeder Änderung.

## 2. Programmaufbau und Bedienung

HTS hat zwei Ebenen: die Turnierübersicht und die Ansicht eines geöffneten Turniers mit fünf Seiten.

### Kopfleiste

Die dunkelblaue Kopfleiste zeigt links das HTS-Logo und daneben das geöffnete Turnier („Turnier: Name · Datum“, bei Testturnieren „TESTTURNIER: …“). Darunter liegt das Menü, rechts oben der runde Knopf zurück zur Turnierübersicht. Bei Testturnieren steht daneben ein Würfel-Knopf (Abschnitt 3). Ganz rechts steht immer der **Info-Knopf (i)**: Er zeigt Version, Signatur, die Lizenz (GPL 3.0), Links zur Feature- und Bug-Sammlung auf GitHub sowie die Lizenzen der mitgelieferten Bibliotheken. In der Turnierübersicht gibt es nur den Info-Knopf; neue Turniere legt der Knopf **Neues Turnier** auf der Seite an.

| Menüpunkt | Zweck |
| --- | --- |
| ← Turniere | zurück zur Turnierübersicht |
| Teilnehmende | Meldeliste: importieren, erfassen, suchen |
| Klassen | Klassen anlegen, Teilnehmende zuordnen, Modus festlegen, Phasen und Endergebnis |
| Tische | belegte und freie Tische, Warteschlange, Tischzuordnung, Tischnamen |
| Spiele | alle Spiele klassenübergreifend, Suche, Ergebnisse und Korrekturen, Export-Markierung |
| Präsentationsansicht | Anzeige auf einem zweiten Bildschirm starten, Wechselzeit und Ansichten wählen (Abschnitt 6) |

„Spiele“ ist ausgegraut, bis das Turnier oder eine Klasse gestartet ist.

### Bedienregeln

- **Automatisches Speichern:** Jede Änderung wird sofort gespeichert; es gibt keinen Speichern-Knopf.
- **Drag & Drop:** Teilnehmende, Doppelpartner, Gruppenmitglieder und KO-Plätze werden mit der Maus gezogen. Erreicht der Mauszeiger beim Ziehen den oberen oder unteren (im KO-Baum auch linken oder rechten) Rand der Ansicht, blättert sie automatisch weiter – je näher am Rand, desto schneller.
- **Suche:** Alle Suchfelder in Klassen, Gruppenphase, KO-Baum und auf der Seite „Spiele“ suchen in **Vorname, Nachname und Verein**. Mehrere Wörter (durch Leerzeichen getrennt) müssen alle passen, jedes darf in einem der drei Felder stehen, auch als Wortteil; Groß-/Kleinschreibung, Umlaute (ü = ue), Reihenfolge und Satzzeichen am Wortrand spielen keine Rolle. Beispiel Richard Michaelis, Grün-Weiß Harburg: Treffer bei „Richard Michaelis“, „Michaelis Harburg“, „richard grün“ oder nur „Harburg“ (dann auch alle anderen aus Vereinen mit „Harburg“ im Namen). Doppel werden gefunden, wenn eine der beiden Personen passt.
- **KO-Darstellungen** (KO-Baum, Spiele der KO-Qualifikation, Spielplan im PDF) zeigen hinter jedem Namen den Verein.
- **Doppelklick:** öffnet in Gruppenübersicht, KO-Baum und Spieleliste die Ergebniseingabe.
- **Knöpfe** sind immer ausgeschrieben; wird es eng, bricht eine Knopfzeile um.
- **Dialoge** passen ihre Größe dem Inhalt an.
- **Kopfbereich einer Seite:** Unter dem Seitentitel stehen Turniername, Datum und – sobald gestartet – „● Gestartet hh:mm Uhr“. Rechts oben stehen die Knöpfe der Seite: „Turnier starten“, „Bereite Klassen starten“, nach Turnierende „Ergebnisse exportieren“ und auf der Seite „Klassen“ **Klasse anlegen**.

## 3. Turniere und Teilnehmende

### Turniere anlegen und verwalten

In der Turnierübersicht legt **Neues Turnier** ein Turnier an. Pflicht sind Name, Datum und die Anzahl der Spieltische; das Turnier ist sofort gespeichert.

- Jede Karte zeigt Datum, Anzahl Teilnehmende und Klassen sowie den Status.
- **Öffnen** oder Doppelklick öffnet das Turnier, **Bearbeiten** ändert Name, Datum, Spieltische und den Ort der Turnierdatei, **Löschen** entfernt die Turnierdatei (Exporte bleiben erhalten).
- Beim Anlegen zeigt der Dialog, wo die Turnierdatei landet (Standard: `Dokumente\HTS Turniere\<Name>\Turnierdatei`); **Durchsuchen** wählt einen anderen Ordner (Abschnitt 1).
- **Turnierdatei übernehmen** holt eine vorhandene `.hts`-Datei als neues Turnier herein.
- **Fertiges Turnier importieren (click-TT)** liest eine click-TT-Ergebnisdatei (Format TournamentPortal, egal ob aus click-TT, poinTT oder HTS) und legt daraus ein neues, abgeschlossenes Turnier an: je Konkurrenz eine Klasse, Phase 1 als Gruppenphase oder KO-System (inkl. Spiel um Platz 3), alle Spiele mit Sätzen und die Platzierungen. Nicht Angetretene stehen ohne Klasse in der Teilnehmendenliste. Spätere Phasen aus der Datei werden nicht übernommen, sondern im Bericht genannt; Unstimmigkeiten erscheinen als Hinweise statt eines Abbruchs.
- **Kopie von:** Beim Anlegen lässt sich ein bestehendes Turnier als Vorlage wählen – **ohne Auslosung** (Teilnehmende, Klassen, Zuordnungen, Einstellungen, Tische; Standard), **alles** (inkl. Auslosungen, Spielen und Ergebnissen) oder **nur Teilnehmende** (ohne Klassen). Das Original bleibt unverändert.
- **Testturnier:** Das Häkchen beim Anlegen bzw. Bearbeiten markiert ein Übungsturnier (Karte „Testturnier“). Dort trägt der Würfel-Knopf in der Kopfleiste **alle offenen Spiele der laufenden Phasen** zufällig und regelgerecht ein (Best of 5 bzw. 7, Verlängerung bis höchstens 20:18), inklusive der Folgerunden im KO- und Schweizer System – ohne Spielzettel zu drucken und ohne Phasen abzuschließen. Angehaltene Phasen werden nicht simuliert. Exporte sind auch für Testturniere möglich.

### Teilnehmende aus click-TT importieren

Auf der Seite „Teilnehmende“ liest **XML der Teilnehmenden importieren** den Turnierexport aus click-TT ein.

1. XML-Datei wählen; HTS zeigt eine Vorschau mit Teilnehmenden und Konkurrenzen.
2. Nach dem Bestätigen werden fehlende Klassen angelegt. **Einzel:** Die Gemeldeten stehen direkt „In der Klasse“ – auch in neu angelegten Klassen.
3. **Doppel:** In click-TT gemeldete Paare werden als Doppelpaarungen übernommen. Wer in der Datei **ohne Partner/in** gemeldet ist, wird kein halbes Doppel, sondern steht in der Doppelklasse unter „Verfügbar“ (zählt bei „Partner zulosen“ mit).
4. Wartelisten-Meldungen (Nachrücker) kommen nicht in die Klasse, sondern auf die Warteliste.

Ein erneuter Import ergänzt und aktualisiert, löscht aber niemanden. HTS legt eine Kopie der importierten Datei ab; sie ist die Grundlage für den späteren click-TT-Export (Abschnitt 10).

### Von Hand erfassen

**Teilnehmende neu erfassen** fragt Vorname, Nachname, Geschlecht, Jahrgang, Verein und TTR ab; alle Felder sind Pflicht. In der Klassenansicht kann die neue Person direkt der Klasse zugeordnet werden.

### Liste und Suche

Die Teilnehmendenliste lässt sich nach Vorname, Nachname und Geburtsdatum durchsuchen und nach Vorname, Nachname und TTR sortieren. Umlaute und Groß-/Kleinschreibung spielen bei der Suche keine Rolle. Die Spalte „Warteliste“ zeigt, in welchen Klassen jemand nur auf der Warteliste steht.

## 4. Klassen

Eine Klasse ist eine Konkurrenz, z. B. „Mädchen U15 Einzel“. Links steht die Liste der Klassen, rechts die gewählte Klasse mit ihren Reitern.

### Klasse anlegen

**Klasse anlegen** fragt Name, Einzel oder Doppel, Typ, Jahrgangsgrenzen und eine Höchstzahl ab.

| Typ | Wer darf melden |
| --- | --- |
| Erwachsene | alle |
| Damen | nur weiblich |
| Jugend | jünger als 21 Jahre am Turniertag, weiblich und männlich |
| Mädchen | jünger als 21 Jahre, nur weiblich |

Jugendliche dürfen sich zusätzlich bei Erwachsenen bzw. Damen melden. Jahrgangsgrenzen sind optional (z. B. „Jg. 2012 und jünger“). Die Höchstzahl ist nur eine Kontrolle: Wer über der Grenze gemeldet ist, wird hervorgehoben.

**Klasse bearbeiten** ist möglich, bis in Phase 1 das erste Ergebnis eingetragen ist; danach zeigt der Dialog die Einstellungen nur noch zum Lesen.

### Reiter einer Klasse

- **Teilnehmende der Klasse** – Zuordnung und Doppelpaarungen
- **Phase 1, Phase 2** – erscheinen erst nach „Modus festlegen“, z. B. „Phase 1 – Gruppenübersicht“, „Phase 2 – KO-System“
- **Endergebnis** – erscheint erst, wenn die Klasse beendet ist

Wer eine Klasse in der Liste wählt, landet direkt in der **aktuellsten Phase**; bei einer beendeten Klasse im „Endergebnis“, bei einer noch nicht ausgelosten Klasse unter „Teilnehmende der Klasse“.

### Teilnehmende zuordnen

- **Verfügbar** (links) = nicht gemeldet, **In der Klasse** (rechts) = gemeldet.
- Zuordnen per Drag & Drop, Doppelklick oder Pfeilknöpfe; eine Person kann in mehreren Klassen spielen.
- Filter: Suche (Vorname, Nachname, Verein) und Geschlecht; bei „Verfügbar“ zusätzlich „nur passende“ (Alter und Geschlecht der Klasse). Den Vereinsfilter gibt es nur in Doppelklassen – dort hilft er beim Zusammenstellen der Paare; im Einzel spielt die Reihenfolge keine Rolle.
- Die Warteliste-Spalte zeigt, wer für diese Klasse auf der Warteliste stand.

### Doppel

In einer Doppelklasse steht links „Verfügbar“ (mit Vereinsfilter), rechts „Doppelpaarungen“. Man zieht zwei Personen auf „Neues Doppel“ oder auf den freien Platz einer Paarung. Unvollständige Paarungen sind ausgegraut und werden nicht ausgelost. Zurück in die Liste ziehen nimmt eine Person aus der Paarung.

- **TTR eines Doppels** (Setzung und Anzeige): der Mittelwert beider Werte. Hat eine Person keinen TTR, zählt der Wert der Partnerin bzw. des Partners; haben beide keinen, bekommt das Doppel 0 und wird zuletzt gesetzt – mehrere Doppel mit 0 ordnet das Los.
- **Suche über den Doppelpaarungen:** findet Vorname, Nachname und Verein beider Personen. Angezeigt werden nur passende Paarungen (auch „ohne Partner/in“), die Nummern bleiben die ursprünglichen, der Titel zeigt z. B. „Doppelpaarungen (3 von 8 …)“. Drag & Drop funktioniert auch in der gefilterten Liste.
- **Automatisches Blättern:** Zieht man eine Person an den oberen oder unteren Rand der Doppelpaarungen, blättert die Liste weiter.

- **Partner zulosen** bildet aus allen, die in der Klasse noch ohne Partner/in sind, rein zufällig Doppel. Bei ungerader Zahl bleibt eine Person übrig; HTS weist darauf hin.
- Doppel werden überall ausgeschrieben, je Person eine Zeile „Nachname, Vorname“ (Ergebnisdialog, KO-Baum, Spielzettel, PDFs); in der Klassenliste tragen Doppelklassen das Abzeichen „Doppel“.

Sobald eine Klasse ausgelost ist, sind ihre Teilnehmenden festgelegt. Zum Ändern muss die Auslosung verworfen werden.

## 5. Modus festlegen und Auslosung (Phase 1)

Stehen die Teilnehmenden fest, öffnet **Teilnehmende fertig – Modus festlegen** (unter „Teilnehmende der Klasse“) den Dialog für Phase 1. „Auslosen“ erstellt Phase 1 mit allen Spielen; danach erscheint der Reiter „Phase 1“.

| Einstellung | Gilt für | Auswahl |
| --- | --- | --- |
| Modus | alle | KO-System, Gruppenphase (jeder gegen jeden), Schweizer System |
| Setzung | KO, Schweizer System | nach Setzung (TTR) oder zufällig |
| Setzung | Gruppen | TTR Setzung (Schlange), TTR Setzung (Stärkegruppen), Gruppenkopf gesetzt oder zufällig |
| Gruppen | Gruppen | Gruppengröße oder Anzahl Gruppen |
| Bei gleichem Spielverhältnis | Gruppen | Satz- und Ballverhältnis oder direkter Vergleich |
| Anzahl Runden | Schweizer System | Vorschlag: Runden eines KO-Systems + 1 |
| Gewinnsätze | alle | 3 (Best of 5, Standard) oder 4 (Best of 7) – gilt für alle Phasen der Klasse |
| Vereinsgleichheit beachten | KO, Gruppen | eigene Zeile mit Checkbox: an (Standard) oder aus |

Doppel werden nach dem Doppel-TTR gesetzt (Abschnitt 4). Die Auswahllisten im Dialog sind gleich breit. Im Phasen-Reiter stehen danach **Neu auslosen** (gleiche Einstellungen) und **Auslosung verwerfen** (zurück zur Zuordnung, der Reiter verschwindet).

**Doppelklassen:** Bei mehr als 5 Doppeln ist Phase 1 immer ein KO-System. Bis 5 Doppel stehen KO-System oder „Gruppenphase“ (eine Gruppe, jeder gegen jeden) zur Wahl. Das Schweizer System gibt es für Doppel nicht, und Vereinsgleichheit spielt bei Doppeln keine Rolle.

### KO-System

Das Feld wird auf die nächste Zweierpotenz aufgefüllt (4, 8, 16, 32, 64, 128, 256 …); fehlende Plätze sind Freilose für die Besten. Zwei Freilose treffen nie aufeinander. Nach Setzung gilt diese Setzliste (16er-Feld, von oben nach unten):

| Spiel | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Paarung | 1–16 | 9–8 | 5–12 | 13–4 | 3–14 | 11–6 | 7–10 | 15–2 |

So treffen sich Nr. 1 und 2 frühestens im Finale, Nr. 1/4 und 2/3 frühestens im Halbfinale. Kleinere und größere Felder folgen demselben Muster (8er: 1–8, 5–4, 3–6, 7–2).

**Spiel um Platz 3:** Im Phasen-Reiter eines KO-Systems (Phase 1 oder 2) schaltet der Schalter **„Spiel um Platz 3“** oben rechts (neben „PDF / Drucken“) ein zusätzliches Spiel der beiden Halbfinalverlierer zu bzw. ab. Es erscheint unter der Spalte „Finale“, seine Plätze heißen „Verlierer/in Spiel n“ mit den Nummern der beiden Halbfinals. Der Gewinner wird 3., der Verlierer 4. – sonst teilen sich beide Platz 3.

- Geht jederzeit, auch während die Phase läuft. Sind die Halbfinals schon gespielt, wird das Spiel sofort angelegt und eingereiht; vor dem Start steht es vor dem Finale.
- Abschalten geht nur, solange das Spiel noch nicht am Tisch ist bzw. gespielt wurde.
- Wird ein Halbfinale korrigiert und ändert sich der Verlierer, wird das Spiel um Platz 3 zurückgesetzt (mit Rückfrage, wenn es schon gespielt ist).
- Nicht möglich: in der KO-Qualifikation des Qualifikationsmodus und wenn ein Halbfinale ein Freilos ist (z. B. 3 Teilnehmende).

### Gruppenphase

- **Schlange:** Nr. 1 kommt in Gruppe A, Nr. 2 in B usw.; danach rückwärts von der letzten Gruppe, dann wieder vorwärts.
- **Gruppenkopf gesetzt:** TTR-Nr. 1 ist Kopf von Gruppe A, Nr. 2 von B usw.; die übrigen Plätze werden wie bei den Stärkegruppen gelost.
- **Zufällig:** Die Gruppen werden ohne Rücksicht auf den TTR gelost.
- **Stärkegruppen:** Bei 16 Teilnehmenden und 4 Gruppen sind das 1–4, 5–8, 9–12 und 13–16; die letzte Stärkegruppe nimmt einen Rest auf (bei 15: 9–15). Jede Gruppe bekommt aus jeder Stärkegruppe mindestens eine Person, gezogen per Los.
- Ungleich große Gruppen sind erlaubt; die Vorschau im Dialog zeigt Gruppengrößen und Anzahl Spiele.

### Schweizer System

- Runde 1: nach Setzung spielt die obere gegen die untere Hälfte, sonst zufällig.
- Jede weitere Runde wird automatisch nach der Tabelle gepaart, sobald die vorige Runde komplett gespielt ist.
- Niemand spielt zweimal gegen denselben Gegner.
- Bei ungerader Zahl gibt es je Runde ein Freilos, bevorzugt für Tabellenletzte und höchstens eines je Person. Es zählt als Sieg.
- Das Schweizer System gibt es nur in Phase 1, und es folgt keine weitere Phase: Nach der letzten Runde gibt es nur das Endergebnis.

### Vereinsgleichheit

Ist „Vereinsgleichheit beachten“ an, trennt die Auslosung Vereinskameraden, solange das möglich ist:

- **Schlange:** Die Schlange wird unterbrochen, der Nächststärkere ohne Vereinsgleichheit kommt auf den Platz.
- **Stärkegruppen:** Aus derselben Stärkegruppe wird jemand ohne Vereinsgleichheit gezogen.
- **Nachbessern bei Gruppen** (Schlange, Stärkegruppen, zufällig): Nach der Verteilung tauscht HTS Mitglieder mit Vereinsgleichheit gegen möglichst gleich starke Mitglieder anderer Gruppen, solange das die Vereinsgleichheiten verringert. Gruppenköpfe und Stärkegruppen bleiben dabei erhalten.
- **KO-System:** Vereinskameraden treffen nicht in Runde 1 aufeinander. Der schwächer Gesetzte tauscht mit dem Nächstplatzierten.

Verbliebene Vereinsgleichheit wird bis zum Start der Phase rot markiert. Ab Phase 2 spielt sie keine Rolle mehr – mit einer Ausnahme: In Runde 1 der KO-Qualifikation des Qualifikationsmodus wird nach Möglichkeit ein Gegner aus einem anderen Verein zugelost (siehe Abschnitt 8). Bei Doppeln wird nie auf Vereinsgleichheit geprüft.

### Auslosung von Hand ändern

- **Gruppen (vor dem Start):** Einen Namen auf ein anderes Mitglied ziehen tauscht beide. Auf eine andere Gruppe ziehen verschiebt die Person; eine Gruppe behält mindestens 2.
- **KO-System:** Einen Namen auf einen anderen Platz der 1. Runde ziehen tauscht die Plätze, auch mit Freilosen. Den Kopf „Spiel x“ auf eine andere Paarung ziehen tauscht ganze Paarungen. Wann das nach dem Start noch geht, steht in Abschnitt 6.

Die Spiele werden dabei neu angelegt.

### Ansicht der Gruppenphase

- Je Gruppe steht links die Tabelle und rechts die Spieleliste. Die Spiele sind sortiert: aktiv/pausiert, eingereiht, geplant, abgeschlossen – innerhalb nach Spielnummer; nach jeder Ergebniseingabe wird neu sortiert.
- Die ersten 7 Spiele einer Gruppe sind sichtbar, der Rest lässt sich je Gruppe ausklappen.
- Ein Klick auf eine abgeschlossene Spielzeile klappt die Satzergebnisse darunter auf bzw. zu; Doppelklick korrigiert das Ergebnis.
- **Drucken** auf einer Gruppenkarte erzeugt das PDF nur dieser Gruppe (jederzeit, aktueller Stand); **alle Gruppen · PDF / Drucken** oben rechts das PDF der ganzen Phase.
- Die **Suche** über der Ansicht zeigt nur die Gruppen mit Treffern und deren Spiele; Treffer sind markiert. Neben dem Feld steht „n Treffer“; **Enter** springt zum nächsten Treffer, **Umschalt+Enter** zum vorherigen („Treffer 2 von 5“, nach dem letzten wieder der erste).
- Die Infozeile oben rechts zeigt z. B. „12 Gruppen · 34 Spiele offen“ bzw. „keine Spiele offen“.

### KO-Baum in HTS

Der KO-Baum zeigt je Runde eine Spalte („Runde der letzten 128“ … „Achtelfinale“, „Viertelfinale“, „Halbfinale“, „Finale“).

- **Karten:** Kopf „Spiel n“ mit Tisch und Status, darunter beide Parteien mit Kennung (Setznummer bzw. ab Phase 2 z. B. „A1“), Name ausgeschrieben, Verein klein dahinter und rechts TTR bzw. nach dem Spiel die Sätze; Doppel zweizeilig ohne TTR. Unter einem abgeschlossenen Spiel stehen die Satzergebnisse. Freilose sind blass dargestellt.
- **Noch offene Plätze** heißen „Sieger/in Spiel n“ (im Spiel um Platz 3 „Verlierer/in Spiel n“).
- **Linker Kartenrand nach Status:** grau = anstehend (geplant, eingereiht), türkis = läuft, gelb = pausiert, schwarz = abgeschlossen.
- **Turnierpfad:** Durchgezogene graue Linien verbinden jedes Spiel mit seinem Folgespiel (nicht beim Spiel um Platz 3); hellgraue, gestrichelte senkrechte Linien trennen die Runden.
- **Rundentitel** bleiben beim Scrollen nach unten oben stehen und wandern beim seitlichen Scrollen mit ihrer Spalte.
- **Infozeile** oben rechts, z. B. „KO-System · 128er-Feld · 95 Begegnungen · 32 Freilose · zufällig · Vereinsgleichheit beachtet · 12 Spiele offen“ (ab Phase 2 „nach Platzierung der Vorphase“).
- **Suche:** Das Suchfeld (Vorname, Nachname, Verein) hebt alle Partien der gefundenen Personen bzw. ihrer Doppel samt Pfad türkis hervor, alle anderen Karten werden abgeblendet. Die Ansicht springt zum ersten Treffer; **Enter** / **Umschalt+Enter** springen zum nächsten / vorherigen Treffer („Treffer 2 von 5“).
- Doppelklick auf eine Karte öffnet die Ergebniseingabe.

## 6. Turnierablauf an den Tischen

Gestartete Klassen teilen sich eine Warteschlange, aus der HTS jeden freien Tisch sofort belegt. Jedes Spiel hat einen der Status geplant, eingereiht, aktiv, pausiert oder abgeschlossen; das Bild zeigt die Übergänge.

```mermaid
stateDiagram-v2
    direction LR
    geplant --> eingereiht: Phase gestartet
    eingereiht --> aktiv: freier Tisch
    aktiv --> abgeschlossen: Ergebnis
    eingereiht --> geplant: Phase anhalten
    aktiv --> eingereiht: zurückziehen
    aktiv --> pausiert: Phase anhalten
    pausiert --> aktiv: fortsetzen
```

*Status eines Spiels: Nur eingereihte Spiele kommen an einen Tisch; pausierte Spiele behalten ihren Tisch.*

### Starten

- **bereit** markiert eine ausgeloste Klasse; **Turnier starten** (Kopfzeile) startet alle bereiten Klassen auf einmal und weist auf nicht bereite hin.
- **starten** im Phasen-Reiter startet nur diese Klasse.
- Beim Start wird die Startzeit festgehalten; die Auslosung ist danach fest (Ausnahmen: Abschnitt 5 und unten).

### Warteschlange und Tische

- Ein freier Tisch bekommt das nächste eingereihte Spiel. Spielt eine Person daraus gerade an einem anderen Tisch, wird das Spiel übersprungen.
- Kann eine Klasse gerade kein Spiel stellen, bekommen andere gestartete Klassen den Tisch.
- Ist ein Ergebnis eingetragen, wird der Tisch frei und sofort neu belegt.
- Die Seite „Tische“ zeigt je Tisch eine Karte und rechts die Warteschlange. Die Anzahl der Tische lässt sich dort ändern.

### Tischkarten

- Alle Karten sind gleich breit und je Zeile gleich hoch; wie viele nebeneinander stehen, richtet sich nach der Fensterbreite.
- **Kopfzeile:** Tischname, direkt dahinter „alle Klassen“ bzw. „nur: …“ (Klick öffnet „Tische den Klassen zuordnen“) und rechts der Status: aktiv, pausiert, frei oder deaktiviert.
- **Linker Rand:** türkis = Spiel läuft, gelb = pausiert, hellgrün = frei, schwarz = deaktiviert.
- Darunter bei belegten Tischen die Partie (Klasse, Phase/Runde, Spiel-Nr., Namen, Beginn) und rechts die Knöpfe.

### Tische benennen

Standardmäßig heißen die Tische „Tisch 1“, „Tisch 2“ usw. Ein Klick auf den Namen in der Tischkarte öffnet das Umbenennen: bis zu 30 Zeichen, z. B. „Halle 2 – Center Court“. Ein leerer Name stellt den Standardnamen wieder her. Der Name erscheint überall dort, wo sonst „Tisch n“ stünde: Tischkarten, Spielkarten in Gruppen und KO-Baum (an engen Stellen gekürzt, voller Name im Tooltip), Seite „Spiele“, Auswahl bei „wechseln“, Ergebnisdialog, Spielzettel und PDFs. Intern bleibt die Nummer die Reihenfolge. Die Namen werden in der Turnierdatei gespeichert.

### Tische deaktivieren

Auf der Karte eines freien Tisches schaltet **Tisch deaktivieren** ihn ab: Er bekommt keine Spiele mehr, hat einen schwarzen Rand und das Kennzeichen „deaktiviert“. **Tisch aktivieren** gibt ihn wieder frei; das nächste passende Spiel kommt sofort dorthin. Deaktivieren geht nur, wenn kein Spiel am Tisch liegt. Bei voller Warteschlange wird ein Tisch nach dem Ergebnis sofort neu belegt – dann das laufende Spiel vorher mit „wechseln“ an einen anderen freien Tisch geben. Der Zustand wird in der Turnierdatei gespeichert.

### Aktionen auf einer Tischkarte

| Knopf | Wirkung |
| --- | --- |
| Ergebnis | Ergebnis Satz für Satz eintragen (Abschnitt 7) |
| Drucken | Spielzettel (A6) als PDF zeigen, speichern oder erneut drucken |
| wechseln | Spiel an einen anderen Tisch verschieben; ist er belegt, tauschen die Spiele |
| zurückziehen | Spiel vom Tisch nehmen; es reiht sich hinten in der Warteschlange ein |
| fortsetzen | nur bei pausierten Spielen: das Spiel läuft am Tisch weiter |
| Tisch deaktivieren / aktivieren | nur bei freien bzw. deaktivierten Tischen (siehe oben) |

In der Warteschlange holt **an Tisch** ein Spiel sofort an einen freien Tisch.

### Tische den Klassen zuordnen

**Tische den Klassen zuordnen** öffnet eine Tabelle Tisch × Klasse. Ohne Haken ist ein Tisch für alle Klassen da (Standard). Ein reservierter Tisch bekommt nur Spiele seiner Klassen. Passt das nächste Spiel nicht, nimmt er das erste passende spätere, z. B. Spiel 3, 7 oder 12 der Warteschlange. „alle“ setzt oder löscht eine ganze Spalte.

### Phase anhalten und fortsetzen

- **Phase anhalten:** Spiele an den Tischen werden pausiert und bleiben ihrem Tisch zugeordnet; für sie lässt sich kein Ergebnis eintragen. Eingereihte Spiele verlassen die Warteschlange. Oben links im Phasen-Reiter steht dann ein gelber Hinweis „Phase n ist angehalten …“ (bei wenig Platz mehrzeilig).
- Ein pausiertes Spiel wird am Tisch bewusst mit **fortsetzen** wieder gestartet.
- **Phase fortsetzen** setzt alle pausierten Spiele fort und reiht die übrigen an ihrem alten Platz wieder ein.
- In einer angehaltenen KO-Phase lassen sich Plätze und Paarungen der 1. Runde frei per Drag & Drop umsortieren. Nicht mehr gültige Spiele werden gelöscht bzw. zurückgesetzt, samt abhängiger Spiele späterer Runden.
- In einer laufenden KO-Phase geht das Umsortieren nur für Spiele, die noch nicht begonnen haben.
- Eine angehaltene Folgephase (Phase 2) kann **neu ausgelost** werden, wenn an keinem Tisch mehr ein Spiel von ihr läuft. Ihre Ergebnisse entfallen, die neue Auslosung bleibt angehalten.

### Präsentationsansicht (zweiter Bildschirm)

Die Seite **Präsentationsansicht** zeigt das Turnier auf einem zweiten Bildschirm (Beamer, Fernseher) – randlos, mit großer Schrift und ohne Bedienelemente. Erkennt HTS einen zweiten Bildschirm, startet **Auf zweitem Bildschirm starten** die Anzeige; **Vorschau im Fenster** zeigt dieselbe Anzeige zum Ausprobieren in einem normalen Fenster. **Esc** oder **Anzeige beenden** schließt sie. Die Anzeige aktualisiert sich bei jedem neuen Ergebnis.

- **Wechselzeit:** Sekunden je Seite (5–600, Standard 20). Ein Balken unter der Kopfleiste zeigt den Ablauf.
- **Ansichten in der Rotation:** ein eigener Block „Tische und Warteschlange“ (Tische, „Die nächsten 16 Spiele“), darunter **je Klasse** ein Status („nicht ausgelost“, „ausgelost“, „läuft“, „angehalten“, „beendet“) mit allen Phasen und – bei beendeten Klassen – dem Endergebnis. Jede Ansicht hat ein Häkchen. Laufende und beendete Klassen sind zunächst eingeschaltet, ausgeloste, noch nicht gestartete zunächst ausgeschaltet; nicht ausgeloste Klassen haben noch kein Häkchen. Die Auswahl wird mit dem Turnier gespeichert.
- **Reihenfolge:** laufende Phasen, Tische und die nächsten 16 Spiele, beendete Phasen, Endergebnisse, zuletzt eingeschaltete, noch nicht gestartete Klassen – je Block in der Reihenfolge der Klassenliste.

Die Seiten richten sich nach der **Auflösung des zweiten Bildschirms**. Nie wird eine Karte zwischen zwei Seiten zerschnitten; die Kopfleiste zeigt „Seite x / y“.

| Ansicht | Aufteilung |
| --- | --- |
| Gruppen | höchstens 4 je Bildschirm in einem festen 2×2-Raster mit gleich großen Zellen (links oben, rechts oben, links unten, rechts unten); weniger Gruppen behalten ihren Platz, leere Zellen bleiben frei. Passen nur 2, stehen sie nebeneinander (bei ungerader Zahl die letzte links); eine einzelne Gruppe ohne Raster |
| Tische | so groß wie möglich in einem Raster gleich großer Zellen; reicht der Platz nicht, mehrere Seiten mit nur ganzen Tischen |
| KO-System | höchstens 4 Runden nebeneinander ab der ersten Runde mit offenem Spiel; reicht die Höhe nicht, je Seite ein Block der ersten gezeigten Runde (z. B. 8 Spiele) samt Folgespielen, die Rundentitel auf jeder Seite |
| Qualifikation, Schweizer System, Platzierungsspiele | Seitenwechsel nur zwischen ganzen Karten bzw. Tabellenzeilen |
| Endergebnis | 20 Plätze je Seite |

Gruppen und Tische werden nie kleiner als die normale HTS-Schrift dargestellt – dann gibt es lieber mehr Seiten.

## 7. Ergebnisse erfassen und korrigieren

Ergebnisse werden Satz für Satz eingetragen („1. Satz“, „2. Satz“ …, Spalte „Punkte“); das Satzverhältnis rechnet HTS selbst. Die erste Zahl gehört immer zur erstgenannten Partei. Gespielt wird auf 3 oder 4 Gewinnsätze – je nach Einstellung der Klasse („Modus festlegen“). Die Einstellung lässt sich ändern, solange in der Klasse noch kein Ergebnis eingetragen ist; Ergebnisdialog und Spielzettel (bis zu 7 Sätze) passen sich an.

### Eingabe

| Eingabe | ergibt |
| --- | --- |
| 11:8 (auch 11-8, 11/8, 11 8) | 11:8 |
| +8 | 11:8 |
| -7 | 7:11 |
| +0 | 11:0 |
| +10 | 12:10 |
| -12 | 12:14 |

- **Enter** springt zum nächsten Satz. Ist das Spiel entschieden, springt Enter auf „Ergebnis speichern“; ein weiteres Enter speichert.
- Hat eine Seite die nötigen Gewinnsätze (3 bzw. 4), sind die übrigen Sätze gesperrt.
- Gültig ist ein Satz mit 11 Punkten und höchstens 9 für den Gegner oder, ab 10:10, mit genau 2 Punkten Abstand.
- Ungültige Sätze (z. B. 13:14 oder 11:10) und leere Sätze vor einem eingetragenen Satz werden rot markiert und blockieren das Speichern.

### Aufgabe

Im Ergebnisfenster lässt sich markieren, wer aufgibt.

- Das Spiel mit der Aufgabe zählt 0:3 für die aufgebende Partei, alle Sätze 0:11.
- Alle noch offenen Spiele dieser Partei in der Phase gelten sofort als 3:0 (Sätze 11:0) für die Gegner. Sie zählen für die Turnierwertung, werden aber nicht exportiert.
- Im Schweizer System wird die Partei danach nicht mehr gepaart.
- Wird die Aufgabe wegkorrigiert, öffnet HTS die so gewerteten Spiele wieder.

### Wo man einträgt

- Seite „Tische“: Knopf **Ergebnis**
- Gruppenübersicht und KO-Baum: Doppelklick auf das Spiel
- Seite „Spiele“: Doppelklick oder **Ergebnis eintragen / korrigieren**

### Korrekturen

Abgeschlossene Spiele lassen sich jederzeit korrigieren, auch in abgeschlossenen Phasen; Tabellen, Platzierungen und Endergebnis rechnen sich neu. Ändert sich im KO-System dadurch der Sieger und sind Folgespiele schon gespielt oder laufen, fragt HTS nach. Diese Folgespiele werden dann zurückgesetzt und mit dem neuen Sieger neu eingereiht; eine beendete Klasse muss danach erneut abgeschlossen werden.

### Seite „Spiele“

Die Seite listet alle Spiele aller Klassen. Gefiltert wird nach Status (standardmäßig aktiv, pausiert und eingereiht) und nach Klasse; das Suchfeld (Vorname, Nachname, Verein) findet die Spiele einer Person bzw. eines Doppels. Die Spalte „Tisch“ zeigt die Nummer bzw. den eigenen Tischnamen. Die Spalte „Export“ steuert die click-TT-Ergebnisdatei (Abschnitt 10).

## 8. Wertung, Phasen und Endergebnis

Eine Klasse hat höchstens zwei Phasen (eine Phase 3 gibt es nicht). Nach jeder Phase entscheidet die Turnierleitung: Endergebnis oder neue Phase. Das Bild zeigt, welche Phase worauf folgen kann.

```mermaid
flowchart LR
    subgraph P1[Phase 1]
        P1KO[KO-System]
        P1G[Gruppenphase]
        P1S[Schweizer System]
    end
    subgraph P2[Phase 2]
        P2G[Gruppenphase]
        P2KO[KO-System]
        P2P[Platzierungsspiele*]
        P2Q[Qualifikationsmodus**]
    end
    P1G -- neue Phase --> P2
    P1 -- Phase abschließen --> E[Endergebnis: Klasse beendet]
    P2 -- Phase abschließen --> E
```

*\* nur nach genau 2 Gruppen. \*\* nur nach Gruppen in Phase 1. Nach KO-System oder Schweizer System in Phase 1 und nach Phase 2 geht es immer zum Endergebnis.*

### Gruppentabellen

Jede Gruppe zeigt Platz, Name, Verein, TTR, Spiele, Siege:Niederlagen (S:N), Sätze und Bälle. Gewertet wird zuerst nach dem Spielverhältnis, bei Gleichstand je nach Einstellung:

- **Satz- und Ballverhältnis:** Satzdifferenz, dann Balldifferenz, dann gewonnene Sätze.
- **Direkter Vergleich:** eine Tabelle nur aus den Spielen der Gleichplatzierten untereinander, danach Satz- und Balldifferenz aller Spiele.
- **Letztes Mittel – das Los:** Ist danach immer noch alles gleich, entscheidet ein festes Los. Es wird einmal je Gruppe bestimmt und mit dem Turnier gespeichert, damit die Tabelle nicht springt. Der TTR zählt im Ergebnis nicht; er zählt nur im Qualifikationsmodus innerhalb einer Stufe. Solange in einer Gruppe noch kein Spiel beendet ist, bleibt die Reihenfolge der Auslosung stehen.

### Schweizer System

Die Tabelle zeigt zusätzlich Punkte (Siege + Freilos), **BHZ** (Buchholz: Summe der Punkte aller Gegner) und **FBHZ** (Feinbuchholz: Summe der Buchholzzahlen aller Gegner). Reihenfolge: Punkte, Buchholz, Feinbuchholz, Satzdifferenz, Balldifferenz, gewonnene Sätze, zuletzt das feste Los.

### Phase abschließen

Wie viele Spiele noch offen sind, steht im Phasen-Reiter rechts in der Infozeile („3 Spiele offen“ bzw. „keine Spiele offen“). Sind alle gespielt, erscheint oben rechts bei den Knöpfen **Phase abschließen** mit zwei Möglichkeiten:

1. **Endergebnis:** Die Klasse ist beendet, der Reiter „Endergebnis“ erscheint.
2. **Neue Phase** (nur nach einer Gruppenphase in Phase 1): Gruppenphase, KO-System, Gruppenphase mit Platzierungsspielen oder Qualifikationsmodus, dazu Aufsteiger je Gruppe und Gruppengröße. Die neue Phase startet sofort.

Nach einem KO-System oder Schweizer System in Phase 1 und nach jeder Phase 2 gibt es nur das Endergebnis. Ist Phase 2 angelegt, führt der Knopf **Zu Phase 2** oben rechts dorthin.

### KO-System ab Phase 2

Ab Phase 2 entscheidet die Gruppenplatzierung, nicht der TTR:

- Rangfolge: erst alle Gruppenersten, dann alle Zweiten usw. Innerhalb einer Platzierung zählen Spiel-, Satz- und Punktverhältnis (Anteil gewonnen an gesamt); bei Gleichheit entscheidet das Los.
- Freilose gehen an die besten Gruppenersten.
- Der Höchstplatzierte ohne Freilos spielt gegen den Niedrigstplatzierten, der noch keinen Gegner hat und nicht aus seiner Gruppe kommt.
- Spieler derselben Gruppe stehen in verschiedenen Hälften und können sich erst im Finale treffen.
- Im KO-Baum steht vor jedem Namen statt der Setznummer die Herkunft: Gruppenbuchstabe und Gruppenplatz, z. B. **A1** (Erster Gruppe A) oder **D3** (Dritter Gruppe D). Im KO-System der Phase 1 bleiben es die Setznummern.
- Ein Spiel um Platz 3 lässt sich wie in Phase 1 zuschalten.

### Gruppenphase mit Platzierungsspielen

Nur nach genau 2 Gruppen wählbar.

- Die ersten beiden jeder Gruppe spielen eine Endgruppe um Platz 1–4. Das Spiel Erster gegen Zweiter aus Phase 1 wird übernommen, es bleiben 4 Spiele über Kreuz.
- Wertung der Endgruppe: Spielverhältnis, direkter Vergleich, Satz- und Ballverhältnis.
- Die Dritten spielen um Platz 5/6, die Vierten um 7/8 usw.
- Bei ungleich großen Gruppen bekommt der Übrige den letzten Platz ohne Spiel.

### Qualifikationsmodus

Nur als Phase 2 nach einer Gruppenphase in Phase 1 wählbar. Er bestimmt nach dem Muster der Hamburger Einzelmeisterschaften (HEM), wer sich für ein Folgeturnier qualifiziert und in welcher Reihenfolge Nachrücker zum Zug kommen. Vor- und Hauptqualifikation sind getrennte Klassen bzw. Turniere (je Klasse höchstens 2 Phasen).

| Einstellung (Dialog „Phase abschließen“) | Bedeutung |
| --- | --- |
| Direkt qualifiziert je Gruppe (Q) | die besten Q jeder Gruppe sind qualifiziert und für den Tag fertig; Q = 0 ist erlaubt |
| In die KO-Qualifikation je Gruppe (K) | die nächsten K jeder Gruppe spielen die KO-Qualifikation (z. B. Q = 1, K = 2: Zweite und Dritte); K = 0 = keine Spiele |
| Qualifiziert nach Siegen (n) | wer in der KO-Qualifikation n Spiele gewinnt, ist qualifiziert |
| Nachrücker je Gruppe (ohne Spiel) (N) | die nächsten N Gruppenplätze nach Q und K werden ohne Spiel Nachrücker (z. B. Dritte und Vierte) |

**Ablauf der KO-Qualifikation:**

- **Die Gewinner spielen weiter.** Wer n Spiele gewonnen hat, ist qualifiziert; wer verliert, scheidet aus und wird Nachrücker.
- **Runde 1 wird zugelost:** Die Teilnehmenden werden nach Gruppenplatz in eine obere und eine untere Hälfte geteilt; jeder aus der oberen spielt gegen einen per Los gezogenen aus der unteren Hälfte **einer anderen Gruppe** (bei der HEM: Gruppenzweite gegen Gruppendritte). Ist Vereinsgleichheit für die Klasse eingeschaltet, wird nach Möglichkeit auch ein anderer Verein gewählt.
- **Ab Runde 2** (und in der Zusatzrunde) wird ganz frei gelost.
- **Freilos:** Bei ungerader Zahl hat der Bestplatzierte der Runde ein Freilos; es zählt wie ein Sieg.
- **Zusatzrunde:** Im Phasen-Reiter schaltet „Zusatzrunde für die Verlierer der Runde n“ bei Bedarf (auch während der Phase) ein weiteres Spiel der Verlierer der letzten Runde zu – etwa, wenn Startplätze frei werden. Deren Sieger stehen dann als Nachrücker vor den Verlierern.
- Spieler der aktuellen Runde lassen sich per Drag & Drop tauschen (auch mit dem Freilos). Läuft die Phase, nur bei Spielen, die noch nicht am Tisch bzw. gespielt sind; ist die Phase angehalten, werden betroffene Spiele zurückgesetzt.

**Reihenfolge im Ergebnis:**

1. **qualifiziert:** zuerst die direkt Qualifizierten (untereinander nach TTR), dann die über die KO-Qualifikation Qualifizierten (nach Gruppenplatz, dann TTR).
2. **Nachrücker Rang 1, 2, …** von oben nach unten: ein Freilos in der letzten Runde, dann die Verlierer der letzten Runde (mit Zusatzrunde: deren Sieger, dann deren Verlierer), dann die Verlierer der vorletzten Runde usw. – wer später ausgeschieden ist, steht weiter oben. Danach folgen die **Nachrücker ohne Spiel** (N je Gruppe) als eigener Rang. Innerhalb eines Rangs gilt immer: zuerst der bessere Gruppenplatz (z. B. alle Dritten vor allen Vierten), dann der TTR; jede/r bekommt einen eigenen Platz.
3. **Übrige:** Wer weder qualifiziert noch Nachrücker ist, bekommt den Platz seiner Gruppenplatzierung. **Gleiche Gruppenplätze teilen sich einen Platz** – z. B. alle Gruppenfünften denselben Platz.

**Beispiele:**

| | Vorqualifikation | Hauptqualifikation |
| --- | --- | --- |
| Einstellung | Q = 2, K = 0, N = 2 | z. B. Q = 1, K = 2, n = 2, N = 0 |
| qualifiziert | Gruppenerste und -zweite (nach TTR) | Gruppenerste (nach TTR) und wer 2 KO-Spiele gewinnt |
| Nachrücker | Gruppendritte vor Gruppenvierten, je nach TTR – ohne Spiel | Verlierer der KO-Qualifikation, später Ausgeschiedene zuerst |
| Rest | ab Platz 5 je Gruppenplatz ein geteilter Platz | ab Platz 4 je Gruppenplatz ein geteilter Platz |

In der Vorqualifikation hat Phase 2 damit keine Spiele: Nach dem Anlegen steht das Ergebnis sofort fest.

### Endergebnis

Der Reiter „Endergebnis“ listet alle Platzierungen über alle Phasen. Wer in einer früheren Phase ausgeschieden ist, folgt nach Gruppenplatz hinter den Teilnehmenden der späteren Phase.

| Ergebnis aus | Platzierung |
| --- | --- |
| KO-System | Sieger 1., Finale 2., Halbfinale zweimal 3. (mit Spiel um Platz 3: Gewinner 3., Verlierer 4.), Viertelfinale 5.–8., Achtelfinale 9.–16. |
| eine Gruppe | Tabellenplatz |
| mehrere Gruppen | alle Gruppenersten teilen sich Platz 1 bis n, alle Zweiten die nächsten Plätze usw. |
| Platzierungsspiele | Endgruppe 1.–4., dann je Spiel Sieger x., Verlierer x+1. |
| Qualifikationsmodus | qualifiziert, dann Nachrücker Rang 1, 2, … (je Rang Gruppenplatz, dann TTR), dann die übrigen nach Gruppenplatz (gleicher Gruppenplatz = gleicher Platz) |

## 9. Drucken und PDF

Alle Ausdrucke entstehen als PDF und öffnen sich im eingebauten PDF-Viewer. Nur der automatische Spielzettel geht ohne Viewer direkt an den Drucker.

| Dokument | Wo | Format | Inhalt |
| --- | --- | --- | --- |
| Spielzettel | automatisch beim Aufruf an einen Tisch; Tische → Drucken | A6 hoch | Tisch (bzw. Tischname), Zeit, Spiel-Nr., Konkurrenz, Namen mit Verein (Doppel je Person eine Zeile), Satztabelle, Wahl, Gewinner/in, Unterschrift |
| Phase (Gruppen) | Phasen-Reiter → alle Gruppen · PDF / Drucken; einzelne Gruppe: „Drucken“ auf der Gruppenkarte | A4 quer | je Gruppe eine Seite: Tabelle mit Zwischenstand, darunter der Spielplan zweispaltig je Runde („Runde k“) mit Ergebnissen und Sätzen sowie kommenden Spielen |
| Phase (KO-System) | Phasen-Reiter → PDF / Drucken | A4 quer | Turnierbaum: Kästen „Spiel n“ mit Namen und Verein, Sätze unter dem Kasten, Freilose mit Kasten, Spiel um Platz 3 unter dem Finale, offene Plätze „Sieger/in bzw. Verlierer/in Spiel n“; große Felder auf mehreren Seiten. Danach der Spielplan je Runde als Karten |
| Endergebnis | Reiter Endergebnis → Als PDF anzeigen / drucken | A4 hoch | alle Platzierungen der Klasse |
| Gesamtergebnis | Kopfzeile → Ergebnisse exportieren | A4 hoch | Endergebnis aller Klassen, jede Klasse auf neuer Seite |

### Automatischer Spielzettel

Sobald ein Spiel an einen Tisch geht, druckt HTS den Spielzettel ohne Rückfrage auf dem Windows-Standarddrucker. Kann der Drucker kein A6, wird in A6-Größe auf das Standardpapier gedruckt. Auf der Seite „Tische“ lässt sich das mit dem Häkchen „Spielzettel automatisch drucken“ abschalten; die Einstellung gilt je Rechner. Wird ein Spiel mit „an Tisch“ geholt, druckt HTS ebenfalls; bei „wechseln“ nicht. In Testturnieren werden beim Simulieren keine Spielzettel gedruckt.

### PDF-Viewer

- Zoom mit − und +
- **Speichern** als PDF-Datei (Strg+S) – standardmäßig in den Ordner „PDF Export“ des Turniers; ein anderer gewählter Ordner gilt danach für dieses Turnier
- **Drucken** über den Windows-Druckdialog (Strg+P)
- Esc schließt das Fenster

Im Phasen-PDF bricht ein langer Spielplan auf Fortsetzungsseiten um. Die Spalte „Sätze“ zeigt 5 Sätze auch mit Verlängerung vollständig. Das Spiel um Platz 3 erscheint als eigener Abschnitt mit Runde „Pl. 3/4“. Namen mit Zeichen, die die PDF-Schrift nicht kennt (z. B. Ł), werden in ihrer Grundform gedruckt (L).

## 10. Ergebnisse exportieren (click-TT)

Sind alle ausgelosten Klassen beendet, erscheint in der Kopfzeile **Ergebnisse exportieren** mit zwei Einträgen: **Gesamtergebnis als PDF** und **click-TT-Ergebnisdatei (XML)**.

### So entsteht die Datei

Die Ergebnisdatei ist so aufgebaut wie in poinTT („Export/Import → Click-TT → Ergebnisse exportieren“):

1. HTS nimmt die ursprünglich importierte click-TT-Turnierdatei. Beim Import wurde eine Kopie im Turnierordner abgelegt; fehlt sie, fragt HTS nach der Datei.
2. Jede Konkurrenz wird über den Klassennamen gefunden, sonst über die meisten gemeinsamen Teilnehmenden.
3. Je Konkurrenz kommt ein Abschnitt `<matches>` hinter `<players>` mit einem `<match>` je Spiel.
4. Jeder Teilnehmer erhält das Attribut `placement` aus dem Endergebnis.
5. Die Datei heißt „Ergebnisse - <erste 20 Zeichen des Turniernamens> - TT_MM_JJJJ.xml“ und wird standardmäßig im Ordner „XML Export“ des Turniers gespeichert (ein anderer gewählter Ordner gilt danach für dieses Turnier) und kann in click-TT beim Turnier hochgeladen werden.

### Inhalt eines Spiels

| Attribut | Bedeutung |
| --- | --- |
| nr | laufende Nummer in der Konkurrenz |
| group | Gruppe oder Runde, bei mehreren Phasen mit „Phase n“ davor |
| scheduled | Beginn, Format JJJJ-MM-TT hh:mm |
| player-a, player-b | click-TT-ID der Teilnehmenden |
| set-a-1 … set-a-7, set-b-1 … set-b-7 | Punkte je Satz, nicht gespielte Sätze 0 |
| sets-a, sets-b | gewonnene Sätze |
| matches-a, matches-b | 1 für den Sieger, 0 für den Verlierer |
| games-a, games-b | Bälle insgesamt |

### Welche Spiele exportiert werden

| Spiel | Export |
| --- | --- |
| abgeschlossenes Spiel einer beendeten Klasse | ja (Standard) |
| Haken „Export“ auf der Seite „Spiele“ entfernt (nicht relevant für Export) | nein |
| Spiel, in dem aufgegeben wurde | ja, 0:3 mit Sätzen 0:11 |
| nach einer Aufgabe kampflos gewertetes Spiel | nein (Spalte zeigt „nein (kampflos)“) |
| in die Endgruppe übernommenes Spiel | nur einmal, als Original aus Phase 1 (Kopie zeigt „übernommen“) |
| Klasse noch nicht beendet | nein, die Konkurrenz wird im Bericht genannt |

Die Export-Markierung gibt es nur auf der Seite „Spiele“ und nur für abgeschlossene Spiele. Nach dem Export zeigt HTS einen Bericht: Anzahl Konkurrenzen, Spiele und Platzierungen sowie, was nicht exportiert wurde.

## 11. Dateien, Technik und Grenzen

### Dateien

| Datei | Ort | Inhalt |
| --- | --- | --- |
| `<Turniername>.hts` | `Dokumente\HTS Turniere\<Turniername>\Turnierdatei` (oder per „Durchsuchen“ gewählt) | ein Turnier: Teilnehmende, Klassen, Paarungen, Auslosungen (samt festem Los), Spiele, Tische (Anzahl, Namen, deaktivierte Tische), Tischzuordnung, Einstellungen der Klassen und der Präsentationsansicht, Testturnier-Kennzeichen und Speicherorte (XML) |
| `<Turniername>.clicktt.xml` | neben der Turnierdatei | Kopie der importierten click-TT-Datei, Grundlage des Exports |
| `Ergebnisse - <Name> - TT_MM_JJJJ.xml` | `…\<Turniername>\XML Export` (Standard) | click-TT-Ergebnisdatei |
| PDF-Dateien | `…\<Turniername>\PDF Export` (Standard) | aus dem PDF-Viewer gespeichert |

Neuere Versionen müssen ältere Turnierdateien nicht unbedingt lesen können; ein laufendes Turnier sollte mit der Version beendet werden, mit der es begonnen wurde.

### Technik

- Java 21 und JavaFX 21, ausgeliefert mit eigener Java-Laufzeit (Eclipse Temurin 21)
- PDF: Apache PDFBox 3.0.5 (Apache-Lizenz 2.0, Hinweise im Ordner `Lizenzen`)
- Lizenz: GNU General Public License v3.0 (Datei `LICENSE.txt`); die Lizenzen von JavaFX und der Java-Laufzeit liegen im Ordner `Lizenzen`
- Quellcode und Issues: GitHub-Projekt „Hamburger-Turniersoftware---HTS“
- Quellcode: Paket `de.hts` mit den Bereichen Teilnehmende, Turnier und Auslosung, Speichern, click-TT-Import und -Export, PDF und Oberfläche; dazu automatische Tests

### Bekannte Grenzen

- Eine Aufgabe wirkt nur in der Phase, in der sie passiert; für eine spätere Phase muss die Partei von Hand berücksichtigt werden.
- Gruppen lassen sich nur vor dem Start der Phase per Drag & Drop ändern.
- Die click-TT-Ergebnisdatei ist noch nicht gegen einen echten Upload in click-TT geprüft.
- „Fertiges Turnier importieren“ übernimmt nur Phase 1 je Konkurrenz.
- Ein Tisch lässt sich nur deaktivieren, solange kein Spiel an ihm liegt.

### Glossar

| Begriff | Bedeutung |
| --- | --- |
| Klasse | eine Konkurrenz, z. B. „Jungen U19 Einzel“ |
| Phase | ein Abschnitt einer Klasse (Gruppen, KO-System, Schweizer System, Platzierungsspiele, Qualifikationsmodus); höchstens 2 |
| Qualifikationsmodus | Phase 2 nach Gruppen: Beste je Gruppe direkt qualifiziert, Folgende spielen eine KO-Qualifikation (Gewinner spielen weiter, qualifiziert nach n Siegen), weitere Gruppenplätze werden ohne Spiel Nachrücker |
| Nachrücker Rang x | Stufe im Qualifikationsmodus: rückt nach, wenn Qualifizierte absagen; innerhalb eines Rangs nach Gruppenplatz, dann TTR |
| Zusatzrunde | zuschaltbares weiteres Spiel der Verlierer der letzten Runde der KO-Qualifikation |
| Testturnier | Übungsturnier, in dem alle offenen Spiele per Würfel-Knopf simuliert werden können |
| Tischname | eigener Name eines Tisches (bis 30 Zeichen) statt „Tisch n“ |
| Setzung | Reihenfolge nach TTR bzw. Doppel-TTR (Phase 1) oder nach Platzierung (ab Phase 2) |
| Gruppenkopf gesetzt | Gruppen-Setzung: TTR-Nr. 1 führt Gruppe A, Nr. 2 Gruppe B usw. |
| Festes Los | letztes Kriterium bei völliger Gleichheit in Gruppentabellen und im Schweizer System; einmal bestimmt und gespeichert |
| Präsentationsansicht | Anzeige auf einem zweiten Bildschirm mit wechselnden Ansichten |
| Spiel um Platz 3 | zuschaltbares Spiel der beiden Halbfinalverlierer im KO-System; Gewinner 3., Verlierer 4. |
| A1, D3 … | Kennung im KO-Baum ab Phase 2: Gruppenbuchstabe + Gruppenplatz aus Phase 1 |
| Freilos | Platz ohne Gegner; die Partei kommt kampflos weiter (KO) bzw. erhält einen Sieg (Schweizer System) |
| Spielverhältnis | gewonnene zu verlorenen Spielen (S:N) |
| Buchholz (BHZ) | Summe der Punkte aller Gegner im Schweizer System |
| Feinbuchholz (FBHZ) | Summe der Buchholzzahlen aller Gegner |
| Vereinsgleichheit | zwei Parteien aus demselben Verein in einer Gruppe bzw. in einem Erstrundenspiel |
| Warteschlange | eingereihte Spiele, die auf einen freien Tisch warten |
| Aufgabe | eine Partei gibt während eines Spiels auf; ihre offenen Spiele der Phase gelten als verloren |
