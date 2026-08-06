package model;

/**
 * Visual biome for a track: selects Kenney ground tiles and flora sprites.
 * Configured per track via {@code track.N.terrain} in {@code tracks.properties}.
 */
public enum Terrain {

	GRASS("grass"),
	SAND("sand");

	private final String id;

	Terrain(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	/**
	 * Parses a terrain id (case-insensitive). Unknown values fall back to
	 * {@link #GRASS}. Legacy ids {@code desert} → sand and
	 * {@code forest}/{@code autumn} → grass.
	 */
	public static Terrain fromId(String rawId) {
		if (rawId == null || rawId.isBlank()) {
			return GRASS;
		}
		String normalized = rawId.trim().toLowerCase();
		if ("desert".equals(normalized)) {
			return SAND;
		}
		if ("forest".equals(normalized) || "autumn".equals(normalized)) {
			return GRASS;
		}
		for (Terrain terrain : values()) {
			if (terrain.id.equals(normalized)) {
				return terrain;
			}
		}
		System.err.println("Unknown terrain '" + rawId + "', falling back to grass.");
		return GRASS;
	}
}
