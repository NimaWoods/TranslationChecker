package com.gui.services;

import com.gui.contsants.Language;

import java.nio.charset.Charset;
import java.nio.file.Path;

public class LocaleEncodingService {

    public static Language getLocaleWithEncoding(String lang) {
        for (Language locale : Language.values()) {
            if (locale.getLocale().getLanguage().equals(lang)) {
                return locale;
            }
        }
        return Language.GERMAN;
    }

    private Charset getFileEncoding(Path path, Charset defaultEncoding) {
        // Implement the method logic here
        return defaultEncoding;
    }

}
