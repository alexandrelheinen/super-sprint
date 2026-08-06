package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import view.GameFrame;

/**
 * Finish-line lap detection must require a real side-to-side crossing through
 * the start lane. Proximity jitter near the line (common after collisions)
 * must not inflate the counter and end a race early.
 */
public class FinishLineLapTest {

	private static final float FORWARD_SPEED_MS = 20f;
	private static final float FORWARD_HEADING = (float) (-Math.PI / 2);
	private static final float REVERSE_HEADING = (float) (Math.PI / 2);

	private Circuit circuit;
	private Car car;
	private double laneCenterXMeters;
	private double lineYMeters;

	@BeforeEach
	public void setUp() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		car = new Car(0, 1, "1", frame, circuit);
		laneCenterXMeters = WorldUnits.pxToM(
				(Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0 - Circuit.CAR_ANCHOR_HALF_WIDTH_PX);
		lineYMeters = WorldUnits.pxToM(GameFrame.TILE_SIZE - Circuit.CAR_ANCHOR_HALF_HEIGHT_PX);
	}

	@Test
	public void forwardCrossingIncrementsLapCountOnce() {
		placeRelativeToLine(10, FORWARD_HEADING);
		assertEquals(0, car.getLapCount());

		placeRelativeToLine(-10, FORWARD_HEADING);
		assertEquals(1, car.getLapCount());

		placeRelativeToLine(-40, FORWARD_HEADING);
		assertEquals(1, car.getLapCount());
	}

	@Test
	public void proximityJitterNearFinishLineDoesNotInflateLaps() {
		placeRelativeToLine(10, FORWARD_HEADING);
		placeRelativeToLine(-10, FORWARD_HEADING);
		assertEquals(1, car.getLapCount());

		for (int index = 0; index < 50; index++) {
			// Stay on the far side and bob within a few pixels of the line.
			placeRelativeToLine(index % 2 == 0 ? -1 : -5, FORWARD_HEADING);
		}
		assertEquals(1, car.getLapCount(), "Far-side jitter must not award extra laps");

		for (int index = 0; index < 50; index++) {
			// Same on the start side after driving back without a scored reverse
			// through the lane - move outside the lane first, then jitter.
			placeAt(WorldUnits.pxToM(3 * GameFrame.TILE_SIZE), lineYMeters + WorldUnits.pxToM(20), FORWARD_HEADING);
			placeRelativeToLine(index % 2 == 0 ? 1 : 5, FORWARD_HEADING);
		}
		assertEquals(1, car.getLapCount(), "Start-side jitter must not award extra laps");
	}

	@Test
	public void reverseCrossingDecrementsLapCount() {
		placeRelativeToLine(10, FORWARD_HEADING);
		placeRelativeToLine(-10, FORWARD_HEADING);
		assertEquals(1, car.getLapCount());

		placeRelativeToLine(10, REVERSE_HEADING);
		assertEquals(0, car.getLapCount());
	}

	@Test
	public void crossingOutsideStartLaneDoesNotCount() {
		double rightLaneX = WorldUnits.pxToM(
				3 * GameFrame.TILE_SIZE
						+ (Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0
						- Circuit.CAR_ANCHOR_HALF_WIDTH_PX);
		placeAt(rightLaneX, lineYMeters + WorldUnits.pxToM(20), FORWARD_HEADING);
		placeAt(rightLaneX, lineYMeters - WorldUnits.pxToM(20), FORWARD_HEADING);
		assertEquals(0, car.getLapCount());
	}

	@Test
	public void fiveLapRaceEndsOnlyAfterSixForwardCrossings() {
		int targetLaps = 5;
		int crossings = 0;
		// Start below the line, then alternate full side transitions in-lane.
		placeRelativeToLine(30, FORWARD_HEADING);
		for (int index = 0; index < targetLaps + 1; index++) {
			placeRelativeToLine(-30, FORWARD_HEADING);
			crossings++;
			assertEquals(crossings, car.getLapCount());
			// Return via the opposite side of the track so the next forward
			// crossing is a fresh side transition through the start lane.
			double rightLaneX = WorldUnits.pxToM(
					3 * GameFrame.TILE_SIZE
							+ (Circuit.INNER_RADIUS + Circuit.OUTER_RADIUS) / 2.0
							- Circuit.CAR_ANCHOR_HALF_WIDTH_PX);
			placeAt(rightLaneX, lineYMeters - WorldUnits.pxToM(30), REVERSE_HEADING);
			placeAt(rightLaneX, lineYMeters + WorldUnits.pxToM(30), REVERSE_HEADING);
			placeRelativeToLine(30, FORWARD_HEADING);
		}
		assertTrue(car.getLapCount() > targetLaps);
		assertEquals(targetLaps + 1, car.getLapCount());
	}

	private void placeRelativeToLine(double deltaYPixels, float heading) {
		placeAt(laneCenterXMeters, lineYMeters + WorldUnits.pxToM(deltaYPixels), heading);
	}

	private void placeAt(double xMeters, double yMeters, float heading) {
		car.applyKinematicState(xMeters, yMeters, heading, FORWARD_SPEED_MS);
	}
}
