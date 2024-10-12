package com.gui.contsants;

import java.util.Properties;

public enum SettingsConstant {

	BASE_PATH("base.path", "PATH_TO_PROJECT"),
	DEEPL_API_KEY("api.key", "KEY"),
	SEARCH_UNSET_ONLY("search.unset.only", "true"),
	CONVERT_FILES("files.convert", "false"),
	LANGUAGE_DETECTION("files.detect", "true");

	private final String key;
	private final String defaultValue;

	SettingsConstant(String key, String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}

	public static String getSettingValue(Properties properties, SettingsConstant setting) {
		return properties.getProperty(setting.getKey(), setting.getDefaultValue());
	}

	public String getKey() {
		return key;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
}
