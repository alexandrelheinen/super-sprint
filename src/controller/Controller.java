package controller;

import model.Car;
import model.Circuit;
import view.GameFrame;

public abstract class Controller {

	private static int playerCount = 0;
	protected Car car;
	protected GameFrame frame;

	public Controller(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		playerCount++;
		String name = (this instanceof HumanController)
				? Integer.toString(playerCount)
				: "C";
		car = new Car(modelIndex, startPosition, name, frame, playerCount, circuit);
		System.out.println("Car created: model " + modelIndex + "; start position " + startPosition);
		System.out.println(this.getClass());
		System.out.println(" ------------- ");
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
		car.applyPhysics(Game.TICK_INTERVAL_MS / 1000.0);
	}
}
