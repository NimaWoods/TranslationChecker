package com.gui.contsants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public enum Language {
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

	Language(Locale locale, Charset encoding) {
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
