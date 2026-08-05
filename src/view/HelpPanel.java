package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import model.ConfigLoader;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.KeyboardPanel;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

/**
 * Help screen content hosted inside {@link AppShell}.
 */
public class HelpPanel extends BackgroundPanel {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = ConfigLoader.getString("messages.help.dialog.title", "Help");
	private static final String CONTROLS_TITLE = ConfigLoader.getString("messages.help.controls.title", "Controls");
	private static final String PLAYER_ONE = ConfigLoader.getString("messages.help.player.one", "Player 1");
	private static final String PLAYER_TWO = ConfigLoader.getString("messages.help.player.two", "Player 2");
	private static final String INFO_TITLE = ConfigLoader.getString("messages.help.info.title", "Information");
	private static final String INFO_BODY = ConfigLoader.getMessage(
			"messages.help.info.body",
			"Software Project 2014/2015 — Sequence 6\nVersion 14.01.14\n\nConfigure laps in Race Setup before launching the grid.\nFinish first to enter the Hall of Fame, ranked by mean time per lap.");
	private static final String CLOSE = ConfigLoader.getString("messages.help.button.close", "Back to Menu");

	private static final int PANEL_INSET = 22;
	private static final int SECTION_GAP = 16;
	private static final int PLAYER_LABEL_GAP = 8;
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 54;

	private final ArcadeButton closeButton;
	private final JTextArea infoArea;

	public HelpPanel(Component scaleContext, Runnable onClose) {
		super(BackgroundPanel.Style.SCREEN);
		setLayout(new BorderLayout(0, SECTION_GAP));
		setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		add(ThemedPanel.createHeader(TITLE, null, scaleContext), BorderLayout.NORTH);

		GlassCard controlsCard = new GlassCard(new BorderLayout(), scaleContext, CONTROLS_TITLE);
		controlsCard.setOpaque(false);

		JPanel keyboardRow = new JPanel(new GridLayout(1, 2, SECTION_GAP, 0));
		keyboardRow.setOpaque(false);
		keyboardRow.add(buildPlayerPanel(scaleContext, PLAYER_ONE, KeyboardPanel.Layout.ARROWS));
		keyboardRow.add(buildPlayerPanel(scaleContext, PLAYER_TWO, KeyboardPanel.Layout.WASD));
		controlsCard.add(keyboardRow, BorderLayout.CENTER);
		add(controlsCard, BorderLayout.CENTER);

		GlassCard infoCard = new GlassCard(new BorderLayout(), scaleContext, INFO_TITLE);
		infoCard.setOpaque(false);
		infoArea = new JTextArea(INFO_BODY);
		infoArea.setEditable(false);
		infoArea.setOpaque(false);
		infoArea.setLineWrap(true);
		infoArea.setWrapStyleWord(true);
		infoArea.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, scaleContext));
		infoArea.setForeground(GameTheme.TEXT_PRIMARY);
		infoArea.setBorder(new EmptyBorder(8, 4, 8, 4));
		infoCard.add(infoArea, BorderLayout.CENTER);

		closeButton = new ArcadeButton(CLOSE, false);
		closeButton.addActionListener(event -> onClose.run());
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(closeButton, BorderLayout.EAST);

		JPanel south = new JPanel(new BorderLayout(0, SECTION_GAP));
		south.setOpaque(false);
		south.add(infoCard, BorderLayout.CENTER);
		south.add(footer, BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);
	}

	public void applyScaledMetrics(Component scaleContext) {
		closeButton.applyScaledSize(scaleContext, BUTTON_WIDTH, BUTTON_HEIGHT);
		infoArea.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, scaleContext));
		revalidate();
		repaint();
	}

	private static JPanel buildPlayerPanel(Component owner, String playerLabel, KeyboardPanel.Layout layout) {
		JPanel panel = new JPanel(new BorderLayout(0, PLAYER_LABEL_GAP));
		panel.setOpaque(false);
		JLabel label = ThemedPanel.createLabel(playerLabel, owner);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, owner));
		panel.add(label, BorderLayout.NORTH);
		JPanel keyboardHolder = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
		keyboardHolder.setOpaque(false);
		keyboardHolder.add(new KeyboardPanel(owner, layout));
		panel.add(keyboardHolder, BorderLayout.CENTER);
		return panel;
	}
}
