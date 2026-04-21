/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/categories/SimSpoofPreferenceCategory.java
 */

package app.morphe.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.tiktok.settings.Settings;
import app.morphe.extension.tiktok.settings.SettingsStatus;
import app.morphe.extension.tiktok.settings.preference.InputTextPreference;
import app.morphe.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class SimSpoofPreferenceCategory extends ConditionalPreferenceCategory {
    public SimSpoofPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Bypass regional restriction");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.simSpoofEnabled || SettingsStatus.locationSpoofEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                "Fake sim card info",
                "Bypass regional restriction by fake sim card information.",
                Settings.SIM_SPOOF
        ));
        addPreference(new InputTextPreference(
                context,
                "Country ISO", "us, uk, jp, ...",
                Settings.SIM_SPOOF_ISO
        ));
        addPreference(new InputTextPreference(
                context,
                "Operator mcc+mnc", "mcc+mnc",
                Settings.SIMSPOOF_MCCMNC
        ));
        addPreference(new InputTextPreference(
                context,
                "Operator name", "Name of the operator.",
                Settings.SIMSPOOF_OP_NAME
        ));

        addPreference(new TogglePreference(
                context,
                "Fake location",
                "Spoof latitude and longitude values used by app location checks.",
                Settings.LOCATION_SPOOF
        ));
        addPreference(new InputTextPreference(
                context,
                "Latitude", "e.g. 37.7749",
                Settings.LOCATION_SPOOF_LAT
        ));
        addPreference(new InputTextPreference(
                context,
                "Longitude", "e.g. -122.4194",
                Settings.LOCATION_SPOOF_LON
        ));
    }
}
