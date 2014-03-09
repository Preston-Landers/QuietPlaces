package edu.utexas.quietplaces;

/**
 * Generic bucket for configuration values.
 *
 */
public class Config {
    public static final String APPNAME = "QuietPlaces";

    // http://developer.android.com/training/location/receive-location-updates.html
    //
    // Basically, we normally only ask for location updates every 20 seconds,
    // but we'll accept updates as often as every 2 seconds, if other apps
    // are requesting more frequent updates.

    public static final long LOCATION_UPDATE_INTERVAL_MS = 20000;
    public static final long LOCATION_FASTEST_INTERVAL_MS = 2000;
}
