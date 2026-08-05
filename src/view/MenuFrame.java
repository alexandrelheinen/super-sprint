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
import javax.swing.SwingUtilities;

import controller.Game;
import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.GameSettings;
import model.HallOfFame;
import model.ResourcePaths;
import view.components.ArcadeButton;
import view.components.ThemedPanel;
import view.theme.GameTheme;

public class MenuFrame extends JFrame implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final String WINDOW_TITLE = GameSettings.GAME_TITLE;
	private static final int MAX_HUMAN_PLAYERS = GameSettings.MAX_HUMAN_PLAYERS;
	private static final int MAX_CARS = GameSettings.MAX_CARS;
	private static final int STAT_COUNT = Car.STAT_COUNT;
	private static final int DEFAULT_SELECTED_INDEX = 0;
	private static final int DEFAULT_SELECTED_TRACK = 1;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int SINGLE_PLAYER_COUNT = 1;

	private static final int PANEL_INSET = 18;
	private static final int MAIN_PANEL_VERTICAL_GAP = 18;
	private static final int BUTTON_GRID_COLUMNS = 2;
	private static final int BUTTON_GRID_GAP = 14;
	private static final int RACE_PANEL_GAP = 16;
	private static final int SETUP_COLUMN_COUNT = 4;
	private static final int SETUP_COLUMN_GAP = 18;
	private static final int VERTICAL_STRUT_SMALL = 8;
	private static final int VERTICAL_STRUT_MEDIUM = 12;
	private static final int STATS_GRID_ROWS = 3;
	private static final int STATS_GRID_COLUMNS = 2;
	private static final int STATS_GRID_GAP = 8;
	private static final int ACTION_BUTTON_COLUMNS = 2;
	private static final int ACTION_BUTTON_GAP = 14;

	private static final int MENU_IMAGE_WIDTH = 700;
	private static final int MENU_IMAGE_HEIGHT = 300;
	private static final int MAIN_BUTTON_WIDTH = 190;
	private static final int MAIN_BUTTON_HEIGHT = 52;
	private static final int ACTION_BUTTON_WIDTH = 180;
	private static final int ACTION_BUTTON_HEIGHT = 52;
	private static final int CAR_PREVIEW_WIDTH = 96;
	private static final int CAR_PREVIEW_HEIGHT = 56;
	private static final int TRACK_PREVIEW_WIDTH = 140;
	private static final int TRACK_PREVIEW_HEIGHT = 96;

	private static final int MNEMONIC_ONE_PLAYER = 0;
	private static final int MNEMONIC_TWO_PLAYERS = 1;
	private static final int MNEMONIC_HALL_OF_FAME = 2;
	private static final int MNEMONIC_HELP = 3;
	private static final int MNEMONIC_START_RACE = 10;
	private static final int MNEMONIC_MAIN_MENU = 11;

	private static final String COMBO_NAME_CAR1 = "car1";
	private static final String COMBO_NAME_CAR2 = "car2";
	private static final String COMBO_NAME_TRACK = "track";
	private static final String COMBO_NAME_LAPS = "laps";

	private static final String HERO_TITLE = "SUPER SPRINT SUPELEC";
	private static final String HERO_SUBTITLE = "Arcade top-down racing";
	private static final String BUTTON_ONE_PLAYER = "1 Player";
	private static final String BUTTON_TWO_PLAYERS = "2 Players";
	private static final String BUTTON_HALL_OF_FAME = "Hall of Fame";
	private static final String BUTTON_HELP = "Help & Info";
	private static final String RACE_SETUP_TITLE = "Race Setup";
	private static final String RACE_SETUP_SUBTITLE = "Choose cars, track, laps, and launch the grid";
	private static final String PLAYER_SECTION_PREFIX = "Player ";
	private static final String TRACK_SECTION_TITLE = "Track";
	private static final String LAPS_SECTION_TITLE = "Laps";
	private static final String BUTTON_START_RACE = "Start Race";
	private static final String BUTTON_MAIN_MENU = "Main Menu";
	private static final String[] STAT_LABELS = {"Acceleration", "Top Speed", "Handling"};
	private static final int[][] STAT_BAR_LIMITS = {{100, 250}, {200, 400}, {30, 60}};

	private static final String SPRITE_ICON = "icon.png";
	private static final String SPRITE_MENU = "menu.png";
	private static final String TRACK_PREVIEW_PREFIX = "track_preview";
	private static final String TRACK_PREVIEW_SUFFIX = ".png";

	private static final String HELP_MESSAGE = "SUPER SPRINT SUPELEC\n"
			+ "_______________________________________\n"
			+ "GENERAL INFORMATION:\n\n"
			+ "Software Project 2014/2015 - Sequence 6\n"
			+ "Version from 14.01.14\n"
			+ "_______________________________________\n"
			+ "CONTROLS:\n\n"
			+ "Player 1: arrow keys\n"
			+ "Player 2: W/A/S/D keys\n"
			+ "_______________________________________\n"
			+ "Choose the number of laps in the race setup screen.";

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

	private final ThemedPanel lapsPanel;
	@SuppressWarnings("rawtypes")
	private final JComboBox lapMenu;

	private final ArcadeButton startButton;
	private final ArcadeButton backToMenuButton;

	private int selectedTrack = DEFAULT_SELECTED_TRACK;
	private int selectedLapCount = GameCatalog.DEFAULT_LAP_COUNT;
	private final int[] selectedCarModels = new int[MAX_HUMAN_PLAYERS];
	private int humanPlayerCount;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public MenuFrame() throws FileNotFoundException {
		super(WINDOW_TITLE);

		hallFrame = new HallFrame(this);
		hallOfFame = new HallOfFame(hallFrame);

		mainPanel = new ThemedPanel();
		mainPanel.setLayout(new BorderLayout(0, MAIN_PANEL_VERTICAL_GAP));
		mainPanel.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));
		((ThemedPanel) mainPanel).styleSurface(GameTheme.BACKGROUND_DARK);

		menuImageLabel = new JLabel("", JLabel.CENTER);
		JPanel heroPanel = new ThemedPanel();
		heroPanel.setLayout(new BorderLayout());
		heroPanel.setOpaque(false);
		heroPanel.add(ThemedPanel.createHeader(HERO_TITLE, HERO_SUBTITLE, this), BorderLayout.NORTH);
		heroPanel.add(menuImageLabel, BorderLayout.CENTER);

		buttonPanel = new JPanel(new GridLayout(BUTTON_GRID_COLUMNS, BUTTON_GRID_COLUMNS, BUTTON_GRID_GAP, BUTTON_GRID_GAP));
		buttonPanel.setOpaque(false);
		mainButtons = new ArcadeButton[] {
				new ArcadeButton(BUTTON_ONE_PLAYER),
				new ArcadeButton(BUTTON_TWO_PLAYERS),
				new ArcadeButton(BUTTON_HALL_OF_FAME, false),
				new ArcadeButton(BUTTON_HELP, false)
		};
		for (int index = 0; index < mainButtons.length; index++) {
			buttonPanel.add(mainButtons[index]);
			mainButtons[index].addActionListener(this);
			mainButtons[index].setMnemonic(index);
		}

		mainPanel.add(heroPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		racePanel = new ThemedPanel();
		racePanel.setLayout(new BorderLayout(RACE_PANEL_GAP, RACE_PANEL_GAP));
		racePanel.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));
		((ThemedPanel) racePanel).styleSurface(GameTheme.BACKGROUND_LIGHT);

		JPanel raceHeader = ThemedPanel.createHeader(RACE_SETUP_TITLE, RACE_SETUP_SUBTITLE, this);
		racePanel.add(raceHeader, BorderLayout.NORTH);

		JPanel setupBody = new JPanel(new GridLayout(1, SETUP_COLUMN_COUNT, SETUP_COLUMN_GAP, 0));
		setupBody.setOpaque(false);

		carPanels = new ThemedPanel[MAX_HUMAN_PLAYERS];
		carMenus = new JComboBox[MAX_HUMAN_PLAYERS];
		carStatBars = new JProgressBar[MAX_HUMAN_PLAYERS][STAT_COUNT];
		carIcons = new JLabel[MAX_HUMAN_PLAYERS];

		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			carPanels[playerIndex] = new ThemedPanel();
			carPanels[playerIndex].setLayout(new BoxLayout(carPanels[playerIndex], BoxLayout.Y_AXIS));
			carPanels[playerIndex].setBorder(ThemedPanel.sectionBorder(PLAYER_SECTION_PREFIX + (playerIndex + ONE_BASED_INDEX_OFFSET), this));

			String[] carOptions = GameCatalog.carModelOptions();
			carMenus[playerIndex] = new JComboBox(carOptions);
			carMenus[playerIndex].addItemListener(this);
			carMenus[playerIndex].setName(playerIndex == 0 ? COMBO_NAME_CAR1 : COMBO_NAME_CAR2);
			carIcons[playerIndex] = new JLabel("", JLabel.CENTER);

			carPanels[playerIndex].add(Box.createVerticalStrut(VERTICAL_STRUT_SMALL));
			carPanels[playerIndex].add(carMenus[playerIndex]);
			carPanels[playerIndex].add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
			carPanels[playerIndex].add(carIcons[playerIndex]);
			carPanels[playerIndex].add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));

			JPanel statsPanel = new JPanel(new GridLayout(STATS_GRID_ROWS, STATS_GRID_COLUMNS, STATS_GRID_GAP, STATS_GRID_GAP));
			statsPanel.setOpaque(false);
			for (int statIndex = 0; statIndex < STAT_COUNT; statIndex++) {
				statsPanel.add(ThemedPanel.createLabel(STAT_LABELS[statIndex], this));
				carStatBars[playerIndex][statIndex] = new JProgressBar();
				carStatBars[playerIndex][statIndex].setMinimum(STAT_BAR_LIMITS[statIndex][0]);
				carStatBars[playerIndex][statIndex].setMaximum(STAT_BAR_LIMITS[statIndex][1]);
				carStatBars[playerIndex][statIndex].setStringPainted(true);
				carStatBars[playerIndex][statIndex].setForeground(GameTheme.ACCENT_BLUE);
				statsPanel.add(carStatBars[playerIndex][statIndex]);
			}
			carPanels[playerIndex].add(statsPanel);
			setupBody.add(carPanels[playerIndex]);
		}

		trackPanel = new ThemedPanel();
		trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
		trackPanel.setBorder(ThemedPanel.sectionBorder(TRACK_SECTION_TITLE, this));
		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setName(COMBO_NAME_TRACK);
		trackIcon = new JLabel("", JLabel.CENTER);
		trackPanel.add(Box.createVerticalStrut(VERTICAL_STRUT_SMALL));
		trackPanel.add(trackMenu);
		trackPanel.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
		trackPanel.add(trackIcon);
		setupBody.add(trackPanel);

		lapsPanel = new ThemedPanel();
		lapsPanel.setLayout(new BoxLayout(lapsPanel, BoxLayout.Y_AXIS));
		lapsPanel.setBorder(ThemedPanel.sectionBorder(LAPS_SECTION_TITLE, this));
		lapMenu = new JComboBox(GameCatalog.lapCountOptions());
		lapMenu.addItemListener(this);
		lapMenu.setName(COMBO_NAME_LAPS);
		lapsPanel.add(Box.createVerticalStrut(VERTICAL_STRUT_SMALL));
		lapsPanel.add(lapMenu);
		setupBody.add(lapsPanel);

		racePanel.add(setupBody, BorderLayout.CENTER);

		JPanel actionPanel = new JPanel(new GridLayout(1, ACTION_BUTTON_COLUMNS, ACTION_BUTTON_GAP, 0));
		actionPanel.setOpaque(false);
		startButton = new ArcadeButton(BUTTON_START_RACE);
		startButton.setMnemonic(MNEMONIC_START_RACE);
		startButton.addActionListener(this);
		backToMenuButton = new ArcadeButton(BUTTON_MAIN_MENU, false);
		backToMenuButton.setMnemonic(MNEMONIC_MAIN_MENU);
		backToMenuButton.addActionListener(this);
		actionPanel.add(backToMenuButton);
		actionPanel.add(startButton);
		racePanel.add(actionPanel, BorderLayout.SOUTH);

		initializeRaceSetupDefaults();

		setContentPane(mainPanel);
		showMainMenu();
		setIconImage(new ImageIcon(ResourcePaths.bundledSprite(SPRITE_ICON)).getImage());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		UiScale.enableDelayedResize(this, this::applyScaledMetrics);
		setVisible(true);
	}

	private void initializeRaceSetupDefaults() {
		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			carMenus[playerIndex].setSelectedIndex(DEFAULT_SELECTED_INDEX);
		}
		trackMenu.setSelectedIndex(DEFAULT_SELECTED_INDEX);
		lapMenu.setSelectedIndex(GameCatalog.defaultLapCountOptionIndex());
		refreshRaceSetupPreviews();
	}

	private void refreshRaceSetupPreviews() {
		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			int modelIndex = carMenus[playerIndex].getSelectedIndex();
			if (modelIndex >= 0) {
				updateCarPreview(playerIndex, modelIndex);
			}
		}
		int trackIndex = trackMenu.getSelectedIndex();
		if (trackIndex >= 0) {
			updateTrackPreview(trackIndex);
		}
		int lapIndex = lapMenu.getSelectedIndex();
		if (lapIndex >= 0) {
			selectedLapCount = GameCatalog.lapCountAt(lapIndex);
		}
	}

	private void applyScaledMetrics() {
		UiScale.fitLabelIcon(menuImageLabel, this, ResourcePaths.bundledSprite(SPRITE_MENU), MENU_IMAGE_WIDTH, MENU_IMAGE_HEIGHT);
		for (ArcadeButton button : mainButtons) {
			button.applyScaledSize(this, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT);
		}
		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			styleComboBox(carMenus[playerIndex]);
			styleProgressBars(playerIndex);
		}
		styleComboBox(trackMenu);
		styleComboBox(lapMenu);
		if (racePanel.isShowing()) {
			refreshRaceSetupPreviews();
		}
		startButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
		backToMenuButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
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
		carMenus[1].setEnabled(players != SINGLE_PLAYER_COUNT);
		carPanels[1].setVisible(players != SINGLE_PLAYER_COUNT);
		humanPlayerCount = players;
		setContentPane(racePanel);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		revalidate();
		repaint();
		refreshRaceSetupPreviews();
		SwingUtilities.invokeLater(this::refreshRaceSetupPreviews);
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
			case MNEMONIC_ONE_PLAYER:
				showRaceMenu(SINGLE_PLAYER_COUNT);
				break;
			case MNEMONIC_TWO_PLAYERS:
				showRaceMenu(MAX_HUMAN_PLAYERS);
				break;
			case MNEMONIC_HALL_OF_FAME:
				hallFrame.showHall();
				break;
			case MNEMONIC_HELP:
				JOptionPane.showMessageDialog(this, HELP_MESSAGE);
				break;
			case MNEMONIC_START_RACE:
				setVisible(false);
				int[] carModels = new int[MAX_CARS];
				for (int playerIndex = 0; playerIndex < humanPlayerCount; playerIndex++) {
					carModels[playerIndex] = selectedCarModels[playerIndex];
				}
				Random random = new Random();
				for (int aiIndex = humanPlayerCount; aiIndex < MAX_CARS; aiIndex++) {
					carModels[aiIndex] = random.nextInt(Car.CAR_MODEL_COUNT) + ONE_BASED_INDEX_OFFSET;
				}
				new Game(carModels, selectedTrack, humanPlayerCount, selectedLapCount, hallOfFame, this);
				break;
			case MNEMONIC_MAIN_MENU:
				showMainMenu();
				break;
			default:
				break;
		}
	}

	private void updateCarPreview(int playerIndex, int modelIndex) {
		carIcons[playerIndex].setIcon(
				UiScale.scaledCarIcon(this, modelIndex + ONE_BASED_INDEX_OFFSET, CAR_PREVIEW_WIDTH, CAR_PREVIEW_HEIGHT));
		int[] stats = Car.CAR_MODEL_STATS[modelIndex];
		for (int statIndex = 0; statIndex < STAT_COUNT; statIndex++) {
			carStatBars[playerIndex][statIndex].setValue(stats[statIndex]);
			carStatBars[playerIndex][statIndex].setString(Integer.toString(stats[statIndex]));
		}
		selectedCarModels[playerIndex] = modelIndex + ONE_BASED_INDEX_OFFSET;
	}

	private void updateTrackPreview(int trackIndex) {
		selectedTrack = trackIndex + ONE_BASED_INDEX_OFFSET;
		UiScale.fitLabelIcon(
				trackIcon,
				this,
				ResourcePaths.bundledSprite(TRACK_PREVIEW_PREFIX + (trackIndex + ONE_BASED_INDEX_OFFSET) + TRACK_PREVIEW_SUFFIX),
				TRACK_PREVIEW_WIDTH,
				TRACK_PREVIEW_HEIGHT);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void itemStateChanged(ItemEvent event) {
		if (event.getStateChange() != ItemEvent.SELECTED) {
			return;
		}
		JComboBox box = (JComboBox) event.getSource();
		String name = box.getName();
		switch (name) {
			case COMBO_NAME_CAR1 -> updateCarPreview(0, box.getSelectedIndex());
			case COMBO_NAME_CAR2 -> updateCarPreview(1, box.getSelectedIndex());
			case COMBO_NAME_TRACK -> updateTrackPreview(box.getSelectedIndex());
			case COMBO_NAME_LAPS -> selectedLapCount = GameCatalog.lapCountAt(box.getSelectedIndex());
			default -> {
			}
		}
	}
}
