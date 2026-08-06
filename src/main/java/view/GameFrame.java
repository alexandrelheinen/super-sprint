package view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.ResourcePaths;
import model.Terrain;
import model.TrackGeometry;
import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Race viewport rendered with a {@link BufferStrategy} on a {@link Canvas}.
 * Hosted inside {@link AppShell}. Simulation stays in logical track pixels;
 * the view contain-scales (min of width/height fit) into the shell.
 */
public class GameFrame extends Canvas implements Observer {

	public static final int TILE_SIZE = 219;
	public static final int[] CAR_RENDER_OFFSET = {0, 0};

	private static final long serialVersionUID = 1L;
	private static final int BUFFER_STRATEGY_BUFFERS = 2;
	/** Padding around the asphalt ribbon when contain-fitting into the canvas. */
	private static final int VIEW_PADDING_PX = 24;
	private static final int HUD_BAR_HEIGHT = 72;
	private static final int OUTER_TREE_COUNT = 22;
	private static final int INFIELD_TREE_COUNT = 18;
	private static final int START_SLOT_COUNT = Circuit.START_SLOT_COUNT;
	// Cars face up on the grid, so their on-screen width is the sprite height
	// and their nose sits half a sprite width above the slot center.
	private static final int START_MARKER_HALF_WIDTH = Circuit.CAR_ANCHOR_HALF_HEIGHT_PX + 4;
	private static final int START_MARKER_NOSE_OFFSET = Circuit.CAR_ANCHOR_HALF_WIDTH_PX + 5;
	private static final int START_MARKER_TICK_LENGTH = 10;
	private static final int START_MARKER_STROKE = 2;
	/** Soft painted look: asphalt shadow under translucent chalk-white. */
	private static final Color START_MARKER_SHADOW = new Color(36, 48, 52, 95);
	private static final Color START_MARKER_PAINT = new Color(206, 216, 220, 165);
	private static final int FINISH_CHECKER_ROWS = 2;
	private static final int FINISH_CHECKER_COLS = 14;
	private static final int FINISH_BAND_HEIGHT = 12;
	private static final Color FINISH_CHECKER_DARK = new Color(28, 32, 34);
	private static final Color FINISH_CHECKER_LIGHT = new Color(236, 238, 240);
	private static final Color FINISH_EDGE_ACCENT = new Color(240, 196, 48, 200);
	private static final int HUD_SIDE_PADDING = 24;
	private static final int HUD_LAP_CHIP_GAP = 18;
	private static final int SPRITE_CENTER_DIVISOR = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int MS_PER_SECOND = 1000;
	private static final String HUD_RACE_TIME_PREFIX = "TIME ";
	private static final String HUD_TIME_SUFFIX = "s";
	private static final String HUD_LAP_SEPARATOR = "/";
	private static final String HUD_HUMAN_PLAYER_PREFIX = "P";
	private static final String HUD_AI_PLAYER_PREFIX = "C";
	private static final String HUD_AI_PLAYER_NAME = "C";
	private static final String HUD_SLOT_SEPARATOR = " ";
	private static final String LOG_SPRITE_LOADED = "Sprite #";
	private static final String LOG_SPRITE_LOADED_SUFFIX = " loaded";
	private static final String LOG_SPRITE_SEPARATOR = " **************** \n";
	private static final String ERROR_CAR_SPRITES = "Error loading car sprites: ";
	private static final String ERROR_TRACK_TILES = "Error loading track tile images: ";
	private static final String OBSERVER_CAR_TOKEN = "Car";

	private final BufferedImage[] carSprites;
	private final BufferedImage[][] trackTiles;
	private final int[][] trackMap;
	private final boolean[] renderFlags;
	private final int trackNumber;
	private final int[] carModels;
	private final int[] mapDimensions;
	private final java.awt.Font hudFont;
	private final Dimension preferredRaceSize;
	private final Rectangle2D.Float viewBounds;
	private final Terrain terrain;
	private BufferedImage staticScene;
	private volatile boolean renderingEnabled = true;
	private volatile boolean racingInputEnabled = false;
	private volatile Car[] hudCars;
	private volatile int totalLaps;
	private volatile String countdownLabel = "";
	private volatile float countdownProgress = 0f;
	private volatile boolean countdownGoStep = false;
	private volatile boolean countdownVisible = false;

	public GameFrame(int[] carModels, int[][] trackMap, int trackNumber) {
		this.trackMap = trackMap;
		this.trackNumber = trackNumber;
		this.terrain = GameCatalog.trackTerrain(trackNumber);
		this.carModels = carModels.clone();
		mapDimensions = new int[] {trackMap.length, trackMap[0].length};

		renderFlags = new boolean[carModels.length + ONE_BASED_INDEX_OFFSET];
		carSprites = new BufferedImage[carModels.length];
		for (int index = 0; index < carModels.length; index++) {
			try {
				carSprites[index] = ResourcePaths.loadCarSprite(carModels[index]);
				System.out.println(LOG_SPRITE_LOADED + (index + ONE_BASED_INDEX_OFFSET) + LOG_SPRITE_LOADED_SUFFIX);
			} catch (IOException exception) {
				System.err.println(ERROR_CAR_SPRITES + exception.getMessage());
			}
		}
		System.out.println(LOG_SPRITE_SEPARATOR);

		trackTiles = loadTrackTiles(trackMap);
		hudFont = GameTheme.FONT_HUD;
		viewBounds = computeAsphaltViewBounds(trackMap);

		preferredRaceSize = contentSizeFor(mapDimensions[0], mapDimensions[1]);
		setPreferredSize(preferredRaceSize);
		setSize(preferredRaceSize);
		setFocusable(true);
		setIgnoreRepaint(true);
	}

	/**
	 * Baseline race content size used to size the shell. Live races contain-fit
	 * the asphalt ribbon into whatever canvas they receive.
	 */
	public static Dimension contentSizeFor(int rows, int columns) {
		return new Dimension(
				columns * TILE_SIZE + 2 * VIEW_PADDING_PX,
				rows * TILE_SIZE + 2 * VIEW_PADDING_PX + HUD_BAR_HEIGHT);
	}

	/**
	 * Axis-aligned bounds of the asphalt ribbon in track-local pixels (origin at
	 * the top-left of the tile grid), padded so curbs stay on screen.
	 */
	private static Rectangle2D.Float computeAsphaltViewBounds(int[][] trackMap) {
		Path2D unitPath = TrackGeometry.buildPreviewPath(trackMap);
		AffineTransform toPixels = AffineTransform.getScaleInstance(TILE_SIZE, TILE_SIZE);
		Shape centerline = toPixels.createTransformedShape(unitPath);
		float stroke = (Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS) + VIEW_PADDING_PX * 2f;
		Shape band = new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
				.createStrokedShape(centerline);
		Rectangle2D bounds = band.getBounds2D();
		int trackW = trackMap[0].length * TILE_SIZE;
		int trackH = trackMap.length * TILE_SIZE;
		float x = (float) Math.max(0, bounds.getX());
		float y = (float) Math.max(0, bounds.getY());
		float w = (float) Math.min(trackW - x, bounds.getWidth());
		float h = (float) Math.min(trackH - y, bounds.getHeight());
		return new Rectangle2D.Float(x, y, Math.max(1f, w), Math.max(1f, h));
	}

	/**
	 * Creates the buffer strategy once this canvas is displayable inside the shell.
	 */
	public void realizeBufferStrategy() {
		if (!isDisplayable()) {
			throw new IllegalStateException("Race canvas must be displayable before creating a buffer strategy");
		}
		createBufferStrategy(BUFFER_STRATEGY_BUFFERS);
		requestFocusInWindow();
	}

	/**
	 * Registers the cars and lap target so the HUD can show per-car lap counts.
	 */
	public void attachRaceStatus(Car[] cars, int lapCount) {
		hudCars = cars.clone();
		totalLaps = lapCount;
	}

	public void setRacingInputEnabled(boolean enabled) {
		racingInputEnabled = enabled;
	}

	public boolean isRacingInputEnabled() {
		return racingInputEnabled;
	}

	public void setCountdownPresentation(String label, float progress, boolean goStep) {
		countdownLabel = label != null ? label : "";
		countdownProgress = Math.max(0f, Math.min(1f, progress));
		countdownGoStep = goStep;
		countdownVisible = !countdownLabel.isEmpty();
	}

	public void clearCountdownPresentation() {
		countdownVisible = false;
		countdownLabel = "";
		countdownProgress = 0f;
		countdownGoStep = false;
	}

	/**
	 * Warms the static scenery cache and presents a complete race frame (track,
	 * flora, tiles, cars on the grid, HUD) before the countdown begins.
	 */
	public void presentPreparedScene(Circuit circuit) {
		staticScene();
		// Double-present so both BufferStrategy backs show the prepared scene.
		renderCompositeFrame(circuit);
		renderCompositeFrame(circuit);
		Toolkit.getDefaultToolkit().sync();
	}

	/**
	 * Paints a full race frame in one pass (scene + cars + HUD + countdown).
	 * Used for the opening present and animated countdown; the live race still
	 * uses the observer-driven path in {@link #update}.
	 */
	public void renderCompositeFrame(Circuit circuit) {
		if (!renderingEnabled || !isDisplayable()) {
			return;
		}
		BufferStrategy bufferStrategy = getBufferStrategy();
		if (bufferStrategy == null) {
			return;
		}
		Graphics graphics = null;
		try {
			graphics = bufferStrategy.getDrawGraphics();
			Graphics2D graphics2D = (Graphics2D) graphics;
			graphics2D.setRenderingHint(
					RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			graphics2D.setColor(RaceSceneryPainter.letterboxColor(terrain));
			graphics2D.fillRect(0, 0, getWidth(), getHeight());

			applyViewTransform(graphics2D);
			graphics2D.drawImage(staticScene(), 0, 0, null);
			paintFinishLine(graphics2D, circuit);
			paintCarsInView(graphics2D);

			graphics2D.setTransform(new AffineTransform());
			UiPainter.paintHudStrip(graphics2D, 0, getHeight() - HUD_BAR_HEIGHT, getWidth(), HUD_BAR_HEIGHT);
			paintHudContent(graphics2D, circuit);
			if (countdownVisible) {
				paintCountdownOverlay(graphics2D);
			}
		} finally {
			if (graphics != null) {
				graphics.dispose();
			}
		}
		bufferStrategy.show();
		Toolkit.getDefaultToolkit().sync();
	}

	/** Track-local origin: cars / finish line are relative to the tile grid. */
	private int trackOriginX() {
		return 0;
	}

	private int trackOriginY() {
		return 0;
	}

	/**
	 * Contain-scale the asphalt ribbon into the canvas above the HUD - uses the
	 * smaller of width and height fit ratios so the whole track stays visible.
	 */
	private float viewScale() {
		int canvasWidth = Math.max(1, getWidth());
		int availHeight = Math.max(1, getHeight() - HUD_BAR_HEIGHT);
		return Math.min(canvasWidth / viewBounds.width, availHeight / viewBounds.height);
	}

	private float viewOffsetX() {
		return (getWidth() - viewBounds.width * viewScale()) / 2f - viewBounds.x * viewScale();
	}

	private float viewOffsetY() {
		int availHeight = Math.max(1, getHeight() - HUD_BAR_HEIGHT);
		return (availHeight - viewBounds.height * viewScale()) / 2f - viewBounds.y * viewScale();
	}

	private void applyViewTransform(Graphics2D graphics2D) {
		graphics2D.translate(viewOffsetX(), viewOffsetY());
		graphics2D.scale(viewScale(), viewScale());
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public int[][] getTrackMap() {
		return trackMap;
	}

	public void shutdown() {
		renderingEnabled = false;
	}

	@Override
	public void update(Observable observable, Object argument) {
		if (!renderingEnabled || !isDisplayable()) {
			return;
		}
		BufferStrategy bufferStrategy = getBufferStrategy();
		if (bufferStrategy == null) {
			return;
		}
		Graphics graphics = null;

		if (observable.toString().contains(OBSERVER_CAR_TOKEN)) {
			try {
				Car car = (Car) observable;
				BufferedImage sprite = carSprites[car.getSpriteIndex()];
				graphics = bufferStrategy.getDrawGraphics();
				Graphics2D graphics2D = (Graphics2D) graphics;
				graphics2D.setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				applyViewTransform(graphics2D);

				AffineTransform transform = new AffineTransform();
				transform.rotate(
						car.getAngle(),
						sprite.getWidth() / (double) SPRITE_CENTER_DIVISOR,
						sprite.getHeight() / (double) SPRITE_CENTER_DIVISOR);
				AffineTransformOp rotation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
				graphics2D.drawImage(
						sprite,
						rotation,
						trackOriginX() + car.getX() - CAR_RENDER_OFFSET[0] / SPRITE_CENTER_DIVISOR,
						trackOriginY() + car.getY() - CAR_RENDER_OFFSET[1] / SPRITE_CENTER_DIVISOR);
				renderFlags[car.getSpriteIndex()] = true;
			} finally {
				if (graphics != null) {
					graphics.dispose();
				}
			}
		} else {
			try {
				Circuit circuit = (Circuit) observable;
				graphics = bufferStrategy.getDrawGraphics();
				Graphics2D graphics2D = (Graphics2D) graphics;
				graphics2D.setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);

				// Letterbox outside the contain-scaled race with terrain tone.
				graphics2D.setColor(RaceSceneryPainter.letterboxColor(terrain));
				graphics2D.fillRect(0, 0, getWidth(), getHeight());

				applyViewTransform(graphics2D);
				graphics2D.drawImage(staticScene(), 0, 0, null);
				paintFinishLine(graphics2D, circuit);

				// HUD stays in screen space under the contain-fitted track.
				graphics2D.setTransform(new AffineTransform());
				UiPainter.paintHudStrip(graphics2D, 0, getHeight() - HUD_BAR_HEIGHT, getWidth(), HUD_BAR_HEIGHT);
				paintHudContent(graphics2D, circuit);
				if (countdownVisible) {
					paintCountdownOverlay(graphics2D);
				}
				renderFlags[renderFlags.length - ONE_BASED_INDEX_OFFSET] = true;
			} finally {
				if (graphics != null) {
					graphics.dispose();
				}
			}
		}

		if (allFlagsSet(renderFlags)) {
			bufferStrategy.show();
			Toolkit.getDefaultToolkit().sync();
			resetFlags(renderFlags);
		}
	}

	private void paintCarsInView(Graphics2D graphics2D) {
		Car[] cars = hudCars;
		if (cars == null) {
			return;
		}
		for (Car car : cars) {
			BufferedImage sprite = carSprites[car.getSpriteIndex()];
			if (sprite == null) {
				continue;
			}
			AffineTransform transform = new AffineTransform();
			transform.rotate(
					car.getAngle(),
					sprite.getWidth() / (double) SPRITE_CENTER_DIVISOR,
					sprite.getHeight() / (double) SPRITE_CENTER_DIVISOR);
			AffineTransformOp rotation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
			graphics2D.drawImage(
					sprite,
					rotation,
					trackOriginX() + car.getX() - CAR_RENDER_OFFSET[0] / SPRITE_CENTER_DIVISOR,
					trackOriginY() + car.getY() - CAR_RENDER_OFFSET[1] / SPRITE_CENTER_DIVISOR);
		}
	}

	/**
	 * Centered 3-2-1-GO presentation above the HUD: pop-in scale, soft halo,
	 * outlined yellow type that settles then fades at the end of each step.
	 */
	private void paintCountdownOverlay(Graphics2D graphics2D) {
		String label = countdownLabel;
		if (label == null || label.isEmpty()) {
			return;
		}
		float progress = countdownProgress;
		float pop = progress < 0.28f ? 1f - progress / 0.28f : 0f;
		float scale = 1f + 0.42f * pop * pop;
		float fade = progress > 0.82f ? Math.max(0f, 1f - (progress - 0.82f) / 0.18f) : 1f;
		float alpha = Math.max(0f, Math.min(1f, fade));

		int centerX = getWidth() / 2;
		int centerY = Math.max(1, getHeight() - HUD_BAR_HEIGHT) / 2;
		float baseSize = countdownGoStep ? 86f : 108f;
		Font font = GameTheme.FONT_TITLE.deriveFont(Font.BOLD, baseSize * scale);
		graphics2D.setFont(font);
		FontMetrics metrics = graphics2D.getFontMetrics();
		int textWidth = metrics.stringWidth(label);
		int textX = centerX - textWidth / 2;
		int textY = centerY + (metrics.getAscent() - metrics.getDescent()) / 2;

		Graphics2D overlay = (Graphics2D) graphics2D.create();
		overlay.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		overlay.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		float ringRadius = Math.max(textWidth, metrics.getHeight()) * (0.72f + 0.18f * pop);
		overlay.setColor(new Color(0, 0, 0, 90));
		overlay.fill(new Ellipse2D.Float(
				centerX - ringRadius,
				centerY - ringRadius * 0.72f,
				ringRadius * 2f,
				ringRadius * 1.44f));

		Color halo = countdownGoStep
				? new Color(255, 230, 90, 55)
				: new Color(90, 170, 255, 45);
		overlay.setColor(halo);
		overlay.setStroke(new BasicStroke(10f + 14f * pop, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		overlay.draw(new Ellipse2D.Float(
				centerX - ringRadius * 0.85f,
				centerY - ringRadius * 0.62f,
				ringRadius * 1.7f,
				ringRadius * 1.24f));

		overlay.setColor(new Color(0, 0, 0, 160));
		overlay.drawString(label, textX + 4, textY + 5);

		Color fill = countdownGoStep ? GameTheme.ACCENT_YELLOW : GameTheme.TEXT_PRIMARY;
		overlay.setColor(new Color(20, 28, 44));
		for (int ox = -3; ox <= 3; ox++) {
			for (int oy = -3; oy <= 3; oy++) {
				if (ox == 0 && oy == 0) {
					continue;
				}
				overlay.drawString(label, textX + ox, textY + oy);
			}
		}
		overlay.setColor(fill);
		overlay.drawString(label, textX, textY);
		overlay.dispose();
	}

	/**
	 * Static track layer in track-local pixels (tile grid). The live view
	 * contain-fits {@link #viewBounds} into the canvas above the HUD.
	 */
	private BufferedImage staticScene() {
		int trackPixelWidth = mapDimensions[1] * TILE_SIZE;
		int trackPixelHeight = mapDimensions[0] * TILE_SIZE;
		if (staticScene != null
				&& staticScene.getWidth() == trackPixelWidth
				&& staticScene.getHeight() == trackPixelHeight) {
			return staticScene;
		}

		BufferedImage scene = new BufferedImage(trackPixelWidth, trackPixelHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = scene.createGraphics();
		UiPainter.enableQuality(graphics2D);
		int seed = RaceSceneryPainter.seedFor(trackMap);

		RaceSceneryPainter.paintGround(graphics2D, trackPixelWidth, trackPixelHeight, terrain, seed ^ 0x51ED);

		// Block flora on the asphalt ribbon only (not whole tiles), so corner
		// runoff and the outer belt can still get scenery.
		Path2D unitPath = TrackGeometry.buildPreviewPath(trackMap);
		Shape centerline = AffineTransform.getScaleInstance(TILE_SIZE, TILE_SIZE)
				.createTransformedShape(unitPath);
		Shape asphaltRibbon = new BasicStroke(
				Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND).createStrokedShape(centerline);
		RaceSceneryPainter.paintFlora(
				graphics2D,
				trackPixelWidth,
				trackPixelHeight,
				asphaltRibbon,
				terrain,
				seed ^ 0xC0FFEE,
				OUTER_TREE_COUNT,
				0.045,
				0.095);
		RaceSceneryPainter.paintFloraInOpenTiles(
				graphics2D,
				trackMap,
				TILE_SIZE,
				trackOriginX(),
				trackOriginY(),
				terrain,
				seed ^ 0xBEE5,
				INFIELD_TREE_COUNT);

		for (int column = 0; column < mapDimensions[1]; column++) {
			for (int row = 0; row < mapDimensions[0]; row++) {
				graphics2D.drawImage(
						trackTiles[row][column],
						trackOriginX() + column * TILE_SIZE,
						trackOriginY() + row * TILE_SIZE,
						null);
			}
		}

		paintStartGrid(graphics2D);
		graphics2D.dispose();
		staticScene = scene;
		return staticScene;
	}

	/**
	 * Checkered start/finish band across the start lane, centered on the
	 * finish-line geometry used for lap detection.
	 */
	private void paintFinishLine(Graphics2D graphics2D, Circuit circuit) {
		if (circuit == null || circuit.getFinishLine() == null) {
			return;
		}
		int x1 = trackOriginX() + Math.round((float) circuit.getFinishLine().getX1());
		int x2 = trackOriginX() + Math.round((float) circuit.getFinishLine().getX2());
		int y = trackOriginY() + Math.round((float) circuit.getFinishLine().getY1());
		int left = Math.min(x1, x2);
		int width = Math.abs(x2 - x1);
		if (width < 1) {
			return;
		}
		int top = y - FINISH_BAND_HEIGHT / 2;
		int cellWidth = Math.max(1, width / FINISH_CHECKER_COLS);
		int cellHeight = Math.max(1, FINISH_BAND_HEIGHT / FINISH_CHECKER_ROWS);

		Object previousAa = graphics2D.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		for (int row = 0; row < FINISH_CHECKER_ROWS; row++) {
			for (int col = 0; col < FINISH_CHECKER_COLS; col++) {
				int cellX = left + col * cellWidth;
				int cellW = (col == FINISH_CHECKER_COLS - 1) ? (left + width - cellX) : cellWidth;
				boolean dark = ((row + col) & 1) == 0;
				graphics2D.setColor(dark ? FINISH_CHECKER_DARK : FINISH_CHECKER_LIGHT);
				graphics2D.fillRect(cellX, top + row * cellHeight, cellW, cellHeight);
			}
		}
		graphics2D.setColor(FINISH_EDGE_ACCENT);
		graphics2D.fillRect(left, top - 1, width, 1);
		graphics2D.fillRect(left, top + FINISH_BAND_HEIGHT, width, 1);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
	}

	/**
	 * Draws one grid marker per start slot as soft painted brackets on the
	 * asphalt: a chalk-white stroke over a faint dark underlay so the marks
	 * read as worn track paint instead of UI chrome.
	 */
	private void paintStartGrid(Graphics2D graphics2D) {
		Object previousAa = graphics2D.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int slotIndex = 0; slotIndex < START_SLOT_COUNT; slotIndex++) {
			int centerX = trackOriginX() + Math.round(Circuit.startSlotCenterX(slotIndex));
			int noseY = trackOriginY() + Math.round(Circuit.startSlotCenterY(slotIndex))
					- START_MARKER_NOSE_OFFSET;
			int leftX = centerX - START_MARKER_HALF_WIDTH;
			int rightX = centerX + START_MARKER_HALF_WIDTH;
			paintStartMarkerStroke(graphics2D, leftX, rightX, noseY, START_MARKER_SHADOW, 3);
			paintStartMarkerStroke(graphics2D, leftX, rightX, noseY, START_MARKER_PAINT, START_MARKER_STROKE);
		}
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
	}

	private void paintStartMarkerStroke(
			Graphics2D graphics2D,
			int leftX,
			int rightX,
			int noseY,
			Color color,
			int stroke) {
		graphics2D.setColor(color);
		int width = rightX - leftX;
		graphics2D.fillRoundRect(leftX, noseY, width, stroke, stroke, stroke);
		graphics2D.fillRoundRect(leftX, noseY, stroke, START_MARKER_TICK_LENGTH, stroke, stroke);
		graphics2D.fillRoundRect(
				rightX - stroke,
				noseY,
				stroke,
				START_MARKER_TICK_LENGTH,
				stroke,
				stroke);
	}

	/**
	 * HUD line in screen space: race timer on the left, lap counters on the
	 * right, tinted with each car's livery color.
	 */
	private void paintHudContent(Graphics2D graphics2D, Circuit circuit) {
		graphics2D.setFont(hudFont);
		java.awt.FontMetrics metrics = graphics2D.getFontMetrics();
		int hudTop = getHeight() - HUD_BAR_HEIGHT;
		int baseline = hudTop + (HUD_BAR_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		String timerText = HUD_RACE_TIME_PREFIX
				+ ((int) circuit.getRaceTimeMs() / MS_PER_SECOND)
				+ HUD_TIME_SUFFIX;
		graphics2D.drawString(timerText, HUD_SIDE_PADDING, baseline);

		Car[] cars = hudCars;
		if (cars == null) {
			return;
		}
		String[] lapTexts = new String[cars.length];
		int chipsWidth = 0;
		for (int index = 0; index < cars.length; index++) {
			lapTexts[index] = lapCounterText(cars[index], index);
			chipsWidth += metrics.stringWidth(lapTexts[index]);
			if (index > 0) {
				chipsWidth += HUD_LAP_CHIP_GAP;
			}
		}
		int rightEdge = getWidth() - HUD_SIDE_PADDING;
		int chipX = Math.max(HUD_SIDE_PADDING + metrics.stringWidth(timerText) + HUD_LAP_CHIP_GAP, rightEdge - chipsWidth);
		for (int index = 0; index < cars.length; index++) {
			graphics2D.setColor(GameCatalog.carModelColor(cars[index].getModelIndex()));
			graphics2D.drawString(lapTexts[index], chipX, baseline);
			chipX += metrics.stringWidth(lapTexts[index]) + HUD_LAP_CHIP_GAP;
		}
	}

	private String lapCounterText(Car car, int slotIndex) {
		String label = HUD_AI_PLAYER_NAME.equals(car.getName())
				? HUD_AI_PLAYER_PREFIX + (slotIndex + ONE_BASED_INDEX_OFFSET)
				: HUD_HUMAN_PLAYER_PREFIX + car.getName();
		// lapCount is finish crossings (start line counts once). Display the
		// lap the car is currently racing, capped at the configured total.
		int currentLap = Math.min(Math.max(car.getLapCount(), 1), Math.max(totalLaps, 1));
		return label + HUD_SLOT_SEPARATOR + currentLap + HUD_LAP_SEPARATOR + totalLaps;
	}

	private static boolean allFlagsSet(boolean[] flags) {
		for (boolean flag : flags) {
			if (!flag) {
				return false;
			}
		}
		return true;
	}

	private static void resetFlags(boolean[] flags) {
		for (int index = 0; index < flags.length; index++) {
			flags[index] = false;
		}
	}

	private BufferedImage[][] loadTrackTiles(int[][] map) {
		BufferedImage[][] tiles = new BufferedImage[mapDimensions[0]][mapDimensions[1]];
		for (int column = 0; column < mapDimensions[1]; column++) {
			for (int row = 0; row < mapDimensions[0]; row++) {
				try {
					tiles[row][column] = ResourcePaths.loadTrackTile(map[row][column]);
				} catch (Exception exception) {
					System.err.println(ERROR_TRACK_TILES + exception.getMessage());
				}
			}
		}
		return tiles;
	}
}
