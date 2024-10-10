package com.gui.dialogs;

import com.gui.services.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class SettingsDialog {

	public JTextField getBasePathField() {
		return basePathField;
	}

	public void setBasePathField(JTextField basePathField) {
		this.basePathField = basePathField;
	}

	public JTextField getLanguagesField() {
		return languagesField;
	}

	public void setLanguagesField(JTextField languagesField) {
		this.languagesField = languagesField;
	}

	public JCheckBox getSearchUnsetOnlyCheckbox() {
		return searchUnsetOnlyCheckbox;
	}

	public void setSearchUnsetOnlyCheckbox(JCheckBox searchUnsetOnlyCheckbox) {
		this.searchUnsetOnlyCheckbox = searchUnsetOnlyCheckbox;
	}

	public JCheckBox getConvertFilesCheckbox() {
		return convertFilesCheckbox;
	}

	public void setConvertFilesCheckbox(JCheckBox convertFilesCheckbox) {
		this.convertFilesCheckbox = convertFilesCheckbox;
	}


	private JTextField basePathField;
	private JTextField languagesField;
	private JCheckBox searchUnsetOnlyCheckbox;
	private JCheckBox convertFilesCheckbox;

	SettingsManager settingsDAO = new SettingsManager();

	public void show(JFrame parent, Properties settings) {
		JDialog settingsDialog = new JDialog(parent, "Settings", true);
		settingsDialog.setLayout(new GridLayout(5, 2));

		JLabel basePathLabel = new JLabel("Base Path:");
		basePathField = new JTextField(settings.getProperty("base.path", "path_to_project"));
		settingsDialog.add(basePathLabel);
		settingsDialog.add(basePathField);

		JLabel languagesLabel = new JLabel("Languages:");
		languagesField = new JTextField(settings.getProperty("languages", "de,en,es,fr,hu,it,nl,ru"));
		settingsDialog.add(languagesLabel);
		settingsDialog.add(languagesField);

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
			settingsDAO.saveSettings(settings, settingsDialog);
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