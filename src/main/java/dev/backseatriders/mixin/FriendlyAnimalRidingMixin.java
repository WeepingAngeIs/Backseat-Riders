package dev.backseatriders.mixin;

import dev.backseatriders.FriendlyAnimalData;
import dev.backseatriders.FriendlyAnimalMountAttempt;
import dev.backseatriders.FriendlyAnimalRidePolicy;
import dev.backseatriders.RideableTrustData;
import dev.backseatriders.RideablesSettingsData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mounting and rear-seat spacing for admin-registered adult cows, sheep and armadillos. */
@Mixin(Entity.class)
public abstract class FriendlyAnimalRidingMixin {
    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true, remap = false)
    private void backseatriders$checkRegisteredAnimalAccess(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity animal = (Entity) (Object) this;
        if (!FriendlyAnimalRidePolicy.supports(animal) || !(animal.level() instanceof ServerLevel level)) return;
        FriendlyAnimalData owners = FriendlyAnimalData.get(level);
        java.util.UUID owner = owners.owner(animal);
        if (owner == null) return;
        if (!FriendlyAnimalMountAttempt.isAuthorizedAttempt(animal, passenger)) {
            cir.setReturnValue(false);
            return;
        }
        if (!(passenger instanceof ServerPlayer rider)) {
            cir.setReturnValue(false);
            return;
        }
        boolean trusted = !owner.equals(rider.getUUID()) && RideableTrustData.isTrustedOwner(owner, rider);
        boolean allowed = FriendlyAnimalRidePolicy.mayMount(
            RideablesSettingsData.get(level).friendlyAnimalsEnabled(), true,
            FriendlyAnimalRidePolicy.supports(animal), FriendlyAnimalRidePolicy.isAdult(animal),
            animal.isAlive(), animal.isRemoved(), animal.getPassengers().size(), owner, rider.getUUID(), trusted);
        cir.setReturnValue(allowed);
    }

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionRearAnimalPassenger(Entity passenger, CallbackInfo ci) {
        Entity animal = (Entity) (Object) this;
        if (!FriendlyAnimalRidePolicy.supports(animal) || animal.getPassengers().size() != 2
                || animal.getPassengers().indexOf(passenger) != 1) return;
        double rearOffset = Math.max(0.25, Math.min(0.65, animal.getBbWidth() * 0.35));
        double yaw = Math.toRadians(animal.getYRot());
        passenger.setPos(passenger.getX() - Math.sin(yaw) * rearOffset,
            passenger.getY(), passenger.getZ() - Math.cos(yaw) * rearOffset);
    }
}
