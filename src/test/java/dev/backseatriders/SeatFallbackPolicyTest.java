package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class SeatFallbackPolicyTest {
    @Test
    void doesNotMakeEmptyOrNonRideableFormsRideable() {
        assertFalse(SeatFallbackPolicy.shouldAppendBodyFallback(0));
    }

    @Test
    void addsOneSeatOnlyToSingleSeatProfiles() {
        assertTrue(SeatFallbackPolicy.shouldAppendBodyFallback(1));
    }

    @Test
    void preservesExistingMultiSeatProfiles() {
        assertFalse(SeatFallbackPolicy.shouldAppendBodyFallback(2));
        assertFalse(SeatFallbackPolicy.shouldAppendBodyFallback(6));
    }
}
