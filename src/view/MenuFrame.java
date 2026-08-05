package view;

import java.awt.Color;
import java.awt.Component;
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
import javax.swing.WindowConstants;

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
	private final JLabel menuImageLabel;
	private final JPanel buttonPanel;
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
	private final SpringLayout raceLayout;

	private int selectedTrack = 1;
	private final int[] selectedCarModels = new int[2];
	private int humanPlayerCount;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public MenuFrame() throws FileNotFoundException {
		super("Super Sprint Supelec");

		hallFrame = new HallFrame(this);
		hallOfFame = new HallOfFame(hallFrame);

		mainPanel = new JPanel();
		menuImageLabel = new JLabel();
		JPanel imagePanel = new JPanel();
		imagePanel.add(menuImageLabel);

		buttonPanel = new JPanel(new GridLayout(2, 2, 12, 12));
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
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
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

			JPanel statsPanel = new JPanel(new GridLayout(3, 2, 8, 8));
			String[] statLabels = {"Acceleration", "Top Speed", "Handling"};
			int[][] statLimits = {{100, 250}, {200, 400}, {30, 60}};
			for (int statIndex = 0; statIndex < 3; statIndex++) {
				statsPanel.add(new JLabel(statLabels[statIndex]));
				carStatBars[playerIndex][statIndex] = new JProgressBar();
				carStatBars[playerIndex][statIndex].setMinimum(statLimits[statIndex][0]);
				carStatBars[playerIndex][statIndex].setMaximum(statLimits[statIndex][1]);
				carStatBars[playerIndex][statIndex].setStringPainted(true);
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
		trackPanel.add(trackIcon);
		trackMenu.setSelectedIndex(0);

		startButton = new JButton("Start Race");
		startButton.setMnemonic(10);
		startButton.addActionListener(this);

		backToMenuButton = new JButton("Main Menu");
		backToMenuButton.setMnemonic(11);
		backToMenuButton.addActionListener(this);

		racePanel.add(trackPanel);
		racePanel.add(startButton);
		racePanel.add(backToMenuButton);

		raceLayout = new SpringLayout();
		racePanel.setLayout(raceLayout);
		applyRaceLayoutConstraints();

		setContentPane(mainPanel);
		showMainMenu();
		setIconImage(new ImageIcon(ResourcePaths.bundledSprite("icon.png")).getImage());
		mainPanel.setBackground(Color.BLACK);
		imagePanel.setBackground(Color.BLACK);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		UiScale.enableDelayedResize(this, this::applyScaledMetrics);
		setVisible(true);

		carMenus[0].setSelectedIndex(0);
		carMenus[1].setSelectedIndex(0);
	}

	private void applyRaceLayoutConstraints() {
		Container contentPane = racePanel;
		int padding = 16;
		int rowGap = 140;
		raceLayout.putConstraint(SpringLayout.WEST, carPanels[0], padding, SpringLayout.WEST, contentPane);
		raceLayout.putConstraint(SpringLayout.NORTH, carPanels[0], padding, SpringLayout.NORTH, contentPane);
		raceLayout.putConstraint(SpringLayout.WEST, carPanels[1], 0, SpringLayout.WEST, carPanels[0]);
		raceLayout.putConstraint(SpringLayout.NORTH, carPanels[1], rowGap, SpringLayout.NORTH, carPanels[0]);
		raceLayout.putConstraint(SpringLayout.WEST, trackPanel, padding, SpringLayout.EAST, carPanels[1]);
		raceLayout.putConstraint(SpringLayout.NORTH, trackPanel, padding, SpringLayout.NORTH, contentPane);
		raceLayout.putConstraint(SpringLayout.EAST, startButton, -padding, SpringLayout.EAST, contentPane);
		raceLayout.putConstraint(SpringLayout.NORTH, startButton, padding, SpringLayout.NORTH, contentPane);
		raceLayout.putConstraint(SpringLayout.EAST, backToMenuButton, -padding, SpringLayout.EAST, contentPane);
		raceLayout.putConstraint(SpringLayout.NORTH, backToMenuButton, 12, SpringLayout.SOUTH, startButton);
	}

	private void applyScaledMetrics() {
		UiScale.fitLabelIcon(menuImageLabel, this, ResourcePaths.bundledSprite("menu.png"), 640, 280);
		for (JButton button : mainButtons) {
			button.setFont(UiScale.scaledFont(this, button.getFont()));
			button.setPreferredSize(new Dimension(UiScale.scale(this, 180), UiScale.scale(this, 48)));
		}
		for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
			scaleComponentTree(carPanels[playerIndex]);
			if (carMenus[playerIndex].getSelectedIndex() >= 0) {
				int modelIndex = carMenus[playerIndex].getSelectedIndex();
				UiScale.fitLabelIcon(
						carIcons[playerIndex],
						this,
						ResourcePaths.carSpritePath(modelIndex + 1),
						80,
						48);
			}
		}
		scaleComponentTree(trackPanel);
		if (trackMenu.getSelectedIndex() >= 0) {
			int trackIndex = trackMenu.getSelectedIndex();
			UiScale.fitLabelIcon(
					trackIcon,
					this,
					ResourcePaths.bundledSprite("track_preview" + (trackIndex + 1) + ".png"),
					120,
					80);
		}
		startButton.setFont(UiScale.scaledFont(this, startButton.getFont()));
		backToMenuButton.setFont(UiScale.scaledFont(this, backToMenuButton.getFont()));
		startButton.setPreferredSize(new Dimension(UiScale.scale(this, 140), UiScale.scale(this, 52)));
		backToMenuButton.setPreferredSize(new Dimension(UiScale.scale(this, 140), UiScale.scale(this, 40)));
		revalidate();
		repaint();
	}

	private void scaleComponentTree(Component component) {
		component.setFont(UiScale.scaledFont(this, component.getFont()));
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				scaleComponentTree(child);
			}
		}
	}

	private void showMainMenu() {
		setContentPane(mainPanel);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		revalidate();
		repaint();
	}

	private void showRaceMenu(int players) {
		carMenus[1].setEnabled(players != 1);
		humanPlayerCount = players;
		setContentPane(racePanel);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		revalidate();
		repaint();
	}

	public void showMenu() {
		showMainMenu();
		setVisible(true);
		toFront();
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
						this,
						"SUPER SPRINT SUPELEC\n"
								+ "_______________________________________\n"
								+ "GENERAL INFORMATION:\n\n"
								+ "Software Project 2014/2015 - Sequence 6\n"
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
		if (event.getStateChange() != ItemEvent.SELECTED) {
			return;
		}
		JComboBox box = (JComboBox) event.getSource();
		String name = box.getName();
		if (name.contains("car")) {
			int playerIndex = name.contains("1") ? 0 : 1;
			int modelIndex = box.getSelectedIndex();
			UiScale.fitLabelIcon(
					carIcons[playerIndex],
					this,
					ResourcePaths.carSpritePath(modelIndex + 1),
					80,
					48);
			int[] stats = Car.CAR_MODEL_STATS[modelIndex];
			for (int statIndex = 0; statIndex < 3; statIndex++) {
				carStatBars[playerIndex][statIndex].setValue(stats[statIndex]);
				carStatBars[playerIndex][statIndex].setString(Integer.toString(stats[statIndex]));
			}
			selectedCarModels[playerIndex] = modelIndex + 1;
		} else {
			int trackIndex = box.getSelectedIndex();
			selectedTrack = trackIndex + 1;
			UiScale.fitLabelIcon(
					trackIcon,
					this,
					ResourcePaths.bundledSprite("track_preview" + (trackIndex + 1) + ".png"),
					120,
					80);
		}
	}
}
