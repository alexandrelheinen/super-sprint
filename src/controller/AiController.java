package controller;

import java.util.ArrayList;
import java.util.List;

import model.Car;
import model.Circuit;
import model.DubinsVehicle;
import model.ReferencePath;
import model.WorldUnits;
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
	private final HybridMpccPathFollowController hybridController;

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
				handling * TURN_RATE_PER_HANDLING,
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
