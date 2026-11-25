<!--
  HTS – Dokumentation (Markdown-Quelle)
  Diese Datei ist die fortschreibbare Fassung der Dokumentation. Für neue Versionen:
  einfach diese Datei (am besten zusammen mit dem Quellcode-ZIP) wieder an Claude geben
  und um die Änderungen der neuen Version erweitern lassen.
  Stand: 20.11.2026 · Programmversion 1.3.0
-->

# HTS – Hamburger Turniersoftware: Dokumentation

Stand: 25.11.2026 · Programmversion **1.9.0**

## Versionsverlauf der Dokumentation

| Datum | Programmversion | Änderung |
| --- | --- | --- |
| 25.11.2026 | 1.9.0 | Online-Anmeldung am Turniertag über den Live-Server (QR-Code, „Online-Anmeldung bis“, Schalter auf „Live im Internet“) – neues Kapitel 12; Handy-Zugang im Hallen-WLAN entfernt (lokaler Server, Mobile Einstellungen, Anmeldung und Ergebnisse per Handy); gemeldete Doppel/Mixed sofort als Paarungen (offene markiert, Auslosen/Start gesperrt); Filter „Verfügbar“ je Klasse; „Bezahlt“ nur mit Gebühr > 0 €; Live-Seiten: KO-Baum, Freigestellte, Knopf „← Alle Turniere“ |
| 25.11.2026 | 1.8.0 | Reiter „Programm“ (Dunkelmodus, Speicherorte, Einstellungen aus älterer Version übernehmen); neue Ablage „Dokumente\HTS“ mit „Export files“, Ausrichter und Protokoll im Programmordner; Turnierwelt-Link; neue Turniere veröffentlicht, Vornamen immer abgekürzt; Rückfrage „Live-Daten auch vom Webserver löschen?“; Live-Seiten: Startseite „Aktuelle“/„Vergangene Turniere“ mit Ausrichter, eigene Seite je Spiel, nichts im Browser gespeichert (Lesezeichen „Meine Spiele“), Seite „Spenden“; Spiel um Platz 3 nach dem Finale nummeriert; Rechtstexte per update.sh |
| 24.11.2026 | 1.7.0 | Neue Farben (neutral, Grün #187250, Mint) und neues Logo; Live-Seiten neu (Reiter je Phase, Gruppenkacheln, Spielersuche, Knopf „Tische“, Aktualisierung ohne Neuladen, lesbare Adresse Name_TT_MM_JJJJ); QR-Code-Aushang; Webserver-Reiter mit Admin-Passwort, Adresse und Schlüssel verschlüsselt; Protokoll und Fehlerbericht; Logo als SVG; Freistellung 6 × 2 mit Auslosung je Hälfte; Knopftexte ohne „…“ |
| 24.11.2026 | 1.6.0 | Live-Server für das Internet (Paket hts-server, Einrichtung mit install.sh auf Ubuntu 24.04: HTTPS, Firewall, automatische Updates; Startseite mit allen Turnieren, Impressum/Datenschutz, Aufbewahrung 90 Tage) – neuer Abschnitt „Der Live-Server“ in Kapitel 11 |
| 23.11.2026 | 1.5.1 | Behoben: Einfrieren nach dem Auslosen bzw. beim Klassenwechsel großer Gruppenphasen und Dauerlast in der Gruppenansicht; Schnelleingabe im Ergebnisdialog wieder entfernt |
| 23.11.2026 | 1.5.0 | Urkunden nach jeder abgeschlossenen Phase (feste Plätze), Spalte „Gedruckt“, Urkunden-Reiter dreispaltig mit Suche; Reiter „Ausrichter“ (Name, Logo auf Schiedsrichterzettel und Live-Seiten) und „Webserver“; Turnierseite „Live im Internet“ (Veröffentlichen, Vornamen abkürzen, Live-Daten löschen, nur Endergebnis); Tablet-Modus, Zahlenfeld und Schnelleingabe im Ergebnisdialog, Antippen statt Doppelklick, Tauschen per Antippen, Zoom, Tische hochkant; letztes Ergebnis zurücknehmen; „Bitte warten“-Hinweis |
| 22.11.2026 | 1.4.0 | Urkunden drucken (Frage „Urkundendruck starten?“ beim Turnierende, Reiter „Urkunden drucken“ mit Auswahl, Vorschau, Urkundentexten je Turnier und Druckanpassung von Name und Verein), Zettel-Design je Turnier (Spielerzettel neu, Schiedsrichterzettel nach DTTB), Reiter „Archivierte Turniere“, Anzahl der Spiele als eigene Zeile in der Präsentationsansicht, „Danke an:“ in der Info, Dialoggröße bleibt nach Ändern mit der Maus |
| 20.11.2026 | 1.3.0 | Gruppenphase mit Freistellung (S1–S4 direkt ins KO ohne Freilose), Korrektur-Reiter „Phase n – Korrektur“ mit „Unveränderte Ergebnisse übernehmen“ und „Phase n weiterführen“ (alte Phase „deaktiviert“), Reiterleiste mit Blättern (6 je Seite), Zusatzspiel in der Qualifikation, feste Leiste und aktuelle Runde zuerst im Schweizer System, Gold/Silber/Bronze und Aufsteiger-Chips, Badeni ohne BHZ/FBHZ |
| 19.11.2026 | 1.2.0 | Nachtrag 0.42.0–1.2.0: Lizenz AGPL-3.0, Turnierdateien früherer Versionen bleiben lesbar (0.42.0); Startgebühren mit Kassenübersicht, Zeitplan je Klasse, Export-Knöpfe und aktualisierte Teilnehmerliste, Warteschlange je Klasse in der Präsentationsansicht, Handy-Zugang ausgeblendet (1.0.0); Startzeiten im Zeitplan, Startgebühren überarbeitet (bezahlt = angemeldet, „Gebühr erlassen“), Linux und macOS, Bedienung auf Windows-Tablets (1.1.0); neue Phasentypen: Haupt- und Trostrunde, fortgesetztes und doppeltes K.-o.-System, Gruppen nach Leistungsstufen mit Phase 3, TOP24-/TOP32-Platzierungsspiele sowie das Schweizer System nach Badeni (1.2.0) |
| 11.11.2026 | 0.41.0 | Nachtrag der Versionen 0.31.0–0.41.0: Vorgabeturnier sowie 2er und 3er Turnier, Anwesenheit am Turniertag (Meldungen, „Angemeldet“, Startsperre für importierte Klassen), Handy-Zugang mit Live-Anzeige und Anmeldung per QR-Code (Seite „Mobile Einstellungen“), Setzplatz und durchsuchbarer Vereinsfilter, Qualifikationsmodus überarbeitet (KO-Qualifikation je Gruppe oder insgesamt, Runden einzeln starten, Entscheidung nach Runde 1, Herkunft „A2“), KO-Folgephase mit bestimmten Gruppenplätzen, Phase vorzeitig beenden bzw. neu starten, feste Spielreihenfolge in Gruppen, Vereinsgleichheit „Schwächster zu Stärkstem“, Tischzuordnung „Haken = darf spielen“, kompakter Klassenkopf, Texte nie abgekürzt, Satzeingabe ohne Vorzeichen |
| 04.11.2026 | 0.30.0 | Nachtrag 0.29.0 und 0.30.0: Präsentationsansicht für den zweiten Bildschirm (Auswahl je Klasse mit Status, Seiten ohne zerschnittene Karten, Gruppen im 2×2-Raster, Tische nach Auflösung), Info-Knopf und Lizenz GPL 3.0, kompakter Kopfbereich, Klasse öffnet die aktuellste Phase, Setzung „Gruppenkopf gesetzt“, Import (Einzel direkt in die Klasse, Doppel ohne Partner/in unter „Verfügbar“), Vereinsfilter nur noch im Doppel, Suche und automatisches Blättern bei den Doppelpaarungen, Doppel-TTR bei fehlenden Werten, festes Los als letztes Kriterium in Tabellen |
| 03.11.2026 | 0.28.0 | Nachtrag der Versionen 0.21.0–0.28.0: Testturnier und Simulation, Turnier kopieren, fertiges Turnier aus click-TT importieren, Suche über Vorname/Nachname/Verein (auch im KO-Baum, Enter springt zum nächsten Treffer), KO-Baum mit Pfaden, fixierten Rundentiteln und Statusfarben, Schalter „Spiel um Platz 3“, Doppelregeln und „Partner zulosen“, Qualifikationsmodus neu (HEM-Logik, Nachrücker je Gruppe, freie Auslosung, Zusatzrunde), Tische benennen und deaktivieren, neue PDFs (KO-Turnierbaum, Gruppen zweispaltig), Leistung und Hardware |
| 31.10.2026 | 0.20.0 | Spiel um Platz 3, Gewinnsätze je Klasse (3/4), „A1“-Kennungen im KO-Baum der Phase 2, Schweizer System ohne Folgephase, Vereinsgleichheit in der Qualifikation, neue Speicherorte „Dokumente\HTS Turniere“ |
| 31.10.2026 | 0.19.0 | höchstens 2 Phasen je Klasse; Qualifikationsmodus neu: Nachrückerränge, Verlierer spielen weiter, Q = 0 möglich, geteilte Gruppenplätze |
| 30.10.2026 | 0.18.0 | Qualifikationsmodus (Abschnitt 8), Vereinsnamen in KO-Darstellungen |
| 30.10.2026 | 0.17.0 | erste vollständige Fassung |

## 1. Überblick

HTS (Hamburger Turniersoftware) organisiert Tischtennisturniere von der Meldeliste bis zur click-TT-Ergebnisdatei. Diese Dokumentation beschreibt Version 1.3.0. HTS ist freie Software unter der GNU Affero General Public License v3.0 oder später (AGPL-3.0-or-later, seit 0.42.0; bis 0.41.0 GPL 3.0); Wünsche und Fehler nimmt das GitHub-Projekt als Issue entgegen (Label „Feature“ bzw. „bug“).

Das Programm deckt den ganzen Turniertag ab:

- Teilnehmende aus click-TT importieren oder von Hand erfassen
- Klassen (Konkurrenzen) anlegen und Teilnehmende zuordnen, auch Doppel
- Modus festlegen und auslosen: KO-System, Gruppenphase, Schweizer System (auch nach Badeni) oder doppeltes K.-o.-System
- bis zu drei Phasen je Klasse, z. B. Gruppen, dann KO-System, Haupt- und Trostrunde, TOP24/TOP32 oder Gruppen nach Leistungsstufen mit anschließender Endrunde
- Startgebühren je Klasse mit Kassenübersicht und Quittungen, Zeitplan mit Startzeiten je Runde
- Tische automatisch belegen, Spielzettel drucken, Ergebnisse Satz für Satz erfassen
- Endergebnisse als PDF und die Ergebnisdatei für den click-TT-Upload erzeugen
- Qualifikationen nach dem Muster der Hamburger Einzelmeisterschaften (HEM): Vor- und Hauptqualifikation mit Nachrückerrängen
- zum Üben: Testturniere, in denen alle Spiele per Knopfdruck simuliert werden
- eine Präsentationsansicht für einen zweiten Bildschirm (Beamer, Fernseher) mit wechselnden Ansichten
- Anwesenheit am Turniertag erfassen, seit 1.9.0 auch über die Online-Anmeldung per QR-Code auf dem Live-Server (Abschnitt 12)
- Vorgabeturniere mit Punktevorsprung nach TTR sowie 2er und 3er Turniere (Mannschaftskämpfe; Abschnitt 13)

### Systemvoraussetzungen

| Punkt | Mindestens | Empfohlen |
| --- | --- | --- |
| Betriebssystem | Windows 10 oder 11, 64 Bit (x64); seit 1.1.0 auch Linux (x86_64) und macOS auf Apple-Rechnern mit M-Chip | Windows 11 |
| Java | nicht nötig, eine Java-Laufzeit (Temurin 21) liegt bei | – |
| Prozessor | Zweikern-Prozessor der letzten rund 10 Jahre | Vierkern-Prozessor |
| Arbeitsspeicher | 4 GB | 8 GB |
| Speicherplatz | ca. 200 MB für das Programm, wenige MB je Turnier | – |
| Bildschirm | 1366 × 768 Pixel (auch mit 125 % oder 150 % Windows-Skalierung) | 1920 × 1080 oder größer; für die Turnierleitung gern ein zweiter Bildschirm |
| Grafik | beliebig; ohne passenden Grafiktreiber (z. B. in einer virtuellen Maschine) zeichnet JavaFX per Software – langsamer, aber lauffähig | aktueller Grafiktreiber (Direct3D) |
| Drucker | optional; für Spielzettel ein Standarddrucker, ideal mit A6-Papier | – |
| Internet | nicht nötig, HTS läuft vollständig offline | – |
| Internet | für „Live im Internet“ und die Online-Anmeldung (Abschnitte 11 und 12): Laptop mit Internet (z. B. über das Handy) | WLAN oder mobiles Netz |

**Tablet-Modus (seit 1.5.0):** Info → „Bedienung“ → Tablet-Modus hält die großen Bedienelemente dauerhaft. Am Tablet öffnet ein Antippen, was mit der Maus ein Doppelklick öffnet; gesperrte Knöpfe zeigen ihren Hinweis per Antippen; beim Antippen eines Eingabefelds öffnet sich unter Windows die Bildschirmtastatur; die Teilnehmerliste hat je Zeile ein „⋮“ für die Aktionen; in der Auslosung tauscht man per Antippen (erst auswählen, dann Ziel antippen); Gruppen und KO-Baum lassen sich mit zwei Fingern (bzw. Strg + Mausrad) zoomen; hochkant steht auf der Seite „Tische“ die Warteschlange unter den Tischen. Bei längeren Vorgängen erscheint „Bitte warten“. Auf einem echten Tablet ist das noch nicht geprüft.

Windows auf ARM-Prozessoren sollte über die x64-Emulation von Windows 11 funktionieren, ist aber nicht geprüft. Die macOS-Fassung ist noch nicht auf einem echten Mac geprüft. Auf **Windows-Tablets** lässt sich HTS mit dem Finger bedienen (seit 1.1.0): Sobald ein Finger den Bildschirm berührt, werden Knöpfe, Tabellenzeilen, Menüs und Bildlaufleisten größer; langes Antippen öffnet das Kontextmenü wie ein Rechtsklick, Wischen blättert.

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

**Linux** (seit 1.1.0): `hts-<Version>-linux.tar.gz` entpacken (z. B. `tar xzf hts-1.3.0-linux.tar.gz`) und im Ordner `hts-linux` **HTS starten.sh** ausführen. **macOS** (Apple-Rechner mit M-Chip): `hts-<Version>-macos.tar.gz` entpacken; beim ersten Mal Rechtsklick auf **HTS starten.command** → „Öffnen“ (die App ist nicht signiert), danach genügt ein Doppelklick. Java ist in beiden Paketen enthalten; die Turniere liegen im Ordner `HTS Turniere` unter „Dokumente“ des Benutzers.

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

### Protokoll und Fehlerbericht (seit 1.7.0)

- HTS schreibt je Tag eine Protokolldatei in den Ordner **Protokoll** im Programmordner (seit 1.8.0; vorher im Stammordner) (`hts-JJJJ-MM-TT.log`, höchstens 5 MB, nach 30 Tagen gelöscht): Programmstart (Version, Betriebssystem, Java, Bildschirm, Speicher), wichtige Schritte (Turnier geöffnet, Klasse gestartet/beendet, Folgephase, PDF, Webserver-Status) und jeder unerwartete Fehler mit technischen Einzelheiten. **Keine Namen, Vereine oder Ergebnisse** – nur Kennungen und Klassennamen.
- **Unerwarteter Fehler:** HTS speichert das offene Turnier und zeigt „Es ist ein Fehler aufgetreten“ mit **Fehlerbericht speichern** (ZIP der Protokolle der letzten 7 Tage) und **Weiter**. Den Bericht bitte an ein GitHub-Issue anhängen.
- Die Startdateien legen Absturzberichte der Java-Laufzeit in `<Benutzerordner>\.hts` ab; sie kommen mit in den Fehlerbericht.
- In der **Info**: **Protokollordner öffnen** und **Fehlerbericht erstellen**.

Gespeichert wird automatisch nach jeder Änderung.

## 2. Programmaufbau und Bedienung

HTS hat zwei Ebenen: die Turnierübersicht und die Ansicht eines geöffneten Turniers mit ihren Seiten.

### Kopfleiste

Die weiße Kopfleiste (seit 1.7.0; vorher dunkelblau) mit grüner Linie zeigt links das HTS-Logo und daneben das geöffnete Turnier („Turnier: Name · Datum“, bei Testturnieren „TESTTURNIER: …“). Darunter liegt das Menü, rechts oben der runde Knopf zurück zur Turnierübersicht. Bei Testturnieren steht daneben ein Würfel-Knopf (Abschnitt 3). Ganz rechts steht immer der **Info-Knopf (i)**: Er zeigt Version, Signatur, die Lizenz (AGPL 3.0) samt Lizenzhinweis, Links zur Feature- und Bug-Sammlung auf GitHub sowie – seit 1.4.0 – den Bereich „Danke an:“ und die Lizenzen der mitgelieferten Bibliotheken (PDFBox, JSVG, JavaFX, Java-Laufzeit, Schrift Comic Neue) – seit 1.7.0 außerdem **Protokollordner öffnen** und **Fehlerbericht erstellen** (Abschnitt 1, „Protokoll und Fehlerbericht“). Lässt sich das Fenster mit der Maus vergrößern, bleibt die gewählte Größe erhalten (seit 1.4.0; vorher sprang es zurück). In der Turnierübersicht gibt es nur den Info-Knopf; neue Turniere legt der Knopf **Neues Turnier** auf der Seite an.

| Menüpunkt | Zweck |
| --- | --- |
| ← Turniere | zurück zur Turnierübersicht |
| Teilnehmende | Meldeliste: importieren, erfassen, suchen |
| Klassen | Klassen anlegen, Teilnehmende zuordnen, Modus festlegen, Phasen und Endergebnis |
| Tische | belegte und freie Tische, Warteschlange, Tischzuordnung, Tischnamen |
| Spiele | alle Spiele klassenübergreifend, Suche, Ergebnisse und Korrekturen, Export-Markierung |
| Präsentationsansicht | Anzeige auf einem zweiten Bildschirm starten, Wechselzeit und Ansichten wählen (Abschnitt 6) |
| Live im Internet | Turnier auf dem Webserver veröffentlichen, Datenschutz (Abschnitt 11, seit 1.5.0) |
| Urkunden drucken | ab Turnierstart: Urkunden der feststehenden Plätze auswählen, Texte einstellen, Vorschau, PDF und Druck (Abschnitt 9, seit 1.4.0) |
| Live im Internet | Veröffentlichen, Online-Anmeldung, QR-Code-Aushänge (Abschnitte 11 und 12) |
| Vorgaberegeln | nur im Vorgabeturnier: Art der Vorgabe und Grenzen (Abschnitt 13) |

„Spiele“ ist ausgegraut, bis das Turnier oder eine Klasse gestartet ist. Wird das Fenster schmal, bricht das Menü in eine zweite Zeile um; unter 1200 Pixeln Breite entfällt der Schriftzug „Hamburger Turniersoftware“, der Turniername bricht notfalls um.

### Bedienregeln

- **Automatisches Speichern:** Jede Änderung wird sofort gespeichert; es gibt keinen Speichern-Knopf.
- **Drag & Drop:** Teilnehmende, Doppelpartner, Gruppenmitglieder und KO-Plätze werden mit der Maus gezogen. Erreicht der Mauszeiger beim Ziehen den oberen oder unteren (im KO-Baum auch linken oder rechten) Rand der Ansicht, blättert sie automatisch weiter – je näher am Rand, desto schneller.
- **Suche:** Alle Suchfelder in Klassen, Gruppenphase, KO-Baum und auf der Seite „Spiele“ suchen in **Vorname, Nachname und Verein**. Mehrere Wörter (durch Leerzeichen getrennt) müssen alle passen, jedes darf in einem der drei Felder stehen, auch als Wortteil; Groß-/Kleinschreibung, Umlaute (ü = ue), Reihenfolge und Satzzeichen am Wortrand spielen keine Rolle. Beispiel Richard Michaelis, Grün-Weiß Harburg: Treffer bei „Richard Michaelis“, „Michaelis Harburg“, „richard grün“ oder nur „Harburg“ (dann auch alle anderen aus Vereinen mit „Harburg“ im Namen). Doppel werden gefunden, wenn eine der beiden Personen passt.
- **KO-Darstellungen** (KO-Baum, Spiele der KO-Qualifikation, Spielplan im PDF) zeigen hinter jedem Namen den Verein.
- **Doppelklick:** öffnet in Gruppenübersicht, KO-Baum und Spieleliste die Ergebniseingabe.
- **Texte werden nie abgekürzt** („…“ gibt es nicht): Knöpfe sind immer ausgeschrieben, wird es eng, bricht eine Knopfzeile um. Tabellenspalten sind so breit wie ihr Inhalt (notfalls mit waagerechter Scrollleiste), KO- und Qualifikationskarten so breit wie der längste Name samt Verein. Reicht die Höhe nicht, scrollt die Seite.
- **Dialoge** passen ihre Größe dem Inhalt an; auf kleinen Bildschirmen scrollen sie, statt abzuschneiden.
- **Auswahlmenüs** haben zwei Formen: Menüs, die etwas *tun* oder einstellen, sind weiß mit Rahmen und sagen, wozu sie dienen (z. B. „Aktion für Markierte wählen ▾“, „PDF / Drucken ▾“, „Gleichstand: … ▾“); Filter sind hellgrau gefüllt (z. B. „Alle ▾“).
- **Durchsuchbare Auswahl** (Vereinsfilter): Tippen filtert nur die aufklappende Liste – ein Teil des Namens genügt, Groß-/Kleinschreibung egal. Mit den Pfeiltasten bewegt man sich in der Liste; erst **Enter** oder ein Klick wählt den Eintrag und filtert die Tabelle. **Esc** oder Verlassen des Feldes stellt den bisherigen Wert wieder her; leeres Feld + Enter = „Alle“.
- **Kopfbereich einer Seite:** Unter dem Seitentitel stehen Turniername, Datum und – sobald gestartet – „● Gestartet hh:mm Uhr“. Rechts oben stehen die Knöpfe der Seite: „Turnier starten“, „Bereite Klassen starten“, nach Turnierende „Ergebnisse exportieren“ und auf der Seite „Klassen“ **Klasse anlegen**.

## 3. Turniere und Teilnehmende

### Turniere anlegen und verwalten

In der Turnierübersicht legt **Neues Turnier** ein Turnier an. Pflicht sind Name, Datum und die Anzahl der Spieltische; das Turnier ist sofort gespeichert.

- Jede Karte zeigt Datum, Anzahl Teilnehmende und Klassen sowie den Status.
- **Ausrichter (seit 1.5.0):** Reiter in der Turnierübersicht. Name des Verbands bzw. Vereins und Logo (**Logo hochladen** als PNG, JPG oder – seit 1.7.0 – SVG; SVG wird als scharfes PNG mit 1200 Bildpunkten übernommen, externe Verweise in der Datei werden nicht geladen; **Standard-Logo (HaTTV)** oder **Kein Logo**). Gilt für alle Turniere auf dem Rechner und liegt seit 1.8.0 im Programmordner (vorher im Ablageordner der Turniere; beim ersten Start von 1.8.0 übernommen). Das Logo steht oben rechts auf dem Schiedsrichterzettel und auf den Live-Seiten, der Name seit 1.8.0 auch auf der Startseite der Live-Seiten.
- **Webserver (seit 1.5.0):** Reiter in der Turnierübersicht. Adresse und geheimer Schlüssel des Webservers (nur auf diesem Rechner, nicht in der Turnierdatei) und **Verbindung testen** (Abschnitt 11). Seit 1.7.0 gesperrt: Adresse und Schlüssel erscheinen nur verdeckt („http•••••.de“, „••••••••“); **Entsperren** fragt das Admin-Passwort (40 Zeichen, nach mehreren Fehlversuchen Wartezeit). Entsperrt: Felder bearbeiten (Werte verdeckt, **Anzeigen** zeigt sie), **Speichern**, **Verbindung testen**, **Sperren**; beim Verlassen des Reiters wird wieder gesperrt. Adresse und Schlüssel liegen verschlüsselt auf dem Rechner (AES-256; unter Windows an das Benutzerkonto gebunden) – auf einem anderen Rechner bzw. unter einem anderen Benutzer müssen sie neu eingetragen werden, bis dahin pausiert das Veröffentlichen.
- **Programm (seit 1.8.0):** Reiter in der Turnierübersicht. **Dunkelmodus aktivieren** – dunkler Hintergrund mit heller Schrift für alle Fenster und Dialoge, gilt sofort und wird gemerkt; Ausdrucke und PDFs bleiben hell. **Speicherorte** für die HTS-Turnierdateien (speichern und laden), die PDF-Exporte und die XML-Exporte (click-TT), je mit **Ordner wählen** und **Vorgabe**. Vorgabe: neue Turnierdateien direkt in `Dokumente\HTS` (bei gleichem Namen „Name (2).hts“), alle PDF- und XML-Dateien gemeinsam in `Dokumente\HTS\Export files`; deren Dateinamen beginnen mit Turniername und Datum. Turniere aus älteren Versionen bleiben in ihrem bisherigen Ordner (`Dokumente\HTS Turniere\…`) und werden dort weiter gespeichert. **Programmordner**: Ausrichter, Programmeinstellungen und der Ordner „Protokoll“ liegen dort, wo „HTS starten“ liegt (ist der Ordner nicht beschreibbar, z. B. unter „C:\Programme“, im Benutzerordner unter `.hts`); **Programmordner öffnen** und **Einstellungen aus älterer Version übernehmen** (Ordner der bisherigen HTS-Version wählen; bei Versionen vor 1.8.0 den Ordner „HTS Turniere“ unter „Dokumente“).
- **Turniere und Archivierte Turniere (seit 1.4.0):** Der Reiter „Turniere“ zeigt alle Turniere, deren Datum höchstens 14 Tage zurückliegt (auch künftige und solche ohne Datum); ältere stehen nur noch unter **Archivierte Turniere**. Beide Listen sind nach Datum sortiert, das neueste zuerst. Öffnen, Bearbeiten und Löschen funktionieren in beiden Listen gleich; ein manuelles Archivieren gibt es nicht – ändert man das Datum, wandert das Turnier entsprechend.
- **Öffnen** oder Doppelklick öffnet das Turnier, **Bearbeiten** ändert Name, Datum, Spieltische und den Ort der Turnierdatei, **Löschen** entfernt die Turnierdatei (Exporte bleiben erhalten).
- Beim Anlegen zeigt der Dialog, wo die Turnierdatei landet (Standard: `Dokumente\HTS Turniere\<Name>\Turnierdatei`); **Durchsuchen** wählt einen anderen Ordner (Abschnitt 1).
- **Turnierdatei übernehmen** holt eine vorhandene `.hts`-Datei als neues Turnier herein.
- **Fertiges Turnier importieren (click-TT)** liest eine click-TT-Ergebnisdatei (Format TournamentPortal, egal ob aus click-TT, poinTT oder HTS) und legt daraus ein neues, abgeschlossenes Turnier an: je Konkurrenz eine Klasse, Phase 1 als Gruppenphase oder KO-System (inkl. Spiel um Platz 3), alle Spiele mit Sätzen und die Platzierungen. Nicht Angetretene stehen ohne Klasse in der Teilnehmendenliste. Spätere Phasen aus der Datei werden nicht übernommen, sondern im Bericht genannt; Unstimmigkeiten erscheinen als Hinweise statt eines Abbruchs.
- **Kopie von:** Beim Anlegen lässt sich ein bestehendes Turnier als Vorlage wählen – **ohne Auslosung** (Teilnehmende, Klassen, Zuordnungen, Einstellungen, Tische; Standard), **alles** (inkl. Auslosungen, Spielen und Ergebnissen) oder **nur Teilnehmende** (ohne Klassen). Das Original bleibt unverändert.
- **Vorgabeturnier:** Option beim Anlegen (später nicht umschaltbar); siehe Abschnitt 13.
- **Testturnier:** Das Häkchen beim Anlegen bzw. Bearbeiten markiert ein Übungsturnier (Karte „Testturnier“). Dort trägt der Würfel-Knopf in der Kopfleiste **alle offenen Spiele der laufenden Phasen** zufällig und regelgerecht ein (Best of 5 bzw. 7, Verlängerung bis höchstens 20:18), inklusive der Folgerunden im KO- und Schweizer System – ohne Spielzettel zu drucken und ohne Phasen abzuschließen. Angehaltene Phasen werden nicht simuliert. Exporte sind auch für Testturniere möglich.

### Teilnehmende aus click-TT importieren

Auf der Seite „Teilnehmende“ liest **XML der Teilnehmenden importieren** den Turnierexport aus click-TT ein.

1. XML-Datei wählen; HTS zeigt eine Vorschau mit Teilnehmenden und Konkurrenzen. Jede Konkurrenz hat einen Haken **„Importieren“**: Abgewählte Klassen werden nicht angelegt, und wer nur in abgewählten Klassen gemeldet ist, wird nicht übernommen. Die Zahlen im Dialog passen sich an.
2. Nach dem Bestätigen werden fehlende Klassen angelegt; sie gelten als **„aus XML importiert“** (wichtig für die Startsperre, Abschnitt 4).
3. Die Gemeldeten werden als **Meldung** übernommen: „In der Klasse“ beginnt bei 0. Wer am Turniertag anwesend ist (Spalte „Angemeldet“), erscheint in der Klasse unter „Verfügbar“ und wird von dort übernommen (Abschnitt 4).
4. **Doppel:** Der in click-TT gemeldete Partner wird gemerkt; „Alle anwesenden Gemeldeten übernehmen“ bildet daraus die Paarung. Wer **ohne Partner/in** gemeldet ist, steht in der Doppelklasse unter „Verfügbar“ (zählt bei „Partner zulosen“ mit). Seit 1.9.0 stehen gemeldete Paarungen (Doppel und Mixed) schon nach dem Import unter „Doppelpaarungen“; solange eine/r der beiden noch nicht angemeldet ist, ist die Paarung orange markiert („nicht angemeldet: Name“), bei endgültig abgemeldeter Partnerin bzw. abgemeldetem Partner rot („Paarung auflösen“). Auslosen und Starten der Klasse sind so lange gesperrt.
5. Wartelisten-Meldungen sind **Nachrücker**: Sie kommen nicht in die Klasse, sondern auf die Warteliste (Spalte „Nachrücker“).

Ein erneuter Import ergänzt und aktualisiert, löscht aber niemanden. HTS legt eine Kopie der importierten Datei ab; sie ist die Grundlage für den späteren click-TT-Export (Abschnitt 10).

### Von Hand erfassen

**Teilnehmende neu erfassen** fragt Vorname, Nachname, Geschlecht, Jahrgang, Verein und TTR ab; alle Felder sind Pflicht (im Vorgabeturnier sind Verein und Jahrgang freiwillig). In der Klassenansicht kann die neue Person direkt der Klasse zugeordnet werden. Von Hand erfasste Personen gelten sofort als angemeldet.

### Liste, Filter und Markierung

- **Filter:** Vorname, Nachname, Geburtsdatum (Jahrgang oder Datum), Geschlecht, **Verein** (durchsuchbare Auswahl, Abschnitt 2) und **Klasse** (gemeldet, in der Klasse oder Nachrücker dieser Klasse). Alle Filter lassen sich kombinieren; Umlaute und Groß-/Kleinschreibung spielen keine Rolle.
- **Sortieren** nach Vorname, Nachname, TTR und Setzplatz; Personen ohne TTR stehen in beiden Sortierrichtungen unten.
- **Spalten:** „Angemeldet“ (siehe unten), „Nachrücker“ (✓ = auf einer Warteliste), „Setzplatz“, Name, Geburtsdatum, Geschlecht, Verein, TTR, Klassen, Lizenz-Nr.
- **Setzplatz:** Ist die Liste auf eine **per XML importierte Klasse** gefiltert, erscheint die Spalte „Setzplatz“ – die Rangfolge nach TTR unter den Gemeldeten dieser Klasse. Gleicher TTR ergibt denselben Platz (z. B. 3 und 3, danach 5); ohne TTR stehen alle gemeinsam ganz unten; bei Doppeln zählt der Durchschnitts-TTR der Paarung. Endgültig Abgemeldete und reine Nachrücker bekommen keinen Setzplatz.
- **Markieren:** Klick markiert eine Zeile, Strg+Klick schaltet einzelne Zeilen um, Umschalt+Klick markiert einen Bereich. Die Markierung lässt sich ganz aufheben: die einzige markierte Zeile noch einmal anklicken, in den leeren Bereich unter den Zeilen klicken oder **Esc** drücken. Doppelklick öffnet „Bearbeiten“.

### Anwesenheit am Turniertag

Die Spalte **„Angemeldet“** bedeutet: am Turniertag anwesend. Sie lässt sich per Klick abhaken; seit 1.9.0 melden sich Teilnehmende bzw. Betreuende auch über die Online-Anmeldung an oder ab (Abschnitt 12) – solche Zeilen zeigen neben dem Haken „online“, und „Aktion für Markierte → Online-Meldung zurücknehmen“ stellt den Stand davor wieder her.

| Zustand | Darstellung | Bedeutung |
| --- | --- | --- |
| offen | leeres Kästchen | noch nicht angemeldet |
| angemeldet | Haken | anwesend |
| Abmeldung per Handy | Zeile orange, „Abmeldung ?“ | nur noch in Turnierdateien bis 1.8.0 (Handy-Zugang im Hallen-WLAN) – über „Aktion für Markierte“ ändern |
| endgültig abgemeldet | Zeile gelb, „abgemeldet“ | spielt nicht; nur die Turnierleitung kann das zurücknehmen |

Über der Liste stehen in einer Zeile:

- **Aktion für Markierte wählen ▾:** angemeldet (anwesend), endgültig abmelden, Abmeldung zurücknehmen – jeweils für die markierten Zeilen.
- **Abgemeldete als PDF:** Liste der endgültig Abgemeldeten je Klasse (Abschnitt 9); aktiv, sobald mindestens eine Person endgültig abgemeldet ist.
- **Rechtsklick** auf eine Zeile bietet dieselben Aktionen für diese Person.

**Nachrücker** (nur auf Wartelisten) zählen nicht zu den Angemeldeten: Sie haben kein Kästchen und keine Zeilenfarbe, zählen nicht in der Statuszeile, blockieren keinen Start und können sich nicht per Handy an- oder abmelden. Wird ein Nachrücker in eine Klasse übernommen, gilt er automatisch als angemeldet. Die Statuszeile unten zeigt z. B. „48 Teilnehmende · 36 angemeldet · 4 abgemeldet · 4 Abmeldung(en) zu bestätigen“.

### Startgebühren

Seit 1.0.0, überarbeitet in 1.1.0. Die Gebühr einer Person ist die Summe der Startgebühren ihrer Einzelklassen (Doppel ohne Gebühr) plus ggf. die Nachmeldegebühr. Die Seite „Teilnehmende“ zeigt die Spalten „Gebühr“ und „Bezahlt“.

- **Bezahlen:** über **Aktion für Markierte wählen ▾** oder Rechtsklick – **Bezahlt – bar**, **Bezahlt – Überweisung**, **Gebühr erlassen** (mit optionalem Grund, erscheint in der Kassenübersicht) und **Zahlung rückgängig** (Anmeldung und Klassen bleiben). Gesammelt je Verein über **Startgebühren ▾ → Verein abrechnen** mit Quittung als PDF; dazu eine **Kassenübersicht** als PDF.
- **Bezahlt heißt angemeldet:** Wer als bezahlt oder erlassen markiert wird, gilt automatisch als angemeldet – angemeldet heißt aber nicht bezahlt.
- **In die Klassen kommen nur Bezahlte:** Unter „Verfügbar“ stehen vor dem Turnierstart nur Bezahlte; „Alle anwesenden Gemeldeten übernehmen“ nimmt nur Bezahlte. Nach dem Turnierstart erscheinen Angemeldete ohne Zahlung hellblau markiert (auch in „Teilnehmende“ mit Hinweis und „Nur diese zeigen“) und lassen sich übernehmen – oder gleich mit **Bezahlt – bar und in die Klasse**.
- Beim Start einer Klasse warnt HTS vor offenen Gebühren; der Start bleibt möglich.
- Ob Quittungen und Kassenübersicht eine Kassensicherung (TSE) erfordern, bitte mit dem Kassenwart bzw. Steuerberater klären.

## 4. Klassen

Eine Klasse ist eine Konkurrenz, z. B. „Mädchen U15 Einzel“. Links steht die Liste der Klassen, rechts die gewählte Klasse mit ihren Reitern.

### Klasse anlegen

**Klasse anlegen** fragt in Auswahllisten Name, Spielart (Einzel, Doppel, 2er bzw. 3er Turnier), Alter, wer mitspielen darf, Jahrgangsgrenzen und eine Höchstzahl ab; daraus ergibt sich der Klassentyp.

| Typ | Wer darf melden |
| --- | --- |
| Erwachsene | alle |
| Damen | nur weiblich |
| Jugend | jünger als 21 Jahre am Turniertag, weiblich und männlich |
| Mädchen | jünger als 21 Jahre, nur weiblich |

Jugendliche dürfen sich zusätzlich bei Erwachsenen bzw. Damen melden. Jahrgangsgrenzen sind optional (z. B. „Jg. 2012 und jünger“). Die Höchstzahl ist nur eine Kontrolle: Wer über der Grenze gemeldet ist, wird hervorgehoben.

**Startgebühr** (seit 1.0.0): Jede Klasse hat eine Startgebühr – aus click-TT vorbelegt, im Dialog änderbar. Gebühren zählen nur für Einzelklassen; Doppel sind gebührenfrei. Die Nachmeldegebühr gilt je Turnier für alle, die nicht in der click-TT-Datei standen (Turnierübersicht → **Bearbeiten**). Zahlung und Kasse: Abschnitt 3, „Startgebühren“. Seit 1.9.0 gibt es Spalten „Bezahlt“ und „Gebühr“, den hellblauen Hinweis und alle Zahlungsaktionen nur, wenn eine Gebühr über 0 € eingestellt ist (Startgebühr einer Einzelklasse oder Nachmeldegebühr) – sonst genügt der Haken „Angemeldet“.

**Zeitplan** (seit 1.0.0): Mit „mit Zeitplan“ und den Minuten je Runde beginnt Runde n frühestens Start + (n − 1) × Rundendauer; die Runden werden nacheinander eingereiht. Ist eine Runde vor dem Zeitplan fertig, fragt HTS je Gruppe bzw. Runde „Jetzt starten“ oder „Warten bis hh:mm Uhr“; wartende Runden stehen im Phasen-Reiter mit **Runde jetzt starten** und werden zur geplanten Zeit automatisch eingereiht. Seit 1.1.0:

- **Startzeit:** Optional eine Uhrzeit (höchstens 12 Stunden vor oder nach jetzt); leer = ab dem Start der Klasse. Liegt die Startzeit in der Zukunft, wartet Runde 1 bis dahin („Runde jetzt starten“ zieht sie vor); liegt sie in der Vergangenheit, laufen die fälligen Runden ohne Wartefrage nacheinander. Auch nach dem Start lässt sie sich verschieben – noch nicht gestartete Runden bekommen neue Zeiten.
- **Anzeige:** Hinter jeder Runde steht die geplante Zeit, z. B. „R2 · ab 14:25“ – in der Phasenübersicht, im PDF der Phase, auf den Gruppenaushängen, in der Präsentationsansicht und bei den Platzierungsplänen. Vor dem Start erscheint keine Zeit (außer es ist eine Startzeit eingestellt).

**Klasse bearbeiten** ist möglich, bis in Phase 1 das erste Ergebnis eingetragen ist; danach zeigt der Dialog die Einstellungen nur noch zum Lesen. Im Vorgabeturnier gibt es nur „Erwachsene“ und „Damen“.

**Klasse löschen** (Knopf „Löschen“ oder Taste **Entf** in der Klassenliste) fragt nach und nennt, was verloren geht – nur, was es in der Klasse wirklich gibt: Zuordnungen, Meldungen, Auslosung, Phasen, eingetragene Ergebnisse. Vorausgewählt ist „Abbrechen“. Gibt es Ergebnisse, folgt eine zweite Rückfrage.

**Klassenkopf:** Name und Kurzinfo (z. B. „Einzel · 66 in der Klasse · Jugend · Gruppenphase“) stehen in einer Zeile, rechts der Status („ausgelost“, „bereit“, „läuft“, „angehalten“, „beendet“) und bei Bedarf die Marke **„Start gesperrt“**.

### Reiter einer Klasse

- **Teilnehmende der Klasse** – Zuordnung und Doppelpaarungen
- **Phase 1, Phase 2, Phase 3** – erscheinen erst nach „Modus festlegen“, z. B. „Phase 1 – Gruppenübersicht“, „Phase 2 – KO-System“; bei Haupt- und Trostrunde zwei Reiter „Phase 2 – Hauptrunde“ und „Phase 2 – Trostrunde“ (seit 1.2.0)
- **Phase n – Korrektur** (orange, seit 1.3.0) – erscheint hinter der laufenden Phase, wenn eine Ergebniskorrektur in der Vorphase diese Phase verändert (Abschnitt 7, „Korrekturen“)
- **Phase n – deaktiviert** (grau, durchgestrichen, seit 1.3.0) – eine durch „Phase n weiterführen“ ersetzte Phase, nur zum Nachsehen; mehrere werden durchnummeriert („deaktiviert (2)“)
- **Endergebnis** – erscheint erst, wenn die Klasse beendet ist

**Blättern (seit 1.3.0):** Je Seite zeigt die Reiterleiste höchstens 6 Reiter. Hat eine Klasse mehr, stehen rechts über der Leiste die Pfeile **‹ ›** und „Seite x / y“; ein Pfeil wechselt die Seite und wählt deren ersten Reiter. Der gewählte Reiter springt beim Öffnen der Klasse automatisch auf seine Seite.

Wer eine Klasse in der Liste wählt, landet direkt in der **aktuellsten Phase**; bei einer beendeten Klasse im „Endergebnis“, bei einer noch nicht ausgelosten Klasse unter „Teilnehmende der Klasse“.

### Teilnehmende zuordnen

- **Verfügbar** (links) = angemeldete (anwesende) Personen, die noch nicht in der Klasse sind; **In der Klasse** (rechts) = spielt mit.
- **Alle anwesenden Gemeldeten übernehmen** holt alle angemeldeten Gemeldeten dieser Klasse auf einmal herein (Doppel mit dem gemeldeten Partner).
- Zuordnen per Drag & Drop, Doppelklick oder Pfeilknöpfe; eine Person kann in mehreren Klassen spielen. Nachrücker zieht die Turnierleitung von Hand in die Klasse. Wer zurück nach „Verfügbar“ gezogen wird, bleibt angemeldet und ist wieder gemeldet bzw. Nachrücker (seit 0.42.0).
- Filter: Suche (Vorname, Nachname, Verein) und Geschlecht; bei „Verfügbar“ zusätzlich „nur passende“ (Alter und Geschlecht der Klasse). Den Vereinsfilter (durchsuchbar) gibt es nur in Doppelklassen – dort hilft er beim Zusammenstellen der Paare. Seit 1.9.0 merkt sich HTS die Filter über „Verfügbar“ je Klasse (solange das Turnier offen ist); eine noch nicht geöffnete Klasse beginnt mit den Vorgaben.
- Die Nachrücker-Spalte zeigt, wer für diese Klasse auf der Warteliste stand.

**Startsperre für importierte Klassen:** Eine Klasse „aus XML importiert“ kann erst starten, wenn alle Gemeldeten angemeldet oder endgültig abgemeldet sind (Nachrücker zählen nicht). Solange das nicht so ist, trägt der Klassenkopf die Marke „Start gesperrt“, und links neben **Teilnehmende fertig – Modus festlegen** steht der Hinweis „XX Gemeldete noch nicht an- oder abgemeldet.“ mit dem Link **Offene Personen anzeigen** (öffnet „Teilnehmende“ mit dieser Klasse im Filter). „Turnier starten“ startet die übrigen bereiten Klassen und nennt die gesperrten.

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
| Modus | alle | KO-System, Gruppenphase (jeder gegen jeden), Schweizer System, Schweizer System nach Badeni, Doppeltes K.-o.-System (die beiden seit 1.2.0), Gruppenphase mit Freistellung (seit 1.3.0, nur Einzel) |
| Setzung | KO, Schweizer System | nach Setzung (TTR) oder zufällig |
| Setzung | Gruppen | TTR Setzung (Schlange), TTR Setzung (Stärkegruppen), Gruppenkopf gesetzt oder zufällig |
| Gruppen | Gruppen | Gruppengröße oder Anzahl Gruppen |
| Bei gleichem Spielverhältnis | Gruppen | Satz- und Ballverhältnis oder direkter Vergleich |
| Anzahl Runden | Schweizer System | Vorschlag: Runden eines KO-Systems + 1; nach Badeni 5 |
| Gewinnsätze | alle | 3 (Best of 5, Standard) oder 4 (Best of 7) – gilt für alle Phasen der Klasse |
| Vereinsgleichheit beachten | KO, Gruppen | eigene Zeile mit Checkbox: an (Standard) oder aus |

Doppel werden nach dem Doppel-TTR gesetzt (Abschnitt 4). Die Auswahllisten im Dialog sind gleich breit.

**Werkzeugzeile im Phasen-Reiter** (eine Zeile, bricht nur um, wenn der Platz nicht reicht): Suche, „Gleichstand: … ▾“ (nur Gruppen), dann die Knöpfe in dieser Reihenfolge – **Auslosung verwerfen** (bzw. in Phase 2 **Zurück zu Phase 1**), **Neu auslosen**, **bereit**, **starten**, nach dem Start stattdessen die Phasen-Knöpfe (Phase anhalten/fortsetzen, Phase neu starten, Phase vorzeitig beenden, Phase abschließen …), zuletzt **PDF / Drucken ▾** – und am Ende die Infozeile, z. B. „3 Gruppen · 23 Spiele offen“.

- **Neu auslosen** lost mit denselben Einstellungen neu. Bei TTR-Setzung kommt dieselbe Einteilung heraus, außer wo der Zufall mitentscheidet: bei gleichem TTR, bei Personen ohne TTR und – selten – bei der Auflösung von Vereinsgleichheiten. Außerdem wird das feste Los neu bestimmt; Tauschaktionen per Drag & Drop gehen verloren.
- **Auslosung verwerfen** (Phase 1) bzw. **Zurück zu Phase 1** (Phase 2 wird gelöscht, Phase 1 ist wieder offen) geht, solange in der letzten Phase noch kein Ergebnis eingetragen ist – auch nach dem Start. Mit Sicherheitsabfrage; Spiele an Tischen werden zurückgezogen, die Tische sind sofort frei.

**Doppelklassen:** Bei mehr als 5 Doppeln ist Phase 1 immer ein KO-System. Bis 5 Doppel stehen KO-System oder „Gruppenphase“ (eine Gruppe, jeder gegen jeden) zur Wahl. Das Schweizer System (auch nach Badeni) gibt es für Doppel nicht, und Vereinsgleichheit spielt bei Doppeln keine Rolle. Das doppelte K.-o.-System ist auch für Doppel wählbar.

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

**Spielreihenfolge in den Gruppen:** Die Spiele richten sich nach der **Position in der Gruppe nach der Auslosung** (P1 = erste Position, bei TTR-Setzung der Gruppenkopf; nach einem Tausch per Drag & Drop die Position, auf die getauscht wurde): zuerst stark gegen schwach, zum Schluss stark gegen stark. Bei ungerader Größe entfällt jeweils das Spiel gegen die fehlende letzte Position (7er wie 8er ohne P8 usw.). Gilt für alle Gruppenphasen (Phase 1 und 2); Gruppen mit mehr als 12 Mitgliedern werden nach dem Kreisverfahren eingeteilt.

| Größe | Runde 1 | Runde 2 | Runde 3 | Runde 4 | Runde 5 | Runde 6 | Runde 7 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 3/4 | 1-3 · 2-4 | 1-4 · 2-3 | 1-2 · 3-4 | | | | |
| 5/6 | 1-5 · 2-4 · 3-6 | 1-6 · 2-5 · 3-4 | 1-4 · 2-3 · 5-6 | 1-3 · 2-6 · 4-5 | 1-2 · 3-5 · 4-6 | | |
| 7/8 | 1-8 · 2-7 · 3-6 · 4-5 | 1-7 · 2-6 · 3-5 · 4-8 | 1-6 · 2-5 · 3-8 · 4-7 | 1-5 · 2-8 · 3-7 · 4-6 | 1-4 · 2-3 · 5-8 · 6-7 | 1-3 · 2-4 · 5-7 · 6-8 | 1-2 · 3-4 · 5-6 · 7-8 |

9er/10er (9 Runden): 1-10 2-9 3-8 4-7 5-6 · 1-9 2-8 3-10 4-6 5-7 · 1-8 2-6 3-7 4-10 5-9 · 1-7 2-10 3-6 4-9 5-8 · 1-6 2-5 3-4 7-10 8-9 · 1-5 2-4 3-9 6-10 7-8 · 1-4 2-3 5-10 6-8 7-9 · 1-3 2-7 4-5 6-9 8-10 · 1-2 3-5 4-8 6-7 9-10.

11er/12er (11 Runden): erst obere gegen untere Hälfte (Runde 1: 1-12 2-11 3-10 4-9 5-8 6-7, danach jeweils um eine Stelle versetzt), ab Runde 7 innerhalb der Hälften nach dem 6er-Schema; letzte Runde 1-2 3-5 4-6 7-8 9-11 10-12.

**Gemeinsames Ende:** Gruppen mit mehr Runden beginnen früher – eine Gruppe startet um so viele Runden eher, wie sie mehr Runden hat (z. B. spielen 5er-Gruppen ihre Runden 1 und 2 vor den 4er-Gruppen), damit alle Gruppen etwa gleichzeitig fertig werden. Die Spielnummern folgen dieser Reihenfolge.

### Gruppenphase mit Freistellung

Seit 1.3.0, nur im Einzel und ab 10 Teilnehmenden. Die Setzplätze **S1–S4** spielen keine Gruppenspiele und steigen erst in Phase 2 ein; alle anderen spielen eine normale Gruppenphase. Beispiele: weibliche Jugend 15/19 mit 20 Spielerinnen – 4 frei + 4 Gruppen × Plätze 1–3 = 16er-Feld; männliche Jugend 15/19 mit 28 Spielern – 4 frei + 6 Vierergruppen × Plätze 1–2 = 16er-Feld.

- **Freigestellte wählen:** Im Modus-Dialog stehen vier Auswahllisten S1–S4. Vorausgewählt sind die 4 mit dem höchsten TTR (bei gleichem TTR entscheidet das Los); jede Liste lässt sich ändern, **Auf TTR-Vorschlag zurücksetzen** stellt den Vorschlag wieder her. Ausgelost wird erst, wenn alle vier Plätze mit verschiedenen Personen belegt sind.
- **Gruppen:** Gruppengröße oder Anzahl Gruppen wie gewohnt (ohne die Freigestellten), dazu **Weiter je Gruppe:** Plätze 1–2 oder Plätze 1–3.
- **Kein Freilos:** 4 + Gruppen × Weiterkommende muss ein volles KO-Feld ergeben (8, 16, 32, 64 oder 128). Sonst steht im Dialog die Rechnung mit Vorschlägen, z. B. „4 Freigestellte + 5 Gruppen × 2 Weiterkommende = 14 – das ist kein KO-Feld ohne Freilose … Möglich: 6 Gruppen (à 4) × Plätze 1–2 = 12 + 4 = 16er-Feld“, und **Auslosen** bleibt gesperrt.
- **Ansicht:** Über den Gruppen steht die Leiste **„Freigestellt für das Achtelfinale“** (bzw. Viertel-/Sechzehntelfinale) mit S1–S4, Verein und TTR. Vor dem Start lässt sich eine freigestellte Person per Drag & Drop mit einem Gruppenmitglied tauschen.
- **Phase 2** ist immer ein KO-System ohne Freilose („Phase abschließen“ bietet nur dieses bzw. das Endergebnis). Bei 4 Gruppen × Plätze 1–3 gelten feste Paarungen von oben nach unten: S1–D3, B1–D2, A1–C2, S4–C3, S3–A3, C1–B2, D1–A2, S2–B3. **Bei 6 Gruppen × Plätze 1–2 (seit 1.7.0)** wird je Baumhälfte ausgelost: oben S1 und S4, unten S2 und S3 (wer oben bzw. unten im Viertel steht, entscheidet das Los); drei ausgeloste Gruppen haben ihren Ersten oben und den Zweiten unten, die anderen drei umgekehrt – Erster und Zweiter einer Gruppe treffen also frühestens im Finale aufeinander, und jeder Gruppenbuchstabe kommt je Hälfte nur einmal vor. Die Platzarten von oben nach unten: obere Hälfte S–2 · 1–1 · 1–2 · 2–S, untere Hälfte S–2 · 2–1 · 1–1 · 2–S (Beispiel: S1–D2 · F1–E1 · A1–B2 · C2–S4 | S3–E2 · A2–B1 · C1–D1 · F2–S2). **Neu auslosen** lost erneut; vor dem Start lässt sich wie gewohnt tauschen. In allen anderen Fällen ist die Setzliste S1–S4, dann die Gruppenersten, dann die Zweiten (ggf. Dritten), jeweils nach Spiel-, Satz- und Punkteverhältnis (Gleichstand: Los); Setzplatz 1 spielt gegen den letzten, 2 gegen den vorletzten usw. So spielen die Freigestellten gegen die schlechtesten Gruppenzweiten (S1 gegen den schlechtesten), die besten Gruppenersten gegen die übrigen Zweiten und die schlechtesten Gruppenersten gegeneinander (bei 6 × 2: S1–Z6, S2–Z5, S3–Z4, S4–Z3, E1–Z2, E2–Z1, E3–E6, E4–E5). Zwei aus derselben Gruppe treffen in Runde 1 nicht aufeinander (Tausch mit dem Nächstplatzierten).
- **Endergebnis:** KO-Platzierungen, dahinter die übrigen nach Gruppenplatz; solange die Gruppenphase läuft, stehen die Freigestellten als „freigestellt (S1)“ usw. oben.

### Schweizer System

- Runde 1: nach Setzung spielt die obere gegen die untere Hälfte, sonst zufällig.
- Jede weitere Runde wird automatisch nach der Tabelle gepaart, sobald die vorige Runde komplett gespielt ist.
- Niemand spielt zweimal gegen denselben Gegner.
- Bei ungerader Zahl gibt es je Runde ein Freilos, bevorzugt für Tabellenletzte und höchstens eines je Person. Es zählt als Sieg.
- Das Schweizer System gibt es nur in Phase 1, und es folgt keine weitere Phase: Nach der letzten Runde gibt es nur das Endergebnis.
- **Ansicht (seit 1.3.0, auch bei Badeni):** Die Knöpfe **Weitere Runde** und **Spieler pausieren** (nur Badeni bzw. bei laufender Klasse) stehen mit dem Infotext rechts daneben als feste Leiste über der Tabelle, oben und unten durch eine graue Linie abgesetzt; sie bleibt beim Scrollen stehen. Rechts neben der Tabelle stehen die Spiele: oben immer die aktuelle Runde mit „läuft · n offen“, darunter die früheren Runden; sind alle Runden gespielt, von der letzten zur ersten. Die Plätze 1–3 liegen (sobald gespielt wurde) in runden Chips in Gold, Silber und Bronze.

### Schweizer System nach Badeni

Seit 1.2.0, nach der Unterlage des BaTTV (A. Enichlmayr). Alle spielen in einem Feld; je Runde bilden die Spieler mit gleich vielen Siegen eine **Siegegruppe**.

- **Paarung:** Innerhalb der Siegegruppe (nach Reihung) spielt – wie beim Skispringen – der Erste gegen den Letzten, der Zweite gegen den Vorletzten …, die Mitte gegeneinander. Runde 1 ist nach TTR gereiht (bei gleichem TTR gelost).
- **Ungerade Siegegruppe:** Der Schwächste der Gruppe spielt in der nächstniedrigeren Gruppe mit.
- **Freilos** (ungerades Feld): der Beste der unteren Hälfte der Reihung, niemand zweimal. Es zählt als Sieg.
- **Keine Wiederholungen:** Wäre eine Paarung schon gespielt, kommt der nächstmögliche Gegner; geht es in einer Gruppe gar nicht, spielt sie mit der nächsten zusammen. Nur wenn es überhaupt nicht anders geht, wird wiederholt.
- **Reihung:** 1. Siege (Freilos zählt als Sieg), 2. TTR, 3. Satzdifferenz, dann Balldifferenz und festes Los – ohne Buchholz. Die Spalten BHZ und FBHZ entfallen deshalb in Tabelle, Präsentationsansicht und PDF (seit 1.3.0); stattdessen steht dort der TTR.
- **Runden:** Vorschlag 5 (mindestens 5 empfohlen). Im Phasen-Reiter hängt **Weitere Runde** eine Runde an (z. B. als Platzierungsrunde); sie wird gepaart, sobald die laufende Runde komplett gespielt ist.
- **Spieler pausieren:** wählt Personen, die ab der nächsten Runde nicht gepaart werden (kein Freilos); ihr Stand bleibt erhalten, das Häkchen lässt sich wieder entfernen.
- Wie beim Schweizer System gibt es keine Phase 2 – das Endergebnis ist die Tabelle.

### Doppeltes K.-o.-System

Seit 1.2.0 (WO 7.4), als Modus für Phase 1 oder als Phase 2 nach einer Gruppenphase (Abschnitt 8). Auslosung wie beim KO-System (Setzliste, Freilose an die Besten, Vereinsgleichheit in Runde 1).

- **Siegerrunde:** ein normales KO-System. Wer verliert, scheidet nicht aus, sondern spielt in der **Trostrunde** weiter.
- **Trostrunde:** Die Verlierer der ersten Runde spielen gegeneinander; die Verlierer späterer Runden der Siegerrunde kommen jeweils dazu – „gekreuzt“, damit sich Begegnungen möglichst nicht wiederholen. Wer in der Trostrunde verliert, ist ausgeschieden (zweite Niederlage).
- **Finale:** Sieger der Siegerrunde gegen Sieger der Trostrunde. Gewinnt die Siegerin/der Sieger der Trostrunde, haben beide eine Niederlage – dann folgt automatisch ein **Stichkampf**; sonst entfällt er.
- **Plätze:** Sieger des Finales bzw. Stichkampfs 1., Verlierer 2., Verlierer im Finale der Trostrunde 3., davor 4., dann je Trostrunden-Runde ein geteilter Bereich.
- Wird das Finale so korrigiert, dass der Stichkampf entfällt (oder nötig wird), passt HTS ihn an – nach Rückfrage, wenn er schon gespielt ist.

### Vereinsgleichheit

Ist „Vereinsgleichheit beachten“ an, trennt die Auslosung Vereinskameraden, solange das möglich ist:

- **Schlange:** Die Schlange wird unterbrochen, der Nächststärkere ohne Vereinsgleichheit kommt auf den Platz.
- **Stärkegruppen:** Aus derselben Stärkegruppe wird jemand ohne Vereinsgleichheit gezogen.
- **Nachbessern bei Gruppen** (Schlange, Stärkegruppen, zufällig): Nach der Verteilung tauscht HTS Mitglieder mit Vereinsgleichheit gegen möglichst gleich starke Mitglieder anderer Gruppen, solange das die Vereinsgleichheiten verringert. Gruppenköpfe und Stärkegruppen bleiben dabei erhalten.
- **Unvermeidbare Vereinsgleichheit** (ein Verein hat mehr Teilnehmende als es Gruppen gibt; Gruppen mit 3 bis 10 Mitgliedern, bis zu 5 aus einem Verein je Gruppe): Die Vereinsmitglieder werden nach TTR durchgezählt. Die ersten (so viele wie Gruppen) kommen in verschiedene Gruppen; danach gilt **Schwächste/r zu Stärkste/m**: der/die Schwächste kommt zur Nr. 1 des Vereins, der/die Zweitschwächste zur Nr. 2 usw. Ab drei je Gruppe wechselt die Richtung wie bei der Schlange. Beispiel 4 Gruppen, 12 aus einem Verein: A = 1, 8, 9 · B = 2, 7, 10 · C = 3, 6, 11 · D = 4, 5, 12. Dafür tauscht HTS mit einem möglichst gleich starken Mitglied der Zielgruppe (höchstens Gruppenzahl − 1 Plätze in der Schlange entfernt; Gruppenköpfe bleiben). Wollen zwei Vereine in dieselbe Gruppe und passt das nicht, hat der Verein mit dem stärkeren Stärksten Vorrang; der andere wird eine Gruppe weiter gesetzt. Gilt für alle Setzungsarten, nicht für Doppel.
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
- **Drucken** auf einer Gruppenkarte erzeugt das PDF nur dieser Gruppe (jederzeit, aktueller Stand); **PDF / Drucken ▾** in der Werkzeugzeile bietet „Alle Gruppen“ und jede einzelne Gruppe.
- Die **Suche** über der Ansicht zeigt nur die Gruppen mit Treffern und deren Spiele; Treffer sind markiert. Neben dem Feld steht „n Treffer“; **Enter** springt zum nächsten Treffer, **Umschalt+Enter** zum vorherigen („Treffer 2 von 5“, nach dem letzten wieder der erste).
- Die Infozeile am Ende der Werkzeugzeile zeigt z. B. „12 Gruppen · 34 Spiele offen“ bzw. „keine Spiele offen“, bei Bedarf mit „angehalten“, „noch nicht gestartet“ oder „vorzeitig beendet“.
- **Gleichstand: … ▾** stellt ein, was bei gleichem Spielverhältnis zählt (Satz- und Ballverhältnis oder direkter Vergleich).
- **Aufsteiger-Chips (seit 1.3.0):** Solange Phase 2 nicht angelegt ist, sind die Plätze nicht markiert. Danach liegt der Gruppenplatz der Weiterkommenden in einem runden Chip ohne Beschriftung: **dunkelblau** = in Phase 2, Hauptrunde bzw. direkt qualifiziert; **hellblau** = Trostrunde bzw. KO-Qualifikation; **grau** = Nachrücker im Qualifikationsmodus. Wer nicht weiterkommt, hat keinen Chip; bei „Gruppen nach Leistungsstufen“ (alle kommen weiter) gibt es keine Markierung. Ein Tooltip nennt die Bedeutung.

### KO-Baum in HTS

Der KO-Baum zeigt je Runde eine Spalte („Runde der letzten 128“ … „Achtelfinale“, „Viertelfinale“, „Halbfinale“, „Finale“).

- **Karten:** Kopf „Spiel n“ mit Tisch und Status, darunter beide Parteien mit Kennung (Setznummer bzw. ab Phase 2 z. B. „A1“), Name ausgeschrieben, Verein klein dahinter und rechts TTR bzw. nach dem Spiel die Sätze; Doppel zweizeilig ohne TTR. Unter einem abgeschlossenen Spiel stehen die Satzergebnisse. Freilose sind blass dargestellt.
- **Noch offene Plätze** heißen „Sieger/in Spiel n“ (im Spiel um Platz 3 „Verlierer/in Spiel n“).
- **Linker Kartenrand nach Status:** grau = anstehend (geplant, eingereiht), türkis = läuft, gelb = pausiert, schwarz = abgeschlossen.
- **Turnierpfad:** Durchgezogene graue Linien verbinden jedes Spiel mit seinem Folgespiel (nicht beim Spiel um Platz 3); hellgraue, gestrichelte senkrechte Linien trennen die Runden.
- **Rundentitel** bleiben beim Scrollen nach unten oben stehen und wandern beim seitlichen Scrollen mit ihrer Spalte.
- **Infozeile** am Ende der Werkzeugzeile, z. B. „KO-System · 128er-Feld · 95 Begegnungen · 32 Freilose · zufällig · Vereinsgleichheit beachtet · 12 Spiele offen“ (ab Phase 2 „nach Platzierung der Vorphase“).
- **Suche:** Das Suchfeld (Vorname, Nachname, Verein) hebt alle Partien der gefundenen Personen bzw. ihrer Doppel samt Pfad türkis hervor, alle anderen Karten werden abgeblendet. Die Ansicht springt zum ersten Treffer; **Enter** / **Umschalt+Enter** springen zum nächsten / vorherigen Treffer („Treffer 2 von 5“).
- Doppelklick auf eine Karte öffnet die Ergebniseingabe.

## 6. Turnierablauf an den Tischen

Gestartete Klassen teilen sich eine Warteschlange, aus der HTS jeden freien Tisch sofort belegt. Jedes Spiel hat einen der Status geplant, eingereiht, aktiv, pausiert, abgeschlossen oder – nach „Phase vorzeitig beenden“ – nicht gespielt; das Bild zeigt die Übergänge.

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
    eingereiht --> nicht_gespielt: Phase vorzeitig beenden
    aktiv --> nicht_gespielt: Phase vorzeitig beenden
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
- **Kopfzeile:** Tischname, direkt dahinter „alle Klassen“, „nur: …“ bzw. „keine Klasse zugeordnet“ (Klick öffnet „Tische den Klassen zuordnen“; lange Listen brechen um) und rechts der Status: aktiv, pausiert, frei oder deaktiviert.
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

**Tische den Klassen zuordnen** öffnet eine Tabelle Tisch × Klasse. **Haken = diese Klasse darf an diesem Tisch spielen.** Standardmäßig sind alle Haken gesetzt; soll eine Klasse an einem Tisch nicht spielen, wird der Haken bewusst entfernt. Neu angelegte Klassen dürfen automatisch an allen Tischen spielen, Folgephasen folgen ihrer Klasse.

- Ein eingeschränkter Tisch bekommt nur Spiele seiner Klassen. Passt das nächste Spiel nicht, nimmt er das erste passende spätere, z. B. Spiel 3, 7 oder 12 der Warteschlange.
- Ein Tisch **ohne Haken** bekommt keine Spiele, bleibt aber aktiv (Kopfzeile „keine Klasse zugeordnet“).
- „alle“ über einer Spalte setzt bzw. entfernt alle Haken einer Klasse, „alle“ neben einem Tisch die ganze Zeile.

### Phase anhalten und fortsetzen

- **Phase anhalten:** Spiele an den Tischen werden pausiert und bleiben ihrem Tisch zugeordnet; für sie lässt sich kein Ergebnis eintragen. Eingereihte Spiele verlassen die Warteschlange. Oben links im Phasen-Reiter steht dann ein gelber Hinweis „Phase n ist angehalten …“ (bei wenig Platz mehrzeilig).
- Ein pausiertes Spiel wird am Tisch bewusst mit **fortsetzen** wieder gestartet.
- **Phase fortsetzen** setzt alle pausierten Spiele fort und reiht die übrigen an ihrem alten Platz wieder ein.
- In einer angehaltenen KO-Phase lassen sich Plätze und Paarungen der 1. Runde frei per Drag & Drop umsortieren. Nicht mehr gültige Spiele werden gelöscht bzw. zurückgesetzt, samt abhängiger Spiele späterer Runden.
- In einer laufenden KO-Phase geht das Umsortieren nur für Spiele, die noch nicht begonnen haben.
- Eine angehaltene Folgephase (Phase 2) kann **neu ausgelost** werden, wenn an keinem Tisch mehr ein Spiel von ihr läuft. Ihre Ergebnisse entfallen, die neue Auslosung bleibt angehalten.
- Phase vorzeitig beenden und Phase neu starten: Abschnitt 8.

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

Seit 1.0.0: **Die nächsten Spiele (alle Klassen)** mit einstellbarer Anzahl (4–48, Standard 16; mehr als 16 auf mehreren Seiten; seit 1.4.0 als eigene, eingerückte Zeile „Anzahl der Spiele:“ unter dem Häkchen, ausgegraut, solange das Häkchen fehlt) und je laufender Klasse **Warteschlange dieser Klasse** – bei Gruppen nach jeder Seite mit Gruppen die Warteschlange genau dieser Gruppen, sonst nach den Seiten der Phase. Seit 1.1.0 stehen bei Klassen mit Zeitplan die Startzeiten der Runden dabei; seit 1.2.0 zeigt ein Platzierungsplan je Wertung (z. B. Haupt- und Trostrunde) eine eigene Seite.

## 7. Ergebnisse erfassen und korrigieren

Ergebnisse werden Satz für Satz eingetragen („1. Satz“, „2. Satz“ …, Spalte „Punkte“); das Satzverhältnis rechnet HTS selbst. Die erste Zahl gehört immer zur erstgenannten Partei. Gespielt wird auf 3 oder 4 Gewinnsätze – je nach Einstellung der Klasse („Modus festlegen“). Die Einstellung lässt sich ändern, solange in der Klasse noch kein Ergebnis eingetragen ist; Ergebnisdialog und Spielzettel (bis zu 7 Sätze) passen sich an.

### Eingabe

Seit 1.5.0 zeigt der Ergebnisdialog im Tablet-Modus bzw. nach dem ersten Antippen ein **Zahlenfeld** (0–9, „−“, „:“, ⌫, Weiter). Auf der Seite **Tische** öffnet ein Klick bzw. Antippen auf die Partie einer Tischkarte die Ergebniseingabe. **Letztes Ergebnis zurücknehmen** (Tische und Spiele) nimmt das zuletzt eingetragene Ergebnis zurück, solange noch nichts davon abhängt (das Spiel läuft dann wieder, am alten Tisch, wenn er frei ist); sonst gilt „Ergebnis korrigieren“.

| Eingabe | ergibt |
| --- | --- |
| 11:8 (auch 11-8, 11/8, 11 8) | 11:8 |
| 8 (ohne Vorzeichen = „+“) | 11:8 |
| +8 | 11:8 |
| -7 | 7:11 |
| +0 | 11:0 |
| +10 bzw. 10 | 12:10 |
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

**Korrektur-Reiter (seit 1.3.0):** Gibt es nach der korrigierten Phase schon die nächste Phase (und danach keine weitere), rechnet HTS diese Phase mit denselben Einstellungen und demselben Los neu. Kommt dabei etwas anderes heraus – andere Gruppenplätze und damit andere Paarungen oder andere Teilnehmende –, erscheint ohne Dialog der Reiter **„Phase n – Korrektur“** hinter der laufenden Phase; in der Statuszeile steht ein Hinweis. Ohne Auswirkung (z. B. nur andere Bälle, gleiche Plätze) entsteht kein Reiter.

- Der Reiter zeigt die Phase genau wie die laufende, nur korrekt. **Neue Partien** haben einen orangen Rahmen, **neue Spielerinnen und Spieler** (in dieser Phase bzw. in dieser Partie) sind hellorange hinterlegt; eine Legende steht über der Ansicht.
- Die laufende Phase bleibt ausschlaggebend: Aus dem Korrektur-Reiter wird nichts eingereiht, Ergebnisse lassen sich dort nicht eintragen.
- **Korrektur verwerfen:** Nur der Korrektur-Reiter verschwindet. Die Korrektur in der Vorphase bleibt, die laufende Phase läuft unverändert weiter.
- **Unveränderte Ergebnisse übernehmen:** Spiele mit derselben Paarung wie ein schon gespieltes Spiel der laufenden Phase bekommen dessen Ergebnis; im KO rücken die Sieger nach, und daraus entstehende gleiche Paarungen werden ebenfalls übernommen. Der Knopf lässt sich jederzeit erneut drücken (z. B. nachdem in der laufenden Phase weitergespielt wurde).
- **Phase n weiterführen:** Ein Warnhinweis (Abbrechen links, Weiter rechts) nennt die Folgen: Die bisherige Phase wird **deaktiviert** und heißt „Phase n – deaktiviert“; ihre laufenden Spiele werden von den Tischen zurückgerufen, offene Spiele finden nicht mehr statt. Ihre gespielten Spiele bleiben zum Nachsehen, sind auf der Seite „Spiele“ als „deaktiviert“ markiert und kommen weder ins Endergebnis noch in die click-TT-Ergebnisdatei. Die korrigierte Phase wird zur laufenden Phase und sofort eingereiht.
- Es gibt höchstens einen Korrektur-Reiter: Jede weitere Korrektur in der Vorphase rechnet ihn neu (verschwindet die Abweichung, verschwindet auch der Reiter). Deaktivierte Reiter kann es mehrere geben.
- Die Auswirkung betrifft nur die **direkt folgende** Phase: Eine Korrektur in Phase 1 bei laufender Phase 3 ändert nur das Ergebnis; eine Korrektur in Phase 2 kann einen Korrektur-Reiter für Phase 3 erzeugen. Bei Phasen aus Dateien vor 1.3.0 fehlt das gespeicherte Los – dort entsteht kein Korrektur-Reiter.

### Seite „Spiele“

Die Seite listet alle Spiele aller Klassen. Gefiltert wird nach Status (standardmäßig aktiv, pausiert und eingereiht), nach Klasse und nach **Gruppe** (alle Gruppen und KO-Runden der gewählten Klasse; bei „Alle Klassen“ filtert z. B. „Gruppe A“ die Gruppe A aller Klassen). Die Spalte mit Gruppe bzw. Runde heißt „Gruppe“. Die Filterzeile bricht bei wenig Platz um. Das Suchfeld (Vorname, Nachname, Verein) findet die Spiele einer Person bzw. eines Doppels. Die Spalte „Tisch“ zeigt die Nummer bzw. den eigenen Tischnamen. Die Spalte „Export“ steuert die click-TT-Ergebnisdatei (Abschnitt 10). Spiele einer deaktivierten Phase (seit 1.3.0) sind grau hinterlegt, haben den Status „deaktiviert“, in der Spalte „Gruppe“ den Zusatz „(Phase deaktiviert)“ und beim Export „nein (deaktiviert)“; Spiele eines Korrektur-Reiters erscheinen hier erst, wenn die Phase weitergeführt wird.

## 8. Wertung, Phasen und Endergebnis

Eine Klasse hat höchstens drei Phasen; eine Phase 3 gibt es nur nach „Gruppen nach Leistungsstufen“ (seit 1.2.0). Nach jeder Phase entscheidet die Turnierleitung: Endergebnis oder neue Phase. Das Bild zeigt, welche Phase worauf folgen kann.

```mermaid
flowchart LR
    subgraph P1[Phase 1]
        P1KO[KO-System]
        P1G[Gruppenphase]
        P1S[Schweizer System / Badeni]
        P1D[Doppeltes KO]
    end
    subgraph P2[Phase 2]
        P2G[Gruppenphase]
        P2KO[KO-System]
        P2P[Platzierungsspiele*]
        P2Q[Qualifikationsmodus]
        P2HT[Haupt- und Trostrunde]
        P2F[Fortgesetztes KO]
        P2D[Doppeltes KO]
        P2T[TOP24 / TOP32]
        P2L[Gruppen nach Leistungsstufen]
    end
    subgraph P3[Phase 3]
        P3E[Endrunde je Gruppenplatz]
    end
    P1G -- neue Phase --> P2
    P2L -- neue Phase --> P3
    P1 -- Phase abschließen --> E[Endergebnis: Klasse beendet]
    P2 -- Phase abschließen --> E
    P3 -- Phase abschließen --> E
```

*\* nur nach genau 2 Gruppen. Alle Phase-2-Typen nur nach einer Gruppenphase in Phase 1. Nach KO-System, Schweizer System, Badeni oder doppeltem KO in Phase 1 und nach Phase 2 (außer Leistungsstufen) geht es immer zum Endergebnis.*

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
2. **Neue Phase** (nach einer Gruppenphase in Phase 1): Gruppenphase, KO-System, Gruppenphase mit Platzierungsspielen, Qualifikationsmodus, Gruppen nach Leistungsstufen, Haupt- und Trostrunde, Fortgesetztes K.-o.-System (WO 7.3), Doppeltes K.-o.-System (WO 7.4), TOP24- oder TOP32-Platzierungsspiele – dazu die passenden Einstellungen. Nach „Gruppen nach Leistungsstufen“ (Phase 2) gibt es als Phase 3 die „Endrunde je Gruppenplatz“. Die neue Phase startet sofort – nur der Qualifikationsmodus wartet auf „Qualifikation Runde 1 starten“. Ungültige Einstellungen (z. B. ein Gruppenplatz in Haupt- und Trostrunde) markiert HTS mit dem Grund; „Phase abschließen“ ist dann gesperrt.

**Aufsteiger in ein KO-System:** entweder **„Die Besten je Gruppe: [n]“** (wie bisher, z. B. 2 = Erste und Zweite) **oder „Bestimmte Gruppenplätze: [2,3,4]“** (durch Komma getrennt, z. B. für eine Trostrunde). Ungültige Plätze (doppelt, größer als die größte Gruppe, weniger als 2 Aufsteiger) markiert HTS rot am Feld mit dem Grund.

Nach einem KO-System, Schweizer System (auch Badeni) oder doppelten KO in Phase 1 und nach jeder Phase 2 außer „Gruppen nach Leistungsstufen“ gibt es nur das Endergebnis. Ist die Folgephase angelegt, führt der Knopf **Zu Phase 2** (bzw. 3) oben rechts dorthin. Phasen aus den neuen Typen von 1.2.0 lassen sich nicht „neu auslosen“ – stattdessen „Zurück zu Phase n“ und die Phase neu anlegen; vor dem ersten Spiel lassen sich Teilnehmende per Drag & Drop tauschen.

### KO-System ab Phase 2

Ab Phase 2 entscheidet die Gruppenplatzierung, nicht der TTR:

- Rangfolge: erst alle Gruppenersten, dann alle Zweiten usw. Innerhalb einer Platzierung zählen Spiel-, Satz- und Punktverhältnis (Anteil gewonnen an gesamt); bei Gleichheit entscheidet das Los.
- Freilose gehen an die besten Gruppenersten.
- Der Höchstplatzierte ohne Freilos spielt gegen den Niedrigstplatzierten, der noch keinen Gegner hat und nicht aus seiner Gruppe kommt.
- Spieler derselben Gruppe stehen in verschiedenen Hälften und können sich erst im Finale treffen.
- Im KO-Baum steht vor jedem Namen statt der Setznummer die Herkunft: Gruppenbuchstabe und Gruppenplatz, z. B. **A1** (Erster Gruppe A) oder **D3** (Dritter Gruppe D). Im KO-System der Phase 1 bleiben es die Setznummern.
- Ein Spiel um Platz 3 lässt sich wie in Phase 1 zuschalten.
- **Mit bestimmten Gruppenplätzen:** Die Rangliste gilt in der Reihenfolge Gruppenplatz, Spiel-, Satz- und Ballverhältnis, direkter Vergleich (nur bei gleicher Gruppe), Los; daraus ergeben sich die üblichen KO-Positionen. Im Endergebnis stehen zuerst die Gruppenplätze vor dem ersten KO-Platz (z. B. alle Gruppenersten), dann die KO-Phase, dann die übrigen Gruppenplätze; Nicht-Aufsteiger behalten ihren Gruppenplatz.

### Platzierungspläne (seit 1.2.0)

Haupt- und Trostrunde, fortgesetztes und doppeltes KO, TOP24/TOP32 und die Endrunde je Gruppenplatz sind **Platzierungspläne**: feste Folgen von Spielen, bei denen Sieger und Verlierer jeweils in ein bestimmtes nächstes Spiel oder auf einen bestimmten Platz gehen.

- **Darstellung:** Je Abschnitt (z. B. „Hauptrunde“, „Platz 5–8“, „Trostrunde“) die Spiele in Spalten nach Spielrunde. Jede Karte zeigt beide Parteien (mit Herkunft „A1“, „D3“), Tisch, Status und Sätze, darunter, wohin es geht – z. B. „Sieger/in → Spiel 21 · Verlierer/in: Platz 7“. Offene Plätze heißen „Sieger/in Spiel n“ bzw. „Verlierer/in Spiel n“; Freilose und entfallene Spiele (z. B. ein nicht nötiger Stichkampf) sind blass.
- **Auslosung:** aus der Rangliste der gewählten Gruppenplätze wie beim KO-System ab Phase 2 (Freilose an die Besten, Spieler derselben Gruppe möglichst in verschiedenen Hälften). Vor dem ersten Spiel lassen sich zwei Teilnehmende per Drag & Drop tauschen.
- **Spiele** entstehen, sobald beide Gegner feststehen, und werden sofort eingereiht. Korrekturen mit anderem Sieger setzen betroffene Folgespiele nach Rückfrage zurück.
- **PDF der Phase:** je Abschnitt die Spiele mit Paarung, Ergebnis und Weg von Sieger und Verlierer. Die Präsentationsansicht zeigt je Wertung eine Seite.

### Haupt- und Trostrunde

Gruppenplätze für die **Hauptrunde** (z. B. 1,2) und die **Trostrunde** (z. B. 3,4) frei wählbar; beide Runden sind KO-Systeme, jede mit zuschaltbarem Spiel um Platz 3. Es gibt zwei Reiter und zwei **getrennte Endergebnisse**: Die Trostrunde zählt eigens ab Platz 1. Im Reiter „Endergebnis“ wählt eine Auswahl zwischen Hauptrunde, Trostrunde und „Gesamt (Reihenfolge im click-TT-Export)“; das Endergebnis-PDF enthält beide. Wer in keiner der beiden Runden spielt, steht in der Hauptwertung nach Gruppenplatz dahinter. Im **click-TT-Export** zählt die Trostrunde hinter der Hauptrunde weiter – spielen 16 in der Hauptrunde, ist der Sieger der Trostrunde 17., bei 32 der 33.

### Fortgesetztes K.-o.-System (WO 7.3)

KO-System aus den gewählten Gruppenplätzen, in dem auch die Verlierer ihre Plätze ausspielen: Die Halbfinalverlierer spielen um Platz 3, die Verlierer des Viertelfinales in einem eigenen Baum um die Plätze 5–8, die des Achtelfinales um 9–16 usw. – jeder Baum als eigener Abschnitt untereinander im selben Reiter. **Weiterspielen** stellt ein, ab welcher Runde die Verlierer weiterspielen („alle Plätze ausspielen“ oder z. B. „ab Viertelfinale“; Verlierer früherer Runden teilen sich ihren Platz, z. B. 9.–16.).

### TOP24- und TOP32-Platzierungsspiele

Zwei eigene Typen nach dem Muster der DTTB-Ranglistenturniere: TOP24 für 4 Gruppen à 6, TOP32 für 4 Gruppen à 8. Gruppenerste und -zweite spielen über Kreuz (B1–A2, A1–B2, D1–C2, C1–D2) um den Einzug ins Halbfinale um Platz 1–4; die Verlierer und die Sieger der Spiele der Dritten bis Sechsten (bzw. Achten) spielen eine zweite Runde, danach werden in Halbfinals und Platzierungsspielen alle 24 bzw. 32 Plätze ausgespielt. Wählbar nach jeder Gruppenphase in Phase 1: Fehlt ein Gruppenplatz, ist er ein Freilos (die Plätze werden lückenlos durchgezählt); Teilnehmende aus weiteren Gruppen bzw. hinteren Gruppenplätzen folgen im Endergebnis nach Gruppenplatz dahinter.

### Gruppen nach Leistungsstufen und Phase 3

**Phase 2 „Gruppen nach Leistungsstufen“:** Die Gruppenplätze der Vorrunde werden zu Stufen zusammengefasst – **Gruppenplätze je Stufe** = 2 ergibt Stufe 1 aus allen Ersten und Zweiten, Stufe 2 aus den Dritten und Vierten usw. Je Stufe entstehen neue Gruppen mit der gewählten **Gruppengröße**, entweder **gespiegelt** (A mit der letzten Gruppe, B mit der vorletzten …; bei 8 Gruppen also A+H, B+G, C+F, D+E) oder **ausgelost**. Ergebnisse aus Phase 1 werden nicht übernommen; die Gruppennamen laufen weiter (nach A–H heißen die neuen Gruppen I, J, K …). Im Endergebnis steht eine Stufe vollständig vor der nächsten, innerhalb der Stufe nach Gruppenplatz.

**Phase 3 „Endrunde je Gruppenplatz“** (nur nach Leistungsstufen): Je Stufe spielen die Gleichplatzierten der Gruppen ihre Plätze aus – bei vier Gruppen der Stufe Halbfinale (erste gegen vierte, zweite gegen dritte Gruppe), danach Finale und Spiel um Platz 3; bei zwei Gruppen ein Spiel. So bekommt jede Person einen eigenen Platz.

### Phase vorzeitig beenden

**Phase vorzeitig beenden** (Werkzeugzeile, nur in der letzten Phase einer laufenden Klasse) beendet nach einer Rückfrage alle offenen Spiele der Phase – auch die an Tischen: Sie gelten als **„nicht gespielt“** (grau), zählen in keiner Wertung, kommen nicht in den XML-Export, und ihre Tische werden frei. Alle bisher gespielten Spiele bleiben und werden exportiert. Die Klasse ist danach beendet; das Endergebnis ergibt sich aus dem bisherigen Stand (Gruppen nach den gespielten Spielen, im KO teilen sich alle, deren Runde offen blieb, ihre Plätze – „Phase vorzeitig beendet“). **Beenden zurücknehmen** öffnet die Spiele wieder, solange keine weitere Phase angelegt ist.

### Phase neu starten

**Phase neu starten** setzt nach einer Rückfrage (mit Anzahl der Ergebnisse, die verloren gehen; „Abbrechen“ vorausgewählt) alle Ergebnisse der Phase zurück. Auslosung bzw. Übernahme aus der Vorphase bleiben. Spiele an Tischen werden abgebrochen, alle Spiele in der ursprünglichen Reihenfolge neu eingereiht (in einer angehaltenen Phase: geplant). Im KO stehen die Gegner ab Runde 2 wieder leer; in der Qualifikation und im Schweizer System bleibt nur Runde 1. Übernommene Ergebnisse aus der Vorphase bleiben; Aufgaben, kampflose Wertungen und „nicht relevant für Export“ werden zurückgesetzt. Möglich nur, solange es keine weitere Phase gibt – auch nach „vorzeitig beenden“ bzw. in einer beendeten Klasse, die dann wieder läuft.

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
| In die KO-Qualifikation **je Gruppe** (K) | die nächsten K jeder Gruppe spielen die KO-Qualifikation (z. B. Q = 1, K = 2: Zweite und Dritte); K = 0 = keine Spiele |
| In die KO-Qualifikation **insgesamt** | statt je Gruppe die Zahl der Topplatzierten nach den direkt Qualifizierten über alle Gruppen – nach Gruppenplatz, dann Spiel-, Satz-, Punktverhältnis, Gleichstand Los. Beispiel HEM: 16 Gruppen, Gruppenerste direkt, 24 insgesamt → alle Zweiten und die 8 besten Dritten |
| Nachrücker je Gruppe (ohne Spiel) (N) | die nächsten N Gruppenplätze nach den KO-Teilnehmenden werden ohne Spiel Nachrücker (bei „insgesamt“ ab dem ersten Nicht-KO-Teilnehmenden jeder Gruppe) |

**Das Feld muss aufgehen:** Die KO-Qualifikation braucht 3, 4, 7, 8, 11, 12 … Teilnehmende (damit Runde 2 ohne Freilos auskommt), ohne direkt Qualifizierte mindestens 3. Sonst wird das Feld rot hervorgehoben, darunter steht der Grund, und „Phase abschließen“ ist gesperrt.

**Ablauf der KO-Qualifikation:**

- **Runde einzeln starten:** Die Phase wird ausgelost, aber noch nicht eingereiht. Über den Karten und in der Werkzeugzeile steht **„Qualifikation Runde 1 starten“**; ein gelber Hinweis sagt „… ist ausgelost, aber noch nicht gestartet“. Bis zum Start lassen sich alle Spieler der Runde per Drag & Drop tauschen (auch mit dem Freilos, nur innerhalb der Runde). Andere Klassen belegen die Tische währenddessen normal weiter.
- **Runde 1:** besser gegen schlechter Platzierte, möglichst aus einer anderen Gruppe (und bei eingeschalteter Vereinsgleichheit aus einem anderen Verein); bleiben nur gleiche Gruppenplätze übrig, spielen diese gegeneinander. Bei ungerader Zahl hat der Stärkste nach der Gruppenphase (Gruppenplatz, Spiel-, Satz-, Ballverhältnis, TTR) ein **Freilos** – es zählt als Sieg.
- **Nach Runde 1 fragt HTS** (sobald das letzte Ergebnis eingetragen ist), ob die Gewinner jetzt qualifiziert sind:
  - **Gewinner sind qualifiziert:** Es gibt nur eine KO-Runde; die Verlierer sind Nachrücker 1. Ranges, Rangfolge nach TTR.
  - **Weitere KO-Runde für die Gewinner:** Runde 2 wird frei gelost (ohne Freilos) und wartet wieder auf **„Qualifikation Runde 2 starten“** – vorher lässt sie sich ebenso per Drag & Drop umstellen. Wer 2 Spiele gewinnt, ist qualifiziert.
  - Wird der Dialog mit „Später entscheiden“ geschlossen, stehen beide Knöpfe in der Auslosungsansicht; bis zur Entscheidung gilt die Phase als nicht abgeschlossen. „Gewinner sind qualifiziert“ lässt sich bis zum Klassenende zurücknehmen, „Weitere KO-Runde“, solange in Runde 2 noch kein Ergebnis steht (**Entscheidung zurücknehmen**).
- Höchstens 2 KO-Runden. **Zusatzspiel (seit 1.3.0):** Ist die letzte KO-Runde gespielt und gibt es mindestens 2 Verlierer, steht im Phasen-Reiter der Knopf **„Zusatzspiel für die Verlierer der Runde 2“** (nur bei Bedarf; frei gelost, möglichst ohne Wiederholung). **Zusatzspiel zurücknehmen** geht, solange darin kein Ergebnis steht und nichts läuft. Nach dem letzten Ergebnis fragt HTS – wie nach Runde 1 –, ob die Gewinner des Zusatzspiels **qualifiziert** oder **Nachrücker** sind („Später entscheiden“ lässt die Knöpfe im Reiter stehen; bis zur Entscheidung ist die Phase nicht abgeschlossen; änderbar bis zum Klassenende). Qualifiziert: Verlierer des Zusatzspiels = Nachrücker 1. Ranges, Verlierer der Runde 1 = 2. Ranges. Nachrücker: Gewinner des Zusatzspiels = 1., Verlierer des Zusatzspiels = 2., Verlierer der Runde 1 = 3. Ranges – innerhalb eines Ranges nach TTR.
- **Herkunft „A2“:** Überall in der Qualifikation steht vor dem Namen Gruppe und Gruppenplatz aus Phase 1 (z. B. A2, B3) – auf den Karten, in den Listen, in der Präsentationsansicht, in der Live-Anzeige und in den PDFs.

**Ansicht:** Oben die Listen „Qualifiziert durch Gruppenplatzierung“, „Qualifiziert durch KO-Runde“, „Nachrücker 1./2./… Ranges“ und „Platzierte“ – nummeriert, zweispaltig, mit Verein, Herkunft und TTR; darunter die Runden mit ihren Spielkarten (so viele je Reihe, wie in die Breite passen).

**Reihenfolge im Ergebnis:**

1. **Qualifiziert durch Gruppenplatzierung:** alle direkt Qualifizierten teilen sich die ersten Plätze (z. B. 1.–16.), untereinander nach TTR aufgeführt.
2. **Qualifiziert durch KO-Runde:** teilen sich die Folgeplätze (z. B. 17.–22.).
3. **Nachrücker 1., 2., … Ranges**, fortlaufend nummeriert und mit Einzelplätzen: bei zwei Runden zuerst die Verlierer der Runde 2 (Gruppenplatz, Verhältnisse, TTR), dann die Verlierer der Runde 1 (nur nach TTR); bei einer Runde die Verlierer der Runde 1 nach TTR; danach die Nachrücker aus der Gruppenphase (zuerst Gruppenplatz, dann TTR). Ohne KO-Runde sind die Nachrücker aus der Gruppenphase „Nachrücker 1. Ranges“.
4. **Übrige (Platzierte):** Wer weder qualifiziert noch Nachrücker ist, bekommt den Platz seiner Gruppenplatzierung; **gleiche Gruppenplätze teilen sich einen Platz** (z. B. die nicht berücksichtigten Gruppendritten).

**Beispiele:**

| | Vorqualifikation | Hauptqualifikation |
| --- | --- | --- |
| Einstellung | Q = 2, K = 0, N = 2 | z. B. Q = 1, K = 2 je Gruppe (oder 24 insgesamt), N = 0 |
| qualifiziert | Gruppenerste und -zweite | Gruppenerste und die Gewinner der KO-Qualifikation |
| Nachrücker | Gruppendritte vor Gruppenvierten, je nach TTR – ohne Spiel | Verlierer der KO-Qualifikation, später Ausgeschiedene zuerst |
| Rest | ab Platz 5 je Gruppenplatz ein geteilter Platz | übrige je Gruppenplatz ein geteilter Platz |

In der Vorqualifikation hat Phase 2 damit keine Spiele: Nach dem Anlegen steht das Ergebnis sofort fest.

### Endergebnis

Der Reiter „Endergebnis“ listet alle Platzierungen über alle Phasen. Wer in einer früheren Phase ausgeschieden ist, folgt nach Gruppenplatz hinter den Teilnehmenden der späteren Phase.

| Ergebnis aus | Platzierung |
| --- | --- |
| KO-System | Sieger 1., Finale 2., Halbfinale zweimal 3. (mit Spiel um Platz 3: Gewinner 3., Verlierer 4.), Viertelfinale 5.–8., Achtelfinale 9.–16. |
| eine Gruppe | Tabellenplatz |
| mehrere Gruppen | alle Gruppenersten teilen sich Platz 1 bis n, alle Zweiten die nächsten Plätze usw. |
| Platzierungsspiele | Endgruppe 1.–4., dann je Spiel Sieger x., Verlierer x+1. |
| Qualifikationsmodus | direkt Qualifizierte (geteilter Bereich), über die KO-Runde Qualifizierte (geteilter Bereich), dann Nachrücker Rang 1, 2, … (Einzelplätze), dann die übrigen nach Gruppenplatz (gleicher Gruppenplatz = gleicher Platz) |
| Haupt- und Trostrunde | zwei getrennte Wertungen wie im KO-System; für click-TT die Trostrunde hinter der Hauptrunde |
| fortgesetztes KO, TOP24/TOP32, Endrunde je Gruppenplatz | jeder Platz ausgespielt (bzw. geteilter Bereich vor der eingestellten Runde); Freilose werden übersprungen |
| doppeltes KO | Finale bzw. Stichkampf 1./2., Finale der Trostrunde 3./4., dann je Trostrunden-Runde ein Bereich |
| Gruppen nach Leistungsstufen | Stufe für Stufe, in der Stufe nach Gruppenplatz |
| Schweizer System nach Badeni | Tabelle: Siege, TTR, Sätze |
| vorzeitig beendete Phase | Stand bei Abbruch; im KO teilen sich alle mit offener Runde ihre Plätze |

## 9. Drucken und PDF

Alle Ausdrucke entstehen als PDF und öffnen sich im eingebauten PDF-Viewer. Nur der automatische Spielzettel geht ohne Viewer direkt an den Drucker.

| Dokument | Wo | Format | Inhalt |
| --- | --- | --- | --- |
| Spielzettel | automatisch beim Aufruf an einen Tisch; Tische → Drucken | A6 hoch | je nach Zettel-Design (siehe unten): Spielerzettel oder Schiedsrichterzettel |
| Urkunden | Reiter „Urkunden drucken“ → Als PDF speichern / Drucken (seit 1.4.0) | A4 hoch | je Urkunde eine Seite nach der Vorlage des HaTTV (siehe unten) |
| Phase (Gruppen) | Phasen-Reiter → PDF / Drucken ▾ → Alle Gruppen bzw. eine Gruppe; einzelne Gruppe auch über „Drucken“ auf der Gruppenkarte | A4 quer | je Gruppe eine Seite: Tabelle mit Zwischenstand, darunter der Spielplan zweispaltig je Runde („Runde k“) mit Ergebnissen und Sätzen sowie kommenden Spielen |
| Phase (Platzierungsplan) | Phasen-Reiter → PDF / Drucken | A4 quer | je Abschnitt die Spiele mit Paarung (Herkunft „A1“), Ergebnis bzw. Tisch und dem Weg von Sieger und Verlierer (seit 1.2.0) |
| Kassenübersicht, Quittung | Teilnehmende → Startgebühren ▾ | A4 | Einnahmen nach bar, Überweisung, erlassen (mit Grund) und offen; Quittung je Verein (seit 1.0.0) |
| Phase (KO-System) | Phasen-Reiter → PDF / Drucken | A4 quer | Turnierbaum: Kästen „Spiel n“ mit Namen und Verein, Sätze unter dem Kasten, Freilose mit Kasten, Spiel um Platz 3 unter dem Finale, offene Plätze „Sieger/in bzw. Verlierer/in Spiel n“; große Felder auf mehreren Seiten. Danach der Spielplan je Runde als Karten |
| Endergebnis | Reiter Endergebnis → Als PDF anzeigen / drucken | A4 hoch | alle Platzierungen der Klasse; bei Haupt- und Trostrunde beide Wertungen nacheinander |
| Gesamtergebnis | Kopfzeile → Ergebnisse exportieren | A4 hoch | Endergebnis aller Klassen, jede Klasse auf neuer Seite |
| Qualifikation | Phasen-Reiter → PDF / Drucken ▾ | A4 | Listen der Qualifizierten und Nachrücker mit Herkunft („A2 Name (Verein)“), Spielplan der Runden |
| Abgemeldete | Teilnehmende → Abgemeldete als PDF | A4 hoch | endgültig Abgemeldete je Klasse: Name, Vorname, Verein, TTR, abgemeldet durch Handy oder Turnierleitung; sortiert nach Klasse, dann Name |
| QR-Code-Aushang Online-Anmeldung | Live im Internet → Online-Anmeldung → QR-Code-Aushang drucken | A4 | QR-Code zum Anmelde-Link, Turniername, Datum, „Hier anmelden bis … Uhr“, Logo des Ausrichters (seit 1.9.0) |
| Spielzettel 2er/3er Turnier | wie Spielzettel | A4 | Schema der Partien, Felder für die Aufstellung, Teams mit TTR, ggf. Vorgabe-Tabelle |

### Zettel-Design (seit 1.4.0)

Im Reiter **Tische** steht oben rechts **Zettel-Design:**. Die Auswahl gilt für alle Spielzettel (Einzel und Doppel, A6) dieses Turniers und wird in der Turnierdatei gespeichert; ältere Turnierdateien verwenden den Spielerzettel. Der Mannschaftszettel des 2er/3er Turniers (A4) bleibt in beiden Fällen unverändert.

| Design | Inhalt |
| --- | --- |
| Spielerzettel (Standard) | Tisch, Spiel-Nr., Zeit, Konkurrenz, beide Parteien mit Verein (Doppel je Person eine Zeile), Satztabelle „Bälle Spieler/in 1/2“, **Ergebnis (gewonnene Sätze)** in zwei quadratischen Boxen (z. B. 3 : 2) und **Gewinner/in** mit Strich für den Namen. Keine „Wahl“ und keine Schiedsrichter-Unterschrift mehr |
| Schiedsrichterzettel | genau A6; oben rechts das Logo des Ausrichters (seit 1.5.0), unten rechts die HTS-Marke; nach dem DTTB-Schiedsrichterzettel, schwarz-weiß-tauglich: Kopf mit Spiel-Nr., Zeit, Tisch und Konkurrenz; Einzel A gegen X, Doppel A/B gegen X/Y; je Person Auf/Rück und die Karten (gelb, gelb-rot 1 und 2), je Seite Auszeit „T“; Satztabelle mit „Sätze“ und **1. Aufschlag** je Satz (Einzel A X, Doppel A B X Y); Felder Gewinner/in und Schiedsrichter/in; HTS-Schriftzug statt DTTB-Logo |

### Urkunden (seit 1.4.0)

Der Reiter **Urkunden drucken** steht ab Turnierstart in der Kopfleiste (seit 1.5.0). Er zeigt nur Plätze, die feststehen: alle Plätze beendeter Klassen und – in laufenden Klassen – die Plätze derer, die in keiner späteren Phase mehr spielen und deren Phase komplett gespielt ist (Folgephase angelegt), z. B. die in der Gruppenphase Ausgeschiedenen. Solange ein Korrektur-Reiter offen ist, zeigt die Klasse keine festen Plätze.

Sobald eine Phase abgeschlossen ist (Folgephase angelegt oder **Klasse beenden**) und dadurch Plätze feststehen, fragt HTS einmal **Urkundendruck starten?** – links **Abbrechen**, rechts **Zur Druckansicht**. „Zur Druckansicht“ öffnet den Reiter mit den Plätzen 1 bis 3 dieser Klasse (soweit fest); „Abbrechen“ lässt ihn ohne ausgewählte Urkunde. Der Druck beginnt nie von selbst.

- **Aufbau (seit 1.5.0):** Auf breiten Bildschirmen links die Auswahl und die Urkundentexte, in der Mitte die Tabelle (mit Suchfeld für Name, Verein oder Klasse), rechts die Vorschau; auf kleinen Bildschirmen steht die Vorschau unter den Einstellungen. Name und Verein ändert ein Doppelklick (am Tablet ein Antippen) in der Zelle; die Spalte **Gedruckt** zeigt, was schon gedruckt bzw. als PDF gespeichert wurde.
- **Auswahl:** Klasse („Alle Klassen“ oder eine Klasse), dann **Plätze 1 bis 3**, **Alle Plätze**, **Noch nicht gedruckte** (seit 1.5.0) oder **Eigene Auswahl** (Platz von … bis …). In der Liste steht je Urkunde ein Häkchen mit Klasse, Platz, Name und Verein; der Zähler zeigt z. B. „8 von 16 Urkunden ausgewählt“. Ein geteilter Platz (z. B. 5.–8.) erscheint auf der Urkunde als „den 5. Platz“.
- **Vorschau** der ausgewählten Urkunden mit Blättern (‹ ›, „Urkunde 1 von 8“), **Als PDF speichern** (Ordner „PDF Export“ des Turniers) und **Drucken** (Druckdialog).
- **Name und Verein nur für den Druck ändern:** Die Felder in der Liste lassen sich überschreiben (z. B. ausgeschriebener Vereinsname). Geänderte Zeilen tragen die Markierung „nur für Druck geändert“; ein Klick darauf stellt die Originalwerte wieder her. Die Änderung wird in der Turnierdatei gespeichert und wirkt **nur auf die Urkunde** – nicht auf Teilnehmerdaten, Ergebnisse, Präsentationsansicht oder die click-TT-Ergebnisdatei.
- **Urkundentexte** (je Turnier, in der Turnierdatei): Überschrift (Vorgabe „Ehrenurkunde“), Ort („Hamburg“), Unterschrift rechts („Jugendausschuss“), Ausrichter unten („Hamburger Tisch-Tennis-Verband e.V.“). Bleibt ein Feld leer, bleibt die Stelle auf der Urkunde frei; ohne Ort entfällt auch das Komma („den 12. September 2026“). Das Häkchen **Oben 5 cm frei lassen (für vorgedrucktes Briefpapier)** ist voreingestellt; ohne Häkchen rückt der Text nach oben.

| Zeile der Urkunde | Inhalt |
| --- | --- |
| Überschrift | Urkundentext „Überschrift“ (Arial 50 pt) |
| Name | Einzel „Vorname Nachname“, Doppel beide Personen, Mannschaft der Teamname (Comic Neue fett 22 pt) |
| Verein | Verein bzw. beide Vereine (Comic Neue fett 18 pt) |
| Turnier | Turniername, bei Bedarf auf zwei Zeilen umbrochen |
| Klasse | „in der Altersklasse <Klasse>“ |
| Platz | „den <n>. Platz“ (fett 24 pt) |
| Schluss | „Als Anerkennung wird diese Urkunde überreicht“ |
| unten | links „<Ort>, den <Turnierdatum>“, rechts „<Unterschrift rechts>“, darunter „<Ausrichter unten>“ (fett 20 pt) |

Zu lange Namen werden kleiner gesetzt, nie mit „…“ gekürzt. Die Schrift Comic Neue (SIL Open Font License 1.1) ist in HTS eingebettet; ihr Lizenztext liegt im Programmordner unter „Lizenzen“.

### Automatischer Spielzettel

Sobald ein Spiel an einen Tisch geht, druckt HTS den Spielzettel ohne Rückfrage auf dem Windows-Standarddrucker. Kann der Drucker kein A6, wird in A6-Größe auf das Standardpapier gedruckt. Auf der Seite „Tische“ lässt sich das mit dem Häkchen „Spielzettel automatisch drucken“ abschalten; die Einstellung gilt je Rechner. Wird ein Spiel mit „an Tisch“ geholt, druckt HTS ebenfalls; bei „wechseln“ nicht. In Testturnieren werden beim Simulieren keine Spielzettel gedruckt.

### PDF-Viewer

- Zoom mit − und +
- **Speichern** als PDF-Datei (Strg+S) – standardmäßig in den Ordner „PDF Export“ des Turniers; ein anderer gewählter Ordner gilt danach für dieses Turnier
- **Drucken** über den Windows-Druckdialog (Strg+P)
- Esc schließt das Fenster

Im Phasen-PDF bricht ein langer Spielplan auf Fortsetzungsseiten um. Die Spalte „Sätze“ zeigt 5 Sätze auch mit Verlängerung vollständig. Das Spiel um Platz 3 erscheint als eigener Abschnitt mit Runde „Pl. 3/4“. Namen mit Zeichen, die die PDF-Schrift nicht kennt (z. B. Ł), werden in ihrer Grundform gedruckt (L).

## 10. Ergebnisse exportieren (click-TT)

Seit 1.0.0 stehen auf der Seite „Klassen“ oben rechts zwei Knöpfe: **Gesamtergebnis als PDF** und **click-TT-Ergebnisdatei (XML)** – immer sichtbar, aktiv, sobald alle ausgelosten Klassen beendet sind.

- **Von Hand Nachgetragene:** Wurden Teilnehmende von Hand erfasst, bietet HTS an, eine aktualisierte Teilnehmerliste aus click-TT einzuspielen: eindeutige Personen (Vor-/Nachname, Verein) werden automatisch zugeordnet, die übrigen im Zuordnungsfenster von Hand. Beim erneuten Einlesen werden sie nicht doppelt angelegt, sondern übernehmen die click-TT-Nummer.
- **Bericht:** Nach dem Export listet HTS alle nicht exportierten Spiele mit Grund (auch als PDF), z. B. „Doppel so nicht in click-TT gemeldet“ für am Turniertag zugeloste Doppel.
- **Haupt- und Trostrunde:** Die Platzierungen der Trostrunde zählen hinter der Hauptrunde weiter (Abschnitt 8).

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
| „nicht gespielt“ nach „Phase vorzeitig beenden“ | nein |
| Haken „Export“ auf der Seite „Spiele“ entfernt (nicht relevant für Export) | nein |
| Spiel, in dem aufgegeben wurde | ja, 0:3 mit Sätzen 0:11 |
| nach einer Aufgabe kampflos gewertetes Spiel | nein (Spalte zeigt „nein (kampflos)“) |
| in die Endgruppe übernommenes Spiel | nur einmal, als Original aus Phase 1 (Kopie zeigt „übernommen“) |
| Klasse noch nicht beendet | nein, die Konkurrenz wird im Bericht genannt |

Die Export-Markierung gibt es nur auf der Seite „Spiele“ und nur für abgeschlossene Spiele. Vorgabeturniere haben keinen click-TT-Export. Nach dem Export zeigt HTS einen Bericht: Anzahl Konkurrenzen, Spiele und Platzierungen sowie, was nicht exportiert wurde.

## 11. Live im Internet (seit 1.5.0)

Ergebnisse lassen sich im Internet einsehen. Vom Server fließen nur die Meldungen der Online-Anmeldung zurück (seit 1.9.0, Abschnitt 12). Den früheren Handy-Zugang im Hallen-WLAN gibt es seit 1.9.0 nicht mehr.

1. Turnierübersicht → **Webserver**: Adresse und geheimer Schlüssel eintragen, **Verbindung testen**.
2. Im Turnier → **Live im Internet**: **Dieses Turnier veröffentlichen** (seit 1.8.0 bei neuen, kopierten und importierten Turnieren schon eingeschaltet, bei Testturnieren nicht). HTS lädt nach jeder Änderung den Stand hoch (höchstens alle 5 Sekunden). Unten rechts steht „Live: veröffentlicht vor 5 s“; bei Funkloch „keine Verbindung – wird nachgeholt“ – der letzte Stand wird alle 10 Sekunden erneut gesendet.
3. **Adresse für Zuschauer** kopieren (Knopf **Adresse kopieren**), z. B. für die Vereinsseite. Seit 1.7.0 ist sie lesbar: `https://<Server>/live/<Turniername>_<TT>_<MM>_<JJJJ>` (Umlaute ausgeschrieben, Sonderzeichen als „_“).
4. **QR-Code-Aushang drucken** (seit 1.7.0): A4-PDF mit großem QR-Code zur Live-Seite, Turniername, Datum, Adresse und Logo des Ausrichters – zum Aushängen in der Halle. Beim **Turnier starten** fragt HTS danach, wenn „Live im Internet“ eingeschaltet ist.

5. **Turnierwelt-Link** (seit 1.8.0): Unter **Turnier bearbeiten** (bzw. beim Anlegen) lässt sich die Adresse der Turnierseite auf mytischtennis.de eintragen (nur https). Sie erscheint auf den Live-Seiten als **zur Turnierwelt ↗** und auf der Seite „Live im Internet“ als Link.

Datenschutz: Vornamen erscheinen auf dem Webserver **immer abgekürzt** („M. Mustermann“, nur in der hochgeladenen Kopie; seit 1.8.0 ohne Schalter). **Live-Daten löschen** (Turnier vom Server entfernen) und **Nur Endergebnis behalten** (alles außer den Endergebnissen entfernen) schalten das Veröffentlichen aus. Wird ein veröffentlichtes Turnier in der Turnierübersicht **gelöscht**, fragt HTS seit 1.8.0 **„Live-Daten auch vom Webserver löschen?“** – bei Ja verschwindet es sofort von der Live-Seite; ist der Server nicht erreichbar, holt HTS das beim nächsten Start bzw. beim nächsten Veröffentlichen nach. Den Namen des Ausrichters (Reiter „Ausrichter“) schickt HTS beim Hochladen mit.

### Der Live-Server (seit 1.6.0)

Den passenden Server liefert HTS mit: das Paket **hts-server-1.6.0.tar.gz** für einen gemieteten Linux-Server (Ubuntu 24.04, z. B. netcup VPS; 1–2 Kerne und 1–2 GB RAM genügen) und eine eigene Domain (z. B. hts-livescoring.de). Die vollständige Schritt-für-Schritt-Anleitung steht in **docs/LIVE-SERVER-ANLEITUNG.md**, in Kürze:

1. Domain per A-/AAAA-Eintrag auf den Server zeigen lassen.
2. Paket auf den Server kopieren (`scp hts-server-1.6.0.tar.gz root@hts-livescoring.de:`), anmelden (`ssh root@hts-livescoring.de`), entpacken und **`./install.sh hts-livescoring.de`** starten.
3. Das Skript richtet alles ein – Java 21, Caddy mit automatischem HTTPS-Zertifikat (http und www leiten auf https://hts-livescoring.de um), Firewall (nur SSH, 80, 443), automatische Sicherheitsupdates (Neustart bei Bedarf um 4:30 Uhr), den Dienst **hts-server** mit Autostart – und zeigt am Ende **einmalig den geheimen Schlüssel**. Diesen sicher notieren und in HTS unter **Webserver** eintragen. Neuer Schlüssel: `./install.sh --neuer-schluessel`.
4. **Impressum** und **Datenschutzerklärung** ausfüllen (`/etc/hts-server/impressum.html` und `datenschutz.html`; Vorlagen sind auf HTS zugeschnitten), AV-Vertrag mit dem Anbieter abschließen, in der Ausschreibung auf die Veröffentlichung hinweisen. Seit 1.8.0 geht es auch so: fertige Fassungen als Datei **hts-rechtstexte.tar.gz** (mit `impressum.html` und/oder `datenschutz.html`) neben das Paket legen – `install.sh` und `update.sh` prüfen sie (keine Platzhalter mehr), sichern die vorherigen Dateien als `.bak-<Datum>` und spielen sie ein. Der Text der Seite **Spenden** steht in `/etc/hts-server/spenden.html`.

Was Zuschauer sehen:

| Adresse | Inhalt |
| --- | --- |
| https://hts-livescoring.de | Reiter **Aktuelle Turniere** (laufende zuerst, dann nach Datum) – seit 1.8.0; je Karte Datum, Stand, Ausrichter mit Logo und ggf. „zur Turnierwelt“ |
| …/vergangen | Reiter **Vergangene Turniere** (beendet, nur noch Endergebnis oder Datum vor heute; neueste zuerst) |
| …/live/<Turniername>_<TT>_<MM>_<JJJJ> | ein Turnier: Tische, Klassen mit Reitern je Phase, Gruppenkacheln, KO-Runden und Ergebnissen, Spielersuche, „Meine Spiele“; Logo des Ausrichters (seit 1.7.0 lesbare Adresse; die alte Adresse mit Kennung leitet um) |
| …/live/<Turnier>/<Klasse>/spiel45 | seit 1.8.0 eine Seite je Spiel (Klassenname mit „_“ statt Leerzeichen; ab Phase 2 `…/<Klasse>/phase2/spiel3`): Spieler mit Verein, Tisch, Status, Ergebnis und Sätze, **Link kopieren** |
| …/impressum, …/datenschutz, …/spenden | Impressum, Datenschutzerklärung und Seite „Spenden“ (auf jeder Seite verlinkt) |

**Aufbau der Live-Seiten (seit 1.7.0):** oben Logo, Turniername, Datum und Stand, darunter das **Suchfeld** (Vor-/Nachname oder Verein über alle Klassen; Treffer mit Klassen und dem Link **Meine Spiele**, den man seit 1.8.0 als Lesezeichen speichert) und die Leiste **Tische** + je Klasse ein Knopf. Eine Klasse hat je Phase einen Reiter („Phase 1 · Gruppen“, „Phase 2 · KO-Runde“ …; die laufende ist vorgewählt) und den Reiter **Ergebnis** (Zwischenstand bzw. Endergebnis). Gruppen erscheinen als **Kacheln** mit Status (läuft, beendet, wartet), Fortschritt („5 von 6 Spielen“) und aktuellem Ersten – **antippen** zeigt die Tabelle, **Spiele anzeigen** die Spiele der Gruppe. KO-Phasen stehen als Liste je Runde. Laufende Spiele heißen schlicht **„laufend“** (ohne Punktestand), beendete zeigen Ergebnis und Sätze. Jedes Spiel führt beim Antippen auf seine eigene Seite. Spielnummern zählen je Klasse und Phase und bleiben fest: Das Spiel um Platz 3 bekommt seit 1.8.0 die Nummer nach dem Finale, wird aber weiterhin vor dem Finale aufgerufen. Seit 1.9.0: oben auf jeder Turnierseite der Knopf **← Alle Turniere** zur Startseite; KO-Phasen erscheinen als **KO-Baum** (Spalten je Runde, waagerecht wischbar, Spiel um Platz 3 unter dem Finale, Umschalter **Baum | Liste**); in Gruppenphasen mit Freistellung steht über den Gruppen die Karte **Freigestellt** (S1–S4, direkt in der KO-Runde) – auch auf „Meine Spiele“ und in der Suche.

Die Seiten aktualisieren sich alle 10 Sekunden ohne Neuladen – aufgeklappte Kacheln, der gewählte Reiter und der Suchtext bleiben. Nach **Nur Endergebnis behalten** zeigt die Turnierseite nur noch die Endergebnisse; **Live-Daten löschen** entfernt das Turnier sofort. Spätestens 90 Tage nach der letzten Änderung löscht der Server ein Turnier von selbst. Keine Cookies, keine Zugriffsprotokolle, keine Inhalte von Drittanbietern und seit 1.8.0 auch nichts im Browser gespeichert (kein Local oder Session Storage); Suchmaschinen werden von den Turnierseiten ferngehalten. Neue Versionen spielt **`./update.sh`** aus dem Ordner des neuen Pakets ein (bei Problemen automatisch zurück zur vorigen Version). Die Schnittstelle beschreibt docs/WEBSERVER-PROTOKOLL.md.

## 12. Online-Anmeldung am Turniertag (seit 1.9.0)

Am Turniertag melden sich Teilnehmende über einen QR-Code selbst an oder ab; Betreuende können ihren ganzen Verein anmelden. Das läuft über den Live-Server (Abschnitt 11) – das Turnier muss veröffentlicht werden.

1. **Uhrzeit festlegen:** beim Anlegen bzw. unter **Bearbeiten** das Feld **Online-Anmeldung bis** (z. B. 10:30) oder auf der Seite **Live im Internet** im Bereich „Online-Anmeldung zum Turnier“.
2. **Einschalten:** Schalter **Online-Anmeldung einschalten** (Vorgabe aus). Darunter stehen der Anmelde-Link, **Link kopieren**, **QR-Code-Aushang drucken** (A4 mit Turniername, Datum, „Hier anmelden bis 10:30 Uhr“ und Logo des Ausrichters) und **Neuen Anmelde-Link erzeugen** (der alte Link und gedruckte QR-Codes werden sofort ungültig).
3. **Am Turniertag:** Spieler/innen scannen den Code, suchen ihren Namen und tippen **Ich bin da – anmelden** bzw. **Ich komme nicht – abmelden**. Betreuer/innen wählen ihren Verein, haken die Personen ab und melden sie gemeinsam an oder ab.
4. **In HTS:** Die Meldungen kommen nach wenigen Sekunden an (HTS holt sie mit dem geheimen Schlüssel ab; danach löscht der Server sie). In der Teilnehmerliste steht neben dem Haken „online“; **Aktion für Markierte → Online-Meldung zurücknehmen** stellt den Stand davor wieder her. Jede Änderung von Hand ersetzt die Online-Meldung.

Regeln: Online offen ist die Anmeldung nur, wenn der Schalter an ist, heute der Turniertag ist und die Uhrzeit noch nicht erreicht ist (sowie nicht alle Klassen beendet sind). Ausschalten schließt sie sofort. Eine **Online-Abmeldung** heißt „nicht angemeldet“ – endgültig abmelden bleibt bei der Turnierleitung. Endgültig Abgemeldete, Nachrücker und Personen, deren Klassen schon laufen, können sich online nicht mehr an- oder abmelden. Nach der Uhrzeit bleibt die Anwesenheit in HTS wie bisher änderbar, bis Phase 1 der Klasse gestartet ist. Der Server speichert keine IP-Adressen und setzt keine Cookies; die Datenschutzerklärung hat dafür einen eigenen Abschnitt.

> **Entfallen (1.9.0):** Der Handy-Zugang über das Hallen-WLAN (eigener Server auf dem Laptop, Port 8080, Seite „Mobile Einstellungen“, Anmeldung und Ergebnisse per Handy, WLAN-QR-Codes). Turnierdateien mit diesen Einstellungen lassen sich weiter öffnen.

## 13. Vorgabeturnier sowie 2er und 3er Turnier

### Vorgabeturnier

Option beim Anlegen eines Turniers (später nicht umschaltbar). TTR-Unterschiede werden durch einen **Punktevorsprung** ausgeglichen: Die Partei mit Vorsprung beginnt jeden Satz mit X:0.

- Eigener Menüpunkt **Vorgaberegeln**; bis die Art der Vorgabe gespeichert ist, gibt es nur „Teilnehmende“ und „Vorgaberegeln“. Speichern nur per Knopf mit Rückfrage; nach dem Start der ersten Klasse nur noch lesbar.
- Art der Vorgabe: **TTR-Klassen** (2–7 Klassen, Bälle je Klassenunterschied) oder **Fester TTR-Abstand** (2–10 Kategorien, Bälle je Kategorie); Grenzen getrennt für Erwachsene und Damen, lückenlos, alle Werte frei pflegbar. Aufschlag gelost oder für die Partei ohne Vorsprung; Halbierung im Doppel einstellbar.
- Doppel: TTR-Höhere und TTR-Tiefere werden verglichen (Mixed: Herr–Herr, Dame–Dame), die Vorgaben verrechnet.
- Anzeige „+X“ in Gruppen-, KO-, Tisch-, Spiele- und Präsentationsansicht, im Ergebnisdialog und in den PDFs; Spielzettel mit „+ X Punkte“.
- Start einer Klasse gesperrt bei fehlendem TTR oder TTR bzw. Abstand über der höchsten Grenze (mit Hinweis).
- Klassentypen nur „Erwachsene“ und „Damen“; Verein und Jahrgang keine Pflicht; kein click-TT-Export.
- **CSV-Import:** Vorname, Nachname, TTR-Wert, Klasse (Erwachsene/Damen), Komma-getrennt, UTF-8, ab Zeile 2; ersetzt alle Teilnehmenden (nur vor der ersten Auslosung, mit Rückfrage). Vorlage: „CSV-Vorlage speichern“.

### 2er und 3er Turnier (nur im Vorgabeturnier)

Mannschaftskämpfe: Teams werden wie Doppelpaarungen gebildet (Drag & Drop, „Teams zulosen“), der Teamname ergibt sich aus den Nachnamen; Option „mindestens 1 Dame je Team“. Eine Begegnung Team gegen Team ist ein Spiel an einem Tisch; es werden immer alle Partien gespielt.

| Turnier | Partien |
| --- | --- |
| 3er (mit Dame) | Mixed (H2/D), Herren-Einzel (H1), Damen-Einzel (D), Herren-Doppel (H1/H2), Mixed (H1/D) |
| 3er (ohne Dame) | Doppel 1 (T2/T3), Einzel 1 (T1), Einzel 2 (T3), Doppel 2 (T1/T2), Doppel 3 (T1/T3) |
| 2er, Gruppenphase | A1–B1, A2–B2, Doppel |
| 2er, KO-System | zusätzlich A1–B2, A2–B1 (5 Partien); mit Dame ist sie automatisch Teilnehmende/r 2 |

Ergebnis: Aufstellung beider Teams wählen und je Partie den Sieger eintragen (mit Vorgabe je Partie). Gruppenwertung nach Begegnungen, Partien, festem Los. Spielzettel A4 je Begegnung.

## 14. Dateien, Technik und Grenzen

### Dateien

| Datei | Ort | Inhalt |
| --- | --- | --- |
| `<Turniername>.hts` | seit 1.8.0 direkt in `Dokumente\HTS` (bzw. im Reiter „Programm“ gewählt; bei gleichem Namen „(2)“); Turniere älterer Versionen bleiben in `Dokumente\HTS Turniere\<Turniername>\Turnierdatei` (oder per „Durchsuchen“ gewählt) | ein Turnier: Teilnehmende (mit Anwesenheit und Meldungen), Klassen, Paarungen, Auslosungen (samt festem Los), Spiele, Tische (Anzahl, Namen, deaktivierte Tische), Tischzuordnung (ausgeschlossene Klassen je Tisch), Einstellungen der Klassen, der Präsentationsansicht und der Vorgabe, Schlüssel der Handy-Anmeldung, Testturnier-Kennzeichen und Speicherorte (XML) |
| `<Turniername>.clicktt.xml` | neben der Turnierdatei | Kopie der importierten click-TT-Datei, Grundlage des Exports |
| `Ergebnisse - <Name> - TT_MM_JJJJ.xml` | `Dokumente\HTS\Export files` (seit 1.8.0), ältere Turniere `…\<Turniername>\XML Export` | click-TT-Ergebnisdatei |
| PDF-Dateien | `Dokumente\HTS\Export files` (seit 1.8.0; Name beginnt mit Turniername und Datum, z. B. „Herbstturnier 2026-11-28 - Endergebnis Herren C.pdf“), ältere Turniere `…\<Turniername>\PDF Export` | aus dem PDF-Viewer gespeichert |
| `ausrichter.properties`, `ausrichter-logo.png`, `programm.properties`, Ordner `Protokoll` | Programmordner (dort, wo „HTS starten“ liegt; seit 1.8.0); ist er nicht beschreibbar, `<Benutzerordner>\.hts` | Ausrichter, Programmeinstellungen (Dunkelmodus, Speicherorte), Protokoll und Absturzberichte |

Neuere Versionen öffnen ältere Turnierdateien (Zusage seit 0.42.0; jede Datei vermerkt die HTS-Version, mit der sie gespeichert wurde, ein automatischer Test öffnet Beispieldateien früherer Versionen). Dateien aus einer neueren Version lehnt eine ältere HTS-Version mit Hinweis ab. Ab 0.38.0 repariert HTS Turnierdateien, in denen durch einen früheren Fehler leere Kopien von Klassen entstanden waren (viele Klassen gleichen Namens nach dem Zuordnen von Tischen), automatisch beim Öffnen; alte Tischzuordnungen werden auf „Haken = darf spielen“ umgerechnet.

### Technik

- Java 21 und JavaFX 21.0.8, ausgeliefert mit eigener Java-Laufzeit (Eclipse Temurin 21.0.13) – je ein Paket für Windows (x64), Linux (x86_64) und macOS (Apple M)
- PDF: Apache PDFBox 3.0.8 (Apache-Lizenz 2.0, Hinweise im Ordner `Lizenzen`)
- Live-Server im Internet: eigenes Paket hts-server (Java 21), Anleitung in docs/LIVE-SERVER-ANLEITUNG.md
- Abhängigkeiten und Sicherheitslücken werden alle 6 Monate geprüft (Datei `docs/ABHAENGIGKEITEN.md` im Quellcode)
- Lizenz: GNU Affero General Public License v3.0 oder später (Datei `LICENSE.txt`, Lizenzhinweis in `NOTICE.txt`); die Handy-Seiten verlinken den Quellcode (AGPL § 13); die Lizenzen von JavaFX und der Java-Laufzeit liegen im Ordner `Lizenzen`
- Quellcode und Issues: GitHub-Projekt „Hamburger-Turniersoftware---HTS“
- Quellcode: Paket `de.hts` mit den Bereichen Teilnehmende, Turnier und Auslosung, Speichern, click-TT-Import und -Export, PDF und Oberfläche; dazu automatische Tests

### Bekannte Grenzen

- Eine Aufgabe wirkt nur in der Phase, in der sie passiert; für eine spätere Phase muss die Partei von Hand berücksichtigt werden.
- Gruppen lassen sich nur vor dem Start der Phase per Drag & Drop ändern.
- „Fertiges Turnier importieren“ übernimmt nur Phase 1 je Konkurrenz.
- Ein Tisch lässt sich nur deaktivieren, solange kein Spiel an ihm liegt.

### Glossar

| Begriff | Bedeutung |
| --- | --- |
| Klasse | eine Konkurrenz, z. B. „Jungen U19 Einzel“ |
| Phase | ein Abschnitt einer Klasse (Gruppen, KO-System, Schweizer System, Platzierungsspiele, Qualifikationsmodus, Platzierungsplan); höchstens 3 – Phase 3 nur nach Leistungsstufen |
| Platzierungsplan | feste Folge von Spielen, in der Sieger und Verlierer jeweils in ein bestimmtes Spiel oder auf einen Platz gehen: Haupt- und Trostrunde, fortgesetztes/doppeltes KO, TOP24/TOP32, Endrunde je Gruppenplatz |
| Stichkampf | im doppelten KO: zweites Finale, wenn die Siegerin/der Sieger der Trostrunde das Finale gewinnt |
| Siegegruppe | im Schweizer System nach Badeni: alle mit gleich vielen Siegen; gepaart Erster gegen Letzten |
| Leistungsstufe | Phase 2: Gruppen aus Gleichplatzierten der Vorrunde (z. B. alle Ersten und Zweiten) |
| Qualifikationsmodus | Phase 2 nach Gruppen: Beste je Gruppe direkt qualifiziert, Folgende spielen eine KO-Qualifikation mit 1 oder 2 Runden (Entscheidung nach Runde 1), weitere Gruppenplätze werden ohne Spiel Nachrücker |
| Nachrücker Rang x | Stufe im Qualifikationsmodus: rückt nach, wenn Qualifizierte absagen |
| Nachrücker (Warteliste) | in click-TT auf der Warteliste gemeldet; zählt nicht zu den Angemeldeten |
| Meldung | in click-TT für eine Klasse gemeldet; kommt erst nach der Anmeldung am Turniertag in die Klasse |
| Angemeldet | am Turniertag anwesend (Haken oder Online-Anmeldung) |
| Startsperre | importierte Klasse startet erst, wenn alle Gemeldeten an- oder abgemeldet sind |
| Setzplatz | Rangfolge nach TTR in einer importierten Klasse |
| Online-Anmeldung | An- und Abmeldung am Turniertag per QR-Code über den Live-Server (seit 1.9.0) |
| Vorgabeturnier | Turnier mit Punktevorsprung nach TTR-Unterschied |
| nicht gespielt | Status offener Spiele nach „Phase vorzeitig beenden“; zählt nicht, kein Export |
| Testturnier | Übungsturnier, in dem alle offenen Spiele per Würfel-Knopf simuliert werden können |
| Tischname | eigener Name eines Tisches (bis 30 Zeichen) statt „Tisch n“ |
| Setzung | Reihenfolge nach TTR bzw. Doppel-TTR (Phase 1) oder nach Platzierung (ab Phase 2) |
| Gruppenkopf gesetzt | Gruppen-Setzung: TTR-Nr. 1 führt Gruppe A, Nr. 2 Gruppe B usw. |
| Festes Los | letztes Kriterium bei völliger Gleichheit in Gruppentabellen und im Schweizer System; einmal bestimmt und gespeichert |
| Präsentationsansicht | Anzeige auf einem zweiten Bildschirm mit wechselnden Ansichten |
| Spiel um Platz 3 | zuschaltbares Spiel der beiden Halbfinalverlierer im KO-System; Gewinner 3., Verlierer 4. |
| A1, D3 … | Herkunft im KO-Baum und in der Qualifikation ab Phase 2: Gruppenbuchstabe + Gruppenplatz aus Phase 1 |
| Freilos | Platz ohne Gegner; die Partei kommt kampflos weiter (KO) bzw. erhält einen Sieg (Schweizer System) |
| Spielverhältnis | gewonnene zu verlorenen Spielen (S:N) |
| Buchholz (BHZ) | Summe der Punkte aller Gegner im Schweizer System |
| Feinbuchholz (FBHZ) | Summe der Buchholzzahlen aller Gegner |
| Vereinsgleichheit | zwei Parteien aus demselben Verein in einer Gruppe bzw. in einem Erstrundenspiel |
| Warteschlange | eingereihte Spiele, die auf einen freien Tisch warten |
| Aufgabe | eine Partei gibt während eines Spiels auf; ihre offenen Spiele der Phase gelten als verloren |
