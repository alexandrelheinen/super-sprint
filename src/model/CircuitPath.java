package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds ordered centerline waypoints in meters for pure pursuit tracking.
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

		double tileSizeMeters = WorldUnits.METERS_PER_TILE;
		double centerRadiusMeters = WorldUnits.pxToM((Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0);

		double leftX = minCol * tileSizeMeters + tileSizeMeters / 2.0;
		double rightX = maxCol * tileSizeMeters + tileSizeMeters / 2.0;
		double topY = minRow * tileSizeMeters + tileSizeMeters / 2.0;
		double bottomY = midRow * tileSizeMeters + tileSizeMeters / 2.0;

		List<double[]> points = new ArrayList<>();

		appendStraight(points, leftX, bottomY, rightX, bottomY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				maxRow,
				maxCol,
				-Math.PI / 2.0,
				0.0,
				centerRadiusMeters);
		appendStraight(points, rightX, bottomY, rightX, topY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				minRow,
				maxCol,
				0.0,
				Math.PI / 2.0,
				centerRadiusMeters);
		appendStraight(points, rightX, topY, leftX, topY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				minRow,
				minCol,
				Math.PI / 2.0,
				Math.PI,
				centerRadiusMeters);
		appendStraight(points, leftX, topY, leftX, bottomY, STRAIGHT_SAMPLE_COUNT);
		appendCornerArcForTile(
				points,
				trackMap,
				maxRow,
				minCol,
				Math.PI,
				3.0 * Math.PI / 2.0,
				centerRadiusMeters);

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
			double radiusMeters) {
		double[] center = cornerCenterMeters(trackMap[row][col], row, col);
		appendCornerArc(points, center[0], center[1], startAngle, endAngle, radiusMeters);
	}

	private static double[] cornerCenterMeters(int tileType, int row, int col) {
		double tileSizeMeters = WorldUnits.METERS_PER_TILE;
		double tileOriginX = col * tileSizeMeters;
		double tileOriginY = row * tileSizeMeters;

		switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT:
				return new double[] {tileOriginX, tileOriginY + tileSizeMeters};
			case Circuit.TILE_CORNER_TOP_RIGHT:
				return new double[] {tileOriginX + tileSizeMeters, tileOriginY + tileSizeMeters};
			case Circuit.TILE_CORNER_TOP_LEFT:
				return new double[] {tileOriginX + tileSizeMeters, tileOriginY};
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
			double radiusMeters) {
		double delta = endAngle - startAngle;
		if (delta < 0.0) {
			delta += TWO_PI;
		}
		for (int index = 1; index <= ARC_SAMPLE_COUNT; index++) {
			double t = index / (double) ARC_SAMPLE_COUNT;
			double angle = startAngle + t * delta;
			points.add(new double[] {
					cornerX + radiusMeters * Math.cos(angle),
					cornerY + radiusMeters * Math.sin(angle)
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
