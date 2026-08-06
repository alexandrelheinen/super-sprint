package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultTest {

	@Test
	void meanLapTimeIsDurationDividedByLaps() {
		Result result = new Result("Alex", 45_000.0, 3, 0);
		assertEquals(15_000.0, result.getMeanLapTimeMs(), 1e-9);
		assertEquals(45_000.0, result.getDurationMs(), 1e-9);
		assertEquals(3, result.getLapCount());
		assertEquals("Alex", result.getName());
		assertEquals(0, result.getCarModelIndex());
	}

	@Test
	void fewerLapsWithSameMeanComparesEqual() {
		Result threeLaps = new Result("A", 30_000.0, 3, 1);
		Result fiveLaps = new Result("B", 50_000.0, 5, 2);
		assertEquals(threeLaps.getMeanLapTimeMs(), fiveLaps.getMeanLapTimeMs(), 1e-9);
	}

	@Test
	void fasterMeanRanksAheadRegardlessOfTotalDuration() {
		Result shortSlow = new Result("Slow", 20_000.0, 1, 0); // 20 s/lap
		Result longFast = new Result("Fast", 45_000.0, 3, 1); // 15 s/lap
		assertTrue(longFast.getMeanLapTimeMs() < shortSlow.getMeanLapTimeMs());
		assertTrue(longFast.getDurationMs() > shortSlow.getDurationMs());
	}

	@Test
	void rejectsNonPositiveLapCount() {
		assertThrows(IllegalArgumentException.class, () -> new Result("X", 1000.0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new Result("X", 1000.0, -1, 0));
	}

	@Test
	void rejectsInvalidCarModelIndex() {
		assertThrows(IllegalArgumentException.class, () -> new Result("X", 1000.0, 3, -1));
		assertThrows(
				IllegalArgumentException.class,
				() -> new Result("X", 1000.0, 3, Car.CAR_MODEL_COUNT));
	}
}
