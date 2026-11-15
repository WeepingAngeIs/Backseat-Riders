package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ClaimProtectionDecisionTest {
    @Test
    void absentOptionalClaimModLeavesExistingEventProtectionInControl() {
        assertFalse(ClaimProtectionDecision.denyMount(false, false, false));
        assertFalse(ClaimProtectionDecision.denyMount(false, true, true));
    }

    @Test
    void successfulApiAllowsOnlyWhenTheClaimApiAllows() {
        assertFalse(ClaimProtectionDecision.denyMount(true, true, false));
        assertTrue(ClaimProtectionDecision.denyMount(true, true, true));
    }

    @Test
    void apiFailureFailsClosedWhenTheClaimModIsInstalled() {
        assertTrue(ClaimProtectionDecision.denyMount(true, false, false));
    }
}
