package view.components;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

import view.UiScale;
import view.theme.GameTheme;

public final class StyledComboBox {

	private static final int COMBO_HEIGHT = 36;
	/** Fallback width hint when callers do not supply a prototype. */
	private static final String DEFAULT_PROTOTYPE = "A-Type";

	private StyledComboBox() {
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void apply(javax.swing.JComboBox comboBox, Component context) {
		apply(comboBox, context, DEFAULT_PROTOTYPE);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void apply(javax.swing.JComboBox comboBox, Component context, String prototypeValue) {
		comboBox.setFont(GameTheme.scaled(GameTheme.FONT_BODY, context));
		comboBox.setBackground(GameTheme.PANEL_SURFACE);
		comboBox.setForeground(GameTheme.TEXT_PRIMARY);
		if (prototypeValue != null && !prototypeValue.isEmpty()) {
			comboBox.setPrototypeDisplayValue(prototypeValue);
		}
		comboBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(GameTheme.ACCENT_BLUE, 1),
				new EmptyBorder(6, 10, 6, 10)));
		int height = Math.max(UiScale.scale(context, COMBO_HEIGHT), UiScale.scale(context, 28));
		Dimension size = new Dimension(comboBox.getPreferredSize().width, height);
		comboBox.setPreferredSize(size);
		comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
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
