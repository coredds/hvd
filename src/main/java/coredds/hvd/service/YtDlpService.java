package coredds.hvd.service;

import coredds.hvd.model.DownloadItem;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YtDlpService {

    private static final Logger logger = LoggerFactory.getLogger(YtDlpService.class);
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern DESTINATION_PATTERN = Pattern.compile("\\[download\\] Destination: (.+)");
    private static final Pattern FINAL_DESTINATION_PATTERN = Pattern.compile("\\[Merger\\] Merging formats into \"(.+)\"");
    private static final Pattern FFMPEG_PROGRESS_PATTERN = Pattern.compile("\\[ffmpeg\\]");
    private static final Pattern POST_PROCESS_PATTERN = Pattern.compile("\\[PostProcessor\\]");
    private static final Pattern TITLE_PATTERN = Pattern.compile("\\[info\\] ([^:]+): Downloading");
    private static final Pattern TITLE_EXTRACT_PATTERN = Pattern.compile("\\[download\\] (.+?) \\[");
    private static final Pattern TITLE_INFO_PATTERN = Pattern.compile("\\] (.+?) \\[");

    private String ytDlpPath = "yt-dlp"; // Default, can be configured
    private String outputDirectory = System.getProperty("user.home") + File.separator + "Downloads";
    private final Set<Process> activeProcesses = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
     * Test if FFmpeg is available (required for thumbnail embedding and other features)
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.debug("FFmpeg not available", e);
            return false;
        }
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
                // Use get-title option which works more reliably than --print
                ProcessBuilder pb = new ProcessBuilder(ytDlpPath, "--get-title", "--no-playlist", url);
                // Set UTF-8 environment variables for Windows compatibility
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("LANG", "en_US.UTF-8");
                pb.environment().put("LC_ALL", "en_US.UTF-8");
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String title = reader.readLine();

                int exitCode = process.waitFor();
                if (exitCode == 0 && title != null && !title.trim().isEmpty()) {
                    logger.debug("Extracted video title: {}", title);
                    return title.trim();
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
    public Task<Void> createDownloadTask(DownloadItem item, boolean audioOnly, String audioFormat, String videoQuality, String videoFormat,
            boolean embedSubtitles, boolean embedThumbnail,
            boolean addMetadata, Consumer<String> logConsumer, BiConsumer<String, Boolean> statusConsumer) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<String> command = buildCommand(item, audioOnly, audioFormat, videoQuality, videoFormat, embedSubtitles,
                        embedThumbnail, addMetadata);

                logger.info("Starting download for: " + item.getUrl());
                logger.info("Command: " + String.join(" ", command));
                
                // Update status to downloading
                if (statusConsumer != null) {
                    javafx.application.Platform.runLater(() -> 
                        statusConsumer.accept("status.downloading.video", true));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                // Don't set working directory to avoid PATH issues
                pb.redirectErrorStream(true);
                // Set UTF-8 environment variables for Windows compatibility
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("LANG", "en_US.UTF-8");
                pb.environment().put("LC_ALL", "en_US.UTF-8");

                Process process = pb.start();
                
                // Track the process for cleanup
                activeProcesses.add(process);
                
                // Ensure process is alive
                if (!process.isAlive()) {
                    logger.error("Process failed to start");
                    activeProcesses.remove(process);
                    throw new RuntimeException("Failed to start yt-dlp process");
                }

                // Variables for tracking files and cleanup
                String downloadedFilePath = null;
                String finalFilePath = null;
                List<String> tempFiles = new ArrayList<>();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean receivedOutput = false;
                    boolean inPostProcessing = false;
                    double lastProgress = 0.0;

                    while ((line = reader.readLine()) != null && !isCancelled()) {
                        final String logLine = line;
                        receivedOutput = true;

                        // Detect different processing phases and update status
                        if (POST_PROCESS_PATTERN.matcher(line).find() || FFMPEG_PROGRESS_PATTERN.matcher(line).find()) {
                            if (!inPostProcessing) {
                                inPostProcessing = true;
                                // Set progress to show we're post-processing
                                javafx.application.Platform.runLater(() -> {
                                    item.setProgress(0.95); // Show near completion but still processing
                                });
                                // Update status bar instead of log
                                if (statusConsumer != null) {
                                    javafx.application.Platform.runLater(() -> 
                                        statusConsumer.accept("status.reencoding", true));
                                }
                            }
                        }

                        // Update progress if we can parse it
                        Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
                        if (progressMatcher.find() && !inPostProcessing) {
                            try {
                                double progress = Double.parseDouble(progressMatcher.group(1)) / 100.0;
                                lastProgress = progress;
                                javafx.application.Platform.runLater(() -> item.setProgress(progress));
                            } catch (NumberFormatException e) {
                                logger.warn("Failed to parse progress from: {}", line);
                            }
                        }

                        // Extract destination file path (initial download)
                        Matcher destMatcher = DESTINATION_PATTERN.matcher(line);
                        if (destMatcher.find()) {
                            downloadedFilePath = destMatcher.group(1);
                            logger.debug("Found destination: {}", downloadedFilePath);
                            
                            // Track intermediate files for cleanup (definitely temporary files only)
                            if (downloadedFilePath.matches(".*\\.f\\d+\\.(mp4|webm|m4a|aac)$") ||     // .f399.mp4, .f251.webm
                                downloadedFilePath.matches(".*\\.temp\\.(mp4|webm|m4a)$") ||           // .temp.mp4
                                downloadedFilePath.matches(".*\\.part$") ||                             // .part files
                                downloadedFilePath.matches(".*\\.ytdl$") ||                             // .ytdl files
                                downloadedFilePath.matches(".*\\.(webp|png|jpg|jpeg)$")) {             // thumbnail files
                                tempFiles.add(downloadedFilePath);
                                logger.debug("Added temp file for cleanup: {}", downloadedFilePath);
                            }
                            
                            // Extract title from filename as backup if no title was found yet
                            if (item.getTitle() == null || "Unknown Title".equals(item.getTitle())) {
                                String titleFromFilename = extractTitleFromFilename(downloadedFilePath);
                                if (titleFromFilename != null) {
                                    javafx.application.Platform.runLater(() -> item.setTitle(titleFromFilename));
                                }
                            }
                        }

                        // Extract final merged file path
                        Matcher finalDestMatcher = FINAL_DESTINATION_PATTERN.matcher(line);
                        if (finalDestMatcher.find()) {
                            finalFilePath = finalDestMatcher.group(1);
                            logger.debug("Found final destination: {}", finalFilePath);
                            // Update status when merging
                            if (statusConsumer != null) {
                                javafx.application.Platform.runLater(() -> 
                                    statusConsumer.accept("status.merging", true));
                            }
                        }

                        // Note: Title extraction is now handled separately via --print command
                        // This avoids polluting the title with operation messages

                        // Send log to consumer
                        if (logConsumer != null) {
                            javafx.application.Platform.runLater(() -> logConsumer.accept(logLine));
                        }
                        
                        // Yield to allow UI updates
                        Thread.yield();
                    }
                    
                    // If task was cancelled, force kill the process
                    if (isCancelled() && process.isAlive()) {
                        logger.info("Task cancelled - killing yt-dlp process");
                        process.destroyForcibly();
                    }
                    
                    if (!receivedOutput) {
                        logger.warn("No output received from yt-dlp process");
                    }

                    // Store the file path for completed download (prefer final merged file over temp file)
                    final String bestFilePath = finalFilePath != null ? finalFilePath : downloadedFilePath;
                    
                    if (bestFilePath != null) {
                        javafx.application.Platform.runLater(() -> item.setFilePath(bestFilePath));
                    }
                }
                
                // Capture variables for cleanup after process completion
                final List<String> tempFilesForCleanup = new ArrayList<>(tempFiles);
                final String finalFilePathForCleanup = finalFilePath;

                int exitCode = process.waitFor();

                // Remove process from active set when done
                activeProcesses.remove(process);

                if (exitCode == 0) {
                    // Update status for cleanup
                    if (statusConsumer != null) {
                        javafx.application.Platform.runLater(() -> 
                            statusConsumer.accept("status.cleanup", true));
                    }
                    
                    // Wait a moment for all post-processing to complete
                    try {
                        Thread.sleep(2000); // Wait 2 seconds for thumbnail processing to finish
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Clean up intermediate files using outer class method
                    YtDlpService.this.cleanupIntermediateFiles(tempFilesForCleanup, finalFilePathForCleanup);
                    
                    javafx.application.Platform.runLater(() -> {
                        item.setStatus(DownloadItem.Status.COMPLETED);
                        item.setProgress(1.0);
                    });
                    
                    // Final completion status
                    if (statusConsumer != null) {
                        javafx.application.Platform.runLater(() -> 
                            statusConsumer.accept("status.download.completed", false));
                    }
                } else if (isCancelled()) {
                    // Task was cancelled, don't update status (will be set to PAUSED by controller)
                    logger.info("Download task was cancelled for: {}", item.getUrl());
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

    private List<String> buildCommand(DownloadItem item, boolean audioOnly, String audioFormat, String videoQuality, String videoFormat,
            boolean embedSubtitles, boolean embedThumbnail, boolean addMetadata) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);

        // Output directory and filename template
        command.add("-P");
        command.add(outputDirectory);
        
        // Use proper filename template to preserve video title and avoid yt-dlp default naming
        command.add("-o");
        command.add("%(title).200s [%(id)s].%(ext)s");
        
        // Also force no default filename format
        command.add("--no-clean-infojson");

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
            String formatString = buildVideoFormatString(videoQuality, videoFormat);
            // Fallback to simple format if complex format is empty or problematic
            if (formatString == null || formatString.trim().isEmpty()) {
                formatString = "best";
            }
            command.add(formatString);
            
            // Always ensure compatible output format with H.264 video
            String targetFormat = getTargetVideoFormat(videoFormat);
            command.add("--merge-output-format");
            command.add(targetFormat);
            
            // Re-encode if the downloaded format isn't compatible with the target
            // This ensures proper codecs for maximum player compatibility
            command.add("--recode-video");
            command.add(targetFormat);
            
            // CRITICAL: Audio format must come AFTER re-encoding to override default codec
            // For video downloads with specific audio format, force post-processing
            if (audioFormat != null && !audioFormat.isEmpty()) {
                command.add("--audio-format");
                command.add(audioFormat);
            }
        }

        // Subtitle options
        if (embedSubtitles) {
            command.add("--write-subs");
            command.add("--embed-subs");
        }

        // Thumbnail options - simplified to avoid FFmpeg conflicts
        if (embedThumbnail) {
            // Write thumbnail file (always works)
            command.add("--write-thumbnail");
            // Embed thumbnail (may conflict with post-processing, but try anyway)
            command.add("--embed-thumbnail");
        }

        // Metadata options
        if (addMetadata) {
            command.add("--add-metadata");
        }

        // Progress and verbose output
        command.add("--newline");
        command.add("--progress");

        // Playlist handling
        if (item.isNoPlaylist()) {
            command.add("--no-playlist");
            logger.info("Added --no-playlist flag for single video download");
        } else {
            logger.info("Downloading playlist (no --no-playlist flag)");
        }

        // URL
        command.add(item.getUrl());

        return command;
    }

    /**
     * Build video format string combining quality and format preferences
     * Prioritizes resolution quality, then uses FFmpeg post-processing for compatibility
     */
    private String buildVideoFormatString(String videoQuality, String videoFormat) {
        
        // Handle special cases first - check for all possible translations
        if (isBestAvailable(videoQuality) || videoQuality == null) {
            if (isBestFormat(videoFormat) || videoFormat == null) {
                // Get the best quality available, we'll handle compatibility via post-processing
                return "bestvideo+bestaudio/best";
            } else {
                // Best quality video in specific format (try format-specific first)
                return "bestvideo[ext=" + videoFormat + "]+bestaudio/bestvideo+bestaudio/best";
            }
        }
        
        if (isWorstAvailable(videoQuality)) {
            if (isBestFormat(videoFormat) || videoFormat == null) {
                return "worst";
            } else {
                return "worst[ext=" + videoFormat + "]/worst";
            }
        }
        
        // For specific resolutions, prioritize getting the resolution we want
        String heightTarget = getHeightFromQuality(videoQuality);
        if (heightTarget == null) {
            return "bestvideo+bestaudio/best";
        }
        
        if (isBestFormat(videoFormat) || videoFormat == null) {
            // Prioritize resolution over format - get the best quality at target resolution
            return "bestvideo[height<=" + heightTarget + "]+bestaudio/" +
                   "best[height<=" + heightTarget + "]/best";
        } else {
            // Height + format preference - try format-specific first, fallback to any format
            return "bestvideo[height<=" + heightTarget + "][ext=" + videoFormat + "]+bestaudio/" +
                   "bestvideo[height<=" + heightTarget + "]+bestaudio/" +
                   "best[height<=" + heightTarget + "]/best";
        }
    }
    
    /**
     * Get the height value from quality string
     */
    private String getHeightFromQuality(String videoQuality) {
        return switch (videoQuality) {
            case "2160p (4K)" -> "2160";
            case "1440p" -> "1440";
            case "1080p" -> "1080";
            case "720p" -> "720";
            case "480p" -> "480";
            case "360p" -> "360";
            default -> null;
        };
    }
    
    /**
     * Check if the quality string represents "Best Available" in any language
     */
    private boolean isBestAvailable(String videoQuality) {
        return "Best Available".equals(videoQuality) || 
               "Melhor Disponível".equals(videoQuality) || 
               "Mejor Disponible".equals(videoQuality) ||
               "Migliore Disponibile".equals(videoQuality) ||
               "最高品質".equals(videoQuality) ||
               "Beste verfügbar".equals(videoQuality);
    }
    
    /**
     * Check if the quality string represents "Worst Available" in any language
     */
    private boolean isWorstAvailable(String videoQuality) {
        return "Worst Available".equals(videoQuality) || 
               "Pior Disponível".equals(videoQuality) || 
               "Peor Disponible".equals(videoQuality) ||
               "Peggiore Disponibile".equals(videoQuality) ||
               "最低品質".equals(videoQuality) ||
               "Schlechteste verfügbar".equals(videoQuality);
    }
    
    /**
     * Check if the format string represents "Best Format" in any language
     */
    private boolean isBestFormat(String videoFormat) {
        return "Best Format".equals(videoFormat) || 
               "Melhor Formato".equals(videoFormat) || 
               "Mejor Formato".equals(videoFormat) ||
               "Miglior Formato".equals(videoFormat) ||
               "最高フォーマット".equals(videoFormat) ||
               "Bestes Format".equals(videoFormat);
    }

    /**
     * Get the target video format for output, prioritizing compatibility
     */
    private String getTargetVideoFormat(String videoFormat) {
        if (isBestFormat(videoFormat) || videoFormat == null) {
            return "mp4"; // Default to mp4 for best compatibility
        }
        
        return switch (videoFormat) {
            case "mp4" -> "mp4";
            case "webm" -> "webm"; 
            case "mkv" -> "mkv";
            case "avi" -> "avi";
            case "mov" -> "mov";
            default -> "mp4"; // Default fallback for compatibility
        };
    }
    
    /**
     * Get the FFmpeg audio codec name for the specified audio format
     */
    private String getAudioCodec(String audioFormat) {
        return switch (audioFormat) {
            case "mp3" -> "libmp3lame";
            case "aac" -> "aac";
            case "m4a" -> "aac";
            case "opus" -> "libopus";
            case "flac" -> "flac";
            case "wav" -> "pcm_s16le";
            default -> "aac"; // Default fallback
        };
    }
    
    /**
     * Extract video title from filename using our template pattern
     */
    private String extractTitleFromFilename(String filePath) {
        if (filePath == null) return null;
        
        // Extract just the filename from the full path
        String filename = new File(filePath).getName();
        
        // Our template is: %(title)s [%(id)s].%(ext)s
        // So we need to extract everything before the last [video_id]
        int lastBracketIndex = filename.lastIndexOf(" [");
        if (lastBracketIndex > 0) {
            return filename.substring(0, lastBracketIndex).trim();
        }
        
        // Fallback: remove extension and return
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex).trim();
        }
        
        return filename.trim();
    }

    
    /**
     * Kill all active yt-dlp processes - used during app shutdown
     */
    public void killAllActiveProcesses() {
        logger.info("Killing {} active yt-dlp processes", activeProcesses.size());
        
        for (Process process : activeProcesses) {
            try {
                if (process.isAlive()) {
                    process.destroyForcibly();
                    logger.debug("Killed yt-dlp process: {}", process.pid());
                }
            } catch (Exception e) {
                logger.warn("Failed to kill process: {}", e.getMessage());
            }
        }
        
        activeProcesses.clear();
    }
    
    /**
     * Clean up intermediate files after successful download
     */
    private void cleanupIntermediateFiles(List<String> tempFiles, String finalFilePath) {
        logger.info("Starting cleanup process...");
        
        // Clean up tracked temp files
        if (tempFiles != null && !tempFiles.isEmpty()) {
            logger.info("Cleaning up {} tracked intermediate files", tempFiles.size());
            
            for (String tempFile : tempFiles) {
                deleteFileIfExists(tempFile, finalFilePath);
            }
        }
        
        // Additional cleanup: scan for other temp files based on final file pattern
        if (finalFilePath != null) {
            cleanupAdditionalTempFiles(finalFilePath);
        }
    }
    
        /**
     * Clean up additional temp files that might not be caught by the tracking
     */
    private void cleanupAdditionalTempFiles(String finalFilePath) {
        try {
            File finalFile = new File(finalFilePath);
            File parentDir = finalFile.getParentFile();
            String baseName = finalFile.getName();
            
            // Extract video ID from filename like "Title [videoId].mp4"
            String videoId = null;
            if (baseName.matches(".*\\[([a-zA-Z0-9_-]+)\\]\\.\\w+$")) {
                videoId = baseName.replaceAll(".*\\[([a-zA-Z0-9_-]+)\\]\\.\\w+$", "$1");
            }
            
            if (videoId != null && parentDir != null && parentDir.exists()) {
                logger.info("Scanning directory for additional temp files with video ID: {}", videoId);
                File[] files = parentDir.listFiles();
                if (files != null) {
                    int cleanedCount = 0;
                    for (File file : files) {
                        String fileName = file.getName();
                        // Clean up files with same video ID that are temp files
                        if (fileName.contains(videoId) && !fileName.equals(baseName)) {
                            if (fileName.matches(".*\\.temp\\.(mp4|webm|m4a)$") ||
                                fileName.matches(".*\\.part$") ||
                                fileName.matches(".*\\.ytdl$") ||
                                fileName.matches(".*\\.(png|webp|jpg|jpeg)$") ||  // Include thumbnails
                                fileName.matches(".*\\.f\\d+\\.(mp4|webm|m4a)$")) { // Include format files
                                logger.info("Cleaning up temp/thumbnail file: {}", fileName);
                                deleteFileIfExists(file.getAbsolutePath(), finalFilePath);
                                cleanedCount++;
                            }
                        }
                    }
                    logger.info("Additional cleanup removed {} files", cleanedCount);
                }
            }
        } catch (Exception e) {
            logger.warn("Error during additional temp file cleanup: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to delete a file if it exists and is not the final file
     */
    private void deleteFileIfExists(String filePath, String finalFilePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && !filePath.equals(finalFilePath)) {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.debug("Deleted temp file: {}", filePath);
                } else {
                    logger.warn("Failed to delete temp file: {}", filePath);
                }
            }
        } catch (Exception e) {
            logger.warn("Error deleting temp file {}: {}", filePath, e.getMessage());
        }
    }
}