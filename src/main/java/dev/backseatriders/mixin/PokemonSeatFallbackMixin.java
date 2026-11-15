package dev.backseatriders.mixin;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.SeatFallbackPolicy;
import dev.backseatriders.SeatFallbackPosition;
import java.util.ArrayList;
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
        if (nativeSeats == null || !SeatFallbackPolicy.shouldAppendBodyFallback(nativeSeats.size())) {
            return;
        }

        List<Seat> extendedSeats = new ArrayList<>(nativeSeats);
        extendedSeats.add(new Seat(SeatFallbackPosition.LOCATOR, null, List.of(), false));
        cir.setReturnValue(extendedSeats);
    }

    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionBodyFallback(Entity passenger, Entity.MoveFunction updater, CallbackInfo ci) {
        PokemonEntity pokemon = (PokemonEntity) (Object) this;
        if (SeatFallbackPosition.isBodyFallback(pokemon, passenger)) {
            SeatFallbackPosition.position(pokemon, passenger, updater);
        }
    }
}
