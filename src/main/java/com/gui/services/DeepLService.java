package com.gui.services;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.gui.manager.SettingsManager;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class DeepLService {

	private static final Logger logger = Logger.getLogger(DeepLService.class.getName());
	private static CloseableHttpClient HTTPCLIENT;
	private static boolean profilesLoaded;

	private static CloseableHttpClient getHttpClient() {
		if (HTTPCLIENT == null) {
			HTTPCLIENT = HttpClients.createDefault();
		}
		return HTTPCLIENT;
	}

	public static void closeHttpClient() {
		if (HTTPCLIENT != null) {
			try {
				HTTPCLIENT.close();
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error closing HttpClient", e);
			}
		}
	}

	public DeepLService () {
		getHttpClient();
	}

	public static void main(String[] args) {
		try {
			String result = translateString("Hello World", "EN", "DE");
			System.out.println("Translated text: " + result);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Translates the given Properties object using DeepL API.
	 *
	 * @param properties      The Properties object containing key-value pairs to be translated.
	 * @param sourceLanguage  The source language code (e.g., "EN", "DE").
	 * @param targetLanguage  The target language code (e.g., "EN", "DE").
	 * @return A new Properties object containing the translated key-value pairs.
	 */
	public static Properties translateProperties(Properties properties, String sourceLanguage, String targetLanguage) {
		Properties translatedProperties = new Properties();

		List<String> keys = properties.stringPropertyNames().stream().collect(toList());
		List<String> values = keys.stream().map(properties::getProperty).collect(toList());

		List<String> translatedValues = translateTextList(values, sourceLanguage, targetLanguage);

		for (int i = 0; i < keys.size(); i++) {
			translatedProperties.setProperty(keys.get(i), translatedValues.get(i));
		}

		return translatedProperties;
	}

	/**
	 * Translates a list of strings using the DeepL API.
	 *
	 * @param textList        The list of strings to be translated.
	 * @param sourceLanguage  The source language code.
	 * @param targetLanguage  The target language code.
	 * @return A list of translated strings.
	 */
	public static List<String> translateTextList(List<String> textList, String sourceLanguage, String targetLanguage) {
		List<String> translatedTextList = new ArrayList<>();

		for (String text : textList) {
			try {
				String translatedText = translateString(text, sourceLanguage, targetLanguage);
				translatedTextList.add(translatedText);
			} catch (IOException | ParseException e) {
				logger.log(Level.SEVERE, "Error translating text", e);
				translatedTextList.add(text); // Add original text if translation fails
			}
		}

		return translatedTextList;
	}

	/**
	 * Translates a single string using the DeepL API.
	 *
	 * @param text            The text to be translated.
	 * @param sourceLanguage  The source language code.
	 * @param targetLanguage  The target language code.
	 * @return The translated text.
	 */
	private static String translateString(String text, String sourceLanguage, String targetLanguage) throws IOException, ParseException {

		if (sourceLanguage.equals("auto")) {
				sourceLanguage = detectLanguage(text);
		}

		SettingsManager settingsManager = new SettingsManager();
		String authKey = settingsManager.getSettings().getProperty("api.key");

		final HttpPost httppost = new HttpPost("https://api-free.deepl.com/v2/translate?auth_key=" + authKey);

		// Hinzufügen der Header
		httppost.addHeader(HttpHeaders.HOST, "api-free.deepl.com");
		httppost.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:66.0) Gecko/20100101 Firefox/66.0");
		httppost.addHeader(HttpHeaders.ACCEPT, "*/*");
		httppost.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

		// Parameter der Anfrage
		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("auth_key", authKey));
		params.add(new BasicNameValuePair("text", text));
		params.add(new BasicNameValuePair("source_lang", sourceLanguage));
		params.add(new BasicNameValuePair("target_lang", targetLanguage));

		// Setzen der Parameter in den Http-Post-Request
		httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

		// Ausführen der Anfrage und erhalten der Antwort
		HttpEntity entity = getHttpClient().execute(httppost).getEntity();

		if (entity != null) {
			// Konvertieren der Antwort in einen JSON-String
			String responseString = EntityUtils.toString(entity);
			JSONObject response = new JSONObject(responseString);

			// Rückgabe des übersetzten Textes aus der JSON-Antwort
			return response.getJSONArray("translations").getJSONObject(0).getString("text");
		}

		// Falls keine gültige Antwort vorliegt, gib den Originaltext zurück
		return text;
	}

	/**
	 * Detects the language of the given text using Apache Tika.
	 *
	 * @param text The text whose language is to be detected.
	 * @return The detected language code (e.g., "en" for English, "de" for German).
	 */
	public static String detectLanguage(String text) {
		// Profile nur laden, wenn sie noch nicht geladen wurden
		if (!profilesLoaded) {
			try {
				DetectorFactory.loadProfile("src/main/java/com/gui/profiles");
				profilesLoaded = true;
			} catch (LangDetectException e) {
				logger.log(Level.SEVERE, "Error loading language profiles", e);
				return null;
			}
		}

		try {
			Detector detector = DetectorFactory.create();
			detector.append(text);
			return detector.detect();
		} catch (LangDetectException e) {
			logger.log(Level.SEVERE, "Error detecting language", e);
			return null;
		}
	}
}
