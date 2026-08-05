package view.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import view.ui.UiPainter;

public class GlassCard extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int CORNER_ARC = 20;
	private static final int CARD_PADDING = 14;
	private static final int TITLE_CLEARANCE = 8;

	public GlassCard(LayoutManager layout, Component context, String title) {
		super(layout != null ? layout : new BorderLayout());
		setOpaque(false);
		setBorder(new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
		if (title != null && !title.isEmpty()) {
			setBorder(javax.swing.BorderFactory.createCompoundBorder(
					ThemedPanel.sectionBorder(title, context),
					new EmptyBorder(TITLE_CLEARANCE, CARD_PADDING, CARD_PADDING, CARD_PADDING)));
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		UiPainter.paintGlassSurface(graphics2D, 0, 0, getWidth(), getHeight(), CORNER_ARC);
		graphics2D.dispose();
		super.paintComponent(graphics);
	}
}
