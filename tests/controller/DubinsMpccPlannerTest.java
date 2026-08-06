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
	public void plannerAttemptsBypassAroundBlockingOpponent() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 16.0);
		DubinsMpccPlanner planner = planner(25.0);
		// Stationary car sitting on the racing line ahead.
		DynamicObstacle blocker = new DynamicObstacle(14.0, 0.0, 0.0, 0.0, 1.8);

		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, path, List.of(blocker));
		boolean steered = false;
		double averageSpeed = 0.0;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			averageSpeed += command.speedCommand();
			if (Math.abs(command.turnRateCommand()) > 0.15) {
				steered = true;
			}
		}
		averageSpeed /= plan.commands().length;
		assertTrue(steered, "Expected a lateral bypass attempt in the MPCC plan");
		assertTrue(
				averageSpeed > 8.0,
				"Bypass should keep rolling; opponent cost is not a hard no-go (avg speed "
						+ averageSpeed + ")");

		// Opponent proximity may get closer than a timid straight hold — that is
		// intentional risk for overtaking — but the plan must stay in-lane.
		DubinsVehicle rollout = vehicle.copy();
		double maxAbsCrossTrack = 0.0;
		int hint = ReferencePath.NO_HINT;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			rollout.step(command.speedCommand(), command.turnRateCommand(), DELTA_SECONDS);
			ReferencePath.Projection projection = path.project(
					rollout.getX(),
					rollout.getY(),
					hint);
			hint = projection.closestIndex();
			maxAbsCrossTrack = Math.max(maxAbsCrossTrack, Math.abs(projection.crossTrackError()));
		}
		double maxAllowed = MpccConfig.DEFAULT.getLaneHalfWidthMeters()
				- MpccConfig.DEFAULT.getEgoRadiusMeters();
		assertTrue(
				maxAbsCrossTrack < maxAllowed,
				"Bypass left the lane: |cte|=" + maxAbsCrossTrack + " limit=" + maxAllowed);
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

	@Test
	public void wallWeightStrictlyExceedsOpponentWeight() {
		assertTrue(
				MpccConfig.DEFAULT.getWeightWall() > MpccConfig.DEFAULT.getWeightObstacle(),
				"Wall collisions must be treated as more critical than car collisions");
	}

	@Test
	public void wallSoftConstraintCostsMoreThanComparableCarSoftConstraint() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		// Place the ego car near the wall margin so wallCost is active.
		double nearWallY = MpccConfig.DEFAULT.getLaneHalfWidthMeters()
				- MpccConfig.DEFAULT.getEgoRadiusMeters()
				- MpccConfig.DEFAULT.getWallSafeMarginMeters()
				+ 1.0;
		DubinsVehicle nearWall = vehicleAt(5.0, nearWallY, 0.0, 14.0);
		DubinsVehicle onCenter = vehicleAt(5.0, 0.0, 0.0, 14.0);
		DubinsMpccPlanner planner = planner(25.0);
		double[] speeds = fill(planner.getConfig().getHorizonStepCount(), 14.0);
		double[] turns = fill(planner.getConfig().getHorizonStepCount(), 0.0);

		double wallCost = planner.evaluate(nearWall, path, List.of(), speeds, turns);
		DynamicObstacle blocker = new DynamicObstacle(
				5.0 + 1.0,
				0.0,
				0.0,
				0.0,
				MpccConfig.DEFAULT.getEgoRadiusMeters());
		double carCost = planner.evaluate(onCenter, path, List.of(blocker), speeds, turns);
		assertTrue(
				wallCost > carCost,
				"Near-wall cost " + wallCost + " should exceed comparable car proximity cost " + carCost);
	}

	@Test
	public void avoidanceKeepsPredictedTrajectoryInsideLane() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 16.0);
		DubinsMpccPlanner planner = planner(25.0);
		DynamicObstacle blocker = new DynamicObstacle(14.0, 0.0, 0.0, 0.0, 1.8);
		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, path, List.of(blocker));

		DubinsVehicle rollout = vehicle.copy();
		double maxAbsCrossTrack = 0.0;
		int hint = ReferencePath.NO_HINT;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			rollout.step(command.speedCommand(), command.turnRateCommand(), DELTA_SECONDS);
			ReferencePath.Projection projection = path.project(
					rollout.getX(),
					rollout.getY(),
					hint);
			hint = projection.closestIndex();
			maxAbsCrossTrack = Math.max(maxAbsCrossTrack, Math.abs(projection.crossTrackError()));
		}
		double maxAllowed = MpccConfig.DEFAULT.getLaneHalfWidthMeters()
				- MpccConfig.DEFAULT.getEgoRadiusMeters();
		assertTrue(
				maxAbsCrossTrack < maxAllowed,
				"Avoidance left the lane: |cte|=" + maxAbsCrossTrack + " limit=" + maxAllowed);
	}

	@Test
	public void keepsMovingToAttemptPassInsteadOfFullStop() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 16.0);
		DubinsMpccPlanner planner = planner(25.0);
		DynamicObstacle blocker = new DynamicObstacle(14.0, 0.0, 0.0, 0.0, 1.8);
		DubinsMpccPlanner.Plan plan = planner.plan(vehicle, path, List.of(blocker));

		double averageSpeed = 0.0;
		for (DubinsMpccPlanner.Command command : plan.commands()) {
			averageSpeed += command.speedCommand();
		}
		averageSpeed /= plan.commands().length;
		assertTrue(
				averageSpeed > 8.0,
				"Opponent cost must stay soft enough to keep moving for a pass, got avg speed "
						+ averageSpeed);
	}

	@Test
	public void progressPastTrafficBeatsFullStopEvenWithCloserApproach() {
		ReferencePath path = TestPaths.straightEast(240, 0.5);
		DubinsVehicle vehicle = vehicleAt(5.0, 0.0, 0.0, 16.0);
		DubinsMpccPlanner planner = planner(25.0);
		DynamicObstacle blocker = new DynamicObstacle(12.0, 0.0, 0.0, 0.0, 1.5);
		int horizon = planner.getConfig().getHorizonStepCount();

		double[] cruiseSpeeds = fill(horizon, 16.0);
		double[] passTurns = fill(horizon, -0.45);
		double[] stopSpeeds = fill(horizon, 0.0);
		double[] stopTurns = fill(horizon, 0.0);

		double passCost = planner.evaluate(vehicle, path, List.of(blocker), cruiseSpeeds, passTurns);
		double stopCost = planner.evaluate(vehicle, path, List.of(blocker), stopSpeeds, stopTurns);
		assertTrue(
				passCost < stopCost,
				"Risk-tolerant progress should beat full-stop avoidance: pass="
						+ passCost + " stop=" + stopCost);
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

}
