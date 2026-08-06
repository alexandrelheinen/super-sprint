package controller;

import model.Circuit;
import model.WorldUnits;

/**
 * Tunable horizon and weights for the hand-rolled Dubins MPCC planner.
 *
 * <p>Wall / track-boundary soft constraints are intentionally weighted much
 * higher than opponent soft constraints: leaving the asphalt is more critical
 * than brushing another car. Opponent costs are only a mild preference — the
 * planner should still take risks to bypass traffic when progress pays for it.
 */
public final class MpccConfig {

	/** Half-width of the painted lane corridor, matching {@link Circuit} radii. */
	public static final double DEFAULT_LANE_HALF_WIDTH_METERS = WorldUnits.pxToM(
			(Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS) / 2.0);

	public static final MpccConfig DEFAULT = new MpccConfig(
			10,
			0.05,
			20,
			6.0,
			3.0,
			8.0,
			2.4,
			0.15,
			12.0,
			1.8,
			2.0,
			2.5,
			500.0,
			DEFAULT_LANE_HALF_WIDTH_METERS,
			2.0,
			3.0,
			3.0,
			4,
			0.8,
			0.35);

	private final int horizonStepCount;
	private final double dtSeconds;
	private final int replanIntervalTicks;
	private final double weightContour;
	private final double weightHeading;
	private final double weightLag;
	private final double weightProgress;
	private final double weightControl;
	private final double weightObstacle;
	private final double obstacleSafeMarginMeters;
	private final double egoRadiusMeters;
	private final double triggerDistanceMeters;
	private final double weightWall;
	private final double laneHalfWidthMeters;
	private final double wallSafeMarginMeters;
	private final double wallTriggerMarginMeters;
	private final double refineStepScale;
	private final int refinePassCount;
	private final double cruiseSpeedRatio;
	private final double curvatureGain;

	public MpccConfig(
			int horizonStepCount,
			double dtSeconds,
			int replanIntervalTicks,
			double weightContour,
			double weightHeading,
			double weightLag,
			double weightProgress,
			double weightControl,
			double weightObstacle,
			double obstacleSafeMarginMeters,
			double egoRadiusMeters,
			double triggerDistanceMeters,
			double weightWall,
			double laneHalfWidthMeters,
			double wallSafeMarginMeters,
			double wallTriggerMarginMeters,
			double refineStepScale,
			int refinePassCount,
			double cruiseSpeedRatio,
			double curvatureGain) {
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
		this.weightObstacle = weightObstacle;
		this.obstacleSafeMarginMeters = obstacleSafeMarginMeters;
		this.egoRadiusMeters = egoRadiusMeters;
		this.triggerDistanceMeters = triggerDistanceMeters;
		this.weightWall = weightWall;
		this.laneHalfWidthMeters = laneHalfWidthMeters;
		this.wallSafeMarginMeters = wallSafeMarginMeters;
		this.wallTriggerMarginMeters = wallTriggerMarginMeters;
		this.refineStepScale = refineStepScale;
		this.refinePassCount = refinePassCount;
		this.cruiseSpeedRatio = cruiseSpeedRatio;
		this.curvatureGain = curvatureGain;
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

	public double getRefineStepScale() {
		return refineStepScale;
	}

	public int getRefinePassCount() {
		return refinePassCount;
	}

	public double getCruiseSpeedRatio() {
		return cruiseSpeedRatio;
	}

	public double getCurvatureGain() {
		return curvatureGain;
	}
}
