package view.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import view.theme.GameTheme;

public class ThemedPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int SECTION_BORDER_WIDTH = 2;
	private static final int HEADER_SUBTITLE_GAP = 6;
	private static final int SECTION_TITLE_TOP_INSET = 10;
	private static final int SECTION_TITLE_SIDE_INSET = 8;

	public ThemedPanel() {
		setOpaque(true);
		setBackground(GameTheme.PANEL_SURFACE);
	}

	public static Border sectionBorder(String title, Component context) {
		TitledBorder border = BorderFactory.createTitledBorder(
				BorderFactory.createEmptyBorder(
						SECTION_TITLE_TOP_INSET,
						SECTION_TITLE_SIDE_INSET,
						4,
						SECTION_TITLE_SIDE_INSET),
				BorderFactory.createLineBorder(GameTheme.BORDER_SOFT, SECTION_BORDER_WIDTH),
				title,
				TitledBorder.LEFT,
				TitledBorder.ABOVE_TOP,
				GameTheme.scaled(GameTheme.FONT_SUBTITLE, context),
				GameTheme.ACCENT_YELLOW);
		return border;
	}

	public static JLabel createHeading(String text, Component context) {
		JLabel label = new JLabel(text, JLabel.CENTER);
		label.setForeground(GameTheme.TEXT_PRIMARY);
		label.setFont(GameTheme.scaled(GameTheme.FONT_TITLE, context));
		return label;
	}

	public static JLabel createLabel(String text, Component context) {
		JLabel label = new JLabel(text);
		label.setForeground(GameTheme.TEXT_PRIMARY);
		label.setFont(GameTheme.scaled(GameTheme.FONT_BODY, context));
		return label;
	}

	public static JPanel createHeader(String title, String subtitle, Component context) {
		JPanel header = new JPanel(new BorderLayout(0, HEADER_SUBTITLE_GAP));
		header.setOpaque(false);
		header.add(createHeading(title, context), BorderLayout.NORTH);
		if (subtitle != null && !subtitle.isEmpty()) {
			JLabel subtitleLabel = createLabel(subtitle, context);
			subtitleLabel.setHorizontalAlignment(JLabel.CENTER);
			subtitleLabel.setForeground(GameTheme.TEXT_MUTED);
			header.add(subtitleLabel, BorderLayout.SOUTH);
		}
		return header;
	}

	public void styleSurface(Color background) {
		setBackground(background);
	}
}
