package com.TranslationCheckerGUI.tools;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings {

	private static final Logger logger = Logger.getLogger(Settings.class.getName());
	private Properties settings;

	public Settings() {
		settings = new Properties();
	}

	public Properties getSettings() {
		return settings;
	}

	public void loadSettings() {
		Path settingsPath = Paths.get("settings.properties");

		try (InputStream input = Files.newInputStream(settingsPath)) {
			settings.load(input);
		} catch (IOException e) {
			createDefaultSettings(settingsPath);
		}
	}

	private void createDefaultSettings(Path settingsPath) {
		settings.setProperty("base.path", "path_to_project");
		settings.setProperty("languages", "de,en,es,fr,hu,it,nl,ru");

		try (OutputStream output = Files.newOutputStream(settingsPath)) {
			settings.store(output, "Default settings");
			logger.info("Settings file created with default values.");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error creating settings file.", e);
			JOptionPane.showMessageDialog(null, "Error creating settings file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void saveSettings() {
		Path settingsPath = Paths.get("settings.properties");

		try (OutputStream output = Files.newOutputStream(settingsPath)) {
			settings.store(output, "Saved settings");
			logger.info("Settings saved successfully.");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error saving settings file.", e);
			JOptionPane.showMessageDialog(null, "Error saving settings file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}