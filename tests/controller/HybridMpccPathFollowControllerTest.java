package controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import model.DubinsVehicle;
import model.ReferencePath;
import model.TestPaths;

public class HybridMpccPathFollowControllerTest {

	private static final double DELTA_SECONDS = 0.01;

	@Test
	public void usesPdWhenNoOpponentsAreNearby() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		DubinsVehicle vehicle = vehicleAt(10.0, 0.0, 0.0, 12.0);
		HybridMpccPathFollowController hybrid = hybrid(vehicle);
		hybrid.setObstacles(List.of());

		double[] commands = hybrid.track(10.0, 0.0, 0.0, 12.0, path, DELTA_SECONDS);
		assertFalse(hybrid.wasLastCommandFromMpcc());
		assertTrue(commands[0] > 0.0);
	}

	@Test
	public void switchesToMpccWhenOpponentIsInsideTriggerDistance() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(8.0, 0.0, 0.0, 14.0);
		HybridMpccPathFollowController hybrid = hybrid(vehicle);
		hybrid.setObstacles(List.of(new DynamicObstacle(12.0, 0.0, 0.0, 0.0, 1.5)));

		hybrid.track(8.0, 0.0, 0.0, 14.0, path, DELTA_SECONDS);
		assertTrue(
				hybrid.wasLastCommandFromMpcc(),
				"Nearby opponent should trigger the sparse MPCC planner");
	}

	@Test
	public void fallsBackToPdAfterThreatLeaves() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(8.0, 0.0, 0.0, 14.0);
		HybridMpccPathFollowController hybrid = hybrid(vehicle);
		hybrid.setObstacles(List.of(new DynamicObstacle(12.0, 0.0, 0.0, 0.0, 1.5)));

		// Consume a full MPCC open-loop plan.
		for (int step = 0; step < 80; step++) {
			hybrid.track(8.0 + step * 0.05, 0.0, 0.0, 14.0, path, DELTA_SECONDS);
		}

		hybrid.setObstacles(List.of());
		// With no threat and an exhausted plan, the next command must be PD.
		for (int step = 0; step < 5; step++) {
			hybrid.track(20.0, 0.0, 0.0, 14.0, path, DELTA_SECONDS);
		}
		assertFalse(hybrid.wasLastCommandFromMpcc());
	}

	@Test
	public void switchesToMpccWhenNearWallEvenWithoutOpponents() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		double nearWallY = MpccConfig.DEFAULT.getLaneHalfWidthMeters()
				- MpccConfig.DEFAULT.getEgoRadiusMeters()
				- 0.5;
		DubinsVehicle vehicle = vehicleAt(8.0, nearWallY, 0.0, 14.0);
		HybridMpccPathFollowController hybrid = hybrid(vehicle);
		hybrid.setObstacles(List.of());

		hybrid.track(8.0, nearWallY, 0.0, 14.0, path, DELTA_SECONDS);
		assertTrue(
				hybrid.wasLastCommandFromMpcc(),
				"Proximity to a wall should trigger MPCC even without opponents");
	}

	private static HybridMpccPathFollowController hybrid(DubinsVehicle vehicle) {
		PdPathFollowController pd = new PdPathFollowController(4.0, 1.0, 2.5, 2.4, 0.8, 25.0, 0.45);
		return new HybridMpccPathFollowController(pd, vehicle, MpccConfig.DEFAULT);
	}

	private static DubinsVehicle vehicleAt(double x, double y, double heading, double speed) {
		DubinsVehicle vehicle = new DubinsVehicle(x, y, heading, 30.0, 0.0, 8.0, 16.0, 40.0);
		vehicle.syncFullState(x, y, heading, speed, 0.0);
		return vehicle;
	}
}
