package app.morphe.extension.tiktok.spoof.location;

import java.util.Locale;
import java.util.MissingResourceException;

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

        String country = Settings.LOCATION_SPOOF_COUNTRY.get();
        double[] preset = CountryLocations.getPresetCoordinates(country);
        if (!CountryLocations.isCustom(country) && preset != null) {
            return preset[0];
        }

        return parseOrDefault(Settings.LOCATION_SPOOF_LAT.get(), original);
    }

    public static double getLongitude(double original) {
        if (!enabled()) return original;

        String country = Settings.LOCATION_SPOOF_COUNTRY.get();
        double[] preset = CountryLocations.getPresetCoordinates(country);
        if (!CountryLocations.isCustom(country) && preset != null) {
            return preset[1];
        }

        return parseOrDefault(Settings.LOCATION_SPOOF_LON.get(), original);
    }

    public static String getCountryCode(String original) {
        if (!enabled()) return original;

        String country = Settings.LOCATION_SPOOF_COUNTRY.get();
        if (CountryLocations.isCustom(country)) {
            country = Settings.SIM_SPOOF_ISO.get();
        }

        if (country == null) return original;
        String trimmed = country.trim().toUpperCase(Locale.ROOT);
        return trimmed.isEmpty() ? original : trimmed;
    }

    public static String getIso3Country(String original) {
        String country = getCountryCode(original);
        if (country == null) return original;

        try {
            return new Locale("", country).getISO3Country();
        } catch (MissingResourceException ignored) {
            return original;
        }
    }
}
