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
import model.GameCatalog;
import model.ResourcePaths;
import view.theme.GameTheme;
import view.ui.UiPainter;

public class GameFrame extends JFrame implements Observer {

	public static final int TILE_SIZE = 219;
	public static final int[] CAR_RENDER_OFFSET = {40, 29};

	private static final long serialVersionUID = 1L;
	private static final int FRAME_HORIZONTAL_MARGIN = 20;
	private static final int FRAME_TITLE_BAR_HEIGHT = 50;
	private static final int FRAME_BOTTOM_MARGIN = 20;
	private static final int BUFFER_STRATEGY_BUFFERS = 2;
	private static final int TRACK_TOP_OFFSET = 10;
	private static final int HUD_BAR_HEIGHT = 64;
	private static final int HUD_BADGE_WIDTH = 156;
	private static final int HUD_BADGE_HEIGHT = 30;
	private static final int HUD_BADGE_GAP = 10;
	private static final int START_SLOT_COUNT = Circuit.START_SLOT_COUNT;
	private static final int START_MARKER_LEFT_OFFSET = 20;
	private static final int START_MARKER_RIGHT_OFFSET = 17;
	private static final int HUD_LEFT_OFFSET = 20;
	private static final int HUD_TEXT_TOP_PADDING = 18;
	private static final int SPRITE_CENTER_DIVISOR = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int MS_PER_SECOND = 1000;

	private static final String SPRITE_ICON = "icon.png";
	private static final String SPRITE_TRACK_PREFIX = "track";
	private static final String SPRITE_TRACK_SUFFIX = ".png";
	private static final String HUD_RACE_TIME_PREFIX = "TIME ";
	private static final String HUD_TIME_SUFFIX = "s";
	private static final String HUD_TRACK_SEPARATOR = "  •  ";
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
	private volatile boolean renderingEnabled = true;

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
		setSize(
				FRAME_HORIZONTAL_MARGIN + mapDimensions[1] * TILE_SIZE,
				FRAME_TITLE_BAR_HEIGHT
						+ FRAME_BOTTOM_MARGIN
						+ TRACK_TOP_OFFSET
						+ mapDimensions[0] * TILE_SIZE
						+ HUD_BAR_HEIGHT);
		setVisible(true);
		setResizable(false);
		createBufferStrategy(BUFFER_STRATEGY_BUFFERS);
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
						car.getX() - CAR_RENDER_OFFSET[0] / SPRITE_CENTER_DIVISOR,
						car.getY() - CAR_RENDER_OFFSET[1] / SPRITE_CENTER_DIVISOR);
				graphics2D.drawString(
						car.getName(),
						car.getX() - CAR_RENDER_OFFSET[0] / SPRITE_CENTER_DIVISOR,
						car.getY() - CAR_RENDER_OFFSET[1] / SPRITE_CENTER_DIVISOR);
				int hudBarTop = TRACK_TOP_OFFSET + mapDimensions[0] * TILE_SIZE;
				String badgeText = GameCatalog.carModelName(carModels[car.getSpriteIndex()])
						+ "  P"
						+ car.getName()
						+ "  L"
						+ car.getLapCount();
				UiPainter.paintHudBadge(
						graphics2D,
						HUD_LEFT_OFFSET + car.getSpriteIndex() * (HUD_BADGE_WIDTH + HUD_BADGE_GAP),
						hudBarTop + 26,
						HUD_BADGE_WIDTH,
						HUD_BADGE_HEIGHT,
						badgeText,
						hudFont);
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
				AffineTransformOp identity = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR);
				int trackPixelWidth = mapDimensions[1] * TILE_SIZE;
				int trackPixelHeight = mapDimensions[0] * TILE_SIZE;
				int hudBarTop = TRACK_TOP_OFFSET + trackPixelHeight;

				UiPainter.paintScreenBackdrop(graphics2D, getWidth(), getHeight());
				UiPainter.paintRaceViewportFrame(graphics2D, 0, TRACK_TOP_OFFSET, trackPixelWidth, trackPixelHeight);

				for (int column = 0; column < mapDimensions[1]; column++) {
					for (int row = 0; row < mapDimensions[0]; row++) {
						graphics2D.drawImage(
								trackTiles[row][column],
								identity,
								column * TILE_SIZE,
								TRACK_TOP_OFFSET + row * TILE_SIZE);
					}
				}

				graphics2D.setColor(java.awt.Color.WHITE);
				for (int slotIndex = 0; slotIndex < START_SLOT_COUNT; slotIndex++) {
					float startX = Circuit.START_POSITIONS[trackNumber - ONE_BASED_INDEX_OFFSET][slotIndex][0];
					float startY = Circuit.START_POSITIONS[trackNumber - ONE_BASED_INDEX_OFFSET][slotIndex][1];
					graphics2D.drawLine(
							(int) startX - START_MARKER_LEFT_OFFSET,
							(int) startY - START_MARKER_LEFT_OFFSET,
							(int) startX + START_MARKER_RIGHT_OFFSET,
							(int) startY - START_MARKER_LEFT_OFFSET);
				}
				graphics2D.setColor(java.awt.Color.YELLOW);
				graphics2D.drawLine(
						(int) circuit.getFinishLine().getX1(),
						(int) circuit.getFinishLine().getY1(),
						(int) circuit.getFinishLine().getX2(),
						(int) circuit.getFinishLine().getY2());

				UiPainter.paintHudStrip(graphics2D, 0, hudBarTop, getWidth(), HUD_BAR_HEIGHT);
				graphics2D.setFont(hudFont);
				graphics2D.setColor(GameTheme.TEXT_PRIMARY);
				String timerText = HUD_RACE_TIME_PREFIX
						+ ((int) circuit.getRaceTimeMs() / MS_PER_SECOND)
						+ HUD_TIME_SUFFIX
						+ HUD_TRACK_SEPARATOR
						+ GameCatalog.trackName(trackNumber);
				graphics2D.drawString(timerText, HUD_LEFT_OFFSET, hudBarTop + HUD_TEXT_TOP_PADDING);
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
