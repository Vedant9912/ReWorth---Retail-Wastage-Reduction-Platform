package com.reworth.util;

public final class HaversineUtil {
    private static final double R = 6371.0;
    private HaversineUtil() {}

    /** Great-circle distance between two lat/lng points in km. O(1). */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng/2)*Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    public static double round2(double v) { return Math.round(v*100.0)/100.0; }
}
