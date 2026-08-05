package controller;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import view.GameFrame;

public abstract class Controller {

	private static final String AI_PLAYER_LABEL = "C";
	private static final String LOG_CAR_CREATED = "Car created: ";
	private static final String LOG_START_POSITION = "; start position ";
	private static final String LOG_SEPARATOR = " ------------- ";
	private static final double MS_PER_SECOND = 1000.0;

	private static int playerCount = 0;
	protected Car car;
	protected GameFrame frame;

	public Controller(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		playerCount++;
		String name = (this instanceof HumanController)
				? Integer.toString(playerCount)
				: AI_PLAYER_LABEL;
		car = new Car(modelIndex, startPosition, name, frame, playerCount, circuit);
		System.out.println(
				LOG_CAR_CREATED
						+ GameCatalog.carModelName(modelIndex)
						+ LOG_START_POSITION
						+ startPosition);
		System.out.println(this.getClass());
		System.out.println(LOG_SEPARATOR);
		this.frame = frame;
	}

	public Car getCar() {
		return car;
	}

	public static int getPlayerCount() {
		return playerCount;
	}

	public static void resetPlayerCount() {
		playerCount = 0;
	}

	public void update() {
		car.applyPhysics(Game.TICK_INTERVAL_MS / MS_PER_SECOND);
	}
}
