package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import model.Circuit;
import view.GameFrame;

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
		int keyCode = event.getKeyCode();
		if (playerNumber == PLAYER_ONE) {
			switch (keyCode) {
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_UP:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_LEFT:
					car.applySteeringInput(keyCode);
					break;
				default:
					break;
			}
		} else if (playerNumber == PLAYER_TWO) {
			switch (keyCode) {
				case KeyEvent.VK_S:
				case KeyEvent.VK_W:
				case KeyEvent.VK_D:
				case KeyEvent.VK_A:
					car.applySteeringInput(keyCode);
					break;
				default:
					break;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		int keyCode = event.getKeyCode();
		if (playerNumber == PLAYER_ONE) {
			switch (keyCode) {
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_UP:
					car.releaseAcceleration();
					break;
				default:
					break;
			}
		} else if (playerNumber == PLAYER_TWO) {
			switch (keyCode) {
				case KeyEvent.VK_S:
				case KeyEvent.VK_W:
					car.releaseAcceleration();
					break;
				default:
					break;
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent event) {
		// Not used.
	}
}
