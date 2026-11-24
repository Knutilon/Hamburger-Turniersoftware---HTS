# HTS – Protokoll „Live im Internet“ (Webserver)

Stand: HTS 1.6.0. HTS lädt nur hoch (seit 1.5.0, Issues #135–#138); vom Server fließt nichts zurück. Seit 1.6.0 gibt
es den passenden Server: den HTS-Live-Server (#101, `de.hts.web.LiveHost`, Paket `hts-server-x.y.z.tar.gz`, Einrichtung
siehe `docs/LIVE-SERVER-ANLEITUNG.md`).

Alle Anfragen gehen über HTTPS an die in HTS eingetragene Adresse (Turnierübersicht → „Webserver“) und tragen den
geheimen Schlüssel als `Authorization: Bearer <Schlüssel>` sowie `X-HTS-Version: <Version>`.

| Anfrage | Inhalt | Zweck |
| --- | --- | --- |
| `GET /api/status` | – | „Verbindung testen“ (2xx = in Ordnung, 401/403 = Schlüssel abgelehnt) |
| `PUT /api/turniere/<ID>` | Turnierdatei (`.hts`, `application/octet-stream`) | aktueller Stand des Turniers |
| `PUT /api/turniere/<ID>/logo` | PNG (`image/png`) | Logo des Ausrichters, einmal je Sitzung |
| `DELETE /api/turniere/<ID>` | – | „Live-Daten löschen“ |
| `POST /api/turniere/<ID>/nur-endergebnis` | – | „Nur Endergebnis behalten“ |

- `<ID>` ist die Kennung des Turniers aus der Turnierdatei. Zuschauer erreichen die Live-Seiten seit 1.7.0 unter der
  lesbaren Adresse `<Adresse>/live/<Turniername>_<TT>_<MM>_<JJJJ>` (`de.hts.web.LiveSlug`; vorher `<Adresse>/live/<ID>`,
  die weiter auf die lesbare Adresse umleitet).
- HTS sendet nach jeder Änderung höchstens alle 5 Sekunden. Ist der Server nicht erreichbar, behält HTS den letzten Stand
  und versucht es alle 10 Sekunden erneut.
- Mit „Vornamen abkürzen“ enthält die hochgeladene Turnierdatei nur die Anfangsbuchstaben der Vornamen („M.“); die
  Turnierdatei auf dem Rechner bleibt unverändert.
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
- Öffentliche Seiten: `/` (alle Turniere: laufend, demnächst, abgeschlossen), `/live/<ID>` mit `…/tische`,
  `…/klasse?k=`, `…/ergebnis?k=`, `…/suche`, `…/ich?id=`, `…/logo.png`; `/impressum` und `/datenschutz` aus
  `impressum.html` bzw. `datenschutz.html` im Konfigurationsordner. Keine Anmeldung und keine Ergebniseingabe über das
  Internet.
- Keine Cookies, keine Zugriffsprotokolle, Meldungen ohne IP-Adressen; Kopfzeilen `Content-Security-Policy`,
  `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`; `robots.txt` sperrt `/live/` für Suchmaschinen.
