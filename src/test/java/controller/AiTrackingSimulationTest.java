package controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.PhysicsSimulator;
import model.ReferencePath;
import model.TrackGeometry;
import model.WorldUnits;
import view.GameFrame;

/**
 * End-to-end simulation of the in-game AI stack: PD/MPCC commands mapped to
 * arcade Car controls and integrated via {@link PhysicsSimulator#simulateStep}.
 */
public class AiTrackingSimulationTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;
	private static final double SIMULATED_SECONDS = 60.0;
	private static final double CONVERGENCE_SECONDS = 3.0;
	private static final double SPEED_MEASUREMENT_START_SECONDS = 5.0;
	private static final double LANE_HALF_WIDTH_METERS =
			WorldUnits.pxToM((Circuit.OUTER_RADIUS - Circuit.INNER_RADIUS) / 2.0);
	private static final double MIN_AVERAGE_SPEED_MS = 8.0;

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
				result.lapsCompleted >= 2,
				"Expected at least 2 laps in " + SIMULATED_SECONDS + " s, got " + result.lapsCompleted);
	}

	private static SimulationResult simulate(int trackIndex, int slotIndex, int modelIndex) {
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {modelIndex}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		ReferencePath path = TrackGeometry.buildReferencePath(trackMap);
		AiController ai = new AiController(modelIndex, slotIndex + 1, frame, circuit, path);

		int steps = (int) Math.round(SIMULATED_SECONDS / DELTA_SECONDS);
		int lapsCompleted = 0;
		int previousIndex = -1;
		double maxCrossTrackError = 0.0;
		double speedSum = 0.0;
		int speedSampleCount = 0;
		Car car = ai.getCar();

		for (int step = 0; step < steps; step++) {
			circuit.shouldRenderAfterVisualTick();
			ai.update();
			PhysicsSimulator.simulateStep(new Car[] {car}, circuit, DELTA_SECONDS);

			double elapsed = (step + 1) * DELTA_SECONDS;
			ReferencePath.Projection projection = path.project(
					car.getPositionXMeters(),
					car.getPositionYMeters());
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
				speedSum += Math.abs(car.getSpeed());
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
