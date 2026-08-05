package controller;

import model.DubinsVehicle;
import model.ReferencePath;

/**
 * PD path-following controller for a Dubins unicycle.
 *
 * <p>The turn-rate command combines a curvature feedforward term
 * ({@code speed * referenceCurvature}) with proportional–derivative action on
 * heading error and a proportional cross-track correction that steers back
 * towards the reference line. Speed tracking ramps toward a
 * curvature-modulated cruise reference using a second PD loop. Commands are
 * saturated by {@link DubinsVehicle#step(double, double, double)}.
 */
public class PdPathFollowController {

	private static final double MIN_CROSS_TRACK_SPEED = 1.0;

	private final double kpHeading;
	private final double kdHeading;
	private final double kpCrossTrack;
	private final double kpSpeed;
	private final double kdSpeed;
	private final double cruiseSpeed;
	private final double curvatureGain;

	private double previousHeadingError;
	private double previousSpeedError;

	public PdPathFollowController(
			double kpHeading,
			double kdHeading,
			double kpCrossTrack,
			double kpSpeed,
			double kdSpeed,
			double cruiseSpeed,
			double curvatureGain) {
		this.kpHeading = kpHeading;
		this.kdHeading = kdHeading;
		this.kpCrossTrack = kpCrossTrack;
		this.kpSpeed = kpSpeed;
		this.kdSpeed = kdSpeed;
		this.cruiseSpeed = cruiseSpeed;
		this.curvatureGain = curvatureGain;
	}

	public double getCrossTrackError() {
		return crossTrackError;
	}

	public double getHeadingError() {
		return headingError;
	}

	public double getCurvature() {
		return curvature;
	}

	private double crossTrackError;
	private double headingError;
	private double curvature;
	private int lastProjectionIndex = ReferencePath.NO_HINT;

	/**
	 * @return {@code [speedCommand, turnRateCommand]}
	 */
	public double[] track(
			double x,
			double y,
			double heading,
			double speed,
			ReferencePath path,
			double deltaSeconds) {
		if (path.isEmpty() || deltaSeconds <= 0.0) {
			return new double[] {speed, 0.0};
		}

		ReferencePath.Projection projection = path.project(x, y, lastProjectionIndex);
		lastProjectionIndex = projection.closestIndex();
		curvature = projection.curvature();
		crossTrackError = projection.crossTrackError();
		headingError = DubinsVehicle.wrapAngle(projection.referenceHeading() - heading);

		double headingErrorDerivative = (headingError - previousHeadingError) / deltaSeconds;
		previousHeadingError = headingError;
		// Positive cross-track error means the vehicle is right of the path
		// (screen coordinates, y down), so steer left: subtract the term.
		// The correction angle shrinks with speed to avoid oscillation.
		double crossTrackAngle = Math.atan2(
				kpCrossTrack * crossTrackError,
				Math.max(Math.abs(speed), MIN_CROSS_TRACK_SPEED));
		double turnRateCommand = speed * curvature
				+ kpHeading * (headingError - crossTrackAngle)
				+ kdHeading * headingErrorDerivative;

		double speedReference = cruiseSpeed / (1.0 + curvatureGain * Math.abs(curvature));
		double speedError = speedReference - speed;
		double speedErrorDerivative = (speedError - previousSpeedError) / deltaSeconds;
		previousSpeedError = speedError;
		double speedCommand = speed + kpSpeed * speedError + kdSpeed * speedErrorDerivative;

		return new double[] {speedCommand, turnRateCommand};
	}
}
