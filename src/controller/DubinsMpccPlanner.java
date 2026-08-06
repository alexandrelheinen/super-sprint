package controller;

import java.util.List;

import model.DubinsVehicle;
import model.ReferencePath;

/**
 * Short-horizon Model Predictive Contouring Control on a Dubins unicycle.
 *
 * <p>Formulates a single-shooting NLP over speed and turn-rate commands,
 * warm-started from a PD path follower and refined with coordinate descent.
 * Costs penalize contouring (cross-track) error, lag behind a virtual path
 * progress variable, control roughness, soft track-wall violations, and a mild
 * preference against predicted opponents. Wall / lane-boundary costs dominate
 * so avoidance does not drive cars off the asphalt. Opponent proximity is
 * avoid-if-possible, not a hard no-go — progress rewards still encourage risky
 * overtakes when a bypass is available.
 */
public final class DubinsMpccPlanner {

	private final MpccConfig config;
	private final PdPathFollowController warmStartController;

	public DubinsMpccPlanner(MpccConfig config, PdPathFollowController warmStartController) {
		this.config = config;
		this.warmStartController = warmStartController;
	}

	public MpccConfig getConfig() {
		return config;
	}

	/**
	 * Plans a horizon of commands from the current vehicle state.
	 *
	 * @return planned commands (length = horizon); never {@code null}
	 */
	public Plan plan(
			DubinsVehicle vehicle,
			ReferencePath path,
			List<DynamicObstacle> obstacles) {
		int horizon = config.getHorizonStepCount();
		if (path.isEmpty() || horizon <= 0) {
			return Plan.hold(vehicle.getSpeed(), horizon);
		}

		double[] bestSpeeds = new double[horizon];
		double[] bestTurns = new double[horizon];
		seedFromPd(vehicle, path, bestSpeeds, bestTurns);
		double bestCost = evaluate(vehicle, path, obstacles, bestSpeeds, bestTurns);

		double[] candidateSpeeds = bestSpeeds.clone();
		double[] candidateTurns = bestTurns.clone();
		double[][] biasProfiles = {
				{0.0, 0.0},
				{0.0, -0.55},
				{0.0, 0.55},
				{-0.22, 0.0},
				{-0.18, -0.70},
				{-0.18, 0.70},
				{0.08, -0.35},
				{0.08, 0.35}
		};
		for (double[] bias : biasProfiles) {
			applySpeedBias(bestSpeeds, candidateSpeeds, bias[0], vehicle.getMaxSpeed());
			applyTurnBias(bestTurns, candidateTurns, bias[1], vehicle.getMaxTurnRate());
			refine(vehicle, path, obstacles, candidateSpeeds, candidateTurns);
			double cost = evaluate(vehicle, path, obstacles, candidateSpeeds, candidateTurns);
			if (cost < bestCost) {
				bestCost = cost;
				System.arraycopy(candidateSpeeds, 0, bestSpeeds, 0, horizon);
				System.arraycopy(candidateTurns, 0, bestTurns, 0, horizon);
			}
		}

		Command[] commands = new Command[horizon];
		for (int index = 0; index < horizon; index++) {
			commands[index] = new Command(bestSpeeds[index], bestTurns[index]);
		}
		return new Plan(commands, bestCost);
	}

	private void seedFromPd(
			DubinsVehicle vehicle,
			ReferencePath path,
			double[] speeds,
			double[] turns) {
		DubinsVehicle rollout = vehicle.copy();
		PdPathFollowController seedController = copyWarmStartController();
		double dt = config.getDtSeconds();
		for (int index = 0; index < speeds.length; index++) {
			double[] commands = seedController.track(
					rollout.getX(),
					rollout.getY(),
					rollout.getHeading(),
					rollout.getSpeed(),
					path,
					dt);
			speeds[index] = commands[0];
			turns[index] = commands[1];
			rollout.step(commands[0], commands[1], dt);
		}
	}

	private PdPathFollowController copyWarmStartController() {
		return new PdPathFollowController(
				warmStartController.getKpHeading(),
				warmStartController.getKdHeading(),
				warmStartController.getKpCrossTrack(),
				warmStartController.getKpSpeed(),
				warmStartController.getKdSpeed(),
				warmStartController.getCruiseSpeed(),
				warmStartController.getCurvatureGain());
	}

	private void refine(
			DubinsVehicle vehicle,
			ReferencePath path,
			List<DynamicObstacle> obstacles,
			double[] speeds,
			double[] turns) {
		double speedStep = config.getRefineStepScale() * Math.max(1.0, vehicle.getMaxAcceleration() * config.getDtSeconds());
		double turnStep = config.getRefineStepScale() * Math.max(0.2, vehicle.getMaxTurnRate() * 0.25);
		double bestCost = evaluate(vehicle, path, obstacles, speeds, turns);

		for (int pass = 0; pass < config.getRefinePassCount(); pass++) {
			boolean improved = false;
			for (int index = 0; index < speeds.length; index++) {
				bestCost = improveScalar(
						vehicle,
						path,
						obstacles,
						speeds,
						turns,
						index,
						true,
						speedStep,
						vehicle.getMinSpeed(),
						vehicle.getMaxSpeed(),
						bestCost);
				double nextCost = improveScalar(
						vehicle,
						path,
						obstacles,
						speeds,
						turns,
						index,
						false,
						turnStep,
						-vehicle.getMaxTurnRate(),
						vehicle.getMaxTurnRate(),
						bestCost);
				if (nextCost < bestCost - 1e-9) {
					improved = true;
				}
				bestCost = nextCost;
			}
			if (!improved) {
				break;
			}
			speedStep *= 0.5;
			turnStep *= 0.5;
		}
	}

	private double improveScalar(
			DubinsVehicle vehicle,
			ReferencePath path,
			List<DynamicObstacle> obstacles,
			double[] speeds,
			double[] turns,
			int index,
			boolean speedDimension,
			double step,
			double minValue,
			double maxValue,
			double bestCost) {
		double original = speedDimension ? speeds[index] : turns[index];
		double[] trialValues = {
				clamp(original + step, minValue, maxValue),
				clamp(original - step, minValue, maxValue)
		};
		for (double trial : trialValues) {
			if (Math.abs(trial - original) < 1e-12) {
				continue;
			}
			if (speedDimension) {
				speeds[index] = trial;
			} else {
				turns[index] = trial;
			}
			double cost = evaluate(vehicle, path, obstacles, speeds, turns);
			if (cost < bestCost) {
				bestCost = cost;
				original = trial;
			} else if (speedDimension) {
				speeds[index] = original;
			} else {
				turns[index] = original;
			}
		}
		if (speedDimension) {
			speeds[index] = original;
		} else {
			turns[index] = original;
		}
		return bestCost;
	}

	double evaluate(
			DubinsVehicle vehicle,
			ReferencePath path,
			List<DynamicObstacle> obstacles,
			double[] speeds,
			double[] turns) {
		DubinsVehicle rollout = vehicle.copy();
		double dt = config.getDtSeconds();
		double sampleSpacing = estimateSampleSpacing(path);
		ReferencePath.Projection startProjection = path.project(
				rollout.getX(),
				rollout.getY(),
				ReferencePath.NO_HINT);
		double virtualProgress = startProjection.closestIndex();
		int previousIndex = startProjection.closestIndex();
		double previousSpeedCommand = rollout.getSpeed();
		double previousTurnCommand = rollout.getTurnRate();
		double cost = 0.0;

		for (int index = 0; index < speeds.length; index++) {
			rollout.step(speeds[index], turns[index], dt);
			double time = (index + 1) * dt;
			ReferencePath.Projection projection = path.project(
					rollout.getX(),
					rollout.getY(),
					previousIndex);
			previousIndex = projection.closestIndex();

			double cruise = vehicle.getMaxSpeed() * config.getCruiseSpeedRatio()
					/ (1.0 + config.getCurvatureGain() * Math.abs(projection.curvature()));
			virtualProgress += (cruise / Math.max(sampleSpacing, 1e-3)) * dt;

			double contour = projection.crossTrackError();
			double headingError = DubinsVehicle.wrapAngle(
					projection.referenceHeading() - rollout.getHeading());
			double lagSamples = virtualProgress - projection.closestIndex();
			lagSamples = wrapSampleDelta(lagSamples, path.sampleCount());
			double lagMeters = lagSamples * sampleSpacing;
			// Progress is measured from the start of the horizon so early
			// dithering around the same sample is not rewarded.
			int forwardSamples = forwardSampleDelta(
					startProjection.closestIndex(),
					projection.closestIndex(),
					path.sampleCount());

			cost += config.getWeightContour() * contour * contour;
			cost += config.getWeightHeading() * headingError * headingError;
			cost += config.getWeightLag() * lagMeters * lagMeters;
			cost -= config.getWeightProgress() * forwardSamples * sampleSpacing;
			double deltaSpeed = speeds[index] - previousSpeedCommand;
			double deltaTurn = turns[index] - previousTurnCommand;
			cost += config.getWeightControl() * (deltaSpeed * deltaSpeed + deltaTurn * deltaTurn);
			previousSpeedCommand = speeds[index];
			previousTurnCommand = turns[index];

			cost += wallCost(contour);
			cost += obstacleCost(rollout.getX(), rollout.getY(), time, obstacles);
		}
		return cost;
	}

	/**
	 * Soft barrier against the track walls. Uses cross-track error versus the
	 * lane half-width so leaving the asphalt is penalized far more heavily than
	 * a comparable car-to-car clearance violation.
	 */
	private double wallCost(double crossTrackError) {
		double absCrossTrack = Math.abs(crossTrackError);
		double wallClearance = config.getLaneHalfWidthMeters()
				- config.getEgoRadiusMeters()
				- absCrossTrack;
		double violation = config.getWallSafeMarginMeters() - wallClearance;
		if (violation <= 0.0) {
			return 0.0;
		}
		// Quadratic near the wall, with an extra cubic kick once the body would
		// intersect the boundary so off-track plans are almost never chosen.
		double outside = Math.max(0.0, -wallClearance);
		return config.getWeightWall() * (violation * violation + 8.0 * outside * outside * outside);
	}

	/**
	 * Mild, saturating preference against other cars. Close traffic is
	 * discouraged, but the cost stays finite under overlap so the planner can
	 * accept a brush when progress / bypass is worth the risk. Walls remain the
	 * real no-go via {@link #wallCost(double)}.
	 */
	private double obstacleCost(
			double x,
			double y,
			double timeSeconds,
			List<DynamicObstacle> obstacles) {
		if (obstacles == null || obstacles.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		for (DynamicObstacle obstacle : obstacles) {
			double ox = obstacle.predictedX(timeSeconds);
			double oy = obstacle.predictedY(timeSeconds);
			double distance = Math.hypot(x - ox, y - oy);
			double clearance = distance - config.getEgoRadiusMeters() - obstacle.getRadiusMeters();
			double violation = config.getObstacleSafeMarginMeters() - clearance;
			if (violation > 0.0) {
				// Saturate so deep overlap does not explode into a hard barrier.
				double saturated = violation / (1.0 + 0.45 * violation);
				total += config.getWeightObstacle() * saturated * saturated;
			}
		}
		return total;
	}

	private static void applySpeedBias(
			double[] source,
			double[] destination,
			double relativeBias,
			double maxSpeed) {
		for (int index = 0; index < source.length; index++) {
			destination[index] = clamp(source[index] * (1.0 + relativeBias), 0.0, maxSpeed);
		}
	}

	private static void applyTurnBias(
			double[] source,
			double[] destination,
			double absoluteBias,
			double maxTurnRate) {
		for (int index = 0; index < source.length; index++) {
			destination[index] = clamp(source[index] + absoluteBias, -maxTurnRate, maxTurnRate);
		}
	}

	private static double estimateSampleSpacing(ReferencePath path) {
		if (path.sampleCount() < 2) {
			return 1.0;
		}
		double[][] waypoints = path.waypoints();
		return Math.max(
				1e-3,
				Math.hypot(
						waypoints[1][0] - waypoints[0][0],
						waypoints[1][1] - waypoints[0][1]));
	}

	private static int forwardSampleDelta(int fromIndex, int toIndex, int sampleCount) {
		if (sampleCount <= 0) {
			return 0;
		}
		return Math.floorMod(toIndex - fromIndex, sampleCount);
	}

	private static double wrapSampleDelta(double delta, int sampleCount) {
		if (sampleCount <= 0) {
			return delta;
		}
		double half = sampleCount * 0.5;
		while (delta > half) {
			delta -= sampleCount;
		}
		while (delta < -half) {
			delta += sampleCount;
		}
		return delta;
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

	public record Command(double speedCommand, double turnRateCommand) {
	}

	public record Plan(Command[] commands, double cost) {
		static Plan hold(double speed, int horizon) {
			Command[] commands = new Command[Math.max(horizon, 0)];
			for (int index = 0; index < commands.length; index++) {
				commands[index] = new Command(speed, 0.0);
			}
			return new Plan(commands, 0.0);
		}
	}
}
