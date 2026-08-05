package view;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;

import controller.Game;
import model.Car;
import model.Circuit;
import model.HallOfFame;
import model.ResourcePaths;

public class MenuFrame extends JFrame implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;
	private static final int LAP_COUNT = 3;

	private final HallFrame hallFrame;
	private final HallOfFame hallOfFame;

	private final JPanel mainPanel;
	private final JButton[] mainButtons;

	private final JPanel racePanel;
	private final JPanel[] carPanels;
	@SuppressWarnings("rawtypes")
	private final JComboBox[] carMenus;
	private final JLabel[] carIcons;
	private final JProgressBar[][] carStatBars;

	private final JPanel trackPanel;
	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final JLabel trackIcon;

	private final JButton startButton;
	private final JButton backToMenuButton;

	private int selectedTrack = 1;
	private final int[] selectedCarModels = new int[2];
	private int humanPlayerCount;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public MenuFrame() throws FileNotFoundException {
		super("Super Sprint Supelec");

		hallFrame = new HallFrame(this);
		hallOfFame = new HallOfFame(hallFrame);

		mainPanel = new JPanel();
		JPanel imagePanel = new JPanel();
		imagePanel.add(new JLabel(new ImageIcon(ResourcePaths.bundledSprite("menu.png"))));

		JPanel buttonPanel = new JPanel(new GridLayout(2, 2));
		mainButtons = new JButton[] {
				new JButton("1 Player"),
				new JButton("2 Players"),
				new JButton("Hall of Fame"),
				new JButton("Help & Info")
		};
		for (int index = 0; index < mainButtons.length; index++) {
			buttonPanel.add(mainButtons[index]);
			mainButtons[index].addActionListener(this);
			mainButtons[index].setMnemonic(index);
		}
		mainPanel.add(imagePanel);
		mainPanel.add(buttonPanel);

		racePanel = new JPanel();
		carPanels = new JPanel[2];
		carMenus = new JComboBox[2];
		carStatBars = new JProgressBar[2][3];
		carIcons = new JLabel[2];
		trackPanel = new JPanel();

		for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
			String[] carOptions = new String[Car.CAR_MODEL_COUNT];
			for (int modelIndex = 0; modelIndex < Car.CAR_MODEL_COUNT; modelIndex++) {
				carOptions[modelIndex] = "Model " + (modelIndex + 1);
			}
			carMenus[playerIndex] = new JComboBox(carOptions);
			carMenus[playerIndex].setBackground(Color.WHITE);
			carMenus[playerIndex].addItemListener(this);
			carMenus[playerIndex].setName("car" + (playerIndex + 1));
			carIcons[playerIndex] = new JLabel();

			carPanels[playerIndex] = new JPanel();
			carPanels[playerIndex].setLayout(new BoxLayout(carPanels[playerIndex], BoxLayout.Y_AXIS));
			carPanels[playerIndex].add(new JLabel("Player " + (playerIndex + 1)));
			carPanels[playerIndex].add(carMenus[playerIndex]);
			carPanels[playerIndex].add(carIcons[playerIndex]);

			JPanel statsPanel = new JPanel(new GridLayout(3, 2));
			String[] statLabels = {"Acceleration  ", "Top Speed  ", "Handling  "};
			int[][] statLimits = {{100, 250}, {200, 400}, {30, 60}};
			for (int statIndex = 0; statIndex < 3; statIndex++) {
				statsPanel.add(new JLabel(statLabels[statIndex]));
				carStatBars[playerIndex][statIndex] = new JProgressBar();
				carStatBars[playerIndex][statIndex].setMinimum(statLimits[statIndex][0]);
				carStatBars[playerIndex][statIndex].setMaximum(statLimits[statIndex][1]);
				carStatBars[playerIndex][statIndex].setStringPainted(true);
				carStatBars[playerIndex][statIndex].setPreferredSize(new Dimension(10, 10));
				statsPanel.add(carStatBars[playerIndex][statIndex]);
			}
			carPanels[playerIndex].add(statsPanel);
			racePanel.add(carPanels[playerIndex]);
		}

		String[] trackOptions = new String[Circuit.TRACK_COUNT];
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			trackOptions[trackIndex] = "Track " + (trackIndex + 1);
		}
		trackMenu = new JComboBox(trackOptions);
		trackMenu.setBackground(Color.WHITE);
		trackMenu.addItemListener(this);
		trackMenu.setName("track");
		trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
		trackPanel.add(new JLabel("Choose a track"));
		trackPanel.add(trackMenu);
		trackIcon = new JLabel();
		trackPanel.add(new JLabel("     "));
		trackPanel.add(trackIcon);
		trackMenu.setSelectedIndex(0);

		startButton = new JButton("Start Race");
		startButton.setMnemonic(10);
		startButton.addActionListener(this);
		startButton.setPreferredSize(new Dimension(100, 60));

		backToMenuButton = new JButton("Main Menu");
		backToMenuButton.setMnemonic(11);
		backToMenuButton.addActionListener(this);
		backToMenuButton.setPreferredSize(new Dimension(120, 30));

		racePanel.add(trackPanel);
		racePanel.add(startButton);
		racePanel.add(backToMenuButton);

		SpringLayout layout = new SpringLayout();
		Container contentPane = getContentPane();
		racePanel.setLayout(layout);
		layout.putConstraint(SpringLayout.WEST, carPanels[0], 10, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, carPanels[0], 10, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, carPanels[1], 0, SpringLayout.WEST, carPanels[0]);
		layout.putConstraint(SpringLayout.NORTH, carPanels[1], 140, SpringLayout.NORTH, carPanels[0]);
		layout.putConstraint(SpringLayout.WEST, trackPanel, 20, SpringLayout.EAST, carPanels[1]);
		layout.putConstraint(SpringLayout.NORTH, trackPanel, 10, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, startButton, 262, SpringLayout.EAST, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, startButton, 90, SpringLayout.SOUTH, carPanels[0]);
		layout.putConstraint(SpringLayout.WEST, backToMenuButton, 252, SpringLayout.EAST, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, backToMenuButton, 140, SpringLayout.SOUTH, carPanels[0]);

		add(racePanel);
		showMainMenu();
		setIconImage(new ImageIcon(ResourcePaths.bundledSprite("icon.png")).getImage());
		mainPanel.setBackground(Color.BLACK);
		imagePanel.setBackground(Color.BLACK);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setResizable(false);

		carMenus[0].setSelectedIndex(0);
		carMenus[1].setSelectedIndex(0);
	}

	private void showMainMenu() {
		remove(racePanel);
		add(mainPanel);
		setSize(430, 380);
		repaint();
	}

	private void showRaceMenu(int players) {
		carMenus[1].setEnabled(players != 1);
		humanPlayerCount = players;
		remove(mainPanel);
		add(racePanel);
		setSize(420, 320);
		repaint();
	}

	public void showMenu() {
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		JButton button = (JButton) event.getSource();
		switch (button.getMnemonic()) {
			case 0:
				showRaceMenu(1);
				break;
			case 1:
				showRaceMenu(2);
				break;
			case 2:
				hallFrame.showHall();
				break;
			case 3:
				JOptionPane.showMessageDialog(
						null,
						"SUPER SPRINT SUPELEC\n"
								+ "_______________________________________\n"
								+ "GENERAL INFORMATION:\n\n"
								+ "Software Project 2014/2015 - Sequence 6\n"
								+ "Alexandre LOEBLEIN HEINEN & Gautier SHARPIN\n"
								+ "Version from 14.01.14\n"
								+ "_______________________________________\n"
								+ "CONTROLS:\n\n"
								+ "Player 1: arrow keys\n"
								+ "Player 2: W/A/S/D keys\n"
								+ "_______________________________________\n"
								+ "The race always lasts 3 laps.");
				break;
			case 10:
				setVisible(false);
				showMainMenu();
				int[] carModels = new int[4];
				for (int playerIndex = 0; playerIndex < humanPlayerCount; playerIndex++) {
					carModels[playerIndex] = selectedCarModels[playerIndex];
				}
				Random random = new Random();
				for (int aiIndex = humanPlayerCount; aiIndex < 4; aiIndex++) {
					carModels[aiIndex] = random.nextInt(4) + 1;
				}
				new Game(carModels, selectedTrack, humanPlayerCount, LAP_COUNT, hallOfFame);
				break;
			case 11:
				showMainMenu();
				break;
			default:
				break;
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void itemStateChanged(ItemEvent event) {
		JComboBox box = (JComboBox) event.getSource();
		String name = box.getName();
		if (name.contains("car")) {
			int playerIndex = name.contains("1") ? 0 : 1;
			int modelIndex = box.getSelectedIndex();
			carIcons[playerIndex].setIcon(new ImageIcon(ResourcePaths.carSpritePath(modelIndex + 1)));
			int[] stats = Car.CAR_MODEL_STATS[modelIndex];
			for (int statIndex = 0; statIndex < 3; statIndex++) {
				carStatBars[playerIndex][statIndex].setValue(stats[statIndex]);
				carStatBars[playerIndex][statIndex].setString(Integer.toString(stats[statIndex]));
			}
			selectedCarModels[playerIndex] = modelIndex + 1;
		} else {
			int trackIndex = box.getSelectedIndex();
			selectedTrack = trackIndex + 1;
			trackIcon.setIcon(new ImageIcon(ResourcePaths.bundledSprite("mini_circuit" + (trackIndex + 1) + ".png")));
		}
	}
}
