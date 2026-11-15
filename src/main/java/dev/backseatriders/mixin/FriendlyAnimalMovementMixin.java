package dev.backseatriders.mixin;

import dev.backseatriders.FriendlyAnimalData;
import dev.backseatriders.FriendlyAnimalRidePolicy;
import dev.backseatriders.RideableTrustData;
import dev.backseatriders.RideablesSettingsData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ground-only, server-authoritative walking controls for explicitly registered animals. */
@Mixin(LivingEntity.class)
public abstract class FriendlyAnimalMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void backseatriders$driveRegisteredAnimal(Vec3 ignoredInput, CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (!(living instanceof Mob animal) || !FriendlyAnimalRidePolicy.supports(animal)
                || !(animal.level() instanceof ServerLevel level) || animal.getPassengers().isEmpty()) return;
        FriendlyAnimalData owners = FriendlyAnimalData.get(level);
        if (!owners.isRegistered(animal) || !RideablesSettingsData.get(level).friendlyAnimalsEnabled()
                || !FriendlyAnimalRidePolicy.isAdult(animal) || !animal.isAlive() || animal.isRemoved()) return;
        // Let vanilla gravity and fluid movement handle states this conservative adapter does not support.
        if (!animal.onGround() || animal.isInWaterOrBubble()) return;

        ci.cancel();
        animal.getNavigation().stop();
        animal.setDeltaMovement(Vec3.ZERO);
        Entity first = animal.getPassengers().get(0);
        if (!(first instanceof ServerPlayer driver)) return;
        java.util.UUID owner = owners.owner(animal);
        boolean mayDrive = owner != null && (owner.equals(driver.getUUID())
            || RideablesSettingsData.get(level).trustedRidersCanDrive()
                && RideableTrustData.isTrustedOwner(owner, driver));
        if (!mayDrive) {
            driver.stopRiding();
            return;
        }

        float yaw = driver.getYRot();
        animal.setYRot(yaw);
        animal.setYHeadRot(yaw);
        double strafe = driver.xxa;
        double forward = driver.zza;
        double magnitude = Math.sqrt(strafe * strafe + forward * forward);
        if (magnitude > 1.0) {
            strafe /= magnitude;
            forward /= magnitude;
        }
        if (magnitude < 1.0E-4) return;
        double radians = Math.toRadians(yaw);
        double speed = RideablesSettingsData.get(level).groundFallbackSpeed();
        double dx = (strafe * Math.cos(radians) - forward * Math.sin(radians)) * speed;
        double dz = (strafe * Math.sin(radians) + forward * Math.cos(radians)) * speed;
        animal.move(MoverType.SELF, new Vec3(dx, 0.0, dz));
    }
}
