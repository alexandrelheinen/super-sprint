package controller;

import java.util.TimerTask;

import model.Circuit;

public class GameTickTask extends TimerTask {

	private static final String LOG_RACE_STARTED = "Race started with ";
	private static final String LOG_CARS_SUFFIX = " cars.";
	private static final String LOG_SEPARATOR = " ------------- ";

	private final Controller[] controllers;
	private final Circuit circuit;
	private final Game game;

	public GameTickTask(Controller[] controllers, Circuit circuit, Game game) {
		this.controllers = controllers;
		this.circuit = circuit;
		this.game = game;
		System.out.println(LOG_RACE_STARTED + controllers.length + LOG_CARS_SUFFIX);
		System.out.println(LOG_SEPARATOR);
	}

	@Override
	public void run() {
		if (!game.isRunning()) {
			return;
		}
		circuit.tick();
		game.checkRaceFinished();
		for (int index = 0; index < controllers.length; index++) {
			circuit.enforceTrackBoundaries(controllers[index].getCar());
			controllers[index].update();
			for (int otherIndex = 0; otherIndex < index; otherIndex++) {
				controllers[index].getCar().collideWith(controllers[otherIndex].getCar());
			}
		}
	}
}
