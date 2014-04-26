package edu.utexas.quietplaces;

import android.app.AlarmManager;
import android.content.Context;


public class PlacesConstants {

    public static boolean DEVELOPER_MODE = true;

    public static String PLACES_LIST_BASE_URI = "https://maps.googleapis.com/maps/api/place/search/xml?sensor=true";
    public static String PLACES_DETAIL_BASE_URI = "https://maps.googleapis.com/maps/api/place/details/xml?sensor=true&reference=";

    // The default search radius when searching for places nearby.
    public static int DEFAULT_RADIUS = 300; // 150;

    // The maximum distance (meters) the user should travel between Places updates.
    public static int MAX_DISTANCE = 75;

    // The maximum time that should pass before the user gets a location update.
    public static long MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    // Use the GPS (fine location provider) when the Activity is visible?
    public static boolean USE_GPS_WHEN_ACTIVITY_VISIBLE = true;

    // Maximum latency before you force a cached detail page to be updated.
    public static long MAX_DETAILS_UPDATE_LATENCY = AlarmManager.INTERVAL_DAY;

    // Prefetching place details is useful but potentially expensive. The following
    // values lets you disable prefetching when on mobile data or low battery conditions.
    // Only prefetch on WIFI?
    public static boolean PREFETCH_ON_WIFI_ONLY = false;
    // Disable prefetching when battery is low?
    public static boolean DISABLE_PREFETCH_ON_LOW_BATTERY = true;


    // The maximum number of locations to prefetch for each update.
    public static int PREFETCH_LIMIT = 5;

    // Places API returns up to 60 results per query, but split across 3 requests
    // The next page doesn't become available immediately, so we wait this amount of
    // time before trying.
    // TODO: make the code wait a shorter time, but handle the INVALID_REQUEST by retrying it
    public static final long PLACES_NEXT_PAGE_INTERVAL_MS = 1500;

    /**
     * These values are constants used for intents, extras, and shared preferences.
     */
    public static String SP_KEY_LAST_LIST_UPDATE_TIME = "SP_KEY_LAST_LIST_UPDATE_TIME";
    public static String SP_KEY_LAST_LIST_UPDATE_LAT = "SP_KEY_LAST_LIST_UPDATE_LAT";
    public static String SP_KEY_LAST_LIST_UPDATE_LNG = "SP_KEY_LAST_LIST_UPDATE_LNG";
    public static String SP_KEY_RUN_ONCE = "SP_KEY_RUN_ONCE";
    public static String SP_KEY_API_PLACE_TYPES = "SP_API_PLACE_TYPES";

    // Default Place types to search for...
    public static String SP_KEY_API_PLACE_TYPES_DEFAULT = Config.joinString(Config.PLACE_TYPE_DEFAULTS, "|");

    public static String EXTRA_KEY_REFERENCE = "reference";
    public static String EXTRA_KEY_ID = "id";
    public static String EXTRA_KEY_LOCATION = "location";
    public static String EXTRA_KEY_RADIUS = "radius";
    public static String EXTRA_KEY_TIME_STAMP = "time_stamp";
    public static String EXTRA_KEY_FORCEREFRESH = "force_refresh";
    public static String EXTRA_KEY_IN_BACKGROUND = "EXTRA_KEY_IN_BACKGROUND";

    public static String ARGUMENTS_KEY_REFERENCE = "reference";
    public static String ARGUMENTS_KEY_ID = "id";

    public static boolean SUPPORTS_GINGERBREAD = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
    public static boolean SUPPORTS_HONEYCOMB = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    public static boolean SUPPORTS_FROYO = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO;
    public static boolean SUPPORTS_ECLAIR = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR;

    public static String CONSTRUCTED_LOCATION_PROVIDER = "CONSTRUCTED_LOCATION_PROVIDER";


/*
    public static String readFully(InputStream inputStream, String encoding)
            throws IOException {
        return new String(readFully(inputStream), encoding);
    }

    private static byte[] readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
    }
*/

    public static String getPlacesAPIKey(Context context, boolean addParamKey) {
        String apiKey = context.getString(R.string.google_browser_api_key);
        if (addParamKey) {
            return "&key=" + apiKey;
        }
        else {
            return apiKey;
        }
    }

}
