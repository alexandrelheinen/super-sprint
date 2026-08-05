package view.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;

import view.UiScale;

/**
 * Compact soft-keyboard cluster on a rigid 2×3 grid so the top key stays
 * perfectly centered over the middle bottom key.
 */
public class KeyboardPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int KEY_GAP = 8;

	public enum Layout {
		ARROWS,
		WASD
	}

	private final int keySize;
	private final int keyGap;

	public KeyboardPanel(Component context, Layout layout, int unscaledKeySize) {
		setOpaque(false);
		keySize = UiScale.scale(context, unscaledKeySize);
		keyGap = UiScale.scale(context, KEY_GAP);
		setLayout(new GridLayout(2, 3, keyGap, keyGap));

		if (layout == Layout.ARROWS) {
			add(spacer(keySize));
			add(sized(KeyCap.arrowUp(context), keySize));
			add(spacer(keySize));
			add(sized(KeyCap.arrowLeft(context), keySize));
			add(sized(KeyCap.arrowDown(context), keySize));
			add(sized(KeyCap.arrowRight(context), keySize));
		} else {
			add(spacer(keySize));
			add(sized(new KeyCap(context, "W"), keySize));
			add(spacer(keySize));
			add(sized(new KeyCap(context, "A"), keySize));
			add(sized(new KeyCap(context, "S"), keySize));
			add(sized(new KeyCap(context, "D"), keySize));
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(3 * keySize + 2 * keyGap, 2 * keySize + keyGap);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	private static JPanel spacer(int size) {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setPreferredSize(new Dimension(size, size));
		panel.setMinimumSize(new Dimension(size, size));
		panel.setMaximumSize(new Dimension(size, size));
		return panel;
	}

	private static KeyCap sized(KeyCap keyCap, int size) {
		keyCap.forceSize(size);
		return keyCap;
	}
}
