package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import model.ResourcePaths;

public class GameFrame extends JFrame implements Observer {

	public static final int TILE_SIZE = 219;
	public static final int[] CAR_RENDER_OFFSET = {40, 29};
	private static final long serialVersionUID = 1L;

	private final BufferedImage[] carSprites;
	private final BufferedImage[][] trackTiles;
	private BufferedImage backgroundTexture;
	private final int[][] trackMap;
	private final boolean[] renderFlags;
	private final int trackNumber;
	private final int[] carModels;
	private final int[] mapDimensions;
	private final Color hudColor;
	private final Font hudFont;
	private volatile boolean renderingEnabled = true;

	public GameFrame(String title, int[] carModels, int[][] trackMap, int trackNumber) {
		super(title);
		this.trackMap = trackMap;
		this.trackNumber = trackNumber;
		this.carModels = carModels.clone();
		mapDimensions = new int[] {trackMap.length, trackMap[0].length};

		renderFlags = new boolean[carModels.length + 1];
		carSprites = new BufferedImage[carModels.length];
		for (int index = 0; index < carModels.length; index++) {
			try {
				carSprites[index] = ResourcePaths.loadCarSprite(carModels[index]);
				System.out.println("Sprite #" + (index + 1) + " loaded");
			} catch (IOException exception) {
				System.err.println("Error loading car sprites: " + exception.getMessage());
			}
		}
		System.out.println(" **************** \n");

		trackTiles = loadTrackTiles(trackMap);
		try {
			backgroundTexture = ImageIO.read(new File(ResourcePaths.bundledSprite("texture.png")));
		} catch (Exception exception) {
			System.err.println("Error loading background texture: " + exception.getMessage());
		}

		hudColor = new Color(0, 90, 180);
		hudFont = new Font("Segoe UI", Font.BOLD, 20);

		setIconImage(new ImageIcon(ResourcePaths.bundledSprite("icon.png")).getImage());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setSize(20 + mapDimensions[1] * TILE_SIZE, 50 + 20 + mapDimensions[0] * TILE_SIZE);
		setVisible(true);
		setResizable(false);
		createBufferStrategy(2);
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public int[][] getTrackMap() {
		return trackMap;
	}

	public void shutdown() {
		renderingEnabled = false;
		setVisible(false);
	}

	@Override
	public void update(Observable observable, Object argument) {
		if (!renderingEnabled || !isDisplayable()) {
			return;
		}
		BufferStrategy bufferStrategy = getBufferStrategy();
		if (bufferStrategy == null) {
			return;
		}
		Graphics graphics = null;

		if (observable.toString().contains("Car")) {
			try {
				Car car = (Car) observable;
				BufferedImage sprite = carSprites[car.getSpriteIndex()];
				graphics = bufferStrategy.getDrawGraphics();
				Graphics2D graphics2D = (Graphics2D) graphics;

				AffineTransform transform = new AffineTransform();
				transform.rotate(car.getAngle(), sprite.getWidth() / 2.0, sprite.getHeight() / 2.0);
				AffineTransformOp rotation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
				graphics2D.drawImage(
						sprite,
						rotation,
						car.getX() - CAR_RENDER_OFFSET[0] / 2,
						car.getY() - CAR_RENDER_OFFSET[1] / 2);
				graphics2D.drawString(
						car.getName(),
						car.getX() - CAR_RENDER_OFFSET[0] / 2,
						car.getY() - CAR_RENDER_OFFSET[1] / 2);
				graphics2D.setFont(hudFont);
				graphics2D.setColor(hudColor);
				graphics2D.drawString(
						GameCatalog.carModelName(carModels[car.getSpriteIndex()]) + " [P"
								+ car.getName() + ": " + car.getLapCount() + "]",
						2 * TILE_SIZE + 100 * car.getSpriteIndex(),
						mapDimensions[0] * TILE_SIZE + 50);
				renderFlags[car.getSpriteIndex()] = true;
			} finally {
				if (graphics != null) {
					graphics.dispose();
				}
			}
		} else {
			try {
				Circuit circuit = (Circuit) observable;
				graphics = bufferStrategy.getDrawGraphics();
				Graphics2D graphics2D = (Graphics2D) graphics;
				AffineTransformOp identity = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR);
				graphics.drawImage(backgroundTexture, 0, 0, null);

				for (int column = 0; column < mapDimensions[1]; column++) {
					for (int row = 0; row < mapDimensions[0]; row++) {
						graphics2D.drawImage(trackTiles[row][column], identity, column * TILE_SIZE, 10 + row * TILE_SIZE);
					}
				}

				graphics2D.setColor(Color.WHITE);
				for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
					float startX = Circuit.START_POSITIONS[trackNumber - 1][slotIndex][0];
					float startY = Circuit.START_POSITIONS[trackNumber - 1][slotIndex][1];
					graphics2D.drawLine((int) startX - 20, (int) startY - 20, (int) startX + 17, (int) startY - 20);
				}
				graphics2D.setColor(Color.YELLOW);
				graphics2D.drawLine(
						(int) circuit.getFinishLine().getX1(),
						(int) circuit.getFinishLine().getY1(),
						(int) circuit.getFinishLine().getX2(),
						(int) circuit.getFinishLine().getY2());
				graphics2D.setFont(new Font("Segoe UI", Font.BOLD, 20));
				graphics2D.setColor(hudColor);
				graphics2D.drawString(
						"Race Time: " + ((int) circuit.getRaceTimeMs() / 1000) + " s  |  "
								+ GameCatalog.trackName(trackNumber),
						20,
						mapDimensions[0] * TILE_SIZE + 50);
				renderFlags[renderFlags.length - 1] = true;
			} finally {
				if (graphics != null) {
					graphics.dispose();
				}
			}
		}

		if (allFlagsSet(renderFlags)) {
			bufferStrategy.show();
			Toolkit.getDefaultToolkit().sync();
			resetFlags(renderFlags);
		}
	}

	private static boolean allFlagsSet(boolean[] flags) {
		for (boolean flag : flags) {
			if (!flag) {
				return false;
			}
		}
		return true;
	}

	private static void resetFlags(boolean[] flags) {
		for (int index = 0; index < flags.length; index++) {
			flags[index] = false;
		}
	}

	private BufferedImage[][] loadTrackTiles(int[][] map) {
		BufferedImage[][] tiles = new BufferedImage[mapDimensions[0]][mapDimensions[1]];
		for (int column = 0; column < mapDimensions[1]; column++) {
			for (int row = 0; row < mapDimensions[0]; row++) {
				try {
					tiles[row][column] = ImageIO.read(new File(ResourcePaths.bundledSprite("track" + map[row][column] + ".png")));
				} catch (Exception exception) {
					System.err.println("Error loading track tile images: " + exception.getMessage());
				}
			}
		}
		return tiles;
	}
}
