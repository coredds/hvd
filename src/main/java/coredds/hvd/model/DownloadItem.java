package coredds.hvd.model;

import javafx.beans.property.*;

public class DownloadItem {

    public enum Status {
        QUEUED("Queued"),
        DOWNLOADING("Downloading"),
        PAUSED("Paused"),
        COMPLETED("Completed"),
        ERROR("Error");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final StringProperty url = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty format = new SimpleStringProperty();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.QUEUED);
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final StringProperty filePath = new SimpleStringProperty();
    private boolean noPlaylist = false;

    public DownloadItem() {
        // Default constructor
    }

    public DownloadItem(String url) {
        this.url.set(url);
        this.title.set("Unknown");
        this.format.set("best");
    }

    public DownloadItem(String url, String title, String format) {
        this.url.set(url);
        this.title.set(title);
        this.format.set(format);
    }

    // URL property
    public StringProperty urlProperty() {
        return url;
    }

    public String getUrl() {
        return url.get();
    }

    public void setUrl(String url) {
        this.url.set(url);
    }

    // Title property
    public StringProperty titleProperty() {
        return title;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    // Format property
    public StringProperty formatProperty() {
        return format;
    }

    public String getFormat() {
        return format.get();
    }

    public void setFormat(String format) {
        this.format.set(format);
    }

    // Status property
    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public Status getStatus() {
        return status.get();
    }

    public void setStatus(Status status) {
        this.status.set(status);
    }

    // Progress property
    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    // Error message property
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    // File path property
    public StringProperty filePathProperty() {
        return filePath;
    }

    public String getFilePath() {
        return filePath.get();
    }

    public void setFilePath(String filePath) {
        this.filePath.set(filePath);
    }

    // NoPlaylist property
    public boolean isNoPlaylist() {
        return noPlaylist;
    }

    public void setNoPlaylist(boolean noPlaylist) {
        this.noPlaylist = noPlaylist;
    }
}