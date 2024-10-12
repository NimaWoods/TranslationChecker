package com.gui.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class FileWarningDialog {

    public static void show(Map<String, String> unreadableFiles, String title) {
        String[] columnNames = {"File", "Error"};
        Object[][] data = new Object[unreadableFiles.size()][2];

        int i = 0;
        for (Map.Entry<String, String> entry : unreadableFiles.entrySet()) {
            data[i][0] = entry.getKey();
            data[i][1] = entry.getValue();
            i++;
        }

        // Verwende die Factory, um die Tabelle und das ScrollPane zu erstellen
        JTable table = UIComponentFactory.createTable(new DefaultTableModel(data, columnNames));
        JScrollPane scrollPane = UIComponentFactory.createScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 200));

        JOptionPane.showMessageDialog(null, scrollPane, title, JOptionPane.WARNING_MESSAGE);
    }
}