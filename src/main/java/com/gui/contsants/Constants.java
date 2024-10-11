package com.gui.contsants;

public enum Constants {
	DEEPL_AUTH_KEY("your_deepl_auth_key"),
	BASEDATA_SEPARATOR_CHAR(",");

	private final String value;

	Constants(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}


