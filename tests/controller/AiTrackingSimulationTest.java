package controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Car;
import model.Circuit;
import model.ReferencePath;
import model.TrackGeometry;
import model.WorldUnits;

/**
 * End-to-end simulation of the AI tracking stack (reference path, Dubins
 * vehicle, PD controller) using the exact gains used in-game. These tests
 * fail whenever the AI cannot actually drive laps.
 */
public class AiTrackingSimulationTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;
	private static final double SIMULATED_SECONDS = 60.0;
	private static final double CONVERGENCE_SECONDS = 3.0;
	private static final double SPEED_MEASUREMENT_START_SECONDS = 5.0;
	private static final double LANE_HALF_WIDTH_METERS =
			WorldUnits.pxToM((Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS) / 2.0);
	private static final double MIN_AVERAGE_SPEED_MS = 10.0;
	private static final int INITIAL_HEADING_QUARTER_TURNS = -1;

	@Test
	public void aiCompletesALapOnEveryTrackFromEveryStartSlot() {
		for (int trackIndex = 0; trackIndex < Game.TRACK_MAPS.length; trackIndex++) {
			for (int slotIndex = 0; slotIndex < Circuit.START_SLOT_COUNT; slotIndex++) {
				SimulationResult result = simulate(trackIndex, slotIndex, slotIndex % Car.CAR_MODEL_COUNT);
				String scenario = "track " + (trackIndex + 1) + ", slot " + (slotIndex + 1);
				assertTrue(
						result.lapsCompleted >= 1,
						"AI failed to complete a lap on " + scenario);
				assertTrue(
						result.maxCrossTrackErrorMeters < LANE_HALF_WIDTH_METERS,
						"AI left the lane on " + scenario
								+ " (max cross-track error " + result.maxCrossTrackErrorMeters + " m)");
				assertTrue(
						result.averageSpeedMs > MIN_AVERAGE_SPEED_MS,
						"AI too slow on " + scenario
								+ " (average speed " + result.averageSpeedMs + " m/s)");
			}
		}
	}

	@Test
	public void aiKeepsMakingForwardProgress() {
		SimulationResult result = simulate(0, 0, 0);
		assertTrue(
				result.lapsCompleted >= 3,
				"Expected at least 3 laps in " + SIMULATED_SECONDS + " s, got " + result.lapsCompleted);
	}

	private static SimulationResult simulate(int trackIndex, int slotIndex, int modelIndex) {
		int[][] trackMap = Game.TRACK_MAPS[trackIndex];
		ReferencePath path = TrackGeometry.buildReferencePath(trackMap);
		float[] startPixels = Circuit.START_POSITIONS[trackIndex][slotIndex];
		double[] stats = Car.CAR_MODEL_STATS[modelIndex];

		TrackingLoop loop = AiController.createTrackingLoop(
				stats[Car.STAT_MAX_SPEED_INDEX],
				stats[Car.STAT_ACCELERATION_INDEX],
				stats[Car.STAT_HANDLING_INDEX],
				WorldUnits.pxToM(startPixels[0]),
				WorldUnits.pxToM(startPixels[1]),
				INITIAL_HEADING_QUARTER_TURNS * Math.PI / 2.0);

		int steps = (int) Math.round(SIMULATED_SECONDS / DELTA_SECONDS);
		int lapsCompleted = 0;
		int previousIndex = -1;
		double maxCrossTrackError = 0.0;
		double speedSum = 0.0;
		int speedSampleCount = 0;

		for (int step = 0; step < steps; step++) {
			loop.step(path, DELTA_SECONDS);
			double elapsed = (step + 1) * DELTA_SECONDS;

			ReferencePath.Projection projection = path.project(
					loop.getVehicle().getX(),
					loop.getVehicle().getY());
			if (previousIndex >= 0 && wrapsForward(previousIndex, projection.closestIndex(), path.sampleCount())) {
				lapsCompleted++;
			}
			previousIndex = projection.closestIndex();

			if (elapsed > CONVERGENCE_SECONDS) {
				maxCrossTrackError = Math.max(
						maxCrossTrackError,
						Math.abs(projection.crossTrackError()));
			}
			if (elapsed > SPEED_MEASUREMENT_START_SECONDS) {
				speedSum += loop.getVehicle().getSpeed();
				speedSampleCount++;
			}
		}

		return new SimulationResult(
				lapsCompleted,
				maxCrossTrackError,
				speedSampleCount > 0 ? speedSum / speedSampleCount : 0.0);
	}

	private static boolean wrapsForward(int previousIndex, int currentIndex, int sampleCount) {
		return previousIndex > sampleCount * 9 / 10 && currentIndex < sampleCount / 10;
	}

	private record SimulationResult(
			int lapsCompleted,
			double maxCrossTrackErrorMeters,
			double averageSpeedMs) {
	}
}
