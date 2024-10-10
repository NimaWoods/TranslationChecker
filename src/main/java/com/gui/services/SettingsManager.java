package com.gui.services;

import com.gui.dialogs.SettingsDialog;

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

	public void saveSettings(Properties settings, JDialog dialog) {

		String basePathField = settings.get("base.path").toString();
		String languagesField = settings.get("languages").toString();
		String searchUnsetOnlyCheckbox = settings.get("search.unset.only").toString();
		String convertFilesCheckbox = settings.get("convert.files").toString();

		Path BASE_PATH = Path.of(basePathField);

		if (!Files.exists(BASE_PATH) || !Files.isDirectory(BASE_PATH)) {
			logger.log(Level.SEVERE, "Base path does not exist or is not a directory: " + BASE_PATH);
			JOptionPane.showMessageDialog(dialog, "The specified base path does not exist or is not a valid directory: " + BASE_PATH, "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		settings.setProperty("base.path", BASE_PATH.getText());
		settings.setProperty("languages", languagesField.getText());
		settings.setProperty("search.unset.only", Boolean.toString(searchUnsetOnlyCheckbox.isSelected()));
		settings.setProperty("convert.files", Boolean.toString(convertFilesCheckbox.isSelected()));

		try (OutputStream output = Files.newOutputStream(Paths.get("settings.properties"))) {
			settings.store(output, null);
			dialog.dispose();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(dialog, "Error saving settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}