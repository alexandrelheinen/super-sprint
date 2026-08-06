package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ResourcePathsTest {

	@Test
	void formatsZeroBasedSnakeCaseSpriteNames() {
		assertEquals("car_00.png", ResourcePaths.carSpriteFileName(0));
		assertEquals("car_08.png", ResourcePaths.carSpriteFileName(8));
		assertEquals("car_00_menu.png", ResourcePaths.carMenuSpriteFileName(0));
		assertEquals("car_08_menu.png", ResourcePaths.carMenuSpriteFileName(8));
		assertEquals("track_00.png", ResourcePaths.trackTileFileName(0));
		assertEquals("track_06.png", ResourcePaths.trackTileFileName(6));
		assertEquals("track_preview_00.png", ResourcePaths.trackPreviewFileName(0));
		assertEquals("track_preview_03.png", ResourcePaths.trackPreviewFileName(3));
	}

	@Test
	void rejectsNegativeSpriteIndexes() {
		assertThrows(IllegalArgumentException.class, () -> ResourcePaths.indexedSpriteFile("car", -1));
	}

	@Test
	void bundledCarAndTrackSpritesExist() {
		for (int model = 0; model < Car.CAR_MODEL_COUNT; model++) {
			assertTrue(Files.exists(Path.of(ResourcePaths.bundledSprite(ResourcePaths.carSpriteFileName(model)))));
			assertTrue(Files.exists(Path.of(ResourcePaths.bundledSprite(ResourcePaths.carMenuSpriteFileName(model)))));
		}
		for (int tile = Circuit.TILE_STRAIGHT_HORIZONTAL; tile <= Circuit.TILE_OPEN; tile++) {
			assertTrue(Files.exists(Path.of(ResourcePaths.trackTilePath(tile))));
		}
	}

	@Test
	void appHomeResolvesRepositoryRootWithSprites() {
		Path home = ResourcePaths.appHome();
		assertTrue(Files.isDirectory(home.resolve("src").resolve("sprites")),
				"app home should contain src/sprites: " + home);
		assertTrue(Files.isDirectory(home.resolve("src").resolve("data").resolve("config")),
				"app home should contain config: " + home);
	}

	@Test
	void userDataDirectoryIsOsSpecific() {
		Path userData = ResourcePaths.userDataDirectory();
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String pathText = userData.toString();
		assertTrue(pathText.contains("super-sprint-supelec"));
		if (osName.contains("win")) {
			assertTrue(pathText.toLowerCase(Locale.ROOT).contains("appdata")
					|| pathText.contains("super-sprint-supelec"));
		} else if (osName.contains("mac")) {
			assertTrue(pathText.contains("Application Support"));
		} else {
			assertTrue(pathText.contains(".local") || pathText.contains("share")
					|| System.getenv("XDG_DATA_HOME") != null);
		}
	}
}
