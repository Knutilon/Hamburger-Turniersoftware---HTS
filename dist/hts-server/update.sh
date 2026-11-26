#!/usr/bin/env bash
# HTS – Hamburger Turniersoftware: Live-Server auf eine neue Version bringen (#101)
# Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Aufruf als root im entpackten Ordner der NEUEN Version:   ./update.sh
# Ersetzt das Programm (hts-server.jar); Einstellungen, Schlüssel und Turniere bleiben. Impressum und Datenschutz
# bleiben ebenso – außer neben dem Paket liegt eine Datei hts-rechtstexte.tar.gz mit eigenen Fassungen (1.8.0).
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

# 1.8.0 (#159): eigene Rechtstexte einspielen – Datei hts-rechtstexte.tar.gz (oder Ordner rechtstexte/) neben diesem
# Ordner bzw. darin. Enthält impressum.html und/oder datenschutz.html; alte Dateien werden als .bak-<Datum> gesichert.
rechtstexte_einspielen() {
  local etc="$1" quelle="" tmp="" f n=0
  for c in "$HERE/hts-rechtstexte.tar.gz" "$HERE/../hts-rechtstexte.tar.gz" "$HERE/rechtstexte" "$HERE/../rechtstexte"; do
    if [[ -e "$c" ]]; then quelle="$c"; break; fi
  done
  if [[ -z "$quelle" ]]; then
    echo "Rechtstexte: keine Datei hts-rechtstexte.tar.gz gefunden – Impressum und Datenschutz bleiben unverändert."
    return 0
  fi
  tmp="$(mktemp -d)"
  if [[ -f "$quelle" ]]; then
    for f in impressum.html datenschutz.html; do
      tar -xzf "$quelle" -C "$tmp" --no-same-owner --no-same-permissions "$f" 2>/dev/null || true
    done
  else
    for f in impressum.html datenschutz.html; do [[ -f "$quelle/$f" ]] && cp "$quelle/$f" "$tmp/$f"; done
  fi
  for f in impressum.html datenschutz.html; do
    [[ -f "$tmp/$f" && ! -L "$tmp/$f" ]] || continue
    if [[ $(stat -c %s "$tmp/$f") -gt 524288 ]]; then echo "Rechtstexte: $f ist zu groß – übersprungen." >&2; continue; fi
    if grep -q "__AUSFUELLEN__" "$tmp/$f"; then echo "Rechtstexte: In $f stehen noch Platzhalter (__AUSFUELLEN__) – übersprungen." >&2; continue; fi
    if [[ -f "$etc/$f" ]] && cmp -s "$tmp/$f" "$etc/$f"; then echo "Rechtstexte: $f ist schon aktuell."; continue; fi
    if [[ -f "$etc/$f" ]]; then
      cp -p "$etc/$f" "$etc/$f.bak-$(date +%Y-%m-%d-%H%M)"
      install -o root -g hts -m 644 "$tmp/$f" "$etc/$f"
      echo "Rechtstexte: $f eingespielt (vorherige Fassung gesichert)."
    else
      install -o root -g hts -m 644 "$tmp/$f" "$etc/$f"
      echo "Rechtstexte: $f eingespielt."
    fi
    n=$((n + 1))
  done
  rm -rf "$tmp"
  return 0
}

# 1.11.0 (#188): SSH absichern – fail2ban (5 Fehlversuche in 10 Minuten → 1 Tag gesperrt), höchstens 3 Passwortversuche je
# Verbindung, Firewall-Bremse für SSH. Bricht bei Problemen nicht ab (nur Warnung), damit der Live-Server weiterläuft.
ssh_absichern() {
  local port
  port="$( (sshd -T 2>/dev/null || true) | awk '/^port /{print $2; exit}')"; port="${port:-22}"
  if ! command -v fail2ban-client >/dev/null 2>&1; then
    DEBIAN_FRONTEND=noninteractive apt-get install -y -q fail2ban >/dev/null 2>&1 || echo "Hinweis: fail2ban ließ sich nicht installieren." >&2
  fi
  if command -v fail2ban-client >/dev/null 2>&1; then
    mkdir -p /etc/fail2ban/jail.d
    if [[ -f /etc/fail2ban/jail.d/hts-ssh.local ]]; then
      sed -i 's/^bantime = .*/bantime = 1d/' /etc/fail2ban/jail.d/hts-ssh.local
    else
      printf '[sshd]\nenabled = true\nport = %s\nmaxretry = 5\nfindtime = 10m\nbantime = 1d\n' "$port" > /etc/fail2ban/jail.d/hts-ssh.local
    fi
    systemctl enable fail2ban >/dev/null 2>&1 || true
    systemctl restart fail2ban || echo "Hinweis: fail2ban startet nicht – siehe: journalctl -u fail2ban -n 30" >&2
    echo "SSH-Schutz: fail2ban aktiv (5 Fehlversuche in 10 Minuten → 1 Tag gesperrt)."
  fi
  if [[ -d /etc/ssh/sshd_config.d ]] && [[ ! -f /etc/ssh/sshd_config.d/hts.conf ]]; then
    printf 'MaxAuthTries 3\n' > /etc/ssh/sshd_config.d/hts.conf
    if sshd -t 2>/dev/null; then
      systemctl reload ssh 2>/dev/null || systemctl reload sshd 2>/dev/null || true
    else
      rm -f /etc/ssh/sshd_config.d/hts.conf
      echo "Hinweis: MaxAuthTries nicht gesetzt (SSH-Einstellungen nicht prüfbar)." >&2
    fi
  fi
  if command -v ufw >/dev/null 2>&1; then
    ufw limit "$port"/tcp >/dev/null 2>&1 || true
    ufw delete allow "$port"/tcp >/dev/null 2>&1 || true
  fi
  echo "Selbst ausgesperrt? In der Server-Konsole des Anbieters (z. B. netcup SCP) anmelden und eingeben:"
  echo "  fail2ban-client set sshd unbanip <eigene IP-Adresse>"
}

cp "$OPT/hts-server.jar" "$OPT/hts-server.jar.vorher"
install -o root -g root -m 644 "$HERE/hts-server.jar" "$OPT/hts-server.jar"
for f in LICENSE LIESMICH.txt; do [[ -f "$HERE/$f" ]] && install -o root -g root -m 644 "$HERE/$f" "$OPT/$f"; done
[[ -d "$HERE/Lizenzen" ]] && { rm -rf "$OPT/Lizenzen"; cp -r "$HERE/Lizenzen" "$OPT/Lizenzen"; }
install -o root -g root -m 755 "$HERE/update.sh" "$OPT/update.sh"
if [[ ! -f /etc/hts-server/spenden.html && -f "$HERE/vorlagen/spenden.html" ]]; then     # 1.8.0 (#153)
  install -o root -g hts -m 644 "$HERE/vorlagen/spenden.html" /etc/hts-server/spenden.html
fi
rechtstexte_einspielen /etc/hts-server
ssh_absichern
# 1.10.0 (#171): mehr Arbeitsspeicher für große Turniere (vorher 512 MB)
if grep -q -- "-Xmx512m" /etc/systemd/system/hts-server.service; then
  sed -i 's/-Xmx512m/-Xmx1g/' /etc/systemd/system/hts-server.service
  systemctl daemon-reload
  echo "Arbeitsspeicher des Live-Servers auf 1 GB erhöht."
fi
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
