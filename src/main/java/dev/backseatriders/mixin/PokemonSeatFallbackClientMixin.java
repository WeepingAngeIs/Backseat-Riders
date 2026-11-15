package dev.backseatriders.mixin;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import dev.backseatriders.SeatFallbackPosition;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses Cobblemon's animated model matrix when the configured locator exists, with body fallback otherwise. */
@Mixin(value = PokemonClientDelegate.class, remap = false)
public abstract class PokemonSeatFallbackClientMixin {
    @Shadow public abstract PokemonEntity getCurrentEntity();

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionGeneratedSeat(Entity passenger, Entity.MoveFunction updater,
                                                      CallbackInfo ci) {
        PokemonEntity pokemon = getCurrentEntity();
        if (pokemon == null) return;
        Seat seat = pokemon.getSeatForPassenger(passenger);
        if (SeatFallbackPosition.slot(seat) == null) return;
        if (seat.getLocator() != null && ((PosableState) (Object) this).getLocatorStates().containsKey(seat.getLocator())) {
            SeatFallbackPosition.applyLocatorOffset(pokemon, passenger, updater);
        } else {
            SeatFallbackPosition.position(pokemon, passenger, updater);
        }
    }
}

