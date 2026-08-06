package model;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads and merges bundled {@code /data/config/*.properties} classpath resources.
 * Files are merged in the declared order; later files override duplicate keys.
 */
public final class ConfigLoader {

	private static final String CONFIG_ROOT = "/data/config/";
	private static final String NEWLINE_ESCAPE = "\\n";
	private static final String LIST_SEPARATOR = ",";
	private static final String RGB_SEPARATOR = ",";

	/**
	 * Ordered config payloads on the classpath. Keep alphabetical so merge order
	 * stays stable and matches the historical directory listing behaviour.
	 */
	private static final String[] CONFIG_FILES = {
			"cars.properties",
			"catalog.properties",
			"game.properties",
			"messages.properties",
			"theme.properties",
			"tracks.properties",
			"ui.properties",
			"world.properties"
	};

	private static final Properties PROPERTIES = loadAll();

	private ConfigLoader() {
	}

	public static String getString(String key, String fallback) {
		return PROPERTIES.getProperty(key, fallback).trim();
	}

	public static String getMessage(String key, String fallback) {
		return unescapeNewlines(getString(key, fallback));
	}

	public static int getInt(String key, int fallback) {
		String value = PROPERTIES.getProperty(key);
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException exception) {
			System.err.println("Invalid integer for config key " + key + ": " + value);
			return fallback;
		}
	}

	public static float getFloat(String key, float fallback) {
		String value = PROPERTIES.getProperty(key);
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Float.parseFloat(value.trim());
		} catch (NumberFormatException exception) {
			System.err.println("Invalid float for config key " + key + ": " + value);
			return fallback;
		}
	}

	public static double getDouble(String key, double fallback) {
		String value = PROPERTIES.getProperty(key);
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException exception) {
			System.err.println("Invalid double for config key " + key + ": " + value);
			return fallback;
		}
	}

	public static String[] getCommaSeparated(String key, String fallback) {
		String raw = getString(key, fallback);
		if (raw.isEmpty()) {
			return new String[0];
		}
		String[] values = raw.split(LIST_SEPARATOR);
		for (int index = 0; index < values.length; index++) {
			values[index] = values[index].trim();
		}
		return values;
	}

	public static int[] getIntList(String key, String fallback) {
		String[] tokens = getCommaSeparated(key, fallback);
		int[] values = new int[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			try {
				values[index] = Integer.parseInt(tokens[index]);
			} catch (NumberFormatException exception) {
				throw new IllegalStateException("Invalid integer list entry for " + key + ": " + tokens[index], exception);
			}
		}
		return values;
	}

	public static Color getColor(String key, String fallbackRgb) {
		String raw = getString(key, fallbackRgb);
		String[] channels = raw.split(RGB_SEPARATOR);
		if (channels.length != 3) {
			throw new IllegalStateException("Expected RGB value for " + key + ", got: " + raw);
		}
		try {
			return new Color(
					Integer.parseInt(channels[0].trim()),
					Integer.parseInt(channels[1].trim()),
					Integer.parseInt(channels[2].trim()));
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("Invalid RGB value for " + key + ": " + raw, exception);
		}
	}

	private static String unescapeNewlines(String value) {
		return value.replace(NEWLINE_ESCAPE, "\n");
	}

	private static Properties loadAll() {
		Properties merged = new Properties();
		for (String fileName : CONFIG_FILES) {
			loadInto(merged, CONFIG_ROOT + fileName);
		}
		return merged;
	}

	private static void loadInto(Properties target, String absoluteClasspathPath) {
		try (InputStream stream = ConfigLoader.class.getResourceAsStream(absoluteClasspathPath)) {
			if (stream == null) {
				System.err.println("Missing config resource: " + absoluteClasspathPath);
				return;
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				Properties fileProperties = new Properties();
				fileProperties.load(reader);
				target.putAll(fileProperties);
			}
		} catch (IOException exception) {
			System.err.println("Could not load config resource " + absoluteClasspathPath + ": " + exception.getMessage());
		}
	}
}
