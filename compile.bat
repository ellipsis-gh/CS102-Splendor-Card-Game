@echo off
if not exist classes mkdir classes

echo Compiling Java files...
javac -d classes src\model\*.java src\network\*.java classes src/config/GameConfig.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful.
) else (
    echo Compilation failed.
    exit /b 1
)