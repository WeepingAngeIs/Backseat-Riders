package dev.backseatriders.mixin;

import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.RideablesSettingsData;
import dev.backseatriders.SeatFallbackLists;
import dev.backseatriders.SeatFallbackPolicy;
import dev.backseatriders.SeatFallbackPosition;
import dev.backseatriders.PokemonSeatFallbackAccess;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonSeatFallbackMixin implements PokemonSeatFallbackAccess {
    @Unique private static final EntityDataAccessor<Integer> backseatriders$syncedSeatCount =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<CompoundTag> backseatriders$syncedSeatOffsets =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.COMPOUND_TAG);
    @Unique private static final EntityDataAccessor<Boolean> backseatriders$syncedGroundFallback =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Float> backseatriders$syncedGroundSpeed =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.FLOAT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void backseatriders$defineSeatCount(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(backseatriders$syncedSeatCount, 0);
        builder.define(backseatriders$syncedSeatOffsets, new CompoundTag());
        builder.define(backseatriders$syncedGroundFallback, false);
        builder.define(backseatriders$syncedGroundSpeed, 0.06F);
    }

    @Override
    @Unique
    public CompoundTag backseatriders$getSeatOffsets() {
        return ((PokemonEntity) (Object) this).getEntityData().get(backseatriders$syncedSeatOffsets);
    }

    @Override
    @Unique
    public void backseatriders$setSeatOffsets(CompoundTag offsets) {
        ((PokemonEntity) (Object) this).getEntityData().set(backseatriders$syncedSeatOffsets, offsets);
    }

    @Override @Unique
    public boolean backseatriders$isGroundFallbackEnabled() {
        return ((PokemonEntity) (Object) this).getEntityData().get(backseatriders$syncedGroundFallback);
    }

    @Override @Unique
    public void backseatriders$setGroundFallbackEnabled(boolean enabled) {
        ((PokemonEntity) (Object) this).getEntityData().set(backseatriders$syncedGroundFallback, enabled);
    }

    @Override @Unique
    public double backseatriders$getGroundFallbackSpeed() {
        return ((PokemonEntity) (Object) this).getEntityData().get(backseatriders$syncedGroundSpeed);
    }

    @Override @Unique
    public void backseatriders$setGroundFallbackSpeed(double speed) {
        ((PokemonEntity) (Object) this).getEntityData().set(backseatriders$syncedGroundSpeed, (float) speed);
    }

    @Inject(method = "getSeats", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$appendConfiguredSeats(CallbackInfoReturnable<List<Seat>> cir) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        List<Seat> nativeSeats = cir.getReturnValue();
        if (nativeSeats == null) return;

        int targetCount;
        if (pokemon.level() instanceof ServerLevel serverLevel) {
            RideablesSettingsData settings = RideablesSettingsData.get(serverLevel);
            boolean nativeBehavior = GroundRideEligibility.hasNativeRidingBehaviour(pokemon);
            boolean groundFallback = settings.groundFallbackEnabled() && !nativeBehavior;
            pokemon.getEntityData().set(backseatriders$syncedGroundFallback, groundFallback);
            pokemon.getEntityData().set(backseatriders$syncedGroundSpeed, (float) settings.groundFallbackSpeed());
            boolean mayGenerateSeats = nativeBehavior || groundFallback;
            targetCount = !mayGenerateSeats ? nativeSeats.size() : SeatFallbackPolicy.effectiveSeatCount(nativeSeats.size(),
                backseatriders$hasNativeBehaviorWithoutSeatProfile(pokemon) || groundFallback,
                settings.minimumSeats(),
                pokemon.getOccupiedSeats().size(), backseatriders$highestOccupiedFallbackIndex(pokemon));
            pokemon.getEntityData().set(backseatriders$syncedSeatCount, targetCount);
        } else {
            targetCount = Math.max(nativeSeats.size(), pokemon.getEntityData().get(backseatriders$syncedSeatCount));
        }
        if (targetCount <= nativeSeats.size()) return;
        int nativeSeatCount = nativeSeats.size();
        cir.setReturnValue(SeatFallbackLists.ensureCount(nativeSeats, targetCount,
            index -> SeatFallbackPosition.seat(nativeSeatCount, index)));
    }

    @Unique
    private static boolean backseatriders$hasNativeBehaviorWithoutSeatProfile(PokemonEntity pokemon) {
        RidingProperties properties = pokemon.getRideProp();
        return properties != null && properties.getSeats().isEmpty()
            && properties.getBehaviours() != null && !properties.getBehaviours().isEmpty();
    }

    @Unique
    private static int backseatriders$highestOccupiedFallbackIndex(PokemonEntity pokemon) {
        return pokemon.getOccupiedSeats().keySet().stream()
            .map(SeatFallbackPosition::slot)
            .filter(slot -> slot != null)
            .mapToInt(SeatFallbackPosition.FallbackSlot::index)
            .max().orElse(-1);
    }

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionClientBodyFallback(Entity passenger, Entity.MoveFunction updater, CallbackInfo ci) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        if (pokemon.level().isClientSide() && SeatFallbackPosition.isBodyFallback(pokemon, passenger)) {
            SeatFallbackPosition.position(pokemon, passenger, updater);
        }
    }
}

