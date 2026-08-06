package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.ReferencePath;
import model.TrackGeometry;
import view.GameFrame;

/**
 * AI must drive through arcade Car controls / applyPhysics, not a separate plant.
 */
public class AiArcadeControlBridgeTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;
	private static final double EPSILON = 1e-6;

	@Test
	public void stoppedAiCannotChangeHeading() {
		AiController ai = aiOnTrack(0, 0, 0);
		Car car = ai.getCar();
		float heading = car.getAngle();
		assertEquals(0f, car.getSpeed(), EPSILON);

		// Force a strong steer desire while still stopped (full duty).
		ai.applyArcadeControls(0.0, car.getMaxTurnRate());
		car.applyPhysics(DELTA_SECONDS);
		assertEquals(heading, car.getAngle(), EPSILON);
		assertEquals(0f, car.getSpeed(), EPSILON);
	}

	@Test
	public void speedCommandMapsToAccelerateOrBrake() {
		AiController ai = aiOnTrack(0, 0, 0);
		Car car = ai.getCar();
		car.applyKinematicState(
				car.getPositionXMeters(),
				car.getPositionYMeters(),
				car.getAngle(),
				15f);

		ai.applyArcadeControls(30.0, 0.0);
		assertTrue(car.isAccelerating());
		assertFalse(car.isBraking());

		ai.applyArcadeControls(12.0, 0.0);
		assertFalse(car.isBraking(), "Mild slowdown should coast, not brake");
		assertFalse(car.isAccelerating());

		ai.applyArcadeControls(0.0, 0.0);
		assertTrue(car.isBraking());
		assertFalse(car.isAccelerating());
	}

	@Test
	public void updateUsesSharedPhysicsPlantNotKinematicTeleport() {
		AiController ai = aiOnTrack(0, 0, 0);
		Car car = ai.getCar();
		double startX = car.getPositionXMeters();
		double startY = car.getPositionYMeters();
		float startHeading = car.getAngle();

		// One AI tick from rest: may accelerate, but must not pivot in place.
		ai.update();
		assertEquals(startHeading, car.getAngle(), EPSILON);
		assertTrue(car.getSpeed() >= 0f);
		// Pose may move only after speed builds; from a single tick at rest heading stays.
		if (car.getSpeed() == 0f) {
			assertEquals(startX, car.getPositionXMeters(), EPSILON);
			assertEquals(startY, car.getPositionYMeters(), EPSILON);
		}
	}

	private static AiController aiOnTrack(int trackIndex, int slotIndex, int modelIndex) {
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {modelIndex}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		ReferencePath path = TrackGeometry.buildReferencePath(trackMap);
		return new AiController(modelIndex, slotIndex + 1, frame, circuit, path);
	}
}
