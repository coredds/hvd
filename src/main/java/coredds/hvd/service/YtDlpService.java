package coredds.hvd.service;

import coredds.hvd.model.DownloadItem;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YtDlpService {

    private static final Logger logger = LoggerFactory.getLogger(YtDlpService.class);
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern DESTINATION_PATTERN = Pattern.compile("\\[download\\] Destination: (.+)");

    private String ytDlpPath = "yt-dlp"; // Default, can be configured
    private String outputDirectory = System.getProperty("user.home") + File.separator + "Downloads";

    public YtDlpService() {
        // Default constructor - detection will happen later in a background thread
        logger.debug("YtDlpService initialized with default path: " + ytDlpPath);
    }

    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Test if yt-dlp is available and working
     */
    public boolean isYtDlpAvailable() {
        // First try the configured path
        if (isYtDlpAvailableAtPath(ytDlpPath)) {
            return true;
        }

        // If default "yt-dlp" fails, try to auto-detect
        if ("yt-dlp".equals(ytDlpPath)) {
            String detectedPath = detectYtDlpPath();
            if (detectedPath != null) {
                logger.info("Auto-detected yt-dlp at: " + detectedPath);
                this.ytDlpPath = detectedPath;
                return true;
            }
        }

        return false;
    }

    /**
     * Test if yt-dlp is available at a specific path
     */
    private boolean isYtDlpAvailableAtPath(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.debug("yt-dlp not available at path: " + path, e);
            return false;
        }
    }

    /**
     * Try to detect yt-dlp in common installation locations
     */
    private String detectYtDlpPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String[] commonPaths;

        if (os.contains("win")) {
            // Windows paths
            commonPaths = new String[] {
                    "yt-dlp.exe", // In PATH
                    userHome + "\\AppData\\Local\\Programs\\Python\\Python*\\Scripts\\yt-dlp.exe",
                    userHome + "\\AppData\\Roaming\\Python\\Python*\\Scripts\\yt-dlp.exe",
                    "C:\\Python*\\Scripts\\yt-dlp.exe",
                    "C:\\Program Files\\Python*\\Scripts\\yt-dlp.exe",
                    "C:\\Program Files (x86)\\Python*\\Scripts\\yt-dlp.exe",
                    userHome + "\\scoop\\apps\\yt-dlp\\current\\yt-dlp.exe", // Scoop
                    "C:\\ProgramData\\chocolatey\\bin\\yt-dlp.exe", // Chocolatey
                    userHome + "\\AppData\\Local\\Microsoft\\WindowsApps\\yt-dlp.exe" // Microsoft Store
            };
        } else {
            // Unix/Linux/macOS paths
            commonPaths = new String[] {
                    "/opt/homebrew/bin/yt-dlp", // Apple Silicon Homebrew
                    "/usr/local/bin/yt-dlp", // Intel Homebrew / manual install
                    userHome + "/.pyenv/shims/yt-dlp", // pyenv
                    userHome + "/.local/bin/yt-dlp", // pip user install
                    "/usr/bin/yt-dlp", // System package manager
                    "/bin/yt-dlp", // System install
                    "/snap/bin/yt-dlp" // Snap package (Linux)
            };
        }

        for (String path : commonPaths) {
            // Handle wildcard paths for Windows Python installations
            if (path.contains("*")) {
                String detectedPath = findWildcardPath(path);
                if (detectedPath != null && isYtDlpAvailableAtPath(detectedPath)) {
                    return detectedPath;
                }
            } else if (isYtDlpAvailableAtPath(path)) {
                return path;
            }
        }

        return null; // Not found in any common location
    }

    /**
     * Handle wildcard paths for Windows Python installations
     */
    private String findWildcardPath(String wildcardPath) {
        try {
            String basePath = wildcardPath.substring(0, wildcardPath.indexOf('*'));
            String suffix = wildcardPath.substring(wildcardPath.lastIndexOf('*') + 1);

            java.io.File baseDir = new java.io.File(basePath).getParentFile();
            if (!baseDir.exists())
                return null;

            for (java.io.File dir : baseDir.listFiles()) {
                if (dir.isDirectory() && dir.getName().startsWith("Python")) {
                    String candidatePath = basePath.replace("Python*", dir.getName()) + suffix;
                    java.io.File candidate = new java.io.File(candidatePath);
                    if (candidate.exists()) {
                        return candidatePath;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to resolve wildcard path: " + wildcardPath, e);
        }
        return null;
    }

    /**
     * Extract video information without downloading
     */
    public CompletableFuture<String> extractVideoInfo(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(ytDlpPath, "--get-title", url);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String title = reader.readLine();

                int exitCode = process.waitFor();
                if (exitCode == 0 && title != null) {
                    return title;
                }

                return "Unknown Title";
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to extract video info for: " + url, e);
                return "Unknown Title";
            }
        });
    }

    /**
     * Create a download task for a given download item
     */
    public Task<Void> createDownloadTask(DownloadItem item, boolean audioOnly, String audioFormat, String videoFormat,
            boolean embedSubtitles, boolean embedThumbnail,
            boolean addMetadata, Consumer<String> logConsumer) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<String> command = buildCommand(item, audioOnly, audioFormat, videoFormat, embedSubtitles,
                        embedThumbnail, addMetadata);

                logger.info("Starting download for: " + item.getUrl());
                logger.info("Command: " + String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new java.io.File(outputDirectory));
                pb.redirectErrorStream(true);

                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    String downloadedFilePath = null;

                    while ((line = reader.readLine()) != null && !isCancelled()) {
                        final String logLine = line;

                        // Update progress if we can parse it
                        Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
                        if (progressMatcher.find()) {
                            try {
                                double progress = Double.parseDouble(progressMatcher.group(1)) / 100.0;
                                javafx.application.Platform.runLater(() -> item.setProgress(progress));
                            } catch (NumberFormatException e) {
                                // Ignore parsing errors
                            }
                        }

                        // Extract destination file path
                        Matcher destMatcher = DESTINATION_PATTERN.matcher(line);
                        if (destMatcher.find()) {
                            downloadedFilePath = destMatcher.group(1);
                        }

                        // Send log to consumer
                        if (logConsumer != null) {
                            javafx.application.Platform.runLater(() -> logConsumer.accept(logLine));
                        }
                    }

                    // Store the file path for completed download
                    final String finalFilePath = downloadedFilePath;
                    if (finalFilePath != null) {
                        javafx.application.Platform.runLater(() -> item.setFilePath(finalFilePath));
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    javafx.application.Platform.runLater(() -> {
                        item.setStatus(DownloadItem.Status.COMPLETED);
                        item.setProgress(1.0);
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        item.setStatus(DownloadItem.Status.ERROR);
                        item.setErrorMessage("Download failed with exit code: " + exitCode);
                    });
                }

                return null;
            }
        };
    }

    private List<String> buildCommand(DownloadItem item, boolean audioOnly, String audioFormat, String videoFormat,
            boolean embedSubtitles, boolean embedThumbnail, boolean addMetadata) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);

        // Output directory
        command.add("-P");
        command.add(outputDirectory);

        // Audio only options
        if (audioOnly) {
            command.add("--extract-audio");
            if (audioFormat != null && !audioFormat.isEmpty()) {
                command.add("--audio-format");
                command.add(audioFormat);
            }
        } else {
            // Video format selection
            command.add("-f");
            if (videoFormat != null && !videoFormat.isEmpty()) {
                // Handle special format mappings
                String formatString = mapVideoFormat(videoFormat);
                command.add(formatString);
            } else {
                command.add("bestvideo+bestaudio/best");
            }
        }

        // Subtitle options
        if (embedSubtitles) {
            command.add("--write-subs");
            command.add("--embed-subs");
        }

        // Thumbnail options
        if (embedThumbnail) {
            command.add("--embed-thumbnail");
        }

        // Metadata options
        if (addMetadata) {
            command.add("--add-metadata");
        }

        // Progress and verbose output
        command.add("--newline");
        command.add("--progress");

        // URL
        command.add(item.getUrl());

        return command;
    }

    /**
     * Map user-friendly video format names to yt-dlp format strings
     */
    private String mapVideoFormat(String videoFormat) {
        return switch (videoFormat) {
            case "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]";
            case "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
            case "1440p" -> "bestvideo[height<=1440]+bestaudio/best[height<=1440]";
            case "2160p (4K)" -> "bestvideo[height<=2160]+bestaudio/best[height<=2160]";
            case "mp4" -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]";
            case "webm" -> "bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=webm]";
            case "mkv" -> "bestvideo[ext=mkv]+bestaudio/best[ext=mkv]";
            case "avi" -> "bestvideo[ext=avi]+bestaudio/best[ext=avi]";
            case "mov" -> "bestvideo[ext=mov]+bestaudio/best[ext=mov]";
            case "bestvideo+bestaudio" -> "bestvideo+bestaudio/best";
            case "worst" -> "worst";
            case "best" -> "best";
            default -> videoFormat; // Use the format string as-is for custom formats
        };
    }
}