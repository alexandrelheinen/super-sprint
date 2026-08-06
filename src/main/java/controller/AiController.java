package controller;

import java.util.ArrayList;
import java.util.List;

import model.Car;
import model.Circuit;
import model.DubinsVehicle;
import model.ReferencePath;
import model.WorldUnits;
import view.GameFrame;

/**
 * AI opponent that plans with PD / sparse MPCC, then drives through the same
 * arcade car controls a human uses (accelerate, brake, steer left/right) so the
 * shared {@link Car#applyPhysics(double)} plant enforces identical saturations
 * and the no-steer-while-stopped rule.
 */
public class AiController extends Controller {

	private static final double MS_PER_SECOND = 1000.0;
	private static final double TURN_RATE_DOT = 40.0;
	private static final double CURVATURE_GAIN = 0.45;
	private static final double HEADING_KP_PER_HANDLING = 0.09;
	private static final double HEADING_KD_PER_HANDLING = 0.025;
	private static final double CROSS_TRACK_KP_PER_HANDLING = 0.06;
	private static final double SPEED_KP = 2.4;
	private static final double SPEED_KD = 0.8;
	/**
	 * Target straight-line cruise as a fraction of the car model's
	 * {@code maxSpeed}. Must stay at {@code 1.0} so PD and MPCC share the same
	 * saturated top speed.
	 */
	private static final double CRUISE_SPEED_RATIO = 1.0;
	/** Ignore tiny speed-command errors so throttle does not chatter. */
	private static final double SPEED_COMMAND_DEADBAND_MS = 1.25;
	/**
	 * Only press brake when clearly too fast; mild shortfalls coast like a human
	 * releasing throttle ({@code 0.5 * accel}).
	 */
	private static final double BRAKE_COMMAND_DEADBAND_MS = 4.0;
	/** Minimum |turn fraction| before pulsing left/right. */
	private static final double TURN_FRACTION_DEADBAND = 0.05;

	private final ReferencePath referencePath;
	private final TrackingLoop trackingLoop;
	private final DubinsVehicle vehicle;
	private final HybridMpccPathFollowController hybridController;
	/** Accumulator for bang-bang steering duty cycle (same buttons, averaged rate). */
	private double steerDutyAccumulator;

	public AiController(
			int modelIndex,
			int startPosition,
			GameFrame frame,
			Circuit circuit,
			ReferencePath referencePath) {
		super(modelIndex, startPosition, frame, circuit);
		this.referencePath = referencePath;
		trackingLoop = createHybridTrackingLoop(
				car.getStat(Car.STAT_MAX_SPEED_INDEX),
				car.getStat(Car.STAT_ACCELERATION_INDEX),
				car.getStat(Car.STAT_HANDLING_INDEX),
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle());
		vehicle = trackingLoop.getVehicle();
		hybridController = (HybridMpccPathFollowController) trackingLoop.getController();
	}

	@Override
	protected String driverLabelForSeat(int seatNumber) {
		return "C";
	}

	/**
	 * Builds the vehicle model and PD controller used by AI drivers. Exposed
	 * so tests can simulate exactly the tracking setup used in-game before MPCC.
	 */
	public static TrackingLoop createTrackingLoop(
			double maxSpeedMs,
			double maxAccelerationMs2,
			double handling,
			double xMeters,
			double yMeters,
			double heading) {
		DubinsVehicle vehicle = createVehicle(
				maxSpeedMs,
				maxAccelerationMs2,
				handling,
				xMeters,
				yMeters,
				heading);
		return new TrackingLoop(vehicle, createPdController(maxSpeedMs, handling));
	}

	/**
	 * Builds the hybrid PD + sparse MPCC stack used by in-game AI opponents.
	 */
	public static TrackingLoop createHybridTrackingLoop(
			double maxSpeedMs,
			double maxAccelerationMs2,
			double handling,
			double xMeters,
			double yMeters,
			double heading) {
		return createHybridTrackingLoop(
				maxSpeedMs,
				maxAccelerationMs2,
				handling,
				xMeters,
				yMeters,
				heading,
				MpccConfig.DEFAULT);
	}

	public static TrackingLoop createHybridTrackingLoop(
			double maxSpeedMs,
			double maxAccelerationMs2,
			double handling,
			double xMeters,
			double yMeters,
			double heading,
			MpccConfig config) {
		DubinsVehicle vehicle = createVehicle(
				maxSpeedMs,
				maxAccelerationMs2,
				handling,
				xMeters,
				yMeters,
				heading);
		PdPathFollowController pdController = createPdController(maxSpeedMs, handling);
		HybridMpccPathFollowController hybridController = new HybridMpccPathFollowController(
				pdController,
				vehicle,
				config);
		return new TrackingLoop(vehicle, hybridController);
	}

	/**
	 * Publishes nearby cars so the MPCC soft constraints can avoid collisions.
	 */
	public void updateOpponents(Controller[] controllers) {
		List<DynamicObstacle> obstacles = new ArrayList<>();
		for (Controller other : controllers) {
			if (other == this) {
				continue;
			}
			Car opponent = other.getCar();
			double radiusMeters = WorldUnits.pxToM(
					0.5 * Math.hypot(opponent.getSpriteWidth(), opponent.getSpriteHeight()));
			obstacles.add(new DynamicObstacle(
					opponent.getPositionXMeters(),
					opponent.getPositionYMeters(),
					opponent.getAngle(),
					opponent.getSpeed(),
					Math.max(radiusMeters, 1.0)));
		}
		hybridController.setObstacles(obstacles);
	}

	public boolean wasLastCommandFromMpcc() {
		return hybridController.wasLastCommandFromMpcc();
	}

	@Override
	public void update() {
		double deltaSeconds = Game.TICK_INTERVAL_MS / MS_PER_SECOND;
		vehicle.syncPose(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				car.getSpeed());
		double[] commands = hybridController.track(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				car.getSpeed(),
				referencePath,
				deltaSeconds);
		applyArcadeControls(commands[0], commands[1]);
		car.applyPhysics(deltaSeconds);
	}

	/**
	 * Maps planner speed / turn-rate commands onto the same discrete arcade
	 * buttons a human holds. Steering uses a duty cycle so the <em>average</em>
	 * yaw rate tracks the continuous planner command while each tick still only
	 * presses full left, full right, or neither.
	 */
	public void applyArcadeControls(double speedCommand, double turnRateCommand) {
		double speedError = speedCommand - car.getSpeed();
		if (speedError > SPEED_COMMAND_DEADBAND_MS) {
			car.startAccelerating();
		} else if (speedError < -BRAKE_COMMAND_DEADBAND_MS) {
			car.startBraking();
		} else {
			car.stopAccelerating();
			car.stopBraking();
		}

		double maxTurnRate = Math.max(car.getMaxTurnRate(), 1e-6);
		double turnFraction = clamp(turnRateCommand / maxTurnRate, -1.0, 1.0);
		car.stopSteeringLeft();
		car.stopSteeringRight();
		if (Math.abs(turnFraction) < TURN_FRACTION_DEADBAND) {
			steerDutyAccumulator = 0.0;
			return;
		}
		steerDutyAccumulator += Math.abs(turnFraction);
		if (steerDutyAccumulator >= 1.0) {
			steerDutyAccumulator -= 1.0;
			if (turnFraction > 0.0) {
				car.startSteeringRight();
			} else {
				car.startSteeringLeft();
			}
		}
	}

	private static double clamp(double value, double min, double max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private static DubinsVehicle createVehicle(
			double maxSpeedMs,
			double maxAccelerationMs2,
			double handling,
			double xMeters,
			double yMeters,
			double heading) {
		return new DubinsVehicle(
				xMeters,
				yMeters,
				heading,
				maxSpeedMs,
				0.0,
				handling * Car.TURN_RATE_PER_HANDLING,
				maxAccelerationMs2,
				TURN_RATE_DOT);
	}

	private static PdPathFollowController createPdController(double maxSpeedMs, double handling) {
		return new PdPathFollowController(
				handling * HEADING_KP_PER_HANDLING,
				handling * HEADING_KD_PER_HANDLING,
				handling * CROSS_TRACK_KP_PER_HANDLING,
				SPEED_KP,
				SPEED_KD,
				maxSpeedMs * CRUISE_SPEED_RATIO,
				CURVATURE_GAIN);
	}
}
