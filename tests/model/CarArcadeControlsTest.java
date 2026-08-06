package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		car.setSteeringLeft(true);
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
		car.setSteeringRight(true);
		car.applyPhysics(DELTA_SECONDS);
		assertTrue(car.getAngle() > heading, "Expected right turn to increase heading");
	}

	@Test
	public void accelerateAndBrakeUseCarAccelerationStat() {
		Car car = carAtRest();
		double accel = car.getStat(Car.STAT_ACCELERATION_INDEX);
		car.setAccelerating(true);
		car.applyPhysics(DELTA_SECONDS);
		assertEquals(accel * DELTA_SECONDS, car.getSpeed(), 1e-6);

		car.clearControls();
		car.applyKinematicState(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				20f);
		car.setBraking(true);
		float before = car.getSpeed();
		car.applyPhysics(DELTA_SECONDS);
		assertTrue(car.getSpeed() < before, "Brake should reduce forward speed");
	}

	@Test
	public void turnRateMatchesHandlingScale() {
		Car car = carAtRest();
		assertEquals(
				car.getStat(Car.STAT_HANDLING_INDEX) * Car.TURN_RATE_PER_HANDLING,
				car.getMaxTurnRate(),
				EPSILON);
	}

	private static Car carAtRest() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		return new Car(0, 1, "1", frame, 1, circuit);
	}
}
