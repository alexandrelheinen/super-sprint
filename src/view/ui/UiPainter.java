package view.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import view.theme.GameTheme;

public final class UiPainter {

	private static final int CHECKER_SIZE = 28;
	private static final int CHECKER_ALPHA = 18;
	private static final float STRIPE_STROKE = 1.2f;
	private static final int STRIPE_SPACING = 42;
	private static final int STRIPE_ALPHA = 22;

	private UiPainter() {
	}

	public static void enableQuality(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static void paintMenuBackdrop(Graphics2D graphics, int width, int height) {
		enableQuality(graphics);
		graphics.setPaint(new GradientPaint(
				0,
				0,
				GameTheme.BACKGROUND_TOP,
				0,
				height,
				GameTheme.BACKGROUND_DARK));
		graphics.fillRect(0, 0, width, height);

		paintDiagonalStripes(graphics, width, height);
		paintBottomCheckerFade(graphics, width, height);
		paintTopAccentGlow(graphics, width);
	}

	public static void paintScreenBackdrop(Graphics2D graphics, int width, int height) {
		enableQuality(graphics);
		graphics.setPaint(new GradientPaint(
				0,
				0,
				GameTheme.BACKGROUND_TOP,
				width,
				height,
				GameTheme.BACKGROUND_DARK));
		graphics.fillRect(0, 0, width, height);
		paintDiagonalStripes(graphics, width, height);
		paintTopAccentGlow(graphics, width);
	}

	public static void paintGlassSurface(Graphics2D graphics, int x, int y, int width, int height, int arc) {
		paintGlassSurface(graphics, x, y, width, height, arc, true);
	}

	/**
	 * @param paintTopAccent when false, skips the yellow top bar so ABOVE_TOP
	 *        section titles (Player / Track) are not covered.
	 */
	public static void paintGlassSurface(
			Graphics2D graphics,
			int x,
			int y,
			int width,
			int height,
			int arc,
			boolean paintTopAccent) {
		enableQuality(graphics);
		graphics.setColor(GameTheme.GLASS_FILL);
		graphics.fillRoundRect(x, y, width, height, arc, arc);
		graphics.setColor(GameTheme.GLASS_BORDER);
		graphics.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
		if (paintTopAccent) {
			graphics.setColor(GameTheme.ACCENT_YELLOW);
			graphics.fillRoundRect(x + 12, y, width - 24, 3, 2, 2);
		}
	}

	private static final int TITLE_UNDERLINE_GAP = 8;
	private static final int TITLE_UNDERLINE_HEIGHT = 3;

	/**
	 * Paints the glowing title and its yellow underline. Returns the Y coordinate
	 * just below the underline so callers can place secondary text underneath.
	 */
	public static int paintTitleGlow(Graphics2D graphics, String title, int centerX, int baselineY, int fontSize) {
		enableQuality(graphics);
		graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.BOLD, fontSize));
		int textWidth = graphics.getFontMetrics().stringWidth(title);
		int textX = centerX - textWidth / 2;
		graphics.setColor(new Color(GameTheme.ACCENT_BLUE.getRed(), GameTheme.ACCENT_BLUE.getGreen(), GameTheme.ACCENT_BLUE.getBlue(), 90));
		graphics.drawString(title, textX + 2, baselineY + 2);
		graphics.setColor(GameTheme.TEXT_PRIMARY);
		graphics.drawString(title, textX, baselineY);
		int underlineTop = baselineY + TITLE_UNDERLINE_GAP;
		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.fillRect(centerX - textWidth / 2, underlineTop, textWidth, TITLE_UNDERLINE_HEIGHT);
		return underlineTop + TITLE_UNDERLINE_HEIGHT;
	}

	public static void paintHudStrip(Graphics2D graphics, int x, int y, int width, int height) {
		enableQuality(graphics);
		graphics.setPaint(new GradientPaint(
				0,
				y,
				GameTheme.HUD_BACKGROUND,
				0,
				y + height,
				GameTheme.BACKGROUND_DARK));
		graphics.fillRect(x, y, width, height);
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);
		graphics.fillRect(x, y, width, 2);
		graphics.setColor(GameTheme.GLASS_BORDER);
		graphics.drawLine(x, y, x + width, y);
	}

	public static void paintHudBadge(
			Graphics2D graphics,
			int x,
			int y,
			int width,
			int height,
			String text,
			java.awt.Font font) {
		enableQuality(graphics);
		paintGlassSurface(graphics, x, y, width, height, 14);
		graphics.setFont(font);
		graphics.setColor(GameTheme.TEXT_PRIMARY);
		var metrics = graphics.getFontMetrics();
		int textX = x + (width - metrics.stringWidth(text)) / 2;
		int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics.drawString(text, textX, textY);
	}

	public static void paintRaceViewportFrame(Graphics2D graphics, int x, int y, int width, int height) {
		enableQuality(graphics);
		// Border only — infield/open tiles should show the grass scenery underneath.
		graphics.setColor(GameTheme.GLASS_BORDER);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRect(x - 6, y - 6, width + 12, height + 12);
		graphics.setColor(GameTheme.ACCENT_BLUE);
		graphics.drawRect(x - 2, y - 2, width + 4, height + 4);
	}

	private static void paintDiagonalStripes(Graphics2D graphics, int width, int height) {
		graphics.setColor(new Color(255, 255, 255, STRIPE_ALPHA));
		graphics.setStroke(new BasicStroke(STRIPE_STROKE));
		for (int offset = -height; offset < width + height; offset += STRIPE_SPACING) {
			graphics.drawLine(offset, height, offset + height, 0);
		}
	}

	private static void paintBottomCheckerFade(Graphics2D graphics, int width, int height) {
		int rows = Math.max(4, height / CHECKER_SIZE);
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < width / CHECKER_SIZE + 1; column++) {
				if ((row + column) % 2 != 0) {
					continue;
				}
				int alpha = CHECKER_ALPHA + row * 4;
				graphics.setColor(new Color(255, 255, 255, Math.min(alpha, 60)));
				int y = height - (row + 1) * CHECKER_SIZE;
				graphics.fillRect(column * CHECKER_SIZE, y, CHECKER_SIZE, CHECKER_SIZE);
			}
		}
	}

	private static void paintTopAccentGlow(Graphics2D graphics, int width) {
		graphics.setPaint(new GradientPaint(
				0,
				0,
				new Color(GameTheme.ACCENT_BLUE_BRIGHT.getRed(), GameTheme.ACCENT_BLUE_BRIGHT.getGreen(), GameTheme.ACCENT_BLUE_BRIGHT.getBlue(), 80),
				0,
				90,
				new Color(0, 0, 0, 0)));
		graphics.fillRect(0, 0, width, 90);
	}
}
