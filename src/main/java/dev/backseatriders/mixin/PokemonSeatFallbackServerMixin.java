package dev.backseatriders.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import dev.backseatriders.SeatFallbackPosition;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokemonServerDelegate.class, remap = false)
public abstract class PokemonSeatFallbackServerMixin {
    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionServerBodyFallback(Entity passenger, Entity.MoveFunction updater,
                                                            CallbackInfo ci) {
        PokemonEntity pokemon = ((PokemonServerDelegate) (Object) this).getEntity();
        if (SeatFallbackPosition.isBodyFallback(pokemon, passenger)) {
            SeatFallbackPosition.position(pokemon, passenger, updater);
        }
    }
}
