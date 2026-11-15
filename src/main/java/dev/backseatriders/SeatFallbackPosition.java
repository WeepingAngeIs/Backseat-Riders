package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.Objects;
import net.minecraft.world.entity.Entity;

public final class SeatFallbackPosition {
    public static final String LOCATOR = "backseatriders:body_fallback";

    private SeatFallbackPosition() {}

    public static Seat bodyFallbackSeat() {
        return FallbackSeatHolder.SEAT;
    }

    private static final class FallbackSeatHolder {
        private static final Seat SEAT = new Seat(LOCATOR, null, java.util.List.of(), false);
    }

    public static boolean isBodyFallback(PokemonEntity pokemon, Entity passenger) {
        Seat seat = pokemon.getSeatForPassenger(passenger);
        return seat != null && Objects.equals(LOCATOR, seat.getLocator());
    }

    public static Position bodyRelativePosition(double x, double y, double z,
                                                double width, double height, float yawDegrees) {
        double rearOffset = Math.max(0.2, Math.min(0.9, width * 0.3));
        double yOffset = Math.max(0.15, height * 0.72);
        double yaw = Math.toRadians(yawDegrees);
        return new Position(
            x + Math.sin(yaw) * rearOffset,
            y + yOffset,
            z - Math.cos(yaw) * rearOffset
        );
    }

    public static void position(PokemonEntity pokemon, Entity passenger, Entity.MoveFunction updater) {
        Position position = bodyRelativePosition(pokemon.getX(), pokemon.getY(), pokemon.getZ(),
            pokemon.getBbWidth(), pokemon.getBbHeight(), pokemon.getYRot());
        updater.accept(passenger, position.x(), position.y(), position.z());
    }

    public record Position(double x, double y, double z) {}
}
