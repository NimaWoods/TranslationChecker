/*
 * Created on 23/07/2024, 14:02
 *
 * Copyright (c) 2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.Usage.Detail;
import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.COMMON;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEBUG;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEEPL_API_CHARACTER_LIMIT;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEEPL_AUTH_KEY;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DE_TOPSYSTEM;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.FRAMEWORK;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.GHS;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.PACKAGE_SIZE;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.ROOT;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.SRC;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.getProjectByModuleName;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.parseGhsAndCreateNewPropertyFiles;
import static de.topsystem.localization.tools.PropertyImporter.modifyBundles;
import static java.util.ResourceBundle.getBundle;

public class FindAndTranslateFlaggedProperties {

    // CharacterCount and packageCount for debugging to take into account the character limit of the free Deepl API
    private static int totalCharacterCount = 0;
    private static int totalPackageCount = 0;

    private static final Translator translator = new Translator(DEEPL_AUTH_KEY);

    public static long deeplApiLimit() throws DeepLException, InterruptedException {
        final Detail deeplApiCharacterDetail = translator.getUsage().getCharacter();
        return Objects.requireNonNull(deeplApiCharacterDetail).getLimit() - deeplApiCharacterDetail.getCount();
    }

    public static void executeFindAndTranslate(String project, LocaleWithEncoding language) throws Exception {
        // Create missing properties files for the product
        if (project.equals("product")) {
            parseGhsAndCreateNewPropertyFiles(Paths.get(project + "/ghs"), language);
            parseGhsAndCreateNewPropertyFiles(Paths.get(project + "/framework"), language);
            parseGhsAndCreateNewPropertyFiles(Paths.get(project + "/common"), language);
        }
        HashMap<String, Map<String, String>> newPropertyTranslations = findNotTranslatedProperties(project, language);
        if (DEBUG) {
            for (Map.Entry<String, Map<String, String>> entry : newPropertyTranslations.entrySet()) {
                System.out.println("File: " + entry.getKey());
                for (final String key : entry.getValue().keySet()) {
                    System.out.println(key + " --> " + entry.getValue().get(key));
                }
            }
        }
        // Write new translations in the .properties files
        newPropertyTranslations.forEach((coord, properties) -> {
            try {
                final HashMap<String, Map<String, String>> map = new HashMap<>();
                map.put(coord, properties);
                modifyBundles(language, translate(map, language));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        if (DEBUG) {
            System.out.println("_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-");
        }
    }

    public static HashMap<String, Map<String, String>> findNotTranslatedProperties(String project, LocaleWithEncoding locale) {
        if (project.equals("product")) {
            final HashMap<String, Map<String, String>> map = new HashMap<>();
            map.putAll(parsePropertiesFiles(project + '/' + GHS + SRC + DE_TOPSYSTEM, locale));
            map.putAll(parsePropertiesFiles(project + '/' + FRAMEWORK + SRC + DE_TOPSYSTEM, locale));
            map.putAll(parsePropertiesFiles(project + '/' + COMMON + SRC + DE_TOPSYSTEM, locale));
            return map;
        }

        return parsePropertiesFiles(project + '/' + getProjectByModuleName(true).get(project) + '/' + SRC + DE_TOPSYSTEM, locale);
    }

    public static HashMap<String, Map<String, String>> parsePropertiesFiles(String startDirectory, LocaleWithEncoding locale) {
        final HashMap<String, Map<String, String>> fileContentMap = new HashMap<>();
        final Path startPath = Paths.get(startDirectory);

        try {
            // Start from the project root and read all .properties files and their lines
            Files.walk(startPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("messages_" + locale.getLocale().getLanguage() + "\\.properties"))
                    .forEach(path -> {
                        HashMap<String, String> lines = readLinesFromFile(path, locale);
                        fileContentMap.put(path.toString().replace("\\properties\\messages_" + locale.getLocale().getLanguage() + ".properties", ""), lines);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContentMap;
    }

    private static HashMap<String, String> readLinesFromFile(Path filePath, LocaleWithEncoding locale) {
        final String translationFlag = "(" + locale.getLocale().getLanguage().toUpperCase() + ")";
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(filePath, locale.getEncoding());
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, String> properties = new HashMap<>();
        for (final String line : lines) {
            if (line.contains("=") && !line.startsWith("#") && line.contains(translationFlag)) {
                // Determine the key and its translation without the translationFlag
                final int equalsIndex = line.indexOf("=");
                properties.put(line.substring(0, equalsIndex).trim(), line.substring(equalsIndex + 1).replace(translationFlag, "").trim());
            } else if (line.contains("=") && !line.startsWith("#") && line.contains("?") && !line.endsWith("?")) { // If the translation has an encoding issue
                // Determine the key and its translation
                final String key = line.substring(0, line.indexOf("=")).trim();
                try {
                    final ClassLoader loader = new URLClassLoader(new URL[]{ ROOT.toFile().toURI().toURL() });
                    final ResourceBundle rb = getBundle(String.valueOf(filePath).replace("_hu.properties", ""), Locale.ENGLISH, loader);
                    properties.put(key, rb.getString(key).trim());
                } catch (Exception e) {
                    System.out.println("No resource bundle found for " + filePath.toString().replace("hu", "en"));
                }
            }
        }

        return properties;
    }

    public static HashMap<String, Map<String, String>> translate(HashMap<String, Map<String, String>> translationMap, LocaleWithEncoding targetLanguage) throws Exception {

        final HashMap<String, Map<String, String>> translatedMap = translationMap;
        final List<String> stringsToTranslate = new ArrayList<>();
        final List<String> translationPackage = new ArrayList<>();
        final List<TextResult> result = new ArrayList<>();

        // CharacterCount and packageCount for debugging to take into account the character limit of the free Deepl API
        int characterCount = 0;
        int packageCount = 0;

        // Extract the text to be translated into a list
        for (final String propertyFile : translationMap.keySet()) {
            for (final String property : translationMap.get(propertyFile).keySet()) {
                stringsToTranslate.add(translationMap.get(propertyFile).get(property));
            }
        }
        if (DEBUG) {
            System.out.println("----------------------------------------------------------------------------------------------------");
        }
        int i = 0;
        for (final String s : stringsToTranslate) {
            // pack the StringsToTranslate into packages of the size [PACKAGE_SIZE]
            if (i < PACKAGE_SIZE || totalCharacterCount + s.toCharArray().length > DEEPL_API_CHARACTER_LIMIT) {
                translationPackage.add(s.equals("") ? " " : s);
                totalCharacterCount = totalCharacterCount + s.toCharArray().length;
                characterCount = characterCount + s.toCharArray().length;
                i++;
            }
            // Translate when the package size is reached
            if (i == PACKAGE_SIZE) {
                if (DEBUG) {
                    totalPackageCount++;
                    packageCount++;
                    System.out.println("Translating package " + packageCount);
                    System.out.println("Characters translated in this File: " + characterCount);
                }
                result.addAll(translator.translateText(translationPackage, Locale.ENGLISH.getLanguage(), targetLanguage.getLocale().getLanguage()));
                translationPackage.clear();
                i = 0;
            }
        }
        // In the event that the maximum [PACKAGE_SIZE] was not adhered to for the remaining translations, translate here
        if (!translationPackage.isEmpty()) {
            if (DEBUG) {
                totalPackageCount++;
                packageCount++;
                System.out.println("Translating package " + packageCount);
                System.out.println("Characters translated in this File: " + characterCount);
            }
            result.addAll(translator.translateText(translationPackage, Locale.ENGLISH.getLanguage(), targetLanguage.getLocale().getLanguage()));
            translationPackage.clear();
        }

        // Insert the translated properties back into a map so that they can be written to the .properties files
        if (!result.isEmpty()) {
            for (final String propertyFile : translatedMap.keySet()) {
                for (final String property : translatedMap.get(propertyFile).keySet()) {
                    translatedMap.get(propertyFile).replace(property, result.get(0).getText());
                    result.remove(result.get(0));
                }
            }
        }

        if (DEBUG) {
            System.out.println("Total packages: " + totalPackageCount);
            System.out.println("Total characters used: " + totalCharacterCount);
        }
        return translatedMap;
    }

}