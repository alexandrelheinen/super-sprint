package view;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import model.Circuit;
import model.ResourcePaths;
import model.Terrain;

/**
 * Terrain-aware ground fill and flora placement for the race view and track
 * menu previews. Flora art is derived from Kenney's Top-down Tanks Redux (CC0).
 */
public final class RaceSceneryPainter {

	private enum GroundFill {
		MEADOW,
		DAPPLED,
		DUNES
	}

	private static final class TerrainStyle {
		final Color groundLight;
		final Color groundDark;
		final Color patch;
		final GroundFill fill;
		final String[] spriteFiles;
		final double[] sizeScales;
		final int[] weights;
		final double densityScale;

		TerrainStyle(
				Color groundLight,
				Color groundDark,
				Color patch,
				GroundFill fill,
				String[] spriteFiles,
				double[] sizeScales,
				int[] weights,
				double densityScale) {
			this.groundLight = groundLight;
			this.groundDark = groundDark;
			this.patch = patch;
			this.fill = fill;
			this.spriteFiles = spriteFiles;
			this.sizeScales = sizeScales;
			this.weights = weights;
			this.densityScale = densityScale;
		}
	}

	private static final Map<Terrain, TerrainStyle> STYLES = buildStyles();
	private static final Map<Terrain, BufferedImage[]> FLORA_CACHE = new EnumMap<>(Terrain.class);

	private RaceSceneryPainter() {
	}

	public static Color letterboxColor(Terrain terrain) {
		return style(terrain).groundDark;
	}

	public static void paintGround(Graphics2D graphics, int width, int height, Terrain terrain, int seed) {
		TerrainStyle palette = style(terrain);
		graphics.setPaint(new GradientPaint(0, 0, palette.groundLight, width, height, palette.groundDark));
		graphics.fillRect(0, 0, width, height);

		graphics.setPaint(new GradientPaint(
				0,
				0,
				withAlpha(palette.groundDark, 0),
				0,
				height * 0.18f,
				withAlpha(palette.groundDark, 60)));
		graphics.fillRect(0, 0, width, Math.max(1, (int) (height * 0.22f)));

		graphics.setPaint(new GradientPaint(
				0,
				height,
				withAlpha(palette.groundDark, 75),
				0,
				height * 0.78f,
				withAlpha(palette.groundDark, 0)));
		graphics.fillRect(0, (int) (height * 0.72f), width, height);

		switch (palette.fill) {
			case DAPPLED -> paintDappledFill(graphics, width, height, palette, seed);
			case DUNES -> paintDuneFill(graphics, width, height, palette, seed);
			case MEADOW -> paintMeadowPatches(graphics, width, height, palette, seed);
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

	private static void paintMeadowPatches(
			Graphics2D graphics,
			int width,
			int height,
			TerrainStyle palette,
			int seed) {
		Random random = new Random(seed);
		int patchCount = Math.max(12, (width * height) / 700);
		for (int index = 0; index < patchCount; index++) {
			double cx = random.nextDouble() * width;
			double cy = random.nextDouble() * height;
			double rw = 6 + random.nextDouble() * (12 + width * 0.01);
			double rh = 3 + random.nextDouble() * (8 + height * 0.008);
			graphics.setColor(palette.patch);
			graphics.fill(new Ellipse2D.Double(cx - rw, cy - rh, rw * 2, rh * 2));
		}
	}

	private static void paintDappledFill(
			Graphics2D graphics,
			int width,
			int height,
			TerrainStyle palette,
			int seed) {
		Random random = new Random(seed);
		int patchCount = Math.max(28, (width * height) / 420);
		for (int index = 0; index < patchCount; index++) {
			double cx = random.nextDouble() * width;
			double cy = random.nextDouble() * height;
			double rw = 4 + random.nextDouble() * (9 + width * 0.008);
			double rh = 3 + random.nextDouble() * (7 + height * 0.007);
			graphics.setColor(palette.patch);
			graphics.fill(new Ellipse2D.Double(cx - rw, cy - rh, rw * 2, rh * 2));
		}
		graphics.setColor(withAlpha(palette.groundDark, 28));
		for (int index = 0; index < 8; index++) {
			double x = random.nextDouble() * width;
			graphics.fill(new Rectangle2D.Double(x, 0, 2 + random.nextDouble() * 4, height));
		}
	}

	private static void paintDuneFill(
			Graphics2D graphics,
			int width,
			int height,
			TerrainStyle palette,
			int seed) {
		Random random = new Random(seed);
		graphics.setColor(withAlpha(palette.groundLight, 55));
		double bandGap = Math.max(18, Math.min(width, height) * 0.045);
		for (double offset = -height; offset < width + height; offset += bandGap) {
			double thickness = bandGap * (0.35 + random.nextDouble() * 0.35);
			java.awt.geom.Path2D band = new java.awt.geom.Path2D.Double();
			band.moveTo(offset, 0);
			band.lineTo(offset + thickness, 0);
			band.lineTo(offset - height + thickness, height);
			band.lineTo(offset - height, height);
			band.closePath();
			graphics.fill(band);
		}
		int speckles = Math.max(10, (width * height) / 1400);
		graphics.setColor(palette.patch);
		for (int index = 0; index < speckles; index++) {
			double cx = random.nextDouble() * width;
			double cy = random.nextDouble() * height;
			double r = 2 + random.nextDouble() * 5;
			graphics.fill(new Ellipse2D.Double(cx - r, cy - r * 0.55, r * 2, r * 1.1));
		}
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
		double angle = random.nextDouble() * Math.PI * 2.0;

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

	private static synchronized BufferedImage[] loadFlora(Terrain terrain) {
		BufferedImage[] cached = FLORA_CACHE.get(terrain);
		if (cached != null) {
			return cached;
		}
		TerrainStyle palette = style(terrain);
		List<BufferedImage> loaded = new ArrayList<>();
		for (String relativePath : palette.spriteFiles) {
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
		BufferedImage[] sprites = loaded.toArray(new BufferedImage[0]);
		FLORA_CACHE.put(terrain, sprites);
		return sprites;
	}

	private static TerrainStyle style(Terrain terrain) {
		TerrainStyle palette = STYLES.get(terrain);
		return palette != null ? palette : STYLES.get(Terrain.GRASS);
	}

	private static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static Map<Terrain, TerrainStyle> buildStyles() {
		Map<Terrain, TerrainStyle> styles = new EnumMap<>(Terrain.class);
		styles.put(Terrain.GRASS, new TerrainStyle(
				new Color(96, 132, 48),
				new Color(52, 86, 34),
				new Color(68, 104, 38, 100),
				GroundFill.MEADOW,
				new String[] {
						"trees/grass/canopy_large.png",
						"trees/grass/canopy_small.png",
						"trees/grass/canopy_accent.png",
				},
				new double[] {1.0, 0.78, 0.7},
				new int[] {44, 40, 16},
				1.0));
		styles.put(Terrain.FOREST, new TerrainStyle(
				new Color(54, 92, 42),
				new Color(28, 52, 28),
				new Color(36, 64, 32, 120),
				GroundFill.DAPPLED,
				new String[] {
						"trees/forest/canopy_large.png",
						"trees/forest/canopy_deep.png",
						"trees/forest/canopy_small.png",
				},
				new double[] {1.05, 1.0, 0.72},
				new int[] {38, 34, 28},
				1.45));
		styles.put(Terrain.AUTUMN, new TerrainStyle(
				new Color(118, 112, 48),
				new Color(78, 68, 32),
				new Color(140, 92, 40, 95),
				GroundFill.MEADOW,
				new String[] {
						"trees/autumn/canopy_large.png",
						"trees/autumn/canopy_small.png",
						"trees/autumn/canopy_accent.png",
				},
				new double[] {1.0, 0.78, 0.74},
				new int[] {40, 34, 26},
				1.1));
		styles.put(Terrain.DESERT, new TerrainStyle(
				new Color(214, 178, 108),
				new Color(168, 128, 72),
				new Color(196, 148, 84, 90),
				GroundFill.DUNES,
				new String[] {
						"trees/desert/scrub_bush.png",
						"trees/desert/scrub_large.png",
						"trees/desert/scrub_small.png",
						"trees/desert/scrub_twigs.png",
				},
				new double[] {0.95, 0.72, 0.62, 0.5},
				new int[] {30, 30, 24, 16},
				0.7));
		return styles;
	}
}
