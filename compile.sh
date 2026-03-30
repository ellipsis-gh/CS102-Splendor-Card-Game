#!/usr/bin/env sh
set -eu

javac -encoding UTF-8 -d classes -sourcepath src src/Main.java
echo "Compilation successful"
