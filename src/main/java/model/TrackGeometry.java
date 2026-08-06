package model;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared track centerline geometry for menu previews and AI path tracking.
 *
 * <p>Legacy tile constants are swapped relative to on-screen orientation:
 * {@code TILE_STRAIGHT_HORIZONTAL} tiles appear in vertical runs and
 * {@code TILE_STRAIGHT_VERTICAL} tiles in horizontal runs.
 */
public final class TrackGeometry {

	private static final int[][] NEIGHBOR_OFFSETS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
	private static final double BEZIER_ARC_FACTOR = 4.0 / 3.0;
	private static final double SAMPLE_SPACING_METERS = 0.35;

	private TrackGeometry() {
	}

	public static Path2D buildPreviewPath(int[][] trackMap) {
		double cellSize = 1.0;
		double centerRadius = ((Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0) / 219.0 * cellSize;

		Path2D path = new Path2D.Double();
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				appendPreviewTileGeometry(
						path,
						trackMap[row][column],
						column * cellSize,
						row * cellSize,
						cellSize,
						centerRadius);
			}
		}
		return path;
	}

	public static ReferencePath buildReferencePath(int[][] trackMap) {
		List<Segment> segments = buildOrderedSegments(trackMap);
		if (segments.isEmpty()) {
			return ReferencePath.empty();
		}

		List<ReferencePath.SampleBuilder> samples = new ArrayList<>();
		for (Segment segment : segments) {
			segment.sample(samples, SAMPLE_SPACING_METERS);
		}
		return ReferencePath.fromSamples(samples);
	}

	public static double[][] buildCenterline(int[][] trackMap) {
		return buildReferencePath(trackMap).waypoints();
	}

	private static List<Segment> buildOrderedSegments(int[][] trackMap) {
		List<int[]> orderedTiles = orderTrackTiles(trackMap);
		if (orderedTiles.isEmpty()) {
			return List.of();
		}

		double tileSizeMeters = WorldUnits.METERS_PER_TILE;
		double radiusMeters = WorldUnits.pxToM((Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0);
		List<Segment> segments = new ArrayList<>();
		double[] previousExit = null;

		for (int index = 0; index < orderedTiles.size(); index++) {
			int[] cell = orderedTiles.get(index);
			int[] previous = orderedTiles.get((index - 1 + orderedTiles.size()) % orderedTiles.size());
			int[] next = orderedTiles.get((index + 1) % orderedTiles.size());
			TilePath tilePath = createTilePath(
					trackMap,
					cell[0],
					cell[1],
					directionBetween(previous, cell),
					directionBetween(cell, next),
					tileSizeMeters,
					radiusMeters);
			if (previousExit != null) {
				double[] entry = tilePath.entryPoint();
				if (Math.hypot(previousExit[0] - entry[0], previousExit[1] - entry[1]) > 1e-3) {
					segments.add(new LineSegment(previousExit[0], previousExit[1], entry[0], entry[1]));
				}
			}
			segments.addAll(tilePath.segments());
			previousExit = tilePath.exitPoint();
		}
		return segments;
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

		for (int initialDirection = 0; initialDirection < NEIGHBOR_OFFSETS.length; initialDirection++) {
			List<int[]> ordered = new ArrayList<>();
			Set<String> visited = new HashSet<>();
			if (findTrackCycle(trackMap, start, initialDirection, trackCells.size(), ordered, visited)) {
				return ordered;
			}
		}

		return orderTrackTilesGreedy(trackMap, start);
	}

	private static boolean findTrackCycle(
			int[][] trackMap,
			int[] current,
			int travelDirection,
			int targetTileCount,
			List<int[]> ordered,
			Set<String> visited) {
		if (ordered.isEmpty()) {
			ordered.add(current.clone());
			visited.add(key(current));
		}

		if (ordered.size() == targetTileCount) {
			return isAdjacentTrackTile(trackMap, current, ordered.get(0));
		}

		for (int turnOffset : new int[] {3, 0, 1, 2}) {
			int direction = (travelDirection + turnOffset) % NEIGHBOR_OFFSETS.length;
			int[] next = adjacentTrackTile(trackMap, current, direction);
			if (next == null || visited.contains(key(next))) {
				continue;
			}
			ordered.add(next.clone());
			visited.add(key(next));
			if (findTrackCycle(
					trackMap,
					next,
					directionBetween(current, next),
					targetTileCount,
					ordered,
					visited)) {
				return true;
			}
			ordered.remove(ordered.size() - 1);
			visited.remove(key(next));
		}
		return false;
	}

	private static List<int[]> orderTrackTilesGreedy(int[][] trackMap, int[] start) {
		List<int[]> ordered = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		int[] current = start;
		int travelDirection = 0;

		while (true) {
			ordered.add(current);
			visited.add(key(current));
			int[] next = chooseNextTile(trackMap, current, travelDirection, visited, start, visited.size());
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

	private static boolean isAdjacentTrackTile(int[][] trackMap, int[] from, int[] to) {
		for (int direction = 0; direction < NEIGHBOR_OFFSETS.length; direction++) {
			int[] neighbor = adjacentTrackTile(trackMap, from, direction);
			if (neighbor != null && neighbor[0] == to[0] && neighbor[1] == to[1]) {
				return true;
			}
		}
		return false;
	}

	private static int[] adjacentTrackTile(int[][] trackMap, int[] current, int direction) {
		int[] offset = NEIGHBOR_OFFSETS[direction];
		int row = current[0] + offset[0];
		int column = current[1] + offset[1];
		if (row < 0 || row >= trackMap.length || column < 0 || column >= trackMap[row].length) {
			return null;
		}
		if (trackMap[row][column] == Circuit.TILE_OPEN) {
			return null;
		}
		// Grid adjacency is not enough: both tiles must open onto the shared edge,
		// otherwise chicane layouts produce paths that cut across unconnected tiles.
		int oppositeDirection = (direction + 2) % NEIGHBOR_OFFSETS.length;
		if (!tileOpensTowards(trackMap[current[0]][current[1]], direction)
				|| !tileOpensTowards(trackMap[row][column], oppositeDirection)) {
			return null;
		}
		return new int[] {row, column};
	}

	/**
	 * @param direction index into {@link #NEIGHBOR_OFFSETS}: 0 = east, 1 = south,
	 *        2 = west, 3 = north (screen coordinates, y down)
	 * @return whether the tile artwork has a lane opening on that side
	 */
	private static boolean tileOpensTowards(int tileType, int direction) {
		return switch (tileType) {
			// Legacy tile constants are swapped relative to on-screen orientation.
			case Circuit.TILE_STRAIGHT_HORIZONTAL -> direction == 1 || direction == 3;
			case Circuit.TILE_STRAIGHT_VERTICAL -> direction == 0 || direction == 2;
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> direction == 1 || direction == 2;
			case Circuit.TILE_CORNER_TOP_RIGHT -> direction == 0 || direction == 1;
			case Circuit.TILE_CORNER_TOP_LEFT -> direction == 0 || direction == 3;
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> direction == 2 || direction == 3;
			default -> false;
		};
	}

	private static int[] chooseNextTile(
			int[][] trackMap,
			int[] current,
			int travelDirection,
			Set<String> visited,
			int[] start,
			int visitedCount) {
		for (int turnOffset : new int[] {3, 0, 1, 2}) {
			int direction = (travelDirection + turnOffset) % NEIGHBOR_OFFSETS.length;
			int[] candidate = adjacentTrackTile(trackMap, current, direction);
			if (candidate == null) {
				continue;
			}
			if (visited.contains(key(candidate))) {
				if (candidate[0] == start[0] && candidate[1] == start[1]) {
					return candidate;
				}
				continue;
			}
			return candidate;
		}
		return null;
	}

	private static TilePath createTilePath(
			int[][] trackMap,
			int row,
			int column,
			int incomingDirection,
			int outgoingDirection,
			double tileSizeMeters,
			double radiusMeters) {
		double originX = column * tileSizeMeters;
		double originY = row * tileSizeMeters;
		double centerX = originX + tileSizeMeters / 2.0;
		double centerY = originY + tileSizeMeters / 2.0;
		int tileType = trackMap[row][column];

		double[] entryPoint = tileEntryPoint(
				incomingDirection,
				originX,
				originY,
				tileSizeMeters,
				centerX,
				centerY);
		double[] exitPoint = tileExitPoint(
				outgoingDirection,
				originX,
				originY,
				tileSizeMeters,
				centerX,
				centerY);

		if (tileType == Circuit.TILE_STRAIGHT_HORIZONTAL
				|| tileType == Circuit.TILE_STRAIGHT_VERTICAL
				|| isStraightThroughCorner(incomingDirection, outgoingDirection)) {
			return new TilePath(
					entryPoint,
					exitPoint,
					List.of(new LineSegment(entryPoint[0], entryPoint[1], exitPoint[0], exitPoint[1])));
		}

		double[] arcCenter = cornerCenterMeters(tileType, originX, originY, tileSizeMeters);
		double startAngle = Math.atan2(entryPoint[1] - arcCenter[1], entryPoint[0] - arcCenter[0]);
		double endAngle = Math.atan2(exitPoint[1] - arcCenter[1], exitPoint[0] - arcCenter[0]);
		double sweepAngle = endAngle - startAngle;
		if (sweepAngle > Math.PI) {
			sweepAngle -= 2.0 * Math.PI;
		} else if (sweepAngle <= -Math.PI) {
			sweepAngle += 2.0 * Math.PI;
		}
		double[] arcEntry = {
				arcCenter[0] + radiusMeters * Math.cos(startAngle),
				arcCenter[1] + radiusMeters * Math.sin(startAngle)
		};
		double[] arcExit = {
				arcCenter[0] + radiusMeters * Math.cos(startAngle + sweepAngle),
				arcCenter[1] + radiusMeters * Math.sin(startAngle + sweepAngle)
		};
		List<Segment> segments = new ArrayList<>();
		if (Math.hypot(entryPoint[0] - arcEntry[0], entryPoint[1] - arcEntry[1]) > 1e-3) {
			segments.add(new LineSegment(entryPoint[0], entryPoint[1], arcEntry[0], arcEntry[1]));
		}
		segments.add(new ArcSegment(arcCenter[0], arcCenter[1], radiusMeters, startAngle, sweepAngle));
		if (Math.hypot(exitPoint[0] - arcExit[0], exitPoint[1] - arcExit[1]) > 1e-3) {
			segments.add(new LineSegment(arcExit[0], arcExit[1], exitPoint[0], exitPoint[1]));
		}
		return new TilePath(entryPoint, exitPoint, segments);
	}

	private static final class TilePath {
		private final double[] entryPoint;
		private final double[] exitPoint;
		private final List<Segment> segments;

		private TilePath(double[] entryPoint, double[] exitPoint, List<Segment> segments) {
			this.entryPoint = entryPoint;
			this.exitPoint = exitPoint;
			this.segments = segments;
		}

		private double[] entryPoint() {
			return entryPoint;
		}

		private double[] exitPoint() {
			return exitPoint;
		}

		private List<Segment> segments() {
			return segments;
		}
	}

	private static boolean isStraightThroughCorner(int incomingDirection, int outgoingDirection) {
		return incomingDirection == outgoingDirection;
	}

	private static double[] tileEntryPoint(
			int incomingDirection,
			double originX,
			double originY,
			double tileSizeMeters,
			double centerX,
			double centerY) {
		return switch (incomingDirection) {
			case 0 -> new double[] {originX, centerY};
			case 1 -> new double[] {centerX, originY};
			case 2 -> new double[] {originX + tileSizeMeters, centerY};
			case 3 -> new double[] {centerX, originY + tileSizeMeters};
			default -> new double[] {centerX, centerY};
		};
	}

	private static double[] tileExitPoint(
			int outgoingDirection,
			double originX,
			double originY,
			double tileSizeMeters,
			double centerX,
			double centerY) {
		return switch (outgoingDirection) {
			case 0 -> new double[] {originX + tileSizeMeters, centerY};
			case 1 -> new double[] {centerX, originY + tileSizeMeters};
			case 2 -> new double[] {originX, centerY};
			case 3 -> new double[] {centerX, originY};
			default -> new double[] {centerX, centerY};
		};
	}

	private static double[] cornerCenterMeters(int tileType, double originX, double originY, double tileSizeMeters) {
		return switch (tileType) {
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> new double[] {originX, originY + tileSizeMeters};
			case Circuit.TILE_CORNER_TOP_RIGHT -> new double[] {originX + tileSizeMeters, originY + tileSizeMeters};
			case Circuit.TILE_CORNER_TOP_LEFT -> new double[] {originX + tileSizeMeters, originY};
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> new double[] {originX, originY};
			default -> new double[] {originX + tileSizeMeters / 2.0, originY + tileSizeMeters / 2.0};
		};
	}

	static void appendPreviewTileGeometry(
			Path2D path,
			int tileType,
			double originX,
			double originY,
			double cellSize,
			double radius) {
		double centerX = originX + cellSize / 2.0;
		double centerY = originY + cellSize / 2.0;

		switch (tileType) {
			case Circuit.TILE_STRAIGHT_HORIZONTAL -> {
				path.moveTo(centerX, originY);
				path.lineTo(centerX, originY + cellSize);
			}
			case Circuit.TILE_STRAIGHT_VERTICAL -> {
				path.moveTo(originX, centerY);
				path.lineTo(originX + cellSize, centerY);
			}
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> appendPreviewQuarterArc(
					path, originX, originY + cellSize, radius, 270.0);
			case Circuit.TILE_CORNER_TOP_RIGHT -> appendPreviewQuarterArc(
					path, originX + cellSize, originY + cellSize, radius, 180.0);
			case Circuit.TILE_CORNER_TOP_LEFT -> appendPreviewQuarterArc(
					path, originX + cellSize, originY, radius, 90.0);
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> appendPreviewQuarterArc(
					path, originX, originY, radius, 0.0);
			default -> {
			}
		}
	}

	private static void appendPreviewQuarterArc(
			Path2D path,
			double centerX,
			double centerY,
			double radius,
			double startAngleDegrees) {
		double startRadians = Math.toRadians(startAngleDegrees);
		double endRadians = Math.toRadians(startAngleDegrees + 90.0);
		double startX = centerX + radius * Math.cos(startRadians);
		double startY = centerY + radius * Math.sin(startRadians);
		double endX = centerX + radius * Math.cos(endRadians);
		double endY = centerY + radius * Math.sin(endRadians);
		double handleLength = BEZIER_ARC_FACTOR * Math.tan(Math.toRadians(22.5)) * radius;

		path.moveTo(startX, startY);
		path.curveTo(
				startX - handleLength * Math.sin(startRadians),
				startY + handleLength * Math.cos(startRadians),
				endX + handleLength * Math.sin(endRadians),
				endY - handleLength * Math.cos(endRadians),
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

	interface Segment {
		void sample(List<ReferencePath.SampleBuilder> points, double spacingMeters);
	}

	private static final class LineSegment implements Segment {
		private final double startX;
		private final double startY;
		private final double endX;
		private final double endY;

		private LineSegment(double startX, double startY, double endX, double endY) {
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
		}

		@Override
		public void sample(List<ReferencePath.SampleBuilder> points, double spacingMeters) {
			double dx = endX - startX;
			double dy = endY - startY;
			double length = Math.hypot(dx, dy);
			if (length < 1e-6) {
				return;
			}
			int count = Math.max(1, (int) Math.ceil(length / spacingMeters));
			double heading = Math.atan2(dy, dx);
			for (int index = 1; index <= count; index++) {
				double t = index / (double) count;
				points.add(new ReferencePath.SampleBuilder(
						startX + t * dx,
						startY + t * dy,
						heading,
						0.0));
			}
		}
	}

	private static final class ArcSegment implements Segment {
		private final double centerX;
		private final double centerY;
		private final double radius;
		private final double startAngle;
		private final double sweepAngle;

		private ArcSegment(
				double centerX,
				double centerY,
				double radius,
				double startAngle,
				double sweepAngle) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.radius = radius;
			this.startAngle = startAngle;
			this.sweepAngle = sweepAngle;
		}

		@Override
		public void sample(List<ReferencePath.SampleBuilder> points, double spacingMeters) {
			double arcLength = Math.abs(sweepAngle) * radius;
			int count = Math.max(1, (int) Math.ceil(arcLength / spacingMeters));
			double signedCurvature = Math.copySign(1.0 / radius, sweepAngle);
			for (int index = 1; index <= count; index++) {
				double t = index / (double) count;
				double angle = startAngle + t * sweepAngle;
				double tangent = angle + Math.copySign(Math.PI / 2.0, sweepAngle);
				points.add(new ReferencePath.SampleBuilder(
						centerX + radius * Math.cos(angle),
						centerY + radius * Math.sin(angle),
						tangent,
						signedCurvature));
			}
		}
	}
}
