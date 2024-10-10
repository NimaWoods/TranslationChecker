/*
 * Created on 16/07/2024, 13:23
 *
 * Copyright (c) 2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding.DUTCH;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding.HUNGARIAN;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding.ROMANIAN;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding.SPANISH;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;
import static java.util.Locale.GERMAN;
import static java.util.Locale.ITALIAN;

public class ConstantsAndMethodsForImport {

    // Debug mode toggle
    protected static final boolean DEBUG = true;

    // New locales and list of currently available locales
    public static final Locale[] AVAILABLE_LOCALES = { GERMAN, ENGLISH, FRENCH, ITALIAN, ROMANIAN.getLocale(), DUTCH.getLocale(), SPANISH.getLocale(), HUNGARIAN.getLocale() };

    public enum LocaleWithEncoding {
        GERMAN(Locale.GERMAN, StandardCharsets.ISO_8859_1),
        ENGLISH(Locale.ENGLISH, StandardCharsets.ISO_8859_1),
        FRENCH(Locale.FRENCH, StandardCharsets.ISO_8859_1),
        ITALIAN(Locale.ITALIAN, StandardCharsets.ISO_8859_1),
        ROMANIAN(new Locale("ro"), Charset.forName("ISO-8859-2")),
        DUTCH(new Locale("nl"), StandardCharsets.ISO_8859_1),
        SPANISH(new Locale("es"), StandardCharsets.ISO_8859_1),
        HUNGARIAN(new Locale("hu"), Charset.forName("ISO-8859-2"));

        private final Locale locale;
        private final Charset encoding;

        LocaleWithEncoding(Locale locale, Charset encoding) {
            this.locale = locale;
            this.encoding = encoding;
        }

        public Locale getLocale() {
            return locale;
        }

        public Charset getEncoding() {
            return encoding;
        }
    }


    // Paths in the monorepo
    protected static final Path ROOT = Paths.get(".");
    protected static final String SRC = "src/";
    protected static final String GHS = "ghs/";
    protected static final String COMMON = "common/";
    protected static final String FRAMEWORK = "framework/";
    protected static final String DE_TOPSYSTEM = "de/topsystem/";

    protected static final String BASEDATA_LOCALIZATION_PATH = "database/liquibase/latest/data/i18n/localized-";
    protected static final String[] BASEDATA_FILE_NAMES = new String[]{
            "abs-req-status-history",
            "cargo-discrepancies",
            "cargo-event-history-reasons",
            "cargo-events",
            "constants",
            "contract-states",
            "contract_types",
            "corporate-regulation-rule-values",
            "corporate-regulation-rules",
            "durations",
            "matrix-dimension-types",
            "price-formula-parameters",
            "price_criteria",
            "roster-kpi-id-suffix",
            "roster-kpi",
            "search-restrictions",
            "shp-discrepancy-remarks",
            "sla-task-attributes"
    };
    // To differentiate between project and product basedata files
    protected static final String BASEDATA_PRODUCT_FILE_ENDING = ".csv";
    protected static final String BASEDATA_PROJECT_FILE_ENDING = "-project.csv";

    // Separator char for basedata imports and DeepL tool
    protected static final char BASEDATA_SEPARATOR_CHAR = '§';
    // Separator char for .properties import
    protected static final String LOCALIZATION_SEPARATOR_CHAR = "\\t";


    // DeepL
    /*
        Interner Key: beadc63e-c0ec-a243-8f4f-43e60a907156:fx (Für Testzwecke auf jeden fall diesen nutzen)
        EPG Connect Key: 47d3f6b1-ad38-143a-86fc-b17a5dc97fb6
    */
    protected static final String DEEPL_AUTH_KEY = "beadc63e-c0ec-a243-8f4f-43e60a907156:fx";
    protected static final int DEEPL_API_CHARACTER_LIMIT = 5000;
    protected static final int PACKAGE_SIZE = 50; // maximum 50


    // Method to get an map of all modules and the respective project in which they are located
    // Toggle to get only the ghs_xxx modules
    public static Map<String, String> getProjectByModuleName(boolean onlyGHS) {
        final Map<String, String> folderMap = new HashMap<>();
        try {
            Files.walk(ROOT, 2)
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        // Get the parent directory
                        final Path parent = path.getParent();
                        if (parent != null) {
                            // Add to the map: folder -> parent
                            String parentName = parent.toString();
                            String pathName = path.toString();

                            parentName = parentName.contains("\\") ?
                                    parentName.substring(parentName.indexOf("\\") + 1) :
                                    parentName;
                            pathName = pathName.contains("\\") ?
                                    pathName.substring(pathName.lastIndexOf("\\") + 1) :
                                    pathName;
                            if (pathName.contains("ghs_") || !onlyGHS) {
                                folderMap.put(parentName, pathName);
                            }

                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folderMap;
    }

    protected static void parseGhsAndCreateNewPropertyFiles(Path startDir, LocaleWithEncoding language) throws IOException {
        Files.walkFileTree(startDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("messages_en.properties")) {
                    final Path path = Paths.get(file.toFile().getPath().replace("_en", "_" + language.getLocale().getLanguage()));
                    createFileAndDirectoryIfNotExixts(path, language);
                    if (DEBUG) {
                        System.out.println("Creating new property file for: " + path);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    protected static void createFileAndDirectoryIfNotExixts(Path path, LocaleWithEncoding locale) {
        try {
            if (Files.notExists(path)) {
                // Ensure parent directories exist
                Files.createDirectories(path.getParent());
                // Create file
                Files.createFile(path);

                copyAndFlagMissingProperties(path, locale);
            }
        } catch (IOException e) {
            System.err.println("An error occurred while creating the file: " + e.getMessage());
        }
    }

    private static void copyAndFlagMissingProperties(Path path, LocaleWithEncoding locale) throws IOException {
        final String translateToFlag = " (" + locale.getLocale().getLanguage().toUpperCase() + ")";
        // Copy all english properties into the newly created file and flag them as "to be translated"
        final Path enPropertiesFilePath = Paths.get(path.toFile().getPath().replace("_" + locale.getLocale().getLanguage(), "_en"));
        if (Files.exists(enPropertiesFilePath)) {
            final List<String> lines = Files.readAllLines(enPropertiesFilePath, locale.getEncoding());
            final List<String> modifiedLines = new ArrayList<>();
            for (String line : lines) {
                line = line.contains("=") ? line : "";
                modifiedLines.add(line.isEmpty() || line.startsWith("#") ? line : line + translateToFlag);
            }
            Files.write(path, modifiedLines, locale.getEncoding());
        }
    }

}
