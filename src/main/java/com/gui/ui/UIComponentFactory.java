package com.gui.ui;

import com.gui.contsants.DesignColorsConstant;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class UIComponentFactory {

	public static JButton createButton(String text) {
		JButton button = new JButton(text);
		button.setBackground(DesignColorsConstant.MATERIAL_BLUE.getColor());
		button.setForeground(DesignColorsConstant.WHITE.getColor());
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		return button;
	}

	public static JTextField createTextField(String initialText) {
		JTextField textField = new JTextField(initialText);
		textField.setBorder(BorderFactory.createLineBorder(DesignColorsConstant.DARK_GRAY.getColor()));  // Rahmenfarbe
		textField.setBackground(DesignColorsConstant.LIGHT_GRAY.getColor());
		textField.setPreferredSize(new Dimension(200, 30));
		return textField;
	}

	// Methode zum Erstellen einer Standard-Checkbox
	public static JCheckBox createCheckBox(String label, boolean isSelected) {
		JCheckBox checkBox = new JCheckBox(label, isSelected);
		return checkBox;
	}

	// Methode zum Erstellen eines Standard-Labels
	public static JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		return label;
	}

	// Methode zum Erstellen einer Tabelle mit Design
	public static JTable createTable(DefaultTableModel model) {
		JTable table = new JTable(model);
		table.setShowGrid(true);
		table.setGridColor(DesignColorsConstant.GRID_COLOR.getColor());
		table.setRowHeight(25);
		table.setSelectionBackground(DesignColorsConstant.MATERIAL_PINK.getColor());
		table.setSelectionForeground(DesignColorsConstant.BLACK.getColor());
		return table;
	}

	// Methode zum Erstellen eines ScrollPanes für eine Tabelle
	public static JScrollPane createScrollPane(JTable table) {
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));  // Rand um das ScrollPane
		return scrollPane;
	}

	// Methode zum Erstellen eines Standard-Tabellen-Headers
	public static JTableHeader createTableHeader(JTable table) {
		JTableHeader header = table.getTableHeader();
		header.setFont(new Font("SansSerif", Font.BOLD, 14));
		header.setBackground(DesignColorsConstant.MATERIAL_BLUE.getColor());  // Material Blau aus Enum
		header.setForeground(DesignColorsConstant.WHITE.getColor());  // Weißer Text
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, DesignColorsConstant.HEADER_BORDER_COLOR.getColor()));  // Randfarbe aus Enum
		return header;
	}
}