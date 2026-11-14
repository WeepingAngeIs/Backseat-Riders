package dev.backseatriders;

import java.util.Set;
import java.util.UUID;

/** Small, deterministic authorization rule shared by the runtime hook and unit tests. */
final class TrustPolicy {
    private TrustPolicy() {}

    static boolean permits(UUID liveOwner, UUID registeredOwner, UUID rider, Set<UUID> trustedRiders) {
        return liveOwner != null && liveOwner.equals(registeredOwner) && trustedRiders.contains(rider);
    }
}

