package model;

import java.util.List;

/**
 * Sampled nominal path with heading and curvature used by path-following controllers.
 */
public final class ReferencePath {

	private final double[][] samples;
	private final double[] headings;
	private final double[] curvatures;

	private ReferencePath(double[][] samples, double[] headings, double[] curvatures) {
		this.samples = samples;
		this.headings = headings;
		this.curvatures = curvatures;
	}

	public static ReferencePath empty() {
		return new ReferencePath(new double[0][], new double[0], new double[0]);
	}

	public boolean isEmpty() {
		return samples.length == 0;
	}

	public double[][] waypoints() {
		return samples;
	}

	public int sampleCount() {
		return samples.length;
	}

	public double headingAt(int index) {
		return headings[index];
	}

	static ReferencePath fromSamples(List<SampleBuilder> builders) {
		if (builders.isEmpty()) {
			return empty();
		}

		double[][] points = new double[builders.size()][];
		double[] headings = new double[builders.size()];
		double[] curvatures = new double[builders.size()];
		for (int index = 0; index < builders.size(); index++) {
			SampleBuilder builder = builders.get(index);
			points[index] = new double[] {builder.x, builder.y};
			headings[index] = builder.heading;
			curvatures[index] = builder.curvature;
		}
		return new ReferencePath(points, headings, curvatures);
	}

	/** Hint value requesting a full-path projection search. */
	public static final int NO_HINT = -1;

	private static final int FORWARD_SEARCH_WINDOW = 180;
	private static final int BACKWARD_SEARCH_WINDOW = 40;

	/**
	 * Projects a world position onto the closest path sample and returns tracking errors.
	 */
	public Projection project(double x, double y) {
		return project(x, y, NO_HINT);
	}

	/**
	 * Projects onto the path. With {@link #NO_HINT} the whole path is searched;
	 * otherwise the search is limited to a window around {@code hintIndex} so
	 * closed loops keep matching forward progress instead of jumping backwards.
	 */
	public Projection project(double x, double y, int hintIndex) {
		if (samples.length == 0) {
			return new Projection(0, 0.0, 0.0, 0.0, 0.0);
		}

		int firstOffset;
		int lastOffset;
		int normalizedHint;
		if (hintIndex < 0) {
			normalizedHint = 0;
			firstOffset = 0;
			lastOffset = samples.length - 1;
		} else {
			normalizedHint = Math.floorMod(hintIndex, samples.length);
			firstOffset = -Math.min(samples.length - 1, BACKWARD_SEARCH_WINDOW);
			lastOffset = Math.min(samples.length - 1, FORWARD_SEARCH_WINDOW);
		}

		int closestIndex = normalizedHint;
		double minDistance = Double.POSITIVE_INFINITY;
		for (int offset = firstOffset; offset <= lastOffset; offset++) {
			int index = Math.floorMod(normalizedHint + offset, samples.length);
			double distance = Math.hypot(samples[index][0] - x, samples[index][1] - y);
			if (distance < minDistance) {
				minDistance = distance;
				closestIndex = index;
			}
		}

		int nextIndex = (closestIndex + 1) % samples.length;
		double segmentX = samples[nextIndex][0] - samples[closestIndex][0];
		double segmentY = samples[nextIndex][1] - samples[closestIndex][1];
		double segmentLength = Math.hypot(segmentX, segmentY);
		double crossTrackError = 0.0;
		if (segmentLength > 1e-9) {
			double normalX = -segmentY / segmentLength;
			double normalY = segmentX / segmentLength;
			crossTrackError = normalX * (x - samples[closestIndex][0]) + normalY * (y - samples[closestIndex][1]);
		}

		return new Projection(
				closestIndex,
				headings[closestIndex],
				curvatures[closestIndex],
				crossTrackError,
				minDistance);
	}

	public record Projection(
			int closestIndex,
			double referenceHeading,
			double curvature,
			double crossTrackError,
			double distance) {
	}

	static final class SampleBuilder {
		private final double x;
		private final double y;
		private final double heading;
		private final double curvature;

		SampleBuilder(double x, double y, double heading, double curvature) {
			this.x = x;
			this.y = y;
			this.heading = heading;
			this.curvature = curvature;
		}
	}
}
