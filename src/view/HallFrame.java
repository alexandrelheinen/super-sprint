package view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import model.Circuit;
import model.HallOfFame;
import model.Result;

public class HallFrame extends JFrame implements Observer, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;
	private static final int REFERENCE_TABLE_WIDTH = 520;
	private static final int REFERENCE_TABLE_HEIGHT = 220;

	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final JTable resultsTable;
	private final JTable headerTable;
	private final MenuFrame menuFrame;
	private final JButton closeButton;
	private final JPanel panel;
	private HallOfFame hallOfFame;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public HallFrame(MenuFrame menuFrame) {
		super("Hall Of Fame");
		this.menuFrame = menuFrame;

		panel = new JPanel();
		String[] trackOptions = new String[Circuit.TRACK_COUNT];
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			trackOptions[trackIndex] = "Track " + (trackIndex + 1);
		}

		String[][] header = {{"Rank", "Name", "Time", "Date"}};
		String[] columnNames = {"", "", "", ""};
		headerTable = new JTable(header, columnNames);
		resultsTable = new JTable(new String[HallOfFame.MAX_RESULTS][4], columnNames);
		headerTable.setEnabled(false);
		resultsTable.setEnabled(false);

		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setSelectedIndex(0);

		closeButton = new JButton("Close");
		closeButton.addActionListener(this);

		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(trackMenu);
		panel.add(headerTable);
		panel.add(resultsTable);
		panel.add(closeButton);
		add(panel);

		applyLayout(layout);
		applyScaledMetrics();

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				hideHall();
			}
		});
		UiScale.applyQuarterScreenSize(this);
		UiScale.enableDelayedResize(this, this::applyScaledMetrics);
		setVisible(false);
	}

	private void applyLayout(SpringLayout layout) {
		Container contentPane = getContentPane();
		layout.putConstraint(SpringLayout.WEST, trackMenu, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, trackMenu, 10, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, headerTable, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, headerTable, 8, SpringLayout.SOUTH, trackMenu);
		layout.putConstraint(SpringLayout.WEST, resultsTable, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, resultsTable, 8, SpringLayout.SOUTH, headerTable);
		layout.putConstraint(SpringLayout.EAST, closeButton, -10, SpringLayout.EAST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, closeButton, 10, SpringLayout.SOUTH, resultsTable);
	}

	private void applyScaledMetrics() {
		int tableWidth = UiScale.scale(this, REFERENCE_TABLE_WIDTH);
		int headerHeight = UiScale.scale(this, 24);
		int tableHeight = UiScale.scale(this, REFERENCE_TABLE_HEIGHT);
		headerTable.setPreferredSize(new Dimension(tableWidth, headerHeight));
		resultsTable.setPreferredSize(new Dimension(tableWidth, tableHeight));
		headerTable.setFont(UiScale.scaledFont(this, headerTable.getFont().deriveFont(Font.BOLD, 12f)));
		resultsTable.setFont(UiScale.scaledFont(this, resultsTable.getFont()));
		trackMenu.setFont(UiScale.scaledFont(this, trackMenu.getFont()));
		closeButton.setFont(UiScale.scaledFont(this, closeButton.getFont()));
		panel.revalidate();
		panel.repaint();
	}

	@Override
	public void update(Observable observable, Object argument) {
		hallOfFame = (HallOfFame) observable;
		int trackIndex = hallOfFame.getLastUpdatedTrackIndex();
		trackMenu.setSelectedIndex(trackIndex);
		populateTable(trackIndex);
	}

	private void populateTable(int trackIndex) {
		for (int rankIndex = 0; rankIndex < HallOfFame.MAX_RESULTS; rankIndex++) {
			resultsTable.setValueAt(Integer.toString(rankIndex + 1), rankIndex, 0);
			try {
			 Result result = hallOfFame.getResult(trackIndex, rankIndex);
				resultsTable.setValueAt(result.getName(), rankIndex, 1);
				resultsTable.setValueAt(result.getTimeMs() / 1000.0 + " s", rankIndex, 2);
				resultsTable.setValueAt(result.getDate(), rankIndex, 3);
			} catch (RuntimeException exception) {
				resultsTable.setValueAt("-", rankIndex, 1);
				resultsTable.setValueAt("-", rankIndex, 2);
				resultsTable.setValueAt("-", rankIndex, 3);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		hideHall();
	}

	public void showHall() {
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
