package model;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Regenerates {@code src/data/hall_of_fame.dat} using the current {@link Result} class.
 */
public final class HallOfFameSeedGenerator {

	private static final int DEFAULT_BASE_TIME_MS = 30000;
	private static final int DEFAULT_TIME_STEP_MS = 1000;

	private HallOfFameSeedGenerator() {
	}

	public static void main(String[] args) throws Exception {
		Path outputFile = Path.of(args.length > 0 ? args[0] : "src/data/hall_of_fame.dat");
		Files.createDirectories(outputFile.getParent());

		int defaultLaps = GameConfig.DEFAULT_LAP_COUNT;
		try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(outputFile.toFile()))) {
			String[] defaultNames = GameConfig.HALL_DEFAULT_NAMES;
			for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
				for (int rankIndex = 0; rankIndex < HallOfFame.MAX_RESULTS; rankIndex++) {
					output.writeObject(new Result(
							defaultNames[rankIndex],
							DEFAULT_BASE_TIME_MS + (long) DEFAULT_TIME_STEP_MS * rankIndex,
							defaultLaps));
				}
			}
		}
		System.out.println("Generated " + outputFile.toAbsolutePath());
	}
}
