package controller;

import java.util.Collections;
import java.util.List;

import model.DubinsVehicle;
import model.ReferencePath;

/**
 * Path follower that uses PD by default and a short-horizon MPCC when opponents
 * are nearby or the car is close to a wall.
 *
 * <p>Committed open-loop MPCC plans run to completion to avoid replan chatter.
 * Wall emergencies may interrupt early; otherwise traffic plans are held until
 * exhausted so steering does not oscillate between left/right seeds.
 */
public class HybridMpccPathFollowController implements PathFollowController {

	private final PdPathFollowController pdController;
	private final DubinsMpccPlanner planner;
	private final DubinsVehicle vehicle;
	private final MpccConfig config;

	private List<DynamicObstacle> obstacles = List.of();
	private DubinsMpccPlanner.Command[] activePlan = new DubinsMpccPlanner.Command[0];
	private int planCommandIndex;
	private double planCommandElapsedSeconds;
	private int ticksSinceReplan = Integer.MAX_VALUE / 4;
	private boolean lastCommandFromMpcc;
	private int lastProjectionHint = ReferencePath.NO_HINT;

	public HybridMpccPathFollowController(
			PdPathFollowController pdController,
			DubinsVehicle vehicle,
			MpccConfig config) {
		this.pdController = pdController;
		this.vehicle = vehicle;
		this.config = config;
		this.planner = new DubinsMpccPlanner(config, pdController);
	}

	public PdPathFollowController getPdController() {
		return pdController;
	}

	public DubinsMpccPlanner getPlanner() {
		return planner;
	}

	public boolean wasLastCommandFromMpcc() {
		return lastCommandFromMpcc;
	}

	public void setObstacles(List<DynamicObstacle> obstacles) {
		if (obstacles == null || obstacles.isEmpty()) {
			this.obstacles = List.of();
		} else {
			this.obstacles = List.copyOf(obstacles);
		}
	}

	public List<DynamicObstacle> getObstacles() {
		return Collections.unmodifiableList(obstacles);
	}

	@Override
	public double[] track(
			double x,
			double y,
			double heading,
			double speed,
			ReferencePath path,
			double deltaSeconds) {
		ticksSinceReplan++;
		ReferencePath.Projection projection = path.isEmpty()
				? null
				: path.project(x, y, lastProjectionHint);
		if (projection != null) {
			lastProjectionHint = projection.closestIndex();
		}

		boolean threatNearby = nearestObstacleDistance(x, y) <= config.getTriggerDistanceMeters();
		boolean nearWall = projection != null && isNearWall(projection.crossTrackError());
		boolean needsMpcc = threatNearby || nearWall;
		boolean planExhausted = planCommandIndex >= activePlan.length;
		boolean planStale = ticksSinceReplan >= config.getReplanIntervalTicks();

		if (!needsMpcc && planExhausted) {
			activePlan = new DubinsMpccPlanner.Command[0];
			planCommandIndex = 0;
			planCommandElapsedSeconds = 0.0;
		} else if (nearWall && (planExhausted || planStale)) {
			// Walls may interrupt a traffic plan; asphalt first.
			commitPlan(planner.plan(vehicle, path, obstacles));
		} else if (threatNearby && planExhausted) {
			// Commit a pass plan and hold it open-loop until done (stability).
			commitPlan(planner.plan(vehicle, path, obstacles));
		}

		if (planCommandIndex < activePlan.length) {
			DubinsMpccPlanner.Command command = activePlan[planCommandIndex];
			planCommandElapsedSeconds += deltaSeconds;
			if (planCommandElapsedSeconds + 1e-12 >= config.getDtSeconds()) {
				planCommandIndex++;
				planCommandElapsedSeconds = 0.0;
			}
			lastCommandFromMpcc = true;
			return new double[] {command.speedCommand(), command.turnRateCommand()};
		}

		lastCommandFromMpcc = false;
		return pdController.track(x, y, heading, speed, path, deltaSeconds);
	}

	private void commitPlan(DubinsMpccPlanner.Plan plan) {
		activePlan = plan.commands();
		planCommandIndex = 0;
		planCommandElapsedSeconds = 0.0;
		ticksSinceReplan = 0;
	}

	private boolean isNearWall(double crossTrackError) {
		double wallClearance = config.getLaneHalfWidthMeters()
				- config.getEgoRadiusMeters()
				- Math.abs(crossTrackError);
		return wallClearance <= config.getWallTriggerMarginMeters();
	}

	private double nearestObstacleDistance(double x, double y) {
		double nearest = Double.POSITIVE_INFINITY;
		for (DynamicObstacle obstacle : obstacles) {
			double distance = Math.hypot(obstacle.getX() - x, obstacle.getY() - y)
					- config.getEgoRadiusMeters()
					- obstacle.getRadiusMeters();
			nearest = Math.min(nearest, distance);
		}
		return nearest;
	}
}
