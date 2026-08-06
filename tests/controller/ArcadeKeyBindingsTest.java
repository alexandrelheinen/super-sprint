package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.Test;

public class ArcadeKeyBindingsTest {

	@Test
	public void seatOneUsesArrowKeys() {
		ArcadeKeyBindings keys = ArcadeKeyBindings.forHumanSeat(1);
		assertEquals(KeyEvent.VK_UP, keys.accelerateKey());
		assertEquals(KeyEvent.VK_DOWN, keys.brakeKey());
		assertEquals(KeyEvent.VK_LEFT, keys.steerLeftKey());
		assertEquals(KeyEvent.VK_RIGHT, keys.steerRightKey());
	}

	@Test
	public void seatTwoUsesWasd() {
		ArcadeKeyBindings keys = ArcadeKeyBindings.forHumanSeat(2);
		assertEquals(KeyEvent.VK_W, keys.accelerateKey());
		assertEquals(KeyEvent.VK_S, keys.brakeKey());
		assertEquals(KeyEvent.VK_A, keys.steerLeftKey());
		assertEquals(KeyEvent.VK_D, keys.steerRightKey());
	}

	@Test
	public void unsupportedSeatIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> ArcadeKeyBindings.forHumanSeat(3));
	}
}
