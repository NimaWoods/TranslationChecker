/*
 * Created on 23.11.20, 13:16
 *
 * Copyright (c) 2020-2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import de.topsystem.common.resource.ResourceBundleXMLControl;
import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.core5.http.ParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.topsystem.localization.tools.BundleUtil.TranslationType.DEEPL;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.AVAILABLE_LOCALES;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.SRC;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class BundleUtil {

    public static final String PROPERTIES_MESSAGES_DE_PROPERTIES = "/properties/messages_de.properties";

    private static final ResourceBundleXMLControl RESOURCE_BUNDLE_XML_CONTROL = new ResourceBundleXMLControl();

    /**
     * Allows to choose between DeepL translation and preparation for translation by concating the known german/english
     * texts and adding a translation marker, e.g. " (ES)"
     */
    enum TranslationType {
        CONCAT_KNOWN,
        DEEPL
    }

    @Nullable
    static String getValue(String key, @Nonnull ResourceBundle bundle) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private static class BundleInfo {
        final Set<String> allKeys = new HashSet<>();
        final String path;

        BundleInfo(String path) {
            this.path = path;
        }
    }

    static final Map<Locale, Multimap<String, BundleInfo>> valueOccurences = Arrays.stream(AVAILABLE_LOCALES).collect(toMap(Function.identity(),
            v -> HashMultimap.create()));

    /**
     * @param args
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException {

        try {
            rewriteMessageBundles();

        } finally {
            DeepLUtil.closeHttpClient();
        }

    }

    private static void rewriteMessageBundles() throws MalformedURLException {

        final Path root = Paths.get(".");
        final ClassLoader loader = new URLClassLoader(new URL[]{root.toFile().toURI().toURL()});
        final Table<BundleInfo, Locale, ResourceBundle> allItems = HashBasedTable.create();

        try (Stream<Path> files = Files.walk(root)) {

            files.map(p -> p.toString().replace(File.separatorChar, '/'))
                    .filter(p -> p.endsWith(PROPERTIES_MESSAGES_DE_PROPERTIES))
                    .filter(p -> p.regionMatches(p.indexOf('/', 3), '/' + SRC, 0, SRC.length()))
                    .forEach(p -> {

                        final String bundlePackage = p.substring(3, p.length() - PROPERTIES_MESSAGES_DE_PROPERTIES.length());
                        final BundleInfo bundleInfo = new BundleInfo(bundlePackage);

                        for (Locale locale : AVAILABLE_LOCALES) {
                            final ResourceBundle rb = getBundle(bundlePackage + "/properties/messages", locale, loader, RESOURCE_BUNDLE_XML_CONTROL);
                            if (locale.equals(rb.getLocale())) {
                                allItems.put(bundleInfo, locale, rb);
                                bundleInfo.allKeys.addAll(rb.keySet());
                                final String localizationRemark = "(" + locale.getLanguage().toUpperCase() + ")";
                                for (String key : rb.keySet()) {
                                    final String value = rb.getString(key);
                                    if (value != null && value.endsWith(localizationRemark)) {
                                        valueOccurences.get(locale).put(value, bundleInfo);
                                    }
                                }
                            }
                        }
                    });

            //printDoubles();

            //process(allItems);

            exportReview(allItems, "C:/Users/a.behr/Desktop");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void exportReview(Table<BundleInfo, Locale, ResourceBundle> allItems, String baseDir) {

        AtomicInteger line = new AtomicInteger();
        final Path base = Paths.get(baseDir);
        final Set<String> seenGerman = new HashSet<>();

        try (BufferedWriter outSpanish = Files.newBufferedWriter(base.resolve("spanish.txt"), CREATE, TRUNCATE_EXISTING, WRITE);
             BufferedWriter outGerman = Files.newBufferedWriter(base.resolve("german.txt"), CREATE, TRUNCATE_EXISTING, WRITE)) {

            allItems.rowMap().entrySet().forEach(p -> {

                final Map<Locale, ResourceBundle> bundles = p.getValue();
                final ResourceBundle spanish = bundles.get(AVAILABLE_LOCALES[4]);
                BundleInfo bundleInfo = p.getKey();
                if (spanish == null) {
                    System.err.println("No spanish for " + bundleInfo.path);
                    return;
                }

                final ResourceBundle german = bundles.get(Locale.GERMAN);
                bundleInfo.allKeys.forEach(k -> {
                    try {
                        String value = spanish.getString(k);
                        if (value.indexOf(" (T)") >= 0) {
                            try {
                                String germanValue = german.getString(k);
                                if (seenGerman.add(germanValue)) {
                                    writeValue(outSpanish, value.substring(0, value.length() - 4));
                                    writeValue(outGerman, germanValue);
                                    line.getAndIncrement();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (MissingResourceException e) {
                        System.err.println(e.getMessage());
                    }
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Written %d lines." , line.get());
    }

    private static void writeValue(BufferedWriter outSpanish, String what) throws IOException {
        outSpanish.write(what.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n"));
        outSpanish.newLine();
    }

    private static void process(Table<BundleInfo, Locale, ResourceBundle> allItems) {
        allItems.rowMap().entrySet().forEach(p -> {

            final BundleInfo bundleInfo = p.getKey();
            final int numTotalKeys = bundleInfo.allKeys.size();
            System.out.print(bundleInfo.path + ": " + numTotalKeys + " ");

            final Map<Locale, ResourceBundle> bundles = p.getValue();
            for (Locale locale : AVAILABLE_LOCALES) {
                final ResourceBundle resourceBundle = bundles.get(locale);
                System.out.print(resourceBundle != null ? resourceBundle.keySet().size() + " " : "- ");
            }

            // check english and german have same keys
            ResourceBundle english = bundles.get(Locale.ENGLISH);
            ResourceBundle german = bundles.get(Locale.GERMAN);

            // printMissing(english, german);
            rewriteBundles(bundleInfo, bundles, english, german);
        });
    }

    private static void printDoubles() {
        valueOccurences.entrySet().forEach(e -> {
            AtomicInteger potentialForSave = new AtomicInteger();
            System.out.println(e.getKey().getLanguage());
            final Multimap<String, BundleInfo> value = e.getValue();
            Map<Integer, List<String>> occs = value.keySet().stream().collect(groupingBy(k -> value.get(k).size()));
            occs.entrySet().stream().filter(en -> en.getKey() > 1).forEach(n -> {
                potentialForSave.getAndAdd((n.getKey() - 1) * n.getValue().size());
                System.out.println(n);
            });
            System.out.println("potentialForSave: " + potentialForSave);
        });
    }

    private static void printMissing(ResourceBundle english, ResourceBundle german) {
        final Set<String> englishKeys = english.keySet();
        final Set<String> germanKeys = german.keySet();
        if (!englishKeys.equals(germanKeys)) {
            printMissing(englishKeys, germanKeys, "de");
            printMissing(germanKeys, englishKeys, "en");
        }
    }

    private static void rewriteBundles(BundleInfo bundleInfo, Map<Locale, ResourceBundle> bundles, ResourceBundle english, ResourceBundle german) {
        IntStream.of(LocaleWithEncoding.values().length).forEach(i -> {
            final LocaleWithEncoding locale = LocaleWithEncoding.values()[i];
            try {
                rewriteBundle(locale, bundles.get(locale.getLocale()), bundleInfo.path, english, german);
            } catch (ConfigurationException | IOException | ParseException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @param english
     * @param german
     * @param locale
     * @param bundle
     * @param path
     * @throws IOException
     * @throws ConfigurationException
     */
    private static void rewriteBundle(LocaleWithEncoding locale, ResourceBundle bundle, String path, ResourceBundle english,
                                      ResourceBundle german) throws IOException, ConfigurationException, ParseException {

        final String language = locale.getLocale().getLanguage();
        final boolean useUTF = language.equals("ru");

        final Path englishPath = Paths.get(getPropertyFileName(LocaleWithEncoding.ENGLISH, path));
        final Path targetPath = Paths.get(getPropertyFileName(locale, path));

        if (useUTF) {
            // convert english file to UTF
            final String content = new String(Files.readAllBytes(englishPath), StandardCharsets.ISO_8859_1);
            Files.write(targetPath, content.getBytes(), TRUNCATE_EXISTING, CREATE);
        } else {
            Files.copy(englishPath, targetPath, REPLACE_EXISTING);
        }

        final FileBasedConfigurationBuilder<FileBasedConfiguration> builder = getBuilder(locale, path, useUTF);
        final Configuration configuration = builder.getConfiguration();

        for (String key : german.keySet()) {

            String value;
            found:
            {
                final String localizationRemark = "(" + language.toUpperCase() + ")";
                if (bundle != null) {
                    value = getValue(key, bundle);
                    if (StringUtils.isNotEmpty(value) && !value.endsWith(localizationRemark)) {
                        break found;
                    }
                }

                value = translate(key, localizationRemark, english, german, locale.getLocale().getLanguage().toUpperCase(), DEEPL);
            }

            configuration.setProperty(key, value);
        }
        builder.save();
    }

    /**
     * The main entry point for localization, used to support the different {@link TranslationType}s
     *
     * @param key
     * @param localizationRemark
     * @param english
     * @param german
     * @param target
     * @param how
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @Nonnull
    private static String translate(String key, String localizationRemark, ResourceBundle english, ResourceBundle german,
                                    String target, TranslationType how) throws IOException, ParseException {
        switch (how) {
            case DEEPL:
                return DeepLUtil.translate(key, target, english, german);

            case CONCAT_KNOWN:
            default:
                final String englishGerman =
                        Stream.of(english, german).map(b -> getValue(key, b)).filter(StringUtils::isNotEmpty).distinct().collect(joining("/"));
                return englishGerman + (englishGerman.length() > 0 ? " " + localizationRemark : "");
        }
    }

    /**
     * @param locale
     * @param path
     * @param useUTF
     * @return
     */
    protected static FileBasedConfigurationBuilder<FileBasedConfiguration> getBuilder(LocaleWithEncoding locale, String path, boolean useUTF) {
        Parameters params = new Parameters();
        final String propertyFileName = getPropertyFileName(locale, path);
        final PropertiesBuilderParameters propertiesBuilderParameters = params.properties()
                .setIOFactory(new PropertiesConfiguration.JupIOFactory(false))
                .setFileName(propertyFileName);

        if (useUTF) {
            propertiesBuilderParameters.setEncoding("UTF-8");
        } else {
            propertiesBuilderParameters.setEncoding(locale.getEncoding().toString());
        }

        return new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                .configure(propertiesBuilderParameters);
    }

    /**
     * @param locale
     * @param path
     * @return
     */
    @Nonnull
    private static String getPropertyFileName(@Nonnull LocaleWithEncoding locale, String path) {
        return "./" + path + "/properties/messages_" + locale.getLocale().getLanguage() + /*(locale == Locale.ENGLISH ? "" : "_out") + */ ".properties";
    }

    /**
     * @param englishKeys
     * @param germanKeys
     * @param s
     */
    private static void printMissing(Set<String> englishKeys, Set<String> germanKeys, String s) {
        final Sets.SetView<String> difference = Sets.difference(englishKeys, germanKeys);
        if (difference.size() > 0) {
            System.out.print("missing in " + s + ": " + difference + " ");
        }
    }

}
