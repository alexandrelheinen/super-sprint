package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ReferencePathTest {

	private static final double EPSILON = 1e-9;

	/** Straight path heading east along y = 0, one sample every 0.5 m. */
	private static ReferencePath eastboundPath(int sampleCount) {
		List<ReferencePath.SampleBuilder> samples = new ArrayList<>();
		for (int index = 0; index < sampleCount; index++) {
			samples.add(new ReferencePath.SampleBuilder(index * 0.5, 0.0, 0.0, 0.0));
		}
		return ReferencePath.fromSamples(samples);
	}

	@Test
	public void emptyPathReportsEmpty() {
		assertTrue(ReferencePath.empty().isEmpty());
		assertEquals(0, ReferencePath.empty().sampleCount());
	}

	@Test
	public void projectsOnPathPointWithZeroError() {
		ReferencePath path = eastboundPath(41);
		ReferencePath.Projection projection = path.project(5.0, 0.0);
		assertEquals(10, projection.closestIndex());
		assertEquals(0.0, projection.crossTrackError(), EPSILON);
		assertEquals(0.0, projection.referenceHeading(), EPSILON);
	}

	@Test
	public void crossTrackErrorIsPositiveRightOfTravelDirection() {
		ReferencePath path = eastboundPath(41);
		// Screen coordinates are y-down, so y > 0 is right of an eastbound path.
		ReferencePath.Projection right = path.project(5.0, 2.0);
		ReferencePath.Projection left = path.project(5.0, -2.0);
		assertTrue(right.crossTrackError() > 0.0);
		assertTrue(left.crossTrackError() < 0.0);
		assertEquals(2.0, right.crossTrackError(), 1e-6);
	}

	@Test
	public void fullSearchFindsClosestSampleAnywhereOnPath() {
		ReferencePath path = eastboundPath(1000);
		ReferencePath.Projection projection = path.project(400.0, 0.3, ReferencePath.NO_HINT);
		assertEquals(800, projection.closestIndex());
	}

	@Test
	public void windowedSearchFollowsForwardProgress() {
		ReferencePath path = eastboundPath(1000);
		ReferencePath.Projection projection = path.project(100.6, 0.0, 200);
		assertEquals(201, projection.closestIndex());
	}

	@Test
	public void windowedSearchAllowsSmallBackwardCorrections() {
		ReferencePath path = eastboundPath(1000);
		// Hint slightly ahead of the true closest sample (e.g. after a collision knockback).
		ReferencePath.Projection projection = path.project(95.0, 0.0, 200);
		assertEquals(190, projection.closestIndex());
	}

	@Test
	public void windowedSearchWrapsAroundClosedLoops() {
		ReferencePath path = eastboundPath(1000);
		// Hint near the end of the loop; the closest sample is past the wrap point.
		ReferencePath.Projection projection = path.project(1.0, 0.0, 995);
		assertEquals(2, projection.closestIndex());
	}
}
