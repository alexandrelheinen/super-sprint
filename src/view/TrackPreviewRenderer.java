package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import model.Circuit;
import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Renders a smooth track outline for menu previews using cubic splines through
 * ordered tile centerline waypoints with endpoint tangent constraints.
 */
public final class TrackPreviewRenderer {

	private static final int INNER_RADIUS_RATIO = Circuit.INNER_RADIUS;
	private static final int OUTER_RADIUS_RATIO = Circuit.OUTER_RADIUS;
	private static final int TILE_UNIT = 219;
	private static final float TRACK_STROKE = 5f;
	private static final float PADDING_RATIO = 0.12f;
	private static final float OPEN_ALPHA = 0.18f;
	private static final float SPLINE_TENSION = 6f;

	private TrackPreviewRenderer() {
	}

	public static BufferedImage render(int[][] trackMap, int width, int height) {
		double[][] centerline = TrackPreviewPathBuilder.buildOrderedCenterline(trackMap);
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

		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (double[] point : centerline) {
			minX = Math.min(minX, point[0]);
			minY = Math.min(minY, point[1]);
			maxX = Math.max(maxX, point[0]);
			maxY = Math.max(maxY, point[1]);
		}

		double spanX = Math.max(maxX - minX, 1e-3);
		double spanY = Math.max(maxY - minY, 1e-3);
		double drawableWidth = width * (1.0 - 2.0 * PADDING_RATIO);
		double drawableHeight = height * (1.0 - 2.0 * PADDING_RATIO);
		double scale = Math.min(drawableWidth / spanX, drawableHeight / spanY);

		Path2D trackPath = buildConstrainedCubicSpline(centerline, minX, minY, scale, width, height);
		graphics.setStroke(new BasicStroke(TRACK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);
		graphics.draw(trackPath);

		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(1, 1, width - 2, height - 2, 14, 14);
		graphics.dispose();
		return image;
	}

	private static Path2D buildConstrainedCubicSpline(
			double[][] points,
			double minX,
			double minY,
			double scale,
			int width,
			int height) {
		int count = points.length;
		Path2D path = new Path2D.Double();
		if (count == 0) {
			return path;
		}
		if (count == 1) {
			double[] mapped = map(points[0], minX, minY, scale, width, height);
			path.moveTo(mapped[0], mapped[1]);
			return path;
		}

		double[][] mapped = new double[count][];
		for (int index = 0; index < count; index++) {
			mapped[index] = map(points[index], minX, minY, scale, width, height);
		}

		path.moveTo(mapped[0][0], mapped[0][1]);
		for (int index = 0; index < count; index++) {
			int nextIndex = (index + 1) % count;
			int previousIndex = (index - 1 + count) % count;
			int afterNextIndex = (index + 2) % count;

			double[] previous = mapped[previousIndex];
			double[] current = mapped[index];
			double[] next = mapped[nextIndex];
			double[] afterNext = mapped[afterNextIndex];

			double controlOneX;
			double controlOneY;
			double controlTwoX;
			double controlTwoY;
			if (index == 0) {
				controlOneX = current[0] + (next[0] - current[0]) / 3.0;
				controlOneY = current[1] + (next[1] - current[1]) / 3.0;
			} else {
				controlOneX = current[0] + (next[0] - previous[0]) / SPLINE_TENSION;
				controlOneY = current[1] + (next[1] - previous[1]) / SPLINE_TENSION;
			}
			if (nextIndex == 0) {
				controlTwoX = next[0] - (next[0] - current[0]) / 3.0;
				controlTwoY = next[1] - (next[1] - current[1]) / 3.0;
			} else {
				controlTwoX = next[0] - (afterNext[0] - current[0]) / SPLINE_TENSION;
				controlTwoY = next[1] - (afterNext[1] - current[1]) / SPLINE_TENSION;
			}

			path.curveTo(controlOneX, controlOneY, controlTwoX, controlTwoY, next[0], next[1]);
		}
		return path;
	}

	private static double[] map(double[] point, double minX, double minY, double scale, int width, int height) {
		double paddingX = width * PADDING_RATIO;
		double paddingY = height * PADDING_RATIO;
		double drawableWidth = width - paddingX * 2.0;
		double drawableHeight = height - paddingY * 2.0;
		double spanX = drawableWidth / scale;
		double spanY = drawableHeight / scale;
		double alignX = paddingX + (drawableWidth - spanX * scale) / 2.0;
		double alignY = paddingY + (drawableHeight - spanY * scale) / 2.0;

		return new double[] {
				alignX + (point[0] - minX) * scale,
				alignY + (point[1] - minY) * scale
		};
	}
}
