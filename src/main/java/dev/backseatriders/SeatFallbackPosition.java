package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import net.minecraft.world.entity.Entity;

/** Stable seats and bounded body-relative placement for forms without a model locator. */
public final class SeatFallbackPosition {
    private static final String LOCATOR_PREFIX = "backseatriders:body_fallback_";
    public static final int MAX_SEATS = 16;
    private static final Seat[][] SEATS = createSeatPool();

    private SeatFallbackPosition() {}

    private static Seat[][] createSeatPool() {
        Seat[][] seats = new Seat[MAX_SEATS + 1][MAX_SEATS];
        for (int nativeCount = 0; nativeCount <= MAX_SEATS; nativeCount++) {
            for (int index = 0; index < MAX_SEATS; index++) {
                seats[nativeCount][index] = new Seat(locator(nativeCount, index), null, List.of(), false);
            }
        }
        return seats;
    }

    public static Seat seat(int nativeSeatCount, int index) {
        if (nativeSeatCount < 0 || nativeSeatCount > MAX_SEATS || index < 0 || index >= MAX_SEATS) {
            throw new IllegalArgumentException("Fallback seat index is outside the supported range");
        }
        return SEATS[nativeSeatCount][index];
    }

    private static String locator(int nativeCount, int index) {
        return LOCATOR_PREFIX + nativeCount + "_" + index;
    }

    public static FallbackSlot slot(Seat seat) {
        if (seat == null || seat.getLocator() == null || !seat.getLocator().startsWith(LOCATOR_PREFIX)) return null;
        String[] parts = seat.getLocator().substring(LOCATOR_PREFIX.length()).split("_");
        if (parts.length != 2) return null;
        try {
            int nativeCount = Integer.parseInt(parts[0]);
            int index = Integer.parseInt(parts[1]);
            if (nativeCount < 0 || nativeCount > MAX_SEATS || index < 0 || index >= MAX_SEATS) return null;
            return new FallbackSlot(nativeCount, index);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean isBodyFallback(PokemonEntity pokemon, Entity passenger) {
        return slot(pokemon.getSeatForPassenger(passenger)) != null;
    }

    public static Position bodyRelativePosition(double x, double y, double z,
                                                double width, double height, float yawDegrees,
                                                int nativeSeatCount, int seatIndex) {
        double safeWidth = Math.max(0.1, Math.min(3.0, width));
        double safeHeight = Math.max(0.1, Math.min(8.0, height));
        double longitudinal;
        if (nativeSeatCount == 0) {
            longitudinal = seatIndex == 0 ? safeWidth * 0.18
                : -Math.min(0.9, safeWidth * (0.18 + 0.12 * (seatIndex - 1)));
        } else {
            longitudinal = -Math.min(0.9, safeWidth * (0.3 + 0.18 * (seatIndex - nativeSeatCount)));
        }
        double yOffset = Math.max(0.15, Math.min(safeHeight * 0.78, safeHeight * 0.72));
        double yaw = Math.toRadians(yawDegrees);
        return new Position(x + Math.sin(yaw) * longitudinal, y + yOffset, z - Math.cos(yaw) * longitudinal);
    }

    public static Position bodyRelativePosition(double x, double y, double z,
                                                double width, double height, float yawDegrees) {
        return bodyRelativePosition(x, y, z, width, height, yawDegrees, 1, 1);
    }

    public static void position(PokemonEntity pokemon, Entity passenger, Entity.MoveFunction updater) {
        FallbackSlot slot = slot(pokemon.getSeatForPassenger(passenger));
        if (slot == null) return;
        Position position = bodyRelativePosition(pokemon.getX(), pokemon.getY(), pokemon.getZ(),
            pokemon.getBbWidth(), pokemon.getBbHeight(), pokemon.getYRot(), slot.nativeSeatCount(), slot.index());
        updater.accept(passenger, position.x(), position.y(), position.z());
    }

    public record FallbackSlot(int nativeSeatCount, int index) {}
    public record Position(double x, double y, double z) {}
}

