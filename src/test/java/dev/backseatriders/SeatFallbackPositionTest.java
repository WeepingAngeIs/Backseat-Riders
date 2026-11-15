package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SeatFallbackPositionTest {
    @Test
    void placesFallbackBehindAndAboveMountAtZeroYaw() {
        var position = SeatFallbackPosition.bodyRelativePosition(1.0, 2.0, 3.0, 2.0, 3.0, 0.0f);
        assertEquals(1.0, position.x(), 1.0e-9);
        assertEquals(4.16, position.y(), 1.0e-9);
        assertEquals(2.4, position.z(), 1.0e-9);
    }

    @Test
    void rotatesRearOffsetWithMountYaw() {
        var position = SeatFallbackPosition.bodyRelativePosition(1.0, 2.0, 3.0, 2.0, 3.0, 90.0f);
        assertEquals(1.6, position.x(), 1.0e-9);
        assertEquals(4.16, position.y(), 1.0e-9);
        assertEquals(3.0, position.z(), 1.0e-9);
    }

    @Test
    void clampsOffsetsForTinyAndHugeMountDimensions() {
        var tiny = SeatFallbackPosition.bodyRelativePosition(0, 0, 0, 0.1, 0.1, 0.0f);
        assertEquals(0.15, tiny.y(), 1.0e-9);
        assertEquals(-0.2, tiny.z(), 1.0e-9);

        var huge = SeatFallbackPosition.bodyRelativePosition(0, 0, 0, 10.0, 10.0, 0.0f);
        assertEquals(7.2, huge.y(), 1.0e-9);
        assertEquals(-0.9, huge.z(), 1.0e-9);
    }
}
