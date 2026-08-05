package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
