#!/bin/bash

# hvd Media Downloader - Build App Bundle Script
# This script builds the JAR and creates a macOS app bundle

echo "Building hvd macOS App Bundle..."

# Set up Maven path
export PATH=$PATH:$(pwd)/apache-maven-3.9.6/bin

# Clean and build JAR first
echo "Building JAR..."
mvn clean package -q

# Check if build was successful
if [ ! -f "target/hvd-1.0.0.jar" ]; then
    echo "Error: JAR file not found. Build may have failed."
    exit 1
fi

echo "JAR build successful!"

# Clean up any existing app bundle
echo "Cleaning existing app bundle..."
rm -rf hvd.app

# Create app bundle structure
echo "Creating app bundle structure..."
mkdir -p hvd.app/Contents/{MacOS,Resources}

# Copy Info.plist
echo "Creating Info.plist..."
cat > hvd.app/Contents/Info.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>hvd</string>
    <key>CFBundleIdentifier</key>
    <string>coredds.hvd</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>hvd</string>
    <key>CFBundleDisplayName</key>
    <string>hvd</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1.0.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.14</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.utilities</string>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2025 CoreDDS. All rights reserved.</string>
</dict>
</plist>
EOF

# Create executable script
echo "Creating executable script..."
cat > hvd.app/Contents/MacOS/hvd << 'EOF'
#!/bin/bash

# hvd Media Downloader App Bundle Launcher

# Get the directory where this script is located
SCRIPT_DIR="$(dirname "$0")"
APP_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
RESOURCES_DIR="$APP_DIR/Contents/Resources"

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
CLASSPATH="$RESOURCES_DIR/hvd-1.0.0.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/ch/qos/logback/logback-classic/1.4.8/logback-classic-1.4.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/ch/qos/logback/logback-core/1.4.8/logback-core-1.4.8.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar"

# Set up PATH to include common binary locations where yt-dlp might be installed
export PATH="/opt/homebrew/bin:/usr/local/bin:$HOME/.pyenv/shims:$HOME/.pyenv/bin:$HOME/.local/bin:/usr/bin:/bin:$PATH"

# Change to the Resources directory so relative paths work correctly
cd "$RESOURCES_DIR"

# Launch hvd with JavaFX runtime
exec java \
    --module-path "$MODULE_PATH" \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
    --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED \
    --add-opens javafx.graphics/javafx.stage=ALL-UNNAMED \
    --add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
    -cp "$CLASSPATH" \
    -Dprism.order=sw \
    -Djava.awt.headless=false \
    -Dfile.encoding=UTF-8 \
    coredds.hvd.HvdApplication
EOF

# Make executable script executable
chmod +x hvd.app/Contents/MacOS/hvd

# Create Resources directory and copy JAR file
echo "Creating Resources directory and copying JAR to app bundle..."
mkdir -p hvd.app/Contents/Resources/
cp target/hvd-1.0.0.jar hvd.app/Contents/Resources/

echo ""
echo "✅ App bundle created successfully!"
echo "📱 You can now double-click 'hvd.app' to run the application"
echo "📁 App bundle location: $(pwd)/hvd.app" 