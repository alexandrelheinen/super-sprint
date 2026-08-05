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

	private UiScale() {
	}

	public static Dimension quarterScreenSize() {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension(Math.max(640, screen.width / 2), Math.max(480, screen.height / 2));
	}

	public static float scaleFactor(Component component) {
		return Math.min(
				component.getWidth() / (float) REFERENCE_WIDTH,
				component.getHeight() / (float) REFERENCE_HEIGHT);
	}

	public static int scale(Component component, int value) {
		return Math.round(value * scaleFactor(component));
	}

	public static Font scaledFont(Component component, Font baseFont) {
		float factor = scaleFactor(component);
		return baseFont.deriveFont(Math.max(baseFont.getSize2D() * factor, 11f));
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
		frame.setMinimumSize(new Dimension(size.width / 2, size.height / 2));
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
