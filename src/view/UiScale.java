package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import model.ConfigLoader;
import model.ResourcePaths;

/**
 * Helpers for a fixed application window size and content scaling relative to
 * that baseline. The shell is never resized after {@link #lockFixedShellSize}.
 */
public final class UiScale {

	private static final String KEY_MIN_WINDOW_WIDTH = "ui.scale.min.window.width";
	private static final String KEY_MIN_WINDOW_HEIGHT = "ui.scale.min.window.height";
	private static final String KEY_MIN_FONT_SIZE = "ui.scale.min.font.size";
	private static final String KEY_SCREEN_WIDTH_DIVISOR = "ui.scale.screen.width.divisor";
	private static final String KEY_SCREEN_HEIGHT_DIVISOR = "ui.scale.screen.height.divisor";
	private static final String KEY_SHELL_WIDTH_RATIO = "ui.scale.shell.width.ratio";
	private static final String KEY_SHELL_HEIGHT_RATIO = "ui.scale.shell.height.ratio";

	private static final int MIN_WINDOW_WIDTH = ConfigLoader.getInt(KEY_MIN_WINDOW_WIDTH, 640);
	private static final int MIN_WINDOW_HEIGHT = ConfigLoader.getInt(KEY_MIN_WINDOW_HEIGHT, 480);
	private static final int SCREEN_WIDTH_DIVISOR = ConfigLoader.getInt(KEY_SCREEN_WIDTH_DIVISOR, 2);
	private static final int SCREEN_HEIGHT_DIVISOR = ConfigLoader.getInt(KEY_SCREEN_HEIGHT_DIVISOR, 2);
	private static final float SHELL_WIDTH_RATIO = ConfigLoader.getFloat(KEY_SHELL_WIDTH_RATIO, 0.55f);
	private static final float SHELL_HEIGHT_RATIO = ConfigLoader.getFloat(KEY_SHELL_HEIGHT_RATIO, 0.72f);
	private static final float MIN_FONT_SIZE = ConfigLoader.getFloat(KEY_MIN_FONT_SIZE, 14f);

	private static Dimension lockedContentSize;

	private UiScale() {
	}

	/**
	 * Baseline used for font/control scaling (half the screen on each axis).
	 */
	public static Dimension quarterScreenSize() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(
				Math.max(MIN_WINDOW_WIDTH, screen.width / SCREEN_WIDTH_DIVISOR),
				Math.max(MIN_WINDOW_HEIGHT, screen.height / SCREEN_HEIGHT_DIVISOR));
	}

	/**
	 * Content area large enough for menus and the largest race track.
	 */
	public static Dimension fixedShellContentSize(Dimension largestRaceContent) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension menu = new Dimension(
				Math.max(MIN_WINDOW_WIDTH, (int) (screen.width * SHELL_WIDTH_RATIO)),
				Math.max(MIN_WINDOW_HEIGHT, (int) (screen.height * SHELL_HEIGHT_RATIO)));
		Dimension race = largestRaceContent != null ? largestRaceContent : menu;
		return new Dimension(
				Math.max(menu.width, race.width),
				Math.max(menu.height, race.height));
	}

	/**
	 * Sizes the frame once to the fixed shell dimensions and disables resizing
	 * for the lifetime of the window.
	 */
	public static void lockFixedShellSize(JFrame frame, Dimension largestRaceContent) {
		lockedContentSize = fixedShellContentSize(largestRaceContent);
		frame.setResizable(false);
		frame.getContentPane().setPreferredSize(lockedContentSize);
		frame.getContentPane().setMinimumSize(lockedContentSize);
		// Realize peer so insets are valid before applying the outer size.
		frame.pack();
		Dimension outer = outerSizeForContent(frame, lockedContentSize);
		frame.setSize(outer);
		frame.validate();
		// Some WMs change insets after setSize; grow until the client area matches.
		Dimension actual = frame.getContentPane().getSize();
		int deltaW = lockedContentSize.width - actual.width;
		int deltaH = lockedContentSize.height - actual.height;
		if (deltaW != 0 || deltaH != 0) {
			outer = new Dimension(outer.width + deltaW, outer.height + deltaH);
			frame.setSize(outer);
			frame.validate();
		}
		frame.setMinimumSize(outer);
		frame.setMaximumSize(outer);
		frame.setPreferredSize(outer);
		frame.setLocationRelativeTo(null);
	}

	private static Dimension outerSizeForContent(JFrame frame, Dimension content) {
		Insets insets = frame.getInsets();
		return new Dimension(
				content.width + insets.left + insets.right,
				content.height + insets.top + insets.bottom);
	}

	public static float scaleFactor(Component component) {
		Dimension baseline = quarterScreenSize();
		Dimension size = lockedContentSize != null
				? lockedContentSize
				: new Dimension(
						component.getWidth() > 0 ? component.getWidth() : baseline.width,
						component.getHeight() > 0 ? component.getHeight() : baseline.height);
		return Math.min(size.width / (float) baseline.width, size.height / (float) baseline.height);
	}

	public static int scale(Component component, int value) {
		return Math.round(value * scaleFactor(component));
	}

	public static Font scaledFont(Component component, Font baseFont) {
		float factor = scaleFactor(component);
		return baseFont.deriveFont(Math.max(baseFont.getSize2D() * factor, MIN_FONT_SIZE));
	}

	public static ImageIcon scaledCarIcon(Component component, int modelIndex, int width, int height) {
		try {
			BufferedImage sprite = ResourcePaths.loadCarMenuSprite(modelIndex);
			return new ImageIcon(scaleImage(sprite, scale(component, width), scale(component, height)));
		} catch (IOException exception) {
			return scaledIcon(component, ResourcePaths.carMenuSpritePath(modelIndex), width, height);
		}
	}

	/**
	 * Fits {@code source} inside {@code width}×{@code height} while preserving
	 * aspect ratio, centered on a transparent canvas. Never stretches the sprite
	 * to fill — menu preview slots stay a fixed size regardless of car shape.
	 */
	private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
			return scaled;
		}
		double fit = Math.min(
				width / (double) source.getWidth(),
				height / (double) source.getHeight());
		int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * fit));
		int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * fit));
		int offsetX = (width - drawWidth) / 2;
		int offsetY = (height - drawHeight) / 2;
		java.awt.Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(
				java.awt.RenderingHints.KEY_INTERPOLATION,
				java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(source, offsetX, offsetY, drawWidth, drawHeight, null);
		graphics.dispose();
		return scaled;
	}

	public static ImageIcon scaledIcon(Component component, String spritePath, int width, int height) {
		ImageIcon icon = new ImageIcon(spritePath);
		int scaledWidth = scale(component, width);
		int scaledHeight = scale(component, height);
		Image image = icon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
		return new ImageIcon(image);
	}
}
