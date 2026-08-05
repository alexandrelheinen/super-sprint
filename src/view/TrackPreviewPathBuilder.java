package view;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Circuit;

/**
 * Builds a single closed track outline by walking track tiles and composing
 * constrained cubic Bézier segments (lines for straights, tangents for corners).
 */
final class TrackPreviewPathBuilder {

	private static final int[][] NEIGHBOR_OFFSETS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
	private static final double INNER_RADIUS_RATIO = Circuit.INNER_RADIUS;
	private static final double OUTER_RADIUS_RATIO = Circuit.OUTER_RADIUS;
	private static final double TILE_UNIT = 219.0;
	private static final double BEZIER_ARC_FACTOR = 4.0 / 3.0;
	private static final double POSITION_TOLERANCE = 1e-3;

	private TrackPreviewPathBuilder() {
	}

	static Path2D buildTrackPath(int[][] trackMap) {
		List<int[]> orderedTiles = orderTrackTiles(trackMap);
		Path2D path = new Path2D.Double();
		if (orderedTiles.isEmpty()) {
			return path;
		}

		int[] bounds = findTrackBounds(trackMap);
		double radius = midRadius();
		boolean pathStarted = false;

		for (int index = 0; index < orderedTiles.size(); index++) {
			int[] cell = orderedTiles.get(index);
			int[] previous = orderedTiles.get((index - 1 + orderedTiles.size()) % orderedTiles.size());
			int[] next = orderedTiles.get((index + 1) % orderedTiles.size());
			int incomingDirection = directionBetween(previous, cell);
			int outgoingDirection = directionBetween(cell, next);
			pathStarted = appendTileSegment(
					path,
					trackMap,
					cell[0],
					cell[1],
					bounds,
					incomingDirection,
					outgoingDirection,
					radius,
					pathStarted);
		}
		path.closePath();
		return path;
	}

	private static double midRadius() {
		double innerRadius = INNER_RADIUS_RATIO / TILE_UNIT;
		double outerRadius = OUTER_RADIUS_RATIO / TILE_UNIT;
		return (innerRadius + outerRadius) / 2.0;
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

	private static boolean appendTileSegment(
			Path2D path,
			int[][] trackMap,
			int row,
			int column,
			int[] bounds,
			int incomingDirection,
			int outgoingDirection,
			double radius,
			boolean pathStarted) {
		double cellWidth = 1.0;
		double cellHeight = 1.0;
		double originX = column * cellWidth;
		double originY = row * cellHeight;
		int tileType = trackMap[row][column];

		if (isStraightTile(tileType)) {
			double[] entry = straightPoint(originX, originY, cellWidth, cellHeight, radius, row, column, bounds, incomingDirection, true);
			double[] exit = straightPoint(originX, originY, cellWidth, cellHeight, radius, row, column, bounds, outgoingDirection, false);
			if (!pathStarted) {
				path.moveTo(entry[0], entry[1]);
				pathStarted = true;
			} else {
				connectTo(path, entry[0], entry[1]);
			}
			path.lineTo(exit[0], exit[1]);
			return pathStarted;
		}

		double[] center = cornerCenter(tileType, originX, originY, cellWidth, cellHeight);
		int entrySide = (incomingDirection + 2) % 4;
		double startAngle = cornerBoundaryAngle(tileType, entrySide);
		double endAngle = cornerBoundaryAngle(tileType, outgoingDirection);
		if (!pathStarted) {
			double[] startPoint = pointOnArc(center[0], center[1], radius, startAngle);
			path.moveTo(startPoint[0], startPoint[1]);
			pathStarted = true;
		}
		appendCircularArc(path, center[0], center[1], radius, startAngle, endAngle, false);
		return pathStarted;
	}

	private static boolean isStraightTile(int tileType) {
		return tileType == Circuit.TILE_STRAIGHT_HORIZONTAL || tileType == Circuit.TILE_STRAIGHT_VERTICAL;
	}

	private static void connectTo(Path2D path, double x, double y) {
		var current = path.getCurrentPoint();
		if (current == null
				|| Math.hypot(current.getX() - x, current.getY() - y) > POSITION_TOLERANCE) {
			path.lineTo(x, y);
		}
	}

	private static double[] straightPoint(
			double originX,
			double originY,
			double cellWidth,
			double cellHeight,
			double radius,
			int row,
			int column,
			int[] bounds,
			int direction,
			boolean entry) {
		boolean horizontal = isHorizontalSegment(row, column, bounds, direction);
		if (horizontal) {
			double lineY = row == bounds[0] ? originY + radius : originY + cellHeight - radius;
			double leftX = originX + radius;
			double rightX = originX + cellWidth - radius;
			if (direction == 0) {
				return entry ? new double[] {leftX, lineY} : new double[] {rightX, lineY};
			}
			return entry ? new double[] {rightX, lineY} : new double[] {leftX, lineY};
		}

		double lineX = column == bounds[2] ? originX + radius : originX + cellWidth - radius;
		double topY = originY + radius;
		double bottomY = originY + cellHeight - radius;
		if (direction == 1) {
			return entry ? new double[] {lineX, topY} : new double[] {lineX, bottomY};
		}
		return entry ? new double[] {lineX, bottomY} : new double[] {lineX, topY};
	}

	private static boolean isHorizontalSegment(int row, int column, int[] bounds, int direction) {
		if (direction == 0 || direction == 2) {
			return true;
		}
		if (direction == 1 || direction == 3) {
			return false;
		}
		return row == bounds[0] || row == bounds[1];
	}

	private static double[] cornerCenter(int tileType, double originX, double originY, double cellWidth, double cellHeight) {
		return switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> new double[] {originX, originY + cellHeight};
			case Circuit.TILE_CORNER_TOP_RIGHT -> new double[] {originX + cellWidth, originY + cellHeight};
			case Circuit.TILE_CORNER_TOP_LEFT -> new double[] {originX + cellWidth, originY};
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> new double[] {originX, originY};
			default -> new double[] {originX + cellWidth / 2.0, originY + cellHeight / 2.0};
		};
	}

	/**
	 * Returns the arc angle (Java convention, y-down) for a tile edge meeting the centerline.
	 */
	private static double cornerBoundaryAngle(int tileType, int side) {
		return switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> switch (side) {
				case 2 -> 180.0;
				case 3 -> 270.0;
				case 0 -> 0.0;
				default -> 90.0;
			};
			case Circuit.TILE_CORNER_TOP_RIGHT -> switch (side) {
				case 3 -> 270.0;
				case 0 -> 0.0;
				case 1 -> 90.0;
				default -> 180.0;
			};
			case Circuit.TILE_CORNER_TOP_LEFT -> switch (side) {
				case 0 -> 0.0;
				case 1 -> 90.0;
				case 2 -> 180.0;
				default -> 270.0;
			};
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> switch (side) {
				case 1 -> 90.0;
				case 2 -> 180.0;
				case 3 -> 270.0;
				default -> 0.0;
			};
			default -> 0.0;
		};
	}

	private static double[] pointOnArc(double centerX, double centerY, double radius, double angleDegrees) {
		double radians = Math.toRadians(angleDegrees);
		return new double[] {
				centerX + radius * Math.cos(radians),
				centerY + radius * Math.sin(radians)
		};
	}

	private static void appendCircularArc(
			Path2D path,
			double centerX,
			double centerY,
			double radius,
			double startAngleDegrees,
			double endAngleDegrees,
			boolean moveToStart) {
		double startRadians = Math.toRadians(startAngleDegrees);
		double endRadians = Math.toRadians(endAngleDegrees);
		double startX = centerX + radius * Math.cos(startRadians);
		double startY = centerY + radius * Math.sin(startRadians);
		double endX = centerX + radius * Math.cos(endRadians);
		double endY = centerY + radius * Math.sin(endRadians);

		if (moveToStart) {
			path.moveTo(startX, startY);
		} else {
			connectTo(path, startX, startY);
		}

		double sweepDegrees = endAngleDegrees - startAngleDegrees;
		while (sweepDegrees <= -360.0) {
			sweepDegrees += 360.0;
		}
		while (sweepDegrees > 360.0) {
			sweepDegrees -= 360.0;
		}
		if (sweepDegrees > 180.0) {
			sweepDegrees -= 360.0;
		}
		if (sweepDegrees < -180.0) {
			sweepDegrees += 360.0;
		}
		if (Math.abs(sweepDegrees) < POSITION_TOLERANCE) {
			return;
		}

		double handleLength = BEZIER_ARC_FACTOR
				* Math.tan(Math.toRadians(Math.abs(sweepDegrees) / 4.0))
				* radius;
		double startTangentX = -Math.sin(startRadians);
		double startTangentY = Math.cos(startRadians);
		double endTangentX = -Math.sin(endRadians);
		double endTangentY = Math.cos(endRadians);
		if (sweepDegrees < 0.0) {
			startTangentX = -startTangentX;
			startTangentY = -startTangentY;
			endTangentX = -endTangentX;
			endTangentY = -endTangentY;
		}

		path.curveTo(
				startX + handleLength * startTangentX,
				startY + handleLength * startTangentY,
				endX - handleLength * endTangentX,
				endY - handleLength * endTangentY,
				endX,
				endY);
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

	private static String key(int[] cell) {
		return cell[0] + ":" + cell[1];
	}
}
