package dev.backseatriders;

/** Dependency-free geometry for generated body-attached seats. */
public final class SeatFallbackGeometry {
    private SeatFallbackGeometry() {}

    public static Position bodyRelativePosition(double x, double y, double z,
                                                double width, double height, float yawDegrees,
                                                int nativeSeatCount, int seatIndex) {
        double safeWidth = Math.max(0.1, Math.min(3.0, width));
        double safeHeight = Math.max(0.1, Math.min(8.0, height));
        double longitudinal = nativeSeatCount == 0
            ? (seatIndex == 0 ? safeWidth * 0.18 : -Math.min(0.9, safeWidth * (0.18 + 0.12 * (seatIndex - 1))))
            : -Math.min(0.9, safeWidth * (0.3 + 0.18 * (seatIndex - nativeSeatCount)));
        double yOffset = Math.max(0.15, Math.min(safeHeight * 0.78, safeHeight * 0.72));
        double yaw = Math.toRadians(yawDegrees);
        return new Position(x + Math.sin(yaw) * longitudinal, y + yOffset, z - Math.cos(yaw) * longitudinal);
    }

    public record Position(double x, double y, double z) {}
}

