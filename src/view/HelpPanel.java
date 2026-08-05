package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import model.ConfigLoader;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.KeyboardPanel;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

/**
 * Help screen: Player 1 / Player 2 keyboards sit side by side above an enriched
 * How to play guide.
 */
public class HelpPanel extends BackgroundPanel {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = ConfigLoader.getString("messages.help.dialog.title", "Help");
	private static final String CONTROLS_TITLE = ConfigLoader.getString("messages.help.controls.title", "Controls");
	private static final String PLAYER_ONE = ConfigLoader.getString("messages.help.player.one", "Player 1");
	private static final String PLAYER_TWO = ConfigLoader.getString("messages.help.player.two", "Player 2");
	private static final String LEGEND = ConfigLoader.getString(
			"messages.help.controls.legend",
			"↑ / W accelerate   ·   ↓ / S brake   ·   ← → / A D steer");
	private static final String INFO_TITLE = ConfigLoader.getString("messages.help.info.title", "How to play");
	private static final String CLOSE = ConfigLoader.getString("messages.help.button.close", "Back to Menu");
	private static final String INFO_BODY = buildInfoBody();

	private static final int PANEL_INSET = 14;
	private static final int SECTION_GAP = 8;
	private static final int PLAYER_LABEL_GAP = 2;
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 54;
	private static final int SCROLL_UNIT = 16;
	private static final int HELP_KEY_SIZE = 28;

	private final ArcadeButton closeButton;
	private final JPanel keyboardRow;
	private final JLabel legendLabel;
	private final JTextArea infoArea;

	public HelpPanel(Component scaleContext, Runnable onClose) {
		super(BackgroundPanel.Style.SCREEN);
		setLayout(new BorderLayout(0, SECTION_GAP));
		setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		add(ThemedPanel.createHeader(TITLE, null, scaleContext), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, SECTION_GAP));
		body.setOpaque(false);

		JPanel controlsCard = new JPanel(new BorderLayout(0, 4));
		controlsCard.setOpaque(false);
		controlsCard.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				ThemedPanel.sectionBorder(CONTROLS_TITLE, scaleContext),
				new EmptyBorder(4, 10, 8, 10)));

		keyboardRow = new JPanel(new GridLayout(1, 2, 20, 0));
		keyboardRow.setOpaque(false);
		rebuildKeyboards(scaleContext);
		controlsCard.add(keyboardRow, BorderLayout.CENTER);

		legendLabel = ThemedPanel.createLabel(LEGEND, scaleContext);
		legendLabel.setHorizontalAlignment(JLabel.CENTER);
		legendLabel.setForeground(GameTheme.TEXT_MUTED);
		applyLegendFont(scaleContext);
		JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		legendRow.setOpaque(false);
		legendRow.add(legendLabel);
		controlsCard.add(legendRow, BorderLayout.SOUTH);
		body.add(controlsCard, BorderLayout.NORTH);

		GlassCard infoCard = new GlassCard(new BorderLayout(), scaleContext, INFO_TITLE);
		infoCard.setOpaque(false);
		infoArea = new JTextArea(INFO_BODY);
		infoArea.setEditable(false);
		infoArea.setOpaque(false);
		infoArea.setFocusable(false);
		infoArea.setLineWrap(true);
		infoArea.setWrapStyleWord(true);
		infoArea.setFont(scaledInfoFont(scaleContext));
		infoArea.setForeground(GameTheme.TEXT_PRIMARY);
		infoArea.setBorder(new EmptyBorder(6, 10, 12, 10));
		infoCard.add(wrapScroll(infoArea), BorderLayout.CENTER);
		body.add(infoCard, BorderLayout.CENTER);

		add(body, BorderLayout.CENTER);

		closeButton = new ArcadeButton(CLOSE, false);
		closeButton.addActionListener(event -> onClose.run());
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(closeButton, BorderLayout.EAST);
		add(footer, BorderLayout.SOUTH);
	}

	public void applyScaledMetrics(Component scaleContext) {
		closeButton.applyScaledSize(scaleContext, BUTTON_WIDTH, BUTTON_HEIGHT);
		infoArea.setFont(scaledInfoFont(scaleContext));
		applyLegendFont(scaleContext);
		rebuildKeyboards(scaleContext);
		revalidate();
		repaint();
	}

	private void rebuildKeyboards(Component scaleContext) {
		keyboardRow.removeAll();
		keyboardRow.add(buildPlayerPanel(scaleContext, PLAYER_ONE, KeyboardPanel.Layout.ARROWS));
		keyboardRow.add(buildPlayerPanel(scaleContext, PLAYER_TWO, KeyboardPanel.Layout.WASD));
		keyboardRow.revalidate();
	}

	private void applyLegendFont(Component context) {
		Font body = GameTheme.scaled(GameTheme.FONT_BODY, context);
		float size = Math.max(11f, body.getSize2D() * 0.68f);
		legendLabel.setFont(body.deriveFont(Font.PLAIN, size));
	}

	private static Font scaledInfoFont(Component context) {
		Font body = GameTheme.scaled(GameTheme.FONT_BODY, context);
		float size = Math.max(13f, body.getSize2D() * 0.8f);
		return body.deriveFont(Font.PLAIN, size);
	}

	private static String buildInfoBody() {
		return section(
				"messages.help.info.goal.title",
				"Goal",
				"messages.help.info.goal.body",
				"Be first to finish the chosen lap count on a modular top-down track.")
				+ "\n\n"
				+ section(
						"messages.help.info.setup.title",
						"Race setup",
						"messages.help.info.setup.body",
						"Pick Single Player or Multiplayer, then choose car model(s), track, and laps.")
				+ "\n\n"
				+ section(
						"messages.help.info.driving.title",
						"On the track",
						"messages.help.info.driving.body",
						"Stay on the asphalt — leaving the lane cuts speed. Cross the yellow finish line to count a lap.")
				+ "\n\n"
				+ section(
						"messages.help.info.collisions.title",
						"Bumps",
						"messages.help.info.collisions.body",
						"Cars can nudge each other. Contact briefly mixes speed and heading.")
				+ "\n\n"
				+ section(
						"messages.help.info.scoring.title",
						"Winning & Hall of Fame",
						"messages.help.info.scoring.body",
						"First to finish wins. Hall of Fame ranks by mean lap time.")
				+ "\n\n"
				+ ConfigLoader.getString(
						"messages.help.info.project",
						"Software Project 2014/2015 — Sequence 6 (Supélec). English UI in a single-window shell.");
	}

	private static String section(String titleKey, String titleFallback, String bodyKey, String bodyFallback) {
		return ConfigLoader.getString(titleKey, titleFallback)
				+ "\n"
				+ ConfigLoader.getMessage(bodyKey, bodyFallback);
	}

	private static JScrollPane wrapScroll(Component view) {
		JScrollPane scrollPane = new JScrollPane(view);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
		return scrollPane;
	}

	private static JPanel buildPlayerPanel(Component owner, String playerLabel, KeyboardPanel.Layout layout) {
		JPanel panel = new JPanel(new BorderLayout(0, PLAYER_LABEL_GAP));
		panel.setOpaque(false);
		JLabel label = ThemedPanel.createLabel(playerLabel, owner);
		label.setHorizontalAlignment(JLabel.CENTER);
		Font labelFont = GameTheme.scaled(GameTheme.FONT_BODY, owner);
		label.setFont(labelFont.deriveFont(Font.BOLD, Math.max(12f, labelFont.getSize2D() * 0.78f)));
		panel.add(label, BorderLayout.NORTH);
		JPanel keyboardHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		keyboardHolder.setOpaque(false);
		keyboardHolder.add(new KeyboardPanel(owner, layout, HELP_KEY_SIZE));
		panel.add(keyboardHolder, BorderLayout.CENTER);
		return panel;
	}
}
