@echo off
chcp 65001 >NUL

call compile.bat
if errorlevel 1 exit /b 1

javac -encoding UTF-8 -d classes -sourcepath src src\network\ClientMain.java
if errorlevel 1 exit /b 1

REM Optional args: host, port
REM Example: run_client.bat 127.0.0.1 5000
java -Dfile.encoding=UTF-8 -cp classes network.ClientMain %*
pause
