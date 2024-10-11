package com.gui.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.Language;
import com.gui.manager.TranslationKeyManager;
import com.gui.services.DeepLService;
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

		editDialogTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "saveEditing");
		editDialogTable.getActionMap().put("saveEditing", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (editDialogTable.isEditing()) {
					editDialogTable.getCellEditor().stopCellEditing();  // Editor stoppen und Wert speichern
				}
			}
		});

		translationsWithPaths.forEach(
				(language, details) -> editDialogTableModel.addRow(new Object[] { language, key, details[0], details[1] }));

		JScrollPane scrollPane = new JScrollPane(editDialogTable);
		JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
		dialog.add(scrollPane, BorderLayout.CENTER);
		dialog.add(scrollBar, BorderLayout.EAST);

		registerTableModelListenerForEditValue(editDialogTableModel);

		JPanel southPanel = new JPanel(new BorderLayout());

		JButton translateButtons = new JButton("Translate");
		translateButtons.addActionListener(e -> {

			// Get selected row from the table
			List<String> selectedValues = new ArrayList<>();

			if (editDialogTable.getSelectedRowCount() > 1) {
				int[] selectedRowsInEditDialog = editDialogTable.getSelectedRows();
				for (int selectedRowInEditDialog : selectedRowsInEditDialog) {
					String selectedValue = (String) editDialogTable.getValueAt(selectedRowInEditDialog, 2);
					selectedValues.add(selectedValue);
				}
			} else {
				int selectedRowInEditDialog = editDialogTable.getSelectedRow();
				if (selectedRowInEditDialog == -1) {
					JOptionPane.showMessageDialog(null, "Please select a row to translate.");
					return;
				}

				String selectedValue = (String) editDialogTable.getValueAt(selectedRowInEditDialog, 2);
				// Remove "(*)" at the end of the value
				Pattern languageCodePattern = Pattern.compile("\\s*\\((?i:" +
						Arrays.stream(Language.values())
								.map(Language::name)
								.collect(Collectors.joining("|")) +
						")\\)$");

				selectedValue = languageCodePattern.matcher(selectedValue).replaceAll("");
				selectedValues.add(selectedValue);
			}

			DeepLService.translateTextList(selectedValues, "auto",
					editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString());

			TranslationKeyManager translationKeyManager = new TranslationKeyManager();

			// Update the Value in the file
			try {
				translationKeyManager.updateKeyInFile(editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString(),
						editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 1).toString(), selectedValues.get(0),
						editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 3).toString());
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			// Update the value in the Dialog table
			translationKeyManager.updateColumnValue(editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString(),
					editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 1).toString(), selectedValues.get(0), editDialogTableModel);

			// Update the value in the main table
			TranslationCheckerApp app = new TranslationCheckerApp();
			JTable mainTable = app.getTable();
			DefaultTableModel mainTableModel = app.getTableModel();

			translationKeyManager.updateColumnValue(mainTable.getValueAt(mainTable.getSelectedRow(), 0).toString(),
					mainTable.getValueAt(mainTable.getSelectedRow(), 1).toString(), selectedValues.get(0), mainTableModel);

		});
		southPanel.add(translateButtons, BorderLayout.LINE_START);


		dialog.add(southPanel, BorderLayout.SOUTH);

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
					TranslationKeyManager translationKeyManager = new TranslationKeyManager();
					try {
						translationKeyManager.updateKeyInFile(language, key, newValue, filePath);
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}

					// Synchronisiere den Wert im Edit Dialog auch in der Haupttablle
					if (model != tableModel) {
						translationKeyManager.updateColumnValue(language, key, newValue, tableModel);
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
}