package dev.backseatriders;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/** Owner-wide UUID rider permissions. Names are resolved only for display. */
public final class RideableTrustData extends SavedData {
    private static final String KEY = "backseat_riders_trust";
    private final Map<UUID, Set<UUID>> entries = new HashMap<>();
    private static final Factory<RideableTrustData> FACTORY = new Factory<>(RideableTrustData::new, RideableTrustData::load, null);

    public static RideableTrustData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public static boolean isPokemon(Entity entity) {
        return entity.getClass().getName().equals("com.cobblemon.mod.common.entity.pokemon.PokemonEntity");
    }

    public static boolean isOwner(Entity entity, Player player) {
        UUID owner = ownerId(entity);
        return owner != null && owner.equals(player.getUUID());
    }

    public static boolean canRide(Entity entity, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        UUID owner = ownerId(entity);
        return owner != null && entity.isAlive() && hasFreeSeat(entity)
            && TrustPolicy.permits(owner, player.getUUID(), get(serverPlayer.serverLevel()).trusted(owner));
    }

    private static boolean hasFreeSeat(Entity entity) {
        try {
            Object seats = entity.getClass().getMethod("getSeats").invoke(entity);
            Object occupied = entity.getClass().getMethod("getOccupiedSeats").invoke(entity);
            return seats instanceof List<?> seatList && occupied instanceof Map<?, ?> occupiedMap
                && seatList.size() > occupiedMap.size();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static UUID ownerId(Entity entity) {
        try {
            Object value = entity.getClass().getMethod("getOwnerUUID").invoke(entity);
            return value instanceof UUID id ? id : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public boolean trust(UUID owner, UUID rider) {
        if (!entries.computeIfAbsent(owner, ignored -> new HashSet<>()).add(rider)) return false;
        setDirty();
        return true;
    }

    public boolean revoke(UUID owner, UUID rider) {
        Set<UUID> riders = entries.get(owner);
        if (riders == null || !riders.remove(rider)) return false;
        if (riders.isEmpty()) entries.remove(owner);
        setDirty();
        return true;
    }

    public Set<UUID> trusted(UUID owner) { return Set.copyOf(entries.getOrDefault(owner, Set.of())); }

    public static RideableTrustData load(CompoundTag tag, HolderLookup.Provider registries) {
        RideableTrustData data = new RideableTrustData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag row = (CompoundTag) element;
            try {
                UUID owner = row.getUUID("owner");
                Set<UUID> riders = new HashSet<>();
                ListTag trusted = row.getList("riders", Tag.TAG_COMPOUND);
                for (Tag id : trusted) riders.add(((CompoundTag) id).getUUID("id"));
                if (!riders.isEmpty()) data.entries.put(owner, riders);
            } catch (IllegalArgumentException ignored) { /* Skip malformed rows. */ }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((owner, riderIds) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("owner", owner);
            ListTag riders = new ListTag();
            riderIds.forEach(id -> { CompoundTag value = new CompoundTag(); value.putUUID("id", id); riders.add(value); });
            row.put("riders", riders);
            list.add(row);
        });
        tag.put("entries", list);
        return tag;
    }
}

