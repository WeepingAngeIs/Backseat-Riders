package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.Entity;

public final class SeatFallbackPosition {
    public static final String LOCATOR = "backseatriders:body_fallback";

    // Seat is an identity key in Cobblemon's occupiedSeats map; reuse this instance on every getSeats call.
    public static final Seat BODY_FALLBACK_SEAT = new Seat(LOCATOR, null, List.of(), false);

    private SeatFallbackPosition() {}

    public static List<Seat> appendFallbackIfNeeded(List<Seat> nativeSeats) {
        if (!SeatFallbackPolicy.shouldAppendBodyFallback(nativeSeats.size())) {
            return nativeSeats;
        }
        List<Seat> extendedSeats = new ArrayList<>(nativeSeats);
        extendedSeats.add(BODY_FALLBACK_SEAT);
        return extendedSeats;
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
