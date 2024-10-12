package com.gui.services;

import java.io.*;
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

    static Logger logger = Logger.getLogger(FileEncodingConverter.class.getName());

    final Map<String, String> unreadableFiles = new HashMap<>();

    public Map<String, String> getUnreadableFiles() {
        return unreadableFiles;
    }

    public static void convertFileEncoding(File inputFile, Charset sourceCharset, Charset targetCharset) {
        File tempFile = new File(inputFile.getAbsolutePath() + ".tmp");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), sourceCharset));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), targetCharset))) {

            logger.log(Level.INFO, "Starting conversion for file: {0}", inputFile.getAbsolutePath());
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }

            // Rename the temp file to the original file
            if (!tempFile.renameTo(inputFile)) {
                throw new IOException("Failed to rename temp file to " + inputFile.getName());
            }
            logger.log(Level.INFO, "File encoding conversion completed for: {0}", inputFile.getAbsolutePath());

        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Unsupported encoding encountered: {0}", e.getMessage());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "File not found: {0}", inputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error occurred during file conversion: {0}", e.getMessage());
        } finally {
            // Clean up temp file in case of failure
            if (tempFile.exists() && !tempFile.delete()) {
                logger.log(Level.WARNING, "Failed to delete temp file: {0}", tempFile.getAbsolutePath());
            }
        }
    }

    public Path convertFile(String lang, Path path, boolean convertFiles, Charset inputEncoding, List<String[]> convertedFiles) {

        String inputFilePath = path.toString();
        File inputFile = new File(inputFilePath);

        if (convertFiles) {
            Charset outputEncoding = getLocaleWithEncoding(lang).getEncoding();
	        if (!inputEncoding.equals(outputEncoding)) {
		        try {
			        FileEncodingConverter.convertFileEncoding(inputFile, inputEncoding, outputEncoding);
		        } catch (Exception e) {
			        logger.log(Level.WARNING, "Error converting file: " + inputFilePath, e);
			        unreadableFiles.put(path.toString(), "Error converting file: " + e.getMessage());
			        return null;
		        }
		        convertedFiles.add(new String[]{inputFilePath, inputEncoding.name(), outputEncoding.name()});
		        path = Paths.get(inputFilePath);
	        }
        }
        return path;
    }
}
