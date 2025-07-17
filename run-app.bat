@echo off
REM hvd Media Downloader Application Launcher (Windows)
REM This script handles Windows-specific JavaFX launch

echo Starting hvd...

REM Set up Maven path
set PATH=%PATH%;%CD%\apache-maven-3.9.6\bin

REM Check Java version
echo Java version:
java --version

REM Check if yt-dlp is available
echo Checking yt-dlp...
where yt-dlp >nul 2>&1
if %errorlevel% equ 0 (
    echo yt-dlp is available
    yt-dlp --version
) else (
    echo Warning: yt-dlp not found in PATH
)

REM Build the application if needed
echo Building application...
call mvn clean compile -q

REM Find JavaFX JARs in the local repository
set JAVAFX_VERSION=21.0.1
set M2_REPO=%USERPROFILE%\.m2\repository
set JAVAFX_PATH=%M2_REPO%\org\openjfx

REM Create module path for JavaFX (Windows platform)
set MODULE_PATH=%JAVAFX_PATH%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar

REM Create classpath for application dependencies
set CLASSPATH=target\classes
set CLASSPATH=%CLASSPATH%;%M2_REPO%\ch\qos\logback\logback-classic\1.4.8\logback-classic-1.4.8.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\ch\qos\logback\logback-core\1.4.8\logback-core-1.4.8.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\com\fasterxml\jackson\core\jackson-databind\2.15.2\jackson-databind-2.15.2.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\com\fasterxml\jackson\core\jackson-core\2.15.2\jackson-core-2.15.2.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\com\fasterxml\jackson\core\jackson-annotations\2.15.2\jackson-annotations-2.15.2.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\slf4j\slf4j-api\2.0.7\slf4j-api-2.0.7.jar

echo Launching application...

REM Launch with JavaFX runtime
java ^
    --module-path "%MODULE_PATH%" ^
    --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
    --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED ^
    --add-opens javafx.graphics/javafx.stage=ALL-UNNAMED ^
    --add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED ^
    -cp "%CLASSPATH%" ^
    -Dfile.encoding=UTF-8 ^
    coredds.hvd.HvdApplication

echo Application exited.
pause 