@echo off
chcp 65001 >NUL
set "ROOT=%~dp0"
set "CP=%ROOT%classes;%ROOT%lib\jansi-2.4.1.jar"

call "%ROOT%compile.bat"
if errorlevel 1 exit /b 1

REM Optional: host port — e.g. run_client.bat 127.0.0.1 5000
java -Dfile.encoding=UTF-8 -cp "%CP%" network.ClientMain %*
pause
