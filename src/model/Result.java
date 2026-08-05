package model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One Hall of Fame entry: player name, total race duration, and lap count.
 * Rankings use {@link #getMeanLapTimeMs()} so races with different lap counts
 * remain comparable.
 */
public class Result implements Serializable {

	private static final long serialVersionUID = 2L;

	private final String name;
	private final double durationMs;
	private final int lapCount;
	private final String date;

	public Result(String name, double durationMs, int lapCount) {
		if (lapCount <= 0) {
			throw new IllegalArgumentException("Lap count must be positive: " + lapCount);
		}
		this.name = name;
		this.durationMs = durationMs;
		this.lapCount = lapCount;
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

	/** Mean duration per lap in milliseconds (ranking key). */
	public double getMeanLapTimeMs() {
		return durationMs / lapCount;
	}

	public String getDate() {
		return date;
	}
}
