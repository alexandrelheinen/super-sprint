package controller;

import model.Car;
import model.Circuit;
import model.DubinsVehicle;
import model.ReferencePath;
import view.GameFrame;

public class AiController extends Controller {

	private static final double MS_PER_SECOND = 1000.0;
	private static final double TURN_RATE_PER_HANDLING = 0.2;
	private static final double TURN_RATE_DOT = 40.0;
	private static final double CURVATURE_GAIN = 0.45;
	private static final double HEADING_KP_PER_HANDLING = 0.09;
	private static final double HEADING_KD_PER_HANDLING = 0.025;
	private static final double CROSS_TRACK_KP_PER_HANDLING = 0.06;
	private static final double SPEED_KP = 2.4;
	private static final double SPEED_KD = 0.8;
	private static final double CRUISE_SPEED_RATIO = 0.88;

	private final ReferencePath referencePath;
	private final TrackingLoop trackingLoop;
	private final DubinsVehicle vehicle;

	public AiController(
			int modelIndex,
			int startPosition,
			GameFrame frame,
			Circuit circuit,
			ReferencePath referencePath) {
		super(modelIndex, startPosition, frame, circuit);
		this.referencePath = referencePath;

		double handling = car.getStat(Car.STAT_HANDLING_INDEX);
		double maxTurnRate = handling * TURN_RATE_PER_HANDLING;
		double maxAcceleration = car.getStat(Car.STAT_ACCELERATION_INDEX);
		double cruiseSpeed = car.getStat(Car.STAT_MAX_SPEED_INDEX) * CRUISE_SPEED_RATIO;

		vehicle = new DubinsVehicle(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				car.getStat(Car.STAT_MAX_SPEED_INDEX),
				0.0,
				maxTurnRate,
				maxAcceleration,
				TURN_RATE_DOT);

		PdPathFollowController pathController = new PdPathFollowController(
				handling * HEADING_KP_PER_HANDLING,
				handling * HEADING_KD_PER_HANDLING,
				handling * CROSS_TRACK_KP_PER_HANDLING,
				SPEED_KP,
				SPEED_KD,
				cruiseSpeed,
				CURVATURE_GAIN);
		trackingLoop = new TrackingLoop(vehicle, pathController);
	}

	@Override
	public void update() {
		double deltaSeconds = Game.TICK_INTERVAL_MS / MS_PER_SECOND;
		trackingLoop.stepFromPose(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				car.getSpeed(),
				referencePath,
				deltaSeconds);
		car.applyKinematicState(
				vehicle.getX(),
				vehicle.getY(),
				(float) vehicle.getHeading(),
				(float) vehicle.getSpeed());
	}
}
