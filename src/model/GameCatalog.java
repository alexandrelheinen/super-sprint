package model;

public final class GameCatalog {

	public static final String[] CAR_MODEL_NAMES = {"A-Type", "B-Type", "Z-Type", "T-Rex"};
	public static final String[] TRACK_NAMES = {
			"Campus Loop",
			"Foundry Eight",
			"Serpent Pass",
			"Metro Chicane"
	};
	public static final int[] LAP_COUNT_OPTIONS = {1, 2, 3, 5, 7, 10};
	public static final int DEFAULT_LAP_COUNT = 3;

	private GameCatalog() {
	}

	public static String carModelName(int modelIndex) {
		validateIndex(modelIndex, Car.CAR_MODEL_COUNT, "car model");
		return CAR_MODEL_NAMES[modelIndex - 1];
	}

	public static String trackName(int trackIndex) {
		validateIndex(trackIndex, Circuit.TRACK_COUNT, "track");
		return TRACK_NAMES[trackIndex - 1];
	}

	public static String[] carModelOptions() {
		return CAR_MODEL_NAMES.clone();
	}

	public static String[] trackOptions() {
		return TRACK_NAMES.clone();
	}

	public static String[] lapCountOptions() {
		String[] options = new String[LAP_COUNT_OPTIONS.length];
		for (int index = 0; index < LAP_COUNT_OPTIONS.length; index++) {
			int laps = LAP_COUNT_OPTIONS[index];
			options[index] = laps + (laps == 1 ? " lap" : " laps");
		}
		return options;
	}

	public static int lapCountAt(int optionIndex) {
		if (optionIndex < 0 || optionIndex >= LAP_COUNT_OPTIONS.length) {
			throw new IllegalArgumentException("Invalid lap count option index: " + optionIndex);
		}
		return LAP_COUNT_OPTIONS[optionIndex];
	}

	public static int defaultLapCountOptionIndex() {
		for (int index = 0; index < LAP_COUNT_OPTIONS.length; index++) {
			if (LAP_COUNT_OPTIONS[index] == DEFAULT_LAP_COUNT) {
				return index;
			}
		}
		return 0;
	}

	public static void validateLapCount(int laps) {
		for (int option : LAP_COUNT_OPTIONS) {
			if (option == laps) {
				return;
			}
		}
		throw new IllegalArgumentException("Invalid lap count: " + laps);
	}

	private static void validateIndex(int index, int count, String label) {
		if (index < 1 || index > count) {
			throw new IllegalArgumentException("Invalid " + label + " index: " + index);
		}
	}
}
