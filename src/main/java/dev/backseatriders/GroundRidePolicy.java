package dev.backseatriders;

import java.util.UUID;

/** Conservative eligibility checks for ground-only movement when a form has no native ride behaviour. */
public final class GroundRidePolicy {
    private GroundRidePolicy() {}

    public static boolean mayBoard(boolean enabled, boolean hasNativeBehaviour, boolean alive, boolean removed,
                                   boolean battling, boolean evolving, boolean busy, boolean tethered,
                                   boolean recallBeam, boolean hasSeat, UUID owner, UUID rider, boolean trusted) {
        if (!enabled || hasNativeBehaviour || !alive || removed || battling || evolving || busy || tethered
                || recallBeam || !hasSeat || owner == null || rider == null) return false;
        return owner.equals(rider) || trusted;
    }

    public static boolean mayDrive(boolean enabled, boolean hasNativeBehaviour, boolean alive, boolean removed,
                                   boolean battling, boolean evolving, boolean busy, boolean tethered,
                                   boolean recallBeam, UUID owner, UUID rider, boolean trusted, boolean trustedDrive) {
        if (!enabled || hasNativeBehaviour || !alive || removed || battling || evolving || busy || tethered
                || recallBeam || owner == null || rider == null) return false;
        return owner.equals(rider) || trusted && trustedDrive;
    }
}

