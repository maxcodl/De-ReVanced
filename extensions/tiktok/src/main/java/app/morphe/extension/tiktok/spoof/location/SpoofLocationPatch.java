package app.morphe.extension.tiktok.spoof.location;

import app.morphe.extension.tiktok.settings.Settings;

public class SpoofLocationPatch {
    private static boolean enabled() {
        return Settings.LOCATION_SPOOF.get();
    }

    private static double parseOrDefault(String value, double fallback) {
        if (value == null) return fallback;

        String trimmed = value.trim();
        if (trimmed.isEmpty()) return fallback;

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static double getLatitude(double original) {
        if (!enabled()) return original;

        return parseOrDefault(Settings.LOCATION_SPOOF_LAT.get(), original);
    }

    public static double getLongitude(double original) {
        if (!enabled()) return original;

        return parseOrDefault(Settings.LOCATION_SPOOF_LON.get(), original);
    }
}
