package view.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import model.ResourcePaths;
import view.UiScale;
import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Main menu hero: the bundled splash artwork framed in a rounded panel with a
 * dark scrim at the bottom and the game title overlaid on top of it.
 */
public class HeroBanner extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int MIN_HEIGHT = 220;
	private static final int TITLE_FONT_SIZE = 42;
	private static final int SUBTITLE_OFFSET = 34;
	private static final int CORNER_ARC = 28;
	private static final int TITLE_BOTTOM_OFFSET = 74;
	private static final double SCRIM_START_RATIO = 0.42;
	private static final int SCRIM_MAX_ALPHA = 235;

	private static final String SPLASH_SPRITE = "splash.png";
	private static final String ERROR_SPLASH = "Error loading splash artwork: ";

	private final Component context;
	private final String title;
	private final String subtitle;
	private final BufferedImage splashImage;

	public HeroBanner(Component context, String title, String subtitle) {
		this.context = context;
		this.title = title;
		this.subtitle = subtitle;
		this.splashImage = loadSplashImage();
		setOpaque(false);
	}

	private static BufferedImage loadSplashImage() {
		try {
			return ImageIO.read(new File(ResourcePaths.bundledSprite(SPLASH_SPRITE)));
		} catch (IOException exception) {
			System.err.println(ERROR_SPLASH + exception.getMessage());
			return null;
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width, UiScale.scale(context, MIN_HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);

		if (splashImage != null) {
			paintSplashArtwork(graphics2D);
		}

		int centerX = getWidth() / 2;
		int titleBaseline = splashImage != null
				? getHeight() - UiScale.scale(context, TITLE_BOTTOM_OFFSET)
				: getHeight() / 2 - 8;
		UiPainter.paintTitleGlow(
				graphics2D,
				title,
				centerX,
				titleBaseline,
				UiScale.scale(context, TITLE_FONT_SIZE));
		graphics2D.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, context));
		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		var metrics = graphics2D.getFontMetrics();
		int subtitleWidth = metrics.stringWidth(subtitle);
		graphics2D.drawString(subtitle, centerX - subtitleWidth / 2, titleBaseline + UiScale.scale(context, SUBTITLE_OFFSET));
		graphics2D.dispose();
	}

	/**
	 * Draws the splash image scaled to cover the banner (cropping overflow),
	 * clipped to a rounded frame, with a bottom gradient scrim so the
	 * overlaid title stays readable.
	 */
	private void paintSplashArtwork(Graphics2D graphics2D) {
		int width = getWidth();
		int height = getHeight();
		int arc = UiScale.scale(context, CORNER_ARC);
		Shape frame = new RoundRectangle2D.Float(0, 0, width, height, arc, arc);
		Shape previousClip = graphics2D.getClip();
		graphics2D.setClip(frame);

		double scale = Math.max(
				width / (double) splashImage.getWidth(),
				height / (double) splashImage.getHeight());
		int drawWidth = (int) Math.ceil(splashImage.getWidth() * scale);
		int drawHeight = (int) Math.ceil(splashImage.getHeight() * scale);
		graphics2D.drawImage(
				splashImage,
				(width - drawWidth) / 2,
				(height - drawHeight) / 2,
				drawWidth,
				drawHeight,
				null);

		int scrimTop = (int) (height * SCRIM_START_RATIO);
		graphics2D.setPaint(new GradientPaint(
				0,
				scrimTop,
				new Color(
						GameTheme.BACKGROUND_DARK.getRed(),
						GameTheme.BACKGROUND_DARK.getGreen(),
						GameTheme.BACKGROUND_DARK.getBlue(),
						0),
				0,
				height,
				new Color(
						GameTheme.BACKGROUND_DARK.getRed(),
						GameTheme.BACKGROUND_DARK.getGreen(),
						GameTheme.BACKGROUND_DARK.getBlue(),
						SCRIM_MAX_ALPHA)));
		graphics2D.fillRect(0, scrimTop, width, height - scrimTop);

		graphics2D.setClip(previousClip);
		graphics2D.setColor(GameTheme.GLASS_BORDER);
		graphics2D.draw(new RoundRectangle2D.Float(0, 0, width - 1f, height - 1f, arc, arc));
		graphics2D.setColor(GameTheme.ACCENT_YELLOW);
		graphics2D.fillRoundRect(UiScale.scale(context, 14), 0, width - UiScale.scale(context, 28), 3, 2, 2);
	}
}
