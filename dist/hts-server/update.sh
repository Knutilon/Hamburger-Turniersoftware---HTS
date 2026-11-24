#!/usr/bin/env bash
# HTS – Hamburger Turniersoftware: Live-Server auf eine neue Version bringen (#101)
# Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Aufruf als root im entpackten Ordner der NEUEN Version:   ./update.sh
# Ersetzt nur das Programm (hts-server.jar); Einstellungen, Schlüssel, Impressum und Turniere bleiben.
# Geht etwas schief, wird die vorige Version automatisch wiederhergestellt.

set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OPT=/opt/hts-server

[[ $EUID -eq 0 ]] || { echo "Bitte als root ausführen (z. B. mit sudo ./update.sh)." >&2; exit 1; }
[[ -f /etc/systemd/system/hts-server.service ]] || { echo "Der Live-Server ist noch nicht eingerichtet – bitte zuerst ./install.sh ausführen." >&2; exit 1; }
[[ -f "$HERE/hts-server.jar" ]] || { echo "hts-server.jar fehlt im Ordner $HERE." >&2; exit 1; }
if [[ -f "$HERE/SHA256SUMS" ]]; then
  (cd "$HERE" && sha256sum --quiet -c SHA256SUMS) || { echo "Prüfsumme stimmt nicht – Paket beschädigt?" >&2; exit 1; }
fi
if [[ "$HERE" == "$OPT" ]]; then
  echo "Bitte update.sh aus dem entpackten Ordner der neuen Version starten, nicht aus $OPT." >&2; exit 1
fi

cp "$OPT/hts-server.jar" "$OPT/hts-server.jar.vorher"
install -o root -g root -m 644 "$HERE/hts-server.jar" "$OPT/hts-server.jar"
for f in LICENSE LIESMICH.txt; do [[ -f "$HERE/$f" ]] && install -o root -g root -m 644 "$HERE/$f" "$OPT/$f"; done
[[ -d "$HERE/Lizenzen" ]] && { rm -rf "$OPT/Lizenzen"; cp -r "$HERE/Lizenzen" "$OPT/Lizenzen"; }
install -o root -g root -m 755 "$HERE/update.sh" "$OPT/update.sh"
systemctl restart hts-server

PORT="$(sed -n 's/^port=//p' /etc/hts-server/server.properties)"; PORT="${PORT:-8090}"
for _ in $(seq 1 30); do
  if curl -fs "http://127.0.0.1:$PORT/" >/dev/null; then
    echo "Aktualisiert – der HTS-Live-Server läuft wieder."
    exit 0
  fi
  sleep 1
done
echo "Die neue Version startet nicht – stelle die vorige wieder her." >&2
cp "$OPT/hts-server.jar.vorher" "$OPT/hts-server.jar"
systemctl restart hts-server
echo "Vorige Version läuft wieder. Fehlermeldungen: journalctl -u hts-server -n 50" >&2
exit 1
