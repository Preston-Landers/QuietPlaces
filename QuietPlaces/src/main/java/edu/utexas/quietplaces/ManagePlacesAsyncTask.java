package edu.utexas.quietplaces;

import android.os.AsyncTask;
import android.util.Log;
import edu.utexas.quietplaces.content_providers.PlacesContentProvider;
import edu.utexas.quietplaces.fragments.QPMapFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This task is launched from MainActivity when it gets the broadcast message
 * that our Google Places database has updated.
 *
 * This is where we convert these discovered places into QuietPlaces and update
 * geofences.
 */
public class ManagePlacesAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = Config.PACKAGE_NAME + ".ManagePlacesAsyncTask";

    private MainActivity mainActivity;

    private Set<String> placeTypesToMatch;

    private List<PlacesContentProvider.Place> matchedPlaces;

    ManagePlacesAsyncTask(MainActivity activity) {
        mainActivity = activity;
        placeTypesToMatch = mainActivity.getSettingsFragment().getCurrentlySelectedPlaceTypes();
    }

    @Override
    protected java.lang.Void doInBackground(Void... params) {
        Log.d(TAG, "doInBackground");
        if (mainActivity == null) {
            Log.e(TAG, "mainActivity is null in doInBackground, can't update places.");
            return null;
        }


        String sortOrder = null;

        List<PlacesContentProvider.Place> placeList = PlacesContentProvider.getAllPlaces(mainActivity, sortOrder);

        if (placeList == null) {
            Log.e(TAG, "GPlaces table query return null.");
            return null;
        }
        if (placeList.size() == 0) {
            Log.w(TAG, "GPlaces table appears to be empty.");
            return null;
        }

        matchedPlaces = new ArrayList<PlacesContentProvider.Place>();
        for (PlacesContentProvider.Place place : placeList) {
            // Log.i(TAG, "Looking at place: " + place);
            if (placeMatchesCriteria(place)) {
                Log.w(TAG, "Found a place matching our criteria: " + place);
                matchedPlaces.add(place);
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG, "onPostExecute");

        // NOTE: I'm not sure whether want to call loadAutomaticQuietPlaces even
        // if our list has 0 entries, because the user might have
        // moved to a new location and we need to get rid of the
        // the old entries. Yet maybe there's no harm in letting them stick
        // around until we actually do get something substantial to update.
        if (matchedPlaces.size() == 0) {
            Log.w(TAG, "No nearby places matched our search criteria.");
            return;
        }

        // Delete all of the automatically added places
        QPMapFragment qpMapFragment = mainActivity.getMapFragment();
        if (qpMapFragment == null) {
            Log.e(TAG, "MainActivity.getMapFragment returned null.");
            return;
        }

        qpMapFragment.loadAutomaticQuietPlaces(matchedPlaces);
    }

    private boolean placeMatchesCriteria(PlacesContentProvider.Place place) {
        // Log.i(TAG, "Running criteria on place: " + place);
        for (String placeType : place.getTypesArray()) {
            if (placeTypeMatchesCriteria(placeType)) {
                Log.i(TAG, "FOUND PLACE: " + place);
                return true;
            }
        }
        return false;
    }

    private boolean placeTypeMatchesCriteria(String placeType) {
        return placeTypesToMatch.contains(placeType.trim());
    }

}
