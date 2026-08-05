package controller;

import java.util.Timer;

import javax.swing.SwingUtilities;

import model.Car;
import model.Circuit;
import model.ConfigLoader;
import model.GameCatalog;
import model.GameConfig;
import model.HallOfFame;
import model.ReferencePath;
import model.TrackGeometry;
import view.GameFrame;
import view.MenuFrame;

public class Game {

	public static final int TICK_INTERVAL_MS = 10;
	public static final int TIMER_START_DELAY_TICKS = 5;
	public static final int MS_PER_SECOND = 1000;
	public static final int ONE_BASED_INDEX_OFFSET = 1;

	private static final String GAME_TITLE_PREFIX = GameConfig.GAME_TITLE + " — ";
	private static final String MSG_COMPUTER_WON = ConfigLoader.getMessage(
			"messages.game.computer.won",
			"The computer won.");
	private static final double AI_HALL_OF_FAME_TIME_MULTIPLIER = 1000.0;

	public static final int[][][] TRACK_MAPS = {
			{
					{Circuit.TILE_CORNER_TOP_RIGHT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_RIGHT},
					{Circuit.TILE_STRAIGHT_HORIZONTAL, Circuit.TILE_OPEN, Circuit.TILE_OPEN,
							Circuit.TILE_STRAIGHT_HORIZONTAL},
					{Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_LEFT}
			},
			{
					{Circuit.TILE_CORNER_TOP_RIGHT, Circuit.TILE_CORNER_BOTTOM_RIGHT, Circuit.TILE_OPEN,
							Circuit.TILE_OPEN},
					{Circuit.TILE_STRAIGHT_HORIZONTAL, Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_RIGHT},
					{Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_LEFT}
			},
			{
					{Circuit.TILE_CORNER_TOP_RIGHT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_RIGHT},
					{Circuit.TILE_STRAIGHT_HORIZONTAL, Circuit.TILE_OPEN, Circuit.TILE_CORNER_TOP_RIGHT,
							Circuit.TILE_CORNER_BOTTOM_LEFT},
					{Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_CORNER_BOTTOM_LEFT,
							Circuit.TILE_OPEN}
			},
			{
					{Circuit.TILE_CORNER_TOP_RIGHT, Circuit.TILE_CORNER_BOTTOM_RIGHT, Circuit.TILE_CORNER_TOP_RIGHT,
							Circuit.TILE_CORNER_BOTTOM_RIGHT},
					{Circuit.TILE_STRAIGHT_HORIZONTAL, Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_CORNER_BOTTOM_LEFT,
							Circuit.TILE_STRAIGHT_HORIZONTAL},
					{Circuit.TILE_CORNER_TOP_LEFT, Circuit.TILE_STRAIGHT_VERTICAL, Circuit.TILE_STRAIGHT_VERTICAL,
							Circuit.TILE_CORNER_BOTTOM_LEFT}
			}
	};

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

	public Game(
			int[] carModels,
			int trackNumber,
			int humanPlayers,
			int laps,
			HallOfFame hallOfFame,
			MenuFrame menuFrame) {
		Controller.resetPlayerCount();
		GameCatalog.validateLapCount(laps);
		this.trackIndex = trackNumber;
		trackMap = Game.TRACK_MAPS[trackNumber - ONE_BASED_INDEX_OFFSET];
		this.lapCount = laps;
		this.hallOfFame = hallOfFame;
		this.menuFrame = menuFrame;
		this.humanPlayerCount = humanPlayers;

		gameFrame = new GameFrame(
				GAME_TITLE_PREFIX + GameCatalog.trackName(trackNumber),
				carModels,
				trackMap,
				trackNumber);
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
		int track = trackIndex - ONE_BASED_INDEX_OFFSET;
		boolean humanWon = winnerIndex < humanPlayerCount;

		SwingUtilities.invokeLater(() -> {
			detachRenderObservers();
			gameFrame.shutdown();
			if (humanWon) {
				hallOfFame.tryAddResult(raceTimeMs, track);
			} else {
				hallOfFame.tryAddResult(raceTimeMs * AI_HALL_OF_FAME_TIME_MULTIPLIER, track);
				javax.swing.JOptionPane.showMessageDialog(menuFrame, MSG_COMPUTER_WON);
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
