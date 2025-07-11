package com.gui.manager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gui.contsants.LanguagesConstant;

/**
 * Manager class for handling language settings
 * Allows loading and saving language settings from/to properties file
 */
/**
 * Manager class for handling language settings
 * Loads and saves language settings from/to properties file
 * Provides methods to add and remove languages
 */
public class LanguageManager {
    private static LanguageManager instance;
    private static final Logger logger = Logger.getLogger(LanguageManager.class.getName());
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String LANGUAGES_PREFIX = "language.";
    private static final String ENCODINGS_PREFIX = "encoding.";
    
    private final Properties settings;
    private final Map<String, Locale> availableLanguages;
    private final Map<String, Charset> languageEncodings;
    
    /**
     * Gets the singleton instance of LanguageManager
     * 
     * @return The LanguageManager instance
     */
    public static synchronized LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private LanguageManager() {
        settings = new Properties();
        availableLanguages = new HashMap<>();
        languageEncodings = new HashMap<>();
        loadSettings();
    }
    
    /**
     * Loads settings from the properties file
     * If no language settings are found, initializes with default languages
     */
    private void loadSettings() {
        try (FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
            settings.load(in);
            
            // Check if we have any language settings
            boolean hasLanguageSettings = false;
            for (String key : settings.stringPropertyNames()) {
                if (key.startsWith(LANGUAGES_PREFIX)) {
                    hasLanguageSettings = true;
                    break;
                }
            }
            
            if (!hasLanguageSettings) {
                // Initialize with default languages
                initializeDefaultLanguages();
                saveSettings();
            } else {
                // Load languages from properties
                loadLanguagesFromProperties();
            }
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading settings", e);
            // Initialize with default languages
            initializeDefaultLanguages();
            saveSettings();
        }
    }
    
    /**
     * Initializes the language settings with default values from LanguagesConstant
     */
    private void initializeDefaultLanguages() {
        // Add default languages from LanguagesConstant enum
        for (LanguagesConstant lang : LanguagesConstant.values()) {
            String langCode = lang.getLanguageCode();
            String langName = lang.getDisplayName();
            String encoding = lang.getEncoding().name();
            
            settings.setProperty(LANGUAGES_PREFIX + langCode, langName);
            settings.setProperty(ENCODINGS_PREFIX + langCode, encoding);
            
            availableLanguages.put(langCode, lang.getLocale());
            languageEncodings.put(langCode, lang.getEncoding());
        }
    }
    
    /**
     * Loads languages from properties file
     */
    private void loadLanguagesFromProperties() {
        availableLanguages.clear();
        languageEncodings.clear();
        
        for (String key : settings.stringPropertyNames()) {
            if (key.startsWith(LANGUAGES_PREFIX)) {
                String langCode = key.substring(LANGUAGES_PREFIX.length());
                String langName = settings.getProperty(key);
                Locale locale = new Locale(langCode);
                availableLanguages.put(langCode, locale);
                
                // Get encoding for this language
                String encodingKey = ENCODINGS_PREFIX + langCode;
                String encodingName = settings.getProperty(encodingKey, StandardCharsets.UTF_8.name());
                try {
                    Charset charset = Charset.forName(encodingName);
                    languageEncodings.put(langCode, charset);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Invalid charset: " + encodingName + ", using UTF-8", e);
                    languageEncodings.put(langCode, StandardCharsets.UTF_8);
                }
            }
        }
    }
    
    /**
     * Saves the current settings to the properties file
     */
    public void saveSettings() {
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            settings.store(out, "Updated language settings");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving settings", e);
        }
    }
    
    /**
     * Adds a new language
     * 
     * @param langCode ISO language code (e.g., "de" for German)
     * @param encodingName Encoding name (e.g., "ISO-8859-1")
     * @return true if added successfully, false otherwise
     */
    public boolean addLanguage(String langCode, String encodingName) {
        try {
            Locale locale = new Locale(langCode);
            Charset charset = Charset.forName(encodingName);
            
            // Get language name to store in properties
            String langName = locale.getDisplayLanguage(Locale.ENGLISH);
            settings.setProperty(LANGUAGES_PREFIX + langCode, langName); // Using langName here
            settings.setProperty(ENCODINGS_PREFIX + langCode, encodingName);
            
            availableLanguages.put(langCode, locale);
            languageEncodings.put(langCode, charset);
            
            saveSettings();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding language: " + langCode, e);
            return false;
        }
    }
    
    /**
     * Removes a language
     * 
     * @param langCode ISO language code to remove
     * @return true if removed successfully, false otherwise
     */
    public boolean removeLanguage(String langCode) {
        if (availableLanguages.containsKey(langCode)) {
            settings.remove(LANGUAGES_PREFIX + langCode);
            settings.remove(ENCODINGS_PREFIX + langCode);
            
            availableLanguages.remove(langCode);
            languageEncodings.remove(langCode);
            
            saveSettings();
            return true;
        }
        return false;
    }
    
    /**
     * Gets all available languages
     * 
     * @return Map of language codes to Locale objects
     */
    public Map<String, Locale> getAvailableLanguages() {
        return new HashMap<>(availableLanguages);
    }
    
    /**
     * Gets the encoding for a specific language
     * 
     * @param langCode ISO language code
     * @return Charset for the language, or UTF-8 if not found
     */
    public Charset getEncodingForLanguage(String langCode) {
        return languageEncodings.getOrDefault(langCode, StandardCharsets.UTF_8);
    }
    
    /**
     * Gets a list of all available locales
     * 
     * @return List of available Locale objects
     */
    public List<Locale> getAvailableLocales() {
        return new ArrayList<>(availableLanguages.values());
    }
    
    /**
     * Gets the Properties object
     * 
     * @return Properties object
     */
    public Properties getSettings() {
        return settings;
    }
}
