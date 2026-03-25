#!/bin/bash

# Create output directory
mkdir -p classes

# Compile Java files
javac -d classes src/config/GameConfig.java src/network/*.java src/model/*.java

echo "Compilation complete."
