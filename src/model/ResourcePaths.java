package model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.imageio.ImageIO;

import view.SpriteImageProcessor;

public final class ResourcePaths {

	/** Optional absolute app root override for packaged launches. */
	public static final String APP_HOME_PROPERTY = "super.sprint.home";

	private static final Path APP_HOME = resolveAppHome();
	private static final Path BUNDLED_SPRITE_DIR = APP_HOME.resolve(Paths.get("src", "sprites"));
	private static final Path PREPARED_SPRITE_DIR = APP_HOME.resolve(Paths.get("build", "sprites"));
	private static final Path SEED_HALL_OF_FAME = APP_HOME.resolve(Paths.get("src", "data", "hall_of_fame.dat"));
	private static final String APP_DATA_DIR_NAME = "super-sprint-supelec";
	private static final String HALL_OF_FAME_FILE_NAME = "hall_of_fame.dat";
	private static final String XDG_DATA_HOME_ENV = "XDG_DATA_HOME";
	private static final String APPDATA_ENV = "APPDATA";
	private static final String USER_HOME_PROPERTY = "user.home";
	private static final String OS_NAME_PROPERTY = "os.name";
	private static final String LOCAL_SHARE_DIR = ".local";
	private static final String SHARE_DIR = "share";
	private static final String MAC_APP_SUPPORT = "Library";
	private static final String MAC_APPLICATION_SUPPORT = "Application Support";

	/** Zero-padded width for indexed sprite stems (`car_00.png`, `track_01.png`). */
	public static final int SPRITE_INDEX_DIGITS = 2;
	private static final String CAR_SPRITE_STEM = "car";
	private static final String CAR_MENU_SPRITE_SUFFIX = "_menu";
	private static final String TRACK_TILE_STEM = "track";
	private static final String TRACK_PREVIEW_STEM = "track_preview";
	private static final int CODE_SOURCE_PARENT_WALK_LIMIT = 4;

	private ResourcePaths() {
	}

	/**
	 * Root directory that contains {@code src/sprites} and {@code build/sprites}.
	 * In development this is the repository root; in a packaged app it is the
	 * directory next to the application jar (or the value of
	 * {@value #APP_HOME_PROPERTY}).
	 */
	public static Path appHome() {
		return APP_HOME;
	}

	public static Path userDataDirectory() {
		String osName = System.getProperty(OS_NAME_PROPERTY, "").toLowerCase(Locale.ROOT);
		if (osName.contains("win")) {
			String appData = System.getenv(APPDATA_ENV);
			if (appData != null && !appData.isBlank()) {
				return Paths.get(appData, APP_DATA_DIR_NAME);
			}
			return Paths.get(System.getProperty(USER_HOME_PROPERTY), "AppData", "Roaming", APP_DATA_DIR_NAME);
		}
		if (osName.contains("mac")) {
			return Paths.get(
					System.getProperty(USER_HOME_PROPERTY),
					MAC_APP_SUPPORT,
					MAC_APPLICATION_SUPPORT,
					APP_DATA_DIR_NAME);
		}
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

	private static Path resolveAppHome() {
		String configured = System.getProperty(APP_HOME_PROPERTY);
		if (configured != null && !configured.isBlank()) {
			return Paths.get(configured).toAbsolutePath().normalize();
		}
		Path fromCodeSource = detectAppHomeFromCodeSource();
		if (fromCodeSource != null) {
			return fromCodeSource;
		}
		return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
	}

	private static Path detectAppHomeFromCodeSource() {
		try {
			URL location = ResourcePaths.class.getProtectionDomain().getCodeSource().getLocation();
			if (location == null) {
				return null;
			}
			Path codePath = Paths.get(location.toURI()).toAbsolutePath().normalize();
			Path candidate = Files.isRegularFile(codePath) ? codePath.getParent() : codePath;
			for (int depth = 0; depth < CODE_SOURCE_PARENT_WALK_LIMIT && candidate != null; depth++) {
				if (looksLikeAppHome(candidate)) {
					return candidate;
				}
				candidate = candidate.getParent();
			}
		} catch (URISyntaxException | RuntimeException ignored) {
			// Fall back to user.dir.
		}
		return null;
	}

	private static boolean looksLikeAppHome(Path directory) {
		return Files.isDirectory(directory.resolve("src").resolve("sprites"))
				|| Files.isDirectory(directory.resolve("build").resolve("sprites"))
				|| Files.isDirectory(directory.resolve("src").resolve("data"));
	}
}
