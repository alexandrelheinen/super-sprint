package view;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import model.Car;
import model.GameCatalog;
import model.GameConfig;

/**
 * Launches a headless-friendly all-AI exhibition race for release demos.
 *
 * <p>Defaults: Dune Horseshoe, three laps, four distinct car models, zero human
 * players.
 *
 * <p>Args: {@code [laps] [cars]}. {@code cars} may be a comma-separated list of
 * model indexes ({@code 0,1,2,3}), {@code identical} (all model 0), or
 * {@code identical:N}. The {@code DEMO_CARS} environment variable is used when
 * the cars argument is omitted.
 */
public final class DemoRaceCapture {

	private static final int DUNE_HORSESHOE_TRACK_INDEX = 3;
	/** Yellow, green, blue, and red liveries - visually distinct on sand. */
	private static final int[] DEFAULT_DISTINCT_CAR_MODELS = {0, 1, 2, 3};
	private static final int DEFAULT_LAP_COUNT = 3;
	private static final String ENV_DEMO_CARS = "DEMO_CARS";
	/** Hold the Race Complete screen long enough for the release clip to show results. */
	private static final long POST_RACE_HOLD_MS = 5_000L;
	/** Includes racing, post-win coast-down, and the results hold. */
	private static final long MAX_WAIT_MS = 240_000L;

	private DemoRaceCapture() {
	}

	public static void main(String[] args) throws Exception {
		int lapCount = args.length > 0
				? Integer.parseInt(args[0])
				: DEFAULT_LAP_COUNT;
		GameCatalog.validateLapCount(lapCount);

		String carSpec = args.length > 1 ? args[1] : System.getenv(ENV_DEMO_CARS);
		int[] carModels = parseCarModels(carSpec, GameConfig.MAX_CARS, Car.CAR_MODEL_COUNT);

		CountDownLatch raceFinished = new CountDownLatch(1);
		AtomicReference<AppShell> shellRef = new AtomicReference<>();
		AtomicReference<Throwable> startupFailure = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			try {
				AppShell shell = new AppShell();
				shellRef.set(shell);
				shell.toFront();
				shell.requestFocus();
				shell.startAiExhibitionRace(
						carModels,
						DUNE_HORSESHOE_TRACK_INDEX,
						lapCount,
						() -> new Thread(() -> {
							try {
								Thread.sleep(POST_RACE_HOLD_MS);
							} catch (InterruptedException interrupted) {
								Thread.currentThread().interrupt();
							} finally {
								raceFinished.countDown();
							}
						}, "demo-race-hold").start());
				System.out.println(
						"Demo race started: track="
								+ GameCatalog.trackName(DUNE_HORSESHOE_TRACK_INDEX)
								+ " laps=" + lapCount
								+ " cars=" + Arrays.toString(carModels)
								+ " (all AI)");
			} catch (Throwable failure) {
				startupFailure.set(failure);
				raceFinished.countDown();
			}
		});

		if (startupFailure.get() != null) {
			throw new RuntimeException("Failed to start demo race", startupFailure.get());
		}

		boolean finished = raceFinished.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS);
		SwingUtilities.invokeAndWait(() -> {
			AppShell shell = shellRef.get();
			if (shell != null) {
				shell.dispose();
			}
		});

		if (!finished) {
			System.err.println("Demo race timed out after " + MAX_WAIT_MS + " ms");
			System.exit(1);
		}
		System.out.println("Demo race finished");
		System.exit(0);
	}

	/**
	 * Parses a demo car-model specification.
	 *
	 * @param spec {@code null}/blank for the default distinct liveries;
	 *        {@code identical} / {@code identical:N}; or comma-separated indexes
	 * @param slotCount number of race slots to fill
	 * @param modelCount valid model index upper bound (exclusive)
	 */
	public static int[] parseCarModels(String spec, int slotCount, int modelCount) {
		if (slotCount <= 0) {
			throw new IllegalArgumentException("slotCount must be positive");
		}
		if (modelCount <= 0) {
			throw new IllegalArgumentException("modelCount must be positive");
		}
		if (spec == null || spec.isBlank()) {
			return defaultDistinctModels(slotCount);
		}

		String trimmed = spec.trim();
		String lower = trimmed.toLowerCase();
		if (lower.equals("identical") || lower.startsWith("identical:")) {
			int modelIndex = 0;
			if (lower.startsWith("identical:")) {
				modelIndex = Integer.parseInt(trimmed.substring("identical:".length()).trim());
			}
			validateModelIndex(modelIndex, modelCount);
			int[] models = new int[slotCount];
			Arrays.fill(models, modelIndex);
			return models;
		}

		String[] parts = trimmed.split(",");
		if (parts.length != slotCount) {
			throw new IllegalArgumentException(
					"Expected " + slotCount + " car model indexes, got " + parts.length
							+ " from '" + spec + "'");
		}
		int[] models = new int[slotCount];
		for (int index = 0; index < parts.length; index++) {
			int modelIndex = Integer.parseInt(parts[index].trim());
			validateModelIndex(modelIndex, modelCount);
			models[index] = modelIndex;
		}
		return models;
	}

	private static int[] defaultDistinctModels(int slotCount) {
		int[] models = new int[slotCount];
		for (int index = 0; index < slotCount; index++) {
			models[index] = index < DEFAULT_DISTINCT_CAR_MODELS.length
					? DEFAULT_DISTINCT_CAR_MODELS[index]
					: index % Car.CAR_MODEL_COUNT;
		}
		return models;
	}

	private static void validateModelIndex(int modelIndex, int modelCount) {
		if (modelIndex < 0 || modelIndex >= modelCount) {
			throw new IllegalArgumentException(
					"Car model index out of range: " + modelIndex
							+ " (valid 0.." + (modelCount - 1) + ")");
		}
	}
}
