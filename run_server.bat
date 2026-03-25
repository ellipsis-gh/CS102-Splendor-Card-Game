@echo off
cd /d "%~dp0"
if not exist classes (
    echo Run compile.bat first.
    pause
    exit /b 1
)
echo Starting Splendor server...
start "Splendor Server" java -cp classes network.ServerMain
timeout /t 2 /nobreak > nul
echo Launching client for you (Player 1)...
java -cp classes network.ClientMain 127.0.0.1
pause