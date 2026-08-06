package model;

/**
 * Single entry point for the race physics step: Dubins integration, infinite-mass
 * wall contacts, and finite-mass rectangle–rectangle car collisions.
 *
 * <p>No third-party physics engine is used. Libraries such as dyn4j or JBox2D
 * could simulate rigid bodies, but they fight the shared non-holonomic Dubins
 * plant used by human and AI drivers. Impulse resolution on oriented boxes keeps
 * that plant intact while giving mass-aware bumps and solid walls.
 */
public final class PhysicsSimulator {

	private static final int CAR_COLLISION_ITERATIONS = 4;
	private static final double RESTITUTION = 0.18;
	private static final double FRICTION = 0.12;
	private static final double POSITION_CORRECTION_PERCENT = 0.85;
	private static final double POSITION_SLOP_METERS = WorldUnits.pxToM(0.35);
	private static final double MIN_SEPARATION_METERS = WorldUnits.pxToM(0.5);
	private static final double WALL_RESTITUTION = 0.08;
	/** Extra asphalt recovery after a wall hit (keeps AI CTE inside the lane). */
	private static final double WALL_EXTRA_PUSH_METERS = WorldUnits.pxToM(2.0);
	private static final double WALL_SPEED_DAMPING = 0.35;

	private PhysicsSimulator() {
	}

	/**
	 * Integrates every car, then resolves wall and car–car contacts.
	 *
	 * @param cars          race cars for this tick (non-null entries)
	 * @param circuit       track used for wall contacts
	 * @param deltaSeconds  simulation step
	 */
	public static void simulateStep(Car[] cars, Circuit circuit, double deltaSeconds) {
		if (cars == null || cars.length == 0) {
			return;
		}
		for (Car car : cars) {
			if (car != null) {
				car.applyPhysics(deltaSeconds);
			}
		}
		for (Car car : cars) {
			if (car != null) {
				resolveWallCollision(car, circuit);
			}
		}
		for (int iteration = 0; iteration < CAR_COLLISION_ITERATIONS; iteration++) {
			for (int index = 0; index < cars.length; index++) {
				Car car = cars[index];
				if (car == null) {
					continue;
				}
				for (int otherIndex = 0; otherIndex < index; otherIndex++) {
					Car other = cars[otherIndex];
					if (other != null) {
						resolveCarCollision(car, other);
					}
				}
			}
		}
		// Second wall pass: car–car separation can shove a car back off asphalt.
		for (Car car : cars) {
			if (car != null) {
				resolveWallCollision(car, circuit);
			}
		}
	}

	/**
	 * Infinite-mass wall response: push the car back onto asphalt and remove the
	 * velocity component driving further into the boundary.
	 */
	public static void resolveWallCollision(Car car, Circuit circuit) {
		Circuit.WallContact contact = circuit.findWallContact(car);
		if (contact == null) {
			return;
		}

		double correction = Math.max(contact.penetrationMeters(), MIN_SEPARATION_METERS)
				+ WALL_EXTRA_PUSH_METERS;
		car.translateByMeters(
				contact.normalX() * correction,
				contact.normalY() * correction);

		double headingX = Math.cos(car.getAngle());
		double headingY = Math.sin(car.getAngle());
		double velocityX = car.getSpeed() * headingX;
		double velocityY = car.getSpeed() * headingY;
		double intoWall = velocityX * contact.normalX() + velocityY * contact.normalY();
		// Normal points onto the track; negative intoWall means driving into the wall.
		if (intoWall < 0.0) {
			double scale = -(1.0 + WALL_RESTITUTION) * intoWall;
			velocityX += scale * contact.normalX();
			velocityY += scale * contact.normalY();
		}
		double tangentialSpeed = velocityX * headingX + velocityY * headingY;
		// Arcade wall slap: dump most forward speed so recovery stays in-lane.
		car.setSpeed((float) (tangentialSpeed * WALL_SPEED_DAMPING));
	}

	/**
	 * Finite-mass OBB collision between two Dubins cars treated as moving rectangles.
	 */
	public static void resolveCarCollision(Car carA, Car carB) {
		OrientedBox boxA = OrientedBox.fromCar(carA);
		OrientedBox boxB = OrientedBox.fromCar(carB);
		OrientedBox.Contact contact = OrientedBox.collide(boxA, boxB);
		if (contact == null) {
			return;
		}

		double massA = Math.max(carA.getMassKg(), 1.0);
		double massB = Math.max(carB.getMassKg(), 1.0);
		double invMassA = 1.0 / massA;
		double invMassB = 1.0 / massB;
		double invMassSum = invMassA + invMassB;

		double normalX = contact.normalX();
		double normalY = contact.normalY();
		double penetration = contact.penetrationMeters();

		double correction = Math.max(penetration - POSITION_SLOP_METERS, 0.0)
				* POSITION_CORRECTION_PERCENT
				/ invMassSum;
		if (correction < MIN_SEPARATION_METERS * 0.5 && penetration > 0.0) {
			correction = MIN_SEPARATION_METERS / invMassSum;
		}
		carA.translateByMeters(-normalX * correction * invMassA, -normalY * correction * invMassA);
		carB.translateByMeters(normalX * correction * invMassB, normalY * correction * invMassB);

		double headingAX = Math.cos(carA.getAngle());
		double headingAY = Math.sin(carA.getAngle());
		double headingBX = Math.cos(carB.getAngle());
		double headingBY = Math.sin(carB.getAngle());
		double velocityAX = carA.getSpeed() * headingAX;
		double velocityAY = carA.getSpeed() * headingAY;
		double velocityBX = carB.getSpeed() * headingBX;
		double velocityBY = carB.getSpeed() * headingBY;

		double relativeX = velocityAX - velocityBX;
		double relativeY = velocityAY - velocityBY;
		double separating = relativeX * normalX + relativeY * normalY;
		if (separating > 0.0) {
			return;
		}

		double impulse = -(1.0 + RESTITUTION) * separating / invMassSum;
		double impulseX = impulse * normalX;
		double impulseY = impulse * normalY;

		double tangentX = relativeX - separating * normalX;
		double tangentY = relativeY - separating * normalY;
		double tangentLength = Math.hypot(tangentX, tangentY);
		if (tangentLength > 1e-9) {
			tangentX /= tangentLength;
			tangentY /= tangentLength;
			double tangentVelocity = relativeX * tangentX + relativeY * tangentY;
			double frictionImpulse = -tangentVelocity / invMassSum;
			double maxFriction = Math.abs(impulse) * FRICTION;
			frictionImpulse = clamp(frictionImpulse, -maxFriction, maxFriction);
			impulseX += frictionImpulse * tangentX;
			impulseY += frictionImpulse * tangentY;
		}

		velocityAX += impulseX * invMassA;
		velocityAY += impulseY * invMassA;
		velocityBX -= impulseX * invMassB;
		velocityBY -= impulseY * invMassB;

		// Project world impulses back onto the Dubins speed scalar.
		carA.setSpeed((float) (velocityAX * headingAX + velocityAY * headingAY));
		carB.setSpeed((float) (velocityBX * headingBX + velocityBY * headingBY));

		// Mild heading exchange for off-axis bumps (arcade readability).
		double deltaAngle = carA.getAngle() - carB.getAngle();
		float angleNudge = (float) (0.012 * deltaAngle * (massB / (massA + massB)));
		carA.setAngle(carA.getAngle() - angleNudge);
		carB.setAngle((float) (carB.getAngle() + angleNudge * (massA / massB)));
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

	/**
	 * Axis-aligned extents in the car body frame, oriented by heading. Position
	 * is the sprite top-left anchor; the geometric center accounts for rotation
	 * about that anchor.
	 */
	static final class OrientedBox {
		private final double centerX;
		private final double centerY;
		private final double halfLength;
		private final double halfWidth;
		private final double angle;

		private OrientedBox(
				double centerX,
				double centerY,
				double halfLength,
				double halfWidth,
				double angle) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.halfLength = halfLength;
			this.halfWidth = halfWidth;
			this.angle = angle;
		}

		static OrientedBox fromCar(Car car) {
			double halfLength = WorldUnits.pxToM(car.getSpriteWidth() * 0.5);
			double halfWidth = WorldUnits.pxToM(car.getSpriteHeight() * 0.5);
			double angle = car.getAngle();
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			// Sprite anchor is top-left of the unrotated rectangle; rotate the
			// local center offset (halfLength, halfWidth) about that anchor.
			double centerX = car.getPositionXMeters() + halfLength * cos - halfWidth * sin;
			double centerY = car.getPositionYMeters() + halfLength * sin + halfWidth * cos;
			return new OrientedBox(centerX, centerY, halfLength, halfWidth, angle);
		}

		/**
		 * SAT contact with normal pointing from {@code a} toward {@code b}.
		 */
		static Contact collide(OrientedBox a, OrientedBox b) {
			double smallestPenetration = Double.POSITIVE_INFINITY;
			double bestNormalX = 0.0;
			double bestNormalY = 0.0;

			double[][] axes = {
					{Math.cos(a.angle), Math.sin(a.angle)},
					{-Math.sin(a.angle), Math.cos(a.angle)},
					{Math.cos(b.angle), Math.sin(b.angle)},
					{-Math.sin(b.angle), Math.cos(b.angle)}
			};

			for (double[] axis : axes) {
				double axisX = axis[0];
				double axisY = axis[1];
				double[] projectionA = a.project(axisX, axisY);
				double[] projectionB = b.project(axisX, axisY);
				double overlap = Math.min(projectionA[1], projectionB[1])
						- Math.max(projectionA[0], projectionB[0]);
				if (overlap <= 0.0) {
					return null;
				}
				if (overlap < smallestPenetration) {
					smallestPenetration = overlap;
					bestNormalX = axisX;
					bestNormalY = axisY;
				}
			}

			double centerDeltaX = b.centerX - a.centerX;
			double centerDeltaY = b.centerY - a.centerY;
			if (centerDeltaX * bestNormalX + centerDeltaY * bestNormalY < 0.0) {
				bestNormalX = -bestNormalX;
				bestNormalY = -bestNormalY;
			}
			return new Contact(bestNormalX, bestNormalY, smallestPenetration);
		}

		private double[] project(double axisX, double axisY) {
			double extent = halfLength * Math.abs(axisX * Math.cos(angle) + axisY * Math.sin(angle))
					+ halfWidth * Math.abs(axisX * -Math.sin(angle) + axisY * Math.cos(angle));
			double center = centerX * axisX + centerY * axisY;
			return new double[] {center - extent, center + extent};
		}

		record Contact(double normalX, double normalY, double penetrationMeters) {
		}
	}
}
