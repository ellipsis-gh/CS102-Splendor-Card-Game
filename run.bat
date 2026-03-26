if not exist classes (
    echo Run compile.bat first.
    exit /b 1
)
java -cp classes Main
