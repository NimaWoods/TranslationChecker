package com.gui.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gui.services.LocaleEncodingService.getLocaleWithEncoding;

public class FileEncodingConverter {

    Logger logger = Logger.getLogger(getClass().getName());

    final Map<Path, String> unreadableFiles = new HashMap<>();

    public Map<Path, String> getUnreadableFiles() {
        return unreadableFiles;
    }

    public static void convertFileEncoding(String inputFilePath, Charset inputEncoding, String outputEncoding) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), inputEncoding));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputFilePath), outputEncoding))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }

            System.out.println("Converted file " + inputFilePath + " from " + inputEncoding + " to " + outputEncoding);
        } catch (IOException e) {
            System.err.println("Error converting file: " + inputFilePath);
            e.printStackTrace();
        }
    }

    Path convertFile(String lang, Path path, boolean convertFiles, Charset inputEncoding, List<String[]> convertedFiles) {
        if (convertFiles) {
            Charset outputEncoding = getLocaleWithEncoding(lang).getEncoding();
            if (outputEncoding != null) {
                if (!inputEncoding.equals(outputEncoding)) {
                    String inputFilePath = path.toString();
                    try {
                        FileEncodingConverter.convertFileEncoding(inputFilePath, inputEncoding, outputEncoding.name());
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error converting file: " + inputFilePath, e);
                        unreadableFiles.put(path, "Error converting file: " + e.getMessage());
                        return null;
                    }
                    convertedFiles.add(new String[]{inputFilePath, inputEncoding.name(), outputEncoding.name()});
                    path = Paths.get(inputFilePath);
                }
            } else {
                logger.log(Level.WARNING, "No encoding found for language: " + lang);
            }
        }
        return path;
    }

    public static String convertPath(String path) {
        return path.replace("\\", "/");
    }
}
