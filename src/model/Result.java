package model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Result implements Serializable {

	private static final long serialVersionUID = 1L;
	private final String name;
	private final double timeMs;
	private final String date;

	public Result(String name, double timeMs) {
		this.name = name;
		this.timeMs = timeMs;
		DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		this.date = format.format(new Date());
	}

	public String getName() {
		return name;
	}

	public double getTimeMs() {
		return timeMs;
	}

	public String getDate() {
		return date;
	}
}
