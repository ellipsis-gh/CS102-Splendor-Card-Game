@echo off
chcp 65001 >NUL

call compile.bat
if errorlevel 1 exit /b 1

javac -encoding UTF-8 -d classes -sourcepath src src\network\ServerMain.java
if errorlevel 1 exit /b 1

REM Optional arg: port
REM Example: run_server.bat 5000
java -Dfile.encoding=UTF-8 -cp classes network.ServerMain %*
pause
