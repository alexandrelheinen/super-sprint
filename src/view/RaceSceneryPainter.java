package view;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import model.Circuit;
import model.ResourcePaths;

/**
 * Grass field plus placed top-down tree sprites for the race view and track
 * menu previews. Tree art is from Kenney's Top-down Tanks Redux (CC0).
 */
public final class RaceSceneryPainter {

	/** Grass tones sampled from the splash artwork. */
	public static final Color GRASS_LIGHT = new Color(96, 132, 48);
	public static final Color GRASS_DARK = new Color(52, 86, 34);
	public static final Color GRASS_PATCH = new Color(68, 104, 38, 100);

	private static final String[] TREE_SPRITE_FILES = {
			"trees/tree_green_large.png",
			"trees/tree_green_small.png",
			"trees/tree_brown_large.png",
			"trees/tree_brown_small.png",
	};
	/** Relative draw sizes vs the placement {@code size} parameter. */
	private static final double[] TREE_SIZE_SCALE = {1.0, 0.78, 0.95, 0.72};
	/** Selection weights: green canopies most often, autumn accents less. */
	private static final int[] TREE_WEIGHTS = {40, 32, 16, 12};

	private static BufferedImage[] treeSprites;

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
	 * Places top-down tree sprites outside {@code blockedArea}
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
		BufferedImage[] sprites = loadTreeSprites();
		if (sprites.length == 0) {
			return;
		}
		Random random = new Random(seed);
		double minSize = Math.min(width, height) * minSizeFraction;
		double maxSize = Math.min(width, height) * maxSizeFraction;
		int placed = 0;
		int attempts = 0;
		while (placed < treeCount && attempts < treeCount * 40) {
			attempts++;
			int variant = pickTreeVariant(random);
			double size = (minSize + random.nextDouble() * Math.max(1.0, maxSize - minSize))
					* TREE_SIZE_SCALE[variant];
			double x = size * 0.7 + random.nextDouble() * (width - size * 1.4);
			double y = size * 0.7 + random.nextDouble() * (height - size * 1.4);
			double clearance = size * 0.35;
			Rectangle2D footprint = new Rectangle2D.Double(
					x - size * 0.5 - clearance,
					y - size * 0.5 - clearance,
					size + clearance * 2,
					size + clearance * 2);
			if (blockedArea != null && blockedArea.intersects(footprint)) {
				continue;
			}
			paintTreeSprite(graphics, sprites[variant], x, y, size, random);
			placed++;
		}
	}

	/**
	 * Places tree sprites inside {@link Circuit#TILE_OPEN} cells
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
		BufferedImage[] sprites = loadTreeSprites();
		if (sprites.length == 0) {
			return;
		}
		Random random = new Random(seed);
		List<int[]> openCells = new ArrayList<>();
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
			int variant = pickTreeVariant(random);
			double size = tileSize * (0.28 + random.nextDouble() * 0.22) * TREE_SIZE_SCALE[variant];
			double x = originX + cell[1] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			double y = originY + cell[0] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			paintTreeSprite(graphics, sprites[variant], x, y, size, random);
		}
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

	public static int seedFor(int[][] trackMap) {
		int seed = trackMap.length * 31 + trackMap[0].length;
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				seed = 31 * seed + trackMap[row][column];
			}
		}
		return seed;
	}

	private static void paintTreeSprite(
			Graphics2D graphics,
			BufferedImage sprite,
			double x,
			double y,
			double size,
			Random random) {
		if (sprite == null || size < 2) {
			return;
		}
		double aspect = (double) sprite.getHeight() / Math.max(1, sprite.getWidth());
		int drawW = Math.max(2, (int) Math.round(size));
		int drawH = Math.max(2, (int) Math.round(size * aspect));
		double angle = random.nextDouble() * Math.PI * 2.0;

		AffineTransform previous = graphics.getTransform();
		graphics.translate(x, y);
		graphics.rotate(angle);
		graphics.drawImage(sprite, -drawW / 2, -drawH / 2, drawW, drawH, null);
		graphics.setTransform(previous);
	}

	private static int pickTreeVariant(Random random) {
		int total = 0;
		for (int weight : TREE_WEIGHTS) {
			total += weight;
		}
		int roll = random.nextInt(total);
		int cumulative = 0;
		for (int index = 0; index < TREE_WEIGHTS.length; index++) {
			cumulative += TREE_WEIGHTS[index];
			if (roll < cumulative) {
				return index;
			}
		}
		return 0;
	}

	private static synchronized BufferedImage[] loadTreeSprites() {
		if (treeSprites != null) {
			return treeSprites;
		}
		List<BufferedImage> loaded = new ArrayList<>();
		for (String relativePath : TREE_SPRITE_FILES) {
			try {
				File file = new File(ResourcePaths.bundledSprite(relativePath));
				BufferedImage image = ImageIO.read(file);
				if (image != null) {
					loaded.add(image);
				}
			} catch (IOException ignored) {
				// Skip missing variants; placement still works with whatever loads.
			}
		}
		treeSprites = loaded.toArray(new BufferedImage[0]);
		return treeSprites;
	}
}
