package com.gui.manager;

import com.gui.TranslationCheckerApp;
import com.gui.contsants.LanguagesConstant;
import com.gui.services.DeepLService;
import com.gui.ui.FileWarningDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TranslationManager {

	private final JTable editDialogTable;
	private final DefaultTableModel editDialogTableModel;
	private final JButton translateButtons;
	private List<String> selectedValues;
	private boolean dialogAlreadyOpen = false;

	public TranslationManager(JTable editDialogTable, DefaultTableModel editDialogTableModel, JButton translateButtons) {
		this.editDialogTable = editDialogTable;
		this.editDialogTableModel = editDialogTableModel;
		this.translateButtons = translateButtons;
		this.selectedValues = new ArrayList<>();
	}

	public void addTranslateButtonListener() {
		translateButtons.addActionListener(e -> {
			if (dialogAlreadyOpen) {
				return;
			}

			translateButtons.setEnabled(false);

			// Create a SwingWorker to run the task in the background
			SwingWorker<Void, Void> worker = new SwingWorker<>() {
				@Override
				protected Void doInBackground() throws Exception {
					selectedValues.clear();

					int[] selectedRowsInEditDialog = editDialogTable.getSelectedRows();
					if (selectedRowsInEditDialog.length == 0) {
						throw new Exception("Please select a row to translate.");
					}

					for (int selectedRow : selectedRowsInEditDialog) {
						String selectedValue = (String) editDialogTable.getValueAt(selectedRow, 2);
						selectedValues.add(selectedValue);
					}

					boolean isEnoughLeft = DeepLService.isEnoughTokensLeft(selectedValues);

					if (!isEnoughLeft) {
						// Dialog nur auf dem EDT anzeigen
						SwingUtilities.invokeLater(() -> {
							dialogAlreadyOpen = true;
							int charCount = selectedValues.stream().mapToInt(String::length).sum();
							JOptionPane optionPane = new JOptionPane(
									"Not enough tokens left for translation.\n" +
											"\n" +
											"Characters Needed: " + charCount + "\n" +
											"Characters Left: " + DeepLService.getRemainingCharacters() + "\n" +
											"\n" +
											"Continue?",
									JOptionPane.ERROR_MESSAGE
							);

							JDialog dialog = optionPane.createDialog("Not enough tokens left");

							optionPane.setOptions(new Object[]{});

							JButton yesButton = new JButton("Yes");
							yesButton.addActionListener(e -> {
								optionPane.setValue(JOptionPane.YES_OPTION);
								dialog.dispose();
								startTranslation(selectedRowsInEditDialog);
								dialogAlreadyOpen = false;
							});

							JButton noButton = new JButton("No");
							noButton.addActionListener(e -> {
								optionPane.setValue(JOptionPane.NO_OPTION);
								dialog.dispose();
								dialogAlreadyOpen = false; // Dialog ist geschlossen
							});

							JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
							buttonPanel.add(yesButton);
							buttonPanel.add(noButton);

							optionPane.add(buttonPanel, BorderLayout.SOUTH);

							dialog.setAlwaysOnTop(true);
							dialog.setVisible(true);
						});
					} else {
						startTranslation(selectedRowsInEditDialog);
					}
					return null;
				}

				private void startTranslation(int[] selectedRowsInEditDialog) {
					List<String> newValues = new ArrayList<>();
					Map<String, String> failedFiles = new HashMap<>();
					TranslationCheckerApp app = new TranslationCheckerApp();
					DefaultTableModel mainTableModel = app.getTableModel();

					TranslationKeyManager translationKeyManager = new TranslationKeyManager();

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
							failedFiles.put(Path.of(filePath) + " (" + key.toString() + ")", e.getMessage());
							continue;
						}

						// Update the value in the file and both tables
						try {
							translationKeyManager.updateKeyInFile(language, key, translatedValue, filePath);
							translationKeyManager.updateColumnValue(language, key, translatedValue, editDialogTableModel);
							translationKeyManager.updateColumnValue(language, key, translatedValue, mainTableModel);
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					}

					// If there are failed translations, show a dialog
					if (!failedFiles.isEmpty()) {
						FileWarningDialog.show(failedFiles, "Translation failed for the following files:");
					}
				}

				@Override
				protected void done() {
					try {
						get(); // Wait for the doInBackground method to complete
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(null, "Error during translation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						System.out.printf("Error during translation: %s%n", ex.getMessage());
					} finally {
						translateButtons.setEnabled(true);
					}
				}
			};

			worker.execute();
		});
	}
}

