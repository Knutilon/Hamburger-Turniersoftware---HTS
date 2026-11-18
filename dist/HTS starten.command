#!/bin/sh
# HTS – Hamburger Turniersoftware
# Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
#
# SPDX-License-Identifier: AGPL-3.0-or-later
# Dieses Programm ist freie Software unter der GNU Affero General
# Public License v3.0; Einzelheiten in der Datei LICENSE.
# Hamburger Turniersoftware (HTS) starten – macOS (Doppelklick im Finder)
cd "$(dirname "$0")" || exit 1
# Beim ersten Start: Sperre „aus dem Internet geladen“ für diesen Ordner aufheben
xattr -dr com.apple.quarantine . 2>/dev/null
if [ -x "jre/Contents/Home/bin/java" ]; then exec "jre/Contents/Home/bin/java" -jar hts.jar "$@"; fi
if [ -x "jre/bin/java" ]; then exec "jre/bin/java" -jar hts.jar "$@"; fi
if [ -x /usr/libexec/java_home ] && JH=$(/usr/libexec/java_home -v 21 2>/dev/null); then exec "$JH/bin/java" -jar hts.jar "$@"; fi
echo "HTS konnte nicht gestartet werden: Es wurde keine Java-Laufzeit gefunden."
echo "Der Ordner \"jre\" muss neben der Datei hts.jar liegen (Archiv vollständig entpacken)."
read -r _
