package view.components;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import view.theme.GameTheme;

public class KeyboardPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public enum Layout {
		ARROWS,
		WASD
	}

	public KeyboardPanel(Component context, Layout layout) {
		setOpaque(false);
		setLayout(new GridBagLayout());

		if (layout == Layout.ARROWS) {
			add(new KeyCap(context, "↑"), grid(1, 1, 1, 1));
			add(new KeyCap(context, "←"), grid(0, 2, 1, 1));
			add(new KeyCap(context, "↓"), grid(1, 2, 1, 1));
			add(new KeyCap(context, "→"), grid(2, 2, 1, 1));
		} else {
			add(new KeyCap(context, "W"), grid(1, 1, 1, 1));
			add(new KeyCap(context, "A"), grid(0, 2, 1, 1));
			add(new KeyCap(context, "S"), grid(1, 2, 1, 1));
			add(new KeyCap(context, "D"), grid(2, 2, 1, 1));
		}
	}

	private static GridBagConstraints grid(int x, int y, int width, int height) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		constraints.gridheight = height;
		constraints.insets = new java.awt.Insets(4, 4, 4, 4);
		return constraints;
	}
}
