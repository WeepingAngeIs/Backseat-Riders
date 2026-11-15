package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatFallbackPositionTest {
    @Test
    void placesFallbackBehindAndAboveMountAtZeroYaw() {
        var position = SeatFallbackGeometry.bodyRelativePosition(1.0, 2.0, 3.0, 2.0, 3.0, 0.0f, 1, 1);
        assertEquals(1.0, position.x(), 1.0e-9);
        assertEquals(4.16, position.y(), 1.0e-9);
        assertEquals(3.6, position.z(), 1.0e-9);
    }

    @Test
    void rotatesRearOffsetWithMountYaw() {
        var position = SeatFallbackGeometry.bodyRelativePosition(1.0, 2.0, 3.0, 2.0, 3.0, 90.0f, 1, 1);
        assertEquals(0.4, position.x(), 1.0e-9);
        assertEquals(4.16, position.y(), 1.0e-9);
        assertEquals(3.0, position.z(), 1.0e-9);
    }

    @Test
    void clampsOffsetsForTinyAndHugeMountDimensions() {
        var tiny = SeatFallbackGeometry.bodyRelativePosition(0, 0, 0, 0.1, 0.1, 0.0f, 1, 1);
        assertEquals(0.15, tiny.y(), 1.0e-9);
        assertEquals(0.03, tiny.z(), 1.0e-9);

        var huge = SeatFallbackGeometry.bodyRelativePosition(0, 0, 0, 10.0, 10.0, 0.0f, 1, 1);
        assertEquals(5.76, huge.y(), 1.0e-9);
        assertEquals(0.9, huge.z(), 1.0e-9);
    }

    @Test
    void reusesPassedFallbackIdentityAcrossFreshNativeSeatLists() {
        Object nativeSeat = new Object();
        Object stableFallbackSeat = new Object();
        List<Object> firstCall = SeatFallbackLists.appendIfSingle(List.of(nativeSeat), stableFallbackSeat);
        List<Object> secondCall = SeatFallbackLists.appendIfSingle(List.of(nativeSeat), stableFallbackSeat);

        assertSame(nativeSeat, firstCall.get(0));
        assertSame(stableFallbackSeat, firstCall.get(1));
        assertSame(stableFallbackSeat, secondCall.get(1));
        assertEquals(2, firstCall.size());
        assertEquals(2, secondCall.size());
    }

    @Test
    void leavesZeroAndMultiSeatListsUntouched() {
        List<Object> empty = List.of();
        List<Object> pair = List.of(new Object(), new Object());
        assertSame(empty, SeatFallbackLists.appendIfSingle(empty, new Object()));
        assertSame(pair, SeatFallbackLists.appendIfSingle(pair, new Object()));
    }
}

