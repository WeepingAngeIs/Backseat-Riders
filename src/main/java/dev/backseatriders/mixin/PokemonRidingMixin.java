package dev.backseatriders.mixin;

import dev.backseatriders.RideableTrustData;
import dev.backseatriders.GroundRideEligibility;
import dev.backseatriders.PokemonSeatFallbackAccess;
import java.util.List;
import java.util.Objects;
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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.cobblemon.mod.common.entity.pokemon.PokemonEntity", remap = false)
public abstract class PokemonRidingMixin {
    @Unique private static final ThreadLocal<UUID> backseatriders$ownerUuid = new ThreadLocal<>();
    @Unique private static final EntityDataAccessor<Optional<UUID>> backseatriders$trustedDriver =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void backseatriders$defineTrustedDriver(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(backseatriders$trustedDriver, Optional.empty());
    }

    @Redirect(method = "isRideableFor$lambda$0", at = @At(value = "INVOKE",
        target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;getOwner()Lnet/minecraft/world/entity/LivingEntity;",
        ordinal = 1), remap = false)
    private static LivingEntity backseatriders$captureOwnerUuid(PokemonEntity pokemon) {
        backseatriders$ownerUuid.set(RideableTrustData.ownerId(pokemon));
        return pokemon.getOwner();
    }

    /** Change only Cobblemon's owner equality gate; the surrounding native guards still run. */
    @Redirect(method = "isRideableFor$lambda$0", at = @At(value = "INVOKE",
        target = "Lkotlin/jvm/internal/Intrinsics;areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z"), remap = false)
    private static boolean backseatriders$authorizedOwnerComparison(Object owner, Object rider) {
        UUID ownerId = backseatriders$ownerUuid.get();
        backseatriders$ownerUuid.remove();
        boolean ownerMatches = Objects.equals(owner, rider);
        return ownerMatches || rider instanceof Player riderPlayer && ownerId != null
            && RideableTrustData.isTrustedOwner(ownerId, riderPlayer);
    }

    /** Cobblemon normally permits any non-owner once a passenger is aboard; this project requires an explicit grant. */
    @Redirect(method = "isRideableFor$lambda$0", at = @At(value = "INVOKE",
        target = "Ljava/util/List;isEmpty()Z", ordinal = 1), remap = false)
    private static boolean backseatriders$untrustedPassengerGate(List<?> passengers) {
        return dev.backseatriders.RideAccessPolicy.passengerGateReportsEmpty(false, passengers.isEmpty());
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
            boolean mayDrive = passenger instanceof ServerPlayer player
                && (GroundRideEligibility.hasNativeRidingBehaviour((PokemonEntity) pokemon)
                    ? RideableTrustData.isOwner(pokemon, player) || RideableTrustData.isTrustedDriver(pokemon, player)
                    : GroundRideEligibility.mayDrive((PokemonEntity) pokemon, player));
            if (mayDrive) {
                ServerPlayer player = (ServerPlayer) passenger;
                pokemon.getEntityData().set(backseatriders$trustedDriver, Optional.of(player.getUUID()));
                cir.setReturnValue(player);
                return;
            }
        }
        pokemon.getEntityData().set(backseatriders$trustedDriver, Optional.empty());
    }

    /** A behaviorless form gets the conservative ground controller only after all explicit safety gates pass. */
    @Inject(method = "isRideableFor", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$allowSafeGroundFallback(ServerPlayer rider, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        if (GroundRideEligibility.mayBoard(pokemon, rider)) cir.setReturnValue(true);
    }

    /** Replace only the behavior-provided input vector; Cobblemon retains its own collision, friction, and gravity path. */
    @ModifyVariable(method = "travel", at = @At(value = "STORE", ordinal = 0), ordinal = 0, remap = false)
    private Vec3 backseatriders$groundOnlyInput(Vec3 nativeInput) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        PokemonSeatFallbackAccess access = (PokemonSeatFallbackAccess) pokemon;
        if (!access.backseatriders$isGroundFallbackEnabled()) return nativeInput;
        LivingEntity controlling = pokemon.getControllingPassenger();
        if (!(controlling instanceof Player rider) || !GroundRideEligibility.mayDrive(pokemon, rider)) return nativeInput;
        if (!pokemon.onGround() || pokemon.isInWaterOrBubble()) return Vec3.ZERO;

        pokemon.setYRot(rider.getYRot());
        Vec3 input = new Vec3(rider.xxa, 0.0, rider.zza);
        if (input.lengthSqr() > 1.0) input = input.normalize();
        return input.scale(access.backseatriders$getGroundFallbackSpeed());
    }
}


