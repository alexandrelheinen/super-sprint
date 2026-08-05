package model;

import java.awt.Color;

public final class GameCatalog {

	private static final String LAP_LABEL_SINGULAR = " lap";
	private static final String LAP_LABEL_PLURAL = " laps";
	private static final String CAR_OPTION_SEPARATOR = " - ";

	public static final String[] CAR_MODEL_NAMES = GameConfig.CAR_MODEL_NAMES;
	public static final int[] CAR_MODEL_NUMBERS = GameConfig.CAR_MODEL_NUMBERS;
	public static final Color[] CAR_MODEL_COLORS = GameConfig.CAR_MODEL_COLORS;
	public static final String[] TRACK_NAMES = GameConfig.TRACK_NAMES;
	public static final Terrain[] TRACK_TERRAINS = GameConfig.TRACK_TERRAINS;
	public static final int[][][] TRACK_MAPS = GameConfig.TRACK_MAPS;
	public static final int[] LAP_COUNT_OPTIONS = GameConfig.LAP_COUNT_OPTIONS;
	public static final int DEFAULT_LAP_COUNT = GameConfig.DEFAULT_LAP_COUNT;

	private GameCatalog() {
	}

	/** Zero-based car model index. */
	public static String carModelName(int modelIndex) {
		validateIndex(modelIndex, Car.CAR_MODEL_COUNT, "car model");
		return CAR_MODEL_NAMES[modelIndex];
	}

	/** Racing number painted on the car livery. */
	public static int carModelNumber(int modelIndex) {
		validateIndex(modelIndex, Car.CAR_MODEL_COUNT, "car model");
		return CAR_MODEL_NUMBERS[modelIndex];
	}

	/** Mean sprite color used for HUD / UI accents. */
	public static Color carModelColor(int modelIndex) {
		validateIndex(modelIndex, Car.CAR_MODEL_COUNT, "car model");
		return CAR_MODEL_COLORS[modelIndex];
	}

	/** Zero-based track index. */
	public static String trackName(int trackIndex) {
		validateIndex(trackIndex, Circuit.TRACK_COUNT, "track");
		return TRACK_NAMES[trackIndex];
	}

	/** Zero-based track index → terrain / biome for scenery. */
	public static Terrain trackTerrain(int trackIndex) {
		validateIndex(trackIndex, Circuit.TRACK_COUNT, "track");
		return TRACK_TERRAINS[trackIndex];
	}

	/** Zero-based track index → tile map ({@code track_XX.png} ids). */
	public static int[][] trackMap(int trackIndex) {
		validateIndex(trackIndex, Circuit.TRACK_COUNT, "track");
		return TRACK_MAPS[trackIndex];
	}

	/** Dropdown label: {@code 12 - Vintage Yellow Hot Rod}. */
	public static String carModelOptionLabel(int modelIndex) {
		validateIndex(modelIndex, Car.CAR_MODEL_COUNT, "car model");
		return CAR_MODEL_NUMBERS[modelIndex] + CAR_OPTION_SEPARATOR + CAR_MODEL_NAMES[modelIndex];
	}

	public static String[] carModelOptions() {
		String[] options = new String[CAR_MODEL_NAMES.length];
		for (int index = 0; index < options.length; index++) {
			options[index] = carModelOptionLabel(index);
		}
		return options;
	}

	public static String[] trackOptions() {
		return TRACK_NAMES.clone();
	}

	public static String[] lapCountOptions() {
		String[] options = new String[LAP_COUNT_OPTIONS.length];
		for (int index = 0; index < LAP_COUNT_OPTIONS.length; index++) {
			int laps = LAP_COUNT_OPTIONS[index];
			options[index] = laps + (laps == 1 ? LAP_LABEL_SINGULAR : LAP_LABEL_PLURAL);
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
		if (index < 0 || index >= count) {
			throw new IllegalArgumentException("Invalid " + label + " index: " + index);
		}
	}
}
