package view.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

import view.UiScale;
import view.theme.GameTheme;
import view.ui.UiPainter;

public class KeyCap extends JComponent {

	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_SIZE = 46;
	private static final int CORNER_ARC = 10;

	private final Component context;
	private final String label;

	public KeyCap(Component context, String label) {
		this.context = context;
		this.label = label;
		setOpaque(false);
	}

	@Override
	public Dimension getPreferredSize() {
		int size = UiScale.scale(context, DEFAULT_SIZE);
		return new Dimension(size, size);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);
		graphics2D.setPaint(new GradientPaint(
				0,
				0,
				GameTheme.ACCENT_BLUE_BRIGHT,
				0,
				getHeight(),
				GameTheme.ACCENT_BLUE));
		graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_ARC, CORNER_ARC);
		graphics2D.setColor(GameTheme.GLASS_BORDER);
		graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_ARC, CORNER_ARC);
		Font font = GameTheme.scaled(GameTheme.FONT_BUTTON, context);
		graphics2D.setFont(font);
		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		var metrics = graphics2D.getFontMetrics();
		int textX = (getWidth() - metrics.stringWidth(label)) / 2;
		int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics2D.drawString(label, textX, textY);
		graphics2D.dispose();
	}
}
