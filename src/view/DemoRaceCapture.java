package view;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import model.GameCatalog;

/**
 * Launches a headless-friendly all-AI exhibition race for release demos.
 *
 * <p>Defaults: Dune Horseshoe, three laps, four distinct car models, zero human
 * players. Optional arg: {@code [laps]}.
 */
public final class DemoRaceCapture {

	private static final int DUNE_HORSESHOE_TRACK_INDEX = 3;
	/** Yellow, green, blue, and red liveries - visually distinct on sand. */
	private static final int[] DEMO_CAR_MODELS = {0, 1, 2, 3};
	private static final int DEFAULT_LAP_COUNT = 3;
	private static final long POST_RACE_HOLD_MS = 4_000L;
	private static final long MAX_WAIT_MS = 180_000L;

	private DemoRaceCapture() {
	}

	public static void main(String[] args) throws Exception {
		int lapCount = args.length > 0
				? Integer.parseInt(args[0])
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
						DEMO_CAR_MODELS,
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
								+ " cars=0,1,2,3 (all AI)");
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
}
