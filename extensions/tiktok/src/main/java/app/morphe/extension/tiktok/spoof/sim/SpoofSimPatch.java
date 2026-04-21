/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/spoof/sim/SpoofSimPatch.java
 */
package app.morphe.extension.tiktok.spoof.sim;

import java.util.Locale;

import app.morphe.extension.tiktok.settings.Settings;

public class SpoofSimPatch {
    private static boolean enabled() {
        return Settings.SIM_SPOOF.get();
    }

    private static String pickValue(String spoofed, String original) {
        if (spoofed == null) return original;

        String trimmed = spoofed.trim();
        return trimmed.isEmpty() ? original : trimmed;
    }

    public static String getCountryIso(String original) {
        if (!enabled()) return original;

        String spoofed = pickValue(Settings.SIM_SPOOF_ISO.get(), original);
        return spoofed == null ? null : spoofed.toLowerCase(Locale.ROOT);
    }

    public static String getOperator(String original) {
        if (!enabled()) return original;

        return pickValue(Settings.SIMSPOOF_MCCMNC.get(), original);
    }

    public static String getOperatorName(String original) {
        if (!enabled()) return original;

        return pickValue(Settings.SIMSPOOF_OP_NAME.get(), original);
    }
}
