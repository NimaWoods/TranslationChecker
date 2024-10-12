package com.gui;

import com.gui.contsants.LanguagesConstant;
import com.gui.core.TranslationCheck;
import com.gui.manager.SettingsManager;
import com.gui.manager.TranslationManager;
import com.gui.model.LanguageProperties;
import com.gui.ui.EditTranslationsDialog;
import com.gui.ui.SettingsDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationCheckerApp extends JFrame {

	private static final Logger logger = Logger.getLogger(TranslationCheckerApp.class.getName());

	private JTable table;

	public JTable getTable() {
		return table;
	}

	public void setTable(JTable table) {
		this.table = table;
	}

	public DefaultTableModel getTableModel() {
		return tableModel;
	}

	public void setTableModel(DefaultTableModel tableModel) {
		this.tableModel = tableModel;
	}

	private DefaultTableModel tableModel;
	private JProgressBar progressBar;
	private JLabel statusLabel;
	private SettingsManager settings;

	SettingsDialog settingsDialog;

	public TranslationCheckerApp() {
		settings = new SettingsManager();
		settings.loadSettings();

		initLookAndFeel();
		initComponents();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			TranslationCheckerApp gui = new TranslationCheckerApp();
			gui.setVisible(true);
		});
	}

	private void initLookAndFeel() {
		try {
			UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initComponents() {
		setTitle("Translation Checker");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);

		String[] columnNames = {"LanguagesConstant", "Key", "Value", "File Path"};
		tableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 2;
			}
		};

		table = new JTable(tableModel);
		configureTable();

		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);

		statusLabel = new JLabel("Translation Checker");

		initButtonsAndPanels();
	}

	private void configureTable() {
		JTableHeader header = table.getTableHeader();
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
		table.setShowGrid(true);
		table.setGridColor(Color.GRAY);
		table.getColumnModel().getColumn(0).setMinWidth(50);
		table.getColumnModel().getColumn(0).setMaxWidth(150);
	}


	private void initButtonsAndPanels() {
		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(e -> {
			TranslationCheck translationCheck = new TranslationCheck(progressBar, this);
			translationCheck.startTranslationCheck();
		});

		JButton settingsButton = new JButton("Settings");
		settingsDialog = new SettingsDialog();
		settingsButton.addActionListener(e -> {
			settingsDialog.show(this, settings.getSettings());
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JButton translateButtons = new JButton("Translate");
		translateButtons.addActionListener(e -> {
			TranslationManager translationManager = new TranslationManager(table, tableModel, translateButtons);
			translationManager.addTranslateButtonListener();
		});

		JButton allTranslationsButton = new JButton("Edit all Translations");
		allTranslationsButton.addActionListener(e -> {
			EditTranslationsDialog editTranslationsDialog = new EditTranslationsDialog(table, tableModel);
			editTranslationsDialog.show();
		});

		JTextField searchField = new JTextField(20);
		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(e -> searchTable(searchField.getText()));

		JPanel southPanel = new JPanel(new BorderLayout());
		JPanel southWestPanel = new JPanel(new BorderLayout());
		JPanel northPanel = new JPanel(new BorderLayout());

		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.add(searchField);
		searchPanel.add(searchButton);

		buttonPanel.add(translateButtons);
		buttonPanel.add(allTranslationsButton);

		northPanel.add(searchPanel, BorderLayout.WEST);
		northPanel.add(buttonPanel, BorderLayout.EAST);

		southWestPanel.add(settingsButton, BorderLayout.WEST);
		southWestPanel.add(refreshButton, BorderLayout.CENTER);

		southPanel.add(southWestPanel, BorderLayout.WEST);
		southPanel.add(progressBar, BorderLayout.NORTH);
		southPanel.add(statusLabel, BorderLayout.EAST);

		add(southPanel, BorderLayout.SOUTH);
		add(northPanel, BorderLayout.NORTH);
	}

	private void searchTable(String search) {
		if (search.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a search term.");
			return;
		}

		for (int i = 0; i < tableModel.getRowCount(); i++) {
			String key = (String) tableModel.getValueAt(i, 1);
			if (key.contains(search)) {
				table.setRowSelectionInterval(i, i);
				table.scrollRectToVisible(table.getCellRect(i, 0, true));
				break;
			}
		}
	}

	public void updateTable(Map<LanguagesConstant, List<LanguageProperties>> propertiesMap, boolean searchUnsetOnly) {
		tableModel.setRowCount(0);

		Pattern languageCodePattern = Pattern.compile(".*\\((\\w{2})\\)$");

		int totalEntries = 0;

		for (LanguagesConstant lang : propertiesMap.keySet()) {
			List<LanguageProperties> languageFiles = propertiesMap.get(lang);

			for (LanguageProperties languageProps : languageFiles) {
				Properties properties = languageProps.getProperties();
				Path filePath = languageProps.getPath();

				for (String key : properties.stringPropertyNames()) {
					String value = properties.getProperty(key, "").trim(); // Trim Leerzeichen

					// Matcher nur einmal berechnen
					Matcher matcher = languageCodePattern.matcher(value);

					if (searchUnsetOnly) {
						if (matcher.matches()) {
							tableModel.addRow(new Object[]{lang.getLocale(), key, value, filePath.toString()});
							totalEntries++;
						} else if (value.isEmpty()) {
							tableModel.addRow(new Object[]{lang.getLocale(), key, " ", filePath.toString()});
							totalEntries++;
						}
					} else {
						tableModel.addRow(new Object[]{lang.getLocale(), key, value, filePath.toString()});
						totalEntries++;
					}
				}
			}
		}

		statusLabel.setText("Total Entries: " + totalEntries);
		sortTable();
	}

	public void sortTable() {
		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);

		List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
		sorter.setSortKeys(sortKeys);

		table.revalidate();
		table.repaint();
	}

	public void setStatusLabel(String text) {
		statusLabel.setText(text);
	}
}