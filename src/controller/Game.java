package controller;

import java.util.Timer;

import javax.swing.JOptionPane;

import model.Circuit;
import model.HallOfFame;
import view.GameFrame;

public class Game {

	public static final int TICK_INTERVAL_MS = 10;

	private final HallOfFame hallOfFame;
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

	public Game(int[] carModels, int trackNumber, int humanPlayers, int laps, HallOfFame hallOfFame) {
		Controller.resetPlayerCount();
		this.trackIndex = trackNumber;
		trackMap = Game.TRACK_MAPS[trackNumber - 1];
		this.lapCount = laps;
		this.hallOfFame = hallOfFame;
		this.humanPlayerCount = humanPlayers;

		gameFrame = new GameFrame("Super Sprint Supelec", carModels, trackMap, trackNumber);
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

		gameTimer = new Timer();
		gameTimer.scheduleAtFixedRate(
				new GameTickTask(controllers, circuit, this),
				(long) 5 * TICK_INTERVAL_MS,
				TICK_INTERVAL_MS);
		running = true;
	}

	public void checkRaceFinished() {
		for (int index = 0; index < controllers.length; index++) {
			if (controllers[index].getCar().getLapCount() <= lapCount) {
				continue;
			}
			if (running) {
				finishGame(index);
			}
			return;
		}
	}

	private void finishGame(int winnerIndex) {
		running = false;
		gameTimer.cancel();
		gameTimer.purge();
		double raceTimeMs = circuit.getRaceTimeMs();
		if (winnerIndex < humanPlayerCount) {
			hallOfFame.tryAddResult(raceTimeMs, trackIndex - 1);
		} else {
			hallOfFame.tryAddResult(raceTimeMs * 1000.0, trackIndex - 1);
			JOptionPane.showMessageDialog(null, "The computer won.");
		}
		gameFrame.setVisible(false);
		gameFrame.dispose();
	}
}
