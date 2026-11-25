#!/usr/bin/env bash
# HTS – Hamburger Turniersoftware: Einrichtung des Live-Servers (#101)
# Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Richtet einen frischen Ubuntu-24.04-Server (z. B. netcup VPS) für die Live-Ergebnisse von HTS ein:
#   Java 21, Caddy (automatisches HTTPS), Firewall, automatische Sicherheitsupdates, HTS-Live-Server als Dienst.
# Aufruf als root im entpackten Ordner:   ./install.sh [domain]          (Vorgabe: hts-livescoring.de)
# Erneut ausführen ist gefahrlos: Einstellungen, Schlüssel, Impressum und Turniere bleiben erhalten.
# Neuen geheimen Schlüssel erzeugen:      ./install.sh --neuer-schluessel [domain]

set -euo pipefail

DOMAIN="hts-livescoring.de"
NEW_KEY=0
for a in "$@"; do
  case "$a" in
    --neuer-schluessel) NEW_KEY=1 ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    -*) echo "Unbekannte Option: $a" >&2; exit 2 ;;
    *) DOMAIN="$a" ;;
  esac
done
if ! [[ "$DOMAIN" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
  echo "Ungültige Domain: $DOMAIN" >&2; exit 2
fi

HERE="$(cd "$(dirname "$0")" && pwd)"
OPT=/opt/hts-server
ETC=/etc/hts-server
DATA=/var/lib/hts-server
PORT=8090

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

schritt() { printf '\n\033[1;32m==> %s\033[0m\n' "$*"; }
warnung() { printf '\033[1;33mHinweis: %s\033[0m\n' "$*"; }

[[ $EUID -eq 0 ]] || { echo "Bitte als root ausführen (z. B. mit sudo ./install.sh)." >&2; exit 1; }
[[ -f "$HERE/hts-server.jar" ]] || { echo "hts-server.jar fehlt im Ordner $HERE." >&2; exit 1; }
if [[ -f "$HERE/SHA256SUMS" ]]; then
  (cd "$HERE" && sha256sum --quiet -c SHA256SUMS) || { echo "Prüfsumme stimmt nicht – Paket beschädigt?" >&2; exit 1; }
fi
. /etc/os-release
if [[ "${ID:-}" != "ubuntu" && "${ID:-}" != "debian" ]]; then
  echo "Dieses Skript ist für Ubuntu (24.04) bzw. Debian gedacht, gefunden: ${PRETTY_NAME:-unbekannt}." >&2; exit 1
fi
[[ "${VERSION_ID:-}" == "24.04" ]] || warnung "getestet für Ubuntu 24.04, gefunden: ${PRETTY_NAME:-unbekannt} – es wird trotzdem versucht."

export DEBIAN_FRONTEND=noninteractive

schritt "Pakete aktualisieren und Java 21, Firewall, automatische Updates installieren"
apt-get update -q
apt-get -y -q full-upgrade
apt-get install -y -q openjdk-21-jre-headless ufw unattended-upgrades curl gnupg ca-certificates openssl apt-transport-https

schritt "Caddy (Webserver mit automatischem HTTPS) aus der offiziellen Paketquelle installieren"
if [[ ! -f /usr/share/keyrings/caddy-stable-archive-keyring.gpg ]]; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
fi
if [[ ! -f /etc/apt/sources.list.d/caddy-stable.list ]]; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' > /etc/apt/sources.list.d/caddy-stable.list
fi
chmod o+r /usr/share/keyrings/caddy-stable-archive-keyring.gpg /etc/apt/sources.list.d/caddy-stable.list
apt-get update -q
apt-get install -y -q caddy

schritt "Benutzer und Ordner anlegen"
id hts &>/dev/null || useradd --system --home-dir "$DATA" --no-create-home --shell /usr/sbin/nologin hts
install -d -o root -g root -m 755 "$OPT"
install -d -o root -g hts -m 750 "$ETC"
install -d -o hts -g hts -m 750 "$DATA"
install -o root -g root -m 644 "$HERE/hts-server.jar" "$OPT/hts-server.jar"
for f in LICENSE LIESMICH.txt; do [[ -f "$HERE/$f" ]] && install -o root -g root -m 644 "$HERE/$f" "$OPT/$f"; done
[[ -d "$HERE/Lizenzen" ]] && { rm -rf "$OPT/Lizenzen"; cp -r "$HERE/Lizenzen" "$OPT/Lizenzen"; }
install -o root -g root -m 755 "$HERE/update.sh" "$OPT/update.sh"

schritt "Einstellungen und geheimer Schlüssel"
KEY=""
if [[ ! -f "$ETC/server.properties" || $NEW_KEY -eq 1 ]]; then
  KEY="$(openssl rand -base64 36 | tr '+/' '-_' | tr -d '=\n')"
  HASH="$(printf '%s' "$KEY" | sha256sum | cut -d' ' -f1)"
  RET=90; MAXMB=25
  if [[ -f "$ETC/server.properties" ]]; then
    RET="$(sed -n 's/^retention-days=//p' "$ETC/server.properties")"; RET="${RET:-90}"
    MAXMB="$(sed -n 's/^max-upload-mb=//p' "$ETC/server.properties")"; MAXMB="${MAXMB:-25}"
  fi
  cat > "$ETC/server.properties" <<EOF
# HTS-Live-Server – Einstellungen (nach Änderungen: systemctl restart hts-server)
# Nur der SHA-256-Prüfwert des geheimen Schlüssels steht hier, nicht der Schlüssel selbst.
key-sha256=$HASH
bind=127.0.0.1
port=$PORT
data=$DATA
content=$ETC
# Turniere werden so viele Tage nach der letzten Änderung automatisch gelöscht:
retention-days=$RET
# Größte erlaubte Turnierdatei in MB:
max-upload-mb=$MAXMB
site-title=HTS Live-Scoring
EOF
fi
chown root:hts "$ETC/server.properties"; chmod 640 "$ETC/server.properties"
for f in impressum.html datenschutz.html spenden.html; do
  if [[ ! -f "$ETC/$f" && -f "$HERE/vorlagen/$f" ]]; then
    install -o root -g hts -m 644 "$HERE/vorlagen/$f" "$ETC/$f"
    sed -i "s/__DOMAIN__/$DOMAIN/g" "$ETC/$f"
  fi
done
rechtstexte_einspielen "$ETC"                                   # 1.8.0 (#159)

schritt "Dienst hts-server einrichten (startet automatisch, auch nach Neustart)"
cat > /etc/systemd/system/hts-server.service <<EOF
[Unit]
Description=HTS Live-Server (Hamburger Turniersoftware)
After=network-online.target
Wants=network-online.target

[Service]
User=hts
Group=hts
ExecStart=/usr/bin/java -Xmx512m -Djava.awt.headless=true -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar $OPT/hts-server.jar $ETC/server.properties
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
PrivateDevices=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictSUIDSGID=true
LockPersonality=true
ReadWritePaths=$DATA
ReadOnlyPaths=$ETC $OPT

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable hts-server >/dev/null
systemctl restart hts-server

schritt "Caddy für $DOMAIN einrichten (HTTPS-Zertifikat wird automatisch geholt)"
if [[ -f /etc/caddy/Caddyfile && ! -f /etc/caddy/Caddyfile.vor-hts ]] && ! grep -q "HTS-Live-Server" /etc/caddy/Caddyfile; then
  cp /etc/caddy/Caddyfile /etc/caddy/Caddyfile.vor-hts
fi
cat > /etc/caddy/Caddyfile <<EOF
# HTS-Live-Server – eingerichtet von install.sh. Keine Zugriffsprotokolle (kein "log"-Eintrag).
$DOMAIN {
	encode zstd gzip
	request_body {
		max_size 30MB
	}
	header Strict-Transport-Security "max-age=31536000"
	reverse_proxy 127.0.0.1:$PORT
}

www.$DOMAIN {
	redir https://$DOMAIN{uri} permanent
}
EOF
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
systemctl enable caddy >/dev/null
systemctl reload caddy 2>/dev/null || systemctl restart caddy

schritt "Firewall: nur SSH (22), HTTP (80) und HTTPS (443) erlauben"
SSH_PORT="$( (sshd -T 2>/dev/null || true) | awk '/^port /{print $2; exit}')"; SSH_PORT="${SSH_PORT:-22}"
ufw allow "$SSH_PORT"/tcp >/dev/null
ufw allow 80/tcp >/dev/null
ufw allow 443/tcp >/dev/null
ufw --force enable >/dev/null

schritt "Automatische Sicherheitsupdates (Neustart bei Bedarf um 4:30 Uhr)"
cat > /etc/apt/apt.conf.d/20auto-upgrades <<EOF
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
cat > /etc/apt/apt.conf.d/52hts-unattended <<EOF
Unattended-Upgrade::Origins-Pattern { "origin=cloudsmith/caddy/stable"; };
Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-Time "04:30";
EOF
timedatectl set-timezone Europe/Berlin 2>/dev/null || true

schritt "Prüfen"
ok=0
for _ in $(seq 1 30); do
  if curl -fs "http://127.0.0.1:$PORT/" >/dev/null; then ok=1; break; fi
  sleep 1
done
if [[ $ok -eq 1 ]]; then echo "HTS-Live-Server antwortet."; else warnung "HTS-Live-Server antwortet nicht – siehe: journalctl -u hts-server -n 50"; fi
MYIPS="$(hostname -I 2>/dev/null || true)"
DNSIP="$( (getent ahostsv4 "$DOMAIN" 2>/dev/null || true) | awk 'NR==1{print $1}')"
if [[ -z "$DNSIP" ]]; then
  warnung "$DOMAIN ist im DNS noch nicht zu finden. Sobald der A-Eintrag wirkt, holt Caddy das Zertifikat von selbst."
elif [[ " $MYIPS " != *" $DNSIP "* ]]; then
  warnung "$DOMAIN zeigt auf $DNSIP, dieser Server hat aber: $MYIPS – bitte den A-Eintrag prüfen."
else
  echo "$DOMAIN zeigt auf diesen Server ($DNSIP)."
fi

printf '\n\033[1m================================================================\033[0m\n'
echo "Fertig. Live-Seiten: https://$DOMAIN"
echo
if [[ -n "$KEY" ]]; then
  echo "GEHEIMER SCHLÜSSEL (wird nur jetzt angezeigt – bitte sicher notieren):"
  echo
  printf '    \033[1m%s\033[0m\n' "$KEY"
  echo
  # shellcheck disable=SC1111
  echo "In HTS: Turnierübersicht → Reiter „Webserver“ → Adresse https://$DOMAIN und diesen Schlüssel"
  # shellcheck disable=SC1111
  echo "eintragen → „Verbindung testen“."
else
  echo "Der geheime Schlüssel ist unverändert. Neuen Schlüssel erzeugen: ./install.sh --neuer-schluessel"
fi
echo
if grep -q "__AUSFUELLEN__" "$ETC/impressum.html" "$ETC/datenschutz.html" 2>/dev/null; then
  warnung "Impressum und Datenschutzerklärung sind noch Vorlagen. Bitte ausfüllen:"
  echo "    nano $ETC/impressum.html"
  echo "    nano $ETC/datenschutz.html"
  echo "(Änderungen gelten sofort, kein Neustart nötig.)"
fi
printf '\033[1m================================================================\033[0m\n'
