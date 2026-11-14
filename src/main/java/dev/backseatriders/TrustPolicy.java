package dev.backseatriders;

import java.util.Set;
import java.util.UUID;

/** Small, deterministic authorization rule shared by the runtime hook and unit tests. */
final class TrustPolicy {
    private TrustPolicy() {}

    static boolean permits(UUID owner, UUID rider, Set<UUID> trustedRiders) {
        return owner != null && trustedRiders.contains(rider);
    }
}

