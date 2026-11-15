package dev.backseatriders;

import java.util.Set;
import java.util.UUID;

/** Decision helpers for the narrow owner/passenger gates in Cobblemon's riding method. */
public final class RideAccessPolicy {
    private RideAccessPolicy() {}

    static boolean ownerGate(UUID owner, UUID rider, Set<UUID> trustedRiders) {
        return owner != null && (owner.equals(rider) || trustedRiders.contains(rider));
    }

    /** Return the value Cobblemon should see for its second (passenger-list) emptiness check. */
    public static boolean passengerGateReportsEmpty(boolean ownerOrTrusted, boolean actuallyEmpty) {
        return !ownerOrTrusted || actuallyEmpty;
    }
}

