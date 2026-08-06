package model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

public class HallOfFame extends Observable {

	public static final int MAX_RESULTS = 10;
	/** Returned by {@link #findPlacementRank} when a result would not place. */
	public static final int NO_PLACEMENT = -1;

	private static final int DEFAULT_BASE_TIME_MS = 30000;
	private static final int DEFAULT_TIME_STEP_MS = 1000;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final String MSG_CREATE_FILE_SUFFIX = ConfigLoader.getMessage(
			"messages.hall.create.file.suffix",
			"\nA new Hall of Fame file will be created.");

	private final Result[][] results;
	private final Path hallOfFameFile;
	private final Runnable showHallAction;
	private int lastUpdatedTrackIndex;

	public HallOfFame(Observer hallView, Runnable showHallAction) throws FileNotFoundException {
		results = new Result[Circuit.TRACK_COUNT][MAX_RESULTS];
		hallOfFameFile = initializeUserHallOfFameFile();
		this.showHallAction = showHallAction;

		try (InputStream fileStream = new FileInputStream(hallOfFameFile.toFile());
				ObjectInputStream input = new ObjectInputStream(fileStream)) {
			for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
				for (int rankIndex = 0; rankIndex < MAX_RESULTS; rankIndex++) {
					results[trackIndex][rankIndex] = (Result) input.readObject();
				}
			}
		} catch (Exception exception) {
			System.err.println(
					"Could not load Hall of Fame from " + hallOfFameFile + ": "
							+ exception.getMessage()
							+ MSG_CREATE_FILE_SUFFIX.trim());
			initializeDefaultRecords();
		}

		addObserver(hallView);
		lastUpdatedTrackIndex = 0;
		setChanged();
		notifyObservers();
	}

	private Path initializeUserHallOfFameFile() throws FileNotFoundException {
		try {
			Path userDirectory = ResourcePaths.userDataDirectory();
			Files.createDirectories(userDirectory);
			Path userFile = ResourcePaths.userHallOfFameFile();
			if (!Files.exists(userFile)) {
				Path seedFile = ResourcePaths.seedHallOfFameFile();
				if (Files.exists(seedFile)) {
					Files.copy(seedFile, userFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			if (!Files.exists(userFile)) {
				throw new FileNotFoundException("Hall of Fame file not found: " + userFile);
			}
			return userFile;
		} catch (IOException exception) {
			throw new FileNotFoundException(exception.getMessage());
		}
	}

	private void initializeDefaultRecords() {
		String[] defaultNames = GameConfig.HALL_DEFAULT_NAMES;
		int defaultLaps = GameConfig.DEFAULT_LAP_COUNT;
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			for (int rankIndex = 0; rankIndex < MAX_RESULTS; rankIndex++) {
				results[trackIndex][rankIndex] = new Result(
						defaultNames[rankIndex],
						DEFAULT_BASE_TIME_MS + (long) DEFAULT_TIME_STEP_MS * rankIndex,
						defaultLaps,
						rankIndex % Car.CAR_MODEL_COUNT);
			}
		}
		persistResults();
	}

	private void persistResults() {
		try (OutputStream fileStream = new FileOutputStream(hallOfFameFile.toFile());
				ObjectOutputStream output = new ObjectOutputStream(fileStream)) {
			for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
				for (int rankIndex = 0; rankIndex < MAX_RESULTS; rankIndex++) {
					output.writeObject(results[trackIndex][rankIndex]);
				}
			}
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(null, exception.getMessage());
		}
	}

	public Result getResult(int trackIndex, int rankIndex) {
		return results[trackIndex][rankIndex];
	}

	/**
	 * Returns the 0-based leaderboard rank this race would earn when ordered by
	 * mean lap time, or {@link #NO_PLACEMENT} if it is slower than every entry.
	 */
	public int findPlacementRank(double durationMs, int lapCount, int trackIndex) {
		double meanLapTimeMs = durationMs / lapCount;
		int insertionIndex = MAX_RESULTS;
		for (int rankIndex = MAX_RESULTS - ONE_BASED_INDEX_OFFSET; rankIndex >= 0; rankIndex--) {
			Result existing = results[trackIndex][rankIndex];
			if (existing == null || meanLapTimeMs < existing.getMeanLapTimeMs()) {
				insertionIndex = rankIndex;
			}
		}
		return insertionIndex < MAX_RESULTS ? insertionIndex : NO_PLACEMENT;
	}

	/**
	 * Inserts a named result at the rank implied by mean lap time and opens the
	 * Hall of Fame. Caller must ensure the result places ({@link #findPlacementRank}
	 * is not {@link #NO_PLACEMENT}).
	 */
	public void addResult(
			String playerName,
			double durationMs,
			int lapCount,
			int trackIndex,
			int carModelIndex) {
		int rankIndex = findPlacementRank(durationMs, lapCount, trackIndex);
		if (rankIndex == NO_PLACEMENT) {
			setChanged();
			notifyObservers();
			return;
		}
		for (int shiftIndex = MAX_RESULTS - ONE_BASED_INDEX_OFFSET; shiftIndex > rankIndex; shiftIndex--) {
			results[trackIndex][shiftIndex] = results[trackIndex][shiftIndex - ONE_BASED_INDEX_OFFSET];
		}
		lastUpdatedTrackIndex = trackIndex;
		results[trackIndex][rankIndex] = new Result(playerName, durationMs, lapCount, carModelIndex);
		persistResults();
		setChanged();
		notifyObservers();
		showHallAction.run();
	}

	public int getLastUpdatedTrackIndex() {
		return lastUpdatedTrackIndex;
	}
}
