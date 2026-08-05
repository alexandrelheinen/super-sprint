package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class SpriteImageProcessor {

	private static final int BLACK_THRESHOLD = 32;
	private static final int ALPHA_OPAQUE_SHIFT = 24;
	private static final int RED_CHANNEL_SHIFT = 16;
	private static final int GREEN_CHANNEL_SHIFT = 8;
	private static final int CHANNEL_MASK = 0xFF;
	private static final int FULL_ALPHA = 0xFF;

	private SpriteImageProcessor() {
	}

	public static BufferedImage normalizeCarSprite(BufferedImage source) {
		if (source == null) {
			return null;
		}
		BufferedImage processed = new BufferedImage(
				source.getWidth(),
				source.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = processed.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, processed.getWidth(), processed.getHeight());

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int rgb = source.getRGB(x, y);
				int red = (rgb >> RED_CHANNEL_SHIFT) & CHANNEL_MASK;
				int green = (rgb >> GREEN_CHANNEL_SHIFT) & CHANNEL_MASK;
				int blue = rgb & CHANNEL_MASK;
				if (red <= BLACK_THRESHOLD && green <= BLACK_THRESHOLD && blue <= BLACK_THRESHOLD) {
					continue;
				}
				processed.setRGB(
						x,
						y,
						(FULL_ALPHA << ALPHA_OPAQUE_SHIFT) | (red << RED_CHANNEL_SHIFT) | (green << GREEN_CHANNEL_SHIFT) | blue);
			}
		}
		graphics.dispose();
		return processed;
	}
}
