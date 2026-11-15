package dev.backseatriders.mixin;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.SeatFallbackPolicy;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonSeatFallbackMixin {
    @Unique
    private static final String BACKSEATRIDERS_BODY_FALLBACK = "backseatriders:body_fallback";

    @Inject(method = "getSeats", at = @At("RETURN"), cancellable = true, remap = false)
    private void backseatriders$appendBodyFallback(CallbackInfoReturnable<List<Seat>> cir) {
        List<Seat> nativeSeats = cir.getReturnValue();
        if (nativeSeats == null || !SeatFallbackPolicy.shouldAppendBodyFallback(nativeSeats.size())) {
            return;
        }

        List<Seat> extendedSeats = new ArrayList<>(nativeSeats);
        extendedSeats.add(new Seat(BACKSEATRIDERS_BODY_FALLBACK, null, List.of(), false));
        cir.setReturnValue(extendedSeats);
    }
}
