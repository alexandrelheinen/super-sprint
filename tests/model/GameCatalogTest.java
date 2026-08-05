package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GameCatalogTest {

	@Test
	public void resolvesValidCarAndTrackNames() {
		assertEquals(GameCatalog.CAR_MODEL_NAMES[0], GameCatalog.carModelName(1));
		assertEquals(GameCatalog.TRACK_NAMES[Circuit.TRACK_COUNT - 1], GameCatalog.trackName(Circuit.TRACK_COUNT));
	}

	@Test
	public void rejectsOutOfRangeIndices() {
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.carModelName(0));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackName(Circuit.TRACK_COUNT + 1));
	}

	@Test
	public void lapCountOptionsMatchCatalog() {
		assertEquals(GameCatalog.LAP_COUNT_OPTIONS.length, GameCatalog.lapCountOptions().length);
		for (int index = 0; index < GameCatalog.LAP_COUNT_OPTIONS.length; index++) {
			assertEquals(GameCatalog.LAP_COUNT_OPTIONS[index], GameCatalog.lapCountAt(index));
		}
	}

	@Test
	public void defaultLapCountIsAValidOption() {
		int defaultIndex = GameCatalog.defaultLapCountOptionIndex();
		assertEquals(GameCatalog.DEFAULT_LAP_COUNT, GameCatalog.lapCountAt(defaultIndex));
		assertDoesNotThrow(() -> GameCatalog.validateLapCount(GameCatalog.DEFAULT_LAP_COUNT));
	}

	@Test
	public void rejectsInvalidLapCounts() {
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.validateLapCount(-1));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.lapCountAt(-1));
		assertThrows(
				IllegalArgumentException.class,
				() -> GameCatalog.lapCountAt(GameCatalog.LAP_COUNT_OPTIONS.length));
	}
}
