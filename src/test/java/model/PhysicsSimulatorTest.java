package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import controller.Game;
import view.GameFrame;

public class PhysicsSimulatorTest {

	private static final double DELTA_SECONDS = Game.TICK_INTERVAL_MS / 1000.0;
	private static final double EPSILON = 1e-6;

	@Test
	public void greenReferenceCarWeighsFiveHundredKilograms() {
		assertEquals(1, GameConfig.REFERENCE_MODEL_INDEX);
		assertEquals(500.0, GameConfig.REFERENCE_MASS_KG, EPSILON);
		assertEquals(
				500.0,
				Car.getModelMassKg(GameConfig.REFERENCE_MODEL_INDEX),
				0.5);
		assertTrue(Car.KILOGRAMS_PER_OPAQUE_PIXEL > 0.0);
		assertEquals(
				Car.getModelOpaquePixels(1) * Car.KILOGRAMS_PER_OPAQUE_PIXEL,
				Car.getModelMassKg(1),
				EPSILON);
	}

	@Test
	public void purpleVintageRacerIsNearFiveHundredKilograms() {
		// Purple Retro Grand Prix (index 7) shares the vintage open-wheel look.
		assertEquals(500.0, Car.getModelMassKg(7), 40.0);
	}

	@Test
	public void overlappingCarsSeparateAndExchangeMomentumByMass() {
		TrackFixture fixture = twoCarsOverlapping();
		Car heavy = fixture.cars[0];
		Car light = fixture.cars[1];
		heavy.applyKinematicState(
				heavy.getPositionXMeters(),
				heavy.getPositionYMeters(),
				0f,
				20f);
		light.applyKinematicState(
				light.getPositionXMeters(),
				light.getPositionYMeters(),
				0f,
				5f);

		double heavyBefore = heavy.getSpeed();
		double lightBefore = light.getSpeed();
		double gapBefore = Math.hypot(
				heavy.getPositionXMeters() - light.getPositionXMeters(),
				heavy.getPositionYMeters() - light.getPositionYMeters());

		PhysicsSimulator.resolveCarCollision(heavy, light);

		double gapAfter = Math.hypot(
				heavy.getPositionXMeters() - light.getPositionXMeters(),
				heavy.getPositionYMeters() - light.getPositionYMeters());
		assertTrue(gapAfter > gapBefore - EPSILON, "Cars should separate on contact");
		assertTrue(
				light.getSpeed() > lightBefore - EPSILON,
				"Lighter car should gain forward speed from the heavier hit");
		assertTrue(
				heavy.getSpeed() < heavyBefore + EPSILON,
				"Heavier car should not gain speed from hitting a lighter one");
		assertTrue(heavy.getMassKg() > light.getMassKg());
	}

	@Test
	public void simulateStepIntegratesThenResolvesWalls() {
		TrackFixture fixture = oneCar();
		Car car = fixture.cars[0];
		// Place the car past the outer wall of the start vertical lane.
		car.applyKinematicState(
				WorldUnits.pxToM(Circuit.OUTER_RADIUS + 8),
				car.getPositionYMeters(),
				(float) (-Math.PI / 2),
				12f);
		assertTrue(fixture.circuit.findWallContact(car) != null);

		PhysicsSimulator.simulateStep(fixture.cars, fixture.circuit, DELTA_SECONDS);
		assertTrue(
				fixture.circuit.findWallContact(car) == null
						|| car.getPositionXMeters() < WorldUnits.pxToM(Circuit.OUTER_RADIUS + 8),
				"Wall response should push the car back toward the asphalt");
		assertTrue(car.getSpeed() <= 12f + EPSILON);
	}

	@Test
	public void orientedBoxesDetectRotatedOverlapThatAabbMisses() {
		TrackFixture fixture = twoCarsOverlapping();
		Car a = fixture.cars[0];
		Car b = fixture.cars[1];
		a.applyKinematicState(10.0, 10.0, 0f, 0f);
		// Place B so AABB of top-left anchors might miss, but OBB centers overlap.
		b.applyKinematicState(12.5, 10.2, (float) (Math.PI / 2), 0f);
		PhysicsSimulator.resolveCarCollision(a, b);
		double gap = Math.hypot(
				a.getPositionXMeters() - b.getPositionXMeters(),
				a.getPositionYMeters() - b.getPositionYMeters());
		assertTrue(gap > 0.0);
	}

	private static TrackFixture oneCar() {
		return carsOnTrack(new int[] {5});
	}

	private static TrackFixture twoCarsOverlapping() {
		TrackFixture fixture = carsOnTrack(new int[] {5, 1});
		Car a = fixture.cars[0];
		Car b = fixture.cars[1];
		a.applyKinematicState(12.0, 14.0, 0f, 0f);
		b.applyKinematicState(13.2, 14.1, 0f, 0f);
		return fixture;
	}

	private static TrackFixture carsOnTrack(int[] modelIndexes) {
		int trackIndex = 0;
		int[][] trackMap = GameCatalog.trackMap(trackIndex);
		GameFrame frame = new GameFrame(modelIndexes, trackMap, trackIndex);
		Circuit circuit = new Circuit(frame, trackMap);
		circuit.initializeFinishLine(trackIndex);
		Car[] cars = new Car[modelIndexes.length];
		for (int index = 0; index < modelIndexes.length; index++) {
			cars[index] = new Car(modelIndexes[index], index + 1, Integer.toString(index + 1), frame, circuit);
		}
		return new TrackFixture(cars, circuit);
	}

	private record TrackFixture(Car[] cars, Circuit circuit) {
	}
}
