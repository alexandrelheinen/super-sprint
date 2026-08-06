package controller;

import model.ReferencePath;

/**
 * Computes speed and turn-rate commands that follow a {@link ReferencePath}.
 */
public interface PathFollowController {

	/**
	 * @return {@code [speedCommand, turnRateCommand]}
	 */
	double[] track(
			double x,
			double y,
			double heading,
			double speed,
			ReferencePath path,
			double deltaSeconds);
}
