package coredds.hvd.controller;

import coredds.hvd.model.DownloadItem;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class ProgressCell extends TableCell<DownloadItem, Double> {

    private static final Logger logger = LoggerFactory.getLogger(ProgressCell.class);

    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Button openFileButton;
    private final HBox container;

    public ProgressCell() {
        // Create UI components
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(160);

        progressLabel = new Label("0%");
        progressLabel.setPrefWidth(70);
        progressLabel.setAlignment(Pos.CENTER_RIGHT);

        openFileButton = new Button("📁");
        openFileButton.setTooltip(new Tooltip("Open downloaded file"));
        openFileButton.setPrefWidth(30);
        openFileButton.setVisible(false);
        openFileButton.setOnAction(event -> openFile());

        // Create container with space for progress display and open button
        container = new HBox(5);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(progressBar, progressLabel, openFileButton);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(Double progress, boolean empty) {
        super.updateItem(progress, empty);

        if (empty || progress == null) {
            setGraphic(null);
            return;
        }

        DownloadItem item = getTableRow().getItem();
        if (item == null) {
            setGraphic(null);
            return;
        }

        // Update progress bar and label
        progressBar.setProgress(progress);

        if (item.getStatus() == DownloadItem.Status.COMPLETED) {
            progressLabel.setText("Complete");
            progressLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            progressBar.setVisible(false);
            openFileButton.setVisible(true);
        } else if (item.getStatus() == DownloadItem.Status.ERROR) {
            progressLabel.setText("Error");
            progressLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            progressBar.setVisible(false);
            openFileButton.setVisible(false);
        } else {
            int percentage = (int) Math.round(progress * 100);
            progressLabel.setText(percentage + "%");
            progressLabel.setStyle("-fx-text-fill: #333333;");
            progressBar.setVisible(true);
            openFileButton.setVisible(false);
        }

        setGraphic(container);
    }

    private void openFile() {
        DownloadItem item = getTableRow().getItem();
        if (item == null || item.getFilePath() == null || item.getFilePath().isEmpty()) {
            showAlert("Error", "File path not available. The file may have been moved or deleted.");
            return;
        }

        File file = new File(item.getFilePath());
        if (!file.exists()) {
            showAlert("Error", "File not found: " + item.getFilePath());
            return;
        }

        try {
            // Use Desktop API to open file with default application
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file);
                } else {
                    // Fallback: open containing folder
                    desktop.open(file.getParentFile());
                }
            } else {
                // Platform-specific fallback
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", file.getAbsolutePath());
                } else if (os.contains("win")) {
                    pb = new ProcessBuilder("cmd", "/c", "start", file.getAbsolutePath());
                } else {
                    // Linux/Unix
                    pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                }

                pb.start();
            }

            logger.info("Opened file: " + file.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to open file: " + file.getAbsolutePath(), e);
            showAlert("Error", "Failed to open file: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}