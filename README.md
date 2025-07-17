# hvd

A modern JavaFX-based media downloader built on [yt-dlp](https://github.com/yt-dlp/yt-dlp), allowing you to download videos and audio from YouTube and other platforms with ease.

**✅ Cross-Platform Support:** Windows, macOS, and Linux compatible

## Features

- **Intuitive Interface**: Clean and modern JavaFX UI with tabbed layout
- **Video & Audio Downloads**: Choose between video downloads or audio-only extraction
- **Multiple Format Support**: Support for various audio formats (MP3, AAC, M4A, OPUS, FLAC, WAV)
- **Batch Downloads**: Add multiple URLs and download them sequentially
- **Real-time Progress**: Visual progress bars and live logging
- **Flexible Configuration**: Customize output directory, yt-dlp path, and download options
- **Advanced Options**: Embed subtitles, thumbnails, and metadata

## Platform-Specific Setup

**🪟 Windows Users:** See [WINDOWS-SETUP.md](WINDOWS-SETUP.md) for detailed Windows installation guide with batch scripts.

**🍎 macOS Users:** Use the shell scripts (`.sh` files) provided.

**🐧 Linux Users:** Use the shell scripts (`.sh` files) provided.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17 or higher**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)

2. **Apache Maven 3.6.0 or higher**
   - Download from [Maven Official Site](https://maven.apache.org/download.cgi)

3. **yt-dlp**
   - Install via pip: `pip install yt-dlp`
   - Or download from [GitHub releases](https://github.com/yt-dlp/yt-dlp/releases)

### Dependencies (automatically handled by Maven)

- **JavaFX 17**: UI framework
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging framework

## Installation and Setup

### 1. Install yt-dlp

```bash
# Using pip (recommended)
pip install yt-dlp

# Or using pip3
pip3 install yt-dlp

# Verify installation
yt-dlp --version
```

### 2. Clone and Build the Application

```bash
# Clone the repository
git clone https://github.com/coredds/hvd.git
cd hvd

# Build the project
mvn clean compile

# Run the application
mvn javafx:run
```

### 3. Alternative: Create an Executable JAR

```bash
# Create a JAR with dependencies
mvn clean package

# Run the JAR (requires proper JavaFX runtime)
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/hvd-1.0.0.jar
```

## Usage

### First Time Setup

1. **Launch the application**
   ```bash
   mvn javafx:run
   ```

2. **Configure yt-dlp path** (Settings tab)
   - If yt-dlp is in your PATH, leave as "yt-dlp"
   - Otherwise, browse to the yt-dlp executable location
   - Click "Test" to verify the configuration

3. **Set default output directory** (Settings tab)
   - Choose where downloaded files should be saved
   - Default is your system's Downloads folder

### Downloading Videos

1. **Switch to Downloads tab**

2. **Enter video URLs**
   - Paste one or more video URLs (one per line)
   - Supports YouTube, Vimeo, and other yt-dlp compatible sites

3. **Configure download options**
   - **Download Type**: Choose Video or Audio Only
   - **Audio Format**: Select format for audio downloads (MP3, AAC, etc.)
   - **Output Directory**: Choose destination folder
   - **Additional Options**: 
     - Embed Subtitles
     - Embed Thumbnail
     - Add Metadata

4. **Add to queue and start download**
   - Click "Add to Queue" to add URLs to the download list
   - Click "Start All" to begin downloading
   - Monitor progress in the download table

### Monitoring Downloads

- **Download Table**: Shows URL, title, format, status, and progress
- **Logs Tab**: Real-time output from yt-dlp
- **Status Updates**: Downloads show as Queued → Downloading → Completed/Error

## Project Structure

```
hvd/
├── src/
│   └── main/
│       ├── java/
│       │   └── coredds/
│       │       └── hvd/
│       │           ├── HvdApplication.java    # Main application class
│       │           ├── controller/
│       │           │   ├── MainController.java     # Main UI controller  
│       │           │   └── ProgressCell.java       # Custom progress cell
│       │           ├── model/
│       │           │   └── DownloadItem.java       # Download item model
│       │           └── service/
│       │                   └── YtDlpService.java       # yt-dlp integration service
│       └── resources/
│           ├── css/
│           │   └── styles.css                          # Application styling
│           └── fxml/
│               └── main-view.fxml                      # Main UI layout
├── pom.xml                                             # Maven project configuration
└── README.md                                           # This file
```

## Troubleshooting

### Common Issues

1. **"yt-dlp not found"**
   - Ensure yt-dlp is installed and in your PATH
   - Or configure the full path in Settings → yt-dlp Configuration

2. **JavaFX runtime errors**
   - Ensure you're using JDK 17 or higher
   - JavaFX is included as a dependency and should work automatically

3. **Download failures**
   - Check the Logs tab for detailed error messages
   - Verify the video URL is accessible
   - Some sites may require additional yt-dlp options

4. **Permission errors**
   - Ensure write permissions to the output directory
   - On macOS/Linux, you may need to run with appropriate permissions

### System-Specific Notes

#### macOS
- You may need to allow the application in System Preferences → Security & Privacy

#### Linux
- Ensure you have the necessary audio/video codecs installed for format conversion

#### Windows
- Windows Defender may flag the application - add it to exceptions if needed

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/coredds/hvd.git
cd hvd

# Install dependencies and compile
mvn clean compile

# Run in development mode
mvn javafx:run

# Create distribution package
mvn clean package
```

### Adding Features

The application is designed to be extensible:

- **New yt-dlp options**: Add to `YtDlpService.buildCommand()`
- **UI improvements**: Modify `main-view.fxml` and `MainController.java`
- **New download formats**: Extend the `DownloadItem` model
- **Advanced features**: Add new service classes and integrate with the controller

## Contributing

1. Fork the [hvd repository](https://github.com/coredds/hvd)
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source. Please check the LICENSE file for details.

## Acknowledgments

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - The excellent command-line program that powers this GUI
- [JavaFX](https://openjfx.io/) - The UI framework used for this application
- [Maven](https://maven.apache.org/) - Build and dependency management

## Support

If you encounter issues or have questions:

1. Check the troubleshooting section above
2. Review the yt-dlp documentation for format and option details
3. Open an issue on the [hvd repository](https://github.com/coredds/hvd/issues)

---

**Note**: This application is a GUI wrapper for yt-dlp. All download capabilities and site support depend on the underlying yt-dlp installation. 
