#!/bin/sh
# Hamburger Turniersoftware (HTS) starten – Linux
cd "$(dirname "$0")" || exit 1
if [ -x "jre/bin/java" ]; then exec "jre/bin/java" -jar hts.jar "$@"; fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then exec "$JAVA_HOME/bin/java" -jar hts.jar "$@"; fi
if command -v java >/dev/null 2>&1; then exec java -jar hts.jar "$@"; fi
echo "HTS konnte nicht gestartet werden: Es wurde keine Java-Laufzeit gefunden."
echo "Der Ordner \"jre\" muss neben der Datei hts.jar liegen (Archiv vollständig entpacken)."
read -r _
