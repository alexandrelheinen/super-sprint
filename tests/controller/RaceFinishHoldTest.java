package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import view.GameFrame;

/**
 * Verifies the post-win coast-down contract: with throttle cleared, a moving car
 * reaches zero speed under physics, and the results hold lasts two seconds of
 * ticks after that.
 */
public class RaceFinishHoldTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;

	@Test
	public void postFinishHoldIsTwoSecondsOfTicks() {
		assertEquals(2000, Game.POST_FINISH_HOLD_MS);
		assertEquals(200, Game.POST_FINISH_HOLD_MS / Game.TICK_INTERVAL_MS);
	}

	@Test
	public void clearedCommandsCoastToStopUnderPhysics() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		Car car = new Car(0, 1, "1", frame, circuit);

		double xMeters = car.getPositionXMeters();
		double yMeters = car.getPositionYMeters();
		car.applyKinematicState(xMeters, yMeters, car.getAngle(), 25f);
		car.clearControls();

		int maxSteps = 20_000;
		int steps = 0;
		while (car.getSpeed() != 0f && steps < maxSteps) {
			circuit.shouldRenderAfterVisualTick();
			car.clearControls();
			car.applyPhysics(DELTA_SECONDS);
			steps++;
		}

		assertEquals(0f, car.getSpeed(), "Car should coast to a full stop after commands are cleared");
		assertTrue(steps > 0, "Coast-down should take at least one physics step");
		assertTrue(
				steps * Game.TICK_INTERVAL_MS < 30_000,
				"Coast-down should finish within a reasonable time");
	}
}
