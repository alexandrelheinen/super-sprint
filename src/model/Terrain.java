package model;

/**
 * Visual biome for a track: selects ground fill style and flora sprite set.
 * Configured per track via {@code catalog.track.terrains}.
 */
public enum Terrain {

	GRASS("grass"),
	FOREST("forest"),
	AUTUMN("autumn"),
	DESERT("desert");

	private final String id;

	Terrain(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	/**
	 * Parses a terrain id (case-insensitive). Unknown values fall back to
	 * {@link #GRASS}.
	 */
	public static Terrain fromId(String rawId) {
		if (rawId == null || rawId.isBlank()) {
			return GRASS;
		}
		String normalized = rawId.trim().toLowerCase();
		for (Terrain terrain : values()) {
			if (terrain.id.equals(normalized)) {
				return terrain;
			}
		}
		System.err.println("Unknown terrain '" + rawId + "', falling back to grass.");
		return GRASS;
	}
}
