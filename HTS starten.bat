@echo off
rem HTS - Hamburger Turniersoftware
rem Copyright 2026 by Richard Johan Frederich Michaelis, Hamburg, Germany
rem
rem SPDX-License-Identifier: AGPL-3.0-or-later
rem Dieses Programm ist freie Software unter der GNU Affero General
rem Public License v3.0; Einzelheiten in der Datei LICENSE.
rem Hamburger Turniersoftware (HTS) starten
cd /d "%~dp0"
rem 1.7.0 (#140): Absturzbericht der Java-Laufzeit nach %USERPROFILE%\.hts (kommt in den Fehlerbericht)
if not exist "%USERPROFILE%\.hts" mkdir "%USERPROFILE%\.hts"
set "HTSOPTS=-XX:ErrorFile=%USERPROFILE%\.hts\hs_err_pid%%p.log"

rem 1. Mitgelieferte Java-Laufzeit im Ordner "jre"
if exist "jre\bin\javaw.exe" (
  start "" "jre\bin\javaw.exe" "%HTSOPTS%" -jar hts.jar
  exit /b
)

rem 2. Installiertes Java ueber JAVA_HOME
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" (
  start "" "%JAVA_HOME%\bin\javaw.exe" "%HTSOPTS%" -jar hts.jar
  exit /b
)

rem 3. Java im Suchpfad
where javaw >nul 2>nul
if %errorlevel%==0 (
  start "" javaw "%HTSOPTS%" -jar hts.jar
  exit /b
)

echo.
echo  HTS konnte nicht gestartet werden: Es wurde keine Java-Laufzeit gefunden.
echo.
echo  Der Ordner "jre" muss neben der Datei hts.jar liegen.
echo  Bitte das ZIP-Archiv vollstaendig entpacken (nicht direkt aus dem ZIP starten).
echo.
pause
