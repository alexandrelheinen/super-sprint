package model;

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

	/** Non-transparent race-sprite pixel counts per model (mass proxy). */
	public static final int[] CAR_MODEL_OPAQUE_PIXELS = GameConfig.CAR_MODEL_OPAQUE_PIXELS;

	/** Curb weight in kg per model: opaque pixels × {@link #KILOGRAMS_PER_OPAQUE_PIXEL}. */
	public static final double[] CAR_MODEL_MASS_KG = GameConfig.CAR_MODEL_MASS_KG;

	/**
	 * Mass scale from the green reference car ({@code 500 kg / opaque pixel count}).
	 * Used for UI display and relative collision impulses.
	 */
	public static final double KILOGRAMS_PER_OPAQUE_PIXEL = GameConfig.KILOGRAMS_PER_OPAQUE_PIXEL;

	/**
	 * Yaw rate scale shared by human and AI arcade controls: max turn rate is
	 * {@code handling * TURN_RATE_PER_HANDLING} rad/s while moving.
	 * Retuned for human-drivable steering (was 0.2 ≈ 5°/tick at handling 44).
	 */
	public static final double TURN_RATE_PER_HANDLING = 0.075;

	private static final double INITIAL_ANGLE = -Math.PI / 2;
	private static final double STOP_THRESHOLD_MS = 1.1;
	private static final double DECELERATION_FACTOR = 0.5;
	private static final int MOTION_STATE_IDLE = 0;
	private static final int MOTION_STATE_ACCELERATING = 1;
	private static final int MOTION_STATE_DECELERATING = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final String MAX_CARS_ERROR = "Error: the maximum number of cars is " + MAX_CARS + ".";

	private final double[] stats;
	private final double[] positionMeters;
	private final double massKg;
	private double angle;
	private double speedMs;
	private double accelerationMs2;
	private final Circuit circuit;
	private int motionState;
	private final int spriteIndex;
	private final int modelIndex;
	private final String name;
	private int lapCount;
	/**
	 * Which side of the finish line the car last occupied: {@code +1} start/grid
	 * side, {@code -1} far side. Used so lap counting requires a real crossing
	 * instead of mere proximity to the line.
	 */
	private int finishLineSide;

	private boolean accelerating;
	private boolean braking;
	private boolean steeringLeft;
	private boolean steeringRight;

	public Car(
			int modelIndex,
			int ranking,
			String name,
			GameFrame frame,
			Circuit circuit) {
		super();
		this.circuit = circuit;
		this.name = name;
		this.modelIndex = modelIndex;
		stats = CAR_MODEL_STATS[modelIndex].clone();
		massKg = CAR_MODEL_MASS_KG[modelIndex];
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
		// Every grid slot sits on the start side of the finish line.
		finishLineSide = 1;
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

	public int getFinishLineSide() {
		return finishLineSide;
	}

	public void setFinishLineSide(int finishLineSide) {
		this.finishLineSide = finishLineSide;
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

	public static int getModelOpaquePixels(int modelIndex) {
		return CAR_MODEL_OPAQUE_PIXELS[modelIndex];
	}

	public static double getModelMassKg(int modelIndex) {
		return CAR_MODEL_MASS_KG[modelIndex];
	}

	/** Non-transparent race-sprite pixel count for this car's model. */
	public int getOpaquePixels() {
		return CAR_MODEL_OPAQUE_PIXELS[modelIndex];
	}

	/** Curb weight in kilograms derived from opaque pixels × kg/pixel. */
	public double getMassKg() {
		return massKg;
	}

	/** Max yaw rate for this car's handling, in rad/s. */
	public double getMaxTurnRate() {
		return stats[STAT_HANDLING_INDEX] * TURN_RATE_PER_HANDLING;
	}

	public void setSpeed(float speedMs) {
		this.speedMs = speedMs;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public boolean isAccelerating() {
		return accelerating;
	}

	public boolean isBraking() {
		return braking;
	}

	public boolean isSteeringLeft() {
		return steeringLeft;
	}

	public boolean isSteeringRight() {
		return steeringRight;
	}

	/**
	 * Applies an externally integrated pose (e.g. tests) and updates lap detection.
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

	public void startAccelerating() {
		accelerating = true;
		braking = false;
		refreshThrottleState();
		setChanged();
	}

	public void stopAccelerating() {
		accelerating = false;
		refreshThrottleState();
		setChanged();
	}

	public void startBraking() {
		braking = true;
		accelerating = false;
		refreshThrottleState();
		setChanged();
	}

	public void stopBraking() {
		braking = false;
		refreshThrottleState();
		setChanged();
	}

	public void startSteeringLeft() {
		steeringLeft = true;
		setChanged();
	}

	public void stopSteeringLeft() {
		steeringLeft = false;
		setChanged();
	}

	public void startSteeringRight() {
		steeringRight = true;
		setChanged();
	}

	public void stopSteeringRight() {
		steeringRight = false;
		setChanged();
	}

	/** Clears all held arcade controls and coasts. */
	public void clearControls() {
		accelerating = false;
		braking = false;
		steeringLeft = false;
		steeringRight = false;
		releaseAcceleration();
	}

	public void releaseAcceleration() {
		accelerating = false;
		braking = false;
		accelerationMs2 = 0;
		motionState = MOTION_STATE_DECELERATING;
		setChanged();
	}

	public void applyPhysics(double deltaSeconds) {
		refreshThrottleState();

		int steerDirection = 0;
		if (steeringLeft && !steeringRight) {
			steerDirection = -1;
		} else if (steeringRight && !steeringLeft) {
			steerDirection = 1;
		}
		// Same rule for human and AI: no yaw authority while stopped.
		if (steerDirection != 0 && speedMs != 0.0) {
			angle += steerDirection * getMaxTurnRate() * deltaSeconds;
		}

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

	private void refreshThrottleState() {
		if (accelerating) {
			accelerationMs2 = stats[STAT_ACCELERATION_INDEX];
			motionState = MOTION_STATE_ACCELERATING;
		} else if (braking) {
			accelerationMs2 = -stats[STAT_ACCELERATION_INDEX];
			motionState = MOTION_STATE_ACCELERATING;
		} else if (motionState != MOTION_STATE_DECELERATING && motionState != MOTION_STATE_IDLE) {
			accelerationMs2 = 0;
			motionState = MOTION_STATE_DECELERATING;
		}
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

}
