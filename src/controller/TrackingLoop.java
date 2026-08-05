package controller;

import model.DubinsVehicle;

/**
 * Local tracking loop combining a Dubins vehicle model and pure pursuit.
 */
public class TrackingLoop {

	private final DubinsVehicle vehicle;
	private final PurePursuitController controller;
	private final double cruiseSpeed;
	private final double curvatureGain;

	public TrackingLoop(
			DubinsVehicle vehicle,
			PurePursuitController controller,
			double cruiseSpeed,
			double curvatureGain) {
		this.vehicle = vehicle;
		this.controller = controller;
		this.cruiseSpeed = cruiseSpeed;
		this.curvatureGain = curvatureGain;
	}

	/**
	 * Compute a filtered turn-rate command for the current pose and path.
	 */
	public double computeTurnRate(double x, double y, double heading, double speed, double[][] path, double deltaSeconds) {
		vehicle.syncPose(x, y, heading, speed);
		double speedReference = cruiseSpeed / (1.0 + curvatureGain * Math.abs(controller.getCurvature()));
		double[] commands = controller.track(x, y, heading, path, speedReference);
		return vehicle.filterTurnRate(commands[1], deltaSeconds);
	}
}
