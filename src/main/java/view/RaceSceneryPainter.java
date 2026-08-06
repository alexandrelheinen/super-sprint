package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Circuit;
import model.ResourcePaths;
import model.Terrain;

/**
 * Terrain-aware scenery for the race view and track menu previews.
 * Ground tiles and flora come from Kenney Top-down Tanks Redux (CC0),
 * extracted at build time onto the classpath under {@code /sprites/kenney/}.
 */
public final class RaceSceneryPainter {

	private static final class TerrainStyle {
		final Color letterbox;
		final String[] groundTiles;
		final String[] floraFiles;
		final double[] sizeScales;
		final int[] weights;
		final double densityScale;

		TerrainStyle(
				Color letterbox,
				String[] groundTiles,
				String[] floraFiles,
				double[] sizeScales,
				int[] weights,
				double densityScale) {
			this.letterbox = letterbox;
			this.groundTiles = groundTiles;
			this.floraFiles = floraFiles;
			this.sizeScales = sizeScales;
			this.weights = weights;
			this.densityScale = densityScale;
		}
	}

	private static final Map<Terrain, TerrainStyle> STYLES = buildStyles();
	private static final Map<Terrain, BufferedImage[]> GROUND_CACHE = new EnumMap<>(Terrain.class);
	private static final Map<Terrain, BufferedImage[]> FLORA_CACHE = new EnumMap<>(Terrain.class);

	private RaceSceneryPainter() {
	}

	public static Color letterboxColor(Terrain terrain) {
		return style(terrain).letterbox;
	}

	/**
	 * Fills the playfield by tiling Kenney grass/sand tiles.
	 */
	public static void paintGround(Graphics2D graphics, int width, int height, Terrain terrain, int seed) {
		BufferedImage[] tiles = loadGroundTiles(terrain);
		if (tiles.length == 0) {
			graphics.setColor(style(terrain).letterbox);
			graphics.fillRect(0, 0, width, height);
			return;
		}
		Random random = new Random(seed);
		int tileSize = tiles[0].getWidth();
		if (tileSize < 1) {
			tileSize = 64;
		}
		for (int y = 0; y < height; y += tileSize) {
			for (int x = 0; x < width; x += tileSize) {
				// Deterministic checker with occasional swaps for variety.
				int variant = ((x / tileSize) + (y / tileSize)) & 1;
				if (tiles.length > 1 && random.nextInt(7) == 0) {
					variant = 1 - variant;
				}
				variant = Math.floorMod(variant, tiles.length);
				graphics.drawImage(tiles[variant], x, y, tileSize, tileSize, null);
			}
		}
	}

	/**
	 * Places flora sprites outside {@code blockedArea}
	 * (usually the asphalt footprint).
	 */
	public static void paintFlora(
			Graphics2D graphics,
			int width,
			int height,
			Shape blockedArea,
			Terrain terrain,
			int seed,
			int treeCount,
			double minSizeFraction,
			double maxSizeFraction) {
		TerrainStyle palette = style(terrain);
		BufferedImage[] sprites = loadFlora(terrain);
		if (sprites.length == 0) {
			return;
		}
		int targetCount = Math.max(1, (int) Math.round(treeCount * palette.densityScale));
		Random random = new Random(seed);
		double minSize = Math.min(width, height) * minSizeFraction;
		double maxSize = Math.min(width, height) * maxSizeFraction;
		int placed = 0;
		int attempts = 0;
		while (placed < targetCount && attempts < targetCount * 40) {
			attempts++;
			int variant = pickVariant(random, palette.weights);
			double size = (minSize + random.nextDouble() * Math.max(1.0, maxSize - minSize))
					* palette.sizeScales[variant];
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
			paintFloraSprite(graphics, sprites[variant], x, y, size, random);
			placed++;
		}
	}

	/**
	 * Places flora inside {@link Circuit#TILE_OPEN} cells
	 * (infield / runoff), keeping them off asphalt tiles.
	 */
	public static void paintFloraInOpenTiles(
			Graphics2D graphics,
			int[][] trackMap,
			int tileSize,
			int originX,
			int originY,
			Terrain terrain,
			int seed,
			int maxTrees) {
		TerrainStyle palette = style(terrain);
		BufferedImage[] sprites = loadFlora(terrain);
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
		int count = Math.min(
				Math.max(1, (int) Math.round(maxTrees * palette.densityScale)),
				openCells.size() * 3);
		for (int index = 0; index < count; index++) {
			int[] cell = openCells.get(random.nextInt(openCells.size()));
			int variant = pickVariant(random, palette.weights);
			double size = tileSize * (0.28 + random.nextDouble() * 0.22) * palette.sizeScales[variant];
			double x = originX + cell[1] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			double y = originY + cell[0] * tileSize + tileSize * (0.25 + random.nextDouble() * 0.5);
			paintFloraSprite(graphics, sprites[variant], x, y, size, random);
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

	private static void paintFloraSprite(
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
		// Small random yaw so top-down trees don't look stamped.
		double angle = (random.nextDouble() - 0.5) * 0.7;

		AffineTransform previous = graphics.getTransform();
		graphics.translate(x, y);
		graphics.rotate(angle);
		graphics.drawImage(sprite, -drawW / 2, -drawH / 2, drawW, drawH, null);
		graphics.setTransform(previous);
	}

	private static int pickVariant(Random random, int[] weights) {
		int total = 0;
		for (int weight : weights) {
			total += weight;
		}
		int roll = random.nextInt(Math.max(1, total));
		int cumulative = 0;
		for (int index = 0; index < weights.length; index++) {
			cumulative += weights[index];
			if (roll < cumulative) {
				return index;
			}
		}
		return 0;
	}

	private static synchronized BufferedImage[] loadGroundTiles(Terrain terrain) {
		BufferedImage[] cached = GROUND_CACHE.get(terrain);
		if (cached != null) {
			return cached;
		}
		BufferedImage[] tiles = loadKenneyImages(style(terrain).groundTiles);
		GROUND_CACHE.put(terrain, tiles);
		return tiles;
	}

	private static synchronized BufferedImage[] loadFlora(Terrain terrain) {
		BufferedImage[] cached = FLORA_CACHE.get(terrain);
		if (cached != null) {
			return cached;
		}
		BufferedImage[] sprites = loadKenneyImages(style(terrain).floraFiles);
		FLORA_CACHE.put(terrain, sprites);
		return sprites;
	}

	private static BufferedImage[] loadKenneyImages(String[] fileNames) {
		List<BufferedImage> loaded = new ArrayList<>();
		for (String fileName : fileNames) {
			try {
				BufferedImage image = ResourcePaths.loadKenneySprite(fileName);
				if (image != null) {
					loaded.add(image);
				}
			} catch (IOException ignored) {
				// Skip missing variants; placement still works with whatever loads.
			}
		}
		return loaded.toArray(new BufferedImage[0]);
	}

	private static TerrainStyle style(Terrain terrain) {
		TerrainStyle palette = STYLES.get(terrain);
		return palette != null ? palette : STYLES.get(Terrain.GRASS);
	}

	private static Map<Terrain, TerrainStyle> buildStyles() {
		Map<Terrain, TerrainStyle> styles = new EnumMap<>(Terrain.class);
		styles.put(Terrain.GRASS, new TerrainStyle(
				new Color(72, 118, 48),
				new String[] {"tileGrass1.png", "tileGrass2.png"},
				new String[] {
						"treeGreen_large.png",
						"treeGreen_small.png",
						"treeGreen_twigs.png",
						"treeGreen_leaf.png",
				},
				new double[] {1.0, 0.72, 0.48, 0.4},
				new int[] {40, 34, 18, 8},
				1.0));
		styles.put(Terrain.SAND, new TerrainStyle(
				new Color(196, 164, 104),
				new String[] {"tileSand1.png", "tileSand2.png"},
				new String[] {
						"treeBrown_large.png",
						"treeBrown_small.png",
						"treeBrown_twigs.png",
						"treeBrown_leaf.png",
				},
				new double[] {1.0, 0.72, 0.48, 0.4},
				new int[] {38, 32, 20, 10},
				0.85));
		return styles;
	}
}
