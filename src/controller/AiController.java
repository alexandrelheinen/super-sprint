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
	private static final double LOOKAHEAD_DISTANCE_METERS = 9.0;
	private static final double CURVATURE_GAIN = 0.0025;
	private static final double MIN_TRACKING_SPEED_MS = 4.0;

	private final double[][] pathMeters;
	private final TrackingLoop trackingLoop;

	public AiController(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		super(modelIndex, startPosition, frame, circuit);
		pathMeters = CircuitPath.buildCenterline(circuit.getTrackMap());

		double handling = car.getStat(Car.STAT_HANDLING_INDEX);
		double maxTurnRate = handling * TURN_RATE_PER_HANDLING;
		double maxAcceleration = car.getStat(Car.STAT_ACCELERATION_INDEX);
		double cruiseSpeed = Math.max(MIN_TRACKING_SPEED_MS, car.getStat(Car.STAT_MAX_SPEED_INDEX) * 0.85);

		DubinsVehicle vehicle = new DubinsVehicle(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				car.getStat(Car.STAT_MAX_SPEED_INDEX),
				0.0,
				maxTurnRate,
				maxAcceleration,
				TURN_RATE_DOT);
		PurePursuitController pursuitController = new PurePursuitController(LOOKAHEAD_DISTANCE_METERS);
		trackingLoop = new TrackingLoop(vehicle, pursuitController, cruiseSpeed, CURVATURE_GAIN);
	}

	@Override
	public void update() {
		double deltaSeconds = Game.TICK_INTERVAL_MS / MS_PER_SECOND;
		double speedMs = Math.max(Math.abs(car.getSpeed()), MIN_TRACKING_SPEED_MS);
		double turnRate = trackingLoop.computeTurnRate(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				speedMs,
				pathMeters,
				deltaSeconds);
		car.setAngle((float) (car.getAngle() + turnRate * deltaSeconds));
		car.applySteeringInput(KeyEvent.VK_UP);
		super.update();
	}
}
