package model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.imageio.ImageIO;

import view.SpriteImageProcessor;

public final class ResourcePaths {

	private static final Path BUNDLED_SPRITE_DIR = Paths.get("src", "sprites");
	private static final Path PREPARED_SPRITE_DIR = Paths.get("build", "sprites");
	private static final Path SEED_HALL_OF_FAME = Paths.get("src", "data", "hall_of_fame.dat");
	private static final String APP_DATA_DIR_NAME = "super-sprint-supelec";
	private static final String HALL_OF_FAME_FILE_NAME = "hall_of_fame.dat";
	private static final String XDG_DATA_HOME_ENV = "XDG_DATA_HOME";
	private static final String USER_HOME_PROPERTY = "user.home";
	private static final String LOCAL_SHARE_DIR = ".local";
	private static final String SHARE_DIR = "share";

	/** Zero-padded width for indexed sprite stems (`car_00.png`, `track_01.png`). */
	public static final int SPRITE_INDEX_DIGITS = 2;
	private static final String CAR_SPRITE_STEM = "car";
	private static final String CAR_MENU_SPRITE_SUFFIX = "_menu";
	private static final String TRACK_TILE_STEM = "track";
	private static final String TRACK_PREVIEW_STEM = "track_preview";

	private ResourcePaths() {
	}

	public static Path userDataDirectory() {
		String xdgDataHome = System.getenv(XDG_DATA_HOME_ENV);
		Path baseDirectory = (xdgDataHome != null && !xdgDataHome.isBlank())
				? Paths.get(xdgDataHome)
				: Paths.get(System.getProperty(USER_HOME_PROPERTY), LOCAL_SHARE_DIR, SHARE_DIR);
		return baseDirectory.resolve(APP_DATA_DIR_NAME);
	}

	public static Path userHallOfFameFile() {
		return userDataDirectory().resolve(HALL_OF_FAME_FILE_NAME);
	}

	public static Path seedHallOfFameFile() {
		return SEED_HALL_OF_FAME;
	}

	public static String bundledSprite(String fileName) {
		return BUNDLED_SPRITE_DIR.resolve(fileName).toString();
	}

	/**
	 * Kenney Top-down Tanks Redux sprites extracted at build time into
	 * {@code build/sprites/kenney/}.
	 */
	public static String kenneySprite(String fileName) {
		Path prepared = PREPARED_SPRITE_DIR.resolve("kenney").resolve(fileName);
		if (Files.isRegularFile(prepared)) {
			return prepared.toString();
		}
		return BUNDLED_SPRITE_DIR.resolve("kenney").resolve(fileName).toString();
	}

	/**
	 * Builds {@code stem_XX.png} with a zero-based, zero-padded index
	 * (e.g. {@code car_00.png}, {@code track_03.png}).
	 */
	public static String indexedSpriteFile(String stem, int zeroBasedIndex) {
		if (zeroBasedIndex < 0) {
			throw new IllegalArgumentException("Sprite index must be >= 0: " + zeroBasedIndex);
		}
		return String.format(Locale.ROOT, "%s_%0" + SPRITE_INDEX_DIGITS + "d.png", stem, zeroBasedIndex);
	}

	public static String carSpriteFileName(int modelIndex) {
		return indexedSpriteFile(CAR_SPRITE_STEM, modelIndex);
	}

	/** Larger keyed sprite used in the race-setup menu preview. */
	public static String carMenuSpriteFileName(int modelIndex) {
		String raceName = carSpriteFileName(modelIndex);
		int dot = raceName.lastIndexOf('.');
		if (dot < 0) {
			return raceName + CAR_MENU_SPRITE_SUFFIX;
		}
		return raceName.substring(0, dot) + CAR_MENU_SPRITE_SUFFIX + raceName.substring(dot);
	}

	public static String trackTileFileName(int tileType) {
		return indexedSpriteFile(TRACK_TILE_STEM, tileType);
	}

	public static String trackPreviewFileName(int trackIndex) {
		return indexedSpriteFile(TRACK_PREVIEW_STEM, trackIndex);
	}

	public static String carSpritePath(int modelIndex) {
		return resolveSpritePath(carSpriteFileName(modelIndex));
	}

	public static String carMenuSpritePath(int modelIndex) {
		String menuFile = carMenuSpriteFileName(modelIndex);
		Path prepared = PREPARED_SPRITE_DIR.resolve(menuFile);
		if (Files.exists(prepared)) {
			return prepared.toString();
		}
		Path bundled = BUNDLED_SPRITE_DIR.resolve(menuFile);
		if (Files.exists(bundled)) {
			return bundled.toString();
		}
		// Older checkouts may only have race-sized sprites.
		return carSpritePath(modelIndex);
	}

	public static String trackTilePath(int tileType) {
		return bundledSprite(trackTileFileName(tileType));
	}

	public static BufferedImage loadCarSprite(int modelIndex) throws IOException {
		BufferedImage source = ImageIO.read(new File(carSpritePath(modelIndex)));
		return SpriteImageProcessor.normalizeCarSprite(source);
	}

	public static BufferedImage loadCarMenuSprite(int modelIndex) throws IOException {
		BufferedImage source = ImageIO.read(new File(carMenuSpritePath(modelIndex)));
		return SpriteImageProcessor.normalizeCarSprite(source);
	}

	private static String resolveSpritePath(String fileName) {
		Path prepared = PREPARED_SPRITE_DIR.resolve(fileName);
		if (Files.exists(prepared)) {
			return prepared.toString();
		}
		return bundledSprite(fileName);
	}
}
