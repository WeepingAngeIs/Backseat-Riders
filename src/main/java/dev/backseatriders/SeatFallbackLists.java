package dev.backseatriders;

import java.util.ArrayList;
import java.util.List;

public final class SeatFallbackLists {
    private SeatFallbackLists() {}

    public static <T> List<T> appendIfSingle(List<T> nativeItems, T fallbackItem) {
        if (nativeItems.size() != 1) {
            return nativeItems;
        }
        List<T> extended = new ArrayList<>(nativeItems);
        extended.add(fallbackItem);
        return extended;
    }
}
