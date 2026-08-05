package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only factory for building synthetic {@link ReferencePath} instances
 * from outside the {@code model} package.
 */
public final class TestPaths {

	private TestPaths() {
	}

	/** Straight path heading east along y = 0. */
	public static ReferencePath straightEast(int sampleCount, double spacingMeters) {
		List<ReferencePath.SampleBuilder> samples = new ArrayList<>();
		for (int index = 0; index < sampleCount; index++) {
			samples.add(new ReferencePath.SampleBuilder(index * spacingMeters, 0.0, 0.0, 0.0));
		}
		return ReferencePath.fromSamples(samples);
	}

	/** Closed circle of the given radius centered at the origin, swept clockwise on screen. */
	public static ReferencePath circle(double radiusMeters, int sampleCount) {
		List<ReferencePath.SampleBuilder> samples = new ArrayList<>();
		for (int index = 0; index < sampleCount; index++) {
			double angle = 2.0 * Math.PI * index / sampleCount;
			samples.add(new ReferencePath.SampleBuilder(
					radiusMeters * Math.cos(angle),
					radiusMeters * Math.sin(angle),
					angle + Math.PI / 2.0,
					1.0 / radiusMeters));
		}
		return ReferencePath.fromSamples(samples);
	}
}
