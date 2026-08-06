package controller;

import model.Circuit;
import model.WorldUnits;

/**
 * Tunable horizon and weights for the hand-rolled Dubins MPCC planner.
 *
 * <p>Priority order:
 * <ol>
 *   <li>Stay on asphalt (walls are near no-go)</li>
 *   <li>Keep speed / make progress (overtakes are encouraged)</li>
 *   <li>Respect car actuator limits as soft saturating costs (speed, accel, turn)</li>
 *   <li>Prefer not to hit cars (soft, saturating — never a hard barrier)</li>
 *   <li>Stay near the racing line (weak; free band allows passing offsets)</li>
 * </ol>
 */
public final class MpccConfig {

	/** Half-width of the painted lane corridor, matching {@link Circuit} radii. */
	public static final double DEFAULT_LANE_HALF_WIDTH_METERS = WorldUnits.pxToM(
			(Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS) / 2.0);

	/**
	 * Retuned for earlier, longer-horizon traffic avoidance: ~1.2 s look-ahead,
	 * stronger soft car proximity, and a wider trigger so MPCC engages before
	 * contact. Ego radius tracks car half-width more than the bounding circle so
	 * side-by-side passes stay geometrically feasible in-lane. Walls remain
	 * strictly heavier than soft opponent proximity.
	 */
	public static final MpccConfig DEFAULT = new MpccConfig(
			20,
			0.06,
			40,
			0.55,
			0.8,
			2.5,
			9.0,
			2.2,
			2.5,
			14.0,
			14.0,
			12.0,
			18.0,
			1.8,
			1.55,
			7.5,
			900.0,
			DEFAULT_LANE_HALF_WIDTH_METERS,
			2.4,
			2.2,
			3.0,
			1.35,
			3);

	private final int horizonStepCount;
	private final double dtSeconds;
	private final int replanIntervalTicks;
	private final double weightContour;
	private final double weightHeading;
	private final double weightLag;
	private final double weightProgress;
	private final double weightControl;
	private final double weightSpeed;
	private final double weightSpeedLimit;
	private final double weightTurnLimit;
	private final double weightAcceleration;
	private final double weightObstacle;
	private final double obstacleSafeMarginMeters;
	private final double egoRadiusMeters;
	private final double triggerDistanceMeters;
	private final double weightWall;
	private final double laneHalfWidthMeters;
	private final double wallSafeMarginMeters;
	private final double wallTriggerMarginMeters;
	private final double contourDeadzoneMeters;
	private final double refineStepScale;
	private final int refinePassCount;

	public MpccConfig(
			int horizonStepCount,
			double dtSeconds,
			int replanIntervalTicks,
			double weightContour,
			double weightHeading,
			double weightLag,
			double weightProgress,
			double weightControl,
			double weightSpeed,
			double weightSpeedLimit,
			double weightTurnLimit,
			double weightAcceleration,
			double weightObstacle,
			double obstacleSafeMarginMeters,
			double egoRadiusMeters,
			double triggerDistanceMeters,
			double weightWall,
			double laneHalfWidthMeters,
			double wallSafeMarginMeters,
			double wallTriggerMarginMeters,
			double contourDeadzoneMeters,
			double refineStepScale,
			int refinePassCount) {
		if (weightWall <= weightObstacle) {
			throw new IllegalArgumentException(
					"weightWall must exceed weightObstacle (walls are more critical than cars)");
		}
		this.horizonStepCount = horizonStepCount;
		this.dtSeconds = dtSeconds;
		this.replanIntervalTicks = replanIntervalTicks;
		this.weightContour = weightContour;
		this.weightHeading = weightHeading;
		this.weightLag = weightLag;
		this.weightProgress = weightProgress;
		this.weightControl = weightControl;
		this.weightSpeed = weightSpeed;
		this.weightSpeedLimit = weightSpeedLimit;
		this.weightTurnLimit = weightTurnLimit;
		this.weightAcceleration = weightAcceleration;
		this.weightObstacle = weightObstacle;
		this.obstacleSafeMarginMeters = obstacleSafeMarginMeters;
		this.egoRadiusMeters = egoRadiusMeters;
		this.triggerDistanceMeters = triggerDistanceMeters;
		this.weightWall = weightWall;
		this.laneHalfWidthMeters = laneHalfWidthMeters;
		this.wallSafeMarginMeters = wallSafeMarginMeters;
		this.wallTriggerMarginMeters = wallTriggerMarginMeters;
		this.contourDeadzoneMeters = contourDeadzoneMeters;
		this.refineStepScale = refineStepScale;
		this.refinePassCount = refinePassCount;
	}

	public int getHorizonStepCount() {
		return horizonStepCount;
	}

	public double getDtSeconds() {
		return dtSeconds;
	}

	public int getReplanIntervalTicks() {
		return replanIntervalTicks;
	}

	public double getWeightContour() {
		return weightContour;
	}

	public double getWeightHeading() {
		return weightHeading;
	}

	public double getWeightLag() {
		return weightLag;
	}

	public double getWeightProgress() {
		return weightProgress;
	}

	public double getWeightControl() {
		return weightControl;
	}

	public double getWeightSpeed() {
		return weightSpeed;
	}

	public double getWeightSpeedLimit() {
		return weightSpeedLimit;
	}

	public double getWeightTurnLimit() {
		return weightTurnLimit;
	}

	public double getWeightAcceleration() {
		return weightAcceleration;
	}

	public double getWeightObstacle() {
		return weightObstacle;
	}

	public double getObstacleSafeMarginMeters() {
		return obstacleSafeMarginMeters;
	}

	public double getEgoRadiusMeters() {
		return egoRadiusMeters;
	}

	public double getTriggerDistanceMeters() {
		return triggerDistanceMeters;
	}

	public double getWeightWall() {
		return weightWall;
	}

	public double getLaneHalfWidthMeters() {
		return laneHalfWidthMeters;
	}

	public double getWallSafeMarginMeters() {
		return wallSafeMarginMeters;
	}

	public double getWallTriggerMarginMeters() {
		return wallTriggerMarginMeters;
	}

	public double getContourDeadzoneMeters() {
		return contourDeadzoneMeters;
	}

	public double getRefineStepScale() {
		return refineStepScale;
	}

	public int getRefinePassCount() {
		return refinePassCount;
	}
}
