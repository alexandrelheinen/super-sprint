package view;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.GameConfig;

/**
 * Launches a headless-friendly all-AI exhibition race for release demos.
 *
 * <p>Args: {@code <trackId> <carIds> [laps]}.
 *
 * <ul>
 *   <li>{@code trackId} — zero-based track catalog index
 *   <li>{@code carIds} — comma- or whitespace-separated model indexes
 *       ({@code 0,0,0,0} / {@code 0 0 0 0}), or {@code identical} /
 *       {@code identical:N}
 *   <li>{@code laps} — optional lap count (default 3)
 * </ul>
 */
public final class DemoRaceCapture {

	private static final int DEFAULT_LAP_COUNT = 3;
	/** Hold the Race Complete screen long enough for the release clip to show results. */
	private static final long POST_RACE_HOLD_MS = 5_000L;
	/** Includes racing, post-win coast-down, and the results hold. */
	private static final long MAX_WAIT_MS = 240_000L;

	private DemoRaceCapture() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: DemoRaceCapture <trackId> <carIds> [laps]");
			System.err.println("  trackId  zero-based track index (0.."
					+ (Circuit.TRACK_COUNT - 1) + ")");
			System.err.println("  carIds   comma/space list, e.g. 0,0,0,0 or \"0 0 0 0\"");
			System.err.println("           or identical / identical:N");
			System.err.println("  laps     optional, default " + DEFAULT_LAP_COUNT);
			System.exit(2);
		}

		int trackIndex = parseTrackIndex(args[0], Circuit.TRACK_COUNT);
		int[] carModels = parseCarModels(args[1], GameConfig.MAX_CARS, Car.CAR_MODEL_COUNT);
		int lapCount = args.length > 2
				? Integer.parseInt(args[2])
				: DEFAULT_LAP_COUNT;
		GameCatalog.validateLapCount(lapCount);

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
						trackIndex,
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
								+ trackIndex + " (" + GameCatalog.trackName(trackIndex) + ")"
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
	 * Parses a zero-based track catalog index.
	 */
	public static int parseTrackIndex(String spec, int trackCount) {
		if (spec == null || spec.isBlank()) {
			throw new IllegalArgumentException("trackId is required");
		}
		int trackIndex = Integer.parseInt(spec.trim());
		if (trackIndex < 0 || trackIndex >= trackCount) {
			throw new IllegalArgumentException(
					"Track index out of range: " + trackIndex
							+ " (valid 0.." + (trackCount - 1) + ")");
		}
		return trackIndex;
	}

	/**
	 * Parses a demo car-model specification.
	 *
	 * @param spec comma- or whitespace-separated indexes; {@code identical} /
	 *        {@code identical:N}
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
			throw new IllegalArgumentException("carIds is required");
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

		String[] parts = trimmed.split("[,\\s]+");
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

	private static void validateModelIndex(int modelIndex, int modelCount) {
		if (modelIndex < 0 || modelIndex >= modelCount) {
			throw new IllegalArgumentException(
					"Car model index out of range: " + modelIndex
							+ " (valid 0.." + (modelCount - 1) + ")");
		}
	}
}
