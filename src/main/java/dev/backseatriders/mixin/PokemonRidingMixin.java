package dev.backseatriders.mixin;

import dev.backseatriders.RideableTrustData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.cobblemon.mod.common.entity.pokemon.PokemonEntity", remap = false)
public abstract class PokemonRidingMixin {
    @Unique private static final ThreadLocal<Boolean> backseatriders$ownerOrTrusted = ThreadLocal.withInitial(() -> false);
    @Unique private static final EntityDataAccessor<Optional<UUID>> backseatriders$trustedDriver =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void backseatriders$defineTrustedDriver(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(backseatriders$trustedDriver, Optional.empty());
    }

    /** Change only Cobblemon's owner equality gate; the surrounding native guards still run. */
    @Redirect(method = "isRideableFor$lambda$0", at = @At(value = "INVOKE",
        target = "Lkotlin/jvm/internal/Intrinsics;areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z"), remap = false)
    private static boolean backseatriders$authorizedOwnerComparison(Object owner, Object rider) {
        boolean ownerOrTrusted = owner instanceof Player ownerPlayer && rider instanceof Player riderPlayer
            && RideableTrustData.ownerOrTrusted(ownerPlayer, riderPlayer);
        backseatriders$ownerOrTrusted.set(ownerOrTrusted);
        return ownerOrTrusted;
    }

    /** Cobblemon normally permits any non-owner once a passenger is aboard; this project requires an explicit grant. */
    @Redirect(method = "isRideableFor$lambda$0", at = @At(value = "INVOKE",
        target = "Ljava/util/List;isEmpty()Z", ordinal = 1), remap = false)
    private static boolean backseatriders$untrustedPassengerGate(List<?> passengers) {
        boolean ownerOrTrusted = backseatriders$ownerOrTrusted.get();
        backseatriders$ownerOrTrusted.remove();
        return dev.backseatriders.RideAccessPolicy.passengerGateReportsEmpty(ownerOrTrusted, passengers.isEmpty());
    }

    /** Keep the native selected driver when one exists; otherwise let a trusted onboard rider drive. */
    @Inject(method = "getControllingPassenger", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$trustedDriver(CallbackInfoReturnable<LivingEntity> cir) {
        Entity pokemon = (Entity) (Object) this;
        if (cir.getReturnValue() != null) {
            if (!pokemon.level().isClientSide()) pokemon.getEntityData().set(backseatriders$trustedDriver, Optional.empty());
            return;
        }
        if (pokemon.level().isClientSide()) {
            UUID driver = pokemon.getEntityData().get(backseatriders$trustedDriver).orElse(null);
            if (driver != null) {
                for (Entity passenger : pokemon.getPassengers()) {
                    if (passenger.getUUID().equals(driver) && passenger instanceof LivingEntity living) {
                        cir.setReturnValue(living);
                        return;
                    }
                }
            }
            return;
        }
        for (Entity passenger : pokemon.getPassengers()) {
            if (passenger instanceof ServerPlayer player && RideableTrustData.isTrusted(pokemon, player)) {
                pokemon.getEntityData().set(backseatriders$trustedDriver, Optional.of(player.getUUID()));
                cir.setReturnValue(player);
                return;
            }
        }
        pokemon.getEntityData().set(backseatriders$trustedDriver, Optional.empty());
    }
}

