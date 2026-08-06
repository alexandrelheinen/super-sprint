package model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One Hall of Fame entry: player name, total race duration, lap count, and the
 * car model used. Rankings use {@link #getMeanLapTimeMs()} so races with
 * different lap counts remain comparable.
 */
public class Result implements Serializable {

	/** Bumped when {@code carModelIndex} was added; incompatible with UID 2 files. */
	private static final long serialVersionUID = 3L;

	private final String name;
	private final double durationMs;
	private final int lapCount;
	private final int carModelIndex;
	private final String date;

	public Result(String name, double durationMs, int lapCount, int carModelIndex) {
		if (lapCount <= 0) {
			throw new IllegalArgumentException("Lap count must be positive: " + lapCount);
		}
		GameCatalog.carModelName(carModelIndex);
		this.name = name;
		this.durationMs = durationMs;
		this.lapCount = lapCount;
		this.carModelIndex = carModelIndex;
		DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		this.date = format.format(new Date());
	}

	public String getName() {
		return name;
	}

	/** Total race duration in milliseconds. */
	public double getDurationMs() {
		return durationMs;
	}

	public int getLapCount() {
		return lapCount;
	}

	/** Zero-based car model index from {@link GameCatalog}. */
	public int getCarModelIndex() {
		return carModelIndex;
	}

	/** Mean duration per lap in milliseconds (ranking key). */
	public double getMeanLapTimeMs() {
		return durationMs / lapCount;
	}

	public String getDate() {
		return date;
	}
}
