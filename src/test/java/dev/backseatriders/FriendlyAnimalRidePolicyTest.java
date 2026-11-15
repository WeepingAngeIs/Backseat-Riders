package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FriendlyAnimalRidePolicyTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID rider = UUID.randomUUID();

    @Test
    void registrationIsDisabledByDefaultAndOwnershipIsExplicit() {
        assertFalse(canMount(false, false, 0, false));
        assertFalse(canMount(true, false, 0, false));
        assertFalse(canMount(true, true, 0, false));
        assertTrue(canMount(true, true, 0, true));
    }

    @Test
    void rejectsYoungDeadOrUnsupportedAnimals() {
        assertFalse(FriendlyAnimalRidePolicy.mayMount(true, true, true, false, true, false,
            0, owner, rider, true));
        assertFalse(FriendlyAnimalRidePolicy.mayMount(true, true, false, true, true, false,
            0, owner, rider, true));
        assertFalse(FriendlyAnimalRidePolicy.mayMount(true, true, true, true, false, false,
            0, owner, rider, true));
    }

    @Test
    void capsSeatsAndRevocationRemovesPermission() {
        assertTrue(canMount(true, true, 0, true));
        assertTrue(canMount(true, true, 1, true));
        assertFalse(canMount(true, true, 2, true));
        assertFalse(canMount(true, true, 1, false));
    }

    private boolean canMount(boolean enabled, boolean registered, int passengers, boolean trusted) {
        return FriendlyAnimalRidePolicy.mayMount(enabled, registered, true, true, true, false,
            passengers, owner, rider, trusted);
    }
}
