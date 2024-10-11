package com.gui.contsants;

public enum Constants {
	DEEPL_AUTH_KEY("beadc63e-c0ec-a243-8f4f-43e60a907156:fx"),
	BASEDATA_SEPARATOR_CHAR(",");

	private final String value;

	Constants(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}


