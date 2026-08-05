package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import model.ConfigLoader;
import model.GameCatalog;
import model.HallOfFame;
import model.Result;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.StyledComboBox;
import view.components.ThemedPanel;
import view.theme.GameTheme;
import view.ui.BackgroundPanel;

/**
 * Hall of Fame screen content hosted inside {@link AppShell}.
 */
public class HallPanel extends BackgroundPanel implements Observer, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMN_NAMES = {
			ConfigLoader.getString("messages.hall.table.rank", "Rank"),
			ConfigLoader.getString("messages.hall.table.name", "Name"),
			ConfigLoader.getString("messages.hall.table.duration", "Duration"),
			ConfigLoader.getString("messages.hall.table.laps", "Laps"),
			ConfigLoader.getString("messages.hall.table.mean", "Mean"),
			ConfigLoader.getString("messages.hall.table.date", "Date")
	};
	private static final String HEADER_TITLE = ConfigLoader.getString("messages.hall.header.title", "Hall of Fame");
	private static final String HEADER_SUBTITLE = ConfigLoader.getString(
			"messages.hall.header.subtitle",
			"Best mean lap times by track");
	private static final String TRACK_LABEL = ConfigLoader.getString("messages.hall.section.track", "Track");
	private static final String LEADERBOARD_TITLE = ConfigLoader.getString("messages.hall.section.leaderboard", "Leaderboard");
	private static final String CLOSE_BUTTON_LABEL = ConfigLoader.getString("messages.hall.button.close", "Close");
	private static final String EMPTY_CELL = ConfigLoader.getString("messages.hall.empty.cell", "-");
	private static final String TIME_SUFFIX = ConfigLoader.getString("messages.hall.time.suffix", " s");
	private static final int ONE_BASED_INDEX_OFFSET = 1;

	private static final int PANEL_INSET = 22;
	private static final int BODY_VERTICAL_GAP = 14;
	private static final int TABLE_ROW_HEIGHT = 28;
	private static final int MIN_ROW_HEIGHT = 22;
	private static final int COMBO_HEIGHT = 44;
	private static final int CLOSE_BUTTON_WIDTH = 150;
	private static final int CLOSE_BUTTON_HEIGHT = 48;
	private static final int MS_PER_SECOND = 1000;

	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final DefaultTableModel tableModel;
	private final JTable resultsTable;
	private final ArcadeButton closeButton;
	private final Runnable onClose;
	private HallOfFame hallOfFame;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public HallPanel(Component scaleContext, Runnable onClose) {
		super(BackgroundPanel.Style.SCREEN);
		this.onClose = onClose;

		setLayout(new BorderLayout(0, BODY_VERTICAL_GAP));
		setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		add(ThemedPanel.createHeader(HEADER_TITLE, HEADER_SUBTITLE, scaleContext), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, BODY_VERTICAL_GAP));
		body.setOpaque(false);

		GlassCard selectorCard = new GlassCard(new BorderLayout(), scaleContext, TRACK_LABEL);
		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		StyledComboBox.apply(trackMenu, scaleContext, longestItemLabel(trackMenu));
		layoutTrackSelector(trackMenu, scaleContext);
		selectorCard.add(trackMenu, BorderLayout.CENTER);
		body.add(selectorCard, BorderLayout.NORTH);

		tableModel = new DefaultTableModel(COLUMN_NAMES, 0);
		resultsTable = new JTable(tableModel);
		resultsTable.setEnabled(false);
		resultsTable.setShowVerticalLines(false);
		resultsTable.setShowHorizontalLines(true);
		resultsTable.setGridColor(GameTheme.BORDER_SOFT);
		resultsTable.setOpaque(true);
		resultsTable.setBackground(GameTheme.PANEL_SURFACE);
		resultsTable.setFillsViewportHeight(true);
		resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		resultsTable.getColumnModel().getColumn(0).setPreferredWidth(UiScale.scale(scaleContext, 52));
		resultsTable.getColumnModel().getColumn(1).setPreferredWidth(UiScale.scale(scaleContext, 100));
		resultsTable.getColumnModel().getColumn(2).setPreferredWidth(UiScale.scale(scaleContext, 80));
		resultsTable.getColumnModel().getColumn(3).setPreferredWidth(UiScale.scale(scaleContext, 52));
		resultsTable.getColumnModel().getColumn(4).setPreferredWidth(UiScale.scale(scaleContext, 90));
		resultsTable.setRowHeight(Math.max(MIN_ROW_HEIGHT, UiScale.scale(scaleContext, TABLE_ROW_HEIGHT)));
		resultsTable.setFont(GameTheme.scaled(GameTheme.FONT_BODY, scaleContext));
		resultsTable.setDefaultRenderer(Object.class, new ThemedCellRenderer());
		JTableHeader header = resultsTable.getTableHeader();
		header.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, scaleContext));
		header.setBackground(GameTheme.ACCENT_BLUE);
		header.setForeground(GameTheme.TEXT_PRIMARY);
		header.setOpaque(true);
		header.setDefaultRenderer(new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(
					JTable table,
					Object value,
					boolean isSelected,
					boolean hasFocus,
					int row,
					int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setOpaque(true);
				setBackground(GameTheme.ACCENT_BLUE);
				setForeground(GameTheme.TEXT_PRIMARY);
				setBorder(new EmptyBorder(8, 10, 8, 10));
				return this;
			}
		});

		JScrollPane scrollPane = new JScrollPane(resultsTable);
		scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		int tableViewportHeight = resultsTable.getRowHeight() * (HallOfFame.MAX_RESULTS + 1);
		resultsTable.setPreferredScrollableViewportSize(new Dimension(0, tableViewportHeight));
		scrollPane.getViewport().setBackground(GameTheme.PANEL_SURFACE);
		scrollPane.getViewport().setOpaque(true);
		scrollPane.setOpaque(true);
		scrollPane.setBackground(GameTheme.PANEL_SURFACE);

		GlassCard tableCard = new GlassCard(new BorderLayout(), scaleContext, LEADERBOARD_TITLE);
		tableCard.add(scrollPane, BorderLayout.CENTER);
		body.add(tableCard, BorderLayout.CENTER);
		add(body, BorderLayout.CENTER);

		closeButton = new ArcadeButton(CLOSE_BUTTON_LABEL, false);
		closeButton.addActionListener(this);
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(closeButton, BorderLayout.EAST);
		add(footer, BorderLayout.SOUTH);
	}

	public void applyScaledMetrics(Component scaleContext) {
		closeButton.applyScaledSize(scaleContext, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
		StyledComboBox.apply(trackMenu, scaleContext, longestItemLabel(trackMenu));
		layoutTrackSelector(trackMenu, scaleContext);
		resultsTable.setRowHeight(Math.max(MIN_ROW_HEIGHT, UiScale.scale(scaleContext, TABLE_ROW_HEIGHT)));
		int tableViewportHeight = resultsTable.getRowHeight() * (HallOfFame.MAX_RESULTS + 1);
		resultsTable.setPreferredScrollableViewportSize(new Dimension(0, tableViewportHeight));
		resultsTable.setFont(GameTheme.scaled(GameTheme.FONT_BODY, scaleContext));
		resultsTable.getTableHeader().setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, scaleContext));
		revalidate();
		repaint();
	}

	public void refreshOnShow() {
		if (hallOfFame != null) {
			populateTable(trackMenu.getSelectedIndex());
		}
	}

	@Override
	public void update(Observable observable, Object argument) {
		hallOfFame = (HallOfFame) observable;
		int trackIndex = hallOfFame.getLastUpdatedTrackIndex();
		trackMenu.setSelectedIndex(trackIndex);
		populateTable(trackIndex);
	}

	private void populateTable(int trackIndex) {
		tableModel.setRowCount(0);
		for (int rankIndex = 0; rankIndex < HallOfFame.MAX_RESULTS; rankIndex++) {
			try {
				Result result = hallOfFame.getResult(trackIndex, rankIndex);
				tableModel.addRow(new Object[] {
						Integer.toString(rankIndex + ONE_BASED_INDEX_OFFSET),
						result.getName(),
						formatSeconds(result.getDurationMs()) + TIME_SUFFIX,
						Integer.toString(result.getLapCount()),
						formatSeconds(result.getMeanLapTimeMs()) + TIME_SUFFIX,
						result.getDate()
				});
			} catch (RuntimeException exception) {
				tableModel.addRow(new Object[] {
						Integer.toString(rankIndex + ONE_BASED_INDEX_OFFSET),
						EMPTY_CELL,
						EMPTY_CELL,
						EMPTY_CELL,
						EMPTY_CELL,
						EMPTY_CELL
				});
			}
		}
	}

	private static String formatSeconds(double durationMs) {
		return String.format("%.2f", durationMs / MS_PER_SECOND);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		onClose.run();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void itemStateChanged(ItemEvent event) {
		if (event.getStateChange() != ItemEvent.SELECTED || hallOfFame == null) {
			return;
		}
		JComboBox box = (JComboBox) event.getSource();
		populateTable(box.getSelectedIndex());
	}

	private void layoutTrackSelector(JComboBox<?> comboBox, Component scaleContext) {
		int comboHeight = UiScale.scale(scaleContext, COMBO_HEIGHT);
		Dimension comboSize = new Dimension(comboBox.getPreferredSize().width, comboHeight);
		comboBox.setPreferredSize(comboSize);
		comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));
	}

	private static String longestItemLabel(JComboBox<?> comboBox) {
		String longest = "Track";
		for (int index = 0; index < comboBox.getItemCount(); index++) {
			Object item = comboBox.getItemAt(index);
			if (item == null) {
				continue;
			}
			String text = String.valueOf(item);
			if (text.length() > longest.length()) {
				longest = text;
			}
		}
		return longest;
	}

	private static final class ThemedCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(
				JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setOpaque(true);
			setForeground(GameTheme.TEXT_PRIMARY);
			setBackground(row % 2 == 0 ? GameTheme.GLASS_FILL : GameTheme.PANEL_SURFACE);
			setBorder(new EmptyBorder(6, 10, 6, 10));
			return this;
		}
	}
}
