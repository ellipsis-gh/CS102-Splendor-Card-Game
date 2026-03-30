#!/usr/bin/env sh
set -eu

CP="src:lib/jansi-2.4.1.jar"

javac -encoding UTF-8 -d classes -cp "$CP" src/Main.java
echo "Compilation successful"
