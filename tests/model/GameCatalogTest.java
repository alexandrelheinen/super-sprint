package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GameCatalogTest {

	@Test
	public void resolvesValidCarAndTrackNames() {
		assertEquals(GameCatalog.CAR_MODEL_NAMES[0], GameCatalog.carModelName(0));
		assertEquals(
				GameCatalog.TRACK_NAMES[Circuit.TRACK_COUNT - 1],
				GameCatalog.trackName(Circuit.TRACK_COUNT - 1));
	}

	@Test
	public void exposesNineCarsWithNumbersAndColors() {
		assertEquals(9, Car.CAR_MODEL_COUNT);
		assertEquals(12, GameCatalog.carModelNumber(0));
		assertEquals("Vintage Yellow Hot Rod", GameCatalog.carModelName(0));
		assertEquals(6, GameCatalog.carModelNumber(8));
		assertEquals("Teal Vintage Sports", GameCatalog.carModelName(8));
		for (int index = 0; index < Car.CAR_MODEL_COUNT; index++) {
			assertEquals(index, Integer.parseInt(ConfigLoader.getString("car." + index + ".index", "-1")));
			assertEquals(GameCatalog.carModelNumber(index), GameCatalog.CAR_MODEL_NUMBERS[index]);
			assertEquals(GameCatalog.carModelColor(index), GameCatalog.CAR_MODEL_COLORS[index]);
		}
	}

	@Test
	public void carModelOptionsIncludeNumberAndName() {
		assertEquals("12 - Vintage Yellow Hot Rod", GameCatalog.carModelOptionLabel(0));
		assertEquals("6 - Teal Vintage Sports", GameCatalog.carModelOptionLabel(8));
		String[] options = GameCatalog.carModelOptions();
		assertEquals(Car.CAR_MODEL_COUNT, options.length);
		assertEquals("12 - Vintage Yellow Hot Rod", options[0]);
		assertEquals("77 - Silver Open-Wheel Racer", options[4]);
	}

	@Test
	public void loadsDistinctCarStatsFromProperties() {
		assertEquals(16.5, Car.getModelStat(0, Car.STAT_ACCELERATION_INDEX));
		assertEquals(30.0, Car.getModelStat(0, Car.STAT_MAX_SPEED_INDEX));
		assertEquals(44.0, Car.getModelStat(0, Car.STAT_HANDLING_INDEX));
		assertEquals(19.5, Car.getModelStat(4, Car.STAT_ACCELERATION_INDEX));
		assertEquals(38.0, Car.getModelStat(4, Car.STAT_MAX_SPEED_INDEX));
		assertEquals(10.0, Car.getModelStat(5, Car.STAT_ACCELERATION_INDEX));
		assertEquals(56.0, Car.getModelStat(6, Car.STAT_HANDLING_INDEX));
		for (int index = 0; index < Car.CAR_MODEL_COUNT; index++) {
			assertEquals(
					ConfigLoader.getDouble("car." + index + ".acceleration", -1),
					Car.getModelStat(index, Car.STAT_ACCELERATION_INDEX));
			assertEquals(
					ConfigLoader.getDouble("car." + index + ".maxSpeed", -1),
					Car.getModelStat(index, Car.STAT_MAX_SPEED_INDEX));
			assertEquals(
					ConfigLoader.getDouble("car." + index + ".handling", -1),
					Car.getModelStat(index, Car.STAT_HANDLING_INDEX));
		}
	}

	@Test
	public void rejectsOutOfRangeIndices() {
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.carModelName(-1));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.carModelName(Car.CAR_MODEL_COUNT));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackName(-1));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackName(Circuit.TRACK_COUNT));
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
