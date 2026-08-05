package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Renders track menu previews by stroking a tile-accurate outline built from
 * constrained cubic Bézier segments (lines for straights, tangents for corners).
 */
public final class TrackPreviewRenderer {

	private static final float TRACK_STROKE = 5f;
	private static final float PADDING_RATIO = 0.12f;
	private static final float OPEN_ALPHA = 0.18f;

	private TrackPreviewRenderer() {
	}

	public static BufferedImage render(int[][] trackMap, int width, int height) {
		Path2D trackPath = TrackPreviewPathBuilder.buildTrackPath(trackMap);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		UiPainter.enableQuality(graphics);

		graphics.setColor(new Color(0, 0, 0, 0));
		graphics.fillRect(0, 0, width, height);
		graphics.setColor(new Color(
				GameTheme.BACKGROUND_DARK.getRed(),
				GameTheme.BACKGROUND_DARK.getGreen(),
				GameTheme.BACKGROUND_DARK.getBlue(),
				(int) (255 * OPEN_ALPHA)));
		graphics.fillRoundRect(0, 0, width, height, 16, 16);

		Rectangle2D bounds = trackPath.getBounds2D();
		double spanX = Math.max(bounds.getWidth(), 1e-3);
		double spanY = Math.max(bounds.getHeight(), 1e-3);
		double paddingX = width * PADDING_RATIO;
		double paddingY = height * PADDING_RATIO;
		double drawableWidth = width - paddingX * 2.0;
		double drawableHeight = height - paddingY * 2.0;
		double scale = Math.min(drawableWidth / spanX, drawableHeight / spanY);
		double alignX = paddingX + (drawableWidth - spanX * scale) / 2.0;
		double alignY = paddingY + (drawableHeight - spanY * scale) / 2.0;

		graphics.translate(alignX - bounds.getX() * scale, alignY - bounds.getY() * scale);
		graphics.scale(scale, scale);

		graphics.setStroke(new BasicStroke((float) (TRACK_STROKE / scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);
		graphics.draw(trackPath);

		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(1, 1, width - 2, height - 2, 14, 14);
		graphics.dispose();
		return image;
	}
}
