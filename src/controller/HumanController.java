package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import model.Circuit;
import view.GameFrame;

/**
 * Human driver: tracks physically held keys and maps them onto the same
 * {@link model.Car} arcade controls the AI uses.
 *
 * <p>Keys are recorded on every press/release even during the countdown. Car
 * controls are refreshed every tick once racing input is enabled, so holding a
 * key through "GO" applies immediately instead of waiting for OS key-repeat.
 * {@link #keyTyped} is intentionally unused — arrow keys do not produce typed
 * events.
 */
public class HumanController extends Controller implements KeyListener {

	private static final int PLAYER_ONE = 1;
	private static final int PLAYER_TWO = 2;

	private final int playerNumber;
	private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();

	public HumanController(
			int modelIndex,
			int startPosition,
			int playerNumber,
			GameFrame frame,
			Circuit circuit) {
		super(modelIndex, startPosition, frame, circuit);
		this.playerNumber = playerNumber;
		frame.addKeyListener(this);
	}

	@Override
	public void update() {
		if (frame.isRacingInputEnabled()) {
			syncCarFromPressedKeys();
		} else {
			car.clearControls();
		}
		super.update();
	}

	@Override
	public void keyPressed(KeyEvent event) {
		pressedKeys.add(event.getKeyCode());
		if (frame.isRacingInputEnabled()) {
			syncCarFromPressedKeys();
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		pressedKeys.remove(event.getKeyCode());
		if (frame.isRacingInputEnabled()) {
			syncCarFromPressedKeys();
		}
	}

	@Override
	public void keyTyped(KeyEvent event) {
		// Arrow keys and many game keys never produce typed events.
	}

	/**
	 * Applies the currently held keys to the car. Package-visible for tests.
	 */
	void syncCarFromPressedKeys() {
		if (playerNumber == PLAYER_ONE) {
			car.setAccelerating(pressedKeys.contains(KeyEvent.VK_UP));
			car.setBraking(pressedKeys.contains(KeyEvent.VK_DOWN));
			car.setSteeringLeft(pressedKeys.contains(KeyEvent.VK_LEFT));
			car.setSteeringRight(pressedKeys.contains(KeyEvent.VK_RIGHT));
			return;
		}
		if (playerNumber == PLAYER_TWO) {
			car.setAccelerating(pressedKeys.contains(KeyEvent.VK_W));
			car.setBraking(pressedKeys.contains(KeyEvent.VK_S));
			car.setSteeringLeft(pressedKeys.contains(KeyEvent.VK_A));
			car.setSteeringRight(pressedKeys.contains(KeyEvent.VK_D));
		}
	}

	/** Test helper: simulate a physical key going down without racing enabled. */
	void pressKeyForTest(int keyCode) {
		pressedKeys.add(keyCode);
	}

	/** Test helper: simulate a physical key going up. */
	void releaseKeyForTest(int keyCode) {
		pressedKeys.remove(keyCode);
	}
}
