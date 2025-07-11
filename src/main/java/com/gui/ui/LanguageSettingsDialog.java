package com.gui.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.gui.manager.LanguageManager;

/**
 * Dialog for managing language settings
 */
public class LanguageSettingsDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private final LanguageManager languageManager;
    private final JTable languagesTable;
    private final DefaultTableModel tableModel;
    
    private final JTextField languageCodeField;
    private final JComboBox<String> encodingComboBox;
    
    /**
     * Creates a new language settings dialog
     * 
     * @param parent Parent frame
     */
    public LanguageSettingsDialog(JFrame parent) {
        super(parent, "Language Settings", true);
        this.languageManager = LanguageManager.getInstance();
        
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(parent);
        
        // Create table model
        String[] columnNames = {"Language Code", "Language Name", "Encoding"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        languagesTable = new JTable(tableModel);
        languagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(languagesTable);
        
        // Create form panel for adding languages
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add Language"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Language code field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Language Code (ISO 639):"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        languageCodeField = new JTextField(10);
        formPanel.add(languageCodeField, gbc);
        
        // Encoding combo box
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Encoding:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        
        // Get available charsets
        List<String> charsets = new ArrayList<>();
        Map<String, Charset> availableCharsets = Charset.availableCharsets();
        for (String name : availableCharsets.keySet()) {
            charsets.add(name);
        }
        
        encodingComboBox = new JComboBox<>(new DefaultComboBoxModel<>(
                charsets.toArray(new String[0])));
        encodingComboBox.setSelectedItem("UTF-8");
        formPanel.add(encodingComboBox, gbc);
        
        // Add button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = new JButton("Add Language");
        addButton.addActionListener(e -> addLanguage());
        formPanel.add(addButton, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedLanguage());
        buttonPanel.add(removeButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        // Add components to dialog
        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Load languages
        loadLanguages();
    }
    
    /**
     * Loads languages from the language manager into the table
     */
    private void loadLanguages() {
        tableModel.setRowCount(0);
        
        Map<String, Locale> languages = languageManager.getAvailableLanguages();
        for (Map.Entry<String, Locale> entry : languages.entrySet()) {
            String langCode = entry.getKey();
            Locale locale = entry.getValue();
            Charset charset = languageManager.getEncodingForLanguage(langCode);
            
            Vector<String> row = new Vector<>();
            row.add(langCode);
            row.add(locale.getDisplayLanguage(Locale.ENGLISH));
            row.add(charset.name());
            
            tableModel.addRow(row);
        }
    }
    
    /**
     * Adds a new language
     */
    private void addLanguage() {
        String langCode = languageCodeField.getText().trim().toLowerCase();
        String encoding = (String) encodingComboBox.getSelectedItem();
        
        if (langCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "Please enter a language code", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate language code (ISO 639 is typically 2 or 3 characters)
        if (langCode.length() < 2 || langCode.length() > 3) {
            JOptionPane.showMessageDialog(this, 
                    "Language code should be 2 or 3 characters (ISO 639 standard)", 
                    "Invalid Language Code", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if language already exists
        if (languageManager.getAvailableLanguages().containsKey(langCode)) {
            JOptionPane.showMessageDialog(this, 
                    "Language already exists", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        boolean success = languageManager.addLanguage(langCode, encoding);
        if (success) {
            loadLanguages();
            languageCodeField.setText("");
            JOptionPane.showMessageDialog(this, 
                    "Language added successfully", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Failed to add language", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Removes the selected language
     */
    private void removeSelectedLanguage() {
        int selectedRow = languagesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                    "Please select a language to remove", 
                    "No Selection", 
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String langCode = (String) tableModel.getValueAt(selectedRow, 0);
        
        // Confirm removal
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove the language: " + langCode + "?", 
                "Confirm Removal", 
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = languageManager.removeLanguage(langCode);
            if (success) {
                loadLanguages();
                JOptionPane.showMessageDialog(this, 
                        "Language removed successfully", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                        "Failed to remove language", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Shows the language settings dialog
     * 
     * @param parent Parent frame
     */
    public static void showDialog(JFrame parent) {
        LanguageSettingsDialog dialog = new LanguageSettingsDialog(parent);
        dialog.setVisible(true);
    }
}
