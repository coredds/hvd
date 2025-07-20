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
    private Label statusLabel;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");

        // Initialize services
        executorService = Executors.newCachedThreadPool();
        downloadItems = FXCollections.observableArrayList();
        ytDlpService = new YtDlpService();
        preferencesService = new PreferencesService();

        // Setup Download Type Toggle Group
        ToggleGroup downloadTypeGroup = new ToggleGroup();
        videoRadioButton.setToggleGroup(downloadTypeGroup);
        audioRadioButton.setToggleGroup(downloadTypeGroup);

        // Load and apply saved preferences
        loadPreferences();

        // Setup format combo boxes
        audioFormatComboBox.setItems(FXCollections.observableArrayList(
                "mp3", "aac", "m4a", "opus", "flac", "wav"));
        
        videoFormatComboBox.setItems(FXCollections.observableArrayList(
                "best", "mp4", "webm", "mkv", "avi"));

        // Setup combo box visibility based on download type
        downloadTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == audioRadioButton) {
                audioFormatComboBox.setVisible(true);
                videoFormatComboBox.setVisible(false);
            } else {
                audioFormatComboBox.setVisible(false);
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
        checkYtDlpStatus();
    }

    private void setupDownloadsTable() {

        // Setup output directory
        outputDirectoryField.setText(System.getProperty("user.home") + File.separator + "Downloads");

        // Setup table
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

        String savedVideoFormat = preferencesService.getPreference(PreferencesService.VIDEO_FORMAT, "best");
        videoFormatComboBox.setValue(savedVideoFormat);

        // Load checkbox preferences
        embedSubtitlesCheckBox.setSelected(
            preferencesService.getBooleanPreference(PreferencesService.EMBED_SUBTITLES, false));
        embedThumbnailCheckBox.setSelected(
            preferencesService.getBooleanPreference(PreferencesService.EMBED_THUMBNAIL, false));
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
            if (newVal != null && !newVal.trim().isEmpty()) {
                preferencesService.setPreference(PreferencesService.OUTPUT_DIRECTORY, newVal);
                preferencesService.savePreferences();
            }
        });

        // Save format preferences when changed
        audioFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preferencesService.setPreference(PreferencesService.AUDIO_FORMAT, newVal);
                preferencesService.savePreferences();
            }
        });

        videoFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preferencesService.setPreference(PreferencesService.VIDEO_FORMAT, newVal);
                preferencesService.savePreferences();
            }
        });

        // Save checkbox preferences when changed
        embedSubtitlesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferencesService.setBooleanPreference(PreferencesService.EMBED_SUBTITLES, newVal);
            preferencesService.savePreferences();
        });

        embedThumbnailCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferencesService.setBooleanPreference(PreferencesService.EMBED_THUMBNAIL, newVal);
            preferencesService.savePreferences();
        });

        addMetadataCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferencesService.setBooleanPreference(PreferencesService.ADD_METADATA, newVal);
            preferencesService.savePreferences();
        });

        // Save settings tab preferences when changed
        if (ytDlpPathField != null) {
            ytDlpPathField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    preferencesService.setPreference(PreferencesService.YTDLP_PATH, newVal);
                    preferencesService.savePreferences();
                }
            });
        }

        if (defaultOutputField != null) {
            defaultOutputField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
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
        boolean isAudioMode = audioRadioButton.isSelected();
        preferencesService.setBooleanPreference(PreferencesService.DOWNLOAD_TYPE_AUDIO, isAudioMode);
        preferencesService.savePreferences();
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
                DownloadItem item = new DownloadItem(url);

                // Set format based on selection
                if (audioRadioButton.isSelected()) {
                    item.setFormat("audio-" + audioFormatComboBox.getValue());
                } else {
                    item.setFormat("video-" + videoFormatComboBox.getValue());
                }

                downloadItems.add(item);

                // Try to extract title asynchronously
                ytDlpService.extractVideoInfo(url).thenAccept(title -> {
                    javafx.application.Platform.runLater(() -> item.setTitle(title));
                });
            }
        }

        urlTextArea.clear();
        appendLog("Added " + urls.length + " item(s) to queue");
    }

    @FXML
    private void startAllDownloads() {
        if (downloadItems.isEmpty()) {
            showAlert("Information", "No items in download queue");
            return;
        }

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

        boolean audioOnly = audioRadioButton.isSelected();
        String audioFormat = audioFormatComboBox.getValue();
        String videoFormat = videoFormatComboBox.getValue();

        Task<Void> downloadTask = ytDlpService.createDownloadTask(
                item, audioOnly, audioFormat, videoFormat,
                embedSubtitlesCheckBox.isSelected(),
                embedThumbnailCheckBox.isSelected(),
                addMetadataCheckBox.isSelected(),
                this::appendLog);

        executorService.submit(downloadTask);
    }

    @FXML
    private void pauseAllDownloads() {
        // Note: This is a simplified implementation
        // In a real application, you'd need to manage Task cancellation properly
        appendLog("Pause functionality not fully implemented in this version");
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
                statusLabel.setText("yt-dlp is working correctly");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("yt-dlp not found or not working");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        executorService.submit(testTask);
    }

    private void checkYtDlpStatus() {
        Task<Boolean> checkTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return ytDlpService.isYtDlpAvailable();
            }
        };

        checkTask.setOnSucceeded(e -> {
            boolean available = checkTask.getValue();
            if (available) {
                statusLabel.setText("yt-dlp is available");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("yt-dlp not found - please install or configure path");
                statusLabel.setStyle("-fx-text-fill: orange;");
            }
        });

        executorService.submit(checkTask);
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
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}