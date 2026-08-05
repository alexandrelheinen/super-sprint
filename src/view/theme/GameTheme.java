package view.theme;

import java.awt.Color;
import java.awt.Font;

import model.ConfigLoader;
import view.UiScale;

public final class GameTheme {

	private static final String KEY_COLOR_BACKGROUND_DARK = "theme.color.background.dark";
	private static final String KEY_COLOR_BACKGROUND_LIGHT = "theme.color.background.light";
	private static final String KEY_COLOR_PANEL_SURFACE = "theme.color.panel.surface";
	private static final String KEY_COLOR_ACCENT_BLUE = "theme.color.accent.blue";
	private static final String KEY_COLOR_ACCENT_BLUE_BRIGHT = "theme.color.accent.blue.bright";
	private static final String KEY_COLOR_ACCENT_YELLOW = "theme.color.accent.yellow";
	private static final String KEY_COLOR_TEXT_PRIMARY = "theme.color.text.primary";
	private static final String KEY_COLOR_TEXT_MUTED = "theme.color.text.muted";
	private static final String KEY_COLOR_BORDER_SOFT = "theme.color.border.soft";
	private static final String KEY_FONT_FAMILY = "theme.font.family";
	private static final String KEY_FONT_SIZE_TITLE = "theme.font.size.title";
	private static final String KEY_FONT_SIZE_SUBTITLE = "theme.font.size.subtitle";
	private static final String KEY_FONT_SIZE_BODY = "theme.font.size.body";
	private static final String KEY_FONT_SIZE_BUTTON = "theme.font.size.button";
	private static final String KEY_FONT_SIZE_HUD = "theme.font.size.hud";

	private static final String DEFAULT_FONT_FAMILY = "Segoe UI";
	private static final int DEFAULT_FONT_SIZE_TITLE = 28;
	private static final int DEFAULT_FONT_SIZE_SUBTITLE = 16;
	private static final int DEFAULT_FONT_SIZE_BODY = 14;
	private static final int DEFAULT_FONT_SIZE_BUTTON = 15;
	private static final int DEFAULT_FONT_SIZE_HUD = 20;

	public static final Color BACKGROUND_DARK = ConfigLoader.getColor(KEY_COLOR_BACKGROUND_DARK, "12,12,18");
	public static final Color BACKGROUND_LIGHT = ConfigLoader.getColor(KEY_COLOR_BACKGROUND_LIGHT, "228,232,240");
	public static final Color PANEL_SURFACE = ConfigLoader.getColor(KEY_COLOR_PANEL_SURFACE, "18,24,36");
	public static final Color ACCENT_BLUE = ConfigLoader.getColor(KEY_COLOR_ACCENT_BLUE, "0,90,180");
	public static final Color ACCENT_BLUE_BRIGHT = ConfigLoader.getColor(KEY_COLOR_ACCENT_BLUE_BRIGHT, "30,130,230");
	public static final Color ACCENT_YELLOW = ConfigLoader.getColor(KEY_COLOR_ACCENT_YELLOW, "255,210,0");
	public static final Color TEXT_PRIMARY = ConfigLoader.getColor(KEY_COLOR_TEXT_PRIMARY, "245,247,250");
	public static final Color TEXT_MUTED = ConfigLoader.getColor(KEY_COLOR_TEXT_MUTED, "170,180,196");
	public static final Color BORDER_SOFT = ConfigLoader.getColor(KEY_COLOR_BORDER_SOFT, "54,68,92");

	private static final String FONT_FAMILY = ConfigLoader.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
	private static final int FONT_SIZE_TITLE = ConfigLoader.getInt(KEY_FONT_SIZE_TITLE, DEFAULT_FONT_SIZE_TITLE);
	private static final int FONT_SIZE_SUBTITLE = ConfigLoader.getInt(KEY_FONT_SIZE_SUBTITLE, DEFAULT_FONT_SIZE_SUBTITLE);
	private static final int FONT_SIZE_BODY = ConfigLoader.getInt(KEY_FONT_SIZE_BODY, DEFAULT_FONT_SIZE_BODY);
	private static final int FONT_SIZE_BUTTON = ConfigLoader.getInt(KEY_FONT_SIZE_BUTTON, DEFAULT_FONT_SIZE_BUTTON);
	private static final int FONT_SIZE_HUD = ConfigLoader.getInt(KEY_FONT_SIZE_HUD, DEFAULT_FONT_SIZE_HUD);

	public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE_TITLE);
	public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE_SUBTITLE);
	public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE_BODY);
	public static final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE_BUTTON);
	public static final Font FONT_HUD = new Font(FONT_FAMILY, Font.BOLD, FONT_SIZE_HUD);

	private GameTheme() {
	}

	public static Font scaled(Font font, java.awt.Component component) {
		return UiScale.scaledFont(component, font);
	}
}
