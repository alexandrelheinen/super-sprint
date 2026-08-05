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
import view.theme.GameTheme;
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
	private static final int SETUP_COLUMN_COUNT = 3;
	private static final int SETUP_COLUMN_GAP = 16;
	private static final int SETUP_ROW_GAP = 16;
	private static final int SETUP_COMBO_HEIGHT = 44;
	private static final int SETUP_PREVIEW_HEIGHT = 84;
	private static final int SETUP_STAT_GAP = 4;
	private static final int VERTICAL_STRUT_MEDIUM = 12;
	private static final int ACTION_BUTTON_COLUMNS = 2;
	private static final int ACTION_BUTTON_GAP = 14;

	private static final int MAIN_BUTTON_WIDTH = 220;
	private static final int MAIN_BUTTON_HEIGHT = 58;
	private static final int ACTION_BUTTON_WIDTH = 200;
	private static final int ACTION_BUTTON_HEIGHT = 56;
	private static final int CAR_PREVIEW_WIDTH = 130;
	private static final int CAR_PREVIEW_HEIGHT = 72;
	private static final int TRACK_PREVIEW_WIDTH = 170;
	private static final int TRACK_PREVIEW_HEIGHT = 110;

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

	private static final String HERO_TITLE = ConfigLoader.getString("messages.menu.hero.title", "SUPER SPRINT");
	private static final String HERO_BRAND = ConfigLoader.getString("messages.menu.hero.brand", "Supélec");
	private static final String BUTTON_ONE_PLAYER = ConfigLoader.getString("messages.menu.button.one.player", "1 Player");
	private static final String BUTTON_TWO_PLAYERS = ConfigLoader.getString("messages.menu.button.two.players", "2 Players");
	private static final String BUTTON_HALL_OF_FAME = ConfigLoader.getString("messages.menu.button.hall", "Hall of Fame");
	private static final String BUTTON_HELP = ConfigLoader.getString("messages.menu.button.help", "Help");
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
	private final JPanel[] carPreviewPanels;

	private final GlassCard trackAndLapsPanel;
	@SuppressWarnings("rawtypes")
	private JComboBox trackMenu;
	private JLabel trackIcon;
	private JPanel trackPreviewPanel;

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
		heroPanel.add(new HeroBanner(this, HERO_TITLE, HERO_BRAND), BorderLayout.CENTER);

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
		carPreviewPanels = new JPanel[MAX_HUMAN_PLAYERS];

		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			String sectionTitle = PLAYER_SECTION_PREFIX + (playerIndex + ONE_BASED_INDEX_OFFSET);
			carPanels[playerIndex] = buildCarSetupCard(sectionTitle, playerIndex);
			addSetupCard(setupBody, carPanels[playerIndex], playerIndex);
		}

		// Track and laps share one top-aligned column card.
		trackAndLapsPanel = buildTrackAndLapsSetupCard();
		addSetupCard(setupBody, trackAndLapsPanel, 2);

		// Keep setup content at the top; card heights are equalized explicitly.
		JPanel setupHolder = new JPanel(new BorderLayout());
		setupHolder.setOpaque(false);
		setupHolder.add(setupBody, BorderLayout.NORTH);
		racePanel.add(setupHolder, BorderLayout.CENTER);

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
		int previewHeight = UiScale.scale(this, SETUP_PREVIEW_HEIGHT);
		for (JPanel previewPanel : carPreviewPanels) {
			previewPanel.setPreferredSize(new Dimension(UiScale.scale(this, CAR_PREVIEW_WIDTH), previewHeight));
			previewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, previewHeight));
			previewPanel.setMinimumSize(new Dimension(UiScale.scale(this, CAR_PREVIEW_WIDTH), previewHeight));
		}
		trackPreviewPanel.setPreferredSize(new Dimension(UiScale.scale(this, TRACK_PREVIEW_WIDTH), previewHeight));
		trackPreviewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, previewHeight));
		trackPreviewPanel.setMinimumSize(new Dimension(UiScale.scale(this, TRACK_PREVIEW_WIDTH), previewHeight));
		equalizeVisibleSetupCardHeights();
	}

	/**
	 * Makes every visible Race Setup card share the tallest content height so
	 * columns line up. Cards fill that height; their BorderLayout.NORTH bodies
	 * keep controls stacked from the top.
	 */
	private void equalizeVisibleSetupCardHeights() {
		GlassCard[] setupCards = {carPanels[0], carPanels[1], trackAndLapsPanel};
		for (GlassCard card : setupCards) {
			card.setPreferredSize(null);
			card.setMinimumSize(null);
			card.setMaximumSize(null);
		}
		setupBody.invalidate();
		int maxHeight = 0;
		for (GlassCard card : setupCards) {
			if (card.isVisible()) {
				maxHeight = Math.max(maxHeight, card.getPreferredSize().height);
			}
		}
		if (maxHeight <= 0) {
			return;
		}
		for (GlassCard card : setupCards) {
			if (!card.isVisible()) {
				continue;
			}
			Dimension equalSize = new Dimension(card.getPreferredSize().width, maxHeight);
			card.setPreferredSize(equalSize);
			card.setMinimumSize(equalSize);
		}
	}

	private void layoutComboBox(JComboBox<?> comboBox) {
		int comboHeight = UiScale.scale(this, SETUP_COMBO_HEIGHT);
		Dimension comboSize = new Dimension(comboBox.getPreferredSize().width, comboHeight);
		comboBox.setPreferredSize(comboSize);
		comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));
	}

	/**
	 * Setup cards share a common preferred height (via
	 * {@link #equalizeVisibleSetupCardHeights}) and stay top-anchored.
	 */
	private void addSetupCard(JPanel container, JPanel card, int columnIndex) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = columnIndex;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 0.0;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new java.awt.Insets(
				0,
				columnIndex == 0 ? 0 : SETUP_COLUMN_GAP / 2,
				0,
				columnIndex == SETUP_COLUMN_COUNT - 1 ? 0 : SETUP_COLUMN_GAP / 2);
		container.add(card, constraints);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private GlassCard buildCarSetupCard(String sectionTitle, int playerIndex) {
		GlassCard card = new GlassCard(new BorderLayout(0, VERTICAL_STRUT_MEDIUM), this, sectionTitle);

		String[] carOptions = GameCatalog.carModelOptions();
		carMenus[playerIndex] = new JComboBox(carOptions);
		carMenus[playerIndex].addItemListener(this);
		carMenus[playerIndex].setName(playerIndex == 0 ? COMBO_NAME_CAR1 : COMBO_NAME_CAR2);

		carIcons[playerIndex] = new JLabel("", JLabel.CENTER);
		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.setOpaque(false);
		previewPanel.setPreferredSize(new Dimension(CAR_PREVIEW_WIDTH, SETUP_PREVIEW_HEIGHT));
		previewPanel.add(carIcons[playerIndex], BorderLayout.CENTER);
		carPreviewPanels[playerIndex] = previewPanel;

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
			if (statIndex < STAT_COUNT - 1) {
				statsPanel.add(Box.createVerticalStrut(SETUP_STAT_GAP));
			}
		}

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		carMenus[playerIndex].setAlignmentX(Component.LEFT_ALIGNMENT);
		previewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(carMenus[playerIndex]);
		body.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
		body.add(previewPanel);
		body.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
		body.add(statsPanel);
		card.add(body, BorderLayout.NORTH);
		return card;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private GlassCard buildTrackAndLapsSetupCard() {
		GlassCard card = new GlassCard(new BorderLayout(0, VERTICAL_STRUT_MEDIUM), this, TRACK_SECTION_TITLE);
		String[] trackOptions = GameCatalog.trackOptions();
		trackMenu = new JComboBox(trackOptions);
		trackMenu.addItemListener(this);
		trackMenu.setName(COMBO_NAME_TRACK);

		trackIcon = new JLabel("", JLabel.CENTER);
		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.setOpaque(false);
		previewPanel.setPreferredSize(new Dimension(TRACK_PREVIEW_WIDTH, SETUP_PREVIEW_HEIGHT));
		previewPanel.add(trackIcon, BorderLayout.CENTER);
		trackPreviewPanel = previewPanel;

		lapMenu = new JComboBox(GameCatalog.lapCountOptions());
		lapMenu.addItemListener(this);
		lapMenu.setName(COMBO_NAME_LAPS);

		JLabel lapsLabel = ThemedPanel.createLabel(LAPS_SECTION_TITLE, this);
		lapsLabel.setForeground(GameTheme.ACCENT_YELLOW);
		lapsLabel.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, this));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		trackMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
		previewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		lapsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		lapMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
		body.add(trackMenu);
		body.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
		body.add(previewPanel);
		body.add(Box.createVerticalStrut(SETUP_ROW_GAP));
		body.add(lapsLabel);
		body.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
		body.add(lapMenu);
		card.add(body, BorderLayout.NORTH);
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
		UiScale.applyRaceSetupSize(this);
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

	void openRaceSetupForScreenshot() {
		showRaceMenu(SINGLE_PLAYER_COUNT);
	}

	void openHelpForScreenshot() {
		HelpDialog helpDialog = new HelpDialog(this);
		helpDialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
		helpDialog.setVisible(true);
	}

	void openHallOfFameForScreenshot() {
		hallFrame.showHall();
		setVisible(false);
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
		trackIcon.setIcon(new ImageIcon(TrackPreviewRenderer.render(
				Game.TRACK_MAPS[trackIndex],
				UiScale.scale(this, TRACK_PREVIEW_WIDTH),
				UiScale.scale(this, SETUP_PREVIEW_HEIGHT))));
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
