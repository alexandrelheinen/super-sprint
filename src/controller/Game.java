package controller;

import java.util.Timer;

import javax.swing.SwingUtilities;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.GameConfig;
import model.HallOfFame;
import model.ReferencePath;
import model.TrackGeometry;
import view.AppShell;
import view.GameFrame;

public class Game {

	public static final int TICK_INTERVAL_MS = 10;
	public static final int TIMER_START_DELAY_TICKS = 5;
	public static final int MS_PER_SECOND = 1000;
	public static final int ONE_BASED_INDEX_OFFSET = 1;
	/** Extra hold after the winner has fully stopped before showing results. */
	public static final int POST_FINISH_HOLD_MS = 2000;
	private static final double MS_PER_SECOND_D = 1000.0;

	/** Track tile maps loaded from {@code tracks.properties}. */
	public static final int[][][] TRACK_MAPS = GameConfig.TRACK_MAPS;

	private final HallOfFame hallOfFame;
	private final AppShell appShell;
	private final GameFrame gameFrame;
	private final Controller[] controllers;
	private final Timer gameTimer;
	private final Circuit circuit;
	private final int[][] trackMap;
	private final int humanPlayerCount;
	private final int lapCount;
	private final int trackIndex;
	private boolean running;
	private boolean racing;
	private boolean finishing;
	private int winnerIndex;
	private double finalRaceTimeMs;
	private boolean winnerStopped;
	private int postStopHoldMs;
	private RaceCountdown countdown;

	public Game(
			int[] carModels,
			int trackNumber,
			int humanPlayers,
			int laps,
			HallOfFame hallOfFame,
			AppShell appShell) {
		Controller.resetPlayerCount();
		GameCatalog.validateLapCount(laps);
		this.trackIndex = trackNumber;
		trackMap = GameCatalog.trackMap(trackNumber);
		this.lapCount = laps;
		this.hallOfFame = hallOfFame;
		this.appShell = appShell;
		this.humanPlayerCount = humanPlayers;

		gameFrame = new GameFrame(carModels, trackMap, trackNumber);
		controllers = new Controller[carModels.length];
		circuit = new Circuit(gameFrame, trackMap);
		circuit.initializeFinishLine(trackNumber);

		ReferencePath aiReferencePath = TrackGeometry.buildReferencePath(trackMap);
		for (int index = 0; index < carModels.length; index++) {
			if (index < humanPlayers) {
				controllers[index] = new HumanController(
						carModels[index], index + ONE_BASED_INDEX_OFFSET, index + ONE_BASED_INDEX_OFFSET, gameFrame,
						circuit);
			} else {
				controllers[index] = new AiController(
						carModels[index], index + ONE_BASED_INDEX_OFFSET, gameFrame, circuit, aiReferencePath);
			}
		}

		Car[] cars = new Car[controllers.length];
		for (int index = 0; index < controllers.length; index++) {
			cars[index] = controllers[index].getCar();
		}
		gameFrame.attachRaceStatus(cars, laps);

		appShell.showRace(gameFrame);

		// Build scenery/sprites and present a full frame before the countdown
		// so 3-2-1-GO never plays over an empty or half-drawn canvas.
		gameFrame.presentPreparedScene(circuit);
		countdown = new RaceCountdown();
		gameFrame.setCountdownPresentation(countdown.label(), countdown.progress(), countdown.isGoStep());
		gameFrame.renderCompositeFrame(circuit);
		gameFrame.setRacingInputEnabled(false);
		racing = false;
		finishing = false;
		winnerIndex = -1;
		finalRaceTimeMs = 0.0;
		winnerStopped = false;
		postStopHoldMs = 0;

		gameTimer = new Timer(true);
		gameTimer.scheduleAtFixedRate(
				new GameTickTask(controllers, circuit, this),
				(long) TIMER_START_DELAY_TICKS * TICK_INTERVAL_MS,
				TICK_INTERVAL_MS);
		running = true;
	}

	public boolean isRunning() {
		return running;
	}

	/** {@code true} once the countdown has finished and cars may move. */
	public boolean isRacing() {
		return racing;
	}

	/**
	 * {@code true} after a winner is decided while cars coast to a stop before
	 * the results screen.
	 */
	public boolean isFinishing() {
		return finishing;
	}

	public RaceCountdown getCountdown() {
		return countdown;
	}

	public GameFrame getGameFrame() {
		return gameFrame;
	}

	public Circuit getCircuit() {
		return circuit;
	}

	void beginRacing() {
		racing = true;
		countdown = null;
		gameFrame.clearCountdownPresentation();
		gameFrame.setRacingInputEnabled(true);
	}

	/**
	 * Starts the finish coast-down when any car has more finish-line crossings
	 * than the configured lap count. Crossing the start/finish on the way off
	 * the grid increments the counter once, so a race of N laps ends at counter
	 * {@code N + 1} (N full circuits after leaving the grid). If several cars
	 * cross the threshold on the same tick, the highest counter wins.
	 */
	public void checkRaceFinished() {
		if (!running || !racing || finishing) {
			return;
		}
		int detectedWinner = -1;
		int bestLapCount = lapCount;
		for (int index = 0; index < controllers.length; index++) {
			int carLapCount = controllers[index].getCar().getLapCount();
			if (carLapCount > bestLapCount) {
				bestLapCount = carLapCount;
				detectedWinner = index;
			}
		}
		if (detectedWinner >= 0) {
			beginFinishSequence(detectedWinner);
		}
	}

	/**
	 * Freezes the official race clock, ignores further driving input, and clears
	 * throttle/brake so every car coasts. Results appear after the winner stops
	 * and {@link #POST_FINISH_HOLD_MS} elapses.
	 */
	private void beginFinishSequence(int detectedWinner) {
		finishing = true;
		racing = false;
		winnerIndex = detectedWinner;
		finalRaceTimeMs = circuit.getRaceTimeMs();
		winnerStopped = false;
		postStopHoldMs = 0;
		gameFrame.setRacingInputEnabled(false);
		for (Controller controller : controllers) {
			controller.getCar().releaseAcceleration();
		}
	}

	/**
	 * Coasts all cars with physics only (no AI path tracking or player input),
	 * without advancing race time. Completes the race once the winner is stopped
	 * for {@link #POST_FINISH_HOLD_MS}.
	 */
	void tickFinishSequence() {
		if (!running || !finishing) {
			return;
		}

		boolean shouldRender = circuit.shouldRenderAfterVisualTick();
		double deltaSeconds = TICK_INTERVAL_MS / MS_PER_SECOND_D;
		for (int index = 0; index < controllers.length; index++) {
			Car car = controllers[index].getCar();
			car.releaseAcceleration();
			circuit.enforceTrackBoundaries(car);
			car.applyPhysics(deltaSeconds);
			for (int otherIndex = 0; otherIndex < index; otherIndex++) {
				car.collideWith(controllers[otherIndex].getCar());
			}
		}

		Car winner = controllers[winnerIndex].getCar();
		if (!winnerStopped) {
			if (winner.getSpeed() == 0f) {
				winnerStopped = true;
			}
		} else {
			winner.setSpeed(0f);
			postStopHoldMs += TICK_INTERVAL_MS;
			if (postStopHoldMs >= POST_FINISH_HOLD_MS) {
				finishGame();
				return;
			}
		}

		if (shouldRender) {
			gameFrame.renderCompositeFrame(circuit);
		}
	}

	private void finishGame() {
		if (!running) {
			return;
		}
		running = false;
		finishing = false;
		gameTimer.cancel();
		gameTimer.purge();

		int completedWinnerIndex = winnerIndex;
		double raceTimeMs = finalRaceTimeMs;

		SwingUtilities.invokeLater(() -> {
			detachRenderObservers();
			gameFrame.shutdown();
			appShell.showRaceComplete(
					hallOfFame,
					completedWinnerIndex,
					humanPlayerCount,
					raceTimeMs,
					lapCount,
					trackIndex);
		});
	}

	private void detachRenderObservers() {
		circuit.deleteObservers();
		for (Controller controller : controllers) {
			controller.getCar().deleteObservers();
		}
	}
}
