@echo off
if not exist classes mkdir classes
javac -d classes model\Token.java model\Card.java model\Noble.java model\Deck.java model\Board.java model\Player.java model\CardLoader.java model\SplendorAI.java model\Main.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful.
) else (
    echo Compilation failed.
    exit /b 1
)
