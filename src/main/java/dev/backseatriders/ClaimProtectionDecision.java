package dev.backseatriders;

/** Fail-closed decision policy for an optional claim-protection adapter. */
public final class ClaimProtectionDecision {
    private ClaimProtectionDecision() {}

    public static boolean denyMount(boolean claimModLoaded, boolean apiCallSucceeded, boolean protectedAction) {
        return claimModLoaded && (!apiCallSucceeded || protectedAction);
    }
}
