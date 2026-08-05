package model;

import java.awt.geom.Line2D;
import java.util.Observable;

import controller.Game;
import view.GameFrame;

public class Circuit extends Observable {

	public static final int TRACK_COUNT = GameConfig.TRACK_NAMES.length;
	public static final int START_SLOT_COUNT = GameConfig.MAX_CARS;

	/** Tile type ids are zero-based and match {@code track_XX.png} filenames. */
	public static final int TILE_STRAIGHT_HORIZONTAL = 0;
	public static final int TILE_STRAIGHT_VERTICAL = 1;
	public static final int TILE_CORNER_BOTTOM_RIGHT = 2;
	public static final int TILE_CORNER_TOP_RIGHT = 3;
	public static final int TILE_CORNER_TOP_LEFT = 4;
	public static final int TILE_CORNER_BOTTOM_LEFT = 5;
	public static final int TILE_OPEN = 6;

	public static final int INNER_RADIUS = 26;
	public static final int OUTER_RADIUS = 191;
	private static final double FINISH_LINE_RESET_DISTANCE = 3;
	private static final double FINISH_LINE_CROSSING_DISTANCE = 3;
	private static final double OFF_TRACK_SPEED_FACTOR = -0.2;
	private static final double TILE_CENTER_DIVISOR = 2.0;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final String LOG_TRACK_ROWS = "Track rows: ";
	private static final String LOG_TRACK_COLUMNS = ", columns: ";
	private static final String LOG_FRAME_WIDTH = "Frame width: ";
	private static final String LOG_FRAME_HEIGHT = ", height: ";
	private static final String ERROR_LEFT_TRACK = "Car left the playable area.";

	/**
	 * Start grid geometry, derived from the track layout instead of magic
	 * offsets: every track starts on the vertical lane of the left column
	 * (row 1), cars face up, and the finish line sits on the tile boundary
	 * just above the grid.
	 */
	private static final double LANE_CENTER_PX = (INNER_RADIUS + OUTER_RADIUS) / 2.0;
	private static final float START_SLOT_STAGGER_PX = 34f;
	private static final float START_GRID_TOP_GAP_PX = 56f;
	private static final float START_SLOT_SPACING_PX = 50f;
	public static final int CAR_ANCHOR_HALF_WIDTH_PX = 19;
	public static final int CAR_ANCHOR_HALF_HEIGHT_PX = 10;

	/**
	 * Sprite anchor (top-left) start position per track and slot, in pixels.
	 */
	public static final float[][][] START_POSITIONS = buildStartPositions();

	private static float[][][] buildStartPositions() {
		float[][][] positions = new float[TRACK_COUNT][START_SLOT_COUNT][2];
		for (int trackIndex = 0; trackIndex < TRACK_COUNT; trackIndex++) {
			for (int slotIndex = 0; slotIndex < START_SLOT_COUNT; slotIndex++) {
				positions[trackIndex][slotIndex][0] =
						startSlotCenterX(slotIndex) - CAR_ANCHOR_HALF_WIDTH_PX;
				positions[trackIndex][slotIndex][1] =
						startSlotCenterY(slotIndex) - CAR_ANCHOR_HALF_HEIGHT_PX;
			}
		}
		return positions;
	}

	/** Pole position sits on the inside of the first corner (right of the lane center). */
	public static float startSlotCenterX(int slotIndex) {
		float stagger = slotIndex % 2 == 0 ? START_SLOT_STAGGER_PX : -START_SLOT_STAGGER_PX;
		return (float) (LANE_CENTER_PX + stagger);
	}

	public static float startSlotCenterY(int slotIndex) {
		return GameFrame.TILE_SIZE + START_GRID_TOP_GAP_PX + slotIndex * START_SLOT_SPACING_PX;
	}

	/**
	 * Simulation runs every tick; observers are only notified (and the frame
	 * repainted) every {@code RENDER_TICK_DIVISOR} ticks so the 100 Hz physics
	 * loop is not slowed down by full-scene rendering.
	 */
	public static final int RENDER_TICK_DIVISOR = 2;

	private double raceTimeMs;
	private long tickCount;
	private boolean renderTick;
	private final int[][] trackMap;
	private final int[] mapDimensions;
	private final int[] frameDimensions;
	private Line2D finishLine;

	public Circuit(GameFrame frame, int[][] trackMap) {
		this.trackMap = trackMap;
		addObserver(frame);
		raceTimeMs = 0.0;
		mapDimensions = new int[] {trackMap.length, trackMap[0].length};
		frameDimensions = new int[] {
				GameFrame.TILE_SIZE * mapDimensions[1],
				GameFrame.TILE_SIZE * mapDimensions[0]
		};
		System.out.println(LOG_TRACK_ROWS + mapDimensions[0] + LOG_TRACK_COLUMNS + mapDimensions[1]);
		System.out.println(LOG_FRAME_WIDTH + frameDimensions[0] + LOG_FRAME_HEIGHT + frameDimensions[1]);
	}

	/**
	 * The finish line spans the start lane exactly (inner to outer wall) on
	 * the tile boundary just above the start grid.
	 */
	public void initializeFinishLine(int trackNumber) {
		finishLine = new Line2D.Float(
				INNER_RADIUS,
				GameFrame.TILE_SIZE,
				OUTER_RADIUS,
				GameFrame.TILE_SIZE);
	}

	public int crossFinishLine(Car car) {
		int crossingDirection = 0;
		if (car.hasCrossedFinishLine()) {
			if (finishLine.ptLineDist(car.getX(), car.getY()) > FINISH_LINE_RESET_DISTANCE) {
				car.toggleFinishLineFlag();
			}
		} else if (finishLine.ptLineDist(car.getX(), car.getY()) < FINISH_LINE_CROSSING_DISTANCE
				&& finishLine.getP1().distance(car.getX(), car.getY()) < GameFrame.TILE_SIZE) {
			crossingDirection = (int) -Math.signum(car.getSpeed() * Math.sin(car.getAngle()));
			car.toggleFinishLineFlag();
		}
		return crossingDirection;
	}

	public void tick() {
		raceTimeMs += Game.TICK_INTERVAL_MS;
		tickCount++;
		renderTick = tickCount % RENDER_TICK_DIVISOR == 0;
		if (renderTick) {
			setChanged();
			notifyObservers();
		}
	}

	/**
	 * @return whether observers should repaint on the current tick
	 */
	public boolean isRenderTick() {
		return renderTick;
	}

	public double getRaceTimeMs() {
		return raceTimeMs;
	}

	public void enforceTrackBoundaries(Car car) {
		boolean onTrack = true;
		int[] gridCell = getGridCoordinates(car);

		try {
			int tileType = trackMap[gridCell[0]][gridCell[1]];
			float[] localPosition = {
					car.getX() - (float) gridCell[1] * GameFrame.TILE_SIZE,
					car.getY() - (float) gridCell[0] * GameFrame.TILE_SIZE
			};

			switch (tileType) {
				case TILE_STRAIGHT_HORIZONTAL:
					if (localPosition[0] <= Circuit.INNER_RADIUS || localPosition[0] >= Circuit.OUTER_RADIUS) {
						onTrack = false;
					}
					break;
				case TILE_STRAIGHT_VERTICAL:
					if (localPosition[1] <= Circuit.INNER_RADIUS || localPosition[1] >= Circuit.OUTER_RADIUS) {
						onTrack = false;
					}
					break;
				case TILE_CORNER_BOTTOM_RIGHT:
					onTrack = isInsideCircularCorner(localPosition[0], localPosition[1], 0, GameFrame.TILE_SIZE);
					break;
				case TILE_CORNER_TOP_RIGHT:
					onTrack = isInsideCircularCorner(
							localPosition[0], localPosition[1], GameFrame.TILE_SIZE, GameFrame.TILE_SIZE);
					break;
				case TILE_CORNER_TOP_LEFT:
					onTrack = isInsideCircularCorner(
							localPosition[0], localPosition[1], GameFrame.TILE_SIZE, 0);
					break;
				case TILE_CORNER_BOTTOM_LEFT:
					onTrack = isInsideCircularCorner(localPosition[0], localPosition[1], 0, 0);
					break;
				case TILE_OPEN:
				default:
					break;
			}

			if (!onTrack) {
				car.setSpeed((float) (OFF_TRACK_SPEED_FACTOR * car.getSpeed()));
				car.translateByMeters(
						-WorldUnits.pxToM(Math.signum(localPosition[0] - GameFrame.TILE_SIZE / TILE_CENTER_DIVISOR)),
						-WorldUnits.pxToM(Math.signum(localPosition[1] - GameFrame.TILE_SIZE / TILE_CENTER_DIVISOR)));
			}
		} catch (RuntimeException exception) {
			System.err.println(ERROR_LEFT_TRACK);
			System.err.println(exception.getMessage());
			System.err.println("==============");
		}
	}

	public Line2D getFinishLine() {
		return finishLine;
	}

	private boolean isInsideCircularCorner(float x, float y, int cornerX, int cornerY) {
		double radius = Math.sqrt(Math.pow(x - cornerX, 2) + Math.pow(y - cornerY, 2));
		return radius > Circuit.INNER_RADIUS && radius < Circuit.OUTER_RADIUS;
	}

	public int[] getGridCoordinates(Car car) {
		int row = (int) (1.0 * mapDimensions[0] * car.getY() / frameDimensions[1]);
		int column = (int) (1.0 * mapDimensions[1] * car.getX() / frameDimensions[0]);
		return new int[] {row, column};
	}

	public int[][] getTrackMap() {
		return trackMap;
	}
}
