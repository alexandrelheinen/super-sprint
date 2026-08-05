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
	public static final int STAT_HANDLING_INDEX = 2;

	public static final int[][] CAR_MODEL_STATS = {
			{120, 280, 50},
			{200, 320, 38},
			{146, 374, 40},
			{170, 250, 55}
	};

	private static final int[] SPRITE_DIMENSIONS = {38, 22};
	private static final double COLLISION_BLEND = 0.1;
	private static final double INITIAL_ANGLE = -Math.PI / 2;
	private static final double TURN_RATE = 0.002;
	private static final float STOP_THRESHOLD = 11.0f;
	private static final double DECELERATION_FACTOR = 0.5;
	private static final double COLLISION_ANGLE_FACTOR = 0.01;
	private static final int MOTION_STATE_IDLE = 0;
	private static final int MOTION_STATE_ACCELERATING = 1;
	private static final int MOTION_STATE_DECELERATING = 2;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final String MAX_CARS_ERROR = "Error: the maximum number of cars is " + MAX_CARS + ".";

	private final int[] stats;
	private final float[] position;
	private float angle;
	private float speed;
	private float acceleration;
	private final Circuit circuit;
	private int motionState;
	private final int spriteIndex;
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
		stats = CAR_MODEL_STATS[modelIndex - ONE_BASED_INDEX_OFFSET];
		try {
			position = Circuit.START_POSITIONS[frame.getTrackNumber() - ONE_BASED_INDEX_OFFSET][ranking - ONE_BASED_INDEX_OFFSET]
					.clone();
		} catch (RuntimeException exception) {
			System.err.println(exception.toString());
			System.err.println(MAX_CARS_ERROR);
			throw exception;
		}
		angle = (float) INITIAL_ANGLE;
		speed = 0;
		acceleration = 0;
		addObserver(frame);
		motionState = MOTION_STATE_IDLE;
		spriteIndex = ranking - ONE_BASED_INDEX_OFFSET;
		lapCount = 0;
		finishLineCrossed = false;
	}

	public int getX() {
		return Math.round(position[0]);
	}

	public int getY() {
		return Math.round(position[1]);
	}

	public float getAngle() {
		return angle;
	}

	public int getSpriteIndex() {
		return spriteIndex;
	}

	public float getSpeed() {
		return speed;
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

	public int getStat(int index) {
		return stats[index];
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public void translateBy(float deltaX, float deltaY) {
		position[0] += deltaX;
		position[1] += deltaY;
	}

	public void toggleFinishLineFlag() {
		finishLineCrossed = !finishLineCrossed;
	}

	public void applySteeringInput(int keyCode) {
		switch (keyCode) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				if (speed != 0.0) {
					angle -= stats[STAT_HANDLING_INDEX] * TURN_RATE;
				}
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				if (speed != 0.0) {
					angle += stats[STAT_HANDLING_INDEX] * TURN_RATE;
				}
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				acceleration = stats[0];
				motionState = MOTION_STATE_ACCELERATING;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				acceleration = -stats[0];
				motionState = MOTION_STATE_ACCELERATING;
				break;
			default:
				break;
		}
		setChanged();
	}

	public void releaseAcceleration() {
		acceleration = 0;
		motionState = MOTION_STATE_DECELERATING;
	}

	public void applyPhysics(double deltaSeconds) {
		if (motionState == MOTION_STATE_DECELERATING) {
			if (speed < STOP_THRESHOLD && speed > -STOP_THRESHOLD) {
				acceleration = 0;
				speed = 0;
				motionState = MOTION_STATE_IDLE;
			} else {
				acceleration = (float) (-DECELERATION_FACTOR * stats[0] * Math.signum(speed));
			}
		}

		if (Math.abs(speed) >= stats[1]) {
			speed = Math.signum(speed) * stats[1];
		}

		position[0] += speed * Math.cos(angle) * deltaSeconds;
		position[1] += speed * Math.sin(angle) * deltaSeconds;
		speed += acceleration * deltaSeconds;
		lapCount += circuit.crossFinishLine(this);

		setChanged();
		notifyObservers();
	}

	public void collideWith(Car otherCar) {
		Shape thisShape = new Rectangle(getX(), getY(), SPRITE_DIMENSIONS[0], SPRITE_DIMENSIONS[1]);
		Shape otherShape = new Rectangle(otherCar.getX(), otherCar.getY(), SPRITE_DIMENSIONS[0], SPRITE_DIMENSIONS[1]);

		AffineTransform thisTransform = new AffineTransform();
		AffineTransform otherTransform = new AffineTransform();

		thisTransform.rotate(getAngle(), getX(), getY());
		otherTransform.rotate(otherCar.getAngle(), otherCar.getX(), otherCar.getY());

		thisShape = thisTransform.createTransformedShape(thisShape);
		otherShape = otherTransform.createTransformedShape(otherShape);

		if (thisShape.intersects(otherShape.getBounds2D()) || otherShape.intersects(thisShape.getBounds2D())) {
			float deltaAngle = angle - otherCar.getAngle();
			float angleAdjustment = (float) (COLLISION_ANGLE_FACTOR * deltaAngle);
			angle -= angleAdjustment;
			otherCar.setAngle(otherCar.getAngle() + angleAdjustment);
			float previousSpeed = speed;
			speed = (float) ((1 - COLLISION_BLEND) * speed + Math.cos(deltaAngle) * COLLISION_BLEND * otherCar.getSpeed());
			otherCar.setSpeed(
					(float) ((1 - COLLISION_BLEND) * otherCar.getSpeed() + Math.cos(deltaAngle) * COLLISION_BLEND * previousSpeed));
			int deltaX = (int) Math.signum(getX() - otherCar.getX());
			int deltaY = (int) Math.signum(getY() - otherCar.getY());
			translateBy(deltaX, deltaY);
			otherCar.translateBy(-deltaX, -deltaY);
		}
	}
}
