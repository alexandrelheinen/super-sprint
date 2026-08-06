package controller;

/**
 * Predicted opponent used as a soft constraint in the short-horizon MPCC.
 */
public final class DynamicObstacle {

	private final double x;
	private final double y;
	private final double heading;
	private final double speed;
	private final double radiusMeters;

	public DynamicObstacle(
			double x,
			double y,
			double heading,
			double speed,
			double radiusMeters) {
		this.x = x;
		this.y = y;
		this.heading = heading;
		this.speed = speed;
		this.radiusMeters = radiusMeters;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getHeading() {
		return heading;
	}

	public double getSpeed() {
		return speed;
	}

	public double getRadiusMeters() {
		return radiusMeters;
	}

	/** Constant-velocity prediction of the obstacle centre. */
	public double predictedX(double timeSeconds) {
		return x + speed * Math.cos(heading) * timeSeconds;
	}

	/** Constant-velocity prediction of the obstacle centre. */
	public double predictedY(double timeSeconds) {
		return y + speed * Math.sin(heading) * timeSeconds;
	}
}
