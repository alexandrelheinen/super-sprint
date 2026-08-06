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

	public void checkRaceFinished() {
		if (!running || !racing) {
			return;
		}
		for (int index = 0; index < controllers.length; index++) {
			if (controllers[index].getCar().getLapCount() <= lapCount) {
				continue;
			}
			finishGame(index);
			return;
		}
	}

	private void finishGame(int winnerIndex) {
		if (!running) {
			return;
		}
		running = false;
		gameTimer.cancel();
		gameTimer.purge();

		double raceTimeMs = circuit.getRaceTimeMs();

		SwingUtilities.invokeLater(() -> {
			detachRenderObservers();
			gameFrame.shutdown();
			appShell.showRaceComplete(
					hallOfFame,
					winnerIndex,
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
