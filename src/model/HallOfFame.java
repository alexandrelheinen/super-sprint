package model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Observable;

import javax.swing.JOptionPane;

import view.HallFrame;

public class HallOfFame extends Observable {

	public static final int MAX_RESULTS = 10;

	private final Result[][] results;
	private int lastUpdatedTrackIndex;

	public HallOfFame(HallFrame frame) throws FileNotFoundException {
		results = new Result[Circuit.TRACK_COUNT][MAX_RESULTS];
		try (FileInputStream fileStream = new FileInputStream("halloffame.dat");
				ObjectInputStream input = new ObjectInputStream(fileStream)) {
			for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
				for (int rankIndex = 0; rankIndex < MAX_RESULTS; rankIndex++) {
					results[trackIndex][rankIndex] = (Result) input.readObject();
				}
			}
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(
					null,
					exception.getMessage() + "\nA new Hall of Fame file will be created.");
			initializeDefaultRecords();
		}

		addObserver(frame);
		lastUpdatedTrackIndex = 0;
		setChanged();
		notifyObservers();
		frame.hideHall();
	}

	private void initializeDefaultRecords() {
		String[] defaultNames = {
				"Paul", "Alexandre", "Chloe", "Nathan", "Raphael",
				"Louise", "Arthur", "Emma", "Jules", "Amelie"
		};
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			for (int rankIndex = 0; rankIndex < MAX_RESULTS; rankIndex++) {
				results[trackIndex][rankIndex] = new Result(defaultNames[rankIndex], 30000 + 1000L * rankIndex);
			}
		}
		persistResults();
	}

	private void persistResults() {
		try (FileOutputStream fileStream = new FileOutputStream("halloffame.dat");
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

	public void tryAddResult(double timeMs, int trackIndex) {
		int insertionIndex = MAX_RESULTS;
		for (int rankIndex = MAX_RESULTS - 1; rankIndex >= 0; rankIndex--) {
			double existingTime = results[trackIndex][rankIndex].getTimeMs();
			if (timeMs < existingTime) {
				insertionIndex = rankIndex;
			}
		}
		if (insertionIndex < MAX_RESULTS) {
			insertResult(insertionIndex, timeMs, trackIndex);
		} else {
			setChanged();
			notifyObservers();
		}
	}

	public int getLastUpdatedTrackIndex() {
		return lastUpdatedTrackIndex;
	}

	private void insertResult(int rankIndex, double timeMs, int trackIndex) {
		String message = "New Hall of Fame entry!\n#"
				+ (rankIndex + 1)
				+ " - Track "
				+ (trackIndex + 1)
				+ "\nEnter the player name:";
		String playerName = JOptionPane.showInputDialog(message, "Alexandre LOEBLEIN HEINEN");
		for (int shiftIndex = MAX_RESULTS - 1; shiftIndex > rankIndex; shiftIndex--) {
			results[trackIndex][shiftIndex] = results[trackIndex][shiftIndex - 1];
		}
		lastUpdatedTrackIndex = trackIndex;
		results[trackIndex][rankIndex] = new Result(playerName, timeMs);
		persistResults();
		setChanged();
		notifyObservers();
	}
}
