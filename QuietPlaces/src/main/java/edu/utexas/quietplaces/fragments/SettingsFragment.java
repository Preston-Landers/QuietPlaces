package edu.utexas.quietplaces.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import edu.utexas.quietplaces.Config;
import edu.utexas.quietplaces.MainActivity;
import edu.utexas.quietplaces.PlacesConstants;
import edu.utexas.quietplaces.R;
import edu.utexas.quietplaces.utils.PlatformSpecificImplementationFactory;
import edu.utexas.quietplaces.utils.base.SharedPreferenceSaver;

import java.util.*;


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

    public static final String KEY_CONTROL_RINGER = "pref_control_ringer";
    public static final String KEY_USE_LOCATION = "pref_use_location";
    public static final String KEY_MAP_TYPE = "pref_map_type";
    public static final String KEY_USE_VIBRATE = "pref_use_vibrate";

    private static final String MAP_TYPE_NORMAL = "MAP_TYPE_NORMAL";
    private static final String MAP_TYPE_HYBRID = "MAP_TYPE_HYBRID";
    private static final String MAP_TYPE_SATELLITE = "MAP_TYPE_SATELLITE";
    private static final String MAP_TYPE_TERRAIN = "MAP_TYPE_TERRAIN";
    private static final String MAP_TYPE_NONE = "MAP_TYPE_NONE";

    private static final String MAP_TYPE_DEFAULT = MAP_TYPE_NORMAL;

    /// IMPORTANT: these values MUST start with pref_cat_
    public static final String PLACE_CAT = "pref_cat_";

    // Dining & Entertainment
    public static final String KEY_CAT_MOVIE_THEATER = PLACE_CAT + "movie_theater";
    public static final String KEY_CAT_RESTAURANT = PLACE_CAT + "restaurant";
    public static final String KEY_CAT_ART = PLACE_CAT + "art";
    public static final String KEY_CAT_CAFE = PLACE_CAT + "cafe";
    public static final String KEY_CAT_GYM = PLACE_CAT + "gym";

    // Religious & Funeral
    public static final String KEY_CAT_PLACES_OF_WORSHIP = PLACE_CAT + "places_of_worship";
    public static final String KEY_CAT_FUNERAL = PLACE_CAT + "funeral";

    // Medical & Health
    public static final String KEY_CAT_HOSPITALS = PLACE_CAT + "hospital";
    public static final String KEY_CAT_DOCTOR = PLACE_CAT + "doctor";
    public static final String KEY_CAT_PHARMACY = PLACE_CAT + "pharmacy";

    // Schools & Libraries
    public static final String KEY_CAT_SCHOOL = PLACE_CAT + "school";
    public static final String KEY_CAT_UNIVERSITY = PLACE_CAT + "university";
    public static final String KEY_CAT_LIBRARY = PLACE_CAT + "library";

    // Government
    public static final String KEY_CAT_COURTHOUSE = PLACE_CAT + "courthouse";
    public static final String KEY_CAT_POLICE = PLACE_CAT + "police";
    public static final String KEY_CAT_GOVT_OFFICE = PLACE_CAT + "govt_office";



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
        MainActivity mainActivity = (MainActivity) getActivity();
        if (key.equals(KEY_MAP_TYPE)) {
            String mapType = sharedPreferences.getString(KEY_MAP_TYPE, MAP_TYPE_DEFAULT);
            String mapTypeLabel = getMapTypeLabel(mapType);
            int newMapType = getMapTypeInt(mapType);
            Log.i(TAG, "Map type changed to " + mapTypeLabel);
            mapTypeList.setSummary(mapTypeLabel);

            if (mainActivity != null) {
                QPMapFragment mapFragment = mainActivity.getMapFragment();
                if (mapFragment != null) {
                    mapFragment.setMapType(newMapType);
                }
            }
        }
        else if (key.equals(KEY_USE_LOCATION)) {
            boolean usingLocation = sharedPreferences.getBoolean(SettingsFragment.KEY_USE_LOCATION, false);
            if (usingLocation) {
                if (mainActivity != null) {
                    Log.w(TAG, "'Use Location' setting changed to ON. Enabling location updates.");
                    mainActivity.enableMainActivityLocationUpdates();
                } else {
                    Log.e(TAG, "'Use Location' setting changed to ON, but unable to find main activity to enable location updates.");
                }
            } else {
                if (mainActivity != null) {
                    Log.w(TAG, "'Use Location' setting changed to OFF. Disabling location updates.");
                    mainActivity.disableMainActivityLocationUpdates();
                } else {
                    Log.e(TAG, "'Use Location' setting changed to OFF, but unable to find main activity to disable location updates.");
                }
            }

        }

        // Any time we change any 'category' preference, update the master place type pref.
        if (isPlaceCategoryPref(key)) {
            updateMasterPlaceTypes();
        }
    }


    private boolean isPlaceCategoryPref(String prefName) {
        return prefName.indexOf(PLACE_CAT) == 0;
    }

    public static String getSelectedPlaceTypes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PlacesConstants.SP_KEY_API_PLACE_TYPES, getDefaultSelectedPlaceTypes());
    }

    private static String getDefaultSelectedPlaceTypes() {
        return Config.joinString(Config.PLACE_TYPE_DEFAULTS, "|");
    }

    public void updateMasterPlaceTypes() {
        updateSelectedPlacesPref(getCurrentlySelectedPlaceTypes());
    }

    private void updateSelectedPlacesPref(Set<String> selectedPlaceTypes) {
        List<String> placeList = new ArrayList<String>(selectedPlaceTypes);
        Collections.sort(placeList); // want this in stable order just in case it matters...
        String placeTypes = Config.joinString(placeList, "|");
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(PlacesConstants.SP_KEY_API_PLACE_TYPES, placeTypes);
        SharedPreferenceSaver sharedPreferenceSaver = PlatformSpecificImplementationFactory.getSharedPreferenceSaver(getActivity());
        sharedPreferenceSaver.savePreferences(prefsEditor, false);
        prefsEditor.apply(); // ?? didn't have to do this in other spots but IntelliJ wants it...

        Log.w(TAG, "Updated selected place types to: " + placeTypes);
    }

    // see also: https://developers.google.com/places/documentation/supported_types

    public Set<String> getCurrentlySelectedPlaceTypes() {
        Set<String> placeTypes = new HashSet<String>();
        // NEed to handle EACH new preference checkbox here!
        if (sharedPreferences.getBoolean(KEY_CAT_MOVIE_THEATER, false)) {
            placeTypes.add("movie_theater");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_RESTAURANT, false)) {
            placeTypes.add("restaurant");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_ART, false)) {
            placeTypes.add("art_gallery");
            placeTypes.add("museum");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_CAFE, false)) {
            placeTypes.add("cafe");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_GYM, false)) {
            placeTypes.add("gym");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_PLACES_OF_WORSHIP, false)) {
            placeTypes.add("church");
            placeTypes.add("mosque");
            placeTypes.add("hindu_temple");
            placeTypes.add("synagogue");

            // Does this cover all of the above or is it different?!
            placeTypes.add("place_of_worship");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_FUNERAL, false)) {
            placeTypes.add("cemetery");
            placeTypes.add("funeral_home");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_HOSPITALS, false)) {
            placeTypes.add("hospital");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_DOCTOR, false)) {
            placeTypes.add("doctor");
            placeTypes.add("dentist");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_PHARMACY, false)) {
            placeTypes.add("pharmacy");
            placeTypes.add("physiotherapist");
            placeTypes.add("health");
            placeTypes.add("veterinary_care");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_COURTHOUSE, false)) {
            placeTypes.add("courthouse");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_POLICE, false)) {
            placeTypes.add("fire_station");
            placeTypes.add("police");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_GOVT_OFFICE, false)) {
            placeTypes.add("city_hall");
            placeTypes.add("post_office");
            placeTypes.add("embassy");
            placeTypes.add("local_government_office");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_SCHOOL, false)) {
            placeTypes.add("school");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_LIBRARY, false)) {
            placeTypes.add("library");
        }
        if (sharedPreferences.getBoolean(KEY_CAT_UNIVERSITY, false)) {
            placeTypes.add("university");
        }

        return placeTypes;
    }
}
