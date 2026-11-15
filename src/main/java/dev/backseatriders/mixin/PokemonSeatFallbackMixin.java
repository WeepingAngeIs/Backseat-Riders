package dev.backseatriders.mixin;

import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.RideablesSettingsData;
import dev.backseatriders.SeatFallbackLists;
import dev.backseatriders.SeatFallbackPolicy;
import dev.backseatriders.SeatFallbackPosition;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonSeatFallbackMixin {
    @Unique private static final EntityDataAccessor<Integer> backseatriders$syncedSeatCount =
        SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = false)
    private void backseatriders$defineSeatCount(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(backseatriders$syncedSeatCount, 0);
    }

    @Inject(method = "getSeats", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$appendConfiguredSeats(CallbackInfoReturnable<List<Seat>> cir) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        List<Seat> nativeSeats = cir.getReturnValue();
        if (nativeSeats == null) return;

        int targetCount;
        if (pokemon.level() instanceof ServerLevel serverLevel) {
            targetCount = SeatFallbackPolicy.effectiveSeatCount(nativeSeats.size(),
                backseatriders$hasBehaviorWithoutSeatProfile(pokemon),
                RideablesSettingsData.get(serverLevel).minimumSeats(),
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
    private static boolean backseatriders$hasBehaviorWithoutSeatProfile(PokemonEntity pokemon) {
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

