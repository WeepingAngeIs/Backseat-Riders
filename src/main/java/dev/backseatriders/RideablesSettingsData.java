package dev.backseatriders;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-owned settings that affect trusted access and generated seats. */
public final class RideablesSettingsData extends SavedData {
    private static final String KEY = "backseat_riders_settings";
    private static final Factory<RideablesSettingsData> FACTORY =
        new Factory<>(RideablesSettingsData::new, RideablesSettingsData::load, null);

    private int minimumSeats = 2;
    private boolean trustedRidersCanBoard = true;
    private boolean trustedRidersCanDrive = true;

    public static RideablesSettingsData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public int minimumSeats() { return minimumSeats; }
    public boolean trustedRidersCanBoard() { return trustedRidersCanBoard; }
    public boolean trustedRidersCanDrive() { return trustedRidersCanDrive; }

    public void setMinimumSeats(int seats) {
        int normalized = Math.max(2, Math.min(16, seats));
        if (minimumSeats != normalized) {
            minimumSeats = normalized;
            setDirty();
        }
    }

    public void setTrustedRidersCanBoard(boolean enabled) {
        if (trustedRidersCanBoard != enabled) {
            trustedRidersCanBoard = enabled;
            setDirty();
        }
    }

    public void setTrustedRidersCanDrive(boolean enabled) {
        if (trustedRidersCanDrive != enabled) {
            trustedRidersCanDrive = enabled;
            setDirty();
        }
    }

    public static RideablesSettingsData load(CompoundTag tag, HolderLookup.Provider registries) {
        RideablesSettingsData data = new RideablesSettingsData();
        if (tag.contains("minimumSeats")) data.minimumSeats = Math.max(2, Math.min(16, tag.getInt("minimumSeats")));
        data.trustedRidersCanBoard = !tag.contains("trustedCanBoard") || tag.getBoolean("trustedCanBoard");
        data.trustedRidersCanDrive = !tag.contains("trustedCanDrive") || tag.getBoolean("trustedCanDrive");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("minimumSeats", minimumSeats);
        tag.putBoolean("trustedCanBoard", trustedRidersCanBoard);
        tag.putBoolean("trustedCanDrive", trustedRidersCanDrive);
        return tag;
    }
}

