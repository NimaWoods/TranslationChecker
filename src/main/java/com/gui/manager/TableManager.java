package com.gui.manager;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;

import com.gui.TranslationCheckerApp;

public class TableManager {

    private boolean isUpdating = false;

    public void registerTableModelListenerForEditValue(DefaultTableModel model, JTable table) {
        model.addTableModelListener(e -> {
            if (isUpdating || e.getType() != TableModelEvent.UPDATE) {
                return;
            }

            try {
                isUpdating = true;

                // Ensure cell editing has been stopped
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }

                // Get the selected row in the view (after sorting/filtering)
                int selectedRowInView = table.getSelectedRow();

                // Check if a row is selected
                if (selectedRowInView == -1) {
                    return;
                }

                // Convert view row index to model row index
                int modelRow = table.convertRowIndexToModel(selectedRowInView);

                // Retrieve values from the selected row
                String newValue = model.getValueAt(modelRow, 2).toString();  // Spalte "Value"
                String language = model.getValueAt(modelRow, 0).toString();  // Spalte "Language"
                String key = model.getValueAt(modelRow, 1).toString();  // Spalte "Key"
                String filePath = model.getValueAt(modelRow, 3).toString();  // Spalte "File Path"

                // Update the key in the file
                TranslationKeyManager translationKeyManager = new TranslationKeyManager();
                try {
                    translationKeyManager.updateKeyInFile(language, key, newValue, filePath);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                // Update the column value in the current model
                translationKeyManager.updateColumnValue(language, key, newValue, model);

                TranslationCheckerApp app = new TranslationCheckerApp();
                if (app.getTableModel() != model) {
                    translationKeyManager.updateColumnValue(language, key, newValue, app.getTableModel());
                }
            } finally {
                isUpdating = false;
            }
        });
    }
}