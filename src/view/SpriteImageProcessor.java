package view;

import java.awt.image.BufferedImage;

public final class SpriteImageProcessor {

	private static final int ALPHA_OPAQUE_SHIFT = 24;
	private static final int RED_CHANNEL_SHIFT = 16;
	private static final int GREEN_CHANNEL_SHIFT = 8;
	private static final int CHANNEL_MASK = 0xFF;
	private static final int FULL_ALPHA = 0xFF;
	private static final int TRANSPARENT_ALPHA = 0x00;
	private static final int COLOR_MATCH_TOLERANCE = 36;

	private SpriteImageProcessor() {
	}

	public static BufferedImage normalizeCarSprite(BufferedImage source) {
		if (source == null) {
			return null;
		}

		int keyRed;
		int keyGreen;
		int keyBlue;
		if (source.getColorModel().hasAlpha()) {
			int keyRgb = source.getRGB(0, 0);
			keyRed = (keyRgb >> RED_CHANNEL_SHIFT) & CHANNEL_MASK;
			keyGreen = (keyRgb >> GREEN_CHANNEL_SHIFT) & CHANNEL_MASK;
			keyBlue = keyRgb & CHANNEL_MASK;
		} else {
			int keyRgb = source.getRGB(0, 0);
			keyRed = (keyRgb >> RED_CHANNEL_SHIFT) & CHANNEL_MASK;
			keyGreen = (keyRgb >> GREEN_CHANNEL_SHIFT) & CHANNEL_MASK;
			keyBlue = keyRgb & CHANNEL_MASK;
		}

		BufferedImage processed = new BufferedImage(
				source.getWidth(),
				source.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int rgb = source.getRGB(x, y);
				int red = (rgb >> RED_CHANNEL_SHIFT) & CHANNEL_MASK;
				int green = (rgb >> GREEN_CHANNEL_SHIFT) & CHANNEL_MASK;
				int blue = rgb & CHANNEL_MASK;
				int alpha = (rgb >> ALPHA_OPAQUE_SHIFT) & CHANNEL_MASK;

				if (source.getColorModel().hasAlpha() && alpha == TRANSPARENT_ALPHA) {
					continue;
				}
				if (matchesKeyColor(red, green, blue, keyRed, keyGreen, keyBlue)) {
					continue;
				}

				processed.setRGB(
						x,
						y,
						(FULL_ALPHA << ALPHA_OPAQUE_SHIFT) | (red << RED_CHANNEL_SHIFT) | (green << GREEN_CHANNEL_SHIFT) | blue);
			}
		}
		return trimTransparentBorder(processed);
	}

	/**
	 * Crops fully transparent margins so collision bounds match visible pixels.
	 */
	public static BufferedImage trimTransparentBorder(BufferedImage source) {
		if (source == null) {
			return null;
		}

		int minX = source.getWidth();
		int minY = source.getHeight();
		int maxX = -1;
		int maxY = -1;

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int alpha = (source.getRGB(x, y) >> ALPHA_OPAQUE_SHIFT) & CHANNEL_MASK;
				if (alpha == TRANSPARENT_ALPHA) {
					continue;
				}
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}

		if (maxX < minX || maxY < minY) {
			return source;
		}

		int width = maxX - minX + 1;
		int height = maxY - minY + 1;
		BufferedImage trimmed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				trimmed.setRGB(x, y, source.getRGB(minX + x, minY + y));
			}
		}
		return trimmed;
	}

	private static boolean matchesKeyColor(
			int red,
			int green,
			int blue,
			int keyRed,
			int keyGreen,
			int keyBlue) {
		int delta = Math.abs(red - keyRed) + Math.abs(green - keyGreen) + Math.abs(blue - keyBlue);
		return delta <= COLOR_MATCH_TOLERANCE;
	}
}
