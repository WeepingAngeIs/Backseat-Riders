package dev.backseatriders.mixin;

import dev.backseatriders.RideableTrustData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.cobblemon.mod.common.entity.pokemon.PokemonEntity", remap = false)
public abstract class PokemonRidingMixin {
    /** Let the native test run first, then allow the owner-approved passenger exception if a seat remains. */
    @Inject(method = "isRideableFor", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$allowTrusted(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        Entity pokemon = (Entity) (Object) this;
        if (!cir.getReturnValue() && RideableTrustData.canRide(pokemon, player)) cir.setReturnValue(true);
    }

    /** Keep the native selected driver when one exists; otherwise let a trusted onboard rider drive. */
    @Inject(method = "getControllingPassenger", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$trustedDriver(CallbackInfoReturnable<LivingEntity> cir) {
        if (cir.getReturnValue() != null) return;
        Entity pokemon = (Entity) (Object) this;
        for (Entity passenger : pokemon.getPassengers()) {
            if (passenger instanceof ServerPlayer player && RideableTrustData.isTrusted(pokemon, player)) {
                cir.setReturnValue(player);
                return;
            }
        }
    }
}

