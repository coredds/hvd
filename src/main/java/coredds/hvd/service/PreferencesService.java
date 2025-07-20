package coredds.hvd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.Properties;

/**
 * Service for managing user preferences and configuration persistence
 */
public class PreferencesService {
    
    private static final Logger logger = LoggerFactory.getLogger(PreferencesService.class);
    private static final String PREFERENCES_FILE = "hvd-preferences.properties";
    
    private Properties preferences;
    private File preferencesFile;
    
    public PreferencesService() {
        preferences = new Properties();
        
        // Use application directory for storing preferences (same folder as JAR)
        File appDir = getApplicationDirectory();
        preferencesFile = new File(appDir, PREFERENCES_FILE);
        
        loadPreferences();
    }
    
    /**
     * Load preferences from file
     */
    private void loadPreferences() {
        if (preferencesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(preferencesFile)) {
                preferences.load(fis);
                logger.info("Loaded preferences from: {}", preferencesFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to load preferences: {}", e.getMessage());
            }
        } else {
            logger.info("No existing preferences file found, using defaults");
        }
    }
    
    /**
     * Save preferences to file
     */
    public void savePreferences() {
        try (FileOutputStream fos = new FileOutputStream(preferencesFile)) {
            preferences.store(fos, "hvd Media Downloader Preferences");
            logger.debug("Saved preferences to: {}", preferencesFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save preferences: {}", e.getMessage());
        }
    }
    
    /**
     * Get a preference value
     */
    public String getPreference(String key, String defaultValue) {
        return preferences.getProperty(key, defaultValue);
    }
    
    /**
     * Set a preference value
     */
    public void setPreference(String key, String value) {
        if (value != null) {
            preferences.setProperty(key, value);
        } else {
            preferences.remove(key);
        }
    }
    
    /**
     * Get a boolean preference
     */
    public boolean getBooleanPreference(String key, boolean defaultValue) {
        String value = preferences.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Set a boolean preference
     */
    public void setBooleanPreference(String key, boolean value) {
        preferences.setProperty(key, String.valueOf(value));
    }
    
    /**
     * Get the directory where the application is running from
     */
    private File getApplicationDirectory() {
        try {
            // Get the location of the JAR file
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            
            // If it's a JAR file, return its parent directory
            if (jarFile.isFile() && jarPath.endsWith(".jar")) {
                return jarFile.getParentFile();
            }
            
            // If running from IDE/classes directory, use current working directory
            return new File(System.getProperty("user.dir"));
            
        } catch (Exception e) {
            logger.warn("Failed to determine application directory, using current working directory: {}", e.getMessage());
            return new File(System.getProperty("user.dir"));
        }
    }
    
    // Preference keys constants
    public static final String OUTPUT_DIRECTORY = "output.directory";
    public static final String AUDIO_FORMAT = "audio.format";
    public static final String VIDEO_FORMAT = "video.format";
    public static final String DOWNLOAD_TYPE_AUDIO = "download.type.audio";
    public static final String EMBED_SUBTITLES = "embed.subtitles";
    public static final String EMBED_THUMBNAIL = "embed.thumbnail";
    public static final String ADD_METADATA = "add.metadata";
    public static final String YTDLP_PATH = "ytdlp.path";
    public static final String DEFAULT_OUTPUT_DIRECTORY = "default.output.directory";
} 