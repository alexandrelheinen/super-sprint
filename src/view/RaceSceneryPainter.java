package view;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import model.Circuit;

/**
 * Procedural grass and trees for race view and track-menu previews.
 * Splash art bakes scenery into {@code splash.png}; no separate tree/grass
 * sprites ship with the game, so this painter recreates that look in code.
 */
public final class RaceSceneryPainter {

	/** Grass tones sampled from the splash artwork. */
	public static final Color GRASS_LIGHT = new Color(96, 132, 48);
	public static final Color GRASS_DARK = new Color(52, 86, 34);
	public static final Color GRASS_PATCH = new Color(68, 104, 38, 100);

	/** Splash-inspired canopy greens (darker / denser than preview stubs). */
	private static final Color TREE_SHADOW = new Color(20, 28, 16, 55);
	private static final Color TREE_CANOPY_DARK = new Color(31, 48, 29);
	private static final Color TREE_CANOPY = new Color(45, 71, 35);
	private static final Color TREE_CANOPY_MID = new Color(58, 76, 40);
	private static final Color TREE_CANOPY_LIGHT = new Color(78, 108, 48);
	private static final Color TREE_HIGHLIGHT = new Color(98, 128, 58);

	private RaceSceneryPainter() {
	}

	public static void paintGrassField(Graphics2D graphics, int width, int height) {
		graphics.setPaint(new GradientPaint(0, 0, GRASS_LIGHT, width, height, GRASS_DARK));
		graphics.fillRect(0, 0, width, height);

		graphics.setPaint(new GradientPaint(
				0,
				0,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 0),
				0,
				height * 0.18f,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 60)));
		graphics.fillRect(0, 0, width, Math.max(1, (int) (height * 0.22f)));

		graphics.setPaint(new GradientPaint(
				0,
				height,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 75),
				0,
				height * 0.78f,
				new Color(GRASS_DARK.getRed(), GRASS_DARK.getGreen(), GRASS_DARK.getBlue(), 0)));
		graphics.fillRect(0, (int) (height * 0.72f), width, height);
	}

	public static void paintGrassPatches(Graphics2D graphics, int width, int height, int seed) {
		Random random = new Random(seed);
		int patchCount = Math.max(12, (width * height) / 700);
		for (int index = 0; index < patchCount; index++) {
			double cx = random.nextDouble() * width;
			double cy = random.nextDouble() * height;
			double rw = 6 + random.nextDouble() * (12 + width * 0.01);
			double rh = 3 + random.nextDouble() * (8 + height * 0.008);
			graphics.setColor(GRASS_PATCH);
			graphics.fill(new Ellipse2D.Double(cx - rw, cy - rh, rw * 2, rh * 2));
		}
	}

	/**
	 * Places bushy, splash-inspired tree canopies outside {@code blockedArea}
	 * (usually the asphalt footprint).
	 */
	public static void paintTrees(
			Graphics2D graphics,
			int width,
			int height,
			Shape blockedArea,
			int seed,
			int treeCount,
			double minSizeFraction,
			double maxSizeFraction) {
		Random random = new Random(seed);
		double minSize = Math.min(width, height) * minSizeFraction;
		double maxSize = Math.min(width, height) * maxSizeFraction;
		int placed = 0;
		int attempts = 0;
		while (placed < treeCount && attempts < treeCount * 40) {
			attempts++;
			double size = minSize + random.nextDouble() * Math.max(1.0, maxSize - minSize);
			double x = size * 0.7 + random.nextDouble() * (width - size * 1.4);
			double y = size * 0.7 + random.nextDouble() * (height - size * 1.4);
			double clearance = size * 0.35;
			Rectangle2D footprint = new Rectangle2D.Double(
					x - size * 0.55 - clearance,
					y - size * 0.55 - clearance,
					size * 1.1 + clearance * 2,
					size * 1.1 + clearance * 2);
			if (blockedArea != null && blockedArea.intersects(footprint)) {
				continue;
			}
			paintSplashTree(graphics, x, y, size, random);
			if (random.nextDouble() < 0.35) {
				// Occasional twin canopy like the splash clumps.
				double ox = (random.nextDouble() - 0.5) * size * 0.7;
				double oy = (random.nextDouble() - 0.5) * size * 0.55;
				paintSplashTree(graphics, x + ox, y + oy, size * (0.65 + random.nextDouble() * 0.25), random);
			}
			placed++;
		}
	}

	/**
	 * Top-down bushy canopy inspired by splash greens — layered ellipses, no
	 * tall trunk (trunks look wrong from above at race scale).
	 */
	public static void paintSplashTree(Graphics2D graphics, double x, double y, double size, Random random) {
		graphics.setColor(TREE_SHADOW);
		graphics.fill(new Ellipse2D.Double(x - size * 0.45, y + size * 0.1, size * 0.9, size * 0.35));

		int blobs = 4 + random.nextInt(3);
		for (int index = 0; index < blobs; index++) {
			double ox = (random.nextDouble() - 0.5) * size * 0.55;
			double oy = (random.nextDouble() - 0.5) * size * 0.45;
			double rw = size * (0.28 + random.nextDouble() * 0.28);
			double rh = size * (0.24 + random.nextDouble() * 0.26);
			Color canopy = switch (index % 4) {
				case 0 -> TREE_CANOPY_DARK;
				case 1 -> TREE_CANOPY;
				case 2 -> TREE_CANOPY_MID;
				default -> TREE_CANOPY_LIGHT;
			};
			graphics.setColor(canopy);
			graphics.fill(new Ellipse2D.Double(x + ox - rw, y + oy - rh, rw * 2, rh * 2));
		}

		graphics.setColor(TREE_HIGHLIGHT);
		double hw = size * (0.14 + random.nextDouble() * 0.1);
		double hh = size * (0.12 + random.nextDouble() * 0.08);
		graphics.fill(new Ellipse2D.Double(
				x - size * 0.15 - hw,
				y - size * 0.25 - hh,
				hw * 2,
				hh * 2));
	}

	/** Axis-aligned asphalt footprint from the tile map (non-{@link Circuit#TILE_OPEN}). */
	public static Area asphaltFootprint(int[][] trackMap, int tileSize, int originX, int originY) {
		Area area = new Area();
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				if (trackMap[row][column] == Circuit.TILE_OPEN) {
					continue;
				}
				area.add(new Area(new Rectangle2D.Double(
						originX + column * tileSize,
						originY + row * tileSize,
						tileSize,
						tileSize)));
			}
		}
		return area;
	}

	/**
	 * Places splash-style canopies inside {@link Circuit#TILE_OPEN} cells
	 * (infield / runoff), keeping them off asphalt tiles.
	 */
	public static void paintTreesInOpenTiles(
			Graphics2D graphics,
			int[][] trackMap,
			int tileSize,
			int originX,
			int originY,
			int seed,
			int maxTrees) {
		Random random = new Random(seed);
		java.util.List<int[]> openCells = new java.util.ArrayList<>();
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				if (trackMap[row][column] == Circuit.TILE_OPEN) {
					openCells.add(new int[] {row, column});
				}
			}
		}
		if (openCells.isEmpty()) {
			return;
		}
		int count = Math.min(maxTrees, openCells.size() * 2);
		for (int index = 0; index < count; index++) {
			int[] cell = openCells.get(random.nextInt(openCells.size()));
			double size = tileSize * (0.22 + random.nextDouble() * 0.2);
			double x = originX + cell[1] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			double y = originY + cell[0] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			paintSplashTree(graphics, x, y, size, random);
		}
	}

	public static int seedFor(int[][] trackMap) {
		int seed = trackMap.length * 31 + trackMap[0].length;
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				seed = 31 * seed + trackMap[row][column];
			}
		}
		return seed;
	}
}
