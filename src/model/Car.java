package model;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.Observable;

import view.GameFrame;

public class Car extends Observable {

	public static final int[][] CAR_MODEL_STATS = {
			{120, 280, 50},
			{200, 320, 38},
			{146, 374, 40},
			{170, 250, 55}
	};
	public static final int CAR_MODEL_COUNT = 4;

	private static final int[] SPRITE_DIMENSIONS = {38, 22};
	private static final double COLLISION_BLEND = 0.1;

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
		stats = CAR_MODEL_STATS[modelIndex - 1];
		try {
			position = Circuit.START_POSITIONS[frame.getTrackNumber() - 1][ranking - 1].clone();
		} catch (RuntimeException exception) {
			System.err.println(exception.toString());
			System.err.println("Error: the maximum number of cars is 4.");
			throw exception;
		}
		angle = (float) (-Math.PI / 2);
		speed = 0;
		acceleration = 0;
		addObserver(frame);
		motionState = 0;
		spriteIndex = ranking - 1;
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
		double turnRate = 0.002;

		switch (keyCode) {
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				if (speed != 0.0) {
					angle -= stats[2] * turnRate;
				}
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				if (speed != 0.0) {
					angle += stats[2] * turnRate;
				}
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				acceleration = stats[0];
				motionState = 1;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				acceleration = -stats[0];
				motionState = 1;
				break;
			default:
				break;
		}
		setChanged();
	}

	public void releaseAcceleration() {
		acceleration = 0;
		motionState = 2;
	}

	public void applyPhysics(double deltaSeconds) {
		float stopThreshold = 11.0f;

		if (motionState == 2) {
			if (speed < stopThreshold && speed > -stopThreshold) {
				acceleration = 0;
				speed = 0;
				motionState = 0;
			} else {
				acceleration = (float) (-0.5 * stats[0] * Math.signum(speed));
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
			float angleAdjustment = (float) (0.01 * deltaAngle);
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
