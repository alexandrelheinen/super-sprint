package model;

/**
 * Typed accessors for game and catalog settings loaded from {@code src/data/config/}.
 */
public final class GameConfig {

	private static final String KEY_GAME_TITLE = "game.title";
	private static final String KEY_MAX_CARS = "game.race.max.cars";
	private static final String KEY_MAX_HUMAN_PLAYERS = "game.race.max.human.players";
	private static final String KEY_CAR_MODEL_NAMES = "catalog.car.names";
	private static final String KEY_TRACK_NAMES = "catalog.track.names";
	private static final String KEY_LAP_COUNT_OPTIONS = "catalog.lap.options";
	private static final String KEY_LAP_COUNT_DEFAULT = "catalog.lap.default";
	private static final String KEY_HALL_DEFAULT_NAMES = "catalog.hall.default.names";
	private static final String KEY_PIXELS_PER_METER = "world.pixelsPerMeter";
	private static final String KEY_METERS_PER_TILE = "world.metersPerTile";

	private static final String DEFAULT_GAME_TITLE = "Super Sprint Supelec";
	private static final String DEFAULT_CAR_MODEL_NAMES = "A-Type,B-Type,Z-Type,T-Rex";
	private static final String DEFAULT_TRACK_NAMES = "Campus Loop,Foundry Eight,Serpent Pass,Metro Chicane";
	private static final String DEFAULT_LAP_COUNT_OPTIONS = "1,2,3,5,7,10";
	private static final int DEFAULT_LAP_COUNT_FALLBACK = 3;
	private static final int DEFAULT_MAX_CARS = 4;
	private static final int DEFAULT_MAX_HUMAN_PLAYERS = 2;
	private static final double DEFAULT_PIXELS_PER_METER = 10.0;
	private static final double DEFAULT_METERS_PER_TILE = 21.9;
	private static final String DEFAULT_HALL_NAMES =
			"Paul,Alexandre,Chloe,Nathan,Raphael,Louise,Arthur,Emma,Jules,Amelie";

	public static final String GAME_TITLE = ConfigLoader.getString(KEY_GAME_TITLE, DEFAULT_GAME_TITLE);
	public static final String[] CAR_MODEL_NAMES = ConfigLoader.getCommaSeparated(KEY_CAR_MODEL_NAMES, DEFAULT_CAR_MODEL_NAMES);
	public static final String[] TRACK_NAMES = ConfigLoader.getCommaSeparated(KEY_TRACK_NAMES, DEFAULT_TRACK_NAMES);
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
}
