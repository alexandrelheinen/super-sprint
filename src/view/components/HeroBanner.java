package view.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
 * dark scrim at the top and the game title overlaid near the top edge.
 */
public class HeroBanner extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int MIN_HEIGHT = 260;
	/** Main menu hero title - larger than the global theme title size. */
	private static final int TITLE_FONT_SIZE = 58;
	/** Brand line under the title (e.g. Supélec). */
	private static final int BRAND_FONT_SIZE = 32;
	/** Empty space above SUPER SPRINT, in title line-heights. */
	private static final int TITLE_TOP_MARGIN_LINES = 1;
	/** Clearance between the title underline and the top of the brand glyphs. */
	private static final int BRAND_CLEARANCE = 16;
	private static final int CORNER_ARC = 28;
	private static final double SCRIM_END_RATIO = 0.55;
	private static final int SCRIM_MAX_ALPHA = 235;

	private static final String SPLASH_SPRITE = "splash.png";
	private static final String ERROR_SPLASH = "Error loading splash artwork: ";

	private final Component context;
	private final String title;
	private final String brandLine;
	private final BufferedImage splashImage;

	public HeroBanner(Component context, String title, String brandLine) {
		this.context = context;
		this.title = title;
		this.brandLine = brandLine;
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
		int titleFontSize = UiScale.scale(context, TITLE_FONT_SIZE);
		Font titleFont = GameTheme.FONT_TITLE.deriveFont(Font.BOLD, (float) titleFontSize);
		var titleMetrics = graphics2D.getFontMetrics(titleFont);
		// One title line-height of empty margin above the capitals, then the baseline.
		int titleBaseline = splashImage != null
				? TITLE_TOP_MARGIN_LINES * titleMetrics.getHeight() + titleMetrics.getAscent()
				: getHeight() / 2 - 8;
		int underlineBottom = UiPainter.paintTitleGlow(
				graphics2D,
				title,
				centerX,
				titleBaseline,
				titleFontSize);

		if (brandLine != null && !brandLine.isEmpty()) {
			int brandFontSize = UiScale.scale(context, BRAND_FONT_SIZE);
			Font brandFont = GameTheme.FONT_SUBTITLE.deriveFont(Font.BOLD, (float) brandFontSize);
			graphics2D.setFont(brandFont);
			graphics2D.setColor(GameTheme.TEXT_PRIMARY);
			var metrics = graphics2D.getFontMetrics();
			int brandWidth = metrics.stringWidth(brandLine);
			// Place below the yellow underline with room for accents (é).
			int brandBaseline = underlineBottom
					+ UiScale.scale(context, BRAND_CLEARANCE)
					+ metrics.getAscent();
			graphics2D.drawString(brandLine, centerX - brandWidth / 2, brandBaseline);
		}
		graphics2D.dispose();
	}

	/**
	 * Draws the splash image scaled to cover the banner (cropping overflow),
	 * clipped to a rounded frame, with a top gradient scrim so the
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

		int scrimBottom = (int) (height * SCRIM_END_RATIO);
		graphics2D.setPaint(new GradientPaint(
				0,
				0,
				new Color(
						GameTheme.BACKGROUND_DARK.getRed(),
						GameTheme.BACKGROUND_DARK.getGreen(),
						GameTheme.BACKGROUND_DARK.getBlue(),
						SCRIM_MAX_ALPHA),
				0,
				scrimBottom,
				new Color(
						GameTheme.BACKGROUND_DARK.getRed(),
						GameTheme.BACKGROUND_DARK.getGreen(),
						GameTheme.BACKGROUND_DARK.getBlue(),
						0)));
		graphics2D.fillRect(0, 0, width, scrimBottom);

		graphics2D.setClip(previousClip);
		graphics2D.setColor(GameTheme.GLASS_BORDER);
		graphics2D.draw(new RoundRectangle2D.Float(0, 0, width - 1f, height - 1f, arc, arc));
		graphics2D.setColor(GameTheme.ACCENT_YELLOW);
		graphics2D.fillRoundRect(UiScale.scale(context, 14), 0, width - UiScale.scale(context, 28), 3, 2, 2);
	}
}
