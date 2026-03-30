#!/usr/bin/env sh
set -eu

./compile.sh

CP="src:lib/jansi-2.4.1.jar"

javac -encoding UTF-8 -d classes -cp "$CP" src/network/ServerMain.java
java -Dfile.encoding=UTF-8 -cp "classes:lib/jansi-2.4.1.jar" network.ServerMain "$@"
