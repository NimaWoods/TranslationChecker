/*
 * Created on 15/09/2023, 10:28
 *
 * Copyright (c) 2023-2024
 * topsystem GmbH, Aachen, Germany
 *
 * All rights reserved
 */

package de.topsystem.localization;

import com.deepl.api.DeepLException;
import com.formdev.flatlaf.FlatLightLaf;
import de.topsystem.localization.tools.ConstantsAndMethodsForImport.LocaleWithEncoding;
import org.apache.commons.configuration2.ex.ConfigurationException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Vector;

import static de.topsystem.localization.tools.BaseDataImporter.executeBasedataTranslationsImport;
import static de.topsystem.localization.tools.ConstantsAndMethodsForImport.getProjectByModuleName;
import static de.topsystem.localization.tools.FindAndTranslateFlaggedProperties.deeplApiLimit;
import static de.topsystem.localization.tools.FindAndTranslateFlaggedProperties.executeFindAndTranslate;
import static de.topsystem.localization.tools.PropertyImporter.executeLocalizationTranslationsImport;

public class TranslationApplication {

    public static void main(String[] args) {
        createAndShowGUI();
    }

    private static boolean validateFilePath(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private static void createAndShowGUI() {
        // Set the FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Main GUI
        JFrame frame = new JFrame("Translation Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 400);  // Set the window size to 1000x400
        frame.setResizable(false);  // Disable resizing

        JTabbedPane tabbedPane = new JTabbedPane();

        // Setup tabs
        tabbedPane.addTab("Import translations (messages_xx.properties)", createImportTranslationsPanel());
        tabbedPane.addTab("Import basedata translations (localized-xxx.csv)", createImportBasedataTranslationsPanel());
        tabbedPane.addTab("Machine translation of (LOCALE) properties", machineTranslationPanel());

        // Add tabbedPane to frame
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null); // Center the frame
        frame.setVisible(true);
    }

    private static JPanel createImportTranslationsPanel() {
        JPanel mainPanel = new JPanel(new CardLayout());

        // Original input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel informationField = new JLabel("Please enter the path to your file and the language you wish to import");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        inputPanel.add(informationField, gbc);

        JLabel fileFormatRule = new JLabel("The file must have the format " +
                "\"ghs/src/de/topsystem/ghs/bl/core/service/impl.Active<tab>Activ\" in order to be imported.");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        inputPanel.add(fileFormatRule, gbc);

        JTextField pathField = getPathField(gbc, 2, inputPanel);

        JLabel errorMessage = getErrorMessageLabel(gbc, 3, inputPanel);

        JComboBox<String> projectComboBox = getProjectComboBox(gbc, 4, inputPanel, false);

        JComboBox<LocaleWithEncoding> comboBox = getLanguageComboBox(gbc, 5, inputPanel);

        JButton okButton = getOkButton(gbc, 6, inputPanel);

        // Adding input panel to main panel
        mainPanel.add(inputPanel, "Input");

        JPanel importingPanel = getLoadingPanel("Importing...", gbc);

        JPanel importedPanel = getSuccessPanel("Imported successfully!", gbc);

        JButton resetButton = getResetButton(gbc, 2, importedPanel);

        // Adding importing and imported panel to main panel
        mainPanel.add(importingPanel, "Importing");
        mainPanel.add(importedPanel, "Imported");

        // OK button action listener
        okButton.addActionListener(e -> {
            final String filePath = pathField.getText();
            final LocaleWithEncoding language = (LocaleWithEncoding) comboBox.getSelectedItem();
            final String project = (String) projectComboBox.getSelectedItem();

            errorMessage.setText("");  // Clear previous messages

            if (!filePath.isEmpty() && language != null) {
                if (validateFilePath(filePath)) {
                    // Disable button and switch to importing panel
                    okButton.setEnabled(false);
                    CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
                    cardLayout.show(mainPanel, "Importing");

                    // Start importing
                    new Thread(() -> {
                        try {
                            executeLocalizationTranslationsImport(filePath, language, project);
                            SwingUtilities.invokeLater(() -> {
                                // Success message
                                CardLayout cl = (CardLayout) mainPanel.getLayout();
                                cl.show(mainPanel, "Imported");
                            });
                        } catch (IOException | ConfigurationException ex) {
                            SwingUtilities.invokeLater(() -> {
                                String errorMsg = "Error: " + ex.getMessage();
                                JPanel errorPanel = getErrorPanel(errorMsg, gbc, () -> {
                                    pathField.setText("");
                                    comboBox.setSelectedIndex(0);
                                    errorMessage.setText("");
                                    okButton.setEnabled(true);
                                    CardLayout cl = (CardLayout) mainPanel.getLayout();
                                    cl.show(mainPanel, "Input");  // Reset to input panel
                                });
                                mainPanel.add(errorPanel, "Error");
                                CardLayout cl = (CardLayout) mainPanel.getLayout();
                                cl.show(mainPanel, "Error");
                            });
                        }
                    }).start();
                } else {
                    errorMessage.setText("The specified path does not point to a valid file.");
                }
            } else {
                errorMessage.setText("Please enter a file path.");
            }
        });

        // Reset button action to revert to the original input panel
        resetButton.addActionListener(evt -> {
            pathField.setText("");  // Clear input field
            comboBox.setSelectedIndex(0);  // Reset language selection
            errorMessage.setText("");  // Clear error messages
            okButton.setEnabled(true);  // Re-enable the button
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "Input");  // Switch back to input panel
        });

        return mainPanel;
    }

    private static JPanel createImportBasedataTranslationsPanel() {
        JPanel mainPanel = new JPanel(new CardLayout());

        // Original input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel informationField = new JLabel("Please enter the path to your file, project and the language you wish to import");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        inputPanel.add(informationField, gbc);

        JLabel fileFormatRule = new JLabel("The file must have the format \"ID,Locale,Translation\" in order to be imported.");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        inputPanel.add(fileFormatRule, gbc);

        JTextField pathField = getPathField(gbc, 2, inputPanel);

        JComboBox<String> projectComboBox = getProjectComboBox(gbc, 3, inputPanel, true);

        JLabel inputErrorMessage = getErrorMessageLabel(gbc, 4, inputPanel);

        JComboBox<LocaleWithEncoding> languageComboBox = getLanguageComboBox(gbc, 5, inputPanel);

        JCheckBox includeProductCheckBox = new JCheckBox();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Project and product import:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        inputPanel.add(includeProductCheckBox, gbc);

        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JLabel warning = new JLabel("Attention: This option may lead to errors or wrong imports!");
        warning.setForeground(Color.RED);
        inputPanel.add(warning, gbc);

        JButton okButton = getOkButton(gbc, 7, inputPanel);

        JPanel importingPanel = getLoadingPanel("Importing...", gbc);

        JPanel importedPanel = getSuccessPanel("Imported successfully!", gbc);

        JButton resetButton = getResetButton(gbc, 2, importedPanel);

        // Adding all panels to main panel
        mainPanel.add(inputPanel, "Input");
        mainPanel.add(importedPanel, "Imported");
        mainPanel.add(importingPanel, "Importing");

        // OK button action listener
        okButton.addActionListener(e -> {
            // Get input and start import
            final String filePath = pathField.getText();
            final LocaleWithEncoding language = (LocaleWithEncoding) languageComboBox.getSelectedItem();
            final String project = (String) projectComboBox.getSelectedItem();
            final boolean includeProduct = includeProductCheckBox.isSelected();
            inputErrorMessage.setText("");  // Clear previous messages

            if (!filePath.isEmpty() && project != null && language != null) {
                if (validateFilePath(filePath)) {
                    // Disable button and switch to importing panel
                    okButton.setEnabled(false);
                    CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
                    cardLayout.show(mainPanel, "Importing");

                    // Start importing
                    new Thread(() -> {
                        try {
                            executeBasedataTranslationsImport(Paths.get(filePath), language, project, includeProduct);
                            SwingUtilities.invokeLater(() -> {
                                CardLayout cl = (CardLayout) mainPanel.getLayout();
                                cl.show(mainPanel, "Imported");
                            });
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                String errorMsg = "Error: " + ex.getMessage();
                                JPanel errorPanel = getErrorPanel(errorMsg, gbc, () -> {
                                    pathField.setText("");
                                    languageComboBox.setSelectedIndex(0);
                                    inputErrorMessage.setText("");
                                    okButton.setEnabled(true);
                                    CardLayout cl = (CardLayout) mainPanel.getLayout();
                                    cl.show(mainPanel, "Input");
                                });
                                mainPanel.add(errorPanel, "Error");
                                CardLayout cl = (CardLayout) mainPanel.getLayout();
                                cl.show(mainPanel, "Error");
                            });
                        }
                    }).start();
                } else {
                    inputErrorMessage.setText("The specified path does not point to a valid file.");
                }
            } else {
                inputErrorMessage.setText("Please enter a file path and a project.");
            }
        });

        // Reset button action to revert to the original input panel
        resetButton.addActionListener(evt -> {
            pathField.setText("");  // Clear the input field
            languageComboBox.setSelectedIndex(0);  // Reset the language selection
            inputErrorMessage.setText("");  // Clear error messages
            okButton.setEnabled(true);  // Re-enable the button
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "Input");  // Switch back to input panel
        });
        return mainPanel;
    }

    private static JPanel machineTranslationPanel() {
        JPanel mainPanel = new JPanel(new CardLayout());

        // Original input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel informationField = new JLabel("Please enter the language and project you wish to translate");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        inputPanel.add(informationField, gbc);

        JComboBox<String> projectComboBox = getProjectComboBox(gbc, 2, inputPanel, false);

        JLabel errorMessageLabel = getErrorMessageLabel(gbc, 3, inputPanel);

        JComboBox<LocaleWithEncoding> comboBox = getLanguageComboBox(gbc, 4, inputPanel);

        JButton okButton = getOkButton(gbc, 5, inputPanel);
        final long[] deeplApiLimit = { 0 };
        try {
            deeplApiLimit[0] = deeplApiLimit();
        } catch (DeepLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        JLabel deeplApiCharsLeft = new JLabel("Characters left: " + deeplApiLimit[0]);
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        inputPanel.add(deeplApiCharsLeft, gbc);

        // Adding input panel to main panel
        mainPanel.add(inputPanel, "Input");

        JPanel translatingPanel = getLoadingPanel("Translating...", gbc);

        JPanel translatedPanel = getSuccessPanel("Translated successfully!", gbc);

        JButton resetButton = getResetButton(gbc, 2, translatedPanel);

        // Adding importing and imported panel to main panel
        mainPanel.add(translatingPanel, "Translating");
        mainPanel.add(translatedPanel, "Translated");

        // OK button action listener
        okButton.addActionListener(e -> {
            // Get input and start import
            final LocaleWithEncoding language = (LocaleWithEncoding) comboBox.getSelectedItem();
            final String project = (String) projectComboBox.getSelectedItem();
            errorMessageLabel.setText("");  // Clear previous messages

            // Disable button and switch to importing panel
            okButton.setEnabled(false);
            CardLayout cardLayout = (CardLayout) mainPanel.getLayout();
            cardLayout.show(mainPanel, "Translating");

            // Start importing
            new Thread(() -> {
                try {
                    executeFindAndTranslate(project, language);
                    SwingUtilities.invokeLater(() -> {
                        // Switch to success message
                        CardLayout cl = (CardLayout) mainPanel.getLayout();
                        cl.show(mainPanel, "Translated");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = "Error: " + ex.getMessage();
                        JPanel errorPanel = getErrorPanel(errorMsg, gbc, () -> {
                            comboBox.setSelectedIndex(0);
                            errorMessageLabel.setText("");
                            okButton.setEnabled(true);
                            CardLayout cl = (CardLayout) mainPanel.getLayout();
                            cl.show(mainPanel, "Input");
                        });
                        mainPanel.add(errorPanel, "Error");
                        CardLayout cl = (CardLayout) mainPanel.getLayout();
                        cl.show(mainPanel, "Error");
                    });
                }
            }).start();
        });


        // Reset button action to revert to the original input panel
        resetButton.addActionListener(evt -> {
            comboBox.setSelectedIndex(0);  // Reset the language selection
            errorMessageLabel.setText("");  // Clear error messages
            okButton.setEnabled(true);  // Re-enable the button

            // Update the API limit and label in a cleaner way
            updateDeeplApiCharsLeft(deeplApiCharsLeft, deeplApiLimit);

            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "Input");  // Switch back to input panel
        });

        return mainPanel;
    }

    // Error panel to display exceptions
    private static JPanel getErrorPanel(String errorMessage, GridBagConstraints gbc, Runnable resetAction) {
        JPanel errorPanel = new JPanel(new GridBagLayout());

        // Text area for displaying error messages
        JTextArea errorTextArea = new JTextArea(10, 40);
        errorTextArea.setText(errorMessage);
        errorTextArea.setEditable(false);  // Make it read-only
        errorTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));  // Use monospaced font for a "code-like" look

        // Add the text area to a scroll pane
        JScrollPane scrollPane = new JScrollPane(errorTextArea);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        errorPanel.add(scrollPane, gbc);

        // "OK" button to reset the GUI
        JButton okButton = new JButton("OK");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        errorPanel.add(okButton, gbc);

        // Add action listener to reset the GUI when "OK" is pressed
        okButton.addActionListener(e -> resetAction.run());

        return errorPanel;
    }

    // Loading panel
    private static JPanel getLoadingPanel(String text, GridBagConstraints gbc) {

        JPanel loadingPanel = new JPanel(new GridBagLayout());
        JLabel loadingLabel = new JLabel(text);
        gbc.gridx = 0;
        gbc.gridy = 0;
        loadingPanel.add(loadingLabel, gbc);

        return loadingPanel;
    }

    // Success panel
    private static JPanel getSuccessPanel(String text, GridBagConstraints gbc) {

        JPanel translatedPanel = new JPanel(new GridBagLayout());
        JLabel successLabel = new JLabel(text);
        gbc.gridy = 1;
        translatedPanel.add(successLabel, gbc);

        return translatedPanel;
    }

    // Textfield for the Path
    private static JTextField getPathField(GridBagConstraints gbc, int gridy, JPanel panel) {

        JTextField pathField = new JTextField(30);
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Path:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(pathField, gbc);

        return pathField;
    }

    // LanguagesConstant dropdown menu
    private static JComboBox<LocaleWithEncoding> getLanguageComboBox(GridBagConstraints gbc, int gridy, JPanel panel) {

        JComboBox<LocaleWithEncoding> comboBox = new JComboBox<>(LocaleWithEncoding.values());
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        panel.add(new JLabel("LanguagesConstant:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(comboBox, gbc);

        return comboBox;
    }

    // Project dropdown menu
    private static JComboBox<String> getProjectComboBox(GridBagConstraints gbc, int gridy, JPanel panel, boolean allOption) {

        Vector<String> projects = new Vector<>(getProjectByModuleName(true).keySet());
        // add nothing option
        projects.add("");
        Collections.sort(projects);
        if (allOption) {
            projects.add("ALL");
        }

        JComboBox<String> projectComboBox = new JComboBox<>(projects);
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Project:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(projectComboBox, gbc);

        return projectComboBox;
    }

    // Error message label
    private static JLabel getErrorMessageLabel(GridBagConstraints gbc, int gridy, JPanel panel) {

        JLabel errorMessage = new JLabel();
        errorMessage.setForeground(Color.RED);
        gbc.gridx = 1;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(errorMessage, gbc);

        return errorMessage;
    }

    // OK button
    private static JButton getOkButton(GridBagConstraints gbc, int gridy, JPanel panel) {

        JButton okButton = new JButton("OK");
        gbc.gridx = 1;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(okButton, gbc);

        return okButton;
    }

    // Reset Button
    private static JButton getResetButton(GridBagConstraints gbc, int gridy, JPanel panel) {

        JButton resetButton = new JButton("OK");
        gbc.gridy = gridy;
        panel.add(resetButton, gbc);

        return resetButton;
    }

    // Method to update Deepl API limit and label
    private static void updateDeeplApiCharsLeft(JLabel deeplApiCharsLeft, long[] deeplApiLimit) {
        try {
            deeplApiLimit[0] = deeplApiLimit();  // Fetch updated limit
            deeplApiCharsLeft.setText("Characters left: " + deeplApiLimit[0]);  // Update label
        } catch (DeepLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
