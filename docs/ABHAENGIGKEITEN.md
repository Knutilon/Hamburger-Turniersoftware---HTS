# HTS – Abhängigkeiten und Sicherheitsprüfung (CVE)

<!--
  MASCHINENLESBAR – nicht umformatieren:
  LETZTE_PRUEFUNG: 2026-11-04
  PRUEFINTERVALL_MONATE: 6
  NAECHSTE_PRUEFUNG_FAELLIG_AB: 2027-05-04
-->

**Letzte Prüfung:** 04.11.2026 · **Nächste Prüfung fällig ab:** 04.05.2027 (Intervall 6 Monate)

Diese Datei hält fest, welche Fremdkomponenten HTS ausliefert, in welcher Version, und wann zuletzt auf neue
Versionen und bekannte Sicherheitslücken (CVE) geprüft wurde. Geprüft wird **nur**, wenn die letzte Prüfung älter
als 6 Monate ist (Regel und Ablauf: `CLAUDE.md` im Projektwurzelverzeichnis).

## Ausgelieferte Komponenten (Windows-ZIP)

| Komponente | Ausgeliefert | Woher im Build | Lizenz | Neueste Version bei Prüfung | Bekannte CVEs / Bewertung für HTS |
| --- | --- | --- | --- | --- | --- |
| Java-Laufzeit Eclipse Temurin 21 (JRE, Windows x64) | 21.0.13+9 (20.10.2026) – seit HTS 0.31.0 (vorher 21.0.8+9) | Ordner `jre` im Windows-ZIP (`jre/release`) | GPLv2 + Classpath Exception | 21.0.13+9 (30.10.2026) | Seit 21.0.8 sind 5 vierteljährliche Sicherheitsupdates erschienen (Oracle Critical Patch Updates); ein Scanner meldet hier Treffer. Die meisten betreffen Netzwerk, TLS/Krypto oder Sandboxing, die HTS kaum nutzt (kein Server, keine Netzwerkverbindung). Mit 0.31.0 aktualisiert (SHA-256 der Adoptium-Datei geprüft). |
| JavaFX (OpenJFX) 21, Windows | 21.0.8 (`javafx.runtime.version=21.0.8+2`) – im Fat-Jar `hts.jar`, seit HTS 0.32.0 (vorher 21 GA) | Windows-Jars `javafx-base/-graphics/-controls-21.0.8-win.jar` (vom Nutzer von Maven Central geladen); `pom.xml` 21.0.8 | GPLv2 + Classpath Exception | 21.0.13 (20.10.2026); öffentlich auf Maven Central mindestens bis 21.0.8, neuere 21.0.x-Updates verteilt Gluon teils nur an LTS-Abonnenten | Die Updates seit 21 GA enthalten Sicherheitskorrekturen (u. a. WebView/Medien, die HTS nicht nutzt). Mit 0.32.0 auf 21.0.8 aktualisiert; neuere 21.0.x bei Bedarf prüfen. |
| Apache PDFBox (pdfbox, pdfbox-io, fontbox) | 3.0.8 – seit HTS 0.32.0 (vorher 3.0.5) | Fat-Jar `hts.jar`; `pom.xml` (Jars vom Nutzer von Maven Central geladen) | Apache 2.0 | 3.0.8 (11.07.2026) | Keine bekannte CVE für den Kern von 3.0.5 (Snyk). CVE-2026-33929 betrifft nur das Beispiel „ExtractEmbeddedFiles“ (nicht in HTS). CVE-2026-23907 (Pfad-Traversal beim Auslesen eingebetteter Dateien über `PDComplexFileSpecification.getFilename()`) betrifft das Lesen/Extrahieren von Anhängen – HTS erzeugt PDFs nur und liest keine fremden PDFs. Mit 0.32.0 auf 3.0.8 aktualisiert. |
| Apache Commons Logging | 1.3.5 | Fat-Jar `hts.jar` (von PDFBox benötigt) | Apache 2.0 | 1.3.5 aktuell | Keine bekannte CVE. |

## Nur zum Bauen (nicht ausgeliefert)

| Komponente | Version | Bewertung |
| --- | --- | --- |
| JUnit Jupiter | 5.10.2 | nur Tests, nicht ausgeliefert – unkritisch |
| maven-surefire-plugin / javafx-maven-plugin / maven-shade-plugin | 3.2.5 / 0.0.8 / 3.5.3 | nur Build – unkritisch |

## Eigener Code – geprüfte Angriffsflächen

- HTS ist ein Desktop-Programm ohne Netzwerkzugriff ins Internet. Seit 0.35.0 gibt es einen **optionalen
  Handy-Zugang**: Nur nach Klick auf „Handy-Zugang starten“ öffnet HTS einen Webserver (Java-eigener
  `com.sun.net.httpserver`, keine zusätzliche Bibliothek) auf Port 8080 im lokalen Netz; beim Verlassen des Turniers
  wird er beendet.
  - `/live`: nur lesend (Tische, Klassen, Ergebnisse, „Meine Spiele“); andere Methoden als GET werden abgewiesen.
  - `/anmeldung/<Schlüssel>`: setzt nur die Anwesenheit (anwesend bzw. „Abmeldung“, die die Turnierleitung bestätigen
    muss); der zufällige 8-stellige Schlüssel steht nur im ausgedruckten QR-Code; die Turnierleitung kann die
    Anmeldung sperren; endgültig Abgemeldete ändert nur die Turnierleitung.
  - Alle Ausgaben werden HTML- bzw. JSON-maskiert; Anfragen werden auf dem Oberflächen-Thread ausgeführt; der
    Anfragekörper ist auf 64 KB begrenzt. Keine Anmeldung/kein Passwort: Wer im WLAN ist und den QR-Code hat, kann
    Anwesenheiten melden – deshalb ein eigenes Turnier-WLAN (z. B. Router ohne Internet) nutzen.
- Eingaben von außen sind sonst nur Dateien: click-TT-XML (Import), `.hts`-Turnierdateien, Ergebnis-XML.
- Alle XML-Parser sind gegen XXE gehärtet: `TournamentFile` (`FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`),
  `ClickTtXmlImporter` und `ClickTtResultExporter` (`ACCESS_EXTERNAL_DTD`/`ACCESS_EXTERNAL_SCHEMA` leer).
  Bei jeder Prüfung erneut kontrollieren, dass neue Parser ebenso abgesichert sind.

## Verlauf der Prüfungen

| Datum | Geprüft von | Ergebnis | Maßnahme |
| --- | --- | --- | --- |
| 04.11.2026 | Claude (auf Wunsch des Nutzers) | keine kritische Lücke mit Bezug zu HTS; Updates verfügbar für Temurin (21.0.8 → 21.0.13), JavaFX (21 GA → 21.0.x) und PDFBox (3.0.5 → 3.0.8) | Empfehlung in ZWISCHENSTAND.md (Feature-Sammlung 0.31.0) notiert; Aktualisierung nur nach Entscheidung des Nutzers |
| 06.11.2026 | Claude (Umsetzung Issue #12, keine neue Prüfung) | Temurin 21.0.13+9 eingebaut | JavaFX und PDFBox noch nicht aktualisiert: Maven Central und Gluon/Apache-Downloads sind aus der Arbeitsumgebung gesperrt – Jar-Dateien vom Nutzer nötig (Issue #12 bleibt offen) |
| 07.11.2026 | Claude (HTS 0.35.0, keine neue Prüfung) | Neu: optionaler Handy-Zugang (lokaler Webserver, JDK-Modul `jdk.httpserver`, im ausgelieferten JRE enthalten) – keine neue Bibliothek | Angriffsflächen-Abschnitt ergänzt; bei der nächsten Prüfung CVEs zu `jdk.httpserver` (Teil der Java-Laufzeit) mit bewerten |
| 06.11.2026 | Claude (HTS 0.32.0, keine neue Prüfung) | JavaFX 21.0.8 und PDFBox/pdfbox-io/fontbox 3.0.8 eingebaut (Jars vom Nutzer, Herkunft Maven Central; Archive und Manifest-Versionen geprüft) | Issue #12 erledigt |

## Quellen der Prüfung 04.11.2026

- Adoptium: [Eclipse Temurin 17.0.21, 21.0.13, 25.0.5 and 27.0.1 Available](https://adoptium.net/news/2026/10/eclipse-temurin-17021-21013-2505-2701-available)
- Gluon: [OpenJFX 21 Release Notes](https://gluonhq.com/products/javafx/openjfx-21-release-notes/), [April 2026 Critical Patch Update for OpenJFX](https://gluonhq.com/news/april-2026-critical-patch-update-for-openjfx-now-available/)
- Maven Central: [org/openjfx/javafx-controls/21.0.8](https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.8/)
- Apache PDFBox: [Startseite (3.0.8)](https://pdfbox.apache.org/), [Snyk: org.apache.pdfbox:pdfbox](https://security.snyk.io/package/maven/org.apache.pdfbox%3Apdfbox)
- [CVE-2026-33929 (SentinelOne)](https://www.sentinelone.com/vulnerability-database/cve-2026-33929/), [CVE-2026-23907 (OpenCVE)](https://app.opencve.io/cve/CVE-2026-23907)
