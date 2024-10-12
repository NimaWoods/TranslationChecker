package com.gui.ui;

import com.gui.contsants.SettingsConstant;
import com.gui.manager.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class SettingsDialog {

	SettingsManager settingsManager = new SettingsManager();

	public String getBasePathField() {
		return basePathField.getText();
	}

	public String getApiKeyField() {
		return apiKeyField.getText();
	}

	public boolean isSearchUnsetOnlyCheckboxSelected() {
		return searchUnsetOnlyCheckbox.isSelected();
	}
	private JCheckBox languageDetectionCheckbox;

	private JTextField basePathField;
	private JTextField apiKeyField;
	private JCheckBox searchUnsetOnlyCheckbox;
	private JCheckBox convertFilesCheckbox;

	public boolean isLanguageDetectionCheckboxSelected() {
		return languageDetectionCheckbox.isSelected();
	}

	public boolean isConvertFilesCheckboxSelected() {
		return convertFilesCheckbox.isSelected();
	}

	public void show(JFrame parent, Properties settings) {
		JDialog settingsDialog = new JDialog(parent, "Settings", true);
		settingsDialog.setLayout(new GridLayout(6, 2));

		// Labels und Textfelder mit der UIComponentFactory erstellen
		JLabel basePathLabel = UIComponentFactory.createLabel("Base Path:");
		basePathField = UIComponentFactory.createTextField(
				SettingsConstant.getSettingValue(settings, SettingsConstant.BASE_PATH));
		settingsDialog.add(basePathLabel);
		settingsDialog.add(basePathField);

		JLabel apiKeyLabel = UIComponentFactory.createLabel("DeepL API Key: ");
		apiKeyField = UIComponentFactory.createTextField(
				SettingsConstant.getSettingValue(settings, SettingsConstant.DEEPL_API_KEY));
		settingsDialog.add(apiKeyLabel);
		settingsDialog.add(apiKeyField);

		// CheckBoxen mit der UIComponentFactory erstellen
		searchUnsetOnlyCheckbox = UIComponentFactory.createCheckBox("Search only unset keys",
				Boolean.parseBoolean(SettingsConstant.getSettingValue(settings, SettingsConstant.SEARCH_UNSET_ONLY)));
		settingsDialog.add(UIComponentFactory.createLabel("Search unset keys only:"));
		settingsDialog.add(searchUnsetOnlyCheckbox);

		convertFilesCheckbox = UIComponentFactory.createCheckBox("Convert Files",
				Boolean.parseBoolean(SettingsConstant.getSettingValue(settings, SettingsConstant.CONVERT_FILES)));
		settingsDialog.add(UIComponentFactory.createLabel("Convert Files to right format:"));
		settingsDialog.add(convertFilesCheckbox);

		languageDetectionCheckbox = UIComponentFactory.createCheckBox("Detect Language",
				Boolean.parseBoolean(SettingsConstant.getSettingValue(settings, SettingsConstant.LANGUAGE_DETECTION)));
		settingsDialog.add(UIComponentFactory.createLabel("Detect Source Translation Language:"));
		settingsDialog.add(languageDetectionCheckbox);

		// Buttons und Panel für die Schaltflächen mit der UIComponentFactory erstellen
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveButton = UIComponentFactory.createButton("Save");
		saveButton.addActionListener(e -> {
			settingsManager.saveSettings(settings, settingsDialog, this);
			settingsDialog.dispose();
		});
		buttonPanel.add(saveButton);

		JButton cancelButton = UIComponentFactory.createButton("Cancel");
		cancelButton.addActionListener(e -> settingsDialog.dispose());
		buttonPanel.add(cancelButton);

		settingsDialog.add(buttonPanel);

		settingsDialog.pack();
		settingsDialog.setLocationRelativeTo(parent);
		settingsDialog.setVisible(true);
	}
}