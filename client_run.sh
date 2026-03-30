#!/usr/bin/env sh
set -eu

./compile.sh

javac -encoding UTF-8 -d classes -sourcepath src src/network/ClientMain.java
java -Dfile.encoding=UTF-8 -cp classes network.ClientMain "$@"
