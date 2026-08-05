package controller;

import model.DubinsVehicle;
import model.ReferencePath;

/**
 * Local tracking loop combining a Dubins vehicle model and a path-following controller.
 */
public class TrackingLoop {

	private final DubinsVehicle vehicle;
	private final PdPathFollowController controller;

	public TrackingLoop(DubinsVehicle vehicle, PdPathFollowController controller) {
		this.vehicle = vehicle;
		this.controller = controller;
	}

	public DubinsVehicle getVehicle() {
		return vehicle;
	}

	/**
	 * Integrate one tracking step on the reference path.
	 */
	public void step(ReferencePath path, double deltaSeconds) {
		double[] commands = controller.track(
				vehicle.getX(),
				vehicle.getY(),
				vehicle.getHeading(),
				vehicle.getSpeed(),
				path,
				deltaSeconds);
		vehicle.step(commands[0], commands[1], deltaSeconds);
	}

	/**
	 * Sync vehicle pose from the game car, run one step, and leave updated state in the vehicle.
	 */
	public void stepFromPose(
			double x,
			double y,
			double heading,
			double speed,
			ReferencePath path,
			double deltaSeconds) {
		vehicle.syncPose(x, y, heading, speed);
		step(path, deltaSeconds);
	}
}
