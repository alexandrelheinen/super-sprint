package view.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import model.ConfigLoader;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.KeyboardPanel;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

public class HelpDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = ConfigLoader.getString("messages.help.dialog.title", "Controls & Information");
	private static final String SUBTITLE = ConfigLoader.getString(
			"messages.help.dialog.subtitle",
			"Semi-pro arcade layout — memorize your racing lines and inputs");
	private static final String PLAYER_ONE = ConfigLoader.getString("messages.help.player.one", "Player 1 — Steering");
	private static final String PLAYER_TWO = ConfigLoader.getString("messages.help.player.two", "Player 2 — Steering");
	private static final String INFO_TITLE = ConfigLoader.getString("messages.help.info.title", "Race Briefing");
	private static final String INFO_BODY = ConfigLoader.getMessage(
			"messages.help.info.body",
			"Software Project 2014/2015 — Sequence 6\nVersion 14.01.14\n\nConfigure laps in Race Setup before launching the grid.\nFinish first within the lap limit to enter the Hall of Fame.");
	private static final String CLOSE = ConfigLoader.getString("messages.help.button.close", "Back to Menu");

	private static final int PANEL_INSET = 22;
	private static final int SECTION_GAP = 16;
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 48;

	public HelpDialog(Component owner) {
		super(javax.swing.SwingUtilities.getWindowAncestor(owner), TITLE, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		BackgroundPanel root = new BackgroundPanel(BackgroundPanel.Style.SCREEN);
		root.setLayout(new BorderLayout(0, SECTION_GAP));
		root.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		root.add(ThemedPanel.createHeader(TITLE, SUBTITLE, owner), BorderLayout.NORTH);

		JPanel keyboardRow = new JPanel(new GridLayout(1, 2, SECTION_GAP, 0));
		keyboardRow.setOpaque(false);
		keyboardRow.add(buildKeyboardCard(owner, PLAYER_ONE, KeyboardPanel.Layout.ARROWS));
		keyboardRow.add(buildKeyboardCard(owner, PLAYER_TWO, KeyboardPanel.Layout.WASD));
		root.add(keyboardRow, BorderLayout.CENTER);

		GlassCard infoCard = new GlassCard(new BorderLayout(), owner, INFO_TITLE);
		infoCard.setOpaque(false);
		javax.swing.JTextArea infoArea = new javax.swing.JTextArea(INFO_BODY);
		infoArea.setEditable(false);
		infoArea.setOpaque(false);
		infoArea.setLineWrap(true);
		infoArea.setWrapStyleWord(true);
		infoArea.setFont(GameTheme.scaled(GameTheme.FONT_BODY, owner));
		infoArea.setForeground(GameTheme.TEXT_PRIMARY);
		infoCard.add(infoArea, BorderLayout.CENTER);

		ArcadeButton closeButton = new ArcadeButton(CLOSE, false);
		closeButton.applyScaledSize(owner, BUTTON_WIDTH, BUTTON_HEIGHT);
		closeButton.addActionListener(event -> dispose());
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(closeButton, BorderLayout.EAST);

		JPanel south = new JPanel(new BorderLayout(0, SECTION_GAP));
		south.setOpaque(false);
		south.add(infoCard, BorderLayout.CENTER);
		south.add(footer, BorderLayout.SOUTH);
		root.add(south, BorderLayout.SOUTH);

		setContentPane(root);
		pack();
		setLocationRelativeTo(owner);
	}

	private static GlassCard buildKeyboardCard(Component owner, String title, KeyboardPanel.Layout layout) {
		GlassCard card = new GlassCard(new BorderLayout(), owner, title);
		card.add(new KeyboardPanel(owner, layout), BorderLayout.CENTER);
		return card;
	}

	public void showDialog() {
		setVisible(true);
	}
}
