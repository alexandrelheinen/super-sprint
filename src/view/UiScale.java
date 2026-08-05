package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import model.ConfigLoader;
import model.ResourcePaths;

/**
 * Helpers for sizing Swing windows relative to the screen and scaling content
 * when the user resizes a frame after initial layout.
 */
public final class UiScale {

	private static final String KEY_MIN_WINDOW_WIDTH = "ui.scale.min.window.width";
	private static final String KEY_MIN_WINDOW_HEIGHT = "ui.scale.min.window.height";
	private static final String KEY_MIN_FONT_SIZE = "ui.scale.min.font.size";
	private static final String KEY_SCREEN_WIDTH_DIVISOR = "ui.scale.screen.width.divisor";
	private static final String KEY_SCREEN_HEIGHT_DIVISOR = "ui.scale.screen.height.divisor";

	private static final int MIN_WINDOW_WIDTH = ConfigLoader.getInt(KEY_MIN_WINDOW_WIDTH, 640);
	private static final int MIN_WINDOW_HEIGHT = ConfigLoader.getInt(KEY_MIN_WINDOW_HEIGHT, 480);
	private static final int SCREEN_WIDTH_DIVISOR = ConfigLoader.getInt(KEY_SCREEN_WIDTH_DIVISOR, 2);
	private static final int SCREEN_HEIGHT_DIVISOR = ConfigLoader.getInt(KEY_SCREEN_HEIGHT_DIVISOR, 2);
	private static final int MINIMUM_SIZE_DIVISOR = 2;
	private static final float MIN_FONT_SIZE = ConfigLoader.getFloat(KEY_MIN_FONT_SIZE, 11f);

	private UiScale() {
	}

	/**
	 * Default window size: half the screen width and height (one quarter of total screen area).
	 */
	public static Dimension quarterScreenSize() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(
				Math.max(MIN_WINDOW_WIDTH, screen.width / SCREEN_WIDTH_DIVISOR),
				Math.max(MIN_WINDOW_HEIGHT, screen.height / SCREEN_HEIGHT_DIVISOR));
	}

	public static float scaleFactor(Component component) {
		Dimension baseline = quarterScreenSize();
		int width = component.getWidth() > 0 ? component.getWidth() : baseline.width;
		int height = component.getHeight() > 0 ? component.getHeight() : baseline.height;
		return Math.min(width / (float) baseline.width, height / (float) baseline.height);
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
			BufferedImage sprite = ResourcePaths.loadCarSprite(modelIndex);
			return new ImageIcon(scaleImage(sprite, scale(component, width), scale(component, height)));
		} catch (IOException exception) {
			return scaledIcon(component, ResourcePaths.carSpritePath(modelIndex), width, height);
		}
	}

	private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = scaled.createGraphics();
		graphics.setRenderingHint(
				java.awt.RenderingHints.KEY_INTERPOLATION,
				java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(source, 0, 0, width, height, null);
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

	public static void applyQuarterScreenSize(JFrame frame) {
		Dimension size = quarterScreenSize();
		frame.setSize(size);
		frame.setMinimumSize(new Dimension(size.width / MINIMUM_SIZE_DIVISOR, size.height / MINIMUM_SIZE_DIVISOR));
		frame.setLocationRelativeTo(null);
	}

	public static void enableDelayedResize(JFrame frame, Runnable onResize) {
		frame.setResizable(false);
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				onResize.run();
			}
		});
		SwingUtilitiesHelper.invokeLater(() -> frame.setResizable(true));
	}

	public static void fitLabelIcon(JLabel label, Component context, String spritePath, int width, int height) {
		label.setIcon(scaledIcon(context, spritePath, width, height));
	}
}
