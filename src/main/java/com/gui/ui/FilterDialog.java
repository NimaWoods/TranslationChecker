package com.gui.ui;

import com.gui.contsants.LanguagesConstant;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class FilterDialog {

	private JComboBox<String> filterComboBox;

	public void show(JFrame parent) {
		JDialog settingsDialog = new JDialog(parent, "Filter", true);
		settingsDialog.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel languageFilterText = UIComponentFactory.createLabel("Language: ");

		String[] languageFilterArray = new String[LanguagesConstant.values().length + 1];
		languageFilterArray[0] = "";

		int i = 1;
		for (String locale : Arrays.stream(LanguagesConstant.values()).map(Enum::name).toArray(String[]::new)) {
			languageFilterArray[i++] = locale;
		}

		filterComboBox = UIComponentFactory.createComboBox(languageFilterArray);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		settingsDialog.add(languageFilterText, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		settingsDialog.add(filterComboBox, gbc);

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton okButton = UIComponentFactory.createButton("Save");
		okButton.addActionListener(e -> settingsDialog.dispose());

		southPanel.add(okButton);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		settingsDialog.add(southPanel, gbc);

		settingsDialog.pack();
		settingsDialog.setLocationRelativeTo(parent);
		settingsDialog.setVisible(true);
	}

	public String getSelectedFilter() {
		return (String) filterComboBox.getSelectedItem();
	}
}