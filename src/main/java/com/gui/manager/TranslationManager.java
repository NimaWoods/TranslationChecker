package com.gui.manager;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.services.DeepLService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TranslationManager {

	private final JTable editDialogTable;
	private final DefaultTableModel editDialogTableModel;
	private final JButton translateButtons;
	private List<String> selectedValues;  // moved to an instance variable

	public TranslationManager(JTable editDialogTable, DefaultTableModel editDialogTableModel, JButton translateButtons) {
		this.editDialogTable = editDialogTable;
		this.editDialogTableModel = editDialogTableModel;
		this.translateButtons = translateButtons;
		this.selectedValues = new ArrayList<>();  // initialize the list
	}

	public void addTranslateButtonListener() {
		translateButtons.addActionListener(e -> {

			// Get selected row from the table
			int selectedRowInEditDialog = editDialogTable.getSelectedRow();
			if (selectedRowInEditDialog == -1) {
				JOptionPane.showMessageDialog(null, "Please select a row to translate.");
				return;
			}

			// Disable the translate button to prevent multiple clicks during the process
			translateButtons.setEnabled(false);

			// Create a SwingWorker to run the task in the background
			SwingWorker<Void, Void> worker = new SwingWorker<>() {
				@Override
				protected Void doInBackground() throws Exception {
					selectedValues.clear();  // clear the list before adding new values

					if (editDialogTable.getSelectedRowCount() > 1) {
						int[] selectedRowsInEditDialog = editDialogTable.getSelectedRows();
						for (int selectedRowInEditDialog : selectedRowsInEditDialog) {
							String selectedValue = (String) editDialogTable.getValueAt(selectedRowInEditDialog, 2);
							selectedValues.add(selectedValue);
						}
					} else {
						String selectedValue = (String) editDialogTable.getValueAt(selectedRowInEditDialog, 2);
						// Remove "(*)" at the end of the value
						Pattern languageCodePattern = Pattern.compile("\\s*\\((?i:" +
								Arrays.stream(LanguagesConstant.values())
										.map(LanguagesConstant::name)
										.collect(Collectors.joining("|")) +
								")\\)$");

						selectedValue = languageCodePattern.matcher(selectedValue).replaceAll("");
						selectedValues.add(selectedValue);
					}

					// Call the translation service (background task)
					DeepLService.translateTextList(selectedValues, "auto",
							editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString());

					// Update the Value in the file
					TranslationKeyManager translationKeyManager = new TranslationKeyManager();
					translationKeyManager.updateKeyInFile(editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString(),
							editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 1).toString(), selectedValues.get(0),
							editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 3).toString());

					return null;
				}

				@Override
				protected void done() {
					try {
						get();

						TranslationKeyManager translationKeyManager = new TranslationKeyManager();
						translationKeyManager.updateColumnValue(editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 0).toString(),
								editDialogTable.getValueAt(editDialogTable.getSelectedRow(), 1).toString(), selectedValues.get(0), editDialogTableModel);

						TranslationCheckerApp app = new TranslationCheckerApp();
						JTable mainTable = app.getTable();
						DefaultTableModel mainTableModel = app.getTableModel();
						translationKeyManager.updateColumnValue(mainTable.getValueAt(mainTable.getSelectedRow(), 0).toString(),
								mainTable.getValueAt(mainTable.getSelectedRow(), 1).toString(), selectedValues.get(0), mainTableModel);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Error during translation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					} finally {
						DeepLService.closeHttpClient();  // Close the HttpClient
						translateButtons.setEnabled(true);
					}
				}
			};

			// Execute the worker to start the background task
			worker.execute();
		});
	}
}
