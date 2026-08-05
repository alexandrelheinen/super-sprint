package model;

import java.util.ArrayList;
import java.util.List;

import view.GameFrame;

/**
 * Builds ordered centerline waypoints for pure pursuit tracking on tile circuits.
 */
public final class CircuitPath {

	private static final int ARC_SAMPLE_COUNT = 8;
	private static final int STRAIGHT_SAMPLE_COUNT = 6;
	private static final double TWO_PI = 2.0 * Math.PI;

	private CircuitPath() {
	}

	public static double[][] buildCenterline(int[][] trackMap) {
		int[] bounds = findTrackBounds(trackMap);
		if (bounds == null) {
			return new double[][] {{0.0, 0.0}, {1.0, 0.0}};
		}

		int minRow = bounds[0];
		int maxRow = bounds[1];
		int minCol = bounds[2];
		int maxCol = bounds[3];
		int midRow = (minRow + maxRow) / 2;

		double tileSize = GameFrame.TILE_SIZE;
		double centerRadius = (Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0;

		double leftX = minCol * tileSize + tileSize / 2.0;
		double rightX = maxCol * tileSize + tileSize / 2.0;
		double topY = minRow * tileSize + tileSize / 2.0;
		double bottomY = midRow * tileSize + tileSize / 2.0;

		List<double[]> points = new ArrayList<>();

		appendStraight(points, leftX, bottomY, rightX, bottomY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				maxRow,
				maxCol,
				-Math.PI / 2.0,
				0.0,
				centerRadius);
		appendStraight(points, rightX, bottomY, rightX, topY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				minRow,
				maxCol,
				0.0,
				Math.PI / 2.0,
				centerRadius);
		appendStraight(points, rightX, topY, leftX, topY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				minRow,
				minCol,
				Math.PI / 2.0,
				Math.PI,
				centerRadius);
		appendStraight(points, leftX, topY, leftX, bottomY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				maxRow,
				minCol,
				Math.PI,
				3.0 * Math.PI / 2.0,
				centerRadius);

		return toArray(points);
	}

	private static int[] findTrackBounds(int[][] trackMap) {
		int minRow = trackMap.length;
		int maxRow = -1;
		int minCol = trackMap[0].length;
		int maxCol = -1;

		for (int row = 0; row < trackMap.length; row++) {
			for (int col = 0; col < trackMap[row].length; col++) {
				if (trackMap[row][col] != Circuit.TILE_OPEN) {
					minRow = Math.min(minRow, row);
					maxRow = Math.max(maxRow, row);
					minCol = Math.min(minCol, col);
					maxCol = Math.max(maxCol, col);
				}
			}
		}

		if (maxRow < 0) {
			return null;
		}
		return new int[] {minRow, maxRow, minCol, maxCol};
	}

	private static void appendCornerArcForTile(
			List<double[]> points,
			int[][] trackMap,
			int row,
			int col,
			double startAngle,
			double endAngle,
			double radius) {
		double[] center = cornerCenter(trackMap[row][col], row, col);
		appendCornerArc(points, center[0], center[1], startAngle, endAngle, radius);
	}

	private static double[] cornerCenter(int tileType, int row, int col) {
		double tileSize = GameFrame.TILE_SIZE;
		double tileOriginX = col * tileSize;
		double tileOriginY = row * tileSize;

		switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT:
				return new double[] {tileOriginX, tileOriginY + tileSize};
			case Circuit.TILE_CORNER_TOP_RIGHT:
				return new double[] {tileOriginX + tileSize, tileOriginY + tileSize};
			case Circuit.TILE_CORNER_TOP_LEFT:
				return new double[] {tileOriginX + tileSize, tileOriginY};
			case Circuit.TILE_CORNER_BOTTOM_LEFT:
			default:
				return new double[] {tileOriginX, tileOriginY};
		}
	}

	private static void appendStraight(
			List<double[]> points,
			double startX,
			double startY,
			double endX,
			double endY,
			int sampleCount) {
		for (int index = 1; index <= sampleCount; index++) {
			double t = index / (double) sampleCount;
			points.add(new double[] {
					startX + t * (endX - startX),
					startY + t * (endY - startY)
			});
		}
	}

	private static void appendCornerArc(
			List<double[]> points,
			double cornerX,
			double cornerY,
			double startAngle,
			double endAngle,
			double radius) {
		double delta = endAngle - startAngle;
		if (delta < 0.0) {
			delta += TWO_PI;
		}
		for (int index = 1; index <= ARC_SAMPLE_COUNT; index++) {
			double t = index / (double) ARC_SAMPLE_COUNT;
			double angle = startAngle + t * delta;
			points.add(new double[] {
					cornerX + radius * Math.cos(angle),
					cornerY + radius * Math.sin(angle)
			});
		}
	}

	private static double[][] toArray(List<double[]> points) {
		double[][] path = new double[points.size()][2];
		for (int index = 0; index < points.size(); index++) {
			path[index] = points.get(index);
		}
		return path;
	}
}
