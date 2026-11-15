package dev.backseatriders;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.armadillo.Armadillo;

/** Bounded eligibility policy for explicitly assigned, adult friendly animals. */
public final class FriendlyAnimalRidePolicy {
    public static final int MAX_PASSENGERS = 2;

    private FriendlyAnimalRidePolicy() {}

    public static boolean supports(Entity entity) {
        return entity instanceof Cow || entity instanceof Sheep || entity instanceof Armadillo;
    }

    public static boolean isAdult(Entity entity) {
        return entity instanceof AgeableMob ageable && !ageable.isBaby();
    }

    public static boolean mayMount(boolean enabled, boolean registered, boolean supported, boolean adult,
                                   boolean alive, boolean removed, int passengers, UUID owner, UUID rider,
                                   boolean trusted) {
        if (!enabled || !registered || !supported || !adult || !alive || removed
                || passengers >= MAX_PASSENGERS || owner == null || rider == null) return false;
        return owner.equals(rider) || trusted;
    }
}
