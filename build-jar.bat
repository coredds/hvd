@echo off
REM hvd Media Downloader - Build JAR Script (Windows)
REM This script builds the JAR file only

echo Building hvd JAR...

REM Set up Maven path (assuming Maven is in PATH or use local installation)
set PATH=%PATH%;%CD%\apache-maven-3.9.6\bin

REM Clean and build JAR
echo Building JAR with dependencies...
call mvn clean package -q

REM Check if build was successful
if not exist "target\hvd-1.0.0.2.jar" (
    echo Error: JAR file not found. Build may have failed.
    exit /b 1
)

echo Build successful! JAR created: target\hvd-1.0.0.2.jar
echo To run the application, use: build-and-run.bat
pause 