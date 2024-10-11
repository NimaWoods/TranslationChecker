package com.gui.services;

import com.gui.contsants.Constants;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class DeepLService {

	private static final Logger logger = Logger.getLogger(DeepLService.class.getName());
	private static CloseableHttpClient HTTPCLIENT;

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
		final HttpPost httppost = new HttpPost("https://api-free.deepl.com/v2/translate");

		httppost.addHeader("Authorization", "DeepL-Auth-Key " + Constants.DEEPL_AUTH_KEY.getValue());

		final List<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("text", text));
		params.add(new BasicNameValuePair("source_lang", sourceLanguage));
		params.add(new BasicNameValuePair("target_lang", targetLanguage));

		httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

		HttpEntity entity = getHttpClient().execute(httppost).getEntity();

		if (entity != null) {
			JSONObject response = new JSONObject(EntityUtils.toString(entity));
			return ((JSONObject) response.getJSONArray("translations").get(0)).getString("text");
		}

		return text;
	}

	public static void main(String[] args) {
		Properties properties = new Properties();
		properties.setProperty("welcome.message", "Welcome to our website");
		properties.setProperty("goodbye.message", "Thank you for visiting");

		Properties translatedProperties = translateProperties(properties, "EN", "DE");

		for (Map.Entry<Object, Object> entry : translatedProperties.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}

		closeHttpClient();
	}
}
