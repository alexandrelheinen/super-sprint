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
}
