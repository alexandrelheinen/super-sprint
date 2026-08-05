package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Renders track menu previews by stroking a tile-accurate outline built from
 * constrained cubic Bézier segments, over shared procedural scenery.
 */
public final class TrackPreviewRenderer {

	private static final float TRACK_STROKE = 5f;
	private static final float TRACK_ASPHALT_STROKE = 7.5f;
	private static final float PADDING_RATIO = 0.12f;
	private static final int CORNER_ARC = 16;
	private static final int BORDER_ARC = 14;
	private static final Color ASPHALT = new Color(48, 52, 58);
	private static final int TREE_COUNT = 7;

	private TrackPreviewRenderer() {
	}

	public static BufferedImage render(int[][] trackMap, int width, int height) {
		Path2D trackPath = TrackPreviewPathBuilder.buildTrackPath(trackMap);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		UiPainter.enableQuality(graphics);

		Shape frame = new RoundRectangle2D.Float(0, 0, width, height, CORNER_ARC, CORNER_ARC);
		graphics.setClip(frame);

		int seed = RaceSceneryPainter.seedFor(trackMap);
		RaceSceneryPainter.paintGrassField(graphics, width, height);
		RaceSceneryPainter.paintGrassPatches(graphics, width, height, seed ^ 0x9E3779B9);

		Rectangle2D bounds = trackPath.getBounds2D();
		double spanX = Math.max(bounds.getWidth(), 1e-3);
		double spanY = Math.max(bounds.getHeight(), 1e-3);
		double paddingX = width * PADDING_RATIO;
		double paddingY = height * PADDING_RATIO;
		double drawableWidth = width - paddingX * 2.0;
		double drawableHeight = height - paddingY * 2.0;
		double scale = Math.min(drawableWidth / spanX, drawableHeight / spanY);
		double alignX = paddingX + (drawableWidth - spanX * scale) / 2.0;
		double alignY = paddingY + (drawableHeight - spanY * scale) / 2.0;

		AffineTransform trackTransform = new AffineTransform();
		trackTransform.translate(alignX - bounds.getX() * scale, alignY - bounds.getY() * scale);
		trackTransform.scale(scale, scale);

		Shape trackInScreen = trackTransform.createTransformedShape(trackPath);
		Shape trackBand = new BasicStroke(
				TRACK_ASPHALT_STROKE,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND).createStrokedShape(trackInScreen);

		RaceSceneryPainter.paintTrees(
				graphics,
				width,
				height,
				trackBand,
				seed ^ 0xA5A5A5A5,
				TREE_COUNT,
				0.09,
				0.17);

		graphics.setStroke(new BasicStroke(TRACK_ASPHALT_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(ASPHALT);
		graphics.draw(trackInScreen);
		graphics.setStroke(new BasicStroke(TRACK_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(GameTheme.ACCENT_BLUE_BRIGHT);
		graphics.draw(trackInScreen);

		graphics.setClip(null);
		graphics.setColor(GameTheme.ACCENT_YELLOW);
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawRoundRect(1, 1, width - 2, height - 2, BORDER_ARC, BORDER_ARC);
		graphics.dispose();
		return image;
	}
}
