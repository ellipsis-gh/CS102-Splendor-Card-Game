#!/usr/bin/env sh
set -eu

./compile.sh
java -Dfile.encoding=UTF-8 -cp classes Main
