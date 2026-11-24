#!/bin/sh
# HTS – Hamburger Turniersoftware
# Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
#
# SPDX-License-Identifier: AGPL-3.0-or-later
# Dieses Programm ist freie Software unter der GNU Affero General
# Public License v3.0; Einzelheiten in der Datei LICENSE.
# Hamburger Turniersoftware (HTS) starten – Linux
cd "$(dirname "$0")" || exit 1
# 1.7.0 (#140): Absturzbericht der Java-Laufzeit in ~/.hts (kommt in den Fehlerbericht)
mkdir -p "$HOME/.hts" 2>/dev/null
JOPTS="-XX:ErrorFile=$HOME/.hts/hs_err_pid%p.log"
if [ -x "jre/bin/java" ]; then exec "jre/bin/java" "$JOPTS" -jar hts.jar "$@"; fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then exec "$JAVA_HOME/bin/java" "$JOPTS" -jar hts.jar "$@"; fi
if command -v java >/dev/null 2>&1; then exec java "$JOPTS" -jar hts.jar "$@"; fi
echo "HTS konnte nicht gestartet werden: Es wurde keine Java-Laufzeit gefunden."
echo "Der Ordner \"jre\" muss neben der Datei hts.jar liegen (Archiv vollständig entpacken)."
read -r _
