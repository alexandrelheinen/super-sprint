package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DubinsVehicleTest {

	private static final double EPSILON = 1e-9;

	private static DubinsVehicle vehicle(double heading) {
		return new DubinsVehicle(0.0, 0.0, heading, 30.0, 0.0, 5.0, 10.0, 40.0);
	}

	@Test
	public void wrapAngleKeepsAnglesInMinusPiToPi() {
		assertEquals(0.0, DubinsVehicle.wrapAngle(2.0 * Math.PI), EPSILON);
		assertEquals(Math.PI / 2.0, DubinsVehicle.wrapAngle(Math.PI / 2.0), EPSILON);
		assertEquals(-Math.PI / 2.0, DubinsVehicle.wrapAngle(3.0 * Math.PI / 2.0), EPSILON);
		assertEquals(Math.PI, Math.abs(DubinsVehicle.wrapAngle(3.0 * Math.PI)), EPSILON);
	}

	@Test
	public void speedIsRateLimitedByMaxAcceleration() {
		DubinsVehicle vehicle = vehicle(0.0);
		vehicle.step(30.0, 0.0, 0.1);
		// One step can add at most maxAcceleration * dt = 1.0 m/s.
		assertEquals(1.0, vehicle.getSpeed(), EPSILON);
	}

	@Test
	public void speedSaturatesAtMaxSpeed() {
		DubinsVehicle vehicle = vehicle(0.0);
		for (int step = 0; step < 500; step++) {
			vehicle.step(1000.0, 0.0, 0.1);
		}
		assertEquals(30.0, vehicle.getSpeed(), EPSILON);
	}

	@Test
	public void speedNeverDropsBelowMinSpeed() {
		DubinsVehicle vehicle = vehicle(0.0);
		for (int step = 0; step < 100; step++) {
			vehicle.step(-1000.0, 0.0, 0.1);
		}
		assertEquals(0.0, vehicle.getSpeed(), EPSILON);
	}

	@Test
	public void turnRateIsRateLimitedAndSaturated() {
		DubinsVehicle vehicle = vehicle(0.0);
		double filtered = vehicle.filterTurnRate(100.0, 0.01);
		// One step can add at most maxTurnRateDot * dt = 0.4 rad/s.
		assertEquals(0.4, filtered, EPSILON);
		for (int step = 0; step < 100; step++) {
			filtered = vehicle.filterTurnRate(100.0, 0.01);
		}
		assertEquals(5.0, filtered, EPSILON);
	}

	@Test
	public void integratesStraightMotionAlongHeading() {
		DubinsVehicle vehicle = vehicle(0.0);
		for (int step = 0; step < 100; step++) {
			vehicle.step(10.0, 0.0, 0.01);
		}
		assertTrue(vehicle.getX() > 0.0);
		assertEquals(0.0, vehicle.getY(), EPSILON);
		assertEquals(0.0, vehicle.getHeading(), EPSILON);
	}

	@Test
	public void syncPoseOverridesState() {
		DubinsVehicle vehicle = vehicle(0.0);
		vehicle.syncPose(3.0, 4.0, 1.0, 12.0);
		assertEquals(3.0, vehicle.getX(), EPSILON);
		assertEquals(4.0, vehicle.getY(), EPSILON);
		assertEquals(1.0, vehicle.getHeading(), EPSILON);
		assertEquals(12.0, vehicle.getSpeed(), EPSILON);
	}
}
