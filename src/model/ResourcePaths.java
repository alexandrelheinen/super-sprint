package model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import view.SpriteImageProcessor;

public final class ResourcePaths {

	private static final Path BUNDLED_SPRITE_DIR = Paths.get("src", "sprites");
	private static final Path PREPARED_SPRITE_DIR = Paths.get("build", "sprites");
	private static final Path SEED_HALL_OF_FAME = Paths.get("src", "data", "hall_of_fame.dat");
	private static final String APP_DATA_DIR_NAME = "super-sprint-supelec";
	private static final String HALL_OF_FAME_FILE_NAME = "hall_of_fame.dat";
	private static final String CAR_SPRITE_PREFIX = "car";
	private static final String CAR_SPRITE_SUFFIX = ".png";
	private static final String XDG_DATA_HOME_ENV = "XDG_DATA_HOME";
	private static final String USER_HOME_PROPERTY = "user.home";
	private static final String LOCAL_SHARE_DIR = ".local";
	private static final String SHARE_DIR = "share";

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

	public static String carSpritePath(int modelIndex) {
		String fileName = CAR_SPRITE_PREFIX + modelIndex + CAR_SPRITE_SUFFIX;
		Path prepared = PREPARED_SPRITE_DIR.resolve(fileName);
		if (Files.exists(prepared)) {
			return prepared.toString();
		}
		return bundledSprite(fileName);
	}

	public static BufferedImage loadCarSprite(int modelIndex) throws IOException {
		BufferedImage source = ImageIO.read(new File(carSpritePath(modelIndex)));
		return SpriteImageProcessor.normalizeCarSprite(source);
	}
}
