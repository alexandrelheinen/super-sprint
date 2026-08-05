package view.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import model.ConfigLoader;
import model.HallOfFame;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

/**
 * Modal race-finish screen: winner, total time, and leaderboard placement by
 * mean lap time. Human winners who place can enter a name before saving.
 */
public class RaceCompletionDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = ConfigLoader.getString("messages.race.complete.title", "Race Complete");
	private static final String WINNER_LABEL = ConfigLoader.getString("messages.race.complete.winner", "Winner");
	private static final String TIME_LABEL = ConfigLoader.getString("messages.race.complete.time", "Time");
	private static final String LAPS_LABEL = ConfigLoader.getString("messages.race.complete.laps", "Laps");
	private static final String MEAN_LABEL = ConfigLoader.getString("messages.race.complete.mean", "Mean lap time");
	private static final String NEW_RECORD_FORMAT = ConfigLoader.getString(
			"messages.race.complete.new.record",
			"New record — Leaderboard #%d: %s s/lap");
	private static final String PLACEMENT_FORMAT = ConfigLoader.getString(
			"messages.race.complete.placement",
			"Leaderboard #%d: %s s/lap");
	private static final String NO_PLACE = ConfigLoader.getString(
			"messages.race.complete.no.place",
			"Did not place on the leaderboard");
	private static final String COMPUTER_NAME = ConfigLoader.getString("messages.race.complete.computer", "Computer");
	private static final String PLAYER_PREFIX = ConfigLoader.getString(
			"messages.race.complete.player.prefix",
			"Player ");
	private static final String NAME_PROMPT = ConfigLoader.getString(
			"messages.race.complete.name.prompt",
			"Your name");
	private static final String DEFAULT_PLAYER = ConfigLoader.getString("messages.hall.default.player", "Player");
	private static final String CONTINUE_LABEL = ConfigLoader.getString("messages.race.complete.continue", "Continue");
	private static final String TIME_SUFFIX = ConfigLoader.getString("messages.hall.time.suffix", " s");

	private static final int PANEL_INSET = 22;
	private static final int SECTION_GAP = 16;
	private static final int ROW_GAP = 8;
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 54;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int MS_PER_SECOND = 1000;

	private final HallOfFame hallOfFame;
	private final double durationMs;
	private final int lapCount;
	private final int trackIndex;
	private final boolean humanWinner;
	private final int placementRank;
	private final JTextField nameField;

	public RaceCompletionDialog(
			Component owner,
			HallOfFame hallOfFame,
			int winnerIndex,
			int humanPlayerCount,
			double durationMs,
			int lapCount,
			int trackIndex) {
		super(javax.swing.SwingUtilities.getWindowAncestor(owner), TITLE, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.hallOfFame = hallOfFame;
		this.durationMs = durationMs;
		this.lapCount = lapCount;
		this.trackIndex = trackIndex;
		this.humanWinner = winnerIndex < humanPlayerCount;
		this.placementRank = hallOfFame.findPlacementRank(durationMs, lapCount, trackIndex);

		String winnerName = humanWinner
				? PLAYER_PREFIX + (winnerIndex + ONE_BASED_INDEX_OFFSET)
				: COMPUTER_NAME;
		boolean canSave = humanWinner && placementRank != HallOfFame.NO_PLACEMENT;

		BackgroundPanel root = new BackgroundPanel(BackgroundPanel.Style.SCREEN);
		root.setLayout(new BorderLayout(0, SECTION_GAP));
		root.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));
		root.add(ThemedPanel.createHeader(TITLE, null, owner), BorderLayout.NORTH);

		GlassCard summaryCard = new GlassCard(new BorderLayout(0, SECTION_GAP), owner, null);
		JPanel summary = new JPanel(new GridLayout(0, 1, 0, ROW_GAP));
		summary.setOpaque(false);
		summary.add(buildInfoRow(owner, WINNER_LABEL, winnerName));
		summary.add(buildInfoRow(owner, TIME_LABEL, formatSeconds(durationMs) + TIME_SUFFIX));
		summary.add(buildInfoRow(owner, LAPS_LABEL, Integer.toString(lapCount)));
		summary.add(buildInfoRow(owner, MEAN_LABEL, formatSeconds(durationMs / lapCount) + TIME_SUFFIX + "/lap"));

		JLabel placementLabel = ThemedPanel.createLabel(buildPlacementText(), owner);
		placementLabel.setForeground(
				placementRank != HallOfFame.NO_PLACEMENT ? GameTheme.ACCENT_YELLOW : GameTheme.TEXT_MUTED);
		placementLabel.setHorizontalAlignment(JLabel.CENTER);
		summary.add(placementLabel);
		summaryCard.add(summary, BorderLayout.CENTER);
		root.add(summaryCard, BorderLayout.CENTER);

		JPanel south = new JPanel(new BorderLayout(0, SECTION_GAP));
		south.setOpaque(false);

		if (canSave) {
			GlassCard nameCard = new GlassCard(new BorderLayout(0, ROW_GAP), owner, NAME_PROMPT);
			nameField = new JTextField(DEFAULT_PLAYER);
			nameField.setFont(GameTheme.scaled(GameTheme.FONT_BODY, owner));
			nameField.setForeground(GameTheme.TEXT_PRIMARY);
			nameField.setBackground(GameTheme.PANEL_SURFACE);
			nameField.setCaretColor(GameTheme.TEXT_PRIMARY);
			nameField.setBorder(new EmptyBorder(10, 12, 10, 12));
			nameCard.add(nameField, BorderLayout.CENTER);
			south.add(nameCard, BorderLayout.CENTER);
		} else {
			nameField = null;
		}

		ArcadeButton continueButton = new ArcadeButton(CONTINUE_LABEL);
		continueButton.applyScaledSize(owner, BUTTON_WIDTH, BUTTON_HEIGHT);
		continueButton.addActionListener(event -> onContinue());
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		footer.setOpaque(false);
		footer.add(continueButton);
		south.add(footer, BorderLayout.SOUTH);
		root.add(south, BorderLayout.SOUTH);

		setContentPane(root);
		pack();
		java.awt.Dimension packed = getSize();
		java.awt.Dimension baseline = view.UiScale.quarterScreenSize();
		setSize(
				Math.max(packed.width, (int) (baseline.width * 0.55)),
				Math.max(packed.height, (int) (baseline.height * 0.55)));
		setMinimumSize(new java.awt.Dimension(packed.width, packed.height));
		setLocationRelativeTo(owner);
		getRootPane().setDefaultButton(continueButton);
	}

	private String buildPlacementText() {
		if (placementRank == HallOfFame.NO_PLACEMENT) {
			return NO_PLACE;
		}
		int displayRank = placementRank + ONE_BASED_INDEX_OFFSET;
		String meanText = formatSeconds(durationMs / lapCount);
		if (humanWinner) {
			return String.format(NEW_RECORD_FORMAT, displayRank, meanText);
		}
		return String.format(PLACEMENT_FORMAT, displayRank, meanText);
	}

	private static JPanel buildInfoRow(Component owner, String label, String value) {
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		JLabel keyLabel = ThemedPanel.createLabel(label, owner);
		keyLabel.setForeground(GameTheme.TEXT_MUTED);
		JLabel valueLabel = ThemedPanel.createLabel(value, owner);
		valueLabel.setHorizontalAlignment(JLabel.RIGHT);
		valueLabel.setForeground(GameTheme.TEXT_PRIMARY);
		row.add(keyLabel, BorderLayout.WEST);
		row.add(valueLabel, BorderLayout.EAST);
		return row;
	}

	private static String formatSeconds(double durationMs) {
		return String.format("%.2f", durationMs / MS_PER_SECOND);
	}

	private void onContinue() {
		if (nameField != null && placementRank != HallOfFame.NO_PLACEMENT) {
			String playerName = nameField.getText();
			if (playerName == null || playerName.isBlank()) {
				playerName = DEFAULT_PLAYER;
			}
			hallOfFame.addResult(playerName.trim(), durationMs, lapCount, trackIndex);
		}
		dispose();
	}

	public void showDialog() {
		setVisible(true);
	}
}
