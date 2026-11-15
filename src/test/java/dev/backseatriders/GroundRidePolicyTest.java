package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroundRidePolicyTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRUSTED = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test void allowsOwnerAndGrantedBoardingOnlyOnSafeBehaviorlessMounts() {
        assertTrue(board(OWNER, false, true));
        assertTrue(board(TRUSTED, true, true));
        assertFalse(board(STRANGER, false, true));
        assertFalse(board(TRUSTED, true, false));
    }

    @Test void blocksNativeProfilesAndUnsafePokemonStates() {
        assertFalse(GroundRidePolicy.mayBoard(true, true, true, false, false, false, false, false, false,
            true, OWNER, OWNER, false));
        assertFalse(GroundRidePolicy.mayBoard(true, false, true, false, true, false, false, false, false,
            true, OWNER, OWNER, false));
        assertFalse(GroundRidePolicy.mayBoard(true, false, true, false, false, true, false, false, false,
            true, OWNER, OWNER, false));
        assertFalse(GroundRidePolicy.mayBoard(true, false, true, false, false, false, false, true, false,
            true, OWNER, OWNER, false));
        assertFalse(GroundRidePolicy.mayBoard(true, false, true, false, false, false, false, false, true,
            true, OWNER, OWNER, false));
    }

    @Test void trustedDrivingCanBeDisabledWithoutRemovingBoarding() {
        assertTrue(drive(TRUSTED, true, true));
        assertFalse(drive(TRUSTED, true, false));
        assertTrue(drive(OWNER, false, false));
    }

    private boolean board(UUID rider, boolean trusted, boolean enabled) {
        return GroundRidePolicy.mayBoard(enabled, false, true, false, false, false, false, false, false,
            true, OWNER, rider, trusted);
    }

    private boolean drive(UUID rider, boolean trusted, boolean trustedDrive) {
        return GroundRidePolicy.mayDrive(true, false, true, false, false, false, false, false, false,
            OWNER, rider, trusted, trustedDrive);
    }
}

