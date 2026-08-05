package view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import model.ConfigLoader;
import model.GameCatalog;
import model.HallOfFame;
import model.Result;
import view.components.ArcadeButton;
import view.components.ThemedPanel;
import view.theme.GameTheme;

public class HallFrame extends JFrame implements Observer, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final String WINDOW_TITLE = ConfigLoader.getString("messages.hall.window.title", "Hall Of Fame");
	private static final String[] COLUMN_NAMES = {
			ConfigLoader.getString("messages.hall.table.rank", "Rank"),
			ConfigLoader.getString("messages.hall.table.name", "Name"),
			ConfigLoader.getString("messages.hall.table.time", "Time"),
			ConfigLoader.getString("messages.hall.table.date", "Date")
	};
	private static final String HEADER_TITLE = ConfigLoader.getString("messages.hall.header.title", "Hall of Fame");
	private static final String HEADER_SUBTITLE = ConfigLoader.getString(
			"messages.hall.header.subtitle",
			"Best lap times by track");
	private static final String TRACK_LABEL = ConfigLoader.getString("messages.hall.section.track", "Track");
	private static final String LEADERBOARD_TITLE = ConfigLoader.getString("messages.hall.section.leaderboard", "Leaderboard");
	private static final String CLOSE_BUTTON_LABEL = ConfigLoader.getString("messages.hall.button.close", "Close");
	private static final String EMPTY_CELL = ConfigLoader.getString("messages.hall.empty.cell", "-");
	private static final String TIME_SUFFIX = ConfigLoader.getString("messages.hall.time.suffix", " s");
	private static final int ONE_BASED_INDEX_OFFSET = 1;

	private static final int ROOT_LAYOUT_GAP = 16;
	private static final int PANEL_INSET = 18;
	private static final int BODY_VERTICAL_GAP = 12;
	private static final int SELECTOR_GAP = 12;
	private static final int TABLE_ROW_HEIGHT = 24;
	private static final int MIN_ROW_HEIGHT = 1;
	private static final int CLOSE_BUTTON_WIDTH = 140;
	private static final int CLOSE_BUTTON_HEIGHT = 46;
	private static final int MS_PER_SECOND = 1000;

	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final DefaultTableModel tableModel;
	private final JTable resultsTable;
	private final MenuFrame menuFrame;
	private final ArcadeButton closeButton;
	private HallOfFame hallOfFame;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public HallFrame(MenuFrame menuFrame) {
		super(WINDOW_TITLE);
		this.menuFrame = menuFrame;

		ThemedPanel root = new ThemedPanel();
		root.setLayout(new BorderLayout(0, ROOT_LAYOUT_GAP));
		root.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));
		root.styleSurface(GameTheme.BACKGROUND_DARK);

		root.add(ThemedPanel.createHeader(HEADER_TITLE, HEADER_SUBTITLE, this), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, BODY_VERTICAL_GAP));
		body.setOpaque(false);

		JPanel selectorPanel = new JPanel(new BorderLayout(SELECTOR_GAP, 0));
		selectorPanel.setOpaque(false);
		selectorPanel.add(ThemedPanel.createLabel(TRACK_LABEL, this), BorderLayout.WEST);

		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		trackMenu.addItemListener(this);
		selectorPanel.add(trackMenu, BorderLayout.CENTER);
		body.add(selectorPanel, BorderLayout.NORTH);

		tableModel = new DefaultTableModel(COLUMN_NAMES, 0);
		resultsTable = new JTable(tableModel);
		resultsTable.setEnabled(false);
		resultsTable.setRowHeight(Math.max(MIN_ROW_HEIGHT, UiScale.scale(this, TABLE_ROW_HEIGHT)));
		resultsTable.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		resultsTable.getTableHeader().setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, this));
		resultsTable.setGridColor(GameTheme.BORDER_SOFT);
		resultsTable.setBackground(GameTheme.PANEL_SURFACE);
		resultsTable.setForeground(GameTheme.TEXT_PRIMARY);
		resultsTable.getTableHeader().setBackground(GameTheme.ACCENT_BLUE);
		resultsTable.getTableHeader().setForeground(GameTheme.TEXT_PRIMARY);

		JScrollPane scrollPane = new JScrollPane(resultsTable);
		scrollPane.setBorder(ThemedPanel.sectionBorder(LEADERBOARD_TITLE, this));
		scrollPane.getViewport().setBackground(GameTheme.PANEL_SURFACE);
		body.add(scrollPane, BorderLayout.CENTER);
		root.add(body, BorderLayout.CENTER);

		closeButton = new ArcadeButton(CLOSE_BUTTON_LABEL, false);
		closeButton.addActionListener(this);
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(closeButton, BorderLayout.EAST);
		root.add(footer, BorderLayout.SOUTH);

		setContentPane(root);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				hideHall();
			}
		});
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		UiScale.enableDelayedResize(this, this::applyScaledMetrics);
		setVisible(false);
	}

	private void applyScaledMetrics() {
		closeButton.applyScaledSize(this, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
		trackMenu.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		resultsTable.setRowHeight(Math.max(MIN_ROW_HEIGHT, UiScale.scale(this, TABLE_ROW_HEIGHT)));
		resultsTable.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		resultsTable.getTableHeader().setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, this));
		revalidate();
		repaint();
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
						result.getTimeMs() / MS_PER_SECOND + TIME_SUFFIX,
						result.getDate()
				});
			} catch (RuntimeException exception) {
				tableModel.addRow(new Object[] {
						Integer.toString(rankIndex + ONE_BASED_INDEX_OFFSET),
						EMPTY_CELL,
						EMPTY_CELL,
						EMPTY_CELL
				});
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		hideHall();
	}

	public void showHall() {
		if (hallOfFame != null) {
			populateTable(trackMenu.getSelectedIndex());
		}
		applyScaledMetrics();
		setVisible(true);
		toFront();
	}

	public void hideHall() {
		setVisible(false);
		menuFrame.showMenu();
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
}
