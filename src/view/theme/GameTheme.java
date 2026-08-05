package view.theme;

import java.awt.Color;
import java.awt.Font;

import view.UiScale;

public final class GameTheme {

	public static final Color BACKGROUND_DARK = new Color(12, 12, 18);
	public static final Color BACKGROUND_LIGHT = new Color(228, 232, 240);
	public static final Color PANEL_SURFACE = new Color(18, 24, 36);
	public static final Color ACCENT_BLUE = new Color(0, 90, 180);
	public static final Color ACCENT_BLUE_BRIGHT = new Color(30, 130, 230);
	public static final Color ACCENT_YELLOW = new Color(255, 210, 0);
	public static final Color TEXT_PRIMARY = new Color(245, 247, 250);
	public static final Color TEXT_MUTED = new Color(170, 180, 196);
	public static final Color BORDER_SOFT = new Color(54, 68, 92);

	public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
	public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 16);
	public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
	public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 15);

	private GameTheme() {
	}

	public static Font scaled(Font font, java.awt.Component component) {
		return UiScale.scaledFont(component, font);
	}
}
