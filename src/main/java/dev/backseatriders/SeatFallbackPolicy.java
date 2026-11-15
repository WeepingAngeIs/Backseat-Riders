package dev.backseatriders;

public final class SeatFallbackPolicy {
    private SeatFallbackPolicy() {}

    public static int effectiveSeatCount(int filteredNativeSeatCount, boolean canSynthesizeEmptyProfile,
                                         int minimumSeatCount, int occupiedSeatCount,
                                         int highestOccupiedFallbackIndex) {
        if (filteredNativeSeatCount == 0 && !canSynthesizeEmptyProfile) {
            return 0;
        }
        if (filteredNativeSeatCount >= SeatFallbackPosition.MAX_SEATS) return filteredNativeSeatCount;
        int required = Math.max(filteredNativeSeatCount, Math.max(2, Math.min(16, minimumSeatCount)));
        required = Math.max(required, occupiedSeatCount);
        return Math.min(SeatFallbackPosition.MAX_SEATS,
            Math.max(required, highestOccupiedFallbackIndex + 1));
    }
}

