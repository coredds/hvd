package coredds.hvd.controller;

import coredds.hvd.model.DownloadItem;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressCell extends TableCell<DownloadItem, Double> {

    private static final Logger logger = LoggerFactory.getLogger(ProgressCell.class);

    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final HBox container;

    public ProgressCell() {
        // Create UI components
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(180);

        progressLabel = new Label("0%");
        progressLabel.setPrefWidth(70);
        progressLabel.setAlignment(Pos.CENTER_RIGHT);

        // Create container with just progress display (removed file navigation button)
        container = new HBox(5);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(progressBar, progressLabel);
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
            int percentage = (int) Math.round(progress * 100);
            progressLabel.setText(percentage + "%");
            progressLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            progressBar.setVisible(true);
        } else if (item.getStatus() == DownloadItem.Status.ERROR) {
            progressLabel.setText("Error");
            progressLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            progressBar.setVisible(false);
        } else {
            int percentage = (int) Math.round(progress * 100);
            progressLabel.setText(percentage + "%");
            progressLabel.setStyle("-fx-text-fill: #333333;");
            progressBar.setVisible(true);
        }

        setGraphic(container);
    }


}