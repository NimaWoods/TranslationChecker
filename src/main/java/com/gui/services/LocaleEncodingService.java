package com.gui.services;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public class LocaleEncodingService {

    public static localeWithEncoding getLocaleWithEncoding(String lang) {
        for (localeWithEncoding locale : localeWithEncoding.values()) {
            if (locale.getLocale().getLanguage().equals(lang)) {
                return locale;
            }
        }
        return localeWithEncoding.GERMAN;
    }

    // Inner class for handling encoding and locale
    public enum localeWithEncoding {
        GERMAN(Locale.GERMAN, StandardCharsets.ISO_8859_1),
        ENGLISH(Locale.ENGLISH, StandardCharsets.ISO_8859_1),
        FRENCH(Locale.FRENCH, StandardCharsets.ISO_8859_1),
        ITALIAN(Locale.ITALIAN, StandardCharsets.ISO_8859_1),
        RUSSIAN(new Locale("ru"), StandardCharsets.UTF_8),
        ROMANIAN(new Locale("ro"), Charset.forName("ISO-8859-2")),
        DUTCH(new Locale("nl"), StandardCharsets.ISO_8859_1),
        SPANISH(new Locale("es"), StandardCharsets.ISO_8859_1),
        HUNGARIAN(new Locale("hu"), Charset.forName("ISO-8859-2"));

        private final Locale locale;
        private final Charset encoding;

        localeWithEncoding(Locale locale, Charset encoding) {
            this.locale = locale;
            this.encoding = encoding;
        }

        public Locale getLocale() {

            return locale;
        }

        public Charset getEncoding() {

            return Charset.forName(String.valueOf(encoding));
        }
    }

    // Inner class to hold language properties and associated file path
    public static class LanguageProperties {
        private final Properties properties;
        private final Path file;

        public LanguageProperties(Properties properties, Path file) {
            this.properties = properties;
            this.file = file;
        }

        public Properties getProperties() {
            return properties;
        }

        public Path getPath() {
            return file;
        }
    }

    private Charset getFileEncoding(Path path, Charset defaultEncoding) {
        // Implement the method logic here
        return defaultEncoding;
    }

}
