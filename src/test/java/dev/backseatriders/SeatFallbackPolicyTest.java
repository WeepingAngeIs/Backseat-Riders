package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SeatFallbackPolicyTest {
    @Test
    void givesTwoSeatsToRideableProfilesWithNoNativeSeats() {
        assertEquals(2, SeatFallbackPolicy.effectiveSeatCount(0, true, 2, 0, -1));
        assertEquals(0, SeatFallbackPolicy.effectiveSeatCount(0, false, 2, 0, -1));
    }

    @Test
    void minimumDoesNotReduceExistingNativeCountsAndCanBeRaised() {
        assertEquals(2, SeatFallbackPolicy.effectiveSeatCount(1, false, 2, 0, -1));
        assertEquals(2, SeatFallbackPolicy.effectiveSeatCount(2, false, 2, 0, -1));
        assertEquals(6, SeatFallbackPolicy.effectiveSeatCount(6, false, 2, 0, -1));
        assertEquals(8, SeatFallbackPolicy.effectiveSeatCount(6, false, 8, 0, -1));
    }

    @Test
    void configurationDecreaseNeverDropsAnOccupiedFallbackSeat() {
        assertEquals(4, SeatFallbackPolicy.effectiveSeatCount(1, false, 2, 1, 3));
    }

    @Test
    void boundsSyntheticSeatCountEvenIfExistingOccupancyIsMalformed() {
        assertEquals(16, SeatFallbackPolicy.effectiveSeatCount(0, true, 16, 20, 19));
        assertEquals(20, SeatFallbackPolicy.effectiveSeatCount(20, false, 16, 20, 19));
    }
}

