package view.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import javax.swing.JComponent;

import view.UiScale;
import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Soft keyboard key. Letter labels are drawn as text; the four arrow
 * directions are drawn as filled triangles so they render correctly even when
 * the UI font has no arrow glyphs (common on Linux).
 */
public class KeyCap extends JComponent {

	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_SIZE = 56;
	private static final int CORNER_ARC = 10;
	private static final double ARROW_INSET_RATIO = 0.28;

	public enum Glyph {
		LETTER,
		ARROW_UP,
		ARROW_DOWN,
		ARROW_LEFT,
		ARROW_RIGHT
	}

	private final Component context;
	private final String label;
	private final Glyph glyph;
	private Dimension forcedSize;

	public KeyCap(Component context, String label) {
		this(context, label, Glyph.LETTER);
	}

	public KeyCap(Component context, String label, Glyph glyph) {
		this.context = context;
		this.label = label;
		this.glyph = glyph;
		setOpaque(false);
	}

	public void forceSize(int size) {
		forcedSize = new Dimension(size, size);
	}

	public static KeyCap arrowUp(Component context) {
		return new KeyCap(context, "Up", Glyph.ARROW_UP);
	}

	public static KeyCap arrowDown(Component context) {
		return new KeyCap(context, "Down", Glyph.ARROW_DOWN);
	}

	public static KeyCap arrowLeft(Component context) {
		return new KeyCap(context, "Left", Glyph.ARROW_LEFT);
	}

	public static KeyCap arrowRight(Component context) {
		return new KeyCap(context, "Right", Glyph.ARROW_RIGHT);
	}

	@Override
	public Dimension getPreferredSize() {
		if (forcedSize != null) {
			return new Dimension(forcedSize);
		}
		int size = UiScale.scale(context, DEFAULT_SIZE);
		return new Dimension(size, size);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);
		graphics2D.setPaint(new GradientPaint(
				0,
				0,
				GameTheme.ACCENT_BLUE_BRIGHT,
				0,
				getHeight(),
				GameTheme.ACCENT_BLUE));
		graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_ARC, CORNER_ARC);
		graphics2D.setColor(GameTheme.GLASS_BORDER);
		graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_ARC, CORNER_ARC);

		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		if (glyph == Glyph.LETTER) {
			Font font = GameTheme.scaled(GameTheme.FONT_BUTTON, context);
			graphics2D.setFont(font);
			var metrics = graphics2D.getFontMetrics();
			int textX = (getWidth() - metrics.stringWidth(label)) / 2;
			int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			graphics2D.drawString(label, textX, textY);
		} else {
			graphics2D.fill(arrowPath());
		}
		graphics2D.dispose();
	}

	private Path2D arrowPath() {
		double inset = Math.min(getWidth(), getHeight()) * ARROW_INSET_RATIO;
		double left = inset;
		double top = inset;
		double right = getWidth() - inset;
		double bottom = getHeight() - inset;
		double midX = getWidth() / 2.0;
		double midY = getHeight() / 2.0;

		Path2D path = new Path2D.Double();
		switch (glyph) {
			case ARROW_UP -> {
				path.moveTo(midX, top);
				path.lineTo(right, bottom);
				path.lineTo(left, bottom);
			}
			case ARROW_DOWN -> {
				path.moveTo(left, top);
				path.lineTo(right, top);
				path.lineTo(midX, bottom);
			}
			case ARROW_LEFT -> {
				path.moveTo(left, midY);
				path.lineTo(right, top);
				path.lineTo(right, bottom);
			}
			case ARROW_RIGHT -> {
				path.moveTo(left, top);
				path.lineTo(right, midY);
				path.lineTo(left, bottom);
			}
			default -> {
			}
		}
		path.closePath();
		return path;
	}
}
