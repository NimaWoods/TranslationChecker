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

		JLabel basePathLabel = new JLabel("Base Path:");
		basePathField = new JTextField(settings.getProperty("base.path", "PATH_TO_PROJECT"));
		settingsDialog.add(basePathLabel);
		settingsDialog.add(basePathField);

		JLabel apiKeyLabel = new JLabel("DeepL API Key: ");
		apiKeyField = new JTextField(settings.getProperty("api.key", "KEY"));
		settingsDialog.add(apiKeyLabel);
		settingsDialog.add(apiKeyField);

		searchUnsetOnlyCheckbox = new JCheckBox("Search only unset keys",
				Boolean.parseBoolean(settings.getProperty("search.unset.only", "true")));
		settingsDialog.add(new JLabel("Search unset keys only:"));
		settingsDialog.add(searchUnsetOnlyCheckbox);

		convertFilesCheckbox = new JCheckBox("Convert Files", Boolean.parseBoolean(settings.getProperty("convert.files", "false")));
		settingsDialog.add(new JLabel("Convert Files to right format:"));
		settingsDialog.add(convertFilesCheckbox);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> {
			configurationManager.saveSettings(settings, settingsDialog, this);
			settingsDialog.dispose();
		});
		buttonPanel.add(saveButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> settingsDialog.dispose());
		buttonPanel.add(cancelButton);

		settingsDialog.add(buttonPanel);

		settingsDialog.pack();
		settingsDialog.setLocationRelativeTo(parent);
		settingsDialog.setVisible(true);
	}
}