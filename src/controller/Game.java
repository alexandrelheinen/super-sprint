package controller;

import java.util.Timer;

import javax.swing.SwingUtilities;

import model.Circuit;
import model.GameCatalog;
import model.HallOfFame;
import view.GameFrame;
import view.MenuFrame;

public class Game {

	public static final int TICK_INTERVAL_MS = 10;

	private final HallOfFame hallOfFame;
	private final MenuFrame menuFrame;
	private final GameFrame gameFrame;
	private final Controller[] controllers;
	private final Timer gameTimer;
	private final Circuit circuit;
	private final int[][] trackMap;
	private final int humanPlayerCount;
	private final int lapCount;
	private final int trackIndex;
	private boolean running;

	public static final int[][][] TRACK_MAPS = {
			{{4, 2, 2, 3}, {1, 7, 7, 1}, {5, 2, 2, 6}},
			{{4, 3, 7, 7}, {1, 5, 2, 3}, {5, 2, 2, 6}},
			{{4, 2, 2, 3}, {1, 7, 4, 6}, {5, 2, 6, 7}},
			{{4, 3, 4, 3}, {1, 5, 6, 1}, {5, 2, 2, 6}}
	};

	public Game(
			int[] carModels,
			int trackNumber,
			int humanPlayers,
			int laps,
			HallOfFame hallOfFame,
			MenuFrame menuFrame) {
		Controller.resetPlayerCount();
		this.trackIndex = trackNumber;
		trackMap = Game.TRACK_MAPS[trackNumber - 1];
		this.lapCount = laps;
		this.hallOfFame = hallOfFame;
		this.menuFrame = menuFrame;
		this.humanPlayerCount = humanPlayers;

		gameFrame = new GameFrame(
				"Super Sprint Supelec — " + GameCatalog.trackName(trackNumber),
				carModels,
				trackMap,
				trackNumber);
		controllers = new Controller[carModels.length];
		circuit = new Circuit(gameFrame, trackMap);
		circuit.initializeFinishLine(trackNumber);

		for (int index = 0; index < carModels.length; index++) {
			if (index < humanPlayers) {
				controllers[index] = new HumanController(
						carModels[index], index + 1, index + 1, gameFrame, circuit);
			} else {
				controllers[index] = new AiController(carModels[index], index + 1, gameFrame, circuit);
			}
		}

		gameTimer = new Timer(true);
		gameTimer.scheduleAtFixedRate(
				new GameTickTask(controllers, circuit, this),
				(long) 5 * TICK_INTERVAL_MS,
				TICK_INTERVAL_MS);
		running = true;
	}

	public boolean isRunning() {
		return running;
	}

	public void checkRaceFinished() {
		if (!running) {
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
		int track = trackIndex - 1;
		boolean humanWon = winnerIndex < humanPlayerCount;

		SwingUtilities.invokeLater(() -> {
			detachRenderObservers();
			gameFrame.shutdown();
			if (humanWon) {
				hallOfFame.tryAddResult(raceTimeMs, track);
			} else {
				hallOfFame.tryAddResult(raceTimeMs * 1000.0, track);
				javax.swing.JOptionPane.showMessageDialog(menuFrame, "The computer won.");
			}
			menuFrame.showMenu();
		});
	}

	private void detachRenderObservers() {
		circuit.deleteObservers();
		for (Controller controller : controllers) {
			controller.getCar().deleteObservers();
		}
	}
}
