package model;

/**
 * Dubins-like unicycle kinematic model with bounded dynamics.
 *
 * <p>State is {@code (x, y, heading)} in world coordinates. Controls are
 * {@code (speed, turnRate)} saturated and rate-limited before integration,
 * following the ARCO {@code DubinsVehicle} design.
 */
public class DubinsVehicle {

	private double x;
	private double y;
	private double heading;
	private final double maxSpeed;
	private final double minSpeed;
	private final double maxTurnRate;
	private final double maxAcceleration;
	private final double maxTurnRateDot;

	private double speed;
	private double turnRate;

	public DubinsVehicle(
			double x,
			double y,
			double heading,
			double maxSpeed,
			double minSpeed,
			double maxTurnRate,
			double maxAcceleration,
			double maxTurnRateDot) {
		this.x = x;
		this.y = y;
		this.heading = heading;
		this.maxSpeed = maxSpeed;
		this.minSpeed = minSpeed;
		this.maxTurnRate = maxTurnRate;
		this.maxAcceleration = maxAcceleration;
		this.maxTurnRateDot = maxTurnRateDot;
		this.speed = 0.0;
		this.turnRate = 0.0;
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

	public void syncPose(double x, double y, double heading, double speed) {
		this.x = x;
		this.y = y;
		this.heading = heading;
		this.speed = speed;
	}

	/**
	 * Rate-limit a turn-rate command without integrating the full pose.
	 *
	 * @return filtered turn rate in rad/s
	 */
	public double filterTurnRate(double turnRateCommand, double deltaSeconds) {
		double maxDeltaTurn = maxTurnRateDot * deltaSeconds;
		double deltaTurn = clamp(turnRateCommand - turnRate, -maxDeltaTurn, maxDeltaTurn);
		turnRate = clamp(turnRate + deltaTurn, -maxTurnRate, maxTurnRate);
		return turnRate;
	}

	/**
	 * Integrate one step with command saturation and filtering.
	 */
	public void step(double speedCommand, double turnRateCommand, double deltaSeconds) {
		double maxDeltaSpeed = maxAcceleration * deltaSeconds;
		double deltaSpeed = clamp(speedCommand - speed, -maxDeltaSpeed, maxDeltaSpeed);
		speed = clamp(speed + deltaSpeed, minSpeed, maxSpeed);

		filterTurnRate(turnRateCommand, deltaSeconds);

		x += speed * Math.cos(heading) * deltaSeconds;
		y += speed * Math.sin(heading) * deltaSeconds;
		heading = wrapAngle(heading + turnRate * deltaSeconds);
	}

	public static double wrapAngle(double angle) {
		return Math.atan2(Math.sin(angle), Math.cos(angle));
	}

	private static double clamp(double value, double min, double max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}
}
