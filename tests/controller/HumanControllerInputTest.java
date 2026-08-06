package controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.Test;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import view.GameFrame;

/**
 * Human input must track held keys through the countdown and apply them on the
 * first racing tick — never wait for OS key-repeat.
 */
public class HumanControllerInputTest {

	@Test
	public void heldKeyThroughCountdownAppliesImmediatelyWhenRacingStarts() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		HumanController human = new HumanController(0, 1, 1, frame, circuit);
		Car car = human.getCar();

		frame.setRacingInputEnabled(false);
		human.pressKeyForTest(KeyEvent.VK_UP);
		human.pressKeyForTest(KeyEvent.VK_LEFT);
		human.update();
		assertFalse(car.isAccelerating(), "Countdown must keep the car idle");
		assertFalse(car.isSteeringLeft(), "Countdown must keep the car idle");

		frame.setRacingInputEnabled(true);
		human.update();
		assertTrue(car.isAccelerating(), "Held accelerate must apply on first racing tick");
		assertTrue(car.isSteeringLeft(), "Held steer must apply on first racing tick");
	}

	@Test
	public void wasdSeatUsesStoredBindingsWithoutPlayerNumberBranch() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		HumanController human = new HumanController(0, 1, ArcadeKeyBindings.wasd(), frame, circuit);
		frame.setRacingInputEnabled(true);
		human.pressKeyForTest(KeyEvent.VK_W);
		human.pressKeyForTest(KeyEvent.VK_A);
		human.update();
		assertTrue(human.getCar().isAccelerating());
		assertTrue(human.getCar().isSteeringLeft());
	}

	@Test
	public void keyReleaseClearsControlEvenIfTrackedDuringCountdown() {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(new int[] {0}, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		HumanController human = new HumanController(0, 1, 1, frame, circuit);
		Car car = human.getCar();

		frame.setRacingInputEnabled(false);
		human.pressKeyForTest(KeyEvent.VK_RIGHT);
		human.releaseKeyForTest(KeyEvent.VK_RIGHT);
		frame.setRacingInputEnabled(true);
		human.update();
		assertFalse(car.isSteeringRight());
	}
}
