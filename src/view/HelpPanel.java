package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.Box;
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
 * Help screen: player keyboards sit side by side above the information text.
 */
public class HelpPanel extends BackgroundPanel {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_INFO_BODY =
			"Super Sprint Supélec is a top-down arcade racer on modular tracks.\n\n"
					+ "Race setup\n"
					+ "• Choose Single Player or Multiplayer from the main menu.\n"
					+ "• Pick a car model, track, and lap count before starting.\n"
					+ "• Empty grid slots are filled by AI opponents.\n\n"
					+ "On the track\n"
					+ "• Stay on the asphalt — leaving the lane cuts your speed.\n"
					+ "• Cross the yellow finish line in the racing direction to count laps.\n"
					+ "• Cars can bump each other; contact mixes speed and heading briefly.\n\n"
					+ "Winning & Hall of Fame\n"
					+ "• First to complete the chosen lap count wins.\n"
					+ "• Human winners who place can save a name to the Hall of Fame.\n"
					+ "• Rankings use mean lap time (total time ÷ laps), so different race lengths stay comparable.\n\n"
					+ "Project\n"
					+ "Software Project 2014/2015 — Sequence 6 (Supélec). Maintained with English UI and a single-window shell.";

	private static final String TITLE = ConfigLoader.getString("messages.help.dialog.title", "Help");
	private static final String CONTROLS_TITLE = ConfigLoader.getString("messages.help.controls.title", "Controls");
	private static final String PLAYER_ONE = ConfigLoader.getString("messages.help.player.one", "Player 1");
	private static final String PLAYER_TWO = ConfigLoader.getString("messages.help.player.two", "Player 2");
	private static final String INFO_TITLE = ConfigLoader.getString("messages.help.info.title", "How to play");
	private static final String INFO_BODY = ConfigLoader.getMessage("messages.help.info.body", DEFAULT_INFO_BODY);
	private static final String CLOSE = ConfigLoader.getString("messages.help.button.close", "Back to Menu");

	private static final int PANEL_INSET = 18;
	private static final int SECTION_GAP = 12;
	private static final int PLAYER_LABEL_GAP = 6;
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 54;
	private static final int SCROLL_UNIT = 16;
	private static final int HELP_KEY_SIZE = 36;

	private final ArcadeButton closeButton;
	private final JTextArea infoArea;
	private final JPanel keyboardRow;

	public HelpPanel(Component scaleContext, Runnable onClose) {
		super(BackgroundPanel.Style.SCREEN);
		setLayout(new BorderLayout(0, SECTION_GAP));
		setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		add(ThemedPanel.createHeader(TITLE, null, scaleContext), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, SECTION_GAP));
		body.setOpaque(false);

		GlassCard controlsCard = new GlassCard(new BorderLayout(), scaleContext, CONTROLS_TITLE);
		controlsCard.setOpaque(false);
		keyboardRow = new JPanel(new GridLayout(1, 2, SECTION_GAP, 0));
		keyboardRow.setOpaque(false);
		rebuildKeyboards(scaleContext);
		controlsCard.add(keyboardRow, BorderLayout.CENTER);
		body.add(controlsCard, BorderLayout.NORTH);

		GlassCard infoCard = new GlassCard(new BorderLayout(), scaleContext, INFO_TITLE);
		infoCard.setOpaque(false);
		infoArea = new JTextArea(INFO_BODY);
		infoArea.setEditable(false);
		infoArea.setOpaque(false);
		infoArea.setFocusable(false);
		infoArea.setLineWrap(true);
		infoArea.setWrapStyleWord(true);
		infoArea.setRows(Math.min(16, INFO_BODY.split("\n", -1).length + 2));
		infoArea.setFont(GameTheme.scaled(GameTheme.FONT_BODY, scaleContext));
		infoArea.setForeground(GameTheme.TEXT_PRIMARY);
		infoArea.setBorder(new EmptyBorder(8, 10, 14, 10));
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
		infoArea.setFont(GameTheme.scaled(GameTheme.FONT_BODY, scaleContext));
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
		label.setFont(GameTheme.scaled(GameTheme.FONT_BODY, owner));
		panel.add(label, BorderLayout.NORTH);
		JPanel keyboardHolder = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
		keyboardHolder.setOpaque(false);
		keyboardHolder.add(new KeyboardPanel(owner, layout, HELP_KEY_SIZE));
		panel.add(keyboardHolder, BorderLayout.CENTER);
		panel.add(Box.createVerticalStrut(4), BorderLayout.SOUTH);
		return panel;
	}
}
