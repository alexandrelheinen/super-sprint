package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pure ranking helpers for mean-lap-time ordering (no Swing / file I/O).
 */
class HallOfFameRankingTest {

	@Test
	void insertionIndexPrefersLowerMeanLapTime() {
		Result[] board = {
				new Result("A", 30_000.0, 3, 0), // 10 s/lap
				new Result("B", 33_000.0, 3, 1), // 11 s/lap
				new Result("C", 36_000.0, 3, 2) // 12 s/lap
		};
		assertEquals(0, findInsertionIndex(board, 27_000.0, 3));
		assertEquals(1, findInsertionIndex(board, 32_000.0, 3));
		assertEquals(3, findInsertionIndex(board, 40_000.0, 3));
	}

	@Test
	void rankingUsesMeanNotTotalDuration() {
		Result[] board = {
				new Result("ThreeLap", 30_000.0, 3, 0), // 10 s/lap
				new Result("ThreeLapSlow", 36_000.0, 3, 1) // 12 s/lap
		};
		// 1 lap at 9 s total beats a 30 s / 3-lap race on mean
		assertEquals(0, findInsertionIndex(board, 9_000.0, 1));
		// 5 laps totaling 55 s (11 s/lap) slots between the two entries
		assertEquals(1, findInsertionIndex(board, 55_000.0, 5));
	}

	/**
	 * Mirrors {@link HallOfFame#findPlacementRank} insertion scanning without
	 * requiring a persisted board.
	 */
	private static int findInsertionIndex(Result[] board, double durationMs, int lapCount) {
		double meanLapTimeMs = durationMs / lapCount;
		int insertionIndex = board.length;
		for (int rankIndex = board.length - 1; rankIndex >= 0; rankIndex--) {
			if (meanLapTimeMs < board[rankIndex].getMeanLapTimeMs()) {
				insertionIndex = rankIndex;
			}
		}
		return insertionIndex;
	}
}
