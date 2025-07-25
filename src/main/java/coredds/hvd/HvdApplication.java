package coredds.hvd;

import coredds.hvd.controller.MainController;
import coredds.hvd.service.PreferencesService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class HvdApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(HvdApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting hvd application");

        // Detect and set the appropriate locale
        Locale currentLocale = detectAndSetLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", currentLocale);
        
        logger.info("Using locale: {} ({})", currentLocale.toLanguageTag(), currentLocale.getDisplayName());

        FXMLLoader fxmlLoader = new FXMLLoader(HvdApplication.class.getResource("/fxml/main-view.fxml"));
        fxmlLoader.setResources(bundle);
        Scene scene = new Scene(fxmlLoader.load(), 960, 720);
        
        // Get controller reference for shutdown handling
        MainController controller = fxmlLoader.getController();

        // Add CSS styling
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        // Set localized window title
        String windowTitle = bundle.getString("app.title");
        stage.setTitle(windowTitle);
        
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

    /**
     * Detects the operating system locale and sets the most appropriate supported locale.
     * First checks for user preference, then falls back to OS detection.
     * Supported locales: English (default), Portuguese (Brazil), Spanish
     * @return The locale to use for the application
     */
    private Locale detectAndSetLocale() {
        // Check for user language preference first
        PreferencesService preferencesService = new PreferencesService();
        String userLanguage = preferencesService.getPreference(PreferencesService.LANGUAGE, "auto");
        
        Locale appLocale;
        
        if (!"auto".equals(userLanguage)) {
            // User has selected a specific language
            appLocale = getLocaleFromLanguageCode(userLanguage);
            logger.info("User language preference: {} - using {}", userLanguage, appLocale.getDisplayName());
        } else {
            // Auto-detect from system
            Locale systemLocale = Locale.getDefault();
            String language = systemLocale.getLanguage();
            
            logger.info("System locale detected: {} ({})", systemLocale.toLanguageTag(), systemLocale.getDisplayName());

            if ("pt".equals(language)) {
                // Portuguese - default to Brazilian Portuguese
                appLocale = new Locale("pt", "BR");
                logger.info("Portuguese detected - using Brazilian Portuguese");
            } else if ("es".equals(language)) {
                // Spanish - use generic Spanish
                appLocale = new Locale("es");
                logger.info("Spanish detected - using Spanish");
            } else if ("it".equals(language)) {
                // Italian
                appLocale = new Locale("it");
                logger.info("Italian detected - using Italian");
            } else if ("ja".equals(language)) {
                // Japanese
                appLocale = new Locale("ja");
                logger.info("Japanese detected - using Japanese");
            } else if ("de".equals(language)) {
                // German
                appLocale = new Locale("de");
                logger.info("German detected - using German");
            } else {
                // Default to English for any other language
                appLocale = Locale.ENGLISH;
                if (!"en".equals(language)) {
                    logger.info("Unsupported language '{}' detected - defaulting to English", language);
                } else {
                    logger.info("English detected - using English");
                }
            }
        }

        // Set the default locale for the entire application
        Locale.setDefault(appLocale);
        
        return appLocale;
    }

    /**
     * Get a Locale object from a language code
     */
    private Locale getLocaleFromLanguageCode(String languageCode) {
        switch (languageCode) {
            case "en":
                return Locale.ENGLISH;
            case "pt_BR":
                return new Locale("pt", "BR");
            case "es":
                return new Locale("es");
            case "it":
                return new Locale("it");
            case "ja":
                return new Locale("ja");
            case "de":
                return new Locale("de");
            default:
                return Locale.ENGLISH;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}