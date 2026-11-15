package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.Objects;
import net.minecraft.world.entity.Entity;

public final class SeatFallbackPosition {
    public static final String LOCATOR = "backseatriders:body_fallback";

    private SeatFallbackPosition() {}

    public static boolean isBodyFallback(PokemonEntity pokemon, Entity passenger) {
        Seat seat = pokemon.getSeatForPassenger(passenger);
        return seat != null && Objects.equals(LOCATOR, seat.getLocator());
    }

    public static void position(PokemonEntity pokemon, Entity passenger, Entity.MoveFunction updater) {
        double width = pokemon.getBbWidth();
        double height = pokemon.getBbHeight();
        double rearOffset = Math.max(0.2, Math.min(0.9, width * 0.3));
        double yOffset = Math.max(0.15, height * 0.72);
        double yaw = Math.toRadians(pokemon.getYRot());
        double x = pokemon.getX() + Math.sin(yaw) * rearOffset;
        double y = pokemon.getY() + yOffset;
        double z = pokemon.getZ() - Math.cos(yaw) * rearOffset;
        updater.accept(passenger, x, y, z);
    }
}
