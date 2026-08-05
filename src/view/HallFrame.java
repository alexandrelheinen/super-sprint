package view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SpringLayout;

import model.Circuit;
import model.HallOfFame;
import model.Result;

public class HallFrame extends JFrame implements Observer, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final JTable resultsTable;
	private final MenuFrame menuFrame;
	private HallOfFame hallOfFame;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public HallFrame(MenuFrame menuFrame) {
		super("Hall Of Fame");
		this.menuFrame = menuFrame;

		JPanel panel = new JPanel();
		String[] trackOptions = new String[Circuit.TRACK_COUNT];
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			trackOptions[trackIndex] = "Track " + (trackIndex + 1);
		}

		String[][] header = {{"Rank", "Name", "Time", "Date"}};
		String[] columnNames = {"", "", "", ""};
		JTable headerTable = new JTable(header, columnNames);
		resultsTable = new JTable(new String[HallOfFame.MAX_RESULTS][4], columnNames);
		headerTable.setPreferredSize(new Dimension(400, 17));
		headerTable.setEnabled(false);
		headerTable.setFont(headerTable.getFont().deriveFont(Font.BOLD, 12));
		resultsTable.setPreferredSize(new Dimension(400, 160));
		resultsTable.setEnabled(false);

		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setSelectedIndex(0);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);

		SpringLayout layout = new SpringLayout();
		Container contentPane = getContentPane();
		panel.setLayout(layout);
		layout.putConstraint(SpringLayout.WEST, trackMenu, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, trackMenu, 10, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, headerTable, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, headerTable, 8, SpringLayout.SOUTH, trackMenu);
		layout.putConstraint(SpringLayout.WEST, resultsTable, 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, resultsTable, 20, SpringLayout.NORTH, headerTable);
		layout.putConstraint(
				SpringLayout.EAST,
				closeButton,
				(int) headerTable.getPreferredSize().getWidth(),
				SpringLayout.WEST,
				headerTable);
		layout.putConstraint(SpringLayout.NORTH, closeButton, 10, SpringLayout.SOUTH, contentPane);

		panel.add(trackMenu);
		panel.add(headerTable);
		panel.add(resultsTable);
		panel.add(closeButton);
		add(panel);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setSize((int) resultsTable.getPreferredSize().getWidth() + 25, 265);
		setVisible(false);
		setResizable(false);
	}

	@Override
	public void update(Observable observable, Object argument) {
		hallOfFame = (HallOfFame) observable;
		int trackIndex = hallOfFame.getLastUpdatedTrackIndex();
		trackMenu.setSelectedIndex(trackIndex);
		populateTable(trackIndex);
		setVisible(true);
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
		setVisible(false);
		menuFrame.showMenu();
	}

	public void showHall() {
		setVisible(true);
	}

	public void hideHall() {
		setVisible(false);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void itemStateChanged(ItemEvent event) {
		JComboBox box = (JComboBox) event.getSource();
		populateTable(box.getSelectedIndex());
	}
}
