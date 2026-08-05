package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TerrainCatalogTest {

	@Test
	void parsesKnownTerrainIds() {
		assertEquals(Terrain.GRASS, Terrain.fromId("grass"));
		assertEquals(Terrain.SAND, Terrain.fromId("SAND"));
		assertEquals(Terrain.SAND, Terrain.fromId(" sand "));
	}

	@Test
	void mapsLegacyTerrainIds() {
		assertEquals(Terrain.SAND, Terrain.fromId("desert"));
		assertEquals(Terrain.GRASS, Terrain.fromId("forest"));
		assertEquals(Terrain.GRASS, Terrain.fromId("autumn"));
	}

	@Test
	void unknownTerrainFallsBackToGrass() {
		assertEquals(Terrain.GRASS, Terrain.fromId("tundra"));
	}

	@Test
	void eachConfiguredTrackHasATerrain() {
		assertEquals(4, GameCatalog.TRACK_TERRAINS.length);
		assertEquals(Terrain.GRASS, GameCatalog.trackTerrain(0));
		assertEquals(Terrain.SAND, GameCatalog.trackTerrain(1));
		assertEquals(Terrain.GRASS, GameCatalog.trackTerrain(2));
		assertEquals(Terrain.SAND, GameCatalog.trackTerrain(3));
	}

	@Test
	void rejectsOutOfRangeTrackTerrainLookup() {
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackTerrain(-1));
		assertThrows(IllegalArgumentException.class, () -> GameCatalog.trackTerrain(Circuit.TRACK_COUNT));
	}
}
