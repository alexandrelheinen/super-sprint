package model;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import javax.imageio.ImageIO;

import view.SpriteImageProcessor;

/**
 * Resolves bundled classpath resources ({@code /sprites/...}, {@code /data/...})
 * and OS-specific writable user data paths.
 */
public final class ResourcePaths {

	private static final String SPRITES_ROOT = "/sprites/";
	private static final String KENNEY_ROOT = "/sprites/kenney/";
	private static final String SEED_HALL_OF_FAME = "/data/hall_of_fame.dat";
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

	/** Zero-padded width for indexed sprite stems ({@code car_00.png}, {@code track_01.png}). */
	public static final int SPRITE_INDEX_DIGITS = 2;
	private static final String CAR_SPRITE_STEM = "car";
	private static final String CAR_MENU_SPRITE_SUFFIX = "_menu";
	private static final String TRACK_TILE_STEM = "track";
	private static final String TRACK_PREVIEW_STEM = "track_preview";

	private ResourcePaths() {
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

	/** Classpath location of the seed Hall of Fame payload. */
	public static String seedHallOfFameResource() {
		return SEED_HALL_OF_FAME;
	}

	public static InputStream openSeedHallOfFame() throws IOException {
		return openResource(SEED_HALL_OF_FAME);
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

	public static URL spriteUrl(String fileName) {
		return requireResource(SPRITES_ROOT + fileName);
	}

	public static URL kenneySpriteUrl(String fileName) {
		return requireResource(KENNEY_ROOT + fileName);
	}

	public static BufferedImage loadSprite(String fileName) throws IOException {
		return readImage(SPRITES_ROOT + fileName);
	}

	public static BufferedImage loadKenneySprite(String fileName) throws IOException {
		return readImage(KENNEY_ROOT + fileName);
	}

	public static BufferedImage loadCarSprite(int modelIndex) throws IOException {
		BufferedImage source = loadSprite(carSpriteFileName(modelIndex));
		return SpriteImageProcessor.normalizeCarSprite(source);
	}

	public static BufferedImage loadCarMenuSprite(int modelIndex) throws IOException {
		try {
			BufferedImage source = loadSprite(carMenuSpriteFileName(modelIndex));
			return SpriteImageProcessor.normalizeCarSprite(source);
		} catch (IOException exception) {
			// Older checkouts may only have race-sized sprites.
			return loadCarSprite(modelIndex);
		}
	}

	public static BufferedImage loadTrackTile(int tileType) throws IOException {
		return loadSprite(trackTileFileName(tileType));
	}

	public static boolean resourceExists(String absoluteClasspathPath) {
		return ResourcePaths.class.getResource(absoluteClasspathPath) != null;
	}

	public static InputStream openResource(String absoluteClasspathPath) throws IOException {
		URL url = ResourcePaths.class.getResource(absoluteClasspathPath);
		if (url == null) {
			throw new FileNotFoundException("Missing classpath resource: " + absoluteClasspathPath);
		}
		return url.openStream();
	}

	private static URL requireResource(String absoluteClasspathPath) {
		return Objects.requireNonNull(
				ResourcePaths.class.getResource(absoluteClasspathPath),
				"Missing classpath resource: " + absoluteClasspathPath);
	}

	private static BufferedImage readImage(String absoluteClasspathPath) throws IOException {
		try (InputStream input = openResource(absoluteClasspathPath)) {
			BufferedImage image = ImageIO.read(input);
			if (image == null) {
				throw new IOException("Unreadable image resource: " + absoluteClasspathPath);
			}
			return image;
		}
	}
}
