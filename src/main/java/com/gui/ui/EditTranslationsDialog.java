package com.gui.ui;

import com.gui.manager.TableManager;
import com.gui.manager.TranslationKeyManager;
import com.gui.manager.TranslationManager;
import com.gui.services.LocaleEncodingService;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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

		String[] columnNames = {"Language", "Key", "Value", "File Path"};

		// Verwende die Factory, um die Tabelle zu erstellen
		editDialogTableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 2;
			}
		};

		editDialogTable = UIComponentFactory.createTable(editDialogTableModel);

		// Daten in die Tabelle einfügen
		translationsWithPaths.forEach((language, details) -> editDialogTableModel.addRow(new Object[]{language, key, details[0], details[1]}));

		// Verwende die Factory, um das ScrollPane zu erstellen
		JScrollPane scrollPane = UIComponentFactory.createScrollPane(editDialogTable);
		dialog.add(scrollPane, BorderLayout.CENTER);

		TableManager tableManager = new TableManager();
		tableManager.registerTableModelListenerForEditValue(editDialogTableModel, editDialogTable);

		// Verwende die Factory, um den Button zu erstellen
		JPanel southPanel = new JPanel(new BorderLayout());
		JButton translateButton = UIComponentFactory.createButton("Translate");
		TranslationManager translationManager = new TranslationManager(table, tableModel, translateButton);
		translationManager.addTranslateButtonListener();

		southPanel.add(translateButton, BorderLayout.LINE_START);

		dialog.add(southPanel, BorderLayout.SOUTH);

		dialog.setSize(800, 400);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
}