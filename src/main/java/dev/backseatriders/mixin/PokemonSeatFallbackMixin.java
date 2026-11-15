package dev.backseatriders.mixin;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.SeatFallbackPolicy;
import dev.backseatriders.SeatFallbackLists;
import dev.backseatriders.SeatFallbackPosition;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonSeatFallbackMixin {
    @Inject(method = "getSeats", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$appendBodyFallback(CallbackInfoReturnable<List<Seat>> cir) {
        List<Seat> nativeSeats = cir.getReturnValue();
        if (nativeSeats != null && SeatFallbackPolicy.shouldAppendBodyFallback(nativeSeats.size())) {
            cir.setReturnValue(SeatFallbackLists.appendIfSingle(nativeSeats,
                SeatFallbackPosition.bodyFallbackSeat()));
        }
    }

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionBodyFallback(Entity passenger, Entity.MoveFunction updater, CallbackInfo ci) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        if (SeatFallbackPosition.isBodyFallback(pokemon, passenger)) {
            SeatFallbackPosition.position(pokemon, passenger, updater);
        }
    }
}
