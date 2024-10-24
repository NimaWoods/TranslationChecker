package com.gui.contsants;

import java.awt.*;

public enum DesignColorsConstant {

	MATERIAL_BLUE(new Color(21, 92, 73)),
	MATERIAL_PINK(new Color(244, 196, 201)),
	LIGHT_GRAY(new Color(245, 245, 245)),
	DARK_GRAY(new Color(128, 128, 128)),
	WHITE(Color.WHITE),
	BLACK(Color.BLACK),
	GRID_COLOR(new Color(192, 192, 192)),
	HEADER_BORDER_COLOR(new Color(192, 192, 192));

	private final Color color;

	DesignColorsConstant(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

}
