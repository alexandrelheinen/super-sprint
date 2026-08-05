package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingUtilities;

import controller.Game;
import model.Car;
import model.ConfigLoader;
import model.GameCatalog;
import model.GameConfig;
import model.HallOfFame;
import model.ResourcePaths;
import view.components.ArcadeButton;
import view.components.GlassCard;
import view.components.HeroBanner;
import view.components.StatBar;
import view.components.StyledComboBox;
import view.components.ThemedPanel;
import view.dialogs.HelpDialog;
import view.ui.BackgroundPanel;

public class MenuFrame extends JFrame implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final String WINDOW_TITLE = GameConfig.GAME_TITLE;
	private static final int MAX_HUMAN_PLAYERS = GameConfig.MAX_HUMAN_PLAYERS;
	private static final int MAX_CARS = GameConfig.MAX_CARS;
	private static final int STAT_COUNT = Car.STAT_COUNT;
	private static final int DEFAULT_SELECTED_INDEX = 0;
	private static final int DEFAULT_SELECTED_TRACK = 1;
	private static final int ONE_BASED_INDEX_OFFSET = 1;
	private static final int SINGLE_PLAYER_COUNT = 1;

	private static final int PANEL_INSET = 22;
	private static final int MAIN_PANEL_VERTICAL_GAP = 18;
	private static final int BUTTON_GRID_COLUMNS = 2;
	private static final int BUTTON_GRID_GAP = 14;
	private static final int RACE_PANEL_GAP = 18;
	private static final int SETUP_COLUMN_COUNT = 4;
	private static final int SETUP_COLUMN_GAP = 16;
	private static final int SETUP_CARD_HEIGHT = 360;
	private static final int SETUP_COMBO_HEIGHT = 40;
	private static final int SETUP_PREVIEW_HEIGHT = 120;
	private static final int VERTICAL_STRUT_SMALL = 8;
	private static final int VERTICAL_STRUT_MEDIUM = 12;
	private static final int ACTION_BUTTON_COLUMNS = 2;
	private static final int ACTION_BUTTON_GAP = 14;

	private static final int MAIN_BUTTON_WIDTH = 210;
	private static final int MAIN_BUTTON_HEIGHT = 54;
	private static final int ACTION_BUTTON_WIDTH = 190;
	private static final int ACTION_BUTTON_HEIGHT = 52;
	private static final int CAR_PREVIEW_WIDTH = 110;
	private static final int CAR_PREVIEW_HEIGHT = 64;
	private static final int TRACK_PREVIEW_WIDTH = 150;
	private static final int TRACK_PREVIEW_HEIGHT = 100;

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

	private static final String HERO_TITLE = ConfigLoader.getString("messages.menu.hero.title", "SUPER SPRINT SUPELEC");
	private static final String HERO_SUBTITLE = ConfigLoader.getString("messages.menu.hero.subtitle", "Arcade top-down racing");
	private static final String BUTTON_ONE_PLAYER = ConfigLoader.getString("messages.menu.button.one.player", "1 Player");
	private static final String BUTTON_TWO_PLAYERS = ConfigLoader.getString("messages.menu.button.two.players", "2 Players");
	private static final String BUTTON_HALL_OF_FAME = ConfigLoader.getString("messages.menu.button.hall", "Hall of Fame");
	private static final String BUTTON_HELP = ConfigLoader.getString("messages.menu.button.help", "Help & Info");
	private static final String RACE_SETUP_TITLE = ConfigLoader.getString("messages.menu.race.setup.title", "Race Setup");
	private static final String PLAYER_SECTION_PREFIX = ConfigLoader.getString("messages.menu.section.player.prefix", "Player ");
	private static final String TRACK_SECTION_TITLE = ConfigLoader.getString("messages.menu.section.track", "Track");
	private static final String LAPS_SECTION_TITLE = ConfigLoader.getString("messages.menu.section.laps", "Laps");
	private static final String BUTTON_START_RACE = ConfigLoader.getString("messages.menu.button.start", "Start Race");
	private static final String BUTTON_MAIN_MENU = ConfigLoader.getString("messages.menu.button.main", "Main Menu");
	private static final String[] STAT_LABELS = {
			ConfigLoader.getString("messages.menu.stat.acceleration", "Acceleration (m/s²)"),
			ConfigLoader.getString("messages.menu.stat.top.speed", "Top Speed (m/s)"),
			ConfigLoader.getString("messages.menu.stat.handling", "Handling")
	};
	private static final double[][] STAT_BAR_LIMITS = {
			{8.0, 25.0},
			{20.0, 40.0},
			{30.0, 60.0}
	};
	private static final String[] STAT_VALUE_SUFFIXES = {" m/s²", " m/s", ""};

	private static final String SPRITE_ICON = "icon.png";
	private static final String TRACK_PREVIEW_PREFIX = "track_preview";
	private static final String TRACK_PREVIEW_SUFFIX = ".png";

	private final HallFrame hallFrame;
	private final HallOfFame hallOfFame;

	private final BackgroundPanel mainPanel;
	private final JPanel buttonPanel;
	private final ArcadeButton[] mainButtons;

	private final BackgroundPanel racePanel;
	private final JPanel setupBody;
	private final GlassCard[] carPanels;
	@SuppressWarnings("rawtypes")
	private final JComboBox[] carMenus;
	private final JLabel[] carIcons;
	private final StatBar[][] carStatBars;

	private final GlassCard trackPanel;
	@SuppressWarnings("rawtypes")
	private JComboBox trackMenu;
	private JLabel trackIcon;

	private final GlassCard lapsPanel;
	@SuppressWarnings("rawtypes")
	private JComboBox lapMenu;

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

		mainPanel = new BackgroundPanel(BackgroundPanel.Style.MENU);
		mainPanel.setLayout(new BorderLayout(0, MAIN_PANEL_VERTICAL_GAP));
		mainPanel.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		JPanel heroPanel = new JPanel(new BorderLayout());
		heroPanel.setOpaque(false);
		heroPanel.add(new HeroBanner(this, HERO_TITLE, HERO_SUBTITLE), BorderLayout.CENTER);

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

		racePanel = new BackgroundPanel(BackgroundPanel.Style.SCREEN);
		racePanel.setLayout(new BorderLayout(RACE_PANEL_GAP, RACE_PANEL_GAP));
		racePanel.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		JPanel raceHeader = ThemedPanel.createHeader(RACE_SETUP_TITLE, null, this);
		racePanel.add(raceHeader, BorderLayout.NORTH);

		setupBody = new JPanel(new GridBagLayout());
		setupBody.setOpaque(false);

		carPanels = new GlassCard[MAX_HUMAN_PLAYERS];
		carMenus = new JComboBox[MAX_HUMAN_PLAYERS];
		carStatBars = new StatBar[MAX_HUMAN_PLAYERS][STAT_COUNT];
		carIcons = new JLabel[MAX_HUMAN_PLAYERS];

		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			String sectionTitle = PLAYER_SECTION_PREFIX + (playerIndex + ONE_BASED_INDEX_OFFSET);
			carPanels[playerIndex] = buildCarSetupCard(sectionTitle, playerIndex);
			addSetupCard(setupBody, carPanels[playerIndex], playerIndex);
		}

		trackPanel = buildTrackSetupCard();
		addSetupCard(setupBody, trackPanel, 2);

		lapsPanel = buildLapsSetupCard();
		addSetupCard(setupBody, lapsPanel, 3);

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
		for (ArcadeButton button : mainButtons) {
			button.applyScaledSize(this, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT);
		}
		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			StyledComboBox.apply(carMenus[playerIndex], this);
			layoutComboBox(carMenus[playerIndex]);
		}
		StyledComboBox.apply(trackMenu, this);
		layoutComboBox(trackMenu);
		StyledComboBox.apply(lapMenu, this);
		layoutComboBox(lapMenu);
		updateSetupCardHeights();
		if (racePanel.isShowing()) {
			refreshRaceSetupPreviews();
		}
		startButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
		backToMenuButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
		revalidate();
		repaint();
	}

	private void updateSetupCardHeights() {
		int cardHeight = UiScale.scale(this, SETUP_CARD_HEIGHT);
		Dimension cardSize = new Dimension(0, cardHeight);
		for (GlassCard carPanel : carPanels) {
			carPanel.setPreferredSize(cardSize);
			carPanel.setMinimumSize(cardSize);
		}
		trackPanel.setPreferredSize(cardSize);
		trackPanel.setMinimumSize(cardSize);
		lapsPanel.setPreferredSize(cardSize);
		lapsPanel.setMinimumSize(cardSize);
	}

	private void layoutComboBox(JComboBox<?> comboBox) {
		int comboHeight = UiScale.scale(this, SETUP_COMBO_HEIGHT);
		Dimension comboSize = new Dimension(comboBox.getPreferredSize().width, comboHeight);
		comboBox.setPreferredSize(comboSize);
		comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));
	}

	private void addSetupCard(JPanel container, GlassCard card, int columnIndex) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = columnIndex;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new java.awt.Insets(0, columnIndex == 0 ? 0 : SETUP_COLUMN_GAP / 2, 0, columnIndex == 3 ? 0 : SETUP_COLUMN_GAP / 2);
		container.add(card, constraints);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private GlassCard buildCarSetupCard(String sectionTitle, int playerIndex) {
		GlassCard card = new GlassCard(new BorderLayout(0, VERTICAL_STRUT_MEDIUM), this, sectionTitle);

		String[] carOptions = GameCatalog.carModelOptions();
		carMenus[playerIndex] = new JComboBox(carOptions);
		carMenus[playerIndex].addItemListener(this);
		carMenus[playerIndex].setName(playerIndex == 0 ? COMBO_NAME_CAR1 : COMBO_NAME_CAR2);
		card.add(carMenus[playerIndex], BorderLayout.NORTH);

		carIcons[playerIndex] = new JLabel("", JLabel.CENTER);
		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.setOpaque(false);
		previewPanel.setPreferredSize(new Dimension(CAR_PREVIEW_WIDTH, SETUP_PREVIEW_HEIGHT));
		previewPanel.add(carIcons[playerIndex], BorderLayout.CENTER);
		card.add(previewPanel, BorderLayout.CENTER);

		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
		statsPanel.setOpaque(false);
		for (int statIndex = 0; statIndex < STAT_COUNT; statIndex++) {
			carStatBars[playerIndex][statIndex] = new StatBar(this);
			carStatBars[playerIndex][statIndex].configure(
					STAT_LABELS[statIndex],
					STAT_BAR_LIMITS[statIndex][0],
					STAT_BAR_LIMITS[statIndex][1],
					STAT_VALUE_SUFFIXES[statIndex]);
			statsPanel.add(carStatBars[playerIndex][statIndex]);
			statsPanel.add(Box.createVerticalStrut(6));
		}
		card.add(statsPanel, BorderLayout.SOUTH);
		return card;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private GlassCard buildTrackSetupCard() {
		GlassCard card = new GlassCard(new BorderLayout(0, VERTICAL_STRUT_MEDIUM), this, TRACK_SECTION_TITLE);
		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setName(COMBO_NAME_TRACK);
		card.add(trackMenu, BorderLayout.NORTH);

		trackIcon = new JLabel("", JLabel.CENTER);
		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.setOpaque(false);
		previewPanel.setPreferredSize(new Dimension(TRACK_PREVIEW_WIDTH, SETUP_PREVIEW_HEIGHT));
		previewPanel.add(trackIcon, BorderLayout.CENTER);
		card.add(previewPanel, BorderLayout.CENTER);
		return card;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private GlassCard buildLapsSetupCard() {
		GlassCard card = new GlassCard(new BorderLayout(), this, LAPS_SECTION_TITLE);
		lapMenu = new JComboBox(GameCatalog.lapCountOptions());
		lapMenu.addItemListener(this);
		lapMenu.setName(COMBO_NAME_LAPS);
		JPanel lapSelector = new JPanel(new BorderLayout());
		lapSelector.setOpaque(false);
		lapSelector.add(lapMenu, BorderLayout.NORTH);
		card.add(lapSelector, BorderLayout.NORTH);
		return card;
	}

	private void showMainMenu() {
		setContentPane(mainPanel);
		UiScale.applyQuarterScreenSize(this);
		applyScaledMetrics();
		revalidate();
		repaint();
	}

	private void showRaceMenu(int players) {
		boolean singlePlayer = players == SINGLE_PLAYER_COUNT;
		carMenus[1].setEnabled(!singlePlayer);
		carPanels[1].setVisible(!singlePlayer);
		GridBagLayout layout = (GridBagLayout) setupBody.getLayout();
		GridBagConstraints playerTwoConstraints = layout.getConstraints(carPanels[1]);
		playerTwoConstraints.weightx = singlePlayer ? 0.0 : 1.0;
		layout.setConstraints(carPanels[1], playerTwoConstraints);
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
				new HelpDialog(this).showDialog();
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
		for (int statIndex = 0; statIndex < STAT_COUNT; statIndex++) {
			carStatBars[playerIndex][statIndex].setValue(Car.getModelStat(modelIndex, statIndex));
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
