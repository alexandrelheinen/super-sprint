package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Renders track menu previews by stroking a tile-accurate outline built from
 * constrained cubic Bézier segments. Scenery (grass + trees) is drawn
 * procedurally — the splash art includes grass/trees, but those are not
 * available as separate sprites.
 */
public final class TrackPreviewRenderer {

	private static final float TRACK_STROKE = 5f;
	private static final float TRACK_ASPHALT_STROKE = 7.5f;
	private static final float PADDING_RATIO = 0.12f;
	private static final int CORNER_ARC = 16;
	private static final int BORDER_ARC = 14;

	/** Grass tones sampled from the splash artwork (~avg 78,115,40). */
	private static final Color GRASS_LIGHT = new Color(96, 132, 48);
	private static final Color GRASS_DARK = new Color(52, 86, 34);
	private static final Color GRASS_PATCH = new Color(68, 104, 38, 90);
	private static final Color TREE_TRUNK = new Color(92, 62, 36);
	private static final Color TREE_CANOPY_DARK = new Color(36, 78, 28);
	private static final Color TREE_CANOPY = new Color(54, 104, 36);
	private static final Color TREE_CANOPY_LIGHT = new Color(78, 128, 48);
	private static final Color ASPHALT = new Color(48, 52, 58);

	private static final int TREE_COUNT = 7;
	private static final double TREE_CLEARANCE_RATIO = 0.55;

	private TrackPreviewRenderer() {
	}

	public static BufferedImage render(int[][] trackMap, int width, int height) {
		Path2D trackPath = TrackPreviewPathBuilder.buildTrackPath(trackMap);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		UiPainter.enableQuality(graphics);

		Shape frame = new RoundRectangle2D.Float(0, 0, width, height, CORNER_ARC, CORNER_ARC);
		graphics.setClip(frame);

		paintGrassField(graphics, width, height, seedFor(trackMap));
		paintGrassPatches(graphics, width, height, seedFor(trackMap) ^ 0x9E3779B9);

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

		AffineTransform trackTransform = new AffineTransform();
		trackTransform.translate(alignX - bounds.getX() * scale, alignY - bounds.getY() * scale);
		trackTransform.scale(scale, scale);

		Shape trackInScreen = trackTransform.createTransformedShape(trackPath);
		Shape trackBand = new BasicStroke(
				TRACK_ASPHALT_STROKE,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND).createStrokedShape(trackInScreen);

		paintTrees(graphics, width, height, trackBand, seedFor(trackMap) ^ 0xA5A5A5A5);

		graphics.setStroke(new BasicStroke(TRACK_ASPHALT_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(ASPHALT);
		graphics.draw(trackInScreen);
		graphics.setStroke(new BasicStroke(TRACK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);
		graphics.draw(trackInScreen);

		graphics.setClip(null);
		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(1, 1, width - 2, height - 2, BORDER_ARC, BORDER_ARC);
		graphics.dispose();
		return image;
	}

	private static void paintGrassField(Graphics2D graphics, int width, int height, int seed) {
		graphics.setPaint(new GradientPaint(
				0,
				0,
				GRASS_LIGHT,
				width,
				height,
				GRASS_DARK));
		graphics.fillRect(0, 0, width, height);

		// Soft vignette toward the edges so the track reads in the center.
		graphics.setPaint(new GradientPaint(
				0,
				0,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 0),
				0,
				height * 0.15f,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 55)));
		graphics.fillRect(0, 0, width, Math.max(1, (int) (height * 0.2f)));
		graphics.setPaint(new GradientPaint(
				0,
				height,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 70),
				0,
				height * 0.8f,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 0)));
		graphics.fillRect(0, (int) (height * 0.75f), width, height);
	}

	private static void paintGrassPatches(Graphics2D graphics, int width, int height, int seed) {
		Random random = new Random(seed);
		int patchCount = Math.max(8, (width * height) / 900);
		for (int index = 0; index < patchCount; index++) {
			double cx = random.nextDouble() * width;
			double cy = random.nextDouble() * height;
			double rw = 4 + random.nextDouble() * 10;
			double rh = 2 + random.nextDouble() * 6;
			graphics.setColor(GRASS_PATCH);
			graphics.fill(new Ellipse2D.Double(cx - rw, cy - rh, rw * 2, rh * 2));
		}
	}

	private static void paintTrees(
			Graphics2D graphics,
			int width,
			int height,
			Shape trackBand,
			int seed) {
		Random random = new Random(seed);
		double minSize = Math.min(width, height) * 0.09;
		double maxSize = Math.min(width, height) * 0.16;
		int placed = 0;
		int attempts = 0;
		while (placed < TREE_COUNT && attempts < TREE_COUNT * 24) {
			attempts++;
			double size = minSize + random.nextDouble() * (maxSize - minSize);
			double x = size * 0.6 + random.nextDouble() * (width - size * 1.2);
			double y = size * 0.7 + random.nextDouble() * (height - size * 1.3);
			Ellipse2D canopy = new Ellipse2D.Double(x - size * 0.55, y - size * 0.7, size * 1.1, size * 0.95);
			if (trackBand.intersects(
					canopy.getX() - size * TREE_CLEARANCE_RATIO,
					canopy.getY() - size * TREE_CLEARANCE_RATIO,
					canopy.getWidth() + size * TREE_CLEARANCE_RATIO * 2,
					canopy.getHeight() + size * TREE_CLEARANCE_RATIO * 2)) {
				continue;
			}
			paintTree(graphics, x, y, size);
			placed++;
		}
	}

	private static void paintTree(Graphics2D graphics, double x, double y, double size) {
		double trunkWidth = size * 0.18;
		double trunkHeight = size * 0.35;
		graphics.setColor(new Color(0, 0, 0, 35));
		graphics.fill(new Ellipse2D.Double(x - size * 0.35, y + size * 0.15, size * 0.7, size * 0.22));

		graphics.setColor(TREE_TRUNK);
		graphics.fill(new RoundRectangle2D.Double(
				x - trunkWidth / 2,
				y - trunkHeight * 0.15,
				trunkWidth,
				trunkHeight,
				trunkWidth * 0.4,
				trunkWidth * 0.4));

		graphics.setColor(TREE_CANOPY_DARK);
		graphics.fill(new Ellipse2D.Double(x - size * 0.5, y - size * 0.75, size, size * 0.7));
		graphics.setColor(TREE_CANOPY);
		graphics.fill(new Ellipse2D.Double(x - size * 0.42, y - size * 0.85, size * 0.84, size * 0.62));
		graphics.setColor(TREE_CANOPY_LIGHT);
		graphics.fill(new Ellipse2D.Double(x - size * 0.22, y - size * 0.9, size * 0.4, size * 0.32));
	}

	private static int seedFor(int[][] trackMap) {
		int seed = trackMap.length * 31 + trackMap[0].length;
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				seed = 31 * seed + trackMap[row][column];
			}
		}
		return seed;
	}
}
