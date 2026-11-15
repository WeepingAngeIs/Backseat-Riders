package dev.backseatriders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

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

    public static <T> List<T> ensureCount(List<T> nativeItems, int targetCount, IntFunction<T> fallbackFactory) {
        if (targetCount <= nativeItems.size()) return nativeItems;
        List<T> extended = new ArrayList<>(nativeItems);
        for (int index = nativeItems.size(); index < targetCount; index++) {
            extended.add(fallbackFactory.apply(index));
        }
        return extended;
    }
}

