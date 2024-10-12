package com.gui;

import com.gui.contsants.LanguagesConstant;
import com.gui.core.TranslationCheck;
import com.gui.manager.ConfigurationManager;
import com.gui.manager.TranslationManager;
import com.gui.model.LanguageProperties;
import com.gui.ui.EditTranslationsDialog;
import com.gui.ui.SettingsDialog;
import com.gui.ui.UIComponentFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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

	public DefaultTableModel getTableModel() {
		return tableModel;
	}

	private static final Logger logger = Logger.getLogger(TranslationCheckerApp.class.getName());
	private JTable table;
	private DefaultTableModel tableModel;
	private JProgressBar progressBar;
	private JLabel statusLabel;
	private ConfigurationManager settings;
	SettingsDialog settingsDialog;

	public TranslationCheckerApp() {
		settings = new ConfigurationManager();
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
			UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());  // Modernes FlatLaf Look and Feel
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initComponents() {
		setTitle("Translation Checker");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);  // Fenster maximiert starten
		setLocationRelativeTo(null);

		// Tabelle initialisieren
		String[] columnNames = {"Language", "Key", "Value", "File Path"};
		tableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 2;  // Nur die Value-Spalte ist editierbar
			}
		};

		// Verwende die Factory, um die Tabelle zu erstellen
		table = UIComponentFactory.createTable(tableModel);
		table.setTableHeader(UIComponentFactory.createTableHeader(table));  // Verwende die Factory für den Header

		// ScrollPane für die Tabelle erstellen
		JScrollPane scrollPane = UIComponentFactory.createScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);

		statusLabel = UIComponentFactory.createLabel("Translation Checker");
		statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);  // Rechtsbündig ausgerichtet

		initButtonsAndPanels();
	}

	private void initButtonsAndPanels() {
		JButton refreshButton = UIComponentFactory.createButton("Refresh");
		refreshButton.addActionListener(e -> {
			TranslationCheck translationCheck = new TranslationCheck(progressBar, this);
			translationCheck.startTranslationCheck();
		});

		JButton settingsButton = UIComponentFactory.createButton("Settings");
		settingsDialog = new SettingsDialog();
		settingsButton.addActionListener(e -> settingsDialog.show(this, settings.getSettings()));

		JButton translateButton = UIComponentFactory.createButton("Translate");
		TranslationManager translationManager = new TranslationManager(table, tableModel, translateButton);
		translationManager.addTranslateButtonListener();

		JButton allTranslationsButton = UIComponentFactory.createButton("Edit all Translations");
		allTranslationsButton.addActionListener(e -> {
			EditTranslationsDialog editTranslationsDialog = new EditTranslationsDialog(table, tableModel);
			editTranslationsDialog.show();
		});

		// Suchleiste
		JTextField searchField = UIComponentFactory.createTextField("");
		JButton searchButton = UIComponentFactory.createButton("Search");
		searchButton.addActionListener(e -> searchTable(searchField.getText()));

		// Panels erstellen
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(translateButton);
		buttonPanel.add(allTranslationsButton);

		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.add(searchField);
		searchPanel.add(searchButton);

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(searchPanel, BorderLayout.WEST);
		northPanel.add(buttonPanel, BorderLayout.EAST);

		JPanel southPanel = new JPanel(new BorderLayout());
		JPanel southWestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));  // FlowLayout für die Buttons links
		southWestPanel.add(settingsButton);
		southWestPanel.add(refreshButton);

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
					String value = properties.getProperty(key, "").trim();  // Trim Leerzeichen

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