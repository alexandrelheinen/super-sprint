package controller;

import model.DubinsVehicle;

/**
 * Pure pursuit path-tracking controller for a unicycle vehicle.
 *
 * <p>Turn rate follows {@code omega = speed * curvature} with
 * {@code curvature = 2 * sin(alpha) / lookaheadDistance}, matching ARCO.
 */
public class PurePursuitController {

	private final double lookaheadDistance;
	private double crossTrackError;
	private double headingError;
	private double curvature;

	public PurePursuitController(double lookaheadDistance) {
		this.lookaheadDistance = lookaheadDistance;
	}

	public double getCrossTrackError() {
		return crossTrackError;
	}

	public double getHeadingError() {
		return headingError;
	}

	public double getCurvature() {
		return curvature;
	}

	/**
	 * Compute pure pursuit speed and turn-rate commands.
	 *
	 * @return {@code [speedCommand, turnRateCommand]}
	 */
	public double[] track(double x, double y, double heading, double[][] path, double speed) {
		if (path.length < 2) {
			return new double[] {speed, 0.0};
		}

		int closestIndex = findClosestWaypointIndex(x, y, path);
		updateTrackingErrors(x, y, heading, path, closestIndex);

		double[] lookahead = findLookahead(x, y, path, closestIndex, lookaheadDistance);
		double dx = lookahead[0] - x;
		double dy = lookahead[1] - y;
		double dxVehicle = Math.cos(heading) * dx + Math.sin(heading) * dy;
		double dyVehicle = -Math.sin(heading) * dx + Math.cos(heading) * dy;
		double alpha = Math.atan2(dyVehicle, dxVehicle);
		curvature = 2.0 * Math.sin(alpha) / lookaheadDistance;
		double turnRateCommand = speed * curvature;
		return new double[] {speed, turnRateCommand};
	}

	private static int findClosestWaypointIndex(double x, double y, double[][] path) {
		int closestIndex = 0;
		double minDistance = Double.POSITIVE_INFINITY;
		for (int index = 0; index < path.length; index++) {
			double distance = Math.hypot(path[index][0] - x, path[index][1] - y);
			if (distance < minDistance) {
				minDistance = distance;
				closestIndex = index;
			}
		}
		return closestIndex;
	}

	private void updateTrackingErrors(double x, double y, double heading, double[][] path, int closestIndex) {
		double segmentX;
		double segmentY;
		if (closestIndex < path.length - 1) {
			segmentX = path[closestIndex + 1][0] - path[closestIndex][0];
			segmentY = path[closestIndex + 1][1] - path[closestIndex][1];
		} else {
			segmentX = path[closestIndex][0] - path[closestIndex - 1][0];
			segmentY = path[closestIndex][1] - path[closestIndex - 1][1];
		}

		double segmentLength = Math.hypot(segmentX, segmentY);
		if (segmentLength > 1e-9) {
			double normalX = -segmentY / segmentLength;
			double normalY = segmentX / segmentLength;
			crossTrackError = normalX * (x - path[closestIndex][0]) + normalY * (y - path[closestIndex][1]);
			headingError = DubinsVehicle.wrapAngle(heading - Math.atan2(segmentY, segmentX));
		} else {
			crossTrackError = 0.0;
			headingError = 0.0;
		}
	}

	private static double[] findLookahead(
			double x,
			double y,
			double[][] path,
			int startIndex,
			double lookahead) {
		int firstSegment = Math.max(0, startIndex - 1);
		for (int index = firstSegment; index < path.length - 1; index++) {
			double endX = path[index + 1][0];
			double endY = path[index + 1][1];
			if (Math.hypot(endX - x, endY - y) >= lookahead) {
				double[] intersection = circleSegmentIntersection(
						x,
						y,
						lookahead,
						path[index][0],
						path[index][1],
						endX,
						endY);
				if (intersection != null) {
					return intersection;
				}
			}
		}

		int nextIndex = Math.min(startIndex + 1, path.length - 1);
		return path[nextIndex].clone();
	}

	private static double[] circleSegmentIntersection(
			double centerX,
			double centerY,
			double radius,
			double startX,
			double startY,
			double endX,
			double endY) {
		double dx = endX - startX;
		double dy = endY - startY;
		double fx = startX - centerX;
		double fy = startY - centerY;

		double a = dx * dx + dy * dy;
		if (a < 1e-12) {
			return null;
		}

		double b = 2.0 * (fx * dx + fy * dy);
		double c = fx * fx + fy * fy - radius * radius;
		double discriminant = b * b - 4.0 * a * c;
		if (discriminant < 0.0) {
			return null;
		}

		double sqrtDiscriminant = Math.sqrt(discriminant);
		double t2 = (-b + sqrtDiscriminant) / (2.0 * a);
		double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
		for (double t : new double[] {t2, t1}) {
			if (t >= 0.0 && t <= 1.0) {
				return new double[] {startX + t * dx, startY + t * dy};
			}
		}
		return null;
	}
}
