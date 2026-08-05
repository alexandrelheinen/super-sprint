package view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import controller.Game;

public final class TrackPreviewGenerator {

	private static final int PREVIEW_WIDTH = 150;
	private static final int PREVIEW_HEIGHT = 100;
	private static final int ONE_BASED_INDEX_OFFSET = 1;

	private TrackPreviewGenerator() {
	}

	public static void main(String[] args) throws IOException {
		Path outputDirectory = Paths.get(args.length > 0 ? args[0] : "build/sprites");
		Files.createDirectories(outputDirectory);

		for (int trackIndex = 0; trackIndex < Game.TRACK_MAPS.length; trackIndex++) {
			Path outputFile = outputDirectory.resolve(
					"track_preview" + (trackIndex + ONE_BASED_INDEX_OFFSET) + ".png");
			ImageIO.write(
					TrackPreviewRenderer.render(Game.TRACK_MAPS[trackIndex], PREVIEW_WIDTH, PREVIEW_HEIGHT),
					"png",
					outputFile.toFile());
			System.out.println("Generated " + outputFile);
		}
	}
}
