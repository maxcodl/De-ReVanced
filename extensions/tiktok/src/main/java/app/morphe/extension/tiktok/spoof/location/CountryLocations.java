package app.morphe.extension.tiktok.spoof.location;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CountryLocations {
    private static final String CUSTOM = "custom";

    private static final Map<String, double[]> COUNTRY_COORDS = new HashMap<>();

    static {
        COUNTRY_COORDS.put("us", new double[]{37.0902, -95.7129});
        COUNTRY_COORDS.put("gb", new double[]{55.3781, -3.4360});
        COUNTRY_COORDS.put("ca", new double[]{56.1304, -106.3468});
        COUNTRY_COORDS.put("au", new double[]{-25.2744, 133.7751});
        COUNTRY_COORDS.put("nz", new double[]{-40.9006, 174.8860});
        COUNTRY_COORDS.put("jp", new double[]{36.2048, 138.2529});
        COUNTRY_COORDS.put("kr", new double[]{35.9078, 127.7669});
        COUNTRY_COORDS.put("in", new double[]{20.5937, 78.9629});
        COUNTRY_COORDS.put("sg", new double[]{1.3521, 103.8198});
        COUNTRY_COORDS.put("my", new double[]{4.2105, 101.9758});
        COUNTRY_COORDS.put("th", new double[]{15.8700, 100.9925});
        COUNTRY_COORDS.put("id", new double[]{-0.7893, 113.9213});
        COUNTRY_COORDS.put("ph", new double[]{12.8797, 121.7740});
        COUNTRY_COORDS.put("vn", new double[]{14.0583, 108.2772});
        COUNTRY_COORDS.put("ae", new double[]{23.4241, 53.8478});
        COUNTRY_COORDS.put("sa", new double[]{23.8859, 45.0792});
        COUNTRY_COORDS.put("tr", new double[]{38.9637, 35.2433});
        COUNTRY_COORDS.put("de", new double[]{51.1657, 10.4515});
        COUNTRY_COORDS.put("fr", new double[]{46.2276, 2.2137});
        COUNTRY_COORDS.put("es", new double[]{40.4637, -3.7492});
        COUNTRY_COORDS.put("it", new double[]{41.8719, 12.5674});
        COUNTRY_COORDS.put("nl", new double[]{52.1326, 5.2913});
        COUNTRY_COORDS.put("be", new double[]{50.5039, 4.4699});
        COUNTRY_COORDS.put("se", new double[]{60.1282, 18.6435});
        COUNTRY_COORDS.put("no", new double[]{60.4720, 8.4689});
        COUNTRY_COORDS.put("fi", new double[]{61.9241, 25.7482});
        COUNTRY_COORDS.put("dk", new double[]{56.2639, 9.5018});
        COUNTRY_COORDS.put("pl", new double[]{51.9194, 19.1451});
        COUNTRY_COORDS.put("ch", new double[]{46.8182, 8.2275});
        COUNTRY_COORDS.put("at", new double[]{47.5162, 14.5501});
        COUNTRY_COORDS.put("pt", new double[]{39.3999, -8.2245});
        COUNTRY_COORDS.put("ie", new double[]{53.1424, -7.6921});
        COUNTRY_COORDS.put("gr", new double[]{39.0742, 21.8243});
        COUNTRY_COORDS.put("ro", new double[]{45.9432, 24.9668});
        COUNTRY_COORDS.put("hu", new double[]{47.1625, 19.5033});
        COUNTRY_COORDS.put("cz", new double[]{49.8175, 15.4730});
        COUNTRY_COORDS.put("ua", new double[]{48.3794, 31.1656});
        COUNTRY_COORDS.put("ru", new double[]{61.5240, 105.3188});
        COUNTRY_COORDS.put("mx", new double[]{23.6345, -102.5528});
        COUNTRY_COORDS.put("br", new double[]{-14.2350, -51.9253});
        COUNTRY_COORDS.put("ar", new double[]{-38.4161, -63.6167});
        COUNTRY_COORDS.put("cl", new double[]{-35.6751, -71.5430});
        COUNTRY_COORDS.put("co", new double[]{4.5709, -74.2973});
        COUNTRY_COORDS.put("pe", new double[]{-9.1900, -75.0152});
        COUNTRY_COORDS.put("za", new double[]{-30.5595, 22.9375});
        COUNTRY_COORDS.put("eg", new double[]{26.8206, 30.8025});
        COUNTRY_COORDS.put("ng", new double[]{9.0820, 8.6753});
        COUNTRY_COORDS.put("ma", new double[]{31.7917, -7.0926});
    }

    private CountryLocations() {
    }

    public static String[] getCountryEntries() {
        String[] countries = Locale.getISOCountries();
        Arrays.sort(countries, COUNTRY_COMPARATOR);

        String[] entries = new String[countries.length + 1];
        entries[0] = "Custom location";
        for (int i = 0; i < countries.length; i++) {
            String code = countries[i];
            entries[i + 1] = new Locale("", code).getDisplayCountry(Locale.ENGLISH) + " (" + code + ")";
        }
        return entries;
    }

    public static String[] getCountryValues() {
        String[] countries = Locale.getISOCountries();
        Arrays.sort(countries, COUNTRY_COMPARATOR);

        String[] values = new String[countries.length + 1];
        values[0] = CUSTOM;
        for (int i = 0; i < countries.length; i++) {
            values[i + 1] = countries[i].toLowerCase(Locale.ROOT);
        }
        return values;
    }

    public static double[] getPresetCoordinates(String countryCode) {
        if (countryCode == null) return null;
        return COUNTRY_COORDS.get(countryCode.toLowerCase(Locale.ROOT));
    }

    public static boolean isCustom(String countryCode) {
        return countryCode == null || CUSTOM.equalsIgnoreCase(countryCode);
    }

    private static final Comparator<String> COUNTRY_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            String aName = new Locale("", a).getDisplayCountry(Locale.ENGLISH);
            String bName = new Locale("", b).getDisplayCountry(Locale.ENGLISH);
            return aName.compareTo(bName);
        }
    };
}
