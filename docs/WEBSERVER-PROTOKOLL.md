# HTS – Protokoll „Live im Internet“ (Webserver)

Stand: HTS 1.9.0. HTS lädt nur hoch (seit 1.5.0, Issues #135–#138); vom Server fließt nichts zurück. Seit 1.6.0 gibt
es den passenden Server: den HTS-Live-Server (#101, `de.hts.web.LiveHost`, Paket `hts-server-x.y.z.tar.gz`, Einrichtung
siehe `docs/LIVE-SERVER-ANLEITUNG.md`).

Alle Anfragen gehen über HTTPS an die in HTS eingetragene Adresse (Turnierübersicht → „Webserver“) und tragen den
geheimen Schlüssel als `Authorization: Bearer <Schlüssel>` sowie `X-HTS-Version: <Version>`. Seit 1.8.0 trägt
`PUT /api/turniere/<ID>` zusätzlich `X-HTS-Ausrichter: <Name>` (Name des Ausrichters aus dem Reiter „Ausrichter“,
URL-kodiert nach UTF-8, höchstens 120 Zeichen, ohne Steuerzeichen); fehlt die Kopfzeile, zeigt der Server keinen Namen.

| Anfrage | Inhalt | Zweck |
| --- | --- | --- |
| `GET /api/status` | – | „Verbindung testen“ (2xx = in Ordnung, 401/403 = Schlüssel abgelehnt) |
| `PUT /api/turniere/<ID>` | Turnierdatei (`.hts`, `application/octet-stream`) | aktueller Stand des Turniers |
| `PUT /api/turniere/<ID>/logo` | PNG (`image/png`) | Logo des Ausrichters, einmal je Sitzung |
| `DELETE /api/turniere/<ID>` | – | „Live-Daten löschen“ |
| `POST /api/turniere/<ID>/nur-endergebnis` | – | „Nur Endergebnis behalten“ |
| `GET /api/turniere/<ID>/anmeldungen` | – | seit 1.9.0: Meldungen der Online-Anmeldung abholen, Antwort `{"bis":N,"meldungen":[{"n":1,"id":"…","a":1,"t":Millisekunden}]}` (`a` 1 = angemeldet, 0 = nicht angemeldet) |
| `POST /api/turniere/<ID>/anmeldungen` | `bis=<N>` (`application/x-www-form-urlencoded`) | seit 1.9.0: Quittung – der Server löscht alle Meldungen bis Nummer N |

- `<ID>` ist die Kennung des Turniers aus der Turnierdatei. Zuschauer erreichen die Live-Seiten seit 1.7.0 unter der
  lesbaren Adresse `<Adresse>/live/<Turniername>_<TT>_<MM>_<JJJJ>` (`de.hts.web.LiveSlug`; vorher `<Adresse>/live/<ID>`,
  die weiter auf die lesbare Adresse umleitet).
- HTS sendet nach jeder Änderung höchstens alle 30 Sekunden (bis 1.9.0: 5 Sekunden). Ist der Server nicht erreichbar,
  behält HTS den letzten Stand und versucht es alle 10 Sekunden erneut.
- Seit 1.10.0: Der Server setzt bei jeder angemeldeten API-Antwort `X-HTS-Gzip: 1`. Danach sendet HTS die Turnierdatei
  gzip-komprimiert mit `Content-Encoding: gzip` (etwa ein Neuntel der Größe). Der Server entpackt höchstens
  `max-upload-mb` Megabyte (größer oder nicht entpackbar → 413). Ältere Server melden das nicht und bekommen die Datei
  unkomprimiert.
- Seit 1.10.0 hält der Server fertige Live-Seiten bis zum nächsten Upload bereit (höchstens 60 Sekunden, 600 Seiten je
  Turnier).
- Die hochgeladene Turnierdatei enthält nur die Anfangsbuchstaben der Vornamen („M.“) – seit 1.8.0 immer (vorher nur mit
  „Vornamen abkürzen“); die Turnierdatei auf dem Rechner bleibt unverändert. Der Link zur Turnierwelt (seit 1.8.0) steht
  in der Turnierdatei (`turnierwelt`, nur https-Adressen werden angezeigt).
- Seit 1.9.0 (Online-Anmeldung): Schlüssel, Schalter und Uhrzeit stehen in der hochgeladenen Turnierdatei
  (`checkin-token`, `online-checkin`, `checkin-until`). Zuschauer erreichen die Anmeldeseite unter
  `<Adresse>/anmeldung/<Schlüssel>` (26 Zeichen); `…/daten.json` liefert die Liste (abgekürzte Vornamen), `POST …/melden`
  mit `ids=<Kennung,…>&anwesend=1|0` meldet. Offen nur, wenn der Schalter an ist, heute der Turniertag (Europa/Berlin) und
  die Uhrzeit noch nicht erreicht ist; höchstens 120 Meldungen je Minute und Turnier (sonst `429`). HTS holt die Meldungen
  alle 5 Sekunden ab, solange der Schalter an ist (bis 30 Minuten nach der Uhrzeit), übernimmt sie und quittiert.
- Seit 1.8.0 sendet HTS `DELETE /api/turniere/<ID>` auch beim Löschen eines veröffentlichten Turniers in der
  Turnierübersicht (nach Rückfrage); gelingt das nicht, merkt HTS den Auftrag (`webserver-loeschen.txt` im
  Programmordner) und holt ihn beim nächsten Start bzw. Veröffentlichen nach.
- Antworten mit 2xx gelten als Erfolg; alles andere (auch 401/403) als Fehler, der in HTS unten rechts angezeigt wird.

## Verhalten des HTS-Live-Servers (1.6.0)

- Schlüssel: Der Server kennt nur den SHA-256-Prüfwert (`key-sha256` in `server.properties`) und vergleicht in
  konstanter Zeit. Mehr als 30 Fehlversuche je Minute → vorübergehend `429`.
- `GET /api/status` antwortet `{"ok":true,"server":"HTS-Live-Server","version":"…"}`.
- `<ID>`: nur Buchstaben, Ziffern, `-` und `_` (höchstens 64 Zeichen), sonst `400`.
- `PUT /api/turniere/<ID>`: höchstens `max-upload-mb` (Vorgabe 25 MB, sonst `413`); die Datei wird wie in HTS gelesen
  (gegen XXE gehärtet), ungültige Dateien → `400`. Gespeichert wird atomar als `turnier.hts`; ein neuer Stand hebt
  „Nur Endergebnis“ wieder auf.
- `PUT …/logo`: nur PNG (Signatur geprüft), höchstens 2 MB.
- `POST …/nur-endergebnis`: Startseite des Turniers zeigt nur noch die Endergebnisse; Tische, Klassen, Suche und
  „Meine Spiele“ leiten dorthin um.
- Aufbewahrung: Turniere werden `retention-days` (Vorgabe 90) Tage nach der letzten Änderung automatisch gelöscht.
- Öffentliche Seiten: `/` (seit 1.8.0 „Aktuelle Turniere“, seit 1.10.0 „Laufende Turniere“), `/download` (seit 1.10.0 „Download HTS“) und `/vergangen` („Vergangene Turniere“: beendet, nur
  Endergebnis oder Datum vor heute), `/live/<Turnier>` mit `…/tische`, `…/klasse?k=`, `…/ergebnis?k=`, `…/suche`,
  `…/ich?id=`, `…/logo.png` und seit 1.8.0 `…/<Klasse>/spiel<Nr>` bzw. `…/<Klasse>/phase<n>/spiel<Nr>` (Klassenname mit
  „_“ statt Leerzeichen); `/impressum`, `/datenschutz` und `/spenden` aus `impressum.html`, `datenschutz.html` bzw.
  `spenden.html` im Konfigurationsordner (Spenden ohne Datei: Vorgabetext); seit 1.9.0 `/anmeldung/<Schlüssel>`. Logo und Seitensymbol mit `?v=<Version>`
  (7 Tage zwischengespeichert, ohne Version 1 Stunde). Keine Anmeldung und keine Ergebniseingabe über das Internet.
- Der Server speichert je Turnier in `meta.properties` zusätzlich `organizer` (aus `X-HTS-Ausrichter`) und `world`
  (Turnierwelt-Link) – seit 1.8.0.
- Keine Cookies, nichts im Browser gespeichert (seit 1.8.0 weder Local noch Session Storage), keine Zugriffsprotokolle, Meldungen ohne IP-Adressen; Kopfzeilen `Content-Security-Policy`,
  `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`; `robots.txt` sperrt `/live/` für Suchmaschinen.
