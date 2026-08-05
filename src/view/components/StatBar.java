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

public class StatBar extends JComponent {

	private static final long serialVersionUID = 1L;
	private static final int BAR_HEIGHT = 18;
	private static final int CORNER_ARC = 10;

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

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(super.getPreferredSize().width, UiScale.scale(context, BAR_HEIGHT + 18));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.enableQuality(graphics2D);
		Font font = GameTheme.scaled(GameTheme.FONT_BODY, context);
		graphics2D.setFont(font);
		graphics2D.setColor(GameTheme.TEXT_MUTED);
		graphics2D.drawString(label, 0, UiScale.scale(context, 12));

		int barY = UiScale.scale(context, 16);
		int barHeight = UiScale.scale(context, BAR_HEIGHT);
		int barWidth = getWidth();
		graphics2D.setColor(GameTheme.BACKGROUND_DARK);
		graphics2D.fillRoundRect(0, barY, barWidth, barHeight, CORNER_ARC, CORNER_ARC);

		double ratio = (value - minimum) / (maximum - minimum);
		int fillWidth = Math.max(UiScale.scale(context, 6), (int) (barWidth * ratio));
		graphics2D.setPaint(new GradientPaint(
				0,
				barY,
				GameTheme.ACCENT_BLUE_BRIGHT,
				fillWidth,
				barY,
				GameTheme.ACCENT_BLUE));
		graphics2D.fillRoundRect(0, barY, fillWidth, barHeight, CORNER_ARC, CORNER_ARC);

		graphics2D.setColor(GameTheme.TEXT_PRIMARY);
		String valueText = formatValue(value) + valueSuffix;
		FontMetrics metrics = graphics2D.getFontMetrics();
		graphics2D.drawString(valueText, barWidth - metrics.stringWidth(valueText), barY + barHeight - 4);
		graphics2D.dispose();
	}

	private static String formatValue(double value) {
		if (Math.rint(value) == value) {
			return Integer.toString((int) value);
		}
		return String.format("%.1f", value);
	}
}
