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
| JavaFX (OpenJFX) 21, Windows | 21 GA (`javafx.runtime.version=21+31`, 19.09.2023) – im Fat-Jar `hts.jar` | Windows-Jars `javafx-*-21-win.jar` (Build-Bibliotheken); **Achtung:** `pom.xml` nennt 21.0.5, ausgeliefert wird aber 21 GA | GPLv2 + Classpath Exception | 21.0.13 (20.10.2026); öffentlich auf Maven Central mindestens bis 21.0.8, neuere 21.0.x-Updates verteilt Gluon teils nur an LTS-Abonnenten | Die Updates seit 21 GA enthalten Sicherheitskorrekturen (u. a. WebView/Medien, die HTS nicht nutzt; HTS nutzt nur `javafx.controls`/`graphics` und `swing`). Risiko gering, Aktualisierung auf die neueste frei verfügbare 21.0.x empfohlen; `pom.xml` und ausgelieferte Version angleichen. |
| Apache PDFBox (pdfbox, pdfbox-io, fontbox) | 3.0.5 | Fat-Jar `hts.jar`; `pom.xml` | Apache 2.0 | 3.0.8 (11.07.2026) | Keine bekannte CVE für den Kern von 3.0.5 (Snyk). CVE-2026-33929 betrifft nur das Beispiel „ExtractEmbeddedFiles“ (nicht in HTS). CVE-2026-23907 (Pfad-Traversal beim Auslesen eingebetteter Dateien über `PDComplexFileSpecification.getFilename()`) betrifft das Lesen/Extrahieren von Anhängen – HTS erzeugt PDFs nur und liest keine fremden PDFs. Risiko sehr gering, Aktualisierung auf 3.0.8 empfohlen. |
| Apache Commons Logging | 1.3.5 | Fat-Jar `hts.jar` (von PDFBox benötigt) | Apache 2.0 | 1.3.5 aktuell | Keine bekannte CVE. |

## Nur zum Bauen (nicht ausgeliefert)

| Komponente | Version | Bewertung |
| --- | --- | --- |
| JUnit Jupiter | 5.10.2 | nur Tests, nicht ausgeliefert – unkritisch |
| maven-surefire-plugin / javafx-maven-plugin / maven-shade-plugin | 3.2.5 / 0.0.8 / 3.5.3 | nur Build – unkritisch |

## Eigener Code – geprüfte Angriffsflächen

- HTS ist ein Desktop-Programm ohne Server, ohne offenen Port, ohne Netzwerkzugriff zur Laufzeit.
- Eingaben von außen sind nur Dateien: click-TT-XML (Import), `.hts`-Turnierdateien, Ergebnis-XML.
- Alle XML-Parser sind gegen XXE gehärtet: `TournamentFile` (`FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`),
  `ClickTtXmlImporter` und `ClickTtResultExporter` (`ACCESS_EXTERNAL_DTD`/`ACCESS_EXTERNAL_SCHEMA` leer).
  Bei jeder Prüfung erneut kontrollieren, dass neue Parser ebenso abgesichert sind.

## Verlauf der Prüfungen

| Datum | Geprüft von | Ergebnis | Maßnahme |
| --- | --- | --- | --- |
| 04.11.2026 | Claude (auf Wunsch des Nutzers) | keine kritische Lücke mit Bezug zu HTS; Updates verfügbar für Temurin (21.0.8 → 21.0.13), JavaFX (21 GA → 21.0.x) und PDFBox (3.0.5 → 3.0.8) | Empfehlung in ZWISCHENSTAND.md (Feature-Sammlung 0.31.0) notiert; Aktualisierung nur nach Entscheidung des Nutzers |
| 06.11.2026 | Claude (Umsetzung Issue #12, keine neue Prüfung) | Temurin 21.0.13+9 eingebaut | JavaFX und PDFBox noch nicht aktualisiert: Maven Central und Gluon/Apache-Downloads sind aus der Arbeitsumgebung gesperrt – Jar-Dateien vom Nutzer nötig (Issue #12 bleibt offen) |

## Quellen der Prüfung 04.11.2026

- Adoptium: [Eclipse Temurin 17.0.21, 21.0.13, 25.0.5 and 27.0.1 Available](https://adoptium.net/news/2026/10/eclipse-temurin-17021-21013-2505-2701-available)
- Gluon: [OpenJFX 21 Release Notes](https://gluonhq.com/products/javafx/openjfx-21-release-notes/), [April 2026 Critical Patch Update for OpenJFX](https://gluonhq.com/news/april-2026-critical-patch-update-for-openjfx-now-available/)
- Maven Central: [org/openjfx/javafx-controls/21.0.8](https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.8/)
- Apache PDFBox: [Startseite (3.0.8)](https://pdfbox.apache.org/), [Snyk: org.apache.pdfbox:pdfbox](https://security.snyk.io/package/maven/org.apache.pdfbox%3Apdfbox)
- [CVE-2026-33929 (SentinelOne)](https://www.sentinelone.com/vulnerability-database/cve-2026-33929/), [CVE-2026-23907 (OpenCVE)](https://app.opencve.io/cve/CVE-2026-23907)
