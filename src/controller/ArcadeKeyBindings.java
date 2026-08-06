package controller;

import java.awt.event.KeyEvent;

/**
 * Arcade key map for one human driver. Built once at construction so the
 * controller never branches on player number while handling input.
 */
public final class ArcadeKeyBindings {

	private final int accelerateKey;
	private final int brakeKey;
	private final int steerLeftKey;
	private final int steerRightKey;

	private ArcadeKeyBindings(
			int accelerateKey,
			int brakeKey,
			int steerLeftKey,
			int steerRightKey) {
		this.accelerateKey = accelerateKey;
		this.brakeKey = brakeKey;
		this.steerLeftKey = steerLeftKey;
		this.steerRightKey = steerRightKey;
	}

	/** Arrow keys (player 1). */
	public static ArcadeKeyBindings arrows() {
		return new ArcadeKeyBindings(
				KeyEvent.VK_UP,
				KeyEvent.VK_DOWN,
				KeyEvent.VK_LEFT,
				KeyEvent.VK_RIGHT);
	}

	/** WASD (player 2). */
	public static ArcadeKeyBindings wasd() {
		return new ArcadeKeyBindings(
				KeyEvent.VK_W,
				KeyEvent.VK_S,
				KeyEvent.VK_A,
				KeyEvent.VK_D);
	}

	/**
	 * Resolves the binding set for a 1-based human seat. Player number is only
	 * meaningful here — callers should store the returned bindings, not the seat.
	 */
	public static ArcadeKeyBindings forHumanSeat(int humanSeatNumber) {
		if (humanSeatNumber == 1) {
			return arrows();
		}
		if (humanSeatNumber == 2) {
			return wasd();
		}
		throw new IllegalArgumentException(
				"Unsupported human seat: " + humanSeatNumber + " (expected 1 or 2)");
	}

	public int accelerateKey() {
		return accelerateKey;
	}

	public int brakeKey() {
		return brakeKey;
	}

	public int steerLeftKey() {
		return steerLeftKey;
	}

	public int steerRightKey() {
		return steerRightKey;
	}
}
