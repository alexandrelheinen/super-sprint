package controller;

import java.awt.event.KeyEvent;

import model.Car;
import model.Circuit;
import model.CircuitPath;
import model.DubinsVehicle;
import view.GameFrame;

public class AiController extends Controller {

	private static final double MS_PER_SECOND = 1000.0;
	private static final double TURN_RATE_PER_HANDLING = 0.2;
	private static final double TURN_RATE_DOT = 40.0;
	private static final double ACCELERATION_SCALE = 0.35;
	private static final double LOOKAHEAD_DISTANCE = 90.0;
	private static final double CURVATURE_GAIN = 0.0025;
	private static final double MIN_TRACKING_SPEED = 40.0;

	private final double[][] path;
	private final TrackingLoop trackingLoop;

	public AiController(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		super(modelIndex, startPosition, frame, circuit);
		path = CircuitPath.buildCenterline(circuit.getTrackMap());

		double handling = car.getStat(Car.STAT_HANDLING_INDEX);
		double maxTurnRate = handling * TURN_RATE_PER_HANDLING;
		double maxAcceleration = car.getStat(0) * ACCELERATION_SCALE;
		double cruiseSpeed = Math.max(MIN_TRACKING_SPEED, car.getStat(1) * 0.85);

		DubinsVehicle vehicle = new DubinsVehicle(
				car.getX(),
				car.getY(),
				car.getAngle(),
				car.getStat(1),
				0.0,
				maxTurnRate,
				maxAcceleration,
				TURN_RATE_DOT);
		PurePursuitController pursuitController = new PurePursuitController(LOOKAHEAD_DISTANCE);
		trackingLoop = new TrackingLoop(vehicle, pursuitController, cruiseSpeed, CURVATURE_GAIN);
	}

	@Override
	public void update() {
		double deltaSeconds = Game.TICK_INTERVAL_MS / MS_PER_SECOND;
		double speed = Math.max(Math.abs(car.getSpeed()), MIN_TRACKING_SPEED);
		double turnRate = trackingLoop.computeTurnRate(
				car.getX(),
				car.getY(),
				car.getAngle(),
				speed,
				path,
				deltaSeconds);
		car.setAngle((float) (car.getAngle() + turnRate * deltaSeconds));
		car.applySteeringInput(KeyEvent.VK_UP);
		super.update();
	}
}
