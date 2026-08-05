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

import model.ResourcePaths;

/**
 * Helpers for sizing Swing windows relative to the screen and scaling content
 * when the user resizes a frame after initial layout.
 */
public final class UiScale {

	public static final int REFERENCE_WIDTH = 960;
	public static final int REFERENCE_HEIGHT = 720;

	private static final int MIN_WINDOW_WIDTH = 640;
	private static final int MIN_WINDOW_HEIGHT = 480;
	private static final int SCREEN_SIZE_DIVISOR = 2;
	private static final int MINIMUM_SIZE_DIVISOR = 2;
	private static final float MIN_FONT_SIZE = 11f;

	private UiScale() {
	}

	public static Dimension quarterScreenSize() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(
				Math.max(MIN_WINDOW_WIDTH, screen.width / SCREEN_SIZE_DIVISOR),
				Math.max(MIN_WINDOW_HEIGHT, screen.height / SCREEN_SIZE_DIVISOR));
	}

	public static float scaleFactor(Component component) {
		int width = component.getWidth() > 0 ? component.getWidth() : REFERENCE_WIDTH;
		int height = component.getHeight() > 0 ? component.getHeight() : REFERENCE_HEIGHT;
		return Math.min(width / (float) REFERENCE_WIDTH, height / (float) REFERENCE_HEIGHT);
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
			int scaledWidth = scale(component, width);
			int scaledHeight = scale(component, height);
			Image image = sprite.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
			return new ImageIcon(image);
		} catch (IOException exception) {
			return scaledIcon(component, ResourcePaths.carSpritePath(modelIndex), width, height);
		}
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
