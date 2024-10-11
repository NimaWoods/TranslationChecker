package com.gui.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gui.contsants.Language;

public class LocaleEncodingService {

    Logger logger = Logger.getLogger(LocaleEncodingService.class.getName());

    public static Language getLocaleWithEncoding(String lang) {
        for (Language locale : Language.values()) {
            if (locale.getLocale().getLanguage().equals(lang)) {
                return locale;
            }
        }
        return Language.GERMAN;
    }

    private Charset getFileEncoding(Path path, Charset defaultEncoding) {
        return defaultEncoding;
    }

    public Map<String, String[]> loadTranslationsForKey(String key, Path selectedFilePath) {
        Map<String, String[]> translationsWithPaths = new HashMap<>();

        Path parentDir = selectedFilePath.getParent();

        try (Stream<Path> files = Files.list(parentDir)) {
            List<Path> propertiesFiles = files.filter(
                            path -> path.getFileName().toString().startsWith("messages_") && path.getFileName().toString().endsWith(".properties"))
                    .collect(Collectors.toList());

            for (Path path : propertiesFiles) {
                Properties properties = new Properties();
                try (InputStream inputStream = Files.newInputStream(path)) {
                    properties.load(inputStream);
                    String value = properties.getProperty(key);
                    if (value != null) {
                        String language = extractLanguageFromFileName(path.getFileName().toString());
                        translationsWithPaths.put(language, new String[] { value, path.toString() });
                    } else {
                        translationsWithPaths.put(extractLanguageFromFileName(path.getFileName().toString()),
                                new String[] { "", path.toString() });
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error loading properties file: " + path, e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading properties files from directory: " + parentDir, e);
        }
        return translationsWithPaths;
    }

    private String extractLanguageFromFileName(String fileName) {
        int startIndex = fileName.indexOf('_') + 1;
        int endIndex = fileName.indexOf('.');
        if (startIndex > 0 && endIndex > startIndex) {
            return fileName.substring(startIndex, endIndex);
        }
        return "unknown";
    }
}
