package view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Captures PNG screenshots of each major UI screen for visual verification.
 */
public final class UiScreenshotCapture {

	private UiScreenshotCapture() {
	}

	public static void main(String[] args) throws Exception {
		Path outputDirectory = Path.of(args.length > 0 ? args[0] : "artifacts/screenshots");
		Files.createDirectories(outputDirectory);

		SwingUtilities.invokeAndWait(() -> {
			try {
				captureScreens(outputDirectory);
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		});
	}

	private static void captureScreens(Path outputDirectory) throws IOException {
		AppShell shell = new AppShell();
		paintToFile(shell, outputDirectory.resolve("01-main-menu.png"));

		shell.openRaceSetupForScreenshot();
		paintToFile(shell, outputDirectory.resolve("02-race-setup.png"));

		shell.openHelpForScreenshot();
		paintToFile(shell, outputDirectory.resolve("03-help.png"));

		shell.openHallOfFameForScreenshot();
		paintToFile(shell, outputDirectory.resolve("04-hall-of-fame.png"));

		shell.dispose();
		System.out.println("Screenshots saved to " + outputDirectory.toAbsolutePath());
	}

	private static void paintToFile(JFrame frame, Path outputFile) throws IOException {
		frame.validate();
		frame.repaint();
		writeImage(outputFile, renderComponent(frame));
		System.out.println("Saved " + outputFile);
	}

	private static BufferedImage renderComponent(java.awt.Component component) {
		int width = Math.max(component.getWidth(), 1);
		int height = Math.max(component.getHeight(), 1);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		component.paint(graphics);
		graphics.dispose();
		return image;
	}

	private static void writeImage(Path outputFile, BufferedImage image) throws IOException {
		ImageIO.write(image, "png", outputFile.toFile());
	}
}
