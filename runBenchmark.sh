#!/bin/bash

# Directory containing JAR files
lib_dir="lib"

# Start with an empty classpath
classpath="build"

# Loop over the JAR files in the directory
for jar in "$lib_dir"/*.jar; do
  # Append the JAR file to the classpath
  classpath="$classpath:$jar"
done

# Execute the Java command with the constructed classpath
java -cp "$classpath" benchmarker.Benchmarker