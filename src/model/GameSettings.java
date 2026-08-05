package model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GameSettings {

	private static final String SETTINGS_FILE_NAME = "game.properties";
	private static final Path SETTINGS_PATH = Path.of("src", "data", SETTINGS_FILE_NAME);

	private static final String KEY_GAME_TITLE = "game.title";
	private static final String KEY_CAR_MODEL_NAMES = "car.model.names";
	private static final String KEY_TRACK_NAMES = "track.names";
	private static final String KEY_LAP_COUNT_OPTIONS = "lap.count.options";
	private static final String KEY_LAP_COUNT_DEFAULT = "lap.count.default";
	private static final String KEY_MAX_CARS = "race.max.cars";
	private static final String KEY_MAX_HUMAN_PLAYERS = "race.max.human.players";

	private static final String DEFAULT_GAME_TITLE = "Super Sprint Supelec";
	private static final String DEFAULT_CAR_MODEL_NAMES = "A-Type,B-Type,Z-Type,T-Rex";
	private static final String DEFAULT_TRACK_NAMES = "Campus Loop,Foundry Eight,Serpent Pass,Metro Chicane";
	private static final String DEFAULT_LAP_COUNT_OPTIONS = "1,2,3,5,7,10";
	private static final int DEFAULT_LAP_COUNT_FALLBACK = 3;
	private static final int DEFAULT_MAX_CARS = 4;
	private static final int DEFAULT_MAX_HUMAN_PLAYERS = 2;

	private static final Properties PROPERTIES = loadProperties();

	public static final String GAME_TITLE = getString(KEY_GAME_TITLE, DEFAULT_GAME_TITLE);
	public static final String[] CAR_MODEL_NAMES = getCommaSeparated(KEY_CAR_MODEL_NAMES, DEFAULT_CAR_MODEL_NAMES);
	public static final String[] TRACK_NAMES = getCommaSeparated(KEY_TRACK_NAMES, DEFAULT_TRACK_NAMES);
	public static final int[] LAP_COUNT_OPTIONS = getIntList(KEY_LAP_COUNT_OPTIONS, DEFAULT_LAP_COUNT_OPTIONS);
	public static final int DEFAULT_LAP_COUNT = getInt(KEY_LAP_COUNT_DEFAULT, DEFAULT_LAP_COUNT_FALLBACK);
	public static final int MAX_CARS = getInt(KEY_MAX_CARS, DEFAULT_MAX_CARS);
	public static final int MAX_HUMAN_PLAYERS = getInt(KEY_MAX_HUMAN_PLAYERS, DEFAULT_MAX_HUMAN_PLAYERS);

	private GameSettings() {
	}

	private static Properties loadProperties() {
		Properties properties = new Properties();
		if (Files.exists(SETTINGS_PATH)) {
			try (InputStream input = Files.newInputStream(SETTINGS_PATH)) {
				properties.load(input);
			} catch (IOException exception) {
				System.err.println("Could not load " + SETTINGS_PATH + ": " + exception.getMessage());
			}
			return properties;
		}
		try (InputStream input = GameSettings.class.getResourceAsStream("/data/" + SETTINGS_FILE_NAME)) {
			if (input != null) {
				properties.load(input);
			}
		} catch (IOException exception) {
			System.err.println("Could not load bundled game settings: " + exception.getMessage());
		}
		return properties;
	}

	private static String getString(String key, String fallback) {
		return PROPERTIES.getProperty(key, fallback).trim();
	}

	private static int getInt(String key, int fallback) {
		String value = PROPERTIES.getProperty(key);
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException exception) {
			System.err.println("Invalid integer for " + key + ": " + value);
			return fallback;
		}
	}

	private static String[] getCommaSeparated(String key, String fallback) {
		String raw = getString(key, fallback);
		String[] values = raw.split(",");
		for (int index = 0; index < values.length; index++) {
			values[index] = values[index].trim();
		}
		return values;
	}

	private static int[] getIntList(String key, String fallback) {
		String[] tokens = getCommaSeparated(key, fallback);
		int[] values = new int[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			try {
				values[index] = Integer.parseInt(tokens[index]);
			} catch (NumberFormatException exception) {
				throw new IllegalStateException("Invalid lap count option: " + tokens[index], exception);
			}
		}
		return values;
	}
}
