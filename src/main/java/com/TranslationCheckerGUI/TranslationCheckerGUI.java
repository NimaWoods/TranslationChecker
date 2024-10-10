package com.TranslationCheckerGUI;

import com.TranslationCheckerGUI.Dialogs.SettingsDialog;
import com.TranslationCheckerGUI.tools.Settings;
import com.TranslationCheckerGUI.tools.TranslationCheck;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.logging.Logger;

public class TranslationCheckerGUI extends JFrame {

	private static final Logger logger = Logger.getLogger(TranslationCheckerGUI.class.getName());
	private JTable table;
	private DefaultTableModel tableModel;
	private JProgressBar progressBar;
	private JLabel statusLabel;
	private Settings settings;

	public TranslationCheckerGUI() {
		settings = new Settings();
		settings.loadSettings();

		initLookAndFeel();
		initComponents();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			TranslationCheckerGUI gui = new TranslationCheckerGUI();
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
			TranslationCheck translationCheck = new TranslationCheck(progressBar, new String[]{"en", "de"}, "basePath", settings.getSettings());
			translationCheck.startTranslationCheck();
		});

		JButton settingsButton = new JButton("Settings");
		SettingsDialog settingsDialog = new SettingsDialog();
		settingsButton.addActionListener(e -> settingsDialog.showSettingsDialog(this, settings.getSettings()));

		JButton allTranslationsButton = new JButton("Edit Translations");

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
}