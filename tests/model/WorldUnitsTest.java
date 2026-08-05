package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class WorldUnitsTest {

	private static final double EPSILON = 1e-9;

	@Test
	public void pixelToMeterConversionRoundTrips() {
		assertEquals(21.9, WorldUnits.pxToM(WorldUnits.mToPx(21.9)), EPSILON);
		assertEquals(219.0, WorldUnits.mToPx(WorldUnits.pxToM(219.0)), EPSILON);
	}

	@Test
	public void tileSizeMatchesWorldScale() {
		assertEquals(
				WorldUnits.METERS_PER_TILE,
				WorldUnits.pxToM(view.GameFrame.TILE_SIZE),
				EPSILON);
	}

	@Test
	public void roundedConversionRoundsToNearestPixel() {
		assertEquals(22, WorldUnits.mToPxRounded(2.16));
		assertEquals(21, WorldUnits.mToPxRounded(2.14));
	}
}
