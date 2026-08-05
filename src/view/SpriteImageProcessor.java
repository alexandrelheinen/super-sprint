package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class SpriteImageProcessor {

	private static final int BLACK_THRESHOLD = 32;

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
				int red = (rgb >> 16) & 0xFF;
				int green = (rgb >> 8) & 0xFF;
				int blue = rgb & 0xFF;
				if (red <= BLACK_THRESHOLD && green <= BLACK_THRESHOLD && blue <= BLACK_THRESHOLD) {
					continue;
				}
				processed.setRGB(x, y, (0xFF << 24) | (red << 16) | (green << 8) | blue);
			}
		}
		graphics.dispose();
		return processed;
	}
}
