#!/bin/bash

# Enable globstar for Bash 4+ to use **
shopt -s globstar

# Directory containing JAR files
lib_dir="lib"

# Start with an empty classpath or include necessary directories
classpath="build"

# Loop over the JAR files in the directory
for jar in "$lib_dir"/*.jar; do
  # Append the JAR file to the classpath
  classpath="$classpath:$jar"
done

# Compile the Java files
javac -d build -cp "$classpath" src/**/*.java || { echo "Compilation failed"; exit 1; }
