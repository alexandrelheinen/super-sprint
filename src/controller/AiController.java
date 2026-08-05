package controller;

import java.awt.event.KeyEvent;

import model.Car;
import model.Circuit;
import view.GameFrame;

public class AiController extends Controller {

	private static final double AI_PROPORTIONAL_GAIN_FACTOR = 0.0007;
	private static final double AI_DERIVATIVE_GAIN_FACTOR = -30.0;
	private static final double AI_MAX_TURN_RATE_FACTOR = 0.002;
	private static final double AI_INITIAL_GRID_ROW_OFFSET = 1;
	private static final double ANGLE_HALF_TURN = Math.PI / 2;
	private static final double ANGLE_FULL_TURN = Math.PI;
	private static final double ANGLE_ZERO = 0;
	private static final double ANGLE_TWO_PI = 2 * Math.PI;

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
		previousGridCell[0] += AI_INITIAL_GRID_ROW_OFFSET;
		targetHeading = ANGLE_ZERO;
		previousHeadingError = 0;
	}

	private double normalizeAngle(double angle) {
		while (angle <= -Math.PI) {
			angle += ANGLE_TWO_PI;
		}
		while (angle > Math.PI) {
			angle -= ANGLE_TWO_PI;
		}
		return angle;
	}

	private void applyControlOutput() {
		double proportionalGain = AI_PROPORTIONAL_GAIN_FACTOR * car.getStat(Car.STAT_HANDLING_INDEX);
		double derivativeGain = AI_DERIVATIVE_GAIN_FACTOR / car.getStat(Car.STAT_HANDLING_INDEX);
		double currentHeading = car.getAngle();
		double headingError = normalizeAngle(targetHeading - currentHeading);
		double controlOutput = proportionalGain * headingError + derivativeGain * (headingError - previousHeadingError);
		double maxTurnRate = AI_MAX_TURN_RATE_FACTOR * car.getStat(Car.STAT_HANDLING_INDEX);
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
		targetHeading = ANGLE_ZERO;
		switch (circuit.getTileType(car)) {
			case Circuit.TILE_STRAIGHT_HORIZONTAL:
				if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = -ANGLE_HALF_TURN;
				} else if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = ANGLE_HALF_TURN;
				}
				break;
			case Circuit.TILE_STRAIGHT_VERTICAL:
				if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = ANGLE_ZERO;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = ANGLE_FULL_TURN;
				}
				break;
			case Circuit.TILE_CORNER_BOTTOM_RIGHT:
				if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = ANGLE_HALF_TURN;
				} else if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = ANGLE_FULL_TURN;
				}
				break;
			case Circuit.TILE_CORNER_TOP_RIGHT:
				if (previousGridCell[0] == currentGridCell[0] + 1) {
					targetHeading = ANGLE_ZERO;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = ANGLE_HALF_TURN;
				}
				break;
			case Circuit.TILE_CORNER_TOP_LEFT:
				if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = ANGLE_ZERO;
				} else if (previousGridCell[1] == currentGridCell[1] + 1) {
					targetHeading = -ANGLE_HALF_TURN + ANGLE_TWO_PI;
				}
				break;
			case Circuit.TILE_CORNER_BOTTOM_LEFT:
				if (previousGridCell[0] == currentGridCell[0] - 1) {
					targetHeading = ANGLE_FULL_TURN;
				} else if (previousGridCell[1] == currentGridCell[1] - 1) {
					targetHeading = -ANGLE_HALF_TURN;
				}
				break;
			case Circuit.TILE_OPEN:
			default:
				break;
		}
	}
}
