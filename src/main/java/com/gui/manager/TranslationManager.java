package com.gui.manager;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.services.DeepLService;
import com.gui.ui.FileWarningDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.*;
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

			translateButtons.setEnabled(false);

			// Get selected rows from the table
			int[] selectedRowsInEditDialog = editDialogTable.getSelectedRows();
			if (selectedRowsInEditDialog.length == 0) {
				JOptionPane.showMessageDialog(null, "Please select a row to translate.");
				return;
			}

			// Create a SwingWorker to run the task in the background
			SwingWorker<Void, Void> worker = new SwingWorker<>() {
				@Override
				protected Void doInBackground() throws Exception {
					selectedValues.clear();  // clear the list before adding new values

					TranslationKeyManager translationKeyManager = new TranslationKeyManager();
					TranslationCheckerApp app = new TranslationCheckerApp();
					JTable mainTable = app.getTable();
					DefaultTableModel mainTableModel = app.getTableModel();

					List<String> newValues = new ArrayList<>();
					Map<Path, String> failedFiles = new HashMap<>();

					// Loop through each selected row in the dialog table
					for (int selectedRow : selectedRowsInEditDialog) {
						String selectedValue = (String) editDialogTable.getValueAt(selectedRow, 2);

						// Remove "(*)" at the end of the value
						Pattern languageCodePattern = Pattern.compile("\\s*\\((?i:" +
								Arrays.stream(LanguagesConstant.values())
										.map(LanguagesConstant::name)
										.collect(Collectors.joining("|")) +
								")\\)$");
						selectedValue = languageCodePattern.matcher(selectedValue).replaceAll("");
						selectedValues.add(selectedValue);  // Add the cleaned value to the list

						String language = editDialogTable.getValueAt(selectedRow, 0).toString();
						String key = editDialogTable.getValueAt(selectedRow, 1).toString();
						String filePath = editDialogTable.getValueAt(selectedRow, 3).toString();

						// Translate the value using DeepL API
						String translatedValue;
						try {
							translatedValue = DeepLService.translateString(selectedValue, "auto", language);
							newValues.add(translatedValue);
						} catch (Exception e) {
							failedFiles.put(Path.of(filePath), e.getMessage());
							continue; // Skip further processing for this row
						}

						// Update the value in the file and both tables
						translationKeyManager.updateKeyInFile(language, key, translatedValue, filePath);
						translationKeyManager.updateColumnValue(language, key, translatedValue, editDialogTableModel);
						translationKeyManager.updateColumnValue(language, key, translatedValue, mainTableModel);
					}

					// If there are failed translations, show a dialog
					if (!failedFiles.isEmpty()) {
						FileWarningDialog.show(failedFiles, "Translation failed for the following files:");
					}

					return null;
				}

				@Override
				protected void done() {
					try {
						get(); // Wait for the doInBackground method to complete
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Error during translation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						System.out.printf("Error during translation: %s%n", ex.getMessage());
					} finally {
						// Re-enable the translate button
						translateButtons.setEnabled(true);
					}
				}
			};

			worker.execute();
		});
	}
}

