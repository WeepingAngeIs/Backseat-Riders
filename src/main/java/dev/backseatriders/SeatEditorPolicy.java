package dev.backseatriders;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Input validation shared by the server payload handler and unit tests. */
public final class SeatEditorPolicy {
    private static final Pattern PROFILE = Pattern.compile("[a-z0-9_.-]{1,64}/[a-z0-9_.-]{1,64}");
    private static final Pattern LOCATOR = Pattern.compile("[A-Za-z0-9_.:-]{1,64}");

    private SeatEditorPolicy() {}

    public static boolean isValidProfile(String profile) {
        return profile != null && PROFILE.matcher(profile).matches();
    }

    public static boolean isValidSeatCount(int count) {
        return count >= 2 && count <= 16;
    }

    public static boolean isValidLocator(String locator) {
        return locator != null && LOCATOR.matcher(locator).matches();
    }

    public static boolean isValidLocators(CompoundTag locators) {
        if (locators == null || locators.size() > 16) return false;
        Map<String, String> values = new HashMap<>();
        for (String key : locators.getAllKeys()) {
            if (!locators.contains(key, Tag.TAG_STRING)) return false;
            values.put(key, locators.getString(key));
        }
        return isValidLocatorMap(values);
    }

    public static boolean isValidLocatorMap(Map<String, String> locators) {
        if (locators == null || locators.size() > 16) return false;
        for (Map.Entry<String, String> entry : locators.entrySet()) {
            int index;
            try { index = Integer.parseInt(entry.getKey()); }
            catch (NumberFormatException invalid) { return false; }
            if (index < 0 || index >= 16 || !isValidLocator(entry.getValue())) return false;
        }
        return true;
    }

    public static boolean isValidOffsets(CompoundTag offsets) {
        if (offsets == null || offsets.size() > 16) return false;
        for (String key : offsets.getAllKeys()) {
            int index;
            try { index = Integer.parseInt(key); }
            catch (NumberFormatException invalid) { return false; }
            if (index < 0 || index >= 16 || !offsets.contains(key, Tag.TAG_COMPOUND)) return false;
            CompoundTag row = offsets.getCompound(key);
            if (row.size() != 3 || !row.contains("x", Tag.TAG_DOUBLE) || !row.contains("y", Tag.TAG_DOUBLE)
                    || !row.contains("z", Tag.TAG_DOUBLE)
                    || !isValidOffset(row.getDouble("x"), row.getDouble("y"), row.getDouble("z"))) return false;
        }
        return true;
    }

    public static boolean isValidOffset(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
            && Math.abs(x) <= 4.0 && Math.abs(y) <= 4.0 && Math.abs(z) <= 4.0;
    }
}

