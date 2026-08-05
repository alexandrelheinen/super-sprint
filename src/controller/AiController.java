package controller;

import java.awt.event.KeyEvent;

import model.Car;
import model.Circuit;
import view.GameFrame;

public class AiController extends Controller {

	private final Circuit circuit;
	private int[] previousGridCell;
	private int[] currentGridCell;
	private double targetHeading;
	private double previousHeadingError;

	public AiController(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		super(modelIndex, startPosition, frame, circuit);
		this.circuit = circuit;
		currentGridCell = circuit.getGridCoordinates(car);
		previousGridCell = currentGridCell.clone();
		previousGridCell[0] += 1;
		targetHeading = 0;
		previousHeadingError = 0;
	}

	private double normalizeAngle(double angle) {
		while (angle <= -Math.PI) {
			angle += 2 * Math.PI;
		}
		while (angle > Math.PI) {
			angle -= 2 * Math.PI;
		}
		return angle;
	}

	private void applyControlOutput() {
		double proportionalGain = 0.0007 * car.getStat(2);
		double derivativeGain = -30.0 / car.getStat(2);
		double currentHeading = car.getAngle();
		double headingError = normalizeAngle(targetHeading - currentHeading);
		double controlOutput = proportionalGain * headingError + derivativeGain * (headingError - previousHeadingError);
		double maxTurnRate = 0.002 * car.getStat(2);
		if (controlOutput > maxTurnRate) {
			controlOutput = maxTurnRate;
		}
		car.setAngle(car.getAngle() + (float) controlOutput);
		previousHeadingError = headingError;
	}

	@Override
	public void update() {
		updateTargetHeading();
		applyControlOutput();
		car.applySteeringInput(KeyEvent.VK_UP);
		super.update();
	}

	private void updateTargetHeading() {
		int[] gridCell = circuit.getGridCoordinates(car).clone();
		if (gridCell[0] != currentGridCell[0] || gridCell[1] != currentGridCell[1]) {
			previousGridCell = currentGridCell.clone();
			currentGridCell = gridCell;
		}
		targetHeading = 0;
		switch (circuit.getTileType(car)) {
			case 1:
				if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = -Math.PI / 2;
				} else if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = Math.PI / 2;
				}
				break;
			case 2:
				if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = 0;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = Math.PI;
				}
				break;
			case 3:
				if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = Math.PI / 2;
				} else if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = Math.PI;
				}
				break;
			case 4:
				if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = 0;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = Math.PI / 2;
				}
				break;
			case 5:
				if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = 0;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = -Math.PI / 2 + 2 * Math.PI;
				}
				break;
			case 6:
				if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = Math.PI;
				} else if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = -Math.PI / 2;
				}
				break;
			case 7:
			default:
				break;
		}
	}
}
