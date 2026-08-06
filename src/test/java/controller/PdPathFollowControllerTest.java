package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.ReferencePath;
import model.TestPaths;

public class PdPathFollowControllerTest {

	private static final double DELTA_SECONDS = 0.01;

	private static PdPathFollowController controller() {
		return new PdPathFollowController(4.0, 1.0, 2.5, 2.4, 0.8, 25.0, 0.45);
	}

	@Test
	public void steersLeftWhenRightOfThePath() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		// y-down screen coordinates: y > 0 is right of an eastbound path.
		double[] commands = controller().track(20.0, 2.0, 0.0, 10.0, path, DELTA_SECONDS);
		assertTrue(commands[1] < 0.0, "Expected a left-turn command, got " + commands[1]);
	}

	@Test
	public void steersRightWhenLeftOfThePath() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		double[] commands = controller().track(20.0, -2.0, 0.0, 10.0, path, DELTA_SECONDS);
		assertTrue(commands[1] > 0.0, "Expected a right-turn command, got " + commands[1]);
	}

	@Test
	public void turnsBackTowardReferenceHeading() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		double[] commands = controller().track(20.0, 0.0, 0.4, 10.0, path, DELTA_SECONDS);
		assertTrue(commands[1] < 0.0, "Expected correction against +0.4 rad heading error");
	}

	@Test
	public void appliesCurvatureFeedforwardOnArcs() {
		double radius = 10.0;
		ReferencePath path = TestPaths.circle(radius, 720);
		// Exactly on the first sample with matching heading and no errors.
		double speed = 15.0;
		double[] commands = controller().track(
				radius, 0.0, Math.PI / 2.0, speed, path, DELTA_SECONDS);
		assertEquals(speed / radius, commands[1], 0.15);
	}

	@Test
	public void speedCommandRampsTowardCruiseSpeed() {
		ReferencePath path = TestPaths.straightEast(200, 0.5);
		double[] slowCommands = controller().track(20.0, 0.0, 0.0, 5.0, path, DELTA_SECONDS);
		assertTrue(slowCommands[0] > 5.0, "Below cruise speed the command should accelerate");

		double[] fastCommands = controller().track(20.0, 0.0, 0.0, 40.0, path, DELTA_SECONDS);
		assertTrue(fastCommands[0] < 40.0, "Above cruise speed the command should decelerate");
	}

	@Test
	public void emptyPathHoldsCurrentSpeedAndHeading() {
		double[] commands = controller().track(0.0, 0.0, 0.0, 12.0, ReferencePath.empty(), DELTA_SECONDS);
		assertEquals(12.0, commands[0], 1e-9);
		assertEquals(0.0, commands[1], 1e-9);
	}
}
