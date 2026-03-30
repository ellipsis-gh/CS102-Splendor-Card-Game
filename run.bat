@echo off
chcp 65001 >NUL

call compile.bat
if errorlevel 1 exit /b 1

java -Dfile.encoding=UTF-8 -cp classes Main
