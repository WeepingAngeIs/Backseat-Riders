package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrustPolicyTest {
    private final UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID rider = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID newOwner = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test void trustedRiderIsPermittedOnlyForMatchingLiveOwner() {
        assertTrue(TrustPolicy.permits(owner, owner, rider, Set.of(rider)));
        assertFalse(TrustPolicy.permits(newOwner, owner, rider, Set.of(rider)));
    }

    @Test void untrustedAndRevokedUuidAreDenied() {
        assertFalse(TrustPolicy.permits(owner, owner, newOwner, Set.of(rider)));
        assertFalse(TrustPolicy.permits(owner, owner, rider, Set.of()));
    }

    @Test void missingOwnerIsDenied() {
        assertFalse(TrustPolicy.permits(null, owner, rider, Set.of(rider)));
    }
}

