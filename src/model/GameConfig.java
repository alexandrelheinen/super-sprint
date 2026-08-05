package model;

import java.awt.Color;

/**
 * Typed accessors for game and catalog settings loaded from {@code src/data/config/}.
 */
public final class GameConfig {

	private static final String KEY_GAME_TITLE = "game.title";
	private static final String KEY_MAX_CARS = "game.race.max.cars";
	private static final String KEY_MAX_HUMAN_PLAYERS = "game.race.max.human.players";
	private static final String KEY_CAR_MODEL_NAMES = "catalog.car.names";
	private static final String KEY_TRACK_NAMES = "catalog.track.names";
	private static final String KEY_TRACK_TERRAINS = "catalog.track.terrains";
	private static final String KEY_LAP_COUNT_OPTIONS = "catalog.lap.options";
	private static final String KEY_LAP_COUNT_DEFAULT = "catalog.lap.default";
	private static final String KEY_HALL_DEFAULT_NAMES = "catalog.hall.default.names";
	private static final String KEY_PIXELS_PER_METER = "world.pixelsPerMeter";
	private static final String KEY_METERS_PER_TILE = "world.metersPerTile";

	private static final String DEFAULT_GAME_TITLE = "Super Sprint Supelec";
	private static final String DEFAULT_CAR_MODEL_NAMES =
			"Vintage Yellow Hot Rod,Classic Green Formula,Blue GT Coupe,Red Flame Muscle,"
					+ "Silver Open-Wheel Racer,Brown Vintage Wagon,Orange Classic Roadster,"
					+ "Purple Retro Grand Prix,Teal Vintage Sports";
	private static final String DEFAULT_TRACK_NAMES = "Campus Loop,Foundry Eight,Serpent Pass,Metro Chicane";
	private static final String DEFAULT_TRACK_TERRAINS = "grass,sand,grass,sand";
	private static final String DEFAULT_LAP_COUNT_OPTIONS = "1,2,3,5,7,10";
	private static final int DEFAULT_LAP_COUNT_FALLBACK = 3;
	private static final int DEFAULT_MAX_CARS = 4;
	private static final int DEFAULT_MAX_HUMAN_PLAYERS = 2;
	private static final double DEFAULT_PIXELS_PER_METER = 10.0;
	private static final double DEFAULT_METERS_PER_TILE = 21.9;
	private static final String DEFAULT_HALL_NAMES =
			"Paul,Alexandre,Chloe,Nathan,Raphael,Louise,Arthur,Emma,Jules,Amelie";
	private static final int[] DEFAULT_CAR_NUMBERS = {12, 8, 21, 45, 77, 56, 3, 9, 6};
	private static final String[] DEFAULT_CAR_COLORS = {
			"200,160,40",
			"40,140,60",
			"40,90,180",
			"200,50,50",
			"160,160,165",
			"120,80,50",
			"210,110,40",
			"120,60,160",
			"40,140,140"
	};
	private static final int DEFAULT_SPRITE_WIDTH = 40;
	private static final int DEFAULT_SPRITE_HEIGHT = 20;
	/** Fallback stats: acceleration (m/s²), max speed (m/s), handling index. */
	private static final double[][] DEFAULT_CAR_STATS = {
			{16.5, 30.0, 44.0},
			{21.0, 35.5, 34.0},
			{14.0, 33.0, 46.0},
			{18.0, 29.0, 40.0},
			{19.5, 38.0, 32.0},
			{10.0, 24.0, 50.0},
			{15.0, 28.5, 56.0},
			{20.0, 36.5, 36.0},
			{13.5, 31.0, 52.0}
	};

	public static final String GAME_TITLE = ConfigLoader.getString(KEY_GAME_TITLE, DEFAULT_GAME_TITLE);
	public static final String[] CAR_MODEL_NAMES = loadCarModelNames();
	public static final int[] CAR_MODEL_NUMBERS = loadCarModelNumbers(CAR_MODEL_NAMES.length);
	public static final Color[] CAR_MODEL_COLORS = loadCarModelColors(CAR_MODEL_NAMES.length);
	public static final int[][] CAR_MODEL_SPRITE_DIMENSIONS = loadCarSpriteDimensions(CAR_MODEL_NAMES.length);
	public static final double[][] CAR_MODEL_STATS = loadCarModelStats(CAR_MODEL_NAMES.length);
	public static final String[] TRACK_NAMES = ConfigLoader.getCommaSeparated(KEY_TRACK_NAMES, DEFAULT_TRACK_NAMES);
	public static final Terrain[] TRACK_TERRAINS = loadTrackTerrains();
	public static final int[] LAP_COUNT_OPTIONS = ConfigLoader.getIntList(KEY_LAP_COUNT_OPTIONS, DEFAULT_LAP_COUNT_OPTIONS);
	public static final int DEFAULT_LAP_COUNT = ConfigLoader.getInt(KEY_LAP_COUNT_DEFAULT, DEFAULT_LAP_COUNT_FALLBACK);
	public static final int MAX_CARS = ConfigLoader.getInt(KEY_MAX_CARS, DEFAULT_MAX_CARS);
	public static final int MAX_HUMAN_PLAYERS = ConfigLoader.getInt(KEY_MAX_HUMAN_PLAYERS, DEFAULT_MAX_HUMAN_PLAYERS);
	public static final String[] HALL_DEFAULT_NAMES =
			ConfigLoader.getCommaSeparated(KEY_HALL_DEFAULT_NAMES, DEFAULT_HALL_NAMES);
	public static final double PIXELS_PER_METER =
			ConfigLoader.getDouble(KEY_PIXELS_PER_METER, DEFAULT_PIXELS_PER_METER);
	public static final double METERS_PER_TILE =
			ConfigLoader.getDouble(KEY_METERS_PER_TILE, DEFAULT_METERS_PER_TILE);

	private GameConfig() {
	}

	private static String[] loadCarModelNames() {
		String[] fromCatalog = ConfigLoader.getCommaSeparated(KEY_CAR_MODEL_NAMES, "");
		if (fromCatalog.length > 0) {
			return fromCatalog;
		}
		String[] defaults = DEFAULT_CAR_MODEL_NAMES.split(",");
		String[] names = new String[defaults.length];
		for (int index = 0; index < defaults.length; index++) {
			names[index] = ConfigLoader.getString("car." + index + ".name", defaults[index].trim());
		}
		return names;
	}

	private static int[] loadCarModelNumbers(int count) {
		int[] numbers = new int[count];
		for (int index = 0; index < count; index++) {
			int fallback = index < DEFAULT_CAR_NUMBERS.length ? DEFAULT_CAR_NUMBERS[index] : index + 1;
			numbers[index] = ConfigLoader.getInt("car." + index + ".number", fallback);
		}
		return numbers;
	}

	private static Color[] loadCarModelColors(int count) {
		Color[] colors = new Color[count];
		for (int index = 0; index < count; index++) {
			String fallback = index < DEFAULT_CAR_COLORS.length ? DEFAULT_CAR_COLORS[index] : "180,180,180";
			colors[index] = ConfigLoader.getColor("car." + index + ".color", fallback);
		}
		return colors;
	}

	private static int[][] loadCarSpriteDimensions(int count) {
		int[][] dimensions = new int[count][2];
		for (int index = 0; index < count; index++) {
			dimensions[index][0] = ConfigLoader.getInt("car." + index + ".width", DEFAULT_SPRITE_WIDTH);
			dimensions[index][1] = ConfigLoader.getInt("car." + index + ".height", DEFAULT_SPRITE_HEIGHT);
		}
		return dimensions;
	}

	private static double[][] loadCarModelStats(int count) {
		double[][] stats = new double[count][3];
		for (int index = 0; index < count; index++) {
			double[] fallback = index < DEFAULT_CAR_STATS.length
					? DEFAULT_CAR_STATS[index]
					: new double[] {14.0, 30.0, 45.0};
			stats[index][0] = ConfigLoader.getDouble("car." + index + ".acceleration", fallback[0]);
			stats[index][1] = ConfigLoader.getDouble("car." + index + ".maxSpeed", fallback[1]);
			stats[index][2] = ConfigLoader.getDouble("car." + index + ".handling", fallback[2]);
		}
		return stats;
	}

	private static Terrain[] loadTrackTerrains() {
		String[] ids = ConfigLoader.getCommaSeparated(KEY_TRACK_TERRAINS, DEFAULT_TRACK_TERRAINS);
		Terrain[] terrains = new Terrain[TRACK_NAMES.length];
		for (int index = 0; index < terrains.length; index++) {
			String id = index < ids.length ? ids[index] : Terrain.GRASS.id();
			terrains[index] = Terrain.fromId(id);
		}
		return terrains;
	}
}
