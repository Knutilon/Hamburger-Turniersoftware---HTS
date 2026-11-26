# HTS – Live-Server einrichten (Schritt für Schritt)

Stand: HTS 1.7.0 (Live-Server seit 1.6.0, #101; neue Live-Seiten und lesbare Adressen seit 1.7.0, #147). Diese Anleitung richtet die Live-Ergebnisse von HTS auf einem gemieteten Server ein,
beschrieben am Beispiel **netcup VPS mit Ubuntu 24.04** und der Domain **hts-livescoring.de**. Für eine andere Domain
überall die eigene einsetzen.

So funktioniert es: HTS auf dem Turnier-Laptop bleibt führend. Es lädt nach jeder Änderung den Turnierstand über HTTPS
auf den Server (seit 1.10.0 komprimiert und höchstens alle 30 Sekunden). Der Server zeigt ihn Zuschauerinnen und Zuschauern unter
`https://hts-livescoring.de`. Vom Server fließt nichts zurück zu HTS, Ergebnisse werden weiter nur am Laptop eingegeben.

## Was Sie brauchen

- einen Server mit Ubuntu 24.04 (Minimal reicht; 1–2 Kerne, 1–2 GB RAM, 10 GB Speicher genügen),
- eine Domain, deren **A-Eintrag** (IPv4) und **AAAA-Eintrag** (IPv6) auf den Server zeigen – für `@` (die Domain selbst)
  und `www`,
- das Paket **hts-server-1.7.0.tar.gz** (beim Release auf GitHub bzw. vom Betreuer),
- auf dem eigenen Rechner ein Terminal: unter Windows die „Eingabeaufforderung“ oder „PowerShell“ (ssh und scp sind
  eingebaut), unter macOS/Linux das „Terminal“.

## 1. Domain prüfen

Auf dem eigenen Rechner:

```
nslookup hts-livescoring.de
nslookup www.hts-livescoring.de
```

Beide müssen die IP-Adressen des Servers zeigen (stehen im netcup Server Control Panel unter „Netzwerk“). Neue
Einträge brauchen manchmal einige Minuten bis Stunden.

## 2. Paket auf den Server kopieren

Im Ordner, in dem das Paket liegt (z. B. „Downloads“):

```
scp hts-server-1.7.0.tar.gz root@hts-livescoring.de:
```

Beim ersten Verbinden fragt ssh, ob dem Server vertraut werden soll: mit `yes` bestätigen. Dann das Root-Passwort (aus
der netcup-Mail bzw. selbst gesetzt) eingeben – beim Tippen erscheinen keine Zeichen, das ist normal.

## 3. Einrichten

```
ssh root@hts-livescoring.de
tar xzf hts-server-1.7.0.tar.gz
cd hts-server
./install.sh hts-livescoring.de
```

Das Skript arbeitet etwa 2–5 Minuten und erledigt alles selbst:

| Schritt | Was passiert |
| --- | --- |
| Updates | Ubuntu wird auf den neuesten Stand gebracht |
| Java 21 | wird installiert (ohne Oberfläche) |
| Caddy | Webserver aus der offiziellen Paketquelle; holt und erneuert das HTTPS-Zertifikat (Let's Encrypt) automatisch, leitet `http://` und `www.` auf `https://hts-livescoring.de` um; speichert keine Zugriffsprotokolle |
| HTS-Live-Server | läuft als Dienst `hts-server` unter einem eigenen Benutzer ohne Anmeldung, nur intern hinter Caddy, startet nach einem Neustart von selbst |
| Firewall | nur SSH, HTTP (80) und HTTPS (443) sind offen |
| Sicherheitsupdates | werden täglich automatisch eingespielt; ist ein Neustart nötig, startet der Server um 4:30 Uhr neu |
| Geheimer Schlüssel | wird zufällig erzeugt (48 Zeichen) und **nur einmal am Ende angezeigt** |

Am Ende steht:

```
GEHEIMER SCHLÜSSEL (wird nur jetzt angezeigt – bitte sicher notieren):

    Ab3x…(48 Zeichen)
```

**Den Schlüssel sicher notieren** (z. B. im Passwortmanager). Auf dem Server liegt nur ein Prüfwert davon, er lässt
sich dort nicht wieder auslesen. Verloren? Mit `./install.sh --neuer-schluessel` einen neuen erzeugen und in HTS
eintragen; der alte wird damit ungültig.

## 4. Impressum und Datenschutzerklärung ausfüllen

Eine öffentliche Website braucht beides. Das Skript hat Vorlagen angelegt, die auf HTS zugeschnitten sind:

```
nano /etc/hts-server/impressum.html
nano /etc/hts-server/datenschutz.html
```

Alle Stellen `__AUSFUELLEN__` ersetzen (Name, Anschrift, Kontakt, bei Vereinen Vertretung und Registereintrag,
Anschrift von netcup laut AV-Vertrag, zuständige Aufsichtsbehörde, Datum). Speichern mit **Strg+O**, **Enter**,
Beenden mit **Strg+X**. Die Änderungen gelten sofort. Bitte beide Texte vor der Veröffentlichung von fachkundiger Seite
prüfen lassen.

**Seit HTS 1.8.0 geht es auch ohne `nano`:** Die fertigen Fassungen als `impressum.html` und/oder `datenschutz.html`
in eine Datei `hts-rechtstexte.tar.gz` packen und zusammen mit dem Paket hochladen:

```
scp hts-server-1.8.0.tar.gz hts-rechtstexte.tar.gz root@hts-livescoring.de:
```

`install.sh` bzw. `update.sh` finden die Datei neben dem Paket, prüfen sie (keine Platzhalter `__AUSFUELLEN__` mehr,
höchstens 512 KB), sichern die vorherigen Fassungen als `impressum.html.bak-<Datum>` usw. und spielen sie ein. Ohne
diese Datei bleiben Impressum und Datenschutz unverändert. Den Text der Seite **Spenden** ändert
`nano /etc/hts-server/spenden.html`.

Außerdem:

- **AV-Vertrag** mit netcup abschließen (im netcup-Kundenkonto, Vertrag zur Auftragsverarbeitung nach Art. 28 DSGVO).
- In der **Turnierausschreibung** darauf hinweisen, dass Ergebnisse live im Internet veröffentlicht werden.

## 5. HTS verbinden

1. HTS öffnen → Turnierübersicht → Reiter **„Webserver“**.
2. **Entsperren** (Admin-Passwort, seit HTS 1.7.0), dann Adresse `https://hts-livescoring.de` und den notierten Schlüssel
   eintragen → **Speichern**.
3. **„Verbindung testen“** → „Verbindung in Ordnung.“
4. Im Turnier → **„Live im Internet“** einschalten (seit 1.8.0 bei neuen Turnieren schon an, bei Testturnieren nicht).
   Auf dem Server erscheinen die Vornamen immer abgekürzt („M. Mustermann“).
5. Die Adresse für Zuschauer (z. B. `https://hts-livescoring.de/live/Hamburger_Ranglistenturnier_28_11_2026`) steht
   darunter, mit „Adresse kopieren“ und **„QR-Code-Aushang drucken“** (A4 zum Aushängen; beim Turnierstart fragt HTS
   danach). Alle Turniere stehen außerdem auf `https://hts-livescoring.de`.

Unten rechts zeigt HTS den Stand: „veröffentlicht“ bzw. „keine Verbindung – wird nachgeholt“ (bei Funkloch hält HTS
den letzten Stand und versucht es alle 10 Sekunden erneut).

### Online-Anmeldung am Turniertag (seit HTS 1.9.0)

Im Turnier → „Live im Internet“ → **Online-Anmeldung zum Turnier**: Uhrzeit „Online-Anmeldung bis“ eintragen (geht auch
schon beim Anlegen), **Online-Anmeldung einschalten** und den **QR-Code-Aushang drucken**. Am Turniertag melden sich
Spieler/innen über den QR-Code an oder ab, Betreuer/innen auch für ihren Verein. HTS holt die Meldungen alle paar Sekunden
vom Server ab – dafür muss der Laptop Internet haben und das Turnier veröffentlicht werden. Nach der Uhrzeit oder nach dem
Ausschalten nimmt der Link nichts mehr an.

## 6. Ausprobieren

Am besten mit einem **Testturnier**: ein paar Ergebnisse eintragen und `https://hts-livescoring.de` auf dem Handy über
das **mobile Netz** (nicht das Hallen-WLAN) öffnen. Die Seiten aktualisieren sich alle 10 Sekunden von selbst.

| Adresse | Inhalt |
| --- | --- |
| `https://hts-livescoring.de` | „Laufende Turniere“ (bis 1.9.0 „Aktuelle Turniere“) mit Ausrichter und ggf. „zur Turnierwelt“; Reiter „Vergangene Turniere“ (`…/vergangen`) |
| `…/live/<Turniername>_<TT>_<MM>_<JJJJ>` | ein Turnier, z. B. `…/live/Hamburger_Ranglistenturnier_28_11_2026`: Reiter je Phase, Gruppenkacheln, Tische, Spielersuche, „Meine Spiele“ |
| `…/live/<Turnier>/<Klasse>/spiel45` | ein einzelnes Spiel mit „Link kopieren“ (seit 1.8.0) |
| `…/impressum`, `…/datenschutz`, `…/spenden` | Impressum, Datenschutzerklärung und Seite „Spenden“ |

Nach dem Turnier in HTS **„Nur Endergebnis behalten“** (Tische und Spiele verschwinden, Endergebnisse bleiben) oder
**„Live-Daten löschen“**. Spätestens **90 Tage** nach der letzten Änderung löscht der Server ein Turnier von selbst
(einstellbar in `/etc/hts-server/server.properties`, `retention-days`).

## 7. Neue Version einspielen

Neues Paket kopieren und im neuen Ordner `update.sh` starten (Beispiel 1.10.0 → 1.11.0; `hts-rechtstexte.tar.gz` nur,
wenn Impressum bzw. Datenschutz mit eingespielt werden sollen):

```
scp hts-server-1.11.0.tar.gz hts-rechtstexte.tar.gz root@hts-livescoring.de:
ssh root@hts-livescoring.de
rm -rf hts-server
tar xzf hts-server-1.11.0.tar.gz
cd hts-server
./update.sh
```

Einstellungen, Schlüssel und Turniere bleiben erhalten; Impressum und Datenschutz ebenso, außer neben dem Paket liegt
`hts-rechtstexte.tar.gz` (siehe Abschnitt 4). Beim Update auf 1.8.0 legt `update.sh` außerdem die Vorlage
`spenden.html` an; beim Update auf 1.10.0 erhöht es den Arbeitsspeicher des Live-Servers von 512 MB auf 1 GB; seit 1.11.0 richtet es den SSH-Schutz ein (fail2ban: 5 Fehlversuche in 10 Minuten → 1 Tag gesperrt, höchstens 3 Passwortversuche je Verbindung, Firewall-Bremse). Wer sich selbst aussperrt, meldet sich über die Server-Konsole des Anbieters (netcup: SCP) an und gibt `fail2ban-client set sshd unbanip <eigene IP>` ein. Startet die
neue Version nicht, stellt `update.sh` die vorige automatisch wieder her.

## 8. Hilfe bei Problemen

| Problem | Prüfen / Lösung |
| --- | --- |
| „Verbindung testen“: *Server nicht erreichbar* | Adresse mit `https://`? Domain zeigt auf den Server (Schritt 1)? `systemctl status caddy hts-server` |
| „Verbindung testen“: *Schlüssel abgelehnt* | Schlüssel vollständig kopiert (48 Zeichen, keine Leerzeichen)? Sonst `./install.sh --neuer-schluessel` |
| Browser meldet Zertifikatsfehler | Caddy konnte noch kein Zertifikat holen – meist zeigt die Domain (noch) nicht auf den Server. `journalctl -u caddy -n 50` |
| Seite zeigt „Dieses Turnier gibt es hier (nicht mehr)“ | „Live im Internet“ in HTS eingeschaltet? Daten gelöscht oder älter als 90 Tage? |
| Live-Server läuft nicht | `journalctl -u hts-server -n 50`, dann `systemctl restart hts-server` |

Nützliche Befehle auf dem Server:

```
systemctl status hts-server        # läuft der Live-Server?
journalctl -u hts-server -n 50     # letzte Meldungen (ohne IP-Adressen)
systemctl restart hts-server       # neu starten
ls /var/lib/hts-server/turniere    # gespeicherte Turniere
```

## Technik in Kürze

- Programm: `/opt/hts-server/hts-server.jar` (HTS-Live-Server, Java 21), Dienst `hts-server` (systemd, abgeschottet:
  nur Lesezugriff aufs System, schreibt nur in `/var/lib/hts-server`).
- Einstellungen: `/etc/hts-server/server.properties` (Prüfwert des Schlüssels als SHA-256, Port 8090 nur auf
  127.0.0.1, Aufbewahrungsfrist, größte Turnierdatei 25 MB).
- Daten: `/var/lib/hts-server/turniere/<Kennung>/` (letzter Stand `turnier.hts`, Logo des Ausrichters, `meta.properties`).
- Schnittstelle: siehe `docs/WEBSERVER-PROTOKOLL.md`.
- Keine Cookies, keine Zugriffsprotokolle, keine Inhalte von Drittanbietern; Sicherheits-Kopfzeilen (CSP, HSTS u. a.).
