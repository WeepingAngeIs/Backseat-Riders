package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Stable seats and bounded body-relative placement for forms without a model locator. */
public final class SeatFallbackPosition {
    private static final String LOCATOR_PREFIX = "backseatriders:body_fallback_";
    public static final int MAX_SEATS = 16;
    private static final Map<SeatKey, WeakReference<Seat>> LOCATOR_SEATS = new HashMap<>();
    private static final Map<Seat, FallbackSlot> GENERATED_SEAT_SLOTS =
        Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Delay Cobblemon class initialization until a fallback Seat is actually requested. */
    private static final class SeatPool {
        private static final Seat[][] SEATS = createSeatPool();
    }

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

    public static synchronized Seat seat(int nativeSeatCount, int index) {
        return seat(nativeSeatCount, index, null);
    }

    public static synchronized Seat seat(int nativeSeatCount, int index, String modelLocator) {
        if (nativeSeatCount < 0 || nativeSeatCount > MAX_SEATS || index < 0 || index >= MAX_SEATS) {
            throw new IllegalArgumentException("Fallback seat index is outside the supported range");
        }
        if (modelLocator == null || modelLocator.isBlank()) {
            Seat seat = SeatPool.SEATS[nativeSeatCount][index];
            GENERATED_SEAT_SLOTS.put(seat, new FallbackSlot(nativeSeatCount, index));
            return seat;
        }
        SeatKey key = new SeatKey(nativeSeatCount, index, modelLocator);
        WeakReference<Seat> ref = LOCATOR_SEATS.get(key);
        Seat seat = ref == null ? null : ref.get();
        if (seat == null) {
            seat = new Seat(modelLocator, null, List.of(), false);
            LOCATOR_SEATS.put(key, new WeakReference<>(seat));
        }
        GENERATED_SEAT_SLOTS.put(seat, new FallbackSlot(nativeSeatCount, index));
        return seat;
    }

    private static String locator(int nativeCount, int index) {
        return LOCATOR_PREFIX + nativeCount + "_" + index;
    }

    public static FallbackSlot slot(Seat seat) {
        return seat == null ? null : GENERATED_SEAT_SLOTS.get(seat);
    }

    public static boolean isGeneratedSeat(PokemonEntity pokemon, Entity passenger) {
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
        Position custom = configuredOffset(pokemon, slot);
        Position position = custom == null
            ? bodyRelativePosition(pokemon.getX(), pokemon.getY(), pokemon.getZ(),
                pokemon.getBbWidth(), pokemon.getBbHeight(), pokemon.getYRot(), slot.nativeSeatCount(), slot.index())
            : localToWorld(pokemon.getX(), pokemon.getY(), pokemon.getZ(), pokemon.getYRot(), custom);
        updater.accept(passenger, position.x(), position.y(), position.z());
    }

    public static void applyLocatorOffset(PokemonEntity pokemon, Entity passenger, Entity.MoveFunction updater) {
        FallbackSlot slot = slot(pokemon.getSeatForPassenger(passenger));
        if (slot == null) return;
        Position offset = configuredOffset(pokemon, slot);
        if (offset == null) return;
        Position worldOffset = localToWorld(0.0, 0.0, 0.0, pokemon.getYRot(), offset);
        updater.accept(passenger, passenger.getX() + worldOffset.x(), passenger.getY() + worldOffset.y(),
            passenger.getZ() + worldOffset.z());
    }

    private static Position configuredOffset(PokemonEntity pokemon, FallbackSlot slot) {
        PokemonSeatFallbackAccess access = (PokemonSeatFallbackAccess) pokemon;
        if (pokemon.level() instanceof ServerLevel serverLevel) {
            RideablesSettingsData settings = RideablesSettingsData.get(serverLevel);
            String profile = RideablesSettingsData.profileKey(pokemon);
            CompoundTag synchronizedOffsets = new CompoundTag();
            for (int index = 0; index < MAX_SEATS; index++) {
                RideablesSettingsData.Offset offset = settings.seatOffset(profile, index);
                if (offset == null) continue;
                CompoundTag value = new CompoundTag();
                value.putDouble("x", offset.x());
                value.putDouble("y", offset.y());
                value.putDouble("z", offset.z());
                synchronizedOffsets.put(Integer.toString(index), value);
            }
            access.backseatriders$setSeatOffsets(synchronizedOffsets);
            RideablesSettingsData.Offset offset = settings.seatOffset(profile, slot.index());
            return offset == null ? null : new Position(offset.x(), offset.y(), offset.z());
        }
        CompoundTag synchronizedOffsets = access.backseatriders$getSeatOffsets();
        String key = Integer.toString(slot.index());
        if (!synchronizedOffsets.contains(key)) return null;
        CompoundTag offset = synchronizedOffsets.getCompound(key);
        return new Position(offset.getDouble("x"), offset.getDouble("y"), offset.getDouble("z"));
    }

    private static Position localToWorld(double x, double y, double z, float yawDegrees, Position local) {
        double yaw = Math.toRadians(yawDegrees);
        return new Position(x + local.x() * Math.cos(yaw) + local.z() * Math.sin(yaw), y + local.y(),
            z - local.x() * Math.sin(yaw) + local.z() * Math.cos(yaw));
    }

    public record FallbackSlot(int nativeSeatCount, int index) {}
    public record Position(double x, double y, double z) {}
    private record SeatKey(int nativeSeatCount, int index, String locator) {}
}

