package dev.backseatriders;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world, UUID-only rider permissions. Names are resolved only for display. */
public final class RideableTrustData extends SavedData {
    private static final String KEY = "backseat_riders_trust";
    private final Map<UUID, Entry> entries = new HashMap<>();

    private record Entry(UUID owner, Set<UUID> riders) {}

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
        UUID owner = ownerId(entity);
        if (owner == null) return false;
        Entry entry = get(((net.minecraft.server.level.ServerPlayer) player).serverLevel()).entries.get(entity.getUUID());
        return entry != null && entry.owner().equals(owner) && entry.riders().contains(player.getUUID());
    }

    public static UUID ownerId(Entity entity) {
        try {
            Method method = entity.getClass().getMethod("getOwnerUUID");
            Object value = method.invoke(entity);
            return value instanceof UUID id ? id : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public boolean trust(UUID pokemon, UUID rider) {
        Entry old = entries.get(pokemon);
        if (old != null && old.riders().contains(rider)) return false;
        UUID owner = old == null ? null : old.owner();
        // Owner is captured from the live Pok�mon by registerOwner before this is called.
        if (owner == null) return false;
        Set<UUID> riders = old == null ? new HashSet<>() : new HashSet<>(old.riders());
        riders.add(rider);
        entries.put(pokemon, new Entry(owner, riders));
        setDirty();
        return true;
    }

    public void registerOwner(UUID pokemon, UUID owner) {
        Entry old = entries.get(pokemon);
        if (old == null || !owner.equals(old.owner())) {
            // Ownership changes revoke all former owner's grants.
            entries.put(pokemon, new Entry(owner, new HashSet<>()));
            setDirty();
        }
    }

    public boolean revoke(UUID pokemon, UUID rider) {
        Entry old = entries.get(pokemon);
        if (old == null || !old.riders().contains(rider)) return false;
        Set<UUID> riders = new HashSet<>(old.riders());
        riders.remove(rider);
        entries.put(pokemon, new Entry(old.owner(), riders));
        setDirty();
        return true;
    }

    public Set<UUID> trusted(UUID pokemon) {
        Entry entry = entries.get(pokemon);
        return entry == null ? Set.of() : Set.copyOf(entry.riders());
    }

    public static RideableTrustData load(CompoundTag tag, HolderLookup.Provider registries) {
        RideableTrustData data = new RideableTrustData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag row = (CompoundTag) element;
            try {
                UUID pokemon = row.getUUID("pokemon");
                UUID owner = row.getUUID("owner");
                Set<UUID> riders = new HashSet<>();
                ListTag trusted = row.getList("riders", Tag.TAG_COMPOUND);
                for (Tag id : trusted) riders.add(((CompoundTag) id).getUUID("id"));
                data.entries.put(pokemon, new Entry(owner, riders));
            } catch (IllegalArgumentException ignored) { /* Skip malformed rows. */ }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((pokemon, entry) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("pokemon", pokemon);
            row.putUUID("owner", entry.owner());
            ListTag riders = new ListTag();
            entry.riders().forEach(id -> { CompoundTag value = new CompoundTag(); value.putUUID("id", id); riders.add(value); });
            row.put("riders", riders);
            list.add(row);
        });
        tag.put("entries", list);
        return tag;
    }
}

