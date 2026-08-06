package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Car;
import model.DubinsVehicle;
import model.ReferencePath;
import model.TestPaths;

/**
 * Active AI must saturate speed, acceleration and turn rate from the car model
 * stats, and PD / MPCC must share the same cruise target so a loner does not
 * permanently fall behind a pack that is on MPCC.
 */
public class AiCarLimitSaturationTest {

	private static final double EPSILON = 1e-9;
	private static final double DELTA_SECONDS = 0.01;

	@Test
	public void hybridAiCruiseMatchesCarMaxSpeed() {
		for (int modelIndex = 0; modelIndex < Car.CAR_MODEL_COUNT; modelIndex++) {
			double maxSpeed = Car.CAR_MODEL_STATS[modelIndex][Car.STAT_MAX_SPEED_INDEX];
			double acceleration = Car.CAR_MODEL_STATS[modelIndex][Car.STAT_ACCELERATION_INDEX];
			double handling = Car.CAR_MODEL_STATS[modelIndex][Car.STAT_HANDLING_INDEX];

			TrackingLoop loop = AiController.createHybridTrackingLoop(
					maxSpeed,
					acceleration,
					handling,
					0.0,
					0.0,
					0.0);
			DubinsVehicle vehicle = loop.getVehicle();
			HybridMpccPathFollowController hybrid =
					(HybridMpccPathFollowController) loop.getController();

			assertEquals(maxSpeed, vehicle.getMaxSpeed(), EPSILON);
			assertEquals(acceleration, vehicle.getMaxAcceleration(), EPSILON);
			assertEquals(maxSpeed, hybrid.getPdController().getCruiseSpeed(), EPSILON);
		}
	}

	@Test
	public void clearRoadPdAndMpccShareTheSameCruiseTarget() {
		double maxSpeed = 30.0;
		TrackingLoop loop = AiController.createHybridTrackingLoop(
				maxSpeed,
				16.0,
				44.0,
				0.0,
				0.0,
				0.0);
		HybridMpccPathFollowController hybrid =
				(HybridMpccPathFollowController) loop.getController();
		PdPathFollowController pd = hybrid.getPdController();
		assertEquals(maxSpeed, pd.getCruiseSpeed(), EPSILON);

		ReferencePath path = TestPaths.straightEast(200, 0.5);
		DubinsVehicle vehicle = loop.getVehicle();
		vehicle.syncPose(5.0, 0.0, 0.0, maxSpeed);

		double[] speeds = new double[MpccConfig.DEFAULT.getHorizonStepCount()];
		double[] turns = new double[speeds.length];
		for (int index = 0; index < speeds.length; index++) {
			speeds[index] = maxSpeed;
			turns[index] = 0.0;
		}
		// At cruise on a clear straight, speed shortfall cost must be ~0.
		double atCruiseCost = hybrid.getPlanner().evaluate(
				vehicle,
				path,
				java.util.List.of(),
				speeds,
				turns);

		for (int index = 0; index < speeds.length; index++) {
			speeds[index] = maxSpeed * 0.88;
		}
		double belowCruiseCost = hybrid.getPlanner().evaluate(
				vehicle,
				path,
				java.util.List.of(),
				speeds,
				turns);
		assertTrue(
				belowCruiseCost > atCruiseCost + 0.05,
				"MPCC must still prefer car maxSpeed cruise over a softer PD-era target");
	}

	@Test
	public void vehicleStepSaturatesToCarModelLimits() {
		double maxSpeed = Car.CAR_MODEL_STATS[0][Car.STAT_MAX_SPEED_INDEX];
		double acceleration = Car.CAR_MODEL_STATS[0][Car.STAT_ACCELERATION_INDEX];
		double handling = Car.CAR_MODEL_STATS[0][Car.STAT_HANDLING_INDEX];
		TrackingLoop loop = AiController.createHybridTrackingLoop(
				maxSpeed,
				acceleration,
				handling,
				0.0,
				0.0,
				0.0);
		DubinsVehicle vehicle = loop.getVehicle();

		vehicle.step(1_000.0, 0.0, DELTA_SECONDS);
		assertEquals(acceleration * DELTA_SECONDS, vehicle.getSpeed(), EPSILON);

		for (int step = 0; step < 5_000; step++) {
			vehicle.step(1_000.0, 1_000.0, DELTA_SECONDS);
		}
		assertEquals(maxSpeed, vehicle.getSpeed(), EPSILON);
		assertEquals(vehicle.getMaxTurnRate(), Math.abs(vehicle.getTurnRate()), EPSILON);
	}
}
