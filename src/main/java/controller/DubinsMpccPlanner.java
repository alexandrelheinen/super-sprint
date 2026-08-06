package controller;

import java.util.List;

import model.DubinsVehicle;
import model.ReferencePath;

/**
 * Short-horizon Model Predictive Contouring Control on a Dubins unicycle.
 *
 * <p>Single-shooting NLP over speed and turn-rate commands, warm-started from
 * PD and refined with smooth pass seeds + light coordinate descent. Walls are
 * near no-go; car actuator limits (max speed, acceleration, turn rate) enter as
 * soft saturating costs; progress and speed are strongly rewarded so traffic is
 * bypassed rather than waited out; racing-line contouring is weak inside a free
 * band. The Dubins plant still hard-saturates actuators when integrating.
 */
public final class DubinsMpccPlanner {

	/** Refine may probe mildly past actuator limits; soft costs pull back. */
	private static final double SOFT_LIMIT_SEARCH_SCALE = 1.5;
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
		smoothCommands(bestSpeeds, bestTurns);
		double bestCost = evaluate(vehicle, path, obstacles, bestSpeeds, bestTurns);

		double[] candidateSpeeds = bestSpeeds.clone();
		double[] candidateTurns = bestTurns.clone();

		// Pass-oriented seeds: commit early to a lateral offset, then settle.
		// Turn biases are fractions of the car's max turn rate so MPCC adapts
		// when arcade TURN_RATE_PER_HANDLING is retuned.
		double maxTurnRate = vehicle.getMaxTurnRate();
		double[][] passProfiles = {
				{0.00, 0.00},
				{0.04, -0.55},
				{0.04, 0.55},
				{0.08, -0.80},
				{0.08, 0.80},
				{0.10, -1.00},
				{0.10, 1.00},
				{0.02, -0.35},
				{0.02, 0.35},
				{-0.10, 0.00},
				{-0.18, -0.50},
				{-0.18, 0.50}
		};
		for (double[] profile : passProfiles) {
			applyPassProfile(
					bestSpeeds,
					bestTurns,
					candidateSpeeds,
					candidateTurns,
					profile[0],
					profile[1] * maxTurnRate,
					vehicle.getMaxSpeed(),
					maxTurnRate);
			refine(vehicle, path, obstacles, candidateSpeeds, candidateTurns);
			smoothCommands(candidateSpeeds, candidateTurns);
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

	/**
	 * Speed boost plus a two-phase lateral pass (offset then settle).
	 */
	private static void applyPassProfile(
			double[] sourceSpeeds,
			double[] sourceTurns,
			double[] destinationSpeeds,
			double[] destinationTurns,
			double relativeSpeedBias,
			double peakTurnBias,
			double maxSpeed,
			double maxTurnRate) {
		int horizon = sourceSpeeds.length;
		int turnPhase = Math.max(2, (horizon * 5) / 8);
		for (int index = 0; index < horizon; index++) {
			destinationSpeeds[index] = clamp(
					sourceSpeeds[index] * (1.0 + relativeSpeedBias),
					0.0,
					maxSpeed);
			double turnBias;
			if (Math.abs(peakTurnBias) < 1e-9) {
				turnBias = 0.0;
			} else if (index < turnPhase) {
				turnBias = peakTurnBias;
			} else {
				// Counter-steer settle so the plan does not keep spinning.
				turnBias = -0.35 * peakTurnBias;
			}
			destinationTurns[index] = clamp(
					sourceTurns[index] + turnBias,
					-maxTurnRate,
					maxTurnRate);
		}
	}

	/** Light temporal smoothing to cut high-frequency command chatter. */
	private static void smoothCommands(double[] speeds, double[] turns) {
		if (speeds.length < 3) {
			return;
		}
		double[] smoothSpeeds = speeds.clone();
		double[] smoothTurns = turns.clone();
		for (int index = 1; index < speeds.length - 1; index++) {
			smoothSpeeds[index] = 0.25 * speeds[index - 1] + 0.5 * speeds[index] + 0.25 * speeds[index + 1];
			smoothTurns[index] = 0.25 * turns[index - 1] + 0.5 * turns[index] + 0.25 * turns[index + 1];
		}
		System.arraycopy(smoothSpeeds, 0, speeds, 0, speeds.length);
		System.arraycopy(smoothTurns, 0, turns, 0, turns.length);
	}

	private void refine(
			DubinsVehicle vehicle,
			ReferencePath path,
			List<DynamicObstacle> obstacles,
			double[] speeds,
			double[] turns) {
		double speedStep = config.getRefineStepScale()
				* Math.max(1.0, vehicle.getMaxAcceleration() * config.getDtSeconds());
		double turnStep = config.getRefineStepScale() * Math.max(0.15, vehicle.getMaxTurnRate() * 0.18);
		double bestCost = evaluate(vehicle, path, obstacles, speeds, turns);
		double speedSearchMax = vehicle.getMaxSpeed() * SOFT_LIMIT_SEARCH_SCALE;
		double turnSearchMax = vehicle.getMaxTurnRate() * SOFT_LIMIT_SEARCH_SCALE;

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
						speedSearchMax,
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
						-turnSearchMax,
						turnSearchMax,
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

			// Same cruise / curvature derating as the PD warm-start so MPCC does
			// not target a higher top speed than clear-road PD (pack vs loner).
			double cruise = warmStartController.getCruiseSpeed()
					/ (1.0 + warmStartController.getCurvatureGain()
							* Math.abs(projection.curvature()));
			virtualProgress += (cruise / Math.max(sampleSpacing, 1e-3)) * dt;

			double contour = projection.crossTrackError();
			double contourExcess = Math.max(
					0.0,
					Math.abs(contour) - config.getContourDeadzoneMeters());
			double headingError = DubinsVehicle.wrapAngle(
					projection.referenceHeading() - rollout.getHeading());
			double lagSamples = virtualProgress - projection.closestIndex();
			lagSamples = wrapSampleDelta(lagSamples, path.sampleCount());
			double lagMeters = lagSamples * sampleSpacing;
			int forwardSamples = forwardSampleDelta(
					startProjection.closestIndex(),
					projection.closestIndex(),
					path.sampleCount());

			cost += config.getWeightContour() * contourExcess * contourExcess;
			cost += config.getWeightHeading() * headingError * headingError;
			cost += config.getWeightLag() * lagMeters * lagMeters;
			cost -= config.getWeightProgress() * forwardSamples * sampleSpacing;

			double speedShortfall = Math.max(0.0, cruise - rollout.getSpeed());
			cost += config.getWeightSpeed() * speedShortfall * speedShortfall;

			double deltaSpeed = speeds[index] - previousSpeedCommand;
			double deltaTurn = turns[index] - previousTurnCommand;
			cost += config.getWeightControl() * (deltaSpeed * deltaSpeed + deltaTurn * deltaTurn);
			cost += actuatorSoftConstraintCost(
					speeds[index],
					turns[index],
					deltaSpeed,
					deltaTurn,
					dt,
					vehicle);
			previousSpeedCommand = speeds[index];
			previousTurnCommand = turns[index];

			cost += wallCost(contour);
			cost += obstacleCost(rollout.getX(), rollout.getY(), time, obstacles);
		}
		return cost;
	}

	/**
	 * Soft saturating penalties for car-model actuator limits. The Dubins plant
	 * still hard-clips during rollout; these terms make the NLP prefer feasible
	 * unsaturated commands instead of relying only on post-hoc clamping.
	 */
	private double actuatorSoftConstraintCost(
			double speedCommand,
			double turnRateCommand,
			double deltaSpeed,
			double deltaTurn,
			double dtSeconds,
			DubinsVehicle vehicle) {
		double speedHigh = speedCommand - vehicle.getMaxSpeed();
		double speedLow = vehicle.getMinSpeed() - speedCommand;
		double turnExcess = Math.abs(turnRateCommand) - vehicle.getMaxTurnRate();
		double accelExcess = 0.0;
		double turnAccelExcess = 0.0;
		if (dtSeconds > 1e-12) {
			accelExcess = Math.abs(deltaSpeed) / dtSeconds - vehicle.getMaxAcceleration();
			turnAccelExcess = Math.abs(deltaTurn) / dtSeconds - vehicle.getMaxTurnRateDot();
		}
		return config.getWeightSpeedLimit() * saturatingViolation(Math.max(speedHigh, speedLow))
				+ config.getWeightTurnLimit() * saturatingViolation(turnExcess)
				+ config.getWeightAcceleration() * (
						saturatingViolation(accelExcess) + saturatingViolation(turnAccelExcess));
	}

	/**
	 * Soft barrier used for actuator, wall, and obstacle slack — grows then
	 * saturates so a single huge violation cannot dominate every other term.
	 */
	private static double saturatingViolation(double violation) {
		if (violation <= 0.0) {
			return 0.0;
		}
		double saturated = violation / (1.0 + 0.65 * violation);
		return saturated * saturated;
	}

	/**
	 * Soft barrier against the track walls. Leaving the asphalt is near no-go.
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
		double outside = Math.max(0.0, -wallClearance);
		return config.getWeightWall() * (violation * violation + 8.0 * outside * outside * outside);
	}

	/**
	 * Soft preference against other cars — avoid when a cheap lateral pass
	 * exists. Mild proximity saturates; actual overlaps add a non-saturating
	 * quadratic so ploughing through traffic is never the cheapest plan.
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
				total += config.getWeightObstacle() * saturatingViolation(violation);
			}
			// Predicted hull overlap: non-saturating so ploughing through traffic
			// cannot be masked by progress / speed rewards over the horizon.
			if (clearance < 0.0) {
				double overlap = -clearance;
				total += config.getWeightObstacle()
						* (20.0 * overlap * overlap + 12.0 * overlap * overlap * overlap);
			}
		}
		return total;
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
