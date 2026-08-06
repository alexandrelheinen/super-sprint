package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
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
	void bundledCarAndTrackSpritesExistOnClasspath() throws Exception {
		for (int model = 0; model < Car.CAR_MODEL_COUNT; model++) {
			BufferedImage race = ResourcePaths.loadCarSprite(model);
			BufferedImage menu = ResourcePaths.loadCarMenuSprite(model);
			assertNotNull(race);
			assertNotNull(menu);
			assertTrue(race.getWidth() > 0);
			assertTrue(menu.getWidth() > 0);
		}
		for (int tile = Circuit.TILE_STRAIGHT_HORIZONTAL; tile <= Circuit.TILE_OPEN; tile++) {
			BufferedImage tileImage = ResourcePaths.loadTrackTile(tile);
			assertNotNull(tileImage);
			assertTrue(tileImage.getWidth() > 0);
		}
	}

	@Test
	void seedHallOfFameResourceExists() {
		assertTrue(ResourcePaths.resourceExists(ResourcePaths.seedHallOfFameResource()));
	}

	@Test
	void userDataDirectoryIsOsSpecific() {
		java.nio.file.Path userData = ResourcePaths.userDataDirectory();
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
