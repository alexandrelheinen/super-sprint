package model;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.Observable;

import view.GameFrame;

public class Car extends Observable {

	public static final int CAR_MODEL_COUNT = GameConfig.CAR_MODEL_NAMES.length;
	public static final int MAX_CARS = GameConfig.MAX_CARS;
	public static final int STAT_COUNT = 3;
	public static final int STAT_ACCELERATION_INDEX = 0;
	public static final int STAT_MAX_SPEED_INDEX = 1;
	public static final int STAT_HANDLING_INDEX = 2;

	/**
	 * Car model stats in SI units: acceleration (m/s²), max speed (m/s), handling index.
	 * Loaded from {@code cars.properties}.
	 */
	public static final double[][] CAR_MODEL_STATS = GameConfig.CAR_MODEL_STATS;

	/**
	 * Trimmed car sprite size in pixels (width, height) per model index.
	 * Loaded from {@code cars.properties} after sprite preparation.
	 */
	public static final int[][] CAR_MODEL_SPRITE_DIMENSIONS = GameConfig.CAR_MODEL_SPRITE_DIMENSIONS;

	private static final double COLLISION_BLEND = 0.1;
	private static final double INITIAL_ANGLE = -Math.PI / 2;
	private static final double TURN_RATE = 0.002;
	private static final double STOP_THRESHOLD_MS = 1.1;
	private static final double DECELERATION_FACTOR = 0.5;
	private static final double COLLISION_ANGLE_FACTOR = 0.01;
	private static final double COLLISION_NUDGE_METERS = WorldUnits.pxToM(1.0);
	private static final int MOTION_STATE_IDLE = 0;
	private static final int MOTION_STATE_ACCELERATING = 1;
	private static final int MOTION_STATE_DECELERATING = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final String MAX_CARS_ERROR = "Error: the maximum number of cars is " + MAX_CARS + ".";

	private final double[] stats;
	private final double[] positionMeters;
	private double angle;
	private double speedMs;
	private double accelerationMs2;
	private final Circuit circuit;
	private int motionState;
	private final int spriteIndex;
	private final int modelIndex;
	private final String name;
	private int lapCount;
	private boolean finishLineCrossed;

	public Car(
			int modelIndex,
			int ranking,
			String name,
			GameFrame frame,
			int playerNumber,
			Circuit circuit) {
		super();
		this.circuit = circuit;
		this.name = name;
		this.modelIndex = modelIndex;
		stats = CAR_MODEL_STATS[modelIndex].clone();
		try {
			float[] startPositionPixels = Circuit.START_POSITIONS[frame.getTrackNumber()][ranking
					- ONE_BASED_INDEX_OFFSET].clone();
			positionMeters = new double[] {
					WorldUnits.pxToM(startPositionPixels[0]),
					WorldUnits.pxToM(startPositionPixels[1])
			};
		} catch (RuntimeException exception) {
			System.err.println(exception.toString());
			System.err.println(MAX_CARS_ERROR);
			throw exception;
		}
		angle = (float) INITIAL_ANGLE;
		speedMs = 0;
		accelerationMs2 = 0;
		addObserver(frame);
		motionState = MOTION_STATE_IDLE;
		spriteIndex = ranking - ONE_BASED_INDEX_OFFSET;
		lapCount = 0;
		finishLineCrossed = false;
	}

	public int getX() {
		return WorldUnits.mToPxRounded(positionMeters[0]);
	}

	public int getY() {
		return WorldUnits.mToPxRounded(positionMeters[1]);
	}

	public double getPositionXMeters() {
		return positionMeters[0];
	}

	public double getPositionYMeters() {
		return positionMeters[1];
	}

	public float getAngle() {
		return (float) angle;
	}

	public int getSpriteIndex() {
		return spriteIndex;
	}

	public int getModelIndex() {
		return modelIndex;
	}

	public int getSpriteWidth() {
		return CAR_MODEL_SPRITE_DIMENSIONS[modelIndex][0];
	}

	public int getSpriteHeight() {
		return CAR_MODEL_SPRITE_DIMENSIONS[modelIndex][1];
	}

	/** Forward speed in m/s. */
	public float getSpeed() {
		return (float) speedMs;
	}

	public String getName() {
		return name;
	}

	public boolean hasCrossedFinishLine() {
		return finishLineCrossed;
	}

	public int getLapCount() {
		return lapCount;
	}

	public double getStat(int index) {
		return stats[index];
	}

	public static double getModelStat(int modelIndex, int statIndex) {
		return CAR_MODEL_STATS[modelIndex][statIndex];
	}

	public void setSpeed(float speedMs) {
		this.speedMs = speedMs;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	/**
	 * Applies an externally integrated pose (e.g. Dubins tracker) and updates lap detection.
	 */
	public void applyKinematicState(double xMeters, double yMeters, float angle, float speedMs) {
		positionMeters[0] = xMeters;
		positionMeters[1] = yMeters;
		this.angle = angle;
		this.speedMs = speedMs;
		accelerationMs2 = 0.0;
		motionState = MOTION_STATE_IDLE;
		lapCount += circuit.crossFinishLine(this);
		setChanged();
		notifyRenderObservers();
	}

	public void translateByMeters(double deltaXMeters, double deltaYMeters) {
		positionMeters[0] += deltaXMeters;
		positionMeters[1] += deltaYMeters;
	}

	public void toggleFinishLineFlag() {
		finishLineCrossed = !finishLineCrossed;
	}

	public void applySteeringInput(int keyCode) {
		switch (keyCode) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				if (speedMs != 0.0) {
					angle -= stats[STAT_HANDLING_INDEX] * TURN_RATE;
				}
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				if (speedMs != 0.0) {
					angle += stats[STAT_HANDLING_INDEX] * TURN_RATE;
				}
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				accelerationMs2 = stats[STAT_ACCELERATION_INDEX];
				motionState = MOTION_STATE_ACCELERATING;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				accelerationMs2 = -stats[STAT_ACCELERATION_INDEX];
				motionState = MOTION_STATE_ACCELERATING;
				break;
			default:
				break;
		}
		setChanged();
	}

	public void releaseAcceleration() {
		accelerationMs2 = 0;
		motionState = MOTION_STATE_DECELERATING;
	}

	public void applyPhysics(double deltaSeconds) {
		if (motionState == MOTION_STATE_DECELERATING) {
			if (speedMs < STOP_THRESHOLD_MS && speedMs > -STOP_THRESHOLD_MS) {
				accelerationMs2 = 0;
				speedMs = 0;
				motionState = MOTION_STATE_IDLE;
			} else {
				accelerationMs2 = -DECELERATION_FACTOR * stats[STAT_ACCELERATION_INDEX] * Math.signum(speedMs);
			}
		}

		if (Math.abs(speedMs) >= stats[STAT_MAX_SPEED_INDEX]) {
			speedMs = Math.signum(speedMs) * stats[STAT_MAX_SPEED_INDEX];
		}

		positionMeters[0] += speedMs * Math.cos(angle) * deltaSeconds;
		positionMeters[1] += speedMs * Math.sin(angle) * deltaSeconds;
		speedMs += accelerationMs2 * deltaSeconds;
		lapCount += circuit.crossFinishLine(this);

		setChanged();
		notifyRenderObservers();
	}

	/**
	 * Notifies observers only on render ticks so the 100 Hz physics loop does
	 * not trigger a repaint on every simulation step.
	 */
	private void notifyRenderObservers() {
		if (circuit.isRenderTick()) {
			notifyObservers();
		}
	}

	public void collideWith(Car otherCar) {
		Shape thisShape = new Rectangle(getX(), getY(), getSpriteWidth(), getSpriteHeight());
		Shape otherShape = new Rectangle(
				otherCar.getX(),
				otherCar.getY(),
				otherCar.getSpriteWidth(),
				otherCar.getSpriteHeight());

		AffineTransform thisTransform = new AffineTransform();
		AffineTransform otherTransform = new AffineTransform();

		thisTransform.rotate(getAngle(), getX(), getY());
		otherTransform.rotate(otherCar.getAngle(), otherCar.getX(), otherCar.getY());

		thisShape = thisTransform.createTransformedShape(thisShape);
		otherShape = otherTransform.createTransformedShape(otherShape);

		if (thisShape.intersects(otherShape.getBounds2D()) || otherShape.intersects(thisShape.getBounds2D())) {
			float deltaAngle = getAngle() - otherCar.getAngle();
			float angleAdjustment = (float) (COLLISION_ANGLE_FACTOR * deltaAngle);
			angle -= angleAdjustment;
			otherCar.setAngle(otherCar.getAngle() + angleAdjustment);
			double previousSpeed = speedMs;
			speedMs = (1 - COLLISION_BLEND) * speedMs + Math.cos(deltaAngle) * COLLISION_BLEND * otherCar.getSpeed();
			otherCar.setSpeed(
					(float) ((1 - COLLISION_BLEND) * otherCar.getSpeed()
							+ Math.cos(deltaAngle) * COLLISION_BLEND * previousSpeed));
			double deltaX = Math.signum(positionMeters[0] - otherCar.getPositionXMeters()) * COLLISION_NUDGE_METERS;
			double deltaY = Math.signum(positionMeters[1] - otherCar.getPositionYMeters()) * COLLISION_NUDGE_METERS;
			translateByMeters(deltaX, deltaY);
			otherCar.translateByMeters(-deltaX, -deltaY);
		}
	}
}
