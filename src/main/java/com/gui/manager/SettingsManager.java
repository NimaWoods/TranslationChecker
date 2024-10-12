package com.gui.manager;

import com.gui.contsants.SettingsConstant;
import com.gui.ui.SettingsDialog;

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

public class SettingsManager {

	private static final Logger logger = Logger.getLogger(SettingsManager.class.getName());
	private Properties settings;

	public SettingsManager() {
		settings = new Properties();
		loadSettings();
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
		// Verwende die Enum-Standardwerte, um Standardeinstellungen zu erstellen
		for (SettingsConstant constant : SettingsConstant.values()) {
			settings.setProperty(constant.getKey(), constant.getDefaultValue());
		}

		try (OutputStream output = Files.newOutputStream(settingsPath)) {
			settings.store(output, "Default settings");
			logger.info("Settings file created with default values.");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error creating settings file.", e);
			JOptionPane.showMessageDialog(null, "Error creating settings file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void saveSettings(Properties settings, JDialog dialog, SettingsDialog settingsDialog) {
		String basePathField = settingsDialog.getBasePathField();
		String searchUnsetOnlyCheckbox = Boolean.toString(settingsDialog.isSearchUnsetOnlyCheckboxSelected());
		String apiKeyField = settingsDialog.getApiKeyField();
		String convertFilesCheckbox = Boolean.toString(settingsDialog.isConvertFilesCheckboxSelected());
		String languageDetectionCheckbox = Boolean.toString(settingsDialog.isLanguageDetectionCheckboxSelected());

		Path BASE_PATH = Path.of(basePathField);

		if (!Files.exists(BASE_PATH) || !Files.isDirectory(BASE_PATH)) {
			logger.log(Level.SEVERE, "Base path does not exist or is not a directory: " + BASE_PATH);
			JOptionPane.showMessageDialog(dialog, "The specified base path does not exist or is not a valid directory: "
					+ BASE_PATH, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Verwende die Enum-Schlüssel, um die neuen Werte zu speichern
		settings.setProperty(SettingsConstant.BASE_PATH.getKey(), basePathField);
		settings.setProperty(SettingsConstant.SEARCH_UNSET_ONLY.getKey(), searchUnsetOnlyCheckbox);
		settings.setProperty(SettingsConstant.DEEPL_API_KEY.getKey(), apiKeyField);
		settings.setProperty(SettingsConstant.CONVERT_FILES.getKey(), convertFilesCheckbox);
		settings.setProperty(SettingsConstant.LANGUAGE_DETECTION.getKey(), languageDetectionCheckbox);

		try (OutputStream output = Files.newOutputStream(Paths.get("settings.properties"))) {
			settings.store(output, null);
			dialog.dispose();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(dialog, "Error saving settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}