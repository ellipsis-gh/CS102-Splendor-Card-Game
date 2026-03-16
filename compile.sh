#!/bin/bash
set -e

mkdir -p classes
javac -d classes src/model/*.java src/network/*.java

echo "Compilation successful."