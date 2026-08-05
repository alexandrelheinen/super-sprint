package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
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

import model.Circuit;
import model.GameCatalog;
import model.HallOfFame;
import model.Result;
import view.components.ArcadeButton;
import view.components.ThemedPanel;
import view.theme.GameTheme;

public class HallFrame extends JFrame implements Observer, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;
	private static final String[] COLUMN_NAMES = {"Rank", "Name", "Time", "Date"};

	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final DefaultTableModel tableModel;
	private final JTable resultsTable;
	private final MenuFrame menuFrame;
	private final ArcadeButton closeButton;
	private HallOfFame hallOfFame;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public HallFrame(MenuFrame menuFrame) {
		super("Hall Of Fame");
		this.menuFrame = menuFrame;

		ThemedPanel root = new ThemedPanel();
		root.setLayout(new BorderLayout(0, 16));
		root.setBorder(new EmptyBorder(18, 18, 18, 18));
		root.styleSurface(GameTheme.BACKGROUND_DARK);

		root.add(ThemedPanel.createHeader("Hall of Fame", "Best lap times by track", this), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, 12));
		body.setOpaque(false);

		JPanel selectorPanel = new JPanel(new BorderLayout(12, 0));
		selectorPanel.setOpaque(false);
		selectorPanel.add(ThemedPanel.createLabel("Track", this), BorderLayout.WEST);

		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		trackMenu.addItemListener(this);
		selectorPanel.add(trackMenu, BorderLayout.CENTER);
		body.add(selectorPanel, BorderLayout.NORTH);

		tableModel = new DefaultTableModel(COLUMN_NAMES, 0);
		resultsTable = new JTable(tableModel);
		resultsTable.setEnabled(false);
		resultsTable.setRowHeight(Math.max(1, UiScale.scale(this, 24)));
		resultsTable.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		resultsTable.getTableHeader().setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, this));
		resultsTable.setGridColor(GameTheme.BORDER_SOFT);
		resultsTable.setBackground(GameTheme.PANEL_SURFACE);
		resultsTable.setForeground(GameTheme.TEXT_PRIMARY);
		resultsTable.getTableHeader().setBackground(GameTheme.ACCENT_BLUE);
		resultsTable.getTableHeader().setForeground(GameTheme.TEXT_PRIMARY);

		JScrollPane scrollPane = new JScrollPane(resultsTable);
		scrollPane.setBorder(ThemedPanel.sectionBorder("Leaderboard", this));
		scrollPane.getViewport().setBackground(GameTheme.PANEL_SURFACE);
		body.add(scrollPane, BorderLayout.CENTER);
		root.add(body, BorderLayout.CENTER);

		closeButton = new ArcadeButton("Close", false);
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
		closeButton.applyScaledSize(this, 140, 46);
		trackMenu.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		resultsTable.setRowHeight(Math.max(1, UiScale.scale(this, 24)));
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
						Integer.toString(rankIndex + 1),
						result.getName(),
						result.getTimeMs() / 1000.0 + " s",
						result.getDate()
				});
			} catch (RuntimeException exception) {
				tableModel.addRow(new Object[] {Integer.toString(rankIndex + 1), "-", "-", "-"});
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
