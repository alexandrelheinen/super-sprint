package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import controller.Game;

public class TrackGeometryTest {

	private static final double SAMPLE_SPACING_UPPER_BOUND_METERS = 1.0;
	private static final double LOOP_CLOSURE_TOLERANCE_METERS = 1.0;

	@Test
	public void buildsNonEmptyReferencePathForEveryTrack() {
		for (int[][] trackMap : Game.TRACK_MAPS) {
			ReferencePath path = TrackGeometry.buildReferencePath(trackMap);
			assertTrue(path.sampleCount() > 100, "Reference path should be densely sampled");
		}
	}

	@Test
	public void referencePathsFormClosedLoops() {
		for (int[][] trackMap : Game.TRACK_MAPS) {
			double[][] waypoints = TrackGeometry.buildReferencePath(trackMap).waypoints();
			double[] first = waypoints[0];
			double[] last = waypoints[waypoints.length - 1];
			double gap = Math.hypot(first[0] - last[0], first[1] - last[1]);
			assertTrue(
					gap < LOOP_CLOSURE_TOLERANCE_METERS,
					"Loop should close, but start/end gap was " + gap + " m");
		}
	}

	@Test
	public void consecutiveSamplesAreCloselySpaced() {
		for (int[][] trackMap : Game.TRACK_MAPS) {
			double[][] waypoints = TrackGeometry.buildReferencePath(trackMap).waypoints();
			for (int index = 1; index < waypoints.length; index++) {
				double spacing = Math.hypot(
						waypoints[index][0] - waypoints[index - 1][0],
						waypoints[index][1] - waypoints[index - 1][1]);
				assertTrue(
						spacing < SAMPLE_SPACING_UPPER_BOUND_METERS,
						"Sample spacing too large: " + spacing + " m at index " + index);
			}
		}
	}

	@Test
	public void everySampleLiesInsideTheTrackLane() {
		for (int trackIndex = 0; trackIndex < Game.TRACK_MAPS.length; trackIndex++) {
			int[][] trackMap = Game.TRACK_MAPS[trackIndex];
			double[][] waypoints = TrackGeometry.buildReferencePath(trackMap).waypoints();
			for (double[] waypoint : waypoints) {
				assertTrue(
						isInsideLane(trackMap, waypoint[0], waypoint[1]),
						"Sample off track " + (trackIndex + 1)
								+ " at (" + waypoint[0] + ", " + waypoint[1] + ") m");
			}
		}
	}

	@Test
	public void headingsMatchTravelDirectionBetweenSamples() {
		for (int[][] trackMap : Game.TRACK_MAPS) {
			ReferencePath path = TrackGeometry.buildReferencePath(trackMap);
			double[][] waypoints = path.waypoints();
			for (int index = 1; index < waypoints.length; index++) {
				double travelHeading = Math.atan2(
						waypoints[index][1] - waypoints[index - 1][1],
						waypoints[index][0] - waypoints[index - 1][0]);
				double headingGap = Math.abs(DubinsVehicle.wrapAngle(
						travelHeading - path.headingAt(index)));
				assertTrue(
						headingGap < 0.35,
						"Heading inconsistent with travel direction at index " + index
								+ ": gap " + headingGap + " rad");
			}
		}
	}

	@Test
	public void previewCenterlineMatchesReferencePath() {
		for (int[][] trackMap : Game.TRACK_MAPS) {
			assertEquals(
					TrackGeometry.buildReferencePath(trackMap).sampleCount(),
					TrackGeometry.buildCenterline(trackMap).length);
		}
	}

	/**
	 * Mirrors {@link Circuit#enforceTrackBoundaries} lane checks without
	 * requiring a window: straights constrain one axis, corners the radius.
	 */
	private static boolean isInsideLane(int[][] trackMap, double xMeters, double yMeters) {
		double tileSize = WorldUnits.METERS_PER_TILE;
		int row = (int) Math.floor(yMeters / tileSize);
		int column = (int) Math.floor(xMeters / tileSize);
		if (row < 0 || row >= trackMap.length || column < 0 || column >= trackMap[row].length) {
			return false;
		}
		double innerRadius = WorldUnits.pxToM(Circuit.INNER_RADIUS);
		double outerRadius = WorldUnits.pxToM(Circuit.OUTER_RADIUS);
		double localX = xMeters - column * tileSize;
		double localY = yMeters - row * tileSize;
		return switch (trackMap[row][column]) {
			case Circuit.TILE_STRAIGHT_HORIZONTAL -> localX > innerRadius && localX < outerRadius;
			case Circuit.TILE_STRAIGHT_VERTICAL -> localY > innerRadius && localY < outerRadius;
			case Circuit.TILE_CORNER_BOTTOM_RIGHT -> isInsideCorner(localX, localY, 0.0, tileSize, innerRadius, outerRadius);
			case Circuit.TILE_CORNER_TOP_RIGHT -> isInsideCorner(localX, localY, tileSize, tileSize, innerRadius, outerRadius);
			case Circuit.TILE_CORNER_TOP_LEFT -> isInsideCorner(localX, localY, tileSize, 0.0, innerRadius, outerRadius);
			case Circuit.TILE_CORNER_BOTTOM_LEFT -> isInsideCorner(localX, localY, 0.0, 0.0, innerRadius, outerRadius);
			default -> false;
		};
	}

	private static boolean isInsideCorner(
			double x,
			double y,
			double cornerX,
			double cornerY,
			double innerRadius,
			double outerRadius) {
		double radius = Math.hypot(x - cornerX, y - cornerY);
		return radius > innerRadius && radius < outerRadius;
	}
}
