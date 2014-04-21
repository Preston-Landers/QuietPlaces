package edu.utexas.quietplaces.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import edu.utexas.quietplaces.Config;
import edu.utexas.quietplaces.MainActivity;
import edu.utexas.quietplaces.R;


/**
 * Standard settings fragment.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = Config.PACKAGE_NAME + ".fragments.SettingsFragment";

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final int SECTION_NUMBER = 4;

    public static final String KEY_USE_LOCATION = "pref_use_location";
    public static final String KEY_MAP_TYPE = "pref_map_type";
    public static final String KEY_USE_VIBRATE = "pref_use_vibrate";

    private static final String MAP_TYPE_NORMAL = "MAP_TYPE_NORMAL";
    private static final String MAP_TYPE_HYBRID = "MAP_TYPE_HYBRID";
    private static final String MAP_TYPE_SATELLITE = "MAP_TYPE_SATELLITE";
    private static final String MAP_TYPE_TERRAIN = "MAP_TYPE_TERRAIN";
    private static final String MAP_TYPE_NONE = "MAP_TYPE_NONE";

    private static final String MAP_TYPE_DEFAULT = MAP_TYPE_NORMAL;


    // TODO: this is bad: should be getting these from the string array resource!
    private static final String MAP_TYPE_NORMAL_LABEL = "Normal";
    private static final String MAP_TYPE_HYBRID_LABEL = "Hybrid";
    private static final String MAP_TYPE_SATELLITE_LABEL = "Satellite";
    private static final String MAP_TYPE_TERRAIN_LABEL = "Terrain";
    private static final String MAP_TYPE_NONE_LABEL = "None";

    private Preference mapTypeList;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Activity activity = getActivity();
        if (activity != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            mapTypeList = findPreference(KEY_MAP_TYPE);

            // Set the current value into the summary.
            String mapType = sharedPreferences.getString(KEY_MAP_TYPE, MAP_TYPE_DEFAULT);
            String mapTypeLabel = getMapTypeLabel(mapType);
            mapTypeList.setSummary(mapTypeLabel);

            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                SECTION_NUMBER);
    }

    // TODO: should be getting from the string array resource
    public String getMapTypeLabel(String mapType) {
        String mapTypeLabel = MAP_TYPE_NORMAL_LABEL;
        if (mapType.equals(MAP_TYPE_NORMAL)) {
            mapTypeLabel = MAP_TYPE_NORMAL_LABEL;
        } else if (mapType.equals(MAP_TYPE_HYBRID)) {
            mapTypeLabel = MAP_TYPE_HYBRID_LABEL;
        } else if (mapType.equals(MAP_TYPE_SATELLITE)) {
            mapTypeLabel = MAP_TYPE_SATELLITE_LABEL;
        } else if (mapType.equals(MAP_TYPE_TERRAIN)) {
            mapTypeLabel = MAP_TYPE_TERRAIN_LABEL;
        } else if (mapType.equals(MAP_TYPE_NONE)) {
            mapTypeLabel = MAP_TYPE_NONE_LABEL;
        }
        return mapTypeLabel;
    }

    public int getMapTypeInt(String mapType) {
        int newMapType = GoogleMap.MAP_TYPE_NORMAL;
        if (mapType.equals(MAP_TYPE_NORMAL)) {
            newMapType = GoogleMap.MAP_TYPE_NORMAL;
        } else if (mapType.equals(MAP_TYPE_HYBRID)) {
            newMapType = GoogleMap.MAP_TYPE_HYBRID;
        } else if (mapType.equals(MAP_TYPE_SATELLITE)) {
            newMapType = GoogleMap.MAP_TYPE_SATELLITE;
        } else if (mapType.equals(MAP_TYPE_TERRAIN)) {
            newMapType = GoogleMap.MAP_TYPE_TERRAIN;
        } else if (mapType.equals(MAP_TYPE_NONE)) {
            newMapType = GoogleMap.MAP_TYPE_NONE;
        }
        return newMapType;
    }

    public int getMapTypeInt() {
        return getMapTypeInt(sharedPreferences.getString(KEY_MAP_TYPE, MAP_TYPE_DEFAULT));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_MAP_TYPE)) {
            String mapType = sharedPreferences.getString(KEY_MAP_TYPE, MAP_TYPE_DEFAULT);
            String mapTypeLabel = getMapTypeLabel(mapType);
            int newMapType = getMapTypeInt(mapType);
            Log.i(TAG, "Map type changed to " + mapTypeLabel);
            mapTypeList.setSummary(mapTypeLabel);

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                QPMapFragment mapFragment = mainActivity.getMapFragment();
                if (mapFragment != null) {
                    mapFragment.setMapType(newMapType);
                }
            }
        }
    }
}
