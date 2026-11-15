package dev.backseatriders.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.backseatriders.SeatFallbackPosition;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate", remap = false)
public abstract class PokemonSeatFallbackServerMixin {
    @Inject(method = "positionRider", at = @At("TAIL"), remap = false)
    private void backseatriders$positionBodyFallback(Entity passenger, Entity.MoveFunction positionUpdater,
                                                      CallbackInfo ci) {
        Entity vehicle = passenger.getVehicle();
        if (vehicle instanceof PokemonEntity pokemon
                && SeatFallbackPosition.isBodyFallback(pokemon, passenger)) {
            SeatFallbackPosition.position(pokemon, passenger, positionUpdater);
        }
    }
}
