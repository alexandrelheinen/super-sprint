package controller;

import model.Car;
import model.Circuit;
import model.GameCatalog;
import view.GameFrame;

/**
 * Base race driver. Subclasses supply the HUD label via
 * {@link #driverLabelForSeat(int)} (Replace Conditional with Polymorphism).
 */
public abstract class Controller {

	private static final String LOG_CAR_CREATED = "Car created: ";
	private static final String LOG_START_POSITION = "; start position ";
	private static final String LOG_SEPARATOR = " ------------- ";
	private static int playerCount = 0;
	protected Car car;
	protected GameFrame frame;

	protected Controller(int modelIndex, int startPosition, GameFrame frame, Circuit circuit) {
		playerCount++;
		int seatNumber = playerCount;
		car = new Car(modelIndex, startPosition, driverLabelForSeat(seatNumber), frame, circuit);
		System.out.println(
				LOG_CAR_CREATED
						+ GameCatalog.carModelName(modelIndex)
						+ LOG_START_POSITION
						+ startPosition);
		System.out.println(this.getClass());
		System.out.println(LOG_SEPARATOR);
		this.frame = frame;
	}

	/** HUD / logging label for this seat (human seat index or AI marker). */
	protected abstract String driverLabelForSeat(int seatNumber);

	public Car getCar() {
		return car;
	}

	public static void resetPlayerCount() {
		playerCount = 0;
	}

	/**
	 * Applies driver controls for this tick. Integration and collisions run in
	 * {@link model.PhysicsSimulator#simulateStep}.
	 */
	public void update() {
		// Subclasses set arcade controls; the shared physics step integrates.
	}

	/** Drops throttle, brake and steering so the shared plant can coast cleanly. */
	public void clearDrivingControls() {
		car.clearControls();
	}
}
