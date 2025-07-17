package coredds.hvd;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HvdApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(HvdApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting hvd application");

        FXMLLoader fxmlLoader = new FXMLLoader(HvdApplication.class.getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 720);

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage.setTitle("hvd");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}