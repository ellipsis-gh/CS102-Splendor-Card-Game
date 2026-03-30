@echo off
chcp 65001 >NUL
set "ROOT=%~dp0"
set "JANSI=%ROOT%lib\jansi-2.4.1.jar"
if not exist "%JANSI%" (
  echo Jansi JAR not found. Downloading to lib\ ...
  if not exist "%ROOT%lib" mkdir "%ROOT%lib"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fusesource/jansi/jansi/2.4.1/jansi-2.4.1.jar' -OutFile '%JANSI%' -UseBasicParsing"
  if errorlevel 1 (
    echo Failed to download jansi-2.4.1.jar — place it manually at %JANSI%
    exit /b 1
  )
)
if not exist "%ROOT%classes" mkdir "%ROOT%classes"

javac -encoding UTF-8 -d "%ROOT%classes" -sourcepath "%ROOT%src" -cp "%JANSI%" ^
  "%ROOT%src\Main.java" "%ROOT%src\network\ServerMain.java" "%ROOT%src\network\ClientMain.java"
if errorlevel 1 exit /b 1
echo Compilation successful
