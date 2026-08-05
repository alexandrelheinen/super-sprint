package view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Circuit;

/**
 * Builds an ordered centerline through a track tile map for preview rendering.
 */
final class TrackPreviewPathBuilder {

	private static final int[][] NEIGHBOR_OFFSETS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
	private static final int ARC_SAMPLE_COUNT = 4;
	private static final double INNER_RADIUS_RATIO = Circuit.INNER_RADIUS;
	private static final double OUTER_RADIUS_RATIO = Circuit.OUTER_RADIUS;
	private static final double TILE_UNIT = 219.0;

	private TrackPreviewPathBuilder() {
	}

	static double[][] buildOrderedCenterline(int[][] trackMap) {
		List<int[]> orderedTiles = orderTrackTiles(trackMap);
		if (orderedTiles.isEmpty()) {
			return new double[][] {{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}, {0.0, 1.0}};
		}

		int[] bounds = findTrackBounds(trackMap);
		List<double[]> points = new ArrayList<>();
		for (int index = 0; index < orderedTiles.size(); index++) {
			int[] cell = orderedTiles.get(index);
			int[] previous = orderedTiles.get((index - 1 + orderedTiles.size()) % orderedTiles.size());
			int[] next = orderedTiles.get((index + 1) % orderedTiles.size());
			appendTileCenterline(
					points,
					trackMap,
					cell[0],
					cell[1],
					bounds,
					directionBetween(previous, cell),
					directionBetween(cell, next));
		}
		return dedupeConsecutive(points);
	}

	private static int[] findTrackBounds(int[][] trackMap) {
		int minRow = trackMap.length;
		int maxRow = -1;
		int minCol = trackMap[0].length;
		int maxCol = -1;
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				if (trackMap[row][column] != Circuit.TILE_OPEN) {
					minRow = Math.min(minRow, row);
					maxRow = Math.max(maxRow, row);
					minCol = Math.min(minCol, column);
					maxCol = Math.max(maxCol, column);
				}
			}
		}
		return new int[] {minRow, maxRow, minCol, maxCol};
	}

	private static List<int[]> orderTrackTiles(int[][] trackMap) {
		List<int[]> trackCells = new ArrayList<>();
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				if (trackMap[row][column] != Circuit.TILE_OPEN) {
					trackCells.add(new int[] {row, column});
				}
			}
		}
		if (trackCells.isEmpty()) {
			return trackCells;
		}

		int[] start = trackCells.stream()
				.min((left, right) -> {
					int rowCompare = Integer.compare(left[0], right[0]);
					return rowCompare != 0 ? rowCompare : Integer.compare(left[1], right[1]);
				})
				.orElseThrow();
		List<int[]> ordered = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		int[] current = start;
		int travelDirection = 0;

		while (true) {
			ordered.add(current);
			visited.add(key(current));
			int[] next = chooseNextTile(trackMap, current, travelDirection, visited);
			if (next == null) {
				break;
			}
			travelDirection = directionBetween(current, next);
			current = next;
			if (key(current).equals(key(start)) && ordered.size() > 1) {
				break;
			}
		}
		return ordered;
	}

	private static int[] chooseNextTile(
			int[][] trackMap,
			int[] current,
			int travelDirection,
			Set<String> visited) {
		for (int turnOffset : new int[] {3, 0, 1, 2}) {
			int direction = (travelDirection + turnOffset) % NEIGHBOR_OFFSETS.length;
			int[] neighborOffset = NEIGHBOR_OFFSETS[direction];
			int row = current[0] + neighborOffset[0];
			int column = current[1] + neighborOffset[1];
			if (row < 0 || row >= trackMap.length || column < 0 || column >= trackMap[row].length) {
				continue;
			}
			if (trackMap[row][column] == Circuit.TILE_OPEN) {
				continue;
			}
			int[] candidate = new int[] {row, column};
			if (visited.contains(key(candidate))) {
				continue;
			}
			return candidate;
		}
		return null;
	}

	private static void appendTileCenterline(
			List<double[]> points,
			int[][] trackMap,
			int row,
			int column,
			int[] bounds,
			int incomingDirection,
			int outgoingDirection) {
		double cellWidth = 1.0;
		double cellHeight = 1.0;
		double originX = column * cellWidth;
		double originY = row * cellHeight;
		double innerRadius = (INNER_RADIUS_RATIO / TILE_UNIT) * Math.min(cellWidth, cellHeight);
		double outerRadius = (OUTER_RADIUS_RATIO / TILE_UNIT) * Math.min(cellWidth, cellHeight);
		double centerX = originX + cellWidth / 2.0;
		double centerY = originY + cellHeight / 2.0;

		int tileType = trackMap[row][column];
		if (tileType == Circuit.TILE_STRAIGHT_HORIZONTAL || tileType == Circuit.TILE_STRAIGHT_VERTICAL) {
			appendStraightCenterline(
					points,
					originX,
					originY,
					cellWidth,
					cellHeight,
					innerRadius,
					centerX,
					centerY,
					row,
					column,
					bounds,
					incomingDirection,
					outgoingDirection);
			return;
		}

		switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> appendCornerArc(
					points, originX, originY + cellHeight, innerRadius, outerRadius, 180, 90);
			case Circuit.TILE_CORNER_TOP_RIGHT -> appendCornerArc(
					points, originX + cellWidth, originY + cellHeight, innerRadius, outerRadius, 270, 90);
			case Circuit.TILE_CORNER_TOP_LEFT -> appendCornerArc(
					points, originX + cellWidth, originY, innerRadius, outerRadius, 0, 90);
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> appendCornerArc(
					points, originX, originY, innerRadius, outerRadius, 90, 90);
			default -> {
			}
		}
	}

	private static void appendStraightCenterline(
			List<double[]> points,
			double originX,
			double originY,
			double cellWidth,
			double cellHeight,
			double innerRadius,
			double centerX,
			double centerY,
			int row,
			int column,
			int[] bounds,
			int incomingDirection,
			int outgoingDirection) {
		boolean horizontal = isHorizontalTraversal(incomingDirection, outgoingDirection);
		if (horizontal) {
			double lineY = row == bounds[0] ? originY + innerRadius : originY + cellHeight - innerRadius;
			if (incomingDirection == 2 || outgoingDirection == 2) {
				addPoint(points, originX + cellWidth - innerRadius, lineY);
				addPoint(points, originX + innerRadius, lineY);
			} else {
				addPoint(points, originX + innerRadius, lineY);
				addPoint(points, originX + cellWidth - innerRadius, lineY);
			}
			return;
		}

		double lineX = column == bounds[2] ? originX + innerRadius : originX + cellWidth - innerRadius;
		if (incomingDirection == 1 || outgoingDirection == 1) {
			addPoint(points, lineX, originY + cellHeight - innerRadius);
			addPoint(points, lineX, originY + innerRadius);
		} else {
			addPoint(points, lineX, originY + innerRadius);
			addPoint(points, lineX, originY + cellHeight - innerRadius);
		}
	}

	private static boolean isHorizontalTraversal(int incomingDirection, int outgoingDirection) {
		return incomingDirection == 0
				|| incomingDirection == 2
				|| outgoingDirection == 0
				|| outgoingDirection == 2;
	}

	private static void appendCornerArc(
			List<double[]> points,
			double cornerX,
			double cornerY,
			double innerRadius,
			double outerRadius,
			int startAngleDegrees,
			int extentDegrees) {
		double midRadius = (innerRadius + outerRadius) / 2.0;
		double startAngle = Math.toRadians(startAngleDegrees);
		double endAngle = Math.toRadians(startAngleDegrees + extentDegrees);
		for (int sample = 1; sample <= ARC_SAMPLE_COUNT; sample++) {
			double t = sample / (double) ARC_SAMPLE_COUNT;
			double angle = startAngle + t * (endAngle - startAngle);
			addPoint(
					points,
					cornerX + midRadius * Math.cos(angle),
					cornerY + midRadius * Math.sin(angle));
		}
	}

	private static int directionBetween(int[] from, int[] to) {
		int deltaRow = to[0] - from[0];
		int deltaColumn = to[1] - from[1];
		if (deltaColumn > 0) {
			return 0;
		}
		if (deltaRow > 0) {
			return 1;
		}
		if (deltaColumn < 0) {
			return 2;
		}
		return 3;
	}

	private static void addPoint(List<double[]> points, double x, double y) {
		if (points.isEmpty()) {
			points.add(new double[] {x, y});
			return;
		}
		double[] last = points.get(points.size() - 1);
		if (Math.hypot(last[0] - x, last[1] - y) > 1e-4) {
			points.add(new double[] {x, y});
		}
	}

	private static double[][] dedupeConsecutive(List<double[]> points) {
		return points.toArray(new double[0][]);
	}

	private static String key(int[] cell) {
		return cell[0] + ":" + cell[1];
	}
}
