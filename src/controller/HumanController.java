package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import model.Circuit;
import view.GameFrame;

/**
 * Human driver: held arcade keys map to the same {@link model.Car} controls the
 * AI uses (accelerate / brake / steer left / steer right).
 */
public class HumanController extends Controller implements KeyListener {

	private static final int PLAYER_ONE = 1;
	private static final int PLAYER_TWO = 2;

	private final int playerNumber;

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
	public void keyPressed(KeyEvent event) {
		if (!frame.isRacingInputEnabled()) {
			return;
		}
		applyKey(event.getKeyCode(), true);
	}

	@Override
	public void keyReleased(KeyEvent event) {
		if (!frame.isRacingInputEnabled()) {
			return;
		}
		applyKey(event.getKeyCode(), false);
	}

	@Override
	public void keyTyped(KeyEvent event) {
		// Not used.
	}

	private void applyKey(int keyCode, boolean pressed) {
		if (playerNumber == PLAYER_ONE) {
			switch (keyCode) {
				case KeyEvent.VK_UP:
					car.setAccelerating(pressed);
					break;
				case KeyEvent.VK_DOWN:
					car.setBraking(pressed);
					break;
				case KeyEvent.VK_LEFT:
					car.setSteeringLeft(pressed);
					break;
				case KeyEvent.VK_RIGHT:
					car.setSteeringRight(pressed);
					break;
				default:
					break;
			}
		} else if (playerNumber == PLAYER_TWO) {
			switch (keyCode) {
				case KeyEvent.VK_W:
					car.setAccelerating(pressed);
					break;
				case KeyEvent.VK_S:
					car.setBraking(pressed);
					break;
				case KeyEvent.VK_A:
					car.setSteeringLeft(pressed);
					break;
				case KeyEvent.VK_D:
					car.setSteeringRight(pressed);
					break;
				default:
					break;
			}
		}
	}
}
