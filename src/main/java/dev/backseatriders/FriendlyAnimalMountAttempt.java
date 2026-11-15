package dev.backseatriders;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Scopes passenger admission to the last-priority, non-canceled interaction handler. */
public final class FriendlyAnimalMountAttempt {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    private FriendlyAnimalMountAttempt() {}

    public static boolean startRiding(Entity animal, ServerPlayer rider) {
        Attempt previous = CURRENT.get();
        CURRENT.set(new Attempt(animal, rider));
        try {
            return rider.startRiding(animal, false);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static boolean isAuthorizedAttempt(Entity animal, Entity passenger) {
        Attempt attempt = CURRENT.get();
        return attempt != null && attempt.animal() == animal && attempt.rider() == passenger;
    }

    private record Attempt(Entity animal, ServerPlayer rider) {}
}
