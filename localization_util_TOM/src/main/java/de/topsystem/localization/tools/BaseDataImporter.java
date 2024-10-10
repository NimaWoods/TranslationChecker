/*
 * Created on 11.08.21, 15:07
 *
 * Copyright (c) 2021-2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;

import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_FILE_NAMES;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_LOCALIZATION_PATH;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_PRODUCT_FILE_ENDING;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_PROJECT_FILE_ENDING;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_SEPARATOR_CHAR;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEBUG;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.GHS;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.getProjectByModuleName;

/**
 * Apply translations from a wordlist into the given set of localized-*.csv
 */
public class BaseDataImporter {

    public static void executeBasedataTranslationsImport(Path translations, LocaleWithEncoding targetLanguage, String project, boolean projectAndProduct) throws IOException {
        executeForProject(translations, targetLanguage, project);
        if (projectAndProduct && !project.equals("product")) {
            executeForProject(translations, targetLanguage, "product");
        }
    }

    private static void executeForProject(Path translations, LocaleWithEncoding targetLanguage, String project) {
        int numberOfTranslationsApplied;
        int numberOfTranslationsTotal;
        final Map<String, String> ghs_projectNames = getProjectByModuleName(true);
        final Map<String, String> translationMap;
        final Set<String> allTranslations;

        try (Stream<String> lines = Files.lines(translations, targetLanguage.getEncoding())) {
            translationMap = lines.collect(Collectors.toMap(l -> l.substring(0, l.indexOf(BASEDATA_SEPARATOR_CHAR)), l -> l.substring(l.indexOf(BASEDATA_SEPARATOR_CHAR) + 1)));
            numberOfTranslationsTotal = translationMap.size();
            allTranslations = translationMap.keySet();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (DEBUG) {
            numberOfTranslationsApplied = 0;
        }
        for (String fileName : BASEDATA_FILE_NAMES) {
            try {
                final Path filePath =
                        Paths.get(project + '/' + (project.equals("product") ? GHS : ghs_projectNames.get(project))
                                + '/' + BASEDATA_LOCALIZATION_PATH + fileName +
                                (project.equals("product") ? BASEDATA_PRODUCT_FILE_ENDING : BASEDATA_PROJECT_FILE_ENDING));
                if (Files.exists(filePath)) {
                    final List<String> lines = Files.readAllLines(filePath);

                    Set<String> translatedKeys = new HashSet<>();
                    Set<String> processedLines = new HashSet<>();
                    List<String> linesWithTranslations = new ArrayList<>();

                    // Loop backwards through all lines so that the new translation is added at the end of their respective block
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        final String l = lines.get(i);

                        // Check for empty line and add it directly to the list
                        if (l.trim().isEmpty()) {
                            linesWithTranslations.add(0, l);
                            continue;
                        }

                        final int index = l.indexOf(',');

                        if (index >= 0) {
                            final String language = l.substring(index + 1, index + 3);
                            final String key = l.substring(0, index);

                            if (targetLanguage.getLocale().getLanguage().equals(language) && translatedKeys.contains(key)) {
                                continue;
                            }

                            if (!processedLines.contains(l)) {
                                linesWithTranslations.add(0, l);
                                processedLines.add(l);
                            }

                            if (!translatedKeys.contains(key)) {
                                final String translation = translationMap.get(key);
                                if (translation != null) {
                                    translatedKeys.add(key);
                                    final String translationLine = key + "," + targetLanguage.getLocale().getLanguage() + "," + translation;
                                    if (!processedLines.contains(translationLine)) {
                                        linesWithTranslations.add(0, translationLine);
                                        processedLines.add(translationLine);
                                    }
                                }
                            }
                        } else {
                            if (!processedLines.contains(l)) {
                                linesWithTranslations.add(0, l);
                                processedLines.add(l);
                            }
                        }
                    }

                    if (!linesWithTranslations.isEmpty()) {
                        Files.write(filePath, linesWithTranslations);
                    }

                    if (DEBUG) {
                        numberOfTranslationsApplied += translatedKeys.size();
                        allTranslations.removeAll(translatedKeys);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (DEBUG) {
            System.out.println("Basedata imported: " + numberOfTranslationsApplied + '/' + numberOfTranslationsTotal);
            if (numberOfTranslationsApplied != numberOfTranslationsTotal) {
                System.out.println("Translations that could not be applied:");
                allTranslations.forEach(v -> System.out.println(v.replaceAll("\n", "\\n")));
            }
        }
    }

}
