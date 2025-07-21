package coredds.hvd.controller;

import coredds.hvd.model.DownloadItem;
import coredds.hvd.service.YtDlpService;
import coredds.hvd.service.PreferencesService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Download Tab Controls
    @FXML
    private TextArea urlTextArea;
    @FXML
    private RadioButton videoRadioButton;
    @FXML
    private RadioButton audioRadioButton;
    @FXML
    private ComboBox<String> audioFormatComboBox;
    @FXML
    private ComboBox<String> videoQualityComboBox;
    @FXML
    private ComboBox<String> videoFormatComboBox;
    @FXML
    private TextField outputDirectoryField;
    @FXML
    private Button browseOutputButton;
    @FXML
    private CheckBox embedSubtitlesCheckBox;
    @FXML
    private CheckBox embedThumbnailCheckBox;
    @FXML
    private CheckBox addMetadataCheckBox;
    @FXML
    private Button addToQueueButton;
    @FXML
    private Button startAllButton;
    @FXML
    private Button pauseAllButton;
    @FXML
    private Button removeSelectedButton;
    @FXML
    private TableView<DownloadItem> downloadTable;
    @FXML
    private TableColumn<DownloadItem, String> urlColumn;
    @FXML
    private TableColumn<DownloadItem, String> titleColumn;
    @FXML
    private TableColumn<DownloadItem, String> formatColumn;
    @FXML
    private TableColumn<DownloadItem, DownloadItem.Status> statusColumn;
    @FXML
    private TableColumn<DownloadItem, Double> progressColumn;

    // Settings Tab Controls
    @FXML
    private TextField ytDlpPathField;
    @FXML
    private Button browseYtDlpButton;
    @FXML
    private TextField defaultOutputField;
    @FXML
    private Button browseDefaultOutputButton;
    @FXML
    private Button testYtDlpButton;
    @FXML
    private Button testFFmpegButton;
    @FXML
    private Label ytDlpStatusLabel;
    @FXML
    private Label ffmpegStatusLabel;

    // Logs Tab Controls
    @FXML
    private TextArea logTextArea;
    @FXML
    private Button clearLogsButton;

    // Services and Data
    private YtDlpService ytDlpService;
    private PreferencesService preferencesService;
    private ObservableList<DownloadItem> downloadItems;
    private ExecutorService executorService;
    private List<Task<Void>> activeTasks;
    
    // Flag to prevent saving preferences during initialization
    private boolean isInitializing = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");

        // Initialize services
        executorService = Executors.newCachedThreadPool();
        downloadItems = FXCollections.observableArrayList();
        activeTasks = new ArrayList<>();
        ytDlpService = new YtDlpService();
        preferencesService = new PreferencesService();

        // Setup Download Type Toggle Group
        ToggleGroup downloadTypeGroup = new ToggleGroup();
        videoRadioButton.setToggleGroup(downloadTypeGroup);
        audioRadioButton.setToggleGroup(downloadTypeGroup);

        // Setup format combo boxes
        audioFormatComboBox.setItems(FXCollections.observableArrayList(
                "mp3", "aac", "m4a", "opus", "flac", "wav"));
        
        // Setup video quality combo box (resolution/quality options)
        videoQualityComboBox.setItems(FXCollections.observableArrayList(
                "Best Available", "2160p (4K)", "1440p", "1080p", "720p", "480p", "360p", "Worst Available"));
        
        // Setup video format combo box (file format/container options)
        videoFormatComboBox.setItems(FXCollections.observableArrayList(
                "Best Format", "mp4", "webm", "mkv", "avi", "mov"));

        // Load and apply saved preferences BEFORE setting up listeners
        loadPreferences();

        // Setup combo box visibility based on download type
        downloadTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == audioRadioButton) {
                // Audio Only: Show audio format, hide video controls
                audioFormatComboBox.setVisible(true);
                videoQualityComboBox.setVisible(false);
                videoFormatComboBox.setVisible(false);
            } else {
                // Video: Show ALL controls (video has both video and audio streams)
                audioFormatComboBox.setVisible(true);
                videoQualityComboBox.setVisible(true);
                videoFormatComboBox.setVisible(true);
            }
            // Save download type preference
            saveDownloadTypePreference();
        });

        // Add listeners to save preferences when user changes settings
        setupPreferenceListeners();

        // Setup button actions
        browseOutputButton.setOnAction(e -> browseOutputDirectory());
        addToQueueButton.setOnAction(e -> addToQueue());
        startAllButton.setOnAction(e -> startAllDownloads());
        pauseAllButton.setOnAction(e -> pauseAllDownloads());
        removeSelectedButton.setOnAction(e -> removeSelectedItems());

        // Setup Downloads Table
        setupDownloadsTable();

        // Setup Settings Tab
        setupSettingsTab();
        
        // Setup Logs Tab
        setupLogsTab();
        
        // Initial status check
        checkDependencyStatus();
        
        // Initialization complete - enable preference saving
        isInitializing = false;
    }

    private void setupDownloadsTable() {

        // Setup table (output directory is handled by loadPreferences())
        setupDownloadTable();

        // Setup button actions
        browseOutputButton.setOnAction(e -> browseOutputDirectory());
        addToQueueButton.setOnAction(e -> addToQueue());
        startAllButton.setOnAction(e -> startAllDownloads());
        pauseAllButton.setOnAction(e -> pauseAllDownloads());
        removeSelectedButton.setOnAction(e -> removeSelectedItems());
    }

    private void setupDownloadTable() {
        // Setup columns
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        formatColumn.setCellValueFactory(new PropertyValueFactory<>("format"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));

        // Setup custom progress cell with percentage and open file button
        progressColumn.setCellFactory(column -> new ProgressCell());

        // Set table items
        downloadTable.setItems(downloadItems);

        // Setup row selection
        downloadTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupSettingsTab() {
        // Set default values
        ytDlpPathField.setText("yt-dlp");
        defaultOutputField.setText(System.getProperty("user.home") + File.separator + "Downloads");

        // Setup button actions
        browseYtDlpButton.setOnAction(e -> browseYtDlpPath());
        browseDefaultOutputButton.setOnAction(e -> browseDefaultOutput());
        testYtDlpButton.setOnAction(e -> testYtDlp());
        testFFmpegButton.setOnAction(e -> testFFmpeg());
    }

    private void setupLogsTab() {
        logTextArea.setEditable(false);
        clearLogsButton.setOnAction(e -> logTextArea.clear());

        // Add initial log message
        appendLog("Application started");
    }

    /**
     * Load preferences and apply them to UI components
     */
    private void loadPreferences() {
        // Load output directory
        String savedOutputDir = preferencesService.getPreference(PreferencesService.OUTPUT_DIRECTORY, null);
        if (savedOutputDir != null && new File(savedOutputDir).exists()) {
            outputDirectoryField.setText(savedOutputDir);
        } else {
            // Use default if no saved preference or directory doesn't exist
            String userHome = System.getProperty("user.home");
            String defaultOutputPath = userHome + File.separator + "Downloads" + File.separator + "hvd";
            outputDirectoryField.setText(defaultOutputPath);
        }

        // Load download type (audio vs video)
        boolean isAudioMode = preferencesService.getBooleanPreference(PreferencesService.DOWNLOAD_TYPE_AUDIO, false);
        if (isAudioMode) {
            audioRadioButton.setSelected(true);
        } else {
            videoRadioButton.setSelected(true);
        }

        // Load format preferences
        String savedAudioFormat = preferencesService.getPreference(PreferencesService.AUDIO_FORMAT, "mp3");
        audioFormatComboBox.setValue(savedAudioFormat);

        String savedVideoQuality = preferencesService.getPreference(PreferencesService.VIDEO_QUALITY, "1080p");
        videoQualityComboBox.setValue(savedVideoQuality);

        String savedVideoFormat = preferencesService.getPreference(PreferencesService.VIDEO_FORMAT, "Best Format");
        videoFormatComboBox.setValue(savedVideoFormat);

        // Load checkbox preferences
        embedSubtitlesCheckBox.setSelected(
            preferencesService.getBooleanPreference(PreferencesService.EMBED_SUBTITLES, false));
        embedThumbnailCheckBox.setSelected(
            preferencesService.getBooleanPreference(PreferencesService.EMBED_THUMBNAIL, true)); // Default to checked
        addMetadataCheckBox.setSelected(
            preferencesService.getBooleanPreference(PreferencesService.ADD_METADATA, false));

        // Load yt-dlp path
        String savedYtDlpPath = preferencesService.getPreference(PreferencesService.YTDLP_PATH, "yt-dlp");
        if (ytDlpPathField != null) {
            ytDlpPathField.setText(savedYtDlpPath);
        }

        // Load default output directory for settings
        String savedDefaultOutput = preferencesService.getPreference(PreferencesService.DEFAULT_OUTPUT_DIRECTORY, null);
        if (savedDefaultOutput != null && defaultOutputField != null) {
            defaultOutputField.setText(savedDefaultOutput);
        }

        logger.info("Loaded user preferences");
    }

    /**
     * Setup listeners to automatically save preferences when user changes settings
     */
    private void setupPreferenceListeners() {
        // Save output directory when changed
        outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing && newVal != null && !newVal.trim().isEmpty()) {
                preferencesService.setPreference(PreferencesService.OUTPUT_DIRECTORY, newVal);
                preferencesService.savePreferences();
            }
        });

        // Save format preferences when changed
        audioFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing && newVal != null) {
                preferencesService.setPreference(PreferencesService.AUDIO_FORMAT, newVal);
                preferencesService.savePreferences();
            }
        });

        videoQualityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing && newVal != null) {
                preferencesService.setPreference(PreferencesService.VIDEO_QUALITY, newVal);
                preferencesService.savePreferences();
            }
        });

        videoFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing && newVal != null) {
                preferencesService.setPreference(PreferencesService.VIDEO_FORMAT, newVal);
                preferencesService.savePreferences();
            }
        });

        // Save checkbox preferences when changed
        embedSubtitlesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing) {
                preferencesService.setBooleanPreference(PreferencesService.EMBED_SUBTITLES, newVal);
                preferencesService.savePreferences();
            }
        });

        embedThumbnailCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing) {
                preferencesService.setBooleanPreference(PreferencesService.EMBED_THUMBNAIL, newVal);
                preferencesService.savePreferences();
            }
        });

        addMetadataCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isInitializing) {
                preferencesService.setBooleanPreference(PreferencesService.ADD_METADATA, newVal);
                preferencesService.savePreferences();
            }
        });

        // Save settings tab preferences when changed
        if (ytDlpPathField != null) {
            ytDlpPathField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isInitializing && newVal != null && !newVal.trim().isEmpty()) {
                    preferencesService.setPreference(PreferencesService.YTDLP_PATH, newVal);
                    preferencesService.savePreferences();
                }
            });
        }

        if (defaultOutputField != null) {
            defaultOutputField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isInitializing && newVal != null && !newVal.trim().isEmpty()) {
                    preferencesService.setPreference(PreferencesService.DEFAULT_OUTPUT_DIRECTORY, newVal);
                    preferencesService.savePreferences();
                }
            });
        }
    }

    /**
     * Save download type preference
     */
    private void saveDownloadTypePreference() {
        if (!isInitializing) {
            boolean isAudioMode = audioRadioButton.isSelected();
            preferencesService.setBooleanPreference(PreferencesService.DOWNLOAD_TYPE_AUDIO, isAudioMode);
            preferencesService.savePreferences();
        }
    }

    @FXML
    private void browseOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        
        // Set initial directory to current output directory if it exists
        String currentPath = outputDirectoryField.getText();
        if (currentPath != null && !currentPath.trim().isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        File selectedDirectory = directoryChooser.showDialog(browseOutputButton.getScene().getWindow());
        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
            // Preference will be saved automatically by the listener
        }
    }

    @FXML
    private void addToQueue() {
        String urlText = urlTextArea.getText().trim();
        if (urlText.isEmpty()) {
            showAlert("Error", "Please enter at least one URL");
            return;
        }

        String[] urls = urlText.split("\n");
        for (String url : urls) {
            url = url.trim();
            if (!url.isEmpty()) {
                processUrl(url);
            }
        }

        urlTextArea.clear();
        appendLog("Added " + urls.length + " item(s) to queue");
    }

    private void processUrl(String url) {
        // Check if this is a playlist URL
        if (isPlaylistUrl(url)) {
            logger.info("Playlist URL detected: {}", url);
            showPlaylistChoiceDialog(url);
        } else {
            logger.info("Regular URL detected: {}", url);
            addSingleUrlToQueue(url, false);
        }
    }

    private boolean isPlaylistUrl(String url) {
        // Check for common playlist URL patterns
        boolean isPlaylist = url.contains("list=") || 
               url.contains("&list=") ||
               url.contains("playlist?") ||
               url.matches(".*youtube\\.com/playlist.*") ||
               url.matches(".*youtu\\.be/.*\\?.*list=.*");
        logger.info("URL pattern check for '{}': isPlaylist = {}", url, isPlaylist);
        return isPlaylist;
    }

    private void showPlaylistChoiceDialog(String url) {
        logger.info("Showing playlist choice dialog for URL: {}", url);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Playlist Detected");
        alert.setHeaderText("This URL appears to be a playlist or contains playlist information.");
        alert.setContentText("What would you like to download?");

        ButtonType singleVideoButton = new ButtonType("Single Video Only");
        ButtonType entirePlaylistButton = new ButtonType("Entire Playlist");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(singleVideoButton, entirePlaylistButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == singleVideoButton) {
                addSingleUrlToQueue(url, true); // true = no playlist flag
                appendLog("User chose: Single Video Only - will add --no-playlist flag");
            } else if (response == entirePlaylistButton) {
                addSingleUrlToQueue(url, false); // false = download playlist
                appendLog("User chose: Entire Playlist - will download all videos");
            }
            // Cancel does nothing
        });
    }

    private void addSingleUrlToQueue(String url, boolean noPlaylist) {
        DownloadItem item = new DownloadItem(url);
        item.setNoPlaylist(noPlaylist);
        logger.info("Created DownloadItem with noPlaylist flag: {}", noPlaylist);

        // Set format based on selection
        if (audioRadioButton.isSelected()) {
            item.setFormat("audio-" + audioFormatComboBox.getValue());
        } else {
            String quality = videoQualityComboBox.getValue();
            String format = videoFormatComboBox.getValue();
            item.setFormat("video-" + quality + " (" + format + ")");
        }

        downloadItems.add(item);

        // Try to extract title asynchronously
        ytDlpService.extractVideoInfo(url).thenAccept(title -> {
            javafx.application.Platform.runLater(() -> item.setTitle(title));
        });
    }

    @FXML
    private void startAllDownloads() {
        if (downloadItems.isEmpty()) {
            showAlert("Information", "No items in download queue");
            return;
        }

        // Check for FFmpeg if thumbnail embedding is enabled
        if (embedThumbnailCheckBox.isSelected()) {
            Task<Boolean> ffmpegCheckTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return ytDlpService.isFFmpegAvailable();
                }
            };
            
            ffmpegCheckTask.setOnSucceeded(e -> {
                boolean ffmpegAvailable = ffmpegCheckTask.getValue();
                if (!ffmpegAvailable) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("FFmpeg Not Found");
                    alert.setHeaderText("Thumbnail embedding requires FFmpeg");
                    alert.setContentText("FFmpeg is not installed or not found in PATH. " +
                            "Thumbnail embedding may fail.\n\n" +
                            "Install FFmpeg from ffmpeg.org or disable thumbnail embedding.\n\n" +
                            "Continue anyway?");
                    
                    ButtonType continueButton = new ButtonType("Continue");
                    ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(continueButton, cancelButton);
                    
                    alert.showAndWait().ifPresent(response -> {
                        if (response == continueButton) {
                            startDownloadsInternal();
                        }
                    });
                } else {
                    startDownloadsInternal();
                }
            });
            
            executorService.submit(ffmpegCheckTask);
        } else {
            startDownloadsInternal();
        }
    }
    
    private void startDownloadsInternal() {
        // Update service settings
        ytDlpService.setYtDlpPath(ytDlpPathField.getText());
        ytDlpService.setOutputDirectory(outputDirectoryField.getText());

        for (DownloadItem item : downloadItems) {
            if (item.getStatus() == DownloadItem.Status.QUEUED) {
                startDownload(item);
            }
        }
    }

    private void startDownload(DownloadItem item) {
        item.setStatus(DownloadItem.Status.DOWNLOADING);
        
        logger.info("Starting download for URL: {} with noPlaylist flag: {}", item.getUrl(), item.isNoPlaylist());

        boolean audioOnly = audioRadioButton.isSelected();
        String audioFormat = audioFormatComboBox.getValue();
        String videoQuality = videoQualityComboBox.getValue();
        String videoFormat = videoFormatComboBox.getValue();

        Task<Void> downloadTask = ytDlpService.createDownloadTask(
                item, audioOnly, audioFormat, videoQuality, videoFormat,
                embedSubtitlesCheckBox.isSelected(),
                embedThumbnailCheckBox.isSelected(),
                addMetadataCheckBox.isSelected(),
                this::appendLog);

        // Track the task for pause/cancel functionality
        activeTasks.add(downloadTask);
        
        // Remove task from active list when completed
        downloadTask.setOnSucceeded(e -> activeTasks.remove(downloadTask));
        downloadTask.setOnFailed(e -> activeTasks.remove(downloadTask));
        downloadTask.setOnCancelled(e -> {
            activeTasks.remove(downloadTask);
            item.setStatus(DownloadItem.Status.PAUSED);
        });

        executorService.submit(downloadTask);
    }

    @FXML
    private void pauseAllDownloads() {
        int pausedCount = 0;
        
        // Cancel all active download tasks
        List<Task<Void>> tasksToCancel = new ArrayList<>(activeTasks);
        for (Task<Void> task : tasksToCancel) {
            if (!task.isDone()) {
                task.cancel(true); // Force cancellation
                pausedCount++;
            }
        }
        
        // Update UI for downloading items that were cancelled
        for (DownloadItem item : downloadItems) {
            if (item.getStatus() == DownloadItem.Status.DOWNLOADING) {
                item.setStatus(DownloadItem.Status.PAUSED);
            }
        }
        
        appendLog("Paused " + pausedCount + " active download(s)");
    }

    @FXML
    private void removeSelectedItems() {
        ObservableList<DownloadItem> selectedItems = downloadTable.getSelectionModel().getSelectedItems();
        if (!selectedItems.isEmpty()) {
            downloadItems.removeAll(selectedItems);
            appendLog("Removed " + selectedItems.size() + " item(s) from queue");
        }
    }

    @FXML
    private void browseYtDlpPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory Containing yt-dlp");

        File selectedDirectory = directoryChooser.showDialog(browseYtDlpButton.getScene().getWindow());
        if (selectedDirectory != null) {
            ytDlpPathField.setText(selectedDirectory.getAbsolutePath() + "/yt-dlp");
        }
    }

    @FXML
    private void browseDefaultOutput() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Default Output Directory");

        File selectedDirectory = directoryChooser.showDialog(browseDefaultOutputButton.getScene().getWindow());
        if (selectedDirectory != null) {
            defaultOutputField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void testYtDlp() {
        ytDlpService.setYtDlpPath(ytDlpPathField.getText());

        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ytDlpService.isYtDlpAvailable();
            }
        };

        testTask.setOnSucceeded(e -> {
            boolean available = testTask.getValue();
            if (available) {
                ytDlpStatusLabel.setText("yt-dlp is working correctly");
                ytDlpStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                ytDlpStatusLabel.setText("yt-dlp not found or not working");
                ytDlpStatusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        executorService.submit(testTask);
    }
    
    @FXML
    private void testFFmpeg() {
        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ytDlpService.isFFmpegAvailable();
            }
        };

        testTask.setOnSucceeded(e -> {
            boolean available = testTask.getValue();
            if (available) {
                ffmpegStatusLabel.setText("FFmpeg is working correctly");
                ffmpegStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                ffmpegStatusLabel.setText("FFmpeg not found in PATH");
                ffmpegStatusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        executorService.submit(testTask);
    }

    private void checkDependencyStatus() {
        // Check yt-dlp status
        Task<Boolean> ytDlpCheckTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ytDlpService.isYtDlpAvailable();
            }
        };

        ytDlpCheckTask.setOnSucceeded(e -> {
            boolean available = ytDlpCheckTask.getValue();
            if (available) {
                ytDlpStatusLabel.setText("yt-dlp is available");
                ytDlpStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                ytDlpStatusLabel.setText("yt-dlp not found - please install or configure path");
                ytDlpStatusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        // Check FFmpeg status  
        Task<Boolean> ffmpegCheckTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ytDlpService.isFFmpegAvailable();
            }
        };

        ffmpegCheckTask.setOnSucceeded(e -> {
            boolean available = ffmpegCheckTask.getValue();
            if (available) {
                ffmpegStatusLabel.setText("FFmpeg is available");
                ffmpegStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                ffmpegStatusLabel.setText("FFmpeg not found - install for advanced features");
                ffmpegStatusLabel.setStyle("-fx-text-fill: orange;");
            }
        });

        executorService.submit(ytDlpCheckTask);
        executorService.submit(ffmpegCheckTask);
    }

    private void appendLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = String.format("[%s] %s\n", timestamp, message);
        logTextArea.appendText(logEntry);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        logger.info("Shutting down application...");
        
        // Cancel all active download tasks
        if (activeTasks != null) {
            for (Task<Void> task : activeTasks) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
            activeTasks.clear();
        }
        
        // Kill all yt-dlp processes
        if (ytDlpService != null) {
            ytDlpService.killAllActiveProcesses();
        }
        
        // Shutdown executor
        if (executorService != null) {
            executorService.shutdownNow(); // Force shutdown instead of graceful
            try {
                if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for executor shutdown");
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Application shutdown complete");
    }
}