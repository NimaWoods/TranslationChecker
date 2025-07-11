package com.gui.contsants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enum representing default supported languages
 * This class only provides default language definitions and does not depend on any other classes
 */
public enum LanguagesConstant {
	// Default languages
	GERMAN(Locale.GERMAN, StandardCharsets.ISO_8859_1),
	ENGLISH(Locale.ENGLISH, StandardCharsets.ISO_8859_1),
	FRENCH(Locale.FRENCH, StandardCharsets.ISO_8859_1),
	ITALIAN(Locale.ITALIAN, StandardCharsets.ISO_8859_1),
	RUSSIAN(new Locale("ru"), StandardCharsets.UTF_8),
	ROMANIAN(new Locale("ro"), Charset.forName("ISO-8859-2")),
	DUTCH(new Locale("nl"), StandardCharsets.ISO_8859_1),
	SPANISH(new Locale("es"), StandardCharsets.ISO_8859_1),
	HUNGARIAN(new Locale("hu"), Charset.forName("ISO-8859-2"));

	private final Locale locale;
	private final Charset encoding;

	LanguagesConstant(Locale locale, Charset encoding) {
		this.locale = locale;
		this.encoding = encoding;
	}

	/**
	 * Gets the locale for this language
	 * 
	 * @return The locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Gets the encoding for this language
	 * 
	 * @return The charset encoding
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * Gets the locale and encoding as a map
	 * 
	 * @return Map with language code as key and encoding name as value
	 */
	public Map<String, String> getLocaleWithEncoding() {
		Map<String, String> result = new HashMap<>();
		result.put(locale.getLanguage(), encoding.name());
		return result;
	}
	
	/**
	 * Gets the language code
	 * 
	 * @return The language code
	 */
	public String getLanguageCode() {
		return locale.getLanguage();
	}
	
	/**
	 * Gets the display name of the language
	 * 
	 * @return The display name
	 */
	public String getDisplayName() {
		return locale.getDisplayLanguage(Locale.ENGLISH);
	}
}
