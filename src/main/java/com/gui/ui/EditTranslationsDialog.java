package com.gui.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.Language;
import com.gui.services.LocaleEncodingService;

public class EditTranslationsDialog {

	private final JTable table;
	private final DefaultTableModel tableModel;
	private DefaultTableModel editDialogTableModel;
	private JTable editDialogTable;

	public EditTranslationsDialog(JTable table, DefaultTableModel tableModel) {
		this.table = table;
		this.tableModel = tableModel;
	}

	public void show() {
		int selectedRow = table.getSelectedRow();
		if (selectedRow == -1) {
			JOptionPane.showMessageDialog(null, "Please select a row to view translations.");
			return;
		}

		int modelRow = table.convertRowIndexToModel(selectedRow);

		String selectedKey = (String) tableModel.getValueAt(modelRow, 1); // Key aus der Tabelle
		String filePath = (String) tableModel.getValueAt(modelRow, 3); // Pfad aus der Tabelle

		Path selectedFilePath = Paths.get(filePath);
		System.out.println("Converted Path Object: " + selectedFilePath);

		LocaleEncodingService localeEncodingService = new LocaleEncodingService();
		Map<String, String[]> translationsWithPaths = localeEncodingService.loadTranslationsForKey(selectedKey, selectedFilePath);

		if (translationsWithPaths.isEmpty()) {
			JOptionPane.showMessageDialog(null, "No translations found for the selected key.");
			return;
		}

		openEditTranslationsDialog(selectedKey, translationsWithPaths);
	}

	private void openEditTranslationsDialog(String key, Map<String, String[]> translationsWithPaths) {
		JDialog dialog = new JDialog((Frame) null, "Translations for Key: " + key, true);
		dialog.setLayout(new BorderLayout());

		String[] columnNames = { "Language", "Key", "Value", "File Path" };

		editDialogTableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 2;
			}
		};

		editDialogTable = new JTable(editDialogTableModel);
		editDialogTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		editDialogTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		editDialogTable.getColumnModel().getColumn(2).setPreferredWidth(400);
		editDialogTable.getColumnModel().getColumn(3).setPreferredWidth(150);

		editDialogTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke("ENTER"), "saveEditing");
		editDialogTable.getActionMap().put("saveEditing", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (editDialogTable.isEditing()) {
					editDialogTable.getCellEditor().stopCellEditing();  // Editor stoppen und Wert speichern
				}
			}
		});

		translationsWithPaths.forEach((language, details) ->
				editDialogTableModel.addRow(new Object[] { language, key, details[0], details[1] }));

		JScrollPane scrollPane = new JScrollPane(editDialogTable);
		JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
		dialog.add(scrollPane, BorderLayout.CENTER);
		dialog.add(scrollBar, BorderLayout.EAST);

		registerTableModelListenerForEditValue(editDialogTableModel);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dialog.dispose());
		dialog.add(closeButton, BorderLayout.SOUTH);

		dialog.setSize(800, 400);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void registerTableModelListenerForEditValue(DefaultTableModel model) {
		model.addTableModelListener(e -> {
			if (e.getType() == TableModelEvent.UPDATE) {
				JTable currentTable = model == tableModel ? table : findTableByModel(model);

				int startRow = e.getFirstRow();
				int endRow = e.getLastRow();

				for (int row = startRow; row <= endRow; row++) {
					int modelRow = currentTable.convertRowIndexToModel(row);

					String newValue;

					if (currentTable.isEditing()) {
						newValue = (String) currentTable.getCellEditor().getCellEditorValue();
					} else {
						newValue = (String) currentTable.getValueAt(modelRow, 2);
					}

					// Sprache, Key und Datei aus der Tabelle holen
					String language = (String) model.getValueAt(modelRow, 0);
					String key = (String) model.getValueAt(modelRow, 1);
					String filePath = (String) model.getValueAt(modelRow, 3);

					// Aktualisiere den Key in der Datei
					try {
						updateKeyInFile(language, key, newValue, filePath);
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}

					// Synchronisiere den Wert im Edit Dialog auch in der Haupttablle
					if (model != tableModel) {
						updateColumnValue(language, key, newValue);
					}
				}
			}
		});
	}

	private JTable findTableByModel(DefaultTableModel model) {
		if (model == tableModel) {
			return table;
		} else if (model == editDialogTableModel) {
			return editDialogTable;
		}
		return null;
	}

	// Methode, um den Wert in der Haupttabelle zu aktualisieren, wenn er im Edit-Dialog geändert wurde
	private void updateColumnValue(String language, String key, String newValue) {
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			String tableLanguage = tableModel.getValueAt(row, 0).toString();
			String tableKey = tableModel.getValueAt(row, 1).toString();

			if (tableLanguage.equals(language) && tableKey.equals(key)) {
				tableModel.setValueAt(newValue, row, 2);
				break;
			}
		}
	}

	private void updateKeyInFile(String language, String key, String newValue, String filePath) throws IOException {
		Path path = Path.of(filePath);
		Charset encoding = LocaleEncodingService.getLocaleWithEncoding(language).getEncoding();

		// Lese alle Zeilen der Datei
		List<String> lines = Files.readAllLines(path, encoding);
		boolean keyFound = false;

		// Gehe durch jede Zeile und ersetze die Zeile, die den Key enthält
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.startsWith(key + "=")) {
				lines.set(i, key + "=" + newValue); // Aktualisiere die Zeile mit dem neuen Wert
				keyFound = true;
				break; // Sobald der Key gefunden und aktualisiert wurde, verlasse die Schleife
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

	private Language getLanguage(String lang) {
		return LocaleEncodingService.getLocaleWithEncoding(lang);
	}
}