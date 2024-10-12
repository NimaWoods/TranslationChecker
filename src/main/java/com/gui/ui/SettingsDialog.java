package com.gui.ui;

import com.gui.manager.ConfigurationManager;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class SettingsDialog {

	ConfigurationManager configurationManager = new ConfigurationManager();

	public String getBasePathField() {
		return basePathField.getText();
	}

	public String getApiKeyField() {
		return apiKeyField.getText();
	}

	public boolean isSearchUnsetOnlyCheckboxSelected() {
		return searchUnsetOnlyCheckbox.isSelected();
	}

	private JTextField basePathField;
	private JTextField apiKeyField;
	private JCheckBox searchUnsetOnlyCheckbox;
	private JCheckBox convertFilesCheckbox;

	public boolean isConvertFilesCheckboxSelected() {
		return convertFilesCheckbox.isSelected();
	}

	public void show(JFrame parent, Properties settings) {
		JDialog settingsDialog = new JDialog(parent, "Settings", true);
		settingsDialog.setLayout(new GridLayout(5, 2));

		// Labels und Textfelder mit der UIComponentFactory erstellen
		JLabel basePathLabel = UIComponentFactory.createLabel("Base Path:");
		basePathField = UIComponentFactory.createTextField(settings.getProperty("base.path", "PATH_TO_PROJECT"));
		settingsDialog.add(basePathLabel);
		settingsDialog.add(basePathField);

		JLabel apiKeyLabel = UIComponentFactory.createLabel("DeepL API Key: ");
		apiKeyField = UIComponentFactory.createTextField(settings.getProperty("api.key", "KEY"));
		settingsDialog.add(apiKeyLabel);
		settingsDialog.add(apiKeyField);

		// CheckBoxen mit der UIComponentFactory erstellen
		searchUnsetOnlyCheckbox = UIComponentFactory.createCheckBox("Search only unset keys",
				Boolean.parseBoolean(settings.getProperty("search.unset.only", "true")));
		settingsDialog.add(UIComponentFactory.createLabel("Search unset keys only:"));
		settingsDialog.add(searchUnsetOnlyCheckbox);

		convertFilesCheckbox = UIComponentFactory.createCheckBox("Convert Files",
				Boolean.parseBoolean(settings.getProperty("convert.files", "false")));
		settingsDialog.add(UIComponentFactory.createLabel("Convert Files to right format:"));
		settingsDialog.add(convertFilesCheckbox);

		// Buttons und Panel für die Schaltflächen mit der UIComponentFactory erstellen
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveButton = UIComponentFactory.createButton("Save");
		saveButton.addActionListener(e -> {
			configurationManager.saveSettings(settings, settingsDialog, this);
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