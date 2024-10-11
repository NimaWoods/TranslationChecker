package com.gui.manager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import com.gui.TranslationCheckerApp;
import com.gui.services.LocaleEncodingService;

public class TranslationKeyManager {

	/**
	 * Updates the value of a translation key in the table and in the file.
	 **/
	public void updateColumnValue(String language, String key, String newValue, DefaultTableModel tableModel) {
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String tableLanguage = tableModel.getValueAt(row, 0).toString();
			String tableKey = tableModel.getValueAt(row, 1).toString();

			if (tableLanguage.equals(language) && tableKey.equals(key)) {
				tableModel.setValueAt(newValue, row, 2);
				break;
			}
		}
	}

	public void updateKeyInFile(String language, String key, String newValue, String filePath) throws IOException {
		Path path = Path.of(filePath);
		Charset encoding = LocaleEncodingService.getLocaleWithEncoding(language).getEncoding();

		List<String> lines = Files.readAllLines(path, encoding);
		boolean keyFound = false;

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.startsWith(key + "=")) {
				lines.set(i, key + "=" + newValue);
				keyFound = true;
				break;
			}
		}

		if (!keyFound) {
			// Füge den neuen Schlüssel hinzu, falls er nicht vorhanden ist
			lines.add(key + "=" + newValue);
		}

		// Schreibe die geänderten Zeilen zurück in die Datei
		Files.write(path, lines, encoding);

		TranslationCheckerApp app = new TranslationCheckerApp();
		app.setStatusLabel("Successfully updated key '" + key + "' with new value '" + newValue + "' in file: " + filePath);
		System.out.println("Successfully updated key '" + key + "' with new value '" + newValue + "' in file: " + filePath);
	}
}
