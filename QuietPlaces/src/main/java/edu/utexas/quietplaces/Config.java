package edu.utexas.quietplaces;

import android.content.Context;
import android.graphics.Color;
import com.google.android.gms.location.Geofence;

/**
 * Generic bucket for configuration values.
 *
 */
public class Config {
    public static final String APP_NAME = "QuietPlaces";
    public static final String PACKAGE_NAME = "edu.utexas.quietplaces";

    // http://developer.android.com/training/location/receive-location-updates.html
    //
    // Basically, we normally only ask for location updates every 90 seconds,
    // but we'll accept updates as often as every 10 seconds, if other apps
    // are requesting more frequent updates.

    public static final long LOCATION_UPDATE_INTERVAL_MS = 90000;
    public static final long LOCATION_FASTEST_INTERVAL_MS = 10000;

    // Probably belongs in an XML file somewhere
    public static final int QP_CIRCLE_STROKE_COLOR = Color.argb(150, 0, 0, 255);
    public static final int QP_CIRCLE_FILL_COLOR = Color.argb(100, 138, 241, 255);
    public static final int QP_CIRCLE_SELECTED_FILL_COLOR = Color.argb(100, 250, 35, 35);
    public static final float QP_CIRCLE_STROKE_WIDTH = 5;  // default is 10

    // The suggested radius of a manually added quiet place is a multiplier of the current
    // width in meters of the shortest dimension of the current map viewing region
    public static final double SUGGESTED_RADIUS_MULTIPLIER = 0.1;

    // And when resizing a quiet place, we multiply the suggested radius (for the current zoom
    // level, which may be different than the QP's current radius) by this factor to obtain
    // the incremental size change when you hit the resize buttons.
    public static final double QP_RESIZE_INCREMENT_FACTOR = 0.2;

    // Minimum size for a quiet place in meters
    public static final double QP_SIZE_FLOOR = 5.0;

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    static String getTransitionString(Context context, int transitionType) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return context.getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return context.getString(R.string.geofence_transition_exited);

            default:
                return context.getString(R.string.geofence_transition_unknown);
        }
    }
}
