package view;

import javax.swing.SwingUtilities;

final class SwingUtilitiesHelper {

	private SwingUtilitiesHelper() {
	}

	static void invokeLater(Runnable task) {
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
}
