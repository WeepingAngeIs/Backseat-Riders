package dev.backseatriders;

import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Input validation shared by the server payload handler and unit tests. */
public final class SeatEditorPolicy {
    private static final Pattern PROFILE = Pattern.compile("[a-z0-9_.-]{1,64}/[a-z0-9_.-]{1,64}");

    private SeatEditorPolicy() {}

    public static boolean isValidProfile(String profile) {
        return profile != null && PROFILE.matcher(profile).matches();
    }

    public static boolean isValidSeatCount(int count) {
        return count >= 2 && count <= 16;
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

