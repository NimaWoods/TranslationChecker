/*
 * Created on 15/09/2023, 10:28
 *
 * Copyright (c) 2023-2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;

import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import static de.topsystem.localization.tools.BundleUtil.getBuilder;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEBUG;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.LOCALIZATION_SEPARATOR_CHAR;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.ROOT;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.createFileAndDirectoryIfNotExixts;
import static java.util.ResourceBundle.getBundle;

public class PropertyImporter {

    private static final Charset ENCODING = Charset.forName("ISO-8859-2");

    public static void executeLocalizationTranslationsImport(String filePath, LocaleWithEncoding language, String project)
            throws IOException, ConfigurationException, IllegalArgumentException {
        final HashMap<String, Map<String, String>> replacements = new HashMap<>();
        final ClassLoader loader = new URLClassLoader(new URL[]{ ROOT.toFile().toURI().toURL() });

        findReplacementsWithCoordinate(readTranslation(filePath, language), language, replacements, project, loader);

        modifyBundles(language, replacements);
    }

    private static void findReplacementsWithCoordinate(Map<String, String> translations, LocaleWithEncoding sourceLocale,
            Map<String, Map<String, String>> replacements, String project, ClassLoader loader) throws IOException {

        final Set<String> values = translations.keySet();
        final Set<String> foundValues = new HashSet<>();

        for (final String keys : values) {
            final String pathName = project + '/' + keys.substring(0, keys.indexOf(".")) + "/properties/messages";
            final Path filePath = Paths.get(pathName + "_" + sourceLocale.getLocale().getLanguage() + ".properties");

            createFileAndDirectoryIfNotExixts(filePath, sourceLocale);

            String propertyName = keys.substring(keys.indexOf(".") + 1);
            final ResourceBundle rb = getBundle(pathName, sourceLocale.getLocale(), loader);

            final Map<String, String> bundleReplacements = new HashMap<>();
            boolean found = false;

            for (String key : rb.keySet()) {
                if (key.equals(propertyName)) {
                    foundValues.add(keys);
                    found = true;
                    bundleReplacements.put(key, translations.get(keys));
                }
            }
            if (!found) {
                final List<String> modifiedLines = new ArrayList<>();
                modifiedLines.add(propertyName + "=");

                Files.write(filePath, modifiedLines, sourceLocale.getEncoding());
                bundleReplacements.put(propertyName, translations.get(keys));
            }
            if (!bundleReplacements.isEmpty()) {
                replacements.computeIfAbsent(pathName, pkg -> new HashMap<>()).putAll(bundleReplacements);
            }
        }

        values.removeAll(foundValues);
        if (DEBUG) {
            System.out.println("Translations that could not be applied:");
            values.forEach(v -> System.out.println(v.replaceAll("\n", "\\n")));
        }
    }


    protected static void modifyBundles(LocaleWithEncoding targetLocale, HashMap<String, Map<String, String>> replacements) throws ConfigurationException {
        for (Entry<String, Map<String, String>> e : replacements.entrySet()) {
            modifyBundle(targetLocale, e.getKey(), e.getValue());
        }
    }

    private static void modifyBundle(LocaleWithEncoding targetLocale, String bundlePackage, Map<String, String> bundleReplacements) throws ConfigurationException {
        if (DEBUG) {
            System.out.println("Processing bundle " + bundlePackage);
        }
        final FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getBuilder(targetLocale, bundlePackage.replace("/properties/messages",
                ""), false);
        final Configuration configuration = builder.getConfiguration();
        boolean hasChanges = false;

        for (Entry<String, String> entry : bundleReplacements.entrySet()) {
            final String key = entry.getKey();
            final String newValue = entry.getValue();
            if (!newValue.equals(configuration.getProperty(key))) {
                if (DEBUG) {
                    System.out.println("Changing entry: " + key + ": " + configuration.getProperty(key) + " -> " + newValue);
                }
                configuration.setProperty(key, newValue);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            builder.save();
        }
    }


    private static Map<String, String> readTranslation(String path, LocaleWithEncoding locale) throws IllegalArgumentException, IOException {
        Map<String, String> translations = new HashMap<>();

        Files.newBufferedReader(Paths.get(path), locale.getEncoding()).lines()
                .forEach(line -> {
                    final String[] items = line.replaceAll("\\\\n", "\n").split(LOCALIZATION_SEPARATOR_CHAR);
                    if (items.length != 2) {
                        throw new IllegalArgumentException("Invalid line: " + line);
                    }
                    final String key = items[0];
                    translations.put(key, items[1]);
                });

        return translations;
    }

}
