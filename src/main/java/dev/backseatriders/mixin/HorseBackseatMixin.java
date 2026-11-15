package dev.backseatriders.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import dev.backseatriders.RideableTrustData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HorseBackseatMixin {
    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true, remap = false)
    private void backseatriders$allowSecondHorseRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity vehicle = (Entity) (Object) this;
        if (vehicle instanceof AbstractHorse horse && horse.isTamed() && passenger instanceof Player player
                && horse.getPassengers().size() < 2 && backseatriders$mayRide(horse, player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$spaceSecondHorseRider(Entity passenger, CallbackInfo ci) {
        Entity vehicle = (Entity) (Object) this;
        if (!(vehicle instanceof AbstractHorse horse) || horse.getPassengers().size() < 2
                || horse.getPassengers().indexOf(passenger) != 1) return;
        double yaw = Math.toRadians(vehicle.getYRot());
        double rearOffset = 0.42;
        passenger.setPos(passenger.getX() + Math.sin(yaw) * rearOffset,
            passenger.getY(), passenger.getZ() - Math.cos(yaw) * rearOffset);
    }

    @Unique
    private static boolean backseatriders$mayRide(AbstractHorse horse, Player player) {
        var owner = horse.getOwnerUUID();
        return owner != null && (owner.equals(player.getUUID())
            || player instanceof ServerPlayer serverPlayer && RideableTrustData.isTrustedOwner(owner, serverPlayer));
    }
}
