package dev.backseatriders;

public final class SeatFallbackPolicy {
    private SeatFallbackPolicy() {}

    public static boolean shouldAppendBodyFallback(int filteredNativeSeatCount) {
        return filteredNativeSeatCount == 1;
    }
}
