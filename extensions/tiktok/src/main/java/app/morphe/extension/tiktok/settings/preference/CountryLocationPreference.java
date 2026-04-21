package app.morphe.extension.tiktok.settings.preference;

import android.content.Context;
import android.preference.ListPreference;
import android.view.View;

import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.tiktok.Utils;
import app.morphe.extension.tiktok.spoof.location.CountryLocations;

@SuppressWarnings("deprecation")
public class CountryLocationPreference extends ListPreference {
    public CountryLocationPreference(Context context, StringSetting setting) {
        super(context);

        setTitle("Country preset");
        setSummary("Pick a country preset or use custom latitude/longitude.");
        setKey(setting.key);

        setEntries(CountryLocations.getCountryEntries());
        setEntryValues(CountryLocations.getCountryValues());
        setValue(setting.get());
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Utils.setTitleAndSummaryColor(view);
    }
}
