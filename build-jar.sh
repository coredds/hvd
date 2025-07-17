#!/bin/bash

# hvd Media Downloader - Build JAR Script
# This script builds the JAR file only

echo "Building hvd JAR..."

# Set up Maven path
export PATH=$PATH:$(pwd)/apache-maven-3.9.6/bin

# Clean and build JAR
echo "Building JAR with dependencies..."
mvn clean package -q

# Check if build was successful
if [ ! -f "target/hvd-1.0.0.jar" ]; then
    echo "Error: JAR file not found. Build may have failed."
    exit 1
fi

echo "Build successful! JAR created: target/hvd-1.0.0.jar"
echo "To run the application, use: ./build-and-run.sh" 