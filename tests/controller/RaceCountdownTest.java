package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RaceCountdownTest {

	@Test
	void walksThroughThreeTwoOneGo() {
		RaceCountdown countdown = new RaceCountdown();
		assertEquals("3", countdown.label());

		assertTrue(countdown.advance(RaceCountdown.NUMBER_STEP_MS));
		assertEquals("2", countdown.label());

		assertTrue(countdown.advance(RaceCountdown.NUMBER_STEP_MS));
		assertEquals("1", countdown.label());

		assertTrue(countdown.advance(RaceCountdown.NUMBER_STEP_MS));
		assertEquals("GO!", countdown.label());
		assertTrue(countdown.isGoStep());

		assertFalse(countdown.advance(RaceCountdown.GO_STEP_MS));
		assertTrue(countdown.isFinished());
		assertEquals("", countdown.label());
	}

	@Test
	void progressResetsEachStep() {
		RaceCountdown countdown = new RaceCountdown();
		assertEquals(0f, countdown.progress(), 0.001f);
		countdown.advance(RaceCountdown.NUMBER_STEP_MS / 2);
		assertEquals(0.5f, countdown.progress(), 0.001f);
		countdown.advance(RaceCountdown.NUMBER_STEP_MS / 2);
		assertEquals("2", countdown.label());
		assertEquals(0f, countdown.progress(), 0.001f);
	}
}
