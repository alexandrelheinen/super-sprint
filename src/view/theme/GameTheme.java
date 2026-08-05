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

	private static final String FONT_FAMILY = "Segoe UI";
	private static final int FONT_SIZE_TITLE = 28;
	private static final int FONT_SIZE_SUBTITLE = 16;
	private static final int FONT_SIZE_BODY = 14;
	private static final int FONT_SIZE_BUTTON = 15;
	private static final int FONT_SIZE_HUD = 20;

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
