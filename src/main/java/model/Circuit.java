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
	/** Extra width beyond the painted finish segment so wall-hugging cars still score. */
	private static final double FINISH_LINE_LATERAL_MARGIN_PX = 8;
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

	/**
	 * Updates lap progress when {@code car} crosses the finish segment.
	 * Counts only a real side-to-side transition through the start lane:
	 * start/grid side → far side is {@code +1}, the reverse is {@code -1}.
	 * Proximity alone (weaving near the line after a collision) does not score.
	 */
	public int crossFinishLine(Car car) {
		if (finishLine == null) {
			return 0;
		}

		double centerX = WorldUnits.mToPx(car.getPositionXMeters()) + CAR_ANCHOR_HALF_WIDTH_PX;
		double centerY = WorldUnits.mToPx(car.getPositionYMeters()) + CAR_ANCHOR_HALF_HEIGHT_PX;
		double lineY = finishLine.getY1();
		double laneLeft = Math.min(finishLine.getX1(), finishLine.getX2()) - FINISH_LINE_LATERAL_MARGIN_PX;
		double laneRight = Math.max(finishLine.getX1(), finishLine.getX2()) + FINISH_LINE_LATERAL_MARGIN_PX;
		boolean inLane = centerX >= laneLeft && centerX <= laneRight;

		int side = 0;
		if (centerY > lineY) {
			side = 1;
		} else if (centerY < lineY) {
			side = -1;
		}

		int previousSide = car.getFinishLineSide();
		int lapDelta = 0;
		if (inLane && previousSide != 0 && side != 0 && previousSide != side) {
			lapDelta = previousSide > 0 ? 1 : -1;
		}
		if (side != 0) {
			car.setFinishLineSide(side);
		}
		return lapDelta;
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
	 * Advances the render cadence without charging race time - used during the
	 * pre-race countdown while the track is already on screen.
	 *
	 * @return {@code true} when this visual tick should repaint
	 */
	public boolean shouldRenderAfterVisualTick() {
		tickCount++;
		renderTick = tickCount % RENDER_TICK_DIVISOR == 0;
		return renderTick;
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

	/**
	 * Legacy helper retained for tests: applies the infinite-mass wall response
	 * used by {@link PhysicsSimulator}.
	 */
	public void enforceTrackBoundaries(Car car) {
		PhysicsSimulator.resolveWallCollision(car, this);
	}

	/**
	 * Probes whether {@code car} is off asphalt and, if so, returns a contact
	 * with an inward unit normal (onto the track) and a penetration depth.
	 * Walls are treated as infinite-mass obstacles by the physics step.
	 */
	public WallContact findWallContact(Car car) {
		int[] gridCell;
		try {
			gridCell = getGridCoordinates(car);
			int tileType = trackMap[gridCell[0]][gridCell[1]];
			float localX = car.getX() - (float) gridCell[1] * GameFrame.TILE_SIZE;
			float localY = car.getY() - (float) gridCell[0] * GameFrame.TILE_SIZE;
			return wallContactForTile(tileType, localX, localY);
		} catch (RuntimeException exception) {
			System.err.println(ERROR_LEFT_TRACK);
			System.err.println(exception.getMessage());
			System.err.println("==============");
			return null;
		}
	}

	private WallContact wallContactForTile(int tileType, float localX, float localY) {
		switch (tileType) {
			case TILE_STRAIGHT_HORIZONTAL:
				if (localX <= INNER_RADIUS) {
					return new WallContact(1.0, 0.0, WorldUnits.pxToM(INNER_RADIUS - localX + 1.0));
				}
				if (localX >= OUTER_RADIUS) {
					return new WallContact(-1.0, 0.0, WorldUnits.pxToM(localX - OUTER_RADIUS + 1.0));
				}
				return null;
			case TILE_STRAIGHT_VERTICAL:
				if (localY <= INNER_RADIUS) {
					return new WallContact(0.0, 1.0, WorldUnits.pxToM(INNER_RADIUS - localY + 1.0));
				}
				if (localY >= OUTER_RADIUS) {
					return new WallContact(0.0, -1.0, WorldUnits.pxToM(localY - OUTER_RADIUS + 1.0));
				}
				return null;
			case TILE_CORNER_BOTTOM_RIGHT:
				return circularCornerContact(localX, localY, 0, GameFrame.TILE_SIZE);
			case TILE_CORNER_TOP_RIGHT:
				return circularCornerContact(localX, localY, GameFrame.TILE_SIZE, GameFrame.TILE_SIZE);
			case TILE_CORNER_TOP_LEFT:
				return circularCornerContact(localX, localY, GameFrame.TILE_SIZE, 0);
			case TILE_CORNER_BOTTOM_LEFT:
				return circularCornerContact(localX, localY, 0, 0);
			case TILE_OPEN:
			default:
				return null;
		}
	}

	private WallContact circularCornerContact(float localX, float localY, int cornerX, int cornerY) {
		double deltaX = localX - cornerX;
		double deltaY = localY - cornerY;
		double radius = Math.hypot(deltaX, deltaY);
		if (radius <= 1e-6) {
			return null;
		}
		double normalX = deltaX / radius;
		double normalY = deltaY / radius;
		if (radius < INNER_RADIUS) {
			return new WallContact(
					normalX,
					normalY,
					WorldUnits.pxToM(INNER_RADIUS - radius + 1.0));
		}
		if (radius > OUTER_RADIUS) {
			return new WallContact(
					-normalX,
					-normalY,
					WorldUnits.pxToM(radius - OUTER_RADIUS + 1.0));
		}
		return null;
	}

	/**
	 * Infinite-mass wall contact. {@code normalX/normalY} point back onto the
	 * asphalt; {@code penetrationMeters} is how far to push along that normal.
	 */
	public record WallContact(double normalX, double normalY, double penetrationMeters) {
	}

	public Line2D getFinishLine() {
		return finishLine;
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
