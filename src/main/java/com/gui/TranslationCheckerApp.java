package com.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import com.gui.contsants.Language;
import com.gui.core.TranslationCheck;
import com.gui.manager.SettingsManager;
import com.gui.model.LanguageProperties;
import com.gui.ui.EditTranslationsDialog;
import com.gui.ui.SettingsDialog;

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

		String[] columnNames = {"Language", "Key", "Value", "File Path"};
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

		JButton allTranslationsButton = new JButton("Edit Translations");
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

		northPanel.add(searchPanel, BorderLayout.WEST);
		northPanel.add(allTranslationsButton, BorderLayout.EAST);

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

	public void updateTable(Map<Language, List<LanguageProperties>> propertiesMap, boolean searchUnsetOnly, String[] LANGUAGES) {
		tableModel.setRowCount(0);

		Pattern languageCodePattern = Pattern.compile(".*\\((" + String.join("|", LANGUAGES) + ")\\)$");
		int totalEntries = 0;

		for (Language lang : propertiesMap.keySet()) {
			List<LanguageProperties> languageFiles = propertiesMap.get(lang);

			for (LanguageProperties languageProps : languageFiles) {
				Properties properties = languageProps.getProperties();
				Path filePath = languageProps.getPath();

				for (String key : properties.stringPropertyNames()) {
					String value = properties.getProperty(key, "");

					if (searchUnsetOnly) {
						if (languageCodePattern.matcher(value).matches()) {
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
		sortTabel();
	}

	public void sortTabel() {
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