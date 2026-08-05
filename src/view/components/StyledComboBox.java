package view.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import view.theme.GameTheme;

public final class StyledComboBox {

	private StyledComboBox() {
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void apply(javax.swing.JComboBox comboBox, Component context) {
		comboBox.setFont(GameTheme.scaled(GameTheme.FONT_BODY, context));
		comboBox.setBackground(GameTheme.PANEL_SURFACE);
		comboBox.setForeground(GameTheme.TEXT_PRIMARY);
		comboBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(GameTheme.ACCENT_BLUE, 1),
				new EmptyBorder(6, 10, 6, 10)));
		comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
			javax.swing.JLabel label = new javax.swing.JLabel(String.valueOf(value));
			label.setOpaque(true);
			label.setFont(GameTheme.scaled(GameTheme.FONT_BODY, context));
			label.setBorder(new EmptyBorder(6, 10, 6, 10));
			if (isSelected) {
				label.setBackground(GameTheme.ACCENT_BLUE);
				label.setForeground(GameTheme.TEXT_PRIMARY);
			} else {
				label.setBackground(GameTheme.PANEL_SURFACE);
				label.setForeground(GameTheme.TEXT_PRIMARY);
			}
			return label;
		});
	}
}
