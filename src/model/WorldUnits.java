package model;

/**
 * Converts between simulation coordinates (SI) and view coordinates (pixels).
 */
public final class WorldUnits {

	public static final double PIXELS_PER_METER = GameConfig.PIXELS_PER_METER;
	public static final double METERS_PER_TILE = GameConfig.METERS_PER_TILE;

	private WorldUnits() {
	}

	public static double pxToM(double pixels) {
		return pixels / PIXELS_PER_METER;
	}

	public static double mToPx(double meters) {
		return meters * PIXELS_PER_METER;
	}

	public static int mToPxRounded(double meters) {
		return (int) Math.round(mToPx(meters));
	}

	public static float mToPxFloat(double meters) {
		return (float) mToPx(meters);
	}

	public static String formatMetersPerSecond(double speedMs) {
		return String.format("%.1f m/s", speedMs);
	}

	public static String formatMetersPerSecondSquared(double accelerationMs2) {
		return String.format("%.1f m/s²", accelerationMs2);
	}

	public static String formatHandling(double handling) {
		return String.format("%.0f", handling);
	}
}
