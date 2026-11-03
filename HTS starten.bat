@echo off
rem Hamburger Turniersoftware (HTS) starten
cd /d "%~dp0"

rem 1. Mitgelieferte Java-Laufzeit im Ordner "jre"
if exist "jre\bin\javaw.exe" (
  start "" "jre\bin\javaw.exe" -jar hts.jar
  exit /b
)

rem 2. Installiertes Java ueber JAVA_HOME
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" (
  start "" "%JAVA_HOME%\bin\javaw.exe" -jar hts.jar
  exit /b
)

rem 3. Java im Suchpfad
where javaw >nul 2>nul
if %errorlevel%==0 (
  start "" javaw -jar hts.jar
  exit /b
)

echo.
echo  HTS konnte nicht gestartet werden: Es wurde keine Java-Laufzeit gefunden.
echo.
echo  Der Ordner "jre" muss neben der Datei hts.jar liegen.
echo  Bitte das ZIP-Archiv vollstaendig entpacken (nicht direkt aus dem ZIP starten).
echo.
pause
