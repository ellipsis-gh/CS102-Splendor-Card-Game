@echo off
chcp 65001 >NUL

javac -encoding UTF-8 -d classes -sourcepath src src\Main.java
if errorlevel 1 exit /b 1
echo Compilation successful
