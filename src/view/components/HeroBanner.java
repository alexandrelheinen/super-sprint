package view.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import view.UiScale;
import view.theme.GameTheme;
import view.ui.UiPainter;

public class HeroBanner extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int MIN_HEIGHT = 220;
	private static final int TITLE_FONT_SIZE = 34;
	private static final int SUBTITLE_OFFSET = 34;

	private final Component context;
	private final String title;
	private final String subtitle;

	public HeroBanner(Component context, String title, String subtitle) {
		this.context = context;
		this.title = title;
		this.subtitle = subtitle;
		setOpaque(false);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width, UiScale.scale(context, MIN_HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);
		int centerX = getWidth() / 2;
		int titleBaseline = getHeight() / 2 - 8;
		UiPainter.paintTitleGlow(
				graphics2D,
				title,
				centerX,
				titleBaseline,
				UiScale.scale(context, TITLE_FONT_SIZE));
		graphics2D.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, context));
		graphics2D.setColor(GameTheme.TEXT_MUTED);
		var metrics = graphics2D.getFontMetrics();
		int subtitleWidth = metrics.stringWidth(subtitle);
		graphics2D.drawString(subtitle, centerX - subtitleWidth / 2, titleBaseline + UiScale.scale(context, SUBTITLE_OFFSET));
		graphics2D.dispose();
	}
}
