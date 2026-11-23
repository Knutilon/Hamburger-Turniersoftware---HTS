# HTS – Protokoll „Live im Internet“ (Webserver)

Stand: HTS 1.5.0 (Issues #135–#138). HTS lädt nur hoch; vom Server fließt nichts zurück. Der Server selbst (#101) ist
noch nicht gebaut – dieses Protokoll beschreibt, was er annehmen muss.

Alle Anfragen gehen über HTTPS an die in HTS eingetragene Adresse (Turnierübersicht → „Webserver“) und tragen den
geheimen Schlüssel als `Authorization: Bearer <Schlüssel>` sowie `X-HTS-Version: <Version>`.

| Anfrage | Inhalt | Zweck |
| --- | --- | --- |
| `GET /api/status` | – | „Verbindung testen“ (2xx = in Ordnung, 401/403 = Schlüssel abgelehnt) |
| `PUT /api/turniere/<ID>` | Turnierdatei (`.hts`, `application/octet-stream`) | aktueller Stand des Turniers |
| `PUT /api/turniere/<ID>/logo` | PNG (`image/png`) | Logo des Ausrichters, einmal je Sitzung |
| `DELETE /api/turniere/<ID>` | – | „Live-Daten löschen“ |
| `POST /api/turniere/<ID>/nur-endergebnis` | – | „Nur Endergebnis behalten“ |

- `<ID>` ist die Kennung des Turniers aus der Turnierdatei. Zuschauer erreichen die Live-Seiten unter
  `<Adresse>/live/<ID>`.
- HTS sendet nach jeder Änderung höchstens alle 5 Sekunden. Ist der Server nicht erreichbar, behält HTS den letzten Stand
  und versucht es alle 10 Sekunden erneut.
- Mit „Vornamen abkürzen“ enthält die hochgeladene Turnierdatei nur die Anfangsbuchstaben der Vornamen („M.“); die
  Turnierdatei auf dem Rechner bleibt unverändert.
- Antworten mit 2xx gelten als Erfolg; alles andere (auch 401/403) als Fehler, der in HTS unten rechts angezeigt wird.
