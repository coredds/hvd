# hvd - Windows Setup Guide

> **Repository:** [https://github.com/coredds/hvd](https://github.com/coredds/hvd)

This guide explains how to set up and run the hvd media downloader on Windows.

## Prerequisites

### 1. Java 17 or Higher
- Download and install Java 17 or later from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
- Verify installation: `java --version`

### 2. Apache Maven
- Download from [Maven Website](https://maven.apache.org/download.cgi)
- Extract to a folder (e.g., `C:\apache-maven-3.9.6`)
- Add Maven `bin` directory to your PATH environment variable
- Verify installation: `mvn --version`

### 3. yt-dlp Installation

Choose one of these installation methods:

#### Option A: Python pip (Recommended)
```cmd
# Install Python from python.org if not already installed
pip install yt-dlp
```

#### Option B: Chocolatey
```cmd
# Install Chocolatey first: https://chocolatey.org/install
choco install yt-dlp
```

#### Option C: Scoop
```cmd
# Install Scoop first: https://scoop.sh
scoop install yt-dlp
```

#### Option D: Direct Download
- Download `yt-dlp.exe` from [GitHub Releases](https://github.com/yt-dlp/yt-dlp/releases)
- Place it in a folder that's in your PATH (e.g., `C:\Windows`)

### 4. FFmpeg Installation (Required for Advanced Features)

FFmpeg is required for embedding thumbnails, format conversion, and subtitle embedding.

#### Option A: Windows Package Manager (Recommended)
```cmd
winget install ffmpeg
```

#### Option B: Chocolatey
```cmd
choco install ffmpeg
```

#### Option C: Scoop
```cmd
scoop install ffmpeg
```

#### Option D: Direct Download
1. Download FFmpeg from [FFmpeg.org](https://ffmpeg.org/download.html#build-windows)
2. Extract to a folder (e.g., `C:\ffmpeg`)
3. Add the `bin` directory to your PATH environment variable (e.g., `C:\ffmpeg\bin`)
4. Verify installation: `ffmpeg -version`

> **Note:** Without FFmpeg, you can still download videos and audio, but features like thumbnail embedding will not work.

## Building and Running

### Quick Start
1. Open Command Prompt or PowerShell
2. Clone and navigate to the hvd project directory:
   ```cmd
   git clone https://github.com/coredds/hvd.git
   cd hvd
   ```
3. Run the build and launch script:
   ```cmd
   build-and-run.bat
   ```

### Alternative: Build Only
```cmd
build-jar.bat
```

### Alternative: Run from Source
```cmd
run-app.bat
```

## Windows-Specific Features

### JavaFX Platform Dependencies
The Windows batch scripts automatically use Windows-specific JavaFX dependencies:
- `javafx-controls-21.0.1-win.jar`
- `javafx-fxml-21.0.1-win.jar`
- `javafx-graphics-21.0.1-win.jar`
- `javafx-base-21.0.1-win.jar`

### yt-dlp Auto-Detection
The application automatically detects yt-dlp installations in common Windows locations:
- System PATH (`yt-dlp.exe`)
- Python Scripts directories
- Chocolatey installation (`C:\ProgramData\chocolatey\bin\`)
- Scoop installation (`%USERPROFILE%\scoop\apps\yt-dlp\`)
- Microsoft Store installation

### File Operations
- Downloads default to `%USERPROFILE%\Downloads`
- File opening uses Windows default applications
- Folder navigation uses Windows Explorer

## Troubleshooting

### Common Issues

#### 1. "yt-dlp not found"
**Solution:** Ensure yt-dlp is installed and in your PATH:
```cmd
where yt-dlp
yt-dlp --version
```

#### 2. "JavaFX runtime components are missing"
**Solution:** The batch scripts handle JavaFX dependencies automatically. If you see this error:
- Ensure you're using the provided batch scripts
- Check that Maven downloaded JavaFX dependencies: `mvn dependency:resolve`

#### 3. "Java not found"
**Solution:** 
- Verify Java installation: `java --version`
- Add Java to your PATH if needed
- Use full path to java.exe in batch scripts if necessary

#### 4. Build Failures
**Solution:**
- Ensure Maven is in PATH: `mvn --version`
- Run with verbose output: `mvn clean package -X`
- Check firewall/antivirus isn't blocking Maven downloads

### Performance Optimization

#### For better download performance:
1. **Windows Defender Exclusion:** Add the hvd directory to Windows Defender exclusions
2. **Firewall:** Ensure yt-dlp can access the internet
3. **Disk Space:** Ensure adequate free space in Downloads folder

## Advanced Configuration

### Custom yt-dlp Path
If yt-dlp is installed in a non-standard location:
1. Launch the application
2. Go to Settings tab
3. Set the custom path in "yt-dlp Executable Path"

### Custom Output Directory
1. Launch the application
2. Set your preferred download location in the "Output Directory" field
3. The setting will be remembered for future downloads

### Environment Variables
You can set these environment variables to customize behavior:
- `JAVA_HOME`: Java installation directory
- `M2_HOME`: Maven installation directory
- `HVD_OUTPUT_DIR`: Default download directory

## Building Distribution Package

To create a portable Windows distribution:

1. Build the JAR file:
   ```cmd
   build-jar.bat
   ```

2. Create a distribution folder:
   ```cmd
   mkdir hvd-windows-dist
   copy target\hvd-1.0.0.jar hvd-windows-dist\
   copy build-and-run.bat hvd-windows-dist\
   copy README.md hvd-windows-dist\
   ```

3. The resulting folder can be zipped and distributed to other Windows machines

## System Requirements

- **OS:** Windows 10 or later (Windows 7/8 may work but not tested)
- **Memory:** 512MB RAM minimum, 1GB recommended
- **Disk:** 100MB for application, additional space for downloads
- **Network:** Internet connection for downloading content

## Support

For Windows-specific issues:
1. Check this guide first
2. Verify all prerequisites are met
3. Run with verbose logging by adding `-Dlogging.level.coredds.hvd=DEBUG` to java command
4. Check the `application.log` file for detailed error information 