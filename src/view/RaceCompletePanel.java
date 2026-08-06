package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import model.ConfigLoader;
import model.GameCatalog;
import model.HallOfFame;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

/**
 * Race-finish screen hosted inside {@link AppShell}.
 */
public class RaceCompletePanel extends BackgroundPanel {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = ConfigLoader.getString("messages.race.complete.title", "Race Complete");
	private static final String WINNER_LABEL = ConfigLoader.getString("messages.race.complete.winner", "Winner");
	private static final String CAR_LABEL = ConfigLoader.getString("messages.race.complete.car", "Car");
	private static final String TIME_LABEL = ConfigLoader.getString("messages.race.complete.time", "Time");
	private static final String LAPS_LABEL = ConfigLoader.getString("messages.race.complete.laps", "Laps");
	private static final String MEAN_LABEL = ConfigLoader.getString("messages.race.complete.mean", "Mean lap time");
	private static final String NEW_RECORD_FORMAT = ConfigLoader.getString(
			"messages.race.complete.new.record",
			"New record - Leaderboard #%d: %s s/lap");
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
	private final Runnable onContinueWithoutSave;
	private final double durationMs;
	private final int lapCount;
	private final int trackIndex;
	private final int winnerCarModelIndex;
	private final boolean humanWinner;
	private final int placementRank;
	private final JTextField nameField;
	private final ArcadeButton continueButton;

	public RaceCompletePanel(
			Component scaleContext,
			HallOfFame hallOfFame,
			int winnerIndex,
			int humanPlayerCount,
			double durationMs,
			int lapCount,
			int trackIndex,
			int winnerCarModelIndex,
			Runnable onContinueWithoutSave) {
		super(BackgroundPanel.Style.SCREEN);
		this.hallOfFame = hallOfFame;
		this.onContinueWithoutSave = onContinueWithoutSave;
		this.durationMs = durationMs;
		this.lapCount = lapCount;
		this.trackIndex = trackIndex;
		this.winnerCarModelIndex = winnerCarModelIndex;
		this.humanWinner = winnerIndex < humanPlayerCount;
		this.placementRank = hallOfFame.findPlacementRank(durationMs, lapCount, trackIndex);

		String winnerName = humanWinner
				? PLAYER_PREFIX + (winnerIndex + ONE_BASED_INDEX_OFFSET)
				: COMPUTER_NAME;
		boolean canSave = humanWinner && placementRank != HallOfFame.NO_PLACEMENT;

		setLayout(new BorderLayout(0, SECTION_GAP));
		setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));
		add(ThemedPanel.createHeader(TITLE, null, scaleContext), BorderLayout.NORTH);

		GlassCard summaryCard = new GlassCard(new BorderLayout(0, SECTION_GAP), scaleContext, null);
		JPanel summary = new JPanel(new GridLayout(0, 1, 0, ROW_GAP));
		summary.setOpaque(false);
		summary.add(buildInfoRow(scaleContext, WINNER_LABEL, winnerName));
		summary.add(buildInfoRow(
				scaleContext,
				CAR_LABEL,
				GameCatalog.carModelOptionLabel(winnerCarModelIndex)));
		summary.add(buildInfoRow(scaleContext, TIME_LABEL, formatSeconds(durationMs) + TIME_SUFFIX));
		summary.add(buildInfoRow(scaleContext, LAPS_LABEL, Integer.toString(lapCount)));
		summary.add(buildInfoRow(
				scaleContext,
				MEAN_LABEL,
				formatSeconds(durationMs / lapCount) + TIME_SUFFIX + "/lap"));

		JLabel placementLabel = ThemedPanel.createLabel(buildPlacementText(), scaleContext);
		placementLabel.setForeground(
				placementRank != HallOfFame.NO_PLACEMENT ? GameTheme.ACCENT_YELLOW : GameTheme.TEXT_MUTED);
		placementLabel.setHorizontalAlignment(JLabel.CENTER);
		summary.add(placementLabel);
		summaryCard.add(summary, BorderLayout.NORTH);

		JPanel stack = new JPanel();
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setOpaque(false);
		pinHeightStretchWidth(summaryCard);
		stack.add(summaryCard);

		if (canSave) {
			stack.add(Box.createVerticalStrut(SECTION_GAP));
			GlassCard nameCard = new GlassCard(new BorderLayout(0, ROW_GAP), scaleContext, NAME_PROMPT);
			nameField = new JTextField(DEFAULT_PLAYER);
			nameField.setFont(GameTheme.scaled(GameTheme.FONT_BODY, scaleContext));
			nameField.setForeground(GameTheme.TEXT_PRIMARY);
			nameField.setBackground(GameTheme.PANEL_SURFACE);
			nameField.setCaretColor(GameTheme.TEXT_PRIMARY);
			nameField.setBorder(new EmptyBorder(10, 12, 10, 12));
			nameCard.add(nameField, BorderLayout.NORTH);
			pinHeightStretchWidth(nameCard);
			stack.add(nameCard);
		} else {
			nameField = null;
		}

		// Keep the result stack under the title instead of stretching it through
		// the fixed shell's empty vertical space.
		JPanel body = new JPanel(new BorderLayout());
		body.setOpaque(false);
		body.add(stack, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);

		continueButton = new ArcadeButton(CONTINUE_LABEL);
		continueButton.addActionListener(event -> onContinue());
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		footer.setOpaque(false);
		footer.add(continueButton);
		add(footer, BorderLayout.SOUTH);

		applyScaledMetrics(scaleContext);
	}

	public void applyScaledMetrics(Component context) {
		continueButton.applyScaledSize(context, BUTTON_WIDTH, BUTTON_HEIGHT);
		if (nameField != null) {
			nameField.setFont(GameTheme.scaled(GameTheme.FONT_BODY, context));
		}
		revalidate();
		repaint();
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

	/** BoxLayout grows children up to their max size; lock height, free width. */
	private static void pinHeightStretchWidth(JComponent component) {
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		Dimension preferred = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
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
			hallOfFame.addResult(
					playerName.trim(),
					durationMs,
					lapCount,
					trackIndex,
					winnerCarModelIndex);
			return;
		}
		onContinueWithoutSave.run();
	}
}
