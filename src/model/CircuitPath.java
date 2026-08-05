package model;

/**
 * Builds ordered centerline waypoints in meters for path tracking.
 *
 * @deprecated Use {@link TrackGeometry#buildCenterline(int[][])} instead.
 */
@Deprecated
public final class CircuitPath {

	private CircuitPath() {
	}

	public static double[][] buildCenterline(int[][] trackMap) {
		return TrackGeometry.buildCenterline(trackMap);
	}
}
