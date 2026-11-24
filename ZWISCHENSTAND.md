# Zwischenstand HTS 1.5.1 (23.11.2026)

## Ablauf GitHub (BESTÄTIGT 03.11.2026)

PFLICHT BEI JEDEM „KOMPILIEREN“ (Nutzer 18.11.2026):
- IMMER alle drei Pakete bauen: hts-x.y.z-windows.zip, hts-x.y.z-linux.tar.gz, hts-x.y.z-macos.tar.gz (Bau siehe
  „1.1.0-NACHTRAG: LINUX“ unter Umgesetzt: $S/fat-linux, $S/fat-mac, $S/dist, JREs in $S/jx bzw. Uploads), alle drei
  per SendUserFile schicken, Release-Link mit Download-Hinweis für alle drei.
- Datei-Header (Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany + SPDX AGPL-3.0-or-later) in
  ALLEN Quelldateien prüfen bzw. ergänzen: .java (main und test), hts.css, Startdateien (HTS starten.bat,
  dist/HTS starten.sh, dist/HTS starten.command). Prüfbefehl: für jede Datei „SPDX-License-Identifier“ in den ersten
  Zeilen.
- Was im Abschnitt „Backlog“ steht, NICHT einbauen.
- HANDBUCH (Nutzer 18.11.2026, 21:53: „kompilieren inklusive Handbuch“): bei jedem „kompilieren“ das Handbuch
  mitziehen – Claude-Dokument „HTS-Dokumentation“ (Docs-Artifact 5b0b78d4-…, volle Ids mit Präfix mc3yk00qn9b) UND
  docs/HTS-Dokumentation.md (kommt in alle drei Pakete). Stand 23.11.2026: beide auf 1.5.1 (Docs-Dokument
  https://claude.ai/artifact/CF57Uw5Z3LoTJc9kK3R972, Knoten dc7b8f6f-8478) → bei jedem „kompilieren“ die neue Version
  nachtragen (Versionsverlauf, Stand-Zeile, Kapitel).
  Ersetzt die alte Regel „Dokumentation nur auf Zuruf“.

WUNSCH (15.11.2026): „wie bisher alles immer in das GitHub-Repository committen“ – auch Nachlieferungen zu einer
Version (z. B. Pakete, Startdateien, Doku) gleich als eigenen Commit hochladen.

Bei jedem „kompilieren“: Quellcode (src, pom.xml, README.md, ZWISCHENSTAND.md, CLAUDE.md, LICENSE, NOTICE, docs, samples, .gitignore,
„HTS starten.bat“) als EIN Commit „HTS x.y.z“ auf main in https://github.com/Knutilon/Hamburger-Turniersoftware---HTS
(Klon /home/claude/hamburger-turniersoftware---hts, push mit -c push.negotiate=false). Keine Releases/Tags, keine
alte Versionshistorie. Windows-ZIP weiter per Datei. Repo ist öffentlich. 0.28.0 hochgeladen (Commit b4cb270).
WUNSCH (03.11.2026, ab der nächsten Version): zusätzlich Release je Version mit dem Windows-ZIP und dem Nutzer statt
des ZIPs den Link zum aktuellen Release nennen. PROBLEM: Release anlegen ist aus der Sitzung gesperrt (HTTP 403 „not
permitted for this session type“), Tags-Push ebenfalls.
ENTSCHIEDEN (Weg 2): Commit „HTS x.y.z“ hochladen; dem Nutzer das Windows-ZIP schicken plus vorausgefüllten Link
https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/releases/new?tag=vX.Y.Z&target=main&title=HTS%20X.Y.Z
(+ Release-Text „Neu in X.Y.Z“ aus der README; SEIT 11.11.2026 direkt im Link als &body=… vorausgefüllt, URL-kodiert,
Punkte je eine Zeile, „hier oben“ → „in der README“, Schlusszeile Download-Hinweis). Nutzer zieht das ZIP hinein und veröffentlicht; danach
Link zum Release: https://github.com/Knutilon/Hamburger-Turniersoftware---HTS/releases/tag/vX.Y.Z

## Weg zu 1.0.0 (11.11.2026, mit dem Nutzer besprochen)

- Praxistest 1 erfolgreich; Praxistest 2 am Wochenende 14./15.11.2026.
- Kern ist vollständig. Handy-/Netzwerk-Funktionen (Handy-Zugang, Live-Anzeige, Anmeldung per QR-Code) gehören NICHT
  zum Kern und blockieren 1.0.0 nicht.
- Turnierdateien bleiben ab 1.0 lesbar (Feature-Sammlung Punkt „Turnierdateien bleiben lesbar“).
- Plan (ÜBERHOLT 15.11.2026: 0.9.0 entfällt): 0.9.0 als Testversion, danach 1.0.0; danach 1.0.x =
  Fehlerbehebungen, 1.x.0 = neue Funktionen.
- 0.42.0 umgesetzt (11.11.2026). 1.0.0 umgesetzt (15.11.2026) mit #77–#82; 0.9.0 entfiel. Ab jetzt 1.0.x =
  Fehlerbehebungen, 1.x.0 = neue Funktionen.
- Nutzer 15.11.2026: ALLE Punkte der Feature-Sammlung sind für 1.0.0: #34 WLAN-QR-Code, #77 Zeitplan, #78 doppelte
  Person beim erneuten Import, #79 Spielerabgleich (Design freigegeben); #34 dagegen ersetzt durch „Reiter Mobile
  Einstellungen ausblenden“ (Handy-Funktionen kommen später wieder).
- click-TT-Upload der Ergebnisdatei: Praxistest erfolgreich (Nutzer 15.11.2026); Einschränkung unter „Bekannte
  Grenzen“ in der Doku gestrichen (mit 1.1.0 ausgeliefert).

## Feature-Sammlung für 1.6.0 (NICHT kompilieren – bis „… kompilieren“)

(neue Punkte zusätzlich als GitHub-Issue mit Label „Feature“ bzw. „bug“ anlegen; Fehlerbehebungen ggf. als 1.5.x)

1. TABLET: TEST AUF ECHTEM GERÄT (Issue #133, offen aus 1.5.0): Die Tablet-Funktionen von 1.5.0 auf einem echten
   Windows-Tablet prüfen (Bildschirmtastatur/TabTip, Antippen, Zoom, Hochformat) – kann nur der Nutzer bzw. jemand mit
   Gerät; Ergebnisse als Bug/Feature melden.

2. ABSTURZ-PROTOKOLL (LOGGING) UND FEHLERBERICHT (Nutzer 23.11.2026, Issue #140):
   - Protokolldatei je Tag im Ablageordner der Turniere unter „Protokoll“ (hts-JJJJ-MM-TT.log), Aufbewahrung 30 Tage,
     höchstens wenige MB je Datei; java.util.logging, keine neue Bibliothek.
   - Inhalt: Programmstart (Version, Betriebssystem, Java, Bildschirmauflösung/Skalierung, Arbeitsspeicher), wichtige
     Schritte ohne Details (Turnier geöffnet, Phase angelegt, PDF erstellt, Webserver-Status), jeder unerwartete Fehler mit
     Stacktrace – auch aus Hintergrund-Threads (Drucken, Hochladen) und dem JavaFX-Thread (UncaughtExceptionHandler).
   - Datenschutz: keine Namen, Vereine oder Ergebnisse, nur Kennungen (Spiel-Nr., Klassenname).
   - Unerwarteter Fehler: Turnier vorher speichern, Fenster „Es ist ein Fehler aufgetreten“ mit „Fehlerbericht speichern“
     (ZIP der Protokolle der letzten Tage, z. B. für ein GitHub-Issue) und „Weiter“.
   - Startdateien (bat/sh/command): Absturzbericht der Java-Laufzeit (-XX:ErrorFile) und Hinweis bei Speichermangel
     (-XX:+ExitOnOutOfMemoryError o. Ä. + Protokolleintrag) in den Protokollordner.
   - Info: Knöpfe „Protokollordner öffnen“ und „Fehlerbericht erstellen“. Texte nie abkürzen, DialogSizing.fit.

3. WEBSERVER-EINSTELLUNGEN NUR MIT ADMIN-PASSWORT (Nutzer 23.11.2026, Issue #141):
   - Reiter „Webserver“ (Turnierübersicht): ohne Admin-Passwort nur Anzeige, Adresse und Schlüssel MASKIERT (nicht voll
     einsehbar, z. B. „http•••••.de“ bzw. nur „••••••••“), Felder und „Verbindung testen“ gesperrt; Knopf „Entsperren“ →
     Dialog Passwort (40 Zeichen). Richtig → Felder bearbeitbar (Werte weiter verdeckt, Auge-Knopf zum Zeigen nur
     entsperrt) bis zum Verlassen des Reiters. Falsch → Hinweis, kurze Wartezeit nach mehreren Fehlversuchen.
   - Das Passwort NIE im Quellcode, in Doku, Issues oder Commits (Repo ist öffentlich) – nur gesalzener Prüfwert:
     PBKDF2WithHmacSHA256, 310000 Iterationen, Salt (Base64) 5JmOyoZF68LcCHc+AkXMfw==,
     Prüfwert (Base64) 3OtUkAagHuJ0D58v5sOQfP5S4wj78l6FFD54U0yCrsg= (Länge 40 wird vorher geprüft; Vergleich zeitkonstant).
   - Hinweis an den Nutzer: Schutz gilt für die Oberfläche; Adresse/Schlüssel liegen weiter in den Java-Einstellungen des
     Rechners (Windows-Registrierung) – ggf. später verschlüsselt ablegen.

4. WEBSERVER-ADRESSE UND SCHLÜSSEL VERSCHLÜSSELT ABLEGEN (Nutzer 23.11.2026, Issue #142; ergänzt Punkt 3):
   - Statt Klartext in Preferences: AES-256-GCM (javax.crypto, keine neue Bibliothek), je Wert eigener Zufalls-IV.
   - Schlüssel zum Entschlüsseln (HTS muss ohne Admin-Passwort hochladen können):
     Windows → zufälliger AES-Schlüssel, geschützt mit DPAPI (an das Windows-Benutzerkonto gebunden; Aufruf über
     PowerShell ConvertTo-/ConvertFrom-SecureString bzw. [Security.Cryptography.ProtectedData], keine JNA);
     Linux/macOS → zufälliger Schlüssel in einer Datei im Benutzerprofil, nur für den Benutzer lesbar (rw-------).
   - Vorhandene Klartext-Einträge (1.5.0) beim ersten Start verschlüsselt übernehmen und den Klartext löschen.
   - Lässt sich nicht entschlüsseln (anderer Rechner/Benutzer): Hinweis „Webserver-Einstellungen neu eintragen“ (mit
     Admin-Passwort), Veröffentlichen pausiert.
   - Grenze (dem Nutzer gesagt): schützt gegen Mitlesen in der Registrierung und Kopieren auf andere Rechner, nicht gegen
     jemanden, der unter demselben Benutzerkonto angemeldet ist.

5. AUSRICHTER: „LOGO HOCHLADEN“ OHNE „…“, SVG ERLAUBEN (Nutzer 23.11.2026, Issue #143):
   - Knopf im Reiter „Ausrichter“ heißt „Logo hochladen“ (ohne „…“; auch sonst keine „…“ in Knopftexten prüfen).
   - Dateiauswahl: PNG, JPG und SVG. SVG wird beim Hochladen in ein PNG umgerechnet (lange Seite ca. 1200 Bildpunkte,
     damit es auf dem A6-Zettel scharf druckt; Transparenz bleibt). Dafür neue Bibliothek nötig – Vorschlag JSVG
     (com.github.weisj:jsvg, MIT-Lizenz, ohne weitere Abhängigkeiten): in pom.xml und Fat-Jar, Lizenztext unter
     „Lizenzen“, Info „Mitgelieferte Bibliotheken“, docs/ABHAENGIGKEITEN.md (CVE-Prüfung). Jar muss ggf. der Nutzer
     liefern, falls Maven Central aus der Arbeitsumgebung gesperrt ist.
   - Unlesbare SVG (z. B. mit externen Verweisen/Skripten): verständliche Fehlermeldung; externe Verweise nicht laden.

6. FREISTELLUNG: KO-PAARUNGEN BEI 6 GRUPPEN × PLÄTZE 1–2 – SCHEMA MIT AUSLOSUNG JE HÄLFTE (Nutzer 23./24.11.2026,
   Issue #145): Gruppenphase mit Freistellung, 6 Gruppen A–F, „Weiter je Gruppe“ Plätze 1–2 → 16er-KO ohne Freilose.
   Grundschema (Reihenfolge im Baum von oben nach unten):
     obere Hälfte:  S1–D2 · F1–E1 · A1–B2 · C2–S4
     untere Hälfte: S3–E2 · A2–B1 · C1–D1 · F2–S2
   NEU 24.11.2026 (zweite Klarstellung 09:07): AUSLOSUNG statt festem Schema, mit diesen Regeln:
   - S1 und S4 immer in der oberen Hälfte, S2 und S3 immer in der unteren (Platz innerhalb der Hälfte – oberstes bzw.
     unterstes S-Spiel – wird ausgelost).
   - Gruppenerster und Gruppenzweiter derselben Gruppe NIE in derselben Baumhälfte: es werden 3 Gruppen ausgelost, deren
     Erste oben und Zweite unten spielen; bei den übrigen 3 Gruppen umgekehrt. So hat jede Hälfte 3 Erste und 3 Zweite.
   - Platzarten je Spiel bleiben (oben S–2 · 1–1 · 1–2 · 2–S; unten S–2 · 2–1 · 1–1 · 2–S); innerhalb der Hälfte werden die
     Ersten bzw. Zweiten zufällig auf die Plätze ihrer Art verteilt.
   - Folge: jeder Gruppenbuchstabe je Hälfte nur einmal → kein Gruppenduell bis zum Finale. Zwei Gruppenerste gegeneinander
     (je Hälfte ein 1–1-Spiel) ist gewollt.
   - Das Grundschema oben (S1–D2 · F1–E1 · … ) ist nur EINE mögliche Auslosung.
   „Neu auslosen“ lost erneut; Tausch per Ziehen/Antippen vor dem Start wie bisher. Handbuch und Modus-Dialog-Hinweis
   ergänzen; Test mit 6 × 2 (Hälften-Zugehörigkeit, Platzarten, Zufall mit festem Startwert).

7. NEUE FARBGEBUNG „NEUTRAL MIT GRÜN #187250 UND MINT“ UND NEUES HTS-LOGO (Nutzer 24.11.2026, Issue #146;
   Ablauf: Vorschlag #00965F → Varianten D1–D3 → „zu dominant“ → neutrale Grundlage t1 → Partnerfarbe Mint →
   Primärfarbe dunkler im Grün des Logos → auf Wunsch #187250 statt #0F5038 für die Oberfläche; das LOGO bleibt bei #0F5038 (Nutzer 18:16). Vorschau-Stildatei: docs/entwuerfe/hts-farbschema-vorschau.css):
   - GRUNDLAGE NEUTRAL: Schrift und Überschriften dunkelgrau #1F2933 (bisher Marineblau), Seitenhintergrund #F6F7F8,
     Karten weiß ohne farbigen Rand. Nebenknöpfe weiß mit grauem Rand #D0D5DB und Schrift #1F2933.
   - PRIMÄRFARBE = GRÜN #187250 (Hover #125C40), weiße Schrift (Kontrast ca. 5,9 : 1): Hauptknöpfe („Öffnen“,
     „Neues Turnier“, „Ergebnis“, Standardknopf in Dialogen, „Weiter“ im Zahlenfeld), Schriftzug „Hamburger
     Turniersoftware“, Linie unter der Kopfzeile (2 px). #00965F entfällt; #0F5038 nur im Logo.
   - PARTNERFARBE MINT #5FD3A8 an ALLEN Stellen, an denen bisher Türkis war: runde Knöpfe oben rechts, Pfeilkreise in der
     Turnierliste, Unterstrich des aktiven Reiters, Abzeichen „aktiv“/„bereit“, Kartenrand „läuft“ (wie bisher Türkis),
     Ablagefelder beim Ziehen, Fokusrahmen von Eingabefeldern, Trennstrich vor dem Turniernamen. Schrift/Symbole auf Mint
     dunkelgrün #0B3D2C. Helle Tönung Mint #E4F7EF statt Türkis-hell: markierte Zeilen, Hover, „Weiter“-Abzeichen.
   - KOPFZEILE WEISS: Menüpunkte #4A5560, aktiver Reiter Schrift #1F2933 mit Mint-Unterstrich (3 px).
   - Ausgegraute Knöpfe deutlich blasser (Deckkraft ca. 45 %), damit „nicht verfügbar“ klar erkennbar ist.
   - Gelb bleibt „Testturnier“/„pausiert“/„Ergebnis fehlt“, Rot = Fehler; Punkt „gestartet/live“ Grün des Logo-Balls #19B877.
   - Umsetzung: Variablen in hts.css (-hts-navy, -hts-turquoise … umbenennen, z. B. -hts-text, -hts-primary, -hts-accent,
     -hts-accent-light), fest eingetragene Farben in hts.css und Java (Color.web …, z. B. Abzeichen „beendet“ in der
     Präsentationsansicht, Kartenränder) auf die Variablen umstellen; Präsentationsanzeige, Live-Seiten (LiveServer) und
     Info-Dialog mitziehen. Druck (Schiedsrichterzettel, Urkunden, Listen) bleibt hell; Marineblau/Türkis dort durch
     Grün #187250/Mint ersetzen, soweit farbig gedruckt wird.
   - NEUES LOGO (Variante 3 „Dunkelgrün“, Entwurf docs/entwuerfe/hts-logo-dunkelgruen.png): dunkelgrüner Hintergrund
     (#0F5038 → #0A3526, bleibt so – NICHT an #187250 angleichen), „HTS“ weiß, Ball, Flugbahn und Unterstrich hellgrün (#19B877); das „T“ ist eine kleine
     Tischtennisplatte (Plattenkante = Querbalken mit grüner Oberfläche, Netz quer, Mittellinie; Mittelfuß mit Fußplatte
     = Stamm). Ersetzt src/main/resources/de/hts/ui/hts-logo.png (Fenster-Symbol, Kopfzeile, Präsentation, Live-Seiten,
     HTS-Kennzeichen auf dem Schiedsrichterzettel) sowie die Symbole in den Paketen (.ico 16/32/48/256, .icns, .png).
     Lesbarkeit bei 16 und 32 px prüfen.
   - Prüfen bei 1093 × 614 und 911 × 512; Handbuch-Bildschirmfotos neu.

8. LIVE-SERVER AUF DEM GEMIETETEN NETCUP-SERVER (#101, Nutzer 24.11.2026 aus dem Backlog geholt: Server gebucht –
   netcup VPS nano G11.5s, Nürnberg, 2 Kerne/2 GB/60 GB, IPv4+IPv6; Ubuntu 24.04.5 LTS UEFI Minimal installiert; Domain
   beim Domain-Anbieter per A/AAAA-Eintrag auf den Server gelegt). HTS bleibt führend, der Server hält nur eine Kopie.
   - SERVER-PROGRAMM ohne Bildschirm (eigenes Jar, z. B. hts-server.jar, nur Java 21, ohne JavaFX): nimmt das Protokoll
     aus docs/WEBSERVER-PROTOKOLL.md an (GET /api/status, PUT /api/turniere/<ID>, PUT …/logo, DELETE, POST
     …/nur-endergebnis; Schlüssel als Bearer, Vergleich in konstanter Zeit), liest die .hts-Datei sicher (Größengrenze,
     kein XXE) und zeigt unter /live/<ID> die LiveServer-Seiten: Tische, Gruppen, KO-Baum, Ergebnisse, „Meine Spiele“,
     Ausrichter-Logo. Aktualisierung ohne Neuladen (kurzes Polling bzw. SSE). Startseite /live listet laufende Turniere.
     Fußzeile mit Links „Impressum“ und „Datenschutz“ (Adressen in der Server-Konfiguration).
     Live-Scoring nur je Partie/Satz; KEINE Ergebniseingabe über das Internet in dieser Stufe (#93 bleibt Backlog).
   - Daten auf dem Server: je Turnier ein Ordner (letzter Stand + Logo); „Live-Daten löschen“ löscht, „Nur Endergebnis
     behalten“ entfernt Tische/Spiele; Aufbewahrung automatisch begrenzen (z. B. 90 Tage nach letzter Änderung).
   - EINRICHTUNGSSKRIPT für Ubuntu 24.04 (ein Befehl als root, fragt nur nach der Domain und den Impressum/Datenschutz-
     Links): Benutzer „hts“ ohne Anmeldung, Java 21 (openjdk-21-jre-headless), Caddy aus dem offiziellen Paketquellen-
     Eintrag (automatisches HTTPS, Weiterleitung http → https), Firewall ufw (nur 22, 80, 443), unattended-upgrades,
     systemd-Dienst hts-server (Autostart, Neustart bei Absturz, nur auf 127.0.0.1 hinter Caddy), erzeugt den geheimen
     Schlüssel (zufällig, 40+ Zeichen) und zeigt ihn am Ende EINMAL an. Optional SSH nur mit Schlüssel.
     UPDATE-SKRIPT für neue Versionen (lädt hts-server.jar aus dem GitHub-Release, prüft SHA-256, startet neu).
   - Release: hts-server.jar + Skripte als zusätzliche Dateien beim Release.
   - ANLEITUNG docs/LIVE-SERVER-ANLEITUNG.md Schritt für Schritt für genau diesen Server (SSH-Anmeldung, Skript, Prüfen,
     Schlüssel in HTS eintragen, Verbindung testen, Testturnier, Fehlerbehebung), dazu AV-Vertrag netcup, Impressum/
     Datenschutz, Hinweis in der Ausschreibung.
   - Tests: Protokoll (Schlüssel falsch → 401, Größe, Löschen), Seiten mit Belastungs-Turnier, Last (viele Zuschauer).

## Backlog – NICHT kompilieren (Nutzer 18.11.2026: bei KEINEM „kompilieren“ berücksichtigen, bis ausdrücklich etwas anderes gesagt wird)

- (#101) → am 24.11.2026 in die Feature-Sammlung 1.6.0 übernommen (Punkt 8).

## Entfallen (Nutzer 18.11.2026: Ansatz über gemieteten Hetzner-Server CX23 statt dessen)

1. WIEDER ZURÜCKGESTELLT (15.11.2026: statt dessen Punkt 5 – Reiter „Mobile Einstellungen“ ausblenden; NICHT in
   1.0.0 umsetzen, erst wenn die Handy-Funktionen später ausgebaut werden): WLAN-QR-CODE AUF DEM
   AUSHANG (Issue #34): In HTS einmal WLAN-Name und Passwort eintragen; der Aushang
   „QR-Codes drucken“ bekommt einen dritten QR-Code „1. WLAN verbinden“ (Format WIFI:T:WPA;S:…;P:…;;), darunter
   „2. Live-Anzeige“ und „3. Anmeldung am Turniertag“. So bleibt das WLAN geschützt, die Teilnehmenden tippen nichts
   ab. Vorschlag (Rückfrage gestellt): Eingabe in einem kleinen Dialog beim Klick auf „QR-Codes drucken“, gespeichert
   als Programmeinstellung auf dem Laptop (gleicher Router für alle Turniere), nicht in der Turnierdatei; Name und
   Passwort zusätzlich als Text unter dem QR-Code; Verschlüsselung WPA/WPA2 (Standard); Feld leer = kein WLAN-QR-Code
   (wie bisher Felder zum Ausfüllen).
- (#92) HTS ALS ANDROID-APP (Nutzer 15.11.2026, Issue #92): „Als echte App anstreben, aber als GitHub-APK-Datei“ – Android
   zuerst, Bau über GitHub Actions (GluonFX/GraalVM + Android-SDK), APK als Datei im Release, kein Play Store; iPad
   zurückgestellt (Apple-Konto, Mac). Vor der Umsetzung klären: Umfang der App (ganzes HTS oder Teil), Bedienung auf
   Tablet/Handy, Dateien (.hts/XML/PDF) auf dem Gerät, Drucken. NICHT kompilieren bis „… kompilieren“.


## Umgesetzt

1.5.1 (23.11.2026, Commit „HTS 1.5.1“, Closes #144; nur Fehlerbehebungen – auf Zuruf „kompilieren“ als 1.5.1
   gebaut, die Punkte #140–#143 der Feature-Sammlung bleiben für 1.6.0):
   0. FEHLER 1.5.0 – LANGSAM NACH DEM AUSLOSEN / BEIM KLASSENWECHSEL (Nutzer 23.11.2026, Issue #144, ERLEDIGT im Code,
   für 1.5.1): Ursache die Zoom-Hülle (Group) um overviewBox in DrawView (#129) – 16 Gruppen: Auslosen + Anzeige 17,8 s statt
   0,8 s. Jetzt Hülle nur bei Zoom ≠ 100 % (setZoom tauscht den Inhalt des Scrollbereichs, Mindestbreite dann fest statt
   gebunden). Messung mit /home/claude/work/uikeep/ShotLag.java (1.4.0-Vergleich in /home/claude/work/old140).
   Daneben (schon seit 0.34): Gruppenansicht im Leerlauf ca. 40–50 % Last im Programm-Thread. Ursache gefunden: die
   Bindung overviewBox.minWidth an scroll.viewportBounds (createDoubleBinding) wurde bei jedem Layout ungültig (der
   Scrollbereich setzt die Ansichtsgrenzen jedes Mal neu) und forderte erneut Layout an → Endlosschleife je Bild.
   ERLEDIGT: DrawView.bindMinWidth als ChangeListener mit Schwelle 0,5 px – Leerlauf 2,0 s → 0,5 s CPU je 4 s (wie KO-Ansicht).
   Absturz selbst nicht nachvollziehbar (kein Protokoll) – vermutlich das lange Einfrieren; Protokoll (Punkt 2) hilft künftig.
   0b. SCHNELLEINGABE IM ERGEBNISDIALOG ENTFERNEN (Nutzer 23.11.2026, betrifft #122, ERLEDIGT im Code, für 1.5.1): Die
   Knöpfe „11:3 … 11:9“ / „3:11 … 9:11“ sind aus „Ergebnis eintragen“ entfernt; das Zahlenfeld (Tablet) bleibt.
   README/Handbuch beim nächsten „kompilieren“ anpassen.


1.5.0 (23.11.2026, Commit „HTS 1.5.0“, Closes #114–#132, #135–#139; #133 bleibt offen). README „Neu in 1.5.0“,
   Handbuch (md + Claude-Docs), docs/WEBSERVER-PROTOKOLL.md neu.
   - #114 hts.css .spinner als ein graues Feld. #115/#116 Urkunden je abgeschlossener Phase: Tournament.fixedEntrants
     (keine spätere Phase + Phase komplett, ohne offenen Korrektur-Reiter), certificateClasses, certAskKey („Klasse ·
     Phase n“ bzw. Klassenname, Datei „cert-asked“), Reiter ab Turnierstart; Dialog-Text je Phase/Klasse.
   - Belastungstest 30 × 200 (stress140/Stress140.java, uikeep/ShotBig.java): Urkundenliste jetzt TableView (vorher
     OutOfMemory), certName über playerById, Tournament.currentCertEntry. Urkunden-Reiter dreispaltig ab 1420 px
     (CertificatesView.layoutFor), Suche, „n Urkunden drucken“.
   - #117 TouchSupport.isActivate (Antippen = Doppelklick) an allen Öffnen-Stellen, Knopf „Ergebnis korrigieren“ in
     Gruppenzeilen. #118 Szenenfilter zeigt Tooltip des Rahmens gesperrter Knöpfe beim Antippen/Klick. #119 TabTip.exe
     (sonst osk.exe) beim Antippen eines Eingabefelds (nur Windows, ungetestet). #120/#129 Zoom (Scale auf overviewBox,
     ZoomEvent, Strg+Mausrad, Knopf „Zoom n % – zurücksetzen“), größere Blätter-Pfeile. #121/#122 Zahlenfeld (nur
     Tablet-Modus/Touch) und Schnelleingabe im ResultDialog. #123 Tischkarte: Klick auf die Partie = Ergebnis.
     #124 TouchSupport.setTabletMode (Preferences), Schalter in der Info. #125 größere Dialogknöpfe (.touch).
     #126 Spalte „Aktionen“ mit „⋮“ in PlayerTableView. #127 DrawView: Tauschen per Antippen (TAP_SOURCE/TAP_TARGET
     an startDrag/dropTarget, Stil .tap-picked). #128 TablesView hochkant (StackPane wide/tall). #130 Ui.busy
     („Bitte warten“, alle Reports-PDFs, Phase abschließen, Urkunden drucken). #131 Tournament.undoableResult/
     undoLastResult, Knopf auf Tische/Spiele. #132 certPrinted (Datei „cert-printed“), Spalte „Gedruckt“, Auswahl
     „Noch nicht gedruckte“, Reports.printWithDialog(…, onPrinted).
   - #135–#138 de.hts.web.Publisher (HTTP-Client, Drosselung 5 s, Puffer/Retry 10 s, Status), WebSettings
     (Preferences), Tournament.publish/publishAbbrev (Datei „publish“, „publish-abbrev“), TournamentFile.publishBytes/
     abbreviateFirstNames; Overview-Reiter „Webserver“ (Verbindung testen = GET /api/status), Turnierseite „Live im
     Internet“, Statusanzeige unten rechts. Getestet gegen Testempfänger (Version150Test). Server (#101) weiter Backlog.
   - #139 de.hts.persistence.Organizer (ausrichter.properties + ausrichter-logo.png im Ablageordner), Overview-Reiter
     „Ausrichter“, Logo auf Schiedsrichterzettel (PdfCanvas.image, rechtsbündig 90 × 38 px) und Live-Seiten
     (/live/logo.png). Standard-Logo HaTTV (de/hts/report/hattv-logo.png, vom Nutzer im Zettel-Entwurf eingefügt;
     Hinweis in Lizenzen/LIESMICH und ABHAENGIGKEITEN).
   - Schiedsrichterzettel nach Nutzer-Entwurf: Logo oben rechts, HTS-Marke unten rechts in der Legendenzeile (genau A6).
   Tests: Version150Test (8), Version140Test (9), Fixture turnierdatei-1.5.0; alle Tests grün.


1.4.0 (22.11.2026, Commit „HTS 1.4.0“, Closes #108–#113). README „Neu in 1.4.0“, Handbuch (md + Claude-Docs) nachgetragen.
   #108 Enum SlipDesign (PLAYER/REFEREE), Tournament.slipDesign, Turnierdatei „slip-design“ (auch in der Leseliste von
   TournamentFile ergänzt); PdfReports.slip verzweigt: playerSlip (ohne Wahl/Unterschrift SR, ohne Zeile „Sätze“, neu
   „Ergebnis (gewonnene Sätze)“ mit zwei Boxen + Gewinner/in) und refereeSlip (A6, Entwurfsmaße in px × 0,75; A/X bzw.
   A B X Y, Auf/Rück, Karten, Auszeit T, 1. Aufschlag, Gewinner/in, Schiedsrichter/in; dunkelblau #173a6c, grau #e4e4e4);
   PdfCanvas: farbige text/rect/fill/line. WorkspaceView.slipDesignControl (ComboBox in topActions der Seite „Tische“,
   Zellen mit Kurzbeschreibung). Mannschaftszettel unverändert.
   #109 InfoDialog.THANKS „Danke an:“ + Comic Neue in „Mitgelieferte Bibliotheken“.
   #110 DialogSizing: state[0] = Programm setzt Größe, state[1] = Nutzer hat geändert (Fensterlistener „hts.sizeWatch“);
   danach nur noch mitwachsen in der Höhe. NOCH MIT ECHTER MAUS UNTER WINDOWS PRÜFEN (nur Xvfb getestet).
   #111 PresentationView.nextRow: eingerückte Zeile .count-box „Anzahl der Spiele:“ + Hinweis, ausgegraut ohne Häkchen.
   #112 OverviewView: Reiter „Turniere“/„Archivierte Turniere“ (isArchived: Datum vor mehr als 14 Tagen), beide nach
   Datum absteigend; im Archiv ohne „Neues Turnier“/Import. Ablageordner-Zeile bricht um (statt „…“).
   #113 Urkunden: Tournament.certTitle/Place/Signer/Host (leer bleibt leer), certLeaveTop, certTabShown, certOverrides
   (Klasse+Partei → Name/Verein), certificateEntries()/CertEntry, setCertOverride; Turnierdatei „cert-title“ … „cert-tab“
   (hasAttribute, damit leere Werte erhalten bleiben) und „cert-ov.N“ (URL-kodiert). CertificateReport (A4, Positionen
   der Vorlage, Comic Neue Bold als PDType0Font; Dokument wird einmal gespeichert/neu geladen, damit Vorschau und Druck
   die eingebettete Schrift sehen). Schrift: Debian fonts-comic-neue 2.51 (OTF/CFF) mit fontTools in TTF umgerechnet
   (/home/claude/work/comic), src/main/resources/de/hts/fonts/ComicNeue-Bold.ttf + OFL-ComicNeue.txt (auch in
   „Lizenzen“ aller Pakete, LIESMICH ergänzt, docs/ABHAENGIGKEITEN.md). CertificatesDialog („Urkundendruck starten?“,
   Knopfreihenfolge „CO“ = links Abbrechen, rechts Zur Druckansicht), CertificatesView (Klasse, Plätze 1 bis 3 / Alle /
   Eigene Auswahl, Liste mit Häkchen und Feldern, Markierung „nur für Druck geändert“, Urkundentexte, Vorschau mit
   Blättern, PDF/Drucken). WorkspaceView.offerCertificates beim Übergang zu isCompletelyFinished (nach „Klasse
   beenden“), Menüpunkt „certs“ dauerhaft (certTabShown oder schon beendet beim Öffnen).
   Tests: Version140Test (7), Fixture turnierdatei-1.4.0; alle 262 Tests grün. Screenshots 1093×614 / 911×512 mit
   /home/claude/work/uikeep/Shot140.java (shots140.sh).
   Offene Annahme: Auf der Urkunde steht nur der Turniername (ohne „errang bei …“) – ggf. mit dem Nutzer klären.


1.3.0 (20.11.2026, Commit „HTS 1.3.0“, Closes #102–#107): #102 PdfReports.standings(…, badeni) und DrawView.groupCard
   ohne BHZ/FBHZ bei Badeni. #103 DrawView.buildSwiss mit fester Leiste (stickyHolder/setStickyBar, Stil „sticky-bar“,
   Infotext bricht um) und swissRoundList (aktuelle Runde zuerst, Chip „läuft · n offen“). #104 Chips .medal-1/2/3
   (Schweizer System/Badeni nach dem ersten Spiel) und .adv-1/2/3 über Tournament.advanceMarks/advanceMarkText (nur mit
   angelegter Phase 2; Leistungsstufen ohne). #105 PlayingClass.qualExtraDecision (Datei qual-extra-decision),
   Tournament.canAdd/add/canRemove/removeQualificationExtra, qualificationExtraDecisionPending, decideQualificationExtra,
   Nachrücker-Ränge in qualificationTiers; DrawView-Knöpfe, ResultEntry-Frage. #106 PlayingClass.exempt/exemptPlaces
   (Datei exempt, exempt-places), Tournament.groupEntrantsOf/exemptSuggestion/exemptProblem/exemptSuggestionText/
   drawGroupsWithExemption/exemptFieldSize/buildExemptKnockout (festes Schema EXEMPT_4X3, sonst seedOrder +
   avoidSameGroupInRoundOne), swapGroupMembers tauscht Freigestellte; ModeDialog Opt.EXEMPT (S1–S4-Auswahl, Prüfung,
   „Auslosen“ gesperrt), PhaseCloseDialog nur KO, DrawView.exemptBar. #107 PlayingClass.recipe/recipeSeed (jede
   Folgephase merkt Art, Einstellungen und Los: withRecipe/recreate), correctionOf, deactivated (Datei recipe,
   recipe-seed, correction-of, deactivated); Tournament.updateCorrection (nach correctResult/recordTeamResult),
   newEntrants/newPairings, takeOverCount/takeOverUnchanged, adoptInfo/adoptCorrection, discardCorrection,
   deactivatedAfter, isInactiveMatch; removeClass räumt Korrektur-/deaktivierte Phasen mit ab. UI: ClassesView-Reiter
   (deaktiviert, Korrektur, Blättern mit 6 Reitern je Seite: logicalTabs/showTabs/selectTab), DrawView.showSideClass
   (Leiste, Legende, Warnhinweis adoptCorrection), Markierungen corr-new-pair/corr-new-player, MatchesView „deaktiviert“.
   Außerdem: KnockoutDraw.pairUp (Vorausschau gegen Gruppenduelle in Runde 1), Reiterkopf-Reihenfolge (clear + addAll).
   Tests Version130Test (7), Fixture turnierdatei-1.3.0.hts, Belastungstest mit Freistellung (32er-Feld) und
   Korrektur-Reiter. Umgesetzte Punkte (Wortlaut der Feature-Sammlung):

   1. BADENI: SPALTEN BHZ UND FBHZ AUSBLENDEN (Nutzer 20.11.2026, Issue #102): Beim Schweizer System nach Badeni zählen
      Buchholz und Feinbuchholz nicht (Reihung Siege, TTR, Sätze); in der Tabelle stehen dort derzeit nur „null“.
      Vorschlag (übernommen): Spalten auch im PDF der Phase, in der Präsentationsansicht und auf den Live-Seiten
      entfernen; normales Schweizer System unverändert; in 1.3.0.
   2. BADENI: LEISTE „WEITERE RUNDE / SPIELER PAUSIEREN“ FIXIEREN (Nutzer 20.11.2026, Issue #103): Knöpfe links, Infotext
      rechts daneben (bricht um, nie „…“), oben und unten feine graue Linie (#d5d9e0) als Trennung von den Phasen-Knöpfen
      bzw. der Tabelle; die Leiste bleibt beim Scrollen der Tabelle stehen, gescrollt mit leichtem Schatten.
      Spalte „Spiele“ bleibt rechts neben der Tabelle: oben immer die aktuelle Runde (Status „läuft · n offen“), darunter
      die früheren Runden absteigend; sind alle Runden gespielt, von der letzten bis zur ersten Runde. Gilt für Schweizer
      System und Badeni. Design vom Nutzer bestätigt (20.11.2026): Design-Canvas
      https://claude.ai/artifact/6fv4ofe1C21Qqqfhm7R8Aj, Bilder 1–3.
   3. TABELLEN-MARKIERUNG (Nutzer 20.11.2026, Issue #104): Schweizer System und Badeni – Plätze 1–3 als runde Chips
      in Gold (#e9c44a), Silber (#cfd4dc), Bronze (#d79a62). Gruppenphase – keine Platz-Markierung; erst wenn Phase 2 als
      Phase 2 angelegt ist, liegt der Gruppenplatz der Weiterkommenden in einem runden Chip OHNE Beschriftung (Nutzer
      20.11.2026, bei allen Phasentypen): dunkelblau = in Phase 2 / Hauptrunde / direkt qualifiziert; hellblau = Trostrunde
      bzw. KO-Qualifikation; grau = Nachrücker (Qualifikationsmodus); Leistungsstufen (alle kommen weiter): keine Markierung
      (bestätigt 20.11.2026). Wer nicht weiterkommt: ohne Chip. Design: gleicher Canvas, Bilder 1, 3 und 4.
   4. QUALIFIKATION: OPTIONALES ZUSATZSPIEL FÜR DIE VERLIERER DER RUNDE 2 (Nutzer 20.11.2026, Issue #105; Anlass: HEM-Quali
      12 Gruppen à 6–7, direkter Vergleich, Gruppenerste direkt, Zweite/Dritte KO, 2 Siege = qualifiziert – Simulation:
      alles abbildbar außer diesem Spiel; die Zusatzrunde wurde in 0.38.0 entfernt). Nach Runde 2 Knopf „Zusatzspiel für die
      Verlierer der Runde 2“ im Phasen-Reiter (nur bei Bedarf, zurücknehmbar ohne Ergebnis); frei gelost, möglichst ohne
      Wiederholung. KORREKTUR Nutzer 20.11.2026: für die Gewinner des Zusatzspiels wird entschieden, ob sie qualifiziert
      oder Nachrücker sind (Vorschlag BESTÄTIGT 20.11.2026: Frage nach dem letzten Zusatzspiel wie nach Runde 1, nachträglich
      änderbar bis zum Klassenende). Qualifiziert: Verlierer Zusatzspiel = Nachrücker 1. Ranges, Verlierer Runde 1 = 2.;
      Nachrücker: Gewinner Zusatzspiel = 1., Verlierer Zusatzspiel = 2., Verlierer Runde 1 = 3. Ranges; innerhalb eines
      Ranges nach TTR. Ohne Zusatzspiel wie bisher. Dazu veralteten Hinweis „Zusatzrunde im Phasen-Reiter“ im Dialog „Phase abschließen“ korrigieren.
   5. PHASE 1 „GRUPPENPHASE MIT FREISTELLUNG“ (Nutzer 20.11.2026, Issue #106): Setzplätze 1–4 freigestellt (keine Gruppenspiele),
      Auswahl im Modus-Dialog: STANDARD vorausgewählt die 4 mit dem höchsten TTR (gleicher TTR: Los; Nutzer 20.11.2026),
      änderbar per Auswahlliste je Platz oder Drag & Drop, „Auf TTR-Vorschlag zurücksetzen“; Phase anlegen erst mit 4. Gruppenanzahl/
      -größe frei; „Weiter je Gruppe“ Plätze 1–2 oder 1–3. Phase 2 fest: KO ab Achtelfinale OHNE Freilose – Freigestellte +
      Gruppen × Weiterkommende muss 2/4/8/16/32/64/128 ergeben, sonst schon bei der Phasenanlage (Modus-Dialog) Hinweis mit Rechnung und Vorschlag, Anlage der Phase gesperrt (bestätigt).
      Beispiele: weibl. Jugend 15/19 4 + 4 Gruppen × 3 = 16; männl. Jugend 15/19 28 Spieler: 4 + 6 Vierergruppen × 2 = 16.
      Feste Paarungen bei 4 × 3 (bestätigt): S1–D3, B1–D2, A1–C2, S4–C3, S3–A3, C1–B2, D1–A2, S2–B3; sonst Freigestellte auf
      Setzplatz 1–4 gegen die schlechtesten Gruppenzweiten (nach Spiel-, Satz-, Punkteverhältnis; S1 gegen den schlechtesten),
      die schlechtesten Gruppenersten gegeneinander, die besten Gruppenersten gegen die übrigen Gruppenzweiten (BESTÄTIGT
      20.11.2026). Beispiel 6 × 2: S1–Z6, S2–Z5, S3–Z4, S4–Z3, E1–Z2, E2–Z1, E3–E6, E4–E5; gleiche Gruppe in Runde 1 möglichst
      vermeiden (Tausch mit dem Nächstplatzierten). Design: https://claude.ai/artifact/VvCMhT5WsVpLhJvbfFb1zn
   6. KORREKTUR IN PHASE 1 BEI LAUFENDER FOLGEPHASE (Nutzer 20.11.2026, Issue #107): Ändert eine Korrektur in Phase 1 die
      Folgephase, entsteht ein weiterer Reiter „Phase 2 – Korrektur“ (kein Dialog); Hin- und Herschalten möglich; Ansicht 1:1
      wie die laufende Phase, nur korrekt – markiert nur neue Partien (Rahmen) und neue Spieler/innen (hinterlegt). Laufende Phase bleibt ausschlaggebend, aus der Korrektur wird nichts eingereiht, bis sie
      aktiv ist. Knopf „Unveränderte Ergebnisse übernehmen“ (gespielte Spiele mit gleicher Paarung). Knopf „Phase 2
      weiterführen“ mit Warn-Dialog (Weiter rechts, Abbrechen links): alte Phase → Reiter „Phase 2 – deaktiviert“, ihre Spiele
      in der Spielübersicht markiert und aus der Ergebnis-XML entfernt, laufende Spiele zurückgerufen. Kernpunkt: Spiele in
      abgeschlossenen Phasen bleiben bearbeitbar; Knopf „Korrektur verwerfen“ (Nutzer 12:09/12:11) verwirft nur die
      korrigierte Phase (Reiter verschwindet); die Korrektur in der Vorphase BLEIBT, laufende Phase läuft unverändert. Nur ein Korrektur-Reiter (rechnet jede weitere Korrektur ein),
      aber mehrere „deaktiviert“-Reiter möglich; Reiterleiste mit höchstens 6 Reitern je Seite und Pfeilen (Seite 2 = Reiter
      7–12 usw.). Auswirkung NUR auf die direkt folgende Phase: Korrektur in Phase 1 bei laufender Phase 3 ändert nur das
      Ergebnis (kein Einfluss auf den Verlauf); Korrektur in Phase 2 → Prüfung + Korrektur-Reiter für Phase 3.
      Reiterleiste: alle Reiter der Klasse zählen zu den 6 je Seite. Design (bestätigt 20.11.2026):
      https://claude.ai/artifact/NQBy8vHntVYCjfKsGmP5LT

1.2.0 (19.11.2026, Commit „HTS 1.2.0“, Closes #94–#100): neue Plan-Engine Plan.java (Modus TournamentMode.PLAN,
   Knoten mit Quellen S#/W:/L:/-, Bedingung IFB: für den Stichkampf, Speichern als <plan> mit plan-section/plan-node/
   plan-seed/plan-result), Tournament.createPlanPhase/createTieredPhase/drawDoubleKo/drawBadeni, planState/resolvePlan/
   rankPlan, finalResultSections (getrennte Wertungen), BadeniSystem.java, MAX_PHASES = 3 (Phase 3 nur nach
   Leistungsstufen), Gruppennamen fortlaufend (groupLabel/groupNameOffset). UI: PhaseCloseDialog (enum K), ModeDialog
   (Badeni, Doppel-KO), DrawView.buildPlan, Reiter je Wertung („Name#g“), Endergebnis-Auswahl, Präsentation, PDF,
   Live-Seiten. Import fertiger Turniere lehnt Nicht-KO-Felder ab (doppeltes KO). Tests Version120Test (15), Fixture
   turnierdatei-1.2.0.hts, Belastungstest mit allen neuen Typen. Umgesetzte Punkte (Wortlaut der Feature-Sammlung):

   4. PHASE 2 „HAUPT- UND TROSTRUNDE“ (Nutzer 18.11.2026, Issue #94, für 1.2.0): Nur nach Phase 1 = Gruppenphase.
      Beim Erstellen von Phase 2 festlegen, welche Gruppenplätze in die Hauptrunde (KO) und welche in die Trostrunde
      (KO) kommen (z. B. 1–2 Haupt, 3–4 Trost); je Runde Haken „Spiel um Platz 3“.
      - KO-Bäume wie gewohnt: Freilose, Setzung/Auslosung (Gruppensieger gesetzt, Gruppengleiche getrennt), Drag & Drop,
        Anzeige und Drucken (wie createNextPhase(KNOCKOUT)).
      - ZWEI REITER „Phase 2 – Hauptrunde“ und „Phase 2 – Trostrunde“, je eigener Baum, Drucken, Starten; beide parallel
        an den Tischen.
      - ÄNDERUNG (Nutzer 18.11.2026, 20:26): GETRENNTE ENDERGEBNISSE in der Klasse – „Endergebnis Hauptrunde“ (1., 2., …)
        und „Endergebnis Trostrunde“ (eigene Zählung 1., 2., …); eigene Reiter bzw. Abschnitte, auch im PDF
        „Gesamtergebnis“ und in der Präsentationsansicht. (Ersetzt „Trostrunde zählt anschließend weiter“.)
      - click-TT-Export: Spiele beider Bäume; Platzierung in click-TT (nur eine Zahl je Person): Hauptrunde wie ihr
        Endergebnis, Trostrunde fortlaufend dahinter – BESTÄTIGT (Nutzer 18.11.2026): Trostsieger = Anzahl der
        Hauptrunden-Teilnehmenden + 1 (16 in der Hauptrunde → 17., 32 → 33. usw.), weitere Trostplätze entsprechend.

   5. PHASE-1-TYP „BADENI (SCHWEIZER SYSTEM)“ (Nutzer 18.11.2026, Issue #95, für 1.2.0; Unterlage
      docs/referenz/Badeni-Schweizer-System.pdf, BaTTV/Enichlmayr 2014): Option für Phase 1 (Einzel; wie Schweizer
      System kein Doppel).
      - Grundlage Schweizer System: alle in EINEM Feld; je Runde Siegegruppen (gleiche Anzahl Siege), in jeder Gruppe
        wird ausgelost. Runde 1: alle nach TTR sortiert.
      - REIHUNG: 1. Siege, 2. TTR, 3. Sätze (KEINE Buchholz/Feinbuchholz, Bälle nicht genannt).
      - AUSLOSUNG „Skisprung“: in jeder Siegegruppe (nach Reihung) Erster gegen Letzten, Zweiter gegen Vorletzten …,
        die Mitte gegeneinander (30 Spieler: 1–30, 2–29 … 15–16).
      - FREILOS bei ungeradem Feld: der beste Spieler der unteren Hälfte der Reihung; niemand zweimal (sonst der
        nächste); Freilos farbig markiert.
      - Mindestens 5 Runden.
      - ANNAHMEN (Nutzer hat nur die Regeln oben bestätigt, Rest offen gelassen – so umsetzen und dem Nutzer sagen):
        ungerade Siegegruppe → TTR-Schwächster rutscht in die nächstniedrigere Gruppe; Wiederholungen vermeiden
        (nächstmöglicher Gegner, nur wenn unvermeidbar wiederholen); Standard 5 Runden, änderbar, plus „Weitere Runde“
        als Platzierungsrunde; „Spieler pausieren lassen“ je Runde (nicht ausgelost, Stand bleibt).
      - KEINE PHASE 2 (Nutzer 18.11.2026, 21:02): Badeni UND Schweizer System haben keine Folgephase – das Endergebnis
        ist die Reihung nach der letzten Runde. Für das Schweizer System sperrt HTS das schon heute
        (createNextPhase: „Nach dem Schweizer System gibt es keine weitere Phase.“) – für Badeni genauso umsetzen.

   6. PHASE 2 „FORTGESETZTES K.-O.-SYSTEM“ (WO 7.3; vorher „modifiziertes KO-System“, umbenannt 18.11.2026; Issue #96,
      für 1.2.0): NUR als Phase 2 nach Gruppenphase.
      Wie einfaches KO, aber Verlierer bestimmter Runden spielen gegen die in derselben Runde Unterlegenen um die Plätze
      des Gesamtfeldes (Halbfinal-Verlierer um 3–4, Viertelfinal-Verlierer um 5–8, Achtelfinale um 9–16 …).
      - Beim Erstellen wählbar: „Alle Plätze ausspielen“ oder „ab Achtelfinale / Viertelfinale / Halbfinale“; Verlierer
        früherer Runden teilen sich den Platz (z. B. 17.–32.).
      - Aufsteiger je Gruppe wie normales KO; Freilose, Setzung, Drag & Drop, Anzeige, Drucken wie gewohnt (auch in den
        Platzierungsbäumen, dort ggf. Freilose, wenn weniger Verlierer).
      - EIN Reiter „Phase 2 – Fortgesetztes K.-o.-System“: oben Hauptbaum, darunter Bäume „Platz 3–4“, „Platz 5–8“, „Platz 9–16“ …;
        Drucken gemeinsam oder je Baum.
      - Endergebnis und click-TT-Export mit den ausgespielten Plätzen.

   7. PHASE 3 (Nutzer 18.11.2026, Issue #97, für 1.2.0): Eine Klasse kann eine 3. Phase haben, wenn Phase 2 ebenfalls
      eine Gruppenphase (GROUPS) ist. Bisher Tournament.MAX_PHASES = 2 (canHaveNextPhase: nur nach GROUPS).
      ANNAHMEN (dem Nutzer gesagt): Phase 3 bietet dieselben Arten wie Phase 2 nach Gruppen (KO, Gruppen,
      Qualifikation, Haupt-/Trostrunde, fortgesetztes KO); höchstens 3 Phasen (MAX_PHASES = 3, Phase 4 nicht);
      Endergebnis: Phase 3 vorne, übrige nach Platz in Phase 2, dann Phase 1; Reiter „Phase 3 – …“, PDF, Präsentation,
      click-TT-Export (Spiele aller Phasen; Ergebnis-Import liest weiter nur Phase 1) wie bei Phase 2; Turnierdateien
      kompatibel (Phasen-Kette über source/follow-up).
      EINSCHRÄNKUNG (Nutzer 18.11.2026, 21:18): Phase 3 NUR nach Phase 2 = „Gruppen nach Leistungsstufen“ (nicht nach
      einer normalen Gruppenphase). Phase 3 bietet vorerst NUR die Art „Endrunde je Gruppenplatz“ (Nutzer 21:22) – die
      Annahme „dieselben Arten wie Phase 2“ ist damit hinfällig.
      ERWEITERUNG (Nutzer 18.11.2026, Vorbild Top-48-Bundesranglistenturnier Jugend 19 Wiesbaden, Ergebnis-PDF
      https://hamburg-tischtennis.de/shared-files/5093/?Ergebnisse.pdf; 48 TN, Phase 1 = 8 Gruppen A–H zu 6):
      a) Phase 2 „GRUPPEN NACH LEISTUNGSSTUFEN“: einstellbar Stufen aus Vorrundenplätzen (1–2 → Gruppen 01–04, 3–4 →
         05–08, 5–6 → 09–12) und Gruppengröße (4); alle kommen weiter. Gruppenbildung gespiegelt (A+H, B+G, C+F, D+E:
         Gruppe 01 = A1, A2, H1, H2) als Standard, wahlweise ausgelost (ANNAHME, Nutzer nicht festgelegt). KEINE Übernahme
         von Ergebnissen (A1 spielt erneut gegen A2). Gruppennamen fortlaufend (01–12 bzw. nach A–H weiter I–T).
      b) Phase 3 „ENDRUNDE JE GRUPPENPLATZ“: Gleichplatzierte der 4 Gruppen einer Stufe = 4er-Block; Halbfinale Gruppe
         1.–4. und 2.–3. (z. B. 01/1–04/1, 02/1–03/1), dann Spiel um Platz x und x+2; Platzbereiche automatisch: Stufe 1
         Erste 1–4, Zweite 5–8, Dritte 9–12, Vierte 13–16; Stufe 2 17–32; Stufe 3 33–48. 12 Blöcke × 4 Spiele = 48.
         Endergebnis lückenlos 1–48 (click-TT ebenso).

   8. PHASE 2 „TOP24-PLATZIERUNGSSPIELE“ (Nutzer 18.11.2026, Issue #98, für 1.2.0; Vorbild Top-24-Bundesranglisten-
      turnier Jugend 19/15 Ettlingen 02/2024, PDF erg_jug-23-24-DTTB-TOP24-ergebnisse-1.pdf; alle 4 Konkurrenzen gleich):
      Nach Phase 1 = 4 Gruppen A–D zu 6 spielen alle 24 in 44 Spielen die Plätze 1–24 aus. Kein reines 7.3 (Verlierer
      der oberen Stufe treffen auf SIEGER der nächsten Stufe = gestaffelter Einstieg).
      - Runde 1 (Sp. 1–12): 1–2 über Kreuz B1–A2, A1–B2, D1–C2, C1–D2; 3–4: D3–C4, C3–D4, B3–A4, A3–B4; 5–6: D5–C6,
        A5–B6, B5–A6, C5–D6.
      - HF (21, 22): W1–W3, W2–W4; Finale 33, Platz 3 34.
      - Runde 2: 13 L4–W7, 14 L1–W5, 15 L2–W6, 16 L3–W8; 17 W10–L5, 18 W12–L7, 19 W11–L6, 20 W9–L8.
      - Platz 5–8: 23 W13–W16, 24 W14–W15 → 35 (Pl. 5), 36 (Pl. 7); 9–12: 25 L13–L15, 26 L14–L16 → 37, 38;
        13–16: 27 W17–W19, 28 W18–W20 → 39, 40; 17–20: 29 L17–L19, 30 L18–L20 → 41, 42; 21–24: 31 L9–L10,
        32 L11–L12 → 43, 44.
      - Intern als Platzierungsplan aus Vorlage (je Spiel Herkunft „Gruppe B Platz 1“ / „Verlierer Spiel 4“, Plätze für
        Sieger/Verlierer) – später weitere Vorlagen; evtl. Basis für #96/#97.
      - Anzeige wie KO-Baum mit Platzierungsblöcken, Drucken, Präsentation; Endergebnis/click-TT lückenlos 1–24.
      - WÄHLBAR (Nutzer 18.11.2026, 21:32): der Einfachheit halber IMMER nach einer Gruppenphase in Phase 1.
        ANNAHME für abweichende Felder: der Plan nutzt Gruppen A–D und Plätze 1–6; fehlende Gruppenplätze =
        Freilos; weitere Gruppen bzw. Plätze ab 7 spielen nicht mit und werden im Endergebnis nach Gruppenplatz dahinter
        eingereiht (Hinweis beim Erstellen der Phase 2).
   9. DOPPELTES K.-O.-SYSTEM (WO 7.4) (Nutzer 18.11.2026, Issue #99, für 1.2.0): Ausscheiden erst nach der zweiten
      Niederlage, bis einschließlich Endspiel; Verlierer in die Trostrunde mit „Kreuzen“ (wiederholte Begegnungen
      weitgehend vermeiden, sonst trotzdem austragen); haben nach dem Endspiel beide Finalisten je eine Niederlage →
      Stichkampf. Turnierliste/Freilose wie WO D 7.2.
      ANNAHMEN: wählbar wie einfaches KO (Phase 1 direkt oder Phase 2 nach Gruppen); Siegerrunde und Trostrunde als zwei
      Bäume in einem Reiter; Endergebnis 1., 2., 3., weitere nach Ausscheiderunde in der Trostrunde (geteilt); click-TT
      mit allen Spielen.

   10. PHASE 2 „TOP32-PLATZIERUNGSSPIELE“ (Nutzer 18.11.2026, Issue #100, für 1.2.0) – EIGENE, GETRENNT WÄHLBARE OPTION
      neben TOP24 (Nutzer 21:38: keine automatische Wahl nach Gruppengröße).
      Vorbild Top 32 Jugend 13 Bergisch Gladbach,
        docs/referenz/TOP32-Ergebnisse-Jugend13.pdf: 4 Gruppen zu 8, 60 Spiele, Plätze 1–32; gleiches Muster plus
        4. Stufe: Runde 1 Sp. 13–16 = Plätze 7–8 (D7–C8, A7–B8, B7–A8, C7–D8); HF 29, 30 (W1–W3, W2–W4), Finale 45,
        Platz 3 46; Runde 2: 17 L4–W7, 18 L1–W5, 19 L2–W6, 20 L3–W8; 21 W10–L5, 22 W12–L7, 23 W11–L6, 24 W9–L8;
        25 W13–L10, 26 W14–L11, 27 W15–L9, 28 W16–L12; Blöcke: 5–8 = 31 W17–W20, 32 W18–W19 → 47/48; 9–12 = 33
        L17–L20, 34 L18–L19 → 49/50; 13–16 = 35 W21–W23, 36 W22–W24 → 51/52; 17–20 = 37 L21–L23, 38 L22–L24 → 53/54;
        21–24 = 39 W25–W27, 40 W26–W28 → 55/56; 25–28 = 41 L25–L27, 42 L26–L28 → 57/58; 29–32 = 43 L13–L15,
        44 L16–L14 → 59/60.
      - Wählbar wie TOP24: immer nach einer Gruppenphase in Phase 1; Plan nutzt Gruppen A–D und Plätze 1–8; fehlende
        Gruppenplätze = Freilos; weitere Gruppen bzw. Plätze ab 9 spielen nicht mit und stehen im Endergebnis nach
        Gruppenplatz dahinter (Hinweis beim Erstellen). Intern dieselbe Platzierungsplan-Vorlage wie TOP24.


1.1.0-NACHTRAG: LINUX- UND MACOS-PAKET (Issue #90): FÜR 1.1.0 NACHGELIEFERT (15.11.2026) als hts-1.1.0-linux.tar.gz und
   hts-1.1.0-macos.tar.gz (Dateien vom Nutzer: Temurin 21.0.13+9 JRE Linux x64 / macOS aarch64, JavaFX 21.0.8 linux /
   mac-aarch64; Ablage $S/jx und Anhänge). Bau: $S/fat-linux bzw. $S/fat-mac (= fat ohne *.dll + JavaFX-Jars der
   Plattform) → hts.jar; Ordner $S/dist/hts-linux|hts-macos mit jre, „HTS starten.sh“ bzw. „HTS starten.command“
   (liegen jetzt in dist/ im Quellcode), Doku, Lizenzen (LIESMICH aktualisiert: AGPL, voller Name – Vorlage für
   Windows in dist/Lizenzen-Vorlage, Windows-ZIP 1.1.0 neu gepackt). Linux hier getestet (startet), macOS ungetestet. AB JETZT bei jedem
   „kompilieren“ alle drei Pakete bauen; Release-Link: Download-Hinweis für alle drei. Issue #90 geschlossen (Commit d314a25).

1.1.0 (15.11.2026, Issues #83–#89, #91, #93). README „Neu in 1.1.0“.
Fehlerbehebungen aus dem Belastungstest (vorher „1.0.1“): #83 Tournament.rankKnockout zählt Spiele je Runde; #84
ClickTtResultExporter.withoutId nur ohne click-TT-Nummer; #85 TablesView (Warteschlange max. 100 + Hinweis, FlowPane,
Umbruch), Tournament.entrantName per Index/playerById-Cache, Ui.fitColumns misst jeden Text einmal; #87 DrawView
Verein/Tischname umbrechen (Tischname min = Vorzugsbreite bis 130). Zusätzlich: WorkspaceView.HeaderRow (Knöpfe oben
rechts bei Platzmangel in eigener Zeile, vorher Überlagerung von „Gestartet …“ bei 911 px), Spalte „Bezahlt“ mit
fit-min. Doku: click-TT-Upload-Einschränkung gestrichen.
#86 Tournament.displayedStart/startText („ab 14:25“): DrawView Rundenlabel und KO-Rundentitel, PdfReports (Gruppen
„Runde k · ab … Uhr“, KO-Überschrift, sonst Spalte Runde), PresentationShow Warteschlange.
#88 PlayingClass.scheduleStart (Datei: schedule-start), Tournament.scheduleStartOf/nearestTime/setScheduleStart,
ClassDialog Feld „Startzeit“ (auch bei gesperrter Klasse: „Startzeit speichern“), resumeClass gibt Runde 1 bei
Startzeit in der Zukunft nicht frei.
#89 Gebühr nur Einzelklassen (feeClassesOf/hasFees ohne isDoubles), markPaid → angemeldet, Payment.WAIVED +
waivedReason (Datei: paid="waived", waived-reason), waiveFee, paymentPending, availableForClasses, presentUnpaid,
takeOverPresentEntries nur Bezahlte; PlayerTableView setPendingMark (hellblau fee-pending-row, Hinweis mit „Nur diese
zeigen“), ClassesView feeHint + „Bezahlt – bar und in die Klasse“, FeeWaiveDialog, Kassenübersicht „erlassen (nicht
in der Summe)“, Menütexte „Bezahlt – bar/Überweisung“, „Gebühr erlassen“, „Zahlung rückgängig“.
#91 TouchSupport (Stilklasse „touch“ bei Fingerbedienung, langes Antippen = Kontextmenü), hts.css .touch-Regeln.
#93 Tournament phoneResults/phoneResultsDirect (Datei), tableResultToken, submitPhoneResult, Posteingang;
LiveServer /ergebnis/<Schlüssel>/<Tisch>; Mobile Einstellungen (ausgeblendet): Optionen + „QR-Codes für die Tische
drucken“ (PdfReports.tableQrSheet); WorkspaceView.askPhoneResults.
Tests: Version110Test, PhasesTest.koPlatzbereicheNachTatsaechlichAusgeschiedenen; Beispieldatei turnierdatei-1.1.0.hts.
Belastungstest (60 Klassen) nach den Änderungen ohne Probleme.

Feature-Sammlung 1.1.0 (zur Nachverfolgung):
1. KO-Endergebnis: Platzbereiche nie größer als die Teilnehmerzahl (120 im 128er-Feld: 65.–120. statt 65.–128.)
   (Issue #83; Tournament.rankKnockout zählt die Spiele je Runde; Test PhasesTest.koPlatzbereicheNachTatsaechlichAusgeschiedenen).
2. click-TT-Export: am Turniertag zugeloste Doppel nicht mehr als „von Hand nachgetragen“; withoutId nur für Personen
   ohne click-TT-Nummer, sonst Grund „Doppel so nicht in click-TT gemeldet“ / „in click-TT nicht für diese Konkurrenz
   gemeldet“ (Issue #84).
3. Große Turniere: „Tische“ 11 s → 0,5 s (Warteschlange zeigt die nächsten 100 + Hinweis), „Spiele“ 6 s → 0,2 s
   (entrantName per Index, Ui.fitColumns misst jeden Text einmal mit wiederverwendetem Messknoten); „Tische“ bei
   1093×614/911×512 ohne „…“ (FlowPane, Umbruch) (Issue #85).
4. Gruppenübersicht: lange Vereinsnamen (z. B. „TuS Germania Schnelsen“) und lange Tischnamen in der Spieleliste
   umbrechen statt „…“ (Issue #87, Hinweis aus Screenshot des Nutzers 15.11.2026; DrawView club/tableInfo).
2. ZEITPLAN: STARTZEITEN IN DER PHASENÜBERSICHT (Nutzer 15.11.2026, Issue #86, für 1.1.0): Bei Klassen „mit
   Zeitplan“ steht in der Spieleliste der Phasenübersicht hinter der Rundennummer die geplante Startzeit, z. B.
   „R2 · ab 14:25“ (Status rechts bleibt). Vor dem Start der Klasse KEINE Zeit (erst ab Start echte Uhrzeiten, aus
   Tournament.plannedStart). Zusätzlich im PDF der Phase (Gruppen-/KO-Übersicht), auf den Gruppenaushängen
   (Knopf „Drucken“ je Gruppe, PdfReports.group) und in der Präsentationsansicht – auf den Gruppen-/KO-Seiten
   ebenso wie in der Warteschlange je Klasse (Nutzer 15.11.2026, 21:03). Nicht auf dem Spielzettel.
3. ZEITPLAN: STARTZEIT EINSTELLBAR (Nutzer 15.11.2026, Issue #88, für 1.1.0): In „Klasse anlegen/bearbeiten“ bei
   „mit Zeitplan“ zusätzlich eine Startzeit (Uhrzeit, bis 12 Stunden vor oder nach jetzt); Runde n frühestens
   Startzeit + (n − 1) × Rundendauer (PlayingClass.scheduleAnchor statt Startzeitpunkt der Klasse).
   - Feld leer: wie bisher ab dem Start der Klasse.
   - Startzeit in der Zukunft, Klasse vorher gestartet: Runde 1 wartet bis zur Startzeit (wartende Runde mit
     „Runde jetzt starten“ vorziehbar, automatische Einreihung zur Startzeit).
   - Startzeit in der Vergangenheit: fällige Runden laufen nacheinander wie gewohnt, ohne Wartefrage.
   - Auch nach dem Start änderbar (z. B. Verzögerung): noch nicht gestartete Runden bekommen neue Zeiten, laufende
     bleiben.
4. STARTGEBÜHREN ÜBERARBEITEN (Nutzer 15.11.2026, Issue #89, für 1.1.0; Design freigegeben:
   „HTS – Bezahlt, angemeldet, erlassen“ https://claude.ai/artifact/JqDwZ3NVcuJ4Yjc94VjHrF, Ordner $S/bezahlt):
   - Gebühr je Person = Summe der EINZELklassen (click-TT competition-Gebühr); Doppel ohne Gebühr; ggf. + Nachmeldung.
   - „Bezahlt“ (bar/Überweisung) oder „erlassen“ setzt automatisch „angemeldet“ (Attendance PRESENT). Angemeldet ≠
     bezahlt (angemeldet = technisch, z. B. click-TT-Export; bezahlt = organisatorisch).
   - Klasse → „Verfügbar“: VOR Turnierstart nur Bezahlte/Erlassene (Hinweis „n Angemeldete haben noch nicht bezahlt
     …“; „Alle anwesenden Gemeldeten übernehmen“ nimmt nur Bezahlte). NACH Turnierstart auch Angemeldete ohne
     Zahlung, hellblau markiert (#e8f7f9 / Rand #a8dde4 / Text #0f4552 / Kennzeichnung „offen n,00 €“ #1d6b7a) und
     übernehmbar; Knopf „Bezahlt – bar und in die Klasse“. In der Klasse bleiben Unbezahlte markiert.
   - Regel nur, wenn das Turnier Gebühren hat bzw. die Person etwas zu zahlen hat (sonst wie bisher „angemeldet“).
   - Teilnehmende nach Turnierstart: Angemeldete ohne Zahlung hellblau hinterlegt, Kennzeichnung „offen“; hellblauer
     Hinweis (KEIN Warngelb) mit Anzahl und Knopf „Nur diese zeigen“; Rechtsklick/Aktion: „Bezahlt – bar“, „Bezahlt –
     Überweisung“, „Gebühr erlassen“ (ohne „…“), „Zahlung rückgängig“; Summenzeile bezahlt · erlassen · offen (Betrag).
   - Neuer Zahlungsstand „erlassen“ (Player.Payment.WAIVED) mit optionalem Grund (erscheint in der Kassenübersicht);
     Fenster „Gebühr erlassen“ zeigt Gebühren je Klasse und Summe; Hinweis hellblau „Gilt wie bezahlt …“.
   - „Zahlung rückgängig“: angemeldet und Klassen bleiben unverändert.
6. BEDIENUNG AUF WINDOWS-TABLETS (Nutzer 15.11.2026, Issue #91, für 1.1.0): Touch: Bedienelemente ≥ ca. 44 px,
   Ziehen per Finger (Teilnehmende, Gruppen, Tische), Kontextmenü per langem Antippen, Wischen zum Blättern,
   Bildschirmtastatur verdeckt keine Eingaben.
8. ERGEBNISSE PER HANDY EINTRAGEN (Nutzer 15.11.2026, Issue #93, für 1.1.0 – EINBAUEN, ABER AUSGEBLENDET wie der
   ganze Handy-Zugang, HtsApp.MOBILE_ENABLED = false): in „Mobile Einstellungen“ Option „Ergebnisse per Handy
   eintragen“ (Standard aus); je Tisch QR-Code mit geschütztem Link; Handy zeigt das aktuelle Spiel des Tisches,
   Satzergebnisse mit derselben Prüfung wie in HTS; in HTS „Ergebnis vom Handy“ zur Bestätigung (übernehmen/
   verwerfen), Option „direkt übernehmen“.

1.0.0 (Issues #77–#82). README „Neu in 1.0.0“.
#80 HtsApp.MOBILE_ENABLED = false (Menüpunkt weg, Code bleibt). #78 Tournament.adoptManualPersons in
mergeImportedEntries (Player.hasClickTtNumber, samePerson: Vor-/Nachname, Verein/Jahrgang soweit bei beiden).
#79 Export: zwei Knöpfe (startBox jetzt FlowPane), Trockenlauf → ClickTtExportDialogs.askMissing (Abbrechen / Ohne
Abgleich speichern / Aktualisierte Teilnehmerliste einspielen), Tournament.matchManual + Zuordnungsfenster (Karten,
erledigte klappen zu mit „Ändern“, gewählte Personen verschwinden aus den übrigen, „Zuordnung lösen“), adoptNumbers +
neue Datei als Export-Grundlage; Bericht mit Tabelle der nicht exportierten Spiele (Report.notExported/withoutId),
PDF PdfReports.notExported. #81 PresentationPlan.Kind.QUEUE („queue:<Klasse>“, standardmäßig aus),
Tournament.presentationNextCount (pres-next, 4–48), PresentationShow: Gruppen je Seite + Warteschlange dieser Gruppen,
sonst nach der Phase; Seiten zu 16 Spielen. #82 PlayingClass.feeCents (fee-cents, aus click-TT entry-fee),
Tournament.lateFeeCents (late-fee-cents, TournamentDialog.withLateFee), Player.Payment/paidCents/paidAt/paidByClub,
feeOf/markPaid/unpaidIn, Spalten „Bezahlt“/„Gebühr“, Menüs, ClubSettlementDialog, PdfReports.receipt/cashReport,
Warnung beim Klassenstart. #77 PlayingClass.scheduleMinutes/Anchor/releasedRounds/askedRounds, Gating in queueReady
(roundOpen), ScheduleQuestion + WorkspaceView-Timer (2 s Rückfragen, 20 s fällige Runden), DrawView-Hinweis
„Runde jetzt starten“, Simulation ignoriert den Zeitplan, Neustart der Phase setzt ihn zurück. Beispieldatei
turnierdatei-1.0.0.hts. Tests: 226 bestanden (Version100Test). Bildschirmfotos 1093×614 und 911×512.
Feature-Sammlung 1.0.0 (zur Nachverfolgung):
2. FÜR 1.0.0 (Nutzer, 15.11.2026) – KLASSE „MIT ZEITPLAN“ (15.11.2026, Issue #77): Beim Anlegen einer Klasse Option „mit Zeitplan“ und Angabe, wie lange eine
   Runde dauert. Ist eine Runde (KO-Runde, Runde der Qualifikation, Runde der Platzierungsspiele/Top 4, Runde einer
   Gruppe) beendet und man ist noch VOR dem Zeitplan, fragt HTS kurz, ob die nächste Runde trotzdem gestartet werden
   soll; die Rückfrage nennt Klasse und betroffene Gruppe/KO-Runde/Qualifikationsrunde. ENTSCHIEDEN (Nutzer,
   15.11.2026): Zeitplan ab Start der Phase, Runde n geplant ab Start + (n−1) × Rundendauer. „Warten“ hält die Spiele
   der nächsten Runde zurück und reiht sie zur geplanten Uhrzeit automatisch ein (vorher per „Runde jetzt starten“
   möglich). Bei Gruppen: Rückfrage je Gruppe einzeln (jede Gruppe sofort eine eigene Rückfrage). Hinter dem Zeitplan
   keine Rückfrage. VERSION: 1.0.0.
3. FÜR 1.0.0 (Nutzer, 15.11.2026) – BUG #78 (15.11.2026, Frage des Nutzers zu click-TT): VON HAND
   NACHGETRAGENE PERSON WIRD BEIM ERNEUTEN IMPORT DOPPELT.
   Nachgestellt: Person in HTS von Hand erfasst und gespielt, danach in click-TT nachgemeldet und die neue click-TT-Datei
   in HTS importiert → die Person steht zweimal in HTS (Schlüssel „name:…“ gegen „id:NU…“), der Export ordnet die
   click-TT-ID dem leeren Doppel zu, die Spiele fehlen weiter. Ohne erneuten Import, nur mit der neuen Datei als
   Export-Grundlage, werden alle Spiele übertragen (Test: 6 statt 3 Spiele, 0 Warnungen).
   Vorschlag (Rückfrage gestellt): beim Import Personen ohne click-TT-Nummer über Vorname + Nachname + Jahrgang
   (Verein, falls bei beiden vorhanden) zuordnen und die Nummern übernehmen statt doppelt anzulegen; dazu im
   Exportbericht je fehlender Person den Hinweis „in click-TT nachmelden, neue Datei importieren“.
4. FÜR 1.0.0 (Nutzer, 15.11.2026) – SPIELERABGLEICH VOR DEM CLICK-TT-EXPORT (15.11.2026, Idee des
   Nutzers, Issue #79, baut auf Punkt 3/#78 auf):
   - Statt Dropdown „Ergebnisse exportieren ▾“ zwei Knöpfe im Standard-Design: „Gesamtergebnis als PDF“ und
     „click-TT-Ergebnisdatei (XML)“ – immer sichtbar, deaktiviert, bis das Turnier gespielt ist.
   - Klick auf „click-TT-Ergebnisdatei“: wie bisher Speicherort wählen – ODER, wenn Teilnehmende von Hand nachgetragen
     wurden, vorher eine aktualisierte XML-Teilnehmerliste aus click-TT einspielen (Spielerabgleich).
   - Matching: Nachgetragene ↔ Personen der neuen Datei über Vor-/Nachname und Verein. Nicht eindeutig zuordenbare
     neue Personen der Datei: Zuordnung von Hand; passt nichts, Aktualisierung abbrechen.
   - Für abgeschlossene Turniere alle nicht exportierten Spiele anzeigen (wegen Nachtragung oder bewusst ausgelassen).
   - Designvorschlag (15.11.2026) als Design-Artefakt „HTS – Spielerabgleich vor dem click-TT-Export“
     (https://claude.ai/artifact/E6VN38zGbxmgJGFXFTiZEA): 1 Export-Knöpfe (deaktiviert/aktiv), 2 Rückfrage beim Klick
     (Abbrechen / Ohne Abgleich speichern / Aktualisierte Teilnehmerliste einspielen), 3 Zuordnungsfenster (Bitte
     zuordnen mit Kandidaten und Grund, „keine Zuordnung“; Eindeutig zugeordnet mit „Zuordnung lösen“; Neu in der
     Datei mit „zuordnen zu“; Fußzeile: links „Abbrechen – nichts ändern“, rechts „Übernehmen und Speichern“, OHNE
     Text zur Spielezahl – so vom Nutzer gewünscht), 4 Nicht exportierte Spiele (Grund je Spiel). Vom Nutzer als guter
     Vorschlag angenommen (15.11.2026). Ergänzt: KEINE Suche in der Datei. „Neue Personen“ = Personen der Datei, die
     keinem bekannten Teilnehmenden zugeordnet werden konnten. Ähnlichkeit (Vor-/Nachname, Verein) NUR für das
     automatische Matching; bei der Zuordnung von Hand stehen ALLE noch nicht zugeordneten neuen Personen zur Wahl
     (ohne Ähnlichkeits-Begründung, mit gemeldeter Klasse) plus „keine Zuordnung“. Bereich 3 „Neu in der Datei, nicht
     zugeordnet“. Entwurf 3b (mehrere offen): je nachgetragener Person eine Karte untereinander; erledigte Karten
     klappen zu einer Zeile „✓ … ⟷ …“ mit „Ändern“; eine schon gewählte neue Person verschwindet aus den übrigen
     Karten; Fortschritt „1 von 3 erledigt“; „Übernehmen und Speichern“ erst aktiv, wenn jede Karte eine Wahl hat
     (Person oder „keine Zuordnung“), daneben „Noch n Zuordnungen offen“. DESIGN VOM NUTZER FREIGEGEBEN (15.11.2026,
     „passt“) – so umsetzen (Entwürfe 1, 2, 3, 3b, 4 im Design-Artefakt). Version: 1.0.0.
5. FÜR 1.0.0 (Nutzer, 15.11.2026) – REITER „MOBILE EINSTELLUNGEN“ AUSBLENDEN: Der Menüpunkt verschwindet; der Code
   (Handy-Zugang, Live-Anzeige, Anmeldung per QR-Code, QR-Aushang) bleibt im Programm und wird später ausgebaut.
   Vorschlag: ein Schalter im Code (z. B. HtsApp.MOBILE_ENABLED = false); ohne Reiter läuft kein Server, damit
   verschwinden auch „Handyabmeldungen bearbeiten“ und Hinweise auf den Handy-Zugang. Eine in älteren Turnierdateien
   gespeicherte Handyabmeldung bleibt bearbeitbar. Doku/README: Handy-Zugang als „derzeit ausgeblendet“ kennzeichnen.
6. FÜR 1.0.0 – PRÄSENTATIONSANSICHT: WARTESCHLANGE JE KLASSE (15.11.2026): Auf der Seite „Präsentationsansicht“ je
   laufender Klasse ein zusätzlicher Haken „Warteschlange dieser Klasse“ (zeigt nur deren eingereihte Spiele). „Die
   nächsten 16 Spiele“ heißt künftig „Die nächsten Spiele (alle Klassen)“ mit einstellbarer Anzahl (gilt nur für die
   klassenübergreifende Warteschlange). UI-Vorschlag VOM NUTZER FREIGEGEBEN (15.11.2026, „passt so“): Design-Artefakt „HTS – Präsentationsansicht:
   Warteschlange je Klasse“ (https://claude.ai/artifact/DLWqnNadSL3fnWfvsxXEaw): Anzahl 4–48, Standard 16, mehr als 16 →
   mehrere Seiten; Klassen-Warteschlange standardmäßig aus, alle eingereihten Spiele der Klasse, bei Bedarf mehrere
   Seiten, nur bei laufender Klasse; Anzahl wird im Turnier gespeichert.
   KORREKTUR (Nutzer, 15.11.2026) – Reihenfolge JE SEITE MIT GRUPPEN: Auf jede Beamer-Seite mit Gruppen folgt direkt die
   Warteschlange genau dieser Gruppen (z. B. Seite Gruppen A–D → Warteschlange Gruppen A–D → Seite Gruppen E–H →
   Warteschlange Gruppen E–H → nächste Klasse). Schweizer System: Warteschlange der Phase nach ihren Seiten (vom Nutzer
   bestätigt). KO-System und Qualifikation ebenso: Warteschlange nach ALLEN Seiten der Phase (Nutzer, 15.11.2026). Haben mehrere Klassen den Haken, erscheinen ihre Warteschlangen so nacheinander.
7. FÜR 1.0.0 – STARTGEBÜHREN ERFASSEN (15.11.2026, Ideenaustausch, Issue #82, Stufe 1 – offline): Betrag je Person aus den gemeldeten
   Klassen (click-TT: competition entry-fee), Spalte „Bezahlt“ (bar / Überweisung) in „Teilnehmende“, passend zur
   Anmeldung am Turniertag; Abrechnung je Verein (Summe, Quittung als PDF); Kassenübersicht als PDF (bezahlt, offen,
   Summen bar / Überweisung); optional Nachmeldegebühr und Hinweis beim Klassenstart auf Unbezahlte. Keine Bankdaten der
   Teilnehmenden. HINWEIS: mit Quittungen/Tagesabschluss ggf. Kassensicherungspflicht (TSE) – vom Verein/Steuerberater
   klären lassen. Stufe 2 (Überweisungs-QR-Code) und 3 (Karte/PayPal) NICHT aufgenommen.
   ENTSCHIEDEN (Nutzer, 15.11.2026): FÜR 1.0.0. Zahlung je Person UND je Verein/Betreuer. Alle Zusatzregeln: Nachmelde-
   gebühr, Quittung als PDF (je Person bzw. Verein), Kassenübersicht als PDF, Hinweis beim Klassenstart auf Unbezahlte.
   Details ENTSCHIEDEN: Nachmeldegebühr = ein Betrag je Turnier (Dialog „Bearbeiten“ des Turniers), einmal je Person, für
   alle, die nicht in der click-TT-Datei standen. Startgebühr je Klasse als Feld in „Klasse anlegen/bearbeiten“, aus
   click-TT vorbelegt und änderbar. Beim Klassenstart nur Warnung mit Namen der Unbezahlten (Start bleibt möglich).



0.42.0 (Issues #71–#76). README „Neu in 0.42.0“.
#71 Sex.fromClickTt: 0 und 2 = weiblich. #75 Player merkt die Herkunft einer Klassenzuordnung (classOrigins:
registration / waiting-list, Turnierdatei: <entry from="…">); removeFromClass stellt Meldung bzw. Warteliste wieder her
(auch removePair, Doppel); erneuter Import setzt die Herkunft. Ältere Turnierdateien kennen die Herkunft nicht.
#74 Ui.growingSearch (min = Platzhalterbreite, max 450, HBox-Hgrow bzw. FlowPane-Restplatz) für Klassen (Verfügbar,
In der Klasse, Doppel), Spiele, Auslosung. #72 Datei-Kopf in allen 102 .java-Dateien (FileHeaderTest). #73 LICENSE =
AGPL-3.0-Text (choosealicense), NOTICE, README-Kopf, Info-Fenster (HtsApp.AUTHOR, InfoDialog.NOTICE), pom <licenses>,
LiveServer.SOURCE_FOOTER „Quellcode (AGPL-3.0)“; ZIP mit LICENSE.txt + NOTICE.txt. #76 Datei-Attribut hts-version,
Zusage im Javadoc von FORMAT_VERSION (bleibt 7, neue Angaben nur optional), Beispieldateien
src/test/resources/turnierdatei-0.41.0.hts (mit dem 0.41-Jar erzeugt) und -0.42.0.hts samt .properties
(TournamentFileCompatTest; Generator src/test/java/de/hts/persistence/CompatFixtureGenerator.java – bei JEDEM
„kompilieren“ mit der neuen Version eine weitere Datei turnierdatei-<Version>.hts/.properties ablegen).
Doku-Markdown: Lizenz, Dateien-Zusage, Zurückziehen (Claude-Docs-Dokument/PDF nicht neu erzeugt). Tests: 218 bestanden.
Bildschirmfotos 1093×614, 911×512, 1900×1000 (Klassen, Teilnehmende, Spiele).
Feature-Sammlung 0.42.0 (zur Nachverfolgung):
2. BUG (11.11.2026, Meldung des Nutzers mit Turnierteilnehmer_08.11.2026.xml): GESCHLECHT BEIM XML-IMPORT. Wird beim
   Import nur die Klasse „Mädchen 13 Einzel“ ausgewählt, stehen die Spielerinnen ohne Wert in Spalte „G“. Ursache:
   click-TT schreibt bei Spielerinnen sex="0" (Spieler sex="1"); Sex.fromClickTt kennt nur 1 = m und 2 = w, 0 wird
   „unbekannt“. Lösung: sex="0" als „w“ importieren (2 weiterhin als „w“ lesen). Test mit der Beispieldatei ergänzen.
3. DATEI-HEADER (11.11.2026): Am Anfang jeder .java-Datei ein Kommentarblock mit dem Copyright-Hinweis
   „Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany“. ENTSCHIEDEN (Nutzer, 11.11.2026): alle
   .java-Dateien in src/main und src/test (ca. 100), Block vor „package“, genau in dieser Form:
     /*
      * HTS – Hamburger Turniersoftware
      * Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
      *
      * SPDX-License-Identifier: AGPL-3.0-or-later
      * Dieses Programm ist freie Software unter der GNU Affero General
      * Public License v3.0; Einzelheiten in der Datei LICENSE.
      */
   Ein Test prüft, dass keine Datei ohne Header ist.
4. LIZENZ AUF AGPL ÄNDERN (11.11.2026): Lizenz von GPL-3.0 auf GNU Affero General Public License v3.0 umstellen.
   ENTSCHIEDEN (Nutzer, 11.11.2026): „AGPL-3.0 oder später“ (AGPL-3.0-or-later); Quellcode-Link auf den Handy-Seiten
   ja. LICENSE durch den AGPL-3.0-Text ersetzen; Info-Fenster, README, pom.xml
   (<licenses>), Dokumentation und Datei-Header anpassen. Wegen des Handy-Zugangs (Netzwerk-Klausel § 13) auf den
   Handy-Seiten unten einen Link „Quellcode (AGPL-3.0)“ zum GitHub-Projekt zeigen. Mitgelieferte Bibliotheken
   (PDFBox Apache 2.0, JavaFX/Temurin GPL 2 mit Classpath Exception) sind damit vereinbar. Bisher veröffentlichte
   Versionen bleiben unter GPL-3.0.
   LIZENZHINWEIS (11.11.2026, Text vom Nutzer, Tippfehler korrigiert, voller Name – ENTSCHIEDEN): neue Datei NOTICE
   (LICENSE bleibt reiner AGPL-Text, damit GitHub die Lizenz erkennt), derselbe Hinweis oben in der README und im
   Info-Fenster:
     HTS - Hamburger Turniersoftware is an application for executing tournaments
     Copyright (C) 2026  Richard Johan Frederich Michaelis, Hamburg, Germany

     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU Affero General Public License as
     published by the Free Software Foundation, either version 3 of the
     License, or (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU Affero General Public License for more details.

     You should have received a copy of the GNU Affero General Public License
     along with this program.  If not, see <https://www.gnu.org/licenses/>.
   Copyright-Name überall einheitlich „Richard Johan Frederich Michaelis, Hamburg, Germany“ (auch README-Kopf,
   Info-Fenster, Datei-Header #72).
5. SUCHFELDER WACHSEN MIT (11.11.2026, Screenshot Klassenansicht „Verfügbar“ / „In der Klasse“): Auf größeren
   Bildschirmen die Suchzeilen breiter machen. Dabei fiel auf: der Platzhalter „Suchen: Vorname, Nachname, Verein“ ist
   dort abgeschnitten („Suchen: Vorname, Na“) – verstößt gegen „Texte nie abkürzen“. Umsetzung: Suchfelder in
   Klassen (Verfügbar, In der Klasse, Doppelpaarungen), Spiele und Auslosung mindestens so breit wie der Platzhalter;
   wird die Zeile breiter, wächst das Suchfeld mit (Filter-Auswahl und „nur passende“ behalten ihre Breite), höchstens
   450 Bildpunkte (ENTSCHIEDEN, Nutzer 11.11.2026); wird es eng, bricht die Filterzeile um statt den Platzhalter zu kürzen.
6. BUG (11.11.2026): ZURÜCKZIEHEN NACH „VERFÜGBAR“ LÖSCHT DIE MELDUNG. Zieht man Teilnehmende von „In der Klasse“
   zurück nach „Verfügbar“, soll nur die Zuordnung zur Klasse entfallen – angemeldet (anwesend) und gemeldet bleiben.
   Geprüft mit Turnierteilnehmer_08.11.2026.xml: Die Anwesenheit bleibt erhalten, aber die MELDUNG geht verloren
   (Player.addToClass entfernt registrations/waitingLists, unassign stellt sie nicht wieder her). Folgen: Spalte
   „Klassen“ zeigt nicht mehr „gemeldet: …“, keine Markierung „gemeldet“ unter „Verfügbar“, kein Setzplatz, „Alle
   anwesenden Gemeldeten übernehmen“ holt die Person nicht mehr; ein Nachrücker verliert seinen Wartelistenplatz.
   ENTSCHIEDEN (Nutzer, 11.11.2026: „passt so“): beim Übernehmen merken, ob die Person gemeldet bzw. Nachrücker war (auch in der
   Turnierdatei); beim Zurückziehen wieder „gemeldet“ bzw. „Nachrücker“. Anwesenheit bleibt unverändert. Gilt für
   Ziehen, Doppelklick und Pfeilknopf, Einzel und Doppel. Test ergänzen.
7. TURNIERDATEIEN BLEIBEN LESBAR (11.11.2026, Weg zu 1.0.0, vom Nutzer freigegeben): Jede neuere HTS-Version öffnet
   ältere Turnierdateien. Grundlage gibt es schon (format-version, derzeit 7; neuere Formate werden mit Hinweis
   abgelehnt). Vorschlag: zusätzlich die Programmversion in die Datei schreiben; je Formatänderung eine Umwandlung beim
   Öffnen; je veröffentlichter Version eine Beispiel-Turnierdatei (mit Gruppen, KO, Qualifikation, Doppel, Anwesenheit)
   unter src/test/resources ablegen, ein Test öffnet alle und prüft Klassen, Spiele und Ergebnisse. In Doku und README
   den Satz „Neuere Versionen müssen ältere Turnierdateien nicht unbedingt lesen können“ ersetzen durch die Zusage.



0.41.0 (Issues #68–#70). README „Neu in 0.41.0“. DOKU AKTUALISIERT auf 0.41.0 (11.11.2026, auf Zuruf): docs/HTS-Dokumentation.md, PDF (29 Seiten) und
Claude-Docs-Dokument (neue Abschnitte 11 Handy-Zugang, 12 Vorgabeturnier/2er-3er, alter Abschnitt 11 jetzt 13; Statusbild
mit „nicht gespielt“). Noch nicht im Repository (Commit mit dem nächsten „kompilieren“ oder auf Zuruf).
#68 SearchChoice (Textfeld + Popup-Liste) statt editierbarer ComboBox für die Vereinsfilter (PlayerTableView,
ClassesView); Ui.makeSearchable entfernt. #69 PlayerTableView: Klick auf einzige markierte Zeile / leeren Bereich / Esc
hebt die Markierung auf (nicht beim Kästchen „Angemeldet“, Doppelklick bleibt). #70 decideQualification(false): Phase
angehalten + awaitingStart, Runde 2 erst per „Qualifikation Runde 2 starten“ (Knopf oben und über den Karten, gelber
Hinweis); undo hebt das Warten auf; simulateAll startet gleich. Infozeile der Werkzeugzeile bricht um. Tests: 210
bestanden (Version041Test). Geprüft mit Bildschirmfoto, Robot-Klicks und simulierten Tastatureingaben.
Feature-Sammlung 0.41.0 (zur Nachverfolgung):

2. BUG: VEREINSFILTER (durchsuchbar, #63) FEHLERHAFT (08.11.2026, Issue #68, Label „bug“) – BEHOBEN IM QUELLCODE, kommt
   mit dem nächsten „kompilieren“. Ursachen: (a) Feld anfangs leer statt „Alle“ (Editor nach setEditable nicht mit dem
   Wert abgeglichen); (b) beim Tippen filterte die editierbare ComboBox ihre eigenen Einträge – fiel der gewählte
   Eintrag heraus, setzte JavaFX Auswahl und Wert zurück (Text verschwand, Tabelle sprang), Pfeiltasten änderten den
   Wert sofort. Lösung: eigenes Steuerelement SearchChoice (Textfeld + aufklappende Liste): Tippen filtert nur die Liste,
   Wert erst bei Enter/Klick, Pfeiltasten nur in der Liste, Esc/Fokusverlust stellt her (Esc wurde vom Popup
   verschluckt – setHideOnEscape(false)). Geprüft mit simulierten Eingaben und Bildschirmfoto.

3. BUG: TEILNEHMENDE – MARKIERUNG NICHT AUFHEBBAR (08.11.2026, Issue #69, Label „bug“) – BEHOBEN IM QUELLCODE, kommt
   mit dem nächsten „kompilieren“. Ist einmal jemand markiert, blieb immer mindestens eine Zeile markiert (Standard-
   verhalten der JavaFX-Tabelle). Jetzt: erneuter Klick auf die einzige markierte Zeile, Klick in den leeren Bereich der
   Tabelle oder Esc hebt die Markierung auf; Strg+Klick wie gewohnt; Doppelklick öffnet weiterhin „Bearbeiten“; Klick
   auf das Kästchen „Angemeldet“ ändert die Markierung nicht. Mit Robot-Klicks geprüft.

4. QUALIFIKATION: JEDE RUNDE ERST PER „QUALI-RUNDE STARTEN“, AUCH RUNDE 2 PER DRAG & DROP (08.11.2026, Issue #70) –
   GEKLÄRT (Vorschläge 1–4 angenommen). Auch in Qualifikationsrunde 2 sollen die Spieler per Drag & Drop getauscht werden können; für
   beide Runden der Phase 2 gibt es „Quali-Runde starten“.
   Vorschläge: (1) Runde 2 wird nach „Weitere KO-Runde für die Gewinner“ ausgelost, aber noch nicht eingereiht; über den
   Karten der Runde der Knopf „Qualifikation Runde 2 starten“ (Runde 1 entsprechend „Qualifikation Runde 1 starten“
   statt „Phase starten“). (2) Bis zum Start lassen sich alle Spieler der Runde inkl. Freilos per Drag & Drop tauschen
   (nur innerhalb dieser Runde). (3) Andere Klassen nutzen die Tische weiter. (4) „Phase anhalten“ bleibt für eine
   laufende Runde.

0.40.0 (Issue #67). README „Neu in 0.40.0“.
#67 GroupDraw.roundRobin: Tabelle FIXED für 4/6/8/10/12 (ungerade Größe = nächste gerade ohne die letzte Position),
2er und über 12 weiter Kreisverfahren; P = Position in der Gruppe nach der Auslosung. Tests: 209 bestanden
(Version040Test: Schemata wie vorgegeben, jede Paarung genau einmal für 2–14).
Feature-Sammlung 0.40.0 (zur Nachverfolgung):

2. GRUPPENPHASE: FESTE SPIELREIHENFOLGE NACH GRUPPENPOSITION (08.11.2026, Issue #67) – GEKLÄRT.
   P1 = Gruppenkopf, P2 … in Setzreihenfolge der Gruppe. Bei 7 (5) Mitgliedern entfällt das Spiel gegen P8 (P6).
   8er/7er: R1 1-8 2-7 3-6 4-5 · R2 1-7 2-6 3-5 4-8 · R3 1-6 2-5 3-8 4-7 · R4 1-5 2-8 3-7 4-6 · R5 1-4 2-3 5-8 6-7 ·
            R6 1-3 2-4 5-7 6-8 · R7 1-2 3-4 5-6 7-8 (geprüft: jede Paarung genau einmal)
   6er/5er (laut Nutzer): R1 1-5 2-4 3-4 · R2 1-6 2-5 3-6 · R3 1-4 2-3 5-6 · R4 1-3 2-6 4-5 · R5 1-2 3-5 4-6
            → Tippfehler vermutet (P4 bzw. P6 je Runde doppelt); Vorschlag: R1 1-5 2-4 3-6 · R2 1-6 2-5 3-4
   4er: R1 1-3 2-4 · R2 1-4 2-3 · R3 1-2 3-4      3er: R1 1-3 · R2 2-3 · R3 1-2
   Bestätigt (08.11.): 6er/5er korrigiert: R1 1-5 2-4 3-6 · R2 1-6 2-5 3-4 · R3 1-4 2-3 5-6 · R4 1-3 2-6 4-5 ·
   R5 1-2 3-5 4-6. Gilt für alle Gruppenphasen (Phase 1 und 2). Gemeinsames Ende (#46) bleibt. Nur für künftige
   Auslosungen. Schema für 9/10 und 11/12 fortführen (über 12 wie bisher) – Tabellen BESTÄTIGT (alle Paarungen
   geprüft):
   12er/11er (wie 8er: erst obere gegen untere Hälfte, dann innerhalb der Hälften nach dem 6er-Schema):
     R1 1-12 2-11 3-10 4-9 5-8 6-7 · R2 1-11 2-10 3-9 4-8 5-7 6-12 · R3 1-10 2-9 3-8 4-7 5-12 6-11 ·
     R4 1-9 2-8 3-7 4-12 5-11 6-10 · R5 1-8 2-7 3-12 4-11 5-10 6-9 · R6 1-7 2-12 3-11 4-10 5-9 6-8 ·
     R7 1-6 2-5 3-4 7-12 8-11 9-10 · R8 1-5 2-4 3-6 7-11 8-10 9-12 · R9 1-4 2-3 5-6 7-10 8-9 11-12 ·
     R10 1-3 2-6 4-5 7-9 8-12 10-11 · R11 1-2 3-5 4-6 7-8 9-11 10-12
   10er/9er: R1 1-10 2-9 3-8 4-7 5-6 · R2 1-9 2-8 3-10 4-6 5-7 · R3 1-8 2-6 3-7 4-10 5-9 · R4 1-7 2-10 3-6 4-9 5-8 ·
     R5 1-6 2-5 3-4 7-10 8-9 · R6 1-5 2-4 3-9 6-10 7-8 · R7 1-4 2-3 5-10 6-8 7-9 · R8 1-3 2-7 4-5 6-9 8-10 ·
     R9 1-2 3-5 4-8 6-7 9-10
   Bestätigt: P = Position in der Gruppe nach der Auslosung (Reihenfolge, in der die Gruppe nach der Auslosung steht;
   nach Drag & Drop die Position, auf die getauscht wurde).

0.39.0 (Issues #62–#66). README „Neu in 0.39.0“.
#62 Tournament.seedPositions (XML-Klassen, ohne Abgemeldete/Nachrücker, Gleichstand gleicher Platz, ohne TTR unten,
Doppel Ø-TTR); PlayerTableView Spalte „Setzplatz“ nur bei Klassenfilter (setSeedSource). #63 Ui.makeSearchable /
setSearchItems (editierbare ComboBox, FilteredList der Einträge, Wert erst bei Auswahl; Esc/Fokusverlust stellt her) für
die Vereinsfilter in PlayerTableView und ClassesView. #64 attendanceMenu: „Aktion für Markierte wählen“,
„Handyabmeldungen bearbeiten“, CSS .action-menu, Ui.oneLineWhenPossible statt fester Umbruchbreite. #65 DrawView: eine
FlowPane (Suche, Gleichstand, Knöpfe in neuer Reihenfolge, PDF zuletzt, Infozeile am Ende), printBtn .action-menu.
#66 „Handyabmeldungen“ sichtbar bei laufendem LiveServer oder offenen Handyabmeldungen. Geprüft bei 1093 × 614 und
911 × 512. Tests: 207 bestanden (Version039Test).
Feature-Sammlung 0.39.0 (zur Nachverfolgung):

2. TEILNEHMENDE: SETZPOSITION NACH TTR (08.11.2026, Issue #62) – GEKLÄRT. In der Übersicht „Teilnehmende“
   wird eine Setzposition angezeigt: Rangfolge nach TTR innerhalb der Klasse, ohne TTR ganz unten; nur wenn auf eine
   Klasse gefiltert ist und die Klasse per XML importiert wurde. Bestätigt: gleicher TTR = gleicher Setzplatz
   (z. B. 3 und 3, dann 5); Doppel nach Ø-TTR.

3. VEREINSFILTER ALS DURCHSUCHBARE AUSWAHL (08.11.2026, Issue #63) – GEKLÄRT. Die Auswahl „Verein:“ soll
   durchsuchbar sein. Vorschläge: Eintippen filtert die Liste sofort (Teil des Namens genügt, Groß-/Kleinschreibung
   egal, z. B. „hamb“ → „Hamburger TV“, „SC Hamburg“); Pfeiltasten + Enter wählen, Esc stellt den vorigen Wert her,
   leeres Feld = „Alle“. Überall, wo nach Verein gefiltert wird: Teilnehmende sowie in den Klassen die Listen
   „Verfügbar“ und „In der Klasse“.
   Bestätigt (08.11.): Tippen filtert nur die Optionen der Auswahlliste; die Tabelle wird erst gefiltert, wenn ein
   Wert ausgewählt wurde (Enter bzw. Klick).

4. TEILNEHMENDE: KNOPFZEILE (08.11.2026, Issue #64) – GEKLÄRT. „Abgemeldete als PDF“ steht rechts neben
   den beiden Auswahlmenüs (statt in einer zweiten Zeile – Ursache: FlowPane-Standardumbruchbreite 400 px). Die Menüs
   „Aktion bezüglich Markierte“ und „Handyabmeldungen“ bekommen den Stil der Filterauswahlen (abgerundet, hellgrau,
   Pfeil rechts). Vorschlag: denselben Stil auch für „PDF / Drucken ▾“ im Klassenkopf; Umbruch nur, wenn es wirklich
   eng wird.

5. KLASSENANSICHT: WERKZEUGZEILE IN EINER ZEILE (08.11.2026, Issue #65) – GEKLÄRT. Suche, „Gleichstand“,
   „PDF / Drucken ▾“ und die Knöpfe (z. B. Neu auslosen, Auslosung verwerfen, bereit, starten) in einer Zeile
   (Umbruch nur, wenn es wirklich nicht passt); alle Auswahlmenüs im Design der Auswahl „Gleichstand“ (weiß,
   abgerundet, Rahmen, Pfeil). Rückfrage: gilt das Gleichstand-Design dann auch für die Menüs auf „Teilnehmende“ (#64)?
   Reihenfolge der Knöpfe (08.11., Nutzer): ganz links „Auslosung verwerfen“, dann „Neu auslosen“, „bereit“,
   „starten“, als letztes „PDF / Drucken“.
   Design (08.11., Nutzer): Ja – auch die Menüs auf „Teilnehmende“. Vorlage sind zwei Beispiele: Auswahl wie
   „Gleichstand“ (weiß, Rahmen, abgerundet, Pfeil) und Filter wie „Alle ▾“ (hellgrau gefüllt, abgerundet, ohne
   Rahmen). Bei Aktions-Menüs zeigt der Text, wozu sie dienen (z. B. „Aktion für Markierte wählen“, „Suchen“).
   Vorschlag (Rückfrage gestellt): Aktions- und Einstellungsmenüs im Gleichstand-Design mit erklärendem Text
   („Aktion für Markierte wählen ▾“, „Handyabmeldungen bearbeiten ▾“, „PDF / Drucken ▾“, „Gleichstand: … ▾“);
   Filter- und Suchauswahlen im „Alle“-Design (Verein, Klasse, Geschlecht, Status, Gruppe); durchsuchbare Auswahl
   (#63) zeigt beim Öffnen „Verein suchen …“. BESTÄTIGT (Aufteilung und Texte). Vorschlag für die übrigen Knöpfe (je nach Zustand): Phasen-Knöpfe
   („Phase anhalten/starten/fortsetzen“, „Phase neu starten“, „Phase vorzeitig beenden“, „Phase abschließen“ …) an
   der Stelle von „bereit/starten“, „PDF / Drucken“ immer zuletzt.

6. TEILNEHMENDE: „HANDYABMELDUNGEN“ NUR BEI LAUFENDEM HANDY-ZUGANG (08.11.2026, Issue #66) – GEKLÄRT (Vorschlag angenommen). Das
   Menü „Handyabmeldungen“ auf „Teilnehmende“ erscheint nur, wenn unter „Mobile Einstellungen“ der Handy-Zugang
   gestartet ist. Vorschlag: zusätzlich anzeigen, solange noch offene Handyabmeldungen (orange) bestehen – auch wenn
   der Zugang inzwischen beendet ist –, damit sie bestätigt oder verworfen werden können.

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
Erledigt (15.11.2026): Export-Datei mit echtem click-TT-Upload getestet – erfolgreich.

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
