package dev.backseatriders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/** Operator-assigned ownership for explicitly registered friendly animals. */
public final class FriendlyAnimalData extends SavedData {
    private static final String KEY = "backseat_riders_friendly_animals";
    private static final Factory<FriendlyAnimalData> FACTORY =
        new Factory<>(FriendlyAnimalData::new, FriendlyAnimalData::load, null);
    private final Map<UUID, UUID> owners = new HashMap<>();

    public static FriendlyAnimalData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public UUID owner(UUID animalId) { return owners.get(animalId); }
    public UUID owner(Entity animal) { return owners.get(animal.getUUID()); }
    public boolean isRegistered(Entity animal) { return owners.containsKey(animal.getUUID()); }

    public boolean assign(UUID animalId, UUID ownerId) {
        if (ownerId.equals(owners.put(animalId, ownerId))) return false;
        setDirty();
        return true;
    }

    public boolean remove(UUID animalId) {
        if (owners.remove(animalId) == null) return false;
        setDirty();
        return true;
    }

    public static FriendlyAnimalData load(CompoundTag tag, HolderLookup.Provider registries) {
        FriendlyAnimalData data = new FriendlyAnimalData();
        ListTag rows = tag.getList("animals", Tag.TAG_COMPOUND);
        for (Tag value : rows) {
            CompoundTag row = (CompoundTag) value;
            try {
                data.owners.put(row.getUUID("animal"), row.getUUID("owner"));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed ownership rows rather than blocking world startup.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag rows = new ListTag();
        owners.forEach((animal, owner) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("animal", animal);
            row.putUUID("owner", owner);
            rows.add(row);
        });
        tag.put("animals", rows);
        return tag;
    }
}
