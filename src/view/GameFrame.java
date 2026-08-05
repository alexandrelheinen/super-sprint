package view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import model.Car;
import model.Circuit;
import model.ResourcePaths;
import view.theme.GameTheme;
import view.ui.UiPainter;

public class GameFrame extends JFrame implements Observer {

	public static final int TILE_SIZE = 219;
	public static final int[] CAR_RENDER_OFFSET = {0, 0};

	private static final long serialVersionUID = 1L;
	private static final int BUFFER_STRATEGY_BUFFERS = 2;
	/** Breathing room between the track walls and the window border. */
	private static final int TRACK_MARGIN = 28;
	private static final int HUD_BAR_HEIGHT = 80;
	private static final int START_SLOT_COUNT = Circuit.START_SLOT_COUNT;
	// Cars face up on the grid, so their on-screen width is the sprite height
	// and their nose sits half a sprite width above the slot center.
	private static final int START_MARKER_HALF_WIDTH = Circuit.CAR_ANCHOR_HALF_HEIGHT_PX + 6;
	private static final int START_MARKER_NOSE_OFFSET = Circuit.CAR_ANCHOR_HALF_WIDTH_PX + 6;
	private static final int START_MARKER_TICK_LENGTH = 8;
	private static final int HUD_SIDE_PADDING = 24;
	private static final int HUD_LAP_CHIP_GAP = 18;
	private static final int SPRITE_CENTER_DIVISOR = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int MS_PER_SECOND = 1000;

	/** HUD text colors matching the car sprite liveries (models 1-4). */
	private static final java.awt.Color[] CAR_MODEL_HUD_COLORS = {
			new java.awt.Color(110, 170, 255),
			new java.awt.Color(225, 205, 100),
			new java.awt.Color(130, 205, 130),
			new java.awt.Color(240, 110, 110)
	};

	private static final String SPRITE_ICON = "icon.png";
	private static final String SPRITE_TRACK_PREFIX = "track";
	private static final String SPRITE_TRACK_SUFFIX = ".png";
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
	private BufferedImage staticScene;
	private volatile boolean renderingEnabled = true;
	private volatile Car[] hudCars;
	private volatile int totalLaps;

	public GameFrame(String title, int[] carModels, int[][] trackMap, int trackNumber) {
		super(title);
		this.trackMap = trackMap;
		this.trackNumber = trackNumber;
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

		setIconImage(new ImageIcon(ResourcePaths.bundledSprite(SPRITE_ICON)).getImage());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		java.awt.Insets insets = getInsets();
		setSize(
				insets.left + insets.right + 2 * TRACK_MARGIN + mapDimensions[1] * TILE_SIZE,
				insets.top + insets.bottom
						+ 2 * TRACK_MARGIN
						+ mapDimensions[0] * TILE_SIZE
						+ HUD_BAR_HEIGHT);
		setLocationRelativeTo(null);
		setVisible(true);
		setResizable(false);
		createBufferStrategy(BUFFER_STRATEGY_BUFFERS);
	}

	/**
	 * Registers the cars and lap target so the HUD can show per-car lap counts.
	 */
	public void attachRaceStatus(Car[] cars, int lapCount) {
		hudCars = cars.clone();
		totalLaps = lapCount;
	}

	private int trackOriginX() {
		return getInsets().left + TRACK_MARGIN;
	}

	private int trackOriginY() {
		return getInsets().top + TRACK_MARGIN;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public int[][] getTrackMap() {
		return trackMap;
	}

	public void shutdown() {
		renderingEnabled = false;
		setVisible(false);
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

				graphics2D.drawImage(staticScene(), 0, 0, null);

				graphics2D.setColor(java.awt.Color.YELLOW);
				graphics2D.drawLine(
						trackOriginX() + (int) circuit.getFinishLine().getX1(),
						trackOriginY() + (int) circuit.getFinishLine().getY1(),
						trackOriginX() + (int) circuit.getFinishLine().getX2(),
						trackOriginY() + (int) circuit.getFinishLine().getY2());

				paintHudContent(graphics2D, circuit);
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

	/**
	 * Everything that never changes during a race (backdrop, viewport frame,
	 * track tiles, start markers, HUD strip) is rendered once into an
	 * offscreen image. Repainting it on every simulation tick previously took
	 * longer than the tick interval, so the race ran in slow motion.
	 */
	private BufferedImage staticScene() {
		if (staticScene != null
				&& staticScene.getWidth() == getWidth()
				&& staticScene.getHeight() == getHeight()) {
			return staticScene;
		}

		BufferedImage scene = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = scene.createGraphics();
		int trackPixelWidth = mapDimensions[1] * TILE_SIZE;
		int trackPixelHeight = mapDimensions[0] * TILE_SIZE;

		UiPainter.paintScreenBackdrop(graphics2D, getWidth(), getHeight());
		UiPainter.paintRaceViewportFrame(graphics2D, trackOriginX(), trackOriginY(), trackPixelWidth, trackPixelHeight);

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

		UiPainter.paintHudStrip(graphics2D, 0, hudBarTop(), getWidth(), HUD_BAR_HEIGHT);
		graphics2D.dispose();
		staticScene = scene;
		return staticScene;
	}

	/**
	 * Draws one grid marker per start slot: a line just ahead of the car's
	 * nose (cars face up) with a short tick trailing down each side.
	 */
	private void paintStartGrid(Graphics2D graphics2D) {
		graphics2D.setColor(java.awt.Color.WHITE);
		for (int slotIndex = 0; slotIndex < START_SLOT_COUNT; slotIndex++) {
			int centerX = trackOriginX() + Math.round(Circuit.startSlotCenterX(slotIndex));
			int noseY = trackOriginY() + Math.round(Circuit.startSlotCenterY(slotIndex))
					- START_MARKER_NOSE_OFFSET;
			int leftX = centerX - START_MARKER_HALF_WIDTH;
			int rightX = centerX + START_MARKER_HALF_WIDTH;
			graphics2D.fillRect(leftX, noseY, rightX - leftX, 2);
			graphics2D.fillRect(leftX, noseY, 2, START_MARKER_TICK_LENGTH);
			graphics2D.fillRect(rightX - 2, noseY, 2, START_MARKER_TICK_LENGTH);
		}
	}

	private int hudBarTop() {
		return trackOriginY() + mapDimensions[0] * TILE_SIZE + TRACK_MARGIN;
	}

	/**
	 * HUD line: race timer and track name on the left, one lap counter per
	 * car on the right, tinted with the car's livery color. Text is
	 * vertically centered in the HUD strip.
	 */
	private void paintHudContent(Graphics2D graphics2D, Circuit circuit) {
		graphics2D.setFont(hudFont);
		java.awt.FontMetrics metrics = graphics2D.getFontMetrics();
		int baseline = hudBarTop() + (HUD_BAR_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		String timerText = HUD_RACE_TIME_PREFIX
				+ ((int) circuit.getRaceTimeMs() / MS_PER_SECOND)
				+ HUD_TIME_SUFFIX;
		graphics2D.drawString(timerText, trackOriginX(), baseline);

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
		int rightEdge = getWidth() - getInsets().right - HUD_SIDE_PADDING;
		int chipX = Math.max(trackOriginX() + metrics.stringWidth(timerText) + HUD_LAP_CHIP_GAP, rightEdge - chipsWidth);
		for (int index = 0; index < cars.length; index++) {
			graphics2D.setColor(CAR_MODEL_HUD_COLORS[
					(cars[index].getModelIndex() - ONE_BASED_INDEX_OFFSET) % CAR_MODEL_HUD_COLORS.length]);
			graphics2D.drawString(lapTexts[index], chipX, baseline);
			chipX += metrics.stringWidth(lapTexts[index]) + HUD_LAP_CHIP_GAP;
		}
	}

	private String lapCounterText(Car car, int slotIndex) {
		String label = HUD_AI_PLAYER_NAME.equals(car.getName())
				? HUD_AI_PLAYER_PREFIX + (slotIndex + ONE_BASED_INDEX_OFFSET)
				: HUD_HUMAN_PLAYER_PREFIX + car.getName();
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
					tiles[row][column] = ImageIO.read(new File(ResourcePaths.bundledSprite(
							SPRITE_TRACK_PREFIX + map[row][column] + SPRITE_TRACK_SUFFIX)));
				} catch (Exception exception) {
					System.err.println(ERROR_TRACK_TILES + exception.getMessage());
				}
			}
		}
		return tiles;
	}
}
