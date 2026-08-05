package view;

import java.awt.geom.Path2D;

import model.Circuit;

/**
 * Builds the track outline by drawing each tile's centerline geometry.
 *
 * Note: the legacy tile constants are swapped relative to their on-screen
 * orientation — {@code TILE_STRAIGHT_HORIZONTAL} tiles appear in vertical runs
 * and {@code TILE_STRAIGHT_VERTICAL} tiles in horizontal runs (see
 * {@code Game.TRACK_MAPS}). This builder follows the actual orientation.
 */
final class TrackPreviewPathBuilder {

	private static final double INNER_RADIUS_RATIO = Circuit.INNER_RADIUS;
	private static final double OUTER_RADIUS_RATIO = Circuit.OUTER_RADIUS;
	private static final double TILE_UNIT = 219.0;
	private static final double BEZIER_ARC_FACTOR = 4.0 / 3.0;

	private TrackPreviewPathBuilder() {
	}

	static Path2D buildTrackPath(int[][] trackMap) {
		double cellSize = 1.0;
		double centerRadius = ((INNER_RADIUS_RATIO + OUTER_RADIUS_RATIO) / 2.0) / TILE_UNIT * cellSize;

		Path2D path = new Path2D.Double();
		for (int row = 0; row < trackMap.length; row++) {
			for (int column = 0; column < trackMap[row].length; column++) {
				appendTileGeometry(
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

	private static void appendTileGeometry(
			Path2D path,
			int tileType,
			double originX,
			double originY,
			double cellSize,
			double radius) {
		double centerX = originX + cellSize / 2.0;
		double centerY = originY + cellSize / 2.0;

		switch (tileType) {
			// Legacy naming: HORIZONTAL tiles are vertical runs on screen.
			case Circuit.TILE_STRAIGHT_HORIZONTAL -> {
				path.moveTo(centerX, originY);
				path.lineTo(centerX, originY + cellSize);
			}
			// Legacy naming: VERTICAL tiles are horizontal runs on screen.
			case Circuit.TILE_STRAIGHT_VERTICAL -> {
				path.moveTo(originX, centerY);
				path.lineTo(originX + cellSize, centerY);
			}
			// Arc centers sit on the inside of the bend; angles use the
			// Java 2D screen convention (0° = east, 90° = south, y-down).
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> appendQuarterArc(
					path, originX, originY + cellSize, radius, 270.0);
			case Circuit.TILE_CORNER_TOP_RIGHT -> appendQuarterArc(
					path, originX + cellSize, originY + cellSize, radius, 180.0);
			case Circuit.TILE_CORNER_TOP_LEFT -> appendQuarterArc(
					path, originX + cellSize, originY, radius, 90.0);
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> appendQuarterArc(
					path, originX, originY, radius, 0.0);
			default -> {
			}
		}
	}

	/**
	 * Appends a 90° circular arc as a single cubic Bézier whose endpoints and
	 * endpoint tangents are constrained to the circle.
	 */
	private static void appendQuarterArc(
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
}
