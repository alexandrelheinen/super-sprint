package model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcePaths {

	private static final Path BUNDLED_SPRITE_DIR = Paths.get("src", "sprites");
	private static final Path PREPARED_SPRITE_DIR = Paths.get("build", "sprites");
	private static final Path SEED_HALL_OF_FAME = Paths.get("src", "data", "hall_of_fame.dat");
	private static final String APP_DATA_DIR_NAME = "super-sprint-supelec";

	private ResourcePaths() {
	}

	public static Path userDataDirectory() {
		String xdgDataHome = System.getenv("XDG_DATA_HOME");
		Path baseDirectory = (xdgDataHome != null && !xdgDataHome.isBlank())
				? Paths.get(xdgDataHome)
				: Paths.get(System.getProperty("user.home"), ".local", "share");
		return baseDirectory.resolve(APP_DATA_DIR_NAME);
	}

	public static Path userHallOfFameFile() {
		return userDataDirectory().resolve("hall_of_fame.dat");
	}

	public static Path seedHallOfFameFile() {
		return SEED_HALL_OF_FAME;
	}

	public static String bundledSprite(String fileName) {
		return BUNDLED_SPRITE_DIR.resolve(fileName).toString();
	}

	public static String carSpritePath(int modelIndex) {
		String fileName = "car" + modelIndex + ".png";
		Path prepared = PREPARED_SPRITE_DIR.resolve(fileName);
		if (Files.exists(prepared)) {
			return prepared.toString();
		}
		return bundledSprite(fileName);
	}
}
