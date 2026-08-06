package controller;

/**
 * Tunable horizon and weights for the hand-rolled Dubins MPCC planner.
 */
public final class MpccConfig {

	public static final MpccConfig DEFAULT = new MpccConfig(
			10,
			0.05,
			20,
			4.0,
			3.0,
			8.0,
			1.2,
			0.15,
			120.0,
			2.5,
			2.0,
			2.5,
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
			double refineStepScale,
			int refinePassCount,
			double cruiseSpeedRatio,
			double curvatureGain) {
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
