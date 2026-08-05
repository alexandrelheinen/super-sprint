package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import controller.Game;
import model.Car;
import model.Circuit;
import model.HallOfFame;
import model.ResourcePaths;
import view.components.ArcadeButton;
import view.components.ThemedPanel;
import view.theme.GameTheme;

public class MenuFrame extends JFrame implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;
	private static final int LAP_COUNT = 3;

	private final HallFrame hallFrame;
	private final HallOfFame hallOfFame;

	private final JPanel mainPanel;
	private final JLabel menuImageLabel;
	private final JPanel buttonPanel;
	private final ArcadeButton[] mainButtons;

	private final JPanel racePanel;
	private final ThemedPanel[] carPanels;
	@SuppressWarnings("rawtypes")
	private final JComboBox[] carMenus;
	private final JLabel[] carIcons;
	private final JProgressBar[][] carStatBars;

	private final ThemedPanel trackPanel;
	@SuppressWarnings("rawtypes")
	private final JComboBox trackMenu;
	private final JLabel trackIcon;

	private final ArcadeButton startButton;
	private final ArcadeButton backToMenuButton;

	private int selectedTrack = 1;
	private final int[] selectedCarModels = new int[2];
	private int humanPlayerCount;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public MenuFrame() throws FileNotFoundException {
		super("Super Sprint Supelec");

		hallFrame = new HallFrame(this);
		hallOfFame = new HallOfFame(hallFrame);

		mainPanel = new ThemedPanel();
		mainPanel.setLayout(new BorderLayout(0, 18));
		mainPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
		((ThemedPanel) mainPanel).styleSurface(GameTheme.BACKGROUND_DARK);

		menuImageLabel = new JLabel("", JLabel.CENTER);
		JPanel heroPanel = new ThemedPanel();
		heroPanel.setLayout(new BorderLayout());
		heroPanel.setOpaque(false);
		heroPanel.add(ThemedPanel.createHeader("SUPER SPRINT SUPELEC", "Arcade top-down racing", this), BorderLayout.NORTH);
		heroPanel.add(menuImageLabel, BorderLayout.CENTER);

		buttonPanel = new JPanel(new GridLayout(2, 2, 14, 14));
		buttonPanel.setOpaque(false);
		mainButtons = new ArcadeButton[] {
				new ArcadeButton("1 Player"),
				new ArcadeButton("2 Players"),
				new ArcadeButton("Hall of Fame", false),
				new ArcadeButton("Help & Info", false)
		};
		for (int index = 0; index < mainButtons.length; index++) {
			buttonPanel.add(mainButtons[index]);
			mainButtons[index].addActionListener(this);
			mainButtons[index].setMnemonic(index);
		}

		mainPanel.add(heroPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		racePanel = new ThemedPanel();
		racePanel.setLayout(new BorderLayout(16, 16));
		racePanel.setBorder(new EmptyBorder(18, 18, 18, 18));
		((ThemedPanel) racePanel).styleSurface(GameTheme.BACKGROUND_LIGHT);

		JPanel raceHeader = ThemedPanel.createHeader("Race Setup", "Choose cars, track, and launch the grid", this);
		racePanel.add(raceHeader, BorderLayout.NORTH);

		JPanel setupBody = new JPanel(new GridLayout(1, 3, 18, 0));
		setupBody.setOpaque(false);

		carPanels = new ThemedPanel[2];
		carMenus = new JComboBox[2];
		carStatBars = new JProgressBar[2][3];
		carIcons = new JLabel[2];

		for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
			carPanels[playerIndex] = new ThemedPanel();
			carPanels[playerIndex].setLayout(new BoxLayout(carPanels[playerIndex], BoxLayout.Y_AXIS));
			carPanels[playerIndex].setBorder(ThemedPanel.sectionBorder("Player " + (playerIndex + 1), this));

			String[] carOptions = new String[Car.CAR_MODEL_COUNT];
			for (int modelIndex = 0; modelIndex < Car.CAR_MODEL_COUNT; modelIndex++) {
				carOptions[modelIndex] = "Model " + (modelIndex + 1);
			}
			carMenus[playerIndex] = new JComboBox(carOptions);
			carMenus[playerIndex].addItemListener(this);
			carMenus[playerIndex].setName("car" + (playerIndex + 1));
			carIcons[playerIndex] = new JLabel("", JLabel.CENTER);

			carPanels[playerIndex].add(Box.createVerticalStrut(8));
			carPanels[playerIndex].add(carMenus[playerIndex]);
			carPanels[playerIndex].add(Box.createVerticalStrut(12));
			carPanels[playerIndex].add(carIcons[playerIndex]);
			carPanels[playerIndex].add(Box.createVerticalStrut(12));

			JPanel statsPanel = new JPanel(new GridLayout(3, 2, 8, 8));
			statsPanel.setOpaque(false);
			String[] statLabels = {"Acceleration", "Top Speed", "Handling"};
			int[][] statLimits = {{100, 250}, {200, 400}, {30, 60}};
			for (int statIndex = 0; statIndex < 3; statIndex++) {
				statsPanel.add(ThemedPanel.createLabel(statLabels[statIndex], this));
				carStatBars[playerIndex][statIndex] = new JProgressBar();
				carStatBars[playerIndex][statIndex].setMinimum(statLimits[statIndex][0]);
				carStatBars[playerIndex][statIndex].setMaximum(statLimits[statIndex][1]);
				carStatBars[playerIndex][statIndex].setStringPainted(true);
				carStatBars[playerIndex][statIndex].setForeground(GameTheme.ACCENT_BLUE);
				statsPanel.add(carStatBars[playerIndex][statIndex]);
			}
			carPanels[playerIndex].add(statsPanel);
			setupBody.add(carPanels[playerIndex]);
		}

		trackPanel = new ThemedPanel();
		trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
		trackPanel.setBorder(ThemedPanel.sectionBorder("Track", this));
		String[] trackOptions = new String[Circuit.TRACK_COUNT];
		for (int trackIndex = 0; trackIndex < Circuit.TRACK_COUNT; trackIndex++) {
			trackOptions[trackIndex] = "Track " + (trackIndex + 1);
		}
		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setName("track");
		trackIcon = new JLabel("", JLabel.CENTER);
		trackPanel.add(Box.createVerticalStrut(8));
		trackPanel.add(trackMenu);
		trackPanel.add(Box.createVerticalStrut(12));
		trackPanel.add(trackIcon);
		setupBody.add(trackPanel);
		racePanel.add(setupBody, BorderLayout.CENTER);

		JPanel actionPanel = new JPanel(new GridLayout(1, 2, 14, 0));
		actionPanel.setOpaque(false);
		startButton = new ArcadeButton("Start Race");
		startButton.setMnemonic(10);
		startButton.addActionListener(this);
		backToMenuButton = new ArcadeButton("Main Menu", false);
		backToMenuButton.setMnemonic(11);
		backToMenuButton.addActionListener(this);
		actionPanel.add(backToMenuButton);
		actionPanel.add(startButton);
		racePanel.add(actionPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
		showMainMenu();
		setIconImage(new ImageIcon(ResourcePaths.bundledSprite("icon.png")).getImage());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		UiScale.enableDelayedResize(this, this::applyScaledMetrics);
		setVisible(true);

		carMenus[0].setSelectedIndex(0);
		carMenus[1].setSelectedIndex(0);
		trackMenu.setSelectedIndex(0);
	}

	private void applyScaledMetrics() {
		UiScale.fitLabelIcon(menuImageLabel, this, ResourcePaths.bundledSprite("menu.png"), 700, 300);
		for (ArcadeButton button : mainButtons) {
			button.applyScaledSize(this, 190, 52);
		}
		for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
			styleComboBox(carMenus[playerIndex]);
			styleProgressBars(playerIndex);
			if (carMenus[playerIndex].getSelectedIndex() >= 0) {
				updateCarPreview(playerIndex, carMenus[playerIndex].getSelectedIndex());
			}
		}
		styleComboBox(trackMenu);
		if (trackMenu.getSelectedIndex() >= 0) {
			updateTrackPreview(trackMenu.getSelectedIndex());
		}
		startButton.applyScaledSize(this, 180, 52);
		backToMenuButton.applyScaledSize(this, 180, 52);
		revalidate();
		repaint();
	}

	private void styleComboBox(JComboBox<?> comboBox) {
		comboBox.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
		comboBox.setBackground(Color.WHITE);
	}

	private void styleProgressBars(int playerIndex) {
		for (JProgressBar bar : carStatBars[playerIndex]) {
			bar.setFont(GameTheme.scaled(GameTheme.FONT_BODY, this));
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
		carPanels[1].setVisible(players != 1);
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
		if (!(event.getSource() instanceof ArcadeButton button)) {
			return;
		}
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
				new Game(carModels, selectedTrack, humanPlayerCount, LAP_COUNT, hallOfFame, this);
				break;
			case 11:
				showMainMenu();
				break;
			default:
				break;
		}
	}

	private void updateCarPreview(int playerIndex, int modelIndex) {
		carIcons[playerIndex].setIcon(UiScale.scaledCarIcon(this, modelIndex + 1, 96, 56));
		int[] stats = Car.CAR_MODEL_STATS[modelIndex];
		for (int statIndex = 0; statIndex < 3; statIndex++) {
			carStatBars[playerIndex][statIndex].setValue(stats[statIndex]);
			carStatBars[playerIndex][statIndex].setString(Integer.toString(stats[statIndex]));
		}
		selectedCarModels[playerIndex] = modelIndex + 1;
	}

	private void updateTrackPreview(int trackIndex) {
		selectedTrack = trackIndex + 1;
		UiScale.fitLabelIcon(
				trackIcon,
				this,
				ResourcePaths.bundledSprite("track_preview" + (trackIndex + 1) + ".png"),
				140,
				96);
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
			updateCarPreview(playerIndex, box.getSelectedIndex());
		} else {
			updateTrackPreview(box.getSelectedIndex());
		}
	}
}
