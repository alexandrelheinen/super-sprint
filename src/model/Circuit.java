package model;

import java.awt.geom.Line2D;
import java.util.Observable;

import controller.Game;
import view.GameFrame;

public class Circuit extends Observable {

	public static final int TRACK_COUNT = 4;
	public static final float[][][] START_POSITIONS = {
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}},
			{{147, 290}, {75, 340}, {147, 390}, {75, 440}}
	};

	private static final int INNER_RADIUS = 26;
	private static final int OUTER_RADIUS = 191;

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
		System.out.println("Track rows: " + mapDimensions[0] + ", columns: " + mapDimensions[1]);
		System.out.println("Frame width: " + frameDimensions[0] + ", height: " + frameDimensions[1]);
	}

	public void initializeFinishLine(int trackNumber) {
		float startX = Circuit.START_POSITIONS[trackNumber - 1][0][0];
		float startY = Circuit.START_POSITIONS[trackNumber - 1][0][1];
		finishLine = new Line2D.Float(startX - 122, startY - 50, startX + 43, startY - 50);
	}

	public int crossFinishLine(Car car) {
		int crossingDirection = 0;
		if (car.hasCrossedFinishLine()) {
			if (finishLine.ptLineDist(car.getX(), car.getY()) > 3) {
				car.toggleFinishLineFlag();
			}
		} else if (finishLine.ptLineDist(car.getX(), car.getY()) < 3
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
				case 1:
					if (localPosition[0] <= Circuit.INNER_RADIUS || localPosition[0] >= Circuit.OUTER_RADIUS) {
						onTrack = false;
					}
					break;
				case 2:
					if (localPosition[1] <= Circuit.INNER_RADIUS || localPosition[1] >= Circuit.OUTER_RADIUS) {
						onTrack = false;
					}
					break;
				case 3:
					onTrack = isInsideCircularCorner(localPosition[0], localPosition[1], 0, GameFrame.TILE_SIZE);
					break;
				case 4:
					onTrack = isInsideCircularCorner(
							localPosition[0], localPosition[1], GameFrame.TILE_SIZE, GameFrame.TILE_SIZE);
					break;
				case 5:
					onTrack = isInsideCircularCorner(
							localPosition[0], localPosition[1], GameFrame.TILE_SIZE, 0);
					break;
				case 6:
					onTrack = isInsideCircularCorner(localPosition[0], localPosition[1], 0, 0);
					break;
				case 7:
				default:
					break;
			}

			if (!onTrack) {
				car.setSpeed((float) (-0.2 * car.getSpeed()));
				car.translateBy(
						(float) -Math.signum(localPosition[0] - GameFrame.TILE_SIZE / 2.0),
						(float) -Math.signum(localPosition[1] - GameFrame.TILE_SIZE / 2.0));
			}
		} catch (RuntimeException exception) {
			System.err.println("Car left the playable area.");
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
