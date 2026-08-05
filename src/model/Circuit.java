package model;

import java.awt.geom.Line2D;
import java.util.Observable;

import controller.Game;
import view.GameFrame;

public class Circuit extends Observable {

	public static final int TRACK_COUNT = GameSettings.TRACK_NAMES.length;
	public static final int START_SLOT_COUNT = GameSettings.MAX_CARS;

	public static final int TILE_STRAIGHT_HORIZONTAL = 1;
	public static final int TILE_STRAIGHT_VERTICAL = 2;
	public static final int TILE_CORNER_BOTTOM_RIGHT = 3;
	public static final int TILE_CORNER_TOP_RIGHT = 4;
	public static final int TILE_CORNER_TOP_LEFT = 5;
	public static final int TILE_CORNER_BOTTOM_LEFT = 6;
	public static final int TILE_OPEN = 7;

	private static final int INNER_RADIUS = 26;
	private static final int OUTER_RADIUS = 191;
	private static final double FINISH_LINE_LEFT_OFFSET = -122;
	private static final double FINISH_LINE_RIGHT_OFFSET = 43;
	private static final double FINISH_LINE_Y_OFFSET = -50;
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

	public static final float[][][] START_POSITIONS = {
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}}
	};

	private double raceTimeMs;
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

	public void initializeFinishLine(int trackNumber) {
		float startX = Circuit.START_POSITIONS[trackNumber - ONE_BASED_INDEX_OFFSET][0][0];
		float startY = Circuit.START_POSITIONS[trackNumber - ONE_BASED_INDEX_OFFSET][0][1];
		finishLine = new Line2D.Float(
				(float) (startX + FINISH_LINE_LEFT_OFFSET),
				(float) (startY + FINISH_LINE_Y_OFFSET),
				(float) (startX + FINISH_LINE_RIGHT_OFFSET),
				(float) (startY + FINISH_LINE_Y_OFFSET));
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
		setChanged();
		notifyObservers();
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
				car.translateBy(
						(float) -Math.signum(localPosition[0] - GameFrame.TILE_SIZE / TILE_CENTER_DIVISOR),
						(float) -Math.signum(localPosition[1] - GameFrame.TILE_SIZE / TILE_CENTER_DIVISOR));
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

	public int getTileType(Car car) {
		int[] gridCell = getGridCoordinates(car);
		return trackMap[gridCell[0]][gridCell[1]];
	}

	public int[] getGridCoordinates(Car car) {
		int row = (int) (1.0 * mapDimensions[0] * car.getY() / frameDimensions[1]);
		int column = (int) (1.0 * mapDimensions[1] * car.getX() / frameDimensions[0]);
		return new int[] {row, column};
	}
}
