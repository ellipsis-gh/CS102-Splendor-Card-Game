@echo off
chcp 65001 >NUL
set "ROOT=%~dp0"
set "CP=%ROOT%classes;%ROOT%lib\jansi-2.4.1.jar"

call "%ROOT%compile.bat"
if errorlevel 1 exit /b 1

java -Dfile.encoding=UTF-8 -cp "%CP%" Main
