package controller;

import java.util.TimerTask;

import model.Circuit;
import view.GameFrame;

public class GameTickTask extends TimerTask {

	private static final String LOG_RACE_STARTED = "Race started with ";
	private static final String LOG_CARS_SUFFIX = " cars.";
	private static final String LOG_SEPARATOR = " ------------- ";

	private final Controller[] controllers;
	private final Circuit circuit;
	private final Game game;
	private boolean raceStartLogged;

	public GameTickTask(Controller[] controllers, Circuit circuit, Game game) {
		this.controllers = controllers;
		this.circuit = circuit;
		this.game = game;
		raceStartLogged = false;
	}

	@Override
	public void run() {
		if (!game.isRunning()) {
			return;
		}

		if (game.isFinishing()) {
			game.tickFinishSequence();
			return;
		}

		if (!game.isRacing()) {
			tickCountdown();
			return;
		}

		if (!raceStartLogged) {
			System.out.println(LOG_RACE_STARTED + controllers.length + LOG_CARS_SUFFIX);
			System.out.println(LOG_SEPARATOR);
			raceStartLogged = true;
		}

		circuit.tick();
		for (int index = 0; index < controllers.length; index++) {
			if (controllers[index] instanceof AiController aiController) {
				aiController.updateOpponents(controllers);
			}
		}
		for (int index = 0; index < controllers.length; index++) {
			circuit.enforceTrackBoundaries(controllers[index].getCar());
			controllers[index].update();
			for (int otherIndex = 0; otherIndex < index; otherIndex++) {
				controllers[index].getCar().collideWith(controllers[otherIndex].getCar());
			}
		}
		// Evaluate after movement so a car that completes the final lap this
		// tick ends the race immediately, and same-tick ties use the highest
		// lap counter rather than controller order from the previous step.
		game.checkRaceFinished();
	}

	private void tickCountdown() {
		RaceCountdown countdown = game.getCountdown();
		if (countdown == null) {
			game.beginRacing();
			return;
		}

		boolean stillCounting = countdown.advance(Game.TICK_INTERVAL_MS);
		GameFrame frame = game.getGameFrame();
		frame.setCountdownPresentation(countdown.label(), countdown.progress(), countdown.isGoStep());

		// Repaint at the same cadence as the race loop so the pop/fade animates.
		if (circuit.shouldRenderAfterVisualTick()) {
			frame.renderCompositeFrame(circuit);
		}

		if (!stillCounting) {
			game.beginRacing();
		}
	}
}
