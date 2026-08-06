package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DynamicObstacleTest {

	@Test
	public void predictsConstantVelocityCentre() {
		DynamicObstacle obstacle = new DynamicObstacle(1.0, 2.0, 0.0, 10.0, 1.5);
		assertEquals(1.0, obstacle.predictedX(0.0), 1e-9);
		assertEquals(2.0, obstacle.predictedY(0.0), 1e-9);
		assertEquals(3.0, obstacle.predictedX(0.2), 1e-9);
		assertEquals(2.0, obstacle.predictedY(0.2), 1e-9);
	}
}
