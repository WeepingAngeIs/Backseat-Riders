package dev.backseatriders;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Server-owned settings that affect trusted access and generated seats. */
public final class RideablesSettingsData extends SavedData {
    private static final String KEY = "backseat_riders_settings";
    private static final Factory<RideablesSettingsData> FACTORY =
        new Factory<>(RideablesSettingsData::new, RideablesSettingsData::load, null);

    private int minimumSeats = 2;
    private boolean trustedRidersCanBoard = true;
    private boolean trustedRidersCanDrive = true;
    private boolean groundFallbackEnabled = true;
    private boolean friendlyAnimalsEnabled = false;
    private double groundFallbackSpeed = 0.06;
    private final Map<String, Map<Integer, Offset>> seatOffsets = new HashMap<>();
    private final Map<String, Map<Integer, String>> seatLocators = new HashMap<>();
    private final Map<String, Integer> profileSeatCounts = new HashMap<>();

    public static RideablesSettingsData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public int minimumSeats() { return minimumSeats; }
    public boolean trustedRidersCanBoard() { return trustedRidersCanBoard; }
    public boolean trustedRidersCanDrive() { return trustedRidersCanDrive; }
    public boolean groundFallbackEnabled() { return groundFallbackEnabled; }
    public boolean friendlyAnimalsEnabled() { return friendlyAnimalsEnabled; }
    public double groundFallbackSpeed() { return groundFallbackSpeed; }

    public static String profileKey(String species, String form) {
        return normalize(species) + "/" + normalize(form);
    }

    public static String profileKey(PokemonEntity pokemon) {
        return profileKey(pokemon.getPokemon().getSpecies().getName(), pokemon.getPokemon().getForm().getName());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public Offset seatOffset(String profile, int seatIndex) {
        return seatOffsets.getOrDefault(profile, Map.of()).get(seatIndex);
    }

    public String seatLocator(String profile, int seatIndex) {
        return seatLocators.getOrDefault(profile, Map.of()).get(seatIndex);
    }

    public CompoundTag seatLocatorsTag(String profile) {
        CompoundTag result = new CompoundTag();
        seatLocators.getOrDefault(profile, Map.of()).forEach((index, locator) ->
            result.putString(Integer.toString(index), locator));
        return result;
    }

    public boolean setSeatLocator(String profile, int seatIndex, String locator) {
        if (seatIndex < 0 || seatIndex >= 16) throw new IllegalArgumentException("Seat index must be 0-15");
        if (locator == null || locator.isBlank()) return clearSeatLocator(profile, seatIndex);
        if (!SeatEditorPolicy.isValidLocator(locator)) throw new IllegalArgumentException("Invalid locator name");
        Map<Integer, String> locators = seatLocators.computeIfAbsent(profile, ignored -> new HashMap<>());
        if (locator.equals(locators.get(seatIndex))) return false;
        locators.put(seatIndex, locator);
        setDirty();
        return true;
    }

    public boolean clearSeatLocator(String profile, int seatIndex) {
        Map<Integer, String> locators = seatLocators.get(profile);
        if (locators == null || locators.remove(seatIndex) == null) return false;
        if (locators.isEmpty()) seatLocators.remove(profile);
        setDirty();
        return true;
    }

    public boolean replaceSeatLocators(String profile, CompoundTag requested) {
        if (!SeatEditorPolicy.isValidLocators(requested)) throw new IllegalArgumentException("Invalid locator map");
        Map<Integer, String> replacement = new HashMap<>();
        for (String key : requested.getAllKeys()) replacement.put(Integer.parseInt(key), requested.getString(key));
        Map<Integer, String> previous = seatLocators.getOrDefault(profile, Map.of());
        if (previous.equals(replacement)) return false;
        if (replacement.isEmpty()) seatLocators.remove(profile);
        else seatLocators.put(profile, replacement);
        setDirty();
        return true;
    }

    public int seatCount(String profile) {
        return profileSeatCounts.getOrDefault(profile, minimumSeats);
    }

    public CompoundTag seatOffsetsTag(String profile) {
        CompoundTag result = new CompoundTag();
        seatOffsets.getOrDefault(profile, Map.of()).forEach((index, offset) -> {
            CompoundTag row = new CompoundTag();
            row.putDouble("x", offset.x());
            row.putDouble("y", offset.y());
            row.putDouble("z", offset.z());
            result.put(Integer.toString(index), row);
        });
        return result;
    }

    public boolean setSeatCount(String profile, int count) {
        if (count < 2 || count > 16) throw new IllegalArgumentException("Seat count must be 2-16");
        if (profileSeatCounts.getOrDefault(profile, minimumSeats) == count
                && profileSeatCounts.containsKey(profile)) return false;
        profileSeatCounts.put(profile, count);
        setDirty();
        return true;
    }

    public boolean replaceSeatOffsets(String profile, CompoundTag requested) {
        Map<Integer, Offset> replacement = new HashMap<>();
        for (int index = 0; index < 16; index++) {
            String key = Integer.toString(index);
            if (!requested.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) continue;
            CompoundTag row = requested.getCompound(key);
            replacement.put(index, new Offset(clampOffset(row.getDouble("x")),
                clampOffset(row.getDouble("y")), clampOffset(row.getDouble("z"))));
        }
        Map<Integer, Offset> previous = seatOffsets.getOrDefault(profile, Map.of());
        if (previous.equals(replacement)) return false;
        if (replacement.isEmpty()) seatOffsets.remove(profile);
        else seatOffsets.put(profile, replacement);
        setDirty();
        return true;
    }

    public boolean setSeatOffset(String profile, int seatIndex, double x, double y, double z) {
        if (seatIndex < 0 || seatIndex >= 16) throw new IllegalArgumentException("Seat index must be 0-15");
        Offset offset = new Offset(clampOffset(x), clampOffset(y), clampOffset(z));
        Map<Integer, Offset> offsets = seatOffsets.computeIfAbsent(profile, ignored -> new HashMap<>());
        if (offset.equals(offsets.put(seatIndex, offset))) return false;
        setDirty();
        return true;
    }

    public boolean clearSeatOffset(String profile, int seatIndex) {
        Map<Integer, Offset> offsets = seatOffsets.get(profile);
        if (offsets == null || offsets.remove(seatIndex) == null) return false;
        if (offsets.isEmpty()) seatOffsets.remove(profile);
        setDirty();
        return true;
    }

    private static double clampOffset(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Seat offsets must be finite");
        return Math.max(-4.0, Math.min(4.0, value));
    }

    public record Offset(double x, double y, double z) {}

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

    public void setGroundFallbackEnabled(boolean enabled) {
        if (groundFallbackEnabled != enabled) {
            groundFallbackEnabled = enabled;
            setDirty();
        }
    }

    public void setFriendlyAnimalsEnabled(boolean enabled) {
        if (friendlyAnimalsEnabled != enabled) {
            friendlyAnimalsEnabled = enabled;
            setDirty();
        }
    }

    public void setGroundFallbackSpeed(double speed) {
        if (!Double.isFinite(speed)) throw new IllegalArgumentException("Ground fallback speed must be finite");
        double normalized = Math.max(0.02, Math.min(0.12, speed));
        if (Double.compare(groundFallbackSpeed, normalized) != 0) {
            groundFallbackSpeed = normalized;
            setDirty();
        }
    }

    public static RideablesSettingsData load(CompoundTag tag, HolderLookup.Provider registries) {
        RideablesSettingsData data = new RideablesSettingsData();
        if (tag.contains("minimumSeats")) data.minimumSeats = Math.max(2, Math.min(16, tag.getInt("minimumSeats")));
        data.trustedRidersCanBoard = !tag.contains("trustedCanBoard") || tag.getBoolean("trustedCanBoard");
        data.trustedRidersCanDrive = !tag.contains("trustedCanDrive") || tag.getBoolean("trustedCanDrive");
        data.groundFallbackEnabled = !tag.contains("groundFallbackEnabled") || tag.getBoolean("groundFallbackEnabled");
        data.friendlyAnimalsEnabled = tag.getBoolean("friendlyAnimalsEnabled");
        if (tag.contains("groundFallbackSpeed")) {
            try { data.setGroundFallbackSpeed(tag.getDouble("groundFallbackSpeed")); }
            catch (IllegalArgumentException ignored) { /* Keep the conservative default. */ }
        }
        var profiles = tag.getList("seatOffsets", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (net.minecraft.nbt.Tag item : profiles) {
            var row = (CompoundTag) item;
            String profile = row.getString("profile");
            if (row.contains("count")) {
                int count = row.getInt("count");
                if (!profile.isBlank() && count >= 2 && count <= 16) data.profileSeatCounts.put(profile, count);
            }
            var seats = row.getList("seats", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (net.minecraft.nbt.Tag seatTag : seats) {
                var seat = (CompoundTag) seatTag;
                int index = seat.getInt("index");
                if (profile.isBlank() || index < 0 || index >= 16) continue;
                try {
                    data.seatOffsets.computeIfAbsent(profile, ignored -> new HashMap<>()).put(index,
                        new Offset(clampOffset(seat.getDouble("x")), clampOffset(seat.getDouble("y")), clampOffset(seat.getDouble("z"))));
                } catch (IllegalArgumentException ignored) { /* Skip invalid values. */ }
            }
            CompoundTag locators = row.getCompound("locators");
            for (String key : locators.getAllKeys()) {
                try {
                    int index = Integer.parseInt(key);
                    String locator = locators.getString(key);
                    if (index >= 0 && index < 16 && SeatEditorPolicy.isValidLocator(locator)) {
                        data.seatLocators.computeIfAbsent(profile, ignored -> new HashMap<>()).put(index, locator);
                    }
                } catch (NumberFormatException ignored) { /* Skip malformed locator indices. */ }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("minimumSeats", minimumSeats);
        tag.putBoolean("trustedCanBoard", trustedRidersCanBoard);
        tag.putBoolean("trustedCanDrive", trustedRidersCanDrive);
        tag.putBoolean("groundFallbackEnabled", groundFallbackEnabled);
        tag.putBoolean("friendlyAnimalsEnabled", friendlyAnimalsEnabled);
        tag.putDouble("groundFallbackSpeed", groundFallbackSpeed);
        var profiles = new net.minecraft.nbt.ListTag();
        seatOffsets.forEach((profile, offsets) -> {
            CompoundTag row = new CompoundTag();
            row.putString("profile", profile);
            if (profileSeatCounts.containsKey(profile)) row.putInt("count", profileSeatCounts.get(profile));
            var seats = new net.minecraft.nbt.ListTag();
            offsets.forEach((index, offset) -> {
                CompoundTag seat = new CompoundTag();
                seat.putInt("index", index);
                seat.putDouble("x", offset.x());
                seat.putDouble("y", offset.y());
                seat.putDouble("z", offset.z());
                seats.add(seat);
            });
            row.put("seats", seats);
            row.put("locators", seatLocatorsTag(profile));
            profiles.add(row);
        });
        profileSeatCounts.forEach((profile, count) -> {
            if (seatOffsets.containsKey(profile)) return;
            CompoundTag row = new CompoundTag();
            row.putString("profile", profile);
            row.putInt("count", count);
            row.put("seats", new net.minecraft.nbt.ListTag());
            row.put("locators", seatLocatorsTag(profile));
            profiles.add(row);
        });
        seatLocators.forEach((profile, locators) -> {
            if (seatOffsets.containsKey(profile) || profileSeatCounts.containsKey(profile)) return;
            CompoundTag row = new CompoundTag();
            row.putString("profile", profile);
            row.put("seats", new net.minecraft.nbt.ListTag());
            row.put("locators", seatLocatorsTag(profile));
            profiles.add(row);
        });
        tag.put("seatOffsets", profiles);
        return tag;
    }
}

