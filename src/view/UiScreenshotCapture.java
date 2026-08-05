package view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
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
		MenuFrame menuFrame = new MenuFrame();
		paintToFile(menuFrame, outputDirectory.resolve("01-main-menu.png"));

		menuFrame.openRaceSetupForScreenshot();
		paintToFile(menuFrame, outputDirectory.resolve("02-race-setup.png"));

		HelpDialogHolder helpDialog = new HelpDialogHolder(menuFrame);
		helpDialog.show();
		paintToFile(helpDialog.dialog(), outputDirectory.resolve("03-help.png"));
		helpDialog.dispose();

		menuFrame.setVisible(true);
		menuFrame.openHallOfFameForScreenshot();
		JFrame hallFrame = findVisibleFrame("Hall");
		if (hallFrame != null) {
			paintToFile(hallFrame, outputDirectory.resolve("04-hall-of-fame.png"));
			hallFrame.dispose();
		}

		menuFrame.dispose();
		System.out.println("Screenshots saved to " + outputDirectory.toAbsolutePath());
	}

	private static void paintToFile(JFrame frame, Path outputFile) throws IOException {
		frame.validate();
		frame.repaint();
		writeImage(outputFile, renderComponent(frame));
		System.out.println("Saved " + outputFile);
	}

	private static void paintToFile(JDialog dialog, Path outputFile) throws IOException {
		dialog.validate();
		dialog.repaint();
		writeImage(outputFile, renderComponent(dialog));
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

	private static JFrame findVisibleFrame(String titleFragment) {
		for (java.awt.Window window : java.awt.Window.getWindows()) {
			if (window instanceof JFrame frame
					&& frame.isShowing()
					&& frame.getTitle() != null
					&& frame.getTitle().contains(titleFragment)) {
				return frame;
			}
		}
		return null;
	}

	private static final class HelpDialogHolder {
		private final view.dialogs.HelpDialog dialog;

		private HelpDialogHolder(MenuFrame menuFrame) {
			dialog = new view.dialogs.HelpDialog(menuFrame);
			dialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
		}

		private void show() {
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		}

		private JDialog dialog() {
			return dialog;
		}

		private void dispose() {
			dialog.dispose();
		}
	}
}
