package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

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
import view.theme.GameTheme;
import view.ui.BackgroundPanel;
import view.ui.UiPainter;

/**
 * Single application window. Menus, Hall of Fame, help, race completion, and
 * the race viewport are all shown by swapping content inside this frame.
 */
public class AppShell extends JFrame implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final String WINDOW_TITLE = GameConfig.GAME_TITLE;
	private static final int MAX_HUMAN_PLAYERS = GameConfig.MAX_HUMAN_PLAYERS;
	private static final int MAX_CARS = GameConfig.MAX_CARS;
	private static final int STAT_COUNT = Car.STAT_COUNT;
	private static final int DEFAULT_SELECTED_INDEX = 0;
	private static final int DEFAULT_SELECTED_TRACK = 0;
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
	private static final int SETUP_PREVIEW_HEIGHT = 110;
	private static final int SETUP_STAT_GAP = 4;
	private static final int VERTICAL_STRUT_MEDIUM = 12;
	private static final int ACTION_BUTTON_COLUMNS = 2;
	private static final int ACTION_BUTTON_GAP = 14;

	private static final int MAIN_BUTTON_WIDTH = 220;
	private static final int MAIN_BUTTON_HEIGHT = 58;
	private static final int ACTION_BUTTON_WIDTH = 200;
	private static final int ACTION_BUTTON_HEIGHT = 56;
	private static final int CAR_PREVIEW_WIDTH = 200;
	private static final int CAR_PREVIEW_HEIGHT = 110;
	/** Fallback track preview width before the column has been laid out. */
	private static final int TRACK_PREVIEW_WIDTH = 200;
	/** Track preview occupies this fraction of the Track/Laps column content width. */
	private static final float TRACK_PREVIEW_COLUMN_WIDTH_RATIO = 0.66f;
	private static final int PREVIEW_WELL_ARC = 12;

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

	private static final String CARD_MAIN = "main";
	private static final String CARD_SETUP = "setup";
	private static final String CARD_HALL = "hall";
	private static final String CARD_HELP = "help";
	private static final String CARD_RACE_COMPLETE = "raceComplete";

	private static final String HERO_TITLE = ConfigLoader.getString("messages.menu.hero.title", "SUPER SPRINT");
	private static final String HERO_BRAND = ConfigLoader.getString("messages.menu.hero.brand", "Supélec");
	private static final String BUTTON_ONE_PLAYER = ConfigLoader.getString("messages.menu.button.one.player", "Single Player");
	private static final String BUTTON_TWO_PLAYERS = ConfigLoader.getString("messages.menu.button.two.players", "Multiplayer");
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
	/** Window / taskbar icon sizes derived from the bundled 512×512 PNG. */
	private static final int[] WINDOW_ICON_SIZES = {16, 32, 48, 64, 128, 256, 512};

	private final CardLayout cards = new CardLayout();
	private final JPanel cardRoot = new JPanel(cards);
	private final HallPanel hallPanel;
	private final HelpPanel helpPanel;
	private final HallOfFame hallOfFame;

	private final BackgroundPanel mainPanel;
	private final JPanel buttonPanel;
	private final ArcadeButton[] mainButtons;

	private final BackgroundPanel raceSetupPanel;
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
	private String activeCard = CARD_MAIN;
	private GameFrame activeRaceView;

	@SuppressWarnings({"rawtypes", "unchecked"})
	public AppShell() throws FileNotFoundException {
		super(WINDOW_TITLE);

		hallPanel = new HallPanel(this, this::showMainMenu);
		helpPanel = new HelpPanel(this, this::showMainMenu);
		hallOfFame = new HallOfFame(hallPanel, this::showHallOfFame);

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

		raceSetupPanel = new BackgroundPanel(BackgroundPanel.Style.SCREEN);
		raceSetupPanel.setLayout(new BorderLayout(RACE_PANEL_GAP, RACE_PANEL_GAP));
		raceSetupPanel.setBorder(new EmptyBorder(PANEL_INSET, PANEL_INSET, PANEL_INSET, PANEL_INSET));

		JPanel raceHeader = ThemedPanel.createHeader(RACE_SETUP_TITLE, null, this);
		raceSetupPanel.add(raceHeader, BorderLayout.NORTH);

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

		trackAndLapsPanel = buildTrackAndLapsSetupCard();
		addSetupCard(setupBody, trackAndLapsPanel, 2);
		trackAndLapsPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				if (CARD_SETUP.equals(activeCard)) {
					refreshTrackPreviewLayout();
				}
			}
		});

		JPanel setupHolder = new JPanel(new BorderLayout());
		setupHolder.setOpaque(false);
		setupHolder.add(setupBody, BorderLayout.NORTH);
		raceSetupPanel.add(setupHolder, BorderLayout.CENTER);

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
		raceSetupPanel.add(actionPanel, BorderLayout.SOUTH);

		initializeRaceSetupDefaults();

		cardRoot.add(mainPanel, CARD_MAIN);
		cardRoot.add(raceSetupPanel, CARD_SETUP);
		cardRoot.add(hallPanel, CARD_HALL);
		cardRoot.add(helpPanel, CARD_HELP);

		setContentPane(cardRoot);
		setIconImages(loadWindowIcons());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);
		setVisible(true);
		UiScale.lockFixedShellSize(this, largestRaceContentSize());
		showMainMenu();
		applyScaledMetrics();
	}

	private static Dimension largestRaceContentSize() {
		int maxRows = 0;
		int maxColumns = 0;
		for (int[][] map : Game.TRACK_MAPS) {
			maxRows = Math.max(maxRows, map.length);
			maxColumns = Math.max(maxColumns, map[0].length);
		}
		return GameFrame.contentSizeFor(maxRows, maxColumns);
	}

	/**
	 * Loads the bundled Flaticon race PNG and builds crisp multi-resolution
	 * window icons (taskbar / title bar pick the nearest size).
	 */
	private static List<Image> loadWindowIcons() {
		List<Image> icons = new ArrayList<>();
		try {
			BufferedImage master = ImageIO.read(new File(ResourcePaths.bundledSprite(SPRITE_ICON)));
			if (master == null) {
				icons.add(new ImageIcon(ResourcePaths.bundledSprite(SPRITE_ICON)).getImage());
				return icons;
			}
			for (int size : WINDOW_ICON_SIZES) {
				BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = scaled.createGraphics();
				graphics.setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics.setRenderingHint(
						RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				graphics.drawImage(master, 0, 0, size, size, null);
				graphics.dispose();
				icons.add(scaled);
			}
		} catch (IOException exception) {
			icons.add(new ImageIcon(ResourcePaths.bundledSprite(SPRITE_ICON)).getImage());
		}
		return icons;
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
		String carPrototype = longestComboLabel(carMenus[0]);
		for (int playerIndex = 0; playerIndex < MAX_HUMAN_PLAYERS; playerIndex++) {
			StyledComboBox.apply(carMenus[playerIndex], this, carPrototype);
			layoutComboBox(carMenus[playerIndex]);
		}
		StyledComboBox.apply(trackMenu, this, longestComboLabel(trackMenu));
		layoutComboBox(trackMenu);
		StyledComboBox.apply(lapMenu, this, longestComboLabel(lapMenu));
		layoutComboBox(lapMenu);
		updateSetupCardHeights();
		if (CARD_SETUP.equals(activeCard)) {
			refreshRaceSetupPreviews();
			SwingUtilities.invokeLater(this::refreshTrackPreviewLayout);
		}
		startButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
		backToMenuButton.applyScaledSize(this, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
		hallPanel.applyScaledMetrics(this);
		helpPanel.applyScaledMetrics(this);
		revalidate();
		repaint();
	}

	private void updateSetupCardHeights() {
		for (JPanel previewPanel : carPreviewPanels) {
			// Car wells stretch to the column width; sprite stays on a fixed canvas.
			lockPreviewSlot(
					previewPanel,
					UiScale.scale(this, CAR_PREVIEW_WIDTH),
					UiScale.scale(this, CAR_PREVIEW_HEIGHT),
					false);
		}
		Dimension trackSize = resolveTrackPreviewSize();
		lockPreviewSlot(trackPreviewPanel, trackSize.width, trackSize.height, true);
		equalizeVisibleSetupCardHeights();
	}

	/**
	 * Track preview is 66% of the Track/Laps column content width, with height
	 * keeping the same aspect as the baseline 200×110 thumbnail.
	 */
	private Dimension resolveTrackPreviewSize() {
		int fallbackWidth = UiScale.scale(this, TRACK_PREVIEW_WIDTH);
		int fallbackHeight = UiScale.scale(this, SETUP_PREVIEW_HEIGHT);
		int columnWidth = trackColumnContentWidth();
		if (columnWidth <= 0) {
			return new Dimension(fallbackWidth, fallbackHeight);
		}
		int width = Math.max(1, Math.round(columnWidth * TRACK_PREVIEW_COLUMN_WIDTH_RATIO));
		// Keep the preview inside the card's padded content area.
		if (trackAndLapsPanel != null) {
			Insets insets = trackAndLapsPanel.getInsets();
			int maxContentWidth = Math.max(
					1,
					trackAndLapsPanel.getWidth() - insets.left - insets.right);
			width = Math.min(width, maxContentWidth);
		}
		int height = Math.max(
				1,
				Math.round(width * (SETUP_PREVIEW_HEIGHT / (float) TRACK_PREVIEW_WIDTH)));
		return new Dimension(width, height);
	}

	/**
	 * Full Track/Laps column width (the card itself). Preview size is 66% of
	 * this, clamped so it still fits inside the card's content insets.
	 */
	private int trackColumnContentWidth() {
		if (trackAndLapsPanel != null && trackAndLapsPanel.getWidth() > 0) {
			return trackAndLapsPanel.getWidth();
		}
		if (setupBody != null && setupBody.getWidth() > 0) {
			int visibleColumns = 1;
			for (GlassCard card : carPanels) {
				if (card != null && card.isVisible()) {
					visibleColumns++;
				}
			}
			int gaps = Math.max(0, visibleColumns - 1) * SETUP_COLUMN_GAP;
			return Math.max(0, (setupBody.getWidth() - gaps) / Math.max(1, visibleColumns));
		}
		return 0;
	}

	/**
	 * Preview wells keep a fixed height so stats / laps never jump when the
	 * selected car or track sprite changes. When {@code centerInColumn} is true
	 * the slot is exactly {@code width}×{@code height}; otherwise width may grow
	 * with the column while height stays locked.
	 */
	private void lockPreviewSlot(JPanel previewPanel, int width, int height, boolean centerInColumn) {
		Dimension fixed = new Dimension(width, height);
		previewPanel.setPreferredSize(fixed);
		previewPanel.setMinimumSize(fixed);
		previewPanel.setMaximumSize(centerInColumn
				? fixed
				: new Dimension(Integer.MAX_VALUE, height));
		previewPanel.setAlignmentX(centerInColumn ? Component.CENTER_ALIGNMENT : Component.LEFT_ALIGNMENT);
	}

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
			// Equalize height only — locking min width to the natural preferred
			// width overflowed the fixed shell when three columns were visible.
			card.setPreferredSize(new Dimension(card.getPreferredSize().width, maxHeight));
			card.setMinimumSize(new Dimension(0, maxHeight));
		}
	}

	private void layoutComboBox(JComboBox<?> comboBox) {
		int comboHeight = UiScale.scale(this, SETUP_COMBO_HEIGHT);
		Dimension comboSize = new Dimension(comboBox.getPreferredSize().width, comboHeight);
		comboBox.setPreferredSize(comboSize);
		comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboHeight));
	}

	private static String longestComboLabel(JComboBox<?> comboBox) {
		String longest = "12 - Vintage Yellow Hot Rod";
		for (int index = 0; index < comboBox.getItemCount(); index++) {
			Object item = comboBox.getItemAt(index);
			if (item == null) {
				continue;
			}
			String text = String.valueOf(item);
			if (text.length() > longest.length()) {
				longest = text;
			}
		}
		return longest;
	}

	private void addSetupCard(JPanel container, JPanel card, int columnIndex) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = columnIndex;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 0.0;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(
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
		carIcons[playerIndex].setOpaque(false);
		JPanel previewPanel = createPreviewWell();
		previewPanel.add(carIcons[playerIndex], BorderLayout.CENTER);
		carPreviewPanels[playerIndex] = previewPanel;
		lockPreviewSlot(
				previewPanel,
				UiScale.scale(this, CAR_PREVIEW_WIDTH),
				UiScale.scale(this, CAR_PREVIEW_HEIGHT),
				false);

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
		stretchHorizontally(carMenus[playerIndex]);
		stretchHorizontally(statsPanel);
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
		trackIcon.setOpaque(false);
		JPanel previewPanel = createPreviewWell();
		previewPanel.add(trackIcon, BorderLayout.CENTER);
		trackPreviewPanel = previewPanel;
		Dimension trackSize = resolveTrackPreviewSize();
		lockPreviewSlot(previewPanel, trackSize.width, trackSize.height, true);

		lapMenu = new JComboBox(GameCatalog.lapCountOptions());
		lapMenu.addItemListener(this);
		lapMenu.setName(COMBO_NAME_LAPS);

		JLabel lapsLabel = ThemedPanel.createLabel(LAPS_SECTION_TITLE, this);
		lapsLabel.setForeground(GameTheme.ACCENT_YELLOW);
		lapsLabel.setFont(GameTheme.scaled(GameTheme.FONT_SUBTITLE, this));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		stretchHorizontally(trackMenu);
		stretchHorizontally(lapsLabel);
		stretchHorizontally(lapMenu);
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

	private JPanel createPreviewWell() {
		JPanel previewPanel = new JPanel(new BorderLayout()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D graphics2D = (Graphics2D) graphics.create();
				UiPainter.paintPreviewWell(graphics2D, 0, 0, getWidth(), getHeight(), PREVIEW_WELL_ARC);
				graphics2D.dispose();
				super.paintComponent(graphics);
			}
		};
		previewPanel.setOpaque(false);
		return previewPanel;
	}

	/**
	 * BoxLayout only grows children up to their maximum size; unset maxima fall
	 * back to preferred size and leave combo/stat rows narrow and clipped.
	 * Height stays unbounded here — preview slots use {@link #lockPreviewSlot}.
	 */
	private static void stretchHorizontally(javax.swing.JComponent component) {
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

	private void showCard(String cardName) {
		clearRaceView();
		if (!cardRoot.equals(getContentPane())) {
			setContentPane(cardRoot);
		}
		activeCard = cardName;
		cards.show(cardRoot, cardName);
		revalidate();
		repaint();
	}

	public void showMainMenu() {
		setTitle(WINDOW_TITLE);
		showCard(CARD_MAIN);
		applyScaledMetrics();
		toFront();
	}

	private void showRaceSetup(int players) {
		boolean singlePlayer = players == SINGLE_PLAYER_COUNT;
		carMenus[1].setEnabled(!singlePlayer);
		carPanels[1].setVisible(!singlePlayer);
		GridBagLayout layout = (GridBagLayout) setupBody.getLayout();
		GridBagConstraints playerTwoConstraints = layout.getConstraints(carPanels[1]);
		playerTwoConstraints.weightx = singlePlayer ? 0.0 : 1.0;
		layout.setConstraints(carPanels[1], playerTwoConstraints);
		humanPlayerCount = players;
		setTitle(WINDOW_TITLE + " — " + RACE_SETUP_TITLE);
		showCard(CARD_SETUP);
		applyScaledMetrics();
		refreshRaceSetupPreviews();
		// Column width is only final after the first layout pass — rebuild the
		// track preview at 66% of that width once the card has a real size.
		SwingUtilities.invokeLater(this::refreshTrackPreviewLayout);
	}

	public void showHallOfFame() {
		setTitle(WINDOW_TITLE + " — " + ConfigLoader.getString("messages.hall.header.title", "Hall of Fame"));
		hallPanel.refreshOnShow();
		showCard(CARD_HALL);
		applyScaledMetrics();
		toFront();
	}

	public void showHelp() {
		setTitle(WINDOW_TITLE + " — " + ConfigLoader.getString("messages.help.dialog.title", "Help"));
		showCard(CARD_HELP);
		applyScaledMetrics();
	}

	/**
	 * Installs the race canvas as the sole content of this window without
	 * changing the fixed shell size.
	 */
	public void showRace(GameFrame raceView, String raceTitle) {
		clearRaceView();
		activeRaceView = raceView;
		activeCard = null;
		setTitle(raceTitle);
		JPanel raceHost = new JPanel(new BorderLayout());
		raceHost.setOpaque(true);
		raceHost.setBackground(GameTheme.BACKGROUND_DARK);
		raceHost.add(raceView, BorderLayout.CENTER);
		setContentPane(raceHost);
		revalidate();
		repaint();
		raceView.realizeBufferStrategy();
		toFront();
	}

	public void showRaceComplete(
			HallOfFame hallOfFame,
			int winnerIndex,
			int humanPlayerCount,
			double durationMs,
			int lapCount,
			int trackIndex) {
		clearRaceView();
		RaceCompletePanel panel = new RaceCompletePanel(
				this,
				hallOfFame,
				winnerIndex,
				humanPlayerCount,
				durationMs,
				lapCount,
				trackIndex,
				this::showMainMenu);
		// Replace any previous completion card so each finish gets fresh state.
		Component[] components = cardRoot.getComponents();
		for (Component component : components) {
			if (component instanceof RaceCompletePanel) {
				cardRoot.remove(component);
			}
		}
		cardRoot.add(panel, CARD_RACE_COMPLETE);
		setTitle(WINDOW_TITLE + " — " + ConfigLoader.getString("messages.race.complete.title", "Race Complete"));
		showCard(CARD_RACE_COMPLETE);
		panel.applyScaledMetrics(this);
		applyScaledMetrics();
	}

	private void clearRaceView() {
		if (activeRaceView != null) {
			activeRaceView.shutdown();
			activeRaceView = null;
		}
	}

	void openRaceSetupForScreenshot() {
		showRaceSetup(SINGLE_PLAYER_COUNT);
	}

	void openHelpForScreenshot() {
		showHelp();
	}

	void openHallOfFameForScreenshot() {
		showHallOfFame();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (!(event.getSource() instanceof ArcadeButton button)) {
			return;
		}
		switch (button.getMnemonic()) {
			case MNEMONIC_ONE_PLAYER:
				showRaceSetup(SINGLE_PLAYER_COUNT);
				break;
			case MNEMONIC_TWO_PLAYERS:
				showRaceSetup(MAX_HUMAN_PLAYERS);
				break;
			case MNEMONIC_HALL_OF_FAME:
				showHallOfFame();
				break;
			case MNEMONIC_HELP:
				showHelp();
				break;
			case MNEMONIC_START_RACE:
				int[] carModels = new int[MAX_CARS];
				for (int playerIndex = 0; playerIndex < humanPlayerCount; playerIndex++) {
					carModels[playerIndex] = selectedCarModels[playerIndex];
				}
				Random random = new Random();
				for (int aiIndex = humanPlayerCount; aiIndex < MAX_CARS; aiIndex++) {
					carModels[aiIndex] = random.nextInt(Car.CAR_MODEL_COUNT);
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
				UiScale.scaledCarIcon(this, modelIndex, CAR_PREVIEW_WIDTH, CAR_PREVIEW_HEIGHT));
		for (int statIndex = 0; statIndex < STAT_COUNT; statIndex++) {
			carStatBars[playerIndex][statIndex].setValue(Car.getModelStat(modelIndex, statIndex));
		}
		selectedCarModels[playerIndex] = modelIndex;
	}

	private void updateTrackPreview(int trackIndex) {
		selectedTrack = trackIndex;
		Dimension size = resolveTrackPreviewSize();
		lockPreviewSlot(trackPreviewPanel, size.width, size.height, true);
		trackIcon.setIcon(new ImageIcon(TrackPreviewRenderer.render(
				trackIndex,
				Game.TRACK_MAPS[trackIndex],
				size.width,
				size.height)));
	}

	/** Re-measure the Track column and rebuild the preview after layout settles. */
	private void refreshTrackPreviewLayout() {
		if (!CARD_SETUP.equals(activeCard) || trackMenu == null || trackPreviewPanel == null) {
			return;
		}
		int trackIndex = trackMenu.getSelectedIndex();
		if (trackIndex < 0) {
			return;
		}
		Dimension nextSize = resolveTrackPreviewSize();
		Dimension currentSize = trackPreviewPanel.getPreferredSize();
		if (currentSize != null
				&& currentSize.width == nextSize.width
				&& currentSize.height == nextSize.height
				&& trackIcon.getIcon() != null
				&& trackIcon.getIcon().getIconWidth() == nextSize.width
				&& trackIcon.getIcon().getIconHeight() == nextSize.height) {
			return;
		}
		updateTrackPreview(trackIndex);
		equalizeVisibleSetupCardHeights();
		setupBody.revalidate();
		setupBody.repaint();
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
