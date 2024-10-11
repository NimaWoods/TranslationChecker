package com.gui.model;

import java.nio.file.Path;
import java.util.Properties;

public class LanguageProperties {
	private final Properties properties;
	private final Path path;

	public LanguageProperties(Properties properties, Path path) {
		this.properties = properties;
		this.path = path;
	}

	public Properties getProperties() {
		return properties;
	}

	public Path getPath() {
		return path;
	}
}