package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TerrainCatalogTest {

	@Test
	void parsesKnownTerrainIds() {
		assertEquals(Terrain.GRASS, Terrain.fromId("grass"));
		assertEquals(Terrain.FOREST, Terrain.fromId("FOREST"));
		assertEquals(Terrain.AUTUMN, Terrain.fromId(" autumn "));
		assertEquals(Terrain.DESERT, Terrain.fromId("desert"));
	}

	@Test
	void unknownTerrainFallsBackToGrass() {
		assertEquals(Terrain.GRASS, Terrain.fromId("tundra"));
	}

	@Test
	void eachConfiguredTrackHasATerrain() {
		assertEquals(Circuit.TRACK_COUNT, GameCatalog.TRACK_TERRAINS.length);
		assertEquals(Terrain.GRASS, GameCatalog.trackTerrain(0));
		assertEquals(Terrain.AUTUMN, GameCatalog.trackTerrain(1));
		assertEquals(Terrain.FOREST, GameCatalog.trackTerrain(2));
		assertEquals(Terrain.DESERT, GameCatalog.trackTerrain(3));
	}

	@Test
	void rejectsOutOfRangeTrackTerrainLookup() {
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackTerrain(-1));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackTerrain(Circuit.TRACK_COUNT));
	}
}
