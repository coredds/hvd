#!/bin/bash

# hvd Media Downloader Application Launcher
# This script handles macOS-specific JavaFX launch issues

echo "Starting hvd..."

# Set up Maven path
export PATH=$PATH:$(pwd)/apache-maven-3.9.6/bin

# Check Java version
echo "Java version:"
java --version

# Check if yt-dlp is available
echo "Checking yt-dlp..."
if command -v yt-dlp &> /dev/null; then
    echo "yt-dlp is available: $(yt-dlp --version)"
else
    echo "Warning: yt-dlp not found in PATH"
fi

# Build the application if needed
echo "Building application..."
mvn clean compile -q

# Find JavaFX JARs in the local repository
JAVAFX_VERSION="21.0.1"
M2_REPO="$HOME/.m2/repository"
JAVAFX_PATH="$M2_REPO/org/openjfx"

# Create module path for JavaFX
MODULE_PATH="$JAVAFX_PATH/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION-mac-aarch64.jar"
MODULE_PATH="$MODULE_PATH:$JAVAFX_PATH/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION-mac-aarch64.jar"
MODULE_PATH="$MODULE_PATH:$JAVAFX_PATH/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION-mac-aarch64.jar"
MODULE_PATH="$MODULE_PATH:$JAVAFX_PATH/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION-mac-aarch64.jar"

# Create classpath for application dependencies
CLASSPATH="target/classes"
CLASSPATH="$CLASSPATH:$M2_REPO/ch/qos/logback/logback-classic/1.4.8/logback-classic-1.4.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/ch/qos/logback/logback-core/1.4.8/logback-core-1.4.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar"

echo "Launching application..."
echo "If the application doesn't appear, check the Console app for crash logs."

# Launch with JavaFX runtime
java \
    --module-path "$MODULE_PATH" \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
    --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED \
    --add-opens javafx.graphics/javafx.stage=ALL-UNNAMED \
    --add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
    -cp "$CLASSPATH" \
    -Dprism.order=sw \
    -Djava.awt.headless=false \
    -Dprism.verbose=true \
    -Dfile.encoding=UTF-8 \
    coredds.hvd.HvdApplication

echo "Application exited." 