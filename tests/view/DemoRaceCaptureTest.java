package view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DemoRaceCaptureTest {

	@Test
	public void blankSpecUsesDistinctDefaults() {
		assertArrayEquals(new int[] {0, 1, 2, 3}, DemoRaceCapture.parseCarModels(null, 4, 9));
		assertArrayEquals(new int[] {0, 1, 2, 3}, DemoRaceCapture.parseCarModels("  ", 4, 9));
	}

	@Test
	public void identicalFillsEverySlotWithModelZero() {
		assertArrayEquals(new int[] {0, 0, 0, 0}, DemoRaceCapture.parseCarModels("identical", 4, 9));
	}

	@Test
	public void identicalWithIndexFillsEverySlot() {
		assertArrayEquals(new int[] {2, 2, 2, 2}, DemoRaceCapture.parseCarModels("identical:2", 4, 9));
	}

	@Test
	public void commaSeparatedModelsAreParsed() {
		assertArrayEquals(new int[] {3, 3, 3, 3}, DemoRaceCapture.parseCarModels("3,3,3,3", 4, 9));
		assertArrayEquals(new int[] {0, 1, 2, 3}, DemoRaceCapture.parseCarModels("0,1,2,3", 4, 9));
	}

	@Test
	public void rejectsWrongArityAndOutOfRangeIndexes() {
		assertThrows(IllegalArgumentException.class, () -> DemoRaceCapture.parseCarModels("0,1", 4, 9));
		assertThrows(IllegalArgumentException.class, () -> DemoRaceCapture.parseCarModels("identical:99", 4, 9));
		assertThrows(IllegalArgumentException.class, () -> DemoRaceCapture.parseCarModels("0,1,2,9", 4, 9));
	}
}
