package coredds.hvd;

import coredds.hvd.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
        
        // Get controller reference for shutdown handling
        MainController controller = fxmlLoader.getController();

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        // Set window title
        stage.setTitle("Media Downloader");
        
        // Set application icons
        stage.getIcons().addAll(
            new Image(getClass().getResourceAsStream("/icons/32.png")),
            new Image(getClass().getResourceAsStream("/icons/48.png")),
            new Image(getClass().getResourceAsStream("/icons/64.png")),
            new Image(getClass().getResourceAsStream("/icons/128.png")),
            new Image(getClass().getResourceAsStream("/icons/256.png"))
        );
        
        // Add shutdown hook to ensure cleanup on abrupt exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered - cleaning up...");
            if (controller != null) {
                controller.shutdown();
            }
        }));
        
        // Handle window close event
        stage.setOnCloseRequest(e -> {
            logger.info("Window close requested - shutting down...");
            controller.shutdown();
        });
        
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}