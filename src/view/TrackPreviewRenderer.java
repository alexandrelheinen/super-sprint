package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import model.Circuit;
import view.theme.GameTheme;
import view.ui.UiPainter;

public final class TrackPreviewRenderer {

	private static final int INNER_RADIUS_RATIO = 26;
	private static final int OUTER_RADIUS_RATIO = 191;
	private static final int TILE_UNIT = 219;
	private static final float TRACK_STROKE = 14f;
	private static final float OPEN_ALPHA = 0.18f;

	private TrackPreviewRenderer() {
	}

	public static BufferedImage render(int[][] trackMap, int width, int height) {
		int rows = trackMap.length;
		int columns = trackMap[0].length;
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

		double cellWidth = width / (double) columns;
		double cellHeight = height / (double) rows;
		double innerRadius = (INNER_RADIUS_RATIO / (double) TILE_UNIT) * Math.min(cellWidth, cellHeight);
		double outerRadius = (OUTER_RADIUS_RATIO / (double) TILE_UNIT) * Math.min(cellWidth, cellHeight);
		double centerRadius = (innerRadius + outerRadius) / 2.0;

		graphics.setStroke(new BasicStroke(TRACK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				drawTile(
						graphics,
						trackMap[row][column],
						column * cellWidth,
						row * cellHeight,
						cellWidth,
						cellHeight,
						innerRadius,
						outerRadius,
						centerRadius);
			}
		}

		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(1, 1, width - 2, height - 2, 14, 14);
		graphics.dispose();
		return image;
	}

	private static void drawTile(
			Graphics2D graphics,
			int tileType,
			double originX,
			double originY,
			double cellWidth,
			double cellHeight,
			double innerRadius,
			double outerRadius,
			double centerRadius) {
		if (tileType == Circuit.TILE_OPEN) {
			return;
		}

		double centerX = originX + cellWidth / 2.0;
		double centerY = originY + cellHeight / 2.0;

		switch (tileType) {
			case Circuit.TILE_STRAIGHT_HORIZONTAL -> graphics.draw(new Rectangle2D.Double(
					originX + innerRadius,
					centerY - centerRadius / 4.0,
					cellWidth - innerRadius * 2.0,
					centerRadius / 2.0));
			case Circuit.TILE_STRAIGHT_VERTICAL -> graphics.draw(new Rectangle2D.Double(
					centerX - centerRadius / 4.0,
					originY + innerRadius,
					centerRadius / 2.0,
					cellHeight - innerRadius * 2.0));
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> drawCornerArc(
					graphics, originX, originY + cellHeight, innerRadius, outerRadius, 180, 90);
			case Circuit.TILE_CORNER_TOP_RIGHT -> drawCornerArc(
					graphics, originX + cellWidth, originY + cellHeight, innerRadius, outerRadius, 270, 90);
			case Circuit.TILE_CORNER_TOP_LEFT -> drawCornerArc(
					graphics, originX + cellWidth, originY, innerRadius, outerRadius, 0, 90);
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> drawCornerArc(
					graphics, originX, originY, innerRadius, outerRadius, 90, 90);
			default -> {
			}
		}
	}

	private static void drawCornerArc(
			Graphics2D graphics,
			double cornerX,
			double cornerY,
			double innerRadius,
			double outerRadius,
			int startAngle,
			int extent) {
		double midRadius = (innerRadius + outerRadius) / 2.0;
		Arc2D arc = new Arc2D.Double(
				cornerX - midRadius,
				cornerY - midRadius,
				midRadius * 2.0,
				midRadius * 2.0,
				startAngle,
				extent,
				Arc2D.OPEN);
		graphics.draw(arc);
	}
}
