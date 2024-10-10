/*
 * Created on 12.08.21, 12:24
 *
 * Copyright (c) 2021-2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization.tools;

import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static de.topsystem.localization.tools.BundleUtil.getValue;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.BASEDATA_SEPARATOR_CHAR;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.DEEPL_AUTH_KEY;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Used by {@link BundleUtil} to support localizing ressource-bundles. Can be used itself to translate word lists using
 * the {@link #translateWordlist(String, String, String)} function
 */
public class DeepLUtil {

    static CloseableHttpClient HTTPCLIENT;

    public static void main(String[] args) throws MalformedURLException {

        final String path = "C:/Users/a.behr/Desktop/actions.txt";
        final LocaleWithEncoding sourceLocale = LocaleWithEncoding.GERMAN;
        final LocaleWithEncoding targetLocale = LocaleWithEncoding.ENGLISH;

        executeDeepLTranslation(path, sourceLocale, targetLocale);
    }


    public static void executeDeepLTranslation(String filePath, LocaleWithEncoding sourceLanguage, LocaleWithEncoding targetLanguge) {
        try {
            translateWordlist(filePath, sourceLanguage.getLocale().getLanguage().toUpperCase(), targetLanguge.getLocale().getLanguage().toUpperCase());
        } finally {
            closeHttpClient();
        }
    }

    static void closeHttpClient() {
        if (HTTPCLIENT != null) {
            try {
                HTTPCLIENT.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static CloseableHttpClient getHttpClient() {
        if (HTTPCLIENT == null) {
            HTTPCLIENT = HttpClients.createDefault();
        }
        return HTTPCLIENT;
    }

    /**
     * Takes a file (e.g. "items.txt") consisting of lines of text to be translated and outputs a "items_out.txt" next to
     * it, where each line consists of the line of the original file, with an '§' and the translation appended to each line
     * <p>
     * items.txt, "en", "es":
     * Bear
     * Giraffe
     * <p>
     * items_out.txt:
     * Bear§Oso
     * Giraffe§Jirafa
     * <p>
     * The {@link BaseDataImporter} class takes the outgoing file and distributes the result into according .csv files
     *
     * @param fileName
     * @param source
     * @param target
     */
    private static void translateWordlist(String fileName, String source, String target) {
        Path in = Paths.get(fileName);
        Path out = Paths.get(FilenameUtils.removeExtension(fileName) + "_out." + FilenameUtils.getExtension(fileName));

        try (Stream<String> lines = Files.lines(in);
             final BufferedWriter writer = Files.newBufferedWriter(out, CREATE, TRUNCATE_EXISTING, WRITE)) {

            lines.filter(l -> l.indexOf(BASEDATA_SEPARATOR_CHAR) < 0).forEach(l -> {
                try {
                    String translated = translateString(l, source, target);
                    writer.write(l + BASEDATA_SEPARATOR_CHAR + translated);
                    writer.newLine();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param key
     * @param targetLanguage
     * @param english
     * @param german
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @Nonnull
    static String translate(String key, String targetLanguage, ResourceBundle english, ResourceBundle german) throws IOException, ParseException {
        String text = getValue(key, english);
        String langId = "EN";
        if (StringUtils.isEmpty(text)) {
            text = getValue(key, german);
            if (StringUtils.isEmpty(text)) {
                return "";
            }
            langId = "DE";
        }

        return translateString(text, langId, targetLanguage) + " (T)";
    }

    @Nonnull
    private static String translateString(String text, String sourceLanguage, String targetLanguage) throws IOException, ParseException {
        final HttpPost httppost = new HttpPost("https://api-free.deepl.com/v2/translate?auth_key=" + DEEPL_AUTH_KEY);

        httppost.addHeader(HttpHeaders.HOST, "api-free.deepl.com");
        httppost.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:66.0) Gecko/20100101 Firefox/66.0");
        httppost.addHeader(HttpHeaders.ACCEPT, "*/*");
        httppost.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        // Request parameters and other properties.
        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("auth_key", DEEPL_AUTH_KEY));
        params.add(new BasicNameValuePair("text", text));
        params.add(new BasicNameValuePair("source_lang", sourceLanguage));
        params.add(new BasicNameValuePair("target_lang", targetLanguage));

        httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

        // Execute and get the response.
        HttpEntity entity = getHttpClient().execute(httppost).getEntity();

        if (entity != null) {
            JSONObject response = new JSONObject(EntityUtils.toString(entity));
            return ((JSONObject) response.getJSONArray("translations").get(0)).getString("text");
        }

        return "";
    }
}
