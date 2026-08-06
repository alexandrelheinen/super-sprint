package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import controller.Game;
import view.GameFrame;

public class CarArcadeControlsTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;
	private static final double EPSILON = 1e-6;

	@Test
	public void cannotSteerWhileStopped() {
		Car car = carAtRest();
		float heading = car.getAngle();
		car.startSteeringLeft();
		car.applyPhysics(DELTA_SECONDS);
		assertEquals(heading, car.getAngle(), EPSILON);
		assertEquals(0f, car.getSpeed(), EPSILON);
	}

	@Test
	public void steersWhileMoving() {
		Car car = carAtRest();
		car.applyKinematicState(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				20f);
		float heading = car.getAngle();
		car.startSteeringRight();
		car.applyPhysics(DELTA_SECONDS);
		assertTrue(car.getAngle() > heading, "Expected right turn to increase heading");
	}

	@Test
	public void accelerateAndBrakeUseCarAccelerationStat() {
		Car car = carAtRest();
		double accel = car.getStat(Car.STAT_ACCELERATION_INDEX);
		car.startAccelerating();
		car.applyPhysics(DELTA_SECONDS);
		assertEquals(accel * DELTA_SECONDS, car.getSpeed(), 1e-6);

		car.clearControls();
		car.applyKinematicState(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				20f);
		car.startBraking();
		float before = car.getSpeed();
		car.applyPhysics(DELTA_SECONDS);
		assertTrue(car.getSpeed() < before, "Brake should reduce forward speed");
	}

	@Test
	public void clearControlsDropsSteeringSoFinishCoastDoesNotSpin() {
		Car car = carAtRest();
		car.applyKinematicState(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				20f);
		car.startSteeringLeft();
		car.startAccelerating();
		assertTrue(car.isSteeringLeft());
		assertTrue(car.isAccelerating());

		car.clearControls();
		assertFalse(car.isSteeringLeft());
		assertFalse(car.isAccelerating());
		assertFalse(car.isBraking());
		assertFalse(car.isSteeringRight());

		float heading = car.getAngle();
		car.applyPhysics(DELTA_SECONDS);
		assertEquals(heading, car.getAngle(), EPSILON, "Cleared steer must not yaw while coasting");
	}

	@Test
	public void turnRateMatchesHandlingScale() {
		Car car = carAtRest();
		assertEquals(
				car.getStat(Car.STAT_HANDLING_INDEX) * Car.TURN_RATE_PER_HANDLING,
				car.getMaxTurnRate(),
				EPSILON);
	}

	@Test
	public void turnRateIsHumanDrivableAtTickCadence() {
		// At 100 Hz, a mid-handling car should yaw roughly 1–2.5° per tick — arcade
		// snappy, but not the previous ~5°/tick that made corners undrivable.
		Car car = carAtRest();
		double degreesPerTick = Math.toDegrees(car.getMaxTurnRate() * DELTA_SECONDS);
		assertTrue(degreesPerTick > 0.8, "Too sluggish: " + degreesPerTick);
		assertTrue(degreesPerTick < 2.8, "Too twitchy for humans: " + degreesPerTick);
		assertTrue(
				Car.TURN_RATE_PER_HANDLING < 0.12,
				"TURN_RATE_PER_HANDLING should stay well below the old 0.2 scale");
	}

	private static Car carAtRest() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		return new Car(0, 1, "1", frame, circuit);
	}
}
