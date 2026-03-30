#!/usr/bin/env sh
set -eu

./compile.sh
java -Dfile.encoding=UTF-8 -cp "classes:lib/jansi-2.4.1.jar" Main
