package view.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class BackgroundPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public enum Style {
		MENU,
		SCREEN
	}

	private final Style style;

	public BackgroundPanel(Style style) {
		this.style = style;
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		if (style == Style.MENU) {
			UiPainter.paintMenuBackdrop(graphics2D, getWidth(), getHeight());
		} else {
			UiPainter.paintScreenBackdrop(graphics2D, getWidth(), getHeight());
		}
		graphics2D.dispose();
		super.paintComponent(graphics);
	}
}
