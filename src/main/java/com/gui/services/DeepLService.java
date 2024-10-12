package com.gui.services;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.gui.contsants.LanguagesConstant;
import com.gui.manager.SettingsManager;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

	public DeepLService() {
		getHttpClient();
	}

	public static void main(String[] args) {
		try {
			String result = translateString("Hello World (de)", "EN", "DE");
			System.out.println("Translated text: " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Translates the given Properties object using DeepL API.
	 *
	 * @param properties     The Properties object containing key-value pairs to be translated.
	 * @param sourceLanguage The source language code (e.g., "EN", "DE").
	 * @param targetLanguage The target language code (e.g., "EN", "DE").
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
	 * @param textList       The list of strings to be translated.
	 * @param sourceLanguage The source language code.
	 * @param targetLanguage The target language code.
	 * @return A list of translated strings.
	 */
	public static List<String> translateTextList(List<String> textList, String sourceLanguage, String targetLanguage) {
		List<String> translatedTextList = new ArrayList<>();

		for (String text : textList) {
			try {
				String translatedText = translateString(text, sourceLanguage, targetLanguage);
				translatedTextList.add(translatedText);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error translating text", e);
				translatedTextList.add(text); // Add original text if translation fails
			}
		}

		return translatedTextList;
	}

	/**
	 * Translates a single string using the DeepL API.
	 *
	 * @param text           The text to be translated.
	 * @param sourceLanguage The source language code.
	 * @param targetLanguage The target language code.
	 * @return The translated text.
	 */
	public static String translateString(String text, String sourceLanguage, String targetLanguage) throws IOException, ParseException {

		if (sourceLanguage.equals("auto")) {
			sourceLanguage = detectLanguage(text);

			if(Objects.equals(sourceLanguage, targetLanguage)) {
				throw new IllegalArgumentException("Detected Source language (" + sourceLanguage + ") and target language (" + targetLanguage + ") are the same");
			}
		}

		// Get your API key from settings
		SettingsManager settingsManager = new SettingsManager();
		String authKey = settingsManager.getSettings().getProperty("api.key");

		// Prepare HTTP POST request to DeepL API
		final HttpPost httppost = new HttpPost("https://api-free.deepl.com/v2/translate");
		httppost.addHeader("Authorization", "DeepL-Auth-Key " + authKey);
		httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

		// Prepare request parameters
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("text", text));
		params.add(new BasicNameValuePair("source_lang", sourceLanguage));
		params.add(new BasicNameValuePair("target_lang", targetLanguage));

		// Set entity with the form data
		httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

		// Execute the request and get the response
		HttpEntity entity = getHttpClient().execute(httppost).getEntity();

		if (entity != null) {
			String responseString = EntityUtils.toString(entity);
			JSONObject jsonResponse = new JSONObject(responseString);

			return removeMarker(jsonResponse.getJSONArray("translations").getJSONObject(0).getString("text"));
		}

		return text;
	}

	/**
	 * Detects the language of the given text using Apache Tika.
	 *
	 * @param text The text whose language is to be detected.
	 * @return The detected language code (e.g., "en" for English, "de" for German).
	 */
	public static String detectLanguage(String text) {
		// Überprüfen, ob die Sprachprofile bereits geladen wurden
		if (!profilesLoaded) {
			try {
				DetectorFactory.loadProfile("src/main/java/com/gui/profiles");
				logger.info("Successfully loaded profiles. Loaded " + DetectorFactory.getLangList().size() + " profiles.");
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

	private static String removeMarker(String text) {

		String languageCodes = Arrays.stream(LanguagesConstant.values())
				.map(lang -> lang.getLocale().getLanguage())
				.collect(Collectors.joining("|"));

		// Regular expression zum Entfernen des Sprachmarkers am Ende des Textes
		return text.replaceAll("\\s*\\(\\b(" + languageCodes + ")\\b\\)$", "").trim();
	}



}