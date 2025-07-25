@echo off
echo Creating Windows distribution for hvd...

REM Clean and build the project
echo Building project...
call apache-maven-3.9.6\bin\mvn.cmd clean package -q
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)

REM Create distribution directory
echo Creating distribution directory...
if exist "dist" rmdir /s /q "dist"
mkdir "dist"
mkdir "dist\lib"

REM Copy main JAR
echo Copying main JAR...
copy "target\hvd-1.0.0.2.jar" "dist\"

REM Copy dependencies
echo Copying dependencies...
call apache-maven-3.9.6\bin\mvn.cmd dependency:copy-dependencies -DoutputDirectory=dist\lib -q

REM Create launcher batch file
echo Creating batch launcher...
(
echo @echo off
echo.
echo REM Check if Java is available
echo java --version ^>nul 2^>^&1
echo if errorlevel 1 ^(
echo     echo Error: Java is not installed or not in PATH
echo     echo Please install Java 17 or higher from https://adoptium.net/
echo     pause
echo     exit /b 1
echo ^)
echo.
echo REM Launch application with JavaFX ^(using Windows-specific JARs^) - no console window
echo javaw --module-path "lib\javafx-controls-21.0.1-win.jar;lib\javafx-fxml-21.0.1-win.jar;lib\javafx-graphics-21.0.1-win.jar;lib\javafx-base-21.0.1-win.jar" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -cp "hvd-1.0.0.2.jar;lib\jackson-databind-2.15.2.jar;lib\jackson-core-2.15.2.jar;lib\jackson-annotations-2.15.2.jar;lib\logback-classic-1.4.8.jar;lib\logback-core-1.4.8.jar;lib\slf4j-api-2.0.7.jar" coredds.hvd.HvdApplication
echo.
echo if errorlevel 1 ^(
echo     echo Application exited with error
echo     echo If you see JavaFX errors, please ensure Java 17+ is installed with JavaFX support
echo     pause
echo ^)
) > "dist\hvd.bat"

REM Create silent VBS launcher
echo Creating silent VBS launcher...
(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo.
echo ' Get the directory where this script is located
echo scriptDir = CreateObject^("Scripting.FileSystemObject"^).GetParentFolderName^(WScript.ScriptFullName^)
echo.
echo ' Change to the application directory
echo WshShell.CurrentDirectory = scriptDir
echo.
echo ' Build the Java command
echo javaCmd = "javaw --module-path ""lib\javafx-controls-21.0.1-win.jar;lib\javafx-fxml-21.0.1-win.jar;lib\javafx-graphics-21.0.1-win.jar;lib\javafx-base-21.0.1-win.jar"" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -cp ""hvd-1.0.0.2.jar;lib\jackson-databind-2.15.2.jar;lib\jackson-core-2.15.2.jar;lib\jackson-annotations-2.15.2.jar;lib\logback-classic-1.4.8.jar;lib\logback-core-1.4.8.jar;lib\slf4j-api-2.0.7.jar"" coredds.hvd.HvdApplication"
echo.
echo ' Run the command silently ^(0 = hidden window, false = don't wait^)
echo WshShell.Run javaCmd, 0, false
) > "dist\hvd.vbs"

REM Create README for distribution
echo Creating README...
(
echo Holyrics Video Downloader - Windows Distribution
echo ===============================================
echo.
echo Requirements:
echo - Java 17 or higher
echo - yt-dlp installed and accessible from PATH
echo.
echo To run ^(choose one^):
echo 1. RECOMMENDED: Double-click "Holyrics Video Downloader.lnk" ^(desktop shortcut with icon^)
echo 2. Alternative: Double-click hvd.vbs ^(silent launch, no console window^)
echo 3. Troubleshooting: Double-click hvd.bat ^(shows console output^)
echo.
echo Files included:
echo - Holyrics Video Downloader.lnk  ^(Desktop shortcut - RECOMMENDED^)
echo - hvd-1.0.0.2.jar               ^(Main application^)
echo - lib\                          ^(Dependencies^)
echo - hvd.vbs                       ^(Silent launcher^)
echo - hvd.bat                       ^(Console launcher^)
echo - 128.ico                       ^(Application icon^)
echo - icons\                        ^(Additional icons^)
echo.
echo Installation:
echo 1. Extract this folder to your desired location
echo 2. Copy "Holyrics Video Downloader.lnk" to your Desktop ^(optional^)
echo 3. Install yt-dlp: pip install yt-dlp
echo.
echo Troubleshooting:
echo - If shortcut doesn't work, try hvd.vbs directly
echo - If you see Java errors, ensure Java 17+ is installed
echo - Settings are saved in hvd-preferences.properties
echo.
echo Visit: https://github.com/coredds/hvd
) > "dist\README.txt"

REM Copy icons to distribution
echo Copying icons...
if exist "src\main\resources\icons" (
    mkdir "dist\icons"
    copy "src\main\resources\icons\*.png" "dist\icons\" >nul 2>&1
    copy "src\main\resources\icons\128.ico" "dist\" >nul 2>&1
)

REM Create professional shortcut with icon
echo Creating application shortcut...
powershell -Command "$WScriptShell = New-Object -ComObject WScript.Shell; $Shortcut = $WScriptShell.CreateShortcut('%CD%\dist\Holyrics Video Downloader.lnk'); $Shortcut.TargetPath = '%CD%\dist\hvd.vbs'; $Shortcut.WorkingDirectory = '%CD%\dist'; $Shortcut.IconLocation = '%CD%\dist\128.ico'; $Shortcut.Description = 'Holyrics Video Downloader - Download videos and audio from YouTube and other platforms'; $Shortcut.Save()"

echo.
echo ✓ Windows distribution created in 'dist' folder
echo.
echo Contents:
dir /b "dist"
echo.
echo ✓ Professional shortcut created: "Holyrics Video Downloader.lnk"
echo.
echo To distribute: 
echo 1. Zip the 'dist' folder and send to users
echo 2. Users can extract and double-click the shortcut to run
echo 3. Optionally, users can copy the shortcut to their Desktop
echo.
pause 