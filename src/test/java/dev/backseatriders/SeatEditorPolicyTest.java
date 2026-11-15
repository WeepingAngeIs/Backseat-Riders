package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SeatEditorPolicyTest {
    @Test void onlyAcceptsNormalizedSpeciesAndFormKeys() {
        assertTrue(SeatEditorPolicy.isValidProfile("bulbasaur/normal"));
        assertTrue(SeatEditorPolicy.isValidProfile("mr_mime/galarian"));
        assertFalse(SeatEditorPolicy.isValidProfile("/normal"));
        assertFalse(SeatEditorPolicy.isValidProfile("bulbasaur/normal/extra"));
        assertFalse(SeatEditorPolicy.isValidProfile("Bulbasaur/Normal"));
        assertFalse(SeatEditorPolicy.isValidProfile("bulbasaur/../bad"));
        assertFalse(SeatEditorPolicy.isValidProfile("x".repeat(65) + "/normal"));
    }

    @Test void boundsSeatCountToSupportedRange() {
        assertTrue(SeatEditorPolicy.isValidSeatCount(2));
        assertTrue(SeatEditorPolicy.isValidSeatCount(16));
        assertFalse(SeatEditorPolicy.isValidSeatCount(1));
        assertFalse(SeatEditorPolicy.isValidSeatCount(17));
    }

    @Test void acceptsOnlyFiniteBoundedOffsets() {
        assertTrue(SeatEditorPolicy.isValidOffset(4.0, -4.0, 0.25));
        assertFalse(SeatEditorPolicy.isValidOffset(Double.NaN, 0.0, 0.0));
        assertFalse(SeatEditorPolicy.isValidOffset(0.0, 4.01, 0.0));
    }
}

