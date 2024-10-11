package com.gui.ui;

import javax.swing.*;
import java.util.List;

public class ConvertedFilesDialog {

	public void show(List<String[]> convertedFiles) {
		if (!convertedFiles.isEmpty()) {
			String[] columnNames = {"Converted Files", "Old Encoding", "New Encoding"};

			Object[][] data = new Object[convertedFiles.size()][3];

			for (int i = 0; i < convertedFiles.size(); i++) {
				data[i][0] = convertedFiles.get(i)[0];
				data[i][1] = convertedFiles.get(i)[1];
				data[i][2] = convertedFiles.get(i)[2];
			}

			JTable table = new JTable(data, columnNames);
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setPreferredSize(new java.awt.Dimension(800, 200));

			JOptionPane.showMessageDialog(null, scrollPane, "Converted Files", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null, "No files were converted.", "Converted Files", JOptionPane.INFORMATION_MESSAGE);
		}
	}

}
