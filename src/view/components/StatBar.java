package view.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

import view.UiScale;
import view.theme.GameTheme;
import view.ui.UiPainter;

/**
 * Labeled horizontal bar for car stats. Height follows the scaled body font so
 * labels are never clipped at the top of the component.
 */
public class StatBar extends JComponent {

	private static final long serialVersionUID = 1L;
	private static final int BAR_HEIGHT = 22;
	private static final int LABEL_TO_BAR_GAP = 4;
	private static final int BOTTOM_PAD = 2;
	private static final int CORNER_ARC = 10;
	private static final int MIN_WIDTH = 120;
	private static final int VALUE_INSET = 8;

	private final Component context;
	private double minimum;
	private double maximum;
	private double value;
	private String label;
	private String valueSuffix = "";

	public StatBar(Component context) {
		this.context = context;
		setOpaque(false);
	}

	public void configure(String label, int minimum, int maximum) {
		configure(label, minimum, maximum, "");
	}

	public void configure(String label, double minimum, double maximum, String valueSuffix) {
		this.label = label;
		this.minimum = minimum;
		this.maximum = maximum;
		this.valueSuffix = valueSuffix == null ? "" : valueSuffix;
	}

	public void setValue(int value) {
		setValue((double) value);
	}

	public void setValue(double value) {
		this.value = Math.max(minimum, Math.min(maximum, value));
		repaint();
	}

	private Font labelFont() {
		return GameTheme.scaled(GameTheme.FONT_BODY, context);
	}

	private FontMetrics labelMetrics() {
		return context.getFontMetrics(labelFont());
	}

	private int contentHeight() {
		FontMetrics metrics = labelMetrics();
		return metrics.getAscent()
				+ metrics.getDescent()
				+ UiScale.scale(context, LABEL_TO_BAR_GAP)
				+ UiScale.scale(context, BAR_HEIGHT)
				+ UiScale.scale(context, BOTTOM_PAD);
	}

	@Override
	public Dimension getPreferredSize() {
		// Fixed size — do not call super.getPreferredSize()/getMinimumSize() here.
		// Those can recurse through BoxLayout when preferred size is unset.
		return new Dimension(MIN_WIDTH, contentHeight());
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(MIN_WIDTH, contentHeight());
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, contentHeight());
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);
		Font font = labelFont();
		graphics2D.setFont(font);
		FontMetrics metrics = graphics2D.getFontMetrics();

		int labelBaseline = metrics.getAscent();
		int barY = labelBaseline + metrics.getDescent() + UiScale.scale(context, LABEL_TO_BAR_GAP);
		int barHeight = UiScale.scale(context, BAR_HEIGHT);
		int barWidth = getWidth();

		graphics2D.setColor(GameTheme.TEXT_MUTED);
		if (label != null) {
			graphics2D.drawString(label, 0, labelBaseline);
		}

		graphics2D.setColor(GameTheme.BACKGROUND_DARK);
		graphics2D.fillRoundRect(0, barY, barWidth, barHeight, CORNER_ARC, CORNER_ARC);

		double ratio = maximum <= minimum ? 0.0 : (value - minimum) / (maximum - minimum);
		ratio = Math.max(0.0, Math.min(1.0, ratio));
		int fillWidth = Math.max(UiScale.scale(context, 6), (int) (barWidth * ratio));
		graphics2D.setPaint(new GradientPaint(
				0,
				barY,
				GameTheme.ACCENT_BLUE_BRIGHT,
				fillWidth,
				barY,
				GameTheme.ACCENT_BLUE));
		graphics2D.fillRoundRect(0, barY, fillWidth, barHeight, CORNER_ARC, CORNER_ARC);

		// Value sits in the bar so it never collides with the label in narrow columns.
		String valueText = formatValue(value) + valueSuffix;
		float valueSize = Math.max(12f, barHeight - UiScale.scale(context, 6));
		Font valueFont = font.deriveFont(Font.BOLD, valueSize);
		graphics2D.setFont(valueFont);
		FontMetrics valueMetrics = graphics2D.getFontMetrics();
		int valueX = barWidth - valueMetrics.stringWidth(valueText) - UiScale.scale(context, VALUE_INSET);
		int valueY = barY + (barHeight + valueMetrics.getAscent() - valueMetrics.getDescent()) / 2;
		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		graphics2D.drawString(valueText, Math.max(UiScale.scale(context, VALUE_INSET), valueX), valueY);
		graphics2D.dispose();
	}

	private static String formatValue(double value) {
		if (Math.rint(value) == value) {
			return Integer.toString((int) value);
		}
		return String.format("%.1f", value);
	}
}
