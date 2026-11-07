# Hinweise für Claude (HTS – Hamburger Turniersoftware)

Diese Datei wird von Claude automatisch gelesen. Die folgenden Regeln gelten **ohne ausdrückliche Anweisung im Prompt**.

## Pflicht: Abhängigkeits- und CVE-Prüfung alle 6 Monate

Bei jeder Sitzung, in der am Projekt gearbeitet wird – spätestens bevor kompiliert, paketiert (Windows-ZIP) oder ein
Commit „HTS x.y.z“ erstellt wird – **selbständig** Folgendes tun:

1. In `docs/ABHAENGIGKEITEN.md` den Wert `LETZTE_PRUEFUNG: JJJJ-MM-TT` lesen.
2. Das aktuelle Datum ermitteln (Zeit-Werkzeug, nicht schätzen).
3. **Liegt die letzte Prüfung weniger als 6 Monate zurück: nichts prüfen**, nichts erwähnen, normal weiterarbeiten.
4. **Liegt sie 6 Monate oder länger zurück (oder fehlt die Datei/der Wert): die Prüfung durchführen**, bevor
   paketiert wird:
   - Für jede Komponente aus `docs/ABHAENGIGKEITEN.md` (Temurin-21-JRE im Ordner `jre`, JavaFX 21, Apache PDFBox inkl.
     pdfbox-io/fontbox, Commons Logging, dazu neu hinzugekommene Bibliotheken aus `pom.xml` und dem Fat-Jar) per
     Websuche die neueste Version der jeweiligen Linie und bekannte CVEs ermitteln.
   - Tatsächlich ausgelieferte Versionen prüfen, nicht nur `pom.xml` (JRE: `jre/release`; JavaFX: `javafx.properties`
     im Fat-Jar `hts.jar`; Jar-Namen der Build-Bibliotheken).
   - Jede CVE kurz für HTS bewerten (Desktop-Programm ohne Server/Netzwerk; Eingaben nur über XML- und .hts-Dateien).
   - Kontrollieren, dass alle XML-Parser weiterhin gegen XXE gehärtet sind (siehe Abschnitt in der Datei).
   - `docs/ABHAENGIGKEITEN.md` aktualisieren: Tabelle, Quellen, neue Zeile im „Verlauf der Prüfungen“ und im
     maschinenlesbaren Kommentar `LETZTE_PRUEFUNG` und `NAECHSTE_PRUEFUNG_FAELLIG_AB` (= Prüfdatum + 6 Monate).
   - Dem Nutzer das Ergebnis in wenigen Sätzen mitteilen. Versionen **nicht eigenmächtig aktualisieren**: Empfohlene
     Updates als Punkt in die Feature-Sammlung in `ZWISCHENSTAND.md` (und als GitHub-Issue „Feature“) eintragen; nur
     eine kritische, für HTS tatsächlich ausnutzbare Lücke sofort und deutlich melden.

## Pflicht: Oberfläche – Texte nie abkürzen (seit 0.36.0, Wunsch des Nutzers)

Bei jeder Änderung an der Oberfläche gilt ohne ausdrückliche Anweisung:
- Texte (Beschriftungen, Knöpfe, Tabellenzellen, Namen auf Spielkarten) werden **nie mit „…“ abgekürzt**.
- Wird es eng: Knöpfe und Texte in eine zweite Zeile umbrechen (FlowPane, `setWrapText(true)`) oder skalieren.
- Tabellen: Spalten so breit wie ihr Inhalt (`Ui.fitColumns`), notfalls waagerechte Scrollleiste.
- KO- und Qualifikationskarten so breit wie der längste Name (notfalls scrollt der Baum).
- Dialoge über `DialogSizing.fit` (scrollen bei kleinem Bildschirm statt abzuschneiden).
- Prüfen bei 1366 × 768 mit Windows-Skalierung 125 % und 150 % (entspricht ca. 1093 × 614 bzw. 911 × 512 Bildpunkten).

## Weitere Projektregeln

Arbeitsablauf (Feature-Sammlung, „kompilieren“, GitHub-Commit, Release-Link, Dokumentation nur auf Zuruf) steht in
`ZWISCHENSTAND.md`. Antworten an den Nutzer auf Deutsch.
