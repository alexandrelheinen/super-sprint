package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import model.DubinsVehicle;
import model.ReferencePath;
import model.TestPaths;

public class DubinsMpccPlannerTest {

	private static final double DELTA_SECONDS = 0.05;

	@Test
	public void emptyPathHoldsSpeed() {
		DubinsVehicle vehicle = vehicleAt(0.0, 0.0, 0.0, 12.0);
		DubinsMpccPlanner planner = planner(25.0);
		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, ReferencePath.empty(), List.of());
		assertEquals(10, plan.commands().length);
		assertEquals(12.0, plan.commands()[0].speedCommand(), 1e-9);
		assertEquals(0.0, plan.commands()[0].turnRateCommand(), 1e-9);
	}

	@Test
	public void obstacleOnPathRaisesAvoidanceCostRelativeToClearLane() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 15.0);
		DubinsMpccPlanner planner = planner(25.0);
		DynamicObstacle blocker = new DynamicObstacle(12.0, 0.0, 0.0, 0.0, 1.5);

		double[] speeds = fill(planner.getConfig().getHorizonStepCount(), 15.0);
		double[] turns = fill(planner.getConfig().getHorizonStepCount(), 0.0);
		double blockedCost = planner.evaluate(vehicle, path, List.of(blocker), speeds, turns);
		double clearCost = planner.evaluate(vehicle, path, List.of(), speeds, turns);
		assertTrue(
				blockedCost > clearCost + 1.0,
				"Expected obstacle soft constraint to increase cost: blocked="
						+ blockedCost + " clear=" + clearCost);
	}

	@Test
	public void plannerSteersAwayFromBlockingOpponent() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 16.0);
		DubinsMpccPlanner planner = planner(25.0);
		// Stationary car sitting on the racing line ahead.
		DynamicObstacle blocker = new DynamicObstacle(14.0, 0.0, 0.0, 0.0, 1.8);

		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, path, List.of(blocker));
		boolean steered = false;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			if (Math.abs(command.turnRateCommand()) > 0.15) {
				steered = true;
				break;
			}
		}
		assertTrue(steered, "Expected a non-trivial avoidance steer in the MPCC plan");

		// Roll out the plan and ensure the closest approach beats a straight PD hold.
		double plannedClearance = closestApproach(vehicle, plan, blocker);
		DubinsMpccPlanner.Plan straight = straightPlan(vehicle.getSpeed(), planner.getConfig().getHorizonStepCount());
		double straightClearance = closestApproach(vehicle, straight, blocker);
		assertTrue(
				plannedClearance > straightClearance + 0.15,
				"MPCC clearance " + plannedClearance
						+ " should beat straight drive clearance " + straightClearance);
	}

	@Test
	public void warmStartOnClearPathKeepsNearCruiseProgress() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 18.0);
		DubinsMpccPlanner planner = planner(25.0);
		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, path, List.of());
		double averageSpeed = 0.0;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			averageSpeed += command.speedCommand();
		}
		averageSpeed /= plan.commands().length;
		assertTrue(averageSpeed > 15.0, "Clear-road plan should stay near cruise, got " + averageSpeed);
	}

	private static DubinsMpccPlanner planner(double cruiseSpeed) {
		PdPathFollowController pd = new PdPathFollowController(4.0, 1.0, 2.5, 2.4, 0.8, cruiseSpeed, 0.45);
		return new DubinsMpccPlanner(MpccConfig.DEFAULT, pd);
	}

	private static DubinsVehicle vehicleAt(double x, double y, double heading, double speed) {
		DubinsVehicle vehicle = new DubinsVehicle(x, y, heading, 30.0, 0.0, 8.0, 16.0, 40.0);
		vehicle.syncPose(x, y, heading, speed);
		return vehicle;
	}

	private static double[] fill(int count, double value) {
		double[] values = new double[count];
		for (int index = 0; index < count; index++) {
			values[index] = value;
		}
		return values;
	}

	private static DubinsMpccPlanner.Plan straightPlan(double speed, int horizon) {
		DubinsMpccPlanner.Command[] commands = new DubinsMpccPlanner.Command[horizon];
		for (int index = 0; index < horizon; index++) {
			commands[index] = new DubinsMpccPlanner.Command(speed, 0.0);
		}
		return new DubinsMpccPlanner.Plan(commands, 0.0);
	}

	private static double closestApproach(
			DubinsVehicle start,
			DubinsMpccPlanner.Plan plan,
			DynamicObstacle obstacle) {
		DubinsVehicle rollout = start.copy();
		double closest = Double.POSITIVE_INFINITY;
		double time = 0.0;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			rollout.step(command.speedCommand(), command.turnRateCommand(), DELTA_SECONDS);
			time += DELTA_SECONDS;
			double distance = Math.hypot(
					rollout.getX() - obstacle.predictedX(time),
					rollout.getY() - obstacle.predictedY(time));
			closest = Math.min(closest, distance);
		}
		return closest;
	}
}
