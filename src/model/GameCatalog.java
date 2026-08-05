package model;

public final class GameCatalog {

	public static final String[] CAR_MODEL_NAMES = {"A-Type", "B-Type", "Z-Type", "T-Rex"};
	public static final String[] TRACK_NAMES = {
			"Campus Loop",
			"Foundry Eight",
			"Serpent Pass",
			"Metro Chicane"
	};

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

	private static void validateIndex(int index, int count, String label) {
		if (index < 1 || index > count) {
			throw new IllegalArgumentException("Invalid " + label + " index: " + index);
		}
	}
}
