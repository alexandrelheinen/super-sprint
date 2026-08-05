package view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AssetPaths {

	private static final Path PREPARED_SPRITE_DIR = Paths.get("build", "images");

	private AssetPaths() {
	}

	public static String carSpritePath(int modelIndex) {
		Path prepared = PREPARED_SPRITE_DIR.resolve("voiture" + modelIndex + ".png");
		if (Files.exists(prepared)) {
			return prepared.toString();
		}
		return "images/voiture" + modelIndex + ".png";
	}
}
