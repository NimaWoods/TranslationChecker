package com.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Map;

public class FileWarningDialog {

    public static void show(Map<Path, String> unreadableFiles) {
        String[] columnNames = {"File", "Error"};
        Object[][] data = new Object[unreadableFiles.size()][2];

        int i = 0;
        for (Map.Entry<Path, String> entry : unreadableFiles.entrySet()) {
            data[i][0] = entry.getKey().toString();
            data[i][1] = entry.getValue();
            i++;
        }

        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 200));

        JOptionPane.showMessageDialog(null, scrollPane, "Unreadable Files", JOptionPane.WARNING_MESSAGE);
    }

}
