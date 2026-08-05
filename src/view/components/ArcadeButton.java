package view.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

import view.theme.GameTheme;

public class ArcadeButton extends JButton {

	private static final long serialVersionUID = 1L;
	private final boolean primary;

	public ArcadeButton(String text) {
		this(text, true);
	}

	public ArcadeButton(String text, boolean primary) {
		super(text);
		this.primary = primary;
		setFocusPainted(false);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setForeground(GameTheme.TEXT_PRIMARY);
		setFont(GameTheme.FONT_BUTTON);
	}

	public void applyScaledSize(java.awt.Component context, int width, int height) {
		setFont(GameTheme.scaled(getFont(), context));
		setPreferredSize(new Dimension(
				view.UiScale.scale(context, width),
				view.UiScale.scale(context, height)));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int arc = 18;
		Color top = primary ? GameTheme.ACCENT_BLUE_BRIGHT : GameTheme.PANEL_SURFACE;
		Color bottom = primary ? GameTheme.ACCENT_BLUE : GameTheme.BORDER_SOFT;
		if (getModel().isPressed()) {
			top = bottom.darker();
			bottom = top;
		} else if (getModel().isRollover()) {
			top = top.brighter();
		}

		graphics2D.setColor(bottom);
		graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
		graphics2D.setColor(top);
		graphics2D.fillRoundRect(0, 0, getWidth(), getHeight() - 3, arc, arc);
		graphics2D.setColor(GameTheme.BORDER_SOFT);
		graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

		Font font = getFont();
		graphics2D.setFont(font);
		graphics2D.setColor(getForeground());
		var metrics = graphics2D.getFontMetrics();
		int textX = (getWidth() - metrics.stringWidth(getText())) / 2;
		int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics2D.drawString(getText(), textX, textY);
		graphics2D.dispose();
	}
}
