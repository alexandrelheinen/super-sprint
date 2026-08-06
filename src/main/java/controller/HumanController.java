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
 * <p>Which keys belong to this seat is fixed at construction via
 * {@link ArcadeKeyBindings}. Press/release are always recorded (even during
 * countdown); car controls sync every racing tick so a key held through GO
 * applies immediately instead of waiting for OS key-repeat.
 */
public class HumanController extends Controller implements KeyListener {

	private final ArcadeKeyBindings keyBindings;
	private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();

	public HumanController(
			int modelIndex,
			int startPosition,
			int humanSeatNumber,
			GameFrame frame,
			Circuit circuit) {
		this(modelIndex, startPosition, ArcadeKeyBindings.forHumanSeat(humanSeatNumber), frame, circuit);
	}

	/** Construction with an explicit key map (tests / custom seats). */
	HumanController(
			int modelIndex,
			int startPosition,
			ArcadeKeyBindings keyBindings,
			GameFrame frame,
			Circuit circuit) {
		super(modelIndex, startPosition, frame, circuit);
		this.keyBindings = keyBindings;
		frame.addKeyListener(this);
	}

	@Override
	protected String driverLabelForSeat(int seatNumber) {
		return Integer.toString(seatNumber);
	}

	@Override
	public void update() {
		if (frame.isRacingInputEnabled()) {
			syncCarFromPressedKeys();
		} else {
			car.clearControls();
		}
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
	 * Applies the currently held keys to the car. Same path for every human
	 * seat — only {@link #keyBindings} differs.
	 */
	void syncCarFromPressedKeys() {
		applyHeldKey(keyBindings.accelerateKey(), car::startAccelerating, car::stopAccelerating);
		applyHeldKey(keyBindings.brakeKey(), car::startBraking, car::stopBraking);
		applyHeldKey(keyBindings.steerLeftKey(), car::startSteeringLeft, car::stopSteeringLeft);
		applyHeldKey(keyBindings.steerRightKey(), car::startSteeringRight, car::stopSteeringRight);
	}

	private void applyHeldKey(int keyCode, Runnable whenPressed, Runnable whenReleased) {
		if (pressedKeys.contains(keyCode)) {
			whenPressed.run();
		} else {
			whenReleased.run();
		}
	}

	ArcadeKeyBindings keyBindings() {
		return keyBindings;
	}

	/** Test helper: simulate a physical key going down. */
	void pressKeyForTest(int keyCode) {
		pressedKeys.add(keyCode);
	}

	/** Test helper: simulate a physical key going up. */
	void releaseKeyForTest(int keyCode) {
		pressedKeys.remove(keyCode);
	}
}
